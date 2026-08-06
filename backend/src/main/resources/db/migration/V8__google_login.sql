-- V8: đăng nhập bằng Google (FR-3)
-- Đặc tả: docs/features/01-auth.md

-- `sub` của Google: định danh ổn định, KHÔNG đổi kể cả khi người dùng đổi địa chỉ Gmail.
-- Vì vậy đây mới là khoá liên kết tài khoản, không phải email.
ALTER TABLE users ADD COLUMN google_id VARCHAR(64);
CREATE UNIQUE INDEX uk_users_google_id ON users (google_id) WHERE google_id IS NOT NULL;

-- Tài khoản chỉ đăng nhập bằng Google thì không có mật khẩu nào để lưu.
-- Đặt mật khẩu ngẫu nhiên cho họ là tệ hơn: nó tạo ra một lối vào bằng mật khẩu mà
-- chính chủ không biết, và làm luồng "quên mật khẩu" hoạt động sai ngữ nghĩa.
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Mỗi tài khoản phải vào được bằng ít nhất một cách
ALTER TABLE users ADD CONSTRAINT ck_users_login_method
    CHECK (password_hash IS NOT NULL OR google_id IS NOT NULL);

COMMENT ON COLUMN users.google_id IS
    'Google subject id (sub). NULL = tài khoản chỉ dùng email + mật khẩu.';
COMMENT ON COLUMN users.password_hash IS
    'NULL = tài khoản chỉ đăng nhập bằng Google, chưa từng đặt mật khẩu.';
