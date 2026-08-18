package com.datn.quizai.classroom.service;

import com.datn.quizai.attempt.domain.AttemptStatus;
import com.datn.quizai.attempt.domain.QuizAttempt;
import com.datn.quizai.attempt.repository.QuizAttemptRepository;
import com.datn.quizai.classroom.domain.Assignment;
import com.datn.quizai.classroom.repository.AssignmentRepository;
import com.datn.quizai.classroom.repository.ClassroomMemberRepository;
import com.datn.quizai.notification.domain.NotificationType;
import com.datn.quizai.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Nhắc hạn nộp bài tập (features/14 + features/16, FR-65).
 * <p>
 * Đây là chỗ làm cho loại thông báo {@code ASSIGNMENT_DUE} — vốn đã khai sẵn trong ràng buộc {@code CHECK}
 * của V18 nhưng chưa có nguồn phát — trở thành thật. Khai sẵn rồi để không ai gửi là đúng cái đã tránh ở
 * FR-84; giờ tính năng 14 có mặt thì trả nốt.
 *
 * <h3>Chỉ nhắc người CHƯA nộp</h3>
 * Nhắc cả lớp thì em đã nộp từ tuần trước cũng nhận "bài sắp hết hạn" — một thông báo sai sự thật với chính
 * người nhận, và là kiểu làm người ta tắt thông báo vĩnh viễn.
 *
 * <h3>Chống gửi trùng vẫn là khoá ở cơ sở dữ liệu</h3>
 * Khoá {@code assignment:{id}} — không kèm ngày. Một bài tập chỉ có <b>một</b> hạn nộp, nên nhắc đúng một
 * lần là đủ; kèm ngày vào thì job chạy lại hôm sau sẽ nhắc tiếp cùng một bài.
 */
@Service
public class AssignmentDueReminderJob {

    private static final Logger log = LoggerFactory.getLogger(AssignmentDueReminderJob.class);

    /** Nhắc trước hạn bao lâu. Một ngày: đủ để làm kịp, chưa xa tới mức quên mất là đã được nhắc. */
    private static final int NHAC_TRUOC_GIO = 24;

    private final AssignmentRepository assignmentRepository;
    private final ClassroomMemberRepository memberRepository;
    private final QuizAttemptRepository attemptRepository;
    private final NotificationService notificationService;

    public AssignmentDueReminderJob(AssignmentRepository assignmentRepository,
                                    ClassroomMemberRepository memberRepository,
                                    QuizAttemptRepository attemptRepository,
                                    NotificationService notificationService) {
        this.assignmentRepository = assignmentRepository;
        this.memberRepository = memberRepository;
        this.attemptRepository = attemptRepository;
        this.notificationService = notificationService;
    }

    /** 7:05 mỗi ngày — ngay sau job nhắc ôn tập, để hai thông báo tới cùng một lúc thay vì rải rác. */
    @Scheduled(cron = "0 5 7 * * *")
    public void chay() {
        int daGui = nhacHanNop(OffsetDateTime.now());
        log.info("Job nhắc hạn nộp: đã gửi {} thông báo", daGui);
    }

    /**
     * Tách khỏi {@link #chay()} để test gọi được với một mốc thời gian cụ thể.
     * <p>
     * Gọi hai lần là an toàn: lần thứ hai không tạo thông báo nào nhờ khoá chống trùng.
     *
     * @return số thông báo <b>thật sự</b> được tạo
     */
    public int nhacHanNop(OffsetDateTime now) {
        var sapHetHan = assignmentRepository.findDueBetween(now, now.plusHours(NHAC_TRUOC_GIO));
        int daGui = 0;

        for (Assignment baiTap : sapHetHan) {
            Set<UUID> daNop = new HashSet<>();
            for (QuizAttempt luot : attemptRepository.findByAssignmentId(baiTap.getId())) {
                if (luot.getStatus() == AttemptStatus.SUBMITTED) {
                    daNop.add(luot.getUser().getId());
                }
            }

            for (var thanhVien : memberRepository.findByClassroomWithUser(baiTap.getClassroom().getId())) {
                UUID userId = thanhVien.getUser().getId();
                if (daNop.contains(userId)) {
                    continue;
                }

                boolean vuaTao = notificationService.taoNeuChuaCo(
                        userId,
                        NotificationType.ASSIGNMENT_DUE,
                        "Bài tập \"%s\" sắp hết hạn".formatted(baiTap.getTitle()),
                        "Lớp %s · hạn nộp trong vòng %d giờ tới."
                                .formatted(baiTap.getClassroom().getName(), NHAC_TRUOC_GIO),
                        """
                        {"kind":"ASSIGNMENT_DUE","assignmentId":"%s"}""".formatted(baiTap.getId()),
                        "assignment:" + baiTap.getId()).isPresent();

                if (vuaTao) {
                    daGui++;
                }
            }
        }
        return daGui;
    }
}
