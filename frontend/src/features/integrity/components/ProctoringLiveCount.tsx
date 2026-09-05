import { Typography } from 'antd'
import type { SoLanGhiNhan } from '../hooks/useProctoring'

const { Text } = Typography

/**
 * Dòng đếm số tín hiệu đã ghi nhận, hiện ngay trên màn làm bài (features/12).
 *
 * ## Vì sao cần
 * Trước đây tín hiệu được thu hoàn toàn im lặng: người thi bị tính điểm rủi ro mà không biết mình đang bị
 * tính cái gì, và chỉ giáo viên mới thấy con số ấy sau khi bài đã nộp. Cho người thi thấy ngay có ba tác
 * dụng, theo thứ tự quan trọng:
 *
 * 1. **Công bằng** — bị đánh giá theo một tiêu chí không ai nói cho biết là bất công, kể cả khi tiêu chí đúng.
 * 2. **Ngăn ngừa hơn là bắt lỗi** — mục đích của cơ chế này là làm người ta không gian lận, chứ không phải
 *    bắt được nhiều người. Một con số thấy ngay có sức răn đe hơn hẳn một báo cáo sau bài mà người thi không
 *    bao giờ đọc.
 * 3. **Giảm báo động giả** — phần lớn tín hiệu là vô tình: thông báo bật lên, mở máy tính bỏ túi. Người thi
 *    thấy ngay thì tự điều chỉnh, nên điểm rủi ro cuối cùng phản ánh đúng hơn.
 *
 * ## Vì sao KHÔNG phải hộp cảnh báo đỏ
 * Ba lựa chọn trình bày dưới đây đều cố ý, và bỏ cái nào cũng làm hỏng mục đích:
 *
 * - **Không popup.** Một hộp nhảy ra giữa bài thi phá đúng thứ mà chế độ thi nghiêm ngặt đang bảo vệ: sự tập
 *   trung. Và nó làm người trung thực hoảng nhiều hơn người gian lận — người gian lận vốn đã biết mình làm gì.
 * - **Không màu đỏ.** Màu đỏ là ngôn ngữ của lỗi. Rời tab không phải lỗi.
 * - **Chữ "đã ghi nhận", không phải "vi phạm".** Đây là điểm quan trọng nhất. Toàn bộ thiết kế chống gian lận
 *   của hệ thống dựng trên nguyên tắc *hệ thống đưa dữ kiện, con người kết luận* — báo cáo cho giáo viên nói
 *   thẳng điều đó. Hiện chữ "bạn đang vi phạm" là hệ thống tự phán, mâu thuẫn với chính dòng chữ nó in ra ở
 *   phía bên kia. Rời trang một lần vì thông báo bật lên **không** phải gian lận, và chỉ giáo viên — người
 *   biết hoàn cảnh lớp mình — mới có tư cách kết luận điều đó.
 *
 * ## Không hiện khi chưa có gì
 * Một dòng "đã ghi nhận: 0 lần" thường trực là lời nhắc liên tục rằng người thi đang bị theo dõi, trong khi
 * họ chưa làm gì cả. Việc thông báo đang bị ghi nhận đã do `ProctoringNotice` đảm nhiệm.
 */
export default function ProctoringLiveCount({ soLan }: { soLan: SoLanGhiNhan }) {
  const phan = [
    soLan.roiTrang > 0 && `${soLan.roiTrang} lần rời trang`,
    soLan.dan > 0 && `${soLan.dan} lần dán nội dung`,
    soLan.thoatToanManHinh > 0 && `${soLan.thoatToanManHinh} lần thoát toàn màn hình`,
  ].filter(Boolean)

  if (phan.length === 0) {
    return null
  }

  return (
    <div className="border-line bg-surface-subtle border px-3 py-2">
      <Text className="text-ink-soft text-xs">
        Đã ghi nhận trong bài này: <b>{phan.join(' · ')}</b>. Đây là số liệu ghi nhận, không phải kết luận
        vi phạm — giáo viên là người xem xét.
      </Text>
    </div>
  )
}
