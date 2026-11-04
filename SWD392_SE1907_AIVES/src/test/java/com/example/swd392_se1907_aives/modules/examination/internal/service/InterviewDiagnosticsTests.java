package com.example.swd392_se1907_aives.modules.examination.internal.service;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.slf4j.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class InterviewDiagnosticsTests {
    final ch.qos.logback.classic.Logger logger=(ch.qos.logback.classic.Logger)LoggerFactory.getLogger(InterviewDiagnostics.class);
    final ListAppender<ILoggingEvent> appender=new ListAppender<>();
    @BeforeEach void setup(){appender.start();logger.addAppender(appender);new InterviewDiagnostics(true);}
    @AfterEach void cleanup(){logger.detachAppender(appender);appender.stop();new InterviewDiagnostics(false);MDC.clear();}
    @Test void logsOnlyMetadataAndNeverRawPromptsAudioAnswersOrSecrets()throws Exception{
        InterviewDiagnostics.event("diagnostic.test","model","gpt-6.1-sol","rawChars",300,"messageChars",120,
                "text","PRIVATE-TRANSCRIPT","referenceAnswer","PRIVATE-ANSWER","apiKey","sk-private-key",
                "voiceId","sk-private-key","body",Map.of("secret","PRIVATE-BODY"));
        assertThat(appender.list).hasSize(1);
        String output=appender.list.getFirst().getFormattedMessage();
        assertThat(output).doesNotContain("PRIVATE-","sk-private-key","referenceAnswer","apiKey");
        var record=new ObjectMapper().readTree(output.substring("INTERVIEW_DIAG ".length()));
        assertThat(record.path("model").asText()).isEqualTo("gpt-6.1-sol");
        assertThat(record.path("rawChars").asInt()).isEqualTo(300);
    }
    @Test void scopesRestoreTheRequestContextAndSpansRetainCorrelationForCallbacks()throws Exception{
        UUID attempt=UUID.randomUUID(),prompt=UUID.randomUUID();InterviewDiagnostics.Span trace;
        try(var scope=InterviewDiagnostics.scope(attempt,prompt,null)){trace=InterviewDiagnostics.start("tts.realtime","voiceId","marin");}
        assertThat(MDC.get("interview.attemptId")).isNull();
        trace.event("first_byte","firstByteMs",12);
        var event=new ObjectMapper().readTree(appender.list.getLast().getFormattedMessage().substring("INTERVIEW_DIAG ".length()));
        assertThat(event.path("attemptId").asText()).isEqualTo(attempt.toString());
        assertThat(event.path("promptId").asText()).isEqualTo(prompt.toString());
        assertThat(MDC.get("interview.attemptId")).isNull();
    }
    @Test void theFlagDisablesAllDiagnosticOutput(){
        new InterviewDiagnostics(false);InterviewDiagnostics.event("not_emitted","rawChars",50);
        InterviewDiagnostics.start("not_emitted").event("end");assertThat(appender.list).isEmpty();
    }
}
