# Đo hiệu năng & thời gian sinh đề (Chương 3.4)

Hai script lấy **số liệu đo được bằng máy** cho mục 3.4.2 (hiệu năng) và một phần Bảng 3.3 (thời gian sinh đề).

> Phần đánh giá **chủ quan** — tỷ lệ chấp nhận câu hỏi (Bảng 3.3) và tỷ lệ phát hiện/báo nhầm
> giám sát (Bảng 3.4) — **không** đo tự động được; bạn phải tự thực nghiệm và đánh giá rồi điền.

## 1. Load test API (mục 3.4.2 — p95, chịu tải 50 VU)

Cài k6 (một lần): `winget install k6` (Windows) hoặc xem https://k6.io/docs/get-started/installation/

```bash
# Backend phải đang chạy + đã seed tài khoản (chạy ../capture/seed.mjs trước để có gv.demo)
k6 run -e EMAIL=gv.demo@eduexam.local -e PASSWORD=Demo@12345 load-test.js
```

Đọc kết quả:
- `http_req_duration ... p(95)=XXXms` → so với ngưỡng NFR **< 500ms**.
- `http_req_failed ... rate` → tỉ lệ lỗi.
- Phần `checks` phải 100%.

Điền các số p95 / tỉ lệ lỗi / số VU vào mục **3.4.2** của báo cáo.

## 2. Thời gian sinh đề (Bảng 3.4 — cột thời gian)

Tiền đề: đã upload 1 tài liệu trong UI giảng viên và tài liệu ở trạng thái **READY** (đã embedding);
backend đã cấu hình `GEMINI_API_KEY` thật. Lấy `DOCUMENT_ID` từ URL hoặc API danh sách tài liệu.

```bash
node measure-generation.mjs --email gv.demo@eduexam.local --password Demo@12345 \
     --doc <DOCUMENT_ID> --count 10 --runs 3
```

Script in **thời gian sinh trung bình** (mục tiêu < 60s) + nội dung từng câu để bạn tự review.
→ Điền cột "Thời gian sinh" của Bảng 3.4; cột "Tỷ lệ chấp nhận" bạn tự đánh giá từng câu (chấp nhận / cần sửa).

## 3. Đánh giá giám sát (Bảng 3.5) — làm thủ công

Không có script. Thực hiện kịch bản mô phỏng khi đang làm bài thi và đếm:
- Chuyển tab / mất focus: chủ động chuyển tab N lần, đếm số lần hệ thống ghi `TAB_HIDDEN/WINDOW_BLUR`.
- Con trỏ rời cửa sổ: di chuột ra ngoài N lần.
- Vắng mặt / nhiều khuôn mặt / quay mặt đi: mô phỏng trước webcam N lần.

Với mỗi loại: tỷ lệ phát hiện = (phát hiện đúng / số lần thử); báo nhầm = (báo khi không vi phạm / tổng).
Mục tiêu: chuyển tab ≥ 90%, khuôn mặt ≥ 80%, báo nhầm < 15%.
