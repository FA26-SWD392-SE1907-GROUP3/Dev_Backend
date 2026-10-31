package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamCandidate;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPool;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPoolId;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.AiInterviewRequestRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.AttemptQuestionRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamQuestionPoolRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.InterviewTurnRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewAiClient;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewStateStore;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;
import com.example.swd392_se1907_aives.modules.identity.internal.repository.UserRepository;
import com.example.swd392_se1907_aives.modules.identity.internal.service.JwtService;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Course;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollment;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollmentId;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Question;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Rubric;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.RubricCriterion;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseEnrollmentRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricCriterionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricRepository;


import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:interviews;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.interview.timeouts-enabled=false",
    "app.ai.api-key=test-key-not-real","JWT_SECRET=interview_test_secret_at_least_32_characters_long"})
@AutoConfigureMockMvc
class InterviewFlowTests {
    static final Path STORAGE=Path.of("target","test-interviews-"+UUID.randomUUID()).toAbsolutePath();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("app.interview.storage-dir",STORAGE::toString);}
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JwtService jwt;
    @Autowired UserRepository users;@Autowired CourseRepository courses;@Autowired CourseEnrollmentRepository enrollments;
    @Autowired ExamRepository exams;@Autowired ExamCandidateRepository candidates;@Autowired QuestionRepository questions;
    @Autowired ExamQuestionPoolRepository pool;@Autowired AttemptQuestionRepository assigned;@Autowired InterviewTurnRepository turns;
    @Autowired ExamAttemptRepository attempts;@Autowired RubricRepository rubrics;@Autowired RubricCriterionRepository criteria;
    @Autowired InterviewStateStore store;@Autowired AiInterviewRequestRepository requests;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @MockitoBean InterviewAiClient ai;
    @MockitoBean com.example.swd392_se1907_aives.modules.examination.internal.service.ConversationAiClient conversationAi;
    @MockitoBean com.example.swd392_se1907_aives.modules.examination.internal.service.AttemptGradingAiClient gradingAi;
    @Autowired com.example.swd392_se1907_aives.modules.examination.internal.service.AttemptGradingService grading;
    User student,teacher;Course course;Exam exam;ExamCandidate candidate;String token;
    @BeforeEach void setup(){
        student=user("STUDENT");teacher=user("LECTURER");token=token(student);
        course=new Course();course.setCode("V"+UUID.randomUUID().toString().substring(0,8));course.setName("Viva");course.setIsActive(true);course.setCreatedAt(OffsetDateTime.now());course=courses.saveAndFlush(course);
        var enrollment=new CourseEnrollment();enrollment.setId(new CourseEnrollmentId(course.getId(),student.getId()));enrollment.setEnrollmentStatus("ACTIVE");enrollment.setEnrolledAt(OffsetDateTime.now());enrollments.saveAndFlush(enrollment);
        exam=new Exam();exam.setCourseId(course.getId());exam.setCreatedBy(teacher.getId());exam.setTitle("Viva test");exam.setStatus("ACTIVE");exam.setSelectionMode("RANDOM");exam.setMainQuestionCount(1);exam.setMaxFollowupsPerQuestion(2);exam.setAnswerTimeLimitSeconds(120);exam.setSlotDurationMinutes(20);exam.setExamLanguage("vi");exam.setExamTimezone("Asia/Ho_Chi_Minh");exam.setCreatedAt(OffsetDateTime.now());exam.setUpdatedAt(OffsetDateTime.now());exam=exams.saveAndFlush(exam);
        candidate=new ExamCandidate();candidate.setCourseId(course.getId());candidate.setExamId(exam.getId());candidate.setStudentId(student.getId());candidate.setStatus("REGISTERED");candidate.setScheduledStart(OffsetDateTime.now().minusMinutes(1));candidate.setScheduledEnd(OffsetDateTime.now().plusMinutes(20));candidate.setCreatedAt(OffsetDateTime.now());candidate=candidates.saveAndFlush(candidate);
        addQuestion("Giải thích phân chia trách nhiệm trong kiến trúc phần mềm?");
        when(ai.configured()).thenReturn(true);when(ai.followupModel()).thenReturn("test-followup");when(ai.sttModel()).thenReturn("test-stt");when(ai.ttsModel()).thenReturn("test-tts");
        when(ai.followup(anyString())).thenReturn(new InterviewAiClient.Decision(false,"","Đủ rõ"));
    }
    @AfterAll static void cleanup()throws Exception{if(Files.exists(STORAGE))try(var paths=Files.walk(STORAGE)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}

    @Test void asksAdaptiveFollowupAndFinishesWithoutLeakingReferenceAnswers()throws Exception{
        when(ai.followup(anyString())).thenReturn(new InterviewAiClient.Decision(true,"Vì sao tách trách nhiệm giúp dễ bảo trì?","Cần làm rõ lý do"),new InterviewAiClient.Decision(false,"","Đủ rõ"));
        String id=start();JsonNode state=state(id);String prompt=prompt(state);
        var rubricSnapshot=json.readTree(attempts.findById(UUID.fromString(id)).orElseThrow().getRubricSnapshot());
        assertThat(rubricSnapshot.path("maxScore").asInt()).isEqualTo(10);assertThat(rubricSnapshot.path("criteria").get(0).path("maxPoints").asInt()).isEqualTo(10);assertThat(assigned.findByAttemptIdOrderBySequenceNo(UUID.fromString(id)).getFirst().getRubricSnapshot()).isNull();
        assertThat(state.toString()).doesNotContain("PRIVATE-REFERENCE");assertThat(state.path("prompt").path("deadline").isNull()).isTrue();
        begin(id,prompt);JsonNode followup=answer(id,prompt,"Các thành phần cần tách biệt.",200);
        assertThat(followup.path("prompt").path("followupNumber").asInt()).isEqualTo(1);assertThat(followup.path("turns")).hasSize(3);
        String next=prompt(followup);begin(id,next);JsonNode finished=answer(id,next,"Có thể thay đổi từng thành phần riêng.",200);
        assertThat(finished.path("status").asText()).isEqualTo("COMPLETED");assertThat(finished.path("turns")).hasSize(4);
        verify(ai,times(2)).followup(contains("PRIVATE-REFERENCE"));
        mvc.perform(post("/api/v1/interviews/"+id+"/answers").header("Authorization",token).contentType("application/json").content(body(Map.of("promptId",prompt,"text","Duplicate")))).andExpect(status().isConflict());
    }
    @Test void maxFollowupSettingIsEnforcedWithoutCallingAiAgain()throws Exception{
        exam.setMaxFollowupsPerQuestion(1);exams.saveAndFlush(exam);
        when(ai.followup(anyString())).thenReturn(new InterviewAiClient.Decision(true,"Bạn có thể nêu ví dụ cụ thể?","Làm rõ"));
        String id=start(),prompt=prompt(state(id));begin(id,prompt);var followup=answer(id,prompt,"Một ví dụ.",200);
        String second=prompt(followup);begin(id,second);assertThat(answer(id,second,"Ví dụ lớp truy cập dữ liệu riêng.",200).path("status").asText()).isEqualTo("COMPLETED");
        verify(ai,times(1)).followup(anyString());
    }
    @Test void followupBudgetAppliesToWholeStudentAttemptAndDoesNotResetOnNextMainQuestion()throws Exception{
        exam.setMainQuestionCount(2);exam.setMaxFollowupsPerStudent(1);exam.setMaxFollowupsPerQuestion(10);exams.saveAndFlush(exam);addQuestion("Câu chính thứ hai?");
        when(ai.followup(anyString())).thenReturn(new InterviewAiClient.Decision(true,"Bạn có thể giải thích thêm?","Chưa rõ"));
        String id=start(),first=prompt(state(id));begin(id,first);var followup=answer(id,first,"Câu trả lời chính.",200);
        assertThat(followup.path("followupsUsed").asInt()).isEqualTo(1);assertThat(state(id).path("maxFollowups").asInt()).isEqualTo(1);
        String deep=prompt(followup);begin(id,deep);var next=answer(id,deep,"Câu trả lời đào sâu.",200);
        assertThat(next.path("prompt").path("sequenceNo").asInt()).isEqualTo(2);assertThat(next.path("followupsUsed").asInt()).isEqualTo(1);
        String second=prompt(next);begin(id,second);var completed=answer(id,second,"Trả lời câu chính tiếp theo.",200);
        assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");assertThat(completed.path("followupsUsed").asInt()).isEqualTo(1);
        verify(ai,times(1)).followup(anyString());
    }
    @Test void perQuestionLimitResetsForNextMainButCannotExceedTotalBudget()throws Exception{
        exam.setMainQuestionCount(2);exam.setMaxFollowupsPerStudent(4);exam.setMaxFollowupsPerQuestion(1);exams.saveAndFlush(exam);addQuestion("Câu chính tiếp theo?");
        when(ai.followup(anyString())).thenReturn(new InterviewAiClient.Decision(true,"Hãy giải thích lập luận của em?","Cần làm rõ"));
        String id=start();var current=state(id);
        assertThat(current.path("maxFollowupsPerQuestion").asInt()).isEqualTo(1);
        for(int sequence=1;sequence<=2;sequence++){
            String main=prompt(current);begin(id,main);var followup=answer(id,main,"Em đưa ra lập luận này.",200);
            assertThat(followup.path("prompt").path("sequenceNo").asInt()).isEqualTo(sequence);
            assertThat(followup.path("prompt").path("followupNumber").asInt()).isEqualTo(1);
            String deep=prompt(followup);begin(id,deep);current=answer(id,deep,"Em giải thích thêm căn cứ.",200);
        }
        assertThat(current.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(current.path("followupsUsed").asInt()).isEqualTo(2);
        verify(ai,times(2)).followup(anyString());
    }
    @Test void zeroPerQuestionLimitSkipsFollowupsEvenWithTotalBudgetRemaining()throws Exception{
        exam.setMaxFollowupsPerStudent(3);exam.setMaxFollowupsPerQuestion(0);exams.saveAndFlush(exam);
        String id=start(),main=prompt(state(id));begin(id,main);
        assertThat(answer(id,main,"Câu trả lời.",200).path("status").asText()).isEqualTo("COMPLETED");
        verify(ai,never()).followup(anyString());
    }
    @Test void deadlinesSurviveReloadAndStaleTimersCannotSkipNextQuestion()throws Exception{
        exam.setMainQuestionCount(2);exams.saveAndFlush(exam);addQuestion("Câu hỏi thứ hai về kiến trúc?");
        String id=start();var initial=state(id);assertThat(initial.path("turns")).hasSize(1);
        String prompt=prompt(initial);JsonNode begun=begin(id,prompt);String deadline=begun.path("prompt").path("deadline").asText();
        assertThat(OffsetDateTime.parse(begin(id,prompt).path("prompt").path("deadline").asText()).toInstant().toEpochMilli()).isEqualTo(OffsetDateTime.parse(deadline).toInstant().toEpochMilli());
        var turn=turns.findById(UUID.fromString(prompt)).orElseThrow();turn.setResponseDeadlineAt(OffsetDateTime.now().minusSeconds(1));turns.saveAndFlush(turn);
        store.advanceSystem(UUID.fromString(id));var next=state(id);assertThat(prompt(next)).isNotEqualTo(prompt);assertThat(next.path("completedQuestions").asInt()).isEqualTo(1);
        mvc.perform(post("/api/v1/interviews/"+id+"/timeout").header("Authorization",token).contentType("application/json").content(body(Map.of("promptId",prompt))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.completedQuestions").value(1));
        answer(id,prompt,"Too late",409);
    }
    @Test void providerFailureKeepsAnswerAndRetryDoesNotDuplicateIt()throws Exception{
        when(ai.followup(anyString())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"OpenAI quota"));
        String id=start(),prompt=prompt(state(id));begin(id,prompt);var failed=answer(id,prompt,"Câu trả lời đã nộp.",200);
        assertThat(failed.path("needsContinuation").asBoolean()).isTrue();assertThat(failed.path("error").asText()).contains("quota");
        doReturn(new InterviewAiClient.Decision(false,"","Hoàn tất")).when(ai).followup(anyString());
        mvc.perform(post("/api/v1/interviews/"+id+"/continue").header("Authorization",token).contentType("application/json").content(body(Map.of("answerTurnId",failed.path("answerTurnId").asText()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(turns.findByAttemptQuestionIdOrderByTurnNo(assigned.findByAttemptIdOrderBySequenceNo(UUID.fromString(id)).getFirst().getId())).hasSize(2);
    }
    @Test void audioIsTranscribedStoredAndReplayableAndSpeechIsCached()throws Exception{
        byte[] wav=wav();when(ai.speech(anyString())).thenReturn(wav);when(ai.transcribe(any(),anyString(),anyString())).thenReturn("Câu trả lời bằng giọng nói.");
        String id=start(),prompt=prompt(state(id));
        for(int i=0;i<2;i++)mvc.perform(get("/api/v1/interviews/"+id+"/speech/"+prompt).header("Authorization",token)).andExpect(status().isOk()).andExpect(content().bytes(wav));
        verify(ai,times(1)).speech(anyString());begin(id,prompt);
        var response=json.readTree(mvc.perform(multipart("/api/v1/interviews/"+id+"/audio-answers").file(new MockMultipartFile("file","answer.wav","audio/wav",wav)).param("promptId",prompt).header("Authorization",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED")).andReturn().getResponse().getContentAsString());
        var answer=response.path("turns").get(1);assertThat(answer.path("text").asText()).isEqualTo("Câu trả lời bằng giọng nói.");assertThat(answer.path("hasAudio").asBoolean()).isTrue();
        mvc.perform(get("/api/v1/interviews/"+id+"/audio/"+answer.path("id").asText()).header("Authorization",token)).andExpect(status().isOk()).andExpect(content().bytes(wav));
    }
    @Test void failedSttCanBeRetriedAndOtherStudentsCannotReadTheAttempt()throws Exception{
        String id=start(),prompt=prompt(state(id));begin(id,prompt);
        when(ai.transcribe(any(),anyString(),anyString())).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY,"STT failed"));
        mvc.perform(multipart("/api/v1/interviews/"+id+"/audio-answers").file(new MockMultipartFile("file","answer.wav","audio/wav",wav())).param("promptId",prompt).header("Authorization",token)).andExpect(status().isBadGateway());
        assertThat(prompt(state(id))).isEqualTo(prompt);
        doReturn("Thử lại thành công.").when(ai).transcribe(any(),anyString(),anyString());
        mvc.perform(multipart("/api/v1/interviews/"+id+"/audio-answers").file(new MockMultipartFile("file","answer.wav","audio/wav",wav())).param("promptId",prompt).header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(state(id).path("turns")).hasSize(2);
        mvc.perform(get("/api/v1/interviews/"+id).header("Authorization",token(user("STUDENT")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/interviews/"+id).header("Authorization",token(teacher))).andExpect(status().isForbidden());
    }
    @Test void simultaneousAnswersAndStartsAreSerialized()throws Exception{
        var executor=Executors.newFixedThreadPool(2);
        try {
            var starts=executor.invokeAll(List.<Callable<org.springframework.test.web.servlet.MvcResult>>of(()->mvc.perform(post("/api/v1/attempts/start/"+candidate.getId()).header("Authorization",token)).andReturn(),()->mvc.perform(post("/api/v1/attempts/start/"+candidate.getId()).header("Authorization",token)).andReturn()));
            var codes=List.of(starts.get(0).get().getResponse().getStatus(),starts.get(1).get().getResponse().getStatus());assertThat(codes).containsOnly(200);
            assertThat(json.readTree(starts.get(0).get().getResponse().getContentAsString()).path("attemptId").asText()).isEqualTo(json.readTree(starts.get(1).get().getResponse().getContentAsString()).path("attemptId").asText());
            assertThat(attempts.findByCandidateIdOrderByAttemptNoAsc(candidate.getId())).hasSize(1);
            String id=attempts.findByCandidateIdOrderByAttemptNoAsc(candidate.getId()).getFirst().getId().toString();String prompt=prompt(state(id));begin(id,prompt);
            var submissions=executor.invokeAll(List.of(()->postAnswer(id,prompt,"A"),()->postAnswer(id,prompt,"B")));
            assertThat(List.of(submissions.get(0).get(),submissions.get(1).get())).containsExactlyInAnyOrder(200,409);
        }finally{executor.shutdownNow();}
    }
    @Test void scheduledEndClosesRemainingQuestionsAndTicketRequiresActiveAnswer()throws Exception{
        String id=start(),prompt=prompt(state(id));
        mvc.perform(post("/api/v1/interviews/"+id+"/live-ticket").header("Authorization",token).contentType("application/json").content(body(Map.of("promptId",prompt)))).andExpect(status().isConflict());
        begin(id,prompt);
        mvc.perform(post("/api/v1/interviews/"+id+"/live-ticket").header("Authorization",token).contentType("application/json").content(body(Map.of("promptId",prompt)))).andExpect(status().isOk()).andExpect(jsonPath("$.ticket").isString());
        candidate.setScheduledEnd(OffsetDateTime.now().minusSeconds(1));candidates.saveAndFlush(candidate);store.advanceSystem(UUID.fromString(id));
        assertThat(state(id).path("status").asText()).isEqualTo("COMPLETED");
    }
    @Test void retryingStartReturnsTheSameAttemptAndDoesNotResetTheAnswerDeadline()throws Exception{
        String id=start(),prompt=prompt(state(id));begin(id,prompt);
        var deadline=state(id).path("prompt").path("deadline").asText();
        var startedAt=attempts.findById(UUID.fromString(id)).orElseThrow().getStartedAt();
        assertThat(start()).isEqualTo(id);
        assertThat(state(id).path("prompt").path("deadline").asText()).isEqualTo(deadline);
        assertThat(attempts.findById(UUID.fromString(id)).orElseThrow().getStartedAt()).isEqualTo(startedAt);
        assertThat(attempts.findByCandidateIdOrderByAttemptNoAsc(candidate.getId())).hasSize(1);
    }
    @Test void aLateAiFollowupCannotReopenAnAttemptCancelledWhileAiWasThinking()throws Exception{
        String id=start(),prompt=prompt(state(id));begin(id,prompt);
        when(ai.followup(anyString())).thenAnswer(invocation->{
            var attempt=attempts.findById(UUID.fromString(id)).orElseThrow();attempt.setStatus("ABORTED");attempt.setEndedAt(OffsetDateTime.now());attempts.saveAndFlush(attempt);
            var schedule=candidates.findById(candidate.getId()).orElseThrow();schedule.setStatus("CANCELLED");candidates.saveAndFlush(schedule);
            return new InterviewAiClient.Decision(true,"Không được hỏi sau khi hủy lịch","Đã hủy");
        });
        var response=answer(id,prompt,"Câu trả lời được giữ lại.",200);
        assertThat(response.path("status").asText()).isEqualTo("ABORTED");assertThat(response.path("prompt").isNull()).isTrue();
        assertThat(response.path("turns")).hasSize(2);assertThat(response.path("followupsUsed").asInt()).isZero();
    }
    @Test void loadingTranscriptDoesNotAddOneDatabaseQueryPerCompletedQuestion()throws Exception{
        var statistics=entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean enabled=statistics.isStatisticsEnabled();statistics.setStatisticsEnabled(true);
        try{
            String id=start();state(id);statistics.clear();state(id);
            long baseline=statistics.getPrepareStatementCount();
            for(int i=2;i<=20;i++){
                var question=new com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.AttemptQuestion();
                question.setAttemptId(UUID.fromString(id));question.setExamId(exam.getId());question.setQuestionId(UUID.randomUUID());
                question.setSequenceNo(i);question.setQuestionTextSnapshot("Historical question "+i);question.setSelectionMode("RANDOM");
                question.setStatus("FINISHED");question.setCreatedAt(OffsetDateTime.now());question=assigned.saveAndFlush(question);
                var turn=new com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.InterviewTurn();
                turn.setAttemptQuestionId(question.getId());turn.setTurnNo(1);turn.setSpeaker("AI");turn.setKind("MAIN_QUESTION");
                turn.setFollowupNumber(0);turn.setTextContent("Historical question "+i);turn.setSpeechStatus("NONE");turn.setCreatedAt(OffsetDateTime.now());turns.saveAndFlush(turn);
            }
            statistics.clear();var response=state(id);
            assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(baseline+1);
            assertThat(response.path("turns")).hasSize(20);
            assertThat(response.path("completedQuestions").asInt()).isEqualTo(19);
            for(int i=0;i<20;i++)assertThat(response.path("turns").get(i).path("sequenceNo").asInt()).isEqualTo(i+1);
        }finally{statistics.setStatisticsEnabled(enabled);}
    }
    private int postAnswer(String id,String prompt,String text)throws Exception{return mvc.perform(post("/api/v1/interviews/"+id+"/answers").header("Authorization",token).contentType("application/json").content(body(Map.of("promptId",prompt,"text",text)))).andReturn().getResponse().getStatus();}
    private JsonNode answer(String id,String prompt,String text,int expected)throws Exception{return json.readTree(mvc.perform(post("/api/v1/interviews/"+id+"/answers").header("Authorization",token).contentType("application/json").content(body(Map.of("promptId",prompt,"text",text))))
        .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString());}
    private JsonNode begin(String id,String prompt)throws Exception{return json.readTree(mvc.perform(post("/api/v1/interviews/"+id+"/begin").header("Authorization",token).contentType("application/json").content(body(Map.of("promptId",prompt))))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
    private JsonNode state(String id)throws Exception{return json.readTree(mvc.perform(get("/api/v1/interviews/"+id).header("Authorization",token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
    private String prompt(JsonNode state){return state.path("prompt").path("id").asText();}
    private String body(Object value)throws Exception{return json.writeValueAsString(value);}
    private String start()throws Exception{return json.readTree(mvc.perform(post("/api/v1/attempts/start/"+candidate.getId()).header("Authorization",token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("attemptId").asText();}
    @Test void gradesAllQuestionsOnceAfterCompletionUsingTheAttemptRubric()throws Exception {
        excludeEarlierGradingJobs();
        addQuestion("Nêu ví dụ áp dụng nguyên tắc phân chia trách nhiệm?");
        exam.setMainQuestionCount(2);exam.setMaxFollowupsPerQuestion(0);exam.setMaxFollowupsPerStudent(0);exams.saveAndFlush(exam);
        String id=start();
        grading.processPending();verifyNoInteractions(gradingAi);
        var first=state(id);begin(id,prompt(first));
        var second=answer(id,prompt(first),"EVIDENCE-FIRST-ANSWER",200);
        grading.processPending();verifyNoInteractions(gradingAi);
        begin(id,prompt(second));
        var completed=answer(id,prompt(second),"EVIDENCE-SECOND-ANSWER",200);
        assertThat(completed.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(attempts.findById(UUID.fromString(id)).orElseThrow().getGradingStatus()).isEqualTo("PENDING");
        assertThat(assigned.findByAttemptIdOrderBySequenceNo(UUID.fromString(id))).allMatch(q->q.getRubricSnapshot()==null);
        when(gradingAi.grade(anyString())).thenReturn(json.readTree("{\"feedback\":\"Đánh giá toàn bộ hai câu trả lời\",\"criteria\":[{\"sortOrder\":1,\"score\":8,\"feedback\":\"Lập luận tốt trên cả hai câu\"}]}"));
        grading.processPending();
        var evidence=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(gradingAi).grade(evidence.capture());
        var payload=json.readTree(evidence.getValue());
        assertThat(payload.path("wholeAttempt")).hasSize(2);
        assertThat(evidence.getValue()).contains("EVIDENCE-FIRST-ANSWER","EVIDENCE-SECOND-ANSWER","PRIVATE-REFERENCE");
        mvc.perform(get("/api/v1/attempts/"+id+"/grading").header("Authorization",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.result.totalScore").value(8)).andExpect(jsonPath("$.result.maxScore").value(10))
            .andExpect(jsonPath("$.result.criteria.length()").value(1));
        grading.processPending();verify(gradingAi,times(1)).grade(anyString());
        mvc.perform(get("/api/v1/attempts/"+id+"/grading").header("Authorization",token(user("STUDENT")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/attempts/"+id+"/grading").header("Authorization",token(user("LECTURER")))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/attempts/"+id+"/grading/retry").header("Authorization",token)).andExpect(status().isConflict());
    }

    @Test void invalidAiGradeFailsWithoutPersistingPointsAndCanBeRetried()throws Exception {
        excludeEarlierGradingJobs();
        exam.setMaxFollowupsPerQuestion(0);exam.setMaxFollowupsPerStudent(0);exams.saveAndFlush(exam);
        String id=start();var first=state(id);
        mvc.perform(post("/api/v1/attempts/"+id+"/grading/retry").header("Authorization",token)).andExpect(status().isConflict());
        begin(id,prompt(first));answer(id,prompt(first),"Đáp án đã nộp",200);
        when(gradingAi.grade(anyString())).thenReturn(json.readTree("{\"feedback\":\"Điểm sai\",\"criteria\":[{\"sortOrder\":1,\"score\":99,\"feedback\":\"Vượt thang điểm\"}]}"));
        grading.processPending();
        var attempt=attempts.findById(UUID.fromString(id)).orElseThrow();
        assertThat(attempt.getGradingStatus()).isEqualTo("FAILED");assertThat(attempt.getGradingResult()).isNull();
        mvc.perform(post("/api/v1/attempts/"+id+"/grading/retry").header("Authorization",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"));
        when(gradingAi.grade(anyString())).thenReturn(json.readTree("{\"feedback\":\"Đã đánh giá toàn bài\",\"criteria\":[{\"sortOrder\":1,\"score\":7,\"feedback\":\"Đạt yêu cầu\"}]}"));
        grading.processPending();
        mvc.perform(get("/api/v1/attempts/"+id+"/grading").header("Authorization",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.totalScore").value(7));
    }

    @Test void streamingUsesPriorAnswersAndRetriesAreIdempotent() throws Exception {
        when(conversationAi.stream(anyString(), any())).thenAnswer(invocation -> {
            java.util.function.Consumer<String> sink = invocation.getArgument(1);
            sink.accept("Cảm ơn bạn. "); sink.accept("Bạn giải thích thêm vì sao?");
            return new InterviewAiClient.Decision(true, "Cảm ơn bạn. Bạn giải thích thêm vì sao?", "Cần làm rõ");
        });
        String id = start(), first = prompt(state(id));
        String response = stream(id, first, "FIRST-EVIDENCE");
        assertThat(response).contains("event:saved", "event:delta", "event:complete", "Cảm ơn bạn.");
        assertThat(response).doesNotContain("PRIVATE-REFERENCE");
        var next = state(id);
        assertThat(next.path("turns")).hasSize(3);
        stream(id, first, "FIRST-EVIDENCE");
        assertThat(state(id).path("turns")).hasSize(3);
        verify(conversationAi, times(1)).stream(anyString(), any());
        doReturn(new InterviewAiClient.Decision(false, "", "Đủ rõ")).when(conversationAi).stream(anyString(), any());
        stream(id, prompt(next), "SECOND-EVIDENCE");
        var contexts = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(conversationAi, times(2)).stream(contexts.capture(), any());
        assertThat(contexts.getAllValues().getLast()).contains("FIRST-EVIDENCE", "SECOND-EVIDENCE", "rubric");
        assertThat(state(id).path("status").asText()).isEqualTo("COMPLETED");
    }

    @Test void streamingFailureKeepsAnswerAndSameRequestCanResume() throws Exception {
        when(conversationAi.stream(anyString(), any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI timeout"));
        String id = start(), first = prompt(state(id));
        assertThat(stream(id, first, "RETAINED-ANSWER")).contains("event:error", "AI timeout");
        assertThat(state(id).path("needsContinuation").asBoolean()).isTrue();
        assertThat(state(id).path("turns")).hasSize(2);
        doReturn(new InterviewAiClient.Decision(false, "", "Đủ rõ")).when(conversationAi).stream(anyString(), any());
        assertThat(stream(id, first, "RETAINED-ANSWER")).contains("event:complete");
        assertThat(state(id).path("turns")).hasSize(2);
        assertThat(state(id).path("status").asText()).isEqualTo("COMPLETED");
    }

    @Test void streamAndFinishRejectAnonymousOtherOwnersAndInvalidText() throws Exception {
        String id = start(), first = prompt(state(id)), url = "/api/v1/interviews/" + id;
        String payload = body(Map.of("promptId", first, "text", "answer"));
        mvc.perform(post(url + "/turns/stream").contentType("application/json").content(payload)).andExpect(status().isUnauthorized());
        mvc.perform(post(url + "/turns/stream").header("Authorization", token(user("STUDENT"))).contentType("application/json").content(payload)).andExpect(status().isForbidden());
        for (String invalid : List.of(" ", "x".repeat(12001)))
            mvc.perform(post(url + "/turns/stream").header("Authorization", token).contentType("application/json")
                    .content(body(Map.of("promptId", first, "text", invalid)))).andExpect(status().isBadRequest());
        mvc.perform(post(url + "/finish")).andExpect(status().isUnauthorized());
        mvc.perform(post(url + "/finish").header("Authorization", token(user("STUDENT")))).andExpect(status().isForbidden());
        assertThat(state(id).path("turns")).hasSize(1);
        verifyNoInteractions(conversationAi);
    }

    @Test void finishPreservesTranscriptAndGradesExactlyOnceWithUnansweredQuestions() throws Exception {
        excludeEarlierGradingJobs();
        exam.setMainQuestionCount(2); exams.saveAndFlush(exam); addQuestion("Câu chưa trả lời?");
        when(conversationAi.stream(anyString(), any())).thenReturn(new InterviewAiClient.Decision(false, "", "Đủ rõ"));
        String id = start(); stream(id, prompt(state(id)), "FINISH-EVIDENCE");
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/v1/interviews/" + id + "/finish").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        assertThat(state(id).toString()).contains("FINISH-EVIDENCE");
        when(gradingAi.grade(anyString())).thenReturn(json.readTree("{\"feedback\":\"Đã chấm\",\"criteria\":[{\"sortOrder\":1,\"score\":6,\"feedback\":\"Còn thiếu ý\"}]}"));
        grading.processPending(); grading.processPending();
        verify(gradingAi, times(1)).grade(contains("FINISH-EVIDENCE"));
        mvc.perform(get("/api/v1/attempts/" + id + "/grading").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.totalScore").value(6));
    }

    @Test void contextIncludesEarlierMainQuestions() throws Exception {
        exam.setMainQuestionCount(2); exams.saveAndFlush(exam); addQuestion("Câu chính thứ hai?");
        when(conversationAi.stream(anyString(), any())).thenReturn(new InterviewAiClient.Decision(false, "", "Đủ rõ"));
        String id = start(); stream(id, prompt(state(id)), "EARLIER-MAIN-ANSWER");
        stream(id, prompt(state(id)), "CURRENT-MAIN-ANSWER");
        var context = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(conversationAi, times(2)).stream(context.capture(), any());
        assertThat(context.getAllValues().getLast()).contains("EARLIER-MAIN-ANSWER", "CURRENT-MAIN-ANSWER");
    }

    private String stream(String id, String prompt, String text) throws Exception {
        var result = mvc.perform(post("/api/v1/interviews/" + id + "/turns/stream")
                .header("Authorization", token).contentType("application/json")
                .content(body(Map.of("promptId", prompt, "text", text))))
                .andExpect(request().asyncStarted()).andReturn();
        result.getAsyncResult(10000);
        return mvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test void concurrentStreamRetryDoesNotCallAiOrAppendAnAnswerTwice() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        when(conversationAi.stream(anyString(), any())).thenAnswer(invocation -> {
            entered.countDown(); assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            return new InterviewAiClient.Decision(false, "", "Đủ rõ");
        });
        String id = start(), first = prompt(state(id));
        String payload = body(Map.of("promptId", first, "text", "CONCURRENT-EVIDENCE"));
        var result = mvc.perform(post("/api/v1/interviews/" + id + "/turns/stream").header("Authorization", token)
                .contentType("application/json").content(payload)).andExpect(request().asyncStarted()).andReturn();
        try {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            mvc.perform(post("/api/v1/interviews/" + id + "/turns/stream").header("Authorization", token)
                    .contentType("application/json").content(payload)).andExpect(status().isConflict());
        } finally { release.countDown(); }
        result.getAsyncResult(10000); mvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        assertThat(state(id).path("turns")).hasSize(2);
        verify(conversationAi, times(1)).stream(anyString(), any());
    }

    @Test void finishingDuringStreamNeverReopensTheAttemptWhenAiReturnsLate() throws Exception {
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        when(conversationAi.stream(anyString(), any())).thenAnswer(invocation -> {
            entered.countDown(); assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
            return new InterviewAiClient.Decision(true, "Vì sao câu này không được hỏi nữa?", "Late");
        });
        String id = start(), first = prompt(state(id));
        var result = mvc.perform(post("/api/v1/interviews/" + id + "/turns/stream").header("Authorization", token)
                .contentType("application/json").content(body(Map.of("promptId", first, "text", "KEPT-WHEN-FINISHING"))))
                .andExpect(request().asyncStarted()).andReturn();
        try {
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            mvc.perform(post("/api/v1/interviews/" + id + "/finish").header("Authorization", token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        } finally { release.countDown(); }
        result.getAsyncResult(10000); mvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        var finished = state(id);
        assertThat(finished.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(finished.path("turns")).hasSize(2);
        assertThat(finished.toString()).contains("KEPT-WHEN-FINISHING").doesNotContain("Vì sao câu này không được hỏi nữa?");
    }

    private void excludeEarlierGradingJobs() {
        for(var previous:attempts.findAll())if("PENDING".equals(previous.getGradingStatus())) {
            previous.setGradingStatus(null);attempts.saveAndFlush(previous);
        }
    }
    private String token(User u){return "Bearer "+jwt.generateToken(u.getId().toString(),u.getEmail(),u.getRole());}
    private User user(String role){var value=new User();value.setFullName("Viva tester");value.setEmail(UUID.randomUUID()+"@example.com");value.setPasswordHash("unused");value.setIsActive(true);value.setRole(role);value.setCreatedAt(OffsetDateTime.now());value.setUpdatedAt(OffsetDateTime.now());return users.saveAndFlush(value);}
    private void addQuestion(String content){
        var rubric=new Rubric();rubric.setCourseId(course.getId());rubric.setCreatedBy(teacher.getId());rubric.setName("Private rubric");rubric.setMaxScore(BigDecimal.TEN);rubric.setStatus("PUBLISHED");rubric.setCreatedAt(OffsetDateTime.now());rubric.setUpdatedAt(OffsetDateTime.now());rubric=rubrics.saveAndFlush(rubric);
        exam.setRubricId(rubric.getId());exam=exams.saveAndFlush(exam);
        var criterion=new RubricCriterion();criterion.setRubricId(rubric.getId());criterion.setCriterionName("Giải thích");criterion.setMaxPoints(BigDecimal.TEN);criterion.setSortOrder(1);criteria.saveAndFlush(criterion);
        var q=new Question();q.setCourseId(course.getId());q.setCreatedBy(teacher.getId());q.setContent(content);q.setReferenceAnswer("PRIVATE-REFERENCE");q.setDifficulty("MEDIUM");q.setSourceType("MANUAL");q.setStatus("APPROVED");q.setApprovedBy(teacher.getId());q.setApprovedAt(OffsetDateTime.now());q.setCreatedAt(OffsetDateTime.now());q.setUpdatedAt(OffsetDateTime.now());q=questions.saveAndFlush(q);
        var item=new ExamQuestionPool();item.setId(new ExamQuestionPoolId(exam.getId(),q.getId()));item.setCourseId(course.getId());item.setSelectionWeight(1);item.setAddedAt(OffsetDateTime.now());pool.saveAndFlush(item);
    }
    static byte[] wav(){ByteBuffer b=ByteBuffer.allocate(4844).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(4836).put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16).putShort((short)1).putShort((short)1).putInt(24000).putInt(48000).putShort((short)2).putShort((short)16).put("data".getBytes(StandardCharsets.US_ASCII)).putInt(4800);return b.array();}
}
