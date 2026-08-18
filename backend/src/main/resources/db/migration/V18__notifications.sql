-- ============================================================
--  V18 — Thông báo & nhắc ôn tập (features/16)
-- ============================================================
-- Hai bảng: thông báo, và cài đặt tắt/bật theo loại.
--
-- Ràng buộc định hình cả hai bảng: **không được gửi trùng**. Job nhắc ôn tập chạy hằng ngày, và một job
-- hằng ngày thì sớm muộn cũng chạy hai lần — deploy lại giữa trưa, hai instance cùng thức, hoặc ai đó gọi
-- tay để thử. Không có chốt thì người dùng nhận đúng một câu nhắc ba lần, và đó là kiểu lỗi làm người ta
-- tắt thông báo vĩnh viễn.

CREATE TABLE notifications (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       TEXT,
    -- Dữ liệu để giao diện điều hướng: {"deckId": "..."} hoặc {"badgeCode": "..."}. KHÔNG nhồi cả nội
    -- dung vào đây — `title`/`body` là thứ người dùng đọc, `data` là thứ nút bấm cần.
    data       JSONB,
    is_read    BOOLEAN      NOT NULL DEFAULT false,
    /*
     * Khoá chống trùng — bài học nguyên xi từ `xp_events` ở V15.
     *
     * Ví dụ: `srs:2026-08-18` (nhắc ôn của ngày đó), `badge:PERFECT_1`, `level:5`. Ràng buộc duy nhất
     * dưới đây chặn ở **cơ sở dữ liệu**, không chặn trong Java: kiểm trong Java thua cuộc khi hai
     * instance cùng thức dậy đúng nửa đêm, mà đó chính là tình huống sẽ xảy ra.
     *
     * NULL cho thông báo không cần chống trùng (ví dụ thông báo hệ thống gửi tay). Ràng buộc UNIQUE của
     * PostgreSQL coi mỗi NULL là một giá trị khác nhau, nên nhiều dòng NULL cùng tồn tại được — đúng
     * điều cần, không phải may mắn.
     */
    dedupe_key VARCHAR(100),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_notifications_dedupe UNIQUE (user_id, dedupe_key),
    CONSTRAINT ck_notifications_type CHECK (type IN (
        'SRS_REMINDER',    -- có thẻ ghi nhớ đến hạn ôn (features/11)
        'ACHIEVEMENT',     -- lên cấp hoặc mở khoá huy hiệu (features/13, FR-53)
        'ASSIGNMENT_DUE',  -- bài tập sắp hết hạn — chờ features/14
        'ROOM_INVITE',     -- lời mời vào phòng đấu — chờ tính năng mời của features/04
        'SYSTEM'           -- thông báo hệ thống
    ))
);

-- Truy vấn nóng: trung tâm thông báo của một người, mới nhất trước.
CREATE INDEX idx_notifications_user ON notifications (user_id, created_at DESC);

-- Số chưa đọc hiện trên chuông ở mọi trang, nên nó là truy vấn chạy nhiều nhất của cả tính năng.
-- Chỉ mục một phần: chỉ đánh dấu dòng chưa đọc, và đó cũng là phần nhỏ dần theo thời gian.
CREATE INDEX idx_notifications_unread ON notifications (user_id) WHERE is_read = false;

/*
 * Cài đặt: lưu **danh sách loại bị tắt** thay vì một cột boolean cho mỗi loại.
 *
 * Đặc tả gợi ý `notification_settings(user_id, srs_reminder, assignment_due, achievement, ...)` — mỗi loại
 * một cột. Bỏ cách đó vì thêm một loại thông báo mới sẽ cần một migration, và tính năng 14 (lớp học) chắc
 * chắn sẽ thêm loại. Mảng jsonb thì thêm loại không cần đụng schema.
 *
 * Mặc định `[]` = bật tất cả. Chọn mặc định bật vì người dùng chưa từng vào trang cài đặt thì vẫn nên nhận
 * nhắc ôn — đó là lý do tính năng này tồn tại; mặc định tắt thì nó chỉ chạy cho người đi tìm nó.
 */
CREATE TABLE notification_settings (
    user_id        UUID        PRIMARY KEY,
    disabled_types JSONB       NOT NULL DEFAULT '[]',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_notification_settings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

COMMENT ON COLUMN notifications.dedupe_key IS
    'Khoá chống gửi trùng, cùng cơ chế với xp_events. Ràng buộc UNIQUE (user_id, dedupe_key) chặn ở CSDL '
    'vì job hằng ngày sớm muộn cũng chạy hai lần. NULL khi không cần chống trùng.';

COMMENT ON TABLE notification_settings IS
    'Loại thông báo BỊ TẮT, dạng mảng jsonb. Không dùng một cột cho mỗi loại vì thêm loại mới sẽ cần '
    'migration. Mặc định rỗng = bật tất cả.';
