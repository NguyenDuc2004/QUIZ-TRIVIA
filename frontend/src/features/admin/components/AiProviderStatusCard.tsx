import { Alert, Skeleton, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { AiProviderStatus } from '../api/adminApi'
import { useAiConfig } from '../hooks/useAdmin'

const { Text } = Typography

/**
 * Trạng thái cấu hình các nhà cung cấp AI (FR-83).
 *
 * **Không có ô nào hiện giá trị khoá API** — `security.md` cấm hiển thị khoá trong UI hay log, và backend
 * cũng không trả về giá trị để có mà hiện. Thứ cần khi chẩn đoán "vì sao AI không chạy" là *đã cấu hình
 * hay chưa*, không phải khoá là gì.
 *
 * Component này hiện **cả khi chưa có lượt gọi nào**: đó chính là lúc câu hỏi "AI có chạy được không"
 * đáng trả lời nhất, nên nó không được nằm sau nhánh trạng thái rỗng của bảng chi phí.
 */
export default function AiProviderStatusCard() {
  const { data, isPending } = useAiConfig()

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 2 }} />
  }
  if (!data) {
    return null
  }

  const columns: ColumnsType<AiProviderStatus> = [
    {
      title: 'Nhà cung cấp',
      dataIndex: 'ten',
      render: (ten: string, row) => (
        <div className="flex items-center gap-2">
          <Tag color={ten === 'gemini' ? 'geekblue' : 'orange'} className="mr-0!">
            {ten}
          </Tag>
          {data.thuTuUuTien[0] === ten && (
            <Text className="text-ink-soft text-xs">ưu tiên đầu</Text>
          )}
          {!data.thuTuUuTien.includes(row.ten) && (
            <Text className="text-ink-soft text-xs">ngoài thứ tự ưu tiên</Text>
          )}
        </div>
      ),
    },
    {
      title: 'Khoá API',
      dataIndex: 'daCauHinh',
      width: 150,
      // Chỉ true/false. Không có cột nào hiện giá trị, kể cả dạng che một phần.
      render: (daCauHinh: boolean) =>
        daCauHinh ? <Tag color="green">Đã cấu hình</Tag> : <Tag>Để trống</Tag>,
    },
    {
      title: 'Dùng được',
      dataIndex: 'sanSang',
      width: 130,
      render: (sanSang: boolean, row) =>
        sanSang ? (
          <Tag color="green">Sẵn sàng</Tag>
        ) : row.daCauHinh ? (
          // Có khoá nhưng không nằm trong provider-order: nguyên nhân khó đoán nhất của "đã có key mà
          // AI vẫn không chạy", nên nói thẳng ra thay vì chỉ hiện một dấu gạch
          <Tag color="gold">Bị loại khỏi thứ tự</Tag>
        ) : (
          <Tag>Không</Tag>
        ),
    },
    {
      title: 'Hỗ trợ',
      width: 200,
      render: (_, row) => (
        <Text className="text-ink-soft text-xs">
          {[row.hoTroEmbedding && 'vector nhúng', row.hoTroStreaming && 'streaming']
            .filter(Boolean)
            .join(' · ') || 'chỉ sinh văn bản'}
        </Text>
      ),
    },
  ]

  return (
    <div className="flex flex-col gap-3">
      {!data.coTheGoiAi && (
        <Alert
          type="error"
          showIcon
          message="Không có nhà cung cấp AI nào được cấu hình"
          description="Mọi chức năng AI (sinh đề, chấm tự luận, trợ lý học tập) sẽ báo lỗi. Đặt khoá API trong biến môi trường của máy chủ rồi khởi động lại backend."
        />
      )}

      <div>
        <Text className="mb-2 block text-sm font-bold">Cấu hình nhà cung cấp</Text>
        <Table<AiProviderStatus>
          rowKey="ten"
          size="small"
          columns={columns}
          dataSource={data.nhaCungCap}
          pagination={false}
        />
        <Text className="text-ink-soft mt-2 block text-xs">
          Thứ tự thử: {data.thuTuUuTien.join(' → ')} · tác vụ nền thử lại tối đa{' '}
          {data.soLuongThuLaiTacVuNen} lần. Khoá API đặt bằng biến môi trường, không sửa được qua giao
          diện — và giá trị khoá không bao giờ được trả về đây.
        </Text>
      </div>
    </div>
  )
}
