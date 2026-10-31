package com.example.swd392_se1907_aives.modules.examination.internal.config;

import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewStateStore;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration @EnableScheduling @RequiredArgsConstructor
@ConditionalOnProperty(name="app.interview.timeouts-enabled",havingValue="true",matchIfMissing=true)
public class InterviewTimeoutWorker {
    private final ExamAttemptRepository attempts;
    private final InterviewStateStore store;
    @Scheduled(fixedDelay=5000) public void expire(){
        for(var attempt:attempts.findTop100ByStatusOrderByLastActivityAtAsc("IN_PROGRESS"))try{store.advanceSystem(attempt.getId());}
        catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not advance interview {} ({})",attempt.getId(),e.getClass().getSimpleName());}
    }
}
