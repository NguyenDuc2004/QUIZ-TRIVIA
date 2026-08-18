import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import AdminIntegrityPage from './AdminIntegrityPage'
import { integrityApi, type IntegrityReport } from '../api/integrityApi'

/**
 * Trang này là **giao diện của một quyết định về người khác** — người rà soát đọc nó rồi đánh dấu bài của một
 * người học là không hợp lệ. Nên test không chỉ hỏi "có render không" mà hỏi bốn điều về nội dung:
 *
 * 1. **Lý do cụ thể phải hiện ra**, không chỉ con số. Một bảng chỉ có "98/100" thì người rà soát không có gì
 *    để cân nhắc ngoài việc tin con số đó.
 * 2. **Câu nhắc "không phải bằng chứng" phải hiện ngay**, trước khi ai kịp đọc điểm.
 * 3. **Hàng chờ trống là tin tốt**, và phải nói bằng chữ chứ không phải một bảng rỗng — bảng rỗng đọc như
 *    "tải lỗi".
 * 4. **Đổi bộ lọc phải hỏi lại server đúng trạng thái** — nếu không thì tab "Không hợp lệ" hiện dữ liệu
 *    PENDING và người rà soát tưởng mình đã kết luận những bài chưa ai xem.
 */

function baoCao(ghiDe: Partial<IntegrityReport> = {}): IntegrityReport {
  return {
    attemptId: 'att-1',
    tenQuiz: 'Kiểm tra giữa kỳ Toán 12',
    tenNguoiLam: 'Nguyễn Văn An',
    riskScore: 98,
    biGanCo: true,
    flags: ['Dán nội dung 2 lần, trong đó 2 lần đoạn dài', 'Chuyển tab 3 lần'],
    aiNote: 'Chuỗi sự kiện cho thấy nhiều lần rời trang kèm dán đoạn dài.',
    reviewStatus: 'PENDING',
    reviewedAt: null,
    reviewNote: null,
    soSuKien: 7,
    suKien: [],
    canhBao: 'Tín hiệu hành vi thu từ trình duyệt nên có thể bị chặn hoặc giả mạo.',
    ...ghiDe,
  }
}

function trang(rows: IntegrityReport[]) {
  return {
    content: rows,
    page: 0,
    size: 20,
    totalElements: rows.length,
    totalPages: 1,
    last: true,
  }
}

function hienThi() {
  // retry tắt: test không nên chờ ba lần thử lại của TanStack Query khi API cố tình lỗi
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <AdminIntegrityPage />
    </QueryClientProvider>,
  )
}

describe('AdminIntegrityPage', () => {
  it('hiện lý do cụ thể bên cạnh điểm rủi ro, không chỉ con số', async () => {
    vi.spyOn(integrityApi, 'danhSachGanCo').mockResolvedValue(trang([baoCao()]))

    hienThi()

    expect(await screen.findByText('Nguyễn Văn An')).toBeInTheDocument()
    expect(screen.getByText('98/100')).toBeInTheDocument()
    expect(screen.getByText('Dán nội dung 2 lần, trong đó 2 lần đoạn dài')).toBeInTheDocument()
    expect(screen.getByText('Chuyển tab 3 lần')).toBeInTheDocument()
  })

  it('nhắc "không phải bằng chứng gian lận" ngay đầu trang', async () => {
    vi.spyOn(integrityApi, 'danhSachGanCo').mockResolvedValue(trang([baoCao()]))

    hienThi()

    expect(await screen.findByText('Điểm rủi ro không phải bằng chứng gian lận')).toBeInTheDocument()
  })

  it('hàng chờ trống thì nói bằng chữ, không để bảng rỗng', async () => {
    vi.spyOn(integrityApi, 'danhSachGanCo').mockResolvedValue(trang([]))

    hienThi()

    expect(await screen.findByText('Không có bài nào chờ rà soát')).toBeInTheDocument()
  })

  it('đổi bộ lọc thì hỏi lại server đúng trạng thái đó', async () => {
    const goi = vi.spyOn(integrityApi, 'danhSachGanCo').mockResolvedValue(trang([]))

    hienThi()
    await screen.findByText('Không có bài nào chờ rà soát')
    expect(goi.mock.calls[0][0]).toMatchObject({ status: 'PENDING', page: 0 })

    await userEvent.click(screen.getByText('Không hợp lệ'))

    // Không kiểm `calls[1]` theo chỉ số: antd Segmented có thể render lại nhiều lần, nên hỏi "đã có lần gọi
    // nào với INVALID chưa" mới là điều thật sự cần đúng
    expect(goi.mock.calls.some(([p]) => p.status === 'INVALID')).toBe(true)
  })

  it('bấm Xem thì mở thẻ chi tiết kèm nhận định của AI và hai nút kết luận', async () => {
    vi.spyOn(integrityApi, 'danhSachGanCo').mockResolvedValue(trang([baoCao()]))
    vi.spyOn(integrityApi, 'baoCao').mockResolvedValue(baoCao())

    hienThi()
    await userEvent.click(await screen.findByRole('button', { name: 'Xem' }))

    expect(await screen.findByText('Tính toàn vẹn bài thi')).toBeInTheDocument()
    expect(screen.getByText('Nhận định của AI')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Hợp lệ/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Không hợp lệ/ })).toBeInTheDocument()
  })

  it('bài ĐÃ kết luận thì không còn nút kết luận nữa', async () => {
    // Hai lần kết luận trên cùng một bài là hai lần ghi đè lặng lẽ; nút phải biến mất, không chỉ đổi màu
    const daXong = baoCao({
      reviewStatus: 'INVALID',
      reviewedAt: '2026-08-17T10:00:00Z',
      reviewNote: 'Dán 1500 ký tự giữa bài',
    })
    vi.spyOn(integrityApi, 'danhSachGanCo').mockResolvedValue(trang([daXong]))
    vi.spyOn(integrityApi, 'baoCao').mockResolvedValue(daXong)

    hienThi()
    await userEvent.click(await screen.findByRole('button', { name: 'Xem' }))

    expect(await screen.findByText('Đã đánh dấu không hợp lệ')).toBeInTheDocument()
    expect(screen.getByText(/Dán 1500 ký tự giữa bài/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /^Hợp lệ$/ })).not.toBeInTheDocument()
  })
})
