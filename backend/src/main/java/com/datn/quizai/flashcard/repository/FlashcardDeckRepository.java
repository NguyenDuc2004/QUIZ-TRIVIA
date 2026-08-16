package com.datn.quizai.flashcard.repository;

import com.datn.quizai.flashcard.domain.FlashcardDeck;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FlashcardDeckRepository extends JpaRepository<FlashcardDeck, UUID> {

    /**
     * Bộ thẻ của một người, mới nhất trước.
     * <p>
     * {@code join fetch owner} để không sinh N+1 khi map sang DTO — cùng lý do như `QuizRepository`.
     */
    @Query(value = """
            select d from FlashcardDeck d
              join fetch d.owner
            where d.owner.id = :ownerId
              and (:keyword is null or lower(d.title) like :keyword)
            """,
            countQuery = """
                    select count(d) from FlashcardDeck d
                    where d.owner.id = :ownerId
                      and (:keyword is null or lower(d.title) like :keyword)
                    """)
    Page<FlashcardDeck> findOwnedDecks(@Param("ownerId") UUID ownerId,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    @Query("select d from FlashcardDeck d join fetch d.owner where d.id = :id")
    Optional<FlashcardDeck> findByIdWithOwner(@Param("id") UUID id);
}
