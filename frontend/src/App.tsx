import { Navigate, Route, Routes } from 'react-router-dom'
import { Result } from 'antd'
import ProtectedRoute from '@/features/auth/components/ProtectedRoute'
import ForgotPasswordPage from '@/features/auth/pages/ForgotPasswordPage'
import LoginPage from '@/features/auth/pages/LoginPage'
import ProfilePage from '@/features/auth/pages/ProfilePage'
import RegisterPage from '@/features/auth/pages/RegisterPage'
import { useAuthStore, useIsAuthenticated } from '@/features/auth/store/authStore'
import { trangChuTheoVaiTro } from '@/features/auth/trangChu'
import GradeAttemptPage from '@/features/analytics/pages/GradeAttemptPage'
import MyProgressPage from '@/features/analytics/pages/MyProgressPage'
import QuizStatsPage from '@/features/analytics/pages/QuizStatsPage'
import AssistantPage from '@/features/chat/pages/AssistantPage'
import AttemptPage from '@/features/attempt/pages/AttemptPage'
import MyAttemptsPage from '@/features/attempt/pages/MyAttemptsPage'
import LearningPathPage from '@/features/recommend/pages/LearningPathPage'
import DeckDetailPage from '@/features/flashcard/pages/DeckDetailPage'
import DecksPage from '@/features/flashcard/pages/DecksPage'
import AchievementsPage from '@/features/gamification/pages/AchievementsPage'
import LeaderboardPage from '@/features/season/pages/LeaderboardPage'
import ClassroomsPage from '@/features/classroom/pages/ClassroomsPage'
import ClassroomDetailPage from '@/features/classroom/pages/ClassroomDetailPage'
import MyAssignmentsPage from '@/features/classroom/pages/MyAssignmentsPage'
import AssignmentResultsPage from '@/features/classroom/pages/AssignmentResultsPage'
import NotificationsPage from '@/features/notification/pages/NotificationsPage'
import NotificationSettingsPage from '@/features/notification/pages/NotificationSettingsPage'
import ReviewSessionPage from '@/features/flashcard/pages/ReviewSessionPage'
import QuizIntroPage from '@/features/attempt/pages/QuizIntroPage'
import BrowseQuizzesPage from '@/features/quiz/pages/BrowseQuizzesPage'
import AdminLayout from '@/features/admin/components/AdminLayout'
import AdminAiUsagePage from '@/features/admin/pages/AdminAiUsagePage'
import AdminCategoriesPage from '@/features/admin/pages/AdminCategoriesPage'
import AdminOverviewPage from '@/features/admin/pages/AdminOverviewPage'
import AdminQuizzesPage from '@/features/admin/pages/AdminQuizzesPage'
import AdminQuizDetailPage from '@/features/admin/pages/AdminQuizDetailPage'
import AdminRoomsPage from '@/features/admin/pages/AdminRoomsPage'
import AdminUsersPage from '@/features/admin/pages/AdminUsersPage'
import AdminIntegrityPage from '@/features/integrity/pages/AdminIntegrityPage'
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
  const role = useAuthStore((state) => state.user?.role)
  return useIsAuthenticated() ? <Navigate to={trangChuTheoVaiTro(role)} replace /> : <>{children}</>
}

/** `/` đưa mỗi vai trò về đúng khu của mình. */
function TrangChu() {
  const role = useAuthStore((state) => state.user?.role)
  return <Navigate to={trangChuTheoVaiTro(role)} replace />
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

      {/*
        Khu học tập. `roles` cố ý KHÔNG có ADMIN: quản trị viên chỉ làm việc trong khu quản trị.
        Khu này không có gì cho họ, và để một tài khoản có quyền tác động lên người khác đi lang thang
        giữa dữ liệu của người khác là mở một cửa không cần thiết. Admin gõ tay đường dẫn ở đây sẽ được
        đưa về `/admin` — xem `trangChuTheoVaiTro`.
      */}
      <Route
        element={
          <ProtectedRoute roles={['LEARNER', 'CREATOR']}>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<TrangChu />} />
        {/* Bộ mặt "học viên": lưới card */}
        <Route path="/quizzes" element={<BrowseQuizzesPage />} />
        <Route path="/quizzes/:id" element={<QuizIntroPage />} />
        {/* Một đường dẫn cho cả lúc đang làm và lúc xem kết quả — phân biệt theo attempt.status */}
        <Route path="/attempts/:id" element={<AttemptPage />} />
        <Route path="/my-attempts" element={<MyAttemptsPage />} />
        <Route path="/my-progress" element={<MyProgressPage />} />
        <Route path="/learning-path" element={<LearningPathPage />} />
        {/* Thẻ ghi nhớ (features/11) — chức năng của người học, mọi tài khoản đã đăng nhập đều dùng được.
            Màn ôn thẻ nhận `?deckId=` để ôn trong một bộ, thiếu tham số thì ôn mọi thẻ đến hạn. */}
        <Route path="/flashcards" element={<DecksPage />} />
        {/* Thành tích (features/13) — XP, cấp độ, chuỗi ngày, huy hiệu */}
        <Route path="/achievements" element={<AchievementsPage />} />
        {/* Bảng xếp hạng theo mùa (features/15) — đọc từ Redis ZSET, dựng lại được từ xp_events */}
        <Route path="/leaderboard" element={<LeaderboardPage />} />
        {/* Lớp học & giao bài (features/14). Mọi tài khoản đã đăng nhập đều vào được: học sinh là
            người HỌC, không phải người soạn nội dung, nên bắt có vai trò CREATOR chỉ để vào lớp là sai
            hẳn mô hình. Backend chặn riêng việc TẠO lớp ở CREATOR/ADMIN. */}
        <Route path="/classrooms" element={<ClassroomsPage />} />
        <Route path="/classrooms/:id" element={<ClassroomDetailPage />} />
        <Route path="/my-assignments" element={<MyAssignmentsPage />} />
        <Route path="/assignments/:id/results" element={<AssignmentResultsPage />} />
        {/* Thông báo (features/16). Không có mục trên thanh điều hướng: lối vào là chuông ở góc phải,
            và thêm một mục thứ sáu vào thanh vừa gom xuống 5 mục là đi ngược việc vừa làm. */}
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/notifications/settings" element={<NotificationSettingsPage />} />
        <Route path="/flashcards/review" element={<ReviewSessionPage />} />
        <Route path="/flashcards/decks/:id" element={<DeckDetailPage />} />
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
        Khu quản trị: layout RIÊNG, không nằm trong AppLayout (docs/ui-design-system.md §1).
        Menu khu học tập không liên quan gì khi đang khoá tài khoản hay xem chi phí AI, và nền tối cùng
        bố cục sidebar là dấu hiệu để admin luôn biết mình đang ở khu có những thao tác tác động lên
        người khác. `roles` chặn ở FE cho người gõ tay đường dẫn; AdminController vẫn chặn ADMIN ở cấp
        lớp nên đây chỉ là lớp thứ hai, không phải lớp duy nhất.
      */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute roles={['ADMIN']}>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<AdminOverviewPage />} />
        <Route path="users" element={<AdminUsersPage />} />
        <Route path="categories" element={<AdminCategoriesPage />} />
        <Route path="quizzes" element={<AdminQuizzesPage />} />
        {/* Xem nội dung quiz để có căn cứ trước khi kiểm duyệt. Nằm TRONG khu admin vì quản trị viên
            không vào được khu học tập — dẫn sang `/quizzes/{id}` sẽ chỉ bị đá ngược về `/admin`. */}
        <Route path="quizzes/:id" element={<AdminQuizDetailPage />} />
        <Route path="rooms" element={<AdminRoomsPage />} />
        {/* Rà soát tính toàn vẹn bài thi (features/12) */}
        <Route path="integrity" element={<AdminIntegrityPage />} />
        <Route path="ai" element={<AdminAiUsagePage />} />
        {/* Hồ sơ dùng lại đúng trang của khu học tập: nội dung y hệt (đổi tên, ảnh, mật khẩu), và dựng
            một bản thứ hai chỉ để đổi khung là hai chỗ phải sửa mỗi lần đổi một dòng. */}
        <Route path="profile" element={<ProfilePage />} />
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
