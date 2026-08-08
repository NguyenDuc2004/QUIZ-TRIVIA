import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { clearPersistedSession, clearStoredSession } from '@/features/auth/store/authStore'
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
 * Lượt làm mới token **đang chạy**, dùng chung cho mọi request cùng gặp 401.
 * <p>
 * Đây là hàng rào chống một lỗi khó thấy: backend **luân chuyển** refresh token (`rotate`), nên lượt
 * làm mới đầu tiên làm token cũ mất hiệu lực ngay. Một trang mở ra thường bắn vài request cần quyền
 * cùng lúc; nếu mỗi request tự đi làm mới thì lượt đầu thắng, những lượt sau cầm token đã chết và
 * thất bại → hệ thống kết luận phiên hết hạn rồi **đẩy người dùng ra trang đăng nhập dù phiên vẫn
 * còn cứu được**. Gộp về một lượt duy nhất là hết.
 */
let refreshInFlight: Promise<string> | null = null

function refreshAccessToken(refreshToken: string): Promise<string> {
  refreshInFlight ??= axios
    // axios "trần" để không lọt vào interceptor này lần nữa
    .post(`${BASE_URL}/auth/refresh`, { refreshToken })
    .then(({ data }) => {
      tokenStorage.save(data.accessToken, data.refreshToken)
      return data.accessToken as string
    })
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

/**
 * Kết thúc phiên và đưa về trang đăng nhập **kèm lý do**.
 * <p>
 * Không nói gì thì người dùng thấy mình bị ném ra trang đăng nhập không rõ vì sao, và tưởng hệ thống
 * lỗi. `?expired=1` để trang đăng nhập nói đúng một câu: phiên đã hết, đăng nhập lại.
 */
function endSession() {
  if (window.location.pathname === '/login') {
    // Không điều hướng thì không có lần nạp lại trang nào dọn state hộ — phải tự dọn
    clearPersistedSession()
    return
  }
  // Thứ tự quan trọng: chỉ xoá localStorage rồi điều hướng cứng. Xoá state React trước thì
  // ProtectedRoute kịp Navigate sang '/login' trần và `?expired=1` mất theo.
  clearStoredSession()
  window.location.assign('/login?expired=1')
}

/**
 * Access token sống 15 phút. Gặp 401 thì làm mới token đúng **một** lần rồi phát lại request gốc;
 * không cứu được thì kết thúc phiên và đẩy về trang đăng nhập.
 * <p>
 * <b>Mọi</b> lối 401 không cứu được đều phải đi qua {@link endSession}. Bản đầu chỉ xử lý nhánh
 * "làm mới thất bại"; nhánh **không có refresh token** thì lặng lẽ `reject`, để lại đúng một trạng
 * thái lửng lơ: token đã chết nhưng `user` vẫn còn trong localStorage, nên header vẫn hiện tên người
 * dùng như đang đăng nhập trong khi mọi API cần quyền đều 401. Giao diện trông như vẫn hoạt động, chỗ
 * nào cần quyền thì im lặng trống — người dùng không có cách nào biết mình cần đăng nhập lại.
 */
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined

    if (error.response?.status !== 401 || !config) {
      return Promise.reject(error)
    }

    // Chính các endpoint auth trả 401 là chuyện bình thường (sai mật khẩu, refresh token chết) —
    // để mỗi form tự hiển thị lỗi của nó, đừng đá người dùng ra khỏi trang đang thao tác.
    if (NO_RETRY_PATHS.some((path) => config.url?.includes(path))) {
      return Promise.reject(error)
    }

    const refreshToken = tokenStorage.getRefresh()
    if (!refreshToken || config._retried) {
      endSession()
      return Promise.reject(error)
    }

    config._retried = true
    try {
      const accessToken = await refreshAccessToken(refreshToken)
      config.headers.Authorization = `Bearer ${accessToken}`
      return apiClient.request(config)
    } catch (refreshError) {
      endSession()
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
