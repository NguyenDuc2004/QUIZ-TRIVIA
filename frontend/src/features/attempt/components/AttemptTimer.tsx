import { useEffect, useRef, useState } from 'react'
import { Typography } from 'antd'
import { formatDuration } from '../constants'

const { Text } = Typography

/** Số giây còn lại tính từ bây giờ tới hạn nộp, không cho âm. */
function secondsLeft(expiresAt: string) {
  return Math.max(0, Math.round((new Date(expiresAt).getTime() - Date.now()) / 1000))
}

/**
 * Đồng hồ đếm ngược tới hạn nộp bài (FR-16).
 * <p>
 * Chỉ là lớp hiển thị và một lần gọi {@code onExpire}: hạn nộp thật do backend giữ
 * (`quiz_attempts.expires_at`), nên chỉnh giờ máy hay sửa JS cũng không kéo dài được thời gian.
 */
export default function AttemptTimer({
  expiresAt,
  onExpire,
}: {
  expiresAt: string
  onExpire: () => void
}) {
  const [remaining, setRemaining] = useState(() => secondsLeft(expiresAt))
  // Giữ callback mới nhất để interval không phải tạo lại mỗi lần cha render
  const onExpireRef = useRef(onExpire)
  onExpireRef.current = onExpire
  const firedRef = useRef(false)

  useEffect(() => {
    const tick = () => {
      const left = secondsLeft(expiresAt)
      setRemaining(left)
      if (left === 0 && !firedRef.current) {
        firedRef.current = true
        onExpireRef.current()
      }
    }

    tick()
    const id = window.setInterval(tick, 1000)
    return () => window.clearInterval(id)
  }, [expiresAt])

  const urgent = remaining <= 60

  return (
    <div className="flex flex-col items-center border border-line bg-surface px-4 py-3">
      <Text className="text-ink-soft text-xs">Thời gian còn lại</Text>
      <span
        className={`font-mono text-2xl font-extrabold tabular-nums ${
          urgent ? 'text-urgent' : 'text-ink'
        }`}
      >
        {formatDuration(remaining)}
      </span>
    </div>
  )
}
