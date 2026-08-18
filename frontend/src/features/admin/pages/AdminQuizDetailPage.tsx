import { Link, useParams } from 'react-router-dom'
import { Alert, Card, Descriptions, Skeleton, Tag, Typography } from 'antd'
import { ArrowLeftOutlined, CheckCircleFilled } from '@ant-design/icons'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import { useQuizDetail } from '@/features/quiz/hooks/useQuizQueries'

const { Text, Paragraph } = Typography

/**
 * Xem nội dung một quiz, **trong khu quản trị** (features/10).
 *
 * ## Vì sao có trang này thay vì dẫn sang khu học tập
 * Trang kiểm duyệt trước đây dẫn thẳng sang `/quizzes/{id}` — nhưng từ khi quản trị viên bị chặn khỏi khu học
 * tập, đường đó thành một cú đá ngược về `/admin`. Bỏ hẳn liên kết thì tệ hơn nữa: admin phải quyết định **ẩn
 * nội dung của người khác mà không nhìn được nội dung đó**, đúng kiểu quyết định mù mà cả tính năng chống gian
 * lận đang cố tránh.
 *
 * ## Chỉ đọc, và cố ý không có nút nào
 * Không sửa, không xoá, không ẩn ở đây. Hành động kiểm duyệt nằm ở trang danh sách — nơi có ngữ cảnh đầy đủ
 * (chủ sở hữu, số lượt, trạng thái). Trang này trả lời đúng một câu hỏi: *"trong quiz này có gì?"*
 */
export default function AdminQuizDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { data, isPending, isError } = useQuizDetail(id)

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }

  if (isError || !data) {
    return (
      <EmptyState
        title="Không mở được quiz này"
        hint="Quiz không tồn tại hoặc đã bị xoá"
        action={<Link to="/admin/quizzes">Về danh sách kiểm duyệt</Link>}
      />
    )
  }

  const { quiz, questions } = data

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title={quiz.title}
        description={
          <Link to="/admin/quizzes">
            <ArrowLeftOutlined /> Về danh sách kiểm duyệt
          </Link>
        }
      />

      {/* Nhắc ngay đầu trang: đây là nội dung của người khác, không phải của mình */}
      <Alert
        type="info"
        showIcon
        message="Trang chỉ đọc"
        description="Bạn đang xem nội dung do người dùng khác soạn, để có căn cứ trước khi kiểm duyệt. Mọi thao tác ẩn/hiện nằm ở trang danh sách."
      />

      <Descriptions bordered size="small" column={2}>
        <Descriptions.Item label="Chủ sở hữu">{quiz.ownerDisplayName}</Descriptions.Item>
        <Descriptions.Item label="Danh mục">{quiz.categoryName ?? '—'}</Descriptions.Item>
        <Descriptions.Item label="Độ khó">{quiz.difficulty}</Descriptions.Item>
        <Descriptions.Item label="Hiển thị">{quiz.visibility}</Descriptions.Item>
        <Descriptions.Item label="Số câu">{questions.length}</Descriptions.Item>
        <Descriptions.Item label="Tạo lúc">
          {quiz.createdAt ? new Date(quiz.createdAt).toLocaleString('vi-VN') : '—'}
        </Descriptions.Item>
        {quiz.description && (
          <Descriptions.Item label="Mô tả" span={2}>
            {quiz.description}
          </Descriptions.Item>
        )}
      </Descriptions>

      {questions.length === 0 ? (
        <EmptyState
          title="Quiz này chưa có câu hỏi nào"
          hint="Quiz rỗng thì không ai làm được — thường là bản nháp người soạn chưa hoàn thành"
        />
      ) : (
        <div className="flex flex-col gap-3">
          {questions.map((cau, i) => (
            <Card key={cau.id} size="small">
              <div className="mb-2 flex flex-wrap items-center gap-2">
                <Text className="font-bold!">Câu {i + 1}</Text>
                <Tag className="mr-0!">{cau.type}</Tag>
                {cau.topic && <Tag color="blue" className="mr-0!">{cau.topic}</Tag>}
                <Text className="text-ink-soft text-xs">{cau.points} điểm</Text>
              </div>

              <Paragraph className="mb-2!">{cau.content}</Paragraph>

              {/* Hiện cả đáp án đúng: người kiểm duyệt cần thấy đề SAI ở đâu, mà đề sai thường nằm ở
                  đáp án chứ không ở câu hỏi. Endpoint này vốn chỉ trả cho chủ sở hữu hoặc Admin. */}
              <div className="flex flex-col gap-1">
                {cau.options.map((pa) => (
                  <div key={pa.id} className="flex items-start gap-2">
                    <span className="w-4 shrink-0 pt-0.5">
                      {pa.correct && <CheckCircleFilled className="text-success text-xs" />}
                    </span>
                    <Text className={`text-sm ${pa.correct ? 'font-bold!' : 'text-ink-soft'}`}>
                      {pa.content}
                    </Text>
                  </div>
                ))}
              </div>

              {cau.explanation && (
                <div className="border-line mt-2 border-t pt-2">
                  <Text className="text-ink-soft text-xs">Giải thích: {cau.explanation}</Text>
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  )
}
