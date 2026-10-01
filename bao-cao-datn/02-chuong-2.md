# CHƯƠNG 2. PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG

Trên cơ sở yêu cầu và công nghệ đã trình bày ở Chương 1, chương này phân tích và thiết kế hệ thống Quiz AI: xác định tác nhân, mô hình hóa và đặc tả use case, hiện thực hóa use case bằng biểu đồ trình tự và biểu đồ lớp; sau đó thiết kế cơ sở dữ liệu, kiến trúc và giao diện.

## 2.1. Phân tích hệ thống

### 2.1.1. Mô tả bài toán và tác nhân

Hệ thống cần cho phép người tạo nội dung quản lý quiz và ngân hàng câu hỏi, nạp học liệu và sinh đề tự động từ học liệu đó, mở phòng đấu trí và xem thống kê kết quả; cho phép người học làm bài cá nhân, tham gia phòng đấu nhiều người theo thời gian thực, hỏi trợ lý học tập và nhận gợi ý bài thi theo năng lực; cho phép quản trị viên quản lý tài khoản, nội dung và giám sát chi phí gọi mô hình AI. Ngoài ra, khách chưa đăng nhập cần xem được nội dung giới thiệu để biết hệ thống có gì trước khi quyết định đăng ký. Hệ thống có bốn tác nhân (Bảng 2.1).

**Bảng 2.1. Các tác nhân của hệ thống**

| Tác nhân | Vai trò | Chức năng chính |
|----------|---------|-----------------|
| Khách | — | Xem danh sách và thông tin giới thiệu quiz công khai (tiêu đề, mô tả, danh mục, độ khó, số câu); đăng ký, đăng nhập; vào phòng đấu bằng mã PIN khi chủ phòng cho phép |
| Người học | LEARNER | Làm bài cá nhân; tham gia phòng đấu; hỏi trợ lý học tập; nhận gợi ý bài thi và lộ trình học; xem tiến độ và lịch sử làm bài; quản lý hồ sơ |
| Người tạo nội dung | CREATOR | Toàn bộ chức năng của người học, thêm: quản lý quiz và ngân hàng câu hỏi; nạp và chia sẻ học liệu; sinh đề bằng AI và duyệt câu hỏi; mở và điều khiển phòng đấu; chấm tay câu tự luận; xem thống kê quiz của mình |
| Quản trị viên | ADMIN | Toàn bộ chức năng của người tạo nội dung, thêm: quản lý tài khoản người dùng; quản lý toàn bộ nội dung; cấu hình nhà cung cấp AI; giám sát nhật ký và chi phí gọi mô hình |

Ba tác nhân đã đăng nhập có quan hệ tổng quát hóa: vai trò CREATOR bao hàm toàn bộ quyền của LEARNER, ADMIN bao hàm toàn bộ quyền của CREATOR. Nhờ vậy một người dùng vừa tạo nội dung vừa làm bài và dùng trợ lý học tập bình thường, không cần hai tài khoản.

Nguyên tắc chung của hệ thống là mọi hành vi tạo ra dữ liệu học tập đều yêu cầu tài khoản; khách chỉ được duyệt nội dung công khai và **không xem được nội dung câu hỏi** để tránh lộ đề. Hệ quả kỹ thuật là cột người dùng trong bảng lượt làm bài không cho phép rỗng — không có lượt làm bài ẩn danh, nên mọi thống kê, bảng xếp hạng và đồ thị gợi ý đều gắn với một người dùng thật.

Phòng đấu là ngoại lệ có chủ đích: khách biết mã PIN **và** được chủ phòng bật tùy chọn cho khách thì vào chơi được, vì tình huống thực tế là quét mã QR trong lớp học, không phải ai cũng có tài khoản. Đổi lại, khách dùng khóa phiên riêng chỉ mở đúng một phòng và dữ liệu của họ chỉ sống trong một ván: không có lịch sử làm bài, không vào thống kê cá nhân, không lên đồ thị gợi ý. Tùy chọn này mặc định tắt.

### 2.1.2. Biểu đồ use case

Biểu đồ use case tổng quát (Hình 2.1) thể hiện quan hệ giữa bốn tác nhân và các nhóm chức năng, trong đó có quan hệ tổng quát hóa giữa ba vai trò đã đăng nhập.

[HÌNH 2.1: Biểu đồ use case tổng quát của hệ thống — cần chèn]

Bảng 2.2 liệt kê các use case tiêu biểu theo tác nhân.

**Bảng 2.2. Danh sách use case tiêu biểu**

| Mã | Use case | Tác nhân |
|----|----------|----------|
| UC-01 | Đăng ký, đăng nhập, đặt lại mật khẩu | Khách |
| UC-02 | Tìm kiếm và xem giới thiệu quiz công khai | Khách, Người học |
| UC-03 | Quản lý quiz và ngân hàng câu hỏi | Người tạo nội dung |
| UC-04 | Nạp và chia sẻ học liệu | Người tạo nội dung |
| UC-05 | Làm bài quiz cá nhân | Người học |
| UC-06 | Tham gia phòng đấu thời gian thực | Người học, Khách |
| UC-07 | Sinh đề bằng AI từ học liệu | Người tạo nội dung |
| UC-08 | Chấm và giải thích câu tự luận bằng AI | Hệ thống, Người tạo nội dung |
| UC-09 | Hỏi trợ lý học tập | Người học, Người tạo nội dung |
| UC-10 | Nhận gợi ý bài thi và lộ trình học | Người học |
| UC-11 | Mở và điều khiển phòng đấu | Người tạo nội dung |
| UC-12 | Xem tiến độ học tập và lịch sử làm bài | Người học |
| UC-13 | Xem thống kê quiz của mình | Người tạo nội dung |
| UC-14 | Quản lý người dùng và giám sát chi phí AI | Quản trị viên |
| UC-15 | Ôn tập thẻ ghi nhớ theo lịch lặp lại ngắt quãng | Người học |
| UC-16 | Quản lý lớp học, giao bài và theo dõi nộp bài | Người tạo nội dung, Người học |
| UC-17 | Xem báo cáo tính toàn vẹn và kết luận về một lượt thi | Người tạo nội dung, Quản trị viên |
| UC-18 | Xem thành tích: cấp độ, huy hiệu, chuỗi ngày học, thử thách | Người học |
| UC-19 | Chốt mùa xếp hạng và trao phần thưởng | Hệ thống (lịch thời gian), Người học |
| UC-20 | Nhận và quản lý thông báo nhắc ôn tập | Hệ thống (lịch thời gian), Người học |

Sáu use case cuối bảng thuộc các nhóm chức năng mở rộng. Hai trong số đó — UC-19 và UC-20 — có tác nhân là **lịch thời gian** chứ không phải người dùng thao tác trực tiếp: chúng do công việc định kỳ khởi động, và người học chỉ là bên nhận kết quả. UC-17 đáng chú ý ở chỗ khác: hệ thống chỉ **cung cấp dữ kiện**, còn kết luận một lượt thi hợp lệ hay không luôn do con người đưa ra.

### 2.1.3. Đặc tả use case

Phần này đặc tả năm use case chính, bao quát cả bốn tác nhân và bốn trụ cột của đề tài: phòng đấu thời gian thực, sinh đề bằng RAG, trợ lý học tập bám học liệu và gợi ý dựa trên đồ thị. Mỗi use case có một biểu đồ use case riêng, lần lượt từ Hình 2.2 đến Hình 2.6; **bảng đặc tả đầy đủ của cả năm use case — tác nhân, tiền điều kiện, luồng chính, luồng thay thế và hậu điều kiện — trình bày ở Phụ lục A**.

#### 2.1.3.1. Use case Đăng nhập

[HÌNH 2.2: Biểu đồ use case Đăng nhập — cần chèn]

#### 2.1.3.2. Use case Tham gia phòng đấu thời gian thực

[HÌNH 2.3: Biểu đồ use case Tham gia phòng đấu thời gian thực — cần chèn]

#### 2.1.3.3. Use case Sinh đề bằng AI từ học liệu

[HÌNH 2.4: Biểu đồ use case Sinh đề bằng AI từ học liệu — cần chèn]

#### 2.1.3.4. Use case Hỏi trợ lý học tập

[HÌNH 2.5: Biểu đồ use case Hỏi trợ lý học tập — cần chèn]

#### 2.1.3.5. Use case Nhận gợi ý bài thi và lộ trình học

[HÌNH 2.6: Biểu đồ use case Nhận gợi ý bài thi và lộ trình học — cần chèn]

### 2.1.4. Biểu đồ lớp của hệ thống

Hình 2.7 thể hiện biểu đồ lớp thiết kế cho các lớp thực thể cốt lõi của hệ thống cùng quan hệ kết hợp và bội số giữa chúng: một người dùng sở hữu nhiều quiz, nhiều học liệu và nhiều phòng đấu; một quiz thuộc một danh mục và liên kết nhiều câu hỏi qua lớp trung gian có thứ tự; một câu hỏi có nhiều phương án trả lời; một học liệu chia thành nhiều đoạn có vector nhúng; một quiz có nhiều lượt làm bài, mỗi lượt có nhiều câu trả lời; một phòng đấu có nhiều người chơi; một người dùng có nhiều phiên hội thoại với trợ lý, mỗi phiên có nhiều tin nhắn.

[HÌNH 2.7: Biểu đồ lớp thiết kế tổng thể — cần chèn]

### 2.1.5. Hiện thực hóa use case

Phần này hiện thực hóa năm use case chính nêu trên. Theo phương pháp phân tích hướng đối tượng, mỗi use case được mô hình bằng biểu đồ trình tự (tương tác giữa các đối tượng theo thời gian) và biểu đồ lớp phân tích VOPC (View Of Participating Classes — các lớp tham gia theo ba khuôn mẫu «boundary», «control», «entity»).

#### 2.1.5.1. Use case Đăng nhập

Người dùng nhập thông tin trên lớp biên `LoginPage`; lớp điều khiển `AuthService` truy vấn lớp thực thể `User` qua `UserRepository`, so khớp mật khẩu rồi gọi `JwtService` sinh access token và `RefreshTokenService` lưu phiên vào Redis (Hình 2.8, 2.9).

[HÌNH 2.8: Biểu đồ trình tự use case Đăng nhập — cần chèn]

[HÌNH 2.9: Biểu đồ lớp VOPC use case Đăng nhập — cần chèn]

#### 2.1.5.2. Use case Tham gia phòng đấu thời gian thực

Lớp biên `RoomPage` kết nối qua STOMP; `StompAuthChannelInterceptor` xác thực JWT tại khung CONNECT. `RoomService` quản lý vòng đời phòng, `RoomStateStore` giữ trạng thái đang chơi trên Redis, `SpeedScorer` tính điểm theo tốc độ, `GameEventPublisher` xuất bản sự kiện qua Redis Pub/Sub và `GameEventRelay` ở mỗi tiến trình máy chủ phát tiếp tới người chơi đang kết nối với nó; kết quả cuối ván ghi vào thực thể `GameRoom` và `GameRoomPlayer` (Hình 2.10, 2.11).

[HÌNH 2.10: Biểu đồ trình tự use case Tham gia phòng đấu thời gian thực — cần chèn]

[HÌNH 2.11: Biểu đồ lớp VOPC use case Tham gia phòng đấu thời gian thực — cần chèn]

#### 2.1.5.3. Use case Sinh đề bằng AI từ học liệu

Luồng RAG gồm hai pha. Pha nạp học liệu: `MaterialService` nhận tệp, `TextExtractor` (Apache Tika) bóc tách văn bản, `TextChunker` chia đoạn, `MaterialIngestionService` gọi `AiOrchestrator` sinh vector nhúng rồi lưu qua `MaterialChunkRepository`. Pha sinh đề: `AiJobService` tạo công việc nền và trả mã công việc; `QuestionGenerationService` truy hồi các đoạn liên quan, `QuestionPromptBuilder` dựng prompt, `AiOrchestrator` gọi `GeminiProvider` (dự phòng `GroqProvider`), `QuestionJsonParser` kiểm chứng kết quả trước khi lưu câu hỏi nháp; `AiRequestLogger` ghi nhật ký lời gọi (Hình 2.12, 2.13).

[HÌNH 2.12: Biểu đồ trình tự use case Sinh đề bằng AI từ học liệu — cần chèn]

[HÌNH 2.13: Biểu đồ lớp VOPC use case Sinh đề bằng AI từ học liệu — cần chèn]

#### 2.1.5.4. Use case Hỏi trợ lý học tập

Lớp biên `AssistantPage` gửi câu hỏi và nhận luồng SSE. `ChatService` gọi `AiOrchestrator` sinh vector nhúng cho câu hỏi, dùng `MaterialChunkRepository` truy hồi các đoạn **trong phạm vi được phép đọc** (tài liệu của người gọi hoặc đã chia sẻ), lọc theo ngưỡng khoảng cách, `ChatPromptBuilder` dựng prompt kèm lịch sử phiên, rồi `AiOrchestrator.stream` phát từng mảnh chữ về giao diện; hội thoại lưu vào `ChatSession` và `ChatMessage` (Hình 2.14, 2.15).

[HÌNH 2.14: Biểu đồ trình tự use case Hỏi trợ lý học tập — cần chèn]

[HÌNH 2.15: Biểu đồ lớp VOPC use case Hỏi trợ lý học tập — cần chèn]

#### 2.1.5.5. Use case Nhận gợi ý bài thi và lộ trình học

Lớp biên `RecommendationPage` gọi `RecommendationService`; lớp này dùng `RecommendationRepository` chạy các truy vấn Cypher trên Neo4j để tìm chủ đề người học còn yếu (từ quan hệ `PRACTICED`), các quiz thuộc chủ đề đó chưa từng làm (loại trừ theo quan hệ `ATTEMPTED`) và các quiz mà nhóm người học tương tự đã làm; kết quả được hợp nhất, xếp hạng kèm lý do gợi ý rồi trả về (Hình 2.16, 2.17).

[HÌNH 2.16: Biểu đồ trình tự use case Nhận gợi ý bài thi và lộ trình học — cần chèn]

[HÌNH 2.17: Biểu đồ lớp VOPC use case Nhận gợi ý bài thi và lộ trình học — cần chèn]

## 2.2. Thiết kế hệ thống

### 2.2.1. Thiết kế cơ sở dữ liệu

Hệ thống áp dụng nguyên tắc lưu trữ đa hệ với ba hệ quản trị, mỗi hệ đảm nhiệm loại dữ liệu phù hợp với đặc tính của nó: PostgreSQL 16 cho dữ liệu nghiệp vụ có tính giao dịch và kho vector học liệu, Neo4j 5 cho đồ thị hành vi phục vụ gợi ý, Redis cho dữ liệu ngắn hạn và thông điệp thời gian thực.

Cơ sở dữ liệu quan hệ được thiết kế theo các quy ước: dùng kiểu `uuid` làm khóa chính cho mọi bảng; đặt tên theo quy ước snake_case; cột thời gian dùng `timestamptz`; kiểu liệt kê lưu dạng `varchar` kèm ràng buộc `CHECK` thay vì kiểu enum của PostgreSQL để việc bổ sung giá trị mới không cần thay đổi kiểu; dữ liệu phi cấu trúc lưu `jsonb`. Mọi thay đổi lược đồ thực hiện qua migration Flyway được đánh số và không sửa lại tệp đã áp dụng, nhờ đó cơ sở dữ liệu ở mọi môi trường dựng lại được từ đầu một cách xác định. Hình 2.18 thể hiện sơ đồ thực thể quan hệ tổng quan. Để hình đọc được ở khổ giấy, sơ đồ **không vẽ các đường nối tới bảng `users`**: gần như mọi bảng đều có khóa ngoại `user_id` hoặc `owner_id` trỏ về `users`, và vẽ đủ thì bảng này trở thành một trục có hai mươi bảng treo vào, khiến sơ đồ dàn ngang và mất khả năng đọc. Các quan hệ đó vẫn tồn tại đầy đủ trong lược đồ.

[HÌNH 2.18: Sơ đồ thực thể quan hệ (ERD) tổng quan — cần chèn]

Lược đồ quan hệ gồm 35 bảng trên PostgreSQL, tạo qua 23 tệp migration Flyway được đánh số và tổ chức theo nhóm chức năng. **Bảng B.1 ở Phụ lục B liệt kê đầy đủ các nhóm này kèm mô tả từng bảng.**

Nhóm bảng chống gian lận có bốn đặc điểm thiết kế xuất phát từ **ràng buộc đạo đức** chứ không từ nhu cầu kỹ thuật, nên cần nêu rõ. Thứ nhất, hai bảng này **chỉ có dữ liệu cho lượt thi tính điểm**; lượt luyện tập không sinh dòng nào, và máy chủ từ chối tín hiệu gửi lên cho lượt luyện tập. Thứ hai, cột chi tiết được máy chủ **dựng lại từ một danh sách trường vô hại** thay vì lưu nguyên gói tin của phía trình duyệt — phía trình duyệt đã chỉ đọc độ dài đoạn dán rồi bỏ chuỗi đi, nhưng nếu chỉ có một lớp bảo vệ thì một bản mã nguồn phía người dùng bị sửa đủ để nội dung chảy vào cơ sở dữ liệu. Thứ ba, cột trạng thái rà soát mặc định là *chờ rà soát* và **không có đường nào để hệ thống tự đổi giá trị đó**: tín hiệu thu từ trình duyệt có thể bị chặn hoặc giả mạo, nên chúng chỉ là cảnh báo hỗ trợ quyết định của con người. Giao diện phản ánh đúng điều này — mọi báo cáo đều hiện kèm một câu nhắc rằng điểm rủi ro không phải bằng chứng gian lận, và câu nhắc đó đặt ngay cạnh con số chứ không ở cuối trang. Thứ tư, **người thi được biết mình đang bị ghi nhận cái gì**: thông báo đầy đủ hiện ở trang giới thiệu quiz kèm ô xác nhận đã đọc — tức trước khi đồng hồ chạy, khi họ còn kịp đóng bớt tab hay chọn chỗ ngồi — và trong lúc làm bài có một dòng đếm số lần đã ghi nhận. Dòng đếm dùng chữ *đã ghi nhận* chứ không phải *vi phạm*, vì rời trang một lần do thông báo bật lên không phải gian lận, và người có tư cách kết luận điều đó là giáo viên chứ không phải hệ thống. Hai loại tín hiệu cố ý không hiện cho người thi: sao chép đề bài — việc bình thường của người học nghiêm túc — và *trả lời nhanh bất thường*, vốn là một suy đoán của hệ thống chứ không phải hành động người thi tự biết mình vừa làm.

Riêng bảng `material_chunks` có một đặc điểm thiết kế cần nêu rõ: cột vector nhúng **không** được lập chỉ mục xấp xỉ. Nguyên nhân đã trình bày ở mục 1.3.2 — truy vấn RAG phải lọc quyền đọc trước rồi mới xếp theo khoảng cách, trong khi chỉ mục xấp xỉ làm ngược lại nên bỏ sót kết quả một cách im lặng. Ở quy mô vài trăm tới vài nghìn đoạn, quét tuần tự trên tập đã lọc quyền vừa nhanh vừa không bỏ sót; khi kho vượt cỡ vài chục nghìn đoạn mới cần chỉ mục xấp xỉ, và lúc đó phải bật kèm cơ chế quét lặp của pgvector để chỉ mục tự tìm thêm khi bộ lọc quyền loại bớt ứng viên.

Mô hình đồ thị trên Neo4j gồm ba loại nút `User`, `Quiz`, `Topic` và ba loại quan hệ `ATTEMPTED`, `PRACTICED`, `COVERS` như đã trình bày ở mục 1.3.5; ràng buộc duy nhất trên định danh của cả ba loại nút được tạo lúc ứng dụng khởi động, thiếu bước này thì lệnh `MERGE` vẫn chạy nhưng quét toàn bộ nút mỗi lần và chậm dần theo kích thước đồ thị mà không có triệu chứng gì. Dữ liệu trên Redis gồm trạng thái phòng đang chơi, kênh xuất bản sự kiện ván đấu, khóa phiên khách vãng lai, refresh token cùng chỉ mục ngược từ người dùng tới các phiên của họ, mã OTP đặt lại mật khẩu cùng bộ đếm số lần thử sai, bộ đếm hạn mức gọi AI theo ngày, mốc tạm ngừng gọi nhà cung cấp AI đang quá tải, bộ đệm lời giải thích lý do gợi ý, bảng xếp hạng mùa đang chạy dạng tập hợp có thứ tự, và các khóa chống trùng của thông báo. Điểm chung của mọi khóa trên là chúng **dựng lại được**: Redis giữ chỉ mục và trạng thái ngắn hạn, PostgreSQL giữ nguồn sự thật.

Sơ đồ Hình 2.18 chỉ vẽ nhóm dữ liệu lõi. Các bảng của chức năng mở rộng không xuất hiện trên sơ đồ đó vì cùng lý do đã nêu với `users`: gần như mọi bảng trong ba nhóm cuối đều chỉ nối về `users` bằng một cạnh duy nhất, nên vẽ đủ 35 bảng chỉ làm sơ đồ dàn ngang mà không thêm thông tin nào về cấu trúc.

### 2.2.2. Thiết kế kiến trúc và mô-đun

Mã nguồn phía máy chủ được tổ chức theo nghiệp vụ dưới gói gốc `com.datn.quizai`, gồm các mô-đun `auth`, `user`, `quiz`, `attempt`, `file`, `realtime`, `ai`, `chat`, `recommend`, `analytics`, `admin`, `flashcard`, `gamification`, `season`, `integrity`, `classroom`, `notification`, cùng `common` (thực thể cơ sở, kiểm tra quyền sở hữu, DTO dùng chung, xử lý ngoại lệ) và `config` (cấu hình bảo mật, WebSocket, Redis Pub/Sub, tài liệu API). Nguyên tắc tổ chức là **nhóm theo tính năng, bên trong mỗi tính năng mới chia theo tầng** (`controller`, `service`, `repository`, `domain`, `dto`); nhờ vậy sửa một tính năng chỉ cần mở một thư mục mà ranh giới các tầng vẫn rõ. Hai gói `common` và `config` không chia theo tầng vì không phải tính năng nghiệp vụ. Thư mục kiểm thử phản chiếu đúng cấu trúc này.

Quan hệ phụ thuộc là một chiều Controller → Service → Repository → Domain. Controller không chứa logic nghiệp vụ và không bao giờ trả trực tiếp thực thể ra API mà chuyển qua DTO. Riêng lớp tích hợp AI được cô lập sau interface `AiProvider` cùng lớp điều phối `AiOrchestrator`, nên việc đổi hoặc thêm nhà cung cấp không ảnh hưởng tầng nghiệp vụ.

Phía giao diện cũng tổ chức theo tính năng dưới `src/features/<tên tính năng>`, mỗi tính năng gồm `api` (lời gọi máy chủ), `hooks` (logic dùng lại), `components`, `pages` và `store` khi cần; phần dùng chung đặt ở `src/shared`.

### 2.2.3. Thiết kế giao diện

Giao diện tuân theo một bộ quy ước thống nhất để mỗi trang không mang một phong cách riêng: màu sắc, bo góc và đổ bóng khai báo tập trung dưới dạng biến thay vì viết trực tiếp trong từng thành phần; nút hành động chính dùng một màu nhấn duy nhất trên toàn hệ thống; trang dành cho người học trình bày theo lưới thẻ, trang quản lý theo bảng; tiêu đề trang và trạng thái danh sách rỗng dùng lại thành phần chung. Một quy tắc riêng: giao diện **không hiển thị dữ liệu không có thật**. Các nền tảng thương mại thường hiện điểm đánh giá và số lượt học; hệ thống này chưa thu thập những số đó nên không sinh ra để trang trông phong phú hơn.

Hai bố cục vừa nêu được minh họa bằng hai màn tiêu biểu: trang khám phá quiz đại diện lưới thẻ của khu học tập (Hình 2.19), trang quản lý người dùng đại diện bảng của khu quản trị (Hình 2.20).

[HÌNH 2.19: Thiết kế giao diện trang khám phá quiz — cần chèn]

[HÌNH 2.20: Thiết kế giao diện quản lý người dùng (khu quản trị) — cần chèn]

Các màn còn lại không đưa bản phác vào đây, vì Chương 3 đã có ảnh chụp của chính chúng sau khi hiện thực: làm bài (Hình 3.3), kết quả làm bài (Hình 3.4), phòng đấu (Hình 3.4), học liệu và sinh đề (Hình 3.5), trợ lý học tập (Hình 3.6), gợi ý và lộ trình học (Hình 3.7), giám sát AI (Hình 3.8).

**Khu quản trị dùng khung giao diện riêng.** Khu học tập dùng thanh điều hướng ngang; khu quản trị dùng thanh điều hướng dọc nền tối. Đây là quyết định thiết kế, không phải khác biệt thẩm mỹ: thao tác ở khu học tập chỉ tác động lên dữ liệu của chính người dùng, còn khóa tài khoản hay đổi vai trò tác động lên người khác và không có nút hoàn tác, nên một bố cục khác hẳn giúp quản trị viên luôn nhận biết mình đang ở khu nào. Ngoài ra hai khu là hai ngữ cảnh làm việc khác nhau, và thanh dọc còn chỗ mở rộng khi bổ sung chức năng quản trị. Lối vào đặt ở menu tài khoản, lối ra đặt trong thanh dọc — vào khu quản trị là chuyển ngữ cảnh chứ không phải điều hướng trong cùng một ngữ cảnh.

## 2.3. Kết luận chương 2

Chương 2 đã phân tích bài toán và xác định bốn tác nhân cùng quan hệ tổng quát hóa giữa chúng, xây dựng biểu đồ use case tổng quát và đặc tả năm use case chính bao quát bốn trụ cột của đề tài; hiện thực hóa cả năm use case đó bằng biểu đồ trình tự và biểu đồ lớp VOPC theo chuẩn UML; đồng thời thiết kế biểu đồ lớp tổng thể, cơ sở dữ liệu trên ba hệ quản trị, kiến trúc mô-đun và giao diện. Trong quá trình đặc tả, các luồng thay thế đã được nêu rõ cho những tình huống mà hệ thống buộc phải xử lý đúng: chuyển nhà cung cấp AI khi lỗi tạm thời, giữ điểm khi người chơi mất kết nối giữa ván, trả lời "không biết" khi không có học liệu liên quan, và không gợi ý bừa khi chưa có dữ liệu hành vi. Các thiết kế này là cơ sở trực tiếp cho việc xây dựng, thử nghiệm và đánh giá hệ thống ở Chương 3.
