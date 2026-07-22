# 04 — Phòng đấu trí thời gian thực (Multiplayer)

**Ưu tiên:** [M] Must · **Trụ cột phiếu:** Real-time độ trễ thấp (Spring WebSocket + Redis)

## Mục tiêu
Cho phép nhiều người chơi cùng tham gia một phòng đấu quiz thời gian thực, đồng bộ chính xác, độ trễ thấp — tương tự Kahoot.

## Use case
- Host tạo phòng từ một quiz, chia sẻ mã phòng.
- Người chơi vào phòng bằng mã, chờ ở phòng chờ.
- Host bắt đầu; mọi người nhận cùng câu hỏi đồng thời, trả lời, thấy bảng xếp hạng trực tiếp.

## Yêu cầu chức năng
- **FR-20** [M] Tạo/tham gia phòng bằng room code; host điều khiển bắt đầu/kết thúc.
- **FR-21** [M] Đồng bộ real-time qua **Spring WebSocket (STOMP)**: người vào/ra, câu hiện tại, đếm ngược, kết quả.
- **FR-22** [M] Mọi người nhận cùng câu hỏi đồng thời; tính điểm theo độ chính xác + tốc độ trả lời.
- **FR-23** [M] **Live leaderboard** cập nhật sau mỗi câu, độ trễ thấp.
- **FR-24** [M] Dùng **Redis** lưu trạng thái phòng + Pub/Sub đồng bộ giữa các instance backend.
- **FR-25** [S] Xử lý mất kết nối / kết nối lại (reconnect).

## Luồng xử lý
```
Host tạo phòng → game_rooms (Postgres) + room:{code} (Redis, TTL)
Người chơi join → WebSocket handshake (xác thực JWT) → subscribe /topic/room/{code}
   → server phát PLAYER_JOINED
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

## Ghi chú kỹ thuật
- **Redis Pub/Sub** để khi backend chạy nhiều instance, message phòng vẫn đồng bộ tới đúng người chơi.
- Tính điểm theo tốc độ: điểm cao hơn nếu trả lời đúng nhanh.
- Trạng thái live nằm ở Redis (không ở DB quan hệ) để giảm độ trễ.
- Là đối tượng chính của **kiểm thử chịu tải** — xem [roadmap.md](../roadmap.md) mục 2.2.
