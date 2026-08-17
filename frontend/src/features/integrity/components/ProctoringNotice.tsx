import { Alert, Typography } from 'antd'
import { EyeOutlined } from '@ant-design/icons'

const { Text } = Typography

/**
 * Thông báo cho người thi biết bài đang được ghi nhận tín hiệu hành vi (features/12).
 *
 * **Đây không phải một chi tiết trang trí — nó là ràng buộc của đặc tả.** Thu tín hiệu hành vi mà không nói
 * với người bị thu là làm sau lưng họ. Nên thông báo này:
 * - hiện **ngay trên màn làm bài**, không ẩn trong điều khoản hay tooltip;
 * - nói **đúng những gì được ghi** và **đúng những gì không được ghi**;
 * - nói rõ hệ thống **không tự kết luận** — tín hiệu chỉ để giáo viên rà soát.
 *
 * Không hiện với lượt luyện tập, vì luyện tập không thu gì cả.
 */
export default function ProctoringNotice() {
  return (
    <Alert
      type="info"
      showIcon
      icon={<EyeOutlined />}
      message="Bài thi này có ghi nhận một số tín hiệu hành vi"
      description={
        <div className="flex flex-col gap-1">
          <Text className="text-sm">
            Hệ thống ghi nhận: <b>số lần</b> bạn chuyển tab hoặc rời cửa sổ, <b>số lần</b> sao chép và dán,
            và <b>độ dài</b> đoạn văn bản được dán.
          </Text>
          <Text className="text-sm">
            Hệ thống <b>không</b> ghi hình, <b>không</b> ghi âm, <b>không</b> đọc nội dung bạn dán và{' '}
            <b>không</b> theo dõi gì ngoài phạm vi trang làm bài này.
          </Text>
          <Text className="text-ink-soft text-xs">
            Các tín hiệu này chỉ giúp giáo viên biết bài nào nên xem lại. Hệ thống không tự động kết luận hay
            xử lý bài nào — mọi kết luận đều do người thật đưa ra. Chế độ luyện tập không ghi nhận gì.
          </Text>
        </div>
      }
    />
  )
}
