import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Button, Card, Progress, Skeleton, Tag, Typography } from 'antd'
import { BulbOutlined } from '@ant-design/icons'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import { useDueCards, useReviewCard } from '../hooks/useFlashcards'
import type { ReviewQuality } from '../api/flashcardApi'

const { Text, Title, Paragraph } = Typography

/**
 * Phiên ôn tập (features/11, FR-41).
 *
 * Luồng: xem mặt trước → tự nhớ → lật thẻ → tự đánh giá mức nhớ. Bốn nút đánh giá **chỉ hiện sau khi lật**,
 * vì đánh giá trước lúc thấy đáp án thì người học không có gì để đối chiếu và con số đưa vào thuật toán trở
 * thành nhiễu.
 *
 * Danh sách thẻ lấy **một lần** rồi đi hết trong bộ nhớ (`staleTime: Infinity` ở hook). Nạp lại sau mỗi lần
 * đánh giá thì thẻ vừa ôn biến khỏi danh sách giữa lúc React đang render và cả phiên ôn nhảy vị trí.
 */
export default function ReviewSessionPage() {
  const [searchParams] = useSearchParams()
  const deckId = searchParams.get('deckId') ?? undefined

  const { data: cards, isLoading } = useDueCards(deckId)
  const review = useReviewCard()

  const [viTri, setViTri] = useState(0)
  const [daLat, setDaLat] = useState(false)
  const [hienGoiY, setHienGoiY] = useState(false)

  const tong = cards?.length ?? 0
  const the = cards?.[viTri]

  const danhGia = async (quality: ReviewQuality) => {
    if (!the) return
    await review.mutateAsync({ id: the.id, quality })
    setDaLat(false)
    setHienGoiY(false)
    setViTri((truoc) => truoc + 1)
  }

  if (isLoading) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }

  if (tong === 0) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader title="Ôn thẻ" />
        <EmptyState
          title="Không có thẻ nào đến hạn"
          hint="Lịch ôn đã giãn ra — quay lại khi có thẻ tới hạn. Muốn ôn thêm thì tạo thẻ mới."
          action={
            <Link to="/flashcards">
              <Button type="primary">Về danh sách bộ thẻ</Button>
            </Link>
          }
        />
      </div>
    )
  }

  // Đã đi hết danh sách
  if (!the) {
    return (
      <div className="flex flex-col gap-6">
        <PageHeader title="Xong phiên ôn" />
        <EmptyState
          title={`Đã ôn xong ${tong} thẻ`}
          hint="Thẻ nhớ tốt sẽ quay lại muộn hơn, thẻ chưa nhớ quay lại ngày mai."
          action={
            <Link to="/flashcards">
              <Button type="primary">Về danh sách bộ thẻ</Button>
            </Link>
          }
        />
      </div>
    )
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-col gap-4">
      <div className="flex items-center justify-between">
        <Text className="text-ink-soft text-sm">
          Thẻ {viTri + 1} / {tong}
        </Text>
        <Link to="/flashcards" className="text-sm">
          Dừng phiên ôn
        </Link>
      </div>
      <Progress percent={Math.round((viTri / tong) * 100)} showInfo={false} />

      <Card className="min-h-72">
        <div className="flex min-h-56 flex-col items-center justify-center gap-4 text-center">
          <Text className="text-ink-soft text-xs uppercase">Mặt trước</Text>
          <Title level={3} className="mb-0! whitespace-pre-line">
            {the.front}
          </Title>

          {the.hint && !daLat && (
            hienGoiY ? (
              <Text className="text-ink-soft text-sm">Gợi ý: {the.hint}</Text>
            ) : (
              <Button type="link" size="small" icon={<BulbOutlined />} onClick={() => setHienGoiY(true)}>
                Xem gợi ý
              </Button>
            )
          )}

          {daLat && (
            <div className="border-line w-full border-t pt-4">
              <Text className="text-ink-soft text-xs uppercase">Mặt sau</Text>
              <Paragraph className="mt-2 mb-0! text-base whitespace-pre-line">{the.back}</Paragraph>
            </div>
          )}
        </div>
      </Card>

      {!daLat ? (
        <Button type="primary" size="large" block onClick={() => setDaLat(true)}>
          Lật thẻ
        </Button>
      ) : (
        <div className="flex flex-col gap-2">
          <Text className="text-ink-soft text-center text-xs">
            Bạn nhớ thẻ này ở mức nào? Câu trả lời quyết định khi nào gặp lại nó.
          </Text>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {MUC_NHO.map((muc) => (
              <Button
                key={muc.quality}
                size="large"
                danger={muc.quality === 'AGAIN'}
                loading={review.isPending}
                onClick={() => danhGia(muc.quality)}
              >
                <span className="flex flex-col leading-tight">
                  <span className="font-bold">{muc.nhan}</span>
                  <span className="text-ink-soft text-[11px]">{muc.moTa}</span>
                </span>
              </Button>
            ))}
          </div>
        </div>
      )}

      {the.source === 'FROM_WRONG_ANSWER' && (
        <Tag color="volcano" className="mx-auto w-fit">
          Thẻ này sinh từ một câu bạn đã trả lời sai
        </Tag>
      )}
    </div>
  )
}

/**
 * Bốn mức nhớ, kèm mô tả hệ quả thật.
 * <p>
 * Ghi rõ "ôn lại ngày mai" cho hai mức đầu vì đó là điều dễ hiểu sai nhất: nhiều người tưởng "Khó" nghĩa là
 * vẫn nhớ nhưng hơi khó, trong khi ranh giới của SM-2 xếp nó vào bên chưa nhớ.
 */
const MUC_NHO: { quality: ReviewQuality; nhan: string; moTa: string }[] = [
  { quality: 'AGAIN', nhan: 'Không nhớ', moTa: 'ôn lại ngày mai' },
  { quality: 'HARD', nhan: 'Khó', moTa: 'ôn lại ngày mai' },
  { quality: 'GOOD', nhan: 'Nhớ được', moTa: 'giãn lịch' },
  { quality: 'EASY', nhan: 'Dễ', moTa: 'giãn lịch nhiều' },
]
