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
  | 'PROCTORING_FLAG'
  | 'PROCTORING_WARNING'

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
  /**
   * Đường dẫn mã QR trỏ tới, do BACKEND dựng từ địa chỉ LAN thật của máy.
   * Không tự ghép từ `window.location.origin`: host mở trang bằng localhost thì QR cũng mang
   * localhost, và điện thoại quét sẽ trỏ về chính nó.
   */
  joinUrl: string
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

/**
 * Cờ đỏ chống gian lận — server chỉ gửi cho host, qua kênh riêng `/user/queue/room/{code}`
 * (features/12, cảnh báo live).
 */
export interface ProctoringFlag {
  playerId: string
  displayName: string | null
  guest: boolean
  /** Số câu KHÁC NHAU có khuôn rời-rồi-về. Dùng làm khoá thay thế cờ cũ của cùng người chơi. */
  soCauLap: number
  /** Câu mô tả do server dựng — client KHÔNG tự ghép chuỗi từ con số. */
  lyDo: string
}

/** Lời nhắc host gửi riêng cho một người chơi. */
export interface ProctoringWarning {
  message: string
}

/** Một dòng trong bản tổng kết host xem sau ván. */
export interface RoomProctoringSummaryRow {
  playerId: string
  displayName: string | null
  guest: boolean
  /** Tổng số lần rời trang — chỉ để thấy độ ồn, KHÔNG phải căn cứ gắn cờ. */
  soLanRoiTrang: number
  /** Số câu có khuôn lặp — đây mới là căn cứ. */
  soCauLap: number
  biGanCo: boolean
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

  /** Tổng kết chống gian lận của phòng — server trả 403 cho người không phải host. */
  proctoring: (roomCode: string) =>
    apiClient
      .get<RoomProctoringSummaryRow[]>(`/rooms/${roomCode}/proctoring`)
      .then((res) => res.data),
}
