import { useEffect } from 'react'
import { Controller, useFieldArray, useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import MathText from '@/shared/components/MathText'
import XemTruocCongThuc, { coDanhDauCongThuc } from '@/shared/components/XemTruocCongThuc'
import {
  Alert,
  AutoComplete,
  Button,
  Checkbox,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  Space,
} from 'antd'
import ImageUploader from '@/shared/components/ImageUploader'
import type { Question, QuestionBody, QuestionType } from '../api/quizApi'
import { DIFFICULTY_OPTIONS, QUESTION_TYPE_HINT, QUESTION_TYPE_OPTIONS } from '../constants'
import { useCreateQuestion, useQuestionTopics, useUpdateQuestion } from '../hooks/useQuizQueries'
import { questionSchema, type QuestionForm } from '../schema'

interface Props {
  open: boolean
  /** null = tạo mới */
  question: Question | null
  onClose: () => void
}

/** Bộ lựa chọn mặc định khi đổi loại câu hỏi. */
function defaultOptions(type: QuestionType) {
  switch (type) {
    case 'TRUE_FALSE':
      return [
        { content: 'Đúng', correct: true },
        { content: 'Sai', correct: false },
      ]
    case 'SINGLE_CHOICE':
      return [
        { content: '', correct: true },
        { content: '', correct: false },
      ]
    case 'MULTIPLE_CHOICE':
      return [
        { content: '', correct: true },
        { content: '', correct: true },
        { content: '', correct: false },
      ]
    case 'FILL_BLANK':
      return [{ content: '', correct: true }]
    case 'SHORT_ANSWER':
      return [{ content: '', correct: true }]
  }
}

export default function QuestionFormModal({ open, question, onClose }: Props) {
  const createQuestion = useCreateQuestion()
  const updateQuestion = useUpdateQuestion()
  const { data: topics } = useQuestionTopics()
  const topicOptions = (topics ?? []).map((item) => ({
    value: item.topic,
    label: `${item.topic} (${item.questionCount} câu)`,
  }))

  const {
    control,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<QuestionForm>({
    resolver: zodResolver(questionSchema),
    defaultValues: {
      type: 'SINGLE_CHOICE',
      content: '',
      explanation: '',
      imageUrl: null,
      rubric: '',
      difficulty: 'MEDIUM',
      topic: '',
      points: 1,
      options: defaultOptions('SINGLE_CHOICE'),
    },
  })

  const { fields, append, remove, replace } = useFieldArray({ control, name: 'options' })
  const type = watch('type')
  const options = watch('options')

  useEffect(() => {
    if (!open) return
    reset({
      type: question?.type ?? 'SINGLE_CHOICE',
      content: question?.content ?? '',
      explanation: question?.explanation ?? '',
      imageUrl: question?.imageUrl ?? null,
      rubric: question?.rubric ?? '',
      difficulty: question?.difficulty ?? 'MEDIUM',
      topic: question?.topic ?? '',
      points: question?.points ?? 1,
      options: question
        ? question.options.map((o) => ({ content: o.content, correct: o.correct }))
        : defaultOptions('SINGLE_CHOICE'),
    })
  }, [open, question, reset])

  /** Đổi loại câu hỏi → dựng lại bộ lựa chọn cho khớp luật của loại đó. */
  const handleTypeChange = (nextType: QuestionType) => {
    setValue('type', nextType)
    replace(defaultOptions(nextType))
  }

  const submit = handleSubmit(async (values) => {
    const body: QuestionBody = {
      type: values.type,
      content: values.content,
      explanation: values.explanation || null,
      imageUrl: values.imageUrl || null,
      rubric: values.rubric || null,
      difficulty: values.difficulty,
      topic: values.topic || null,
      points: values.points,
      options: values.options.map((o) => ({ content: o.content, correct: o.correct })),
    }

    if (question) {
      await updateQuestion.mutateAsync({ id: question.id, body })
    } else {
      await createQuestion.mutateAsync(body)
    }
    onClose()
  })

  const isChoiceBased = type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE' || type === 'TRUE_FALSE'
  const canAddOption = type !== 'TRUE_FALSE' && type !== 'SHORT_ANSWER'

  return (
    <Modal
      open={open}
      width={720}
      title={question ? 'Sửa câu hỏi' : 'Thêm câu hỏi vào ngân hàng'}
      okText={question ? 'Lưu' : 'Thêm'}
      cancelText="Hủy"
      confirmLoading={createQuestion.isPending || updateQuestion.isPending}
      onOk={submit}
      onCancel={onClose}
      destroyOnHidden
    >
      <Form layout="vertical" className="mt-4!">
        <Form.Item label="Loại câu hỏi">
          <Select value={type} options={QUESTION_TYPE_OPTIONS} onChange={handleTypeChange} />
        </Form.Item>

        <Alert type="info" showIcon className="mb-4!" message={QUESTION_TYPE_HINT[type]} />

        <Form.Item
          label="Nội dung câu hỏi"
          validateStatus={errors.content && 'error'}
          help={errors.content?.message}
        >
          <Controller
            name="content"
            control={control}
            render={({ field }) => (
              <>
                <Input.TextArea {...field} rows={2} />
                {/* Xem trước ngay dưới ô nhập: người viết thấy kết quả trong lúc gõ, nên vừa biết
                    tính năng công thức tồn tại vừa kiểm được mình gõ đúng cú pháp chưa. */}
                <XemTruocCongThuc noiDung={field.value ?? ''} />
              </>
            )}
          />
        </Form.Item>

        <Form.Item
          label={
            type === 'FILL_BLANK'
              ? 'Các đáp án được chấp nhận'
              : type === 'SHORT_ANSWER'
                ? 'Đáp án mẫu'
                : 'Các lựa chọn'
          }
          validateStatus={errors.options && 'error'}
          help={errors.options?.message ?? (errors.options as unknown as { root?: { message?: string } })?.root?.message}
        >
          <Space direction="vertical" className="w-full">
            {fields.map((fieldItem, index) => (
              <Space key={fieldItem.id} align="start" className="w-full">
                {/* Đánh dấu đáp án đúng: radio cho loại 1 đáp án, checkbox cho loại nhiều đáp án */}
                {isChoiceBased &&
                  (type === 'MULTIPLE_CHOICE' ? (
                    <Controller
                      name={`options.${index}.correct`}
                      control={control}
                      render={({ field }) => (
                        <Checkbox
                          checked={field.value}
                          onChange={(event) => field.onChange(event.target.checked)}
                        />
                      )}
                    />
                  ) : (
                    <Radio
                      checked={options?.[index]?.correct ?? false}
                      onChange={() =>
                        replace(
                          (options ?? []).map((option, i) => ({ ...option, correct: i === index })),
                        )
                      }
                    />
                  ))}

                <Controller
                  name={`options.${index}.content`}
                  control={control}
                  render={({ field }) =>
                    type === 'SHORT_ANSWER' ? (
                      <Input.TextArea {...field} rows={2} style={{ width: 560 }} />
                    ) : (
                      <div style={{ width: 520 }}>
                        <Input
                          {...field}
                          disabled={type === 'TRUE_FALSE'}
                          placeholder={
                            type === 'FILL_BLANK'
                              ? 'Một cách viết đáp án được chấp nhận'
                              : 'Nội dung lựa chọn'
                          }
                        />
                        {/* Chỉ xem trước khi lựa chọn ĐÃ có `$...$` — phần gợi ý cú pháp đã nói một
                            lần ở ô nội dung, nhắc lại dưới từng lựa chọn là bốn dòng chữ giống nhau
                            trong một hộp thoại vốn đã dày. */}
                        {coDanhDauCongThuc(field.value ?? '') && (
                          <div className="bg-surface-subtle border-line mt-1 rounded-control border px-2 py-1">
                            <MathText>{field.value ?? ''}</MathText>
                          </div>
                        )}
                      </div>
                    )
                  }
                />

                {canAddOption && fields.length > 1 && (
                  <Button type="text" danger onClick={() => remove(index)}>
                    Xóa
                  </Button>
                )}
              </Space>
            ))}

            {canAddOption && (
              <Button type="dashed" onClick={() => append({ content: '', correct: false })}>
                + {type === 'FILL_BLANK' ? 'Thêm đáp án được chấp nhận' : 'Thêm lựa chọn'}
              </Button>
            )}
          </Space>
        </Form.Item>

        <Space size="large" align="start" className="w-full">
          <Form.Item label="Độ khó">
            <Controller
              name="difficulty"
              control={control}
              render={({ field }) => (
                <Radio.Group {...field} optionType="button" options={DIFFICULTY_OPTIONS} />
              )}
            />
          </Form.Item>

          <Form.Item label="Điểm" validateStatus={errors.points && 'error'} help={errors.points?.message}>
            <Controller
              name="points"
              control={control}
              render={({ field }) => (
                <InputNumber {...field} min={1} onChange={(value) => field.onChange(value ?? 1)} />
              )}
            />
          </Form.Item>

          <Form.Item label="Chủ đề" help="Dùng để lọc câu hỏi khi soạn quiz">
            <Controller
              name="topic"
              control={control}
              render={({ field }) => (
                // Gõ tự do được, nhưng xổ sẵn chủ đề đã dùng: không có gợi ý thì hôm nay gõ
                // "Lịch sử Việt Nam", mai gõ "Lịch sử VN" và thành hai chủ đề khác nhau,
                // lọc sẽ sót câu mà không ai biết vì sao.
                <AutoComplete
                  {...field}
                  style={{ width: 260 }}
                  placeholder="Ví dụ: Lịch sử Việt Nam"
                  options={topicOptions}
                  filterOption={(input, option) =>
                    String(option?.value ?? '')
                      .toLowerCase()
                      .includes(input.toLowerCase())
                  }
                />
              )}
            />
          </Form.Item>
        </Space>

        {/* FR-11. Đặt TRƯỚC ô giải thích vì ảnh thuộc về đề bài — người soạn đọc từ trên xuống sẽ
            gặp nó cùng lúc với nội dung câu hỏi, không phải sau phần hậu kiểm */}
        <Form.Item
          label="Ảnh minh hoạ"
          help="Không bắt buộc. Ảnh hiện ngay dưới nội dung câu hỏi khi người học làm bài."
        >
          <Controller
            name="imageUrl"
            control={control}
            render={({ field }) => (
              <ImageUploader value={field.value ?? null} onChange={field.onChange} />
            )}
          />
        </Form.Item>

        <Form.Item label="Giải thích đáp án" help="Hiện cho người học sau khi nộp bài">
          <Controller
            name="explanation"
            control={control}
            render={({ field }) => <Input.TextArea {...field} rows={2} />}
          />
        </Form.Item>

        {/* Chỉ câu tự luận mới cần: những loại khác chấm bằng logic, không gọi AI (features/06) */}
        {type === 'SHORT_ANSWER' && (
          <Form.Item
            label="Tiêu chí chấm"
            help="Không bắt buộc, nhưng có tiêu chí thì AI chấm ổn định hơn hẳn giữa các lần. Ví dụ: “Nêu đủ 3 nguyên nhân: mỗi ý 3 điểm; diễn đạt rõ ràng: 1 điểm”."
          >
            <Controller
              name="rubric"
              control={control}
              render={({ field }) => (
                <Input.TextArea
                  {...field}
                  rows={3}
                  placeholder="Mỗi ý đúng được bao nhiêu điểm, thiếu gì thì trừ bao nhiêu…"
                />
              )}
            />
          </Form.Item>
        )}
      </Form>
    </Modal>
  )
}
