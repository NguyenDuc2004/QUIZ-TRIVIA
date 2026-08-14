# 10 — Quản trị (Admin)

**Ưu tiên:** [S] Should

## Mục tiêu
Cho phép quản trị viên quản lý người dùng, nội dung, cấu hình AI và giám sát hệ thống.

## Use case
- Admin quản lý user, đổi vai trò, kiểm duyệt nội dung.
- Admin cấu hình AI provider/fallback và theo dõi chi phí.

## Yêu cầu chức năng
- **FR-36** ✅ Quản lý người dùng: danh sách có lọc theo từ khoá / vai trò / trạng thái, khoá và mở khoá, đổi vai trò.
- **FR-37** ✅ Xem chi phí và độ tin cậy AI (token, nhà cung cấp đã dùng, độ trễ, tỉ lệ lỗi và tỉ lệ dùng dự phòng).
- **FR-38** ⏳ Kiểm duyệt quiz/câu hỏi công khai — chưa làm.
- **FR-39** ⏳ Cấu hình thứ tự nhà cung cấp AI và hạn mức ở runtime — chưa làm.

## Luồng xử lý
- Admin xem tổng hợp từ `ai_request_logs` để theo dõi chi phí và tần suất chuyển dự phòng.
- Khoá tài khoản → cập nhật `users.locked` **và thu hồi mọi phiên** của người đó.

---

## Khoá tài khoản, không xoá người dùng

Không có endpoint xoá người dùng, và đó là chủ ý. Bài đã làm, quiz đã soạn, học liệu đã nạp đều là dữ
liệu **người khác đang dùng hoặc đang được thống kê**: một quiz công khai có thể đang được nhiều người
làm, một lượt làm bài nằm trong bảng xếp hạng, một tài liệu đã chia sẻ đang là nguồn cho trợ lý trả lời.
Xoá tài khoản kéo theo xoá hoặc làm mồ côi những thứ đó.

Biện pháp tương ứng là **khoá**: chặn đường vào, giữ nguyên dữ liệu. `V12` thêm cột `users.locked` cùng
chỉ mục một phần trên đúng nhóm tài khoản bị khoá.

### Khoá phải có hiệu lực NGAY, không phải sau vài phút

Chỉ đặt cờ `locked = true` là chưa đủ:

| Nếu chỉ đặt cờ | Hệ quả |
|---|---|
| Access token đang cầm | Vẫn dùng được tới khi hết hạn — 15 phút |
| Refresh token đang cầm | Vẫn gia hạn được — tới 14 ngày |

Nghĩa là "khoá" chỉ thực sự có hiệu lực sau vài phút tới vài ngày, đúng lúc quản trị viên tin rằng nó có
hiệu lực ngay. Vì vậy khoá tài khoản **thu hồi toàn bộ phiên** (`revokeAll`), và trạng thái khoá được
kiểm ở **cả hai** lối vào:

- `login` — chặn đăng nhập mới.
- `refresh` — chặn gia hạn. Chặn một lối mà bỏ lối kia thì bất kỳ đường nào cấp lại token về sau cũng
  mở lại cửa cho một tài khoản đang bị khoá.

### Thông báo nói rõ lý do, nhưng chỉ sau khi khớp mật khẩu

Đăng nhập vào tài khoản bị khoá trả **403** kèm câu *"Tài khoản đã bị khoá. Vui lòng liên hệ quản trị
viên"*. Hai điều cân bằng ở đây:

- **Với người dùng thật**, giữ nguyên thông báo *"email hoặc mật khẩu không đúng"* sẽ khiến họ đi đặt
  lại mật khẩu hết lần này lần khác mà vẫn không vào được.
- **Với kẻ tấn công**, nói "bị khoá" cho một người chưa chứng minh được họ là chủ tài khoản chính là
  tiết lộ email đó tồn tại. Nên kiểm tra `locked` chạy **sau** khi đã khớp mật khẩu: lúc đó họ đã biết
  mật khẩu, câu này không cho thêm thông tin gì.

## Đổi vai trò cũng phải thu hồi phiên

Vai trò nằm **trong** access token (claim `role`), nên token cũ vẫn mang vai trò cũ tới khi hết hạn.
Không thu hồi phiên thì người vừa bị hạ quyền còn dùng quyền cũ thêm 15 phút.

## Hai việc quản trị viên không làm được

Chặn ở **tầng nghiệp vụ**, không tin vào việc giao diện ẩn nút:

- **Không tự khoá chính mình.**
- **Không tự hạ vai trò của chính mình.**

Hệ thống chỉ có một cấp quản trị, nên một lần bấm sai là mất quyền quản trị mà không còn ai mở lại được
— trừ khi sửa trực tiếp cơ sở dữ liệu. Giao diện cũng vô hiệu hoá hai thao tác đó với chính hàng của
người đang đăng nhập: để nút bấm được rồi báo lỗi là bắt người dùng học bằng cách thất bại.

## Giám sát AI: ba nhóm số liệu, ba câu hỏi khác nhau

| Nhóm | Trả lời câu hỏi |
|---|---|
| Chi phí | Tiêu bao nhiêu token, ở chức năng nào |
| Độ tin cậy | Tỉ lệ lời gọi thất bại, và **bao nhiêu lượt phải dùng nhà cung cấp dự phòng** — tỉ lệ dự phòng cao là dấu hiệu nhà cung cấp chính đang lỗi hoặc hạn mức bị đụng trần thường xuyên |
| Độ trễ | Trung bình **và P95** — trung bình một mình che mất những lần chậm bất thường, mà đúng những lần đó mới là thứ người dùng nhớ |

Truy vấn tổng hợp đi qua `JdbcTemplate` chứ không JPA: đây là truy vấn phân tích, kết quả là các con số
gộp theo nhóm chứ không map về một hàng bảng nào. Map thành entity rồi gộp ở Java sẽ tải toàn bộ nhật ký
lên bộ nhớ để đếm, mà số bản ghi này chỉ tăng theo thời gian.

Độ trễ trả `null` khi chưa có lời gọi nào để tính, **không** trả 0: 0 ms là một giá trị có nghĩa (nhanh
bất thường), còn "chưa gọi lần nào" là không có số. Gộp hai thứ thành 0 làm giao diện hiển thị một độ
trễ không tồn tại.

## API liên quan
[api.md](../api.md) mục 9 (`/admin/*`).

## Dữ liệu liên quan
`users`, `ai_request_logs` — [database.md](../database.md).

## Ghi chú kỹ thuật
- Chỉ vai trò ADMIN truy cập; bảo vệ bằng `@PreAuthorize("hasRole('ADMIN')")`.
- Không hiển thị API key trong UI/log.
