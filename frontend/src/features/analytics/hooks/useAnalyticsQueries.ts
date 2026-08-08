import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { analyticsApi } from '../api/analyticsApi'

const ANALYTICS_KEY = 'analytics'

export function useMyProgress() {
  return useQuery({
    queryKey: [ANALYTICS_KEY, 'me'],
    queryFn: () => analyticsApi.myProgress(),
    staleTime: 60 * 1000,
  })
}

export function useQuizStats(quizId: string | undefined) {
  return useQuery({
    queryKey: [ANALYTICS_KEY, 'quiz', quizId],
    queryFn: () => analyticsApi.quizStats(quizId!),
    enabled: Boolean(quizId),
    staleTime: 60 * 1000,
  })
}

export function useQuizAttempts(quizId: string | undefined) {
  return useQuery({
    queryKey: [ANALYTICS_KEY, 'quiz', quizId, 'attempts'],
    queryFn: () => analyticsApi.quizAttempts(quizId!),
    enabled: Boolean(quizId),
  })
}

export function useGradingView(attemptId: string | undefined) {
  return useQuery({
    queryKey: [ANALYTICS_KEY, 'grading', attemptId],
    queryFn: () => analyticsApi.gradingView(attemptId!),
    enabled: Boolean(attemptId),
  })
}

/**
 * Chấm tay một câu.
 * <p>
 * Xoá cache của **cả nhánh** `analytics`: chấm xong thì bài đó đổi điểm, danh sách bài làm đổi cờ
 * "cần chấm", và bảng thống kê quiz đổi điểm trung bình lẫn phân bố. Chỉ làm mới màn đang mở thì
 * quay lại danh sách sẽ thấy số cũ, và người chấm tưởng thao tác không ăn.
 */
export function useOverrideGrade(attemptId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: { answerId: string; score: number; feedback?: string }) =>
      analyticsApi.overrideGrade(attemptId, input.answerId, input.score, input.feedback),
    onSuccess: () => {
      message.success('Đã lưu điểm')
      queryClient.invalidateQueries({ queryKey: [ANALYTICS_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}
