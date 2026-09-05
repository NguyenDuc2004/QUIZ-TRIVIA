import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'
import XemTruocCongThuc, { coDanhDauCongThuc } from './XemTruocCongThuc'

/**
 * Ranh giới quan trọng nhất ở đây: component này **đoán**, nhưng chỉ đoán để ĐỀ NGHỊ.
 * Đoán sai thì người viết bỏ qua một dòng chữ — khác hẳn đoán sai lúc tự đổi nội dung.
 */
describe('XemTruocCongThuc', () => {
  it('đã có $...$ thì dựng công thức để người viết kiểm lại', () => {
    const { container } = render(<XemTruocCongThuc noiDung={'Đạo hàm của $y = x^2$ là gì?'} />)
    expect(container.querySelector('.katex')).not.toBeNull()
    expect(container.textContent).toContain('Xem trước')
  })

  it('trông như toán mà CHƯA đánh dấu thì nhắc cú pháp, KHÔNG tự sửa', () => {
    const { container } = render(<XemTruocCongThuc noiDung="Đạo hàm của y = x^2 là gì?" />)
    expect(container.textContent).toContain('$...$')
    // Không dựng gì cả: nội dung của người viết giữ nguyên xi cho tới khi chính họ đánh dấu.
    expect(container.querySelector('.katex')).toBeNull()
  })

  it('chữ thường thì KHÔNG hiện gì — nhắc thường trực sẽ bị đọc lướt qua', () => {
    const { container } = render(<XemTruocCongThuc noiDung="MTP là Sơn Tùng, đúng hay sai?" />)
    expect(container.innerHTML).toBe('')
  })

  it('KHÔNG nhắc vì dấu gạch chéo — "km/h", "và/hoặc" đầy trong câu chữ bình thường', () => {
    // Một gợi ý nhảy ra sai chỗ vài lần là người dùng thôi đọc nó.
    const { container } = render(<XemTruocCongThuc noiDung="Vận tốc 60 km/h nghĩa là gì?" />)
    expect(container.innerHTML).toBe('')
  })

  it('rỗng thì không đổ', () => {
    const { container } = render(<XemTruocCongThuc noiDung="" />)
    expect(container.innerHTML).toBe('')
  })

  it('coDanhDauCongThuc: một dấu $ lẻ hay $$ rỗng đều KHÔNG tính là công thức', () => {
    expect(coDanhDauCongThuc('Chi phí $50 cho mỗi lần gọi')).toBe(false)
    expect(coDanhDauCongThuc('Giá là 100$$ một tháng')).toBe(false)
    expect(coDanhDauCongThuc('Cho $x^2$ bất kỳ')).toBe(true)
  })
})
