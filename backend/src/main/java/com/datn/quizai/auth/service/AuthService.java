package com.datn.quizai.auth.service;

import com.datn.quizai.auth.dto.AuthResponse;
import com.datn.quizai.auth.dto.ChangePasswordRequest;
import com.datn.quizai.auth.dto.ForgotPasswordRequest;
import com.datn.quizai.auth.dto.ResetPasswordRequest;
import com.datn.quizai.auth.dto.GoogleLoginRequest;
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
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       PasswordResetOtpService otpService,
                       MailService mailService,
                       GoogleTokenVerifier googleTokenVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.otpService = otpService;
        this.mailService = mailService;
        this.googleTokenVerifier = googleTokenVerifier;
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

        requireNotLocked(user);
        return issueTokens(user);
    }

    /**
     * Chặn tài khoản đã bị quản trị viên khoá (features/10).
     * <p>
     * Gọi <b>sau</b> khi đã khớp mật khẩu, không phải trước. Nói "tài khoản bị khoá" cho một người
     * chưa chứng minh được họ là chủ tài khoản chính là tiết lộ email đó có tồn tại — đúng thứ mà
     * thông báo gộp "email hoặc mật khẩu không đúng" đang tránh. Khớp mật khẩu rồi thì họ đã biết
     * mật khẩu, nên câu này không cho thêm thông tin gì cho kẻ tấn công.
     * <p>
     * Ngược lại, với người dùng thật thì phải nói rõ: giữ nguyên thông báo "email hoặc mật khẩu không
     * đúng" sẽ khiến họ đi đặt lại mật khẩu hết lần này lần khác mà vẫn không vào được.
     */
    private void requireNotLocked(User user) {
        if (user.isLocked()) {
            throw BusinessException.forbidden(
                    "Tài khoản đã bị khoá. Vui lòng liên hệ quản trị viên để được mở lại.");
        }
    }

    /** Rotation: refresh token cũ bị thu hồi, chỉ dùng được một lần. */
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        UUID userId = refreshTokenService.resolve(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.unauthorized("Tài khoản không còn tồn tại"));

        // Phải kiểm ở đây nữa, không chỉ ở login: khoá tài khoản có thu hồi mọi phiên, nhưng nếu
        // chặn duy nhất ở login thì bất kỳ đường nào cấp lại refresh token về sau cũng mở lại cửa cho
        // một tài khoản đang bị khoá. Chốt ở cả hai lối vào để trạng thái khoá không phụ thuộc thứ tự
        // xảy ra của các thao tác.
        requireNotLocked(user);

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

        if (!user.hasPassword()) {
            // Tài khoản tạo qua Google chưa từng có mật khẩu — không có "mật khẩu hiện tại" để đối chiếu
            throw BusinessException.badRequest(
                    "Tài khoản này đăng nhập bằng Google nên chưa có mật khẩu. "
                            + "Dùng chức năng Quên mật khẩu để đặt mật khẩu đầu tiên.");
        }
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
     * Đăng nhập bằng Google (FR-3).
     * <p>
     * Ba tình huống, xử lý khác nhau:
     * <ol>
     *   <li><b>Đã liên kết</b> (khớp {@code google_id}) — đăng nhập luôn.</li>
     *   <li><b>Có tài khoản email/mật khẩu cùng địa chỉ</b> — <i>liên kết</i> tài khoản Google vào
     *       đó thay vì tạo tài khoản thứ hai. Chỉ làm được vì Google đã xác minh email; nếu không
     *       thì bất kỳ ai tạo tài khoản Google với email của người khác sẽ chiếm được tài khoản.</li>
     *   <li><b>Hoàn toàn mới</b> — tạo tài khoản không có mật khẩu, vai trò LEARNER.</li>
     * </ol>
     * Tài khoản mới luôn là LEARNER: cho tự chọn vai trò qua tham số là mở đường tự phong CREATOR,
     * cùng lý do với việc đăng ký thường bị hạ vai trò ADMIN.
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleAccount account = googleTokenVerifier.verify(request.idToken());

        User user = userRepository.findByGoogleId(account.subject())
                .orElseGet(() -> linkOrCreate(account));

        // Google đã xác minh xong danh tính nên người gọi chắc chắn là chủ tài khoản — nói rõ lý do
        // bị chặn ở đây không tiết lộ gì thêm
        requireNotLocked(user);

        // Ảnh đại diện có thể đổi bên Google, đồng bộ lại mỗi lần đăng nhập
        if (account.pictureUrl() != null && !account.pictureUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(account.pictureUrl());
        }

        return issueTokens(user);
    }

    private User linkOrCreate(GoogleTokenVerifier.GoogleAccount account) {
        return userRepository.findByEmail(account.email())
                .map(existing -> {
                    existing.setGoogleId(account.subject());
                    log.info("Đã liên kết tài khoản Google vào người dùng sẵn có {}", existing.getId());
                    return existing;
                })
                .orElseGet(() -> {
                    User created = new User(account.email(), null,
                            displayNameOrFallback(account), Role.LEARNER);
                    created.setGoogleId(account.subject());
                    created.setAvatarUrl(account.pictureUrl());
                    log.info("Tạo tài khoản mới từ Google cho {}", account.email());
                    return userRepository.save(created);
                });
    }

    /** Google không phải lúc nào cũng trả tên; lấy phần trước @ làm tên hiển thị tạm. */
    private String displayNameOrFallback(GoogleTokenVerifier.GoogleAccount account) {
        if (account.displayName() != null && !account.displayName().isBlank()) {
            return account.displayName();
        }
        String email = account.email();
        return email.substring(0, email.indexOf('@'));
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
