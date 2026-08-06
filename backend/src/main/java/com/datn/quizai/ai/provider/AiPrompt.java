package com.datn.quizai.ai.provider;

/**
 * Một yêu cầu gửi tới mô hình ngôn ngữ, độc lập với provider.
 *
 * @param systemInstruction chỉ dẫn vai trò/ràng buộc, tách khỏi nội dung người dùng để
 *                          giảm nguy cơ prompt injection (docs/security.md §3)
 * @param userPrompt        nội dung yêu cầu
 * @param jsonOutput        true = ép mô hình trả JSON thuần, không kèm lời dẫn
 * @param temperature       0 = bám sát ngữ cảnh, cao hơn = sáng tạo hơn
 */
public record AiPrompt(String systemInstruction, String userPrompt, boolean jsonOutput, double temperature) {

    public static AiPrompt json(String systemInstruction, String userPrompt) {
        return new AiPrompt(systemInstruction, userPrompt, true, 0.4);
    }
}
