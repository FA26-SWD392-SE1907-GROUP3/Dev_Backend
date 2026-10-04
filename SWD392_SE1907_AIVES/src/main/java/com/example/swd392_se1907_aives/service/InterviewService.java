package com.example.swd392_se1907_aives.service;
import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.domain.enums.*;
import com.example.swd392_se1907_aives.dto.ApiModels.Answer;
import com.example.swd392_se1907_aives.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.*;
import java.util.*;
import static com.example.swd392_se1907_aives.service.ManagementService.*;
@Service
@RequiredArgsConstructor
@Transactional
public class InterviewService {
 private final ExamScheduleRepository schedules; private final InterviewLogRepository logs; private final QuestionRepository questions;
 private final RubricRepository rubrics; private final AccessService access; private final AiService ai;
 private ExamSchedule locked(Integer id) { var s=found(schedules.lockById(id)); access.schedule(s); return s; }
 private List<InterviewLog> history(ExamSchedule s) { return logs.findByExamScheduleScheduleIdOrderBySequenceNumberAsc(s.getScheduleId()); }
 private LocalDateTime end(ExamSchedule s) { var end=s.getAllocatedEndTime(); return end.isBefore(s.getExamSession().getEndTime())?end:s.getExamSession().getEndTime(); }
 private void close(ExamSchedule s) {
  var now=LocalDateTime.now();
  for(var log:history(s)) if(log.getAnsweredAt()==null) {
   log.setAnsweredAt(now);
   log.setTimeTakenSeconds((int)Math.max(0,Math.min(s.getExamSession().getAnswerTimeLimitSeconds(),Duration.between(log.getAskedAt(),now).getSeconds())));
  }
  s.setStatus(ScheduleStatus.COMPLETED); s.setActualEndTime(now);
 }
 public Map<String,Object> state(Integer id) {
  var s=found(schedules.findById(id)); access.schedule(s); var history=history(s);
  return Views.map("schedule",Views.schedule(s),"logs",history.stream().map(Views::log).toList(),"answerTimeLimitSeconds",s.getExamSession().getAnswerTimeLimitSeconds(),"serverTime",LocalDateTime.now());
 }
 public Map<String,Object> start(Integer id) {
  var s=locked(id);
  require(access.current().getUserId().equals(s.getStudent().getUserId()),"Only the assigned student can start");
  if(s.getStatus()==ScheduleStatus.IN_PROGRESS) return state(id);
  require(s.getStatus()==ScheduleStatus.NOT_STARTED,"Exam attempt cannot be restarted");
  require(s.getExamSession().getStatus()==ExamStatus.PUBLISHED,"Exam is not published");
  var now=LocalDateTime.now();
  require(!now.isBefore(s.getAllocatedStartTime()) && now.isBefore(end(s)),"Outside your allocated exam time");
  s.setStatus(ScheduleStatus.IN_PROGRESS); s.setActualStartTime(now);
  askMain(s,List.of()); return state(id);
 }
 private InterviewLog append(ExamSchedule s,String text,Question q,InterviewLog parent,int sequence) {
  var l=InterviewLog.builder().examSchedule(s).question(q).questionContent(text).questionType(parent==null?QuestionType.MAIN:QuestionType.FOLLOW_UP)
    .parentLog(parent).sequenceNumber(sequence).askedAt(LocalDateTime.now()).build();
  return logs.saveAndFlush(l);
 }
 private void askMain(ExamSchedule s,List<InterviewLog> history) {
  var exam=s.getExamSession();
  long mainCount=history.stream().filter(l -> l.getQuestionType()==QuestionType.MAIN).count();
  if(mainCount>=exam.getMaxMainQuestions()) { close(s); return; }
  Set<Integer> used=new HashSet<>();
  history.stream().filter(l -> l.getQuestion()!=null).forEach(l -> used.add(l.getQuestion().getQuestionId()));
  var candidates=new ArrayList<>(questions.findBySubjectSubjectIdAndStatus(exam.getSubject().getSubjectId(),QuestionStatus.APPROVED).stream()
   .filter(q -> q.getCreatedBy().getUserId().equals(exam.getCreatedBy().getUserId()) && !used.contains(q.getQuestionId()) && !rubrics.findByQuestionQuestionId(q.getQuestionId()).isEmpty()).toList());
  require(!candidates.isEmpty(),"No approved unused question available");
  var previous=schedules.findByExamSessionExamId(exam.getExamId()).stream()
   .filter(other -> !other.getScheduleId().equals(s.getScheduleId()) && other.getActualStartTime()!=null && other.getActualStartTime().isBefore(s.getActualStartTime()))
   .max(Comparator.comparing(ExamSchedule::getActualStartTime));
  Set<Integer> previousIds=new HashSet<>();
  previous.ifPresent(p -> logs.findByExamScheduleScheduleIdOrderBySequenceNumberAsc(p.getScheduleId()).stream().filter(l -> l.getQuestion()!=null).forEach(l -> previousIds.add(l.getQuestion().getQuestionId())));
  Collections.shuffle(candidates);
  candidates.sort(Comparator.comparing(q -> previousIds.contains(q.getQuestionId())));
  var q=candidates.get(0); append(s,q.getQuestionContent(),q,null,history.size()+1);
 }
 public Map<String,Object> answer(Integer id,Answer input) {
  var s=locked(id);
  require(access.current().getUserId().equals(s.getStudent().getUserId()),"Only the assigned student can answer");
  require(s.getStatus()==ScheduleStatus.IN_PROGRESS,"Attempt is not in progress");
  var history=history(s); require(!history.isEmpty(),"No active question"); var active=history.get(history.size()-1);
  require(active.getLogId().equals(input.logId()),"Answer does not match the current question");
  if(active.getAnsweredAt()!=null) { require(Objects.equals(active.getStudentAnswerTranscript(),input.transcript()),"Answer already submitted"); return state(id); }
  var now=LocalDateTime.now(); var due=active.getAskedAt().plusSeconds(s.getExamSession().getAnswerTimeLimitSeconds());
  require(now.isBefore(due) && now.isBefore(end(s)),"Answer deadline has passed; continue to the next question");
  active.setStudentAnswerTranscript(input.transcript()); active.setAnsweredAt(now);
  active.setTimeTakenSeconds((int)Duration.between(active.getAskedAt(),now).getSeconds());
  return state(id);
 }
 public Map<String,Object> next(Integer id,Integer afterLogId) {
  var s=locked(id); require(access.current().getUserId().equals(s.getStudent().getUserId()),"Only the assigned student can continue");
  if(s.getStatus()==ScheduleStatus.COMPLETED) return state(id);
  require(s.getStatus()==ScheduleStatus.IN_PROGRESS,"Attempt is not in progress");
  var history=history(s); require(!history.isEmpty(),"No active question"); var active=history.get(history.size()-1);
  // Retries after a successful transition return the current question, never ask another one.
  if(!active.getLogId().equals(afterLogId)) { require(history.stream().anyMatch(l -> l.getLogId().equals(afterLogId)),"Unknown question"); return state(id); }
  var now=LocalDateTime.now();
  if(!now.isBefore(end(s))) { close(s); return state(id); }
  if(active.getAnsweredAt()==null) {
   require(!now.isBefore(active.getAskedAt().plusSeconds(s.getExamSession().getAnswerTimeLimitSeconds())),"Submit the answer or wait for its deadline");
   active.setAnsweredAt(now); active.setTimeTakenSeconds(s.getExamSession().getAnswerTimeLimitSeconds());
  }
  var root=active.getParentLog()==null?active:active.getParentLog();
  long followUps=history.stream().filter(l -> l.getParentLog()!=null && l.getParentLog().getLogId().equals(root.getLogId())).count();
  if(followUps<s.getExamSession().getMaxFollowUpQuestions() && active.getStudentAnswerTranscript()!=null && !active.getStudentAnswerTranscript().isBlank()) {
   var criteria=rubrics.findByQuestionQuestionId(root.getQuestion().getQuestionId());
   String guide=criteria.stream().map(r -> r.getCriteriaName()+": "+r.getGuideline()).reduce("",(a,b)->a+"\n"+b);
   String prior="MAIN ANSWER: "+root.getStudentAnswerTranscript()+history.stream().filter(l -> l.getParentLog()!=null && l.getParentLog().getLogId().equals(root.getLogId())).map(l -> l.getQuestionContent()+"\n"+l.getStudentAnswerTranscript()).reduce("",(a,b)->a+"\n"+b);
   String follow=ai.followUp(root.getQuestionContent(),active.getStudentAnswerTranscript(),prior,guide);
   if(!LocalDateTime.now().isBefore(end(s))) { close(s); return state(id); }
   if(follow!=null) { append(s,follow,null,root,history.size()+1); return state(id); }
  }
  askMain(s,history); return state(id);
 }
 public Map<String,Object> finish(Integer id) {
  var s=locked(id);
  require(access.current().getUserId().equals(s.getStudent().getUserId()),"Only the assigned student can finish");
  if(s.getStatus()==ScheduleStatus.COMPLETED) return state(id);
  require(s.getStatus()==ScheduleStatus.IN_PROGRESS && !LocalDateTime.now().isBefore(end(s)),"Finish is available when your exam time expires");
  close(s); return state(id);
 }
}
