package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;

import com.example.swd392_se1907_aives.modules.identity.api.Account;
import com.example.swd392_se1907_aives.modules.identity.api.IdentityDirectory;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollmentId;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseLecturerId;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseEnrollmentRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseLecturerRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class DefaultCourseAccessService implements CourseAccessService {
    private final CourseLecturerRepository lecturers;
    private final CourseEnrollmentRepository enrollments;
    private final IdentityDirectory identities;
    public Account current(){return identities.current();}
    public void requireTeacher(UUID courseId){
        var user=current();
        if("ADMIN".equals(user.getRole()))return;
        if(!"LECTURER".equals(user.getRole()) || !lecturers.existsById(new CourseLecturerId(courseId,user.getId())))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bạn chưa được phân công môn học này. Hãy liên hệ quản trị viên để được phân công.");
    }
    public void requireEnrolled(UUID courseId){
        var user=current();
        if("ADMIN".equals(user.getRole()))return;
        if(!"STUDENT".equals(user.getRole()) || !enrollments.findById(new CourseEnrollmentId(courseId,user.getId())).map(x -> "ACTIVE".equals(x.getEnrollmentStatus())).orElse(false))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bạn chưa được ghi danh hoặc đã ngừng học môn của kỳ thi. Hãy nhờ Admin ghi danh lại vào môn học trước khi vào thi.");
    }
}
