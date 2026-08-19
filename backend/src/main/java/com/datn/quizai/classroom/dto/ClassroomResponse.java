package com.datn.quizai.classroom.dto;

import com.datn.quizai.classroom.domain.Classroom;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một lớp học nhìn từ phía người gọi (features/14, FR-54).
 *
 * @param vaiTroCuaToi OWNER | CO_TEACHER | STUDENT — quyết định giao diện hiện gì. Tính ở máy chủ chứ không
 *                     để frontend so {@code ownerId} với id của mình: cùng một câu hỏi mà hai đầu tự trả lời
 *                     thì sớm muộn hai câu trả lời lệch nhau, và lệch ở đây nghĩa là học sinh thấy nút giao
 *                     bài
 * @param classCode    <b>chỉ trả cho chủ nhiệm và trợ giảng</b>, null với học sinh — mã lớp là thứ để mời
 *                     người vào, không phải thông tin mọi thành viên cần cầm
 */
public record ClassroomResponse(
        UUID id,
        String name,
        String description,
        String classCode,
        String ownerName,
        long soThanhVien,
        long soBaiTap,
        String vaiTroCuaToi,
        OffsetDateTime createdAt
) {
    public static ClassroomResponse of(Classroom lop, String vaiTro, long soThanhVien, long soBaiTap) {
        boolean laGiaoVien = "OWNER".equals(vaiTro) || "CO_TEACHER".equals(vaiTro);
        return new ClassroomResponse(
                lop.getId(), lop.getName(), lop.getDescription(),
                laGiaoVien ? lop.getClassCode() : null,
                lop.getOwner().getDisplayName(),
                soThanhVien, soBaiTap, vaiTro, lop.getCreatedAt());
    }
}
