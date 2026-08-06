package com.datn.quizai.auth.service;

import com.datn.quizai.common.exception.BusinessException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Xác minh ID token do Google cấp cho frontend (FR-3).
 * <p>
 * <b>Vì sao dùng thư viện chính chủ thay vì tự viết:</b> xác minh một ID token gồm tải bộ khoá công
 * khai của Google, chọn đúng khoá theo {@code kid}, kiểm chữ ký RS256, rồi kiểm {@code iss},
 * {@code aud} và hạn dùng — kèm việc làm mới bộ khoá khi Google xoay khoá. Sót bất kỳ bước nào
 * cũng thành lỗ hổng cho phép người khác giả token và đăng nhập thành bất kỳ ai.
 * <p>
 * Riêng {@code aud} là bước hay bị bỏ nhất mà lại quan trọng nhất: không kiểm thì một token Google
 * hợp lệ <i>cấp cho ứng dụng khác</i> vẫn dùng đăng nhập vào đây được.
 */
@Service
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    /** Google phát token với một trong hai giá trị issuer này. */
    private static final List<String> ISSUERS = List.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifier(@Value("${app.oauth.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.verifier = this.clientId.isBlank() ? null : new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(this.clientId))
                .setIssuers(ISSUERS)
                .build();
    }

    public boolean isConfigured() {
        return verifier != null;
    }

    /** Thông tin lấy được từ token đã xác minh. */
    public record GoogleAccount(String subject, String email, boolean emailVerified,
                                String displayName, String pictureUrl) {
    }

    /**
     * @throws BusinessException 503 nếu chưa cấu hình Client ID, 401 nếu token sai/hết hạn/không
     *                           phải cấp cho ứng dụng này
     */
    public GoogleAccount verify(String idTokenString) {
        if (!isConfigured()) {
            throw new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình đăng nhập Google. Thêm GOOGLE_CLIENT_ID vào .env rồi khởi động lại backend.");
        }

        GoogleIdToken token;
        try {
            token = verifier.verify(idTokenString);
        } catch (Exception e) {
            log.warn("Không xác minh được ID token Google: {}", e.getMessage());
            throw BusinessException.unauthorized("Đăng nhập Google thất bại, vui lòng thử lại");
        }

        if (token == null) {
            // verify() trả null khi chữ ký sai, hết hạn, sai issuer hoặc sai audience
            throw BusinessException.unauthorized("Token Google không hợp lệ");
        }

        GoogleIdToken.Payload payload = token.getPayload();
        boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());

        if (payload.getEmail() == null || !emailVerified) {
            // Không có email đã xác minh thì không được phép ghép vào tài khoản sẵn có:
            // ai đó tạo tài khoản Google với email chưa xác minh sẽ chiếm được tài khoản người khác.
            throw BusinessException.unauthorized(
                    "Tài khoản Google này chưa xác minh email nên không dùng để đăng nhập được");
        }

        return new GoogleAccount(
                payload.getSubject(),
                payload.getEmail().toLowerCase(java.util.Locale.ROOT),
                true,
                (String) payload.get("name"),
                (String) payload.get("picture"));
    }
}
