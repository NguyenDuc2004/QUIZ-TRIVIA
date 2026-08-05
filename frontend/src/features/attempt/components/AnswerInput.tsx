import { Checkbox, Input, Radio, Space } from 'antd'
import type { AnswerPayload, AttemptQuestion } from '../api/attemptApi'

const { TextArea } = Input

/**
 * Ô trả lời của một câu, đổi theo loại câu hỏi.
 * <p>
 * Trắc nghiệm ghi vào {@code optionIds}, điền khuyết/tự luận ghi vào {@code text} —
 * đúng hình dạng payload backend chờ nhận.
 */
export default function AnswerInput({
  question,
  value,
  disabled,
  onChange,
  onCommit,
}: {
  question: AttemptQuestion
  value: AnswerPayload
  disabled?: boolean
  /** Đổi giá trị đang chọn/đang gõ (chưa gửi lên server). */
  onChange: (next: AnswerPayload) => void
  /** Chốt câu trả lời và gửi lên server. */
  onCommit: (payload: AnswerPayload) => void
}) {
  const selected = value.optionIds ?? []

  const commitOptions = (optionIds: string[]) => {
    const next = { optionIds }
    onChange(next)
    onCommit(next)
  }

  switch (question.type) {
    case 'SINGLE_CHOICE':
    case 'TRUE_FALSE':
      return (
        <Radio.Group
          disabled={disabled}
          value={selected[0]}
          onChange={(event) => commitOptions([event.target.value])}
          className="w-full"
        >
          <Space direction="vertical" size={8} className="w-full">
            {question.options.map((option) => (
              <Radio key={option.id} value={option.id} className="w-full border border-line p-3">
                {option.content}
              </Radio>
            ))}
          </Space>
        </Radio.Group>
      )

    case 'MULTIPLE_CHOICE':
      return (
        <Checkbox.Group
          disabled={disabled}
          value={selected}
          onChange={(values) => commitOptions(values as string[])}
          className="w-full"
        >
          <Space direction="vertical" size={8} className="w-full">
            {question.options.map((option) => (
              <Checkbox key={option.id} value={option.id} className="w-full border border-line p-3">
                {option.content}
              </Checkbox>
            ))}
          </Space>
        </Checkbox.Group>
      )

    case 'FILL_BLANK':
      return (
        <Input
          size="large"
          disabled={disabled}
          placeholder="Nhập đáp án"
          value={value.text ?? ''}
          onChange={(event) => onChange({ text: event.target.value })}
          // Gửi khi rời ô hoặc bấm Enter, không gửi từng ký tự
          onBlur={() => onCommit({ text: value.text ?? '' })}
          onPressEnter={() => onCommit({ text: value.text ?? '' })}
        />
      )

    case 'SHORT_ANSWER':
      return (
        <TextArea
          rows={5}
          disabled={disabled}
          placeholder="Nhập câu trả lời của bạn"
          value={value.text ?? ''}
          onChange={(event) => onChange({ text: event.target.value })}
          onBlur={() => onCommit({ text: value.text ?? '' })}
        />
      )

    default:
      return null
  }
}
