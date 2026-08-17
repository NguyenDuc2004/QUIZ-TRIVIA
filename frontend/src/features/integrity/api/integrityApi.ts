import { apiClient } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/types'

/** Sáu loại tín hiệu server chấp nhận. Khớp `ProctoringEventType` của backend. */
export type ProctoringEventType =
  | 'TAB_HIDDEN'
  | 'WINDOW_BLUR'
  | 'COPY'
  | 'PASTE'
  | 'FULLSCREEN_EXIT'
  | 'ANSWER_TOO_FAST'

/**
 * Một tín hiệu gửi lên.
 *
 * `length` là **độ dài** đoạn đã dán, không phải nội dung. Client không gửi nội dung, và server cũng không
 * nhận — nó dựng lại `detail` từ danh sách trường vô hại. Hai lớp cùng bảo đảm một điều.
 */
export interface ProctoringEvent {
  type: ProctoringEventType
  occurredAt: string
  length?: number
  seconds?: number
}

export type ReviewStatus = 'PENDING' | 'VALID' | 'INVALID'

export interface IntegrityReport {
  attemptId: string
  tenQuiz: string
  tenNguoiLam: string
  /** 0–100. KHÔNG phải xác suất gian lận — là tổng có trọng số của các tín hiệu. */
  riskScore: number
  biGanCo: boolean
  /** Lý do cụ thể. Đây là thứ người rà soát thật sự đọc, không phải con số. */
  flags: string[]
  /** null = chưa gọi mô hình hoặc gọi thất bại; khác chuỗi rỗng. */
  aiNote: string | null
  reviewStatus: ReviewStatus
  reviewedAt: string | null
  reviewNote: string | null
  soSuKien: number
  suKien: { type: string; detail: string | null; occurredAt: string }[]
  /** Câu nhắc server luôn trả kèm: tín hiệu giả mạo được, đây không phải bằng chứng. */
  canhBao: string
}

export const integrityApi = {
  /** Gửi lô tín hiệu. Server từ chối nếu lượt không phải chế độ thi. */
  guiSuKien: (attemptId: string, events: ProctoringEvent[]) =>
    apiClient.post<{ soSuKienDaGhi: number }>(`/attempts/${attemptId}/proctoring-events`, { events })
      .then((res) => res.data),

  baoCao: (attemptId: string) =>
    apiClient.get<IntegrityReport>(`/attempts/${attemptId}/integrity`).then((res) => res.data),

  raSoat: (attemptId: string, status: Exclude<ReviewStatus, 'PENDING'>, note?: string) =>
    apiClient.put<IntegrityReport>(`/attempts/${attemptId}/integrity/review`, { status, note })
      .then((res) => res.data),

  danhSachGanCo: (params: { status?: ReviewStatus; page?: number; size?: number }) =>
    apiClient.get<PageResponse<IntegrityReport>>('/admin/integrity/flagged', { params })
      .then((res) => res.data),
}
