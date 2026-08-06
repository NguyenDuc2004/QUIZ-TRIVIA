import { QRCodeSVG } from 'qrcode.react'
import { Alert, Button, Tag, Typography, message } from 'antd'

const { Text, Title } = Typography

/** Địa chỉ mà chỉ chính máy này hiểu — điện thoại quét sẽ trỏ về chính nó và báo không tìm thấy. */
function isLocalOnly(hostname: string) {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1'
}

/**
 * Thẻ mời vào phòng: mã PIN 6 số cỡ lớn + mã QR.
 * <p>
 * QR vẽ ở <b>client</b> bằng {@code qrcode.react} chứ không xin ảnh từ server. Nội dung QR chỉ là
 * một đường dẫn, nên sinh ở đâu cũng cho kết quả như nhau — vẽ tại chỗ thì khỏi truyền ảnh qua
 * mạng, khỏi thêm thư viện vào backend, và co giãn theo màn hình vì là SVG.
 * <p>
 * QR lấy đúng địa chỉ đang mở ({@code window.location.origin}). Nếu host đang mở bằng
 * {@code localhost} thì QR cũng mã hoá {@code localhost} — điện thoại quét sẽ trỏ về chính nó và
 * báo không tìm thấy. Trường hợp đó thẻ này cảnh báo thẳng thay vì để người dùng tự đoán.
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
  const localOnly = isLocalOnly(window.location.hostname)

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
    <div className="flex flex-col gap-4">
      {localOnly && (
        <Alert
          type="warning"
          showIcon
          message="Điện thoại sẽ không quét được mã QR này"
          description={
            <>
              Bạn đang mở trang bằng <Text code>localhost</Text>, nên QR cũng trỏ về{' '}
              <Text code>localhost</Text> — trên điện thoại địa chỉ đó là chính chiếc điện thoại.
              <br />
              Hãy mở lại trang bằng <b>địa chỉ IP trong mạng LAN</b> của máy này (ví dụ{' '}
              <Text code>http://192.168.0.101:5173</Text>), rồi QR sẽ tự trỏ đúng. Điện thoại phải
              nối cùng Wi-Fi.
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
            <QRCodeSVG value={joinUrl} size={180} level="M" />
          </div>
          <Text className="text-ink-soft text-xs">Quét để vào phòng</Text>
          {/* Hiện thẳng đường dẫn: người dùng thấy ngay QR đang trỏ đi đâu */}
          <Text className="max-w-56 truncate text-center text-ink-soft text-xs" title={joinUrl}>
            {joinUrl}
          </Text>
          <Button size="small" onClick={copy}>
            Sao chép link
          </Button>
        </div>
      </div>
    </div>
  )
}
