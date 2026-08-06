import { apiClient } from '@/shared/api/client'
import type { QuestionType } from '@/features/quiz/api/quizApi'

export type RoomStatus = 'WAITING' | 'PLAYING' | 'FINISHED'

export type GameEventType =
  | 'PLAYER_JOINED'
  | 'PLAYER_LEFT'
  | 'GAME_STARTED'
  | 'QUESTION'
  | 'PLAYER_ANSWERED'
  | 'ANSWER_RESULT'
  | 'QUESTION_CLOSED'
  | 'LEADERBOARD'
  | 'GAME_FINISHED'

export interface RoomPlayer {
  rank: number
  userId: string
  displayName: string
  score: number
  correctCount: number
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
  totalQuestions: number
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

export const roomApi = {
  create: (quizId: string, secondsPerQuestion?: number) =>
    apiClient
      .post<RoomView>('/rooms', { quizId, secondsPerQuestion })
      .then((res) => res.data),

  join: (roomCode: string) =>
    apiClient.post<RoomView>(`/rooms/${roomCode}/join`).then((res) => res.data),

  /** Cũng là đường phục hồi sau khi mất kết nối (FR-25). */
  get: (roomCode: string) => apiClient.get<RoomView>(`/rooms/${roomCode}`).then((res) => res.data),

  leave: (roomCode: string) =>
    apiClient.delete<void>(`/rooms/${roomCode}/players/me`).then((res) => res.data),
}
