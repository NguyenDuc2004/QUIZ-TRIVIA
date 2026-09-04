import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useSearchParams } from 'react-router-dom'
import { Alert, Button, Form, Input, Typography } from 'antd'
import GoogleLoginButton from '../components/GoogleLoginButton'
import { useLogin } from '../hooks/useAuthMutations'
import { loginSchema, type LoginForm } from '../schema'

const { Title, Paragraph } = Typography

/** Trang đăng nhập — khối giữa trang, viền mảnh, nút đen full-width (docs/ui-design-system.md). */
export default function LoginPage() {
  const login = useLogin()
  // `?expired=1` do axios interceptor gắn khi phiên hết hạn. Bị đưa về đây mà không được nói vì sao
  // thì người dùng tưởng hệ thống lỗi — nhất là khi họ đang làm dở việc gì.
  const [searchParams] = useSearchParams()
  const sessionExpired = searchParams.get('expired') === '1'
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
      <div className="w-full max-w-md border border-line bg-surface p-8">
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

        {sessionExpired && (
          <Alert
            type="warning"
            showIcon
            className="mb-4"
            message="Phiên đăng nhập đã hết"
            // Nói đúng thứ được giữ: mỗi câu được lưu lên server ngay khi chọn/rời ô, nên bài dở làm
            // tiếp được. Hứa "không mất gì" thì sai — chữ đang gõ mà chưa rời ô thì vẫn mất.
            description="Đăng nhập lại để tiếp tục. Những câu bạn đã trả lời vẫn được lưu."
          />
        )}

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
