import { Tag, Typography } from 'antd'
import type { QuizSummary } from '../api/quizApi'
import { DIFFICULTY_COLOR, DIFFICULTY_LABEL } from '../constants'
import { coverOf } from '../coverGradient'

const { Text } = Typography

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
      {/* Ảnh bìa 16:9. Quiz chưa có ảnh thì vẽ khối màu theo tiêu đề thay vì để trống. */}
      {quiz.thumbnailUrl ? (
        <div className="relative aspect-video">
          <img
            src={quiz.thumbnailUrl}
            alt=""
            loading="lazy"
            className="h-full w-full object-cover"
          />
          {/* Lớp phủ tối dần để chữ danh mục đọc được trên ảnh sáng */}
          <span className="absolute inset-x-0 bottom-0 bg-linear-to-t from-black/60 to-transparent p-3 text-xs font-bold text-white">
            {quiz.categoryName ?? 'Chưa phân loại'}
          </span>
        </div>
      ) : (
        <div className="flex aspect-video items-end p-3" style={{ background: coverOf(quiz.title) }}>
          <span className="text-xs font-bold text-white/90">
            {quiz.categoryName ?? 'Chưa phân loại'}
          </span>
        </div>
      )}

      <div className="flex flex-1 flex-col gap-1 p-3">
        <h3 className="line-clamp-2-title mb-0! text-base leading-snug font-bold text-ink">
          {quiz.title}
        </h3>

        <Text className="text-ink-soft text-xs">{quiz.ownerDisplayName}</Text>

        <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1">
          <Text className="text-ink! text-xs font-bold">{quiz.questionCount} câu hỏi</Text>
          {minutes && <Text className="text-ink-soft text-xs">· {minutes} phút</Text>}
          {/* Ẩn HẲN khi chưa ai làm, không hiện "0 người đã làm": số 0 đọc như một lời chê và phạt oan
              mọi quiz mới, trong khi thứ nó thật sự nói chỉ là "chưa ai kịp làm".

              Nhãn là "người" chứ không phải "lượt" — một người luyện tập 50 lần không làm quiz này thành
              phổ biến, và backend cũng đếm `distinct user_id` đúng theo nghĩa đó. */}
          {quiz.learnerCount > 0 && (
            <Text className="text-ink-soft text-xs">· {quiz.learnerCount} người đã làm</Text>
          )}
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
