import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'node:path'

/**
 * Cấu hình riêng cho vitest, KHÔNG gộp vào `vite.config.ts`.
 *
 * Lý do: vitest 3 chưa hỗ trợ Vite 8 nên nó tự cài một bản vite riêng trong `node_modules/vitest/`.
 * Hai bản vite dùng hai bộ type plugin khác nhau (rolldown ở Vite 8 vs rollup ở bản cũ), nên nhồi
 * trường `test` vào `vite.config.ts` sẽ làm `tsc -b` đổ ở danh sách `plugins` với lỗi
 * "Plugin<any>[] is not assignable to PluginOption" — dù cấu hình vẫn chạy được.
 *
 * File này cố ý **không** nằm trong `include` của `tsconfig.node.json`, nên `tsc -b` không kiểm nó và
 * xung đột type ở trên không xảy ra. Đổi lại phải khai lại alias `@` và plugin react ở đây; đó là cái
 * giá nhỏ so với việc để lệnh build của dự án đỏ.
 *
 * Khi vitest lên bản hỗ trợ Vite 8, gộp lại vào `vite.config.ts` và xoá file này.
 */
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
    },
  },
  test: {
    // jsdom thay cho trình duyệt thật: đủ để render component và bắt lỗi logic, không cần Chrome
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    // Chỉ nhặt file test trong src, không quét node_modules
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
  },
})
