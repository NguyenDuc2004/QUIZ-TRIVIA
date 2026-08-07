import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { recommendApi } from '../api/recommendApi'

const RECOMMEND_KEY = 'recommendations'

/**
 * Gợi ý đổi chậm — chỉ sau khi người dùng nộp thêm bài. Để `staleTime` dài để chuyển qua lại giữa
 * các trang không gọi lại truy vấn đồ thị (mấy câu Cypher này nặng hơn một lần đọc bảng).
 */
export function useRecommendedQuizzes(limit = 8) {
  return useQuery({
    queryKey: [RECOMMEND_KEY, 'quizzes', limit],
    queryFn: () => recommendApi.quizzes(limit),
    staleTime: 5 * 60 * 1000,
  })
}

export function useLearningPath() {
  return useQuery({
    queryKey: [RECOMMEND_KEY, 'path'],
    queryFn: () => recommendApi.path(),
    staleTime: 5 * 60 * 1000,
  })
}

export function useRebuildGraph() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => recommendApi.rebuild(),
    onSuccess: (data) => {
      message.success(`Đã phân tích lại ${data.syncedAttempts} bài làm của bạn`)
      queryClient.invalidateQueries({ queryKey: [RECOMMEND_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}
