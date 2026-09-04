import { useEffect, useState } from 'react'
import { Alert, Table, Tag, Typography } from 'antd'
import { roomApi, type RoomProctoringSummaryRow } from '../api/roomApi'

const { Text } = Typography

/**
 * Bản tổng kết chống gian lận host xem **sau ván** (features/12, cảnh báo live).
 *
 * ## Vì sao cần bản này khi giữa ván đã có cờ
 * Giữa ván host đang lo điều hành: cờ hiện ra rồi trôi đi. Bảng này là chỗ đọc lại khi đã xong, có thời gian.
 *
 * ## Vì sao liệt kê cả người CHƯA bị gắn cờ
 * Nếu chỉ hiện người vượt ngưỡng thì "không ai làm gì" và "hệ thống không thu được tín hiệu nào" cho ra cùng
 * một bảng trống — hai tình huống rất khác nhau mà host không phân biệt được. Có dòng cho người ở mức thấp
 * thì host biết cơ chế *đang chạy* và những người này *đã được xét*.
 */
export default function RoomProctoringSummary({ roomCode }: { roomCode: string }) {
  const [rows, setRows] = useState<RoomProctoringSummaryRow[] | null>(null)
  const [loi, setLoi] = useState(false)

  useEffect(() => {
    let huy = false
    roomApi
      .proctoring(roomCode)
      .then((data) => {
        if (!huy) setRows(data)
      })
      .catch(() => {
        if (!huy) setLoi(true)
      })
    return () => {
      huy = true
    }
  }, [roomCode])

  // Người không phải host nhận 403 — không hiện gì, kể cả một dòng lỗi. Nói "bạn không có quyền xem" là tiết
  // lộ rằng có một bảng như vậy tồn tại.
  if (loi || rows === null) {
    return null
  }

  if (rows.length === 0) {
    return (
      <div className="border border-line bg-surface p-4">
        <Text className="font-bold! block">Chống gian lận</Text>
        <Text className="text-ink-soft text-sm">
          Không thu được tín hiệu rời trang nào trong ván này.
        </Text>
      </div>
    )
  }

  const soBiGanCo = rows.filter((row) => row.biGanCo).length

  return (
    <div className="border border-line bg-surface p-4">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <Text className="font-bold!">Chống gian lận</Text>
        <Tag className="mr-0!">chỉ bạn thấy bảng này</Tag>
      </div>

      {/* Chỉ hiện khi CÓ người bị gắn cờ.

          Câu nhắc "không phải kết luận" đứng TRƯỚC mọi con số — đặt sau bảng thì host đã đọc xong danh
          sách và đã kết luận rồi mới thấy nó. Nhưng nó chỉ có việc để làm khi bảng đang chỉ vào một người
          cụ thể: lúc không ai bị gắn cờ thì không có ai để kết luận oan, và một thẻ cảnh báo dài giải
          thích chuyện giả mạo tín hiệu trở thành tiếng ồn ngay trên cùng — đọc như thể hệ thống đang có
          điều muốn nói, trong khi nó chỉ đang nói "không có gì". */}
      {soBiGanCo > 0 && (
        <Alert
          type="warning"
          showIcon
          className="mb-3"
          message={`${soBiGanCo} người có khuôn lặp đáng để hỏi lại`}
          description={
            'Tín hiệu do trình duyệt người chơi gửi lên nên chặn được và giả mạo được. Bảng này để bạn ' +
            'biết nên hỏi ai, không phải để kết luận ai gian lận. Hệ thống không trừ điểm của bất kỳ ai.'
          }
        />
      )}

      <Table<RoomProctoringSummaryRow>
        scroll={{ x: 'max-content' }}
        rowKey="playerId"
        size="small"
        dataSource={rows}
        pagination={false}
        columns={[
          {
            title: 'Người chơi',
            dataIndex: 'displayName',
            render: (ten: string | null, row) => (
              <div className="flex flex-wrap items-center gap-2">
                <Text>{ten ?? 'Người chơi'}</Text>
                {row.guest && <Tag className="mr-0!">khách</Tag>}
              </div>
            ),
          },
          {
            title: 'Số câu có khuôn lặp',
            dataIndex: 'soCauLap',
            width: 170,
            render: (soCau: number, row) =>
              row.biGanCo ? (
                <Tag color="red" className="mr-0!">
                  {soCau} câu
                </Tag>
              ) : (
                <Text className="text-ink-soft">{soCau} câu</Text>
              ),
          },
          {
            // Cột này KHÔNG phải căn cứ gắn cờ — nói rõ ở tiêu đề để host không đọc nó như thước đo chính
            title: 'Số lần rời trang (tham khảo)',
            dataIndex: 'soLanRoiTrang',
            width: 210,
            render: (soLan: number) => <Text className="text-ink-soft">{soLan}</Text>,
          },
        ]}
      />
    </div>
  )
}
