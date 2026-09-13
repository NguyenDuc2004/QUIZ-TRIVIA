# KẾT LUẬN

## 1. Những kết quả đạt được

Đồ án đã xây dựng hoàn chỉnh một ứng dụng web Quiz/Trivia tích hợp trí tuệ nhân tạo, hiện thực **16 nhóm chức năng** với 87 yêu cầu chức năng, chạy trên kiến trúc ba hệ quản trị dữ liệu và có **609 phép kiểm thử tự động đều đạt**. Bốn trọng tâm đặt ra trong phiếu giao đề tài đều có sản phẩm và số liệu đối chứng.

**Phòng đấu thời gian thực** đồng bộ qua Spring WebSocket với giao thức STOMP và phát tán sự kiện qua Redis Pub/Sub, điểm tính theo tốc độ trả lời. Kết quả đo ở mục 3.5: **100 người mỗi phòng với P95 là 216 ms, không mất một sự kiện nào** ở mọi mức tải đã thử tới 200 người.

**Sinh đề và trợ lý học tập** dùng chung một đường ống truy hồi trên kho vector pgvector, nên câu hỏi sinh ra và câu trả lời của trợ lý đều bám học liệu và nêu được nguồn. Kết quả đo ở mục 3.6: chấm tự luận **sai lệch trung bình 0,13 trên thang 10**, sinh đề **10/10 câu đúng chuẩn cấu trúc**, trợ lý **không suy đoán** với cả hai câu hỏi ngoài phạm vi học liệu.

**Gợi ý cá nhân hoá bằng Neo4j** đồng bộ hành vi làm bài sang đồ thị để trả lời ba truy vấn: chủ đề còn yếu, người học có kết quả tương tự, và thứ tự chủ đề nên ôn.

Ngoài bốn trọng tâm bắt buộc, đồ án hiện thực thêm **bảy nhóm chức năng mở rộng**: quản trị hệ thống, thẻ ghi nhớ theo thuật toán lặp lại ngắt quãng SM-2, chống gian lận khi thi, trò chơi hoá, lớp học và giao bài, bảng xếp hạng theo mùa, và thông báo nhắc ôn tập.

## 2. Hạn chế

Toàn bộ số liệu được đo trên **một máy đơn**, nên con số 216 ms ở mục 3.5 phản ánh chi phí xử lý của máy chủ và tầng phát tán, chưa bao gồm độ trễ mạng thật; đồ án cũng chưa đo kịch bản nhiều phòng đấu chạy song song. Với phần đánh giá AI, việc chấm được đối chiếu với đáp án theo tiêu chí trên cỡ mẫu nhỏ, đủ để phát hiện xu hướng nhưng chưa đủ cho kết luận thống kê, và đồ án không đánh giá chất lượng sư phạm của câu hỏi sinh ra — đây chính là lý do hệ thống buộc người tạo nội dung duyệt từng câu trước khi câu hỏi vào ngân hàng.

Một giới hạn cố hữu của sản phẩm: nội dung quiz do người dùng tạo nên hệ thống chỉ kiểm được cấu trúc, không kiểm được tính đúng đắn của câu hỏi người dùng tự soạn.

## 3. Hướng phát triển

Trước mắt, hai phép đo cần được mở rộng phạm vi: đo lại hiệu năng trên hạ tầng nhiều máy chủ có độ trễ mạng thật, và tăng cỡ mẫu đánh giá AI kèm nhiều người chấm độc lập để đối chiếu với chấm tay. Về chức năng, hướng đáng làm tiếp là ứng dụng di động cho phòng đấu — quét mã QR rồi chơi trên điện thoại vốn là kịch bản sử dụng chính — và sinh câu hỏi theo nhiều mức nhận thức thay vì chỉ theo độ khó. Xa hơn, đồ thị hành vi hiện mới phục vụ gợi ý, trong khi cùng tập dữ liệu đó trả lời được những câu hỏi có giá trị sư phạm hơn, chẳng hạn những chủ đề nào thường bị hiểu sai cùng nhau.

Đồ án đã hoàn thành các mục tiêu đặt ra trong phần Mở đầu. Quá trình thực hiện cho thấy phần khó nhất của một hệ thống tích hợp mô hình ngôn ngữ không nằm ở việc gọi được mô hình, mà nằm ở việc **dựng đủ hàng rào quanh nó** — giới hạn miền giá trị, kiểm chứng cấu trúc đầu ra, cách ly quyền đọc dữ liệu, và giữ quyền kết luận cuối cùng cho con người.
