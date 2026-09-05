package com.datn.quizai.classroom.dto;

import com.datn.quizai.integrity.domain.ReviewStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một dòng trong bảng theo dõi lớp (features/14, FR-57).
 * <p>
 * Có <b>một dòng cho mỗi thành viên</b>, kể cả người chưa làm — đó chính là thứ giáo viên cần nhìn. Chỉ trả
 * người đã nộp thì bảng "theo dõi" biến thành bảng "điểm", và câu hỏi thật sự (*ai chưa làm?*) không có chỗ
 * nào trả lời.
 *
 * @param diem null khi chưa nộp — khác 0 điểm, và trộn hai thứ đó là làm hỏng cả điểm trung bình lẫn cách
 *             đọc bảng
 */
public record AssignmentResultRow(
        UUID userId,
        String tenHocSinh,
        UUID attemptId,
        Integer diem,
        Integer diemToiDa,
        OffsetDateTime nopLuc,
        TrangThaiBaiTap trangThai,
        String trangThaiNhan,
        Integer riskScore,
        ReviewStatus reviewStatus
) {
}
