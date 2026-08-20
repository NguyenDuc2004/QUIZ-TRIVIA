# 16 — Thông báo & Nhắc ôn tập (Notifications)

**Ưu tiên:** [S] Should · **Tận dụng:** SRS (features/11), scheduler, WebSocket

## Mục tiêu
Giữ chân người dùng và hỗ trợ ghi nhớ bằng hệ thống thông báo: nhắc ôn tập theo lịch **lặp lại ngắt quãng (SRS)**, nhắc hạn nộp bài, thông báo thành tích (badge/level), lời mời phòng đấu.

## Use case
- Learner nhận nhắc "Bạn có N thẻ đến hạn ôn hôm nay".
- Học sinh nhận nhắc "Bài tập X sắp đến hạn nộp".
- Learner nhận thông báo khi lên level / mở khóa huy hiệu.
- Người dùng nhận thông báo real-time (in-app) và/hoặc email.

## Yêu cầu chức năng
- **FR-65** [S] ✅ Tạo & lưu thông báo theo loại. Năm loại khai trong ràng buộc `CHECK`, nhưng chỉ ba loại **có nguồn phát**: `SRS_REMINDER`, `ACHIEVEMENT`, `SYSTEM`. `ASSIGNMENT_DUE` chờ tính năng 14, `ROOM_INVITE` chờ cơ chế mời của tính năng 04 — phòng đấu hiện vào bằng mã PIN, không có lời mời.
- **FR-66** [S] ✅ **Nhắc ôn tập theo SRS:** job 7:00 mỗi ngày, một câu `group by` lấy đúng người có thẻ đến hạn.
- **FR-67** [S] ✅ Thông báo **in-app real-time** qua `/user/queue/notifications`, đi vòng qua Redis Pub/Sub.
- **FR-68** [S] ✅ Trung tâm thông báo: chuông + chấm đỏ, danh sách phân trang, đánh dấu đã đọc / tất cả đã đọc.
- **FR-69** [C] ✅ Gửi **email** — **mặc định TẮT**, là bản sao của thông báo in-app. Xem mục bên dưới.
- **FR-70** [C] 🟡 Cài đặt: **bật/tắt từng loại đã làm**; **khung giờ nhắc (quiet hours) bỏ** — lý do bên dưới.

## Email (FR-69)

### Hoá ra hạ tầng đã có sẵn

Lý do hoãn ban đầu nói cần *"một máy chủ SMTP thật, một tài khoản gửi, và khoá nằm trong `.env`"* — và cho
rằng ba thứ đó không có trong stack. **Sai**: `spring-boot-starter-mail` cùng toàn bộ cấu hình `spring.mail`
và `app.mail.from` đã nằm trong dự án từ [features/01](01-auth.md) để gửi **OTP đặt lại mật khẩu** (FR-4).
Thứ duy nhất thiếu là nối thông báo vào đường gửi đã có.

### Mặc định TẮT — và dấu hiệu bật/tắt là TÀI KHOẢN GỬI, không phải host

Đây là chỗ dễ sai nhất: `spring.mail.host` có **giá trị mặc định** (`smtp.gmail.com`) vì nó vốn được cấu hình
sẵn cho OTP. Lấy host làm dấu hiệu thì tính năng **luôn "đang bật"** và hệ thống cố gửi thư ngay lần chạy đầu
tiên — đúng thứ mà "mặc định tắt" muốn tránh. Không có `spring.mail.username` thì không gửi được gì, dù host
trỏ đúng máy chủ Gmail.

### Email là BẢN SAO, không phải kênh thay thế

Thông báo **luôn** được ghi vào cơ sở dữ liệu và **luôn** hiện trong ứng dụng; email chỉ gửi thêm cho người
không mở ứng dụng thường xuyên. Nhờ vậy email hỏng — SMTP sập, hộp thư đầy, thư vào spam — **không làm mất
thông báo nào**.

Đó cũng là lý do `EmailSender` chạy `@Async` và **nuốt mọi lỗi**: một máy chủ SMTP chậm không được phép làm
chậm việc nộp bài hay job nhắc ôn, và một lần gửi hỏng không được phép rollback thông báo đã ghi.

### Kiểm được tới đâu, và KHÔNG kiểm được cái gì

Test dùng **máy chủ SMTP thật chạy trong bộ nhớ** (GreenMail, phạm vi test), không mock `JavaMailSender`.
Mock chỉ chứng minh code *gọi đúng hàm* — nó vẫn xanh khi thư thiếu người nhận, sai mã hoá tiếng Việt, hay
tiêu đề rỗng, đúng những thứ hỏng mà người dùng sẽ thấy.

| Chứng minh được | KHÔNG chứng minh được |
|---|---|
| Thư tới đúng địa chỉ | Thư vào **hộp thư đến** thay vì thư rác |
| Tiêu đề có tiền tố loại thông báo | Nhà cung cấp thư có chặn tên miền gửi không |
| Tiếng Việt giải mã đúng dấu (quoted-printable UTF-8) | Tỉ lệ thư tới thật |
| Nội dung rỗng thì lùi về tiêu đề, không gửi thư trắng | |

Lý do hoãn ban đầu — *"gửi thành công ở phía mình không nói được gì về việc thư có tới"* — **vẫn đúng ở cột
phải**, và nó phụ thuộc danh tiếng tên miền người gửi, nằm ngoài phạm vi đồ án. Nên phát biểu đúng là: *soạn
đúng và gửi đúng giao thức thì đã kiểm; giao được thư thì chưa*.

## Vì sao bỏ khung giờ nhắc (quiet hours)

Đây là một cột **sẽ không làm gì cả**, và dự án đã có một quyết định y hệt: ô nhập hạn mức AI ở FR-84.

Hai lý do:
1. **Job nhắc ôn chạy đúng một lần mỗi ngày, 7:00.** Một khung giờ im lặng 22:00–07:00 không chặn được gì —
   thời điểm gửi vốn đã cố định và nằm ngoài khung đó.
2. **Thông báo in-app không đánh thức ai.** Quiet hours có nghĩa với đẩy về điện thoại hoặc email, mà email đã
   hoãn. Không có kênh nào mà nó tác động tới.

Nên thay vì tạo cột `quiet_hours` để trang cài đặt có thêm một ô đẹp, phần cài đặt chỉ làm thứ có tác dụng
thật: **tắt/bật theo loại**, chặn ngay ở lúc *tạo* thông báo. Khi nào có đẩy về điện thoại thì quiet hours trở
thành việc đáng làm, và lúc đó nó cũng cần thêm **múi giờ người dùng** — thứ hệ thống hiện không lưu.

## Luồng xử lý
```
Nguồn sự kiện:
  - Scheduler hằng ngày: quét thẻ SRS đến hạn, assignment sắp hết hạn → tạo notification
  - Domain event (level up, badge, room invite) → tạo notification
Gửi:
  - Lưu notifications (PostgreSQL)
  - Push real-time qua WebSocket /user/queue/notifications (nếu online)
  - (tùy chọn) gửi email async
```

## API liên quan
```
GET    /api/v1/notifications              Danh sách thông báo (phân trang)
PUT    /api/v1/notifications/{id}/read     Đánh dấu đã đọc
PUT    /api/v1/notifications/read-all      Đánh dấu tất cả đã đọc
GET/PUT /api/v1/notifications/settings     Cài đặt loại thông báo
```
WebSocket: subscribe `/user/queue/notifications` để nhận real-time.

## Dữ liệu liên quan (bổ sung PostgreSQL) — `V18__notifications.sql`
- `notifications(id, user_id, type, title, body, data jsonb, is_read, dedupe_key, created_at)`
- `notification_settings(user_id PK, disabled_types jsonb, created_at, updated_at)`

Hai chỗ **khác đặc tả gốc**, cả hai đều có lý do:

| Khác ở đâu | Vì sao |
|---|---|
| Thêm cột **`dedupe_key`** + ràng buộc `UNIQUE (user_id, dedupe_key)` | Đặc tả gợi ý dùng khoá phân tán Redis để job không gửi trùng. Khoá phân tán chỉ chặn *hai instance cùng lúc*; ràng buộc duy nhất chặn **mọi** đường tới việc gửi trùng — deploy lại giữa trưa, gọi tay để thử, tính lại XP. Và nó không thêm một thành phần nữa có thể chết. Khoá là `srs:{ngày}`, `badge:{mã}`, `level:{cấp}` |
| **`disabled_types jsonb`** thay cho một cột boolean mỗi loại | Thêm loại thông báo mới thì không phải đụng schema, mà tính năng 14 chắc chắn sẽ thêm loại. Mặc định `[]` = bật tất cả: người chưa từng vào trang cài đặt vẫn nên nhận nhắc ôn — đó là lý do tính năng này tồn tại |

Chống trùng **không đi qua ngoại lệ**: dùng `INSERT ... ON CONFLICT DO NOTHING`. Cách hiển nhiên hơn —
`save()` rồi bắt `DataIntegrityViolationException` — không chạy được, vì `save()` của JPA chưa gửi câu lệnh
xuống CSDL nên vi phạm ràng buộc nổ lúc commit, *sau khi* đã ra khỏi khối `catch`; còn chữa bằng `saveAndFlush`
thì rơi vào bẫy thứ hai (transaction đã rollback-only nên commit vẫn vỡ với `UnexpectedRollbackException`).
Trùng khoá là **đường chạy bình thường** của một job hằng ngày, nên nó không nên đi qua cơ chế ngoại lệ.

## Loại SYSTEM không tắt được

Đây là kênh nói những việc người dùng **cần** biết: bảo trì, đổi điều khoản, sự cố dữ liệu. Cho tắt kênh đó là
để người dùng tự bỏ tai nghe rồi mình lại yên tâm là "đã thông báo". Đổi lại là một cam kết: chỉ dùng `SYSTEM`
cho việc thật, không dùng cho tiếp thị hay giới thiệu tính năng.

API nhận `SYSTEM` trong danh sách tắt thì **bỏ qua trong im lặng** chứ không trả 400: giao diện không gửi được
yêu cầu đó, nên nếu nó tới thì là client hỏng — và trả lỗi cho một việc mà kết quả cuối vẫn đúng chỉ thêm một
nhánh lỗi phải xử lý ở cả hai đầu.

## Ghi chú kỹ thuật
- **Scheduler:** `@Scheduled(cron = "0 0 7 * * *")`. `@EnableScheduling` bật ở `AsyncConfig` — thiếu annotation
  đó thì `@Scheduled` bị bỏ qua **hoàn toàn** mà không có cảnh báo nào: job không chạy và không có lỗi nào để
  lần ra. Chống gửi trùng bằng ràng buộc duy nhất, không bằng khoá phân tán (xem bảng trên).
- **7 giờ sáng, không phải nửa đêm:** nhắc ôn chỉ có nghĩa nếu tới lúc người ta có thể ôn. Nửa đêm thì đúng
  ranh giới ngày nhưng thông báo nằm đó tới sáng, và lúc đọc thì nó đã là "hôm qua". Giờ này theo múi giờ máy
  chủ — hệ thống không lưu múi giờ người dùng, và bịa một mặc định thì sai với người ở múi khác.
- **Real-time đi vòng qua Redis Pub/Sub**, không gửi thẳng vào broker: broker STOMP của Spring nằm trong bộ nhớ
  *từng* instance, nên instance A tạo thông báo mà người dùng giữ WebSocket ở instance B là gửi vào chỗ không
  có ai. Cùng đường mà phòng đấu đã dùng (`GameEventPublisher`/`GameEventRelay`), và dùng chung một
  `RedisMessageListenerContainer`.
- **Khoá tìm phiên WebSocket là chuỗi UUID người dùng**, đúng vì `RoomParticipant` cài
  `AuthenticatedPrincipal` và trả `playerId` làm tên. Đây là **ràng buộc giữa hai tính năng**: đổi chỗ đó thì
  thông báo real-time lặng lẽ không tới ai.
- **Đẩy thất bại không được làm vỡ việc tạo thông báo.** Thông báo đã ở trong CSDL, người dùng sẽ thấy ở lần mở
  trang sau. Real-time là phần thêm cho người đang online, không phải điều kiện để thông báo tồn tại.
- **Không có endpoint tạo thông báo.** Thông báo chỉ sinh từ sự kiện thật hoặc job nền — mở một đường ghi qua
  API là mở kênh spam sẵn có.
- **Frontend không tự chèn vào danh sách** khi nhận tin đẩy: nó làm mất hiệu lực cache của TanStack Query để
  hỏi lại máy chủ. Chèn tay thì phải tự lo phân trang, thứ tự và số chưa đọc — ba chỗ dễ lệch với máy chủ.
- **Không hiện toast khi có thông báo mới:** chuông đã có chấm đỏ, còn một hộp bật lên giữa lúc người ta đang
  làm bài thi thì gây hại nhiều hơn giúp.
- Tôn trọng cài đặt người dùng, và chặn ở lúc **tạo** chứ không lọc lúc **đọc**: lọc lúc đọc thì bảng vẫn phình
  theo thứ người dùng đã nói là không muốn, và một ngày nào đó có ai viết truy vấn khác quên mất bộ lọc.
