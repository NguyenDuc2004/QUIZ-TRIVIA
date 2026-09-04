# CLAUDE.md — Hướng dẫn làm việc trên dự án

Đồ án tốt nghiệp: **Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo** (HaUI, K17).
SV Nguyễn Khắc Minh Đức · GVHD ThS. Nguyễn Đức Lưu · Thời gian 20/07/2026 – 20/09/2026.

## 1. Đọc gì trước khi code

| Cần biết | File |
|---|---|
| Mục tiêu, phạm vi, tác nhân & quyền | [docs/overview.md](docs/overview.md) |
| Kiến trúc, cấu trúc package, luồng dữ liệu | [docs/architecture.md](docs/architecture.md) |
| **Quy ước code + quy trình lát cắt dọc** | [docs/conventions.md](docs/conventions.md) |
| **Chuẩn giao diện (bắt buộc khi làm FE)** | [docs/ui-design-system.md](docs/ui-design-system.md) |
| Công nghệ & phiên bản | [docs/tech-stack.md](docs/tech-stack.md) |
| Schema PostgreSQL / Neo4j / Redis | [docs/database.md](docs/database.md) |
| Endpoint REST & STOMP | [docs/api.md](docs/api.md) |
| Bảo mật (JWT, RBAC, bảo mật AI) | [docs/security.md](docs/security.md) |
| Đặc tả từng tính năng | [docs/features/](docs/features/) (16 file) |
| Kế hoạch theo ngày | [docs/ke-hoach-tien-do.md](docs/ke-hoach-tien-do.md) |

**Tài liệu là nguồn sự thật.** Nếu code và tài liệu lệch nhau → dừng lại, hỏi, rồi sửa cả hai cho khớp; không âm thầm làm khác spec.

## 2. Stack (không tự đổi)

- **Backend** `backend/`: Java 21, Spring Boot 3.x, Maven, Spring Web + Security + Data JPA + Data Neo4j + Data Redis + WebSocket (STOMP), Flyway, Jakarta Validation, springdoc-openapi, Resilience4j, Apache Tika.
- **Frontend** `frontend/`: React 19 + TypeScript + Vite 8, TanStack Query, Zustand, React Router 7, **Ant Design v6** (component) + Tailwind v4 (chỉ layout/spacing, **không nạp preflight** — xem `src/index.css`), React Hook Form + Zod, `@stomp/stompjs` + SockJS, EventSource cho SSE.
- **Dữ liệu**: PostgreSQL 16 + pgvector (5432) · Neo4j 5 (7687) · Redis 7 (6379) — chạy qua `docker compose up -d`.
- **AI**: Google Gemini (chính) → **Groq** (dự phòng — groq.com, *không phải* Grok của xAI), gọi qua `WebClient` trong `AiOrchestrator` tự viết. **KHÔNG** dùng Spring AI, **KHÔNG** LangChain4j.
- Không dùng: Next.js, RabbitMQ, microservices, MongoDB.

## 3. Cấu trúc thư mục

```
backend/src/main/java/com/datn/quizai/
   <feature>/{controller,service,repository,domain,dto}   # nhóm theo tính năng, trong đó chia theo tầng
   config/ · common/                                      # KHÔNG chia tầng (không phải tính năng)
backend/src/main/resources/db/migration/V{n}__*.sql       # Flyway
backend/src/test/java/...                                 # phản chiếu đúng cấu trúc trên
frontend/src/features/<feature>/{api,components,hooks,pages,store}
frontend/src/shared/                                       # dùng chung
docs/ · infra/ · docker-compose.yml
```

Tính năng đã có: `auth`, `user`, `quiz`, `attempt`, `file`, `realtime`, `ai` (RAG + sinh đề). Sắp tới: `recommend`, `analytics` — xem cây đầy đủ ở [architecture.md §3](docs/architecture.md).

Một tính năng = **thêm package vào `backend/` (kèm 5 thư mục tầng) + thêm folder vào `frontend/src/features/`**, tên trùng nhau. Không tạo repo/thư mục riêng cho từng tính năng.

## 4. Quy trình bắt buộc — lát cắt dọc

Làm **trọn một tính năng** rồi mới sang tính năng khác. Thứ tự trong mỗi lát cắt (chi tiết ở [conventions.md §7](docs/conventions.md)):

1. Migration Flyway → 2. Backend (Entity→Repository→Service→DTO→Controller) → 3. Frontend (api→hooks→components→pages→route) → 4. Nối FE↔BE chạy thật → 5. **Viết test VÀ chạy pass** → 6. Cập nhật `docs/bao-cao/nhat-ky-tien-do.md` + mục báo cáo tương ứng.

Đủ cả **Definition of Done 7 mục** ([conventions.md §7](docs/conventions.md)) mới được bắt đầu tính năng kế tiếp. Bị chặn → ghi `[!]` vào nhật ký kèm lý do, không bỏ dở nhảy việc khác.

## 5. Quy tắc code cốt lõi

- Controller không chứa logic nghiệp vụ; không expose Entity ra API (dùng DTO/record).
- Mọi thay đổi schema qua Flyway `V{n}__mô_tả.sql`; **không sửa migration đã commit**.
- Mọi lời gọi LLM đi qua `AiProvider`/`AiOrchestrator`, không gọi thẳng API trong service nghiệp vụ.
- Tác vụ AI nặng chạy nền, trả `jobId`.
- **Guest (chưa đăng nhập) chỉ được `GET` danh sách/giới thiệu quiz công khai** — không làm bài, không xem nội dung câu hỏi. Mọi thứ khác `authenticated()`.
  - **Ngoại lệ duy nhất — phòng đấu:** khách vào được khi biết mã PIN 6 số **và** host bật `allowGuests` cho phòng đó. Khách dùng *khoá phiên* riêng (Redis `roomguest:{key}`), không phải JWT, và khoá chỉ mở đúng một phòng.
- **Giao diện theo [ui-design-system.md](docs/ui-design-system.md)**: không hardcode màu/bo góc/shadow trong component; nút hành động chính màu tím đặc `violet-600` (`type="primary"`); trang người học dùng lưới card, trang quản lý dùng bảng; dùng lại `PageHeader`/`EmptyState`; **không bịa dữ liệu** (rating, số lượt học) cho đẹp giao diện.
- Không commit secret. `.env` đã gitignore; `.env.example` là bản mẫu.
- Commit theo Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`…), tiếng Việt.

## 6. Lệnh hay dùng

```bash
docker compose up -d                  # bật PostgreSQL + Neo4j + Redis
cd backend && ./mvnw spring-boot:run  # chạy BE (http://localhost:8080)
cd backend && ./mvnw test             # chạy test BE
cd frontend && npm run dev            # chạy FE (http://localhost:5173)
cd frontend && npm test               # chạy test FE (vitest, chạy một lượt rồi thoát)
cd frontend && npm run build          # oxlint + tsc -b + vite build — CHẠY CẢ LỆNH NÀY sau khi đổi cấu hình
node scripts/seed-demo.mjs            # nạp dữ liệu demo (cần BE đang chạy); chạy lại không nhân đôi
```

> **`npm run build` chạy `oxlint` trước khi biên dịch.** Luật `react/rules-of-hooks` đã bật từ đầu và bắt
> đúng lỗi "gọi hook sau lệnh return sớm" — nhưng nó chỉ chạy khi ai đó gõ `npm run lint`, mà không ai gõ.
> Hệ quả: một lỗi loại đó nằm trong mã nguồn cho tới khi người dùng bấm vào và nhận màn hình trắng. Gắn vào
> `build` để nó chạy ở nơi chắc chắn có người chạy.

> Cấu hình vitest nằm ở `frontend/vitest.config.ts` **riêng**, không gộp vào `vite.config.ts`: vitest 3
> chưa hỗ trợ Vite 8 nên nó tự cài một bản vite riêng, hai bộ type plugin không khớp và gộp lại sẽ làm
> `npm run build` đổ. Vì vậy sau khi đổi cấu hình build hoặc test, phải chạy **cả** `npm test` **và**
> `npm run build`.

## 7. Viết báo cáo

Dùng skill `viet-bao-cao` (khung 3 chương HaUI ở `.claude/skills/viet-bao-cao/references/cau-truc-bao-cao.md`).
**Số liệu mục 3.5 (load test) và 3.6 (độ chính xác AI) chỉ ghi khi đã đo thật** — không ước lượng.
