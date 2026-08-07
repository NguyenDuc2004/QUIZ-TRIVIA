# Kế hoạch & Tiến độ công việc theo ngày

> **Thời gian:** 20/07/2026 → 20/09/2026 (9 tuần).
> **Cách dùng:** Làm xong việc nào, đổi `[ ]` thành `[x]`. Cuối mỗi tuần cập nhật mục "Tổng kết tuần".
> **Quy ước lịch:** Thứ 2–Thứ 7 làm việc, **Chủ nhật = đệm/ôn/bù việc trễ**.
> **Ưu tiên:** MVP 4 trụ cột (Auth + Quiz/Chơi đơn + Multiplayer + AI-RAG + Neo4j) phải xong trước tuần 8.

**Chú thích trạng thái tuần:** 🔴 chưa bắt đầu · 🟡 đang làm · 🟢 xong

---

## Phạm vi & mức độ hoàn thiện

> Đây là **đồ án sinh viên tốt nghiệp**, không phải sản phẩm production. Mục tiêu số 1 là **đủ 4 trụ cột + chạy được + demo mượt + có số liệu để viết báo cáo**.

**Chuẩn "đủ để bảo vệ" (bắt buộc):**
- Tính năng chạy đúng ở happy path, demo không lỗi.
- Có test cho phần lõi (không cần phủ 100%).
- Có số liệu load test real-time + đánh giá độ chính xác AI (phiếu yêu cầu).
- Kiến trúc rõ ràng, tài liệu API đầy đủ.

**Mức "production — nên làm nếu còn thời gian", KHÔNG chặn tiến độ:** _(đánh dấu `[tùy chọn]` trong kế hoạch)_
- Tối ưu hiệu năng sâu, phủ test cao, CI/CD, monitoring/observability.
- Xử lý mọi edge case, retry/backoff tinh vi, rate-limit đầy đủ.
- Bảo mật nâng cao ngoài JWT/RBAC/validate cơ bản.

> **Nguyên tắc:** khi kẹt thời gian, ưu tiên **rộng** (đủ 4 trụ cột chạy được) hơn **sâu** (một phần hoàn hảo). Việc `[tùy chọn]` luôn nhường chỗ cho MVP.

---

## Tuần 1 — Nền tảng & hạ tầng (20–26/07) 🟡

- **T2 20/07** — [x] Khởi tạo repo, cấu trúc thư mục BE (Spring Boot) + FE (React/Vite) *(xong 05/08)* · [x] Docker Compose: PostgreSQL, Neo4j, Redis chạy được *(xong 05/08)*
- **T3 21/07** — [x] Cấu hình Flyway + migration đầu tiên (`V1__init.sql`: extension `pgvector`, bảng `users`) · [x] Cấu hình OpenAPI/Swagger *(xong 05/08)*
- **T4 22/07** — [x] Spring Security + JWT (access + refresh token có rotation) · [x] Cấu trúc package chuẩn (Controller→Service→Repository) *(xong 05/08)*
- **T5 23/07** — [x] API đăng ký / đăng nhập / refresh / đăng xuất / đổi mật khẩu · [x] RBAC (Guest/Learner/Creator/Admin) *(xong 05/08)*
- **T6 24/07** — [x] FE: routing, trang Đăng nhập/Đăng ký, trang hồ sơ · [x] Kết nối FE↔BE (axios + TanStack Query), lưu token, tự refresh khi 401 *(xong 05/08)*
- **T7 25/07** — [x] Guard route (`ProtectedRoute`) · [x] Test: 30 test pass (AuthService, UserRepository/Testcontainers, AuthController, integration) *(xong 05/08)*
  - Còn nợ: FR-4 quên/đặt lại mật khẩu (cần SMTP), FR-3 OAuth2 Google (mức [S])
- **CN 26/07** — [ ] Đệm / dọn nợ / review tuần

**Tổng kết tuần 1:** _(ghi lại việc chưa xong, chuyển sang tuần sau)_

---

## Tuần 2 — Quiz & Câu hỏi (27/07–02/08) 🔴

- **T2 27/07** — [x] Thiết kế schema: `categories`, `quizzes`, `questions`, `question_options`, `quiz_questions` · [x] Migration Flyway `V2` *(xong 06/08)*
- **T3 28/07** — [x] API CRUD Quiz (Creator) · [x] Phân trang, tìm kiếm, lọc theo danh mục & độ khó *(xong 06/08)*
- **T4 29/07** — [x] API CRUD Câu hỏi (5 loại + luật riêng từng loại) · [x] Validate DTO *(xong 06/08)*
- **T5 30/07** — [x] FE: trang danh sách quiz + tìm kiếm/lọc · [x] FE: form tạo/sửa quiz *(xong 06/08)*
- **T6 31/07** — [x] FE: form câu hỏi theo từng loại + màn soạn quiz (chọn câu, đổi thứ tự) · [x] Upload ảnh — đã làm cho **ảnh bìa quiz** *(xong 06/08)*; ảnh từng câu hỏi dùng lại được endpoint này
- **T7 01/08** — [x] Phân quyền chỉnh sửa (chủ sở hữu/Admin) · [x] Test: 55/55 pass (thêm 25 ca kiểm chứng HTTP) *(xong 06/08)*
- **CN 02/08** — [ ] Đệm / review tuần

**Tổng kết tuần 2:** _(...)_

---

## Tuần 3 — Chơi quiz đơn (03–09/08) 🟢

- **T2 03/08** — [x] Schema `attempt`, `attempt_answer` · [x] API bắt đầu / nộp bài
- **T3 04/08** — [x] Chấm tự động (trắc nghiệm) + tính điểm · [x] Lưu lịch sử làm bài
- **T4 05/08** — [x] API kết quả + giải thích đáp án · [x] Chế độ luyện tập & làm bài tính giờ
- **T5 06/08** — [x] FE: màn làm bài (timer, điều hướng câu) · [x] FE: màn kết quả + review đáp án
- **T6 07/08** — [x] Leaderboard cơ bản · [x] FE: trang lịch sử làm bài · [ ] Thống kê cá nhân *(để cùng features/09)*
- **T7 08/08** — [x] Unit/integration test luồng làm bài (Testcontainers) · [x] Fix bug
- **CN 09/08** — [ ] Đệm / review tuần

**Tổng kết tuần 3:** Làm gộp trong ngày 06/08, sớm hơn kế hoạch. Xong FR-13…FR-19 (trừ xếp hạng theo
danh mục và giờ riêng từng câu). 97/97 test pass, 48/48 + 19/19 ca kiểm chứng HTTP thật.
Còn nợ: chấm tự luận bằng AI (features/06) và xem lại giao diện bằng mắt trên trình duyệt.
— **Mốc: xong nền web cơ bản (auth + quiz + chơi đơn).** ✅

---

## Tuần 4 — Multiplayer real-time ⭐ (10–16/08) 🟢

- **T2 10/08** — [x] Cấu hình Spring WebSocket (STOMP) + Redis Pub/Sub · [x] Model trạng thái phòng
- **T3 11/08** — [x] Tạo/join phòng, danh sách người chơi real-time · [x] Xử lý host/start
- **T4 12/08** — [x] Đồng bộ câu hỏi & đáp án theo lượt · [x] Tính điểm theo tốc độ trả lời
- **T5 13/08** — [x] Live leaderboard trong phòng · [x] Reconnect + rời phòng
- **T6 14/08** — [x] FE: sảnh phòng + phòng chờ · [x] FE: màn chơi real-time + bảng xếp hạng trực tiếp
- **T7 15/08** — [x] Test đồng bộ nhiều client (2 client STOMP thật) · [x] Fix race condition (khoá Redis khi cộng điểm)
- **CN 16/08** — [ ] Đệm / review tuần

**Tổng kết tuần 4:** Làm gộp trong ngày 07/08, sớm hơn kế hoạch. Xong FR-20…FR-25.
113/113 test pass, 30/30 ca kiểm chứng với 2 client thật.
Còn nợ: chưa mở hai trình duyệt nhìn tận mắt; chưa đo tải (để tuần 8).
— **Mốc: trụ cột Multiplayer hoạt động.** ✅

---

## Tuần 5 — Lõi AI + RAG ⭐ (17–23/08) 🟡

- **T2 17/08** — [x] `AiOrchestrator` + `GeminiProvider` + `GrokProvider` · [x] Fallback *(chưa demo được vì thiếu key Grok)* · [ ] Quota/cache · [ ] `[tùy chọn]` circuit breaker
- **T3 18/08** — [x] Structured output + validate JSON · [x] Logging chi phí/token (`ai_request_logs`)
- **T4 19/08** — [x] Pipeline RAG: ingest học liệu (Tika → chunk) · [x] Embedding → lưu `pgvector`
- **T5 20/08** — [x] Similarity search (retrieval) · [x] FE: upload học liệu
- **T6 21/08** — [x] Sinh đề từ học liệu (RAG) · [x] Human-in-the-loop: duyệt trước khi lưu
- **T7 22/08** — [x] FE: màn sinh đề bằng AI + duyệt câu hỏi · [x] Test pipeline (25 ca)
- **CN 23/08** — [ ] Đệm / review tuần

**Tổng kết tuần 5:** Làm gộp trong ngày 07/08, sớm hơn kế hoạch. 138/138 test pass và **22/22 ca
nghiệm thu với Gemini thật** — nạp học liệu → embedding → similarity search → sinh đề bám tài liệu
→ Creator duyệt. Còn nợ: chưa có key Grok nên chưa demo được fallback; chưa giới hạn hạn mức và
chưa cache theo hash(prompt).
— **Mốc: trụ cột AI sinh đề (RAG) hoạt động.** ✅

---

## Tuần 6 — AI chấm & trợ lý (24–30/08) 🔴

- **T2 24/08** — [x] Chấm câu tự luận (short-answer) bằng AI + rubric · [x] Trả điểm + nhận xét *(xong 09/08)*
- **T3 25/08** — [x] Giải thích đáp án bằng AI · [x] Gộp vào luồng kết quả làm bài *(xong 09/08)*
- **T4 26/08** — [ ] Chatbot trợ lý học tập RAG (backend) · [ ] SSE streaming phản hồi
- **T5 27/08** — [ ] FE: giao diện chatbot streaming · [ ] Chống ảo giác (grounding, trích nguồn)
- **T6 28/08** — [ ] Test độ chính xác chấm & grounding · [ ] Fix chất lượng prompt
- **T7 29/08** — [ ] Demo fallback Gemini→Grok, đo thời gian chuyển · [ ] Fix bug AI
- **CN 30/08** — [ ] Đệm / review tuần

**Tổng kết tuần 6:** _(...)_ — **Mốc: trụ cột AI (sinh đề + chấm + chatbot) hoàn chỉnh.**

---

## Tuần 7 — Gợi ý Neo4j ⭐ (31/08–06/09) 🔴

- **T2 31/08** — [ ] Mô hình đồ thị (User–Topic–Quiz) · [ ] Job đồng bộ PostgreSQL → Neo4j (idempotent)
- **T3 01/09** — [ ] Cập nhật đồ thị theo hành vi làm bài · [ ] Kiểm tra lệch dữ liệu định kỳ
- **T4 02/09** — [ ] Cypher: gợi ý quiz theo sở thích/năng lực · [ ] API gợi ý
- **T5 03/09** — [ ] Cypher: lộ trình học cá nhân hóa · [ ] API lộ trình
- **T6 04/09** — [ ] FE: khu vực "Gợi ý cho bạn" + lộ trình học · [ ] Kiểm tra tính hợp lý gợi ý
- **T7 05/09** — [ ] Test đồng bộ + truy vấn · [ ] Fix bug
- **CN 06/09** — [ ] Đệm / review tuần

**Tổng kết tuần 7:** _(...)_ — **Mốc: đủ 4 trụ cột MVP để bảo vệ.**

---

## Tuần 8 — Tính năng nâng cao & Kiểm thử (07–13/09) 🔴

- **T2 07/09** — [ ] `[tùy chọn]` **Flashcard + SRS** (tái dùng pipeline RAG) — backend · [ ] `[tùy chọn]` Thuật toán lặp lại ngắt quãng
- **T3 08/09** — [ ] `[tùy chọn]` FE Flashcard · [ ] `[tùy chọn]` **Chống gian lận**: thu thập hành vi + risk score
- **T4 09/09** — [ ] **Load test real-time** (k6/Gatling): N người/phòng, đo P95 latency, throughput · [ ] So sánh có/không Redis Pub/Sub
- **T5 10/09** — [ ] **Đánh giá độ chính xác AI**: sinh đề, chấm tự luận, tỉ lệ grounded — thu thập số liệu
- **T6 11/09** — [ ] Tối ưu hiệu năng cơ bản (index, cache) · [ ] Rà soát bảo mật (JWT, RBAC, prompt injection) · [ ] `[tùy chọn]` tối ưu query sâu
- **T7 12/09** — [ ] Sửa lỗi tổng hợp · [ ] Bổ sung test còn thiếu
- **CN 13/09** — [ ] Đệm / review tuần

**Tổng kết tuần 8:** _(...)_ — **Mốc: có số liệu kiểm thử cho báo cáo.**

> ⚠️ Nếu thiếu thời gian: lùi Flashcard **hoặc** Anti-cheat sang backlog. **Không lùi** load test & đánh giá AI (phiếu yêu cầu).

---

## Tuần 9 — Hoàn thiện (14–20/09) 🔴

- **T2 14/09** — [ ] Dockerize hoàn chỉnh (build FE+BE, compose full stack) · [ ] Biến môi trường `.env` · [ ] `[tùy chọn]` CI/CD, monitoring
- **T3 15/09** — [ ] Hoàn thiện tài liệu API (Swagger) · [ ] README hướng dẫn chạy
- **T4 16/09** — [ ] Viết báo cáo: kiến trúc, số liệu real-time, số liệu AI, gợi ý Neo4j
- **T5 17/09** — [ ] Chuẩn bị kịch bản demo (happy path 4 trụ cột) · [ ] Data mẫu để demo
- **T6 18/09** — [ ] Chạy thử demo end-to-end · [ ] Dự phòng sự cố demo
- **T7 19/09** — [ ] Rà soát báo cáo + slide · [ ] Đệm cuối
- **CN 20/09** — [ ] **HẠN NỘP** — kiểm tra lần cuối, nộp

**Tổng kết tuần 9:** _(...)_ — **Bảo vệ đồ án.**

---

## Backlog mở rộng — toàn bộ `[tùy chọn]` (chỉ làm nếu MVP đã ổn / hướng phát triển)

- [ ] `[tùy chọn]` Gamification (XP, badge, streak, daily challenge)
- [ ] `[tùy chọn]` Lớp học & giao bài
- [ ] `[tùy chọn]` Thông báo & nhắc ôn tập (scheduler + SRS + WebSocket)
- [ ] `[tùy chọn]` Bảng xếp hạng theo mùa (Redis Sorted Set)

> Thứ tự ưu tiên: Gamification → Classroom → Notifications → Seasonal leaderboard.

---

## Bảng theo dõi nhanh 4 trụ cột (MVP)

| Trụ cột | Tuần | Trạng thái |
|---|---|---|
| Auth + Quiz + Chơi đơn | 1–3 | ⬜ |
| Multiplayer real-time | 4 | ⬜ |
| AI sinh đề + RAG + chấm + chatbot | 5–6 | ⬜ |
| Gợi ý Neo4j | 7 | ⬜ |

_(Đổi ⬜ → ✅ khi trụ cột hoàn thành và test xong.)_
