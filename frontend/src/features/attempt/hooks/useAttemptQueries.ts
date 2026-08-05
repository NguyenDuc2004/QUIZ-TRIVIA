import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '@/shared/api/client'
import { attemptApi, type AnswerBody, type AttemptMode } from '../api/attemptApi'

const ATTEMPT_KEY = 'attempts'

export function useAttempt(attemptId: string | undefined) {
  return useQuery({
    queryKey: [ATTEMPT_KEY, attemptId],
    queryFn: () => attemptApi.get(attemptId!),
    enabled: Boolean(attemptId),
    // Bài đang làm giữ trạng thái ở local (đáp án đang chọn), tự refetch sẽ ghi đè mất
    refetchOnWindowFocus: false,
  })
}

export function useAttemptHistory(params: { quizId?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: [ATTEMPT_KEY, 'history', params],
    queryFn: () => attemptApi.history(params),
  })
}

export function useLeaderboard(quizId: string | undefined) {
  return useQuery({
    queryKey: ['leaderboard', quizId],
    queryFn: () => attemptApi.leaderboard(quizId!),
    enabled: Boolean(quizId),
  })
}

/** Bắt đầu (hoặc làm tiếp) bài rồi chuyển thẳng sang màn làm bài. */
export function useStartAttempt() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ quizId, mode }: { quizId: string; mode: AttemptMode }) =>
      attemptApi.start(quizId, mode),
    onSuccess: (data) => {
      queryClient.setQueryData([ATTEMPT_KEY, data.attempt.id], data)
      navigate(`/attempts/${data.attempt.id}`)
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useAnswerQuestion(attemptId: string | undefined) {
  return useMutation({
    mutationFn: (body: AnswerBody) => attemptApi.answer(attemptId!, body),
    // Hết giờ / bài đã nộp đều trả 409 kèm thông điệp rõ ràng — hiện nguyên văn
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useSubmitAttempt(attemptId: string | undefined) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => attemptApi.submit(attemptId!),
    onSuccess: (data) => {
      queryClient.setQueryData([ATTEMPT_KEY, data.attempt.id], data)
      queryClient.invalidateQueries({ queryKey: [ATTEMPT_KEY, 'history'] })
      queryClient.invalidateQueries({ queryKey: ['leaderboard', data.attempt.quizId] })
      message.success(`Đã nộp bài — ${data.attempt.totalScore}/${data.attempt.maxScore} điểm`)
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}
