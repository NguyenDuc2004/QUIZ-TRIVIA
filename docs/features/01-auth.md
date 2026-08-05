# 01 — Xác thực & Phân quyền

**Ưu tiên:** [M] Must

## Mục tiêu
Cho phép người dùng đăng ký, đăng nhập an toàn và phân quyền truy cập theo vai trò (RBAC).

## Use case
- Guest đăng ký tài khoản, đăng nhập.
- Người dùng đổi mật khẩu, khôi phục khi quên.
- Hệ thống phân quyền Learner / Creator / Admin.

## Yêu cầu chức năng
- **FR-1** [M] ✅ Đăng ký bằng email + mật khẩu (email chuẩn hóa chữ thường, băm BCrypt). Xác thực email: chưa làm (tùy chọn).
- **FR-2** [M] ✅ Đăng nhập/đăng xuất; Access Token (JWT HS256, 15 phút) + Refresh Token (Redis, 14 ngày, có rotation).
- **FR-3** [S] ⏳ Đăng nhập qua OAuth2 (Google) — chưa làm, mức Should.
- **FR-4** [M] ⏳ Quên/đặt lại mật khẩu qua email — **chưa làm vì cần cấu hình SMTP** (chọn nhà cung cấp mail + biến môi trường).
- **FR-5** [M] ✅ Quản lý hồ sơ: `PUT /users/me` (tên, avatar) + `POST /auth/change-password`.
- **FR-6** [M] ✅ Phân quyền theo vai trò: enum LEARNER/CREATOR/ADMIN trong token, `@EnableMethodSecurity` cho `@PreAuthorize`; tự đăng ký ADMIN bị hạ xuống LEARNER.

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

## Quy tắc truy cập cho Guest (chưa đăng nhập)

`SecurityConfig` chỉ `permitAll` đúng các đường dẫn sau, **mọi thứ còn lại `authenticated()`**:

```
POST /api/v1/auth/register, /login, /refresh, /forgot-password, /reset-password
GET  /api/v1/quizzes, /api/v1/quizzes/{id}      (chỉ bản ghi visibility = public)
GET  /v3/api-docs/**, /swagger-ui/**            (tài liệu API, môi trường dev)
```

- **Guest không được làm bài**: `POST /quizzes/{id}/attempts` và toàn bộ `/attempts/**` yêu cầu đăng nhập → trả **401**.
- `GET /quizzes/{id}` với Guest **không kèm danh sách câu hỏi** (tránh lộ đề); chỉ trả tiêu đề, mô tả, danh mục, độ khó, số câu, thời lượng.
- Quiz `visibility = private` với Guest trả **404** (không phải 403) để không lộ sự tồn tại của tài nguyên.
- WebSocket `/ws`: xác thực JWT ngay tại handshake, Guest bị từ chối kết nối.
