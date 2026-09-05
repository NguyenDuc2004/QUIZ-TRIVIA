import { Button, Dropdown } from 'antd'
import type { MenuProps } from 'antd'
import { MoreOutlined } from '@ant-design/icons'

/**
 * Cột thao tác của một hàng bảng: **một** hành động chính hiện rõ, phần còn lại gom vào menu ba chấm.
 *
 * ## Vì sao không dàn hàng ngang như trước
 * Bảng "Quiz của tôi" trước đây có sáu liên kết chữ nằm ngang trên mỗi hàng — *Soạn câu hỏi · Làm thử ·
 * Thống kê · Xuất · Sửa · Xóa*. Ba vấn đề, và cái thứ ba là nghiêm trọng nhất:
 *
 * 1. Sáu chữ chiếm 260px, ép cột nội dung — thứ người dùng thật sự đọc — hẹp lại.
 * 2. Trên màn hẹp chúng xuống dòng, một hàng bảng cao gấp ba hàng khác.
 * 3. **Xóa** — thao tác nguy hiểm nhất và không hoàn tác được — nằm ngang hàng với *Soạn câu hỏi*, cách
 *    nhau đúng vài chục pixel, và lại là chữ đỏ nên hút mắt nhất trong sáu chữ.
 *
 * Gom vào menu thì hành động phá hoại cần thêm một lần bấm mới chạm tới được, còn hành động hay dùng
 * nhất đứng riêng và rõ ràng.
 *
 * ## `stopPropagation`
 * Nhiều bảng cho bấm cả hàng để mở chi tiết. Không chặn nổi bọt thì bấm vào menu vừa mở menu vừa chuyển
 * trang.
 */
export default function RowActions({
  chinh,
  items,
}: {
  /** Hành động chính — hiện thẳng, dạng nút viền. */
  chinh?: React.ReactNode
  /** Các hành động phụ, gom vào menu ba chấm. */
  items: MenuProps['items']
}) {
  return (
    <div className="flex items-center justify-end gap-1" onClick={(e) => e.stopPropagation()}>
      {chinh}
      <Dropdown menu={{ items }} trigger={['click']} placement="bottomRight">
        <Button type="text" size="small" aria-label="Thao tác khác" icon={<MoreOutlined />} />
      </Dropdown>
    </div>
  )
}
