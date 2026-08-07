package com.datn.quizai.attempt.dto;

import com.datn.quizai.attempt.domain.GradedBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Một bài làm nhìn từ phía <b>người chấm</b> (features/09, trả nợ features/06).
 * <p>
 * Vì sao không dùng lại {@code AttemptDetailResponse}: nó là màn hình của <i>người học xem bài mình
 * làm</i> — trả về mọi câu, kèm đáp án đúng và lời giải thích. Chủ quiz cần thứ khác hẳn: chỉ những
 * câu <b>người</b> phải chấm, kèm tiêu chí chấm và những gì AI đã nói. Ép một DTO phục vụ hai mục
 * đích thì mỗi lần thêm trường lại phải nghĩ "trường này ai được thấy" — và sẽ có lần nghĩ sai.
 * <p>
 * Chỉ chứa câu tự luận. Câu trắc nghiệm máy chấm theo đáp án cố định, không có gì để người xem lại;
 * đưa chúng vào đây là mở rộng phạm vi bài làm mà chủ quiz đọc được, không đổi lấy điều gì.
 *
 * @param answers theo thứ tự câu trong đề, không phải theo trạng thái chấm — người chấm đọc bài
 *                theo mạch bài làm
 */
public record GradingViewResponse(
        UUID attemptId,
        UUID quizId,
        String quizTitle,
        String learnerName,
        OffsetDateTime submittedAt,
        int totalScore,
        int maxScore,
        List<EssayAnswer> answers
) {
    /**
     * Một câu tự luận cần (hoặc đã được) chấm.
     *
     * @param rubric       tiêu chí chấm — thứ AI đã dùng để chấm, nên người chấm lại phải thấy đúng
     *                     nó, nếu không hai lượt chấm dựa trên hai chuẩn khác nhau
     * @param sampleAnswer đáp án mẫu, gộp các cách trả lời được chấp nhận
     * @param score        điểm hiện tại; với {@link GradedBy#PENDING_AI} và
     *                     {@link GradedBy#AI_FAILED} thì là 0 và <b>không có nghĩa là bài sai</b> —
     *                     đọc {@code gradedBy} trước khi hiển thị con số này
     * @param needsGrading câu này đang chờ người chấm; câu đã có điểm AI vẫn sửa được nhưng không
     *                     nằm trong nhóm cần làm
     */
    public record EssayAnswer(
            UUID answerId,
            int orderIndex,
            String questionContent,
            String rubric,
            String sampleAnswer,
            String learnerAnswer,
            int score,
            int maxScore,
            GradedBy gradedBy,
            String aiFeedback,
            String aiSuggestions,
            boolean needsGrading
    ) {
    }
}
