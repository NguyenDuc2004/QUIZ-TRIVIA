package com.datn.quizai.quiz.repository;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.quiz.domain.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    /**
     * Danh sách quiz công khai — dùng cho Guest và mọi người dùng.
     * {@code join fetch} owner/category để không sinh N+1 khi map sang DTO.
     */
    @Query(value = """
            select q from Quiz q
              join fetch q.owner
              left join fetch q.category c
            where q.visibility = com.datn.quizai.quiz.domain.Visibility.PUBLIC
              and (:categoryId is null or c.id = :categoryId)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:keyword is null or lower(q.title) like :keyword)
            """,
            countQuery = """
                    select count(q) from Quiz q
                    where q.visibility = com.datn.quizai.quiz.domain.Visibility.PUBLIC
                      and (:categoryId is null or q.category.id = :categoryId)
                      and (:difficulty is null or q.difficulty = :difficulty)
                      and (:keyword is null or lower(q.title) like :keyword)
                    """)
    Page<Quiz> findPublicQuizzes(@Param("categoryId") UUID categoryId,
                                 @Param("difficulty") Difficulty difficulty,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    /** Quiz của chính người dùng, gồm cả quiz riêng tư. */
    @Query(value = """
            select q from Quiz q
              join fetch q.owner o
              left join fetch q.category c
            where o.id = :ownerId
              and (:categoryId is null or c.id = :categoryId)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:keyword is null or lower(q.title) like :keyword)
            """,
            countQuery = """
                    select count(q) from Quiz q
                    where q.owner.id = :ownerId
                      and (:categoryId is null or q.category.id = :categoryId)
                      and (:difficulty is null or q.difficulty = :difficulty)
                      and (:keyword is null or lower(q.title) like :keyword)
                    """)
    Page<Quiz> findOwnedQuizzes(@Param("ownerId") UUID ownerId,
                                @Param("categoryId") UUID categoryId,
                                @Param("difficulty") Difficulty difficulty,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    @Query("""
            select q from Quiz q
              join fetch q.owner
              left join fetch q.category
            where q.id = :id
            """)
    Optional<Quiz> findByIdWithOwner(@Param("id") UUID id);

    /**
     * Nạp sẵn câu hỏi để trả chi tiết quiz cho chủ sở hữu.
     * <p>
     * Chỉ fetch một collection ({@code quizQuestions}); lựa chọn của câu hỏi
     * ({@code Question.options}) để Hibernate nạp theo lô nhờ {@code @BatchSize} —
     * fetch hai collection kiểu List trong cùng câu truy vấn sẽ lỗi MultipleBagFetchException.
     */
    @Query("""
            select distinct q from Quiz q
              join fetch q.owner
              left join fetch q.quizQuestions qq
              left join fetch qq.question
            where q.id = :id
            """)
    Optional<Quiz> findByIdWithQuestions(@Param("id") UUID id);
}
