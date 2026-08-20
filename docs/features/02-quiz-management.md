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
- **FR-12** [C] 🟡 Import/Export quiz — **JSON đã làm**, CSV không làm (lý do bên dưới).

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


## Xuất / nhập quiz (FR-12)

```
GET  /api/v1/quizzes/{id}/export   Tải file JSON (chỉ chủ quiz)   ✅
POST /api/v1/quizzes/import        Nhập file thành quiz mới       ✅
```

### JSON, không CSV — chọn định dạng theo hình dạng dữ liệu

Đặc tả ghi "JSON/CSV". Một quiz là dữ liệu **lồng nhau**: mỗi câu có nhiều lựa chọn, mỗi lựa chọn có cờ
đúng/sai. Nhét vào bảng phẳng thì phải chọn một trong hai cách, và cả hai đều tệ:

| Cách | Hỏng ở đâu |
|---|---|
| Một dòng mỗi lựa chọn | Thông tin câu hỏi lặp ở mọi dòng; sửa một chỗ quên chỗ kia là hỏng |
| Cột `option1..option6` | Chặn cứng số lựa chọn; câu điền khuyết nhiều đáp án không đủ chỗ |

Ngược lại, [bảng điểm lớp (FR-58)](14-classroom.md) vốn **phẳng** nên dùng CSV. Chọn theo hình dạng dữ liệu,
không theo thói quen.

### File là NỘI DUNG ĐỀ, không phải bản sao một dòng cơ sở dữ liệu

Không mang theo `id`, chủ sở hữu, hay số liệu thống kê:

- Mang `id` → nhập vào máy khác sẽ đụng id có sẵn hoặc **ghi đè nhầm quiz của người khác**.
- Mang lượt làm bài → nhập xong quiz mới đã "có 500 lượt học" mà chưa ai làm — đúng kiểu bịa số mà cả dự án
  tránh.
- Mang `imageUrl` → đường dẫn trỏ vào `uploads/` của **máy cũ**; nhập sang máy khác chỉ tạo một đề đầy ảnh vỡ.

### Ba quyết định về hành vi nhập

| Quyết định | Vì sao |
|---|---|
| **Luôn tạo mới**, không bao giờ ghi đè | Một file cũ nhập nhầm sẽ xoá mất công sức sửa đề mà không có cách nào lấy lại — và đó đúng là thao tác người ta hay làm nhầm nhất với chức năng nhập file |
| **Luôn PRIVATE**, không đọc `visibility` từ file | Nhập xong mà đề tự xuất hiện ở mục Khám phá cho cả thiên hạ xem là một bất ngờ không dễ chịu; muốn công khai thì bấm thêm một nút |
| **Tất cả hoặc không có gì** (một transaction) | Nửa chừng hỏng mà đã ghi năm câu thì người dùng có một quiz cụt, không biết thiếu câu nào, và lần nhập lại tạo thêm một quiz cụt nữa |

`formatVersion` mới hơn bản đang chạy thì **từ chối rõ ràng** thay vì đọc bừa: file mới có thể chứa trường
bản này không hiểu, và đọc bừa sẽ **im lặng làm mất đúng những trường đó** — người dùng tưởng nhập thành công.

Chặn trên **500 câu mỗi file**: không có chặn thì một file 50 000 câu treo cả tiến trình nhập.


## Số người đã làm quiz

Hiện dưới mỗi thẻ quiz và trên trang giới thiệu, để người học chọn được giữa hai chục quiz cùng chủ đề.

### Ba quyết định, cái đầu quan trọng nhất

| Quyết định | Vì sao |
|---|---|
| **Đếm NGƯỜI, không đếm LƯỢT** (`count(distinct user_id)`) | Một người luyện tập 50 lần sẽ làm quiz trông như có 50 người quan tâm — con số đó vừa sai vừa dễ thổi phồng. Nhãn cũng phải là *"N người đã làm"*, không phải *"N lượt"* |
| **Chỉ tính bài đã xong** (`status <> 'IN_PROGRESS'`) | Bấm vào rồi thoát ngay không phải là "đã làm quiz này". `EXPIRED` **vẫn tính**: hết giờ thì bài vẫn được chấm trên phần đã trả lời |
| **0 thì giao diện ẩn hẳn**, không hiện *"0 người đã làm"* | Số 0 đọc như một lời chê và phạt oan mọi quiz mới, trong khi thứ nó thật sự nói chỉ là *"chưa ai kịp làm"*. API vẫn trả 0 — việc ẩn là của giao diện, backend không bịa giá trị khác để né |

### `@Formula`, không phải cột đếm sẵn

Cột đếm sẵn cần ai đó cập nhật mỗi lần có người nộp bài — thêm một chỗ có thể lệch với sự thật, để đổi lấy
tốc độ mà trang danh sách chưa cần. `@Formula` nằm ngay trong câu SELECT của danh sách nên **không sinh
N+1**, đúng cách `questionCount` đang làm.

### Vẫn KHÔNG có đánh giá / số sao

Hệ thống không có dữ liệu đánh giá, và dự án đã từ chối bịa nó **hai lần**: [features/07](07-recommendation-neo4j.md)
bỏ `q.rating` khỏi mô hình đồ thị, và `CLAUDE.md` §5 cấm thẳng. Muốn có sao thì phải làm hẳn tính năng đánh
giá (bảng `quiz_ratings`, chỉ cho đánh giá sau khi nộp bài, ẩn trung bình khi dưới 5 phiếu) — không phải
dán một ô sao lên thẻ.

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
