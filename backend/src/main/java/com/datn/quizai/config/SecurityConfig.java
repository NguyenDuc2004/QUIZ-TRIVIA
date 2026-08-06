package com.datn.quizai.config;

import com.datn.quizai.auth.security.JwtAuthenticationFilter;
import com.datn.quizai.common.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.UUID;

/**
 * Cấu hình bảo mật: API stateless dùng Bearer JWT.
 * <p>
 * Luật Guest (docs/features/01-auth.md): chưa đăng nhập chỉ gọi được `/auth/**` và
 * `GET /api/v1/quizzes*` — mọi thứ còn lại phải có access token hợp lệ.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // bật @PreAuthorize cho phân quyền theo vai trò
public class SecurityConfig {

    /**
     * Các đường dẫn GET mở cho Guest.
     * <p>
     * Lưu ý: {@code /api/v1/quizzes/*} chỉ trả thông tin giới thiệu quiz công khai;
     * câu hỏi nằm ở {@code /api/v1/quizzes/{id}/questions} (không khớp mẫu này) nên
     * vẫn yêu cầu đăng nhập + quyền sở hữu.
     */
    private static final String[] PUBLIC_GET = {
            "/api/v1/quizzes",
            "/api/v1/quizzes/*",
            "/api/v1/categories",
            // Khách quét QR cần xem được phòng và danh sách avatar trước khi có danh tính.
            // Mã PIN 6 số chính là thứ chặn cửa; việc vào phòng còn phải được host bật cho phép.
            "/api/v1/rooms/*",
            "/api/v1/rooms/avatars",
            // Ảnh bìa quiz công khai phải xem được khi chưa đăng nhập; tên file là UUID ngẫu nhiên
            // nên không đoán được ảnh của quiz riêng tư. Chỉ mở GET — POST /api/v1/files vẫn cần quyền.
            "/uploads/**",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Qualifier("appCorsConfigurationSource") CorsConfigurationSource corsSource) throws Exception {
        http
                // API stateless dùng Bearer token → không cần CSRF (docs/security.md §2)
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        // Không chặn dispatch nội bộ tới /error, nếu không mọi 404/500 của
                        // người dùng chưa đăng nhập đều bị biến thành 401.
                        .requestMatchers("/error").permitAll()
                        // Bắt tay WebSocket không mang được header Authorization (nhất là khi
                        // SockJS lùi về long-polling). Xác thực làm ở frame STOMP CONNECT —
                        // xem StompAuthChannelInterceptor.
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        // Vào phòng với tư cách khách — RoomService kiểm allowGuests của từng phòng
                        .requestMatchers(HttpMethod.POST, "/api/v1/rooms/*/join-as-guest").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(forbiddenHandler())
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Chưa đăng nhập / token sai → 401 theo response lỗi chuẩn (docs/api.md §10). */
    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> writeError(response, 401, "Unauthorized",
                "Bạn cần đăng nhập để sử dụng chức năng này", request.getRequestURI());
    }

    /** Đã đăng nhập nhưng thiếu quyền → 403. */
    private AccessDeniedHandler forbiddenHandler() {
        return (request, response, deniedException) -> writeError(response, 403, "Forbidden",
                "Bạn không có quyền thực hiện hành động này", request.getRequestURI());
    }

    private void writeError(HttpServletResponse response, int status, String error,
                            String message, String path) throws java.io.IOException {
        response.setStatus(status);
        // Khai báo charset rõ ràng + ghi qua OutputStream để Jackson tự encode UTF-8,
        // tránh tiếng Việt bị lỗi font ở client.
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(status, error, message, path,
                UUID.randomUUID().toString().substring(0, 8)));
    }

    /**
     * Đặt tên riêng để không nhập nhằng với bean {@code mvcHandlerMappingIntrospector}
     * (cũng là một {@link CorsConfigurationSource}).
     */
    @Bean("appCorsConfigurationSource")
    CorsConfigurationSource appCorsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
