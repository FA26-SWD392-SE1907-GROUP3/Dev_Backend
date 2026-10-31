package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.CourseView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.CreateCourse;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.LecturerView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.StudentView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.UserIdRequest;

import java.util.List;
import java.util.UUID;

public interface CourseService {
    List<CourseView> list(UUID lecturerId);

    List<StudentView> students(UUID courseId);

    List<LecturerView> assignedLecturers(UUID courseId);

    CourseView create(CreateCourse request);

    CourseView update(UUID id, CreateCourse request);

    void remove(UUID id);

    CourseView restore(UUID id);

    void unassign(UUID courseId, UUID lecturerId);

    void replaceLecturer(UUID courseId, UUID lecturerId, UserIdRequest request);

    void unenroll(UUID courseId, UUID studentId);

    void assignLecturer(UUID courseId, UserIdRequest request);

    void enroll(UUID courseId, UserIdRequest request);
}
