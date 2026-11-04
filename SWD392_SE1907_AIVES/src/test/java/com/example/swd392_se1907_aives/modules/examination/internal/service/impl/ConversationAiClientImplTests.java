package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ConversationAiClientImplTests {
    private final ObjectMapper json = new ObjectMapper();
    private String event(String type, Object payload) throws Exception {
        var data = new HashMap<String,Object>(); data.put("type",type);
        if(type.equals("response.output_text.delta"))data.put("delta",payload);else data.put("response",payload);
        return "data: "+json.writeValueAsString(data)+"\n\n";
    }
    private String response(String content)throws Exception {
        int mid=content.length()/2;
        return event("response.output_text.delta",content.substring(0,mid))
            +event("response.output_text.delta",content.substring(mid))+event("response.completed",Map.of("status","completed"));
    }
    private ConversationAiClientImpl client(String url)throws IOException {
        return new ConversationAiClientImpl(json,"test-not-real","configured-model",url,new ClassPathResource("prompts/interview-system.txt"),1);
    }
    @ParameterizedTest @ValueSource(strings={"VAGUE","MISSING","CONTRADICTION"})
    void validatesAllFollowupReasonsAndOnlyEmitsTheSpokenMessage(String reason)throws Exception {
        var client=client("http://localhost");var chunks=new ArrayList<String>();
        try {
            String message="Tôi nghe em nói về tách trách nhiệm. Em giải thích thêm ý này được không?";
            var result=client.readEvents(new BufferedReader(new StringReader(response(json.writeValueAsString(Map.of(
                "decision","FOLLOW_UP","reason",reason,"message",message))))),chunks::add);
            assertThat(result.askFollowup()).isTrue();assertThat(result.reason()).isEqualTo(reason);
            assertThat(result.question()).isEqualTo(message);assertThat(chunks).containsExactly(message);
        }finally{client.close();}
    }
    @Test void nextContainsAContextualAcknowledgementInsteadOfAControlToken()throws Exception {
        var client=client("http://localhost");var chunks=new ArrayList<String>();
        try {
            var result=client.readEvents(new BufferedReader(new StringReader(response(json.writeValueAsString(Map.of(
                "decision","NEXT","reason","SUFFICIENT","message","Tôi đã ghi nhận cách em chia các thành phần."))))),chunks::add);
            assertThat(result.askFollowup()).isFalse();assertThat(result.feedback()).contains("chia các thành phần");
            assertThat(String.join("",chunks)).doesNotContain("decision","SUFFICIENT","[[NEXT]]");
        }finally{client.close();}
    }
    @Test void rejectsMalformedUnknownAndInconsistentJsonWithoutEmittingAnyPartialContent()throws Exception {
        var client=client("http://localhost");var chunks=new ArrayList<String>();
        try {
            for(String invalid:List.of("[[NEXT]]","{broken}",
                "{\"decision\":\"FOLLOW_UP\",\"reason\":\"SUFFICIENT\",\"message\":\"Vì sao?\"}",
                "{\"decision\":\"FOLLOW_UP\",\"reason\":\"MISSING\",\"message\":\"Vì sao? Khi nào?\"}",
                "{\"decision\":\"NEXT\",\"reason\":\"OTHER\",\"message\":\"Được.\"}",
                "{\"decision\":\"NEXT\",\"reason\":\"SUFFICIENT\",\"message\":\"Được.\",\"extra\":true}")) {
                String stream=response(invalid);
                assertThatThrownBy(()->client.readEvents(new BufferedReader(new StringReader(stream)),chunks::add)).isInstanceOf(IOException.class);
            }
            assertThatThrownBy(()->client.readEvents(new BufferedReader(new StringReader("")),chunks::add)).isInstanceOf(IOException.class);
            assertThat(chunks).isEmpty();
        }finally{client.close();}
    }
    @ParameterizedTest @ValueSource(strings={"configured-model","gpt-6.1-sol"})
    void quotaFailureFallsBackAndRequestUsesStrictSchemaWithoutStoringProviderData(String model)throws Exception {
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var body=new java.util.concurrent.atomic.AtomicReference<String>();
        server.createContext("/responses",e->{body.set(new String(e.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));e.sendResponseHeaders(429,-1);e.close();});server.start();
        var client=new ConversationAiClientImpl(json,"test-not-real",model,
            "http://127.0.0.1:"+server.getAddress().getPort(),new ClassPathResource("prompts/interview-system.txt"),1);
        try {
            var result=client.stream("PRIVATE-CONTEXT",text->{});
            assertThat(result.askFollowup()).isFalse();assertThat(result.reason()).isEqualTo("AI_UNAVAILABLE");
            var sent=json.readTree(body.get());assertThat(sent.path("stream").asBoolean()).isTrue();assertThat(sent.path("store").asBoolean()).isFalse();
            assertThat(sent.path("model").asText()).isEqualTo(model);
            if (model.equals("gpt-6.1-sol")) {
                assertThat(sent.at("/reasoning/effort").asText()).isEqualTo("low");
                assertThat(sent.path("max_output_tokens").asInt()).isEqualTo(4096);
            } else {
                assertThat(sent.has("reasoning")).isFalse();
                assertThat(sent.path("max_output_tokens").asInt()).isEqualTo(900);
            }
            assertThat(sent.at("/text/format/type").asText()).isEqualTo("json_schema");assertThat(sent.at("/text/format/strict").asBoolean()).isTrue();
        }finally{server.stop(0);client.close();}
    }
    @Test void malformedProviderJsonIsRetriedOnlyOnceThenFallsBack()throws Exception {
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var count=new java.util.concurrent.atomic.AtomicInteger();var bytes=response("not-json").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        server.createContext("/responses",e->{count.incrementAndGet();e.getRequestBody().readAllBytes();e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();});server.start();
        var client=client("http://127.0.0.1:"+server.getAddress().getPort());var chunks=new ArrayList<String>();
        try{assertThat(client.stream("context",chunks::add).reason()).isEqualTo("AI_UNAVAILABLE");assertThat(count.get()).isEqualTo(2);assertThat(chunks).hasSize(1);}
        finally{server.stop(0);client.close();}
    }
    @Test void stalledProviderHasOneSharedTimeoutBudgetAndFallsBack()throws Exception {
        var server=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        var worker=java.util.concurrent.Executors.newCachedThreadPool();server.setExecutor(worker);
        server.createContext("/responses",e->{try{e.sendResponseHeaders(200,0);e.getResponseBody().write("data: {\"type\":\"response.created\"}\n\n".getBytes());e.getResponseBody().flush();Thread.sleep(5000);}catch(Exception ignored){}finally{e.close();}});server.start();
        var client=client("http://127.0.0.1:"+server.getAddress().getPort());long start=System.nanoTime();
        try{assertThat(client.stream("context",text->{}).reason()).isEqualTo("AI_UNAVAILABLE");assertThat(java.time.Duration.ofNanos(System.nanoTime()-start).toMillis()).isLessThan(2500);}
        finally{server.stop(0);worker.shutdownNow();client.close();}
    }
}
