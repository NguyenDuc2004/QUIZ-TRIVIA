import { Link } from 'react-router-dom'
import { Alert, Card, Skeleton, Switch, Typography } from 'antd'
import PageHeader from '@/shared/components/PageHeader'
import type { NotificationType } from '../api/notificationApi'
import { useNotificationSettings, useUpdateNotificationSettings } from '../hooks/useNotifications'

const { Text } = Typography

/** Một câu giải thích cho từng loại — công tắc không nói được nó tắt cái gì. */
const GIAI_THICH: Partial<Record<NotificationType, string>> = {
  SRS_REMINDER: 'Nhắc mỗi sáng khi bạn có thẻ ghi nhớ đến hạn ôn.',
  ACHIEVEMENT: 'Khi bạn lên cấp hoặc mở khoá huy hiệu mới.',
}

/**
 * Cài đặt thông báo (features/16, FR-70 phần bật/tắt theo loại).
 *
 * Danh sách công tắc lấy từ **máy chủ** (`dieuChinhDuoc`), không hardcode: loại nào đã có nguồn phát là việc
 * của máy chủ. Hardcode ở đây thì trang sẽ hiện công tắc cho loại chưa ai gửi — một công tắc không làm gì cả,
 * đúng cái đã hoãn ở FR-84 (ô nhập hạn mức AI của tính năng 10).
 */
export default function NotificationSettingsPage() {
  const { data, isPending } = useNotificationSettings()
  const capNhat = useUpdateNotificationSettings()

  if (isPending || !data) {
    return <Skeleton active paragraph={{ rows: 4 }} />
  }

  const dangTat = new Set(data.disabledTypes)

  /** Gửi cả trạng thái mới, không gửi "bật cái này": hai tab mở song song thì không chồng lên nhau lạ lùng. */
  const doi = (type: NotificationType, bat: boolean) => {
    const moi = new Set(dangTat)
    if (bat) {
      moi.delete(type)
    } else {
      moi.add(type)
    }
    capNhat.mutate([...moi])
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Cài đặt thông báo"
        description={
          <>
            Chọn loại thông báo bạn muốn nhận. <Link to="/notifications">Về danh sách thông báo</Link>
          </>
        }
      />

      <Card>
        <div className="flex flex-col divide-y divide-line">
          {data.dieuChinhDuoc.map(({ type, nhan }) => (
            <div key={type} className="flex items-start justify-between gap-4 py-3 first:pt-0 last:pb-0">
              <div className="min-w-0">
                <Text className="block font-bold!">{nhan}</Text>
                {GIAI_THICH[type] && (
                  <Text className="text-ink-soft block text-sm">{GIAI_THICH[type]}</Text>
                )}
              </div>
              <Switch
                checked={!dangTat.has(type)}
                loading={capNhat.isPending}
                onChange={(bat) => doi(type, bat)}
                aria-label={nhan}
              />
            </div>
          ))}
        </div>
      </Card>

      {/* Nói ra thứ KHÔNG tắt được, thay vì để người dùng đi tìm một công tắc không tồn tại */}
      <Alert
        type="info"
        showIcon
        message="Thông báo hệ thống luôn bật"
        description="Đây là kênh để thông báo bảo trì, thay đổi điều khoản hoặc sự cố dữ liệu — những việc bạn cần biết. Đổi lại, kênh này không dùng để giới thiệu tính năng hay tiếp thị."
      />
    </div>
  )
}
