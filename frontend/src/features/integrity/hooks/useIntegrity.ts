import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { integrityApi, type ReviewStatus } from '../api/integrityApi'

const KEY = 'integrity'

export function useIntegrityReport(attemptId: string | undefined) {
  return useQuery({
    queryKey: [KEY, 'report', attemptId],
    queryFn: () => integrityApi.baoCao(attemptId!),
    enabled: Boolean(attemptId),
    // Lượt luyện tập không có báo cáo và trả 404 — đó là kết quả bình thường, không phải sự cố cần thử lại
    retry: false,
  })
}

export function useFlaggedAttempts(params: { status?: ReviewStatus; page?: number; size?: number }) {
  return useQuery({
    queryKey: [KEY, 'flagged', params],
    queryFn: () => integrityApi.danhSachGanCo(params),
  })
}

export function useReviewIntegrity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ attemptId, status, note }: {
      attemptId: string
      status: Exclude<ReviewStatus, 'PENDING'>
      note?: string
    }) => integrityApi.raSoat(attemptId, status, note),
    onSuccess: (report) => {
      message.success(
        report.reviewStatus === 'VALID'
          ? 'Đã đánh dấu bài hợp lệ'
          : 'Đã đánh dấu bài không hợp lệ',
      )
      queryClient.invalidateQueries({ queryKey: [KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}
