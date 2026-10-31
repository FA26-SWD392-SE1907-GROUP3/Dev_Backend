package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.service.ConversationAiClient;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        long started = System.nanoTime();
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(Map.of(
                            "model", model, "store", false, "stream", true, "max_output_tokens", 900,
                            "instructions", instructions, "input", context)))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var body = response.body()) {
                if (response.statusCode() != 200) throw failure(
                        response.statusCode() == 429 || response.statusCode() == 401 || response.statusCode() == 403
                                ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY,
                        "Dịch vụ AI tạm thời chưa sẵn sàng. Câu trả lời đã lưu, hãy thử tiếp tục.");
                // HttpRequest timeout does not cover consumption of an InputStream body.
                var deadline = watchdog.schedule(() -> { try { body.close(); } catch (IOException ignored) {} },
                        Math.max(1, timeoutSeconds * 1000L - Duration.ofNanos(System.nanoTime() - started).toMillis()), TimeUnit.MILLISECONDS);
                try (var reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
                    return readEvents(reader, onText);
                } finally { deadline.cancel(false); }
            }
        } catch (ResponseStatusException e) { throw e; }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw failure(HttpStatus.BAD_GATEWAY, "Yêu cầu AI bị gián đoạn. Câu trả lời đã lưu.");
        } catch (IOException e) {
            throw failure(HttpStatus.BAD_GATEWAY, "AI mất kết nối hoặc hết thời gian. Câu trả lời đã lưu, hãy thử tiếp tục.");
        }
    }

    // Package-visible for provider-protocol tests; never forward provider payloads or reasoning.
    InterviewAiClient.Decision readEvents(BufferedReader reader, Consumer<String> onText) throws IOException {
        StringBuilder text = new StringBuilder(), data = new StringBuilder();
        boolean completed = false;
        int emitted = 0;
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
                if (text.length() > 2000) throw new IOException("Answer too long");
                // Hold the control token even when split across provider chunks.
                if (!"[[NEXT]]".startsWith(text.toString().stripLeading()) && !text.toString().contains("[[NEXT]]")) {
                    onText.accept(text.substring(emitted)); emitted = text.length();
                }
            } else if ("response.completed".equals(type)) {
                if (!"completed".equals(event.path("response").path("status").asText()))
                    throw new IOException("Incomplete response");
                completed = true; break;
            } else if (type.equals("error") || type.equals("response.failed") || type.equals("response.incomplete")
                    || type.startsWith("response.refusal")) throw new IOException("Provider failed");
        }
        if (!completed) throw new IOException("Stream ended before completion");
        String result = text.toString().strip();
        if (result.equals("[[NEXT]]")) return new InterviewAiClient.Decision(false, "", "Đã đủ rõ");
        if (result.isBlank() || result.contains("[[NEXT]]") || !result.endsWith("?")
                || result.chars().filter(c -> c == '?').count() != 1) throw new IOException("Invalid question");
        return new InterviewAiClient.Decision(true, result, "Làm rõ câu trả lời trong ngữ cảnh hội thoại");
    }

    private ResponseStatusException failure(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
    @PreDestroy public void close() { watchdog.shutdownNow(); http.close(); }
}
