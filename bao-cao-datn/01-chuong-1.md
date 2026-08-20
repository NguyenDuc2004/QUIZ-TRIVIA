# CHƯƠNG 1. TỔNG QUAN VỀ ĐỀ TÀI

Chương này trình bày kết quả khảo sát hiện trạng các nền tảng quiz trực tuyến và các giải pháp ứng dụng AI liên quan, từ đó xác định yêu cầu của hệ thống Quiz AI; đồng thời giới thiệu cơ sở lý thuyết và công nghệ được sử dụng để xây dựng hệ thống.

## 1.1. Khảo sát hiện trạng

### 1.1.1. Khảo sát các nền tảng quiz trực tuyến hiện có

Trên thị trường hiện có nhiều nền tảng hỗ trợ tạo và tổ chức bài trắc nghiệm trực tuyến với mức độ tính năng khác nhau. Bảng 1.1 so sánh một số nền tảng tiêu biểu theo các tiêu chí liên quan trực tiếp đến ba bài toán của đề tài.

**Bảng 1.1. So sánh một số nền tảng quiz trực tuyến hiện có**

| Nền tảng | Thi đấu thời gian thực | Sinh đề từ học liệu bằng AI | Chấm câu tự luận | Gợi ý theo năng lực | Chi phí |
|----------|:----------------------:|:---------------------------:|:----------------:|:-------------------:|---------|
| Kahoot! | Rất mạnh, là điểm nhấn chính | Không | Không | Hạn chế | Freemium |
| Quizizz | Có, theo mã tham gia phòng | Không | Không | Có luyện lại câu sai | Freemium |
| Google Forms | Không | Không | Không (chỉ so khớp chuỗi) | Không | Miễn phí |
| Moodle Quiz | Không | Không (chỉ lấy ngẫu nhiên từ ngân hàng có sẵn) | Chấm tay | Không | Mã nguồn mở |
| Azota | Không | Không | Chấm tay | Không | Có gói trả phí |

Kết quả khảo sát cho thấy các nền tảng phổ biến đã giải quyết tốt phần số hóa quy trình: tạo câu hỏi, phát đề, chấm điểm tự động và tổ chức phòng chơi theo mã tham gia. Tuy nhiên còn ba khoảng trống. Thứ nhất, không nền tảng nào cho phép người dạy nạp giáo trình của mình rồi sinh câu hỏi từ chính tài liệu đó — phần tốn nhiều công sức nhất khi ra đề. Thứ hai, loại câu trả lời ngắn và tự luận hoặc không được hỗ trợ, hoặc chỉ so khớp chuỗi máy móc, hoặc phải chấm tay; điều này khiến loại câu hỏi có giá trị đánh giá cao nhất bị loại khỏi các nền tảng tự động hóa. Thứ ba, việc gợi ý nội dung học tiếp chủ yếu dựa trên lựa chọn của người học hoặc mức độ phổ biến của bài thi, chưa dựa trên mô hình năng lực suy ra từ dữ liệu hành vi. Ba khoảng trống này là hướng mà đề tài nhắm tới, trong đó cơ chế phòng đấu theo mã tham gia — vốn đã được kiểm chứng hiệu quả — được kế thừa và giữ nguyên.

### 1.1.2. Khảo sát giải pháp AI trong sinh đề, chấm bài và trợ lý học tập

Về sinh đề tự động, hướng tiếp cận trực tiếp là yêu cầu mô hình ngôn ngữ lớn sinh câu hỏi từ một chủ đề cho trước; tuy nhiên cách làm này dễ dẫn tới câu hỏi lệch phạm vi hoặc chứa thông tin không có trong tài liệu (hiện tượng ảo giác — hallucination). Kỹ thuật RAG [1] khắc phục bằng cách truy hồi các đoạn văn bản liên quan từ học liệu nguồn rồi đưa vào ngữ cảnh của mô hình, nhờ đó câu hỏi sinh ra bám sát nội dung và có thể kèm trích dẫn nguồn để kiểm chứng. Một hướng thay thế là tinh chỉnh (fine-tuning) mô hình trên tài liệu riêng, song không phù hợp với bài toán này: mỗi lần người dùng nạp tài liệu mới lại phải huấn luyện lại, chi phí cao, và mô hình vẫn không nêu được nguồn của câu trả lời.

Về chấm câu trả lời ngắn, cách làm truyền thống là so khớp chuỗi hoặc so khớp từ khóa, nhưng cách này không nhận ra hai câu diễn đạt khác nhau mà cùng nghĩa. Mô hình ngôn ngữ lớn xử lý được điều đó, song lại phát sinh một rủi ro đặc thù: đây là chỗ duy nhất mà nội dung do người học tự gõ đi thẳng vào ngữ cảnh của mô hình, nên người học có thể chèn chỉ thị nhằm điều khiển mô hình cho điểm cao (tấn công tiêm chỉ thị — prompt injection). Vì vậy giải pháp chấm bằng AI phải đi kèm nhiều lớp phòng ngừa, trong đó lớp cuối là ràng buộc cứng miền giá trị điểm số ở phía hệ thống.

Về trợ lý học tập, các trợ lý hội thoại thông dụng trả lời dựa trên kiến thức đã học của mô hình nên không nắm được học liệu riêng của từng lớp, và không cho biết câu trả lời lấy từ đâu. Trong bối cảnh ôn thi, một câu trả lời sai nhưng trôi chảy còn tệ hơn việc không trả lời, bởi người học chưa nắm kiến thức thì không có cơ sở để nghi ngờ. Do đó trợ lý trong đề tài được xây dựng trên cùng nền RAG với chức năng sinh đề, kèm hai ràng buộc: chỉ trả lời dựa trên học liệu đã truy hồi, và luôn trả về danh sách tài liệu đã dựa vào để người dùng đối chiếu.

Về gợi ý cá nhân hóa, các thuật toán lọc cộng tác trên mô hình quan hệ thuần đòi hỏi nhiều phép tự kết phức tạp khi cần duyệt qua nhiều bậc quan hệ (người học → bài thi → chủ đề → bài thi khác). Cơ sở dữ liệu đồ thị [7] lưu quan hệ như một đối tượng có thể duyệt trực tiếp, nhờ đó câu truy vấn diễn đạt gần với cách phát biểu bài toán và chi phí duyệt không phụ thuộc tổng kích thước dữ liệu.

## 1.2. Yêu cầu hệ thống

### 1.2.1. Yêu cầu chức năng

- Xác thực và phân quyền: đăng ký và đăng nhập bằng email kèm mật khẩu; đăng nhập bằng tài khoản Google theo luồng ID token; đặt lại mật khẩu qua mã OTP gửi email; xác thực bằng JWT ngắn hạn kết hợp refresh token có xoay vòng; phân quyền theo bốn tác nhân kết hợp kiểm tra quyền sở hữu tài nguyên; đăng xuất khỏi một thiết bị hoặc mọi thiết bị.
- Quản lý quiz và câu hỏi: người tạo nội dung thêm, sửa, xóa quiz kèm danh mục, độ khó, thời lượng, chế độ hiển thị công khai hoặc riêng tư và ảnh bìa; quản lý ngân hàng câu hỏi dùng lại được với năm loại câu hỏi (một đáp án, nhiều đáp án, đúng/sai, điền chỗ trống, trả lời ngắn); mỗi câu có chủ đề, điểm, lời giải thích và tiêu chí chấm; tìm kiếm và lọc quiz theo từ khóa, danh mục, độ khó.
- Làm bài cá nhân: hai chế độ luyện tập và làm bài tính giờ; đề được chốt tại thời điểm bắt đầu để việc chỉnh sửa quiz sau đó không ảnh hưởng bài đang làm; tự động lưu câu trả lời ngay khi người học chọn; chấm tự động các loại câu có đáp án xác định; hết giờ tự chuyển trạng thái hết hạn; xem lại kết quả kèm đáp án đúng, lời giải thích và lịch sử làm bài.
- Phòng đấu trí thời gian thực: chủ phòng mở phòng từ một quiz, nhận mã PIN sáu ký tự và mã QR để chia sẻ; người chơi tham gia bằng mã PIN, chọn biệt danh và ảnh đại diện; máy chủ phát câu hỏi đồng thời tới mọi người chơi; tính điểm theo độ chính xác kết hợp tốc độ trả lời; bảng xếp hạng cập nhật trực tiếp sau mỗi câu; giữ nguyên điểm khi người chơi mất kết nối rồi vào lại; chủ phòng có thể cho khách chưa có tài khoản vào chơi (mặc định tắt).
- Sinh đề tự động bằng RAG: nạp học liệu từ tệp PDF, DOCX, TXT hoặc dán trực tiếp văn bản; hệ thống bóc tách văn bản, chia đoạn, sinh vector nhúng và lưu trên pgvector; sinh câu hỏi theo chủ đề, độ khó, loại và số lượng yêu cầu; tác vụ chạy nền và trả về mã công việc để theo dõi trạng thái; câu hỏi sinh ra kèm đoạn học liệu nguồn và chỉ vào ngân hàng sau khi người tạo nội dung duyệt.
- Chấm và giải thích câu tự luận bằng AI: chấm câu trả lời ngắn dựa trên đáp án mẫu và tiêu chí chấm; trả về điểm kèm nhận xét và gợi ý cải thiện; ràng buộc cứng điểm trong miền hợp lệ của câu; cho phép chủ quiz chấm tay ghi đè; khi gọi mô hình thất bại thì chuyển sang trạng thái dừng rõ ràng thay vì treo ở trạng thái đang chấm.
- Trợ lý học tập: hỏi đáp bám học liệu, hội thoại giữ được ngữ cảnh; phản hồi theo luồng để giảm thời gian tới chữ đầu tiên; mỗi câu trả lời kèm danh sách tài liệu đã dựa vào; khi không có học liệu đủ liên quan thì trả lời rõ là không biết thay vì suy đoán; lưu và mở lại được lịch sử hội thoại.
- Gợi ý cá nhân hóa bằng Neo4j: gợi ý quiz theo chủ đề người học còn yếu và chưa từng làm; gợi ý theo hành vi của những người học có kết quả tương tự; lộ trình học đề xuất thứ tự chủ đề nên ôn; đồng bộ dữ liệu hành vi từ cơ sở dữ liệu quan hệ sang đồ thị sau mỗi lượt nộp bài và dựng lại được đồ thị từ lịch sử.
- Thống kê và báo cáo: tiến độ học tập cá nhân theo chủ đề; thống kê theo từng quiz cho chủ sở hữu; nhật ký các lần gọi AI kèm số token, độ trễ và nhà cung cấp phục vụ giám sát chi phí.
- Quản trị hệ thống: quản lý tài khoản người dùng và nội dung; cấu hình nhà cung cấp AI; giám sát nhật ký và chi phí gọi mô hình.

### 1.2.2. Yêu cầu phi chức năng

Các yêu cầu phi chức năng quan trọng được tổng hợp trong Bảng 1.2.

**Bảng 1.2. Yêu cầu phi chức năng của hệ thống**

| Loại | Yêu cầu | Mức ngưỡng |
|------|---------|-----------|
| Hiệu năng | Thời gian phản hồi API nghiệp vụ thông thường | < 500 ms (p95) |
| Hiệu năng thời gian thực | Độ trễ đồng bộ sự kiện trong phòng đấu | đo và báo cáo ở mục 3.5 |
| Khả năng chịu tải | Số người chơi đồng thời trong một phòng đấu | đo và báo cáo ở mục 3.5 |
| Hiệu năng AI | Thời gian sinh một bộ câu hỏi từ học liệu | chạy nền, trả mã công việc ngay |
| Độ tin cậy | Nhà cung cấp AI chính lỗi tạm thời | tự chuyển sang nhà cung cấp dự phòng |
| Độ tin cậy | Cơ sở dữ liệu đồ thị không phản hồi | API gợi ý trả danh sách rỗng, không ảnh hưởng việc nộp bài |
| Bảo mật | Băm mật khẩu và mã OTP, ký JWT | BCrypt; khóa bí mật ≥ 32 ký tự; access token 15 phút |
| Bảo mật | Thu hồi phiên đăng nhập | đổi mật khẩu thu hồi phiên trên mọi thiết bị |
| Bảo mật AI | Chống tiêm chỉ thị qua nội dung người dùng | tách chỉ dẫn hệ thống khỏi dữ liệu; ràng buộc cứng miền điểm |
| Cô lập dữ liệu | Phạm vi truy xuất học liệu | chỉ tài liệu của chính người gọi hoặc tài liệu đã được chia sẻ |
| Khả năng mở rộng | Chạy nhiều tiến trình máy chủ | API không trạng thái; trạng thái phòng đặt ở Redis kèm Pub/Sub |
| Khả dụng | Giao diện tiếng Việt, bố cục nhất quán | dùng lại bộ thành phần và hệ màu tập trung |
| Trung thực dữ liệu | Không hiển thị số liệu không có thật | không bịa điểm đánh giá, số lượt học |
| Bảo trì | Kiểm tra kiểu và xác thực dữ liệu vào | TypeScript strict (giao diện), Bean Validation (máy chủ) |
| Bảo trì | Quản lý phiên bản lược đồ cơ sở dữ liệu | Flyway; không sửa migration đã áp dụng |

## 1.3. Cơ sở lý thuyết và công nghệ sử dụng

### 1.3.1. Kiến trúc và nền tảng phát triển

Hệ thống được xây dựng theo kiến trúc khối đơn mô-đun hóa (Modular Monolith) với các tầng Controller — Service — Repository — Domain; mã nguồn chia theo nghiệp vụ để dễ bảo trì và có thể tách thành dịch vụ riêng về sau. Hệ thống dùng ba kênh giao tiếp, mỗi kênh cho một dạng dữ liệu: REST cho nghiệp vụ thông thường, WebSocket cho phòng đấu thời gian thực, và SSE (Server-Sent Events) cho luồng trả lời của trợ lý học tập. Phần máy chủ thiết kế không trạng thái với JWT trong header. Hình 1.1 thể hiện kiến trúc tổng thể.

Phần máy chủ dùng Java 21 (LTS) và Spring Boot 3.x [3] (Spring Web, Spring Security, Spring Data JPA, Spring Data Neo4j, Spring WebSocket) cùng Apache Tika [11] (bóc tách tài liệu), Flyway (quản lý phiên bản lược đồ), Jakarta Bean Validation, Resilience4j (cầu dao khi gọi AI) và springdoc-openapi (sinh tài liệu API). Phần giao diện dùng React 19 + Vite 8 + TypeScript [4] ở chế độ strict, Ant Design v6 cho thành phần giao diện kết hợp Tailwind CSS v4 cho bố cục, TanStack Query và Zustand quản lý trạng thái, React Hook Form kết hợp Zod cho biểu mẫu, và `@stomp/stompjs` cho kết nối thời gian thực. Hệ thống dùng ba cơ sở dữ liệu theo nguyên tắc lưu trữ đa hệ (polyglot persistence): PostgreSQL 16 [5] mở rộng bằng pgvector [6] cho dữ liệu nghiệp vụ và kho vector học liệu, Neo4j 5 [7] cho đồ thị hành vi, và Redis [8] cho dữ liệu ngắn hạn cùng thông điệp thời gian thực.

[HÌNH 1.1: Kiến trúc tổng thể hệ thống — cần chèn]

Một vector nhúng (embedding) là dãy số thực biểu diễn ý nghĩa ngữ nghĩa của một đoạn văn bản trong không gian nhiều chiều (mô hình embedding của Gemini sinh vector 768 chiều); hai đoạn có nội dung gần nhau sẽ có vector gần nhau theo khoảng cách cosine. pgvector bổ sung kiểu dữ liệu `vector` cùng các toán tử tính khoảng cách vào PostgreSQL, cho phép lưu và truy vấn vector ngay trong cơ sở dữ liệu quan hệ. Việc dùng chung một cơ sở dữ liệu cho cả dữ liệu nghiệp vụ và kho vector là lựa chọn có chủ ý: một hệ quản trị vector chuyên biệt sẽ nhanh hơn ở quy mô rất lớn, nhưng đổi lại mất khả năng lọc quyền truy cập cùng lúc với tìm kiếm vector trong một truy vấn — điều kiện bắt buộc của hệ thống này, bởi mỗi lần truy xuất đều phải giới hạn trong phạm vi tài liệu người gọi được phép đọc.

### 1.3.2. Sinh đề tự động và trợ lý học tập bằng kỹ thuật RAG

RAG (Retrieval-Augmented Generation) [1] kết hợp truy hồi thông tin với sinh văn bản bằng mô hình ngôn ngữ lớn: thay vì để mô hình tự sinh dựa trên kiến thức đã học, RAG bổ sung vào ngữ cảnh các đoạn văn bản liên quan truy hồi từ học liệu, nhờ đó nội dung bám sát nguồn và truy vết được. Trong hệ thống, RAG là nền chung cho hai chức năng: sinh đề từ học liệu và trợ lý học tập. Quy trình gồm hai pha (Hình 1.2).

Pha lập chỉ mục xử lý học liệu người dùng nạp lên: Apache Tika [11] bóc tách văn bản từ PDF, DOCX và TXT; bộ chia đoạn cắt văn bản thành các đoạn có phần chồng lấp và cắt theo ranh giới câu để không làm gãy ngữ nghĩa; mỗi đoạn được sinh vector nhúng 768 chiều và lưu vào bảng `material_chunks` cùng trạng thái xử lý của tài liệu. Toàn bộ pha này chạy dưới dạng công việc nền vì thời gian xử lý phụ thuộc kích thước tài liệu.

Pha truy hồi và sinh nhận câu hỏi hoặc yêu cầu của người dùng, sinh vector nhúng cho nó, truy hồi các đoạn gần nhất theo khoảng cách cosine (toán tử `<=>` của pgvector) **trong phạm vi tài liệu người gọi được phép đọc**, loại bỏ các đoạn có khoảng cách vượt ngưỡng liên quan, ghép các đoạn còn lại thành ngữ cảnh, dựng prompt và gọi mô hình sinh. Các tham số chính của pipeline được tổng hợp trong Bảng 1.3.

[HÌNH 1.2: Pipeline RAG cho sinh đề và trợ lý học tập — cần chèn]

**Bảng 1.3. Tham số chính của pipeline RAG**

| Tham số | Giá trị | Ghi chú |
|---------|---------|---------|
| Định dạng học liệu | PDF, DOCX, TXT, văn bản dán tay | bóc tách bằng Apache Tika |
| Giới hạn kích thước tài liệu | 10 MB | vượt ngưỡng thì báo lỗi rõ ràng |
| Mô hình embedding | Gemini embedding | vector 768 chiều |
| Số đoạn truy hồi (top-K) | 5 | theo khoảng cách cosine `<=>` |
| Ngưỡng khoảng cách | 0,75 | loại đoạn không đủ liên quan |
| Phạm vi truy hồi | tài liệu của người gọi hoặc đã được chia sẻ | lọc quyền **trước** khi xếp hạng |
| Mô hình sinh | Gemini (`gemini-3.6-flash`) | dự phòng Groq (`openai/gpt-oss-120b`) |
| Số câu sinh mỗi lần | tối đa 20 | giới hạn để kiểm soát chi phí |

Hai biện pháp bảo đảm câu trả lời bám nguồn (grounding). Thứ nhất là **ngưỡng khoảng cách**: truy vấn vector luôn trả về đủ số đoạn được yêu cầu, kể cả khi không đoạn nào liên quan tới câu hỏi — đoạn "gần nhất" trong một kho toàn tài liệu Toán vẫn là một đoạn Toán khi người dùng hỏi về Lịch sử. Nếu không lọc, prompt sẽ chứa ngữ cảnh sai và mô hình sẽ cố trả lời từ đó; vì vậy hệ thống loại mọi đoạn vượt ngưỡng, và khi không còn đoạn nào thì prompt nói rõ là không có tài liệu liên quan để mô hình trả lời "không biết" thay vì đoán. Thứ hai là **trả kèm trích dẫn**: mỗi câu trả lời đi cùng danh sách tài liệu và đoạn văn bản đã dựa vào.

Một điểm quan trọng về thứ tự xử lý trong truy vấn vector: bộ lọc quyền đọc phải được áp dụng **trước** khi xếp hạng theo khoảng cách và cắt lấy top-K, chứ không phải sau. Nếu dùng chỉ mục xấp xỉ (approximate nearest neighbor) theo cách thông thường, hệ quản trị sẽ lấy K đoạn gần nhất trên toàn kho rồi mới lọc quyền trên K dòng đó; những đoạn không được phép đọc bị loại mà không có gì bù lại, nên kết quả trả về ít hơn K và thường rỗng — sai lệch xảy ra hoàn toàn im lặng, không sinh lỗi hay cảnh báo. Với quy mô kho học liệu của đề tài, hệ thống lọc quyền trong một biểu thức bảng chung được vật chất hóa rồi mới tính khoảng cách, tức tìm kiếm chính xác trên đúng tập được phép đọc.

Về tích hợp mô hình, hệ thống gọi trực tiếp REST API của nhà cung cấp qua `WebClient` và tự hiện thực lớp điều phối `AiOrchestrator`, không dùng các thư viện trừu tượng hóa sẵn, nhằm kiểm soát trực tiếp cơ chế dự phòng, hạn mức và nhật ký — ba điểm cần đo và báo cáo. Khóa API là bí mật nên mọi lời gọi đều thực hiện từ máy chủ, và khóa được truyền trong header thay vì trên chuỗi truy vấn. Prompt gồm ba thành phần: chỉ dẫn hệ thống (yêu cầu chỉ dùng thông tin trong tài liệu, ràng buộc theo loại câu hỏi, bắt buộc kèm giải thích), phần ngữ cảnh là các đoạn học liệu đã truy hồi được rào trong khối dữ liệu riêng, và lược đồ JSON đầu ra. Kết quả trả về được phân tích và kiểm chứng theo lược đồ, các câu sai cấu trúc bị loại; chỉ những câu người tạo nội dung duyệt mới vào ngân hàng — cơ chế con người ở vòng cuối (human-in-the-loop). Do nhà cung cấp giới hạn số lượt gọi mỗi phút, hệ thống đọc đúng thời gian chờ trong thân phản hồi lỗi thay vì tự suy đoán, và cho hàng đợi công việc AI chạy tuần tự để hai công việc cùng thức dậy sau một lần chờ không lại cùng bị từ chối.

### 1.3.3. Chấm và giải thích câu trả lời ngắn bằng AI

Với các loại câu hỏi có đáp án xác định, hệ thống chấm bằng logic thuần Java. Riêng câu trả lời ngắn được chấm bằng mô hình ngôn ngữ lớn dựa trên ba đầu vào: nội dung câu hỏi, đáp án mẫu và tiêu chí chấm (rubric) do người soạn cung cấp. Tiêu chí chấm là thành phần quan trọng: thiếu nó thì mô hình tự nghĩ ra thang điểm riêng và hai lần chấm cùng một bài có thể lệch nhau. Kết quả trả về gồm điểm số, nhận xét về bài làm và gợi ý việc cần làm để khá hơn; hai phần sau tách riêng để giao diện nhấn mạnh khác nhau.

Đây là bề mặt tấn công lớn nhất của hệ thống, vì khác với học liệu do người tạo nội dung nạp, bài làm là nội dung do chính người học tự gõ rồi đi thẳng vào prompt. Hệ thống áp dụng bốn lớp phòng ngừa tiêm chỉ thị. Lớp thứ nhất, bài làm được rào trong một khối dữ liệu có dấu mở và đóng riêng. Lớp thứ hai, chỉ dẫn hệ thống nói rõ rằng câu lệnh xuất hiện bên trong khối đó là nội dung cần chấm, và bài chỉ chứa những câu như vậy thì nhận không điểm. Lớp thứ ba, nếu người học tự gõ đúng chuỗi rào để "đóng" khối dữ liệu rồi viết chỉ thị ở bên ngoài, chuỗi đó bị vô hiệu hóa trước khi dựng prompt. Lớp thứ tư là hàng rào cuối: **điểm mô hình trả về luôn bị ràng buộc về miền từ 0 tới điểm tối đa của câu**, nên dù ba lớp trên thủng và mô hình nghe theo chỉ thị, điểm vẫn không vượt được trần thật của câu.

Ngoài ra, việc ghi đè điểm là hành động của con người: sau khi người tạo nội dung đã chấm tay, kết quả AI trả về muộn hơn sẽ bị bỏ qua. Khi gọi mô hình thất bại, câu chuyển sang một trạng thái dừng riêng thay vì nằm mãi ở trạng thái đang chờ chấm — không có trạng thái này thì người học thấy "đang chấm" vĩnh viễn mà không ai biết là đã hỏng.

### 1.3.4. Phòng đấu thời gian thực với WebSocket và Redis

Giao thức HTTP hoạt động theo mô hình yêu cầu — phản hồi: máy chủ chỉ gửi dữ liệu khi trình duyệt hỏi. Muốn cập nhật liên tục, cách thô sơ là hỏi lại theo chu kỳ (polling), nhưng cách này tạo độ trễ trung bình bằng nửa chu kỳ và sinh nhiều yêu cầu vô ích. WebSocket [9] mở một kết nối song công duy trì liên tục, cho phép cả hai phía chủ động gửi dữ liệu bất cứ lúc nào. Trên nền đó, giao thức STOMP bổ sung lớp ngữ nghĩa mà WebSocket thuần không có: khái niệm đích đến, cơ chế đăng ký theo chủ đề và khung tin có tiêu đề; nhờ vậy ứng dụng chỉ cần cho người chơi đăng ký chủ đề của phòng rồi gửi tin tới đó, thay vì tự định nghĩa định dạng tin và tự quản lý danh sách người nhận.

Việc xác thực được thực hiện tại khung STOMP CONNECT chứ không phải lúc bắt tay HTTP, vì trình duyệt không cho phép gắn tiêu đề tùy ý vào yêu cầu nâng cấp WebSocket. Thành viên xác thực bằng JWT; khách vãng lai dùng một khóa phiên riêng gắn chặt với đúng một phòng, không phải JWT nên không mở được bất kỳ API nào khác.

Redis [8] đảm nhiệm bốn vai trò. Thứ nhất, lưu trạng thái phòng đang chơi (câu hỏi hiện tại, điểm từng người): trạng thái này thay đổi liên tục trong vài phút rồi hết giá trị, nên ghi vào cơ sở dữ liệu quan hệ mỗi lần đổi là tốn kém không cần thiết; chỉ kết quả cuối ván mới ghi xuống. Thứ hai, đồng bộ thời gian thực qua cơ chế xuất bản — đăng ký (Pub/Sub): khi một người chơi trả lời, sự kiện cập nhật điểm được xuất bản để mọi tiến trình máy chủ đang giữ kết nối của phòng đó đều nhận được và phát tiếp cho người chơi của mình — đây là điều kiện để hệ thống chạy nhiều tiến trình mà người chơi trong cùng phòng vẫn thấy trạng thái nhất quán. Thứ ba, quản lý phiên đăng nhập: mỗi refresh token là một khóa có thời gian sống, nhờ đó phiên thu hồi được — điều mà JWT tự thân không làm được. Thứ tư, lưu bộ đệm và bộ đếm hạn mức cho các lời gọi mô hình.

Cách tính điểm theo tốc độ khiến độ trễ trở thành yêu cầu chức năng chứ không chỉ là chỉ tiêu kỹ thuật: nếu điểm phụ thuộc thời gian trả lời thì độ trễ không đều giữa các người chơi sẽ trực tiếp gây bất công về điểm số. Đây cũng là lý do đề tài đặt yêu cầu đo hiệu năng chịu tải thời gian thực, trình bày ở mục 3.5.

### 1.3.5. Gợi ý cá nhân hóa bằng cơ sở dữ liệu đồ thị Neo4j

Neo4j [7] lưu dữ liệu dưới dạng nút và quan hệ, cả hai đều mang thuộc tính. Khác với mô hình quan hệ — nơi liên kết giữa các bảng được suy ra lúc truy vấn qua phép kết — trong đồ thị, quan hệ là đối tượng được lưu trực tiếp và duyệt được với chi phí không phụ thuộc tổng kích thước dữ liệu. Ngôn ngữ truy vấn Cypher mô tả mẫu cần tìm bằng cú pháp gợi hình: `(a:User)-[:ATTEMPTED]->(q:Quiz)<-[:ATTEMPTED]-(b:User)` diễn tả "hai người dùng cùng làm một bài thi".

Mô hình đồ thị của hệ thống gồm ba loại nút là `User`, `Quiz`, `Topic` và ba loại quan hệ: `ATTEMPTED` (người học đã làm bài thi, kèm điểm và độ chính xác), `PRACTICED` (năng lực của người học trên một chủ đề) và `COVERS` (bài thi bao gồm chủ đề nào). Mô hình này đã được lược bớt có chủ ý so với bản thiết kế ban đầu, theo hai nguyên tắc. Nguyên tắc thứ nhất: cạnh giữ *sự thật đo được*, truy vấn giữ *cách diễn giải* — các quan hệ kiểu "yếu ở chủ đề" thực chất chỉ là `PRACTICED` nhìn qua một ngưỡng, mà đưa ngưỡng vào cạnh thì mỗi lần đổi ngưỡng phải dựng lại toàn bộ đồ thị, còn để ngưỡng ở truy vấn thì đổi lúc nào cũng được. Nguyên tắc thứ hai: không lưu quan hệ mà hệ thống không có nguồn dữ liệu để suy ra — quan hệ "chủ đề tiên quyết" bị loại vì không ai khai báo chủ đề nào phải học trước chủ đề nào, và tự sinh quan hệ đó là hệ thống bịa ra kiến thức sư phạm mà nó không có.

Về đồng bộ dữ liệu, nguồn sự thật là PostgreSQL còn Neo4j chỉ là bản chiếu phục vụ phân tích; hệ quả thực tế là đồ thị lệch hoặc mất thì dựng lại được từ lịch sử làm bài, nên không cần giao dịch hai pha, chỉ cần các thao tác bất biến theo số lần chạy (idempotent). Sau mỗi lượt nộp bài, hệ thống phát sự kiện ở pha sau khi giao dịch được ghi nhận để khởi động công việc nền đồng bộ; việc đồng bộ chạy lần thứ hai sau khi AI chấm xong câu tự luận, vì lúc mới nộp những câu đó còn không điểm nên năng lực tính ra chưa đúng. Toàn bộ thao tác dùng `MERGE`, và năng lực theo chủ đề được tính lại từ đầu trên toàn bộ lịch sử thay vì cộng dồn — cộng dồn thì chạy hai lần là số liệu nhân đôi. Cuối cùng, cơ sở dữ liệu đồ thị không phản hồi cũng không được làm hỏng việc nộp bài: việc đồng bộ chạy nền và nuốt lỗi, còn API gợi ý trả về danh sách rỗng thay vì lỗi hệ thống.

### 1.3.6. Bảo mật

Xác thực dựa trên JWT [10] với access token thời hạn ngắn (15 phút) mang thông tin định danh và vai trò, kết hợp refresh token thời hạn dài lưu ở Redis và được xoay vòng: mỗi lần gia hạn, token cũ bị thu hồi và cấp cặp mới. Mỗi lần đăng nhập cấp một refresh token riêng nên nhiều thiết bị dùng song song được mà không đá nhau; đăng xuất chỉ thu hồi phiên của thiết bị đó, trong khi **đổi mật khẩu thu hồi phiên trên mọi thiết bị** — người dùng đổi mật khẩu thường tin rằng mình vừa cắt hết truy cập, hệ thống phải làm đúng điều đó. Mật khẩu và mã OTP đặt lại mật khẩu đều băm bằng BCrypt trước khi lưu; mã OTP sinh bằng bộ sinh số giả ngẫu nhiên an toàn, sống mười phút, dùng một lần, sai quá năm lần thì hủy và giãn cách sáu mươi giây giữa hai lần xin mã. Với đăng nhập bằng Google, hệ thống xác minh ID token bằng thư viện chính chủ, kiểm tra chữ ký, tổ chức phát hành, hạn dùng và đặc biệt là định danh ứng dụng nhận token — bỏ bước cuối thì một token hợp lệ cấp cho ứng dụng khác cũng đăng nhập được vào hệ thống.

Phân quyền theo mô hình RBAC bốn tác nhân, kiểm soát ở tầng phương thức bằng `@PreAuthorize` kết hợp kiểm tra quyền sở hữu tài nguyên thay vì chỉ kiểm tra vai trò. Khi người dùng truy cập tài nguyên của người khác, hệ thống trả về mã 404 thay vì 403 để không tiết lộ sự tồn tại của tài nguyên. Hệ thống phòng ngừa các rủi ro theo OWASP Top 10 [12]: truy vấn tham số hóa ở cả JPA và Cypher chống tiêm mã; làm sạch nội dung do người dùng và AI sinh chống XSS; CSRF được vô hiệu hóa do máy chủ không trạng thái và dùng Bearer token thay vì cookie; CORS chỉ cho phép nguồn của giao diện; nhận dạng ảnh tải lên bằng chữ ký byte thay vì tin vào `Content-Type` do máy khách khai, tên tệp do máy chủ sinh từ UUID nên không có đường tấn công vượt thư mục; và giới hạn tần suất các endpoint AI.

Riêng về bảo mật AI, ngoài bốn lớp chống tiêm chỉ thị khi chấm bài đã trình bày ở mục 1.3.3, hệ thống áp dụng thêm bốn biện pháp: không gửi dữ liệu nhạy cảm tới mô hình; cô lập học liệu giữa các tài khoản bằng cách lọc quyền đọc trong mọi truy vấn tương đồng; giữ nguyên nguyên tắc con người ở vòng cuối để nội dung AI sinh ra không tự vào ngân hàng câu hỏi; và ghi nhật ký mọi lời gọi kèm số token, độ trễ, nhà cung cấp phục vụ giám sát chi phí. Nhật ký này được ghi trong một giao dịch riêng, nên công việc chính thất bại và bị quay lui thì bản ghi giám sát vẫn còn.

### 1.3.7. Docker và môi trường phát triển

Docker [13] là nền tảng ảo hóa ở mức ứng dụng: mỗi ứng dụng cùng phụ thuộc của nó được đóng vào một container dùng chung nhân hệ điều hành của máy chủ, nên nhẹ và khởi động nhanh hơn máy ảo. Docker Compose khai báo một tập nhiều container cùng cấu hình mạng và ổ lưu trữ trong một tệp, rồi bật tất cả bằng một lệnh.

Hệ thống cần đồng thời ba dịch vụ dữ liệu là PostgreSQL 16 kèm pgvector, Neo4j 5 và Redis 7; cài đặt thủ công cả ba vừa tốn thời gian vừa dễ lệch phiên bản, nên toàn bộ được khai báo trong Docker Compose với phiên bản ghi thẳng trong tệp cấu hình. Việc dùng container còn phục vụ kiểm thử tự động: các bài kiểm thử tích hợp dùng Testcontainers để dựng PostgreSQL thật có pgvector cho mỗi lần chạy, nhờ đó kiểm thử truy vấn vector trên đúng hệ quản trị sẽ dùng khi triển khai thay vì thay bằng cơ sở dữ liệu trong bộ nhớ có hành vi khác.

Kết luận chương 1. Chương 1 đã khảo sát hiện trạng các nền tảng quiz trực tuyến và chỉ ra ba khoảng trống về sinh đề từ học liệu, chấm câu tự luận và gợi ý theo năng lực; xác định yêu cầu chức năng và phi chức năng của hệ thống; đồng thời trình bày cơ sở lý thuyết và công nghệ gồm kiến trúc phân lớp, kiến trúc RAG cùng các biện pháp bảo đảm bám nguồn, cơ chế chấm bài an toàn trước tiêm chỉ thị, đồng bộ thời gian thực bằng WebSocket và Redis, mô hình đồ thị cho gợi ý cá nhân hóa, bảo mật và môi trường triển khai. Các nội dung này là nền tảng cho việc phân tích và thiết kế hệ thống ở Chương 2.
