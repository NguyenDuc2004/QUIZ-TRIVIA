import { apiClient } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/types'
import type { Role } from '@/features/auth/api/authApi'
import type { QuizSummary } from '@/features/quiz/api/quizApi'

export interface AdminUser {
  id: string
  email: string
  displayName: string
  avatarUrl: string | null
  role: Role
  locked: boolean
  /** "Mật khẩu" | "Google" | "Mật khẩu và Google" — đủ để hỗ trợ khi người dùng báo không vào được. */
  loginMethod: string
  createdAt: string
}

export interface AiUsageByFeature {
  chucNang: string
  luotGoi: number
  tokenVao: number
  tokenRa: number
  /** null = chưa có lời gọi nào để tính, khác hẳn với 0 ms. */
  doTreTrungBinhMs: number | null
}

export interface AiUsageByProvider {
  nhaCungCap: string
  luotGoi: number
  luotThatBai: number
}

export interface AiUsageSummary {
  tongLuotGoi: number
  luotThanhCong: number
  luotThatBai: number
  luotDungDuPhong: number
  tongTokenVao: number
  tongTokenRa: number
  doTreTrungBinhMs: number | null
  doTreP95Ms: number | null
  theoChucNang: AiUsageByFeature[]
  theoNhaCungCap: AiUsageByProvider[]
}

/** Khớp SystemOverview.DiemTheoNgay. */
export interface DiemTheoNgay {
  ngay: string
  nguoiDungMoi: number
  luotLamBai: number
}

export interface PhanBoDanhMuc {
  danhMuc: string
  soQuiz: number
}

export interface TiLeHoanThanh {
  trangThai: string
  soLuot: number
  /** null = chưa có bài nào đã nộp, khác hẳn với 0%. */
  doChinhXacTrungBinh: number | null
}

export interface SystemOverview {
  tongNguoiDung: number
  soNguoiHoc: number
  soNguoiTaoNoiDung: number
  soQuanTri: number
  soBiKhoa: number
  dangKyHomNay: number
  tongQuiz: number
  quizCongKhai: number
  tongCauHoi: number
  tongHocLieu: number
  tongLuotLamBai: number
  luotLamBaiHomNay: number
  phongDangCho: number
  phongDangChoi: number
  luotGoiAiThangNay: number
  tokenThangNay: number
  tangTruong: DiemTheoNgay[]
  theoDanhMuc: PhanBoDanhMuc[]
  tiLeHoanThanh: TiLeHoanThanh[]
}

export interface AdminCategory {
  id: string
  name: string
  slug: string
  description: string | null
  soQuiz: number
}

export interface CategoryBody {
  name: string
  /** Để trống thì backend tự sinh từ tên, có bỏ dấu tiếng Việt. */
  slug?: string
  description?: string
}

export interface LiveRoom {
  id: string
  roomCode: string
  tenChuPhong: string
  tenQuiz: string
  status: 'WAITING' | 'PLAYING' | 'FINISHED'
  /** null = không còn trạng thái ở Redis, tức phòng treo. Không phải "0 người". */
  soNguoiChoi: number | null
  cauHienTai: number | null
  tongSoCau: number | null
  choKhachVao: boolean
  taoLuc: string
  batDauLuc: string | null
  treo: boolean
}

export interface AiProviderStatus {
  ten: string
  /** Chỉ true/false — backend không bao giờ trả giá trị khoá. */
  daCauHinh: boolean
  sanSang: boolean
  hoTroEmbedding: boolean
  hoTroStreaming: boolean
}

export interface AiConfig {
  nhaCungCap: AiProviderStatus[]
  thuTuUuTien: string[]
  coTheGoiAi: boolean
  soLuongThuLaiTacVuNen: number
}

export const adminApi = {
  overview: (days: number) =>
    apiClient.get<SystemOverview>('/admin/overview', { params: { days } }).then((res) => res.data),

  revokeSessions: (id: string) =>
    apiClient.post<{ soPhienDaThuHoi: number }>(`/admin/users/${id}/revoke`)
      .then((res) => res.data),

  categories: () =>
    apiClient.get<AdminCategory[]>('/admin/categories').then((res) => res.data),

  createCategory: (body: CategoryBody) =>
    apiClient.post<AdminCategory>('/admin/categories', body).then((res) => res.data),

  updateCategory: (id: string, body: CategoryBody) =>
    apiClient.put<AdminCategory>(`/admin/categories/${id}`, body).then((res) => res.data),

  deleteCategory: (id: string) =>
    apiClient.delete<void>(`/admin/categories/${id}`).then((res) => res.data),

  quizzes: (params: { keyword?: string; categoryId?: string; page?: number; size?: number }) =>
    apiClient.get<PageResponse<QuizSummary>>('/admin/quizzes', { params }).then((res) => res.data),

  hideQuiz: (id: string) =>
    apiClient.put<void>(`/admin/quizzes/${id}/hide`).then((res) => res.data),

  rooms: () => apiClient.get<LiveRoom[]>('/admin/rooms').then((res) => res.data),

  closeRoom: (roomCode: string) =>
    apiClient.post<void>(`/admin/rooms/${roomCode}/close`).then((res) => res.data),

  aiConfig: () => apiClient.get<AiConfig>('/admin/ai/config').then((res) => res.data),

  users: (params: {
    keyword?: string
    role?: Role
    locked?: boolean
    page?: number
    size?: number
  }) => apiClient.get<PageResponse<AdminUser>>('/admin/users', { params }).then((res) => res.data),

  changeRole: (id: string, role: Role) =>
    apiClient.put<AdminUser>(`/admin/users/${id}/role`, null, { params: { role } })
      .then((res) => res.data),

  setLocked: (id: string, locked: boolean) =>
    apiClient.put<AdminUser>(`/admin/users/${id}/locked`, null, { params: { locked } })
      .then((res) => res.data),

  aiUsage: (days: number) =>
    apiClient.get<AiUsageSummary>('/admin/ai/usage', { params: { days } }).then((res) => res.data),
}
