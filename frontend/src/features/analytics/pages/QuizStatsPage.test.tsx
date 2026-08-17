import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import QuizStatsPage from './QuizStatsPage'
import { analyticsApi, type QuizAttemptSummary, type QuizStats } from '../api/analyticsApi'

/**
 * Trang thống kê quiz là **đường vào duy nhất** để chủ quiz biết bài nào đáng rà soát (features/12, FR-47).
 * Trước khi có cột rủi ro ở đây, quyền xem báo cáo có mà đường đi tới thì không: một giáo viên 200 bài nộp
 * phải mở từng bài mới biết. Nên bốn điều dưới đây là bốn điều làm cho FR-47 đúng trên thực tế:
 *
 * 1. **Bài vượt ngưỡng phải nhìn thấy được ngay trong bảng**, không cần mở ra.
 * 2. **Bài bình thường không hiện con số nào** — máy chủ trả `null`, và giao diện hiện `—` chứ không hiện
 *    `0/100`. Gắn một điểm "mức đáng ngờ" vào từng người học là mời người ta xếp hạng học sinh theo độ nghi.
 * 3. **Dòng cảnh báo đếm theo bài CHƯA ai xem.** Đếm cả bài đã kết luận thì con số không bao giờ giảm và
 *    chủ quiz không biết còn bao nhiêu việc thật.
 * 4. **Cảnh báo luôn kèm câu "không phải bằng chứng"** — con số này không được xuất hiện một mình.
 */

function bai(ghiDe: Partial<QuizAttemptSummary> = {}): QuizAttemptSummary {
  return {
    attemptId: 'att-1',
    learnerName: 'Nguyễn Văn An',
    score: 8,
    maxScore: 10,
    submittedAt: '2026-08-17T10:00:00Z',
    pendingAiCount: 0,
    failedAiCount: 0,
    needsManualGrading: false,
    riskScore: null,
    reviewStatus: null,
    ...ghiDe,
  }
}

const THONG_KE: QuizStats = {
  totalAttempts: 3,
  distinctLearners: 3,
  averagePercent: 75,
  completionPercent: 100,
  scoreDistribution: Array.from({ length: 10 }, (_, i) => ({
    fromPercent: i * 10,
    toPercent: i * 10 + 10,
    label: `${i * 10}-${i * 10 + 10}%`,
    attemptCount: 0,
  })),
  hardestQuestions: [],
}

function hienThi(attempts: QuizAttemptSummary[]) {
  vi.spyOn(analyticsApi, 'quizStats').mockResolvedValue(THONG_KE)
  vi.spyOn(analyticsApi, 'quizAttempts').mockResolvedValue(attempts)

  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/my-quizzes/q1/stats']}>
        <Routes>
          <Route path="/my-quizzes/:id/stats" element={<QuizStatsPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('QuizStatsPage — cờ đáng rà soát', () => {
  it('bài vượt ngưỡng hiện điểm rủi ro ngay trong bảng', async () => {
    hienThi([bai({ riskScore: 98, reviewStatus: 'PENDING' })])

    expect(await screen.findByText('98/100')).toBeInTheDocument()
  })

  it('bài bình thường hiện dấu gạch, KHÔNG hiện 0/100', async () => {
    hienThi([bai()])

    expect(await screen.findByText('Nguyễn Văn An')).toBeInTheDocument()
    expect(screen.queryByText('0/100')).not.toBeInTheDocument()
    expect(screen.queryByText(/tín hiệu hành vi đáng xem/)).not.toBeInTheDocument()
  })

  it('dòng cảnh báo đếm ĐÚNG số bài chưa ai xem', async () => {
    hienThi([
      bai({ attemptId: 'a1', riskScore: 98, reviewStatus: 'PENDING' }),
      bai({ attemptId: 'a2', learnerName: 'Trần Thị Bích', riskScore: 70, reviewStatus: 'PENDING' }),
      // Bài này đã kết luận — không còn là việc phải làm, nên KHÔNG được tính vào con số
      bai({ attemptId: 'a3', learnerName: 'Lê Minh Cường', riskScore: 66, reviewStatus: 'VALID' }),
      bai({ attemptId: 'a4', learnerName: 'Phạm Thu Dung' }),
    ])

    expect(await screen.findByText('2 bài có nhiều tín hiệu hành vi đáng xem')).toBeInTheDocument()
  })

  it('cảnh báo nói rõ đây không phải bằng chứng gian lận', async () => {
    hienThi([bai({ riskScore: 98, reviewStatus: 'PENDING' })])

    expect(await screen.findByText(/không phải bằng chứng gian lận/)).toBeInTheDocument()
    expect(screen.getByText(/hệ thống không tự xử lý bài nào/)).toBeInTheDocument()
  })

  it('bài đã kết luận hiện rõ kết luận đó, không chỉ đổi màu', async () => {
    hienThi([bai({ riskScore: 66, reviewStatus: 'INVALID' })])

    expect(await screen.findByText('đã đánh dấu không hợp lệ')).toBeInTheDocument()
  })
})
