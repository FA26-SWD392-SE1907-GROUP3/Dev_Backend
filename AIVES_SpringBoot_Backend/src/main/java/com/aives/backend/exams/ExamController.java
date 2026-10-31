package com.aives.backend.exams;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.aives.backend.common.AccessControl;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ExamController {
    private final NamedParameterJdbcTemplate db;
    private final AccessControl access;
    public ExamController(NamedParameterJdbcTemplate db, AccessControl access) {
        this.db = db; this.access = access;
    }

    public enum Mode { RANDOM, ADAPTIVE }
    public record CreateExam(@NotBlank String title, String instructions,
        @NotNull Mode selectionMode, @Min(1) @Max(100) int mainQuestionCount,
        @Min(0) @Max(10) int maxFollowupsPerQuestion,
        @Min(10) @Max(3600) int answerTimeLimitSeconds,
        @Min(1) @Max(240) int slotDurationMinutes) {}
    public record ScheduleExam(@NotNull Instant startsAt, @NotNull Instant endsAt) {}
    public record AddCandidate(@NotNull UUID studentId, @NotNull Instant scheduledStart,
                               @NotNull Instant scheduledEnd) {}

    @GetMapping("/courses/{courseId}/exams")
    public List<Map<String, Object>> list(@PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt) {
        access.member(courseId, jwt);
        String filter = "STUDENT".equals(access.role(jwt))
            ? " AND e.status IN ('SCHEDULED','ACTIVE','CLOSED') AND EXISTS (SELECT 1 FROM exam_candidates ec " +
              "WHERE ec.exam_id=e.id AND ec.student_id=:uid)" : "";
        return db.queryForList("SELECT e.id, e.title, e.status, e.selection_mode, e.main_question_count, " +
                "e.starts_at, e.ends_at FROM exams e WHERE e.course_id=:course" + filter + " ORDER BY e.created_at DESC",
            Map.of("course", courseId, "uid", access.userId(jwt)));
    }

    @PostMapping("/courses/{courseId}/exams")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public Map<String, Object> create(@PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody CreateExam req) {
        access.lecturer(courseId, jwt);
        UUID id = UUID.randomUUID();
        db.update("""
            INSERT INTO exams(id,course_id,created_by,title,instructions,selection_mode,main_question_count,
              max_followups_per_question,answer_time_limit_seconds,slot_duration_minutes)
            VALUES (:id,:course,:uid,:title,:instructions,:mode,:count,:followups,:seconds,:duration)
            """, new MapSqlParameterSource().addValue("id", id).addValue("course", courseId)
            .addValue("uid", access.userId(jwt)).addValue("title", req.title())
            .addValue("instructions", req.instructions()).addValue("mode", req.selectionMode().name())
            .addValue("count", req.mainQuestionCount()).addValue("followups", req.maxFollowupsPerQuestion())
            .addValue("seconds", req.answerTimeLimitSeconds()).addValue("duration", req.slotDurationMinutes()));
        return Map.of("id", id, "status", "DRAFT");
    }

    @PostMapping("/exams/{examId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public Map<String, String> pool(@PathVariable UUID examId, @PathVariable UUID questionId,
                                     @AuthenticationPrincipal Jwt jwt) {
        UUID courseId = examCourse(examId);
        access.lecturer(courseId, jwt);
        int count = db.update("""
            INSERT INTO exam_question_pool (exam_id,question_id,course_id)
            SELECT e.id, q.id, e.course_id FROM exams e JOIN questions q ON q.course_id=e.course_id
            WHERE e.id=:exam AND e.status='DRAFT' AND q.id=:question AND q.status='APPROVED'
            """, Map.of("exam", examId, "question", questionId));
        if (count == 0) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Exam must be DRAFT and question must be approved in the same course");
        return Map.of("message", "Question added to exam pool");
    }

    @PostMapping("/exams/{examId}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public Map<String, String> schedule(@PathVariable UUID examId, @AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody ScheduleExam req) {
        access.lecturer(examCourse(examId), jwt);
        if (!req.startsAt().isAfter(Instant.now()) || !req.endsAt().isAfter(req.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam start must be in the future and end after start");
        }
        int changed = db.update("""
            UPDATE exams e SET status='SCHEDULED', starts_at=:starts, ends_at=:ends
            WHERE e.id=:exam AND e.status='DRAFT'
              AND (SELECT count(*) FROM exam_question_pool eqp JOIN questions q ON q.id=eqp.question_id
                   WHERE eqp.exam_id=e.id AND q.status='APPROVED') >= e.main_question_count
            """, new MapSqlParameterSource().addValue("exam", examId)
                .addValue("starts", java.sql.Timestamp.from(req.startsAt())).addValue("ends", java.sql.Timestamp.from(req.endsAt())));
        if (changed == 0) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Exam must be DRAFT and have enough approved questions");
        return Map.of("status", "SCHEDULED");
    }

    @PostMapping("/exams/{examId}/candidates")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @Transactional
    public Map<String, Object> addCandidate(@PathVariable UUID examId, @AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody AddCandidate req) {
        UUID courseId = examCourse(examId);
        access.lecturer(courseId, jwt);
        var exams = db.query("SELECT starts_at, ends_at, slot_duration_minutes, status FROM exams WHERE id=:id",
            Map.of("id", examId), (rs, n) -> new ExamSchedule(
                rs.getTimestamp("starts_at") == null ? null : rs.getTimestamp("starts_at").toInstant(),
                rs.getTimestamp("ends_at") == null ? null : rs.getTimestamp("ends_at").toInstant(),
                rs.getInt("slot_duration_minutes"), rs.getString("status")));
        ExamSchedule exam = exams.get(0);
        long minutes = java.time.Duration.between(req.scheduledStart(), req.scheduledEnd()).toMinutes();
        if (!"SCHEDULED".equals(exam.status()) || req.scheduledStart().isBefore(exam.start()) ||
            req.scheduledEnd().isAfter(exam.end()) || minutes != exam.duration() ||
            !req.scheduledStart().plusSeconds(exam.duration() * 60L).equals(req.scheduledEnd())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slot must fit the scheduled exam and configured slot duration");
        }
        int active = access.count("SELECT count(*) FROM course_enrollments WHERE course_id=:course " +
                "AND student_id=:student AND enrollment_status='ACTIVE'",
            Map.of("course", courseId, "student", req.studentId()));
        if (active == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student not actively enrolled");
        int overlapping = access.count("""
            SELECT count(*) FROM exam_candidates WHERE exam_id=:exam
            AND scheduled_start < :slotEnd AND scheduled_end > :slotStart
            """, new MapSqlParameterSource().addValue("exam", examId)
            .addValue("slotStart", java.sql.Timestamp.from(req.scheduledStart())).addValue("slotEnd", java.sql.Timestamp.from(req.scheduledEnd())).getValues());
        // This starter models a single oral-exam booth. Parallel booths need an explicit booth_id.
        if (overlapping > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Slot overlaps another candidate");
        UUID id = UUID.randomUUID();
        db.update("""
            INSERT INTO exam_candidates(id,exam_id,course_id,student_id,scheduled_start,scheduled_end)
            VALUES (:id,:exam,:course,:student,:starts,:ends)
            """, new MapSqlParameterSource().addValue("id", id).addValue("exam", examId)
            .addValue("course", courseId).addValue("student", req.studentId())
            .addValue("starts", java.sql.Timestamp.from(req.scheduledStart())).addValue("ends", java.sql.Timestamp.from(req.scheduledEnd())));
        return Map.of("id", id, "status", "REGISTERED");
    }

    @GetMapping("/exams/me")
    public List<Map<String, Object>> myExams(@AuthenticationPrincipal Jwt jwt) {
        access.isStudent(jwt);
        return db.queryForList("""
            SELECT e.id AS exam_id, e.title, e.status AS exam_status, e.course_id,
              ec.id AS candidate_id, ec.status AS candidate_status, ec.scheduled_start, ec.scheduled_end
            FROM exam_candidates ec JOIN exams e ON e.id=ec.exam_id
            WHERE ec.student_id=:student ORDER BY ec.scheduled_start DESC
            """, Map.of("student", access.userId(jwt)));
    }

    private UUID examCourse(UUID examId) {
        var ids = db.query("SELECT course_id FROM exams WHERE id=:id", Map.of("id", examId),
            (rs,n) -> rs.getObject("course_id", UUID.class));
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found");
        return ids.get(0);
    }

    private record ExamSchedule(Instant start, Instant end, int duration, String status) {}
}
