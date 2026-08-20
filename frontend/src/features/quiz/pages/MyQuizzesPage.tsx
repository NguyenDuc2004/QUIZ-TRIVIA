import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Input, Popconfirm, Select, Space, Table, Tag, Tooltip, Upload, message } from 'antd'
import { UploadOutlined } from '@ant-design/icons'
import { useQueryClient } from '@tanstack/react-query'
import { getApiErrorMessage } from '@/shared/api/client'
import type { ColumnsType } from 'antd/es/table'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import { quizApi, type Difficulty, type QuizSummary, type Visibility } from '../api/quizApi'
import { DIFFICULTY_COLOR, DIFFICULTY_LABEL, DIFFICULTY_OPTIONS, VISIBILITY_LABEL } from '../constants'
import { useCategories, useDeleteQuiz, useQuizList } from '../hooks/useQuizQueries'
import QuizFormModal from '../components/QuizFormModal'

/**
 * Trang "Quiz của tôi" — bộ mặt **bảng điều khiển**: bảng dày thông tin, nút viền mảnh
 * (docs/ui-design-system.md §1). Hiển thị cả quiz riêng tư.
 */
export default function MyQuizzesPage() {
  const [page, setPage] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [categoryId, setCategoryId] = useState<string | undefined>()
  const [difficulty, setDifficulty] = useState<Difficulty | undefined>()
  const [editing, setEditing] = useState<QuizSummary | null>(null)
  const [creating, setCreating] = useState(false)

  const { data: categories } = useCategories()
  const { data, isFetching } = useQuizList({
    mine: true,
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
      render: (title: string, row) => (
        <div className="flex items-center gap-3">
          {/* Ảnh nhỏ để nhận ra quiz nhanh; quiz chưa có ảnh thì để ô trống cùng kích thước cho bảng khỏi so le */}
          {row.thumbnailUrl ? (
            <img
              src={row.thumbnailUrl}
              alt=""
              loading="lazy"
              className="h-10 w-16 shrink-0 border border-line object-cover"
            />
          ) : (
            <div className="h-10 w-16 shrink-0 border border-dashed border-line bg-surface-subtle" />
          )}
          <Link to={`/my-quizzes/${row.id}`} className="font-bold">
            {title}
          </Link>
        </div>
      ),
    },
    {
      title: 'Danh mục',
      dataIndex: 'categoryName',
      width: 140,
      render: (value: string | null) => value ?? <span className="text-ink-soft">—</span>,
    },
    {
      title: 'Độ khó',
      dataIndex: 'difficulty',
      width: 120,
      render: (value: Difficulty) => (
        <Tag color={DIFFICULTY_COLOR[value]}>{DIFFICULTY_LABEL[value]}</Tag>
      ),
    },
    { title: 'Số câu', dataIndex: 'questionCount', width: 90, align: 'center' },
    {
      title: 'Hiển thị',
      dataIndex: 'visibility',
      width: 120,
      render: (value: Visibility) => (
        <Tag color={value === 'PUBLIC' ? 'purple' : undefined}>{VISIBILITY_LABEL[value]}</Tag>
      ),
    },
    {
      title: 'Thời gian',
      dataIndex: 'timeLimitSec',
      width: 130,
      render: (value: number | null) =>
        value ? `${Math.round(value / 60)} phút` : <span className="text-ink-soft">Không giới hạn</span>,
    },
    {
      title: '',
      key: 'actions',
      width: 260,
      render: (_, row) => (
        <Space size="small">
          <Link to={`/my-quizzes/${row.id}`} className="text-sm font-bold">
            Soạn câu hỏi
          </Link>
          {/* Chủ quiz làm được bài trên quiz của mình, kể cả quiz riêng tư — dùng để tự kiểm đề */}
          {row.questionCount > 0 && (
            <>
              <Link to={`/quizzes/${row.id}`} className="text-sm font-bold">
                Làm thử
              </Link>
              {/* Cửa vào thống kê VÀ vào việc chấm tay câu tự luận (features/09) */}
              <Link to={`/my-quizzes/${row.id}/stats`} className="text-sm font-bold">
                Thống kê
              </Link>
            </>
          )}
          <Tooltip title="Tải file JSON để sao lưu hoặc chia sẻ đề">
            <Button
              type="link"
              size="small"
              className="px-0!"
              loading={dangXuat === row.id}
              onClick={() => void xuatQuiz(row)}
            >
              Xuất
            </Button>
          </Tooltip>
          <Button type="link" size="small" onClick={() => setEditing(row)}>
            Sửa
          </Button>
          <Popconfirm
            title="Xóa quiz này?"
            description="Câu hỏi vẫn còn trong ngân hàng."
            okText="Xóa"
            cancelText="Hủy"
            okButtonProps={{ danger: true }}
            onConfirm={() => deleteQuiz.mutate(row.id)}
          >
            <Button type="link" size="small" danger>
              Xóa
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const queryClient = useQueryClient()
  const [dangXuat, setDangXuat] = useState<string | null>(null)
  const [dangNhap, setDangNhap] = useState(false)

  /** Tải file JSON của một quiz (FR-12). Phải qua axios để mang header Authorization. */
  const xuatQuiz = async (row: QuizSummary) => {
    setDangXuat(row.id)
    try {
      const file = await quizApi.exportQuiz(row.id)
      const blob = new Blob([JSON.stringify(file, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `quiz-${row.title}.json`
      a.click()
      URL.revokeObjectURL(url)
    } catch (error) {
      message.error(getApiErrorMessage(error))
    } finally {
      setDangXuat(null)
    }
  }

  /**
   * Đọc file ở client rồi gửi lên dạng JSON (FR-12).
   *
   * Bắt lỗi JSON hỏng RIÊNG với lỗi server: hai thứ này người dùng xử lý khác hẳn nhau — file hỏng thì
   * họ phải chọn file khác, còn server từ chối thì nội dung file có vấn đề.
   */
  const nhapQuiz = async (file: File) => {
    setDangNhap(true)
    try {
      const noiDung = await file.text()
      let duLieu
      try {
        duLieu = JSON.parse(noiDung)
      } catch {
        message.error('File không phải JSON hợp lệ')
        return
      }
      const moi = await quizApi.importQuiz(duLieu)
      message.success(`Đã nhập "${moi.title}" với ${moi.questionCount} câu. Quiz đang ở chế độ riêng tư.`)
      await queryClient.invalidateQueries({ queryKey: ['quizzes'] })
    } catch (error) {
      message.error(getApiErrorMessage(error))
    } finally {
      setDangNhap(false)
    }
  }

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Quiz của tôi"
        description="Quiz bạn tạo, gồm cả quiz đang ở chế độ riêng tư."
        actions={
          <>
            <Upload
              accept=".json"
              showUploadList={false}
              // beforeUpload trả false = KHÔNG tự gửi lên server. File JSON phải đọc ở client rồi POST
              // thành body JSON; để Upload tự gửi thì nó gói vào multipart và endpoint không nhận được.
              beforeUpload={(file) => {
                void nhapQuiz(file)
                return false
              }}
            >
              <Button icon={<UploadOutlined />} loading={dangNhap}>
                Nhập từ file
              </Button>
            </Upload>
            <Button type="primary" onClick={() => setCreating(true)}>
              Tạo quiz
            </Button>
          </>
        }
      />

      <div className="border border-line bg-white">
        <div className="flex flex-wrap gap-2 border-b border-line p-3">
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
        </div>

        <Table<QuizSummary>
          rowKey="id"
          size="middle"
          loading={isFetching}
          columns={columns}
          dataSource={data?.content ?? []}
          locale={{
            emptyText: (
              <EmptyState
                title="Bạn chưa tạo quiz nào"
                hint="Tạo quiz rồi thêm câu hỏi từ ngân hàng câu hỏi."
                action={
                  <Button type="primary" onClick={() => setCreating(true)}>
                    Tạo quiz đầu tiên
                  </Button>
                }
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
