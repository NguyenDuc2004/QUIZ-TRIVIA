import { Link } from 'react-router-dom'
import { Button, Skeleton, Tag, Typography } from 'antd'
import { useRecommendedQuizzes } from '../hooks/useRecommendQueries'

const { Title, Text } = Typography

/**
 * Khu "Gợi ý cho bạn" trên trang Khám phá (FR-34).
 * <p>
 * **Không có gợi ý thì ẩn hẳn khu này**, không hiện một ô trống hay lời mời chào — người mới chưa
 * làm bài nào mà thấy "Gợi ý cho bạn: (trống)" thì chỉ thấy hệ thống hỏng. Chỗ giải thích vì sao
 * chưa có gợi ý nằm ở trang Lộ trình học, nơi người dùng chủ động đi tìm.
 */
export default function RecommendedQuizzes() {
  const { data, isPending } = useRecommendedQuizzes(4)

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 2 }} />
  }
  if (!data || data.length === 0) {
    return null
  }

  return (
    <section className="border border-line bg-white p-5">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Title level={4} className="mb-0!">
          Gợi ý cho bạn
        </Title>
        <Text className="text-ink-soft text-xs">Dựa trên những gì bạn đã làm</Text>
        <Link to="/learning-path" className="ml-auto text-sm font-bold underline">
          Xem lộ trình học
        </Link>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {data.map((item) => (
          <div key={item.quizId} className="flex flex-col border border-line p-4">
            <Tag
              color={item.source === 'WEAK_TOPIC' ? 'orange' : 'blue'}
              className="mr-0! mb-2 self-start"
            >
              {item.source === 'WEAK_TOPIC' ? 'Ôn chỗ đang yếu' : 'Người giống bạn đã làm'}
            </Tag>

            <Text className="mb-1 font-bold">{item.title}</Text>

            {/* Lý do do backend viết — gợi ý không nói vì sao thì người dùng không có căn cứ để tin */}
            <Text className="mb-3 text-ink-soft text-xs">{item.reason}</Text>

            <Link to={`/quizzes/${item.quizId}`} className="mt-auto">
              <Button size="small" block>
                Làm thử
              </Button>
            </Link>
          </div>
        ))}
      </div>
    </section>
  )
}
