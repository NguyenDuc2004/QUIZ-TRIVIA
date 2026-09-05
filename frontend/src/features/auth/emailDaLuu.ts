const KHOA = 'quizai-email-cuoi'

/**
 * Email dùng ở lần đăng nhập gần nhất, để điền sẵn vào ô email.
 *
 * ## Chỉ email, KHÔNG BAO GIỜ mật khẩu
 * Mật khẩu là việc của trình duyệt và trình quản lý mật khẩu — chúng mã hoá bằng khoá của hệ điều
 * hành. Ứng dụng tự lưu mật khẩu vào `localStorage` thì bất kỳ đoạn JavaScript nào chạy trên trang
 * cũng đọc được: một thư viện npm bị chèn mã độc, hay một lỗ XSS, là lộ **mật khẩu gốc** — thứ người
 * dùng thường dùng lại ở nơi khác, nên thiệt hại vượt xa ứng dụng này.
 *
 * Email thì không phải bí mật, và nó đã hiện sẵn ở trang Hồ sơ.
 *
 * ## Đi theo lựa chọn "ghi nhớ đăng nhập"
 * Bỏ tick nghĩa là *"máy này đừng giữ lại gì về tôi"*. Giữ email lại thì vẫn để lộ **ai vừa dùng máy
 * này** — đúng thứ họ bỏ tick để tránh. Nên lúc đó không những không lưu mà còn **xoá bản cũ**: người
 * dùng trước có thể đã tick, và người ngồi sau không cần biết địa chỉ của họ.
 *
 * ## Đăng xuất KHÔNG xoá
 * Đăng xuất là "tôi xong việc", không phải "quên tôi đi". Xoá lúc đó thì lần đăng nhập sau lại phải
 * gõ đủ email — đúng cái phiền mà thứ này sinh ra để bớt.
 *
 * Mọi thao tác bọc `try/catch`: chế độ riêng tư hoặc trình duyệt chặn lưu trữ sẽ ném lỗi, và một tiện
 * ích nhỏ không được phép làm hỏng cả màn đăng nhập.
 */
export const emailDaLuu = {
  doc(): string {
    try {
      return localStorage.getItem(KHOA) ?? ''
    } catch {
      return ''
    }
  },

  /** Gọi sau khi đăng nhập THÀNH CÔNG — lưu lúc đang gõ thì nhớ luôn cả email gõ sai. */
  luu(email: string, ghiNho: boolean) {
    try {
      if (ghiNho) {
        localStorage.setItem(KHOA, email)
      } else {
        localStorage.removeItem(KHOA)
      }
    } catch {
      // Không lưu được thì thôi, không có gì để báo cho người dùng.
    }
  },
}
