import { describe, expect, it, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './ProtectedRoute'
import { useAuthStore } from '../store/authStore'
import { tokenStorage } from '@/shared/api/tokenStorage'

/**
 * `ProtectedRoute` là lớp chắn giao diện, không phải lớp bảo mật — quyền thật do backend quyết định.
 * Nhưng nó quyết định **người dùng bị đưa đi đâu**, và đưa sai chỗ thì họ không hiểu vì sao mình bị
 * chặn. Ba hành vi cần giữ đúng:
 *
 * 1. Chưa đăng nhập → `/login`.
 * 2. Đã đăng nhập nhưng sai vai trò → `/quizzes`, **không** phải `/login`: họ không thiếu phiên đăng
 *    nhập mà thiếu quyền; đẩy sang màn đăng nhập chỉ gây hiểu nhầm là phiên đã hết.
 * 3. Chưa đọc xong phiên đã lưu (`isReady = false`) → chờ, **không** điều hướng: điều hướng sớm làm
 *    trang nháy sang `/login` mỗi lần tải lại dù người dùng vẫn đang đăng nhập.
 * 4. Đích của "sai vai trò" phải **theo vai trò**, không cố định `/quizzes`. Từ khi Admin bị chặn khỏi khu
 *    học tập, đẩy họ về `/quizzes` là đẩy vào đúng chỗ vừa từ chối họ — vòng lặp chuyển hướng, trang trắng,
 *    không có lỗi nào để lần ra. Đây là loại lỗi không thấy khi thử tay bằng tài khoản người học.
 */

function renderAt(path: string, element: React.ReactNode) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={path} element={element} />
        <Route path="/login" element={<div>TRANG ĐĂNG NHẬP</div>} />
        <Route path="/quizzes" element={<div>TRANG KHÁM PHÁ</div>} />
        <Route path="/admin" element={<div>KHU QUẢN TRỊ</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

const NoiDung = () => <div>NỘI DUNG ĐƯỢC BẢO VỆ</div>

function dangNhapVoiVaiTro(role: 'LEARNER' | 'CREATOR' | 'ADMIN') {
  tokenStorage.save('access', 'refresh')
  useAuthStore.setState({
    user: { id: 'u1', email: 'a@example.com', displayName: 'A', avatarUrl: null, role,
            createdAt: '2026-08-14T00:00:00Z' },
    isReady: true,
  })
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    tokenStorage.clear()
    useAuthStore.setState({ user: null, isReady: true })
  })

  it('chưa đăng nhập thì đưa về trang đăng nhập', () => {
    renderAt('/my-quizzes', <ProtectedRoute><NoiDung /></ProtectedRoute>)

    expect(screen.getByText('TRANG ĐĂNG NHẬP')).toBeInTheDocument()
    expect(screen.queryByText('NỘI DUNG ĐƯỢC BẢO VỆ')).not.toBeInTheDocument()
  })

  it('đã đăng nhập, không yêu cầu vai trò cụ thể thì cho vào', () => {
    dangNhapVoiVaiTro('LEARNER')

    renderAt('/my-attempts', <ProtectedRoute><NoiDung /></ProtectedRoute>)

    expect(screen.getByText('NỘI DUNG ĐƯỢC BẢO VỆ')).toBeInTheDocument()
  })

  it('đúng vai trò yêu cầu thì cho vào', () => {
    dangNhapVoiVaiTro('CREATOR')

    renderAt('/ai/materials',
      <ProtectedRoute roles={['CREATOR', 'ADMIN']}><NoiDung /></ProtectedRoute>)

    expect(screen.getByText('NỘI DUNG ĐƯỢC BẢO VỆ')).toBeInTheDocument()
  })

  it('sai vai trò thì về trang khám phá, KHÔNG phải trang đăng nhập', () => {
    // Người học gõ tay /ai/materials: trước khi có prop `roles`, họ vào được trang rồi mới ăn 403 từ
    // API — đúng về bảo mật nhưng hiện ra một trang hỏng
    dangNhapVoiVaiTro('LEARNER')

    renderAt('/ai/materials',
      <ProtectedRoute roles={['CREATOR', 'ADMIN']}><NoiDung /></ProtectedRoute>)

    expect(screen.getByText('TRANG KHÁM PHÁ')).toBeInTheDocument()
    expect(screen.queryByText('TRANG ĐĂNG NHẬP'))
      .not.toBeInTheDocument()
    expect(screen.queryByText('NỘI DUNG ĐƯỢC BẢO VỆ')).not.toBeInTheDocument()
  })

  it('ADMIN bị chặn khỏi khu học tập thì về /admin, KHÔNG phải /quizzes', () => {
    // Nếu đích là /quizzes thì Admin bị đá về đúng chỗ vừa từ chối họ và vòng lặp mãi. Đây là phép kiểm
    // giữ cho ai đó về sau không "đơn giản hoá" lại thành một hằng số.
    dangNhapVoiVaiTro('ADMIN')

    renderAt('/quizzes', <ProtectedRoute roles={['LEARNER', 'CREATOR']}><NoiDung /></ProtectedRoute>)

    expect(screen.getByText('KHU QUẢN TRỊ')).toBeInTheDocument()
    expect(screen.queryByText('TRANG KHÁM PHÁ')).not.toBeInTheDocument()
    expect(screen.queryByText('NỘI DUNG ĐƯỢC BẢO VỆ')).not.toBeInTheDocument()
  })

  it('CREATOR vẫn vào được khu học tập', () => {
    // Đối chứng: nếu thiếu, phép kiểm trên vẫn xanh cả khi chặn nhầm CẢ MỌI vai trò khỏi khu học tập
    dangNhapVoiVaiTro('CREATOR')

    renderAt('/quizzes', <ProtectedRoute roles={['LEARNER', 'CREATOR']}><NoiDung /></ProtectedRoute>)

    expect(screen.getByText('NỘI DUNG ĐƯỢC BẢO VỆ')).toBeInTheDocument()
  })

  it('ADMIN vào được khu vực của CREATOR', () => {
    dangNhapVoiVaiTro('ADMIN')

    renderAt('/ai/generate',
      <ProtectedRoute roles={['CREATOR', 'ADMIN']}><NoiDung /></ProtectedRoute>)

    expect(screen.getByText('NỘI DUNG ĐƯỢC BẢO VỆ')).toBeInTheDocument()
  })

  it('chưa đọc xong phiên đã lưu thì CHỜ, không điều hướng', () => {
    // Điều hướng sớm ở đây làm trang nháy sang /login mỗi lần F5 dù phiên vẫn còn
    tokenStorage.save('access', 'refresh')
    useAuthStore.setState({ user: null, isReady: false })

    renderAt('/my-quizzes', <ProtectedRoute><NoiDung /></ProtectedRoute>)

    expect(screen.queryByText('TRANG ĐĂNG NHẬP')).not.toBeInTheDocument()
    expect(screen.queryByText('NỘI DUNG ĐƯỢC BẢO VỆ')).not.toBeInTheDocument()
  })

  it('còn refresh token dù access token đã mất thì vẫn coi là đang đăng nhập', () => {
    // Access token sống 15 phút nên sự tồn tại của nó chưa bao giờ là bằng chứng phiên còn sống.
    // Thứ quyết định là refresh token — chặn ở đây thì người dùng bị đẩy ra TRƯỚC KHI có request nào
    // kịp làm mới token.
    tokenStorage.save('', 'refresh-con-song')
    useAuthStore.setState({
      user: { id: 'u1', email: 'a@example.com', displayName: 'A', avatarUrl: null,
              role: 'LEARNER', createdAt: '2026-08-14T00:00:00Z' },
      isReady: true,
    })

    renderAt('/my-attempts', <ProtectedRoute><NoiDung /></ProtectedRoute>)

    expect(screen.getByText('NỘI DUNG ĐƯỢC BẢO VỆ')).toBeInTheDocument()
  })
})
