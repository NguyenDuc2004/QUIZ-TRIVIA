import { useState } from 'react'
import { Button, Upload, message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { ACCEPTED_IMAGE_TYPES, MAX_IMAGE_BYTES, fileApi } from '@/shared/api/fileApi'

/**
 * Ô chọn ảnh: tải file lên server rồi giữ lại đường dẫn server trả về.
 * <p>
 * Kiểm dung lượng và kiểu file ngay ở client để báo lỗi nhanh, nhưng đó chỉ là tiện lợi —
 * backend vẫn kiểm lại bằng chữ ký byte vì client sửa được.
 */
export default function ImageUploader({
  value,
  onChange,
  hint,
}: {
  value: string | null
  onChange: (url: string | null) => void
  hint?: string
}) {
  const [uploading, setUploading] = useState(false)

  const upload = async (file: File) => {
    if (file.size > MAX_IMAGE_BYTES) {
      message.error(`Ảnh tối đa ${MAX_IMAGE_BYTES / 1024 / 1024}MB`)
      return
    }
    setUploading(true)
    try {
      const uploaded = await fileApi.uploadImage(file)
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
        <div className="relative w-full max-w-xs overflow-hidden border border-line">
          <img src={value} alt="Ảnh bìa" className="aspect-video w-full object-cover" />
        </div>
      ) : (
        <div className="flex aspect-video w-full max-w-xs items-center justify-center border border-dashed border-line bg-surface-subtle">
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
        {hint ?? `JPG, PNG, GIF hoặc WebP · tối đa ${MAX_IMAGE_BYTES / 1024 / 1024}MB · nên dùng ảnh ngang 16:9`}
      </span>
    </div>
  )
}
