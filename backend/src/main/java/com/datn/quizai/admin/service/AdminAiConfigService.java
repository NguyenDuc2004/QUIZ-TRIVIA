package com.datn.quizai.admin.service;

import com.datn.quizai.admin.dto.AiConfigResponse;
import com.datn.quizai.ai.provider.AiOrchestrator;
import com.datn.quizai.ai.provider.AiProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Đọc trạng thái cấu hình AI cho khu quản trị (features/10, FR-83).
 * <p>
 * <b>Chỉ đọc, không có thao tác ghi</b> — và đó là thiết kế, không phải thiếu sót:
 * <ul>
 *   <li><b>Khoá API</b> chỉ đọc được ở dạng "đã cấu hình / để trống". Sửa khoá là việc của biến môi
 *       trường và người có quyền truy cập máy chủ; mở đường ghi qua API nghĩa là bất kỳ ai chiếm được
 *       một phiên quản trị cũng đổi được đích đến của mọi lời gọi AI.</li>
 *   <li><b>System prompt</b> không đọc và không ghi. Nó là nơi đặt bốn lớp chống tiêm chỉ thị khi chấm
 *       bài; đọc được qua API là bước đầu để sửa được nó.</li>
 *   <li><b>Hạn mức mỗi người dùng</b> chưa làm — xem ghi chú ở dưới.</li>
 * </ul>
 *
 * <h3>Vì sao chưa có đặt hạn mức (FR-84)</h3>
 * Thêm một ô nhập "mỗi Creator tối đa N lượt/ngày" thì làm được ngay, nhưng {@code AiOrchestrator} hiện
 * <b>không đọc con số đó</b> và cũng chưa đếm lượt gọi theo từng người dùng. Một ô nhập lưu được giá trị
 * mà không chặn được gì là thứ tệ hơn cả việc không có nó: quản trị viên tin rằng chi phí đã được
 * giới hạn, trong khi thực tế không. Làm đúng thì phải đếm lượt theo user ở Redis và chặn trong
 * {@code AiOrchestrator} — đó là một lát cắt riêng, đã ghi vào nợ.
 */
@Service
public class AdminAiConfigService {

    private final List<AiProvider> providers;
    private final AiOrchestrator orchestrator;
    private final String providerOrder;
    private final int maxAttemptsBackground;

    public AdminAiConfigService(List<AiProvider> providers,
                               AiOrchestrator orchestrator,
                               @Value("${app.ai.provider-order}") String providerOrder,
                               @Value("${app.ai.max-attempts-background:4}") int maxAttemptsBackground) {
        this.providers = providers;
        this.orchestrator = orchestrator;
        this.providerOrder = providerOrder;
        this.maxAttemptsBackground = maxAttemptsBackground;
    }

    public AiConfigResponse config() {
        List<String> thuTu = List.of(providerOrder.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        List<String> sanSang = orchestrator.availableProviders();

        List<AiConfigResponse.ProviderStatus> trangThai = providers.stream()
                .map(p -> new AiConfigResponse.ProviderStatus(
                        p.name(),
                        p.isConfigured(),
                        // Có khoá nhưng bị loại khỏi provider-order thì vẫn không được gọi — tách thành
                        // cờ riêng vì đây là nguyên nhân khó đoán nhất khi "AI không chạy dù đã có key"
                        sanSang.contains(p.name()) && thuTu.contains(p.name()),
                        p.supportsEmbedding(),
                        p.supportsStreaming()))
                .sorted((a, b) -> Integer.compare(indexOf(thuTu, a.ten()), indexOf(thuTu, b.ten())))
                .toList();

        return new AiConfigResponse(trangThai, thuTu, orchestrator.isAvailable(), maxAttemptsBackground);
    }

    /** Provider không nằm trong thứ tự ưu tiên thì xếp cuối, không phải xếp đầu. */
    private static int indexOf(List<String> order, String name) {
        int i = order.indexOf(name);
        return i < 0 ? Integer.MAX_VALUE : i;
    }
}
