import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import NotificationBell, { khiNao } from './NotificationBell'
import { notificationApi, type Notification } from '../api/notificationApi'
import { useAuthStore } from '@/features/auth/store/authStore'

/**
 * Chuông là lối vào duy nhất của tính năng thông báo — không có mục nào trên thanh điều hướng. Nên bốn điều
 * dưới đây là bốn điều làm nó dùng được:
 *
 * 1. **Chấm đỏ đúng số chưa đọc**, và biến mất khi hết — đó là toàn bộ giá trị của nó ở các trang khác.
 * 2. **Không gọi API danh sách khi hộp chưa mở.** Chuông ở mọi trang, còn danh sách chỉ cần khi bấm vào.
 * 3. **Bấm vào một thông báo thì đánh dấu đã đọc.** Không thì chấm đỏ không bao giờ tắt.
 * 4. **Đã đọc rồi thì không gọi lại API đánh dấu** — một request không đổi gì.
 */

function thongBao(ghiDe: Partial<Notification> = {}): Notification {
  return {
    id: 'n1',
    type: 'SRS_REMINDER',
    loaiNhan: 'Nhắc ôn tập',
    title: 'Bạn có 7 thẻ đến hạn ôn hôm nay',
    body: 'Ôn đúng hạn giúp bạn nhớ lâu hơn',
    data: { kind: 'SRS_DUE', soThe: 7 },
    read: false,
    createdAt: new Date().toISOString(),
    ...ghiDe,
  }
}

function trang(rows: Notification[]) {
  return { content: rows, page: 0, size: 8, totalElements: rows.length, totalPages: 1, last: true }
}

function hienThi() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <NotificationBell />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('NotificationBell', () => {
  beforeEach(() => {
    // Hook chốt theo `user`, không theo token: chuông cũng dựng cho khách vào phòng đấu bằng mã PIN, và
    // gọi API lúc đó là một request 401 mỗi lần tải trang
    useAuthStore.setState({
      user: {
        id: 'u1', email: 'a@example.com', displayName: 'A', avatarUrl: null,
        role: 'LEARNER', createdAt: '2026-08-14T00:00:00Z',
      },
      isReady: true,
    })
    vi.restoreAllMocks()
  })

  it('hiện số chưa đọc trên chuông', async () => {
    vi.spyOn(notificationApi, 'soChuaDoc').mockResolvedValue(3)
    vi.spyOn(notificationApi, 'danhSach').mockResolvedValue(trang([]))

    hienThi()

    expect(await screen.findByText('3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /3 chưa đọc/ })).toBeInTheDocument()
  })

  it('không có gì chưa đọc thì không hiện chấm', async () => {
    vi.spyOn(notificationApi, 'soChuaDoc').mockResolvedValue(0)
    vi.spyOn(notificationApi, 'danhSach').mockResolvedValue(trang([]))

    hienThi()

    expect(await screen.findByRole('button', { name: 'Thông báo' })).toBeInTheDocument()
    expect(screen.queryByText('0')).not.toBeInTheDocument()
  })

  it('CHƯA mở hộp thì không gọi API danh sách', async () => {
    vi.spyOn(notificationApi, 'soChuaDoc').mockResolvedValue(2)
    const danhSach = vi.spyOn(notificationApi, 'danhSach').mockResolvedValue(trang([]))

    hienThi()
    await screen.findByText('2')

    // Chuông hiện ở MỌI trang. Kéo về 8 thông báo đầy đủ chỉ để vẽ một chấm đỏ là tốn vô ích, và đã có
    // endpoint riêng rẻ hơn cho con số
    expect(danhSach).not.toHaveBeenCalled()
  })

  it('mở hộp thì hiện thông báo kèm loại và thời điểm', async () => {
    vi.spyOn(notificationApi, 'soChuaDoc').mockResolvedValue(1)
    vi.spyOn(notificationApi, 'danhSach').mockResolvedValue(trang([thongBao()]))

    hienThi()
    await userEvent.click(await screen.findByRole('button', { name: /1 chưa đọc/ }))

    expect(await screen.findByText('Bạn có 7 thẻ đến hạn ôn hôm nay')).toBeInTheDocument()
    expect(screen.getByText(/Nhắc ôn tập/)).toBeInTheDocument()
  })

  it('bấm vào thông báo CHƯA đọc thì đánh dấu đã đọc', async () => {
    vi.spyOn(notificationApi, 'soChuaDoc').mockResolvedValue(1)
    vi.spyOn(notificationApi, 'danhSach').mockResolvedValue(trang([thongBao()]))
    const danhDau = vi.spyOn(notificationApi, 'danhDauDaDoc').mockResolvedValue(undefined)

    hienThi()
    await userEvent.click(await screen.findByRole('button', { name: /1 chưa đọc/ }))
    await userEvent.click(await screen.findByText('Bạn có 7 thẻ đến hạn ôn hôm nay'))

    expect(danhDau).toHaveBeenCalledWith('n1')
  })

  it('bấm vào thông báo ĐÃ đọc thì KHÔNG gọi lại API', async () => {
    vi.spyOn(notificationApi, 'soChuaDoc').mockResolvedValue(0)
    vi.spyOn(notificationApi, 'danhSach').mockResolvedValue(trang([thongBao({ read: true })]))
    const danhDau = vi.spyOn(notificationApi, 'danhDauDaDoc').mockResolvedValue(undefined)

    hienThi()
    await userEvent.click(await screen.findByRole('button', { name: 'Thông báo' }))
    await userEvent.click(await screen.findByText('Bạn có 7 thẻ đến hạn ôn hôm nay'))

    expect(danhDau).not.toHaveBeenCalled()
  })

  it('hộp rỗng thì nói bằng chữ, không để một khoảng trống', async () => {
    vi.spyOn(notificationApi, 'soChuaDoc').mockResolvedValue(0)
    vi.spyOn(notificationApi, 'danhSach').mockResolvedValue(trang([]))

    hienThi()
    await userEvent.click(await screen.findByRole('button', { name: 'Thông báo' }))

    expect(await screen.findByText(/Chưa có thông báo nào/)).toBeInTheDocument()
  })

  it('khách chưa đăng nhập thì KHÔNG gọi API nào', async () => {
    useAuthStore.setState({ user: null, isReady: true })
    const dem = vi.spyOn(notificationApi, 'soChuaDoc').mockResolvedValue(0)

    hienThi()

    expect(dem).not.toHaveBeenCalled()
  })
})

describe('khiNao', () => {
  it('đổi mốc thời gian thành câu tiếng Việt đọc được', () => {
    const bayGio = Date.now()
    expect(khiNao(new Date(bayGio - 10_000).toISOString())).toBe('vừa xong')
    expect(khiNao(new Date(bayGio - 5 * 60_000).toISOString())).toBe('5 phút trước')
    expect(khiNao(new Date(bayGio - 3 * 3_600_000).toISOString())).toBe('3 giờ trước')
    expect(khiNao(new Date(bayGio - 30 * 3_600_000).toISOString())).toBe('hôm qua')
    expect(khiNao(new Date(bayGio - 3 * 86_400_000).toISOString())).toBe('3 ngày trước')
  })

  it('quá một tuần thì hiện ngày cụ thể, không phải "60 ngày trước"', () => {
    // "412 ngày trước" là một con số người đọc phải tự trừ ra ngày — vô dụng hơn chính ngày đó
    const cu = new Date(Date.now() - 400 * 86_400_000).toISOString()
    expect(khiNao(cu)).toMatch(/\d{1,2}\/\d{1,2}\/\d{4}/)
  })
})
