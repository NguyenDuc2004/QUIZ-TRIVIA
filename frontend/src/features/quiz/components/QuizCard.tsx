import { Tag, Typography } from 'antd'
import type { QuizSummary } from '../api/quizApi'
import { DIFFICULTY_COLOR, DIFFICULTY_LABEL } from '../constants'

const { Text } = Typography

/** Bảng màu khối ảnh bìa giả lập — chọn theo ký tự đầu của tiêu đề cho ổn định. */
const COVER_GRADIENTS = [
  'linear-gradient(135deg, #5624d0, #a435f0)',
  'linear-gradient(135deg, #1c1d1f, #6a6f73)',
  'linear-gradient(135deg, #0e6e5c, #19857b)',
  'linear-gradient(135deg, #b4690e, #e59819)',
  'linear-gradient(135deg, #2d2f31, #5624d0)',
]

function coverOf(title: string) {
  const index = title.charCodeAt(0) % COVER_GRADIENTS.length
  return COVER_GRADIENTS[index]
}

/**
 * Card quiz cho các trang kiểu "học viên" (docs/ui-design-system.md §1).
 * <p>
 * Chỗ Udemy hiển thị điểm đánh giá thì ở đây hiển thị **dữ liệu thật**: số câu, độ khó,
 * thời lượng, người tạo — không bịa số sao hay số lượt học (§7).
 */
export default function QuizCard({ quiz, onClick }: { quiz: QuizSummary; onClick?: () => void }) {
  const minutes = quiz.timeLimitSec ? Math.round(quiz.timeLimitSec / 60) : null

  return (
    <article
      className="browse-card flex h-full cursor-pointer flex-col overflow-hidden"
      onClick={onClick}
    >
      {/* Khối bìa 16:9 — thay cho ảnh khoá học của Udemy khi quiz chưa có ảnh */}
      <div
        className="flex aspect-video items-end p-3"
        style={{ background: coverOf(quiz.title) }}
      >
        <span className="text-xs font-bold text-white/90">
          {quiz.categoryName ?? 'Chưa phân loại'}
        </span>
      </div>

      <div className="flex flex-1 flex-col gap-1 p-3">
        <h3 className="line-clamp-2-title mb-0! text-base leading-snug font-bold text-ink">
          {quiz.title}
        </h3>

        <Text className="text-ink-soft text-xs">{quiz.ownerDisplayName}</Text>

        <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1">
          <Text className="text-ink! text-xs font-bold">{quiz.questionCount} câu hỏi</Text>
          {minutes && <Text className="text-ink-soft text-xs">· {minutes} phút</Text>}
        </div>

        <div className="mt-auto flex items-center gap-1 pt-2">
          <Tag color={DIFFICULTY_COLOR[quiz.difficulty]} className="mr-0!">
            {DIFFICULTY_LABEL[quiz.difficulty]}
          </Tag>
          {quiz.aiGenerated && <Tag className="mr-0!">AI sinh</Tag>}
        </div>
      </div>
    </article>
  )
}
