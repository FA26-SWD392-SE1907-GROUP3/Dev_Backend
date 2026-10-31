package com.example.swd392_se1907_aives.modules.teaching.api;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Course and membership queries used when scheduling or executing an examination. */
public interface CourseCatalog {
    Optional<CourseInfo> findById(UUID id);
    /** Joins the caller's transaction; callers retain the lock through their mutation. */
    Optional<CourseInfo> findLockedById(UUID id);
    List<UUID> assignedCourseIds(UUID lecturerId);
    boolean isEnrolled(UUID courseId,UUID studentId);
}
