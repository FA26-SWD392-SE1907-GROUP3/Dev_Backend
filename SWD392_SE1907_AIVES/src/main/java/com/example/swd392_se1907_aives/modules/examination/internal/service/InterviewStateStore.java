package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.AiInterviewRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.AttemptQuestion;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamAttempt;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamCandidate;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.InterviewTurn;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.AiInterviewRequestRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.AttemptQuestionRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.InterviewTurnRepository;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

/** All state transitions lock the attempt. Network calls are made by InterviewService outside these transactions. */
@Service @RequiredArgsConstructor
public class InterviewStateStore {
    private final ExamAttemptRepository attempts;
    private final ExamCandidateRepository candidates;
    private final ExamRepository exams;
    private final AttemptQuestionRepository questions;
    private final InterviewTurnRepository turns;
    private final AiInterviewRequestRepository requests;
    private final CourseAccessService access;
    private final ObjectMapper json;
    private final InterviewAudioStorage audio;
    private final InterviewAiClient ai;
    private record Owned(ExamAttempt attempt,ExamCandidate candidate,Exam exam,List<AttemptQuestion> questions) {}
    public record Prompt(UUID id,UUID questionId,int sequenceNo,String kind,int followupNumber,String text,OffsetDateTime deadline) {}
    public record TurnView(UUID id,UUID questionId,int sequenceNo,String speaker,String kind,int followupNumber,String text,String speechStatus,boolean hasAudio) {}
    public record State(UUID attemptId,String status,int questionCount,int completedQuestions,int maxFollowups,int followupsUsed,int maxFollowupsPerQuestion,int answerTimeLimitSeconds,
                        OffsetDateTime scheduledEnd,OffsetDateTime serverNow,Prompt prompt,boolean busy,boolean needsContinuation,
                        UUID answerTurnId,String error,boolean aiConfigured,List<TurnView> turns) {}
    public record AnswerPlan(UUID turnId,UUID questionId,String text) {}
    public record DecisionPlan(UUID requestId,UUID answerTurnId,String context) {}
    public record SpeechPlan(UUID requestId,UUID turnId,String text,String cachedKey) {}
    public record LivePermission(UUID studentId,UUID promptId,OffsetDateTime deadline) {}

    @Transactional public State state(UUID attemptId){Owned owned=load(attemptId,true);normalize(owned);return view(owned);}
    @Transactional public void advanceSystem(UUID attemptId){normalize(load(attemptId,false));}
    @Transactional public State begin(UUID attemptId,UUID promptId){
        Owned owned=load(attemptId,true);normalize(owned);AttemptQuestion question=current(owned);
        InterviewTurn prompt=activePrompt(question,promptId);
        if(prompt.getResponseDeadlineAt()==null){
            OffsetDateTime now=OffsetDateTime.now();prompt.setStartedAt(now);
            prompt.setResponseDeadlineAt(earlier(now.plusSeconds(owned.exam().getAnswerTimeLimitSeconds()),owned.candidate().getScheduledEnd()));
            turns.save(prompt);question.setStatus("ANSWERING");if(question.getStartedAt()==null)question.setStartedAt(now);questions.save(question);
        }
        return view(owned);
    }
    @Transactional public LivePermission livePermission(UUID attemptId,UUID promptId){
        Owned owned=load(attemptId,true);normalize(owned);var prompt=activePrompt(current(owned),promptId);
        if(prompt.getResponseDeadlineAt()==null || !OffsetDateTime.now().isBefore(prompt.getResponseDeadlineAt()))throw conflict("Hãy bắt đầu trả lời trước khi bật micro.");
        return new LivePermission(owned.candidate().getStudentId(),promptId,prompt.getResponseDeadlineAt());
    }
    @Transactional public AnswerPlan reserveAnswer(UUID attemptId,UUID promptId,String text,boolean spoken){
        Owned owned=load(attemptId,true);normalize(owned);var question=current(owned);var prompt=activePrompt(question,promptId);
        if(prompt.getResponseDeadlineAt()==null)throw conflict("Chưa bắt đầu thời gian trả lời.");
        OffsetDateTime now=OffsetDateTime.now();
        if(!now.isBefore(prompt.getResponseDeadlineAt()))throw conflict("Đã hết thời gian trả lời.");
        var history=history(question);var previous=last(history);
        InterviewTurn answer;
        if(previous!=null && "STUDENT".equals(previous.getSpeaker()) && "FAILED".equals(previous.getSpeechStatus()))answer=previous;
        else {answer=new InterviewTurn();answer.setAttemptQuestionId(question.getId());answer.setTurnNo(prompt.getTurnNo()+1);answer.setCreatedAt(now);}
        answer.setSpeaker("STUDENT");answer.setReplyToTurnId(promptId);answer.setFollowupNumber(prompt.getFollowupNumber());
        answer.setKind(prompt.getFollowupNumber()==0?"MAIN_ANSWER":"FOLLOWUP_ANSWER");answer.setTextContent(spoken?null:text);
        answer.setSpeechStatus(spoken?"PROCESSING":"SUCCEEDED");answer.setStartedAt(now);answer.setEndedAt(spoken?null:now);answer.setAudioObjectKey(null);
        turns.saveAndFlush(answer);prompt.setEndedAt(now);turns.save(prompt);question.setStatus(spoken?"ANSWERING":"ASKING");questions.save(question);
        owned.attempt().setLastActivityAt(now);attempts.save(owned.attempt());
        if(spoken)request(question.getId(),answer.getId(),"STT",ai.sttModel());
        return new AnswerPlan(answer.getId(),question.getId(),answer.getTextContent());
    }
    @Transactional public void audioSaved(UUID attemptId,UUID turnId,String key){load(attemptId,true);var answer=turns.findById(turnId).orElseThrow();answer.setAudioObjectKey(key);turns.save(answer);}
    @Transactional public void transcribed(UUID attemptId,UUID turnId,String text){
        Owned owned=load(attemptId,true);var answer=turns.findById(turnId).orElseThrow();
        if(!"PROCESSING".equals(answer.getSpeechStatus()))throw conflict("Lượt trả lời đã được xử lý.");
        answer.setTextContent(text);answer.setSpeechStatus("SUCCEEDED");answer.setAiModelName(ai.sttModel());answer.setEndedAt(OffsetDateTime.now());turns.save(answer);
        var question=questions.findById(answer.getAttemptQuestionId()).orElseThrow();question.setStatus("ASKING");questions.save(question);
        finishRequest(answer.getId(),"STT",null,null);
    }
    @Transactional public void transcriptionFailed(UUID attemptId,UUID turnId,String message){
        load(attemptId,true);var answer=turns.findById(turnId).orElseThrow();answer.setSpeechStatus("FAILED");answer.setEndedAt(OffsetDateTime.now());turns.save(answer);
        finishRequest(turnId,"STT",null,message);
    }
    @Transactional public DecisionPlan claimDecision(UUID attemptId,UUID answerId){
        Owned owned=load(attemptId,true);normalize(owned);var question=current(owned);var history=history(question);var answer=last(history);
        if(answer==null || !answerId.equals(answer.getId()) || !"STUDENT".equals(answer.getSpeaker()) || !"SUCCEEDED".equals(answer.getSpeechStatus()))throw conflict("Không có câu trả lời đã lưu cần tiếp tục.");
        if(!canFollowup(owned,question) || !OffsetDateTime.now().isBefore(owned.candidate().getScheduledEnd())){
            finish(question,"FINISHED");normalize(owned);return null;
        }
        var previous=requests.findFirstByInputTurnIdAndRequestTypeOrderByRequestedAtDesc(answerId,"FOLLOWUP_GENERATION");
        if(previous.isPresent() && running(previous.get()))throw conflict("AI đang xử lý câu trả lời này.");
        var request=request(question.getId(),answerId,"FOLLOWUP_GENERATION",ai.followupModel());
        try {
            String context=json.writeValueAsString(Map.of("mainQuestion",question.getQuestionTextSnapshot(),
                "referenceAnswer",Objects.toString(question.getReferenceAnswerSnapshot(),""),"rubric",Objects.toString(question.getRubricSnapshot(),""),
                "followupsUsed",followupsFor(question),"maxFollowups",owned.exam().getMaxFollowupsPerQuestion(),"scope","Giới hạn hỏi xoáy cho câu chính hiện tại, đồng thời không vượt tổng giới hạn của lượt thi",
                "conversation",history.stream().filter(t->t.getTextContent()!=null).map(t->Map.of("speaker",t.getSpeaker(),"text",t.getTextContent())).toList()));
            return new DecisionPlan(request.getId(),answerId,context);
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    @Transactional public void decisionCompleted(UUID attemptId,DecisionPlan plan,InterviewAiClient.Decision decision){
        Owned owned=load(attemptId,true);var request=requests.findById(plan.requestId()).orElseThrow();
        if(!"RUNNING".equals(request.getStatus()))return;
        if(requests.findFirstByInputTurnIdAndRequestTypeOrderByRequestedAtDesc(plan.answerTurnId(),"FOLLOWUP_GENERATION").filter(r->r.getId().equals(plan.requestId())).isEmpty())return;
        var question=questions.findById(request.getAttemptQuestionId()).orElseThrow();var history=history(question);var answer=last(history);
        if(terminal(question) || answer==null || !plan.answerTurnId().equals(answer.getId())){completeRequest(request,null,"Lượt thi đã chuyển trạng thái.");return;}
        if(decision.askFollowup() && canFollowup(owned,question) && OffsetDateTime.now().isBefore(owned.candidate().getScheduledEnd())){
            String normalized=decision.question().replaceAll("\\s+"," ").strip();
            if(history.stream().anyMatch(t->"AI".equals(t.getSpeaker()) && normalized.equalsIgnoreCase(Objects.toString(t.getTextContent(),"").replaceAll("\\s+"," ").strip())))throw conflict("AI đã lặp lại câu hỏi. Câu trả lời đã lưu, hãy thử tiếp tục.");
            var prompt=new InterviewTurn();prompt.setAttemptQuestionId(question.getId());prompt.setTurnNo(answer.getTurnNo()+1);prompt.setSpeaker("AI");prompt.setKind("FOLLOWUP_QUESTION");
            prompt.setFollowupNumber(answer.getFollowupNumber()+1);prompt.setReplyToTurnId(answer.getId());prompt.setTextContent(decision.question());prompt.setFollowupReason(decision.reason());
            prompt.setAiModelName(ai.followupModel());prompt.setSpeechStatus("NONE");prompt.setCreatedAt(OffsetDateTime.now());turns.saveAndFlush(prompt);
            question.setStatus("ASKING");questions.save(question);completeRequest(request,prompt.getId(),null);
        }else {finish(question,"FINISHED");completeRequest(request,null,null);}
        normalize(owned);
    }
    @Transactional public void decisionFailed(UUID attemptId,UUID requestId,String message){load(attemptId,true);completeRequest(requests.findById(requestId).orElseThrow(),null,message);}
    @Transactional public State timeout(UUID attemptId,UUID promptId){
        Owned owned=load(attemptId,true);normalize(owned);
        // The expected prompt prevents an old browser timer from timing out the next question.
        var question=owned.questions().stream().filter(q->!terminal(q)).findFirst();
        if(question.isPresent()){
            var history=history(question.get());var last=last(history);
            if(last!=null && promptId.equals(last.getId()) && last.getResponseDeadlineAt()!=null && !OffsetDateTime.now().isBefore(last.getResponseDeadlineAt()))finish(question.get(),"TIMED_OUT");
        }
        normalize(owned);return view(owned);
    }
    @Transactional public SpeechPlan claimSpeech(UUID attemptId,UUID turnId){
        Owned owned=load(attemptId,true);var turn=visibleTurn(owned,turnId);
        if(!"AI".equals(turn.getSpeaker()) || "SYSTEM_NOTE".equals(turn.getKind()))throw conflict("Lượt này không có giọng đọc câu hỏi.");
        if(audio.exists(turn.getAudioObjectKey()))return new SpeechPlan(null,turnId,turn.getTextContent(),turn.getAudioObjectKey());
        var previous=requests.findFirstByInputTurnIdAndRequestTypeOrderByRequestedAtDesc(turnId,"TTS");
        if(previous.isPresent() && running(previous.get()))throw conflict("Giọng đọc đang được tạo. Vui lòng thử lại sau vài giây.");
        var req=request(turn.getAttemptQuestionId(),turnId,"TTS",ai.ttsModel());turn.setSpeechStatus("PROCESSING");turns.save(turn);
        return new SpeechPlan(req.getId(),turnId,turn.getTextContent(),null);
    }
    @Transactional public void speechFinished(UUID attemptId,SpeechPlan plan,String key,String message){
        load(attemptId,true);var turn=turns.findById(plan.turnId()).orElseThrow();turn.setSpeechStatus(message==null?"SUCCEEDED":"FAILED");
        if(key!=null)turn.setAudioObjectKey(key);turns.save(turn);completeRequest(requests.findById(plan.requestId()).orElseThrow(),turn.getId(),message);
    }
    @Transactional(readOnly=true) public String audioKey(UUID attemptId,UUID turnId){return visibleTurn(loadRead(attemptId),turnId).getAudioObjectKey();}

    private Owned load(UUID id,boolean authorize){
        ExamAttempt attempt=attempts.findLockedById(id).orElseThrow(()->missing("Không tìm thấy lượt thi."));
        return related(attempt,authorize);
    }
    private Owned loadRead(UUID id){return related(attempts.findById(id).orElseThrow(()->missing("Không tìm thấy lượt thi.")),true);}
    private Owned related(ExamAttempt attempt,boolean authorize){
        ExamCandidate candidate=candidates.findById(attempt.getCandidateId()).orElseThrow();
        if(authorize && (!"STUDENT".equals(access.current().getRole()) || !candidate.getStudentId().equals(access.current().getId())))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bạn không có quyền truy cập lượt thi này.");
        if(authorize)access.requireEnrolled(candidate.getCourseId());
        return new Owned(attempt,candidate,exams.findById(attempt.getExamId()).orElseThrow(),questions.findByAttemptIdOrderBySequenceNo(attempt.getId()));
    }
    private void normalize(Owned owned){
        if(!"IN_PROGRESS".equals(owned.attempt().getStatus()))return;
        OffsetDateTime now=OffsetDateTime.now();
        for(var question:owned.questions()){
            if(terminal(question))continue;
            var history=history(question);var latest=last(history);
            if(latest!=null && "STUDENT".equals(latest.getSpeaker()) && "PROCESSING".equals(latest.getSpeechStatus())){
                if(latest.getStartedAt()!=null && latest.getStartedAt().plusSeconds(150).isAfter(now))return;
                latest.setSpeechStatus("FAILED");turns.save(latest);finishRequest(latest.getId(),"STT",null,"Xử lý bản ghi bị gián đoạn. Hãy thử lại.");
            }
            if(!now.isBefore(owned.candidate().getScheduledEnd())){finish(question,latest!=null && "STUDENT".equals(latest.getSpeaker()) && "SUCCEEDED".equals(latest.getSpeechStatus())?"FINISHED":"TIMED_OUT");continue;}
            if(latest!=null && "STUDENT".equals(latest.getSpeaker()) && "SUCCEEDED".equals(latest.getSpeechStatus()))return;
            InterviewTurn prompt=history.stream().filter(t->"AI".equals(t.getSpeaker()) && !"SYSTEM_NOTE".equals(t.getKind())).reduce((a,b)->b).orElse(null);
            if(prompt!=null && prompt.getResponseDeadlineAt()!=null && !now.isBefore(prompt.getResponseDeadlineAt())){finish(question,"TIMED_OUT");continue;}
            if(prompt==null){
                prompt=new InterviewTurn();prompt.setAttemptQuestionId(question.getId());prompt.setTurnNo(1);prompt.setSpeaker("AI");prompt.setKind("MAIN_QUESTION");prompt.setFollowupNumber(0);
                prompt.setTextContent(question.getQuestionTextSnapshot());prompt.setSpeechStatus("NONE");prompt.setCreatedAt(now);turns.saveAndFlush(prompt);
            }
            if("PENDING".equals(question.getStatus())){question.setStatus("ASKING");questions.save(question);}return;
        }
        owned.attempt().setStatus("COMPLETED");owned.attempt().setEndedAt(now);owned.attempt().setLastActivityAt(now);attempts.save(owned.attempt());
        owned.candidate().setStatus("COMPLETED");candidates.save(owned.candidate());
    }
    private State view(Owned owned){
        AttemptQuestion current=owned.questions().stream().filter(q->!terminal(q)).findFirst().orElse(null);
        Prompt prompt=null;boolean busy=false,continuation=false;UUID answerId=null;String error=null;
        if(current!=null && "IN_PROGRESS".equals(owned.attempt().getStatus())){
            var history=history(current);var latest=last(history);
            if(latest!=null && "STUDENT".equals(latest.getSpeaker())){
                answerId=latest.getId();busy="PROCESSING".equals(latest.getSpeechStatus());
                if("SUCCEEDED".equals(latest.getSpeechStatus())){
                    var req=requests.findFirstByInputTurnIdAndRequestTypeOrderByRequestedAtDesc(latest.getId(),"FOLLOWUP_GENERATION");
                    busy=req.isPresent() && running(req.get());continuation=!busy;
                    if(req.isPresent() && !busy)error="RUNNING".equals(req.get().getStatus())?"Xử lý AI bị gián đoạn. Câu trả lời đã lưu, bạn có thể tiếp tục.":req.get().getErrorMessage();
                }else if("FAILED".equals(latest.getSpeechStatus()))error="Không nhận dạng được bản ghi. Bạn có thể ghi lại hoặc nhập câu trả lời trước khi hết giờ.";
            }
            if(latest!=null && ("AI".equals(latest.getSpeaker()) || "FAILED".equals(latest.getSpeechStatus()))){
                var selected=history.stream().filter(t->"AI".equals(t.getSpeaker()) && !"SYSTEM_NOTE".equals(t.getKind())).reduce((a,b)->b).orElse(null);
                if(selected!=null)prompt=new Prompt(selected.getId(),current.getId(),current.getSequenceNo(),selected.getKind(),selected.getFollowupNumber(),selected.getTextContent(),selected.getResponseDeadlineAt());
            }
        }
        List<TurnView> visible=new ArrayList<>();
        for(var question:owned.questions())if(!"PENDING".equals(question.getStatus()))for(var turn:history(question))visible.add(new TurnView(turn.getId(),question.getId(),question.getSequenceNo(),turn.getSpeaker(),turn.getKind(),turn.getFollowupNumber(),turn.getTextContent(),turn.getSpeechStatus(),turn.getAudioObjectKey()!=null));
        return new State(owned.attempt().getId(),owned.attempt().getStatus(),owned.questions().size(),(int)owned.questions().stream().filter(this::terminal).count(),owned.exam().followupBudget(),followupsUsed(owned),owned.exam().getMaxFollowupsPerQuestion(),owned.exam().getAnswerTimeLimitSeconds(),
            owned.candidate().getScheduledEnd(),OffsetDateTime.now(),prompt,busy,continuation,answerId,error,ai.configured(),visible);
    }
    private int followupsUsed(Owned owned){return (int)turns.countByAttemptQuestionIdInAndKind(owned.questions().stream().map(AttemptQuestion::getId).toList(),"FOLLOWUP_QUESTION");}
    private long followupsFor(AttemptQuestion question){return history(question).stream().filter(t->"FOLLOWUP_QUESTION".equals(t.getKind())).count();}
    private boolean canFollowup(Owned owned,AttemptQuestion question){return followupsUsed(owned)<owned.exam().followupBudget() && followupsFor(question)<owned.exam().getMaxFollowupsPerQuestion();}
    private InterviewTurn activePrompt(AttemptQuestion question,UUID promptId){
        var history=history(question);var latest=last(history);
        if(latest==null || ("STUDENT".equals(latest.getSpeaker()) && !"FAILED".equals(latest.getSpeechStatus())))throw conflict("Câu trả lời này đang được xử lý hoặc đã được nộp.");
        var prompt=history.stream().filter(t->"AI".equals(t.getSpeaker()) && !"SYSTEM_NOTE".equals(t.getKind())).reduce((a,b)->b).orElseThrow(()->conflict("Chưa có câu hỏi hiện tại."));
        if(!promptId.equals(prompt.getId()))throw conflict("Câu hỏi đã thay đổi. Hãy tải lại lượt thi.");return prompt;
    }
    private InterviewTurn visibleTurn(Owned owned,UUID id){
        var turn=turns.findById(id).orElseThrow(()->missing("Không tìm thấy lượt hội thoại."));
        if(owned.questions().stream().noneMatch(q->q.getId().equals(turn.getAttemptQuestionId()) && !"PENDING".equals(q.getStatus())))throw missing("Không tìm thấy lượt hội thoại.");return turn;
    }
    private AttemptQuestion current(Owned owned){if(!"IN_PROGRESS".equals(owned.attempt().getStatus()))throw conflict("Lượt thi đã kết thúc.");return owned.questions().stream().filter(q->!terminal(q)).findFirst().orElseThrow(()->conflict("Không còn câu hỏi."));}
    private List<InterviewTurn> history(AttemptQuestion question){return turns.findByAttemptQuestionIdOrderByTurnNo(question.getId());}
    private InterviewTurn last(List<InterviewTurn> values){return values.isEmpty()?null:values.getLast();}
    private boolean terminal(AttemptQuestion q){return Set.of("FINISHED","TIMED_OUT").contains(q.getStatus());}
    private void finish(AttemptQuestion q,String status){
        if("TIMED_OUT".equals(status) && !terminal(q)){
            var previous=last(history(q));var note=new InterviewTurn();note.setAttemptQuestionId(q.getId());note.setTurnNo(previous==null?1:previous.getTurnNo()+1);
            note.setSpeaker("AI");note.setKind("SYSTEM_NOTE");note.setFollowupNumber(0);note.setTextContent("Hết thời gian trả lời hoặc ca thi đã kết thúc.");note.setSpeechStatus("NONE");note.setCreatedAt(OffsetDateTime.now());turns.save(note);
        }
        q.setStatus(status);q.setCompletedAt(OffsetDateTime.now());questions.save(q);
    }
    private boolean running(AiInterviewRequest req){return "RUNNING".equals(req.getStatus()) && req.getRequestedAt().plusSeconds(150).isAfter(OffsetDateTime.now());}
    private AiInterviewRequest request(UUID question,UUID input,String type,String model){
        var req=new AiInterviewRequest();
        req.setAttemptQuestionId(question);req.setInputTurnId(input);req.setRequestType(type);req.setProvider("OPENAI");req.setModelName(model);req.setStatus("RUNNING");
        req.setParameters(json.createObjectNode());req.setErrorMessage(null);req.setRequestedAt(OffsetDateTime.now());req.setFinishedAt(null);return requests.saveAndFlush(req);
    }
    private void finishRequest(UUID input,String type,UUID output,String error){requests.findFirstByInputTurnIdAndRequestTypeOrderByRequestedAtDesc(input,type).ifPresent(req->completeRequest(req,output,error));}
    private void completeRequest(AiInterviewRequest req,UUID output,String error){req.setStatus(error==null?"SUCCEEDED":"FAILED");req.setOutputTurnId(output);req.setErrorMessage(error);req.setFinishedAt(OffsetDateTime.now());req.setLatencyMs((int)Math.min(Integer.MAX_VALUE,Math.max(0,Duration.between(req.getRequestedAt(),req.getFinishedAt()).toMillis())));requests.save(req);}
    private OffsetDateTime earlier(OffsetDateTime a,OffsetDateTime b){return a.isBefore(b)?a:b;}
    private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private ResponseStatusException missing(String message){return new ResponseStatusException(HttpStatus.NOT_FOUND,message);}
}
