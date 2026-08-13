package com.datn.quizai.chat.service;

import com.datn.quizai.ai.chat.ChatPromptBuilder;
import com.datn.quizai.ai.provider.AiOrchestrator;
import com.datn.quizai.ai.provider.AiPrompt;
import com.datn.quizai.ai.repository.LearningMaterialRepository;
import com.datn.quizai.ai.repository.MaterialChunkRepository;
import com.datn.quizai.chat.domain.ChatMessage;
import com.datn.quizai.chat.domain.ChatRole;
import com.datn.quizai.chat.domain.ChatSession;
import com.datn.quizai.chat.domain.ChatSource;
import com.datn.quizai.chat.dto.AskableMaterialResponse;
import com.datn.quizai.chat.dto.ChatMessageResponse;
import com.datn.quizai.chat.dto.ChatSessionResponse;
import com.datn.quizai.chat.repository.ChatMessageRepository;
import com.datn.quizai.chat.repository.ChatSessionRepository;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Trợ lý học tập RAG (docs/features/08 — FR-31).
 * <p>
 * Một lượt hỏi đi qua bốn bước: <b>embedding câu hỏi → tìm đoạn học liệu gần nghĩa → dựng prompt có
 * rào → stream câu trả lời</b>. Dùng lại đúng pipeline RAG của features/05, không dựng kho vector thứ
 * hai.
 * <p>
 * <b>Ranh giới transaction ở đây là chỗ dễ sai nhất.</b> Một lượt hỏi gồm hai lần ghi cách nhau hàng
 * chục giây: câu hỏi ghi ngay, câu trả lời ghi khi luồng token kết thúc. Không thể gói cả hai vào một
 * transaction — giữ transaction mở suốt thời gian gọi mô hình là chiếm một kết nối CSDL để chờ mạng,
 * và chỉ vài người dùng đồng thời là cạn pool. Nên: transaction ngắn cho phần chuẩn bị, rồi
 * {@link ChatMessageWriter} mở transaction mới lúc ghi câu trả lời.
 */
@Service
public class ChatService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ChatService.class);

    /**
     * Số đoạn học liệu đưa vào prompt.
     * <p>
     * Năm là chỗ dung hoà: ít hơn thì dễ trượt đúng đoạn chứa câu trả lời, nhiều hơn thì prompt loãng
     * và mô hình bám vào đoạn ít liên quan nhất — mà token thì vẫn phải trả.
     */
    private static final int TOP_K = 5;

    /**
     * Bỏ đoạn có cosine distance vượt ngưỡng này.
     * <p>
     * Không có ngưỡng thì truy vấn vector <i>luôn</i> trả về 5 đoạn, kể cả khi tài liệu không liên
     * quan gì tới câu hỏi — "gần nhất" trong một kho toàn tài liệu Toán vẫn là một đoạn Toán khi người
     * ta hỏi về Lịch sử. Prompt khi đó có ngữ cảnh sai, và mô hình sẽ cố trả lời từ nó thay vì nói
     * "tài liệu không đề cập". Thà trả về rỗng để prompt nói thẳng là không có gì liên quan.
     */
    private static final double MAX_DISTANCE = 0.75;

    /** Độ dài tiêu đề phiên, cắt từ câu hỏi đầu tiên. */
    private static final int TITLE_MAX_CHARS = 120;

    /** Độ dài đoạn trích lưu kèm câu trả lời — đủ để đối chiếu, không phải để đọc lại cả tài liệu. */
    private static final int SOURCE_EXCERPT_CHARS = 300;

    /** Số học liệu liệt kê cho người dùng chọn — đủ để chọn, không biến thành trang duyệt tài liệu. */
    private static final int ASKABLE_LIMIT = 50;

    private final AiOrchestrator aiOrchestrator;
    private final MaterialChunkRepository chunkRepository;
    private final LearningMaterialRepository materialRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatMessageWriter messageWriter;

    public ChatService(AiOrchestrator aiOrchestrator,
                       MaterialChunkRepository chunkRepository,
                       LearningMaterialRepository materialRepository,
                       ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       UserRepository userRepository,
                       ChatMessageWriter messageWriter) {
        this.aiOrchestrator = aiOrchestrator;
        this.chunkRepository = chunkRepository;
        this.materialRepository = materialRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.messageWriter = messageWriter;
    }

    /**
     * Học liệu người dùng được phép hỏi trợ lý — của chính họ cộng tài liệu đã được chia sẻ.
     * <p>
     * Không có danh sách này thì người học hỏi mò: họ không biết kho có tài liệu gì, thuộc chủ đề nào,
     * và chỉ biết một tài liệu tồn tại <i>sau khi</i> tình cờ hỏi trúng nó qua khối trích dẫn. Đây cũng
     * là điều kiện để dùng được tham số {@code materialId} vốn đã có ở API hỏi đáp: có danh sách thì
     * giao diện mới cho chọn giới hạn câu hỏi trong một tài liệu.
     */
    @Transactional(readOnly = true)
    public List<AskableMaterialResponse> askableMaterials(UUID userId) {
        return materialRepository.findAskable(userId, Limit.of(ASKABLE_LIMIT)).stream()
                .map(m -> AskableMaterialResponse.from(m, userId))
                .toList();
    }

    /** Kết quả bước chuẩn bị: đủ thứ để bắt đầu stream, và transaction đã đóng. */
    public record Prepared(UUID sessionId, List<ChatSource> sources, AiPrompt prompt) {
    }

    /**
     * Bước chuẩn bị, chạy trong <b>một transaction ngắn</b>: mở/kiểm phiên, lưu câu hỏi, truy xuất học
     * liệu, dựng prompt.
     * <p>
     * Tách khỏi việc stream để transaction đóng lại <i>trước</i> khi gọi mô hình. Gộp chung thì một
     * kết nối CSDL bị giữ suốt thời gian chờ mạng.
     */
    @Transactional
    public Prepared prepare(UUID userId, UUID sessionId, UUID materialId, String question) {
        ChatSession session = sessionId == null
                ? openSession(userId, question)
                : sessionRepository.findOwned(sessionId, userId)
                        .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phiên hội thoại"));

        // Đọc lịch sử TRƯỚC khi lưu câu hỏi mới, nếu không câu hỏi vừa gửi lại xuất hiện trong phần
        // "hội thoại trước đó" của chính nó
        List<ChatMessage> history = recentHistory(session.getId());
        messageRepository.save(new ChatMessage(session, ChatRole.USER, question, null));

        List<MaterialChunkRepository.Chunk> chunks = retrieve(userId, materialId, question);

        AiPrompt prompt = new AiPrompt(
                ChatPromptBuilder.systemInstruction(),
                ChatPromptBuilder.userPrompt(question, chunks, history),
                false,
                // Thấp nhưng không bằng 0: giải thích cần diễn đạt tự nhiên, còn nội dung đã bị
                // neo bằng học liệu nên không cần siết thêm bằng temperature
                0.3);

        return new Prepared(session.getId(), toSources(chunks), prompt);
    }

    /**
     * Stream câu trả lời, và lưu lại khi xong.
     * <p>
     * <b>Không</b> đánh {@code @Transactional}: hàm này trả về ngay một {@code Flux} chưa chạy, nên
     * transaction (nếu có) sẽ commit trước cả token đầu tiên — một chú thích vô nghĩa mà lại làm người
     * đọc tin rằng việc ghi ở đây được bảo vệ.
     */
    public Flux<String> streamAnswer(UUID userId, Prepared prepared) {
        StringBuilder answer = new StringBuilder();
        return aiOrchestrator.stream(prepared.prompt(), "chat", userId)
                .doOnNext(answer::append)
                .doOnComplete(() -> {
                    if (!answer.isEmpty()) {
                        messageWriter.saveAnswer(prepared.sessionId(), answer.toString(), prepared.sources());
                    }
                });
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> sessions(UUID userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ChatSessionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(UUID sessionId, UUID userId) {
        // Kiểm quyền trước khi đọc tin nhắn: 404 chứ không 403 — phiên của người khác thì không được
        // biết là nó có tồn tại
        sessionRepository.findOwned(sessionId, userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phiên hội thoại"));

        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(ChatMessageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteSession(UUID sessionId, UUID userId) {
        ChatSession session = sessionRepository.findOwned(sessionId, userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phiên hội thoại"));
        sessionRepository.delete(session);
    }

    // ------------------------------------------------------------------ nội bộ

    private ChatSession openSession(UUID userId, String question) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));
        return sessionRepository.save(new ChatSession(user, title(question)));
    }

    private String title(String question) {
        String oneLine = question.strip().replaceAll("\\s+", " ");
        return oneLine.length() <= TITLE_MAX_CHARS
                ? oneLine
                : oneLine.substring(0, TITLE_MAX_CHARS - 1) + "…";
    }

    /** Vài lượt gần nhất, đảo lại thành cũ trước mới sau cho khớp mạch đọc của prompt. */
    private List<ChatMessage> recentHistory(UUID sessionId) {
        List<ChatMessage> newestFirst = messageRepository.findBySessionIdOrderByCreatedAtDesc(
                sessionId, Limit.of(ChatPromptBuilder.HISTORY_TURNS));
        List<ChatMessage> oldestFirst = new ArrayList<>(newestFirst);
        java.util.Collections.reverse(oldestFirst);
        return oldestFirst;
    }

    /**
     * Truy xuất học liệu liên quan, kèm cả tài liệu người khác <b>đã chia sẻ</b>.
     * <p>
     * Người học không sở hữu học liệu nào, nên không mở tới tài liệu chia sẻ thì mọi câu hỏi của họ
     * đều truy xuất được con số không (features/08, migration V10).
     */
    private List<MaterialChunkRepository.Chunk> retrieve(UUID userId, UUID materialId, String question) {
        List<Float> queryEmbedding = aiOrchestrator.embed(question, userId);
        List<MaterialChunkRepository.Chunk> found = chunkRepository
                .searchSimilarIncludingShared(userId, materialId, queryEmbedding, TOP_K);

        List<MaterialChunkRepository.Chunk> kept = found.stream()
                .filter(chunk -> chunk.distance() <= MAX_DISTANCE)
                .toList();

        // Ghi lại khoảng cách thật: khi trợ lý trả lời "không có tài liệu" mà kho rõ ràng có tài
        // liệu liên quan, đây là chỗ duy nhất cho biết ngưỡng lọc đang quá chặt hay embedding trượt
        if (log.isDebugEnabled()) {
            log.debug("Truy xuất '{}': {} đoạn, giữ {} (ngưỡng {}). Khoảng cách: {}",
                    ChatPromptBuilder.excerpt(question, 60), found.size(), kept.size(), MAX_DISTANCE,
                    found.stream().map(c -> "%.3f".formatted(c.distance())).toList());
        }
        return kept;
    }

    /** Mỗi tài liệu chỉ trích dẫn <b>một lần</b>, dù nhiều đoạn của nó cùng được dùng. */
    private List<ChatSource> toSources(List<MaterialChunkRepository.Chunk> chunks) {
        java.util.Map<UUID, ChatSource> byMaterial = new java.util.LinkedHashMap<>();
        for (MaterialChunkRepository.Chunk chunk : chunks) {
            byMaterial.putIfAbsent(chunk.materialId(), new ChatSource(
                    chunk.materialId(), chunk.materialTitle(),
                    ChatPromptBuilder.excerpt(chunk.content(), SOURCE_EXCERPT_CHARS)));
        }
        return List.copyOf(byMaterial.values());
    }
}
