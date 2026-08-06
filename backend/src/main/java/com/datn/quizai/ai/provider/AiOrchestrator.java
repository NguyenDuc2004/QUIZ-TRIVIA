package com.datn.quizai.ai.provider;

import com.datn.quizai.ai.service.AiRequestLogger;
import com.datn.quizai.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cửa duy nhất để nghiệp vụ gọi mô hình ngôn ngữ (docs/architecture.md §5).
 * <p>
 * Trách nhiệm:
 * <ul>
 *   <li><b>Fallback theo thứ tự cấu hình</b> {@code app.ai.provider-order} — Gemini hỏng thì
 *       chuyển sang Grok, nhưng chỉ với lỗi <i>tạm thời</i>: prompt sai định dạng thì gửi sang
 *       provider khác cũng hỏng y vậy, thử lại chỉ tốn tiền.</li>
 *   <li><b>Bỏ qua provider chưa cấu hình key</b> thay vì gọi rồi nhận lỗi.</li>
 *   <li><b>Ghi audit</b> mọi lần gọi: provider nào phục vụ, bao nhiêu token, mất bao lâu.</li>
 * </ul>
 * <p>
 * Không có provider nào chạy được thì ném lỗi 503 kèm thông điệp người dùng hiểu được,
 * chứ không để lộ chi tiết lỗi của bên thứ ba.
 */
@Service
public class AiOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestrator.class);

    /**
     * Số lần thử tối đa với <b>cùng một</b> provider khi gặp lỗi tạm thời.
     * Để 3 vì 503/429 của nhà cung cấp thường hết sau vài giây; cao hơn thì người dùng chờ quá lâu.
     */
    private static final int MAX_ATTEMPTS_PER_PROVIDER = 3;
    /** Giãn cách giữa các lần thử, tăng dần để không dồn thêm tải lên provider đang quá tải. */
    private static final long BASE_BACKOFF_MILLIS = 1200;

    /** Thứ tự ưu tiên, đã lọc theo cấu hình. */
    private final List<AiProvider> orderedProviders;
    private final AiRequestLogger requestLogger;

    public AiOrchestrator(List<AiProvider> providers,
                          AiRequestLogger requestLogger,
                          @Value("${app.ai.provider-order}") String providerOrder) {
        this.requestLogger = requestLogger;

        Map<String, AiProvider> byName = new LinkedHashMap<>();
        providers.forEach(provider -> byName.put(provider.name(), provider));

        this.orderedProviders = Arrays.stream(providerOrder.split(","))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .map(byName::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        log.info("Thứ tự provider AI: {}", orderedProviders.stream().map(AiProvider::name).toList());
    }

    /** Có ít nhất một provider đã cấu hình key hay chưa — dùng để báo lỗi sớm, rõ ràng. */
    public boolean isAvailable() {
        return orderedProviders.stream().anyMatch(AiProvider::isConfigured);
    }

    /** Tên các provider đang dùng được, hiển thị ở trang cấu hình cho Admin. */
    public List<String> availableProviders() {
        return orderedProviders.stream().filter(AiProvider::isConfigured).map(AiProvider::name).toList();
    }

    /**
     * Sinh văn bản, tự chuyển provider khi gặp lỗi tạm thời.
     *
     * @param feature tên tính năng để ghi audit (generation / grading / chat…)
     * @param userId  người yêu cầu, null nếu là tác vụ hệ thống
     */
    public AiCompletion complete(AiPrompt prompt, String feature, UUID userId) {
        return callWithFallback(feature, userId, provider -> provider.complete(prompt), provider -> true);
    }

    /**
     * Sinh vector embedding. Chỉ xét provider có hỗ trợ embedding — hiện chỉ Gemini,
     * nên thực tế đây là lời gọi không có đường lui.
     */
    public List<Float> embed(String text, UUID userId) {
        AiCompletion completion = callWithFallback("embedding", userId,
                provider -> {
                    long startedAt = System.currentTimeMillis();
                    List<Float> vector = provider.embed(text);
                    // Ghi embeddingModel() chứ không phải model(): audit phải nói đúng
                    // model nào tạo ra vector, nếu không số liệu mục 3.6 báo cáo sẽ sai
                    return new AiCompletion(provider.name(), provider.embeddingModel(),
                            serialize(vector), null, null, System.currentTimeMillis() - startedAt);
                },
                AiProvider::supportsEmbedding);

        return deserialize(completion.text());
    }

    // ------------------------------------------------------------------ nội bộ

    private AiCompletion callWithFallback(String feature, UUID userId,
                                          java.util.function.Function<AiProvider, AiCompletion> call,
                                          java.util.function.Predicate<AiProvider> filter) {

        List<AiProvider> candidates = orderedProviders.stream()
                .filter(AiProvider::isConfigured)
                .filter(filter)
                .toList();

        if (candidates.isEmpty()) {
            throw new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình API key cho dịch vụ AI. Thêm GEMINI_API_KEY vào .env rồi khởi động lại backend.");
        }

        List<String> failures = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            AiProvider provider = candidates.get(i);

            // Thử lại CHÍNH provider này trước khi chuyển sang provider khác: 503 "model
            // overloaded" và 429 của nhà cung cấp thường hết sau vài giây, mà chuyển provider
            // thì kết quả sinh ra khác chất lượng. Chỉ khi provider này hết cơ hội mới đi tiếp.
            for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_PROVIDER; attempt++) {
                try {
                    AiCompletion completion = call.apply(provider);
                    requestLogger.logSuccess(userId, feature, completion);
                    if (i > 0 || attempt > 1) {
                        log.info("{} thành công ở lần thử {} (provider thứ {}) cho {}",
                                provider.name(), attempt, i + 1, feature);
                    }
                    return completion;

                } catch (AiProviderException e) {
                    requestLogger.logFailure(userId, feature, provider, e.getMessage());
                    failures.add(e.getMessage());

                    boolean canRetrySameProvider = e.isRetryable() && attempt < MAX_ATTEMPTS_PER_PROVIDER;
                    if (canRetrySameProvider) {
                        log.warn("{} lỗi tạm thời ({}), thử lại sau {}ms",
                                provider.name(), e.getMessage(), backoffMillis(attempt));
                        sleep(backoffMillis(attempt));
                        continue;
                    }

                    boolean isLastProvider = i == candidates.size() - 1;
                    if (!e.isRetryable() || isLastProvider) {
                        // Lỗi do mình gửi sai, hoặc đã hết cả provider lẫn lần thử → dừng luôn
                        throw new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                                "Dịch vụ AI đang không phản hồi, vui lòng thử lại sau");
                    }
                    log.warn("{} vẫn lỗi sau {} lần thử, chuyển sang provider tiếp theo",
                            provider.name(), attempt);
                    break;
                }
            }
        }

        throw new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "Dịch vụ AI đang không phản hồi: " + String.join(" | ", failures));
    }

    /** Backoff tăng dần: 1,2s rồi 2,4s. */
    private long backoffMillis(int attempt) {
        return BASE_BACKOFF_MILLIS * attempt;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bị ngắt khi chờ thử lại lời gọi AI", e);
        }
    }

    /** Nhét vector vào {@code AiCompletion.text} để dùng chung một đường fallback + audit. */
    private String serialize(List<Float> vector) {
        StringBuilder sb = new StringBuilder(vector.size() * 8);
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector.get(i));
        }
        return sb.toString();
    }

    private List<Float> deserialize(String text) {
        String[] parts = text.split(",");
        List<Float> vector = new ArrayList<>(parts.length);
        for (String part : parts) {
            vector.add(Float.parseFloat(part));
        }
        return vector;
    }
}
