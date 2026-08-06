package com.datn.quizai.ai.dto;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Yêu cầu sinh đề. Bỏ trống {@code materialId} và để {@code useMaterials=false} thì sinh theo
 * kiến thức chung; ngược lại dùng RAG bám theo học liệu.
 */
public record GenerateQuestionsRequest(
        @Size(max = 200, message = "Chủ đề tối đa 200 ký tự")
        String topic,

        @Min(value = 1, message = "Ít nhất 1 câu")
        @Max(value = 20, message = "Tối đa 20 câu mỗi lần")
        int count,

        List<QuestionType> types,
        Difficulty difficulty,
        UUID materialId,
        boolean useMaterials
) {
}
