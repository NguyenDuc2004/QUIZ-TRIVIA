-- ============================================================
--  V16 — Bảng xếp hạng theo mùa (features/15)
-- ============================================================
-- Hai bảng ở PostgreSQL + một Sorted Set ở Redis.
--
-- Phân chia trách nhiệm, và đây là quyết định quan trọng nhất của lát cắt này:
--   PostgreSQL = NGUỒN SỰ THẬT. Điểm mùa của một người là tổng `xp_events.xp` trong khoảng thời gian mùa.
--   Redis ZSET = CHỈ MỤC NHANH để trả top N và thứ hạng cá nhân với độ trễ thấp.
--
-- Vì sao không để Redis giữ điểm như đặc tả gợi ý: Redis trong dự án này chạy không bật AOF, và một lần
-- restart mất dữ liệu là mất sạch bảng xếp hạng — không có cách nào dựng lại. Có `xp_events` (thêm ở V15)
-- thì ZSET dựng lại được bất cứ lúc nào bằng một câu SQL, nên mất Redis chỉ là chậm một lần, không phải
-- mất dữ liệu.

CREATE TABLE seasons (
    id       UUID         PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    start_at TIMESTAMPTZ  NOT NULL,
    end_at   TIMESTAMPTZ  NOT NULL,
    status   VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_seasons_status CHECK (status IN ('ACTIVE', 'ENDED')),
    CONSTRAINT ck_seasons_range  CHECK (end_at > start_at)
);

-- Chỉ được có MỘT mùa đang chạy. Chỉ mục một phần trên hằng số là cách bắt cơ sở dữ liệu tự bảo đảm điều
-- đó — kiểm trong Java thì hai lần chốt mùa chạy song song vẫn tạo được hai mùa mới.
CREATE UNIQUE INDEX uk_seasons_one_active ON seasons ((status)) WHERE status = 'ACTIVE';

CREATE INDEX idx_seasons_ended ON seasons (end_at DESC) WHERE status = 'ENDED';

CREATE TABLE season_rankings (
    id              UUID    PRIMARY KEY,
    season_id       UUID    NOT NULL,
    user_id         UUID    NOT NULL,
    final_score     INTEGER NOT NULL,
    final_rank      INTEGER NOT NULL,
    -- Huy hiệu được trao nhờ thứ hạng này; NULL với người ngoài top thưởng
    reward_badge_id UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_season_rankings_season FOREIGN KEY (season_id) REFERENCES seasons (id) ON DELETE CASCADE,
    -- ON DELETE CASCADE theo user: xoá tài khoản thì bảng lưu trữ của họ đi theo. Khác với quiz hay lượt
    -- làm bài — một dòng xếp hạng không có giá trị với ai khác ngoài chính người đó.
    CONSTRAINT fk_season_rankings_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE,
    CONSTRAINT fk_season_rankings_badge  FOREIGN KEY (reward_badge_id) REFERENCES badges (id) ON DELETE SET NULL,
    -- Một người có đúng một dòng trong một mùa. Đây cũng là chốt idempotent của việc chốt mùa: chạy lại
    -- job không tạo thêm dòng nào.
    CONSTRAINT uk_season_rankings        UNIQUE (season_id, user_id),
    CONSTRAINT ck_season_rankings_rank   CHECK (final_rank >= 1),
    CONSTRAINT ck_season_rankings_score  CHECK (final_score >= 0)
);

CREATE INDEX idx_season_rankings_user ON season_rankings (user_id, created_at DESC);
CREATE INDEX idx_season_rankings_top  ON season_rankings (season_id, final_rank);

-- ------------------------------------------------------------------ Huy hiệu mùa
-- Ba huy hiệu dùng chung cho MỌI mùa, không tạo dòng huy hiệu mới cho từng mùa: mùa nào cũng có top 1 nên
-- tạo riêng thì bảng `badges` phình theo thời gian, còn danh sách huy hiệu trên giao diện thì đầy những
-- cái không ai còn cơ hội đạt. Mùa cụ thể đã được ghi ở `season_rankings`.
--
-- condition = SEASON_RANK: loại điều kiện KHÔNG tự xét được bằng số liệu hiện tại như XP hay STREAK —
-- nó chỉ do việc chốt mùa trao. Ghi rõ để `GamificationService.datDieuKien` biết bỏ qua thay vì báo
-- "điều kiện không hỗ trợ" vào log mỗi lần có người nộp bài.
INSERT INTO badges (id, code, name, description, condition, icon, sort_order) VALUES
  (gen_random_uuid(), 'SEASON_TOP1',  'Quán quân mùa',  'Đứng đầu bảng xếp hạng một mùa',
   '{"type":"SEASON_RANK","threshold":1}',  '🥇', 200),
  (gen_random_uuid(), 'SEASON_TOP3',  'Top 3 mùa',      'Vào top 3 bảng xếp hạng một mùa',
   '{"type":"SEASON_RANK","threshold":3}',  '🥈', 210),
  (gen_random_uuid(), 'SEASON_TOP10', 'Top 10 mùa',     'Vào top 10 bảng xếp hạng một mùa',
   '{"type":"SEASON_RANK","threshold":10}', '🥉', 220);

-- ------------------------------------------------------------------ Mùa đầu tiên
-- Tạo sẵn một mùa đang chạy để hệ thống không có khoảng trống "chưa có mùa nào" ngay sau khi cài. Bắt đầu
-- từ đầu tháng hiện tại để XP người dùng đã kiếm trong tháng này được tính — nếu bắt đầu từ now() thì mọi
-- người vào bảng xếp hạng với 0 điểm và nó trống trơn suốt ngày đầu.
INSERT INTO seasons (id, name, start_at, end_at, status)
VALUES (
    gen_random_uuid(),
    'Mùa ' || to_char(now(), 'MM/YYYY'),
    date_trunc('month', now()),
    date_trunc('month', now()) + interval '1 month',
    'ACTIVE'
);

COMMENT ON TABLE seasons IS
    'Mùa giải. Chỉ mục một phần uk_seasons_one_active bảo đảm không bao giờ có hai mùa ACTIVE cùng lúc.';

COMMENT ON TABLE season_rankings IS
    'Bảng lưu trữ sau khi mùa kết thúc. UNIQUE (season_id, user_id) là chốt idempotent của việc chốt mùa: '
    'chạy lại job không tạo thêm dòng nào.';
