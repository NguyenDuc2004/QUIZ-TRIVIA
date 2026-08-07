import { apiClient } from '@/shared/api/client'

/** Vì sao quiz này được gợi ý — quyết định câu chữ và nhóm hiển thị. */
export type RecommendationSource = 'WEAK_TOPIC' | 'SIMILAR_LEARNERS'

export interface RecommendedQuiz {
  quizId: string
  title: string
  source: RecommendationSource
  /** Lý do viết sẵn từ backend — hiện thẳng lên thẻ, frontend không tự chế lại. */
  reason: string
  weakTopics: string[]
  peerCount: number
  /** Số lượt làm thật, **không phải** điểm đánh giá — hệ thống chưa có tính năng đánh giá quiz. */
  attemptCount: number
}

export interface TopicMastery {
  topic: string
  correct: number
  total: number
  /** 0..1 */
  accuracy: number
  /** Ngưỡng "thế nào là yếu" do backend quyết định; frontend không tự đoán lại kẻo nói khác. */
  weak: boolean
  availableQuizzes: number
}

export interface LearningPath {
  topics: TopicMastery[]
  weakCount: number
  /** Lời nhắn khi chưa đủ dữ liệu; null khi lộ trình có nghĩa. */
  note: string | null
}

export const recommendApi = {
  quizzes: (limit = 8) =>
    apiClient
      .get<RecommendedQuiz[]>('/recommendations', { params: { limit } })
      .then((res) => res.data),

  path: () => apiClient.get<LearningPath>('/recommendations/path').then((res) => res.data),

  /** Dựng lại đồ thị từ lịch sử làm bài — cần cho bài đã làm trước khi có tính năng này. */
  rebuild: () =>
    apiClient
      .post<{ syncedAttempts: number }>('/recommendations/rebuild')
      .then((res) => res.data),
}
