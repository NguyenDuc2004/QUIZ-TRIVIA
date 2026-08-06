package com.datn.quizai.auth.controller;

import com.datn.quizai.auth.service.AuthService;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.auth.dto.AuthResponse;
import com.datn.quizai.auth.dto.ChangePasswordRequest;
import com.datn.quizai.auth.dto.LoginRequest;
import com.datn.quizai.auth.dto.RefreshRequest;
import com.datn.quizai.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint xác thực — docs/api.md §1. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Xác thực", description = "Đăng ký, đăng nhập, làm mới token, đăng xuất")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản mới (mặc định vai trò LEARNER)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập, trả access token + refresh token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới access token; refresh token cũ bị thu hồi (rotation)")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất — thu hồi refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Đăng xuất khỏi mọi thiết bị — dùng khi mất máy")
    public java.util.Map<String, Integer> logoutAll(
            @AuthenticationPrincipal JwtService.AuthenticatedUser currentUser) {
        return java.util.Map.of("revokedSessions", authService.logoutAllDevices(currentUser.id()));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu của chính mình. Thu hồi MỌI phiên, kể cả phiên đang gọi — "
            + "client phải đăng nhập lại.")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal JwtService.AuthenticatedUser currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(currentUser.id(), request);
        return ResponseEntity.noContent().build();
    }
}
