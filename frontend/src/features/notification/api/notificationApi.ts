import { apiClient } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/types'

/**
 * Loại thông báo. Khớp `NotificationType` của backend.
 *
 * `ASSIGNMENT_DUE` và `ROOM_INVITE` khai sẵn nhưng **chưa có nguồn phát** — chờ tính năng 14 (lớp học) và
 * cơ chế mời của tính năng 04. Danh sách loại hiện trên trang cài đặt lấy từ **máy chủ**
 * (`dieuChinhDuoc`), không hardcode ở đây: loại nào đã có nguồn phát là việc của máy chủ, và nếu frontend
 * tự liệt kê thì nó sẽ hiện công tắc cho loại chưa ai gửi.
 */
export type NotificationType =
  | 'SRS_REMINDER'
  | 'ACHIEVEMENT'
  | 'ASSIGNMENT_DUE'
  | 'ROOM_INVITE'
  | 'SYSTEM'

/** Dữ liệu điều hướng kèm thông báo. Mỗi `kind` có hình dạng riêng, nên kiểm `kind` trước khi đọc. */
export type NotificationData =
  | { kind: 'SRS_DUE'; soThe: number }
  | { kind: 'LEVEL_UP'; level: number }
  | { kind: 'BADGE'; code: string }

export interface Notification {
  id: string
  type: NotificationType
  /** Nhãn tiếng Việt của loại, do máy chủ trả — frontend không tự dịch tên enum. */
  loaiNhan: string
  title: string
  body: string | null
  /** Máy chủ trả **đối tượng**, không phải chuỗi cần `JSON.parse`. Null khi thông báo không cần điều hướng. */
  data: NotificationData | null
  read: boolean
  createdAt: string
}

export interface NotificationSettings {
  disabledTypes: NotificationType[]
  /** Loại hiện ra trên trang cài đặt — chỉ loại đã có nguồn phát và tắt được. */
  dieuChinhDuoc: { type: NotificationType; nhan: string }[]
}

export const notificationApi = {
  danhSach: (params: { page?: number; size?: number }) =>
    apiClient
      .get<PageResponse<Notification>>('/notifications', { params })
      .then((res) => res.data),

  /** Riêng một endpoint cho con số: chấm đỏ hiện ở mọi trang nên đây là truy vấn chạy nhiều nhất. */
  soChuaDoc: () =>
    apiClient
      .get<{ soChuaDoc: number }>('/notifications/unread-count')
      .then((res) => res.data.soChuaDoc),

  danhDauDaDoc: (id: string) => apiClient.put<void>(`/notifications/${id}/read`).then(() => undefined),

  danhDauTatCa: () =>
    apiClient
      .put<{ daDanhDau: number }>('/notifications/read-all')
      .then((res) => res.data.daDanhDau),

  caiDat: () =>
    apiClient.get<NotificationSettings>('/notifications/settings').then((res) => res.data),

  /** Đặt lại **toàn bộ** danh sách loại bị tắt, không phải bật/tắt từng cái. */
  capNhatCaiDat: (disabledTypes: NotificationType[]) =>
    apiClient
      .put<NotificationSettings>('/notifications/settings', { disabledTypes })
      .then((res) => res.data),
}
