import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Alert, Button, Radio, Skeleton, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getApiErrorMessage } from '@/shared/api/client'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import { DIFFICULTY_COLOR, DIFFICULTY_LABEL } from '@/features/quiz/constants'
import { useQuizSummary } from '@/features/quiz/hooks/useQuizQueries'
import { useAuthStore } from '@/features/auth/store/authStore'
import type { AttemptMode, AttemptSummary, LeaderboardEntry } from '../api/attemptApi'
import { MODE_HINT, MODE_LABEL, STATUS_COLOR, STATUS_LABEL, formatDuration } from '../constants'
import { useAttemptHistory, useLeaderboard, useStartAttempt } from '../hooks/useAttemptQueries'

const { Text, Paragraph } = Typography

/**
 * Trang giới thiệu một quiz — nơi người học chọn chế độ và bắt đầu làm bài,
 * đồng thời xem lại các lần làm trước và bảng xếp hạng (FR-18, FR-19).
 */
export default function QuizIntroPage() {
  const { id: quizId } = useParams<{ id: string }>()
  const currentUser = useAuthStore((state) => state.user)
  const { data: quiz, isPending, error } = useQuizSummary(quizId)
  const { data: history } = useAttemptHistory({ quizId, size: 5 })
  const { data: leaderboard } = useLeaderboard(quizId)
  const startAttempt = useStartAttempt()

  const [mode, setMode] = useState<AttemptMode>('EXAM')

  if (error) {
    return <Alert type="error" showIcon message={getApiErrorMessage(error)} />
  }
  if (isPending || !quiz) {
    return <Skeleton active paragraph={{ rows: 6 }} />
  }

  const minutes = quiz.timeLimitSec ? Math.round(quiz.timeLimitSec / 60) : null
  const inProgress = history?.content.find((attempt) => attempt.status === 'IN_PROGRESS')
  const isOwner = currentUser?.id === quiz.ownerId

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title={quiz.title}
        description={
          <Space size={8} wrap>
            <Link to="/quizzes" className="font-bold">
              ← Khám phá quiz
            </Link>
            <span className="text-ink-soft">·</span>
            <span className="text-ink-soft">{quiz.categoryName ?? 'Chưa phân loại'}</span>
            <Tag color={DIFFICULTY_COLOR[quiz.difficulty]} className="mr-0!">
              {DIFFICULTY_LABEL[quiz.difficulty]}
            </Tag>
            {quiz.aiGenerated && <Tag className="mr-0!">AI sinh</Tag>}
          </Space>
        }
      />

      {isOwner && (
        <Alert
          type="info"
          showIcon
          message="Đây là quiz của bạn. Bạn vẫn làm bài được để tự kiểm đề — đáp án bị giấu y như với người học. Bài của bạn không tính vào bảng xếp hạng vì bạn đã biết trước đáp án."
          action={
            <Link to={`/my-quizzes/${quizId}`}>
              <Button size="small">Soạn câu hỏi</Button>
            </Link>
          }
        />
      )}

      {inProgress && (
        <Alert
          type="warning"
          showIcon
          message="Bạn đang có một bài làm dở trên quiz này."
          action={
            <Link to={`/attempts/${inProgress.id}`}>
              <Button size="small" type="primary">
                Làm tiếp
              </Button>
            </Link>
          }
        />
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="flex flex-col gap-4">
          {quiz.thumbnailUrl && (
            <img
              src={quiz.thumbnailUrl}
              alt=""
              className="aspect-video w-full border border-line object-cover"
            />
          )}

          <div className="border border-line bg-white p-5">
            <Text className="text-ink-soft text-xs font-bold">Giới thiệu</Text>
            <Paragraph className="mt-2! mb-0! whitespace-pre-wrap">
              {quiz.description?.trim() || (
                <Text className="text-ink-soft">Quiz này chưa có mô tả.</Text>
              )}
            </Paragraph>
          </div>

          <div className="border border-line bg-white">
            <div className="border-b border-line px-4 py-3">
              <Text className="font-bold!">Bảng xếp hạng</Text>
            </div>
            <Table<LeaderboardEntry>
              rowKey="userId"
              size="middle"
              pagination={false}
              dataSource={leaderboard ?? []}
              locale={{
                emptyText: (
                  <EmptyState
                    title="Chưa ai hoàn thành quiz này"
                    hint={
                      isOwner
                        ? 'Bảng xếp hạng chỉ tính bài của người học khác, không tính bài của bạn.'
                        : 'Nộp bài đầu tiên để đứng đầu bảng.'
                    }
                  />
                ),
              }}
              columns={LEADERBOARD_COLUMNS}
            />
          </div>

          <div className="border border-line bg-white">
            <div className="flex items-center justify-between border-b border-line px-4 py-3">
              <Text className="font-bold!">Lần làm gần đây của tôi</Text>
              <Link to="/my-attempts" className="text-xs font-bold">
                Xem tất cả
              </Link>
            </div>
            <Table<AttemptSummary>
              rowKey="id"
              size="middle"
              pagination={false}
              dataSource={history?.content ?? []}
              locale={{
                emptyText: <EmptyState title="Bạn chưa làm quiz này lần nào" />,
              }}
              columns={MY_ATTEMPT_COLUMNS}
            />
          </div>
        </div>

        {/* Khối hành động dính theo cuộn — chỗ Udemy đặt giá thì ở đây đặt nút bắt đầu */}
        <aside className="h-fit border border-line bg-white p-5 lg:sticky lg:top-24">
          <div className="mb-4 flex flex-col gap-1">
            <Text className="text-ink-soft text-xs">Nội dung</Text>
            <Text className="font-bold!">{quiz.questionCount} câu hỏi</Text>
            <Text className="text-ink-soft text-xs">
              {minutes ? `Giới hạn ${minutes} phút` : 'Không giới hạn thời gian'}
            </Text>
            <Text className="text-ink-soft text-xs">Người tạo: {quiz.ownerDisplayName}</Text>
          </div>

          <Text className="text-ink-soft text-xs font-bold">Chọn chế độ</Text>
          <Radio.Group
            value={mode}
            onChange={(event) => setMode(event.target.value)}
            className="mt-2 mb-2 w-full"
          >
            <Space direction="vertical" size={4} className="w-full">
              <Radio value="EXAM">{MODE_LABEL.EXAM}</Radio>
              <Radio value="PRACTICE">{MODE_LABEL.PRACTICE}</Radio>
            </Space>
          </Radio.Group>
          <Paragraph className="mb-4! text-ink-soft text-xs">{MODE_HINT[mode]}</Paragraph>

          <Button
            type="primary"
            size="large"
            block
            disabled={quiz.questionCount === 0}
            loading={startAttempt.isPending}
            onClick={() => quizId && startAttempt.mutate({ quizId, mode })}
          >
            {inProgress ? 'Làm tiếp bài dở' : 'Bắt đầu làm bài'}
          </Button>
          {quiz.questionCount === 0 && (
            <Text className="mt-2 block text-ink-soft text-xs">
              Quiz chưa có câu hỏi nên chưa làm được.
            </Text>
          )}
        </aside>
      </div>
    </Space>
  )
}

const LEADERBOARD_COLUMNS: ColumnsType<LeaderboardEntry> = [
  {
    title: '#',
    dataIndex: 'rank',
    width: 60,
    render: (rank: number) => <Text className="font-bold!">{rank}</Text>,
  },
  { title: 'Người chơi', dataIndex: 'displayName' },
  {
    title: 'Điểm',
    key: 'score',
    width: 100,
    render: (_, row) => (
      <Text className="font-bold!">
        {row.totalScore}/{row.maxScore}
      </Text>
    ),
  },
  {
    title: 'Thời gian',
    dataIndex: 'durationSec',
    width: 110,
    render: (value: number | null) => formatDuration(value),
  },
]

const MY_ATTEMPT_COLUMNS: ColumnsType<AttemptSummary> = [
  {
    title: 'Bắt đầu',
    dataIndex: 'startedAt',
    render: (value: string) => new Date(value).toLocaleString('vi-VN'),
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
    title: 'Điểm',
    key: 'score',
    width: 90,
    render: (_, row) =>
      row.status === 'IN_PROGRESS' ? (
        <Text className="text-ink-soft">—</Text>
      ) : (
        <Text className="font-bold!">
          {row.totalScore}/{row.maxScore}
        </Text>
      ),
  },
  {
    title: '',
    key: 'actions',
    width: 90,
    render: (_, row) => (
      <Link to={`/attempts/${row.id}`} className="text-xs font-bold">
        {row.status === 'IN_PROGRESS' ? 'Làm tiếp' : 'Xem lại'}
      </Link>
    ),
  },
]
