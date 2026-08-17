package com.datn.quizai.gamification.service;

import com.datn.quizai.gamification.domain.DailyChallenge;
import com.datn.quizai.gamification.domain.UserDailyProgress;
import com.datn.quizai.gamification.domain.XpSource;
import com.datn.quizai.gamification.repository.DailyChallengeRepository;
import com.datn.quizai.gamification.repository.UserDailyProgressRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Thử thách hằng ngày (features/13, FR-52).
 *
 * <h3>Sinh khi cần, không cần bộ hẹn giờ</h3>
 * Thử thách của hôm nay được tạo ở lần đầu có ai hỏi tới, thay vì một job chạy lúc nửa đêm. Job hẹn giờ thêm
 * một thứ có thể chết âm thầm — và nếu nó chết thì cả ngày đó không ai có thử thách, trong khi cách này chỉ
 * cần có một người mở trang là mọi người đều có.
 * <p>
 * Ràng buộc UNIQUE trên {@code challenge_date} là thứ chặn tạo trùng khi hai người mở trang cùng lúc.
 *
 * <h3>Nội dung quay vòng theo ngày</h3>
 * Chọn mẫu theo số ngày trong năm, không chọn ngẫu nhiên: cùng một ngày thì mọi người thấy cùng một thử
 * thách, và mở lại trang không đổi sang thử thách khác.
 */
@Service
public class DailyChallengeService {

    private static final Logger log = LoggerFactory.getLogger(DailyChallengeService.class);

    /** Các mẫu thử thách. Thêm mẫu mới chỉ cần thêm một dòng, miễn là {@code type} được xử lý. */
    private static final List<Mau> MAU = List.of(
            new Mau("Hoàn thành 3 bài quiz hôm nay", "COMPLETE_ATTEMPTS", 3, 60),
            new Mau("Ôn 10 thẻ ghi nhớ đến hạn", "REVIEW_FLASHCARDS", 10, 50),
            new Mau("Làm đúng 100% một bài quiz", "PERFECT_ATTEMPT", 1, 80),
            new Mau("Hoàn thành 1 bài quiz và ôn 5 thẻ", "MIXED_LIGHT", 6, 40));

    private record Mau(String moTa, String loai, int target, int xp) {
    }

    private final DailyChallengeRepository challengeRepository;
    private final UserDailyProgressRepository progressRepository;
    private final GamificationService gamificationService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbc;

    public DailyChallengeService(DailyChallengeRepository challengeRepository,
                                UserDailyProgressRepository progressRepository,
                                GamificationService gamificationService,
                                ObjectMapper objectMapper,
                                JdbcTemplate jdbc) {
        this.challengeRepository = challengeRepository;
        this.progressRepository = progressRepository;
        this.gamificationService = gamificationService;
        this.objectMapper = objectMapper;
        this.jdbc = jdbc;
    }

    @Transactional
    public DailyChallenge thuThachHomNay() {
        LocalDate homNay = LocalDate.now();
        return challengeRepository.findByChallengeDate(homNay)
                .orElseGet(() -> tao(homNay));
    }

    /**
     * Tạo thử thách cho một ngày, an toàn khi hai người mở trang cùng lúc.
     * <p>
     * Dùng {@code ON CONFLICT DO NOTHING} chứ <b>không</b> bắt {@code DataIntegrityViolationException}: bắt
     * ngoại lệ ràng buộc rồi tiếp tục là một cái bẫy — Spring đã đánh dấu transaction là rollback-only nên
     * lần commit sau đó vẫn vỡ. Câu lệnh này không bao giờ ném, nên transaction sạch và người thứ hai chỉ
     * việc đọc dòng của người thắng.
     */
    private DailyChallenge tao(LocalDate ngay) {
        Mau mau = MAU.get(ngay.getDayOfYear() % MAU.size());
        jdbc.update("""
                insert into daily_challenges (id, challenge_date, description, rule, xp_reward)
                values (gen_random_uuid(), ?, ?, cast(? as jsonb), ?)
                on conflict (challenge_date) do nothing
                """,
                ngay, mau.moTa(),
                "{\"type\":\"%s\",\"target\":%d}".formatted(mau.loai(), mau.target()),
                mau.xp());

        return challengeRepository.findByChallengeDate(ngay)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tạo được thử thách cho ngày " + ngay));
    }

    @Transactional
    public UserDailyProgress tienDoCuaToi(UUID userId) {
        DailyChallenge challenge = thuThachHomNay();
        return progressRepository.findByUserIdAndChallengeId(userId, challenge.getId())
                .orElseGet(() -> progressRepository.save(new UserDailyProgress(userId, challenge)));
    }

    /**
     * Cộng tiến độ cho một loại hành động, và trao thưởng nếu vừa đạt mục tiêu.
     *
     * @param loaiHanhDong {@code ATTEMPT}, {@code PERFECT_ATTEMPT} hoặc {@code FLASHCARD_REVIEW}
     */
    @Transactional
    public void ghiNhanHanhDong(UUID userId, String loaiHanhDong) {
        DailyChallenge challenge = thuThachHomNay();
        if (!hanhDongTinhCho(challenge, loaiHanhDong)) {
            return;
        }

        UserDailyProgress tienDo = tienDoCuaToi(userId);
        if (tienDo.getCompletedAt() != null) {
            // Đã hoàn thành hôm nay: không cộng tiếp, và nhất định không trao thưởng lần hai
            return;
        }

        tienDo.setProgress(tienDo.getProgress() + 1);

        if (tienDo.getProgress() >= target(challenge)) {
            tienDo.setCompletedAt(OffsetDateTime.now());
            // Thưởng đi qua `award` nên vẫn được chặn trùng bởi xp_events, kể cả khi luồng này chạy hai lần
            gamificationService.award(userId, XpSource.DAILY_CHALLENGE,
                    challenge.getId().toString(), challenge.getXpReward());
            log.info("Người dùng {} hoàn thành thử thách ngày {}", userId, challenge.getChallengeDate());
        }
    }

    /** Hành động này có tính vào thử thách hôm nay hay không. */
    private boolean hanhDongTinhCho(DailyChallenge challenge, String loaiHanhDong) {
        String loai = ruleField(challenge, "type");
        return switch (loai) {
            case "COMPLETE_ATTEMPTS" -> "ATTEMPT".equals(loaiHanhDong);
            case "REVIEW_FLASHCARDS" -> "FLASHCARD_REVIEW".equals(loaiHanhDong);
            case "PERFECT_ATTEMPT" -> "PERFECT_ATTEMPT".equals(loaiHanhDong);
            // Mẫu hỗn hợp tính cả hai loại — mục tiêu là tổng số hành động
            case "MIXED_LIGHT" -> "ATTEMPT".equals(loaiHanhDong) || "FLASHCARD_REVIEW".equals(loaiHanhDong);
            default -> {
                log.warn("Thử thách ngày có loại không hỗ trợ: {}", loai);
                yield false;
            }
        };
    }

    public int target(DailyChallenge challenge) {
        try {
            JsonNode rule = objectMapper.readTree(challenge.getRule());
            int target = rule.path("target").asInt();
            // Mục tiêu 0 làm thử thách hoàn thành ngay khi chưa làm gì — coi như dữ liệu sai và dùng 1
            return target > 0 ? target : 1;
        } catch (Exception e) {
            log.warn("Không đọc được rule của thử thách {}: {}", challenge.getId(), e.getMessage());
            return 1;
        }
    }

    private String ruleField(DailyChallenge challenge, String field) {
        try {
            return objectMapper.readTree(challenge.getRule()).path(field).asText();
        } catch (Exception e) {
            return "";
        }
    }
}
