# 03 — Chơi quiz (đơn)

**Ưu tiên:** [M] Must

## Mục tiêu
Cho phép người học làm quiz cá nhân, chấm điểm tự động, xem kết quả và giải thích.

## Use case
- Learner chọn quiz, làm bài luyện tập hoặc thi có tính giờ.
- Learner xem lại lịch sử và kết quả chi tiết.

## Yêu cầu chức năng
- **FR-13** [M] ✅ Làm quiz; ghi nhận từng câu trả lời.
- **FR-14** [M] ✅ Chế độ luyện tập (phản hồi ngay) và chế độ thi (chấm cuối bài).
- **FR-15** [M] ✅ Chấm điểm tự động cho câu có đáp án cố định.
- **FR-16** [M] ✅ Đếm giờ cả bài; tự nộp khi hết giờ. *(Giờ riêng từng câu: cột `questions.time_limit_sec` đã có và được trả về API, nhưng chưa cưỡng chế — để dành cho phòng đấu real-time ở features/04.)*
- **FR-17** [M] ✅ Hiển thị kết quả: điểm, số câu đúng/sai, giải thích từng câu.
- **FR-18** [M] ✅ Lưu lịch sử làm bài (attempt) để xem lại.

- **FR-19** [S] ✅ Bảng xếp hạng theo quiz. *(Xếp hạng theo danh mục chưa làm.)*

## Luồng xử lý
1. Learner bắt đầu → tạo `quiz_attempt` (status: in_progress).
2. Trả lời từng câu → lưu `attempt_answers`.
3. Nộp bài → chấm câu cố định bằng logic; câu tự luận chuyển sang AI (xem [06-ai-grading.md](06-ai-grading.md)).
4. Tính tổng điểm, cập nhật status: submitted, trả kết quả + giải thích.


## Thứ tự thích ứng theo chuỗi đúng/sai (FR-32)

`GET /api/v1/attempts/{id}/next-question` — chỉ có tác dụng với lượt **luyện tập**.

### Đổi THỨ TỰ, không đổi BỘ ĐỀ

Cách hiểu thông thường của "adaptive difficulty" là chọn một *tập câu hỏi khác nhau* cho từng người. Không
làm vậy, vì bán kính ảnh hưởng lớn hơn nhiều so với giá trị:

| Nếu đổi bộ đề | Hỏng cái gì |
|---|---|
| Hai người làm hai bộ câu khác nhau | **Điểm không so được** — bảng xếp hạng theo quiz (FR-19), bảng theo dõi lớp ([features/14](14-classroom.md), FR-57) và thống kê quiz ([features/09](09-analytics.md)) đều dựa trên giả định ngược lại |
| Chọn câu động | Phá bất biến **"chốt đề lúc bắt đầu"** — `attempt_answers` sao lại toàn bộ câu kèm điểm tối đa để chủ quiz sửa đề giữa chừng không làm hỏng bài đang làm |

Nên bộ đề giữ nguyên, thứ tự thì thích ứng. **Mọi câu vẫn được hỏi**, nên điểm vẫn so được và mọi tính năng
dựa trên điểm vẫn đúng.

### Chỉ luyện tập, không áp cho thi

Thi là để **đo**: mọi người phải làm cùng một đề theo cùng một thứ tự, nếu không thì thứ tự trở thành một
biến số ảnh hưởng tới điểm mà không ai kiểm soát. Luyện tập là để **học**, và ở đó thích ứng có ích thật.

### Luật

| Hai câu vừa làm | Câu tiếp theo |
|---|---|
| Đều sai | Câu **dễ nhất** còn lại — lấy lại đà |
| Đều đúng | Câu **khó nhất** còn lại — đừng phí thời gian với thứ đã nắm |
| Trộn, hoặc mới làm một câu | Theo thứ tự đề |

Ngưỡng là **hai** câu chứ không phải một: một câu sai có thể là bấm nhầm hoặc một câu lắt léo, và đổi hướng
ngay lập tức làm độ khó nhảy lên xuống từng câu — người học thấy đề "loạn" chứ không thấy nó thích ứng.

### Chỗ dễ sai nhất: thứ tự LÀM khác thứ tự ĐỀ

Chính thuật toán này đã đổi thứ tự ở các bước trước, nên *"hai câu gần nhất"* phải đọc theo **thời điểm trả
lời**. Lọc danh sách đề rồi lấy hai phần tử cuối sẽ ra hai câu có **số thứ tự** lớn nhất — không phải hai
câu người học vừa làm, và chuỗi đọc ra sai hoàn toàn. Có test dựng riêng một ví dụ mà hai cách đọc cho hai
hướng **ngược nhau**.

**Câu chưa đặt độ khó nằm giữa thang**, không rơi xuống đáy: "không biết" khác "dễ" và cũng khác "khó".

**Server quyết định, không phải client** — dù client có đủ dữ liệu để tự tính. Tính ở client thì hai bản
giao diện sẽ thích ứng khác nhau trên cùng dữ liệu. Server hỏng thì frontend lùi về thứ tự tuần tự: người
đang làm bài không được kẹt lại vì một tính năng phụ trợ gặp sự cố.

## API liên quan
[api.md](../api.md) mục 4 (`/attempts`).

## Dữ liệu liên quan
`quiz_attempts`, `attempt_answers` — [database.md](../database.md).

## Ghi chú kỹ thuật
- Chấm câu cố định (single/multiple/true_false/fill_blank) không dùng AI để tiết kiệm chi phí.
- Mỗi attempt kích hoạt đồng bộ hành vi sang Neo4j (xem [07-recommendation-neo4j.md](07-recommendation-neo4j.md)) — **chưa làm**, chờ features/07.

## Quyết định thiết kế (đã hiện thực)

**1. Chốt đề ngay khi bắt đầu.** `POST /quizzes/{id}/attempts` sao toàn bộ câu hỏi của quiz thành các
dòng `attempt_answers` kèm `order_index` và `max_score`. Chủ quiz sửa/gỡ câu hỏi giữa chừng cũng không
làm hỏng bài đang làm, và điểm bài đã nộp không bao giờ bị tính lại lệch. Đây cũng là lý do
`quiz_attempts.max_score` được lưu chứ không tính động.

**2. Không lộ đáp án khi bài chưa nộp.** Cùng một DTO phục vụ hai màn hình, khác nhau ở cờ *reveal*:
bài `IN_PROGRESS` thì `correctOptionIds`/`explanation`/`correct`/`score` đều null. Câu `FILL_BLANK` và
`SHORT_ANSWER` lưu đáp án ngay trong `question_options`, nên lúc đang làm phải trả `options: []` —
nếu không, mở DevTools là thấy đáp án.

**3. Bài làm là dữ liệu riêng.** Bài của người khác trả **404** (không phải 403), kể cả với chủ quiz và
Admin — cùng quy ước "không tiết lộ sự tồn tại" đã dùng cho quiz PRIVATE. Creator muốn xem kết quả lớp
mình sẽ có API thống kê riêng ở [09-analytics.md](09-analytics.md).

**4. Làm tiếp thay vì tạo bài mới.** Chỉ mục một phần trên `(user_id, quiz_id) WHERE status = 'IN_PROGRESS'`
bảo đảm mỗi người tối đa một bài dở trên một quiz; gọi lại API bắt đầu thì trả về bài đó. Tải lại trang
giữa chừng không mất câu đã trả lời.

**5. Hết giờ chốt kiểu "lười".** Không có job nền quét bài quá hạn. `expires_at` lưu sẵn trong DB;
lần gọi `GET /attempts/{id}` hoặc `submit` kế tiếp sẽ tự chuyển bài sang `EXPIRED` và chấm phần đã làm.
`POST /answers` sau hạn trả 409 mà **không** tự nộp — vì ném lỗi sẽ rollback cả việc nộp; lần gọi sau
chốt lại cho cùng kết quả nên không mất mát gì.

**6. `MULTIPLE_CHOICE` chấm trọn gói.** Chọn thiếu hoặc thừa đều 0 điểm, không chấm từng phần — luật rõ
ràng, dễ giải thích cho người học, và khớp với cách chấm trắc nghiệm phổ biến ở trường.

**7. `FILL_BLANK` bỏ qua hoa/thường và khoảng trắng thừa nhưng GIỮ dấu tiếng Việt.** "toan" không được
tính là "toán": bỏ dấu sẽ biến câu sai thành câu đúng. Muốn chấp nhận nhiều cách viết thì người soạn đề
thêm từng dòng đáp án.

**8. Chủ quiz làm được bài trên quiz của chính mình**, kể cả quiz PRIVATE — đây là cách tự kiểm đề
trước khi xuất bản. Đáp án vẫn bị giấu y như với người học. Lối vào là nút **"Làm thử"** ở trang
*Quiz của tôi* và màn soạn quiz. Không chặn vì chặn cũng vô nghĩa (chủ quiz vốn xem được đáp án ở màn
soạn đề) mà lại mất một chức năng hữu ích.

Nhưng **bài của chủ quiz bị loại khỏi bảng xếp hạng** (`a.user_id <> q.owner_id` trong truy vấn):
người soạn đề biết trước đáp án, để họ lên bảng thì cuộc đua mất công bằng và bảng mất ý nghĩa.
Điểm của chủ quiz vẫn nằm nguyên trong lịch sử cá nhân của họ — chỉ không đem ra so với người học.

**9. `SHORT_ANSWER` tạm 0 điểm, đánh dấu `PENDING_AI`.** Chưa có AI nên không thể chấm; hệ thống nói rõ
trên giao diện là câu đó đang chờ chấm và điểm cuối có thể cao hơn, thay vì lặng lẽ tính sai.
Câu tự luận **bỏ trống** thì chốt sai ngay, không đẩy sang AI.
