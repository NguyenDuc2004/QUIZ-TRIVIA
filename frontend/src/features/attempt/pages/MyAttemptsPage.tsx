import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Progress, Space, Table, Tag, Typography } from 'antd'
import { EyeOutlined, PlayCircleOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import type { AttemptSummary } from '../api/attemptApi'
import { MODE_LABEL, STATUS_COLOR, STATUS_LABEL, formatDuration } from '../constants'
import { useAttemptHistory } from '../hooks/useAttemptQueries'

const { Text } = Typography

/** Lịch sử làm bài (FR-18) — bộ mặt bảng điều khiển (docs/ui-design-system.md §1). */
export default function MyAttemptsPage() {
  const [page, setPage] = useState(0)
  const { data, isFetching } = useAttemptHistory({ page, size: 10 })

  const columns: ColumnsType<AttemptSummary> = [
    {
      title: 'Quiz',
      dataIndex: 'quizTitle',
      render: (title: string, row) => (
        <Space direction="vertical" size={0}>
          <Link to={`/quizzes/${row.quizId}`} className="font-bold">
            {title}
          </Link>
          <Text className="text-ink-soft text-xs">
            {new Date(row.startedAt).toLocaleString('vi-VN')}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Chế độ',
      dataIndex: 'mode',
      width: 110,
      render: (value: AttemptSummary['mode']) => <Tag className="mr-0!">{MODE_LABEL[value]}</Tag>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 120,
      render: (value: AttemptSummary['status']) => (
        <Tag color={STATUS_COLOR[value]} className="mr-0!">
          {STATUS_LABEL[value]}
        </Tag>
      ),
    },
    {
      title: 'Kết quả',
      key: 'score',
      width: 190,
      render: (_, row) => {
        if (row.status === 'IN_PROGRESS') {
          return (
            <Text className="text-ink-soft text-xs">
              {row.answeredCount}/{row.questionCount} câu đã trả lời
            </Text>
          )
        }
        const percent = row.maxScore > 0 ? Math.round((row.totalScore / row.maxScore) * 100) : 0
        return (
          <Space direction="vertical" size={0} className="w-full">
            <Text className="font-bold!">
              {row.totalScore}/{row.maxScore} điểm · {row.correctCount}/{row.questionCount} câu đúng
            </Text>
            <Progress percent={percent} size="small" strokeColor="#1c1d1f" />
          </Space>
        )
      },
    },
    {
      title: 'Thời gian làm',
      dataIndex: 'durationSec',
      width: 120,
      render: (value: number | null) => formatDuration(value),
    },
    {
      title: '',
      key: 'actions',
      width: 90,
      render: (_, row) => (
        <Link to={`/attempts/${row.id}`}>
          <Button
            size="small"
            /* Bài đang làm dở là nút chính: đó là việc người dùng cần làm tiếp, không phải việc xem
               lại. Hai trạng thái, hai mức nhấn mạnh. */
            type={row.status === 'IN_PROGRESS' ? 'primary' : 'default'}
            icon={row.status === 'IN_PROGRESS' ? <PlayCircleOutlined /> : <EyeOutlined />}
          >
            {row.status === 'IN_PROGRESS' ? 'Làm tiếp' : 'Xem lại'}
          </Button>
        </Link>
      ),
    },
  ]

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Lịch sử làm bài"
        description="Toàn bộ các lần bạn làm quiz, kèm điểm và thời gian."
      />

      <div className="soft-panel">
        <Table<AttemptSummary>
          scroll={{ x: 'max-content' }}
          rowKey="id"
          size="middle"
          loading={isFetching}
          columns={columns}
          dataSource={data?.content ?? []}
          locale={{
            emptyText: (
              <EmptyState
                title="Bạn chưa làm bài nào"
                hint="Chọn một quiz ở trang Khám phá để bắt đầu."
              />
            ),
          }}
          pagination={{
            current: (data?.page ?? 0) + 1,
            pageSize: data?.size ?? 10,
            total: data?.totalElements ?? 0,
            showSizeChanger: false,
            onChange: (nextPage) => setPage(nextPage - 1),
          }}
        />
      </div>
    </Space>
  )
}
