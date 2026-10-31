package com.example.swd392_se1907_aives.modules.teaching.internal.repository;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollment;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollmentId;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, CourseEnrollmentId> {
    java.util.List<CourseEnrollment> findByIdStudentIdAndEnrollmentStatus(UUID studentId, String status);
    java.util.List<CourseEnrollment> findByIdCourseId(UUID courseId);



}
