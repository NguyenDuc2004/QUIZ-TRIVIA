package com.datn.quizai.quiz.dto;

/**
 * Một chủ đề trong ngân hàng câu hỏi của người dùng.
 * <p>
 * Chủ đề <b>không phải một bảng riêng</b> — nó là giá trị cột {@code questions.topic}, gom lại khi
 * cần. Cách này không bắt người soạn phải tạo chủ đề trước rồi mới viết được câu hỏi đầu tiên, và
 * dữ liệu cũ dùng được ngay không phải chuyển đổi.
 *
 * @param questionCount số câu đang mang chủ đề này; hiện kèm trong danh sách chọn để người dùng
 *                      thấy ngay chủ đề nào đủ câu để dựng một quiz
 */
public record TopicResponse(String topic, long questionCount) {
}
