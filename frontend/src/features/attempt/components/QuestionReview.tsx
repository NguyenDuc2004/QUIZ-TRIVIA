import { useState } from 'react'
import { Button, Spin, Tag, Typography } from 'antd'
import { QUESTION_TYPE_LABEL } from '@/features/quiz/constants'
import type { AttemptQuestion } from '../api/attemptApi'
import { GRADED_BY_LABEL } from '../constants'
import { useExplainAnswer } from '../hooks/useAttemptQueries'

const { Text, Paragraph } = Typography

/** Nhãn trạng thái của một câu sau khi chấm. */
function verdict(question: AttemptQuestion) {
  if (question.gradedBy === 'PENDING_AI') {
    return <Tag color="purple">AI đang chấm…</Tag>
  }
  if (question.gradedBy === 'AI_FAILED') {
    return <Tag color="orange">Chưa chấm được</Tag>
  }
  // Câu tự luận thường được điểm một phần: "Đúng/Sai" không mô tả đúng, hiện điểm mới đúng
  if (question.gradedBy === 'AI' && question.correct !== true && (question.score ?? 0) > 0) {
    return <Tag color="gold">Đúng một phần</Tag>
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
export default function QuestionReview({
  question,
  attemptId,
  throttledSeconds = 0,
}: {
  question: AttemptQuestion
  /** Có id bài làm thì hiện nút nhờ AI giải thích; bỏ trống thì ẩn (dùng ở màn không có bài). */
  attemptId?: string
  /** Lớn hơn 0 nghĩa là đang chờ hạn mức AI, không phải sắp chấm xong. */
  throttledSeconds?: number
}) {
  const [explanation, setExplanation] = useState<string | null>(null)
  const explain = useExplainAnswer(attemptId)
  const chosen = new Set(question.userAnswer?.optionIds ?? [])
  const correctIds = new Set(question.correctOptionIds ?? [])
  const isChoice = ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(question.type)

  return (
    <div className="border border-line bg-surface p-5">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <Text className="text-ink-soft text-xs font-bold">Câu {question.orderIndex + 1}</Text>
        <Tag className="mr-0!">{QUESTION_TYPE_LABEL[question.type]}</Tag>
        {verdict(question)}
        <Text className="ml-auto text-xs font-bold">
          {question.score ?? 0}/{question.maxScore} điểm
        </Text>
      </div>

      <Paragraph className="mb-4! text-base font-bold!">{question.content}</Paragraph>

      {/* FR-11. Xem lại cũng phải có ảnh: người học đối chiếu đáp án với ĐỀ BÀI, mà thiếu ảnh thì nhiều
          câu không còn hiểu được — "hình nào sau đây là đồ thị hàm số?" mà không có hình là câu vô nghĩa */}
      {question.imageUrl && (
        <img
          src={question.imageUrl}
          alt="Ảnh minh hoạ của câu hỏi"
          className="mb-4 max-h-72 w-auto max-w-full border border-line object-contain"
        />
      )}

      {isChoice ? (
        <div className="flex flex-col gap-2">
          {question.options.map((option) => {
            const isCorrect = correctIds.has(option.id)
            const isChosen = chosen.has(option.id)
            // Xanh = đáp án đúng; đỏ = người dùng chọn nhưng sai
            const tone = isCorrect
              ? 'border-green-500 bg-correct'
              : isChosen
                ? 'border-red-500 bg-wrong'
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
            <div className="border border-green-500 bg-correct p-3">
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

      {question.gradedBy === 'PENDING_AI' && (
        <div className="mt-3 flex items-center gap-2 border border-line bg-surface-subtle p-3">
          <Spin size="small" />
          <Text className="text-ink-soft text-xs">
            {throttledSeconds > 0
              ? `Đang chờ tới lượt gọi dịch vụ AI — khoảng ${throttledSeconds} giây nữa.`
              : 'AI đang chấm câu này. Điểm sẽ tự cập nhật, không cần tải lại trang.'}
          </Text>
        </div>
      )}

      {question.aiFeedback && (
        <div className="mt-3 border border-line p-3">
          <Text className="text-ink-soft text-xs font-bold">
            Nhận xét · {GRADED_BY_LABEL[question.gradedBy ?? 'AI']}
          </Text>
          <Paragraph className="mb-0! whitespace-pre-wrap">{question.aiFeedback}</Paragraph>

          {question.aiSuggestions && (
            <>
              <Text className="mt-3 block text-ink-soft text-xs font-bold">Để làm tốt hơn</Text>
              <Paragraph className="mb-0! whitespace-pre-wrap">{question.aiSuggestions}</Paragraph>
            </>
          )}
        </div>
      )}

      {/* Giải thích do AI viết theo yêu cầu — khác với phần Giải thích cố định do Creator soạn.
          Mỗi lần bấm là một lời gọi mô hình nên chỉ gọi khi người học chủ động muốn. */}
      {attemptId && (
        <div className="mt-3">
          {explanation ? (
            <div className="border-l-2 border-brand bg-surface-subtle p-3">
              <Text className="text-ink-soft text-xs font-bold">AI giải thích</Text>
              <Paragraph className="mb-0! whitespace-pre-wrap">{explanation}</Paragraph>
            </div>
          ) : (
            <Button
              size="small"
              loading={explain.isPending}
              onClick={() =>
                explain.mutate(question.answerId, {
                  onSuccess: (data) =>
                    setExplanation(data.explanation || 'AI chưa đưa ra được giải thích cho câu này.'),
                })
              }
            >
              Nhờ AI giải thích
            </Button>
          )}
        </div>
      )}
    </div>
  )
}
