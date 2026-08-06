import { QRCodeSVG } from 'qrcode.react'
import { Button, Tag, Typography, message } from 'antd'

const { Text, Title } = Typography

/**
 * Thẻ mời vào phòng: mã PIN 6 số cỡ lớn + mã QR.
 * <p>
 * QR vẽ ở <b>client</b> bằng {@code qrcode.react} chứ không xin ảnh từ server. Nội dung QR chỉ là
 * một đường dẫn, nên sinh ở đâu cũng cho kết quả như nhau — vẽ tại chỗ thì khỏi truyền ảnh qua
 * mạng, khỏi thêm thư viện vào backend, và co giãn theo màn hình vì là SVG.
 * <p>
 * Dùng {@code window.location.origin} để QR trỏ đúng địa chỉ đang chạy: máy khác trong cùng mạng
 * LAN quét được mà không phải sửa cấu hình.
 */
export default function RoomInviteCard({
  roomCode,
  allowGuests,
  playerCount,
}: {
  roomCode: string
  allowGuests: boolean
  playerCount: number
}) {
  const joinUrl = `${window.location.origin}/join/${roomCode}`

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(joinUrl)
      message.success('Đã sao chép đường dẫn vào phòng')
    } catch {
      // clipboard API cần HTTPS hoặc localhost; hiện thẳng link để người dùng tự chép
      message.info(joinUrl)
    }
  }

  return (
    <div className="flex flex-col items-center gap-4 border border-line bg-white p-6 sm:flex-row sm:items-start sm:justify-center sm:gap-10">
      <div className="text-center">
        <Text className="text-ink-soft text-xs">Mã phòng</Text>
        <Title level={1} className="mt-1! mb-2! font-mono! tracking-[0.2em]">
          {roomCode}
        </Title>
        <Tag color={allowGuests ? 'green' : undefined} className="mr-0!">
          {allowGuests ? 'Khách vào được' : 'Cần đăng nhập'}
        </Tag>
        <Text className="mt-3 block text-ink-soft text-xs">{playerCount} người trong phòng</Text>
      </div>

      <div className="flex flex-col items-center gap-2">
        {/* Nền trắng + viền là bắt buộc để camera điện thoại bắt được mã */}
        <div className="border border-line bg-white p-2">
          <QRCodeSVG value={joinUrl} size={160} level="M" />
        </div>
        <Text className="text-ink-soft text-xs">Quét để vào phòng</Text>
        <Button size="small" onClick={copy}>
          Sao chép link
        </Button>
      </div>
    </div>
  )
}
