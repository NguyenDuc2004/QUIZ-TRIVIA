# 02 — Quản lý Quiz & Câu hỏi

**Ưu tiên:** [M] Must

## Mục tiêu
Cho phép Creator tạo, quản lý quiz và ngân hàng câu hỏi tái sử dụng theo danh mục/chủ đề.

## Use case
- Creator tạo/sửa/xóa quiz, cấu hình metadata.
- Creator quản lý ngân hàng câu hỏi độc lập và gắn vào nhiều quiz.

## Yêu cầu chức năng
- **FR-7** [M] ✅ CRUD quiz với metadata: tiêu đề, mô tả, danh mục, độ khó, visibility (PUBLIC/PRIVATE), thời gian làm bài. *(Chưa làm: tag tự do — chưa cần cho trụ cột nào.)*
- **FR-8** [M] ✅ Ngân hàng câu hỏi độc lập; bảng nối `quiz_questions` cho phép dùng lại một câu hỏi ở nhiều quiz với thứ tự riêng.
- **FR-9** [M] ✅ Đủ 5 loại: SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK, SHORT_ANSWER — mỗi loại có luật riêng, xem [api.md §3](../api.md).
- **FR-10** [M] ✅ Câu hỏi có nội dung, lựa chọn/đáp án, giải thích, điểm, độ khó, chủ đề, thời gian giới hạn.
- **FR-11** [S] 🔶 Đính kèm hình ảnh — **đã làm ảnh bìa quiz** (tải lên server, lưu ở thư mục local `uploads/`). Ảnh cho từng câu hỏi chưa làm, nhưng dùng lại được nguyên `POST /api/v1/files/images`.
- **FR-12** [C] ⏳ Import/Export quiz (JSON/CSV) — chưa làm.

## Quyết định thiết kế
- **Đáp án của FILL_BLANK / SHORT_ANSWER** dùng chung bảng `question_options`: với `FILL_BLANK` mỗi dòng là một cách viết được chấp nhận (tự đánh dấu `is_correct = true`); với `SHORT_ANSWER` chỉ lưu **một** đáp án mẫu để AI đối chiếu khi chấm ([features/06](06-ai-grading.md)).
- **Xóa câu hỏi đang nằm trong quiz → 409**, buộc bỏ khỏi quiz trước, để không âm thầm làm hụt câu hỏi của quiz đã xuất bản.
- **Đặt câu hỏi vào quiz bằng một endpoint thay thế cả danh sách** (`PUT /quizzes/{id}/questions`) — thứ tự trong mảng là thứ tự câu hỏi; idempotent, tránh lệch thứ tự khi kéo-thả nhiều lần.
- **Quiz PRIVATE của người khác trả 404** (không phải 403) để không tiết lộ tài nguyên tồn tại.

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
