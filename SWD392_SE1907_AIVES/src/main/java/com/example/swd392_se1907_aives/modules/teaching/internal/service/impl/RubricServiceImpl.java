package com.example.swd392_se1907_aives.modules.teaching.internal.service.impl;

import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Rubric;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.RubricCriterion;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricDtos.RubricRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricDtos.RubricView;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricCriterionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.RubricService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RubricServiceImpl implements RubricService {
    private final RubricRepository rubrics;
    private final RubricCriterionRepository criteria;
    private final com.example.swd392_se1907_aives.modules.teaching.api.TeachingUsagePolicy usage;
    private final CourseRepository courses;
    private final CourseAccessService access;

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    public RubricView createRubric(RubricRequest request) {
        access.requireTeacher(request.courseId());
        requireCourse(request.courseId());
        Rubric r = new Rubric();
        r.setCourseId(request.courseId());
        r.setCreatedBy(access.current().getId());
        r.setName(request.name());
        r.setDescription(request.description());
        r.setMaxScore(request.maxScore());
        r.setStatus("DRAFT");
        r.setCreatedAt(OffsetDateTime.now());
        r.setUpdatedAt(OffsetDateTime.now());
        return view(rubrics.save(r));
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    public List<RubricView> rubrics(UUID courseId) {
        access.requireTeacher(courseId);
        return rubrics.findByCourseId(courseId).stream().map(this::view).toList();
    }

    private RubricView view(Rubric r) {
        return new RubricView(
                r.getId(),
                r.getCourseId(),
                r.getName(),
                r.getMaxScore(),
                r.getStatus(),
                r.getDescription());
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public RubricView updateRubric(UUID id, RubricRequest request) {
        var r = editableRubric(id);
        if (!r.getCourseId().equals(request.courseId()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Không đổi môn của rubric đã tạo.");
        BigDecimal total =
                criteria.findByRubricId(id).stream()
                        .map(RubricCriterion::getMaxPoints)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(request.maxScore()) > 0)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Điểm tối đa không được nhỏ hơn tổng điểm tiêu chí hiện có.");
        r.setName(request.name());
        r.setDescription(request.description());
        r.setMaxScore(request.maxScore());
        r.setUpdatedAt(OffsetDateTime.now());
        return view(rubrics.save(r));
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public void removeRubric(UUID id) {
        var r =
                rubrics.findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy rubric."));
        access.requireTeacher(r.getCourseId());
        if (usage.hasRubricInUse(id))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Rubric đang dùng để chấm kỳ thi. Giữ rubric để bảo toàn căn cứ chấm điểm.");
        r.setStatus("ARCHIVED");
        r.setUpdatedAt(OffsetDateTime.now());
        rubrics.save(r);
    }

    private Rubric editableRubric(UUID id) {
        Rubric r =
                rubrics.findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Rubric not found"));
        access.requireTeacher(r.getCourseId());
        if (!"DRAFT".equals(r.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rubric is not a draft");
        return r;
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public RubricView publish(UUID id) {
        Rubric r = editableRubric(id);
        BigDecimal total =
                criteria.findByRubricId(id).stream()
                        .map(RubricCriterion::getMaxPoints)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(r.getMaxScore()) != 0)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Criteria must sum to max score");
        r.setStatus("PUBLISHED");
        return view(rubrics.save(r));
    }

    private void requireCourse(UUID id) {
        if (!courses.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
    }
}
