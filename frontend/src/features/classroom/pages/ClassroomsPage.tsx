import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Card, Form, Input, Modal, Tag, Typography } from 'antd'
import { PlusOutlined, TeamOutlined } from '@ant-design/icons'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import { useAuthStore } from '@/features/auth/store/authStore'
import type { Classroom } from '../api/classroomApi'
import { useCreateClassroom, useJoinClassroom, useMyClassrooms } from '../hooks/useClassroom'

const { Text, Paragraph } = Typography

/**
 * Lớp của tôi (features/14, FR-54).
 *
 * Chia **hai nhóm** theo vai trò chứ không trộn một danh sách: lớp tôi dạy và lớp tôi học là hai việc khác
 * nhau hoàn toàn — một bên tôi giao bài, một bên tôi làm bài. Trộn lại thì mỗi thẻ phải tự giải thích nó
 * thuộc loại nào, và người vừa dạy vừa học (chuyện bình thường) sẽ phải đọc từng thẻ để biết.
 */
export default function ClassroomsPage() {
  const { data, isPending } = useMyClassrooms()
  const user = useAuthStore((state) => state.user)
  const canCreate = user?.role === 'CREATOR' || user?.role === 'ADMIN'

  const [moTao, setMoTao] = useState(false)
  const [moVao, setMoVao] = useState(false)

  const dayHoc = (data ?? []).filter((l) => l.vaiTroCuaToi !== 'STUDENT')
  const dangHoc = (data ?? []).filter((l) => l.vaiTroCuaToi === 'STUDENT')

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Lớp học"
        description="Lớp bạn dạy và lớp bạn đang tham gia."
        actions={
          <div className="flex gap-2">
            <Button icon={<TeamOutlined />} onClick={() => setMoVao(true)}>
              Vào lớp bằng mã
            </Button>
            {/* Nút tạo lớp chỉ hiện với CREATOR/ADMIN — backend cũng chặn, đây là lớp thứ hai để người
                học không bấm vào rồi mới nhận 403 */}
            {canCreate && (
              <Button type="primary" icon={<PlusOutlined />} onClick={() => setMoTao(true)}>
                Tạo lớp
              </Button>
            )}
          </div>
        }
      />

      {isPending ? (
        <Card loading />
      ) : (data?.length ?? 0) === 0 ? (
        <EmptyState
          title="Bạn chưa ở trong lớp nào"
          hint={
            canCreate
              ? 'Tạo một lớp rồi phát mã cho học sinh, hoặc vào lớp của người khác bằng mã'
              : 'Xin mã lớp từ giáo viên rồi bấm "Vào lớp bằng mã"'
          }
          action={
            <Button type="primary" onClick={() => (canCreate ? setMoTao(true) : setMoVao(true))}>
              {canCreate ? 'Tạo lớp đầu tiên' : 'Vào lớp bằng mã'}
            </Button>
          }
        />
      ) : (
        <>
          {dayHoc.length > 0 && <Nhom tieuDe="Lớp tôi dạy" lops={dayHoc} />}
          {dangHoc.length > 0 && <Nhom tieuDe="Lớp tôi học" lops={dangHoc} />}
        </>
      )}

      <HopTaoLop mo={moTao} dong={() => setMoTao(false)} />
      <HopVaoLop mo={moVao} dong={() => setMoVao(false)} />
    </div>
  )
}

function Nhom({ tieuDe, lops }: { tieuDe: string; lops: Classroom[] }) {
  return (
    <div>
      <Text className="mb-3 block font-bold!">
        {tieuDe} ({lops.length})
      </Text>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {lops.map((lop) => (
          <Link key={lop.id} to={`/classrooms/${lop.id}`}>
            <Card hoverable className="h-full">
              <div className="mb-1 flex items-start justify-between gap-2">
                <Text className="font-bold!">{lop.name}</Text>
                {lop.vaiTroCuaToi === 'CO_TEACHER' && (
                  <Tag color="blue" className="mr-0!">
                    Trợ giảng
                  </Tag>
                )}
              </div>

              {lop.description && (
                <Paragraph className="text-ink-soft mb-2! text-sm" ellipsis={{ rows: 2 }}>
                  {lop.description}
                </Paragraph>
              )}

              <div className="text-ink-soft flex flex-wrap gap-x-3 text-xs">
                <span>{lop.soThanhVien} thành viên</span>
                <span>{lop.soBaiTap} bài tập</span>
                {lop.vaiTroCuaToi === 'STUDENT' && <span>GV: {lop.ownerName}</span>}
              </div>

              {/* Mã lớp chỉ có với giáo viên — máy chủ trả null cho học sinh */}
              {lop.classCode && (
                <div className="border-line mt-3 border-t pt-2">
                  <Text className="text-ink-soft text-xs">Mã lớp</Text>
                  <Text className="ml-2 font-mono font-bold! tracking-widest">{lop.classCode}</Text>
                </div>
              )}
            </Card>
          </Link>
        ))}
      </div>
    </div>
  )
}

function HopTaoLop({ mo, dong }: { mo: boolean; dong: () => void }) {
  const [form] = Form.useForm()
  const tao = useCreateClassroom()

  return (
    <Modal
      title="Tạo lớp mới"
      open={mo}
      onCancel={dong}
      okText="Tạo lớp"
      confirmLoading={tao.isPending}
      onOk={() =>
        form.validateFields().then((v) => {
          tao.mutate(v, { onSuccess: () => { form.resetFields(); dong() } })
        })
      }
    >
      <Form form={form} layout="vertical" className="pt-2">
        <Form.Item
          name="name"
          label="Tên lớp"
          rules={[{ required: true, message: 'Nhập tên lớp' }, { max: 150, message: 'Tối đa 150 ký tự' }]}
        >
          <Input placeholder="Ví dụ: Toán 12A1 — năm học 2026" autoFocus />
        </Form.Item>
        <Form.Item name="description" label="Mô tả (không bắt buộc)">
          <Input.TextArea rows={2} placeholder="Lớp này học gì, ai học" />
        </Form.Item>
        <Text className="text-ink-soft text-xs">
          Hệ thống sẽ sinh một mã lớp 6 ký tự để bạn phát cho học sinh.
        </Text>
      </Form>
    </Modal>
  )
}

function HopVaoLop({ mo, dong }: { mo: boolean; dong: () => void }) {
  const [ma, setMa] = useState('')
  const vao = useJoinClassroom()

  return (
    <Modal
      title="Vào lớp bằng mã"
      open={mo}
      onCancel={dong}
      okText="Vào lớp"
      okButtonProps={{ disabled: ma.trim().length !== 6 }}
      confirmLoading={vao.isPending}
      onOk={() => vao.mutate(ma, { onSuccess: () => { setMa(''); dong() } })}
    >
      <div className="flex flex-col gap-2 pt-2">
        <Input
          value={ma}
          /* Tự viết hoa: mã in trên bảng luôn là chữ hoa, còn bàn phím điện thoại thì mặc định chữ
             thường — bắt người dùng tự bật Shift là một bước thừa để họ gõ sai */
          onChange={(e) => setMa(e.target.value.toUpperCase())}
          maxLength={6}
          placeholder="VD: K7M2PQ"
          className="text-center font-mono text-lg tracking-[0.4em]"
          autoFocus
        />
        <Text className="text-ink-soft text-xs">
          Mã gồm 6 ký tự, do giáo viên cung cấp. Mã không chứa số 0, 1 hay chữ O, I, L.
        </Text>
      </div>
    </Modal>
  )
}
