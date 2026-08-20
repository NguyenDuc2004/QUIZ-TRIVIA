# CHƯƠNG 2. KHẢO SÁT, PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG

Chương 1 đã trình bày cơ sở lý thuyết và các công nghệ được lựa chọn. Chương này chuyển từ công nghệ sang
bài toán: khảo sát nhu cầu thực tế, xác định yêu cầu chức năng và phi chức năng, mô hình hoá hệ thống bằng
use case, rồi thiết kế cơ sở dữ liệu và giao diện. Kết quả của chương là bản thiết kế đủ chi tiết để hiện
thực, được trình bày ở chương 3.

## 2.1. Khảo sát hệ thống

### 2.1.1. Mục đích khảo sát

Khảo sát nhằm trả lời ba câu hỏi trước khi thiết kế:

1. Người học và người dạy hiện gặp khó khăn gì khi ôn tập, kiểm tra bằng hình thức trắc nghiệm trực tuyến.
2. Trong ba hướng tích hợp trí tuệ nhân tạo mà đề tài đặt ra (sinh đề từ học liệu, chấm và giải thích câu
   tự luận, trợ lý hỏi–đáp), hướng nào được mong đợi nhất.
3. Những nền tảng đang phổ biến đã giải quyết được phần nào, còn để trống phần nào — làm cơ sở xác định
   phạm vi để hệ thống không lặp lại thứ đã có mà không thêm giá trị.

### 2.1.2. Phương pháp khảo sát

Đồ án sử dụng hai phương pháp bổ trợ nhau:

**Phân tích nền tảng tương tự.** Nghiên cứu trực tiếp hai nền tảng phổ biến nhất trong nhóm học tập trò
chơi hoá là Kahoot! và Quizizz: dùng thử luồng tạo bài, luồng tham gia phòng chơi, cơ chế tính điểm và
phần thống kê sau ván. Mục đích là xác định các quy ước thiết kế đã được kiểm chứng (mã tham gia phòng,
tính điểm theo tốc độ, bảng xếp hạng trực tiếp) và các khoảng trống về chức năng.

**Khảo sát bằng biểu mẫu trực tuyến.** Thu thập ý kiến từ nhóm người dùng mục tiêu về hình thức ôn tập
hiện tại, mức độ quan tâm tới thi đấu thời gian thực, và mức độ mong đợi ở từng chức năng AI.

### 2.1.3. Đối tượng khảo sát

| Nhóm | Đặc điểm | Điều cần tìm hiểu |
|---|---|---|
| Học sinh, sinh viên | Người ôn tập thường xuyên, quen dùng thiết bị di động | Hình thức ôn tập đang dùng; mức độ hứng thú với thi đấu; nhu cầu được giải thích khi làm sai |
| Giáo viên, người tạo nội dung | Người soạn đề và tổ chức kiểm tra | Thời gian dành cho việc soạn đề; nhu cầu sinh đề từ tài liệu có sẵn; mức độ tin tưởng câu hỏi do AI sinh |
| Người tự ôn thi | Học độc lập, không theo lớp | Nhu cầu được gợi ý nội dung học tiếp theo năng lực |

### 2.1.4. Kết quả khảo sát

**Kết quả phân tích nền tảng tương tự** đã trình bày ở mục 1.1. Bốn kết luận được dùng trực tiếp cho
thiết kế:

1. **Mô hình phòng chơi theo mã tham gia là đúng** và nên giữ: người chơi chỉ cần một mã ngắn, không cần
   tìm kiếm hay kết bạn. Hệ thống áp dụng mã PIN 6 ký tự.
2. **Cần mở rộng loại câu hỏi sang câu trả lời ngắn.** Các nền tảng hiện có hạn chế loại câu này vì khó
   chấm tự động — đúng chỗ mà LLM tạo ra khác biệt.
3. **Sinh đề từ học liệu riêng là khoảng trống rõ nhất.** Không nền tảng nào trong hai nền tảng khảo sát
   cho phép tải lên giáo trình rồi sinh câu hỏi từ đó.
4. **Gợi ý cần dựa trên dữ liệu hành vi**, không chỉ dựa trên độ phổ biến của bài thi.

**Kết quả khảo sát bằng biểu mẫu:** «Số phiếu thu được: … ; tỉ lệ theo từng nhóm đối tượng: … ; mức độ
quan tâm tới từng chức năng: … — cần bổ sung số liệu thật sau khi tổng hợp phiếu.»

> *Ghi chú:* mục này chỉ được điền khi đã tổng hợp phiếu khảo sát thật. Các kết luận thiết kế của đồ án
> hiện dựa trên phân tích nền tảng tương tự và yêu cầu trong phiếu giao đề tài, đều là căn cứ kiểm chứng
> được; số liệu biểu mẫu là phần bổ sung, không được suy đoán.

## 2.2. Các yêu cầu chức năng

Hệ thống gồm 16 nhóm chức năng được đặc tả, trong đó **9 nhóm thuộc mức bắt buộc** (Must) và đã hiện
thực, các nhóm còn lại ở mức nên có (Should) hoặc có thể có (Could).

### 2.2.1. Tổng hợp nhóm chức năng

| Nhóm | Chức năng chính | Tác nhân | Mức | Trạng thái |
|---|---|---|---|---|
| Xác thực & phân quyền | Đăng ký, đăng nhập (mật khẩu và Google), làm mới token, quên mật khẩu bằng OTP, đổi mật khẩu, quản lý hồ sơ, phân quyền RBAC | Guest → Learner/Creator/Admin | Must | Đã xong |
| Quản lý Quiz & câu hỏi | Thêm/sửa/xoá quiz, quản lý 5 loại câu hỏi, ngân hàng câu hỏi, danh mục, ảnh bìa, tìm kiếm và lọc | Creator, Admin | Must | Đã xong |
| Chơi quiz đơn | Bắt đầu bài làm, trả lời, nộp bài, chấm tự động, xem kết quả và giải thích, lịch sử làm bài | Learner | Must | Đã xong |
| Phòng đấu thời gian thực | Tạo phòng, tham gia bằng mã PIN hoặc QR, đồng bộ trạng thái, tính điểm theo tốc độ, bảng xếp hạng trực tiếp, cho khách vãng lai vào chơi | Learner, Creator | Must | Đã xong |
| AI sinh đề từ học liệu | Nạp học liệu (PDF/DOCX/TXT/dán tay), sinh câu hỏi theo chủ đề và độ khó, duyệt trước khi xuất bản | Creator | Must | Đã xong |
| AI chấm & giải thích tự luận | Chấm câu trả lời ngắn theo tiêu chí, sinh nhận xét và gợi ý cải thiện, cho phép người chấm ghi đè | Learner (nhận), Creator (ghi đè) | Must | Đã xong |
| Gợi ý cá nhân hoá | Gợi ý quiz theo chủ đề còn yếu, gợi ý theo người học tương tự, lộ trình học | Learner | Must | Đã xong |
| Trợ lý học tập | Hỏi–đáp bám học liệu, trả lời theo luồng, trích dẫn nguồn, lưu lịch sử hội thoại | Learner, Creator | Must | Đã xong |
| Thống kê & báo cáo | Tiến độ cá nhân, thống kê theo quiz cho chủ sở hữu | Learner, Creator | Should | Đã xong |
| Quản trị | Quản lý người dùng, nội dung, cấu hình AI, giám sát log và chi phí | Admin | Should | Chưa làm |
| Flashcard & lặp lại ngắt quãng | Ôn tập bằng thẻ, lịch ôn theo thuật toán SRS | Learner | Should | Chưa làm |
| Chống gian lận | Ghi nhận hành vi bất thường khi thi, đánh giá mức rủi ro | Learner (bị giám sát), Creator | Should | Chưa làm |
| Gamification | Điểm kinh nghiệm, huy hiệu, chuỗi ngày học, thử thách hằng ngày | Learner | Should | Chưa làm |
| Lớp học & giao bài | Tạo lớp, thêm thành viên, giao bài có hạn nộp | Creator, Learner | Should | Chưa làm |
| Bảng xếp hạng theo mùa | Xếp hạng theo chu kỳ, thưởng cuối mùa | Learner | Should | Chưa làm |
| Thông báo & nhắc ôn | Thông báo trong ứng dụng và qua email | Learner | Should | Chưa làm |

### 2.2.2. Yêu cầu chức năng chi tiết của các nhóm bắt buộc

**Xác thực và phân quyền**

| Mã | Yêu cầu |
|---|---|
| FR-1 | Đăng ký bằng email và mật khẩu; email chuẩn hoá chữ thường; mật khẩu băm bằng BCrypt |
| FR-2 | Đăng nhập, đăng xuất; cấp access token (JWT, hạn 15 phút) và refresh token (hạn 14 ngày, có xoay vòng) |
| FR-3 | Đăng nhập bằng tài khoản Google theo luồng ID token |
| FR-4 | Quên và đặt lại mật khẩu bằng mã OTP 6 chữ số gửi qua email |
| FR-5 | Quản lý hồ sơ cá nhân (tên hiển thị, ảnh đại diện) và đổi mật khẩu |
| FR-6 | Phân quyền theo ba vai trò LEARNER / CREATOR / ADMIN; tự đăng ký vai trò ADMIN bị hạ xuống LEARNER |

**Quản lý quiz và câu hỏi**

| Mã | Yêu cầu |
|---|---|
| FR-7 | Người tạo nội dung thêm, sửa, xoá quiz của mình; đặt danh mục, độ khó, chế độ hiển thị công khai hoặc riêng tư, thời lượng làm bài, ảnh bìa |
| FR-8 | Quản lý câu hỏi với năm loại: một đáp án, nhiều đáp án, đúng/sai, điền khuyết, trả lời ngắn |
| FR-9 | Ngân hàng câu hỏi dùng lại được cho nhiều quiz; mỗi câu có chủ đề, điểm, lời giải thích và tiêu chí chấm |
| FR-10 | Tìm kiếm và lọc quiz theo từ khoá, danh mục, độ khó |
| FR-11 | Khách chưa đăng nhập chỉ xem được danh sách và thông tin giới thiệu quiz công khai, **không** xem được nội dung câu hỏi |

**Chơi quiz đơn**

| Mã | Yêu cầu |
|---|---|
| FR-12 | Bắt đầu một lượt làm bài; đề được chốt tại thời điểm bắt đầu để chủ quiz sửa bài sau đó không ảnh hưởng lượt đang làm |
| FR-13 | Lưu câu trả lời ngay khi người học chọn, không chờ tới lúc nộp |
| FR-14 | Hai chế độ: luyện tập và làm bài tính giờ |
| FR-15 | Chấm tự động các loại câu có đáp án xác định; hiện điểm, đáp án đúng và lời giải thích sau khi nộp |
| FR-16 | Bài có thời lượng thì hết giờ tự chuyển sang trạng thái hết hạn |
| FR-17 | Xem lịch sử làm bài và chi tiết từng lượt |

**Phòng đấu thời gian thực**

| Mã | Yêu cầu |
|---|---|
| FR-18 | Chủ phòng tạo phòng từ một quiz, nhận mã PIN 6 ký tự và mã QR để chia sẻ |
| FR-19 | Người chơi tham gia bằng mã PIN; kết nối WebSocket được xác thực bằng JWT tại khung CONNECT |
| FR-20 | Chủ phòng bắt đầu ván; máy chủ phát câu hỏi đồng thời tới mọi người chơi |
| FR-21 | Tính điểm theo tốc độ trả lời: trả lời đúng và nhanh hơn thì điểm cao hơn |
| FR-22 | Bảng xếp hạng cập nhật trực tiếp sau mỗi câu |
| FR-23 | Chủ phòng có thể cho khách vãng lai (chưa có tài khoản) vào chơi; mặc định tắt. Dữ liệu của khách chỉ tồn tại trong một ván |
| FR-24 | Người chơi mất kết nối rồi vào lại vẫn giữ được điểm đã có |

**AI sinh đề từ học liệu (RAG)**

| Mã | Yêu cầu |
|---|---|
| FR-25 | Nạp học liệu từ tệp PDF, DOCX, TXT hoặc dán trực tiếp văn bản; giới hạn 10MB mỗi tài liệu |
| FR-26 | Hệ thống trích xuất văn bản, chia đoạn, sinh vector nhúng và lưu vào kho vector; trạng thái xử lý hiện rõ trên giao diện |
| FR-27 | Sinh câu hỏi từ học liệu theo chủ đề, độ khó, loại câu và số lượng yêu cầu (tối đa 20 câu mỗi lần) |
| FR-28 | Tác vụ sinh đề chạy nền, trả về mã công việc; người dùng theo dõi được trạng thái |
| FR-29 | Câu hỏi do AI sinh **không** tự vào ngân hàng; người tạo nội dung phải duyệt từng câu. Kết quả trả kèm đoạn học liệu đã dựa vào để đối chiếu |

**AI chấm và giải thích câu tự luận**

| Mã | Yêu cầu |
|---|---|
| FR-30a | Chấm câu trả lời ngắn dựa trên đáp án mẫu và tiêu chí chấm; trả về điểm, nhận xét và gợi ý cải thiện |
| FR-30b | Điểm mô hình trả về luôn bị giới hạn trong khoảng từ 0 tới điểm tối đa của câu |
| FR-30c | Chủ quiz ghi đè được điểm do AI chấm; sau khi người đã chấm thì kết quả AI về sau bị bỏ qua |
| FR-30d | Gọi mô hình thất bại thì câu chuyển sang trạng thái dừng rõ ràng, không treo ở trạng thái "đang chấm" |

**Trợ lý học tập (RAG chatbot)**

| Mã | Yêu cầu |
|---|---|
| FR-31 | Hỏi–đáp trên học liệu, hội thoại giữ được ngữ cảnh; phản hồi theo luồng (SSE); mỗi câu trả lời kèm danh sách tài liệu đã dựa vào; không có tài liệu liên quan thì trả lời rõ là không biết thay vì suy đoán |

**Gợi ý cá nhân hoá (Neo4j)**

| Mã | Yêu cầu |
|---|---|
| FR-32 | Gợi ý quiz theo chủ đề người học còn yếu và chưa từng làm |
| FR-33 | Gợi ý theo hành vi của những người học có kết quả tương tự |
| FR-34 | Lộ trình học đề xuất thứ tự chủ đề nên ôn dựa trên năng lực hiện tại |
| FR-35 | Đồng bộ dữ liệu hành vi từ cơ sở dữ liệu quan hệ sang đồ thị sau mỗi lượt nộp bài; dựng lại được đồ thị từ lịch sử |

## 2.3. Các yêu cầu phi chức năng

| Nhóm | Yêu cầu |
|---|---|
| **Hiệu năng thời gian thực** | Độ trễ đồng bộ trong phòng đấu phải thấp và ổn định giữa các người chơi, vì điểm số phụ thuộc thời gian trả lời. Chỉ tiêu P95 và số người chơi mỗi phòng được đo và báo cáo ở mục 3.5 |
| **Hiệu năng chung** | Truy vấn danh sách quiz có phân trang; truy vấn thống kê không quét toàn bảng; tác vụ AI nặng chạy nền để không giữ kết nối HTTP |
| **Độ tin cậy** | Hệ thống vẫn phục vụ được khi một thành phần phụ trợ lỗi: nhà cung cấp AI chính lỗi thì chuyển sang dự phòng; Neo4j lỗi thì API gợi ý trả danh sách rỗng chứ không làm hỏng việc nộp bài; cầu dao tránh gọi lặp nhà cung cấp đang lỗi |
| **Bảo mật xác thực** | Mật khẩu và mã OTP đều băm bằng BCrypt; access token ngắn hạn kết hợp refresh token có xoay vòng; phiên lưu ở Redis nên thu hồi được; đổi mật khẩu thu hồi phiên trên mọi thiết bị |
| **Bảo mật phân quyền** | Phân quyền theo vai trò ở tầng controller; kiểm tra quyền sở hữu tài nguyên; truy cập tài nguyên của người khác trả về 404 thay vì 403 để không tiết lộ sự tồn tại của tài nguyên |
| **Bảo mật AI** | Tách chỉ dẫn hệ thống khỏi dữ liệu người dùng để chống tiêm chỉ thị (prompt injection); rào nội dung do người dùng nhập trong khối dữ liệu; giới hạn cứng điểm số mô hình trả về; không gửi dữ liệu nhạy cảm tới mô hình; khoá API lưu trong biến môi trường, không đưa vào mã nguồn |
| **Cô lập dữ liệu** | Mọi truy vấn học liệu đều lọc theo quyền đọc: tài liệu của chính người gọi hoặc tài liệu người khác đã chủ động chia sẻ |
| **Khả năng bảo trì** | Kiến trúc phân lớp, module theo tính năng; mọi thay đổi lược đồ đi qua migration được đánh số; có kiểm thử tự động ở nhiều tầng |
| **Khả năng mở rộng** | API không trạng thái; trạng thái thời gian thực đặt ở Redis và đồng bộ qua Pub/Sub nên chạy được nhiều tiến trình máy chủ |
| **Tính khả dụng của giao diện** | Giao diện tiếng Việt, dùng lại một bộ thành phần và một hệ màu thống nhất; thông báo lỗi nói rõ nguyên nhân và việc cần làm; **không hiển thị dữ liệu không có thật** (ví dụ điểm đánh giá, số lượt học) chỉ để giao diện phong phú |

## 2.4. Xác định các tác nhân của hệ thống

| Tác nhân | Mô tả | Quyền chính |
|---|---|---|
| **Guest** | Khách chưa đăng nhập | Chỉ xem danh sách và thông tin giới thiệu quiz công khai (tiêu đề, mô tả, danh mục, độ khó, số câu). **Không** làm bài, **không** xem nội dung câu hỏi |
| **Learner** | Người học đã có tài khoản | Làm bài, vào phòng đấu, dùng trợ lý học tập, nhận gợi ý, xem tiến độ và lịch sử |
| **Creator** | Người tạo nội dung | Toàn bộ quyền của Learner, thêm: tạo và quản lý quiz, quản lý ngân hàng câu hỏi, nạp học liệu, sinh đề bằng AI, mở phòng đấu, xem thống kê quiz của mình, chấm tay câu tự luận |
| **Admin** | Quản trị viên | Quản lý người dùng và nội dung, cấu hình nhà cung cấp AI, giám sát nhật ký và chi phí |

**Hai điểm cần lưu ý về mô hình tác nhân:**

*Một người dùng có thể vừa là Learner vừa là Creator.* Vai trò CREATOR bao hàm toàn bộ quyền của LEARNER,
nên người tạo nội dung vẫn làm bài và dùng trợ lý học tập bình thường.

*Khách vãng lai trong phòng đấu là ngoại lệ có chủ đích.* Nguyên tắc chung của hệ thống là mọi hành vi tạo
ra dữ liệu học tập đều yêu cầu tài khoản. Riêng phòng đấu, khách biết mã PIN **và** được chủ phòng cho
phép thì vào chơi được — vì tình huống thực tế là quét mã QR trong lớp học, không phải ai cũng có tài
khoản. Đổi lại, khách dùng khoá phiên riêng (không phải JWT, chỉ mở đúng một phòng) và dữ liệu của họ chỉ
sống trong một ván: không có lịch sử làm bài, không vào thống kê, không lên đồ thị gợi ý.

**Quy tắc truy cập của Guest** được thể hiện trực tiếp trong cấu hình bảo mật:

| Đường dẫn | Guest |
|---|---|
| `POST /api/v1/auth/register`, `/login`, `/refresh`, `/forgot-password`, `/reset-password` | Cho phép |
| `GET /api/v1/quizzes`, `GET /api/v1/quizzes/{id}` (chỉ quiz công khai, **không** kèm câu hỏi) | Cho phép |
| `GET /api/v1/rooms/{pin}`, `POST /api/v1/rooms/{pin}/join-as-guest` | Cho phép (mã PIN là thứ chặn cửa; trả về 403 nếu chủ phòng không bật cho khách) |
| `POST /api/v1/quizzes/{id}/attempts`, toàn bộ `/api/v1/attempts/**` | 401 |
| `POST /api/v1/rooms`, WebSocket `/ws` | 401 |
| `/api/v1/ai/**`, `/api/v1/recommendations/**`, `/api/v1/users/me` | 401 |

## 2.5. Xây dựng biểu đồ use case

### 2.5.1. Danh sách use case của hệ thống

| Tác nhân | Use case |
|---|---|
| **Guest** | Xem danh sách quiz công khai · Xem giới thiệu một quiz · Đăng ký · Đăng nhập · Quên mật khẩu · Tham gia phòng đấu với tư cách khách (khi được cho phép) |
| **Learner** | Đăng xuất · Quản lý hồ sơ · Đổi mật khẩu · Tìm kiếm và lọc quiz · Làm bài · Nộp bài · Xem kết quả và giải thích · Xem lịch sử làm bài · Xem tiến độ học · Tham gia phòng đấu · Trả lời trong phòng đấu · Xem bảng xếp hạng · Hỏi trợ lý học tập · Xem hội thoại đã lưu · Nhận gợi ý quiz · Xem lộ trình học |
| **Creator** | *(toàn bộ use case của Learner)* · Quản lý quiz · Quản lý câu hỏi · Quản lý ngân hàng câu hỏi · Tải ảnh bìa · Nạp học liệu · Chia sẻ học liệu · Sinh đề bằng AI · Duyệt câu hỏi AI sinh · Mở phòng đấu · Điều khiển ván đấu · Xem thống kê quiz · Chấm tay câu tự luận |
| **Admin** | *(toàn bộ use case của Creator)* · Quản lý người dùng · Quản lý toàn bộ nội dung · Cấu hình nhà cung cấp AI · Giám sát nhật ký và chi phí AI |

### 2.5.2. Biểu đồ use case tổng quan

`[HÌNH 2.1: Biểu đồ use case tổng quan của hệ thống — bốn tác nhân Guest, Learner, Creator, Admin với
quan hệ tổng quát hoá (Creator kế thừa Learner, Admin kế thừa Creator) và các nhóm use case theo mục
2.5.1 — cần vẽ bằng draw.io hoặc StarUML và chèn vào]`

*Hình 2.1. Biểu đồ use case tổng quan*

`[HÌNH 2.2: Biểu đồ use case phân rã nhóm chức năng AI — nạp học liệu, sinh đề, duyệt câu hỏi, chấm tự
luận, trợ lý học tập — cần vẽ và chèn vào]`

*Hình 2.2. Biểu đồ use case nhóm chức năng trí tuệ nhân tạo*

## 2.6. Đặc tả chi tiết các use case

Mục này đặc tả các use case lõi, đại diện cho bốn trụ cột của đề tài. Các use case còn lại tuân theo cùng
khuôn mẫu đặc tả.

### 2.6.1. UC_DangKy — Đăng ký tài khoản

| Mã Use case | UC_DangKy | Tên Use case | Đăng ký tài khoản |
|---|---|---|---|
| Tác nhân | Guest | | |
| Mô tả | Khách tạo tài khoản mới để sử dụng các chức năng học tập | | |
| Sự kiện kích hoạt | Khách chọn "Đăng ký" trên giao diện | | |
| Tiền điều kiện | Khách chưa đăng nhập | | |

*Luồng sự kiện chính:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 1 | Guest | Nhập email, mật khẩu, tên hiển thị và chọn vai trò (người học hoặc người tạo nội dung) |
| 2 | Hệ thống | Kiểm tra định dạng email, độ dài mật khẩu từ 8 tới 72 ký tự, tên hiển thị không rỗng |
| 3 | Hệ thống | Chuẩn hoá email về chữ thường và kiểm tra email chưa tồn tại |
| 4 | Hệ thống | Băm mật khẩu bằng BCrypt và lưu tài khoản; vai trò ADMIN nếu được yêu cầu thì hạ xuống LEARNER |
| 5 | Hệ thống | Cấp access token và refresh token, trả về thông tin tài khoản |
| 6 | Hệ thống | Chuyển người dùng vào trang danh sách quiz ở trạng thái đã đăng nhập |

*Luồng sự kiện thay thế:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 3.1 | Hệ thống | Email đã tồn tại → trả về lỗi 409 kèm thông báo email đã được sử dụng; quay lại bước 1 |
| 2.1 | Hệ thống | Dữ liệu không hợp lệ → trả về lỗi 400 kèm thông báo cho từng trường sai; quay lại bước 1 |

*Hậu điều kiện:* tài khoản mới tồn tại trong hệ thống với vai trò LEARNER hoặc CREATOR; người dùng đang ở
trạng thái đã đăng nhập.

### 2.6.2. UC_DangNhap — Đăng nhập

| Mã Use case | UC_DangNhap | Tên Use case | Đăng nhập |
|---|---|---|---|
| Tác nhân | Guest | | |
| Mô tả | Người dùng đã có tài khoản xác thực để truy cập chức năng theo vai trò | | |
| Sự kiện kích hoạt | Người dùng gửi biểu mẫu đăng nhập, hoặc chọn đăng nhập bằng Google | | |
| Tiền điều kiện | Tài khoản đã tồn tại | | |

*Luồng sự kiện chính:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 1 | Guest | Nhập email và mật khẩu |
| 2 | Hệ thống | Tìm tài khoản theo email đã chuẩn hoá |
| 3 | Hệ thống | So khớp mật khẩu với giá trị băm BCrypt đã lưu |
| 4 | Hệ thống | Cấp access token (hạn 15 phút) chứa vai trò, và refresh token lưu ở Redis với thời gian sống 14 ngày |
| 5 | Hệ thống | Trả về thông tin tài khoản; giao diện hiển thị menu theo vai trò |

*Luồng sự kiện thay thế:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 3.1 | Hệ thống | Email không tồn tại hoặc mật khẩu sai → trả về lỗi 401 với **cùng một thông báo** cho cả hai trường hợp, tránh để kẻ tấn công dò được email nào đã đăng ký |
| 1.1 | Guest | Chọn đăng nhập bằng Google → hệ thống xác minh ID token (chữ ký, tổ chức phát hành, hạn dùng và định danh ứng dụng nhận token), yêu cầu email đã được Google xác minh, rồi liên kết hoặc tạo tài khoản mới với vai trò LEARNER |
| 3.2 | Hệ thống | Tài khoản chỉ đăng nhập bằng Google (không có mật khẩu) mà người dùng nhập mật khẩu → hướng dẫn dùng chức năng quên mật khẩu để đặt mật khẩu đầu tiên |

*Hậu điều kiện:* người dùng ở trạng thái đã đăng nhập; hệ thống ghi nhận một phiên mới, các thiết bị khác
không bị ảnh hưởng.

### 2.6.3. UC_QuanLyQuiz — Quản lý quiz

| Mã Use case | UC_QuanLyQuiz | Tên Use case | Quản lý quiz |
|---|---|---|---|
| Tác nhân | Creator, Admin | | |
| Mô tả | Người tạo nội dung tạo mới, chỉnh sửa, xoá và xuất bản quiz của mình | | |
| Sự kiện kích hoạt | Người tạo nội dung mở trang "Quiz của tôi" | | |
| Tiền điều kiện | Đã đăng nhập với vai trò CREATOR hoặc ADMIN | | |

*Luồng sự kiện chính:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 1 | Creator | Chọn "Tạo quiz", nhập tiêu đề, mô tả, danh mục, độ khó, thời lượng, chế độ hiển thị |
| 2 | Hệ thống | Kiểm tra dữ liệu và lưu quiz với chủ sở hữu là người đang đăng nhập |
| 3 | Creator | Thêm câu hỏi vào quiz: soạn mới hoặc chọn từ ngân hàng câu hỏi, sắp thứ tự |
| 4 | Hệ thống | Lưu liên kết giữa quiz và câu hỏi kèm thứ tự |
| 5 | Creator | Tải ảnh bìa (tuỳ chọn) |
| 6 | Hệ thống | Nhận dạng ảnh theo chữ ký byte, giới hạn 2MB, sinh tên tệp mới từ UUID rồi lưu |
| 7 | Creator | Đặt chế độ hiển thị công khai để người học tìm thấy quiz |

*Luồng sự kiện thay thế:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 2.1 | Hệ thống | Thiếu trường bắt buộc → lỗi 400 kèm chi tiết từng trường |
| 6.1 | Hệ thống | Tệp không phải ảnh hoặc vượt 2MB → lỗi 400, không lưu tệp |
| 3.1 | Creator | Chọn sinh câu hỏi bằng AI → chuyển sang UC_SinhDeAI |
| x.1 | Hệ thống | Người dùng tác động lên quiz không thuộc sở hữu của mình → trả về 404 |

*Hậu điều kiện:* quiz được lưu cùng danh sách câu hỏi; nếu ở chế độ công khai thì xuất hiện trong danh
sách tìm kiếm của người học.

### 2.6.4. UC_LamBai — Làm bài quiz (chơi đơn)

| Mã Use case | UC_LamBai | Tên Use case | Làm bài quiz |
|---|---|---|---|
| Tác nhân | Learner | | |
| Mô tả | Người học thực hiện một lượt làm bài và nhận kết quả | | |
| Sự kiện kích hoạt | Người học chọn "Bắt đầu làm bài" ở trang giới thiệu quiz | | |
| Tiền điều kiện | Đã đăng nhập; quiz tồn tại và người học có quyền truy cập | | |

*Luồng sự kiện chính:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 1 | Learner | Chọn chế độ (luyện tập hoặc tính giờ) và bắt đầu |
| 2 | Hệ thống | Tạo lượt làm bài, **sinh sẵn danh sách câu hỏi của riêng lượt này** kèm điểm tối đa từng câu để chốt đề |
| 3 | Hệ thống | Nếu quiz có thời lượng, tính và lưu thời điểm hết hạn |
| 4 | Hệ thống | Trả về đề bài **không kèm đáp án đúng** |
| 5 | Learner | Trả lời từng câu |
| 6 | Hệ thống | Lưu câu trả lời ngay tại thời điểm chọn, không chờ tới lúc nộp |
| 7 | Learner | Nộp bài |
| 8 | Hệ thống | Chấm các câu có đáp án xác định; câu trả lời ngắn đưa vào hàng đợi chấm bằng AI |
| 9 | Hệ thống | Tính tổng điểm, chuyển trạng thái sang đã nộp, trả về kết quả kèm đáp án đúng và lời giải thích |
| 10 | Hệ thống | Phát sự kiện đồng bộ hành vi sang đồ thị Neo4j (chạy nền) |

*Luồng sự kiện thay thế:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 1.1 | Hệ thống | Người học đã có một lượt đang làm dở trên quiz này → trả về đúng lượt đó để làm tiếp, không tạo lượt mới |
| 7.1 | Hệ thống | Đã quá thời điểm hết hạn → lượt chuyển sang trạng thái hết hạn, chỉ chấm những câu đã trả lời |
| 8.1 | Hệ thống | Gọi mô hình chấm thất bại → câu chuyển sang trạng thái chấm thất bại và chủ quiz chấm tay được, thay vì treo ở trạng thái đang chấm |

*Hậu điều kiện:* lượt làm bài được lưu cùng điểm từng câu; hành vi được đồng bộ sang đồ thị để phục vụ gợi
ý.

### 2.6.5. UC_ThamGiaPhongDau — Tham gia phòng đấu thời gian thực

| Mã Use case | UC_ThamGiaPhongDau | Tên Use case | Tham gia phòng đấu |
|---|---|---|---|
| Tác nhân | Learner, Guest *(khi được chủ phòng cho phép)* | | |
| Mô tả | Nhiều người chơi cùng trả lời một bộ câu hỏi trong thời gian thực, tính điểm theo tốc độ | | |
| Sự kiện kích hoạt | Người chơi nhập mã PIN hoặc quét mã QR của phòng | | |
| Tiền điều kiện | Phòng tồn tại và đang ở trạng thái chờ | | |

*Luồng sự kiện chính:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 1 | Creator | Mở phòng từ một quiz; hệ thống sinh mã PIN 6 ký tự (loại các ký tự dễ đọc nhầm) và mã QR |
| 2 | Learner | Nhập mã PIN, chọn biệt danh và ảnh đại diện |
| 3 | Hệ thống | Mở kết nối WebSocket; xác thực JWT tại khung STOMP CONNECT; đăng ký người chơi vào chủ đề của phòng |
| 4 | Hệ thống | Phát danh sách người chơi cập nhật tới toàn bộ phòng |
| 5 | Creator | Bắt đầu ván |
| 6 | Hệ thống | Phát câu hỏi đồng thời tới mọi người chơi kèm thời gian giới hạn cho câu đó |
| 7 | Learner | Chọn đáp án |
| 8 | Hệ thống | Chấm ngay, tính điểm theo độ chính xác và thời gian trả lời, cập nhật trạng thái phòng ở Redis |
| 9 | Hệ thống | Xuất bản sự kiện qua Redis Pub/Sub để mọi tiến trình máy chủ phát bảng xếp hạng mới tới người chơi của mình |
| 10 | Hệ thống | Lặp lại bước 6–9 cho tới câu cuối, sau đó phát kết quả cuối ván và ghi điểm cuối xuống cơ sở dữ liệu |

*Luồng sự kiện thay thế:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 2.1 | Guest | Người chơi chưa có tài khoản → nếu chủ phòng đã bật cho khách thì được cấp khoá phiên khách (chỉ mở đúng phòng này, sống 6 giờ); nếu không, trả về 403 |
| 3.1 | Hệ thống | Token không hợp lệ → từ chối kết nối WebSocket |
| 7.1 | Learner | Không trả lời trong thời gian giới hạn → tính 0 điểm cho câu đó |
| 3.2 | Learner | Mất kết nối rồi vào lại → nhận lại trạng thái phòng hiện tại và giữ nguyên điểm đã có |

*Hậu điều kiện:* kết quả ván được lưu; người chơi có tài khoản được ghi nhận vào lịch sử, dữ liệu của
khách vãng lai không lưu ngoài phạm vi ván đấu.

### 2.6.6. UC_SinhDeAI — Sinh đề bằng AI từ học liệu

| Mã Use case | UC_SinhDeAI | Tên Use case | Sinh đề bằng AI |
|---|---|---|---|
| Tác nhân | Creator | | |
| Mô tả | Sinh bộ câu hỏi trắc nghiệm từ học liệu do người tạo nội dung nạp lên, theo kiến trúc RAG | | |
| Sự kiện kích hoạt | Người tạo nội dung chọn "Sinh đề bằng AI" | | |
| Tiền điều kiện | Đã đăng nhập với vai trò CREATOR hoặc ADMIN; đã có ít nhất một học liệu ở trạng thái sẵn sàng |  | |

*Luồng sự kiện chính:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 1 | Creator | Nạp học liệu: tải tệp PDF/DOCX/TXT hoặc dán văn bản |
| 2 | Hệ thống | Trích xuất văn bản bằng Apache Tika, chia thành các đoạn, sinh vector nhúng cho từng đoạn và lưu vào kho vector; cập nhật trạng thái sang sẵn sàng |
| 3 | Creator | Chọn học liệu, chủ đề, độ khó, loại câu hỏi và số lượng cần sinh (tối đa 20) |
| 4 | Hệ thống | Tạo công việc nền và trả về mã công việc ngay |
| 5 | Hệ thống | Truy xuất các đoạn học liệu liên quan nhất tới chủ đề yêu cầu |
| 6 | Hệ thống | Dựng prompt gồm chỉ dẫn hệ thống, ngữ cảnh học liệu được rào trong khối dữ liệu riêng, và yêu cầu trả về JSON đúng cấu trúc |
| 7 | Hệ thống | Gọi mô hình qua lớp điều phối; phân tích và xác thực JSON trả về; loại các câu sai cấu trúc |
| 8 | Hệ thống | Lưu câu hỏi ở dạng nháp kèm thông tin nhà cung cấp, mô hình và các đoạn học liệu đã dựa vào |
| 9 | Creator | Xem từng câu cùng đoạn học liệu nguồn, sửa nếu cần, rồi **duyệt** để đưa vào ngân hàng câu hỏi |

*Luồng sự kiện thay thế:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 2.1 | Hệ thống | Tệp hỏng, vượt 10MB hoặc không trích được văn bản → chuyển học liệu sang trạng thái thất bại kèm lý do hiển thị trên giao diện |
| 7.1 | Hệ thống | Nhà cung cấp chính lỗi tạm thời (vượt hạn mức, lỗi máy chủ, hết thời gian chờ) → chuyển sang nhà cung cấp dự phòng |
| 7.2 | Hệ thống | Vượt hạn mức số lượt mỗi phút → trả về lỗi kèm **số giây cần chờ cụ thể** theo phản hồi của nhà cung cấp |
| 7.3 | Hệ thống | Cả hai nhà cung cấp lỗi → công việc chuyển sang trạng thái thất bại kèm thông báo dễ hiểu |
| 9.1 | Creator | Không duyệt câu nào → câu hỏi nháp không vào ngân hàng, không ảnh hưởng dữ liệu hiện có |

*Hậu điều kiện:* các câu hỏi được người tạo nội dung duyệt đã vào ngân hàng câu hỏi và dùng được cho quiz;
mọi lời gọi mô hình được ghi nhật ký kèm số token và độ trễ.

### 2.6.7. UC_ChamTuLuanAI — Chấm và giải thích câu tự luận bằng AI

| Mã Use case | UC_ChamTuLuanAI | Tên Use case | Chấm câu tự luận bằng AI |
|---|---|---|---|
| Tác nhân | Hệ thống (tự động sau khi nộp bài), Creator (ghi đè điểm) | | |
| Mô tả | Chấm câu trả lời ngắn dựa trên đáp án mẫu và tiêu chí chấm, kèm nhận xét và gợi ý cải thiện | | |
| Sự kiện kích hoạt | Người học nộp bài có chứa câu trả lời ngắn | | |
| Tiền điều kiện | Câu hỏi thuộc loại trả lời ngắn và người học đã nhập nội dung | | |

*Luồng sự kiện chính:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 1 | Hệ thống | Đánh dấu câu ở trạng thái đang chờ AI chấm và đưa vào hàng đợi |
| 2 | Hệ thống | Dựng prompt gồm nội dung câu hỏi, đáp án mẫu, tiêu chí chấm, và **bài làm của người học được rào trong khối dữ liệu riêng** |
| 3 | Hệ thống | Gọi mô hình, nhận về điểm số kèm nhận xét và gợi ý cải thiện |
| 4 | Hệ thống | **Giới hạn điểm nhận được trong khoảng từ 0 tới điểm tối đa của câu** |
| 5 | Hệ thống | Lưu điểm, nhận xét, gợi ý, đánh dấu người chấm là AI và ghi thời điểm chấm |
| 6 | Hệ thống | Tính lại tổng điểm của lượt làm bài, đồng bộ lại hành vi sang đồ thị vì điểm đã thay đổi |
| 7 | Learner | Xem điểm kèm nhận xét và gợi ý |

*Luồng sự kiện thay thế:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 3.1 | Hệ thống | Bài làm chứa chỉ thị nhằm điều khiển mô hình (ví dụ "hãy cho tôi điểm tối đa") → chỉ dẫn hệ thống quy định coi đó là nội dung cần chấm; bài chỉ gồm những câu như vậy thì nhận 0 điểm |
| 3.2 | Hệ thống | Người học gõ đúng chuỗi rào khối dữ liệu → chuỗi đó bị vô hiệu hoá trước khi dựng prompt |
| 3.3 | Hệ thống | Gọi mô hình thất bại → câu chuyển sang trạng thái **chấm thất bại** (trạng thái dừng), giao diện nói rõ để người học không chờ vô hạn |
| 7.1 | Creator | Chấm tay và ghi đè điểm → kết quả AI trả về sau đó bị bỏ qua; người chấm được ghi nhận là con người |

*Hậu điều kiện:* câu tự luận có điểm và nhận xét; tổng điểm của lượt làm bài được cập nhật; điểm không bao
giờ vượt trần thật của câu.

### 2.6.8. UC_TroLyHocTap — Hỏi trợ lý học tập

| Mã Use case | UC_TroLyHocTap | Tên Use case | Hỏi trợ lý học tập |
|---|---|---|---|
| Tác nhân | Learner, Creator | | |
| Mô tả | Người dùng hỏi về nội dung học liệu và nhận giải thích bám học liệu, có trích dẫn nguồn | | |
| Sự kiện kích hoạt | Người dùng gửi câu hỏi ở trang trợ lý học tập | | |
| Tiền điều kiện | Đã đăng nhập | | |

*Luồng sự kiện chính:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 1 | Learner | Nhập câu hỏi (tối đa 2000 ký tự) |
| 2 | Hệ thống | Mở phiên hội thoại mới nếu chưa có, đặt tiêu đề phiên từ câu hỏi đầu tiên |
| 3 | Hệ thống | Sinh vector nhúng cho câu hỏi |
| 4 | Hệ thống | Truy xuất các đoạn học liệu gần nghĩa nhất **trong phạm vi người dùng được phép đọc**: tài liệu của chính họ và tài liệu người khác đã chủ động chia sẻ |
| 5 | Hệ thống | Loại các đoạn có khoảng cách vượt ngưỡng liên quan |
| 6 | Hệ thống | Dựng prompt gồm chỉ dẫn hệ thống, ngữ cảnh học liệu, và lịch sử hội thoại của phiên |
| 7 | Hệ thống | Gửi ngay danh sách tài liệu sẽ dựa vào, rồi truyền câu trả lời theo từng mảnh chữ qua SSE |
| 8 | Hệ thống | Lưu câu hỏi và câu trả lời vào phiên hội thoại |

*Luồng sự kiện thay thế:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 5.1 | Hệ thống | Không còn đoạn nào đủ liên quan → prompt nói rõ không có tài liệu liên quan; trợ lý **trả lời là không biết** thay vì suy đoán từ kiến thức nền |
| 7.1 | Hệ thống | Mô hình lỗi trước khi phát mảnh chữ đầu tiên → chuyển sang nhà cung cấp dự phòng |
| 7.2 | Hệ thống | Mô hình lỗi giữa luồng → phát sự kiện lỗi để giao diện hiển thị; **không** chuyển nhà cung cấp giữa dòng vì sẽ nối hai câu trả lời khác nhau thành một đoạn vô nghĩa |
| 1.1 | Learner | Dừng câu trả lời đang chạy → giao diện huỷ yêu cầu |

*Hậu điều kiện:* hội thoại được lưu và mở lại được; mỗi câu trả lời gắn với danh sách tài liệu đã dựa vào.

### 2.6.9. UC_GoiYCaNhanHoa — Nhận gợi ý quiz và lộ trình học

| Mã Use case | UC_GoiYCaNhanHoa | Tên Use case | Nhận gợi ý cá nhân hoá |
|---|---|---|---|
| Tác nhân | Learner | | |
| Mô tả | Hệ thống gợi ý quiz nên làm tiếp và thứ tự chủ đề nên ôn, dựa trên đồ thị hành vi | | |
| Sự kiện kích hoạt | Người học mở trang gợi ý hoặc trang lộ trình học | | |
| Tiền điều kiện | Đã đăng nhập; đã có ít nhất một lượt làm bài để hệ thống có dữ liệu hành vi | | |

*Luồng sự kiện chính:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 1 | Learner | Mở trang gợi ý |
| 2 | Hệ thống | Truy vấn đồ thị: các chủ đề người học có độ chính xác thấp, các quiz thuộc chủ đề đó mà họ chưa từng làm |
| 3 | Hệ thống | Truy vấn nhóm người học có nhiều bài làm trùng nhau, lấy các quiz họ đã làm mà người này chưa làm |
| 4 | Hệ thống | Hợp nhất, xếp hạng và trả về danh sách gợi ý kèm lý do gợi ý |
| 5 | Learner | Chọn một quiz trong danh sách và bắt đầu làm bài |

*Luồng sự kiện thay thế:*

| # | Thực hiện bởi | Hành động |
|---|---|---|
| 2.1 | Hệ thống | Người học chưa có dữ liệu hành vi → trả về danh sách rỗng kèm hướng dẫn làm một bài để hệ thống hiểu năng lực, **không** gợi ý bừa theo độ phổ biến |
| 2.2 | Hệ thống | Cơ sở dữ liệu đồ thị không phản hồi → trả về danh sách rỗng thay vì lỗi hệ thống; các chức năng khác không bị ảnh hưởng |

*Hậu điều kiện:* người học nhận được danh sách gợi ý phù hợp năng lực, kèm lý do để hiểu vì sao được gợi ý.

> **Các use case còn lại** — Đăng xuất, Quản lý hồ sơ, Đổi mật khẩu, Quên mật khẩu, Tìm kiếm và lọc quiz,
> Quản lý câu hỏi, Xem kết quả và lịch sử, Xem tiến độ học, Chia sẻ học liệu, Xem thống kê quiz, Quản lý
> người dùng (Admin) — được đặc tả theo cùng khuôn mẫu trên.

## 2.7. Phân tích use case bằng biểu đồ trình tự và biểu đồ lớp

Mục này mô tả tương tác giữa các lớp theo thời gian đối với các use case lõi. Các biểu đồ tuân theo mô
hình phân lớp **boundary – control – entity**, tương ứng với tầng Controller, Service và Domain trong mã
nguồn.

`[HÌNH 2.3: Biểu đồ trình tự UC_DangNhap — Client → AuthController → AuthService → UserRepository /
JwtService / RefreshTokenService (Redis) — cần vẽ và chèn vào]`

*Hình 2.3. Biểu đồ trình tự use case Đăng nhập*

`[HÌNH 2.4: Biểu đồ trình tự UC_LamBai — Client → AttemptController → AttemptService → QuizAttemptRepository,
AnswerGrader; nhánh nộp bài phát AttemptSubmittedEvent sang job đồng bộ Neo4j — cần vẽ và chèn vào]`

*Hình 2.4. Biểu đồ trình tự use case Làm bài quiz*

`[HÌNH 2.5: Biểu đồ trình tự UC_ThamGiaPhongDau — Client (STOMP) → RoomStompController → RoomService →
RoomStateStore (Redis), SpeedScorer, GameEventPublisher (Redis Pub/Sub) → GameEventRelay → các client khác
— cần vẽ và chèn vào]`

*Hình 2.5. Biểu đồ trình tự use case Tham gia phòng đấu thời gian thực*

`[HÌNH 2.6: Biểu đồ trình tự UC_SinhDeAI — Creator → AiController → MaterialService (Tika, chunk,
embedding) → MaterialChunkRepository (pgvector); nhánh sinh đề: AiJobService → QuestionGenerationService →
AiOrchestrator → GeminiProvider (dự phòng GroqProvider) → QuestionJsonParser — cần vẽ và chèn vào]`

*Hình 2.6. Biểu đồ trình tự use case Sinh đề bằng AI*

`[HÌNH 2.7: Biểu đồ trình tự UC_TroLyHocTap — Client → ChatController → ChatService → AiOrchestrator.embed
→ MaterialChunkRepository.searchSimilarIncludingShared → ChatPromptBuilder → AiOrchestrator.stream → luồng
SSE về client — cần vẽ và chèn vào]`

*Hình 2.7. Biểu đồ trình tự use case Hỏi trợ lý học tập*

`[HÌNH 2.8: Biểu đồ lớp tổng quan các thực thể chính — User, Category, Quiz, Question, QuestionOption,
QuizQuestion, QuizAttempt, AttemptAnswer, GameRoom, GameRoomPlayer, LearningMaterial, MaterialChunk,
ChatSession, ChatMessage, AiJob — cần vẽ và chèn vào]`

*Hình 2.8. Biểu đồ lớp các thực thể chính của hệ thống*

## 2.8. Xây dựng cơ sở dữ liệu

Hệ thống áp dụng **lưu trữ đa hệ**: PostgreSQL cho dữ liệu nghiệp vụ và kho vector, Neo4j cho đồ thị hành
vi, Redis cho dữ liệu ngắn hạn và thông điệp thời gian thực. Mục này trình bày cả ba, trong đó cơ sở dữ
liệu quan hệ được mô tả chi tiết theo từng bảng.

### 2.8.1. Biểu đồ thực thể liên kết (ERD)

Quan hệ giữa các bảng chính:

```
users ──1:N── quizzes ──1:N── quiz_questions ──N:1── questions
  │                                                     │
  │                                   questions ──1:N── question_options
  │
  ├──1:N── quiz_attempts ──1:N── attempt_answers
  ├──1:N── chat_sessions ──1:N── chat_messages
  ├──1:N── learning_materials ──1:N── material_chunks (embedding)
  └──1:N── game_rooms ──1:N── game_room_players

categories ──1:N── quizzes
ai_jobs, ai_request_logs (nhật ký và giám sát các lần gọi AI)
```

`[HÌNH 2.9: Biểu đồ thực thể liên kết đầy đủ của cơ sở dữ liệu PostgreSQL, thể hiện khoá chính, khoá
ngoại và bậc quan hệ theo sơ đồ trên — cần vẽ bằng draw.io hoặc công cụ mô hình hoá và chèn vào]`

*Hình 2.9. Biểu đồ thực thể liên kết của cơ sở dữ liệu*

### 2.8.2. Các bảng trong cơ sở dữ liệu

**Bảng `users` — người dùng hệ thống**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh người dùng | PK, NOT NULL |
| email | varchar(255) | Địa chỉ email, chuẩn hoá chữ thường | UNIQUE, NOT NULL |
| password_hash | varchar | Mật khẩu đã băm BCrypt | NULL nếu tài khoản chỉ đăng nhập bằng Google |
| google_id | varchar(64) | Định danh `sub` của Google | NULL nếu không liên kết Google; UNIQUE bỏ qua NULL |
| display_name | varchar(100) | Tên hiển thị | NOT NULL |
| avatar_url | varchar | Đường dẫn ảnh đại diện | NULL |
| role | varchar(20) | Vai trò: LEARNER / CREATOR / ADMIN | NOT NULL, mặc định LEARNER |
| created_at, updated_at | timestamptz | Thời điểm tạo và cập nhật | NOT NULL |

> Ràng buộc `CHECK (password_hash IS NOT NULL OR google_id IS NOT NULL)`: mỗi tài khoản phải có ít nhất
> một cách đăng nhập, không để lọt bản ghi không vào được bằng đường nào.

**Bảng `categories` — danh mục quiz**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh danh mục | PK |
| name | varchar | Tên danh mục | NOT NULL |
| slug | varchar | Chuỗi định danh dùng trên đường dẫn | UNIQUE |
| description | text | Mô tả | NULL |

**Bảng `quizzes` — bài thi**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh quiz | PK |
| owner_id | UUID | Người tạo quiz | FK → users |
| category_id | UUID | Danh mục | FK → categories, NULL |
| title | varchar | Tiêu đề | NOT NULL |
| description | text | Mô tả | NULL |
| difficulty | varchar | Độ khó: easy / medium / hard | NOT NULL |
| visibility | varchar | Chế độ hiển thị: public / private | NOT NULL |
| is_ai_generated | boolean | Đánh dấu quiz sinh từ AI | Mặc định false |
| time_limit_sec | integer | Thời lượng làm bài (giây) | NULL = không giới hạn |
| thumbnail_url | varchar(500) | Ảnh bìa do máy chủ sinh đường dẫn | NULL = giao diện tự vẽ khối màu |
| created_at, updated_at | timestamptz | Thời điểm tạo và cập nhật | NOT NULL |

**Bảng `questions` — câu hỏi**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh câu hỏi | PK |
| owner_id | UUID | Người soạn câu hỏi | FK → users |
| type | varchar | Loại: single / multiple / true_false / fill_blank / short_answer | NOT NULL |
| content | text | Nội dung câu hỏi | NOT NULL |
| explanation | text | Lời giải thích, hiện sau khi nộp bài | NULL |
| rubric | text | Tiêu chí chấm câu tự luận | NULL; thiếu thì hai lần chấm cùng một bài dễ lệch nhau |
| difficulty | varchar | Độ khó | NOT NULL |
| topic | varchar | Chủ đề, dạng chữ tự do | NULL; không tách thành bảng riêng để không buộc tạo chủ đề trước mới soạn được câu |
| points | integer | Điểm tối đa của câu | NOT NULL |
| source | varchar | Nguồn: manual / ai_generated | NOT NULL |
| ai_metadata | jsonb | Nhà cung cấp, mô hình, mã băm prompt | NULL |

**Bảng `question_options` — phương án trả lời**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh phương án | PK |
| question_id | UUID | Câu hỏi tương ứng | FK → questions |
| content | text | Nội dung phương án | NOT NULL |
| is_correct | boolean | Là phương án đúng hay không | NOT NULL |
| order_index | integer | Thứ tự hiển thị | NOT NULL |

**Bảng `quiz_questions` — bảng nối quiz và câu hỏi**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| quiz_id | UUID | Quiz | FK → quizzes, thuộc khoá chính |
| question_id | UUID | Câu hỏi | FK → questions, thuộc khoá chính |
| order_index | integer | Thứ tự câu trong đề | NOT NULL |

**Bảng `quiz_attempts` — lượt làm bài**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh lượt làm bài | PK |
| user_id | UUID | Người làm bài | FK → users, **NOT NULL** (không có lượt làm bài ẩn danh) |
| quiz_id | UUID | Quiz được làm | FK → quizzes |
| mode | varchar | Chế độ: PRACTICE / EXAM | NOT NULL |
| status | varchar | Trạng thái: IN_PROGRESS / SUBMITTED / EXPIRED | NOT NULL |
| started_at | timestamptz | Thời điểm bắt đầu | NOT NULL |
| expires_at | timestamptz | Thời điểm hết hạn | NULL = không giới hạn thời gian |
| submitted_at | timestamptz | Thời điểm nộp | NULL khi chưa nộp |
| total_score | integer | Tổng điểm đạt được | |
| max_score | integer | Tổng điểm tối đa, **chốt lúc bắt đầu** | NOT NULL |
| created_at, updated_at | timestamptz | | NOT NULL |

> Chỉ mục một phần trên `(user_id, quiz_id)` với điều kiện `status = 'IN_PROGRESS'`: mỗi người tối đa một
> bài đang làm dở trên một quiz; gọi lại API bắt đầu là làm tiếp bài đó.

**Bảng `attempt_answers` — câu trả lời trong một lượt làm bài**

Mỗi dòng là một câu trong đề **của riêng lượt làm bài đó**, sinh sẵn ngay khi bắt đầu để chốt đề: chủ quiz
thêm hoặc bớt câu sau đó không ảnh hưởng bài đang làm hay đã nộp.

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh | PK |
| attempt_id | UUID | Lượt làm bài | FK → quiz_attempts; UNIQUE cùng question_id |
| question_id | UUID | Câu hỏi | FK → questions |
| order_index | integer | Thứ tự câu, sao lại lúc bắt đầu | NOT NULL |
| user_answer | jsonb | Câu trả lời: `{"optionIds":[…]}` hoặc `{"text":"…"}` | NULL = chưa trả lời |
| is_correct | boolean | Đúng hay sai | NULL khi chưa chấm hoặc đang chờ AI |
| score | integer | Điểm đạt được | |
| max_score | integer | Điểm tối đa, chốt lúc bắt đầu | NOT NULL |
| ai_feedback | text | Nhận xét của AI về bài làm | NULL |
| ai_suggestions | text | Gợi ý cải thiện, tách riêng để giao diện nhấn mạnh | NULL |
| graded_by | varchar | NOT_GRADED / AUTO / PENDING_AI / AI / AI_FAILED / HUMAN | NOT NULL |
| answered_at | timestamptz | Thời điểm trả lời | NULL |
| graded_at | timestamptz | Thời điểm chấm xong | NULL |

> `AI_FAILED` là **trạng thái dừng** khi gọi mô hình hỏng. Không có nó thì câu nằm mãi ở `PENDING_AI` và
> người học thấy "đang chấm" vĩnh viễn mà không ai biết đã hỏng.

**Bảng `learning_materials` — học liệu cho RAG**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh học liệu | PK |
| owner_id | UUID | Chủ sở hữu | FK → users; mọi truy vấn đều lọc theo cột này |
| title | varchar | Tiêu đề | NOT NULL |
| topic | varchar | Chủ đề | NULL |
| source_type | varchar | PDF / DOCX / TXT / TEXT (dán tay) | NOT NULL |
| status | varchar | PROCESSING / READY / FAILED | NOT NULL |
| char_count | integer | Số ký tự trích được | |
| chunk_count | integer | Số đoạn đã cắt | |
| error_message | text | Lý do xử lý thất bại, hiện thẳng lên giao diện | NULL |
| shared | boolean | Cho phép người học khác hỏi trợ lý trên tài liệu này | NOT NULL, **mặc định false** |
| created_at, updated_at | timestamptz | | NOT NULL |

> Cột `shared` mặc định *false* là quyết định có chủ ý: chia sẻ phải là hành động có ý thức của chủ tài
> liệu. Tài liệu nạp trước khi có chức năng này giữ nguyên trạng thái riêng tư.

**Bảng `material_chunks` — đoạn học liệu và vector nhúng**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh đoạn | PK |
| material_id | UUID | Học liệu chứa đoạn này | FK → learning_materials, xoá theo tầng |
| chunk_index | integer | Thứ tự đoạn trong tài liệu | UNIQUE cùng material_id |
| content | text | Nội dung đoạn | NOT NULL |
| embedding | vector(768) | Vector nhúng của đoạn | NULL khi chưa sinh xong |
| metadata | jsonb | Thông tin bổ sung | NULL |

> Số chiều 768 khớp mô hình embedding của Gemini. Tìm kiếm tương đồng dùng toán tử khoảng cách cosine
> `<=>`. Bảng này **không có chỉ mục xấp xỉ (ANN)**: truy vấn RAG phải lọc quyền đọc trước rồi mới xếp
> theo khoảng cách, trong khi chỉ mục xấp xỉ xếp hạng trước và lọc sau nên bỏ sót kết quả — chi tiết ở
> mục 3.4.

**Bảng `game_rooms` — phòng đấu**

Trạng thái đang chơi nằm ở Redis; bảng này chỉ lưu thông tin định danh và kết quả.

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh phòng | PK |
| room_code | varchar(8) | Mã PIN 6 ký tự, bỏ các ký tự dễ đọc nhầm (0/O, 1/I) | UNIQUE |
| host_id | UUID | Chủ phòng | FK → users |
| quiz_id | UUID | Quiz dùng cho ván đấu | FK → quizzes |
| status | varchar | WAITING / PLAYING / FINISHED | NOT NULL |
| seconds_per_question | integer | Thời gian mỗi câu | NULL = theo cấu hình câu hỏi, mặc định 20 giây |
| allow_guests | boolean | Cho khách vãng lai vào chơi | NOT NULL, **mặc định false** |
| started_at, finished_at | timestamptz | Thời điểm bắt đầu và kết thúc ván | NULL |
| created_at, updated_at | timestamptz | | NOT NULL |

**Bảng `game_room_players` — người chơi trong phòng**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Định danh | PK |
| room_id | UUID | Phòng | FK → game_rooms |
| user_id | UUID | Tài khoản người chơi | FK → users, **NULL khi là khách vãng lai** |
| display_name | varchar(50) | Biệt danh, chốt tại thời điểm chơi | NOT NULL; với khách đây là nguồn định danh duy nhất |
| avatar | varchar(40) | Mã ảnh đại diện (biểu tượng và màu, không phải tệp ảnh) | NULL |
| is_guest | boolean | Là khách vãng lai hay không | NOT NULL |
| final_score | integer | Điểm cuối ván; trong lúc chơi điểm nằm ở Redis | NULL khi ván chưa xong |
| joined_at | timestamptz | Thời điểm vào phòng | NOT NULL |

> Ràng buộc duy nhất trên `(room_id, user_id)` được thay bằng **chỉ mục một phần** với điều kiện
> `user_id IS NOT NULL`, vì nhiều khách trong cùng phòng đều có `user_id` NULL nên ràng buộc cũ không còn
> diễn đạt đúng ý.

**Bảng `chat_sessions` và `chat_messages` — hội thoại với trợ lý học tập**

| Bảng | Trường | Mô tả |
|---|---|---|
| chat_sessions | id (PK), user_id (FK → users), title, created_at, updated_at | Một phiên hội thoại; tiêu đề cắt từ câu hỏi đầu tiên |
| chat_messages | id (PK), session_id (FK → chat_sessions), role (USER / ASSISTANT), content, created_at | Từng lượt hỏi và trả lời trong phiên |

**Bảng `ai_jobs` — tác vụ AI chạy nền**

| Tên trường | Kiểu dữ liệu | Mô tả | Ghi chú |
|---|---|---|---|
| id | UUID | Mã công việc trả về cho client | PK |
| user_id | UUID | Người yêu cầu | FK → users |
| type | varchar | INGEST_MATERIAL / GENERATE_QUESTIONS | NOT NULL |
| status | varchar | PENDING / RUNNING / SUCCEEDED / FAILED | NOT NULL |
| request | jsonb | Tham số đầu vào | Để JSON nên thêm loại công việc mới không phải đổi lược đồ |
| result | jsonb | Kết quả | NULL khi chưa xong |
| error_message | text | Lý do thất bại | NULL |
| started_at, finished_at, created_at | timestamptz | | |

**Bảng `ai_request_logs` — nhật ký giám sát lời gọi AI**

| Tên trường | Kiểu dữ liệu | Mô tả |
|---|---|---|
| id | UUID | Định danh bản ghi (PK) |
| user_id | UUID | Người dùng phát sinh lời gọi (FK → users) |
| feature | varchar | Chức năng: embedding / generation / grading / chat |
| provider | varchar | Nhà cung cấp đã dùng: gemini / groq |
| model | varchar | Tên mô hình |
| tokens_in, tokens_out | integer | Số token vào và ra |
| latency_ms | integer | Độ trễ của lời gọi |
| status | varchar | Kết quả: SUCCESS / lỗi |
| error_message | text | Chi tiết lỗi |
| created_at | timestamptz | Thời điểm gọi |

> Bản ghi này được ghi trong **giao dịch riêng**, nên công việc chính thất bại và bị quay lui thì nhật ký
> vẫn còn. Đây là nguồn số liệu cho mục 3.6 (chi phí, độ trễ, tỉ lệ chuyển dự phòng).

> **Các bảng của chức năng mở rộng** — flashcard và lịch ôn tập, chống gian lận, gamification, lớp học và
> giao bài, bảng xếp hạng theo mùa, thông báo — đã được thiết kế trong tài liệu nhưng **chưa hiện thực**,
> nên không trình bày chi tiết ở đây; xem phần hướng phát triển ở Kết luận.

### 2.8.3. Mô hình dữ liệu đồ thị trên Neo4j

Đồ thị hành vi **không** phải bản sao của cơ sở dữ liệu quan hệ, mà là một bản chiếu phục vụ phân tích.
Nguồn sự thật vẫn là PostgreSQL; đồ thị lệch hoặc mất thì dựng lại được từ lịch sử làm bài.

**Các loại nút:**

| Nút | Thuộc tính | Ý nghĩa |
|---|---|---|
| `User` | `id` | Người học |
| `Quiz` | `id`, `title`, `visibility` | Bài thi |
| `Topic` | `name` | Chủ đề kiến thức |

**Các loại quan hệ:**

| Quan hệ | Thuộc tính | Ý nghĩa |
|---|---|---|
| `(User)-[:ATTEMPTED]->(Quiz)` | `score`, `maxScore`, `accuracy`, `at` | Người học đã làm bài thi này, kèm kết quả |
| `(User)-[:PRACTICED]->(Topic)` | `correct`, `total`, `accuracy` | Năng lực của người học trên một chủ đề |
| `(Quiz)-[:COVERS]->(Topic)` | `questionCount` | Bài thi bao gồm chủ đề nào |

`[HÌNH 2.10: Mô hình đồ thị Neo4j — ba loại nút User, Quiz, Topic và ba loại quan hệ ATTEMPTED, PRACTICED,
COVERS — cần vẽ và chèn vào]`

*Hình 2.10. Mô hình dữ liệu đồ thị phục vụ gợi ý cá nhân hoá*

**Nguyên tắc thiết kế đồ thị.** Mô hình trên đã được **lược bớt có chủ ý** so với bản thiết kế đầu:

| Quan hệ đã bỏ | Lý do |
|---|---|
| `WEAK_IN`, `STRONG_IN`, `INTERESTED_IN` | Chỉ là quan hệ `PRACTICED` nhìn qua một ngưỡng. Đưa ngưỡng vào **cạnh** thì mỗi lần đổi ngưỡng phải dựng lại toàn bộ đồ thị; để ngưỡng ở **truy vấn** thì đổi lúc nào cũng được. Cạnh giữ *sự thật đo được*, truy vấn giữ *cách diễn giải* |
| `SIMILAR_TO` | Tính được ngay trong truy vấn từ các quiz cùng làm. Lưu sẵn thì cần công việc cập nhật định kỳ, mà giá trị lỗi thời ngay sau mỗi bài nộp |
| `PREREQUISITE_OF` | **Không có nguồn dữ liệu.** Không ai khai báo chủ đề nào phải học trước chủ đề nào; tự sinh quan hệ này là hệ thống bịa ra kiến thức sư phạm mà nó không có |
| Nút `Question` | Chưa dùng tới trong truy vấn nào; chủ đề của quiz suy ra được từ `COVERS` |

**Cơ chế đồng bộ.** Sau mỗi lượt nộp bài, hệ thống phát sự kiện ở pha sau khi giao dịch được ghi nhận
(`AFTER_COMMIT`) để khởi động công việc nền đồng bộ sang đồ thị. Việc đồng bộ chạy **lần thứ hai** sau khi
AI chấm xong câu tự luận, vì lúc mới nộp những câu đó còn 0 điểm nên năng lực tính ra chưa đúng. Toàn bộ
thao tác dùng `MERGE` nên chạy lại bao nhiêu lần cũng cho cùng một kết quả — điều kiện bắt buộc, bởi bước
này cố ý chạy hai lần cho mỗi bài. Năng lực theo chủ đề được **tính lại từ đầu** trên toàn bộ lịch sử thay
vì cộng dồn, vì cộng dồn thì chạy hai lần là số liệu nhân đôi.

### 2.8.4. Dữ liệu lưu trên Redis

| Khoá / Kênh | Vai trò | Ghi chú |
|---|---|---|
| `room:{code}` | Trạng thái phòng đang chơi: người chơi, câu hiện tại, điểm | Có thời gian sống; thay đổi liên tục trong vài phút nên không ghi xuống cơ sở dữ liệu quan hệ mỗi lần |
| `room:{code}:events` | Kênh Pub/Sub phát sự kiện ván đấu tới các tiến trình máy chủ | Điều kiện để chạy nhiều tiến trình mà trạng thái phòng vẫn nhất quán |
| `roomguest:{key}` | Khoá phiên của khách vãng lai trong đúng một phòng | Sống 6 giờ; không phải JWT nên không mở được API nào khác |
| `session:{token}` | Refresh token của một phiên đăng nhập | Cho phép thu hồi phiên — điều JWT tự thân không làm được |
| `user-sessions:{userId}` | Chỉ mục ngược từ người dùng tới các phiên của họ | Cần cho chức năng đăng xuất khỏi mọi thiết bị |
| `ai:cache:{hash}` | Bộ đệm kết quả AI theo mã băm của prompt | Giảm chi phí gọi mô hình |
| `quota:ai:{userId}` | Bộ đếm hạn mức gọi AI theo người dùng | Giới hạn tần suất |
| `leaderboard:season:{seasonId}` | Bảng xếp hạng theo mùa | Dùng kiểu tập hợp có thứ tự |

## 2.9. Thiết kế giao diện

Giao diện tuân theo một bộ quy ước thống nhất nhằm tránh việc mỗi trang có một phong cách riêng: màu sắc,
bo góc và đổ bóng khai báo tập trung dưới dạng biến, không viết trực tiếp trong từng thành phần; nút hành
động chính dùng màu tối, màu tím chỉ dành cho liên kết; trang dành cho người học trình bày theo lưới thẻ,
trang quản lý trình bày theo bảng; và các thành phần dùng chung như tiêu đề trang, trạng thái danh sách
rỗng được tái sử dụng.

Một quy tắc quan trọng: **giao diện không hiển thị dữ liệu không có thật.** Các nền tảng thương mại thường
hiện điểm đánh giá và số lượt học; hệ thống này chưa có dữ liệu đó nên không bịa ra để giao diện trông
phong phú hơn.

Chín màn hình chính được thiết kế wireframe trước khi hiện thực:

`[HÌNH 2.11: Wireframe màn Đăng nhập / Đăng ký — biểu mẫu email và mật khẩu, nút đăng nhập bằng Google, liên kết quên mật khẩu — cần vẽ và chèn vào]`

*Hình 2.11. Wireframe màn hình đăng nhập và đăng ký*

`[HÌNH 2.12: Wireframe màn Khám phá quiz — thanh tìm kiếm, bộ lọc danh mục và độ khó, lưới thẻ quiz — cần vẽ và chèn vào]`

*Hình 2.12. Wireframe màn hình danh sách quiz*

`[HÌNH 2.13: Wireframe màn Làm bài — nội dung câu hỏi, danh sách phương án, đồng hồ đếm ngược, điều hướng câu, nút nộp bài — cần vẽ và chèn vào]`

*Hình 2.13. Wireframe màn hình làm bài*

`[HÌNH 2.14: Wireframe màn Kết quả — tổng điểm, danh sách câu kèm đáp án đúng và lời giải thích, nhận xét của AI cho câu tự luận — cần vẽ và chèn vào]`

*Hình 2.14. Wireframe màn hình kết quả làm bài*

`[HÌNH 2.15: Wireframe sảnh và phòng đấu — ô nhập mã PIN, danh sách người chơi đang chờ, mã QR; màn chơi với câu hỏi và bảng xếp hạng trực tiếp — cần vẽ và chèn vào]`

*Hình 2.15. Wireframe sảnh chờ và phòng đấu thời gian thực*

`[HÌNH 2.16: Wireframe màn Học liệu và Sinh đề AI — danh sách học liệu kèm trạng thái xử lý và công tắc chia sẻ; biểu mẫu sinh đề; danh sách câu hỏi nháp chờ duyệt kèm đoạn học liệu nguồn — cần vẽ và chèn vào]`

*Hình 2.16. Wireframe màn hình học liệu và sinh đề bằng AI*

`[HÌNH 2.17: Wireframe màn Trợ lý học tập — cột danh sách hội thoại, khung hội thoại, ô nhập câu hỏi, khối trích dẫn nguồn dưới mỗi câu trả lời — cần vẽ và chèn vào]`

*Hình 2.17. Wireframe màn hình trợ lý học tập*

`[HÌNH 2.18: Wireframe màn Gợi ý và Lộ trình học — danh sách quiz được gợi ý kèm lý do, sơ đồ thứ tự chủ đề nên ôn — cần vẽ và chèn vào]`

*Hình 2.18. Wireframe màn hình gợi ý và lộ trình học*

`[HÌNH 2.19: Wireframe trang quản trị — quản lý người dùng, quản lý nội dung, giám sát nhật ký và chi phí AI — cần vẽ và chèn vào]`

*Hình 2.19. Wireframe trang quản trị*

---

**Tóm kết chương 2.** Chương này đã xác định yêu cầu của hệ thống trên cơ sở khảo sát nhu cầu và phân tích
các nền tảng tương tự: 35 yêu cầu chức năng thuộc chín nhóm bắt buộc cùng các nhóm mở rộng, các yêu cầu
phi chức năng về hiệu năng thời gian thực, độ tin cậy và bảo mật. Bốn tác nhân được xác định cùng quy tắc
truy cập rõ ràng cho khách chưa đăng nhập. Các use case lõi đại diện cho bốn trụ cột của đề tài đã được
đặc tả chi tiết, kèm luồng thay thế cho những tình huống mà hệ thống phải xử lý đúng: chuyển nhà cung cấp
AI khi lỗi, chống điều khiển mô hình qua bài làm, xử lý mất kết nối trong phòng đấu, và trả lời "không
biết" khi không có học liệu liên quan. Cơ sở dữ liệu được thiết kế trên ba hệ quản trị với vai trò phân
định rõ. Trên cơ sở bản thiết kế này, chương 3 trình bày kết quả hiện thực, kiểm thử và các số liệu đánh
giá hiệu năng cùng độ chính xác của các chức năng AI.
