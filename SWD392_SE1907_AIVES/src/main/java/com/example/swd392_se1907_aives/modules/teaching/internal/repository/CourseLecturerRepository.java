package com.example.swd392_se1907_aives.modules.teaching.internal.repository;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseLecturer;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseLecturerId;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface CourseLecturerRepository extends JpaRepository<CourseLecturer, CourseLecturerId> {
    java.util.List<CourseLecturer> findByIdLecturerId(UUID lecturerId);
    java.util.List<CourseLecturer> findByIdCourseId(UUID courseId);



}
