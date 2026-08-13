package com.datn.quizai.ai.provider;

import com.datn.quizai.ai.service.AiRequestLogger;
import com.datn.quizai.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    /**
     * Số lần thử tối đa cho tác vụ nền — <b>đọc từ cấu hình</b>, không phải hằng số cứng.
     * <p>
     * Cần cấu hình được vì con số này quyết định mức tiêu thụ hạn mức: một lời gọi hỏng với 4 lần
     * thử đốt 4 lượt trong tổng hạn mức ngày. Khi <i>đo</i> độ chính xác AI thì phải đặt về 1 —
     * không thì chính phép đo đốt hết hạn mức nó cần để chạy. Chuyện này đã xảy ra một lần.
     */
    private final int maxAttemptsBackground;

    /** Giãn cách giữa các lần thử, tăng dần để không dồn thêm tải lên provider đang quá tải. */
    private static final long BASE_BACKOFF_MILLIS = 1200;

    /**
     * Chờ tối đa bao lâu cho MỘT lần thử lại, khi có người đang ngồi đợi phản hồi.
     * <p>
     * Provider có thể bảo "chờ 52 giây" (hạn mức tính theo phút của Gemini bản miễn phí). Với
     * request đồng bộ thì chờ ngần ấy chẳng khác gì treo — thà báo lỗi để người dùng bấm lại.
     */
    private static final long INTERACTIVE_WAIT_CAP_MILLIS = 5_000;

    /**
     * Chờ tối đa cho tác vụ nền, nơi không ai ngồi đợi.
     * <p>
     * Đủ dài để vượt qua cửa sổ hạn mức theo phút. Không có mức này thì chấm một bài nhiều câu tự
     * luận sẽ hỏng từ câu thứ sáu trở đi trên gói miễn phí — backoff vài giây không cứu được.
     */
    private static final long BACKGROUND_WAIT_CAP_MILLIS = 75_000;

    /** Thứ tự ưu tiên, đã lọc theo cấu hình. */
    private final List<AiProvider> orderedProviders;
    private final AiRequestLogger requestLogger;
    private final AiThrottleState throttleState;

    public AiOrchestrator(List<AiProvider> providers,
                          AiRequestLogger requestLogger,
                          AiThrottleState throttleState,
                          @Value("${app.ai.provider-order}") String providerOrder,
                          @Value("${app.ai.max-attempts-background:4}") int maxAttemptsBackground) {
        this.requestLogger = requestLogger;
        this.throttleState = throttleState;
        this.maxAttemptsBackground = maxAttemptsBackground;

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
        return complete(prompt, feature, userId, false);
    }

    /**
     * @param background true khi gọi từ tác vụ nền — chấp nhận chờ lâu theo đúng thời gian provider
     *                   đề nghị thay vì bỏ cuộc sau vài giây. Với request đồng bộ luôn để false:
     *                   chờ gần một phút thì người dùng tưởng hệ thống treo.
     */
    public AiCompletion complete(AiPrompt prompt, String feature, UUID userId, boolean background) {
        return callWithFallback(feature, userId, provider -> provider.complete(prompt), provider -> true,
                background ? BACKGROUND_WAIT_CAP_MILLIS : INTERACTIVE_WAIT_CAP_MILLIS);
    }

    /**
     * Sinh văn bản theo luồng token, cho trợ lý học tập (features/08).
     * <p>
     * <b>Fallback chỉ hoạt động TRƯỚC mảnh đầu tiên.</b> Đây là giới hạn thật, không phải thiếu sót:
     * một khi người dùng đã thấy chữ hiện ra, chuyển sang provider khác sẽ nối tiếp câu trả lời của
     * mô hình A bằng câu trả lời của mô hình B — hai mạch văn khác nhau ghép vào nhau thành một đoạn
     * vô nghĩa, mà người đọc không có cách nào biết chỗ nối ở đâu. Thà dừng và báo lỗi.
     * <p>
     * Cũng vì vậy mà ở đây <b>không thử lại chính provider đó</b> như {@link #complete}: thử lại nghĩa
     * là sinh lại từ đầu, và nếu đã phát chữ rồi thì không rút lại được.
     */
    public Flux<String> stream(AiPrompt prompt, String feature, UUID userId) {
        List<AiProvider> candidates = orderedProviders.stream()
                .filter(AiProvider::isConfigured)
                .filter(AiProvider::supportsStreaming)
                .toList();

        if (candidates.isEmpty()) {
            return Flux.error(new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình provider AI nào hỗ trợ streaming."));
        }
        return streamFrom(candidates, 0, prompt, feature, userId);
    }

    private Flux<String> streamFrom(List<AiProvider> candidates, int index,
                                    AiPrompt prompt, String feature, UUID userId) {
        AiProvider provider = candidates.get(index);
        long startedAt = System.currentTimeMillis();
        StringBuilder answer = new StringBuilder();

        return provider.stream(prompt)
                .doOnNext(answer::append)
                .doOnComplete(() -> {
                    requestLogger.logSuccess(userId, feature, new AiCompletion(
                            provider.name(), provider.model(), answer.toString(),
                            null, null, System.currentTimeMillis() - startedAt));
                    throttleState.clear();
                })
                .onErrorResume(error -> {
                    String message = error.getMessage();
                    requestLogger.logFailure(userId, feature, provider, message);

                    // Đã phát chữ ra rồi thì hết đường lui — xem javadoc của stream()
                    if (!answer.isEmpty()) {
                        log.warn("{} lỗi GIỮA luồng sau {} ký tự, không chuyển provider được: {}",
                                provider.name(), answer.length(), message);
                        return Flux.error(error);
                    }

                    boolean retryable = error instanceof AiProviderException e && e.isRetryable();
                    boolean hasNext = index + 1 < candidates.size();
                    if (retryable && hasNext) {
                        log.warn("{} lỗi trước mảnh đầu ({}), chuyển sang {}",
                                provider.name(), message, candidates.get(index + 1).name());
                        return streamFrom(candidates, index + 1, prompt, feature, userId);
                    }

                    rememberThrottle(error);
                    return Flux.error(giveUpStreaming(error));
                });
    }

    /**
     * Ghi lại thời điểm hết hạn mức để giao diện nói được "chờ N giây" thay vì chỉ "lỗi".
     * <p>
     * Dùng chung cờ với luồng {@link #complete}: hạn mức là của cả tài khoản Google, không phải của
     * riêng từng tính năng.
     */
    private void rememberThrottle(Throwable error) {
        if (error instanceof AiProviderException e && e.getRetryAfterMillis() > 0) {
            throttleState.markThrottled(e.getRetryAfterMillis());
        }
    }

    private Throwable giveUpStreaming(Throwable error) {
        if (error instanceof AiProviderException e && e.getRetryAfterMillis() > 0) {
            long seconds = Math.max(1, e.getRetryAfterMillis() / 1000);
            return new BusinessException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Đã hết hạn mức gọi AI. Thử lại sau " + seconds + " giây.");
        }
        return new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "Trợ lý AI đang không phản hồi. Thử lại sau ít phút.");
    }

    /**
     * Sinh vector embedding. Chỉ xét provider có hỗ trợ embedding — hiện chỉ Gemini,
     * nên thực tế đây là lời gọi không có đường lui.
     */
    public List<Float> embed(String text, UUID userId) {
        return embed(text, userId, false);
    }

    /**
     * @param background true khi gọi từ tác vụ nền — nạp học liệu sinh hàng chục embedding liên
     *                   tiếp, chắc chắn đụng hạn mức theo phút, mà không ai ngồi đợi nên chờ được.
     */
    public List<Float> embed(String text, UUID userId, boolean background) {
        AiCompletion completion = callWithFallback("embedding", userId,
                provider -> {
                    long startedAt = System.currentTimeMillis();
                    List<Float> vector = provider.embed(text);
                    // Ghi embeddingModel() chứ không phải model(): audit phải nói đúng
                    // model nào tạo ra vector, nếu không số liệu mục 3.6 báo cáo sẽ sai
                    return new AiCompletion(provider.name(), provider.embeddingModel(),
                            serialize(vector), null, null, System.currentTimeMillis() - startedAt);
                },
                AiProvider::supportsEmbedding,
                background ? BACKGROUND_WAIT_CAP_MILLIS : INTERACTIVE_WAIT_CAP_MILLIS);

        return deserialize(completion.text());
    }

    // ------------------------------------------------------------------ nội bộ

    private AiCompletion callWithFallback(String feature, UUID userId,
                                          java.util.function.Function<AiProvider, AiCompletion> call,
                                          java.util.function.Predicate<AiProvider> filter) {
        return callWithFallback(feature, userId, call, filter, INTERACTIVE_WAIT_CAP_MILLIS);
    }

    private AiCompletion callWithFallback(String feature, UUID userId,
                                          java.util.function.Function<AiProvider, AiCompletion> call,
                                          java.util.function.Predicate<AiProvider> filter,
                                          long waitCapMillis) {

        List<AiProvider> candidates = orderedProviders.stream()
                .filter(AiProvider::isConfigured)
                .filter(filter)
                .toList();

        if (candidates.isEmpty()) {
            throw new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình API key cho dịch vụ AI. Thêm GEMINI_API_KEY vào .env rồi khởi động lại backend.");
        }

        List<String> failures = new ArrayList<>();
        AiProviderException lastFailure = null;

        for (int i = 0; i < candidates.size(); i++) {
            AiProvider provider = candidates.get(i);

            // Thử lại CHÍNH provider này trước khi chuyển sang provider khác: 503 "model
            // overloaded" và 429 của nhà cung cấp thường hết sau vài giây, mà chuyển provider
            // thì kết quả sinh ra khác chất lượng. Chỉ khi provider này hết cơ hội mới đi tiếp.
            int maxAttempts = waitCapMillis >= BACKGROUND_WAIT_CAP_MILLIS
                    ? maxAttemptsBackground : MAX_ATTEMPTS_PER_PROVIDER;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    AiCompletion completion = call.apply(provider);
                    requestLogger.logSuccess(userId, feature, completion);
                    // Gọi được rồi thì gỡ cờ ngay, đừng để giao diện còn hiện "đang chờ hạn mức"
                    throttleState.clear();
                    if (i > 0 || attempt > 1) {
                        log.info("{} thành công ở lần thử {} (provider thứ {}) cho {}",
                                provider.name(), attempt, i + 1, feature);
                    }
                    return completion;

                } catch (AiProviderException e) {
                    requestLogger.logFailure(userId, feature, provider, e.getMessage());
                    failures.add(e.getMessage());
                    lastFailure = e;

                    long wait = waitBeforeRetry(e, attempt, waitCapMillis);
                    boolean canRetrySameProvider = e.isRetryable()
                            && attempt < maxAttempts
                            && wait > 0;
                    if (canRetrySameProvider) {
                        log.warn("{} lỗi tạm thời ({}), thử lại sau {}ms",
                                provider.name(), e.getMessage(), wait);
                        // Chỉ đánh dấu khi CHÍNH provider nói phải chờ bao lâu: đó mới là hết hạn
                        // mức. Backoff tự nghĩ cho lỗi mạng vài giây thì không đáng báo ra ngoài.
                        throttleState.markThrottled(e.getRetryAfterMillis());
                        sleep(wait);
                        continue;
                    }

                    boolean isLastProvider = i == candidates.size() - 1;
                    if (!e.isRetryable() || isLastProvider) {
                        // Lỗi do mình gửi sai, hoặc đã hết cả provider lẫn lần thử → dừng luôn
                        throw giveUp(e);
                    }
                    log.warn("{} vẫn lỗi sau {} lần thử, chuyển sang provider tiếp theo",
                            provider.name(), attempt);
                    break;
                }
            }
        }

        throw lastFailure != null ? giveUp(lastFailure)
                : new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "Dịch vụ AI đang không phản hồi: " + String.join(" | ", failures));
    }

    /**
     * Đổi lỗi provider thành lỗi trả về cho client.
     * <p>
     * Tách riêng <b>hết hạn mức</b> khỏi "dịch vụ hỏng": hai chuyện này người dùng phải xử lý khác
     * nhau. Hết hạn mức thì chờ một phút là chạy lại được, mà báo 503 "không phản hồi" thì họ tưởng
     * hệ thống lỗi và bỏ luôn. Có con số provider đưa ra thì nói thẳng phải chờ bao lâu.
     */
    private BusinessException giveUp(AiProviderException e) {
        long retryAfter = e.getRetryAfterMillis();
        throttleState.markThrottled(retryAfter);
        if (retryAfter > 0) {
            long seconds = Math.max(1, Math.round(retryAfter / 1000.0));
            return new BusinessException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Dịch vụ AI đang quá tải hạn mức. Vui lòng thử lại sau khoảng " + seconds + " giây.");
        }
        return new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                "Dịch vụ AI đang không phản hồi, vui lòng thử lại sau");
    }

    /**
     * Chờ bao lâu trước khi thử lại.
     * <p>
     * Ưu tiên con số <b>chính provider đưa ra</b> — với hạn mức tính theo phút, backoff tự nghĩ
     * (1,2s rồi 2,4s) không bao giờ đủ, thử lại sớm chỉ tốn thêm một lượt gọi và lại 429.
     *
     * @return 0 nghĩa là chờ lâu hơn mức cho phép, đừng thử lại nữa mà chuyển provider hoặc bỏ cuộc
     */
    private long waitBeforeRetry(AiProviderException e, int attempt, long waitCapMillis) {
        long suggested = e.getRetryAfterMillis();
        if (suggested <= 0) {
            return Math.min(backoffMillis(attempt), waitCapMillis);
        }
        // Cộng thêm một chút: chờ đúng sát nút thường vẫn dính 429 vì đồng hồ hai bên lệch nhau
        long wait = suggested + 500;
        return wait <= waitCapMillis ? wait : 0;
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
