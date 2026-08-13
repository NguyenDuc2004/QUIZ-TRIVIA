package com.datn.quizai.chat.service;

import com.datn.quizai.chat.domain.ChatMessage;
import com.datn.quizai.chat.domain.ChatRole;
import com.datn.quizai.chat.domain.ChatSession;
import com.datn.quizai.chat.repository.ChatMessageRepository;
import com.datn.quizai.chat.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Ghi tin nhắn trong <b>transaction riêng</b>, tách khỏi {@link ChatService}.
 * <p>
 * Bean riêng chứ không phải phương thức trong {@code ChatService}, vì hai lý do độc lập:
 * <ol>
 *   <li><b>Câu trả lời được ghi khi luồng stream kết thúc</b> — lúc đó code đang chạy trên luồng của
 *       WebClient, hoàn toàn ngoài transaction đã mở lúc nhận request (transaction đó đã commit từ
 *       lâu, ngay sau khi lưu câu hỏi). Không có transaction mới thì lệnh ghi không chạy.</li>
 *   <li><b>Spring bỏ qua {@code @Transactional} khi gọi phương thức trong cùng một bean</b> — proxy
 *       chỉ chặn lời gọi đi từ ngoài vào. Đặt cùng lớp thì annotation im lặng vô hiệu, và đây là cái
 *       bẫy đã sập ba lần trong dự án này.</li>
 * </ol>
 */
@Service
public class ChatMessageWriter {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageWriter.class);

    private final ChatMessageRepository messageRepository;
    private final ChatSessionRepository sessionRepository;

    public ChatMessageWriter(ChatMessageRepository messageRepository,
                             ChatSessionRepository sessionRepository) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Lưu câu trả lời của trợ lý và đẩy phiên lên đầu danh sách.
     * <p>
     * Nuốt lỗi và chỉ ghi log: người dùng <b>đã đọc</b> câu trả lời trên màn hình rồi. Ném lỗi ở đây
     * chỉ làm luồng SSE kết thúc bằng một thông báo hỏng ngay sau một câu trả lời hoàn chỉnh — người
     * dùng sẽ tưởng câu trả lời vừa đọc là sai. Mất một dòng lịch sử nhẹ hơn nhiều.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAnswer(UUID sessionId, String answer, List<com.datn.quizai.chat.domain.ChatSource> sources) {
        try {
            ChatSession session = sessionRepository.getReferenceById(sessionId);
            messageRepository.save(new ChatMessage(session, ChatRole.ASSISTANT, answer,
                    sources.isEmpty() ? null : sources));
            sessionRepository.touch(sessionId);
        } catch (Exception e) {
            log.warn("Không lưu được câu trả lời của phiên {}: {}", sessionId, e.getMessage());
        }
    }
}
