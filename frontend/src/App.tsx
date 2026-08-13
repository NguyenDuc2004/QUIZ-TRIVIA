import { Navigate, Route, Routes } from 'react-router-dom'
import { Result } from 'antd'
import ProtectedRoute from '@/features/auth/components/ProtectedRoute'
import ForgotPasswordPage from '@/features/auth/pages/ForgotPasswordPage'
import LoginPage from '@/features/auth/pages/LoginPage'
import ProfilePage from '@/features/auth/pages/ProfilePage'
import RegisterPage from '@/features/auth/pages/RegisterPage'
import { useIsAuthenticated } from '@/features/auth/store/authStore'
import GradeAttemptPage from '@/features/analytics/pages/GradeAttemptPage'
import MyProgressPage from '@/features/analytics/pages/MyProgressPage'
import QuizStatsPage from '@/features/analytics/pages/QuizStatsPage'
import AssistantPage from '@/features/chat/pages/AssistantPage'
import AttemptPage from '@/features/attempt/pages/AttemptPage'
import MyAttemptsPage from '@/features/attempt/pages/MyAttemptsPage'
import LearningPathPage from '@/features/recommend/pages/LearningPathPage'
import QuizIntroPage from '@/features/attempt/pages/QuizIntroPage'
import BrowseQuizzesPage from '@/features/quiz/pages/BrowseQuizzesPage'
import GenerateQuestionsPage from '@/features/ai/pages/GenerateQuestionsPage'
import MaterialsPage from '@/features/ai/pages/MaterialsPage'
import JoinRoomPage from '@/features/room/pages/JoinRoomPage'
import RoomLobbyPage from '@/features/room/pages/RoomLobbyPage'
import RoomPage from '@/features/room/pages/RoomPage'
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
      {/* Quên mật khẩu để công khai: người cần nó chính là người không đăng nhập được */}
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />

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
        <Route path="/quizzes/:id" element={<QuizIntroPage />} />
        {/* Một đường dẫn cho cả lúc đang làm và lúc xem kết quả — phân biệt theo attempt.status */}
        <Route path="/attempts/:id" element={<AttemptPage />} />
        <Route path="/my-attempts" element={<MyAttemptsPage />} />
        <Route path="/my-progress" element={<MyProgressPage />} />
        <Route path="/learning-path" element={<LearningPathPage />} />
        {/* Trợ lý học tập: mở cho MỌI người đã đăng nhập, không riêng CREATOR — người học chính là
            đối tượng nó phục vụ (features/08) */}
        <Route path="/assistant" element={<AssistantPage />} />
        {/* Sảnh phòng đấu cần đăng nhập vì chỉ thành viên mới mở được phòng */}
        <Route path="/rooms" element={<RoomLobbyPage />} />
        {/* Khu vực AI — AiController chặn CREATOR/ADMIN ở BE; chặn thêm ở FE để người học gõ tay
            đường dẫn thì được đưa về chỗ khác thay vì thấy một trang chỉ toàn lỗi 403 */}
        <Route
          path="/ai/materials"
          element={
            <ProtectedRoute roles={['CREATOR', 'ADMIN']}>
              <MaterialsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/ai/generate"
          element={
            <ProtectedRoute roles={['CREATOR', 'ADMIN']}>
              <GenerateQuestionsPage />
            </ProtectedRoute>
          }
        />
        {/* Bộ mặt "bảng điều khiển": bảng quản lý */}
        <Route path="/my-quizzes" element={<MyQuizzesPage />} />
        <Route path="/my-quizzes/:id" element={<QuizEditorPage />} />
        {/* Thống kê và chấm tay nằm DƯỚI quiz vì cả hai chỉ có nghĩa với chủ quiz; BE cũng trả
            404 cho quiz của người khác nên đường dẫn không tiết lộ gì */}
        <Route path="/my-quizzes/:id/stats" element={<QuizStatsPage />} />
        <Route path="/my-quizzes/:id/attempts/:attemptId" element={<GradeAttemptPage />} />
        <Route path="/question-bank" element={<QuestionBankPage />} />
        <Route path="/profile" element={<ProfilePage />} />
      </Route>

      {/*
        Hai route phòng đấu để CÔNG KHAI: khách quét QR chưa chắc có tài khoản.
        Cửa vào là mã PIN 6 số, và việc khách chơi được hay không do host bật/tắt cho từng phòng.
        Cả hai đều tự dựng bố cục riêng, không dùng AppLayout — màn chơi cần toàn màn hình.
      */}
      <Route path="/join/:code" element={<JoinRoomPage />} />
      <Route path="/rooms/:code" element={<RoomPage />} />

      <Route path="*" element={<Result status="404" title="404" subTitle="Không tìm thấy trang." />} />
    </Routes>
  )
}
