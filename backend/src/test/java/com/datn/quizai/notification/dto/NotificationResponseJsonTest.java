package com.datn.quizai.notification.dto;

import com.datn.quizai.notification.domain.NotificationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cột {@code data} là {@code jsonb} trong cơ sở dữ liệu nhưng {@code String} trong Java, và DTO này đi
 * <b>hai chiều</b>: ra REST/STOMP, và <i>vào</i> lại khi {@code NotificationRelay} đọc gói tin từ Redis.
 * <p>
 * Đây là lý do có file test riêng cho một record ba dòng. {@code @JsonRawValue} chỉ tác dụng khi <b>ghi</b>.
 * Thiếu deserializer cho chiều đọc thì Jackson gặp một đối tượng JSON ở chỗ chờ {@code String} và ném lỗi —
 * mà chỗ ném là <i>trong listener Redis</i>. Hậu quả: thông báo real-time lặng lẽ không tới ai, người dùng chỉ
 * thấy nó ở lần tải trang sau, và không có lỗi nào ở đường request để lần ra. Loại lỗi đó phải bị bắt bằng
 * test, không thể bằng cách thử tay.
 */
class NotificationResponseJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private NotificationResponse mau(String data) {
        return new NotificationResponse(
                UUID.randomUUID(), NotificationType.SRS_REMINDER, "Nhắc ôn tập",
                "Bạn có 7 thẻ đến hạn ôn hôm nay", "Ôn đúng hạn giúp nhớ lâu hơn",
                data, false, OffsetDateTime.parse("2026-08-18T07:00:00Z"));
    }

    @Test
    @DisplayName("data ra JSON thành ĐỐI TƯỢNG, không phải chuỗi bị escape")
    void shouldWriteDataAsObject() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(mau("{\"kind\":\"SRS_DUE\",\"soThe\":7}")));

        assertThat(json.get("data").isObject())
                .as("client phải nhận đối tượng, không phải chuỗi cần tự JSON.parse")
                .isTrue();
        assertThat(json.get("data").get("soThe").asInt()).isEqualTo(7);
    }

    @Test
    @DisplayName("Vòng ghi rồi ĐỌC LẠI giữ nguyên data — đây là đường đi qua Redis")
    void shouldSurviveRoundTrip() throws Exception {
        NotificationResponse goc = mau("{\"kind\":\"SRS_DUE\",\"soThe\":7}");

        String tren_duong_truyen = mapper.writeValueAsString(goc);
        NotificationResponse doc_lai = mapper.readValue(tren_duong_truyen, NotificationResponse.class);

        // So sánh theo nội dung JSON, không so chuỗi: Jackson có thể đổi khoảng trắng hay thứ tự khoá, và
        // khẳng định chuỗi giống nhau y hệt là khẳng định một điều mạnh hơn thứ thật sự cần đúng.
        assertThat(mapper.readTree(doc_lai.data())).isEqualTo(mapper.readTree(goc.data()));
        assertThat(doc_lai.title()).isEqualTo(goc.title());
        assertThat(doc_lai.type()).isEqualTo(NotificationType.SRS_REMINDER);
        assertThat(doc_lai.createdAt()).isEqualTo(goc.createdAt());
    }

    @Test
    @DisplayName("data null vẫn là null sau vòng ra-vào, không thành chuỗi \"null\"")
    void shouldKeepNullData() throws Exception {
        // Jackson không gọi deserializer cho null tường minh, nhưng nếu ai đó sửa thành đọc cả null thì
        // `readValueAsTree().toString()` trả về đúng chữ "null" — một chuỗi bốn ký tự trông như dữ liệu thật.
        NotificationResponse doc_lai = mapper.readValue(
                mapper.writeValueAsString(mau(null)), NotificationResponse.class);

        assertThat(doc_lai.data()).isNull();
    }
}
