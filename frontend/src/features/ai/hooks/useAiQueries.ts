import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { aiApi, type GenerateRequest } from '../api/aiApi'

const MATERIAL_KEY = 'ai-materials'
const JOB_KEY = 'ai-job'

export function useAiStatus() {
  return useQuery({
    queryKey: ['ai-status'],
    queryFn: aiApi.status,
    staleTime: 5 * 60 * 1000,
  })
}

/**
 * Danh sách học liệu.
 * <p>
 * Tự hỏi lại mỗi 3 giây khi còn tài liệu đang xử lý — việc sinh embedding chạy nền, không có
 * kênh đẩy nên phải hỏi. Xử lý xong hết thì dừng hỏi để khỏi làm phiền server.
 */
export function useMaterials(params: { page?: number; size?: number }) {
  return useQuery({
    queryKey: [MATERIAL_KEY, params],
    queryFn: () => aiApi.listMaterials(params),
    refetchInterval: (query) =>
      query.state.data?.content.some((m) => m.status === 'PROCESSING') ? 3000 : false,
  })
}

/** Theo dõi một job sinh đề tới khi xong. */
export function useAiJob(jobId: string | undefined) {
  return useQuery({
    queryKey: [JOB_KEY, jobId],
    queryFn: () => aiApi.getJob(jobId!),
    enabled: Boolean(jobId),
    refetchInterval: (query) => {
      const data = query.state.data
      const running = data?.status === 'PENDING' || data?.status === 'RUNNING'
      if (!running) return false
      // Đang chờ hạn mức thì giãn nhịp: hỏi mỗi 2 giây suốt một phút chờ là ba chục lần vô ích,
      // mà đồng hồ đếm ngược mỗi 5 giây vẫn đủ mượt để người dùng thấy nó đang nhích.
      return (data?.aiThrottledSeconds ?? 0) > 0 ? 5000 : 2000
    },
  })
}

export function useCreateMaterial() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: { title: string; topic?: string; content: string }) =>
      aiApi.createMaterial(body),
    onSuccess: () => {
      message.success('Đã nhận học liệu, đang xử lý nền')
      queryClient.invalidateQueries({ queryKey: [MATERIAL_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useUploadMaterial() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ file, title, topic }: { file: File; title?: string; topic?: string }) =>
      aiApi.uploadMaterial(file, title, topic),
    onSuccess: () => {
      message.success('Đã nhận tài liệu, đang trích nội dung và xử lý nền')
      queryClient.invalidateQueries({ queryKey: [MATERIAL_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useDeleteMaterial() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => aiApi.deleteMaterial(id),
    onSuccess: () => {
      message.success('Đã xoá học liệu')
      queryClient.invalidateQueries({ queryKey: [MATERIAL_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useGenerateQuestions() {
  return useMutation({
    mutationFn: (body: GenerateRequest) => aiApi.generate(body),
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useApproveQuestions() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ jobId, indexes }: { jobId: string; indexes: number[] }) =>
      aiApi.approve(jobId, indexes),
    onSuccess: (_data, variables) => {
      message.success(`Đã lưu ${variables.indexes.length} câu vào ngân hàng câu hỏi`)
      queryClient.invalidateQueries({ queryKey: ['questions'] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}
