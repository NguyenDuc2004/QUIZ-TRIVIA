import axios from 'axios'

/**
 * Axios instance dùng chung. KHÔNG hardcode URL API ở component (docs/conventions.md §2).
 * Dev: gọi đường dẫn tương đối `/api/v1/...`, Vite proxy sang backend :8080.
 */
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Gắn access token vào mọi request (token do feature auth lưu — sẽ nối ở slice Auth)
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})
