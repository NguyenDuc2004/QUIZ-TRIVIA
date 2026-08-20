package com.datn.quizai.season.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.season.domain.Season;
import com.datn.quizai.season.domain.SeasonRanking;
import com.datn.quizai.season.dto.LeaderboardResponse;
import com.datn.quizai.season.dto.SeasonHistoryItem;
import com.datn.quizai.season.repository.SeasonRankingRepository;
import com.datn.quizai.season.service.SeasonLeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bảng xếp hạng theo mùa (features/15).
 * <p>
 * <b>Chỉ đọc.</b> Điểm mùa là tổng XP kiếm được trong khoảng thời gian mùa, và XP chỉ đến từ hành động học
 * thật (features/13). Không có endpoint nào cộng điểm trực tiếp — mở đường đó là mở đường tự leo hạng.
 * <p>
 * <b>Chỉ có phạm vi toàn hệ thống.</b> FR-62 nêu thêm hai phạm vi <i>theo lớp</i> và <i>theo bạn bè</i>, nhưng
 * lớp học là features/14 (chưa làm) và <i>bạn bè</i> không tồn tại ở bất kỳ đâu trong docs — không bảng, không
 * API, không yêu cầu chức năng. Thêm hai tuỳ chọn luôn trả về cùng một danh sách chỉ để đủ ba mục là hứa với
 * người dùng một thứ không có.
 */
@RestController
@RequestMapping("/api/v1/leaderboard/season")
@Tag(name = "Leaderboard", description = "Bảng xếp hạng theo mùa (Redis Sorted Set)")
@SecurityRequirement(name = "bearerAuth")
public class SeasonLeaderboardController {

    private final SeasonLeaderboardService leaderboardService;
    private final SeasonRankingRepository rankingRepository;

    public SeasonLeaderboardController(SeasonLeaderboardService leaderboardService,
                                       SeasonRankingRepository rankingRepository) {
        this.leaderboardService = leaderboardService;
        this.rankingRepository = rankingRepository;
    }

    @GetMapping("/current")
    @Operation(summary = "Bảng xếp hạng mùa hiện tại kèm thứ hạng của tôi. Đọc từ Redis Sorted Set; nếu "
            + "Redis trống thì tự dựng lại từ xp_events, nên mất Redis chỉ là chậm một lần chứ không mất dữ liệu.")
    public LeaderboardResponse current(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        Season mua = leaderboardService.muaHienTai();
        List<SeasonLeaderboardService.Dong> top = leaderboardService.top(limit);
        SeasonLeaderboardService.Dong cuaToi = leaderboardService.thuHangCuaToi(current.id());

        long tongSoNguoi = leaderboardService.soNguoiThamGia();

        return new LeaderboardResponse(
                mua.getId(), mua.getName(), mua.getStartAt(), mua.getEndAt(),
                tongSoNguoi,
                top.stream().map(d -> toDong(d, tongSoNguoi)).toList(),
                cuaToi == null ? null : toDong(cuaToi, tongSoNguoi));
    }

    @GetMapping("/current/me")
    @Operation(summary = "Chỉ thứ hạng của tôi trong mùa hiện tại. Trả 204 khi tôi chưa có điểm nào — KHÁC "
            + "với hạng cuối, nên không trả một con số hạng sai.")
    public org.springframework.http.ResponseEntity<LeaderboardResponse.Dong> me(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        SeasonLeaderboardService.Dong cuaToi = leaderboardService.thuHangCuaToi(current.id());
        return cuaToi == null
                ? org.springframework.http.ResponseEntity.noContent().build()
                : org.springframework.http.ResponseEntity.ok(
                        toDong(cuaToi, leaderboardService.soNguoiThamGia()));
    }

    @GetMapping("/history")
    @Operation(summary = "Các mùa đã kết thúc kèm thành tích của tôi, mới nhất trước. Đọc từ bảng lưu trữ "
            + "season_rankings, không phụ thuộc Redis.")
    public List<SeasonHistoryItem> history(@AuthenticationPrincipal JwtService.AuthenticatedUser current) {
        return rankingRepository.findHistoryOfUser(current.id()).stream()
                .map(SeasonLeaderboardController::toHistory)
                .toList();
    }

    /**
     * Ánh xạ sang DTO, tính luôn hạng Đồng/Bạc/Vàng (FR-64).
     * <p>
     * Hạng cần <b>tổng số người trong mùa</b> vì nó là vị trí tương đối, không phải ngưỡng điểm tuyệt đối —
     * xem {@code PhanHang} về việc vì sao. Nên tham số đó bắt buộc, không có giá trị mặc định: quên truyền
     * thì mọi người sẽ ra hạng null trong im lặng thay vì đỏ ở chỗ biên dịch.
     */
    private static LeaderboardResponse.Dong toDong(SeasonLeaderboardService.Dong d, long tongSoNguoi) {
        return LeaderboardResponse.Dong.cua(
                d.rank(), d.userId(), d.displayName(), d.avatarUrl(), d.score(), tongSoNguoi);
    }

    private static SeasonHistoryItem toHistory(SeasonRanking r) {
        var badge = r.getRewardBadge();
        return new SeasonHistoryItem(
                r.getSeason().getId(),
                r.getSeason().getName(),
                r.getSeason().getEndAt(),
                r.getFinalRank(),
                r.getFinalScore(),
                badge == null ? null : badge.getName(),
                badge == null ? null : badge.getIcon());
    }
}
