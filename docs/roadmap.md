# Lộ trình & Kiểm thử

> **Thời gian thực hiện theo phiếu:** 20/07/2026 → 20/09/2026 (~9 tuần).

## 1. Lộ trình theo tuần

### Tuần 1 — Nền tảng & hạ tầng
- Khởi tạo Spring Boot + React + Docker Compose (PostgreSQL, Neo4j, Redis).
- Cấu hình Flyway, Security, OpenAPI, cấu trúc package.
- Xác thực/phân quyền (JWT, RBAC).

### Tuần 2 — Quiz & Câu hỏi
- CRUD quiz, ngân hàng câu hỏi, danh mục.
- Frontend: trang quản lý quiz, tạo/sửa câu hỏi.

### Tuần 3 — Chơi quiz đơn
- Luồng làm bài, chấm tự động, lịch sử, kết quả + giải thích.
- Thống kê cơ bản, leaderboard.

### Tuần 4 — Multiplayer real-time ⭐
- WebSocket (STOMP) + Redis Pub/Sub; tạo/join phòng.
- Đồng bộ câu hỏi, đáp án, live leaderboard.

### Tuần 5 — Lõi AI + RAG ⭐
- AiOrchestrator + GeminiProvider + GroqProvider + fallback + circuit breaker.
- Pipeline RAG: ingest học liệu (Tika → chunk → embedding → pgvector).
- Sinh đề từ học liệu + human-in-the-loop.

### Tuần 6 — AI chấm & trợ lý
- Chấm & giải thích câu tự luận.
- Trợ lý học tập RAG chatbot (SSE streaming).

### Tuần 7 — Gợi ý Neo4j ⭐
- Đồng bộ hành vi PostgreSQL → Neo4j.
- Truy vấn Cypher: gợi ý quiz + lộ trình học cá nhân hóa.

### Tuần 8 — Tính năng nâng cao & Kiểm thử
- **Flashcard + SRS** ([features/11](features/11-flashcard-srs.md)) — tái dùng pipeline RAG.
- **Chống gian lận thi** ([features/12](features/12-anti-cheat.md)) — thu thập hành vi + risk score + AI phân tích.
- Load testing real-time; đánh giá độ chính xác AI.
- Tối ưu hiệu năng, bảo mật, sửa lỗi.

> Flashcard & Anti-cheat là mức [S] — làm sau khi MVP (đủ 4 trụ cột) đã ổn định. Nếu thiếu thời gian, có thể lùi 1 trong 2 sang phần "mở rộng".

### Backlog mở rộng (làm nếu còn thời gian / hướng phát triển tương lai)
Các tính năng mức [S]/[C] tăng giá trị sản phẩm nhưng không thuộc 4 trụ cột bắt buộc:
- **Gamification** ([features/13](features/13-gamification.md)) — XP, badge, streak, daily challenge.
- **Lớp học & giao bài** ([features/14](features/14-classroom.md)) — biến app thành công cụ giáo dục.
- **Bảng xếp hạng theo mùa** ([features/15](features/15-seasonal-leaderboard.md)) — Redis Sorted Set.
- **Thông báo & nhắc ôn tập** ([features/16](features/16-notifications.md)) — scheduler + SRS + WebSocket.

> Thứ tự ưu tiên đề xuất trong backlog: Gamification → Classroom → Notifications → Seasonal leaderboard.

### Tuần 9 — Hoàn thiện
- Dockerize hoàn chỉnh, hoàn thiện tài liệu, chuẩn bị demo & báo cáo.

> **MVP tối thiểu để bảo vệ:** Xác thực + CRUD quiz + chơi đơn + **Multiplayer real-time** + **AI sinh đề (RAG)** + **gợi ý Neo4j** (đủ 4 trụ cột của phiếu).

## 2. Kiểm thử

### 2.1. Kiểm thử chức năng
- Unit test (JUnit 5 + Mockito) cho service.
- Integration test với **Testcontainers** (PostgreSQL, Neo4j, Redis thật trong container).

### 2.2. Kiểm thử hiệu năng chịu tải real-time (theo phiếu)
- Công cụ: **k6 / Gatling / JMeter** (hỗ trợ WebSocket).
- Kịch bản: mô phỏng N người chơi đồng thời trong 1 phòng, đo:
  - **Độ trễ (latency)** đồng bộ câu hỏi/đáp án (mục tiêu P95 thấp).
  - **Throughput** số message/giây.
  - Ổn định khi tăng số phòng/người chơi.
- So sánh có/không có Redis Pub/Sub (chứng minh vai trò Redis).

### 2.3. Đánh giá độ chính xác AI (theo phiếu)
- **Sinh đề:** lấy mẫu N câu AI sinh, chuyên gia/người chấm đánh giá tính đúng, độ liên quan học liệu, chất lượng đáp án nhiễu → tỉ lệ đạt.
- **Chấm tự luận:** so điểm AI với điểm người chấm trên tập mẫu → sai số trung bình / độ tương quan.
- **RAG:** đo tỉ lệ câu trả lời có căn cứ (grounded) vs. ảo giác.
- **Fallback:** ✅ đo 20/08 — Groq phục vụ thật 9/9 câu qua ứng dụng, độ trễ TB 2 039 ms so với 10 526 ms của Gemini. *Một lần chuyển thật do lỗi tạm thời chưa quan sát được* (key sai là lỗi vĩnh viễn nên đúng thiết kế là KHÔNG chuyển); logic chuyển kiểm bằng 6 unit test. Xem so-lieu-3.6 §7.

## 3. Tiêu chí đánh giá (cho báo cáo)
- Độ hoàn thiện tính năng theo mức ưu tiên [M].
- Multiplayer đồng bộ chính xác, độ trễ thấp (có số liệu).
- Chất lượng câu hỏi AI & độ chính xác chấm tự luận (có số liệu).
- Gợi ý Neo4j hợp lý theo năng lực.
- Kiến trúc rõ ràng, có test, tài liệu API đầy đủ.

## 4. Rủi ro & giảm thiểu

| Rủi ro | Giảm thiểu |
|--------|-----------|
| Giới hạn hạn mức/chi phí API AI | Fallback Groq (có gói miễn phí), cache, quota, chọn model rẻ |
| LLM trả sai định dạng JSON | Structured output + validate + retry |
| Chất lượng câu hỏi AI không ổn định | Human-in-the-loop duyệt trước khi xuất bản |
| Đồng bộ real-time phức tạp | Redis Pub/Sub + kiểm thử tải sớm |
| Đồng bộ PostgreSQL ↔ Neo4j lệch | Job nền idempotent, kiểm tra định kỳ |
| Phạm vi rộng, thời gian 2 tháng | Ưu tiên MVP đủ 4 trụ cột, tính năng phụ để sau |
