import { Typography } from 'antd'
import type { ScoreBucket } from '../api/analyticsApi'

const { Text } = Typography

/**
 * Phân bố điểm — biểu đồ cột dựng bằng CSS.
 * <p>
 * Không kéo thư viện biểu đồ về chỉ để vẽ mười cái cột: thư viện nhẹ nhất cũng nặng hơn cả tính
 * năng này, và nó mang theo bảng màu riêng đi ngược hệ thống giao diện (docs/ui-design-system.md).
 * <p>
 * Chiều cao cột chia theo **cột cao nhất**, không chia theo tổng số lượt: chia theo tổng thì khi
 * điểm phân tán đều, mọi cột đều lùn tịt và biểu đồ không cho biết điều gì.
 */
export default function ScoreDistributionChart({ buckets }: { buckets: ScoreBucket[] }) {
  const peak = Math.max(...buckets.map((bucket) => bucket.attemptCount), 1)

  return (
    <div className="soft-panel p-4">
      <div className="flex h-40 items-end gap-1">
        {buckets.map((bucket) => (
          <div key={bucket.label} className="flex flex-1 flex-col items-center justify-end gap-1">
            {/* Số lượt hiện phía trên cột; cột rỗng không hiện số 0 cho đỡ nhiễu */}
            <Text className="text-ink-soft text-xs">
              {bucket.attemptCount > 0 ? bucket.attemptCount : ''}
            </Text>
            <div
              className="w-full rounded-t-xs bg-ink"
              // Cột có lượt luôn cao tối thiểu 4px — cột 1 lượt cạnh cột 50 lượt mà tính đúng tỉ lệ
              // thì biến mất, và người đọc tưởng khoảng đó không có ai
              style={{
                height:
                  bucket.attemptCount === 0
                    ? 2
                    : `max(4px, ${(bucket.attemptCount / peak) * 100}%)`,
                opacity: bucket.attemptCount === 0 ? 0.15 : 1,
              }}
            />
          </div>
        ))}
      </div>

      <div className="mt-2 flex gap-1">
        {buckets.map((bucket) => (
          <Text key={bucket.label} className="text-ink-soft flex-1 text-center text-[10px]">
            {bucket.fromPercent}
          </Text>
        ))}
      </div>
      <Text className="text-ink-soft mt-1 block text-center text-xs">Điểm đạt được (%)</Text>
    </div>
  )
}
