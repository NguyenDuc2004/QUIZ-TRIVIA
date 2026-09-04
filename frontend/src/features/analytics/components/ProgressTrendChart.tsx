import { Tooltip, Typography } from 'antd'
import type { AttemptScore } from '../api/analyticsApi'

const { Text } = Typography

/** Chiều cao vùng vẽ, dùng cho cả toạ độ SVG — đổi ở một chỗ. */
const HEIGHT = 160
const WIDTH = 800
const PADDING = 8

/**
 * Đường điểm theo thời gian — SVG viết tay.
 * <p>
 * Trục dọc **cố định 0–100%**, không tự co theo dữ liệu. Co theo dữ liệu thì một người dao động
 * 70–75% sẽ thấy đường răng cưa dựng đứng như đang lên xuống thất thường; cố định thang thì hình
 * dạng đường nói đúng mức độ thay đổi thật.
 */
export default function ProgressTrendChart({ trend }: { trend: AttemptScore[] }) {
  const points = trend.map((item, index) => ({
    ...item,
    x:
      trend.length === 1
        ? WIDTH / 2
        : PADDING + (index / (trend.length - 1)) * (WIDTH - PADDING * 2),
    y: PADDING + (1 - item.percent / 100) * (HEIGHT - PADDING * 2),
  }))

  return (
    <div className="soft-panel p-4">
      <svg
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        className="h-40 w-full"
        preserveAspectRatio="none"
        role="img"
        aria-label="Đường điểm theo thời gian"
      >
        {/* Ba mốc tham chiếu: 0, 50, 100% — không có chúng thì không đọc được độ cao của đường */}
        {[0, 50, 100].map((mark) => {
          const y = PADDING + (1 - mark / 100) * (HEIGHT - PADDING * 2)
          return (
            <line
              key={mark}
              x1={0}
              x2={WIDTH}
              y1={y}
              y2={y}
              stroke="var(--color-line)"
              strokeWidth={1}
              vectorEffect="non-scaling-stroke"
            />
          )
        })}

        {points.length > 1 && (
          <polyline
            fill="none"
            stroke="var(--color-ink)"
            strokeWidth={2}
            vectorEffect="non-scaling-stroke"
            points={points.map((point) => `${point.x},${point.y}`).join(' ')}
          />
        )}

        {points.map((point) => (
          <circle
            key={point.submittedAt + point.quizTitle}
            cx={point.x}
            cy={point.y}
            r={4}
            fill="var(--color-ink)"
          />
        ))}
      </svg>

      {/* Chi tiết từng lượt để dưới dạng chữ: SVG co giãn theo bề ngang nên chữ trong đó bị méo,
          và người đọc vẫn cần biết bài nào ứng với điểm nào */}
      <div className="mt-3 flex flex-wrap gap-2">
        {trend.map((item) => (
          <Tooltip
            key={item.submittedAt + item.quizTitle}
            title={`${item.quizTitle} — ${item.score}/${item.maxScore} điểm`}
          >
            <span className="border border-line rounded-card px-2 py-0.5">
              <Text className="text-xs">
                {new Date(item.submittedAt).toLocaleDateString('vi-VN')} · {item.percent}%
              </Text>
            </span>
          </Tooltip>
        ))}
      </div>
    </div>
  )
}
