import { describe, expect, it } from 'vitest'
import { render } from '@testing-library/react'
import QuizCover from './QuizCover'

/**
 * Bất biến DUY NHẤT mà thành phần này tồn tại để giữ: **khuôn bìa không đổi kích thước theo nội dung
 * bên trong nó.**
 *
 * Lỗi người dùng chỉ ra: thẻ có ảnh thật cao hơn thẻ vẽ gradient khoảng 40px. Nguyên nhân là ảnh dùng
 * `h-full`, mà `height: 100%` không phân giải được khi chiều cao cha do `aspect-ratio` suy ra — ảnh rơi
 * về chiều cao gốc của tệp và kéo khung theo.
 *
 * jsdom không tính bố cục nên không đo được pixel. Nhưng đo pixel không phải cách chặn đúng: thứ cần
 * chặn là **cấu trúc** sinh ra lỗi đó — khung phải giống hệt nhau ở hai nhánh, và mọi thứ bên trong
 * phải `absolute` để không tác động được tới khung.
 */
function khungCua(container: HTMLElement) {
  return container.firstElementChild as HTMLElement
}

describe('QuizCover', () => {
  it('khung có ảnh và khung không ảnh mang ĐÚNG CÙNG một bộ lớp', () => {
    const coAnh = render(
      <QuizCover thumbnailUrl="/anh.png" categoryName="Toán học" title="Đạo hàm" />,
    )
    const khongAnh = render(
      <QuizCover thumbnailUrl={null} categoryName="Toán học" title="Đạo hàm" />,
    )

    expect(khungCua(coAnh.container).className).toBe(khungCua(khongAnh.container).className)
  })

  it('ảnh phải định vị TUYỆT ĐỐI — đây chính là chỗ đã hỏng', () => {
    // `h-full` thì ảnh lấy chiều cao gốc của tệp và kéo khung phình ra. `absolute inset-0` đo theo hộp
    // của phần tử định vị gần nhất — một giá trị xác định — nên ảnh không còn tác động tới khung.
    const { container } = render(
      <QuizCover thumbnailUrl="/anh.png" categoryName={null} title="Đạo hàm" />,
    )
    const anh = container.querySelector('img')!

    expect(anh.className).toContain('absolute')
    expect(anh.className).toContain('inset-0')
    expect(anh.className).toContain('object-cover')
  })

  it('khung luôn giữ tỉ lệ 16:9 và cắt phần thừa', () => {
    // `object-cover` + khuôn cố định là điều kiện để MỌI tệp người dùng tải lên — vuông, dọc, siêu
    // rộng — đều hiện ra cùng một kích thước.
    const { container } = render(<QuizCover thumbnailUrl="/anh.png" categoryName={null} title="X" />)

    expect(khungCua(container).className).toContain('aspect-video')
    expect(khungCua(container).className).toContain('overflow-hidden')
  })

  it('chưa có ảnh thì vẫn vẽ khối màu kèm biểu tượng, không để trống', () => {
    const { container } = render(
      <QuizCover thumbnailUrl={null} categoryName="Tin học" title="Nhập môn Web" />,
    )

    expect(container.querySelector('img')).toBeNull()
    expect(khungCua(container).getAttribute('style')).toContain('background')
    expect(container.textContent?.trim()).not.toBe('')
  })

  it('cùng một quiz cho ra cùng một biểu tượng ở mọi cỡ — chỉ cỡ chữ đổi', () => {
    const lon = render(<QuizCover thumbnailUrl={null} categoryName="Tin học" title="X" />)
    const nho = render(
      <QuizCover thumbnailUrl={null} categoryName="Tin học" title="X" coIcon="nho" />,
    )

    expect(lon.container.textContent).toBe(nho.container.textContent)
    expect(khungCua(lon.container).getAttribute('style')).toBe(
      khungCua(nho.container).getAttribute('style'),
    )
  })

  it('nhãn danh mục chỉ hiện khi được yêu cầu', () => {
    const khong = render(<QuizCover thumbnailUrl={null} categoryName="Vật lý" title="X" />)
    const co = render(<QuizCover thumbnailUrl={null} categoryName="Vật lý" title="X" hienNhan />)

    expect(khong.container.textContent).not.toContain('Vật lý')
    expect(co.container.textContent).toContain('Vật lý')
  })
})
