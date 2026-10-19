package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.repository.*;
import com.example.swd392_se1907_aives.service.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:teaching;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false",
    "JWT_SECRET=test_secret_at_least_32_characters_for_signing_tokens"
})
@AutoConfigureMockMvc
class TeachingFeaturesTests {
    private static final Path STORAGE=Path.of("target","test-materials-"+UUID.randomUUID()).toAbsolutePath();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("app.materials.storage-dir",STORAGE::toString);}
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired CourseRepository courses;
    @Autowired CourseLecturerRepository lecturers;
    @Autowired CourseMaterialRepository materials;
    @Autowired MaterialChunkRepository chunks;
    @Autowired QuestionRepository questions;
    @Autowired QuestionSourceChunkRepository sources;
    @Autowired JwtService jwt;
    @Autowired org.springframework.jdbc.core.JdbcTemplate database;
    @MockitoBean OpenAiQuestionClient ai;
    private User teacher;
    private Course course;
    private String token;

    @BeforeEach void setup(){
        teacher=user("LECTURER");course=course();assign(course,teacher);
        token="Bearer "+jwt.generateToken(teacher.getId().toString(),teacher.getEmail(),teacher.getRole());
        when(ai.configured()).thenReturn(true);when(ai.model()).thenReturn("test-model");
    }
    @AfterAll static void cleanup() throws IOException {
        if(Files.exists(STORAGE))try(var paths=Files.walk(STORAGE)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
    }
    @Test void lecturerSeesAssignedCoursesAndCannotAccessUnassignedCourse() throws Exception {
        Course other=course();
        mvc.perform(get("/api/v1/courses").header("Authorization",token)).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(course.getId().toString()));
        mvc.perform(post("/api/v1/rubrics").header("Authorization",token).contentType("application/json")
            .content(json.writeValueAsString(Map.of("courseId",other.getId(),"name","Denied","maxScore",10))))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/courses/"+other.getId()+"/materials").header("Authorization",token)).andExpect(status().isForbidden());
        User student=user("STUDENT");
        mvc.perform(get("/api/v1/courses/"+course.getId()+"/materials").header("Authorization","Bearer "+jwt.generateToken(student.getId().toString(),student.getEmail(),student.getRole())))
            .andExpect(status().isForbidden());
    }
    @Test void criteriaCanBeReadEditedAndDeletedAndPublishedRubricIsLocked() throws Exception {
        String rubric=createRubric();
        String criterion=addCriterion(rubric,10,1).path("id").asText();
        mvc.perform(get("/api/v1/rubrics/"+rubric+"/criteria").header("Authorization",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].description").value("Giải thích rõ"));
        mvc.perform(put("/api/v1/rubrics/"+rubric+"/criteria/"+criterion).header("Authorization",token).contentType("application/json")
            .content(criterionBody(9,1))).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/rubrics/"+rubric+"/publish").header("Authorization",token)).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/rubrics/"+rubric+"/criteria/"+criterion).header("Authorization",token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/rubrics/"+rubric+"/criteria").header("Authorization",token)).andExpect(jsonPath("$.length()").value(0));
        addCriterion(rubric,10,1);
        mvc.perform(patch("/api/v1/rubrics/"+rubric+"/publish").header("Authorization",token)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/rubrics/"+rubric+"/criteria").header("Authorization",token).contentType("application/json").content(criterionBody(1,2)))
            .andExpect(status().isConflict());
    }
    @Test void documentsCanBeUploadedIndexedDownloadedAndUsedForReviewableAiQuestions() throws Exception {
        String rubric=createRubric();addCriterion(rubric,10,1);
        mvc.perform(patch("/api/v1/rubrics/"+rubric+"/publish").header("Authorization",token)).andExpect(status().isOk());
        byte[] bytes=docx("Software architecture separates responsibilities between components.");
        String material=upload(bytes,"lecture.docx");
        mvc.perform(get("/api/v1/materials/"+material+"/download").header("Authorization",token)).andExpect(status().isOk()).andExpect(content().bytes(bytes));
        mvc.perform(post("/api/v1/materials/"+material+"/index").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.extractionStatus").value("READY"));
        mvc.perform(post("/api/v1/materials/"+material+"/index").header("Authorization",token)).andExpect(status().isOk());
        var records=chunks.findByMaterialIdOrderByChunkIndex(UUID.fromString(material));assertThat(records).hasSize(1);
        mvc.perform(get("/api/v1/materials/"+material+"/chunks").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$[0].content").value("Software architecture separates responsibilities between components."));
        UUID chunkId=records.getFirst().getId();
        when(ai.generate(anyString(),eq(2),eq("UNDERSTAND"),eq("MEDIUM"),any())).thenReturn(List.of(
            new OpenAiQuestionClient.Draft("Phân chia trách nhiệm là gì?","Mỗi thành phần đảm nhận một trách nhiệm.",List.of(chunkId)),
            new OpenAiQuestionClient.Draft("Vì sao cần phân chia trách nhiệm?","Để tách biệt vai trò các thành phần.",List.of(chunkId))));
        JsonNode job=json.readTree(mvc.perform(post("/api/v1/courses/"+course.getId()+"/generation-jobs").header("Authorization",token)
            .contentType("application/json").content(generationBody(rubric,material,2))).andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.generatedCount").value(2)).andReturn().getResponse().getContentAsString());
        var generated=questions.findByCourseId(course.getId());assertThat(generated).hasSize(2);
        for(var q:generated){assertThat(q.getStatus()).isEqualTo("PENDING_REVIEW");assertThat(q.getSourceType()).isEqualTo("AI");assertThat(q.getGenerationJobId().toString()).isEqualTo(job.path("id").asText());assertThat(sources.findByIdQuestionId(q.getId())).hasSize(1);}
        mvc.perform(get("/api/v1/generation-jobs/"+job.path("id").asText()+"/questions").header("Authorization",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].status").value("PENDING_REVIEW")).andExpect(jsonPath("$[0].referenceAnswer").isNotEmpty());
        var otherTeacher=user("LECTURER");String otherToken="Bearer "+jwt.generateToken(otherTeacher.getId().toString(),otherTeacher.getEmail(),otherTeacher.getRole());
        mvc.perform(get("/api/v1/generation-jobs/"+job.path("id").asText()+"/questions").header("Authorization",otherToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/generation-jobs/"+UUID.randomUUID()+"/questions").header("Authorization",token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/questions/"+generated.getFirst().getId()).header("Authorization",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sources[0].filename").value("lecture.docx"));
    }
    @Test void pdfWithLongExtractedTextCanBeIndexedWithoutTruncation() throws Exception {
        byte[] bytes;
        try(var document=new org.apache.pdfbox.pdmodel.PDDocument();var output=new ByteArrayOutputStream()){
            var page=new org.apache.pdfbox.pdmodel.PDPage();document.addPage(page);
            try(var stream=new org.apache.pdfbox.pdmodel.PDPageContentStream(document,page)){
                stream.beginText();stream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),10);stream.newLineAtOffset(30,750);stream.setLeading(14);
                for(int i=0;i<35;i++){stream.showText("Software architecture separates responsibilities between components, row "+i+".");stream.newLine();}stream.endText();
            }
            document.save(output);bytes=output.toByteArray();
        }
        String material=upload(bytes,"long-document.pdf");
        mvc.perform(post("/api/v1/materials/"+material+"/index").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.extractionStatus").value("READY"));
        var saved=chunks.findByMaterialIdOrderByChunkIndex(UUID.fromString(material));assertThat(saved.size()).isGreaterThan(1);assertThat(saved.getFirst().getContent().length()).isGreaterThan(255);
        assertThat(saved.stream().map(MaterialChunk::getContent).collect(java.util.stream.Collectors.joining())).contains("row 34");
    }
    @Test void indexingDatabaseFailureRollsBackChunksRecordsFailureAndCanBeRetried()throws Exception{
        String material=upload(docx("Text to preserve after a database retry. ".repeat(80)),"retry.docx");
        database.execute("ALTER TABLE material_chunks ADD CONSTRAINT test_index_failure CHECK (material_id <> CAST('"+material+"' AS UUID))");
        try{
            mvc.perform(post("/api/v1/materials/"+material+"/index").header("Authorization",token)).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Không lưu được văn bản")));
            assertThat(materials.findById(UUID.fromString(material)).orElseThrow().getExtractionStatus()).isEqualTo("FAILED");
            assertThat(chunks.countByMaterialId(UUID.fromString(material))).isZero();
        }finally{database.execute("ALTER TABLE material_chunks DROP CONSTRAINT test_index_failure");}
        mvc.perform(post("/api/v1/materials/"+material+"/index").header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.extractionStatus").value("READY"));
        assertThat(chunks.countByMaterialId(UUID.fromString(material))).isGreaterThan(1);
    }
    @Test void chunkBoundariesDoNotBreakUnicodeSurrogatePairs()throws Exception{
        String text="a".repeat(1199)+"😀"+"b".repeat(400);String material=upload(docx(text),"unicode.docx");
        mvc.perform(post("/api/v1/materials/"+material+"/index").header("Authorization",token)).andExpect(status().isOk());
        var records=chunks.findByMaterialIdOrderByChunkIndex(UUID.fromString(material));
        assertThat(records.stream().map(MaterialChunk::getContent).collect(java.util.stream.Collectors.joining())).isEqualTo(text);
        assertThat(records).allMatch(r->!Character.isHighSurrogate(r.getContent().charAt(r.getContent().length()-1)));
    }
    @Test void badUploadsAndCrossCourseMaterialsAreRejectedAndProviderFailuresAreRecorded() throws Exception {
        mvc.perform(multipart("/api/v1/courses/"+course.getId()+"/materials").file(new MockMultipartFile("file","fake.pdf","application/pdf","not PDF".getBytes())).header("Authorization",token))
            .andExpect(status().isBadRequest());
        String rubric=createRubric();addCriterion(rubric,10,1);
        mvc.perform(patch("/api/v1/rubrics/"+rubric+"/publish").header("Authorization",token));
        String material=upload(docx("Architecture and components"),"valid.docx");
        mvc.perform(post("/api/v1/materials/"+material+"/index").header("Authorization",token));
        when(ai.generate(anyString(),eq(1),anyString(),anyString(),any())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,"OpenAI unavailable"));
        mvc.perform(post("/api/v1/courses/"+course.getId()+"/generation-jobs").header("Authorization",token).contentType("application/json").content(generationBody(rubric,material,1)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("FAILED")).andExpect(jsonPath("$.generatedCount").value(0));
        assertThat(questions.findByCourseId(course.getId())).isEmpty();
        Course other=course();assign(other,teacher);
        var foreign=materials.findById(UUID.fromString(material)).orElseThrow();foreign.setCourseId(other.getId());materials.saveAndFlush(foreign);
        mvc.perform(post("/api/v1/courses/"+course.getId()+"/generation-jobs").header("Authorization",token).contentType("application/json").content(generationBody(rubric,material,1)))
            .andExpect(status().isBadRequest());
    }
    private String upload(byte[] bytes,String filename) throws Exception {
        return json.readTree(mvc.perform(multipart("/api/v1/courses/"+course.getId()+"/materials").file(new MockMultipartFile("file",filename,MaterialService.DOCX,bytes)).header("Authorization",token))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.extractionStatus").value("UPLOADED")).andReturn().getResponse().getContentAsString()).path("id").asText();
    }
    private String createRubric() throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/rubrics").header("Authorization",token).contentType("application/json")
            .content(json.writeValueAsString(Map.of("courseId",course.getId(),"name","Architecture","maxScore",10))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("id").asText();
    }
    private JsonNode addCriterion(String rubric,int points,int order) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/rubrics/"+rubric+"/criteria").header("Authorization",token).contentType("application/json").content(criterionBody(points,order)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    private String criterionBody(int points,int order) throws Exception {return json.writeValueAsString(Map.of("criterionName","Hiểu nội dung","description","Giải thích rõ","maxPoints",points,"sortOrder",order));}
    private String generationBody(String rubric,String material,int count) throws Exception {return json.writeValueAsString(Map.of("rubricId",rubric,"materialIds",List.of(material),"desiredCount",count,"bloomLevel","UNDERSTAND","difficulty","MEDIUM","instruction",""));}
    private User user(String role){var user=new User();user.setFullName("Test Teacher");user.setEmail(UUID.randomUUID()+"@example.com");user.setPasswordHash("unused");user.setRole(role);user.setIsActive(true);user.setCreatedAt(OffsetDateTime.now());user.setUpdatedAt(OffsetDateTime.now());return users.saveAndFlush(user);}
    private Course course(){var value=new Course();value.setCode("C"+UUID.randomUUID().toString().substring(0,8));value.setName("Test Course");value.setIsActive(true);value.setCreatedAt(OffsetDateTime.now());return courses.saveAndFlush(value);}
    private void assign(Course c,User u){var assignment=new CourseLecturer();assignment.setId(new CourseLecturerId(c.getId(),u.getId()));assignment.setAssignedAt(OffsetDateTime.now());lecturers.saveAndFlush(assignment);}
    private byte[] docx(String text) throws Exception {var output=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(output)){zip.putNextEntry(new ZipEntry("word/document.xml"));zip.write(("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body><w:p><w:r><w:t>"+text+"</w:t></w:r></w:p></w:body></w:document>").getBytes(StandardCharsets.UTF_8));zip.closeEntry();}return output.toByteArray();}
}
