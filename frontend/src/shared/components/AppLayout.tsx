import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Avatar, Button, Drawer, Dropdown, Input, Layout, Space, Tag, Typography } from 'antd'
import type { MenuProps } from 'antd'
import {
  DesktopOutlined,
  DownOutlined,
  HistoryOutlined,
  LogoutOutlined,
  MenuOutlined,
  MoonOutlined,
  SearchOutlined,
  SunOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { useLogout } from '@/features/auth/hooks/useAuthMutations'
import AppFooter from './AppFooter'
import ThemeToggle from './ThemeToggle'
import { useAuthStore } from '@/features/auth/store/authStore'
import { useThemeStore, type CheDoMau } from '@/shared/theme/themeStore'
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
  const [moMenu, setMoMenu] = useState(false)
  const cheDoMau = useThemeStore((s) => s.cheDo)
  const datCheDo = useThemeStore((s) => s.datCheDo)

  const canCreate = user?.role === 'CREATOR' || user?.role === 'ADMIN'

  /**
   * Màn cần TOÀN BỘ sự chú ý thì không có chân trang: đang làm bài và đang ôn thẻ.
   *
   * Ở chế độ thi nghiêm ngặt (FR-48) mọi link dẫn ra ngoài là một chỗ để người thi bấm nhầm — thoát ra
   * giữa bài rồi bị ghi nhận là rời trang.
   *
   * KHÔNG liệt kê `/rooms/:code` ở đây dù phòng đấu cũng cần tập trung: trang đó nằm NGOÀI `AppLayout`
   * (nó công khai cho khách vãng lai quét QR), nên một điều kiện cho nó ở đây sẽ không bao giờ chạy — và
   * một nhánh chết kèm chú thích tự tin là thứ khiến người đọc sau tin nhầm rằng chỗ này đã lo liệu rồi.
   */
  const location = useLocation()
  const manTapTrung =
    location.pathname.startsWith('/attempts/') ||
    location.pathname.startsWith('/flashcards/review')

  // Một kết nối WebSocket cho cả phiên đăng nhập, gắn ở layout vì thông báo tới bất cứ lúc nào ở bất cứ
  // trang nào (features/16, FR-67). Hook tự bỏ qua khi chưa đăng nhập.
  useNotificationSocket()

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
    {
      // Lịch sử làm bài nằm ở ĐÂY chứ không trong menu "Học tập", và chỉ ở một chỗ.
      //
      // Menu "Học tập" là nơi chọn *việc sắp làm* — vào lớp, ôn thẻ, xem lộ trình. Lịch sử làm bài
      // ngược lại: nó là *hồ sơ của riêng mình*, cùng loại với Trang cá nhân, nên thuộc về menu tài
      // khoản. Đặt đúng nhóm thì người dùng đoán được chỗ tìm mà không phải nhớ.
      //
      // Không để cả hai nơi: hai lối vào cùng một trang làm người dùng dừng lại tự hỏi chúng có khác
      // nhau không, và mỗi lần thêm mục mới lại phải quyết định nhân đôi hay không.
      key: 'attempts',
      icon: <HistoryOutlined />,
      label: 'Lịch sử làm bài',
      onClick: () => navigate('/my-attempts'),
    },
    // KHÔNG còn mục "Khu quản trị" ở đây. Quản trị viên không vào được layout này nữa (route khu học tập
    // chỉ nhận LEARNER và CREATOR), nên mục đó là một nhánh không ai chạy tới — và một nhánh chết trong
    // giao diện thì lần sau đọc code sẽ tưởng Admin vẫn qua lại được giữa hai khu.
    { type: 'divider' },
    {
      /*
       * Chế độ màu là menu con, không phải một công tắc bật/tắt.
       *
       * Công tắc chỉ diễn tả được hai trạng thái, mà ở đây có ba — và trạng thái thứ ba mới là mặc
       * định: "theo hệ thống" nghĩa là trang tự sáng ban ngày, tự tối ban đêm theo thiết lập máy.
       * Ép nó vào một công tắc thì hoặc mất trạng thái đó, hoặc người dùng không hiểu công tắc đang
       * ở đâu khi hệ thống vừa đổi.
       */
      key: 'theme',
      icon: cheDoMau === 'dark' ? <MoonOutlined /> : <SunOutlined />,
      label: 'Giao diện',
      children: [
        {
          key: 'theme-light',
          icon: <SunOutlined />,
          label: cheDoMau === 'light' ? 'Sáng ✓' : 'Sáng',
          onClick: () => datCheDo('light' as CheDoMau),
        },
        {
          key: 'theme-dark',
          icon: <MoonOutlined />,
          label: cheDoMau === 'dark' ? 'Tối ✓' : 'Tối',
          onClick: () => datCheDo('dark' as CheDoMau),
        },
        {
          key: 'theme-system',
          icon: <DesktopOutlined />,
          label: cheDoMau === 'system' ? 'Theo hệ thống ✓' : 'Theo hệ thống',
          onClick: () => datCheDo('system' as CheDoMau),
        },
      ],
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
    /* `min-h-screen!` — hậu tố `!` là BẮT BUỘC, và đây chính là chỗ gây lỗi "chân trang trồi lên giữa
       màn hình" ở những trang nội dung ngắn.

       Ant Design đặt `.ant-layout { min-height: 0 }` (để Firefox co được flex item), và CSS đó chèn ở
       NGOÀI layer của Tailwind. Theo luật cascade, luật ngoài layer THẮNG luật trong layer — nên
       `min-h-screen` không có `!` bị đè bằng 0, trang chỉ cao bằng nội dung, và chân trang dừng lại
       ngay dưới nội dung thay vì ở đáy màn hình.

       Không cần thêm gì cho phần nội dung: Ant Design đã đặt sẵn `.ant-layout-content { flex: auto }`
       nên nó tự giãn — chỉ thiếu đúng chiều cao tối thiểu của khung ngoài. */
    /* `overflow-x-clip` là chốt cuối, không phải giải pháp chính.

       Quy ước của dự án: nội dung rộng (bảng, sơ đồ, khối mã) phải tự cuộn trong khung của chính nó,
       thân trang KHÔNG bao giờ cuộn ngang. Khi một phần tử nào đó lỡ rộng quá — và trên hàng trăm màn
       hình thì sớm muộn sẽ có — hệ quả không dừng ở phần tử đó: cả trang bị đẩy sang phải, thanh điều
       hướng và tiêu đề bị cắt mất bên trái, và người dùng không hiểu vì sao. Đúng lỗi trong hai ảnh
       chụp mà người dùng gửi.

       Dùng `clip` chứ không `hidden`: `overflow-hidden` biến phần tử thành khối cuộn, làm hỏng
       `position: sticky` của thanh điều hướng ngay bên trong. */
    <Layout className="min-h-screen! overflow-x-clip">
      {/* `gap-3` trên màn hẹp, `gap-6` từ `lg` trở lên: khoảng cách 24px giữa bảy phần tử là quá rộng
          khi chỉ còn 360px chiều ngang. */}
      <Header className="sticky top-0 z-10 flex items-center gap-3 border-b border-line bg-surface! px-4! lg:gap-6 lg:px-6!">
        {/* Nút mở ngăn kéo điều hướng — chỉ hiện dưới `lg`, đúng chỗ dàn menu ngang bị ẩn đi.

            Bọc trong `div` cùng lý do với ô tìm kiếm bên dưới: `<Button>` của Ant Design có luật
            `display` riêng, đặt lớp ẩn/hiện thẳng lên nó là can thiệp vào bố cục nội bộ của component.
            Với `Button` thì hậu quả nhẹ hơn `Input.Search`, nhưng nguyên tắc thì giống nhau — và làm
            đúng ở cả hai chỗ thì không phải nhớ chỗ nào an toàn chỗ nào không. */}
        <div className="lg:hidden">
          <Button
            type="text"
            aria-label="Mở menu"
            icon={<MenuOutlined />}
            onClick={() => setMoMenu(true)}
          />
        </div>

        <Link to="/quizzes" className="flex items-center gap-1 whitespace-nowrap">
          <span className="text-lg font-extrabold text-ink">Quiz</span>
          <span className="text-lg font-extrabold text-brand">AI</span>
        </Link>

        {/* Ô tìm kiếm dạng viên thuốc, gửi từ khoá sang trang Khám phá quiz.

            Ẩn dưới `md` và thay bằng một nút kính lúp: ô tìm kiếm chiếm nhiều chiều ngang nhất trong
            thanh này, mà trên điện thoại chiều ngang là thứ khan hiếm nhất. Nút vẫn đưa người dùng tới
            đúng trang Khám phá, nơi đã có sẵn một ô tìm kiếm đầy đủ — không mất chức năng nào. */}
        <Link to="/quizzes" className="md:hidden">
          <Button type="text" aria-label="Tìm quiz" icon={<SearchOutlined />} />
        </Link>

        {/* Ô tìm kiếm bọc trong một `div` của mình, và lớp ẩn/hiện đặt lên DIV chứ không lên
            `Input.Search`.

            Đặt thẳng lên component Ant Design là sai, và sai theo kiểu khó đoán: `Input.Search` không
            phải một ô nhập mà là một NHÓM gồm ô nhập và nút bấm, có bố cục nội bộ riêng. Ép
            `display: block !important` lên vỏ ngoài của nhóm đó làm nhóm vỡ ra — ô nhập trôi lên trên
            thanh, nút kính lúp rơi xuống dòng dưới. Đúng thứ nhìn thấy trên màn hình laptop.
            (Bản trước còn thử thêm hậu tố `!` để thắng CSS của Ant Design; nó thắng thật, và đó chính
            là lúc bố cục vỡ.)

            Div bọc ngoài là phần tử của mình, không có luật nào của Ant Design chạm tới, nên `hidden`
            và `md:block` ăn bình thường mà không cần `!` và không phá gì. */}
        <div className="hidden max-w-xl flex-1 md:block">
          <Input.Search
            allowClear
            placeholder="Tìm quiz theo tiêu đề"
            className="w-full"
            style={{ borderRadius: 9999 }}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            onSearch={(value) =>
              navigate(value ? `/quizzes?q=${encodeURIComponent(value)}` : '/quizzes')
            }
          />
        </div>

        {/* Dàn menu ngang: chỉ từ `lg` trở lên. Dưới ngưỡng đó nó nằm trong ngăn kéo bên dưới —
            năm mục cộng ô tìm kiếm cộng nút cộng chuông cộng avatar trên một hàng thì tràn hẳn ra
            ngoài màn hình điện thoại. */}
        {/* `min-w-0` để dàn menu CO ĐƯỢC khi cần: mặc định flex item không co nhỏ hơn nội dung, nên
            năm mục chữ không xuống dòng sẽ ép cả thanh rộng ra thay vì tự nhường chỗ. Đó là nguyên
            nhân thanh điều hướng vượt quá màn hình ở khoảng 1000–1200px, ngay trên ngưỡng `lg`. */}
        <nav className="hidden min-w-0 items-center gap-5 lg:flex">
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
          <NavGroup
            label="Học tập"
            items={canCreate ? MUC_HOC_TAP : [...MUC_HOC_TAP, MUC_HOC_LIEU_CUA_TOI]}
          />

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
          {/* Nút đổi giao diện đứng trước chuông. Nó là thiết lập hiển thị, không phải thông báo hay
              tài khoản, nên tách khỏi cả hai — và đặt ở đây thì nó có mặt trên mọi trang của khu học
              tập mà không chiếm chỗ của nội dung. */}
          <ThemeToggle />

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
                {/* Tên và nhãn vai trò ẩn dưới `md`.

                    Khối này là thứ chiếm nhiều chiều ngang nhất bên phải thanh — một tên dài cộng nhãn
                    vai trò dễ tới 200px — và nó `shrink-0` nên không bao giờ nhường chỗ. Trên điện thoại
                    đó là phần lớn màn hình dành cho một thông tin người dùng đã biết: họ đang đăng nhập
                    bằng tài khoản của chính họ.

                    Avatar và mũi nhọn thì giữ: avatar là đích bấm, mũi nhọn là dấu hiệu đây là menu.
                    Tên và vai trò vẫn đọc được ngay khi mở menu tài khoản.

                    Khu quản trị đã làm đúng việc này từ trước (`hidden sm:inline`); khu học tập thì
                    chưa — hai khung giao diện lệch nhau ở cùng một chi tiết. */}
                <Text className="text-ink! hidden text-sm font-bold md:inline">
                  {user.displayName}
                </Text>
                <Tag color={ROLE_COLOR[user.role]} className="mr-0! hidden md:inline-block">
                  {ROLE_LABEL[user.role] ?? user.role}
                </Tag>
                <DownOutlined className="text-ink-soft text-[10px]" />
              </button>
            </Dropdown>
          )}
        </Space>
      </Header>

      {/* Ngăn kéo điều hướng cho màn hẹp.

          Dựng lại đúng các mục của dàn menu ngang chứ không rút gọn: người dùng điện thoại cần tới
          đúng những trang đó, và một menu "bản mobile" thiếu mục là cách nhanh nhất để một chức năng
          trở nên vô hình với nửa số người dùng.

          Tự đóng sau mỗi lần chọn — `location` đổi thì đóng, xử lý luôn cả trường hợp bấm vào mục
          đang đứng. */}
      <Drawer
        open={moMenu}
        onClose={() => setMoMenu(false)}
        placement="left"
        width={280}
        title="Điều hướng"
        styles={{ body: { padding: 0 } }}
      >
        <div className="flex flex-col py-2">
          <MucNganKeo to="/quizzes" onChon={() => setMoMenu(false)}>
            Khám phá
          </MucNganKeo>
          <MucNganKeo to="/rooms" onChon={() => setMoMenu(false)}>
            Phòng đấu
          </MucNganKeo>
          <MucNganKeo to="/assistant" onChon={() => setMoMenu(false)}>
            Trợ lý AI
          </MucNganKeo>

          <NhomNganKeo
            label="Học tập"
            items={canCreate ? MUC_HOC_TAP : [...MUC_HOC_TAP, MUC_HOC_LIEU_CUA_TOI]}
            onChon={() => setMoMenu(false)}
          />
          {canCreate && (
            <NhomNganKeo label="Thư viện" items={MUC_THU_VIEN} onChon={() => setMoMenu(false)} />
          )}

          {canCreate && (
            <div className="border-line mt-2 border-t px-4 pt-4">
              <Link to="/ai/generate" onClick={() => setMoMenu(false)}>
                <Button type="primary" block icon={<span aria-hidden>✨</span>}>
                  Sinh đề AI
                </Button>
              </Link>
            </div>
          )}
        </div>
      </Drawer>

      <Content className="px-6 py-8">
        <div className="mx-auto max-w-6xl">
          <Outlet />
        </div>
      </Content>

      {/* Ẩn chân trang ở màn LÀM BÀI và PHÒNG ĐẤU.

          Hai màn đó cần toàn bộ sự chú ý, và ở chế độ thi nghiêm ngặt (FR-48) thì mọi link dẫn ra ngoài
          đều là một chỗ để người thi bấm nhầm — thoát ra giữa bài thi rồi bị ghi nhận là rời trang. */}
      {!manTapTrung && <AppFooter />}
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
  { to: '/classrooms', label: 'Lớp học', moTa: 'Lớp bạn dạy và lớp bạn tham gia' },
  { to: '/my-assignments', label: 'Bài tập của tôi', moTa: 'Bài giáo viên giao, kèm hạn nộp' },
  { to: '/flashcards', label: 'Thẻ ghi nhớ', moTa: 'Ôn theo lịch lặp lại ngắt quãng' },
  { to: '/achievements', label: 'Thành tích', moTa: 'XP, cấp độ, chuỗi ngày học và huy hiệu' },
  { to: '/leaderboard', label: 'Xếp hạng mùa', moTa: 'So điểm với người học khác trong mùa này' },
  { to: '/learning-path', label: 'Lộ trình học', moTa: 'Thứ tự chủ đề nên ôn, gợi ý từ đồ thị hành vi' },
  { to: '/my-progress', label: 'Tiến độ', moTa: 'Điểm theo thời gian, mạnh yếu theo chủ đề' },
  // "Lịch sử làm bài" đã chuyển sang menu tài khoản (dưới avatar) — xem `accountMenuItems`.
]

/**
 * Học liệu đứng ở hai nhóm khác nhau tuỳ vai trò, và đó là chủ ý.
 *
 * Với Creator nó là **nguồn để soạn nội dung** (sinh đề bám tài liệu), nên nằm cùng "Quiz của tôi" và
 * "Ngân hàng câu hỏi" ở nhóm Thư viện. Với người học nó là **tài liệu của chính mình để hỏi trợ lý**,
 * không liên quan gì tới soạn nội dung — xếp nó vào nhóm Thư viện sẽ nói sai về việc họ đang làm gì.
 */
const MUC_HOC_LIEU_CUA_TOI: MucMenu = {
  to: '/ai/materials',
  label: 'Học liệu của tôi',
  moTa: 'Tài liệu bạn nạp lên để hỏi trợ lý AI',
}

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

/** Một mục trong ngăn kéo. Cao 44px — đủ cho ngón tay, theo khuyến nghị vùng chạm tối thiểu. */
function MucNganKeo({
  to,
  onChon,
  children,
}: {
  to: string
  onChon: () => void
  children: React.ReactNode
}) {
  return (
    <NavLink
      to={to}
      onClick={onChon}
      className={({ isActive }) =>
        `px-4 py-3 text-sm no-underline! ${
          isActive ? 'text-brand-strong! bg-surface-subtle font-bold' : 'text-ink!'
        }`
      }
    >
      {children}
    </NavLink>
  )
}

/**
 * Một nhóm mục trong ngăn kéo — trải phẳng, không xổ xuống.
 *
 * Trên thanh ngang các nhóm này là menu xổ xuống vì chiều ngang có hạn. Trong ngăn kéo thì chiều DỌC
 * mới là thứ dư dả, nên bắt người dùng bấm thêm một lần để mở nhóm là thêm một thao tác không đổi lại
 * được gì.
 */
function NhomNganKeo({
  label,
  items,
  onChon,
}: {
  label: string
  items: MucMenu[]
  onChon: () => void
}) {
  return (
    <div className="mt-2">
      <div className="text-ink-soft px-4 py-1 text-xs font-bold uppercase">{label}</div>
      {items.map((m) => (
        <MucNganKeo key={m.to} to={m.to} onChon={onChon}>
          {m.label}
        </MucNganKeo>
      ))}
    </div>
  )
}
