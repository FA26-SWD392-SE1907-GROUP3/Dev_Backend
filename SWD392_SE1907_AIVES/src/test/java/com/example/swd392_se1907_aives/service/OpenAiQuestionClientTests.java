package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.service.OpenAiQuestionClient;


import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;

class OpenAiQuestionClientTests {
    private final ObjectMapper mapper=new ObjectMapper();
    private HttpServer server;
    private String url;
    private final AtomicReference<JsonNode> received=new AtomicReference<>();
    private int responseStatus;
    private String responseBody;
    @BeforeEach void start() throws Exception {
        responseStatus=200;
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/responses",exchange->{
            received.set(mapper.readTree(exchange.getRequestBody()));
            byte[] bytes=responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(responseStatus,bytes.length);
            try(var output=exchange.getResponseBody()){output.write(bytes);}
        });
        server.start();url="http://127.0.0.1:"+server.getAddress().getPort()+"/responses";
    }
    @AfterEach void stop(){server.stop(0);}
    @Test void usesResponsesStructuredOutputAndParsesCitedDrafts() throws Exception {
        UUID chunk=UUID.randomUUID();
        String text=mapper.writeValueAsString(Map.of("questions",List.of(Map.of("content","Câu hỏi?","referenceAnswer","Đáp án","sourceChunkIds",List.of(chunk)))));
        responseBody=mapper.writeValueAsString(Map.of("status","completed","output",List.of(Map.of("type","message","content",List.of(Map.of("type","output_text","text",text))))));
        var result=client().generate("[chunkId="+chunk+"] Source",1,"UNDERSTAND","MEDIUM","");
        assertThat(result).hasSize(1);assertThat(result.getFirst().sourceChunkIds()).containsExactly(chunk);
        assertThat(received.get().path("store").asBoolean()).isFalse();
        assertThat(received.get().path("text").path("format").path("type").asText()).isEqualTo("json_schema");
        assertThat(received.get().path("text").path("format").path("strict").asBoolean()).isTrue();
    }
    @Test void rateLimitDoesNotExposeProviderResponseOrApiKey(){
        responseStatus=429;responseBody="{\"error\":\"private upstream error\"}";
        assertThatThrownBy(()->client().generate("source",1,"UNDERSTAND","EASY",""))
            .isInstanceOf(ResponseStatusException.class).hasMessageContaining("hạn mức").hasMessageNotContaining("test-key").hasMessageNotContaining("private upstream");
    }
    @Test void missingKeyDisablesGenerationWithoutCallingProvider(){
        var client=new OpenAiQuestionClient(mapper,"","test-model",url);
        assertThat(client.configured()).isFalse();
        assertThatThrownBy(()->client.generate("source",1,"UNDERSTAND","EASY","")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("OPENAI_API_KEY");
        assertThat(received.get()).isNull();
    }
    @Test void incompleteResponsesAreRejected() throws Exception {
        responseBody="{\"status\":\"incomplete\",\"output\":[]}";
        assertThatThrownBy(()->client().generate("source",1,"UNDERSTAND","EASY","")).hasMessageContaining("chưa hoàn tất");
    }
    private OpenAiQuestionClient client(){return new OpenAiQuestionClient(mapper,"test-key","test-model",url);}
}
