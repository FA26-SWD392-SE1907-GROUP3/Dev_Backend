package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;
import com.example.swd392_se1907_aives.modules.identity.internal.repository.UserRepository;
import com.example.swd392_se1907_aives.modules.identity.internal.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:registration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.interview.timeouts-enabled=false",
    "app.grading.enabled=false","JWT_SECRET=registration_test_secret_at_least_32_characters"})
@AutoConfigureMockMvc
class RegistrationTests {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;@Autowired JwtService jwt;
    static final String PASSWORD="StudyPass123!";
    private String email(){return UUID.randomUUID()+"@example.com";}
    private String payload(String email){return "{\"fullName\":\"Nguyễn Văn An\",\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\",\"confirmPassword\":\""+PASSWORD+"\"}";}
    private User register(String email)throws Exception{
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(payload(email)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.registrationStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.token").doesNotExist());
        return users.findByEmailIgnoreCase(email.trim()).orElseThrow();
    }
    private User account(String role){var user=new User();user.setEmail(email());user.setFullName("Approved "+role);user.setRole(role);user.setIsActive(true);user.setPasswordHash(encoder.encode(PASSWORD));user.setCreatedAt(OffsetDateTime.now());user.setUpdatedAt(OffsetDateTime.now());return users.saveAndFlush(user);}
    private String token(User user){return "Bearer "+jwt.generateToken(user.getId().toString(),user.getEmail(),user.getRole());}

    @Test void publicSignupStoresInactivePendingAccountWithHashedPasswordAndNoRoleChoice()throws Exception{
        String email=email();String request=payload(email).replace("}",",\"role\":\"ADMIN\",\"active\":true}");
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.token").doesNotExist());
        var user=users.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(user.getIsActive()).isFalse();assertThat(user.isPendingRegistration()).isTrue();
        assertThat(user.getRole()).isEqualTo("STUDENT");assertThat(user.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD,user.getPasswordHash())).isTrue();
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",email,"password",PASSWORD))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("chờ Admin")));
    }
    @Test void onlyAdminCanAssignTheActualRoleAndEnableLogin()throws Exception{
        var pending=register(email());var admin=account("ADMIN");String path="/api/v1/users/"+pending.getId()+"/approve-registration";
        mvc.perform(post(path).contentType("application/json").content("{\"role\":\"LECTURER\"}")).andExpect(status().isUnauthorized());
        for(String role:List.of("STUDENT","LECTURER"))mvc.perform(post(path).header("Authorization",token(account(role))).contentType("application/json").content("{\"role\":\"LECTURER\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users/"+pending.getId()).header("Authorization",token(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").doesNotExist()).andExpect(jsonPath("$.data.registrationStatus").value("PENDING"));
        mvc.perform(post(path).header("Authorization",token(admin)).contentType("application/json").content("{\"role\":\"LECTURER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("LECTURER")).andExpect(jsonPath("$.data.active").value(true));
        var login=json.readTree(mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",pending.getEmail(),"password",PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(jwt.validateToken(login.path("data").path("token").asText())).isTrue();
        mvc.perform(post(path).header("Authorization",token(admin)).contentType("application/json").content("{\"role\":\"ADMIN\"}")).andExpect(status().isConflict());
    }
    @Test void pendingAccountCannotBypassApprovalViaActivationOrAnOtherwiseValidJwt()throws Exception{
        var pending=register(email());var admin=account("ADMIN");
        mvc.perform(patch("/api/v1/users/"+pending.getId()+"/activate").header("Authorization",token(admin))).andExpect(status().isConflict());
        pending.setIsActive(true);users.saveAndFlush(pending);
        mvc.perform(get("/api/v1/courses").header("Authorization",token(pending))).andExpect(status().isUnauthorized());
    }
    @Test void invalidFieldsAndMissingApprovalRoleAreRejected()throws Exception{
        String email=email();
        var good=new HashMap<String,Object>();good.put("fullName","Tên người dùng");good.put("email",email);good.put("password",PASSWORD);good.put("confirmPassword",PASSWORD);
        for(var invalid:List.of(Map.of("email","not-email"),Map.of("fullName"," "),Map.of("password","short"),Map.of("confirmPassword","different"),Map.of("password","ắ".repeat(30),"confirmPassword","ắ".repeat(30)))){
            var input=new HashMap<>(good);input.putAll(invalid);
            mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isBadRequest());
        }
        var pending=register(email);var admin=account("ADMIN");
        mvc.perform(post("/api/v1/users/"+pending.getId()+"/approve-registration").header("Authorization",token(admin)).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
    @Test void duplicateEmailIsCaseInsensitiveAndWhitespaceIsNormalized()throws Exception{
        String email=email();register(" "+email.toUpperCase(Locale.ROOT)+" ");
        assertThat(users.findByEmailIgnoreCase(email).orElseThrow().getEmail()).isEqualTo(email);
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(payload(email))).andExpect(status().isConflict());
    }
    @Test void simultaneousDuplicateSignupsCreateOnlyOneAccount()throws Exception{
        String email=email();var workers=Executors.newFixedThreadPool(2);
        try{
            var results=workers.invokeAll(List.<Callable<Integer>>of(
                    ()->mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(payload(email))).andReturn().getResponse().getStatus(),
                    ()->mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(payload(email.toUpperCase(Locale.ROOT)))).andReturn().getResponse().getStatus()));
            assertThat(List.of(results.get(0).get(),results.get(1).get())).containsExactlyInAnyOrder(201,409);
        }finally{workers.shutdownNow();}
    }
    @Test void existingAccountsWithoutRegistrationStatusCanStillLogin()throws Exception{
        var user=account("ADMIN");user.setRegistrationStatus(null);users.saveAndFlush(user);
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(json.writeValueAsString(Map.of("email",user.getEmail(),"password",PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.authenticate").value(true));
    }
}
