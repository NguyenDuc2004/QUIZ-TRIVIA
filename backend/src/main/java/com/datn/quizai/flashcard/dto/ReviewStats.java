package com.datn.quizai.flashcard.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Thống kê ôn tập (features/11, FR-42).
 *
 * @param tongSoThe    số thẻ đã có trạng thái ôn của người này
 * @param soDaThuoc    số thẻ có khoảng ôn ≥ 21 ngày. 21 ngày là <b>ngưỡng quy ước</b> của SM-2 cho ghi nhớ
 *                     dài hạn, không phải kết quả đo — nên giao diện phải nói rõ ngưỡng thay vì chỉ ghi
 *                     "đã thuộc", để không ngụ ý một phép đo không tồn tại
 * @param soDenHanHomNay số thẻ đến hạn hôm nay, gồm cả thẻ quá hạn từ những ngày trước
 * @param duBao        khối lượng ôn từng ngày trong 7 ngày tới, <b>gồm cả ngày không có thẻ</b> (giá trị 0)
 */
public record ReviewStats(
        long tongSoThe,
        long soDaThuoc,
        long soDenHanHomNay,
        List<DiemDuBao> duBao
) {
    public record DiemDuBao(LocalDate ngay, long soThe) {
    }
}
