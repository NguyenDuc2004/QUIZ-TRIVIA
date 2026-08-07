import { Link } from 'react-router-dom'
import { Alert, Button, Progress, Skeleton, Space, Tag, Typography } from 'antd'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import { useLearningPath, useRebuildGraph } from '../hooks/useRecommendQueries'
import RecommendedQuizzes from '../components/RecommendedQuizzes'

const { Text, Paragraph } = Typography

/**
 * Lộ trình học cá nhân hoá (FR-35): các chủ đề đã học xếp theo mức độ yếu, yếu nhất trước.
 * <p>
 * **Trang này nói thật về căn cứ của nó.** Thứ tự không đến từ một mô hình sư phạm nào — hệ thống
 * không có dữ liệu chủ đề nào phải học trước chủ đề nào. Nó xếp theo tỷ lệ trả lời đúng thật của
 * chính người học. Ghi rõ điều đó ngay trên trang, vì một lộ trình trông có vẻ thông minh mà không
 * giải thích được sẽ khiến người học tin nhầm vào nó.
 */
export default function LearningPathPage() {
  const { data, isPending } = useLearningPath()
  const rebuild = useRebuildGraph()

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Lộ trình học của bạn"
        description="Chủ đề đang yếu nhất xếp lên trước, dựa trên tỷ lệ trả lời đúng của chính bạn"
        actions={
          <Button loading={rebuild.isPending} onClick={() => rebuild.mutate()}>
            Phân tích lại
          </Button>
        }
      />

      {isPending ? (
        <Skeleton active paragraph={{ rows: 4 }} />
      ) : (
        <>
          {data?.note && (
            <Alert
              type="info"
              showIcon
              message={data.note}
              description={
                data.topics.length === 0 ? (
                  <>
                    Đã làm bài rồi mà vẫn thấy dòng này? Bấm <b>Phân tích lại</b> — những bài làm
                    trước khi tính năng này có mặt chưa được đưa vào phân tích.
                  </>
                ) : undefined
              }
            />
          )}

          {data && data.topics.length > 0 && (
            <div className="border border-line bg-white">
              {data.topics.map((item, index) => (
                <div
                  key={item.topic}
                  className="flex flex-wrap items-center gap-4 border-b border-line p-4 last:border-b-0"
                >
                  <Text className="w-6 text-ink-soft text-sm font-bold">{index + 1}</Text>

                  <div className="min-w-40 flex-1">
                    <div className="mb-1 flex items-center gap-2">
                      <Text className="font-bold">{item.topic}</Text>
                      {item.weak && (
                        <Tag color="orange" className="mr-0!">
                          Cần ôn
                        </Tag>
                      )}
                    </div>
                    <Text className="text-ink-soft text-xs">
                      Đúng {item.correct}/{item.total} câu
                      {/* Nói thẳng khi chưa đủ căn cứ, thay vì im lặng để người đọc tự suy ra
                          rằng "không có nhãn Cần ôn" nghĩa là "đang ổn" */}
                      {item.total < 3 && ' · chưa đủ dữ liệu để đánh giá'}
                    </Text>
                  </div>

                  {/* Hiện CON SỐ, không dùng status success/exception: Ant Design vẽ dấu tích cho
                      `success` bất kể tỷ lệ là bao nhiêu, nên chủ đề 0/2 câu (chưa đủ dữ liệu để
                      kết luận nên weak=false) lại được gắn tích xanh như thể đang làm tốt. */}
                  <Progress
                    percent={Math.round(item.accuracy * 100)}
                    size="small"
                    strokeColor={item.weak ? '#ff4d4f' : '#52c41a'}
                    className="w-40!"
                  />

                  {/* Khuyên "học tiếp đi" mà không còn quiz nào để làm thì là lời khuyên suông */}
                  {item.availableQuizzes > 0 ? (
                    <Link to={`/quizzes?q=${encodeURIComponent(item.topic)}`}>
                      <Button size="small">Còn {item.availableQuizzes} quiz chưa làm</Button>
                    </Link>
                  ) : (
                    <Text className="text-ink-soft text-xs">Đã làm hết quiz chủ đề này</Text>
                  )}
                </div>
              ))}
            </div>
          )}

          {data && data.topics.length === 0 && !data.note && (
            <EmptyState
              title="Chưa có dữ liệu lộ trình"
              hint="Làm vài quiz có gắn chủ đề để hệ thống hiểu bạn mạnh yếu ở đâu."
            />
          )}

          <RecommendedQuizzes />

          <Paragraph className="text-ink-soft text-xs">
            Thứ tự trên dựa hoàn toàn vào <b>tỷ lệ trả lời đúng của bạn</b>, không dựa vào quan hệ
            tiên quyết giữa các chủ đề — hệ thống chưa có dữ liệu đó nên không suy đoán. Một chủ đề
            chỉ bị đánh giá khi bạn đã trả lời ít nhất 3 câu thuộc chủ đề đó.
          </Paragraph>
        </>
      )}
    </Space>
  )
}
