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

    /**
     * Model dùng cho embedding — thường khác model sinh văn bản.
     * Tách riêng để bản ghi audit nói đúng model nào đã tạo ra vector.
     */
    default String embeddingModel() {
        return model();
    }

    /** Không phải provider nào cũng có API embedding (xAI hiện không có). */
    default boolean supportsEmbedding() {
        return false;
    }

    /** Có sinh văn bản theo luồng token hay không — cần cho trợ lý học tập (features/08). */
    default boolean supportsStreaming() {
        return false;
    }

    /**
     * Sinh văn bản theo <b>luồng</b>: mỗi phần tử là một mảnh, ghép lại được câu trả lời đầy đủ.
     * <p>
     * Tồn tại vì thứ người dùng cảm nhận ở một trợ lý hội thoại là <b>thời gian tới chữ đầu tiên</b>,
     * không phải tổng thời gian. Chờ 8 giây màn hình trắng rồi hiện cả đoạn thì tệ hơn hẳn việc chữ
     * bắt đầu chạy sau 0,5 giây dù cùng kết thúc một lúc.
     * <p>
     * <b>Không</b> giả lập bằng cách gọi {@link #complete} rồi cắt nhỏ chuỗi trả về: làm vậy thì thời
     * gian tới chữ đầu tiên y nguyên, chỉ thêm một lớp trang trí để trông như đang chảy.
     */
    default reactor.core.publisher.Flux<String> stream(AiPrompt prompt) {
        throw new UnsupportedOperationException(name() + " không hỗ trợ streaming");
    }

    /** @return vector embedding của đoạn văn bản; số chiều phải khớp cột `material_chunks.embedding` */
    default List<Float> embed(String text) {
        throw new UnsupportedOperationException(name() + " không hỗ trợ embedding");
    }
}
