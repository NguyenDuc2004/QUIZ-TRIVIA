import { useState } from 'react'
import { Alert, Card, Col, Row, Segmented, Skeleton, Statistic, Typography } from 'antd'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import PageHeader from '@/shared/components/PageHeader'
import EmptyState from '@/shared/components/EmptyState'
import { useSystemOverview } from '../hooks/useAdmin'

const { Text } = Typography

/**
 * Trang đầu khu quản trị (FR-40 → FR-42).
 *
 * Mọi con số trên trang này đến từ dữ liệu thật trong cơ sở dữ liệu. Không có chỉ số nào được nhân hệ số
 * hay ước lượng cho biểu đồ đẹp hơn — `ui-design-system.md §7`. Một trang tổng quan sai số tệ hơn không
 * có trang tổng quan, vì người ta ra quyết định dựa trên nó.
 */
export default function AdminOverviewPage() {
  const [days, setDays] = useState(14)
  const { data, isLoading, error } = useSystemOverview(days)

  if (error) {
    return (
      <Alert
        type="error"
        showIcon
        message="Không tải được số liệu tổng quan"
        description="Hãy thử lại; nếu vẫn lỗi thì kiểm tra kết nối tới cơ sở dữ liệu."
      />
    )
  }

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="Tổng quan hệ thống"
        description="Số liệu lấy trực tiếp từ cơ sở dữ liệu tại thời điểm mở trang."
        actions={
          <Segmented
            value={days}
            onChange={(value) => setDays(Number(value))}
            options={[
              { label: '7 ngày', value: 7 },
              { label: '14 ngày', value: 14 },
              { label: '30 ngày', value: 30 },
            ]}
          />
        }
      />

      {isLoading || !data ? (
        <Skeleton active paragraph={{ rows: 8 }} />
      ) : (
        <>
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic title="Người dùng" value={data.tongNguoiDung} />
                <Text className="text-ink-soft text-xs">
                  {data.soNguoiHoc} người học · {data.soNguoiTaoNoiDung} người tạo ·{' '}
                  {data.soQuanTri} quản trị
                  {data.soBiKhoa > 0 && ` · ${data.soBiKhoa} bị khoá`}
                </Text>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic title="Quiz" value={data.tongQuiz} />
                <Text className="text-ink-soft text-xs">
                  {data.quizCongKhai} công khai · {data.tongCauHoi} câu hỏi ·{' '}
                  {data.tongHocLieu} học liệu
                </Text>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                <Statistic title="Lượt làm bài" value={data.tongLuotLamBai} />
                <Text className="text-ink-soft text-xs">
                  Hôm nay {data.luotLamBaiHomNay} lượt · {data.dangKyHomNay} người mới
                </Text>
              </Card>
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Card>
                {/* Cắt theo tháng dương lịch vì hạn mức và hoá đơn của nhà cung cấp cũng tính theo tháng */}
                <Statistic
                  title="Token AI tháng này"
                  value={data.tokenThangNay}
                  // Dấu chấm phân cách nghìn theo quy ước tiếng Việt. Mặc định của antd là dấu phẩy,
                  // lệch với trang Giám sát AI vốn đã dùng `toLocaleString('vi-VN')`.
                  formatter={(v) => Number(v).toLocaleString('vi-VN')}
                />
                <Text className="text-ink-soft text-xs">
                  {data.luotGoiAiThangNay} lượt gọi · {data.phongDangCho} phòng chờ,{' '}
                  {data.phongDangChoi} đang chơi
                </Text>
              </Card>
            </Col>
          </Row>

          <Card title={`Tăng trưởng ${days} ngày gần nhất`}>
            {/* Ngày không có hoạt động vẫn là một điểm giá trị 0 (backend dùng generate_series): thiếu nó
                thì đường biểu đồ nối thẳng qua khoảng trống và trông như hoạt động liên tục */}
            <ResponsiveContainer width="100%" height={260}>
              <LineChart data={data.tangTruong}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--color-line)" />
                <XAxis dataKey="ngay" tick={{ fontSize: 11 }} tickFormatter={dinhDangNgay} />
                <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
                <Tooltip labelFormatter={(label) => dinhDangNgay(String(label))} />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="nguoiDungMoi"
                  name="Người dùng mới"
                  stroke="var(--color-brand)"
                  strokeWidth={2}
                />
                <Line
                  type="monotone"
                  dataKey="luotLamBai"
                  name="Lượt làm bài"
                  stroke="var(--color-ink)"
                  strokeWidth={2}
                />
              </LineChart>
            </ResponsiveContainer>
          </Card>

          <Row gutter={[16, 16]}>
            <Col xs={24} lg={12}>
              <Card title="Quiz theo danh mục">
                {data.theoDanhMuc.length === 0 ? (
                  <EmptyState
                    title="Chưa có quiz nào"
                    hint="Biểu đồ sẽ hiện khi có quiz đầu tiên."
                  />
                ) : (
                  <ResponsiveContainer width="100%" height={260}>
                    <PieChart>
                      <Pie
                        data={data.theoDanhMuc}
                        dataKey="soQuiz"
                        nameKey="danhMuc"
                        outerRadius={90}
                        label={({ name, value }) => `${name}: ${value}`}
                      >
                        {data.theoDanhMuc.map((phan, index) => (
                          <Cell key={phan.danhMuc} fill={MAU_TRON[index % MAU_TRON.length]} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                )}
              </Card>
            </Col>
            <Col xs={24} lg={12}>
              <Card title="Bài làm theo trạng thái">
                {data.tiLeHoanThanh.length === 0 ? (
                  <EmptyState
                    title="Chưa có lượt làm bài nào"
                    hint="Biểu đồ sẽ hiện khi có người làm bài."
                  />
                ) : (
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={data.tiLeHoanThanh}>
                      <CartesianGrid strokeDasharray="3 3" stroke="var(--color-line)" />
                      <XAxis dataKey="trangThai" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
                      <Tooltip
                        formatter={(value, name) =>
                          name === 'Độ chính xác TB'
                            ? [`${Number(value).toFixed(1)}%`, name]
                            : [value, name]
                        }
                      />
                      <Legend />
                      <Bar dataKey="soLuot" name="Số lượt" fill="var(--color-ink)" />
                      {/* Cột này rỗng ở trạng thái chưa nộp: backend trả null thay vì 0 vì "chưa ai làm"
                          không phải "0% đúng". Recharts bỏ qua null, đúng ý muốn */}
                      <Bar
                        dataKey="doChinhXacTrungBinh"
                        name="Độ chính xác TB"
                        fill="var(--color-brand)"
                      />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </Card>
            </Col>
          </Row>
        </>
      )}
    </div>
  )
}

/** Trục ngày chỉ cần ngày/tháng — chuỗi ISO đầy đủ làm nhãn chồng lên nhau. */
function dinhDangNgay(value: string) {
  const phan = value.split('-')
  // Không phải chuỗi ISO thì trả nguyên văn: nhãn sai còn đỡ hơn nhãn "undefined/undefined"
  return phan.length === 3 ? `${phan[2]}/${phan[1]}` : value
}

const MAU_TRON = [
  'var(--color-brand)',
  'var(--color-brand-strong)',
  'var(--color-ink)',
  'var(--color-ink-soft)',
  'var(--color-star)',
  'var(--color-rating)',
]
