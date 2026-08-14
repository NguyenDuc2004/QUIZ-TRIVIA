-- ============================================================
--  V12 — Khoá tài khoản người dùng (features/10 — Quản trị)
-- ============================================================
-- Quản trị viên cần chặn được một tài khoản mà KHÔNG xoá dữ liệu của họ: bài đã làm, quiz đã soạn,
-- học liệu đã nạp đều là dữ liệu người khác đang dùng hoặc đang được thống kê. Xoá tài khoản kéo theo
-- xoá hoặc mồ côi những thứ đó, nên biện pháp đúng là chặn đường vào, không phải xoá người.

ALTER TABLE users ADD COLUMN locked BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN users.locked IS
    'true = tài khoản bị quản trị viên khoá: không đăng nhập được và mọi phiên hiện có bị thu hồi. '
    'Dữ liệu của họ giữ nguyên — khoá là chặn truy cập, không phải xoá người dùng.';

-- Chỉ mục một phần trên đúng số ít tài khoản bị khoá. Trang quản trị cần lọc nhanh nhóm này, còn
-- phần lớn bản ghi có locked = false nên đánh chỉ mục toàn bảng chỉ phình kích thước mà không giúp gì.
CREATE INDEX idx_users_locked ON users (id) WHERE locked = true;
