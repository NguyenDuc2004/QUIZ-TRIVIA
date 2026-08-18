import { Link, useNavigate } from 'react-router-dom'
import { Button, Card, Skeleton, Tag, Typography } from 'antd'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import type { Assignment, TrangThaiBaiTap } from '../api/classroomApi'
import { useMyAssignments, useStartAssignment } from '../hooks/useClassroom'

const { Text, Paragraph } = Typography

/**
 * Màu thẻ trạng thái.
 *
 * `QUA_HAN` và `NOP_TRE` cố ý **khác màu nhau**: một cái là "bạn còn phải làm", một cái là "xong rồi, chỉ
 * muộn". Dùng chung màu đỏ thì học sinh đã nộp muộn vẫn thấy một dấu đỏ y hệt người chưa làm gì.
 */
const MAU: Record<TrangThaiBaiTap, string> = {
  CHUA_LAM: 'default',
  DANG_LAM: 'processing',
  DA_NOP: 'success',
  NOP_TRE: 'warning',
  QUA_HAN: 'error',
}

/** Bài tập được giao cho tôi, ở mọi lớp (features/14, FR-56). */
export default function MyAssignmentsPage() {
  const { data, isPending } = useMyAssignments()
  const batDau = useStartAssignment()
  const navigate = useNavigate()

  const canLam = (data ?? []).filter(
    (b) => b.trangThai === 'CHUA_LAM' || b.trangThai === 'DANG_LAM' || b.trangThai === 'QUA_HAN',
  )

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Bài tập của tôi"
        description={
          canLam.length > 0
            ? `${canLam.length} bài chưa nộp`
            : 'Bạn đã nộp hết bài được giao'
        }
      />

      {isPending ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : (data?.length ?? 0) === 0 ? (
        <EmptyState
          title="Chưa có bài tập nào"
          hint="Bài tập sẽ hiện ở đây khi giáo viên giao cho lớp bạn"
          action={
            <Link to="/classrooms">
              <Button type="primary">Tới lớp học của tôi</Button>
            </Link>
          }
        />
      ) : (
        <div className="flex flex-col gap-3">
          {data?.map((bai) => (
            <TheBaiTap
              key={bai.id}
              bai={bai}
              dangBatDau={batDau.isPending}
              onLam={() =>
                batDau.mutate(bai.id, {
                  onSuccess: (attemptId) => navigate(`/attempts/${attemptId}`),
                })
              }
              onXem={() => navigate(`/attempts/${bai.attemptId}`)}
            />
          ))}
        </div>
      )}
    </div>
  )
}

function TheBaiTap({
  bai,
  dangBatDau,
  onLam,
  onXem,
}: {
  bai: Assignment
  dangBatDau: boolean
  onLam: () => void
  onXem: () => void
}) {
  const daNop = bai.trangThai === 'DA_NOP' || bai.trangThai === 'NOP_TRE'
  const quaHan = bai.trangThai === 'QUA_HAN'

  return (
    <Card size="small" className={daNop ? '' : 'border-brand/40!'}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-60 flex-1">
          <div className="mb-1 flex flex-wrap items-center gap-2">
            <Text className="font-bold!">{bai.title}</Text>
            <Tag color={MAU[bai.trangThai ?? 'CHUA_LAM']} className="mr-0!">
              {bai.trangThaiNhan}
            </Tag>
          </div>

          <Text className="text-ink-soft block text-sm">
            {bai.tenLop} · {bai.quizTitle} · {bai.soCau} câu
          </Text>

          {bai.instruction && (
            <Paragraph className="mb-0! mt-1 text-sm" ellipsis={{ rows: 2 }}>
              {bai.instruction}
            </Paragraph>
          )}

          <Text className="text-ink-soft mt-1 block text-xs">
            {bai.dueAt ? `Hạn nộp: ${new Date(bai.dueAt).toLocaleString('vi-VN')}` : 'Không có hạn nộp'}
            {daNop && bai.diem !== null && ` · Điểm: ${bai.diem}/${bai.diemToiDa}`}
          </Text>
        </div>

        <div className="shrink-0">
          {daNop ? (
            <Button onClick={onXem}>Xem bài làm</Button>
          ) : (
            <Button type="primary" loading={dangBatDau} onClick={onLam}>
              {bai.trangThai === 'DANG_LAM' ? 'Làm tiếp' : 'Làm bài'}
            </Button>
          )}
        </div>
      </div>

      {/* Nói rõ quá hạn vẫn nộp được. Không nói thì học sinh thấy thẻ đỏ và tưởng đã mất cơ hội —
          rồi thôi không làm, trong khi giáo viên vẫn muốn nhận bài */}
      {quaHan && (
        <Text className="text-ink-soft mt-2 block text-xs">
          Đã quá hạn, nhưng bạn vẫn nộp được. Bài sẽ được đánh dấu là nộp muộn.
        </Text>
      )}
    </Card>
  )
}
