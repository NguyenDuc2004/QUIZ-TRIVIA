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

Biểu đồ use case tổng quát (Hình 2.1) thể hiện quan hệ giữa bốn tác nhân và các nhóm chức năng, trong đó có quan hệ tổng quát hóa giữa ba vai trò đã đăng nhập; các biểu đồ chi tiết theo từng tác nhân ở Hình 2.2 đến Hình 2.5.

[HÌNH 2.1: Biểu đồ use case tổng quát của hệ thống — cần chèn]

[HÌNH 2.2: Biểu đồ use case của tác nhân Khách — cần chèn]

[HÌNH 2.3: Biểu đồ use case của tác nhân Người học — cần chèn]

[HÌNH 2.4: Biểu đồ use case của tác nhân Người tạo nội dung — cần chèn]

[HÌNH 2.5: Biểu đồ use case của tác nhân Quản trị viên — cần chèn]

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

Phần này đặc tả chi tiết bảy use case tiêu biểu, bao quát cả bốn tác nhân và bốn trụ cột của đề tài: phòng đấu thời gian thực, sinh đề bằng RAG, trợ lý học tập bám học liệu và gợi ý dựa trên đồ thị. Mỗi use case kèm biểu đồ use case tương ứng phía trên bảng đặc tả.

#### 2.1.3.1. Use case Đăng nhập

[HÌNH 2.6: Biểu đồ use case Đăng nhập — cần chèn]

**Bảng 2.3. Đặc tả use case UC-01 — Đăng nhập**

| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Khách (người dùng đã có tài khoản) |
| Tiền điều kiện | Tài khoản đã tồn tại, được tạo qua đăng ký hoặc qua liên kết Google |
| Luồng chính | 1. Người dùng nhập email và mật khẩu. 2. Hệ thống chuẩn hóa email về chữ thường và tìm tài khoản. 3. Hệ thống so khớp mật khẩu với giá trị băm BCrypt đã lưu. 4. Hệ thống cấp access token 15 phút chứa vai trò và refresh token lưu ở Redis. 5. Giao diện hiển thị menu theo vai trò và chuyển vào trang danh sách quiz. |
| Luồng thay thế | 3a. Email không tồn tại hoặc mật khẩu sai → trả lỗi 401 với **cùng một thông báo** cho cả hai trường hợp, tránh để dò được email nào đã đăng ký. 1a. Chọn đăng nhập bằng Google → hệ thống xác minh ID token (chữ ký, tổ chức phát hành, hạn dùng, định danh ứng dụng nhận token), yêu cầu email đã được Google xác minh, rồi liên kết hoặc tạo tài khoản mới với vai trò LEARNER. 3b. Tài khoản chỉ đăng nhập bằng Google mà người dùng nhập mật khẩu → hướng dẫn dùng chức năng quên mật khẩu để đặt mật khẩu đầu tiên. |
| Hậu điều kiện | Phiên đăng nhập được thiết lập; các thiết bị khác không bị ảnh hưởng vì mỗi lần đăng nhập cấp một refresh token riêng |

#### 2.1.3.2. Use case Quản lý quiz và ngân hàng câu hỏi

[HÌNH 2.7: Biểu đồ use case Quản lý quiz và ngân hàng câu hỏi — cần chèn]

**Bảng 2.4. Đặc tả use case UC-03 — Quản lý quiz và ngân hàng câu hỏi**

| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Người tạo nội dung |
| Tiền điều kiện | Đã đăng nhập với vai trò CREATOR hoặc ADMIN |
| Luồng chính | 1. Người tạo nội dung tạo quiz: nhập tiêu đề, mô tả, danh mục, độ khó, thời lượng, chế độ hiển thị. 2. Hệ thống kiểm tra dữ liệu và lưu quiz với chủ sở hữu là người đang đăng nhập. 3. Soạn câu hỏi mới (một trong năm loại) hoặc chọn từ ngân hàng câu hỏi, kèm chủ đề, điểm, lời giải thích và tiêu chí chấm với câu tự luận. 4. Hệ thống kiểm tra dữ liệu theo từng loại câu hỏi. 5. Sắp thứ tự câu trong đề; hệ thống lưu liên kết giữa quiz và câu hỏi kèm thứ tự. 6. Tải ảnh bìa (tùy chọn); hệ thống nhận dạng ảnh theo chữ ký byte, giới hạn 2 MB, sinh tên tệp từ UUID rồi lưu. 7. Đặt chế độ công khai để người học tìm thấy quiz. |
| Luồng thay thế | 4a. Dữ liệu không hợp lệ (ví dụ câu một đáp án chưa có phương án đúng) → báo lỗi theo từng trường. 6a. Tệp không phải ảnh hoặc vượt 2 MB → báo lỗi, không lưu tệp. 3a. Chọn sinh câu hỏi bằng AI → chuyển sang UC-07. x1. Người dùng tác động lên quiz không thuộc sở hữu của mình → trả về 404 để không tiết lộ sự tồn tại của tài nguyên. |
| Hậu điều kiện | Quiz được lưu cùng danh sách câu hỏi; nếu ở chế độ công khai thì xuất hiện trong kết quả tìm kiếm của người học |

#### 2.1.3.3. Use case Làm bài quiz cá nhân

[HÌNH 2.8: Biểu đồ use case Làm bài quiz cá nhân — cần chèn]

**Bảng 2.5. Đặc tả use case UC-05 — Làm bài quiz cá nhân**

| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Người học |
| Tiền điều kiện | Đã đăng nhập; quiz tồn tại và người học có quyền truy cập |
| Luồng chính | 1. Người học chọn chế độ (luyện tập hoặc tính giờ) và bắt đầu. 2. Hệ thống tạo lượt làm bài và **sinh sẵn danh sách câu hỏi của riêng lượt này** kèm điểm tối đa từng câu để chốt đề. 3. Nếu quiz có thời lượng, hệ thống tính và lưu thời điểm hết hạn. 4. Hệ thống trả về đề bài **không kèm đáp án đúng**. 5. Người học trả lời từng câu; hệ thống lưu ngay tại thời điểm chọn, không chờ tới lúc nộp. 6. Người học nộp bài. 7. Hệ thống chấm các câu có đáp án xác định; câu trả lời ngắn đưa vào hàng đợi chấm bằng AI (UC-08). 8. Hệ thống tính tổng điểm, chuyển trạng thái đã nộp, trả kết quả kèm đáp án đúng và lời giải thích. 9. Hệ thống phát sự kiện đồng bộ hành vi sang đồ thị Neo4j (chạy nền). |
| Luồng thay thế | 1a. Người học đã có một lượt đang làm dở trên quiz này → hệ thống trả về đúng lượt đó để làm tiếp, không tạo lượt mới. 6a. Đã quá thời điểm hết hạn → lượt chuyển sang trạng thái hết hạn, chỉ chấm những câu đã trả lời. 7a. Gọi mô hình chấm thất bại → câu chuyển sang trạng thái chấm thất bại và chủ quiz chấm tay được, thay vì treo ở trạng thái đang chấm. |
| Hậu điều kiện | Lượt làm bài được lưu cùng điểm từng câu; hành vi được đồng bộ sang đồ thị phục vụ gợi ý |

#### 2.1.3.4. Use case Tham gia phòng đấu thời gian thực

[HÌNH 2.9: Biểu đồ use case Tham gia phòng đấu thời gian thực — cần chèn]

**Bảng 2.6. Đặc tả use case UC-06 — Tham gia phòng đấu thời gian thực**

| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Người học; Khách khi chủ phòng cho phép |
| Tiền điều kiện | Phòng tồn tại và đang ở trạng thái chờ |
| Luồng chính | 1. Chủ phòng mở phòng từ một quiz; hệ thống sinh mã PIN sáu ký tự (loại các ký tự dễ đọc nhầm) và mã QR. 2. Người chơi nhập mã PIN, chọn biệt danh và ảnh đại diện. 3. Hệ thống mở kết nối WebSocket, xác thực JWT tại khung STOMP CONNECT và đăng ký người chơi vào chủ đề của phòng. 4. Hệ thống phát danh sách người chơi cập nhật tới toàn bộ phòng. 5. Chủ phòng bắt đầu ván. 6. Hệ thống phát câu hỏi đồng thời tới mọi người chơi kèm thời gian giới hạn cho câu đó. 7. Người chơi chọn đáp án. 8. Hệ thống chấm ngay, tính điểm theo độ chính xác kết hợp thời gian trả lời, cập nhật trạng thái phòng ở Redis. 9. Hệ thống xuất bản sự kiện qua Redis Pub/Sub để mọi tiến trình máy chủ phát bảng xếp hạng mới tới người chơi của mình. 10. Lặp lại bước 6–9 tới câu cuối, sau đó phát kết quả cuối ván và ghi điểm cuối xuống cơ sở dữ liệu. |
| Luồng thay thế | 2a. Người chơi chưa có tài khoản → nếu chủ phòng đã bật tùy chọn cho khách thì được cấp khóa phiên khách (chỉ mở đúng phòng này, sống sáu giờ); nếu không, trả về 403. 3a. Token không hợp lệ → từ chối kết nối WebSocket. 7a. Không trả lời trong thời gian giới hạn → tính không điểm cho câu đó. 3b. Người chơi mất kết nối rồi vào lại → nhận lại trạng thái phòng hiện tại và **giữ nguyên điểm đã có**. |
| Hậu điều kiện | Kết quả ván được lưu; người chơi có tài khoản được ghi nhận vào lịch sử, dữ liệu của khách vãng lai không lưu ngoài phạm vi ván đấu |

#### 2.1.3.5. Use case Sinh đề bằng AI từ học liệu

[HÌNH 2.10: Biểu đồ use case Sinh đề bằng AI từ học liệu — cần chèn]

**Bảng 2.7. Đặc tả use case UC-07 — Sinh đề bằng AI từ học liệu**

| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Người tạo nội dung |
| Tiền điều kiện | Đã đăng nhập với vai trò CREATOR hoặc ADMIN; đã nạp học liệu và học liệu ở trạng thái sẵn sàng (đã sinh xong vector nhúng) |
| Luồng chính | 1. Người tạo nội dung nạp học liệu: tải tệp PDF/DOCX/TXT hoặc dán văn bản. 2. Hệ thống bóc tách văn bản bằng Apache Tika, chia đoạn, sinh vector nhúng cho từng đoạn, lưu vào kho vector và cập nhật trạng thái sang sẵn sàng. 3. Người tạo nội dung chọn học liệu, chủ đề, độ khó, loại câu hỏi và số lượng cần sinh (tối đa 20). 4. Hệ thống tạo công việc nền và **trả về mã công việc ngay**. 5. Hệ thống truy hồi các đoạn học liệu liên quan nhất tới chủ đề yêu cầu. 6. Hệ thống dựng prompt gồm chỉ dẫn hệ thống, ngữ cảnh học liệu được rào trong khối dữ liệu riêng, và lược đồ JSON đầu ra. 7. Hệ thống gọi mô hình qua lớp điều phối, phân tích và kiểm chứng JSON trả về, loại các câu sai cấu trúc. 8. Hệ thống lưu câu hỏi ở dạng nháp kèm nhà cung cấp, mô hình và các đoạn học liệu đã dựa vào. 9. Người tạo nội dung xem từng câu cùng đoạn học liệu nguồn, sửa nếu cần, rồi **duyệt** để đưa vào ngân hàng câu hỏi. |
| Luồng thay thế | 2a. Tệp hỏng, vượt 10 MB hoặc không bóc tách được văn bản → chuyển học liệu sang trạng thái thất bại kèm lý do hiển thị trên giao diện. 7a. Nhà cung cấp chính lỗi tạm thời (vượt hạn mức, lỗi máy chủ, hết thời gian chờ) → tự chuyển sang nhà cung cấp dự phòng. 7b. Vượt hạn mức số lượt mỗi phút → trả lỗi kèm **số giây cần chờ cụ thể** theo phản hồi của nhà cung cấp. 7c. Cả hai nhà cung cấp lỗi → công việc chuyển sang trạng thái thất bại kèm thông báo dễ hiểu. 9a. Không duyệt câu nào → câu hỏi nháp không vào ngân hàng, không ảnh hưởng dữ liệu hiện có. |
| Hậu điều kiện | Các câu hỏi được duyệt đã vào ngân hàng câu hỏi và dùng được cho quiz; mọi lời gọi mô hình được ghi nhật ký kèm số token và độ trễ |

#### 2.1.3.6. Use case Hỏi trợ lý học tập

[HÌNH 2.11: Biểu đồ use case Hỏi trợ lý học tập — cần chèn]

**Bảng 2.8. Đặc tả use case UC-09 — Hỏi trợ lý học tập**

| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Người học, Người tạo nội dung |
| Tiền điều kiện | Đã đăng nhập |
| Luồng chính | 1. Người dùng nhập câu hỏi (tối đa 2000 ký tự). 2. Hệ thống mở phiên hội thoại mới nếu chưa có và đặt tiêu đề phiên từ câu hỏi đầu tiên. 3. Hệ thống sinh vector nhúng cho câu hỏi. 4. Hệ thống truy hồi các đoạn học liệu gần nghĩa nhất **trong phạm vi người dùng được phép đọc**: tài liệu của chính họ và tài liệu người khác đã chủ động chia sẻ. 5. Hệ thống loại các đoạn có khoảng cách vượt ngưỡng liên quan. 6. Hệ thống dựng prompt gồm chỉ dẫn hệ thống, ngữ cảnh học liệu và lịch sử hội thoại của phiên. 7. Hệ thống gửi trước danh sách tài liệu sẽ dựa vào, rồi truyền câu trả lời theo từng mảnh chữ qua SSE. 8. Hệ thống lưu câu hỏi và câu trả lời vào phiên hội thoại. |
| Luồng thay thế | 5a. Không còn đoạn nào đủ liên quan → prompt nói rõ không có tài liệu liên quan; trợ lý **trả lời là không biết** thay vì suy đoán từ kiến thức nền. 7a. Mô hình lỗi trước khi phát mảnh chữ đầu tiên → chuyển sang nhà cung cấp dự phòng. 7b. Mô hình lỗi giữa luồng → phát sự kiện lỗi để giao diện hiển thị; **không** chuyển nhà cung cấp giữa dòng vì sẽ nối câu trả lời của hai mô hình thành một đoạn vô nghĩa. 1a. Người dùng dừng câu trả lời đang chạy → giao diện hủy yêu cầu. |
| Hậu điều kiện | Hội thoại được lưu và mở lại được; mỗi câu trả lời gắn với danh sách tài liệu đã dựa vào |

#### 2.1.3.7. Use case Nhận gợi ý bài thi và lộ trình học

[HÌNH 2.12: Biểu đồ use case Nhận gợi ý bài thi và lộ trình học — cần chèn]

**Bảng 2.9. Đặc tả use case UC-10 — Nhận gợi ý bài thi và lộ trình học**

| Thành phần | Nội dung |
|------------|----------|
| Tác nhân | Người học |
| Tiền điều kiện | Đã đăng nhập; đã có ít nhất một lượt làm bài để hệ thống có dữ liệu hành vi |
| Luồng chính | 1. Người học mở trang gợi ý. 2. Hệ thống truy vấn đồ thị tìm các chủ đề người học có độ chính xác thấp và các quiz thuộc chủ đề đó mà họ chưa từng làm. 3. Hệ thống truy vấn nhóm người học có nhiều bài làm trùng nhau, lấy các quiz họ đã làm mà người này chưa làm. 4. Hệ thống hợp nhất, xếp hạng và trả về danh sách gợi ý **kèm lý do gợi ý**. 5. Người học chọn một quiz trong danh sách và bắt đầu làm bài. |
| Luồng thay thế | 2a. Người học chưa có dữ liệu hành vi → trả về danh sách rỗng kèm hướng dẫn làm một bài để hệ thống hiểu năng lực, **không** gợi ý bừa theo độ phổ biến. 2b. Cơ sở dữ liệu đồ thị không phản hồi → trả về danh sách rỗng thay vì lỗi hệ thống; các chức năng khác không bị ảnh hưởng. |
| Hậu điều kiện | Người học nhận được danh sách gợi ý phù hợp năng lực kèm lý do để hiểu vì sao được gợi ý |

### 2.1.4. Biểu đồ lớp của hệ thống

Hình 2.13 thể hiện biểu đồ lớp thiết kế cho các lớp thực thể cốt lõi của hệ thống cùng quan hệ kết hợp và bội số giữa chúng: một người dùng sở hữu nhiều quiz, nhiều học liệu và nhiều phòng đấu; một quiz thuộc một danh mục và liên kết nhiều câu hỏi qua lớp trung gian có thứ tự; một câu hỏi có nhiều phương án trả lời; một học liệu chia thành nhiều đoạn có vector nhúng; một quiz có nhiều lượt làm bài, mỗi lượt có nhiều câu trả lời; một phòng đấu có nhiều người chơi; một người dùng có nhiều phiên hội thoại với trợ lý, mỗi phiên có nhiều tin nhắn.

[HÌNH 2.13: Biểu đồ lớp thiết kế tổng thể — cần chèn]

### 2.1.5. Hiện thực hóa use case

Phần này hiện thực hóa bảy use case tiêu biểu nêu trên. Theo phương pháp phân tích hướng đối tượng, mỗi use case được mô hình bằng biểu đồ trình tự (tương tác giữa các đối tượng theo thời gian) và biểu đồ lớp phân tích VOPC (View Of Participating Classes — các lớp tham gia theo ba khuôn mẫu «boundary», «control», «entity»).

#### 2.1.5.1. Use case Đăng nhập

Người dùng nhập thông tin trên lớp biên `LoginPage`; lớp điều khiển `AuthService` truy vấn lớp thực thể `User` qua `UserRepository`, so khớp mật khẩu rồi gọi `JwtService` sinh access token và `RefreshTokenService` lưu phiên vào Redis (Hình 2.14, 2.15).

[HÌNH 2.14: Biểu đồ trình tự use case Đăng nhập — cần chèn]

[HÌNH 2.15: Biểu đồ lớp VOPC use case Đăng nhập — cần chèn]

#### 2.1.5.2. Use case Quản lý quiz và ngân hàng câu hỏi

Trên lớp biên `QuizEditorPage`, `QuizService` và `QuestionService` kiểm tra dữ liệu theo từng loại câu hỏi, gọi `OwnershipGuard` xác nhận quyền sở hữu trước khi lưu thực thể `Quiz`, `Question` cùng các `QuestionOption` và liên kết `QuizQuestion` kèm thứ tự; ảnh bìa đi qua `FileStorageService` để kiểm tra chữ ký byte (Hình 2.16, 2.17).

[HÌNH 2.16: Biểu đồ trình tự use case Quản lý quiz và ngân hàng câu hỏi — cần chèn]

[HÌNH 2.17: Biểu đồ lớp VOPC use case Quản lý quiz và ngân hàng câu hỏi — cần chèn]

#### 2.1.5.3. Use case Làm bài quiz cá nhân

`AttemptService` tạo thực thể `QuizAttempt` và sinh sẵn các `AttemptAnswer` để chốt đề; `AnswerGrader` chấm các loại câu có đáp án xác định bằng logic thuần Java, còn câu tự luận được đánh dấu chờ chấm bằng AI. Khi nộp bài, hệ thống phát `AttemptSubmittedEvent` ở pha sau khi giao dịch được ghi nhận để `AttemptGraphSyncService` đồng bộ hành vi sang Neo4j (Hình 2.18, 2.19).

[HÌNH 2.18: Biểu đồ trình tự use case Làm bài quiz cá nhân — cần chèn]

[HÌNH 2.19: Biểu đồ lớp VOPC use case Làm bài quiz cá nhân — cần chèn]

#### 2.1.5.4. Use case Tham gia phòng đấu thời gian thực

Lớp biên `RoomPage` kết nối qua STOMP; `StompAuthChannelInterceptor` xác thực JWT tại khung CONNECT. `RoomService` quản lý vòng đời phòng, `RoomStateStore` giữ trạng thái đang chơi trên Redis, `SpeedScorer` tính điểm theo tốc độ, `GameEventPublisher` xuất bản sự kiện qua Redis Pub/Sub và `GameEventRelay` ở mỗi tiến trình máy chủ phát tiếp tới người chơi đang kết nối với nó; kết quả cuối ván ghi vào thực thể `GameRoom` và `GameRoomPlayer` (Hình 2.20, 2.21).

[HÌNH 2.20: Biểu đồ trình tự use case Tham gia phòng đấu thời gian thực — cần chèn]

[HÌNH 2.21: Biểu đồ lớp VOPC use case Tham gia phòng đấu thời gian thực — cần chèn]

#### 2.1.5.5. Use case Sinh đề bằng AI từ học liệu

Luồng RAG gồm hai pha. Pha nạp học liệu: `MaterialService` nhận tệp, `TextExtractor` (Apache Tika) bóc tách văn bản, `TextChunker` chia đoạn, `MaterialIngestionService` gọi `AiOrchestrator` sinh vector nhúng rồi lưu qua `MaterialChunkRepository`. Pha sinh đề: `AiJobService` tạo công việc nền và trả mã công việc; `QuestionGenerationService` truy hồi các đoạn liên quan, `QuestionPromptBuilder` dựng prompt, `AiOrchestrator` gọi `GeminiProvider` (dự phòng `GroqProvider`), `QuestionJsonParser` kiểm chứng kết quả trước khi lưu câu hỏi nháp; `AiRequestLogger` ghi nhật ký lời gọi (Hình 2.22, 2.23).

[HÌNH 2.22: Biểu đồ trình tự use case Sinh đề bằng AI từ học liệu — cần chèn]

[HÌNH 2.23: Biểu đồ lớp VOPC use case Sinh đề bằng AI từ học liệu — cần chèn]

#### 2.1.5.6. Use case Hỏi trợ lý học tập

Lớp biên `AssistantPage` gửi câu hỏi và nhận luồng SSE. `ChatService` gọi `AiOrchestrator` sinh vector nhúng cho câu hỏi, dùng `MaterialChunkRepository` truy hồi các đoạn **trong phạm vi được phép đọc** (tài liệu của người gọi hoặc đã chia sẻ), lọc theo ngưỡng khoảng cách, `ChatPromptBuilder` dựng prompt kèm lịch sử phiên, rồi `AiOrchestrator.stream` phát từng mảnh chữ về giao diện; hội thoại lưu vào `ChatSession` và `ChatMessage` (Hình 2.24, 2.25).

[HÌNH 2.24: Biểu đồ trình tự use case Hỏi trợ lý học tập — cần chèn]

[HÌNH 2.25: Biểu đồ lớp VOPC use case Hỏi trợ lý học tập — cần chèn]

#### 2.1.5.7. Use case Nhận gợi ý bài thi và lộ trình học

Lớp biên `RecommendationPage` gọi `RecommendationService`; lớp này dùng `RecommendationRepository` chạy các truy vấn Cypher trên Neo4j để tìm chủ đề người học còn yếu (từ quan hệ `PRACTICED`), các quiz thuộc chủ đề đó chưa từng làm (loại trừ theo quan hệ `ATTEMPTED`) và các quiz mà nhóm người học tương tự đã làm; kết quả được hợp nhất, xếp hạng kèm lý do gợi ý rồi trả về (Hình 2.26, 2.27).

[HÌNH 2.26: Biểu đồ trình tự use case Nhận gợi ý bài thi và lộ trình học — cần chèn]

[HÌNH 2.27: Biểu đồ lớp VOPC use case Nhận gợi ý bài thi và lộ trình học — cần chèn]

## 2.2. Thiết kế hệ thống

### 2.2.1. Thiết kế cơ sở dữ liệu

Hệ thống áp dụng nguyên tắc lưu trữ đa hệ với ba hệ quản trị, mỗi hệ đảm nhiệm loại dữ liệu phù hợp với đặc tính của nó: PostgreSQL 16 cho dữ liệu nghiệp vụ có tính giao dịch và kho vector học liệu, Neo4j 5 cho đồ thị hành vi phục vụ gợi ý, Redis cho dữ liệu ngắn hạn và thông điệp thời gian thực.

Cơ sở dữ liệu quan hệ được thiết kế theo các quy ước: dùng kiểu `uuid` làm khóa chính cho mọi bảng; đặt tên theo quy ước snake_case; cột thời gian dùng `timestamptz`; kiểu liệt kê lưu dạng `varchar` kèm ràng buộc `CHECK` thay vì kiểu enum của PostgreSQL để việc bổ sung giá trị mới không cần thay đổi kiểu; dữ liệu phi cấu trúc lưu `jsonb`. Mọi thay đổi lược đồ thực hiện qua migration Flyway được đánh số và không sửa lại tệp đã áp dụng, nhờ đó cơ sở dữ liệu ở mọi môi trường dựng lại được từ đầu một cách xác định. Hình 2.28 thể hiện sơ đồ thực thể quan hệ tổng quan. Để hình đọc được ở khổ giấy, sơ đồ **không vẽ các đường nối tới bảng `users`**: gần như mọi bảng đều có khóa ngoại `user_id` hoặc `owner_id` trỏ về `users`, và vẽ đủ thì bảng này trở thành một trục có hai mươi bảng treo vào, khiến sơ đồ dàn ngang và mất khả năng đọc. Các quan hệ đó vẫn tồn tại đầy đủ trong lược đồ.

[HÌNH 2.28: Sơ đồ thực thể quan hệ (ERD) tổng quan — cần chèn]

Lược đồ quan hệ gồm 35 bảng trên PostgreSQL, tạo qua 23 tệp migration Flyway được đánh số, tổ chức theo các nhóm chức năng. Bảng 2.10 đến Bảng 2.14 liệt kê nhóm dữ liệu lõi, Bảng 2.15 đến Bảng 2.19 liệt kê nhóm dữ liệu của các chức năng mở rộng, kèm mô tả ngắn gọn.

**Bảng 2.10. Nhóm người dùng và danh mục**

| Bảng | Mô tả |
|------|-------|
| `users` | Tài khoản người dùng: email, mật khẩu băm, định danh Google, tên hiển thị, ảnh đại diện, vai trò. Ràng buộc kiểm tra bảo đảm mỗi tài khoản có ít nhất một cách đăng nhập (mật khẩu hoặc Google) |
| `categories` | Danh mục quiz (tên, chuỗi định danh trên đường dẫn, mô tả) |

**Bảng 2.11. Nhóm quiz và ngân hàng câu hỏi**

| Bảng | Mô tả |
|------|-------|
| `quizzes` | Quiz: tiêu đề, mô tả, danh mục, độ khó, chế độ hiển thị, thời lượng, ảnh bìa, cờ đánh dấu nội dung sinh từ AI |
| `questions` | Câu hỏi trong ngân hàng (năm loại): nội dung, loại, độ khó, điểm, chủ đề dạng chữ tự do, lời giải thích, tiêu chí chấm cho câu tự luận, nguồn tạo và siêu dữ liệu AI |
| `question_options` | Phương án trả lời cho câu trắc nghiệm, kèm cờ đáp án đúng và thứ tự hiển thị |
| `quiz_questions` | Bảng nối nhiều-nhiều giữa quiz và câu hỏi, kèm thứ tự câu trong đề |

**Bảng 2.12. Nhóm làm bài và chấm điểm**

| Bảng | Mô tả |
|------|-------|
| `quiz_attempts` | Lượt làm bài của một người học: chế độ, trạng thái, thời điểm bắt đầu và hết hạn, tổng điểm và điểm tối đa chốt lúc bắt đầu. Chỉ mục một phần bảo đảm mỗi người tối đa một bài đang làm dở trên một quiz |
| `attempt_answers` | Từng câu trong đề của riêng một lượt làm bài, sinh sẵn lúc bắt đầu để chốt đề; lưu câu trả lời dạng `jsonb`, điểm, nhận xét và gợi ý của AI, người chấm và thời điểm chấm |

**Bảng 2.13. Nhóm học liệu, RAG và tác vụ AI**

| Bảng | Mô tả |
|------|-------|
| `learning_materials` | Học liệu người dùng nạp lên: tiêu đề, chủ đề, loại nguồn, trạng thái xử lý, số ký tự và số đoạn, lý do thất bại, và cờ `shared` cho phép người học khác hỏi trợ lý trên tài liệu này (mặc định tắt) |
| `material_chunks` | Các đoạn học liệu kèm vector nhúng 768 chiều (pgvector) phục vụ truy hồi ngữ nghĩa |
| `ai_jobs` | Tác vụ AI chạy nền (nạp học liệu, sinh đề): loại, trạng thái, tham số và kết quả dạng `jsonb` |
| `ai_request_logs` | Nhật ký mọi lời gọi mô hình: chức năng, nhà cung cấp, mô hình, số token vào/ra, độ trễ, trạng thái. Ghi trong giao dịch riêng để công việc chính thất bại thì bản ghi giám sát vẫn còn |

**Bảng 2.14. Nhóm phòng đấu và trợ lý học tập**

| Bảng | Mô tả |
|------|-------|
| `game_rooms` | Phòng đấu: mã PIN sáu ký tự, chủ phòng, quiz, trạng thái, thời gian mỗi câu, cờ cho phép khách vào chơi. Trạng thái đang chơi nằm ở Redis, không ở đây |
| `game_room_players` | Người chơi trong phòng: biệt danh và ảnh đại diện chốt tại thời điểm chơi, cờ khách vãng lai, điểm cuối ván. Cột người dùng cho phép rỗng để khách chơi được; ràng buộc duy nhất dùng chỉ mục một phần chỉ áp cho người có tài khoản |
| `chat_sessions` | Phiên hội thoại với trợ lý học tập, tiêu đề cắt từ câu hỏi đầu tiên |
| `chat_messages` | Từng lượt hỏi và trả lời trong một phiên hội thoại |

**Bảng 2.15. Nhóm thẻ ghi nhớ và lặp lại ngắt quãng**

| Bảng | Mô tả |
|------|-------|
| `flashcard_decks` | Bộ thẻ của một người dùng: tiêu đề, chủ đề |
| `flashcards` | Thẻ ghi nhớ: mặt trước, mặt sau, gợi ý, thẻ nhãn, và nguồn tạo (tự soạn, sinh từ AI, hoặc dựng từ câu đã làm sai) |
| `flashcard_reviews` | **Trạng thái lặp lại ngắt quãng theo từng cặp (thẻ, người dùng)**: hệ số dễ, khoảng cách ngày, số lần ôn đúng liên tiếp, ngày đến hạn. Tách riêng khỏi `flashcards` vì một thẻ dùng chung có thể được nhiều người ôn với lịch hoàn toàn khác nhau |

**Bảng 2.16. Nhóm trò chơi hóa và bảng xếp hạng theo mùa**

| Bảng | Mô tả |
|------|-------|
| `user_stats` | Tổng hợp cho mỗi người dùng: tổng điểm kinh nghiệm, cấp độ, chuỗi ngày học hiện tại và dài nhất, ngày hoạt động gần nhất |
| `xp_events` | Sổ từng lần cộng điểm kinh nghiệm: nguồn, khóa của hành động, số điểm. Ràng buộc duy nhất trên (người dùng, loại nguồn, khóa nguồn) là chốt **idempotent** ở tầng cơ sở dữ liệu — một hành động chỉ cộng điểm đúng một lần dù có gọi lại |
| `badges`, `user_badges` | Định nghĩa huy hiệu (mã, tên, điều kiện dạng `jsonb`) và bản ghi trao huy hiệu cho người dùng kèm thời điểm |
| `daily_challenges`, `user_daily_progress` | Thử thách của từng ngày (luật dạng `jsonb`, điểm thưởng) và tiến độ của mỗi người trên thử thách đó |
| `seasons`, `season_rankings` | Mùa giải (khoảng thời gian, trạng thái) và bảng xếp hạng **chốt lại khi mùa kết thúc**. Bảng xếp hạng đang diễn ra nằm ở Redis dạng Sorted Set, không ở đây — nó là chỉ mục dựng lại được từ `xp_events`, không phải nguồn sự thật |

**Bảng 2.17. Nhóm chống gian lận thi**

| Bảng | Mô tả |
|------|-------|
| `proctoring_events` | Nhật ký tín hiệu hành vi trong chế độ thi: loại tín hiệu (sáu loại, có ràng buộc kiểm tra), thời điểm phát sinh, và chi tiết dạng `jsonb`. Chi tiết **chỉ chứa số** — với thao tác dán chỉ lưu độ dài đoạn văn bản, không lưu nội dung |
| `attempt_integrity` | Bản tổng hợp của mỗi lượt thi: điểm rủi ro 0–100, danh sách cờ giải thích lý do dạng `jsonb`, nhận định của mô hình ngôn ngữ, và trạng thái rà soát. Ràng buộc duy nhất trên lượt thi bảo đảm tính lại không sinh dòng thứ hai |
| `room_proctoring_events` | Tín hiệu hành vi trong **phòng đấu thời gian thực**, tách riêng khỏi `proctoring_events` vì danh tính người chơi ở đây là định danh trong phạm vi phòng chứ không phải tài khoản — một phần người chơi là khách vãng lai nên bảng **không có** khóa ngoại tới `users`. Cột chỉ số câu hỏi là thứ làm nên khái niệm *khuôn lặp*: đếm số câu **khác nhau** có tín hiệu mới phân biệt được một lần bị gián đoạn với việc lặp đi lặp lại ở mọi câu |

Nhóm bảng chống gian lận có ba đặc điểm thiết kế xuất phát từ **ràng buộc đạo đức** chứ không từ nhu cầu kỹ thuật, nên cần nêu rõ. Thứ nhất, hai bảng này **chỉ có dữ liệu cho lượt thi tính điểm**; lượt luyện tập không sinh dòng nào, và máy chủ từ chối tín hiệu gửi lên cho lượt luyện tập. Thứ hai, cột chi tiết được máy chủ **dựng lại từ một danh sách trường vô hại** thay vì lưu nguyên gói tin của phía trình duyệt — phía trình duyệt đã chỉ đọc độ dài đoạn dán rồi bỏ chuỗi đi, nhưng nếu chỉ có một lớp bảo vệ thì một bản mã nguồn phía người dùng bị sửa đủ để nội dung chảy vào cơ sở dữ liệu. Thứ ba, cột trạng thái rà soát mặc định là *chờ rà soát* và **không có đường nào để hệ thống tự đổi giá trị đó**: tín hiệu thu từ trình duyệt có thể bị chặn hoặc giả mạo, nên chúng chỉ là cảnh báo hỗ trợ quyết định của con người. Giao diện phản ánh đúng điều này — mọi báo cáo đều hiện kèm một câu nhắc rằng điểm rủi ro không phải bằng chứng gian lận, và câu nhắc đó đặt ngay cạnh con số chứ không ở cuối trang.

Riêng bảng `material_chunks` có một đặc điểm thiết kế cần nêu rõ: cột vector nhúng **không** được lập chỉ mục xấp xỉ. Nguyên nhân đã trình bày ở mục 1.3.2 — truy vấn RAG phải lọc quyền đọc trước rồi mới xếp theo khoảng cách, trong khi chỉ mục xấp xỉ làm ngược lại nên bỏ sót kết quả một cách im lặng. Ở quy mô vài trăm tới vài nghìn đoạn, quét tuần tự trên tập đã lọc quyền vừa nhanh vừa không bỏ sót; khi kho vượt cỡ vài chục nghìn đoạn mới cần chỉ mục xấp xỉ, và lúc đó phải bật kèm cơ chế quét lặp của pgvector để chỉ mục tự tìm thêm khi bộ lọc quyền loại bớt ứng viên.

Mô hình đồ thị trên Neo4j gồm ba loại nút `User`, `Quiz`, `Topic` và ba loại quan hệ `ATTEMPTED`, `PRACTICED`, `COVERS` như đã trình bày ở mục 1.3.5; ràng buộc duy nhất trên định danh của cả ba loại nút được tạo lúc ứng dụng khởi động, thiếu bước này thì lệnh `MERGE` vẫn chạy nhưng quét toàn bộ nút mỗi lần và chậm dần theo kích thước đồ thị mà không có triệu chứng gì. Dữ liệu trên Redis gồm trạng thái phòng đang chơi, kênh xuất bản sự kiện ván đấu, khóa phiên khách vãng lai, refresh token cùng chỉ mục ngược từ người dùng tới các phiên của họ, mã OTP đặt lại mật khẩu cùng bộ đếm số lần thử sai, bộ đếm hạn mức gọi AI theo ngày, mốc tạm ngừng gọi nhà cung cấp AI đang quá tải, bộ đệm lời giải thích lý do gợi ý, bảng xếp hạng mùa đang chạy dạng tập hợp có thứ tự, và các khóa chống trùng của thông báo. Điểm chung của mọi khóa trên là chúng **dựng lại được**: Redis giữ chỉ mục và trạng thái ngắn hạn, PostgreSQL giữ nguồn sự thật.

**Bảng 2.18. Nhóm lớp học và giao bài**

| Bảng | Mô tả |
|------|-------|
| `classrooms` | Lớp học: tên, mô tả, giáo viên chủ nhiệm, và **mã lớp sáu ký tự** để học viên tự tham gia. Mã dùng chữ và số nhưng bỏ các ký tự dễ đọc nhầm, vì nó được đọc to trong lớp và chép tay lên bảng; dùng cả chữ chứ không chỉ số như mã PIN phòng đấu vì lớp học sống cả học kỳ nên cần không gian mã lớn hơn nhiều |
| `classroom_members` | Thành viên lớp kèm vai trò trong lớp (học viên hoặc trợ giảng). **Giáo viên chủ nhiệm không nằm trong bảng này** — họ là cột chủ sở hữu của `classrooms`; thêm một dòng thành viên nữa cho chủ nhiệm là tạo hai nguồn sự thật cho cùng một câu hỏi, và sớm muộn hai nguồn sẽ lệch nhau |
| `assignments` | Bài tập: gắn một quiz cho một lớp, kèm thời gian mở và hạn nộp đều tùy chọn. Khóa ngoại tới quiz đặt ở chế độ **hạn chế xóa** thay vì xóa lan truyền như phần lớn khóa ngoại khác, vì xóa một quiz đang được giao sẽ xóa luôn bài tập và mọi điểm số gắn với nó. Bảng **không** lưu trạng thái nộp: năm trạng thái (chưa làm, đang làm, đã nộp, nộp trễ, quá hạn) được tính khi hiển thị từ hạn nộp, lượt làm bài và thời điểm hiện tại — lưu thành cột thì phải có một công việc nền cập nhật nó lúc quá hạn, tức thêm một thứ có thể chết để giữ một giá trị vốn suy ra được |

**Bảng 2.19. Nhóm thông báo**

| Bảng | Mô tả |
|------|-------|
| `notifications` | Thông báo gửi tới một người dùng: loại, tiêu đề, nội dung, dữ liệu điều hướng dạng `jsonb`, cờ đã đọc, và **khóa chống trùng**. Chống trùng chặn bằng ràng buộc duy nhất ở tầng cơ sở dữ liệu chứ không kiểm trong mã ứng dụng: kiểm trong mã thua cuộc khi hai tiến trình máy chủ cùng thức dậy đúng mốc giờ đã hẹn, mà đó chính là tình huống sẽ xảy ra. Khóa để rỗng cho thông báo không cần chống trùng — ràng buộc duy nhất của PostgreSQL coi mỗi giá trị rỗng là khác nhau nên nhiều dòng cùng tồn tại được |
| `notification_settings` | Cài đặt của mỗi người dùng, lưu **danh sách loại đã tắt** chứ không phải danh sách đã bật. Người chưa từng mở trang cài đặt thì danh sách rỗng, nghĩa là nhận đủ mọi loại; lưu ngược lại thì người chưa cấu hình sẽ không nhận được gì |

Sơ đồ Hình 2.28 chỉ vẽ nhóm dữ liệu lõi. Các bảng của chức năng mở rộng không xuất hiện trên sơ đồ đó vì cùng lý do đã nêu với `users`: gần như mọi bảng trong ba nhóm cuối đều chỉ nối về `users` bằng một cạnh duy nhất, nên vẽ đủ 35 bảng chỉ làm sơ đồ dàn ngang mà không thêm thông tin nào về cấu trúc.

### 2.2.2. Thiết kế kiến trúc và mô-đun

Mã nguồn phía máy chủ được tổ chức theo nghiệp vụ dưới gói gốc `com.datn.quizai`, gồm các mô-đun `auth`, `user`, `quiz`, `attempt`, `file`, `realtime`, `ai`, `chat`, `recommend`, `analytics`, `admin`, `flashcard`, `gamification`, `season`, `integrity`, `classroom`, `notification`, cùng `common` (thực thể cơ sở, kiểm tra quyền sở hữu, DTO dùng chung, xử lý ngoại lệ) và `config` (cấu hình bảo mật, WebSocket, Redis Pub/Sub, tài liệu API). Nguyên tắc tổ chức là **nhóm theo tính năng, bên trong mỗi tính năng mới chia theo tầng** (`controller`, `service`, `repository`, `domain`, `dto`); nhờ vậy sửa một tính năng chỉ cần mở một thư mục mà ranh giới các tầng vẫn rõ. Hai gói `common` và `config` không chia theo tầng vì không phải tính năng nghiệp vụ. Thư mục kiểm thử phản chiếu đúng cấu trúc này.

Quan hệ phụ thuộc là một chiều Controller → Service → Repository → Domain. Controller không chứa logic nghiệp vụ và không bao giờ trả trực tiếp thực thể ra API mà chuyển qua DTO. Riêng lớp tích hợp AI được cô lập sau interface `AiProvider` cùng lớp điều phối `AiOrchestrator`, nên việc đổi hoặc thêm nhà cung cấp không ảnh hưởng tầng nghiệp vụ. Hình 2.29 thể hiện sơ đồ phân lớp và cấu trúc mô-đun.

[HÌNH 2.29: Sơ đồ phân lớp và cấu trúc mô-đun backend — cần chèn]

Phía giao diện cũng tổ chức theo tính năng dưới `src/features/<tên tính năng>`, mỗi tính năng gồm `api` (lời gọi máy chủ), `hooks` (logic dùng lại), `components`, `pages` và `store` khi cần; phần dùng chung đặt ở `src/shared`.

### 2.2.3. Thiết kế giao diện

Giao diện được thiết kế theo một bộ quy ước thống nhất để tránh việc mỗi trang có một phong cách riêng: màu sắc, bo góc và đổ bóng khai báo tập trung dưới dạng biến chứ không viết trực tiếp trong từng thành phần; nút hành động chính dùng màu tối, màu tím chỉ dành cho liên kết; trang dành cho người học trình bày theo lưới thẻ, trang quản lý trình bày theo bảng; các thành phần dùng chung như tiêu đề trang và trạng thái danh sách rỗng được tái sử dụng. Một quy tắc quan trọng là giao diện **không hiển thị dữ liệu không có thật**: các nền tảng thương mại thường hiện điểm đánh giá và số lượt học, hệ thống này chưa có dữ liệu đó nên không bịa ra để giao diện trông phong phú hơn. Phần này trình bày bản thiết kế của một số màn hình tiêu biểu.

Màn hình đăng nhập và đăng ký (Hình 2.30) dùng chung cho mọi vai trò, có thêm lối đăng nhập bằng tài khoản Google và liên kết đặt lại mật khẩu.

[HÌNH 2.30: Thiết kế giao diện màn hình Đăng nhập và Đăng ký — cần chèn]

Trang khám phá quiz (Hình 2.31) hiển thị lưới thẻ quiz kèm thanh tìm kiếm và bộ lọc theo danh mục, độ khó; đây cũng là trang khách chưa đăng nhập xem được, nhưng không truy cập được nội dung câu hỏi.

[HÌNH 2.31: Thiết kế giao diện trang khám phá quiz — cần chèn]

Màn hình làm bài (Hình 2.32) gồm nội dung câu hỏi, danh sách phương án, đồng hồ đếm ngược với bài có tính giờ, lưới điều hướng giữa các câu và nút nộp bài.

[HÌNH 2.32: Thiết kế giao diện màn hình làm bài — cần chèn]

Màn hình kết quả (Hình 2.33) hiển thị tổng điểm, danh sách câu kèm đáp án đúng và lời giải thích; riêng câu tự luận có thêm nhận xét và gợi ý cải thiện do AI sinh.

[HÌNH 2.33: Thiết kế giao diện màn hình kết quả làm bài — cần chèn]

Sảnh chờ và phòng đấu (Hình 2.34) gồm ô nhập mã PIN, danh sách người chơi đang chờ kèm mã QR để chia sẻ; khi vào ván, màn hình hiển thị câu hỏi và bảng xếp hạng cập nhật trực tiếp sau mỗi câu.

[HÌNH 2.34: Thiết kế giao diện sảnh chờ và phòng đấu thời gian thực — cần chèn]

Màn hình học liệu và sinh đề bằng AI (Hình 2.35) gồm danh sách học liệu kèm trạng thái xử lý và công tắc chia sẻ, biểu mẫu cấu hình sinh đề (học liệu nguồn, chủ đề, loại câu, số lượng, độ khó), và danh sách câu hỏi nháp chờ duyệt kèm đoạn học liệu nguồn để đối chiếu.

[HÌNH 2.35: Thiết kế giao diện học liệu và sinh đề bằng AI — cần chèn]

Màn hình trợ lý học tập (Hình 2.36) gồm cột danh sách hội thoại đã lưu, khung hội thoại chính, ô nhập câu hỏi và khối trích dẫn nguồn hiển thị dưới mỗi câu trả lời.

[HÌNH 2.36: Thiết kế giao diện màn hình trợ lý học tập — cần chèn]

Màn hình gợi ý và lộ trình học (Hình 2.37) hiển thị danh sách quiz được gợi ý kèm lý do gợi ý, cùng thứ tự chủ đề nên ôn dựa trên năng lực hiện tại.

[HÌNH 2.37: Thiết kế giao diện gợi ý và lộ trình học — cần chèn]

### Khu quản trị dùng khung giao diện riêng

Bảy màn trên thuộc khu học tập và dùng chung một khung: thanh điều hướng ngang cố định trên đầu. Khu
quản trị **không** dùng khung đó mà có bố cục riêng với thanh điều hướng dọc (sidebar) nền tối. Đây là
quyết định thiết kế, không phải khác biệt thẩm mỹ, dựa trên ba lý do xếp theo mức quan trọng:

**Thứ nhất, trông khác là một lớp an toàn.** Mọi thao tác ở khu học tập chỉ tác động lên dữ liệu của
chính người đang dùng. Ở khu quản trị thì khác: khoá tài khoản và đổi vai trò tác động lên **người
khác**, và không có nút hoàn tác. Một bố cục khác hẳn khiến quản trị viên luôn nhận biết mình đang ở
khu nào, thay vì tưởng vẫn ở trang cá nhân rồi thực hiện một thao tác không lấy lại được.

**Thứ hai, đây là hai ngữ cảnh làm việc khác nhau.** Các mục *Khám phá, Phòng đấu, Trợ lý AI, Lộ trình,
Tiến độ* không liên quan gì tới việc xem chi phí gọi mô hình hay xử lý một tài khoản vi phạm. Trộn hai
nhóm chức năng vào cùng một thanh menu buộc người dùng tự lọc ra mục mình cần ở mỗi lần dùng.

**Thứ ba, thanh dọc mở rộng được.** Với vai trò người tạo nội dung, thanh ngang của khu học tập đã có
mười mục và sẽ tràn hàng trên màn hình hẹp; trong khi khu quản trị còn hai nhóm chức năng dự kiến bổ
sung là kiểm duyệt nội dung và cấu hình nhà cung cấp AI.

Việc chuyển giữa hai khu đi được **cả hai chiều**: lối vào là mục *"Khu quản trị"* trong menu tài khoản
(chỉ hiện với vai trò quản trị viên), lối ra là mục *"Về khu học tập"* đặt ngay trong sidebar. Đặt lối
vào ở menu tài khoản chứ không ở thanh menu nội dung, vì vào khu quản trị là **chuyển ngữ cảnh** chứ
không phải điều hướng trong cùng một ngữ cảnh.

Màn hình quản lý người dùng (Hình 2.38) gồm sidebar điều hướng, bộ lọc theo từ khoá, vai trò và trạng
thái, cùng bảng danh sách người dùng với thao tác đổi vai trò và khoá tài khoản. **Không có thao tác xoá
người dùng** — lý do đã trình bày ở mục 2.2.1.

[HÌNH 2.38: Thiết kế giao diện quản lý người dùng (khu quản trị) — cần chèn]

Màn hình giám sát AI (Hình 2.39) gồm bốn thẻ số liệu tổng quan (tổng lượt gọi, tỉ lệ thất bại, tổng
token, độ trễ), cảnh báo khi có lượt phải dùng nhà cung cấp dự phòng, và hai bảng tách theo chức năng và
theo nhà cung cấp.

[HÌNH 2.39: Thiết kế giao diện giám sát chi phí AI (khu quản trị) — cần chèn]

## 2.3. Kết luận chương 2

Chương 2 đã phân tích bài toán và xác định bốn tác nhân cùng quan hệ tổng quát hóa giữa chúng, xây dựng biểu đồ use case và đặc tả bảy use case tiêu biểu bao quát bốn trụ cột của đề tài (mỗi đặc tả kèm biểu đồ use case tương ứng); hiện thực hóa cả bảy use case đó bằng biểu đồ trình tự và biểu đồ lớp VOPC theo chuẩn UML; đồng thời thiết kế biểu đồ lớp tổng thể, cơ sở dữ liệu trên ba hệ quản trị, kiến trúc mô-đun và giao diện. Trong quá trình đặc tả, các luồng thay thế đã được nêu rõ cho những tình huống mà hệ thống buộc phải xử lý đúng: chuyển nhà cung cấp AI khi lỗi tạm thời, giữ điểm khi người chơi mất kết nối giữa ván, trả lời "không biết" khi không có học liệu liên quan, và không gợi ý bừa khi chưa có dữ liệu hành vi. Các thiết kế này là cơ sở trực tiếp cho việc xây dựng, thử nghiệm và đánh giá hệ thống ở Chương 3.
