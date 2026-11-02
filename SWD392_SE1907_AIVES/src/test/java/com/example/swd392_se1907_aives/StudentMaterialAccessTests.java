package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;
import com.example.swd392_se1907_aives.modules.identity.internal.repository.UserRepository;
import com.example.swd392_se1907_aives.modules.identity.internal.service.JwtService;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.*;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.*;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.MaterialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:student-materials;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.show-sql=false","app.interview.timeouts-enabled=false",
    "app.grading.enabled=false","JWT_SECRET=student_material_test_secret_at_least_32_characters"})
@AutoConfigureMockMvc
class StudentMaterialAccessTests {
    static final Path STORAGE=Path.of("target","test-student-materials-"+UUID.randomUUID()).toAbsolutePath().normalize();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("app.materials.storage-dir",STORAGE::toString);}
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JwtService jwt;
    @Autowired UserRepository users;@Autowired CourseRepository courses;@Autowired CourseLecturerRepository lecturers;
    @Autowired CourseEnrollmentRepository enrollments;@Autowired CourseMaterialRepository materials;@Autowired MaterialChunkRepository chunks;
    Course course;User teacher,student;String teacherToken,studentToken;
    @BeforeEach void setup(){
        teacher=user("LECTURER");student=user("STUDENT");teacherToken=token(teacher);studentToken=token(student);
        course=new Course();course.setCode("M"+UUID.randomUUID().toString().substring(0,10));course.setName("Student materials");course.setIsActive(true);course.setCreatedAt(OffsetDateTime.now());course=courses.saveAndFlush(course);
        var assignment=new CourseLecturer();assignment.setId(new CourseLecturerId(course.getId(),teacher.getId()));assignment.setAssignedAt(OffsetDateTime.now());lecturers.saveAndFlush(assignment);
        var enrollment=new CourseEnrollment();enrollment.setId(new CourseEnrollmentId(course.getId(),student.getId()));enrollment.setEnrollmentStatus("ACTIVE");enrollment.setEnrolledAt(OffsetDateTime.now());enrollments.saveAndFlush(enrollment);
    }
    @AfterAll static void cleanup()throws Exception{
        if(!STORAGE.startsWith(Path.of("target").toAbsolutePath().normalize()))throw new IllegalStateException("Unexpected test storage path");
        if(Files.exists(STORAGE))try(var paths=Files.walk(STORAGE)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
    }
    private User user(String role){var user=new User();user.setEmail(UUID.randomUUID()+"@example.com");user.setFullName("Materials "+role);user.setRole(role);user.setIsActive(true);user.setPasswordHash("unused");user.setCreatedAt(OffsetDateTime.now());user.setUpdatedAt(OffsetDateTime.now());return users.saveAndFlush(user);}
    private String token(User user){return "Bearer "+jwt.generateToken(user.getId().toString(),user.getEmail(),user.getRole());}
    private String upload(String name,byte[] bytes)throws Exception{
        return json.readTree(mvc.perform(multipart("/api/v1/courses/"+course.getId()+"/materials")
                .file(new MockMultipartFile("file",name,"application/octet-stream",bytes)).header("Authorization",teacherToken))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();
    }
    private byte[] slides(boolean modern,boolean withText)throws Exception{
        var out=new ByteArrayOutputStream();
        if(modern)try(var show=new org.apache.poi.xslf.usermodel.XMLSlideShow()){
            var slide=show.createSlide();if(withText){var text=slide.createTextBox();text.setAnchor(new Rectangle2D.Double(40,40,500,100));text.setText("Architecture responsibilities");}show.write(out);
        }else try(var show=new org.apache.poi.hslf.usermodel.HSLFSlideShow()){
            var slide=show.createSlide();if(withText){var text=slide.createTextBox();text.setAnchor(new Rectangle2D.Double(40,40,500,100));text.setText("Architecture responsibilities");}show.write(out);
        }
        return out.toByteArray();
    }
    private byte[] docx()throws Exception{
        var bytes=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(bytes)){zip.putNextEntry(new ZipEntry("word/document.xml"));zip.write("<document><p><t>Nội dung học tập cho sinh viên.</t></p></document>".getBytes(StandardCharsets.UTF_8));zip.closeEntry();}return bytes.toByteArray();
    }

    @Test void enrolledStudentsCanReadDownloadAndPreviewBothPowerPointFormatsWithoutIndexing()throws Exception{
        for(boolean modern:new boolean[]{false,true}){
            byte[] bytes=slides(modern,true);String name=modern?"lesson.pptx":"lesson.ppt",id=upload(name,bytes);
            mvc.perform(get("/api/v1/courses/"+course.getId()+"/materials").header("Authorization",studentToken)).andExpect(status().isOk());
            mvc.perform(get("/api/v1/materials/"+id+"/download").header("Authorization",studentToken)).andExpect(status().isOk()).andExpect(content().bytes(bytes));
            byte[] preview=mvc.perform(get("/api/v1/materials/"+id+"/preview").header("Authorization",studentToken))
                    .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                    .andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("inline"))).andReturn().getResponse().getContentAsByteArray();
            try(var pdf=org.apache.pdfbox.Loader.loadPDF(preview)){assertThat(pdf.getNumberOfPages()).isEqualTo(1);}
            assertThat(materials.findById(UUID.fromString(id)).orElseThrow().getExtractionStatus()).isEqualTo("UPLOADED");
            assertThat(chunks.findByMaterialIdOrderByChunkIndex(UUID.fromString(id))).isEmpty();
            mvc.perform(post("/api/v1/materials/"+id+"/index").header("Authorization",teacherToken)).andExpect(status().isOk()).andExpect(jsonPath("$.extractionStatus").value("READY"));
            assertThat(chunks.findByMaterialIdOrderByChunkIndex(UUID.fromString(id)).getFirst().getContent()).contains("Architecture responsibilities");
        }
    }
    @Test void studentsCanViewDocxTextAndTheOriginalFileWithoutChangingMetadata()throws Exception{
        byte[] bytes=docx();String id=upload("lecture.docx",bytes);
        mvc.perform(get("/api/v1/materials/"+id+"/text").header("Authorization",studentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Nội dung học tập cho sinh viên."));
        mvc.perform(get("/api/v1/materials/"+id+"/download").header("Authorization",studentToken)).andExpect(content().bytes(bytes));
        assertThat(materials.findById(UUID.fromString(id)).orElseThrow().getExtractionStatus()).isEqualTo("UPLOADED");
    }
    @Test void enrolledStudentCannotUploadRenameDeleteIndexOrGenerateQuestions()throws Exception{
        String id=upload("lecture.docx",docx());
        mvc.perform(multipart("/api/v1/courses/"+course.getId()+"/materials").file(new MockMultipartFile("file","student.docx","application/octet-stream",docx())).header("Authorization",studentToken)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/materials/"+id).header("Authorization",studentToken).contentType("application/json").content("{\"name\":\"changed.docx\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/materials/"+id).header("Authorization",studentToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/materials/"+id+"/index").header("Authorization",studentToken)).andExpect(status().isForbidden());
        assertThat(materials.findById(UUID.fromString(id)).orElseThrow().getOriginalFilename()).isEqualTo("lecture.docx");
        assertThat(chunks.findByMaterialIdOrderByChunkIndex(UUID.fromString(id))).isEmpty();
    }
    @Test void droppingEnrollmentRevokesAllReadAccessIncludingCachedPowerPointPreviews()throws Exception{
        String id=upload("lecture.pptx",slides(true,true));
        mvc.perform(get("/api/v1/materials/"+id+"/preview").header("Authorization",studentToken)).andExpect(status().isOk());
        enrollments.deleteById(new CourseEnrollmentId(course.getId(),student.getId()));enrollments.flush();
        for(String suffix:List.of("/download","/preview","/text","/chunks"))
            mvc.perform(get("/api/v1/materials/"+id+suffix).header("Authorization",studentToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/courses/"+course.getId()+"/materials").header("Authorization",studentToken)).andExpect(status().isForbidden());
    }
    @Test void strangersAndAnonymousUsersCannotReadFiles()throws Exception{
        String id=upload("lecture.docx",docx()),otherToken=token(user("STUDENT"));
        for(String suffix:List.of("/download","/preview","/text","/chunks")){
            mvc.perform(get("/api/v1/materials/"+id+suffix)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/v1/materials/"+id+suffix).header("Authorization",otherToken)).andExpect(status().isForbidden());
        }
    }
    @Test void visualPowerPointCanBeViewedEvenIfAiTextIndexingFails()throws Exception{
        String id=upload("visual.pptx",slides(true,false));
        mvc.perform(post("/api/v1/materials/"+id+"/index").header("Authorization",teacherToken)).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/materials/"+id+"/preview").header("Authorization",studentToken)).andExpect(status().isOk());
    }
    @Test void renameKeepsPowerPointExtensionAndRejectsDisguisedFormats()throws Exception{
        String id=upload("lecture.pptx",slides(true,true));
        mvc.perform(put("/api/v1/materials/"+id).header("Authorization",teacherToken).contentType("application/json").content("{\"name\":\"new-name.pptx\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.originalFilename").value("new-name.pptx"));
        mvc.perform(put("/api/v1/materials/"+id).header("Authorization",teacherToken).contentType("application/json").content("{\"name\":\"new-name.docx\"}")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/courses/"+course.getId()+"/materials").file(new MockMultipartFile("file","fake.ppt","application/vnd.ms-powerpoint","not-a-presentation".getBytes())).header("Authorization",teacherToken)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/courses/"+course.getId()+"/materials").file(new MockMultipartFile("file","fake.pptx",MaterialService.PPTX,docx())).header("Authorization",teacherToken)).andExpect(status().isUnprocessableEntity());
    }
    @Test void archivedCourseCannotBeReadByStudentsEvenWithAnActiveEnrollment()throws Exception{
        String id=upload("lecture.docx",docx());course.setIsActive(false);courses.saveAndFlush(course);
        mvc.perform(get("/api/v1/materials/"+id+"/download").header("Authorization",studentToken)).andExpect(status().isForbidden());
    }
}
