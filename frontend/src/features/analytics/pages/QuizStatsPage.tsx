import { Link, useParams } from 'react-router-dom'
import { Alert, Button, Card, Skeleton, Space, Statistic, Table, Tag, Tooltip, Typography } from 'antd'
import { EditOutlined, EyeOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import { formatPercent } from '../format'
import ScoreDistributionChart from '../components/ScoreDistributionChart'
import type { HardQuestion, QuizAttemptSummary } from '../api/analyticsApi'
import { useQuizAttempts, useQuizStats } from '../hooks/useAnalyticsQueries'

const { Text } = Typography

/**
 * Thống kê một quiz, dành cho chủ quiz (FR-86).
 * <p>
 * Trang này là **bộ mặt bảng điều khiển** (docs/ui-design-system.md §1): số liệu dày, bảng thay cho
 * lưới card. Nó cũng là đường vào việc chấm tay câu tự luận — thứ mà API đã có từ features/06 nhưng
 * không có màn hình nào dẫn tới.
 */
export default function QuizStatsPage() {
  const { id } = useParams<{ id: string }>()
  const { data: stats, isPending, isError } = useQuizStats(id)
  const { data: attempts } = useQuizAttempts(id)

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }

  // Backend trả 404 cho quiz không thuộc mình — cố tình, để không tiết lộ quiz đó tồn tại.
  // Nói đúng như vậy chứ không hứa "thử lại sau", vì thử lại cũng thế.
  if (isError || !stats) {
    return (
      <EmptyState
        title="Không tìm thấy quiz này"
        hint="Quiz không tồn tại, hoặc không phải quiz của bạn"
      />
    )
  }

  const needGrading = (attempts ?? []).filter((row) => row.needsManualGrading)
  // Chỉ đếm bài CHƯA ai xem: bài đã kết luận không còn là việc phải làm, và để lẫn vào thì con số ở dòng cảnh
  // báo không bao giờ giảm dù chủ quiz đã rà soát hết.
  const canRaSoat = (attempts ?? []).filter((row) => row.reviewStatus === 'PENDING')

  const hardColumns: ColumnsType<HardQuestion> = [
    {
      title: 'Câu hỏi',
      dataIndex: 'content',
      render: (content: string, row) => (
        <div>
          <Text>{content}</Text>
          {row.topic && (
            <Tag className="ml-2!" color="default">
              {row.topic}
            </Tag>
          )}
        </div>
      ),
    },
    { title: 'Lượt trả lời', dataIndex: 'answeredCount', width: 120, align: 'center' },
    {
      title: 'Tỉ lệ sai',
      dataIndex: 'wrongPercent',
      width: 120,
      align: 'center',
      render: (percent: number, row) => (
        <Tooltip title={`Sai ${row.wrongCount}/${row.answeredCount} lượt`}>
          <Text className="font-bold">{percent}%</Text>
        </Tooltip>
      ),
    },
  ]

  const attemptColumns: ColumnsType<QuizAttemptSummary> = [
    { title: 'Người học', dataIndex: 'learnerName' },
    {
      title: 'Điểm',
      key: 'score',
      width: 140,
      render: (_, row) => (
        <>
          <Text className="font-bold">
            {row.score}/{row.maxScore}
          </Text>
          {/* Điểm của bài còn câu chưa chấm là điểm TẠM. Không nói ra thì chủ quiz đọc nó như
              điểm cuối cùng và kết luận sai về người học. */}
          {row.needsManualGrading && (
            <Text className="text-ink-soft block text-xs">chưa phải điểm cuối</Text>
          )}
        </>
      ),
    },
    {
      title: 'Nộp lúc',
      dataIndex: 'submittedAt',
      width: 170,
      render: (value: string | null) =>
        value ? new Date(value).toLocaleString('vi-VN') : <Text className="text-ink-soft">—</Text>,
    },
    {
      // Cột này chỉ có nội dung ở bài vượt ngưỡng. Máy chủ không gửi điểm của bài dưới ngưỡng, nên đây không
      // phải "ẩn cho gọn" mà là không có gì để hiện — xem javadoc của `riskScore` trong analyticsApi.
      title: 'Rủi ro',
      key: 'risk',
      width: 130,
      render: (_, row) => {
        if (row.riskScore === null) {
          return <Text className="text-ink-soft">—</Text>
        }
        return (
          <Tooltip title="Điểm rủi ro không phải bằng chứng gian lận. Mở bài để đọc lý do cụ thể.">
            <div className="flex flex-col items-start gap-0.5">
              {/* Cam chứ không đỏ: đỏ đọc thành "đã kết luận có tội", còn trạng thái thật là "đáng xem" */}
              <Tag color="orange" className="mr-0!">
                {row.riskScore}/100
              </Tag>
              {row.reviewStatus !== 'PENDING' && (
                <Text className="text-ink-soft text-xs">
                  {row.reviewStatus === 'VALID' ? 'đã xác nhận hợp lệ' : 'đã đánh dấu không hợp lệ'}
                </Text>
              )}
            </div>
          </Tooltip>
        )
      },
    },
    {
      title: 'Trạng thái chấm',
      key: 'grading',
      width: 200,
      render: (_, row) => {
        if (row.pendingAiCount > 0) {
          return <Tag color="processing">AI đang chấm {row.pendingAiCount} câu</Tag>
        }
        if (row.failedAiCount > 0) {
          return <Tag color="warning">AI không chấm được {row.failedAiCount} câu</Tag>
        }
        return <Tag color="success">Đã chấm xong</Tag>
      },
    },
    {
      title: '',
      key: 'actions',
      width: 120,
      render: (_, row) => (
        <Link to={`/my-quizzes/${id}/attempts/${row.attemptId}`}>
          {/* Nút viền thay cho chữ trần: một hàng bảng có nhiều chữ, và hành động phải nhận ra được
              là hành động ngay từ hình dáng chứ không chỉ từ màu. */}
          <Button size="small" icon={row.needsManualGrading ? <EditOutlined /> : <EyeOutlined />}>
            {row.needsManualGrading ? 'Chấm tay' : 'Xem bài'}
          </Button>
        </Link>
      ),
    },
  ]

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Thống kê quiz"
        description={
          <>
            Số liệu tổng hợp trên bài đã nộp.{' '}
            <Link to={`/my-quizzes/${id}`}>Về trang soạn quiz</Link>
          </>
        }
      />

      {needGrading.length > 0 && (
        <Alert
          type="warning"
          showIcon
          message={`${needGrading.length} bài đang chờ bạn chấm câu tự luận`}
          description="AI không chấm được những câu này (thường vì hết hạn mức). Điểm hiện tại của các bài đó chưa phải điểm cuối."
        />
      )}

      {/* Không có dòng này thì FR-47 chỉ đúng một nửa: chủ quiz CÓ quyền xem báo cáo tính toàn vẹn, nhưng
          đường vào duy nhất là mở từng bài một — với 200 bài nộp thì trên thực tế họ không bao giờ biết */}
      {canRaSoat.length > 0 && (
        <Alert
          type="warning"
          showIcon
          message={`${canRaSoat.length} bài có nhiều tín hiệu hành vi đáng xem`}
          description="Mở bài để đọc lý do cụ thể rồi tự kết luận. Tín hiệu thu từ trình duyệt có thể bị chặn hoặc giả mạo và mỗi tín hiệu đều có cách giải thích vô hại, nên đây không phải bằng chứng gian lận — hệ thống không tự xử lý bài nào."
        />
      )}

      {stats.totalAttempts === 0 ? (
        <EmptyState
          title="Chưa có ai làm quiz này"
          hint="Số liệu xuất hiện ngay khi có bài nộp đầu tiên"
        />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-4">
            <Card>
              <Statistic title="Lượt làm" value={stats.totalAttempts} />
            </Card>
            <Card>
              <Statistic title="Số người học" value={stats.distinctLearners} />
            </Card>
            <Card>
              <Statistic title="Điểm trung bình" value={formatPercent(stats.averagePercent)} />
            </Card>
            <Card>
              <Statistic title="Nộp kịp giờ" value={formatPercent(stats.completionPercent)} />
              {/* Con số này chỉ có nghĩa khi quiz có giới hạn thời gian — nói rõ nó đo cái gì */}
              <Text className="text-ink-soft text-xs">phần còn lại là bài bị hết giờ</Text>
            </Card>
          </div>

          <div>
            <Text className="mb-2 block font-bold">Phân bố điểm</Text>
            <ScoreDistributionChart buckets={stats.scoreDistribution} />
          </div>

          <div>
            <Text className="mb-2 block font-bold">Câu bị làm sai nhiều nhất</Text>
            {stats.hardestQuestions.length === 0 ? (
              <EmptyState
                title="Chưa có câu nào đủ dữ liệu"
                hint="Một câu phải có ít nhất 3 lượt trả lời mới được xếp vào đây — sai 1/1 lượt chỉ nói lên là ít người làm"
              />
            ) : (
              <Table
                scroll={{ x: 'max-content' }}
                rowKey="questionId"
                size="small"
                columns={hardColumns}
                dataSource={stats.hardestQuestions}
                pagination={false}
              />
            )}
          </div>

          <div>
            <Text className="mb-2 block font-bold">Bài làm ({attempts?.length ?? 0})</Text>
            <Table
              scroll={{ x: 'max-content' }}
              rowKey="attemptId"
              size="small"
              columns={attemptColumns}
              dataSource={attempts ?? []}
              pagination={{ pageSize: 20, hideOnSinglePage: true }}
            />
          </div>
        </>
      )}
    </Space>
  )
}
