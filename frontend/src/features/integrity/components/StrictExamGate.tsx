import { Alert, Button, Typography } from 'antd'
import { ExpandOutlined } from '@ant-design/icons'

const { Text, Paragraph } = Typography

/**
 * Cửa vào chế độ thi nghiêm ngặt (features/12, FR-48).
 *
 * ## Vì sao che nội dung đề chứ không chỉ hiện một cảnh báo
 * Một dải cảnh báo phía trên mà bên dưới vẫn đọc được đề thì chẳng ai bấm nút — chế độ nghiêm ngặt trở
 * thành một dòng chữ trang trí. Che đề đi mới làm cú bấm trở thành **bắt buộc về mặt thực tế**, dù trình
 * duyệt không cho ép về mặt kỹ thuật.
 *
 * ## Vì sao nói thẳng "vẫn thoát ra được"
 * Nói dối người học rằng họ *không thể* thoát là một lời hứa mà mọi người sẽ tự phát hiện là sai ngay lần
 * đầu bấm Esc — và khi đó họ kết luận cả cơ chế giám sát là trò đùa. Nói thật rằng *thoát được nhưng sẽ
 * bị ghi nhận* thì đúng sự thật, và vẫn đủ để người định gian lận cân nhắc.
 */
export default function StrictExamGate({
  dangToanManHinh,
  onVao,
}: {
  dangToanManHinh: boolean
  onVao: () => void
}) {
  if (dangToanManHinh) {
    return null
  }

  return (
    <div className="border border-line bg-white p-6 text-center">
      <ExpandOutlined className="text-ink-soft mb-3 text-3xl" />

      <Text className="mb-2 block text-lg font-bold!">Bài thi này yêu cầu chế độ toàn màn hình</Text>

      <Paragraph className="text-ink-soft mx-auto mb-4! max-w-xl">
        Người ra đề đã bật chế độ thi nghiêm ngặt. Bấm nút bên dưới để vào toàn màn hình và bắt đầu làm bài.
      </Paragraph>

      <Button type="primary" size="large" icon={<ExpandOutlined />} onClick={onVao}>
        Vào toàn màn hình và làm bài
      </Button>

      <Paragraph className="text-ink-soft mx-auto mt-4 mb-0! max-w-xl text-xs">
        Bạn <b>vẫn thoát ra được</b> bất cứ lúc nào bằng phím Esc — hệ thống không khoá được trình duyệt của
        bạn và không cố làm vậy. Nhưng mỗi lần thoát sẽ được ghi lại để người chấm xem khi cần.
      </Paragraph>
    </div>
  )
}

/**
 * Nhắc quay lại toàn màn hình khi người học đã thoát ra giữa chừng.
 *
 * Không che đề ở đây: bài đang làm dở, che đi là **phạt người bấm nhầm Esc** bằng cách chặn họ tiếp tục,
 * trong khi tín hiệu đã được ghi rồi. Nhắc là đủ.
 */
export function StrictExamReminder({ onVao }: { onVao: () => void }) {
  return (
    <Alert
      type="warning"
      showIcon
      message="Bạn đã thoát khỏi chế độ toàn màn hình"
      description={
        <div className="flex flex-col items-start gap-2">
          <Text className="text-sm">
            Lần thoát này đã được ghi lại. Bài làm của bạn vẫn giữ nguyên — hãy quay lại toàn màn hình để
            tiếp tục.
          </Text>
          <Button size="small" icon={<ExpandOutlined />} onClick={onVao}>
            Quay lại toàn màn hình
          </Button>
        </div>
      }
    />
  )
}
