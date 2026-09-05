import { create } from 'zustand'
import { createJSONStorage, persist, type StateStorage } from 'zustand/middleware'
import { tokenStorage } from '@/shared/api/tokenStorage'
import type { AuthResult, UserProfile } from '../api/authApi'

interface AuthState {
  user: UserProfile | null
  /** Đã đọc xong phiên lưu ở localStorage chưa — dùng để tránh nháy trang khi F5. */
  isReady: boolean
  setSession: (result: AuthResult, ghiNho?: boolean) => void
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
 * Hồ sơ người dùng đi theo ĐÚNG lựa chọn "ghi nhớ đăng nhập" như token.
 *
 * Không có lớp này thì tính năng bỏ tick chỉ đúng một nửa: token vào `sessionStorage` và mất khi đóng
 * trình duyệt, nhưng **tên, email và ảnh đại diện vẫn nằm lại `localStorage`** — đúng thứ người dùng
 * bỏ tick để tránh khi ngồi máy chung. Giao diện không lộ ra phiên đăng nhập giả (`useIsAuthenticated`
 * đòi có CẢ hồ sơ lẫn token), nhưng dữ liệu cá nhân thì vẫn đọc được bằng công cụ dev.
 *
 * Một quy tắc, hai thứ dùng: `tokenStorage` biết đang ở chế độ nào, lớp này chỉ hỏi lại.
 */
const khoTheoLuaChon: StateStorage = {
  getItem: (khoa) => sessionStorage.getItem(khoa) ?? localStorage.getItem(khoa),
  setItem: (khoa, giaTri) => {
    const nho = tokenStorage.dangGhiNho()
    ;(nho ? localStorage : sessionStorage).setItem(khoa, giaTri)
    ;(nho ? sessionStorage : localStorage).removeItem(khoa)
  },
  removeItem: (khoa) => {
    localStorage.removeItem(khoa)
    sessionStorage.removeItem(khoa)
  },
}

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
  // Xoá ở CẢ HAI kho: hồ sơ có thể đang nằm ở `sessionStorage` nếu người dùng bỏ tick "ghi nhớ".
  localStorage.removeItem(PERSIST_KEY)
  sessionStorage.removeItem(PERSIST_KEY)
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

      /**
       * @param ghiNho chỉ truyền từ màn ĐĂNG NHẬP. Bỏ trống ở những chỗ khác (đăng ký, Google, đổi
       *   vai trò) để giữ nguyên lựa chọn hiện tại — xem `tokenStorage.save`.
       */
      setSession: (result, ghiNho) => {
        // Ghi token TRƯỚC khi `set`: `persist` chạy ngay sau `set` và nó hỏi `tokenStorage` xem đang
        // ở chế độ nào để chọn kho. Đảo thứ tự thì hồ sơ vào một kho còn token vào kho kia.
        tokenStorage.save(result.accessToken, result.refreshToken, ghiNho)
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
      storage: createJSONStorage(() => khoTheoLuaChon),
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
