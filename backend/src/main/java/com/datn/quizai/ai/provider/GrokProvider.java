package com.datn.quizai.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider dự phòng — xAI Grok (docs/architecture.md §5).
 * <p>
 * API của xAI tương thích OpenAI nên thân request khác hẳn Gemini; đó chính là lý do phải có
 * {@link AiProvider}: nghiệp vụ không cần biết hai bên khác nhau chỗ nào.
 * <p>
 * xAI hiện <b>không có API embedding</b>, nên provider này chỉ phục vụ sinh văn bản. Khi Gemini
 * chết, sinh đề vẫn chạy được nhưng nạp học liệu mới thì không.
 */
@Component
public class GrokProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GrokProvider.class);

    private static final String BASE_URL = "https://api.x.ai/v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(90);

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GrokProvider(WebClient.Builder builder,
                        @Value("${app.ai.grok.api-key:}") String apiKey,
                        @Value("${app.ai.grok.model}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.webClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String name() {
        return "grok";
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
        if (!isConfigured()) {
            throw new AiProviderException(name(), "Chưa cấu hình GROK_API_KEY", false, null);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", prompt.temperature());
        body.put("messages", List.of(
                Map.of("role", "system", "content", prompt.systemInstruction()),
                Map.of("role", "user", "content", prompt.userPrompt())));
        if (prompt.jsonOutput()) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        long startedAt = System.currentTimeMillis();
        JsonNode response = post(body);
        long latency = System.currentTimeMillis() - startedAt;

        JsonNode message = response.path("choices").path(0).path("message");
        if (message.path("content").isMissingNode()) {
            throw new AiProviderException(name(), "Phản hồi không có nội dung", false, null);
        }

        JsonNode usage = response.path("usage");
        return new AiCompletion(name(), model, message.get("content").asText(),
                usage.path("prompt_tokens").isMissingNode() ? null : usage.get("prompt_tokens").asInt(),
                usage.path("completion_tokens").isMissingNode() ? null : usage.get("completion_tokens").asInt(),
                latency);
    }

    private JsonNode post(Map<String, Object> body) {
        try {
            return webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(TIMEOUT);

        } catch (WebClientResponseException e) {
            boolean retryable = e.getStatusCode().is5xxServerError() || e.getStatusCode().value() == 429;
            log.warn("Grok trả {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiProviderException(name(), "HTTP " + e.getStatusCode(), retryable, e);

        } catch (RuntimeException e) {
            throw new AiProviderException(name(), "Gọi API thất bại: " + e.getMessage(), true, e);
        }
    }

    /** Chặn rõ ràng thay vì để lỗi lạ ở tầng dưới — xAI không có endpoint embedding. */
    @Override
    public List<Float> embed(String text) {
        throw new AiProviderException(name(), "xAI không có API embedding", false, null);
    }
}
