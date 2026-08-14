import { useState } from 'react'
import { Alert, Segmented, Skeleton, Space, Statistic, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import EmptyState from '@/shared/components/EmptyState'
import PageHeader from '@/shared/components/PageHeader'
import type { AiUsageByFeature, AiUsageByProvider } from '../api/adminApi'
import { useAiUsage } from '../hooks/useAdmin'

const { Text } = Typography

const FEATURE_LABEL: Record<string, string> = {
  embedding: 'Sinh vector nhúng',
  generation: 'Sinh đề',
  grading: 'Chấm tự luận',
  'grade-answer': 'Chấm tự luận',
  chat: 'Trợ lý học tập',
}

/** Hiện "—" thay vì 0 khi chưa có số: 0 ms là một giá trị có nghĩa, "chưa đo" thì không. */
function ms(value: number | null) {
  return value === null ? '—' : `${value.toLocaleString('vi-VN')} ms`
}

export default function AdminAiUsagePage() {
  const [days, setDays] = useState(30)
  const { data, isPending } = useAiUsage(days)

  const featureColumns: ColumnsType<AiUsageByFeature> = [
    {
      title: 'Chức năng',
      dataIndex: 'chucNang',
      render: (value: string) => (
        <Text className="text-sm font-bold">{FEATURE_LABEL[value] ?? value}</Text>
      ),
    },
    { title: 'Lượt gọi', dataIndex: 'luotGoi', align: 'right' },
    {
      title: 'Token vào',
      dataIndex: 'tokenVao',
      align: 'right',
      render: (v: number) => v.toLocaleString('vi-VN'),
    },
    {
      title: 'Token ra',
      dataIndex: 'tokenRa',
      align: 'right',
      render: (v: number) => v.toLocaleString('vi-VN'),
    },
    {
      title: 'Độ trễ trung bình',
      dataIndex: 'doTreTrungBinhMs',
      align: 'right',
      render: (v: number | null) => ms(v),
    },
  ]

  const providerColumns: ColumnsType<AiUsageByProvider> = [
    {
      title: 'Nhà cung cấp',
      dataIndex: 'nhaCungCap',
      render: (value: string) => (
        <Tag color={value === 'gemini' ? 'geekblue' : 'orange'}>{value}</Tag>
      ),
    },
    { title: 'Lượt gọi', dataIndex: 'luotGoi', align: 'right' },
    {
      title: 'Lượt thất bại',
      dataIndex: 'luotThatBai',
      align: 'right',
      render: (v: number) => (v > 0 ? <Text type="danger">{v}</Text> : v),
    },
  ]

  if (isPending) {
    return <Skeleton active paragraph={{ rows: 8 }} />
  }

  if (!data || data.tongLuotGoi === 0) {
    return (
      <Space direction="vertical" size="large" className="w-full">
        <PageHeader title="Giám sát AI" description="Chi phí, độ tin cậy và độ trễ của các lời gọi mô hình" />
        <EmptyState
          title="Chưa có lời gọi mô hình nào trong khoảng thời gian này"
          hint="Số liệu xuất hiện sau khi có người dùng sinh đề, chấm tự luận hoặc hỏi trợ lý học tập"
        />
      </Space>
    )
  }

  const tiLeThatBai = data.tongLuotGoi === 0 ? 0 : (data.luotThatBai / data.tongLuotGoi) * 100

  return (
    <Space direction="vertical" size="large" className="w-full">
      <PageHeader
        title="Giám sát AI"
        description="Chi phí, độ tin cậy và độ trễ của các lời gọi mô hình. Không hiển thị khoá API hay nội dung câu hỏi."
        actions={
          <Segmented
            value={days}
            onChange={(value) => setDays(value as number)}
            options={[
              { label: '7 ngày', value: 7 },
              { label: '30 ngày', value: 30 },
              { label: '90 ngày', value: 90 },
            ]}
          />
        }
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="border border-line bg-white p-4">
          <Statistic title="Tổng lượt gọi" value={data.tongLuotGoi} />
        </div>
        <div className="border border-line bg-white p-4">
          <Statistic
            title="Tỉ lệ thất bại"
            value={tiLeThatBai}
            precision={1}
            suffix="%"
            valueStyle={tiLeThatBai > 5 ? { color: '#c0392b' } : undefined}
          />
          <Text className="text-ink-soft text-xs">{data.luotThatBai} / {data.tongLuotGoi} lượt</Text>
        </div>
        <div className="border border-line bg-white p-4">
          <Statistic
            title="Tổng token"
            value={data.tongTokenVao + data.tongTokenRa}
            formatter={(v) => Number(v).toLocaleString('vi-VN')}
          />
          <Text className="text-ink-soft text-xs">
            vào {data.tongTokenVao.toLocaleString('vi-VN')} · ra {data.tongTokenRa.toLocaleString('vi-VN')}
          </Text>
        </div>
        <div className="border border-line bg-white p-4">
          <Statistic title="Độ trễ P95" value={ms(data.doTreP95Ms)} />
          <Text className="text-ink-soft text-xs">trung bình {ms(data.doTreTrungBinhMs)}</Text>
        </div>
      </div>

      {/* Dùng dự phòng là tín hiệu vận hành, không phải lỗi: nhà cung cấp chính đang có vấn đề hoặc
          hạn mức bị đụng trần thường xuyên. Nói rõ ý nghĩa thay vì chỉ hiện một con số. */}
      {data.luotDungDuPhong > 0 && (
        <Alert
          type="warning"
          showIcon
          message={`${data.luotDungDuPhong} lượt phải dùng nhà cung cấp dự phòng`}
          description="Nhà cung cấp chính đang lỗi tạm thời hoặc hạn mức bị đụng trần. Hệ thống vẫn trả lời được, nhưng nên kiểm tra hạn mức và trạng thái dịch vụ."
        />
      )}

      <div>
        <Text className="mb-2 block text-sm font-bold">Theo chức năng</Text>
        <Table<AiUsageByFeature>
          rowKey="chucNang"
          size="small"
          columns={featureColumns}
          dataSource={data.theoChucNang}
          pagination={false}
        />
      </div>

      <div>
        <Text className="mb-2 block text-sm font-bold">Theo nhà cung cấp</Text>
        <Table<AiUsageByProvider>
          rowKey="nhaCungCap"
          size="small"
          columns={providerColumns}
          dataSource={data.theoNhaCungCap}
          pagination={false}
        />
      </div>
    </Space>
  )
}
