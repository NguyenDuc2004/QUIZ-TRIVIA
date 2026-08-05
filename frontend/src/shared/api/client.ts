import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { tokenStorage } from './tokenStorage'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

/**
 * Axios instance dùng chung. KHÔNG hardcode URL API ở component (docs/conventions.md §2).
 * Dev: gọi đường dẫn tương đối `/api/v1/...`, Vite proxy sang backend :8080.
 */
export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const token = tokenStorage.getAccess()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** Các endpoint không được tự động refresh (tránh vòng lặp). */
const NO_RETRY_PATHS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout']

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

/**
 * Access token sống 15 phút. Khi backend trả 401, thử làm mới token đúng MỘT lần
 * rồi phát lại request gốc; thất bại thì xóa phiên và đẩy về trang đăng nhập.
 */
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined
    const refreshToken = tokenStorage.getRefresh()

    const shouldRefresh =
      error.response?.status === 401 &&
      config &&
      !config._retried &&
      refreshToken &&
      !NO_RETRY_PATHS.some((path) => config.url?.includes(path))

    if (!shouldRefresh) {
      return Promise.reject(error)
    }

    config._retried = true
    try {
      // Dùng axios "trần" để không lọt vào interceptor này lần nữa
      const { data } = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken })
      tokenStorage.save(data.accessToken, data.refreshToken)
      config.headers.Authorization = `Bearer ${data.accessToken}`
      return apiClient.request(config)
    } catch (refreshError) {
      tokenStorage.clear()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
      return Promise.reject(refreshError)
    }
  },
)

/** Lấy thông báo lỗi tiếng Việt từ response lỗi chuẩn của backend (docs/api.md §10). */
export function getApiErrorMessage(error: unknown, fallback = 'Đã có lỗi xảy ra, vui lòng thử lại'): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    if (data?.message) {
      return data.message
    }
    if (!error.response) {
      return 'Không kết nối được tới server. Kiểm tra backend đã chạy chưa.'
    }
  }
  return fallback
}
