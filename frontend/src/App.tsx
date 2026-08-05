import { Navigate, Route, Routes } from 'react-router-dom'
import { Result } from 'antd'
import ProtectedRoute from '@/features/auth/components/ProtectedRoute'
import LoginPage from '@/features/auth/pages/LoginPage'
import ProfilePage from '@/features/auth/pages/ProfilePage'
import RegisterPage from '@/features/auth/pages/RegisterPage'
import { useIsAuthenticated } from '@/features/auth/store/authStore'

/** Đã đăng nhập thì không cần vào lại trang đăng nhập/đăng ký. */
function GuestOnlyRoute({ children }: { children: React.ReactNode }) {
  return useIsAuthenticated() ? <Navigate to="/" replace /> : <>{children}</>
}

export default function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <GuestOnlyRoute>
            <LoginPage />
          </GuestOnlyRoute>
        }
      />
      <Route
        path="/register"
        element={
          <GuestOnlyRoute>
            <RegisterPage />
          </GuestOnlyRoute>
        }
      />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <ProfilePage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Result status="404" title="404" subTitle="Không tìm thấy trang." />} />
    </Routes>
  )
}
