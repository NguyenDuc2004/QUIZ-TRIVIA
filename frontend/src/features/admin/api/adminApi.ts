import { apiClient } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/types'
import type { Role } from '@/features/auth/api/authApi'

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

export const adminApi = {
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
