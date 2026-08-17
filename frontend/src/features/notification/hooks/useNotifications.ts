import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { useAuthStore } from '@/features/auth/store/authStore'
import { notificationApi, type NotificationType } from '../api/notificationApi'

export type { Notification } from '../api/notificationApi'

export const NOTIFICATION_KEY = 'notifications'

/**
 * Số chưa đọc cho chấm đỏ trên chuông.
 *
 * `enabled` theo trạng thái đăng nhập: chuông nằm trong `AppLayout`, và `AppLayout` cũng dựng cho khách vào
 * phòng đấu bằng mã PIN — gọi endpoint này lúc đó là một request 401 mỗi lần tải trang.
 *
 * Không `refetchInterval`: WebSocket đã đẩy xuống khi có thông báo mới, nên hỏi lại theo chu kỳ chỉ thêm tải
 * cho đúng cái đã có đường khác. Mất WebSocket thì con số cập nhật ở lần đổi trang — đủ tốt cho một chấm đỏ.
 */
export function useUnreadCount() {
  const user = useAuthStore((state) => state.user)
  return useQuery({
    queryKey: [NOTIFICATION_KEY, 'unread'],
    queryFn: () => notificationApi.soChuaDoc(),
    enabled: Boolean(user),
  })
}

/**
 * Danh sách thông báo.
 *
 * `enabled` có mặt vì chuông chỉ nên gọi endpoint này **khi hộp đã mở**: chuông hiện ở mọi trang, và kéo về
 * cả trang thông báo chỉ để vẽ một chấm đỏ là tốn vô ích — con số đã có `useUnreadCount` rẻ hơn. Không có
 * tham số này thì bên gọi chỉ có thể đổi `params`, mà đổi params vẫn là một request.
 */
export function useNotifications(
  params: { page?: number; size?: number } = {},
  options: { enabled?: boolean } = {},
) {
  const user = useAuthStore((state) => state.user)
  return useQuery({
    queryKey: [NOTIFICATION_KEY, 'list', params],
    queryFn: () => notificationApi.danhSach(params),
    enabled: Boolean(user) && options.enabled !== false,
  })
}

/**
 * Đánh dấu một thông báo đã đọc.
 *
 * Không hiện thông báo thành công: người dùng vừa bấm vào một thông báo để đọc nó, và một hộp "đã đánh dấu
 * đã đọc" bật lên là báo lại cho họ điều họ vừa tự làm.
 */
export function useMarkRead() {
  const queryClient = useQueryClient()
  return useMutation({
    // Bọc trong arrow chứ KHÔNG truyền thẳng `notificationApi.danhDauDaDoc`: TanStack Query gọi `mutationFn`
    // với hai tham số (variables, context), nên truyền thẳng thì hàm API nhận thêm một object context ở vị
    // trí thứ hai. Hôm nay vô hại vì hàm chỉ đọc tham số đầu, nhưng ngày nào đó nó có tham số thứ hai tuỳ
    // chọn thì context của TanStack lặng lẽ chảy vào đúng chỗ đó.
    mutationFn: (id: string) => notificationApi.danhDauDaDoc(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [NOTIFICATION_KEY] }),
  })
}

export function useMarkAllRead() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => notificationApi.danhDauTatCa(),
    onSuccess: (soDaDanhDau) => {
      // Nói đúng số vừa đổi. "Đã đánh dấu tất cả" khi thực tế không có cái nào chưa đọc là một câu vô nghĩa.
      if (soDaDanhDau > 0) {
        message.success(`Đã đánh dấu ${soDaDanhDau} thông báo là đã đọc`)
      }
      queryClient.invalidateQueries({ queryKey: [NOTIFICATION_KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useNotificationSettings() {
  const user = useAuthStore((state) => state.user)
  return useQuery({
    queryKey: [NOTIFICATION_KEY, 'settings'],
    queryFn: () => notificationApi.caiDat(),
    enabled: Boolean(user),
  })
}

export function useUpdateNotificationSettings() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (disabledTypes: NotificationType[]) =>
      notificationApi.capNhatCaiDat(disabledTypes),
    onSuccess: (settings) => {
      message.success('Đã lưu cài đặt thông báo')
      // Ghi thẳng kết quả máy chủ trả về vào cache thay vì chỉ invalidate: máy chủ có thể LỌC bớt lựa chọn
      // (SYSTEM không tắt được), nên nếu giữ trạng thái đã gửi thì giao diện hiện một công tắc đã tắt mà
      // thực tế vẫn bật
      queryClient.setQueryData([NOTIFICATION_KEY, 'settings'], settings)
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}
