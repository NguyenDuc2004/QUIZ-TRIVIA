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
    /** Ai đó bật/tắt "Sẵn sàng" ở phòng chờ. */
    PLAYER_READY,
    /** Ai đó đổi avatar trong phòng chờ. */
    PLAYER_AVATAR_CHANGED,
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
    GAME_FINISHED,

    /**
     * Cờ đỏ chống gian lận — gửi <b>chỉ cho host</b> (features/12, cảnh báo live).
     * <p>
     * Đây là loại sự kiện duy nhất mà việc phát cho cả phòng gây hại thật: nó nêu tên một người chơi đang bị
     * nghi, dựa trên tín hiệu client giả mạo được. Kênh phát chung {@code /topic/room/{code}} có mọi người
     * chơi subscribe, nên loại này bắt buộc đi qua {@code GameEventPublisher.toUser}.
     */
    PROCTORING_FLAG,

    /**
     * Lời nhắc của host — gửi <b>chỉ cho người chơi bị nhắc</b> (features/12, cảnh báo live).
     * <p>
     * Cùng lý do riêng tư như {@link #PROCTORING_FLAG}: nhắc riêng thì đủ để người định gian lận biết mình
     * đang bị thấy, còn nêu tên trước cả phòng là một hình phạt công khai không hoàn tác được.
     */
    PROCTORING_WARNING
}
