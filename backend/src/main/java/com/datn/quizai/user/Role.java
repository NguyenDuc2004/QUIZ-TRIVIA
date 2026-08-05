package com.datn.quizai.user;

/**
 * Vai trò người dùng (docs/overview.md §5).
 * Guest = chưa đăng nhập nên không có giá trị tương ứng trong bảng users.
 */
public enum Role {
    /** Người học: chơi quiz, vào phòng đấu, chatbot, nhận gợi ý. */
    LEARNER,
    /** Người tạo nội dung: quyền của LEARNER + tạo quiz, sinh đề AI, tạo phòng. */
    CREATOR,
    /** Quản trị: quản lý user & nội dung, cấu hình AI, giám sát. */
    ADMIN;

    /** Tên authority dùng trong Spring Security (`hasRole('CREATOR')` → `ROLE_CREATOR`). */
    public String authority() {
        return "ROLE_" + name();
    }
}
