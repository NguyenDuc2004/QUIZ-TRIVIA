import type { AttemptMode, AttemptStatus, GradedBy } from './api/attemptApi'

export const MODE_LABEL: Record<AttemptMode, string> = {
  PRACTICE: 'Luyện tập',
  EXAM: 'Thi',
}

export const MODE_HINT: Record<AttemptMode, string> = {
  PRACTICE: 'Trả lời câu nào chấm ngay câu đó, xem giải thích luôn. Không sửa lại câu đã chấm.',
  EXAM: 'Làm hết rồi mới nộp, chấm một lần cuối bài. Sửa đáp án thoải mái trước khi nộp.',
}

export const STATUS_LABEL: Record<AttemptStatus, string> = {
  IN_PROGRESS: 'Đang làm',
  SUBMITTED: 'Đã nộp',
  EXPIRED: 'Hết giờ',
}

export const STATUS_COLOR: Record<AttemptStatus, string | undefined> = {
  IN_PROGRESS: 'processing',
  SUBMITTED: 'green',
  EXPIRED: 'red',
}

export const GRADED_BY_LABEL: Record<GradedBy, string> = {
  NOT_GRADED: 'Chưa chấm',
  AUTO: 'Máy chấm',
  PENDING_AI: 'Chờ AI chấm',
  AI: 'AI chấm',
  AI_FAILED: 'AI chấm hỏng',
  HUMAN: 'Giáo viên chấm',
}

/** Đổi số giây thành dạng mm:ss (hoặc h:mm:ss khi dài hơn một giờ). */
export function formatDuration(totalSeconds: number | null | undefined): string {
  if (totalSeconds == null || totalSeconds < 0) return '—'

  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  const pad = (value: number) => String(value).padStart(2, '0')

  return hours > 0 ? `${hours}:${pad(minutes)}:${pad(seconds)}` : `${pad(minutes)}:${pad(seconds)}`
}
