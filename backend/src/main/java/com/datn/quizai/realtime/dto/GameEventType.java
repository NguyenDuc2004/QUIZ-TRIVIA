package com.datn.quizai.realtime.dto;

/**
 * Các loại sự kiện server phát trong phòng đấu (docs/api.md §5.2).
 * <p>
 * Trừ {@link #ANSWER_RESULT} gửi riêng cho từng người, tất cả đều phát cho cả phòng.
 */
public enum GameEventType {
    /** Có người vào phòng — kèm danh sách người chơi mới nhất. */
    PLAYER_JOINED,
    /** Có người rời phòng. */
    PLAYER_LEFT,
    /** Host bấm bắt đầu. */
    GAME_STARTED,
    /** Câu hỏi mới, phát đồng thời cho mọi người. <b>Không kèm đáp án đúng.</b> */
    QUESTION,
    /**
     * Có thêm người trả lời xong — chỉ báo <b>số lượng</b>, không nói ai đúng ai sai.
     * Để host biết còn chờ bao nhiêu người mà không lộ kết quả giữa chừng.
     */
    PLAYER_ANSWERED,
    /** Kết quả riêng của người vừa trả lời — gửi <b>chỉ cho người đó</b>. */
    ANSWER_RESULT,
    /** Hết câu: bây giờ mới công bố đáp án đúng và giải thích cho cả phòng. */
    QUESTION_CLOSED,
    /** Bảng xếp hạng cập nhật. */
    LEADERBOARD,
    /** Hết ván, kèm bảng xếp hạng chung cuộc. */
    GAME_FINISHED
}
