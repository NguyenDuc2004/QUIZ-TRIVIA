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
import org.hibernate.annotations.Formula;

import java.util.ArrayList;
import java.util.List;

/** Bài quiz — bảng `quizzes` (docs/database.md §1.2). */
@Entity
@Table(name = "quizzes")
@Getter
@Setter
@NoArgsConstructor
public class Quiz extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "is_ai_generated", nullable = false)
    private boolean aiGenerated = false;

    /** Đường dẫn ảnh bìa do server sinh ({@code /uploads/images/…}); null = giao diện tự vẽ khối màu. */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /** Thời gian làm bài toàn quiz (giây); null = không giới hạn. */
    @Column(name = "time_limit_sec")
    private Integer timeLimitSec;

    /**
     * Chế độ thi nghiêm ngặt (features/12, FR-48): yêu cầu toàn màn hình và khoá chuột phải.
     * <p>
     * <b>Chỉ có nghĩa với lượt {@code EXAM}</b> — lượt luyện tập bỏ qua hoàn toàn, giống như tín hiệu chống
     * gian lận cũng chỉ thu ở lượt EXAM. Ràng buộc này chốt ở tầng service vì {@code mode} nằm ở bảng khác.
     * <p>
     * Là <b>rào cản ma sát</b> chứ không phải khoá: trình duyệt không cho ép toàn màn hình và người dùng
     * luôn bấm Esc thoát được. Giá trị thật là làm việc rời bài thi trở nên <i>có chủ ý</i> và để lại tín
     * hiệu {@code FULLSCREEN_EXIT}.
     */
    @Column(name = "strict_exam", nullable = false)
    private boolean strictExam = false;

    /** Danh sách câu hỏi kèm thứ tự trong quiz này. */
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizQuestion> quizQuestions = new ArrayList<>();

    /**
     * Số câu hỏi, tính ngay trong câu SELECT của quiz nên danh sách nhiều quiz
     * không sinh N+1 query. Chỉ đọc.
     */
    @Formula("(select count(*) from quiz_questions qq where qq.quiz_id = id)")
    private int questionCount;

    /**
     * Số NGƯỜI đã làm xong quiz này — hiện dưới mỗi thẻ quiz để người học chọn được giữa hai chục quiz
     * cùng chủ đề.
     *
     * <h4>Đếm người, không đếm lượt</h4>
     * {@code count(distinct user_id)}, không phải {@code count(*)}. Một người luyện tập 50 lần sẽ làm quiz
     * trông như có 50 người quan tâm — con số đó vừa sai vừa dễ thổi phồng. Nhãn trên giao diện cũng phải
     * là "N người đã làm", không phải "N lượt".
     *
     * <h4>Chỉ tính bài đã xong</h4>
     * {@code status <> 'IN_PROGRESS'}. Bấm vào rồi thoát ngay không phải là "đã làm quiz này". {@code EXPIRED}
     * vẫn tính: hết giờ thì bài vẫn được chấm trên phần đã trả lời, người ta đã làm thật.
     *
     * <h4>Vì sao @Formula chứ không phải một cột đếm sẵn</h4>
     * Cột đếm sẵn cần ai đó cập nhật mỗi lần có người nộp bài — thêm một chỗ có thể lệch với sự thật, để
     * đổi lấy tốc độ mà trang danh sách chưa cần. {@code @Formula} nằm ngay trong câu SELECT của danh sách
     * nên không sinh N+1, đúng cách {@link #questionCount} đang làm.
     */
    @Formula("(select count(distinct a.user_id) from quiz_attempts a "
            + "where a.quiz_id = id and a.status <> 'IN_PROGRESS')")
    private int learnerCount;

    public Quiz(User owner, String title) {
        this.owner = owner;
        this.title = title;
    }
}
