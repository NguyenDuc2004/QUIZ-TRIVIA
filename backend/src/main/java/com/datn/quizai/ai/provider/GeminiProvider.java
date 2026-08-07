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

    /** Khớp cả {@code "retryDelay": "52s"} lẫn {@code Please retry in 52.03s}. */
    private static final java.util.regex.Pattern RETRY_DELAY_PATTERN =
            java.util.regex.Pattern.compile("retry(?:Delay)?\"?\\s*(?:in|:)?\\s*\"?(\\d+(?:\\.\\d+)?)s",
                    java.util.regex.Pattern.CASE_INSENSITIVE);

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final Duration TIMEOUT = Duration.ofSeconds(90);

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final String embeddingModel;
    private final int embeddingDimensions;

    /**
     * Model embedding và số chiều đều đọc từ cấu hình: Google đã một lần gỡ
     * {@code text-embedding-004} khiến toàn bộ pipeline RAG chết, nên không hardcode nữa.
     * Đổi số chiều thì phải có migration đổi kiểu cột {@code material_chunks.embedding} cho khớp.
     */
    public GeminiProvider(WebClient.Builder builder,
                          @Value("${app.ai.gemini.api-key:}") String apiKey,
                          @Value("${app.ai.gemini.model}") String model,
                          @Value("${app.ai.gemini.embedding-model}") String embeddingModel,
                          @Value("${app.ai.gemini.embedding-dimensions}") int embeddingDimensions) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.embeddingModel = embeddingModel;
        this.embeddingDimensions = embeddingDimensions;
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
    public String embeddingModel() {
        return embeddingModel;
    }

    @Override
    public List<Float> embed(String text) {
        Map<String, Object> body = Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of("parts", List.of(Map.of("text", text))),
                // Model mặc định trả 3072 chiều; xin đúng số chiều của cột trong CSDL
                "outputDimensionality", embeddingDimensions);

        JsonNode response = post("/models/" + embeddingModel + ":embedContent", body);
        JsonNode values = response.path("embedding").path("values");

        if (!values.isArray() || values.isEmpty()) {
            throw new AiProviderException(name(), "Phản hồi embedding không có mảng values", false, null);
        }
        if (values.size() != embeddingDimensions) {
            // Chặn sớm, nếu không PostgreSQL sẽ báo lỗi khó hiểu khi ghi vào cột vector(n)
            throw new AiProviderException(name(),
                    "Embedding trả về " + values.size() + " chiều, cần " + embeddingDimensions, false, null);
        }

        List<Float> vector = new ArrayList<>(values.size());
        values.forEach(value -> vector.add((float) value.asDouble()));
        return normalize(vector);
    }

    /**
     * Chuẩn hoá về vector đơn vị.
     * <p>
     * Google chỉ bảo đảm vector đã chuẩn hoá khi lấy đủ 3072 chiều; cắt bớt chiều thì mất tính
     * chất đó. Cosine distance vốn không quan tâm độ dài nên hiện tại không ảnh hưởng kết quả,
     * nhưng chuẩn hoá sẵn thì sau này đổi sang inner product cũng không phải sửa gì.
     */
    private List<Float> normalize(List<Float> vector) {
        double sumOfSquares = 0;
        for (Float value : vector) {
            sumOfSquares += (double) value * value;
        }
        double norm = Math.sqrt(sumOfSquares);
        if (norm == 0) {
            return vector;
        }
        return vector.stream().map(value -> (float) (value / norm)).toList();
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
            String errorBody = e.getResponseBodyAsString();
            log.warn("Gemini trả {} cho {}: {}", e.getStatusCode(), path, errorBody);
            throw new AiProviderException(name(), "HTTP " + e.getStatusCode(), retryable,
                    retryAfterMillis(errorBody), e);

        } catch (RuntimeException e) {
            // Timeout hoặc lỗi mạng — cũng là tạm thời
            throw new AiProviderException(name(), "Gọi API thất bại: " + e.getMessage(), true, e);
        }
    }

    /**
     * Đọc thời gian chờ Gemini đề nghị khi vượt hạn mức.
     * <p>
     * Nó nằm ở hai chỗ tuỳ loại lỗi: khối {@code RetryInfo.retryDelay} dạng {@code "52s"}, hoặc
     * chỉ nằm trong câu tiếng Anh {@code "Please retry in 52.03s"}. Bắt cả hai vì Gemini không
     * nhất quán, mà thiếu con số này thì backoff mặc định vài giây không bao giờ qua nổi cửa sổ
     * hạn mức tính theo phút.
     *
     * @return mili-giây cần chờ, 0 nếu không tìm thấy
     */
    // Để mức package cho test đọc được: đây là phần dễ vỡ nhất khi Gemini đổi câu chữ thông báo lỗi
    long retryAfterMillis(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return 0;
        }
        java.util.regex.Matcher matcher = RETRY_DELAY_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            return 0;
        }
        try {
            double seconds = Double.parseDouble(matcher.group(1));
            return Math.round(seconds * 1000);
        } catch (NumberFormatException e) {
            return 0;
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
