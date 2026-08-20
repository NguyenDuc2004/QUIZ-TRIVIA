# Công nghệ sử dụng

## 1. Backend

| Thành phần | Công nghệ | Ghi chú |
|-----------|-----------|---------|
| Ngôn ngữ | Java 21 (LTS) | |
| Framework | Spring Boot 3.x | |
| Web REST | Spring Web | REST API |
| Streaming | Spring WebFlux / SSE | Chatbot streaming |
| Real-time | **Spring WebSocket (STOMP)** | Phòng đấu multiplayer |
| Bảo mật | Spring Security + JWT | |
| ORM quan hệ | Spring Data JPA + Hibernate | PostgreSQL |
| ORM đồ thị | **Spring Data Neo4j** | Gợi ý cá nhân hóa |
| Vector search | **pgvector** (qua JPA/native query) | RAG retrieval |
| Trích xuất tài liệu | Apache Tika | PDF/DOCX/TXT → text |
| Migration | Flyway | Schema PostgreSQL |
| Validation | Jakarta Bean Validation | |
| Resilience | Resilience4j | Circuit breaker, retry cho AI |
| Tài liệu API | springdoc-openapi (Swagger UI) | |
| Test | JUnit 5, Mockito, Testcontainers | |
| Load test | **k6 / Gatling / JMeter** | Kiểm thử chịu tải real-time |
| Build | Maven | |

## 2. Tích hợp AI

| Thành phần | Công nghệ | Ghi chú |
|-----------|-----------|---------|
| Provider chính | **Google Gemini API** | `gemini-3.6-flash` (nhanh), `gemini-2.5-pro` (chất lượng) |
| Provider dự phòng | **Groq API** (groq.com) | Fallback khi Gemini lỗi tạm thời. **Không phải Grok của xAI** — hai chữ khác đúng một ký tự. Đã đổi từ xAI Grok sang đây vì xAI **không có gói miễn phí**: đo thật ngày 08/08/2026 cho thấy key hợp lệ vẫn bị chặn ở tầng quyền (403 `permission-denied`), nên suốt cả dự án fallback **chưa một lần chạy thật**. Groq có gói miễn phí, API tương thích OpenAI, và **có streaming** — thứ xAI không có, nhờ đó trợ lý học tập lần đầu cũng có đường lui |
| Embedding | Gemini embedding | Tạo vector cho RAG |
| HTTP client | Spring `WebClient` | Gọi REST của LLM |
| Abstraction | ~~Spring AI~~ (không dùng) | Gọi trực tiếp qua `WebClient`, tự viết `AiOrchestrator` để kiểm soát fallback/quota |

## 3. Frontend

| Thành phần | Công nghệ |
|-----------|-----------|
| Framework | React 19 + TypeScript |
| Build tool | Vite |
| Data fetching | TanStack Query (React Query) |
| State | Zustand |
| Routing | React Router |
| UI | **Ant Design** (component) + TailwindCSS (layout/spacing) |
| Form | React Hook Form + Zod |
| HTTP | Axios / Fetch |
| Real-time | `@stomp/stompjs` + SockJS |
| Streaming | EventSource (SSE) cho chatbot |

> **Antd + Tailwind:** Dùng Ant Design v6 cho component (Table, Form, Modal…), Tailwind v4 chỉ cho layout/spacing.
> Tailwind v4 không còn `tailwind.config.js`; để **không nạp Preflight** (reset CSS sẽ đè style của Antd), trong `src/index.css` chỉ import `theme.css` + `utilities.css` thay vì `tailwindcss`. Tùy biến theme qua `ConfigProvider` của Antd.

## 4. Hạ tầng & CSDL

| Thành phần | Công nghệ | Vai trò |
|-----------|-----------|---------|
| CSDL quan hệ | **PostgreSQL 16 + pgvector** | Dữ liệu nghiệp vụ + vector học liệu (cổng 5432) |
| CSDL đồ thị | **Neo4j 5** | Hành vi, gợi ý, lộ trình học |
| Cache / Real-time | **Redis** | Cache, session, quota, trạng thái phòng, Pub/Sub |
| Gửi email | **Spring Mail + Gmail SMTP** | Mã OTP đặt lại mật khẩu. Dùng *App Password* vì Google đã chặn đăng nhập SMTP bằng mật khẩu tài khoản từ 2022 |
| Đăng nhập Google | **google-api-client** (`GoogleIdTokenVerifier`) + Google Identity Services ở FE | Xác minh ID token. Không dùng `spring-boot-starter-oauth2-client` vì luồng ID token không cần server-side redirect và không cần Client Secret |
| Lưu file người dùng | **Thư mục đĩa local** (`app.storage.upload-dir`, mặc định `backend/uploads/`) | Ảnh bìa quiz, sau này là ảnh câu hỏi. Backend phục vụ tĩnh tại `/uploads/**`. Chọn đĩa local thay vì S3/MinIO vì đồ án chạy một máy chủ duy nhất; đổi sang object storage sau chỉ cần thay `FileStorageService` |
| Container | Docker + Docker Compose | Chạy toàn bộ stack local |
| CI/CD | GitHub Actions (tùy chọn) | |
| Triển khai | VPS / Render / Railway | |

## 5. Biến môi trường chính

```
# Database
POSTGRES_URL   # jdbc:postgresql://localhost:5432/quizdb
POSTGRES_USER, POSTGRES_PASSWORD
NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD
REDIS_HOST, REDIS_PORT

# Gửi email (OTP quên mật khẩu)
MAIL_USERNAME          # địa chỉ Gmail dùng để gửi
MAIL_PASSWORD          # App Password 16 ký tự, KHÔNG phải mật khẩu Gmail
                       # https://myaccount.google.com/apppasswords (cần bật xác minh 2 bước)
MAIL_FROM_NAME         # tên hiện ở ô Người gửi, mặc định "Quiz AI"
OTP_TTL_MINUTES        # mặc định 10
OTP_MAX_ATTEMPTS       # mặc định 5
OTP_RESEND_COOLDOWN    # mặc định 60 (giây)

# Hạn mức AI
APP_AI_ASYNC_POOL_SIZE # số luồng chạy tác vụ AI nền, mặc định 1 (tuần tự).
                       # Để 1 vì Gemini miễn phí giới hạn 5 lượt/phút — chạy song song chỉ khiến
                       # các job tranh nhau rồi cùng 429. Nâng lên khi dùng gói trả phí.

# Đăng nhập Google
GOOGLE_CLIENT_ID       # OAuth 2.0 Client ID loại "Web application" (Google Cloud Console)
                       # KHÔNG cần Client Secret. Frontend đọc cùng giá trị qua VITE_GOOGLE_CLIENT_ID
                       # Authorized JavaScript origins phải liệt kê đủ mọi origin mở trang login

# Địa chỉ công khai — QUAN TRỌNG khi deploy
FRONTEND_BASE_URL      # https://quiz.example.com — địa chỉ đưa vào mã QR phòng đấu.
                       # Để trống khi dev: backend tự dò IP LAN (chỉ máy cùng Wi-Fi vào được).
FRONTEND_DEV_PORT      # mặc định 5173, chỉ dùng khi FRONTEND_BASE_URL để trống
CORS_ALLOWED_ORIGINS   # mẫu origin, ví dụ https://quiz.example.com
                       # Để trống khi dev: mở cho localhost + dải IP nội bộ

# Security
JWT_SECRET, JWT_ACCESS_TTL, JWT_REFRESH_TTL

# Lưu file
UPLOAD_DIR             # mặc định uploads
MAX_IMAGE_SIZE_BYTES   # mặc định 2097152 (2MB)

# AI
GEMINI_API_KEY, GEMINI_MODEL
GROQ_API_KEY          # Lấy miễn phí ở https://console.groq.com/keys — Groq, KHÔNG phải Grok.
                      # Để trống thì AiOrchestrator lọc Groq ra khỏi danh sách ngay từ đầu, và đó
                      # là hành vi ĐÚNG. Đừng điền một key không dùng được: khi Gemini hết hạn mức
                      # nó sẽ chuyển sang Groq, Groq trả 401 (lỗi KHÔNG tạm thời), và người dùng
                      # nhận "unauthorized" thay cho "hết hạn mức, chờ N giây" vốn hữu ích hơn.
GROQ_MODEL            # mặc định llama-3.3-70b-versatile
                      # Kiểm model còn sống: GET https://api.groq.com/openai/v1/models
AI_PROVIDER_ORDER=gemini,groq
```

> **Không commit API key** vào repo. Dùng `.env` (đã gitignore) hoặc secret manager.

> **Khi deploy, bắt buộc đặt `FRONTEND_BASE_URL` và `CORS_ALLOWED_ORIGINS`.** Mặc định của hai biến
> này chỉ đúng cho môi trường dev: mã QR sẽ mang IP LAN (chỉ máy cùng Wi-Fi quét được) và CORS mở cho
> mọi dải IP nội bộ. Đặt đúng tên miền là mã QR chạy được với bất kỳ ai, ở bất kỳ đâu.
