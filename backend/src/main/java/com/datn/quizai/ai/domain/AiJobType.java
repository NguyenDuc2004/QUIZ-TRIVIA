package com.datn.quizai.ai.domain;

/** Các loại tác vụ AI chạy nền. */
public enum AiJobType {
    /** Trích text → cắt đoạn → sinh embedding cho một tài liệu. */
    INGEST_MATERIAL,
    /** Sinh bộ câu hỏi nháp từ học liệu hoặc từ chủ đề. */
    GENERATE_QUESTIONS,
    /**
     * Sinh thẻ ghi nhớ nháp từ học liệu (features/11, FR-38).
     * <p>
     * Thêm giá trị ở đây phải đi kèm migration sửa {@code ck_ai_jobs_type} — xem V14.
     */
    GENERATE_FLASHCARDS
}
