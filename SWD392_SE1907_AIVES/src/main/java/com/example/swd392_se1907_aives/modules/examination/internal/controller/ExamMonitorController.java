package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamAttempt;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamCandidate;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.AiInterviewRequestRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.AttemptQuestionRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.InterviewTurnRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewAudioStorage;
import com.example.swd392_se1907_aives.modules.identity.api.Account;
import com.example.swd392_se1907_aives.modules.identity.api.IdentityDirectory;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseCatalog;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseInfo;


import lombok.RequiredArgsConstructor;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/exams/{examId}") @PreAuthorize("hasRole('LECTURER')")
public class ExamMonitorController {
    private final ExamRepository exams; private final CourseCatalog courses; private final CourseAccessService access;
    private final ExamCandidateRepository candidates; private final ExamAttemptRepository attempts;
    private final AttemptQuestionRepository questions; private final InterviewTurnRepository turns;
    private final IdentityDirectory users; private final InterviewAudioStorage audio;
    private final AiInterviewRequestRepository requests;
    public record AttemptView(UUID id,int attemptNo,String status,OffsetDateTime startedAt,OffsetDateTime endedAt,
        OffsetDateTime lastActivityAt,int completedQuestions,int questionCount,int timedOutQuestions,int followupsUsed,
        Integer currentSequence,String phase,OffsetDateTime deadline){}
    public record CandidateView(UUID id,String fullName,String email,OffsetDateTime scheduledStart,OffsetDateTime scheduledEnd,
        String status,List<AttemptView> attempts){}
    public record MonitorView(UUID examId,String title,String examStatus,String courseCode,String courseName,
        OffsetDateTime serverNow,List<CandidateView> candidates){}
    public record TurnView(UUID id,int sequenceNo,int turnNo,String speaker,String kind,int followupNumber,String text,
        String speechStatus,boolean hasAudio,OffsetDateTime createdAt,OffsetDateTime startedAt,OffsetDateTime endedAt,OffsetDateTime deadline){}
    public record TranscriptView(UUID attemptId,String studentName,String email,int attemptNo,String status,
        OffsetDateTime startedAt,OffsetDateTime endedAt,OffsetDateTime serverNow,List<TurnView> turns){}

    @GetMapping("/monitor") @Transactional(readOnly=true)
    public MonitorView monitor(@PathVariable("examId") UUID examId){
        Exam exam=exam(examId);CourseInfo course=courses.findById(exam.getCourseId()).orElseThrow();
        var roster=candidates.findByExamIdOrderByScheduledStartAsc(examId);
        var students=users.findAllById(roster.stream().map(ExamCandidate::getStudentId).toList()).stream().collect(Collectors.toMap(Account::getId,u->u));
        var all=attempts.findByExamIdOrderByStartedAtDesc(examId);
        var progress=all.isEmpty()?List.<AttemptQuestionRepository.Progress>of():questions.findProgressByAttemptIdIn(all.stream().map(ExamAttempt::getId).toList());
        var turnProgress=progress.isEmpty()?List.<InterviewTurnRepository.Progress>of():turns.findProgressByAttemptQuestionIdIn(progress.stream().map(AttemptQuestionRepository.Progress::getId).toList());
        var byAttempt=progress.stream().collect(Collectors.groupingBy(AttemptQuestionRepository.Progress::getAttemptId));
        var byQuestion=turnProgress.stream().collect(Collectors.groupingBy(InterviewTurnRepository.Progress::getAttemptQuestionId));
        var aiProgress=progress.isEmpty()?List.<AiInterviewRequestRepository.Progress>of():requests.findProgressByAttemptQuestionIdIn(progress.stream().map(AttemptQuestionRepository.Progress::getId).toList());
        var byRequest=aiProgress.stream().filter(r->"FOLLOWUP_GENERATION".equals(r.getRequestType())).collect(Collectors.groupingBy(AiInterviewRequestRepository.Progress::getAttemptQuestionId));
        var byCandidate=all.stream().collect(Collectors.groupingBy(ExamAttempt::getCandidateId));
        List<CandidateView> rows=roster.stream().map(c->{
            Account student=students.get(c.getStudentId());
            var views=byCandidate.getOrDefault(c.getId(),List.of()).stream().sorted(Comparator.comparing(ExamAttempt::getAttemptNo).reversed())
                .map(a->view(a,byAttempt.getOrDefault(a.getId(),List.of()),byQuestion,byRequest)).toList();
            return new CandidateView(c.getId(),student.getFullName(),student.getEmail(),c.getScheduledStart(),c.getScheduledEnd(),c.getStatus(),views);
        }).toList();
        return new MonitorView(examId,exam.getTitle(),exam.getStatus(),course.getCode(),course.getName(),OffsetDateTime.now(),rows);
    }
    private AttemptView view(ExamAttempt a,List<AttemptQuestionRepository.Progress> progress,Map<UUID,List<InterviewTurnRepository.Progress>> byQuestion,Map<UUID,List<AiInterviewRequestRepository.Progress>> byRequest){
        int completed=(int)progress.stream().filter(q->terminal(q.getStatus())).count();
        int timedOut=(int)progress.stream().filter(q->"TIMED_OUT".equals(q.getStatus())).count();
        int followups=(int)progress.stream().flatMap(q->byQuestion.getOrDefault(q.getId(),List.of()).stream()).filter(t->"FOLLOWUP_QUESTION".equals(t.getKind())).count();
        var current=progress.stream().filter(q->!terminal(q.getStatus())).min(Comparator.comparing(AttemptQuestionRepository.Progress::getSequenceNo)).orElse(null);
        var latest=current==null?null:byQuestion.getOrDefault(current.getId(),List.of()).stream().max(Comparator.comparing(InterviewTurnRepository.Progress::getTurnNo)).orElse(null);
        String phase=a.getStatus();OffsetDateTime deadline=null;
        if("IN_PROGRESS".equals(a.getStatus())){
            phase="PREPARING";
            if(latest!=null){
                if("STUDENT".equals(latest.getSpeaker())){
                    phase="FAILED".equals(latest.getSpeechStatus())?"TRANSCRIPTION_FAILED":"PROCESSING";
                    if("SUCCEEDED".equals(latest.getSpeechStatus())){
                        var request=byRequest.getOrDefault(current.getId(),List.of()).stream().max(Comparator.comparing(AiInterviewRequestRepository.Progress::getRequestedAt)).orElse(null);
                        if(request!=null && ("FAILED".equals(request.getStatus()) || ("RUNNING".equals(request.getStatus()) && request.getRequestedAt().plusSeconds(150).isBefore(OffsetDateTime.now()))))phase="AI_FAILED";
                    }
                }
                else {deadline=latest.getResponseDeadlineAt();phase=deadline==null?"QUESTION_READY":deadline.isAfter(OffsetDateTime.now())?"ANSWERING":"DEADLINE_REACHED";}
            }
        }
        return new AttemptView(a.getId(),a.getAttemptNo(),a.getStatus(),a.getStartedAt(),a.getEndedAt(),a.getLastActivityAt(),completed,progress.size(),timedOut,followups,current==null?null:current.getSequenceNo(),phase,deadline);
    }
    @GetMapping("/attempts/{attemptId}/transcript") @Transactional(readOnly=true)
    public TranscriptView transcript(@PathVariable("examId") UUID examId,@PathVariable("attemptId") UUID attemptId){
        ExamAttempt attempt=attempt(examId,attemptId);var candidate=candidates.findById(attempt.getCandidateId()).orElseThrow();
        Account student=users.findById(candidate.getStudentId()).orElseThrow();
        List<TurnView> history=new ArrayList<>();
        for(var q:questions.findByAttemptIdOrderBySequenceNo(attemptId))for(var t:turns.findByAttemptQuestionIdOrderByTurnNo(q.getId()))
            history.add(new TurnView(t.getId(),q.getSequenceNo(),t.getTurnNo(),t.getSpeaker(),t.getKind(),t.getFollowupNumber(),t.getTextContent(),t.getSpeechStatus(),audio.exists(t.getAudioObjectKey()),t.getCreatedAt(),t.getStartedAt(),t.getEndedAt(),t.getResponseDeadlineAt()));
        return new TranscriptView(attemptId,student.getFullName(),student.getEmail(),attempt.getAttemptNo(),attempt.getStatus(),attempt.getStartedAt(),attempt.getEndedAt(),OffsetDateTime.now(),history);
    }
    @GetMapping("/attempts/{attemptId}/audio/{turnId}") @Transactional(readOnly=true)
    public ResponseEntity<Resource> audio(@PathVariable("examId") UUID examId,@PathVariable("attemptId") UUID attemptId,@PathVariable("turnId") UUID turnId){
        attempt(examId,attemptId);
        var turn=turns.findById(turnId).orElseThrow(()->missing("Không tìm thấy bản ghi."));
        var q=questions.findById(turn.getAttemptQuestionId()).orElseThrow();
        if(!attemptId.equals(q.getAttemptId()))throw missing("Bản ghi không thuộc lượt thi này.");
        String key=turn.getAudioObjectKey();if(!audio.exists(key))throw missing("Bản ghi không còn trên máy chủ.");
        String mime=key.endsWith(".wav")?"audio/wav":key.endsWith(".ogg")?"audio/ogg":key.endsWith(".mp4")?"audio/mp4":"audio/webm";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime)).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(new FileSystemResource(audio.path(key)));
    }
    private Exam exam(UUID id){var e=exams.findById(id).orElseThrow(()->missing("Không tìm thấy kỳ thi."));access.requireTeacher(e.getCourseId());return e;}
    private ExamAttempt attempt(UUID examId,UUID id){exam(examId);return attempts.findById(id).filter(a->examId.equals(a.getExamId())).orElseThrow(()->missing("Lượt thi không thuộc kỳ thi đã chọn."));}
    private boolean terminal(String status){return Set.of("FINISHED","TIMED_OUT").contains(status);}
    private ResponseStatusException missing(String text){return new ResponseStatusException(HttpStatus.NOT_FOUND,text);}
}
