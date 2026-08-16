package com.datn.quizai.flashcard.dto;

import com.datn.quizai.flashcard.domain.Flashcard;
import com.datn.quizai.flashcard.domain.FlashcardReview;
import com.datn.quizai.flashcard.domain.FlashcardSource;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Một thẻ, kèm trạng thái ôn của người đang gọi (features/11).
 *
 * @param dueDate      {@code null} khi người này <b>chưa từng ôn</b> thẻ — khác hẳn với "đến hạn hôm nay".
 *                     Giao diện cần phân biệt hai trạng thái đó để không hiện một ngày không tồn tại
 * @param intervalDays khoảng ôn hiện tại; 0 với thẻ chưa ôn lần nào
 */
public record FlashcardResponse(
        UUID id,
        UUID deckId,
        String front,
        String back,
        String hint,
        FlashcardSource source,
        LocalDate dueDate,
        Integer intervalDays,
        Integer totalReviews
) {
    public static FlashcardResponse from(Flashcard card, FlashcardReview review) {
        return new FlashcardResponse(
                card.getId(),
                card.getDeck().getId(),
                card.getFront(),
                card.getBack(),
                card.getHint(),
                card.getSource(),
                review == null ? null : review.getDueDate(),
                review == null ? null : review.getIntervalDays(),
                review == null ? null : review.getTotalReviews());
    }
}
