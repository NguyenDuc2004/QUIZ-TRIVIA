import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import type { Role } from '@/features/auth/api/authApi'
import { adminApi } from '../api/adminApi'

const ADMIN_KEY = 'admin'

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
