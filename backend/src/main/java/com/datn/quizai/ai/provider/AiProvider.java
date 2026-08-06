package com.datn.quizai.ai.provider;

import java.util.List;

/**
 * Giao diện chung cho mọi nhà cung cấp mô hình (docs/architecture.md §5).
 * <p>
 * Nghiệp vụ chỉ nói chuyện với {@code AiOrchestrator}, không bao giờ gọi thẳng lớp cài đặt —
 * nhờ vậy đổi/thêm provider không phải sửa code sinh đề hay chấm bài
 * (docs/conventions.md §1 — AI).
 */
public interface AiProvider {

    /** Tên dùng trong cấu hình `app.ai.provider-order` và trong bản ghi audit. */
    String name();

    /** Model đang dùng, ghi vào audit để biết kết quả sinh ra từ đâu. */
    String model();

    /** Chưa cấu hình API key thì orchestrator bỏ qua provider này thay vì gọi rồi lỗi. */
    boolean isConfigured();

    AiCompletion complete(AiPrompt prompt);

    /** Không phải provider nào cũng có API embedding (xAI hiện không có). */
    default boolean supportsEmbedding() {
        return false;
    }

    /** @return vector embedding của đoạn văn bản; số chiều phải khớp cột `material_chunks.embedding` */
    default List<Float> embed(String text) {
        throw new UnsupportedOperationException(name() + " không hỗ trợ embedding");
    }
}
