package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewDiagnostics;
import com.fasterxml.jackson.databind.ObjectMapper;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ConversationDiagnosticsTests {
    final ObjectMapper json=new ObjectMapper();
    final Logger logger=(Logger)LoggerFactory.getLogger(InterviewDiagnostics.class);
    final ListAppender<ILoggingEvent> logs=new ListAppender<>();
    ConversationAiClientImpl client;
    @BeforeEach void setup()throws Exception{logs.start();logger.addAppender(logs);new InterviewDiagnostics(true);
        client=new ConversationAiClientImpl(json,"sk-not-real","gpt-6.1-sol","http://localhost",new ClassPathResource("prompts/interview-system.txt"),1);}
    @AfterEach void cleanup(){client.close();logger.detachAppender(logs);logs.stop();new InterviewDiagnostics(false);}
    String event(String type,Map<String,Object> fields)throws Exception{var object=new LinkedHashMap<String,Object>();object.put("type",type);object.putAll(fields);return "data: "+json.writeValueAsString(object)+"\n\n";}
    @Test void logsFirstTokenCompletionAndValidatedLengthsWithoutRawOutput()throws Exception{
        String message="PRIVATE OUTPUT CONTENT";
        String content=json.writeValueAsString(Map.of("decision","NEXT","reason","SUFFICIENT","message",message));
        String events=event("response.output_text.delta",Map.of("delta",content))
                +event("response.completed",Map.of("response",Map.of("status","completed")));
        assertThat(client.readEvents(new BufferedReader(new StringReader(events)),ignored->{}).feedback()).isEqualTo(message);
        String log=String.join("\n",logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
        assertThat(log).contains("llm.parse.first_token","llm.parse.terminal","llm.parse.validated","\"finishReason\":\"completed\"",
                "\"rawChars\":"+content.length(),"\"messageChars\":"+message.length()).doesNotContain(message,"sk-not-real");
    }
    @Test void incompleteOutputRetainsTheProviderReasonAndStillRejectsTheResponse()throws Exception{
        String events=event("response.output_text.delta",Map.of("delta","{truncated"))
                +event("response.incomplete",Map.of("response",Map.of("status","incomplete","incomplete_details",Map.of("reason","max_output_tokens"))));
        assertThatThrownBy(()->client.readEvents(new BufferedReader(new StringReader(events)),ignored->{})).isInstanceOf(IOException.class);
        assertThat(String.join("\n",logs.list.stream().map(ILoggingEvent::getFormattedMessage).toList()))
                .contains("max_output_tokens","\"status\":\"incomplete\"").doesNotContain("{truncated");
    }
}
