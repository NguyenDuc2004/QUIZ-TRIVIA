import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button, Card, Pagination, Skeleton, Tag, Typography } from 'antd'
import { CheckOutlined, SettingOutlined } from '@ant-design/icons'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import { duongDan, khiNao } from '../components/NotificationBell'
import {
  useMarkAllRead,
  useMarkRead,
  useNotifications,
  useUnreadCount,
} from '../hooks/useNotifications'

const { Text } = Typography

const SO_MOI_TRANG = 20

/** Trung tâm thông báo (features/16, FR-68) — danh sách đầy đủ, có phân trang. */
export default function NotificationsPage() {
  const [page, setPage] = useState(0)
  const { data, isPending } = useNotifications({ page, size: SO_MOI_TRANG })
  const { data: soChuaDoc } = useUnreadCount()
  const markRead = useMarkRead()
  const markAllRead = useMarkAllRead()
  const navigate = useNavigate()

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Thông báo"
        description={
          soChuaDoc ? `${soChuaDoc} thông báo chưa đọc` : 'Bạn đã đọc hết thông báo'
        }
        actions={
          <div className="flex gap-2">
            {(soChuaDoc ?? 0) > 0 && (
              <Button
                icon={<CheckOutlined />}
                loading={markAllRead.isPending}
                onClick={() => markAllRead.mutate()}
              >
                Đánh dấu tất cả đã đọc
              </Button>
            )}
            <Link to="/notifications/settings">
              <Button icon={<SettingOutlined />}>Cài đặt</Button>
            </Link>
          </div>
        }
      />

      {isPending ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : data?.content.length === 0 ? (
        <EmptyState
          title="Chưa có thông báo nào"
          hint="Bạn sẽ được nhắc khi có thẻ ghi nhớ đến hạn ôn, và khi mở khoá thành tích mới"
          action={
            <Link to="/flashcards">
              <Button type="primary">Tới bộ thẻ của tôi</Button>
            </Link>
          }
        />
      ) : (
        <>
          <div className="flex flex-col gap-2">
            {data?.content.map((n) => {
              const dich = duongDan(n.data)
              return (
                <Card
                  key={n.id}
                  size="small"
                  /* Nền nhạt cho cái chưa đọc — cùng dấu hiệu với hộp thả xuống ở chuông, để hai chỗ không
                     dạy người dùng hai quy ước khác nhau */
                  className={n.read ? '' : 'bg-brand-subtle!'}
                  onClick={() => {
                    if (!n.read) markRead.mutate(n.id)
                    if (dich) navigate(dich)
                  }}
                  hoverable={Boolean(dich) || !n.read}
                >
                  <div className="flex flex-wrap items-start gap-x-3 gap-y-1">
                    <div className="min-w-60 flex-1">
                      <Text className={`block ${n.read ? '' : 'font-bold!'}`}>{n.title}</Text>
                      {n.body && <Text className="text-ink-soft block text-sm">{n.body}</Text>}
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      <Tag className="mr-0!">{n.loaiNhan}</Tag>
                      <Text className="text-ink-soft text-xs">{khiNao(n.createdAt)}</Text>
                    </div>
                  </div>
                </Card>
              )
            })}
          </div>

          {(data?.totalPages ?? 0) > 1 && (
            <Pagination
              align="center"
              current={page + 1}
              pageSize={SO_MOI_TRANG}
              total={data?.totalElements ?? 0}
              showSizeChanger={false}
              onChange={(next) => setPage(next - 1)}
            />
          )}
        </>
      )}
    </div>
  )
}
