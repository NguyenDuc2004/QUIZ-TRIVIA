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

export const fileApi = {
  uploadImage: (file: File) => {
    const form = new FormData()
    form.append('file', file)

    // Để axios tự đặt Content-Type kèm boundary của multipart — đặt tay là hỏng
    return apiClient
      .post<UploadedFile>('/files/images', form, { headers: { 'Content-Type': undefined } })
      .then((res) => res.data)
  },
}
