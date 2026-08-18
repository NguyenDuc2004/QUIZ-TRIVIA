package com.datn.quizai.season.dto;

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
    public record Dong(int rank, UUID userId, String displayName, String avatarUrl, int score) {
    }
}
