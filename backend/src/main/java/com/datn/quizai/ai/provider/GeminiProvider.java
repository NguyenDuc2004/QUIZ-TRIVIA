package com.datn.quizai.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Provider chính — Google Gemini (docs/tech-stack.md §2).
 * <p>
 * Gọi REST bằng {@code WebClient} tự viết, <b>không</b> dùng Spring AI hay LangChain4j: đề tài
 * yêu cầu tự hiện thực lớp tích hợp, và tự viết thì kiểm soát được đúng những gì gửi đi.
 * <p>
 * API key đi trong header {@code x-goog-api-key} chứ không phải query string — query string
 * bị ghi vào log truy cập của proxy và lịch sử trình duyệt.
 */
@Component
public class GeminiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    /** 768 chiều, khớp cột `material_chunks.embedding vector(768)`. */
    private static final String EMBEDDING_MODEL = "text-embedding-004";
    private static final Duration TIMEOUT = Duration.ofSeconds(90);

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GeminiProvider(WebClient.Builder builder,
                          @Value("${app.ai.gemini.api-key:}") String apiKey,
                          @Value("${app.ai.gemini.model}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.webClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public AiCompletion complete(AiPrompt prompt) {
        Map<String, Object> generationConfig = prompt.jsonOutput()
                ? Map.of("temperature", prompt.temperature(), "responseMimeType", "application/json")
                : Map.of("temperature", prompt.temperature());

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", prompt.systemInstruction()))),
                "contents", List.of(Map.of("role", "user",
                        "parts", List.of(Map.of("text", prompt.userPrompt())))),
                "generationConfig", generationConfig);

        long startedAt = System.currentTimeMillis();
        JsonNode response = post("/models/" + model + ":generateContent", body);
        long latency = System.currentTimeMillis() - startedAt;

        String text = extractText(response);
        JsonNode usage = response.path("usageMetadata");

        return new AiCompletion(name(), model, text,
                usage.path("promptTokenCount").isMissingNode() ? null : usage.get("promptTokenCount").asInt(),
                usage.path("candidatesTokenCount").isMissingNode() ? null : usage.get("candidatesTokenCount").asInt(),
                latency);
    }

    @Override
    public boolean supportsEmbedding() {
        return true;
    }

    @Override
    public List<Float> embed(String text) {
        Map<String, Object> body = Map.of(
                "model", "models/" + EMBEDDING_MODEL,
                "content", Map.of("parts", List.of(Map.of("text", text))));

        JsonNode response = post("/models/" + EMBEDDING_MODEL + ":embedContent", body);
        JsonNode values = response.path("embedding").path("values");

        if (!values.isArray() || values.isEmpty()) {
            throw new AiProviderException(name(), "Phản hồi embedding không có mảng values", false, null);
        }

        List<Float> vector = new ArrayList<>(values.size());
        values.forEach(value -> vector.add((float) value.asDouble()));
        return vector;
    }

    private JsonNode post(String path, Map<String, Object> body) {
        if (!isConfigured()) {
            throw new AiProviderException(name(), "Chưa cấu hình GEMINI_API_KEY", false, null);
        }
        try {
            return webClient.post()
                    .uri(path)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(TIMEOUT);

        } catch (WebClientResponseException e) {
            // 429 (hết hạn mức) và 5xx là lỗi tạm thời → đáng chuyển sang provider dự phòng
            boolean retryable = e.getStatusCode().is5xxServerError() || e.getStatusCode().value() == 429;
            log.warn("Gemini trả {} cho {}: {}", e.getStatusCode(), path, e.getResponseBodyAsString());
            throw new AiProviderException(name(), "HTTP " + e.getStatusCode(), retryable, e);

        } catch (RuntimeException e) {
            // Timeout hoặc lỗi mạng — cũng là tạm thời
            throw new AiProviderException(name(), "Gọi API thất bại: " + e.getMessage(), true, e);
        }
    }

    /** Gemini trả nội dung ở {@code candidates[0].content.parts[*].text}. */
    private String extractText(JsonNode response) {
        JsonNode parts = response.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            String reason = response.path("candidates").path(0).path("finishReason").asText("");
            throw new AiProviderException(name(),
                    "Phản hồi rỗng" + (reason.isBlank() ? "" : " (finishReason=" + reason + ")"), false, null);
        }

        StringBuilder text = new StringBuilder();
        parts.forEach(part -> text.append(part.path("text").asText("")));
        return text.toString();
    }
}
