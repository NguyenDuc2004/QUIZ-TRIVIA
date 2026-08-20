import { useCallback, useEffect, useRef, useState } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { tokenStorage } from '@/shared/api/tokenStorage'
import type { GameEvent } from '../api/roomApi'
import { guestSession } from '../api/guestSession'

export type SocketStatus = 'connecting' | 'connected' | 'disconnected'

type RoomAction = 'start' | 'answer' | 'next' | 'ready' | 'avatar' | 'proctoring' | 'warn'

interface Options {
  roomCode: string | undefined
  /** Sự kiện phát cho cả phòng. */
  onEvent: (event: GameEvent) => void
  /** Sự kiện gửi riêng cho mình (kết quả câu vừa trả lời). */
  onPrivateEvent?: (event: GameEvent) => void
  /** Lỗi nghiệp vụ trong ván đấu (hết giờ, không phải host…). */
  onError?: (error: { status: number; message: string }) => void
}

/**
 * Nối WebSocket/STOMP tới phòng đấu.
 * <p>
 * Gửi <b>một trong hai</b> loại danh tính ở frame CONNECT: `Authorization` với thành viên đã đăng
 * nhập, hoặc `X-Guest-Key` với khách vãng lai vừa quét QR. Ưu tiên khoá khách nếu tab này đang là
 * một phiên khách — để người đã có tài khoản vẫn mở được một tab khác chơi với tư cách khách.
 * <p>
 * Token đi trong header chứ không phải query string — query string bị ghi vào log của proxy.
 * <p>
 * `@stomp/stompjs` tự kết nối lại sau khi rớt mạng. Nối lại xong client nên gọi
 * `GET /rooms/{code}` để dựng lại trạng thái, vì các sự kiện lỡ mất không được phát lại.
 */
export function useRoomSocket({ roomCode, onEvent, onPrivateEvent, onError }: Options) {
  const [status, setStatus] = useState<SocketStatus>('connecting')
  const clientRef = useRef<Client | null>(null)

  // Giữ callback mới nhất trong ref: nếu đưa thẳng vào deps, mỗi lần cha render lại
  // sẽ ngắt và nối lại WebSocket.
  const handlers = useRef({ onEvent, onPrivateEvent, onError })
  handlers.current = { onEvent, onPrivateEvent, onError }

  useEffect(() => {
    if (!roomCode) return

    const guest = guestSession.get(roomCode)
    const connectHeaders: Record<string, string> = guest
      ? { 'X-Guest-Key': guest.guestKey }
      : { Authorization: `Bearer ${tokenStorage.getAccess() ?? ''}` }

    const parse = (message: IMessage) => JSON.parse(message.body)

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders,
      reconnectDelay: 3000,
      onConnect: () => {
        setStatus('connected')
        client.subscribe(`/topic/room/${roomCode}`, (m) => handlers.current.onEvent(parse(m)))
        client.subscribe(`/user/queue/room/${roomCode}`, (m) =>
          handlers.current.onPrivateEvent?.(parse(m)),
        )
        client.subscribe('/user/queue/errors', (m) => handlers.current.onError?.(parse(m)))
      },
      onWebSocketClose: () => setStatus('disconnected'),
      onStompError: () => setStatus('disconnected'),
    })

    client.activate()
    clientRef.current = client

    return () => {
      clientRef.current = null
      void client.deactivate()
    }
  }, [roomCode])

  const send = useCallback(
    (action: RoomAction, body?: unknown) => {
      const client = clientRef.current
      if (!client?.connected || !roomCode) return
      client.publish({
        destination: `/app/room/${roomCode}/${action}`,
        body: JSON.stringify(body ?? {}),
      })
    },
    [roomCode],
  )

  return { status, send }
}
