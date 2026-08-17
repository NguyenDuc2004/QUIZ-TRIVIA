package com.datn.quizai.notification.service;

import com.datn.quizai.gamification.service.BadgeEarnedEvent;
import com.datn.quizai.gamification.service.LevelUpEvent;
import com.datn.quizai.notification.domain.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Biến sự kiện thành tích thành thông báo (features/16 + features/13 FR-53).
 * <p>
 * FR-53 là món nợ tính năng 13 để lại: lát cắt đó ghi "cần tính năng 16". Đây là chỗ trả.
 * <p>
 * <b>Không</b> đặt {@code @Transactional(REQUIRES_NEW)} ở đây như các listener khác trong dự án:
 * {@link NotificationService#taoNeuChuaCo} đã tự mở transaction riêng, nên thêm một lớp nữa chỉ tạo hai
 * transaction lồng nhau cho một lần ghi. Đó cũng là lý do lớp này không có {@code try/catch}: ngoại lệ trong
 * listener sau-commit không quay lại được luồng nghiệp vụ, và {@code taoNeuChuaCo} đã nuốt đúng loại ngoại
 * lệ cần nuốt.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Lên cấp.
     * <p>
     * Khoá chống trùng là {@code level:{cấp}} — <b>không</b> kèm ngày. Một người chỉ lên cấp 5 đúng một lần
     * trong đời, nên khoá theo cấp là khoá tự nhiên. Kèm ngày vào thì mất tác dụng chống trùng ở đúng trường
     * hợp cần nó: tính lại XP hay chạy lại một job cũ sẽ gửi thêm một thông báo "bạn đã lên cấp 5".
     */
    @TransactionalEventListener
    public void onLevelUp(LevelUpEvent event) {
        notificationService.taoNeuChuaCo(
                event.userId(),
                NotificationType.ACHIEVEMENT,
                "Bạn đã lên cấp %d".formatted(event.capMoi()),
                "Từ cấp %d lên cấp %d. Tiếp tục giữ nhịp học nhé.".formatted(event.capCu(), event.capMoi()),
                """
                {"kind":"LEVEL_UP","level":%d}""".formatted(event.capMoi()),
                "level:" + event.capMoi());

        log.debug("Đã tạo thông báo lên cấp {} cho {}", event.capMoi(), event.userId());
    }

    /** Mở khoá huy hiệu. Khoá chống trùng là mã huy hiệu — cũng chỉ mở khoá được một lần. */
    @TransactionalEventListener
    public void onBadgeEarned(BadgeEarnedEvent event) {
        notificationService.taoNeuChuaCo(
                event.userId(),
                NotificationType.ACHIEVEMENT,
                "Mở khoá huy hiệu: " + event.name(),
                "Xem huy hiệu ở trang Thành tích.",
                """
                {"kind":"BADGE","code":"%s"}""".formatted(event.code()),
                "badge:" + event.code());

        log.debug("Đã tạo thông báo huy hiệu {} cho {}", event.code(), event.userId());
    }
}
