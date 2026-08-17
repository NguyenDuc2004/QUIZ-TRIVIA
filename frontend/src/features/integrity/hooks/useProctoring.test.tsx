import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useProctoring } from './useProctoring'
import { integrityApi } from '../api/integrityApi'

/**
 * `useProctoring` là chỗ duy nhất trong dự án đọc bàn phím/clipboard của người đang thi, nên nó cần test
 * chặt hơn một hook thường. Bốn điều phải giữ đúng, và cả bốn đều là **ràng buộc của đặc tả**, không phải
 * tuỳ chọn kỹ thuật:
 *
 * 1. **Chế độ luyện tập không thu gì.** Bên gọi truyền `enabled = false`; nếu hook vẫn gắn listener thì lời
 *    hứa in trên màn làm bài trở thành sai sự thật.
 * 2. **Không bao giờ gửi nội dung đã dán** — chỉ độ dài. Đây là điều dễ vỡ nhất khi ai đó sửa hook về sau,
 *    nên test kiểm cả *không có trường nào chứa chuỗi đó*, chứ không chỉ kiểm `length` có mặt.
 * 3. **Gom lô rồi gửi**, không gửi từng tín hiệu: người đang thi không nên chịu thêm một request mỗi lần
 *    bấm Ctrl+C.
 * 4. **Lỗi gửi không được vỡ ra ngoài.** Cơ chế giám sát gặp sự cố thì bài thi vẫn phải chạy.
 */
describe('useProctoring', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.spyOn(integrityApi, 'guiSuKien').mockResolvedValue({ soSuKienDaGhi: 0 })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
    datTrangThaiHienThi('visible')
  })

  it('lượt luyện tập (enabled = false) không gửi gì dù có tín hiệu', () => {
    renderHook(() => useProctoring('a1', false))

    document.dispatchEvent(new Event('copy'))
    window.dispatchEvent(new Event('blur'))
    vi.advanceTimersByTime(30_000)

    expect(integrityApi.guiSuKien).not.toHaveBeenCalled()
  })

  it('không có attemptId thì không gửi gì', () => {
    renderHook(() => useProctoring(undefined, true))

    document.dispatchEvent(new Event('copy'))
    vi.advanceTimersByTime(30_000)

    expect(integrityApi.guiSuKien).not.toHaveBeenCalled()
  })

  it('dán chỉ gửi ĐỘ DÀI, không có nội dung nào rời khỏi trình duyệt', () => {
    const noiDungBiMat = 'đáp án chép từ chỗ khác'
    renderHook(() => useProctoring('a1', true))

    danVanBan(noiDungBiMat)
    vi.advanceTimersByTime(10_000)

    const [, lo] = vi.mocked(integrityApi.guiSuKien).mock.calls[0]
    expect(lo).toHaveLength(1)
    expect(lo[0].type).toBe('PASTE')
    expect(lo[0].length).toBe(noiDungBiMat.length)
    // Kiểm cả gói tin, không chỉ trường `length`: nếu ai đó thêm một trường `text` vào sau này thì test
    // phải đỏ, chứ không im lặng đi qua vì `length` vẫn đúng.
    expect(JSON.stringify(lo)).not.toContain(noiDungBiMat)
  })

  it('gom nhiều tín hiệu vào MỘT request, không gửi từng cái', () => {
    renderHook(() => useProctoring('a1', true))

    document.dispatchEvent(new Event('copy'))
    document.dispatchEvent(new Event('copy'))
    window.dispatchEvent(new Event('blur'))
    vi.advanceTimersByTime(10_000)

    expect(integrityApi.guiSuKien).toHaveBeenCalledTimes(1)
    const [attemptId, lo] = vi.mocked(integrityApi.guiSuKien).mock.calls[0]
    expect(attemptId).toBe('a1')
    expect(lo.map((e) => e.type)).toEqual(['COPY', 'COPY', 'WINDOW_BLUR'])
  })

  it('chỉ ghi lúc trang bị ẩn, KHÔNG ghi lúc quay lại', () => {
    // Ghi cả hai chiều làm số lần chuyển tab bị đếm gấp đôi, và điểm rủi ro theo đó cũng gấp đôi
    renderHook(() => useProctoring('a1', true))

    datTrangThaiHienThi('hidden')
    document.dispatchEvent(new Event('visibilitychange'))
    datTrangThaiHienThi('visible')
    document.dispatchEvent(new Event('visibilitychange'))
    vi.advanceTimersByTime(10_000)

    const [, lo] = vi.mocked(integrityApi.guiSuKien).mock.calls[0]
    expect(lo.map((e) => e.type)).toEqual(['TAB_HIDDEN'])
  })

  it('không có tín hiệu nào thì không gửi request rỗng', () => {
    renderHook(() => useProctoring('a1', true))

    vi.advanceTimersByTime(60_000)

    expect(integrityApi.guiSuKien).not.toHaveBeenCalled()
  })

  it('gửi phần còn lại khi rời màn làm bài', () => {
    const { unmount } = renderHook(() => useProctoring('a1', true))

    document.dispatchEvent(new Event('copy'))
    unmount()   // chưa tới chu kỳ 10 giây nào

    expect(integrityApi.guiSuKien).toHaveBeenCalledTimes(1)
  })

  it('tháo listener khi rời màn hình: tín hiệu sau đó không được thu nữa', () => {
    const { unmount } = renderHook(() => useProctoring('a1', true))
    unmount()
    vi.mocked(integrityApi.guiSuKien).mockClear()

    document.dispatchEvent(new Event('copy'))
    vi.advanceTimersByTime(30_000)

    expect(integrityApi.guiSuKien).not.toHaveBeenCalled()
  })

  it('server lỗi thì hook im lặng, không ném ra màn làm bài', async () => {
    vi.mocked(integrityApi.guiSuKien).mockRejectedValue(new Error('500'))
    renderHook(() => useProctoring('a1', true))

    document.dispatchEvent(new Event('copy'))
    expect(() => vi.advanceTimersByTime(10_000)).not.toThrow()
    await vi.runOnlyPendingTimersAsync()

    expect(integrityApi.guiSuKien).toHaveBeenCalled()
  })
})

/** jsdom không cho gán `document.visibilityState` trực tiếp. */
function datTrangThaiHienThi(giaTri: DocumentVisibilityState) {
  Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => giaTri })
}

/**
 * jsdom không dựng được `ClipboardEvent` kèm `clipboardData`, nên gắn tay đúng thứ hook đọc.
 * Chuỗi thật được đưa vào để test chứng minh nó **không** đi ra ngoài.
 */
function danVanBan(noiDung: string) {
  const su = new Event('paste') as Event & { clipboardData: { getData: () => string } }
  su.clipboardData = { getData: () => noiDung }
  document.dispatchEvent(su)
}
