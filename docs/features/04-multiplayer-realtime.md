# 04 — Phòng đấu trí thời gian thực (Multiplayer)

**Ưu tiên:** [M] Must · **Trụ cột phiếu:** Real-time độ trễ thấp (Spring WebSocket + Redis)

## Mục tiêu
Cho phép nhiều người chơi cùng tham gia một phòng đấu quiz thời gian thực, đồng bộ chính xác, độ trễ thấp — tương tự Kahoot.

## Use case
- Host tạo phòng từ một quiz; hệ thống sinh **mã PIN 6 số** và **mã QR**, host chiếu lên màn hình.
- Người chơi **quét QR bằng điện thoại** hoặc gõ mã PIN để vào phòng chờ.
- Ở phòng chờ, mỗi người chọn nhân vật rồi bấm **Sẵn sàng**; danh sách cập nhật real-time.
- Host bắt đầu; mọi người nhận cùng câu hỏi đồng thời, trả lời, thấy bảng xếp hạng trực tiếp.

## Yêu cầu chức năng
- **FR-20** [M] ✅ Tạo/tham gia phòng bằng room code; host điều khiển bắt đầu/kết thúc.
- **FR-21** [M] ✅ Đồng bộ real-time qua **Spring WebSocket (STOMP)**: người vào/ra, câu hiện tại, đếm ngược, kết quả.
- **FR-22** [M] ✅ Mọi người nhận cùng câu hỏi đồng thời; tính điểm theo độ chính xác + tốc độ trả lời.
- **FR-23** [M] ✅ **Live leaderboard** cập nhật sau mỗi câu.
- **FR-24** [M] ✅ Dùng **Redis** lưu trạng thái phòng + Pub/Sub đồng bộ giữa các instance backend.
- **FR-25** [S] ✅ Kết nối lại tự động (`@stomp/stompjs`) + đồng bộ trạng thái bằng `GET /rooms/{code}`.

### Quản lý phòng chơi & mã QR
- **FR-26** [M] ✅ Mã phòng là **PIN 6 chữ số**; kèm **mã QR** trỏ tới `/join/{PIN}`.
- **FR-27** [M] ✅ Quét QR vào thẳng phòng chờ qua WebSocket, không phải gõ mã.
- **FR-28a** [M] ✅ **Khách vãng lai** (chưa đăng nhập) vào chơi được — *chỉ khi host bật cho phòng đó*.
- **FR-28b** [M] ✅ **Avatar**: khách bấm "Ngẫu nhiên" hoặc chọn nhân vật; thành viên đổi được ngay trong phòng.
- **FR-28c** [M] ✅ **Phòng chờ live**: danh sách người chơi dạng thẻ kèm avatar, trạng thái **Đã sẵn sàng**.

## Luồng xử lý
```
Host tạo phòng → sinh PIN 6 số → game_rooms (Postgres) + room:{code} (Redis, TTL)
   → frontend vẽ QR trỏ tới /join/{PIN}

Người chơi vào phòng, một trong hai đường:
  (a) Thành viên : POST /rooms/{PIN}/join            → dùng JWT sẵn có
  (b) Khách      : POST /rooms/{PIN}/join-as-guest   → nhận guestKey (chỉ dùng cho phòng này)
   → WebSocket CONNECT (Authorization HOẶC X-Guest-Key) → subscribe /topic/room/{code}
   → server phát PLAYER_JOINED

Phòng chờ: đổi avatar / bấm Sẵn sàng → PLAYER_AVATAR_CHANGED, PLAYER_READY
Host gửi /app/room/{code}/start → server phát GAME_STARTED + QUESTION đầu tiên
Mỗi câu:
   - server phát QUESTION (đồng thời) + bắt đầu đếm ngược
   - người chơi gửi /app/room/{code}/answer { questionId, answer, timeMs }
   - server chấm, tính điểm (đúng + nhanh), phát ANSWER_RESULT + LEADERBOARD
Kết thúc → server phát GAME_FINISHED, lưu điểm cuối vào game_room_players
```

## API liên quan
[api.md](../api.md) mục 5 (REST `/rooms` + WebSocket `/ws`, kênh `/topic/room/{code}`, `/app/room/{code}/*`).

## Dữ liệu liên quan
- Postgres: `game_rooms`, `game_room_players` — metadata & kết quả cuối.
- Redis: `room:{code}` (trạng thái live), `room:{code}:events` (Pub/Sub) — [database.md](../database.md) mục 3.

## Quyết định thiết kế (đã hiện thực)

**1. Mọi thông điệp đi vòng qua Redis Pub/Sub.** Broker của Spring nằm trong bộ nhớ từng instance;
gửi thẳng thì người chơi đang giữ WebSocket ở instance khác sẽ không nhận được. Nên `GameEventPublisher`
luôn publish lên `room:{code}:events`, còn `GameEventRelay` ở **mọi** instance nghe theo mẫu
`room:*:events` rồi chuyển tiếp cho client của riêng mình. Nghe theo mẫu thay vì subscribe từng phòng
để mở/đóng phòng không phải quản lý vòng đời subscription.

**2. Thời gian do server đo.** Mốc phát câu hỏi nằm trong trạng thái phòng ở Redis; điểm tốc độ tính
từ hiệu số ở server. Payload đáp án **cố tình không có** trường thời gian — nếu tin client thì ai cũng
khai "trả lời trong 1ms" để ăn trọn điểm.

**3. Đáp án chỉ rời server khi câu đã đóng.** `QUESTION` không kèm đáp án đúng. Người trả lời nhận
kết quả của riêng mình qua `/user/queue/…`; cả phòng chỉ biết *số người* đã xong. Nếu phát kết quả cho
cả phòng, người chưa trả lời chỉ cần nhìn ai vừa được cộng điểm là đoán ra.

**4. Chia hai nơi lưu.** PostgreSQL giữ metadata và điểm cuối; Redis giữ trạng thái đang chơi. Mỗi
lượt trả lời chỉ chạm Redis — đây là điểm nóng của kiểm thử chịu tải sau này.

**5. Khoá khi cộng điểm.** Nhiều người bấm đáp án gần như cùng lúc, nếu mỗi luồng đọc-sửa-ghi JSON
trạng thái thì sẽ mất lượt (lost update). `RoomStateStore.update` giữ khoá `room:{code}:lock` (SETNX
kèm TTL) trong lúc đọc-sửa-ghi; TTL ngắn để tiến trình chết giữa chừng không treo phòng.

**6. Không có job nền đếm giờ.** Client đếm ngược tới `deadlineAtMillis`; server chỉ việc từ chối đáp
án gửi sau hạn. Câu chuyển khi host bấm `/next` hoặc khi mọi người đã trả lời. Bớt được một scheduler
mà vẫn đúng, vì hạn nộp là dữ liệu chứ không phải sự kiện.

**7. Dùng lại `AnswerGrader` của chế độ chơi đơn.** Hai chế độ không bao giờ chấm khác nhau trên cùng
một câu hỏi; sửa luật chấm chỉ phải sửa một chỗ.

**8. Mã phòng là PIN toàn chữ số.** Bản đầu dùng chữ + số và bỏ các ký tự dễ nhầm; nay chuyển hẳn
sang 6 chữ số vì người chơi gõ trên **bàn phím số của điện thoại** — không phải chuyển qua lại giữa
chữ và số, và không còn khả năng nhầm O với 0.

**9. QR vẽ ở phía client.** Nội dung QR chỉ là một đường dẫn `/join/{PIN}`, nên sinh ở đâu cũng cho
kết quả như nhau. Vẽ bằng `qrcode.react` thì khỏi thêm thư viện vào backend, khỏi truyền ảnh qua
mạng, và là SVG nên co giãn theo màn hình khi chiếu máy chiếu.

**10. Khách dùng *khoá phiên*, không dùng JWT.** JWT của hệ thống gắn với một tài khoản có thật và
mang vai trò RBAC; khách thì không có dòng nào trong `users`. Cấp JWT cho họ sẽ phải thêm vai trò
GUEST vào enum `Role`, kéo theo ràng buộc CHECK của bảng `users` và mọi chỗ phân quyền. Thay vào đó
khách nhận một khoá ngẫu nhiên trong Redis, **gắn chặt với đúng một phòng** và tự hết hạn — không mở
được bất kỳ API nào khác.

**11. Cho khách vào là tuỳ chọn của từng phòng** (`game_rooms.allow_guests`, mặc định *tắt*). Giữ
được luật chung "chưa đăng nhập thì không vào phòng đấu" làm mặc định an toàn, mà lớp học vẫn dùng
được kiểu quét-là-chơi khi giáo viên chủ động bật.

**12. Avatar là emoji trên nền màu, không phải file ảnh.** Không phải tải ảnh, không phụ thuộc dịch
vụ ảnh bên ngoài, chạy được cả khi mất mạng. Đổi lại đây là biểu tượng vui chứ không phải tranh
nhân vật vẽ tay — muốn có bộ chibi thật thì phải mua hoặc tự vẽ rồi thay `PlayerAvatar`.

**13. `next` tính bước chuyển ngay trong khoá.** Kênh STOMP đến của Spring chạy đa luồng, nên hai
lệnh "câu tiếp theo" gửi sát nhau có thể xử lý song song, cùng đọc một trạng thái cũ và cùng phát
câu kế tiếp. *Lỗi này chỉ lộ ra khi chạy thật với hai client.*

## Ghi chú kỹ thuật
- **Redis Pub/Sub** để khi backend chạy nhiều instance, message phòng vẫn đồng bộ tới đúng người chơi.
- Tính điểm theo tốc độ: điểm cao hơn nếu trả lời đúng nhanh.
- Trạng thái live nằm ở Redis (không ở DB quan hệ) để giảm độ trễ.
- Là đối tượng chính của **kiểm thử chịu tải** — xem [roadmap.md](../roadmap.md) mục 2.2.
