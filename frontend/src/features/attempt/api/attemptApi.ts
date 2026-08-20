import { apiClient } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/types'
import type { Difficulty, QuestionType } from '@/features/quiz/api/quizApi'

export type AttemptMode = 'PRACTICE' | 'EXAM'
export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'EXPIRED'
export type GradedBy = 'NOT_GRADED' | 'AUTO' | 'PENDING_AI' | 'AI' | 'AI_FAILED' | 'HUMAN'

/** Nội dung trả lời: trắc nghiệm dùng optionIds, điền khuyết/tự luận dùng text. */
export interface AnswerPayload {
  optionIds?: string[] | null
  text?: string | null
}

export interface AttemptSummary {
  id: string
  quizId: string
  quizTitle: string
  mode: AttemptMode
  /**
   * Chế độ thi nghiêm ngặt CÓ ÁP CHO LƯỢT NÀY hay không (FR-48).
   * Server đã tính sẵn `quiz.strictExam && mode === 'EXAM'` — client KHÔNG tự nhân lại, xem
   * AttemptSummaryResponse ở backend.
   */
  strictExam: boolean
  status: AttemptStatus
  startedAt: string
  expiresAt: string | null
  submittedAt: string | null
  totalScore: number
  maxScore: number
  questionCount: number
  answeredCount: number
  correctCount: number
  durationSec: number | null
}

/**
 * Câu hỏi trong bài làm. Khi bài chưa nộp, backend để null toàn bộ trường lộ đáp án
 * (correctOptionIds, explanation, correct, score) — đừng dựng UI dựa vào chúng lúc đang làm.
 */
export interface AttemptQuestion {
  /** Id dòng câu trả lời — cần cho ghi đè điểm và xin giải thích. */
  answerId: string
  questionId: string
  orderIndex: number
  type: QuestionType
  content: string
  difficulty: Difficulty
  maxScore: number
  timeLimitSec: number | null
  options: { id: string; content: string }[]
  userAnswer: AnswerPayload | null
  correctOptionIds: string[] | null
  explanation: string | null
  correct: boolean | null
  score: number | null
  gradedBy: GradedBy | null
  aiFeedback: string | null
  aiSuggestions: string | null
}

export interface AttemptDetail {
  attempt: AttemptSummary
  questions: AttemptQuestion[]
  /**
   * Số câu tự luận AI còn đang chấm (features/06). Lớn hơn 0 nghĩa là tổng điểm đang hiển thị mới
   * là điểm **tạm** — màn kết quả phải nói rõ và hỏi lại, đừng để người học tưởng mình mất điểm.
   */
  gradingPending: number
  /**
   * Số giây còn phải chờ vì nhà cung cấp AI đang chặn hạn mức; 0 = đang chạy bình thường.
   * Dùng để nói thật với người học thay vì để họ nhìn vòng quay đứng yên mà đoán.
   */
  aiThrottledSeconds: number
}

export interface AnswerFeedback {
  questionId: string
  userAnswer: AnswerPayload
  answeredCount: number
  questionCount: number
  correct: boolean | null
  score: number | null
  correctOptionIds: string[] | null
  explanation: string | null
}

export interface LeaderboardEntry {
  rank: number
  userId: string
  displayName: string
  totalScore: number
  maxScore: number
  durationSec: number | null
  submittedAt: string
}

export interface ExplanationResponse {
  explanation: string
}

export interface AnswerBody {
  questionId: string
  optionIds?: string[]
  text?: string
}

export const attemptApi = {
  /** Bắt đầu (hoặc làm tiếp) một bài trên quiz. */
  start: (quizId: string, mode: AttemptMode) =>
    apiClient.post<AttemptDetail>(`/quizzes/${quizId}/attempts`, { mode }).then((res) => res.data),

  get: (attemptId: string) =>
    apiClient.get<AttemptDetail>(`/attempts/${attemptId}`).then((res) => res.data),

  answer: (attemptId: string, body: AnswerBody) =>
    apiClient.post<AnswerFeedback>(`/attempts/${attemptId}/answers`, body).then((res) => res.data),

  submit: (attemptId: string) =>
    apiClient.post<AttemptDetail>(`/attempts/${attemptId}/submit`).then((res) => res.data),

  history: (params: { quizId?: string; page?: number; size?: number }) =>
    apiClient.get<PageResponse<AttemptSummary>>('/attempts', { params }).then((res) => res.data),

  leaderboard: (quizId: string) =>
    apiClient.get<LeaderboardEntry[]>(`/quizzes/${quizId}/leaderboard`).then((res) => res.data),

  /** Nhờ AI giải thích một câu trong bài đã nộp (FR-30). */
  explain: (attemptId: string, answerId: string) =>
    apiClient
      .post<ExplanationResponse>(`/attempts/${attemptId}/answers/${answerId}/explain`)
      .then((res) => res.data),

  /** Chủ quiz chấm tay, ghi đè điểm AI (FR-30). */
  overrideGrade: (attemptId: string, answerId: string, body: { score: number; feedback?: string }) =>
    apiClient
      .patch<AttemptDetail>(`/attempts/${attemptId}/answers/${answerId}/grade`, body)
      .then((res) => res.data),
}
