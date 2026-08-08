/**
 * Phần trăm cho thẻ số liệu.
 * <p>
 * Truyền thẳng chuỗi vào `value` của `<Statistic>` thay vì dùng `suffix="%"`: Ant Design chèn một
 * khoảng trắng trước suffix, ra thành "6.0 %" — tiếng Việt không viết vậy.
 * <p>
 * `null` thành **"—"**, không thành "0%". Backend trả null khi *chưa có dữ liệu*, còn 0% nghĩa là
 * làm mà sai hết; `?? 0` ở chỗ hiển thị là xoá mất chính sự phân biệt đó (docs/features/09).
 */
export function formatPercent(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : `${value.toFixed(1)}%`
}
