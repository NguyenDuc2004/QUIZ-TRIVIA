package com.datn.quizai.ai.provider;

import com.datn.quizai.ai.AiJson;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider dự phòng — <b>Groq</b> (docs/architecture.md §5).
 *
 * <h3>Vì sao thay xAI Grok bằng Groq</h3>
 * Không phải vì Grok kém, mà vì <b>xAI không có gói miễn phí</b>: suốt cả dự án, đường dự phòng chưa một lần
 * chạy thật được, và mục 3.6 của báo cáo phải ghi *"chưa demo được fallback vì chưa có key"*. Một đường dự
 * phòng chưa từng chạy thì không ai biết nó có chạy hay không — nó là một lời hứa, không phải một tính năng.
 * Groq có gói miễn phí nên lần đầu tiên đo được cả chuỗi Gemini → dự phòng bằng số liệu thật.
 * <p>
 * <b>Groq ≠ Grok.</b> Groq (groq.com) là nhà cung cấp hạ tầng suy luận chạy mô hình mở (Llama…) trên phần
 * cứng riêng; Grok là mô hình của xAI. Hai chữ khác nhau đúng một ký tự nên mọi chỗ nhắc tới đều viết rõ.
 *
 * <h3>Ba điểm khác Gemini mà tầng nghiệp vụ không cần biết</h3>
 * API của Groq tương thích OpenAI nên thân request khác hẳn Gemini — đó chính là lý do {@link AiProvider}
 * tồn tại. Ngoài ra:
 * <ul>
 *   <li><b>Có streaming</b> — khác {@code GrokProvider} cũ. Nhờ vậy trợ lý học tập (features/08) lần đầu có
 *       đường lui thật: Gemini chết thì chữ vẫn chảy, thay vì cả tính năng tắt.</li>
 *   <li><b>Không có API embedding.</b> Giống xAI trước đây: Gemini chết thì sinh đề vẫn chạy nhưng
 *       <i>nạp học liệu mới</i> thì không.</li>
 *   <li><b>Hạn mức trả trong header</b> {@code retry-after} khi 429 — cùng ý nghĩa với phần
 *       {@code retryAfterMillis} của Gemini, nên orchestrator chờ đúng chứ không đoán.</li>
 * </ul>
 */
@Component
public class GroqProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GroqProvider.class);

    private static final String BASE_URL = "https://api.groq.com/openai/v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(90);

    /** Groq báo hết mảnh bằng đúng chuỗi này, theo chuẩn SSE của OpenAI. */
    private static final String SSE_KET_THUC = "[DONE]";

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public GroqProvider(WebClient.Builder builder,
                        @Value("${app.ai.groq.api-key:}") String apiKey,
                        @Value("${app.ai.groq.model}") String model) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.webClient = builder.baseUrl(BASE_URL).build();
    }

    @Override
    public String name() {
        return "groq";
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
            throw new AiProviderException(name(), "Chưa cấu hình GROQ_API_KEY", false, null);
        }

        long startedAt = System.currentTimeMillis();
        JsonNode response = post(thanRequest(prompt, false));
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

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    /**
     * Streaming theo chuẩn SSE của OpenAI: mỗi dòng {@code data:} là một mảnh JSON, dòng cuối là
     * {@code [DONE]}.
     * <p>
     * {@code bodyToFlux(String.class)} của Spring đã bóc sẵn tiền tố {@code data: }, nên ở đây chỉ còn phần
     * JSON. Mảnh nào không mang chữ thì <b>bỏ qua</b> chứ không ném lỗi — mảnh đầu chỉ có {@code role}, mảnh
     * cuối chỉ có {@code finish_reason} và {@code usage}, cả hai đều hợp lệ mà rỗng chữ.
     */
    @Override
    public Flux<String> stream(AiPrompt prompt) {
        if (!isConfigured()) {
            return Flux.error(new AiProviderException(name(), "Chưa cấu hình GROQ_API_KEY", false, null));
        }

        return webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(thanRequest(prompt, true))
                .retrieve()
                .bodyToFlux(String.class)
                .takeUntil(SSE_KET_THUC::equals)
                .mapNotNull(this::bocManh)
                .onErrorMap(this::toProviderException);
    }

    /** @return phần chữ của một mảnh, hoặc null nếu mảnh đó không mang chữ nào */
    String bocManh(String chunk) {
        if (SSE_KET_THUC.equals(chunk)) {
            return null;
        }
        try {
            String text = AiJson.readTree(chunk)
                    .path("choices").path(0)
                    .path("delta").path("content")
                    .asText("");
            return text.isEmpty() ? null : text;

        } catch (Exception e) {
            // Bỏ mảnh hỏng chứ không kéo đổ cả luồng: người dùng thà thiếu vài chữ hơn mất cả câu trả lời
            // đang chạy dở. Cùng cách xử lý với GeminiProvider.
            log.warn("Bỏ qua mảnh SSE không đọc được từ Groq: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Object> thanRequest(AiPrompt prompt, boolean streaming) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", prompt.temperature());
        body.put("messages", List.of(
                Map.of("role", "system", "content", prompt.systemInstruction()),
                Map.of("role", "user", "content", prompt.userPrompt())));
        if (streaming) {
            body.put("stream", true);
        }
        if (prompt.jsonOutput()) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
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
            throw toProviderException(e);

        } catch (RuntimeException e) {
            throw new AiProviderException(name(), "Gọi API thất bại: " + e.getMessage(), true, e);
        }
    }

    private AiProviderException toProviderException(Throwable error) {
        if (error instanceof AiProviderException provider) {
            return provider;
        }
        if (error instanceof WebClientResponseException e) {
            boolean retryable = e.getStatusCode().is5xxServerError() || e.getStatusCode().value() == 429;
            log.warn("Groq trả {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return new AiProviderException(name(), "HTTP " + e.getStatusCode(), retryable,
                    choBaoLau(e), e);
        }
        return new AiProviderException(name(), "Lỗi mạng: " + error.getMessage(), true, null);
    }

    /**
     * Groq nói thẳng phải chờ bao lâu ở header {@code retry-after} (đơn vị giây).
     * <p>
     * Đọc nó thay vì để orchestrator đoán: đoán ngắn thì đâm vào hạn mức lần nữa, đoán dài thì bắt người
     * dùng chờ vô ích. {@code 0} = không nói, orchestrator dùng khoảng lùi mặc định của nó.
     */
    private long choBaoLau(WebClientResponseException e) {
        return docRetryAfter(e.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
    }

    /**
     * Tách khỏi {@link #choBaoLau} để test được mà không phải dựng một {@code WebClientResponseException}.
     * <p>
     * Groq trả số <b>giây</b>, có thể là số thập phân ({@code "2.5"}). Đọc nhầm sang milli-giây thì hệ thống
     * chờ 2 mili-giây rồi gọi lại — đâm thẳng vào hạn mức lần nữa, và vòng lặp đó nhìn giống "Groq luôn lỗi".
     */
    static long docRetryAfter(String header) {
        if (header == null || header.isBlank()) {
            return 0;
        }
        try {
            double giay = Double.parseDouble(header.trim());
            return giay <= 0 ? 0 : (long) (giay * 1000);
        } catch (NumberFormatException ex) {
            // Chuẩn HTTP cho phép retry-after là một MỐC THỜI GIAN dạng chữ. Groq không dùng dạng đó, nhưng
            // nếu có thì trả 0 để orchestrator dùng khoảng lùi mặc định, chứ không đổ.
            return 0;
        }
    }

    /** Chặn rõ ràng thay vì để lỗi lạ ở tầng dưới — Groq không có endpoint embedding. */
    @Override
    public List<Float> embed(String text) {
        throw new AiProviderException(name(), "Groq không có API embedding", false, null);
    }
}
