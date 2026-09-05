import { describe, expect, it } from 'vitest'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Chặn một LỚP lỗi, không phải một lỗi cụ thể: ghép **màu tuyệt đối** với **màu token**.
 *
 * `--color-ink` là màu CHỮ, nên ở chế độ tối nó lật thành gần trắng. Ghép `bg-ink` với `text-white`
 * cho ra chữ trắng trên nền gần trắng — phần tử biến mất, và biến mất đúng ở trạng thái "đang được
 * chọn", tức thứ người dùng cần thấy nhất.
 *
 * Lỗi này đã xuất hiện hai lần ở hai trang khác nhau (chip danh mục và ô số câu đang làm), nên nó
 * không phải sơ suất một lần mà là một cái bẫy có sẵn trong cách đặt tên token. Không phép kiểm giao
 * diện nào bắt được — nó chỉ lộ ra khi có người mở trang ở chế độ tối và nhìn.
 */
const NEN_LAT_THEO_CHE_DO = ['bg-ink', 'bg-surface', 'bg-canvas', 'bg-surface-subtle']
const CHU_TUYET_DOI = ['text-white', 'text-black']

function moiFileTsx(thuMuc: string): string[] {
  return readdirSync(thuMuc).flatMap((ten) => {
    const duong = join(thuMuc, ten)
    if (statSync(duong).isDirectory()) {
      return ten === 'node_modules' ? [] : moiFileTsx(duong)
    }
    return ten.endsWith('.tsx') ? [duong] : []
  })
}

describe('không ghép màu tuyệt đối với màu token', () => {
  it('không có className nào vừa dùng nền token vừa dùng chữ trắng/đen tuyệt đối', () => {
    const viPham: string[] = []

    for (const duong of moiFileTsx(join(import.meta.dirname, '..', '..'))) {
      const dong = readFileSync(duong, 'utf8').split('\n')
      dong.forEach((noiDung, i) => {
        const coNen = NEN_LAT_THEO_CHE_DO.some((c) => noiDung.includes(`${c} `) || noiDung.includes(`${c}'`))
        const coChu = CHU_TUYET_DOI.some((c) => noiDung.includes(c))
        if (coNen && coChu) {
          viPham.push(`${duong}:${i + 1} → ${noiDung.trim()}`)
        }
      })
    }

    expect(viPham, `Dùng text-canvas / text-ink thay cho màu tuyệt đối:\n${viPham.join('\n')}`).toEqual([])
  })
})
