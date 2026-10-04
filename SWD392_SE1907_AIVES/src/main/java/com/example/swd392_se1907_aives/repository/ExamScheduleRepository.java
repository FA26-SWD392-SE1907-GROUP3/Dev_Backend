package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Integer> { List<ExamSchedule> findByExamSessionExamId(Integer examId); List<ExamSchedule> findByStudentUserId(Integer userId); boolean existsByExamSessionExamIdAndStudentUserId(Integer examId, Integer studentId); @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select s from ExamSchedule s where s.scheduleId = :id") Optional<ExamSchedule> lockById(@org.springframework.data.repository.query.Param("id") Integer id); }
