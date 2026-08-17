package com.datn.quizai.analytics.service;

import com.datn.quizai.analytics.dto.LearnerProgressResponse;
import com.datn.quizai.analytics.dto.QuizAttemptSummary;
import com.datn.quizai.analytics.dto.QuizStatsResponse;
import com.datn.quizai.analytics.repository.AnalyticsRepository;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.OwnershipGuard;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.integrity.domain.AttemptIntegrity;
import com.datn.quizai.integrity.domain.ReviewStatus;
import com.datn.quizai.integrity.repository.AttemptIntegrityRepository;
import com.datn.quizai.integrity.service.RiskScorer;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.quiz.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Thống kê học tập và thống kê quiz (docs/features/09, FR-85 & FR-86).
 * <p>
 * <b>Hai phạm vi truy cập khác nhau, đừng lẫn:</b>
 * <ul>
 *   <li>{@code /analytics/me} — dữ liệu của <i>chính người gọi</i>, ai đăng nhập cũng xem được phần
 *       của mình.</li>
 *   <li>{@code /analytics/quizzes/{id}} — dữ liệu tổng hợp của <i>quiz mình sở hữu</i>. Đây là ngoại
 *       lệ có chủ đích của luật "bài của ai người ấy xem", cùng lý do với việc chấm tay: không thấy
 *       gì thì không đánh giá được đề mình ra.</li>
 * </ul>
 */
@Service
public class AnalyticsService {

    /**
     * Câu phải có đủ ngần này lượt trả lời mới được xếp vào "câu khó".
     * <p>
     * Sai 1 trên 1 lượt là 100% sai nhưng không nói lên điều gì về câu hỏi — nó chỉ nói là ít người
     * làm. Cùng lý do với ngưỡng 3 câu khi kết luận người học yếu một chủ đề (features/07).
     */
    private static final int MIN_ANSWERS_FOR_HARD_QUESTION = 3;

    /** Số câu khó trả về — đủ để Creator biết sửa chỗ nào, không thành một bảng dài vô dụng. */
    private static final int HARD_QUESTION_LIMIT = 10;

    private final AnalyticsRepository repository;
    private final QuizRepository quizRepository;
    private final AttemptIntegrityRepository integrityRepository;

    public AnalyticsService(AnalyticsRepository repository,
                            QuizRepository quizRepository,
                            AttemptIntegrityRepository integrityRepository) {
        this.repository = repository;
        this.quizRepository = quizRepository;
        this.integrityRepository = integrityRepository;
    }

    /** Tiến độ của chính người gọi (FR-85). */
    @Transactional(readOnly = true)
    public LearnerProgressResponse myProgress(UUID userId) {
        AnalyticsRepository.LearnerOverviewRow overview = repository.findLearnerOverview(userId);

        List<LearnerProgressResponse.AttemptScore> trend = repository.findLearnerScoreTrend(userId)
                .stream()
                .map(row -> new LearnerProgressResponse.AttemptScore(
                        row.getSubmittedAt(), row.getQuizTitle(), row.getScore(), row.getMaxScore(),
                        percent(row.getScore(), row.getMaxScore())))
                .toList();

        return new LearnerProgressResponse(
                overview.getTotalAttempts(),
                overview.getDistinctQuizzes(),
                averagePercent(overview.getSumScore(), overview.getSumMaxScore()),
                trend);
    }

    /** Thống kê một quiz mình sở hữu (FR-86). */
    @Transactional(readOnly = true)
    public QuizStatsResponse quizStats(UUID quizId, JwtService.AuthenticatedUser current) {
        requireOwnedQuiz(quizId, current);

        AnalyticsRepository.QuizOverviewRow overview = repository.findQuizOverview(quizId);
        long total = overview.getTotalAttempts();

        return new QuizStatsResponse(
                total,
                overview.getDistinctLearners(),
                averagePercent(overview.getSumScore(), overview.getSumMaxScore()),
                total == 0 ? null : round(overview.getSubmittedCount() * 100.0 / total),
                scoreDistribution(quizId),
                hardestQuestions(quizId));
    }

    /**
     * Bài làm trên quiz mình sở hữu, kèm cờ cần chấm tay và cờ đáng rà soát (FR-86, FR-47 + nợ từ
     * features/06).
     * <p>
     * API ghi đè điểm đã có từ lát cắt 6, nhưng Creator không có cách nào <i>tìm ra</i> bài nào cần
     * chấm — nên tính năng đó trên thực tế không dùng được. Danh sách này là cửa vào còn thiếu.
     * <p>
     * <b>Điểm rủi ro cũng vào đây vì đúng lý do đó.</b> FR-47 cho chủ quiz quyền xem báo cáo tính toàn
     * vẹn, nhưng đường vào duy nhất là màn chấm từng bài — một giáo viên có 200 bài nộp sẽ không bao giờ
     * biết bài nào đáng xem trừ khi bấm vào cả 200. Quyền có mà đường đi thì không, nên trên thực tế chỉ
     * Admin phát hiện được, còn người hiểu hoàn cảnh lớp mình nhất thì không thấy gì.
     *
     * <h3>Chỉ gửi điểm khi bài VƯỢT NGƯỠNG, dưới ngưỡng thì gửi null</h3>
     * Không phải để tiết kiệm băng thông. Gắn một con số "mức đáng ngờ" vào <i>từng</i> người học là mời
     * người ta xếp hạng học sinh theo độ nghi — đúng cái tác hại mà cả tính năng này cố tránh. Và một điểm
     * 45 không kèm cờ nào thì không nói gì cả: danh sách lý do rỗng, người chấm không làm gì được với nó.
     * Quyết định này đặt ở <b>máy chủ</b> chứ không để giao diện tự lọc, cùng lý do với việc trả 404 thay
     * vì 403: một lát nữa có ai thêm một cột vào bảng thì con số không được phép đã nằm sẵn ở đó.
     */
    @Transactional(readOnly = true)
    public List<QuizAttemptSummary> quizAttempts(UUID quizId, JwtService.AuthenticatedUser current) {
        requireOwnedQuiz(quizId, current);

        List<AnalyticsRepository.QuizAttemptRow> rows = repository.findQuizAttempts(quizId);
        Map<UUID, AttemptIntegrity> toanVen = napToanVen(rows);

        return rows.stream()
                .map(row -> {
                    AttemptIntegrity tv = toanVen.get(row.getAttemptId());
                    // Hai biến rời thay vì hai biểu thức ba ngôi: chúng phải cùng có hoặc cùng không, và
                    // viết thành một khối if thì không có cách nào lệch nhau về sau
                    Integer diemRuiRo = null;
                    ReviewStatus trangThaiRaSoat = null;
                    if (tv != null && tv.getRiskScore() >= RiskScorer.NGUONG_GAN_CO) {
                        diemRuiRo = tv.getRiskScore();
                        trangThaiRaSoat = tv.getReviewStatus();
                    }
                    return new QuizAttemptSummary(
                            row.getAttemptId(), row.getLearnerName(), row.getScore(), row.getMaxScore(),
                            row.getSubmittedAt(), row.getPendingAiCount(), row.getFailedAiCount(),
                            row.getPendingAiCount() + row.getFailedAiCount() > 0,
                            diemRuiRo, trangThaiRaSoat);
                })
                .toList();
    }

    /**
     * Nạp bản tổng hợp tính toàn vẹn cho cả danh sách trong <b>một</b> truy vấn.
     * <p>
     * Gọi {@code findByAttemptId} trong vòng lặp thì một quiz 200 bài nộp thành 200 truy vấn cho một cột
     * hiển thị — và nó sẽ không lộ ra khi thử với ba bài trên máy mình.
     */
    private Map<UUID, AttemptIntegrity> napToanVen(List<AnalyticsRepository.QuizAttemptRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = rows.stream().map(AnalyticsRepository.QuizAttemptRow::getAttemptId).toList();
        return integrityRepository.findByAttemptIdIn(ids).stream()
                .collect(Collectors.toMap(AttemptIntegrity::getAttemptId, tv -> tv));
    }

    // ------------------------------------------------------------------ nội bộ

    /**
     * Chỉ chủ quiz (hoặc Admin) mới xem được thống kê.
     * <p>
     * Trả <b>404</b> chứ không phải 403 — cùng quy ước với chỗ khác trong dự án: người không có
     * quyền thì không được biết quiz đó có tồn tại hay không.
     */
    private void requireOwnedQuiz(UUID quizId, JwtService.AuthenticatedUser current) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));

        if (!OwnershipGuard.canManage(quiz.getOwner().getId(), current)) {
            throw BusinessException.notFound("Không tìm thấy quiz");
        }
    }

    /**
     * Mười khoảng 10%, <b>luôn trả đủ 10 phần tử</b> kể cả khoảng không có lượt nào.
     * <p>
     * CSDL chỉ trả về khoảng có dữ liệu. Nếu để client tự chèn số 0 vào chỗ trống thì biểu đồ sẽ
     * thiếu cột và trục hoành lệch — mà mỗi client lại tự chèn một kiểu.
     */
    private List<QuizStatsResponse.ScoreBucket> scoreDistribution(UUID quizId) {
        Map<Integer, Long> counts = repository.findScoreDistribution(quizId).stream()
                .collect(Collectors.toMap(
                        AnalyticsRepository.ScoreBucketRow::getBucket,
                        AnalyticsRepository.ScoreBucketRow::getAttemptCount,
                        Long::sum));

        List<QuizStatsResponse.ScoreBucket> buckets = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            int from = i * 10;
            int to = from + 10;
            buckets.add(new QuizStatsResponse.ScoreBucket(
                    from, to, from + "–" + to + "%", counts.getOrDefault(i, 0L)));
        }
        return buckets;
    }

    private List<QuizStatsResponse.HardQuestion> hardestQuestions(UUID quizId) {
        return repository.findHardestQuestions(quizId).stream()
                .filter(row -> row.getAnsweredCount() >= MIN_ANSWERS_FOR_HARD_QUESTION)
                .filter(row -> row.getWrongCount() > 0)
                .limit(HARD_QUESTION_LIMIT)
                .map(row -> new QuizStatsResponse.HardQuestion(
                        row.getQuestionId(), row.getContent(), row.getTopic(),
                        row.getAnsweredCount(), row.getWrongCount(),
                        round(row.getWrongCount() * 100.0 / row.getAnsweredCount())))
                .toList();
    }

    /** Null khi chưa có dữ liệu — 0% nghĩa là làm mà sai hết, khác hẳn chưa làm gì. */
    private Double averagePercent(long sumScore, long sumMaxScore) {
        return sumMaxScore == 0 ? null : round(sumScore * 100.0 / sumMaxScore);
    }

    private double percent(int score, int maxScore) {
        return maxScore == 0 ? 0 : round(score * 100.0 / maxScore);
    }

    /** Một chữ số thập phân: chính xác hơn thì cũng không ai đọc, mà bảng thì rối. */
    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
