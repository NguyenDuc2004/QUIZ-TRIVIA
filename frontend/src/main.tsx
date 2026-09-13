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

/**
 * Cho các hàm thông báo TĨNH (`message.success(...)`) mượn được theme đang dùng.
 *
 * `message.success()` gọi được từ mọi nơi, kể cả trong hook và hàm không phải component — đó là lý do
 * 119 lời gọi trong dự án đều dùng dạng tĩnh. Cái giá là chúng render trong một cây React riêng, ngoài
 * `ConfigProvider` ở trên, nên **không thấy theme**: giao diện đang tối thì thông báo vẫn nổi lên nền
 * sáng. Ant Design cảnh báo đúng chuyện đó ở console.
 *
 * `holderRender` là đường Ant Design mở sẵn: nó bọc cây riêng kia bằng chính `ConfigProvider` của mình.
 * Cách này sửa một chỗ, không phải đổi 119 lời gọi sang `App.useApp()` — mà nhiều lời gọi nằm trong
 * hook truy vấn, nơi không gọi hook của Ant Design được.
 *
 * Đọc chế độ từ store nên khi người dùng đổi Sáng/Tối, thông báo đổi theo.
 */
function ThemeChoThongBaoTinh({ children }: { children: React.ReactNode }) {
  const cheDo = useThemeStore((s) => s.cheDo)
  const that = quyDoi(cheDo)
  return (
    <ConfigProvider locale={viVN} theme={that === 'dark' ? darkTheme : appTheme}>
      {children}
    </ConfigProvider>
  )
}

ConfigProvider.config({
  holderRender: (children) => <ThemeChoThongBaoTinh>{children}</ThemeChoThongBaoTinh>,
})

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
