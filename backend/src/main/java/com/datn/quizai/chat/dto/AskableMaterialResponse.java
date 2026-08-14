package com.datn.quizai.chat.dto;

import com.datn.quizai.ai.domain.LearningMaterial;

import java.util.UUID;

/**
 * Một học liệu mà người gọi được phép hỏi trợ lý (features/08).
 * <p>
 * <b>Chỉ mang metadata, cố ý không có {@code content} và không có đoạn nào.</b> Người học được
 * <i>hỏi trên</i> tài liệu, không được <i>đọc toàn văn</i> tài liệu của người khác — đây đúng là lằn
 * ranh đã đặt ra khi thêm cờ {@code shared} ở V10. Trả kèm nội dung ở đây sẽ mở một đường đọc trọn
 * tài liệu mà chủ của nó chưa từng đồng ý.
 *
 * @param mine tài liệu này của chính người gọi hay của người khác chia sẻ — để giao diện nói rõ
 *             nguồn, tránh việc người học tưởng mình sở hữu tài liệu mà thực ra chỉ được đọc ké
 */
public record AskableMaterialResponse(
        UUID id,
        String title,
        String topic,
        String sourceType,
        int chunkCount,
        boolean mine
) {
    public static AskableMaterialResponse from(LearningMaterial material, UUID currentUserId) {
        return new AskableMaterialResponse(
                material.getId(),
                material.getTitle(),
                material.getTopic(),
                material.getSourceType() == null ? null : material.getSourceType().name(),
                material.getChunkCount(),
                material.getOwner() != null && currentUserId.equals(material.getOwner().getId()));
    }
}
