# Đặc tả API

REST, prefix `/api/v1`, trả JSON, xác thực Bearer JWT. Tài liệu tự sinh bằng **Swagger/OpenAPI** (`/swagger-ui.html`). Real-time dùng **WebSocket (STOMP)**.

## 1. Xác thực — `/auth`
```
POST   /api/v1/auth/register        Đăng ký → 201 + access & refresh token          ✅
POST   /api/v1/auth/login           Đăng nhập → access + refresh token              ✅
POST   /api/v1/auth/refresh         Làm mới token (rotation: token cũ bị thu hồi)   ✅
POST   /api/v1/auth/logout          Đăng xuất → 204, thu hồi refresh token          ✅
POST   /api/v1/auth/change-password Đổi mật khẩu (cần đăng nhập) → 204              ✅
POST   /api/v1/auth/forgot-password Quên mật khẩu                                   ⏳ cần cấu hình SMTP
POST   /api/v1/auth/reset-password  Đặt lại mật khẩu bằng token gửi qua email       ⏳ cần cấu hình SMTP
```
> `register`, `login`, `refresh`, `logout` mở cho Guest; `change-password` yêu cầu Bearer token.
> Access token sống 15 phút, refresh token 14 ngày (lưu Redis key `session:{token}`).

## 2. Người dùng — `/users`
```
GET    /api/v1/users/me             Hồ sơ hiện tại                                  ✅
PUT    /api/v1/users/me             Cập nhật tên hiển thị / ảnh đại diện            ✅
GET    /api/v1/users/me/progress    Tiến độ học tập                                 ⏳ sau khi có attempt
```

## 3. Quiz & Câu hỏi — `/quizzes`, `/questions`
```
GET    /api/v1/quizzes              Danh sách (filter: category, difficulty, q)
POST   /api/v1/quizzes              Tạo quiz
GET    /api/v1/quizzes/{id}         Chi tiết
PUT    /api/v1/quizzes/{id}         Cập nhật
DELETE /api/v1/quizzes/{id}         Xóa
GET    /api/v1/questions            Ngân hàng câu hỏi
POST   /api/v1/questions            Tạo câu hỏi
PUT    /api/v1/questions/{id}       Sửa
DELETE /api/v1/questions/{id}       Xóa
```

## 4. Chơi quiz (đơn) — `/attempts`
```
POST   /api/v1/quizzes/{id}/attempts   Bắt đầu làm bài
POST   /api/v1/attempts/{id}/answers   Nộp câu trả lời
POST   /api/v1/attempts/{id}/submit    Nộp bài & chấm
GET    /api/v1/attempts/{id}           Kết quả chi tiết
GET    /api/v1/attempts                 Lịch sử làm bài
GET    /api/v1/quizzes/{id}/leaderboard Bảng xếp hạng
```

## 5. Phòng đấu real-time — REST + WebSocket

### 5.1. REST (quản lý phòng)
```
POST   /api/v1/rooms                 Tạo phòng (host) → room_code
POST   /api/v1/rooms/{code}/join     Tham gia phòng
GET    /api/v1/rooms/{code}          Thông tin phòng
```

### 5.2. WebSocket (STOMP) — endpoint `/ws`
```
SUBSCRIBE /topic/room/{code}         Nhận sự kiện phòng (player join/leave, câu hỏi, leaderboard)
SEND      /app/room/{code}/start     Host bắt đầu ván
SEND      /app/room/{code}/answer    Người chơi gửi đáp án { questionId, answer, timeMs }
SEND      /app/room/{code}/next      Host chuyển câu tiếp theo
```
**Sự kiện server phát về (message payload `type`):** `PLAYER_JOINED`, `GAME_STARTED`, `QUESTION`, `ANSWER_RESULT`, `LEADERBOARD`, `GAME_FINISHED`.

## 6. Tính năng AI — `/ai`
```
POST   /api/v1/ai/materials              Upload học liệu (RAG) → xử lý nền
GET    /api/v1/ai/materials              Danh sách học liệu
POST   /api/v1/ai/generate-questions     Sinh đề từ học liệu/chủ đề (async → jobId)
GET    /api/v1/ai/jobs/{jobId}           Trạng thái/kết quả job
POST   /api/v1/ai/grade                  Chấm câu tự luận
POST   /api/v1/ai/chat                   Trợ lý RAG (SSE stream)
GET    /api/v1/ai/chat/sessions          Danh sách phiên chat
```

**Ví dụ — sinh đề:**
```json
POST /api/v1/ai/generate-questions
{
  "materialId": "uuid-hoặc-null",
  "topic": "Lịch sử Việt Nam thời Lý",
  "count": 5,
  "types": ["single_choice", "true_false"],
  "difficulty": "medium",
  "language": "vi"
}
→ 202 Accepted { "jobId": "..." }
```

**Schema câu hỏi AI trả về (đã validate):**
```json
{
  "questions": [
    {
      "type": "single_choice",
      "question": "Thủ đô nước ta thời Lý là?",
      "options": ["Thăng Long", "Hoa Lư", "Phú Xuân", "Cổ Loa"],
      "correctAnswer": "Thăng Long",
      "explanation": "Năm 1010 Lý Công Uẩn dời đô về Thăng Long.",
      "difficulty": "medium",
      "topic": "Lịch sử"
    }
  ]
}
```

## 7. Gợi ý cá nhân hóa (Neo4j) — `/recommendations`
```
GET    /api/v1/recommendations           Gợi ý quiz cá nhân hóa
GET    /api/v1/recommendations/path      Lộ trình học tập đề xuất
```

## 7b. Flashcard & SRS — `/decks`, `/flashcards`
```
GET/POST/PUT/DELETE /api/v1/decks         Quản lý bộ thẻ
GET/POST/PUT/DELETE /api/v1/flashcards     Quản lý thẻ
POST   /api/v1/ai/generate-flashcards      Sinh thẻ từ học liệu/chủ đề (async → jobId)
GET    /api/v1/flashcards/due               Thẻ đến hạn ôn hôm nay
POST   /api/v1/flashcards/{id}/review       Gửi kết quả ôn { quality } → cập nhật SRS
GET    /api/v1/flashcards/stats             Thống kê ôn tập
```

## 7c. Chống gian lận — `/attempts/{id}/proctoring-events`, `/integrity`
```
POST   /api/v1/attempts/{id}/proctoring-events   Gửi sự kiện hành vi (batch)
GET    /api/v1/attempts/{id}/integrity           Báo cáo tính toàn vẹn (Creator/Admin)
GET    /api/v1/admin/integrity/flagged           Danh sách bài thi bị gắn cờ
PUT    /api/v1/admin/integrity/{id}/review       Đánh dấu hợp lệ/không hợp lệ
```
Real-time: sự kiện có thể gửi qua WebSocket `/app/room/{code}/proctoring`.

## 7d. Gamification — `/gamification`
```
GET    /api/v1/gamification/me            XP, level, streak, huy hiệu
GET    /api/v1/gamification/badges        Danh sách huy hiệu (đã/chưa mở khóa)
GET    /api/v1/gamification/daily         Daily challenge hôm nay + tiến độ
```

## 7e. Lớp học — `/classrooms`, `/assignments`
```
GET/POST/PUT/DELETE /api/v1/classrooms                Quản lý lớp
POST   /api/v1/classrooms/{code}/join                 Tham gia lớp
GET    /api/v1/classrooms/{id}/members                Thành viên
POST   /api/v1/classrooms/{id}/assignments            Giao bài
GET    /api/v1/classrooms/{id}/assignments            Danh sách bài giao
GET    /api/v1/assignments/{id}/results               Kết quả toàn lớp (giáo viên)
GET    /api/v1/me/assignments                          Bài được giao cho tôi
```

## 7f. Bảng xếp hạng theo mùa — `/leaderboard/season`
```
GET    /api/v1/leaderboard/season/current       BXH mùa hiện tại (scope: global/class/friends)
GET    /api/v1/leaderboard/season/current/me     Thứ hạng của tôi
GET    /api/v1/leaderboard/season/history        Lịch sử các mùa
```

## 7g. Thông báo — `/notifications`
```
GET    /api/v1/notifications              Danh sách thông báo
PUT    /api/v1/notifications/{id}/read     Đánh dấu đã đọc
PUT    /api/v1/notifications/read-all      Đánh dấu tất cả đã đọc
GET/PUT /api/v1/notifications/settings     Cài đặt thông báo
```
WebSocket: subscribe `/user/queue/notifications` (real-time in-app).

## 8. Thống kê — `/analytics`
```
GET    /api/v1/analytics/me              Tiến độ cá nhân (điểm mạnh/yếu theo chủ đề)
GET    /api/v1/analytics/quizzes/{id}    Thống kê 1 quiz (Creator)
```

## 9. Admin — `/admin`
```
GET    /api/v1/admin/users               Quản lý user
PUT    /api/v1/admin/users/{id}/role     Đổi vai trò
GET    /api/v1/admin/ai/logs             Log & chi phí AI
PUT    /api/v1/admin/ai/config           Cấu hình provider/fallback
```

## 10. Quy ước chung

- **Mã trạng thái:** `200` OK, `201` Created, `202` Accepted (job nền), `400/401/403/404/409/422`, `429` (rate limit), `5xx`.
- **Response lỗi chuẩn:**
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "...", "traceId": "..." }
```
- **Phân trang:** `?page=0&size=20&sort=createdAt,desc`.
- Mọi endpoint (trừ `/auth/*` và quiz công khai) yêu cầu `Authorization: Bearer <token>`.
