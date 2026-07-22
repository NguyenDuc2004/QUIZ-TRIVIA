# 09 — Thống kê & Báo cáo

**Ưu tiên:** [S] Should

## Mục tiêu
Cung cấp cái nhìn về tiến độ học tập cho Learner và hiệu quả bài giao cho Creator.

## Use case
- Learner xem tiến độ, điểm mạnh/yếu theo chủ đề.
- Creator xem thống kê quiz đã giao.

## Yêu cầu chức năng
- **FR-26** [M] Learner xem tiến độ: số quiz đã làm, điểm trung bình, điểm mạnh/yếu theo chủ đề.
- **FR-27** [S] Creator xem thống kê bài giao: tỉ lệ hoàn thành, phân bố điểm, câu sai nhiều nhất.
- **FR-28** [C] Xuất báo cáo PDF.

## Luồng xử lý
- Tổng hợp từ `quiz_attempts` + `attempt_answers` theo chủ đề/câu hỏi.
- Điểm mạnh/yếu theo chủ đề dùng chung tín hiệu với gợi ý Neo4j (WEAK_IN).

## API liên quan
[api.md](../api.md) mục 8 (`/analytics/me`, `/analytics/quizzes/{id}`).

## Dữ liệu liên quan
`quiz_attempts`, `attempt_answers`, `questions.topic` — [database.md](../database.md).

## Ghi chú kỹ thuật
- Có thể trực quan hóa bằng biểu đồ ở frontend (điểm theo thời gian, phân bố điểm).
