import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { getApiErrorMessage } from '@/shared/api/client'
import { attemptApi, type AnswerBody, type AttemptMode } from '../api/attemptApi'

const ATTEMPT_KEY = 'attempts'

/** Nhịp hỏi lại khi AI còn đang chấm — đủ nhanh để thấy điểm nhảy, đủ chậm để không dội API. */
const GRADING_POLL_MS = 3000

/**
 * Nhịp hỏi lại khi biết chắc AI đang bị chặn hạn mức.
 * <p>
 * Hỏi mỗi 3 giây trong lúc chờ cả phút là gọi hai chục lần vô ích. Giãn ra, nhưng vẫn đủ dày để
 * đồng hồ đếm ngược trên màn hình không nhảy giật.
 */
const THROTTLED_POLL_MS = 10000

export function useAttempt(attemptId: string | undefined) {
  return useQuery({
    queryKey: [ATTEMPT_KEY, attemptId],
    queryFn: () => attemptApi.get(attemptId!),
    enabled: Boolean(attemptId),
    // Bài đang làm giữ trạng thái ở local (đáp án đang chọn), tự refetch sẽ ghi đè mất
    refetchOnWindowFocus: false,
    // Chấm tự luận chạy nền (features/06): hỏi lại cho tới khi hết câu chờ, rồi dừng hẳn.
    // Không có nhánh này thì người học phải tự bấm F5 mới thấy điểm phần tự luận.
    refetchInterval: (query) => {
      const data = query.state.data
      if (!data?.gradingPending) return false
      return data.aiThrottledSeconds > 0 ? THROTTLED_POLL_MS : GRADING_POLL_MS
    },
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

/** Nhờ AI giải thích một câu. Không lưu vào CSDL nên mỗi lần bấm là một lời gọi mô hình. */
export function useExplainAnswer(attemptId: string | undefined) {
  return useMutation({
    mutationFn: (answerId: string) => attemptApi.explain(attemptId!, answerId),
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
      message.success(
        data.gradingPending > 0
          ? `Đã nộp bài — AI đang chấm ${data.gradingPending} câu tự luận`
          : `Đã nộp bài — ${data.attempt.totalScore}/${data.attempt.maxScore} điểm`,
      )
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}
