# 10 — Quản trị (Admin)

**Ưu tiên:** [S] Should

## Mục tiêu
Cho phép quản trị viên quản lý người dùng, nội dung, cấu hình AI và giám sát hệ thống.

## Use case
- Admin quản lý user, đổi vai trò, kiểm duyệt nội dung.
- Admin cấu hình AI provider/fallback và theo dõi chi phí.

## Yêu cầu chức năng
- Quản lý người dùng (khóa/mở, đổi vai trò).
- Kiểm duyệt quiz/câu hỏi công khai.
- Cấu hình thứ tự AI provider (gemini/grok) và hạn mức.
- Xem log & chi phí AI (token, provider đã dùng, độ trễ, tỉ lệ lỗi/fallback).

## Luồng xử lý
- Admin xem `ai_request_logs` để theo dõi chi phí & tần suất fallback.
- Điều chỉnh cấu hình AI runtime (override `application.yml`).

## API liên quan
[api.md](../api.md) mục 9 (`/admin/*`).

## Dữ liệu liên quan
`users`, `ai_request_logs` — [database.md](../database.md).

## Ghi chú kỹ thuật
- Chỉ vai trò ADMIN truy cập; bảo vệ bằng `@PreAuthorize("hasRole('ADMIN')")`.
- Không hiển thị API key trong UI/log.
