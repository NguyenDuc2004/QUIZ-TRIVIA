package com.datn.quizai.notification.service;

import com.datn.quizai.notification.domain.Notification;
import com.datn.quizai.notification.domain.NotificationSettings;
import com.datn.quizai.notification.domain.NotificationType;
import com.datn.quizai.notification.repository.NotificationRepository;
import com.datn.quizai.notification.repository.NotificationSettingsRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Tạo, đọc và đánh dấu thông báo (features/16, FR-65 & FR-68).
 *
 * <h3>Hai chốt mà mọi lời gọi tạo thông báo đều đi qua</h3>
 * <ol>
 *   <li><b>Cài đặt của người dùng</b> — tắt loại nào thì không tạo dòng nào cho loại đó. Chặn ở lúc
 *       <i>tạo</i> chứ không lọc lúc <i>đọc</i>: lọc lúc đọc thì bảng vẫn phình theo thứ người dùng đã nói
 *       là không muốn, và một ngày nào đó có ai viết một truy vấn khác quên mất bộ lọc.</li>
 *   <li><b>Khoá chống trùng</b> — cùng khoá thì không tạo dòng thứ hai, và chốt nằm ở ràng buộc duy nhất của
 *       cơ sở dữ liệu. Xem {@link #taoNeuChuaCo}.</li>
 * </ol>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final NotificationSettingsRepository settingsRepository;
    private final NotificationPusher pusher;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository repository,
                               NotificationSettingsRepository settingsRepository,
                               NotificationPusher pusher,
                               ObjectMapper objectMapper) {
        this.repository = repository;
        this.settingsRepository = settingsRepository;
        this.pusher = pusher;
        this.objectMapper = objectMapper;
    }

    /**
     * Tạo thông báo nếu chưa có, rồi đẩy real-time.
     *
     * <h3>Vì sao chạy trong transaction RIÊNG</h3>
     * Người gọi chủ yếu là listener {@code @TransactionalEventListener}, chạy <b>sau khi</b> transaction
     * nghiệp vụ đã commit. Ở thời điểm đó không còn transaction nào để tham gia, nên propagation mặc định
     * ({@code REQUIRED}) sẽ cố nối vào một transaction đã hoàn tất. {@code REQUIRES_NEW} mở một transaction
     * mới — cùng lý do như các listener khác trong dự án.
     *
     * <h3>Chống trùng KHÔNG đi qua ngoại lệ</h3>
     * Xem {@link NotificationRepository#chenNeuChuaCo} — trùng khoá là đường chạy bình thường của một job
     * hằng ngày, nên nó được xử lý bằng {@code ON CONFLICT DO NOTHING} thay vì bắt
     * {@code DataIntegrityViolationException}. Hai lý do cụ thể tại sao cách bắt ngoại lệ không chạy được
     * nằm trong javadoc của phương thức đó.
     *
     * @param dedupeKey khoá chống trùng; {@code null} nếu thông báo này được phép gửi lại
     * @return thông báo vừa tạo, hoặc {@code empty} khi bị chặn bởi cài đặt hoặc đã tồn tại
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Notification> taoNeuChuaCo(UUID userId, NotificationType type, String title,
                                               String body, String data, String dedupeKey) {
        if (biTat(userId, type)) {
            return Optional.empty();
        }

        UUID id = UUID.randomUUID();
        int daChen = repository.chenNeuChuaCo(id, userId, type.name(), title, body, data, dedupeKey);

        if (daChen == 0) {
            log.debug("Bỏ qua thông báo trùng: user={} key={}", userId, dedupeKey);
            return Optional.empty();
        }

        // Đọc lại để lấy `created_at` do cơ sở dữ liệu sinh, thay vì đoán bằng đồng hồ của tiến trình này:
        // hai đồng hồ lệch nhau thì thứ tự thông báo trên giao diện lệch theo. Một SELECT thêm, và chỉ ở
        // đường đã chèn thành công.
        Optional<Notification> vuaTao = repository.findById(id);

        // Đẩy SAU khi đã chắc chắn có trong cơ sở dữ liệu: đẩy trước thì người dùng thấy một thông báo
        // không có trong danh sách của họ
        vuaTao.ifPresent(n -> pusher.day(userId, n));
        return vuaTao;
    }

    @Transactional(readOnly = true)
    public Page<Notification> danhSach(UUID userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long soChuaDoc(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    /**
     * Đánh dấu một thông báo đã đọc.
     * <p>
     * Thông báo của người khác thì coi như không tồn tại: không ném lỗi, chỉ không làm gì. Ở đây không có gì
     * để "báo sai" cho người gọi — họ không mất dữ liệu, và một mã lỗi chỉ nói cho người dò id biết là id đó
     * có thật.
     */
    @Transactional
    public void danhDauDaDoc(UUID notificationId, UUID userId) {
        repository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .ifPresent(n -> n.setRead(true));
    }

    @Transactional
    public int danhDauTatCaDaDoc(UUID userId) {
        return repository.markAllRead(userId);
    }

    // ------------------------------------------------------------------ cài đặt

    @Transactional(readOnly = true)
    public Set<NotificationType> loaiBiTat(UUID userId) {
        return settingsRepository.findById(userId)
                .map(s -> doc(s.getDisabledTypes()))
                .orElseGet(() -> EnumSet.noneOf(NotificationType.class));
    }

    /**
     * Đặt lại danh sách loại bị tắt.
     * <p>
     * Loại không tắt được ({@code SYSTEM}) bị <b>bỏ qua trong im lặng</b> thay vì trả lỗi: người dùng không
     * gửi được yêu cầu đó từ giao diện, nên nếu nó tới thì đó là client hỏng hoặc gọi tay. Trả 400 cho một
     * việc mà kết quả cuối cùng vẫn đúng ("SYSTEM vẫn bật") chỉ thêm một nhánh lỗi phải xử lý ở cả hai đầu.
     */
    @Transactional
    public Set<NotificationType> capNhatCaiDat(UUID userId, Set<NotificationType> tat) {
        Set<NotificationType> loc = tat.stream()
                .filter(NotificationType::tatDuoc)
                .collect(java.util.stream.Collectors.toCollection(
                        () -> EnumSet.noneOf(NotificationType.class)));

        NotificationSettings settings = settingsRepository.findById(userId)
                .orElseGet(() -> new NotificationSettings(userId));
        settings.setDisabledTypes(viet(loc));
        settingsRepository.save(settings);
        return loc;
    }

    // ------------------------------------------------------------------ nội bộ

    private boolean biTat(UUID userId, NotificationType type) {
        return type.tatDuoc() && loaiBiTat(userId).contains(type);
    }

    /**
     * Đọc mảng jsonb thành tập enum.
     * <p>
     * Giá trị lạ bị bỏ qua thay vì ném lỗi: nếu về sau có loại bị xoá khỏi enum thì dữ liệu cũ vẫn còn tên đó
     * trong cột, và làm vỡ cả trang cài đặt vì một tên không còn dùng là phản ứng thái quá.
     */
    private Set<NotificationType> doc(String json) {
        Set<NotificationType> ket = EnumSet.noneOf(NotificationType.class);
        if (json == null || json.isBlank()) {
            return ket;
        }
        try {
            List<String> ten = objectMapper.readValue(json, new TypeReference<>() {
            });
            for (String t : ten) {
                try {
                    ket.add(NotificationType.valueOf(t));
                } catch (IllegalArgumentException ignored) {
                    log.debug("Bỏ qua loại thông báo không còn dùng trong cài đặt: {}", t);
                }
            }
        } catch (Exception e) {
            log.warn("Không đọc được cài đặt thông báo, coi như bật tất cả: {}", e.getMessage());
        }
        return ket;
    }

    private String viet(Set<NotificationType> loai) {
        try {
            return objectMapper.writeValueAsString(loai.stream().map(Enum::name).toList());
        } catch (Exception e) {
            // Không thể xảy ra với một danh sách chuỗi, nhưng để mặc định an toàn là "bật tất cả"
            return "[]";
        }
    }
}
