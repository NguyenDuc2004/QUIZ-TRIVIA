import { Typography } from 'antd'
import MathText from './MathText'

const { Text } = Typography

/** Có ít nhất một cặp `$...$` với phần giữa không rỗng — tức người viết ĐÃ đánh dấu công thức. */
export function coDanhDauCongThuc(noiDung: string): boolean {
  return /\$[^$]+\$/.test(noiDung ?? '')
}

/**
 * Trông có vẻ là toán nhưng CHƯA được đánh dấu.
 *
 * Cố ý bắt hẹp — chỉ `^` (luỹ thừa) và các lệnh LaTeX viết trần. Không bắt dấu `/` hay `*`: chúng
 * xuất hiện đầy trong câu chữ bình thường ("và/hoặc", "km/h"), và một gợi ý nhảy ra sai chỗ vài lần
 * là người dùng thôi đọc nó.
 */
function coVeLaToan(noiDung: string): boolean {
  return /\^|\\frac|\\sqrt|\\sum|\\int/.test(noiDung ?? '')
}

/**
 * Xem trước công thức ngay dưới ô nhập, và nhắc cú pháp đúng lúc người viết cần.
 *
 * ## Vì sao GỢI Ý thì đoán được, còn TỰ ĐỔI thì không
 * `MathText` cố ý không tự nhận diện toán trong chữ thường: đoán sai là bóp méo chính câu chữ người
 * dùng viết ra — một câu Tin học nhắc `a/b`, một câu Tiếng Anh có dấu `^`, đều thành ký hiệu vô nghĩa.
 * Luật đó không đổi.
 *
 * Nhưng "không tự đoán" chỉ công bằng nếu **người viết biết cách tự đánh dấu**, mà trước đây không có
 * gì trong màn soạn câu hỏi nhắc tới `$...$`. Giáo viên gõ `y = x^2` sẽ mãi gõ như vậy và không hiểu
 * vì sao đề của mình trông thô hơn đề AI sinh.
 *
 * Chỗ này đoán, nhưng đoán để **đề nghị** chứ không phải để **sửa**. Đoán sai thì người viết bỏ qua
 * một dòng chữ; đoán sai lúc tự đổi thì họ mất nội dung. Hai việc khác hẳn nhau về hậu quả, nên chịu
 * được hai mức chắc chắn khác nhau.
 *
 * ## Chỉ hiện khi có ích
 * Không có dấu hiệu toán nào thì component trả `null`. Một dòng nhắc thường trực ở mọi câu hỏi sẽ bị
 * đọc lướt qua ngay từ câu thứ ba, và lúc thật sự cần thì nó đã thành nền.
 */
export default function XemTruocCongThuc({ noiDung }: { noiDung: string }) {
  if (coDanhDauCongThuc(noiDung)) {
    return (
      <div className="bg-surface-subtle border-line mt-2 rounded-control border px-3 py-2">
        <Text className="text-ink-soft mb-1 block text-xs font-bold">Xem trước</Text>
        <MathText>{noiDung}</MathText>
      </div>
    )
  }

  if (coVeLaToan(noiDung)) {
    return (
      <Text className="text-ink-soft mt-1 block text-xs">
        Có vẻ đây là công thức. Bọc phần công thức trong <b>$...$</b> để nó hiện thành công thức thật
        — ví dụ <code>$y = x^2$</code> thay cho <code>y = x^2</code>. Áp dụng cho cả các lựa chọn.
      </Text>
    )
  }

  return null
}
