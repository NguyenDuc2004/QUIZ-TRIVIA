package com.datn.quizai.attempt.service;

import com.datn.quizai.attempt.domain.AnswerPayload;
import com.datn.quizai.attempt.domain.GradedBy;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.QuestionOption;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chấm điểm câu có đáp án cố định (FR-15).
 * <p>
 * Cố tình <b>không</b> gọi AI ở đây: bốn loại câu trắc nghiệm/điền khuyết so khớp được bằng
 * logic thuần nên chấm tại chỗ, vừa nhanh vừa không tốn chi phí (docs/features/03 §Ghi chú).
 * Riêng SHORT_ANSWER trả về {@link GradedBy#PENDING_AI} để features/06 chấm sau.
 * <p>
 * Lớp thuần logic, không phụ thuộc Spring — test bằng unit test thường.
 */
public final class AnswerGrader {

    private AnswerGrader() {
    }

    /**
     * Kết quả chấm một câu.
     *
     * @param correct  null khi chưa kết luận được (câu chờ AI chấm)
     * @param score    điểm đạt được, luôn 0..{@code question.points}
     * @param gradedBy nguồn chấm để hiển thị và để job AI biết cần xử lý câu nào
     */
    public record GradeResult(Boolean correct, int score, GradedBy gradedBy) {

        static GradeResult of(boolean correct, int points) {
            return new GradeResult(correct, correct ? points : 0, GradedBy.AUTO);
        }
    }

    /** Câu bỏ trống: 0 điểm, tính là sai, không cần đẩy sang AI. */
    private static final GradeResult BLANK = new GradeResult(false, 0, GradedBy.AUTO);

    public static GradeResult grade(Question question, AnswerPayload answer) {
        int points = question.getPoints() == null ? 1 : question.getPoints();

        // Bỏ trống thì kết luận được ngay, kể cả câu tự luận — không tốn một lượt gọi AI
        if (answer == null || answer.isEmpty()) {
            return BLANK;
        }

        return switch (question.getType()) {
            // Một đáp án đúng duy nhất: chọn đúng một lựa chọn và phải là lựa chọn đúng
            case SINGLE_CHOICE, TRUE_FALSE -> {
                Set<UUID> selected = selectedIds(answer);
                yield GradeResult.of(selected.size() == 1 && selected.equals(correctIds(question)), points);
            }
            // Nhiều đáp án đúng: chấm trọn gói — chọn thiếu hoặc chọn thừa đều tính sai
            case MULTIPLE_CHOICE -> GradeResult.of(selectedIds(answer).equals(correctIds(question)), points);
            // Điền khuyết: khớp với bất kỳ đáp án nào được chấp nhận
            case FILL_BLANK -> {
                String typed = normalize(answer.text());
                boolean matched = question.getOptions().stream()
                        .filter(QuestionOption::isCorrect)
                        .anyMatch(option -> normalize(option.getContent()).equals(typed));
                yield GradeResult.of(matched, points);
            }
            // Tự luận: máy không chấm được, để AI xử lý sau (features/06)
            case SHORT_ANSWER -> new GradeResult(null, 0, GradedBy.PENDING_AI);
        };
    }

    private static Set<UUID> selectedIds(AnswerPayload answer) {
        return answer.optionIds() == null ? Set.of() : Set.copyOf(answer.optionIds());
    }

    private static Set<UUID> correctIds(Question question) {
        return question.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .map(QuestionOption::getId)
                .collect(Collectors.toSet());
    }

    /**
     * So khớp đáp án điền khuyết bỏ qua hoa/thường và khoảng trắng thừa.
     * <p>
     * Cố ý <b>không</b> bỏ dấu tiếng Việt: "toán" và "toan" là hai từ khác nhau,
     * chấp nhận cả hai sẽ chấm sai thành đúng.
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
