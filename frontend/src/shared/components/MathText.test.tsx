import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'
import MathText from './MathText'

/**
 * Thành phần này chạm vào **nội dung do người dùng viết**, nên rủi ro lớn nhất không phải "công thức
 * không đẹp" mà là **làm hỏng chữ bình thường**. Phần lớn phép kiểm dưới đây canh đúng ranh giới đó.
 */
describe('MathText', () => {
  it('chữ thường giữ nguyên từng ký tự', () => {
    const { container } = render(<MathText>Đạo hàm của hàm số y = 2^(x^2 - x) là:</MathText>)
    expect(container.textContent).toBe('Đạo hàm của hàm số y = 2^(x^2 - x) là:')
  })

  it('KHÔNG tự nhận diện toán khi không có dấu $', () => {
    // Đây là bất biến quan trọng nhất. Tự đoán chỗ nào là công thức sẽ bóp méo những câu hoàn toàn
    // bình thường: một câu Tin học nhắc `a/b`, một câu Tiếng Anh có dấu mũ.
    const { container } = render(<MathText>Tỉ lệ a/b và ký hiệu x^2 trong văn bản</MathText>)
    expect(container.querySelector('.katex')).toBeNull()
    expect(container.textContent).toContain('a/b')
  })

  it('dựng phần nằm giữa hai dấu $ thành công thức', () => {
    const { container } = render(<MathText>{'Đạo hàm của $y = x^2$ là gì?'}</MathText>)
    expect(container.querySelector('.katex')).not.toBeNull()
    // Chữ ngoài công thức vẫn còn nguyên
    expect(container.textContent).toContain('Đạo hàm của')
    expect(container.textContent).toContain('là gì?')
  })

  it('hai dấu $ liền nhau là chữ, và giữ NGUYÊN cả hai dấu', () => {
    // Câu hỏi về giá tiền hoặc về cú pháp shell có thể chứa `$`. Biến chúng thành công thức là một
    // lỗi im lặng — không báo gì, chỉ hiện sai.
    //
    // Và không được "thoát" bằng cách gộp `$$` thành `$`: người viết gõ hai dấu mà màn hình hiện một
    // cũng là sửa chữ của họ, chỉ theo hướng khác. Không đổi gì là hành vi duy nhất không bao giờ sai.
    const { container } = render(<MathText>{'Giá là 100$$ một tháng'}</MathText>)
    expect(container.querySelector('.katex')).toBeNull()
    expect(container.textContent).toBe('Giá là 100$$ một tháng')
  })

  it('dấu $ lẻ không có dấu đóng thì giữ nguyên làm chữ', () => {
    const { container } = render(<MathText>{'Chi phí $50 cho mỗi lần gọi'}</MathText>)
    expect(container.querySelector('.katex')).toBeNull()
    expect(container.textContent).toBe('Chi phí $50 cho mỗi lần gọi')
  })

  it('công thức sai cú pháp vẫn hiện được, không làm trắng trang', () => {
    // Người học không sửa được LaTeX của người ra đề. Ném lỗi ở đây là phạt nhầm người.
    const { container } = render(<MathText>{'Thử $\\frac{a$ xem sao'}</MathText>)
    expect(container.textContent).toContain('Thử')
    expect(container.textContent).toContain('xem sao')
  })

  it('chuỗi rỗng không làm đổ', () => {
    const { container } = render(<MathText>{''}</MathText>)
    expect(container.textContent).toBe('')
  })
})
