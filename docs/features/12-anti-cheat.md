# 12 — Chống gian lận thi trực tuyến (Anti-Cheat / Proctoring)

**Ưu tiên:** [S] Should · **Tận dụng:** dữ liệu hành vi, AI phân tích, real-time

## Mục tiêu
Phát hiện và cảnh báo hành vi gian lận trong chế độ thi (bài thi tính điểm & phòng đấu real-time), tăng độ tin cậy của kết quả — một điểm khác biệt mang tính học thuật cho đồ án.

## Use case
- Trong bài thi, hệ thống ghi nhận tín hiệu hành vi bất thường (chuyển tab, copy/paste, thời gian trả lời bất thường).
- Sau bài thi, hệ thống tính **điểm rủi ro (risk score)** và gắn cờ.
- Creator/Admin xem báo cáo tính toàn vẹn (integrity report) để rà soát.

## Yêu cầu chức năng
- **FR-43** [S] ✅ Thu thập tín hiệu hành vi phía client trong chế độ thi:
  - Rời/chuyển tab, mất focus cửa sổ (`visibilitychange`, `blur`).
  - Sao chép/dán (`copy`/`paste`).
  - Thoát toàn màn hình (nếu bật fullscreen).
  - Thời gian trả lời bất thường (quá nhanh so với độ khó).
- **FR-44** [S] ⛔ Phát hiện **đáp án trùng bất thường** giữa các người chơi trong cùng phòng real-time — **bỏ**, xem "Vì sao bỏ FR-44" bên dưới.
- **FR-45** [S] ✅ Tính **risk score** & gắn cờ (flags) cho mỗi lần làm bài; lưu nhật ký sự kiện.
- **FR-46** [S] ✅ **AI phân tích hành vi:** LLM tổng hợp chuỗi sự kiện + số liệu thành nhận định mức độ nghi ngờ + giải thích.
- **FR-47** [S] ✅ Báo cáo tính toàn vẹn cho Creator/Admin; cho phép đánh dấu hợp lệ/không hợp lệ. Có **ba** đường vào, và thiếu một đường nào thì yêu cầu này chỉ đúng với một nhóm người dùng:
  - **Admin** — hàng chờ toàn hệ thống `/admin/integrity`, lọc theo trạng thái rà soát.
  - **Chủ quiz** — cột *Rủi ro* + dòng cảnh báo ở trang thống kê quiz, dẫn sang màn chấm bài. Không có cột này thì chủ quiz *có quyền* xem báo cáo nhưng phải mở từng bài mới tìm ra, nên với hàng trăm bài nộp thì trên thực tế chỉ Admin phát hiện được — còn người hiểu hoàn cảnh lớp mình nhất thì không thấy gì.
  - **Giáo viên chủ nhiệm** — cột *Rủi ro* + nút *Xem bài* ở **bảng theo dõi bài tập của lớp** ([tính năng 14](14-classroom.md)). Đường này thiếu suốt một thời gian, và đó là chỗ thiếu nặng nhất: bài tập giao cho lớp **chạy ở chế độ thi** nên có thu tín hiệu hành vi, nhưng giáo viên mở đúng màn hình đó chứ không mở trang thống kê quiz. Tín hiệu được ghi mà không ai thấy thì bằng không ghi.
- **FR-48** [C] ✅ Chế độ thi nghiêm ngặt: yêu cầu toàn màn hình, khoá chuột phải, cảnh báo khi thoát. *Chủ quiz bật ở form soạn quiz; chỉ áp cho lượt EXAM.* Xem "Vì sao gọi là ma sát chứ không phải khoá" bên dưới.
- **FR-88** [S] ✅ **Minh bạch với người thi:** nói luật *trước* khi bắt đầu (có ô xác nhận đã đọc), và hiện số lần đã ghi nhận *ngay trong lúc thi*. Xem mục riêng bên dưới.

## FR-88 — người thi phải biết mình đang bị ghi nhận cái gì

Bản đầu của tính năng này thu tín hiệu **hoàn toàn im lặng**. Thông báo `ProctoringNotice` có tồn tại, nhưng
nằm ở màn *làm bài* — tức người thi chỉ đọc được **sau khi đồng hồ đã chạy**. Đọc lúc đó thì không còn lựa
chọn nào: không đóng bớt tab được, không đổi chỗ ngồi được, và lần rời trang đầu tiên rất có thể xảy ra ngay
trong lúc đang đọc chính dòng chữ giải thích rằng rời trang sẽ bị ghi lại.

Hệ quả: người thi bị tính điểm rủi ro mà không biết mình đang bị tính cái gì. **Bị đánh giá theo một tiêu chí
không ai nói cho biết là bất công, kể cả khi tiêu chí đó đúng.**

### Ba thay đổi

| Thay đổi | Vì sao |
|---|---|
| **Thông báo đầy đủ chuyển sang trang giới thiệu quiz**, kèm ô "Tôi đã đọc và hiểu" chặn nút Bắt đầu | Biến *"có dán thông báo"* thành *"người thi đã đọc"*. Ô này **không nhớ giữa các lần**: mục đích là đọc trước **lần thi này**, không phải một lần duy nhất rồi thôi |
| Màn làm bài giữ **bản rút gọn một dòng** | Người làm tiếp bài đang dở vào thẳng màn làm bài, không qua trang giới thiệu — bỏ hẳn thì đúng nhóm đó không được nhắc gì |
| **Dòng đếm số lần đã ghi nhận**, cập nhật ngay trong lúc thi | Ngăn ngừa có giá trị hơn bắt lỗi: một con số thấy ngay răn đe mạnh hơn một báo cáo sau bài mà người thi không bao giờ đọc. Nó cũng lọc bớt báo động giả — phần lớn tín hiệu là vô tình, thấy ngay thì người thi tự điều chỉnh |

### Vì sao KHÔNG phải hộp cảnh báo đỏ

Ba lựa chọn trình bày dưới đây đều có chủ ý, bỏ cái nào cũng hỏng mục đích:

- **Không popup.** Một hộp nhảy ra giữa bài thi phá đúng thứ FR-48 đang bảo vệ: sự tập trung. Và nó làm người
  trung thực hoảng nhiều hơn người gian lận — người gian lận vốn đã biết mình đang làm gì.
- **Không màu đỏ.** Đỏ là ngôn ngữ của lỗi. Rời tab không phải lỗi.
- **Chữ "đã ghi nhận", không phải "vi phạm".** Đây là điểm quan trọng nhất. Cả tính năng dựng trên nguyên tắc
  *hệ thống đưa dữ kiện, con người kết luận*, và báo cáo phía giáo viên in thẳng câu đó ra. Hiện chữ *"bạn
  đang vi phạm"* là hệ thống tự phán — **mâu thuẫn với chính dòng chữ nó in ra ở đầu bên kia**. Có một phép
  kiểm riêng đọc câu chữ của thành phần này để bắt kiểu mâu thuẫn đó.

### Hai loại tín hiệu cố ý KHÔNG đếm ra màn hình

`COPY` và `ANSWER_TOO_FAST` vẫn được ghi nhận về máy chủ nhưng không hiện cho người thi:

- **Sao chép đề bài** là việc bình thường của người học nghiêm túc (chép câu hỏi ra giấy nháp).
- **"Trả lời nhanh bất thường"** là một *suy đoán của hệ thống*, không phải một hành động người thi tự biết
  mình vừa làm. Hiện nó lên chỉ gây hoang mang mà không giúp họ sửa được gì.

Ngược lại, chuyển tab và mất tiêu điểm cửa sổ được **gộp làm một** nhóm "rời trang": với người thi đó là một
hành động chứ không phải hai, tách ra chỉ làm con số trông đáng sợ hơn thực tế.

### Lo ngại đã cân nhắc rồi bác bỏ

*"Cho người thi thấy thì họ biết ngưỡng mà né."* Tín hiệu do **chính trình duyệt của họ** gửi lên, nên họ
chặn được và giả mạo được — giao diện báo cáo đã nói thẳng điều đó với giáo viên. Giấu ngưỡng là bảo mật bằng
che giấu, mà lớp che giấu ấy vốn đã thủng sẵn. Đổi lại là mất tính công bằng với **toàn bộ** người thi trung
thực, để phòng một nhóm vốn có cách khác dễ hơn.

## Cảnh báo live trong phòng đấu ✅ (đã làm)

Trước lát cắt này, chống gian lận **chỉ áp cho bài thi cá nhân chế độ EXAM** và phòng đấu (tính năng 04)
không có tín hiệu nào. Giờ phòng đấu có: người chơi báo tín hiệu rời trang, host thấy cờ đỏ trên kênh riêng và
nhắc riêng được, và có bản tổng kết sau ván.

### Một câu trong thiết kế ban đầu KHÔNG thực hiện được

Bản chốt đầu ghi *"sau ván: dùng lại cơ chế `PENDING → VALID/INVALID` kèm ghi chú đã có"*. Khi bắt tay làm thì
câu đó **sai với schema thật**, ở cả hai đầu:

| Ràng buộc của V17 | Phòng đấu |
|---|---|
| `proctoring_events.attempt_id NOT NULL` → `quiz_attempts` | Phòng đấu **không tạo dòng `quiz_attempts` nào** — điểm nằm ở `game_room_players` |
| `proctoring_events.user_id NOT NULL` → `users` | **Khách vãng lai không có dòng `users`** — mà đó là nhóm người phòng đấu tồn tại để phục vụ |

Nhồi một `attempt_id` giả để lách thì mọi truy vấn thống kê theo lượt thi sẽ đếm cả những dòng không thuộc lượt
thi nào. Nên có **bảng riêng** `room_proctoring_events` (V20), khoá theo `player_id` phạm vi phòng.

Và bảng đó **cố ý không có** cột trạng thái rà soát lẫn điểm rủi ro. Hai cột ấy chỉ có nghĩa khi có người quay
lại kết luận; ván xong là phòng tan, không ai quay lại. Thêm chúng vào là hứa một quy trình xử lý không tồn tại.

### Host được làm gì, và không được làm gì

| Trong ván | Sau ván |
|---|---|
| Host thấy cờ, bấm **"Nhắc riêng"** → thí sinh đó nhận thông báo *"hệ thống ghi nhận bạn rời trang làm bài"*. Mọi tín hiệu vào log | Kết luận và xử lý điểm ở màn báo cáo, dùng lại cơ chế `PENDING → VALID/INVALID` kèm ghi chú đã có |

**Không có trừ điểm, không buộc nút Kick vào tín hiệu hành vi.** Lý do là lý do trung tâm của cả tính năng:
ở màn rà soát sau bài thi, giáo viên có *thời gian* — đọc chuỗi tín hiệu, cân nhắc hoàn cảnh, hỏi lại học sinh,
rồi mới kết luận, và quyết định lùi lại được. Giữa phòng đấu thì host có ba giây, giữa lúc đang điều hành, trên
một tín hiệu vẫn giả mạo được và vẫn có cách giải thích vô hại. Một thông báo hệ thống bật lên → cờ đỏ → học
sinh bị đuổi khỏi cuộc thi tính điểm, không hoàn tác được, không được nói gì. Nhắc thì đủ để người định gian
lận biết mình đang bị thấy, mà không phạt oan ai.

Kick vẫn nên có, nhưng cho việc khác (phá phòng, biệt danh bậy) — đó là việc của tính năng 04.

### Ba việc phải làm trước — đã làm cả ba

1. **Kênh riêng cho host** ✅ — `PROCTORING_FLAG` đi qua `GameEventPublisher.toUser(..., hostId, ...)` tới
   `/user/queue/room/{code}`, không lên `/topic/room/{code}`. Hạ tầng này đã có sẵn từ `ANSWER_RESULT`.
2. **Ngưỡng khác bài thi** ✅ — `RoomFlagDetector` đếm **số câu khác nhau** có khuôn rời-rồi-về, ngưỡng 2 câu.
   Không đếm số lần: bốn lần rời-về trong *cùng một câu* vẫn không gắn cờ, dù ở bài thi cá nhân thì chuỗi đó
   vượt ngưỡng "chuyển tab 3 lần".
3. **Khách vãng lai** ✅ — `room_proctoring_events.player_id` là danh tính phạm vi phòng, không có khoá ngoại
   tới `users`. Khách nhận được lời nhắc vì `RoomParticipant` cài `AuthenticatedPrincipal` trả `playerId`, nên
   `convertAndSendToUser` tìm đúng phiên WebSocket của họ mà không cần JWT.

### Vì sao khuôn lặp tự loại được trường hợp vô hại

Server đóng số thứ tự câu *đang mở* vào mỗi tín hiệu. Người bị gián đoạn thật — nghe một cuộc gọi 30 giây —
lúc quay lại thì ván đã sang câu sau, và WebSocket vẫn mở nên client đã nhận câu mới trong lúc ẩn: tín hiệu
`TAB_VISIBLE` của họ mang **số câu khác**. Câu bị rời chỉ có một nửa cặp, không thành khuôn.

Người tra cứu ở tab khác thì phải quay lại *trước khi hết giờ* mới trả lời được, nên cả hai nửa cùng một số
câu. Khuôn này không cần biết họ đi đâu; nó chỉ phân biệt *đi rồi về kịp để trả lời* với *đi và mất câu đó*.

### Một bài học về test, không phải về sản phẩm

Test tích hợp đầu tiên đỏ ba chỗ, và ba test phủ định **xanh rỗng** cùng lúc đó. Nguyên nhân: Spring xử lý
message STOMP trên một **bể luồng**, nên hai frame gửi cách nhau vài milli-giây không có thứ tự đảm bảo — kể cả
khi cùng một session. Cả bốn tín hiệu bị xử lý sau lệnh `next` của host nên cùng mang một số câu, và cờ không
bao giờ sinh ra.

Chờ một sự kiện quay về (`ANSWER_RESULT`) cũng không cứu được, vì nó cũng chỉ là một message khác trên cùng bể
luồng đó. Điểm đồng bộ duy nhất đáng tin là **trạng thái đã ghi**: đọc `GET /rooms/{code}/proctoring` cho tới
khi thấy đúng số liệu. Và mỗi test phủ định giờ kèm một **đối chứng dương** — chứng minh tín hiệu *đã* ghi đủ
rồi hệ thống mới chủ động không gắn cờ; thiếu nó thì "không có cờ" vẫn xanh khi cả đường ghi bị hỏng.

FR-44 vẫn không làm được (phòng đấu không lưu lựa chọn từng câu) nhưng cảnh báo live không cần dữ liệu đó, nên
không đụng tới quyết định dưới đây.

## Vì sao bỏ FR-44

Phòng đấu real-time (tính năng 04) giữ toàn bộ diễn biến ván trong Redis và **chỉ ghi xuống PostgreSQL bảng
xếp hạng cuối ván** — không lưu ai chọn phương án nào ở câu nào. Muốn đối chiếu đáp án giữa các người chơi thì
phải đổi mô hình dữ liệu của tính năng 04 để lưu từng lựa chọn của từng người ở từng câu.

Đó là một cái giá thật cho một tín hiệu yếu: trong phòng đấu mọi người trả lời **cùng một bộ câu trắc nghiệm
4 phương án**, nên hai người cùng chọn đúng là chuyện thường, và hai người cùng chọn *sai giống nhau* cũng
thường vì các phương án gây nhiễu được thiết kế để hấp dẫn. Với phòng 5–10 người, trùng khớp cao xuất hiện
liên tục mà không có ai gian lận — tức là tín hiệu này chủ yếu sinh ra báo động sai. Sáu tín hiệu còn lại của
FR-43 nói được nhiều hơn với chi phí thấp hơn hẳn.

Kết quả: phần chống gian lận này áp dụng cho **bài thi cá nhân chế độ EXAM**, không áp dụng cho phòng đấu.
Nếu về sau tính năng 04 có nhu cầu lưu chi tiết từng câu vì lý do khác (xem lại ván, thống kê câu khó trong
phòng đấu) thì FR-44 làm được gần như miễn phí trên dữ liệu đó.

## Bốn quyết định của bản này

| Quyết định | Vì sao |
|---|---|
| **Người làm bài không xem được điểm rủi ro của chính mình** (404, không phải 403) | Biết chính xác tín hiệu nào bị tính và nặng bao nhiêu là biết cách tránh. 403 thì cũng đã tiết lộ rằng lượt đó có báo cáo và đang bị gắn cờ |
| **Server dựng lại `detail` từ danh sách trường vô hại**, không tin gói tin của client | Client đã chỉ gửi độ dài đoạn dán, nhưng nếu chỉ có một lớp thì một bản client bị sửa là đủ để nội dung người dùng chảy vào cơ sở dữ liệu. Hai lớp cùng bảo đảm một điều |
| **Trọng số giảm dần theo số lần** (hệ số 0.3 từ lần thứ 4) | Bài thi 60 phút trên máy có thông báo hệ thống thì mất focus mươi lần là bình thường. Cộng tuyến tính thì mọi bài thi dài đều bị gắn cờ, và khi mọi bài đều bị gắn cờ thì cờ không còn nghĩa gì |
| **`review_status` mặc định PENDING, hệ thống không bao giờ tự kết luận** | Đặc tả ghi rõ "không tự động phạt". API còn từ chối nhận `PENDING` như một kết luận — nó là trạng thái ban đầu, không phải một lựa chọn của người rà soát |
| **Danh sách bài làm của chủ quiz chỉ nhận điểm rủi ro của bài VƯỢT NGƯỠNG; dưới ngưỡng thì máy chủ trả `null`** | Gắn một con số "mức đáng ngờ" vào *từng* người học là mời người ta xếp hạng học sinh theo độ nghi — đúng tác hại cả tính năng này cố tránh. Một điểm 45 không kèm cờ nào cũng không dùng được vào việc gì: danh sách lý do rỗng. Quyết định đặt ở **máy chủ**, không để giao diện tự lọc, cùng lý do với 404 thay vì 403 |

**Điểm rủi ro luôn được trả kèm một câu nhắc** (`canhBao`) rằng tín hiệu giả mạo được và đây không phải bằng
chứng. Giao diện hiện câu đó **cạnh con số**, không giấu xuống cuối trang: người đọc phải thấy hai thứ cùng
lúc. Thẻ báo cáo cũng cố ý dùng **màu cam thay vì đỏ** — đỏ đọc thành "đã kết luận có tội", còn trạng thái
thật chỉ là "đáng xem".

## Luồng xử lý
```
Client (chế độ thi) lắng nghe sự kiện → gửi proctoring events (batch, qua REST/WebSocket)
   → server lưu proctoring_events
Khi nộp bài → tính risk score từ:
   - tần suất/loại sự kiện (chuyển tab, paste...)
   - thời gian trả lời so với baseline độ khó
   - (real-time) độ tương đồng đáp án với người chơi khác
   → AI tổng hợp → nhận định + giải thích
   → lưu attempt_integrity (risk_score, flags, ai_note)
Creator/Admin xem báo cáo → xác nhận hợp lệ / không hợp lệ
```

## Tính điểm rủi ro (gợi ý)
`risk_score` = tổng có trọng số của các tín hiệu, chuẩn hóa 0–100:
- Mỗi lần chuyển tab/mất focus: +trọng số.
- Paste nội dung dài: +trọng số cao.
- Thời gian trả lời < ngưỡng tối thiểu theo độ khó: +trọng số.
- Đáp án trùng khớp bất thường với người chơi khác (real-time): +trọng số cao.
- Ngưỡng cảnh báo: ví dụ ≥ 60 → gắn cờ nghi ngờ.

## API liên quan
```
POST   /api/v1/attempts/{id}/proctoring-events   Gửi sự kiện hành vi (batch ≤ 50) — chỉ lượt EXAM của chính mình
GET    /api/v1/attempts/{id}/integrity           Báo cáo tính toàn vẹn (chủ quiz hoặc Admin)
PUT    /api/v1/attempts/{id}/integrity/review    Đánh dấu hợp lệ/không hợp lệ (chủ quiz hoặc Admin)
GET    /api/v1/admin/integrity/flagged           Hàng chờ bài bị gắn cờ (Admin), lọc theo review_status
GET    /api/v1/rooms/{code}/proctoring          Tổng kết tín hiệu của phòng đấu — CHỈ host
```
Và hai kênh STOMP của phòng đấu:
```
SEND   /app/room/{code}/proctoring   Tín hiệu rời trang { type: TAB_HIDDEN | TAB_VISIBLE }
SEND   /app/room/{code}/warn         Host nhắc riêng một người { playerId }
```
**Đường dẫn rà soát không nằm dưới `/admin/`** như bản nháp ban đầu của tài liệu này: FR-47 nói rõ báo cáo dành
cho *Creator/Admin*, nên tiền tố `/admin/` sẽ mâu thuẫn với chính yêu cầu đó — chủ quiz phải kết luận được bài
của quiz mình mà không cần quyền quản trị. Chỉ **hàng chờ toàn hệ thống** là việc của Admin nên giữ `/admin/`.

**Bài thi cá nhân dùng REST theo lô, phòng đấu dùng STOMP gửi ngay** — hai đường khác nhau vì hai mục đích
khác nhau, không phải vì thiếu nhất quán:

| | Bài thi cá nhân | Phòng đấu |
|---|---|---|
| Đường đi | REST, gom lô 10 giây | STOMP, gửi ngay |
| Vì sao | Tín hiệu chỉ dùng để tính điểm rủi ro *sau khi nộp*; trễ 10 giây không ai thấy, mà gom lô thì bớt request lúc người ta đang thi | Cờ phải tới host **trong lúc câu hỏi còn sống**; gom lô thì cờ đến sau khi ván đã sang câu khác và host chẳng còn gì làm với nó |
| Loại tín hiệu | 6 loại, có cả `COPY`/`PASTE` | 2 loại, chỉ `TAB_HIDDEN`/`TAB_VISIBLE` — đáp án phòng đấu là nút bấm, không có gì để dán |

## Vì sao FR-48 gọi là ma sát chứ không phải khoá

Đặc tả viết *"**bắt buộc** fullscreen"*. Không làm được, và điều quan trọng là **nói thẳng** thay vì để
người dùng tự phát hiện:

| Trình duyệt cho phép | Trình duyệt KHÔNG cho phép |
|---|---|
| Vào toàn màn hình **từ một cú bấm của người dùng** | Tự vào toàn màn hình khi trang mở |
| Biết lúc người dùng thoát ra | Chặn phím Esc |
| Chặn menu chuột phải | Chặn F12 / Ctrl+Shift+I |

Nên FR-48 làm ba việc: (1) che đề cho tới khi người học **chủ động** bấm vào toàn màn hình; (2) phát hiện
lúc thoát và nhắc quay lại; (3) để lại tín hiệu `FULLSCREEN_EXIT` trong hồ sơ.

Giá trị thật nằm ở chỗ **biến việc rời bài thi thành có chủ ý** và để lại dấu vết, chứ không phải dựng một
bức tường. Đây đúng nguyên tắc trung tâm của cả tính năng 12: *hệ thống đưa dữ kiện, người thật quyết định*.

**Ba chỗ trong giao diện đều nói thật về giới hạn này** — chữ trợ giúp ở form soạn quiz, cảnh báo ở trang
giới thiệu quiz, và cửa vào trước khi làm bài. Nói dối rằng *không thể* thoát là một lời hứa mà ai cũng tự
phát hiện là sai ngay lần đầu bấm Esc; và khi đó họ kết luận cả cơ chế giám sát là trò đùa. Nguy hiểm hơn:
**giáo viên sẽ tin vào một rào chắn không tồn tại** và bỏ qua việc rà soát tín hiệu — tức mất đúng thứ có
tác dụng thật.

**Che đề, không chỉ hiện cảnh báo.** Một dải cảnh báo mà bên dưới vẫn đọc được đề thì chẳng ai bấm nút, và
chế độ nghiêm ngặt thành một dòng chữ trang trí. Nhưng **thoát ra giữa chừng thì chỉ nhắc, không che lại**:
che đi là phạt người bấm nhầm Esc bằng cách chặn họ làm tiếp, trong khi tín hiệu đã ghi rồi.

**Thiết bị không hỗ trợ thì vẫn cho làm bài.** Safari trên iPhone không có Fullscreen API cho phần tử
thường. Chặn ở đó là biến một hạn chế của thiết bị thành mất quyền dự thi.

## Dữ liệu liên quan (bổ sung PostgreSQL)

`V17__anti_cheat.sql` — bài thi cá nhân:
- `proctoring_events(id, attempt_id, user_id, event_type, detail jsonb, occurred_at)`
- `attempt_integrity(id, attempt_id, risk_score, flags jsonb, ai_note text, review_status: PENDING/VALID/INVALID, reviewed_by, reviewed_at, review_note)`

`V21__strict_exam.sql` — chế độ thi nghiêm ngặt:
- `quizzes` thêm cột `strict_exam BOOLEAN NOT NULL DEFAULT FALSE`

Đặt ở **quiz** chứ không phải ở lượt làm bài: người quyết định mức nghiêm khắc là người ra đề. Đặt ở
`quiz_attempts` thì người làm bài tự chọn được, tức tự tắt được. Mặc định **FALSE** để không đổi hành vi
của những quiz đang chạy mà chủ quiz không hề biết.

`V20__room_proctoring.sql` — phòng đấu:
- `room_proctoring_events(id, room_id, player_id, player_name, is_guest, event_type, question_index, occurred_at)`

**Không** có `risk_score` lẫn `review_status` ở bảng phòng đấu, và **không** có khoá ngoại tới `users`. Cả hai
đều là quyết định, không phải thiếu sót — lý do ở mục "Cảnh báo live trong phòng đấu" phía trên.

`detail` **chỉ chứa số** — độ dài đoạn dán, số giây. Không bao giờ chứa nội dung người dùng: server tự dựng
lại trường này thay vì lưu nguyên gói tin của client.

## Ghi chú kỹ thuật & Ràng buộc
- **Chỉ áp dụng chế độ thi** (không áp dụng luyện tập), thông báo rõ cho người dùng (minh bạch, tôn trọng quyền riêng tư — **không** ghi hình/không thu thập dữ liệu ngoài phạm vi bài thi).
- Tín hiệu client **có thể bị bỏ qua/giả mạo** → chỉ dùng làm cảnh báo hỗ trợ con người quyết định, **không tự động phạt**.
- AI phân tích qua AiOrchestrator (fallback Gemini→Grok); không gửi PII — prompt chỉ nhận **số đếm theo loại
  tín hiệu**, không có tên người, email hay nội dung bài làm.
- ~~Phát hiện đáp án trùng dùng dữ liệu phòng ở Redis + đối chiếu sau ván.~~ Không áp dụng — FR-44 đã bỏ.
- **Phòng đấu: quyền của host dừng ở nhắc riêng.** Không trừ điểm, không đuổi. Cùng nguyên tắc "không tự động
  phạt" ở trên, nhưng ở phòng đấu nó nghiêm hơn một bậc: host chỉ có ba giây và không có đường lùi lại.
- **Tín hiệu phòng đấu do server đóng số câu**, không nhận số câu từ client — nếu tin client thì một client sửa
  đổi có thể dồn mọi tín hiệu vào một câu để không bao giờ thành khuôn lặp.
- Là dữ liệu tốt cho phần **đánh giá** trong báo cáo (đo tỉ lệ phát hiện đúng/nhầm) — nhưng muốn có tỉ lệ thì
  cần một bộ bài thi **có nhãn** (biết trước bài nào gian lận). Chưa có bộ đó thì mục 3.6 chỉ ghi được nhận
  định định tính, không ghi số.
