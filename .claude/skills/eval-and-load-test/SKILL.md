---
name: eval-and-load-test
description: Dùng khi thu thập số liệu cho báo cáo (mục 3.5 & 3.6) — load test real-time phòng đấu bằng k6/Gatling (P95 latency, throughput) và đánh giá độ chính xác AI (sinh đề, chấm tự luận, grounding, fallback Gemini→Grok). Xuất bảng số liệu đưa thẳng vào báo cáo.
---

# Đánh giá hiệu năng real-time & độ chính xác AI

Hai hạng mục **bắt buộc theo phiếu đề tài**. Khác với `backend-testing` (mock LLM, kiểm đúng/sai): skill này **đo số liệu thực** trên hệ thống đã chạy, để viết Chương 3.

## A. Load test real-time (→ mục 3.5 báo cáo)
1. **Công cụ:** k6 (ưu tiên, có x->ws) hoặc Gatling cho WebSocket/STOMP.
2. **Kịch bản:** mô phỏng N người chơi/1 phòng (N = 10, 50, 100…): kết nối → join → nhận `QUESTION` → gửi `ANSWER` → nhận `LEADERBOARD`.
3. **Đo:** **P95 latency** (từ lúc server phát câu hỏi tới lúc client nhận / từ lúc trả lời tới khi có kết quả), **throughput** (msg/s), tỉ lệ lỗi, mức ổn định khi tăng tải.
4. **So sánh có/không Redis Pub/Sub** (chứng minh giá trị kiến trúc scale ngang).
5. Xuất **bảng số liệu + biểu đồ** → dán vào mục 3.5.

| Kịch bản | Client/phòng | P95 (ms) | Throughput | Tỉ lệ lỗi | Ghi chú |
|---|---|---|---|---|---|

## B. Đánh giá độ chính xác AI (→ mục 3.6 báo cáo)
1. **Sinh đề (RAG):** lấy mẫu M câu → chấm tay: đúng nội dung học liệu? đáp án nhiễu hợp lý? đúng chuẩn cấu trúc? → **tỉ lệ đạt / tỉ lệ cần sửa**.
2. **Chấm tự luận:** so điểm AI với điểm người chấm trên tập mẫu → **sai số trung bình / tương quan**.
3. **Grounding chatbot:** tỉ lệ câu trả lời có trích nguồn đúng ngữ cảnh vs ảo giác (test cả câu hỏi ngoài học liệu → phải từ chối/nói thiếu thông tin).
4. **Fallback:** tắt Gemini (giả lập lỗi) → xác nhận Grok tiếp quản, **đo thời gian chuyển**; kiểm `ai_request_logs` ghi đúng `provider`.
5. Xuất bảng chỉ số → dán vào mục 3.6.

| Hạng mục | Chỉ số | Cách đo | Kết quả |
|---|---|---|---|

## Quy trình chung
1. Seed data trước (skill `data-seeding`) để có phòng/học liệu/attempt thật.
2. Chạy trên môi trường ổn định (Docker Compose full stack); ghi cấu hình máy.
3. Lặp lại ≥3 lần lấy trung bình; nêu rõ điều kiện đo.
4. Đưa số liệu + nhận xét vào báo cáo; lưu script test vào repo để tái lập.

## Checklist
- [ ] Load test có so sánh có/không Redis Pub/Sub.
- [ ] Đo P95 (không chỉ trung bình) + throughput + tỉ lệ lỗi.
- [ ] Đánh giá AI có tập mẫu rõ ràng + tiêu chí chấm minh bạch.
- [ ] Có kịch bản câu-hỏi-ngoài-học-liệu để test grounding.
- [ ] Demo fallback đo được thời gian chuyển.
- [ ] Số liệu tái lập được (script lưu trong repo); ghi điều kiện đo.

## Chống mẫu (tránh)
- Chỉ báo latency trung bình (giấu đuôi P95). Đánh giá AI cảm tính không có tập mẫu. Đo 1 lần rồi kết luận. Chạy load test trên máy đang tải nặng khác.
