package com.datn.quizai.classroom.dto;

import com.datn.quizai.classroom.domain.ClassroomMember;
import com.datn.quizai.classroom.domain.MemberRole;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một thành viên trong danh sách lớp (features/14, FR-54).
 *
 * @param email chỉ giáo viên gọi được endpoint này, nên email hiện ra là chấp nhận được — giáo viên cần phân
 *              biệt hai học sinh trùng tên, và trong lớp thật thì chuyện đó xảy ra thường xuyên
 */
public record MemberResponse(
        UUID userId,
        String displayName,
        String email,
        MemberRole role,
        String vaiTroNhan,
        OffsetDateTime joinedAt
) {
    public static MemberResponse from(ClassroomMember m) {
        return new MemberResponse(
                m.getUser().getId(), m.getUser().getDisplayName(), m.getUser().getEmail(),
                m.getRole(), m.getRole().nhan(), m.getJoinedAt());
    }
}
