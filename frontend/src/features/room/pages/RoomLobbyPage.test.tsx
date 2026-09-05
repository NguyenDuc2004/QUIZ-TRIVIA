import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import RoomLobbyPage from './RoomLobbyPage'
import { quizApi, type QuizSummary } from '@/features/quiz/api/quizApi'

/**
 * Người dùng bấm "Mở phòng đấu trí" từ trang giới thiệu một quiz, sang tới sảnh thì **quiz đó không
 * được chọn sẵn** — và nút "Mở phòng" bên này thì `disabled` khi chưa chọn gì. Tức là bấm một nút
 * hứa mở phòng rồi đến nơi gặp một nút mở phòng bấm không được.
 *
 * Ba tình huống phải đúng, và tình huống thứ ba mới là chỗ dễ làm ẩu: khi KHÔNG dựng được lựa chọn
 * nào cho quiz trong URL, màn hình trông y hệt lúc chưa sửa gì — nên bắt buộc phải nói ra lý do.
 */
function quiz(ghiDe: Partial<QuizSummary> = {}): QuizSummary {
  return {
    id: 'q-cong-khai',
    title: 'Toán 12 Đạo hàm',
    description: null,
    categoryId: null,
    categoryName: 'Toán học',
    difficulty: 'MEDIUM',
    visibility: 'PUBLIC',
    aiGenerated: false,
    thumbnailUrl: null,
    timeLimitSec: null,
    strictExam: false,
    questionCount: 5,
    ...ghiDe,
  } as QuizSummary
}

function hienThi(url: string, danhSachCong: QuizSummary[] = [quiz()]) {
  vi.spyOn(quizApi, 'list').mockResolvedValue({
    content: danhSachCong,
    page: 0,
    size: 50,
    totalElements: danhSachCong.length,
    totalPages: 1,
    last: true,
  } as never)

  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[url]}>
        <RoomLobbyPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('RoomLobbyPage — quiz chỉ định qua ?quizId=', () => {
  it('chọn sẵn quiz công khai đến từ URL', async () => {
    vi.spyOn(quizApi, 'get').mockResolvedValue(quiz())

    hienThi('/rooms?quizId=q-cong-khai')

    expect(await screen.findByTitle('Toán 12 Đạo hàm · 5 câu')).toBeInTheDocument()
  })

  it('chọn sẵn được cả quiz RIÊNG TƯ của chính mình', async () => {
    // Danh sách trong sảnh chỉ có quiz công khai, nhưng backend CHO PHÉP mở phòng từ quiz riêng tư
    // của chính mình. Không nạp riêng thì chủ quiz thấy một mã UUID trần, hoặc ô chọn rỗng.
    vi.spyOn(quizApi, 'get').mockResolvedValue(
      quiz({ id: 'q-rieng', title: 'Đề nháp của tôi', visibility: 'PRIVATE', questionCount: 8 }),
    )

    hienThi('/rooms?quizId=q-rieng', [])

    expect(await screen.findByTitle('Đề nháp của tôi · 8 câu')).toBeInTheDocument()
  })

  it('quiz trong URL không dùng được thì NÓI RA, không im lặng', async () => {
    // Im lặng thì màn hình trông y hệt lúc chưa sửa gì — ô chọn rỗng, nút "Mở phòng" bấm không được
    // — mà người dùng vừa bấm một nút hứa điều ngược lại.
    vi.spyOn(quizApi, 'get').mockRejectedValue(new Error('404'))

    hienThi('/rooms?quizId=khong-ton-tai', [])

    expect(await screen.findByText('Không mở được phòng cho quiz bạn vừa chọn')).toBeInTheDocument()
  })

  it('quiz chưa có câu hỏi thì nói rõ LÝ DO đó, không nói chung chung', async () => {
    vi.spyOn(quizApi, 'get').mockResolvedValue(quiz({ id: 'q-rong', questionCount: 0 }))

    hienThi('/rooms?quizId=q-rong', [])

    expect(await screen.findByText(/chưa có câu hỏi nào/)).toBeInTheDocument()
  })

  it('vào sảnh mà KHÔNG có ?quizId= thì không cảnh báo gì', async () => {
    vi.spyOn(quizApi, 'get').mockRejectedValue(new Error('không được gọi'))

    hienThi('/rooms')

    expect(await screen.findByText('🎬 Mở phòng mới')).toBeInTheDocument()
    expect(screen.queryByText('Không mở được phòng cho quiz bạn vừa chọn')).not.toBeInTheDocument()
  })
})
