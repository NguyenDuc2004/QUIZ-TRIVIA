package com.datn.quizai.flashcard.repository;

import com.datn.quizai.flashcard.domain.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlashcardRepository extends JpaRepository<Flashcard, UUID> {

    List<Flashcard> findByDeckIdOrderByCreatedAt(UUID deckId);

    long countByDeckId(UUID deckId);

    /**
     * Số thẻ của nhiều bộ trong <b>một</b> truy vấn — dùng cho danh sách bộ thẻ.
     * <p>
     * Bộ rỗng không xuất hiện trong kết quả (không có dòng nào để gộp), nên tầng gọi phải mặc định 0
     * thay vì coi thiếu khoá là lỗi.
     */
    @Query("""
            select c.deck.id as deckId, count(c) as soThe
            from Flashcard c
            where c.deck.id in :deckIds
            group by c.deck.id
            """)
    List<SoTheRow> demTheoBo(@Param("deckIds") List<UUID> deckIds);

    interface SoTheRow {
        UUID getDeckId();

        long getSoThe();
    }

    /** Nạp kèm bộ thẻ và chủ bộ để kiểm quyền mà không phải đi thêm hai lượt truy vấn. */
    @Query("select c from Flashcard c join fetch c.deck d join fetch d.owner where c.id = :id")
    Optional<Flashcard> findByIdWithDeckOwner(@Param("id") UUID id);

    /**
     * Đã có thẻ nào trong bộ này sinh từ câu hỏi đó chưa.
     * <p>
     * Dùng trước khi sinh thẻ từ câu trả lời sai. Chỉ mục một phần
     * {@code uk_flashcards_deck_question} đã chặn ở tầng cơ sở dữ liệu, nhưng kiểm trước ở đây để trả
     * lời được "đã bỏ qua bao nhiêu thẻ trùng" thay vì để một ràng buộc ném ngoại lệ giữa vòng lặp.
     */
    boolean existsByDeckIdAndQuestionId(UUID deckId, UUID questionId);
}
