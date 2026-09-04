import type { ReactNode } from 'react'

/**
 * Nhãn trạng thái dạng viên thuốc.
 *
 * ## Vì sao không dùng `<Tag>` của Ant Design
 * `Tag` bo góc 4px theo quy ước hình khối của dự án (ui-design-system.md §4) và không có chỗ đặt chấm màu
 * dẫn đầu. Nhãn trạng thái là **ngoại lệ có chủ ý** của quy ước đó: chúng nằm lẫn trong bảng dày đặc chữ,
 * và hình viên thuốc tách chúng ra khỏi chữ thường ngay từ hình dáng — trước cả khi mắt đọc tới màu.
 *
 * ## Chấm màu, không phải nền màu
 * Nền màu đậm ở mỗi ô làm bảng loang lổ và tranh chỗ với nội dung. Một chấm nhỏ dẫn đầu đủ để phân biệt,
 * và giữ được điều quan trọng hơn: **màu không phải nguồn thông tin duy nhất**. Chữ luôn đứng cạnh chấm,
 * nên người mù màu đọc được đúng thứ người khác đọc.
 */
export default function Pill({
  children,
  mau,
  icon,
  chamMau,
}: {
  children: ReactNode
  /** Tông nền/chữ của viên thuốc. */
  mau?: 'trungTinh' | 'tim' | 'xanhLa' | 'vang' | 'do' | 'xanhDuong'
  /** Biểu tượng dẫn đầu, ví dụ ✨. */
  icon?: ReactNode
  /** Chấm tròn dẫn đầu — dùng cho thang có thứ tự như độ khó. */
  chamMau?: string
}) {
  return (
    <span className={`pill pill-${mau ?? 'trungTinh'}`}>
      {chamMau && <span className="pill-dot" style={{ background: chamMau }} aria-hidden />}
      {icon && <span aria-hidden>{icon}</span>}
      {children}
    </span>
  )
}
