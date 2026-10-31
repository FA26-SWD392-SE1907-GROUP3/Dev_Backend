package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ConversationAiClientImplTests {
    private final ObjectMapper json = new ObjectMapper();

    private String event(String type, Object payload) throws Exception {
        var data = new java.util.HashMap<String, Object>(); data.put("type", type);
        if (type.equals("response.output_text.delta")) data.put("delta", payload); else data.put("response", payload);
        return "data: " + json.writeValueAsString(data) + "\n\n";
    }
    private ConversationAiClientImpl client() throws IOException {
        return new ConversationAiClientImpl(json, "test", "test", "http://localhost", new ClassPathResource("prompts/interview-system.txt"), 1);
    }
    @Test void streamsOnlyTextAndRequiresCompletedLifecycle() throws Exception {
        var client = client(); var chunks = new ArrayList<String>();
        try {
            var stream = event("response.created", Map.of()) + event("response.output_text.delta", "Cảm ơn. ")
                    + event("response.output_text.delta", "Vì sao?") + event("response.completed", Map.of("status", "completed"));
            var result = client.readEvents(new BufferedReader(new StringReader(stream)), chunks::add);
            assertThat(String.join("", chunks)).isEqualTo("Cảm ơn. Vì sao?");
            assertThat(result.askFollowup()).isTrue();
            assertThatThrownBy(() -> client.readEvents(new BufferedReader(new StringReader("")), chunks::add)).isInstanceOf(IOException.class);
        } finally { client.close(); }
    }
    @Test void splitControlTokenIsNeverShownOrSpoken() throws Exception {
        var client = client(); var chunks = new ArrayList<String>();
        try {
            var stream = event("response.output_text.delta", "[[NE") + event("response.output_text.delta", "XT]]")
                    + event("response.completed", Map.of("status", "completed"));
            assertThat(client.readEvents(new BufferedReader(new StringReader(stream)), chunks::add).askFollowup()).isFalse();
            assertThat(chunks).isEmpty();
        } finally { client.close(); }
    }
    @Test void rejectsMultipleQuestionsAndIncompleteProviderResponses() throws Exception {
        var client = client();
        try {
            var multiple = event("response.output_text.delta", "Vì sao? Khi nào?") + event("response.completed", Map.of("status", "completed"));
            assertThatThrownBy(() -> client.readEvents(new BufferedReader(new StringReader(multiple)), text -> {})).isInstanceOf(IOException.class);
            var incomplete = event("response.completed", Map.of("status", "incomplete"));
            assertThatThrownBy(() -> client.readEvents(new BufferedReader(new StringReader(incomplete)), text -> {})).isInstanceOf(IOException.class);
        } finally { client.close(); }
    }

    @Test void providerRequestUsesStreamingAndMapsQuotaFailureWithoutLeakingPayload() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        var requestBody = new java.util.concurrent.atomic.AtomicReference<String>();
        server.createContext("/responses", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(429, -1); exchange.close();
        });
        server.start();
        var client = new ConversationAiClientImpl(json, "test-not-real", "configured-model", "http://127.0.0.1:" + server.getAddress().getPort(), new ClassPathResource("prompts/interview-system.txt"), 1);
        try {
            assertThatThrownBy(() -> client.stream("PRIVATE-CONTEXT", text -> {}))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .hasMessageContaining("503").hasMessageNotContaining("PRIVATE-CONTEXT");
            var sent = json.readTree(requestBody.get());
            assertThat(sent.path("stream").asBoolean()).isTrue();
            assertThat(sent.path("store").asBoolean()).isFalse();
            assertThat(sent.path("model").asText()).isEqualTo("configured-model");
        } finally { server.stop(0); client.close(); }
    }

    @Test void stalledStreamBodyHasATimeoutAndCannotBecomeACompletedTurn() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        var worker = java.util.concurrent.Executors.newSingleThreadExecutor(); server.setExecutor(worker);
        server.createContext("/responses", exchange -> {
            try {
                exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write("data: {\"type\":\"response.created\"}\n\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();
                Thread.sleep(3000);
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        }); server.start();
        var client = new ConversationAiClientImpl(json, "test-not-real", "test", "http://127.0.0.1:" + server.getAddress().getPort(), new ClassPathResource("prompts/interview-system.txt"), 1);
        long started = System.nanoTime();
        try {
            assertThatThrownBy(() -> client.stream("context", text -> {}))
                    .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                    .hasMessageContaining("Câu trả lời đã lưu");
            assertThat(java.time.Duration.ofNanos(System.nanoTime() - started).toMillis()).isLessThan(2500);
        } finally { server.stop(0); worker.shutdownNow(); client.close(); }
    }
}
