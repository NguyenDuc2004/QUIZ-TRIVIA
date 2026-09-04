import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alert, Button, Form, Input, Steps, Typography, message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { authApi } from '../api/authApi'
import ThemeToggle from '@/shared/components/ThemeToggle'

const { Title, Paragraph, Text } = Typography

/** Giãn cách xin lại mã, khớp với `app.mail.otp.resend-cooldown-seconds` ở backend. */
const RESEND_COOLDOWN_SEC = 60

/**
 * Quên mật khẩu qua OTP email (FR-4) — hai bước trên cùng một trang.
 * <p>
 * Sau bước 1, giao diện <b>không</b> nói "đã gửi mã tới email này" theo kiểu khẳng định email tồn
 * tại. Backend cố tình trả kết quả giống nhau dù có tài khoản hay không, để không ai dò được danh
 * sách người dùng; giao diện phải nói cùng một giọng, nếu không thì công sức ở backend thành vô ích.
 */
export default function ForgotPasswordPage() {
  const navigate = useNavigate()

  const [step, setStep] = useState(0)
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [sending, setSending] = useState(false)
  const [resetting, setResetting] = useState(false)
  const [cooldown, setCooldown] = useState(0)

  useEffect(() => {
    if (cooldown <= 0) return
    const id = window.setInterval(() => setCooldown((value) => Math.max(0, value - 1)), 1000)
    return () => window.clearInterval(id)
  }, [cooldown])

  const requestOtp = async () => {
    setSending(true)
    try {
      await authApi.forgotPassword(email.trim())
      setStep(1)
      setCooldown(RESEND_COOLDOWN_SEC)
    } catch (error) {
      message.error(getApiErrorMessage(error))
    } finally {
      setSending(false)
    }
  }

  const submitReset = async () => {
    if (newPassword !== confirmPassword) {
      message.error('Hai lần nhập mật khẩu không khớp')
      return
    }
    setResetting(true)
    try {
      await authApi.resetPassword({ email: email.trim(), otp: otp.trim(), newPassword })
      message.success('Đã đổi mật khẩu. Hãy đăng nhập lại.')
      navigate('/login', { replace: true })
    } catch (error) {
      message.error(getApiErrorMessage(error))
    } finally {
      setResetting(false)
    }
  }

  const emailLooksValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-surface-subtle p-4">
      {/* Nút đổi giao diện cho trang khách.

          Bốn trang này nằm NGOÀI cả hai layout nên không có thanh điều hướng, tức trước bản này người
          chưa đăng nhập không có đường nào đổi giao diện — và trang đăng nhập lại đúng là trang đầu
          tiên họ thấy. Đặt ở góc trên phải, `absolute` để không đẩy khối nội dung đang căn giữa. */}
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>
      <div className="w-full max-w-md border border-line bg-surface p-8">
        <div className="mb-1 flex items-center justify-center gap-1">
          <span className="text-2xl font-extrabold text-ink">Quiz</span>
          <span className="text-2xl font-extrabold text-brand">AI</span>
        </div>
        <Title level={3} className="mb-1! text-center! font-bold!">
          Quên mật khẩu
        </Title>
        <Paragraph className="mb-6! text-center text-ink-soft text-xs">
          Lấy lại tài khoản bằng mã gửi tới email
        </Paragraph>

        <Steps
          size="small"
          current={step}
          className="mb-6!"
          items={[{ title: 'Nhập email' }, { title: 'Nhập mã & mật khẩu mới' }]}
        />

        {step === 0 ? (
          <Form layout="vertical">
            <Form.Item label={<span className="font-bold">Email tài khoản</span>}>
              <Input
                size="large"
                autoComplete="email"
                placeholder="ban@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                onPressEnter={() => emailLooksValid && requestOtp()}
              />
            </Form.Item>

            <Button
              type="primary"
              size="large"
              block
              loading={sending}
              disabled={!emailLooksValid}
              onClick={requestOtp}
            >
              Gửi mã xác thực
            </Button>
          </Form>
        ) : (
          <Form layout="vertical">
            {/* Câu chữ cố tình trung lập: không xác nhận email này có tài khoản hay không */}
            <Alert
              className="mb-4"
              type="info"
              showIcon
              message="Kiểm tra hòm thư của bạn"
              description={
                <>
                  Nếu <Text code>{email.trim()}</Text> có tài khoản, chúng tôi vừa gửi một mã gồm 6
                  chữ số tới đó. Mã có hiệu lực 10 phút. Nhớ xem cả mục Spam.
                </>
              }
            />

            <Form.Item label={<span className="font-bold">Mã xác thực</span>}>
              <Input
                size="large"
                maxLength={6}
                inputMode="numeric"
                placeholder="123456"
                className="text-center font-mono text-xl tracking-[0.5em]"
                value={otp}
                // Mã chỉ có chữ số — lọc luôn để dán nhầm cũng không sao
                onChange={(event) => setOtp(event.target.value.replace(/[^0-9]/g, ''))}
              />
            </Form.Item>

            <Form.Item
              label={<span className="font-bold">Mật khẩu mới</span>}
              help="Tối thiểu 8 ký tự"
            >
              <Input.Password
                size="large"
                autoComplete="new-password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
              />
            </Form.Item>

            <Form.Item label={<span className="font-bold">Nhập lại mật khẩu mới</span>}>
              <Input.Password
                size="large"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                onPressEnter={submitReset}
              />
            </Form.Item>

            <Button
              type="primary"
              size="large"
              block
              loading={resetting}
              disabled={otp.length !== 6 || newPassword.length < 8}
              onClick={submitReset}
            >
              Đặt lại mật khẩu
            </Button>

            <div className="mt-3 flex items-center justify-between">
              <Button type="link" className="px-0!" onClick={() => setStep(0)}>
                Đổi email khác
              </Button>
              <Button
                type="link"
                className="px-0!"
                disabled={cooldown > 0 || sending}
                onClick={requestOtp}
              >
                {cooldown > 0 ? `Gửi lại sau ${cooldown}s` : 'Gửi lại mã'}
              </Button>
            </div>

            <Text className="mt-2 block text-ink-soft text-xs">
              Đặt lại mật khẩu sẽ đăng xuất tài khoản khỏi mọi thiết bị.
            </Text>
          </Form>
        )}

        <div className="mt-6 border-t border-line pt-4 text-center text-sm">
          Nhớ ra mật khẩu rồi?{' '}
          <Link to="/login" className="font-bold underline">
            Đăng nhập
          </Link>
        </div>
      </div>
    </div>
  )
}
