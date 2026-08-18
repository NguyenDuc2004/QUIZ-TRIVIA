package com.datn.quizai.season.service;

import com.datn.quizai.gamification.domain.Badge;
import com.datn.quizai.gamification.domain.UserBadge;
import com.datn.quizai.gamification.repository.BadgeRepository;
import com.datn.quizai.gamification.repository.UserBadgeRepository;
import com.datn.quizai.season.domain.Season;
import com.datn.quizai.season.domain.SeasonRanking;
import com.datn.quizai.season.domain.SeasonStatus;
import com.datn.quizai.season.repository.SeasonRankingRepository;
import com.datn.quizai.season.repository.SeasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

/**
 * Chốt mùa và mở mùa mới (features/15, FR-63).
 *
 * <h3>Idempotent — đặc tả yêu cầu, và đây là cách bảo đảm</h3>
 * Chạy lại job không được trao thưởng hai lần hay tạo hai mùa mới. Ba chốt, đi từ ngoài vào:
 * <ol>
 *   <li>Chỉ chốt mùa đang {@code ACTIVE} <b>và</b> đã quá {@code end_at}. Chốt xong mùa thành {@code ENDED}
 *       nên lần chạy sau không tìm thấy gì.</li>
 *   <li>{@code season_rankings} có {@code UNIQUE (season_id, user_id)} — ghi lại là vi phạm ràng buộc, không
 *       phải âm thầm nhân đôi.</li>
 *   <li>{@code user_badges} có {@code UNIQUE (user_id, badge_id)}, và kiểm trước bằng
 *       {@code existsByUserIdAndBadgeId}.</li>
 * </ol>
 * Ngoài ra {@code uk_seasons_one_active} ở cơ sở dữ liệu chặn việc tồn tại hai mùa {@code ACTIVE} — kể cả khi
 * hai tiến trình chạy job cùng lúc.
 */
@Service
public class SeasonClosingService {

    private static final Logger log = LoggerFactory.getLogger(SeasonClosingService.class);

    /** Số người được ghi vào bảng lưu trữ. Sâu hơn thế thì bảng phình mà không ai tra tới. */
    private static final int LUU_TOP = 100;

    /** Mốc trao huy hiệu: hạng 1, top 3, top 10. */
    private static final int[] MOC_THUONG = {1, 3, 10};
    private static final String[] MA_HUY_HIEU = {"SEASON_TOP1", "SEASON_TOP3", "SEASON_TOP10"};

    private final SeasonRepository seasonRepository;
    private final SeasonRankingRepository rankingRepository;
    private final SeasonLeaderboardService leaderboardService;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final StringRedisTemplate redis;

    public SeasonClosingService(SeasonRepository seasonRepository,
                                SeasonRankingRepository rankingRepository,
                                SeasonLeaderboardService leaderboardService,
                                BadgeRepository badgeRepository,
                                UserBadgeRepository userBadgeRepository,
                                StringRedisTemplate redis) {
        this.seasonRepository = seasonRepository;
        this.rankingRepository = rankingRepository;
        this.leaderboardService = leaderboardService;
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.redis = redis;
    }

    /**
     * Kiểm mỗi giờ xem có mùa nào đã quá hạn.
     * <p>
     * Mỗi giờ chứ không phải đúng nửa đêm: hẹn đúng một thời điểm thì máy chủ tắt lúc đó là bỏ luôn cả mùa.
     * Quét định kỳ thì trễ nhất một giờ, và mùa vẫn được chốt kể cả sau khi khởi động lại.
     */
    @Scheduled(cron = "0 5 * * * *")
    public void quetMuaQuaHan() {
        seasonRepository.findFirstByStatusAndEndAtBefore(SeasonStatus.ACTIVE, OffsetDateTime.now())
                .ifPresent(mua -> chotMua(mua.getId()));
    }

    /** Kết quả chốt mùa, để báo lại cho quản trị viên hoặc test. */
    public record KetQua(String tenMua, int soNguoiLuu, int soHuyHieuTrao, String tenMuaMoi) {
    }

    /**
     * Chốt một mùa: lưu bảng xếp hạng, trao huy hiệu, đóng mùa, mở mùa mới.
     * <p>
     * Tách khỏi {@link #quetMuaQuaHan} để test gọi thẳng được, và để về sau quản trị viên chốt tay được nếu
     * cần.
     */
    @Transactional
    public KetQua chotMua(java.util.UUID seasonId) {
        Season mua = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mùa " + seasonId));

        if (mua.getStatus() == SeasonStatus.ENDED) {
            log.debug("Mùa {} đã chốt trước đó, bỏ qua", mua.getName());
            return new KetQua(mua.getName(), 0, 0, null);
        }

        // Dựng lại ZSET trước khi chốt: nếu Redis từng mất dữ liệu giữa mùa thì bảng xếp hạng đang thiếu, và
        // chốt một bảng thiếu là trao thưởng sai người. Cơ sở dữ liệu mới là nguồn sự thật.
        leaderboardService.dungLaiTuCoSoDuLieu(mua);

        List<SeasonLeaderboardService.Dong> bang = leaderboardService.top(LUU_TOP);
        int soHuyHieu = 0;

        for (SeasonLeaderboardService.Dong dong : bang) {
            Badge thuong = huyHieuChoHang(dong.rank()).orElse(null);

            SeasonRanking ranking = new SeasonRanking();
            ranking.setSeason(mua);
            ranking.setUserId(dong.userId());
            ranking.setFinalScore(dong.score());
            ranking.setFinalRank(dong.rank());
            ranking.setRewardBadge(thuong);
            rankingRepository.save(ranking);

            if (thuong != null && !userBadgeRepository.existsByUserIdAndBadgeId(dong.userId(), thuong.getId())) {
                userBadgeRepository.save(new UserBadge(dong.userId(), thuong));
                soHuyHieu++;
            }
        }

        mua.setStatus(SeasonStatus.ENDED);
        // saveAndFlush, KHÔNG chỉ setStatus: Hibernate xếp mọi INSERT trước mọi UPDATE khi flush cuối
        // transaction, nên mùa mới ở `moMuaMoi` sẽ được chèn trong lúc mùa cũ vẫn còn ACTIVE — và
        // `uk_seasons_one_active` chặn đúng ngay chỗ đó. Ép ghi trạng thái ENDED xuống trước để chỉ còn một
        // mùa ACTIVE tại mọi thời điểm.
        seasonRepository.saveAndFlush(mua);

        // Xoá ZSET của mùa cũ: điểm mùa đã chốt nằm ở `season_rankings`, giữ thêm bản ở Redis chỉ là một chỗ
        // nữa để lệch. Xoá sau khi đã lưu, không phải trước.
        try {
            redis.delete(SeasonLeaderboardService.key(mua.getId()));
        } catch (Exception e) {
            log.warn("Không xoá được ZSET của mùa {}: {}", mua.getName(), e.getMessage());
        }

        Season muaMoi = moMuaMoi(mua);
        log.info("Đã chốt mùa {}: lưu {} người, trao {} huy hiệu. Mùa mới: {}",
                mua.getName(), bang.size(), soHuyHieu, muaMoi.getName());
        return new KetQua(mua.getName(), bang.size(), soHuyHieu, muaMoi.getName());
    }

    /**
     * Mở mùa kế tiếp, bắt đầu ngay khi mùa cũ kết thúc.
     * <p>
     * Bắt đầu từ {@code endAt} của mùa cũ chứ không từ {@code now()}: job quét mỗi giờ nên có thể chạy muộn
     * vài chục phút, và nếu lấy {@code now()} thì XP kiếm trong khoảng trống đó không thuộc mùa nào.
     */
    private Season moMuaMoi(Season muaCu) {
        OffsetDateTime batDau = muaCu.getEndAt();
        OffsetDateTime ketThuc = batDau.toLocalDate()
                .with(TemporalAdjusters.firstDayOfNextMonth())
                .atStartOfDay(ZoneId.systemDefault())
                .toOffsetDateTime();
        // Mùa cũ kết thúc đúng đầu tháng thì mốc trên trả về chính tháng sau — luôn dài ít nhất một tháng
        if (!ketThuc.isAfter(batDau)) {
            ketThuc = batDau.plusMonths(1);
        }

        Season moi = new Season();
        moi.setName("Mùa " + batDau.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy")));
        moi.setStartAt(batDau);
        moi.setEndAt(ketThuc);
        moi.setStatus(SeasonStatus.ACTIVE);
        return seasonRepository.save(moi);
    }

    /** Huy hiệu ứng với một thứ hạng; rỗng nếu hạng đó không được thưởng. */
    private Optional<Badge> huyHieuChoHang(int hang) {
        for (int i = 0; i < MOC_THUONG.length; i++) {
            if (hang <= MOC_THUONG[i]) {
                // Tra theo mã, không quét findAll(): hàm này gọi cho từng người trong top 100
                return badgeRepository.findByCode(MA_HUY_HIEU[i]);
            }
        }
        return Optional.empty();
    }
}
