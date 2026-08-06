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
- **FR-4** [M] ✅ Quên/đặt lại mật khẩu bằng **mã OTP 6 chữ số gửi qua email** (Gmail SMTP + App Password).

### Quên mật khẩu qua OTP — bốn lớp bảo vệ

| Chặn kiểu tấn công gì | Cách làm |
|---|---|
| Dò danh sách người dùng | `forgot-password` **luôn trả 204**, dù email có tài khoản hay không |
| Đọc trộm Redis | OTP lưu **dạng băm BCrypt**, không lưu thô |
| Dò 6 chữ số | Sai quá **5 lần** thì huỷ mã, bắt xin lại |
| Bơm email vào hòm thư người khác | Giãn cách **60 giây** giữa hai lần xin mã (429) |

Thêm: mã sống 10 phút, chỉ dùng **một lần**, và đặt lại mật khẩu xong thì **thu hồi phiên trên mọi
thiết bị** — người vừa lấy lại tài khoản cần chắc kẻ chiếm dụng bị đá ra.

`reset-password` xác minh mã **trước** khi tra người dùng: làm ngược lại thì thời gian phản hồi giữa
"email không tồn tại" và "mã sai" khác nhau, đủ để dò email qua độ trễ.
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
