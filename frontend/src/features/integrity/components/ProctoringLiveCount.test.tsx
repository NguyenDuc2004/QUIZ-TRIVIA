import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import ProctoringLiveCount from './ProctoringLiveCount'

/**
 * Phần lớn giá trị của thành phần này nằm ở **cách nói**, không ở việc nó render được.
 *
 * Toàn bộ thiết kế chống gian lận của hệ thống dựng trên nguyên tắc *hệ thống đưa dữ kiện, con người kết
 * luận* — và báo cáo phía giáo viên in thẳng câu đó ra. Nếu màn hình người thi lại nói "bạn đã vi phạm" thì
 * hai đầu của cùng một cơ chế nói ngược nhau, mà không có phép kiểm nào bắt được kiểu mâu thuẫn đó ngoài
 * một phép kiểm nhìn vào chính câu chữ.
 */
describe('ProctoringLiveCount', () => {
  it('chưa có tín hiệu nào thì không hiện gì', () => {
    const { container } = render(
      <ProctoringLiveCount soLan={{ roiTrang: 0, dan: 0, thoatToanManHinh: 0 }} />,
    )

    // Một dòng "đã ghi nhận: 0 lần" thường trực là lời nhắc liên tục rằng người thi đang bị theo dõi,
    // trong khi họ chưa làm gì cả.
    expect(container).toBeEmptyDOMElement()
  })

  it('chỉ liệt kê loại tín hiệu ĐÃ xảy ra, không liệt kê loại bằng 0', () => {
    render(<ProctoringLiveCount soLan={{ roiTrang: 2, dan: 0, thoatToanManHinh: 0 }} />)

    expect(screen.getByText(/2 lần rời trang/)).toBeInTheDocument()
    expect(screen.queryByText(/dán nội dung/)).not.toBeInTheDocument()
    expect(screen.queryByText(/toàn màn hình/)).not.toBeInTheDocument()
  })

  it('nhiều loại tín hiệu thì gộp trên một dòng', () => {
    render(<ProctoringLiveCount soLan={{ roiTrang: 3, dan: 1, thoatToanManHinh: 2 }} />)

    expect(screen.getByText(/3 lần rời trang/)).toBeInTheDocument()
    expect(screen.getByText(/1 lần dán nội dung/)).toBeInTheDocument()
    expect(screen.getByText(/2 lần thoát toàn màn hình/)).toBeInTheDocument()
  })

  it('KHÔNG dùng chữ mang nghĩa buộc tội', () => {
    const { container } = render(
      <ProctoringLiveCount soLan={{ roiTrang: 5, dan: 3, thoatToanManHinh: 1 }} />,
    )
    const chu = container.textContent ?? ''

    // Rời tab một lần vì thông báo bật lên KHÔNG phải gian lận. Chỉ giáo viên — người biết hoàn cảnh lớp
    // mình — mới có tư cách kết luận điều đó.
    for (const tuCam of ['gian lận', 'cảnh cáo', 'bị phạt', 'vi phạm quy chế']) {
      expect(chu.toLowerCase()).not.toContain(tuCam)
    }

    // Và phải nói rõ ai mới là người kết luận, ngay cạnh con số chứ không ở đâu khác
    expect(chu).toMatch(/không phải kết luận/i)
    expect(chu).toMatch(/giáo viên/i)
  })
})
