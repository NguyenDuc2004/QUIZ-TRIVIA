import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Alert,
  Button,
  Input,
  Modal,
  Skeleton,
  Space,
  Spin,
  Tooltip,
  Typography,
  Upload,
} from 'antd'
import {
  ArrowUpOutlined,
  BorderOutlined,
  DeleteOutlined,
  MessageOutlined,
  PaperClipOutlined,
  PlusOutlined,
  RobotOutlined,
} from '@ant-design/icons'
import MathText from '@/shared/components/MathText'
import Pill from '@/shared/components/Pill'
import PageHeader from '@/shared/components/PageHeader'
import { useUploadMaterial } from '@/features/ai/hooks/useAiQueries'
import type { AskableMaterial, ChatMessage, ChatSession } from '../api/chatApi'
import {
  mergeMessages,
  useAskAssistant,
  useAskableMaterials,
  useChatMessages,
  useChatSessions,
  useDeleteChatSession,
} from '../hooks/useChat'

const { Text, Paragraph } = Typography

/**
 * Trợ lý học tập RAG (features/08 — FR-31).
 * <p>
 * **Trang này nói thật về căn cứ của câu trả lời.** Mỗi câu trả lời hiện kèm danh sách tài liệu đã
 * dựa vào, và khi không dựa vào tài liệu nào thì nói rõ điều đó thay vì để im. Một trợ lý AI trả lời
 * trôi chảy mà không cho biết lấy từ đâu sẽ được người học tin nhiều hơn mức nó đáng được tin — và
 * đây là ứng dụng ôn thi, tin sai thì học sai.
 */
export default function AssistantPage() {
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const [xoaPhien, setXoaPhien] = useState<ChatSession | null>(null)
  const bottomRef = useRef<HTMLDivElement>(null)

  // null = hỏi trên mọi tài liệu đọc được; có giá trị = giới hạn trong đúng một tài liệu.
  // Hữu ích khi ôn đúng một chương và không muốn câu trả lời lẫn tài liệu khác.
  const [materialId, setMaterialId] = useState<string | null>(null)

  const { data: sessions, isPending: sessionsLoading } = useChatSessions()
  const { data: materials, isPending: materialsLoading } = useAskableMaterials()
  const { data: savedMessages, isPending: messagesLoading } = useChatMessages(sessionId)
  const deleteSession = useDeleteChatSession()
  const uploadMaterial = useUploadMaterial()
  const { ask, stop, streaming, pendingQuestion, isStreaming } = useAskAssistant(
    sessionId,
    setSessionId,
  )

  const selectedMaterial = materials?.find((m) => m.id === materialId) ?? null

  const messages = mergeMessages(savedMessages, pendingQuestion, streaming)

  const goiY = useMemo(() => goiYCauHoi(materials, selectedMaterial), [materials, selectedMaterial])

  // Cuộn xuống theo từng mảnh chữ để câu trả lời đang chạy luôn nằm trong tầm mắt
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages.length, streaming?.text])

  const send = (noiDung?: string) => {
    const question = (noiDung ?? draft).trim()
    if (!question || isStreaming) {
      return
    }
    setDraft('')
    void ask(question, materialId ?? undefined)
  }

  return (
    /* KHÔNG dùng `<Space>` ở đây như các trang khác: `Space` bọc mỗi phần tử con vào một
       `.ant-space-item` riêng, nên `flex-1` đặt trên lưới bên dưới sẽ áp vào cái bọc đó chứ không
       tới được lưới — khối chat không bao giờ nhận được phần chiều cao còn lại. Một `flex` tự viết
       làm đúng việc `Space` làm (xếp dọc, cách nhau 24px) mà giữ được chuỗi chiều cao liền mạch. */
    <div className="chat-trang flex w-full flex-col gap-6">
      <PageHeader
        title="Trợ lý học tập"
        description="Hỏi về nội dung trong học liệu — trợ lý trả lời kèm tài liệu đã dựa vào"
        actions={
          <Button
            disabled={isStreaming}
            onClick={() => {
              setSessionId(null)
              setDraft('')
            }}
          >
            Hội thoại mới
          </Button>
        }
      />

      {/* Cột trái xếp hai khối (hội thoại, học liệu), khung hội thoại chiếm trọn chiều cao bên phải.

          `min-h-0` có mặt ở MỌI mắt xích từ đây xuống tới vùng cuộn. Thiếu nó thì `overflow-y-auto`
          bên dưới không có tác dụng: mặc định `min-height` của một flex/grid item là `auto`, tức nó
          nở đúng bằng nội dung và không bao giờ nhỏ hơn để mà phải cuộn. Đây là chỗ dễ mất cả buổi
          nhất khi dựng bố cục kiểu này, vì CSS không báo gì — nó chỉ lặng lẽ cuộn cả trang. */}
      <div className="grid gap-4 lg:min-h-0 lg:flex-1 lg:grid-cols-[260px_1fr]">
        {/* Cột trái: hai khối xếp dọc, mỗi khối tự cuộn trong lòng nó */}
        <div className="flex flex-col gap-4 lg:min-h-0">
        {/* Danh sách phiên — cao tối đa 40% cột, dài hơn thì cuộn tại chỗ thay vì đẩy khối học liệu
            xuống dưới mép màn hình */}
        <aside className="chat-rail lg:max-h-[40%] lg:overflow-y-auto">
          <div className="px-2 py-2">
            <Text className="text-ink-soft text-xs font-bold">Hội thoại của bạn</Text>
          </div>
          {sessionsLoading ? (
            <div className="p-2">
              <Skeleton active paragraph={{ rows: 3 }} title={false} />
            </div>
          ) : !sessions || sessions.length === 0 ? (
            <Text className="text-ink-soft block px-2 py-6 text-center text-xs">
              Chưa có hội thoại nào
            </Text>
          ) : (
            <ul className="m-0 list-none p-0">
              {sessions.map((session) => (
                <li
                  key={session.id}
                  className="chat-rail-item"
                  data-dang-chon={session.id === sessionId}
                >
                  <MessageOutlined className="text-ink-soft mt-[3px] text-xs" />
                  <button
                    type="button"
                    className="min-w-0 flex-1 cursor-pointer border-0 bg-transparent p-0 text-left"
                    onClick={() => setSessionId(session.id)}
                  >
                    <Text className="line-clamp-2-title block text-xs font-bold">
                      {session.title}
                    </Text>
                    <Text className="text-ink-soft text-[10px]">
                      {new Date(session.updatedAt).toLocaleString('vi-VN')}
                    </Text>
                  </button>
                  {/* Hộp xác nhận là `Modal` chứ không `Popconfirm`: `Popconfirm` bám vào chính nút
                      kích hoạt, mà nút này CHỈ HIỆN KHI RÊ CHUỘT — mở hộp xong là con trỏ rời hàng,
                      nút mờ đi, và hộp xác nhận neo vào một thứ vô hình. */}
                  <Tooltip title="Xoá hội thoại">
                    <Button
                      type="text"
                      size="small"
                      danger
                      className="chat-rail-xoa"
                      icon={<DeleteOutlined />}
                      aria-label={`Xoá hội thoại ${session.title}`}
                      onClick={() => setXoaPhien(session)}
                    />
                  </Tooltip>
                </li>
              ))}
            </ul>
          )}
        </aside>

        {/* Học liệu hỏi được — trước đây người học không có cách nào biết kho có tài liệu gì, họ chỉ
            thấy tên một tài liệu SAU KHI tình cờ hỏi trúng nó qua khối trích dẫn */}
        <aside className="chat-rail lg:min-h-0 lg:flex-1 lg:overflow-y-auto">
          <div className="px-2 py-2">
            <Text className="text-ink-soft text-xs font-bold">Học liệu hỏi được</Text>
          </div>
          {materialsLoading ? (
            <div className="p-2">
              <Skeleton active paragraph={{ rows: 2 }} title={false} />
            </div>
          ) : !materials || materials.length === 0 ? (
            <div className="px-1 pb-1">
              {/* Nút này hiện với MỌI vai trò kể từ 04/09/2026.

                  Trước đó người học không nạp được tài liệu nào, và vì trợ lý chỉ trả lời dựa trên
                  học liệu nên chức năng của họ chết hẳn khi chưa ai bấm nút chia sẻ — một điều kiện
                  họ không tác động được. Nay họ nạp được tài liệu riêng (tối đa 10, backend cưỡng
                  chế ở MaterialService). */}
              <Upload
                accept=".pdf,.docx,.doc,.txt"
                showUploadList={false}
                className="block"
                beforeUpload={(file) => {
                  uploadMaterial.mutate({ file })
                  return false
                }}
              >
                <button type="button" className="chat-them-hoc-lieu">
                  {uploadMaterial.isPending ? <Spin size="small" /> : <PlusOutlined />}
                  <span className="text-xs font-bold">Thêm học liệu</span>
                  <span className="text-[10px]">PDF, DOCX hoặc TXT</span>
                </button>
              </Upload>
            </div>
          ) : (
            <ul className="m-0 list-none p-0">
              <li className="chat-rail-item" data-dang-chon={materialId === null}>
                <button
                  type="button"
                  className="w-full cursor-pointer border-0 bg-transparent p-0 text-left"
                  onClick={() => setMaterialId(null)}
                >
                  <Text className="block text-xs font-bold">Tất cả học liệu</Text>
                  <Text className="text-ink-soft text-[10px]">
                    {materials.length} tài liệu · trợ lý tự chọn đoạn liên quan nhất
                  </Text>
                </button>
              </li>
              {materials.map((material) => (
                <li
                  key={material.id}
                  className="chat-rail-item"
                  data-dang-chon={material.id === materialId}
                >
                  <button
                    type="button"
                    className="w-full cursor-pointer border-0 bg-transparent p-0 text-left"
                    onClick={() => setMaterialId(material.id)}
                  >
                    <Text className="line-clamp-2-title block text-xs font-bold">
                      {material.title}
                    </Text>
                    <div className="mt-1 flex flex-wrap items-center gap-1">
                      {material.topic ? <Pill>{material.topic}</Pill> : null}
                      {/* Nói rõ tài liệu của ai: người học đọc ké tài liệu được chia sẻ, không phải sở hữu */}
                      <Pill mau={material.mine ? 'xanhDuong' : 'xanhLa'}>
                        {material.mine ? 'Của tôi' : 'Được chia sẻ'}
                      </Pill>
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </aside>

        </div>

        {/* Khung hội thoại. `min-h-[60vh]` chỉ còn tác dụng dưới `lg` — nơi bố cục xếp dọc và cả
            trang cuộn như thường; từ `lg` trở lên chiều cao do lưới quyết định. */}
        <section className="flex min-h-[60vh] flex-col lg:col-start-2 lg:min-h-0">
          <div className="min-h-0 flex-1 overflow-y-auto pb-4">
            {sessionId && messagesLoading ? (
              <Skeleton active paragraph={{ rows: 5 }} />
            ) : messages.length === 0 ? (
              <ManHinhChao
                goiY={goiY}
                coHocLieu={Boolean(materials && materials.length > 0)}
                onChon={send}
              />
            ) : (
              <Space direction="vertical" size="large" className="w-full">
                {messages.map((item) => (
                  <MessageBubble
                    key={item.id}
                    item={item}
                    isStreaming={item.id === 'streaming-answer' && isStreaming}
                  />
                ))}
                {streaming?.error && <Alert type="warning" showIcon message={streaming.error} />}
              </Space>
            )}
            <div ref={bottomRef} />
          </div>

          <div>
            {/* Nói rõ phạm vi đang hỏi: người dùng cần biết vì sao trợ lý không thấy nội dung ở tài
                liệu khác, thay vì tưởng trợ lý quên */}
            {selectedMaterial && (
              <div className="mb-2 flex items-center gap-2">
                <Text className="text-ink-soft text-xs">Chỉ hỏi trong:</Text>
                <Pill icon={<MessageOutlined />}>{selectedMaterial.title}</Pill>
                <Button type="link" size="small" onClick={() => setMaterialId(null)}>
                  Bỏ giới hạn
                </Button>
              </div>
            )}
            <div className="chat-composer flex items-end gap-2">
              <Input.TextArea
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder={
                  selectedMaterial
                    ? `Hỏi về "${selectedMaterial.title}"…`
                    : 'Hỏi về nội dung trong học liệu…'
                }
                autoSize={{ minRows: 1, maxRows: 6 }}
                maxLength={2000}
                disabled={isStreaming}
                variant="borderless"
                onPressEnter={(event) => {
                  // Enter gửi, Shift+Enter xuống dòng — quy ước người dùng đã quen ở mọi khung chat
                  if (!event.shiftKey) {
                    event.preventDefault()
                    send()
                  }
                }}
              />

              <Upload
                accept=".pdf,.docx,.doc,.txt"
                showUploadList={false}
                beforeUpload={(file) => {
                  uploadMaterial.mutate({ file })
                  return false
                }}
              >
                <Tooltip title="Nạp thêm tài liệu vào kho — trợ lý dùng được sau khi xử lý xong">
                  <Button
                    type="text"
                    shape="circle"
                    loading={uploadMaterial.isPending}
                    icon={<PaperClipOutlined />}
                    aria-label="Nạp thêm tài liệu"
                  />
                </Tooltip>
              </Upload>

              {isStreaming ? (
                <Tooltip title="Dừng thì phần đã trả lời vẫn được lưu">
                  <Button
                    shape="circle"
                    onClick={stop}
                    icon={<BorderOutlined />}
                    aria-label="Dừng trả lời"
                  />
                </Tooltip>
              ) : (
                <Button
                  type="primary"
                  shape="circle"
                  disabled={!draft.trim()}
                  onClick={() => send()}
                  icon={<ArrowUpOutlined />}
                  aria-label="Gửi câu hỏi"
                />
              )}
            </div>
            <Text className="text-ink-soft mt-2 block text-xs">
              Trợ lý chỉ dựa trên học liệu. Chưa có tài liệu nào phù hợp thì nó sẽ nói thẳng là không
              biết, thay vì đoán. Nạp thêm tài liệu ở <Link to="/ai/materials">trang Học liệu</Link>.
            </Text>
          </div>
        </section>
      </div>

      <Modal
        open={xoaPhien !== null}
        title="Xoá hội thoại này?"
        okText="Xoá"
        cancelText="Hủy"
        okButtonProps={{ danger: true }}
        onCancel={() => setXoaPhien(null)}
        onOk={() => {
          if (!xoaPhien) {
            return
          }
          if (xoaPhien.id === sessionId) {
            setSessionId(null)
          }
          deleteSession.mutate(xoaPhien.id)
          setXoaPhien(null)
        }}
      >
        <Paragraph className="mb-0!">
          Toàn bộ câu hỏi và câu trả lời trong “{xoaPhien?.title}” sẽ bị xoá.
        </Paragraph>
      </Modal>
    </div>
  )
}

/**
 * Màn hình chào khi chưa có tin nhắn nào.
 *
 * ## Vì sao chip gợi ý sinh từ tên học liệu thật, không phải câu mẫu viết sẵn
 * Trợ lý này chỉ trả lời dựa trên học liệu. Một chip viết sẵn kiểu "Giải thích định luật Newton"
 * đặt trước một kho tài liệu về lập trình sẽ dẫn thẳng tới câu "tôi không biết" — người dùng bấm
 * đúng thứ giao diện mời họ bấm rồi nhận thất bại, và họ sẽ kết luận là trợ lý hỏng.
 *
 * Nên chip chỉ được dựng từ tài liệu **đang thật sự có trong kho**, và khi kho rỗng thì không có
 * chip nào — chỗ đó dành cho việc nạp tài liệu, là việc duy nhất có ích lúc ấy.
 */
function ManHinhChao({
  goiY,
  coHocLieu,
  onChon,
}: {
  goiY: string[]
  coHocLieu: boolean
  onChon: (cauHoi: string) => void
}) {
  return (
    <div className="flex flex-col items-center gap-3 px-4 py-10 text-center">
      <span className="chat-hero-icon" aria-hidden>
        <RobotOutlined />
      </span>
      <Text className="font-bold!">Hỏi trợ lý một câu về nội dung bạn đang học</Text>
      <Text className="text-ink-soft max-w-md text-xs">
        {coHocLieu
          ? 'Trợ lý đọc học liệu trong kho rồi trả lời kèm đoạn đã dựa vào, để bạn đối chiếu lại.'
          : 'Kho chưa có tài liệu nào. Nạp một tài liệu trước, trợ lý mới có căn cứ để trả lời.'}
      </Text>

      {goiY.length > 0 && (
        <div className="mt-3 grid w-full max-w-xl gap-2 sm:grid-cols-2">
          {goiY.map((cauHoi) => (
            <button key={cauHoi} type="button" className="chat-chip" onClick={() => onChon(cauHoi)}>
              {cauHoi}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

/**
 * Bốn câu hỏi mở màn, dựng từ tên tài liệu có thật.
 *
 * Đang giới hạn trong một tài liệu thì mọi gợi ý xoay quanh đúng tài liệu đó — gợi ý tài liệu khác
 * lúc này là mời người dùng bấm vào thứ chính bộ lọc của họ đang chặn.
 */
export function goiYCauHoi(
  materials: AskableMaterial[] | undefined,
  selected: AskableMaterial | null,
): string[] {
  if (selected) {
    return [
      `Tóm tắt ý chính của "${selected.title}"`,
      `Cho tôi 5 câu hỏi ôn tập về "${selected.title}"`,
      `Phần nào trong "${selected.title}" là khó nhất?`,
    ]
  }

  const co = materials ?? []
  if (co.length === 0) {
    return []
  }

  const ket = co.slice(0, 3).map((m) => `Tóm tắt ý chính của "${m.title}"`)
  ket.push(`Cho tôi 5 câu hỏi ôn tập về "${co[0].title}"`)
  return ket
}

/**
 * Một bong bóng hội thoại. Câu trả lời của trợ lý luôn kèm phần "dựa trên tài liệu nào".
 *
 * ## Chỉ câu TRẢ LỜI được dựng công thức, câu hỏi thì không
 * Trợ lý được dặn viết công thức trong `$...$` (`ChatPromptBuilder`), nên với nó dấu `$` là mốc có
 * chủ ý. Người học thì gõ tự do: một câu như "sách giá 100$ còn khoá học 200$" có đủ dấu mở và dấu
 * đóng, và dựng phần ở giữa thành công thức là bóp méo đúng câu chữ họ vừa viết ra. Bên nào tự nhận
 * quy ước thì bên đó được dựng.
 *
 * ## Đang stream thì công thức hiện dần
 * Giữa chừng, `$y = x^` chưa có dấu đóng nên `MathText` để nguyên làm chữ (nó không bao giờ sửa chữ
 * thường); tới khi mảnh chứa dấu đóng về thì đoạn đó thành công thức. Không cần xử lý riêng.
 *
 * ## Trích dẫn nguồn giữ nguyên xi
 * Khối `sources` là đoạn trích từ tài liệu gốc — một trích dẫn thì phải đúng từng ký tự với bản gốc,
 * kể cả khi bản gốc viết `x^2` thô. Đó là thứ người học dùng để ĐỐI CHIẾU, nên nó không được đẹp hơn
 * sự thật.
 */
export function MessageBubble({ item, isStreaming }: { item: ChatMessage; isStreaming: boolean }) {
  const isUser = item.role === 'USER'

  if (isUser) {
    return (
      <div className="flex justify-end">
        <div className="max-w-[80%] bg-surface-subtle px-4 py-2">
          <Paragraph className="mb-0! whitespace-pre-wrap">{item.content}</Paragraph>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-[90%]">
      <Text className="text-ink-soft mb-1 block text-xs font-bold">Trợ lý</Text>
      <Paragraph className="mb-2! whitespace-pre-wrap">
        {/* Dựng công thức ở ĐÂY mà không dựng ở bong bóng câu hỏi — xem chú thích trên hàm. */}
        <MathText>{item.content}</MathText>
        {/* Con trỏ nhấp nháy khi chữ chưa chảy xong — người dùng biết là còn đang viết, không phải đã dừng */}
        {isStreaming && <Spin size="small" className="ml-2" />}
      </Paragraph>

      {item.sources.length > 0 ? (
        <div className="border-l-2 border-line pl-3">
          <Text className="text-ink-soft text-xs font-bold">Dựa trên tài liệu</Text>
          {item.sources.map((source) => (
            <div key={source.materialId} className="mt-1">
              <Text className="text-xs font-bold">{source.title}</Text>
              <Paragraph className="text-ink-soft mb-0! text-xs italic">
                “{source.excerpt}”
              </Paragraph>
            </div>
          ))}
        </div>
      ) : (
        // Nói rõ khi câu trả lời KHÔNG dựa trên tài liệu nào. Im lặng ở đây là để người học tưởng
        // mọi câu đều có căn cứ — mà đó chính là chỗ dễ tin sai nhất.
        !isStreaming && (
          <Text className="text-ink-soft text-xs">
            Không dựa trên tài liệu nào — hãy đối chiếu lại trước khi tin.
          </Text>
        )
      )}
    </div>
  )
}
