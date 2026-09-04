import { useState } from 'react'
import {
  Alert,
  Button,
  Checkbox,
  Collapse,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Spin,
  Tag,
  Typography,
} from 'antd'
import { useAskableMaterials } from '@/features/chat/hooks/useChat'
import { useApproveGenerated, useFlashcardJob, useGenerateCards } from '../hooks/useFlashcards'

const { Text, Paragraph } = Typography

/**
 * Sinh thẻ ghi nhớ từ học liệu bằng AI (features/11, FR-38).
 *
 * Hai bước, và bước hai là bắt buộc:
 * 1. Chọn học liệu nguồn + chủ đề + số thẻ → gửi job nền.
 * 2. **Duyệt từng thẻ** rồi mới lưu.
 *
 * Vì sao không lưu thẳng: một thẻ sai lọt vào bộ sẽ được ôn đi ôn lại theo lịch SRS — tức được *học thuộc*,
 * chứ không chỉ được đọc qua một lần như một câu hỏi trong đề. Người duy nhất phát hiện được nội dung sai là
 * người đọc tài liệu gốc, nên màn duyệt hiện kèm các đoạn học liệu đã dùng để đối chiếu.
 *
 * Danh sách học liệu dùng lại {@code useAskableMaterials} của trợ lý học tập — cùng một câu hỏi ("tài liệu
 * nào tôi được dùng"), nên cùng một câu trả lời: tài liệu của mình cộng tài liệu đã được chia sẻ.
 */
export default function AiGenerateCardsModal({
  deckId,
  open,
  onClose,
}: {
  deckId: string
  open: boolean
  onClose: () => void
}) {
  const [form] = Form.useForm<{ materialId: string; topic?: string; count: number }>()
  const [jobId, setJobId] = useState<string | undefined>()
  const [daChon, setDaChon] = useState<number[]>([])

  const { data: materials, isLoading: dangTaiHocLieu } = useAskableMaterials()
  const generate = useGenerateCards()
  const { data: job } = useFlashcardJob(jobId)
  const approve = useApproveGenerated()

  const dangChay = jobId != null && (job == null || job.status === 'PENDING' || job.status === 'RUNNING')
  const theNhap = job?.status === 'SUCCEEDED' ? (job.result?.flashcards ?? []) : []

  const dong = () => {
    setJobId(undefined)
    setDaChon([])
    form.resetFields()
    onClose()
  }

  const gui = async () => {
    const values = await form.validateFields()
    const ketQua = await generate.mutateAsync({ deckId, body: values })
    setJobId(ketQua.id)
  }

  const luu = async () => {
    if (!jobId || daChon.length === 0) return
    await approve.mutateAsync({ jobId, indexes: daChon })
    dong()
  }

  return (
    <Modal
      open={open}
      title="Sinh thẻ từ học liệu bằng AI"
      width={760}
      onCancel={dong}
      maskClosable={!dangChay}
      footer={
        theNhap.length > 0
          ? [
              <Button key="huy" onClick={dong}>
                Bỏ, không lưu
              </Button>,
              <Button
                key="luu"
                type="primary"
                disabled={daChon.length === 0}
                loading={approve.isPending}
                onClick={luu}
              >
                Lưu {daChon.length} thẻ đã chọn
              </Button>,
            ]
          : [
              <Button key="huy" onClick={dong}>
                Huỷ
              </Button>,
              <Button
                key="sinh"
                type="primary"
                loading={generate.isPending || dangChay}
                disabled={job?.status === 'FAILED'}
                onClick={gui}
              >
                Sinh thẻ
              </Button>,
            ]
      }
    >
      {/* --- Bước 1: chọn nguồn --- */}
      {theNhap.length === 0 && (
        <>
          <Alert
            type="info"
            showIcon
            className="mb-4"
            message="Bắt buộc chọn một học liệu"
            description="Thẻ ghi nhớ được ôn lại hàng chục lần theo lịch, nên một thẻ sai sẽ bị học thuộc. Có tài liệu nguồn thì bạn đối chiếu được trước khi lưu."
          />

          <Form form={form} layout="vertical" requiredMark={false} initialValues={{ count: 10 }}>
            <Form.Item
              name="materialId"
              label="Học liệu nguồn"
              rules={[{ required: true, message: 'Chọn một học liệu' }]}
            >
              <Select
                loading={dangTaiHocLieu}
                placeholder="Chọn tài liệu đã xử lý xong"
                notFoundContent={
                  dangTaiHocLieu ? (
                    <Spin size="small" />
                  ) : (
                    <Empty
                      image={null}
                      description="Chưa có học liệu nào sẵn sàng. Tài liệu của bạn, hoặc tài liệu người khác đã chia sẻ, phải ở trạng thái xử lý xong."
                    />
                  )
                }
                options={materials?.map((m) => ({
                  value: m.id,
                  label: (
                    <span className="flex items-center gap-2">
                      {m.title}
                      {!m.mine && <Tag className="mr-0!">được chia sẻ</Tag>}
                      <Text className="text-ink-soft text-xs">{m.chunkCount} đoạn</Text>
                    </span>
                  ),
                }))}
              />
            </Form.Item>

            <Form.Item
              name="topic"
              label="Chủ đề"
              extra="Để trống thì AI lấy nội dung chính của tài liệu."
            >
              <Input placeholder="Ví dụ: mã trạng thái HTTP" maxLength={200} />
            </Form.Item>

            <Form.Item name="count" label="Số thẻ muốn sinh">
              <InputNumber min={1} max={30} className="w-40!" />
            </Form.Item>
          </Form>

          {dangChay && (
            <Alert
              type="info"
              showIcon
              icon={<Spin size="small" />}
              message="Đang sinh thẻ…"
              description={
                (job?.aiThrottledSeconds ?? 0) > 0
                  ? `Nhà cung cấp AI đang chặn hạn mức, còn khoảng ${job?.aiThrottledSeconds} giây. Hệ thống tự chờ, bạn không cần bấm lại.`
                  : 'Việc này thường mất 10–30 giây. Bạn có thể để mở cửa sổ này.'
              }
            />
          )}

          {job?.status === 'FAILED' && (
            <Alert
              type="error"
              showIcon
              message="Sinh thẻ không thành công"
              description={job.errorMessage ?? 'Thử lại sau.'}
            />
          )}
        </>
      )}

      {/* --- Bước 2: duyệt thẻ nháp --- */}
      {theNhap.length > 0 && (
        <div className="flex flex-col gap-3">
          <Alert
            type="warning"
            showIcon
            message={`AI tạo ${theNhap.length} thẻ — hãy đọc rồi chọn thẻ muốn giữ`}
            description="Thẻ chưa được lưu. Đối chiếu với đoạn học liệu ở cuối nếu có thẻ đáng ngờ."
          />

          <div className="flex items-center gap-3">
            <Checkbox
              indeterminate={daChon.length > 0 && daChon.length < theNhap.length}
              checked={daChon.length === theNhap.length}
              onChange={(e) =>
                setDaChon(e.target.checked ? theNhap.map((_, i) => i) : [])
              }
            >
              Chọn tất cả
            </Checkbox>
            <Text className="text-ink-soft text-xs">
              {job?.result?.provider} · {job?.result?.latencyMs} ms
            </Text>
          </div>

          <div className="flex max-h-80 flex-col gap-2 overflow-y-auto">
            {theNhap.map((the, i) => (
              <label
                key={i}
                className={`border-line flex cursor-pointer gap-3 rounded-control border p-3 ${
                  daChon.includes(i) ? 'bg-surface-subtle' : ''
                }`}
              >
                <Checkbox
                  checked={daChon.includes(i)}
                  onChange={(e) =>
                    setDaChon((truoc) =>
                      e.target.checked ? [...truoc, i] : truoc.filter((x) => x !== i),
                    )
                  }
                />
                <div className="min-w-0 flex-1">
                  <Text className="font-bold!">{the.front}</Text>
                  <Paragraph className="text-ink-soft mb-0! text-sm whitespace-pre-line">
                    {the.back}
                  </Paragraph>
                  {the.hint && (
                    <Text className="text-ink-soft text-xs">Gợi ý: {the.hint}</Text>
                  )}
                </div>
              </label>
            ))}
          </div>

          {/* Thẻ bị loại: nói rõ vì sao yêu cầu 15 mà chỉ nhận 11, thay vì im lặng */}
          {(job?.result?.rejected?.length ?? 0) > 0 && (
            <Collapse
              size="small"
              items={[
                {
                  key: 'loai',
                  label: `${job?.result?.rejected.length} thẻ bị loại tự động`,
                  children: (
                    <ul className="text-ink-soft m-0 pl-4 text-xs">
                      {job?.result?.rejected.map((ly, i) => (
                        <li key={i}>{ly}</li>
                      ))}
                    </ul>
                  ),
                },
              ]}
            />
          )}

          <Collapse
            size="small"
            items={[
              {
                key: 'nguon',
                label: `Đoạn học liệu AI đã dùng (${job?.result?.sourceExcerpts.length ?? 0})`,
                children: (
                  <div className="flex flex-col gap-2">
                    {job?.result?.sourceExcerpts.map((doan, i) => (
                      <Paragraph
                        key={i}
                        className="text-ink-soft border-line mb-0! border-l-2 pl-3 text-xs"
                        ellipsis={{ rows: 4, expandable: true, symbol: 'xem thêm' }}
                      >
                        {doan}
                      </Paragraph>
                    ))}
                  </div>
                ),
              },
            ]}
          />
        </div>
      )}
    </Modal>
  )
}
