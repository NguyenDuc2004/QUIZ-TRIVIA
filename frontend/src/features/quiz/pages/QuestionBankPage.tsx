import { useState } from 'react'
import { Button, Card, Empty, Input, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { Difficulty, Question, QuestionType } from '../api/quizApi'
import {
  DIFFICULTY_COLOR,
  DIFFICULTY_LABEL,
  DIFFICULTY_OPTIONS,
  QUESTION_TYPE_LABEL,
  QUESTION_TYPE_OPTIONS,
} from '../constants'
import { useDeleteQuestion, useQuestionBank } from '../hooks/useQuizQueries'
import QuestionFormModal from '../components/QuestionFormModal'

const { Title, Paragraph, Text } = Typography

export default function QuestionBankPage() {
  const [page, setPage] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [type, setType] = useState<QuestionType | undefined>()
  const [difficulty, setDifficulty] = useState<Difficulty | undefined>()
  const [editing, setEditing] = useState<Question | null>(null)
  const [creating, setCreating] = useState(false)

  const { data, isFetching } = useQuestionBank({
    page,
    size: 10,
    q: keyword || undefined,
    type,
    difficulty,
  })
  const deleteQuestion = useDeleteQuestion()

  const columns: ColumnsType<Question> = [
    {
      title: 'Nội dung',
      dataIndex: 'content',
      render: (content: string, row) => (
        <Space direction="vertical" size={0}>
          <Text>{content}</Text>
          <Text type="secondary" className="text-xs">
            {row.options.filter((o) => o.correct).length} đáp án đúng / {row.options.length} lựa chọn
          </Text>
        </Space>
      ),
    },
    {
      title: 'Loại',
      dataIndex: 'type',
      width: 140,
      render: (value: QuestionType) => <Tag>{QUESTION_TYPE_LABEL[value]}</Tag>,
    },
    {
      title: 'Độ khó',
      dataIndex: 'difficulty',
      width: 120,
      render: (value: Difficulty) => <Tag color={DIFFICULTY_COLOR[value]}>{DIFFICULTY_LABEL[value]}</Tag>,
    },
    { title: 'Chủ đề', dataIndex: 'topic', width: 130, render: (value: string | null) => value ?? '—' },
    { title: 'Điểm', dataIndex: 'points', width: 70, align: 'center' },
    {
      title: 'Nguồn',
      dataIndex: 'source',
      width: 100,
      render: (value: string) =>
        value === 'AI_GENERATED' ? <Tag color="purple">AI sinh</Tag> : <Tag>Tự soạn</Tag>,
    },
    {
      title: '',
      key: 'actions',
      width: 120,
      render: (_, row) => (
        <Space size="small">
          <Button type="link" size="small" onClick={() => setEditing(row)}>
            Sửa
          </Button>
          <Popconfirm
            title="Xóa câu hỏi này?"
            description="Không xóa được nếu câu hỏi đang nằm trong quiz."
            okText="Xóa"
            cancelText="Hủy"
            onConfirm={() => deleteQuestion.mutate(row.id)}
          >
            <Button type="link" size="small" danger>
              Xóa
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Space direction="vertical" size="large" className="w-full">
      <div className="flex items-end justify-between gap-4">
        <div>
          <Title level={3} className="!mb-1">
            Ngân hàng câu hỏi
          </Title>
          <Paragraph type="secondary" className="!mb-0">
            Câu hỏi soạn ở đây dùng lại được cho nhiều quiz.
          </Paragraph>
        </div>
        <Button type="primary" onClick={() => setCreating(true)}>
          Thêm câu hỏi
        </Button>
      </div>

      <Card>
        <Space wrap className="mb-4">
          <Input.Search
            allowClear
            placeholder="Tìm trong nội dung câu hỏi"
            style={{ width: 280 }}
            onSearch={(value) => {
              setKeyword(value)
              setPage(0)
            }}
          />
          <Select
            allowClear
            placeholder="Loại câu hỏi"
            style={{ width: 180 }}
            value={type}
            onChange={(value) => {
              setType(value)
              setPage(0)
            }}
            options={QUESTION_TYPE_OPTIONS}
          />
          <Select
            allowClear
            placeholder="Độ khó"
            style={{ width: 150 }}
            value={difficulty}
            onChange={(value) => {
              setDifficulty(value)
              setPage(0)
            }}
            options={DIFFICULTY_OPTIONS}
          />
        </Space>

        <Table<Question>
          rowKey="id"
          size="middle"
          loading={isFetching}
          columns={columns}
          dataSource={data?.content ?? []}
          locale={{ emptyText: <Empty description="Ngân hàng câu hỏi còn trống" /> }}
          pagination={{
            current: (data?.page ?? 0) + 1,
            pageSize: data?.size ?? 10,
            total: data?.totalElements ?? 0,
            showSizeChanger: false,
            onChange: (nextPage) => setPage(nextPage - 1),
          }}
        />
      </Card>

      <QuestionFormModal
        open={creating || editing !== null}
        question={editing}
        onClose={() => {
          setCreating(false)
          setEditing(null)
        }}
      />
    </Space>
  )
}
