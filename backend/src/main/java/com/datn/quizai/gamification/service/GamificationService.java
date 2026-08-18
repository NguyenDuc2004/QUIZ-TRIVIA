package com.datn.quizai.gamification.service;

import com.datn.quizai.gamification.domain.Badge;
import com.datn.quizai.gamification.domain.UserBadge;
import com.datn.quizai.gamification.domain.UserStats;
import com.datn.quizai.gamification.domain.XpEvent;
import com.datn.quizai.gamification.domain.XpSource;
import com.datn.quizai.gamification.repository.AchievementCounters;
import com.datn.quizai.gamification.repository.BadgeRepository;
import com.datn.quizai.gamification.repository.UserBadgeRepository;
import com.datn.quizai.gamification.repository.UserStatsRepository;
import com.datn.quizai.gamification.repository.XpEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cộng XP, cập nhật cấp độ và chuỗi ngày, trao huy hiệu (features/13, FR-49 → FR-51).
 * <p>
 * Lớp này <b>không được gọi từ service nghiệp vụ</b>. Nó chỉ phản ứng với domain event
 * ({@code AttemptSubmittedEvent}, {@code FlashcardReviewedEvent}) — xem {@link GamificationEventListener}.
 * Nhồi lời gọi cộng XP vào giữa logic làm bài hay ôn thẻ là buộc hai việc không liên quan vào nhau: một lỗi
 * ở gamification sẽ làm vỡ luồng nộp bài.
 *
 * <h3>Idempotent</h3>
 * Mỗi lần cộng XP ghi một dòng vào {@code xp_events} với khoá tự nhiên của hành động. Ràng buộc
 * {@code UNIQUE (user_id, source_type, source_key)} ở cơ sở dữ liệu là chốt cuối; kiểm trước trong Java chỉ
 * để tránh ném ngoại lệ trong luồng bình thường. Cần cả hai vì kiểm trong Java thua cuộc khi hai luồng chạy
 * song song.
 */
@Service
public class GamificationService {

    private static final Logger log = LoggerFactory.getLogger(GamificationService.class);

    private final UserStatsRepository statsRepository;
    private final XpEventRepository xpEventRepository;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final AchievementCounters counters;
    private final ObjectMapper objectMapper;
    private final com.datn.quizai.season.service.SeasonLeaderboardService leaderboardService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public GamificationService(UserStatsRepository statsRepository,
                              XpEventRepository xpEventRepository,
                              BadgeRepository badgeRepository,
                              UserBadgeRepository userBadgeRepository,
                              AchievementCounters counters,
                              ObjectMapper objectMapper,
                              com.datn.quizai.season.service.SeasonLeaderboardService leaderboardService,
                              org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.statsRepository = statsRepository;
        this.xpEventRepository = xpEventRepository;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.counters = counters;
        this.objectMapper = objectMapper;
        this.leaderboardService = leaderboardService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Cộng XP cho một hành động và cập nhật chuỗi ngày.
     *
     * @param sourceKey khoá tự nhiên của hành động — xem {@link XpSource} để biết cách dựng
     * @return true nếu vừa cộng, false nếu hành động này đã được tính trước đó
     */
    @Transactional
    public boolean award(UUID userId, XpSource source, String sourceKey, int xp) {
        if (xp <= 0) {
            return false;
        }
        if (xpEventRepository.existsByUserIdAndSourceTypeAndSourceKey(userId, source, sourceKey)) {
            return false;
        }

        // KHÔNG bắt DataIntegrityViolationException ở đây. Bắt rồi tiếp tục là một cái bẫy: Spring đã đánh
        // dấu transaction là rollback-only nên lần commit sau đó vẫn vỡ với UnexpectedRollbackException,
        // tức "xử lý êm" chỉ là ảo giác. Để ngoại lệ nổi lên cho listener bắt — transaction này rollback,
        // và đó là kết quả ĐÚNG vì luồng kia đã cộng XP cho hành động đó rồi.
        xpEventRepository.save(new XpEvent(userId, source, sourceKey, xp));

        UserStats stats = statsRepository.findById(userId)
                .orElseGet(() -> statsRepository.save(new UserStats(userId)));

        int capCu = stats.getLevel();
        stats.setTotalXp(stats.getTotalXp() + xp);
        stats.setLevel(LevelCalculator.capTuXp(stats.getTotalXp()));

        var streak = StreakCalculator.capNhat(stats.getLastActiveDate(), stats.getCurrentStreak(),
                stats.getLongestStreak(), LocalDate.now());
        stats.setCurrentStreak(streak.currentStreak());
        stats.setLongestStreak(streak.longestStreak());
        stats.setLastActiveDate(LocalDate.now());

        // Đồng bộ điểm mùa (features/15). Gọi sau khi đã ghi xp_events nên đã được chặn trùng ở trên; lỗi
        // Redis bên trong hàm này được nuốt và ghi log, vì XP đã vào cơ sở dữ liệu và ZSET dựng lại được.
        leaderboardService.congDiem(userId, xp);

        List<Badge> vuaTrao = traoHuyHieuDatDuoc(userId, stats);

        // Phát sự kiện sau khi mọi thay đổi đã xong (features/16, FR-53). Người nhận là listener sau-commit,
        // nên phát sớm hơn cũng không đổi thứ tự — nhưng đặt ở đây thì đọc code thấy rõ "đây là kết quả
        // cuối", không phải một trạng thái nửa vời.
        if (stats.getLevel() > capCu) {
            eventPublisher.publishEvent(new LevelUpEvent(userId, capCu, stats.getLevel()));
        }
        for (Badge badge : vuaTrao) {
            eventPublisher.publishEvent(new BadgeEarnedEvent(userId, badge.getCode(), badge.getName()));
        }
        return true;
    }

    /** Chỉ số của một người; tạo dòng mặc định nếu chưa có để giao diện không phải xử lý trường hợp null. */
    @Transactional
    public UserStats statsOf(UUID userId) {
        return statsRepository.findById(userId)
                .orElseGet(() -> statsRepository.save(new UserStats(userId)));
    }

    @Transactional(readOnly = true)
    public List<Badge> allBadges() {
        return badgeRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<UserBadge> badgesOf(UUID userId) {
        return userBadgeRepository.findByUserIdWithBadge(userId);
    }

    /**
     * Xét mọi huy hiệu chưa có và trao những cái đã đạt điều kiện.
     * <p>
     * Xét <b>tất cả</b> chứ không chỉ loại liên quan tới hành động vừa rồi: số huy hiệu nhỏ (mười cái), và
     * xét chọn lọc thì phải giữ một bảng ánh xạ "hành động nào ảnh hưởng điều kiện nào" — một chỗ nữa để
     * quên cập nhật khi thêm huy hiệu mới.
     *
     * @return các huy hiệu vừa trao
     */
    @Transactional
    public List<Badge> traoHuyHieuDatDuoc(UUID userId, UserStats stats) {
        List<Badge> vuaTrao = new ArrayList<>();

        for (Badge badge : badgeRepository.findAllByOrderBySortOrderAsc()) {
            if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
                continue;
            }
            if (!datDieuKien(badge, userId, stats)) {
                continue;
            }
            // Cũng không bắt lỗi ràng buộc ở đây, cùng lý do như trong `award`: bắt rồi tiếp tục thì
            // transaction đã bị đánh dấu rollback-only và commit vẫn vỡ.
            userBadgeRepository.save(new UserBadge(userId, badge));
            vuaTrao.add(badge);
            log.info("Người dùng {} nhận huy hiệu {}", userId, badge.getCode());
        }
        return vuaTrao;
    }

    /**
     * Xét điều kiện của một huy hiệu.
     * <p>
     * Điều kiện không hỗ trợ được thì trả {@code false} và ghi log, <b>không ném ngoại lệ</b>: một dòng dữ
     * liệu huy hiệu sai không đáng làm vỡ luồng nộp bài của người dùng.
     */
    private boolean datDieuKien(Badge badge, UUID userId, UserStats stats) {
        try {
            JsonNode dieuKien = objectMapper.readTree(badge.getCondition());
            String loai = dieuKien.path("type").asText();
            int nguong = dieuKien.path("threshold").asInt();

            return switch (loai) {
                case "XP" -> stats.getTotalXp() >= nguong;
                case "STREAK" -> stats.getCurrentStreak() >= nguong;
                case "PERFECT_ATTEMPTS" -> counters.soBaiHoanHao(userId) >= nguong;
                case "FLASHCARDS_MASTERED" -> counters.soTheDaThuoc(userId) >= nguong;
                // Huy hiệu mùa KHÔNG tự xét được từ số liệu hiện tại — nó chỉ do việc chốt mùa trao
                // (features/15). Trả false im lặng, không ghi log cảnh báo: hàm này chạy mỗi lần có người
                // nộp bài, và một cảnh báo vô nghĩa mỗi lần thì log thành rác.
                case "SEASON_RANK" -> false;
                default -> {
                    log.warn("Huy hiệu {} có điều kiện không hỗ trợ: {}", badge.getCode(), loai);
                    yield false;
                }
            };
        } catch (Exception e) {
            log.warn("Không đọc được điều kiện của huy hiệu {}: {}", badge.getCode(), e.getMessage());
            return false;
        }
    }
}
