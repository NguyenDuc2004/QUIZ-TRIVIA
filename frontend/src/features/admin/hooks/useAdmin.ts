import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import type { Role } from '@/features/auth/api/authApi'
import { adminApi, type CategoryBody } from '../api/adminApi'

const ADMIN_KEY = 'admin'

export function useSystemOverview(days: number) {
  return useQuery({
    queryKey: [ADMIN_KEY, 'overview', days],
    queryFn: () => adminApi.overview(days),
  })
}

export function useRevokeSessions() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => adminApi.revokeSessions(id),
    onSuccess: (result) => {
      // Nói đúng số phiên: 0 phiên nghĩa là người đó đã không đăng nhập ở đâu cả, và quản trị viên cần
      // biết điều đó thay vì tưởng vừa đẩy ai đó ra khỏi một thiết bị lạ.
      message.success(
        result.soPhienDaThuHoi > 0
          ? `Đã thu hồi ${result.soPhienDaThuHoi} phiên đăng nhập. Tài khoản KHÔNG bị khoá.`
          : 'Người dùng này không có phiên đăng nhập nào đang mở.',
      )
      queryClient.invalidateQueries({ queryKey: [ADMIN_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useAdminCategories() {
  return useQuery({
    queryKey: [ADMIN_KEY, 'categories'],
    queryFn: () => adminApi.categories(),
  })
}

export function useSaveCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id?: string; body: CategoryBody }) =>
      id ? adminApi.updateCategory(id, body) : adminApi.createCategory(body),
    onSuccess: (category, variables) => {
      message.success(variables.id ? `Đã cập nhật "${category.name}"` : `Đã thêm "${category.name}"`)
      queryClient.invalidateQueries({ queryKey: [ADMIN_KEY, 'categories'] })
      // Danh mục cũng hiện ở bộ lọc trang khám phá của người học — làm mới luôn để không lệch.
      queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useDeleteCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteCategory(id),
    onSuccess: () => {
      message.success('Đã xoá danh mục')
      queryClient.invalidateQueries({ queryKey: [ADMIN_KEY, 'categories'] })
      queryClient.invalidateQueries({ queryKey: ['categories'] })
    },
    // Lỗi 409 "còn N quiz đang dùng" là thông tin hữu ích, không phải sự cố — hiện nguyên văn từ backend
    // vì nó kèm số lượng cụ thể mà giao diện không tự biết.
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useAdminQuizzes(params: {
  keyword?: string
  categoryId?: string
  page?: number
  size?: number
}) {
  return useQuery({
    queryKey: [ADMIN_KEY, 'quizzes', params],
    queryFn: () => adminApi.quizzes(params),
  })
}

export function useHideQuiz() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => adminApi.hideQuiz(id),
    onSuccess: () => {
      message.success('Đã ẩn quiz khỏi trang khám phá. Quiz vẫn thuộc chủ của nó và không bị xoá.')
      queryClient.invalidateQueries({ queryKey: [ADMIN_KEY, 'quizzes'] })
      queryClient.invalidateQueries({ queryKey: ['quizzes'] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useLiveRooms() {
  return useQuery({
    queryKey: [ADMIN_KEY, 'rooms'],
    queryFn: () => adminApi.rooms(),
    // Phòng đấu thay đổi theo giây. Không tự làm mới thì trang giám sát hiện trạng thái cũ, mà đó đúng
    // là thứ duy nhất trang này có nhiệm vụ nói đúng.
    refetchInterval: 5000,
  })
}

export function useCloseRoom() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (roomCode: string) => adminApi.closeRoom(roomCode),
    onSuccess: (_data, roomCode) => {
      message.success(`Đã đóng phòng ${roomCode}`)
      queryClient.invalidateQueries({ queryKey: [ADMIN_KEY, 'rooms'] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useAiConfig() {
  return useQuery({
    queryKey: [ADMIN_KEY, 'ai-config'],
    queryFn: () => adminApi.aiConfig(),
  })
}

export function useAdminUsers(params: {
  keyword?: string
  role?: Role
  locked?: boolean
  page?: number
  size?: number
}) {
  return useQuery({
    queryKey: [ADMIN_KEY, 'users', params],
    queryFn: () => adminApi.users(params),
  })
}

export function useChangeRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, role }: { id: string; role: Role }) => adminApi.changeRole(id, role),
    onSuccess: (user) => {
      // Nói rõ hệ quả: người đó bị đăng xuất khỏi mọi thiết bị. Vai trò nằm trong access token nên
      // không thu hồi phiên thì họ dùng quyền cũ thêm 15 phút — quản trị viên cần biết điều đó đã xảy ra.
      message.success(`Đã đổi vai trò của ${user.displayName}. Người dùng cần đăng nhập lại.`)
      queryClient.invalidateQueries({ queryKey: [ADMIN_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useSetAiQuota() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, quota }: { id: string; quota: number | null }) => adminApi.setAiQuota(id, quota),
    onSuccess: (user) => {
      // Nói rõ BA trạng thái khác nhau. "Đã lưu" trơn thì quản trị viên không biết mình vừa CẤM ai đó —
      // và 0 với null nhìn trên màn hình gần giống nhau.
      message.success(
        user.aiDailyQuota === null
          ? `${user.displayName} dùng hạn mức mặc định của hệ thống.`
          : user.aiDailyQuota === 0
            ? `Đã CẤM ${user.displayName} gọi AI.`
            : `Hạn mức của ${user.displayName}: ${user.aiDailyQuota} lượt/ngày.`,
      )
      queryClient.invalidateQueries({ queryKey: [ADMIN_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useSetLocked() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, locked }: { id: string; locked: boolean }) => adminApi.setLocked(id, locked),
    onSuccess: (user) => {
      message.success(
        user.locked
          ? `Đã khoá ${user.displayName}. Mọi phiên đăng nhập của họ đã bị thu hồi.`
          : `Đã mở khoá ${user.displayName}.`,
      )
      queryClient.invalidateQueries({ queryKey: [ADMIN_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useAiUsage(days: number) {
  return useQuery({
    queryKey: [ADMIN_KEY, 'ai-usage', days],
    queryFn: () => adminApi.aiUsage(days),
  })
}
