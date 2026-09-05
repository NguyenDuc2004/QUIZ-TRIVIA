# Tổng quan dự án

## 1. Giới thiệu

Ứng dụng **Quiz/Trivia tích hợp AI** là nền tảng web cho phép người dùng tạo, quản lý và tham gia các bài trắc nghiệm. Điểm khác biệt so với các nền tảng truyền thống (Kahoot, Quizizz) là:

- **Phòng đấu trí thời gian thực** nhiều người chơi, độ trễ thấp.
- **Trí tuệ nhân tạo tạo sinh (Generative AI) qua RAG**: trợ lý học tập và tự động sinh đề thi từ tài liệu học liệu.
- **Cơ sở dữ liệu đồ thị Neo4j**: phân tích hành vi, gợi ý quiz và lộ trình học tập cá nhân hóa.

## 2. Mục tiêu (bám phiếu giao đề tài)

1. Xây dựng website Quiz/Trivia hoàn chỉnh, giao diện thân thiện, có **phòng đấu trí Multiplayer real-time** độ trễ thấp.
2. Tích hợp **Generative AI qua kiến trúc RAG** làm trợ lý học tập thông minh và tự động hóa sinh cấu trúc đề thi từ học liệu.
3. Ứng dụng **Neo4j** để phân tích hành vi/sở thích người dùng, đưa ra gợi ý bài thi và lộ trình học cá nhân hóa.
4. **Kiểm thử, đánh giá hiệu năng chịu tải** thời gian thực (Spring WebSocket + Redis) và **độ chính xác** của mô hình AI.

## 3. Kết quả dự kiến

- Website vận hành ổn định, đồng bộ dữ liệu real-time chính xác giữa người chơi trong phòng.
- Trợ lý AI giải đáp học liệu + module tự sinh bộ câu hỏi đạt chuẩn cấu trúc.
- Hệ thống gợi ý dựa trên đồ thị cho lộ trình học chính xác theo năng lực.
- Bộ tài liệu phân tích thiết kế chi tiết + mã nguồn bảo mật, hiệu suất cao, ứng dụng thực tiễn.

## 4. Phạm vi

- Web app: React SPA (frontend) + Spring Boot REST/WebSocket (backend).
- Loại câu hỏi: single-choice, multiple-choice, true/false, fill-in-blank, short-answer (tự luận).
- Chế độ chơi: luyện tập cá nhân, làm bài tính giờ, **phòng đấu real-time nhiều người**.
- Ôn tập bằng **flashcard + lặp lại ngắt quãng (SRS)**; **chống gian lận** trong chế độ thi.
- Mở rộng giáo dục & gắn kết: **gamification** (XP/badge/streak), **lớp học & giao bài**, **bảng xếp hạng theo mùa**, **thông báo/nhắc ôn**.
- LLM: Google Gemini (chính) → **Groq** (dự phòng khi Gemini lỗi — groq.com, *không phải* Grok của xAI).

## 5. Tác nhân & vai trò (RBAC)

| Tác nhân | Mô tả | Quyền chính |
|----------|-------|-------------|
| **Guest** | Khách chưa đăng nhập | **Chỉ xem** danh sách & thông tin giới thiệu quiz công khai (tiêu đề, mô tả, danh mục, độ khó, số câu). **Không được làm bài, không xem nội dung câu hỏi.** *Ngoại lệ duy nhất:* vào **phòng đấu** khi biết mã PIN **và** host đã bật `allowGuests` cho phòng đó |
| **Learner** | Người học đã đăng ký | Chơi quiz, vào phòng đấu, xem tiến độ, chatbot, nhận gợi ý, **nạp học liệu riêng của mình** (tối đa 10) |
| **Creator** | Người tạo nội dung | Quyền Learner + tạo/sửa quiz, sinh đề bằng AI, tạo phòng, xem thống kê |
| **Admin** | Quản trị | Quản lý user & nội dung, cấu hình AI provider, giám sát log & chi phí |

> Một user có thể vừa là Learner vừa là Creator.

**Chuyển giữa Learner và Creator là việc người dùng tự làm** (`PATCH /auth/my-role`, trang Hồ sơ), không
qua admin duyệt. Lý do: **màn đăng ký vốn đã cho tự chọn Creator**, nên dựng hàng chờ duyệt cho người đã
có tài khoản — trong khi người mới chỉ cần bấm một ô — là thủ tục hình thức, ai bị từ chối chỉ việc tạo
tài khoản thứ hai. Chi tiết và các chốt chặn ở `features/01-auth.md`.

**Ranh giới an ninh của dự án là ADMIN, không phải CREATOR.** Admin không tự đăng ký được, không tự cấp
cho mình được, và tài khoản admin đầu tiên do `AdminBootstrap` tạo từ biến môi trường lúc khởi động
(`security.md §1`).

**Quy tắc bắt buộc đăng nhập:** mọi hành vi tạo ra dữ liệu học tập đều yêu cầu tài khoản — làm bài (attempt), vào phòng đấu, chatbot, gợi ý, flashcard. Guest chỉ được duyệt nội dung công khai để biết hệ thống có gì rồi đăng ký.

| Endpoint | Guest |
|---|---|
| `GET /api/v1/quizzes`, `GET /api/v1/quizzes/{id}` (visibility = public) | ✅ cho phép — nhưng **không trả về danh sách câu hỏi** |
| `POST /api/v1/quizzes/{id}/attempts` và toàn bộ `/api/v1/attempts/**` | ❌ 401 |
| `GET /api/v1/rooms/{pin}`, `GET /api/v1/rooms/avatars` | ✅ mở — mã PIN là thứ chặn cửa |
| `POST /api/v1/rooms/{pin}/join-as-guest` | ✅ mở, **403** nếu host không bật cho khách |
| `POST /api/v1/rooms` (mở phòng) | ❌ 401 |
| `/api/v1/rooms/**`, WebSocket `/ws` | ❌ 401 (JWT xác thực ngay lúc handshake) |
| `/api/v1/ai/**`, `/api/v1/recommendations/**`, `/api/v1/users/me` | ❌ 401 |

> Hệ quả kỹ thuật: `quiz_attempts.user_id` **NOT NULL** — không có attempt ẩn danh, không cần gộp dữ liệu khách vào tài khoản sau khi đăng ký. Mọi thống kê, leaderboard và đồ thị Neo4j đều gắn với một user thật.
>
> **Phòng đấu là ngoại lệ có chủ đích** (V7): `game_room_players.user_id` được phép NULL để khách
> vãng lai quét QR vào chơi. Đổi lại, dữ liệu của khách **chỉ sống trong một ván**: không có lịch
> sử làm bài, không vào thống kê cá nhân, không lên đồ thị gợi ý. Host phải chủ động bật
> `allowGuests`; mặc định vẫn là tắt.

## 6. Định nghĩa & từ viết tắt

| Thuật ngữ | Ý nghĩa |
|-----------|---------|
| LLM | Large Language Model — mô hình ngôn ngữ lớn |
| RAG | Retrieval-Augmented Generation — sinh có tăng cường truy xuất |
| RBAC | Role-Based Access Control |
| JWT | JSON Web Token |
| SSE | Server-Sent Events (streaming phản hồi) |
| STOMP | Giao thức nhắn tin trên WebSocket |
| Adaptive | Điều chỉnh độ khó theo năng lực người dùng |
| Attempt | Một lần làm bài của người dùng |

## 7. Đối chiếu phiếu giao đề tài

| Yêu cầu trong phiếu | Tài liệu liên quan |
|---------------------|--------------------|
| Multiplayer real-time độ trễ thấp | [features/04-multiplayer-realtime.md](features/04-multiplayer-realtime.md) |
| Generative AI + RAG (trợ lý + sinh đề) | [features/05-ai-rag-generation.md](features/05-ai-rag-generation.md), [features/08-ai-chatbot-rag.md](features/08-ai-chatbot-rag.md) |
| Neo4j phân tích hành vi & gợi ý | [features/07-recommendation-neo4j.md](features/07-recommendation-neo4j.md) |
| Spring WebSocket + Redis | [architecture.md](architecture.md), [features/04-multiplayer-realtime.md](features/04-multiplayer-realtime.md) |
| Kiểm thử hiệu năng & độ chính xác AI | [roadmap.md](roadmap.md) |
