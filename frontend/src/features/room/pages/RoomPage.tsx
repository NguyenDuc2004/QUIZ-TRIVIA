import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Alert,
  Button,
  Checkbox,
  Input,
  Radio,
  Skeleton,
  Space,
  Tag,
  Typography,
  message,
} from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { useAuthStore } from '@/features/auth/store/authStore'
import { QUESTION_TYPE_LABEL } from '@/features/quiz/constants'
import {
  roomApi,
  type AnswerResult,
  type AvatarOption,
  type GameEvent,
  type LiveQuestion,
  type QuestionClosed,
  type RoomPlayer,
  type RoomView,
} from '../api/roomApi'
import { guestSession } from '../api/guestSession'
import AvatarPicker from '../components/AvatarPicker'
import LobbyPlayerGrid from '../components/LobbyPlayerGrid'
import RoomCountdown from '../components/RoomCountdown'
import RoomInviteCard from '../components/RoomInviteCard'
import RoomLeaderboard from '../components/RoomLeaderboard'
import { useRoomSocket } from '../hooks/useRoomSocket'

const { Text, Paragraph, Title } = Typography

/**
 * Một phòng đấu — cùng một trang phục vụ cả ba giai đoạn (chờ / đang chơi / kết thúc),
 * phân biệt bằng `room.status`.
 * <p>
 * Trang này <b>công khai</b> vì khách vãng lai quét QR cũng vào đây. Danh tính lấy từ tài khoản
 * đăng nhập, hoặc từ phiên khách lưu trong `sessionStorage`.
 */
export default function RoomPage() {
  const { code: roomCode } = useParams<{ code: string }>()
  const currentUser = useAuthStore((state) => state.user)
  const guest = roomCode ? guestSession.get(roomCode) : null
  const currentPlayerId = guest?.playerId ?? currentUser?.id

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
  const [avatars, setAvatars] = useState<AvatarOption[]>([])

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
    roomApi.avatars().then(setAvatars).catch(() => undefined)
  }, [sync])

  const handleEvent = useCallback((event: GameEvent) => {
    switch (event.type) {
      case 'PLAYER_JOINED':
      case 'PLAYER_LEFT':
      case 'PLAYER_READY':
      case 'PLAYER_AVATAR_CHANGED': {
        const data = event.data as { players: RoomPlayer[]; readyCount: number }
        setPlayers(data.players)
        setRoom((prev) => (prev ? { ...prev, readyCount: data.readyCount } : prev))
        setProgress((prev) => ({ ...prev, totalPlayers: data.players.length }))
        break
      }
      case 'GAME_STARTED':
        setRoom((prev) => (prev ? { ...prev, status: 'PLAYING' } : prev))
        setFinalRanking(null)
        break
      case 'QUESTION':
        // Câu mới: xoá sạch mọi thứ của câu cũ để không hiện nhầm đáp án
        setQuestion(event.data as LiveQuestion)
        setSelected([])
        setText('')
        setMyResult(null)
        setClosed(null)
        setProgress((prev) => ({ ...prev, answeredCount: 0 }))
        break
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
    return (
      <div className="mx-auto max-w-2xl p-6">
        <Alert type="error" showIcon message={getApiErrorMessage(loadError)} />
      </div>
    )
  }
  if (!room) {
    return (
      <div className="mx-auto max-w-4xl p-6">
        <Skeleton active paragraph={{ rows: 8 }} />
      </div>
    )
  }

  const isHost = currentPlayerId === room.hostId
  const me = players.find((player) => player.playerId === currentPlayerId)
  const answered = myResult !== null
  const canAnswer = room.status === 'PLAYING' && question !== null && !answered && !closed
  const isChoice = question
    ? ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(question.type)
    : false

  const submitAnswer = () => {
    if (!question) return
    send('answer', {
      questionId: question.questionId,
      optionIds: isChoice ? selected : undefined,
      text: isChoice ? undefined : text,
    })
  }

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6 p-4 sm:p-6">
      <header className="flex flex-wrap items-center gap-3">
        <Link to="/quizzes" className="flex items-center gap-1">
          <span className="text-lg font-extrabold text-ink">Quiz</span>
          <span className="text-lg font-extrabold text-brand">AI</span>
        </Link>
        <Title level={4} className="mb-0!">
          {room.quizTitle}
        </Title>
        <Text className="text-ink-soft text-xs">Chủ phòng: {room.hostDisplayName}</Text>

        {socketStatus !== 'connected' && (
          <Tag color={socketStatus === 'connecting' ? 'processing' : 'red'} className="mr-0!">
            {socketStatus === 'connecting' ? 'Đang kết nối…' : 'Mất kết nối — đang thử lại'}
          </Tag>
        )}

        <div className="ml-auto flex gap-2">
          {isHost && room.status === 'WAITING' && (
            <Button
              type="primary"
              disabled={socketStatus !== 'connected'}
              onClick={() => send('start')}
            >
              Bắt đầu ván
            </Button>
          )}
          {isHost && room.status === 'PLAYING' && (
            <Button type="primary" onClick={() => send('next')}>
              {question && question.index >= question.total - 1 ? 'Kết thúc ván' : 'Câu tiếp theo'}
            </Button>
          )}
        </div>
      </header>

      {room.status === 'WAITING' && (
        <>
          <RoomInviteCard
            roomCode={room.roomCode}
            allowGuests={room.allowGuests}
            playerCount={players.length}
          />

          <div className="border border-line bg-white p-5">
            <div className="mb-4 flex flex-wrap items-center gap-3">
              <Text className="font-bold!">
                Người chơi ({players.length}) · {room.readyCount} đã sẵn sàng
              </Text>
              <Button
                type={me?.ready ? 'default' : 'primary'}
                className="ml-auto"
                disabled={socketStatus !== 'connected'}
                onClick={() => send('ready', { ready: !me?.ready })}
              >
                {me?.ready ? 'Bỏ sẵn sàng' : 'Tôi đã sẵn sàng'}
              </Button>
            </div>

            <LobbyPlayerGrid
              players={players}
              hostId={room.hostId}
              currentPlayerId={currentPlayerId}
            />

            <div className="mt-5 border-t border-line pt-4">
              <AvatarPicker
                avatars={avatars}
                value={me?.avatar ?? undefined}
                label="Đổi nhân vật của bạn"
                onChange={(avatar) => send('avatar', { avatar })}
              />
            </div>
          </div>
        </>
      )}

      {room.status === 'PLAYING' && question && (
        <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
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

          <aside className="h-fit lg:sticky lg:top-6">
            <RoomLeaderboard players={players} currentPlayerId={currentPlayerId} />
          </aside>
        </div>
      )}

      {room.status === 'FINISHED' && (
        <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
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

          <aside className="h-fit">
            <RoomLeaderboard
              players={finalRanking ?? players}
              currentPlayerId={currentPlayerId}
              title="Kết quả chung cuộc"
            />
          </aside>
        </div>
      )}
    </div>
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
