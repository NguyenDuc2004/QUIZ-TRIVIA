package com.datn.quizai.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Thêm hoặc sửa một danh mục (features/10, FR-44).
 *
 * @param slug để trống thì server tự sinh từ {@code name}. Cho phép nhập tay vì slug nằm trên đường dẫn
 *             công khai — đổi tên danh mục không nên làm chết đường dẫn cũ, nên hai thứ tách nhau
 */
public record CategoryRequest(

        @NotBlank(message = "Tên danh mục không được để trống")
        @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
        String name,

        @Size(max = 100, message = "Slug tối đa 100 ký tự")
        @Pattern(regexp = "^$|^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "Slug chỉ gồm chữ thường, số và dấu gạch ngang")
        String slug,

        @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
        String description
) {
}
