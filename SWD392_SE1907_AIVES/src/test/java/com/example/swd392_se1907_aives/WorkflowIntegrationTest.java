package com.example.swd392_se1907_aives;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.example.swd392_se1907_aives.service.AiService;
import tools.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WorkflowIntegrationTest {
 @LocalServerPort int port;
 @MockitoBean AiService ai;
 @org.springframework.beans.factory.annotation.Autowired com.example.swd392_se1907_aives.repository.InterviewLogRepository logRepository;
 private final HttpClient client=HttpClient.newHttpClient();
 private final ObjectMapper json=new ObjectMapper();
 private HttpResponse<String> call(String method,String path,String token,Object body) throws Exception {
  var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api"+path)).header("Content-Type","application/json");
  if(token!=null) builder.header("Authorization","Bearer "+token);
  builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
  return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
 }
 private JsonNode ok(String method,String path,String token,Object body) throws Exception {
  var response=call(method,path,token,body);
  assertTrue(response.statusCode()>=200 && response.statusCode()<300,response.body());
  return response.body().isBlank()?json.createObjectNode():json.readTree(response.body());
 }
 private String login(String username) throws Exception { return ok("POST","/auth/login",null,Map.of("username",username,"password","TestPassword123!")).path("token").asText(); }
 private int account(String admin,String name,String role) throws Exception {
  return ok("POST","/users",admin,Map.of("username",name,"password","TestPassword123!","fullName",name,"email",name+"@aives.local","role",role)).path("userId").asInt();
 }
 @Test void authenticatedOralExamWorkflowAndOwnership() throws Exception {
  assertEquals(401,call("GET","/subjects",null,null).statusCode());
  assertEquals(401,call("POST","/auth/login",null,Map.of("username","testadmin","password","wrong")).statusCode());
  String admin=login("testadmin");
  account(admin,"lecturer1","LECTURER"); int studentId=account(admin,"student1","STUDENT"); account(admin,"student2","STUDENT"); account(admin,"lecturer2","LECTURER");
  String lecturer=login("lecturer1"), student=login("student1"), otherStudent=login("student2"), otherLecturer=login("lecturer2");
  int subject=ok("POST","/subjects",admin,Map.of("subjectCode","SWD392","subjectName","Thiết kế phần mềm")).path("subjectId").asInt();
  assertEquals(409,call("POST","/subjects",admin,Map.of("subjectCode","SWD392","subjectName","Duplicate")).statusCode());
  assertEquals(403,call("POST","/subjects",student,Map.of("subjectCode","BAD","subjectName","No")).statusCode());
  int question=ok("POST","/questions",lecturer,Map.of("subjectId",subject,"topic","Architecture","questionContent","Explain dependency inversion","bloomLevel","UNDERSTAND")).path("questionId").asInt();
  assertEquals(403,call("DELETE","/questions/"+question,otherLecturer,null).statusCode());
  assertEquals(409,call("POST","/questions/"+question+"/review",lecturer,Map.of("status","APPROVED")).statusCode());
  ok("POST","/questions/"+question+"/rubrics",lecturer,Map.of("criteriaName","Correct explanation","maxScore",10,"guideline","Explain dependencies pointing toward domain"));
  ok("POST","/questions/"+question+"/review",lecturer,Map.of("status","APPROVED"));
  var examInput=Map.of("subjectId",subject,"examName","Viva test","startTime",LocalDateTime.now().minusMinutes(10).toString(),"endTime",LocalDateTime.now().plusHours(1).toString(),"maxMainQuestions",1,"maxFollowUpQuestions",1,"answerTimeLimitSeconds",120);
  int exam=ok("POST","/exams",lecturer,examInput).path("examId").asInt();
  var slot=Map.of("studentId",studentId,"allocatedStartTime",LocalDateTime.now().minusMinutes(1).toString(),"allocatedEndTime",LocalDateTime.now().plusMinutes(30).toString());
  int schedule=ok("POST","/exams/"+exam+"/schedules",lecturer,slot).path("scheduleId").asInt();
  assertEquals(409,call("POST","/exams/"+exam+"/schedules",lecturer,slot).statusCode());
  ok("POST","/exams/"+exam+"/publish",lecturer,null);
  assertEquals(409,call("DELETE","/questions/"+question,lecturer,null).statusCode());
  assertEquals(403,call("GET","/interviews/"+schedule,otherStudent,null).statusCode());
  var state=ok("POST","/interviews/"+schedule+"/start",student,null);
  int log=state.path("logs").get(0).path("logId").asInt();
  assertEquals(1,ok("POST","/interviews/"+schedule+"/start",student,null).path("logs").size());
  assertEquals(409,call("POST","/interviews/"+schedule+"/next?afterLogId="+log,student,null).statusCode());
  ok("POST","/interviews/"+schedule+"/answer",student,Map.of("logId",log,"transcript","Dependency inversion makes the domain independent"));
  when(ai.followUp(anyString(),anyString(),anyString(),anyString())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"AI unavailable"));
  assertEquals(503,call("POST","/interviews/"+schedule+"/next?afterLogId="+log,student,null).statusCode());
  assertEquals("Dependency inversion makes the domain independent",ok("GET","/interviews/"+schedule,student,null).path("logs").get(0).path("studentAnswerTranscript").asText());
  doReturn("Give one concrete example.").when(ai).followUp(anyString(),anyString(),anyString(),anyString());
  var follow=ok("POST","/interviews/"+schedule+"/next?afterLogId="+log,student,null);
  assertEquals("FOLLOW_UP",follow.path("logs").get(1).path("questionType").asText());
  assertEquals(log,follow.path("logs").get(1).path("parentLogId").asInt());
  assertEquals(2,ok("POST","/interviews/"+schedule+"/next?afterLogId="+log,student,null).path("logs").size());
  int followId=follow.path("logs").get(1).path("logId").asInt();
  ok("POST","/interviews/"+schedule+"/answer",student,Map.of("logId",followId,"transcript","Infrastructure implements a domain interface"));
  var finished=ok("POST","/interviews/"+schedule+"/next?afterLogId="+followId,student,null);
  assertEquals("COMPLETED",finished.path("schedule").path("status").asText());
  assertEquals(2,finished.path("logs").size());
  verify(ai,times(2)).followUp(anyString(),anyString(),anyString(),anyString());
  ok("POST","/exams/"+exam+"/complete",lecturer,null);
  ok("POST","/auth/logout",student,null);
  assertEquals(401,call("GET","/auth/me",student,null).statusCode());
 }

 @Test void documentIndexingGenerationAndAtomicImport() throws Exception {
  String admin=login("testadmin"); account(admin,"doclecturer","LECTURER"); String lecturer=login("doclecturer");
  int subject=ok("POST","/subjects",admin,Map.of("subjectCode","DOC101","subjectName","Document course")).path("subjectId").asInt();
  String boundary="aives-test-boundary";
  String multipart="--"+boundary+"\r\nContent-Disposition: form-data; name=\"subjectId\"\r\n\r\n"+subject+"\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\nLecture notes\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"notes.txt\"\r\nContent-Type: text/plain\r\n\r\nDependency inversion makes domain code independent of infrastructure.\r\n--"+boundary+"--\r\n";
  var response=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/documents")).header("Authorization","Bearer "+lecturer).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofString(multipart)).build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(200,response.statusCode(),response.body()); var document=json.readTree(response.body()); assertEquals("INDEXED",document.path("status").asText()); int id=document.path("documentId").asInt();
  when(ai.context(anyString(),anyString())).thenAnswer(invocation -> invocation.getArgument(0));
  when(ai.ask(anyString(),anyString())).thenReturn(json.readTree("{\"questions\":[{\"questionContent\":\"Explain dependency inversion\",\"criteria\":[{\"criteriaName\":\"Explanation\",\"maxScore\":10,\"guideline\":\"Domain is independent\"}]}]}"));
  var generated=ok("POST","/documents/"+id+"/generate",lecturer,Map.of("count",1,"topic","dependency inversion","bloomLevel","UNDERSTAND"));
  int question=generated.get(0).path("questionId").asInt(); assertEquals("AI_RAG",generated.get(0).path("source").asText()); assertEquals("PENDING_APPROVAL",generated.get(0).path("status").asText());
  assertEquals(1,ok("GET","/questions/"+question+"/rubrics",lecturer,null).size());
  assertEquals(409,call("DELETE","/documents/"+id,lecturer,null).statusCode());
  var valid=Map.of("subjectId",subject,"questionContent","Imported question","bloomLevel","REMEMBER");
  var invalid=Map.of("subjectId",subject,"questionContent","","bloomLevel","REMEMBER");
  assertEquals(400,call("POST","/questions/import",lecturer,List.of(valid,invalid)).statusCode());
  assertEquals(1,ok("GET","/questions",lecturer,null).size());
  ok("POST","/questions/import",lecturer,List.of(valid)); assertEquals(2,ok("GET","/questions",lecturer,null).size());
  ok("DELETE","/questions/"+question,lecturer,null); ok("DELETE","/documents/"+id,lecturer,null);
 }

 @Test void deadlinesAndMainQuestionLimitsAreEnforcedByServer() throws Exception {
  String admin=login("testadmin"); account(admin,"timeoutlecturer","LECTURER"); int studentId=account(admin,"timeoutstudent","STUDENT");
  String lecturer=login("timeoutlecturer"), student=login("timeoutstudent");
  int subject=ok("POST","/subjects",admin,Map.of("subjectCode","TIME101","subjectName","Time limits")).path("subjectId").asInt();
  for(int i=0;i<2;i++) {
   int q=ok("POST","/questions",lecturer,Map.of("subjectId",subject,"questionContent","Question "+i,"bloomLevel","REMEMBER")).path("questionId").asInt();
   ok("POST","/questions/"+q+"/rubrics",lecturer,Map.of("criteriaName","Correct answer","maxScore",10));
   ok("POST","/questions/"+q+"/review",lecturer,Map.of("status","APPROVED"));
  }
  int exam=ok("POST","/exams",lecturer,Map.of("subjectId",subject,"examName","Deadline exam","startTime",LocalDateTime.now().minusMinutes(5).toString(),"endTime",LocalDateTime.now().plusHours(1).toString(),"maxMainQuestions",2,"maxFollowUpQuestions",0,"answerTimeLimitSeconds",15)).path("examId").asInt();
  int schedule=ok("POST","/exams/"+exam+"/schedules",lecturer,Map.of("studentId",studentId,"allocatedStartTime",LocalDateTime.now().minusMinutes(1).toString(),"allocatedEndTime",LocalDateTime.now().plusMinutes(30).toString())).path("scheduleId").asInt();
  ok("POST","/exams/"+exam+"/publish",lecturer,null);
  var start=ok("POST","/interviews/"+schedule+"/start",student,null); int firstLog=start.path("logs").get(0).path("logId").asInt();
  var expired=logRepository.findById(firstLog).orElseThrow(); expired.setAskedAt(LocalDateTime.now().minusSeconds(30)); logRepository.saveAndFlush(expired);
  assertEquals(409,call("POST","/interviews/"+schedule+"/answer",student,Map.of("logId",firstLog,"transcript","Late answer")).statusCode());
  var next=ok("POST","/interviews/"+schedule+"/next?afterLogId="+firstLog,student,null);
  assertEquals(2,next.path("logs").size()); assertEquals(15,next.path("logs").get(0).path("timeTakenSeconds").asInt());
  assertTrue(next.path("logs").get(0).path("studentAnswerTranscript").isNull());
  assertNotEquals(next.path("logs").get(0).path("questionId").asInt(),next.path("logs").get(1).path("questionId").asInt());
  int secondLog=next.path("logs").get(1).path("logId").asInt();
  ok("POST","/interviews/"+schedule+"/answer",student,Map.of("logId",secondLog,"transcript","On time"));
  var end=ok("POST","/interviews/"+schedule+"/next?afterLogId="+secondLog,student,null);
  assertEquals("COMPLETED",end.path("schedule").path("status").asText()); assertEquals(2,end.path("logs").size());
  verifyNoInteractions(ai);
 }
}
