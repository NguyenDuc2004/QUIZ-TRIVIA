# 14 — Lớp học / Nhóm (Classroom & Assignment)

**Ưu tiên:** [S] Should · **Tận dụng:** quiz, attempt, analytics sẵn có

## Mục tiêu
Biến ứng dụng thành công cụ giáo dục thật: Creator (giáo viên) tạo lớp, mời học sinh, **giao bài (assignment)** có hạn nộp, và theo dõi tiến độ cả lớp.

## Use case
- Giáo viên tạo lớp, chia sẻ mã lớp (class code) để học sinh tham gia.
- Giáo viên giao một quiz làm bài tập, đặt hạn nộp.
- Học sinh làm bài tập được giao; giáo viên xem kết quả toàn lớp.

## Yêu cầu chức năng
- **FR-54** [S] ✅ CRUD lớp học; tham gia bằng **mã lớp 6 ký tự**; quản lý thành viên.
- **FR-55** [S] ✅ Giao **bài tập** (gắn quiz cho lớp) với thời gian mở và hạn nộp, cả hai đều tuỳ chọn.
- **FR-56** [S] ✅ Học sinh xem bài được giao & trạng thái (5 trạng thái, xem bảng dưới).
- **FR-57** [S] ✅ **Bảng theo dõi lớp:** ai đã nộp, điểm, số nộp muộn, điểm trung bình. *Câu sai nhiều* đã có ở [features/09](09-analytics.md) cho từng quiz — không làm lại bản theo lớp vì nó trả lời cùng một câu hỏi trên cùng một tập dữ liệu.
- **FR-58** [C] 🟡 Xuất bảng điểm — **CSV đã làm**, PDF không làm, lý do bên dưới.
- **FR-59** [C] ✅ Vai trò trong lớp: chủ nhiệm / trợ giảng.

## Năm trạng thái của một bài tập

Không lưu trong cơ sở dữ liệu — tính từ ba thứ đã có: hạn nộp, lượt làm bài, thời điểm hiện tại. Lưu thành cột
thì phải có ai đó cập nhật nó lúc quá hạn, tức thêm một job có thể chết, để giữ một giá trị suy ra được trong
một dòng.

| Trạng thái | Khi nào |
|---|---|
| `CHUA_LAM` | Chưa bắt đầu, còn hạn |
| `DANG_LAM` | Có lượt đang làm dở |
| `DA_NOP` | Nộp trước hạn |
| `NOP_TRE` | Nộp sau hạn |
| `QUA_HAN` | Quá hạn mà chưa nộp — **vẫn nộp được**, và khi đó chuyển thành `NOP_TRE` |

**Quá hạn vẫn nộp được, không khoá cứng.** Khoá cứng thì một em mất mạng mười phút là mất trắng bài, và giáo
viên không còn cách nào biết em ấy có làm hay không. Bài nộp muộn được đánh dấu rõ; trừ điểm hay không là
quyết định của giáo viên — cùng nguyên tắc với [features/12](12-anti-cheat.md): hệ thống đưa dữ kiện, người
thật quyết định.

**So thời điểm NỘP với hạn, không so thời điểm hiện tại.** Đây là cái bẫy dễ mắc nhất của phần này: so với
`now` thì bài nộp đúng hạn sẽ thành "nộp muộn" khi giáo viên mở bảng ra xem một tuần sau — và điểm bị trừ oan.
Có test riêng cho đúng tình huống đó.

## Năm quyết định của bản này

| Quyết định | Vì sao |
|---|---|
| **Mỗi học sinh MỘT lượt cho mỗi bài tập**, chốt bằng chỉ mục duy nhất | Đặc tả không nói, nhưng làm lại không giới hạn thì điểm bài tập mất hết ý nghĩa — ai kiên nhẫn hơn thì điểm cao hơn. Chốt ở CSDL vì kiểm trong Java thua cuộc khi học sinh mở hai tab |
| **Endpoint bắt đầu riêng** `POST /assignments/{id}/attempts` thay vì thêm tham số vào endpoint cũ | Quiz của giáo viên thường để PRIVATE, mà `AttemptService.start` chặn quiz PRIVATE của người khác — đúng, không nên nới. Phần cho phép nằm ở tầng lớp học: đã là thành viên của lớp được giao bài thì làm được quiz đó. Nhét kiến thức về lớp học vào tầng làm bài là buộc hai tính năng lẽ ra độc lập vào nhau |
| **Lượt bài tập luôn là `EXAM`** | Bài tập là bài tính điểm, nên nó cũng phải được [features/12](12-anti-cheat.md) thu tín hiệu hành vi. Cho chọn chế độ là mở đường làm bài tập ở chế độ luyện tập — nơi đáp án hiện ngay sau mỗi câu |
| **Mã lớp bỏ 0, O, 1, I, L** | Mã được **đọc to trong lớp và chép tay lên bảng**, không phải copy-paste. Giữ cả `0` và `O` thì một phần lớp gõ nhầm và vào sai chỗ. Bỏ năm ký tự làm không gian mã còn khoảng 887 triệu, vẫn thừa xa nhu cầu |
| **Mã lớp CHỈ trả cho giáo viên**, học sinh nhận `null` | Mã là thứ để **mời người vào**, không phải thông tin mọi thành viên cần cầm. Học sinh có mã thì lớp thành công khai với bất kỳ ai họ chuyển tiếp |

**Chủ nhiệm không nằm trong bảng `classroom_members`** — họ là `classrooms.owner_id`. Để chủ nhiệm thành một
dòng thành viên nữa thì có hai nguồn sự thật cho cùng một câu hỏi, và sớm muộn hai nguồn lệch nhau.

**Trợ giảng làm được gì:** giao bài, xem kết quả, xem thành viên. **Không** xoá lớp, **không** đổi vai trò
người khác — hai việc đó không hoàn tác được, và cho trợ giảng tự nâng người khác lên trợ giảng là mở một
đường để quyền lan ra mà chủ nhiệm không biết.

## Xuất bảng điểm: làm CSV, không làm PDF (FR-58)

Ranh giới không đổi so với lúc hoãn: **CSV rẻ, PDF thì không**. PDF cần thêm một thư viện vào stack cho đúng
một tính năng, và bảng điểm PDF sinh từ máy chủ còn phải nhúng font tiếng Việt — thiếu font thì chữ ra ô
vuông, một lỗi chỉ phát hiện khi ai đó mở file. Giáo viên cần bảng điểm để **tính toán tiếp** (nhập vào sổ,
cộng trung bình), việc đó hợp với bảng tính hơn hẳn với PDF.

`GET /api/v1/assignments/{id}/results.csv` — chỉ chủ nhiệm hoặc trợ giảng.

### Ba luật của file CSV, cả ba đều hỏng lặng lẽ

Không luật nào làm server trả lỗi: file vẫn 200, vẫn tải về, vẫn mở được, chỉ nội dung là sai — đúng loại
lỗi mà lý do hoãn PDF đã nêu, hoá ra CSV cũng có.

| Luật | Không làm thì sao |
|---|---|
| **BOM UTF-8 đầu tệp** | Excel trên Windows không tự đoán UTF-8 cho `.csv`: "Nguyễn Văn An" hiện thành "Nguyá»…n VÄƒn An" |
| **Thoát theo RFC 4180** | Một dấu phẩy trong tên người đẩy lệch cả hàng, và điểm bị gán sang cột khác |
| **Chặn tiêm công thức** | Tên hiển thị do người dùng tự đặt. Một cái tên bắt đầu bằng `=`, `+`, `-` hay `@` **chạy như công thức** khi giáo viên mở bằng Excel — nạn nhân là người không làm gì sai |

Chặn tiêm công thức bằng cách thêm dấu nháy đơn đứng trước. **Bọc ngoặc kép là không đủ** — Excel vẫn diễn
giải công thức nằm trong ngoặc kép; ngoặc kép là luật *định dạng*, không phải luật *an toàn*.

Người **chưa nộp** để **ô trống**, không phải 0 — cùng lý do với `diem = null` ở API: ghi 0 là nói sai về
họ, và mọi phép trung bình trên cột đó sai theo.

## Luồng xử lý
```
Giáo viên tạo lớp → class_code
Học sinh join bằng class_code → classroom_members
Giáo viên tạo assignment(quiz, open_at, due_at) cho lớp
Học sinh làm → quiz_attempt gắn assignment_id
Giáo viên xem tổng hợp từ attempts theo assignment (dùng analytics)
```

## API liên quan
```
GET    /api/v1/classrooms                          Lớp của tôi — cả lớp tôi dạy lẫn lớp tôi học   ✅
POST   /api/v1/classrooms                          Tạo lớp (CREATOR/ADMIN)                        ✅
GET    /api/v1/classrooms/{id}                     Chi tiết lớp                                   ✅
PUT    /api/v1/classrooms/{id}                     Sửa tên/mô tả (giáo viên)                      ✅
DELETE /api/v1/classrooms/{id}                     Xoá lớp (CHỈ chủ nhiệm)                        ✅
POST   /api/v1/classrooms/join/{code}              Tham gia bằng mã — mọi tài khoản đăng nhập     ✅
GET    /api/v1/classrooms/{id}/members             Thành viên (giáo viên)                         ✅
PUT    /api/v1/classrooms/{id}/members/{u}/role    Đổi vai trò (CHỈ chủ nhiệm)                    ✅
DELETE /api/v1/classrooms/{id}/members/{u}         Xoá thành viên (CHỈ chủ nhiệm)                 ✅
GET    /api/v1/classrooms/{id}/assignments         Bài đã giao cho lớp                            ✅
POST   /api/v1/classrooms/{id}/assignments         Giao bài (giáo viên, quiz của chính mình)      ✅
DELETE /api/v1/assignments/{id}                    Gỡ bài tập (giáo viên)                         ✅
GET    /api/v1/me/assignments                      Bài được giao cho tôi, kèm trạng thái của tôi  ✅
POST   /api/v1/assignments/{id}/attempts           Bắt đầu / làm tiếp bài tập                     ✅
GET    /api/v1/assignments/{id}/results            Bảng theo dõi lớp (giáo viên)                  ✅
GET    /api/v1/assignments/{id}/results.csv        Tải bảng điểm CSV (giáo viên)                  ✅
```
- **Tham gia lớp là `/classrooms/join/{code}`**, không phải `/classrooms/{code}/join` như bản nháp: đặt
  `{code}` ở vị trí đầu sẽ đụng khuôn `/classrooms/{id}`, và một mã lớp gõ sai biến thành một UUID không hợp
  lệ — người dùng nhận 400 khó hiểu thay vì "không tìm thấy lớp".
- Người ngoài lớp nhận **404** ở mọi endpoint của lớp đó — không tiết lộ lớp có tồn tại.
- **Tạo lớp** cần CREATOR/ADMIN, nhưng **tham gia** thì mọi tài khoản đã đăng nhập đều làm được: học sinh là
  người *học*, không phải người soạn nội dung, nên bắt họ có vai trò CREATOR chỉ để vào lớp là sai hẳn mô hình.

## Dữ liệu liên quan (bổ sung PostgreSQL) — `V19__classroom.sql`
- `classrooms(id, owner_id, name, description, class_code unique, created_at, updated_at)`
- `classroom_members(id, classroom_id, user_id, role: STUDENT/CO_TEACHER, joined_at)` — `UNIQUE (classroom_id, user_id)`
- `assignments(id, classroom_id, quiz_id, title, instruction, open_at, due_at, created_at, updated_at)`
- `quiz_attempts.assignment_id` — cột thêm, FK nullable

Hai ràng buộc đáng nói:
- `UNIQUE INDEX (assignment_id, user_id) WHERE assignment_id IS NOT NULL` — mỗi học sinh một lượt cho mỗi bài
  tập. Chốt ở CSDL vì kiểm trong Java thua cuộc khi học sinh mở hai tab.
- `assignments.quiz_id` dùng **`ON DELETE RESTRICT`**, không phải CASCADE: xoá một quiz đang được giao sẽ xoá
  luôn bài tập và mọi điểm gắn với nó. Chặn ở đây để giáo viên nhận lỗi rõ ràng thay vì mất dữ liệu trong im
  lặng.

`quiz_attempts.assignment_id` là **UUID thuần**, không phải liên kết `@ManyToOne` ở tầng Java: tầng làm bài
không nên biết gì về lớp học. Một cột id là đủ để tính năng 14 tra ngược, còn một liên kết entity kéo theo
quan hệ hai chiều giữa hai tính năng lẽ ra độc lập.

## Ghi chú kỹ thuật
- Kiểm quyền: chỉ owner/co_teacher xem kết quả lớp; học sinh chỉ thấy bài & kết quả của mình.
- Trạng thái nộp tính từ `quiz_attempts` + `due_at` (đúng hạn/quá hạn).
- Tái dùng [features/09-analytics.md](09-analytics.md) cho thống kê lớp.
- **Đã kết hợp thông báo** ([features/16](16-notifications.md)): job 7:05 mỗi ngày nhắc bài sắp hết hạn trong
  24 giờ tới. Đây là chỗ làm cho loại `ASSIGNMENT_DUE` — vốn khai sẵn trong ràng buộc `CHECK` của V18 nhưng
  chưa có nguồn phát — trở thành thật.
  - **Chỉ nhắc người CHƯA nộp.** Nhắc cả lớp thì em đã nộp từ tuần trước cũng nhận "bài sắp hết hạn" — một
    thông báo sai sự thật với chính người nhận, và là kiểu làm người ta tắt thông báo vĩnh viễn.
  - Khoá chống trùng là `assignment:{id}`, **không kèm ngày**: một bài tập chỉ có một hạn nộp, nên nhắc đúng
    một lần là đủ. Kèm ngày vào thì job chạy hôm sau lại nhắc tiếp cùng một bài.
