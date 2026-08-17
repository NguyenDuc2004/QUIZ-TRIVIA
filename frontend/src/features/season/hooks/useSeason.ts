import { useQuery } from '@tanstack/react-query'
import { seasonApi } from '../api/seasonApi'

const KEY = 'season'

export function useLeaderboard(limit = 20) {
  return useQuery({
    queryKey: [KEY, 'current', limit],
    queryFn: () => seasonApi.current(limit),
    // Bảng xếp hạng đổi khi có người khác học, nên làm mới định kỳ. 30 giây: đủ để thấy thay đổi mà không
    // biến trang thành một vòng lặp gọi API.
    refetchInterval: 30_000,
  })
}

export function useSeasonHistory() {
  return useQuery({ queryKey: [KEY, 'history'], queryFn: () => seasonApi.history() })
}
