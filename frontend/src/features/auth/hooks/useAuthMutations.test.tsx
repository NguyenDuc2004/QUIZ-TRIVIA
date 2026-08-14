import type { ReactNode } from 'react'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useLogin, useLogout } from './useAuthMutations'
import { useAuthStore } from '../store/authStore'

/**
 * Ca hồi quy cho lỗi **rò dữ liệu giữa các tài khoản** gặp ngày 13/08/2026: đăng nhập tài khoản A
 * nhưng thấy lịch sử chat của tài khoản B.
 * <p>
 * Backend lọc đúng theo `userId`; lỗi ở client. Đăng xuất rồi đăng nhập đều là điều hướng phía client
 * nên không có lần nạp lại trang nào ở giữa, `QueryClient` sống nguyên qua cả hai phiên cùng toàn bộ
 * dữ liệu đã tải. Cộng `staleTime: 30_000`, dữ liệu người trước còn được coi là *tươi* nên các trang
 * hiện nó ra ngay mà không gọi lại API — và rò không giới hạn ở lịch sử chat: mọi thứ đi qua cache đều
 * rò (lượt làm bài, tiến độ, quiz của tôi, ngân hàng câu hỏi, học liệu).
 * <p>
 * Kiểm ở tầng hook thay vì tầng giao diện vì đây là lỗi của **vòng đời cache**, không phải của một
 * trang cụ thể: kiểm một trang thì chỉ chứng minh trang đó sạch, còn cache là thứ mọi trang dùng chung.
 */

vi.mock('../api/authApi', () => ({
  authApi: {
    login: vi.fn(async () => ({
      accessToken: 'access-cua-nguoi-moi',
      refreshToken: 'refresh-cua-nguoi-moi',
      user: { id: 'u2', email: 'b@example.com', displayName: 'Người B', avatarUrl: null, role: 'LEARNER' },
    })),
    logout: vi.fn(async () => undefined),
  },
}))

// Điều hướng: hook gọi navigate sau khi đổi phiên, không liên quan điều đang kiểm
vi.mock('react-router-dom', () => ({ useNavigate: () => vi.fn() }))
vi.mock('antd', () => ({ message: { success: vi.fn(), error: vi.fn() } }))

function wrapper(client: QueryClient) {
  return ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>{children}</QueryClientProvider>
  )
}

/** Dựng lại đúng tình huống thật: cache đã có dữ liệu của người dùng trước đó. */
function clientWithPreviousUserData() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: 30_000 } },
  })
  client.setQueryData(['chat', 'sessions'], [{ id: 's1', title: 'Hội thoại của người A' }])
  client.setQueryData(['attempts', 'mine'], [{ id: 'a1', quizTitle: 'Bài của người A' }])
  return client
}

describe('Đổi tài khoản không được để lộ dữ liệu người trước', () => {
  beforeEach(() => {
    useAuthStore.getState().clearSession()
  })

  it('đăng nhập xoá sạch cache của người dùng trước', async () => {
    const client = clientWithPreviousUserData()
    expect(client.getQueryData(['chat', 'sessions'])).toBeDefined()

    const { result } = renderHook(() => useLogin(), { wrapper: wrapper(client) })
    result.current.mutate({ email: 'b@example.com', password: 'MatKhau12345' })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(client.getQueryData(['chat', 'sessions']))
      .toBeUndefined()
    expect(client.getQueryData(['attempts', 'mine']))
      .toBeUndefined()
  })

  it('đăng xuất xoá sạch cache, không để lại cho người đăng nhập sau', async () => {
    const client = clientWithPreviousUserData()

    const { result } = renderHook(() => useLogout(), { wrapper: wrapper(client) })
    result.current.mutate()

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(client.getQueryData(['chat', 'sessions'])).toBeUndefined()
    expect(client.getQueryData(['attempts', 'mine'])).toBeUndefined()
  })

  it('xoá cache TRƯỚC khi đặt phiên mới — không để lúc nào danh tính mới mà dữ liệu cũ', async () => {
    // Thứ tự quan trọng: nếu setSession chạy trước khi xoá cache thì tồn tại một khoảnh khắc
    // component đã thấy người dùng mới nhưng đọc được dữ liệu người cũ, và nó đủ để render ra.
    const client = clientWithPreviousUserData()
    const orderSeen: string[] = []

    const realClear = client.clear.bind(client)
    client.clear = () => {
      orderSeen.push(`clear(user=${useAuthStore.getState().user?.id ?? 'null'})`)
      realClear()
    }
    useAuthStore.subscribe((state) => {
      if (state.user) {
        orderSeen.push(`setSession(user=${state.user.id})`)
      }
    })

    const { result } = renderHook(() => useLogin(), { wrapper: wrapper(client) })
    result.current.mutate({ email: 'b@example.com', password: 'MatKhau12345' })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(orderSeen).toEqual(['clear(user=null)', 'setSession(user=u2)'])
  })
})
