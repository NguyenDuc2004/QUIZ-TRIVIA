import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import { Button, Card, Form, Input, Radio, Typography } from 'antd'
import { useRegister } from '../hooks/useAuthMutations'
import { registerSchema, type RegisterForm } from '../schema'

const { Title, Paragraph } = Typography

export default function RegisterPage() {
  const register = useRegister()
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: { displayName: '', email: '', password: '', confirmPassword: '', role: 'LEARNER' },
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <Card className="w-full max-w-md">
        <Title level={3} className="!mb-1">
          Tạo tài khoản
        </Title>
        <Paragraph type="secondary">Học và thi đấu cùng trợ lý AI</Paragraph>

        <Form
          layout="vertical"
          onFinish={handleSubmit(({ confirmPassword: _confirm, ...body }) => register.mutate(body))}
        >
          <Form.Item
            label="Tên hiển thị"
            validateStatus={errors.displayName && 'error'}
            help={errors.displayName?.message}
          >
            <Controller
              name="displayName"
              control={control}
              render={({ field }) => <Input {...field} size="large" placeholder="Nguyễn Văn A" />}
            />
          </Form.Item>

          <Form.Item label="Email" validateStatus={errors.email && 'error'} help={errors.email?.message}>
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
            label="Nhập lại mật khẩu"
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

          <Form.Item label="Bạn muốn dùng hệ thống để">
            <Controller
              name="role"
              control={control}
              render={({ field }) => (
                <Radio.Group {...field}>
                  <Radio value="LEARNER">Học &amp; thi đấu</Radio>
                  <Radio value="CREATOR">Tạo quiz, sinh đề AI</Radio>
                </Radio.Group>
              )}
            />
          </Form.Item>

          <Button type="primary" htmlType="submit" size="large" block loading={register.isPending}>
            Đăng ký
          </Button>
        </Form>

        <Paragraph className="!mt-4 !mb-0 text-center">
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </Paragraph>
      </Card>
    </div>
  )
}
