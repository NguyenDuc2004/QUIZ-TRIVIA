package com.datn.quizai.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một phòng đấu đang chạy, nhìn từ khu quản trị (features/10, FR-46).
 * <p>
 * Ghép dữ liệu từ <b>hai nguồn</b>: metadata bền (mã phòng, chủ phòng, quiz) lấy từ PostgreSQL, còn số
 * người chơi và câu hiện tại lấy từ trạng thái ở Redis. Đúng cách hai nơi này phân chia trách nhiệm —
 * xem `features/04`.
 *
 * @param soNguoiChoi    {@code null} khi phòng có bản ghi trong cơ sở dữ liệu nhưng <b>không còn trạng
 *                       thái ở Redis</b>. Đó không phải lỗi hiển thị mà là một dấu hiệu vận hành: phòng
 *                       đã hết TTL hoặc tiến trình xử lý nó đã chết, tức phòng treo. Trả null để giao
 *                       diện nói được điều đó thay vì hiện "0 người" như một phòng trống bình thường
 * @param cauHienTai     số thứ tự câu đang phát, 1-based cho người đọc; null khi chưa vào ván
 */
public record LiveRoomResponse(
        UUID id,
        String roomCode,
        String tenChuPhong,
        String tenQuiz,
        String status,
        Integer soNguoiChoi,
        Integer cauHienTai,
        Integer tongSoCau,
        boolean choKhachVao,
        OffsetDateTime taoLuc,
        OffsetDateTime batDauLuc,
        boolean treo
) {
}
