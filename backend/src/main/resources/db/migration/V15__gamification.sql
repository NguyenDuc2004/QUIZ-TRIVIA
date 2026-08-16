-- ============================================================
--  V15 — Gamification: XP, level, streak, huy hiệu, thử thách ngày (features/13)
-- ============================================================
-- Sáu bảng. Năm bảng đầu đúng như đặc tả; bảng `xp_events` là bảng THÊM, và nó là bảng quan trọng nhất —
-- xem ghi chú ở dưới.

-- ------------------------------------------------------------------ 1. Chỉ số của người dùng
CREATE TABLE user_stats (
    -- user_id là khoá chính, không có cột id riêng: một người có đúng một dòng chỉ số.
    user_id          UUID        PRIMARY KEY,
    total_xp         INTEGER     NOT NULL DEFAULT 0,
    level            INTEGER     NOT NULL DEFAULT 1,
    current_streak   INTEGER     NOT NULL DEFAULT 0,
    longest_streak   INTEGER     NOT NULL DEFAULT 0,
    -- Ngày (không giờ): streak tính theo ngày nên so sánh "hôm nay / hôm qua" không phụ thuộc giờ.
    last_active_date DATE,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_stats_user  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_stats_xp    CHECK (total_xp >= 0),
    CONSTRAINT ck_user_stats_level CHECK (level >= 1),
    CONSTRAINT ck_user_stats_streak CHECK (current_streak >= 0 AND longest_streak >= current_streak)
);

-- Bảng xếp hạng và trang tổng quan đều xếp theo XP giảm dần.
CREATE INDEX idx_user_stats_xp ON user_stats (total_xp DESC);

-- ------------------------------------------------------------------ 2. Sổ ghi XP (bảng THÊM)
-- Đặc tả yêu cầu "idempotent: một hành động chỉ cộng XP một lần (chống lặp khi retry)", nhưng không có
-- bảng nào giữ được điều đó — cộng thẳng vào `user_stats.total_xp` thì không có cách nào biết một hành
-- động đã được tính chưa. Nên phải có sổ ghi từng lần cộng, và để cơ sở dữ liệu tự chặn trùng bằng ràng
-- buộc UNIQUE thay vì kiểm trong Java (kiểm trong Java thua cuộc khi hai luồng chạy song song).
CREATE TABLE xp_events (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    -- Khoá tự nhiên của hành động, dạng chuỗi để ghép được nhiều thành phần.
    --   ATTEMPT_SUBMITTED  → "<attemptId>"
    --   FLASHCARD_REVIEW   → "<cardId>:<ngày>"  ← xem lý do ở dưới
    --   ROOM_FINISHED      → "<roomId>"
    source_key  VARCHAR(120) NOT NULL,
    xp          INTEGER     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_xp_events_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_xp_events_xp   CHECK (xp > 0),
    -- Đây là chốt chống cộng trùng. Ôn lại một thẻ nhiều lần trong ngày là hợp lệ về mặt học tập (API ôn
    -- không chặn ôn sớm), nhưng nếu mỗi lần đều cộng XP thì bấm một thẻ trăm lần là trăm lần XP — ràng
    -- buộc này biến việc đó thành "một thẻ mỗi ngày một lần cộng".
    CONSTRAINT uk_xp_events      UNIQUE (user_id, source_type, source_key)
);

CREATE INDEX idx_xp_events_user ON xp_events (user_id, created_at DESC);

-- ------------------------------------------------------------------ 3. Huy hiệu
CREATE TABLE badges (
    id          UUID         PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    -- Điều kiện dạng dữ liệu, KHÔNG hardcode trong Java: {"type":"XP","threshold":1000}.
    -- Thêm huy hiệu mới chỉ cần thêm một dòng, miễn là `type` đã được hỗ trợ.
    condition   JSONB        NOT NULL,
    icon        VARCHAR(20),
    -- Thứ tự hiển thị: huy hiệu dễ trước, khó sau, để trang huy hiệu đọc thành một lộ trình
    sort_order  INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE user_badges (
    id        UUID        PRIMARY KEY,
    user_id   UUID        NOT NULL,
    badge_id  UUID        NOT NULL,
    earned_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_user_badges_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT fk_user_badges_badge FOREIGN KEY (badge_id) REFERENCES badges (id) ON DELETE CASCADE,
    -- Một huy hiệu trao đúng một lần cho một người
    CONSTRAINT uk_user_badges       UNIQUE (user_id, badge_id)
);

CREATE INDEX idx_user_badges_user ON user_badges (user_id, earned_at DESC);

-- ------------------------------------------------------------------ 4. Thử thách hằng ngày
CREATE TABLE daily_challenges (
    -- Một thử thách cho mỗi ngày, dùng chung cho mọi người: đơn giản hơn sinh riêng cho từng người, và
    -- tạo được cảm giác "hôm nay cả nhà cùng làm việc này".
    id          UUID         PRIMARY KEY,
    challenge_date DATE      NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL,
    -- {"type":"COMPLETE_ATTEMPTS","target":3} — cùng cách làm như `badges.condition`
    rule        JSONB        NOT NULL,
    xp_reward   INTEGER      NOT NULL,

    CONSTRAINT ck_daily_challenges_reward CHECK (xp_reward > 0)
);

CREATE TABLE user_daily_progress (
    id           UUID        PRIMARY KEY,
    user_id      UUID        NOT NULL,
    challenge_id UUID        NOT NULL,
    progress     INTEGER     NOT NULL DEFAULT 0,
    completed_at TIMESTAMPTZ,

    CONSTRAINT fk_user_daily_user      FOREIGN KEY (user_id)      REFERENCES users (id)            ON DELETE CASCADE,
    CONSTRAINT fk_user_daily_challenge FOREIGN KEY (challenge_id) REFERENCES daily_challenges (id) ON DELETE CASCADE,
    CONSTRAINT uk_user_daily           UNIQUE (user_id, challenge_id),
    CONSTRAINT ck_user_daily_progress  CHECK (progress >= 0)
);

-- ------------------------------------------------------------------ 5. Huy hiệu ban đầu
-- Bốn loại điều kiện được hỗ trợ, mỗi loại đo một mặt khác nhau của việc học — không phải bốn biến thể của
-- cùng một con số:
--   XP                  → tổng lượng đã học
--   STREAK              → tính đều đặn
--   PERFECT_ATTEMPTS    → chất lượng (bài làm đúng 100%)
--   FLASHCARDS_MASTERED → ghi nhớ dài hạn (thẻ có khoảng ôn ≥ 21 ngày)
INSERT INTO badges (id, code, name, description, condition, icon, sort_order) VALUES
  (gen_random_uuid(), 'FIRST_STEPS',   'Bước đầu',        'Đạt 50 XP đầu tiên',
   '{"type":"XP","threshold":50}',                     '🌱', 10),
  (gen_random_uuid(), 'XP_500',        'Học đều',         'Đạt 500 XP',
   '{"type":"XP","threshold":500}',                    '📘', 20),
  (gen_random_uuid(), 'XP_2000',       'Bền bỉ',          'Đạt 2000 XP',
   '{"type":"XP","threshold":2000}',                   '🏅', 30),
  (gen_random_uuid(), 'STREAK_3',      'Ba ngày liền',    'Học 3 ngày liên tiếp',
   '{"type":"STREAK","threshold":3}',                  '🔥', 40),
  (gen_random_uuid(), 'STREAK_7',      'Một tuần không nghỉ', 'Học 7 ngày liên tiếp',
   '{"type":"STREAK","threshold":7}',                  '🔥', 50),
  (gen_random_uuid(), 'STREAK_30',     'Một tháng đều đặn',   'Học 30 ngày liên tiếp',
   '{"type":"STREAK","threshold":30}',                 '💎', 60),
  (gen_random_uuid(), 'PERFECT_1',     'Điểm tuyệt đối',  'Làm đúng 100% một bài quiz',
   '{"type":"PERFECT_ATTEMPTS","threshold":1}',        '🎯', 70),
  (gen_random_uuid(), 'PERFECT_10',    'Mười lần hoàn hảo', 'Làm đúng 100% mười bài quiz',
   '{"type":"PERFECT_ATTEMPTS","threshold":10}',       '🎯', 80),
  (gen_random_uuid(), 'MASTER_20',     'Nhớ lâu',         'Có 20 thẻ ghi nhớ đã thuộc',
   '{"type":"FLASHCARDS_MASTERED","threshold":20}',    '🧠', 90),
  (gen_random_uuid(), 'MASTER_100',    'Kho kiến thức',   'Có 100 thẻ ghi nhớ đã thuộc',
   '{"type":"FLASHCARDS_MASTERED","threshold":100}',   '🧠', 100);

COMMENT ON TABLE xp_events IS
    'Sổ ghi từng lần cộng XP. Ràng buộc UNIQUE (user_id, source_type, source_key) là chốt chống cộng '
    'trùng khi có retry, và với ôn thẻ thì nó giới hạn mỗi thẻ một lần cộng mỗi ngày.';

COMMENT ON COLUMN badges.condition IS
    'Điều kiện dạng dữ liệu. type ∈ {XP, STREAK, PERFECT_ATTEMPTS, FLASHCARDS_MASTERED}, kèm threshold. '
    'Thêm huy hiệu mới bằng một dòng INSERT, không phải sửa Java.';
