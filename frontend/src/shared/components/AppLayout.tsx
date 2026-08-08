import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Button, Input, Layout, Space, Tag, Typography } from 'antd'
import { useLogout } from '@/features/auth/hooks/useAuthMutations'
import { useAuthStore } from '@/features/auth/store/authStore'

const { Header, Content } = Layout
const { Text } = Typography

const ROLE_LABEL: Record<string, string> = {
  LEARNER: 'Người học',
  CREATOR: 'Người tạo nội dung',
  ADMIN: 'Quản trị viên',
}

const ROLE_COLOR: Record<string, string> = {
  LEARNER: 'green',
  CREATOR: 'geekblue',
  ADMIN: 'volcano',
}

/**
 * Khung chung sau khi đăng nhập — header trắng dính trên, ô tìm kiếm ở giữa,
 * menu bên phải (docs/ui-design-system.md §6).
 */
export default function AppLayout() {
  const user = useAuthStore((state) => state.user)
  const logout = useLogout()
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')

  const canCreate = user?.role === 'CREATOR' || user?.role === 'ADMIN'

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-bold whitespace-nowrap ${
      isActive ? 'text-brand-strong' : 'text-ink hover:text-brand-strong'
    }`

  return (
    <Layout className="min-h-screen">
      <Header className="sticky top-0 z-10 flex items-center gap-6 border-b border-line bg-white! px-6!">
        <Link to="/quizzes" className="flex items-center gap-1 whitespace-nowrap">
          <span className="text-lg font-extrabold text-ink">Quiz</span>
          <span className="text-lg font-extrabold text-brand">AI</span>
        </Link>

        {/* Ô tìm kiếm dạng viên thuốc, gửi từ khoá sang trang Khám phá quiz */}
        <Input.Search
          allowClear
          placeholder="Tìm quiz theo tiêu đề"
          className="max-w-xl flex-1"
          style={{ borderRadius: 9999 }}
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onSearch={(value) =>
            navigate(value ? `/quizzes?q=${encodeURIComponent(value)}` : '/quizzes')
          }
        />

        <nav className="flex items-center gap-5">
          <NavLink to="/quizzes" className={navLinkClass}>
            Khám phá
          </NavLink>
          <NavLink to="/rooms" className={navLinkClass}>
            Phòng đấu
          </NavLink>
          <NavLink to="/learning-path" className={navLinkClass}>
            Lộ trình
          </NavLink>
          <NavLink to="/my-progress" className={navLinkClass}>
            Tiến độ
          </NavLink>
          <NavLink to="/my-attempts" className={navLinkClass}>
            Lịch sử
          </NavLink>
          {canCreate && (
            <>
              <NavLink to="/my-quizzes" className={navLinkClass}>
                Quiz của tôi
              </NavLink>
              <NavLink to="/question-bank" className={navLinkClass}>
                Ngân hàng câu hỏi
              </NavLink>
              <NavLink to="/ai/generate" className={navLinkClass}>
                Sinh đề AI
              </NavLink>
            </>
          )}
        </nav>

        <Space size={8} className="ml-auto shrink-0">
          {user && (
            <Link to="/profile" className="flex items-center gap-2">
              <Text className="text-ink! text-sm font-bold">{user.displayName}</Text>
              <Tag color={ROLE_COLOR[user.role]} className="mr-0!">
                {ROLE_LABEL[user.role] ?? user.role}
              </Tag>
            </Link>
          )}
          <Button size="small" loading={logout.isPending} onClick={() => logout.mutate()}>
            Đăng xuất
          </Button>
        </Space>
      </Header>

      <Content className="px-6 py-8">
        <div className="mx-auto max-w-6xl">
          <Outlet />
        </div>
      </Content>
    </Layout>
  )
}
