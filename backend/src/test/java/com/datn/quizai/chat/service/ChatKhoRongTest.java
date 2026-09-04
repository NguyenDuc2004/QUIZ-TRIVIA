package com.datn.quizai.chat.service;

import com.datn.quizai.ai.provider.AiOrchestrator;
import com.datn.quizai.ai.repository.LearningMaterialRepository;
import com.datn.quizai.ai.repository.MaterialChunkRepository;
import com.datn.quizai.chat.domain.ChatMessage;
import com.datn.quizai.chat.domain.ChatRole;
import com.datn.quizai.chat.domain.ChatSession;
import com.datn.quizai.chat.repository.ChatMessageRepository;
import com.datn.quizai.chat.repository.ChatSessionRepository;
import com.datn.quizai.user.domain.Role;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Kho học liệu rỗng thì <b>không được gọi mô hình</b>.
 *
 * <h3>Vì sao kiểm bằng unit test chứ không trong {@code ChatIntegrationTest}</h3>
 * Điều kiện "kho rỗng" tính cả tài liệu <i>người khác đã chia sẻ</i>, mà tài liệu chia sẻ là dùng
 * chung toàn hệ thống. Trong một lớp test dùng chung một CSDL, chỉ cần một ca test khác chia sẻ tài
 * liệu là điều kiện này không còn dựng lại được — và phép kiểm sẽ đúng hay sai tuỳ thứ tự chạy. Đó
 * đúng là loại phụ thuộc mà chú thích sẵn có trong {@code shouldTellModelWhenNoContextFound} đã cảnh
 * báo. Nên phần quyết định được kiểm ở đây, nơi dựng được đúng trạng thái cần dựng.
 */
class ChatKhoRongTest {

    private AiOrchestrator aiOrchestrator;
    private LearningMaterialRepository materialRepository;
    private MaterialChunkRepository chunkRepository;
    private ChatSessionRepository sessionRepository;
    private ChatMessageRepository messageRepository;
    private ChatMessageWriter messageWriter;
    private ChatService chatService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        aiOrchestrator = mock(AiOrchestrator.class);
        materialRepository = mock(LearningMaterialRepository.class);
        chunkRepository = mock(MaterialChunkRepository.class);
        sessionRepository = mock(ChatSessionRepository.class);
        messageRepository = mock(ChatMessageRepository.class);
        messageWriter = mock(ChatMessageWriter.class);
        UserRepository userRepository = mock(UserRepository.class);

        User user = new User();
        user.setRole(Role.LEARNER);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(sessionRepository.save(any(ChatSession.class)))
                .willAnswer(goi -> goi.getArgument(0));

        chatService = new ChatService(aiOrchestrator, chunkRepository, materialRepository,
                sessionRepository, messageRepository, userRepository, messageWriter);
    }

    @Test
    @DisplayName("Kho rỗng: KHÔNG nhúng câu hỏi và KHÔNG gọi mô hình — kết quả đã biết trước")
    void khoRongThiKhongGoiAi() {
        given(materialRepository.hasAskable(userId)).willReturn(false);

        ChatService.Prepared prepared = chatService.prepare(userId, null, null, "Định lý Pytago?");

        assertThat(prepared.canGoiAi()).isFalse();
        // Cả hai đường đều tốn tiền: `embed` cho câu hỏi, `stream` cho câu trả lời. Đường đi cũ gọi cả
        // hai chỉ để nghe mô hình nói đúng cái câu mà prompt đã bắt nó nói.
        verifyNoInteractions(aiOrchestrator);
        verify(chunkRepository, never()).searchSimilarIncludingShared(any(), any(), anyList(), anyInt());
    }

    @Test
    @DisplayName("Câu trả lời dựng sẵn vẫn được LƯU vào lịch sử, không phải chỉ hiện rồi mất")
    void khoRongVanLuuCauTraLoi() {
        given(materialRepository.hasAskable(userId)).willReturn(false);

        chatService.prepare(userId, null, null, "Định lý Pytago?");

        ArgumentCaptor<ChatMessage> daLuu = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository, org.mockito.Mockito.atLeast(2)).save(daLuu.capture());

        assertThat(daLuu.getAllValues())
                .as("phiên phải có cả câu hỏi lẫn câu trả lời; thiếu câu trả lời thì người dùng mở "
                        + "lại lịch sử và thấy một câu hỏi treo lơ lửng")
                .anyMatch(m -> m.getRole() == ChatRole.ASSISTANT);

        // Lưu bằng `messageRepository` trong CHÍNH transaction này, không qua `messageWriter`:
        // `saveAnswer` là REQUIRES_NEW nên transaction mới của nó không thấy phiên vừa tạo ở đây, rồi
        // nuốt lỗi theo đúng thiết kế — câu trả lời sẽ biến mất không một tiếng động.
        verifyNoInteractions(messageWriter);
    }

    @Test
    @DisplayName("Câu trả lời dựng sẵn nói rõ là THIẾU DỮ LIỆU, và chỉ ra việc cần làm")
    void cauTraLoiNoiRoNguyenNhan() {
        given(materialRepository.hasAskable(userId)).willReturn(false);

        ChatService.Prepared prepared = chatService.prepare(userId, null, null, "Định lý Pytago?");

        // Một lời từ chối trống khiến người dùng tự quy kết cho mình — tưởng câu hỏi sai, hoặc tưởng
        // trợ lý hỏng. Cả hai đều dẫn họ đi sai hướng so với việc thật sự cần làm.
        assertThat(prepared.traLoiSan())
                .contains("trống")
                .contains("nạp");
        assertThat(prepared.sources()).isEmpty();
    }

    @Test
    @DisplayName("Có tài liệu hỏi được thì vẫn đi đường cũ — chặn sớm không được lấn sang trường hợp thường")
    void coHocLieuThiVanGoiAi() {
        given(materialRepository.hasAskable(userId)).willReturn(true);
        given(aiOrchestrator.embed(anyString(), any())).willReturn(java.util.List.of(0.1f));

        ChatService.Prepared prepared = chatService.prepare(userId, null, null, "Định lý Pytago?");

        assertThat(prepared.canGoiAi()).isTrue();
        assertThat(prepared.traLoiSan()).isNull();
        verify(aiOrchestrator).embed(anyString(), any());
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
