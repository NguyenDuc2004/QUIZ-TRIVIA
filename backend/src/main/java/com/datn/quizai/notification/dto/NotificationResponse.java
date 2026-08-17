package com.datn.quizai.notification.dto;

import com.datn.quizai.notification.domain.Notification;
import com.datn.quizai.notification.domain.NotificationType;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một thông báo nhìn từ phía client (features/16, FR-68).
 * <p>
 * DTO này đi <b>hai chiều</b>, và đó là lý do nó phức tạp hơn một record thường:
 * <ul>
 *   <li><b>Ra</b> — trả về từ REST, và đẩy xuống STOMP.</li>
 *   <li><b>Vào</b> — {@link com.datn.quizai.notification.service.NotificationRelay} đọc lại nó từ gói tin
 *       Redis trước khi chuyển tiếp.</li>
 * </ul>
 *
 * @param data cột {@code jsonb} lưu dưới dạng {@code String}, nhưng client phải nhận được một <b>đối tượng</b>
 *             chứ không phải một chuỗi bị escape — không thì client nhận
 *             {@code "data": "{\"deckId\":\"...\"}"} và phải tự {@code JSON.parse}, một bước dịch thừa nằm
 *             sai phía mà mỗi client lại tự làm một kiểu.
 *             <p>
 *             {@code @JsonRawValue} lo chiều ra. Chiều vào <b>bắt buộc</b> phải có
 *             {@link RawJsonDeserializer}: {@code @JsonRawValue} chỉ tác dụng khi ghi, nên nếu thiếu thì
 *             Jackson gặp một đối tượng JSON ở chỗ chờ {@code String} và ném lỗi — mà chỗ ném là trong
 *             listener Redis, tức là thông báo real-time lặng lẽ không tới ai và người dùng chỉ thấy nó ở lần
 *             tải trang sau. Kiểu lỗi rất khó lần ra, nên có test riêng cho vòng ra-vào.
 */
public record NotificationResponse(
        UUID id,
        NotificationType type,
        /** Nhãn tiếng Việt của loại, để giao diện không phải tự dịch tên enum. */
        String loaiNhan,
        String title,
        String body,
        @JsonRawValue @JsonDeserialize(using = RawJsonDeserializer.class) String data,
        boolean read,
        OffsetDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getType().nhan(), n.getTitle(), n.getBody(),
                n.getData(), n.isRead(), n.getCreatedAt());
    }

    /**
     * Đọc một nhánh JSON bất kỳ trở lại thành chuỗi JSON nguyên bản — chiều ngược của
     * {@code @JsonRawValue}.
     * <p>
     * Không xử lý null ở đây: Jackson không gọi deserializer cho {@code null} tường minh mà dùng
     * {@code getNullValue()}, nên {@code data} null vẫn ra null.
     */
    public static class RawJsonDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return p.readValueAsTree().toString();
        }
    }
}
