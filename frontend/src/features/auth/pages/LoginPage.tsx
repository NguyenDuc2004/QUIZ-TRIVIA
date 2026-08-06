import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import { Button, Form, Input, Typography } from 'antd'
import GoogleLoginButton from '../components/GoogleLoginButton'
import { useLogin } from '../hooks/useAuthMutations'
import { loginSchema, type LoginForm } from '../schema'

const { Title, Paragraph } = Typography

/** Trang đăng nhập — khối giữa trang, viền mảnh, nút đen full-width (docs/ui-design-system.md). */
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
    <div className="flex min-h-screen items-center justify-center bg-surface-subtle p-4">
      <div className="w-full max-w-md border border-line bg-white p-8">
        <div className="mb-1 flex items-center justify-center gap-1">
          <span className="text-2xl font-extrabold text-ink">Quiz</span>
          <span className="text-2xl font-extrabold text-brand">AI</span>
        </div>
        <Title level={3} className="mb-1! text-center! font-bold!">
          Đăng nhập
        </Title>
        <Paragraph className="mb-6! text-center text-ink-soft text-xs">
          Học và thi đấu cùng trợ lý AI
        </Paragraph>

        {/* Validation do React Hook Form + Zod đảm nhiệm, antd chỉ lo phần hiển thị */}
        <Form layout="vertical" onFinish={handleSubmit((values) => login.mutate(values))}>
          <Form.Item
            label={<span className="font-bold">Email</span>}
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
            label={<span className="font-bold">Mật khẩu</span>}
            validateStatus={errors.password && 'error'}
            help={errors.password?.message}
          >
            <Controller
              name="password"
              control={control}
              render={({ field }) => (
                <Input.Password
                  {...field}
                  size="large"
                  autoComplete="current-password"
                  placeholder="••••••••"
                />
              )}
            />
          </Form.Item>

          <div className="mb-3 text-right">
            <Link to="/forgot-password" className="text-sm font-bold">
              Quên mật khẩu?
            </Link>
          </div>

          <Button type="primary" htmlType="submit" size="large" block loading={login.isPending}>
            Đăng nhập
          </Button>
        </Form>

        <div className="my-5 flex items-center gap-3">
          <div className="h-px flex-1 bg-line" />
          <span className="text-ink-soft text-xs">hoặc</span>
          <div className="h-px flex-1 bg-line" />
        </div>

        <GoogleLoginButton text="signin_with" />

        <div className="mt-6 border-t border-line pt-4 text-center text-sm">
          Chưa có tài khoản?{' '}
          <Link to="/register" className="font-bold underline">
            Đăng ký
          </Link>
        </div>
      </div>
    </div>
  )
}
