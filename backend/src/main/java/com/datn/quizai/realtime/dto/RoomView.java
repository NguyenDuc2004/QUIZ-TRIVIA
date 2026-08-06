package com.datn.quizai.realtime.dto;

import com.datn.quizai.realtime.domain.GameRoom;
import com.datn.quizai.realtime.domain.RoomState;
import com.datn.quizai.realtime.domain.RoomStatus;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Ảnh chụp toàn bộ phòng tại một thời điểm — dùng cho REST {@code GET /rooms/{code}}.
 * <p>
 * Đây cũng là <b>đường phục hồi khi mất kết nối</b> (FR-25): client vừa kết nối lại chỉ cần gọi
 * endpoint này là dựng lại được màn hình đúng trạng thái, không cần phát lại lịch sử sự kiện.
 *
 * @param currentQuestion câu đang hỏi; null khi phòng chưa bắt đầu hoặc đã kết thúc
 */
public record RoomView(
        String roomCode,
        UUID quizId,
        String quizTitle,
        UUID hostId,
        String hostDisplayName,
        RoomStatus status,
        boolean allowGuests,
        int totalQuestions,
        int readyCount,
        List<RoomPlayerView> players,
        LiveQuestionView currentQuestion,
        int answeredCount
) {
    public static RoomView of(GameRoom room, RoomState state, LiveQuestionView currentQuestion) {
        return new RoomView(
                room.getRoomCode(),
                room.getQuiz().getId(),
                room.getQuiz().getTitle(),
                room.getHost().getId(),
                room.getHost().getDisplayName(),
                state.status(),
                room.isAllowGuests(),
                state.totalQuestions(),
                state.readyCount(),
                ranking(state),
                currentQuestion,
                state.answeredCurrent().size());
    }

    /** Xếp hạng người chơi và đánh số thứ tự từ 1. */
    public static List<RoomPlayerView> ranking(RoomState state) {
        List<RoomState.PlayerState> sorted = state.ranking();
        return IntStream.range(0, sorted.size())
                .mapToObj(i -> RoomPlayerView.of(i + 1, sorted.get(i)))
                .toList();
    }
}
