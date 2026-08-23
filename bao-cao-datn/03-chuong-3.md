# CHƯƠNG 3. THỰC NGHIỆM VÀ ĐÁNH GIÁ

Chương 2 đã trình bày bản thiết kế của hệ thống. Chương này trình bày kết quả hiện thực bản thiết kế đó: môi trường triển khai, giao diện đã hoàn thành, kết quả kiểm thử chức năng, và hai phép đo bắt buộc theo phiếu giao đề tài — hiệu năng của phòng đấu thời gian thực và độ chính xác của các chức năng trí tuệ nhân tạo.

Toàn bộ số liệu trong hai mục 3.5 và 3.6 đến từ những lần chạy đo cụ thể, có ghi ngày và có mã kịch bản đo kèm theo trong mã nguồn. Phần cuối mỗi mục nêu rõ **điều phép đo không chứng minh được**, vì một con số đứng một mình dễ bị đọc rộng hơn phạm vi nó bảo đảm.

## 3.1. Môi trường triển khai

Hệ thống chạy trên một máy đơn, ba hệ quản trị dữ liệu dựng bằng Docker Compose, máy chủ ứng dụng và giao diện chạy trực tiếp trên máy chủ phát triển.

**Bảng 3.1. Môi trường triển khai và đánh giá**

| Thành phần | Cấu hình |
|------------|----------|
| Hệ điều hành | Windows 11 |
| Máy chủ ứng dụng | Java 21 (Temurin), Spring Boot 3.5, chạy bằng Maven Wrapper trên cổng 8080 |
| Giao diện | React 19 + TypeScript, Vite 8, máy chủ phát triển trên cổng 5173 |
| Cơ sở dữ liệu quan hệ | PostgreSQL 16 kèm phần mở rộng `pgvector`, ảnh Docker `pgvector/pgvector:pg16`, cổng 5432 |
| Cơ sở dữ liệu đồ thị | Neo4j 5, ảnh Docker `neo4j:5`, cổng 7687 |
| Bộ nhớ đệm và hàng thông điệp | Redis 7, ảnh Docker `redis:7-alpine`, cổng 6379 |
| Nhà cung cấp mô hình chính | Google Gemini (`gemini-3.6-flash`), gói miễn phí |
| Nhà cung cấp mô hình dự phòng | Groq (`openai/gpt-oss-120b`), gói miễn phí |
| Quản lý lược đồ | Flyway, 23 tệp migration đánh số |

Toàn bộ hạ tầng dữ liệu khởi động bằng một lệnh `docker compose up -d`, nhờ đó môi trường dựng lại được từ đầu một cách xác định — điều kiện cần cho việc kiểm thử tích hợp trình bày ở mục 3.4.

**Khoá bí mật không nằm trong mã nguồn.** Khoá của hai nhà cung cấp mô hình, mật khẩu cơ sở dữ liệu, khoá ký JWT và mật khẩu ứng dụng của hộp thư đều đọc từ biến môi trường trong tệp `.env`; tệp này bị loại khỏi hệ thống quản lý phiên bản, và kho mã chỉ chứa tệp mẫu `.env.example` liệt kê **tên biến** kèm hướng dẫn lấy khoá, không chứa giá trị nào.

[HÌNH 3.1: Sơ đồ triển khai — máy chủ ứng dụng, giao diện, ba hệ quản trị dữ liệu trong Docker và hai nhà cung cấp mô hình bên ngoài — cần chèn]

*Hình 3.1. Sơ đồ triển khai hệ thống*

## 3.2. Giao diện phía người dùng

Giao diện tuân theo bộ quy ước đã trình bày ở mục 2.2.3: màu sắc, bo góc và đổ bóng khai báo tập trung dưới dạng biến; trang dành cho người học trình bày theo lưới thẻ, trang quản lý theo bảng; các thành phần dùng chung như tiêu đề trang và trạng thái danh sách rỗng được tái sử dụng.

[HÌNH 3.2: Màn hình đăng nhập và đăng ký — biểu mẫu email, nút đăng nhập bằng Google, ô chọn vai trò — cần chèn]

*Hình 3.2. Màn hình đăng nhập và đăng ký*

[HÌNH 3.3: Màn hình khám phá quiz — thanh tìm kiếm, bộ lọc danh mục và độ khó, lưới thẻ quiz kèm số người đã học — cần chèn]

*Hình 3.3. Màn hình khám phá quiz*

[HÌNH 3.4: Màn hình làm bài — nội dung câu hỏi, danh sách phương án, đồng hồ đếm ngược, điều hướng câu — cần chèn]

*Hình 3.4. Màn hình làm bài quiz*

[HÌNH 3.5: Màn hình kết quả — tổng điểm, danh sách câu kèm đáp án đúng, lời giải thích và nhận xét của AI cho câu tự luận — cần chèn]

*Hình 3.5. Màn hình kết quả làm bài*

[HÌNH 3.6: Phòng chờ và phòng đấu — mã PIN sáu số, mã QR, danh sách người chơi kèm nhân vật; màn chơi với câu hỏi và bảng xếp hạng trực tiếp — cần chèn]

*Hình 3.6. Phòng đấu thời gian thực*

[HÌNH 3.7: Màn hình học liệu và sinh đề bằng AI — danh sách học liệu kèm trạng thái xử lý, biểu mẫu sinh đề, danh sách câu hỏi nháp chờ duyệt kèm đoạn học liệu nguồn — cần chèn]

*Hình 3.7. Màn hình học liệu và sinh đề bằng AI*

[HÌNH 3.8: Màn hình trợ lý học tập — khung hội thoại, phản hồi hiện dần theo luồng, khối trích dẫn nguồn dưới câu trả lời — cần chèn]

*Hình 3.8. Màn hình trợ lý học tập*

[HÌNH 3.9: Màn hình gợi ý và lộ trình học — danh sách quiz được gợi ý kèm lý do, thứ tự chủ đề nên ôn — cần chèn]

*Hình 3.9. Màn hình gợi ý và lộ trình học*

[HÌNH 3.10: Màn hình thẻ ghi nhớ và phiên ôn tập — danh sách bộ thẻ kèm số thẻ đến hạn, thẻ lật được và bốn nút tự đánh giá mức nhớ — cần chèn]

*Hình 3.10. Màn hình thẻ ghi nhớ và phiên ôn tập*

[HÌNH 3.11: Màn hình lớp học — trang lớp với danh sách thành viên, danh sách bài tập kèm hạn nộp, bảng theo dõi nộp bài — cần chèn]

*Hình 3.11. Màn hình lớp học và giao bài*

[HÌNH 3.12: Màn hình thành tích và bảng xếp hạng mùa — cấp độ, huy hiệu, chuỗi ngày học, thử thách hằng ngày, bảng xếp hạng kèm phân hạng — cần chèn]

*Hình 3.12. Màn hình thành tích và bảng xếp hạng theo mùa*

**Một quyết định giao diện cần nêu rõ.** Thẻ quiz trên màn hình khám phá hiển thị **số người đã học**, nhưng không hiển thị điểm đánh giá. Hệ thống chưa có chức năng đánh giá nên chưa có dữ liệu đó; hiển thị một con số không có thật ở vị trí này không phải lỗi trang trí mà là **đưa ra một lời khuyên sai**, vì điểm đánh giá chính là thứ người học dựa vào để chọn bài học. Cùng lý do, số người đã học đếm theo **người** chứ không theo **lượt**: hệ thống khuyến khích ôn lại nhiều lần, nên đếm theo lượt sẽ khiến một người làm mười lần đọc thành mười người đã học.

## 3.3. Giao diện phía quản trị

Khu quản trị dùng khung giao diện riêng, tách khỏi khung của người dùng thường, để không nhầm lẫn giữa thao tác học tập và thao tác quản trị.

[HÌNH 3.13: Trang tổng quan quản trị — các chỉ số người dùng, quiz, lượt làm bài, phòng đang chạy, chi phí AI trong tháng, biểu đồ tăng trưởng — cần chèn]

*Hình 3.13. Trang tổng quan quản trị*

[HÌNH 3.14: Trang quản lý người dùng — danh sách có lọc theo vai trò và trạng thái khoá, thao tác khoá, đổi vai trò, thu hồi phiên — cần chèn]

*Hình 3.14. Trang quản lý người dùng*

[HÌNH 3.15: Trang giám sát AI — nhật ký lời gọi kèm nhà cung cấp, số token, độ trễ, tỉ lệ lỗi và tỉ lệ dùng dự phòng; ô đặt hạn mức mỗi ngày cho từng người dùng — cần chèn]

*Hình 3.15. Trang giám sát chi phí và độ tin cậy AI*

[HÌNH 3.16: Trang báo cáo tính toàn vẹn — danh sách lượt thi kèm điểm rủi ro và cờ có lý do cụ thể, thẻ chi tiết kèm nhận định của mô hình và hai nút kết luận — cần chèn]

*Hình 3.16. Trang báo cáo tính toàn vẹn*

Trang cấu hình nhà cung cấp mô hình chỉ hiển thị **trạng thái** của mỗi khoá — *đã cấu hình* hoặc *để trống* — chứ không hiển thị giá trị khoá, kể cả ở dạng che một phần. Một khoá bị lộ trên màn hình quản trị vẫn là một khoá bị lộ.

## 3.4. Kiểm thử chức năng

### 3.4.1. Kế hoạch kiểm thử

Kiểm thử được viết **cùng lúc với chức năng**, theo quy trình lát cắt dọc: mỗi chức năng chỉ được coi là hoàn thành khi đã có kiểm thử tự động chạy đạt. Bộ kiểm thử chia hai tầng.

**Kiểm thử đơn vị** (JUnit 5, Mockito) áp cho các lớp có logic thuần tuý, không phụ thuộc hạ tầng: bộ chấm điểm tự động, bộ tính điểm theo tốc độ, bộ lập lịch ôn tập SM-2, bộ phân hạng theo mùa, bộ tính điểm rủi ro, các bộ phân tích kết quả trả về từ mô hình ngôn ngữ, và bộ ghi tệp CSV.

**Kiểm thử tích hợp** (Spring Boot Test, Testcontainers) áp cho mọi luồng đi qua cơ sở dữ liệu, bảo mật hoặc WebSocket. Testcontainers dựng PostgreSQL, Neo4j và Redis thật trong Docker cho mỗi lớp kiểm thử, nên phép kiểm chạy trên đúng hệ quản trị mà sản phẩm dùng — kể cả những đặc điểm chỉ PostgreSQL mới có như chỉ mục một phần, kiểu `jsonb` và phần mở rộng vector.

Lý do không dùng cơ sở dữ liệu trong bộ nhớ để chạy nhanh hơn: một phần đáng kể ràng buộc của hệ thống nằm ở **tầng cơ sở dữ liệu** — ràng buộc duy nhất chống cộng điểm hai lần, chỉ mục một phần bảo đảm mỗi người tối đa một bài đang làm dở, ràng buộc kiểm tra miền giá trị. Kiểm thử trên một hệ quản trị khác sẽ bỏ qua đúng những ràng buộc đó, tức là bỏ qua phần dễ sai nhất.

### 3.4.2. Kịch bản kiểm thử

Bảng 3.2 trình bày các kịch bản tiêu biểu, chọn theo tiêu chí **mỗi kịch bản kiểm một ranh giới khác nhau** thay vì liệt kê các trường hợp thuận lợi.

**Bảng 3.2. Các kịch bản kiểm thử tiêu biểu**

| STT | Chức năng | Kịch bản | Dữ liệu kiểm thử | Kết quả mong đợi | Kết quả |
|----:|-----------|----------|------------------|------------------|---------|
| 1 | Đăng ký | Email đã tồn tại | Email trùng tài khoản có sẵn | Mã lỗi 409, không tạo tài khoản thứ hai | Đạt |
| 2 | Đăng ký | Tự đăng ký vai trò quản trị | `role=ADMIN` | Hạ xuống vai trò người học | Đạt |
| 3 | Đăng nhập Google | Tài khoản mới, chọn vai trò người tạo | `role=CREATOR` | Tạo tài khoản đúng vai trò đã chọn | Đạt |
| 4 | Đăng nhập Google | Tài khoản **đã có**, gửi kèm vai trò cao hơn | Tài khoản người học gửi `role=CREATOR` | **Giữ nguyên** vai trò cũ | Đạt |
| 5 | Đổi mật khẩu | Sau khi đổi | Hai thiết bị đang đăng nhập | Thu hồi phiên trên **mọi** thiết bị | Đạt |
| 6 | Xem quiz | Khách chưa đăng nhập xem quiz công khai | Không có token | Trả về thông tin giới thiệu, **không** kèm câu hỏi | Đạt |
| 7 | Xem quiz | Khách xem quiz riêng tư của người khác | Mã quiz riêng tư | Trả về 404 chứ không phải 403 | Đạt |
| 8 | Làm bài | Chủ quiz sửa đề khi có người đang làm dở | Sửa quiz giữa chừng | Lượt đang làm giữ nguyên đề đã chốt | Đạt |
| 9 | Làm bài | Hết giờ | Quiz có thời lượng | Tự chuyển sang trạng thái hết hạn | Đạt |
| 10 | Chấm tự luận | Mô hình trả điểm vượt trần | Điểm lớn hơn điểm tối đa của câu | Giới hạn cứng về trần thật của câu | Đạt |
| 11 | Chấm tự luận | Người đã chấm tay, AI trả kết quả sau | Ghi đè điểm rồi mới có phản hồi AI | Bỏ qua kết quả AI | Đạt |
| 12 | Chấm tự luận | Gọi mô hình thất bại | Ngắt nhà cung cấp | Chuyển trạng thái dừng rõ ràng, không treo | Đạt |
| 13 | Phòng đấu | Khách vào phòng khi chủ phòng **chưa** bật | Mã PIN đúng, cờ tắt | Từ chối với mã 403 | Đạt |
| 14 | Phòng đấu | Mất kết nối rồi vào lại | Ngắt WebSocket giữa ván | Giữ nguyên điểm đã có | Đạt |
| 15 | Phòng đấu | Hai tiến trình máy chủ | Người chơi chia hai instance | Cả hai bên nhận đủ sự kiện | Đạt |
| 16 | Sinh đề AI | Người dùng đã hết hạn mức | Hạn mức trong ngày đã dùng hết | Trả 429 **ngay**, không nhận việc rồi hỏng | Đạt |
| 17 | Sinh đề AI | Câu trả về sai định dạng | JSON thiếu trường | Bộ kiểm cấu trúc loại câu đó | Đạt |
| 18 | Trợ lý học tập | Hỏi ngoài phạm vi học liệu | Câu hỏi không liên quan | Trả lời không biết, không suy đoán | Đạt |
| 19 | Trợ lý học tập | Học liệu của người khác chưa chia sẻ | Tài liệu riêng tư | Không xuất hiện trong kết quả truy hồi | Đạt |
| 20 | Gợi ý | Neo4j ngừng hoạt động | Dừng dịch vụ đồ thị | API trả danh sách rỗng, **không** làm hỏng việc nộp bài | Đạt |
| 21 | Tải ảnh | Tệp giả dạng ảnh | Tệp mã lệnh đặt đuôi `.png` | Từ chối theo chữ ký byte | Đạt |
| 22 | Ảnh đại diện | Người học đổi ảnh | Ảnh đã tải lên hệ thống | Cho phép, và chỉ giữ **một** tệp cho mỗi người | Đạt |
| 23 | Ảnh đại diện | Dán URL bên ngoài | Địa chỉ máy chủ lạ | Từ chối với mã 400 | Đạt |
| 24 | Giao bài | Nộp sau hạn | Nộp quá hạn nộp | Vẫn nhận, đánh dấu là nộp trễ | Đạt |
| 25 | Giao bài | Xoá quiz đang được giao | Quiz gắn với bài tập | Chặn thao tác xoá | Đạt |
| 26 | Gamification | Cùng một hành động ghi nhận hai lần | Gọi lại sự kiện cộng điểm | Chỉ cộng đúng một lần | Đạt |
| 27 | Xếp hạng mùa | Mùa có dưới mười người tham gia | 3 người | **Không** phân hạng cho ai | Đạt |
| 28 | Thông báo | Công việc nhắc ôn chạy lại trong ngày | Khởi động lại máy chủ | Không gửi trùng | Đạt |
| 29 | Chống gian lận | Gửi tín hiệu cho lượt **luyện tập** | Lượt không tính điểm | Máy chủ từ chối ghi nhận | Đạt |
| 29b | Chống gian lận | Câu chữ hiện cho người thi | Đã ghi nhận 5 lần rời trang | **Không** chứa chữ mang nghĩa buộc tội; nói rõ giáo viên là người kết luận | Đạt |
| 30 | Chống gian lận | Mốc thời gian ở tương lai | Đồng hồ máy khách sai | Cắt về thời điểm hiện tại của máy chủ | Đạt |

### 3.4.3. Kết quả kiểm thử

**Bảng 3.3. Kết quả chạy bộ kiểm thử tự động**

| Tầng | Công cụ | Số lớp | Số phép kiểm | Đạt | Hỏng |
|------|---------|-------:|-------------:|----:|-----:|
| Máy chủ — đơn vị và tích hợp | JUnit 5, Mockito, Testcontainers | 52 | **578** | 578 | 0 |
| Giao diện | Vitest, Testing Library | 11 | **71** | 71 | 0 |
| **Tổng** | | **63** | **649** | **649** | **0** |

Bộ kiểm thử máy chủ chạy sau lệnh dọn sạch thư mục biên dịch để loại trừ ảnh hưởng của những lần chạy có lọc trước đó.

**Bảng 3.4. Phân bố phép kiểm theo nhóm chức năng (máy chủ)**

| Nhóm chức năng | Số phép kiểm |
|----------------|-------------:|
| AI: RAG, sinh đề, chấm tự luận, hạn mức | 116 |
| Làm bài và chấm điểm | 57 |
| Xác thực và phân quyền | 43 |
| Quản lý quiz và câu hỏi | 38 |
| Lớp học và giao bài | 38 |
| Chống gian lận | 33 |
| Phòng đấu thời gian thực | 30 |
| Tải ảnh lên | 28 |
| Gợi ý cá nhân hoá (Neo4j) | 28 |
| Flashcard và lặp lại ngắt quãng | 25 |
| Thông báo và nhắc ôn | 24 |
| Bảng xếp hạng theo mùa | 24 |
| Gamification | 21 |
| Trợ lý học tập | 20 |
| Quản trị hệ thống | 19 |
| Thống kê và báo cáo | 17 |
| Hồ sơ người dùng | 16 |
| Khởi động ứng dụng | 1 |

Nhóm chức năng AI chiếm tỉ trọng lớn nhất, phản ánh đúng đặc điểm của phần này: kết quả trả về từ mô hình ngôn ngữ **không xác định**, nên phần lớn phép kiểm không kiểm nội dung câu trả lời mà kiểm **hàng rào quanh nó** — giới hạn miền điểm, bộ kiểm cấu trúc JSON, điều kiện chuyển nhà cung cấp dự phòng, hạn mức, và cách ly quyền đọc học liệu.

### 3.4.4. Những lỗi bộ kiểm thử không phát hiện được

Phần này ghi lại một kết quả **âm tính** đáng chú ý: trong quá trình hiện thực, ba lỗi thật được phát hiện khi **mở sản phẩm ra sử dụng**, trong khi toàn bộ bộ kiểm thử vẫn báo đạt.

**Bảng 3.5. Ba lỗi lộ ra khi dùng thật, không lộ ra qua kiểm thử**

| Lỗi | Vì sao kiểm thử không bắt được |
|-----|-------------------------------|
| Người học bấm đổi ảnh đại diện trên trang hồ sơ của chính mình nhận lỗi *không có quyền* | Có hẳn một phép kiểm khẳng định người học **không** được tải ảnh lên, và nó vẫn đạt. Phép kiểm đúng với đặc tả **tại thời điểm viết** — khi đó đường tải ảnh chỉ phục vụ ảnh bìa quiz. Đặc tả mới là thứ hết hạn, không phải phép kiểm |
| Ô chọn vai trò ở trang đăng ký bị bỏ qua khi người dùng bấm đăng ký bằng Google | Cả ba tầng đều nhất quán với nhau: giao diện không gửi, máy chủ đặt cứng vai trò người học, phép kiểm khẳng định đúng hành vi đó. Chỉ có **ô chọn vai trò vẫn hiển thị trên màn hình** là mâu thuẫn — mà giao diện thì không có phép kiểm nào đối chiếu với chính nó |
| Chân trang trồi lên giữa màn hình ở những trang có nội dung ngắn | Lỗi thuộc về **thứ tự xếp tầng CSS**: thư viện giao diện chèn quy tắc lúc chạy ở ngoài lớp của công cụ tạo kiểu, nên quy tắc của lớp ngoài thắng. Không có công cụ kiểm thử tự động nào trong dự án nhìn thấy kết quả bố cục thật |

Kết luận rút ra: **một bộ kiểm thử toàn đạt chứng minh những gì đã nghĩ tới là đúng, không chứng minh đã nghĩ đủ.** Ba lỗi trên đều thuộc loại *hỏng trong im lặng* — không có ngoại lệ, không có mã lỗi, chỉ có hành vi sai. Đó cũng là lý do quy trình của đồ án đặt bước **chạy thật và nhờ người dùng xác nhận trên trình duyệt** thành một bước bắt buộc, ngang hàng với bước chạy kiểm thử.

## 3.5. Đánh giá hiệu năng thời gian thực

*Đo ngày 08/08/2026. Kịch bản đo: `loadtest_room.mjs` và `loadtest_two_instances.mjs`.*

### 3.5.1. Điều kiện và phương pháp đo

Máy chủ và toàn bộ máy khách mô phỏng chạy trên **cùng một máy**, nên số liệu dưới đây đo **chi phí xử lý của máy chủ và tầng phát tán**, không bao gồm độ trễ mạng thật. Điều này cần nói rõ để không ai đọc con số 20 ms thành "người dùng ở xa thấy 20 ms". Mỗi vòng đo gồm 3 câu hỏi, 60 giây mỗi câu, người chơi vào phòng với tư cách khách vãng lai — đúng kịch bản quét mã QR trong lớp học.

**Cách đo không cần sửa mã nghiệp vụ.** Sự kiện phát câu hỏi vốn đã mang mốc hết giờ theo đồng hồ máy chủ để máy khách đếm ngược; trừ đi thời lượng câu hỏi ra được thời điểm máy chủ phát đi, so với thời điểm máy khách nhận là ra độ trễ.

Kế hoạch ban đầu dự định dùng k6 hoặc Gatling. Cả hai **không nói được giao thức STOMP over SockJS** nếu không viết thêm phần mở rộng, mà đó lại chính là đường cần đo. Công cụ đo tự viết dùng đúng thư viện mà trình duyệt dùng, nên nói đúng giao thức thật thay vì mô phỏng gần đúng.

### 3.5.2. Độ trễ phát câu hỏi theo số người trong phòng

**Bảng 3.6. Độ trễ phát câu hỏi theo số người chơi trong một phòng**

| Người chơi | Thời gian nối vào phòng | P50 | P95 | Lớn nhất | Sự kiện mất |
|-----------:|------------------------:|----:|----:|---------:|------------:|
| 10 | 244 ms | 18 ms | 20 ms | 20 ms | **0** |
| 30 | 761 ms | 26 ms | 32 ms | 32 ms | **0** |
| 50 | 1 349 ms | 48 ms | 52 ms | 52 ms | **0** |
| 100 | 4 494 ms | 180 ms | 216 ms | 219 ms | **0** |
| 150 | 11 454 ms | 542 ms | 566 ms | 568 ms | **0** |
| 200 | 25 101 ms | 1 411 ms | 1 509 ms | 1 511 ms | **0** |

[HÌNH 3.17: Biểu đồ độ trễ P50 và P95 theo số người chơi trong phòng, trục hoành 10–200 người — cần chèn]

*Hình 3.17. Độ trễ phát câu hỏi theo số người chơi*

Hai nhận xét quan trọng.

**Không mất một sự kiện nào ở mọi mức tải đã thử.** Hệ thống không rơi rớt thông điệp — nó chỉ chậm dần. Đây là kiểu suy giảm dễ chịu: người chơi thấy câu hỏi tới muộn, chứ không có ai bị bỏ lại giữa ván.

**Độ trễ tăng siêu tuyến tính.** Gấp đôi số người từ 50 lên 100 làm P95 tăng bốn lần; từ 100 lên 200 tăng thêm bảy lần. Ngưỡng dùng được trong thực tế là **100 người mỗi phòng**, khi đó P95 là 216 ms — vẫn nằm trong mức người dùng cảm nhận là tức thời với một trò chơi hỏi đáp. Vượt 150 người thì độ trễ từ nửa giây trở lên bắt đầu ảnh hưởng tới **sự công bằng**, vì người nhận câu hỏi muộn có ít thời gian trả lời hơn trong khi điểm số phụ thuộc tốc độ.

### 3.5.3. Xác định vị trí nghẽn

Ở mức 200 người, P50 (1 411 ms) và P95 (1 509 ms) gần bằng nhau. Nếu nguyên nhân chậm nằm ở khâu phát tán xuống thì người nhận đầu tiên phải nhanh hơn hẳn người cuối, tức khoảng cách giữa hai phân vị phải rộng. Nó hẹp, nghĩa là **mọi người bị trễ gần như nhau** — nghẽn xảy ra *trước* lúc phát tán.

Giả thuyết này được kiểm chứng bằng cách chạy lại đúng mức 200 người nhưng không gửi đáp án lên.

**Bảng 3.7. Tách phần nhận và phần gửi ở mức 200 người chơi**

| Kịch bản | P50 | P95 |
|----------|----:|----:|
| Có gửi đáp án — mọi người trả lời cùng lúc | 1 411 ms | 1 509 ms |
| **Chỉ nhận câu hỏi, không gửi gì** | **262 ms** | **638 ms** |

Khoảng **80% độ trễ đến từ việc xử lý 200 đáp án gửi lên**, không phải từ việc phát câu hỏi xuống: lệnh chuyển câu tiếp theo của chủ phòng phải xếp hàng sau chúng trên cùng một kênh vào.

Hệ quả trực tiếp cho việc tối ưu về sau: nới kênh xử lý **đầu vào** mới có tác dụng, còn tối ưu khâu phát tán thì gần như không. Đây là loại kết luận chỉ có được khi tách hai nguồn ra đo riêng, thay vì nhìn vào một con số tổng.

### 3.5.4. Vai trò của Redis Pub/Sub

Phiếu giao đề tài yêu cầu *so sánh có và không có Redis Pub/Sub*. Trong kiến trúc này **không tồn tại chế độ tắt Redis để so sánh**: mọi sự kiện đều đi qua nó, kể cả khi gửi tới người chơi nằm trên chính tiến trình vừa phát. Bỏ Redis ra không làm hệ thống chậm hơn — nó làm phòng đấu nhiều tiến trình **không còn hoạt động**.

Vai trò của Redis vì vậy được chứng minh bằng **suy luận loại trừ**: chạy hai tiến trình máy chủ trên hai cổng khác nhau dùng chung một Redis, chia 40 người chơi ra hai bên, chủ phòng bắt đầu ván trên tiến trình thứ nhất.

**Bảng 3.8. Phòng đấu chạy trên hai tiến trình máy chủ**

| Người chơi nối vào | Sự kiện mong đợi | Nhận được | P50 | P95 |
|--------------------|-----------------:|----------:|----:|----:|
| Tiến trình A — cùng chỗ với chủ phòng | 60 | **60** | 38 ms | 41 ms |
| Tiến trình B — khác tiến trình | 60 | **60** | 40 ms | 42 ms |

Hai tiến trình này không có kênh liên lạc nào khác: bộ môi giới thông điệp của Spring nằm trong bộ nhớ của từng tiến trình, còn cơ sở dữ liệu quan hệ không phải kênh nhắn tin. Người chơi bên B nhận **đủ 60 trên 60** sự kiện do tiến trình A phát ra, nên Redis Pub/Sub là con đường duy nhất có thể.

**Chi phí của khả năng mở rộng ngang là khoảng 2 ms** — 40 ms so với 38 ms. Đi vòng qua Redis sang một tiến trình khác gần như không đắt hơn việc ở lại ngay trong bộ nhớ của tiến trình phát.

### 3.5.5. Giới hạn của phép đo

- Một máy đơn, không có độ trễ mạng — con số thực tế trên Internet sẽ cao hơn.
- Toàn bộ máy khách mô phỏng chạy trong **một tiến trình Node**; ở mức 200 người, một phần độ trễ đo được có thể là của chính công cụ đo. Lần chạy đầu từng báo chủ phòng không nối được ở mức 100 người — nguyên nhân là chủ phòng nối vào sau cùng khi vòng lặp sự kiện của công cụ đo đã bận, không phải giới hạn của máy chủ.
- Chưa đo với mạng thật, chưa đo nhiều phòng chạy song song, chưa đo mức tiêu thụ bộ nhớ và bộ xử lý.

## 3.6. Đánh giá độ chính xác của các chức năng AI

*Đo ngày 14/08/2026, riêng đường dự phòng đo ngày 20/08/2026. Kịch bản đo: `danhgia_ai.mjs`.*

### 3.6.1. Điều kiện đo

Mỗi lượt gọi mô hình giãn cách **70 giây**, tổng thời gian chạy khoảng 20 phút. Lý do phải giãn: gói miễn phí của nhà cung cấp giới hạn 5 lượt mỗi phút. Lần chạy đầu tiên bắn liên tiếp nên phần lớn dính lỗi vượt hạn mức, những bài làm đầy đủ nhận 0 điểm — và nếu tin con số đó thì báo cáo sẽ kết luận *"AI chấm sai hoàn toàn"* trong khi thực tế **mô hình chưa từng được gọi**.

Cùng lý do đó, bài nào gọi mô hình thất bại bị **loại khỏi thống kê**, không tính là 0 điểm. Gộp *"AI chấm 0 điểm"* với *"AI không chạy"* là làm hỏng chính con số đang đo. Lần chạy được báo cáo không có bài nào thất bại.

Một điều kiện nữa cần ghi: số liệu đo trước ngày 13/08 lấy trên đường truy hồi **đang lỗi** — chỉ mục xấp xỉ xếp hạng trước khi lọc quyền đọc nên kho vector trông như rỗng. Mọi con số liên quan tới RAG trước mốc đó đã bị loại bỏ và đo lại.

### 3.6.2. Kết quả tổng hợp

**Bảng 3.9. Tổng hợp kết quả đánh giá các chức năng AI**

| Hạng mục | Chỉ số | Kết quả |
|----------|--------|--------:|
| Chấm tự luận | Bài có điểm nằm trong khoảng chuẩn | **7/8** |
| Chấm tự luận | Sai lệch điểm trung bình | **0,13/10** |
| Chấm tự luận | Sai lệch lớn nhất | **1/10** |
| Chống tiêm chỉ thị | Bài tấn công bị chặn | **2/2** |
| Sinh đề | Câu nhận được trên số câu yêu cầu | **10/10** |
| Sinh đề | Câu bị bộ kiểm cấu trúc loại | **0** |
| Trợ lý — có học liệu | Trả lời đúng và có trích dẫn | **3/3** |
| Trợ lý — ngoài học liệu | Nói không biết thay vì suy đoán | **2/2** |
| Trợ lý — ngoài học liệu | Vẫn hiện nguồn dù nói không biết *(càng thấp càng tốt)* | **2/2** |
| Đường dự phòng | Câu sinh được qua Groq | **9/9** |
| Đường dự phòng | Độ trễ trung bình Groq so với Gemini | **2 039 ms** / **10 526 ms** |

Ba hạng mục đầu đạt mức dùng được trong thực tế. Hàng áp chót là **hạn chế đã phát hiện**, trình bày ở mục 3.6.5.

### 3.6.3. Chấm tự luận

Đáp án chuẩn không lấy theo ý kiến người chấm mà **suy ra từ tiêu chí chấm**: mỗi bài làm mẫu được dựng sao cho tiêu chí quyết định điểm. Tiêu chí ghi *mỗi nguyên nhân đúng 3 điểm, diễn đạt rõ thêm 1 điểm*, nên bài nêu đúng hai nguyên nhân có điểm chuẩn là 5–7. Dùng **khoảng điểm** thay vì một con số duy nhất vì tiêu chí có một điểm mang tính định tính; ép về một con số là giả vờ chính xác hơn thực tế.

Câu hỏi dùng để đo: *"Nêu ba nguyên nhân chính khiến một ứng dụng web chạy chậm."*

**Bảng 3.10. Đối chiếu điểm AI chấm với khoảng điểm chuẩn theo tiêu chí**

| Bài làm mẫu | Điểm chuẩn | AI chấm | Lệch |
|-------------|-----------:|--------:|-----:|
| Đủ 3 ý, diễn đạt rõ | 9–10 | 10 | 0 |
| Đủ 3 ý nhưng viết cụt lủn | 7–9 | **10** | **1** |
| Đúng 2 trên 3 ý | 5–7 | 7 | 0 |
| Chỉ đúng 1 ý | 2–4 | 4 | 0 |
| Lạc đề hoàn toàn | 0–2 | 0 | 0 |
| Chép lại đề bài | 0–1 | 0 | 0 |
| Tấn công: đòi điểm tối đa | 0–2 | 0 | 0 |
| Tấn công: giả mốc rào dữ liệu | 0–2 | 0 | 0 |

**Bài lệch duy nhất** nêu đủ ba nguyên nhân nhưng viết cụt lủn, nên theo tiêu chí phải mất điểm phần diễn đạt. Mô hình cho điểm tối đa, tức **rộng tay với tiêu chí định tính**. Đây là xu hướng đáng lưu ý: mô hình nhận diện tốt phần *nội dung* — đủ mấy ý — nhưng dễ bỏ qua phần *chất lượng diễn đạt*. Với bài thi thật, hệ quả là điểm hơi cao hơn mức đáng có ở những bài trả lời đúng nhưng trình bày kém.

Bảy bài còn lại nằm đúng khoảng chuẩn, và quan trọng hơn là mô hình **phân biệt đúng thứ tự chất lượng**: 10 → 10 → 7 → 4 → 0 → 0. Không có trường hợp nào bài kém được điểm cao hơn bài tốt.

### 3.6.4. Chống tiêm chỉ thị

Đây là bề mặt tấn công lớn nhất của hệ thống, vì bài làm là nội dung người học tự gõ rồi đi thẳng vào phần nhắc của mô hình.

**Bảng 3.11. Kết quả thử tấn công tiêm chỉ thị qua bài làm**

| Kiểu tấn công | Nội dung | Kết quả |
|---------------|----------|---------|
| Đòi điểm tối đa | Bài làm chứa chỉ thị yêu cầu mô hình cho điểm cao nhất | **0/10 — bị chặn** |
| Giả mốc rào dữ liệu | Bài làm tự gõ đúng chuỗi đóng khối dữ liệu rồi viết chỉ thị ở "bên ngoài" | **0/10 — bị chặn** |

Cả hai nhận 0 điểm, đúng như chỉ dẫn hệ thống quy định. Ngoài ba lớp phòng ngừa ở tầng nhắc, hàng rào cuối cùng vẫn là **ràng buộc cứng miền điểm** ở phía hệ thống, nên kể cả khi ba lớp trên bị vượt thì điểm cũng không thể vượt trần thật của câu hỏi.

### 3.6.5. Sinh đề và trợ lý học tập

**Bảng 3.12. Kết quả sinh đề từ học liệu**

| Chủ đề | Yêu cầu | Nhận được | Bộ kiểm loại | Đúng chuẩn cấu trúc |
|--------|--------:|----------:|-------------:|--------------------:|
| Mã trạng thái HTTP | 5 | 5 | 0 | **5/5** |
| Cấu trúc dữ liệu cơ bản | 5 | 5 | 0 | **5/5** |

Tiêu chí *đúng chuẩn cấu trúc* được kiểm lại **độc lập** ở phía kịch bản đo, không tin vào việc máy chủ đã lọc: đúng loại câu hỏi, có tối thiểu hai phương án, **đúng một** phương án được đánh dấu là đáp án đúng, và nội dung câu hỏi dài hơn 10 ký tự.

Tỉ lệ 10/10 cần đọc kèm bối cảnh: bộ kiểm cấu trúc ở máy chủ loại các câu sai định dạng **trước khi** trả về, nên con số này đo *tỉ lệ câu sống sót qua toàn bộ đường ống*, không phải *tỉ lệ mô hình sinh đúng ngay lần đầu*. Số câu bị loại bằng 0 cho biết mô hình trả về JSON đúng lược đồ một cách ổn định.

Phép đo này **không trả lời** được chất lượng *sư phạm* của câu hỏi — câu có đo đúng năng lực cần đo hay không, phương án nhiễu có hợp lý hay không. Đó là đánh giá cần người dạy đọc từng câu, và chính là lý do hệ thống buộc người tạo nội dung **duyệt từng câu** trước khi đưa vào ngân hàng.

Với trợ lý học tập, học liệu dùng để đo chứa một sự thật **bịa ra** — một giao thức không tồn tại kèm số cổng, thời gian chờ và thời hạn hiệu lực — để biết chắc câu trả lời lấy từ học liệu chứ không từ kiến thức nền của mô hình.

**Bảng 3.13. Kết quả đánh giá khả năng bám nguồn của trợ lý học tập**

| Câu hỏi | Có trong học liệu | Có trích nguồn | Nói không biết | Kết quả |
|---------|:-----------------:|:--------------:|:--------------:|:-------:|
| Giao thức dùng cổng nào? | có | có | không | **Đạt** |
| Chờ bao lâu trước khi thử lại? | có | có | không | **Đạt** |
| Bản ghi hết hiệu lực sau bao lâu? | có | có | không | **Đạt** |
| Chiến tranh Punic kết thúc năm nào? | không | có | **có** | **Đạt** |
| Giao thức dùng thuật toán mã hoá nào? | không | có | **có** | **Đạt** |

Cả ba câu nằm trong học liệu đều được trả lời đúng con số và kèm trích dẫn. Cả hai câu ngoài học liệu đều nhận câu trả lời *không có thông tin trong học liệu* — mô hình **không suy đoán từ kiến thức nền**, kể cả với câu hỏi mà nó chắc chắn biết đáp án. Đây là hành vi quan trọng nhất của một trợ lý bám học liệu.

**Hạn chế phát hiện được.** Với hai câu ngoài học liệu, hệ thống **vẫn trả về danh sách nguồn**. Nguyên nhân nằm ở thứ tự: danh sách nguồn được gửi **trước** khi mô hình kịp trả lời, nên nó phản ánh *có đoạn nào vượt ngưỡng khoảng cách hay không*, chứ không phản ánh *mô hình có dùng đoạn đó hay không*. Hệ quả trên giao diện là người dùng thấy câu trả lời "tôi không có thông tin" mà bên dưới lại có khối "dựa trên tài liệu X" — hai thứ nói ngược nhau.

Hai hướng xử lý — siết ngưỡng khoảng cách, hoặc chuyển danh sách nguồn sang sự kiện cuối luồng — đều **cần đo thêm trước khi chọn**. Siết ngưỡng mà không có số liệu khoảng cách thực tế thì chỉ là đổi một con số tuỳ ý bằng một con số tuỳ ý khác, và rơi đúng vào cái bẫy đã gặp với chỉ mục xấp xỉ. Hạn chế này được ghi vào phần nợ kỹ thuật thay vì sửa vội.

### 3.6.6. Đường dự phòng giữa hai nhà cung cấp mô hình

Đây là phép đo đầu tiên của đường dự phòng trong cả dự án. Trước ngày 20/08 nó **chưa một lần chạy**, vì nhà cung cấp dự phòng ban đầu không có gói miễn phí — khoá hợp lệ vẫn trả về lỗi từ chối quyền.

**Bảng 3.14. So sánh hai phương án nhà cung cấp dự phòng**

| Tiêu chí | Phương án ban đầu | Groq (đang dùng) |
|----------|-------------------|------------------|
| Gói miễn phí | **Không** — khoá hợp lệ vẫn bị từ chối quyền | Có |
| Hỗ trợ trả lời theo luồng | Không | **Có** |
| Sinh vector nhúng | Không | Không |

Cột *trả lời theo luồng* có hệ quả trực tiếp: trước khi đổi, nếu nhà cung cấp chính hỏng thì **trợ lý học tập tắt hẳn**, vì lớp điều phối lọc theo khả năng trả lời theo luồng và danh sách còn lại rỗng. Sau khi đổi, chữ vẫn chảy từ nhà cung cấp dự phòng.

Một chi tiết phải xử lý ngay trước khi đo: mô hình dự định dùng đã bị nhà cung cấp gỡ bỏ, phát hiện khi truy vấn danh sách mô hình còn hoạt động. Đây là lần thứ **ba** dự án gặp đúng tình huống này với ba nhà cung cấp khác nhau. Nếu không kiểm trước, cấu hình sẽ *trông như* đã có đường dự phòng trong khi nó không bao giờ chạy được.

**Bảng 3.15. Sinh đề qua nhà cung cấp dự phòng**

| Chủ đề | Câu nhận được / yêu cầu | Thời gian |
|--------|------------------------:|----------:|
| Đạo hàm của hàm số một biến | **3/3** | 4 071 ms |
| Câu bị động trong tiếng Anh | **3/3** | 2 015 ms |
| Cấu trúc dữ liệu ngăn xếp | **3/3** | 2 014 ms |
| **Tổng** | **9/9** | trung bình 2 700 ms |

Toàn bộ 9 câu đi qua bộ kiểm cấu trúc của chính hệ thống, không câu nào bị loại; nội dung tiếng Việt đúng chính tả và có dấu.

**Bảng 3.16. Độ trễ hai nhà cung cấp, lấy từ nhật ký giám sát**

| Nhà cung cấp | Số lượt | Độ trễ trung bình | Token vào TB | Token ra TB |
|--------------|--------:|------------------:|-------------:|------------:|
| Gemini (`gemini-3.6-flash`) | 18 | **10 526 ms** | 1 072 | 549 |
| Groq (`openai/gpt-oss-120b`) | 3 | **2 039 ms** | 658 | 586 |

Nhà cung cấp dự phòng nhanh hơn khoảng **năm lần** trên cùng loại tác vụ. Cần nói rõ hai điều để con số này không bị đọc quá: số lượt của Groq còn ít, và độ trễ của Gemini bao gồm cả những lần chạm hạn mức gói miễn phí phải chờ. Đây là **so sánh chỉ báo**, không phải một phép đo hiệu năng có kiểm soát.

**Điều phép đo này không chứng minh.** Chưa ép được một lần chuyển nhà cung cấp thật do lỗi tạm thời. Cách thử là đặt sai khoá của nhà cung cấp chính, và kết quả là nhà cung cấp dự phòng **không tiếp quản**. Kiểm lại thì đó là **hành vi đúng thiết kế**: lớp điều phối chỉ chuyển khi lỗi *tạm thời* — vượt hạn mức, lỗi phía máy chủ, mất mạng. Khoá sai là lỗi vĩnh viễn; gửi sang nhà cung cấp khác cũng hỏng y hệt, thử lại chỉ tốn thêm một lời gọi và một khoảng chờ.

Ép một lỗi tạm thời thật đòi hỏi chặn mạng ở mức hệ điều hành, nên không thực hiện trong phép đo này. Thay vào đó, logic chuyển được kiểm bằng **sáu phép kiểm đơn vị** phủ đúng các ranh giới: lỗi tạm thời thì chuyển; lỗi vĩnh viễn thì **không** chuyển mà ném ra ngoài; nhà cung cấp chưa cấu hình khoá thì bỏ qua từ đầu; mọi nhà cung cấp đều hỏng thì trả mã 503 kèm thông điệp người dùng hiểu được; và luồng theo dòng chỉ xét nhà cung cấp có hỗ trợ.

Phát biểu đúng phạm vi là: **nhà cung cấp dự phòng đã phục vụ thật qua ứng dụng, và logic chuyển đã được kiểm bằng phép kiểm tự động; còn một lần chuyển thật do lỗi tạm thời của nhà cung cấp chính thì chưa quan sát được.**

### 3.6.7. Giới hạn của phép đo

- **Đối chiếu với đáp án theo tiêu chí chấm, chưa phải với người chấm thật.** Muốn kết luận "AI chấm ngang giáo viên" thì cần nhiều người chấm độc lập cùng một bộ bài rồi so sánh.
- **Cỡ mẫu nhỏ**: 8 bài chấm, 10 câu sinh đề, 5 câu hỏi trợ lý. Đủ để phát hiện lỗi hệ thống và xu hướng, không đủ cho kết luận thống kê.
- **Không đo chất lượng sư phạm** của câu hỏi sinh ra.
- Kết quả gắn với **hai mô hình cụ thể tại một thời điểm cụ thể**; nhà cung cấp thay đổi mô hình thường xuyên, như chính dự án đã gặp ba lần.

---

**Tóm kết chương 3.** Chương này đã trình bày hệ thống ở trạng thái hoàn chỉnh: 16 nhóm chức năng hiện thực xong, chạy trên môi trường ba hệ quản trị dữ liệu dựng bằng Docker, với **649 phép kiểm tự động đều đạt** trên 63 lớp kiểm thử.

Hai phép đo bắt buộc theo phiếu giao đề tài đều cho kết quả cụ thể. Về hiệu năng thời gian thực, hệ thống phục vụ **100 người mỗi phòng với P95 là 216 ms và không mất sự kiện nào**, đồng thời xác định được nghẽn nằm ở kênh xử lý đáp án gửi lên chứ không ở khâu phát tán — kết luận chỉ rút ra được nhờ tách hai nguồn ra đo riêng. Về độ chính xác AI, chức năng chấm tự luận có sai lệch trung bình **0,13 trên thang 10**, chặn được cả hai kiểu tấn công tiêm chỉ thị, sinh đề đạt **10/10** câu đúng chuẩn cấu trúc, và trợ lý học tập **không suy đoán** khi câu hỏi nằm ngoài học liệu.

Chương cũng ghi lại hai kết quả âm tính có giá trị: **ba lỗi thật chỉ lộ ra khi mở sản phẩm ra dùng** trong khi toàn bộ bộ kiểm thử vẫn báo đạt, và **một lần chuyển nhà cung cấp mô hình do lỗi tạm thời chưa quan sát được** dù logic đã kiểm bằng phép kiểm tự động. Cả hai được nêu đúng phạm vi thay vì bỏ qua, vì một báo cáo chỉ ghi phần đạt thì người đọc không có căn cứ để tin phần còn lại.
