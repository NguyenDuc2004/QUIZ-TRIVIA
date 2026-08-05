import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import { Button, Card, Form, Input, Typography } from 'antd'
import { useLogin } from '../hooks/useAuthMutations'
import { loginSchema, type LoginForm } from '../schema'

const { Title, Paragraph } = Typography

export default function LoginPage() {
  const login = useLogin()
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <Card className="w-full max-w-md">
        <Title level={3} className="!mb-1">
          Đăng nhập
        </Title>
        <Paragraph type="secondary">Quiz/Trivia tích hợp AI</Paragraph>

        {/* Validation do React Hook Form + Zod đảm nhiệm, antd chỉ lo phần hiển thị */}
        <Form layout="vertical" onFinish={handleSubmit((values) => login.mutate(values))}>
          <Form.Item
            label="Email"
            validateStatus={errors.email && 'error'}
            help={errors.email?.message}
          >
            <Controller
              name="email"
              control={control}
              render={({ field }) => (
                <Input {...field} size="large" autoComplete="email" placeholder="ban@example.com" />
              )}
            />
          </Form.Item>

          <Form.Item
            label="Mật khẩu"
            validateStatus={errors.password && 'error'}
            help={errors.password?.message}
          >
            <Controller
              name="password"
              control={control}
              render={({ field }) => (
                <Input.Password {...field} size="large" autoComplete="current-password" placeholder="••••••••" />
              )}
            />
          </Form.Item>

          <Button type="primary" htmlType="submit" size="large" block loading={login.isPending}>
            Đăng nhập
          </Button>
        </Form>

        <Paragraph className="!mt-4 !mb-0 text-center">
          Chưa có tài khoản? <Link to="/register">Đăng ký</Link>
        </Paragraph>
      </Card>
    </div>
  )
}
