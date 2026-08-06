# Đặc tả API

REST, prefix `/api/v1`, trả JSON, xác thực Bearer JWT. Tài liệu tự sinh bằng **Swagger/OpenAPI** (`/swagger-ui.html`). Real-time dùng **WebSocket (STOMP)**.

## 1. Xác thực — `/auth`
```
POST   /api/v1/auth/register        Đăng ký → 201 + access & refresh token          ✅
POST   /api/v1/auth/login           Đăng nhập → access + refresh token              ✅
POST   /api/v1/auth/refresh         Làm mới token (rotation: token cũ bị thu hồi)   ✅
POST   /api/v1/auth/logout          Đăng xuất → 204, thu hồi refresh token          ✅
POST   /api/v1/auth/logout-all       Đăng xuất mọi thiết bị (mất máy)                ✅
POST   /api/v1/auth/change-password Đổi mật khẩu (cần đăng nhập) → 204              ✅
POST   /api/v1/auth/forgot-password Gửi mã OTP đặt lại mật khẩu → 204               ✅
POST   /api/v1/auth/reset-password  Đặt lại mật khẩu bằng mã OTP → 204              ✅
```

**Quên mật khẩu (FR-4).** `forgot-password` **luôn trả 204** dù email có tài khoản hay không — báo
"email không tồn tại" là mở đường cho việc dò danh sách người dùng. Mã 6 chữ số, sống 10 phút, dùng
một lần; sai quá 5 lần thì mã bị huỷ; xin mã lại trong vòng 60 giây trả **429**. Đặt lại thành công
sẽ **thu hồi phiên trên mọi thiết bị**.
> `register`, `login`, `refresh`, `logout` mở cho Guest; `change-password` yêu cầu Bearer token.
> Access token sống 15 phút, refresh token 14 ngày (lưu Redis key `session:{token}`).

## 2. Người dùng — `/users`
```
GET    /api/v1/users/me             Hồ sơ hiện tại                                  ✅
PUT    /api/v1/users/me             Cập nhật tên hiển thị / ảnh đại diện            ✅
GET    /api/v1/users/me/progress    Tiến độ học tập                                 ⏳ sau khi có attempt
```

## 3. Danh mục, Quiz & Câu hỏi
```
GET    /api/v1/categories               Danh mục (công khai)                            ✅

GET    /api/v1/quizzes                  Danh sách (categoryId, difficulty, q, mine)     ✅
GET    /api/v1/quizzes/{id}             Giới thiệu quiz, KHÔNG kèm câu hỏi              ✅
GET    /api/v1/quizzes/{id}/questions   Câu hỏi + đáp án đúng (chủ sở hữu/Admin)        ✅
POST   /api/v1/quizzes                  Tạo quiz (mặc định PRIVATE)                     ✅
PUT    /api/v1/quizzes/{id}             Cập nhật metadata                               ✅
PUT    /api/v1/quizzes/{id}/questions   Đặt lại danh sách & thứ tự câu hỏi              ✅
DELETE /api/v1/quizzes/{id}             Xóa quiz (câu hỏi vẫn còn trong ngân hàng)      ✅

GET    /api/v1/questions                Ngân hàng câu hỏi của tôi (type, difficulty,
                                        topic, q + phân trang)                          ✅
POST   /api/v1/questions                Tạo câu hỏi                                     ✅
GET    /api/v1/questions/{id}           Chi tiết câu hỏi                                ✅
PUT    /api/v1/questions/{id}           Sửa (thay toàn bộ lựa chọn)                     ✅
DELETE /api/v1/questions/{id}           Xóa — 409 nếu đang dùng trong quiz              ✅
```

### 3.1. Tải ảnh lên — `/files`
```
POST   /api/v1/files/images       Tải một ảnh (multipart, field `file`)           ✅
GET    /uploads/images/{ten-file} Xem ảnh — tài nguyên tĩnh, công khai            ✅
```

Trả về `{ url, fileName, size, contentType }`; lấy `url` gán vào `quizzes.thumbnailUrl`.

| Luật | Chi tiết |
|---|---|
| Quyền tải lên | **CREATOR/ADMIN**. Learner → 403, Guest → 401 |
| Định dạng | JPG, PNG, GIF, WebP — nhận dạng bằng **chữ ký byte**, không tin `Content-Type` client khai |
| Dung lượng | tối đa **2MB** cho ảnh (giới hạn multipart chung 25MB dành cho học liệu RAG) |
| Tên file | do server sinh từ UUID; **tên client gửi lên bị bỏ hoàn toàn** |
| Xem ảnh | công khai, không cần token — ảnh bìa quiz phải hiện được với Guest |

`thumbnailUrl` của quiz chỉ nhận đường dẫn nội bộ bắt đầu bằng `/uploads/` và không chứa `..`;
URL bên ngoài trả **400** (tránh link chết và pixel theo dõi nhúng qua ảnh bên thứ ba).

**Quyền:** `GET /categories`, `GET /quizzes`, `GET /quizzes/{id}` mở cho Guest (quiz PRIVATE của người khác trả **404**). Còn lại yêu cầu vai trò **CREATOR/ADMIN** và **quyền sở hữu** (sửa/xóa của người khác → **403**). `GET /quizzes?mine=true` yêu cầu đăng nhập.

**Luật theo loại câu hỏi** (validate ở service, trả 400 nếu vi phạm):

| Loại | Ràng buộc |
|---|---|
| `SINGLE_CHOICE` | ≥ 2 lựa chọn, đúng 1 đáp án đúng |
| `MULTIPLE_CHOICE` | ≥ 3 lựa chọn, ≥ 2 đáp án đúng, còn ≥ 1 lựa chọn sai |
| `TRUE_FALSE` | đúng 2 lựa chọn, 1 đáp án đúng |
| `FILL_BLANK` | ≥ 1 đáp án; hệ thống tự đánh dấu tất cả là đúng (các cách viết được chấp nhận) |
| `SHORT_ANSWER` | đúng 1 đáp án mẫu, dùng làm căn cứ khi AI chấm |

## 4. Chơi quiz (đơn) — `/attempts`
```
POST   /api/v1/quizzes/{id}/attempts    Bắt đầu làm bài (body { mode })            ✅
GET    /api/v1/attempts/{id}            Bài làm: chưa nộp = đề, đã nộp = kết quả   ✅
POST   /api/v1/attempts/{id}/answers    Trả lời một câu                            ✅
POST   /api/v1/attempts/{id}/submit     Nộp bài & chấm                             ✅
GET    /api/v1/attempts                 Lịch sử làm bài của tôi (quizId + trang)   ✅
GET    /api/v1/quizzes/{id}/leaderboard Xếp hạng người học, tối đa 50 dòng         ✅
```

**Quyền:** toàn bộ mục này yêu cầu **đăng nhập** — Guest không làm bài, không xem bảng xếp hạng.
Quiz PRIVATE của người khác trả **404**. Bài làm là dữ liệu riêng: người khác *và cả chủ quiz/Admin*
truy cập đều nhận **404** (thống kê cho Creator nằm ở features/09).
Không giới hạn theo vai trò: **chủ quiz làm được bài trên quiz của mình** (kể cả PRIVATE) để tự kiểm đề,
và cũng bị giấu đáp án như mọi người — nhưng bài của họ **không lên bảng xếp hạng** vì đã biết trước
đáp án (điểm vẫn lưu trong lịch sử cá nhân).

**Chế độ (`mode`)**

| Giá trị | Hành vi |
|---|---|
| `EXAM` (mặc định) | Chỉ lưu câu trả lời, chấm một lần khi nộp. Sửa đáp án thoải mái trước khi nộp. |
| `PRACTICE` | Chấm ngay từng câu, trả về đáp án đúng + giải thích. Câu đã chấm không trả lời lại (409). |

**Luật không lộ đáp án.** Khi `attempt.status = IN_PROGRESS`, các trường `correctOptionIds`,
`explanation`, `correct`, `score` của mọi câu đều là `null`; câu `FILL_BLANK`/`SHORT_ANSWER` còn bị
trả `options: []` vì đáp án của chúng nằm trong `options`. Sau khi nộp mới hiện đầy đủ.

**Chấm điểm** (chi tiết ở [features/03-gameplay.md](features/03-gameplay.md))

| Loại | Cách chấm |
|---|---|
| `SINGLE_CHOICE`, `TRUE_FALSE` | Chọn đúng 1 lựa chọn và phải là lựa chọn đúng |
| `MULTIPLE_CHOICE` | Trọn gói: tập lựa chọn phải trùng khít tập đáp án đúng |
| `FILL_BLANK` | Khớp một đáp án được chấp nhận, bỏ qua hoa/thường và khoảng trắng thừa (**giữ dấu tiếng Việt**) |
| `SHORT_ANSWER` | Máy không chấm → `gradedBy = PENDING_AI`, tạm 0 điểm, chờ features/06 |

**Mã lỗi riêng:** `400` quiz chưa có câu hỏi / id lựa chọn không thuộc câu hỏi / câu một đáp án mà
chọn nhiều · `404` quiz hoặc bài làm không tồn tại (hoặc không phải của mình), câu hỏi ngoài đề ·
`409` bài đã kết thúc, hết giờ, hoặc trả lời lại câu đã chấm ở chế độ luyện tập.

**Ghi chú thiết kế**
- Gọi lại `POST /quizzes/{id}/attempts` khi đang có bài dở trên quiz đó → **trả lại bài cũ để làm tiếp**
  (mỗi người tối đa một bài `IN_PROGRESS` trên một quiz).
- `POST /attempts/{id}/submit` **idempotent**: gọi lại trên bài đã nộp trả đúng kết quả cũ.
- Hết giờ (`expires_at`) thì lần gọi `GET`/`submit` kế tiếp tự chốt bài sang `EXPIRED` và vẫn chấm
  phần đã làm; `POST /answers` khi đó trả 409.

## 5. Phòng đấu real-time — REST + WebSocket

### 5.1. REST (quản lý phòng)
```
POST   /api/v1/rooms                      Mở phòng { quizId, secondsPerQuestion?, allowGuests }  ✅
POST   /api/v1/rooms/{pin}/join           Thành viên vào phòng bằng mã PIN                       ✅
POST   /api/v1/rooms/{pin}/join-as-guest  Khách vào phòng { displayName, avatar? }               ✅
GET    /api/v1/rooms/{pin}                Ảnh chụp phòng (dùng để đồng bộ lại)                   ✅
GET    /api/v1/rooms/avatars              Bộ nhân vật để chọn                                    ✅
DELETE /api/v1/rooms/{pin}/players/me     Rời phòng                                              ✅
```

**Mã phòng là PIN 6 chữ số** (`482913`), gõ được trên bàn phím số của điện thoại. Frontend vẽ **mã QR**
trỏ tới `/join/{pin}` bằng `qrcode.react`; quét xong là vào thẳng phòng chờ.

**Quyền**

| Endpoint | Chưa đăng nhập |
|---|---|
| `POST /rooms` | ❌ 401 — chỉ thành viên mở được phòng |
| `GET /rooms/{pin}`, `GET /rooms/avatars` | ✅ mở — **mã PIN chính là thứ chặn cửa** |
| `POST /rooms/{pin}/join-as-guest` | ✅ mở, nhưng **403** nếu host không bật `allowGuests` |
| `POST /rooms/{pin}/join`, `DELETE …/players/me` | ❌ 401 |

Quiz PRIVATE của người khác trả **404**; quiz chưa có câu hỏi trả **400**; ván đã kết thúc thì vào
lại trả **409**. Host tự động là người chơi đầu tiên nên không phải join thêm lần nữa.

**Khách vãng lai.** `join-as-guest` trả `{ guestKey, playerId, room }`. Client giữ `guestKey` trong
`sessionStorage` và gửi kèm header `X-Guest-Key` khi nối WebSocket. Khoá này **chỉ dùng được cho đúng
phòng đó**, tự hết hạn sau 6 giờ, và không mở được bất kỳ API nào khác.

### 5.2. WebSocket (STOMP) — endpoint `/ws` ✅
```
SUBSCRIBE /topic/room/{code}         Sự kiện phát cho cả phòng
SUBSCRIBE /user/queue/room/{code}    Sự kiện gửi riêng cho mình (kết quả câu vừa trả lời)
SUBSCRIBE /user/queue/errors         Lỗi nghiệp vụ của riêng mình (hết giờ, không phải host…)

SEND      /app/room/{code}/start     Host bắt đầu ván
SEND      /app/room/{code}/answer    Gửi đáp án { questionId, optionIds?, text? }
SEND      /app/room/{code}/next      Host chuyển câu / kết thúc ván
SEND      /app/room/{code}/ready     Bật/tắt Sẵn sàng { ready }
SEND      /app/room/{code}/avatar    Đổi nhân vật { avatar }
```

**Xác thực:** danh tính đi trong header của **frame CONNECT**, không phải query string. Chấp nhận
hai loại — `Authorization: Bearer <JWT>` cho thành viên, `X-Guest-Key: <khoá>` cho khách. Bắt tay
HTTP `/ws` để công khai vì trình duyệt không gắn được header vào yêu cầu nâng cấp WebSocket; chặn
nằm ở frame CONNECT (`StompAuthChannelInterceptor`).

**Sự kiện server phát về** (`{ type, at, data }`):

| `type` | Phạm vi | Nội dung |
|---|---|---|
| `PLAYER_JOINED` / `PLAYER_LEFT` | cả phòng | `{ playerId, players[], readyCount }` |
| `PLAYER_READY` | cả phòng | như trên, sau khi ai đó bật/tắt Sẵn sàng |
| `PLAYER_AVATAR_CHANGED` | cả phòng | như trên, sau khi ai đó đổi nhân vật |
| `GAME_STARTED` | cả phòng | — |
| `QUESTION` | cả phòng | câu hỏi + `deadlineAtMillis`. **Không kèm đáp án đúng** |
| `PLAYER_ANSWERED` | cả phòng | chỉ `{ answeredCount, totalPlayers }` |
| `ANSWER_RESULT` | **riêng người trả lời** | `{ correct, points, totalScore, elapsedMillis }` |
| `QUESTION_CLOSED` | cả phòng | đáp án đúng + giải thích + bảng xếp hạng |
| `LEADERBOARD` | cả phòng | bảng xếp hạng |
| `GAME_FINISHED` | cả phòng | bảng xếp hạng chung cuộc |

> **Vì sao `ANSWER_RESULT` gửi riêng:** phát cho cả phòng thì người chưa trả lời chỉ cần nhìn ai vừa
> được cộng điểm là đoán ra đáp án. Cả phòng chỉ biết *số người* đã xong (`PLAYER_ANSWERED`); đáp án
> đúng đợi tới `QUESTION_CLOSED` mới công bố.

**Tính điểm theo tốc độ (FR-22):** `điểm = points × (500 + 500 × tỉ lệ thời gian còn lại)`.
Đúng tức thì được `points × 1000`, đúng sát giờ chót vẫn được `points × 500`, sai được 0 — nên
**đúng chậm luôn hơn sai nhanh**. Thời gian do **server đo** từ mốc phát câu hỏi; client không gửi
thời gian lên được.

**Chuyển câu:** host bấm `/next`, hoặc tự động khi *mọi người đã trả lời*. Không có job nền đếm giờ —
client đếm ngược tới `deadlineAtMillis`, còn server thì từ chối mọi đáp án gửi sau hạn (**409**).

## 6. Tính năng AI — `/ai`
```
GET    /api/v1/ai/status                 Đã cấu hình provider nào chưa               ✅
GET    /api/v1/ai/materials              Học liệu của tôi                            ✅
GET    /api/v1/ai/materials/{id}         Chi tiết (hỏi lại trạng thái xử lý)         ✅
POST   /api/v1/ai/materials              Nạp học liệu bằng văn bản dán tay → 202     ✅
POST   /api/v1/ai/materials/upload       Nạp từ file PDF/DOCX/TXT (multipart) → 202  ✅
DELETE /api/v1/ai/materials/{id}         Xoá học liệu và toàn bộ vector              ✅
POST   /api/v1/ai/generate-questions     Sinh đề (async → jobId)                     ✅
GET    /api/v1/ai/jobs/{jobId}           Trạng thái/kết quả job                      ✅
POST   /api/v1/ai/jobs/{jobId}/approve   Duyệt câu hỏi đã chọn → ngân hàng câu hỏi   ✅

POST   /api/v1/ai/grade                  Chấm câu tự luận                            ⏳ features/06
POST   /api/v1/ai/chat                   Trợ lý RAG (SSE stream)                     ⏳ features/08
GET    /api/v1/ai/chat/sessions          Danh sách phiên chat                        ⏳ features/08
```

**Quyền:** toàn bộ mục này yêu cầu vai trò **CREATOR/ADMIN** (Learner → 403, Guest → 401). Đây là
công cụ soạn nội dung và mỗi lời gọi đều tốn tiền API, nên không mở cho mọi tài khoản.
Học liệu và job là **dữ liệu riêng**: của người khác trả **404**.

**Ví dụ — sinh đề:**
```json
POST /api/v1/ai/generate-questions
{
  "topic": "mã trạng thái HTTP",
  "count": 5,
  "types": ["SINGLE_CHOICE", "TRUE_FALSE"],
  "difficulty": "EASY",
  "materialId": "uuid-hoặc-bỏ-trống",
  "useMaterials": true
}
→ 202 Accepted { "id": "<jobId>", "status": "PENDING", ... }
```

Rồi hỏi lại `GET /ai/jobs/{jobId}` tới khi `status` là `SUCCEEDED` hoặc `FAILED`:
```json
{
  "status": "SUCCEEDED",
  "result": {
    "questions": [ { "type": "SINGLE_CHOICE", "content": "…", "options": [{"content":"…","correct":true}],
                     "explanation": "…", "difficulty": "EASY", "topic": "…" } ],
    "rejected": ["Câu một đáp án cần ≥2 lựa chọn và đúng 1 đáp án đúng — …"],
    "sourceExcerpts": ["đoạn học liệu đã dùng làm ngữ cảnh…"],
    "provider": "gemini", "model": "gemini-3.6-flash", "latencyMs": 4210
  }
}
```

**Human-in-the-loop.** Câu hỏi sinh ra **không** tự vào ngân hàng. Creator chọn câu nào dùng được
rồi gọi `POST /ai/jobs/{id}/approve` với `{"indexes":[0,2,3]}`; các câu đó mới được lưu, và vẫn
phải qua đúng bộ luật của `QuestionService` như câu soạn tay.

**Vòng đời học liệu:** `POST` trả **202** ngay với `status: PROCESSING`; việc cắt đoạn và sinh
embedding chạy nền. Client hỏi lại `GET /ai/materials/{id}` tới khi `READY` (hoặc `FAILED` kèm
`errorMessage`). Chưa cấu hình API key thì tài liệu chuyển `FAILED` với thông điệp hướng dẫn,
chứ không kẹt mãi ở `PROCESSING`.

**Chống ảo giác (grounding).** Khi `useMaterials = true`, prompt chỉ cấp cho mô hình các đoạn học
liệu tìm được và cấm suy diễn ngoài ngữ cảnh. `sourceExcerpts` trả về chính những đoạn đó để
Creator đối chiếu xem AI có bịa không.

**Chống prompt injection.** Nội dung học liệu do người dùng nạp nên được coi là **dữ liệu**, rào
trong khối `===== NGỮ CẢNH =====` và chỉ dẫn hệ thống nói rõ: bỏ qua mọi câu lệnh nằm bên trong đó.

**Mã lỗi riêng:** `400` số câu ngoài khoảng 1–20 / tài liệu dưới 100 ký tự / file không đọc được /
AI không tạo được câu nào hợp lệ · `404` học liệu hoặc job không phải của mình · `409` duyệt job
chưa xong · `503` chưa cấu hình API key hoặc mọi provider đều không phản hồi.

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
