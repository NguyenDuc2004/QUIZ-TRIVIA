import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Badge, Button, Dropdown, Empty, Spin, Typography } from 'antd'
import { BellOutlined, SettingOutlined } from '@ant-design/icons'
import type { Notification, NotificationData } from '../api/notificationApi'
import {
  useMarkAllRead,
  useMarkRead,
  useNotifications,
  useUnreadCount,
} from '../hooks/useNotifications'

const { Text } = Typography

/** Bao nhiêu thông báo hiện trong hộp thả xuống. Xem thêm thì sang trang riêng. */
const SO_HIEN_TRONG_HOP = 8

/**
 * Chuông thông báo trên thanh điều hướng (features/16, FR-68).
 *
 * Hộp thả xuống **không phải** một trang danh sách thu nhỏ: nó chỉ hiện 8 cái mới nhất để người dùng biết có
 * gì mới và bấm sang chỗ cần đến. Nhồi phân trang vào một hộp 360px là làm hai thứ tệ thay vì một thứ tốt.
 */
export default function NotificationBell() {
  const [moHop, setMoHop] = useState(false)
  const { data: soChuaDoc } = useUnreadCount()
  // Chỉ gọi danh sách khi hộp đã mở: chuông hiện ở mọi trang, mà con số chưa đọc đã có endpoint riêng rẻ hơn
  const { data, isLoading } = useNotifications({ size: SO_HIEN_TRONG_HOP }, { enabled: moHop })
  const markRead = useMarkRead()
  const markAllRead = useMarkAllRead()

  return (
    <Dropdown
      open={moHop}
      onOpenChange={setMoHop}
      trigger={['click']}
      placement="bottomRight"
      popupRender={() => (
        <div className="border-line bg-surface w-90 max-w-[calc(100vw-2rem)] rounded-card border shadow-lg">
          <div className="border-line flex items-center justify-between border-b px-4 py-2.5">
            <Text className="font-bold!">Thông báo</Text>
            {(soChuaDoc ?? 0) > 0 && (
              <Button
                type="link"
                size="small"
                className="px-0!"
                loading={markAllRead.isPending}
                onClick={() => markAllRead.mutate()}
              >
                Đánh dấu tất cả đã đọc
              </Button>
            )}
          </div>

          <div className="max-h-96 overflow-y-auto">
            {isLoading ? (
              <div className="flex justify-center py-8">
                <Spin />
              </div>
            ) : (data?.content.length ?? 0) === 0 ? (
              <Empty
                image={null}
                className="my-6!"
                description={
                  <Text className="text-ink-soft text-sm">
                    Chưa có thông báo nào. Nhắc ôn tập sẽ tới khi bạn có thẻ đến hạn.
                  </Text>
                }
              />
            ) : (
              data?.content.map((n) => (
                <MotDong
                  key={n.id}
                  thongBao={n}
                  onDoc={() => {
                    if (!n.read) markRead.mutate(n.id)
                    setMoHop(false)
                  }}
                />
              ))
            )}
          </div>

          <div className="border-line flex items-center justify-between border-t px-4 py-2">
            <Link to="/notifications" className="text-sm font-bold" onClick={() => setMoHop(false)}>
              Xem tất cả
            </Link>
            <Link
              to="/notifications/settings"
              className="text-ink-soft! text-sm"
              onClick={() => setMoHop(false)}
              aria-label="Cài đặt thông báo"
            >
              <SettingOutlined />
            </Link>
          </div>
        </div>
      )}
    >
      <button
        type="button"
        aria-label={
          soChuaDoc ? `Thông báo, ${soChuaDoc} chưa đọc` : 'Thông báo'
        }
        className="text-ink hover:bg-surface-subtle flex size-9 cursor-pointer items-center justify-center rounded-control border-0 bg-transparent transition-colors"
      >
        {/* Badge của antd tự ẩn khi count = 0, nên không cần rẽ nhánh ở đây */}
        <Badge count={soChuaDoc ?? 0} size="small" overflowCount={99}>
          <BellOutlined className="text-ink! text-lg" />
        </Badge>
      </button>
    </Dropdown>
  )
}

/** Một dòng trong hộp. Bấm vào thì đánh dấu đã đọc và đi tới chỗ liên quan. */
function MotDong({ thongBao, onDoc }: { thongBao: Notification; onDoc: () => void }) {
  const navigate = useNavigate()
  const dich = duongDan(thongBao.data)

  return (
    <button
      type="button"
      onClick={() => {
        onDoc()
        if (dich) navigate(dich)
      }}
      className={`border-line hover:bg-surface-subtle flex w-full cursor-pointer gap-2.5 border-0 border-b px-4 py-3 text-left transition-colors last:border-b-0 ${
        thongBao.read ? 'bg-transparent' : 'bg-brand-subtle'
      }`}
    >
      {/* Dấu chưa đọc là một chấm, không phải chữ "MỚI": chấm đọc được ngay mà không chiếm dòng */}
      <span
        aria-hidden
        className={`mt-1.5 size-2 shrink-0 rounded-full ${
          thongBao.read ? 'bg-transparent' : 'bg-brand'
        }`}
      />
      <span className="min-w-0 flex-1">
        <Text className={`block text-sm ${thongBao.read ? '' : 'font-bold!'}`}>{thongBao.title}</Text>
        {thongBao.body && (
          <Text className="text-ink-soft block text-xs">{thongBao.body}</Text>
        )}
        <Text className="text-ink-soft block text-[11px]">
          {thongBao.loaiNhan} · {khiNao(thongBao.createdAt)}
        </Text>
      </span>
    </button>
  )
}

/**
 * Bấm vào thông báo thì đi đâu.
 *
 * Trả `null` khi không có đích rõ ràng — lúc đó dòng vẫn bấm được để đánh dấu đã đọc nhưng không điều hướng.
 * Đưa người dùng về trang chủ chỉ vì không biết đưa đi đâu là tệ hơn không đi đâu cả: họ mất chỗ đang đứng.
 */
export function duongDan(data: NotificationData | null): string | null {
  if (!data) return null
  switch (data.kind) {
    case 'SRS_DUE':
      return '/flashcards'
    case 'LEVEL_UP':
    case 'BADGE':
      return '/achievements'
    default:
      return null
  }
}

/**
 * "3 phút trước", "hôm qua"…
 *
 * Tự viết thay vì thêm một thư viện định dạng thời gian: đây là chỗ duy nhất trong dự án cần nó, và một
 * thư viện kéo theo locale tiếng Việt là vài chục KB cho sáu nhánh if.
 */
export function khiNao(iso: string): string {
  const giay = Math.floor((Date.now() - new Date(iso).getTime()) / 1000)
  if (giay < 60) return 'vừa xong'
  if (giay < 3600) return `${Math.floor(giay / 60)} phút trước`
  if (giay < 86_400) return `${Math.floor(giay / 3600)} giờ trước`
  if (giay < 172_800) return 'hôm qua'
  if (giay < 604_800) return `${Math.floor(giay / 86_400)} ngày trước`
  return new Date(iso).toLocaleDateString('vi-VN')
}
