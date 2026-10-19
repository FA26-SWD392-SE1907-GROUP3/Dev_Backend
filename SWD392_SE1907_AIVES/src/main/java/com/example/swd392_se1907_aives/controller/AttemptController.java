package com.example.swd392_se1907_aives.controller;
import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.repository.*;
import com.example.swd392_se1907_aives.service.CourseAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/attempts") @PreAuthorize("hasRole('STUDENT')")
public class AttemptController {
    private final ExamCandidateRepository candidates;private final ExamRepository exams;
    private final ExamQuestionPoolRepository pool; private final QuestionRepository questions;
    private final ExamAttemptRepository attempts;private final AttemptQuestionRepository attemptQuestions;
    private final InterviewTurnRepository turns;private final CourseAccessService access;
    private final RubricCriterionRepository rubricCriteria;
    private final RubricRepository rubrics;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final com.example.swd392_se1907_aives.service.QuestionSetSelector selector;
    private final com.example.swd392_se1907_aives.service.InterviewService interview;
    public record AttemptView(UUID attemptId,UUID candidateId,String status,int questionCount){}
    public record AssignedQuestion(UUID id,int sequenceNo,String content,String status){}
    public record AnswerRequest(@NotNull UUID promptId,@NotBlank @Size(max=12000) String text){}
    @PostMapping("/start/{candidateId}") @Transactional
    public AttemptView start(@PathVariable UUID candidateId){
        UUID examId=candidates.findExamIdById(candidateId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Candidate not found"));
        Exam e=exams.findLockedById(examId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Exam not found"));
        ExamCandidate c=candidates.findLockedById(candidateId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Candidate not found"));
        if(!c.getStudentId().equals(access.current().getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Lịch thi này được phân cho sinh viên khác. Hãy mở kỳ thi trong danh sách lịch thi của bạn.");
        access.requireEnrolled(c.getCourseId());
        var now=OffsetDateTime.now();
        if(!"ACTIVE".equals(e.getStatus()) || now.isBefore(c.getScheduledStart()) || now.isAfter(c.getScheduledEnd()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Kỳ thi chưa được kích hoạt hoặc hiện không nằm trong khung giờ thi của bạn. Hãy kiểm tra lịch thi và liên hệ giảng viên.");
        if(!Set.of("REGISTERED","READY").contains(c.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Candidate already started or unavailable");
        if(attempts.existsByCandidateId(candidateId)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Attempt already exists");
        List<ExamQuestionPool> eligible=pool.findAll().stream().filter(p->p.getId().getExamId().equals(e.getId()))
            .filter(p->questions.findById(p.getId().getQuestionId()).map(q->"APPROVED".equals(q.getStatus())).orElse(false)).toList();
        if(eligible.size()<e.getMainQuestionCount())throw new ResponseStatusException(HttpStatus.CONFLICT,"Exam question pool is insufficient");
        var recent=attempts.findTop20ByExamIdOrderByStartedAtDesc(e.getId());Set<UUID> previous=new HashSet<>();Map<UUID,Integer> usage=new HashMap<>();
        for(int i=0;i<recent.size();i++)for(var item:attemptQuestions.findByAttemptIdOrderBySequenceNo(recent.get(i).getId())){usage.merge(item.getQuestionId(),1,Integer::sum);if(i==0)previous.add(item.getQuestionId());}
        var selected=selector.select(eligible.stream().map(item->questions.findById(item.getId().getQuestionId()).orElseThrow()).toList(),e.getMainQuestionCount(),e.getSelectionMode(),previous,usage);
        ExamAttempt attempt=new ExamAttempt();attempt.setCandidateId(candidateId);attempt.setExamId(e.getId());attempt.setAttemptNo(1);
        attempt.setStatus("IN_PROGRESS");attempt.setStartedAt(now);attempt.setLastActivityAt(now);attempt.setCreatedAt(now);attempt=attempts.saveAndFlush(attempt);
        for(int i=0;i<e.getMainQuestionCount();i++){
            var q=selected.get(i);
            AttemptQuestion aq=new AttemptQuestion();aq.setAttemptId(attempt.getId());aq.setExamId(e.getId());
            aq.setQuestionId(q.getId());aq.setSequenceNo(i+1);aq.setQuestionTextSnapshot(q.getContent());
            aq.setReferenceAnswerSnapshot(q.getReferenceAnswer());
            if(q.getRubricId()!=null){
                var rubric=rubrics.findById(q.getRubricId()).orElseThrow();
                aq.setRubricSnapshot(json.valueToTree(Map.of("id",rubric.getId(),"name",rubric.getName(),"maxScore",rubric.getMaxScore(),"bloomLevel",q.getBloomLevel(),"difficulty",q.getDifficulty(),
                    "criteria",rubricCriteria.findByRubricId(q.getRubricId()).stream().sorted(Comparator.comparing(RubricCriterion::getSortOrder))
                        .map(r->Map.of("name",r.getCriterionName(),"description",Objects.toString(r.getDescription(),""),"maxPoints",r.getMaxPoints())).toList())).toString());
            }
            aq.setSelectionMode(e.getSelectionMode());aq.setStatus("PENDING");aq.setCreatedAt(now);attemptQuestions.saveAndFlush(aq);
        }
        c.setStatus("IN_PROGRESS");candidates.save(c);
        return new AttemptView(attempt.getId(),candidateId,attempt.getStatus(),e.getMainQuestionCount());
    }
    @GetMapping("/{id}/questions")
    public List<AssignedQuestion> assigned(@PathVariable UUID id){interview.state(id);var a=own(id);return attemptQuestions.findByAttemptIdOrderBySequenceNo(a.getId())
        .stream().filter(q->!"PENDING".equals(q.getStatus())).map(q->new AssignedQuestion(q.getId(),q.getSequenceNo(),q.getQuestionTextSnapshot(),q.getStatus())).toList();}
    @PostMapping("/questions/{assignedQuestionId}/answer")
    public com.example.swd392_se1907_aives.service.InterviewStateStore.State answer(@PathVariable UUID assignedQuestionId,@Valid @RequestBody AnswerRequest req){
        var aq=attemptQuestions.findById(assignedQuestionId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Question not assigned"));
        var state=interview.state(aq.getAttemptId());
        if(state.prompt()==null || !assignedQuestionId.equals(state.prompt().questionId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Question is not current");
        return interview.textAnswer(aq.getAttemptId(),req.promptId(),req.text());
    }
    private ExamAttempt own(UUID id){
        var a=attempts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Attempt not found"));
        var c=candidates.findById(a.getCandidateId()).orElseThrow();
        if(!c.getStudentId().equals(access.current().getId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Not your attempt");
        return a;
    }
}
