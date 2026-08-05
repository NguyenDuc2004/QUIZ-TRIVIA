import { Link, Outlet, useLocation } from 'react-router-dom'
import { Button, Layout, Menu, Space, Tag, Typography } from 'antd'
import { useLogout } from '@/features/auth/hooks/useAuthMutations'
import { useAuthStore } from '@/features/auth/store/authStore'

const { Header, Content } = Layout
const { Text } = Typography

const ROLE_LABEL: Record<string, string> = {
  LEARNER: 'Người học',
  CREATOR: 'Người tạo nội dung',
  ADMIN: 'Quản trị viên',
}

/** Khung chung cho các trang sau khi đăng nhập. */
export default function AppLayout() {
  const user = useAuthStore((state) => state.user)
  const logout = useLogout()
  const { pathname } = useLocation()

  const canCreate = user?.role === 'CREATOR' || user?.role === 'ADMIN'

  const items = [
    { key: '/quizzes', label: <Link to="/quizzes">Khám phá quiz</Link> },
    ...(canCreate
      ? [
          { key: '/my-quizzes', label: <Link to="/my-quizzes">Quiz của tôi</Link> },
          { key: '/question-bank', label: <Link to="/question-bank">Ngân hàng câu hỏi</Link> },
        ]
      : []),
    { key: '/profile', label: <Link to="/profile">Hồ sơ</Link> },
  ]

  // Route con của /my-quizzes/:id vẫn phải sáng mục "Quiz của tôi"
  const selectedKey = items.map((item) => item.key).find((key) => pathname.startsWith(key)) ?? '/quizzes'

  return (
    <Layout className="min-h-screen">
      <Header className="flex items-center gap-6 px-6">
        <Link to="/quizzes" className="text-white text-base font-semibold whitespace-nowrap">
          Quiz/Trivia AI
        </Link>
        <Menu
          theme="dark"
          mode="horizontal"
          selectedKeys={[selectedKey]}
          items={items}
          className="flex-1 min-w-0"
        />
        <Space>
          {user && (
            <Space size={4}>
              <Text className="!text-white">{user.displayName}</Text>
              <Tag color={user.role === 'ADMIN' ? 'volcano' : user.role === 'CREATOR' ? 'geekblue' : 'green'}>
                {ROLE_LABEL[user.role] ?? user.role}
              </Tag>
            </Space>
          )}
          <Button size="small" loading={logout.isPending} onClick={() => logout.mutate()}>
            Đăng xuất
          </Button>
        </Space>
      </Header>

      <Content className="p-6">
        <div className="mx-auto max-w-6xl">
          <Outlet />
        </div>
      </Content>
    </Layout>
  )
}
