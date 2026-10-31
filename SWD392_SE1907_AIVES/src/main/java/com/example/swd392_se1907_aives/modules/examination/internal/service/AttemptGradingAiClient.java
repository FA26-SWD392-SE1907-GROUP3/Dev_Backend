package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class AttemptGradingAiClient {
    private final String instructions = loadInstructions();

    private static String loadInstructions() {
        try {
            return new org.springframework.core.io.ClassPathResource("prompts/attempt-grading.txt")
                    .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException error) { throw new IllegalStateException("Missing grading prompt", error); }
    }
    private final ObjectMapper json;
    private final String key;
    private final String model;
    private final URI endpoint;
    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public AttemptGradingAiClient(
            ObjectMapper json,
            @Value("${app.ai.api-key:}") String key,
            @Value("${app.ai.grading-model:gpt-4.1-mini}") String model,
            @Value("${app.interview.openai-base-url:https://api.openai.com/v1}") String baseUrl) {
        this.json = json;
        this.key = key;
        this.model = model;
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/responses");
    }

    public JsonNode grade(String evidence) {
        if (key.isBlank())
            throw new IllegalStateException("Chưa cấu hình AI chấm bài trên máy chủ.");
        try {
            var criterion =
                    Map.of(
                            "type",
                            "object",
                            "properties",
                            Map.of(
                                    "sortOrder",
                                    Map.of("type", "integer"),
                                    "score",
                                    Map.of("type", "number"),
                                    "feedback",
                                    Map.of("type", "string")),
                            "required",
                            List.of("sortOrder", "score", "feedback"),
                            "additionalProperties",
                            false);
            var schema =
                    Map.of(
                            "type",
                            "object",
                            "properties",
                            Map.of(
                                    "feedback",
                                    Map.of("type", "string"),
                                    "criteria",
                                    Map.of("type", "array", "items", criterion)),
                            "required",
                            List.of("feedback", "criteria"),
                            "additionalProperties",
                            false);
            var body =
                    Map.of(
                            "model",
                            model,
                            "store",
                            false,
                            "max_output_tokens",
                            6000,
                            "instructions",
                            instructions,
                            "input",
                            evidence,
                            "text",
                            Map.of(
                                    "format",
                                    Map.of(
                                            "type",
                                            "json_schema",
                                            "name",
                                            "whole_exam_grade",
                                            "strict",
                                            true,
                                            "schema",
                                            schema)));
            var request =
                    HttpRequest.newBuilder(endpoint)
                            .timeout(Duration.ofSeconds(90))
                            .header("Authorization", "Bearer " + key)
                            .header("Content-Type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            json.writeValueAsString(body)))
                            .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                throw new IllegalStateException("AI chưa chấm được bài. Vui lòng thử lại.");
            JsonNode result = json.readTree(response.body());
            if (!"completed".equals(result.path("status").asText()))
                throw new IllegalStateException("AI chưa hoàn tất chấm bài.");
            StringBuilder text = new StringBuilder();
            for (var output : result.path("output"))
                for (var part : output.path("content")) {
                    if ("refusal".equals(part.path("type").asText()))
                        throw new IllegalStateException("AI chưa thể chấm nội dung bài thi này.");
                    if ("output_text".equals(part.path("type").asText()))
                        text.append(part.path("text").asText());
                }
            return json.readTree(text.toString());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chấm bài bị gián đoạn.");
        } catch (IllegalStateException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("Không nhận được kết quả chấm bài hợp lệ từ AI.");
        }
    }
}
