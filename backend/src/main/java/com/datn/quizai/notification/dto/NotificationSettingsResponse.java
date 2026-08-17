package com.datn.quizai.notification.dto;

import com.datn.quizai.notification.domain.NotificationType;

import java.util.List;
import java.util.Set;

/**
 * Cài đặt thông báo (features/16, FR-70 phần bật/tắt theo loại).
 * <p>
 * Trả kèm <b>danh sách loại điều chỉnh được</b> thay vì để frontend tự liệt kê: loại nào đã có nguồn phát là
 * việc của máy chủ, và nếu frontend tự hardcode thì nó sẽ hiện công tắc cho loại chưa ai gửi — một công tắc
 * không làm gì cả, đúng cái đã hoãn ở FR-84 (ô nhập hạn mức AI).
 *
 * @param disabledTypes loại người dùng đã tắt
 * @param dieuChinhDuoc loại hiện ra trên trang cài đặt, kèm nhãn tiếng Việt
 */
public record NotificationSettingsResponse(
        Set<NotificationType> disabledTypes,
        List<LoaiCoTheTat> dieuChinhDuoc
) {
    public record LoaiCoTheTat(NotificationType type, String nhan) {
    }

    public static NotificationSettingsResponse of(Set<NotificationType> disabled) {
        List<LoaiCoTheTat> dieuChinh = java.util.Arrays.stream(NotificationType.values())
                .filter(NotificationType::daCoNguonPhat)
                .filter(NotificationType::tatDuoc)
                .map(t -> new LoaiCoTheTat(t, t.nhan()))
                .toList();

        return new NotificationSettingsResponse(disabled, dieuChinh);
    }
}
