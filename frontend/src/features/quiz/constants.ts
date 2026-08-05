import type { Difficulty, QuestionType, Visibility } from './api/quizApi'

export const DIFFICULTY_LABEL: Record<Difficulty, string> = {
  EASY: 'Dễ',
  MEDIUM: 'Trung bình',
  HARD: 'Khó',
}

export const DIFFICULTY_COLOR: Record<Difficulty, string> = {
  EASY: 'green',
  MEDIUM: 'gold',
  HARD: 'red',
}

export const VISIBILITY_LABEL: Record<Visibility, string> = {
  PUBLIC: 'Công khai',
  PRIVATE: 'Riêng tư',
}

export const QUESTION_TYPE_LABEL: Record<QuestionType, string> = {
  SINGLE_CHOICE: 'Một đáp án',
  MULTIPLE_CHOICE: 'Nhiều đáp án',
  TRUE_FALSE: 'Đúng / Sai',
  FILL_BLANK: 'Điền chỗ trống',
  SHORT_ANSWER: 'Trả lời ngắn',
}

/** Mô tả luật của từng loại — hiển thị cho người soạn đề, khớp luật ở QuestionService. */
export const QUESTION_TYPE_HINT: Record<QuestionType, string> = {
  SINGLE_CHOICE: 'Ít nhất 2 lựa chọn, đúng 1 đáp án đúng.',
  MULTIPLE_CHOICE: 'Ít nhất 3 lựa chọn, từ 2 đáp án đúng và phải còn ít nhất 1 lựa chọn sai.',
  TRUE_FALSE: 'Đúng 2 lựa chọn, chọn 1 đáp án đúng.',
  FILL_BLANK: 'Mỗi dòng là một đáp án được chấp nhận (hệ thống tự coi tất cả là đúng).',
  SHORT_ANSWER: 'Một đáp án mẫu duy nhất; AI sẽ đối chiếu khi chấm.',
}

export const DIFFICULTY_OPTIONS = (['EASY', 'MEDIUM', 'HARD'] as Difficulty[]).map((value) => ({
  value,
  label: DIFFICULTY_LABEL[value],
}))

export const QUESTION_TYPE_OPTIONS = (Object.keys(QUESTION_TYPE_LABEL) as QuestionType[]).map(
  (value) => ({ value, label: QUESTION_TYPE_LABEL[value] }),
)
