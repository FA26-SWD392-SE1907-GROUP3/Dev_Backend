package com.example.swd392_se1907_aives;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.swd392_se1907_aives.service.GoogleTokenVerifier;
import com.example.swd392_se1907_aives.repository.UserRepository;
import com.example.swd392_se1907_aives.domain.enums.UserStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthIntegrationTest {
 @LocalServerPort int port;
 @MockitoBean GoogleTokenVerifier verifier;
 @Autowired UserRepository users;
 final ObjectMapper json=new ObjectMapper();
 @Test void onlyAdminCanChangeRolesAndPreviousSessionsAreRevoked() throws Exception {
  var response=call("POST","/auth/register",null,Map.of("username","role_student","password","RolePassword123!","fullName","Role student","email","role_student@aives.local"));
  assertEquals(201,response.statusCode(),response.body()); var student=json.readTree(response.body());
  int id=student.path("user").path("userId").asInt(); String oldToken=student.path("token").asText();
  var update=Map.of("fullName","Role student","email","role_student@aives.local","status","ACTIVE","role","LECTURER");
  assertEquals(403,call("PUT","/users/"+id,oldToken,update).statusCode());
  var adminLogin=json.readTree(call("POST","/auth/login",null,Map.of("username","testadmin","password","TestPassword123!")).body());
  String admin=adminLogin.path("token").asText();
  response=call("PUT","/users/"+id,admin,update); assertEquals(200,response.statusCode(),response.body());
  assertEquals("LECTURER",json.readTree(response.body()).path("role").asText());
  assertEquals(401,call("GET","/auth/me",oldToken,null).statusCode());
  var newSession=json.readTree(call("POST","/auth/login",null,Map.of("username","role_student","password","RolePassword123!")).body());
  assertEquals("LECTURER",newSession.path("user").path("role").asText());
  assertEquals(200,call("GET","/questions",newSession.path("token").asText(),null).statusCode());
  int adminId=adminLogin.path("user").path("userId").asInt();
  var self=Map.of("fullName","Test admin","email","testadmin@aives.local","status","ACTIVE","role","STUDENT");
  assertEquals(409,call("PUT","/users/"+adminId,admin,self).statusCode());
  assertEquals(200,call("GET","/auth/me",admin,null).statusCode());
 }
 HttpResponse<String> call(String method,String path,String token,Object body) throws Exception {
  var b=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api"+path)).header("Content-Type","application/json");
  if(token!=null)b.header("Authorization","Bearer "+token);
  return HttpClient.newHttpClient().send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
 }
 @Test void registrationPersistsStudentAndRejectsDuplicatesAndInvalidPassword() throws Exception {
  var body=new HashMap<String,Object>(Map.of("username","registered_student","password","ValidPassword123!","fullName","Nguyễn Minh An","email","NEWSTUDENT@aives.local","role","ADMIN"));
  var response=call("POST","/auth/register",null,body); assertEquals(201,response.statusCode(),response.body());
  var result=json.readTree(response.body()); assertEquals("STUDENT",result.path("user").path("role").asText());
  assertFalse(response.body().contains("passwordHash"));
  assertEquals(200,call("GET","/auth/me",result.path("token").asText(),null).statusCode());
  assertEquals(403,call("POST","/subjects",result.path("token").asText(),Map.of("subjectCode","NO","subjectName","No")).statusCode());
  assertEquals(200,call("POST","/auth/login",null,Map.of("username","registered_student","password","ValidPassword123!")).statusCode());
  var stored=users.findByUsername("registered_student").orElseThrow(); assertNotEquals("ValidPassword123!",stored.getPasswordHash());
  assertEquals(409,call("POST","/auth/register",null,body).statusCode());
  body.put("username","different_student"); body.put("email","newstudent@aives.local");
  assertEquals(409,call("POST","/auth/register",null,body).statusCode());
  body.put("email","other@aives.local"); body.put("password","short"); assertEquals(400,call("POST","/auth/register",null,body).statusCode());
  body.put("password","á".repeat(40)); assertEquals(400,call("POST","/auth/register",null,body).statusCode());
 }
 @Test void googleNonceIsSingleUseAndAccountCannotBeLinkedByEmailOrBypassInactiveStatus() throws Exception {
  when(verifier.clientId()).thenReturn("test-client");
  String nonce=json.readTree(call("GET","/auth/google/challenge",null,null).body()).path("nonce").asText();
  Jwt jwt=Jwt.withTokenValue("verified").header("alg","RS256").subject("google-subject-1").claim("email","google-test@aives.local").claim("name","Google Student").build();
  when(verifier.verify("verified",nonce)).thenReturn(jwt);
  var input=Map.of("credential","verified","nonce",nonce);
  var response=call("POST","/auth/google",null,input); assertEquals(200,response.statusCode(),response.body());
  assertEquals("STUDENT",json.readTree(response.body()).path("user").path("role").asText());
  assertEquals(401,call("POST","/auth/google",null,input).statusCode());
  var user=users.findByGoogleSubject("google-subject-1").orElseThrow(); user.setUserStatus(UserStatus.INACTIVE); users.saveAndFlush(user);
  String next=json.readTree(call("GET","/auth/google/challenge",null,null).body()).path("nonce").asText();
  when(verifier.verify("verified",next)).thenReturn(jwt);
  assertEquals(401,call("POST","/auth/google",null,Map.of("credential","verified","nonce",next)).statusCode());
  String collision=json.readTree(call("GET","/auth/google/challenge",null,null).body()).path("nonce").asText();
  var other=Jwt.withTokenValue("other").header("alg","RS256").subject("different-subject").claim("email","google-test@aives.local").build();
  when(verifier.verify("other",collision)).thenReturn(other);
  assertEquals(409,call("POST","/auth/google",null,Map.of("credential","other","nonce",collision)).statusCode());
 }
}
