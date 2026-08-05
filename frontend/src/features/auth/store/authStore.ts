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
      name: 'quizai-auth',
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
