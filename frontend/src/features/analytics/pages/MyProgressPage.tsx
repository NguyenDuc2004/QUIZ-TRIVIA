import { Link } from 'react-router-dom'
import { Button, Card, Skeleton, Space, Statistic, Typography } from 'antd'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import ProgressTrendChart from '../components/ProgressTrendChart'
import { useMyProgress } from '../hooks/useAnalyticsQueries'

const { Text } = Typography

/**
 * Tiến độ học tập của tôi (FR-26).
 * <p>
 * **Trang này không nói về điểm mạnh/yếu theo chủ đề** dù nghe rất hợp chỗ. Phần đó nằm ở trang Lộ
 * trình học, tính từ đồ thị Neo4j. Tính lại cùng một kết luận từ PostgreSQL sẽ cho hai màn hình nói
 * về cùng một chuyện bằng hai cách trên hai kho dữ liệu — và đến một ngày chúng lệch nhau thì không
 * ai biết tin màn nào. Ở đây chỉ dẫn sang đó.
 */
export default function MyProgressPage() {
  const { data, isPending } = useMyProgress()

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 6 }} />
  }

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Tiến độ của bạn"
        description="Số bài đã làm, điểm trung bình và mức tiến bộ qua từng lượt"
        actions={
          <Link to="/learning-path">
            <Button>Xem lộ trình học</Button>
          </Link>
        }
      />

      {!data || data.totalAttempts === 0 ? (
        <EmptyState
          title="Chưa có gì để thống kê"
          hint="Làm xong một bài quiz là trang này có số liệu ngay"
          action={
            <Link to="/quizzes">
              <Button type="primary">Tìm quiz để làm</Button>
            </Link>
          }
        />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            <Card>
              <Statistic title="Lượt làm bài" value={data.totalAttempts} />
            </Card>
            <Card>
              <Statistic title="Quiz đã học" value={data.distinctQuizzes} />
              {/* Nói rõ vì sao hai con số lệch nhau, thay vì để người đọc tưởng có lỗi */}
              {data.totalAttempts > data.distinctQuizzes && (
                <Text className="text-ink-soft text-xs">
                  Bạn đã làm lại một số quiz nên số lượt nhiều hơn số quiz
                </Text>
              )}
            </Card>
            <Card>
              <Statistic
                title="Điểm trung bình"
                value={data.averagePercent ?? 0}
                precision={1}
                suffix="%"
              />
            </Card>
          </div>

          <div>
            <Text className="mb-2 block font-bold">Mức tiến bộ qua từng lượt</Text>
            <ProgressTrendChart trend={data.trend} />
          </div>

          <Text className="text-ink-soft text-xs">
            Muốn biết mình đang yếu chủ đề nào? Xem <Link to="/learning-path">lộ trình học</Link> —
            phần đó tính riêng từ tỷ lệ trả lời đúng theo từng chủ đề.
          </Text>
        </>
      )}
    </Space>
  )
}
