import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Alert, Button, Input, List, Modal, Select, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getApiErrorMessage } from '@/shared/api/client'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import type { Difficulty, Question } from '../api/quizApi'
import {
  DIFFICULTY_COLOR,
  DIFFICULTY_LABEL,
  DIFFICULTY_OPTIONS,
  QUESTION_TYPE_LABEL,
  VISIBILITY_LABEL,
} from '../constants'
import {
  useQuestionBank,
  useQuestionTopics,
  useQuizDetail,
  useSetQuizQuestions,
} from '../hooks/useQuizQueries'
import QuestionFormModal from '../components/QuestionFormModal'

const { Text } = Typography

/**
 * Màn soạn quiz — bộ mặt **bảng điều khiển** (docs/ui-design-system.md §1):
 * chọn câu hỏi từ ngân hàng, sắp thứ tự, rồi lưu cả danh sách một lần.
 */
export default function QuizEditorPage() {
  const { id: quizId } = useParams<{ id: string }>()
  const { data, isPending, error } = useQuizDetail(quizId)
  const setQuestions = useSetQuizQuestions()

  /** Danh sách đang chỉnh (chưa lưu) — thứ tự trong mảng là thứ tự câu hỏi. */
  const [selected, setSelected] = useState<Question[]>([])
  const [pickerOpen, setPickerOpen] = useState(false)
  const [creatingQuestion, setCreatingQuestion] = useState(false)

  useEffect(() => {
    if (data) {
      setSelected(data.questions)
    }
  }, [data])

  const selectedIds = useMemo(() => new Set(selected.map((q) => q.id)), [selected])
  const isDirty = useMemo(() => {
    const current = data?.questions.map((q) => q.id).join(',') ?? ''
    return current !== selected.map((q) => q.id).join(',')
  }, [data, selected])

  const totalPoints = selected.reduce((sum, q) => sum + q.points, 0)

  const move = (index: number, delta: number) => {
    const target = index + delta
    if (target < 0 || target >= selected.length) return
    const next = [...selected]
    ;[next[index], next[target]] = [next[target], next[index]]
    setSelected(next)
  }

  const columns: ColumnsType<Question> = [
    {
      title: '#',
      width: 50,
      render: (_, __, index) => <Text className="text-ink-soft text-xs">{index + 1}</Text>,
    },
    {
      title: 'Nội dung',
      dataIndex: 'content',
      render: (content: string) => <Text className="font-bold!">{content}</Text>,
    },
    {
      title: 'Loại',
      dataIndex: 'type',
      width: 140,
      render: (value: Question['type']) => <Tag>{QUESTION_TYPE_LABEL[value]}</Tag>,
    },
    {
      title: 'Độ khó',
      dataIndex: 'difficulty',
      width: 120,
      render: (value: Question['difficulty']) => (
        <Tag color={DIFFICULTY_COLOR[value]}>{DIFFICULTY_LABEL[value]}</Tag>
      ),
    },
    { title: 'Điểm', dataIndex: 'points', width: 70, align: 'center' },
    {
      title: 'Thứ tự',
      key: 'order',
      width: 160,
      render: (_, __, index) => (
        <Space size={4}>
          <Button size="small" disabled={index === 0} onClick={() => move(index, -1)}>
            ↑
          </Button>
          <Button
            size="small"
            disabled={index === selected.length - 1}
            onClick={() => move(index, 1)}
          >
            ↓
          </Button>
          <Button
            size="small"
            danger
            type="link"
            onClick={() => setSelected(selected.filter((_, i) => i !== index))}
          >
            Bỏ
          </Button>
        </Space>
      ),
    },
  ]

  if (error) {
    return <Alert type="error" showIcon message={getApiErrorMessage(error)} />
  }

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title={data?.quiz.title ?? 'Đang tải…'}
        description={
          <span className="flex flex-wrap items-center gap-2">
            <Link to="/my-quizzes" className="font-bold">
              ← Quiz của tôi
            </Link>
            {data && (
              <>
                <span className="text-ink-soft">·</span>
                <span className="text-ink-soft">
                  {data.quiz.categoryName ?? 'Chưa có danh mục'}
                </span>
                <Tag color={DIFFICULTY_COLOR[data.quiz.difficulty]} className="mr-0!">
                  {DIFFICULTY_LABEL[data.quiz.difficulty]}
                </Tag>
                <Tag color={data.quiz.visibility === 'PUBLIC' ? 'purple' : undefined} className="mr-0!">
                  {VISIBILITY_LABEL[data.quiz.visibility]}
                </Tag>
              </>
            )}
          </span>
        }
        actions={
          <>
            {/* Tự làm thử đề của mình trước khi xuất bản; đáp án vẫn bị giấu như người học */}
            {selected.length > 0 && (
              <Link to={`/quizzes/${quizId}`}>
                <Button>Làm thử</Button>
              </Link>
            )}
            <Button onClick={() => setPickerOpen(true)}>Chọn từ ngân hàng</Button>
            <Button onClick={() => setCreatingQuestion(true)}>Soạn câu hỏi mới</Button>
            <Button
              type="primary"
              disabled={!isDirty}
              loading={setQuestions.isPending}
              onClick={() =>
                quizId && setQuestions.mutate({ id: quizId, questionIds: selected.map((q) => q.id) })
              }
            >
              Lưu danh sách
            </Button>
          </>
        }
      />

      {isDirty && (
        <Alert type="warning" showIcon message="Danh sách câu hỏi đã thay đổi nhưng chưa lưu." />
      )}

      <div className="soft-panel">
        <div className="flex items-center justify-between border-b border-line px-4 py-3">
          <Text className="font-bold!">Câu hỏi trong quiz</Text>
          <Text className="text-ink-soft text-xs">
            {selected.length} câu · tổng {totalPoints} điểm
          </Text>
        </div>

        <Table<Question>
          scroll={{ x: 'max-content' }}
          rowKey="id"
          size="middle"
          loading={isPending}
          columns={columns}
          dataSource={selected}
          pagination={false}
          locale={{
            emptyText: (
              <EmptyState
                title="Quiz chưa có câu hỏi nào"
                hint="Chọn câu hỏi có sẵn từ ngân hàng, hoặc soạn câu mới."
                action={
                  <Button type="primary" onClick={() => setPickerOpen(true)}>
                    Chọn từ ngân hàng
                  </Button>
                }
              />
            ),
          }}
        />
      </div>

      <QuestionPickerModal
        open={pickerOpen}
        excludeIds={selectedIds}
        onClose={() => setPickerOpen(false)}
        onPick={(questions) => {
          setSelected([...selected, ...questions])
          setPickerOpen(false)
        }}
      />

      <QuestionFormModal
        open={creatingQuestion}
        question={null}
        onClose={() => setCreatingQuestion(false)}
      />
    </Space>
  )
}

/** Bảng chọn câu hỏi từ ngân hàng, ẩn những câu đã có trong quiz. */
function QuestionPickerModal({
  open,
  excludeIds,
  onClose,
  onPick,
}: {
  open: boolean
  excludeIds: Set<string>
  onClose: () => void
  onPick: (questions: Question[]) => void
}) {
  const [page, setPage] = useState(0)
  const [checked, setChecked] = useState<Question[]>([])
  const [keyword, setKeyword] = useState('')
  const [topic, setTopic] = useState<string | undefined>()
  const [difficulty, setDifficulty] = useState<Difficulty | undefined>()

  // Lọc ngay ở truy vấn chứ không lọc sau khi tải: ngân hàng có thể hàng trăm câu, mà mỗi lần chỉ
  // lấy về 8 câu — lọc phía client thì chỉ lọc được trong 8 câu đó, gần như vô dụng.
  const { data, isFetching } = useQuestionBank({
    page,
    size: 8,
    q: keyword || undefined,
    topic,
    difficulty,
  })
  const { data: topics } = useQuestionTopics()

  const available = (data?.content ?? []).filter((q) => !excludeIds.has(q.id))

  /** Đổi bộ lọc thì phải về trang đầu, nếu không sẽ rơi vào trang trống của kết quả mới. */
  const changeFilter = (apply: () => void) => {
    apply()
    setPage(0)
    }

  return (
    <Modal
      open={open}
      width={800}
      title="Chọn câu hỏi từ ngân hàng"
      okText={`Thêm ${checked.length > 0 ? `(${checked.length})` : ''}`}
      cancelText="Hủy"
      okButtonProps={{ disabled: checked.length === 0 }}
      onOk={() => {
        onPick(checked)
        setChecked([])
      }}
      onCancel={() => {
        setChecked([])
        onClose()
      }}
    >
      <Space wrap className="mb-3 w-full">
        <Input.Search
          allowClear
          placeholder="Tìm trong nội dung câu hỏi"
          style={{ width: 260 }}
          onSearch={(value) => changeFilter(() => setKeyword(value))}
        />
        <Select
          allowClear
          showSearch
          placeholder="Chủ đề"
          style={{ width: 220 }}
          value={topic}
          onChange={(value) => changeFilter(() => setTopic(value))}
          options={(topics ?? []).map((item) => ({
            value: item.topic,
            label: `${item.topic} (${item.questionCount})`,
          }))}
          notFoundContent="Chưa có câu hỏi nào được đặt chủ đề"
        />
        <Select
          allowClear
          placeholder="Độ khó"
          style={{ width: 140 }}
          value={difficulty}
          onChange={(value) => changeFilter(() => setDifficulty(value))}
          options={DIFFICULTY_OPTIONS}
        />
      </Space>

      <List
        loading={isFetching}
        dataSource={available}
        locale={{
          emptyText: (
            <EmptyState
              title={
                keyword || topic || difficulty
                  ? 'Không có câu nào khớp bộ lọc'
                  : 'Không còn câu hỏi nào để thêm'
              }
              hint={
                keyword || topic || difficulty
                  ? 'Thử bỏ bớt điều kiện lọc, hoặc soạn câu mới cho chủ đề này.'
                  : 'Mọi câu trong ngân hàng đã nằm trong quiz này.'
              }
            />
          ),
        }}
        pagination={{
          current: (data?.page ?? 0) + 1,
          pageSize: data?.size ?? 8,
          total: data?.totalElements ?? 0,
          onChange: (nextPage) => setPage(nextPage - 1),
        }}
        renderItem={(question) => {
          const isChecked = checked.some((q) => q.id === question.id)
          return (
            <List.Item
              onClick={() =>
                setChecked(
                  isChecked ? checked.filter((q) => q.id !== question.id) : [...checked, question],
                )
              }
              className={`cursor-pointer px-2! ${isChecked ? 'bg-surface-subtle' : ''}`}
            >
              <List.Item.Meta
                title={<span className="font-bold">{question.content}</span>}
                description={
                  <Space size={4}>
                    <Tag className="mr-0!">{QUESTION_TYPE_LABEL[question.type]}</Tag>
                    <Tag color={DIFFICULTY_COLOR[question.difficulty]} className="mr-0!">
                      {DIFFICULTY_LABEL[question.difficulty]}
                    </Tag>
                    <Text className="text-ink-soft text-xs">{question.points} điểm</Text>
                    {question.topic && (
                      <Text className="text-ink-soft text-xs">· {question.topic}</Text>
                    )}
                  </Space>
                }
              />
            </List.Item>
          )
        }}
      />
    </Modal>
  )
}
