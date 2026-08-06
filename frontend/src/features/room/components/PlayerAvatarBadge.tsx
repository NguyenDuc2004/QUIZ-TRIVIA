/**
 * Avatar người chơi: emoji trên nền tròn màu.
 * <p>
 * Không dùng file ảnh cũng không gọi dịch vụ ảnh bên ngoài — chạy được cả khi mất mạng, không có
 * gì phải tải, và emoji đã có sẵn trên mọi hệ điều hành. Emoji và màu do backend gửi kèm trong
 * mỗi người chơi nên frontend không phải giữ bảng tra riêng.
 */
export default function PlayerAvatarBadge({
  emoji,
  color,
  size = 'md',
  ring,
}: {
  emoji: string | null
  color: string | null
  size?: 'sm' | 'md' | 'lg'
  /** Viền nổi bật, dùng cho avatar đang được chọn. */
  ring?: boolean
}) {
  const box = size === 'lg' ? 'h-16 w-16 text-3xl' : size === 'sm' ? 'h-8 w-8 text-base' : 'h-12 w-12 text-2xl'

  return (
    <span
      className={`flex shrink-0 items-center justify-center rounded-full ${box} ${
        ring ? 'ring-2 ring-ink ring-offset-2' : ''
      }`}
      style={{ backgroundColor: color ?? '#d1d7dc' }}
      aria-hidden="true"
    >
      {emoji ?? '🙂'}
    </span>
  )
}
