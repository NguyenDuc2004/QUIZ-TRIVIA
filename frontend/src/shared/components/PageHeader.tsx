import type { ReactNode } from 'react'
import { Typography } from 'antd'

const { Title, Paragraph } = Typography

/**
 * Tiêu đề chuẩn cho đầu mỗi trang (docs/ui-design-system.md §6).
 * Mọi trang mới phải dùng component này thay vì tự dựng tiêu đề.
 */
export default function PageHeader({
  title,
  description,
  actions,
}: {
  title: string
  description?: ReactNode
  actions?: ReactNode
}) {
  return (
    <div className="flex flex-wrap items-start justify-between gap-4">
      <div className="min-w-0">
        <Title level={2} className="mb-1! font-bold!">
          {title}
        </Title>
        {description && (
          <Paragraph className="mb-0! text-ink-soft">{description}</Paragraph>
        )}
      </div>
      {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
    </div>
  )
}
