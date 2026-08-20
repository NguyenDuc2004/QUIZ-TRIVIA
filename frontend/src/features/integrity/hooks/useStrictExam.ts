import { useCallback, useEffect, useState } from 'react'

/**
 * Chế độ thi nghiêm ngặt (features/12, FR-48): toàn màn hình + khoá chuột phải.
 *
 * ## Điều quan trọng nhất: KHÔNG ép được toàn màn hình, và giao diện phải nói thật về điều đó
 *
 * Trình duyệt chỉ cho vào toàn màn hình **từ một cử chỉ của người dùng** (`requestFullscreen` gọi ngoài
 * handler sự kiện sẽ bị từ chối), và người dùng **luôn** bấm Esc thoát ra được — không API nào chặn nổi.
 * Chuột phải chặn được, nhưng phím tắt vẫn mở devtools.
 *
 * Nên hook này **không hứa** sẽ khoá người học trong bài thi. Cái nó làm là:
 * 1. yêu cầu một cú bấm để vào toàn màn hình trước khi làm bài — biến việc thoát ra thành **có chủ ý**;
 * 2. phát hiện lúc thoát và nhắc quay lại;
 * 3. để lại tín hiệu `FULLSCREEN_EXIT` cho `useProctoring` ghi vào hồ sơ.
 *
 * Đây đúng tinh thần của cả tính năng 12: **hệ thống đưa dữ kiện, người thật quyết định**. Một tính năng
 * quảng cáo là "khoá" mà thực ra không khoá được thì tệ hơn không có — giáo viên sẽ tin vào một rào chắn
 * không tồn tại và bỏ qua việc rà soát tín hiệu.
 *
 * ## Vì sao không tự gọi requestFullscreen trong useEffect
 * Gọi tự động lúc mount thì trình duyệt từ chối (không có cử chỉ người dùng) và ném lỗi ra console, còn
 * người học thì không hiểu vì sao màn hình không đổi. Phải để bên gọi gắn `vaoToanManHinh` vào một cái nút.
 *
 * @param enabled chỉ bật khi `attempt.strictExam` — server đã tính sẵn `quiz.strictExam && mode === EXAM`
 */
export function useStrictExam(enabled: boolean) {
  const [dangToanManHinh, setDangToanManHinh] = useState(() => Boolean(document.fullscreenElement))

  /** Gắn vào một nút — bắt buộc phải là cử chỉ người dùng, xem javadoc phía trên. */
  const vaoToanManHinh = useCallback(async () => {
    try {
      await document.documentElement.requestFullscreen()
      return true
    } catch {
      // Người dùng từ chối, hoặc trình duyệt/thiết bị không hỗ trợ (Safari trên iPhone không có
      // Fullscreen API cho phần tử thường). Không ném ra ngoài: bài thi vẫn phải làm được.
      return false
    }
  }, [])

  const thoatToanManHinh = useCallback(async () => {
    if (document.fullscreenElement) {
      try {
        await document.exitFullscreen()
      } catch {
        // Không làm gì — rời trang là thoát
      }
    }
  }, [])

  useEffect(() => {
    if (!enabled) {
      return
    }

    const onFullscreenChange = () => setDangToanManHinh(Boolean(document.fullscreenElement))

    // Khoá chuột phải. Ma sát, không phải rào chắn — nói rõ ở javadoc. Không chặn phím tắt: chặn cũng
    // không nổi (trình duyệt giữ riêng F12, Ctrl+Shift+I) mà lại phá cả những phím vô hại như Ctrl+F.
    const onContextMenu = (e: MouseEvent) => e.preventDefault()

    document.addEventListener('fullscreenchange', onFullscreenChange)
    document.addEventListener('contextmenu', onContextMenu)

    return () => {
      document.removeEventListener('fullscreenchange', onFullscreenChange)
      document.removeEventListener('contextmenu', onContextMenu)
      // Rời màn làm bài thì trả màn hình lại như cũ. Không làm thì người học nộp bài xong vẫn bị kẹt
      // toàn màn hình và phải tự bấm Esc — một cú bấm mà giờ này chẳng còn ý nghĩa gì.
      void thoatToanManHinh()
    }
  }, [enabled, thoatToanManHinh])

  return {
    /** Đang ở toàn màn hình hay không; luôn false khi `enabled` là false. */
    dangToanManHinh: enabled ? dangToanManHinh : false,
    vaoToanManHinh,
  }
}
