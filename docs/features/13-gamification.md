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
- **FR-49** [S] Cộng **XP** theo hành động (hoàn thành quiz, điểm cao, thắng phòng đấu, ôn flashcard đến hạn); quy đổi **level** theo ngưỡng XP.
- **FR-50** [S] **Huy hiệu (badge):** định nghĩa điều kiện; tự trao khi người dùng đạt.
- **FR-51** [S] **Streak:** đếm chuỗi ngày liên tiếp có hoạt động học; reset khi bỏ lỡ; hiển thị streak hiện tại/dài nhất.
- **FR-52** [S] **Daily challenge:** mỗi ngày một nhiệm vụ (ví dụ "làm 3 quiz chủ đề X") + thưởng XP khi hoàn thành.
- **FR-53** [C] Hiệu ứng/thông báo khi lên level, mở khóa huy hiệu.

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
