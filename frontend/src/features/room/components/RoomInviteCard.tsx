import { QRCodeSVG } from 'qrcode.react'
import { Alert, Button, Tag, Typography, message } from 'antd'

const { Text, Title } = Typography

/** Địa chỉ chỉ chính máy này hiểu — điện thoại quét sẽ trỏ về chính nó. */
function isLocalOnly(url: string) {
  return /^https?:\/\/(localhost|127\.0\.0\.1|\[?::1\]?)(:|\/|$)/.test(url)
}

/**
 * Thẻ mời vào phòng: mã PIN 6 số cỡ lớn + mã QR.
 * <p>
 * <b>Đường dẫn trong QR do backend quyết định</b> ({@code room.joinUrl}), không phải lấy từ
 * {@code window.location.origin}. Lý do: origin phụ thuộc cách <i>host</i> mở trang — mở bằng
 * {@code localhost} (chuyện hoàn toàn tự nhiên khi dev) thì QR cũng mang {@code localhost} và điện
 * thoại quét sẽ trỏ về chính nó. Backend biết địa chỉ LAN thật của máy nên quyết định đúng.
 * <p>
 * QR vẫn vẽ ở client bằng {@code qrcode.react}: nội dung chỉ là một đường dẫn nên vẽ tại chỗ khỏi
 * truyền ảnh qua mạng, và là SVG nên chiếu máy chiếu không vỡ.
 */
export default function RoomInviteCard({
  roomCode,
  joinUrl,
  allowGuests,
  playerCount,
}: {
  roomCode: string
  /** Do backend dựng. Rỗng khi backend không dò được địa chỉ LAN nào (máy không nối mạng). */
  joinUrl: string
  allowGuests: boolean
  playerCount: number
}) {
  // Backend không dò được địa chỉ nào thì đành ghép với origin hiện tại
  const url = joinUrl?.trim() ? joinUrl : `${window.location.origin}/join/${roomCode}`
  const unreachableByPhone = isLocalOnly(url)

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(url)
      message.success('Đã sao chép đường dẫn vào phòng')
    } catch {
      // clipboard API cần HTTPS hoặc localhost; hiện thẳng link để người dùng tự chép
      message.info(url)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      {unreachableByPhone && (
        <Alert
          type="warning"
          showIcon
          message="Điện thoại sẽ không quét được mã QR này"
          description={
            <>
              Máy chủ không tìm được địa chỉ mạng LAN nào, nên đường dẫn đang là{' '}
              <Text code>{url}</Text> — trên điện thoại địa chỉ đó là chính chiếc điện thoại.
              <br />
              Kiểm tra máy đã nối Wi-Fi/LAN chưa, hoặc đặt biến môi trường{' '}
              <Text code>FRONTEND_BASE_URL</Text> rồi khởi động lại backend.
            </>
          }
        />
      )}

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
            <QRCodeSVG value={url} size={180} level="M" />
          </div>
          <Text className="text-ink-soft text-xs">Quét để vào phòng</Text>
          {/* Hiện thẳng đường dẫn: người dùng thấy ngay QR đang trỏ đi đâu */}
          <Text className="max-w-60 truncate text-center text-ink-soft text-xs" title={url}>
            {url}
          </Text>
          <Button size="small" onClick={copy}>
            Sao chép link
          </Button>
        </div>
      </div>
    </div>
  )
}
