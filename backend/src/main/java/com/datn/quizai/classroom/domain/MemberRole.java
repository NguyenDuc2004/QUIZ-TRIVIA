package com.datn.quizai.classroom.domain;

/**
 * Vai trò của một thành viên trong lớp (features/14, FR-59).
 * <p>
 * <b>Chủ nhiệm không có trong enum này</b> — họ là {@code classrooms.owner_id}. Thêm một giá trị
 * {@code OWNER} vào đây là tạo hai nguồn sự thật cho cùng một câu hỏi "ai là chủ lớp", và sớm muộn hai
 * nguồn lệch nhau (xoá dòng thành viên thì lớp còn chủ không?).
 */
public enum MemberRole {

    /** Học sinh: thấy bài được giao và kết quả của chính mình. */
    STUDENT("Học sinh"),

    /**
     * Trợ giảng: quyền như chủ nhiệm trừ việc xoá lớp và đổi vai trò người khác.
     * <p>
     * Hai việc đó giữ riêng cho chủ nhiệm vì chúng không hoàn tác được: xoá lớp là mất toàn bộ bài tập và
     * điểm, còn cho phép trợ giảng tự nâng người khác lên trợ giảng là mở một đường để quyền lan ra mà chủ
     * nhiệm không biết.
     */
    CO_TEACHER("Trợ giảng");

    private final String nhan;

    MemberRole(String nhan) {
        this.nhan = nhan;
    }

    public String nhan() {
        return nhan;
    }
}
