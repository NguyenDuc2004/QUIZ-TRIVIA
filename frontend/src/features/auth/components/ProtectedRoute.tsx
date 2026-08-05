import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { Spin } from 'antd'
import { useAuthStore, useIsAuthenticated } from '../store/authStore'

/**
 * Chặn route cần đăng nhập. Guest bị đẩy về /login (docs/features/01-auth.md).
 * Đây chỉ là lớp chắn giao diện — quyền thật do backend quyết định.
 */
export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const isReady = useAuthStore((state) => state.isReady)
  const isAuthenticated = useIsAuthenticated()
  const location = useLocation()

  // Chờ đọc xong phiên đã lưu, tránh nháy sang /login khi tải lại trang
  if (!isReady) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Spin size="large" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  return <>{children}</>
}
