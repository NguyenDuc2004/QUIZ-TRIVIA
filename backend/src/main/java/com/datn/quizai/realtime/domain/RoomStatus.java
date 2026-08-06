package com.datn.quizai.realtime.domain;

/** Vòng đời một phòng đấu. */
public enum RoomStatus {
    /** Đã tạo, đang chờ người chơi vào; host chưa bấm bắt đầu. */
    WAITING,
    /** Ván đang diễn ra. */
    PLAYING,
    /** Đã hết câu hỏi hoặc host kết thúc sớm; điểm cuối đã lưu xuống Postgres. */
    FINISHED
}
