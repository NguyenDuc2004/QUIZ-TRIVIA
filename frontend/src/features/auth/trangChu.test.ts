import { describe, expect, it } from 'vitest'
import { trangChuTheoVaiTro } from './trangChu'

/**
 * Hàm ba dòng nhưng có test riêng vì nó là **chỗ duy nhất** quyết định "vai trò này thuộc về khu nào", và
 * bốn nơi khác nhau gọi nó. Trả sai một giá trị ở đây là vòng lặp chuyển hướng ở cả bốn nơi cùng lúc.
 */
describe('trangChuTheoVaiTro', () => {
  it('ADMIN thuộc về khu quản trị', () => {
    expect(trangChuTheoVaiTro('ADMIN')).toBe('/admin')
  })

  it('LEARNER và CREATOR thuộc về khu học tập', () => {
    expect(trangChuTheoVaiTro('LEARNER')).toBe('/quizzes')
    expect(trangChuTheoVaiTro('CREATOR')).toBe('/quizzes')
  })

  it('vai trò lạ hoặc chưa biết thì về khu học tập, không phải khu quản trị', () => {
    // Mặc định phải là khu ÍT quyền hơn. Mặc định ngược lại thì một giá trị vai trò hỏng sẽ đưa người dùng
    // thẳng vào khu quản trị — giao diện không chặn được, và chỉ còn backend đứng giữa.
    expect(trangChuTheoVaiTro(undefined)).toBe('/quizzes')
    expect(trangChuTheoVaiTro('VAI_TRO_LA')).toBe('/quizzes')
  })
})
