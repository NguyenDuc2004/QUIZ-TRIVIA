/**
 * Trang mặc định của một vai trò — chỗ duy nhất trong ứng dụng quyết định "đăng nhập xong thì đi đâu".
 *
 * Có hàm này vì cùng một câu hỏi được hỏi ở **bốn** chỗ: sau khi đăng nhập, khi vào `/`, khi người đã đăng
 * nhập mở lại `/login`, và khi `ProtectedRoute` từ chối vì sai vai trò. Viết thẳng `/quizzes` ở cả bốn nơi
 * thì chỉ cần sót một chỗ là **vòng lặp chuyển hướng**: Admin bị đẩy khỏi khu học tập về `/quizzes`, mà
 * `/quizzes` lại đẩy họ đi tiếp — trang trắng, không có lỗi nào để lần ra.
 */
export function trangChuTheoVaiTro(role: string | undefined): string {
  // Quản trị viên chỉ làm việc trong khu quản trị: khu học tập không có gì cho họ, và đưa họ vào đó
  // là mời một tài khoản có quyền tác động lên người khác đi lang thang giữa dữ liệu của người khác.
  return role === 'ADMIN' ? '/admin' : '/quizzes'
}
