package com.datn.quizai.quiz.domain;

import com.datn.quizai.common.BaseEntity;
import com.datn.quizai.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Câu hỏi trong ngân hàng câu hỏi — bảng `questions`.
 * Câu hỏi thuộc về người tạo và tái sử dụng được ở nhiều quiz (FR-8).
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
public class Question extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** Giải thích đáp án, hiện sau khi nộp bài. */
    @Column(columnDefinition = "text")
    private String explanation;

    /**
     * Ảnh minh hoạ của câu hỏi (features/02, FR-11); null = câu hỏi chỉ có chữ.
     * <p>
     * Chỉ nhận đường dẫn nội bộ {@code /uploads/...} do {@code POST /files/images} sinh ra — xem
     * {@code UploadedImagePath} về lý do không nhận URL ngoài.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Tiêu chí chấm câu tự luận (features/06). Không có rubric thì mô hình tự nghĩ ra thang điểm
     * của riêng nó và hai lần chấm cùng một bài có thể lệch nhau — rubric là thứ neo điểm lại.
     */
    @Column(columnDefinition = "text")
    private String rubric;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Column(length = 100)
    private String topic;

    @Column(nullable = false)
    private Integer points = 1;

    /** Thời gian tối đa cho riêng câu này (giây); null = theo cấu hình quiz. */
    @Column(name = "time_limit_sec")
    private Integer timeLimitSec;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionSource source = QuestionSource.MANUAL;

    /** Với câu do AI sinh: provider, model, prompt_hash… (features/05). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_metadata")
    private String aiMetadata;

    /** {@code @BatchSize}: nạp lựa chọn của nhiều câu hỏi trong ít truy vấn, tránh N+1. */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @BatchSize(size = 50)
    private List<QuestionOption> options = new ArrayList<>();

    public Question(User owner, QuestionType type, String content) {
        this.owner = owner;
        this.type = type;
        this.content = content;
    }

    /** Gắn lựa chọn/đáp án và giữ hai chiều quan hệ nhất quán. */
    public void addOption(QuestionOption option) {
        option.setQuestion(this);
        option.setOrderIndex(options.size());
        options.add(option);
    }

    public void clearOptions() {
        options.clear();
    }
}
