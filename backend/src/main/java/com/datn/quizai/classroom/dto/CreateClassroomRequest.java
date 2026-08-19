package com.datn.quizai.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClassroomRequest(

        @NotBlank(message = "Tên lớp không được để trống")
        @Size(max = 150, message = "Tên lớp tối đa 150 ký tự")
        String name,

        @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
        String description
) {
}
