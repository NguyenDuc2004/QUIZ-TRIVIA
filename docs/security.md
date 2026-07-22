# Bảo mật

## 1. Xác thực & phân quyền

- **Mật khẩu:** băm bằng **BCrypt**, không lưu plaintext.
- **JWT:** access token ngắn hạn (15 phút) + refresh token dài hạn; xoay vòng (rotation) refresh token.
- **RBAC:** phân quyền theo vai trò ở tầng controller bằng `@PreAuthorize("hasRole('CREATOR')")`.
- **OAuth2 (tùy chọn):** đăng nhập Google.
- **WebSocket:** xác thực JWT khi handshake; kiểm tra quyền vào phòng trước khi subscribe.

## 2. Chống OWASP Top 10

| Rủi ro | Biện pháp |
|--------|-----------|
| Injection (SQL/Cypher) | Tham số hóa truy vấn (JPA, Cypher parameters) |
| XSS | Escape output ở frontend, sanitize nội dung do người dùng/AI tạo |
| CSRF | SPA dùng Bearer token (không dùng cookie session) → miễn CSRF |
| Broken Access Control | RBAC + kiểm tra quyền sở hữu tài nguyên (owner check) |
| Sensitive Data Exposure | HTTPS bắt buộc, không log dữ liệu nhạy cảm |
| Rate limiting | Giới hạn request (đặc biệt endpoint AI) qua Redis |

## 3. Bảo mật AI

- **Không gửi dữ liệu nhạy cảm** (mật khẩu, PII) tới LLM.
- **Chống prompt injection:** làm sạch input, tách rõ system prompt và user input, không cho input ghi đè chỉ thị hệ thống.
- **Guardrail nội dung:** moderation đầu vào & đầu ra; giới hạn phạm vi chatbot trong học tập.
- **RAG grounding:** yêu cầu LLM chỉ dùng ngữ cảnh truy xuất → giảm ảo giác (hallucination) và rò rỉ.
- **API key:** lưu trong biến môi trường / secret manager; **không commit** vào repo (`.env` đã gitignore).
- **Quota & chi phí:** giới hạn số lần gọi AI theo user; log token tiêu thụ ở `ai_request_logs`.

## 4. Cấu hình chung

- **HTTPS** bắt buộc ở môi trường thật.
- **CORS** cấu hình chặt (chỉ cho phép origin của frontend).
- **Validation** toàn bộ input (Jakarta Bean Validation).
- **Log tập trung** có `traceId` để truy vết, không ghi thông tin nhạy cảm.
