import { describe, expect, it } from 'vitest'
import { goiYCauHoi } from './AssistantPage'
import type { AskableMaterial } from '../api/chatApi'

function taiLieu(id: string, title: string): AskableMaterial {
  return { id, title, topic: null, sourceType: 'PDF', chunkCount: 3, mine: true }
}

/**
 * Chip gợi ý là lời mời — bấm vào là hỏi thật. Nên rủi ro của nó không phải "gợi ý chưa hay" mà là
 * **mời người dùng hỏi một câu chắc chắn thất bại**.
 */
describe('goiYCauHoi', () => {
  it('kho rỗng thì KHÔNG gợi ý câu nào', () => {
    // Trợ lý chỉ trả lời dựa trên học liệu. Kho rỗng mà vẫn bày chip là dẫn người dùng tới câu
    // "tôi không biết" ngay ở lần chạm đầu tiên, và họ sẽ kết luận là trợ lý hỏng.
    expect(goiYCauHoi([], null)).toEqual([])
    expect(goiYCauHoi(undefined, null)).toEqual([])
  })

  it('mọi gợi ý đều nhắc tên tài liệu có thật trong kho', () => {
    const kho = [taiLieu('1', 'Giải tích 1'), taiLieu('2', 'Cấu trúc dữ liệu')]
    const goiY = goiYCauHoi(kho, null)

    expect(goiY.length).toBeGreaterThan(0)
    goiY.forEach((cau) => {
      expect(kho.some((m) => cau.includes(m.title))).toBe(true)
    })
  })

  it('đang lọc theo một tài liệu thì không gợi ý tài liệu khác', () => {
    // Bộ lọc của người dùng đang chặn tài liệu kia — gợi ý nó là mời họ bấm vào thứ chính họ đã tắt.
    const chon = taiLieu('1', 'Giải tích 1')
    const goiY = goiYCauHoi([chon, taiLieu('2', 'Cấu trúc dữ liệu')], chon)

    expect(goiY.length).toBeGreaterThan(0)
    goiY.forEach((cau) => {
      expect(cau).toContain('Giải tích 1')
      expect(cau).not.toContain('Cấu trúc dữ liệu')
    })
  })

  it('không quá 4 chip dù kho có nhiều tài liệu', () => {
    const kho = Array.from({ length: 12 }, (_, i) => taiLieu(String(i), `Tài liệu ${i}`))
    expect(goiYCauHoi(kho, null).length).toBeLessThanOrEqual(4)
  })
})
