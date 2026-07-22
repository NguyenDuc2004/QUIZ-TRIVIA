# 01 — Xác thực & Phân quyền

**Ưu tiên:** [M] Must

## Mục tiêu
Cho phép người dùng đăng ký, đăng nhập an toàn và phân quyền truy cập theo vai trò (RBAC).

## Use case
- Guest đăng ký tài khoản, đăng nhập.
- Người dùng đổi mật khẩu, khôi phục khi quên.
- Hệ thống phân quyền Learner / Creator / Admin.

## Yêu cầu chức năng
- **FR-1** [M] Đăng ký bằng email + mật khẩu; xác thực email (tùy chọn).
- **FR-2** [M] Đăng nhập/đăng xuất; cấp Access Token (JWT) + Refresh Token.
- **FR-3** [S] Đăng nhập qua OAuth2 (Google).
- **FR-4** [M] Quên/đặt lại mật khẩu qua email.
- **FR-5** [M] Quản lý hồ sơ cá nhân (tên, avatar, mật khẩu).
- **FR-6** [M] Phân quyền theo vai trò (RBAC).

## Luồng xử lý (đăng nhập)
1. Người dùng gửi email + mật khẩu.
2. Server xác minh (BCrypt), cấp access token (15 phút) + refresh token.
3. Client lưu token, gửi kèm `Authorization: Bearer` ở các request sau.
4. Access token hết hạn → dùng refresh token để lấy token mới (rotation).

## API liên quan
Xem [api.md](../api.md) mục 1–2 (`/auth/*`, `/users/me`).

## Dữ liệu liên quan
Bảng `users` — xem [database.md](../database.md) mục 1.2.

## Ghi chú kỹ thuật
- Mật khẩu băm BCrypt; không lưu plaintext.
- Phân quyền controller bằng `@PreAuthorize`.
- Chi tiết bảo mật: [security.md](../security.md).
