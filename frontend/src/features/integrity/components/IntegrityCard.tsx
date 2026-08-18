import { useState } from 'react'
import { Alert, Button, Card, Collapse, Input, Progress, Space, Tag, Typography } from 'antd'
import { CheckOutlined, CloseOutlined, RobotOutlined } from '@ant-design/icons'
import { useReviewIntegrity } from '../hooks/useIntegrity'
import type { IntegrityReport } from '../api/integrityApi'

const { Text, Paragraph } = Typography

/**
 * Thẻ báo cáo tính toàn vẹn cho chủ quiz hoặc Admin (features/12, FR-47).
 *
 * ## Điểm rủi ro luôn đi kèm ba thứ
 * 1. **Cờ nói lý do cụ thể** — một con số 70 mà không nói vì sao thì không dùng được vào việc gì.
 * 2. **Câu nhắc** rằng tín hiệu giả mạo được và đây không phải bằng chứng. Server luôn trả câu này; giao diện
 *    hiện nó **cạnh điểm số**, không giấu xuống cuối trang.
 * 3. **Hai nút kết luận** — vì hệ thống không tự kết luận, việc đó chờ người thật.
 *
 * Cố ý **không** tô đỏ toàn thẻ khi điểm cao: màu đỏ đọc thành "đã kết luận có tội", trong khi trạng thái
 * thật là "đáng xem".
 */
export default function IntegrityCard({ report }: { report: IntegrityReport }) {
  const review = useReviewIntegrity()
  const [note, setNote] = useState('')

  const daKetLuan = report.reviewStatus !== 'PENDING'

  return (
    <Card
      title="Tính toàn vẹn bài thi"
      extra={
        daKetLuan ? (
          <Tag color={report.reviewStatus === 'VALID' ? 'green' : 'volcano'}>
            {report.reviewStatus === 'VALID' ? 'Đã xác nhận hợp lệ' : 'Đã đánh dấu không hợp lệ'}
          </Tag>
        ) : (
          <Tag>Chờ rà soát</Tag>
        )
      }
    >
      <Space direction="vertical" size="middle" className="w-full">
        <div>
          <div className="flex items-baseline justify-between">
            <Text className="text-sm font-bold">Điểm rủi ro</Text>
            <Text className="text-lg font-extrabold">{report.riskScore}/100</Text>
          </div>
          <Progress
            percent={report.riskScore}
            showInfo={false}
            /* Chỉ đổi màu ở mức vượt ngưỡng, và dùng cam chứ không đỏ: đỏ đọc thành "có tội" */
            strokeColor={report.biGanCo ? 'var(--color-star)' : undefined}
          />
          <Text className="text-ink-soft text-xs">
            {report.soSuKien} tín hiệu được ghi nhận
            {report.biGanCo ? ' · vượt ngưỡng đáng rà soát (60)' : ' · dưới ngưỡng đáng rà soát'}
          </Text>
        </div>

        {report.flags.length > 0 ? (
          <div>
            <Text className="mb-1 block text-sm font-bold">Lý do</Text>
            <ul className="mb-0 pl-5 text-sm">
              {report.flags.map((f, i) => (
                <li key={i}>{f}</li>
              ))}
            </ul>
          </div>
        ) : (
          <Text className="text-ink-soft text-sm">Không có tín hiệu nào đáng chú ý.</Text>
        )}

        {report.aiNote && (
          <Alert
            type="info"
            showIcon
            icon={<RobotOutlined />}
            message="Nhận định của AI"
            description={<Paragraph className="mb-0!">{report.aiNote}</Paragraph>}
          />
        )}

        {/* Câu nhắc đặt CẠNH điểm số, không giấu xuống cuối: người đọc phải thấy nó cùng lúc với con số */}
        <Alert type="warning" showIcon message="Đọc con số này thế nào" description={report.canhBao} />

        {daKetLuan ? (
          <div>
            <Text className="text-ink-soft text-xs">
              Kết luận lúc{' '}
              {report.reviewedAt && new Date(report.reviewedAt).toLocaleString('vi-VN')}
            </Text>
            {report.reviewNote && (
              <Paragraph className="mb-0! text-sm">Ghi chú: {report.reviewNote}</Paragraph>
            )}
          </div>
        ) : (
          <div className="flex flex-col gap-2">
            <Input.TextArea
              rows={2}
              maxLength={1000}
              placeholder="Ghi chú lý do (nên có, nhất là khi đánh dấu không hợp lệ)"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
            <div className="flex gap-2">
              <Button
                icon={<CheckOutlined />}
                loading={review.isPending}
                onClick={() =>
                  review.mutate({ attemptId: report.attemptId, status: 'VALID', note: note || undefined })
                }
              >
                Hợp lệ
              </Button>
              <Button
                danger
                icon={<CloseOutlined />}
                loading={review.isPending}
                onClick={() =>
                  review.mutate({ attemptId: report.attemptId, status: 'INVALID', note: note || undefined })
                }
              >
                Không hợp lệ
              </Button>
            </div>
          </div>
        )}

        {report.suKien.length > 0 && (
          <Collapse
            size="small"
            items={[
              {
                key: 'chi-tiet',
                label: `Nhật ký ${report.suKien.length} tín hiệu`,
                children: (
                  <div className="max-h-60 overflow-y-auto">
                    {report.suKien.map((e, i) => (
                      <div key={i} className="border-line flex gap-3 border-b py-1 last:border-0">
                        <Text className="text-ink-soft w-36 shrink-0 text-xs">
                          {new Date(e.occurredAt).toLocaleTimeString('vi-VN')}
                        </Text>
                        <Text className="text-xs">{TEN_TIN_HIEU[e.type] ?? e.type}</Text>
                        {/* detail chỉ chứa số (độ dài, số giây) — không bao giờ có nội dung người dùng */}
                        {e.detail && <Text className="text-ink-soft text-xs">{e.detail}</Text>}
                      </div>
                    ))}
                  </div>
                ),
              },
            ]}
          />
        )}
      </Space>
    </Card>
  )
}

const TEN_TIN_HIEU: Record<string, string> = {
  TAB_HIDDEN: 'Chuyển tab / thu nhỏ cửa sổ',
  WINDOW_BLUR: 'Cửa sổ mất focus',
  COPY: 'Sao chép',
  PASTE: 'Dán',
  FULLSCREEN_EXIT: 'Thoát toàn màn hình',
  ANSWER_TOO_FAST: 'Trả lời nhanh bất thường',
}
