package com.example.swd392_se1907_aives.service;
import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.domain.enums.*;
import com.example.swd392_se1907_aives.dto.ApiModels.*;
import com.example.swd392_se1907_aives.repository.*;
import com.example.swd392_se1907_aives.security.TokenStore;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.*;
@Service
@RequiredArgsConstructor
@Transactional
public class ManagementService {
 private final UserRepository users; private final RoleRepository roles; private final SubjectRepository subjects;
 private final QuestionRepository questions; private final SubjectDocumentRepository documents; private final RubricRepository rubrics;
 private final ExamSessionRepository exams; private final ExamScheduleRepository schedules; private final InterviewLogRepository logs;
 private final PasswordEncoder encoder; private final TokenStore tokens; private final AccessService access;
 private final jakarta.validation.Validator validator;
 public static void require(boolean condition, String message) { if(!condition) throw new ResponseStatusException(HttpStatus.CONFLICT,message); }
 public static <T> T found(Optional<T> value) { return value.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found")); }
 public List<Map<String,Object>> users() { return users.findAll().stream().map(Views::user).toList(); }
 public Map<String,Object> user(Integer id) { return Views.user(found(users.findById(id))); }
 public List<Map<String,Object>> roles() { return roles.findAll().stream().map(r -> Views.map("roleId",r.getRoleId(),"roleName",r.getRoleName(),"description",r.getDescription())).toList(); }
 public Map<String,Object> question(Integer id) { return Views.question(ownedQuestion(id)); }
 public Map<String,Object> exam(Integer id) { return Views.exam(ownedExam(id)); }
 public Map<String,Object> schedule(Integer id) { var s=found(schedules.findById(id)); access.schedule(s); return Views.schedule(s); }
 public Map<String,Object> createUser(Account a) {
  require(a.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72,"Password must be at most 72 UTF-8 bytes");
  require(!users.existsByUsername(a.username()) && !users.existsByEmail(a.email()),"Username or email already exists");
  var u = User.builder().username(a.username()).fullName(a.fullName()).email(a.email()).passwordHash(encoder.encode(a.password())).userStatus(UserStatus.ACTIVE).role(found(roles.findByRoleName(a.role()))).build();
  return Views.user(users.saveAndFlush(u));
 }
 public Map<String,Object> updateUser(Integer id, AccountUpdate a) {
  var u = found(users.findById(id));
  require(!id.equals(access.current().getUserId()) || a.status()==UserStatus.ACTIVE,"Cannot deactivate your own account");
  require(a.role()==null || !id.equals(access.current().getUserId()) || a.role()==RoleName.ADMIN,"Bạn không thể tự hạ quyền Admin của tài khoản đang đăng nhập.");
  require(users.findAll().stream().noneMatch(other -> !other.getUserId().equals(id) && other.getEmail().equalsIgnoreCase(a.email())),"Email already exists");
  if(a.role()!=null) u.setRole(found(roles.findByRoleName(a.role())));
  u.setFullName(a.fullName()); u.setEmail(a.email()); u.setUserStatus(a.status());
  var saved=users.saveAndFlush(u); tokens.revokeUser(id);
  return Views.user(saved);
 }
 public void deleteUser(Integer id) { require(!id.equals(access.current().getUserId()),"Cannot delete your own account"); users.delete(found(users.findById(id))); users.flush(); tokens.revokeUser(id); }
 public List<Map<String,Object>> questions(Integer subjectId) {
  var me = access.current();
  var list = subjectId == null ? questions.findAll() : questions.findBySubjectSubjectId(subjectId);
  return list.stream().filter(q -> access.admin() || q.getCreatedBy().getUserId().equals(me.getUserId())).map(Views::question).toList();
 }
 public Question ownedQuestion(Integer id) { var q=found(questions.findById(id)); access.owner(q.getCreatedBy()); return q; }
 private void editable(Question q) {
  require(exams.findAll().stream().noneMatch(e -> e.getStatus()==ExamStatus.PUBLISHED && e.getSubject().getSubjectId().equals(q.getSubject().getSubjectId()) && e.getCreatedBy().getUserId().equals(q.getCreatedBy().getUserId())),"Question bank is in use by a published exam; complete that exam first");
 }
 public List<Map<String,Object>> importQuestions(List<QuestionInput> input) {
  require(input!=null && !input.isEmpty() && input.size()<=100,"Import between 1 and 100 questions");
  for(var question:input) if(question==null || !validator.validate(question).isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Import contains an invalid question");
  return input.stream().map(q -> saveQuestion(null,q)).toList();
 }
 public Map<String,Object> saveQuestion(Integer id, QuestionInput input) {
  var q = id==null ? new Question() : ownedQuestion(id);
  if(id!=null) editable(q);
  if(id!=null) require(logs.findAll().stream().noneMatch(l -> l.getQuestion()!=null && l.getQuestion().getQuestionId().equals(id)),"Question already used; create a new version");
  var subject=found(subjects.findById(input.subjectId()));
  SubjectDocument document = input.documentId()==null ? null : found(documents.findById(input.documentId()));
  if(document!=null) { access.owner(document.getUploadedBy()); require(document.getSubject().getSubjectId().equals(input.subjectId()),"Document belongs to another subject"); }
  q.setSubject(subject); q.setDocument(document); q.setTopic(input.topic()); q.setQuestionContent(input.questionContent()); q.setBloomLevel(input.bloomLevel());
  if(id==null) { q.setCreatedBy(access.current()); q.setSource(QuestionSource.MANUAL); }
  q.setStatus(QuestionStatus.PENDING_APPROVAL); q.setApprovedAt(null); q.setApprovedBy(null);
  return Views.question(questions.saveAndFlush(q));
 }
 public Map<String,Object> review(Integer id, Review input) {
  var q=ownedQuestion(id); require(input.status()!=QuestionStatus.PENDING_APPROVAL,"Choose APPROVED or REJECTED");
  editable(q);
  if(input.status()==QuestionStatus.APPROVED) require(!rubrics.findByQuestionQuestionId(id).isEmpty(),"Question must have a rubric before approval");
  q.setStatus(input.status()); q.setApprovedBy(access.current()); q.setApprovedAt(LocalDateTime.now()); return Views.question(q);
 }
 public void deleteQuestion(Integer id) {
  var q=ownedQuestion(id);
  editable(q);
  require(logs.findAll().stream().noneMatch(l -> l.getQuestion()!=null && l.getQuestion().getQuestionId().equals(id)),"Question already used");
  rubrics.deleteAll(rubrics.findByQuestionQuestionId(id)); questions.delete(q); questions.flush();
 }
 public List<Map<String,Object>> rubric(Integer questionId) { ownedQuestion(questionId); return rubrics.findByQuestionQuestionId(questionId).stream().map(Views::rubric).toList(); }
 public Map<String,Object> saveRubric(Integer questionId, Integer id, Criterion input) {
  var q=ownedQuestion(questionId);
  editable(q);
  require(logs.findAll().stream().noneMatch(l -> l.getQuestion()!=null && l.getQuestion().getQuestionId().equals(questionId)),"Cannot change rubric of a used question");
  var r=id==null ? new Rubric() : found(rubrics.findById(id));
  if(id!=null) require(r.getQuestion().getQuestionId().equals(questionId),"Rubric belongs to another question");
  r.setQuestion(q); r.setCriteriaName(input.criteriaName()); r.setMaxScore(input.maxScore()); r.setGuideline(input.guideline());
  q.setStatus(QuestionStatus.PENDING_APPROVAL); q.setApprovedAt(null); q.setApprovedBy(null);
  return Views.rubric(rubrics.saveAndFlush(r));
 }
 public void deleteRubric(Integer id) {
  var r=found(rubrics.findById(id)); var q=ownedQuestion(r.getQuestion().getQuestionId());
  editable(q);
  require(logs.findAll().stream().noneMatch(l -> l.getQuestion()!=null && l.getQuestion().getQuestionId().equals(q.getQuestionId())),"Cannot change rubric of a used question");
  q.setStatus(QuestionStatus.PENDING_APPROVAL); q.setApprovedAt(null); q.setApprovedBy(null); rubrics.delete(r);
 }
 public ExamSession ownedExam(Integer id) { var e=found(exams.findById(id)); access.owner(e.getCreatedBy()); return e; }
 public List<Map<String,Object>> exams() { var me=access.current(); return exams.findAll().stream().filter(e -> access.admin() || e.getCreatedBy().getUserId().equals(me.getUserId())).map(Views::exam).toList(); }
 public Map<String,Object> saveExam(Integer id, ExamInput a) {
  require(a.endTime().isAfter(a.startTime()),"End time must be after start time");
  var e=id==null ? new ExamSession() : ownedExam(id);
  if(id!=null) { require(e.getStatus()==ExamStatus.DRAFT,"Only draft exams can be edited"); require(schedules.findByExamSessionExamId(id).isEmpty(),"Remove draft schedules before editing exam"); }
  e.setSubject(found(subjects.findById(a.subjectId()))); e.setExamName(a.examName()); e.setStartTime(a.startTime()); e.setEndTime(a.endTime());
  e.setMaxMainQuestions(a.maxMainQuestions()); e.setMaxFollowUpQuestions(a.maxFollowUpQuestions()); e.setAnswerTimeLimitSeconds(a.answerTimeLimitSeconds());
  if(id==null) { e.setCreatedBy(access.current()); e.setStatus(ExamStatus.DRAFT); }
  return Views.exam(exams.saveAndFlush(e));
 }
 public Map<String,Object> publish(Integer id) {
  var e=ownedExam(id); require(e.getStatus()==ExamStatus.DRAFT,"Only draft exams can be published"); require(e.getEndTime().isAfter(LocalDateTime.now()),"Exam has already expired");
  long count=questions.findBySubjectSubjectIdAndStatus(e.getSubject().getSubjectId(),QuestionStatus.APPROVED).stream()
    .filter(q -> q.getCreatedBy().getUserId().equals(e.getCreatedBy().getUserId()) && !rubrics.findByQuestionQuestionId(q.getQuestionId()).isEmpty()).count();
  require(count>=e.getMaxMainQuestions(),"Not enough approved questions with rubrics"); require(!schedules.findByExamSessionExamId(id).isEmpty(),"Add students before publishing");
  e.setStatus(ExamStatus.PUBLISHED); return Views.exam(e);
 }
 public Map<String,Object> complete(Integer id) {
  var e=ownedExam(id); require(e.getStatus()==ExamStatus.PUBLISHED,"Exam is not published");
  var list=schedules.findByExamSessionExamId(id);
  require(list.stream().noneMatch(s -> s.getStatus()==ScheduleStatus.IN_PROGRESS),"Students are still taking the exam");
  require(e.getEndTime().isBefore(LocalDateTime.now()) || list.stream().allMatch(s -> s.getStatus()==ScheduleStatus.COMPLETED || s.getStatus()==ScheduleStatus.MISSED),"Exam is not finished");
  list.stream().filter(s -> s.getStatus()==ScheduleStatus.NOT_STARTED).forEach(s -> s.setStatus(ScheduleStatus.MISSED)); e.setStatus(ExamStatus.COMPLETED); return Views.exam(e);
 }
 public void deleteExam(Integer id) { var e=ownedExam(id); require(e.getStatus()==ExamStatus.DRAFT,"Only draft exams can be deleted"); schedules.deleteAll(schedules.findByExamSessionExamId(id)); exams.delete(e); exams.flush(); }
 public List<Map<String,Object>> schedules(Integer examId) { ownedExam(examId); return schedules.findByExamSessionExamId(examId).stream().map(Views::schedule).toList(); }
 public List<Map<String,Object>> mySchedules() { return schedules.findByStudentUserId(access.current().getUserId()).stream().map(Views::schedule).toList(); }
 public Map<String,Object> saveSchedule(Integer examId,Integer id,ScheduleInput a) {
  var e=ownedExam(examId); require(e.getStatus()==ExamStatus.DRAFT,"Only draft schedules can be changed");
  var student=found(users.findById(a.studentId())); require(student.getRole().getRoleName()==RoleName.STUDENT && student.getUserStatus()==UserStatus.ACTIVE,"Choose an active student");
  require(a.allocatedEndTime().isAfter(a.allocatedStartTime()) && !a.allocatedStartTime().isBefore(e.getStartTime()) && !a.allocatedEndTime().isAfter(e.getEndTime()),"Schedule must be inside exam time range");
  require(schedules.findByExamSessionExamId(examId).stream().noneMatch(s -> !Objects.equals(s.getScheduleId(),id) && s.getStudent().getUserId().equals(a.studentId())),"Student already scheduled");
  require(schedules.findByStudentUserId(a.studentId()).stream().noneMatch(s -> !Objects.equals(s.getScheduleId(),id) && s.getAllocatedStartTime()!=null && s.getAllocatedEndTime()!=null && a.allocatedStartTime().isBefore(s.getAllocatedEndTime()) && a.allocatedEndTime().isAfter(s.getAllocatedStartTime())),"Student has an overlapping schedule");
  var s=id==null ? new ExamSchedule() : found(schedules.findById(id));
  if(id!=null) require(s.getExamSession().getExamId().equals(examId),"Schedule belongs to another exam");
  s.setExamSession(e); s.setStudent(student); s.setAllocatedStartTime(a.allocatedStartTime()); s.setAllocatedEndTime(a.allocatedEndTime()); s.setStatus(ScheduleStatus.NOT_STARTED);
  return Views.schedule(schedules.saveAndFlush(s));
 }
 public void deleteSchedule(Integer id) { var s=found(schedules.findById(id)); var e=ownedExam(s.getExamSession().getExamId()); require(e.getStatus()==ExamStatus.DRAFT,"Only draft schedules can be deleted"); schedules.delete(s); schedules.flush(); }
}
