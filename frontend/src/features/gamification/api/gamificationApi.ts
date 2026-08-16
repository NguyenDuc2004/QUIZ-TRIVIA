import { apiClient } from '@/shared/api/client'

export interface BadgeItem {
  id: string
  code: string
  name: string
  description: string
  icon: string | null
  /** null = chưa mở khoá. Danh sách trả về cả huy hiệu chưa đạt để người học thấy còn gì hướng tới. */
  earnedAt: string | null
}

export interface GamificationOverview {
  totalXp: number
  level: number
  xpTrongCap: number
  /** 0 = đã ở cấp tối đa. Giao diện phải xử lý trường hợp này thay vì chia cho 0. */
  xpCanTrongCap: number
  currentStreak: number
  longestStreak: number
  lastActiveDate: string | null
  /**
   * Hôm nay đã có hoạt động chưa. Cần cờ riêng vì `currentStreak` không nói được điều đó: chuỗi 5 ngày có
   * thể là "đã học hôm nay" hoặc "học đến hôm qua, hôm nay chưa" — hai trạng thái khác nhau hoàn toàn.
   */
  streakConHomNay: boolean
  soHuyHieu: number
  tongSoHuyHieu: number
  huyHieuMoiNhat: BadgeItem[]
}

export interface DailyChallenge {
  id: string
  ngay: string
  description: string
  progress: number
  target: number
  xpReward: number
  completedAt: string | null
}

/**
 * Chỉ có endpoint ĐỌC. XP chỉ đến từ hành động học thật, qua domain event ở backend — không có đường ghi
 * nào qua API, vì mở đường đó là mở đường tự cộng điểm cho mình.
 */
export const gamificationApi = {
  me: () => apiClient.get<GamificationOverview>('/gamification/me').then((res) => res.data),
  badges: () => apiClient.get<BadgeItem[]>('/gamification/badges').then((res) => res.data),
  daily: () => apiClient.get<DailyChallenge>('/gamification/daily').then((res) => res.data),
}
