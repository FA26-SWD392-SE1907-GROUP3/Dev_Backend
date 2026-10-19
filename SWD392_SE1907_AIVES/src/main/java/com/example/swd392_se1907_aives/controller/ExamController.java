package com.example.swd392_se1907_aives.controller;
import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.repository.*;
import com.example.swd392_se1907_aives.service.CourseAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/exams")
public class ExamController {
    private final ExamRepository exams;private final QuestionRepository questions;
    private final ExamQuestionPoolRepository pool;private final ExamCandidateRepository candidates;
    private final CourseEnrollmentRepository enrollments;private final CourseAccessService access;
    private final UserRepository users;
    private final CourseRepository courses;
    private final CourseLecturerRepository lecturers;
    public record ExamCreate(@NotNull UUID courseId,@NotBlank @Size(max=200) String title,@Min(1) @Max(100) int mainQuestionCount,
        @Min(0) @Max(10) Integer maxFollowupsPerQuestion,@Min(15) @Max(600) Integer answerTimeLimitSeconds,
        @Min(0) @Max(100) Integer maxFollowupsPerStudent,@Pattern(regexp="RANDOM|ADAPTIVE") String selectionMode){}
    public record ExamView(UUID id,UUID courseId,String title,String status,String selectionMode,int mainQuestionCount,int maxFollowupsPerQuestion,int answerTimeLimitSeconds,int maxFollowupsPerStudent){}
    @PostMapping @PreAuthorize("hasRole('LECTURER')") public ExamView create(@Valid @RequestBody ExamCreate req){
        access.requireTeacher(req.courseId()); Exam e=new Exam();e.setCourseId(req.courseId());e.setCreatedBy(access.current().getId());e.setTitle(req.title());
        e.setMainQuestionCount(req.mainQuestionCount());e.setMaxFollowupsPerQuestion(req.maxFollowupsPerQuestion()==null?2:req.maxFollowupsPerQuestion());e.setMaxFollowupsPerStudent(req.maxFollowupsPerStudent()==null?Math.min(100,e.getMaxFollowupsPerQuestion()*req.mainQuestionCount()):req.maxFollowupsPerStudent());
        e.setStatus("DRAFT");e.setSelectionMode(req.selectionMode()==null?"RANDOM":req.selectionMode());e.setAnswerTimeLimitSeconds(req.answerTimeLimitSeconds()==null?120:req.answerTimeLimitSeconds());e.setSlotDurationMinutes(20);
        e.setCreatedAt(OffsetDateTime.now());e.setUpdatedAt(OffsetDateTime.now());
        e.setExamLanguage("vi");e.setExamTimezone("Asia/Ho_Chi_Minh");return view(exams.save(e));
    }
    public record QuestionId(@NotNull UUID questionId){}
    @PostMapping("/{id}/questions") @PreAuthorize("hasRole('LECTURER')") @Transactional public void addQuestion(@PathVariable UUID id,@Valid @RequestBody QuestionId req){
        Exam e=locked(id);
        if(!"DRAFT".equals(e.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Chỉ thêm câu hỏi khi kỳ thi còn nháp.");
        Question q=questions.findById(req.questionId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Question not found"));
        if(!e.getCourseId().equals(q.getCourseId())||!"APPROVED".equals(q.getStatus()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Question must be approved and belong to same course");
        var poolId=new ExamQuestionPoolId(id,req.questionId());
        if(pool.existsById(poolId))return;
        ExamQuestionPool item=new ExamQuestionPool();item.setId(poolId);
        item.setCourseId(e.getCourseId());item.setSelectionWeight(1);item.setAddedAt(OffsetDateTime.now());pool.save(item);
    }
    public record CandidateCreate(@NotBlank @Email String studentEmail,@NotNull OffsetDateTime scheduledStart,@NotNull OffsetDateTime scheduledEnd,boolean enrollIfNeeded){}
    @Transactional
    @PostMapping("/{id}/candidates") @PreAuthorize("hasRole('LECTURER')") public UUID assign(@PathVariable UUID id,@Valid @RequestBody CandidateCreate req){
        Exam e=locked(id);
        if(!Set.of("DRAFT","SCHEDULED","ACTIVE").contains(e.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Kỳ thi đã kết thúc hoặc đã hủy.");
        if(!req.scheduledStart().isBefore(req.scheduledEnd()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Thời gian kết thúc phải sau thời gian bắt đầu.");
        User student=users.findByEmailIgnoreCase(req.studentEmail().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tài khoản sinh viên với email này. Hãy tạo tài khoản sinh viên trước khi phân lịch."));
        if(!"STUDENT".equals(student.getRole()) || !Boolean.TRUE.equals(student.getIsActive()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Email phải thuộc tài khoản sinh viên đang hoạt động.");
        // Share the course lock with manual enrollment; avoid duplicate enrollment/schedules on concurrent submissions.
        if(candidates.existsByExamIdAndStudentId(id,student.getId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Sinh viên đã có lịch trong kỳ thi này. Không thể thêm trùng.");
        var enrollmentId=new CourseEnrollmentId(e.getCourseId(),student.getId());
        CourseEnrollment enrollment=enrollments.findById(enrollmentId).orElse(null);
        if(enrollment==null || !"ACTIVE".equals(enrollment.getEnrollmentStatus())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sinh viên chưa được ghi danh hoặc đã ngừng học môn này. Nhờ Admin ghi danh tại Môn học & sinh viên trước khi phân lịch.");
        }
        ExamCandidate c=new ExamCandidate();c.setExamId(id);c.setCourseId(e.getCourseId());c.setStudentId(student.getId());
        c.setScheduledStart(req.scheduledStart());c.setScheduledEnd(req.scheduledEnd());c.setStatus("REGISTERED");
        c.setCreatedAt(OffsetDateTime.now());
        return candidates.save(c).getId();
    }
    @Transactional @PatchMapping("/{id}/activate") @PreAuthorize("hasRole('LECTURER')")
    public ExamView activate(@PathVariable UUID id){
        Exam e=locked(id);
        if(!Set.of("DRAFT","SCHEDULED").contains(e.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Exam cannot be activated");
        long count=pool.findByIdExamId(id).stream().filter(p->questions.findById(p.getId().getQuestionId()).map(q->"APPROVED".equals(q.getStatus())&&e.getCourseId().equals(q.getCourseId())).orElse(false)).count();
        if(count<e.getMainQuestionCount())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Not enough questions in pool");
        e.setStatus("ACTIVE");e.setUpdatedAt(OffsetDateTime.now());return view(exams.save(e));
    }
    @GetMapping @PreAuthorize("hasRole('LECTURER')")
    public List<ExamView> list(){
        var courseIds=lecturers.findByIdLecturerId(access.current().getId()).stream().map(link->link.getId().getCourseId()).toList();
        if(courseIds.isEmpty())return List.of();
        return exams.findByCourseIdIn(courseIds).stream()
            .sorted(Comparator.comparing(Exam::getTitle, String.CASE_INSENSITIVE_ORDER))
            .map(ExamController::view).toList();
    }
    @GetMapping("/{id}") @PreAuthorize("hasRole('LECTURER')") public ExamView one(@PathVariable UUID id){Exam e=get(id);access.requireTeacher(e.getCourseId());return view(e);}
    private Exam get(UUID id){return exams.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Exam not found"));}
    private Exam locked(UUID id){
        UUID courseId=exams.findCourseIdById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Exam not found"));
        access.requireTeacher(courseId);courses.findLockedById(courseId).orElseThrow();return exams.findLockedById(id).orElseThrow();
    }
    public static ExamView view(Exam e){return new ExamView(e.getId(),e.getCourseId(),e.getTitle(),e.getStatus(),e.getSelectionMode(),e.getMainQuestionCount(),e.getMaxFollowupsPerQuestion(),e.getAnswerTimeLimitSeconds(),e.followupBudget());}
}
