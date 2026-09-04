import { Alert, Card, Col, Progress, Row, Skeleton, Statistic, Tag, Tooltip, Typography } from 'antd'
import { CheckCircleOutlined, FireOutlined } from '@ant-design/icons'
import PageHeader from '@/shared/components/PageHeader'
import { useBadges, useDailyChallenge, useGamificationOverview } from '../hooks/useGamification'
import type { BadgeItem } from '../api/gamificationApi'

const { Text, Paragraph } = Typography

/**
 * Thành tích: XP, cấp độ, chuỗi ngày, huy hiệu, thử thách ngày (features/13).
 *
 * Trang này **chỉ hiện dữ liệu thật**. Không có con số nào được làm đẹp: XP đến từ hành động học đã ghi ở
 * `xp_events`, huy hiệu chỉ hiện "đã mở khoá" khi có dòng trong `user_badges`. `ui-design-system.md §7`.
 */
export default function AchievementsPage() {
  const { data: overview, isLoading } = useGamificationOverview()
  const { data: badges } = useBadges()
  const { data: daily } = useDailyChallenge()

  if (isLoading || !overview) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }

  const daMoKhoa = (badges ?? []).filter((b) => b.earnedAt != null)
  const chuaMoKhoa = (badges ?? []).filter((b) => b.earnedAt == null)
  const oCapToiDa = overview.xpCanTrongCap === 0

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Thành tích"
        description="XP đến từ việc làm bài và ôn thẻ. Mỗi hành động chỉ được tính một lần."
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} md={12}>
          {/* Thẻ cấp độ dùng nền tím gradient — đây là một trong ba chỗ của cả hệ thống được phép rực
              (docs/ui-design-system.md §1: khu học tập giữ trung tính, phần trò chơi hoá thì không).
              Cấp độ là *phần thưởng*, mà một phần thưởng trình bày y hệt một ô thống kê quản trị thì
              không còn là phần thưởng. */}
          <Card className="achievement-hero border-0!">
            <div className="flex items-baseline justify-between">
              <Statistic
                title={<span className="text-white/80">Cấp độ</span>}
                value={overview.level}
                valueStyle={{ color: '#fff', fontWeight: 700 }}
              />
              <Text className="text-sm text-white/85">{overview.totalXp} XP</Text>
            </div>
            {/* Ở cấp tối đa thì không còn cấp kế tiếp — hiện 100% kèm chữ, không chia cho 0 */}
            <Progress
              percent={
                oCapToiDa
                  ? 100
                  : Math.round((overview.xpTrongCap / overview.xpCanTrongCap) * 100)
              }
              showInfo={false}
              strokeColor="#fde047"
              trailColor="rgba(255,255,255,.25)"
            />
            <Text className="text-xs text-white/85">
              {oCapToiDa
                ? 'Đã ở cấp cao nhất'
                : `còn ${overview.xpCanTrongCap - overview.xpTrongCap} XP để lên cấp ${overview.level + 1}`}
            </Text>
          </Card>
        </Col>

        <Col xs={24} md={12}>
          <Card>
            <Statistic
              title="Chuỗi ngày học"
              value={overview.currentStreak}
              suffix="ngày"
              prefix={
                /* Ngọn lửa có nhịp đập khi chuỗi đang được giữ, xám khi đã nguội. Trạng thái này vốn
                   chỉ nói bằng chữ ở dòng dưới; cho nó một tín hiệu nhìn thấy ngay là đúng chỗ để
                   sinh động, vì giữ chuỗi chính là việc tính năng này muốn người học làm. */
                <FireOutlined
                  className={overview.streakConHomNay ? 'streak-alight text-star' : 'text-ink-soft'}
                />
              }
            />
            {/* Phân biệt hai trạng thái mà con số chuỗi không nói được */}
            <Text className="text-ink-soft text-xs">
              {overview.streakConHomNay
                ? 'Hôm nay đã học — chuỗi đang được giữ'
                : 'Hôm nay chưa học. Làm một bài hoặc ôn một thẻ để giữ chuỗi.'}
            </Text>
            {overview.longestStreak > overview.currentStreak && (
              <div className="text-ink-soft text-xs">
                Dài nhất từng đạt: {overview.longestStreak} ngày
              </div>
            )}
          </Card>
        </Col>
      </Row>

      {daily && (
        <Card
          title="Thử thách hôm nay"
          extra={<Tag color="purple">+{daily.xpReward} XP</Tag>}
        >
          <Paragraph className="mb-2!">{daily.description}</Paragraph>
          {daily.completedAt ? (
            <Alert
              type="success"
              showIcon
              icon={<CheckCircleOutlined />}
              message="Đã hoàn thành hôm nay"
              description={`Bạn đã nhận ${daily.xpReward} XP thưởng.`}
            />
          ) : (
            <>
              <Progress
                percent={Math.round((daily.progress / daily.target) * 100)}
                format={() => `${daily.progress}/${daily.target}`}
              />
              <Text className="text-ink-soft text-xs">
                Còn {daily.target - daily.progress} bước nữa.
              </Text>
            </>
          )}
        </Card>
      )}

      <Card title={`Huy hiệu — đã mở ${overview.soHuyHieu}/${overview.tongSoHuyHieu}`}>
        <div className="flex flex-col gap-4">
          {daMoKhoa.length > 0 && (
            <div>
              <Text className="mb-2 block text-sm font-bold">Đã mở khoá</Text>
              <div className="flex flex-wrap gap-2">
                {daMoKhoa.map((b) => (
                  <HuyHieu key={b.id} badge={b} />
                ))}
              </div>
            </div>
          )}
          <div>
            <Text className="mb-2 block text-sm font-bold">Chưa mở khoá</Text>
            {/* Hiện cả huy hiệu chưa đạt: danh sách chỉ có cái đã đạt thì không tạo được động lực nào */}
            <div className="flex flex-wrap gap-2">
              {chuaMoKhoa.map((b) => (
                <HuyHieu key={b.id} badge={b} />
              ))}
            </div>
          </div>
        </div>
      </Card>
    </div>
  )
}

/**
 * Một huy hiệu. Chưa mở khoá thì làm mờ và bỏ màu, nhưng **vẫn đọc được mô tả** — đó là mục tiêu.
 *
 * Huy hiệu đã mở có nền vàng nhạt và viền vàng thay cho nền xám: xám là màu của *chưa có gì*, mà đây
 * đúng là thứ người học vừa giành được. Hai nhóm đặt cạnh nhau nên khác biệt phải nhìn ra ngay ở khoảng
 * cách một mét, không phải sau khi đọc chữ.
 */
function HuyHieu({ badge }: { badge: BadgeItem }) {
  const daMo = badge.earnedAt != null
  return (
    <Tooltip
      title={
        daMo
          ? `${badge.description} · mở khoá ${new Date(badge.earnedAt!).toLocaleDateString('vi-VN')}`
          : badge.description
      }
    >
      <div
        className={`flex w-40 flex-col items-center gap-1 rounded-card border px-3 py-3 text-center transition-transform duration-150 ${
          // 60% chứ không 50%: huy hiệu chưa mở CỐ Ý mờ đi để phân biệt với huy hiệu đã mở, nhưng
          // ở chế độ tối, 50% kéo chữ xuống ~4,1:1 — dưới ngưỡng đọc được. Vẫn mờ rõ ràng ở 60%.
          daMo ? 'badge-earned hover:-translate-y-0.5' : 'border-line opacity-60'
        }`}
      >
        <span className={`text-3xl ${daMo ? '' : 'grayscale'}`} aria-hidden>
          {badge.icon ?? '🏅'}
        </span>
        <Text className={`text-xs ${daMo ? 'font-bold!' : ''}`}>{badge.name}</Text>
      </div>
    </Tooltip>
  )
}
