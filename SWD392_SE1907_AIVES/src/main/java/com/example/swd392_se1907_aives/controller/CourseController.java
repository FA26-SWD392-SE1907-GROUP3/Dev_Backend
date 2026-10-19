package com.example.swd392_se1907_aives.controller;
import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.repository.*;
import com.example.swd392_se1907_aives.service.CourseAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/courses")
public class CourseController {
    private final CourseRepository courses;
    private final UserRepository users;
    private final CourseLecturerRepository lecturers;
    private final CourseEnrollmentRepository enrollments;
    private final CourseAccessService access;
    private final ExamRepository exams;
    private final ExamCandidateRepository candidates;
    private final ExamAttemptRepository attempts;
    public record CreateCourse(@NotBlank @Size(max=30) String code,@NotBlank @Size(max=200) String name,@Size(max=2000) String description){}
    public record CourseView(UUID id,String code,String name,String description,boolean active){}
    private CourseView view(Course c){return new CourseView(c.getId(),c.getCode(),c.getName(),c.getDescription(),Boolean.TRUE.equals(c.getIsActive()));}
    @GetMapping public List<CourseView> list(@RequestParam(required=false) UUID lecturerId){
        User user=access.current();
        if(lecturerId!=null){
            if(!"ADMIN".equals(user.getRole()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ Admin quản lý môn học theo giảng viên.");
            users.findById(lecturerId).filter(u->"LECTURER".equals(u.getRole())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy giảng viên."));
            return courses.findAllById(lecturers.findByIdLecturerId(lecturerId).stream().map(x->x.getId().getCourseId()).toList()).stream().sorted(Comparator.comparing(Course::getCode)).map(this::view).toList();
        }
        List<Course> visible=switch(user.getRole()) {
            case "ADMIN" -> courses.findAll();
            case "LECTURER" -> courses.findAllById(lecturers.findByIdLecturerId(user.getId()).stream().map(x->x.getId().getCourseId()).toList());
            case "STUDENT" -> courses.findAllById(enrollments.findByIdStudentIdAndEnrollmentStatus(user.getId(),"ACTIVE").stream().map(x->x.getId().getCourseId()).toList());
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Invalid user role");
        };
        return visible.stream().filter(c->"ADMIN".equals(user.getRole())||Boolean.TRUE.equals(c.getIsActive())).sorted(Comparator.comparing(Course::getCode)).map(this::view).toList();
    }
    public record LecturerView(UUID id,String fullName,String email){}
    public record StudentView(UUID id,String fullName,String email,String enrollmentStatus,boolean active){}
    @GetMapping("/{courseId}/students") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public List<StudentView> students(@PathVariable UUID courseId){
        ensureCourse(courseId);access.requireTeacher(courseId);
        return enrollments.findByIdCourseId(courseId).stream().map(enrollment->{var user=users.findById(enrollment.getId().getStudentId()).orElseThrow();return new StudentView(user.getId(),user.getFullName(),user.getEmail(),enrollment.getEnrollmentStatus(),Boolean.TRUE.equals(user.getIsActive()));})
            .sorted(Comparator.comparing(StudentView::fullName,String.CASE_INSENSITIVE_ORDER)).toList();
    }
    @GetMapping("/{courseId}/lecturers") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public List<LecturerView> assignedLecturers(@PathVariable UUID courseId){
        ensureCourse(courseId);access.requireTeacher(courseId);
        return users.findAllById(lecturers.findByIdCourseId(courseId).stream().map(x->x.getId().getLecturerId()).toList())
            .stream().map(u->new LecturerView(u.getId(),u.getFullName(),u.getEmail())).toList();
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')") public CourseView create(@Valid @RequestBody CreateCourse req){
        if(courses.existsByCodeIgnoreCase(req.code().trim()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Mã môn học đã tồn tại.");
        Course c=new Course();c.setCode(req.code().trim());c.setName(req.name().trim());c.setDescription(req.description());c.setIsActive(true);
        c.setCreatedAt(OffsetDateTime.now());
        return view(courses.save(c));
    }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public CourseView update(@PathVariable("id") UUID id,@Valid @RequestBody CreateCourse req){
        var c=courses.findLockedById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy môn học."));
        if(courses.existsByCodeIgnoreCaseAndIdNot(req.code().trim(),id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Mã môn học đã tồn tại.");
        c.setCode(req.code().trim());c.setName(req.name().trim());c.setDescription(req.description());return view(courses.save(c));
    }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public void remove(@PathVariable("id") UUID id){
        var c=courses.findLockedById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy môn học."));
        if(exams.existsByCourseIdAndStatusIn(id,List.of("ACTIVE","SCHEDULED")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Môn học còn kỳ thi đang mở hoặc đã lên lịch. Kết thúc kỳ thi trước khi ngừng sử dụng môn.");
        c.setIsActive(false);courses.save(c);
    }
    @PatchMapping("/{id}/restore") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public CourseView restore(@PathVariable("id") UUID id){var c=courses.findLockedById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy môn học."));c.setIsActive(true);return view(courses.save(c));}
    @DeleteMapping("/{courseId}/lecturers/{lecturerId}") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public void unassign(@PathVariable("courseId") UUID courseId,@PathVariable("lecturerId") UUID lecturerId){
        courses.findLockedById(courseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy môn học."));
        if(exams.existsByCourseIdAndStatusIn(courseId,List.of("ACTIVE","SCHEDULED")))throw new ResponseStatusException(HttpStatus.CONFLICT,"Không gỡ giảng viên khi môn còn kỳ thi đang mở hoặc đã lên lịch.");
        lecturers.deleteById(new CourseLecturerId(courseId,lecturerId));
    }
    @PutMapping("/{courseId}/lecturers/{lecturerId}") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public void replaceLecturer(@PathVariable("courseId") UUID courseId,@PathVariable("lecturerId") UUID lecturerId,@Valid @RequestBody UserIdRequest req){
        courses.findLockedById(courseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy môn học."));
        if(!lecturers.existsById(new CourseLecturerId(courseId,lecturerId)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Phân công không còn tồn tại.");
        var replacement=user(req.email(),"LECTURER");
        if(replacement.getId().equals(lecturerId))return;
        unassign(courseId,lecturerId);var link=new CourseLecturer();link.setId(new CourseLecturerId(courseId,replacement.getId()));link.setAssignedAt(OffsetDateTime.now());lecturers.save(link);
    }
    @DeleteMapping("/{courseId}/students/{studentId}") @PreAuthorize("hasRole('ADMIN')") @Transactional
    public void unenroll(@PathVariable("courseId") UUID courseId,@PathVariable("studentId") UUID studentId){
        courses.findLockedById(courseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy môn học."));
        for(var c:candidates.findByStudentId(studentId))if(courseId.equals(c.getCourseId())&&attempts.existsByCandidateIdAndStatus(c.getId(),"IN_PROGRESS"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Sinh viên đang thi môn này. Chỉ ngừng ghi danh khi lượt thi kết thúc.");
        var ce=enrollments.findById(new CourseEnrollmentId(courseId,studentId)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy ghi danh."));ce.setEnrollmentStatus("DROPPED");enrollments.save(ce);
    }
    public record UserIdRequest(@NotBlank @Email String email){}
    @PostMapping("/{courseId}/lecturers") @PreAuthorize("hasRole('ADMIN')")
    public void assignLecturer(@PathVariable UUID courseId,@Valid @RequestBody UserIdRequest req){
        ensureCourse(courseId);User u=user(req.email(),"LECTURER");
        CourseLecturer cl=new CourseLecturer();cl.setId(new CourseLecturerId(courseId,u.getId()));lecturers.save(cl);
    }
    @PostMapping("/{courseId}/students") @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void enroll(@PathVariable UUID courseId,@Valid @RequestBody UserIdRequest req){
        ensureCourse(courseId);access.requireTeacher(courseId);User u=user(req.email(),"STUDENT");
        courses.findLockedById(courseId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Course not found"));
        var id=new CourseEnrollmentId(courseId,u.getId());
        CourseEnrollment ce=enrollments.findById(id).orElseGet(()->{var value=new CourseEnrollment();value.setId(id);return value;});
        ce.setEnrollmentStatus("ACTIVE");if(ce.getEnrolledAt()==null)ce.setEnrolledAt(OffsetDateTime.now());enrollments.save(ce);
    }
    private void ensureCourse(UUID id){if(!courses.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Course not found");}
    private User user(String email,String role){User u=users.findByEmailIgnoreCase(email.trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Email not found"));
        if(!role.equals(u.getRole())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid user role");
        if(!Boolean.TRUE.equals(u.getIsActive()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Account is inactive");return u;}
}
