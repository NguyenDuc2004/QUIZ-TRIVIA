import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useStrictExam } from './useStrictExam'

/**
 * `useStrictExam` là chỗ **dễ hứa quá tay nhất** trong cả tính năng chống gian lận: cái tên nghe như một
 * cái khoá, nhưng trình duyệt không cho khoá ai cả. Test ở đây giữ đúng ba ranh giới:
 *
 * 1. **Luyện tập không bị đụng gì** — `enabled = false` thì không gắn một listener nào. Chuột phải vẫn dùng
 *    được, không có gì thay đổi. Đây là ràng buộc của đặc tả, không phải tuỳ chọn.
 * 2. **Trình duyệt từ chối thì KHÔNG được ném lỗi** — Safari trên iPhone không có Fullscreen API cho phần
 *    tử thường. Ném lỗi ở đó là biến một hạn chế của thiết bị thành mất quyền dự thi.
 * 3. **Rời màn làm bài phải trả màn hình lại** — không thì người học nộp bài xong vẫn kẹt toàn màn hình.
 */
describe('useStrictExam', () => {
  // jsdom KHÔNG cài Fullscreen API — đúng loại môi trường mà hook này phải chịu được (Safari trên iPhone
  // cũng không có). Gắn stub để spy được; hành vi "trình duyệt từ chối" thì mock riêng ở từng test.
  beforeEach(() => {
    Object.defineProperty(document.documentElement, 'requestFullscreen', {
      configurable: true,
      writable: true,
      value: () => Promise.resolve(),
    })
    Object.defineProperty(document, 'exitFullscreen', {
      configurable: true,
      writable: true,
      value: () => Promise.resolve(),
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    datFullscreenElement(null)
  })

  it('tắt (luyện tập) thì không gắn listener nào — chuột phải vẫn dùng được', () => {
    const themDoc = vi.spyOn(document, 'addEventListener')
    renderHook(() => useStrictExam(false))

    const daGan = themDoc.mock.calls.map((call) => call[0])
    expect(daGan).not.toContain('contextmenu')
    expect(daGan).not.toContain('fullscreenchange')
  })

  it('tắt thì dangToanManHinh luôn false, kể cả khi trình duyệt đang ở toàn màn hình', () => {
    // Người học mở toàn màn hình vì lý do riêng của họ (xem video ở tab khác chẳng hạn). Lượt luyện tập
    // không được coi đó là một trạng thái của bài thi.
    datFullscreenElement(document.body)
    const { result } = renderHook(() => useStrictExam(false))

    expect(result.current.dangToanManHinh).toBe(false)
  })

  it('bật thì khoá chuột phải', () => {
    renderHook(() => useStrictExam(true))

    const su = new MouseEvent('contextmenu', { cancelable: true, bubbles: true })
    document.dispatchEvent(su)

    expect(su.defaultPrevented).toBe(true)
  })

  it('trình duyệt từ chối toàn màn hình thì trả false, KHÔNG ném lỗi', async () => {
    // Safari trên iPhone không có Fullscreen API cho phần tử thường. Ném lỗi ở đây là biến một hạn chế
    // của thiết bị thành mất quyền dự thi.
    vi.spyOn(document.documentElement, 'requestFullscreen')
      .mockRejectedValue(new Error('Fullscreen request denied'))

    const { result } = renderHook(() => useStrictExam(true))

    await expect(act(() => result.current.vaoToanManHinh())).resolves.not.toThrow()
  })

  it('vào toàn màn hình thành công thì trả true', async () => {
    vi.spyOn(document.documentElement, 'requestFullscreen').mockResolvedValue(undefined)

    const { result } = renderHook(() => useStrictExam(true))

    let duoc: boolean | undefined
    await act(async () => {
      duoc = await result.current.vaoToanManHinh()
    })
    expect(duoc).toBe(true)
  })

  it('theo dõi được lúc người học thoát toàn màn hình', () => {
    datFullscreenElement(document.body)
    const { result } = renderHook(() => useStrictExam(true))
    expect(result.current.dangToanManHinh).toBe(true)

    // Người dùng bấm Esc — không API nào chặn được, nên thứ duy nhất làm được là biết nó đã xảy ra
    act(() => {
      datFullscreenElement(null)
      document.dispatchEvent(new Event('fullscreenchange'))
    })

    expect(result.current.dangToanManHinh).toBe(false)
  })

  it('rời màn làm bài thì tháo listener và trả màn hình lại như cũ', () => {
    datFullscreenElement(document.body)
    const thoat = vi.spyOn(document, 'exitFullscreen').mockResolvedValue(undefined)

    const { unmount } = renderHook(() => useStrictExam(true))
    unmount()

    // Không thoát thì người học nộp bài xong vẫn kẹt toàn màn hình và phải tự bấm Esc
    expect(thoat).toHaveBeenCalled()

    // Và chuột phải phải dùng lại được ngay
    const su = new MouseEvent('contextmenu', { cancelable: true, bubbles: true })
    document.dispatchEvent(su)
    expect(su.defaultPrevented).toBe(false)
  })
})

/** jsdom không có Fullscreen API — gắn tay đúng thứ hook đọc. */
function datFullscreenElement(giaTri: Element | null) {
  Object.defineProperty(document, 'fullscreenElement', {
    configurable: true,
    get: () => giaTri,
  })
}
