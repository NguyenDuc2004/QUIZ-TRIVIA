package com.datn.quizai.attempt.service;

import com.datn.quizai.ai.grading.GradingPromptBuilder;
import com.datn.quizai.attempt.domain.AnswerPayload;
import com.datn.quizai.attempt.domain.AttemptAnswer;
import com.datn.quizai.attempt.domain.AttemptMode;
import com.datn.quizai.attempt.domain.AttemptStatus;
import com.datn.quizai.attempt.domain.GradedBy;
import com.datn.quizai.attempt.domain.QuizAttempt;
import com.datn.quizai.attempt.dto.AnswerFeedbackResponse;
import com.datn.quizai.attempt.dto.AttemptDetailResponse;
import com.datn.quizai.attempt.dto.AttemptSummaryResponse;
import com.datn.quizai.attempt.dto.ExplanationResponse;
import com.datn.quizai.attempt.dto.GradingViewResponse;
import com.datn.quizai.attempt.dto.LeaderboardEntryResponse;
import com.datn.quizai.attempt.dto.OverrideGradeRequest;
import com.datn.quizai.attempt.dto.StartAttemptRequest;
import com.datn.quizai.attempt.dto.SubmitAnswerRequest;
import com.datn.quizai.attempt.repository.QuizAttemptRepository;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.OwnershipGuard;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.QuestionOption;
import com.datn.quizai.quiz.domain.QuestionType;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.quiz.domain.QuizQuestion;
import com.datn.quizai.quiz.domain.Visibility;
import com.datn.quizai.quiz.repository.QuizRepository;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ làm bài quiz đơn (docs/features/03-gameplay.md, FR-13…FR-19).
 * <p>
 * Ba nguyên tắc chi phối toàn bộ lớp này:
 * <ol>
 *   <li><b>Chốt đề lúc bắt đầu</b> — mọi câu hỏi được sao thành dòng {@code attempt_answers}
 *       kèm điểm tối đa, nên chủ quiz sửa đề giữa chừng không làm hỏng bài đang làm.</li>
 *   <li><b>Không lộ đáp án khi chưa nộp</b> — đáp án đúng chỉ nằm trong response sau khi bài
 *       chuyển sang trạng thái kết thúc (riêng chế độ luyện tập thì lộ từng câu vừa trả lời).</li>
 *   <li><b>Bài của ai người ấy xem</b> — truy cập nhầm trả 404 để không tiết lộ bài đó tồn tại.
 *       Ngoại lệ duy nhất: {@link #gradingView} cho chủ quiz đọc <i>phần tự luận</i> của bài làm
 *       trên quiz mình sở hữu, vì không đọc được bài thì không chấm tay được (features/09).</li>
 * </ol>
 */
@Service
public class AttemptService {

    /** Số dòng tối đa trả về cho bảng xếp hạng. */
    private static final int LEADERBOARD_LIMIT = 50;

    private final QuizAttemptRepository attemptRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher events;
    private final AttemptGradeWriter gradeWriter;
    private final AiGradingService aiGradingService;
    private final com.datn.quizai.ai.provider.AiThrottleState aiThrottleState;

    public AttemptService(QuizAttemptRepository attemptRepository,
                          QuizRepository quizRepository,
                          UserRepository userRepository,
                          ApplicationEventPublisher events,
                          AttemptGradeWriter gradeWriter,
                          AiGradingService aiGradingService,
                          com.datn.quizai.ai.provider.AiThrottleState aiThrottleState) {
        this.attemptRepository = attemptRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.events = events;
        this.gradeWriter = gradeWriter;
        this.aiGradingService = aiGradingService;
        this.aiThrottleState = aiThrottleState;
    }

    /**
     * Bắt đầu làm bài (FR-13).
     * <p>
     * Nếu người dùng đang có bài dở trên chính quiz này thì <b>trả lại bài đó để làm tiếp</b>
     * thay vì tạo bài mới — tránh việc tải lại trang là mất hết câu đã trả lời. Bài dở đã
     * quá giờ thì được nộp tự động trước, rồi mới mở bài mới.
     */
    @Transactional
    public AttemptDetailResponse start(UUID quizId, StartAttemptRequest request,
                                       JwtService.AuthenticatedUser current) {
        Quiz quiz = loadPlayableQuiz(quizId, current);
        OffsetDateTime now = OffsetDateTime.now();

        var existing = attemptRepository.findByUserAndQuizAndStatus(
                current.id(), quizId, AttemptStatus.IN_PROGRESS);
        if (existing.isPresent()) {
            QuizAttempt attempt = attemptRepository.findByIdWithAnswers(existing.get().getId()).orElseThrow();
            if (!attempt.isExpiredAt(now)) {
                return detailOf(attempt);
            }
            finish(attempt, AttemptStatus.EXPIRED, attempt.getExpiresAt());
        }

        List<QuizQuestion> questions = quiz.getQuizQuestions().stream()
                .sorted(Comparator.comparingInt(QuizQuestion::getOrderIndex))
                .toList();
        if (questions.isEmpty()) {
            throw BusinessException.badRequest("Quiz này chưa có câu hỏi nào để làm");
        }

        User user = userRepository.getReferenceById(current.id());
        QuizAttempt attempt = new QuizAttempt(user, quiz, request.modeOrDefault());
        attempt.setStartedAt(now);
        if (quiz.getTimeLimitSec() != null) {
            attempt.setExpiresAt(now.plusSeconds(quiz.getTimeLimitSec()));
        }

        int index = 0;
        int maxScore = 0;
        for (QuizQuestion quizQuestion : questions) {
            AttemptAnswer answer = new AttemptAnswer(quizQuestion.getQuestion(), index++);
            attempt.addAnswer(answer);
            maxScore += answer.getMaxScore();
        }
        attempt.setMaxScore(maxScore);

        return detailOf(attemptRepository.save(attempt));
    }

    /**
     * Xem một bài làm: đang làm dở thì trả đề đã giấu đáp án, đã nộp thì trả kết quả đầy đủ
     * kèm giải thích từng câu (FR-17).
     */
    @Transactional
    public AttemptDetailResponse getDetail(UUID attemptId, JwtService.AuthenticatedUser current) {
        QuizAttempt attempt = loadOwnAttempt(attemptId, current);
        finishIfExpired(attempt, OffsetDateTime.now());
        return detailOf(attempt);
    }

    /**
     * Ghi nhận câu trả lời (FR-13). Ở chế độ luyện tập, câu này được chấm và trả đáp án ngay;
     * ở chế độ thi chỉ lưu lại, chấm khi nộp (FR-14).
     */
    @Transactional
    public AnswerFeedbackResponse answer(UUID attemptId, SubmitAnswerRequest request,
                                         JwtService.AuthenticatedUser current) {
        QuizAttempt attempt = loadOwnAttempt(attemptId, current);

        if (attempt.getStatus().isFinished()) {
            throw BusinessException.conflict("Bài làm đã kết thúc, không sửa được câu trả lời");
        }
        // Không tự nộp ở đây: ném lỗi sẽ rollback nên việc nộp cũng mất. Lần gọi
        // GET/submit kế tiếp sẽ chốt bài — cùng dữ liệu nên kết quả không đổi.
        if (attempt.isExpiredAt(OffsetDateTime.now())) {
            throw BusinessException.conflict("Đã hết giờ làm bài, hãy nộp bài để xem kết quả");
        }

        AttemptAnswer answer = attempt.getAnswers().stream()
                .filter(a -> a.getQuestion().getId().equals(request.questionId()))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Câu hỏi này không nằm trong đề của bài làm"));

        if (attempt.getMode() == AttemptMode.PRACTICE
                && answer.getGradedBy() != GradedBy.NOT_GRADED) {
            throw BusinessException.conflict("Câu này đã được chấm ở chế độ luyện tập, không trả lời lại");
        }

        AnswerPayload payload = buildPayload(answer.getQuestion(), request);
        answer.setUserAnswer(payload);
        answer.setAnsweredAt(OffsetDateTime.now());

        int answeredCount = (int) attempt.getAnswers().stream().filter(AttemptAnswer::isAnswered).count();
        int questionCount = attempt.getAnswers().size();

        if (attempt.getMode() != AttemptMode.PRACTICE) {
            return AnswerFeedbackResponse.saved(request.questionId(), payload, answeredCount, questionCount);
        }

        // Chế độ luyện tập: chấm ngay và hiện đáp án + giải thích
        applyGrade(answer);
        Question question = answer.getQuestion();
        return new AnswerFeedbackResponse(
                request.questionId(), payload, answeredCount, questionCount,
                answer.getCorrect(), answer.getScore(),
                question.getOptions().stream()
                        .filter(QuestionOption::isCorrect)
                        .map(QuestionOption::getId)
                        .toList(),
                question.getExplanation());
    }

    /**
     * Nộp bài và chấm (FR-15). Gọi lại trên bài đã nộp thì trả về đúng kết quả cũ
     * (idempotent) thay vì báo lỗi — người dùng bấm nộp hai lần hoặc mạng chập chờn là chuyện thường.
     */
    @Transactional
    public AttemptDetailResponse submit(UUID attemptId, JwtService.AuthenticatedUser current) {
        QuizAttempt attempt = loadOwnAttempt(attemptId, current);

        if (!attempt.getStatus().isFinished()) {
            OffsetDateTime now = OffsetDateTime.now();
            if (attempt.isExpiredAt(now)) {
                finish(attempt, AttemptStatus.EXPIRED, attempt.getExpiresAt());
            } else {
                finish(attempt, AttemptStatus.SUBMITTED, now);
            }
        }

        return detailOf(attempt);
    }

    /** Lịch sử làm bài của chính người dùng (FR-18). */
    @Transactional(readOnly = true)
    public PageResponse<AttemptSummaryResponse> history(UUID userId, UUID quizId, Pageable pageable) {
        Page<QuizAttempt> page = attemptRepository.findHistory(userId, quizId, pageable);

        // Đếm số câu/đúng cho cả trang bằng một truy vấn gộp thay vì nạp answers từng bài
        Map<UUID, AttemptSummaryResponse.Counts> counts = attemptRepository
                .countAnswersByAttemptIds(page.getContent().stream().map(QuizAttempt::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        QuizAttemptRepository.AttemptCountRow::getAttemptId,
                        row -> new AttemptSummaryResponse.Counts(
                                (int) row.getQuestionCount(),
                                (int) row.getAnsweredCount(),
                                (int) row.getCorrectCount())));

        return PageResponse.of(page, attempt -> AttemptSummaryResponse.of(
                attempt, counts.getOrDefault(attempt.getId(), AttemptSummaryResponse.Counts.ZERO)));
    }

    /** Bảng xếp hạng của một quiz (FR-19) — chỉ tính bài đã kết thúc, mỗi người một bài tốt nhất. */
    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> leaderboard(UUID quizId, JwtService.AuthenticatedUser current) {
        loadPlayableQuiz(quizId, current);

        List<QuizAttemptRepository.LeaderboardRow> rows = attemptRepository.findBestAttemptPerUser(quizId);

        // `distinct on` của PostgreSQL bắt buộc sắp theo user_id trước, nên thứ hạng phải xếp lại ở đây
        List<QuizAttemptRepository.LeaderboardRow> ranked = rows.stream()
                .sorted(Comparator
                        .comparingInt(QuizAttemptRepository.LeaderboardRow::getTotalScore).reversed()
                        .thenComparing(row -> durationSec(row.getStartedAt(), row.getSubmittedAt())))
                .limit(LEADERBOARD_LIMIT)
                .toList();

        return IntStream.range(0, ranked.size())
                .mapToObj(i -> {
                    var row = ranked.get(i);
                    return new LeaderboardEntryResponse(
                            i + 1,
                            row.getUserId(),
                            row.getDisplayName(),
                            row.getTotalScore(),
                            row.getMaxScore(),
                            durationSec(row.getStartedAt(), row.getSubmittedAt()),
                            row.getSubmittedAt().atOffset(ZoneOffset.UTC));
                })
                .toList();
    }

    /**
     * Chủ quiz chấm tay một câu, ghi đè điểm AI (docs/features/06 §Use case).
     * <p>
     * Đây là <b>ngoại lệ có chủ đích</b> của luật "bài của ai người ấy xem": mọi API làm bài khác
     * đều chặn chủ quiz đọc bài người khác, nhưng chấm tay thì buộc phải xem được bài. Bù lại,
     * phạm vi hẹp hết mức — chỉ chủ đúng quiz đó (hoặc Admin), chỉ sửa được điểm và nhận xét của
     * một câu, không đọc được danh sách bài làm của ai.
     * <p>
     * Điểm bị ép về [0, maxScore]: chấm tay vẫn không được vượt trần của câu, nếu không bảng xếp
     * hạng sẽ có người điểm cao hơn điểm tối đa của quiz.
     */
    @Transactional
    public AttemptDetailResponse overrideGrade(UUID attemptId, UUID answerId,
                                               OverrideGradeRequest request,
                                               JwtService.AuthenticatedUser current) {
        QuizAttempt attempt = attemptRepository.findByIdWithAnswers(attemptId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy bài làm"));

        requireQuizOwner(attempt, current);

        if (!attempt.getStatus().isFinished()) {
            throw BusinessException.conflict("Bài này chưa nộp, chưa chấm được");
        }

        AttemptAnswer answer = attempt.getAnswers().stream()
                .filter(a -> a.getId().equals(answerId))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy câu trả lời trong bài này"));

        gradeWriter.applyHumanGrade(attempt, answer, request.score(), request.feedback());
        return detailOf(attempt);
    }

    /**
     * Bài làm nhìn từ phía người chấm — chỉ câu tự luận (features/09, trả nợ features/06).
     * <p>
     * Đây là <b>ngoại lệ có chủ đích</b> của luật "bài của ai người ấy xem", và là ngoại lệ duy nhất:
     * chủ quiz đọc được phần tự luận của bài làm trên quiz mình sở hữu. Không có nó thì
     * {@link #overrideGrade} vô nghĩa — chấm mà không đọc được bài thì chấm bằng gì. Phạm vi bị bó
     * đúng bằng mục đích: chỉ câu {@code SHORT_ANSWER}, chỉ trên quiz mình sở hữu.
     */
    @Transactional(readOnly = true)
    public GradingViewResponse gradingView(UUID attemptId, JwtService.AuthenticatedUser current) {
        QuizAttempt attempt = attemptRepository.findByIdWithAnswers(attemptId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy bài làm"));

        requireQuizOwner(attempt, current);

        if (!attempt.getStatus().isFinished()) {
            throw BusinessException.conflict("Bài này chưa nộp, chưa chấm được");
        }

        List<GradingViewResponse.EssayAnswer> answers = attempt.getAnswers().stream()
                .filter(a -> a.getQuestion().getType() == QuestionType.SHORT_ANSWER)
                .sorted(Comparator.comparing(AttemptAnswer::getOrderIndex))
                .map(AttemptService::toEssayAnswer)
                .toList();

        return new GradingViewResponse(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                attempt.getUser().getDisplayName(),
                attempt.getSubmittedAt(),
                attempt.getTotalScore(),
                attempt.getMaxScore(),
                answers);
    }

    private static GradingViewResponse.EssayAnswer toEssayAnswer(AttemptAnswer answer) {
        Question question = answer.getQuestion();
        AnswerPayload payload = answer.getUserAnswer();

        return new GradingViewResponse.EssayAnswer(
                answer.getId(),
                answer.getOrderIndex(),
                question.getContent(),
                question.getRubric(),
                GradingPromptBuilder.sampleAnswer(question),
                payload == null ? null : payload.text(),
                answer.getScore(),
                answer.getMaxScore(),
                answer.getGradedBy(),
                answer.getAiFeedback(),
                answer.getAiSuggestions(),
                answer.getGradedBy() == GradedBy.PENDING_AI || answer.getGradedBy() == GradedBy.AI_FAILED);
    }

    /**
     * Nhờ AI giải thích một câu trong bài đã nộp (docs/features/06 §Ghi chú kỹ thuật).
     * <p>
     * Tách khỏi luồng chấm: với câu có đáp án cố định thì chấm đã xong bằng logic, gọi mô hình
     * thêm chỉ tốn tiền mà không chính xác hơn — nên AI ở đây <b>chỉ giải thích</b>.
     * <p>
     * Gọi đồng bộ vì người dùng chủ động bấm và đứng chờ. Chỉ cho phép trên bài đã nộp: giải thích
     * trước khi nộp chính là đường vòng để lấy đáp án.
     */
    @Transactional(readOnly = true)
    public ExplanationResponse explain(UUID attemptId, UUID answerId,
                                       JwtService.AuthenticatedUser current) {
        QuizAttempt attempt = loadOwnAttempt(attemptId, current);

        if (!attempt.getStatus().isFinished()) {
            throw BusinessException.conflict("Nộp bài xong mới xem được giải thích");
        }

        AttemptAnswer answer = attempt.getAnswers().stream()
                .filter(a -> a.getId().equals(answerId))
                .findFirst()
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy câu trả lời trong bài này"));

        String userText = answer.getUserAnswer() == null ? null : answer.getUserAnswer().text();
        return new ExplanationResponse(
                aiGradingService.explain(answer.getQuestion(), userText, current.id()));
    }

    /**
     * Dựng response kèm thông tin "AI đang bị chặn hạn mức bao lâu nữa".
     * <p>
     * Chỉ hỏi Redis khi bài <b>còn câu chờ chấm</b> — bài đã xong thì hạn mức chẳng liên quan gì,
     * mà endpoint này bị gọi lại liên tục lúc đang chờ nên không nên chạm Redis vô ích.
     */
    private AttemptDetailResponse detailOf(QuizAttempt attempt) {
        boolean waiting = attempt.getAnswers().stream().anyMatch(AttemptAnswer::isAwaitingAi);
        return AttemptDetailResponse.from(attempt, waiting ? aiThrottleState.secondsRemaining() : 0);
    }

    // ------------------------------------------------------------------ nội bộ

    /**
     * Nạp quiz mà người dùng được phép làm.
     * Quiz riêng tư của người khác trả 404 (không phải 403) để không tiết lộ nó tồn tại —
     * cùng quy ước với {@code QuizService.getSummary}.
     */
    private Quiz loadPlayableQuiz(UUID quizId, JwtService.AuthenticatedUser current) {
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));

        if (quiz.getVisibility() == Visibility.PRIVATE
                && !OwnershipGuard.canManage(quiz.getOwner().getId(), current)) {
            throw BusinessException.notFound("Không tìm thấy quiz");
        }
        return quiz;
    }

    /**
     * Nạp bài làm của <b>chính</b> người gọi. Bài của người khác trả 404: bài làm là dữ liệu
     * riêng tư, chủ quiz hay Admin cũng không xem qua API này (thống kê nằm ở features/09).
     */
    private QuizAttempt loadOwnAttempt(UUID attemptId, JwtService.AuthenticatedUser current) {
        QuizAttempt attempt = attemptRepository.findByIdWithAnswers(attemptId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy bài làm"));

        if (!attempt.getUser().getId().equals(current.id())) {
            throw BusinessException.notFound("Không tìm thấy bài làm");
        }
        return attempt;
    }

    /**
     * Chỉ chủ quiz (hoặc Admin) mới đi tiếp. Trả <b>404</b> chứ không phải 403 — cùng quy ước với
     * chỗ khác: người không có quyền thì không được biết bài làm đó có tồn tại hay không.
     */
    private void requireQuizOwner(QuizAttempt attempt, JwtService.AuthenticatedUser current) {
        UUID ownerId = attempt.getQuiz().getOwner().getId();
        if (!OwnershipGuard.canManage(ownerId, current)) {
            throw BusinessException.notFound("Không tìm thấy bài làm");
        }
    }

    private void finishIfExpired(QuizAttempt attempt, OffsetDateTime now) {
        if (!attempt.getStatus().isFinished() && attempt.isExpiredAt(now)) {
            finish(attempt, AttemptStatus.EXPIRED, attempt.getExpiresAt());
        }
    }

    /**
     * Chấm mọi câu chưa chấm, cộng điểm và đóng bài.
     * <p>
     * Câu tự luận không chấm được ở đây — {@code AnswerGrader} trả {@link GradedBy#PENDING_AI} và
     * để lại 0 điểm. Tổng điểm lúc này là <b>điểm tạm</b>: đủ để trả kết quả ngay cho phần trắc
     * nghiệm, còn phần tự luận do {@link AiGradingService} chấm nền rồi cộng lại (features/06).
     * Chấm đồng bộ tại đây thì người học bấm "Nộp bài" xong phải ngồi chờ hàng chục giây và
     * request có thể timeout giữa chừng.
     */
    private void finish(QuizAttempt attempt, AttemptStatus status, OffsetDateTime submittedAt) {
        int total = 0;
        for (AttemptAnswer answer : attempt.getAnswers()) {
            if (answer.getGradedBy() == GradedBy.NOT_GRADED) {
                applyGrade(answer);
            }
            total += answer.getScore();
        }
        attempt.setTotalScore(total);
        attempt.setStatus(status);
        attempt.setSubmittedAt(submittedAt);

        // Phát cho MỌI bài nộp, không chỉ bài có câu tự luận: chấm AI và đồng bộ đồ thị gợi ý
        // (features/07) cùng nghe sự kiện này. Bên nào không có việc thì tự thoát sớm — rẻ hơn
        // nhiều so với việc mỗi bên cần một sự kiện riêng rồi quên phát một cái.
        //
        // Người nhận chạy ở pha AFTER_COMMIT: khởi động luồng nền ngay bây giờ thì nó đọc CSDL
        // trước khi những thay đổi trên kịp commit và không thấy gì.
        events.publishEvent(new AttemptSubmittedEvent(attempt.getId(), attempt.getUser().getId()));
    }

    private void applyGrade(AttemptAnswer answer) {
        AnswerGrader.GradeResult result = AnswerGrader.grade(answer.getQuestion(), answer.getUserAnswer());
        answer.setCorrect(result.correct());
        answer.setScore(result.score());
        answer.setGradedBy(result.gradedBy());
    }

    /**
     * Chuẩn hóa dữ liệu client gửi thành payload đúng với loại câu hỏi, đồng thời chặn
     * những id lựa chọn không thuộc câu hỏi này (client sửa request).
     */
    private AnswerPayload buildPayload(Question question, SubmitAnswerRequest request) {
        if (!question.getType().isChoiceBased()) {
            return AnswerPayload.ofText(request.text());
        }

        List<UUID> selected = request.optionIds() == null ? List.of() : request.optionIds().stream().distinct().toList();
        Set<UUID> valid = question.getOptions().stream().map(QuestionOption::getId).collect(Collectors.toSet());

        if (!valid.containsAll(selected)) {
            throw BusinessException.badRequest("Lựa chọn không thuộc câu hỏi này");
        }
        if (selected.size() > 1 && question.getType() != QuestionType.MULTIPLE_CHOICE) {
            throw BusinessException.badRequest("Câu hỏi này chỉ được chọn một đáp án");
        }
        return AnswerPayload.ofOptions(selected);
    }

    /**
     * Thời gian làm bài của một dòng bảng xếp hạng. Bài không có mốc nộp thì trả giá trị lớn nhất
     * để nó xếp cuối khi so kè cùng điểm — trên thực tế không xảy ra vì truy vấn chỉ lấy bài đã kết thúc.
     */
    private Integer durationSec(Instant startedAt, Instant submittedAt) {
        if (startedAt == null || submittedAt == null) {
            return Integer.MAX_VALUE;
        }
        return (int) Duration.between(startedAt, submittedAt).toSeconds();
    }
}
