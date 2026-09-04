import { create } from 'zustand'

/** Ba trạng thái, không phải hai. `system` là **mặc định**, xem javadoc bên dưới. */
export type CheDoMau = 'light' | 'dark' | 'system'

/** Chế độ thật đang áp lên giao diện — `system` đã được quy về một trong hai. */
export type CheDoThat = 'light' | 'dark'

const KHOA_LUU = 'quizai-theme'

/**
 * Đọc chế độ đã lưu, **không cần React**.
 *
 * Dùng cả ở cấp module trong `main.tsx` (để áp chế độ trước lần render đầu) lẫn làm giá trị khởi tạo của
 * store bên dưới. Một hàm, một nguồn — hai nơi không thể lệch nhau.
 */
export function cheDoDaLuu(): CheDoMau {
  try {
    const luu = localStorage.getItem(KHOA_LUU)
    return luu === 'light' || luu === 'dark' || luu === 'system' ? luu : 'system'
  } catch {
    // Chế độ riêng tư của trình duyệt có thể ném lỗi khi đọc localStorage. Không được để một thiết lập
    // hiển thị làm cả ứng dụng không mở được.
    return 'system'
  }
}

interface ThemeState {
  cheDo: CheDoMau
  datCheDo: (cheDo: CheDoMau) => void
}

/**
 * Chế độ sáng/tối.
 *
 * ## Vì sao ba trạng thái, không phải hai
 * Chỉ có Sáng/Tối thì lần đầu vào web người dùng bị áp một chế độ mà họ không chọn — và nếu đó là Sáng
 * trong khi cả máy họ đang ở chế độ tối thì trang này là thứ duy nhất chói mắt. `system` là **mặc định**:
 * tôn trọng thiết lập hệ điều hành cho tới khi người dùng nói khác đi. Một khi đã chọn tay thì lựa chọn
 * đó **thắng**, kể cả khi hệ điều hành đổi sau.
 *
 * ## Vì sao KHÔNG dùng middleware `persist`
 * Bản đầu dùng `persist`, và nó gây ra một lỗi khó thấy: mã ở cấp module trong `main.tsx` đọc thẳng
 * `localStorage` nên đặt `data-theme` **đúng** ngay lập tức, trong khi store chưa chắc đã nạp xong giá
 * trị đã lưu ở lần render đầu. Kết quả là hai nửa giao diện bất đồng — token Tailwind đã tối, còn Ant
 * Design vẫn lấy bản sáng: nền trang trắng, chữ tiêu đề tối trên nền tối, nút chính sai kiểu.
 *
 * Tự đọc/ghi `localStorage` chỉ tốn vài dòng và bỏ hẳn câu hỏi "đã nạp xong chưa".
 */
export const useThemeStore = create<ThemeState>((set) => ({
  cheDo: cheDoDaLuu(),
  datCheDo: (cheDo) => {
    try {
      localStorage.setItem(KHOA_LUU, cheDo)
    } catch {
      // Không ghi được thì vẫn đổi giao diện cho phiên này; chỉ mất phần nhớ cho lần sau.
    }
    set({ cheDo })
  },
}))

/** Hệ điều hành đang ở chế độ tối hay không. */
export function heDieuHanhToi(): boolean {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
}

/** Quy `system` về một trong hai chế độ thật. */
export function quyDoi(cheDo: CheDoMau): CheDoThat {
  return cheDo === 'system' ? (heDieuHanhToi() ? 'dark' : 'light') : cheDo
}

/**
 * Đặt thuộc tính `data-theme` lên thẻ `<html>`.
 *
 * Tách khỏi React để **gọi được trước khi React kịp render** (xem `main.tsx`). Đặt trong một `useEffect`
 * là quá muộn: trang sẽ vẽ một khung sáng rồi mới nhảy sang tối, và cái nháy trắng đó đúng là thứ người
 * bật chế độ tối muốn tránh nhất.
 */
export function apCheDo(cheDo: CheDoMau): CheDoThat {
  const that = quyDoi(cheDo)
  document.documentElement.dataset.theme = that
  return that
}
