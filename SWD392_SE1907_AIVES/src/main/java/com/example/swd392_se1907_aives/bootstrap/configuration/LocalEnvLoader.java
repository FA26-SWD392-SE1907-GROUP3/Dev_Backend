package com.example.swd392_se1907_aives.bootstrap.configuration;



import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read a developer's local .env from the project root even when IntelliJ's
 * working directory points to the parent folder. Real OS environment
 * variables and JVM -D properties always take precedence.
 *
 * No secrets are shipped with the application or printed to the logs.
 */
public final class LocalEnvLoader {
    private static final Pattern ENV_ENTRY = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(.*)$");

    private LocalEnvLoader() {
    }

    public static void load() {
        for (Path directory : candidateProjectDirectories()) {
            Path envFile = directory.resolve(".env");
            if (Files.isRegularFile(directory.resolve("pom.xml")) && Files.isRegularFile(envFile)) {
                importEnvFile(envFile);
                return;
            }
        }
    }

    private static List<Path> candidateProjectDirectories() {
        Set<Path> directories = new LinkedHashSet<>();

        // First identify the project using target/classes or target/<app>.jar.
        try {
            Path codeSource = Path.of(LocalEnvLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath().normalize();
            Path parent = Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
            for (int i = 0; parent != null && i < 6; i++, parent = parent.getParent()) {
                directories.add(parent);
            }
        } catch (URISyntaxException | RuntimeException ignored) {
            // Falling back to the working directory is sufficient for many setups.
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        directories.add(workingDirectory);
        directories.add(workingDirectory.resolve("SWD392_SE1907_AIVES"));
        return new ArrayList<>(directories);
    }

    private static void importEnvFile(Path file) {
        try {
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }
                Matcher matcher = ENV_ENTRY.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }
                String name = matcher.group(1);
                String value = matcher.group(2).trim();
                if (value.length() >= 2 &&
                        ((value.startsWith("\"") && value.endsWith("\"")) ||
                         (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                // Never replace a deployment environment variable or -D value.
                if (System.getenv(name) == null && System.getProperty(name) == null) {
                    System.setProperty(name, value);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read local .env: " + file, exception);
        }
    }
}
