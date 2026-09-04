import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Skeleton, Tag, Typography, message } from 'antd'
import { getApiErrorMessage } from '@/shared/api/client'
import { boMatCua } from '@/features/quiz/coverGradient'
import { recommendApi, type RecommendationSource } from '../api/recommendApi'
import { useRecommendedQuizzes } from '../hooks/useRecommendQueries'

const { Title, Text } = Typography

/** Ba nguồn gợi ý, mỗi nguồn một nhãn — người học biết vì sao quiz này xuất hiện. */
const SOURCE_LABEL: Record<RecommendationSource, string> = {
  WEAK_TOPIC: 'Ôn chỗ đang yếu',
  SIMILAR_LEARNERS: 'Người giống bạn đã làm',
  NEW_TOPIC: 'Chủ đề mới',
}

const SOURCE_COLOR: Record<RecommendationSource, string> = {
  WEAK_TOPIC: 'orange',
  SIMILAR_LEARNERS: 'blue',
  NEW_TOPIC: 'green',
}

/**
 * Khu "Gợi ý cho bạn" trên trang Khám phá (FR-34).
 * <p>
 * **Rỗng thì nói vì sao rỗng, không im lặng biến mất.** Bản đầu ẩn hẳn khu này khi không có gợi ý,
 * với lý do "người mới thấy ô trống thì tưởng hệ thống hỏng" — nhưng ẩn đi lại tạo ra đúng nỗi nghi
 * đó theo đường khác: người dùng biết tính năng gợi ý tồn tại, không thấy nó, và kết luận là hỏng.
 * Thực tế đã hiểu nhầm như vậy hai lần.
 * <p>
 * Câu giải thích do **backend** viết: chỉ nó biết đang là tình huống nào trong ba (kho chưa có quiz,
 * đã làm hết quiz đang có, hay không truy vấn được đồ thị) — và ba tình huống đó dẫn tới ba việc
 * người dùng nên làm khác nhau.
 * <p>
 * Vẫn ẩn hẳn khi <b>lỗi mạng/401</b>: lúc đó không có cả `note`, mà đoán hộ backend thì dễ nói sai.
 */
export default function RecommendedQuizzes() {
  const { data, isPending } = useRecommendedQuizzes(4)

  // Hook phải nằm TRƯỚC mọi `return` sớm: component này thoát sớm ở ba nhánh (đang tải / không có dữ
  // liệu / danh sách rỗng), và đặt useState sau một trong số đó là vi phạm luật hook — React sẽ đổ.
  const [loiGiai, setLoiGiai] = useState<Record<string, string>>({})
  const [dangHoi, setDangHoi] = useState<string | null>(null)

  /**
   * Hỏi AI vì sao quiz này được gợi ý (FR-36).
   *
   * Thay lý do mẫu bằng lời giải thích, và **ẩn luôn nút** sau khi hỏi: hỏi lại cùng một quiz không cho
   * thêm thông tin gì (backend cache 24 giờ và trả về đúng chuỗi cũ), nên để nút lại chỉ mời bấm vô ích.
   */
  const hoiViSao = async (quizId: string) => {
    setDangHoi(quizId)
    try {
      const giaiThich = await recommendApi.explain(quizId)
      setLoiGiai((prev) => ({ ...prev, [quizId]: giaiThich }))
    } catch (error) {
      // Hết hạn mức AI (429) cũng rơi vào đây — thông báo của backend nói rõ còn bao nhiêu lượt
      message.error(getApiErrorMessage(error))
    } finally {
      setDangHoi(null)
    }
  }

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 2 }} />
  }
  if (!data) {
    return null
  }

  if (data.items.length === 0) {
  return (
      <section className="soft-panel p-5">
        <div className="mb-2 flex flex-wrap items-center gap-3">
          <Title level={4} className="mb-0!">
            Gợi ý cho bạn
          </Title>
          <Link to="/learning-path" className="ml-auto text-sm font-bold underline">
            Xem lộ trình học
          </Link>
        </div>
        <Text className="text-ink-soft text-sm">{data.note}</Text>
      </section>
    )
  }

  return (
    <section className="soft-panel p-5">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Title level={4} className="mb-0!">
          Gợi ý cho bạn
        </Title>
        <Text className="text-ink-soft text-xs">Dựa trên những gì bạn đã làm</Text>
        <Link to="/learning-path" className="ml-auto text-sm font-bold underline">
          Xem lộ trình học
        </Link>
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {data.items.map((item) => (
          // Dùng CHUNG lớp `browse-card` với thẻ ở trang Khám phá. Trước đây chỗ này tự vẽ
          // `border border-line` nên thẻ gợi ý là thứ duy nhất bấm được mà không có hover —
          // cùng một thành phần, hai hành vi, và người dùng nhận ra ngay.
          <div key={item.quizId} className="browse-card flex flex-col overflow-hidden">
            {/*
              Ảnh bìa 16:9, cùng khuôn với thẻ ở lưới Khám phá — một quiz phải trông như chính nó ở
              mọi chỗ nó xuất hiện. Chưa có ảnh thì vẽ khối màu bằng `coverOf` dùng chung, nên cùng
              quiz ra cùng màu ở cả hai trang.
            */}
            {item.thumbnailUrl ? (
              <img
                src={item.thumbnailUrl}
                alt=""
                loading="lazy"
                className="aspect-video w-full object-cover"
              />
            ) : (
              <div
                className="flex aspect-video w-full items-center justify-center"
                style={{ background: boMatCua(item.categoryName, item.title).nen }}
              >
                <span aria-hidden className="select-none text-4xl opacity-90">
                  {boMatCua(item.categoryName, item.title).icon}
                </span>
              </div>
            )}

            <div className="flex flex-1 flex-col p-4">
              <Tag color={SOURCE_COLOR[item.source]} className="mr-0! mb-2 self-start">
                {SOURCE_LABEL[item.source]}
              </Tag>

              <Text className="line-clamp-2-title mb-1 font-bold">{item.title}</Text>

              {/* Lý do do backend viết — gợi ý không nói vì sao thì người dùng không có căn cứ để tin.
                  LUÔN có sẵn và không tốn gì; lời giải thích của AI chỉ là bản nói kỹ hơn khi được hỏi */}
              <Text className="mb-2 text-ink-soft text-xs">
                {loiGiai[item.quizId] ?? item.reason}
              </Text>

              {!loiGiai[item.quizId] && (
                <Button
                  type="link"
                  size="small"
                  loading={dangHoi === item.quizId}
                  className="mb-2 h-auto! self-start p-0!"
                  onClick={() => void hoiViSao(item.quizId)}
                >
                  Vì sao gợi ý này?
                </Button>
              )}

              <Link to={`/quizzes/${item.quizId}`} className="mt-auto">
                <Button size="small" block>
                  Làm thử
                </Button>
              </Link>
            </div>
          </div>
        ))}
      </div>
    </section>
  )
}
