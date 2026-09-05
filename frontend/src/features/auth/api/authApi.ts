import { apiClient } from '@/shared/api/client'

export type Role = 'LEARNER' | 'CREATOR' | 'ADMIN'

/** Khớp UserResponse của backend (com.datn.quizai.user.dto.UserResponse). */
export interface UserProfile {
  id: string
  email: string
  displayName: string
  avatarUrl: string | null
  role: Role
  createdAt: string
}

/** Khớp AuthResponse của backend. */
export interface AuthResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserProfile
}

export interface RegisterBody {
  email: string
  password: string
  displayName: string
  role?: Role
}

export interface LoginBody {
  email: string
  password: string
}

export const authApi = {
  register: (body: RegisterBody) =>
    apiClient.post<AuthResult>('/auth/register', body).then((res) => res.data),

  login: (body: LoginBody) =>
    apiClient.post<AuthResult>('/auth/login', body).then((res) => res.data),

  /**
   * Tự đổi vai trò giữa Người học và Người tạo nội dung.
   *
   * Trả về cặp token MỚI vì vai trò nằm trong access token — client phải thay token đang lưu, nếu
   * không thì đổi vai trò xong vẫn mang vai trò cũ tới 15 phút.
   */
  doiVaiTro: (role: 'LEARNER' | 'CREATOR') =>
    apiClient.patch<AuthResult>('/auth/my-role', { role }).then((res) => res.data),

  /** Gửi ID token của Google; backend tự xác minh chữ ký rồi cấp token của hệ thống. */
  /**
   * Đăng nhập / đăng ký bằng Google.
   *
   * `role` CHỈ có tác dụng khi backend tạo tài khoản mới — tài khoản đã tồn tại giữ nguyên vai trò đang
   * có. Nên trang đăng nhập KHÔNG truyền tham số này: gửi ở đó chỉ tạo ấn tượng sai rằng đăng nhập lại
   * có thể đổi vai trò.
   */
  loginWithGoogle: (idToken: string, role?: Role) =>
    apiClient.post<AuthResult>('/auth/google', { idToken, role }).then((res) => res.data),

  logout: (refreshToken: string) =>
    apiClient.post<void>('/auth/logout', { refreshToken }).then((res) => res.data),

  me: () => apiClient.get<UserProfile>('/users/me').then((res) => res.data),

  /**
   * Cập nhật hồ sơ: tên hiển thị và ảnh đại diện.
   *
   * `avatarUrl` phải là đường dẫn nội bộ do `POST /files/images` sinh ra, HOẶC đúng giá trị đang lưu.
   * Ngoại lệ thứ hai là để người đăng nhập bằng Google giữ được ảnh từ CDN của Google — server ghi giá
   * trị đó lúc đăng nhập, không phải người dùng gửi lên.
   */
  updateProfile: (body: { displayName: string; avatarUrl: string | null }) =>
    apiClient.put<UserProfile>('/users/me', body).then((res) => res.data),

  /**
   * Xin mã OTP đặt lại mật khẩu.
   * Backend luôn trả 204 dù email có tồn tại hay không — đừng dựng giao diện dựa vào việc
   * "email này có trong hệ thống", vì chính chỗ đó là lỗ hổng dò danh sách người dùng.
   */
  forgotPassword: (email: string) =>
    apiClient.post<void>('/auth/forgot-password', { email }).then((res) => res.data),

  resetPassword: (body: { email: string; otp: string; newPassword: string }) =>
    apiClient.post<void>('/auth/reset-password', body).then((res) => res.data),
}
