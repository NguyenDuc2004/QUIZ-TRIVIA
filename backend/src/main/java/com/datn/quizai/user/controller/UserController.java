package com.datn.quizai.user.controller;

import com.datn.quizai.user.service.UserService;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.user.dto.UpdateProfileRequest;
import com.datn.quizai.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Hồ sơ người dùng — docs/api.md §2. Toàn bộ endpoint yêu cầu đăng nhập. */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Người dùng", description = "Hồ sơ cá nhân")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Hồ sơ của người đang đăng nhập")
    public UserResponse me(@AuthenticationPrincipal JwtService.AuthenticatedUser currentUser) {
        return userService.getProfile(currentUser.id());
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật tên hiển thị / ảnh đại diện")
    public UserResponse updateMe(@AuthenticationPrincipal JwtService.AuthenticatedUser currentUser,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUser.id(), request);
    }
}
