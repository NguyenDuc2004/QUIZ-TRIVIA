package com.datn.quizai.gamification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Hai lớp tính toán thuần của gamification (features/13, FR-49 và FR-51). */
class LevelAndStreakTest {

    @Nested
    @DisplayName("Cấp độ theo XP")
    class Cap {

        @Test
        @DisplayName("Cấp 1 từ 0 XP, và ngưỡng tăng dần chứ không tuyến tính")
        void levelsGrowProgressively() {
            assertThat(LevelCalculator.capTuXp(0)).isEqualTo(1);
            assertThat(LevelCalculator.xpCanDeDatCap(1)).isZero();

            // 100 * n^1.5 — khoảng cách giữa các cấp phải NỚI RA, nếu không thì cấp cao mất ý nghĩa
            int khoangTruoc = 0;
            for (int level = 2; level <= 10; level++) {
                int khoang = LevelCalculator.xpCanDeDatCap(level) - LevelCalculator.xpCanDeDatCap(level - 1);
                assertThat(khoang).as("khoảng lên cấp %d phải lớn hơn cấp trước", level)
                        .isGreaterThan(khoangTruoc);
                khoangTruoc = khoang;
            }
        }

        @Test
        @DisplayName("XP đúng ngưỡng thì lên cấp; thiếu 1 XP thì chưa")
        void thresholdIsInclusive() {
            int nguong = LevelCalculator.xpCanDeDatCap(5);
            assertThat(LevelCalculator.capTuXp(nguong)).isEqualTo(5);
            assertThat(LevelCalculator.capTuXp(nguong - 1)).isEqualTo(4);
        }

        @Test
        @DisplayName("XP khổng lồ vẫn dừng ở cấp tối đa, không lặp vô hạn")
        void capsAtMaxLevel() {
            // Không có chặn thì vòng lặp tìm cấp chạy mãi khi XP bị đặt sai, và giao diện hiện một con số
            // vô nghĩa với người học
            assertThat(LevelCalculator.capTuXp(Integer.MAX_VALUE))
                    .isEqualTo(LevelCalculator.LEVEL_TOI_DA);
        }

        @Test
        @DisplayName("Tiến độ trong cấp: 0% ngay khi vừa lên cấp, và không chia cho 0 ở cấp tối đa")
        void progressWithinLevel() {
            var vuaLenCap = LevelCalculator.tienDo(LevelCalculator.xpCanDeDatCap(3));
            assertThat(vuaLenCap.level()).isEqualTo(3);
            assertThat(vuaLenCap.xpTrongCap()).isZero();
            assertThat(vuaLenCap.phanTram()).isZero();

            // Ở cấp tối đa không còn cấp kế tiếp — nếu trả xpCanTrongCap = 0 mà phanTram() vẫn chia thì vỡ
            var toiDa = LevelCalculator.tienDo(Integer.MAX_VALUE);
            assertThat(toiDa.xpCanTrongCap()).isZero();
            assertThat(toiDa.phanTram()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Chuỗi ngày học")
    class Streak {

        private static final LocalDate HOM_NAY = LocalDate.of(2026, 8, 16);

        @Test
        @DisplayName("Lần đầu hoạt động: chuỗi = 1")
        void firstEverActivity() {
            var kq = StreakCalculator.capNhat(null, 0, 0, HOM_NAY);
            assertThat(kq.currentStreak()).isEqualTo(1);
            assertThat(kq.longestStreak()).isEqualTo(1);
            assertThat(kq.laNgayMoi()).isTrue();
        }

        @Test
        @DisplayName("Hoạt động thứ hai trong cùng ngày KHÔNG làm chuỗi nhảy thêm")
        void sameDayDoesNotIncrement() {
            var kq = StreakCalculator.capNhat(HOM_NAY, 5, 9, HOM_NAY);

            // Không chặn thì làm 10 bài trong một ngày thành chuỗi 10 ngày — con số mất hết ý nghĩa
            assertThat(kq.currentStreak()).isEqualTo(5);
            assertThat(kq.longestStreak()).isEqualTo(9);
            assertThat(kq.laNgayMoi()).isFalse();
        }

        @Test
        @DisplayName("Học hôm qua rồi học hôm nay: chuỗi +1, và cập nhật chuỗi dài nhất")
        void consecutiveDayIncrements() {
            var kq = StreakCalculator.capNhat(HOM_NAY.minusDays(1), 6, 6, HOM_NAY);
            assertThat(kq.currentStreak()).isEqualTo(7);
            assertThat(kq.longestStreak()).isEqualTo(7);
        }

        @Test
        @DisplayName("Bỏ lỡ ngày: chuỗi reset về 1, KHÔNG phải 0, và chuỗi dài nhất được giữ")
        void gapResetsToOne() {
            var kq = StreakCalculator.capNhat(HOM_NAY.minusDays(3), 12, 12, HOM_NAY);

            // Đây là chỗ dễ sai nhất: hôm nay người ta VỪA học, nên chuỗi hiện tại đúng là một ngày.
            // Reset về 0 làm màn hình hiện "chuỗi 0 ngày" ngay sau khi họ vừa làm xong một bài.
            assertThat(kq.currentStreak()).isEqualTo(1);
            assertThat(kq.longestStreak()).as("thành tích cũ không bị mất").isEqualTo(12);
            assertThat(kq.laNgayMoi()).isTrue();
        }

        @Test
        @DisplayName("Ngày hoạt động ở tương lai: giữ nguyên chuỗi, không reset")
        void futureDateKeepsStreak() {
            // Dữ liệu sai (đổi múi giờ, sửa tay) không đáng làm người dùng mất chuỗi
            var kq = StreakCalculator.capNhat(HOM_NAY.plusDays(2), 8, 8, HOM_NAY);
            assertThat(kq.currentStreak()).isEqualTo(8);
            assertThat(kq.laNgayMoi()).isFalse();
        }

        @Test
        @DisplayName("Ba mươi ngày liên tiếp cho ra chuỗi 30")
        void thirtyDayRun() {
            int current = 0;
            int longest = 0;
            LocalDate lastActive = null;
            for (int i = 0; i < 30; i++) {
                LocalDate ngay = HOM_NAY.minusDays(29L - i);
                var kq = StreakCalculator.capNhat(lastActive, current, longest, ngay);
                current = kq.currentStreak();
                longest = kq.longestStreak();
                lastActive = ngay;
            }
            assertThat(current).isEqualTo(30);
            assertThat(longest).isEqualTo(30);
        }
    }
}
