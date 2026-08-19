# 10 — Quản trị (Admin)

**Ưu tiên:** [S] Should

## Mục tiêu
Cho phép quản trị viên nắm được tình trạng hệ thống, quản lý người dùng và nội dung công khai, giám sát
phòng đấu đang diễn ra, và kiểm soát chi phí AI.

## Use case
- Admin xem tổng quan sức khoẻ hệ thống ngay khi đăng nhập.
- Admin quản lý người dùng: đổi vai trò, khoá tài khoản, thu hồi phiên khẩn cấp.
- Admin quản lý danh mục và ẩn quiz công khai vi phạm.
- Admin giám sát phòng đấu đang chạy và cưỡng chế đóng phòng khi cần.
- Admin theo dõi chi phí AI và đặt hạn mức cho người tạo nội dung.

## Năm nhóm chức năng

| # | Nhóm | Đường dẫn | Trạng thái |
|---|---|---|---|
| 1 | Tổng quan hệ thống | `/admin` | ⏳ |
| 2 | Người dùng & phân quyền | `/admin/users` | ✅ |
| 3 | Nội dung & danh mục | `/admin/content` | ⏳ |
| 4 | Giám sát phòng đấu | `/admin/rooms` | ⏳ |
| 5 | Cấu hình & giám sát AI | `/admin/ai` | 🟡 giám sát xong, cấu hình chưa |

## Yêu cầu chức năng

| Mã | Yêu cầu | Trạng thái |
|---|---|---|
| FR-71 | Danh sách người dùng có lọc theo từ khoá / vai trò / trạng thái khoá | ✅ |
| FR-72 | Khoá và mở khoá tài khoản; khoá thì **thu hồi mọi phiên ngay** | ✅ |
| FR-73 | Đổi vai trò; **thu hồi phiên** vì vai trò nằm trong token | ✅ |
| FR-74 | Xem chi phí và độ tin cậy AI: token, nhà cung cấp, độ trễ, tỉ lệ lỗi và tỉ lệ dùng dự phòng | ✅ |
| FR-75 | Tổng quan hệ thống: KPI người dùng / quiz / lượt làm bài / phòng đang chạy / chi phí AI tháng này | ✅ |
| FR-76 | Biểu đồ tăng trưởng người dùng và lượt làm bài theo ngày | ✅ |
| FR-77 | Biểu đồ phân bổ quiz theo danh mục, và tỉ lệ hoàn thành bài làm | ✅ |
| FR-78 | Thu hồi phiên đăng nhập của một người dùng mà **không** khoá tài khoản họ | ✅ |
| FR-79 | Thêm / sửa / xoá danh mục quiz | ✅ |
| FR-80 | Xem danh sách quiz công khai và **ẩn** quiz vi phạm (đưa về riêng tư) | ✅ |
| FR-81 | Giám sát phòng đấu đang chạy: mã PIN, chủ phòng, quiz, số người, trạng thái | ✅ |
| FR-82 | Cưỡng chế đóng phòng đấu đang treo hoặc vi phạm | ✅ |
| FR-83 | Xem trạng thái cấu hình nhà cung cấp AI (**đã cấu hình / để trống**, không hiện giá trị khoá) | ✅ |
| FR-84 | Đặt hạn mức số lượt gọi AI mỗi ngày cho mỗi người tạo nội dung | ⏸ hoãn |

**FR-84 hoãn có lý do, xem như nợ kỹ thuật.** `AiOrchestrator` hiện không đếm lượt gọi theo từng người
dùng, nên một ô nhập hạn mức chỉ lưu được con số mà không chặn được gì — quản trị viên sẽ tin rằng chi phí
đã bị giới hạn trong khi thực tế không. Đó là kiểu sai tệ hơn việc thiếu tính năng. Làm đúng cần thêm bộ
đếm theo user ở Redis và điểm chặn trong `AiOrchestrator`; trong lúc chờ, FR-74 vẫn cho thấy chi phí thật
theo từng người để phát hiện lạm dụng.

## Ba việc cố ý KHÔNG làm

Đây là phần quan trọng của thiết kế, không phải phần bỏ sót.

| Không làm | Vì sao |
|---|---|
| **Hiển thị hoặc sửa khoá API trong giao diện** | `security.md` quy định không hiển thị khoá API trong UI hay log. Giao diện chỉ cho biết khoá **đã được cấu hình hay chưa** — đủ để chẩn đoán "vì sao AI không chạy" mà không bao giờ phơi giá trị. Sửa khoá là việc của biến môi trường và người có quyền truy cập máy chủ |
| **Sửa system prompt qua giao diện** | Prompt chính là nơi đặt bốn lớp chống tiêm chỉ thị khi chấm bài (features/06). Mở nó cho giao diện là mở đường phá hàng rào, và một lần sửa sai làm hỏng cả chức năng chấm mà không ai biết cho tới khi có người chấm sai điểm |
| **Admin tự đặt lại mật khẩu người dùng** | Admin biết mật khẩu của người dùng thì đăng nhập thay họ được, và mọi hành động sau đó không còn quy trách nhiệm được cho ai. Hệ thống đã có OTP tự phục vụ; nếu cần hỗ trợ thì gửi email đặt lại, chứ admin không tự đặt |

## Hai việc cần đổi nghiệp vụ nên chưa làm

| Chưa làm | Cần gì |
|---|---|
| **Luồng duyệt quiz** (chờ duyệt → phê duyệt / từ chối kèm lý do) | `quizzes` chỉ có `PUBLIC`/`PRIVATE`. Thêm luồng duyệt là **đổi cách Creator xuất bản**: hiện họ tự đặt công khai là xong, sau này phải chờ admin. Đó là thay đổi nghiệp vụ ảnh hưởng tính năng 02, không chỉ thêm một trang quản trị. Bản này làm mức nhỏ hơn giải quyết được vấn đề thực tế: admin **ẩn** quiz vi phạm (FR-80) |
| **Người dùng báo lỗi câu hỏi** (sai đáp án, câu tối nghĩa) | Chưa có bảng lưu báo cáo, và quan trọng hơn: **chưa có chức năng cho người dùng gửi báo cáo**. Làm trang admin xử lý báo cáo trước khi ai gửi được thì đó là một trang rỗng vĩnh viễn |

## Luồng xử lý
- Admin xem tổng hợp từ `ai_request_logs` để theo dõi chi phí và tần suất chuyển dự phòng.
- Khoá tài khoản → cập nhật `users.locked` **và thu hồi mọi phiên** của người đó.
- Ẩn quiz vi phạm → đổi `quizzes.visibility` về `PRIVATE`; quiz vẫn thuộc chủ của nó, không bị xoá.
- Cưỡng chế đóng phòng → xoá trạng thái phòng ở Redis và chuyển `game_rooms.status` sang `FINISHED`.

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

## Giao diện: khung riêng, không phải thêm mục vào menu chung

Khu quản trị dùng `AdminLayout` riêng thay vì `AppLayout` của khu học tập — xem
[ui-design-system.md §1](../ui-design-system.md) cho quy ước đầy đủ. Tóm lại ba lý do:

1. **Trông khác là một lớp an toàn** — thao tác ở đây tác động lên *người khác* và không có nút hoàn
   tác; nền tối cùng sidebar khiến quản trị viên luôn biết mình đang ở đâu.
2. **Ngữ cảnh làm việc khác** — menu *Khám phá / Phòng đấu / Trợ lý AI / Lộ trình* không liên quan gì
   khi đang khoá tài khoản.
3. **Sidebar mở rộng được** — thanh ngang khu học tập đã 10 mục với vai trò CREATOR.

| Thành phần | Thiết kế |
|---|---|
| Sidebar | Rộng 232px, nền `--color-ink`, thu gọn dưới breakpoint `lg`. Mục đang mở có viền trái trắng + nền mờ |
| Thương hiệu | "Quiz AI" + dòng phụ *"Khu quản trị"* — nói rõ đang ở khu nào ngay từ góc trên |
| Header | Trắng, gọn: tiêu đề khu + góc tài khoản có thẻ *Quản trị viên* màu `volcano` |
| Lối ra | **Không có.** Xem "Admin chỉ ở trong khu quản trị" bên dưới |
| Nội dung | Bảng dày thông tin như bộ mặt bảng điều khiển — dùng lại `PageHeader`, `EmptyState` |

### Admin chỉ ở trong khu quản trị

Tài khoản ADMIN **đăng nhập là vào thẳng `/admin` và ở lại đó**. Khu học tập chỉ nhận `LEARNER` và `CREATOR`;
Admin gõ tay `/quizzes` sẽ được đưa về `/admin`. Không còn mục *"Khu quản trị"* trong menu tài khoản của
`AppLayout` và không còn lối *"Về khu học tập"* trong sidebar — cả hai đều trở thành nhánh không ai chạy tới.

Lý do: khu học tập không có gì cho một tài khoản quản trị, và để một tài khoản có quyền tác động lên người
khác đi lang thang giữa dữ liệu của người khác là mở một cửa không cần thiết.

**Chặn ở tầng định tuyến frontend, không chặn ở API.** Đây là yêu cầu về *điều hướng*, không phải về bảo mật —
và siết ở tầng API sẽ làm vỡ chính khu quản trị: kiểm duyệt quiz, đọc câu hỏi, và báo cáo tính toàn vẹn đều
dựa trên việc ADMIN được `OwnershipGuard.canManage` coi như chủ sở hữu. Quyền backend giữ nguyên.

Hai việc Admin từng phải sang khu học tập mới làm được, nay có bản riêng trong khu này:

| Việc | Trước | Nay |
|---|---|---|
| Xem hồ sơ, đổi mật khẩu | `/profile` ở khu học tập | `/admin/profile` — **dùng lại đúng component** `ProfilePage`, không dựng bản thứ hai |
| Xem nội dung quiz để kiểm duyệt | dẫn sang `/quizzes/{id}` | `/admin/quizzes/{id}` — trang **chỉ đọc**, hiện đề, đáp án đúng và giải thích |

Trang xem quiz là chỗ dễ bị bỏ sót nhất khi chặn: bỏ hẳn liên kết thì Admin phải quyết định **ẩn nội dung của
người khác mà không nhìn được nội dung đó** — đúng kiểu quyết định mù mà [features/12](12-anti-cheat.md) cố
tránh. Trang này cố ý **không có nút hành động nào**; ẩn/hiện vẫn nằm ở trang danh sách, nơi có đủ ngữ cảnh.

**Một cái bẫy trong lúc làm:** đích chuyển hướng khi sai vai trò trước đây là hằng số `/quizzes`. Giữ nguyên nó
thì Admin bị đẩy về đúng chỗ vừa từ chối họ — **vòng lặp chuyển hướng, trang trắng, không có lỗi nào để lần
ra**, và không thấy được khi thử bằng tài khoản người học. Đích nay tính theo vai trò
(`trangChuTheoVaiTro`), và có test riêng giữ cho nó không bị "đơn giản hoá" lại thành hằng số.

Wireframe: `Hình 2.38` (Quản lý người dùng) và `Hình 2.39` (Giám sát AI) trong báo cáo.

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
