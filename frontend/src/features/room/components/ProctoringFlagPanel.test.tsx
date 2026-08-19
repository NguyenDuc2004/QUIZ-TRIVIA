import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import ProctoringFlagPanel from './ProctoringFlagPanel'
import type { ProctoringFlag } from '../api/roomApi'

/**
 * Bảng này là **giao diện của một lời nghi ngờ về người khác, hiện ra giữa lúc đang thi**. Nên test hỏi bốn
 * điều về nội dung, không chỉ hỏi "có render không":
 *
 * 1. **Câu "không phải kết luận" phải hiện TRƯỚC danh sách.** Host có ba giây; đọc tên kèm cờ đỏ rồi mới thấy
 *    lời nhắc thì đã kết luận xong.
 * 2. **Không có nút trừ điểm, không có nút đuổi.** Đây là ràng buộc đã chốt của đặc tả, không phải thứ chưa
 *    kịp làm — nên nó cần một test giữ, để lần sau không ai "bổ sung cho đủ".
 * 3. **Lý do cụ thể phải hiện ra**, không chỉ tên người.
 * 4. **Nhắc rồi thì không nhắc lại được.** Nhắc lần hai không thêm thông tin cho người nhận, chỉ thêm một
 *    hộp thoại bật lên giữa lúc họ đang trả lời.
 */

function co(ghiDe: Partial<ProctoringFlag> = {}): ProctoringFlag {
  return {
    playerId: 'p1',
    displayName: 'Trần Minh Quân',
    guest: false,
    soCauLap: 2,
    lyDo: 'Rời trang rồi quay lại trong lúc còn thời gian trả lời, ở 2 câu',
    ...ghiDe,
  }
}

describe('ProctoringFlagPanel', () => {
  it('không có cờ nào thì không hiện gì cả', () => {
    const { container } = render(
      <ProctoringFlagPanel flags={[]} daNhac={[]} onNhac={vi.fn()} />,
    )

    // Bảng trống vẫn chiếm chỗ trên màn hình host giữa ván, và "không có gì" không cần một khung để nói
    expect(container).toBeEmptyDOMElement()
  })

  it('hiện câu "không phải kết luận" TRƯỚC tên người bị nghi', () => {
    render(<ProctoringFlagPanel flags={[co()]} daNhac={[]} onNhac={vi.fn()} />)

    const canhBao = screen.getByText(/dữ kiện, không phải kết luận/i)
    const ten = screen.getByText('Trần Minh Quân')

    // So thứ tự thật trong DOM, không chỉ kiểm "cả hai đều có mặt"
    expect(canhBao.compareDocumentPosition(ten) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('nói rõ tín hiệu có thể bị giả mạo và hệ thống không trừ điểm', () => {
    render(<ProctoringFlagPanel flags={[co()]} daNhac={[]} onNhac={vi.fn()} />)

    expect(screen.getByText(/giả mạo|sai lệch/i)).toBeInTheDocument()
    expect(screen.getByText(/không trừ điểm/i)).toBeInTheDocument()
  })

  it('hiện lý do cụ thể, không chỉ hiện tên', () => {
    render(<ProctoringFlagPanel flags={[co()]} daNhac={[]} onNhac={vi.fn()} />)

    expect(screen.getByText(/ở 2 câu/)).toBeInTheDocument()
  })

  it('KHÔNG có nút trừ điểm và KHÔNG có nút đuổi khỏi phòng', () => {
    render(<ProctoringFlagPanel flags={[co()]} daNhac={[]} onNhac={vi.fn()} />)

    // Ràng buộc đã chốt của đặc tả: quyền của host giữa ván dừng ở "nhắc". Test này là chỗ giữ nó.
    expect(screen.queryByRole('button', { name: /trừ điểm/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /đuổi|kick|loại/i })).not.toBeInTheDocument()

    const nut = screen.getAllByRole('button')
    expect(nut).toHaveLength(1)
    expect(nut[0]).toHaveTextContent('Nhắc riêng')
  })

  it('bấm Nhắc riêng gọi đúng playerId', async () => {
    const onNhac = vi.fn()
    render(<ProctoringFlagPanel flags={[co({ playerId: 'p-9' })]} daNhac={[]} onNhac={onNhac} />)

    await userEvent.click(screen.getByRole('button', { name: 'Nhắc riêng' }))

    expect(onNhac).toHaveBeenCalledExactlyOnceWith('p-9')
  })

  it('đã nhắc rồi thì nút bị vô hiệu hoá', () => {
    render(<ProctoringFlagPanel flags={[co({ playerId: 'p-9' })]} daNhac={['p-9']} onNhac={vi.fn()} />)

    const nut = screen.getByRole('button', { name: 'Đã nhắc' })
    expect(nut).toBeDisabled()
  })

  it('khách vãng lai được gắn nhãn, và vẫn nhắc được như thành viên', () => {
    render(
      <ProctoringFlagPanel
        flags={[co({ playerId: 'g1', displayName: 'Người chơi vui', guest: true })]}
        daNhac={[]}
        onNhac={vi.fn()}
      />,
    )

    // Khách là nhóm người phòng đấu tồn tại để phục vụ; bỏ họ ra khỏi cơ chế là bỏ mất nửa phòng
    expect(screen.getByText('khách')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Nhắc riêng' })).toBeEnabled()
  })

  it('nhiều người bị gắn cờ thì mỗi người một dòng và một nút riêng', () => {
    render(
      <ProctoringFlagPanel
        flags={[co({ playerId: 'p1', displayName: 'An' }), co({ playerId: 'p2', displayName: 'Bình' })]}
        daNhac={['p1']}
        onNhac={vi.fn()}
      />,
    )

    expect(screen.getByRole('button', { name: 'Đã nhắc' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Nhắc riêng' })).toBeEnabled()
  })
})
