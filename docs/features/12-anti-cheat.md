# 12 — Chống gian lận thi trực tuyến (Anti-Cheat / Proctoring)

**Ưu tiên:** [S] Should · **Tận dụng:** dữ liệu hành vi, AI phân tích, real-time

## Mục tiêu
Phát hiện và cảnh báo hành vi gian lận trong chế độ thi (bài thi tính điểm & phòng đấu real-time), tăng độ tin cậy của kết quả — một điểm khác biệt mang tính học thuật cho đồ án.

## Use case
- Trong bài thi, hệ thống ghi nhận tín hiệu hành vi bất thường (chuyển tab, copy/paste, thời gian trả lời bất thường).
- Sau bài thi, hệ thống tính **điểm rủi ro (risk score)** và gắn cờ.
- Creator/Admin xem báo cáo tính toàn vẹn (integrity report) để rà soát.

## Yêu cầu chức năng
- **FR-43** [S] Thu thập tín hiệu hành vi phía client trong chế độ thi:
  - Rời/chuyển tab, mất focus cửa sổ (`visibilitychange`, `blur`).
  - Sao chép/dán (`copy`/`paste`).
  - Thoát toàn màn hình (nếu bật fullscreen).
  - Thời gian trả lời bất thường (quá nhanh so với độ khó).
- **FR-44** [S] Phát hiện **đáp án trùng bất thường** giữa các người chơi trong cùng phòng real-time.
- **FR-45** [S] Tính **risk score** & gắn cờ (flags) cho mỗi lần làm bài; lưu nhật ký sự kiện.
- **FR-46** [S] **AI phân tích hành vi:** LLM tổng hợp chuỗi sự kiện + số liệu thành nhận định mức độ nghi ngờ + giải thích.
- **FR-47** [S] Báo cáo tính toàn vẹn cho Creator/Admin; cho phép đánh dấu hợp lệ/không hợp lệ.
- **FR-48** [C] Chế độ thi nghiêm ngặt: bắt buộc fullscreen, khóa chuột phải, cảnh báo khi vi phạm.

## Luồng xử lý
```
Client (chế độ thi) lắng nghe sự kiện → gửi proctoring events (batch, qua REST/WebSocket)
   → server lưu proctoring_events
Khi nộp bài → tính risk score từ:
   - tần suất/loại sự kiện (chuyển tab, paste...)
   - thời gian trả lời so với baseline độ khó
   - (real-time) độ tương đồng đáp án với người chơi khác
   → AI tổng hợp → nhận định + giải thích
   → lưu attempt_integrity (risk_score, flags, ai_note)
Creator/Admin xem báo cáo → xác nhận hợp lệ / không hợp lệ
```

## Tính điểm rủi ro (gợi ý)
`risk_score` = tổng có trọng số của các tín hiệu, chuẩn hóa 0–100:
- Mỗi lần chuyển tab/mất focus: +trọng số.
- Paste nội dung dài: +trọng số cao.
- Thời gian trả lời < ngưỡng tối thiểu theo độ khó: +trọng số.
- Đáp án trùng khớp bất thường với người chơi khác (real-time): +trọng số cao.
- Ngưỡng cảnh báo: ví dụ ≥ 60 → gắn cờ nghi ngờ.

## API liên quan
```
POST   /api/v1/attempts/{id}/proctoring-events   Gửi sự kiện hành vi (batch)
GET    /api/v1/attempts/{id}/integrity           Báo cáo tính toàn vẹn (Creator/Admin)
GET    /api/v1/admin/integrity/flagged           Danh sách bài thi bị gắn cờ
PUT    /api/v1/admin/integrity/{id}/review       Đánh dấu hợp lệ/không hợp lệ
```
Phòng real-time: sự kiện có thể gửi qua kênh WebSocket `/app/room/{code}/proctoring`.

## Dữ liệu liên quan (bổ sung PostgreSQL)
- `proctoring_events(id, attempt_id, user_id, event_type, detail jsonb, occurred_at)`
- `attempt_integrity(id, attempt_id, risk_score, flags jsonb, ai_note text, review_status: pending/valid/invalid, reviewed_by)`

## Ghi chú kỹ thuật & Ràng buộc
- **Chỉ áp dụng chế độ thi** (không áp dụng luyện tập), thông báo rõ cho người dùng (minh bạch, tôn trọng quyền riêng tư — **không** ghi hình/không thu thập dữ liệu ngoài phạm vi bài thi).
- Tín hiệu client **có thể bị bỏ qua/giả mạo** → chỉ dùng làm cảnh báo hỗ trợ con người quyết định, **không tự động phạt**.
- AI phân tích qua AiOrchestrator (fallback Gemini→Grok); không gửi PII.
- Phát hiện đáp án trùng dùng dữ liệu phòng ở Redis + đối chiếu sau ván.
- Là dữ liệu tốt cho phần **đánh giá** trong báo cáo (đo tỉ lệ phát hiện đúng/nhầm).
