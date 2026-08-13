package com.datn.quizai.chat.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.chat.dto.AskableMaterialResponse;
import com.datn.quizai.chat.dto.ChatAskRequest;
import com.datn.quizai.chat.dto.ChatMessageResponse;
import com.datn.quizai.chat.dto.ChatSessionResponse;
import com.datn.quizai.chat.service.ChatService;
import com.datn.quizai.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Trợ lý học tập RAG — docs/api.md §6 (docs/features/08, FR-31).
 * <p>
 * <b>Không</b> nằm trong {@code AiController}: lớp đó gắn {@code @PreAuthorize("hasAnyRole('CREATOR',
 * 'ADMIN')")} ở cấp lớp vì nó là bộ công cụ soạn nội dung. Trợ lý học tập thì ngược lại — người học
 * chính là đối tượng nó phục vụ. Nhồi vào đó rồi phải đục một lỗ ngoại lệ trong luật phân quyền của cả
 * lớp là cách chắc chắn để sau này có người mở rộng quyền quá tay.
 * <p>
 * Guest vẫn không dùng được: hội thoại có ngữ cảnh cần một danh tính để lưu phiên, và mỗi lượt hỏi là
 * một lượt hạn mức AI — mở cho người chưa đăng nhập là mở cửa cho người lạ đốt hạn mức.
 */
@RestController
@RequestMapping("/api/v1/ai/chat")
@Tag(name = "AI — Trợ lý học tập", description = "Hỏi đáp bám học liệu, trả lời theo luồng (SSE)")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Hỏi trợ lý, nhận câu trả lời theo luồng.
     * <p>
     * Ba loại sự kiện, mỗi loại một tên riêng để client xử lý khác nhau:
     * <ul>
     *   <li>{@code meta} — <b>một lần, trước mọi chữ</b>: id phiên và danh sách học liệu sẽ dựa vào.
     *       Gửi trước để giao diện hiện được nguồn ngay lúc chữ bắt đầu chạy, và để lượt hỏi đầu tiên
     *       biết id phiên vừa mở mà không cần gọi thêm API.</li>
     *   <li>{@code token} — từng mảnh văn bản, ghép lại thành câu trả lời.</li>
     *   <li>{@code error} — hết hạn mức hoặc mô hình không phản hồi. Phải là <b>sự kiện trong luồng</b>
     *       chứ không phải mã lỗi HTTP: header đã gửi đi từ lúc mở luồng, không đổi status được nữa.</li>
     * </ul>
     * <p>
     * Bước chuẩn bị (mở phiên, lưu câu hỏi, truy xuất học liệu) chạy <b>đồng bộ trước</b> khi mở luồng.
     * Nhờ vậy lỗi ở giai đoạn đó — phiên không tồn tại, câu hỏi rỗng — vẫn trả về đúng mã HTTP như một
     * API thường.
     */
    // Khai charset tường minh cho response tự mô tả được. Đo thật thì nó KHÔNG bắt buộc: chuẩn SSE
    // quy định luồng luôn là UTF-8 và cả EventSource của trình duyệt lẫn WebClient đều giải mã UTF-8
    // dù header không nói gì. (Chữ có dấu từng ra "chÃ o" lúc viết test — nhưng đó là do
    // MockMvc.getContentAsString() mặc định ISO-8859-1, tức lỗi ở phía đọc của test, không phải trên
    // dây. Ghi lại đây để không ai sau này tưởng dòng này đang vá một lỗi thật.)
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    @Operation(summary = "Hỏi trợ lý học tập (FR-31). Trả lời theo luồng SSE: sự kiện `meta` mang id "
            + "phiên và nguồn học liệu, `token` mang từng mảnh chữ, `error` mang lỗi giữa luồng.")
    public Flux<ServerSentEvent<Object>> ask(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @Valid @RequestBody ChatAskRequest request) {

        ChatService.Prepared prepared = chatService.prepare(
                current.id(), request.sessionId(), request.materialId(), request.question());

        Flux<ServerSentEvent<Object>> meta = Flux.just(event("meta", Map.of(
                "sessionId", prepared.sessionId(),
                "sources", prepared.sources())));

        Flux<ServerSentEvent<Object>> tokens = chatService.streamAnswer(current.id(), prepared)
                // Bọc trong JSON, KHÔNG gửi chuỗi thô: chuẩn SSE quy định client bỏ một khoảng trắng
                // đứng ngay sau "data:", nên mảnh bắt đầu bằng khoảng trắng — mà mảnh của Gemini rất
                // thường như vậy (" và", " the") — sẽ bị bóc mất space và câu trả lời hiện ra dính chữ
                // vào nhau. Dấu ngoặc kép của JSON giữ nguyên từng ký tự.
                .map(delta -> event("token", Map.of("t", delta)))
                .onErrorResume(error -> {
                    log.warn("Luồng trả lời của phiên {} dừng giữa đường: {}",
                            prepared.sessionId(), error.getMessage());
                    return Flux.just(event("error", Map.of("message", userMessage(error))));
                });

        return Flux.concat(meta, tokens);
    }

    /**
     * Đặt ở đây chứ không ở {@code AiController}: lớp đó gắn {@code @PreAuthorize} CREATOR/ADMIN cấp
     * lớp, mà danh sách này người học cũng phải xem được. Đục một lỗ ngoại lệ trong luật phân quyền
     * của cả lớp là cách chắc chắn để sau này có người mở quyền quá tay.
     */
    @GetMapping("/materials")
    @Operation(summary = "Học liệu tôi được phép hỏi trợ lý: tài liệu của tôi + tài liệu đã được "
            + "chia sẻ. Chỉ trả metadata, KHÔNG trả nội dung tài liệu.")
    public List<AskableMaterialResponse> askableMaterials(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return chatService.askableMaterials(current.id());
    }

    @GetMapping("/sessions")
    @Operation(summary = "Các phiên hội thoại của tôi, mới hoạt động nhất trước")
    public List<ChatSessionResponse> sessions(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return chatService.sessions(current.id());
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Toàn bộ tin nhắn của một phiên. Phiên của người khác trả 404.")
    public List<ChatMessageResponse> messages(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID sessionId) {
        return chatService.messages(sessionId, current.id());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Operation(summary = "Xoá một phiên hội thoại của tôi, kèm toàn bộ tin nhắn")
    public void deleteSession(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID sessionId) {
        chatService.deleteSession(sessionId, current.id());
    }

    private ServerSentEvent<Object> event(String name, Object data) {
        return ServerSentEvent.builder(data).event(name).build();
    }

    /**
     * Thông điệp cho người dùng đọc.
     * <p>
     * {@link BusinessException} đã mang câu tiếng Việt có ích ("hết hạn mức, chờ 52 giây") nên dùng
     * thẳng. Lỗi khác thì <b>không</b> lộ nguyên văn: nó là chi tiết kỹ thuật của bên thứ ba, người học
     * đọc cũng không làm gì được, mà lại tiết lộ hệ thống bên trong.
     */
    private String userMessage(Throwable error) {
        if (error instanceof BusinessException e) {
            return e.getMessage();
        }
        return "Trợ lý đang gặp sự cố. Thử hỏi lại sau ít phút.";
    }
}
