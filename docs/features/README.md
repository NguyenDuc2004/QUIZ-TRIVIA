# Danh sách tính năng

Mỗi tính năng có file đặc tả riêng. Mức ưu tiên: **[M]** Must · **[S]** Should · **[C]** Could.

| # | Tính năng | File | Ưu tiên | Trụ cột phiếu | Trạng thái |
|---|-----------|------|---------|---------------|---|
| 01 | Xác thực & phân quyền | [01-auth.md](01-auth.md) | [M] | | ✅ xong |
| 02 | Quản lý Quiz & Câu hỏi | [02-quiz-management.md](02-quiz-management.md) | [M] | | ✅ xong |
| 03 | Chơi quiz (đơn) | [03-gameplay.md](03-gameplay.md) | [M] | | ✅ xong |
| 04 | Phòng đấu real-time (Multiplayer) | [04-multiplayer-realtime.md](04-multiplayer-realtime.md) | [M] | ✅ Real-time | ✅ xong |
| 05 | AI sinh đề từ học liệu (RAG) | [05-ai-rag-generation.md](05-ai-rag-generation.md) | [M] | ✅ AI/RAG | ✅ xong |
| 06 | AI chấm & giải thích tự luận | [06-ai-grading.md](06-ai-grading.md) | [M] | ✅ AI | ✅ xong |
| 07 | Gợi ý cá nhân hóa (Neo4j) | [07-recommendation-neo4j.md](07-recommendation-neo4j.md) | [M] | ✅ Neo4j | ✅ xong (hoãn FR-32 adaptive) |
| 08 | Trợ lý học tập RAG chatbot | [08-ai-chatbot-rag.md](08-ai-chatbot-rag.md) | [M] | ✅ AI/RAG | ✅ xong |
| 09 | Thống kê & báo cáo | [09-analytics.md](09-analytics.md) | [S] | | ✅ xong |
| 10 | Quản trị (Admin) | [10-admin.md](10-admin.md) | [S] | | ✅ xong |
| 11 | Flashcard & lặp lại ngắt quãng (SRS) | [11-flashcard-srs.md](11-flashcard-srs.md) | [S] | ➕ AI/RAG | ✅ xong |
| 12 | Chống gian lận thi (Anti-Cheat) | [12-anti-cheat.md](12-anti-cheat.md) | [S] | ➕ AI + Real-time | ✅ xong — kèm cảnh báo live phòng đấu và thi nghiêm ngặt (chỉ bỏ FR-44) |
| 13 | Gamification (XP, badge, streak, daily) | [13-gamification.md](13-gamification.md) | [S] | ➕ Gắn kết | ✅ xong (FR-53 trả ở tính năng 16) |
| 14 | Lớp học & giao bài (Classroom) | [14-classroom.md](14-classroom.md) | [S] | ➕ Giáo dục | ✅ xong — kèm xuất bảng điểm CSV (không làm PDF) |
| 15 | Bảng xếp hạng theo mùa | [15-seasonal-leaderboard.md](15-seasonal-leaderboard.md) | [S] | ➕ Redis | ✅ xong (phạm vi toàn hệ thống) |
| 16 | Thông báo & nhắc ôn tập | [16-notifications.md](16-notifications.md) | [S] | ➕ SRS + Real-time | ✅ xong (hoãn email, bỏ quiet hours) |

## Mẫu cấu trúc mỗi file feature

- **Mục tiêu** — vấn đề tính năng giải quyết.
- **User story / Use case** — ai làm gì.
- **Yêu cầu chức năng (FR)** — chi tiết.
- **Luồng xử lý** — các bước.
- **API liên quan** — trỏ tới [api.md](../api.md).
- **Dữ liệu liên quan** — trỏ tới [database.md](../database.md).
- **Ghi chú kỹ thuật / Ràng buộc.**
