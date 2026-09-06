import { Link } from 'react-router-dom'
import { Typography } from 'antd'
import { useAuthStore } from '@/features/auth/store/authStore'

const { Text } = Typography

/**
 * Chân trang.
 *
 * ## Chỉ có link tới trang CÓ THẬT
 * Chân trang mặc định của mọi mẫu web đều có "Về chúng tôi", "Điều khoản", "Chính sách bảo mật",
 * "Liên hệ", mấy biểu tượng mạng xã hội. Hệ thống này **không có** một trang nào trong số đó. Dán chúng
 * vào là tạo một hàng link chết — và một link chết ở chân trang tệ hơn một chân trang trống, vì nó hứa
 * một thứ rồi để người bấm vào rơi vào trang 404.
 *
 * Cùng nguyên tắc đã dùng khi từ chối hiện số sao đánh giá: **không bịa thứ không có** (CLAUDE.md §5).
 *
 * ## Vì sao ghi thông tin đồ án
 * Đây là sản phẩm của một đồ án tốt nghiệp, không phải một dịch vụ thương mại. Ghi rõ trường, sinh viên
 * và người hướng dẫn là **nói đúng bản chất** của thứ người dùng đang xem — và tiện cho hội đồng khi
 * chấm: mở trang bất kỳ cũng thấy ngay đây là bài của ai.
 *
 * ## Không hiện ở màn làm bài
 * Xem `AppLayout`: trang làm bài và phòng đấu cần toàn bộ sự chú ý, và ở chế độ thi nghiêm ngặt (FR-48)
 * thì mọi thứ ngoài đề bài đều là chỗ để người thi bấm nhầm ra ngoài.
 */
export default function AppFooter() {
  const user = useAuthStore((state) => state.user)
  const laNguoiTao = user?.role === 'CREATOR' || user?.role === 'ADMIN'

  return (
    <footer className="bg-footer-bg mt-8">
      <div className="mx-auto grid max-w-6xl gap-6 px-6 py-8 sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <div className="mb-2 flex items-center gap-1">
            {/* Chữ trắng thay cho `text-ink` (gần đen): trên nền xanh than nó chìm hẳn.
                Chữ "AI" giữ nguyên màu thương hiệu — `--color-brand` (#a435f0) đủ sáng trên nền tối,
                khác `--color-brand-strong` (#5624d0) vốn dành cho nền sáng và sẽ tối om ở đây. */}
            <span className="text-base font-extrabold text-white">Quiz</span>
            <span className="text-brand text-base font-extrabold">AI</span>
          </div>
          <Text className="text-footer-text! block text-xs leading-relaxed">
            Nền tảng học tập bằng câu hỏi trắc nghiệm, có phòng đấu thời gian thực và trợ lý AI đọc
            trên chính học liệu của bạn.
          </Text>
        </div>

        <div>
          <Text className="mb-2 block text-xs font-bold text-white!">Học tập</Text>
          <ul className="flex list-none flex-col gap-1.5 p-0">
            <FooterLink to="/quizzes">Khám phá quiz</FooterLink>
            <FooterLink to="/flashcards">Thẻ ghi nhớ</FooterLink>
            <FooterLink to="/learning-path">Lộ trình học</FooterLink>
            <FooterLink to="/assistant">Trợ lý học tập</FooterLink>
          </ul>
        </div>

        <div>
          <Text className="mb-2 block text-xs font-bold text-white!">Cùng nhau</Text>
          <ul className="flex list-none flex-col gap-1.5 p-0">
            <FooterLink to="/rooms">Phòng đấu trí</FooterLink>
            <FooterLink to="/classrooms">Lớp học</FooterLink>
            <FooterLink to="/leaderboard">Bảng xếp hạng</FooterLink>
            <FooterLink to="/achievements">Thành tích</FooterLink>
          </ul>
        </div>

        <div>
          <Text className="mb-2 block text-xs font-bold text-white!">
            {laNguoiTao ? 'Soạn nội dung' : 'Của tôi'}
          </Text>
          <ul className="flex list-none flex-col gap-1.5 p-0">
            {/* Danh sách đổi theo vai trò: người học không vào được ngân hàng câu hỏi hay trang
                sinh đề (ProtectedRoute chặn), nên hiện link ở đó chỉ để họ bấm vào rồi bị đá ra.
                Riêng HỌC LIỆU thì cả hai vai trò đều vào được từ 04/09/2026 — chỉ khác nhãn, vì với
                người học nó là tài liệu của chính họ chứ không phải nguồn để soạn nội dung. */}
            {laNguoiTao ? (
              <>
                <FooterLink to="/my-quizzes">Quiz của tôi</FooterLink>
                <FooterLink to="/question-bank">Ngân hàng câu hỏi</FooterLink>
                <FooterLink to="/ai/materials">Học liệu</FooterLink>
                <FooterLink to="/ai/generate">Sinh đề bằng AI</FooterLink>
              </>
            ) : (
              <>
                <FooterLink to="/my-attempts">Bài đã làm</FooterLink>
                <FooterLink to="/my-progress">Tiến độ học</FooterLink>
                <FooterLink to="/my-assignments">Bài tập được giao</FooterLink>
                {/* Từ 04/09/2026 người học sở hữu học liệu của chính mình, nên mục này thuộc về cột
                    của họ. Thanh điều hướng đã thêm hôm đó, chân trang thì bỏ sót — hai chỗ liệt kê
                    cùng một bộ mục mà lệch nhau. */}
                <FooterLink to="/ai/materials">Học liệu của tôi</FooterLink>
                <FooterLink to="/profile">Hồ sơ của tôi</FooterLink>
              </>
            )}
          </ul>
        </div>
      </div>

      {/* Thông tin đồ án — lấy đúng từ phiếu giao đề tài, không viết thêm gì */}
      <div className="border-footer-line border-t">
        <div className="text-footer-text mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-2 px-6 py-4 text-xs">
          <span>
            Đồ án tốt nghiệp · Trường Đại học Công nghiệp Hà Nội · Ngành Kỹ thuật phần mềm, K17
          </span>
          <span>
            Sinh viên Nguyễn Khắc Minh Đức · GVHD ThS. Nguyễn Đức Lưu
          </span>
        </div>
      </div>
    </footer>
  )
}

function FooterLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <li>
      {/* Hậu tố `!` là bắt buộc với thẻ <a>: Ant Design chèn `a { color }` ở NGOÀI layer của Tailwind,
          nên luật không có `!` sẽ thua (ui-design-system.md §3).

          Hover ra TRẮNG chứ không ra tím: `--color-brand-strong` là tím đậm cho nền sáng, đặt lên nền
          xanh than thì gần như không đọc được. */}
      <Link to={to} className="text-footer-text! text-xs hover:text-white!">
        {children}
      </Link>
    </li>
  )
}
