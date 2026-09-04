import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import { Button, Form, Input, Radio, Typography } from 'antd'
import GoogleLoginButton from '../components/GoogleLoginButton'
import { useRegister } from '../hooks/useAuthMutations'
import { registerSchema, type RegisterForm } from '../schema'

const { Title, Paragraph } = Typography

export default function RegisterPage() {
  const register = useRegister()
  const {
    control,
    handleSubmit,
    // `watch` để nút Google đọc được vai trò người dùng vừa chọn — nút đó nằm ngoài luồng submit của
    // form nên không nhận được giá trị qua handleSubmit.
    watch,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      displayName: '',
      email: '',
      password: '',
      confirmPassword: '',
      role: 'LEARNER',
    },
  })

  const boldLabel = (text: string) => <span className="font-bold">{text}</span>

  return (
    <div className="flex min-h-screen items-center justify-center bg-surface-subtle p-4">
      <div className="w-full max-w-md border border-line bg-surface p-8">
        <div className="mb-1 flex items-center justify-center gap-1">
          <span className="text-2xl font-extrabold text-ink">Quiz</span>
          <span className="text-2xl font-extrabold text-brand">AI</span>
        </div>
        <Title level={3} className="mb-1! text-center! font-bold!">
          Tạo tài khoản
        </Title>
        <Paragraph className="mb-6! text-center text-ink-soft text-xs">
          Miễn phí — bắt đầu học và tạo quiz ngay
        </Paragraph>

        <Form
          layout="vertical"
          onFinish={handleSubmit(({ confirmPassword: _confirm, ...body }) => register.mutate(body))}
        >
          <Form.Item
            label={boldLabel('Tên hiển thị')}
            validateStatus={errors.displayName && 'error'}
            help={errors.displayName?.message}
          >
            <Controller
              name="displayName"
              control={control}
              render={({ field }) => <Input {...field} size="large" placeholder="Nguyễn Văn A" />}
            />
          </Form.Item>

          <Form.Item
            label={boldLabel('Email')}
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
            label={boldLabel('Mật khẩu')}
            validateStatus={errors.password && 'error'}
            help={errors.password?.message ?? 'Tối thiểu 8 ký tự'}
          >
            <Controller
              name="password"
              control={control}
              render={({ field }) => (
                <Input.Password {...field} size="large" autoComplete="new-password" />
              )}
            />
          </Form.Item>

          <Form.Item
            label={boldLabel('Nhập lại mật khẩu')}
            validateStatus={errors.confirmPassword && 'error'}
            help={errors.confirmPassword?.message}
          >
            <Controller
              name="confirmPassword"
              control={control}
              render={({ field }) => (
                <Input.Password {...field} size="large" autoComplete="new-password" />
              )}
            />
          </Form.Item>

          <Form.Item label={boldLabel('Bạn muốn dùng hệ thống để')}>
            <Controller
              name="role"
              control={control}
              render={({ field }) => (
                <Radio.Group
                  {...field}
                  optionType="button"
                  buttonStyle="solid"
                  options={[
                    { value: 'LEARNER', label: 'Học & thi đấu' },
                    { value: 'CREATOR', label: 'Tạo quiz, sinh đề AI' },
                  ]}
                />
              )}
            />
          </Form.Item>

          <Button type="primary" htmlType="submit" size="large" block loading={register.isPending}>
            Đăng ký
          </Button>
        </Form>

        <div className="my-5 flex items-center gap-3">
          <div className="h-px flex-1 bg-line" />
          <span className="text-ink-soft text-xs">hoặc</span>
          <div className="h-px flex-1 bg-line" />
        </div>

        {/* Truyền vai trò người dùng vừa chọn ở trên. Trước đây không truyền, nên lựa chọn đó bị bỏ
            qua trong im lặng: chọn "Tạo quiz, sinh đề AI" rồi bấm Google vẫn vào với vai trò Người học. */}
        <GoogleLoginButton text="signup_with" role={watch('role')} />

        <div className="mt-6 border-t border-line pt-4 text-center text-sm">
          Đã có tài khoản?{' '}
          <Link to="/login" className="font-bold underline">
            Đăng nhập
          </Link>
        </div>
      </div>
    </div>
  )
}
