package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.service.ConversationAiClient;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDecision;
import java.util.List;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Component
public class ConversationAiClientImpl implements ConversationAiClient {
    private final ObjectMapper json;
    private final String key, model, instructions;
    private final URI endpoint;
    private final int timeoutSeconds;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
        var thread = new Thread(r, "conversation-ai-timeout"); thread.setDaemon(true); return thread;
    });

    public ConversationAiClientImpl(ObjectMapper json,
            @Value("${app.ai.api-key:}") String key,
            @Value("${app.interview.followup-model:gpt-4.1-mini}") String model,
            @Value("${app.interview.openai-base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("classpath:prompts/interview-system.txt") Resource prompt,
            @Value("${app.interview.stream-timeout-seconds:60}") int timeoutSeconds) throws IOException {
        this.json = json; this.key = key; this.model = model;
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/responses");
        this.instructions = prompt.getContentAsString(StandardCharsets.UTF_8);
        this.timeoutSeconds = Math.max(1, Math.min(65, timeoutSeconds));
    }

    @Override
    public InterviewAiClient.Decision stream(String context, Consumer<String> onText) {
        if (key.isBlank()) throw failure(HttpStatus.SERVICE_UNAVAILABLE, "Chưa cấu hình AI cho phòng thi.");
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        for (int attempt = 0; attempt < 2; attempt++) {
            long remaining = deadlineNanos - System.nanoTime();
            if (remaining <= 0) break;
            // Both attempts share one wall-clock budget, including stream consumption.
            long budgetMillis = Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining) / (attempt == 0 ? 2 : 1));
            try {
                var schema = Map.of("type", "object", "additionalProperties", false,
                        "properties", Map.of(
                                "decision", Map.of("type", "string", "enum", List.of("FOLLOW_UP", "NEXT")),
                                "reason", Map.of("type", "string", "enum", List.of("VAGUE", "MISSING", "CONTRADICTION", "SUFFICIENT")),
                                "message", Map.of("type", "string")),
                        "required", List.of("decision", "reason", "message"));
                var payload = Map.of("model", model, "store", false, "stream", true, "max_output_tokens", 900,
                        "instructions", instructions, "input", context,
                        "text", Map.of("format", Map.of("type", "json_schema", "name", "interview_decision", "strict", true, "schema", schema)));
                var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofMillis(budgetMillis))
                        .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(payload))).build();
                long requestStarted = System.nanoTime();
                var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (var body = response.body()) {
                    if (response.statusCode() != 200) {
                        if (response.statusCode() >= 500 && attempt == 0) continue;
                        break;
                    }
                    var timeout = watchdog.schedule(() -> { try { body.close(); } catch (IOException ignored) {} },
                            Math.max(1, budgetMillis - Duration.ofNanos(System.nanoTime() - requestStarted).toMillis()), TimeUnit.MILLISECONDS);
                    try (var reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
                        return readEvents(reader, onText);
                    } finally { timeout.cancel(false); }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw failure(HttpStatus.BAD_GATEWAY, "Yêu cầu AI bị gián đoạn. Câu trả lời đã lưu.");
            } catch (IOException e) {
                // A transport failure or invalid JSON gets one retry. Never re-save the student's answer.
                if (Thread.currentThread().isInterrupted())
                    throw failure(HttpStatus.BAD_GATEWAY, "Yêu cầu AI bị gián đoạn. Câu trả lời đã lưu.");
            }
        }
        var fallback = new InterviewAiClient.Decision(false, "", "AI_UNAVAILABLE",
                "Tôi đã ghi nhận phần trình bày của em. Chúng ta cùng chuyển sang nội dung tiếp theo.");
        onText.accept(fallback.feedback());
        return fallback;
    }

    // Buffer the short JSON response until validated. No control fields or unsafe partial JSON reach speech.
    InterviewAiClient.Decision readEvents(BufferedReader reader, Consumer<String> onText) throws IOException {
        StringBuilder text = new StringBuilder(), data = new StringBuilder();
        boolean completed = false;
        for (String line; (line = reader.readLine()) != null;) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
            if (line.length() > 100000) throw new IOException("Oversized event");
            if (line.startsWith("data:")) {
                if (!data.isEmpty()) data.append('\n');
                data.append(line.substring(5).stripLeading());
                if (data.length() > 100000) throw new IOException("Oversized event");
            }
            if (!line.isEmpty() || data.isEmpty()) continue;
            var event = json.readTree(data.toString()); data.setLength(0);
            String type = event.path("type").asText();
            if ("response.output_text.delta".equals(type)) {
                text.append(event.path("delta").asText());
                if (text.length() > 16000) throw new IOException("Answer too long");
            } else if ("response.completed".equals(type)) {
                if (!"completed".equals(event.path("response").path("status").asText()))
                    throw new IOException("Incomplete response");
                completed = true; break;
            } else if (type.equals("error") || type.equals("response.failed") || type.equals("response.incomplete")
                    || type.startsWith("response.refusal")) throw new IOException("Provider failed");
        }
        if (!completed) throw new IOException("Stream ended before completion");
        final InterviewAiClient.Decision decision;
        try (var parser = json.getFactory().createParser(text.toString())) {
            parser.enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            com.fasterxml.jackson.databind.JsonNode node = json.readTree(parser);
            if (parser.nextToken() != null) throw new IOException("Trailing JSON content");
            if (node == null || !node.isObject() || node.size() != 3
                    || !node.path("decision").isTextual() || !node.path("reason").isTextual() || !node.path("message").isTextual())
                throw new IllegalArgumentException("Invalid fields");
            decision = new InterviewDecision(
                    InterviewDecision.Decision.valueOf(node.path("decision").asText()),
                    InterviewDecision.Reason.valueOf(node.path("reason").asText()), node.path("message").asText()).toInternal();
        } catch (IllegalArgumentException e) { throw new IOException("Invalid interview decision", e); }
        onText.accept(decision.askFollowup() ? decision.question() : decision.feedback());
        return decision;
    }

    private ResponseStatusException failure(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
    @PreDestroy public void close() { watchdog.shutdownNow(); http.close(); }
}
