import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import { tokenStorage } from '@/shared/api/tokenStorage'
import type { AuthResult, UserProfile } from '../api/authApi'

interface AuthState {
  user: UserProfile | null
  /** Đã đọc xong phiên lưu ở localStorage chưa — dùng để tránh nháy trang khi F5. */
  isReady: boolean
  setSession: (result: AuthResult) => void
  setUser: (user: UserProfile) => void
  setReady: () => void
  clearSession: () => void
}

/**
 * Trạng thái đăng nhập. Token do {@link tokenStorage} giữ (axios interceptor cũng đọc ở đó),
 * store này chỉ giữ thông tin người dùng để hiển thị.
 */
/**
 * Khoá localStorage của `persist`. Khai riêng vì {@link clearPersistedSession} cần đến nó, và để
 * tên khoá chỉ tồn tại ở **một** chỗ.
 */
const PERSIST_KEY = 'quizai-auth'

/**
 * Xoá phiên **chỉ trong localStorage**, không đụng vào state React.
 * <p>
 * Dùng ngay trước một lần điều hướng cứng (`window.location.assign`). Đụng vào state lúc đó là tự
 * bắn vào chân: Zustand cập nhật đồng bộ → React kịp render lại → `ProtectedRoute` thấy
 * "chưa đăng nhập" và tự `Navigate` sang `/login` <b>trần</b>, cướp mất `?expired=1` và người dùng
 * lại không biết vì sao mình bị đẩy ra. Trang mới nạp lại từ localStorage nên xoá ở đây là đủ.
 * <p>
 * Xoá <b>thẳng khoá</b> {@code PERSIST_KEY} chứ không nhờ `clearSession()`: `persist` ghi xuống
 * localStorage không đồng bộ, mà điều hướng cứng có thể xảy ra trước khi nó kịp ghi.
 */
export function clearStoredSession() {
  tokenStorage.clear()
  localStorage.removeItem(PERSIST_KEY)
}

/**
 * Xoá sạch phiên, cả localStorage lẫn state trong bộ nhớ.
 * <p>
 * Dùng khi **không** điều hướng cứng (ví dụ đã đang ở trang đăng nhập) — lúc đó phải tự dọn state,
 * không có lần nạp lại trang nào làm hộ.
 */
export function clearPersistedSession() {
  clearStoredSession()
  useAuthStore.getState().clearSession()
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      isReady: false,

      setSession: (result) => {
        tokenStorage.save(result.accessToken, result.refreshToken)
        set({ user: result.user })
      },

      setUser: (user) => set({ user }),

      setReady: () => set({ isReady: true }),

      clearSession: () => {
        tokenStorage.clear()
        set({ user: null })
      },
    }),
    {
      name: PERSIST_KEY,
      partialize: (state) => ({ user: state.user }),
      onRehydrateStorage: () => (state) => state?.setReady(),
    },
  ),
)

/**
 * Đã đăng nhập = có thông tin user và **còn ít nhất một token** để dựng lại quyền truy cập.
 * <p>
 * Trước đây chỉ xét access token, và đó là lỗi: access token sống 15 phút, nên **sự tồn tại của nó
 * chưa bao giờ là bằng chứng phiên còn sống** — nó có thể đã hết hạn mà vẫn nằm nguyên trong
 * localStorage. Thứ quyết định phiên còn hay hết là **refresh token**.
 * <p>
 * Cái sai đó tạo ra một bất đối xứng vô lý cho cùng một trạng thái phiên "cần làm mới token":
 * <ul>
 *   <li>access token <i>hết hạn nhưng còn</i> → coi là đã đăng nhập → trang hiện ra → interceptor
 *       làm mới token → chạy bình thường ✅</li>
 *   <li>access token <i>không còn</i> → coi là chưa đăng nhập → {@code ProtectedRoute} đẩy về
 *       /login <b>trước khi</b> có request nào kịp làm mới token ❌</li>
 * </ul>
 * Cùng một phiên còn cứu được, xử lý hai kiểu khác nhau chỉ vì cái token chết có tình cờ còn nằm
 * trong localStorage hay không.
 * <p>
 * Refresh token <i>chết</i> (khác với <i>không có</i>) vẫn cho qua ở đây — và đúng như vậy: lời gọi
 * API đầu tiên sẽ 401, interceptor thử làm mới, thất bại rồi kết thúc phiên kèm thông báo. Chặn ở
 * tầng giao diện thì người dùng bị đẩy đi mà không ai nói vì sao.
 */
export function useIsAuthenticated() {
  const user = useAuthStore((state) => state.user)
  return Boolean(user && (tokenStorage.getAccess() || tokenStorage.getRefresh()))
}
