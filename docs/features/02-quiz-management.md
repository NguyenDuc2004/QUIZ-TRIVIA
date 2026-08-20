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
- **FR-11** [S] ✅ Đính kèm hình ảnh — **ảnh bìa quiz** và **ảnh cho từng câu hỏi** (V23), cùng đi qua `POST /api/v1/files/images`. Xem "Ảnh câu hỏi" bên dưới.
- **FR-12** [C] ⏳ Import/Export quiz (JSON/CSV) — chưa làm.

## Quyết định thiết kế
- **Đáp án của FILL_BLANK / SHORT_ANSWER** dùng chung bảng `question_options`: với `FILL_BLANK` mỗi dòng là một cách viết được chấp nhận (tự đánh dấu `is_correct = true`); với `SHORT_ANSWER` chỉ lưu **một** đáp án mẫu để AI đối chiếu khi chấm ([features/06](06-ai-grading.md)).
- **Xóa câu hỏi đang nằm trong quiz → 409**, buộc bỏ khỏi quiz trước, để không âm thầm làm hụt câu hỏi của quiz đã xuất bản.
- **Đặt câu hỏi vào quiz bằng một endpoint thay thế cả danh sách** (`PUT /quizzes/{id}/questions`) — thứ tự trong mảng là thứ tự câu hỏi; idempotent, tránh lệch thứ tự khi kéo-thả nhiều lần.
- **Quiz PRIVATE của người khác trả 404** (không phải 403) để không tiết lộ tài nguyên tồn tại.


## Ảnh câu hỏi (FR-11)

Dùng lại **nguyên** đường tải ảnh của ảnh bìa quiz: `POST /api/v1/files/images` đã có kiểm kiểu tệp, giới
hạn kích thước riêng cho ảnh, và sinh đường dẫn nội bộ. Thêm một đường tải riêng là nhân đôi cả ba thứ đó,
và hai bản sao sẽ lệch nhau ở lần sửa đầu tiên.

### Chỉ nhận ảnh của hệ thống — và đây là luật an toàn, không phải luật hiển thị

Luật này từng nằm `private` trong `QuizService` cho riêng ảnh bìa. Giờ tách ra `UploadedImagePath` dùng
chung, vì **nhân đôi một luật an toàn nghĩa là lần sau ai đó nới nó ở một chỗ mà quên chỗ kia — và chỗ bị
quên chính là lỗ hổng**, do không ai nghĩ nó còn tồn tại.

Hai thứ bị chặn:

| Chặn | Vì sao |
|---|---|
| URL ngoài (`http://…`, `//…`, `data:`) | Mỗi lần người học mở đề là **một request kèm IP gửi sang máy chủ lạ**. Người soạn đề nhúng được pixel theo dõi vào bài thi của người khác, và người bị theo dõi không hề biết. Phụ thêm: ảnh bên thứ ba chết bất cứ lúc nào, đề mất hình giữa buổi kiểm tra |
| `..` trong đường dẫn | Chỉ kiểm tiền tố là chưa đủ — `/uploads/../../etc/passwd` vẫn bắt đầu bằng `/uploads/` |

### Ảnh phải tới được ba màn hình, không chỉ được lưu

Chỗ dễ hỏng nhất của tính năng này **không phải** phần lưu, mà là phần hiển thị: lưu xong mà DTO không
mang theo thì ảnh tồn tại trong cơ sở dữ liệu và **người học không bao giờ thấy**.

| Màn hình | DTO | Chiều cao tối đa |
|---|---|---|
| Làm bài đơn | `AttemptQuestionResponse.imageUrl` | `max-h-72` |
| Xem lại sau khi nộp | cùng DTO trên | `max-h-72` |
| Phòng đấu real-time | `LiveQuestionView.imageUrl` | **`max-h-56`** |

Phòng đấu để thấp hơn có lý do: nó **tính điểm theo tốc độ**, nên đẩy các nút đáp án xuống dưới màn hình là
trực tiếp lấy mất điểm của người chơi màn hình nhỏ.

Ảnh nằm ở **phần đề bài** nên luôn hiện, kể cả lúc chưa nộp — khác `explanation` và `correctOptionIds` vốn
chỉ lộ sau khi nộp. Màn xem lại cũng phải có ảnh: *"hình nào sau đây là đồ thị hàm số?"* mà không có hình
là một câu vô nghĩa.

**AI sinh đề không gắn ảnh.** Mô hình sinh chữ, không sinh ảnh; để nó tự điền một đường dẫn nào đó là gắn
ảnh không tồn tại. Creator tự thêm khi duyệt nếu muốn.

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
