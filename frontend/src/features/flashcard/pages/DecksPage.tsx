import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Badge,
  Button,
  Card,
  Col,
  Form,
  Input,
  Modal,
  Popconfirm,
  Row,
  Skeleton,
  Statistic,
  Tag,
  Typography,
} from 'antd'
import { DeleteOutlined, EditOutlined, PlayCircleOutlined, PlusOutlined } from '@ant-design/icons'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import { useDecks, useDeleteDeck, useReviewStats, useSaveDeck } from '../hooks/useFlashcards'
import type { Deck, DeckBody } from '../api/flashcardApi'

const { Text, Paragraph } = Typography

/**
 * Bộ thẻ của tôi (features/11, FR-37 và FR-42).
 *
 * Dùng **lưới card** theo `ui-design-system.md §1`: đây là trang của người học, không phải trang quản lý.
 * Mỗi card nói ngay số thẻ đến hạn hôm nay — đó là câu hỏi duy nhất người học mở trang này để trả lời.
 */
export default function DecksPage() {
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(0)
  const { data, isLoading } = useDecks({ keyword: keyword || undefined, page, size: 12 })
  const { data: stats } = useReviewStats()
  const save = useSaveDeck()
  const remove = useDeleteDeck()

  const [dangSua, setDangSua] = useState<Deck | null>(null)
  const [moForm, setMoForm] = useState(false)
  const [form] = Form.useForm<DeckBody>()

  const moThem = () => {
    setDangSua(null)
    form.resetFields()
    setMoForm(true)
  }

  const moSua = (deck: Deck) => {
    setDangSua(deck)
    form.setFieldsValue({
      title: deck.title,
      description: deck.description ?? undefined,
      topic: deck.topic ?? undefined,
    })
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
        title="Thẻ ghi nhớ"
        description="Ôn theo lịch lặp lại ngắt quãng: thẻ nhớ tốt sẽ giãn dần, thẻ hay quên quay lại sớm."
        actions={
          <div className="flex gap-2">
            {(stats?.soDenHanHomNay ?? 0) > 0 && (
              <Link to="/flashcards/review">
                <Button type="primary" icon={<PlayCircleOutlined />}>
                  Ôn {stats?.soDenHanHomNay} thẻ đến hạn
                </Button>
              </Link>
            )}
            <Button icon={<PlusOutlined />} onClick={moThem}>
              Tạo bộ thẻ
            </Button>
          </div>
        }
      />

      {stats && stats.tongSoThe > 0 && (
        <Row gutter={[16, 16]}>
          <Col xs={12} md={8}>
            <Card>
              <Statistic title="Tổng số thẻ" value={stats.tongSoThe} />
            </Card>
          </Col>
          <Col xs={12} md={8}>
            <Card>
              <Statistic title="Đến hạn hôm nay" value={stats.soDenHanHomNay} />
            </Card>
          </Col>
          <Col xs={24} md={8}>
            <Card>
              <Statistic title="Đã thuộc" value={stats.soDaThuoc} />
              {/* Nói rõ ngưỡng: 21 ngày là quy ước của SM-2, không phải một phép đo mức độ ghi nhớ.
                  Ghi trơ "đã thuộc" là ngụ ý một phép đo không tồn tại. */}
              <Text className="text-ink-soft text-xs">khoảng ôn đã giãn tới 21 ngày trở lên</Text>
            </Card>
          </Col>
        </Row>
      )}

      <Input.Search
        allowClear
        placeholder="Tìm bộ thẻ theo tên"
        className="max-w-sm"
        onSearch={(value) => {
          setKeyword(value)
          setPage(0)
        }}
      />

      {isLoading ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : !data || data.content.length === 0 ? (
        <EmptyState
          title={keyword ? 'Không có bộ thẻ nào khớp từ khoá' : 'Bạn chưa có bộ thẻ nào'}
          hint={
            keyword
              ? undefined
              : 'Tạo một bộ thẻ, rồi tự thêm thẻ hoặc sinh thẻ từ những câu bạn đã trả lời sai.'
          }
          action={
            keyword ? undefined : (
              <Button type="primary" icon={<PlusOutlined />} onClick={moThem}>
                Tạo bộ thẻ đầu tiên
              </Button>
            )
          }
        />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            {data.content.map((deck) => (
              <Col key={deck.id} xs={24} sm={12} lg={8}>
                <Card
                  className="h-full"
                  title={
                    <Link to={`/flashcards/decks/${deck.id}`} className="font-bold">
                      {deck.title}
                    </Link>
                  }
                  extra={
                    deck.soDenHan > 0 ? (
                      <Badge count={deck.soDenHan} overflowCount={99} />
                    ) : undefined
                  }
                  actions={[
                    <Link key="hoc" to={`/flashcards/review?deckId=${deck.id}`}>
                      <Button
                        type="text"
                        size="small"
                        icon={<PlayCircleOutlined />}
                        disabled={deck.soDenHan === 0}
                      >
                        {deck.soDenHan > 0 ? `Ôn ${deck.soDenHan} thẻ` : 'Không có thẻ đến hạn'}
                      </Button>
                    </Link>,
                    <Button
                      key="sua"
                      type="text"
                      size="small"
                      icon={<EditOutlined />}
                      onClick={() => moSua(deck)}
                    >
                      Sửa
                    </Button>,
                    <Popconfirm
                      key="xoa"
                      title="Xoá bộ thẻ này?"
                      description={
                        deck.soThe > 0
                          ? `Xoá luôn ${deck.soThe} thẻ và toàn bộ tiến độ ôn. Không hoàn lại được.`
                          : 'Bộ thẻ chưa có thẻ nào.'
                      }
                      okText="Xoá"
                      cancelText="Thôi"
                      okButtonProps={{ danger: true, loading: remove.isPending }}
                      onConfirm={() => remove.mutate(deck.id)}
                    >
                      <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                    </Popconfirm>,
                  ]}
                >
                  <div className="flex flex-col gap-2">
                    {deck.topic && <Tag className="w-fit">{deck.topic}</Tag>}
                    {deck.description && (
                      <Paragraph className="text-ink-soft mb-0! text-sm" ellipsis={{ rows: 2 }}>
                        {deck.description}
                      </Paragraph>
                    )}
                    <Text className="text-ink-soft text-xs">
                      {deck.soThe} thẻ
                      {deck.soDenHan > 0 && ` · ${deck.soDenHan} đến hạn hôm nay`}
                    </Text>
                  </div>
                </Card>
              </Col>
            ))}
          </Row>

          {data.totalPages > 1 && (
            <div className="flex justify-center gap-2">
              <Button disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                Trang trước
              </Button>
              <Button disabled={data.last} onClick={() => setPage((p) => p + 1)}>
                Trang sau
              </Button>
            </div>
          )}
        </>
      )}

      <Modal
        open={moForm}
        title={dangSua ? `Sửa "${dangSua.title}"` : 'Tạo bộ thẻ'}
        okText="Lưu"
        cancelText="Huỷ"
        confirmLoading={save.isPending}
        onOk={luu}
        onCancel={() => setMoForm(false)}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" requiredMark={false}>
          <Form.Item
            name="title"
            label="Tên bộ thẻ"
            rules={[{ required: true, message: 'Nhập tên bộ thẻ' }]}
          >
            <Input placeholder="Ví dụ: Từ vựng tiếng Anh — Unit 3" />
          </Form.Item>
          <Form.Item name="topic" label="Chủ đề">
            <Input placeholder="Ví dụ: Tiếng Anh" />
          </Form.Item>
          <Form.Item name="description" label="Mô tả">
            <Input.TextArea rows={3} maxLength={2000} showCount />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
