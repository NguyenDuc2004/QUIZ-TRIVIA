import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Alert, Button, Input, Popconfirm, Skeleton, Space, Spin, Tooltip, Typography } from 'antd'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import type { ChatMessage } from '../api/chatApi'
import {
  mergeMessages,
  useAskAssistant,
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
  const bottomRef = useRef<HTMLDivElement>(null)

  const { data: sessions, isPending: sessionsLoading } = useChatSessions()
  const { data: savedMessages, isPending: messagesLoading } = useChatMessages(sessionId)
  const deleteSession = useDeleteChatSession()
  const { ask, stop, streaming, pendingQuestion, isStreaming } = useAskAssistant(
    sessionId,
    setSessionId,
  )

  const messages = mergeMessages(savedMessages, pendingQuestion, streaming)

  // Cuộn xuống theo từng mảnh chữ để câu trả lời đang chạy luôn nằm trong tầm mắt
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages.length, streaming?.text])

  const send = () => {
    const question = draft.trim()
    if (!question || isStreaming) {
      return
    }
    setDraft('')
    void ask(question)
  }

  return (
    <Space direction="vertical" size="large" className="w-full">
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

      <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
        {/* Danh sách phiên */}
        <aside className="border border-line bg-white">
          <div className="border-b border-line px-4 py-3">
            <Text className="text-xs font-bold">Hội thoại của bạn</Text>
          </div>
          {sessionsLoading ? (
            <div className="p-4">
              <Skeleton active paragraph={{ rows: 3 }} title={false} />
            </div>
          ) : !sessions || sessions.length === 0 ? (
            <Text className="text-ink-soft block px-4 py-6 text-center text-xs">
              Chưa có hội thoại nào
            </Text>
          ) : (
            <ul className="m-0 list-none p-0">
              {sessions.map((session) => (
                <li
                  key={session.id}
                  className={`flex items-start gap-2 border-b border-line px-3 py-2 last:border-b-0 ${
                    session.id === sessionId ? 'bg-surface-subtle' : ''
                  }`}
                >
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
                  <Popconfirm
                    title="Xoá hội thoại này?"
                    okText="Xoá"
                    cancelText="Hủy"
                    okButtonProps={{ danger: true }}
                    onConfirm={() => {
                      if (session.id === sessionId) {
                        setSessionId(null)
                      }
                      deleteSession.mutate(session.id)
                    }}
                  >
                    <Button type="link" size="small" danger className="px-0!">
                      Xoá
                    </Button>
                  </Popconfirm>
                </li>
              ))}
            </ul>
          )}
        </aside>

        {/* Khung hội thoại */}
        <section className="flex min-h-[60vh] flex-col border border-line bg-white">
          <div className="flex-1 overflow-y-auto p-4">
            {sessionId && messagesLoading ? (
              <Skeleton active paragraph={{ rows: 5 }} />
            ) : messages.length === 0 ? (
              <EmptyState
                title="Hỏi trợ lý một câu về nội dung bạn đang học"
                hint="Trợ lý chỉ trả lời dựa trên học liệu — tài liệu của bạn, và tài liệu người tạo nội dung đã chia sẻ"
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
                {streaming?.error && (
                  <Alert type="warning" showIcon message={streaming.error} />
                )}
              </Space>
            )}
            <div ref={bottomRef} />
          </div>

          <div className="border-t border-line p-3">
            <div className="flex items-end gap-2">
              <Input.TextArea
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Hỏi về nội dung trong học liệu…"
                autoSize={{ minRows: 1, maxRows: 5 }}
                maxLength={2000}
                disabled={isStreaming}
                onPressEnter={(event) => {
                  // Enter gửi, Shift+Enter xuống dòng — quy ước người dùng đã quen ở mọi khung chat
                  if (!event.shiftKey) {
                    event.preventDefault()
                    send()
                  }
                }}
              />
              {isStreaming ? (
                <Tooltip title="Dừng thì phần đã trả lời vẫn được lưu">
                  <Button onClick={stop}>Dừng</Button>
                </Tooltip>
              ) : (
                <Button type="primary" disabled={!draft.trim()} onClick={send}>
                  Gửi
                </Button>
              )}
            </div>
            <Text className="text-ink-soft mt-2 block text-xs">
              Trợ lý chỉ dựa trên học liệu. Chưa có tài liệu nào phù hợp thì nó sẽ nói thẳng là không
              biết, thay vì đoán. Người tạo nội dung nạp tài liệu ở{' '}
              <Link to="/ai/materials">trang Học liệu</Link>.
            </Text>
          </div>
        </section>
      </div>
    </Space>
  )
}

/** Một bong bóng hội thoại. Câu trả lời của trợ lý luôn kèm phần "dựa trên tài liệu nào". */
function MessageBubble({ item, isStreaming }: { item: ChatMessage; isStreaming: boolean }) {
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
        {item.content}
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
