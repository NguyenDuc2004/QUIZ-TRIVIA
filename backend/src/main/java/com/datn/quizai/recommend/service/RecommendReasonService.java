package com.datn.quizai.recommend.service;

import com.datn.quizai.ai.provider.AiCompletion;
import com.datn.quizai.ai.provider.AiOrchestrator;
import com.datn.quizai.ai.provider.AiPrompt;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.recommend.dto.RecommendedQuizResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Giải thích lý do gợi ý bằng ngôn ngữ tự nhiên (features/07, FR-36).
 *
 * <h3>Vì sao BẤM MỚI GỌI, không tự chạy khi mở trang</h3>
 * Đặc tả ghi FR-36 là "LLM giải thích lý do gợi ý", và cách làm hiển nhiên là sinh lời giải thích cho cả
 * danh sách ngay khi người dùng mở trang. Không làm vậy, vì hai lý do — cái thứ hai chỉ mới xuất hiện:
 * <ol>
 *   <li>Mười thẻ gợi ý là <b>mười lời gọi mô hình</b> cho một lần lướt qua, mà phần lớn thẻ người dùng
 *       không hề quan tâm.</li>
 *   <li><b>Từ khi có hạn mức AI theo người (FR-84)</b>, những lời gọi đó tiêu vào hạn mức của <i>chính
 *       người học</i>. Mở trang gợi ý ba lần là hết lượt sinh đề của họ — họ bị phạt vì một tính năng
 *       họ không chủ động dùng.</li>
 * </ol>
 * Nên mỗi thẻ luôn có sẵn <b>lý do dạng mẫu</b> (dựng từ dữ liệu đồ thị, không tốn gì), và lời giải thích
 * của mô hình chỉ sinh khi người dùng bấm hỏi.
 *
 * <h3>Cache để lần hỏi thứ hai không tính tiền lần nữa</h3>
 * Cùng một người hỏi cùng một quiz thì câu trả lời không đổi trong ngày. Cache ở Redis theo
 * {@code (userId, quizId)}; mất cache thì chỉ tốn thêm một lời gọi, nên không cần bền vững.
 *
 * <h3>Mô hình DIỄN ĐẠT LẠI dữ kiện, không được nghĩ thêm</h3>
 * Prompt chỉ đưa vào những gì đồ thị thật sự biết: chủ đề đang yếu, số người tương tự đã làm, số lượt làm.
 * Mô hình bịa ra một lý do nghe hay (*"quiz này được đánh giá 4.8 sao"*) là **nói dối về dữ liệu không tồn
 * tại** — đúng điều mà features/07 đã bỏ `rating` để tránh. Ràng buộc đó nằm trong system prompt, và câu
 * trả lời rỗng thì trả về lý do mẫu chứ không trả chuỗi trống.
 */
@Service
public class RecommendReasonService {

    private static final Logger log = LoggerFactory.getLogger(RecommendReasonService.class);

    private static final String KEY_PREFIX = "recommendreason:";
    private static final Duration TTL = Duration.ofHours(24);

    /** Đủ cho hai tới ba câu; dài hơn thì không ai đọc trên một tấm thẻ gợi ý. */
    private static final int TOI_DA_KY_TU = 300;

    private static final String CHI_DAN_HE_THONG = """
            Bạn giải thích cho người học VÌ SAO hệ thống gợi ý một quiz cho họ.

            QUY TẮC BẮT BUỘC:
            - Chỉ dùng những dữ kiện được cung cấp. TUYỆT ĐỐI không thêm thông tin nào khác.
            - Không bịa đánh giá, số sao, mức độ phổ biến, tên giảng viên, hay bất kỳ con số nào không có
              trong dữ kiện.
            - Nếu dữ kiện ít, hãy nói ngắn — không kéo dài bằng câu sáo rỗng.
            - Viết tiếng Việt, xưng "bạn", 2-3 câu, giọng thân thiện và cụ thể.
            - Không mở đầu bằng "Chúng tôi gợi ý" hay "Hệ thống đề xuất" — vào thẳng lý do.
            """;

    /** Đủ rộng để chắc chắn phủ hết danh sách người dùng đang nhìn. */
    private static final int SO_GOI_Y_TOI_DA = 20;

    private final AiOrchestrator orchestrator;
    private final RecommendationService recommendationService;
    private final StringRedisTemplate redis;

    public RecommendReasonService(AiOrchestrator orchestrator,
                                  RecommendationService recommendationService,
                                  StringRedisTemplate redis) {
        this.orchestrator = orchestrator;
        this.recommendationService = recommendationService;
        this.redis = redis;
    }

    /**
     * Giải thích vì sao quiz này được gợi ý cho người này.
     *
     * <h4>Tra danh sách gợi ý THẬT thay vì tin quizId từ URL</h4>
     * Nếu nhận thẳng {@code quizId} làm dữ kiện thì bất kỳ ai cũng bảo hệ thống *"giải thích vì sao gợi ý
     * quiz X cho tôi"* với một quiz chưa từng được gợi ý — và mô hình sẽ bịa ra một lý do nghe rất thuyết
     * phục cho một điều không có thật. Dữ kiện phải đến từ đồ thị, không từ tham số người dùng gửi lên.
     */
    public String giaiThich(UUID quizId, UUID userId) {
        List<RecommendedQuizResponse> danhSach =
                recommendationService.recommendQuizzes(userId, SO_GOI_Y_TOI_DA).items();

        RecommendedQuizResponse quiz = danhSach.stream()
                .filter(q -> q.quizId().equals(quizId))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound(
                        "Quiz này không nằm trong danh sách gợi ý hiện tại của bạn"));

        return giaiThich(quiz, userId);
    }

    /**
     * @param quiz   thẻ gợi ý đã dựng — <b>nguồn dữ kiện duy nhất</b> đưa vào prompt
     * @param userId người hỏi; lời gọi tính vào hạn mức AI của họ (FR-84)
     * @return lời giải thích của mô hình, hoặc lý do mẫu nếu mô hình trả về rỗng
     */
    public String giaiThich(RecommendedQuizResponse quiz, UUID userId) {
        String key = KEY_PREFIX + userId + ":" + quiz.quizId();

        String daCo = redis.opsForValue().get(key);
        if (daCo != null) {
            return daCo;
        }

        AiCompletion ketQua = orchestrator.complete(
                new AiPrompt(CHI_DAN_HE_THONG, duLieu(quiz), false, 0.5),
                "recommend-reason", userId);

        String loiGiai = rutGon(ketQua.text());
        if (loiGiai.isBlank()) {
            // Mô hình trả rỗng thì giữ lý do mẫu — thẻ gợi ý không bao giờ được để trống chỗ này, vì gợi ý
            // không nói lý do thì người dùng không có căn cứ để tin hay bỏ qua.
            log.warn("Mô hình trả lời rỗng khi giải thích gợi ý quiz {}", quiz.quizId());
            return quiz.reason();
        }

        redis.opsForValue().set(key, loiGiai, TTL);
        return loiGiai;
    }

    /**
     * Dựng phần dữ kiện của prompt — <b>chỉ những gì đồ thị thật sự biết</b>.
     * <p>
     * Package-private để test kiểm được rằng không có dữ kiện nào bịa ra lọt vào đây. Đây là ranh giới
     * chống ảo giác: mô hình không thể nói về thứ nó không được cho biết.
     */
    static String duLieu(RecommendedQuizResponse quiz) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tên quiz: ").append(quiz.title()).append('\n');

        List<String> yeu = quiz.weakTopics();
        if (yeu != null && !yeu.isEmpty()) {
            sb.append("Chủ đề người học đang làm sai nhiều: ").append(String.join(", ", yeu)).append('\n');
        }
        if (quiz.peerCount() > 0) {
            sb.append("Số người có cách học tương tự đã làm quiz này: ").append(quiz.peerCount()).append('\n');
        }
        if (quiz.attemptCount() > 0) {
            sb.append("Tổng số lượt làm quiz này: ").append(quiz.attemptCount()).append('\n');
        }
        sb.append("Nguồn gợi ý: ").append(switch (quiz.source()) {
            case WEAK_TOPIC -> "quiz chạm vào chủ đề người học đang yếu";
            case SIMILAR_LEARNERS -> "người có hành vi học giống người này đã làm";
            case NEW_TOPIC -> "chủ đề người học chưa từng luyện";
        }).append('\n');

        return sb.toString();
    }

    /** Cắt bớt và bỏ dấu nháy bao ngoài mà mô hình hay thêm vào. */
    private String rutGon(String text) {
        if (text == null) {
            return "";
        }
        String s = text.trim();
        if (s.length() > 1 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s.length() <= TOI_DA_KY_TU ? s : s.substring(0, TOI_DA_KY_TU).trim() + "…";
    }
}
