import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Alert, Button, Modal, Progress, Skeleton, Space, Tag, Typography, message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import PageHeader from '@/shared/components/PageHeader'
import StrictExamGate, { StrictExamReminder } from '@/features/integrity/components/StrictExamGate'
import { useProctoring } from '@/features/integrity/hooks/useProctoring'
import { useStrictExam } from '@/features/integrity/hooks/useStrictExam'
import { QUESTION_TYPE_LABEL } from '@/features/quiz/constants'
import {
  attemptApi,
  type AnswerFeedback,
  type AnswerPayload,
  type AttemptDetail,
  type AttemptQuestion,
} from '../api/attemptApi'
import AnswerInput from '../components/AnswerInput'
import AttemptTimer from '../components/AttemptTimer'
import QuestionReview from '../components/QuestionReview'
import { MODE_LABEL, STATUS_COLOR, STATUS_LABEL, formatDuration } from '../constants'
import { useAnswerQuestion, useAttempt, useSubmitAttempt } from '../hooks/useAttemptQueries'
import ProctoringNotice from '@/features/integrity/components/ProctoringNotice'
import ProctoringLiveCount from '@/features/integrity/components/ProctoringLiveCount'

const { Text, Paragraph } = Typography

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
  const [dangTimCauSau, setDangTimCauSau] = useState(false)

  const question = questions[index]
  const answeredCount = questions.filter((q) => isAnswered(draft[q.questionId])).length
  const unanswered = questions.length - answeredCount
  const isPractice = attempt.mode === 'PRACTICE'
  const locked = isPractice && Boolean(feedback[question.questionId])

  // Thu tín hiệu hành vi CHỈ ở chế độ thi (features/12). Luyện tập không bị theo dõi — và người dùng được
  // nói rõ điều đó ngay trên màn hình, xem khối thông báo bên dưới. Server cũng từ chối lượt PRACTICE, nên
  // đây là lớp thứ hai chứ không phải lớp duy nhất.
  const soLanGhiNhan = useProctoring(attempt.id, !isPractice)

  // FR-48. Dùng thẳng `attempt.strictExam` — KHÔNG tự nhân với `!isPractice` ở đây: server đã tính rồi, và
  // tính lại ở client là mở đường cho hai bên nói khác nhau.
  const { dangToanManHinh, vaoToanManHinh } = useStrictExam(attempt.strictExam)

  /**
   * Đã từng vào toàn màn hình cho bài này chưa.
   *
   * Phân biệt hai trạng thái mà nếu gộp lại thì giao diện sai ở một trong hai: **chưa bắt đầu** (che đề,
   * hiện cửa vào) và **đã vào rồi nhưng vừa thoát ra** (hiện đề, chỉ nhắc). Chỉ nhìn `dangToanManHinh` thì
   * người bấm nhầm Esc giữa bài bị che mất đề đang làm dở.
   */
  const [daVaoToanManHinh, setDaVaoToanManHinh] = useState(false)

  const moToanManHinh = async () => {
    const duoc = await vaoToanManHinh()
    if (duoc) {
      setDaVaoToanManHinh(true)
    } else {
      // Trình duyệt từ chối (Safari trên iPhone không có Fullscreen API cho phần tử thường). Cho làm bài
      // chứ không chặn: chặn là biến một hạn chế của thiết bị thành mất quyền dự thi.
      setDaVaoToanManHinh(true)
      message.warning('Trình duyệt của bạn không vào được toàn màn hình. Bạn vẫn làm bài bình thường.')
    }
  }

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

  /**
   * Sang câu kế tiếp.
   *
   * Chế độ LUYỆN TẬP hỏi server câu nào nên hỏi tiếp (FR-32): sai hai câu liền thì gặp câu dễ hơn, đúng
   * hai câu liền thì gặp câu khó hơn. **Bộ đề không đổi** — chỉ thứ tự đổi, nên điểm vẫn so được giữa
   * các người học.
   *
   * Chế độ THI đi tuần tự: thi là để đo, mọi người phải làm cùng một đề theo cùng một thứ tự.
   *
   * Server hỏng hoặc trả null thì lùi về câu kế tiếp theo thứ tự — người đang làm bài không được kẹt lại
   * vì một tính năng phụ trợ gặp sự cố.
   */
  const sangCauSau = async () => {
    if (!isPractice) {
      setIndex(index + 1)
      return
    }
    setDangTimCauSau(true)
    try {
      const nextId = await attemptApi.nextQuestion(attempt.id)
      const viTri = nextId ? questions.findIndex((q) => q.questionId === nextId) : -1
      setIndex(viTri >= 0 ? viTri : index + 1)
    } catch {
      setIndex(index + 1)
    } finally {
      setDangTimCauSau(false)
    }
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

      {/* Minh bạch là ràng buộc của features/12, không phải chi tiết trang trí: thu tín hiệu hành vi mà
          không nói với người bị thu là làm sau lưng họ. Chỉ hiện ở chế độ thi vì luyện tập không thu gì. */}
      {!isPractice && <ProctoringNotice compact />}
      {!isPractice && <ProctoringLiveCount soLan={soLanGhiNhan} />}

      {/* Đã thoát toàn màn hình giữa chừng: nhắc, KHÔNG che đề. Che đi là phạt người bấm nhầm Esc bằng
          cách chặn họ làm tiếp, trong khi tín hiệu đã ghi rồi */}
      {attempt.strictExam && !dangToanManHinh && daVaoToanManHinh && (
        <StrictExamReminder onVao={() => void moToanManHinh()} />
      )}

      {/* Cửa vào: che đề cho tới khi người học chủ động vào toàn màn hình. Chỉ hiện một dải cảnh báo mà
          bên dưới vẫn đọc được đề thì không ai bấm, và cả chế độ nghiêm ngặt thành dòng chữ trang trí */}
      {attempt.strictExam && !daVaoToanManHinh ? (
        <StrictExamGate dangToanManHinh={dangToanManHinh} onVao={() => void moToanManHinh()} />
      ) : (
      <div className="grid gap-6 lg:grid-cols-[1fr_260px]">
        <div className="flex flex-col gap-4">
          {locked ? (
            <QuestionReview question={mergeFeedback(question, feedback[question.questionId])} />
          ) : (
            <div className="soft-panel p-5">
              <div className="mb-3 flex flex-wrap items-center gap-2">
                <Text className="text-ink-soft text-xs font-bold">
                  Câu {index + 1}/{questions.length}
                </Text>
                <Tag className="mr-0!">{QUESTION_TYPE_LABEL[question.type]}</Tag>
                <Text className="ml-auto text-xs font-bold">{question.maxScore} điểm</Text>
              </div>

              <Paragraph className="mb-4! text-base font-bold!">{question.content}</Paragraph>

              {/* FR-11. Giới hạn chiều cao thay vì để ảnh chiếm cả màn: người học cần thấy ảnh VÀ các
                  lựa chọn cùng lúc, không phải cuộn qua lại giữa đề và đáp án khi đang tính giờ */}
              {question.imageUrl && (
                <img
                  src={question.imageUrl}
                  alt="Ảnh minh hoạ của câu hỏi"
                  className="mb-4 max-h-72 w-auto max-w-full border border-line rounded-card object-contain"
                />
              )}

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
              loading={dangTimCauSau}
              onClick={() => void sangCauSau()}
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

          <div className="soft-panel p-4">
            <Text className="text-ink-soft text-xs">Tiến độ</Text>
            <Progress
              percent={Math.round((answeredCount / questions.length) * 100)}
              size="small"
              strokeColor="#1c1d1f"
            />
            <Text className="text-xs font-bold">
              {answeredCount}/{questions.length} câu đã trả lời
            </Text>

            {/* Nhiều cột hơn ở màn hẹp, ít cột lại ở màn rộng — nghe ngược nhưng đúng: từ `lg` trở
                lên khối này nằm trong cột phụ rộng 260px, còn dưới `lg` nó xuống dòng và giãn hết
                chiều ngang màn hình. Giữ 5 cột ở đó thì mỗi nút rộng gần 70px, thưa thớt và đẩy danh
                sách câu dài xuống quá xa. */}
            <div className="mt-3 grid grid-cols-8 gap-1 sm:grid-cols-10 lg:grid-cols-5">
              {questions.map((q, i) => {
                const done = isAnswered(draft[q.questionId])
                return (
                  <button
                    key={q.questionId}
                    type="button"
                    onClick={() => setIndex(i)}
                    /* `text-canvas` chứ không `text-white`: xem chú thích ở `chipClass` của
                       BrowseQuizzesPage. Ở đây hậu quả nặng hơn — ô biến mất là ô CÂU ĐANG LÀM. */
                    className={`h-8 rounded-small border text-xs font-bold ${
                      i === index
                        ? 'border-ink bg-ink text-canvas'
                        : done
                          ? 'border-line bg-surface-subtle text-ink'
                          : 'border-line bg-surface text-ink-soft'
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
      )}
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

/**
 * Nền khối công bố điểm, đổi theo tỉ lệ đúng.
 *
 * Bốn mức thay vì hai: chỉ "đạt / không đạt" thì một bài 51% và một bài 99% trông y hệt nhau, mà đó là
 * hai kết quả rất khác nhau với người vừa làm xong.
 *
 * Mức thấp nhất dùng **xám đá, không dùng đỏ**. Đỏ ở đây đọc thành một lời phán xét, trong khi một bài
 * luyện tập điểm thấp chỉ có nghĩa là còn chỗ để ôn — và ôn tiếp mới là việc hệ thống muốn người học làm.
 */
function tongDiemTone(percent: number): string {
  if (percent >= 90) return 'result-hero-excellent'
  if (percent >= 70) return 'result-hero-good'
  if (percent >= 50) return 'result-hero-pass'
  return 'result-hero-low'
}

function nhanKetQua(percent: number): string {
  if (percent >= 90) return 'Xuất sắc'
  if (percent >= 70) return 'Khá'
  if (percent >= 50) return 'Đạt'
  return 'Cần ôn thêm'
}

function AttemptResult({ detail }: { detail: AttemptDetail }) {
  const { attempt, questions } = detail

  const percent = attempt.maxScore > 0 ? Math.round((attempt.totalScore / attempt.maxScore) * 100) : 0
  // Lấy thẳng từ backend thay vì tự đếm: cùng một con số quyết định có hỏi lại hay không
  // (xem `useAttempt`), hai chỗ đếm hai kiểu là mầm mống lệch nhau.
  const pendingAi = detail.gradingPending
  // Biết là "đang xếp hàng" hay "sắp xong" thì nói khác nhau — cùng một vòng quay câm là tệ nhất
  const throttled = detail.aiThrottledSeconds
  const failedAi = useMemo(
    () => questions.filter((q) => q.gradedBy === 'AI_FAILED').length,
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

      {/* Khối công bố điểm.

          Trước đây bốn ô thống kê trắng như nhau, trong đó điểm số — thứ người học chờ suốt cả bài —
          nằm ngang hàng với "thời gian làm". Đây là khoảnh khắc cảm xúc nhất của cả luồng học, nên nó
          được một khối riêng, và MÀU ĐỔI THEO KẾT QUẢ: người học biết mình làm thế nào trước cả khi
          đọc con số.

          Màu vẫn đi kèm chữ ("Xuất sắc", "Đạt"…) chứ không thay chữ: người mù màu phải đọc được cùng
          một thông tin, và một mảng màu không tự nói được nó nghĩa là gì. */}
      <div className={`flex flex-wrap items-end justify-between gap-4 p-6 ${tongDiemTone(percent)}`}>
        <div>
          <div className="text-sm font-bold text-white/85">{nhanKetQua(percent)}</div>
          <div className="text-5xl leading-tight font-bold text-white">
            {attempt.totalScore}
            <span className="text-2xl text-white/70">/{attempt.maxScore}</span>
          </div>
          <div className="text-sm text-white/85">
            Đúng {attempt.correctCount}/{attempt.questionCount} câu · {percent}%
          </div>
        </div>
        <div className="text-sm text-white/85">
          Thời gian làm: <b className="text-white">{formatDuration(attempt.durationSec)}</b>
        </div>
      </div>

      {pendingAi > 0 && (
        <Alert
          type="info"
          showIcon
          message={
            throttled > 0
              ? `Đang xếp hàng chờ dịch vụ AI — khoảng ${throttled} giây nữa`
              : `AI đang chấm ${pendingAi} câu tự luận`
          }
          description={
            throttled > 0
              ? `Dịch vụ AI đang bận, ${pendingAi} câu tự luận phải chờ tới lượt. Bạn cứ đóng trang, điểm vẫn được chấm và lưu lại.`
              : 'Điểm đang hiển thị là điểm tạm, phần tự luận chưa được cộng. Trang tự cập nhật khi chấm xong — không cần tải lại.'
          }
        />
      )}

      {failedAi > 0 && (
        <Alert
          type="warning"
          showIcon
          message={`${failedAi} câu chưa chấm tự động được`}
          description="Những câu này đang tính 0 điểm và cần giáo viên chấm tay. Liên hệ người tạo quiz nếu bạn cho rằng điểm chưa đúng."
        />
      )}

      <div className="flex flex-col gap-4">
        {questions.map((question) => (
          <QuestionReview
            key={question.questionId}
            question={question}
            attemptId={attempt.id}
            throttledSeconds={throttled}
          />
        ))}
      </div>
    </Space>
  )
}
