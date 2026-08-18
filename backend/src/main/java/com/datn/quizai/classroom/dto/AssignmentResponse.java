package com.datn.quizai.classroom.dto;

import com.datn.quizai.classroom.domain.Assignment;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một bài tập (features/14, FR-55 & FR-56).
 * <p>
 * Dùng chung cho <b>hai màn hình</b>, khác nhau ở bốn trường cuối:
 * <ul>
 *   <li><b>Giáo viên</b> xem danh sách bài đã giao — bốn trường cuối là {@code null}.</li>
 *   <li><b>Học sinh</b> xem bài được giao cho mình — bốn trường cuối nói tình trạng của <i>chính họ</i>.</li>
 * </ul>
 * Một DTO cho hai màn thay vì hai DTO gần giống nhau: phần chung chiếm tám trên mười hai trường, và tách ra
 * thì mỗi lần thêm một trường vào bài tập phải sửa hai chỗ.
 *
 * @param trangThai chỉ khác null ở màn của học sinh
 * @param attemptId lượt làm bài của chính người gọi; null nếu chưa làm
 */
public record AssignmentResponse(
        UUID id,
        UUID classroomId,
        String tenLop,
        String title,
        String instruction,
        UUID quizId,
        String quizTitle,
        int soCau,
        OffsetDateTime openAt,
        OffsetDateTime dueAt,
        OffsetDateTime createdAt,

        TrangThaiBaiTap trangThai,
        String trangThaiNhan,
        UUID attemptId,
        Integer diem,
        Integer diemToiDa
) {
    /** Bản cho giáo viên: không có phần "của tôi". */
    public static AssignmentResponse choGiaoVien(Assignment a) {
        return dung(a, null, null, null, null);
    }

    public static AssignmentResponse choHocSinh(Assignment a, TrangThaiBaiTap trangThai,
                                                UUID attemptId, Integer diem, Integer diemToiDa) {
        return dung(a, trangThai, attemptId, diem, diemToiDa);
    }

    private static AssignmentResponse dung(Assignment a, TrangThaiBaiTap trangThai,
                                           UUID attemptId, Integer diem, Integer diemToiDa) {
        return new AssignmentResponse(
                a.getId(), a.getClassroom().getId(), a.getClassroom().getName(),
                a.getTitle(), a.getInstruction(),
                a.getQuiz().getId(), a.getQuiz().getTitle(), a.getQuiz().getQuizQuestions().size(),
                a.getOpenAt(), a.getDueAt(), a.getCreatedAt(),
                trangThai, trangThai == null ? null : trangThai.nhan(), attemptId, diem, diemToiDa);
    }
}
