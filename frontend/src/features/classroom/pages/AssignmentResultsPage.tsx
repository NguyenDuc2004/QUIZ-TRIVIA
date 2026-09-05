import { Link, useParams } from 'react-router-dom'
import { Button, Card, Skeleton, Statistic, Table, Tag, Tooltip, Typography, message } from 'antd'
import { DownloadOutlined } from '@ant-design/icons'
import { useState } from 'react'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import { classroomApi, type AssignmentResultRow, type TrangThaiBaiTap } from '../api/classroomApi'
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
  const [dangTai, setDangTai] = useState(false)

  /**
   * Tải bảng điểm CSV (FR-58).
   *
   * Phải đi qua axios rồi tự tạo link tải, không dùng `<a href>` thẳng: request cần header `Authorization`,
   * mà thẻ `<a>` không mang được — server sẽ trả 401 và người dùng nhận một tab trắng.
   */
  const taiCsv = async () => {
    if (!id) return
    setDangTai(true)
    try {
      const blob = await classroomApi.taiBangDiemCsv(id)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `bang-diem-${data?.baiTap.title ?? 'bai-tap'}.csv`
      a.click()
      // Thu hồi ngay sau khi bấm: mỗi blob giữ nguyên bộ nhớ cho tới khi tab đóng nếu không gọi
      URL.revokeObjectURL(url)
    } catch {
      message.error('Không tải được bảng điểm')
    } finally {
      setDangTai(false)
    }
  }

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
        actions={
          <Button icon={<DownloadOutlined />} loading={dangTai} onClick={() => void taiCsv()}>
            Tải bảng điểm (CSV)
          </Button>
        }
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
        scroll={{ x: 'max-content' }}
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
          {
            // Bài tập của lớp CHẠY Ở CHẾ ĐỘ THI, nên có thu tín hiệu hành vi. Trước đây bảng này không
            // hiện gì về chúng, tức tín hiệu được ghi mà không ai thấy — bằng không ghi.
            //
            // Cột chỉ có nội dung ở bài vượt ngưỡng: máy chủ không gửi điểm của bài dưới ngưỡng, nên đây
            // không phải "ẩn cho gọn" mà là không có gì để hiện.
            title: 'Rủi ro',
            key: 'risk',
            width: 130,
            render: (_, row) => {
              if (row.riskScore === null) {
                return <Text className="text-ink-soft">—</Text>
              }
              return (
                <Tooltip title="Điểm rủi ro không phải bằng chứng gian lận. Mở bài để đọc lý do cụ thể.">
                  <div className="flex flex-col items-start gap-0.5">
                    {/* Cam chứ không đỏ: đỏ đọc thành "đã kết luận có tội", còn trạng thái thật là
                        "đáng xem". Giống hệt trang thống kê quiz — cùng một con số thì phải cùng một
                        cách nói, nếu không hai màn hình cho hai ấn tượng khác nhau về cùng một học sinh */}
                    <Tag color="orange" className="mr-0!">
                      {row.riskScore}/100
                    </Tag>
                    {row.reviewStatus !== 'PENDING' && (
                      <Text className="text-ink-soft text-xs">
                        {row.reviewStatus === 'VALID' ? 'đã xác nhận hợp lệ' : 'đã đánh dấu không hợp lệ'}
                      </Text>
                    )}
                  </div>
                </Tooltip>
              )
            },
          },
          {
            // Không có cột này thì cột Rủi ro là ngõ cụt: giáo viên thấy con số nhưng không có đường đọc
            // LÝ DO, mà lý do mới là thứ dùng được. Một con số 70/100 đứng một mình chỉ tạo nghi ngờ.
            title: '',
            key: 'mo',
            width: 90,
            render: (_, row) =>
              row.attemptId ? (
                <Link to={`/attempts/${row.attemptId}`} className="text-sm font-bold underline">
                  Xem bài
                </Link>
              ) : null,
          },
        ]}
      />
    </div>
  )
}
