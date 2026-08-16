package com.datn.quizai.admin.dto;

import java.util.List;

/**
 * Trạng thái cấu hình các nhà cung cấp AI (features/10, FR-83).
 * <p>
 * <b>Không có trường nào mang giá trị khoá API</b>, và đó là điều kiện tiên quyết của endpoint này —
 * `security.md` quy định không hiển thị khoá API trong UI hay log. Thứ quản trị viên cần để chẩn đoán
 * "vì sao AI không chạy" chỉ là <i>đã cấu hình hay chưa</i>, không phải giá trị.
 * <p>
 * Cũng không có system prompt: prompt là nơi đặt bốn lớp chống tiêm chỉ thị khi chấm bài, đọc được qua
 * API là bước đầu để sửa được nó.
 *
 * @param thuTuUuTien thứ tự thử nhà cung cấp, theo `app.ai.provider-order`. Hiện thứ tự này ra vì nó
 *                    giải thích được vì sao một lời gọi lại đi qua nhà cung cấp dự phòng
 */
public record AiConfigResponse(
        List<ProviderStatus> nhaCungCap,
        List<String> thuTuUuTien,
        boolean coTheGoiAi,
        int soLuongThuLaiTacVuNen
) {
    /**
     * @param daCauHinh có khoá API hay không — <b>chỉ true/false</b>, không kèm giá trị
     * @param sanSang   đã cấu hình VÀ nằm trong thứ tự ưu tiên; một nhà cung cấp có khoá nhưng bị loại
     *                  khỏi `provider-order` thì vẫn không được gọi, và đó là chuyện dễ nhầm nên tách
     *                  thành cờ riêng
     */
    public record ProviderStatus(
            String ten,
            boolean daCauHinh,
            boolean sanSang,
            boolean hoTroEmbedding,
            boolean hoTroStreaming
    ) {
    }
}
