package com.datn.quizai.ai.service;

import com.datn.quizai.ai.domain.MaterialStatus;
import com.datn.quizai.ai.repository.LearningMaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ghi trạng thái học liệu trong transaction ngắn, tách riêng khỏi {@link MaterialIngestionService}.
 * <p>
 * Phải là <b>bean riêng</b>: gọi {@code this.method()} trong cùng một lớp sẽ đi thẳng, không qua
 * proxy của Spring, nên {@code @Transactional} không có tác dụng và thay đổi không bao giờ được
 * ghi xuống. Việc nạp học liệu kéo dài hàng phút nên cũng không thể bọc cả quá trình trong một
 * transaction — chỉ những lần ghi ngắn này mới cần.
 */
@Service
public class MaterialStatusWriter {

    private final LearningMaterialRepository repository;

    public MaterialStatusWriter(LearningMaterialRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markReady(UUID materialId, int charCount, int chunkCount) {
        repository.findById(materialId)
                .ifPresent(material -> material.markReady(charCount, chunkCount));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID materialId, String reason) {
        repository.findById(materialId).ifPresent(material -> {
            if (material.getStatus() != MaterialStatus.READY) {
                material.markFailed(reason);
            }
        });
    }
}
