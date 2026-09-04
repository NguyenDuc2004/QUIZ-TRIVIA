# Chuẩn giao diện (UI Design System)

> **Đây là nguồn sự thật về giao diện.** Mọi trang/component mới phải tuân theo tài liệu này.
> Phong cách lấy cảm hứng từ Udemy: vuông vắn, viền mảnh, chữ nhỏ mà đậm, nút hành động chính màu đen, tím dùng để nhấn.
>
> Chỉ mô phỏng **phong cách**; không dùng logo, tên thương hiệu, hình ảnh hay nội dung của Udemy. Font Udemy Sans có bản quyền → dùng **Inter** (tự host qua `@fontsource-variable/inter`).

## 1. Ba bộ mặt — dùng đúng chỗ

Udemy có hai kiểu bố cục rất khác nhau. Chọn sai kiểu là lỗi giao diện, không phải chuyện thẩm mỹ. Dự án
này có thêm bộ mặt thứ ba vì nó có một khu vực mà **đối tượng bị tác động không phải người đang dùng**.

| Kiểu | Khi nào dùng | Trang của dự án | Khung |
|---|---|---|---|
| **Học viên** (browse) | Người dùng đi *tìm* nội dung | Khám phá quiz, giới thiệu quiz, làm bài, phòng đấu, gợi ý | `AppLayout` |
| **Bảng điều khiển** (dashboard) | Người dùng đi *quản lý* nội dung **của mình** | Quiz của tôi, Ngân hàng câu hỏi, Soạn quiz, Học liệu, Thống kê | `AppLayout` |
| **Khu quản trị** (admin) | Quản trị viên tác động lên **người khác** hoặc lên cả hệ thống | Quản lý người dùng, Giám sát AI | `AdminLayout` |

- **Browse** → lưới card, ảnh/khối màu 16:9, tiêu đề đậm cắt 2 dòng, nhiều khoảng trắng.
- **Dashboard** → bảng dày thông tin, nút viền mảnh, hầu như không màu rực, không card lồng card.
- **Admin** → cũng là bảng dày thông tin như dashboard, nhưng **khung khác hẳn**: sidebar dọc nền tối
  thay cho thanh điều hướng ngang.

### Vì sao khu quản trị có khung riêng, không phải thêm mục vào menu chung

Ba lý do, xếp theo mức quan trọng:

1. **Trông khác là một lớp an toàn.** Thao tác ở khu này tác động lên *người khác* — khoá tài khoản, đổi
   vai trò — và không có nút hoàn tác. Nền tối cùng bố cục sidebar khiến quản trị viên luôn biết mình
   đang ở đâu, thay vì tưởng vẫn ở trang cá nhân rồi bấm nhầm. Đây là lý do đầu tiên, không phải thẩm mỹ.
2. **Ngữ cảnh làm việc khác.** Menu *Khám phá / Phòng đấu / Trợ lý AI / Lộ trình / Tiến độ* không liên
   quan gì khi đang xem chi phí AI. Để lẫn vào nhau thì mỗi lần dùng, người ta phải tự lọc ra mục cần.
3. **Sidebar mở rộng được.** Thanh ngang của khu học tập đã chạm giới hạn một lần (11 mục với vai trò
   CREATOR, tràn hàng trên màn hình hẹp) và phải gom lại thành menu nhóm; sidebar dọc còn chỗ cho kiểm
   duyệt nội dung và cấu hình AI.

**Chuyển ngữ cảnh phải đi được cả hai chiều.** Lối vào khu quản trị nằm trong menu tài khoản của
`AppLayout`; lối ra là mục *"Về khu học tập"* đặt ở **đáy** sidebar — không bắt ai đi tìm trong menu
tài khoản, và không để ai mắc kẹt một bên.

### Quy tắc thanh điều hướng khu học tập

Thanh ngang giữ **tối đa 5 mục**. Từng có 11 mục phẳng và nó tràn hàng trên màn hình hẹp, nên gom lại:

| Mục | Nội dung |
|---|---|
| **Khám phá** · **Phòng đấu** · **Trợ lý AI** | Link đơn — ba việc dùng thường xuyên nhất, không chôn vào menu |
| **Học tập** ▾ | Thẻ ghi nhớ · Lộ trình học · Tiến độ · Lịch sử làm bài |
| **Thư viện** ▾ | Quiz của tôi · Ngân hàng câu hỏi · Học liệu — **chỉ CREATOR/ADMIN** |

| Quy tắc | Vì sao |
|---|---|
| **Hành động không nằm trong menu điều hướng** | *Sinh đề AI* là việc người dùng **làm**, không phải nơi họ **đi tới**. Là link giữa những link khác thì nó chìm hoàn toàn. Chuyển thành `<Button type="primary">` đặt cạnh avatar |
| Nút hành động đó **màu đen, không gradient** | §4 cấm gradient (trừ khối ảnh giả lập) và §5 quy định hành động chính màu đen. Làm một nút nổi bằng cách phá quy ước màu thì phần còn lại của giao diện trả giá; icon ✨ đủ để phân biệt nó với các nút đen khác |
| **Nhãn nhóm phải tự sáng khi một trang con đang mở** | Menu xổ xuống *giấu mất* dấu hiệu "đang ở trang nào" — mở trang Thẻ ghi nhớ mà cả thanh menu không có gì sáng thì người dùng mất phương hướng. Mục con cũng được đánh dấu `selectedKeys` trong menu |
| **Mỗi mục trong menu có một dòng mô tả** | Gom vào menu làm mất khả năng đọc hết mọi mục bằng một cái nhìn; dòng mô tả bù lại phần đó cho người chưa quen |
| **Ẩn theo vai trò, không chỉ chặn khi bấm** | LEARNER không thấy nhóm *Thư viện* và không thấy nút *Sinh đề AI*. Hiện rồi báo 403 là bắt người dùng học bằng cách thất bại |

### Quy tắc của sidebar quản trị

| Quy tắc | Vì sao |
|---|---|
| **Gom nhóm có nhãn nhỏ** (*Người dùng & nội dung* · *Giám sát*) | Sáu mục phẳng đọc thành một dãy đều nhau; nhóm theo đối tượng bị tác động giúp tìm bằng mắt. Nhóm đầu (*Tổng quan*) **không có nhãn** — nhãn trùng tên mục thì chiếm dòng mà không thêm thông tin |
| **Thu gọn phải có nút bấm** | Chỉ đặt `breakpoint` + `collapsedWidth={0}` mà không bật `collapsible` thì trên màn hình hẹp sidebar biến mất và **không còn cách nào mở lại** — admin mất hết đường điều hướng. Thu gọn về dải 72px chỉ còn icon, kèm tooltip, và nút gập nằm ở header |
| **Mục đang chọn: nền sáng nhẹ + viền tím mảnh + icon tím + chữ đậm** | Tím dành cho điều hướng theo quy ước màu ở §2, nút hành động vẫn màu đen. Đổi cả nền mục sang màu đặc làm nó trông như một nút bấm. Viền vẽ bằng `ring` **chứ không phải `border`**: `border` cộng 1px vào hộp, làm mục đang chọn cao hơn các mục khác 2px và cả danh sách nhấp lên xuống mỗi lần đổi trang |
| **Chữ trắng đều cho mọi mục**, không làm mờ mục chưa chọn | Mục đang chọn đã có bốn dấu hiệu khác (nền, viền, icon tím, chữ đậm) nên không cần dựa vào độ sáng chữ. Riêng **nhãn nhóm** vẫn mờ hơn — nó là chú thích, không phải thứ bấm được, để trắng đều thì nó cạnh tranh với chính các mục nó giới thiệu |
| **Nền đen tuyền lấy từ token riêng** (`--color-admin-bg` = `#000000`) | Tách rõ nhất khỏi vùng nội dung sáng bên cạnh. Dùng `--color-ink` thì hai khối cạnh nhau trông như cùng một mặt phẳng. Vẫn là token, **không** hardcode mã màu trong component |

## 2. Token màu

| Token | Mã | Dùng cho |
|---|---|---|
| `--color-ink` | `#1c1d1f` | Chữ chính **và nền nút hành động chính** |
| `--color-ink-soft` | `#6a6f73` | Chữ phụ, mô tả, meta |
| `--color-brand` | `#a435f0` | Nhấn mạnh, trạng thái active, viền focus |
| `--color-brand-strong` | `#5624d0` | Link chữ (tím đậm cho dễ đọc) |
| `--color-line` | `#d1d7dc` | Mọi đường viền |
| `--color-surface-subtle` | `#f7f9fa` | Nền khối phụ, header bảng |
| `--color-star` / `--color-rating` | `#e59819` / `#b4690e` | Sao và số điểm (chỉ khi có dữ liệu thật) |
| `--color-badge` | `#eceb98` | Nhãn nổi bật kiểu "Bestseller" |
| `--color-admin-bg` | `#000000` | Nền sidebar khu quản trị (`AdminLayout`) |
| `--color-admin-line` | `#2b2f37` | Đường viền trên nền tối của khu quản trị |
| `--color-footer-bg` | `#2b2d42` | Nền chân trang (xanh than) |
| `--color-footer-line` | `#3d4059` | Đường kẻ trong chân trang |
| `--color-footer-text` | `#c7c9d9` | Chữ phụ và link trong chân trang |

**Chân trang dùng nền tối RIÊNG, không dùng lại nền đen của khu quản trị.** Đen tuyền ở khu quản trị là
một **tín hiệu cảnh báo** (*"thao tác ở đây tác động lên người khác và không hoàn tác được"*); chân trang
chỉ là điểm dừng của trang. Dùng lại đúng màu đó ở mọi trang sẽ làm tín hiệu kia loãng đi.

Chữ trên nền chân trang **không dùng được** `--color-ink` (gần đen) hay `--color-ink-soft` (xám tối) — hai
màu đó sinh ra cho nền sáng và sẽ chìm hẳn. Tiêu đề cột dùng trắng, chữ phụ và link dùng
`--color-footer-text`, hover ra trắng. **Link không hover ra `--color-brand-strong`**: tím đậm đó dành cho
nền sáng, đặt lên xanh than thì gần như không đọc được.

**Quy tắc:** không hardcode mã màu trong file component. Chỉ dùng token Ant Design (`ConfigProvider`) hoặc class Tailwind sinh từ `@theme` (`bg-ink`, `text-ink-soft`, `border-line`…).

### Ghi đè CSS của Ant Design: bắt buộc thêm hậu tố `!`

Bẫy này đã cắn dự án **hai lần**, ở hai chỗ trông chẳng liên quan gì nhau — nên nó là luật chung, không
phải mẹo riêng cho màu chữ:

| Lần | Triệu chứng | Luật của antd đè lên |
|---|---|---|
| 1 | Cả 10 mục điều hướng ra màu tím, mất dấu hiệu "đang ở trang nào" | `a { color }` |
| 2 | **Chân trang trồi lên giữa màn hình** ở trang nội dung ngắn, dưới là mảng trắng | `.ant-layout { min-height: 0 }` đè `min-h-screen` |

Lần thứ hai khó thấy hơn hẳn: nhìn vào thì tưởng phần nội dung không giãn, nhưng antd **đã** đặt
`.ant-layout-content { flex: auto }` rồi — thứ bị mất là **chiều cao tối thiểu của khung ngoài**. Sửa
nhầm chỗ (thêm `flex-1` cho Content) sẽ không có tác dụng gì mà vẫn trông như đã xử lý.

**Quy tắc: mọi utility Tailwind ghi đè lên một thuộc tính mà antd có đặt đều phải có `!`.**

### Đặt màu chữ cho thẻ `<a>`: bắt buộc thêm hậu tố `!`

Ant Design chèn CSS lúc chạy, **ngoài** cascade layer; utility của Tailwind v4 nằm **trong** `@layer`. Theo
luật cascade, luật ngoài layer **thắng** luật trong layer — nên `text-ink` đặt trên một `<a>` bị `a { color }`
của antd đè, dù class có specificity cao hơn.

```tsx
// SAI — cả mục đang mở lẫn mục chưa mở đều ra màu link của antd, mất dấu hiệu "đang ở trang nào"
isActive ? 'text-brand-strong' : 'text-ink'
// ĐÚNG
isActive ? 'text-brand-strong!' : 'text-ink!'
```

Icon bên trong `<a>` cũng phải đặt màu **tường minh**: nó chỉ thừa hưởng màu từ thẻ `a`, nên khi thẻ `a` bị
đè thì icon đổi màu theo mà không có class nào của mình bị sai.

**Cách kiểm:** không tin vào class đã viết — đọc màu đã render (`getComputedStyle(el).color`). Lỗi này từng
làm toàn bộ 10 mục trên thanh điều hướng khu học tập ra màu tím trong khi code ghi `text-ink`.

## 3. Kiểu chữ

| Vai trò | Cỡ / Đậm |
|---|---|
| Tiêu đề trang (h1) | 32px / 700 |
| Tiêu đề mục (h2) | 24px / 700 |
| Tiêu đề card (h3) | 16px / 700, cắt 2 dòng |
| Thân bài | 14px / 400 |
| Meta, chú thích | 12px / 400, màu `ink-soft` |
| Chữ trên nút | 14px / **700** |

Font: `Inter`, dự phòng `system-ui, sans-serif`.

## 4. Hình khối

- **Bán kính bo góc: 4px** cho mọi thứ (nút, input, card, tag). Ant Design mặc định 6–8px → đã hạ trong theme.
- **Viền 1px `--color-line`** thay cho đổ bóng.
- **Hover thẻ bấm được:** viền đậm lại thành `--color-ink-soft` **và** bóng nhẹ `0 2px 8px rgba(0,0,0,.12)`,
  chuyển trong 0,15s. Dùng lớp `.browse-card`, đừng tự viết lại ở từng chỗ.
  - Bản đầu chỉ có bóng `0 2px 4px rgba(0,0,0,.08)` — **gần như không nhìn thấy** trên nền trắng cạnh viền
    sáng, tới mức người dùng báo "thẻ quiz không có hiệu ứng hover" trong khi CSS thì có.
  - Đổi viền chứ không chỉ tăng bóng: **viền 1px là ngôn ngữ hình khối chính của dự án**, nên nói bằng viền
    thì rõ hơn và hợp hệ thống hơn là đổ một cái bóng dày.
- Chiều cao control: 40px (thường), 48px (nút CTA lớn).
- **Gradient: chỉ ở KHOẢNH KHẮC, không ở KHUNG CHỨC NĂNG.** Xem mục 4.1.

### 4.1. Gradient dùng ở đâu, và vì sao không dùng rộng hơn

Bản đầu của tài liệu này cấm gradient trừ khối ảnh giả lập. Cấm sạch thì an toàn nhưng làm mất một thứ:
tài liệu chia sẵn **ba bộ mặt** (mục 1) mà trên thực tế cả ba trông như nhau — phòng đấu tính điểm theo
tốc độ, huy hiệu, phân hạng Vàng/Bạc/Đồng đều được vẽ bằng đúng bộ quy tắc của một bảng quản trị.

Ranh giới không phải *"bao nhiêu"* mà là *"ở đâu"*:

| Dùng gradient | Không dùng |
|---|---|
| Khối bìa quiz chưa có ảnh (`boMatCua`) | Nút bấm |
| Khối mở đầu trang Khám phá (`.browse-hero`) | Bảng, biểu mẫu, ô nhập |
| Bốn phương án trong phòng đấu (`.room-option-*`) | Thanh điều hướng |
| Khối công bố điểm ở màn kết quả (`.result-hero-*`) | **Màn đang làm bài** |
| Thẻ cấp độ, huy hiệu đã mở (`.achievement-hero`, `.badge-earned`) | **Toàn bộ khu quản trị** |
| Huy chương top 3 (`.podium-*`) | |

Ba lý do cụ thể cho cột phải:

1. **Chữ trên nền chuyển màu có độ tương phản không đoán được** — chỗ đọc được, chỗ không.
2. **Nút gradient lặp trên mọi trang làm giao diện trông như một mẫu tải về.** Nút chính màu đen chính là
   thứ đang giữ cho 16 nhóm chức năng trông như *một* sản phẩm chứ không phải 16 trang rời.
3. **Màn làm bài và khu quản trị có lý do riêng.** Màn làm bài cần sự tập trung — đó là lý do nó ẩn cả
   chân trang (mục 1). Nền đen của khu quản trị là một *tín hiệu cảnh báo*; tô màu vào đó là làm loãng
   tín hiệu.

Đối chiếu với sản phẩm cùng loại thì ranh giới này quen thuộc: Kahoot rực rỡ ở **màn chơi**, còn bảng
quản lý câu hỏi của họ vẫn trắng-xám bình thường.

**Màu phải mang thông tin, không chỉ trang trí.** Màu khối bìa buộc vào **danh mục** chứ không vào tiêu
đề — bản đầu chọn bằng `title.charCodeAt(0)`, nên hai quiz cùng "Toán học" ra hai màu khác nhau và mắt
người dùng học một quy luật *không tồn tại*. Cùng nguyên tắc: màu phương án trong phòng đấu buộc vào **vị
trí** (ổn định suốt một câu), và màu khối kết quả buộc vào **mức điểm**.

**Chuyển động phải tôn trọng `prefers-reduced-motion`.** Nhịp đập của ngọn lửa chuỗi ngày học tắt hẳn với
người đặt hệ điều hành ở chế độ giảm chuyển động — một biểu tượng nhấp nháy liên tục là thứ gây khó chịu
thật, và đó là trang người học mở thường xuyên.

## 4.2. Màn hình hẹp

**Điện thoại là thiết bị chính, không phải thiết bị phụ.** Kịch bản dùng chính của phòng đấu là quét mã QR
trong lớp học — người chơi vào bằng điện thoại, chủ phòng chiếu máy chiếu. Một giao diện chỉ chạy tốt trên
màn hình rộng là một giao diện hỏng ở đúng chỗ đông người dùng nhất.

Ba ngưỡng dùng trong dự án, không tự đặt thêm: `sm` 640px, `md` 768px, `lg` 1024px.

| Thành phần | Dưới ngưỡng |
|---|---|
| Thanh điều hướng | Dàn menu ngang ẩn từ dưới `lg`, thay bằng **ngăn kéo** mở bằng nút ba gạch |
| Ô tìm kiếm ở thanh trên | Ẩn dưới `md`, thay bằng nút kính lúp dẫn sang trang Khám phá (nơi đã có ô tìm kiếm đầy đủ) |
| Mọi `<Table>` | **Bắt buộc** `scroll={{ x: 'max-content' }}` — bảng cuộn ngang trong khung của chính nó, không đẩy cả trang tràn ra |
| Lưới thẻ quiz | 1 cột → `sm` 2 cột → `lg` 4 cột |
| Sidebar khu quản trị | Tự thu còn 72px (chỉ biểu tượng) từ `lg` xuống, **không ẩn hẳn** |
| Ô điều hướng câu khi làm bài | 8 cột → `sm` 10 → `lg` 5 |

**Ngăn kéo dựng lại đủ mục của dàn menu ngang, không rút gọn.** Một menu "bản mobile" thiếu mục là cách
nhanh nhất để một chức năng trở nên vô hình với nửa số người dùng.

**`scroll={{ x: 'max-content' }}` chứ không phải một số cứng:** số cứng thì bảng ít cột cũng bị ép rộng ra
và sinh thanh cuộn vô cớ.

**Vùng chạm tối thiểu 44px** cho mục trong ngăn kéo và các nút chính trên màn hẹp.

## 5. Nút

| Loại | Thể hiện | Code |
|---|---|---|
| Hành động chính | **Nền đen, chữ trắng, đậm** | `<Button type="primary">` (theme đã đổi màu primary của Button sang đen) |
| Hành động phụ | Viền đen 1px, chữ đen | `<Button>` |
| Hành động nguy hiểm | Chữ/viền đỏ | `<Button danger>` |
| Link | Chữ tím `brand-strong`, gạch chân khi hover | `<Button type="link">` hoặc `<Link>` |

**Không** dùng nút tím đặc — tím chỉ để nhấn và làm link.

## 6. Component dùng chung (bắt buộc dùng lại, không tự vẽ)

| Component | Ở đâu | Dùng khi |
|---|---|---|
| `PageHeader` | `shared/components/PageHeader.tsx` | Đầu mọi trang: tiêu đề + mô tả + vùng nút |
| `EmptyState` | `shared/components/EmptyState.tsx` | Danh sách rỗng (kèm nút hành động gợi ý) |
| `AppLayout` | `shared/components/AppLayout.tsx` | Khung có header sticky + ô tìm kiếm + menu theo vai trò |
| `QuizCard` | `features/quiz/components/QuizCard.tsx` | Mọi nơi hiển thị quiz theo kiểu browse |

### Góc tài khoản ở header

Avatar + tên + thẻ vai trò gộp thành **một** đích bấm, mở `Dropdown` chứa *Trang cá nhân* và *Đăng
xuất*. Không để nút Đăng xuất bày sẵn ngoài header: nó là hành động rời khỏi hệ thống, đứng cạnh các
mục điều hướng thường thì vừa chiếm chỗ vừa dễ bấm nhầm. Trong menu, *Đăng xuất* nằm **dưới một đường
kẻ** và ở cuối — đó là mục duy nhất không hoàn tác được bằng một lần bấm nữa.

Icon lấy từ `@ant-design/icons` (`UserOutlined`, `LogoutOutlined`, `DownOutlined`) — kiểu nét mảnh,
không dùng icon nền đặc. `DownOutlined` phải có: bỏ đi thì không còn dấu hiệu nào cho biết bấm vào sẽ
mở thêm lựa chọn, người dùng sẽ tưởng đó là một đường dẫn.

> `@ant-design/icons` vốn đã nằm trong `node_modules` vì `antd` phụ thuộc nó, nhưng vẫn **khai báo
> tường minh** trong `package.json`: dùng một package chỉ vì thư viện khác tình cờ kéo về là phụ thuộc
> ngầm, sẽ đứt lặng lẽ khi `antd` lên phiên bản đổi phụ thuộc.

## 7. Trung thực dữ liệu

Udemy hiển thị điểm đánh giá và số học viên. Hệ thống này **chưa có** dữ liệu đó, nên:

- **KHÔNG** bịa số sao, số lượt học, giá tiền chỉ để cho giống Udemy.
- Chỗ Udemy đặt rating thì mình hiển thị dữ liệu thật: **số câu hỏi · độ khó · thời lượng · người tạo**.
- Khi nào có bảng `quiz_attempts` (features/03) thì mới thêm số lượt làm bài; có đánh giá thật thì mới thêm sao.

## 8. Checklist khi review một trang mới

- [ ] Chọn đúng bộ mặt (browse dùng card / dashboard dùng bảng)
- [ ] Không có mã màu hardcode, không `borderRadius`/`boxShadow` tự đặt trong component
- [ ] Nút hành động chính là nút đen, không phải nút tím
- [ ] Có dùng `PageHeader`; danh sách rỗng có `EmptyState`
- [ ] Chữ meta dùng cỡ 12px màu `ink-soft`
- [ ] Không có số liệu bịa (rating, số lượt học…)
- [ ] Tiếng Việt có dấu, nhất quán cách gọi: "quiz", "câu hỏi", "ngân hàng câu hỏi"
