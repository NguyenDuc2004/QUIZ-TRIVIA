package com.datn.quizai.ai.provider;

/**
 * Lỗi khi gọi một provider cụ thể.
 *
 * @see #isRetryable() quyết định có nên chuyển sang provider dự phòng hay không
 */
public class AiProviderException extends RuntimeException {

    private final String provider;
    private final boolean retryable;

    public AiProviderException(String provider, String message, boolean retryable, Throwable cause) {
        super("[" + provider + "] " + message, cause);
        this.provider = provider;
        this.retryable = retryable;
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
}
