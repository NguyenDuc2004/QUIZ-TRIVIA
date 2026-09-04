import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MessageBubble } from './AssistantPage'
import type { ChatMessage } from '../api/chatApi'

function tin(role: 'USER' | 'ASSISTANT', content: string, sources: ChatMessage['sources'] = []) {
  return { id: '1', role, content, sources, createdAt: new Date().toISOString() }
}

describe('MessageBubble', () => {
  it('dựng công thức trong câu trả lời của trợ lý', () => {
    // Trợ lý được dặn viết công thức trong `$...$` (ChatPromptBuilder). Không dựng thì người học đọc
    // đúng chuỗi ký tự "$y = x^2$" — tệ hơn cả khi chưa có KaTeX, vì có thêm hai dấu đô la thừa.
    const { container } = render(
      <MessageBubble item={tin('ASSISTANT', 'Đạo hàm của $y = x^2$ là $y\' = 2x$')} isStreaming={false} />,
    )
    expect(container.querySelectorAll('.katex').length).toBe(2)
  })

  it('KHÔNG dựng công thức trong câu hỏi của người dùng', () => {
    // Người học gõ tự do. Câu này có đủ dấu mở và dấu đóng, nhưng phần ở giữa là giá tiền chứ không
    // phải công thức — dựng nó lên là bóp méo đúng câu chữ họ vừa viết.
    const { container } = render(
      <MessageBubble item={tin('USER', 'Sách giá 100$ còn khoá học 200$ thì mua cái nào?')} isStreaming={false} />,
    )
    expect(container.querySelector('.katex')).toBeNull()
    expect(container.textContent).toContain('100$')
    expect(container.textContent).toContain('200$')
  })

  it('công thức chưa đóng dấu (đang stream) vẫn hiện được, không mất chữ', () => {
    const { container } = render(
      <MessageBubble item={tin('ASSISTANT', 'Đạo hàm của $y = x^')} isStreaming />,
    )
    expect(container.querySelector('.katex')).toBeNull()
    expect(container.textContent).toContain('Đạo hàm của $y = x^')
  })

  it('trích dẫn nguồn giữ nguyên xi, không được dựng thành công thức', () => {
    // Đoạn trích là thứ người học dùng để ĐỐI CHIẾU với tài liệu gốc, nên nó không được đẹp hơn sự thật.
    render(
      <MessageBubble
        item={tin('ASSISTANT', 'Xem tài liệu.', [
          { materialId: 'm1', title: 'Vở ghi', excerpt: 'Đạo hàm của $y = x^2$ là $2x$' },
        ])}
        isStreaming={false}
      />,
    )
    expect(screen.getByText(/Đạo hàm của \$y = x\^2\$ là \$2x\$/)).toBeTruthy()
  })
})
