/**
 * Nơi duy nhất giữ token phía client. Cả axios interceptor và store auth đều
 * đọc/ghi qua đây để không có hai nguồn sự thật.
 *
 * ## Hai chỗ lưu, và vì sao KHÔNG cần thêm cờ để nhớ đã chọn chỗ nào
 * Người dùng chọn "ghi nhớ đăng nhập" thì token vào `localStorage` (sống qua cả lần tắt máy); không
 * chọn thì vào `sessionStorage` (đóng tab là mất). **Chính vị trí của token là cái cờ** — không cần
 * lưu thêm một biến "đã chọn ghi nhớ chưa", và nhờ vậy không có cách nào để cờ và thực tế lệch nhau.
 *
 * Mỗi lần ghi đều **xoá bên còn lại**, nếu không thì một bản token cũ nằm lại ở kho kia và lần đọc
 * sau có thể nhặt trúng nó — người dùng bỏ tick "ghi nhớ" mà vẫn còn phiên trong `localStorage` từ
 * lần đăng nhập trước là đúng thứ tính năng này sinh ra để chặn.
 */
const ACCESS_KEY = 'accessToken'
const REFRESH_KEY = 'refreshToken'

export const tokenStorage = {
  // Đọc `sessionStorage` TRƯỚC: nếu vì lý do nào đó cả hai kho cùng có token, bản của phiên hiện tại
  // mới là bản vừa được ghi.
  getAccess: () => sessionStorage.getItem(ACCESS_KEY) ?? localStorage.getItem(ACCESS_KEY),
  getRefresh: () => sessionStorage.getItem(REFRESH_KEY) ?? localStorage.getItem(REFRESH_KEY),

  /**
   * @param ghiNho `true` → `localStorage`, `false` → `sessionStorage`.
   *   **Bỏ trống → giữ nguyên chỗ đang lưu.** Đây là mặc định quan trọng: axios interceptor gọi hàm
   *   này mỗi lần xoay refresh token, và nó không biết (cũng không nên biết) người dùng đã chọn gì.
   *   Mặc định về `localStorage` thì mọi phiên "chỉ trong lần này" sẽ tự nâng thành phiên vĩnh viễn
   *   ngay ở lần làm mới token đầu tiên — 15 phút sau khi đăng nhập.
   */
  save(accessToken: string, refreshToken: string, ghiNho?: boolean) {
    const nho = ghiNho ?? this.dangGhiNho()
    const kho = nho ? localStorage : sessionStorage
    const khoKia = nho ? sessionStorage : localStorage

    kho.setItem(ACCESS_KEY, accessToken)
    kho.setItem(REFRESH_KEY, refreshToken)
    khoKia.removeItem(ACCESS_KEY)
    khoKia.removeItem(REFRESH_KEY)
  },

  /** Xoá ở CẢ HAI kho — đăng xuất mà còn sót một bản là chưa đăng xuất. */
  clear() {
    for (const kho of [localStorage, sessionStorage]) {
      kho.removeItem(ACCESS_KEY)
      kho.removeItem(REFRESH_KEY)
    }
  },

  /**
   * Phiên hiện tại có phải loại "ghi nhớ" không.
   *
   * Chưa đăng nhập (không có token ở đâu cả) thì trả `true`: đó là mặc định của ô tick ở màn đăng
   * nhập, và giữ cho hành vi trước khi có tính năng này không đổi.
   */
  dangGhiNho(): boolean {
    if (sessionStorage.getItem(REFRESH_KEY) !== null) {
      return false
    }
    return true
  },
}
