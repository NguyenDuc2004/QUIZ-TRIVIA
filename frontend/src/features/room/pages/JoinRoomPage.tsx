import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Alert, Button, Input, Skeleton, Space, Typography, message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { useAuthStore } from '@/features/auth/store/authStore'
import { roomApi, type AvatarOption, type RoomView } from '../api/roomApi'
import { guestSession } from '../api/guestSession'
import AvatarPicker from '../components/AvatarPicker'
import ThemeToggle from '@/shared/components/ThemeToggle'

const { Text, Paragraph, Title } = Typography

/**
 * Màn hình sau khi quét QR: `/join/:code`.
 * <p>
 * Đây là trang <b>công khai</b> — người quét QR chưa chắc có tài khoản. Tuỳ theo phòng có bật cho
 * phép khách hay không mà hiện lối vào bằng biệt danh hoặc yêu cầu đăng nhập.
 * <p>
 * Người đã đăng nhập thì bỏ qua bước này, vào thẳng phòng.
 */
export default function JoinRoomPage() {
  const { code } = useParams<{ code: string }>()
  const navigate = useNavigate()
  const isAuthenticated = Boolean(useAuthStore((state) => state.user))

  const [room, setRoom] = useState<RoomView | null>(null)
  const [avatars, setAvatars] = useState<AvatarOption[]>([])
  const [displayName, setDisplayName] = useState('')
  const [avatar, setAvatar] = useState<string | undefined>()
  const [loadError, setLoadError] = useState<string | null>(null)
  const [joining, setJoining] = useState(false)

  useEffect(() => {
    if (!code) return

    // Đã đăng nhập thì không cần chọn biệt danh, vào thẳng phòng
    if (isAuthenticated) {
      roomApi
        .join(code)
        .then(() => navigate(`/rooms/${code}`, { replace: true }))
        .catch((error) => setLoadError(getApiErrorMessage(error)))
      return
    }

    Promise.all([roomApi.get(code), roomApi.avatars()])
      .then(([roomView, avatarList]) => {
        setRoom(roomView)
        setAvatars(avatarList)
        setAvatar(avatarList[Math.floor(Math.random() * avatarList.length)]?.code)
      })
      .catch((error) => setLoadError(getApiErrorMessage(error)))
  }, [code, isAuthenticated, navigate])

  const joinAsGuest = async () => {
    if (!code) return
    setJoining(true)
    try {
      const session = await roomApi.joinAsGuest(code, displayName.trim(), avatar)
      guestSession.save(code, { guestKey: session.guestKey, playerId: session.playerId })
      navigate(`/rooms/${code}`, { replace: true })
    } catch (error) {
      message.error(getApiErrorMessage(error))
    } finally {
      setJoining(false)
    }
  }

  if (loadError) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-surface-subtle p-4">
        <div className="w-full max-w-md soft-panel p-8">
          <Alert type="error" showIcon message={loadError} />
          <Link to="/rooms">
            {/* Lối ra duy nhất của màn báo lỗi. Để nó chìm thì người dùng đọc xong lỗi rồi không
                thấy rõ phải bấm đâu. */}
            <Button type="primary" className="mt-4" block>
              Về sảnh phòng đấu
            </Button>
          </Link>
        </div>
      </div>
    )
  }

  if (isAuthenticated || !room) {
    return (
      <div className="mx-auto max-w-md p-8">
        <Skeleton active paragraph={{ rows: 4 }} />
      </div>
    )
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-surface-subtle p-4">
      {/* Nút đổi giao diện cho trang khách.

          Bốn trang này nằm NGOÀI cả hai layout nên không có thanh điều hướng, tức trước bản này người
          chưa đăng nhập không có đường nào đổi giao diện — và trang đăng nhập lại đúng là trang đầu
          tiên họ thấy. Đặt ở góc trên phải, `absolute` để không đẩy khối nội dung đang căn giữa. */}
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>
      <div className="w-full max-w-md soft-panel p-8">
        <div className="mb-1 flex items-center justify-center gap-1">
          <span className="text-2xl font-extrabold text-ink">Quiz</span>
          <span className="text-2xl font-extrabold text-brand">AI</span>
        </div>

        <Title level={4} className="mb-1! text-center! font-bold!">
          {room.quizTitle}
        </Title>
        <Paragraph className="mb-6! text-center text-ink-soft text-xs">
          Mã phòng <span className="font-mono font-bold tracking-widest">{room.roomCode}</span> ·{' '}
          {room.players.length} người đang chờ
        </Paragraph>

        {room.status !== 'WAITING' && (
          <Alert
            className="mb-4"
            type="warning"
            showIcon
            message={room.status === 'PLAYING' ? 'Ván đấu đã bắt đầu' : 'Ván đấu đã kết thúc'}
          />
        )}

        {room.allowGuests ? (
          <Space direction="vertical" size={16} className="w-full">
            <div>
              <Text className="text-ink-soft text-xs font-bold">Biệt danh của bạn</Text>
              <Input
                size="large"
                className="mt-2"
                maxLength={30}
                placeholder="Ví dụ: Bé Bút Chì"
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                onPressEnter={joinAsGuest}
              />
            </div>

            <AvatarPicker avatars={avatars} value={avatar} onChange={setAvatar} />

            <Button
              type="primary"
              size="large"
              block
              loading={joining}
              disabled={!displayName.trim() || room.status === 'FINISHED'}
              onClick={joinAsGuest}
            >
              Vào phòng
            </Button>

            <Text className="block text-center text-ink-soft text-xs">
              Chơi với tư cách khách — không cần tài khoản. Điểm chỉ tính trong ván này.
            </Text>
          </Space>
        ) : (
          <Space direction="vertical" size={12} className="w-full">
            <Alert
              type="info"
              showIcon
              message="Phòng này yêu cầu đăng nhập"
              description="Chủ phòng không bật chế độ cho khách. Đăng nhập rồi bạn sẽ được đưa vào phòng."
            />
            <Link to={`/login?next=${encodeURIComponent(`/join/${room.roomCode}`)}`}>
              <Button type="primary" size="large" block>
                Đăng nhập rồi vào phòng
              </Button>
            </Link>
          </Space>
        )}
      </div>
    </div>
  )
}
