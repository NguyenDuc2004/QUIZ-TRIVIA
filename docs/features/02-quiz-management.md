# 02 — Quản lý Quiz & Câu hỏi

**Ưu tiên:** [M] Must

## Mục tiêu
Cho phép Creator tạo, quản lý quiz và ngân hàng câu hỏi tái sử dụng theo danh mục/chủ đề.

## Use case
- Creator tạo/sửa/xóa quiz, cấu hình metadata.
- Creator quản lý ngân hàng câu hỏi độc lập và gắn vào nhiều quiz.

## Yêu cầu chức năng
- **FR-7** [M] CRUD quiz với metadata: tiêu đề, mô tả, danh mục, độ khó, tag, visibility (công khai/riêng tư).
- **FR-8** [M] Ngân hàng câu hỏi độc lập, tái sử dụng giữa nhiều quiz.
- **FR-9** [M] Hỗ trợ loại câu hỏi: single-choice, multiple-choice, true/false, fill-in-blank, short-answer.
- **FR-10** [M] Mỗi câu hỏi: nội dung, lựa chọn, đáp án đúng, giải thích, điểm, độ khó, thời gian giới hạn.
- **FR-11** [S] Đính kèm hình ảnh cho câu hỏi.
- **FR-12** [C] Import/Export quiz (JSON/CSV).

## Luồng xử lý (tạo quiz)
1. Creator tạo quiz + metadata.
2. Thêm câu hỏi: soạn thủ công, chọn từ ngân hàng, hoặc **sinh bằng AI** (xem [05-ai-rag-generation.md](05-ai-rag-generation.md)).
3. Sắp thứ tự câu hỏi (`quiz_questions.order_index`).
4. Đặt visibility và xuất bản.

## API liên quan
[api.md](../api.md) mục 3 (`/quizzes`, `/questions`).

## Dữ liệu liên quan
`quizzes`, `questions`, `question_options`, `quiz_questions`, `categories` — [database.md](../database.md) mục 1.2.

## Ghi chú kỹ thuật
- Câu hỏi có `source` = manual/ai_generated; câu do AI sinh phải được Creator duyệt.
- Kiểm tra quyền sở hữu (owner) khi sửa/xóa.
