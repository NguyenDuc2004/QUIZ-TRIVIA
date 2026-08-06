package com.datn.quizai.ai.provider;

/**
 * Kết quả một lần gọi mô hình.
 *
 * @param provider  tên provider thực sự đã phục vụ — quan trọng khi có fallback
 * @param tokensIn  số token đầu vào (null nếu provider không trả về)
 * @param tokensOut số token sinh ra
 * @param latencyMs độ trễ đo ở phía mình
 */
public record AiCompletion(String provider, String model, String text,
                           Integer tokensIn, Integer tokensOut, long latencyMs) {
}
