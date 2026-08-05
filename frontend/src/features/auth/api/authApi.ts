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

  logout: (refreshToken: string) =>
    apiClient.post<void>('/auth/logout', { refreshToken }).then((res) => res.data),

  me: () => apiClient.get<UserProfile>('/users/me').then((res) => res.data),
}
