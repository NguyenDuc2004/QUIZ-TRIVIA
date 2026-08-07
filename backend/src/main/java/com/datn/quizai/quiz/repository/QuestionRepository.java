package com.datn.quizai.quiz.repository;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.QuestionType;
import com.datn.quizai.quiz.domain.QuizQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    /** Ngân hàng câu hỏi của chính người dùng, có lọc theo loại/độ khó/chủ đề/từ khóa. */
    @Query(value = """
            select q from Question q
            where q.owner.id = :ownerId
              and (:type is null or q.type = :type)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:topic is null or lower(q.topic) = :topic)
              and (:keyword is null or lower(q.content) like :keyword)
            """,
            countQuery = """
                    select count(q) from Question q
                    where q.owner.id = :ownerId
                      and (:type is null or q.type = :type)
                      and (:difficulty is null or q.difficulty = :difficulty)
                      and (:topic is null or lower(q.topic) = :topic)
                      and (:keyword is null or lower(q.content) like :keyword)
                    """)
    Page<Question> findBank(@Param("ownerId") UUID ownerId,
                            @Param("type") QuestionType type,
                            @Param("difficulty") Difficulty difficulty,
                            @Param("topic") String topic,
                            @Param("keyword") String keyword,
                            Pageable pageable);

    /**
     * Các chủ đề người dùng đã đặt, kèm số câu mỗi chủ đề.
     * <p>
     * Gom theo <b>tên gốc</b> nhưng sắp theo tên viết thường, để "Lịch sử" và "lịch sử" nằm cạnh
     * nhau trong danh sách gợi ý — người dùng nhìn thấy là biết mình từng gõ hai kiểu và tự chọn
     * lại một kiểu. Không tự gộp: đó là dữ liệu của họ, hệ thống không được lặng lẽ sửa.
     */
    @Query("""
            select q.topic as topic, count(q) as questionCount
            from Question q
            where q.owner.id = :ownerId and q.topic is not null and q.topic <> ''
            group by q.topic
            order by lower(q.topic)
            """)
    List<TopicCount> findTopics(@Param("ownerId") UUID ownerId);

    /** Một dòng của danh sách chủ đề. */
    interface TopicCount {
        String getTopic();

        long getQuestionCount();
    }

    @Query("""
            select distinct q from Question q
              join fetch q.owner
              left join fetch q.options
            where q.id = :id
            """)
    Optional<Question> findByIdWithOptions(@Param("id") UUID id);

    @Query("""
            select distinct q from Question q
              join fetch q.owner
              left join fetch q.options
            where q.id in :ids
            """)
    List<Question> findAllByIdWithOptions(@Param("ids") List<UUID> ids);

    /** Số quiz đang dùng câu hỏi này — cảnh báo trước khi xóa. */
    @Query("select count(qq) from QuizQuestion qq where qq.question.id = :questionId")
    long countUsagesInQuizzes(@Param("questionId") UUID questionId);
}
