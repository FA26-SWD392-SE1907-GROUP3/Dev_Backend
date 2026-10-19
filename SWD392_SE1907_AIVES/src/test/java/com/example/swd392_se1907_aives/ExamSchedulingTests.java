package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPool;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPoolId;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.AttemptQuestionRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamQuestionPoolRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRepository;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;
import com.example.swd392_se1907_aives.modules.identity.internal.repository.UserRepository;
import com.example.swd392_se1907_aives.modules.identity.internal.service.JwtService;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Course;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollment;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollmentId;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseLecturer;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseLecturerId;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Question;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Rubric;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseEnrollmentRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseLecturerRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionImportBatchRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionReviewRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricRepository;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:scheduling;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.interview.timeouts-enabled=false",
    "app.ai.api-key=","JWT_SECRET=scheduling_test_secret_at_least_32_characters_long"})
@AutoConfigureMockMvc
class ExamSchedulingTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JwtService jwt;
    @Autowired UserRepository users; @Autowired CourseRepository courses; @Autowired CourseLecturerRepository lecturers;
    @Autowired CourseEnrollmentRepository enrollments; @Autowired ExamRepository exams; @Autowired ExamCandidateRepository candidates;
    @Autowired ExamAttemptRepository attempts;
    @Autowired QuestionRepository questions;@Autowired RubricRepository rubrics;@Autowired QuestionReviewRepository reviews;@Autowired ExamQuestionPoolRepository pool;
    @Autowired AttemptQuestionRepository assigned;
    @Autowired QuestionImportBatchRepository imports;
    User student,teacher; Course course; Exam exam; String token;
    @BeforeEach void setup(){
        student=user("STUDENT");teacher=user("LECTURER");token=token(teacher);
        course=new Course();course.setCode("S"+UUID.randomUUID().toString().substring(0,8));course.setName("Scheduling");course.setIsActive(true);course.setCreatedAt(OffsetDateTime.now());course=courses.saveAndFlush(course);
        var lecturer=new CourseLecturer();lecturer.setId(new CourseLecturerId(course.getId(),teacher.getId()));lecturer.setAssignedAt(OffsetDateTime.now());lecturers.saveAndFlush(lecturer);
        exam=new Exam();exam.setCourseId(course.getId());exam.setCreatedBy(teacher.getId());exam.setTitle("Scheduling test");exam.setStatus("ACTIVE");exam.setSelectionMode("RANDOM");exam.setMainQuestionCount(1);exam.setMaxFollowupsPerQuestion(2);exam.setAnswerTimeLimitSeconds(120);exam.setSlotDurationMinutes(20);exam.setExamLanguage("vi");exam.setExamTimezone("Asia/Ho_Chi_Minh");exam.setCreatedAt(OffsetDateTime.now());exam.setUpdatedAt(OffsetDateTime.now());exam=exams.saveAndFlush(exam);
        enrollment("ACTIVE");
    }
    @Test void administratorEnrollsStudentBeforeTeacherCanScheduleEvenWithLegacyEnrollmentFlag()throws Exception{
        enrollments.deleteById(enrollmentId());
        assign(token,student.getEmail(),false,400);
        assertThat(enrollments.existsById(enrollmentId())).isFalse();
        assertThat(candidates.existsByExamIdAndStudentId(exam.getId(),student.getId())).isFalse();
        assign(token,student.getEmail(),true,400);
        mvc.perform(post("/api/v1/courses/"+course.getId()+"/students").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(Map.of("email",student.getEmail())))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/courses/"+course.getId()+"/students").header("Authorization",token(user("ADMIN"))).contentType("application/json").content(json.writeValueAsString(Map.of("email",student.getEmail())))).andExpect(status().isOk());
        String id=assign(token,student.getEmail().toUpperCase(Locale.ROOT),false,200);
        assertThat(enrollments.findById(enrollmentId()).orElseThrow().getEnrollmentStatus()).isEqualTo("ACTIVE");
        mvc.perform(get("/api/v1/student/exams").header("Authorization",token(student)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/v1/courses").header("Authorization",token(student)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(course.getId().toString()));
        assign(token,student.getEmail(),true,409);
        assertThat(candidates.findByStudentId(student.getId())).hasSize(1);
    }
    @Test void teacherCannotReactivateDroppedEnrollmentWhileScheduling()throws Exception{
        enrollment("DROPPED");assign(token,student.getEmail(),false,400);
        assign(token,student.getEmail(),true,400);
        assertThat(enrollments.findById(enrollmentId()).orElseThrow().getEnrollmentStatus()).isEqualTo("DROPPED");
        assertThat(candidates.existsByExamIdAndStudentId(exam.getId(),student.getId())).isFalse();
    }
    @Test void enrolledStudentCanBeScheduledWithoutEnrollmentOption()throws Exception{
        enrollment("ACTIVE");assign(token,student.getEmail(),false,200);
    }
    @Test void unauthorizedAndInvalidAccountsCannotBeEnrolledByScheduling()throws Exception{
        enrollments.deleteById(enrollmentId());
        assign(token(user("LECTURER")),student.getEmail(),true,403);
        assign(token,teacher.getEmail(),true,400);
        student.setIsActive(false);users.saveAndFlush(student);assign(token,student.getEmail(),true,400);
        assign(token,"missing@example.com",true,404);
        assertThat(enrollments.existsById(enrollmentId())).isFalse();
    }
    @Test void invalidScheduleDoesNotLeaveAnEnrollmentBehind()throws Exception{
        enrollments.deleteById(enrollmentId());
        var body=new HashMap<>(body(student.getEmail(),true));body.put("scheduledEnd",OffsetDateTime.now().minusMinutes(5).toString());
        mvc.perform(post("/api/v1/exams/"+exam.getId()+"/candidates").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
        assertThat(enrollments.existsById(enrollmentId())).isFalse();
    }
    @Test void manualEnrollmentIsIdempotentAndPreservesEnrollmentDate()throws Exception{
        String admin=token(user("ADMIN"));
        for(int i=0;i<2;i++)mvc.perform(post("/api/v1/courses/"+course.getId()+"/students").header("Authorization",admin).contentType("application/json").content(json.writeValueAsString(Map.of("email",student.getEmail())))).andExpect(status().isOk());
        var original=enrollments.findById(enrollmentId()).orElseThrow().getEnrolledAt();
        assign(token,student.getEmail(),true,200);
        assertThat(enrollments.findById(enrollmentId()).orElseThrow().getEnrolledAt()).isEqualTo(original);
    }
    @Test void simultaneousAssignmentsCreateOneEnrollmentAndOneSchedule()throws Exception{
        var executor=Executors.newFixedThreadPool(2);
        try{
            Callable<Integer> task=()->mvc.perform(post("/api/v1/exams/"+exam.getId()+"/candidates").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(body(student.getEmail(),true)))).andReturn().getResponse().getStatus();
            var results=executor.invokeAll(List.of(task,task));assertThat(List.of(results.get(0).get(),results.get(1).get())).containsExactlyInAnyOrder(200,409);
            assertThat(candidates.findByStudentId(student.getId())).hasSize(1);
        }finally{executor.shutdownNow();}
    }
    @Test void invalidLoginProduces401ButValidStudentKeeps403ForAnotherStudentsExam()throws Exception{
        String id=assign(token,student.getEmail(),true,200);String url="/api/v1/attempts/start/"+id;
        mvc.perform(post(url)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401)).andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(post(url).header("Authorization","Bearer invalid-token")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(post(url).header("Authorization",token(teacher))).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(post(url).header("Authorization",token(user("STUDENT")))).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("sinh viên khác")));
        enrollment("DROPPED");
        mvc.perform(post(url).header("Authorization",token(student))).andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ghi danh")));
        assertThat(attempts.existsByCandidateId(UUID.fromString(id))).isFalse();
    }
    @Test void loopbackFrontendPreflightIsAllowedWithoutRelaxingOtherOrigins()throws Exception{
        String url="/api/v1/attempts/start/"+UUID.randomUUID();
        mvc.perform(options(url).header("Origin","http://127.0.0.1:3000").header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","Authorization, Content-Type"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://127.0.0.1:3000"));
        mvc.perform(options(url).header("Origin","https://untrusted.example").header("Access-Control-Request-Method","POST"))
            .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
    @Test void teacherReviewsEditsAndRemovesQuestionsBeforeOfficialUse()throws Exception{
        Rubric rubric=rubric("PUBLISHED");var request=questionBody(rubric.getId(),"Câu hỏi cần giáo viên kiểm tra");
        var created=json.readTree(mvc.perform(post("/api/v1/questions").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(request)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_REVIEW")).andReturn().getResponse().getContentAsString());
        String id=created.path("id").asText();
        mvc.perform(put("/api/v1/questions/"+id).header("Authorization",token).contentType("application/json").content(json.writeValueAsString(questionBody(rubric.getId(),"Câu hỏi đã chỉnh sửa"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mvc.perform(patch("/api/v1/questions/"+id+"/review?approve=true").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(questions.findById(UUID.fromString(id)).orElseThrow().getApprovedBy()).isEqualTo(teacher.getId());
        mvc.perform(delete("/api/v1/questions/"+id).header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
        assertThat(questions.findById(UUID.fromString(id))).isPresent();
        assertThat(reviews.findAll().stream().filter(r->r.getQuestionId().toString().equals(id)).toList()).hasSize(3);
    }
    @Test void studentsAdminsAndUnassignedTeachersCannotPerformAcademicDecisions()throws Exception{
        Question question=question("PENDING_REVIEW");String id=question.getId().toString();
        for(User actor:List.of(student,user("ADMIN"),user("LECTURER"))){
            String auth=token(actor);
            mvc.perform(patch("/api/v1/questions/"+id+"/review?approve=true").header("Authorization",auth)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/v1/questions/"+id).header("Authorization",auth)).andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/exams/"+exam.getId()+"/candidates").header("Authorization",auth)).andExpect(status().isForbidden());
            mvc.perform(patch("/api/v1/exams/"+exam.getId()+"/activate").header("Authorization",auth)).andExpect(status().isForbidden());
        }
        assertThat(questions.findById(question.getId()).orElseThrow().getStatus()).isEqualTo("PENDING_REVIEW");
    }
    @Test void editingAnApprovedQuestionWithdrawsApprovalAndRemovesItFromDraftExams()throws Exception{
        exam.setStatus("DRAFT");exams.saveAndFlush(exam);Question question=question("APPROVED");
        mvc.perform(post("/api/v1/exams/"+exam.getId()+"/questions").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(Map.of("questionId",question.getId())))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/questions/"+question.getId()).header("Authorization",token).contentType("application/json").content(json.writeValueAsString(questionBody(question.getRubricId(),"Nội dung mới phải duyệt lại"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        assertThat(pool.findByIdExamId(exam.getId())).isEmpty();
        assertThat(questions.findById(question.getId()).orElseThrow().getApprovedBy()).isNull();
        mvc.perform(post("/api/v1/exams/"+exam.getId()+"/questions").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(Map.of("questionId",question.getId())))).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/exams/"+exam.getId()+"/activate").header("Authorization",token)).andExpect(status().isBadRequest());
    }
    @Test void teacherManagesExamSettingsPoolAndProtectsQuestionsWhenActivated()throws Exception{
        exam.setStatus("DRAFT");exams.saveAndFlush(exam);Question question=question("APPROVED");String url="/api/v1/exams/"+exam.getId();
        mvc.perform(put(url).header("Authorization",token).contentType("application/json").content(json.writeValueAsString(Map.of("courseId",course.getId(),"title","Kỳ thi của giáo viên","mainQuestionCount",1,"maxFollowupsPerQuestion",3,"answerTimeLimitSeconds",90))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.maxFollowupsPerQuestion").value(3));
        mvc.perform(post(url+"/questions").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(Map.of("questionId",question.getId())))).andExpect(status().isOk());
        mvc.perform(get(url+"/questions").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete(url+"/questions/"+question.getId()).header("Authorization",token)).andExpect(status().isOk());assertThat(pool.findByIdExamId(exam.getId())).isEmpty();
        mvc.perform(post(url+"/questions").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(Map.of("questionId",question.getId())))).andExpect(status().isOk());
        mvc.perform(patch(url+"/activate").header("Authorization",token)).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/questions/"+question.getId()).header("Authorization",token)).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/questions/"+question.getId()).header("Authorization",token).contentType("application/json").content(json.writeValueAsString(questionBody(question.getRubricId(),"Sửa khi thi")))).andExpect(status().isConflict());
        mvc.perform(delete(url+"/questions/"+question.getId()).header("Authorization",token)).andExpect(status().isConflict());
    }
    @Test void teacherCanListRescheduleAndRemoveStudentsWithoutAdministrator()throws Exception{
        String id=assign(token,student.getEmail(),true,200),url="/api/v1/exams/"+exam.getId()+"/candidates";
        mvc.perform(get(url).header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].studentEmail").value(student.getEmail()));
        mvc.perform(get("/api/v1/courses/"+course.getId()+"/students").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(student.getId().toString()));
        mvc.perform(put(url+"/"+id).header("Authorization",token).contentType("application/json").content(json.writeValueAsString(Map.of("scheduledStart",OffsetDateTime.now().plusHours(1).toString(),"scheduledEnd",OffsetDateTime.now().plusHours(2).toString())))).andExpect(status().isOk());
        mvc.perform(delete(url+"/"+id).header("Authorization",token)).andExpect(status().isOk());
        assertThat(candidates.findById(UUID.fromString(id))).isEmpty();assertThat(enrollments.findById(enrollmentId())).isPresent();
    }
    @Test void teacherCannotRemoveOrRescheduleAStudentWhoHasStarted()throws Exception{
        Question question=question("APPROVED");var item=new ExamQuestionPool();item.setId(new ExamQuestionPoolId(exam.getId(),question.getId()));item.setCourseId(course.getId());item.setSelectionWeight(1);item.setAddedAt(OffsetDateTime.now());pool.saveAndFlush(item);
        String id=assign(token,student.getEmail(),true,200),url="/api/v1/exams/"+exam.getId()+"/candidates/"+id;
        mvc.perform(post("/api/v1/attempts/start/"+id).header("Authorization",token(student))).andExpect(status().isOk());
        mvc.perform(delete(url).header("Authorization",token)).andExpect(status().isConflict());
        mvc.perform(put(url).header("Authorization",token).contentType("application/json").content(json.writeValueAsString(Map.of("scheduledStart",OffsetDateTime.now().plusHours(1).toString(),"scheduledEnd",OffsetDateTime.now().plusHours(2).toString())))).andExpect(status().isConflict());
        assertThat(attempts.existsByCandidateId(UUID.fromString(id))).isTrue();
    }
    @Test void teacherMustPublishRubricBeforeApprovingQuestion()throws Exception{
        Question question=question("PENDING_REVIEW");var rubric=rubrics.findById(question.getRubricId()).orElseThrow();rubric.setStatus("DRAFT");rubrics.saveAndFlush(rubric);
        mvc.perform(patch("/api/v1/questions/"+question.getId()+"/review?approve=true").header("Authorization",token)).andExpect(status().isBadRequest());
        assertThat(questions.findById(question.getId()).orElseThrow().getStatus()).isEqualTo("PENDING_REVIEW");
    }
    @Test void successiveStudentsGetDifferentQuestionsWhenPoolHasAlternatives()throws Exception{
        for(int i=0;i<2;i++){Question q=question("APPROVED");var item=new ExamQuestionPool();item.setId(new ExamQuestionPoolId(exam.getId(),q.getId()));item.setCourseId(course.getId());item.setSelectionWeight(1);item.setAddedAt(OffsetDateTime.now());pool.saveAndFlush(item);}
        String first=assign(token,student.getEmail(),true,200);
        mvc.perform(post("/api/v1/attempts/start/"+first).header("Authorization",token(student))).andExpect(status().isOk());
        UUID firstAttempt=attempts.findByCandidateIdOrderByAttemptNoAsc(UUID.fromString(first)).getFirst().getId();
        User second=user("STUDENT");
        mvc.perform(post("/api/v1/courses/"+course.getId()+"/students").header("Authorization",token(user("ADMIN"))).contentType("application/json").content(json.writeValueAsString(Map.of("email",second.getEmail())))).andExpect(status().isOk());
        String next=assign(token,second.getEmail(),false,200);
        mvc.perform(post("/api/v1/attempts/start/"+next).header("Authorization",token(second))).andExpect(status().isOk());
        UUID secondAttempt=attempts.findByCandidateIdOrderByAttemptNoAsc(UUID.fromString(next)).getFirst().getId();
        assertThat(assigned.findByAttemptIdOrderBySequenceNo(firstAttempt).getFirst().getQuestionId()).isNotEqualTo(assigned.findByAttemptIdOrderBySequenceNo(secondAttempt).getFirst().getQuestionId());
        mvc.perform(patch("/api/v1/exams/"+exam.getId()+"/close").header("Authorization",token)).andExpect(status().isConflict());
    }
    @Test void teacherCanCloseAnExamBeforeEditingQuestionsUsedInIt()throws Exception{
        Question q=question("APPROVED");var item=new ExamQuestionPool();item.setId(new ExamQuestionPoolId(exam.getId(),q.getId()));item.setCourseId(course.getId());item.setSelectionWeight(1);item.setAddedAt(OffsetDateTime.now());pool.saveAndFlush(item);
        mvc.perform(patch("/api/v1/exams/"+exam.getId()+"/close").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(delete("/api/v1/questions/"+q.getId()).header("Authorization",token)).andExpect(status().isOk());
    }
    @Test void csvImportPreviewsBeforeWritingAndNeverAutoApproves()throws Exception{
        Rubric rubric=rubric("PUBLISHED");String csv="content,referenceAnswer,bloomLevel,difficulty\r\n\"Giải thích ví dụ, có hai phần\",Đáp án,Hiểu,MEDIUM\r\n";
        long before=imports.count();var preview=importFile(token,"q.csv",csv,rubric.getId(),true);
        assertThat(preview.path("status").asText()).isEqualTo("READY");assertThat(preview.path("rows").get(0).path("bloomLevel").asText()).isEqualTo("UNDERSTAND");
        assertThat(questions.findByCourseId(course.getId())).isEmpty();assertThat(imports.count()).isEqualTo(before);
        var saved=importFile(token,"q.csv",csv,rubric.getId(),false);assertThat(saved.path("importedRows").asInt()).isEqualTo(1);
        var imported=questions.findByCourseId(course.getId()).getFirst();assertThat(imported.getSourceType()).isEqualTo("IMPORTED");assertThat(imported.getStatus()).isEqualTo("PENDING_REVIEW");assertThat(imported.getApprovedBy()).isNull();assertThat(imported.getImportBatchId().toString()).isEqualTo(saved.path("batchId").asText());
        mvc.perform(patch("/api/v1/questions/"+imported.getId()+"/review?approve=true").header("Authorization",token)).andExpect(status().isOk());
    }
    @Test void invalidImportReportsRowErrorsAndDoesNotWritePartialQuestions()throws Exception{
        Rubric rubric=rubric("PUBLISHED");String data=json.writeValueAsString(List.of(Map.of("content","Câu hợp lệ","bloomLevel","APPLY","difficulty","MEDIUM"),Map.of("content","Câu sai Bloom","bloomLevel","UNKNOWN","difficulty","EASY")));
        var result=importFile(token,"q.json",data,rubric.getId(),false);assertThat(result.path("status").asText()).isEqualTo("FAILED");assertThat(result.path("importedRows").asInt()).isZero();assertThat(result.path("rows").get(1).path("errors").size()).isPositive();
        assertThat(questions.findByCourseId(course.getId())).isEmpty();assertThat(imports.findById(UUID.fromString(result.path("batchId").asText())).orElseThrow().getStatus()).isEqualTo("FAILED");
    }
    @Test void duplicateImportIsRevalidatedAtCommitAndCannotPublishInjectedStatuses()throws Exception{
        Rubric rubric=rubric("PUBLISHED");String data=json.writeValueAsString(List.of(Map.of("content","Một câu hỏi mới","bloomLevel","ANALYZE","difficulty","HARD","status","APPROVED")));
        assertThat(importFile(token,"q.json",data,rubric.getId(),true).path("status").asText()).isEqualTo("READY");
        importFile(token,"q.json",data,rubric.getId(),false);
        var repeated=importFile(token,"q.json",data,rubric.getId(),false);assertThat(repeated.path("status").asText()).isEqualTo("FAILED");
        assertThat(questions.findByCourseId(course.getId())).hasSize(1).allMatch(q->q.getStatus().equals("PENDING_REVIEW"));
        String duplicate=json.writeValueAsString(List.of(Map.of("content","Trùng trong tệp","bloomLevel","APPLY","difficulty","EASY"),Map.of("content","  TRÙNG  TRONG TỆP  ","bloomLevel","APPLY","difficulty","EASY")));
        assertThat(importFile(token,"q.json",duplicate,rubric.getId(),true).path("status").asText()).isEqualTo("INVALID");
    }
    @Test void importChecksTeacherPermissionsAndPublishedRubric()throws Exception{
        String data="[{\"content\":\"Câu mới\",\"bloomLevel\":\"REMEMBER\",\"difficulty\":\"EASY\"}]";Rubric draft=rubric("DRAFT");
        assertThat(importFile(token,"q.json",data,draft.getId(),true).path("status").asText()).isEqualTo("INVALID");
        for(User actor:List.of(student,user("ADMIN"),user("LECTURER")))mvc.perform(multipart("/api/v1/courses/"+course.getId()+"/question-imports").file(new org.springframework.mock.web.MockMultipartFile("file","q.json","application/json",data.getBytes(java.nio.charset.StandardCharsets.UTF_8))).header("Authorization",token(actor))).andExpect(status().isForbidden());
        assertThat(questions.findByCourseId(course.getId())).isEmpty();
    }
    @Test void examAcceptsAdaptiveSelectionAndStudentFollowupBudgetIndependentlyOfMainCount()throws Exception{
        var body=Map.of("courseId",course.getId(),"title","Thi thích ứng","mainQuestionCount",3,"maxFollowupsPerStudent",4,"selectionMode","ADAPTIVE","answerTimeLimitSeconds",120);
        mvc.perform(post("/api/v1/exams").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isOk()).andExpect(jsonPath("$.selectionMode").value("ADAPTIVE")).andExpect(jsonPath("$.maxFollowupsPerStudent").value(4));
        var invalid=new HashMap<String,Object>(body);invalid.put("maxFollowupsPerStudent",101);
        mvc.perform(post("/api/v1/exams").header("Authorization",token).contentType("application/json").content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest());
    }
    private com.fasterxml.jackson.databind.JsonNode importFile(String auth,String filename,String data,UUID rubricId,boolean preview)throws Exception{
        var request=multipart("/api/v1/courses/"+course.getId()+"/question-imports").file(new org.springframework.mock.web.MockMultipartFile("file",filename,"application/octet-stream",data.getBytes(java.nio.charset.StandardCharsets.UTF_8))).param("preview",String.valueOf(preview)).header("Authorization",auth);if(rubricId!=null)request.param("rubricId",rubricId.toString());
        return json.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private Rubric rubric(String status){var r=new Rubric();r.setCourseId(course.getId());r.setCreatedBy(teacher.getId());r.setName("Rubric giáo viên");r.setStatus(status);r.setMaxScore(java.math.BigDecimal.TEN);r.setCreatedAt(OffsetDateTime.now());r.setUpdatedAt(OffsetDateTime.now());return rubrics.saveAndFlush(r);}
    private Map<String,Object> questionBody(UUID rubricId,String text){return Map.of("courseId",course.getId(),"rubricId",rubricId,"content",text,"referenceAnswer","Đáp án tham khảo","bloomLevel","UNDERSTAND","difficulty","MEDIUM");}
    private Question question(String status){var q=new Question();q.setCourseId(course.getId());q.setRubricId(rubric("PUBLISHED").getId());q.setCreatedBy(teacher.getId());q.setContent("Câu hỏi của giáo viên");q.setReferenceAnswer("Đáp án");q.setBloomLevel("UNDERSTAND");q.setDifficulty("MEDIUM");q.setSourceType("MANUAL");q.setStatus(status);if(status.equals("APPROVED")){q.setApprovedBy(teacher.getId());q.setApprovedAt(OffsetDateTime.now());}q.setCreatedAt(OffsetDateTime.now());q.setUpdatedAt(OffsetDateTime.now());return questions.saveAndFlush(q);}
    private String assign(String auth,String email,boolean enroll,int status)throws Exception{
        var response=mvc.perform(post("/api/v1/exams/"+exam.getId()+"/candidates").header("Authorization",auth).contentType("application/json").content(json.writeValueAsString(body(email,enroll))))
            .andExpect(status().is(status)).andReturn().getResponse();
        return status==200?json.readTree(response.getContentAsString()).asText():null;
    }
    private Map<String,Object> body(String email,boolean enroll){return Map.of("studentEmail",email,"scheduledStart",OffsetDateTime.now().minusMinutes(1).toString(),"scheduledEnd",OffsetDateTime.now().plusMinutes(20).toString(),"enrollIfNeeded",enroll);}
    private CourseEnrollmentId enrollmentId(){return new CourseEnrollmentId(course.getId(),student.getId());}
    private void enrollment(String status){var value=enrollments.findById(enrollmentId()).orElseGet(CourseEnrollment::new);value.setId(enrollmentId());value.setEnrollmentStatus(status);if(value.getEnrolledAt()==null)value.setEnrolledAt(OffsetDateTime.now());enrollments.saveAndFlush(value);}
    private String token(User u){return "Bearer "+jwt.generateToken(u.getId().toString(),u.getEmail(),u.getRole());}
    private User user(String role){var value=new User();value.setFullName("Schedule tester");value.setEmail(UUID.randomUUID()+"@example.com");value.setPasswordHash("unused");value.setIsActive(true);value.setRole(role);value.setCreatedAt(OffsetDateTime.now());value.setUpdatedAt(OffsetDateTime.now());return users.saveAndFlush(value);}
}
