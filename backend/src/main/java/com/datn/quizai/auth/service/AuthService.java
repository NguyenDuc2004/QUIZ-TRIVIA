package com.datn.quizai.auth.service;

import com.datn.quizai.auth.dto.AuthResponse;
import com.datn.quizai.auth.dto.ChangePasswordRequest;
import com.datn.quizai.auth.dto.ForgotPasswordRequest;
import com.datn.quizai.auth.dto.ResetPasswordRequest;
import com.datn.quizai.auth.dto.LoginRequest;
import com.datn.quizai.auth.dto.RegisterRequest;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.user.domain.Role;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
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
    private final PasswordResetOtpService otpService;
    private final MailService mailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       PasswordResetOtpService otpService,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.otpService = otpService;
        this.mailService = mailService;
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

        // Thu hồi MỌI phiên, kể cả phiên đang gọi. Người dùng đổi mật khẩu thường tin là mình vừa
        // cắt hết truy cập — nếu không làm bước này thì chiếc điện thoại bị mất vẫn vào được tới
        // hết hạn refresh token (14 ngày). Client phải đăng nhập lại sau khi đổi mật khẩu.
        int revoked = refreshTokenService.revokeAll(userId);
        log.info("Người dùng {} đã đổi mật khẩu, thu hồi {} phiên", userId, revoked);
    }

    /**
     * Bước 1 của quên mật khẩu (FR-4): gửi mã OTP tới email.
     * <p>
     * <b>Luôn trả về như nhau</b> dù email có tồn tại hay không. Nếu báo "email không tồn tại"
     * thì bất kỳ ai cũng dò được danh sách người dùng của hệ thống chỉ bằng cách thử từng địa chỉ
     * — cùng lý do với việc thông báo đăng nhập sai không nói rõ sai email hay sai mật khẩu.
     * <p>
     * Riêng lỗi 429 (xin mã quá dày) vẫn phải báo, vì đó là phản hồi cho chính hành vi của người
     * gọi chứ không tiết lộ gì về dữ liệu.
     */
    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        otpService.assertCanSend(email);

        userRepository.findByEmail(email).ifPresentOrElse(user -> {
            String code = otpService.issue(email);
            mailService.sendPasswordResetOtp(email, user.getDisplayName(), code, otpService.ttlMinutes());
        }, () -> log.info("Yêu cầu đặt lại mật khẩu cho email không tồn tại — bỏ qua, vẫn trả 204"));
    }

    /**
     * Bước 2 của quên mật khẩu: đổi mật khẩu bằng mã OTP.
     * <p>
     * Xác minh mã <b>trước</b> khi tra người dùng: nếu tra người dùng trước rồi mới kiểm mã, thời
     * gian phản hồi giữa "email không tồn tại" và "mã sai" sẽ khác nhau, đủ để dò email.
     * <p>
     * Đổi xong thu hồi mọi phiên — người vừa lấy lại tài khoản cần chắc chắn kẻ chiếm dụng bị đá ra.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.email());
        otpService.verifyAndConsume(email, request.otp());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> BusinessException.badRequest("Mã xác thực không đúng hoặc đã hết hạn"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        int revoked = refreshTokenService.revokeAll(user.getId());

        log.info("Người dùng {} đã đặt lại mật khẩu qua OTP, thu hồi {} phiên", user.getId(), revoked);
    }

    /**
     * Đăng xuất khỏi mọi thiết bị.
     * <p>
     * Cần cho tình huống mất máy: đăng xuất trên thiết bị đang cầm không giúp gì, vì phiên nằm ở
     * chiếc máy đã mất.
     *
     * @return số phiên đã thu hồi, để giao diện báo lại cho người dùng
     */
    public int logoutAllDevices(UUID userId) {
        return refreshTokenService.revokeAll(userId);
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
