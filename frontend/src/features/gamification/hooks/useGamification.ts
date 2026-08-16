import { useQuery } from '@tanstack/react-query'
import { gamificationApi } from '../api/gamificationApi'

const KEY = 'gamification'

export function useGamificationOverview() {
  return useQuery({ queryKey: [KEY, 'me'], queryFn: () => gamificationApi.me() })
}

export function useBadges() {
  return useQuery({ queryKey: [KEY, 'badges'], queryFn: () => gamificationApi.badges() })
}

export function useDailyChallenge() {
  return useQuery({ queryKey: [KEY, 'daily'], queryFn: () => gamificationApi.daily() })
}
