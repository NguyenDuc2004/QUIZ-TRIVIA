import { Tag, Typography } from 'antd'
import { QUESTION_TYPE_LABEL } from '@/features/quiz/constants'
import type { AttemptQuestion } from '../api/attemptApi'
import { GRADED_BY_LABEL } from '../constants'

const { Text, Paragraph } = Typography

/** Nhãn trạng thái của một câu sau khi chấm. */
function verdict(question: AttemptQuestion) {
  if (question.gradedBy === 'PENDING_AI') {
    return <Tag color="purple">Chờ AI chấm</Tag>
  }
  if (question.correct === true) {
    return <Tag color="green">Đúng</Tag>
  }
  if (!question.userAnswer || isBlank(question)) {
    return <Tag>Bỏ trống</Tag>
  }
  return <Tag color="red">Sai</Tag>
}

function isBlank(question: AttemptQuestion) {
  const answer = question.userAnswer
  if (!answer) return true
  return !(answer.optionIds?.length ?? 0) && !(answer.text ?? '').trim()
}

/**
 * Một câu ở màn xem kết quả: đáp án đúng, lựa chọn người dùng đã chọn và giải thích (FR-17).
 * Chỉ dùng được với dữ liệu bài <b>đã nộp</b> — lúc đang làm backend để null hết các trường này.
 */
export default function QuestionReview({ question }: { question: AttemptQuestion }) {
  const chosen = new Set(question.userAnswer?.optionIds ?? [])
  const correctIds = new Set(question.correctOptionIds ?? [])
  const isChoice = ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(question.type)

  return (
    <div className="border border-line bg-white p-5">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <Text className="text-ink-soft text-xs font-bold">Câu {question.orderIndex + 1}</Text>
        <Tag className="mr-0!">{QUESTION_TYPE_LABEL[question.type]}</Tag>
        {verdict(question)}
        <Text className="ml-auto text-xs font-bold">
          {question.score ?? 0}/{question.maxScore} điểm
        </Text>
      </div>

      <Paragraph className="mb-4! text-base font-bold!">{question.content}</Paragraph>

      {isChoice ? (
        <div className="flex flex-col gap-2">
          {question.options.map((option) => {
            const isCorrect = correctIds.has(option.id)
            const isChosen = chosen.has(option.id)
            // Xanh = đáp án đúng; đỏ = người dùng chọn nhưng sai
            const tone = isCorrect
              ? 'border-green-500 bg-green-50'
              : isChosen
                ? 'border-red-500 bg-red-50'
                : 'border-line'

            return (
              <div key={option.id} className={`flex items-center gap-2 border p-3 ${tone}`}>
                <span className="flex-1">{option.content}</span>
                {isChosen && <Tag className="mr-0!">Bạn chọn</Tag>}
                {isCorrect && <Tag color="green" className="mr-0!">Đáp án đúng</Tag>}
              </div>
            )
          })}
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          <div className="border border-line p-3">
            <Text className="text-ink-soft text-xs">Bạn trả lời</Text>
            <Paragraph className="mb-0! whitespace-pre-wrap">
              {question.userAnswer?.text?.trim() || <Text className="text-ink-soft">(bỏ trống)</Text>}
            </Paragraph>
          </div>
          {question.options.length > 0 && (
            <div className="border border-green-500 bg-green-50 p-3">
              <Text className="text-ink-soft text-xs">
                {question.type === 'FILL_BLANK' ? 'Đáp án được chấp nhận' : 'Đáp án mẫu'}
              </Text>
              <Paragraph className="mb-0!">
                {question.options.map((option) => option.content).join(' · ')}
              </Paragraph>
            </div>
          )}
        </div>
      )}

      {question.explanation && (
        <div className="mt-4 border-l-2 border-brand bg-surface-subtle p-3">
          <Text className="text-ink-soft text-xs font-bold">Giải thích</Text>
          <Paragraph className="mb-0! whitespace-pre-wrap">{question.explanation}</Paragraph>
        </div>
      )}

      {question.aiFeedback && (
        <div className="mt-3 border border-line p-3">
          <Text className="text-ink-soft text-xs font-bold">
            Nhận xét · {GRADED_BY_LABEL[question.gradedBy ?? 'AI']}
          </Text>
          <Paragraph className="mb-0! whitespace-pre-wrap">{question.aiFeedback}</Paragraph>
        </div>
      )}

      {question.gradedBy === 'PENDING_AI' && (
        <Text className="mt-3 block text-ink-soft text-xs">
          Câu tự luận cần AI chấm — tính năng chấm tự luận sẽ bổ sung ở giai đoạn sau, hiện tạm tính 0 điểm.
        </Text>
      )}
    </div>
  )
}
