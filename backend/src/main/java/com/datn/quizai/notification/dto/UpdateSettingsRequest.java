package com.datn.quizai.notification.dto;

import com.datn.quizai.notification.domain.NotificationType;

import java.util.Set;

/**
 * Đặt lại danh sách loại thông báo bị tắt.
 * <p>
 * Là <b>đặt lại toàn bộ</b>, không phải bật/tắt từng loại một: gửi cả trạng thái thì hai tab mở song song
 * không chồng lên nhau theo kiểu khó hiểu, và client không phải tính hiệu của hai tập hợp.
 *
 * @param disabledTypes null hoặc rỗng đều nghĩa là bật tất cả — người dùng bỏ chọn hết thì client gửi mảng
 *                      rỗng, còn null là client cũ chưa biết trường này
 */
public record UpdateSettingsRequest(Set<NotificationType> disabledTypes) {

    public Set<NotificationType> safe() {
        return disabledTypes == null ? Set.of() : disabledTypes;
    }
}
