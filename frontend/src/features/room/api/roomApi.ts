import { apiClient } from '@/shared/api/client'
import type { QuestionType } from '@/features/quiz/api/quizApi'

export type RoomStatus = 'WAITING' | 'PLAYING' | 'FINISHED'

export type GameEventType =
  | 'PLAYER_JOINED'
  | 'PLAYER_LEFT'
  | 'PLAYER_READY'
  | 'PLAYER_AVATAR_CHANGED'
  | 'GAME_STARTED'
  | 'QUESTION'
  | 'PLAYER_ANSWERED'
  | 'ANSWER_RESULT'
  | 'QUESTION_CLOSED'
  | 'LEADERBOARD'
  | 'GAME_FINISHED'

export interface RoomPlayer {
  rank: number
  playerId: string
  displayName: string
  guest: boolean
  ready: boolean
  avatar: string | null
  /** Emoji và màu đi kèm sẵn để vẽ ngay, không phải tra bảng avatar. */
  avatarEmoji: string | null
  avatarColor: string | null
  score: number
  correctCount: number
}

export interface AvatarOption {
  code: string
  emoji: string
  color: string
}

/** Câu hỏi phát trong phòng — backend không kèm đáp án đúng ở sự kiện này. */
export interface LiveQuestion {
  questionId: string
  index: number
  total: number
  type: QuestionType
  content: string
  points: number
  timeLimitSec: number
  /** Mốc hết giờ theo đồng hồ server (epoch millis). */
  deadlineAtMillis: number
  options: { id: string; content: string }[]
}

export interface RoomView {
  roomCode: string
  quizId: string
  quizTitle: string
  hostId: string
  hostDisplayName: string
  status: RoomStatus
  allowGuests: boolean
  totalQuestions: number
  readyCount: number
  players: RoomPlayer[]
  currentQuestion: LiveQuestion | null
  answeredCount: number
}

export interface AnswerResult {
  questionId: string
  correct: boolean
  points: number
  totalScore: number
  elapsedMillis: number
}

export interface QuestionClosed {
  questionId: string
  correctOptionIds: string[]
  explanation: string | null
  leaderboard: RoomPlayer[]
}

/** Gói chung mọi thông điệp server đẩy xuống — phân nhánh theo `type`. */
export interface GameEvent<T = unknown> {
  type: GameEventType
  at: string
  data: T
}

export interface GuestSessionResponse {
  guestKey: string
  /** Client cần để biết thẻ nào trong phòng chờ là mình — hai khách có thể trùng biệt danh. */
  playerId: string
  room: RoomView
}

export const roomApi = {
  create: (body: { quizId: string; secondsPerQuestion?: number; allowGuests: boolean }) =>
    apiClient.post<RoomView>('/rooms', body).then((res) => res.data),

  join: (roomCode: string) =>
    apiClient.post<RoomView>(`/rooms/${roomCode}/join`).then((res) => res.data),

  /** Khách vãng lai vào phòng — chỉ được khi host bật cho phép khách. */
  joinAsGuest: (roomCode: string, displayName: string, avatar?: string) =>
    apiClient
      .post<GuestSessionResponse>(`/rooms/${roomCode}/join-as-guest`, { displayName, avatar })
      .then((res) => res.data),

  /** Cũng là đường phục hồi sau khi mất kết nối (FR-25). Mở cho khách. */
  get: (roomCode: string) => apiClient.get<RoomView>(`/rooms/${roomCode}`).then((res) => res.data),

  avatars: () => apiClient.get<AvatarOption[]>('/rooms/avatars').then((res) => res.data),

  leave: (roomCode: string) =>
    apiClient.delete<void>(`/rooms/${roomCode}/players/me`).then((res) => res.data),
}
