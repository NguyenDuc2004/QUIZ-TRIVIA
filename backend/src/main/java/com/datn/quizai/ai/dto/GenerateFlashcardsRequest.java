package com.datn.quizai.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Yêu cầu sinh thẻ ghi nhớ từ học liệu (features/11, FR-38).
 *
 * @param materialId <b>bắt buộc</b>. Khác sinh đề — nơi bỏ chọn học liệu thì sinh theo kiến thức chung —
 *                   sinh thẻ luôn cần nguồn: thẻ được ôn đi ôn lại hàng chục lần trong nhiều tháng nên một
 *                   thẻ sai sẽ được <i>học thuộc</i>, và người duyệt cần tài liệu để đối chiếu
 * @param deckId     bộ thẻ sẽ nhận thẻ sau khi duyệt. <b>Không có {@code @NotNull}</b> vì client không gửi
 *                   trường này — nó lấy từ đường dẫn qua {@link #withDeckId(UUID)}, và validate của
 *                   {@code @Valid} chạy trên thân yêu cầu trước khi controller kịp gán. Lưu vào job để lúc
 *                   duyệt không phải chọn lại bộ, và để bộ đích không thể bị đổi giữa hai bước
 */
public record GenerateFlashcardsRequest(
        @NotNull(message = "Phải chọn một học liệu để sinh thẻ")
        UUID materialId,

        UUID deckId,

        @Size(max = 200, message = "Chủ đề tối đa 200 ký tự")
        String topic,

        @Min(value = 1, message = "Số thẻ phải từ 1")
        @Max(value = 30, message = "Mỗi lần sinh tối đa 30 thẻ")
        int count
) {
    /**
     * Gán lại bộ thẻ lấy từ đường dẫn.
     * <p>
     * Bộ thẻ nằm ở đường dẫn ({@code /decks/{deckId}/cards/generate}) nên client không cần gửi lại trong
     * thân yêu cầu. Hàm này giữ {@code deckId} là <b>một nguồn duy nhất</b>: nếu nhận cả hai chỗ thì sớm
     * muộn chúng lệch nhau, và không rõ chỗ nào thắng.
     */
    public GenerateFlashcardsRequest withDeckId(UUID deckId) {
        return new GenerateFlashcardsRequest(materialId, deckId, topic, count);
    }
}
