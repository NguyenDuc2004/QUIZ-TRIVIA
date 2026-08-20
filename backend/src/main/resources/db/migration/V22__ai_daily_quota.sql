-- Hạn mức số lượt gọi AI mỗi ngày cho từng người (features/10, FR-84).
--
-- VÌ SAO LÀ MỘT CỘT TRÊN `users`, KHÔNG PHẢI BẢNG RIÊNG
--
-- Đây là một số nguyên gắn với đúng một người, không có lịch sử, không có nhiều dòng cho một người. Một
-- bảng riêng chỉ thêm một phép nối vào mọi truy vấn danh sách người dùng của khu quản trị.
--
-- NULL ≠ 0 — VÀ ĐÂY LÀ CHỖ DỄ HỎNG NHẤT
--
--   NULL = "chưa đặt riêng"  → dùng hạn mức mặc định của hệ thống (app.ai.default-daily-quota)
--   0    = "cấm gọi AI"      → một quyết định thật của quản trị viên
--
-- Gộp hai thứ này thì hoặc là không cấm được ai, hoặc là mọi người dùng mới bị cấm ngay từ lúc tạo tài
-- khoản. `DEFAULT NULL` chứ không phải `DEFAULT 0`.
--
-- VÌ SAO KHÔNG LƯU BỘ ĐẾM Ở ĐÂY
--
-- Bộ đếm tăng ở mỗi lời gọi AI. Ghi vào PostgreSQL nghĩa là một câu UPDATE cho mỗi lời gọi, trên đúng một
-- dòng mà nhiều luồng cùng tranh — chính là kiểu tải mà Redis sinh ra để nhận. Đếm nằm ở Redis
-- (`aiquota:{userId}:{ngày}`), và dựng lại được từ `ai_request_logs` khi Redis rỗng: cùng nguyên tắc
-- "PostgreSQL là nguồn sự thật, Redis là chỉ mục" đã dùng cho bảng xếp hạng mùa (V16).

ALTER TABLE users
    ADD COLUMN ai_daily_quota INTEGER;

ALTER TABLE users
    ADD CONSTRAINT ck_users_ai_daily_quota CHECK (ai_daily_quota IS NULL OR ai_daily_quota >= 0);

COMMENT ON COLUMN users.ai_daily_quota IS
    'FR-84: số lượt gọi AI tối đa mỗi ngày của riêng người này. NULL = dùng mặc định hệ thống; '
    '0 = cấm gọi AI. Bộ đếm nằm ở Redis, không nằm ở đây.';

-- Đếm lượt gọi trong ngày của một người khi Redis rỗng — dựng lại từ bảng audit đã có.
-- Không có chỉ mục này thì mỗi lần dựng lại là một lần quét toàn bảng.
CREATE INDEX idx_ai_request_logs_user_created ON ai_request_logs (user_id, created_at DESC);
