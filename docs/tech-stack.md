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
| Provider dự phòng | **xAI Grok API** | Gói miễn phí, fallback khi Gemini lỗi |
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

# Security
JWT_SECRET, JWT_ACCESS_TTL, JWT_REFRESH_TTL

# Lưu file
UPLOAD_DIR             # mặc định uploads
MAX_IMAGE_SIZE_BYTES   # mặc định 2097152 (2MB)

# AI
GEMINI_API_KEY, GEMINI_MODEL
GROK_API_KEY, GROK_MODEL
AI_PROVIDER_ORDER=gemini,grok
```

> **Không commit API key** vào repo. Dùng `.env` (đã gitignore) hoặc secret manager.
