package com.datn.quizai.notification.service;

import com.datn.quizai.flashcard.repository.FlashcardReviewRepository;
import com.datn.quizai.notification.domain.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Nhắc ôn tập hằng ngày (features/16, FR-66).
 *
 * <h3>Chống gửi trùng nằm ở cơ sở dữ liệu, không ở khoá phân tán</h3>
 * Đặc tả gợi ý "cân nhắc khoá phân tán (Redis) nếu chạy nhiều instance để không gửi trùng". Ở đây chọn cách
 * khác và mạnh hơn: khoá chống trùng {@code srs:{ngày}} với ràng buộc duy nhất trên
 * {@code (user_id, dedupe_key)}. Khoá phân tán chỉ chặn <i>hai instance cùng lúc</i>; ràng buộc duy nhất chặn
 * <b>mọi</b> đường dẫn tới việc gửi trùng — hai instance cùng lúc, deploy lại giữa trưa, hay ai đó gọi tay để
 * thử. Và nó không thêm một thành phần nữa có thể chết.
 * <p>
 * Cái giá là job có thể chạy trùng và làm việc vô ích một lát; với vài trăm người dùng thì đó là một câu
 * {@code group by} chạy hai lần.
 *
 * <h3>Vì sao 7 giờ sáng</h3>
 * Nhắc ôn tập chỉ có nghĩa nếu tới vào lúc người ta có thể ôn. Nửa đêm thì đúng ranh giới ngày nhưng thông báo
 * nằm đó tới sáng, và lúc đọc thì nó đã là "hôm qua". Giờ này cố định theo múi giờ của máy chủ — hệ thống
 * không lưu múi giờ người dùng, và bịa ra một múi giờ mặc định thì sai với người thật ở múi khác.
 */
@Service
public class SrsReminderJob {

    private static final Logger log = LoggerFactory.getLogger(SrsReminderJob.class);

    private final FlashcardReviewRepository reviewRepository;
    private final NotificationService notificationService;

    public SrsReminderJob(FlashcardReviewRepository reviewRepository,
                          NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.notificationService = notificationService;
    }

    /** 7:00 mỗi ngày. {@code @EnableScheduling} bật ở {@code AsyncConfig} — thiếu nó thì job im lặng không chạy. */
    @Scheduled(cron = "0 0 7 * * *")
    public void chay() {
        int daGui = nhacOnTap(LocalDate.now());
        log.info("Job nhắc ôn tập: đã gửi {} thông báo", daGui);
    }

    /**
     * Tách khỏi {@link #chay()} để test gọi được với một ngày cụ thể, và để gọi lại được bằng tay.
     * <p>
     * Gọi hai lần cùng ngày là an toàn: lần thứ hai không tạo thông báo nào nhờ khoá chống trùng.
     *
     * @return số thông báo <b>thật sự</b> được tạo — không tính người đã nhận hôm nay và người đã tắt loại này
     */
    public int nhacOnTap(LocalDate ngay) {
        List<FlashcardReviewRepository.NguoiDenHanRow> canNhac =
                reviewRepository.demDenHanTheoNguoi(ngay);

        String khoa = "srs:" + ngay;
        int daGui = 0;

        for (var dong : canNhac) {
            boolean vuaTao = notificationService.taoNeuChuaCo(
                    dong.getUserId(),
                    NotificationType.SRS_REMINDER,
                    "Bạn có %d thẻ đến hạn ôn hôm nay".formatted(dong.getSoThe()),
                    "Ôn đúng hạn giúp bạn nhớ lâu hơn nhiều so với ôn dồn.",
                    """
                    {"kind":"SRS_DUE","soThe":%d}""".formatted(dong.getSoThe()),
                    khoa).isPresent();

            if (vuaTao) {
                daGui++;
            }
        }
        return daGui;
    }
}
