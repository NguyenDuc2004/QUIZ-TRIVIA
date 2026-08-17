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
- **FR-47** [S] ✅ Báo cáo tính toàn vẹn cho Creator/Admin; cho phép đánh dấu hợp lệ/không hợp lệ. Có **hai** đường vào, và thiếu một trong hai thì yêu cầu này chỉ đúng nửa vời:
  - **Admin** — hàng chờ toàn hệ thống `/admin/integrity`, lọc theo trạng thái rà soát.
  - **Chủ quiz** — cột *Rủi ro* + dòng cảnh báo ở trang thống kê quiz, dẫn sang màn chấm bài. Không có cột này thì chủ quiz *có quyền* xem báo cáo nhưng phải mở từng bài mới tìm ra, nên với hàng trăm bài nộp thì trên thực tế chỉ Admin phát hiện được — còn người hiểu hoàn cảnh lớp mình nhất thì không thấy gì.
- **FR-48** [C] ⏳ Chế độ thi nghiêm ngặt: bắt buộc fullscreen, khóa chuột phải, cảnh báo khi vi phạm.

## Cảnh báo live trong phòng đấu — thiết kế đã chốt, làm sau tính năng 16

Phần chống gian lận này **chỉ áp cho bài thi cá nhân chế độ EXAM**. Phòng đấu (tính năng 04) hiện không có
tín hiệu nào. Đây là lỗ thật, đã chốt hướng làm nhưng **hoãn tới sau tính năng 16 (Thông báo)** vì hạ tầng gửi
thông báo tới một người là thứ phần này cần và tính năng 16 chính là nó.

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

### Ba việc phải làm trước, không phải chi tiết

1. **Thêm kênh riêng cho host.** Hiện chỉ có **một** kênh phát `/topic/room/{code}` và *mọi người chơi đều
   subscribe nó*. Đẩy cảnh báo lên đó là công bố tên người bị nghi cho cả phòng — làm nhục công khai dựa trên
   tín hiệu giả mạo được. Phải dùng `convertAndSendToUser` tới đúng host.
2. **Ngưỡng phải khác ngưỡng bài thi.** Phòng đấu nhiễu hơn nhiều: người chơi trên điện thoại, một tin nhắn
   đến là một `visibilitychange`. Phòng 10 người × 10 câu thì gần như chắc chắn có người đạt 3 lần mà không
   gian lận gì. Nên báo theo **khuôn lặp** (rời trang rồi quay lại đúng trước khi hết giờ câu, lặp ở nhiều câu)
   chứ không đếm số lần — cùng bài học với trọng số giảm dần ở đây.
3. **Khách vãng lai.** Phòng đấu cho khách vào bằng mã PIN, họ không có tài khoản nên log gắn với *khoá phiên*
   chứ không phải `user_id`. Thông báo minh bạch vẫn phải hiện cho họ.

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
```
**Đường dẫn rà soát không nằm dưới `/admin/`** như bản nháp ban đầu của tài liệu này: FR-47 nói rõ báo cáo dành
cho *Creator/Admin*, nên tiền tố `/admin/` sẽ mâu thuẫn với chính yêu cầu đó — chủ quiz phải kết luận được bài
của quiz mình mà không cần quyền quản trị. Chỉ **hàng chờ toàn hệ thống** là việc của Admin nên giữ `/admin/`.

Kênh WebSocket `/app/room/{code}/proctoring` **không hiện thực** — nó chỉ cần cho FR-44, mà FR-44 đã bỏ. Tín
hiệu đi bằng REST theo lô 10 giây một lần; đây là dữ liệu nền không cần độ trễ thấp, dùng WebSocket cho nó chỉ
thêm một kênh phải trông.

## Dữ liệu liên quan (bổ sung PostgreSQL) — `V17__anti_cheat.sql`
- `proctoring_events(id, attempt_id, user_id, event_type, detail jsonb, occurred_at)`
- `attempt_integrity(id, attempt_id, risk_score, flags jsonb, ai_note text, review_status: PENDING/VALID/INVALID, reviewed_by, reviewed_at, review_note)`

`detail` **chỉ chứa số** — độ dài đoạn dán, số giây. Không bao giờ chứa nội dung người dùng: server tự dựng
lại trường này thay vì lưu nguyên gói tin của client.

## Ghi chú kỹ thuật & Ràng buộc
- **Chỉ áp dụng chế độ thi** (không áp dụng luyện tập), thông báo rõ cho người dùng (minh bạch, tôn trọng quyền riêng tư — **không** ghi hình/không thu thập dữ liệu ngoài phạm vi bài thi).
- Tín hiệu client **có thể bị bỏ qua/giả mạo** → chỉ dùng làm cảnh báo hỗ trợ con người quyết định, **không tự động phạt**.
- AI phân tích qua AiOrchestrator (fallback Gemini→Grok); không gửi PII — prompt chỉ nhận **số đếm theo loại
  tín hiệu**, không có tên người, email hay nội dung bài làm.
- ~~Phát hiện đáp án trùng dùng dữ liệu phòng ở Redis + đối chiếu sau ván.~~ Không áp dụng — FR-44 đã bỏ.
- Là dữ liệu tốt cho phần **đánh giá** trong báo cáo (đo tỉ lệ phát hiện đúng/nhầm) — nhưng muốn có tỉ lệ thì
  cần một bộ bài thi **có nhãn** (biết trước bài nào gian lận). Chưa có bộ đó thì mục 3.6 chỉ ghi được nhận
  định định tính, không ghi số.
