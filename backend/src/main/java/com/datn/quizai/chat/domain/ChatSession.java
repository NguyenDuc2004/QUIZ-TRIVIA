package com.datn.quizai.chat.domain;

import com.datn.quizai.common.BaseEntity;
import com.datn.quizai.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một phiên hội thoại với trợ lý học tập — bảng `chat_sessions` (features/08).
 * <p>
 * Cố ý <b>không</b> map {@code @OneToMany} sang {@code chat_messages}: một phiên dài có hàng trăm
 * tin nhắn, mà mọi chỗ dùng đến chúng đều chỉ cần <i>vài lượt gần nhất</i> (dựng ngữ cảnh) hoặc
 * <i>một trang</i> (hiển thị). Nạp cả cụm chỉ để đọc tiêu đề là lãng phí.
 */
@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ChatSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Đặt từ câu hỏi đầu tiên, cắt ngắn — chỉ cần đủ để người dùng nhận ra phiên nào là phiên nào. */
    @Column(nullable = false, length = 200)
    private String title;

    public ChatSession(User user, String title) {
        this.user = user;
        this.title = title;
    }
}
