import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button, Input, Modal, Select, Space, Table, Upload, message } from 'antd'
import {
  BarChartOutlined,
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  GlobalOutlined,
  LockOutlined,
  PlayCircleOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import { useQueryClient } from '@tanstack/react-query'
import { getApiErrorMessage } from '@/shared/api/client'
import type { ColumnsType } from 'antd/es/table'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import QuizCover from '../components/QuizCover'
import Pill from '@/shared/components/Pill'
import RowActions from '@/shared/components/RowActions'
import { quizApi, type Difficulty, type QuizSummary, type Visibility } from '../api/quizApi'
import {
  DIFFICULTY_DOT,
  DIFFICULTY_LABEL,
  DIFFICULTY_OPTIONS,
  DIFFICULTY_PILL,
  VISIBILITY_LABEL,
} from '../constants'
import { useCategories, useDeleteQuiz, useQuizList } from '../hooks/useQuizQueries'
import QuizFormModal from '../components/QuizFormModal'

/**
 * Trang "Quiz của tôi" — bộ mặt **bảng điều khiển**: bảng dày thông tin, nút viền mảnh
 * (docs/ui-design-system.md §1). Hiển thị cả quiz riêng tư.
 */
export default function MyQuizzesPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  /* Xác nhận xóa chuyển từ `Popconfirm` sang `Modal`.

     `Popconfirm` bám vào phần tử kích hoạt, mà phần tử đó giờ là một mục trong menu — menu đóng lại
     ngay khi bấm, nên hộp xác nhận mất luôn điểm neo và không hiện. */
  const [xoaQuiz, setXoaQuiz] = useState<QuizSummary | null>(null)
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
          {/* Cùng khuôn `QuizCover` với lưới Khám phá, chỉ khác bề ngang.

              Trước đây chỗ này tự dựng: ảnh 64×40 (tỉ lệ 16:10, không khớp 16:9 ở mọi nơi khác), và
              quiz chưa có ảnh thì vẽ một ô viền đứt trống trơn — tức cùng một quiz trông khác hẳn ở
              hai trang. Nay nó nhận đúng khối gradient và biểu tượng của chính nó, thu nhỏ lại. */}
          <div className="w-16 shrink-0">
            <QuizCover
              thumbnailUrl={row.thumbnailUrl}
              categoryName={row.categoryName}
              title={title}
              coIcon="nho"
              className="border border-line rounded-card"
            />
          </div>
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
        <Pill mau={DIFFICULTY_PILL[value]} chamMau={DIFFICULTY_DOT[value]}>
          {DIFFICULTY_LABEL[value]}
        </Pill>
      ),
    },
    { title: 'Số câu', dataIndex: 'questionCount', width: 90, align: 'center' },
    {
      title: 'Hiển thị',
      dataIndex: 'visibility',
      width: 120,
      render: (value: Visibility) => (
        <Pill
          mau={value === 'PUBLIC' ? 'xanhDuong' : 'trungTinh'}
          icon={value === 'PUBLIC' ? <GlobalOutlined /> : <LockOutlined />}
        >
          {VISIBILITY_LABEL[value]}
        </Pill>
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
      width: 150,
      align: 'right',
      render: (_, row) => (
        <RowActions
          /* Hành động chính là "Soạn câu hỏi": đó là việc người tạo nội dung mở bảng này để làm.
             Năm hành động còn lại gom vào menu — trong đó có Xóa, thứ trước đây nằm ngang hàng với
             Soạn câu hỏi và lại là chữ đỏ nên hút mắt nhất trong sáu chữ. */
          chinh={
            <Link to={`/my-quizzes/${row.id}`}>
              <Button size="small" icon={<EditOutlined />}>
                Soạn câu hỏi
              </Button>
            </Link>
          }
          items={[
            // Chủ quiz làm được bài trên quiz của mình, kể cả quiz riêng tư — dùng để tự kiểm đề
            ...(row.questionCount > 0
              ? [
                  {
                    key: 'thu',
                    icon: <PlayCircleOutlined />,
                    label: 'Làm thử',
                    onClick: () => navigate(`/quizzes/${row.id}`),
                  },
                  {
                    // Cửa vào thống kê VÀ vào việc chấm tay câu tự luận (features/09)
                    key: 'thongke',
                    icon: <BarChartOutlined />,
                    label: 'Thống kê',
                    onClick: () => navigate(`/my-quizzes/${row.id}/stats`),
                  },
                ]
              : []),
            {
              key: 'xuat',
              icon: <DownloadOutlined />,
              // Chặn bấm lần hai khi đang tải: menu không có chỗ hiện vòng xoay như nút, nên khoá
              // luôn mục đó và đổi chữ là cách duy nhất cho người dùng biết nó đang chạy.
              label: dangXuat === row.id ? 'Đang xuất…' : 'Xuất file JSON',
              disabled: dangXuat === row.id,
              onClick: () => void xuatQuiz(row),
            },
            { key: 'sua', icon: <EditOutlined />, label: 'Sửa thông tin', onClick: () => setEditing(row) },
            { type: 'divider' as const },
            {
              // Nằm sau vạch ngăn và tô đỏ: đây là thao tác không hoàn tác được, nên nó phải tách khỏi
              // nhóm thao tác thường bằng cả khoảng cách lẫn màu.
              key: 'xoa',
              icon: <DeleteOutlined />,
              label: 'Xóa quiz',
              danger: true,
              onClick: () => setXoaQuiz(row),
            },
          ]}
        />
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

      <div className="soft-panel">
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
          scroll={{ x: 'max-content' }}
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

      {/* Hộp xác nhận xóa.

          Vẫn phải xác nhận dù thao tác đã nằm sau một lần bấm trong menu: menu chỉ làm cho việc chạm
          tới nó khó hơn, không làm nó bớt không-hoàn-tác-được. Mô tả nói rõ điều người dùng hay lo
          nhất — câu hỏi KHÔNG mất theo quiz. */}
      <Modal
        open={xoaQuiz !== null}
        title={`Xóa quiz “${xoaQuiz?.title ?? ''}”?`}
        okText="Xóa"
        cancelText="Hủy"
        okButtonProps={{ danger: true, loading: deleteQuiz.isPending }}
        onCancel={() => setXoaQuiz(null)}
        onOk={() => {
          if (xoaQuiz) {
            deleteQuiz.mutate(xoaQuiz.id, { onSuccess: () => setXoaQuiz(null) })
          }
        }}
      >
        Câu hỏi trong quiz này <b>vẫn còn</b> trong ngân hàng câu hỏi — chỉ quiz bị xóa.
      </Modal>
    </Space>
  )
}
