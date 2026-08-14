# MỞ ĐẦU

## 1. Lý do chọn đề tài

Học tập trực tuyến đã trở thành một phần thường trực của giáo dục Việt Nam, kéo theo nhu cầu về những công cụ ôn tập vừa hiệu quả vừa duy trì được động lực cho người học. Trong số đó, hình thức trắc nghiệm hóa việc ôn tập (quiz) được sử dụng rộng rãi nhờ ba đặc điểm: câu hỏi ngắn giúp người học giữ được sự tập trung, kết quả trả về ngay nên người học biết mình sai ở đâu, và cơ chế thi đấu — tính điểm theo tốc độ, bảng xếp hạng trực tiếp — tạo ra động lực mà bài tập truyền thống khó có. Các nền tảng như Kahoot! và Quizizz đã chứng minh mô hình này hiệu quả trong lớp học.

Tuy vậy, khi xem xét các nền tảng hiện có dưới góc nhìn của người ôn thi dài hạn thay vì người chơi một buổi, có thể thấy ba khoảng trống. Thứ nhất, việc soạn đề vẫn là công việc thủ công tốn nhiều thời gian: giáo viên có sẵn giáo trình và tài liệu ôn tập, nhưng để chuyển khối tài liệu đó thành bộ câu hỏi trắc nghiệm thì phải đọc lại, chọn ý, đặt câu hỏi và soạn phương án nhiễu; các nền tảng hiện tại hỗ trợ nhập câu hỏi rất tốt nhưng gần như không hỗ trợ sinh câu hỏi từ chính học liệu của người dùng. Thứ hai, phản hồi cho người học còn dừng ở mức đúng hoặc sai; với câu trả lời ngắn và câu tự luận — loại câu có giá trị đánh giá cao — việc chấm và giải thích đòi hỏi giáo viên đọc từng bài, nên trên các nền tảng tự động hóa loại câu này thường bị bỏ qua. Thứ ba, gợi ý nội dung chưa gắn với năng lực thật của người học, phần lớn dựa trên lựa chọn của chính người học hoặc mức độ phổ biến của bài thi, trong khi dữ liệu hành vi thực chất là quan hệ nhiều chiều giữa người học, chủ đề và bài thi.

Sự phát triển của các mô hình ngôn ngữ lớn (Large Language Model — LLM) mở ra hướng giải quyết cho hai khoảng trống đầu, với điều kiện kiểm soát được hiện tượng mô hình sinh ra nội dung không có trong tài liệu (ảo giác — hallucination). Kỹ thuật Truy hồi tăng cường sinh (Retrieval-Augmented Generation — RAG) [1] khắc phục hạn chế này bằng cách truy hồi các đoạn văn bản liên quan từ học liệu rồi đưa vào ngữ cảnh của mô hình, nhờ đó nội dung sinh ra bám sát nguồn và kèm được trích dẫn để kiểm chứng. Với khoảng trống thứ ba, cơ sở dữ liệu đồ thị [7] biểu diễn quan hệ hành vi một cách tự nhiên hơn mô hình quan hệ thuần, giúp truy vấn gợi ý viết ra gần đúng như cách phát biểu bài toán. Xuất phát từ thực tiễn đó, em lựa chọn đề tài "Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo".

## 2. Mục đích của đề tài

Đề tài hướng tới việc làm chủ công nghệ Spring Boot (Java 21), React 19 + TypeScript, PostgreSQL 16 kết hợp pgvector, cơ sở dữ liệu đồ thị Neo4j, Redis và tích hợp trí tuệ nhân tạo tạo sinh (Google Gemini) qua kỹ thuật RAG; đồng thời nghiên cứu quy trình phân tích, thiết kế và xây dựng một hệ thống Quiz/Trivia hoàn chỉnh với bốn trọng tâm: (1) phòng đấu trí nhiều người chơi theo thời gian thực với độ trễ thấp, (2) tự động sinh cấu trúc đề thi từ học liệu và trợ lý học tập bám học liệu qua kiến trúc RAG, (3) phân tích hành vi người dùng bằng Neo4j để gợi ý bài thi và lộ trình học cá nhân hóa, và (4) kiểm thử, đánh giá hiệu năng chịu tải thời gian thực cùng độ chính xác của các chức năng AI.

## 3. Đối tượng và phạm vi nghiên cứu

Đối tượng nghiên cứu là quy trình ôn tập và kiểm tra đánh giá bằng hình thức trắc nghiệm trực tuyến, cùng các kỹ thuật phục vụ ba bài toán: đồng bộ trạng thái thời gian thực giữa nhiều người chơi, sinh nội dung có kiểm soát nguồn bằng mô hình ngôn ngữ lớn, và gợi ý cá nhân hóa dựa trên đồ thị hành vi.

Phạm vi nghiên cứu là xây dựng một ứng dụng web với bốn tác nhân — khách chưa đăng nhập, người học, người tạo nội dung và quản trị viên — ở mức cơ bản nhưng hoàn chỉnh, gồm: xác thực và phân quyền theo vai trò; quản lý quiz và ngân hàng câu hỏi với năm loại câu hỏi; làm bài cá nhân có tính giờ, tự lưu câu trả lời và chấm tự động; phòng đấu trí nhiều người chơi theo thời gian thực có tính điểm theo tốc độ và bảng xếp hạng trực tiếp; nạp học liệu và sinh đề tự động bằng RAG có bước người dùng duyệt; chấm và giải thích câu trả lời ngắn bằng AI; trợ lý học tập hỏi đáp bám học liệu có trích dẫn nguồn; gợi ý bài thi và lộ trình học dựa trên đồ thị Neo4j; cùng thống kê tiến độ học tập.

Về mô hình trí tuệ nhân tạo, đề tài không huấn luyện mô hình mới mà tập trung vào kiến trúc tích hợp, khả năng bám nguồn và cơ chế dự phòng giữa các nhà cung cấp. Hệ thống sử dụng Google Gemini làm nhà cung cấp chính và xAI Grok làm nhà cung cấp dự phòng. Một số chức năng mở rộng đã được đặc tả nhưng không thuộc phạm vi hiện thực bắt buộc, gồm: ôn tập bằng thẻ ghi nhớ kết hợp lặp lại ngắt quãng, chống gian lận khi thi, trò chơi hóa, lớp học và giao bài, bảng xếp hạng theo mùa, thông báo nhắc ôn tập. Hệ thống được triển khai và đánh giá trên môi trường máy đơn dùng Docker Compose, không hướng tới sản phẩm thương mại.

## 4. Ý nghĩa khoa học và thực tiễn

Về mặt khoa học, đề tài nghiên cứu và ứng dụng kỹ thuật RAG — kết hợp truy hồi ngữ nghĩa trên cơ sở dữ liệu vector và mô hình ngôn ngữ lớn — vào hai bài toán khác nhau là sinh câu hỏi trắc nghiệm và hỏi đáp học liệu, trong đó đặc biệt chú trọng vấn đề bảo đảm câu trả lời bám nguồn có kiểm chứng. Đề tài cũng nghiên cứu mô hình dữ liệu đồ thị cho bài toán gợi ý cá nhân hóa, và kiến trúc đồng bộ trạng thái thời gian thực bằng WebSocket kết hợp Redis Pub/Sub cho phòng đấu nhiều người chơi. Đây đều là những hướng ứng dụng thời sự trong lĩnh vực công nghệ giáo dục.

Về mặt thực tiễn, hệ thống giúp người dạy giảm đáng kể thời gian soạn đề bằng cách sinh bộ câu hỏi trực tiếp từ tài liệu giảng dạy sẵn có, đồng thời cho phép sử dụng loại câu trả lời ngắn — vốn có giá trị đánh giá cao nhưng khó tự động hóa — nhờ cơ chế chấm và giải thích bằng AI. Với người học, hệ thống cung cấp một trợ lý giải đáp bám học liệu kèm trích dẫn nguồn để tự kiểm chứng, cùng gợi ý bài thi theo đúng chủ đề còn yếu thay vì theo mức độ phổ biến. Cơ chế phòng đấu thời gian thực bổ sung yếu tố động lực cần thiết cho việc ôn tập lâu dài.

## 5. Kết cấu của báo cáo

Ngoài phần mở đầu, kết luận và tài liệu tham khảo, nội dung báo cáo gồm ba chương:

- Chương 1 — Tổng quan về đề tài: trình bày kết quả khảo sát hiện trạng các nền tảng quiz trực tuyến và các giải pháp AI liên quan, xác định yêu cầu của hệ thống, đồng thời trình bày cơ sở lý thuyết và công nghệ được sử dụng.
- Chương 2 — Phân tích và thiết kế hệ thống: phân tích yêu cầu, mô hình hóa use case, đặc tả và hiện thực hóa use case bằng biểu đồ trình tự và biểu đồ lớp; thiết kế cơ sở dữ liệu, kiến trúc và giao diện.
- Chương 3 — Thử nghiệm và đánh giá: trình bày quá trình cài đặt, kết quả sản phẩm, kiểm thử chức năng, đánh giá hiệu năng chịu tải thời gian thực và độ chính xác của các thành phần AI.
