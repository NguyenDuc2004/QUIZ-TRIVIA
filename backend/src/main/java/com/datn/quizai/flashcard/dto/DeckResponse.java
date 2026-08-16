package com.datn.quizai.flashcard.dto;

import com.datn.quizai.flashcard.domain.FlashcardDeck;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một bộ thẻ nhìn từ danh sách (features/11).
 *
 * @param soThe    tổng số thẻ trong bộ
 * @param soDenHan số thẻ đến hạn ôn hôm nay <b>của người đang gọi</b>. Đây là con số duy nhất trả lời
 *                 được câu hỏi người học thật sự có ("hôm nay tôi phải ôn gì"), nên nó nằm ngay trong
 *                 danh sách bộ thẻ thay vì bắt họ mở từng bộ ra xem
 */
public record DeckResponse(
        UUID id,
        String title,
        String description,
        String topic,
        long soThe,
        long soDenHan,
        OffsetDateTime createdAt
) {
    public static DeckResponse from(FlashcardDeck deck, long soThe, long soDenHan) {
        return new DeckResponse(deck.getId(), deck.getTitle(), deck.getDescription(), deck.getTopic(),
                soThe, soDenHan, deck.getCreatedAt());
    }
}
