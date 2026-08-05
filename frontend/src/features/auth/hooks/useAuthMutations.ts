import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { tokenStorage } from '@/shared/api/tokenStorage'
import { authApi, type LoginBody, type RegisterBody } from '../api/authApi'
import { useAuthStore } from '../store/authStore'

export function useLogin() {
  const setSession = useAuthStore((state) => state.setSession)
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (body: LoginBody) => authApi.login(body),
    onSuccess: (result) => {
      setSession(result)
      message.success(`Xin chào ${result.user.displayName}`)
      navigate('/', { replace: true })
    },
    onError: (error) => message.error(getApiErrorMessage(error, 'Đăng nhập thất bại')),
  })
}

export function useRegister() {
  const setSession = useAuthStore((state) => state.setSession)
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (body: RegisterBody) => authApi.register(body),
    onSuccess: (result) => {
      setSession(result)
      message.success('Tạo tài khoản thành công')
      navigate('/', { replace: true })
    },
    onError: (error) => message.error(getApiErrorMessage(error, 'Đăng ký thất bại')),
  })
}

export function useLogout() {
  const clearSession = useAuthStore((state) => state.clearSession)
  const navigate = useNavigate()

  return useMutation({
    mutationFn: async () => {
      const refreshToken = tokenStorage.getRefresh()
      if (refreshToken) {
        await authApi.logout(refreshToken)
      }
    },
    // Dù gọi API thất bại vẫn phải xóa phiên ở client
    onSettled: () => {
      clearSession()
      navigate('/login', { replace: true })
    },
  })
}
