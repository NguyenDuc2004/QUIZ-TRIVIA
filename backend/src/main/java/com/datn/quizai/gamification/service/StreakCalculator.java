package com.datn.quizai.gamification.service;

import java.time.LocalDate;

/**
 * Tính chuỗi ngày học liên tiếp (features/13, FR-51).
 * <p>
 * Quy tắc theo đặc tả: so {@code lastActiveDate} với hôm nay — cùng ngày thì giữ nguyên, hôm qua thì +1,
 * xa hơn thì reset về 1.
 * <p>
 * Chỗ dễ làm sai nhất là <b>reset về 1, không phải về 0</b>: hôm nay người ta vừa học, nên chuỗi hiện tại
 * đúng là một ngày. Reset về 0 làm màn hình hiện "chuỗi 0 ngày" ngay sau khi người dùng vừa làm xong một bài.
 * <p>
 * Lớp thuần để kiểm được mọi mốc thời gian mà không cần đổi giờ hệ thống.
 */
public final class StreakCalculator {

    private StreakCalculator() {
    }

    /**
     * @param currentStreak chuỗi mới
     * @param longestStreak chuỗi dài nhất từng đạt
     * @param laNgayMoi     hôm nay có phải lần hoạt động đầu tiên trong ngày. Dùng để biết có nên tính tiến
     *                      độ thử thách ngày và trao XP "ngày mới" hay không — hoạt động thứ hai trong cùng
     *                      ngày không làm streak nhảy thêm
     */
    public record KetQua(int currentStreak, int longestStreak, boolean laNgayMoi) {
    }

    /**
     * @param lastActive ngày hoạt động gần nhất; {@code null} nghĩa là chưa từng hoạt động
     * @param homNay     ngày hôm nay, truyền vào chứ không gọi {@code LocalDate.now()} bên trong để test
     *                   dựng được mọi mốc thời gian
     */
    public static KetQua capNhat(LocalDate lastActive, int currentStreak, int longestStreak,
                                LocalDate homNay) {
        if (lastActive == null) {
            return new KetQua(1, Math.max(longestStreak, 1), true);
        }
        if (lastActive.equals(homNay)) {
            // Hoạt động thứ hai trong cùng ngày: giữ nguyên mọi thứ. Không cộng thêm, cũng không reset.
            return new KetQua(currentStreak, longestStreak, false);
        }
        if (lastActive.plusDays(1).equals(homNay)) {
            int moi = currentStreak + 1;
            return new KetQua(moi, Math.max(longestStreak, moi), true);
        }
        if (lastActive.isAfter(homNay)) {
            // Ngày hoạt động nằm ở tương lai: dữ liệu đã sai từ đâu đó (đổi múi giờ, sửa tay). Giữ nguyên
            // chuỗi thay vì reset — người dùng không đáng mất chuỗi vì một dòng dữ liệu lỗi.
            return new KetQua(currentStreak, longestStreak, false);
        }
        // Bỏ lỡ ít nhất một ngày: chuỗi mới bắt đầu từ hôm nay, tức 1 — KHÔNG phải 0
        return new KetQua(1, Math.max(longestStreak, 1), true);
    }
}
