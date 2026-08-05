import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Form, Input, InputNumber, Modal, Radio, Select } from 'antd'
import type { QuizBody, QuizSummary } from '../api/quizApi'
import { DIFFICULTY_OPTIONS } from '../constants'
import { useCategories, useCreateQuiz, useUpdateQuiz } from '../hooks/useQuizQueries'
import { quizSchema, type QuizForm } from '../schema'

interface Props {
  open: boolean
  /** null = tạo mới */
  quiz: QuizSummary | null
  onClose: () => void
}

export default function QuizFormModal({ open, quiz, onClose }: Props) {
  const { data: categories } = useCategories()
  const createQuiz = useCreateQuiz()
  const updateQuiz = useUpdateQuiz()

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<QuizForm>({
    resolver: zodResolver(quizSchema),
    defaultValues: {
      title: '',
      description: '',
      categoryId: undefined,
      difficulty: 'MEDIUM',
      visibility: 'PRIVATE',
      timeLimitMinutes: null,
    },
  })

  // Nạp lại form mỗi lần mở modal cho đúng quiz đang sửa
  useEffect(() => {
    if (!open) return
    reset({
      title: quiz?.title ?? '',
      description: quiz?.description ?? '',
      categoryId: quiz?.categoryId ?? undefined,
      difficulty: quiz?.difficulty ?? 'MEDIUM',
      visibility: quiz?.visibility ?? 'PRIVATE',
      timeLimitMinutes: quiz?.timeLimitSec ? Math.round(quiz.timeLimitSec / 60) : null,
    })
  }, [open, quiz, reset])

  const submit = handleSubmit(async (values) => {
    const body: QuizBody = {
      title: values.title,
      description: values.description || null,
      categoryId: values.categoryId || null,
      difficulty: values.difficulty,
      visibility: values.visibility,
      timeLimitSec: values.timeLimitMinutes ? values.timeLimitMinutes * 60 : null,
    }

    if (quiz) {
      await updateQuiz.mutateAsync({ id: quiz.id, body })
    } else {
      await createQuiz.mutateAsync(body)
    }
    onClose()
  })

  return (
    <Modal
      open={open}
      title={quiz ? 'Sửa quiz' : 'Tạo quiz mới'}
      okText={quiz ? 'Lưu' : 'Tạo'}
      cancelText="Hủy"
      confirmLoading={createQuiz.isPending || updateQuiz.isPending}
      onOk={submit}
      onCancel={onClose}
      destroyOnHidden
    >
      <Form layout="vertical" className="mt-4!">
        <Form.Item label="Tiêu đề" validateStatus={errors.title && 'error'} help={errors.title?.message}>
          <Controller
            name="title"
            control={control}
            render={({ field }) => <Input {...field} placeholder="Ví dụ: Ôn tập Java cơ bản" />}
          />
        </Form.Item>

        <Form.Item
          label="Mô tả"
          validateStatus={errors.description && 'error'}
          help={errors.description?.message}
        >
          <Controller
            name="description"
            control={control}
            render={({ field }) => <Input.TextArea {...field} rows={3} />}
          />
        </Form.Item>

        <Form.Item label="Danh mục">
          <Controller
            name="categoryId"
            control={control}
            render={({ field }) => (
              <Select
                {...field}
                allowClear
                placeholder="Chọn danh mục"
                options={(categories ?? []).map((c) => ({ value: c.id, label: c.name }))}
              />
            )}
          />
        </Form.Item>

        <Form.Item label="Độ khó">
          <Controller
            name="difficulty"
            control={control}
            render={({ field }) => <Radio.Group {...field} optionType="button" options={DIFFICULTY_OPTIONS} />}
          />
        </Form.Item>

        <Form.Item
          label="Thời gian làm bài (phút)"
          validateStatus={errors.timeLimitMinutes && 'error'}
          help={errors.timeLimitMinutes?.message ?? 'Để trống nếu không giới hạn'}
        >
          <Controller
            name="timeLimitMinutes"
            control={control}
            render={({ field }) => (
              <InputNumber
                {...field}
                min={1}
                max={600}
                className="w-full"
                onChange={(value) => field.onChange(value ?? null)}
              />
            )}
          />
        </Form.Item>

        <Form.Item label="Hiển thị" help="Quiz công khai sẽ xuất hiện ở mục Khám phá quiz">
          <Controller
            name="visibility"
            control={control}
            render={({ field }) => (
              <Radio.Group
                {...field}
                optionType="button"
                options={[
                  { value: 'PRIVATE', label: 'Riêng tư' },
                  { value: 'PUBLIC', label: 'Công khai' },
                ]}
              />
            )}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}
