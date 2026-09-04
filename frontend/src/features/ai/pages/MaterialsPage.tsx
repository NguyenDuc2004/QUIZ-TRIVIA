import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
  Upload,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
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

const STATUS_LABEL: Record<MaterialStatus, string> = {
  PROCESSING: 'Đang xử lý',
  READY: 'Sẵn sàng',
  FAILED: 'Lỗi',
}

const STATUS_COLOR: Record<MaterialStatus, string> = {
  PROCESSING: 'processing',
  READY: 'green',
  FAILED: 'red',
}

/** Kho học liệu cho RAG — bộ mặt bảng điều khiển (docs/ui-design-system.md §1). */
export default function MaterialsPage() {
  const [page, setPage] = useState(0)
  const [pasteOpen, setPasteOpen] = useState(false)

  const { data: aiStatus } = useAiStatus()
  const { data, isFetching } = useMaterials({ page, size: 10 })
  const uploadMaterial = useUploadMaterial()
  const deleteMaterial = useDeleteMaterial()
  const setShared = useSetMaterialShared()

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
      render: (value: string) => <Tag className="mr-0!">{value === 'TEXT' ? 'Dán tay' : value}</Tag>,
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 190,
      render: (value: MaterialStatus, row) => (
        <Space direction="vertical" size={0}>
          <Tag color={STATUS_COLOR[value]} className="mr-0!">
            {STATUS_LABEL[value]}
          </Tag>
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
    {
      title: '',
      key: 'actions',
      width: 80,
      render: (_, row) => (
        <Popconfirm
          title="Xoá học liệu này?"
          description="Toàn bộ vector của tài liệu cũng bị xoá."
          okText="Xoá"
          cancelText="Hủy"
          okButtonProps={{ danger: true }}
          onConfirm={() => deleteMaterial.mutate(row.id)}
        >
          <Button type="link" size="small" danger>
            Xoá
          </Button>
        </Popconfirm>
      ),
    },
  ]

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Học liệu"
        description="Tài liệu bạn nạp vào để AI sinh câu hỏi bám theo nội dung, thay vì bịa."
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

      <div className="border border-line bg-surface">
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

      <div className="border border-line bg-surface p-5">
        <Text className="font-bold!">Tiếp theo</Text>
        <Paragraph className="mt-2! mb-3! text-ink-soft">
          Khi tài liệu đã ở trạng thái <b>Sẵn sàng</b>, sang trang Sinh đề bằng AI để tạo câu hỏi
          bám theo nội dung của nó.
        </Paragraph>
        <Link to="/ai/generate">
          <Button type="primary">Sinh đề bằng AI</Button>
        </Link>
      </div>

      <PasteMaterialModal open={pasteOpen} onClose={() => setPasteOpen(false)} />
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
