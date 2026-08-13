import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { Spin } from 'antd'
import { useAuthStore, useIsAuthenticated } from '../store/authStore'

/**
 * Chặn route cần đăng nhập. Guest bị đẩy về /login (docs/features/01-auth.md).
 * Đây chỉ là lớp chắn giao diện — quyền thật do backend quyết định.
 * <p>
 * `roles` thu hẹp thêm theo vai trò. Không có nó thì người học gõ tay `/ai/materials` sẽ vào được
 * trang rồi mới ăn 403 từ API — đúng về bảo mật nhưng hiện ra một trang hỏng, trong khi câu trả lời
 * đúng là "khu vực này không dành cho bạn".
 */
export default function ProtectedRoute({
  children,
  roles,
}: {
  children: ReactNode
  roles?: string[]
}) {
  const isReady = useAuthStore((state) => state.isReady)
  const isAuthenticated = useIsAuthenticated()
  const role = useAuthStore((state) => state.user?.role)
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

  // Đã đăng nhập nhưng sai vai trò: về trang khám phá, không phải /login — họ không thiếu phiên
  // đăng nhập, mà thiếu quyền, nên đẩy sang màn hình đăng nhập chỉ gây hiểu nhầm.
  if (roles && (!role || !roles.includes(role))) {
    return <Navigate to="/quizzes" replace />
  }

  return <>{children}</>
}
