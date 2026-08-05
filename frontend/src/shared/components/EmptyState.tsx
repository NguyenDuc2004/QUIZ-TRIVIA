import type { ReactNode } from 'react'
import { Typography } from 'antd'

const { Text } = Typography

/**
 * Trạng thái rỗng chuẩn: một câu giải thích + (tuỳ chọn) nút gợi ý hành động tiếp theo.
 * Dùng thay cho <Empty> mặc định của antd để giữ đúng kiểu chữ/khoảng trắng của hệ thống.
 */
export default function EmptyState({
  title,
  hint,
  action,
}: {
  title: string
  hint?: string
  action?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center gap-2 px-4 py-12 text-center">
      <Text className="font-bold!">{title}</Text>
      {hint && <Text className="text-ink-soft text-xs">{hint}</Text>}
      {action && <div className="mt-2">{action}</div>}
    </div>
  )
}
