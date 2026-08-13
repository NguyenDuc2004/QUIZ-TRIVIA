import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Avatar, Dropdown, Input, Layout, Space, Tag, Typography } from 'antd'
import type { MenuProps } from 'antd'
import { DownOutlined, LogoutOutlined, UserOutlined } from '@ant-design/icons'
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

  // Đăng xuất nằm dưới một đường kẻ và là mục cuối: nó là hành động duy nhất trong menu không thể
  // hoàn tác bằng một lần bấm nữa, nên không đặt cạnh mục điều hướng thường.
  const accountMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Trang cá nhân',
      onClick: () => navigate('/profile'),
    },
    { type: 'divider' },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Đăng xuất',
      disabled: logout.isPending,
      onClick: () => logout.mutate(),
    },
  ]

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
          <NavLink to="/assistant" className={navLinkClass}>
            Trợ lý AI
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
              <NavLink to="/ai/materials" className={navLinkClass}>
                Học liệu
              </NavLink>
              <NavLink to="/ai/generate" className={navLinkClass}>
                Sinh đề AI
              </NavLink>
            </>
          )}
        </nav>

        <Space size={8} className="ml-auto shrink-0">
          {user && (
            <Dropdown menu={{ items: accountMenuItems }} trigger={['click']} placement="bottomRight">
              {/* Vùng bấm gộp avatar + tên + vai trò: cả khối là một đích bấm, không phải ba đích
                  cạnh nhau. `DownOutlined` để người dùng biết đây là menu xổ xuống chứ không phải
                  một đường dẫn — bỏ nó đi thì không có gì báo rằng bấm vào sẽ mở thêm lựa chọn. */}
              <button
                type="button"
                className="flex cursor-pointer items-center gap-2 border-0 bg-transparent p-0"
              >
                <Avatar
                  size={28}
                  src={user.avatarUrl ?? undefined}
                  icon={<UserOutlined />}
                  className="shrink-0"
                >
                  {user.displayName?.trim().charAt(0).toUpperCase()}
                </Avatar>
                <Text className="text-ink! text-sm font-bold">{user.displayName}</Text>
                <Tag color={ROLE_COLOR[user.role]} className="mr-0!">
                  {ROLE_LABEL[user.role] ?? user.role}
                </Tag>
                <DownOutlined className="text-ink-soft text-[10px]" />
              </button>
            </Dropdown>
          )}
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
