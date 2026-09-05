import { Button, Tooltip } from 'antd'
import { MoonOutlined, SunOutlined } from '@ant-design/icons'
import { quyDoi, useThemeStore } from '@/shared/theme/themeStore'

/**
 * Nút đổi giao diện sáng / tối.
 *
 * ## Một lần bấm, không phải menu ba lớp
 * Bản đầu đặt lựa chọn này thành menu con trong menu tài khoản: bấm avatar → *Giao diện* → *Tối*. Ba
 * lần bấm cho một thao tác người dùng làm thường xuyên, và nó nằm ở chỗ không ai nghĩ tới. Nút này
 * đứng thẳng trên thanh điều hướng và đổi ngay khi bấm.
 *
 * ## Icon cho biết BẤM SẼ RA GÌ, không phải đang ở đâu
 * Đang sáng thì hiện 🌙 (bấm để sang tối), đang tối thì hiện ☀️. Đây là quy ước ngược trực giác nhưng
 * đúng với cách người dùng đọc một nút: nút mô tả *hành động*, không mô tả *trạng thái*. Chú thích khi
 * rê chuột nói rõ bằng chữ để không ai phải đoán.
 *
 * ## Vẫn giữ ba trạng thái ở menu tài khoản
 * Nút này chỉ lật giữa Sáng và Tối — đủ cho việc dùng hằng ngày. Trạng thái thứ ba *"theo hệ thống"*
 * không diễn tả được bằng một nút hai chiều, nên nó nằm ở menu tài khoản. Ai chưa từng bấm nút này thì
 * vẫn đang ở chế độ theo hệ thống, đúng như mặc định.
 */
export default function ThemeToggle({ className }: { className?: string }) {
  const cheDo = useThemeStore((s) => s.cheDo)
  const datCheDo = useThemeStore((s) => s.datCheDo)

  // Lật theo chế độ ĐANG THẤY, không theo giá trị đã lưu: người ở "theo hệ thống" mà máy đang tối thì
  // bấm nút phải ra sáng. So sánh với chuỗi 'system' rồi mặc định sang tối sẽ cho kết quả ngược.
  const dangToi = quyDoi(cheDo) === 'dark'

  return (
    <Tooltip title={dangToi ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'}>
      <Button
        type="text"
        aria-label={dangToi ? 'Chuyển sang giao diện sáng' : 'Chuyển sang giao diện tối'}
        className={className}
        icon={dangToi ? <SunOutlined /> : <MoonOutlined />}
        onClick={() => datCheDo(dangToi ? 'light' : 'dark')}
      />
    </Tooltip>
  )
}
