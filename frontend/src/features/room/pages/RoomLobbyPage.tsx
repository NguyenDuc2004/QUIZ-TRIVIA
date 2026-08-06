import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input, Select, Space, Typography, message } from 'antd'
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
  const [code, setCode] = useState('')
  const [creating, setCreating] = useState(false)
  const [joining, setJoining] = useState(false)

  const createRoom = async () => {
    if (!quizId) return
    setCreating(true)
    try {
      const room = await roomApi.create(quizId, seconds)
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
      <PageHeader
        title="Phòng đấu trí"
        description="Mở phòng rồi chia sẻ mã cho bạn bè, hoặc nhập mã để vào phòng có sẵn."
      />

      <div className="grid gap-6 md:grid-cols-2">
        <div className="border border-line bg-white p-5">
          <Text className="font-bold!">Mở phòng mới</Text>
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

        <div className="border border-line bg-white p-5">
          <Text className="font-bold!">Vào phòng bằng mã</Text>
          <Paragraph className="mt-1! mb-4! text-ink-soft text-xs">
            Mã phòng gồm 6 ký tự, chủ phòng chia sẻ cho bạn.
          </Paragraph>

          <Space direction="vertical" size={12} className="w-full">
            <Input
              size="large"
              placeholder="VD: 7NN2CW"
              maxLength={8}
              value={code}
              // Mã phòng luôn viết hoa, tự chuyển để người dùng khỏi phải để ý
              onChange={(event) => setCode(event.target.value.toUpperCase())}
              onPressEnter={joinRoom}
              className="text-center font-mono text-lg tracking-widest"
            />
            <Button block loading={joining} disabled={!code.trim()} onClick={joinRoom}>
              Vào phòng
            </Button>
          </Space>
        </div>
      </div>
    </Space>
  )
}
