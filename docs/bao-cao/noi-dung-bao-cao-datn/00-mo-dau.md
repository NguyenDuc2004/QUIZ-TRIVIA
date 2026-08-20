# MỞ ĐẦU

## 1. Lý do chọn đề tài

Học tập trực tuyến đã trở thành một phần thường trực của giáo dục Việt Nam sau giai đoạn dạy học từ xa
diện rộng. Trong đó, hình thức **trắc nghiệm hoá việc ôn tập** (quiz) được sử dụng rộng rãi bởi ba lý
do: câu hỏi ngắn nên người học duy trì được sự tập trung, kết quả trả về ngay nên người học biết mình
sai ở đâu, và cơ chế thi đấu — tính điểm theo tốc độ, bảng xếp hạng — tạo ra động lực mà bài tập truyền
thống khó có. Các nền tảng như Kahoot!, Quizizz đã chứng minh mô hình này hiệu quả trong lớp học.

Tuy vậy, khi quan sát các nền tảng hiện có dưới góc nhìn của người *ôn thi dài hạn* thay vì người *chơi
một buổi*, có thể thấy ba khoảng trống:

**Thứ nhất, việc soạn đề vẫn là công việc thủ công tốn nhiều thời gian.** Giáo viên có sẵn giáo trình,
slide bài giảng, tài liệu ôn tập, nhưng để chuyển hoá khối tài liệu đó thành bộ câu hỏi trắc nghiệm thì
phải đọc lại, chọn ý, đặt câu hỏi, soạn phương án nhiễu — một quy trình lặp đi lặp lại. Các nền tảng
hiện tại hỗ trợ *nhập* câu hỏi rất tốt nhưng gần như không hỗ trợ *sinh* câu hỏi từ chính học liệu của
người dùng.

**Thứ hai, phản hồi cho người học còn dừng ở mức đúng/sai.** Với câu trắc nghiệm nhiều lựa chọn, biết
đáp án đúng là đủ; nhưng với câu trả lời ngắn hoặc câu tự luận, người học cần biết *vì sao* bài làm của
mình chưa đạt. Việc chấm và giải thích loại câu này đòi hỏi giáo viên đọc từng bài, nên trên các nền
tảng tự động hoá, loại câu hỏi có giá trị đánh giá cao nhất lại thường bị bỏ qua.

**Thứ ba, gợi ý nội dung chưa gắn với năng lực thật của người học.** Việc chọn bài ôn tiếp theo phần lớn
do người học tự quyết hoặc dựa trên mức độ phổ biến của bài. Trong khi đó, dữ liệu hành vi — người này
đã làm bài nào, sai ở chủ đề nào, những người có kết quả tương tự thường học tiếp gì — là quan hệ nhiều
chiều giữa người học, chủ đề và bài thi. Biểu diễn quan hệ đó bằng bảng quan hệ thuần thì mỗi truy vấn
gợi ý trở thành một chuỗi phép kết phức tạp, trong khi cơ sở dữ liệu đồ thị mô tả nó tự nhiên hơn nhiều.

Ba khoảng trống trên tương ứng với ba nhóm công nghệ đã đủ chín để áp dụng: **mô hình ngôn ngữ lớn (LLM)
kết hợp kiến trúc RAG** cho việc sinh đề và trợ lý học tập bám học liệu; **cơ sở dữ liệu đồ thị Neo4j**
cho gợi ý cá nhân hoá; và **WebSocket kết hợp Redis** cho phòng đấu nhiều người chơi có độ trễ thấp.
Đề tài *"Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo"* được chọn nhằm giải quyết đồng thời
ba khoảng trống đó trong một hệ thống hoàn chỉnh, thay vì chỉ hiện thực một chức năng đơn lẻ.

Riêng với việc ứng dụng LLM, đề tài đặt trọng tâm vào **tính đáng tin của câu trả lời** chứ không chỉ ở
chỗ gọi được mô hình. Một mô hình ngôn ngữ có thể trả lời trôi chảy về nội dung nó chưa từng đọc — hiện
tượng "ảo giác" (hallucination). Trong bối cảnh ôn thi, một câu trả lời sai nhưng trôi chảy còn tệ hơn
việc không trả lời, vì người học không có cơ sở nào để nghi ngờ. Vì vậy kiến trúc RAG được chọn: mô hình
chỉ được trả lời dựa trên học liệu đã truy xuất, và hệ thống trả kèm trích dẫn nguồn để người dùng đối
chiếu.

## 2. Mục đích và mục tiêu đề tài

**Mục đích:** xây dựng một hệ thống web Quiz/Trivia hoàn chỉnh, có thể vận hành thực tế, trong đó trí tuệ
nhân tạo và cơ sở dữ liệu đồ thị được tích hợp để giảm công sức soạn đề cho người dạy và cá nhân hoá lộ
trình ôn tập cho người học.

**Mục tiêu cụ thể**, theo phiếu giao đề tài:

1. Xây dựng website Quiz/Trivia hoàn chỉnh, giao diện thân thiện, có khả năng xử lý các **phòng đấu trí
   tương tác nhiều người chơi (Multiplayer) theo thời gian thực với độ trễ thấp**.
2. Tích hợp mô hình **Trí tuệ nhân tạo tạo sinh (Generative AI) qua kiến trúc RAG** để làm trợ lý học tập
   thông minh và tự động hoá quy trình sinh cấu trúc đề thi từ tài liệu học liệu.
3. Ứng dụng **cơ sở dữ liệu đồ thị Neo4j** để phân tích hành vi, sở thích của người dùng, từ đó đưa ra
   các gợi ý bài thi và lộ trình học tập cá nhân hoá phù hợp cho từng cá nhân.
4. **Kiểm thử, đánh giá hiệu năng chịu tải thời gian thực** của hệ thống (phối hợp Spring WebSocket và
   Redis) cũng như **độ chính xác của mô hình AI**.

Bốn mục tiêu này là bốn trụ cột của đồ án và được đối chiếu lại ở phần Kết luận.

## 3. Nội dung nghiên cứu

Để đạt các mục tiêu trên, đồ án thực hiện những nội dung sau:

**Về mặt lý thuyết và công nghệ:**

- Nghiên cứu kiến trúc ứng dụng web phân lớp với Spring Boot (Java 21) ở phía máy chủ và React 19 +
  TypeScript ở phía trình duyệt; nguyên tắc tách tầng Controller → Service → Repository.
- Nghiên cứu giao tiếp thời gian thực bằng WebSocket với giao thức STOMP, kết hợp Redis Pub/Sub để đồng
  bộ trạng thái phòng đấu.
- Nghiên cứu kiến trúc RAG: quy trình nạp học liệu (trích xuất văn bản → chia đoạn → sinh vector nhúng →
  lưu vào pgvector) và quy trình truy xuất (tìm kiếm theo độ tương đồng ngữ nghĩa), cùng cách tích hợp
  LLM có cơ chế dự phòng giữa nhiều nhà cung cấp.
- Nghiên cứu mô hình dữ liệu đồ thị trên Neo4j và ngôn ngữ truy vấn Cypher cho bài toán gợi ý.

**Về mặt phân tích và thiết kế:**

- Khảo sát nhu cầu người dùng và phân tích các nền tảng tương tự (Kahoot!, Quizizz) để xác định yêu cầu.
- Xác định tác nhân, đặc tả use case, thiết kế cơ sở dữ liệu quan hệ và đồ thị, thiết kế giao diện.

**Về mặt hiện thực và đánh giá:**

- Hiện thực hệ thống theo phương pháp **lát cắt dọc**: hoàn thiện trọn một chức năng từ cơ sở dữ liệu,
  máy chủ, giao diện cho tới kiểm thử, rồi mới chuyển sang chức năng kế tiếp.
- Kiểm thử chức năng bằng JUnit 5, Mockito và Testcontainers; kiểm thử chịu tải phòng đấu thời gian
  thực; đánh giá độ chính xác của các chức năng AI.

## 4. Phạm vi nghiên cứu

**Về hình thức triển khai:** ứng dụng web, gồm một giao diện đơn trang (SPA) bằng React và một máy chủ
Spring Boot cung cấp REST API, WebSocket và SSE. Đồ án không phát triển ứng dụng di động riêng.

**Về nghiệp vụ quiz:** hỗ trợ năm loại câu hỏi — một đáp án (single-choice), nhiều đáp án
(multiple-choice), đúng/sai (true/false), điền vào chỗ trống (fill-in-blank) và trả lời ngắn/tự luận
(short-answer). Ba chế độ chơi: luyện tập cá nhân, làm bài tính giờ, và phòng đấu thời gian thực nhiều
người.

**Về trí tuệ nhân tạo:** ba chức năng AI được hiện thực — sinh đề từ học liệu, chấm và giải thích câu tự
luận, trợ lý học tập hỏi–đáp bám học liệu. Nhà cung cấp mô hình chính là **Google Gemini**, dự phòng là
**Groq**; mọi lời gọi đi qua một lớp điều phối tự viết. Đồ án **không huấn luyện mô hình mới**, mà
tập trung vào kiến trúc tích hợp, khả năng bám nguồn (grounding) và cơ chế dự phòng.

**Về gợi ý cá nhân hoá:** dùng Neo4j để mô hình hoá quan hệ giữa người học, chủ đề và bài thi; gợi ý dựa
trên hành vi làm bài và điểm yếu theo chủ đề.

**Về các chức năng mở rộng:** hệ thống có thiết kế cho một số chức năng mở rộng (ôn tập bằng flashcard
kết hợp lặp lại ngắt quãng, chống gian lận khi thi, gamification, lớp học và giao bài, bảng xếp hạng theo
mùa, thông báo nhắc ôn). Các chức năng này được đặc tả nhưng **không** thuộc phạm vi hiện thực bắt buộc
của đồ án; phần nào chưa hoàn thành được nêu trung thực ở Kết luận.

**Về quy mô đánh giá:** hệ thống được kiểm thử trên môi trường máy đơn dùng Docker Compose, chưa triển
khai và đo trên hạ tầng nhiều máy chủ với người dùng thật ở quy mô lớn.

## 5. Kết quả mong muốn

1. Website ứng dụng Quiz/Trivia vận hành ổn định, đồng bộ dữ liệu thời gian thực chính xác giữa các
   người chơi trong cùng phòng đấu.
2. Trợ lý AI giải đáp được kiến thức trong học liệu **có dẫn nguồn**, và module tự động tạo bộ câu hỏi
   trắc nghiệm đạt chuẩn cấu trúc, có bước người dùng duyệt trước khi xuất bản.
3. Hệ thống gợi ý dựa trên đồ thị cho ra kết quả gợi ý bài thi và lộ trình học theo năng lực người dùng.
4. Bộ tài liệu phân tích thiết kế chi tiết cùng mã nguồn bảo đảm tính bảo mật, có kiểm thử tự động, và
   có khả năng ứng dụng thực tiễn trong giáo dục cũng như giải trí trực tuyến.
5. Số liệu đo thực nghiệm về hiệu năng chịu tải phòng đấu thời gian thực và về độ chính xác của các chức
   năng AI.

## 6. Bố cục đề tài

Ngoài phần Mở đầu, Kết luận và Tài liệu tham khảo, nội dung báo cáo được trình bày trong ba chương:

**Chương 1 — Tổng quan về ứng dụng Quiz/Trivia tích hợp AI.** Trình bày cơ sở lý thuyết và các công nghệ
được sử dụng: hệ thống quiz trực tuyến và phân tích các nền tảng tương tự; trí tuệ nhân tạo tạo sinh và
mô hình ngôn ngữ lớn; kiến trúc RAG; Spring Boot; React và TypeScript; PostgreSQL cùng phần mở rộng
pgvector; Neo4j; Redis; WebSocket với STOMP; Docker; và kiến trúc client–server phân lớp của hệ thống.

**Chương 2 — Khảo sát, phân tích và thiết kế hệ thống.** Trình bày kết quả khảo sát nhu cầu, các yêu cầu
chức năng và phi chức năng, xác định tác nhân, biểu đồ và đặc tả use case, thiết kế cơ sở dữ liệu quan hệ
và đồ thị, cùng thiết kế giao diện.

**Chương 3 — Thực nghiệm và đánh giá.** Trình bày môi trường triển khai, giao diện thực tế của hệ thống,
kết quả kiểm thử chức năng, kết quả đánh giá hiệu năng chịu tải thời gian thực và kết quả đánh giá độ
chính xác của các chức năng AI.
