import { Alert, Button, Tag, Typography } from 'antd'
import type { ProctoringFlag } from '../api/roomApi'

const { Text } = Typography

/**
 * Bảng cờ đỏ chống gian lận, **chỉ hiện trên màn hình host** (features/12, cảnh báo live).
 *
 * ## Vì sao câu giải thích ở đây không phải để trang trí
 * Host có ba giây giữa lúc đang điều hành ván. Nếu bảng này chỉ hiện tên kèm một cờ đỏ, thứ host đọc được là
 * *"người này gian lận"* — trong khi tín hiệu đến từ trình duyệt của chính người bị nghi, tức chặn được và
 * giả mạo được, và vẫn còn cách giải thích vô hại. Nên dòng cảnh báo nói rõ đây là **dữ kiện, không phải kết
 * luận** đứng trước danh sách, không nằm dưới.
 *
 * ## Chỉ có một nút, và đó là chủ ý
 * Không có "trừ điểm", không có "đuổi khỏi phòng". Một thông báo bật lên → cờ đỏ → người chơi bị loại khỏi
 * cuộc thi tính điểm, không hoàn tác được, không được nói gì. Nhắc thì đủ để người định gian lận biết mình
 * đang bị thấy, mà không phạt oan ai. Xem docs/features/12-anti-cheat.md.
 */
export default function ProctoringFlagPanel({
  flags,
  daNhac,
  onNhac,
}: {
  flags: ProctoringFlag[]
  /** Những playerId host đã nhắc — nhắc lại lần nữa không thêm thông tin gì cho người nhận. */
  daNhac: string[]
  onNhac: (playerId: string) => void
}) {
  if (flags.length === 0) {
    return null
  }

  return (
    <div className="border border-line bg-white p-4">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <Text className="font-bold!">Tín hiệu cần để ý ({flags.length})</Text>
        <Tag className="mr-0!">chỉ bạn thấy bảng này</Tag>
      </div>

      <Alert
        type="warning"
        showIcon
        className="mb-3"
        message="Đây là dữ kiện, không phải kết luận"
        description={
          'Tín hiệu do trình duyệt của người chơi gửi lên nên có thể bị chặn hoặc làm sai lệch, và vẫn có ' +
          'cách giải thích vô hại. Bạn nhắc riêng được; hệ thống không trừ điểm và không loại ai.'
        }
      />

      <div className="flex flex-col gap-2">
        {flags.map((flag) => {
          const nhacRoi = daNhac.includes(flag.playerId)
          return (
            <div
              key={flag.playerId}
              className="flex flex-wrap items-center gap-2 border border-line bg-surface-subtle p-3"
            >
              <div className="min-w-40 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <Text className="font-bold!">{flag.displayName ?? 'Người chơi'}</Text>
                  {flag.guest && <Tag className="mr-0!">khách</Tag>}
                </div>
                {/* Lý do do server dựng — client không tự ghép chuỗi từ con số, vì mức nghiêm khắc và cách
                    diễn đạt thuộc phần quyết định chứ không thuộc phần hiển thị */}
                <Text className="text-ink-soft block text-xs">{flag.lyDo}</Text>
              </div>

              <Button size="small" disabled={nhacRoi} onClick={() => onNhac(flag.playerId)}>
                {nhacRoi ? 'Đã nhắc' : 'Nhắc riêng'}
              </Button>
            </div>
          )
        })}
      </div>
    </div>
  )
}
