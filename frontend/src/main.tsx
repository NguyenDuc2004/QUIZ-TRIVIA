import { StrictMode, useEffect, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider } from 'antd'
import viVN from 'antd/locale/vi_VN'
import '@fontsource-variable/inter'
import App from './App.tsx'
import { appTheme, darkTheme } from './shared/theme/antdTheme'
import { apCheDo, cheDoDaLuu, quyDoi, useThemeStore } from './shared/theme/themeStore'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 30_000 },
  },
})

/*
 * Áp chế độ màu NGAY, trước khi React render lần đầu.
 *
 * Làm trong `useEffect` là quá muộn: trang sẽ vẽ xong một khung sáng rồi mới nhảy sang tối, và cái nháy
 * trắng đó đúng là thứ người bật chế độ tối muốn tránh nhất — nhất là khi mở web trong phòng tối.
 */
apCheDo(cheDoDaLuu())

/**
 * Bọc ngoài để chọn cấu hình Ant Design theo chế độ đang dùng.
 *
 * Phải là component chứ không phải một hằng số: `ConfigProvider` cần render lại khi người dùng đổi chế
 * độ, và nó chỉ làm được điều đó nếu chế độ là state của React.
 */
function KhungGiaoDien({ children }: { children: React.ReactNode }) {
  const cheDo = useThemeStore((s) => s.cheDo)
  const [that, setThat] = useState(() => quyDoi(cheDo))

  useEffect(() => {
    setThat(apCheDo(cheDo))
  }, [cheDo])

  useEffect(() => {
    // Chỉ theo dõi hệ điều hành khi người dùng CHỌN "theo hệ thống". Đã chọn tay Sáng hoặc Tối thì
    // lựa chọn đó thắng, kể cả khi hệ điều hành đổi sau đó.
    if (cheDo !== 'system') {
      return
    }
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const doi = () => setThat(apCheDo('system'))
    mq.addEventListener('change', doi)
    return () => mq.removeEventListener('change', doi)
  }, [cheDo])

  return (
    <ConfigProvider locale={viVN} theme={that === 'dark' ? darkTheme : appTheme}>
      {children}
    </ConfigProvider>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      {/* Token giao diện tập trung ở shared/theme/antdTheme.ts (docs/ui-design-system.md) */}
      <KhungGiaoDien>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </KhungGiaoDien>
    </QueryClientProvider>
  </StrictMode>,
)
