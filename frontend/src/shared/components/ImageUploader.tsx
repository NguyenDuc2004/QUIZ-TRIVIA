import { useState } from 'react'
import { Button, Upload, message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { ACCEPTED_IMAGE_TYPES, MAX_IMAGE_BYTES, fileApi } from '@/shared/api/fileApi'

/**
 * Ô chọn ảnh: tải file lên server rồi giữ lại đường dẫn server trả về.
 *
 * Kiểm dung lượng và kiểu file ngay ở client để báo lỗi nhanh, nhưng đó chỉ là tiện lợi —
 * backend vẫn kiểm lại bằng chữ ký byte vì client sửa được.
 *
 * `variant` đổi ba thứ cùng lúc, và cả ba đều cần đổi cùng nhau:
 * - **endpoint** — ảnh đại diện đi đường riêng, vì `/files/images` chỉ mở cho CREATOR/ADMIN
 * - **khung xem trước** — vuông cho ảnh đại diện, 16:9 cho ảnh bìa
 * - **lời gợi ý** — khuyên "ảnh ngang 16:9" cho ảnh đại diện là khuyên sai: ảnh đại diện luôn hiện trong
 *   khung tròn, ảnh ngang sẽ bị cắt mất hai bên
 */
export default function ImageUploader({
  value,
  onChange,
  hint,
  variant = 'cover',
}: {
  value: string | null
  onChange: (url: string | null) => void
  hint?: string
  variant?: 'cover' | 'avatar'
}) {
  const laAnhDaiDien = variant === 'avatar'
  const [uploading, setUploading] = useState(false)

  const upload = async (file: File) => {
    if (file.size > MAX_IMAGE_BYTES) {
      message.error(`Ảnh tối đa ${MAX_IMAGE_BYTES / 1024 / 1024}MB`)
      return
    }
    setUploading(true)
    try {
      const uploaded = laAnhDaiDien ? await fileApi.uploadAvatar(file) : await fileApi.uploadImage(file)
      onChange(uploaded.url)
    } catch (error) {
      message.error(getApiErrorMessage(error))
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="flex flex-col gap-2">
      {value ? (
        <div
          className={`relative overflow-hidden border border-line ${laAnhDaiDien ? 'w-32' : 'w-full max-w-xs'}`}
        >
          <img
            src={value}
            alt={laAnhDaiDien ? 'Ảnh đại diện' : 'Ảnh bìa'}
            className={`w-full object-cover ${laAnhDaiDien ? 'aspect-square' : 'aspect-video'}`}
          />
        </div>
      ) : (
        <div
          className={`flex items-center justify-center border border-dashed border-line bg-surface-subtle ${
            laAnhDaiDien ? 'aspect-square w-32' : 'aspect-video w-full max-w-xs'
          }`}
        >
          <span className="text-ink-soft text-xs">Chưa có ảnh</span>
        </div>
      )}

      <div className="flex items-center gap-2">
        <Upload
          accept={ACCEPTED_IMAGE_TYPES}
          showUploadList={false}
          maxCount={1}
          // Tự gọi API thay vì để antd tự POST, để dùng chung axios client (có sẵn token + xử lý 401)
          beforeUpload={(file) => {
            void upload(file)
            return false
          }}
        >
          <Button loading={uploading}>{value ? 'Đổi ảnh' : 'Chọn ảnh từ máy'}</Button>
        </Upload>

        {value && (
          <Button type="link" danger onClick={() => onChange(null)}>
            Bỏ ảnh
          </Button>
        )}
      </div>

      <span className="text-ink-soft text-xs">
        {hint ??
          `JPG, PNG, GIF hoặc WebP · tối đa ${MAX_IMAGE_BYTES / 1024 / 1024}MB · ${
            laAnhDaiDien ? 'nên dùng ảnh vuông' : 'nên dùng ảnh ngang 16:9'
          }`}
      </span>
    </div>
  )
}
