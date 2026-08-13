import { apiClient, refreshSessionForRawFetch } from '@/shared/api/client'
import { tokenStorage } from '@/shared/api/tokenStorage'

export interface ChatSource {
  materialId: string
  title: string
  /** Đoạn đã cấp cho mô hình — để người học tự đối chiếu, không phải để đọc lại cả tài liệu. */
  excerpt: string
}

export interface ChatSession {
  id: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  id: string
  role: 'USER' | 'ASSISTANT'
  content: string
  /** Rỗng với tin của người dùng, và cả với câu trả lời không dựa trên tài liệu nào. */
  sources: ChatSource[]
  createdAt: string
}

/** Ba loại sự kiện của luồng SSE, khớp với `ChatController`. */
export type ChatStreamEvent =
  | { type: 'meta'; sessionId: string; sources: ChatSource[] }
  | { type: 'token'; text: string }
  | { type: 'error'; message: string }

export const chatApi = {
  sessions: () => apiClient.get<ChatSession[]>('/ai/chat/sessions').then((res) => res.data),

  messages: (sessionId: string) =>
    apiClient.get<ChatMessage[]>(`/ai/chat/sessions/${sessionId}`).then((res) => res.data),

  deleteSession: (sessionId: string) =>
    apiClient.delete<void>(`/ai/chat/sessions/${sessionId}`).then((res) => res.data),
}

/**
 * Gửi câu hỏi và đọc luồng trả lời.
 * <p>
 * Dùng `fetch` + `ReadableStream` chứ **không** dùng `EventSource`: `EventSource` chỉ gửi được `GET`
 * và không đặt được header, nên không mang nổi `Authorization`. Endpoint này là `POST` (câu hỏi dài,
 * không nhồi vào query string được) và cần token — hai điều `EventSource` không làm được cái nào.
 * <p>
 * Đổi lại phải tự bóc định dạng SSE. Việc đó nhỏ và có lợi: `fetch` cho luôn `AbortSignal` để người
 * dùng bấm Dừng giữa lúc mô hình đang trả lời.
 *
 * @param onEvent gọi cho từng sự kiện, theo đúng thứ tự tới
 */
export async function streamChat(
  body: { question: string; sessionId?: string; materialId?: string },
  onEvent: (event: ChatStreamEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

  const open = (token: string | null) =>
    fetch(`${baseUrl}/ai/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify(body),
      signal,
    })

  let response = await open(tokenStorage.getAccess())

  // `fetch` thô không đi qua interceptor của axios nên không được tự làm mới token. Tự làm ở đây,
  // dùng chung hàm làm mới của interceptor để không có hai lượt refresh song song (backend luân
  // chuyển refresh token, hai lượt song song là tự đăng xuất một phiên còn cứu được).
  if (response.status === 401) {
    response = await open(await refreshSessionForRawFetch())
  }

  if (!response.ok || !response.body) {
    // Lỗi ở giai đoạn chuẩn bị (phiên không tồn tại, câu hỏi rỗng, chưa đăng nhập) vẫn là mã HTTP
    // thường vì backend chạy bước đó trước khi mở luồng — nên đọc `message` như mọi API khác
    const detail = await response.json().catch(() => null)
    throw new Error(detail?.message ?? 'Không gửi được câu hỏi tới trợ lý')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  for (;;) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    // `stream: true` là bắt buộc: một ký tự tiếng Việt có thể bị cắt đôi giữa hai khối byte, giải mã
    // từng khối độc lập sẽ ra ký tự thay thế ở đúng chỗ nối
    buffer += decoder.decode(value, { stream: true })

    // Sự kiện SSE kết thúc bằng một dòng trống; khối cuối cùng có thể còn dở nên giữ lại trong buffer
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() ?? ''
    blocks.forEach((block) => {
      const parsed = parseBlock(block)
      if (parsed) {
        onEvent(parsed)
      }
    })
  }
}

function parseBlock(block: string): ChatStreamEvent | null {
  let name = ''
  const dataLines: string[] = []

  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) {
      name = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      // Chỉ bỏ MỘT khoảng trắng ngay sau dấu hai chấm, đúng như chuẩn SSE — không `trim()`, vì
      // payload là JSON nên khoảng trắng bên trong nó là dữ liệu thật
      dataLines.push(line.slice(5).replace(/^ /, ''))
    }
  }
  if (!name || dataLines.length === 0) {
    return null
  }

  // Nhiều dòng `data:` trong một sự kiện được nối lại bằng \n, theo chuẩn
  const raw = dataLines.join('\n')
  try {
    const payload = JSON.parse(raw)
    if (name === 'meta') {
      return { type: 'meta', sessionId: payload.sessionId, sources: payload.sources ?? [] }
    }
    if (name === 'token') {
      return { type: 'token', text: payload.t ?? '' }
    }
    if (name === 'error') {
      return { type: 'error', message: payload.message ?? 'Trợ lý gặp sự cố' }
    }
  } catch {
    // Khối lỗi định dạng thì bỏ, không kéo đổ cả luồng đang chạy dở
  }
  return null
}
