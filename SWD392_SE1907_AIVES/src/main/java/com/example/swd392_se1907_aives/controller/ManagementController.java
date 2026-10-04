package com.example.swd392_se1907_aives.controller;
import com.example.swd392_se1907_aives.dto.ApiModels.*;
import com.example.swd392_se1907_aives.service.ManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.*;
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ManagementController {
 private final ManagementService service;
 @GetMapping("/roles") @PreAuthorize("hasRole('ADMIN')") public Object roles() { return service.roles(); }
 @GetMapping("/users/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object user(@PathVariable Integer id) { return service.user(id); }
 @GetMapping("/questions/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object question(@PathVariable Integer id) { return service.question(id); }
 @GetMapping("/exams/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object exam(@PathVariable Integer id) { return service.exam(id); }
 @GetMapping("/schedules/{id}") public Object schedule(@PathVariable Integer id) { return service.schedule(id); }
 @PostMapping("/questions/import") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object importQuestions(@Valid @RequestBody List<@Valid QuestionInput> input) { return service.importQuestions(input); }
 @GetMapping("/users") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object users() { return service.users(); }
 @PostMapping("/users") @PreAuthorize("hasRole('ADMIN')") public Object createUser(@Valid @RequestBody Account a) { return service.createUser(a); }
 @PutMapping("/users/{id}") @PreAuthorize("hasRole('ADMIN')") public Object updateUser(@PathVariable Integer id,@Valid @RequestBody AccountUpdate a) { return service.updateUser(id,a); }
 @DeleteMapping("/users/{id}") @PreAuthorize("hasRole('ADMIN')") public void deleteUser(@PathVariable Integer id) { service.deleteUser(id); }
 @GetMapping("/questions") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object questions(@RequestParam(required=false) Integer subjectId) { return service.questions(subjectId); }
 @PostMapping("/questions") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object createQuestion(@Valid @RequestBody QuestionInput a) { return service.saveQuestion(null,a); }
 @PutMapping("/questions/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object updateQuestion(@PathVariable Integer id,@Valid @RequestBody QuestionInput a) { return service.saveQuestion(id,a); }
 @PostMapping("/questions/{id}/review") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object review(@PathVariable Integer id,@Valid @RequestBody Review a) { return service.review(id,a); }
 @DeleteMapping("/questions/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public void deleteQuestion(@PathVariable Integer id) { service.deleteQuestion(id); }
 @GetMapping("/questions/{questionId}/rubrics") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object rubrics(@PathVariable Integer questionId) { return service.rubric(questionId); }
 @PostMapping("/questions/{questionId}/rubrics") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object createRubric(@PathVariable Integer questionId,@Valid @RequestBody Criterion a) { return service.saveRubric(questionId,null,a); }
 @PutMapping("/questions/{questionId}/rubrics/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object updateRubric(@PathVariable Integer questionId,@PathVariable Integer id,@Valid @RequestBody Criterion a) { return service.saveRubric(questionId,id,a); }
 @DeleteMapping("/rubrics/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public void deleteRubric(@PathVariable Integer id) { service.deleteRubric(id); }
 @GetMapping("/exams") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object exams() { return service.exams(); }
 @PostMapping("/exams") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object createExam(@Valid @RequestBody ExamInput a) { return service.saveExam(null,a); }
 @PutMapping("/exams/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object updateExam(@PathVariable Integer id,@Valid @RequestBody ExamInput a) { return service.saveExam(id,a); }
 @PostMapping("/exams/{id}/publish") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object publish(@PathVariable Integer id) { return service.publish(id); }
 @PostMapping("/exams/{id}/complete") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object complete(@PathVariable Integer id) { return service.complete(id); }
 @DeleteMapping("/exams/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public void deleteExam(@PathVariable Integer id) { service.deleteExam(id); }
 @GetMapping("/exams/{examId}/schedules") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object schedules(@PathVariable Integer examId) { return service.schedules(examId); }
 @PostMapping("/exams/{examId}/schedules") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object createSchedule(@PathVariable Integer examId,@Valid @RequestBody ScheduleInput a) { return service.saveSchedule(examId,null,a); }
 @PutMapping("/exams/{examId}/schedules/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public Object updateSchedule(@PathVariable Integer examId,@PathVariable Integer id,@Valid @RequestBody ScheduleInput a) { return service.saveSchedule(examId,id,a); }
 @DeleteMapping("/schedules/{id}") @PreAuthorize("hasAnyRole('ADMIN','LECTURER')") public void deleteSchedule(@PathVariable Integer id) { service.deleteSchedule(id); }
 @GetMapping("/my-schedules") @PreAuthorize("hasRole('STUDENT')") public Object mySchedules() { return service.mySchedules(); }
}
