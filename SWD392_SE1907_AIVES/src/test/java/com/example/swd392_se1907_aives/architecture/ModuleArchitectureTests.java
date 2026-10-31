package com.example.swd392_se1907_aives.architecture;

import static org.assertj.core.api.Assertions.*;

import com.example.swd392_se1907_aives.Swd392Se1907AivesApplication;

import org.junit.jupiter.api.Test;
import org.springframework.asm.*;

import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

/** Checks compiled production bytecode, including method bodies and generated Lombok accessors. */
class ModuleArchitectureTests {
    private static final String ROOT = "com/example/swd392_se1907_aives/";
    private static final Pattern PROJECT_TYPE = Pattern.compile(ROOT + "[\\w/$]+");
    private static final Map<String, Set<String>> ALLOWED =
            Map.of(
                    "identity", Set.of(),
                    "teaching", Set.of("identity"),
                    "examination", Set.of("identity", "teaching"));

    @Test
    void modulesUseOnlyAllowedPublicContractsAndHaveNoCycles() throws Exception {
        Map<String, Set<String>> graph = new HashMap<>();
        List<String> violations = new ArrayList<>();
        for (var entry : productionDependencies().entrySet()) {
            String owner = module(entry.getKey());
            if (!owner.isEmpty()) assertThat(ALLOWED).containsKey(owner);
            for (String dependency : entry.getValue()) {
                String target = module(dependency);
                if (entry.getKey().startsWith(ROOT + "shared/") && !target.isEmpty())
                    violations.add(
                            "Shared infrastructure cannot depend on business modules: "
                                    + dependency);
                if (!target.isEmpty() && !target.equals(owner)) {
                    if (!dependency.startsWith(ROOT + "modules/" + target + "/api/"))
                        violations.add(
                                entry.getKey() + " accesses module internals: " + dependency);
                    if (!owner.isEmpty()) {
                        if (!ALLOWED.getOrDefault(owner, Set.of()).contains(target))
                            violations.add(owner + " is not allowed to depend on " + target);
                        graph.computeIfAbsent(owner, key -> new HashSet<>()).add(target);
                    }
                }
                if (!owner.isEmpty()
                        && dependency.startsWith(ROOT)
                        && target.isEmpty()
                        && !dependency.startsWith(ROOT + "shared/"))
                    violations.add(
                            entry.getKey()
                                    + " depends on non-module implementation: "
                                    + dependency);
            }
        }
        assertThat(violations).isEmpty();
        for (String name : ALLOWED.keySet())
            assertAcyclic(name, graph, new HashSet<>(), new HashSet<>());
    }

    @Test
    void publicContractsExposeOnlyInterfacesAndImmutableValueRecords() throws Exception {
        List<String> violations = new ArrayList<>();
        for (String name : productionDependencies().keySet()) {
            if (!name.contains("/api/") || module(name).isEmpty() || name.endsWith("package-info"))
                continue;
            Class<?> type =
                    Class.forName(name.replace('/', '.'), false, getClass().getClassLoader());
            if (!type.isInterface() && !type.isRecord())
                violations.add(type.getName() + " is neither interface nor record");
            for (var method : type.getDeclaredMethods())
                if (Modifier.isPublic(method.getModifiers())) {
                    assertContractType(method.getGenericReturnType().getTypeName(), violations);
                    for (var parameter : method.getGenericParameterTypes())
                        assertContractType(parameter.getTypeName(), violations);
                }
            for (var constructor : type.getConstructors())
                for (var parameter : constructor.getGenericParameterTypes())
                    assertContractType(parameter.getTypeName(), violations);
        }
        assertThat(violations).isEmpty();
    }

    @Test
    void controllersDelegatePersistenceToServicesAndServicesDoNotDependOnControllers()
            throws Exception {
        List<String> violations = new ArrayList<>();
        for (var entry : productionDependencies().entrySet()) {
            boolean controller = entry.getKey().contains("/internal/controller/");
            boolean service = entry.getKey().contains("/internal/service/");
            for (String dependency : entry.getValue()) {
                if (controller && dependency.contains("/internal/repository/"))
                    violations.add(
                            entry.getKey() + " accesses persistence directly: " + dependency);
                if (service && dependency.contains("/internal/controller/"))
                    violations.add(entry.getKey() + " depends on the HTTP layer: " + dependency);
            }
            if (controller) {
                Class<?> type =
                        Class.forName(
                                entry.getKey().replace('/', '.'),
                                false,
                                getClass().getClassLoader());
                if (type.isAnnotationPresent(
                        org.springframework.transaction.annotation.Transactional.class))
                    violations.add(type.getName() + " owns a transaction instead of its service");
                for (var method : type.getDeclaredMethods())
                    if (method.isAnnotationPresent(
                            org.springframework.transaction.annotation.Transactional.class))
                        violations.add(
                                type.getName()
                                        + "."
                                        + method.getName()
                                        + " owns a transaction instead of its service");
            }
        }
        assertThat(violations).isEmpty();
    }

    @Test
    void controllersDependOnlyOnServiceInterfaces() throws Exception {
        for (String name : productionDependencies().keySet()) {
            Class<?> type =
                    Class.forName(name.replace('/', '.'), false, getClass().getClassLoader());
            if (!type.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class)) continue;
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                assertThat(field.getType().isInterface())
                        .as(
                                "%s.%s must depend on a service interface",
                                type.getSimpleName(), field.getName())
                        .isTrue();
                assertThat(field.getType().getPackageName()).contains(".internal.service");
            }
        }
    }

    @Test
    void serviceInterfacesHaveOneSeparateSpringImplementation() throws Exception {
        List<Class<?>> types = new ArrayList<>();
        for (String name : productionDependencies().keySet())
            types.add(Class.forName(name.replace('/', '.'), false, getClass().getClassLoader()));
        for (Class<?> contract : types) {
            if (!contract.getPackageName().endsWith(".internal.service")
                    || !contract.getSimpleName().endsWith("Service")) continue;
            assertThat(contract.isInterface())
                    .as("%s must be an interface", contract.getName())
                    .isTrue();
            var implementations =
                    types.stream()
                            .filter(type -> !type.isInterface() && contract.isAssignableFrom(type))
                            .toList();
            assertThat(implementations).as("Implementation of %s", contract.getName()).hasSize(1);
            Class<?> implementation = implementations.getFirst();
            assertThat(implementation.getPackageName())
                    .isEqualTo(contract.getPackageName() + ".impl");
            assertThat(implementation.getSimpleName()).isEqualTo(contract.getSimpleName() + "Impl");
            assertThat(
                            implementation.isAnnotationPresent(
                                    org.springframework.stereotype.Service.class))
                    .isTrue();
        }
    }

    @Test
    void allBusinessModulesOwnTheirPersistenceAndOldHorizontalPackagesAreGone() throws Exception {
        var names = productionDependencies().keySet();
        for (String name : ALLOWED.keySet()) {
            assertThat(names)
                    .anyMatch(
                            type ->
                                    type.startsWith(
                                            ROOT + "modules/" + name + "/internal/repository/"));
            assertThat(names)
                    .anyMatch(
                            type ->
                                    type.startsWith(
                                            ROOT + "modules/" + name + "/internal/controller/"));
        }
        assertThat(names)
                .noneMatch(
                        type ->
                                type.startsWith(ROOT + "controller/")
                                        || type.startsWith(ROOT + "repository/")
                                        || type.startsWith(ROOT + "service/")
                                        || type.startsWith(ROOT + "domain/")
                                        || type.startsWith(ROOT + "compatibility/"));
        for (String name : names) {
            Class<?> type =
                    Class.forName(name.replace('/', '.'), false, getClass().getClassLoader());
            if (org.springframework.data.repository.Repository.class.isAssignableFrom(type))
                assertThat(name).contains("/internal/repository/");
            if (type.isAnnotationPresent(jakarta.persistence.Entity.class)
                    || type.isAnnotationPresent(jakarta.persistence.Embeddable.class))
                assertThat(name)
                        .matches(
                                ROOT
                                        + "modules/(?:identity|teaching|examination)/internal/domain/entity/.+");
            if (type.isAnnotationPresent(
                    org.springframework.web.bind.annotation.RestController.class))
                assertThat(name)
                        .matches(
                                ROOT
                                        + "modules/(?:identity|teaching|examination)/internal/controller/.+");
        }
    }

    @Test
    void dependencyReaderDetectsIllegalAccessEvenInsideAMethodBody() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC,
                ROOT + "modules/identity/internal/InvalidExample",
                null,
                "java/lang/Object",
                null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "bad", "()V", null, null);
        String forbidden = ROOT + "modules/examination/internal/repository/ExamRepository";
        method.visitCode();
        method.visitTypeInsn(Opcodes.NEW, forbidden);
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
        writer.visitEnd();
        assertThat(dependencies(writer.toByteArray())).contains(forbidden);
    }

    private static void assertContractType(String signature, List<String> violations) {
        if (signature.contains(".internal.") || signature.contains(".compatibility."))
            violations.add("Public contract leaks " + signature);
    }

    private static String module(String name) {
        String prefix = ROOT + "modules/";
        return name.startsWith(prefix) ? name.substring(prefix.length()).split("/")[0] : "";
    }

    private static void assertAcyclic(
            String node,
            Map<String, Set<String>> graph,
            Set<String> visiting,
            Set<String> visited) {
        if (visited.contains(node)) return;
        assertThat(visiting.add(node)).as("Dependency cycle at %s", node).isTrue();
        for (String target : graph.getOrDefault(node, Set.of()))
            assertAcyclic(target, graph, visiting, visited);
        visiting.remove(node);
        visited.add(node);
    }

    private Map<String, Set<String>> productionDependencies() throws Exception {
        Path directory =
                Path.of(
                        Swd392Se1907AivesApplication.class
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toURI());
        Map<String, Set<String>> result = new TreeMap<>();
        try (var files = Files.walk(directory.resolve(ROOT))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".class")).toList()) {
                byte[] bytes = Files.readAllBytes(file);
                result.put(new ClassReader(bytes).getClassName(), dependencies(bytes));
            }
        }
        return result;
    }

    private static Set<String> dependencies(byte[] bytes) {
        Set<String> result = new HashSet<>();
        new ClassReader(bytes)
                .accept(
                        new ClassVisitor(Opcodes.ASM9) {
                            void add(String value) {
                                if (value != null) {
                                    var matcher = PROJECT_TYPE.matcher(value);
                                    while (matcher.find()) result.add(matcher.group());
                                }
                            }

                            void constant(Object value) {
                                if (value instanceof Type type) add(type.getDescriptor());
                                if (value instanceof Handle handle) {
                                    add(handle.getOwner());
                                    add(handle.getDesc());
                                }
                            }

                            @Override
                            public void visit(
                                    int version,
                                    int access,
                                    String name,
                                    String signature,
                                    String parent,
                                    String[] interfaces) {
                                add(parent);
                                add(signature);
                                if (interfaces != null) for (String type : interfaces) add(type);
                            }

                            @Override
                            public AnnotationVisitor visitAnnotation(
                                    String descriptor, boolean visible) {
                                add(descriptor);
                                return null;
                            }

                            @Override
                            public FieldVisitor visitField(
                                    int access,
                                    String name,
                                    String descriptor,
                                    String signature,
                                    Object value) {
                                add(descriptor);
                                add(signature);
                                constant(value);
                                return null;
                            }

                            @Override
                            public MethodVisitor visitMethod(
                                    int access,
                                    String name,
                                    String descriptor,
                                    String signature,
                                    String[] exceptions) {
                                add(descriptor);
                                add(signature);
                                if (exceptions != null) for (String type : exceptions) add(type);
                                return new MethodVisitor(Opcodes.ASM9) {
                                    @Override
                                    public void visitTypeInsn(int opcode, String type) {
                                        add(type);
                                    }

                                    @Override
                                    public void visitFieldInsn(
                                            int opcode,
                                            String owner,
                                            String name,
                                            String descriptor) {
                                        add(owner);
                                        add(descriptor);
                                    }

                                    @Override
                                    public void visitMethodInsn(
                                            int opcode,
                                            String owner,
                                            String name,
                                            String descriptor,
                                            boolean isInterface) {
                                        add(owner);
                                        add(descriptor);
                                    }

                                    @Override
                                    public void visitInvokeDynamicInsn(
                                            String name,
                                            String descriptor,
                                            Handle bootstrap,
                                            Object... arguments) {
                                        add(descriptor);
                                        constant(bootstrap);
                                        for (Object argument : arguments) constant(argument);
                                    }

                                    @Override
                                    public void visitLdcInsn(Object value) {
                                        constant(value);
                                    }

                                    @Override
                                    public void visitMultiANewArrayInsn(
                                            String descriptor, int dimensions) {
                                        add(descriptor);
                                    }
                                };
                            }
                        },
                        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return result;
    }
}
