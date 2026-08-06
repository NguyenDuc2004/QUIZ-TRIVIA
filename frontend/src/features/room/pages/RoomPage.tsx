import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Alert, Button, Checkbox, Input, Radio, Skeleton, Space, Tag, Typography, message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import PageHeader from '@/shared/components/PageHeader'
import { useAuthStore } from '@/features/auth/store/authStore'
import { QUESTION_TYPE_LABEL } from '@/features/quiz/constants'
import {
  roomApi,
  type AnswerResult,
  type GameEvent,
  type LiveQuestion,
  type QuestionClosed,
  type RoomPlayer,
  type RoomView,
} from '../api/roomApi'
import RoomCountdown from '../components/RoomCountdown'
import RoomLeaderboard from '../components/RoomLeaderboard'
import { useRoomSocket } from '../hooks/useRoomSocket'

const { Text, Paragraph, Title } = Typography

/**
 * Một phòng đấu — cùng một trang phục vụ cả ba giai đoạn (chờ / đang chơi / kết thúc),
 * phân biệt bằng `room.status`. Trạng thái ban đầu lấy từ REST, sau đó cập nhật theo sự kiện
 * WebSocket; nối lại sau khi rớt mạng thì gọi lại REST để đồng bộ (FR-25).
 */
export default function RoomPage() {
  const { code: roomCode } = useParams<{ code: string }>()
  const currentUser = useAuthStore((state) => state.user)

  const [room, setRoom] = useState<RoomView | null>(null)
  const [loadError, setLoadError] = useState<unknown>(null)
  const [question, setQuestion] = useState<LiveQuestion | null>(null)
  const [players, setPlayers] = useState<RoomPlayer[]>([])
  const [progress, setProgress] = useState({ answeredCount: 0, totalPlayers: 0 })
  const [selected, setSelected] = useState<string[]>([])
  const [text, setText] = useState('')
  const [myResult, setMyResult] = useState<AnswerResult | null>(null)
  const [closed, setClosed] = useState<QuestionClosed | null>(null)
  const [finalRanking, setFinalRanking] = useState<RoomPlayer[] | null>(null)

  /** Nạp/đồng bộ lại toàn bộ trạng thái phòng từ REST. */
  const sync = useCallback(async () => {
    if (!roomCode) return
    try {
      const view = await roomApi.get(roomCode)
      setRoom(view)
      setPlayers(view.players)
      setQuestion(view.currentQuestion)
      setProgress({ answeredCount: view.answeredCount, totalPlayers: view.players.length })
    } catch (error) {
      setLoadError(error)
    }
  }, [roomCode])

  useEffect(() => {
    void sync()
  }, [sync])

  const handleEvent = useCallback((event: GameEvent) => {
    switch (event.type) {
      case 'PLAYER_JOINED':
      case 'PLAYER_LEFT': {
        const data = event.data as { players: RoomPlayer[] }
        setPlayers(data.players)
        setProgress((prev) => ({ ...prev, totalPlayers: data.players.length }))
        break
      }
      case 'GAME_STARTED':
        setRoom((prev) => (prev ? { ...prev, status: 'PLAYING' } : prev))
        setFinalRanking(null)
        break
      case 'QUESTION': {
        // Câu mới: xoá sạch mọi thứ của câu cũ để không hiện nhầm đáp án
        setQuestion(event.data as LiveQuestion)
        setSelected([])
        setText('')
        setMyResult(null)
        setClosed(null)
        setProgress((prev) => ({ ...prev, answeredCount: 0 }))
        break
      }
      case 'PLAYER_ANSWERED':
        setProgress(event.data as { answeredCount: number; totalPlayers: number })
        break
      case 'QUESTION_CLOSED':
        setClosed(event.data as QuestionClosed)
        break
      case 'LEADERBOARD':
        setPlayers(event.data as RoomPlayer[])
        break
      case 'GAME_FINISHED':
        setFinalRanking(event.data as RoomPlayer[])
        setRoom((prev) => (prev ? { ...prev, status: 'FINISHED' } : prev))
        setQuestion(null)
        break
      default:
        break
    }
  }, [])

  const { status: socketStatus, send } = useRoomSocket({
    roomCode,
    onEvent: handleEvent,
    onPrivateEvent: (event) => {
      if (event.type === 'ANSWER_RESULT') {
        setMyResult(event.data as AnswerResult)
      }
    },
    onError: (error) => message.error(error.message),
  })

  // Nối lại sau khi rớt mạng: sự kiện lỡ mất không được phát lại nên phải đồng bộ bằng REST
  useEffect(() => {
    if (socketStatus === 'connected') {
      void sync()
    }
  }, [socketStatus, sync])

  if (loadError) {
    return <Alert type="error" showIcon message={getApiErrorMessage(loadError)} />
  }
  if (!room) {
    return <Skeleton active paragraph={{ rows: 6 }} />
  }

  const isHost = currentUser?.id === room.hostId
  const answered = myResult !== null
  const canAnswer = room.status === 'PLAYING' && question !== null && !answered && !closed
  const isChoice = question ? ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(question.type) : false

  const submitAnswer = () => {
    if (!question) return
    send('answer', {
      questionId: question.questionId,
      optionIds: isChoice ? selected : undefined,
      text: isChoice ? undefined : text,
    })
  }

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title={room.quizTitle}
        description={
          <Space size={8} wrap>
            <Link to="/rooms" className="font-bold">
              ← Sảnh phòng đấu
            </Link>
            <span className="text-ink-soft">·</span>
            <Text className="text-ink-soft">Chủ phòng: {room.hostDisplayName}</Text>
            {socketStatus !== 'connected' && (
              <Tag color={socketStatus === 'connecting' ? 'processing' : 'red'} className="mr-0!">
                {socketStatus === 'connecting' ? 'Đang kết nối…' : 'Mất kết nối — đang thử lại'}
              </Tag>
            )}
          </Space>
        }
        actions={
          isHost && room.status === 'WAITING' ? (
            <Button type="primary" disabled={socketStatus !== 'connected'} onClick={() => send('start')}>
              Bắt đầu ván
            </Button>
          ) : isHost && room.status === 'PLAYING' ? (
            <Button type="primary" onClick={() => send('next')}>
              {question && question.index >= question.total - 1 ? 'Kết thúc ván' : 'Câu tiếp theo'}
            </Button>
          ) : null
        }
      />

      <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
        <div className="flex flex-col gap-4">
          {room.status === 'WAITING' && (
            <div className="border border-line bg-white p-8 text-center">
              <Text className="text-ink-soft text-xs">Mã phòng — chia sẻ cho bạn bè</Text>
              <Title level={1} className="mt-2! mb-4! font-mono! tracking-widest">
                {room.roomCode}
              </Title>
              <Paragraph className="mb-0! text-ink-soft">
                {players.length} người đang chờ · {room.totalQuestions} câu hỏi
              </Paragraph>
              {!isHost && (
                <Text className="mt-2 block text-ink-soft text-xs">
                  Chờ chủ phòng bắt đầu ván.
                </Text>
              )}
            </div>
          )}

          {room.status === 'PLAYING' && question && (
            <div className="border border-line bg-white p-5">
              <div className="mb-3 flex flex-wrap items-center gap-2">
                <Text className="text-ink-soft text-xs font-bold">
                  Câu {question.index + 1}/{question.total}
                </Text>
                <Tag className="mr-0!">{QUESTION_TYPE_LABEL[question.type]}</Tag>
                <Text className="ml-auto text-xs font-bold">
                  {progress.answeredCount}/{progress.totalPlayers} đã trả lời
                </Text>
              </div>

              <RoomCountdown
                deadlineAtMillis={question.deadlineAtMillis}
                totalSeconds={question.timeLimitSec}
              />

              <Paragraph className="mt-4! mb-4! text-lg font-bold!">{question.content}</Paragraph>

              {isChoice ? (
                question.type === 'MULTIPLE_CHOICE' ? (
                  <Checkbox.Group
                    disabled={!canAnswer}
                    value={selected}
                    onChange={(values) => setSelected(values as string[])}
                    className="w-full"
                  >
                    <Space direction="vertical" size={8} className="w-full">
                      {question.options.map((option) => (
                        <Checkbox
                          key={option.id}
                          value={option.id}
                          className={`w-full border p-3 ${optionTone(option.id, closed, selected)}`}
                        >
                          {option.content}
                        </Checkbox>
                      ))}
                    </Space>
                  </Checkbox.Group>
                ) : (
                  <Radio.Group
                    disabled={!canAnswer}
                    value={selected[0]}
                    onChange={(event) => setSelected([event.target.value])}
                    className="w-full"
                  >
                    <Space direction="vertical" size={8} className="w-full">
                      {question.options.map((option) => (
                        <Radio
                          key={option.id}
                          value={option.id}
                          className={`w-full border p-3 ${optionTone(option.id, closed, selected)}`}
                        >
                          {option.content}
                        </Radio>
                      ))}
                    </Space>
                  </Radio.Group>
                )
              ) : (
                <Input
                  size="large"
                  disabled={!canAnswer}
                  placeholder="Nhập đáp án"
                  value={text}
                  onChange={(event) => setText(event.target.value)}
                  onPressEnter={submitAnswer}
                />
              )}

              <div className="mt-4 flex items-center gap-3">
                <Button
                  type="primary"
                  disabled={!canAnswer || (isChoice ? selected.length === 0 : !text.trim())}
                  onClick={submitAnswer}
                >
                  Gửi đáp án
                </Button>

                {myResult && (
                  <Tag color={myResult.correct ? 'green' : 'red'} className="mr-0!">
                    {myResult.correct
                      ? `Đúng · +${myResult.points} điểm (${(myResult.elapsedMillis / 1000).toFixed(1)}s)`
                      : 'Sai · 0 điểm'}
                  </Tag>
                )}
                {!myResult && !canAnswer && !closed && (
                  <Text className="text-ink-soft text-xs">Hết giờ trả lời câu này.</Text>
                )}
              </div>

              {closed?.explanation && (
                <div className="mt-4 border-l-2 border-brand bg-surface-subtle p-3">
                  <Text className="text-ink-soft text-xs font-bold">Giải thích</Text>
                  <Paragraph className="mb-0! whitespace-pre-wrap">{closed.explanation}</Paragraph>
                </div>
              )}
            </div>
          )}

          {room.status === 'FINISHED' && (
            <div className="border border-line bg-white p-8 text-center">
              <Title level={3} className="mb-2!">
                Ván đấu kết thúc
              </Title>
              <Paragraph className="mb-4! text-ink-soft">
                {(finalRanking ?? players)[0]?.displayName
                  ? `Người thắng: ${(finalRanking ?? players)[0].displayName}`
                  : 'Không có người chơi nào'}
              </Paragraph>
              <Link to="/rooms">
                <Button type="primary">Về sảnh phòng đấu</Button>
              </Link>
            </div>
          )}
        </div>

        <aside className="h-fit lg:sticky lg:top-24">
          <RoomLeaderboard
            players={finalRanking ?? players}
            currentUserId={currentUser?.id}
            title={room.status === 'FINISHED' ? 'Kết quả chung cuộc' : 'Bảng xếp hạng'}
          />
        </aside>
      </div>
    </Space>
  )
}

/** Tô màu lựa chọn: chỉ tô sau khi câu đã đóng, trước đó tô là lộ đáp án. */
function optionTone(optionId: string, closed: QuestionClosed | null, selected: string[]): string {
  if (!closed) {
    return 'border-line'
  }
  if (closed.correctOptionIds.includes(optionId)) {
    return 'border-green-500 bg-green-50'
  }
  return selected.includes(optionId) ? 'border-red-500 bg-red-50' : 'border-line'
}
