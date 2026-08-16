package com.datn.quizai.flashcard.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Phát ra sau khi một người ôn xong một thẻ (features/11 → dùng bởi features/13).
 * <p>
 * Có {@code ngay} trong sự kiện chứ không để bên nhận tự gọi {@code LocalDate.now()}: khoá chống cộng XP
 * trùng của gamification là {@code cardId:ngày}, và nếu hai bên tự lấy ngày riêng thì một lượt ôn lúc gần
 * nửa đêm có thể được tính sang hai ngày khác nhau.
 */
public record FlashcardReviewedEvent(UUID userId, UUID flashcardId, LocalDate ngay) {
}
