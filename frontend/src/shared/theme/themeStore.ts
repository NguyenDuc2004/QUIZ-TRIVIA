import { create } from 'zustand'
import { persist } from 'zustand/middleware'

/** Ba trạng thái, không phải hai. `system` là **mặc định**, xem javadoc bên dưới. */
export type CheDoMau = 'light' | 'dark' | 'system'

/** Chế độ thật đang áp lên giao diện — `system` đã được quy về một trong hai. */
export type CheDoThat = 'light' | 'dark'

interface ThemeState {
  cheDo: CheDoMau
  datCheDo: (cheDo: CheDoMau) => void
}

const KHOA_LUU = 'quizai-theme'

/**
 * Chế độ sáng/tối.
 *
 * ## Vì sao ba trạng thái, không phải hai
 * Chỉ có Sáng/Tối thì lần đầu vào web người dùng bị áp một chế độ mà họ không chọn — và nếu đó là Sáng
 * trong khi cả máy họ đang ở chế độ tối thì trang này là thứ duy nhất chói mắt. `system` là **mặc định**:
 * tôn trọng thiết lập hệ điều hành cho tới khi người dùng nói khác đi.
 *
 * Và một khi họ đã chọn tay thì lựa chọn đó **thắng** thiết lập hệ điều hành, kể cả khi hệ điều hành đổi
 * sau đó — họ đã nói rõ ý muốn cho trang này.
 *
 * ## Vì sao lưu lại
 * Không lưu thì mỗi lần tải trang lại quay về mặc định, và với người thật sự cần chế độ tối thì đó là
 * bật lại mỗi ngày vài lần.
 */
export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      cheDo: 'system',
      datCheDo: (cheDo) => set({ cheDo }),
    }),
    { name: KHOA_LUU },
  ),
)

/** Hệ điều hành đang ở chế độ tối hay không. */
export function heDieuHanhToi(): boolean {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
}

/** Quy `system` về một trong hai chế độ thật. */
export function quyDoi(cheDo: CheDoMau): CheDoThat {
  if (cheDo === 'system') {
    return heDieuHanhToi() ? 'dark' : 'light'
  }
  return cheDo
}

/**
 * Đặt thuộc tính `data-theme` lên thẻ `<html>`.
 *
 * Tách khỏi React và xuất ra ngoài để **gọi được trước khi React kịp render** (xem `main.tsx`). Đặt
 * trong một `useEffect` là quá muộn: trang sẽ vẽ một khung sáng rồi mới nhảy sang tối, và cái nháy trắng
 * đó đúng là thứ người bật chế độ tối muốn tránh nhất.
 */
export function apCheDo(cheDo: CheDoMau): CheDoThat {
  const that = quyDoi(cheDo)
  document.documentElement.dataset.theme = that
  return that
}

/**
 * Đọc chế độ đã lưu **mà không cần React**.
 *
 * Đọc thẳng localStorage thay vì gọi store: store của zustand chỉ nạp dữ liệu đã lưu khi module được
 * khởi tạo trong vòng đời React, còn hàm này chạy trước đó.
 */
export function cheDoDaLuu(): CheDoMau {
  try {
    const raw = localStorage.getItem(KHOA_LUU)
    const cheDo = raw ? JSON.parse(raw)?.state?.cheDo : null
    return cheDo === 'light' || cheDo === 'dark' || cheDo === 'system' ? cheDo : 'system'
  } catch {
    // Chế độ riêng tư của trình duyệt có thể ném lỗi khi đọc localStorage. Không được để một
    // thiết lập hiển thị làm cả ứng dụng không mở được.
    return 'system'
  }
}
