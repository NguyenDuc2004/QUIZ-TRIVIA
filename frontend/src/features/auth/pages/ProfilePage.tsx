import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Alert, Button, Card, Descriptions, Space, Tag, Typography } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { authApi } from '../api/authApi'
import { useAuthStore } from '../store/authStore'

const { Title, Paragraph } = Typography

const ROLE_LABEL: Record<string, string> = {
  LEARNER: 'Người học',
  CREATOR: 'Người tạo nội dung',
  ADMIN: 'Quản trị viên',
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
      <Title level={3} className="!mb-0">
        Hồ sơ của tôi
      </Title>

      {error && <Alert type="error" showIcon message={getApiErrorMessage(error)} />}

      <Card loading={isPending && !cachedUser}>
        {user && (
          <Descriptions column={1} size="small">
            <Descriptions.Item label="Tên hiển thị">{user.displayName}</Descriptions.Item>
            <Descriptions.Item label="Email">{user.email}</Descriptions.Item>
            <Descriptions.Item label="Vai trò">
              <Tag color={user.role === 'ADMIN' ? 'volcano' : user.role === 'CREATOR' ? 'geekblue' : 'green'}>
                {ROLE_LABEL[user.role] ?? user.role}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Ngày tạo">
              {new Date(user.createdAt).toLocaleString('vi-VN')}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Card>

      <Card title="Bắt đầu từ đâu">
        <Paragraph className="!mb-3">
          {canCreate
            ? 'Bạn có thể soạn câu hỏi vào ngân hàng rồi lắp thành quiz, hoặc xem các quiz công khai.'
            : 'Khám phá các quiz công khai để luyện tập.'}
        </Paragraph>
        <Space wrap>
          <Link to="/quizzes">
            <Button>Khám phá quiz</Button>
          </Link>
          {canCreate && (
            <>
              <Link to="/my-quizzes">
                <Button type="primary">Quiz của tôi</Button>
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
      </Card>
    </Space>
  )
}
