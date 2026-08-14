import '@testing-library/jest-dom/vitest'
import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'

// Tháo DOM sau mỗi ca: để lại thì ca sau truy vấn được cả cây của ca trước và `getByText` báo trùng
afterEach(() => {
  cleanup()
  localStorage.clear()
  sessionStorage.clear()
})

/**
 * jsdom không hiện thực `matchMedia`, mà Ant Design gọi nó khi tính điểm ngắt responsive — thiếu thì
 * mọi component antd ném lỗi ngay lúc render, không liên quan gì tới thứ đang được kiểm.
 */
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }),
})

// Cùng lý do: một số component antd đo kích thước phần tử qua ResizeObserver
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = window.ResizeObserver ?? (ResizeObserverStub as never)
