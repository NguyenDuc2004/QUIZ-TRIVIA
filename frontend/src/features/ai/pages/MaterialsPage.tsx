import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Space,
  Switch,
  Table,
  Typography,
  Upload,
} from 'antd'
import { DeleteOutlined, EditOutlined, FileTextOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import { useAuthStore } from '@/features/auth/store/authStore'
import Pill from '@/shared/components/Pill'
import RowActions from '@/shared/components/RowActions'
import type { Material, MaterialStatus } from '../api/aiApi'
import {
  useAiStatus,
  useCreateMaterial,
  useDeleteMaterial,
  useMaterials,
  useSetMaterialShared,
  useUploadMaterial,
} from '../hooks/useAiQueries'

const { Text, Paragraph } = Typography
const { TextArea } = Input

/**
 * Trần số học liệu của một người học — bản sao để hiển thị của `MaterialService.MAX_MATERIALS_PER_LEARNER`.
 *
 * Con số nằm ở hai nơi, và đó là đánh đổi có ý thức: **backend là nơi cưỡng chế**, thông báo lỗi của nó
 * mang con số thật. Bản ở đây chỉ để nói trước cho người dùng biết trần là bao nhiêu — không nói trước thì
 * họ nạp tới tài liệu thứ 11 mới biết là có giới hạn. Hai bên lệch nhau thì hậu quả là một dòng mô tả sai,
 * không phải một lỗ hổng: người dùng vẫn bị chặn đúng chỗ backend chặn.
 */
const MAX_HOC_LIEU_NGUOI_HOC = 10

const STATUS_LABEL: Record<MaterialStatus, string> = {
  PROCESSING: 'Đang xử lý',
  READY: 'Sẵn sàng',
  FAILED: 'Lỗi',
}

/**
 * Tông viên thuốc và chấm màu cho trạng thái xử lý học liệu.
 *
 * Trạng thái này là một **tiến trình** (đang xử lý → xong / hỏng), nên nó dùng chấm màu giống độ khó:
 * ba chấm xanh dương–xanh lá–đỏ đọc được thành tiến trình ngay, còn ba biểu tượng khác nhau thì không.
 */
const STATUS_PILL: Record<MaterialStatus, 'xanhDuong' | 'xanhLa' | 'do'> = {
  PROCESSING: 'xanhDuong',
  READY: 'xanhLa',
  FAILED: 'do',
}

const STATUS_DOT: Record<MaterialStatus, string> = {
  PROCESSING: '#3b82f6',
  READY: '#22c55e',
  FAILED: '#ef4444',
}

/** Kho học liệu cho RAG — bộ mặt bảng điều khiển (docs/ui-design-system.md §1). */
export default function MaterialsPage() {
  const [page, setPage] = useState(0)
  const [pasteOpen, setPasteOpen] = useState(false)
  /* Cùng lý do với hai bảng kia: `Popconfirm` bám vào phần tử kích hoạt, mà nó giờ là mục trong menu —
     menu đóng ngay khi bấm nên hộp xác nhận mất điểm neo. */
  const [xoaHocLieu, setXoaHocLieu] = useState<Material | null>(null)

  const { data: aiStatus } = useAiStatus()
  const { data, isFetching } = useMaterials({ page, size: 10 })
  const uploadMaterial = useUploadMaterial()
  const deleteMaterial = useDeleteMaterial()
  const setShared = useSetMaterialShared()

  // Người học nạp được tài liệu của CHÍNH họ, nhưng không bật được chia sẻ: bật `shared` là đẩy tài liệu
  // vào trợ lý của mọi người học khác — hành vi xuất bản, và một bề mặt kiểm duyệt. Backend chặn ở
  // `MaterialController.setMaterialShared`; ẩn cột ở đây để họ không nhìn thấy một công tắc mà bấm vào
  // chỉ nhận về lỗi 403.
  const role = useAuthStore((state) => state.user?.role)
  const canShare = role === 'CREATOR' || role === 'ADMIN'

  /**
   * Cột chia sẻ — chỉ dựng cho CREATOR/ADMIN.
   *
   * Phải nằm TRONG hàm: nó dùng `setShared`, là mutation của component. Tách khỏi mảng `columns` thay vì
   * chèn thẳng một biểu thức điều kiện vào giữa: cột này dài hơn 25 dòng, nhét vào giữa mảng thì hai cột
   * đứng cạnh nó bị đẩy thụt vào một mức khác hẳn và mảng đọc không ra hình dạng nữa.
   */
  const cotChiaSe: ColumnsType<Material> = [
      {
        title: 'Chia sẻ',
        key: 'shared',
        width: 210,
        render: (_, row) => (
          <Space direction="vertical" size={0}>
            <Switch
              size="small"
              checked={row.shared}
              // Chỉ bật được khi đã xử lý xong: tài liệu chưa có vector thì chia sẻ ra cũng không ai
              // truy xuất được, và một công tắc bật rồi mà vô tác dụng thì tệ hơn là không cho bật
              disabled={row.status !== 'READY' || setShared.isPending}
              onChange={(shared) => setShared.mutate({ id: row.id, shared })}
            />
            <Text className="text-ink-soft text-xs">
              {row.status !== 'READY'
                ? 'Xử lý xong mới chia sẻ được'
                : row.shared
                  ? 'Người học hỏi được trên tài liệu này'
                  : 'Chỉ bạn dùng được'}
            </Text>
          </Space>
        ),
      },
  ]

  const columns: ColumnsType<Material> = [
    {
      title: 'Tài liệu',
      dataIndex: 'title',
      render: (title: string, row) => (
        <Space direction="vertical" size={0}>
          <Text className="font-bold!">{title}</Text>
          <Text className="text-ink-soft text-xs">
            {row.topic ? `${row.topic} · ` : ''}
            {new Date(row.createdAt).toLocaleString('vi-VN')}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Nguồn',
      dataIndex: 'sourceType',
      width: 90,
      render: (value: string) => (
        <Pill icon={value === 'TEXT' ? <EditOutlined /> : <FileTextOutlined />}>
          {value === 'TEXT' ? 'Dán tay' : value}
        </Pill>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 190,
      render: (value: MaterialStatus, row) => (
        <Space direction="vertical" size={0}>
          <Pill mau={STATUS_PILL[value]} chamMau={STATUS_DOT[value]}>
            {STATUS_LABEL[value]}
          </Pill>
          {row.errorMessage && (
            <Text className="text-xs text-urgent">{row.errorMessage}</Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Đã xử lý',
      key: 'size',
      width: 150,
      render: (_, row) =>
        row.status === 'READY' ? (
          <Text className="text-xs">
            {row.charCount.toLocaleString('vi-VN')} ký tự · {row.chunkCount} đoạn
          </Text>
        ) : (
          <Text className="text-ink-soft text-xs">—</Text>
        ),
    },
    ...(canShare ? cotChiaSe : []),
    {
      title: '',
      key: 'actions',
      width: 70,
      align: 'right',
      render: (_, row) => (
        /* Bảng này chỉ có MỘT thao tác, và nó là thao tác xoá. Không có hành động chính nào để hiện
           ra ngoài, nên menu ba chấm đứng một mình — vẫn đúng ý đồ: việc không hoàn tác được thì
           không nằm sẵn dưới con trỏ. */
        <RowActions
          items={[
            {
              key: 'xoa',
              icon: <DeleteOutlined />,
              label: 'Xoá học liệu',
              danger: true,
              onClick: () => setXoaHocLieu(row),
            },
          ]}
        />
      ),
    },
  ]

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title={canShare ? 'Học liệu' : 'Học liệu của tôi'}
        description={
          canShare
            ? 'Tài liệu bạn nạp vào để AI sinh câu hỏi bám theo nội dung, thay vì bịa.'
            : `Tài liệu bạn nạp lên để hỏi trợ lý. Chỉ mình bạn hỏi được trên chúng, tối đa ${MAX_HOC_LIEU_NGUOI_HOC} tài liệu.`
        }
        actions={
          <>
            <Button onClick={() => setPasteOpen(true)}>Dán văn bản</Button>
            <Upload
              accept=".pdf,.docx,.doc,.txt"
              showUploadList={false}
              beforeUpload={(file) => {
                uploadMaterial.mutate({ file })
                return false
              }}
            >
              <Button type="primary" loading={uploadMaterial.isPending}>
                Tải tài liệu lên
              </Button>
            </Upload>
          </>
        }
      />

      {aiStatus && !aiStatus.available && (
        <Alert
          type="warning"
          showIcon
          message="Chưa cấu hình API key cho dịch vụ AI"
          description="Thêm GEMINI_API_KEY vào file .env rồi khởi động lại backend. Trước đó, tài liệu tải lên sẽ dừng ở trạng thái Lỗi."
        />
      )}

      <div className="soft-panel">
        <Table<Material>
          scroll={{ x: 'max-content' }}
          rowKey="id"
          size="middle"
          loading={isFetching}
          columns={columns}
          dataSource={data?.content ?? []}
          locale={{
            emptyText: (
              <EmptyState
                title="Chưa có học liệu nào"
                hint="Tải lên PDF/DOCX/TXT hoặc dán thẳng văn bản. AI sẽ dựa vào đây để ra đề."
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

      {/* Bước tiếp theo KHÁC NHAU theo vai trò, và người học không bị bỏ trống.

          Trước đây khối này luôn dẫn sang "Sinh đề bằng AI" — một trang mà route chặn ở CREATOR/ADMIN
          và backend trả 403. Người học nạp tài liệu xong được mời làm đúng một việc họ không có
          quyền làm.

          Giấu hẳn khối đi thì họ hết bị mời sai, nhưng cũng hết luôn lối đi tiếp — trong khi họ nạp
          tài liệu CHÍNH LÀ để hỏi trợ lý. Nên đổi đích, không xoá khối. */}
      <div className="soft-panel p-5">
        <Text className="font-bold!">Tiếp theo</Text>
        {canShare ? (
          <>
            <Paragraph className="mt-2! mb-3! text-ink-soft">
              Khi tài liệu đã ở trạng thái <b>Sẵn sàng</b>, sang trang Sinh đề bằng AI để tạo câu hỏi
              bám theo nội dung của nó.
            </Paragraph>
            <Link to="/ai/generate">
              <Button type="primary">Sinh đề bằng AI</Button>
            </Link>
          </>
        ) : (
          <>
            <Paragraph className="mt-2! mb-3! text-ink-soft">
              Khi tài liệu đã ở trạng thái <b>Sẵn sàng</b>, sang Trợ lý học tập để hỏi trên nội dung
              của nó. Trợ lý trả lời kèm đoạn đã dựa vào để bạn đối chiếu lại.
            </Paragraph>
            <Link to="/assistant">
              <Button type="primary">Hỏi trợ lý học tập</Button>
            </Link>
          </>
        )}
      </div>

      <PasteMaterialModal open={pasteOpen} onClose={() => setPasteOpen(false)} />

      {/* Nói rõ hệ quả: xoá học liệu là xoá luôn vector, tức trợ lý mất khả năng trả lời trên tài
          liệu đó. Người dùng không nhìn thấy vector nên phải nói bằng lời. */}
      <Modal
        open={xoaHocLieu !== null}
        title={`Xoá học liệu “${xoaHocLieu?.title ?? ''}”?`}
        okText="Xoá"
        cancelText="Hủy"
        okButtonProps={{ danger: true, loading: deleteMaterial.isPending }}
        onCancel={() => setXoaHocLieu(null)}
        onOk={() => {
          if (xoaHocLieu) {
            deleteMaterial.mutate(xoaHocLieu.id, { onSuccess: () => setXoaHocLieu(null) })
          }
        }}
      >
        Toàn bộ vector của tài liệu cũng bị xoá — trợ lý học tập sẽ không trả lời được trên tài liệu này
        nữa.
      </Modal>
    </Space>
  )
}

/** Nạp học liệu bằng cách dán thẳng văn bản — nhanh hơn với ghi chú ngắn. */
function PasteMaterialModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [title, setTitle] = useState('')
  const [topic, setTopic] = useState('')
  const [content, setContent] = useState('')
  const createMaterial = useCreateMaterial()

  const reset = () => {
    setTitle('')
    setTopic('')
    setContent('')
  }

  return (
    <Modal
      open={open}
      title="Dán văn bản làm học liệu"
      okText="Nạp"
      cancelText="Hủy"
      confirmLoading={createMaterial.isPending}
      okButtonProps={{ disabled: !title.trim() || content.trim().length < 100 }}
      onOk={async () => {
        await createMaterial.mutateAsync({ title, topic: topic || undefined, content })
        reset()
        onClose()
      }}
      onCancel={() => {
        reset()
        onClose()
      }}
      width={720}
      destroyOnHidden
    >
      <Form layout="vertical" className="mt-4!">
        <Form.Item label="Tiêu đề">
          <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Ví dụ: Chương 3 — Giao thức HTTP" />
        </Form.Item>
        <Form.Item label="Chủ đề" help="Không bắt buộc, giúp tìm lại nhanh hơn">
          <Input value={topic} onChange={(e) => setTopic(e.target.value)} placeholder="Lập trình web" />
        </Form.Item>
        <Form.Item
          label="Nội dung"
          help={`Cần ít nhất 100 ký tự — hiện có ${content.trim().length}`}
        >
          <TextArea rows={10} value={content} onChange={(e) => setContent(e.target.value)} />
        </Form.Item>
      </Form>
    </Modal>
  )
}
