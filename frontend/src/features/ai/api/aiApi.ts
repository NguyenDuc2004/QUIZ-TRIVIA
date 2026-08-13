import { apiClient } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/types'
import type { Difficulty, QuestionType } from '@/features/quiz/api/quizApi'

export type MaterialStatus = 'PROCESSING' | 'READY' | 'FAILED'
export type MaterialSourceType = 'PDF' | 'DOCX' | 'TXT' | 'TEXT'
export type AiJobStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'

export interface Material {
  id: string
  title: string
  topic: string | null
  sourceType: MaterialSourceType
  status: MaterialStatus
  fileUrl: string | null
  charCount: number
  chunkCount: number
  errorMessage: string | null
  /** Cho người học hỏi trợ lý AI trên tài liệu này hay chưa (features/08). */
  shared: boolean
  createdAt: string
}

/** Câu hỏi AI sinh ra — vẫn là bản nháp, chưa nằm trong ngân hàng câu hỏi. */
export interface GeneratedQuestion {
  type: QuestionType
  content: string
  options: { content: string; correct: boolean }[]
  explanation: string
  difficulty: Difficulty
  topic: string | null
  sourceExcerpt: string | null
}

export interface GenerationResult {
  questions: GeneratedQuestion[]
  /** Lý do những câu bị loại — hiện ra để người dùng biết vì sao xin 10 câu mà chỉ nhận 7. */
  rejected: string[]
  sourceExcerpts: string[]
  provider: string
  model: string
  latencyMs: number
}

export interface AiJob {
  id: string
  type: 'INGEST_MATERIAL' | 'GENERATE_QUESTIONS'
  status: AiJobStatus
  result: GenerationResult | null
  errorMessage: string | null
  startedAt: string | null
  finishedAt: string | null
  createdAt: string
  /** Số giây còn phải chờ vì nhà cung cấp AI đang chặn hạn mức; 0 = chạy bình thường. */
  aiThrottledSeconds: number
}

export interface GenerateRequest {
  topic?: string
  count: number
  types?: QuestionType[]
  difficulty?: Difficulty
  materialId?: string
  useMaterials: boolean
}

export const aiApi = {
  /** Có provider nào cấu hình key chưa — giao diện báo trước thay vì để bấm rồi mới lỗi. */
  status: () =>
    apiClient
      .get<{ available: boolean; providers: string[] }>('/ai/status')
      .then((res) => res.data),

  listMaterials: (params: { page?: number; size?: number }) =>
    apiClient.get<PageResponse<Material>>('/ai/materials', { params }).then((res) => res.data),

  getMaterial: (id: string) => apiClient.get<Material>(`/ai/materials/${id}`).then((res) => res.data),

  createMaterial: (body: { title: string; topic?: string; content: string }) =>
    apiClient.post<Material>('/ai/materials', body).then((res) => res.data),

  uploadMaterial: (file: File, title?: string, topic?: string) => {
    const form = new FormData()
    form.append('file', file)
    if (title) form.append('title', title)
    if (topic) form.append('topic', topic)

    // Để axios tự đặt Content-Type kèm boundary của multipart
    return apiClient
      .post<Material>('/ai/materials/upload', form, { headers: { 'Content-Type': undefined } })
      .then((res) => res.data)
  },

  /** Bật/tắt chia sẻ học liệu cho người học. Tài liệu chưa xử lý xong trả 409. */
  setMaterialShared: (id: string, shared: boolean) =>
    apiClient
      .patch<Material>(`/ai/materials/${id}/shared`, null, { params: { shared } })
      .then((res) => res.data),

  deleteMaterial: (id: string) => apiClient.delete<void>(`/ai/materials/${id}`).then((res) => res.data),

  generate: (body: GenerateRequest) =>
    apiClient.post<AiJob>('/ai/generate-questions', body).then((res) => res.data),

  getJob: (id: string) => apiClient.get<AiJob>(`/ai/jobs/${id}`).then((res) => res.data),

  approve: (jobId: string, indexes: number[]) =>
    apiClient.post(`/ai/jobs/${jobId}/approve`, { indexes }).then((res) => res.data),
}
