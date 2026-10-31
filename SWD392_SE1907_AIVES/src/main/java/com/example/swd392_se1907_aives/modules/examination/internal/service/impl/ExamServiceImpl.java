package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamCandidate;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPool;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPoolId;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.CandidateCreate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamCreate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.QuestionId;
import com.example.swd392_se1907_aives.modules.examination.internal.mapper.ExamMapper;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamQuestionPoolRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.service.ExamService;
import com.example.swd392_se1907_aives.modules.identity.api.Account;
import com.example.swd392_se1907_aives.modules.identity.api.IdentityDirectory;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseCatalog;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionCatalog;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionInfo;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {
    private final ExamRepository exams;
    private final QuestionCatalog questions;
    private final ExamQuestionPoolRepository pool;
    private final ExamCandidateRepository candidates;
    private final CourseAccessService access;
    private final IdentityDirectory users;
    private final CourseCatalog courses;

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    public ExamView create(ExamCreate request) {
        access.requireTeacher(request.courseId());
        Exam exam = new Exam();
        exam.setCourseId(request.courseId());
        exam.setCreatedBy(access.current().getId());
        ExamMapper.applySettings(exam, request);
        exam.setStatus("DRAFT");
        exam.setSlotDurationMinutes(20);
        exam.setCreatedAt(OffsetDateTime.now());
        exam.setUpdatedAt(OffsetDateTime.now());
        exam.setExamLanguage("vi");
        exam.setExamTimezone("Asia/Ho_Chi_Minh");
        return ExamMapper.view(exams.save(exam));
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public void addQuestion(UUID id, QuestionId request) {
        Exam exam = locked(id);
        if (!"DRAFT".equals(exam.getStatus()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Chỉ thêm câu hỏi khi kỳ thi còn nháp.");
        QuestionInfo q =
                questions
                        .findById(request.questionId())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Question not found"));
        if (!exam.getCourseId().equals(q.getCourseId()) || !"APPROVED".equals(q.getStatus()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Question must be approved and belong to same course");
        var poolId = new ExamQuestionPoolId(id, request.questionId());
        if (pool.existsById(poolId)) return;
        ExamQuestionPool item = new ExamQuestionPool();
        item.setId(poolId);
        item.setCourseId(exam.getCourseId());
        item.setSelectionWeight(1);
        item.setAddedAt(OffsetDateTime.now());
        pool.save(item);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public UUID assign(UUID id, CandidateCreate request) {
        Exam exam = locked(id);
        if (!Set.of("DRAFT", "SCHEDULED", "ACTIVE").contains(exam.getStatus()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Kỳ thi đã kết thúc hoặc đã hủy.");
        if (!request.scheduledStart().isBefore(request.scheduledEnd()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu.");
        Account student =
                users.findByEmailIgnoreCase(request.studentEmail().trim())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Không tìm thấy tài khoản sinh viên với email này."
                                                    + " Hãy tạo tài khoản sinh viên trước khi phân"
                                                    + " lịch."));
        if (!"STUDENT".equals(student.getRole()) || !Boolean.TRUE.equals(student.getIsActive()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Email phải thuộc tài khoản sinh viên đang hoạt động.");
        // Share the course lock with manual enrollment; avoid duplicate enrollment/schedules on
        // concurrent submissions.
        if (candidates.existsByExamIdAndStudentId(id, student.getId()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sinh viên đã có lịch trong kỳ thi này. Không thể thêm trùng.");
        if (!courses.isEnrolled(exam.getCourseId(), student.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sinh viên chưa được ghi danh hoặc đã ngừng học môn này. Nhờ Admin ghi danh tại"
                            + " Môn học & sinh viên trước khi phân lịch.");
        }
        ExamCandidate c = new ExamCandidate();
        c.setExamId(id);
        c.setCourseId(exam.getCourseId());
        c.setStudentId(student.getId());
        c.setScheduledStart(request.scheduledStart());
        c.setScheduledEnd(request.scheduledEnd());
        c.setStatus("REGISTERED");
        c.setCreatedAt(OffsetDateTime.now());
        return candidates.save(c).getId();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('LECTURER')")
    public ExamView activate(UUID id) {
        Exam exam = locked(id);
        if (!Set.of("DRAFT", "SCHEDULED").contains(exam.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exam cannot be activated");
        if (exam.getRubricId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Chọn rubric chấm toàn bài trước khi mở kỳ thi.");
        }
        var rubric =
                questions
                        .findRubricById(exam.getRubricId())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Không tìm thấy rubric chấm toàn bài."));
        if (!exam.getCourseId().equals(rubric.getCourseId())
                || !"PUBLISHED".equals(rubric.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rubric chấm toàn bài phải được công bố và thuộc môn học của kỳ thi.");
        }
        long count =
                pool.findByIdExamId(id).stream()
                        .filter(
                                p ->
                                        questions
                                                .findById(p.getId().getQuestionId())
                                                .map(
                                                        q ->
                                                                "APPROVED".equals(q.getStatus())
                                                                        && exam.getCourseId()
                                                                                .equals(
                                                                                        q
                                                                                                .getCourseId()))
                                                .orElse(false))
                        .count();
        if (count < exam.getMainQuestionCount())
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Not enough questions in pool");
        exam.setStatus("ACTIVE");
        exam.setUpdatedAt(OffsetDateTime.now());
        return ExamMapper.view(exams.save(exam));
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    public List<ExamView> list() {
        var courseIds = courses.assignedCourseIds(access.current().getId());
        if (courseIds.isEmpty()) return List.of();
        return exams.findByCourseIdIn(courseIds).stream()
                .sorted(Comparator.comparing(Exam::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(ExamMapper::view)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    public ExamView one(UUID id) {
        Exam exam = get(id);
        access.requireTeacher(exam.getCourseId());
        return ExamMapper.view(exam);
    }

    private Exam get(UUID id) {
        return exams.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));
    }

    private Exam locked(UUID id) {
        UUID courseId =
                exams.findCourseIdById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Exam not found"));
        access.requireTeacher(courseId);
        courses.findLockedById(courseId).orElseThrow();
        return exams.findLockedById(id).orElseThrow();
    }
}
