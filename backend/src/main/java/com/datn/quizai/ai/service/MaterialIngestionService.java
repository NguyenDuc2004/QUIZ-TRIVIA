package com.datn.quizai.ai.service;

import com.datn.quizai.ai.provider.AiOrchestrator;
import com.datn.quizai.ai.rag.TextChunker;
import com.datn.quizai.ai.repository.MaterialChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

/**
 * Nạp học liệu vào kho vector: cắt đoạn → sinh embedding → ghi vào pgvector
 * (docs/features/05 — pipeline ingestion).
 * <p>
 * <b>Chạy sau khi transaction tạo học liệu đã commit</b> ({@code @TransactionalEventListener} với
 * pha {@code AFTER_COMMIT}). Nếu khởi động luồng nền ngay lúc gọi, nó sẽ đọc CSDL trước khi dòng
 * học liệu kịp commit và không thấy gì.
 * <p>
 * <b>Không</b> bọc cả quá trình trong một transaction: mỗi đoạn là một lời gọi API, tài liệu dài
 * mất hàng phút — giữ transaction suốt thời gian đó là giam một kết nối CSDL vô ích. Các lần ghi
 * ngắn đi qua {@link MaterialStatusWriter}.
 */
@Service
public class MaterialIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MaterialIngestionService.class);

    private final MaterialChunkRepository chunkRepository;
    private final AiOrchestrator aiOrchestrator;
    private final MaterialStatusWriter statusWriter;

    public MaterialIngestionService(MaterialChunkRepository chunkRepository,
                                    AiOrchestrator aiOrchestrator,
                                    MaterialStatusWriter statusWriter) {
        this.chunkRepository = chunkRepository;
        this.aiOrchestrator = aiOrchestrator;
        this.statusWriter = statusWriter;
    }

    /**
     * Nuốt mọi lỗi và ghi vào {@code error_message} thay vì để văng ra: đây là luồng nền, không ai
     * đứng đó nhận exception, mà tài liệu kẹt mãi ở {@code PROCESSING} thì người dùng không hiểu
     * chuyện gì xảy ra.
     */
    @Async("aiTaskExecutor")
    @TransactionalEventListener
    public void onMaterialCreated(MaterialCreatedEvent event) {
        try {
            ingest(event.materialId(), event.rawText(), event.ownerId());
        } catch (Exception e) {
            log.error("Nạp học liệu {} thất bại", event.materialId(), e);
            statusWriter.markFailed(event.materialId(), truncate(e.getMessage()));
        }
    }

    /** Tách riêng để test gọi thẳng được, không phải dựng cả cơ chế sự kiện. */
    public void ingest(UUID materialId, String rawText, UUID ownerId) {
        List<String> chunks = TextChunker.chunk(rawText);
        if (chunks.isEmpty()) {
            statusWriter.markFailed(materialId, "Tài liệu quá ngắn hoặc không có nội dung dùng được");
            return;
        }

        // Nạp lại tài liệu cũ thì xoá đoạn cũ trước, tránh trộn hai lần nạp vào nhau
        chunkRepository.deleteByMaterialId(materialId);

        for (int i = 0; i < chunks.size(); i++) {
            List<Float> embedding = aiOrchestrator.embed(chunks.get(i), ownerId);
            chunkRepository.insert(materialId, i, chunks.get(i), embedding);
        }

        statusWriter.markReady(materialId, rawText.length(), chunks.size());
        log.info("Đã nạp học liệu {}: {} ký tự → {} đoạn", materialId, rawText.length(), chunks.size());
    }

    /** Thông điệp lỗi của bên thứ ba có thể rất dài; cắt bớt cho vừa ô hiển thị. */
    private String truncate(String message) {
        if (message == null) {
            return "Lỗi không xác định";
        }
        return message.length() <= 500 ? message : message.substring(0, 500) + "…";
    }
}
