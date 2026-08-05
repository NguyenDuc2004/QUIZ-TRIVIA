package com.datn.quizai.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình bảo mật nền.
 * <p>
 * Quy tắc (docs/features/01-auth.md): Guest chưa đăng nhập chỉ được xem danh sách/giới thiệu
 * quiz công khai và gọi các endpoint xác thực — mọi thứ còn lại yêu cầu đăng nhập.
 * <p>
 * TODO(slice Auth): gắn JwtAuthenticationFilter + AuthenticationEntryPoint trả lỗi chuẩn.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Chỉ các đường dẫn GET này mở cho Guest. */
    private static final String[] PUBLIC_GET = {
            "/api/v1/quizzes",
            "/api/v1/quizzes/*",
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            @Qualifier("appCorsConfigurationSource") CorsConfigurationSource corsSource) throws Exception {
        http
                // API stateless dùng Bearer token → không cần CSRF (docs/security.md §2)
                .csrf(AbstractHttpConfigurer -> AbstractHttpConfigurer.disable())
                .cors(cors -> cors.configurationSource(corsSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Không chặn dispatch nội bộ tới /error, nếu không mọi 404/500 của
                        // người dùng chưa đăng nhập đều bị biến thành 401.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
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
