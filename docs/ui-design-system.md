# Chuẩn giao diện (UI Design System)

> **Đây là nguồn sự thật về giao diện.** Mọi trang/component mới phải tuân theo tài liệu này.
> Phong cách lấy cảm hứng từ Udemy: vuông vắn, viền mảnh, chữ nhỏ mà đậm, nút hành động chính màu đen, tím dùng để nhấn.
>
> Chỉ mô phỏng **phong cách**; không dùng logo, tên thương hiệu, hình ảnh hay nội dung của Udemy. Font Udemy Sans có bản quyền → dùng **Inter** (tự host qua `@fontsource-variable/inter`).

## 1. Hai bộ mặt — dùng đúng chỗ

Udemy có hai kiểu bố cục rất khác nhau. Chọn sai kiểu là lỗi giao diện, không phải chuyện thẩm mỹ.

| Kiểu | Khi nào dùng | Trang của dự án |
|---|---|---|
| **Học viên** (browse) | Người dùng đi *tìm* nội dung | Khám phá quiz, giới thiệu quiz, làm bài, phòng đấu, gợi ý |
| **Bảng điều khiển** (dashboard) | Người dùng đi *quản lý* nội dung của mình | Quiz của tôi, Ngân hàng câu hỏi, Soạn quiz, Thống kê, Admin |

- **Browse** → lưới card, ảnh/khối màu 16:9, tiêu đề đậm cắt 2 dòng, nhiều khoảng trắng.
- **Dashboard** → bảng dày thông tin, nút viền mảnh, hầu như không màu rực, không card lồng card.

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

**Quy tắc:** không hardcode mã màu trong file component. Chỉ dùng token Ant Design (`ConfigProvider`) hoặc class Tailwind sinh từ `@theme` (`bg-ink`, `text-ink-soft`, `border-line`…).

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
- **Viền 1px `--color-line`** thay cho đổ bóng. Chỉ dùng shadow nhẹ khi hover card: `0 2px 4px rgba(0,0,0,.08)`.
- Chiều cao control: 40px (thường), 48px (nút CTA lớn).
- Không dùng gradient trừ khối ảnh giả lập của card khi quiz chưa có ảnh bìa.

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
