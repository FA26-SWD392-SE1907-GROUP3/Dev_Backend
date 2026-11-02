package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamCandidate;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPoolId;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamCreate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.CandidateView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.PoolView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.Schedule;
import com.example.swd392_se1907_aives.modules.examination.internal.mapper.ExamMapper;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamQuestionPoolRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.service.TeacherExamManagementService;
import com.example.swd392_se1907_aives.modules.identity.api.IdentityDirectory;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseCatalog;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionCatalog;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('LECTURER')")
public class TeacherExamManagementServiceImpl implements TeacherExamManagementService {
    private final ExamRepository exams;
    private final CourseCatalog courses;
    private final CourseAccessService access;
    private final ExamQuestionPoolRepository pool;
    private final QuestionCatalog questions;
    private final IdentityDirectory users;
    private final ExamCandidateRepository candidates;
    private final ExamAttemptRepository attempts;
    private final com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRetakeGrantRepository retakes;

    @Override
    public List<PoolView> pool(UUID id) {
        Exam exam = exam(id, false);
        return pool.findByIdExamId(id).stream()
                .map(link -> questions.findById(link.getId().getQuestionId()).orElseThrow())
                .filter(q -> exam.getCourseId().equals(q.getCourseId()))
                .map(q -> new PoolView(q.getId(), q.getContent(), q.getDifficulty(), q.getStatus()))
                .toList();
    }

    @Override
    @Transactional
    public void removeQuestion(UUID id, UUID questionId) {
        draft(exam(id, true));
        pool.deleteById(new ExamQuestionPoolId(id, questionId));
    }

    @Override
    @Transactional
    public ExamView update(UUID id, ExamCreate request) {
        Exam exam = exam(id, true);
        draft(exam);
        if (!exam.getCourseId().equals(request.courseId()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Không thể đổi môn học của kỳ thi đã tạo.");
        ExamMapper.applySettings(exam, request);
        exam.setUpdatedAt(OffsetDateTime.now());
        exams.save(exam);
        return ExamMapper.view(exam);
    }

    @Override
    public List<CandidateView> candidates(UUID id) {
        exam(id, false);
        return candidates.findByExamIdOrderByScheduledStartAsc(id).stream()
                .map(this::view)
                .toList();
    }

    @Override
    @Transactional
    public CandidateView reschedule(UUID id, UUID candidateId, Schedule request) {
        Exam exam = exam(id, true);
        available(exam);
        if (!request.scheduledStart().isBefore(request.scheduledEnd()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu.");
        ExamCandidate c = editableCandidate(id, candidateId);
        c.setScheduledStart(request.scheduledStart());
        c.setScheduledEnd(request.scheduledEnd());
        return view(candidates.save(c));
    }

    @Override
    @Transactional
    public CandidateView grantRetake(UUID id, UUID candidateId,
            com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.RetakeRequest request) {
        Exam exam = exam(id, true);
        if (!Set.of("ACTIVE", "CLOSED").contains(exam.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ cấp thi lại cho kỳ thi đang mở hoặc đã đóng.");
        var candidate = candidates.findById(candidateId).filter(c -> id.equals(c.getExamId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thí sinh trong kỳ thi."));
        var history = attempts.findByCandidateIdOrderByAttemptNoAsc(candidateId).stream()
                .map(a -> attempts.findLockedById(a.getId()).orElseThrow()).toList();
        candidate = candidates.findLockedById(candidateId).orElseThrow();
        if (history.isEmpty() || !history.getLast().getId().equals(request.previousAttemptId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lịch sử thi đã thay đổi. Hãy tải lại danh sách thí sinh.");
        if (history.stream().anyMatch(a -> Set.of("READY", "IN_PROGRESS").contains(a.getStatus()))
                || !Set.of("COMPLETED", "ABORTED", "TIMED_OUT").contains(history.getLast().getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Thí sinh vẫn còn lượt đang thi. Hãy tiếp tục hoặc hủy lượt đó trước khi cấp thi lại.");
        var now = OffsetDateTime.now();
        if (!request.scheduledStart().isBefore(request.scheduledEnd()) || !request.scheduledEnd().isAfter(now))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn khung giờ thi lại hợp lệ, kết thúc phải ở tương lai và sau giờ bắt đầu.");
        var user = users.findById(candidate.getStudentId()).orElseThrow();
        if (!"STUDENT".equals(user.getRole()) || !Boolean.TRUE.equals(user.getIsActive())
                || !courses.isEnrolled(exam.getCourseId(), user.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sinh viên phải đang hoạt động và được ghi danh vào môn học.");
        var pending = retakes.findFirstByCandidateIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByGrantedAtDesc(candidateId);
        if (pending.isPresent()) {
            var old = pending.get();
            if (old.getScheduledEnd().isAfter(now) && !"CANCELLED".equals(candidate.getStatus()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Đã có một lịch thi lại chưa sử dụng. Hãy hủy lịch đó trước khi cấp lịch khác.");
            old.setRevokedAt(now); retakes.saveAndFlush(old);
        }
        var grant = new com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamRetakeGrant();
        grant.setCandidateId(candidateId); grant.setPreviousAttemptId(request.previousAttemptId());
        grant.setNextAttemptNo(history.getLast().getAttemptNo() + 1); grant.setGrantedBy(access.current().getId());
        grant.setReason(request.reason().strip()); grant.setScheduledStart(request.scheduledStart());
        grant.setScheduledEnd(request.scheduledEnd()); grant.setGrantedAt(now); retakes.saveAndFlush(grant);
        candidate.setScheduledStart(request.scheduledStart()); candidate.setScheduledEnd(request.scheduledEnd());
        candidate.setStatus("READY"); candidates.save(candidate);
        return view(candidate);
    }

    @Override
    public List<com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.RetakeView> retakes(UUID id, UUID candidateId) {
        exam(id, false);
        candidates.findById(candidateId).filter(c -> id.equals(c.getExamId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thí sinh trong kỳ thi."));
        return retakes.findByCandidateIdOrderByGrantedAtAsc(candidateId).stream().map(g ->
                new com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.RetakeView(
                        g.getId(), g.getPreviousAttemptId(), g.getNextAttemptNo(), g.getGrantedBy(), g.getReason(),
                        g.getScheduledStart(), g.getScheduledEnd(), g.getGrantedAt(), g.getConsumedAt(), g.getRevokedAt())).toList();
    }

    @Override
    @Transactional
    public void cancelCandidate(UUID id, UUID candidateId) {
        Exam exam = exam(id, true);
        if (!"CLOSED".equals(exam.getStatus())) available(exam);
        else if (retakes.findFirstByCandidateIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByGrantedAtDesc(candidateId).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Kỳ thi đã đóng. Chỉ có thể hủy lịch thi lại chưa sử dụng.");
        var candidate =
                candidates
                        .findById(candidateId)
                        .filter(c -> id.equals(c.getExamId()))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Không tìm thấy thí sinh trong kỳ thi."));
        // Lock attempts before the candidate: interview transitions update them in that order.
        var history =
                attempts.findByCandidateIdOrderByAttemptNoAsc(candidateId).stream()
                        .map(attempt -> attempts.findLockedById(attempt.getId()).orElseThrow())
                        .toList();
        candidate = candidates.findLockedById(candidateId).orElseThrow();
        if ("CANCELLED".equals(candidate.getStatus())) return;
        if ("COMPLETED".equals(candidate.getStatus())
                || !history.isEmpty() && "COMPLETED".equals(history.getLast().getStatus())
                        && !retakes.findFirstByCandidateIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByGrantedAtDesc(candidateId).isPresent())
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sinh viên đã hoàn thành lượt thi. Giữ nguyên lịch sử kết quả.");
        var now = OffsetDateTime.now();
        for (var attempt : history)
            if (Set.of("READY", "IN_PROGRESS").contains(attempt.getStatus())) {
                attempt.setStatus("ABORTED");
                attempt.setEndedAt(now);
                attempt.setLastActivityAt(now);
                attempts.save(attempt);
            }
        retakes.findFirstByCandidateIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByGrantedAtDesc(candidateId)
                .ifPresent(grant -> { grant.setRevokedAt(now); retakes.save(grant); });
        candidate.setStatus("CANCELLED");
        candidates.save(candidate);
    }

    @Override
    @Transactional
    public void removeDraft(UUID id) {
        Exam exam = exam(id, true);
        draft(exam);
        if (attempts.existsByExamId(id))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Không xóa kỳ thi đã có lượt thi.");
        pool.deleteAll(pool.findByIdExamId(id));
        candidates.deleteAll(candidates.findByExamIdOrderByScheduledStartAsc(id));
        exams.delete(exam);
    }

    @Override
    @Transactional
    public ExamView close(UUID id) {
        Exam exam = exam(id, true);
        if (!"ACTIVE".equals(exam.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ kết thúc kỳ thi đang mở.");
        if (attempts.existsByExamIdAndStatus(id, "IN_PROGRESS"))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Còn sinh viên đang thi. Chỉ kết thúc kỳ thi sau khi các lượt thi hoàn tất.");
        exam.setStatus("CLOSED");
        exam.setUpdatedAt(OffsetDateTime.now());
        exams.save(exam);
        return ExamMapper.view(exam);
    }

    private ExamCandidate editableCandidate(UUID examId, UUID id) {
        ExamCandidate c =
                candidates
                        .findLockedById(id)
                        .filter(x -> examId.equals(x.getExamId()))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Không tìm thấy thí sinh trong kỳ thi."));
        if (!Set.of("REGISTERED", "READY").contains(c.getStatus())
                || attempts.existsByCandidateId(id))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Không thể sửa/xóa lịch của sinh viên đã bắt đầu thi.");
        return c;
    }

    private CandidateView view(ExamCandidate c) {
        var user = users.findById(c.getStudentId()).orElseThrow();
        var history = attempts.findByCandidateIdOrderByAttemptNoAsc(c.getId());
        var latest = history.isEmpty() ? null : history.getLast();
        var grant = retakes.findFirstByCandidateIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByGrantedAtDesc(c.getId()).orElse(null);
        boolean pending = grant != null && grant.getScheduledEnd().isAfter(OffsetDateTime.now()) && "READY".equals(c.getStatus());
        boolean canRetake = latest != null && Set.of("COMPLETED", "ABORTED", "TIMED_OUT").contains(latest.getStatus()) && !pending;
        return new CandidateView(
                c.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                c.getScheduledStart(),
                c.getScheduledEnd(),
                c.getStatus(), latest == null ? null : latest.getId(), canRetake, pending, grant == null ? null : grant.getReason());
    }

    private void available(Exam exam) {
        if (!Set.of("DRAFT", "SCHEDULED", "ACTIVE").contains(exam.getStatus()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Kỳ thi đã kết thúc hoặc đã hủy.");
    }

    private void draft(Exam exam) {
        if (!"DRAFT".equals(exam.getStatus()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Chỉ sửa đề thi khi kỳ thi còn nháp.");
    }

    private Exam exam(UUID id, boolean lock) {
        UUID courseId =
                exams.findCourseIdById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy kỳ thi."));
        access.requireTeacher(courseId);
        if (lock) {
            courses.findLockedById(courseId).orElseThrow();
            return exams.findLockedById(id).orElseThrow();
        }
        return exams.findById(id).orElseThrow();
    }
}
