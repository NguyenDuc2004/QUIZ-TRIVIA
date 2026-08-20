package com.datn.quizai.ai.provider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test hai phần đọc chuỗi của provider dự phòng <b>Groq</b> (groq.com — không phải Grok của xAI).
 *
 * <h3>Vì sao hai phần vặt này đáng có test riêng</h3>
 * Cả hai đều <b>hỏng trong im lặng</b>: không ném lỗi, không ghi log gì bất thường, chỉ làm cả tính năng
 * trông như "Groq lúc nào cũng lỗi".
 * <ul>
 *   <li>{@code bocManh} sai thì luồng streaming của trợ lý học tập chạy mà <i>không ra chữ nào</i> —
 *       người dùng thấy một ô trống, không có thông báo lỗi để mà đọc.</li>
 *   <li>{@code docRetryAfter} đọc nhầm đơn vị thì hệ thống chờ 2 mili-giây thay vì 2 giây, gọi lại,
 *       đâm vào hạn mức lần nữa — vòng lặp đó nhìn giống "provider dự phòng vô dụng".</li>
 * </ul>
 * Đây cũng là lần đầu đường dự phòng của dự án <b>chạy thật được</b> (xAI không có gói miễn phí), nên nó
 * cần test đúng như provider chính.
 */
class GroqProviderTest {

    /** Không cần key thật: chỉ gọi hàm đọc chuỗi, không chạm mạng. */
    private final GroqProvider provider = new GroqProvider(
            WebClient.builder(), "", "openai/gpt-oss-120b");

    // ------------------------------------------------------------------ bóc mảnh SSE

    @Test
    @DisplayName("Bóc được chữ từ mảnh SSE thật của Groq")
    void shouldExtractDeltaFromRealChunk() {
        String chunk = """
                {"id":"chatcmpl-1","object":"chat.completion.chunk","model":"openai/gpt-oss-120b",
                 "choices":[{"index":0,"delta":{"content":"Đạo hàm"},"finish_reason":null}]}
                """;

        assertThat(provider.bocManh(chunk)).isEqualTo("Đạo hàm");
    }

    @Test
    @DisplayName("Mảnh mở đầu chỉ có role thì bỏ qua, không phải lỗi")
    void shouldIgnoreRoleOnlyChunk() {
        // Mảnh đầu tiên của mọi luồng OpenAI-compatible. Coi nó là lỗi thì luồng đổ ngay từ chữ đầu.
        String chunk = """
                {"choices":[{"index":0,"delta":{"role":"assistant","content":""},"finish_reason":null}]}
                """;

        assertThat(provider.bocManh(chunk)).isNull();
    }

    @Test
    @DisplayName("Mảnh kết thúc chỉ có finish_reason và usage thì bỏ qua")
    void shouldIgnoreFinalChunk() {
        String chunk = """
                {"choices":[{"index":0,"delta":{},"finish_reason":"stop"}],
                 "usage":{"prompt_tokens":120,"completion_tokens":48}}
                """;

        assertThat(provider.bocManh(chunk)).isNull();
    }

    @Test
    @DisplayName("Dấu hết luồng [DONE] không phải JSON — bỏ qua chứ không ném lỗi")
    void shouldIgnoreDoneMarker() {
        // Nếu để nó rơi vào bộ đọc JSON thì mỗi câu trả lời kết thúc bằng một dòng log lỗi vô nghĩa
        assertThat(provider.bocManh("[DONE]")).isNull();
    }

    @Test
    @DisplayName("Mảnh hỏng định dạng thì bỏ, KHÔNG kéo đổ cả luồng")
    void shouldSwallowBrokenChunk() {
        // Người dùng thà thiếu vài chữ hơn mất cả câu trả lời đang chạy dở — cùng cách xử lý với Gemini
        assertThat(provider.bocManh("{ đây không phải JSON")).isNull();
    }

    // ------------------------------------------------------------------ retry-after

    @Test
    @DisplayName("Groq trả số GIÂY, phải đổi sang mili-giây")
    void shouldReadRetryAfterAsSeconds() {
        // Đọc nhầm thành mili-giây thì hệ thống chờ 2ms rồi gọi lại, đâm vào hạn mức lần nữa
        assertThat(GroqProvider.docRetryAfter("2")).isEqualTo(2_000);
    }

    @Test
    @DisplayName("Đọc được cả số thập phân")
    void shouldReadFractionalSeconds() {
        assertThat(GroqProvider.docRetryAfter("7.5")).isEqualTo(7_500);
        assertThat(GroqProvider.docRetryAfter(" 0.25 ")).isEqualTo(250);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Wed, 21 Oct 2026 07:28:00 GMT", "không-phải-số", "-3"})
    @DisplayName("Không đọc được thì trả 0 để orchestrator dùng khoảng lùi mặc định, KHÔNG ném lỗi")
    void shouldFallBackToZero(String header) {
        // Chuẩn HTTP cho phép retry-after là một mốc thời gian dạng chữ. Groq không dùng dạng đó, nhưng ném
        // lỗi ở đây thì một header lạ sẽ che mất lỗi 429 thật đang cần xử lý.
        assertThat(GroqProvider.docRetryAfter(header)).isZero();
    }

    @Test
    @DisplayName("Thiếu hẳn header cũng trả 0")
    void shouldHandleMissingHeader() {
        assertThat(GroqProvider.docRetryAfter(null)).isZero();
    }

    // ------------------------------------------------------------------ hợp đồng của provider

    @Test
    @DisplayName("Chưa có key thì coi như chưa cấu hình — orchestrator lọc ra thay vì gọi rồi lỗi")
    void shouldNotBeConfiguredWithoutKey() {
        assertThat(provider.isConfigured()).isFalse();
        assertThat(new GroqProvider(WebClient.builder(), "  ", "m").isConfigured())
                .as("key toàn khoảng trắng cũng là chưa cấu hình")
                .isFalse();
        assertThat(new GroqProvider(WebClient.builder(), "gsk_abc", "m").isConfigured()).isTrue();
    }

    @Test
    @DisplayName("CÓ streaming — đây là thứ xAI không có, và là lý do trợ lý học tập có đường lui")
    void shouldSupportStreaming() {
        assertThat(provider.supportsStreaming()).isTrue();
    }

    @Test
    @DisplayName("KHÔNG có embedding — nói rõ thay vì để lỗi lạ nổ ở tầng dưới")
    void shouldNotSupportEmbedding() {
        assertThat(provider.supportsEmbedding()).isFalse();

        // Gemini chết thì sinh đề vẫn chạy, nhưng NẠP HỌC LIỆU MỚI thì không. Ràng buộc thật, cần nói rõ.
        assertThat(org.assertj.core.api.Assertions
                .catchThrowableOfType(AiProviderException.class, () -> provider.embed("xin chào")))
                .hasMessageContaining("embedding");
    }

    @Test
    @DisplayName("Tên provider là 'groq' — phải khớp app.ai.provider-order và cột audit")
    void shouldBeNamedGroq() {
        // Sai tên thì AiOrchestrator lọc provider này ra khỏi danh sách trong im lặng: không lỗi,
        // không log, chỉ là fallback không bao giờ chạy.
        assertThat(provider.name()).isEqualTo("groq");
    }
}
