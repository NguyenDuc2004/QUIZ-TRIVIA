import { apiClient } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/types'

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'
export type Visibility = 'PUBLIC' | 'PRIVATE'
export type QuestionType =
  | 'SINGLE_CHOICE'
  | 'MULTIPLE_CHOICE'
  | 'TRUE_FALSE'
  | 'FILL_BLANK'
  | 'SHORT_ANSWER'
export type QuestionSource = 'MANUAL' | 'AI_GENERATED'

export interface Category {
  id: string
  name: string
  slug: string
  description: string | null
}

/** Khớp QuizSummaryResponse của backend. */
export interface QuizSummary {
  id: string
  title: string
  description: string | null
  categoryId: string | null
  categoryName: string | null
  difficulty: Difficulty
  visibility: Visibility
  aiGenerated: boolean
  thumbnailUrl: string | null
  timeLimitSec: number | null
  /** Chế độ thi nghiêm ngặt (FR-48): yêu cầu toàn màn hình, khoá chuột phải. Chỉ áp cho lượt EXAM. */
  strictExam: boolean
  questionCount: number
  ownerId: string
  ownerDisplayName: string
  createdAt: string
}

export interface QuestionOption {
  id: string
  content: string
  correct: boolean
  orderIndex: number
}

/** Khớp QuestionResponse của backend (có đáp án đúng — chỉ chủ sở hữu nhận được). */
export interface Question {
  id: string
  type: QuestionType
  content: string
  explanation: string | null
  /** Ảnh minh hoạ (FR-11); null = câu hỏi chỉ có chữ. Đường dẫn nội bộ /uploads/… */
  imageUrl: string | null
  /** Tiêu chí chấm câu tự luận (features/06). */
  rubric: string | null
  difficulty: Difficulty
  topic: string | null
  points: number
  timeLimitSec: number | null
  source: QuestionSource
  options: QuestionOption[]
  createdAt: string
}

export interface QuizDetail {
  quiz: QuizSummary
  questions: Question[]
}

export interface QuizBody {
  title: string
  description?: string | null
  categoryId?: string | null
  difficulty?: Difficulty
  visibility?: Visibility
  timeLimitSec?: number | null
  strictExam?: boolean
  thumbnailUrl?: string | null
}

export interface QuestionBody {
  type: QuestionType
  content: string
  explanation?: string | null
  imageUrl?: string | null
  rubric?: string | null
  difficulty?: Difficulty
  topic?: string | null
  points?: number | null
  timeLimitSec?: number | null
  strictExam?: boolean
  options: { content: string; correct: boolean }[]
}

export interface QuizListParams {
  mine?: boolean
  categoryId?: string
  difficulty?: Difficulty
  q?: string
  page?: number
  size?: number
}

export interface QuestionListParams {
  type?: QuestionType
  difficulty?: Difficulty
  topic?: string
  q?: string
  page?: number
  size?: number
}

export const categoryApi = {
  list: () => apiClient.get<Category[]>('/categories').then((res) => res.data),
}

/** Định dạng file mang đi được (FR-12). Không có id, chủ sở hữu hay số liệu thống kê. */
export interface QuizPortableFile {
  formatVersion: number
  title: string
  description: string | null
  difficulty: Difficulty
  timeLimitSec: number | null
  questions: unknown[]
}

export const quizApi = {
  list: (params: QuizListParams) =>
    apiClient.get<PageResponse<QuizSummary>>('/quizzes', { params }).then((res) => res.data),

  get: (id: string) => apiClient.get<QuizSummary>(`/quizzes/${id}`).then((res) => res.data),

  getDetail: (id: string) =>
    apiClient.get<QuizDetail>(`/quizzes/${id}/questions`).then((res) => res.data),

  create: (body: QuizBody) => apiClient.post<QuizSummary>('/quizzes', body).then((res) => res.data),

  update: (id: string, body: QuizBody) =>
    apiClient.put<QuizSummary>(`/quizzes/${id}`, body).then((res) => res.data),

  remove: (id: string) => apiClient.delete<void>(`/quizzes/${id}`).then((res) => res.data),

  /** Thay toàn bộ danh sách câu hỏi — thứ tự mảng là thứ tự câu hỏi. */
  setQuestions: (id: string, questionIds: string[]) =>
    apiClient.put<QuizDetail>(`/quizzes/${id}/questions`, { questionIds }).then((res) => res.data),

  /** Xuất quiz ra file JSON (FR-12) — chỉ chủ quiz. */
  exportQuiz: (id: string) =>
    apiClient.get<QuizPortableFile>(`/quizzes/${id}/export`).then((res) => res.data),

  /**
   * Nhập quiz từ file JSON (FR-12).
   *
   * LUÔN tạo quiz mới và luôn để riêng tư — không bao giờ ghi đè quiz có sẵn.
   */
  importQuiz: (file: QuizPortableFile) =>
    apiClient.post<QuizSummary>('/quizzes/import', file).then((res) => res.data),
}

/** Một chủ đề trong ngân hàng, kèm số câu đang mang chủ đề đó. */
export interface Topic {
  topic: string
  questionCount: number
}

export const questionApi = {
  list: (params: QuestionListParams) =>
    apiClient.get<PageResponse<Question>>('/questions', { params }).then((res) => res.data),

  /** Chủ đề đã dùng trong ngân hàng — để lọc và để gợi ý lúc soạn câu mới. */
  listTopics: () => apiClient.get<Topic[]>('/questions/topics').then((res) => res.data),

  create: (body: QuestionBody) =>
    apiClient.post<Question>('/questions', body).then((res) => res.data),

  update: (id: string, body: QuestionBody) =>
    apiClient.put<Question>(`/questions/${id}`, body).then((res) => res.data),

  remove: (id: string) => apiClient.delete<void>(`/questions/${id}`).then((res) => res.data),

}
