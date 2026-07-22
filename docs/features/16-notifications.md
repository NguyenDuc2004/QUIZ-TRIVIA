# 16 — Thông báo & Nhắc ôn tập (Notifications)

**Ưu tiên:** [S] Should · **Tận dụng:** SRS (features/11), scheduler, WebSocket

## Mục tiêu
Giữ chân người dùng và hỗ trợ ghi nhớ bằng hệ thống thông báo: nhắc ôn tập theo lịch **lặp lại ngắt quãng (SRS)**, nhắc hạn nộp bài, thông báo thành tích (badge/level), lời mời phòng đấu.

## Use case
- Learner nhận nhắc "Bạn có N thẻ đến hạn ôn hôm nay".
- Học sinh nhận nhắc "Bài tập X sắp đến hạn nộp".
- Learner nhận thông báo khi lên level / mở khóa huy hiệu.
- Người dùng nhận thông báo real-time (in-app) và/hoặc email.

## Yêu cầu chức năng
- **FR-65** [S] Tạo & lưu thông báo theo loại: SRS reminder, assignment due, achievement, room invite, system.
- **FR-66** [S] **Nhắc ôn tập theo SRS:** job nền quét `flashcard_reviews.due_date` → tạo thông báo cho user có thẻ đến hạn.
- **FR-67** [S] Thông báo **in-app real-time** qua WebSocket (kênh riêng theo user).
- **FR-68** [S] Trung tâm thông báo: xem danh sách, đánh dấu đã đọc.
- **FR-69** [C] Gửi **email** cho thông báo quan trọng (nhắc ôn, hạn nộp).
- **FR-70** [C] Tùy chọn cài đặt: bật/tắt từng loại thông báo, khung giờ nhắc.

## Luồng xử lý
```
Nguồn sự kiện:
  - Scheduler hằng ngày: quét thẻ SRS đến hạn, assignment sắp hết hạn → tạo notification
  - Domain event (level up, badge, room invite) → tạo notification
Gửi:
  - Lưu notifications (PostgreSQL)
  - Push real-time qua WebSocket /user/queue/notifications (nếu online)
  - (tùy chọn) gửi email async
```

## API liên quan
```
GET    /api/v1/notifications              Danh sách thông báo (phân trang)
PUT    /api/v1/notifications/{id}/read     Đánh dấu đã đọc
PUT    /api/v1/notifications/read-all      Đánh dấu tất cả đã đọc
GET/PUT /api/v1/notifications/settings     Cài đặt loại thông báo
```
WebSocket: subscribe `/user/queue/notifications` để nhận real-time.

## Dữ liệu liên quan (bổ sung PostgreSQL)
- `notifications(id, user_id, type, title, body, data jsonb, is_read, created_at)`
- `notification_settings(user_id PK, srs_reminder, assignment_due, achievement, email_enabled, quiet_hours jsonb)`

## Ghi chú kỹ thuật
- **Scheduler:** dùng `@Scheduled` (Spring) cho job hằng ngày; cân nhắc khóa phân tán (Redis) nếu chạy nhiều instance để không gửi trùng.
- Real-time in-app tái dùng hạ tầng WebSocket (kênh `/user/queue/...`, gửi tới đúng user).
- Email gửi **bất đồng bộ**; không chặn luồng chính.
- Tôn trọng cài đặt người dùng & quiet hours (không spam).
