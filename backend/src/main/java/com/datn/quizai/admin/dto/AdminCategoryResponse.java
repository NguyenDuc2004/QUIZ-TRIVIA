package com.datn.quizai.admin.dto;

import java.util.UUID;

/**
 * Danh mục kèm <b>số quiz đang dùng nó</b> (features/10, FR-44).
 * <p>
 * Con số đó là thứ quyết định quản trị viên có xoá được danh mục hay không, nên phải hiện ngay trong
 * danh sách chứ không để họ bấm xoá rồi mới nhận lỗi 409. Biết trước thì họ chuyển quiz sang danh mục
 * khác trước; biết sau thì họ chỉ thấy một thao tác thất bại không rõ lý do.
 */
public record AdminCategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        long soQuiz
) {
}
