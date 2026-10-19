package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.repository.*;
import com.example.swd392_se1907_aives.service.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:exam-monitor;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.interview.timeouts-enabled=false",
    "JWT_SECRET=exam_monitor_test_secret_at_least_32_characters_long"})
@AutoConfigureMockMvc
class ExamMonitorTests {
    static final Path STORAGE=Path.of("target","monitor-audio-"+UUID.randomUUID()).toAbsolutePath();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("app.interview.storage-dir",STORAGE::toString);r.add("app.materials.storage-dir",()->STORAGE.resolve("materials").toString());}
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JwtService jwt;
    @Autowired UserRepository users;@Autowired CourseRepository courses;@Autowired CourseLecturerRepository lecturers;
    @Autowired ExamRepository exams;@Autowired ExamCandidateRepository candidates;@Autowired ExamAttemptRepository attempts;
    @Autowired AttemptQuestionRepository questions;@Autowired InterviewTurnRepository turns;
    @Autowired AiInterviewRequestRepository requests;@Autowired InterviewAudioStorage audio;
    @Autowired CourseMaterialRepository materials;@Autowired QuestionGenerationJobRepository jobs;@Autowired QuestionGenerationMaterialRepository jobMaterials;
    User teacher,student,admin;Course course;Exam exam;ExamCandidate candidate;ExamAttempt attempt;AttemptQuestion question;InterviewTurn prompt,answer;
    @BeforeEach void setup(){
        teacher=user("LECTURER");student=user("STUDENT");admin=user("ADMIN");
        course=new Course();course.setCode("M"+UUID.randomUUID().toString().substring(0,8));course.setName("Monitor course");course.setIsActive(true);course.setCreatedAt(OffsetDateTime.now());course=courses.saveAndFlush(course);
        var assigned=new CourseLecturer();assigned.setId(new CourseLecturerId(course.getId(),teacher.getId()));assigned.setAssignedAt(OffsetDateTime.now());lecturers.saveAndFlush(assigned);
        exam=new Exam();exam.setCourseId(course.getId());exam.setCreatedBy(teacher.getId());exam.setTitle("Monitor viva");exam.setStatus("ACTIVE");exam.setSelectionMode("RANDOM");exam.setMainQuestionCount(1);exam.setMaxFollowupsPerQuestion(2);exam.setAnswerTimeLimitSeconds(120);exam.setSlotDurationMinutes(20);exam.setExamLanguage("vi");exam.setExamTimezone("Asia/Ho_Chi_Minh");exam.setCreatedAt(OffsetDateTime.now());exam.setUpdatedAt(OffsetDateTime.now());exam=exams.saveAndFlush(exam);
        candidate=new ExamCandidate();candidate.setCourseId(course.getId());candidate.setExamId(exam.getId());candidate.setStudentId(student.getId());candidate.setStatus("IN_PROGRESS");candidate.setScheduledStart(OffsetDateTime.now().minusMinutes(1));candidate.setScheduledEnd(OffsetDateTime.now().plusMinutes(20));candidate.setCreatedAt(OffsetDateTime.now());candidate=candidates.saveAndFlush(candidate);
        attempt=new ExamAttempt();attempt.setCandidateId(candidate.getId());attempt.setExamId(exam.getId());attempt.setAttemptNo(1);attempt.setStatus("IN_PROGRESS");attempt.setStartedAt(OffsetDateTime.now());attempt.setLastActivityAt(OffsetDateTime.now());attempt.setCreatedAt(OffsetDateTime.now());attempt=attempts.saveAndFlush(attempt);
        question=new AttemptQuestion();question.setAttemptId(attempt.getId());question.setExamId(exam.getId());question.setQuestionId(UUID.randomUUID());question.setSequenceNo(1);question.setQuestionTextSnapshot("Explain your reasoning");question.setReferenceAnswerSnapshot("PRIVATE REFERENCE");question.setRubricSnapshot("PRIVATE RUBRIC");question.setSelectionMode("RANDOM");question.setStatus("ASKING");question.setCreatedAt(OffsetDateTime.now());question=questions.saveAndFlush(question);
        prompt=turn(1,"AI","MAIN_QUESTION","Explain your reasoning","NONE");prompt.setResponseDeadlineAt(OffsetDateTime.now().plusSeconds(120));turns.saveAndFlush(prompt);
    }
    @AfterAll static void cleanup()throws Exception{if(Files.exists(STORAGE))try(var files=Files.walk(STORAGE)){for(var file:files.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
    @Test void assignedTeacherSeesProgressAndHistoryWithoutPrivateAnswersOrStorageKeys()throws Exception{
        mvc.perform(get(base()+"/monitor").header("Authorization",token(teacher))).andExpect(status().isOk())
            .andExpect(jsonPath("$.candidates[0].attempts[0].phase").value("ANSWERING"))
            .andExpect(jsonPath("$.candidates[0].attempts[0].currentSequence").value(1));
        answer=turn(2,"STUDENT","MAIN_ANSWER","Student reasoning","SUCCEEDED");
        String key=audio.save(attempt.getId(),answer.getId(),new byte[]{1,2,3,4},"wav");answer.setAudioObjectKey(key);turns.saveAndFlush(answer);
        var response=mvc.perform(get(base()+"/attempts/"+attempt.getId()+"/transcript").header("Authorization",token(teacher))).andExpect(status().isOk())
            .andExpect(jsonPath("$.turns[1].text").value("Student reasoning")).andExpect(jsonPath("$.turns[1].hasAudio").value(true)).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain(key,"PRIVATE REFERENCE","PRIVATE RUBRIC");
        mvc.perform(get(base()+"/attempts/"+attempt.getId()+"/audio/"+answer.getId()).header("Authorization",token(teacher)))
            .andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1,2,3,4})).andExpect(header().string("Cache-Control","no-store"));
    }
    @Test void adminStudentsAndUnassignedTeachersCannotMonitorReadTranscriptOrAudio()throws Exception{
        for(var actor:List.of(admin,student,user("LECTURER"))){
            mvc.perform(get(base()+"/monitor").header("Authorization",token(actor))).andExpect(status().isForbidden());
            mvc.perform(get(base()+"/attempts/"+attempt.getId()+"/transcript").header("Authorization",token(actor))).andExpect(status().isForbidden());
            mvc.perform(get(base()+"/attempts/"+attempt.getId()+"/audio/"+prompt.getId()).header("Authorization",token(actor))).andExpect(status().isForbidden());
        }
        mvc.perform(get(base()+"/monitor")).andExpect(status().isUnauthorized());
    }
    @Test void attemptAndAudioIdsCannotBeMixedAcrossExamsOrAttempts()throws Exception{
        UUID originalExam=exam.getId();exam.setId(null);exam.setTitle("Second exam");exam=exams.saveAndFlush(exam);
        String foreign="/api/v1/exams/"+exam.getId()+"/attempts/"+attempt.getId();
        mvc.perform(get(foreign+"/transcript").header("Authorization",token(teacher))).andExpect(status().isNotFound());
        mvc.perform(get(foreign+"/audio/"+prompt.getId()).header("Authorization",token(teacher))).andExpect(status().isNotFound());
        question.setAttemptId(UUID.randomUUID());questions.saveAndFlush(question);
        mvc.perform(get("/api/v1/exams/"+originalExam+"/attempts/"+attempt.getId()+"/audio/"+prompt.getId()).header("Authorization",token(teacher))).andExpect(status().isNotFound());
    }
    @Test void processingFailuresAndCompletedTimedOutQuestionsAreVisible()throws Exception{
        answer=turn(2,"STUDENT","MAIN_ANSWER",null,"FAILED");
        mvc.perform(get(base()+"/monitor").header("Authorization",token(teacher))).andExpect(status().isOk()).andExpect(jsonPath("$.candidates[0].attempts[0].phase").value("TRANSCRIPTION_FAILED"));
        answer.setSpeechStatus("SUCCEEDED");answer.setTextContent("An accepted answer");turns.saveAndFlush(answer);
        var request=new AiInterviewRequest();request.setAttemptQuestionId(question.getId());request.setInputTurnId(answer.getId());request.setRequestType("FOLLOWUP_GENERATION");request.setStatus("FAILED");request.setParameters(json.createObjectNode());request.setRequestedAt(OffsetDateTime.now());requests.saveAndFlush(request);
        mvc.perform(get(base()+"/monitor").header("Authorization",token(teacher))).andExpect(status().isOk()).andExpect(jsonPath("$.candidates[0].attempts[0].phase").value("AI_FAILED"));
        var followup=turn(3,"AI","FOLLOWUP_QUESTION","Clarify your reasoning","NONE");followup.setFollowupNumber(1);turns.saveAndFlush(followup);
        question.setStatus("TIMED_OUT");questions.saveAndFlush(question);attempt.setStatus("COMPLETED");attempt.setEndedAt(OffsetDateTime.now());attempts.saveAndFlush(attempt);
        mvc.perform(get(base()+"/monitor").header("Authorization",token(teacher))).andExpect(status().isOk()).andExpect(jsonPath("$.candidates[0].attempts[0].completedQuestions").value(1)).andExpect(jsonPath("$.candidates[0].attempts[0].timedOutQuestions").value(1)).andExpect(jsonPath("$.candidates[0].attempts[0].followupsUsed").value(1));
    }
    @Test void onlyAdminCanFilterCoursesByTeacherAndEnrollStudents()throws Exception{
        String url="/api/v1/courses?lecturerId="+teacher.getId();
        mvc.perform(get(url).header("Authorization",token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(course.getId().toString()));
        mvc.perform(get(url).header("Authorization",token(teacher))).andExpect(status().isForbidden());
        String roster="/api/v1/courses/"+course.getId()+"/students";
        String body=json.writeValueAsString(Map.of("email",student.getEmail()));
        mvc.perform(post(roster).header("Authorization",token(teacher)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post(roster).header("Authorization",token(admin)).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(get(roster).header("Authorization",token(teacher))).andExpect(status().isOk()).andExpect(jsonPath("$[0].email").value(student.getEmail()));
    }
    @Test void missingApiReturns404InsteadOfMaskingTheProblemAs500()throws Exception{
        mvc.perform(get("/api/v1/nonexistent-monitor-api").header("Authorization",token(teacher))).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Khởi động lại backend")));
    }
    @Test void unsupportedMethodAndBodyFormatKeepTheirClientErrorStatus()throws Exception{
        mvc.perform(patch("/api/v1/courses/"+course.getId()+"/students").header("Authorization",token(admin)))
            .andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow",org.hamcrest.Matchers.containsString("POST")))
            .andExpect(jsonPath("$.code").value(405));
        mvc.perform(post("/api/v1/users").header("Authorization",token(admin)).contentType("text/plain").content("not-json"))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value(415));
        mvc.perform(get("/api/v1/courses").header("Authorization",token(admin)).accept("application/xml"))
            .andExpect(status().isNotAcceptable()).andExpect(jsonPath("$.code").value(406));
    }
    @Test void accountEditsRejectBlankNamesAndEmailsAndKeepAuditTimes()throws Exception{
        String url="/api/v1/users/"+student.getId(),originalName=student.getFullName(),originalEmail=student.getEmail();
        mvc.perform(put(url).header("Authorization",token(admin)).contentType("application/json").content("{\"fullName\":\"   \"}")).andExpect(status().isBadRequest());
        mvc.perform(put(url).header("Authorization",token(admin)).contentType("application/json").content("{\"email\":\"\"}")).andExpect(status().isBadRequest());
        var unchanged=users.findById(student.getId()).orElseThrow();assertThat(unchanged.getFullName()).isEqualTo(originalName);assertThat(unchanged.getEmail()).isEqualTo(originalEmail);
        var oldTime=OffsetDateTime.now().minusDays(1);unchanged.setUpdatedAt(oldTime);users.saveAndFlush(unchanged);
        mvc.perform(put(url).header("Authorization",token(admin)).contentType("application/json").content("{\"fullName\":\"Updated student\"}")).andExpect(status().isOk());
        assertThat(users.findById(student.getId()).orElseThrow().getUpdatedAt()).isAfter(oldTime);
    }
    @Test void accountCreationSetsRequiredAuditTimes()throws Exception{
        String email=UUID.randomUUID()+"@example.com";
        mvc.perform(post("/api/v1/users").header("Authorization",token(admin)).contentType("application/json")
            .content(json.writeValueAsString(Map.of("fullName","New student","email",email,"password","test-password-123","role","STUDENT")))).andExpect(status().isOk());
        var created=users.findByEmailIgnoreCase(email).orElseThrow();assertThat(created.getCreatedAt()).isNotNull();assertThat(created.getUpdatedAt()).isNotNull();assertThat(created.getIsActive()).isTrue();
    }
    @Test void lecturerExamListContainsOnlyAssignedCourses()throws Exception{
        mvc.perform(get("/api/v1/exams").header("Authorization",token(teacher))).andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id=='"+exam.getId()+"')]").isNotEmpty());
        mvc.perform(get("/api/v1/exams").header("Authorization",token(user("LECTURER")))).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(post("/api/v1/exams").header("Authorization",token(teacher)).contentType("application/json")
            .content(json.writeValueAsString(Map.of("courseId",course.getId(),"title","x".repeat(201),"mainQuestionCount",1)))).andExpect(status().isBadRequest());
    }
    @Test void administratorEditsArchivesAndRestoresCoursesWithoutDeletingHistory()throws Exception{
        String url="/api/v1/courses/"+course.getId();
        String body=json.writeValueAsString(Map.of("code",course.getCode(),"name","Renamed course","description","Long description ".repeat(60)));
        mvc.perform(put(url).header("Authorization",token(teacher)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put(url).header("Authorization",token(admin)).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed course"));
        mvc.perform(delete(url).header("Authorization",token(admin))).andExpect(status().isConflict());
        exam.setStatus("CLOSED");exams.saveAndFlush(exam);attempt.setStatus("COMPLETED");attempts.saveAndFlush(attempt);
        mvc.perform(delete(url).header("Authorization",token(admin))).andExpect(status().isOk());
        assertThat(courses.findById(course.getId()).orElseThrow().getIsActive()).isFalse();assertThat(attempts.findById(attempt.getId())).isPresent();
        mvc.perform(get("/api/v1/courses").header("Authorization",token(teacher))).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id=='"+course.getId()+"')]").isEmpty());
        mvc.perform(patch(url+"/restore").header("Authorization",token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    }
    @Test void removalOfEnrollmentAndAssignmentProtectsLiveExams()throws Exception{
        String roster="/api/v1/courses/"+course.getId()+"/students";
        mvc.perform(post(roster).header("Authorization",token(admin)).contentType("application/json").content(json.writeValueAsString(Map.of("email",student.getEmail())))).andExpect(status().isOk());
        mvc.perform(delete(roster+"/"+student.getId()).header("Authorization",token(admin))).andExpect(status().isConflict());
        String assignment="/api/v1/courses/"+course.getId()+"/lecturers/"+teacher.getId();
        mvc.perform(delete(assignment).header("Authorization",token(admin))).andExpect(status().isConflict());
        attempt.setStatus("COMPLETED");attempts.saveAndFlush(attempt);exam.setStatus("CLOSED");exams.saveAndFlush(exam);
        mvc.perform(delete(roster+"/"+student.getId()).header("Authorization",token(admin))).andExpect(status().isOk());
        mvc.perform(delete(assignment).header("Authorization",token(admin))).andExpect(status().isOk());
        assertThat(lecturers.existsById(new CourseLecturerId(course.getId(),teacher.getId()))).isFalse();assertThat(attempts.findById(attempt.getId())).isPresent();
    }
    @Test void accountRemovalDisablesAccessAndCannotDeleteTheCurrentAdministrator()throws Exception{
        mvc.perform(delete("/api/v1/users/"+admin.getId()).header("Authorization",token(admin))).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/users/"+student.getId()).header("Authorization",token(teacher))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/users/"+student.getId()).header("Authorization",token(admin))).andExpect(status().isOk());
        assertThat(users.findById(student.getId()).orElseThrow().getIsActive()).isFalse();assertThat(attempts.findById(attempt.getId())).isPresent();
    }
    @Test void administratorReplacesLecturerAtomicallyOnlyAfterLiveExamsEnd()throws Exception{
        var replacement=user("LECTURER");String url="/api/v1/courses/"+course.getId()+"/lecturers/"+teacher.getId();
        String body=json.writeValueAsString(Map.of("email",replacement.getEmail()));
        mvc.perform(put(url).header("Authorization",token(admin)).contentType("application/json").content(body)).andExpect(status().isConflict());
        assertThat(lecturers.existsById(new CourseLecturerId(course.getId(),teacher.getId()))).isTrue();
        attempt.setStatus("COMPLETED");attempts.saveAndFlush(attempt);exam.setStatus("CLOSED");exams.saveAndFlush(exam);
        mvc.perform(put(url).header("Authorization",token(teacher)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put(url).header("Authorization",token(admin)).contentType("application/json").content(body)).andExpect(status().isOk());
        assertThat(lecturers.existsById(new CourseLecturerId(course.getId(),teacher.getId()))).isFalse();
        assertThat(lecturers.existsById(new CourseLecturerId(course.getId(),replacement.getId()))).isTrue();
    }
    @Test void teacherCanEditUnusedRubricAndDeleteDraftExamButNotAnExamWithAttempts()throws Exception{
        String body=json.writeValueAsString(Map.of("courseId",course.getId(),"name","Editable rubric","maxScore",10));
        var r=json.readTree(mvc.perform(post("/api/v1/rubrics").header("Authorization",token(teacher)).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String url="/api/v1/rubrics/"+r.path("id").asText();
        mvc.perform(put(url).header("Authorization",token(teacher)).contentType("application/json").content(body.replace("Editable rubric","Updated rubric"))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated rubric"));
        mvc.perform(delete(url).header("Authorization",token(teacher))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/questions").header("Authorization",token(teacher)).contentType("application/json").content(json.writeValueAsString(Map.of("courseId",course.getId(),"rubricId",r.path("id").asText(),"content","Cannot attach an archived rubric","bloomLevel","UNDERSTAND","difficulty","MEDIUM")))).andExpect(status().isConflict());
        var created=json.readTree(mvc.perform(post("/api/v1/exams").header("Authorization",token(teacher)).contentType("application/json").content(json.writeValueAsString(Map.of("courseId",course.getId(),"title","Unused draft","mainQuestionCount",1)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(delete("/api/v1/exams/"+created.path("id").asText()).header("Authorization",token(teacher))).andExpect(status().isOk());
        assertThat(exams.findById(UUID.fromString(created.path("id").asText()))).isEmpty();
        exam.setStatus("DRAFT");exams.saveAndFlush(exam);mvc.perform(delete(base()).header("Authorization",token(teacher))).andExpect(status().isConflict());
    }
    @Test void materialCanBeRenamedAndDeletedOnlyWhileItHasNoGenerationHistory()throws Exception{
        byte[] pdf;
        try(var doc=new org.apache.pdfbox.pdmodel.PDDocument();var output=new java.io.ByteArrayOutputStream()){
            var page=new org.apache.pdfbox.pdmodel.PDPage();doc.addPage(page);
            try(var text=new org.apache.pdfbox.pdmodel.PDPageContentStream(doc,page)){text.beginText();text.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),12);text.newLineAtOffset(30,700);text.showText("Material CRUD regression sample");text.endText();}doc.save(output);pdf=output.toByteArray();
        }
        var uploaded=json.readTree(mvc.perform(multipart("/api/v1/courses/"+course.getId()+"/materials").file(new org.springframework.mock.web.MockMultipartFile("file","original.pdf","application/pdf",pdf)).header("Authorization",token(teacher))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        UUID id=UUID.fromString(uploaded.path("id").asText());String url="/api/v1/materials/"+id;
        mvc.perform(put(url).header("Authorization",token(teacher)).contentType("application/json").content("{\"name\":\"renamed.pdf\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.originalFilename").value("renamed.pdf"));
        mvc.perform(put(url).header("Authorization",token(teacher)).contentType("application/json").content("{\"name\":\"../invalid.pdf\"}")).andExpect(status().isBadRequest());
        var job=new QuestionGenerationJob();job.setCourseId(course.getId());job.setRequestedBy(teacher.getId());job.setDesiredCount(1);job.setGeneratedCount(0);job.setStatus("FAILED");job.setCreatedAt(OffsetDateTime.now());job=jobs.saveAndFlush(job);
        var link=new QuestionGenerationMaterial();link.setId(new QuestionGenerationMaterialId(job.getId(),id));link.setCourseId(course.getId());jobMaterials.saveAndFlush(link);
        mvc.perform(delete(url).header("Authorization",token(teacher))).andExpect(status().isConflict());assertThat(materials.findById(id)).isPresent();
        jobMaterials.delete(link);mvc.perform(delete(url).header("Authorization",token(teacher))).andExpect(status().isOk());assertThat(materials.findById(id)).isEmpty();
    }
    String base(){return "/api/v1/exams/"+exam.getId();}
    InterviewTurn turn(int no,String speaker,String kind,String text,String speechStatus){var t=new InterviewTurn();t.setAttemptQuestionId(question.getId());t.setTurnNo(no);t.setSpeaker(speaker);t.setKind(kind);t.setTextContent(text);t.setFollowupNumber(0);t.setSpeechStatus(speechStatus);t.setCreatedAt(OffsetDateTime.now());return turns.saveAndFlush(t);}
    String token(User u){return "Bearer "+jwt.generateToken(u.getId().toString(),u.getEmail(),u.getRole());}
    User user(String role){var u=new User();u.setFullName(role+" tester");u.setEmail(UUID.randomUUID()+"@example.com");u.setRole(role);u.setPasswordHash("unused");u.setIsActive(true);u.setCreatedAt(OffsetDateTime.now());u.setUpdatedAt(OffsetDateTime.now());return users.saveAndFlush(u);}
}
