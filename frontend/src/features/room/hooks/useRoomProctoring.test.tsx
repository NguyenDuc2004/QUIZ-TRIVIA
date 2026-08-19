import { afterEach, describe, expect, it, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useRoomProctoring } from './useRoomProctoring'

/**
 * `useRoomProctoring` khác `useProctoring` của bài thi ở hai điểm, và **cả hai điểm khác biệt đó đều dễ bị
 * "sửa cho giống nhau" bởi người đọc code sau này**. Test ở đây tồn tại để chặn đúng việc đó:
 *
 * 1. **Gửi ngay, không gom lô 10 giây.** Gom lô thì cờ tới host sau khi ván đã sang câu khác — host chẳng
 *    còn gì làm với nó, và cả tính năng "cảnh báo live" mất chữ "live".
 * 2. **Gửi cả `TAB_VISIBLE`.** Bài thi cố ý bỏ chiều quay lại. Ở phòng đấu, *quay lại kịp giờ* mới là dấu
 *    hiệu — bỏ nó đi thì server không bao giờ đủ cặp để thành khuôn lặp, và tính năng im lặng chứ không đổ.
 *    Đây là kiểu hỏng tệ nhất: không có lỗi nào để đọc.
 */
describe('useRoomProctoring', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    datTrangThaiHienThi('visible')
  })

  it('phòng chờ (dangChoi = false) không gửi tín hiệu nào', () => {
    const send = vi.fn()
    renderHook(() => useRoomProctoring(send, false))

    doiTab('hidden')
    doiTab('visible')

    // Vào phòng sớm rồi đi làm việc khác trong lúc chờ host bấm bắt đầu là chuyện bình thường
    expect(send).not.toHaveBeenCalled()
  })

  it('rời trang gửi TAB_HIDDEN NGAY, không chờ hết chu kỳ gom lô', () => {
    const send = vi.fn()
    renderHook(() => useRoomProctoring(send, true))

    doiTab('hidden')

    // Không có advanceTimersByTime ở đây là chủ ý: nếu ai đó thêm gom lô vào hook thì test này đỏ
    expect(send).toHaveBeenCalledTimes(1)
    expect(send).toHaveBeenCalledWith('proctoring', { type: 'TAB_HIDDEN' })
  })

  it('quay lại gửi TAB_VISIBLE — nửa còn lại của cặp mà khuôn lặp cần', () => {
    const send = vi.fn()
    renderHook(() => useRoomProctoring(send, true))

    doiTab('hidden')
    doiTab('visible')

    expect(send).toHaveBeenCalledTimes(2)
    expect(send.mock.calls.map((call) => call[1].type)).toEqual(['TAB_HIDDEN', 'TAB_VISIBLE'])
  })

  it('không gửi kèm mốc thời gian hay danh tính — server tự lấy cả hai', () => {
    const send = vi.fn()
    renderHook(() => useRoomProctoring(send, true))

    doiTab('hidden')

    // Kiểm cả gói tin, không chỉ kiểm `type` có mặt: tin client gửi mốc thời gian lên thì một client sửa
    // đổi có thể dồn mọi tín hiệu vào một câu để không bao giờ thành khuôn lặp.
    expect(send.mock.calls[0][1]).toEqual({ type: 'TAB_HIDDEN' })
  })

  it('tháo listener khi rời trang phòng đấu', () => {
    const send = vi.fn()
    const { unmount } = renderHook(() => useRoomProctoring(send, true))

    unmount()
    doiTab('hidden')

    expect(send).not.toHaveBeenCalled()
  })

  it('ván kết thúc (dangChoi về false) thì ngừng gửi', () => {
    const send = vi.fn()
    const { rerender } = renderHook(({ choi }) => useRoomProctoring(send, choi), {
      initialProps: { choi: true },
    })

    doiTab('hidden')
    expect(send).toHaveBeenCalledTimes(1)

    rerender({ choi: false })
    doiTab('visible')

    // Vẫn đúng 1: sau khi ván xong, rời trang không còn là tín hiệu gì cả
    expect(send).toHaveBeenCalledTimes(1)
  })
})

function datTrangThaiHienThi(giaTri: DocumentVisibilityState) {
  Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => giaTri })
}

/** jsdom không tự đổi `visibilityState` khi phát sự kiện, nên phải đặt tay rồi mới phát. */
function doiTab(giaTri: DocumentVisibilityState) {
  datTrangThaiHienThi(giaTri)
  document.dispatchEvent(new Event('visibilitychange'))
}
