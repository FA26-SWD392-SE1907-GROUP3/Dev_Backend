package com.example.swd392_se1907_aives.controller;

import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.repository.*;
import com.example.swd392_se1907_aives.service.CourseAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/exams") @PreAuthorize("hasRole('LECTURER')")
public class TeacherExamManagementController {
    private final ExamRepository exams;private final CourseRepository courses;private final CourseAccessService access;
    private final ExamQuestionPoolRepository pool;private final QuestionRepository questions;private final UserRepository users;
    private final ExamCandidateRepository candidates;private final ExamAttemptRepository attempts;
    public record PoolView(UUID id,String content,String bloomLevel,String difficulty,String status){}
    public record CandidateView(UUID id,UUID studentId,String fullName,String studentEmail,OffsetDateTime scheduledStart,OffsetDateTime scheduledEnd,String status){}
    public record Schedule(@NotNull OffsetDateTime scheduledStart,@NotNull OffsetDateTime scheduledEnd){}
    @GetMapping("/{id}/questions")
    public List<PoolView> pool(@PathVariable UUID id){
        Exam e=exam(id,false);return pool.findByIdExamId(id).stream().map(link->questions.findById(link.getId().getQuestionId()).orElseThrow())
            .filter(q->e.getCourseId().equals(q.getCourseId())).map(q->new PoolView(q.getId(),q.getContent(),q.getBloomLevel(),q.getDifficulty(),q.getStatus())).toList();
    }
    @DeleteMapping("/{id}/questions/{questionId}") @Transactional
    public void removeQuestion(@PathVariable UUID id,@PathVariable UUID questionId){
        draft(exam(id,true));pool.deleteById(new ExamQuestionPoolId(id,questionId));
    }
    @PutMapping("/{id}") @Transactional
    public ExamController.ExamView update(@PathVariable UUID id,@Valid @RequestBody ExamController.ExamCreate req){
        Exam e=exam(id,true);draft(e);
        if(!e.getCourseId().equals(req.courseId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không thể đổi môn học của kỳ thi đã tạo.");
        e.setTitle(req.title());e.setMainQuestionCount(req.mainQuestionCount());e.setMaxFollowupsPerQuestion(req.maxFollowupsPerQuestion()==null?2:req.maxFollowupsPerQuestion());e.setMaxFollowupsPerStudent(req.maxFollowupsPerStudent()==null?Math.min(100,e.getMaxFollowupsPerQuestion()*req.mainQuestionCount()):req.maxFollowupsPerStudent());
        e.setSelectionMode(req.selectionMode()==null?"RANDOM":req.selectionMode());e.setAnswerTimeLimitSeconds(req.answerTimeLimitSeconds()==null?120:req.answerTimeLimitSeconds());e.setUpdatedAt(OffsetDateTime.now());
        exams.save(e);return ExamController.view(e);
    }
    @GetMapping("/{id}/candidates")
    public List<CandidateView> candidates(@PathVariable UUID id){
        exam(id,false);return candidates.findByExamIdOrderByScheduledStartAsc(id).stream().map(this::view).toList();
    }
    @PutMapping("/{id}/candidates/{candidateId}") @Transactional
    public CandidateView reschedule(@PathVariable UUID id,@PathVariable UUID candidateId,@Valid @RequestBody Schedule req){
        Exam e=exam(id,true);available(e);
        if(!req.scheduledStart().isBefore(req.scheduledEnd()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Thời gian kết thúc phải sau thời gian bắt đầu.");
        ExamCandidate c=editableCandidate(id,candidateId);c.setScheduledStart(req.scheduledStart());c.setScheduledEnd(req.scheduledEnd());return view(candidates.save(c));
    }
    @DeleteMapping("/{id}/candidates/{candidateId}") @Transactional
    public void removeCandidate(@PathVariable UUID id,@PathVariable UUID candidateId){
        available(exam(id,true));candidates.delete(editableCandidate(id,candidateId));
    }
    @DeleteMapping("/{id}") @Transactional
    public void removeDraft(@PathVariable("id") UUID id){
        Exam e=exam(id,true);draft(e);
        if(attempts.existsByExamId(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Không xóa kỳ thi đã có lượt thi.");
        pool.deleteAll(pool.findByIdExamId(id));candidates.deleteAll(candidates.findByExamIdOrderByScheduledStartAsc(id));exams.delete(e);
    }
    @PatchMapping("/{id}/close") @Transactional
    public ExamController.ExamView close(@PathVariable UUID id){
        Exam e=exam(id,true);
        if(!"ACTIVE".equals(e.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Chỉ kết thúc kỳ thi đang mở.");
        if(attempts.existsByExamIdAndStatus(id,"IN_PROGRESS"))throw new ResponseStatusException(HttpStatus.CONFLICT,"Còn sinh viên đang thi. Chỉ kết thúc kỳ thi sau khi các lượt thi hoàn tất.");
        e.setStatus("CLOSED");e.setUpdatedAt(OffsetDateTime.now());exams.save(e);
        return ExamController.view(e);
    }
    private ExamCandidate editableCandidate(UUID examId,UUID id){
        ExamCandidate c=candidates.findLockedById(id).filter(x->examId.equals(x.getExamId())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy thí sinh trong kỳ thi."));
        if(!Set.of("REGISTERED","READY").contains(c.getStatus())||attempts.existsByCandidateId(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể sửa/xóa lịch của sinh viên đã bắt đầu thi.");
        return c;
    }
    private CandidateView view(ExamCandidate c){var user=users.findById(c.getStudentId()).orElseThrow();return new CandidateView(c.getId(),user.getId(),user.getFullName(),user.getEmail(),c.getScheduledStart(),c.getScheduledEnd(),c.getStatus());}
    private void available(Exam e){if(!Set.of("DRAFT","SCHEDULED","ACTIVE").contains(e.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Kỳ thi đã kết thúc hoặc đã hủy.");}
    private void draft(Exam e){if(!"DRAFT".equals(e.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Chỉ sửa đề thi khi kỳ thi còn nháp.");}
    private Exam exam(UUID id,boolean lock){
        UUID courseId=exams.findCourseIdById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy kỳ thi."));access.requireTeacher(courseId);
        if(lock){courses.findLockedById(courseId).orElseThrow();return exams.findLockedById(id).orElseThrow();}return exams.findById(id).orElseThrow();
    }
}
