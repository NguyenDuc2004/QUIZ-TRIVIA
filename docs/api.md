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
POST   /api/v1/auth/google          Đăng nhập bằng Google (ID token) → token        ✅
```

**Quên mật khẩu (FR-4).** `forgot-password` **luôn trả 204** dù email có tài khoản hay không — báo
"email không tồn tại" là mở đường cho việc dò danh sách người dùng. Mã 6 chữ số, sống 10 phút, dùng
một lần; sai quá 5 lần thì mã bị huỷ; xin mã lại trong vòng 60 giây trả **429**. Đặt lại thành công
sẽ **thu hồi phiên trên mọi thiết bị**.
**Đăng nhập Google (FR-3).** Frontend lấy **ID token** từ Google Identity Services rồi POST
`{"idToken": "..."}`; backend xác minh chữ ký, `iss` và **`aud` (Client ID của ứng dụng)** với Google
— thiếu bước kiểm `aud` thì một token Google hợp lệ cấp cho ứng dụng *khác* vẫn đăng nhập được vào
đây. Ba tình huống: đã liên kết (khớp `google_id`) → vào luôn; email đã có tài khoản mật khẩu →
**liên kết** vào đó, không tạo tài khoản thứ hai (chỉ làm được vì Google báo email đã xác minh);
hoàn toàn mới → tạo tài khoản không mật khẩu, vai trò **LEARNER** (không cho chọn vai trò qua đường
này). Tài khoản chỉ-Google gọi `change-password` sẽ nhận **400** kèm hướng dẫn dùng Quên mật khẩu để
đặt mật khẩu đầu tiên.

> `register`, `login`, `refresh`, `logout`, `google` mở cho Guest; `change-password` yêu cầu Bearer token.
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
GET    /api/v1/questions/topics         Chủ đề của tôi kèm số câu mỗi chủ đề            ✅
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

**Chủ đề câu hỏi.** `topic` là **cột chữ tự do trên `questions`**, không phải bảng riêng — không
bắt người soạn tạo chủ đề trước rồi mới viết được câu hỏi đầu tiên. `GET /questions/topics` gom lại
các giá trị đã dùng kèm số câu; giao diện dùng nó để (1) lọc ngân hàng, (2) lọc trong hộp chọn câu
lúc soạn quiz, (3) gợi ý khi gõ ô Chủ đề — thiếu (3) thì hôm nay gõ "Lịch sử Việt Nam", mai gõ
"Lịch sử VN" và thành hai chủ đề, lọc sẽ sót mà không ai biết vì sao.

Lọc `?topic=` khớp **chính xác nhưng không phân biệt hoa/thường**. Danh sách chủ đề là dữ liệu
riêng: chỉ trả chủ đề của chính người gọi.

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

POST   /api/v1/attempts/{a}/answers/{b}/explain  Nhờ AI giải thích một câu          ✅
GET    /api/v1/attempts/{id}/grading             Chủ quiz đọc phần tự luận để chấm  ✅
PATCH  /api/v1/attempts/{a}/answers/{b}/grade    Chủ quiz chấm tay, ghi đè điểm AI  ✅
```

**Quyền:** toàn bộ mục này yêu cầu **đăng nhập** — Guest không làm bài, không xem bảng xếp hạng.
Quiz PRIVATE của người khác trả **404**. Bài làm là dữ liệu riêng: người khác *và cả chủ quiz/Admin*
truy cập đều nhận **404**. Ngoại lệ duy nhất là hai endpoint chấm tay
(`GET .../grading` + `PATCH .../grade`) — xem bên dưới; thống kê tổng hợp cho Creator ở §8.
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
| `SHORT_ANSWER` | Máy không chấm → `gradedBy = PENDING_AI`, **AI chấm nền** sau khi nộp (features/06) |

**Chấm tự luận bằng AI (FR-30).** `submit` trả kết quả **ngay** với điểm phần trắc nghiệm; câu tự
luận để `PENDING_AI` và được chấm ở luồng nền — chấm đồng bộ thì người học phải chờ hàng chục giây
và request dễ timeout. Response mang thêm `gradingPending` = số câu còn đang chấm; frontend hỏi lại
mỗi 3 giây tới khi về 0, rồi dừng. Chấm xong, **tổng điểm được cộng lại**: điểm ngay sau khi nộp chỉ
là điểm tạm.

| `gradedBy` | Ý nghĩa |
|---|---|
| `AUTO` | Máy chấm theo đáp án cố định (kể cả câu tự luận **bỏ trống** → 0 điểm, không tốn lượt gọi AI) |
| `PENDING_AI` | Đã nộp, AI đang chấm |
| `AI` | AI đã chấm, có `aiFeedback` và `aiSuggestions` |
| `AI_FAILED` | Gọi mô hình hỏng (hết hạn mức, JSON sai). **Trạng thái dừng** — không có nó thì người học thấy "đang chấm" vĩnh viễn |
| `HUMAN` | Chủ quiz chấm tay, đè lên điểm AI |

Điểm do mô hình trả luôn bị **ép về [0, maxScore]** — hàng rào cuối nếu prompt injection lọt qua và
mô hình bị dụ cho điểm tối đa. Chấm tay cũng chịu cùng trần đó. Kết quả AI về **sau** khi Creator đã
chấm tay thì bị bỏ qua: người luôn thắng máy.

**Chấm tay là ngoại lệ có chủ đích** của luật "bài của ai người ấy xem" — chấm mà không đọc được bài
thì chấm bằng gì. Hai endpoint, phạm vi bó đúng bằng mục đích:

| Endpoint | Cho ai | Phạm vi |
|---|---|---|
| `GET /attempts/{id}/grading` | chủ quiz (hoặc Admin) | **chỉ câu `SHORT_ANSWER`** của bài đó, kèm rubric, đáp án mẫu và những gì AI đã nói. Câu trắc nghiệm không có trong response — máy chấm rồi, không có gì để xem lại |
| `PATCH .../answers/{b}/grade` | chủ quiz (hoặc Admin) | sửa điểm + nhận xét của **một** câu |

Cả hai trả **404** cho người không sở hữu quiz — kể cả chính người học (họ đã có
`GET /attempts/{id}` với đúng thứ họ được thấy). Không có endpoint nào liệt kê bài làm theo người
học; danh sách duy nhất là theo **quiz mình sở hữu** (§8).

`GET .../grading` trả 409 khi bài **chưa nộp**: chưa nộp thì chưa có gì để chấm.

**`POST .../explain`** chỉ chạy trên bài **đã nộp** của chính mình — giải thích trước khi nộp là
đường vòng để lấy đáp án. Với câu có đáp án cố định, AI **chỉ giải thích chứ không chấm**: chấm đã
xong bằng logic, gọi mô hình thêm chỉ tốn tiền mà không chính xác hơn.

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

(Chấm tự luận & giải thích nằm ở mục 4 vì gắn với một bài làm cụ thể, không phải công cụ soạn nội dung)

PATCH  /api/v1/ai/materials/{id}/shared  Bật/tắt chia sẻ học liệu cho người học       ✅
```

**Quyền:** các endpoint trên yêu cầu vai trò **CREATOR/ADMIN** (Learner → 403, Guest → 401). Đây là
công cụ soạn nội dung và mỗi lời gọi đều tốn tiền API, nên không mở cho mọi tài khoản.
Học liệu và job là **dữ liệu riêng**: của người khác trả **404**.

### 6b. Trợ lý học tập (RAG chatbot) — `/ai/chat`
```
POST   /api/v1/ai/chat                   Hỏi trợ lý, trả lời theo luồng SSE          ✅
GET    /api/v1/ai/chat/materials         Học liệu tôi được phép hỏi (chỉ metadata)   ✅
GET    /api/v1/ai/chat/sessions          Phiên hội thoại của tôi                     ✅
GET    /api/v1/ai/chat/sessions/{id}     Toàn bộ tin nhắn của một phiên              ✅
DELETE /api/v1/ai/chat/sessions/{id}     Xoá phiên và toàn bộ tin nhắn               ✅
```

`GET /ai/chat/materials` trả tài liệu của chính người gọi **cộng** tài liệu người khác đã bật `shared`,
chỉ những tài liệu ở trạng thái `READY`. Đây là **cùng phạm vi** mà truy vấn vector của trợ lý dùng —
lệch nhau thì giao diện liệt kê một danh sách khác với thứ trợ lý thật sự đọc được.

Response chỉ mang metadata (`id`, `title`, `topic`, `sourceType`, `chunkCount`, `mine`), **không có
`content` và không có đoạn nào**: người học được *hỏi trên* tài liệu, không được *đọc toàn văn* tài liệu
của người khác. Cờ `mine` để giao diện phân biệt tài liệu của mình với tài liệu đọc ké.

Endpoint này là điều kiện để dùng được `materialId` trong `POST /ai/chat`: có danh sách thì giao diện
mới cho người dùng chọn giới hạn câu hỏi trong một tài liệu.

**Quyền khác hẳn phần trên: mọi tài khoản đã đăng nhập đều dùng được** — người học chính là đối tượng
trợ lý phục vụ. Vì vậy nó nằm ở `ChatController` riêng chứ không nhồi vào `AiController` (lớp đó gắn
`@PreAuthorize` cấp lớp cho CREATOR/ADMIN; đục một lỗ ngoại lệ trong đó là cách chắc chắn để sau này
có người mở rộng quyền quá tay). Guest vẫn bị chặn: hội thoại có ngữ cảnh cần danh tính để lưu phiên,
và mỗi lượt hỏi tiêu một lượt hạn mức AI.

**Phạm vi học liệu.** Trợ lý truy xuất trong **học liệu của chính người hỏi + học liệu người khác đã
bật `shared`**. Người học không sở hữu học liệu nào, nên nếu chỉ tìm trong tài liệu của họ thì mọi câu
hỏi đều truy xuất được con số không — và mô hình sẽ trả lời bằng kiến thức nền, tức là **bịa**, đúng
thứ RAG sinh ra để chống. Tài liệu **chưa** bật `shared` vẫn tuyệt đối riêng tư.

**Ba loại sự kiện SSE**, mỗi loại một tên riêng:

| `event` | `data` | Ghi chú |
|---|---|---|
| `meta` | `{ sessionId, sources[] }` | **Một lần, trước mọi chữ.** Mang id phiên vừa mở (lượt hỏi đầu không cần gọi thêm API) và danh sách học liệu sẽ dựa vào |
| `token` | `{ "t": "…" }` | Một mảnh văn bản |
| `error` | `{ message }` | Hết hạn mức / mô hình không phản hồi |

Mảnh chữ **bọc trong JSON** chứ không gửi thô. Chuẩn SSE quy định client bỏ *một* khoảng trắng đứng
ngay sau `data:`, mà mảnh của Gemini rất thường bắt đầu bằng khoảng trắng — gửi thô thì
`"Vòng lặp" + " for" + " dùng"` hiện ra thành `"Vòng lặpfordùng"`. Đã đo thật, không phải phòng xa.

Lỗi **giữa luồng** phải là sự kiện `error`, không phải mã HTTP: header đã gửi từ lúc mở luồng nên
không đổi status được nữa. Ngược lại, lỗi ở **bước chuẩn bị** (phiên không tồn tại, câu hỏi rỗng)
vẫn trả mã HTTP thường, vì bước đó chạy đồng bộ trước khi mở luồng.

**Mã lỗi riêng:** `400` câu hỏi rỗng hoặc quá 2000 ký tự · `404` phiên không phải của mình ·
`409` bật chia sẻ cho tài liệu chưa xử lý xong.

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
GET    /api/v1/recommendations         Quiz gợi ý cho tôi (limit, mặc định 8)          ✅
GET    /api/v1/recommendations/path    Lộ trình học: chủ đề xếp theo mức độ yếu        ✅
POST   /api/v1/recommendations/rebuild Dựng lại đồ thị của tôi từ lịch sử làm bài      ✅
```

**Quyền:** cả ba yêu cầu **đăng nhập**. Không có khái niệm "gợi ý cho khách" — gợi ý dựa trên lịch
sử của chính người gọi, mà khách thì không có lịch sử.

**Ba nguồn gợi ý, trộn theo thứ tự ưu tiên.**

| `source` | Khi nào có tác dụng |
|---|---|
| `WEAK_TOPIC` | Đang yếu một chủ đề và còn quiz chưa làm thuộc chủ đề đó |
| `SIMILAR_LEARNERS` | Có người khác cùng làm những quiz mình đã làm |
| `NEW_TOPIC` | Còn chủ đề chưa từng luyện — nguồn đáy, luôn có tác dụng khi kho còn quiz |

Hai nguồn đầu đều **cần dữ liệu hành vi để chạy**, nên chúng cạn cùng nhau đúng lúc người dùng cần
nhất: lúc mới đăng ký, và lúc đã làm hết quiz thuộc chủ đề mình yếu. Nguồn thứ ba là đáy, và cũng là
lời giải cho *cold start*.

Mỗi mục kèm **`reason` viết sẵn bằng tiếng Việt** — gợi ý không nói vì sao thì người dùng không có
căn cứ để tin hay bỏ qua.

**Thế nào là "yếu":** tỷ lệ đúng dưới **60%** *và* đã trả lời ít nhất **3 câu** thuộc chủ đề đó. Điều
kiện thứ hai quan trọng không kém: sai 1 trên 1 câu là 0% nhưng không nói lên điều gì, gắn nhãn
"yếu" từ đó là võ đoán và người học đọc xong sẽ mất tin vào cả những nhãn đúng.

**Không có `rating`.** Hệ thống chưa có tính năng đánh giá quiz, nên gợi ý sắp theo *số câu khớp chủ
đề đang yếu* rồi tới *số lượt làm thật* — không sắp theo một con số không tồn tại.

**Danh sách rỗng phải nói được vì sao rỗng.** Cả hai endpoint trả `{ items | topics, note }` chứ
không trả thẳng một mảng:

| Endpoint | `note` khi rỗng |
|---|---|
| `/path` | chưa làm bài nào · chưa đủ 3 câu một chủ đề · không yếu chủ đề nào |
| `/recommendations` | kho chưa có quiz công khai có câu hỏi · **đã làm hết quiz đang có** · không truy vấn được đồ thị |

`note` là `null` khi danh sách **có** dữ liệu — lúc đó không có gì cần giải thích, thêm một dòng chữ
chỉ làm loãng.

Ba tình huống rỗng của `/recommendations` dẫn tới **ba việc người dùng nên làm khác nhau**, nên
không gộp thành một câu chung. Frontend không tự chế câu chữ vì nó không biết đang là tình huống
nào; riêng lỗi mạng/401 thì không có `note` và giao diện ẩn hẳn khu đó.

> Giao diện **ẩn hẳn khu Gợi ý khi rỗng** là thiết kế ban đầu, và nó sai: người dùng biết tính năng
> tồn tại, không thấy nó, và kết luận là hỏng. Đã hiểu nhầm như vậy trên thực tế trước khi có `note`.

**Neo4j hỏng thì trả rỗng, không trả 500** — gợi ý là tính năng phụ trợ trên trang chủ, không đáng
kéo cả trang sập.


## 7b. Flashcard & SRS — `/decks`, `/flashcards`
```
GET    /api/v1/decks                                Bộ thẻ của tôi (?keyword=), kèm số thẻ đến hạn   ✅
POST   /api/v1/decks                                Tạo bộ thẻ                                       ✅
PUT    /api/v1/decks/{id}                           Sửa bộ thẻ                                       ✅
DELETE /api/v1/decks/{id}                           Xoá bộ thẻ (cascade thẻ + tiến độ ôn)            ✅
GET    /api/v1/decks/{id}/cards                     Thẻ trong bộ, kèm trạng thái ôn của tôi          ✅
POST   /api/v1/decks/{id}/cards                     Thêm thẻ                                         ✅
POST   /api/v1/decks/{id}/cards/from-wrong-answers  Sinh thẻ từ câu tôi trả lời sai (KHÔNG gọi AI)   ✅
PUT    /api/v1/flashcards/{id}                      Sửa thẻ (không đặt lại lịch ôn)                  ✅
DELETE /api/v1/flashcards/{id}                      Xoá thẻ                                          ✅
GET    /api/v1/flashcards/due                       Thẻ đến hạn (?deckId=), quá hạn lâu nhất trước   ✅
POST   /api/v1/flashcards/{id}/review               Gửi mức nhớ (?quality=) → lịch kế tiếp           ✅
GET    /api/v1/flashcards/stats                     Thống kê + dự báo 7 ngày                         ✅
POST   /api/v1/decks/{id}/cards/generate            Sinh thẻ từ học liệu qua RAG (async → jobId)     ✅
GET    /api/v1/flashcards/jobs/{id}                 Trạng thái + kết quả job sinh thẻ                ✅
POST   /api/v1/flashcards/jobs/{id}/approve         Duyệt thẻ đã chọn, lưu vào bộ từ job              ✅
```

**Không có tham số `userId` ở bất kỳ đường dẫn nào.** Mọi endpoint làm việc trên dữ liệu của chính người
gọi, lấy từ token. Nhận id người dùng từ client là mở đường đọc bộ thẻ của người khác chỉ bằng cách đổi một
tham số.

**Bộ thẻ của người khác trả `404`, không phải `403`.** Trả 403 là xác nhận bộ thẻ đó tồn tại — tiết lộ
thông tin cho người không có quyền biết. Cùng cách `/quizzes` đang làm.

`GET /flashcards/due` lọc `due_date <= hôm nay` chứ **không** phải `= hôm nay`: thẻ quá hạn từ những ngày
người học không mở ứng dụng vẫn phải hiện ra. Lọc bằng `=` thì nghỉ một ngày là mất luôn thẻ của ngày đó,
đúng lúc người ta cần ôn nhất.

`POST /flashcards/{id}/review` **không kiểm thẻ có đang đến hạn hay không**. Ôn sớm là việc hợp lệ và có
ích; thuật toán tính từ trạng thái hiện tại chứ không từ việc hôm nay là ngày nào.

`from-wrong-answers` **không gọi mô hình AI**: nội dung câu hỏi, đáp án đúng và phần giải thích đã có trong
cơ sở dữ liệu, ghép thành hai mặt thẻ là việc của một câu SQL. Gọi mô hình ở đây chỉ tốn hạn mức để viết
lại thứ đã có, và thêm một đường cho nó bịa nội dung khác với đáp án thật. Trả về `{soDaTao, soBoQua}` —
báo cả số bỏ qua để người dùng hiểu vì sao bấm lần hai ra 0 thẻ mới.

**Ba endpoint sinh thẻ nằm ở `FlashcardController`, KHÔNG ở `AiController`** — dù chúng gọi mô hình.
`AiController` gắn `@PreAuthorize("hasAnyRole('CREATOR','ADMIN')")` ở cấp lớp, mà người học chính là đối
tượng của cả tính năng thẻ ghi nhớ. Đây là cùng lý do `ChatController` đã tách ra trước đó. Kể cả endpoint
tra trạng thái job cũng phải ở đây: để nó bên `AiController` thì người học gửi được yêu cầu nhưng không lấy
được kết quả — tệ hơn là không cho gửi.

`materialId` **bắt buộc**. Khác sinh đề — nơi bỏ chọn học liệu thì sinh theo kiến thức chung — sinh thẻ luôn
cần nguồn: thẻ được ôn đi ôn lại hàng chục lần theo lịch SRS nên một thẻ sai sẽ được *học thuộc*, và người
duyệt cần tài liệu để đối chiếu. Danh sách học liệu chọn được lấy từ `GET /ai/chat/materials` (của mình +
đã chia sẻ), dùng lại của trợ lý học tập vì cùng một câu hỏi "tài liệu nào tôi được dùng".

**Thẻ không tự vào bộ khi job xong.** Phải qua `POST /flashcards/jobs/{id}/approve` với danh sách chỉ số đã
chọn. Bộ thẻ đích lấy từ **yêu cầu đã lưu trong job**, không nhận lại từ client lúc duyệt — nhận lại là mở
đường ghi thẻ vào một bộ khác với bộ đã được kiểm quyền lúc gửi. Quyền trên bộ thẻ kiểm **trước** khi gọi mô
hình, để không tốn tiền API cho một kết quả không lưu được.

Kết quả job kèm `rejected` (thẻ bị loại tự động, kèm lý do) và `sourceExcerpts` (đoạn học liệu đã dùng).
Cả hai đều hiện lên giao diện: cái đầu giải thích vì sao yêu cầu 15 mà nhận 11, cái sau để người duyệt đối
chiếu thẻ đáng ngờ với nguồn.

## 7c. Chống gian lận — `/attempts/{id}/proctoring-events`, `/integrity`
```
POST   /api/v1/attempts/{id}/proctoring-events   Gửi lô tín hiệu hành vi (≤ 50/lô)                 ✅
GET    /api/v1/attempts/{id}/integrity           Báo cáo tính toàn vẹn (chủ quiz hoặc Admin)       ✅
PUT    /api/v1/attempts/{id}/integrity/review    Kết luận hợp lệ / không hợp lệ                    ✅
GET    /api/v1/admin/integrity/flagged           Hàng chờ bài bị gắn cờ (Admin), `?status=`        ✅
```
- `POST proctoring-events` chỉ nhận **lượt EXAM của chính mình**; lượt PRACTICE trả `400`. Thân request là
  `{ events: [{ type, occurredAt, length?, seconds? }] }` — **không có trường nội dung**, và server dựng lại
  `detail` từ đúng hai trường số đó thay vì lưu nguyên gói tin.
- `GET integrity` trả `404` cho **người làm bài** (kể cả bài của chính họ) — xem
  [features/12](features/12-anti-cheat.md). Trả `404` chứ không `403` vì `403` đã là một xác nhận rằng lượt đó
  có báo cáo.
- `PUT review` **không** nằm dưới `/admin/`: FR-47 cho phép chủ quiz kết luận bài của quiz mình mà không cần
  quyền quản trị. Chỉ hàng chờ toàn hệ thống là việc riêng của Admin. Gửi `status: PENDING` trả `400` —
  PENDING là trạng thái ban đầu, không phải một kết luận.
- Mọi báo cáo đều kèm trường `canhBao`: tín hiệu giả mạo được, điểm rủi ro **không phải bằng chứng**.
- Hàng chờ sắp theo điểm rủi ro giảm dần, mặc định lọc `PENDING`, và **không kèm từng sự kiện** (`suKien: []`)
  — trang đó chỉ để chọn bài cần mở.

Kênh WebSocket `/app/room/{code}/proctoring` **không hiện thực**: nó chỉ cần cho FR-44 (đối chiếu đáp án trong
phòng đấu), mà FR-44 đã bỏ — lý do ở [features/12](features/12-anti-cheat.md).

## 7d. Gamification — `/gamification`
```
GET    /api/v1/gamification/me         Tổng quan: XP, cấp độ + tiến độ, chuỗi ngày, số huy hiệu   ✅
GET    /api/v1/gamification/badges     Toàn bộ huy hiệu, earnedAt = null nếu chưa mở khoá          ✅
GET    /api/v1/gamification/daily      Thử thách hôm nay + tiến độ của tôi                        ✅
```

**Chỉ có endpoint ĐỌC — đây là quyết định bảo mật, không phải thiếu sót.** XP chỉ đến từ hành động học thật,
cộng qua domain event ở backend (`AttemptSubmittedEvent`, `FlashcardReviewedEvent`). Mở một đường ghi qua API
là mở đường tự cộng điểm cho mình, và khi đó cả huy hiệu lẫn bảng xếp hạng theo mùa đều mất ý nghĩa.

**Idempotent qua bảng `xp_events`.** Mỗi lần cộng ghi một dòng với khoá tự nhiên của hành động, và ràng buộc
`UNIQUE (user_id, source_type, source_key)` là chốt cuối. Khoá của ôn thẻ gồm cả ngày (`cardId:ngày`) vì API
ôn không chặn ôn sớm — không giới hạn thì bấm một thẻ trăm lần là trăm lần XP.

`GET /badges` trả **cả huy hiệu chưa đạt** (`earnedAt: null`): danh sách chỉ có cái đã đạt thì không tạo được
động lực nào, người học không thấy còn gì để hướng tới.

`GET /me` có cờ `streakConHomNay` riêng bên cạnh `currentStreak`: chuỗi 5 ngày có thể là "đã học hôm nay" hoặc
"học đến hôm qua, hôm nay chưa" — hai trạng thái khác nhau hoàn toàn với người dùng, mà con số không nói được.

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
GET    /api/v1/analytics/me                       Tiến độ của tôi (FR-85)
GET    /api/v1/analytics/quizzes/{id}             Thống kê 1 quiz — chỉ chủ quiz (FR-86)
GET    /api/v1/analytics/quizzes/{id}/attempts    Bài làm trên quiz của tôi, kèm cờ cần chấm tay
```

Cả ba đều **yêu cầu đăng nhập**. Hai endpoint `/quizzes/{id}` trả **404** khi quiz không thuộc
người gọi — không phải 403, để không tiết lộ quiz đó có tồn tại (§10).

`/analytics/me` **không** trả điểm mạnh/yếu theo chủ đề. Phần đó ở `/recommendations/path` (§7),
tính từ đồ thị Neo4j. Tính lại cùng kết luận từ PostgreSQL sẽ cho hai API nói về một chuyện bằng hai
cách trên hai kho dữ liệu, rồi đến lúc chúng lệch nhau thì không biết tin cái nào.

`averagePercent` và `completionPercent` là `null` khi **chưa có dữ liệu** — không phải `0`. 0% nghĩa
là làm mà sai hết, khác hẳn chưa làm gì; client phải phân biệt hai trạng thái này.

`scoreDistribution` luôn có **đúng 10 phần tử** (mười khoảng 10%), kể cả khoảng chưa ai đạt. Trả
thiếu thì mỗi client tự chèn số 0 một kiểu và trục biểu đồ lệch nhau.

`hardestQuestions` đã lọc bỏ câu có **dưới 3 lượt trả lời** và câu **chưa chấm xong**
(`PENDING_AI`/`AI_FAILED`): câu sai 1/1 lượt chỉ nói lên là ít người làm, còn tính câu chờ AI là sai
thì câu tự luận nào cũng thành câu khó nhất đề.

## 9. Admin — `/admin`
```
GET    /api/v1/admin/overview                Tổng quan: KPI + dữ liệu 3 biểu đồ (?days=)     ✅
GET    /api/v1/admin/users                   Danh sách user, lọc theo từ khoá/vai trò/khoá   ✅
PUT    /api/v1/admin/users/{id}/role         Đổi vai trò (?role=)                            ✅
PUT    /api/v1/admin/users/{id}/locked       Khoá / mở khoá tài khoản (?locked=)             ✅
POST   /api/v1/admin/users/{id}/revoke       Thu hồi mọi phiên, KHÔNG khoá tài khoản         ✅
GET    /api/v1/admin/categories              Danh mục kèm số quiz đang dùng                  ✅
POST   /api/v1/admin/categories              Thêm danh mục                                   ✅
PUT    /api/v1/admin/categories/{id}         Sửa danh mục                                    ✅
DELETE /api/v1/admin/categories/{id}         Xoá danh mục (chặn nếu còn quiz dùng)           ✅
GET    /api/v1/admin/quizzes                 Quiz công khai, lọc theo từ khoá/danh mục       ✅
PUT    /api/v1/admin/quizzes/{id}/hide       Ẩn quiz vi phạm (đưa về PRIVATE)                ✅
GET    /api/v1/admin/rooms                   Phòng đấu đang chạy, kèm số người từ Redis      ✅
POST   /api/v1/admin/rooms/{code}/close      Cưỡng chế đóng phòng                            ✅
GET    /api/v1/admin/ai/usage                Tổng hợp chi phí, độ tin cậy, độ trễ (?days=)   ✅
GET    /api/v1/admin/ai/config               Trạng thái provider — KHÔNG trả giá trị khoá    ✅
```

Đóng phòng dùng `POST .../close` chứ **không phải `DELETE /rooms/{code}`** như bản thiết kế đầu: thao tác
này không xoá bản ghi phòng mà chuyển nó sang `FINISHED` và xoá trạng thái Redis. Bản ghi phải còn vì
điểm cuối ván của những người đã chơi nằm ở `game_room_players` tham chiếu tới nó. Một `DELETE` không xoá
gì là tên gọi nói sai việc nó làm.

**Ba thứ cố ý không có endpoint**, và đây là phần thiết kế chứ không phải bỏ sót:

| Không có | Vì sao |
|---|---|
| Đọc hoặc ghi **giá trị khoá API** | `security.md`: không hiển thị khoá API trong UI hay log. `GET /admin/ai/config` chỉ trả `configured: true/false` cho từng nhà cung cấp — đủ để chẩn đoán "vì sao AI không chạy" mà không phơi giá trị. Đổi khoá là việc của biến môi trường |
| Sửa **system prompt** | Prompt là nơi đặt bốn lớp chống tiêm chỉ thị khi chấm bài; mở cho giao diện là mở đường phá hàng rào đó |
| Admin **đặt lại mật khẩu** người dùng | Admin biết mật khẩu thì đăng nhập thay được, và mọi hành động sau đó không quy trách nhiệm được cho ai. Đã có OTP tự phục vụ |

**`PUT /admin/ai/quota` bị hoãn (FR-84), không phải bỏ.** Thêm một ô nhập "mỗi Creator tối đa N lượt/ngày"
thì làm được ngay, nhưng `AiOrchestrator` hiện **không đọc con số đó** và cũng chưa đếm lượt gọi theo từng
người dùng. Một ô nhập lưu được giá trị mà không chặn được gì tệ hơn là không có nó: quản trị viên tin
rằng chi phí đã được giới hạn, trong khi thực tế không. Làm đúng cần đếm lượt theo user ở Redis và chặn
trong `AiOrchestrator` — một lát cắt riêng. Trong lúc chờ, `GET /admin/ai/usage` vẫn cho thấy chi phí thật
để phát hiện lạm dụng.

`POST /admin/users/{id}/revoke` khác `PUT .../locked` ở chỗ nó **chỉ đăng xuất** người dùng khỏi mọi
thiết bị mà không chặn họ đăng nhập lại — dùng khi nghi ngờ tài khoản bị chiếm dụng, hoặc khi người dùng
báo mất máy. Khoá tài khoản là biện pháp mạnh hơn và có tính kỷ luật; hai việc không nên gộp làm một.

`PUT /admin/quizzes/{id}/hide` đưa quiz về `PRIVATE` chứ **không xoá**: quiz vẫn thuộc chủ của nó, họ sửa
lại rồi công khai lại được. Xoá nội dung người khác là biện pháp không đảo lại được, và với một quiz đã
có người làm thì kéo theo cả lượt làm bài của họ.

`DELETE /admin/categories/{id}` **trả 409** nếu còn quiz đang dùng danh mục đó, kèm số lượng — thay vì
xoá kèm hoặc để quiz mồ côi. Quản trị viên cần biết mình đang định làm gì với những quiz đó trước.

`@PreAuthorize("hasRole('ADMIN')")` đặt ở **cấp lớp** `AdminController`: mọi endpoint trong đó chỉ dành
cho ADMIN, không ngoại lệ. Gắn cấp lớp thì thêm endpoint mới cũng tự được bảo vệ.

**Không có endpoint xoá người dùng — đây là chủ ý.** Bài đã làm, quiz đã soạn, học liệu đã nạp đều là
dữ liệu người khác đang dùng hoặc đang được thống kê; xoá tài khoản kéo theo xoá hoặc làm mồ côi những
thứ đó. Biện pháp tương ứng là **khoá**: chặn đường vào, giữ nguyên dữ liệu.

Hai thao tác đều **thu hồi mọi phiên** của người bị tác động:

| Thao tác | Vì sao phải thu hồi phiên |
|---|---|
| Khoá tài khoản | Chỉ đặt cờ thì access token đang cầm vẫn dùng được tới khi hết hạn (15 phút) và refresh token vẫn gia hạn được tới 14 ngày — tức "khoá" chỉ có hiệu lực sau vài phút, đúng lúc quản trị viên tin rằng nó có hiệu lực ngay |
| Đổi vai trò | Vai trò nằm **trong** access token, nên token cũ vẫn mang vai trò cũ; không thu hồi thì người vừa bị hạ quyền còn dùng quyền cũ thêm 15 phút |

Trạng thái khoá được kiểm ở **cả** `login` và `refresh`. Chặn một lối mà bỏ lối kia thì bất kỳ đường nào
cấp lại token về sau cũng mở lại cửa cho một tài khoản đang bị khoá.

Đăng nhập vào tài khoản bị khoá trả **403** (không phải 401) kèm thông báo nói rõ *"tài khoản đã bị
khoá"*. Kiểm tra này chạy **sau** khi đã khớp mật khẩu: nói "bị khoá" cho người chưa chứng minh được họ
là chủ tài khoản chính là tiết lộ email đó tồn tại — đúng thứ mà thông báo gộp *"email hoặc mật khẩu
không đúng"* đang tránh.

Hai chốt chặn ở tầng nghiệp vụ, không tin vào việc giao diện ẩn nút: quản trị viên **không tự khoá** và
**không tự hạ vai trò** chính mình. Hệ thống chỉ có một cấp quản trị nên một lần bấm sai là mất quyền mà
không còn ai mở lại được, trừ khi sửa trực tiếp cơ sở dữ liệu.

`GET /admin/ai/usage` trả ba nhóm số liệu — chi phí (token), độ tin cậy (tỉ lệ lỗi, số lượt phải dùng
nhà cung cấp dự phòng), độ trễ (trung bình và P95) — tách theo chức năng và theo nhà cung cấp.
**Không** trả về khoá API hay nội dung prompt. Độ trễ trả `null` khi chưa có lời gọi nào để tính, không
trả 0: 0 ms là một giá trị có nghĩa, còn "chưa đo" thì không.

## 10. Quy ước chung

- **Mã trạng thái:** `200` OK, `201` Created, `202` Accepted (job nền), `400/401/403/404/409/422`, `429` (rate limit), `5xx`.
- **Response lỗi chuẩn:**
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "...", "traceId": "..." }
```
- **Phân trang:** `?page=0&size=20&sort=createdAt,desc`.
- Mọi endpoint (trừ `/auth/*` và quiz công khai) yêu cầu `Authorization: Bearer <token>`.
