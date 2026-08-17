import { apiClient } from '@/shared/api/client'
import type { ReviewStatus } from '@/features/integrity/api/integrityApi'

/** Một lượt làm bài trên đường tiến bộ. */
export interface AttemptScore {
  submittedAt: string
  quizTitle: string
  score: number
  maxScore: number
  percent: number
}

export interface LearnerProgress {
  totalAttempts: number
  distinctQuizzes: number
  /**
   * `null` = **chưa làm bài nào**, khác hẳn 0 (làm mà sai hết). Đừng thay bằng `?? 0` ở chỗ hiển
   * thị — làm vậy là nói sai về người chưa bắt đầu học.
   */
  averagePercent: number | null
  trend: AttemptScore[]
}

export interface ScoreBucket {
  fromPercent: number
  toPercent: number
  /** Nhãn viết sẵn từ backend, để trục biểu đồ và cách chia khoảng không lệch nhau. */
  label: string
  attemptCount: number
}

export interface HardQuestion {
  questionId: string
  content: string
  topic: string | null
  answeredCount: number
  wrongCount: number
  wrongPercent: number
}

export interface QuizStats {
  totalAttempts: number
  distinctLearners: number
  averagePercent: number | null
  /** Tỉ lệ nộp kịp giờ; phần còn lại là bài bị hết giờ. */
  completionPercent: number | null
  /** Luôn đủ 10 phần tử, kể cả khoảng chưa có ai đạt. */
  scoreDistribution: ScoreBucket[]
  hardestQuestions: HardQuestion[]
}

export interface QuizAttemptSummary {
  attemptId: string
  learnerName: string
  score: number
  maxScore: number
  submittedAt: string
  pendingAiCount: number
  failedAiCount: number
  needsManualGrading: boolean
  /**
   * Điểm rủi ro, **null khi bài không vượt ngưỡng gắn cờ** (features/12).
   *
   * Máy chủ cố ý không gửi điểm của bài dưới ngưỡng: gắn một con số "mức đáng ngờ" vào từng người học là mời
   * người ta xếp hạng học sinh theo độ nghi, và một điểm thấp không kèm lý do nào thì cũng không dùng được
   * vào việc gì. Nên `null` ở đây nghĩa là *không có gì đáng nói*, không phải *thiếu dữ liệu*.
   */
  riskScore: number | null
  /** Chỉ khác null khi `riskScore` khác null. */
  reviewStatus: ReviewStatus | null
}

/** Trạng thái chấm của một câu — quyết định con số `score` có nghĩa hay không. */
export type GradedBy = 'NOT_GRADED' | 'AUTO' | 'PENDING_AI' | 'AI' | 'AI_FAILED' | 'HUMAN'

export interface EssayAnswer {
  answerId: string
  orderIndex: number
  questionContent: string
  rubric: string | null
  sampleAnswer: string | null
  learnerAnswer: string | null
  /** Với `PENDING_AI`/`AI_FAILED` thì luôn là 0 và **không có nghĩa là bài sai** — xem `gradedBy`. */
  score: number
  maxScore: number
  gradedBy: GradedBy
  aiFeedback: string | null
  aiSuggestions: string | null
  needsGrading: boolean
}

export interface GradingView {
  attemptId: string
  quizId: string
  quizTitle: string
  learnerName: string
  submittedAt: string
  totalScore: number
  maxScore: number
  answers: EssayAnswer[]
}

export const analyticsApi = {
  myProgress: () => apiClient.get<LearnerProgress>('/analytics/me').then((res) => res.data),

  quizStats: (quizId: string) =>
    apiClient.get<QuizStats>(`/analytics/quizzes/${quizId}`).then((res) => res.data),

  quizAttempts: (quizId: string) =>
    apiClient
      .get<QuizAttemptSummary[]>(`/analytics/quizzes/${quizId}/attempts`)
      .then((res) => res.data),

  /** Bài làm nhìn từ phía người chấm — chỉ chủ quiz gọi được, và chỉ trả phần tự luận. */
  gradingView: (attemptId: string) =>
    apiClient.get<GradingView>(`/attempts/${attemptId}/grading`).then((res) => res.data),

  overrideGrade: (attemptId: string, answerId: string, score: number, feedback?: string) =>
    apiClient
      .patch(`/attempts/${attemptId}/answers/${answerId}/grade`, { score, feedback })
      .then((res) => res.data),
}
