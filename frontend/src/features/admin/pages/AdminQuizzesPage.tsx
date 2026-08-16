import { useState } from 'react'
import { Alert, Button, Input, Popconfirm, Select, Table, Tag, Typography } from 'antd'
import { EyeInvisibleOutlined } from '@ant-design/icons'
import { Link } from 'react-router-dom'
import PageHeader from '@/shared/components/PageHeader'
import { useAdminCategories, useAdminQuizzes, useHideQuiz } from '../hooks/useAdmin'
import type { QuizSummary } from '@/features/quiz/api/quizApi'

const { Text } = Typography

/**
 * Kiểm duyệt quiz công khai (FR-80).
 *
 * Danh sách này là **đúng danh sách người học nhìn thấy** ở trang khám phá — cùng một truy vấn. Quiz riêng
 * tư không có ở đây: không ai ngoài chủ của nó xem được nên chẳng có gì để kiểm duyệt, và đọc nội dung
 * riêng tư của người khác không phải quyền mà vai trò quản trị cần.
 */
export default function AdminQuizzesPage() {
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState<string | undefined>()
  const [page, setPage] = useState(0)

  const { data, isLoading } = useAdminQuizzes({ keyword: keyword || undefined, categoryId, page, size: 20 })
  const { data: categories } = useAdminCategories()
  const hide = useHideQuiz()

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Kiểm duyệt quiz"
        description="Quiz đang công khai trên trang khám phá. Quiz riêng tư không hiện ở đây."
      />

      <Alert
        type="info"
        showIcon
        message="Ẩn quiz đưa nó về riêng tư, không xoá"
        description="Chủ quiz vẫn giữ nội dung, sửa lại rồi công khai lại được. Lượt làm bài và bảng xếp hạng cũ giữ nguyên."
      />

      <div className="flex flex-wrap gap-2">
        <Input.Search
          allowClear
          placeholder="Tìm theo tên quiz"
          className="max-w-xs"
          onSearch={(value) => {
            setKeyword(value)
            setPage(0)
          }}
        />
        <Select
          allowClear
          placeholder="Mọi danh mục"
          className="min-w-44"
          value={categoryId}
          onChange={(value) => {
            setCategoryId(value)
            setPage(0)
          }}
          options={categories?.map((c) => ({ label: c.name, value: c.id }))}
        />
      </div>

      <Table<QuizSummary>
        rowKey="id"
        loading={isLoading}
        dataSource={data?.content}
        pagination={{
          current: page + 1,
          pageSize: data?.size ?? 20,
          total: data?.totalElements ?? 0,
          showSizeChanger: false,
          onChange: (next) => setPage(next - 1),
        }}
        columns={[
          {
            title: 'Quiz',
            dataIndex: 'title',
            render: (title: string, row) => (
              <div className="min-w-0">
                {/* Mở trang giới thiệu quiz để xem nội dung trước khi quyết định ẩn — không ai nên ẩn một
                    quiz chỉ dựa vào tiêu đề */}
                <Link to={`/quizzes/${row.id}`} className="font-bold">
                  {title}
                </Link>
                <div className="text-ink-soft text-xs">
                  {row.questionCount} câu · {row.categoryName ?? 'Chưa phân loại'}
                  {row.aiGenerated && ' · sinh bằng AI'}
                </div>
              </div>
            ),
          },
          {
            title: 'Chủ quiz',
            dataIndex: 'ownerDisplayName',
            width: 180,
            render: (name: string) => <Text className="text-xs">{name}</Text>,
          },
          {
            title: 'Độ khó',
            dataIndex: 'difficulty',
            width: 110,
            render: (difficulty: QuizSummary['difficulty']) => (
              <Tag color={MAU_DO_KHO[difficulty]}>{TEN_DO_KHO[difficulty]}</Tag>
            ),
          },
          {
            title: 'Tạo lúc',
            dataIndex: 'createdAt',
            width: 120,
            render: (createdAt: string) => (
              <Text className="text-ink-soft text-xs">
                {new Date(createdAt).toLocaleDateString('vi-VN')}
              </Text>
            ),
          },
          {
            title: '',
            width: 110,
            render: (_, row) => (
              <Popconfirm
                title="Ẩn quiz này?"
                description="Quiz sẽ biến khỏi trang khám phá nhưng không bị xoá."
                okText="Ẩn"
                cancelText="Thôi"
                okButtonProps={{ danger: true, loading: hide.isPending }}
                onConfirm={() => hide.mutate(row.id)}
              >
                <Button size="small" danger icon={<EyeInvisibleOutlined />}>
                  Ẩn
                </Button>
              </Popconfirm>
            ),
          },
        ]}
      />
    </div>
  )
}

const TEN_DO_KHO: Record<QuizSummary['difficulty'], string> = {
  EASY: 'Dễ',
  MEDIUM: 'Trung bình',
  HARD: 'Khó',
}

const MAU_DO_KHO: Record<QuizSummary['difficulty'], string> = {
  EASY: 'green',
  MEDIUM: 'gold',
  HARD: 'volcano',
}
