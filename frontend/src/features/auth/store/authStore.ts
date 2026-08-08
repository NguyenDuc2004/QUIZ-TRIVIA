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
 * Xoá sạch phiên **không qua React** — dùng từ axios interceptor khi phiên hết hạn.
 * <p>
 * Interceptor không nằm trong cây component nên không gọi được hook. Gọi
 * `useAuthStore.getState().clearSession()` thì xoá được state trong bộ nhớ, nhưng `persist` ghi lại
 * xuống localStorage **không đồng bộ** — mà ngay sau đây là một lần điều hướng cứng
 * (`window.location.assign`), nên bản ghi có thể chưa kịp xuống đĩa và trang mới lại đọc được
 * `user` cũ. Xoá thẳng khoá là cách duy nhất chắc chắn.
 */
export function clearPersistedSession() {
  tokenStorage.clear()
  useAuthStore.getState().clearSession()
  localStorage.removeItem(PERSIST_KEY)
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

/** Đã đăng nhập = có cả thông tin user và access token. */
export function useIsAuthenticated() {
  const user = useAuthStore((state) => state.user)
  return Boolean(user && tokenStorage.getAccess())
}
