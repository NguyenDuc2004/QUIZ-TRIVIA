package com.datn.quizai.flashcard.domain;

/**
 * Mức nhớ người học tự đánh giá sau khi xem đáp án (features/11, FR-41).
 * <p>
 * Bốn lựa chọn thay vì thang 0–5 trần trụi của SM-2: người học không đánh giá nổi khác biệt giữa "2" và
 * "3", nên bắt họ chọn số chỉ tạo ra dữ liệu nhiễu. Bốn nhãn có nghĩa rồi mới quy về điểm cho thuật toán.
 * <p>
 * Ranh giới nằm giữa {@link #HARD} (2) và {@link #GOOD} (3): dưới 3 là "chưa nhớ" và thẻ quay lại ngày
 * mai, từ 3 trở lên là "có nhớ" và lịch được giãn ra.
 */
public enum ReviewQuality {
    /** Không nhớ gì — ôn lại ngày mai. */
    AGAIN(0),
    /** Nhớ ra nhưng phải cố — vẫn tính là chưa nhớ, ôn lại ngày mai. */
    HARD(2),
    /** Nhớ được, hơi chậm. */
    GOOD(3),
    /** Nhớ ngay, không phải nghĩ. */
    EASY(5);

    private final int diem;

    ReviewQuality(int diem) {
        this.diem = diem;
    }

    /** Điểm q ∈ {0..5} đưa vào công thức SM-2. */
    public int diem() {
        return diem;
    }
}
