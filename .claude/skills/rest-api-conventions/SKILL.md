---
name: rest-api-conventions
description: Dùng khi thiết kế hoặc rà soát REST endpoint trong dự án — đảm bảo prefix, mã trạng thái, response lỗi chuẩn, phân trang và xác thực nhất quán.
---

# Quy ước REST API

## Chuẩn chung
- Prefix: `/api/v1`. Trả JSON. Xác thực `Authorization: Bearer <JWT>` (trừ `/auth/*` và quiz công khai).
- Tài liệu tự sinh: springdoc-openapi (`/swagger-ui.html`).

## Mã trạng thái
`200` OK · `201` Created · `202` Accepted (job nền, trả `jobId`) · `400` sai input · `401` chưa auth · `403` không đủ quyền · `404` không thấy · `409` xung đột · `422` không xử lý được · `429` quá hạn mức · `5xx` lỗi server.

## Response lỗi chuẩn
```json
{
  "timestamp": "2026-07-19T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Mô tả lỗi rõ ràng",
  "path": "/api/v1/quizzes",
  "traceId": "..."
}
```
- Tạo tập trung qua `@RestControllerAdvice`. Không lộ stack trace ra client.

## Phân trang & lọc
- `?page=0&size=20&sort=createdAt,desc`. Trả kèm metadata phân trang.
- Lọc qua query param rõ ràng (`?category=...&difficulty=...&q=...`).

## Đặt tên
- Danh từ số nhiều: `/quizzes`, `/questions`, `/attempts`, `/rooms`, `/recommendations`.
- Hành động đặc biệt là sub-resource: `/attempts/{id}/submit`.

## Checklist
- [ ] Endpoint có `@Valid` cho request body.
- [ ] Có `@PreAuthorize` nếu cần quyền.
- [ ] Trả DTO, không trả Entity.
- [ ] Mã trạng thái đúng ngữ nghĩa.
- [ ] Tác vụ >vài giây trả `202 + jobId`.

## Tham chiếu
`docs/api.md` (đặc tả đầy đủ endpoint).
