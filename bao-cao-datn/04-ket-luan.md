# KẾT LUẬN

## 1. Những kết quả đạt được

Đồ án đã xây dựng hoàn chỉnh một ứng dụng web Quiz/Trivia tích hợp trí tuệ nhân tạo, hiện thực **16 nhóm chức năng** với 87 yêu cầu chức năng, chạy trên kiến trúc ba hệ quản trị dữ liệu và có **645 phép kiểm thử tự động đều đạt**. Bốn trọng tâm đặt ra ở phần Mở đầu đều có sản phẩm và số liệu đối chứng.

**Trọng tâm thứ nhất — phòng đấu nhiều người chơi theo thời gian thực với độ trễ thấp.** Hệ thống đồng bộ trạng thái qua Spring WebSocket với giao thức STOMP, phát tán sự kiện qua Redis Pub/Sub, tính điểm theo tốc độ trả lời và cập nhật bảng xếp hạng trực tiếp sau mỗi câu. Người chơi vào phòng bằng mã PIN sáu số hoặc quét mã QR, và khách chưa có tài khoản cũng chơi được khi chủ phòng cho phép. Kết quả đo (mục 3.5): **100 người mỗi phòng với P95 là 216 ms và không mất một sự kiện nào** ở mọi mức tải đã thử tới 200 người. Kiến trúc chạy được trên nhiều tiến trình máy chủ với chi phí khoảng **2 ms** cho mỗi sự kiện đi vòng qua Redis.

**Trọng tâm thứ hai — sinh đề và trợ lý học tập bám học liệu qua RAG.** Đường ống nạp học liệu trích xuất văn bản từ tệp PDF, DOCX, TXT bằng Apache Tika, chia đoạn, sinh vector nhúng và lưu vào kho vector pgvector. Trên nền đó, hệ thống sinh câu hỏi trắc nghiệm theo chủ đề và độ khó, chấm cùng giải thích câu trả lời ngắn theo tiêu chí, và cung cấp trợ lý hỏi đáp trả lời theo luồng có trích dẫn nguồn. Kết quả đo (mục 3.6): chấm tự luận **sai lệch trung bình 0,13 trên thang 10**, sinh đề **10/10 câu đúng chuẩn cấu trúc**, và trợ lý **không suy đoán** với cả hai câu hỏi nằm ngoài học liệu.

**Trọng tâm thứ ba — gợi ý cá nhân hoá bằng Neo4j.** Hành vi làm bài được đồng bộ sang đồ thị gồm ba loại nút và ba loại quan hệ, phục vụ ba truy vấn: gợi ý quiz theo chủ đề còn yếu, gợi ý theo người học có kết quả tương tự, và đề xuất thứ tự chủ đề nên ôn. Mô hình đồ thị được **lược bớt có chủ ý** so với bản thiết kế ban đầu — cạnh chỉ giữ *sự thật đo được*, còn ngưỡng phân loại đặt ở truy vấn, nên đổi ngưỡng không phải dựng lại đồ thị.

**Trọng tâm thứ tư — kiểm thử hiệu năng và độ chính xác AI.** Hai mục 3.5 và 3.6 trình bày số liệu đo thật, kèm mô tả phương pháp và phần nêu rõ giới hạn của từng phép đo.

Ngoài bốn trọng tâm bắt buộc, đồ án hiện thực thêm **bảy nhóm chức năng mở rộng**: quản trị hệ thống, thẻ ghi nhớ kết hợp lặp lại ngắt quãng theo thuật toán SM-2, chống gian lận khi thi, trò chơi hoá, lớp học và giao bài, bảng xếp hạng theo mùa, và thông báo nhắc ôn tập.

### Ba quyết định kỹ thuật đáng ghi nhận

Ba quyết định dưới đây không phải lựa chọn hiển nhiên, và mỗi quyết định đều xuất phát từ một lỗi hoặc một mâu thuẫn quan sát được trong quá trình làm.

**Không lập chỉ mục xấp xỉ cho kho vector.** Truy vấn RAG của hệ thống phải lọc quyền đọc trước rồi mới xếp theo khoảng cách, trong khi chỉ mục xấp xỉ làm ngược lại. Hệ quả là trợ lý trả lời *"không có tài liệu"* trong khi kho có đoạn hợp lệ — một **lỗi im lặng**, không ngoại lệ, không mã lỗi. Điều đáng nói là toàn bộ số liệu đánh giá AI đo trước khi phát hiện lỗi này đều **không dùng được** và phải đo lại.

**Ranh giới giữa dữ kiện và kết luận trong chức năng chống gian lận.** Tín hiệu hành vi do trình duyệt người thi gửi lên, nên chặn được và giả mạo được. Hệ thống vì vậy **không tự trừ điểm, không tự huỷ bài của ai**: nó tính điểm rủi ro, liệt kê lý do cụ thể của từng cờ, và để người phụ trách kết luận. Giao diện nói thẳng giới hạn đó ngay cạnh con số chứ không giấu ở cuối trang.

**Phân định nguồn sự thật giữa ba hệ quản trị.** Redis giữ chỉ mục và trạng thái ngắn hạn, PostgreSQL giữ sự thật, Neo4j giữ bản chiếu phục vụ phân tích. Nguyên tắc này áp dụng nhất quán: bảng xếp hạng mùa đang chạy nằm ở Redis nhưng dựng lại được từ nhật ký điểm kinh nghiệm; đồ thị hành vi dựng lại được từ lịch sử làm bài. Nhờ vậy mất dữ liệu ở hai hệ sau không phải sự cố, chỉ là một lần dựng lại.

## 2. Hạn chế

Phần này nêu đúng những gì đồ án **chưa** làm được, kể cả khi điều đó làm giảm ấn tượng về kết quả — vì một báo cáo chỉ ghi phần đạt thì người đọc không có căn cứ để tin phần còn lại.

**Về phạm vi đánh giá.** Toàn bộ số liệu đo trên **một máy đơn, không có độ trễ mạng thật**. Con số 216 ms ở mục 3.5 là chi phí xử lý của máy chủ, không phải trải nghiệm của người dùng ở xa. Chưa đo nhiều phòng chạy song song, chưa đo mức tiêu thụ bộ nhớ và bộ xử lý, và chưa có người dùng thật ở quy mô lớn.

**Về đánh giá AI.** Cỡ mẫu nhỏ — 8 bài chấm, 10 câu sinh đề, 5 câu hỏi trợ lý — đủ để phát hiện lỗi hệ thống và xu hướng, không đủ cho kết luận thống kê. Việc chấm được đối chiếu với **đáp án theo tiêu chí**, chưa phải với nhiều giáo viên chấm độc lập, nên chưa thể kết luận *"AI chấm ngang giáo viên"*. Đồ án cũng **không đánh giá chất lượng sư phạm** của câu hỏi sinh ra — câu có đo đúng năng lực cần đo hay không, phương án nhiễu có hợp lý hay không — và đó chính là lý do hệ thống buộc người tạo nội dung duyệt từng câu.

**Về cơ chế dự phòng giữa các nhà cung cấp mô hình.** Nhà cung cấp dự phòng đã phục vụ thật qua ứng dụng và logic chuyển đã được kiểm bằng sáu phép kiểm tự động, nhưng **chưa quan sát được một lần chuyển thật do lỗi tạm thời** của nhà cung cấp chính. Ép một lỗi như vậy đòi hỏi chặn mạng ở mức hệ điều hành, nằm ngoài phạm vi phép đo.

**Về một hạn chế chức năng đã phát hiện.** Trợ lý học tập **vẫn hiển thị danh sách nguồn ngay cả khi trả lời rằng không có thông tin**, do danh sách nguồn được gửi trước khi mô hình kịp trả lời. Hai hướng xử lý đều cần đo thêm trước khi chọn, nên hạn chế này được ghi vào nợ kỹ thuật thay vì sửa bằng một con số ngưỡng đoán.

**Về sáu yêu cầu cố ý không hiện thực đầy đủ** (chi tiết ở mục 2.2.3). Không có yêu cầu mức bắt buộc nào trong số này:

| Yêu cầu | Lý do |
|---------|-------|
| Phát hiện đáp án trùng bất thường trong phòng đấu | Trong phòng đấu mọi người nhận cùng câu hỏi cùng lúc, câu trắc nghiệm thường chỉ có bốn phương án — người cùng chọn một đáp án đúng là điều **phải xảy ra**. Một phép đo mà kết quả dương tính chủ yếu rơi vào người trả lời đúng thì không dùng được để buộc tội ai |
| Xuất báo cáo PDF | Ngoài phạm vi; chỉ là một định dạng xuất khác của số liệu đã hiển thị đầy đủ |
| Xuất/nhập quiz — chỉ làm JSON | Quiz là dữ liệu lồng nhau, biểu diễn bằng định dạng phẳng phải bịa quy ước riêng |
| Xuất bảng điểm lớp — chỉ làm CSV | Ngược lại: bảng điểm vốn phẳng, và giáo viên cần **tính toán** trên nó nhiều hơn là in ra |
| Bảng xếp hạng mùa — chỉ phạm vi toàn hệ thống | Phạm vi theo lớp trùng với bảng theo dõi lớp đã có; phạm vi theo bạn bè thì hệ thống **không có quan hệ bạn bè** để dựa vào |
| Cài đặt thông báo — bỏ khung giờ im lặng | Chỉ có một nguồn thông báo chạy theo lịch cố định buổi sáng, đã nằm ngoài khung giờ nghỉ |

**Về một giới hạn cố hữu của sản phẩm.** Nội dung quiz do người dùng tạo, nên hệ thống không thể kiểm chứng tính đúng đắn của câu hỏi người dùng tự soạn; nó chỉ kiểm được cấu trúc. Cùng lý do, giao diện đa ngôn ngữ nếu làm cũng chỉ dịch được phần khung, không dịch được nội dung học tập.

## 3. Bài học rút ra

**Một bộ kiểm thử toàn đạt chứng minh những gì đã nghĩ tới là đúng, không chứng minh đã nghĩ đủ.** Ba lỗi thật của sản phẩm lộ ra khi mở ra sử dụng chứ không qua kiểm thử (mục 3.4.4), trong đó một lỗi có **hẳn một phép kiểm khẳng định đúng cái hành vi sai đó** — phép kiểm viết đúng với đặc tả tại thời điểm viết, và đặc tả mới là thứ hết hạn khi hệ thống lớn lên. Vì vậy bước *chạy thật và xác nhận trên trình duyệt* được đặt thành một bước bắt buộc, ngang hàng với bước chạy kiểm thử.

**Một con số sai còn nguy hiểm hơn không có số nào.** Lần đo độ chính xác AI đầu tiên cho kết quả rất xấu, và nếu tin nó thì báo cáo đã kết luận *"AI chấm sai hoàn toàn"* — trong khi thực tế mô hình **chưa từng được gọi** vì mọi lời gọi đều vượt hạn mức gói miễn phí. Bài học: trước khi diễn giải một con số, phải kiểm chứng rằng phép đo đã thật sự chạm tới thứ định đo.

**Luật an toàn bị nhân đôi là một lỗ hổng đang chờ.** Quy tắc *"chỉ nhận ảnh của hệ thống này"* từng được viết riêng ở hai chỗ; khi thêm đường tải ảnh thứ ba, chỗ mới bỏ qua phép kiểm dung lượng vì nó nằm trong thân hàm của đường cũ. Người sửa và người quên là cùng một người, cách nhau một ngày.

## 4. Hướng phát triển

**Ngắn hạn — xử lý những gì đã ghi vào nợ.** Giải quyết hạn chế hiển thị nguồn dư của trợ lý học tập bằng cách đo phân bố khoảng cách thực tế rồi mới chọn ngưỡng. Bổ sung công việc dọn tệp ảnh mồ côi trên đĩa. Hoàn thiện bộ kịch bản chụp màn hình tự động để ảnh trong báo cáo dựng lại được như cách 44 sơ đồ đang được dựng.

**Trung hạn — mở rộng phạm vi đánh giá.** Đo trên hạ tầng nhiều máy chủ có độ trễ mạng thật để có con số phản ánh trải nghiệm người dùng. Tăng cỡ mẫu đánh giá AI và mời nhiều người chấm độc lập để đối chiếu với chấm tay. Đo hiệu năng khi nhiều phòng đấu chạy song song — kịch bản gần với một buổi học thật hơn là một phòng đơn lẻ.

**Dài hạn — mở rộng chức năng.** Ứng dụng di động cho phòng đấu, vì quét mã QR rồi chơi trên điện thoại vốn là kịch bản sử dụng chính. Giao diện đa ngôn ngữ cho phần khung. Sinh flashcard và câu hỏi theo nhiều mức nhận thức thay vì chỉ theo độ khó. Và mở rộng phân tích trên đồ thị hành vi: hiện đồ thị mới dùng cho gợi ý, trong khi cùng dữ liệu đó có thể trả lời được những câu hỏi có giá trị sư phạm hơn — chẳng hạn chủ đề nào thường bị hiểu sai cùng nhau.

---

Đồ án đã hoàn thành các mục tiêu đặt ra trong phần Mở đầu: xây dựng được một hệ thống Quiz/Trivia hoàn chỉnh với phòng đấu thời gian thực, tích hợp trí tuệ nhân tạo tạo sinh qua kiến trúc RAG cho ba chức năng khác nhau, gợi ý cá nhân hoá trên cơ sở dữ liệu đồ thị, và đánh giá được cả hiệu năng thời gian thực lẫn độ chính xác AI bằng số liệu đo thật. Quá trình thực hiện cũng cho thấy phần khó nhất của một hệ thống tích hợp mô hình ngôn ngữ không nằm ở việc gọi được mô hình, mà nằm ở việc **dựng đủ hàng rào quanh nó** — giới hạn miền giá trị, kiểm chứng cấu trúc đầu ra, cách ly quyền đọc dữ liệu, và giữ quyền kết luận cuối cùng cho con người.

Em xin chân thành cảm ơn thầy ThS. Nguyễn Đức Lưu đã hướng dẫn tận tình trong suốt quá trình thực hiện đồ án.
