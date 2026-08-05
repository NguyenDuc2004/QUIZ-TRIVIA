import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Alert, Button, Modal, Progress, Skeleton, Space, Tag, Typography } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import PageHeader from '@/shared/components/PageHeader'
import { QUESTION_TYPE_LABEL } from '@/features/quiz/constants'
import type { AnswerFeedback, AnswerPayload, AttemptDetail, AttemptQuestion } from '../api/attemptApi'
import AnswerInput from '../components/AnswerInput'
import AttemptTimer from '../components/AttemptTimer'
import QuestionReview from '../components/QuestionReview'
import { MODE_LABEL, STATUS_COLOR, STATUS_LABEL, formatDuration } from '../constants'
import { useAnswerQuestion, useAttempt, useSubmitAttempt } from '../hooks/useAttemptQueries'

const { Text, Paragraph, Title } = Typography

const EMPTY_ANSWER: AnswerPayload = {}

function isAnswered(payload: AnswerPayload | undefined) {
  if (!payload) return false
  return (payload.optionIds?.length ?? 0) > 0 || (payload.text ?? '').trim().length > 0
}

/**
 * Màn hình một lượt làm bài — cùng một đường dẫn `/attempts/:id` phục vụ hai trạng thái:
 * bài đang làm thì hiện đề, bài đã nộp thì hiện kết quả. Backend quyết định trạng thái đó,
 * frontend chỉ đọc `attempt.status`.
 */
export default function AttemptPage() {
  const { id } = useParams<{ id: string }>()
  const { data, isPending, error } = useAttempt(id)

  if (error) {
    return <Alert type="error" showIcon message={getApiErrorMessage(error)} />
  }
  if (isPending || !data) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }

  return data.attempt.status === 'IN_PROGRESS' ? (
    <TakeAttempt detail={data} />
  ) : (
    <AttemptResult detail={data} />
  )
}

// --------------------------------------------------------------------- làm bài

function TakeAttempt({ detail }: { detail: AttemptDetail }) {
  const { attempt, questions } = detail
  const answerMutation = useAnswerQuestion(attempt.id)
  const submitMutation = useSubmitAttempt(attempt.id)

  const [index, setIndex] = useState(0)
  /** Đáp án đang soạn ở phía client, khởi tạo từ những gì server đã lưu (làm tiếp bài dở). */
  const [draft, setDraft] = useState<Record<string, AnswerPayload>>(() =>
    Object.fromEntries(questions.map((q) => [q.questionId, q.userAnswer ?? {}])),
  )
  /** Kết quả chấm ngay của chế độ luyện tập, theo từng câu. */
  const [feedback, setFeedback] = useState<Record<string, AnswerFeedback>>({})

  const question = questions[index]
  const answeredCount = questions.filter((q) => isAnswered(draft[q.questionId])).length
  const unanswered = questions.length - answeredCount
  const isPractice = attempt.mode === 'PRACTICE'
  const locked = isPractice && Boolean(feedback[question.questionId])

  const commit = (payload: AnswerPayload) => {
    answerMutation.mutate(
      {
        questionId: question.questionId,
        optionIds: payload.optionIds ?? undefined,
        text: payload.text ?? undefined,
      },
      { onSuccess: (data) => isPractice && setFeedback((prev) => ({ ...prev, [question.questionId]: data })) },
    )
  }

  const submit = () =>
    Modal.confirm({
      title: 'Nộp bài?',
      content:
        unanswered > 0
          ? `Còn ${unanswered} câu chưa trả lời, những câu đó sẽ tính 0 điểm.`
          : 'Bạn đã trả lời hết các câu. Nộp bài để xem kết quả.',
      okText: 'Nộp bài',
      cancelText: 'Làm tiếp',
      onOk: () => submitMutation.mutateAsync(),
    })

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title={attempt.quizTitle}
        description={
          <Space size={8} wrap>
            <Tag color={isPractice ? 'blue' : 'volcano'} className="mr-0!">
              Chế độ {MODE_LABEL[attempt.mode]}
            </Tag>
            <Text className="text-ink-soft">
              {questions.length} câu · thang {attempt.maxScore} điểm
            </Text>
          </Space>
        }
      />

      <div className="grid gap-6 lg:grid-cols-[1fr_260px]">
        <div className="flex flex-col gap-4">
          {locked ? (
            <QuestionReview question={mergeFeedback(question, feedback[question.questionId])} />
          ) : (
            <div className="border border-line bg-white p-5">
              <div className="mb-3 flex flex-wrap items-center gap-2">
                <Text className="text-ink-soft text-xs font-bold">
                  Câu {index + 1}/{questions.length}
                </Text>
                <Tag className="mr-0!">{QUESTION_TYPE_LABEL[question.type]}</Tag>
                <Text className="ml-auto text-xs font-bold">{question.maxScore} điểm</Text>
              </div>

              <Paragraph className="mb-4! text-base font-bold!">{question.content}</Paragraph>

              <AnswerInput
                question={question}
                value={draft[question.questionId] ?? EMPTY_ANSWER}
                onChange={(next) => setDraft((prev) => ({ ...prev, [question.questionId]: next }))}
                onCommit={commit}
              />
            </div>
          )}

          <div className="flex items-center gap-2">
            <Button disabled={index === 0} onClick={() => setIndex(index - 1)}>
              ← Câu trước
            </Button>
            <Button
              disabled={index === questions.length - 1}
              onClick={() => setIndex(index + 1)}
            >
              Câu sau →
            </Button>
            <Button
              type="primary"
              className="ml-auto"
              loading={submitMutation.isPending}
              onClick={submit}
            >
              Nộp bài
            </Button>
          </div>
        </div>

        {/* Cột phải dính theo cuộn: đồng hồ, tiến độ và lưới nhảy nhanh giữa các câu */}
        <aside className="flex h-fit flex-col gap-4 lg:sticky lg:top-24">
          {attempt.expiresAt && (
            <AttemptTimer
              expiresAt={attempt.expiresAt}
              // Hết giờ thì nộp luôn; backend cũng tự chốt nên hai bên không lệch nhau (FR-16)
              onExpire={() => submitMutation.mutate()}
            />
          )}

          <div className="border border-line bg-white p-4">
            <Text className="text-ink-soft text-xs">Tiến độ</Text>
            <Progress
              percent={Math.round((answeredCount / questions.length) * 100)}
              size="small"
              strokeColor="#1c1d1f"
            />
            <Text className="text-xs font-bold">
              {answeredCount}/{questions.length} câu đã trả lời
            </Text>

            <div className="mt-3 grid grid-cols-5 gap-1">
              {questions.map((q, i) => {
                const done = isAnswered(draft[q.questionId])
                return (
                  <button
                    key={q.questionId}
                    type="button"
                    onClick={() => setIndex(i)}
                    className={`h-8 border text-xs font-bold ${
                      i === index
                        ? 'border-ink bg-ink text-white'
                        : done
                          ? 'border-line bg-surface-subtle text-ink'
                          : 'border-line bg-white text-ink-soft'
                    }`}
                  >
                    {i + 1}
                  </button>
                )
              })}
            </div>
          </div>
        </aside>
      </div>
    </Space>
  )
}

/** Ghép phản hồi chấm ngay của chế độ luyện tập vào câu hỏi để dùng lại màn xem kết quả. */
function mergeFeedback(question: AttemptQuestion, fb: AnswerFeedback): AttemptQuestion {
  return {
    ...question,
    userAnswer: fb.userAnswer,
    correct: fb.correct,
    score: fb.score,
    correctOptionIds: fb.correctOptionIds,
    explanation: fb.explanation,
    gradedBy: fb.correct === null ? 'PENDING_AI' : 'AUTO',
  }
}

// --------------------------------------------------------------------- kết quả

function AttemptResult({ detail }: { detail: AttemptDetail }) {
  const { attempt, questions } = detail

  const percent = attempt.maxScore > 0 ? Math.round((attempt.totalScore / attempt.maxScore) * 100) : 0
  const pendingAi = useMemo(
    () => questions.filter((q) => q.gradedBy === 'PENDING_AI').length,
    [questions],
  )

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Kết quả bài làm"
        description={
          <Space size={8} wrap>
            <Link to={`/quizzes/${attempt.quizId}`} className="font-bold">
              {attempt.quizTitle}
            </Link>
            <Tag color={STATUS_COLOR[attempt.status]} className="mr-0!">
              {STATUS_LABEL[attempt.status]}
            </Tag>
            <Tag className="mr-0!">Chế độ {MODE_LABEL[attempt.mode]}</Tag>
          </Space>
        }
        actions={
          <>
            <Link to="/my-attempts">
              <Button>Lịch sử làm bài</Button>
            </Link>
            <Link to={`/quizzes/${attempt.quizId}`}>
              <Button type="primary">Làm lại</Button>
            </Link>
          </>
        }
      />

      <div className="grid gap-4 sm:grid-cols-4">
        <Stat label="Điểm" value={`${attempt.totalScore}/${attempt.maxScore}`} highlight />
        <Stat label="Tỷ lệ" value={`${percent}%`} />
        <Stat label="Số câu đúng" value={`${attempt.correctCount}/${attempt.questionCount}`} />
        <Stat label="Thời gian làm" value={formatDuration(attempt.durationSec)} />
      </div>

      {pendingAi > 0 && (
        <Alert
          type="info"
          showIcon
          message={`${pendingAi} câu tự luận đang chờ AI chấm nên hiện tính 0 điểm. Điểm cuối có thể cao hơn.`}
        />
      )}

      <div className="flex flex-col gap-4">
        {questions.map((question) => (
          <QuestionReview key={question.questionId} question={question} />
        ))}
      </div>
    </Space>
  )
}

function Stat({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="border border-line bg-white p-4">
      <Text className="text-ink-soft text-xs">{label}</Text>
      <Title level={3} className={`mb-0! ${highlight ? 'text-brand-strong!' : ''}`}>
        {value}
      </Title>
    </div>
  )
}
