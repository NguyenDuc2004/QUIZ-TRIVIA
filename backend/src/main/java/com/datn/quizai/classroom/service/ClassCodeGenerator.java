package com.datn.quizai.classroom.service;

import java.security.SecureRandom;

/**
 * Sinh mã lớp 6 ký tự (features/14, FR-54).
 *
 * <h3>Bộ ký tự bỏ 0, O, 1, I, L</h3>
 * Mã này được <b>đọc to trong lớp và chép tay lên bảng</b>, không phải copy-paste. Giữ cả {@code 0} và
 * {@code O} thì một phần lớp gõ nhầm và vào sai chỗ — hoặc không vào được và tưởng mình gõ sai. Bỏ năm ký tự
 * dễ nhầm làm không gian mã giảm từ 36⁶ ≈ 2,2 tỉ xuống 31⁶ ≈ 887 triệu, vẫn thừa xa nhu cầu.
 *
 * <h3>Vì sao {@link SecureRandom}</h3>
 * Không phải vì mã lớp là bí mật — nó được đọc to giữa lớp. Mà vì {@code Random} thường có thể đoán được
 * chuỗi tiếp theo từ vài giá trị đã thấy, và khi đó ai biết mã lớp mình cũng đoán được mã lớp bên cạnh.
 * Vào nhầm lớp người khác không phải lỗ hổng lớn, nhưng nó là thứ không cần phải để xảy ra.
 */
public final class ClassCodeGenerator {

    /** 31 ký tự: A–Z bỏ I, O, L; và 2–9 bỏ 0, 1. */
    static final String BANG_CHU = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    static final int DO_DAI = 6;

    private static final SecureRandom RANDOM = new SecureRandom();

    private ClassCodeGenerator() {
    }

    public static String sinh() {
        StringBuilder sb = new StringBuilder(DO_DAI);
        for (int i = 0; i < DO_DAI; i++) {
            sb.append(BANG_CHU.charAt(RANDOM.nextInt(BANG_CHU.length())));
        }
        return sb.toString();
    }
}
