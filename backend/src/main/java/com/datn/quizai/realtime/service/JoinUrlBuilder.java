package com.datn.quizai.realtime.service;

import com.datn.quizai.common.NetworkAddressResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Dựng đường dẫn mà mã QR của phòng đấu trỏ tới.
 * <p>
 * <b>Vì sao việc này ở backend chứ không ở frontend:</b> nếu frontend lấy
 * {@code window.location.origin} thì đường dẫn phụ thuộc vào cách <i>host</i> mở trang. Host mở
 * bằng {@code localhost} — điều hoàn toàn tự nhiên khi dev — thì QR cũng mang {@code localhost},
 * và điện thoại quét sẽ trỏ về chính nó. Backend biết địa chỉ LAN thật của máy nên quyết định được
 * đúng, không phụ thuộc thói quen của người dùng.
 * <p>
 * Thứ tự ưu tiên:
 * <ol>
 *   <li>{@code app.frontend.base-url} nếu được đặt — dùng cho triển khai thật.</li>
 *   <li>Địa chỉ LAN dò được + cổng frontend khi dev.</li>
 *   <li>Đường dẫn tương đối, để frontend tự ghép với origin của nó (không còn cách nào khác).</li>
 * </ol>
 */
@Service
public class JoinUrlBuilder {

    private final NetworkAddressResolver addressResolver;
    private final String configuredBaseUrl;
    private final int devFrontendPort;

    public JoinUrlBuilder(NetworkAddressResolver addressResolver,
                          @Value("${app.frontend.base-url:}") String configuredBaseUrl,
                          @Value("${app.frontend.dev-port}") int devFrontendPort) {
        this.addressResolver = addressResolver;
        this.configuredBaseUrl = configuredBaseUrl == null ? "" : configuredBaseUrl.trim();
        this.devFrontendPort = devFrontendPort;
    }

    public String joinUrl(String roomCode) {
        return baseUrl() + "/join/" + roomCode;
    }

    private String baseUrl() {
        if (!configuredBaseUrl.isBlank()) {
            return stripTrailingSlash(configuredBaseUrl);
        }
        return addressResolver.lanAddress()
                .map(address -> "http://" + address + ":" + devFrontendPort)
                .orElse("");
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
