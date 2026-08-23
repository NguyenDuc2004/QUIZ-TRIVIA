import { useEffect, useRef, useState } from 'react'
import { integrityApi, type ProctoringEvent, type ProctoringEventType } from '../api/integrityApi'

/** Gom tín hiệu rồi gửi mỗi 10 giây. Gửi từng sự kiện là tự tạo tải ngay lúc người dùng đang thi. */
const CHU_KY_GUI_MS = 10_000

/** Tối đa mỗi lô — khớp giới hạn 50 của server. */
const TOI_DA_MOI_LO = 50

/**
 * Thu tín hiệu hành vi trong màn làm bài (features/12, FR-43).
 *
 * ## Chỉ chạy khi `enabled` là true
 * Bên gọi chỉ bật khi `attempt.mode === 'EXAM'` **và** bài đang làm. Luyện tập không bị theo dõi — đó là ràng
 * buộc của đặc tả, và server cũng từ chối lượt PRACTICE, nên đây là lớp thứ hai chứ không phải lớp duy nhất.
 *
 * ## Không bao giờ đọc nội dung
 * Với `paste`, hook chỉ lấy **độ dài** từ clipboard rồi bỏ chuỗi đi. Không có đường nào để nội dung người dùng
 * đi tới server. Server cũng tự dựng lại `detail` từ trường vô hại, nên kể cả hook này bị sửa sai thì nội
 * dung vẫn không vào cơ sở dữ liệu.
 *
 * ## Lỗi gửi không được làm ảnh hưởng bài thi
 * Mọi lời gọi API đều bọc trong `catch` và im lặng: người đang thi không nên thấy một thông báo lỗi vì cơ chế
 * giám sát gặp sự cố. Tín hiệu mất thì điểm rủi ro thấp hơn thực tế — chấp nhận được, vì hệ thống này chỉ
 * cảnh báo chứ không kết luận.
 *
 * ## Trả về số lần đã ghi nhận, để người thi nhìn thấy
 * Trước đây hook không trả gì: tín hiệu được thu hoàn toàn im lặng, và người thi bị tính điểm rủi ro mà
 * không biết mình đang bị tính cái gì. Bị đánh giá theo một tiêu chí không ai nói cho biết là bất công, kể
 * cả khi tiêu chí đó đúng.
 *
 * Đếm **theo nhóm người dùng hiểu được**, không theo loại sự kiện kỹ thuật: rời trang gộp cả chuyển tab lẫn
 * mất tiêu điểm cửa sổ, vì với người thi đó là **một** hành động chứ không phải hai.
 *
 * Con số này là *số lần đã ghi nhận*, **không phải** số lần vi phạm — xem `ProctoringLiveCount`.
 */
/** Số lần đã ghi nhận, gom theo nhóm mà người thi hiểu được. */
export interface SoLanGhiNhan {
  roiTrang: number
  dan: number
  thoatToanManHinh: number
}

const KHONG_CO: SoLanGhiNhan = { roiTrang: 0, dan: 0, thoatToanManHinh: 0 }

export function useProctoring(attemptId: string | undefined, enabled: boolean) {
  const buffer = useRef<ProctoringEvent[]>([])
  // Buffer giữ trong ref, không trong state: nó là mảng, mỗi lần thêm phần tử mà đổi state là dựng lại mảng
  // rồi render lại cả màn làm bài. Đây là dữ liệu nền, giao diện không hiện gì từ chính buffer.

  // Bộ đếm thì NGƯỢC LẠI — phải ở state vì người thi cần thấy nó đổi ngay. Đây là ba số nguyên, không phải
  // mảng, và mọi tín hiệu ở đây đều do người dùng chủ động gây ra (chuyển tab, dán, thoát toàn màn hình) nên
  // tần suất tính bằng lần mỗi phút, không phải lần mỗi khung hình.
  const [soLan, setSoLan] = useState<SoLanGhiNhan>(KHONG_CO)

  useEffect(() => {
    if (!enabled || !attemptId) {
      return
    }

    const them = (type: ProctoringEventType, extra?: Partial<ProctoringEvent>) => {
      if (buffer.current.length >= TOI_DA_MOI_LO * 4) {
        return   // chặn phình bộ nhớ nếu mạng chết lâu; server cũng có chặn trên riêng
      }
      buffer.current.push({ type, occurredAt: new Date().toISOString(), ...extra })

      // Gộp TAB_HIDDEN và WINDOW_BLUR thành một nhóm: với người thi, chuyển tab và bấm sang cửa sổ khác là
      // cùng một hành động "rời khỏi bài thi". Tách ra chỉ làm con số trông đáng sợ hơn thực tế.
      setSoLan((truoc) => {
        if (type === 'TAB_HIDDEN' || type === 'WINDOW_BLUR') {
          return { ...truoc, roiTrang: truoc.roiTrang + 1 }
        }
        if (type === 'PASTE') {
          return { ...truoc, dan: truoc.dan + 1 }
        }
        if (type === 'FULLSCREEN_EXIT') {
          return { ...truoc, thoatToanManHinh: truoc.thoatToanManHinh + 1 }
        }
        // COPY và ANSWER_TOO_FAST cố ý KHÔNG đếm ra màn hình. Sao chép đề bài là việc bình thường của người
        // học nghiêm túc, còn "trả lời nhanh bất thường" là một suy đoán của hệ thống chứ không phải một
        // hành động người thi tự biết mình vừa làm — hiện nó lên chỉ gây hoang mang mà không giúp họ sửa gì.
        return truoc
      })
    }

    const onVisibility = () => {
      // Chỉ ghi khi trang bị ẩn, không ghi lúc quay lại: quay lại không phải một tín hiệu, và ghi cả hai
      // chiều làm số lần chuyển tab bị đếm gấp đôi.
      if (document.visibilityState === 'hidden') {
        them('TAB_HIDDEN')
      }
    }
    const onBlur = () => them('WINDOW_BLUR')
    const onCopy = () => them('COPY')
    const onPaste = (e: ClipboardEvent) => {
      // Lấy độ dài rồi BỎ chuỗi. Không đưa vào biến nào tồn tại lâu hơn dòng này.
      const doDai = e.clipboardData?.getData('text')?.length ?? 0
      them('PASTE', { length: doDai })
    }
    const onFullscreenChange = () => {
      if (!document.fullscreenElement) {
        them('FULLSCREEN_EXIT')
      }
    }

    document.addEventListener('visibilitychange', onVisibility)
    window.addEventListener('blur', onBlur)
    document.addEventListener('copy', onCopy)
    document.addEventListener('paste', onPaste)
    document.addEventListener('fullscreenchange', onFullscreenChange)

    const guiLo = async () => {
      if (buffer.current.length === 0) {
        return
      }
      const lo = buffer.current.splice(0, TOI_DA_MOI_LO)
      try {
        await integrityApi.guiSuKien(attemptId, lo)
      } catch {
        // Im lặng có chủ đích — xem javadoc của hook. KHÔNG đưa lô trở lại buffer: nếu server đang lỗi thì
        // thử lại mãi chỉ làm buffer phình và lặp lại đúng lỗi đó.
      }
    }

    const timer = window.setInterval(guiLo, CHU_KY_GUI_MS)

    return () => {
      document.removeEventListener('visibilitychange', onVisibility)
      window.removeEventListener('blur', onBlur)
      document.removeEventListener('copy', onCopy)
      document.removeEventListener('paste', onPaste)
      document.removeEventListener('fullscreenchange', onFullscreenChange)
      window.clearInterval(timer)
      // Gửi phần còn lại khi rời màn hình (nộp bài hoặc đóng tab). Không await được trong cleanup, nhưng
      // request vẫn được gửi đi — mất một lô cuối cũng chỉ làm điểm rủi ro thấp hơn thực tế.
      void guiLo()
    }
  }, [attemptId, enabled])

  return soLan
}
