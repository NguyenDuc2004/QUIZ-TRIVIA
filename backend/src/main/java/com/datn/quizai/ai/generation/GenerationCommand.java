package com.datn.quizai.ai.generation;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.QuestionType;

import java.util.List;
import java.util.UUID;

/**
 * Tham số một lần sinh đề, đã chuẩn hoá từ request HTTP.
 *
 * @param materialId   học liệu cần bám theo; null mà {@code useMaterials} vẫn true thì tìm trong
 *                     toàn bộ học liệu của người dùng
 * @param useMaterials false = sinh theo kiến thức chung, không dùng RAG
 */
public record GenerationCommand(
        String topic,
        int count,
        List<QuestionType> types,
        Difficulty difficulty,
        UUID materialId,
        boolean useMaterials
) {
}
