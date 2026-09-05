import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Alert, Avatar, Button, Descriptions, Form, Input, Space, Spin, Tag, Typography, message } from 'antd'
import { UserOutlined } from '@ant-design/icons'
import { getApiErrorMessage } from '@/shared/api/client'
import PageHeader from '@/shared/components/PageHeader'
import ImageUploader from '@/shared/components/ImageUploader'
import { authApi } from '../api/authApi'
import { useDoiVaiTro } from '../hooks/useAuthMutations'
import { useAuthStore } from '../store/authStore'

const { Paragraph, Text } = Typography

const ROLE_LABEL: Record<string, string> = {
  LEARNER: 'Người học',
  CREATOR: 'Người tạo nội dung',
  ADMIN: 'Quản trị viên',
}

const ROLE_COLOR: Record<string, string> = {
  LEARNER: 'green',
  CREATOR: 'geekblue',
  ADMIN: 'volcano',
}

/**
 * Hồ sơ của tôi — xem và **sửa** tên hiển thị, ảnh đại diện.
 *
 * ## Ba thứ cố ý KHÔNG cho sửa ở đây
 * | Không sửa | Vì sao |
 * |---|---|
 * | **Email** | Là danh tính đăng nhập. Đổi nó cần xác minh địa chỉ mới trước, nếu không một lần gõ nhầm là mất tài khoản vĩnh viễn |
 * | **Vai trò** | Nằm trong token và quyết định quyền. Tự đổi vai trò là tự nâng quyền — việc của quản trị viên (features/10, FR-73) |
 * | **Mật khẩu** | Đã có luồng riêng qua OTP (features/01). Làm thêm một đường đổi mật khẩu ở đây là hai chỗ cùng làm một việc, và chỗ nào cũng phải tự lo phần bảo mật |
 */
export default function ProfilePage() {
  const cachedUser = useAuthStore((state) => state.user)
  const setUser = useAuthStore((state) => state.setUser)
  const queryClient = useQueryClient()

  const { data, isPending, error } = useQuery({
    queryKey: ['users', 'me'],
    queryFn: async () => {
      const profile = await authApi.me()
      setUser(profile)
      return profile
    },
  })

  const user = data ?? cachedUser
  const canCreate = user?.role === 'CREATOR' || user?.role === 'ADMIN'
  const role = user?.role
  const doiVaiTro = useDoiVaiTro()

  const [dangSua, setDangSua] = useState(false)
  const [ten, setTen] = useState('')
  const [anh, setAnh] = useState<string | null>(null)

  // Nạp lại form mỗi khi hồ sơ đổi hoặc khi mở lại chế độ sửa: mở form với dữ liệu cũ là cách chắc chắn
  // để người dùng lưu đè lên thay đổi họ vừa thực hiện ở tab khác.
  useEffect(() => {
    if (user) {
      setTen(user.displayName)
      setAnh(user.avatarUrl)
    }
  }, [user, dangSua])

  const luu = useMutation({
    mutationFn: () => authApi.updateProfile({ displayName: ten.trim(), avatarUrl: anh }),
    onSuccess: (moi) => {
      // Cập nhật store để thanh điều hướng đổi theo NGAY, không phải chờ tải lại trang — ảnh đại diện
      // hiện ở đó, nên đổi ảnh mà góc phải vẫn ảnh cũ thì người dùng tưởng lưu hỏng.
      setUser(moi)
      queryClient.setQueryData(['users', 'me'], moi)
      setDangSua(false)
      message.success('Đã cập nhật hồ sơ')
    },
    onError: (e) => message.error(getApiErrorMessage(e)),
  })

  const tenHopLe = ten.trim().length > 0 && ten.trim().length <= 100

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Hồ sơ của tôi"
        description="Thông tin tài khoản đang đăng nhập."
        actions={
          user && !dangSua ? <Button onClick={() => setDangSua(true)}>Chỉnh sửa</Button> : undefined
        }
      />

      {error && <Alert type="error" showIcon message={getApiErrorMessage(error)} />}

      <div className="soft-panel p-5">
        {isPending && !cachedUser ? (
          <Spin />
        ) : (
          user &&
          (dangSua ? (
            <Form layout="vertical">
              <Form.Item
                label="Ảnh đại diện"
                help="Không bắt buộc. Ảnh hiện ở thanh điều hướng, bảng xếp hạng và phòng đấu."
              >
                <div className="flex items-center gap-4">
                  <Avatar size={64} src={anh ?? undefined} icon={<UserOutlined />} />
                  <div className="min-w-0 flex-1">
                    <ImageUploader value={anh} onChange={setAnh} variant="avatar" />
                  </div>
                </div>
              </Form.Item>

              <Form.Item
                label="Tên hiển thị"
                validateStatus={tenHopLe ? undefined : 'error'}
                help={
                  tenHopLe
                    ? 'Tên này hiện cho người khác thấy ở bảng xếp hạng và phòng đấu.'
                    : 'Tên hiển thị không được để trống, tối đa 100 ký tự.'
                }
              >
                <Input
                  value={ten}
                  maxLength={100}
                  onChange={(e) => setTen(e.target.value)}
                  placeholder="Ví dụ: Nguyễn Văn An"
                />
              </Form.Item>

              <Space>
                <Button
                  type="primary"
                  loading={luu.isPending}
                  disabled={!tenHopLe}
                  onClick={() => luu.mutate()}
                >
                  Lưu thay đổi
                </Button>
                <Button disabled={luu.isPending} onClick={() => setDangSua(false)}>
                  Huỷ
                </Button>
              </Space>
            </Form>
          ) : (
            <div className="flex flex-wrap items-start gap-5">
              <Avatar size={80} src={user.avatarUrl ?? undefined} icon={<UserOutlined />} />

              <Descriptions column={1} size="small" colon={false} className="min-w-60 flex-1">
                <Descriptions.Item label={<span className="text-ink-soft">Tên hiển thị</span>}>
                  <span className="font-bold">{user.displayName}</span>
                </Descriptions.Item>
                <Descriptions.Item label={<span className="text-ink-soft">Email</span>}>
                  {/* Không sửa được ở đây — xem javadoc của trang */}
                  <Space size={8}>
                    {user.email}
                    <Text className="text-ink-soft text-xs">(không đổi được)</Text>
                  </Space>
                </Descriptions.Item>
                <Descriptions.Item label={<span className="text-ink-soft">Vai trò</span>}>
                  <Tag color={ROLE_COLOR[user.role]} className="mr-0!">
                    {ROLE_LABEL[user.role] ?? user.role}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label={<span className="text-ink-soft">Ngày tạo</span>}>
                  {new Date(user.createdAt).toLocaleString('vi-VN')}
                </Descriptions.Item>
              </Descriptions>
            </div>
          ))
        )}
      </div>

      <div className="soft-panel p-5">
        <Paragraph className="mb-3! font-bold!">Bắt đầu từ đâu</Paragraph>
        <Paragraph className="mb-4! text-ink-soft">
          {canCreate
            ? 'Soạn câu hỏi vào ngân hàng rồi lắp thành quiz, hoặc xem các quiz công khai.'
            : 'Khám phá các quiz công khai để luyện tập.'}
        </Paragraph>
        <Space wrap>
          <Link to="/quizzes">
            <Button type="primary">Khám phá quiz</Button>
          </Link>
          {canCreate && (
            <>
              <Link to="/my-quizzes">
                <Button>Quiz của tôi</Button>
              </Link>
              <Link to="/question-bank">
                <Button>Ngân hàng câu hỏi</Button>
              </Link>
            </>
          )}
          <Button href="/swagger-ui.html" target="_blank">
            Tài liệu API
          </Button>
        </Space>
      </div>

      {/* Đổi vai trò — chỉ hiện với LEARNER và CREATOR, KHÔNG hiện với ADMIN.

          Admin đổi vai trò bằng đường này thì hệ thống có thể mất admin cuối cùng mà không ai ngăn;
          `AdminUserService.changeRole` đã chặn đúng chuyện đó và backend cũng chặn ở đây, nhưng
          không nên bày ra một nút chỉ để báo lỗi. */}
      {role !== 'ADMIN' && (
        <div className="soft-panel p-5">
          <Paragraph className="mb-3! font-bold!">Vai trò của bạn</Paragraph>
          <Paragraph className="mb-4! text-ink-soft">
            {canCreate
              ? 'Bạn đang là Người tạo nội dung: soạn quiz, dùng ngân hàng câu hỏi và sinh đề bằng AI. Chuyển về Người học nếu chỉ muốn luyện tập.'
              : 'Bạn đang là Người học. Chuyển sang Người tạo nội dung để soạn quiz, dùng ngân hàng câu hỏi và sinh đề bằng AI — đổi lại lúc nào cũng được.'}
          </Paragraph>
          <Paragraph className="mb-4! text-ink-soft text-xs">
            Đổi vai trò sẽ <b>đăng xuất các thiết bị khác</b>. Vai trò nằm trong phiên đăng nhập, nên
            thiết bị chưa đăng nhập lại vẫn mang vai trò cũ.
          </Paragraph>
          <Button
            type="primary"
            loading={doiVaiTro.isPending}
            onClick={() => doiVaiTro.mutate(canCreate ? 'LEARNER' : 'CREATOR')}
          >
            {canCreate ? 'Chuyển về Người học' : 'Trở thành Người tạo nội dung'}
          </Button>
        </div>
      )}
    </Space>
  )
}
