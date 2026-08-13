package com.datn.quizai.chat.repository;

import com.datn.quizai.chat.domain.ChatMessage;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /** Toàn bộ tin nhắn của phiên, cũ trước mới sau — dùng khi mở lại phiên trên giao diện. */
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    /**
     * {@code limit} tin nhắn <b>gần nhất</b>, mới trước cũ sau — dùng để dựng ngữ cảnh cho prompt.
     * <p>
     * Lấy ngược rồi đảo lại ở tầng trên, chứ không lấy hết cả phiên rồi cắt: phiên dài có hàng trăm
     * tin nhắn mà prompt chỉ cần vài lượt cuối.
     */
    List<ChatMessage> findBySessionIdOrderByCreatedAtDesc(UUID sessionId, Limit limit);
}
