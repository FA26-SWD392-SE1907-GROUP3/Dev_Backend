package com.example.swd392_se1907_aives.modules.teaching.api;

import com.example.swd392_se1907_aives.modules.identity.api.Account;
import java.util.UUID;

/** Authorization boundary for teaching content and course memberships. */
public interface CourseAccessService {
    Account current();
    void requireTeacher(UUID courseId);
    void requireEnrolled(UUID courseId);
}
