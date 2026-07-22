# 03 — Chơi quiz (đơn)

**Ưu tiên:** [M] Must

## Mục tiêu
Cho phép người học làm quiz cá nhân, chấm điểm tự động, xem kết quả và giải thích.

## Use case
- Learner chọn quiz, làm bài luyện tập hoặc thi có tính giờ.
- Learner xem lại lịch sử và kết quả chi tiết.

## Yêu cầu chức năng
- **FR-13** [M] Làm quiz; ghi nhận từng câu trả lời.
- **FR-14** [M] Chế độ luyện tập (phản hồi ngay) và chế độ thi (chấm cuối bài).
- **FR-15** [M] Chấm điểm tự động cho câu có đáp án cố định.
- **FR-16** [M] Đếm giờ mỗi câu / cả bài; tự nộp khi hết giờ.
- **FR-17** [M] Hiển thị kết quả: điểm, số câu đúng/sai, giải thích từng câu.
- **FR-18** [M] Lưu lịch sử làm bài (attempt) để xem lại.

- **FR-19** [S] Bảng xếp hạng (leaderboard) theo quiz/danh mục.

## Luồng xử lý
1. Learner bắt đầu → tạo `quiz_attempt` (status: in_progress).
2. Trả lời từng câu → lưu `attempt_answers`.
3. Nộp bài → chấm câu cố định bằng logic; câu tự luận chuyển sang AI (xem [06-ai-grading.md](06-ai-grading.md)).
4. Tính tổng điểm, cập nhật status: submitted, trả kết quả + giải thích.

## API liên quan
[api.md](../api.md) mục 4 (`/attempts`).

## Dữ liệu liên quan
`quiz_attempts`, `attempt_answers` — [database.md](../database.md).

## Ghi chú kỹ thuật
- Chấm câu cố định (single/multiple/true_false/fill_blank) không dùng AI để tiết kiệm chi phí.
- Mỗi attempt kích hoạt đồng bộ hành vi sang Neo4j (xem [07-recommendation-neo4j.md](07-recommendation-neo4j.md)).
