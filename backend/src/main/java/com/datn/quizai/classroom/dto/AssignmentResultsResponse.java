package com.datn.quizai.classroom.dto;

import java.util.List;

/**
 * Bảng theo dõi một bài tập (features/14, FR-57).
 *
 * @param diemTrungBinh tính trên <b>bài đã nộp</b>, null khi chưa ai nộp. Tính cả người chưa làm như 0 điểm
 *                      thì con số này nói về tỉ lệ nộp chứ không nói về chất lượng bài — hai câu hỏi khác
 *                      nhau, và {@code tiLeHoanThanh} đã trả lời câu kia rồi
 */
public record AssignmentResultsResponse(
        AssignmentResponse baiTap,
        long soThanhVien,
        long soDaNop,
        long soNopTre,
        Integer diemTrungBinh,
        List<AssignmentResultRow> danhSach
) {
}
