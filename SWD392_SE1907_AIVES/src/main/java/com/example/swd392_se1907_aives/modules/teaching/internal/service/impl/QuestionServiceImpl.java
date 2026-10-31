package com.example.swd392_se1907_aives.modules.teaching.internal.service.impl;

import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.api.TeachingUsagePolicy;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Question;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionReview;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.policy.QuestionClassification;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionDtos.QuestionRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionDtos.QuestionView;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionReviewRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.QuestionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {
    private final QuestionRepository questions;
    private final QuestionReviewRepository reviews;
    private final CourseRepository courses;
    private final CourseAccessService access;
    private final TeachingUsagePolicy usage;

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public QuestionView createQuestion(QuestionRequest request) {
        access.requireTeacher(request.courseId());
        requireCourseExists(request.courseId());
        validateDifficulty(request.difficulty());
        Question question = new Question();
        question.setCourseId(request.courseId());
        question.setCreatedBy(access.current().getId());
        question.setContent(request.content());
        question.setReferenceAnswer(request.referenceAnswer());
        question.setDifficulty(request.difficulty());
        question.setSourceType("MANUAL");
        question.setStatus("PENDING_REVIEW");
        question.setCreatedAt(OffsetDateTime.now());
        question.setUpdatedAt(OffsetDateTime.now());
        return view(questions.save(question));
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    public List<QuestionView> list(UUID courseId) {
        access.requireTeacher(courseId);
        return questions.findByCourseId(courseId).stream().map(this::view).toList();
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public QuestionView review(UUID id, boolean approve, String comment) {
        Question question = getQuestionForUpdate(id);
        if (!"PENDING_REVIEW".equals(question.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question not pending review");
        question.setStatus(approve ? "APPROVED" : "REJECTED");
        question.setUpdatedAt(OffsetDateTime.now());
        if (approve) {
            question.setApprovedBy(access.current().getId());
            question.setApprovedAt(OffsetDateTime.now());
        }
        QuestionReview questionReview = new QuestionReview();
        questionReview.setQuestionId(id);
        questionReview.setReviewerId(access.current().getId());
        questionReview.setDecision(approve ? "APPROVE" : "REJECT");
        questionReview.setComment(comment);
        questionReview.setReviewedAt(OffsetDateTime.now());
        reviews.save(questionReview);
        return view(questions.save(question));
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public QuestionView updateQuestion(UUID id, QuestionRequest request) {
        Question question = getQuestionForUpdate(id);
        if (!question.getCourseId().equals(request.courseId()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Không thể chuyển câu hỏi sang môn học khác.");
        if ("ARCHIVED".equals(question.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Câu hỏi đã được loại bỏ.");
        validateDifficulty(request.difficulty());
        prepareQuestionChange(question);
        question.setContent(request.content());
        question.setReferenceAnswer(request.referenceAnswer());
        question.setDifficulty(request.difficulty());
        question.setStatus("PENDING_REVIEW");
        question.setApprovedBy(null);
        question.setApprovedAt(null);
        question.setUpdatedAt(OffsetDateTime.now());
        audit(
                question,
                "REQUEST_CHANGES",
                "Giảng viên chỉnh sửa; câu hỏi cần được duyệt lại trước khi vào ngân hàng chính"
                        + " thức.");
        return view(questions.save(question));
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public QuestionView archiveQuestion(UUID id) {
        Question question = getQuestionForUpdate(id);
        if ("ARCHIVED".equals(question.getStatus())) return view(question);
        prepareQuestionChange(question);
        question.setStatus("ARCHIVED");
        question.setUpdatedAt(OffsetDateTime.now());
        audit(question, "REJECT", "Giảng viên loại bỏ câu hỏi khỏi danh sách sử dụng.");
        return view(questions.save(question));
    }

    private Question getQuestionForUpdate(UUID id) {
        UUID courseId =
                questions
                        .findCourseIdById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy câu hỏi."));
        access.requireTeacher(courseId);
        courses.findLockedById(courseId).orElseThrow();
        return questions.findById(id).orElseThrow();
    }

    private void prepareQuestionChange(Question question) {
        usage.prepareQuestionChange(question.getId());
    }

    private void audit(Question question, String decision, String comment) {
        var review = new QuestionReview();
        review.setQuestionId(question.getId());
        review.setReviewerId(access.current().getId());
        review.setDecision(decision);
        review.setComment(comment);
        review.setReviewedAt(OffsetDateTime.now());
        reviews.save(review);
    }

    private QuestionView view(Question question) {
        return new QuestionView(
                question.getId(),
                question.getCourseId(),
                question.getContent(),
                question.getDifficulty(),
                question.getStatus());
    }

    private void requireCourseExists(UUID id) {
        if (!courses.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
    }

    private void validateDifficulty(String difficulty) {
        if (!QuestionClassification.isValidDifficulty(difficulty)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Mức câu hỏi phải là Dễ, Trung bình hoặc Khó.");
        }
    }
}
