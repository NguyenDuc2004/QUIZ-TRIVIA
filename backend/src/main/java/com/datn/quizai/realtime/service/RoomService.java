package com.datn.quizai.realtime.service;

import com.datn.quizai.attempt.domain.AnswerPayload;
import com.datn.quizai.attempt.service.AnswerGrader;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.OwnershipGuard;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.quiz.domain.QuizQuestion;
import com.datn.quizai.quiz.domain.Visibility;
import com.datn.quizai.quiz.repository.QuizRepository;
import com.datn.quizai.realtime.domain.GameRoom;
import com.datn.quizai.realtime.domain.GameRoomPlayer;
import com.datn.quizai.realtime.domain.RoomState;
import com.datn.quizai.realtime.domain.RoomStatus;
import com.datn.quizai.realtime.dto.AnswerResultView;
import com.datn.quizai.realtime.dto.CreateRoomRequest;
import com.datn.quizai.realtime.dto.GameEvent;
import com.datn.quizai.realtime.dto.GameEventType;
import com.datn.quizai.realtime.dto.LiveQuestionView;
import com.datn.quizai.realtime.dto.QuestionClosedView;
import com.datn.quizai.realtime.dto.RoomView;
import com.datn.quizai.realtime.dto.SubmitRoomAnswerRequest;
import com.datn.quizai.realtime.repository.GameRoomRepository;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nghiệp vụ phòng đấu thời gian thực (docs/features/04-multiplayer-realtime.md, FR-20…FR-25).
 * <p>
 * Ba điểm quyết định cách viết lớp này:
 * <ol>
 *   <li><b>Tách hai nơi lưu.</b> Metadata và kết quả cuối ở PostgreSQL; trạng thái đang chơi ở
 *       Redis. Mỗi lượt trả lời chỉ chạm Redis, không ghi vào CSDL quan hệ.</li>
 *   <li><b>Thời gian do server đo.</b> Mốc phát câu hỏi nằm trong {@link RoomState}; điểm tốc độ
 *       tính từ hiệu số ở server. Client không gửi lên thời gian của mình được.</li>
 *   <li><b>Đáp án chỉ lộ khi câu đã đóng.</b> Sự kiện QUESTION không mang đáp án đúng; ai trả lời
 *       xong chỉ nhận kết quả của <i>riêng</i> mình qua kênh riêng.</li>
 * </ol>
 */
@Service
public class RoomService {

    /** Bỏ các ký tự dễ đọc nhầm (0/O, 1/I) để người chơi gõ mã phòng không sai. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int CODE_MAX_ATTEMPTS = 20;

    /** Dùng khi cả phòng lẫn câu hỏi đều không cấu hình thời gian. */
    private static final int DEFAULT_SECONDS_PER_QUESTION = 20;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final GameRoomRepository roomRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final RoomStateStore stateStore;
    private final GameEventPublisher publisher;

    public RoomService(GameRoomRepository roomRepository,
                       QuizRepository quizRepository,
                       UserRepository userRepository,
                       RoomStateStore stateStore,
                       GameEventPublisher publisher) {
        this.roomRepository = roomRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.stateStore = stateStore;
        this.publisher = publisher;
    }

    // ------------------------------------------------------------------ REST

    /** Host mở phòng từ một quiz mình chơi được (FR-20). */
    @Transactional
    public RoomView create(CreateRoomRequest request, JwtService.AuthenticatedUser current) {
        Quiz quiz = loadPlayableQuiz(request.quizId(), current);
        List<QuizQuestion> questions = orderedQuestions(quiz);
        if (questions.isEmpty()) {
            throw BusinessException.badRequest("Quiz này chưa có câu hỏi nào để mở phòng");
        }

        User host = userRepository.getReferenceById(current.id());
        GameRoom room = new GameRoom(generateRoomCode(), host, quiz);
        room.setSecondsPerQuestion(request.secondsPerQuestion());
        // Host cũng là một người chơi, cho vào luôn để họ không phải join thêm một bước
        room.addPlayer(new GameRoomPlayer(host));
        roomRepository.save(room);

        RoomState state = RoomState
                .waiting(room.getRoomCode(), quiz.getId(), host.getId(), questions.size())
                .withPlayer(host.getId(), host.getDisplayName());
        stateStore.save(state);

        return RoomView.of(room, state, null);
    }

    /** Người chơi vào phòng bằng mã (FR-20). Vào lại phòng cũ thì giữ nguyên điểm đang có. */
    @Transactional
    public RoomView join(String roomCode, JwtService.AuthenticatedUser current) {
        GameRoom room = requireRoom(roomCode);
        if (room.getStatus() == RoomStatus.FINISHED) {
            throw BusinessException.conflict("Ván đấu này đã kết thúc");
        }

        User user = userRepository.getReferenceById(current.id());
        boolean isNew = room.getPlayers().stream()
                .noneMatch(p -> p.getUser().getId().equals(current.id()));
        if (isNew) {
            room.addPlayer(new GameRoomPlayer(user));
            roomRepository.save(room);
        }

        RoomState state = stateStore.update(roomCode,
                current2 -> current2.withPlayer(current.id(), displayNameOf(current.id())));

        publisher.broadcast(roomCode, GameEvent.of(GameEventType.PLAYER_JOINED,
                Map.of("userId", current.id(), "players", RoomView.ranking(state))));

        return RoomView.of(room, state, currentQuestionView(room, state));
    }

    /**
     * Ảnh chụp phòng — cũng là đường phục hồi sau khi mất kết nối (FR-25):
     * client nối lại rồi gọi endpoint này là dựng lại đúng màn hình đang chơi.
     */
    @Transactional(readOnly = true)
    public RoomView get(String roomCode) {
        GameRoom room = requireRoom(roomCode);
        RoomState state = stateStore.require(roomCode);
        return RoomView.of(room, state, currentQuestionView(room, state));
    }

    /** Rời phòng — chỉ bỏ khỏi trạng thái live, dòng trong CSDL giữ lại để còn thống kê. */
    @Transactional(readOnly = true)
    public void leave(String roomCode, JwtService.AuthenticatedUser current) {
        stateStore.find(roomCode).ifPresent(ignored -> {
            RoomState state = stateStore.update(roomCode, s -> s.withoutPlayer(current.id()));
            publisher.broadcast(roomCode, GameEvent.of(GameEventType.PLAYER_LEFT,
                    Map.of("userId", current.id(), "players", RoomView.ranking(state))));
        });
    }

    // ------------------------------------------------------------------ STOMP

    /** Host bấm bắt đầu → phát câu đầu tiên cho cả phòng cùng lúc (FR-21, FR-22). */
    @Transactional
    public void start(String roomCode, JwtService.AuthenticatedUser current) {
        GameRoom room = requireRoom(roomCode);
        requireHost(room, current);

        if (room.getStatus() != RoomStatus.WAITING) {
            throw BusinessException.conflict("Ván đấu đã bắt đầu hoặc đã kết thúc");
        }

        room.setStatus(RoomStatus.PLAYING);
        room.setStartedAt(OffsetDateTime.now());
        roomRepository.save(room);

        publisher.broadcast(roomCode, GameEvent.of(GameEventType.GAME_STARTED, Map.of()));
        sendQuestion(room, 0);
    }

    /**
     * Người chơi gửi đáp án (FR-22).
     * <p>
     * Chấm lại bằng chính {@code AnswerGrader} của chế độ chơi đơn để hai chế độ không bao giờ
     * cho kết quả khác nhau trên cùng một câu hỏi.
     */
    @Transactional
    public void answer(String roomCode, SubmitRoomAnswerRequest request,
                       JwtService.AuthenticatedUser current) {
        GameRoom room = requireRoom(roomCode);
        RoomState before = stateStore.require(roomCode);

        if (before.status() != RoomStatus.PLAYING) {
            throw BusinessException.conflict("Ván đấu chưa bắt đầu hoặc đã kết thúc");
        }
        if (!before.hasPlayer(current.id())) {
            throw BusinessException.forbidden("Bạn không ở trong phòng này");
        }
        if (before.hasAnswered(current.id())) {
            throw BusinessException.conflict("Bạn đã trả lời câu này rồi");
        }

        Question question = questionAt(room, before.currentIndex());
        if (!question.getId().equals(request.questionId())) {
            // Client gửi chậm một nhịp, câu đã sang câu khác — bỏ qua thay vì chấm nhầm câu
            throw BusinessException.conflict("Câu hỏi đã chuyển, đáp án không còn hợp lệ");
        }

        long now = System.currentTimeMillis();
        if (now > before.questionDeadlineMillis()) {
            throw BusinessException.conflict("Đã hết giờ trả lời câu này");
        }

        // Thời gian do SERVER đo, không lấy từ client
        long elapsed = now - before.questionStartedAtMillis();
        long limit = before.questionDeadlineMillis() - before.questionStartedAtMillis();

        AnswerPayload payload = question.getType().isChoiceBased()
                ? AnswerPayload.ofOptions(request.optionIds() == null ? List.of() : request.optionIds())
                : AnswerPayload.ofText(request.text());

        AnswerGrader.GradeResult graded = AnswerGrader.grade(question, payload);
        boolean correct = Boolean.TRUE.equals(graded.correct());
        int points = SpeedScorer.score(
                question.getPoints() == null ? 1 : question.getPoints(), correct, elapsed, limit);

        RoomState after = stateStore.update(roomCode, s -> s.withAnswer(current.id(), points, correct));

        int totalScore = after.players().stream()
                .filter(p -> p.userId().equals(current.id()))
                .mapToInt(RoomState.PlayerState::score).findFirst().orElse(0);

        publisher.toUser(roomCode, current.id(), GameEvent.of(GameEventType.ANSWER_RESULT,
                new AnswerResultView(question.getId(), correct, points, totalScore, elapsed)));

        // Cả phòng chỉ biết "thêm một người xong", không biết ai đúng ai sai
        publisher.broadcast(roomCode, GameEvent.of(GameEventType.PLAYER_ANSWERED,
                Map.of("answeredCount", after.answeredCurrent().size(),
                        "totalPlayers", after.players().size())));

        // Mọi người đã trả lời thì đóng câu ngay, không bắt cả phòng ngồi chờ hết giờ
        if (after.everyoneAnswered()) {
            closeQuestion(room, after);
        }
    }

    /** Host chuyển câu: đóng câu hiện tại rồi sang câu kế, hết câu thì kết thúc ván. */
    @Transactional
    public void next(String roomCode, JwtService.AuthenticatedUser current) {
        GameRoom room = requireRoom(roomCode);
        requireHost(room, current);

        RoomState state = stateStore.require(roomCode);
        if (state.status() != RoomStatus.PLAYING) {
            throw BusinessException.conflict("Ván đấu chưa bắt đầu hoặc đã kết thúc");
        }

        closeQuestion(room, state);

        if (state.isLastQuestion()) {
            finish(room, state);
        } else {
            sendQuestion(room, state.currentIndex() + 1);
        }
    }

    // ------------------------------------------------------------------ nội bộ

    private void sendQuestion(GameRoom room, int index) {
        Question question = questionAt(room, index);
        int seconds = secondsFor(room, question);

        long startedAt = System.currentTimeMillis();
        long deadline = startedAt + seconds * 1000L;

        RoomState state = stateStore.update(room.getRoomCode(),
                s -> s.withQuestion(index, startedAt, deadline));

        publisher.broadcast(room.getRoomCode(), GameEvent.of(GameEventType.QUESTION,
                LiveQuestionView.of(question, index, state.totalQuestions(), seconds, deadline)));
    }

    /** Công bố đáp án + bảng xếp hạng cho cả phòng. Đây là lúc đáp án đúng mới rời khỏi server. */
    private void closeQuestion(GameRoom room, RoomState state) {
        if (state.currentIndex() < 0) {
            return;
        }
        Question question = questionAt(room, state.currentIndex());

        publisher.broadcast(room.getRoomCode(), GameEvent.of(GameEventType.QUESTION_CLOSED,
                new QuestionClosedView(
                        question.getId(),
                        LiveQuestionView.correctOptionIds(question),
                        question.getExplanation(),
                        RoomView.ranking(state))));

        publisher.broadcast(room.getRoomCode(),
                GameEvent.of(GameEventType.LEADERBOARD, RoomView.ranking(state)));
    }

    /** Kết thúc ván: chốt điểm cuối xuống PostgreSQL rồi phát bảng xếp hạng chung cuộc. */
    private void finish(GameRoom room, RoomState state) {
        GameRoom withPlayers = roomRepository.findByRoomCodeWithPlayers(room.getRoomCode())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng"));

        Map<UUID, Integer> scores = state.players().stream()
                .collect(java.util.stream.Collectors.toMap(
                        RoomState.PlayerState::userId, RoomState.PlayerState::score));

        withPlayers.getPlayers().forEach(player ->
                player.setFinalScore(scores.getOrDefault(player.getUser().getId(), 0)));
        withPlayers.setStatus(RoomStatus.FINISHED);
        withPlayers.setFinishedAt(OffsetDateTime.now());
        roomRepository.save(withPlayers);

        RoomState finished = stateStore.update(room.getRoomCode(), RoomState::finished);

        publisher.broadcast(room.getRoomCode(),
                GameEvent.of(GameEventType.GAME_FINISHED, RoomView.ranking(finished)));
    }

    private GameRoom requireRoom(String roomCode) {
        return roomRepository.findByRoomCode(roomCode.toUpperCase())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng " + roomCode));
    }

    private void requireHost(GameRoom room, JwtService.AuthenticatedUser current) {
        if (!room.getHost().getId().equals(current.id())) {
            throw BusinessException.forbidden("Chỉ chủ phòng mới điều khiển được ván đấu");
        }
    }

    /** Quiz riêng tư của người khác trả 404 — cùng quy ước với chế độ chơi đơn. */
    private Quiz loadPlayableQuiz(UUID quizId, JwtService.AuthenticatedUser current) {
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));

        if (quiz.getVisibility() == Visibility.PRIVATE
                && !OwnershipGuard.canManage(quiz.getOwner().getId(), current)) {
            throw BusinessException.notFound("Không tìm thấy quiz");
        }
        return quiz;
    }

    private List<QuizQuestion> orderedQuestions(Quiz quiz) {
        return quiz.getQuizQuestions().stream()
                .sorted(Comparator.comparingInt(QuizQuestion::getOrderIndex))
                .toList();
    }

    private Question questionAt(GameRoom room, int index) {
        Quiz quiz = quizRepository.findByIdWithQuestions(room.getQuiz().getId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz của phòng"));

        List<QuizQuestion> questions = orderedQuestions(quiz);
        if (index < 0 || index >= questions.size()) {
            throw BusinessException.conflict("Không còn câu hỏi nào");
        }
        return questions.get(index).getQuestion();
    }

    /** Ưu tiên cấu hình của phòng, rồi tới thời gian riêng của câu hỏi, cuối cùng là mặc định. */
    private int secondsFor(GameRoom room, Question question) {
        if (room.getSecondsPerQuestion() != null) {
            return room.getSecondsPerQuestion();
        }
        if (question.getTimeLimitSec() != null) {
            return question.getTimeLimitSec();
        }
        return DEFAULT_SECONDS_PER_QUESTION;
    }

    private String displayNameOf(UUID userId) {
        return userRepository.findById(userId)
                .map(User::getDisplayName)
                .orElse("Người chơi");
    }

    /** Sinh mã phòng ngẫu nhiên, thử lại nếu trùng. */
    private String generateRoomCode() {
        for (int attempt = 0; attempt < CODE_MAX_ATTEMPTS; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            if (!roomRepository.existsByRoomCode(code.toString())) {
                return code.toString();
            }
        }
        throw new IllegalStateException("Không sinh được mã phòng chưa dùng sau nhiều lần thử");
    }

    private LiveQuestionView currentQuestionView(GameRoom room, RoomState state) {
        if (state.status() != RoomStatus.PLAYING || state.currentIndex() < 0) {
            return null;
        }
        Question question = questionAt(room, state.currentIndex());
        int seconds = secondsFor(room, question);
        return LiveQuestionView.of(question, state.currentIndex(), state.totalQuestions(),
                seconds, state.questionDeadlineMillis());
    }
}
