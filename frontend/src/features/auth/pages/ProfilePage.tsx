import { useQuery } from '@tanstack/react-query'
import { Alert, Button, Card, Descriptions, Space, Tag, Typography } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { authApi } from '../api/authApi'
import { useLogout } from '../hooks/useAuthMutations'
import { useAuthStore } from '../store/authStore'

const { Title, Paragraph } = Typography

const ROLE_LABEL: Record<string, string> = {
  LEARNER: 'Người học',
  CREATOR: 'Người tạo nội dung',
  ADMIN: 'Quản trị viên',
}

/** Trang tạm sau khi đăng nhập — chứng minh luồng token hoạt động. */
export default function ProfilePage() {
  const cachedUser = useAuthStore((state) => state.user)
  const setUser = useAuthStore((state) => state.setUser)
  const logout = useLogout()

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

  return (
    <div className="mx-auto max-w-2xl p-8">
      <Space direction="vertical" size="large" className="w-full">
        <div>
          <Title level={2} className="!mb-1">
            Quiz/Trivia tích hợp AI
          </Title>
          <Paragraph type="secondary">Đồ án tốt nghiệp — Trường ĐH Công nghiệp Hà Nội</Paragraph>
        </div>

        {error && <Alert type="error" showIcon message={getApiErrorMessage(error)} />}

        <Card
          title="Hồ sơ của tôi"
          loading={isPending && !cachedUser}
          extra={
            <Button danger loading={logout.isPending} onClick={() => logout.mutate()}>
              Đăng xuất
            </Button>
          }
        >
          {user && (
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Tên hiển thị">{user.displayName}</Descriptions.Item>
              <Descriptions.Item label="Email">{user.email}</Descriptions.Item>
              <Descriptions.Item label="Vai trò">
                <Tag color={user.role === 'CREATOR' ? 'geekblue' : 'green'}>
                  {ROLE_LABEL[user.role] ?? user.role}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Ngày tạo">
                {new Date(user.createdAt).toLocaleString('vi-VN')}
              </Descriptions.Item>
            </Descriptions>
          )}
        </Card>

        <Card title="Tiếp theo">
          <Paragraph className="!mb-2">
            Xác thực &amp; phân quyền đã xong. Chức năng kế tiếp: <b>Quản lý Quiz &amp; Câu hỏi</b>.
          </Paragraph>
          <Button href="/swagger-ui.html" target="_blank">
            Mở tài liệu API
          </Button>
        </Card>
      </Space>
    </div>
  )
}
