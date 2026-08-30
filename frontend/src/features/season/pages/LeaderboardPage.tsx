import { Alert, Avatar, Card, Skeleton, Table, Tag, Typography } from 'antd'
import { TrophyOutlined, UserOutlined } from '@ant-design/icons'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import { useLeaderboard, useSeasonHistory } from '../hooks/useSeason'
import type { LeaderboardRow, SeasonHistoryItem } from '../api/seasonApi'

const { Text } = Typography

/**
 * Bảng xếp hạng theo mùa (features/15).
 *
 * Điểm mùa là **tổng XP kiếm được trong khoảng thời gian mùa** — không phải XP toàn thời gian. Trang nói rõ
 * điều đó vì hai con số khác nhau và người dùng sẽ so chúng với nhau.
 *
 * Chỉ có phạm vi toàn hệ thống: xếp hạng theo lớp cần tính năng 14 (chưa làm), còn "theo bạn bè" không tồn
 * tại ở bất kỳ đâu trong dự án. Không làm bộ lọc rỗng cho đủ mặt.
 */
/**
 * Màu ba hạng. Dùng token màu của Ant Design chứ không hardcode mã màu (ui-design-system.md §2).
 *
 * Vàng > Bạc > Đồng theo đúng thứ tự trực giác của huy chương, để người dùng không phải học một quy ước mới.
 */
const MAU_HANG: Record<'DONG' | 'BAC' | 'VANG', string> = {
  VANG: 'gold',
  BAC: 'default',
  DONG: 'orange',
}

export default function LeaderboardPage() {
  const { data, isLoading } = useLeaderboard(20)
  const { data: history } = useSeasonHistory()

  if (isLoading || !data) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }

  const conLai = Math.max(
    0,
    Math.ceil((new Date(data.ketThuc).getTime() - Date.now()) / 86_400_000),
  )

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={`Bảng xếp hạng — ${data.tenMua}`}
        description={
          <span>
            Điểm mùa là XP bạn kiếm được <b>trong mùa này</b>, không phải tổng XP từ đầu.{' '}
            {conLai > 0 ? `Còn ${conLai} ngày nữa hết mùa.` : 'Mùa đang chờ chốt.'}
          </span>
        }
      />

      {/* Thứ hạng của mình đặt trên cùng: đó là thứ người dùng mở trang này để xem */}
      {data.thuHangCuaToi ? (
        <Card>
          <div className="flex items-center gap-4">
            <div className="text-center">
              <div className="text-brand-strong text-3xl font-extrabold">
                #{data.thuHangCuaToi.rank}
              </div>
              <Text className="text-ink-soft text-xs">/ {data.soNguoiThamGia} người</Text>
            </div>
            <div className="min-w-0">
              <Text className="font-bold!">Thứ hạng của bạn</Text>
              <div className="text-ink-soft text-sm">
                {data.thuHangCuaToi.score} điểm mùa
              </div>
            </div>
          </div>
        </Card>
      ) : (
        /* Chưa có điểm KHÁC hạng cuối — nói đúng trạng thái thay vì hiện một con số hạng sai */
        <Alert
          type="info"
          showIcon
          message="Bạn chưa có điểm trong mùa này"
          description="Làm một bài quiz hoặc ôn thẻ ghi nhớ để vào bảng xếp hạng. Mọi XP kiếm được trong mùa đều tính."
        />
      )}

      <Card title={`Top ${data.top.length} · ${data.soNguoiThamGia} người tham gia`}>
        {data.top.length === 0 ? (
          <EmptyState
            title="Chưa ai có điểm trong mùa này"
            hint="Người đầu tiên làm bài sẽ đứng đầu bảng."
          />
        ) : (
          <Table<LeaderboardRow>
            rowKey="userId"
            dataSource={data.top}
            pagination={false}
            size="small"
            rowClassName={(row) =>
              row.userId === data.thuHangCuaToi?.userId ? 'bg-surface-subtle' : ''
            }
            columns={[
              {
                title: '#',
                dataIndex: 'rank',
                width: 70,
                render: (rank: number) =>
                  /* Top 3 có huy chương riêng từng hạng.
                     Trước đây hạng 1 vàng còn hạng 2 và 3 dùng chung một màu xám, nên vị trí thứ nhì
                     và thứ ba nhìn không khác gì nhau — trong khi cả bảng xếp hạng tồn tại để phân
                     biệt đúng những khác biệt đó. */
                  rank <= 3 ? (
                    <span className={`podium podium-${rank}`}>
                      <TrophyOutlined />
                      <span className="font-bold">{rank}</span>
                    </span>
                  ) : (
                    <Text className="text-ink-soft">{rank}</Text>
                  ),
              },
              {
                title: 'Người học',
                dataIndex: 'displayName',
                render: (ten: string, row) => (
                  <span className="flex items-center gap-2">
                    <Avatar size={24} src={row.avatarUrl ?? undefined} icon={<UserOutlined />} />
                    <Text
                      className={row.userId === data.thuHangCuaToi?.userId ? 'font-bold!' : ''}
                    >
                      {ten}
                      {row.userId === data.thuHangCuaToi?.userId && ' (bạn)'}
                    </Text>
                  </span>
                ),
              },
              {
                // FR-64. Cột này TRỐNG khi mùa chưa đủ 10 người — trống là đúng, không phải thiếu dữ liệu:
                // trao huy hiệu Vàng cho người đứng đầu trong ba người làm mất giá đúng huy hiệu đó.
                title: 'Hạng',
                dataIndex: 'nhanHang',
                width: 100,
                align: 'center',
                render: (nhan: string | null, row: LeaderboardRow) =>
                  nhan ? (
                    <Tag color={MAU_HANG[row.phanHang ?? 'DONG']} className="mr-0!">
                      {nhan}
                    </Tag>
                  ) : (
                    <Text className="text-ink-soft">—</Text>
                  ),
              },
              {
                title: 'Điểm mùa',
                dataIndex: 'score',
                width: 110,
                align: 'right',
                render: (score: number) => <Text className="font-bold!">{score}</Text>,
              },
            ]}
          />
        )}
      </Card>

      {(history?.length ?? 0) > 0 && (
        <Card title="Mùa đã kết thúc">
          <Table<SeasonHistoryItem>
            rowKey="seasonId"
            dataSource={history}
            pagination={false}
            size="small"
            columns={[
              { title: 'Mùa', dataIndex: 'tenMua' },
              {
                title: 'Kết thúc',
                dataIndex: 'ketThuc',
                width: 130,
                render: (v: string) => (
                  <Text className="text-ink-soft text-xs">
                    {new Date(v).toLocaleDateString('vi-VN')}
                  </Text>
                ),
              },
              {
                title: 'Hạng',
                dataIndex: 'finalRank',
                width: 90,
                render: (rank: number | null) =>
                  rank == null ? (
                    <Text className="text-ink-soft text-xs">không tham gia</Text>
                  ) : (
                    <Text className="font-bold!">#{rank}</Text>
                  ),
              },
              { title: 'Điểm', dataIndex: 'finalScore', width: 90 },
              {
                title: 'Phần thưởng',
                dataIndex: 'tenHuyHieu',
                width: 180,
                render: (ten: string | null, row) =>
                  ten == null ? (
                    <Text className="text-ink-soft text-xs">—</Text>
                  ) : (
                    <Tag>
                      {row.iconHuyHieu} {ten}
                    </Tag>
                  ),
              },
            ]}
          />
        </Card>
      )}
    </div>
  )
}
