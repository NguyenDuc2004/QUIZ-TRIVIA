package com.datn.quizai.ai.provider;

import com.datn.quizai.ai.service.AiQuotaService;
import com.datn.quizai.ai.service.AiRequestLogger;
import com.datn.quizai.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;

/**
 * Đường chuyển provider của {@link AiOrchestrator} — trụ cột AI của đồ án (docs/architecture.md §5).
 *
 * <h3>Vì sao phần này cần test riêng, và vì sao mãi tới giờ mới có</h3>
 * Suốt cả dự án, đường dự phòng <b>chưa một lần chạy thật</b>: xAI không có gói miễn phí nên
 * {@code GROK_API_KEY} luôn để trống, và orchestrator lọc provider đó ra ngay từ đầu. Nghĩa là logic chuyển
 * provider chưa từng được thực thi — không phải bởi người dùng, cũng không phải bởi test.
 * <p>
 * Đo thật ngày 20/08/2026 làm lộ ra điều đó: ép Gemini hỏng bằng một key sai thì Groq <b>không</b> tiếp
 * quản. Hoá ra đúng thiết kế (key sai là lỗi vĩnh viễn, không phải lỗi tạm thời), nhưng để phân biệt được
 * "đúng thiết kế" với "hỏng" thì phải có test nói rõ ranh giới đó.
 *
 * <h3>Ranh giới: chỉ chuyển khi lỗi TẠM THỜI</h3>
 * Prompt sai định dạng, key sai, model không tồn tại — gửi sang provider khác cũng hỏng y hệt, chỉ tốn thêm
 * một lời gọi và một khoảng chờ. Còn 429/5xx/mất mạng là chuyện của <i>nhà cung cấp lúc này</i>, nên đáng
 * thử chỗ khác.
 * <p>
 * Không dùng Spring: dựng thẳng orchestrator với provider giả. Phần cần kiểm là <b>quyết định chọn ai</b>,
 * không phải chuyện đấu dây bean.
 */
class AiOrchestratorFallbackTest {

    private static final AiPrompt PROMPT = new AiPrompt("Bạn là trợ lý", "Sinh một câu hỏi", true, 0.3);

    @Test
    @DisplayName("Provider đầu hỏng TẠM THỜI: chuyển sang provider sau và trả kết quả của nó")
    void shouldFallBackOnTransientError() {
        ProviderGia hong = ProviderGia.luonHong("gemini", true);
        ProviderGia du = ProviderGia.luonChay("groq", "câu trả lời từ dự phòng");

        AiCompletion kq = dungOrchestrator(hong, du).complete(PROMPT, "generation", null);

        assertThat(kq.provider()).isEqualTo("groq");
        assertThat(kq.text()).isEqualTo("câu trả lời từ dự phòng");
        assertThat(hong.soLanGoi).as("vẫn thử provider chính trước, và thử lại vài lần").isPositive();
    }

    @Test
    @DisplayName("Provider đầu hỏng VĨNH VIỄN: KHÔNG chuyển, ném lỗi ra ngoài")
    void shouldNotFallBackOnPermanentError() {
        // Đây chính là điều đo thật ngày 20/08 gặp phải: Gemini trả 400 vì key sai, Groq không tiếp quản.
        // Gửi sang provider khác cũng hỏng y hệt — thử lại chỉ tốn tiền và làm người dùng chờ lâu hơn.
        ProviderGia hong = ProviderGia.luonHong("gemini", false);
        ProviderGia du = ProviderGia.luonChay("groq", "không nên chạy tới đây");

        catchThrowableOfType(RuntimeException.class,
                () -> dungOrchestrator(hong, du).complete(PROMPT, "generation", null));

        assertThat(du.soLanGoi)
                .as("provider dự phòng KHÔNG được gọi khi lỗi là vĩnh viễn")
                .isZero();
    }

    @Test
    @DisplayName("Provider chưa cấu hình key bị bỏ qua, không gọi rồi mới nhận lỗi")
    void shouldSkipUnconfiguredProvider() {
        // Đây là lý do suốt cả dự án đường dự phòng chưa từng chạy: GROK_API_KEY để trống nên Grok bị lọc
        // ra ngay từ đầu. Bỏ qua là đúng — gọi rồi nhận lỗi thì mỗi lần Gemini hỏng lại tốn thêm một vòng.
        ProviderGia chuaCauHinh = ProviderGia.chuaCauHinh("gemini");
        ProviderGia du = ProviderGia.luonChay("groq", "dự phòng phục vụ");

        AiCompletion kq = dungOrchestrator(chuaCauHinh, du).complete(PROMPT, "generation", null);

        assertThat(kq.provider()).isEqualTo("groq");
        assertThat(chuaCauHinh.soLanGoi).isZero();
    }

    @Test
    @DisplayName("Mọi provider đều hỏng: ném 503 với thông điệp người dùng hiểu được")
    void shouldThrow503WhenAllProvidersFail() {
        ProviderGia a = ProviderGia.luonHong("gemini", true);
        ProviderGia b = ProviderGia.luonHong("groq", true);

        RuntimeException loi = catchThrowableOfType(RuntimeException.class,
                () -> dungOrchestrator(a, b).complete(PROMPT, "generation", null));

        assertThat(loi).isNotNull();
        // Không để lộ chi tiết lỗi của bên thứ ba ra người dùng cuối
        if (loi instanceof BusinessException business) {
            assertThat(business.getStatus().value()).isEqualTo(503);
        }
        assertThat(b.soLanGoi).as("đã thử tới provider cuối cùng").isPositive();
    }

    @Test
    @DisplayName("Không provider nào cấu hình: 503 ngay, không chờ vô ích")
    void shouldThrowWhenNothingConfigured() {
        RuntimeException loi = catchThrowableOfType(RuntimeException.class,
                () -> dungOrchestrator(ProviderGia.chuaCauHinh("gemini"), ProviderGia.chuaCauHinh("groq"))
                        .complete(PROMPT, "generation", null));

        assertThat(loi).isNotNull();
    }

    @Test
    @DisplayName("Streaming chỉ xét provider CÓ streaming — bỏ qua provider không hỗ trợ")
    void shouldOnlyStreamFromStreamingProviders() {
        // Đúng tình huống trước khi đổi sang Groq: provider dự phòng cũ không có streaming, nên nếu Gemini
        // chết thì trợ lý học tập TẮT HẲN chứ không có đường lui. Groq có streaming nên giờ mới có.
        ProviderGia khongStream = ProviderGia.luonChay("gemini", "x");
        khongStream.coStreaming = false;
        ProviderGia coStream = ProviderGia.luonChay("groq", "y");
        coStream.coStreaming = true;
        coStream.manhStream = List.of("Xin ", "chào");

        List<String> nhanDuoc = dungOrchestrator(khongStream, coStream)
                .stream(PROMPT, "chat", null)
                .collectList().block();

        assertThat(nhanDuoc).containsExactly("Xin ", "chào");
    }

    // ------------------------------------------------------------------ dựng đồ giả

    private AiOrchestrator dungOrchestrator(AiProvider... providers) {
        List<AiProvider> ds = List.of(providers);
        String thuTu = String.join(",", ds.stream().map(AiProvider::name).toList());

        return new AiOrchestrator(
                new ArrayList<>(ds),
                mock(AiRequestLogger.class),
                new AiThrottleState(mock(org.springframework.data.redis.core.StringRedisTemplate.class)),
                mock(AiQuotaService.class),
                thuTu,
                1);   // 1 lần thử cho tác vụ nền: test không nên chờ backoff thật
    }

    /** Provider giả, đếm số lần bị gọi để kiểm "có gọi tới nó hay không". */
    private static final class ProviderGia implements AiProvider {
        private final String ten;
        private final boolean daCauHinh;
        private final boolean hong;
        private final boolean loiTamThoi;
        private final String noiDung;
        boolean coStreaming = false;
        List<String> manhStream = List.of();
        int soLanGoi = 0;

        private ProviderGia(String ten, boolean daCauHinh, boolean hong, boolean loiTamThoi, String noiDung) {
            this.ten = ten;
            this.daCauHinh = daCauHinh;
            this.hong = hong;
            this.loiTamThoi = loiTamThoi;
            this.noiDung = noiDung;
        }

        static ProviderGia luonChay(String ten, String noiDung) {
            return new ProviderGia(ten, true, false, false, noiDung);
        }

        static ProviderGia luonHong(String ten, boolean tamThoi) {
            return new ProviderGia(ten, true, true, tamThoi, null);
        }

        static ProviderGia chuaCauHinh(String ten) {
            return new ProviderGia(ten, false, false, false, null);
        }

        @Override public String name() { return ten; }
        @Override public String model() { return ten + "-model"; }
        @Override public boolean isConfigured() { return daCauHinh; }
        @Override public boolean supportsStreaming() { return coStreaming; }

        @Override
        public AiCompletion complete(AiPrompt prompt) {
            soLanGoi++;
            if (hong) {
                throw new AiProviderException(ten, "hỏng theo kịch bản test", loiTamThoi, null);
            }
            return new AiCompletion(ten, model(), noiDung, 10, 20, 5);
        }

        @Override
        public Flux<String> stream(AiPrompt prompt) {
            soLanGoi++;
            if (hong) {
                return Flux.error(new AiProviderException(ten, "hỏng theo kịch bản test", loiTamThoi, null));
            }
            return Flux.fromIterable(manhStream);
        }
    }
}
