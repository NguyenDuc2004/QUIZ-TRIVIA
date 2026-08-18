import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useQueryClient } from '@tanstack/react-query'
import { tokenStorage } from '@/shared/api/tokenStorage'
import { useAuthStore } from '@/features/auth/store/authStore'
import { NOTIFICATION_KEY } from './useNotifications'

/**
 * Nối WebSocket để nhận thông báo real-time (features/16, FR-67).
 *
 * ## Một kết nối cho cả phiên, không phải một kết nối cho mỗi trang
 * Hook này gắn ở `AppLayout` nên nó sống suốt lúc người dùng đăng nhập. Khác với `useRoomSocket` vốn nối theo
 * từng phòng: thông báo có thể tới bất cứ lúc nào, ở bất cứ trang nào.
 *
 * ## Chỉ nối khi ĐÃ đăng nhập
 * Frame CONNECT của STOMP bị từ chối nếu không có danh tính hợp lệ, và bị từ chối thì `@stomp/stompjs` sẽ thử
 * lại theo `reconnectDelay` — tức là một vòng lặp gõ cửa vô ích mỗi 3 giây với khách chưa đăng nhập. Nên chốt
 * bằng `user` chứ không chỉ bằng sự tồn tại của token.
 *
 * ## Không tự dựng danh sách từ tin nhắn đẩy xuống
 * Nhận được thông báo thì hook **làm mất hiệu lực** cache của TanStack Query thay vì tự chèn vào danh sách.
 * Chèn tay nghe nhanh hơn nhưng phải tự lo phân trang, thứ tự, và số chưa đọc — ba chỗ dễ lệch với máy chủ,
 * mà lệch thì người dùng thấy một danh sách khác với sự thật. Máy chủ là nguồn sự thật duy nhất.
 */
export function useNotificationSocket() {
  const user = useAuthStore((state) => state.user)
  const queryClient = useQueryClient()

  // Giữ trong ref: đưa queryClient vào deps thì mỗi lần nó đổi là một lần ngắt/nối lại WebSocket
  const queryClientRef = useRef(queryClient)
  queryClientRef.current = queryClient

  useEffect(() => {
    if (!user) {
      return
    }
    const token = tokenStorage.getAccess()
    if (!token) {
      return
    }

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        // Đích thật là /user/{userId}/queue/notifications — Spring tự ghép tiền tố theo danh tính của phiên,
        // nên client KHÔNG cần (và không được) tự nhét userId vào đây
        client.subscribe('/user/queue/notifications', () => {
          // Không đọc nội dung gói tin: hook chỉ cần biết "có cái mới" rồi để TanStack Query hỏi lại máy
          // chủ. Tự chèn vào cache thì phải tự lo phân trang, thứ tự và số chưa đọc — ba chỗ dễ lệch.
          queryClientRef.current.invalidateQueries({ queryKey: [NOTIFICATION_KEY] })
          // Cố ý KHÔNG hiện toast: chuông đã có chấm đỏ, còn một hộp bật lên giữa lúc người ta đang làm bài
          // thi thì gây hại nhiều hơn giúp.
        })
      },
    })

    client.activate()
    return () => {
      void client.deactivate()
    }
  }, [user])
}
