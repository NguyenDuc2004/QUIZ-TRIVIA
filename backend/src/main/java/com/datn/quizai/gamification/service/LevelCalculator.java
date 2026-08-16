package com.datn.quizai.gamification.service;

/**
 * Quy đổi XP sang cấp độ (features/13, FR-49).
 *
 * <h3>Đọc công thức của đặc tả theo nghĩa nào</h3>
 * Đặc tả gợi ý {@code xp_needed(level) = 100 * level^1.5}. Hiểu nó là <b>XP tích luỹ để đạt cấp n</b> thì
 * sinh ra một chỗ ngược: lên cấp 2 tốn 283 XP, nhưng từ cấp 2 lên cấp 3 chỉ tốn thêm 237 — tức cấp thứ hai
 * <i>khó hơn</i> cấp thứ ba, và người chơi sẽ thấy ngay. Nguyên nhân là cấp 1 phải bằng 0 XP nên bậc đầu bị
 * kéo dài bất thường.
 * <p>
 * Nên ở đây hiểu nó là <b>XP cho từng bậc</b>: đi từ cấp {@code n} lên {@code n+1} tốn
 * {@code 100 * n^1.5}. Cách này giữ đúng ý "ngưỡng lũy tiến" của đặc tả và khoảng cách tăng đều từ bậc đầu:
 * 100 → 283 → 520 → 800…
 * <p>
 * Lớp thuần, không phụ thuộc Spring: đây là phần có phép tính thật nên phải kiểm được bằng unit test chạy
 * trong vài milli-giây thay vì dựng cả cơ sở dữ liệu để xem một con số có đúng hay không.
 */
public final class LevelCalculator {

    /**
     * Chặn trên của cấp độ.
     * <p>
     * Không có chặn thì vòng lặp tìm cấp chạy mãi nếu XP bị đặt sai thành một số khổng lồ, và giao diện
     * hiện "Cấp 4172" — một con số vô nghĩa với người học. Cấp 100 cần khoảng một triệu XP, tức đã xa hơn
     * mọi cách dùng thật của hệ thống.
     */
    public static final int LEVEL_TOI_DA = 100;

    private LevelCalculator() {
    }

    /** XP cần cho <b>một bậc</b>: đi từ {@code level} lên {@code level + 1}. */
    public static int xpChoBac(int level) {
        return (int) Math.round(100 * Math.pow(level, 1.5));
    }

    /**
     * XP tích luỹ cần để <b>đạt</b> một cấp — tổng các bậc trước nó. Cấp 1 là cấp khởi đầu nên cần 0 XP.
     * <p>
     * Cộng dồn trong vòng lặp thay vì tìm công thức đóng: cấp tối đa là 100 nên vòng lặp dài nhất có 99
     * bước, và một công thức đóng gần đúng sẽ làm ngưỡng lệch khỏi {@link #xpChoBac} vài XP — đủ để người
     * dùng thấy thanh tiến độ đầy mà chưa lên cấp.
     */
    public static int xpCanDeDatCap(int level) {
        int tong = 0;
        for (int bac = 1; bac < level; bac++) {
            tong += xpChoBac(bac);
        }
        return tong;
    }

    /** Cấp độ ứng với tổng XP. */
    public static int capTuXp(int totalXp) {
        int level = 1;
        while (level < LEVEL_TOI_DA && totalXp >= xpCanDeDatCap(level + 1)) {
            level++;
        }
        return level;
    }

    /**
     * Tiến độ tới cấp kế tiếp.
     *
     * @param xpTrongCap  XP đã có trong cấp hiện tại
     * @param xpCanTrongCap XP cần cho cả cấp hiện tại; {@code 0} khi đã ở cấp tối đa — giao diện phải xử lý
     *                      trường hợp này thay vì chia cho 0
     */
    public record TienDo(int level, int xpTrongCap, int xpCanTrongCap) {
        /** Phần trăm hoàn thành cấp hiện tại, 100 khi đã ở cấp tối đa. */
        public int phanTram() {
            return xpCanTrongCap == 0 ? 100 : (int) Math.round(100.0 * xpTrongCap / xpCanTrongCap);
        }
    }

    public static TienDo tienDo(int totalXp) {
        int level = capTuXp(totalXp);
        int nen = xpCanDeDatCap(level);
        if (level >= LEVEL_TOI_DA) {
            return new TienDo(level, 0, 0);
        }
        int tran = xpCanDeDatCap(level + 1);
        return new TienDo(level, totalXp - nen, tran - nen);
    }
}
