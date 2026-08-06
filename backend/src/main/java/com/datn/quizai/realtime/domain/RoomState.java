package com.datn.quizai.realtime.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Trạng thái <b>đang chơi</b> của một phòng, lưu ở Redis key {@code room:{code}} dưới dạng JSON
 * (docs/database.md §3). Cố tình không để trong PostgreSQL: mỗi lượt trả lời đều đọc-ghi trạng
 * thái này, đi qua CSDL quan hệ sẽ chậm và tạo tải ghi vô ích.
 * <p>
 * Bản ghi <b>bất biến</b>: mọi thay đổi tạo ra một {@code RoomState} mới rồi ghi đè. Nhờ vậy
 * không có trạng thái chia sẻ bị sửa ngầm giữa các luồng; phần đồng bộ do khoá Redis ở
 * {@code RoomStateStore} lo.
 *
 * @param currentIndex            thứ tự câu đang hỏi, {@code -1} khi chưa bắt đầu
 * @param questionStartedAtMillis mốc thời gian <b>của server</b> khi phát câu hỏi — dùng để tính
 *                                tốc độ trả lời; không bao giờ tin thời gian client gửi lên
 * @param questionDeadlineMillis  hạn nhận đáp án cho câu hiện tại
 * @param answeredCurrent         những người đã trả lời câu hiện tại (mỗi câu chỉ trả lời một lần)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RoomState(
        String roomCode,
        UUID quizId,
        UUID hostId,
        RoomStatus status,
        int currentIndex,
        int totalQuestions,
        long questionStartedAtMillis,
        long questionDeadlineMillis,
        List<PlayerState> players,
        Set<UUID> answeredCurrent
) {
    /**
     * Một người chơi trong phòng.
     *
     * @param playerId id định danh trong phòng — với người đã đăng nhập là id tài khoản, với khách
     *                 là một UUID ngẫu nhiên sinh lúc vào phòng. Nhờ dùng chung một kiểu, phần
     *                 tính điểm và xếp hạng không cần biết ai là khách ai là thành viên.
     * @param guest    true = khách vãng lai, dùng để hiển thị nhãn và để biết có ghi điểm cuối
     *                 xuống {@code game_room_players} kèm user_id hay không
     * @param ready    người chơi đã bấm "Sẵn sàng" ở phòng chờ chưa
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlayerState(UUID playerId, String displayName, PlayerAvatar avatar,
                              boolean guest, boolean ready, int score, int correctCount) {

        public PlayerState plus(int points, boolean correct) {
            return new PlayerState(playerId, displayName, avatar, guest, ready,
                    score + points, correctCount + (correct ? 1 : 0));
        }

        public PlayerState withReady(boolean value) {
            return new PlayerState(playerId, displayName, avatar, guest, value, score, correctCount);
        }

        public PlayerState withAvatar(PlayerAvatar value) {
            return new PlayerState(playerId, displayName, value, guest, ready, score, correctCount);
        }
    }

    public static RoomState waiting(String roomCode, UUID quizId, UUID hostId, int totalQuestions) {
        return new RoomState(roomCode, quizId, hostId, RoomStatus.WAITING, -1, totalQuestions,
                0L, 0L, List.of(), Set.of());
    }

    // ---------------------------------------------------------------- biến đổi

    /** Thêm người chơi; đã có trong phòng thì giữ nguyên (vào lại không mất điểm). */
    public RoomState withPlayer(UUID playerId, String displayName, PlayerAvatar avatar, boolean guest) {
        if (hasPlayer(playerId)) {
            return this;
        }
        List<PlayerState> next = new ArrayList<>(players);
        next.add(new PlayerState(playerId, displayName, avatar, guest, false, 0, 0));
        return withPlayers(next);
    }

    public RoomState withoutPlayer(UUID playerId) {
        return withPlayers(players.stream().filter(p -> !p.playerId().equals(playerId)).toList());
    }

    public RoomState withReady(UUID playerId, boolean ready) {
        return withPlayers(players.stream()
                .map(p -> p.playerId().equals(playerId) ? p.withReady(ready) : p)
                .toList());
    }

    /** Đổi avatar ngay trong phòng chờ — người đã đăng nhập cũng đổi sang nhân vật vui được. */
    public RoomState withAvatar(UUID playerId, PlayerAvatar avatar) {
        return withPlayers(players.stream()
                .map(p -> p.playerId().equals(playerId) ? p.withAvatar(avatar) : p)
                .toList());
    }

    /** Chuyển sang câu tiếp theo, đặt lại mốc thời gian và danh sách đã trả lời. */
    public RoomState withQuestion(int index, long startedAtMillis, long deadlineMillis) {
        return new RoomState(roomCode, quizId, hostId, RoomStatus.PLAYING, index, totalQuestions,
                startedAtMillis, deadlineMillis, players, Set.of());
    }

    /** Cộng điểm cho một người và đánh dấu họ đã trả lời câu hiện tại. */
    public RoomState withAnswer(UUID playerId, int points, boolean correct) {
        List<PlayerState> next = players.stream()
                .map(p -> p.playerId().equals(playerId) ? p.plus(points, correct) : p)
                .toList();

        Set<UUID> answered = new LinkedHashSet<>(answeredCurrent);
        answered.add(playerId);

        return new RoomState(roomCode, quizId, hostId, status, currentIndex, totalQuestions,
                questionStartedAtMillis, questionDeadlineMillis, next, answered);
    }

    public RoomState finished() {
        return new RoomState(roomCode, quizId, hostId, RoomStatus.FINISHED, currentIndex, totalQuestions,
                questionStartedAtMillis, questionDeadlineMillis, players, answeredCurrent);
    }

    private RoomState withPlayers(List<PlayerState> next) {
        return new RoomState(roomCode, quizId, hostId, status, currentIndex, totalQuestions,
                questionStartedAtMillis, questionDeadlineMillis, next, answeredCurrent);
    }

    // ---------------------------------------------------------------- truy vấn

    @JsonIgnore
    public boolean hasPlayer(UUID playerId) {
        return players.stream().anyMatch(p -> p.playerId().equals(playerId));
    }

    @JsonIgnore
    public boolean hasAnswered(UUID playerId) {
        return answeredCurrent.contains(playerId);
    }

    /** Mọi người chơi đều đã trả lời câu hiện tại → có thể sang câu kế tiếp ngay. */
    @JsonIgnore
    public boolean everyoneAnswered() {
        return !players.isEmpty() && answeredCurrent.size() >= players.size();
    }

    @JsonIgnore
    public int readyCount() {
        return (int) players.stream().filter(PlayerState::ready).count();
    }

    @JsonIgnore
    public boolean isLastQuestion() {
        return currentIndex >= totalQuestions - 1;
    }

    /** Bảng xếp hạng: điểm cao trước, đồng điểm thì nhiều câu đúng hơn xếp trên. */
    @JsonIgnore
    public List<PlayerState> ranking() {
        return players.stream()
                .sorted(Comparator.comparingInt(PlayerState::score).reversed()
                        .thenComparing(Comparator.comparingInt(PlayerState::correctCount).reversed()))
                .toList();
    }
}
