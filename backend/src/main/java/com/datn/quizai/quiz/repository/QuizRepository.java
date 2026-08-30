package com.datn.quizai.quiz.repository;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.quiz.domain.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    /**
     * Tiêu đề + ảnh bìa của nhiều quiz trong <b>một</b> truy vấn.
     * <p>
     * Dùng cho những chỗ đã biết id từ nguồn khác (gợi ý lấy id từ Neo4j) và chỉ cần đủ dữ liệu để
     * vẽ thẻ. Nạp cả thực thể {@code Quiz} ở đây là kéo theo owner, category và các collection —
     * tốn kém mà không dùng tới; còn hỏi từng id một thì thành N+1.
     * <p>
     * Id không còn trong bảng thì <b>không có dòng tương ứng</b>: chỗ gọi dựa vào đó để loại quiz
     * đã xoá ra khỏi danh sách.
     */
    @Query("select q.id as id, q.title as title, q.thumbnailUrl as thumbnailUrl, "
            + "c.name as categoryName "
            + "from Quiz q left join q.category c where q.id in :ids")
    List<QuizCardRow> findCardsByIds(@Param("ids") Collection<UUID> ids);

    /**
     * Số quiz công khai <b>có câu hỏi</b>.
     * <p>
     * Dùng để trả lời "kho có gì để gợi ý hay không". Quiz công khai mà 0 câu hỏi thì không tính:
     * gợi ý nó ra thì người dùng bấm vào cũng không làm được gì.
     */
    @Query("select count(distinct q.id) from Quiz q join q.quizQuestions qq "
            + "where q.visibility = com.datn.quizai.quiz.domain.Visibility.PUBLIC")
    long countPublicQuizzesWithQuestions();

    /** Đủ để vẽ một thẻ quiz, không hơn. */
    interface QuizCardRow {
        UUID getId();

        String getTitle();

        String getThumbnailUrl();

        /**
         * Tên danh mục — giao diện dùng nó để chọn màu và biểu tượng của khối bìa khi quiz chưa có ảnh.
         * <p>
         * Cần ở đây chứ không suy ở phía giao diện: cùng một quiz phải ra <b>cùng một màu</b> ở lưới Khám
         * phá và ở thẻ Gợi ý. Thiếu trường này thì thẻ gợi ý phải đoán màu từ tiêu đề, và người dùng thấy
         * hai thẻ khác màu mà không nhận ra đó là một quiz.
         * <p>
         * {@code null} khi quiz chưa phân loại — {@code left join} chứ không phải {@code join}.
         */
        String getCategoryName();
    }
}
