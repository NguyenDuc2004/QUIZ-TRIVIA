import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Avatar, Button, Dropdown, Input, Layout, Space, Tag, Typography } from 'antd'
import type { MenuProps } from 'antd'
import {
  DownOutlined,
  LogoutOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { useLogout } from '@/features/auth/hooks/useAuthMutations'
import { useAuthStore } from '@/features/auth/store/authStore'
import NotificationBell from '@/features/notification/components/NotificationBell'
import { useNotificationSocket } from '@/features/notification/hooks/useNotificationSocket'

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

  // Một kết nối WebSocket cho cả phiên đăng nhập, gắn ở layout vì thông báo tới bất cứ lúc nào ở bất cứ
  // trang nào (features/16, FR-67). Hook tự bỏ qua khi chưa đăng nhập.
  useNotificationSocket()
  const isAdmin = user?.role === 'ADMIN'

  // Màu chữ phải có hậu tố `!`. Đây là thẻ <a>, và Ant Design chèn CSS `a { color }` lúc chạy ở NGOÀI
  // cascade layer, còn utility Tailwind v4 nằm TRONG @layer — luật ngoài layer thắng luật trong layer.
  // Thiếu `!` thì cả mục đang mở lẫn mục chưa mở đều ra màu link của antd, tức mất luôn dấu hiệu
  // "đang ở trang nào".
  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `text-sm font-bold whitespace-nowrap ${
      isActive ? 'text-brand-strong!' : 'text-ink! hover:text-brand-strong!'
    }`

  // Đăng xuất nằm dưới một đường kẻ và là mục cuối: nó là hành động duy nhất trong menu không thể
  // hoàn tác bằng một lần bấm nữa, nên không đặt cạnh mục điều hướng thường.
  //
  // Lối vào khu quản trị nằm ở đây chứ KHÔNG phải một mục trên thanh điều hướng: nó không phải nơi
  // người ta ghé qua khi đang học, mà là chuyển sang một ngữ cảnh làm việc khác có layout riêng
  // (docs/ui-design-system.md §1). Thanh ngang cũng đã có nhiều mục và sẽ tràn hàng.
  const accountMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Trang cá nhân',
      onClick: () => navigate('/profile'),
    },
    ...(isAdmin
      ? [
          { type: 'divider' as const },
          {
            key: 'admin',
            icon: <SettingOutlined />,
            label: 'Khu quản trị',
            onClick: () => navigate('/admin'),
          },
        ]
      : []),
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

          {/* Gom các mục cá nhân của người học vào một menu. Bảy mục phẳng trước đây tràn hàng trên màn
              hình hẹp, và chúng vốn thuộc hai việc khác nhau: học và soạn nội dung. */}
          <NavGroup label="Học tập" items={MUC_HOC_TAP} />

          {/* Nhóm công cụ soạn nội dung, chỉ hiện với CREATOR và ADMIN */}
          {canCreate && <NavGroup label="Thư viện" items={MUC_THU_VIEN} />}
        </nav>

        <Space size={8} className="ml-auto shrink-0">
          {/*
            "Sinh đề AI" là HÀNH ĐỘNG, không phải điều hướng — nên nó là nút bấm, không nằm ngang hàng
            với các mục menu. Trước đây nó là một link giữa bảy link khác và chìm hoàn toàn.

            Màu đen chứ KHÔNG gradient: `ui-design-system.md §5` quy định nút hành động chính màu đen
            và tím chỉ dùng cho link. Một nút gradient ở đây sẽ là thứ duy nhất trong cả ứng dụng trông
            như vậy, và làm nó nổi bằng cách phá quy ước màu thì phần còn lại của giao diện trả giá.
            Icon ✨ đủ để nó khác mọi nút đen khác mà không cần đổi màu.
          */}
          {canCreate && (
            <Link to="/ai/generate" className="hidden sm:block">
              <Button type="primary" icon={<span aria-hidden>✨</span>}>
                Sinh đề AI
              </Button>
            </Link>
          )}
          {/* Chuông đứng TRƯỚC avatar: thông báo là thứ người dùng nhìn thường xuyên hơn menu tài khoản,
              và đặt sau avatar thì nó rơi ra sát mép phải màn hình */}
          {user && <NotificationBell />}
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

/** Một mục trong menu nhóm. */
interface MucMenu {
  to: string
  label: string
  moTa: string
}

const MUC_HOC_TAP: MucMenu[] = [
  { to: '/flashcards', label: 'Thẻ ghi nhớ', moTa: 'Ôn theo lịch lặp lại ngắt quãng' },
  { to: '/achievements', label: 'Thành tích', moTa: 'XP, cấp độ, chuỗi ngày học và huy hiệu' },
  { to: '/leaderboard', label: 'Xếp hạng mùa', moTa: 'So điểm với người học khác trong mùa này' },
  { to: '/learning-path', label: 'Lộ trình học', moTa: 'Thứ tự chủ đề nên ôn, gợi ý từ đồ thị hành vi' },
  { to: '/my-progress', label: 'Tiến độ', moTa: 'Điểm theo thời gian, mạnh yếu theo chủ đề' },
  { to: '/my-attempts', label: 'Lịch sử làm bài', moTa: 'Các bài đã làm và kết quả' },
]

const MUC_THU_VIEN: MucMenu[] = [
  { to: '/my-quizzes', label: 'Quiz của tôi', moTa: 'Đề đã soạn và thống kê từng đề' },
  { to: '/question-bank', label: 'Ngân hàng câu hỏi', moTa: 'Câu hỏi dùng lại được cho nhiều đề' },
  { to: '/ai/materials', label: 'Học liệu', moTa: 'Tài liệu nguồn cho trợ lý và sinh đề' },
]

/**
 * Một nhóm mục điều hướng dạng menu xổ xuống.
 *
 * Điểm phải xử lý: menu xổ xuống **giấu mất dấu hiệu "đang ở trang nào"** — mở trang Thẻ ghi nhớ thì cả
 * thanh menu không có gì sáng lên, vì mục đó nằm bên trong menu đã đóng. Nên nhãn nhóm tự sáng khi một
 * trang con của nó đang mở, và mục con đó được đánh dấu trong menu.
 *
 * Mỗi mục kèm một dòng mô tả: gom vào menu làm mất khả năng đọc hết mọi mục bằng một cái nhìn, dòng mô tả
 * bù lại phần đó cho người chưa quen.
 */
function NavGroup({ label, items }: { label: string; items: MucMenu[] }) {
  const location = useLocation()
  const navigate = useNavigate()

  const dangMo = items.some(
    (m) => location.pathname === m.to || location.pathname.startsWith(m.to + '/'),
  )

  const menuItems: MenuProps['items'] = items.map((m) => ({
    key: m.to,
    label: (
      <div className="py-0.5">
        <div className="text-sm font-bold">{m.label}</div>
        <div className="text-ink-soft text-xs">{m.moTa}</div>
      </div>
    ),
    onClick: () => navigate(m.to),
  }))

  return (
    <Dropdown
      menu={{
        items: menuItems,
        // Đánh dấu mục con đang mở để menu cũng nói được vị trí hiện tại, không chỉ nhãn nhóm
        selectedKeys: items.filter((m) => location.pathname.startsWith(m.to)).map((m) => m.to),
      }}
      trigger={['click']}
    >
      <button
        type="button"
        className={`flex cursor-pointer items-center gap-1 border-0 bg-transparent p-0 text-sm font-bold whitespace-nowrap ${
          dangMo ? 'text-brand-strong!' : 'text-ink! hover:text-brand-strong!'
        }`}
      >
        {label}
        <DownOutlined className="text-[10px]" />
      </button>
    </Dropdown>
  )
}
