import { Navigate, Route, Routes } from 'react-router-dom'
import { Result } from 'antd'
import ProtectedRoute from '@/features/auth/components/ProtectedRoute'
import ForgotPasswordPage from '@/features/auth/pages/ForgotPasswordPage'
import LoginPage from '@/features/auth/pages/LoginPage'
import ProfilePage from '@/features/auth/pages/ProfilePage'
import RegisterPage from '@/features/auth/pages/RegisterPage'
import { useIsAuthenticated } from '@/features/auth/store/authStore'
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
        <Route path="/learning-path" element={<LearningPathPage />} />
        {/* Sảnh phòng đấu cần đăng nhập vì chỉ thành viên mới mở được phòng */}
        <Route path="/rooms" element={<RoomLobbyPage />} />
        {/* Khu vực AI — controller đã chặn ở BE, chỉ CREATOR/ADMIN gọi được */}
        <Route path="/ai/materials" element={<MaterialsPage />} />
        <Route path="/ai/generate" element={<GenerateQuestionsPage />} />
        {/* Bộ mặt "bảng điều khiển": bảng quản lý */}
        <Route path="/my-quizzes" element={<MyQuizzesPage />} />
        <Route path="/my-quizzes/:id" element={<QuizEditorPage />} />
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
