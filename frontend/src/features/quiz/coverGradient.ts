/** Bảng màu khối ảnh bìa giả lập — chọn theo ký tự đầu của tiêu đề cho ổn định. */
const COVER_GRADIENTS = [
  'linear-gradient(135deg, #5624d0, #a435f0)',
  'linear-gradient(135deg, #1c1d1f, #6a6f73)',
  'linear-gradient(135deg, #0e6e5c, #19857b)',
  'linear-gradient(135deg, #b4690e, #e59819)',
  'linear-gradient(135deg, #2d2f31, #5624d0)',
]

/**
 * Màu bìa thay thế cho quiz chưa có ảnh.
 * <p>
 * Ở đây chứ không phải trong từng component: cùng một quiz phải ra **cùng một màu** ở mọi chỗ nó
 * xuất hiện (lưới Khám phá, thẻ Gợi ý). Mỗi nơi tự chọn màu thì người dùng thấy hai thẻ khác màu và
 * không nhận ra đó là một quiz.
 * <p>
 * Chọn theo tiêu đề, không random: random thì mỗi lần vẽ lại một màu.
 */
export function coverOf(title: string): string {
  return COVER_GRADIENTS[title.charCodeAt(0) % COVER_GRADIENTS.length]
}
