package com.datn.quizai.ai.domain;

import com.datn.quizai.common.BaseEntity;
import com.datn.quizai.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một tài liệu học liệu người dùng nạp vào để AI sinh đề bám theo — bảng `learning_materials`
 * (docs/features/05-ai-rag-generation.md).
 * <p>
 * Các đoạn đã cắt kèm vector nằm ở `material_chunks`; cố ý <b>không</b> map quan hệ
 * {@code @OneToMany} sang đó: một tài liệu có thể có hàng trăm đoạn, mỗi đoạn mang vector 768
 * chiều, nạp cả cụm chỉ để đọc tiêu đề là lãng phí. Truy vấn vector đi qua repository riêng.
 */
@Entity
@Table(name = "learning_materials")
@Getter
@Setter
@NoArgsConstructor
public class LearningMaterial extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 10)
    private MaterialSourceType sourceType;

    @Column(length = 100)
    private String topic;

    /** Đường dẫn file gốc để người dùng tải lại; null khi họ dán thẳng văn bản. */
    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MaterialStatus status = MaterialStatus.PROCESSING;

    @Column(name = "char_count", nullable = false)
    private int charCount;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /**
     * Cho phép mọi người học hỏi trợ lý AI trên tài liệu này (features/08).
     * <p>
     * Mặc định <b>false</b>: chia sẻ phải là hành động có ý thức của chủ tài liệu. Bật mặc định thì
     * mọi tài liệu đã tải lên trước khi có tính năng này bỗng thành công khai, mà chủ của chúng chưa
     * từng đồng ý điều đó.
     */
    @Column(nullable = false)
    private boolean shared = false;

    public LearningMaterial(User owner, String title, MaterialSourceType sourceType) {
        this.owner = owner;
        this.title = title;
        this.sourceType = sourceType;
    }

    public void markReady(int charCount, int chunkCount) {
        this.status = MaterialStatus.READY;
        this.charCount = charCount;
        this.chunkCount = chunkCount;
        this.errorMessage = null;
    }

    public void markFailed(String reason) {
        this.status = MaterialStatus.FAILED;
        this.errorMessage = reason;
    }
}
