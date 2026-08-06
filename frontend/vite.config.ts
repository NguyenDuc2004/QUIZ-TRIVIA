import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  // sockjs-client viết cho môi trường Node nên đọc thẳng biến `global`, thứ không tồn tại trong
  // trình duyệt. Thiếu phần này, `import SockJS` ném ReferenceError ngay lúc nạp module và làm
  // trắng TOÀN BỘ ứng dụng — không riêng trang phòng đấu.
  define: {
    global: 'globalThis',
  },
  optimizeDeps: {
    // Phải khai lại cho esbuild: `define` ở trên chỉ áp cho mã nguồn của mình, không áp cho
    // phần dependency được Vite tiền biên dịch (nơi sockjs-client thực sự nằm).
    esbuildOptions: {
      define: { global: 'globalThis' },
    },
  },
  server: {
    port: 5173,
    // Nghe trên mọi card mạng chứ không chỉ localhost, để điện thoại cùng Wi-Fi quét QR vào được.
    // Mặc định của Vite chỉ bind 127.0.0.1 nên máy khác gọi tới sẽ không có ai trả lời.
    host: true,
    // Proxy khi dev để FE gọi API cùng origin → không vướng CORS
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws': { target: 'http://localhost:8080', changeOrigin: true, ws: true },
      '/actuator': { target: 'http://localhost:8080', changeOrigin: true },
      // Ảnh người dùng tải lên do backend phục vụ tĩnh
      '/uploads': { target: 'http://localhost:8080', changeOrigin: true },
      '/swagger-ui': { target: 'http://localhost:8080', changeOrigin: true },
      '/v3/api-docs': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
