import { apiClient } from '@/shared/api/client'
import type { PageResponse } from '@/shared/api/types'

/** Khớp FlashcardSource của backend. */
export type FlashcardSource = 'MANUAL' | 'AI_GENERATED' | 'FROM_WRONG_ANSWER'

/**
 * Mức nhớ người học tự đánh giá. Ranh giới nằm giữa HARD và GOOD: hai mức đầu đưa thẻ về ôn lại ngày mai,
 * hai mức sau giãn lịch theo SM-2.
 */
export type ReviewQuality = 'AGAIN' | 'HARD' | 'GOOD' | 'EASY'

export interface Deck {
  id: string
  title: string
  description: string | null
  topic: string | null
  soThe: number
  /** Số thẻ đến hạn ôn hôm nay — con số duy nhất trả lời được "hôm nay tôi phải ôn gì". */
  soDenHan: number
  createdAt: string
}

export interface DeckBody {
  title: string
  description?: string
  topic?: string
}

export interface Flashcard {
  id: string
  deckId: string
  front: string
  back: string
  hint: string | null
  source: FlashcardSource
  /** null = chưa từng ôn thẻ này, KHÁC với "đến hạn hôm nay". */
  dueDate: string | null
  intervalDays: number | null
  totalReviews: number | null
}

export interface FlashcardBody {
  front: string
  back: string
  hint?: string
}

export interface ReviewResult {
  dueDate: string
  intervalDays: number
  repetitions: number
  soTheConLai: number
}

export interface ReviewStats {
  tongSoThe: number
  /** Thẻ có khoảng ôn ≥ 21 ngày — ngưỡng quy ước của SM-2, không phải kết quả đo. */
  soDaThuoc: number
  soDenHanHomNay: number
  duBao: { ngay: string; soThe: number }[]
}

export interface WrongAnswerResult {
  soDaTao: number
  soBoQua: number
}

export const flashcardApi = {
  decks: (params: { keyword?: string; page?: number; size?: number }) =>
    apiClient.get<PageResponse<Deck>>('/decks', { params }).then((res) => res.data),

  createDeck: (body: DeckBody) =>
    apiClient.post<Deck>('/decks', body).then((res) => res.data),

  updateDeck: (id: string, body: DeckBody) =>
    apiClient.put<Deck>(`/decks/${id}`, body).then((res) => res.data),

  deleteDeck: (id: string) => apiClient.delete<void>(`/decks/${id}`).then((res) => res.data),

  cards: (deckId: string) =>
    apiClient.get<Flashcard[]>(`/decks/${deckId}/cards`).then((res) => res.data),

  addCard: (deckId: string, body: FlashcardBody) =>
    apiClient.post<Flashcard>(`/decks/${deckId}/cards`, body).then((res) => res.data),

  updateCard: (id: string, body: FlashcardBody) =>
    apiClient.put<Flashcard>(`/flashcards/${id}`, body).then((res) => res.data),

  deleteCard: (id: string) => apiClient.delete<void>(`/flashcards/${id}`).then((res) => res.data),

  fromWrongAnswers: (deckId: string) =>
    apiClient.post<WrongAnswerResult>(`/decks/${deckId}/cards/from-wrong-answers`)
      .then((res) => res.data),

  due: (deckId?: string) =>
    apiClient.get<Flashcard[]>('/flashcards/due', { params: { deckId } }).then((res) => res.data),

  review: (id: string, quality: ReviewQuality) =>
    apiClient.post<ReviewResult>(`/flashcards/${id}/review`, null, { params: { quality } })
      .then((res) => res.data),

  stats: () => apiClient.get<ReviewStats>('/flashcards/stats').then((res) => res.data),
}
