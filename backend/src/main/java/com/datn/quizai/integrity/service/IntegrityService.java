package com.datn.quizai.integrity.service;

import com.datn.quizai.ai.provider.AiOrchestrator;
import com.datn.quizai.ai.provider.AiPrompt;
import com.datn.quizai.integrity.domain.AttemptIntegrity;
import com.datn.quizai.integrity.domain.ProctoringEvent;
import com.datn.quizai.integrity.domain.ProctoringEventType;
import com.datn.quizai.integrity.repository.AttemptIntegrityRepository;
import com.datn.quizai.integrity.repository.ProctoringEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tính điểm rủi ro cho một lượt thi và xin nhận định của mô hình (features/12, FR-45 và FR-46).
 *
 * <h3>Chỉ gọi AI cho bài bị gắn cờ</h3>
 * Gọi mô hình cho <b>mọi</b> lượt thi là tốn hạn mức cho hàng loạt bài sạch mà kết luận đã biết trước ("không
 * có tín hiệu nào"). Chỉ những bài vượt ngưỡng mới cần một đoạn diễn giải để người rà soát đọc nhanh.
 *
 * <h3>Prompt không chứa dữ liệu cá nhân</h3>
 * Chỉ gửi <b>số đếm theo loại tín hiệu</b> — không tên, không email, không id, không nội dung câu trả lời.
 * `security.md` cấm gửi PII sang nhà cung cấp bên ngoài, và ở đây thì số đếm là đủ để diễn giải.
 */
@Service
public class IntegrityService {

    private static final Logger log = LoggerFactory.getLogger(IntegrityService.class);

    private final ProctoringEventRepository eventRepository;
    private final AttemptIntegrityRepository integrityRepository;
    private final AiOrchestrator aiOrchestrator;
    private final ObjectMapper objectMapper;

    public IntegrityService(ProctoringEventRepository eventRepository,
                            AttemptIntegrityRepository integrityRepository,
                            AiOrchestrator aiOrchestrator,
                            ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.integrityRepository = integrityRepository;
        this.aiOrchestrator = aiOrchestrator;
        this.objectMapper = objectMapper;
    }

    /**
     * Tính lại điểm rủi ro và lưu bản tổng hợp.
     * <p>
     * Idempotent: một lượt thi có đúng một bản (khoá duy nhất trên {@code attempt_id}), nên gọi lại chỉ cập
     * nhật con số. <b>Không</b> ghi đè kết luận của người rà soát — nếu ai đó đã kết luận thì tính lại không
     * đưa bài trở về {@code PENDING}, vì như vậy là xoá công việc của họ.
     */
    @Transactional
    public AttemptIntegrity tinhLai(UUID attemptId, UUID userId) {
        List<ProctoringEvent> events = eventRepository.findByAttemptIdOrderByOccurredAt(attemptId);
        RiskScorer.KetQua ketQua = RiskScorer.tinh(events.stream().map(this::toTinHieu).toList());

        AttemptIntegrity integrity = integrityRepository.findByAttemptId(attemptId)
                .orElseGet(() -> {
                    AttemptIntegrity moi = new AttemptIntegrity();
                    moi.setAttemptId(attemptId);
                    return moi;
                });

        integrity.setRiskScore(ketQua.diem());
        integrity.setFlags(toJson(ketQua.co()));

        // Chỉ xin nhận định cho bài bị gắn cờ, và chỉ khi chưa có: gọi lại cho một bài đã có nhận định là tốn
        // hạn mức để nhận về gần đúng một đoạn văn cũ.
        if (ketQua.bịGanCo() && integrity.getAiNote() == null) {
            integrity.setAiNote(xinNhanDinh(ketQua, events));
        }

        return integrityRepository.save(integrity);
    }

    /**
     * Xin mô hình diễn giải chuỗi tín hiệu.
     *
     * @return {@code null} khi gọi thất bại — để {@code ai_note} vẫn là null, phân biệt được với "AI đã xem và
     *         không thấy gì". Lỗi AI không được làm vỡ việc lưu điểm rủi ro: điểm số là phần quan trọng, nhận
     *         định chỉ là phần đọc cho nhanh
     */
    private String xinNhanDinh(RiskScorer.KetQua ketQua, List<ProctoringEvent> events) {
        try {
            Map<ProctoringEventType, Integer> soLan = new EnumMap<>(ProctoringEventType.class);
            for (ProctoringEvent e : events) {
                soLan.merge(e.getEventType(), 1, Integer::sum);
            }

            StringBuilder soLieu = new StringBuilder();
            soLan.forEach((loai, lan) -> soLieu.append("- ").append(moTa(loai))
                    .append(": ").append(lan).append(" lần\n"));

            AiPrompt prompt = new AiPrompt(
                    """
                    Bạn là trợ lý giúp giáo viên rà soát tính toàn vẹn của một bài thi trực tuyến.
                    Nhiệm vụ: đọc số liệu tín hiệu hành vi và viết 2-3 câu tiếng Việt nhận định mức độ đáng
                    chú ý, kèm lý do.

                    Nguyên tắc bắt buộc:
                    - KHÔNG kết luận người học gian lận. Các tín hiệu này thu từ trình duyệt nên có thể bị
                      chặn hoặc giả mạo, và đều có cách giải thích vô hại (thông báo hệ thống bật lên, người
                      dùng đọc lại đề ở tab khác, bàn phím dán do gõ nhầm tổ hợp).
                    - Nêu cả cách giải thích vô hại nếu có, để giáo viên tự quyết định.
                    - Viết văn xuôi, không dùng danh sách, không nhắc lại nguyên số liệu.
                    - Không suy đoán về danh tính, hoàn cảnh hay ý định của người học.
                    """,
                    """
                    Điểm rủi ro hệ thống tính: %d/100 (ngưỡng đáng rà soát: %d).
                    Số liệu tín hiệu:
                    %s
                    Viết nhận định ngắn cho giáo viên.
                    """.formatted(ketQua.diem(), RiskScorer.NGUONG_GAN_CO, soLieu),
                    false, 0.3);

            // Không truyền userId: prompt này không chứa dữ liệu cá nhân, và ghi userId vào nhật ký AI của
            // một lượt rà soát là gắn một người vào một lời gọi mô hình mà không cần thiết.
            return aiOrchestrator.complete(prompt, "integrity-review", null).text().trim();

        } catch (Exception e) {
            log.warn("Không xin được nhận định AI cho lượt thi: {}", e.getMessage());
            return null;
        }
    }

    private static String moTa(ProctoringEventType loai) {
        return switch (loai) {
            case TAB_HIDDEN -> "Chuyển tab hoặc thu nhỏ cửa sổ";
            case WINDOW_BLUR -> "Cửa sổ mất focus";
            case COPY -> "Sao chép từ đề bài";
            case PASTE -> "Dán vào ô trả lời";
            case FULLSCREEN_EXIT -> "Thoát toàn màn hình";
            case ANSWER_TOO_FAST -> "Trả lời nhanh bất thường";
        };
    }

    private RiskScorer.TinHieu toTinHieu(ProctoringEvent event) {
        int doDai = 0;
        if (event.getEventType() == ProctoringEventType.PASTE && event.getDetail() != null) {
            try {
                doDai = objectMapper.readTree(event.getDetail()).path("length").asInt();
            } catch (Exception e) {
                // Chi tiết hỏng thì coi như đoạn ngắn, không bỏ cả sự kiện: việc dán vẫn đã xảy ra
                log.debug("Không đọc được detail của sự kiện {}", event.getId());
            }
        }
        return new RiskScorer.TinHieu(event.getEventType(), doDai);
    }

    private String toJson(List<String> co) {
        try {
            return objectMapper.writeValueAsString(co);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** Đọc lại danh sách cờ từ JSON. */
    public List<String> docCo(AttemptIntegrity integrity) {
        try {
            return objectMapper.readValue(integrity.getFlags(),
                    objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }
}
