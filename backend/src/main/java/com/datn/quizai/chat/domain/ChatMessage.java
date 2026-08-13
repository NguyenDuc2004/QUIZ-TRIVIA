package com.datn.quizai.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Một tin nhắn trong phiên — bảng `chat_messages` (features/08).
 * <p>
 * Không kế thừa {@code BaseEntity} vì tin nhắn <b>không bao giờ được sửa</b>: có {@code created_at}
 * là đủ, thêm {@code updated_at} chỉ tạo ra ấn tượng sai rằng nó sửa được.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /**
     * Trích dẫn học liệu đã dùng để trả lời, <b>chốt tại thời điểm trả lời</b>.
     * <p>
     * Lưu kèm chứ không tra lại lúc hiển thị: học liệu có thể bị xoá hoặc sửa sau đó, mà trích dẫn
     * phải nói đúng thứ mô hình <i>đã đọc</i> lúc trả lời — nếu không thì nó là trích dẫn giả.
     * Null với tin nhắn của người dùng.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private List<ChatSource> sources;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ChatMessage(ChatSession session, ChatRole role, String content, List<ChatSource> sources) {
        this.session = session;
        this.role = role;
        this.content = content;
        this.sources = sources;
    }
}
