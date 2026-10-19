package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.api.TeachingUsagePolicy;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Question;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionReview;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Rubric;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.RubricCriterion;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionReviewRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricCriterionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1")
public class QuestionBankController {
    private final RubricRepository rubrics; private final RubricCriterionRepository criteria; private final QuestionRepository questions;
    private final QuestionReviewRepository reviews; private final CourseRepository courses;
    private final CourseAccessService access;
    private final TeachingUsagePolicy usage;
    public record RubricRequest(@NotNull UUID courseId,@NotBlank @Size(max=180) String name,String description,@NotNull @DecimalMin("0.01") @Digits(integer=4,fraction=2) BigDecimal maxScore){}
    public record RubricView(UUID id, UUID courseId, String name, BigDecimal maxScore,String status,String description){}
    public record QuestionRequest(@NotNull UUID courseId,@NotNull UUID rubricId,@NotBlank String content,String referenceAnswer,
             @NotBlank String bloomLevel,@NotBlank String difficulty){}
    public record QuestionView(UUID id,UUID courseId,UUID rubricId,String content,String bloomLevel,String difficulty,String status){}
    @PostMapping("/rubrics") @PreAuthorize("hasRole('LECTURER')") public RubricView createRubric(@Valid @RequestBody RubricRequest req){
        access.requireTeacher(req.courseId());requireCourse(req.courseId());
        Rubric r=new Rubric();r.setCourseId(req.courseId());r.setCreatedBy(access.current().getId());
        r.setName(req.name());r.setDescription(req.description());r.setMaxScore(req.maxScore());r.setStatus("DRAFT");r.setCreatedAt(OffsetDateTime.now());r.setUpdatedAt(OffsetDateTime.now());
        return view(rubrics.save(r));
    }
    @GetMapping("/courses/{courseId}/rubrics") @PreAuthorize("hasRole('LECTURER')")
    public List<RubricView> rubrics(@PathVariable UUID courseId){access.requireTeacher(courseId);
        return rubrics.findByCourseId(courseId).stream().map(this::view).toList();}
    private RubricView view(Rubric r){return new RubricView(r.getId(),r.getCourseId(),r.getName(),r.getMaxScore(),r.getStatus(),r.getDescription());}
    @PutMapping("/rubrics/{id}") @PreAuthorize("hasRole('LECTURER')") @Transactional
    public RubricView updateRubric(@PathVariable("id") UUID id,@Valid @RequestBody RubricRequest req){
        var r=editableRubric(id);
        if(!r.getCourseId().equals(req.courseId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không đổi môn của rubric đã tạo.");
        BigDecimal total=criteria.findByRubricId(id).stream().map(RubricCriterion::getMaxPoints).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(total.compareTo(req.maxScore())>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Điểm tối đa không được nhỏ hơn tổng điểm tiêu chí hiện có.");
        r.setName(req.name());r.setDescription(req.description());r.setMaxScore(req.maxScore());r.setUpdatedAt(OffsetDateTime.now());return view(rubrics.save(r));
    }
    @DeleteMapping("/rubrics/{id}") @PreAuthorize("hasRole('LECTURER')") @Transactional
    public void removeRubric(@PathVariable("id") UUID id){
        var r=rubrics.findLockedById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy rubric."));access.requireTeacher(r.getCourseId());
        if(questions.existsByRubricId(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Rubric đang gắn với câu hỏi. Giữ rubric để bảo toàn căn cứ chấm điểm.");
        r.setStatus("ARCHIVED");r.setUpdatedAt(OffsetDateTime.now());rubrics.save(r);
    }
    public record CriterionRequest(@NotBlank @Size(max=180) String criterionName,String description,@NotNull @DecimalMin("0.01") @Digits(integer=4,fraction=2) BigDecimal maxPoints,@Min(1) int sortOrder){}
    public record CriterionView(UUID id,UUID rubricId,String criterionName,String description,BigDecimal maxPoints,int sortOrder){}
    private CriterionView criterionView(RubricCriterion c){return new CriterionView(c.getId(),c.getRubricId(),c.getCriterionName(),c.getDescription(),c.getMaxPoints(),c.getSortOrder());}
    @GetMapping("/rubrics/{id}/criteria") @PreAuthorize("hasRole('LECTURER')")
    public List<CriterionView> criteria(@PathVariable UUID id){
        Rubric r=rubrics.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rubric not found"));
        access.requireTeacher(r.getCourseId());
        return criteria.findByRubricId(id).stream().sorted(Comparator.comparing(RubricCriterion::getSortOrder)).map(this::criterionView).toList();
    }
    private Rubric editableRubric(UUID id){
        Rubric r=rubrics.findLockedById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rubric not found"));
        access.requireTeacher(r.getCourseId());
        if(!"DRAFT".equals(r.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Rubric is not a draft");
        return r;
    }
    private void validateCriterion(Rubric rubric,CriterionRequest req,UUID excludedId){
        var others=criteria.findByRubricId(rubric.getId()).stream().filter(c->!c.getId().equals(excludedId)).toList();
        if(others.stream().anyMatch(c->c.getSortOrder()==req.sortOrder()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Thứ tự tiêu chí đã được sử dụng.");
        BigDecimal total=others.stream().map(RubricCriterion::getMaxPoints).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(total.add(req.maxPoints()).compareTo(rubric.getMaxScore())>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tổng điểm tiêu chí vượt điểm tối đa của rubric.");
    }
    @PostMapping("/rubrics/{id}/criteria") @PreAuthorize("hasRole('LECTURER')") @Transactional
    public CriterionView addCriterion(@PathVariable UUID id,@Valid @RequestBody CriterionRequest req){
        Rubric rubric=editableRubric(id);validateCriterion(rubric,req,null);
        RubricCriterion c=new RubricCriterion();c.setRubricId(id);c.setCriterionName(req.criterionName());c.setDescription(req.description());
        c.setMaxPoints(req.maxPoints());c.setSortOrder(req.sortOrder());c=criteria.save(c);
        return criterionView(c);
    }
    @PutMapping("/rubrics/{id}/criteria/{criterionId}") @PreAuthorize("hasRole('LECTURER')") @Transactional
    public CriterionView updateCriterion(@PathVariable UUID id,@PathVariable UUID criterionId,@Valid @RequestBody CriterionRequest req){
        Rubric r=editableRubric(id);
        RubricCriterion c=criteria.findById(criterionId).filter(x->id.equals(x.getRubricId())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Criterion not found"));
        validateCriterion(r,req,criterionId);c.setCriterionName(req.criterionName());c.setDescription(req.description());c.setMaxPoints(req.maxPoints());c.setSortOrder(req.sortOrder());
        return criterionView(criteria.save(c));
    }
    @DeleteMapping("/rubrics/{id}/criteria/{criterionId}") @PreAuthorize("hasRole('LECTURER')") @Transactional
    public void deleteCriterion(@PathVariable UUID id,@PathVariable UUID criterionId){
        editableRubric(id);RubricCriterion c=criteria.findById(criterionId).filter(x->id.equals(x.getRubricId())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Criterion not found"));criteria.delete(c);
    }
    @PatchMapping("/rubrics/{id}/publish") @PreAuthorize("hasRole('LECTURER')") @Transactional public RubricView publish(@PathVariable UUID id){
        Rubric r=editableRubric(id);
        BigDecimal total=criteria.findByRubricId(id).stream().map(RubricCriterion::getMaxPoints).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(total.compareTo(r.getMaxScore())!=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Criteria must sum to max score");
        r.setStatus("PUBLISHED");return view(rubrics.save(r));}
    @PostMapping("/questions") @PreAuthorize("hasRole('LECTURER')") @Transactional public QuestionView createQuestion(@Valid @RequestBody QuestionRequest req){
        access.requireTeacher(req.courseId());requireCourse(req.courseId());
        Rubric r=rubrics.findLockedById(req.rubricId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rubric not found"));
        if(!r.getCourseId().equals(req.courseId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rubric belongs to another course");
        if("ARCHIVED".equals(r.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Rubric đã ngừng sử dụng. Hãy chọn rubric khác.");
        if(!Set.of("REMEMBER","UNDERSTAND","APPLY","ANALYZE").contains(req.bloomLevel())||!Set.of("EASY","MEDIUM","HARD").contains(req.difficulty()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid bloom or difficulty");
        Question q=new Question();q.setCourseId(req.courseId());q.setRubricId(req.rubricId());q.setCreatedBy(access.current().getId());
        q.setContent(req.content());q.setReferenceAnswer(req.referenceAnswer());q.setBloomLevel(req.bloomLevel());q.setDifficulty(req.difficulty());
        q.setSourceType("MANUAL");q.setStatus("PENDING_REVIEW");q.setCreatedAt(OffsetDateTime.now());q.setUpdatedAt(OffsetDateTime.now());return view(questions.save(q));
    }
    @GetMapping("/courses/{courseId}/questions") @PreAuthorize("hasRole('LECTURER')")
    public List<QuestionView> list(@PathVariable UUID courseId){access.requireTeacher(courseId);return questions.findByCourseId(courseId).stream().map(this::view).toList();}
    @PatchMapping("/questions/{id}/review") @PreAuthorize("hasRole('LECTURER')") @Transactional
    public QuestionView review(@PathVariable UUID id,@RequestParam boolean approve,@RequestBody(required=false) String comment){
        Question q=lockedQuestion(id);
        if(!"PENDING_REVIEW".equals(q.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Question not pending review");
        q.setStatus(approve?"APPROVED":"REJECTED");q.setUpdatedAt(OffsetDateTime.now());
        if(approve){if(q.getRubricId()==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rubric required");
            Rubric rubric=rubrics.findById(q.getRubricId()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không tìm thấy rubric."));
            if(!q.getCourseId().equals(rubric.getCourseId()) || !"PUBLISHED".equals(rubric.getStatus()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cần gắn rubric đã công bố của môn học trước khi duyệt.");
            q.setApprovedBy(access.current().getId());q.setApprovedAt(OffsetDateTime.now());}
        QuestionReview rv=new QuestionReview();rv.setQuestionId(id);rv.setReviewerId(access.current().getId());
        rv.setDecision(approve?"APPROVE":"REJECT");rv.setComment(comment);rv.setReviewedAt(OffsetDateTime.now());reviews.save(rv);
        return view(questions.save(q));
    }
    @PutMapping("/questions/{id}") @PreAuthorize("hasRole('LECTURER')") @Transactional
    public QuestionView updateQuestion(@PathVariable UUID id,@Valid @RequestBody QuestionRequest req){
        Question q=lockedQuestion(id);
        if(!q.getCourseId().equals(req.courseId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không thể chuyển câu hỏi sang môn học khác.");
        if("ARCHIVED".equals(q.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Câu hỏi đã được loại bỏ.");
        Rubric rubric=rubrics.findLockedById(req.rubricId()).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không tìm thấy rubric."));
        if(!q.getCourseId().equals(rubric.getCourseId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rubric phải thuộc cùng môn học.");
        if("ARCHIVED".equals(rubric.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Rubric đã ngừng sử dụng. Hãy chọn rubric khác.");
        if(!Set.of("REMEMBER","UNDERSTAND","APPLY","ANALYZE").contains(req.bloomLevel())||!Set.of("EASY","MEDIUM","HARD").contains(req.difficulty()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mức Bloom hoặc độ khó không hợp lệ.");
        removeFromDraftExams(q);
        q.setContent(req.content());q.setReferenceAnswer(req.referenceAnswer());q.setRubricId(req.rubricId());q.setBloomLevel(req.bloomLevel());q.setDifficulty(req.difficulty());
        q.setStatus("PENDING_REVIEW");q.setApprovedBy(null);q.setApprovedAt(null);q.setUpdatedAt(OffsetDateTime.now());
        audit(q,"REQUEST_CHANGES","Giảng viên chỉnh sửa; câu hỏi cần được duyệt lại trước khi vào ngân hàng chính thức.");
        return view(questions.save(q));
    }
    @DeleteMapping("/questions/{id}") @PreAuthorize("hasRole('LECTURER')") @Transactional
    public QuestionView archiveQuestion(@PathVariable UUID id){
        Question q=lockedQuestion(id);if("ARCHIVED".equals(q.getStatus()))return view(q);
        removeFromDraftExams(q);q.setStatus("ARCHIVED");q.setUpdatedAt(OffsetDateTime.now());
        audit(q,"REJECT","Giảng viên loại bỏ câu hỏi khỏi danh sách sử dụng.");return view(questions.save(q));
    }
    private Question lockedQuestion(UUID id){
        UUID courseId=questions.findCourseIdById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy câu hỏi."));
        access.requireTeacher(courseId);courses.findLockedById(courseId).orElseThrow();return questions.findById(id).orElseThrow();
    }
    private void removeFromDraftExams(Question q){usage.prepareQuestionChange(q.getId());}
    private void audit(Question q,String decision,String comment){
        var review=new QuestionReview();review.setQuestionId(q.getId());review.setReviewerId(access.current().getId());review.setDecision(decision);review.setComment(comment);review.setReviewedAt(OffsetDateTime.now());reviews.save(review);
    }
    private QuestionView view(Question q){return new QuestionView(q.getId(),q.getCourseId(),q.getRubricId(),q.getContent(),q.getBloomLevel(),q.getDifficulty(),q.getStatus());}
    private void requireCourse(UUID id){if(!courses.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Course not found");}
}
