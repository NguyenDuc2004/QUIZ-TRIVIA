import { useEffect, useRef, useState } from 'react'
import { Typography } from 'antd'
import type { Role } from '../api/authApi'
import { useGoogleLogin } from '../hooks/useAuthMutations'

const { Text } = Typography

const GSI_SRC = 'https://accounts.google.com/gsi/client'
const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined

/** Phần API của Google Identity Services mà component này dùng tới. */
interface GoogleIdentity {
  accounts: {
    id: {
      initialize: (config: {
        client_id: string
        callback: (response: { credential: string }) => void
      }) => void
      renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void
    }
  }
}

declare global {
  interface Window {
    google?: GoogleIdentity
  }
}

/** Nạp script GSI một lần, dùng chung cho mọi lần render nút. */
let gsiLoader: Promise<void> | null = null

function loadGsi(): Promise<void> {
  if (window.google?.accounts?.id) {
    return Promise.resolve()
  }
  if (!gsiLoader) {
    gsiLoader = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = GSI_SRC
      script.async = true
      script.defer = true
      script.onload = () => resolve()
      script.onerror = () => {
        // Cho phép thử lại ở lần render sau thay vì kẹt mãi ở promise đã reject
        gsiLoader = null
        reject(new Error('Không tải được thư viện Google'))
      }
      document.head.appendChild(script)
    })
  }
  return gsiLoader
}

/**
 * Nút "Đăng nhập bằng Google" (FR-3).
 * <p>
 * Dùng <b>nút do Google tự vẽ</b> (`renderButton`) chứ không tự làm nút gọi API: điều khoản thương
 * hiệu của Google yêu cầu như vậy, và nút chính chủ cũng tự lo phần đa ngôn ngữ, trạng thái đăng
 * nhập sẵn, cùng những thay đổi giao diện sau này.
 * <p>
 * Thứ frontend nhận được là một <b>ID token</b>; nó được gửi thẳng cho backend xác minh chữ ký với
 * Google. Frontend không tự đọc token và cũng không tự khai người dùng là ai.
 */
export default function GoogleLoginButton({
  text = 'signin_with',
  role,
}: {
  text?: 'signin_with' | 'signup_with'
  /**
   * Vai trò mong muốn, CHỈ truyền ở trang đăng ký.
   *
   * Backend chỉ áp nó khi TẠO TÀI KHOẢN MỚI — tài khoản đã tồn tại giữ nguyên vai trò đang có. Truyền
   * ở trang đăng nhập là tạo ấn tượng sai rằng đăng nhập lại đổi được vai trò, trong khi backend bỏ qua.
   */
  role?: Role
}) {
  const containerRef = useRef<HTMLDivElement>(null)
  const [loadError, setLoadError] = useState(false)
  const googleLogin = useGoogleLogin()

  // Giữ mutation mới nhất trong ref: GSI chỉ nhận callback một lần lúc initialize
  const loginRef = useRef(googleLogin)
  loginRef.current = googleLogin

  // Vai trò cũng phải nằm trong ref, cùng lý do: callback được đăng ký MỘT LẦN lúc initialize, nên nếu
  // đọc thẳng biến `role` thì nó bị đóng băng ở giá trị lúc render đầu — người dùng đổi lựa chọn rồi bấm
  // Google sẽ gửi lên vai trò CŨ, và lỗi đó im lặng hoàn toàn.
  const roleRef = useRef(role)
  roleRef.current = role

  useEffect(() => {
    if (!CLIENT_ID) return

    let cancelled = false

    loadGsi()
      .then(() => {
        if (cancelled || !containerRef.current || !window.google) return

        window.google.accounts.id.initialize({
          client_id: CLIENT_ID,
          callback: (response) =>
            loginRef.current.mutate({ idToken: response.credential, role: roleRef.current }),
        })

        window.google.accounts.id.renderButton(containerRef.current, {
          theme: 'outline',
          size: 'large',
          width: containerRef.current.offsetWidth || 320,
          text,
          locale: 'vi',
        })
      })
      .catch(() => !cancelled && setLoadError(true))

    return () => {
      cancelled = true
    }
  }, [text])

  if (!CLIENT_ID) {
    return (
      <Text className="block text-center text-ink-soft text-xs">
        Chưa cấu hình đăng nhập Google (thiếu <code>VITE_GOOGLE_CLIENT_ID</code>)
      </Text>
    )
  }

  if (loadError) {
    return (
      <Text className="block text-center text-ink-soft text-xs">
        Không tải được đăng nhập Google. Kiểm tra kết nối mạng rồi tải lại trang.
      </Text>
    )
  }

  return <div ref={containerRef} className="flex justify-center" />
}
