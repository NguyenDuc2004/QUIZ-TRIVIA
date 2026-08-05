import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Button,
  Card,
  Empty,
  Input,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useAuthStore } from '@/features/auth/store/authStore'
import type { Difficulty, QuizSummary } from '../api/quizApi'
import { DIFFICULTY_COLOR, DIFFICULTY_LABEL, DIFFICULTY_OPTIONS, VISIBILITY_LABEL } from '../constants'
import { useCategories, useDeleteQuiz, useQuizList } from '../hooks/useQuizQueries'
import QuizFormModal from '../components/QuizFormModal'

const { Title, Paragraph } = Typography

/**
 * Dùng cho cả hai màn: "Khám phá quiz" (quiz công khai) và "Quiz của tôi".
 * Chỉ khác tham số `mine` gửi lên backend.
 */
export default function QuizListPage({ mine = false }: { mine?: boolean }) {
  const [page, setPage] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState<string | undefined>()
  const [difficulty, setDifficulty] = useState<Difficulty | undefined>()
  const [editing, setEditing] = useState<QuizSummary | null>(null)
  const [creating, setCreating] = useState(false)

  const user = useAuthStore((state) => state.user)
  const canCreate = user?.role === 'CREATOR' || user?.role === 'ADMIN'

  const { data: categories } = useCategories()
  const { data, isFetching } = useQuizList({
    mine,
    page,
    size: 10,
    q: keyword || undefined,
    categoryId,
    difficulty,
  })
  const deleteQuiz = useDeleteQuiz()

  const columns: ColumnsType<QuizSummary> = [
    {
      title: 'Tiêu đề',
      dataIndex: 'title',
      render: (title: string, row) =>
        mine ? <Link to={`/my-quizzes/${row.id}`}>{title}</Link> : <span>{title}</span>,
    },
    { title: 'Danh mục', dataIndex: 'categoryName', render: (value: string | null) => value ?? '—' },
    {
      title: 'Độ khó',
      dataIndex: 'difficulty',
      width: 120,
      render: (value: Difficulty) => <Tag color={DIFFICULTY_COLOR[value]}>{DIFFICULTY_LABEL[value]}</Tag>,
    },
    { title: 'Số câu', dataIndex: 'questionCount', width: 90, align: 'center' },
    ...(mine
      ? [
          {
            title: 'Hiển thị',
            dataIndex: 'visibility',
            width: 120,
            render: (value: 'PUBLIC' | 'PRIVATE') => (
              <Tag color={value === 'PUBLIC' ? 'blue' : 'default'}>{VISIBILITY_LABEL[value]}</Tag>
            ),
          } as ColumnsType<QuizSummary>[number],
        ]
      : [
          {
            title: 'Người tạo',
            dataIndex: 'ownerDisplayName',
          } as ColumnsType<QuizSummary>[number],
        ]),
    {
      title: 'Thời gian',
      dataIndex: 'timeLimitSec',
      width: 120,
      render: (value: number | null) => (value ? `${Math.round(value / 60)} phút` : 'Không giới hạn'),
    },
    ...(mine
      ? [
          {
            title: '',
            key: 'actions',
            width: 190,
            render: (_: unknown, row: QuizSummary) => (
              <Space size="small">
                <Link to={`/my-quizzes/${row.id}`}>Soạn câu hỏi</Link>
                <Button type="link" size="small" onClick={() => setEditing(row)}>
                  Sửa
                </Button>
                <Popconfirm
                  title="Xóa quiz này?"
                  description="Câu hỏi vẫn còn trong ngân hàng."
                  okText="Xóa"
                  cancelText="Hủy"
                  onConfirm={() => deleteQuiz.mutate(row.id)}
                >
                  <Button type="link" size="small" danger>
                    Xóa
                  </Button>
                </Popconfirm>
              </Space>
            ),
          } as ColumnsType<QuizSummary>[number],
        ]
      : []),
  ]

  return (
    <Space direction="vertical" size="large" className="w-full">
      <div className="flex items-end justify-between gap-4">
        <div>
          <Title level={3} className="!mb-1">
            {mine ? 'Quiz của tôi' : 'Khám phá quiz'}
          </Title>
          <Paragraph type="secondary" className="!mb-0">
            {mine
              ? 'Quiz bạn tạo, gồm cả quiz đang ở chế độ riêng tư.'
              : 'Các quiz đã được xuất bản công khai.'}
          </Paragraph>
        </div>
        {mine && canCreate && (
          <Button type="primary" onClick={() => setCreating(true)}>
            Tạo quiz
          </Button>
        )}
      </div>

      <Card>
        <Space wrap className="mb-4">
          <Input.Search
            allowClear
            placeholder="Tìm theo tiêu đề"
            style={{ width: 260 }}
            onSearch={(value) => {
              setKeyword(value)
              setPage(0)
            }}
          />
          <Select
            allowClear
            placeholder="Danh mục"
            style={{ width: 180 }}
            value={categoryId}
            onChange={(value) => {
              setCategoryId(value)
              setPage(0)
            }}
            options={(categories ?? []).map((c) => ({ value: c.id, label: c.name }))}
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

        <Table<QuizSummary>
          rowKey="id"
          size="middle"
          loading={isFetching}
          columns={columns}
          dataSource={data?.content ?? []}
          locale={{
            emptyText: (
              <Empty
                description={mine ? 'Bạn chưa tạo quiz nào' : 'Chưa có quiz công khai nào'}
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
      </Card>

      <QuizFormModal
        open={creating || editing !== null}
        quiz={editing}
        onClose={() => {
          setCreating(false)
          setEditing(null)
        }}
      />
    </Space>
  )
}
