import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Avatar, Dropdown, Layout, Tag, Tooltip, Typography } from 'antd'
import type { MenuProps } from 'antd'
import {
  DashboardOutlined,
  DownOutlined,
  FileProtectOutlined,
  LineChartOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  PlayCircleOutlined,
  SafetyOutlined,
  TagsOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { useLogout } from '@/features/auth/hooks/useAuthMutations'
import { useAuthStore } from '@/features/auth/store/authStore'
import ThemeToggle from '@/shared/components/ThemeToggle'

const { Sider, Header, Content } = Layout
const { Text } = Typography

/**
 * Khung riêng cho khu quản trị — **cố ý khác hẳn** {@code AppLayout} của khu học tập.
 *
 * Vì sao không dùng chung layout và chỉ thêm mục vào menu (docs/ui-design-system.md §1):
 * - **Trông khác là một lớp an toàn.** Thao tác ở đây tác động lên *người khác* (khoá tài khoản, đổi vai
 *   trò, ẩn quiz) và không có nút hoàn tác. Nền tối cùng bố cục sidebar khiến admin luôn biết mình đang
 *   ở khu quản trị, thay vì tưởng vẫn ở trang cá nhân rồi bấm nhầm. Đây là lý do đầu, không phải thẩm mỹ.
 * - **Ngữ cảnh làm việc khác.** Menu "Khám phá / Phòng đấu / Trợ lý AI / Lộ trình / Tiến độ" không liên
 *   quan gì khi đang đọc chi phí AI.
 * - **Sidebar mở rộng được** — bảy mục vẫn gọn, còn thanh ngang khu học tập đã phải gom nhóm ở 10 mục.
 *
 * <b>Không có lối ra sang khu học tập.</b> Quản trị viên đăng nhập là vào thẳng đây và ở lại đây: khu học
 * tập không có gì cho họ, và để một tài khoản có quyền tác động lên người khác đi lang thang giữa dữ liệu của
 * người khác là mở một cửa không cần thiết. Hai thứ họ từng phải sang bên kia mới làm được — xem hồ sơ và xem
 * nội dung một quiz để kiểm duyệt — nay đều có bản riêng trong khu này.
 */
export default function AdminLayout() {
  const user = useAuthStore((state) => state.user)
  const logout = useLogout()
  const navigate = useNavigate()

  // Thu gọn có nút bấm, KHÔNG để antd tự ẩn sidebar theo breakpoint mà không có gì mở lại: bản trước
  // dùng `breakpoint="lg" collapsedWidth={0}` mà không bật `collapsible`, nên trên màn hình hẹp sidebar
  // biến mất và admin không còn đường điều hướng nào.
  const [thuGon, setThuGon] = useState(false)

  const accountMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Hồ sơ của tôi',
      onClick: () => navigate('/admin/profile'),
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
      <Sider
        width={244}
        collapsible
        collapsed={thuGon}
        onCollapse={setThuGon}
        trigger={null}
        collapsedWidth={72}
        breakpoint="lg"
        className="bg-admin-bg! border-admin-line border-r"
      >
        {/* Sidebar dính theo màn hình: danh sách mục không được cuộn mất khi trang nội dung dài */}
        <div className="sticky top-0 flex h-screen flex-col">
          <div
            className={`border-admin-line flex h-14 shrink-0 items-center border-b ${
              thuGon ? 'justify-center px-0' : 'px-4'
            }`}
          >
            <Link to="/admin" className="flex items-center gap-2.5 overflow-hidden">
              {/* Khối chữ Q làm dấu nhận diện: khi thu gọn còn 72px thì đây là thứ duy nhất còn thấy */}
              <span className="bg-brand flex size-8 shrink-0 items-center justify-center rounded-lg text-sm font-extrabold text-white">
                Q
              </span>
              {!thuGon && (
                <span className="min-w-0">
                  <span className="block text-sm leading-tight font-extrabold text-white">Quiz AI</span>
                  <span className="block text-[11px] leading-tight text-white/70">Khu quản trị</span>
                </span>
              )}
            </Link>
          </div>

          <nav className="min-h-0 flex-1 overflow-y-auto py-3">
            {/* Nhóm đầu không có nhãn: đặt nhãn "Tổng quan" lên trên mục "Tổng quan" là lặp lại đúng
                một chữ hai lần, chiếm dòng mà không thêm thông tin nào */}
            <NavGroup thuGon={thuGon}>
              <NavItem to="/admin" end icon={<DashboardOutlined />} label="Tổng quan" thuGon={thuGon} />
            </NavGroup>

            {/* Gom nhóm theo đối tượng bị tác động, và giữ nguyên thứ tự cũ để không phá trí nhớ vị trí */}
            <NavGroup label="Người dùng & nội dung" thuGon={thuGon}>
              <NavItem to="/admin/users" icon={<TeamOutlined />} label="Người dùng" thuGon={thuGon} />
              <NavItem to="/admin/categories" icon={<TagsOutlined />} label="Danh mục" thuGon={thuGon} />
              <NavItem
                to="/admin/quizzes"
                icon={<FileProtectOutlined />}
                label="Kiểm duyệt quiz"
                thuGon={thuGon}
              />
              <NavItem
                to="/admin/integrity"
                icon={<SafetyOutlined />}
                label="Rà soát bài thi"
                thuGon={thuGon}
              />
            </NavGroup>

            <NavGroup label="Giám sát" thuGon={thuGon}>
              <NavItem
                to="/admin/rooms"
                icon={<PlayCircleOutlined />}
                label="Phòng đấu"
                thuGon={thuGon}
              />
              <NavItem to="/admin/ai" icon={<LineChartOutlined />} label="Giám sát AI" thuGon={thuGon} />
            </NavGroup>
          </nav>

          {/*
            KHÔNG còn lối "Về khu học tập". Quản trị viên chỉ làm việc trong khu này, nên một đường dẫn
            sang khu học tập giờ chỉ là một cú đá ngược về `/admin` — một nút bấm vào thì không có gì xảy
            ra là tệ hơn không có nút.
            Chỗ đó nay là Hồ sơ: trước kia admin đổi mật khẩu bằng cách sang khu học tập, và chặn xong mà
            không thay thế thì họ mất luôn đường đổi mật khẩu của chính mình.
          */}
          <div className="border-admin-line shrink-0 border-t p-2">
            <NavItem to="/admin/profile" icon={<UserOutlined />} label="Hồ sơ của tôi" thuGon={thuGon} />
          </div>
        </div>
      </Sider>

      <Layout>
        <Header className="border-line sticky top-0 z-10 flex h-14! items-center gap-3 border-b bg-surface! px-4!">
          <button
            type="button"
            onClick={() => setThuGon((truoc) => !truoc)}
            aria-label={thuGon ? 'Mở rộng thanh điều hướng' : 'Thu gọn thanh điều hướng'}
            className="text-ink-soft hover:text-ink hover:bg-surface-subtle flex size-8 cursor-pointer items-center justify-center rounded-lg border-0 bg-transparent transition-colors"
          >
            {thuGon ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </button>
          <Text className="text-sm font-bold">Quản trị hệ thống</Text>

          {/* Nút đổi giao diện — cùng nút với khu học tập.

              Đặt NGOÀI khối `{user && ...}`: đây là thiết lập hiển thị, không phụ thuộc việc đã tải
              xong thông tin người dùng hay chưa. `ml-auto` để nó cùng khối tài khoản dạt sang phải. */}
          <div className="ml-auto">
            <ThemeToggle />
          </div>

          {user && (
            <Dropdown
              menu={{ items: accountMenuItems }}
              trigger={['click']}
              placement="bottomRight"
            >
              {/* Cả khối avatar + tên + thẻ vai trò là một đích bấm, không phải ba đích cạnh nhau */}
              <button
                type="button"
                className="hover:bg-surface-subtle flex shrink-0 cursor-pointer items-center gap-2 rounded-lg border-0 bg-transparent px-2 py-1.5 transition-colors"
              >
                <Avatar size={26} src={user.avatarUrl ?? undefined}>
                  {user.displayName?.charAt(0).toUpperCase()}
                </Avatar>
                <Text className="text-ink! hidden text-sm font-bold sm:inline">{user.displayName}</Text>
                <Tag color="volcano" className="mr-0! hidden sm:inline-block">
                  Quản trị viên
                </Tag>
                <DownOutlined className="text-ink-soft text-[10px]" />
              </button>
            </Dropdown>
          )}
        </Header>

        <Content className="bg-surface-subtle px-4 py-6 sm:px-6">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

/**
 * Nhóm mục điều hướng kèm nhãn nhỏ.
 *
 * Khi thu gọn thì nhãn nhóm biến mất và chỉ còn một đường kẻ mảnh: giữ chữ ở bề rộng 72px thì nó bị cắt
 * thành mấy ký tự vô nghĩa, còn bỏ hẳn ranh giới nhóm thì bảy icon xếp liền nhau thành một dãy khó đọc.
 */
function NavGroup({
  label,
  thuGon,
  children,
}: {
  label?: string
  thuGon: boolean
  children: React.ReactNode
}) {
  return (
    <div className="px-2 pb-2 [&+&]:border-t [&+&]:border-admin-line [&+&]:pt-2">
      {/* Nhãn nhóm cố ý mờ hơn mục điều hướng: nó là chú thích, không phải thứ bấm được. Để trắng đều
          thì nó cạnh tranh với chính các mục nó đang giới thiệu */}
      {label && !thuGon && (
        <div className="px-3 pt-1 pb-1.5 text-[10px] font-bold tracking-wider text-white/45 uppercase">
          {label}
        </div>
      )}
      <div className="flex flex-col gap-0.5">{children}</div>
    </div>
  )
}

/**
 * Một mục điều hướng.
 *
 * Chữ trắng đều cho mọi mục. Mục đang chọn KHÔNG phân biệt bằng độ sáng chữ mà bằng <b>nền sáng nhẹ +
 * viền tím mảnh + icon tím + chữ đậm</b> — bốn dấu hiệu cùng lúc, nên bỏ cách làm mờ chữ mục chưa chọn
 * vẫn không mất chỗ đứng. Tím dành cho điều hướng theo quy ước màu (§2), nút hành động vẫn màu đen.
 * <p>
 * Màu chữ và màu icon <b>bắt buộc dùng hậu tố {@code !}</b>. Đây là thẻ {@code <a>}, và Ant Design chèn
 * CSS {@code a { color }} lúc chạy ở <i>ngoài</i> cascade layer, còn utility của Tailwind v4 nằm
 * <i>trong</i> {@code @layer} — luật ngoài layer thắng luật trong layer, nên {@code text-white} thường
 * bị đè và cả nhãn lẫn icon thừa hưởng màu tím của link. Icon cũng phải đặt màu tường minh vì nó chỉ
 * thừa hưởng màu từ thẻ {@code a}.
 *
 * Viền vẽ bằng {@code ring} thay vì {@code border}: border thêm 1px vào hộp và làm mục đang chọn cao
 * hơn các mục khác đúng 2px, đủ để cả danh sách nhấp lên xuống mỗi lần đổi trang.
 */
function NavItem({
  to,
  end,
  icon,
  label,
  thuGon,
}: {
  to: string
  end?: boolean
  icon: React.ReactNode
  label: string
  thuGon: boolean
}) {
  return (
    <Tooltip title={thuGon ? label : ''} placement="right">
      <NavLink
        to={to}
        end={end}
        className={({ isActive }) =>
          `flex items-center gap-3 rounded-lg py-2.5 text-sm transition-colors ${
            thuGon ? 'justify-center px-0' : 'px-3'
          } ${
            isActive
              ? 'ring-brand/40 bg-surface/12 font-bold text-white! ring-1'
              : 'text-white! hover:bg-surface/10'
          }`
        }
      >
        {({ isActive }) => (
          <>
            <span className={`shrink-0 text-base ${isActive ? 'text-brand!' : 'text-white!'}`}>
              {icon}
            </span>
            {!thuGon && <span className="truncate">{label}</span>}
          </>
        )}
      </NavLink>
    </Tooltip>
  )
}
