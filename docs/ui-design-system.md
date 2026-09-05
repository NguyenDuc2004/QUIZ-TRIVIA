# Chuẩn giao diện (UI Design System)

> **Đây là nguồn sự thật về giao diện.** Mọi trang/component mới phải tuân theo tài liệu này.
> **Modern Soft UI** *(từ 05/09/2026)*: bo góc theo thang, thẻ trắng nổi trên nền kem bằng bóng mờ,
> viền gần như không thấy, nhấc nhẹ khi rê chuột, **nút hành động chính màu tím đặc** `violet-600`.
> Giữ lại từ bản Udemy trước đó: chữ nhỏ mà đậm. Chi tiết ở mục 4 (hình khối) và mục 5 (nút).
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
| Nút hành động đó **màu tím đặc, không gradient** | §4.1 cấm gradient ở khung chức năng. Làm một nút nổi bằng cách phá quy ước màu thì phần còn lại của giao diện trả giá; icon ✨ đủ để phân biệt nó với các nút chính khác. (Trước 05/09/2026 nút này màu đen, theo quy ước §5 khi đó.) |
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

## 2.1. Chế độ sáng / tối

Ba trạng thái, chọn ở menu tài khoản: **Sáng · Tối · Theo hệ thống**. Mặc định là *theo hệ thống* — chỉ
có hai lựa chọn thì lần đầu vào web người dùng bị áp một chế độ họ không chọn, và nếu đó là Sáng trong khi
cả máy đang ở chế độ tối thì trang này là thứ duy nhất chói mắt. Đã chọn tay thì lựa chọn đó **thắng** hệ
điều hành, kể cả khi hệ điều hành đổi sau.

**Cách hoạt động:** thuộc tính `data-theme` trên `<html>`, đặt **trước khi React render** (`main.tsx`).
Đặt trong `useEffect` là quá muộn — trang vẽ xong một khung sáng rồi mới nhảy sang tối, và cái nháy trắng
đó đúng là thứ người bật chế độ tối muốn tránh nhất.

**Hai lối vào, có lý do:**

| Nơi | Làm được gì |
|---|---|
| **Nút `ThemeToggle`** trên thanh điều hướng | Lật Sáng ⇄ Tối bằng **một** lần bấm. Có ở khu học tập, khu quản trị, và cả bốn trang khách |
| Menu tài khoản → *Giao diện* | Chọn cả **ba** trạng thái, gồm *theo hệ thống* |

Nút chỉ hai chiều vì một nút không diễn tả được ba trạng thái. Bản đầu chỉ có menu con — ba lần bấm cho
một thao tác làm hằng ngày, ở chỗ không ai nghĩ tới.

**Icon trên nút cho biết BẤM SẼ RA GÌ, không phải đang ở đâu:** đang sáng thì hiện 🌙, đang tối thì hiện
☀️. Nút mô tả *hành động*, không mô tả *trạng thái*; chú thích khi rê chuột nói rõ bằng chữ.

**Bốn trang khách** (`/login`, `/register`, `/forgot-password`, `/join/:code`) nằm ngoài cả hai layout nên
phải gắn nút riêng — không có nó thì người chưa đăng nhập không có đường nào đổi giao diện, mà trang đăng
nhập lại đúng là trang đầu tiên họ thấy.

**Chuyển màu 180ms**, và chỉ cho ba thuộc tính `background-color`, `border-color`, `color`. **Không dùng
`transition: all`**: `all` kéo theo `transform`, `width`, `opacity` của mọi phần tử, làm menu xổ xuống và
modal của Ant Design mở ra chậm và giật — hỏng đúng thứ đang muốn làm mượt. Tắt hẳn với
`prefers-reduced-motion`.

**Hai nơi khai màu, bắt buộc cùng giá trị:** `:root[data-theme='dark']` trong `index.css` (cho Tailwind) và
`darkColors` trong `antdTheme.ts` (cho Ant Design). Hai hệ nhận màu theo hai đường khác nhau; lệch nhau thì
thẻ của Ant Design và thẻ dựng bằng Tailwind đứng cạnh nhau sẽ khác màu nền.

### Bốn quyết định trong bảng màu tối

| Quyết định | Vì sao |
|---|---|
| Nền tối là **xám than `#17181c`**, không phải đen tuyền | Đen tuyền đã có nghĩa riêng: nền khu quản trị, một *tín hiệu cảnh báo* (§1). Chế độ tối cũng đen tuyền thì tín hiệu đó biến mất — admin không phân biệt được đang ở khu quản trị hay chỉ đang bật chế độ tối |
| **Nút chính đảo thành nền sáng chữ tối** | Ở chế độ sáng nền đen là thứ nổi nhất trên trang trắng; ở chế độ tối nút đen trên nền xám than gần như biến mất, và nút quan trọng nhất màn hình lại là thứ khó thấy nhất |
| Tím thương hiệu **sáng lên một bậc** | `--color-brand-strong` (#5624d0) là tím đậm cho nền sáng, đặt lên nền tối gần như không đọc được — đúng lỗi đã gặp với link ở chân trang |
| Chân trang **nhạt hơn** nền chung | Ở chế độ sáng nó tối hơn nền; ở chế độ tối nếu vẫn tối hơn thì nó hoà tan, không còn thấy ranh giới |

### Ba loại màu KHÔNG được viết tuyệt đối

1. **Nền thẻ.** Dùng `bg-surface`, không dùng `bg-white`. `bg-white` ở chế độ tối vẫn trắng, và chữ sáng
   nằm lên đó thành không đọc nổi. Đã đổi **56 chỗ**.
2. **Màu ngữ nghĩa đúng/sai/gấp.** Dùng `.bg-correct`, `.bg-wrong`, `.text-urgent` — không dùng
   `bg-green-50`, `bg-red-50`, `text-red-600`.
3. **Lớp trang trí ở §4.1.** Mỗi lớp phải có bản `:root[data-theme='dark']` tương ứng; chỉ đổi **độ
   sáng**, giữ nguyên tông màu để người dùng vẫn nhận ra đỏ là A, xanh dương là B.
4. **Token riêng của component Ant Design.** `appTheme.components.Layout` đặt cứng `headerBg: '#ffffff'`
   và `bodyBg: '#ffffff'`; `darkTheme` kế thừa cả khối `components`, nên **phải khai lại** — token riêng
   của component **thắng** token toàn cục, và đây là mảng trắng cuối cùng còn sót sau khi đã đổi hết
   `bg-white`. Cùng lý do với `Table` (nền hàng tiêu đề) và `Menu` (nền mục đang chọn).

**Ngoại lệ duy nhất được phép dùng `bg-white`:** nền của **mã QR**. Camera đọc mã bằng tương phản đen trên
trắng — đó là ràng buộc vật lý của máy quét, không phải lựa chọn thẩm mỹ.

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

## 4. Hình khối — Modern Soft UI *(đổi 05/09/2026)*

Ngôn ngữ hình khối của dự án **đã đổi**. Bản trước là *"bo góc 4px cho mọi thứ, viền 1px thay cho đổ
bóng"* — vuông vắn kiểu Udemy. Bản này mềm và nổi: **bo góc theo thang ba bậc, bóng mờ toả rộng, viền
gần như không thấy**. Ghi lại cả bản cũ ở đây để những chú thích trong mã còn nhắc tới nó vẫn đọc được.

### Thang bo góc

| Bậc | Giá trị | Dùng cho | Token |
|---|---|---|---|
| Panel | 24px | Khối mở đầu lớn, khối công bố điểm | `--radius-panel` / `shape.radiusPanel` |
| Card | 16px | Thẻ, bảng, panel nội dung, modal, drawer, ảnh | `--radius-card` / `borderRadiusLG` |
| Control | 12px | Nút, ô nhập, ô chọn, ô đáp án | `--radius-control` / `borderRadius` |
| Small | 8px | Mục trong danh sách, ô số câu | `--radius-small` / `borderRadiusSM` |
| Tròn | 9999px | Nhãn trạng thái `<Pill>`, `<Tag>`, chip lọc, nút hình tròn | — |

Bo góc giờ nói lên **cấp** của phần tử, không còn là một hằng số. Nhờ vậy hai ngoại lệ của bản cũ
(`<Pill>` và khung soạn tin trợ lý) **không còn là ngoại lệ** — chúng chỉ là hai bậc trong thang.

### Viền và bóng

- **`--color-line`** (`slate-200/60` sáng, `white/10` tối) — viền thẻ/panel, siêu mờ. Nó vẫn tồn tại
  và cố ý tồn tại: bỏ hẳn thì trên màn hình tương phản cao hoặc khi người dùng bật `forced-colors`,
  các thẻ mất hết ranh giới, vì **bóng đổ là thứ đầu tiên những chế độ đó loại bỏ**.
- **`--color-line-strong`** — viền **ô nhập, ô chọn, vùng bấm được**. Đây là chỗ duy nhất cố ý đi
  chệch khỏi "viền siêu mờ cho mọi thứ": một cái thẻ mờ ranh giới thì hơi khó nhìn, còn một ô nhập mờ
  ranh giới thì người dùng **không biết bấm vào đâu để gõ**. WCAG 1.4.11 đòi thành phần giao diện
  tương phản tối thiểu 3:1 với nền, mà `slate-200/60` trên nền trắng không đạt.
- **`--shadow-soft`** — hai lớp: một lớp 1px sát mép cho ranh giới, một lớp toả rộng rất mờ cho cảm
  giác nổi. Chỉ lớp toả rộng thì thẻ trông lơ lửng mà không có mép; chỉ lớp sát mép thì nó chỉ là cái
  viền đậm hơn.
- **Ở chế độ tối, bóng đổ gần như vô hình.** Thứ vẽ được mép ở đó là **ánh sáng**, không phải bóng:
  nền thẻ sáng hơn nền trang một bậc (`#1e293b` trên `#0f172a`), và viền là một vệt trắng 10%.

### Nền

| | Sáng | Tối |
|---|---|---|
| Nền trang (`--color-canvas`) | `#f8fafc` | `#0f172a` |
| Nền thẻ (`--color-surface`) | `#ffffff` | `#1e293b` |
| Nền chìm (`--color-surface-subtle`) | `#f1f5f9` | `#172033` |

Thẻ **trắng nổi trên nền kem** — chính chênh lệch này thay cho đường kẻ. Thanh điều hướng cũng vậy:
nó trắng trên nền kem, nên nó nổi lên như một lớp riêng mà không cần viền dưới chân.

**Nền tối đổi sang họ xanh đá làm MẠNH THÊM tín hiệu đen tuyền của khu quản trị** (§1): đen không chỉ
tối hơn mà còn khác hẳn sắc, nên admin phân biệt được ngay cả khi màn hình chỉnh sáng thấp — chỗ mà
hai mức xám gần nhau thì không.

### Hiệu ứng vi tương tác

- **`.soft-lift`** — nhấc 2px + bóng đậm hơn, `0,2s ease-out`. Dùng cho **thẻ và ô bấm được cỡ lớn**.
- **KHÔNG dùng cho hàng bảng.** Hàng bảng cao 50px và xếp sát nhau; nhấc một hàng làm cột chữ bên cạnh
  giật theo, mà bảng là thứ người dùng rà mắt theo hàng ngang nên nhiễu ở đó đắt hơn nhiều so với ở
  một lưới thẻ. Hàng bảng vẫn báo hiệu bằng **đổi nền**.
- **KHÔNG dùng `transition: all`.** `all` kéo theo cả `width`, `opacity`, `transform` của những thứ
  vốn không định chuyển động — dropdown và modal của Ant Design giật cục vì đúng lý do đó. Liệt kê
  từng thuộc tính.
- **`prefers-reduced-motion`**: bỏ phần dịch chuyển, **giữ bóng**. Bóng vẫn báo được "đang trỏ vào
  đây" mà không có gì trượt trên màn hình.
- Chiều cao control: 40px (thường), 48px (nút CTA lớn).
- **Gradient: chỉ ở KHOẢNH KHẮC, không ở KHUNG CHỨC NĂNG.** Xem mục 4.1.

### Lớp dùng chung — đừng viết lại trong component

| Lớp | Là gì |
|---|---|
| `.soft-panel` | Nền thẻ + viền mờ + bo 16px + bóng mềm + `overflow: hidden`. Thay cho tổ hợp `border border-line bg-surface` từng rải ở **45 chỗ** |
| `.soft-lift` | Nhấc lên khi rê chuột |
| `.browse-card` | Thẻ quiz (đã gồm nền, viền, bóng, nhấc) |

Bán kính và bóng **không được hardcode trong component** — chúng là quyết định của hệ thống thiết kế
nên phải sống trong hệ thống thiết kế. Component chỉ gọi tên lớp, hoặc dùng utility sinh từ token
(`rounded-card`, `rounded-control`).

`.soft-panel` mang `overflow: hidden` để bảng bên trong bị cắt theo góc bo. **Nếu sau này thêm bảng có
tiêu đề dính, phải bỏ lớp đó ra khỏi panel ấy** — `overflow: hidden` tạo một vùng cuộn mới và phần tử
`position: sticky` bên trong sẽ chết.

### Bậc chữ và tương phản — đo, đừng ước lượng

| Bậc | Sáng | Tối | Dùng cho |
|---|---|---|---|
| `--color-ink` | `#0f172a` | `#f1f5f9` | Chữ chính |
| `--color-ink-soft` | `#475569` | `#cbd5e1` | Chữ phụ, mô tả, chú thích |
| `--color-placeholder` | `#94a3b8` | `#94a3b8` | Chữ gợi ý trong ô nhập |

**Đo trên nền CHÌM, không chỉ trên nền thẻ.** `slate-500` (`#64748b`) trên nền trắng là 4,76:1 — đạt.
Nhưng trên `--color-surface-subtle` `#f1f5f9` (nền tiêu đề bảng và cột phụ màn Trợ lý) nó chỉ còn
**4,27:1**, tức *không* đạt AA. Chữ phụ ở đây thường 12px nên không được hưởng ngoại lệ "chữ lớn". Vì
vậy bậc này là `slate-600`.

**Đạt AA là sàn, không phải mục tiêu.** Chữ phụ ở chế độ tối trước đây là `#9aa0a6` (~5:1) — đạt
chuẩn, nhưng người dùng vẫn báo khó đọc. Nâng lên `slate-300` (~8,9:1) mà vẫn giữ ba bậc phân biệt rõ.

**Dùng họ xanh đá cho cả ba bậc.** Xám ấm (`#9aa0a6`, `#e8eaed`) đặt trên nền xanh đá thì ngả vàng
nhẹ — thấy rõ nhất ở những khối chữ dài.

**Làm mờ bằng `opacity` cũng phải đo.** Huy hiệu chưa mở khoá cố ý mờ đi, nhưng `opacity-50` ở chế độ
tối kéo chữ xuống ~4,1:1. Dùng `opacity-60`: vẫn mờ rõ ràng mà đọc được.

### Cạm bẫy: lề trên của tiêu đề (hệ quả của việc không nạp preflight)

Dự án cố ý không nạp preflight của Tailwind, nên `h1`–`h6` giữ **lề trên mặc định của trình duyệt**.
Ant Design có token `titleMarginTop` nhưng đọc mã nguồn thì nó chỉ áp qua bộ chọn **anh-em**
(`& + h1.ant-typography`, `div, p, h1… + h1…`) — tiêu đề **đứng đầu khối** không khớp bộ chọn nào, và
phần đó AntD giao cho preflight, thứ ở đây không có.

Hậu quả không nằm ở khoảng cách mà ở **căn hàng**: trong một hàng flex, lề trên làm hộp của tiêu đề
cao hơn hẳn phần chữ của nó, nên `items-center` căn theo hộp chứ không theo chữ.

| Chỗ | Biểu hiện |
|---|---|
| `PageHeader` | Nút hành động bên phải nằm **cao hơn** tiêu đề — ở **mọi trang** |
| "Gợi ý cho bạn" | Dòng mô tả bên cạnh trôi lên trên, dù hàng đã đặt `items-center` |

Đã reset ở `index.css` với điểm đặc hiệu **thấp** (0,1,1) để hai bộ chọn anh-em của AntD (0,2,1) vẫn
thắng — khoảng cách 1,2em giữa hai tiêu đề xếp chồng giữ nguyên, chỉ trường hợp đứng đầu khối về 0.

**Hàng chỉ gồm chữ dùng `items-baseline`, không dùng `items-center`.** Căn tâm hộp giữa chữ 20px và
chữ 12px vẫn lệch vài pixel vì hai hộp cao khác nhau; thứ mắt đọc là **đường chân chữ**. Hàng có lẫn
phần tử dạng khối (thẻ nhãn, avatar, nút) thì vẫn `items-center`.

### Cạm bẫy: màu tuyệt đối ghép với màu token

`--color-ink` là màu **chữ**, nên ở chế độ tối nó lật thành gần trắng. Ghép `bg-ink` với `text-white`
cho ra **chữ trắng trên nền gần trắng** — phần tử biến mất, và biến mất đúng ở trạng thái *đang được
chọn*, tức thứ người dùng cần thấy nhất.

Lỗi này đã xuất hiện ở **hai** trang (chip danh mục ở Khám phá, ô số câu đang làm ở màn làm bài), nên
nó không phải sơ suất một lần mà là cái bẫy có sẵn trong cách đặt tên token. Dùng **`text-canvas`** —
nó lật cùng chiều với `--color-ink` nên cặp này tương phản ở cả hai chế độ.

Có phép kiểm chặn cả lớp lỗi này: `src/shared/theme/mauTuyetDoi.test.ts` quét toàn bộ `.tsx` tìm
className vừa dùng nền token vừa dùng chữ trắng/đen tuyệt đối, và báo đúng `file:dòng`.

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
| Huy chương top 3 (`.podium-*`) — dùng ở **cả hai** bảng xếp hạng | |
| Sảnh phòng đấu: khối mở đầu và viền hai thẻ (`.room-hero`, `.room-card-*`) | |

Ba lý do cụ thể cho cột phải:

1. **Chữ trên nền chuyển màu có độ tương phản không đoán được** — chỗ đọc được, chỗ không.
2. **Nút gradient lặp trên mọi trang làm giao diện trông như một mẫu tải về.** Một màu đặc duy nhất cho
   nút chính (tím `violet-600` từ 05/09/2026, trước đó là đen) chính là thứ đang giữ cho 16 nhóm chức
   năng trông như *một* sản phẩm chứ không phải 16 trang rời.
3. **Màn làm bài và khu quản trị có lý do riêng.** Màn làm bài cần sự tập trung — đó là lý do nó ẩn cả
   chân trang (mục 1). Nền đen của khu quản trị là một *tín hiệu cảnh báo*; tô màu vào đó là làm loãng
   tín hiệu.

Đối chiếu với sản phẩm cùng loại thì ranh giới này quen thuộc: Kahoot rực rỡ ở **màn chơi**, còn bảng
quản lý câu hỏi của họ vẫn trắng-xám bình thường.

**Màu phải mang thông tin, không chỉ trang trí.** Màu khối bìa buộc vào **danh mục** chứ không vào tiêu
đề — bản đầu chọn bằng `title.charCodeAt(0)`, nên hai quiz cùng "Toán học" ra hai màu khác nhau và mắt
người dùng học một quy luật *không tồn tại*. Cùng nguyên tắc: màu phương án trong phòng đấu buộc vào **vị
trí** (ổn định suốt một câu), và màu khối kết quả buộc vào **mức điểm**.

**Cùng một khái niệm thì cùng một hình ảnh.** Hạng nhất ở bảng xếp hạng mùa và hạng nhất ở bảng xếp hạng
của một quiz dùng **chung** lớp `.podium-*`. Vẽ hai kiểu khác nhau cho cùng một khái niệm bắt người dùng
học hai lần cùng một thứ.

**Tô viền, không tô nền, khi bên trong là biểu mẫu.** Hai thẻ ở sảnh phòng đấu phân biệt bằng viền 2px và
màu tiêu đề; nền giữ trắng vì bên trong có ô chọn và ô nhập, mà nền màu làm chữ trong đó khó đọc.

**Chuyển động phải tôn trọng `prefers-reduced-motion`.** Nhịp đập của ngọn lửa chuỗi ngày học tắt hẳn với
người đặt hệ điều hành ở chế độ giảm chuyển động — một biểu tượng nhấp nháy liên tục là thứ gây khó chịu
thật, và đó là trang người học mở thường xuyên.

### 4.2. Màn hình hẹp

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

### Hai lỗi đã gặp khi làm phần này — đọc trước khi sửa thanh điều hướng

**1. ĐỪNG đặt lớp `display` (`hidden`, `block`, `flex`…) thẳng lên component Ant Design — hãy bọc nó
trong một `div` của mình và đặt lên `div` đó.**

Chuyện này đi qua hai bước sai liên tiếp, cả hai đều đáng ghi:

- Bước sai thứ nhất: viết `className="hidden md:block"` lên `<Input.Search>`. Không ăn, vì Ant Design chèn
  luật `display` cho `.ant-input-search` ở **ngoài** layer của Tailwind (đúng cái bẫy §3, lần thứ ba).
  Triệu chứng: trên điện thoại có **hai** ô tìm kiếm cạnh nhau, cái thứ hai bị bóp gần bằng 0 nhưng vẫn
  chiếm chỗ.
- Bước sai thứ hai: thêm `!` để thắng — `hidden! md:block!`. Nó **thắng thật**, và đó chính là lúc bố cục
  vỡ. `Input.Search` không phải một ô nhập mà là một **nhóm** gồm ô nhập và nút bấm, có bố cục nội bộ
  riêng; ép `display: block !important` lên vỏ ngoài làm nhóm đó tách ra — ô nhập trôi lên trên thanh
  điều hướng, nút kính lúp rơi xuống dòng dưới.

Bài học: `!` cho phép **ghi đè** CSS của Ant Design, nhưng ghi đè được không có nghĩa là **nên** ghi đè.
Với thuộc tính `display` của một component có bố cục nội bộ, ghi đè là phá. Bọc ngoài bằng `div` của mình
thì lớp Tailwind ăn bình thường, không cần `!`, và không chạm gì vào bên trong component.

`!` vẫn đúng và vẫn cần cho **màu sắc và kích thước** (xem §3) — chỉ riêng `display` là không.

**2. Một phần tử rộng quá làm cả trang bị đẩy sang phải.** Hệ quả không dừng ở phần tử đó: thanh điều
hướng và tiêu đề bị **cắt mất bên trái**, và không có gì trên màn hình cho biết vì sao. Hai nguyên nhân
thật đã tìm được:

- Khối tài khoản (avatar + tên đầy đủ + nhãn vai trò) đặt `shrink-0` nên không bao giờ nhường chỗ — dễ tới
  200px cho một thông tin người dùng đã biết. Ẩn tên và nhãn dưới `md`, giữ avatar và mũi nhọn.
- Dàn menu ngang thiếu `min-w-0`: mặc định flex item không co nhỏ hơn nội dung, nên năm mục chữ không
  xuống dòng ép cả thanh rộng ra.

Khung ngoài có thêm `overflow-x-clip` làm **chốt cuối**, không phải giải pháp chính. Dùng `clip` chứ không
`hidden`: `overflow-hidden` biến phần tử thành khối cuộn và làm hỏng `position: sticky` của thanh điều
hướng ngay bên trong.

### 4.3. Bảng danh sách

**Cột thao tác: một hành động chính, phần còn lại vào menu ba chấm** (`RowActions`).

Bảng "Quiz của tôi" trước đây có sáu liên kết chữ dàn ngang trên mỗi hàng — *Soạn câu hỏi · Làm thử ·
Thống kê · Xuất · Sửa · Xóa*. Ba vấn đề, cái thứ ba nghiêm trọng nhất:

1. Sáu chữ chiếm 260px, ép cột nội dung — thứ người dùng thật sự đọc — hẹp lại.
2. Trên màn hẹp chúng xuống dòng, một hàng cao gấp ba hàng khác.
3. **Xóa** — thao tác không hoàn tác được — nằm cách *Soạn câu hỏi* vài chục pixel, và lại là chữ đỏ nên
   hút mắt nhất trong sáu chữ.

Trong menu, thao tác phá hoại đặt **sau vạch ngăn** và tô đỏ. Cột thao tác chỉ có một hành động thì dùng
**nút viền có icon**, không dùng chữ trần: hành động phải nhận ra được là hành động từ hình dáng, không
chỉ từ màu.

**Hộp xác nhận phải là `Modal`, không phải `Popconfirm`.** `Popconfirm` bám vào phần tử kích hoạt, mà phần
tử đó giờ là một mục trong menu — menu đóng ngay khi bấm nên hộp xác nhận mất điểm neo và **không hiện**.

**Nhãn trạng thái dùng `<Pill>`**, không dùng `<Tag>`:

| Loại nhãn | Cách thể hiện |
|---|---|
| Thang **có thứ tự** (độ khó, trạng thái xử lý) | Chấm màu dẫn đầu — ba chấm xanh/vàng/đỏ đọc thành một thang ngay |
| Phân loại **không thứ tự** (nguồn, hiển thị) | Biểu tượng dẫn đầu (✨ cho AI sinh, 🌐/🔒 cho hiển thị) |

**Chấm màu, không phải nền màu đậm:** nền đậm ở mỗi ô làm bảng loang lổ và tranh chỗ với nội dung. Và
**màu không bao giờ là nguồn thông tin duy nhất** — chữ luôn đứng cạnh chấm, nên người mù màu đọc được
đúng thứ người khác đọc.

**Hàng bảng** có `padding` dọc 14px thay cho mặc định: nhiều bảng ở đây có ô hai dòng (nội dung + dòng
phụ), nên hàng dính sát nhau và khó dò theo hàng ngang.

### 4.4. Công thức toán — `<MathText>`

**Chỉ dựng ở nội dung do HỆ THỐNG hoặc NGƯỜI RA ĐỀ viết, không dựng ở chữ người dùng vừa gõ.** Mô
hình và người soạn đề được dặn dùng `$...$` nên với họ dấu `$` là mốc có chủ ý; người học gõ tự do,
và một câu như *"sách giá 100$ còn khoá học 200$"* có đủ dấu mở lẫn dấu đóng. Bên nào nhận quy ước
thì bên đó được dựng. Trích dẫn nguồn cũng giữ nguyên xi — nó là thứ để **đối chiếu** với tài liệu
gốc, nên không được đẹp hơn sự thật.

Nội dung có công thức dựng bằng **KaTeX**, đánh dấu bằng `$...$`.

**Bắt buộc phải có mốc `$`, hệ thống KHÔNG tự nhận diện.** KaTeX chỉ dựng được LaTeX, còn nội dung câu hỏi
là văn bản thường do người dùng hoặc mô hình viết — `y = 2^(x^2 - x)` là chữ, không phải LaTeX. Muốn dựng
nó thì phải *đoán* chỗ nào là toán, và đoán sai là làm hỏng chính câu chữ của người dùng: một câu Tin học
nhắc `a/b`, một câu Tiếng Anh có dấu `^` sẽ bị bóp thành ký hiệu vô nghĩa.

**Luật tuyệt đối: không sửa một ký tự nào của chữ thường.** Một `$` chỉ mở công thức khi có `$` đóng phía
sau **và** phần ở giữa không rỗng. `100$$ một tháng` giữ đủ hai dấu; `Chi phí $50` giữ nguyên. Bản đầu gộp
`$$` thành một `$` để "thoát" ký tự — đó cũng là sửa chữ của người dùng, chỉ theo hướng khác.

**Công thức sai cú pháp hiện lại chữ gốc**, không ném lỗi: người học không sửa được LaTeX của người ra đề.

**Prompt sinh đề đã được cập nhật** để mô hình xuất LaTeX trong `$...$`. Câu hỏi tạo trước thay đổi này
vẫn là chữ thường và hiện nguyên xi — đúng như thiết kế, vì hệ thống không tự suy diễn.

### 4.5. Bề ngang vùng nội dung

Mặc định **72rem (1152px)**, và lề trống hai bên trên màn rộng là **cố ý**: nó giữ lưới thẻ ở 4 cột và
giữ dòng chữ đủ ngắn để mắt không phải quét ngang cả gang tay. Kéo hết bề ngang thì được cái đầy, mất
cái đọc được.

**Không lấp lề bằng nội dung phụ.** Bảng xếp hạng phụ, "chủ đề thịnh hành", số lượt xem — hoặc trùng
với thứ đã có, hoặc phải **bịa dữ liệu**, thứ mục 7 cấm rõ.

**Ngoại lệ: trang có bảng dày** khai thêm lớp `trang-rong` để nới lên 90rem. Ở đó thứ quyết định không
còn là độ dài dòng chữ mà là quét theo hàng ngang, và các bảng này đặt `scroll={{ x: 'max-content' }}`
— thiếu chỗ thì bảng **cuộn ngang bên trong khung**, tức người dùng phải kéo để xem cột cuối ngay cạnh
một vùng trống rộng bằng nửa cái bảng.

Sáu trang đang dùng: Học liệu · Quiz của tôi · Ngân hàng câu hỏi · Thống kê quiz · Lịch sử làm bài ·
Kết quả bài tập. Khu quản trị vốn đã full-width nên không thuộc diện này.

**Dùng `:has()` chứ không một danh sách route trong `AppLayout`.** Danh sách route đặt quyết định về bề
ngang của một trang ở một tệp khác hẳn trang đó; thêm trang mới nghĩa là phải *nhớ ra* có cái danh sách
ấy, và người quên không nhận được tín hiệu nào. Với `:has()`, trang tự khai bằng một lớp trên chính nó
— quyết định nằm cạnh thứ nó nói về. Trình duyệt quá cũ không hiểu thì bỏ qua luật và trang giữ bề
ngang mặc định: hỏng về phía an toàn, không vỡ bố cục.

### 4.6. Màn hội thoại: chỉ khung chat cuộn

Trang Trợ lý ràng chiều cao bằng `.chat-trang` (`calc(100dvh - 136px)`, chỉ từ `lg`), rồi để **mỗi
khối tự cuộn trong lòng nó**. Bản trước để khối chat `min-h-[60vh]` nên nó phình theo số tin nhắn và
thứ cuộn là *cả trang*: đọc tới câu trả lời thứ ba thì tiêu đề và cột học liệu đã trôi mất, muốn đổi
tài liệu phải cuộn ngược lên.

Ba điều dễ sai khi dựng kiểu bố cục này:

1. **`min-h-0` phải có ở MỌI mắt xích** từ khung ngoài tới vùng cuộn. Mặc định `min-height` của flex/
   grid item là `auto` — nó nở đúng bằng nội dung và không bao giờ nhỏ hơn để mà phải cuộn. CSS không
   báo gì; nó chỉ lặng lẽ cuộn cả trang.
2. **Không bọc bằng `<Space>`.** `Space` đặt mỗi phần tử con vào một `.ant-space-item` riêng, nên
   `flex-1` áp vào cái bọc chứ không tới được phần tử thật.
3. **Chỉ áp từ `lg`.** Dưới ngưỡng đó bố cục xếp dọc; nhồi ba vùng cuộn vào màn hình điện thoại thì
   mỗi vùng còn vài dòng, để cả trang cuộn như thường là đúng hơn.

Con số 136px = 72px thanh điều hướng (token `Layout.headerHeight`) + 64px đệm dọc của `Content`.
`dvh` chứ không `vh`: trên trình duyệt điện thoại `vh` tính theo khung lúc thanh địa chỉ ẩn, nên đáy
trang — đúng chỗ đặt ô nhập — bị đẩy khỏi vùng nhìn thấy.

**Ô nhập trong khung nổi phải tắt viền và vòng focus của Ant Design ở mọi trạng thái**, và thắng bằng
cách thêm tên thẻ vào bộ chọn (`textarea.ant-input:focus` = (0,2,1)) chứ **không** bằng `!important`.
AntD v6 sinh CSS lúc chạy và chèn sau tệp của dự án, nên bằng điểm đặc hiệu là nó thắng nhờ đứng sau;
còn `!important` thì từng thắng đúng luật rồi làm vỡ bố cục nội bộ của `Input.Search` (§4.2). Dấu
hiệu focus không mất — nó chuyển ra viền của cả khung qua `:focus-within`.

### 4.7. Cột phụ: nền chìm thay cho lưới viền

Cột danh sách đứng cạnh một khung nội dung chính (danh sách hội thoại, danh sách học liệu ở màn Trợ
lý) dùng **một nền `--color-surface-subtle` bo 12px, không kẻ viền quanh từng khối và từng dòng**.

Bản đầu của màn Trợ lý kẻ viền đủ cả hai: viền quanh khối, viền dưới mỗi dòng, viền quanh khung chat.
Kết quả là một bàn cờ — mắt phải bỏ qua hàng chục đường kẻ mới đọc được tên hội thoại, trong khi thứ
duy nhất cần phân biệt là "cột phụ" với "nội dung chính". Một nền chìm làm đúng việc đó mà không thêm
đường nào.

**Thao tác phá hoại trong danh sách kiểu này ẩn cho tới khi rê chuột** (`.chat-rail-xoa`) — nhưng ẩn
bằng `opacity`, không bằng `display: none`, và phải hiện lại ở `:focus-within`. Phần tử ẩn bằng
`display` không nhận được focus bàn phím, nên người dùng bàn phím sẽ Tab tới một nút vô hình mà không
có cách nào biết mình đang đứng ở đâu. Trên thiết bị chạm (`@media (hover: none)`) nút luôn hiện: ở đó
`:hover` hoặc không bao giờ đúng, hoặc dính lại sau khi chạm.

## 5. Nút

| Loại | Thể hiện | Code |
|---|---|---|
| Hành động chính | **Nền tím `violet-600` `#7c3aed`, chữ trắng, đậm** | `<Button type="primary">` |
| Hành động phụ | Viền mờ, chữ theo `--color-ink` | `<Button>` |
| Hành động nguy hiểm | Chữ/viền đỏ | `<Button danger>` |
| Link | Chữ tím `brand-strong`, gạch chân khi hover | `<Button type="link">` hoặc `<Link>` |

**Đổi 05/09/2026: nút chính từ NỀN ĐEN sang TÍM.** Quy ước đen đến từ bản giao diện lấy cảm hứng
Udemy; bản Modern Soft UI (§4) đã bỏ hẳn phong cách đó, và trên nền kem với thẻ bo tròn mềm thì một
nút đen tuyền là thứ cứng nhất màn hình.

Đổi **toàn cục**, không đổi lẻ vài nút: trong cùng một trang mà có nút chính tím và nút chính đen thì
người dùng đọc hai màu đó thành hai **mức** quan trọng khác nhau, trong khi chúng ngang nhau. Một màu
cho một vai trò, hoặc không đổi gì.

Link giờ cùng họ tím với nút nhưng không bị nhầm: **link là chữ gạch chân nằm trong dòng văn, nút là
khối đặc có nền** — màu chưa bao giờ là thứ duy nhất tách hai cái đó.

Chữ trắng trên `#7c3aed` là ~5,9:1 (đạt AA). Màu khi rê chuột `#8b5cf6` xuống ~4:1 — chấp nhận vì đó
là trạng thái tạm thời, còn trạng thái nghỉ mới là thứ người dùng đọc.

**Nút chính giữ nguyên màu tím ở chế độ tối**, không đảo sang nền sáng như bản nền-đen phải làm.

### Nút nào là "hành động chính"

Là hành động chính của **khối chứa nó**, không phải của cả trang. Một thẻ chỉ có một hành động thì
hành động đó là chính, dù thẻ nằm trong lưới bốn thẻ.

| Đúng | Sai |
|---|---|
| Hai thẻ ngang hàng ("Mở phòng" / "Vào phòng") thì **cả hai** đều primary | Một bên primary một bên mặc định — hai lựa chọn ngang nhau trông thành chính/phụ |
| Thẻ gợi ý chỉ có nút "Làm thử" → primary | Để mặc định vì "nó nhỏ" |
| Màn báo lỗi chỉ có lối ra duy nhất → primary | Để chìm, người dùng đọc xong lỗi không thấy phải bấm đâu |
| "Mở phòng đấu trí" ở trang giới thiệu quiz → **mặc định** | Primary — nó là lựa chọn *thay thế* cho "Bắt đầu làm bài" ngay phía trên, hai primary trong một khối thì không còn cái nào là chính |

## 6. Component dùng chung (bắt buộc dùng lại, không tự vẽ)

### Ảnh bìa quiz — luôn dùng `QuizCover`

**Không tự dựng khối bìa.** Mọi chỗ quiz xuất hiện đều gọi `QuizCover`: lưới Khám phá, thẻ gợi ý,
trang giới thiệu quiz, ảnh nhỏ trong bảng "Quiz của tôi". Chỉ đổi `coIcon` (`lon` 72px / `vua` 54px /
`nho` 18px) theo bề ngang chỗ đặt — **khuôn thì không đổi**.

Trước đó mỗi trang tự dựng, và chúng trôi khỏi nhau: ảnh nhỏ trong bảng dùng tỉ lệ 16:10, trang giới
thiệu **không vẽ gì** khi quiz chưa có ảnh, còn thẻ có ảnh thật thì cao hơn thẻ vẽ gradient ~40px.
Quy ước *"một quiz phải trông như chính nó ở mọi chỗ"* đã nằm trong chú thích từ lâu — nhưng quy ước
chỉ nằm trong lời văn thì nó bị phá ở trang thứ tư.

**Bẫy đã sập một lần, ghi lại để không sập nữa:** ảnh bên trong khuôn `aspect-video` phải
`absolute inset-0`, **không** `h-full`. `height: 100%` phân giải theo chiều cao khối chứa, mà chiều
cao đó do `aspect-ratio` suy ra từ bề ngang nên nó là `auto` — phần trăm không phân giải được, ảnh rơi
về chiều cao gốc của tệp và **kéo khuôn phình theo**. Khối gradient không có con nào nên nó giữ đúng
16:9, và hai thứ lệch nhau. `absolute` đo theo hộp của phần tử định vị gần nhất — một giá trị xác định
— nên ảnh không tác động được tới khuôn.

Đó cũng là điều kiện để câu *"mọi ảnh bìa bằng nhau"* đúng với **mọi tệp người dùng tải lên**, dù nó
vuông, dọc hay siêu rộng: `object-cover` cắt cho vừa khuôn thay vì bắt khuôn giãn theo ảnh.

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
