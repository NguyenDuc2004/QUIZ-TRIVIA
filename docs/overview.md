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
- LLM: Google Gemini (chính) → xAI Grok (dự phòng khi Gemini lỗi).

## 5. Tác nhân & vai trò (RBAC)

| Tác nhân | Mô tả | Quyền chính |
|----------|-------|-------------|
| **Guest** | Khách chưa đăng nhập | Xem quiz công khai, chơi thử giới hạn |
| **Learner** | Người học đã đăng ký | Chơi quiz, vào phòng đấu, xem tiến độ, chatbot, nhận gợi ý |
| **Creator** | Người tạo nội dung | Quyền Learner + tạo/sửa quiz, sinh đề bằng AI, tạo phòng, xem thống kê |
| **Admin** | Quản trị | Quản lý user & nội dung, cấu hình AI provider, giám sát log & chi phí |

> Một user có thể vừa là Learner vừa là Creator.

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
