package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.InterviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface InterviewLogRepository extends JpaRepository<InterviewLog, Integer> { List<InterviewLog> findByExamScheduleScheduleIdOrderBySequenceNumberAsc(Integer scheduleId); }
