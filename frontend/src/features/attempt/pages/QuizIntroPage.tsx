import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { TrophyOutlined } from '@ant-design/icons'
import { Alert, Button, Checkbox, Radio, Skeleton, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getApiErrorMessage } from '@/shared/api/client'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import { DIFFICULTY_COLOR, DIFFICULTY_LABEL } from '@/features/quiz/constants'
import { useQuizSummary } from '@/features/quiz/hooks/useQuizQueries'
import { useAuthStore } from '@/features/auth/store/authStore'
import ProctoringNotice from '@/features/integrity/components/ProctoringNotice'
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

  // Xác nhận đã đọc thông báo ghi nhận hành vi. KHÔNG nhớ lại giữa các lần: mục đích của ô này là người thi
  // đọc trước LẦN THI NÀY, không phải một lần duy nhất trong đời rồi thôi. Bỏ qua sau lần đầu thì nó thành
  // một hộp thoại người ta bấm cho xong, đúng thứ mà việc báo trước cần tránh.
  const [daHieu, setDaHieu] = useState(false)

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
              scroll={{ x: 'max-content' }}
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
              scroll={{ x: 'max-content' }}
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
            {/* Trang này là nơi người học QUYẾT ĐỊNH có làm hay không, nên con số thuộc về đây rõ hơn cả */}
            {quiz.learnerCount > 0 && (
              <Text className="text-ink-soft text-xs">{quiz.learnerCount} người đã làm quiz này</Text>
            )}
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

          {/* Nói luật TRƯỚC khi đồng hồ chạy.

              Thông báo này vốn đã có, nhưng nằm ở màn LÀM BÀI — tức người thi chỉ đọc được sau khi bài đã
              bắt đầu và giờ đã chạy. Đọc lúc đó thì không còn lựa chọn nào: không đóng bớt tab được, không
              đổi chỗ ngồi được, và lần rời trang đầu tiên rất có thể xảy ra ngay trong lúc đang đọc chính
              dòng chữ giải thích rằng rời trang sẽ bị ghi lại.

              Ô xác nhận bên dưới là thứ biến "có dán thông báo" thành "người thi đã đọc". Chỉ áp cho chế độ
              Thi, vì luyện tập không ghi nhận gì. */}
          {mode === 'EXAM' && (
            <div className="mb-4 flex flex-col gap-3">
              <ProctoringNotice />
              <Checkbox checked={daHieu} onChange={(e) => setDaHieu(e.target.checked)}>
                <span className="text-sm">Tôi đã đọc và hiểu những tín hiệu được ghi nhận</span>
              </Checkbox>
            </div>
          )}

          {/* FR-48. Nói TRƯỚC khi bấm bắt đầu, không phải lúc màn hình đã đổi: người học cần biết mình sắp
              vào toàn màn hình để còn chọn thời điểm — đóng bớt tab, ngồi vào chỗ yên tĩnh. Bật lên đột ngột
              giữa lúc họ chưa sẵn sàng thì lần thoát ra đầu tiên là do MÌNH gây ra, mà nó vẫn bị ghi lại.
              Chỉ hiện khi đang chọn chế độ Thi, vì luyện tập không áp cờ này */}
          {quiz.strictExam && mode === 'EXAM' && (
            <Alert
              type="warning"
              showIcon
              className="mb-4"
              message="Bài này bật chế độ thi nghiêm ngặt"
              description="Bạn sẽ được yêu cầu vào toàn màn hình trước khi làm bài, và chuột phải bị khoá. Bạn vẫn thoát toàn màn hình được, nhưng mỗi lần thoát đều được ghi lại."
            />
          )}

          <Button
            type="primary"
            size="large"
            block
            disabled={quiz.questionCount === 0 || (mode === 'EXAM' && !daHieu)}
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

          {quiz.questionCount > 0 && (
            <>
              <div className="my-4 border-t border-line" />
              <Text className="text-ink-soft text-xs">Hoặc thi đấu cùng bạn bè</Text>
              <Link to="/rooms">
                <Button block className="mt-2">
                  Mở phòng đấu trí
                </Button>
              </Link>
            </>
          )}
        </aside>
      </div>
    </Space>
  )
}

const LEADERBOARD_COLUMNS: ColumnsType<LeaderboardEntry> = [
  {
    // Dùng lại đúng huy chương của bảng xếp hạng mùa (`.podium-*` trong index.css), không vẽ lại kiểu
    // khác: hai bảng xếp hạng trong cùng một sản phẩm mà hạng nhất trông khác nhau thì người dùng phải
    // học hai lần cùng một thứ.
    title: '#',
    dataIndex: 'rank',
    width: 70,
    render: (rank: number) =>
      rank <= 3 ? (
        <span className={`podium podium-${rank}`}>
          <TrophyOutlined />
          <span className="font-bold">{rank}</span>
        </span>
      ) : (
        <Text className="text-ink-soft">{rank}</Text>
      ),
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
