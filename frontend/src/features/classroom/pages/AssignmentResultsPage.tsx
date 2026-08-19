import { Link, useParams } from 'react-router-dom'
import { Card, Skeleton, Statistic, Table, Tag, Typography } from 'antd'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import type { AssignmentResultRow, TrangThaiBaiTap } from '../api/classroomApi'
import { useAssignmentResults } from '../hooks/useClassroom'

const { Text } = Typography

const MAU: Record<TrangThaiBaiTap, string> = {
  CHUA_LAM: 'default',
  DANG_LAM: 'processing',
  DA_NOP: 'success',
  NOP_TRE: 'warning',
  QUA_HAN: 'error',
}

/**
 * Bảng theo dõi lớp cho một bài tập (features/14, FR-57).
 *
 * Có **một dòng cho mỗi thành viên**, kể cả người chưa làm — đó chính là câu hỏi giáo viên mở trang này để
 * trả lời. Chỉ liệt kê người đã nộp thì bảng "theo dõi" biến thành bảng "điểm", và *ai chưa làm* không có
 * chỗ nào nói.
 */
export default function AssignmentResultsPage() {
  const { id } = useParams<{ id: string }>()
  const { data, isPending, isError } = useAssignmentResults(id)

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }
  if (isError || !data) {
    return (
      <EmptyState
        title="Không mở được bảng theo dõi"
        hint="Bài tập không tồn tại, hoặc bạn không dạy lớp này"
        action={<Link to="/classrooms">Về danh sách lớp</Link>}
      />
    )
  }

  const { baiTap } = data

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={baiTap.title}
        description={
          <>
            <Link to={`/classrooms/${baiTap.classroomId}`}>{baiTap.tenLop}</Link> · {baiTap.quizTitle} ·{' '}
            {baiTap.dueAt
              ? `hạn ${new Date(baiTap.dueAt).toLocaleString('vi-VN')}`
              : 'không có hạn nộp'}
          </>
        }
      />

      <div className="grid gap-4 sm:grid-cols-4">
        <Card>
          <Statistic title="Thành viên" value={data.soThanhVien} />
        </Card>
        <Card>
          <Statistic title="Đã nộp" value={data.soDaNop} suffix={`/ ${data.soThanhVien}`} />
        </Card>
        <Card>
          <Statistic title="Nộp muộn" value={data.soNopTre} />
        </Card>
        <Card>
          {/* Trung bình tính trên BÀI ĐÃ NỘP — nói rõ để không ai đọc nhầm thành trung bình cả lớp */}
          <Statistic
            title="Điểm trung bình"
            value={data.diemTrungBinh ?? '—'}
            formatter={data.diemTrungBinh === null ? () => '—' : undefined}
          />
          <Text className="text-ink-soft text-xs">tính trên bài đã nộp</Text>
        </Card>
      </div>

      <Table<AssignmentResultRow>
        rowKey="userId"
        dataSource={data.danhSach}
        pagination={{ pageSize: 50, hideOnSinglePage: true }}
        columns={[
          { title: 'Học sinh', dataIndex: 'tenHocSinh' },
          {
            title: 'Trạng thái',
            dataIndex: 'trangThai',
            width: 170,
            render: (tt: TrangThaiBaiTap, row) => (
              <Tag color={MAU[tt]} className="mr-0!">
                {row.trangThaiNhan}
              </Tag>
            ),
          },
          {
            title: 'Điểm',
            dataIndex: 'diem',
            width: 110,
            render: (diem: number | null, row) =>
              // null = chưa nộp, KHÁC 0 điểm. Hiện "0" ở đây là nói sai về một học sinh chưa làm bài.
              diem === null ? (
                <Text className="text-ink-soft">—</Text>
              ) : (
                <Text className="font-bold!">
                  {diem}/{row.diemToiDa}
                </Text>
              ),
          },
          {
            title: 'Nộp lúc',
            dataIndex: 'nopLuc',
            width: 170,
            render: (v: string | null) =>
              v ? new Date(v).toLocaleString('vi-VN') : <Text className="text-ink-soft">—</Text>,
          },
        ]}
      />
    </div>
  )
}
