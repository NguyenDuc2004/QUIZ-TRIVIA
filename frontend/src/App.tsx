import { Route, Routes } from 'react-router-dom'
import { Button, Card, Result, Space, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import axios from 'axios'

const { Title, Paragraph, Text } = Typography

/** Trang tạm để xác nhận FE ↔ BE thông nhau. Sẽ thay bằng trang thật ở slice Auth. */
function HomePage() {
  const { data, isPending, isError } = useQuery({
    queryKey: ['backend-health'],
    queryFn: async () => {
      const res = await axios.get('/actuator/health')
      return res.data as { status: string }
    },
  })

  return (
    <div className="mx-auto max-w-3xl p-8">
      <Space direction="vertical" size="large" className="w-full">
        <div>
          <Title level={2}>Quiz/Trivia tích hợp AI</Title>
          <Paragraph type="secondary">
            Đồ án tốt nghiệp — Trường ĐH Công nghiệp Hà Nội
          </Paragraph>
        </div>

        <Card title="Trạng thái backend">
          {isPending && <Text>Đang kiểm tra kết nối…</Text>}
          {isError && (
            <Text type="danger">
              Chưa gọi được backend. Chạy <code>cd backend &amp;&amp; ./mvnw spring-boot:run</code> rồi tải lại trang.
            </Text>
          )}
          {data && (
            <Text type="success">
              Backend phản hồi: <b>{data.status}</b>
            </Text>
          )}
        </Card>

        <Card title="Các bước tiếp theo">
          <Paragraph>
            Khung dự án đã sẵn sàng. Tính năng đầu tiên: <b>Xác thực &amp; phân quyền</b>.
          </Paragraph>
          <Button type="primary" href="/swagger-ui.html" target="_blank">
            Mở tài liệu API
          </Button>
        </Card>
      </Space>
    </div>
  )
}

function NotFoundPage() {
  return <Result status="404" title="404" subTitle="Không tìm thấy trang." />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
