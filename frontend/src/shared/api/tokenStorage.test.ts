import { beforeEach, describe, expect, it } from 'vitest'
import { tokenStorage } from './tokenStorage'

/**
 * Ô "Ghi nhớ đăng nhập" chỉ có nghĩa nếu **không có đường nào lặng lẽ nâng một phiên tạm thành phiên
 * vĩnh viễn**. Phần lớn phép kiểm dưới đây canh đúng chỗ đó.
 */
describe('tokenStorage', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('ghi nhớ → localStorage, sống qua lần đóng trình duyệt', () => {
    tokenStorage.save('a', 'r', true)

    expect(localStorage.getItem('refreshToken')).toBe('r')
    expect(sessionStorage.getItem('refreshToken')).toBeNull()
    expect(tokenStorage.dangGhiNho()).toBe(true)
  })

  it('không ghi nhớ → sessionStorage, đóng tab là mất', () => {
    tokenStorage.save('a', 'r', false)

    expect(sessionStorage.getItem('refreshToken')).toBe('r')
    expect(localStorage.getItem('refreshToken')).toBeNull()
    expect(tokenStorage.dangGhiNho()).toBe(false)
  })

  it('XOAY TOKEN giữ nguyên chỗ lưu — đây là chỗ dễ hỏng ngầm nhất', () => {
    // axios interceptor gọi `save()` mỗi lần làm mới token và nó không biết người dùng đã chọn gì.
    // Mặc định về localStorage thì mọi phiên "chỉ trong lần này" tự nâng thành vĩnh viễn ở lần làm
    // mới đầu tiên — tức 15 phút sau khi đăng nhập. Người dùng bỏ tick mà vẫn bị nhớ.
    tokenStorage.save('a', 'r', false)

    tokenStorage.save('a2', 'r2') // không truyền lựa chọn, đúng như interceptor làm

    expect(sessionStorage.getItem('refreshToken')).toBe('r2')
    expect(localStorage.getItem('refreshToken')).toBeNull()
    expect(tokenStorage.dangGhiNho()).toBe(false)
  })

  it('đổi lựa chọn thì XOÁ bản ở kho cũ, không để sót', () => {
    // Sót một bản ở kho kia thì lần đọc sau có thể nhặt trúng nó — người dùng bỏ tick mà vẫn còn
    // phiên trong localStorage từ lần đăng nhập trước là đúng thứ tính năng này sinh ra để chặn.
    tokenStorage.save('a', 'r', true)
    tokenStorage.save('a2', 'r2', false)

    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
    expect(tokenStorage.getRefresh()).toBe('r2')
  })

  it('đăng xuất xoá ở CẢ HAI kho', () => {
    localStorage.setItem('refreshToken', 'cu')
    tokenStorage.save('a', 'r', false)

    tokenStorage.clear()

    expect(tokenStorage.getAccess()).toBeNull()
    expect(tokenStorage.getRefresh()).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })

  it('chưa đăng nhập thì mặc định là CÓ ghi nhớ', () => {
    // Trùng với ô tick mặc định ở màn đăng nhập, và giữ hành vi trước khi có tính năng này không đổi.
    expect(tokenStorage.dangGhiNho()).toBe(true)
  })
})
