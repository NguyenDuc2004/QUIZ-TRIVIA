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
