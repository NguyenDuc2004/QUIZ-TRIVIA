# 13 — Gamification (Huy hiệu, Streak, XP/Level, Daily Challenge)

**Ưu tiên:** [S] Should · **Tận dụng:** dữ liệu hành vi, ít hạ tầng thêm

## Mục tiêu
Tăng gắn kết và động lực học tập bằng cơ chế trò chơi hóa: điểm kinh nghiệm (XP), cấp độ (level), huy hiệu (badge), chuỗi ngày học (streak) và thử thách hằng ngày (daily challenge). Đồng thời tạo dữ liệu đẹp cho phân tích.

## Use case
- Learner nhận XP khi hoàn thành quiz/ôn flashcard/thắng phòng đấu → lên level.
- Learner mở khóa huy hiệu khi đạt mốc (ví dụ "10 quiz hoàn hảo", "streak 7 ngày").
- Learner duy trì streak bằng cách học mỗi ngày.
- Learner làm daily challenge để nhận thưởng XP.

## Yêu cầu chức năng
- **FR-49** [S] ✅ Cộng **XP** theo hành động (hoàn thành quiz, điểm cao, thắng phòng đấu, ôn flashcard đến hạn); quy đổi **level** theo ngưỡng XP.
- **FR-50** [S] ✅ **Huy hiệu (badge):** định nghĩa điều kiện; tự trao khi người dùng đạt.
- **FR-51** [S] ✅ **Streak:** đếm chuỗi ngày liên tiếp có hoạt động học; reset khi bỏ lỡ; hiển thị streak hiện tại/dài nhất.
- **FR-52** [S] ✅ **Daily challenge:** mỗi ngày một nhiệm vụ (ví dụ "làm 3 quiz chủ đề X") + thưởng XP khi hoàn thành.
- **FR-53** [C] ⏳ Hiệu ứng/thông báo khi lên level, mở khóa huy hiệu — cần tính năng 16 (thông báo).

## Ba quyết định của bản này

| Quyết định | Vì sao |
|---|---|
| **Thêm bảng `xp_events`** (không có trong đặc tả) | Đặc tả yêu cầu *"idempotent: một hành động chỉ cộng XP một lần"* nhưng không có bảng nào giữ được điều đó — cộng thẳng vào `user_stats.total_xp` thì không biết một hành động đã tính chưa. Ràng buộc `UNIQUE (user_id, source_type, source_key)` để **cơ sở dữ liệu** chặn, vì kiểm trong Java thua cuộc khi hai luồng chạy song song |
| **Khoá XP của ôn thẻ gồm cả ngày** (`cardId:ngày`) | API ôn không chặn ôn sớm, nên không giới hạn thì bấm một thẻ trăm lần là trăm lần XP. Ghép ngày vào biến nó thành "mỗi thẻ mỗi ngày một lần" — thưởng người ôn đều, không thưởng người bấm liên tục |
| **Chỉ có endpoint đọc** | XP chỉ đến từ hành động học thật qua domain event. Mở một đường ghi qua API là mở đường tự cộng điểm, và khi đó cả huy hiệu lẫn bảng xếp hạng đều mất ý nghĩa |

**Công thức cấp độ đọc theo nghĩa "XP cho từng bậc", không phải "XP tích luỹ".** Đặc tả gợi ý
`xp_needed(level) = 100 * level^1.5`. Hiểu là XP tích luỹ thì sinh ra chỗ ngược: lên cấp 2 tốn 283 XP nhưng
cấp 3 chỉ tốn thêm 237 — cấp thứ hai *khó hơn* cấp thứ ba. Hiểu là XP cho từng bậc thì ngưỡng tăng đều:
100 → 283 → 520 → 800 → 1118.

**Streak reset về 1, không phải 0.** Hôm nay người ta vừa học nên chuỗi hiện tại đúng là một ngày; reset về 0
làm màn hình hiện "chuỗi 0 ngày" ngay sau khi họ vừa làm xong một bài. Chuỗi dài nhất không bao giờ giảm —
đó là thành tích, không phải trạng thái.

**Thử thách ngày sinh khi cần, không cần bộ hẹn giờ.** Job nửa đêm là thêm một thứ có thể chết âm thầm, và
nếu chết thì cả ngày không ai có thử thách. Tạo ở lần đầu có người hỏi tới, dùng `ON CONFLICT DO NOTHING` để
hai người mở trang cùng lúc không ai bị lỗi.

## Luồng xử lý
```
Sau mỗi hành động (attempt submit, flashcard review, game finished)
   → phát domain event → GamificationService
   → cộng XP, cập nhật level, kiểm tra điều kiện badge, cập nhật streak, tiến độ daily challenge
   → (nếu đạt) trao badge / hoàn thành challenge → thông báo (features/16)
```

## Cơ chế gợi ý
- **Level:** ngưỡng lũy tiến, ví dụ `xp_needed(level) = 100 * level^1.5`.
- **Streak:** so `last_active_date` với hôm nay: cùng ngày → giữ; hôm qua → +1; xa hơn → reset.
- **Badge:** cấu hình dạng dữ liệu (điều kiện + ngưỡng) để dễ thêm mới, không hardcode.

## API liên quan
```
GET    /api/v1/gamification/me            Tổng quan: XP, level, streak, huy hiệu đã đạt
GET    /api/v1/gamification/badges        Danh sách huy hiệu (đã/chưa mở khóa)
GET    /api/v1/gamification/daily         Daily challenge hôm nay + tiến độ
```

## Dữ liệu liên quan (bổ sung PostgreSQL)
- `user_stats(user_id PK, total_xp, level, current_streak, longest_streak, last_active_date)`
- `badges(id, code, name, description, condition jsonb, icon)`
- `user_badges(id, user_id, badge_id, earned_at)`
- `daily_challenges(id, date, description, rule jsonb, xp_reward)`
- `user_daily_progress(id, user_id, challenge_id, progress, completed_at)`

## Ghi chú kỹ thuật
- Cộng XP qua **domain event** để tách khỏi logic nghiệp vụ chính (không nhồi vào service quiz/attempt).
- Idempotent: một hành động chỉ cộng XP một lần (chống lặp khi retry).
- Dữ liệu này nuôi thêm cho **bảng xếp hạng theo mùa** ([features/15](15-seasonal-leaderboard.md)).
