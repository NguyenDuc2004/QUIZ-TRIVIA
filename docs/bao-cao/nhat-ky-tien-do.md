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
| 06/08 (tối) | Chuẩn giao diện Udemy + **lát cắt Làm bài quiz** | 6/6 | 🟢 xong |

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
- [x] Test 86/86 pass (thêm 31 ca)
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

### Ghi chú báo cáo
- **Mục 2.8:** ERD nay có 8 bảng — thêm `quiz_attempts` và `attempt_answers`. Nên nêu rõ *vì sao* `attempt_answers` sinh sẵn ngay lúc bắt đầu (chốt đề) thay vì chỉ ghi khi người dùng trả lời: đây là quyết định thiết kế có lý do rõ ràng, dễ hỏi khi bảo vệ.
- **Mục 2.6:** thêm UC_LamBaiQuiz, UC_XemKetQua, UC_XemLichSu, UC_XemBangXepHang. Luồng thay thế lấy sẵn từ các ca 400/404/409 đã kiểm chứng (hết giờ, nộp hai lần, quiz rỗng, bài của người khác).
- **Mục 2.7 (thiết kế lớp):** `AnswerGrader` là ví dụ tốt để nói về tách logic nghiệp vụ khỏi framework — không phụ thuộc Spring nên test được ở mức đơn vị, 15 ca chạy trong 0,06 giây.
- **Mục 3.4:** 86 test tự động (100% pass) + 48 ca kiểm chứng HTTP. Ba lỗi ở trên đưa vào phần "khó khăn & cách giải quyết"; lỗi số 1 minh họa rõ giá trị của việc chạy thật chứ không chỉ chạy test.
- **Mục bảo mật:** luật "không lộ đáp án khi chưa nộp" và "bài làm của ai người ấy xem (404 chứ không 403)" nên viết thành một mục riêng — đây là phần dễ làm sai và có bằng chứng test kèm theo.

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
