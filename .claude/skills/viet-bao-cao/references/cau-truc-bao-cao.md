# Khung báo cáo ĐATN — "Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo"

> Đây là **khung chính thức** theo mẫu ĐH Công nghiệp Hà Nội (HaUI), đã tùy biến cho đề tài Quiz/Trivia AI.
> Cấu trúc: **Mở đầu → 3 chương → Kết luận → Tài liệu tham khảo**. KHÔNG tự ý đổi sang 4–5 chương.
> Chỗ đánh dấu `«...»` là nội dung cần viết/điền. Mỗi khi code xong 1 chức năng → cập nhật đúng mục theo [bảng map cuối file](#map-chức-năng--mục-báo-cáo).

---

## Thông tin bìa (cố định — không hỏi lại user)

```
BỘ CÔNG THƯƠNG
ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI
------------------------------

ĐỒ ÁN TỐT NGHIỆP
NGÀNH KỸ THUẬT PHẦN MỀM

TÊN ĐỀ TÀI
XÂY DỰNG ỨNG DỤNG QUIZ/TRIVIA
TÍCH HỢP TRÍ TUỆ NHÂN TẠO

CBHD            : ThS. Nguyễn Đức Lưu
Sinh viên       : Nguyễn Khắc Minh Đức
Mã số sinh viên : 2022601585
Lớp             : 2022DHKTPM01 — Khóa K17

Hà Nội – 2026
```

Nguồn: `docs/phieu_giao_de_tai/phieu_giao_de_tai.md` (Trưởng đơn vị: TS. Đặng Trọng Hợp; thời gian thực hiện 20/07/2026 – 20/09/2026).

## Phần đầu (front matter) — số trang i, ii, iii…

| # | Mục | Nội dung | Nguồn |
|---|-----|----------|-------|
| 1 | **Trang bìa** | Như trên | `phieu_giao_de_tai.md` |
| 2 | **Trang bìa phụ** (lót) | Như trang bìa | — |
| 3 | **Lời cảm ơn** | Giọng trang trọng ngôi "em"; cảm ơn Trường CNTT&TT – ĐH Công nghiệp Hà Nội, các thầy cô, đặc biệt CBHD ThS. Nguyễn Đức Lưu. Viết cuối kỳ. | — |
| 4 | **Mục lục** | TOC tự động 3 cấp | tự sinh |
| 5 | **Danh mục hình ảnh** | `Hình x.y` — cập nhật dần | tự sinh |
| 6 | **Danh mục bảng biểu** | `Bảng x.y` — cập nhật dần | tự sinh |
| 7 | **Danh mục các từ viết tắt** | Bảng dưới | `docs/overview.md §6` |

### Danh mục từ viết tắt (bản chuẩn của đề tài)

| Viết tắt | Ý nghĩa |
|----------|---------|
| AI | Artificial Intelligence — Trí tuệ nhân tạo |
| LLM | Large Language Model — Mô hình ngôn ngữ lớn |
| RAG | Retrieval-Augmented Generation — Sinh có tăng cường truy xuất |
| REST | Representational State Transfer |
| API | Application Programming Interface |
| JWT | JSON Web Token |
| RBAC | Role-Based Access Control — Phân quyền theo vai trò |
| STOMP | Simple Text Oriented Messaging Protocol (trên WebSocket) |
| SSE | Server-Sent Events |
| SRS | Spaced Repetition System — Lặp lại ngắt quãng |
| pgvector | Extension lưu & tìm kiếm vector trên PostgreSQL |
| P95 | Phân vị 95 của độ trễ (latency) |
| PK / FK | Primary Key / Foreign Key |
| UC | Use Case |
| CSDL | Cơ sở dữ liệu |

---

## MỞ ĐẦU

- **1. Lý do chọn đề tài** — «Bối cảnh học tập/giải trí trực tuyến, xu hướng gamified learning (Kahoot, Quizizz). Khoảng trống: thiếu cá nhân hóa, chưa tận dụng Generative AI để sinh đề từ học liệu, chưa gợi ý lộ trình theo năng lực → dẫn tới đề tài.» — `docs/overview.md §1`
- **2. Mục đích và mục tiêu đề tài** — bám **nguyên văn 4 mục tiêu** trong phiếu giao đề tài:
  1. Website Quiz/Trivia hoàn chỉnh, giao diện thân thiện, có **phòng đấu Multiplayer real-time** độ trễ thấp.
  2. Tích hợp **Generative AI qua kiến trúc RAG**: trợ lý học tập + tự động sinh cấu trúc đề thi từ học liệu.
  3. Ứng dụng **Neo4j** phân tích hành vi/sở thích, gợi ý bài thi và **lộ trình học cá nhân hóa**.
  4. **Kiểm thử, đánh giá hiệu năng chịu tải real-time** (Spring WebSocket + Redis) và **độ chính xác mô hình AI**.
- **3. Nội dung nghiên cứu** — «(a) kiến trúc web phân lớp Spring Boot + React; (b) real-time WebSocket/STOMP + Redis Pub/Sub; (c) pipeline RAG (ingest→embedding→pgvector→retrieval) + tích hợp LLM có fallback; (d) mô hình đồ thị Neo4j cho gợi ý. Khảo sát Kahoot/Quizizz; thiết kế chức năng–CSDL–giao diện; hiện thực; kiểm thử.»
- **4. Phạm vi nghiên cứu** — `docs/overview.md §4` (5 loại câu hỏi; 3 chế độ chơi; AI sinh đề/chấm/chatbot; Gemini → Grok; gợi ý Neo4j; nêu rõ phần mở rộng tùy chọn: flashcard/SRS, chống gian lận, gamification, lớp học…).
- **5. Kết quả mong muốn** — `docs/overview.md §3` + phiếu giao đề tài.
- **6. Bố cục đề tài** — 3 chương như dưới.

---

## CHƯƠNG 1. TỔNG QUAN VỀ ỨNG DỤNG QUIZ/TRIVIA TÍCH HỢP AI

> Mỗi mục ~½–1 trang, viết theo mạch: **khái niệm → đặc điểm → vai trò trong đề tài này**. Đủ ý, không lan man. Nguồn công nghệ: `docs/tech-stack.md`, `docs/architecture.md`.

- 1.1. Hệ thống Quiz/Trivia trực tuyến — «khái niệm nền tảng trắc nghiệm/đố vui, gamified learning; so sánh Kahoot, Quizizz (ưu/nhược); vai trò trong đề tài.»
- 1.2. Trí tuệ nhân tạo tạo sinh (Generative AI) & LLM — «LLM là gì; ứng dụng trong đề tài: sinh đề, chấm tự luận, chatbot. Gemini 2.5 Flash/Pro (chính) → Grok (dự phòng).» — `features/05, 06, 08`
- 1.3. RAG (Retrieval-Augmented Generation) — «truy xuất học liệu + sinh có dẫn nguồn để chống ảo giác (grounding); vai trò trong sinh đề & chatbot; embedding + tìm kiếm ngữ nghĩa.» — `features/05-ai-rag-generation.md`
- 1.4. Spring Boot — «framework backend Java 21; auto-config, REST, phân lớp Controller→Service→Repository; vai trò: API, nghiệp vụ, tích hợp AI.»
- 1.5. React + TypeScript (Vite) — «SPA, component, TanStack Query, Zustand, Ant Design + Tailwind. LƯU Ý: đề tài dùng React + Vite, KHÔNG phải Next.js.»
- 1.6. PostgreSQL & pgvector — «RDBMS lưu dữ liệu nghiệp vụ; pgvector lưu vector học liệu phục vụ RAG.»
- 1.7. Neo4j (CSDL đồ thị) — «mô hình đồ thị User–Topic–Quiz, Cypher; vai trò: phân tích hành vi, gợi ý quiz & lộ trình học.» — `features/07-recommendation-neo4j.md`
- 1.8. Redis — «in-memory key-value; vai trò: cache, quota AI, trạng thái phòng đấu, **Pub/Sub đồng bộ real-time**, Sorted Set cho leaderboard.»
- 1.9. WebSocket (STOMP) — «kết nối 2 chiều real-time; vai trò: đồng bộ phòng đấu, live leaderboard, tính điểm theo tốc độ.» — `features/04-multiplayer-realtime.md`
- 1.10. Docker & Docker Compose — «đóng gói container; compose chạy PostgreSQL/Neo4j/Redis + BE/FE, đồng nhất môi trường.»
- 1.11. Kiến trúc Client–Server & phân lớp — «React ↔ Spring Boot qua REST/WebSocket/SSE; sơ đồ tổng thể (`architecture.md §1`). Đề tài dùng **monolith phân lớp**, KHÔNG microservices/RabbitMQ.»

---

## CHƯƠNG 2. KHẢO SÁT, PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG

### 2.1. Khảo sát hệ thống
- 2.1.1. Mục đích — «đánh giá nhu cầu học/luyện thi trắc nghiệm, thi đấu, mong muốn hỗ trợ AI; cơ sở chọn công nghệ & thiết kế.»
- 2.1.2. Phương pháp — «khảo sát biểu mẫu online; phân tích nền tảng có sẵn (Kahoot, Quizizz).»
- 2.1.3. Đối tượng khảo sát — «học sinh/sinh viên, giáo viên/người tạo nội dung, người ôn thi.»
- 2.1.4. Kết quả khảo sát — «đa dạng loại câu hỏi, thi đấu real-time, sinh đề nhanh từ tài liệu, gợi ý cá nhân hóa.»

### 2.2. Các yêu cầu chức năng
Nguồn: `docs/features/README.md` + từng file `features/01..16-*.md`.

| Nhóm | Chức năng chính | Tác nhân |
|------|-----------------|----------|
| Xác thực | Đăng ký, đăng nhập, refresh token, phân quyền RBAC | Guest→Learner/Creator/Admin |
| Quản lý Quiz | CRUD quiz/câu hỏi (5 loại), danh mục, tìm kiếm/lọc | Creator, Admin |
| Chơi đơn | Bắt đầu/nộp bài, chấm tự động, kết quả, lịch sử, leaderboard | Learner |
| Multiplayer | Tạo/join phòng, đồng bộ real-time, tính điểm tốc độ, live leaderboard | Learner, Creator |
| AI – Sinh đề (RAG) | Upload học liệu, sinh câu hỏi, duyệt trước xuất bản | Creator |
| AI – Chấm & trợ lý | Chấm tự luận, giải thích đáp án, chatbot RAG streaming | Learner |
| Gợi ý (Neo4j) | Gợi ý quiz, lộ trình học cá nhân hóa | Learner |
| Quản trị | Quản lý người dùng, nội dung, cấu hình AI, giám sát log & chi phí | Admin |
| *(mở rộng)* | Flashcard/SRS, chống gian lận, gamification, lớp học, BXH theo mùa, thông báo | Learner, Creator |

### 2.3. Các yêu cầu phi chức năng
«Hiệu năng real-time (mục tiêu P95 «…» ms), chịu tải N người/phòng, bảo mật (JWT/RBAC, chống prompt injection, quản lý API key — `docs/security.md`), độ tin cậy (fallback Gemini→Grok, circuit breaker), dễ bảo trì (phân lớp, test), giao diện thân thiện.»

### 2.4. Xác định các tác nhân của hệ thống
| Tác nhân | Mô tả |
|----------|-------|
| **Guest** | Khách chưa đăng nhập — **chỉ xem** danh sách/giới thiệu quiz công khai; không làm bài, không xem nội dung câu hỏi, không vào phòng đấu |
| **Learner** | Người học — chơi quiz, vào phòng đấu, chatbot, nhận gợi ý |
| **Creator** | Người tạo nội dung — quyền Learner + tạo quiz/sinh đề AI/tạo phòng |
| **Admin** | Quản trị — quản lý user/nội dung, cấu hình AI, giám sát |

> Một user có thể vừa là Learner vừa là Creator (`docs/overview.md §5`).

### 2.5. Xây dựng biểu đồ Usecase
- 2.5.1. Danh sách Usecase hệ thống — bảng `Tác nhân | Usecase`, liệt kê đủ theo 2.2.
- 2.5.2. Biểu đồ Usecase tổng quan — `Hình 2.x` (vẽ bằng draw.io/StarUML).

### 2.6. Đặc tả chi tiết các Usecase
Mỗi UC lõi dùng **mẫu bảng đặc tả** dưới đây. Điền dần theo tiến độ code.

| Mã Use case | «UC_TenChucNang» | Tên Use case | «Tên» |
|---|---|---|---|
| Tác nhân | «...» | | |
| Mô tả | «...» | | |
| Sự kiện kích hoạt | «...» | | |
| Tiền điều kiện | «...» | | |

*Luồng sự kiện chính:*

| # | Thực hiện bởi | Hành động |
|---|---------------|-----------|
| 1 | «...» | «...» |

*Luồng sự kiện thay thế:*

| # | Thực hiện bởi | Hành động |
|---|---------------|-----------|
| «x.1» | «...» | «...» |

*Hậu điều kiện:* «...»

> **Danh sách UC cần đặc tả:** Đăng ký · Đăng nhập · Đăng xuất · Quản lý Quiz · Quản lý câu hỏi · Làm bài (chơi đơn) · Xem kết quả & lịch sử · Tạo/Tham gia phòng đấu · Sinh đề bằng AI · Chấm tự luận AI · Chatbot trợ lý · Gợi ý quiz/lộ trình · Quản lý người dùng (Admin).

### 2.7. Phân tích các Usecase (biểu đồ trình tự & lớp)
Mỗi UC lõi: `Hình 2.x` biểu đồ trình tự + `Hình 2.y` biểu đồ lớp (boundary–control–entity). Vẽ dần theo tiến độ code.

### 2.8. Xây dựng cơ sở dữ liệu
- 2.8.1. Biểu đồ thực thể liên kết (ERD) — `Hình 2.x`, nguồn `docs/database.md`.
- 2.8.2. Các bảng trong CSDL — mỗi bảng một **mẫu bảng CSDL**; nguồn: `docs/database.md` + migration Flyway.

| Tên trường | Kiểu dữ liệu | Kích thước | Mô tả | Ghi chú |
|------------|--------------|-----------|-------|---------|
| «id» | «bigint» | | «...» | PK, NOT NULL |

> **Nhóm bảng** *(theo `docs/database.md` — dùng đúng tên này):* users · categories · quizzes, questions, question_options, quiz_questions · quiz_attempts, attempt_answers · game_rooms, game_room_players · learning_materials, material_chunks (embedding pgvector) · chat_sessions, chat_messages · ai_request_logs.
> *(Tính năng mở rộng nếu có làm):* flashcard_decks/flashcards/flashcard_reviews · proctoring_events/attempt_integrity · user_stats/badges/user_badges/daily_challenges · classrooms/classroom_members/assignments · seasons/season_rankings · notifications/notification_settings.
> **Neo4j** mô tả riêng (nút User/Topic/Quiz + quan hệ ATTEMPTED/WEAK_IN/SIMILAR_TO), KHÔNG trình bày như bảng SQL — nguồn `features/07-recommendation-neo4j.md`.

### 2.9. Thiết kế giao diện (wireframe)
`Hình 2.x` các màn chính: Đăng nhập/Đăng ký · Danh sách quiz · Làm bài · Kết quả · Sảnh & phòng đấu real-time · Sinh đề AI + duyệt · Chatbot · Gợi ý cho bạn · Trang Admin.

---

## CHƯƠNG 3. THỰC NGHIỆM VÀ ĐÁNH GIÁ

### 3.1. Môi trường triển khai
«Cấu hình máy; Docker Compose (PostgreSQL 16 + pgvector, Neo4j 5, Redis); Java 21 / Spring Boot 3.x / React 18 + Vite; biến môi trường (API key AI để ẩn) — `docs/tech-stack.md §5`, `docker-compose.yml`.»

### 3.2. Giao diện phía người dùng (Learner/Creator)
`Hình 3.x` ảnh chụp thực tế: Đăng ký/Đăng nhập · Danh sách & làm quiz · Kết quả · Phòng đấu real-time · Sinh đề AI · Chatbot · Gợi ý.

### 3.3. Giao diện phía quản trị (Admin)
`Hình 3.x` Quản lý người dùng · Quản lý nội dung · Cấu hình & giám sát AI (chi phí, token, fallback).

### 3.4. Kiểm thử chức năng
- 3.4.1. Kế hoạch kiểm thử — «Auth, Quiz CRUD, làm bài & chấm, phòng đấu, sinh đề AI, chatbot, gợi ý.»
- 3.4.2. Kịch bản kiểm thử — **mẫu bảng**:

| STT | Chức năng | Kịch bản | Các bước | Dữ liệu kiểm thử | Kết quả mong đợi | Kết quả |
|-----|-----------|----------|----------|------------------|------------------|---------|
| 1 | «Đăng ký» | «Hợp lệ» | «...» | «...» | «...» | «Đạt» |

- 3.4.3. Kết quả kiểm thử — «tỉ lệ pass/fail, trình duyệt đã test; kết quả unit/integration test (JUnit 5 + Mockito + Testcontainers) — nguồn skill `backend-testing`, `docs/roadmap.md §2.1`.»

### 3.5. Đánh giá hiệu năng real-time *(bắt buộc theo phiếu giao đề tài)*
«Load test phòng đấu bằng k6/Gatling: N người/phòng, đo **P95 latency**, **throughput**, tỉ lệ lỗi; so sánh có/không Redis Pub/Sub để chứng minh vai trò Redis. Bảng số liệu + biểu đồ.» — nguồn: skill `eval-and-load-test`, `docs/roadmap.md §2.2`.

| Kịch bản | Số client/phòng | P95 latency (ms) | Throughput (msg/s) | Tỉ lệ lỗi | Ghi chú |
|----------|-----------------|------------------|--------------------|-----------|---------|
| «...» | «...» | «...» | «...» | «...» | «...» |

### 3.6. Đánh giá độ chính xác AI *(bắt buộc theo phiếu giao đề tài)*
«(a) chất lượng sinh đề (tỉ lệ câu đạt chuẩn cấu trúc / cần chỉnh sửa); (b) độ chính xác chấm tự luận so với chấm tay; (c) tỉ lệ phản hồi chatbot có dẫn nguồn (grounded) vs. ảo giác; (d) thời gian & tỉ lệ chuyển fallback Gemini→Grok. Bảng số liệu + nhận xét.» — nguồn: skill `eval-and-load-test`, `docs/roadmap.md §2.3`.

| Hạng mục AI | Chỉ số | Cách đo | Kết quả |
|-------------|--------|---------|---------|
| Sinh đề RAG | % câu đạt chuẩn cấu trúc | «...» | «...» |
| Chấm tự luận | Sai lệch điểm so với chấm tay | «...» | «...» |
| Chatbot | % phản hồi grounded (có trích nguồn) | «...» | «...» |
| Fallback | Thời gian chuyển Gemini→Grok | «...» | «...» |

---

## KẾT LUẬN

1. **Những kết quả đạt được** — bám 4 trụ cột: nền web + Auth/RBAC; Multiplayer real-time; AI sinh đề/chấm/chatbot qua RAG; gợi ý Neo4j. Kèm số liệu 3.5 & 3.6. Đối chiếu mục tiêu ở Mở đầu.
2. **Hạn chế** — trung thực: phạm vi test, quy mô người dùng thật, tính năng tùy chọn chưa làm hết.
3. **Hướng phát triển** — `docs/roadmap.md` backlog mở rộng: gamification, lớp học & giao bài, thông báo/nhắc ôn, BXH theo mùa, nâng chất lượng AI, mobile app.

## TÀI LIỆU THAM KHẢO
Đánh số `[n]` theo mẫu trường, phải được trích dẫn trong bài: giáo trình phân tích thiết kế / Java / kiểm thử; tài liệu Spring Boot, React, PostgreSQL + pgvector, Neo4j, Redis; tài liệu & bài báo về RAG và LLM (Gemini). Ghi ngày truy cập cho nguồn web.

---

## Map chương → tài liệu nguồn (tra nhanh)

| Phần báo cáo | File nguồn |
|--------------|-----------|
| Bìa, Mở đầu (mục tiêu, kết quả mong muốn) | `docs/phieu_giao_de_tai/phieu_giao_de_tai.md`, `docs/overview.md` |
| Mở đầu (phạm vi, từ viết tắt) | `docs/overview.md §4, §6` |
| Chương 1 (công nghệ, kiến trúc) | `docs/tech-stack.md`, `docs/architecture.md` |
| Chương 2 (yêu cầu, UC, luồng) | `docs/features/README.md`, `docs/features/01..16-*.md`, `docs/api.md` |
| Chương 2.8 (ERD, bảng) | `docs/database.md`, migration Flyway |
| Chương 2.3 (bảo mật, NFR) | `docs/security.md` |
| Chương 3.1 (môi trường) | `docs/tech-stack.md §5`, `docker-compose.yml`, `.env.example` |
| Chương 3.4 (kiểm thử) | `docs/roadmap.md §2.1`, code `src/test/`, skill `backend-testing` |
| Chương 3.5–3.6 (số liệu) | `docs/roadmap.md §2.2–2.3`, skill `eval-and-load-test` |
| Kết luận | `docs/roadmap.md`, `docs/overview.md §3` |
| Số liệu/quyết định phát sinh hằng ngày | `docs/bao-cao/nhat-ky-tien-do.md` (mục "Ghi chú báo cáo") |

## Map chức năng → mục báo cáo

> Mỗi khi code xong 1 chức năng (BE + FE + test), cập nhật đúng các mục sau rồi tích vào `docs/bao-cao/nhat-ky-tien-do.md`.

| Chức năng (`docs/features/`) | Mục báo cáo cần cập nhật |
|------------------------------|--------------------------|
| 01-auth | 2.6 (đặc tả UC), 2.7 (trình tự/lớp), 2.8 (users), 3.2, 3.4 |
| 02-quiz-management | 2.6, 2.7, 2.8 (categories/quizzes/questions/question_options/quiz_questions), 3.2, 3.4 |
| 03-gameplay (chơi đơn) | 2.6, 2.7, 2.8 (quiz_attempts/attempt_answers), 3.2, 3.4 |
| 04-multiplayer-realtime | 1.8/1.9, 2.6, 2.8 (game_rooms), 3.2, **3.5 (load test)** |
| 05-ai-rag-generation | 1.2/1.3, 2.6, 2.8 (learning_materials/material_chunks), 3.2, **3.6** |
| 06-ai-grading | 1.2, 2.6, 3.2, **3.6** |
| 07-recommendation-neo4j | 1.7, 2.6, 2.8 (mô hình đồ thị), 3.2 |
| 08-ai-chatbot-rag | 1.3, 2.6, 2.8 (chat_sessions/chat_messages), 3.2, **3.6** |
| 09-analytics / 10-admin | 2.6, 3.3 |
| 11-flashcard-srs / 12-anti-cheat | 2.2 (mở rộng), 2.6, 3.2 |
| 13..16 (mở rộng) | 2.2 (mở rộng) hoặc Kết luận §3 (hướng phát triển) nếu chưa làm |
| Kiểm thử tổng hợp | 3.4, 3.5, 3.6 |

## Khác biệt so với báo cáo mẫu HaUI đang tham chiếu

Mẫu tham khảo (báo cáo của Đinh Đức Tài) dùng stack khác — **không copy nội dung công nghệ từ mẫu**, chỉ lấy sườn & cách trình bày:

| Mẫu | Đề tài này |
|-----|-----------|
| Next.js | **React 18 + Vite + TypeScript** |
| RabbitMQ | **Redis Pub/Sub** |
| Microservices | **Monolith phân lớp** (Controller→Service→Repository) |
| 1 vài tác nhân | **4 tác nhân**: Guest / Learner / Creator / Admin |
| — | Bổ sung **3.5 load test real-time** và **3.6 đánh giá độ chính xác AI** (bắt buộc theo phiếu) |
