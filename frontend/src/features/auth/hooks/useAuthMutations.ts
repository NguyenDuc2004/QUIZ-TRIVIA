import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { tokenStorage } from '@/shared/api/tokenStorage'
import { authApi, type LoginBody, type RegisterBody, type Role } from '../api/authApi'
import { useAuthStore } from '../store/authStore'

/**
 * Xoá sạch cache TanStack Query mỗi khi người đang đăng nhập thay đổi.
 * <p>
 * Đây là <b>rào chắn quyền riêng tư</b>, không phải tối ưu hiệu năng. Đăng xuất rồi đăng nhập tài
 * khoản khác là điều hướng phía client — trang không nạp lại, nên `QueryClient` sống nguyên qua cả
 * hai phiên. Với `staleTime: 30_000`, dữ liệu của người trước còn được coi là tươi nên các trang hiện
 * nó ra ngay mà không gọi lại API: đăng nhập tài khoản A xong thấy lịch sử chat của tài khoản B. Đã
 * gặp thật trên máy dev, và không chỉ lịch sử chat — mọi thứ đi qua cache đều rò: lượt làm bài, tiến
 * độ, quiz của tôi, ngân hàng câu hỏi, học liệu.
 * <p>
 * Xoá ở <b>cả</b> lối vào và lối ra, vì hai lối không bao hàm nhau: người dùng có thể mở thẳng
 * `/login` mà chưa từng bấm đăng xuất.
 */
function clearQueryCache(queryClient: QueryClient) {
  queryClient.clear()
}

export function useLogin() {
  const setSession = useAuthStore((state) => state.setSession)
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (body: LoginBody) => authApi.login(body),
    onSuccess: (result) => {
      // Xoá TRƯỚC khi đặt phiên mới: không để tồn tại khoảnh khắc nào mà danh tính đã là người mới
      // trong khi cache vẫn là dữ liệu người cũ
      clearQueryCache(queryClient)
      setSession(result)
      message.success(`Xin chào ${result.user.displayName}`)
      navigate('/', { replace: true })
    },
    onError: (error) => message.error(getApiErrorMessage(error, 'Đăng nhập thất bại')),
  })
}

export function useGoogleLogin() {
  const setSession = useAuthStore((state) => state.setSession)
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: ({ idToken, role }: { idToken: string; role?: Role }) =>
      authApi.loginWithGoogle(idToken, role),
    onSuccess: (result) => {
      clearQueryCache(queryClient)
      setSession(result)
      message.success(`Xin chào ${result.user.displayName}`)
      navigate('/', { replace: true })
    },
    onError: (error) => message.error(getApiErrorMessage(error, 'Đăng nhập Google thất bại')),
  })
}

export function useRegister() {
  const setSession = useAuthStore((state) => state.setSession)
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: (body: RegisterBody) => authApi.register(body),
    onSuccess: (result) => {
      clearQueryCache(queryClient)
      setSession(result)
      message.success('Tạo tài khoản thành công')
      navigate('/', { replace: true })
    },
    onError: (error) => message.error(getApiErrorMessage(error, 'Đăng ký thất bại')),
  })
}

/**
 * Tự đổi vai trò giữa Người học và Người tạo nội dung.
 *
 * `clearQueryCache` là bắt buộc, không phải cho gọn: cache đang giữ kết quả của những truy vấn hỏi
 * theo vai trò cũ — danh sách quiz của tôi, học liệu, trạng thái AI. Giữ nguyên thì người vừa lên
 * Creator mở trang mới và thấy dữ liệu rỗng đã cache từ lúc còn là người học.
 *
 * KHÔNG điều hướng đi đâu: người dùng đang ở trang Hồ sơ và vừa bấm một nút ở đó, đá họ sang trang
 * khác là lấy mất chỗ đứng của họ mà không ai yêu cầu.
 */
export function useDoiVaiTro() {
  const setSession = useAuthStore((state) => state.setSession)
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (role: 'LEARNER' | 'CREATOR') => authApi.doiVaiTro(role),
    onSuccess: (result) => {
      clearQueryCache(queryClient)
      setSession(result)
      message.success(
        result.user.role === 'CREATOR'
          ? 'Bạn đã là Người tạo nội dung — menu Thư viện và Sinh đề AI đã mở'
          : 'Đã chuyển về vai trò Người học',
      )
    },
    onError: (error) => message.error(getApiErrorMessage(error, 'Không đổi được vai trò')),
  })
}

export function useLogout() {
  const clearSession = useAuthStore((state) => state.clearSession)
  const queryClient = useQueryClient()
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
      clearQueryCache(queryClient)
      navigate('/login', { replace: true })
    },
  })
}
