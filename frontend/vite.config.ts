// Cấu hình cho vitest nằm ở `vitest.config.ts` riêng, KHÔNG nhồi trường `test` vào đây: vitest 3 chưa
// hỗ trợ Vite 8 nên nó tự cài một bản vite riêng trong node_modules của mình, và type plugin của hai
// bản không tương thích (rolldown vs rollup) — nhồi vào đây thì `tsc -b` đổ ở danh sách plugins.
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import path from 'node:path'

// https://vite.dev/config/
/**
 * Địa chỉ backend cho proxy khi chạy dev.
 *
 * Đọc từ biến môi trường thay vì gắn cứng vào sáu dòng bên dưới: cổng 8080 là cổng phổ biến nên trên máy
 * dev nó hay bị thứ khác chiếm — một dịch vụ nền, hoặc Tomcat do IDE khởi động. Khi đó chạy backend sang
 * cổng khác là cách nhanh nhất, và không nên phải sửa một file đã commit chỉ để chạy thử trên một máy.
 *
 *   BACKEND_ORIGIN=http://localhost:8081 npm run dev
 *
 * hoặc một dòng `BACKEND_ORIGIN=http://localhost:8081` trong `.env.local` (đã gitignore).
 * Bỏ trống thì vẫn là 8080 như cũ.
 *
 * Phải qua `loadEnv` chứ không đọc thẳng `process.env`: Vite chỉ nạp các tệp `.env*` vào
 * `import.meta.env` của phía client, KHÔNG đổ chúng vào `process.env` của tệp cấu hình này. Đọc thẳng
 * `process.env` thì biến đặt trên dòng lệnh vẫn chạy, nhưng biến đặt trong `.env.local` bị bỏ qua trong
 * im lặng — cấu hình trông như có tác dụng mà không có.
 */
export default defineConfig(({ mode }) => {
  // Tham số thứ ba là tiền tố rỗng: mặc định `loadEnv` chỉ trả biến bắt đầu bằng `VITE_`, mà biến này
  // dùng ở phía máy chủ dev nên KHÔNG được lộ ra bundle của client.
  const env = loadEnv(mode, import.meta.dirname, '')
  const backendOrigin = env.BACKEND_ORIGIN || 'http://localhost:8080'

  return {
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
        '/api': { target: backendOrigin, changeOrigin: true },
        '/ws': { target: backendOrigin, changeOrigin: true, ws: true },
        '/actuator': { target: backendOrigin, changeOrigin: true },
        // Ảnh người dùng tải lên do backend phục vụ tĩnh
        '/uploads': { target: backendOrigin, changeOrigin: true },
        '/swagger-ui': { target: backendOrigin, changeOrigin: true },
        '/v3/api-docs': { target: backendOrigin, changeOrigin: true },
      },
    },
  }
})
