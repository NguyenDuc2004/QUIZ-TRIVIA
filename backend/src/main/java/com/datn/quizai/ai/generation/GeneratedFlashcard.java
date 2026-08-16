package com.datn.quizai.ai.generation;

/**
 * Một thẻ ghi nhớ do AI sinh, <b>chưa lưu</b> (features/11, FR-38).
 * <p>
 * Là thẻ nháp: người dùng duyệt rồi mới thành thẻ thật trong bộ. Không lưu thẳng vì mô hình có thể trả về
 * thẻ đúng định dạng nhưng sai nội dung, và người duy nhất biết được điều đó là người đọc tài liệu gốc.
 */
public record GeneratedFlashcard(String front, String back, String hint) {
}
