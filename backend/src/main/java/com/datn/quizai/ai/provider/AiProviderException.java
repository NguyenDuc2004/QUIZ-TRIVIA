package com.datn.quizai.ai.provider;

/**
 * Lỗi khi gọi một provider cụ thể.
 *
 * @see #isRetryable() quyết định có nên chuyển sang provider dự phòng hay không
 */
public class AiProviderException extends RuntimeException {

    private final String provider;
    private final boolean retryable;
    private final long retryAfterMillis;

    public AiProviderException(String provider, String message, boolean retryable, Throwable cause) {
        this(provider, message, retryable, 0, cause);
    }

    public AiProviderException(String provider, String message, boolean retryable,
                               long retryAfterMillis, Throwable cause) {
        super("[" + provider + "] " + message, cause);
        this.provider = provider;
        this.retryable = retryable;
        this.retryAfterMillis = retryAfterMillis;
    }

    public String getProvider() {
        return provider;
    }

    /**
     * Lỗi tạm thời (429 quá hạn mức, 5xx, timeout) thì đáng thử provider khác.
     * Lỗi do mình gửi sai (4xx còn lại) thì thử provider khác cũng hỏng y vậy.
     */
    public boolean isRetryable() {
        return retryable;
    }

    /**
     * Provider bảo chờ bao lâu rồi hãy gọi lại; 0 nghĩa là không nói gì.
     * <p>
     * Quan trọng với hạn mức theo phút: Gemini bản miễn phí cho 5 lượt/phút và khi vượt thì trả kèm
     * "retry in 52s". Backoff mặc định vài giây không cứu được — chờ theo đúng con số nó đưa mới
     * qua được, còn không thì mọi câu sau câu thứ năm đều hỏng.
     */
    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
