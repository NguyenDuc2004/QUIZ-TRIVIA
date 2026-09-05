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
 *
 * ## Hai chỗ dùng, hai mức chi tiết
 * - **Trang giới thiệu quiz** (`compact` tắt) — bản đầy đủ, đọc TRƯỚC khi đồng hồ chạy. Đây mới là nơi thông
 *   báo có tác dụng thật: người thi còn kịp đóng bớt tab, chọn chỗ ngồi yên tĩnh, hoặc quyết định lùi lại.
 * - **Màn làm bài** (`compact` bật) — bản một dòng, chỉ để nhắc rằng cơ chế đang chạy. Lặp lại nguyên văn cả
 *   đoạn dài ở đây chiếm chỗ của đề bài, và người thi vừa đọc nó xong ở bước trước.
 *
 * Vẫn giữ bản rút gọn thay vì bỏ hẳn: người làm tiếp một bài đang dở vào thẳng màn làm bài, không đi qua
 * trang giới thiệu, nên bỏ hẳn thì đúng nhóm người đó không được nhắc gì.
 */
export default function ProctoringNotice({ compact = false }: { compact?: boolean } = {}) {
  if (compact) {
    return (
      <Alert
        type="info"
        showIcon
        icon={<EyeOutlined />}
        message={
          <Text className="text-sm">
            Bài thi đang ghi nhận số lần rời trang, sao chép và dán — <b>không</b> ghi hình, <b>không</b> đọc
            nội dung. Hệ thống không tự kết luận.
          </Text>
        }
      />
    )
  }

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
