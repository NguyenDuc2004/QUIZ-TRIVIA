import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import {
  flashcardApi,
  type DeckBody,
  type FlashcardBody,
  type GenerateCardsBody,
  type ReviewQuality,
} from '../api/flashcardApi'

const KEY = 'flashcards'

export function useDecks(params: { keyword?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: [KEY, 'decks', params],
    queryFn: () => flashcardApi.decks(params),
  })
}

export function useSaveDeck() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id?: string; body: DeckBody }) =>
      id ? flashcardApi.updateDeck(id, body) : flashcardApi.createDeck(body),
    onSuccess: (deck, variables) => {
      message.success(variables.id ? `Đã cập nhật "${deck.title}"` : `Đã tạo bộ thẻ "${deck.title}"`)
      queryClient.invalidateQueries({ queryKey: [KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useDeleteDeck() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => flashcardApi.deleteDeck(id),
    onSuccess: () => {
      message.success('Đã xoá bộ thẻ')
      queryClient.invalidateQueries({ queryKey: [KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useCards(deckId: string | undefined) {
  return useQuery({
    queryKey: [KEY, 'cards', deckId],
    queryFn: () => flashcardApi.cards(deckId!),
    enabled: Boolean(deckId),
  })
}

export function useSaveCard(deckId: string | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id?: string; body: FlashcardBody }) =>
      id ? flashcardApi.updateCard(id, body) : flashcardApi.addCard(deckId!, body),
    onSuccess: (_card, variables) => {
      message.success(variables.id ? 'Đã cập nhật thẻ' : 'Đã thêm thẻ — đến hạn ôn ngay hôm nay')
      queryClient.invalidateQueries({ queryKey: [KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useDeleteCard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => flashcardApi.deleteCard(id),
    onSuccess: () => {
      message.success('Đã xoá thẻ')
      queryClient.invalidateQueries({ queryKey: [KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useGenerateFromWrongAnswers() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (deckId: string) => flashcardApi.fromWrongAnswers(deckId),
    onSuccess: (result) => {
      // Nói cả số đã bỏ qua: bấm lần hai ra 0 thẻ mới là đúng, và người dùng cần biết vì sao thay vì
      // tưởng chức năng hỏng.
      if (result.soDaTao === 0 && result.soBoQua === 0) {
        message.info('Chưa có câu nào bạn trả lời sai để tạo thẻ. Hãy làm một bài quiz trước.')
      } else if (result.soDaTao === 0) {
        message.info(`Tất cả ${result.soBoQua} câu sai đều đã có thẻ trong bộ này.`)
      } else {
        message.success(
          `Đã tạo ${result.soDaTao} thẻ từ câu bạn trả lời sai` +
            (result.soBoQua > 0 ? ` · bỏ qua ${result.soBoQua} câu đã có thẻ` : ''),
        )
      }
      queryClient.invalidateQueries({ queryKey: [KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useGenerateCards() {
  return useMutation({
    mutationFn: ({ deckId, body }: { deckId: string; body: GenerateCardsBody }) =>
      flashcardApi.generateCards(deckId, body),
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

/**
 * Hỏi lại trạng thái job sinh thẻ.
 *
 * Chỉ hỏi lại khi job còn đang chạy: `refetchInterval` trả về false khi đã xong hoặc đã lỗi. Hỏi mãi thì
 * mỗi 2 giây lại một lượt gọi vô ích suốt thời gian người dùng còn mở màn duyệt.
 */
export function useFlashcardJob(jobId: string | undefined) {
  return useQuery({
    queryKey: [KEY, 'job', jobId],
    queryFn: () => flashcardApi.job(jobId!),
    enabled: Boolean(jobId),
    refetchInterval: (query) => {
      const status = query.state.data?.status
      return status === 'SUCCEEDED' || status === 'FAILED' ? false : 2000
    },
  })
}

export function useApproveGenerated() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ jobId, indexes }: { jobId: string; indexes: number[] }) =>
      flashcardApi.approveGenerated(jobId, indexes),
    onSuccess: (result) => {
      message.success(`Đã lưu ${result.soThe} thẻ vào bộ — tất cả đến hạn ôn ngay hôm nay`)
      queryClient.invalidateQueries({ queryKey: [KEY] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useDueCards(deckId?: string) {
  return useQuery({
    queryKey: [KEY, 'due', deckId ?? 'all'],
    queryFn: () => flashcardApi.due(deckId),
    // Không tự làm mới: đang giữa phiên ôn mà danh sách bị nạp lại thì thẻ nhảy đi giữa lúc người học
    // đang đọc. Phiên ôn lấy danh sách một lần rồi tự đi hết.
    staleTime: Infinity,
  })
}

export function useReviewCard() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, quality }: { id: string; quality: ReviewQuality }) =>
      flashcardApi.review(id, quality),
    // KHÔNG invalidate danh sách thẻ đến hạn ở đây: làm vậy thì mỗi lần bấm một mức nhớ, danh sách bị nạp
    // lại và phiên ôn đang chạy bị xoá giữa chừng. Danh sách bộ thẻ và thống kê thì cần làm mới.
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [KEY, 'decks'] })
      queryClient.invalidateQueries({ queryKey: [KEY, 'stats'] })
    },
    onError: (error) => message.error(getApiErrorMessage(error)),
  })
}

export function useReviewStats() {
  return useQuery({
    queryKey: [KEY, 'stats'],
    queryFn: () => flashcardApi.stats(),
  })
}
