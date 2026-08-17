import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Input,
  InputNumber,
  Skeleton,
  Space,
  Tag,
  Typography,
} from 'antd'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import IntegrityCard from '@/features/integrity/components/IntegrityCard'
import { useIntegrityReport } from '@/features/integrity/hooks/useIntegrity'
import type { EssayAnswer, GradedBy } from '../api/analyticsApi'
import { useGradingView, useOverrideGrade } from '../hooks/useAnalyticsQueries'

const { Text, Paragraph } = Typography

/** Nhãn trạng thái chấm — người chấm cần biết con số đang thấy đến từ đâu. */
const GRADED_BY: Record<GradedBy, { label: string; color?: string }> = {
  NOT_GRADED: { label: 'Chưa chấm' },
  AUTO: { label: 'Máy chấm', color: 'success' },
  PENDING_AI: { label: 'AI đang chấm', color: 'processing' },
  AI: { label: 'AI đã chấm', color: 'blue' },
  AI_FAILED: { label: 'AI không chấm được', color: 'warning' },
  HUMAN: { label: 'Bạn đã chấm', color: 'success' },
}

/**
 * Màn chấm tay câu tự luận (FR-30) — **món nợ của features/06 mà lát cắt 9 trả**.
 * <p>
 * Lát cắt 6 làm xong API ghi đè điểm nhưng không có màn hình nào dẫn tới nó, nên trên thực tế tính
 * năng chấm tay chưa dùng được: chủ quiz vừa không tìm được bài cần chấm, vừa không đọc được bài để
 * mà chấm.
 * <p>
 * Trang chỉ hiện **câu tự luận**. Câu trắc nghiệm máy chấm theo đáp án cố định, không có gì để người
 * xem lại; đưa chúng vào đây chỉ mở rộng phần bài làm mà chủ quiz đọc được, không đổi lấy gì.
 */
export default function GradeAttemptPage() {
  const { id: quizId, attemptId } = useParams<{ id: string; attemptId: string }>()
  const { data, isPending, isError } = useGradingView(attemptId)
  // Lượt luyện tập không có báo cáo và endpoint trả 404 — hook đã tắt retry, ở đây chỉ cần không hiện gì.
  // Không dựng chỗ trống "chưa có dữ liệu giám sát": nói vậy nghe như đang thiếu, trong khi luyện tập
  // KHÔNG được giám sát là chủ ý.
  const { data: toanVen } = useIntegrityReport(attemptId)

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }

  if (isError || !data) {
    return (
      <EmptyState
        title="Không mở được bài làm này"
        hint="Bài không tồn tại, hoặc nó không thuộc quiz của bạn"
      />
    )
  }

  const pending = data.answers.filter((answer) => answer.needsGrading)

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Chấm bài tự luận"
        description={
          <>
            {data.quizTitle} · <Link to={`/my-quizzes/${quizId}/stats`}>Về trang thống kê</Link>
          </>
        }
      />

      <Descriptions bordered size="small" column={3}>
        <Descriptions.Item label="Người học">{data.learnerName}</Descriptions.Item>
        <Descriptions.Item label="Nộp lúc">
          {data.submittedAt ? new Date(data.submittedAt).toLocaleString('vi-VN') : '—'}
        </Descriptions.Item>
        <Descriptions.Item label="Điểm hiện tại">
          <Text className="font-bold">
            {data.totalScore}/{data.maxScore}
          </Text>
        </Descriptions.Item>
      </Descriptions>

      {pending.length > 0 && (
        <Alert
          type="warning"
          showIcon
          message={`${pending.length} câu đang chờ bạn chấm`}
          description="Điểm tổng ở trên chưa tính những câu này, nên nó chưa phải điểm cuối của bài."
        />
      )}

      {/* Đặt TRƯỚC phần bài làm, sau thông tin bài: người chấm cần biết bài này có gì đáng lưu ý trước
          khi đọc và cho điểm, chứ không phải sau khi đã chấm xong (features/12, FR-47) */}
      {toanVen && <IntegrityCard report={toanVen} />}

      {data.answers.length === 0 ? (
        <EmptyState
          title="Bài này không có câu tự luận"
          hint="Toàn bộ câu trong bài đều được máy chấm theo đáp án cố định"
        />
      ) : (
        data.answers.map((answer) => (
          <EssayCard key={answer.answerId} answer={answer} attemptId={attemptId!} />
        ))
      )}
    </Space>
  )
}

/** Một câu tự luận: đề, tiêu chí, bài làm, nhận xét của AI, và ô nhập điểm. */
function EssayCard({ answer, attemptId }: { answer: EssayAnswer; attemptId: string }) {
  // Câu AI chưa chấm được có score = 0. Đổ 0 vào ô nhập là mớm cho người chấm một con số vô nghĩa,
  // rồi họ bấm lưu và bài thành 0 điểm thật. Để trống buộc phải tự quyết định.
  const [score, setScore] = useState<number | null>(answer.needsGrading ? null : answer.score)
  const [feedback, setFeedback] = useState(answer.aiFeedback ?? '')
  const grade = useOverrideGrade(attemptId)

  const status = GRADED_BY[answer.gradedBy]

  return (
    <Card
      title={
        <div className="flex flex-wrap items-center gap-2">
          <Text className="font-bold">Câu {answer.orderIndex + 1}</Text>
          <Tag color={status.color} className="mr-0!">
            {status.label}
          </Tag>
          {/* Chỉ hiện điểm khi con số có nghĩa — với AI_FAILED/PENDING_AI thì 0 không phải điểm */}
          {!answer.needsGrading && (
            <Text className="text-ink-soft text-sm">
              {answer.score}/{answer.maxScore} điểm
            </Text>
          )}
        </div>
      }
    >
      <Space direction="vertical" size="middle" className="w-full">
        <div>
          <Text className="text-ink-soft text-xs">Đề bài</Text>
          <Paragraph className="mb-0!">{answer.questionContent}</Paragraph>
        </div>

        {/* Tiêu chí và đáp án mẫu là đúng những gì AI đã nhìn thấy — người chấm lại phải thấy y hệt,
            nếu không thì hai lượt chấm dựa trên hai chuẩn khác nhau */}
        {answer.rubric && (
          <div className="border-l-2 border-line pl-3">
            <Text className="text-ink-soft text-xs">Tiêu chí chấm</Text>
            <Paragraph className="mb-0! whitespace-pre-wrap">{answer.rubric}</Paragraph>
          </div>
        )}

        {answer.sampleAnswer && (
          <div className="border-l-2 border-line pl-3">
            <Text className="text-ink-soft text-xs">Đáp án mẫu</Text>
            <Paragraph className="mb-0!">{answer.sampleAnswer}</Paragraph>
          </div>
        )}

        <div className="bg-surface-subtle p-3">
          <Text className="text-ink-soft text-xs">Bài làm của người học</Text>
          {answer.learnerAnswer ? (
            <Paragraph className="mb-0! whitespace-pre-wrap">{answer.learnerAnswer}</Paragraph>
          ) : (
            <Paragraph className="text-ink-soft mb-0! italic">Để trống, không trả lời</Paragraph>
          )}
        </div>

        {answer.gradedBy === 'AI' && answer.aiFeedback && (
          <div>
            <Text className="text-ink-soft text-xs">AI nhận xét</Text>
            <Paragraph className="mb-0!">{answer.aiFeedback}</Paragraph>
            {answer.aiSuggestions && (
              <Text className="text-ink-soft text-xs">Gợi ý: {answer.aiSuggestions}</Text>
            )}
          </div>
        )}

        {answer.gradedBy === 'AI_FAILED' && (
          <Alert
            type="info"
            showIcon
            message="AI không chấm được câu này"
            description="Thường là do hết hạn mức của mô hình miễn phí. Bài làm vẫn nguyên, chỉ cần bạn cho điểm."
          />
        )}

        <div className="flex flex-wrap items-end gap-3 border-t border-line pt-3">
          <div>
            <Text className="mb-1 block text-xs">Điểm (0–{answer.maxScore})</Text>
            <InputNumber
              min={0}
              max={answer.maxScore}
              value={score}
              onChange={setScore}
              placeholder="—"
            />
          </div>

          <div className="min-w-60 flex-1">
            <Text className="mb-1 block text-xs">Nhận xét cho người học</Text>
            <Input.TextArea
              rows={2}
              value={feedback}
              onChange={(event) => setFeedback(event.target.value)}
              placeholder="Nói rõ được ý nào, thiếu ý nào"
            />
          </div>

          <Button
            type="primary"
            loading={grade.isPending}
            disabled={score === null}
            onClick={() =>
              grade.mutate({
                answerId: answer.answerId,
                score: score!,
                feedback: feedback.trim() || undefined,
              })
            }
          >
            Lưu điểm
          </Button>
        </div>
      </Space>
    </Card>
  )
}
