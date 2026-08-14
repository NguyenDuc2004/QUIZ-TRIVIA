package com.datn.quizai.admin.dto;

import java.util.List;

/**
 * Tổng hợp chi phí và độ tin cậy của các lời gọi mô hình (features/10, mục 3.6 báo cáo).
 * <p>
 * Ba nhóm số liệu, mỗi nhóm trả lời một câu hỏi quản trị khác nhau:
 * <ul>
 *   <li><b>Chi phí</b> — tiêu bao nhiêu token, ở chức năng nào.</li>
 *   <li><b>Độ tin cậy</b> — tỉ lệ lời gọi thất bại, và bao nhiêu lần phải chuyển sang nhà cung cấp
 *       dự phòng. Tỉ lệ dự phòng cao là dấu hiệu nhà cung cấp chính đang có vấn đề, hoặc hạn mức
 *       đang bị đụng trần thường xuyên.</li>
 *   <li><b>Độ trễ</b> — trung bình và phân vị 95. Trung bình một mình che mất những lần chậm bất
 *       thường, mà đúng những lần đó mới là thứ người dùng nhớ.</li>
 * </ul>
 *
 * @param theoChucNang tách theo `feature` (embedding, generation, grading, chat) — chức năng nào đang
 *                     tiêu nhiều nhất là thông tin cần để quyết định siết chỗ nào
 */
public record AiUsageSummary(
        long tongLuotGoi,
        long luotThanhCong,
        long luotThatBai,
        long luotDungDuPhong,
        long tongTokenVao,
        long tongTokenRa,
        Integer doTreTrungBinhMs,
        Integer doTreP95Ms,
        List<TheoChucNang> theoChucNang,
        List<TheoNhaCungCap> theoNhaCungCap
) {
    public record TheoChucNang(String chucNang, long luotGoi, long tokenVao, long tokenRa,
                               Integer doTreTrungBinhMs) {
    }

    public record TheoNhaCungCap(String nhaCungCap, long luotGoi, long luotThatBai) {
    }
}
