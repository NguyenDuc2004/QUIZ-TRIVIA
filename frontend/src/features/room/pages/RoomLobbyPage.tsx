import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Checkbox, Input, Select, Space, Typography, message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import PageHeader from '@/shared/components/PageHeader'
import { useQuizList } from '@/features/quiz/hooks/useQuizQueries'
import { roomApi } from '../api/roomApi'

const { Text, Paragraph } = Typography

const SECONDS_OPTIONS = [10, 15, 20, 30, 45, 60].map((value) => ({
  value,
  label: `${value} giây / câu`,
}))

/** Sảnh phòng đấu: mở phòng mới từ một quiz công khai, hoặc vào phòng bằng mã. */
export default function RoomLobbyPage() {
  const navigate = useNavigate()
  const { data: quizzes, isPending } = useQuizList({ size: 50 })

  const [quizId, setQuizId] = useState<string | undefined>()
  const [seconds, setSeconds] = useState(20)
  const [allowGuests, setAllowGuests] = useState(true)
  const [code, setCode] = useState('')
  const [creating, setCreating] = useState(false)
  const [joining, setJoining] = useState(false)

  const createRoom = async () => {
    if (!quizId) return
    setCreating(true)
    try {
      const room = await roomApi.create({ quizId, secondsPerQuestion: seconds, allowGuests })
      navigate(`/rooms/${room.roomCode}`)
    } catch (error) {
      message.error(getApiErrorMessage(error))
    } finally {
      setCreating(false)
    }
  }

  const joinRoom = async () => {
    const normalized = code.trim().toUpperCase()
    if (!normalized) return
    setJoining(true)
    try {
      await roomApi.join(normalized)
      navigate(`/rooms/${normalized}`)
    } catch (error) {
      message.error(getApiErrorMessage(error))
    } finally {
      setJoining(false)
    }
  }

  const playableQuizzes = (quizzes?.content ?? []).filter((quiz) => quiz.questionCount > 0)

  return (
    <Space direction="vertical" size="large" className="w-full">
      {/* Khối mở đầu của sảnh phòng đấu.

          Phòng đấu là một trong ba bộ mặt được phép rực (ui-design-system.md §1 và §4.1), nhưng sảnh
          vào phòng lại đang trông y hệt một trang quản lý: hai hộp trắng viền xám. Đây là màn hình
          người dùng đứng NGAY TRƯỚC lúc chơi, nên nó nên báo trước rằng thứ sắp tới là một trò chơi.

          Không bỏ `PageHeader` bên dưới: nó vẫn là tiêu đề trang thật, và khối này chỉ là phần mở đầu
          đặt trên nó. */}
      <div className="room-hero flex flex-col gap-1 p-6 sm:p-8">
        <div className="text-3xl font-bold text-white sm:text-4xl">Đấu trí cùng bạn bè</div>
        <div className="max-w-2xl text-sm text-white/85 sm:text-base">
          Mọi người nhận cùng một câu hỏi cùng lúc. Trả lời đúng và nhanh hơn thì điểm cao hơn, bảng xếp
          hạng cập nhật ngay sau mỗi câu.
        </div>
      </div>

      <PageHeader
        title="Phòng đấu trí"
        description="Mở phòng rồi chia sẻ mã cho bạn bè, hoặc nhập mã để vào phòng có sẵn."
      />

      <div className="grid gap-6 md:grid-cols-2">
        {/* Hai thẻ, hai việc ngược nhau — chủ phòng và người vào. Viền màu để phân biệt ngay ở khoảng
            cách một mét, thay vì phải đọc tiêu đề mới biết bên nào là bên nào. Chỉ tô viền và tiêu đề,
            KHÔNG tô nền: bên trong là biểu mẫu, và nền màu làm ô nhập với chữ khó đọc. */}
        <div className="room-card room-card-host bg-surface p-5">
          <Text className="room-card-title font-bold!">🎬 Mở phòng mới</Text>
          <Paragraph className="mt-1! mb-4! text-ink-soft text-xs">
            Bạn sẽ là chủ phòng: điều khiển lúc bắt đầu và chuyển câu.
          </Paragraph>

          <Space direction="vertical" size={12} className="w-full">
            <Select
              showSearch
              loading={isPending}
              placeholder="Chọn quiz"
              className="w-full"
              value={quizId}
              onChange={setQuizId}
              optionFilterProp="label"
              options={playableQuizzes.map((quiz) => ({
                value: quiz.id,
                label: `${quiz.title} · ${quiz.questionCount} câu`,
              }))}
            />
            <Select
              className="w-full"
              value={seconds}
              onChange={setSeconds}
              options={SECONDS_OPTIONS}
            />
            <Checkbox checked={allowGuests} onChange={(e) => setAllowGuests(e.target.checked)}>
              Cho phép khách vào bằng QR (không cần tài khoản)
            </Checkbox>

            <Button type="primary" block disabled={!quizId} loading={creating} onClick={createRoom}>
              Mở phòng
            </Button>
            {playableQuizzes.length === 0 && !isPending && (
              <Text className="text-ink-soft text-xs">
                Chưa có quiz công khai nào có câu hỏi để mở phòng.
              </Text>
            )}
          </Space>
        </div>

        <div className="room-card room-card-join bg-surface p-5">
          <Text className="room-card-title font-bold!">🔑 Vào phòng bằng mã</Text>
          <Paragraph className="mt-1! mb-4! text-ink-soft text-xs">
            Mã phòng gồm 6 chữ số, hoặc quét mã QR chủ phòng chiếu lên.
          </Paragraph>

          <Space direction="vertical" size={12} className="w-full">
            <Input
              size="large"
              placeholder="VD: 482913"
              maxLength={6}
              inputMode="numeric"
              value={code}
              // Mã phòng luôn viết hoa, tự chuyển để người dùng khỏi phải để ý
              // Mã PIN chỉ có chữ số — lọc luôn để dán nhầm cũng không sao
              onChange={(event) => setCode(event.target.value.replace(/[^0-9]/g, ''))}
              onPressEnter={joinRoom}
              className="text-center font-mono text-lg tracking-widest"
            />
            {/* `type="primary"`: đây là hành động chính của thẻ này, ngang hàng với "Mở phòng" ở
                thẻ bên cạnh. Để một bên nổi một bên chìm thì hai lựa chọn ngang nhau trông như một
                lựa chọn chính và một lựa chọn phụ. */}
            <Button type="primary" block loading={joining} disabled={!code.trim()} onClick={joinRoom}>
              Vào phòng
            </Button>
          </Space>
        </div>
      </div>
    </Space>
  )
}
