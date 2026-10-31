package com.example.swd392_se1907_aives.modules.examination.internal.config;

import com.example.swd392_se1907_aives.modules.examination.internal.service.AttemptGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration @RequiredArgsConstructor
@EnableScheduling
@ConditionalOnProperty(name="app.grading.enabled",havingValue="true",matchIfMissing=true)
public class AttemptGradingWorker {
    private final AttemptGradingService service;
    @Bean(name="taskScheduler") public ThreadPoolTaskScheduler taskScheduler(){
        var scheduler=new ThreadPoolTaskScheduler();scheduler.setPoolSize(2);scheduler.setThreadNamePrefix("interview-clock-");return scheduler;
    }
    @Bean(name="gradingScheduler") public ThreadPoolTaskScheduler gradingScheduler(){
        var scheduler=new ThreadPoolTaskScheduler();scheduler.setPoolSize(1);scheduler.setThreadNamePrefix("attempt-grading-");return scheduler;
    }
    @Scheduled(fixedDelay=5000,initialDelay=10000,scheduler="gradingScheduler")
    public void grade(){service.processPending();}
}
