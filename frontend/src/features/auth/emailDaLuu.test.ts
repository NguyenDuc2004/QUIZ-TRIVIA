import { beforeEach, describe, expect, it } from 'vitest'
import { emailDaLuu } from './emailDaLuu'

/**
 * Tiện ích nhỏ, nhưng có hai chỗ dễ làm sai và cả hai đều là chuyện riêng tư chứ không phải tiện lợi.
 */
describe('emailDaLuu', () => {
  beforeEach(() => localStorage.clear())

  it('có ghi nhớ thì lưu email cho lần sau', () => {
    emailDaLuu.luu('ban@example.com', true)
    expect(emailDaLuu.doc()).toBe('ban@example.com')
  })

  it('KHÔNG ghi nhớ thì không lưu, và XOÁ bản cũ của người trước', () => {
    // Bỏ tick nghĩa là "máy này đừng giữ lại gì về tôi". Giữ email lại thì vẫn để lộ AI vừa dùng máy
    // này — đúng thứ họ bỏ tick để tránh. Và người dùng TRƯỚC có thể đã tick, nên phải xoá bản của
    // họ đi chứ không chỉ bỏ qua lần ghi này.
    emailDaLuu.luu('nguoi-truoc@example.com', true)

    emailDaLuu.luu('nguoi-sau@example.com', false)

    expect(emailDaLuu.doc()).toBe('')
    expect(localStorage.getItem('quizai-email-cuoi')).toBeNull()
  })

  it('chưa từng đăng nhập thì trả chuỗi rỗng, không phải null', () => {
    // Giá trị này đi thẳng vào `defaultValues` của form; `null` sẽ biến ô nhập thành không kiểm soát.
    expect(emailDaLuu.doc()).toBe('')
  })
})
