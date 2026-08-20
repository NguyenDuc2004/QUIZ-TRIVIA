package com.datn.quizai.realtime.repository;

import com.datn.quizai.realtime.domain.RoomProctoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomProctoringEventRepository extends JpaRepository<RoomProctoringEvent, UUID> {

    /** Toàn bộ tín hiệu của một phòng — bản tổng kết host xem sau ván. */
    List<RoomProctoringEvent> findByRoomIdOrderByOccurredAtAsc(UUID roomId);

    /**
     * Tín hiệu của <b>một người</b> trong một phòng, theo thứ tự thời gian.
     * <p>
     * Thứ tự là điều kiện đúng đắn của {@code RoomFlagDetector}, không phải để hiển thị cho đẹp: khuôn lặp
     * chỉ tính khi {@code TAB_HIDDEN} đứng <b>trước</b> {@code TAB_VISIBLE} ở cùng số câu.
     * <p>
     * Kích thước danh sách này cũng là cái chặn trên số dòng mỗi người ghi được — tín hiệu tới qua STOMP
     * từng cái một nên không có "kích thước lô" để giới hạn như đường REST của bài thi. Không chặn thì một
     * client hỏng bơm được số dòng không giới hạn bằng cách bật/tắt tab liên tục.
     */
    List<RoomProctoringEvent> findByRoomIdAndPlayerIdOrderByOccurredAtAsc(UUID roomId, UUID playerId);
}
