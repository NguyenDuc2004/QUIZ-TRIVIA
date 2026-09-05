import { apiClient } from '@/shared/api/client'

/** Vì sao quiz này được gợi ý — quyết định câu chữ và nhóm hiển thị. */
export type RecommendationSource = 'WEAK_TOPIC' | 'SIMILAR_LEARNERS' | 'NEW_TOPIC'

export interface RecommendedQuiz {
  quizId: string
  title: string
  /** `null` khi quiz chưa có ảnh — vẽ ô trống cùng kích thước, **không** bịa ảnh thay thế. */
  thumbnailUrl: string | null
  source: RecommendationSource
  /** Lý do viết sẵn từ backend — hiện thẳng lên thẻ, frontend không tự chế lại. */
  reason: string
  weakTopics: string[]
  peerCount: number
  /** Số lượt làm thật, **không phải** điểm đánh giá — hệ thống chưa có tính năng đánh giá quiz. */
  attemptCount: number
  /**
   * Tên danh mục, dùng để chọn màu và biểu tượng khối bìa khi quiz chưa có ảnh.
   *
   * Backend nạp từ PostgreSQL cùng lúc với tiêu đề và ảnh bìa, không lấy từ đồ thị. Nhờ vậy cùng một
   * quiz ra **cùng một màu** ở lưới Khám phá và ở thẻ Gợi ý — thiếu trường này thì thẻ gợi ý phải đoán
   * màu từ tiêu đề và người dùng thấy hai thẻ khác màu cho cùng một quiz.
   */
  categoryName: string | null
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

export interface Recommendations {
  items: RecommendedQuiz[]
  /**
   * Lý do danh sách rỗng, do backend viết; `null` khi **có** gợi ý.
   *
   * Ba tình huống rỗng khác nhau hẳn (kho chưa có quiz / đã làm hết / chưa lấy được đồ thị) nên
   * frontend **không tự chế câu chữ** — nó không biết đang là tình huống nào.
   */
  note: string | null
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
      .get<Recommendations>('/recommendations', { params: { limit } })
      .then((res) => res.data),

  path: () => apiClient.get<LearningPath>('/recommendations/path').then((res) => res.data),

  /**
   * Nhờ AI giải thích vì sao quiz này được gợi ý (FR-36).
   *
   * CHỦ ĐỘNG bấm mới gọi. Gọi tự động cho cả danh sách là mười lời gọi mô hình cho một lần lướt, và từ
   * FR-84 thì chúng tiêu vào hạn mức AI của chính người học — họ bị phạt vì thứ họ không chủ động dùng.
   * Backend cache 24 giờ nên hỏi lại cùng một quiz không tốn thêm lượt.
   */
  explain: (quizId: string) =>
    apiClient
      .post<{ explanation: string }>(`/recommendations/${quizId}/explain`)
      .then((res) => res.data.explanation),

  /** Dựng lại đồ thị từ lịch sử làm bài — cần cho bài đã làm trước khi có tính năng này. */
  rebuild: () =>
    apiClient
      .post<{ syncedAttempts: number }>('/recommendations/rebuild')
      .then((res) => res.data),
}
