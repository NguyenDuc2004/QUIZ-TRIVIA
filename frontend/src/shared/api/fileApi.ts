import { apiClient } from './client'

/** Khớp UploadedFileResponse của backend. */
export interface UploadedFile {
  /** Đường dẫn công khai dạng `/uploads/images/<uuid>.jpg` — lưu thẳng vào quiz/câu hỏi. */
  url: string
  fileName: string
  size: number
  contentType: string
}

/** Backend chỉ nhận đúng bốn định dạng này (kiểm bằng chữ ký byte, không tin đuôi file). */
export const ACCEPTED_IMAGE_TYPES = 'image/jpeg,image/png,image/gif,image/webp'
export const MAX_IMAGE_BYTES = 2 * 1024 * 1024

/** Để axios tự đặt Content-Type kèm boundary của multipart — đặt tay là hỏng. */
function tai(duongDan: string, file: File) {
  const form = new FormData()
  form.append('file', file)

  return apiClient
    .post<UploadedFile>(duongDan, form, { headers: { 'Content-Type': undefined } })
    .then((res) => res.data)
}

export const fileApi = {
  /** Ảnh bìa quiz và ảnh câu hỏi — backend chỉ cho CREATOR/ADMIN. */
  uploadImage: (file: File) => tai('/files/images', file),

  /**
   * Ảnh đại diện — mọi người dùng đã đăng nhập đều gọi được.
   *
   * Đường riêng chứ không dùng chung `/files/images`: đường kia chỉ mở cho CREATOR/ADMIN, nên người học
   * bấm "Chọn ảnh từ máy" ngay trên trang hồ sơ của mình sẽ nhận `403 Bạn không có quyền`. Backend giữ
   * mỗi người đúng một file ảnh đại diện, nên nó không mang cái rủi ro "chỗ chứa file miễn phí" khiến
   * `/files/images` phải khoá lại.
   */
  uploadAvatar: (file: File) => tai('/files/avatar', file),
}
