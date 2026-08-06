import { z } from 'zod'

/** Giữ khớp validation của backend (QuizRequest / QuestionRequest + luật theo loại câu hỏi). */

export const quizSchema = z.object({
  title: z
    .string()
    .min(1, 'Tiêu đề không được để trống')
    .max(200, 'Tiêu đề tối đa 200 ký tự'),
  description: z.string().max(2000, 'Mô tả tối đa 2000 ký tự').optional().or(z.literal('')),
  categoryId: z.string().optional(),
  /** Đường dẫn server trả về sau khi tải ảnh lên; null = dùng khối màu tự sinh. */
  thumbnailUrl: z.string().nullable().optional(),
  difficulty: z.enum(['EASY', 'MEDIUM', 'HARD']),
  visibility: z.enum(['PUBLIC', 'PRIVATE']),
  /** Nhập theo phút cho dễ dùng, gửi lên backend quy đổi ra giây. */
  timeLimitMinutes: z
    .number({ message: 'Thời gian phải là số' })
    .int('Thời gian phải là số nguyên')
    .min(1, 'Thời gian phải lớn hơn 0')
    .max(600, 'Thời gian tối đa 600 phút')
    .nullable()
    .optional(),
})

export type QuizForm = z.infer<typeof quizSchema>

const optionSchema = z.object({
  content: z.string().min(1, 'Nội dung lựa chọn không được để trống'),
  correct: z.boolean(),
})

export const questionSchema = z
  .object({
    type: z.enum(['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'FILL_BLANK', 'SHORT_ANSWER']),
    content: z.string().min(1, 'Nội dung câu hỏi không được để trống'),
    explanation: z.string().optional().or(z.literal('')),
    difficulty: z.enum(['EASY', 'MEDIUM', 'HARD']),
    topic: z.string().max(100, 'Chủ đề tối đa 100 ký tự').optional().or(z.literal('')),
    points: z.number().int().min(1, 'Điểm phải lớn hơn 0'),
    options: z.array(optionSchema).min(1, 'Phải có ít nhất một lựa chọn/đáp án'),
  })
  // Kiểm ngay trên client những luật mà backend cũng kiểm, để người soạn đề biết lỗi trước khi gửi
  .superRefine((data, ctx) => {
    const correctCount = data.options.filter((o) => o.correct).length
    const addIssue = (message: string) =>
      ctx.addIssue({ code: 'custom', path: ['options'], message })

    switch (data.type) {
      case 'SINGLE_CHOICE':
        if (data.options.length < 2) addIssue('Câu một đáp án phải có ít nhất 2 lựa chọn')
        else if (correctCount !== 1) addIssue('Chọn đúng 1 đáp án đúng')
        break
      case 'MULTIPLE_CHOICE':
        if (data.options.length < 3) addIssue('Câu nhiều đáp án phải có ít nhất 3 lựa chọn')
        else if (correctCount < 2) addIssue('Phải có ít nhất 2 đáp án đúng')
        else if (correctCount === data.options.length) addIssue('Phải còn ít nhất 1 lựa chọn sai')
        break
      case 'TRUE_FALSE':
        if (data.options.length !== 2) addIssue('Câu Đúng/Sai phải có đúng 2 lựa chọn')
        else if (correctCount !== 1) addIssue('Chọn đúng 1 đáp án đúng')
        break
      case 'FILL_BLANK':
        if (data.options.length < 1) addIssue('Cần ít nhất 1 đáp án được chấp nhận')
        break
      case 'SHORT_ANSWER':
        if (data.options.length !== 1) addIssue('Chỉ nhập đúng 1 đáp án mẫu')
        break
    }
  })

export type QuestionForm = z.infer<typeof questionSchema>
