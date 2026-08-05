import { z } from 'zod'

/** Giữ khớp với validation của backend (RegisterRequest / LoginRequest). */

export const loginSchema = z.object({
  email: z.string().min(1, 'Email không được để trống').email('Email không đúng định dạng'),
  password: z.string().min(1, 'Mật khẩu không được để trống'),
})

export const registerSchema = z
  .object({
    displayName: z
      .string()
      .min(1, 'Tên hiển thị không được để trống')
      .max(100, 'Tên hiển thị tối đa 100 ký tự'),
    email: z
      .string()
      .min(1, 'Email không được để trống')
      .email('Email không đúng định dạng')
      .max(255, 'Email tối đa 255 ký tự'),
    password: z
      .string()
      .min(8, 'Mật khẩu phải từ 8 đến 72 ký tự')
      .max(72, 'Mật khẩu phải từ 8 đến 72 ký tự'),
    confirmPassword: z.string().min(1, 'Vui lòng nhập lại mật khẩu'),
    role: z.enum(['LEARNER', 'CREATOR']),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Mật khẩu nhập lại không khớp',
  })

export type LoginForm = z.infer<typeof loginSchema>
export type RegisterForm = z.infer<typeof registerSchema>
