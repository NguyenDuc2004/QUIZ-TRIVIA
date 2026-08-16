# 09 — Thống kê & Báo cáo

**Ưu tiên:** [S] Should · **Trạng thái:** ✅ đã hiện thực (FR-85, FR-86) · FR-87 ngoài phạm vi

## Mục tiêu
Cung cấp cái nhìn về tiến độ học tập cho Learner và hiệu quả bài giao cho Creator.

## Use case
- Learner xem tiến độ, mức tiến bộ qua từng lượt làm bài.
- Creator xem thống kê quiz đã giao, và **tìm ra bài cần chấm tay**.

## Yêu cầu chức năng
- **FR-85** [M] ✅ Learner xem tiến độ: số quiz đã làm, điểm trung bình, đường điểm theo thời gian.
- **FR-86** [S] ✅ Creator xem thống kê bài giao: tỉ lệ nộp kịp giờ, phân bố điểm, câu sai nhiều nhất,
  danh sách bài làm kèm cờ cần chấm tay.
- **FR-87** [C] ❌ Xuất báo cáo PDF — **ngoài phạm vi đồ án**. Mức [C] Could, và nó chỉ là một cách
  đóng gói lại đúng những số liệu đã hiện trên màn hình; đổi lấy một thư viện sinh PDF cùng bộ font
  tiếng Việt riêng thì không xứng với thứ nó thêm vào.

## Điểm mạnh/yếu theo chủ đề nằm ở đâu

FR-85 nhắc tới "điểm mạnh/yếu theo chủ đề", nhưng **API `/analytics/me` không trả phần đó**. Nó nằm
ở `/recommendations/path` ([features/07](07-recommendation-neo4j.md)), tính từ đồ thị Neo4j.

Lý do: tính lại cùng một kết luận ở đây sẽ cho **hai màn hình nói về cùng một chuyện, bằng hai cách,
trên hai kho dữ liệu**. Chúng khớp nhau hôm nay và lệch nhau vào ngày ai đó sửa một trong hai công
thức — lúc đó không có cách nào biết màn nào đúng. Trang Tiến độ dẫn sang trang Lộ trình thay vì tự
trả lời.

## Luồng xử lý

Tổng hợp **ở CSDL**, không gộp trong Java: thống kê chạm vào toàn bộ lịch sử làm bài, kéo hết
`attempt_answers` về rồi cộng trong bộ nhớ là chở dữ liệu đi vòng vô ích và càng dùng lâu càng chậm.

| Số liệu | Cách tính |
|---|---|
| Lượt làm bài / số quiz | `count(a)` và `count(distinct a.quiz.id)` — làm lại một quiz ba lần là ba lượt nhưng một quiz, hai con số nói hai chuyện khác nhau |
| Điểm trung bình | `sum(totalScore) / sum(maxScore)`, **không** phải trung bình của các phần trăm — mỗi quiz một thang điểm |
| Tỉ lệ nộp kịp giờ | `SUBMITTED / (SUBMITTED + EXPIRED)`; nói lên đề có quá dài hay thời gian đặt quá ngắn |
| Phân bố điểm | Chia mười khoảng 10% bằng truy vấn native (JPQL không có hàm chia khoảng) |
| Câu sai nhiều nhất | Đếm trên `attempt_answers`, lọc theo `graded_by` |

Bài `IN_PROGRESS` bị loại khỏi mọi con số: bài đang làm dở chưa nói được gì.

### Ba chỗ dễ ra số sai

1. **`null` ≠ `0`.** `averagePercent` và `completionPercent` trả `null` khi chưa có dữ liệu. 0% nghĩa
   là *làm mà sai hết*; hiển thị 0 cho người chưa làm gì là nói sai về họ, mà trên giao diện hai
   trạng thái đó trông y hệt nhau.
2. **Câu chưa chấm không phải câu sai.** Bảng câu khó chỉ tính `graded_by ∈ {AUTO, AI, HUMAN}`. Tính
   `PENDING_AI`/`AI_FAILED` là sai thì **câu tự luận nào cũng thành câu khó nhất đề**, và Creator đi
   sửa một câu hỏi không có vấn đề gì.
3. **Ít lượt trả lời không phải khó.** Câu sai 1/1 lượt là 100% sai nhưng chỉ nói lên là ít người
   làm. Ngưỡng: **3 lượt** — cùng ngưỡng với việc kết luận người học yếu một chủ đề (features/07).

Phân bố điểm luôn trả **đủ 10 phần tử** kể cả khoảng rỗng. CSDL chỉ trả khoảng có dữ liệu; để client
tự chèn số 0 vào chỗ trống thì mỗi client chèn một kiểu và trục biểu đồ lệch nhau.

## Chấm tay câu tự luận — món nợ của features/06

[features/06](06-ai-grading.md) làm xong API ghi đè điểm (`PATCH .../grade`) nhưng **không có màn hình
nào dẫn tới nó**, nên trên thực tế tính năng đó chưa dùng được: chủ quiz vừa không tìm được bài nào
cần chấm, vừa không đọc được bài để mà chấm. Lát cắt này trả nợ đó bằng hai thứ:

- `GET /analytics/quizzes/{id}/attempts` — danh sách bài làm kèm `pendingAiCount`, `failedAiCount`
  và cờ `needsManualGrading`. Chỉ mang **số liệu tổng hợp**, không mang nội dung trả lời.
- `GET /attempts/{id}/grading` — bài làm nhìn từ phía người chấm.

`GET /attempts/{id}/grading` là **ngoại lệ có chủ đích** của luật "bài của ai người ấy xem", và là
ngoại lệ duy nhất. Phạm vi bó đúng bằng mục đích:

- Chỉ **câu `SHORT_ANSWER`**. Câu trắc nghiệm máy chấm theo đáp án cố định, không có gì để người xem
  lại; đưa vào đây chỉ mở rộng phần bài làm mà chủ quiz đọc được, không đổi lấy gì.
- Chỉ trên quiz **mình sở hữu**. Người khác — kể cả chính người học — nhận **404**, không phải 403.
- Không dùng lại `AttemptDetailResponse`: đó là màn hình *người học xem bài mình làm*. Ép một DTO
  phục vụ hai mục đích thì mỗi lần thêm trường lại phải nghĩ "trường này ai được thấy", và sẽ có lần
  nghĩ sai.

Rubric và đáp án mẫu hiện trên màn chấm là **đúng chuỗi mà AI đã nhìn** (dùng lại
`GradingPromptBuilder.sampleAnswer`). Ghép lại lần thứ hai ở tầng khác là mở đường cho người và máy
chấm theo hai bản đáp án hơi khác nhau.

Ô nhập điểm của câu `AI_FAILED` để **trống**, không điền sẵn 0. Điểm 0 ở đó là giá trị mặc định của
cột chứ không phải kết luận về bài làm; mớm nó vào ô nhập là dụ người chấm bấm lưu một con số vô
nghĩa.

## API liên quan
[api.md](../api.md) mục 8 (`/analytics/*`) và mục 4 (`/attempts/{id}/grading`).

## Dữ liệu liên quan
`quiz_attempts`, `attempt_answers`, `questions.topic` — [database.md](../database.md).
**Không có migration mới**: toàn bộ số liệu suy ra được từ bảng đã có. Thêm bảng tổng hợp sẵn chỉ
đáng làm khi truy vấn thật sự chậm, mà nó chưa chậm.

## Frontend

| Trang | Đường dẫn | Bộ mặt |
|---|---|---|
| Tiến độ của tôi | `/my-progress` | người học |
| Thống kê quiz | `/my-quizzes/:id/stats` | bảng điều khiển |
| Chấm bài tự luận | `/my-quizzes/:id/attempts/:attemptId` | bảng điều khiển |

Hai biểu đồ (đường điểm, phân bố điểm) **viết tay** bằng SVG và CSS. Thư viện biểu đồ nhẹ nhất cũng
nặng hơn cả tính năng này, và nó mang theo bảng màu riêng đi ngược
[ui-design-system.md](../ui-design-system.md).

Hai chi tiết nhỏ nhưng quyết định biểu đồ có đọc được hay không:

- Trục dọc đường điểm **cố định 0–100%**, không co theo dữ liệu. Co theo dữ liệu thì người dao động
  70–75% thấy đường răng cưa dựng đứng như đang lên xuống thất thường.
- Cột phân bố chia theo **cột cao nhất**, không theo tổng số lượt; và cột có lượt luôn cao tối thiểu
  4px, nếu không thì cột 1 lượt cạnh cột 50 lượt biến mất và người đọc tưởng khoảng đó không có ai.

## Ghi chú kỹ thuật
- `AnalyticsRepository` tách khỏi `QuizAttemptRepository`: đây là truy vấn **chỉ đọc để báo cáo**,
  không nạp thực thể để sửa. Trộn chung thì khó nhìn ra chỗ nào là nghiệp vụ làm bài.
- Test: `AnalyticsIntegrationTest` — 13 ca trên PostgreSQL thật. Bắt buộc phải là CSDL thật vì cả lát
  cắt này *là* mấy câu truy vấn gộp; mock repository thì thứ được kiểm chỉ còn "service có gọi hàm
  không". Có ca riêng cho việc `left join a.answers` **không** nhân đôi số dòng — chỗ sai kinh điển
  khiến bài 5 câu thành 5 dòng.
