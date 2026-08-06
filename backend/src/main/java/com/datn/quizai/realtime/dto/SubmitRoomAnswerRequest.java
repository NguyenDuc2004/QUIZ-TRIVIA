package com.datn.quizai.realtime.dto;

import java.util.List;
import java.util.UUID;

/**
 * Đáp án người chơi gửi qua STOMP {@code /app/room/{code}/answer}.
 * <p>
 * Cố ý <b>không</b> nhận trường thời gian từ client: server tự đo từ mốc phát câu hỏi,
 * nếu tin client thì ai cũng khai được "trả lời trong 1ms" để ăn trọn điểm tốc độ.
 */
public record SubmitRoomAnswerRequest(UUID questionId, List<UUID> optionIds, String text) {
}
