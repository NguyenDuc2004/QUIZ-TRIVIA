# Số liệu mục 3.5 — Đánh giá hiệu năng real-time

> Đo thật ngày **08/08/2026**. Mọi con số dưới đây đến từ một lần chạy đo cụ thể, không ước lượng.
> Kịch bản và mã đo: `loadtest_room.mjs`, `loadtest_two_instances.mjs`.

## 1. Điều kiện đo

| Hạng mục | Giá trị |
|---|---|
| Máy | Một máy đơn — **máy chủ và toàn bộ client chạy cùng máy** |
| Backend | Spring Boot 3.5, Java 21, một instance (mục 3 dùng hai) |
| Hạ tầng | PostgreSQL 16, Redis 7, Neo4j 5 — cùng máy, qua Docker |
| Giao thức | STOMP over WebSocket, phát tán qua Redis Pub/Sub |
| Người chơi | Khách vãng lai (đúng kịch bản quét QR vào phòng) |
| Mỗi vòng | 3 câu hỏi, 60 giây/câu |

**Số liệu này KHÔNG bao gồm độ trễ mạng thật.** Nó đo chi phí xử lý của máy chủ và tầng phát tán,
không đo trải nghiệm người dùng ở xa. Ghi rõ để không ai đọc nhầm thành "người dùng thấy 20ms".

## 2. Cách đo — không phải sửa code nghiệp vụ để đo được

Sự kiện `QUESTION` vốn đã mang `deadlineAtMillis` — mốc hết giờ theo đồng hồ máy chủ, dùng để client
đếm ngược. Trừ đi thời lượng câu hỏi ra được **mốc máy chủ phát đi**; so với lúc client nhận là ra
độ trễ. Máy chủ và client cùng máy nên hai đồng hồ là một.

**Vì sao không dùng k6/Gatling như kế hoạch ban đầu:** cả hai không nói được STOMP over SockJS nếu
không viết thêm extension, mà thứ cần đo lại chính là đường đó. Harness tự viết dùng đúng thư viện
`@stomp/stompjs` mà trình duyệt dùng — nói đúng giao thức thật thay vì giả lập.

## 3. Kết quả: độ trễ phát câu hỏi theo số người trong phòng

| Người chơi | Nối vào phòng | P50 | P95 | Max | Sự kiện mất |
|---:|---:|---:|---:|---:|---:|
| 10 | 244 ms | 18 ms | 20 ms | 20 ms | **0** |
| 30 | 761 ms | 26 ms | 32 ms | 32 ms | **0** |
| 50 | 1 349 ms | 48 ms | 52 ms | 52 ms | **0** |
| 100 | 4 494 ms | 180 ms | 216 ms | 219 ms | **0** |
| 150 | 11 454 ms | 542 ms | 566 ms | 568 ms | **0** |
| 200 | 25 101 ms | 1 411 ms | 1 509 ms | 1 511 ms | **0** |

**Không mất một sự kiện nào ở mọi mức tải.** Hệ thống không rơi rớt tin nhắn — nó chỉ chậm dần.
Đây là kiểu suy giảm dễ chịu: người chơi thấy câu hỏi tới muộn, chứ không có ai bị bỏ lại.

Độ trễ tăng **siêu tuyến tính**: gấp đôi người chơi từ 50 lên 100 làm P95 tăng 4 lần; từ 100 lên 200
tăng thêm 7 lần.

Ngưỡng thực dụng: **dưới 100 người/phòng thì P95 ≤ 216 ms** — vẫn nằm trong mức người dùng cảm nhận
là tức thời với một trò chơi hỏi đáp. Trên 150 người thì độ trễ nửa giây trở lên bắt đầu ảnh hưởng
tới sự công bằng, vì người nhận muộn có ít thời gian trả lời hơn.

## 4. Nghẽn nằm ở đâu — tách phần nhận và phần gửi

Ở mức 200 người, P50 (1 411 ms) và P95 (1 509 ms) gần bằng nhau. Nếu chậm là do phát tán xuống thì
người nhận đầu tiên phải nhanh hơn hẳn người cuối, tức khoảng cách P50–P95 phải rộng. Nó hẹp, nghĩa
là **mọi người bị trễ gần như nhau** — nghẽn xảy ra *trước* lúc phát tán.

Kiểm chứng bằng cách chạy lại 200 người nhưng **không gửi đáp án**:

| 200 người chơi | P50 | P95 |
|---|---:|---:|
| Có gửi đáp án (mọi người trả lời cùng lúc) | 1 411 ms | 1 509 ms |
| **Chỉ nhận câu hỏi, không gửi gì** | **262 ms** | **638 ms** |

Khoảng **80% độ trễ đến từ việc xử lý 200 đáp án gửi lên**, không phải từ việc phát câu hỏi xuống.
Lệnh "câu tiếp theo" của chủ phòng phải xếp hàng sau chúng trên cùng một kênh vào.

Hệ quả cho việc tối ưu: nới kênh xử lý *đầu vào* mới có tác dụng; tối ưu phát tán thì gần như không.
Đây là loại kết luận chỉ có được khi tách hai nguồn ra đo riêng, thay vì nhìn một con số tổng.

## 5. Vai trò của Redis Pub/Sub

Phiếu yêu cầu *"so sánh có/không Redis Pub/Sub"*. Trong kiến trúc này **không có chế độ tắt Redis để
so**: mọi sự kiện đều đi qua nó, kể cả khi gửi tới người chơi trên chính instance vừa phát. Bỏ Redis
ra không làm hệ thống chậm hơn — nó làm phòng đấu nhiều instance **không còn hoạt động**.

Chứng minh bằng suy luận loại trừ: chạy **hai instance** (cổng 8080 và 8081) dùng chung Redis, chia
40 người chơi ra hai bên, host bắt đầu ván trên instance A.

| Người chơi nối vào | Sự kiện mong đợi | Nhận được | P50 | P95 |
|---|---:|---:|---:|---:|
| A (8080) — cùng instance với host | 60 | **60** | 38 ms | 41 ms |
| B (8081) — instance khác | 60 | **60** | 40 ms | 42 ms |

Hai tiến trình JVM này không có kênh liên lạc nào khác: broker của Spring nằm trong bộ nhớ từng
instance, còn PostgreSQL không phải kênh nhắn tin. Người chơi bên B nhận **đủ 60/60** sự kiện do
instance A phát ⇒ Redis Pub/Sub là con đường duy nhất có thể.

**Chi phí của khả năng mở rộng ngang: khoảng 2 ms** (40 ms so với 38 ms). Đi vòng qua Redis sang một
JVM khác gần như không đắt hơn ở lại ngay trong bộ nhớ instance phát.

## 6. Nhận xét

1. **Không mất sự kiện ở bất kỳ mức tải nào đã thử** (tới 200 người/phòng) — thiết kế phát tán đúng.
2. **Ngưỡng dùng được: 100 người/phòng**, P95 216 ms. Vượt 150 người thì công bằng của trò chơi bắt
   đầu bị ảnh hưởng.
3. **Nghẽn nằm ở kênh xử lý đáp án gửi lên**, không phải phát tán — đo tách ra mới thấy.
4. **Redis Pub/Sub cho khả năng chạy nhiều instance với giá ~2 ms**, và không có nó thì tính năng
   không tồn tại chứ không phải chậm đi.

## 7. Giới hạn của phép đo này

- Một máy đơn, không có độ trễ mạng — con số thực tế trên Internet sẽ cao hơn.
- Toàn bộ client chạy trong **một tiến trình Node**; ở mức 200 người, một phần độ trễ có thể là của
  chính công cụ đo. Lần chạy đầu từng báo "host không nối được" ở mức 100 — hoá ra do host nối sau
  cùng khi vòng lặp sự kiện của Node đã bận, không phải giới hạn máy chủ. Nối host trước là hết.
- Chưa đo với mạng thật, chưa đo nhiều phòng chạy song song, chưa đo mức tiêu thụ RAM/CPU.
