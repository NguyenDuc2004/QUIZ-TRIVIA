package com.datn.quizai.season.dto;

import com.datn.quizai.season.service.PhanHang;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Bảng xếp hạng mùa hiện tại (features/15, FR-62).
 *
 * @param thuHangCuaToi {@code null} khi người gọi <b>chưa có điểm nào</b> trong mùa — khác hẳn với "hạng
 *                      cuối". Giao diện cần phân biệt để nói "bạn chưa có điểm mùa này" thay vì hiện một con
 *                      số hạng sai
 * @param soNguoiThamGia tổng số người có điểm, để thứ hạng có mẫu số ("hạng 7 / 42")
 */
public record LeaderboardResponse(
        UUID seasonId,
        String tenMua,
        OffsetDateTime batDau,
        OffsetDateTime ketThuc,
        long soNguoiThamGia,
        List<Dong> top,
        Dong thuHangCuaToi
) {
    /**
     * @param phanHang  Đồng / Bạc / Vàng theo VỊ TRÍ TƯƠNG ĐỐI trong mùa (FR-64); {@code null} khi mùa
     *                  chưa đủ người để việc phân hạng có nghĩa. Xem {@code PhanHang} về việc vì sao
     *                  không dùng ngưỡng điểm tuyệt đối
     * @param nhanHang  nhãn tiếng Việt để hiện thẳng; null cùng lúc với {@code phanHang}
     */
    public record Dong(int rank, UUID userId, String displayName, String avatarUrl, int score,
                       PhanHang phanHang, String nhanHang) {

        /** Dựng một dòng kèm hạng tính từ vị trí trong mùa. */
        public static Dong cua(int rank, UUID userId, String displayName, String avatarUrl, int score,
                               long tongSoNguoi) {
            PhanHang hang = PhanHang.cua(rank, tongSoNguoi);
            return new Dong(rank, userId, displayName, avatarUrl, score,
                    hang, hang == null ? null : hang.nhan());
        }
    }
}
