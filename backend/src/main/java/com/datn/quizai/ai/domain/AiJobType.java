package com.datn.quizai.ai.domain;

/** Các loại tác vụ AI chạy nền. */
public enum AiJobType {
    /** Trích text → cắt đoạn → sinh embedding cho một tài liệu. */
    INGEST_MATERIAL,
    /** Sinh bộ câu hỏi nháp từ học liệu hoặc từ chủ đề. */
    GENERATE_QUESTIONS
}
