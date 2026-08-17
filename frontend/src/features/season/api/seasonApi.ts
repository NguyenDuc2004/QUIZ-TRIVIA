import { apiClient } from '@/shared/api/client'

export interface LeaderboardRow {
  rank: number
  userId: string
  displayName: string
  avatarUrl: string | null
  score: number
}

export interface Leaderboard {
  seasonId: string
  tenMua: string
  batDau: string
  ketThuc: string
  soNguoiThamGia: number
  top: LeaderboardRow[]
  /** null = chưa có điểm nào trong mùa, KHÁC với hạng cuối. */
  thuHangCuaToi: LeaderboardRow | null
}

export interface SeasonHistoryItem {
  seasonId: string
  tenMua: string
  ketThuc: string
  finalRank: number | null
  finalScore: number | null
  tenHuyHieu: string | null
  iconHuyHieu: string | null
}

/**
 * Chỉ có endpoint đọc. Điểm mùa là tổng XP kiếm trong khoảng thời gian mùa, và XP chỉ đến từ hành động học
 * thật — không có đường ghi nào để tự leo hạng.
 */
export const seasonApi = {
  current: (limit = 20) =>
    apiClient.get<Leaderboard>('/leaderboard/season/current', { params: { limit } })
      .then((res) => res.data),

  history: () =>
    apiClient.get<SeasonHistoryItem[]>('/leaderboard/season/history').then((res) => res.data),
}
