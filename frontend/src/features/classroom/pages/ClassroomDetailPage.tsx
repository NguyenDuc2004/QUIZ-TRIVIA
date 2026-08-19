import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  Alert,
  Button,
  DatePicker,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Skeleton,
  Table,
  Tabs,
  Tag,
  Typography,
} from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import { useQuizList } from '@/features/quiz/hooks/useQuizQueries'
import type { Assignment, Member } from '../api/classroomApi'
import {
  useAssignQuiz,
  useChangeMemberRole,
  useClassAssignments,
  useClassroom,
  useDeleteAssignment,
  useDeleteClassroom,
  useMembers,
  useRemoveMember,
} from '../hooks/useClassroom'

const { Text, Paragraph } = Typography

/**
 * Một lớp học (features/14, FR-55 & FR-57).
 *
 * Trang này hiện **khác nhau theo vai trò**, và vai trò lấy từ `vaiTroCuaToi` mà máy chủ trả — không tự so
 * `ownerId` với id của mình. Cùng một câu hỏi mà hai đầu tự trả lời thì sớm muộn hai câu trả lời lệch nhau,
 * và lệch ở đây nghĩa là học sinh thấy nút giao bài.
 */
export default function ClassroomDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data: lop, isPending, isError } = useClassroom(id)
  const laGiaoVien = lop?.vaiTroCuaToi === 'OWNER' || lop?.vaiTroCuaToi === 'CO_TEACHER'

  const { data: baiTaps } = useClassAssignments(id)
  const { data: thanhVien } = useMembers(id, laGiaoVien)
  const xoaLop = useDeleteClassroom()
  const navigate = useNavigate()
  const [moGiao, setMoGiao] = useState(false)

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }
  if (isError || !lop) {
    return (
      <EmptyState
        title="Không mở được lớp này"
        hint="Lớp không tồn tại, hoặc bạn không ở trong lớp đó"
        action={<Link to="/classrooms">Về danh sách lớp</Link>}
      />
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={lop.name}
        description={
          <>
            <Link to="/classrooms">Lớp học</Link> · {lop.soThanhVien} thành viên
            {lop.vaiTroCuaToi === 'STUDENT' && <> · GV: {lop.ownerName}</>}
          </>
        }
        actions={
          laGiaoVien && (
            <div className="flex gap-2">
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setMoGiao(true)}>
                Giao bài
              </Button>
              {/* Xoá lớp CHỈ chủ nhiệm — trợ giảng không thấy nút, và backend cũng trả 403 */}
              {lop.vaiTroCuaToi === 'OWNER' && (
                <Popconfirm
                  title="Xoá lớp này?"
                  description="Mọi bài tập của lớp sẽ bị gỡ. Bài làm và điểm của học sinh vẫn còn."
                  okText="Xoá lớp"
                  okButtonProps={{ danger: true }}
                  cancelText="Thôi"
                  onConfirm={() =>
                    xoaLop.mutate(lop.id, { onSuccess: () => navigate('/classrooms') })
                  }
                >
                  <Button danger icon={<DeleteOutlined />}>
                    Xoá lớp
                  </Button>
                </Popconfirm>
              )}
            </div>
          )
        }
      />

      {lop.description && <Paragraph className="mb-0!">{lop.description}</Paragraph>}

      {/* Mã lớp đặt nổi bật: đây là thứ giáo viên mở trang này để lấy, không phải thứ giấu trong menu */}
      {lop.classCode && (
        <Alert
          type="info"
          showIcon
          message={
            <span>
              Mã lớp:{' '}
              <Text className="font-mono text-base font-bold! tracking-widest">{lop.classCode}</Text>
            </span>
          }
          description="Đọc mã này cho học sinh; các em vào mục Lớp học rồi bấm “Vào lớp bằng mã”."
        />
      )}

      <Tabs
        items={[
          {
            key: 'assignments',
            label: `Bài tập (${baiTaps?.length ?? 0})`,
            children: (
              <BangBaiTap
                baiTaps={baiTaps ?? []}
                classroomId={lop.id}
                laGiaoVien={laGiaoVien}
              />
            ),
          },
          ...(laGiaoVien
            ? [
                {
                  key: 'members',
                  label: `Thành viên (${thanhVien?.length ?? 0})`,
                  children: (
                    <BangThanhVien
                      thanhVien={thanhVien ?? []}
                      classroomId={lop.id}
                      laChuNhiem={lop.vaiTroCuaToi === 'OWNER'}
                    />
                  ),
                },
              ]
            : []),
        ]}
      />

      {laGiaoVien && (
        <HopGiaoBai classroomId={lop.id} mo={moGiao} dong={() => setMoGiao(false)} />
      )}
    </div>
  )
}

function BangBaiTap({
  baiTaps,
  classroomId,
  laGiaoVien,
}: {
  baiTaps: Assignment[]
  classroomId: string
  laGiaoVien: boolean
}) {
  const xoa = useDeleteAssignment(classroomId)

  if (baiTaps.length === 0) {
    return (
      <EmptyState
        title="Lớp chưa có bài tập nào"
        hint={laGiaoVien ? 'Bấm “Giao bài” để gán một quiz của bạn cho lớp' : 'Giáo viên chưa giao bài'}
      />
    )
  }

  return (
    <Table<Assignment>
      rowKey="id"
      dataSource={baiTaps}
      pagination={{ pageSize: 20, hideOnSinglePage: true }}
      columns={[
        {
          title: 'Bài tập',
          dataIndex: 'title',
          render: (title: string, row) => (
            <div className="min-w-0">
              <Text className="font-bold!">{title}</Text>
              <div className="text-ink-soft text-xs">
                {row.quizTitle} · {row.soCau} câu
              </div>
            </div>
          ),
        },
        {
          title: 'Hạn nộp',
          dataIndex: 'dueAt',
          width: 180,
          render: (dueAt: string | null) =>
            dueAt ? (
              new Date(dueAt).toLocaleString('vi-VN')
            ) : (
              // "Không có hạn" là một trạng thái hợp lệ, không phải thiếu dữ liệu
              <Text className="text-ink-soft">Không có hạn</Text>
            ),
        },
        ...(laGiaoVien
          ? [
              {
                title: '',
                key: 'actions',
                width: 150,
                render: (_: unknown, row: Assignment) => (
                  <div className="flex items-center gap-3">
                    <Link to={`/assignments/${row.id}/results`} className="text-sm font-bold">
                      Theo dõi
                    </Link>
                    <Popconfirm
                      title="Gỡ bài tập này?"
                      description="Bài làm của học sinh vẫn còn, chỉ thôi thuộc về bài tập."
                      okText="Gỡ"
                      okButtonProps={{ danger: true }}
                      cancelText="Thôi"
                      onConfirm={() => xoa.mutate(row.id)}
                    >
                      <Button size="small" danger type="text" icon={<DeleteOutlined />} />
                    </Popconfirm>
                  </div>
                ),
              },
            ]
          : []),
      ]}
    />
  )
}

function BangThanhVien({
  thanhVien,
  classroomId,
  laChuNhiem,
}: {
  thanhVien: Member[]
  classroomId: string
  laChuNhiem: boolean
}) {
  const doiVaiTro = useChangeMemberRole(classroomId)
  const xoa = useRemoveMember(classroomId)

  if (thanhVien.length === 0) {
    return (
      <EmptyState
        title="Chưa có ai vào lớp"
        hint="Đọc mã lớp ở trên cho học sinh để các em tự vào"
      />
    )
  }

  return (
    <Table<Member>
      rowKey="userId"
      dataSource={thanhVien}
      pagination={{ pageSize: 50, hideOnSinglePage: true }}
      columns={[
        {
          title: 'Học sinh',
          dataIndex: 'displayName',
          render: (ten: string, row) => (
            <div className="min-w-0">
              <Text className="font-bold!">{ten}</Text>
              {/* Email để phân biệt hai học sinh trùng tên — trong lớp thật chuyện đó xảy ra thường xuyên */}
              <div className="text-ink-soft text-xs">{row.email}</div>
            </div>
          ),
        },
        {
          title: 'Vai trò',
          dataIndex: 'role',
          width: 170,
          render: (role: Member['role'], row) =>
            laChuNhiem ? (
              <Select
                size="small"
                value={role}
                className="w-full"
                onChange={(moi) => doiVaiTro.mutate({ userId: row.userId, role: moi })}
                options={[
                  { value: 'STUDENT', label: 'Học sinh' },
                  { value: 'CO_TEACHER', label: 'Trợ giảng' },
                ]}
              />
            ) : (
              <Tag className="mr-0!">{row.vaiTroNhan}</Tag>
            ),
        },
        {
          title: 'Vào lớp',
          dataIndex: 'joinedAt',
          width: 130,
          render: (v: string) => new Date(v).toLocaleDateString('vi-VN'),
        },
        ...(laChuNhiem
          ? [
              {
                title: '',
                key: 'actions',
                width: 60,
                render: (_: unknown, row: Member) => (
                  <Popconfirm
                    title={`Xoá ${row.displayName} khỏi lớp?`}
                    description="Bài làm của em ấy vẫn còn."
                    okText="Xoá"
                    okButtonProps={{ danger: true }}
                    cancelText="Thôi"
                    onConfirm={() => xoa.mutate(row.userId)}
                  >
                    <Button size="small" danger type="text" icon={<DeleteOutlined />} />
                  </Popconfirm>
                ),
              },
            ]
          : []),
      ]}
    />
  )
}

function HopGiaoBai({
  classroomId,
  mo,
  dong,
}: {
  classroomId: string
  mo: boolean
  dong: () => void
}) {
  const [form] = Form.useForm()
  const giao = useAssignQuiz(classroomId)
  // `mine: true` — chỉ quiz CỦA MÌNH. Backend cũng chặn giao quiz người khác, nên danh sách này khớp
  // đúng thứ gửi được: người dùng không chọn được một thứ rồi mới nhận lỗi.
  const { data: quizzes } = useQuizList({ mine: true, size: 100 })

  return (
    <Modal
      title="Giao bài cho lớp"
      open={mo}
      onCancel={dong}
      okText="Giao bài"
      confirmLoading={giao.isPending}
      onOk={() =>
        form.validateFields().then((v) => {
          giao.mutate(
            {
              quizId: v.quizId,
              title: v.title,
              instruction: v.instruction,
              openAt: v.openAt?.toISOString(),
              dueAt: v.dueAt?.toISOString(),
            },
            { onSuccess: () => { form.resetFields(); dong() } },
          )
        })
      }
    >
      <Form form={form} layout="vertical" className="pt-2">
        <Form.Item name="quizId" label="Quiz" rules={[{ required: true, message: 'Chọn một quiz' }]}>
          <Select
            showSearch
            optionFilterProp="label"
            placeholder="Chọn quiz của bạn"
            options={(quizzes?.content ?? []).map((q) => ({
              value: q.id,
              label: `${q.title} (${q.questionCount} câu)`,
              disabled: q.questionCount === 0,
            }))}
          />
        </Form.Item>
        <Form.Item
          name="title"
          label="Tiêu đề bài tập"
          rules={[{ required: true, message: 'Nhập tiêu đề' }]}
        >
          <Input placeholder="Ví dụ: Bài tập tuần 3 — Đạo hàm" />
        </Form.Item>
        <Form.Item name="instruction" label="Hướng dẫn (không bắt buộc)">
          <Input.TextArea rows={2} placeholder="Dặn dò thêm cho học sinh" />
        </Form.Item>

        <div className="grid gap-3 sm:grid-cols-2">
          <Form.Item name="openAt" label="Mở lúc" help="Bỏ trống = mở ngay">
            <DatePicker showTime className="w-full" placeholder="Mở ngay" />
          </Form.Item>
          <Form.Item name="dueAt" label="Hạn nộp" help="Bỏ trống = không có hạn">
            <DatePicker showTime className="w-full" placeholder="Không có hạn" />
          </Form.Item>
        </div>

        <Text className="text-ink-soft text-xs">
          Bài tập chạy ở chế độ thi: mỗi học sinh làm một lần, và hệ thống ghi nhận tín hiệu hành vi như mọi
          bài thi khác. Quá hạn vẫn nộp được và sẽ được đánh dấu nộp muộn.
        </Text>
      </Form>
    </Modal>
  )
}
