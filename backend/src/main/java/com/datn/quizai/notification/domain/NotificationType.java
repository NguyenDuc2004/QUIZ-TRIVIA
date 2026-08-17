package com.datn.quizai.notification.domain;

/**
 * Loại thông báo (features/16, FR-65).
 * <p>
 * Hai loại cuối <b>chưa có nguồn phát</b>: {@code ASSIGNMENT_DUE} chờ tính năng 14 (lớp học),
 * {@code ROOM_INVITE} chờ cơ chế mời của tính năng 04 — phòng đấu hiện vào bằng mã PIN, không có lời mời.
 * Khai sẵn ở đây và trong ràng buộc {@code CHECK} của V18 để hai tính năng đó không cần thêm migration chỉ để
 * thêm một giá trị.
 * <p>
 * Nhưng <b>không</b> đưa chúng ra trang cài đặt: một công tắc bật/tắt cho loại thông báo chưa ai gửi là một
 * công tắc không làm gì cả — cùng lý do đã hoãn ô nhập hạn mức AI ở tính năng 10 (FR-84).
 */
public enum NotificationType {

    /** Có thẻ ghi nhớ đến hạn ôn hôm nay (features/11, FR-66). */
    SRS_REMINDER("Nhắc ôn tập"),

    /** Lên cấp hoặc mở khoá huy hiệu (features/13, FR-53). */
    ACHIEVEMENT("Thành tích"),

    /** Bài tập sắp hết hạn — chưa có nguồn phát, chờ tính năng 14. */
    ASSIGNMENT_DUE("Hạn nộp bài"),

    /** Lời mời vào phòng đấu — chưa có nguồn phát, chờ tính năng 04. */
    ROOM_INVITE("Lời mời phòng đấu"),

    /** Thông báo hệ thống. Người dùng <b>không tắt được</b> loại này — xem {@link #tatDuoc()}. */
    SYSTEM("Hệ thống");

    private final String nhan;

    NotificationType(String nhan) {
        this.nhan = nhan;
    }

    public String nhan() {
        return nhan;
    }

    /** Có nguồn phát thật ở thời điểm hiện tại — quyết định loại nào hiện trên trang cài đặt. */
    public boolean daCoNguonPhat() {
        return this == SRS_REMINDER || this == ACHIEVEMENT || this == SYSTEM;
    }

    /**
     * Người dùng tắt được loại này hay không.
     * <p>
     * {@code SYSTEM} không tắt được vì nó là kênh để nói những việc người dùng <i>cần</i> biết (bảo trì, đổi
     * điều khoản, sự cố dữ liệu). Cho tắt kênh đó là để người dùng tự bỏ tai nghe rồi mình lại yên tâm là đã
     * thông báo. Bù lại: chỉ dùng {@code SYSTEM} cho việc thật, không dùng cho tiếp thị.
     */
    public boolean tatDuoc() {
        return this != SYSTEM;
    }
}
