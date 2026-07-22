# 15 — Bảng xếp hạng theo mùa (Seasonal Leaderboard) & Phần thưởng ảo

**Ưu tiên:** [S] Should · **Tận dụng:** gamification (XP), Redis

## Mục tiêu
Tạo động lực cạnh tranh theo chu kỳ: xếp hạng người dùng theo điểm/XP trong một **mùa (season)** có thời hạn, trao **phần thưởng ảo** (huy hiệu mùa, danh hiệu) cho top người chơi khi mùa kết thúc.

## Use case
- Learner xem thứ hạng của mình trong mùa hiện tại (toàn hệ thống / theo lớp / theo bạn bè).
- Cuối mùa, top người chơi nhận huy hiệu mùa & danh hiệu; bảng được lưu lại (archive) và reset cho mùa mới.

## Yêu cầu chức năng
- **FR-60** [S] Quản lý **mùa** (season) có thời gian bắt đầu/kết thúc.
- **FR-61** [S] Tính điểm mùa (season points) từ hoạt động trong khoảng thời gian mùa (XP kiếm được, thắng phòng đấu...).
- **FR-62** [S] **Bảng xếp hạng theo mùa** với phạm vi: toàn hệ thống, theo lớp học, theo bạn bè (nếu có).
- **FR-63** [S] Kết thúc mùa: chốt bảng, **trao phần thưởng ảo** cho top N, lưu lịch sử, reset điểm mùa.
- **FR-64** [C] Thứ hạng theo phân hạng (bronze/silver/gold...) theo ngưỡng điểm.

## Luồng xử lý
```
Hoạt động của user (kiếm XP) → cộng vào điểm mùa hiện tại
Bảng xếp hạng đọc từ Redis Sorted Set (ZSET) → truy vấn top N & rank cá nhân nhanh
Job nền tại thời điểm kết thúc mùa:
   → chốt ZSET → trao season badge/danh hiệu cho top N (user_badges)
   → lưu season_rankings (archive) → tạo mùa mới → reset ZSET
```

## API liên quan
```
GET    /api/v1/leaderboard/season/current       BXH mùa hiện tại (scope: global/class/friends)
GET    /api/v1/leaderboard/season/current/me     Thứ hạng của tôi
GET    /api/v1/leaderboard/season/history        Lịch sử các mùa
```

## Dữ liệu liên quan
- Redis: `leaderboard:season:{seasonId}` — **Sorted Set** (member=userId, score=điểm mùa) để xếp hạng độ trễ thấp.
- PostgreSQL:
  - `seasons(id, name, start_at, end_at, status: active/ended)`
  - `season_rankings(id, season_id, user_id, final_score, final_rank, reward_badge_id)` *(archive sau khi mùa kết thúc)*

## Ghi chú kỹ thuật
- Dùng **Redis Sorted Set** cho bảng xếp hạng (thao tác `ZADD`, `ZREVRANK`, `ZREVRANGE`) — hiệu năng cao, đúng use case.
- Điểm mùa lấy từ [features/13-gamification.md](13-gamification.md) (đồng bộ khi cộng XP).
- Chốt mùa bằng job nền (scheduler); đảm bảo idempotent nếu chạy lại.
- Phần thưởng ảo là badge/danh hiệu — không liên quan tiền thật.
