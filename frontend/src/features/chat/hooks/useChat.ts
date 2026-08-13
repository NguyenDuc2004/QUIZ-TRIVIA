import { useCallback, useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { chatApi, streamChat, type ChatMessage, type ChatSource } from '../api/chatApi'

const CHAT_KEY = 'chat'

export function useChatSessions() {
  return useQuery({
    queryKey: [CHAT_KEY, 'sessions'],
    queryFn: () => chatApi.sessions(),
  })
}

export function useChatMessages(sessionId: string | null) {
  return useQuery({
    queryKey: [CHAT_KEY, 'messages', sessionId],
    queryFn: () => chatApi.messages(sessionId!),
    enabled: Boolean(sessionId),
  })
}

export function useDeleteChatSession() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (sessionId: string) => chatApi.deleteSession(sessionId),
    onSuccess: () => {
      message.success('Đã xoá phiên hội thoại')
      queryClient.invalidateQueries({ queryKey: [CHAT_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

/** Câu trả lời đang được viết dở — chưa có trong CSDL nên không nằm trong cache của TanStack Query. */
export interface StreamingAnswer {
  text: string
  sources: ChatSource[]
  error: string | null
}

/**
 * Một lượt hỏi đáp có streaming.
 * <p>
 * TanStack Query cố tình **không** quản lý phần đang stream: cache của nó xoay quanh "một request →
 * một kết quả", còn ở đây kết quả lớn dần qua hàng trăm sự kiện. Nhồi vào cache thì mỗi mảnh token là
 * một lần ghi cache và một lần render lại toàn bộ danh sách phiên. Nên phần đang chảy để ở state cục
 * bộ, và chỉ khi xong mới xoá cache để đọc lại lịch sử đã lưu ở server.
 */
export function useAskAssistant(sessionId: string | null,
                                onSessionOpened: (sessionId: string) => void) {
  const queryClient = useQueryClient()
  const [streaming, setStreaming] = useState<StreamingAnswer | null>(null)
  const [pendingQuestion, setPendingQuestion] = useState<string | null>(null)
  const abortRef = useRef<AbortController | null>(null)

  // Hỏi dở mà rời trang thì huỷ request: để nguyên thì luồng vẫn chạy và vẫn tính một lượt hạn mức
  useEffect(() => () => abortRef.current?.abort(), [])

  const stop = useCallback(() => {
    abortRef.current?.abort()
    abortRef.current = null
    setStreaming(null)
    setPendingQuestion(null)
  }, [])

  const ask = useCallback(
    async (question: string, materialId?: string) => {
      const controller = new AbortController()
      abortRef.current = controller

      setPendingQuestion(question)
      setStreaming({ text: '', sources: [], error: null })

      let openedSessionId = sessionId

      try {
        await streamChat(
          { question, sessionId: sessionId ?? undefined, materialId },
          (event) => {
            if (event.type === 'meta') {
              openedSessionId = event.sessionId
              setStreaming((prev) => (prev ? { ...prev, sources: event.sources } : prev))
              if (!sessionId) {
                onSessionOpened(event.sessionId)
              }
            } else if (event.type === 'token') {
              setStreaming((prev) => (prev ? { ...prev, text: prev.text + event.text } : prev))
            } else {
              setStreaming((prev) => (prev ? { ...prev, error: event.message } : prev))
            }
          },
          controller.signal,
        )

        // Xong thì đọc lại từ server: câu trả lời đã được lưu kèm nguồn, và đó là bản chính thức
        if (openedSessionId) {
          await queryClient.invalidateQueries({ queryKey: [CHAT_KEY] })
        }
        setStreaming(null)
        setPendingQuestion(null)
      } catch (error) {
        if (controller.signal.aborted) {
          return
        }
        setStreaming((prev) => ({
          text: prev?.text ?? '',
          sources: prev?.sources ?? [],
          error: error instanceof Error ? error.message : 'Không gửi được câu hỏi',
        }))
        setPendingQuestion(null)
      } finally {
        abortRef.current = null
      }
    },
    [sessionId, onSessionOpened, queryClient],
  )

  return { ask, stop, streaming, pendingQuestion, isStreaming: streaming !== null }
}

/** Ghép lịch sử đã lưu với lượt đang chảy để giao diện chỉ cần vẽ một danh sách. */
export function mergeMessages(
  saved: ChatMessage[] | undefined,
  pendingQuestion: string | null,
  streaming: StreamingAnswer | null,
): ChatMessage[] {
  const merged: ChatMessage[] = [...(saved ?? [])]

  if (pendingQuestion !== null) {
    merged.push({
      id: 'pending-question',
      role: 'USER',
      content: pendingQuestion,
      sources: [],
      createdAt: new Date().toISOString(),
    })
  }
  if (streaming) {
    merged.push({
      id: 'streaming-answer',
      role: 'ASSISTANT',
      content: streaming.text,
      sources: streaming.sources,
      createdAt: new Date().toISOString(),
    })
  }
  return merged
}
