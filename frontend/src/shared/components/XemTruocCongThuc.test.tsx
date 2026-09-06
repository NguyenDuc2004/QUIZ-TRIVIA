import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'
import XemTruocCongThuc, { bocCongThuc, coDanhDauCongThuc } from './XemTruocCongThuc'
import MathText from './MathText'

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

/**
 * `bocCongThuc` là phần ĐOÁN. Nó được phép đoán vì người viết nhìn thấy kết quả ngay và sửa lại được
 * — nhưng đoán càng sát thì càng ít lần họ phải sửa.
 */
describe('bocCongThuc', () => {
  it('khoanh đúng đoạn toán giữa câu tiếng Việt', () => {
    // Chữ có dấu (ố, à) không thuộc tập ký tự toán, nên chúng tự cắt biên cho công thức.
    expect(bocCongThuc('Đạo hàm của hàm số y = x^2 là:')).toBe('Đạo hàm của hàm số $y = x^2$ là:')
  })

  it('KHÔNG nuốt dấu chấm cuối câu vào công thức', () => {
    // Dấu chấm là của CÂU VĂN. Để nó lọt vào thì KaTeX dựng luôn cả dấu chấm.
    expect(bocCongThuc('Cho hàm số y = x^3 - 3x^2 + 2. Hàm số đồng biến?')).toBe(
      'Cho hàm số $y = x^3 - 3x^2 + 2$. Hàm số đồng biến?',
    )
  })

  it('cả ô chỉ có công thức thì bọc trọn', () => {
    expect(bocCongThuc("y' = (2x - 1) . 2^(x^2 - x)")).toBe("$y' = (2x - 1) . 2^(x^2 - x)$")
  })

  it('đã có $...$ rồi thì KHÔNG đụng vào', () => {
    // Người viết đã tự đánh dấu nghĩa là họ đã chọn ranh giới của mình. Bọc chồng lên là phá đúng
    // thứ họ vừa làm đúng.
    const daCo = 'Đạo hàm của $y = x^2$ và của y = x^3 là:'
    expect(bocCongThuc(daCo)).toBe(daCo)
  })

  it('chữ thường không có dấu hiệu toán thì giữ nguyên xi', () => {
    expect(bocCongThuc('MTP là Sơn Tùng, đúng hay sai?')).toBe('MTP là Sơn Tùng, đúng hay sai?')
    expect(bocCongThuc('Vận tốc 60 km/h nghĩa là gì?')).toBe('Vận tốc 60 km/h nghĩa là gì?')
  })

  it('rỗng thì trả rỗng, không đổ', () => {
    expect(bocCongThuc('')).toBe('')
  })

  it('kết quả bọc ra phải DỰNG ĐƯỢC, không chỉ đúng chuỗi', () => {
    const { container } = render(<MathText>{bocCongThuc('Đạo hàm của hàm số y = x^2 là:')}</MathText>)
    expect(container.querySelector('.katex')).not.toBeNull()
    expect(container.textContent).toContain('Đạo hàm của hàm số')
  })
})

/**
 * Hai ca dưới đây do **thử tay bắt được**, không phải do nghĩ ra trước. Chúng lộ đúng một điểm mù:
 * tiếng Việt có rất nhiều từ **không dấu** (`khi`, `cho`, `tam`, `va`), nên xét theo ký tự thì chúng
 * giống hệt biến số. Bộ kiểm ban đầu chỉ có câu chứa chữ có dấu nên xanh hết mà vẫn sai.
 */
describe('bocCongThuc — từ tiếng Việt KHÔNG DẤU', () => {
  it('không nuốt từ không dấu nằm GIỮA hai vế toán', () => {
    // Trước khi sửa: `$x^2 + y^2 khi x = 3$` — chữ "khi" bị dựng thành ký hiệu nghiêng giữa câu.
    expect(bocCongThuc('Tính giá trị của x^2 + y^2 khi x = 3 và y = 4')).toBe(
      'Tính giá trị của $x^2 + y^2$ khi x = 3 và y = 4',
    )
  })

  it('không nuốt từ không dấu ở HAI ĐẦU', () => {
    // Trước khi sửa: `$Cho a^2 + b^2 = c^2, tam$ giác…`
    expect(bocCongThuc('Cho a^2 + b^2 = c^2, tam giác đó là tam giác gì?')).toBe(
      'Cho $a^2 + b^2 = c^2$, tam giác đó là tam giác gì?',
    )
  })

  it('nhưng TÊN HÀM TOÁN thì vẫn thuộc về công thức', () => {
    // `ln` cũng là hai chữ cái thuần. Cắt ở đó thì đáp án của mọi câu đạo hàm hàm mũ đều hỏng.
    expect(bocCongThuc("y' = (2x - 1) . 2^(x^2 - x) . ln 2")).toBe(
      "$y' = (2x - 1) . 2^(x^2 - x) . ln 2$",
    )
  })
})
