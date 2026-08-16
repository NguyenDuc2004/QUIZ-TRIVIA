import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Alert,
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Skeleton,
  Table,
  Tag,
  Tooltip,
  Typography,
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  PlayCircleOutlined,
  PlusOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import {
  useCards,
  useDeleteCard,
  useGenerateFromWrongAnswers,
  useSaveCard,
} from '../hooks/useFlashcards'
import type { Flashcard, FlashcardBody, FlashcardSource } from '../api/flashcardApi'

const { Text } = Typography

/**
 * Thẻ trong một bộ (features/11, FR-37 và FR-39).
 *
 * Dùng **bảng** chứ không phải lưới card: đây là màn soạn nội dung, người dùng cần nhìn nhiều thẻ một lúc
 * và so hai mặt của chúng — đúng bộ mặt "bảng điều khiển" ở `ui-design-system.md §1`.
 */
export default function DeckDetailPage() {
  const { id: deckId } = useParams<{ id: string }>()
  const { data: cards, isLoading } = useCards(deckId)
  const save = useSaveCard(deckId)
  const remove = useDeleteCard()
  const sinhTuCauSai = useGenerateFromWrongAnswers()

  const [dangSua, setDangSua] = useState<Flashcard | null>(null)
  const [moForm, setMoForm] = useState(false)
  const [form] = Form.useForm<FlashcardBody>()

  const soDenHan = (cards ?? []).filter(
    (c) => c.dueDate && c.dueDate <= new Date().toISOString().slice(0, 10),
  ).length

  const moThem = () => {
    setDangSua(null)
    form.resetFields()
    setMoForm(true)
  }

  const moSua = (card: Flashcard) => {
    setDangSua(card)
    form.setFieldsValue({ front: card.front, back: card.back, hint: card.hint ?? undefined })
    setMoForm(true)
  }

  const luu = async () => {
    const values = await form.validateFields()
    await save.mutateAsync({ id: dangSua?.id, body: values })
    setMoForm(false)
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Thẻ trong bộ"
        description={
          <Link to="/flashcards" className="text-sm">
            ← Về danh sách bộ thẻ
          </Link>
        }
        actions={
          <div className="flex flex-wrap gap-2">
            {soDenHan > 0 && (
              <Link to={`/flashcards/review?deckId=${deckId}`}>
                <Button type="primary" icon={<PlayCircleOutlined />}>
                  Ôn {soDenHan} thẻ
                </Button>
              </Link>
            )}
            <Tooltip title="Lấy những câu bạn đã trả lời sai trong các bài quiz và tạo thẻ ôn lại">
              <Button
                icon={<ThunderboltOutlined />}
                loading={sinhTuCauSai.isPending}
                onClick={() => deckId && sinhTuCauSai.mutate(deckId)}
              >
                Tạo từ câu trả lời sai
              </Button>
            </Tooltip>
            <Button icon={<PlusOutlined />} onClick={moThem}>
              Thêm thẻ
            </Button>
          </div>
        }
      />

      <Alert
        type="info"
        showIcon
        message="Sửa nội dung thẻ không làm mất tiến độ ôn"
        description="Lịch ôn của thẻ giữ nguyên khi bạn sửa chữ. Muốn học lại một thẻ từ đầu thì xoá rồi thêm lại."
      />

      {isLoading ? (
        <Skeleton active paragraph={{ rows: 5 }} />
      ) : (
        <Table<Flashcard>
          rowKey="id"
          dataSource={cards}
          pagination={false}
          locale={{
            emptyText: (
              <EmptyState
                title="Bộ thẻ này chưa có thẻ nào"
                hint="Thêm thẻ tay, hoặc bấm “Tạo từ câu trả lời sai” để hệ thống lấy đúng chỗ bạn còn yếu."
                action={
                  <Button type="primary" icon={<PlusOutlined />} onClick={moThem}>
                    Thêm thẻ đầu tiên
                  </Button>
                }
              />
            ),
          }}
          columns={[
            {
              title: 'Mặt trước',
              dataIndex: 'front',
              render: (front: string, row) => (
                <div className="min-w-0">
                  <Text className="font-bold!">{front}</Text>
                  {row.hint && (
                    <div className="text-ink-soft text-xs">Gợi ý: {row.hint}</div>
                  )}
                </div>
              ),
            },
            {
              title: 'Mặt sau',
              dataIndex: 'back',
              render: (back: string) => (
                <Text className="text-sm whitespace-pre-line">{back}</Text>
              ),
            },
            {
              title: 'Nguồn',
              dataIndex: 'source',
              width: 150,
              render: (source: FlashcardSource) => (
                <Tag color={MAU_NGUON[source]}>{TEN_NGUON[source]}</Tag>
              ),
            },
            {
              title: 'Lịch ôn',
              dataIndex: 'dueDate',
              width: 160,
              // null = chưa từng ôn, khác hẳn "đến hạn hôm nay". Hiện hai trạng thái đó bằng cùng một chữ
              // thì người học không biết thẻ đã vào lịch hay chưa.
              render: (dueDate: string | null, row) =>
                dueDate == null ? (
                  <Text className="text-ink-soft text-xs">chưa vào lịch</Text>
                ) : (
                  <div>
                    <Text className="text-xs">{new Date(dueDate).toLocaleDateString('vi-VN')}</Text>
                    <div className="text-ink-soft text-xs">
                      {row.intervalDays === 0
                        ? 'thẻ mới'
                        : `cách ${row.intervalDays} ngày · đã ôn ${row.totalReviews} lần`}
                    </div>
                  </div>
                ),
            },
            {
              title: '',
              width: 100,
              render: (_, row) => (
                <div className="flex gap-1">
                  <Button size="small" icon={<EditOutlined />} onClick={() => moSua(row)} />
                  <Popconfirm
                    title="Xoá thẻ này?"
                    description="Mất luôn tiến độ ôn của thẻ."
                    okText="Xoá"
                    cancelText="Thôi"
                    okButtonProps={{ danger: true, loading: remove.isPending }}
                    onConfirm={() => remove.mutate(row.id)}
                  >
                    <Button size="small" danger icon={<DeleteOutlined />} />
                  </Popconfirm>
                </div>
              ),
            },
          ]}
        />
      )}

      <Modal
        open={moForm}
        title={dangSua ? 'Sửa thẻ' : 'Thêm thẻ'}
        okText="Lưu"
        cancelText="Huỷ"
        confirmLoading={save.isPending}
        onOk={luu}
        onCancel={() => setMoForm(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item
            name="front"
            label="Mặt trước (câu hỏi)"
            rules={[{ required: true, message: 'Nhập mặt trước' }]}
          >
            <Input.TextArea rows={2} maxLength={2000} showCount />
          </Form.Item>
          <Form.Item
            name="back"
            label="Mặt sau (đáp án)"
            rules={[{ required: true, message: 'Nhập mặt sau' }]}
          >
            <Input.TextArea rows={4} maxLength={4000} showCount />
          </Form.Item>
          <Form.Item name="hint" label="Gợi ý" extra="Hiện khi bạn bí, trước lúc xem đáp án.">
            <Input maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

const TEN_NGUON: Record<FlashcardSource, string> = {
  MANUAL: 'Tự viết',
  AI_GENERATED: 'AI sinh',
  FROM_WRONG_ANSWER: 'Từ câu sai',
}

const MAU_NGUON: Record<FlashcardSource, string> = {
  MANUAL: 'default',
  AI_GENERATED: 'purple',
  FROM_WRONG_ANSWER: 'volcano',
}
