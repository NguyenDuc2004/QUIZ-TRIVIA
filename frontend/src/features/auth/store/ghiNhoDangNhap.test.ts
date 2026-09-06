import { beforeEach, describe, expect, it } from 'vitest'
import { useAuthStore } from './authStore'
import type { AuthResult } from '../api/authApi'

/**
 * Bỏ tick "ghi nhớ" phải giấu **cả hồ sơ**, không chỉ token.
 *
 * Nửa dễ quên: token và hồ sơ người dùng được lưu bởi hai cơ chế khác nhau — `tokenStorage` và
 * `persist` của zustand. Chỉ đổi chỗ lưu token thì tính năng đúng một nửa: phiên hết khi đóng trình
 * duyệt, nhưng **tên, email và ảnh đại diện vẫn nằm lại `localStorage`** — đúng thứ người dùng bỏ
 * tick để tránh khi ngồi máy chung. Giao diện không lộ phiên giả (`useIsAuthenticated` đòi có cả hai),
 * nhưng dữ liệu cá nhân vẫn đọc được bằng công cụ dev.
 */
function ketQua(): AuthResult {
  return {
    accessToken: 'a',
    refreshToken: 'r',
    expiresIn: 900,
    user: {
      id: 'u1',
      email: 'nguoihoc@example.com',
      displayName: 'Người học',
      role: 'LEARNER',
      avatarUrl: null,
    },
  } as AuthResult
}

/** `persist` ghi không đồng bộ — chờ hàng đợi vi tác vụ chạy xong rồi mới đọc. */
const doiGhiXong = () => new Promise((r) => setTimeout(r, 0))

describe('Ghi nhớ đăng nhập — hồ sơ đi cùng token', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('CÓ ghi nhớ: cả token lẫn hồ sơ vào localStorage', async () => {
    useAuthStore.getState().setSession(ketQua(), true)
    await doiGhiXong()

    expect(localStorage.getItem('refreshToken')).toBe('r')
    expect(localStorage.getItem('quizai-auth')).toContain('nguoihoc@example.com')
    expect(sessionStorage.getItem('quizai-auth')).toBeNull()
  })

  it('KHÔNG ghi nhớ: localStorage không còn dấu vết nào của người dùng', async () => {
    useAuthStore.getState().setSession(ketQua(), false)
    await doiGhiXong()

    expect(sessionStorage.getItem('refreshToken')).toBe('r')
    expect(sessionStorage.getItem('quizai-auth')).toContain('nguoihoc@example.com')

    // Đây là điều kiện thật của tính năng: đóng trình duyệt xong, máy chung không giữ lại gì.
    expect(localStorage.getItem('refreshToken')).toBeNull()
    expect(localStorage.getItem('quizai-auth')).toBeNull()
  })

  it('đăng nhập lại có tick sau khi đã bỏ tick thì không sót bản cũ', async () => {
    useAuthStore.getState().setSession(ketQua(), false)
    await doiGhiXong()

    useAuthStore.getState().setSession(ketQua(), true)
    await doiGhiXong()

    expect(localStorage.getItem('quizai-auth')).toContain('nguoihoc@example.com')
    expect(sessionStorage.getItem('quizai-auth')).toBeNull()
    expect(sessionStorage.getItem('refreshToken')).toBeNull()
  })
})
