package com.example.swd392_se1907_aives.modules.teaching.internal.service;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseCatalog;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseInfo;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Course;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollmentId;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseEnrollmentRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseLecturerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class CourseCatalogService implements CourseCatalog {
    private final CourseRepository courses;
    private final CourseLecturerRepository lecturers;
    private final CourseEnrollmentRepository enrollments;
    @Override public Optional<CourseInfo> findById(UUID id) { return courses.findById(id).map(this::view); }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public Optional<CourseInfo> findLockedById(UUID id) { return courses.findLockedById(id).map(this::view); }
    @Override public List<UUID> assignedCourseIds(UUID lecturerId) { return lecturers.findByIdLecturerId(lecturerId).stream().map(link->link.getId().getCourseId()).toList(); }
    @Override public boolean isEnrolled(UUID courseId,UUID studentId) { return enrollments.findById(new CourseEnrollmentId(courseId,studentId)).map(link->"ACTIVE".equals(link.getEnrollmentStatus())).orElse(false); }
    private CourseInfo view(Course course) { return new CourseInfo(course.getId(),course.getCode(),course.getName(),course.getDescription(),course.getIsActive()); }
}
