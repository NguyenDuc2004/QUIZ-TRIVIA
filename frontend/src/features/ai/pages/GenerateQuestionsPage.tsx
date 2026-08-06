import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Alert,
  Button,
  Checkbox,
  Form,
  Input,
  InputNumber,
  Radio,
  Select,
  Space,
  Spin,
  Tag,
  Typography,
} from 'antd'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import type { Difficulty, QuestionType } from '@/features/quiz/api/quizApi'
import {
  DIFFICULTY_LABEL,
  DIFFICULTY_OPTIONS,
  QUESTION_TYPE_LABEL,
  QUESTION_TYPE_OPTIONS,
} from '@/features/quiz/constants'
import type { GeneratedQuestion } from '../api/aiApi'
import {
  useAiJob,
  useAiStatus,
  useApproveQuestions,
  useGenerateQuestions,
  useMaterials,
} from '../hooks/useAiQueries'

const { Text, Paragraph } = Typography

/**
 * Sinh đề bằng AI rồi để Creator duyệt (FR-29, human-in-the-loop).
 * <p>
 * Câu hỏi sinh ra <b>chưa</b> vào ngân hàng: người dùng tích chọn câu nào dùng được rồi mới lưu.
 */
export default function GenerateQuestionsPage() {
  const { data: aiStatus } = useAiStatus()
  const { data: materials } = useMaterials({ size: 50 })

  const [topic, setTopic] = useState('')
  const [count, setCount] = useState(5)
  const [types, setTypes] = useState<QuestionType[]>(['SINGLE_CHOICE'])
  const [difficulty, setDifficulty] = useState<Difficulty>('MEDIUM')
  const [groundInMaterials, setGroundInMaterials] = useState(false)
  const [materialId, setMaterialId] = useState<string | undefined>()
  const [jobId, setJobId] = useState<string | undefined>()
  const [selected, setSelected] = useState<number[]>([])

  const generate = useGenerateQuestions()
  const approve = useApproveQuestions()
  const { data: job } = useAiJob(jobId)

  const readyMaterials = useMemo(
    () => (materials?.content ?? []).filter((m) => m.status === 'READY'),
    [materials],
  )

  const running = job?.status === 'PENDING' || job?.status === 'RUNNING'
  const questions = job?.status === 'SUCCEEDED' ? (job.result?.questions ?? []) : []

  const submit = async () => {
    setSelected([])
    const created = await generate.mutateAsync({
      topic: topic.trim() || undefined,
      count,
      types,
      difficulty,
      materialId: groundInMaterials ? materialId : undefined,
      useMaterials: groundInMaterials,
    })
    setJobId(created.id)
  }

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Sinh đề bằng AI"
        description="AI soạn câu hỏi nháp; bạn xem lại và chọn câu nào đáng giữ mới lưu vào ngân hàng."
        actions={
          <Link to="/ai/materials">
            <Button>Kho học liệu</Button>
          </Link>
        }
      />

      {aiStatus && !aiStatus.available && (
        <Alert
          type="warning"
          showIcon
          message="Chưa cấu hình API key cho dịch vụ AI"
          description="Thêm GEMINI_API_KEY vào file .env rồi khởi động lại backend."
        />
      )}

      <div className="grid gap-6 lg:grid-cols-[340px_1fr]">
        <aside className="h-fit border border-line bg-white p-5 lg:sticky lg:top-24">
          <Form layout="vertical">
            <Form.Item label="Chủ đề" help="Mô tả càng cụ thể, câu hỏi càng đúng trọng tâm">
              <Input
                value={topic}
                onChange={(e) => setTopic(e.target.value)}
                placeholder="Ví dụ: mã trạng thái HTTP"
              />
            </Form.Item>

            <Form.Item label="Số câu">
              <InputNumber
                min={1}
                max={20}
                className="w-full"
                value={count}
                onChange={(value) => setCount(value ?? 5)}
              />
            </Form.Item>

            <Form.Item label="Loại câu hỏi">
              <Checkbox.Group
                value={types}
                onChange={(values) => setTypes(values as QuestionType[])}
                options={QUESTION_TYPE_OPTIONS}
                className="flex flex-col gap-1"
              />
            </Form.Item>

            <Form.Item label="Độ khó">
              <Radio.Group
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value)}
                optionType="button"
                options={DIFFICULTY_OPTIONS}
              />
            </Form.Item>

            <Form.Item label="Nguồn kiến thức">
              <Radio.Group
                value={groundInMaterials}
                onChange={(e) => setGroundInMaterials(e.target.value)}
                className="w-full"
              >
                <Space direction="vertical" size={4} className="w-full">
                  <Radio value={false}>Kiến thức chung</Radio>
                  <Radio value={true} disabled={readyMaterials.length === 0}>
                    Bám theo học liệu (RAG)
                  </Radio>
                </Space>
              </Radio.Group>
              {readyMaterials.length === 0 && (
                <Text className="mt-2 block text-ink-soft text-xs">
                  Chưa có học liệu nào sẵn sàng. <Link to="/ai/materials">Nạp học liệu</Link> để dùng RAG.
                </Text>
              )}
            </Form.Item>

            {groundInMaterials && (
              <Form.Item label="Tài liệu" help="Để trống thì tìm trong tất cả học liệu của bạn">
                <Select
                  allowClear
                  className="w-full"
                  placeholder="Tất cả học liệu"
                  value={materialId}
                  onChange={setMaterialId}
                  options={readyMaterials.map((m) => ({ value: m.id, label: m.title }))}
                />
              </Form.Item>
            )}

            <Button
              type="primary"
              block
              loading={generate.isPending || running}
              disabled={types.length === 0}
              onClick={submit}
            >
              {running ? 'AI đang soạn…' : 'Sinh câu hỏi'}
            </Button>
          </Form>
        </aside>

        <div className="flex flex-col gap-4">
          {!jobId && (
            <div className="border border-line bg-white">
              <EmptyState
                title="Chưa có kết quả"
                hint="Chọn cấu hình bên trái rồi bấm Sinh câu hỏi. Mỗi lần mất khoảng 10–30 giây."
              />
            </div>
          )}

          {running && (
            <div className="flex flex-col items-center gap-3 border border-line bg-white p-10">
              <Spin size="large" />
              <Text className="text-ink-soft">AI đang soạn câu hỏi, thường mất 10–30 giây…</Text>
            </div>
          )}

          {job?.status === 'FAILED' && (
            <Alert type="error" showIcon message="Sinh đề thất bại" description={job.errorMessage} />
          )}

          {job?.status === 'SUCCEEDED' && job.result && (
            <>
              <div className="flex flex-wrap items-center gap-3 border border-line bg-white p-4">
                <Text className="font-bold!">{questions.length} câu hỏi nháp</Text>
                <Tag className="mr-0!">{job.result.provider}</Tag>
                <Text className="text-ink-soft text-xs">
                  {job.result.model} · {(job.result.latencyMs / 1000).toFixed(1)}s
                  {job.result.sourceExcerpts.length > 0 &&
                    ` · bám theo ${job.result.sourceExcerpts.length} đoạn học liệu`}
                </Text>

                <Space className="ml-auto">
                  <Button
                    onClick={() =>
                      setSelected(selected.length === questions.length ? [] : questions.map((_, i) => i))
                    }
                  >
                    {selected.length === questions.length ? 'Bỏ chọn hết' : 'Chọn hết'}
                  </Button>
                  <Button
                    type="primary"
                    disabled={selected.length === 0}
                    loading={approve.isPending}
                    onClick={() => jobId && approve.mutate({ jobId, indexes: selected })}
                  >
                    Lưu {selected.length > 0 ? `${selected.length} câu` : ''} vào ngân hàng
                  </Button>
                </Space>
              </div>

              {job.result.rejected.length > 0 && (
                <Alert
                  type="info"
                  showIcon
                  message={`${job.result.rejected.length} câu bị loại vì không đúng luật`}
                  description={
                    <ul className="m-0 pl-4">
                      {job.result.rejected.map((reason) => (
                        <li key={reason} className="text-xs">
                          {reason}
                        </li>
                      ))}
                    </ul>
                  }
                />
              )}

              {questions.map((question, index) => (
                <DraftQuestionCard
                  key={index}
                  index={index}
                  question={question}
                  checked={selected.includes(index)}
                  onToggle={() =>
                    setSelected((prev) =>
                      prev.includes(index) ? prev.filter((i) => i !== index) : [...prev, index],
                    )
                  }
                />
              ))}
            </>
          )}
        </div>
      </div>
    </Space>
  )
}

/** Một câu hỏi nháp kèm ô tích chọn — đáp án đúng hiện rõ để người duyệt kiểm nhanh. */
function DraftQuestionCard({
  index,
  question,
  checked,
  onToggle,
}: {
  index: number
  question: GeneratedQuestion
  checked: boolean
  onToggle: () => void
}) {
  return (
    <div className={`border bg-white p-5 ${checked ? 'border-ink' : 'border-line'}`}>
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <Checkbox checked={checked} onChange={onToggle} />
        <Text className="text-ink-soft text-xs font-bold">Câu {index + 1}</Text>
        <Tag className="mr-0!">{QUESTION_TYPE_LABEL[question.type]}</Tag>
        <Tag className="mr-0!">{DIFFICULTY_LABEL[question.difficulty]}</Tag>
        {question.topic && <Text className="text-ink-soft text-xs">{question.topic}</Text>}
      </div>

      <Paragraph className="mb-3! font-bold!">{question.content}</Paragraph>

      <div className="flex flex-col gap-2">
        {question.options.map((option, i) => (
          <div
            key={i}
            className={`border p-2 text-sm ${
              option.correct ? 'border-green-500 bg-green-50' : 'border-line'
            }`}
          >
            {option.content}
            {option.correct && (
              <Tag color="green" className="ml-2 mr-0!">
                Đáp án đúng
              </Tag>
            )}
          </div>
        ))}
      </div>

      {question.explanation && (
        <div className="mt-3 border-l-2 border-brand bg-surface-subtle p-3">
          <Text className="text-ink-soft text-xs font-bold">Giải thích</Text>
          <Paragraph className="mb-0! whitespace-pre-wrap">{question.explanation}</Paragraph>
        </div>
      )}
    </div>
  )
}
