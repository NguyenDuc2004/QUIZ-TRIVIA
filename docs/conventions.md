# Quy ước phát triển (Conventions)

> Tài liệu này giữ code nhất quán khi phát triển có hỗ trợ AI (vibe coding). Đọc trước khi sinh code.

## 1. Backend (Java / Spring Boot)

- **Cấu trúc:** feature-based package (xem [architecture.md](architecture.md)), phân lớp Controller → Service → Repository.
- **Controller** chỉ nhận/trả DTO, không chứa logic nghiệp vụ.
- **DTO** tách khỏi Entity; dùng record cho DTO khi hợp lý. Không expose Entity ra API.
- **Đặt tên:** `XxxController`, `XxxService`, `XxxRepository`, `XxxDto`, `XxxRequest`, `XxxResponse`.
- **Validation:** annotate DTO request bằng Jakarta Bean Validation; validate ở controller.
- **Exception:** ném exception nghiệp vụ, xử lý tập trung ở `@RestControllerAdvice` → response lỗi chuẩn (xem [api.md](api.md) mục 10).
- **Entity:** kế thừa `BaseEntity` (id UUID, createdAt, updatedAt). Không dùng logic nghiệp vụ trong entity.
- **AI:** mọi lời gọi LLM đi qua `AiProvider`/`AiOrchestrator`, không gọi trực tiếp API trong service nghiệp vụ.
- **Async:** tác vụ > vài giây (sinh đề, xử lý học liệu) chạy nền, trả `jobId`.
- **Migration:** mọi thay đổi schema PostgreSQL qua Flyway (`V{n}__mô_tả.sql`), không sửa file migration đã merge.

## 2. Frontend (React / TypeScript)

- **Cấu trúc:** theo feature (`src/features/<feature>/{api,components,hooks,pages}`), `src/shared` cho dùng chung.
- **Data:** dùng TanStack Query cho gọi API; không tự quản lý loading/error thủ công.
- **Type:** định nghĩa type khớp response backend; validate form bằng Zod.
- **Đặt tên:** component `PascalCase`, hook `useXxx`, file component trùng tên component.
- **Không hardcode URL API:** dùng client tập trung (axios instance) + biến môi trường.

## 3. Git

- **Branch:** `feature/<tên>`, `fix/<tên>`. Nhánh chính: `main`.
- **Commit (Conventional Commits):** `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`.
- Commit nhỏ, thông điệp rõ ràng bằng tiếng Việt hoặc tiếng Anh (nhất quán).

## 4. Cấu hình & bí mật

- Không commit secret/API key. Dùng `.env` (đã gitignore) + `application.yml` đọc từ env.
- Mỗi CSDL (Postgres/Neo4j/Redis) chạy qua Docker Compose khi dev local.

## 5. Thứ tự ưu tiên khi phát triển

Theo [roadmap.md](roadmap.md): nền tảng → quiz → chơi đơn → **multiplayer** → **AI/RAG** → **Neo4j** → kiểm thử. Ưu tiên đủ 4 trụ cột của phiếu trước khi làm tính năng phụ.
