package com.datn.quizai.flashcard.domain;

/**
 * Thẻ này ở đâu ra (features/11).
 * <p>
 * Giữ lại nguồn gốc vì nó trả lời được một câu hỏi thực tế khi ôn: thẻ do AI sinh cần được nhìn bằng con
 * mắt khác thẻ người tự viết, còn thẻ sinh từ câu trả lời sai là bằng chứng cho vòng lặp
 * <i>làm sai → ôn lại</i> mà cả tính năng này tồn tại để tạo ra.
 */
public enum FlashcardSource {
    /** Người dùng tự viết. */
    MANUAL,
    /** Sinh từ học liệu qua pipeline RAG. */
    AI_GENERATED,
    /** Sinh từ một câu người học đã trả lời sai. */
    FROM_WRONG_ANSWER
}
