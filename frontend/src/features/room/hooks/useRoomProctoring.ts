import { useEffect, useRef } from 'react'

/**
 * Thu tín hiệu rời trang / quay lại trong phòng đấu (features/12, cảnh báo live).
 *
 * ## Khác `useProctoring` của bài thi ở hai điểm, và cả hai đều có lý do
 *
 * **1. Gửi ngay, không gom lô.** Bài thi gom 10 giây một lần vì tín hiệu ở đó chỉ dùng để tính điểm rủi ro
 * *sau khi nộp* — trễ 10 giây không ai thấy. Ở phòng đấu, cờ phải tới host **trong lúc câu hỏi còn sống**;
 * gom lô thì cờ đến sau khi ván đã sang câu khác và host chẳng còn gì để làm với nó. Tín hiệu cũng thưa
 * (một lần chuyển tab, không phải mỗi lần bấm phím) nên gửi ngay không tạo tải đáng kể.
 *
 * **2. Gửi CẢ `TAB_VISIBLE`.** Bài thi cố ý không ghi lúc quay lại — ở đó "quay lại" không phải tín hiệu và
 * ghi cả hai chiều làm số lần chuyển tab bị đếm gấp đôi. Ở phòng đấu thì *quay lại kịp giờ* mới chính là
 * dấu hiệu: đó là thứ phân biệt người tra cứu rồi về trả lời với người bị gián đoạn và mất câu đó.
 *
 * ## Không đọc nội dung, không có `copy`/`paste`
 * Câu hỏi phòng đấu hiện trên màn hình vài chục giây và đáp án là các nút bấm — thứ cần thấy là tra cứu ở
 * tab khác, không phải sao chép đề. Nên hook này chỉ nghe `visibilitychange`.
 *
 * @param send  hàm gửi của `useRoomSocket`; hook không tự mở kết nối riêng
 * @param dangChoi bật chỉ khi ván đang diễn ra. Ở phòng chờ, người vào sớm rồi đi làm việc khác là chuyện
 *                 bình thường — server cũng bỏ tín hiệu có `questionIndex = -1`, nên đây là lớp thứ hai
 */
export function useRoomProctoring(
  send: (action: 'proctoring', body: { type: 'TAB_HIDDEN' | 'TAB_VISIBLE' }) => void,
  dangChoi: boolean,
) {
  // Giữ `send` trong ref: nó được `useCallback` hoá theo `roomCode` nhưng nếu bên gọi đổi cách dựng thì
  // effect này không nên tháo và gắn lại listener giữa ván.
  const guiRef = useRef(send)
  guiRef.current = send

  useEffect(() => {
    if (!dangChoi) {
      return
    }
    const onVisibility = () => {
      guiRef.current('proctoring', {
        type: document.visibilityState === 'hidden' ? 'TAB_HIDDEN' : 'TAB_VISIBLE',
      })
    }
    document.addEventListener('visibilitychange', onVisibility)
    return () => document.removeEventListener('visibilitychange', onVisibility)
  }, [dangChoi])
}
