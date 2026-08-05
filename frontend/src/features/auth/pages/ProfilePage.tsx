import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Alert, Button, Descriptions, Space, Spin, Tag, Typography } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import PageHeader from '@/shared/components/PageHeader'
import { authApi } from '../api/authApi'
import { useAuthStore } from '../store/authStore'

const { Paragraph } = Typography

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

export default function ProfilePage() {
  const cachedUser = useAuthStore((state) => state.user)
  const setUser = useAuthStore((state) => state.setUser)

  // Gọi /users/me bằng access token để xác nhận token thật sự dùng được
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

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader title="Hồ sơ của tôi" description="Thông tin tài khoản đang đăng nhập." />

      {error && <Alert type="error" showIcon message={getApiErrorMessage(error)} />}

      <div className="border border-line bg-white p-5">
        {isPending && !cachedUser ? (
          <Spin />
        ) : (
          user && (
            <Descriptions column={1} size="small" colon={false}>
              <Descriptions.Item label={<span className="text-ink-soft">Tên hiển thị</span>}>
                <span className="font-bold">{user.displayName}</span>
              </Descriptions.Item>
              <Descriptions.Item label={<span className="text-ink-soft">Email</span>}>
                {user.email}
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
          )
        )}
      </div>

      <div className="border border-line bg-white p-5">
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
    </Space>
  )
}
