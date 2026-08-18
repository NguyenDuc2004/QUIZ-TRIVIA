package com.datn.quizai.integrity.dto;

import com.datn.quizai.integrity.domain.ProctoringEventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Lô tín hiệu hành vi client gửi lên (features/12, FR-43).
 * <p>
 * Gửi theo lô chứ không từng sự kiện: chuyển tab liên tục sinh hàng chục sự kiện trong vài giây, và một
 * request cho mỗi sự kiện là tự tạo ra tải không cần thiết ngay lúc người dùng đang thi.
 */
public record ProctoringEventsRequest(
        @NotEmpty(message = "Lô sự kiện không được rỗng")
        @Size(max = 50, message = "Mỗi lô tối đa 50 sự kiện")
        @Valid
        List<Item> events
) {
    /**
     * @param length  độ dài đoạn đã dán — <b>chỉ độ dài</b>, server không nhận và không lưu nội dung
     * @param seconds số giây đã dùng cho câu trả lời, với {@code ANSWER_TOO_FAST}
     */
    public record Item(
            @NotNull(message = "Thiếu loại sự kiện")
            ProctoringEventType type,
            OffsetDateTime occurredAt,
            Integer length,
            Integer seconds
    ) {
    }
}
