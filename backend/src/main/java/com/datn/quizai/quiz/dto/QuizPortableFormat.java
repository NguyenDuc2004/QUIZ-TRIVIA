package com.datn.quizai.quiz.dto;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.QuestionType;
import com.datn.quizai.quiz.domain.Quiz;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Định dạng mang đi được của một quiz — dùng cho cả xuất lẫn nhập (features/02, FR-12).
 *
 * <h3>JSON, không CSV</h3>
 * Đặc tả ghi "JSON/CSV". Chỉ làm JSON, vì một quiz là dữ liệu <b>lồng nhau</b>: mỗi câu có nhiều lựa chọn,
 * mỗi lựa chọn có cờ đúng/sai. Nhét cấu trúc đó vào bảng phẳng thì phải chọn một trong hai cách, và cả hai
 * đều tệ:
 * <ul>
 *   <li>Một dòng mỗi lựa chọn → thông tin câu hỏi lặp lại ở mọi dòng, và sửa một chỗ quên chỗ kia là hỏng.</li>
 *   <li>Cột {@code option1..option6} → chặn cứng số lựa chọn, và câu điền khuyết nhiều đáp án thì không đủ chỗ.</li>
 * </ul>
 * Bảng điểm lớp thì ngược lại — nó vốn phẳng, nên [FR-58] dùng CSV. Chọn định dạng theo <i>hình dạng dữ
 * liệu</i>, không theo thói quen.
 *
 * <h3>KHÔNG mang theo id, chủ sở hữu, hay số liệu thống kê</h3>
 * File xuất ra là <b>nội dung đề</b>, không phải bản sao một dòng cơ sở dữ liệu. Mang theo id thì nhập vào
 * máy khác sẽ đụng id có sẵn hoặc ghi đè nhầm quiz của người khác; mang theo lượt làm bài thì nhập xong
 * quiz mới đã "có 500 lượt học" mà chưa ai làm — đúng kiểu bịa số mà cả dự án tránh.
 * <p>
 * Ảnh cũng không mang theo: {@code imageUrl} trỏ vào thư mục {@code uploads/} của <i>máy này</i>, và nhập
 * sang máy khác thì đường dẫn đó không tồn tại. Xuất kèm chỉ tạo ra một đề đầy ảnh vỡ.
 */
public record QuizPortableFormat(

        /** Phiên bản định dạng — có nó thì bản đọc sau này biết file cũ tới mức nào mà xử lý cho đúng. */
        int formatVersion,

        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description,

        Difficulty difficulty,

        Integer timeLimitSec,

        @NotEmpty(message = "File phải có ít nhất một câu hỏi")
        @Valid
        List<CauHoi> questions
) {
    /** Phiên bản hiện tại. Tăng khi cấu trúc đổi theo cách bản đọc cũ không hiểu được. */
    public static final int PHIEN_BAN = 1;

    public record CauHoi(
            @NotNull(message = "Thiếu loại câu hỏi")
            QuestionType type,

            @NotBlank(message = "Nội dung câu hỏi không được để trống")
            String content,

            String explanation,

            /** Tiêu chí chấm câu tự luận (features/06); giữ lại vì nó là một phần của đề, không phải số liệu. */
            String rubric,

            Difficulty difficulty,
            String topic,
            Integer points,

            @NotEmpty(message = "Câu hỏi phải có ít nhất một lựa chọn/đáp án")
            @Valid
            List<LuaChon> options
    ) {
    }

    public record LuaChon(
            @NotBlank(message = "Nội dung lựa chọn không được để trống")
            String content,
            boolean correct
    ) {
    }

    /** Dựng file xuất từ quiz đã nạp kèm câu hỏi. */
    public static QuizPortableFormat from(Quiz quiz, List<Question> questions) {
        return new QuizPortableFormat(
                PHIEN_BAN,
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getDifficulty(),
                quiz.getTimeLimitSec(),
                questions.stream().map(QuizPortableFormat::cauHoiCua).toList());
    }

    private static CauHoi cauHoiCua(Question q) {
        return new CauHoi(
                q.getType(),
                q.getContent(),
                q.getExplanation(),
                q.getRubric(),
                q.getDifficulty(),
                q.getTopic(),
                q.getPoints(),
                q.getOptions().stream()
                        .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                        .map(o -> new LuaChon(o.getContent(), o.isCorrect()))
                        .toList());
    }
}
