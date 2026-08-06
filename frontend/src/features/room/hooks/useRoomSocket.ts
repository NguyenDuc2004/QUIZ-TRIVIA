import { useCallback, useEffect, useRef, useState } from 'react'
import { Client, type IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { tokenStorage } from '@/shared/api/tokenStorage'
import type { GameEvent } from '../api/roomApi'

export type SocketStatus = 'connecting' | 'connected' | 'disconnected'

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
 * Token đi trong header của frame CONNECT chứ không phải query string — query string bị ghi vào
 * log truy cập của proxy, còn header thì không.
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

    const parse = (message: IMessage) => JSON.parse(message.body)

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${tokenStorage.getAccess() ?? ''}` },
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
    (action: 'start' | 'answer' | 'next', body?: unknown) => {
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
