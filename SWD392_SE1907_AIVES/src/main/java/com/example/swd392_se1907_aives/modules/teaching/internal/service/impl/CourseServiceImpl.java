package com.example.swd392_se1907_aives.modules.teaching.internal.service.impl;

import com.example.swd392_se1907_aives.modules.identity.api.Account;
import com.example.swd392_se1907_aives.modules.identity.api.IdentityDirectory;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.api.TeachingUsagePolicy;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Course;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollment;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollmentId;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseLecturer;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseLecturerId;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.CourseView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.CreateCourse;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.LecturerView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.StudentView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.UserIdRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseEnrollmentRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseLecturerRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.CourseService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {
    private final CourseRepository courses;
    private final IdentityDirectory users;
    private final CourseLecturerRepository lecturers;
    private final CourseEnrollmentRepository enrollments;
    private final CourseAccessService access;
    private final TeachingUsagePolicy usage;

    private CourseView view(Course c) {
        return new CourseView(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getDescription(),
                Boolean.TRUE.equals(c.getIsActive()));
    }

    @Override
    public List<CourseView> list(UUID lecturerId) {
        Account user = access.current();
        if (lecturerId != null) {
            if (!"ADMIN".equals(user.getRole()))
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Chỉ Admin quản lý môn học theo giảng viên.");
            users.findById(lecturerId)
                    .filter(u -> "LECTURER".equals(u.getRole()))
                    .orElseThrow(
                            () ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND, "Không tìm thấy giảng viên."));
            return courses
                    .findAllById(
                            lecturers.findByIdLecturerId(lecturerId).stream()
                                    .map(x -> x.getId().getCourseId())
                                    .toList())
                    .stream()
                    .sorted(Comparator.comparing(Course::getCode))
                    .map(this::view)
                    .toList();
        }
        List<Course> visible =
                switch (user.getRole()) {
                    case "ADMIN" -> courses.findAll();
                    case "LECTURER" ->
                            courses.findAllById(
                                    lecturers.findByIdLecturerId(user.getId()).stream()
                                            .map(x -> x.getId().getCourseId())
                                            .toList());
                    case "STUDENT" ->
                            courses.findAllById(
                                    enrollments
                                            .findByIdStudentIdAndEnrollmentStatus(
                                                    user.getId(), "ACTIVE")
                                            .stream()
                                            .map(x -> x.getId().getCourseId())
                                            .toList());
                    default ->
                            throw new ResponseStatusException(
                                    HttpStatus.FORBIDDEN, "Invalid user role");
                };
        return visible.stream()
                .filter(c -> "ADMIN".equals(user.getRole()) || Boolean.TRUE.equals(c.getIsActive()))
                .sorted(Comparator.comparing(Course::getCode))
                .map(this::view)
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public List<StudentView> students(UUID courseId) {
        ensureCourse(courseId);
        access.requireTeacher(courseId);
        return enrollments.findByIdCourseId(courseId).stream()
                .map(
                        enrollment -> {
                            var user =
                                    users.findById(enrollment.getId().getStudentId()).orElseThrow();
                            return new StudentView(
                                    user.getId(),
                                    user.getFullName(),
                                    user.getEmail(),
                                    enrollment.getEnrollmentStatus(),
                                    Boolean.TRUE.equals(user.getIsActive()));
                        })
                .sorted(Comparator.comparing(StudentView::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public List<LecturerView> assignedLecturers(UUID courseId) {
        ensureCourse(courseId);
        access.requireTeacher(courseId);
        return users
                .findAllById(
                        lecturers.findByIdCourseId(courseId).stream()
                                .map(x -> x.getId().getLecturerId())
                                .toList())
                .stream()
                .map(u -> new LecturerView(u.getId(), u.getFullName(), u.getEmail()))
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public CourseView create(CreateCourse request) {
        if (courses.existsByCodeIgnoreCase(request.code().trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã môn học đã tồn tại.");
        Course c = new Course();
        c.setCode(request.code().trim());
        c.setName(request.name().trim());
        c.setDescription(request.description());
        c.setIsActive(true);
        c.setCreatedAt(OffsetDateTime.now());
        return view(courses.save(c));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CourseView update(UUID id, CreateCourse request) {
        var c =
                courses.findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy môn học."));
        if (courses.existsByCodeIgnoreCaseAndIdNot(request.code().trim(), id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã môn học đã tồn tại.");
        c.setCode(request.code().trim());
        c.setName(request.name().trim());
        c.setDescription(request.description());
        return view(courses.save(c));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void remove(UUID id) {
        var c =
                courses.findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy môn học."));
        if (usage.hasOpenExams(id))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Môn học còn kỳ thi đang mở hoặc đã lên lịch. Kết thúc kỳ thi trước khi ngừng"
                            + " sử dụng môn.");
        c.setIsActive(false);
        courses.save(c);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CourseView restore(UUID id) {
        var c =
                courses.findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy môn học."));
        c.setIsActive(true);
        return view(courses.save(c));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void unassign(UUID courseId, UUID lecturerId) {
        courses.findLockedById(courseId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Không tìm thấy môn học."));
        if (usage.hasOpenExams(courseId))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không gỡ giảng viên khi môn còn kỳ thi đang mở hoặc đã lên lịch.");
        lecturers.deleteById(new CourseLecturerId(courseId, lecturerId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void replaceLecturer(UUID courseId, UUID lecturerId, UserIdRequest request) {
        courses.findLockedById(courseId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Không tìm thấy môn học."));
        if (!lecturers.existsById(new CourseLecturerId(courseId, lecturerId)))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Phân công không còn tồn tại.");
        var replacement = user(request.email(), "LECTURER");
        if (replacement.getId().equals(lecturerId)) return;
        unassign(courseId, lecturerId);
        var link = new CourseLecturer();
        link.setId(new CourseLecturerId(courseId, replacement.getId()));
        link.setAssignedAt(OffsetDateTime.now());
        lecturers.save(link);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void unenroll(UUID courseId, UUID studentId) {
        courses.findLockedById(courseId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Không tìm thấy môn học."));
        if (usage.hasStudentInProgress(courseId, studentId))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sinh viên đang thi môn này. Chỉ ngừng ghi danh khi lượt thi kết thúc.");
        var ce =
                enrollments
                        .findById(new CourseEnrollmentId(courseId, studentId))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy ghi danh."));
        ce.setEnrollmentStatus("DROPPED");
        enrollments.save(ce);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void assignLecturer(UUID courseId, UserIdRequest request) {
        ensureCourse(courseId);
        Account u = user(request.email(), "LECTURER");
        CourseLecturer cl = new CourseLecturer();
        cl.setId(new CourseLecturerId(courseId, u.getId()));
        lecturers.save(cl);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void enroll(UUID courseId, UserIdRequest request) {
        ensureCourse(courseId);
        access.requireTeacher(courseId);
        Account u = user(request.email(), "STUDENT");
        courses.findLockedById(courseId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Course not found"));
        var id = new CourseEnrollmentId(courseId, u.getId());
        CourseEnrollment ce =
                enrollments
                        .findById(id)
                        .orElseGet(
                                () -> {
                                    var value = new CourseEnrollment();
                                    value.setId(id);
                                    return value;
                                });
        ce.setEnrollmentStatus("ACTIVE");
        if (ce.getEnrolledAt() == null) ce.setEnrolledAt(OffsetDateTime.now());
        enrollments.save(ce);
    }

    private void ensureCourse(UUID id) {
        if (!courses.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
    }

    private Account user(String email, String role) {
        Account u =
                users.findByEmailIgnoreCase(email.trim())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Email not found"));
        if (!role.equals(u.getRole()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user role");
        if (!Boolean.TRUE.equals(u.getIsActive()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is inactive");
        return u;
    }
}
