---
name: websocket-room
description: Dùng khi hiện thực phòng đấu real-time — Spring WebSocket (STOMP) + Redis Pub/Sub, đồng bộ trạng thái phòng, sự kiện game, live leaderboard, tính điểm theo tốc độ.
---

# Phòng đấu real-time (WebSocket + Redis)

## Thành phần
- `WebSocketConfig` — endpoint `/ws` (STOMP), message broker; interceptor xác thực **JWT khi handshake**.
- `RoomController` (REST) — tạo/join/thông tin phòng.
- `GameEngine` (service) — điều phối ván: phát câu hỏi, chấm, cập nhật điểm.
- Redis — `room:{code}` (trạng thái, TTL) + `room:{code}:events` (Pub/Sub đồng bộ đa instance).

## Kênh STOMP
```
SUBSCRIBE /topic/room/{code}          nhận sự kiện phòng
SEND      /app/room/{code}/start      host bắt đầu
SEND      /app/room/{code}/answer     { questionId, answer, timeMs }
SEND      /app/room/{code}/next       host chuyển câu
```

## Sự kiện server phát (payload có `type`)
`PLAYER_JOINED` · `GAME_STARTED` · `QUESTION` · `ANSWER_RESULT` · `LEADERBOARD` · `GAME_FINISHED`

## Quy tắc
1. **Trạng thái live ở Redis**, KHÔNG ở DB quan hệ (giảm độ trễ). Postgres chỉ lưu `game_rooms`, `game_room_players` (kết quả cuối).
2. **Redis Pub/Sub** để đồng bộ khi chạy nhiều instance backend.
3. **Đồng thời:** mọi người nhận `QUESTION` cùng lúc; server quản đếm ngược.
4. **Điểm theo tốc độ:** đúng + nhanh (`timeMs` nhỏ) → điểm cao hơn.
5. **Kiểm quyền** vào phòng trước khi cho subscribe.
6. **Reconnect:** khôi phục trạng thái từ Redis.

## Checklist
- [ ] JWT xác thực ở handshake.
- [ ] Trạng thái live ở Redis (có TTL).
- [ ] Dùng Pub/Sub, không giữ state trong bộ nhớ instance.
- [ ] Payload mỗi `type` định nghĩa rõ.
- [ ] Có kịch bản test tải (k6/Gatling WebSocket).

## Tham chiếu
`docs/features/04-multiplayer-realtime.md`, `docs/api.md` mục 5, `docs/database.md` mục 3.
