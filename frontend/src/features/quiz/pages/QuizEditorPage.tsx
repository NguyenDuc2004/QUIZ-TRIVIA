import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  Empty,
  List,
  Modal,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { getApiErrorMessage } from '@/shared/api/client'
import type { Question } from '../api/quizApi'
import { DIFFICULTY_COLOR, DIFFICULTY_LABEL, QUESTION_TYPE_LABEL } from '../constants'
import { useQuestionBank, useQuizDetail, useSetQuizQuestions } from '../hooks/useQuizQueries'
import QuestionFormModal from '../components/QuestionFormModal'

const { Title, Paragraph, Text } = Typography

/** Màn soạn quiz: chọn câu hỏi từ ngân hàng, sắp thứ tự, rồi lưu cả danh sách. */
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
    { title: '#', width: 50, render: (_, __, index) => index + 1 },
    { title: 'Nội dung', dataIndex: 'content' },
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
      width: 150,
      render: (_, __, index) => (
        <Space size={4}>
          <Button size="small" disabled={index === 0} onClick={() => move(index, -1)}>
            ↑
          </Button>
          <Button size="small" disabled={index === selected.length - 1} onClick={() => move(index, 1)}>
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
      <div className="flex items-end justify-between gap-4">
        <div>
          <Title level={3} className="!mb-1">
            {data?.quiz.title ?? 'Đang tải…'}
          </Title>
          <Paragraph type="secondary" className="!mb-0">
            <Link to="/my-quizzes">← Quiz của tôi</Link>
            {data && (
              <>
                {' · '}
                {data.quiz.categoryName ?? 'Chưa có danh mục'} ·{' '}
                <Tag color={DIFFICULTY_COLOR[data.quiz.difficulty]}>
                  {DIFFICULTY_LABEL[data.quiz.difficulty]}
                </Tag>
                <Tag color={data.quiz.visibility === 'PUBLIC' ? 'blue' : 'default'}>
                  {data.quiz.visibility === 'PUBLIC' ? 'Công khai' : 'Riêng tư'}
                </Tag>
              </>
            )}
          </Paragraph>
        </div>
        <Space>
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
        </Space>
      </div>

      {isDirty && (
        <Alert
          type="warning"
          showIcon
          message="Danh sách câu hỏi đã thay đổi nhưng chưa lưu."
        />
      )}

      <Card
        title={
          <Space>
            <span>Câu hỏi trong quiz</span>
            <Text type="secondary">
              {selected.length} câu · tổng {totalPoints} điểm
            </Text>
          </Space>
        }
      >
        <Table<Question>
          rowKey="id"
          size="middle"
          loading={isPending}
          columns={columns}
          dataSource={selected}
          pagination={false}
          locale={{
            emptyText: <Empty description="Chưa có câu hỏi nào — chọn từ ngân hàng hoặc soạn mới" />,
          }}
        />
      </Card>

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
  const { data, isFetching } = useQuestionBank({ page, size: 8 })

  const available = (data?.content ?? []).filter((q) => !excludeIds.has(q.id))

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
      <List
        loading={isFetching}
        dataSource={available}
        locale={{ emptyText: 'Không còn câu hỏi nào để thêm' }}
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
              className="cursor-pointer"
              style={{ background: isChecked ? '#f0f5ff' : undefined, paddingInline: 8 }}
            >
              <List.Item.Meta
                title={question.content}
                description={
                  <Space size={4}>
                    <Tag>{QUESTION_TYPE_LABEL[question.type]}</Tag>
                    <Tag color={DIFFICULTY_COLOR[question.difficulty]}>
                      {DIFFICULTY_LABEL[question.difficulty]}
                    </Tag>
                    <Text type="secondary">{question.points} điểm</Text>
                    {question.topic && <Text type="secondary">· {question.topic}</Text>}
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
