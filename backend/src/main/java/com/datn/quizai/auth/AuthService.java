package com.datn.quizai.auth;

import com.datn.quizai.auth.dto.AuthResponse;
import com.datn.quizai.auth.dto.ChangePasswordRequest;
import com.datn.quizai.auth.dto.LoginRequest;
import com.datn.quizai.auth.dto.RegisterRequest;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.user.Role;
import com.datn.quizai.user.User;
import com.datn.quizai.user.UserRepository;
import com.datn.quizai.user.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Nghiệp vụ đăng ký, đăng nhập, làm mới token, đăng xuất, đổi mật khẩu. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw BusinessException.conflict("Email này đã được sử dụng");
        }

        // ADMIN chỉ được cấp bởi Admin sẵn có, không cho tự đăng ký (docs/security.md §1)
        Role role = (request.role() == null || request.role() == Role.ADMIN)
                ? Role.LEARNER
                : request.role();

        User user = userRepository.save(new User(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                role));

        log.info("Đã tạo tài khoản mới: id={} role={}", user.getId(), role);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                // Cùng một thông báo cho email sai và mật khẩu sai → không tiết lộ email nào tồn tại
                .orElseThrow(() -> BusinessException.unauthorized("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("Email hoặc mật khẩu không đúng");
        }

        return issueTokens(user);
    }

    /** Rotation: refresh token cũ bị thu hồi, chỉ dùng được một lần. */
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        UUID userId = refreshTokenService.resolve(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.unauthorized("Tài khoản không còn tồn tại"));

        String newRefreshToken = refreshTokenService.rotate(refreshToken);
        return AuthResponse.of(
                jwtService.generateAccessToken(user),
                newRefreshToken,
                jwtService.accessTtlSeconds(),
                UserResponse.from(user));
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw BusinessException.badRequest("Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        log.info("Người dùng {} đã đổi mật khẩu", userId);
    }

    private AuthResponse issueTokens(User user) {
        return AuthResponse.of(
                jwtService.generateAccessToken(user),
                refreshTokenService.issue(user.getId()),
                jwtService.accessTtlSeconds(),
                UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
