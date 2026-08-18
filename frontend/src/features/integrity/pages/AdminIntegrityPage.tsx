import { useState } from 'react'
import { Alert, Button, Segmented, Skeleton, Table, Tag, Typography } from 'antd'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import { useFlaggedAttempts, useIntegrityReport } from '../hooks/useIntegrity'
import IntegrityCard from '../components/IntegrityCard'
import type { IntegrityReport, ReviewStatus } from '../api/integrityApi'

const { Text } = Typography

/**
 * Hàng chờ rà soát bài thi bị gắn cờ, trong khu quản trị (features/12, FR-47).
 *
 * Mặc định chỉ hiện bài **chưa ai xem**: đây là hàng chờ việc, không phải danh sách lịch sử. Xem lại bài đã
 * kết luận là việc khác, và để lẫn hai thứ thì danh sách dài ra mỗi ngày mà không ai biết còn bao nhiêu việc
 * thật sự phải làm.
 */
export default function AdminIntegrityPage() {
  const [status, setStatus] = useState<ReviewStatus>('PENDING')
  const [page, setPage] = useState(0)
  const [dangMo, setDangMo] = useState<string | undefined>()

  const { data, isLoading } = useFlaggedAttempts({ status, page, size: 20 })
  const { data: chiTiet } = useIntegrityReport(dangMo)

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Rà soát tính toàn vẹn"
        description="Bài thi có nhiều tín hiệu hành vi đáng xem. Chỉ bài ở chế độ thi xuất hiện ở đây."
        actions={
          <Segmented
            value={status}
            onChange={(v) => {
              setStatus(v as ReviewStatus)
              setPage(0)
              setDangMo(undefined)
            }}
            options={[
              { label: 'Chờ rà soát', value: 'PENDING' },
              { label: 'Đã xác nhận hợp lệ', value: 'VALID' },
              { label: 'Không hợp lệ', value: 'INVALID' },
            ]}
          />
        }
      />

      {/* Nhắc ngay đầu trang, trước khi người rà soát nhìn thấy bất kỳ con số nào */}
      <Alert
        type="warning"
        showIcon
        message="Điểm rủi ro không phải bằng chứng gian lận"
        description="Tín hiệu thu từ trình duyệt nên có thể bị chặn hoặc giả mạo, và mỗi tín hiệu đều có cách giải thích vô hại. Hãy đọc lý do cụ thể và cân nhắc hoàn cảnh trước khi kết luận. Hệ thống không tự động xử lý bài nào."
      />

      {isLoading ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : (
        <Table<IntegrityReport>
          rowKey="attemptId"
          dataSource={data?.content}
          pagination={{
            current: page + 1,
            pageSize: data?.size ?? 20,
            total: data?.totalElements ?? 0,
            showSizeChanger: false,
            onChange: (next) => setPage(next - 1),
          }}
          locale={{
            emptyText: (
              <EmptyState
                title={
                  status === 'PENDING'
                    ? 'Không có bài nào chờ rà soát'
                    : 'Chưa có bài nào ở trạng thái này'
                }
                hint={
                  status === 'PENDING'
                    ? 'Bài thi chỉ xuất hiện ở đây khi điểm rủi ro vượt ngưỡng 60.'
                    : undefined
                }
              />
            ),
          }}
          columns={[
            {
              title: 'Điểm rủi ro',
              dataIndex: 'riskScore',
              width: 110,
              render: (diem: number) => (
                <Tag color={diem >= 80 ? 'volcano' : 'gold'}>{diem}/100</Tag>
              ),
            },
            {
              title: 'Bài thi',
              dataIndex: 'tenQuiz',
              render: (ten: string, row) => (
                <div className="min-w-0">
                  <Text className="font-bold!">{ten}</Text>
                  <div className="text-ink-soft text-xs">{row.tenNguoiLam}</div>
                </div>
              ),
            },
            {
              title: 'Lý do',
              dataIndex: 'flags',
              render: (flags: string[]) => (
                <div className="flex flex-col gap-0.5">
                  {flags.map((f, i) => (
                    <Text key={i} className="text-xs">
                      {f}
                    </Text>
                  ))}
                </div>
              ),
            },
            {
              title: 'AI',
              dataIndex: 'aiNote',
              width: 90,
              // null = chưa gọi hoặc gọi thất bại; khác chuỗi rỗng. Phân biệt để người rà soát biết có nhận
              // định để đọc hay không, thay vì mở ra thấy trống rồi tưởng AI không thấy gì.
              render: (note: string | null) =>
                note ? <Tag color="purple">có</Tag> : <Text className="text-ink-soft text-xs">—</Text>,
            },
            {
              title: '',
              width: 90,
              render: (_, row) => (
                <Button
                  size="small"
                  onClick={() => setDangMo(dangMo === row.attemptId ? undefined : row.attemptId)}
                >
                  {dangMo === row.attemptId ? 'Đóng' : 'Xem'}
                </Button>
              ),
            },
          ]}
        />
      )}

      {chiTiet && <IntegrityCard report={chiTiet} />}
    </div>
  )
}
