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
