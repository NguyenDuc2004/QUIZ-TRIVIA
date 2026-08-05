import { Navigate, Route, Routes } from 'react-router-dom'
import { Result } from 'antd'
import ProtectedRoute from '@/features/auth/components/ProtectedRoute'
import LoginPage from '@/features/auth/pages/LoginPage'
import ProfilePage from '@/features/auth/pages/ProfilePage'
import RegisterPage from '@/features/auth/pages/RegisterPage'
import { useIsAuthenticated } from '@/features/auth/store/authStore'
import BrowseQuizzesPage from '@/features/quiz/pages/BrowseQuizzesPage'
import MyQuizzesPage from '@/features/quiz/pages/MyQuizzesPage'
import QuestionBankPage from '@/features/quiz/pages/QuestionBankPage'
import QuizEditorPage from '@/features/quiz/pages/QuizEditorPage'
import AppLayout from '@/shared/components/AppLayout'

/** Đã đăng nhập thì không cần vào lại trang đăng nhập/đăng ký. */
function GuestOnlyRoute({ children }: { children: React.ReactNode }) {
  return useIsAuthenticated() ? <Navigate to="/quizzes" replace /> : <>{children}</>
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

      {/* Khu vực cần đăng nhập, dùng chung layout có header sticky */}
      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<Navigate to="/quizzes" replace />} />
        {/* Bộ mặt "học viên": lưới card */}
        <Route path="/quizzes" element={<BrowseQuizzesPage />} />
        {/* Bộ mặt "bảng điều khiển": bảng quản lý */}
        <Route path="/my-quizzes" element={<MyQuizzesPage />} />
        <Route path="/my-quizzes/:id" element={<QuizEditorPage />} />
        <Route path="/question-bank" element={<QuestionBankPage />} />
        <Route path="/profile" element={<ProfilePage />} />
      </Route>

      <Route path="*" element={<Result status="404" title="404" subTitle="Không tìm thấy trang." />} />
    </Routes>
  )
}
