import { useEffect, useState } from 'react'
import { Progress } from 'antd'

/**
 * Đếm ngược tới hạn trả lời của câu hiện tại.
 * <p>
 * Mốc hết giờ là <b>đồng hồ server</b> gửi xuống trong sự kiện QUESTION. Đây chỉ là phần hiển thị:
 * server vẫn tự kiểm hạn khi nhận đáp án, nên chỉnh giờ máy hay sửa JS cũng không kéo dài được.
 */
export default function RoomCountdown({
  deadlineAtMillis,
  totalSeconds,
}: {
  deadlineAtMillis: number
  totalSeconds: number
}) {
  const [remaining, setRemaining] = useState(() =>
    Math.max(0, Math.ceil((deadlineAtMillis - Date.now()) / 1000)),
  )

  useEffect(() => {
    const tick = () =>
      setRemaining(Math.max(0, Math.ceil((deadlineAtMillis - Date.now()) / 1000)))

    tick()
    const id = window.setInterval(tick, 250)
    return () => window.clearInterval(id)
  }, [deadlineAtMillis])

  const percent = totalSeconds > 0 ? Math.round((remaining / totalSeconds) * 100) : 0

  return (
    <div className="flex items-center gap-3">
      <span
        className={`font-mono text-3xl font-extrabold tabular-nums ${
          remaining <= 5 ? 'text-urgent' : 'text-ink'
        }`}
      >
        {remaining}
      </span>
      <Progress
        percent={percent}
        showInfo={false}
        strokeColor={remaining <= 5 ? '#dc2626' : '#1c1d1f'}
        className="flex-1"
      />
    </div>
  )
}
