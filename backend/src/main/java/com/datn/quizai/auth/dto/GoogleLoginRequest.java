package com.datn.quizai.auth.dto;

import com.datn.quizai.user.domain.Role;
import jakarta.validation.constraints.NotBlank;

/**
 * Đăng nhập bằng Google.
 *
 * @param idToken ID token do Google Identity Services cấp cho frontend. Backend tự xác minh chữ ký
 *                với Google — không tin bất cứ thông tin nào client tự khai kèm theo.
 */
public record GoogleLoginRequest(
        @NotBlank(message = "Thiếu ID token của Google")
        String idToken,

        /**
         * Vai trò mong muốn, <b>chỉ có tác dụng khi tạo tài khoản mới</b>.
         *
         * <h4>Vì sao "chỉ khi tạo mới" là điều kiện bắt buộc, không phải chi tiết</h4>
         * Endpoint này dùng chung cho cả <i>đăng nhập</i> lẫn <i>đăng ký</i> — Google không phân biệt hai
         * việc đó. Nếu áp vai trò ở mọi lần gọi thì bất kỳ ai cũng tự lên CREATOR bằng cách <b>đăng nhập
         * lại</b> và gửi kèm trường này. Tài khoản đã tồn tại luôn giữ nguyên vai trò đang có.
         *
         * <h4>Cùng luật với đăng ký thường</h4>
         * Bỏ trống hoặc {@code ADMIN} đều thành {@code LEARNER}. ADMIN chỉ được cấp bởi Admin sẵn có
         * (docs/security.md §1) — đó mới là ranh giới an ninh thật, không phải CREATOR.
         */
        Role role
) {
}
