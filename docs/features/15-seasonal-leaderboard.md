# 15 — Bảng xếp hạng theo mùa (Seasonal Leaderboard) & Phần thưởng ảo

**Ưu tiên:** [S] Should · **Tận dụng:** gamification (XP), Redis

## Mục tiêu
Tạo động lực cạnh tranh theo chu kỳ: xếp hạng người dùng theo điểm/XP trong một **mùa (season)** có thời hạn, trao **phần thưởng ảo** (huy hiệu mùa, danh hiệu) cho top người chơi khi mùa kết thúc.

## Use case
- Learner xem thứ hạng của mình trong mùa hiện tại (toàn hệ thống / theo lớp / theo bạn bè).
- Cuối mùa, top người chơi nhận huy hiệu mùa & danh hiệu; bảng được lưu lại (archive) và reset cho mùa mới.

## Yêu cầu chức năng
- **FR-60** [S] ✅ Quản lý **mùa** (season) có thời gian bắt đầu/kết thúc.
- **FR-61** [S] ✅ Tính điểm mùa (season points) từ hoạt động trong khoảng thời gian mùa (XP kiếm được, thắng phòng đấu...).
- **FR-62** [S] 🟡 **Bảng xếp hạng theo mùa** — làm phạm vi **toàn hệ thống**. Theo lớp cần tính năng 14 (chưa làm); *theo bạn bè* không tồn tại ở bất kỳ đâu trong docs.
- **FR-63** [S] ✅ Kết thúc mùa: chốt bảng, **trao phần thưởng ảo** cho top N, lưu lịch sử, reset điểm mùa.
- **FR-64** [C] ⏳ Phân hạng bronze/silver/gold — chưa làm. Cần chọn ngưỡng, mà chọn ngưỡng khi chưa có dữ liệu thật thì chỉ là số bịa.

## Ba quyết định của bản này

| Quyết định | Vì sao |
|---|---|
| **Redis là chỉ mục, PostgreSQL là nguồn sự thật** | Đặc tả gợi ý giữ điểm mùa trong ZSET. Nhưng Redis ở dự án này chạy không bật AOF — một lần restart mất dữ liệu là **mất sạch bảng xếp hạng, không dựng lại được**. Điểm mùa thật là `sum(xp_events.xp)` trong khoảng thời gian mùa; ZSET chỉ là bản sao để đọc nhanh, và tự dựng lại khi rỗng |
| **Chỉ phạm vi toàn hệ thống** | Theo lớp phụ thuộc tính năng 14 (chưa làm). *Theo bạn bè* **không tồn tại**: không bảng, không API, không yêu cầu chức năng nào trong toàn bộ docs. Thêm hai bộ lọc luôn trả cùng một danh sách là hứa với người dùng một thứ không có |
| **Chỉ có endpoint đọc** | Điểm mùa đến từ XP, XP đến từ hành động học thật (features/13). Mở đường ghi là mở đường tự leo hạng |

**Chốt mùa idempotent bằng bốn chốt**, không bằng một cờ trong bộ nhớ:
1. Chỉ chốt mùa `ACTIVE` **và** đã quá `end_at`; chốt xong thành `ENDED` nên lần sau không tìm thấy.
2. `season_rankings` có `UNIQUE (season_id, user_id)`.
3. `user_badges` có `UNIQUE (user_id, badge_id)`, kèm kiểm trước.
4. `uk_seasons_one_active` — chỉ mục một phần chặn hai mùa `ACTIVE` cùng tồn tại, kể cả khi hai tiến trình chạy job cùng lúc.

**Ba huy hiệu mùa dùng chung cho mọi mùa**, không tạo huy hiệu mới cho từng mùa: mùa nào cũng có top 1 nên
tạo riêng thì bảng `badges` phình theo thời gian, và danh sách huy hiệu trên giao diện đầy những cái không ai
còn cơ hội đạt. Mùa cụ thể đã ghi ở `season_rankings`.

**Mùa mới bắt đầu từ `end_at` của mùa cũ**, không từ `now()`: job quét mỗi giờ nên có thể chạy muộn vài chục
phút, và lấy `now()` thì XP kiếm trong khoảng trống đó không thuộc mùa nào.

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
