import { Alert, Button, Popconfirm, Table, Tag, Typography } from 'antd'
import { StopOutlined } from '@ant-design/icons'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import { useCloseRoom, useLiveRooms } from '../hooks/useAdmin'
import type { LiveRoom } from '../api/adminApi'

const { Text } = Typography

/**
 * Giám sát phòng đấu đang chạy (FR-81, FR-82).
 *
 * Phòng đấu là phần duy nhất của hệ thống có trạng thái sống ở **hai nơi**: metadata bền ở PostgreSQL,
 * trạng thái đang chơi ở Redis kèm TTL. Khi hai nơi lệch nhau — bản ghi còn mà trạng thái Redis đã hết
 * hạn — phòng đó treo: nó hiện trong danh sách nhưng không ai chơi được. Không có trang này thì cách duy
 * nhất phát hiện là chờ người dùng báo.
 */
export default function AdminRoomsPage() {
  const { data, isLoading } = useLiveRooms()
  const close = useCloseRoom()

  const soTreo = data?.filter((room) => room.treo).length ?? 0

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Phòng đấu đang chạy"
        description="Tự làm mới mỗi 5 giây. Chỉ hiện phòng đang chờ hoặc đang chơi."
      />

      {soTreo > 0 && (
        <Alert
          type="warning"
          showIcon
          message={`${soTreo} phòng đang treo`}
          description="Bản ghi phòng còn nhưng trạng thái ở Redis đã mất — người chơi không vào được. Đóng phòng để dọn."
        />
      )}

      <Table<LiveRoom>
        scroll={{ x: 'max-content' }}
        rowKey="id"
        loading={isLoading}
        dataSource={data}
        pagination={false}
        locale={{
          emptyText: (
            <EmptyState
              title="Không có phòng nào đang chạy"
              hint="Phòng sẽ hiện ở đây ngay khi có người mở."
            />
          ),
        }}
        columns={[
          {
            title: 'Mã PIN',
            dataIndex: 'roomCode',
            width: 110,
            render: (code: string) => <Text className="font-mono font-bold!">{code}</Text>,
          },
          {
            title: 'Quiz',
            dataIndex: 'tenQuiz',
            render: (tenQuiz: string, row) => (
              <div className="min-w-0">
                <Text className="font-bold!">{tenQuiz}</Text>
                <div className="text-ink-soft text-xs">
                  Chủ phòng: {row.tenChuPhong}
                  {row.choKhachVao && ' · cho khách vào'}
                </div>
              </div>
            ),
          },
          {
            title: 'Trạng thái',
            dataIndex: 'status',
            width: 150,
            render: (status: LiveRoom['status'], row) => {
              if (row.treo) {
                return <Tag color="red">Treo</Tag>
              }
              return status === 'WAITING' ? (
                <Tag color="blue">Đang chờ</Tag>
              ) : (
                <Tag color="green">
                  Đang chơi
                  {row.cauHienTai != null && row.tongSoCau != null
                    ? ` · câu ${row.cauHienTai}/${row.tongSoCau}`
                    : ''}
                </Tag>
              )
            },
          },
          {
            title: 'Người chơi',
            dataIndex: 'soNguoiChoi',
            width: 110,
            // null nghĩa là không đọc được trạng thái Redis, không phải "0 người" — hiện dấu gạch để
            // không nói sai rằng phòng đang trống
            render: (soNguoiChoi: number | null) =>
              soNguoiChoi == null ? (
                <Text className="text-ink-soft text-xs">không rõ</Text>
              ) : (
                soNguoiChoi
              ),
          },
          {
            title: 'Mở lúc',
            dataIndex: 'taoLuc',
            width: 110,
            render: (taoLuc: string) => (
              <Text className="text-ink-soft text-xs">
                {new Date(taoLuc).toLocaleTimeString('vi-VN', {
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </Text>
            ),
          },
          {
            title: '',
            width: 120,
            render: (_, row) => (
              <Popconfirm
                title={`Đóng phòng ${row.roomCode}?`}
                description={
                  row.treo
                    ? 'Phòng đang treo, đóng để dọn khỏi danh sách.'
                    : 'Người đang chơi sẽ bị dừng ván. Điểm đã ghi vẫn giữ.'
                }
                okText="Đóng phòng"
                cancelText="Thôi"
                okButtonProps={{ danger: true, loading: close.isPending }}
                onConfirm={() => close.mutate(row.roomCode)}
              >
                <Button size="small" danger icon={<StopOutlined />}>
                  Đóng
                </Button>
              </Popconfirm>
            ),
          },
        ]}
      />
    </div>
  )
}
