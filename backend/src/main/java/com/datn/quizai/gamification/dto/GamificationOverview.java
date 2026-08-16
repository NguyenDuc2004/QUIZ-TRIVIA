package com.datn.quizai.gamification.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Tổng quan trò chơi hoá của người đang gọi (features/13).
 *
 * @param xpTrongCap     XP đã có trong cấp hiện tại
 * @param xpCanTrongCap  XP cần cho cả cấp hiện tại; {@code 0} khi đã ở cấp tối đa — giao diện phải xử lý
 *                       trường hợp này thay vì chia cho 0
 * @param streakConHomNay hôm nay đã có hoạt động chưa. Cần cờ riêng vì {@code currentStreak} không nói được
 *                        điều đó: chuỗi 5 ngày có thể là "đã học hôm nay" hoặc "học đến hôm qua, hôm nay
 *                        chưa" — hai trạng thái khác nhau hoàn toàn với người dùng
 */
public record GamificationOverview(
        int totalXp,
        int level,
        int xpTrongCap,
        int xpCanTrongCap,
        int currentStreak,
        int longestStreak,
        LocalDate lastActiveDate,
        boolean streakConHomNay,
        int soHuyHieu,
        int tongSoHuyHieu,
        List<BadgeResponse> huyHieuMoiNhat
) {
}
