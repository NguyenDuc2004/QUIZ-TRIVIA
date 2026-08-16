package com.datn.quizai.gamification.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.gamification.domain.Badge;
import com.datn.quizai.gamification.domain.DailyChallenge;
import com.datn.quizai.gamification.domain.UserBadge;
import com.datn.quizai.gamification.domain.UserStats;
import com.datn.quizai.gamification.dto.BadgeResponse;
import com.datn.quizai.gamification.dto.DailyChallengeResponse;
import com.datn.quizai.gamification.dto.GamificationOverview;
import com.datn.quizai.gamification.service.DailyChallengeService;
import com.datn.quizai.gamification.service.GamificationService;
import com.datn.quizai.gamification.service.LevelCalculator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * XP, cấp độ, chuỗi ngày, huy hiệu và thử thách ngày (features/13).
 * <p>
 * <b>Chỉ đọc.</b> Không có endpoint nào cộng XP hay trao huy hiệu: XP chỉ đến từ hành động học thật, qua
 * domain event. Mở một đường ghi qua API là mở đường tự cộng điểm cho mình, và khi đó cả bảng xếp hạng lẫn
 * huy hiệu đều mất ý nghĩa.
 * <p>
 * Mọi endpoint làm việc trên dữ liệu của chính người gọi, lấy từ token — không có tham số {@code userId}.
 */
@RestController
@RequestMapping("/api/v1/gamification")
@Tag(name = "Gamification", description = "XP, cấp độ, chuỗi ngày học, huy hiệu, thử thách ngày")
@SecurityRequirement(name = "bearerAuth")
public class GamificationController {

    /** Số huy hiệu mới nhất trả kèm trang tổng quan — đủ để hiện một dải, không phải cả danh sách. */
    private static final int SO_HUY_HIEU_MOI_NHAT = 5;

    private final GamificationService gamificationService;
    private final DailyChallengeService dailyChallengeService;

    public GamificationController(GamificationService gamificationService,
                                 DailyChallengeService dailyChallengeService) {
        this.gamificationService = gamificationService;
        this.dailyChallengeService = dailyChallengeService;
    }

    @GetMapping("/me")
    @Operation(summary = "Tổng quan: XP, cấp độ và tiến độ trong cấp, chuỗi ngày, số huy hiệu đã đạt")
    public GamificationOverview me(@AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        UserStats stats = gamificationService.statsOf(current.id());
        var tienDo = LevelCalculator.tienDo(stats.getTotalXp());
        List<UserBadge> daDat = gamificationService.badgesOf(current.id());

        return new GamificationOverview(
                stats.getTotalXp(),
                tienDo.level(),
                tienDo.xpTrongCap(),
                tienDo.xpCanTrongCap(),
                stats.getCurrentStreak(),
                stats.getLongestStreak(),
                stats.getLastActiveDate(),
                LocalDate.now().equals(stats.getLastActiveDate()),
                daDat.size(),
                gamificationService.allBadges().size(),
                daDat.stream().limit(SO_HUY_HIEU_MOI_NHAT).map(GamificationController::toResponse).toList());
    }

    @GetMapping("/badges")
    @Operation(summary = "Toàn bộ huy hiệu, kèm mốc thời gian mở khoá. Huy hiệu chưa đạt có earnedAt = null "
            + "— trả cả danh sách để người học thấy còn gì để hướng tới.")
    public List<BadgeResponse> badges(@AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        // Nạp huy hiệu đã đạt thành map trước rồi ghép, thay vì hỏi cơ sở dữ liệu cho từng huy hiệu
        Map<UUID, UserBadge> daDat = gamificationService.badgesOf(current.id()).stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getId(), Function.identity()));

        return gamificationService.allBadges().stream()
                .map(badge -> {
                    UserBadge cua = daDat.get(badge.getId());
                    return new BadgeResponse(badge.getId(), badge.getCode(), badge.getName(),
                            badge.getDescription(), badge.getIcon(),
                            cua == null ? null : cua.getEarnedAt());
                })
                .toList();
    }

    @GetMapping("/daily")
    @Operation(summary = "Thử thách hôm nay kèm tiến độ của tôi. Thử thách được tạo ở lần đầu có người hỏi "
            + "tới trong ngày, không cần bộ hẹn giờ.")
    public DailyChallengeResponse daily(@AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        var tienDo = dailyChallengeService.tienDoCuaToi(current.id());
        DailyChallenge challenge = tienDo.getChallenge();

        return new DailyChallengeResponse(
                challenge.getId(),
                challenge.getChallengeDate(),
                challenge.getDescription(),
                tienDo.getProgress(),
                dailyChallengeService.target(challenge),
                challenge.getXpReward(),
                tienDo.getCompletedAt());
    }

    private static BadgeResponse toResponse(UserBadge userBadge) {
        Badge badge = userBadge.getBadge();
        return new BadgeResponse(badge.getId(), badge.getCode(), badge.getName(), badge.getDescription(),
                badge.getIcon(), userBadge.getEarnedAt());
    }
}
