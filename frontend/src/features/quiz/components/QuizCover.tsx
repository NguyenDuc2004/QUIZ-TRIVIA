import { boMatCua } from '../coverGradient'

/** Cỡ biểu tượng khi quiz chưa có ảnh — theo bề ngang của chỗ đặt, không theo ý thích. */
type CoIcon = 'lon' | 'vua' | 'nho'

const CO_ICON: Record<CoIcon, string> = {
  lon: 'text-7xl',
  vua: 'text-[54px]',
  nho: 'text-lg',
}

/**
 * Ảnh bìa quiz — **một khuôn duy nhất cho mọi chỗ quiz xuất hiện**.
 *
 * ## Vì sao phải là component dùng chung
 * Trước đây mỗi trang tự dựng khối bìa, và chúng trôi khỏi nhau: lưới Khám phá dựng một kiểu, thẻ gợi ý
 * một kiểu, trang giới thiệu quiz thì **không vẽ gì cả** khi quiz chưa có ảnh — trong khi chính chú
 * thích ở thẻ gợi ý đã ghi *"một quiz phải trông như chính nó ở mọi chỗ nó xuất hiện"*. Quy ước chỉ nằm
 * trong lời văn thì nó sẽ bị phá ở trang thứ tư.
 *
 * ## Vì sao ảnh phải `absolute inset-0`, không phải `h-full w-full`
 * Đây là lỗi người dùng chỉ ra: thẻ có ảnh thật **cao hơn** thẻ vẽ gradient khoảng 40px, dù cả hai đều
 * đặt `aspect-video`.
 *
 * `height: 100%` phân giải theo chiều cao của khối chứa. Ở đây chiều cao đó do `aspect-ratio` suy ra từ
 * bề ngang, tức nó là `auto` chứ không phải một giá trị xác định — nên phần trăm **không phân giải
 * được**, ảnh rơi về `height: auto` và lấy đúng chiều cao gốc của tệp. Khung phình theo ảnh. Khối
 * gradient không có con nào nên nó giữ đúng 16:9, và hai thứ lệch nhau.
 *
 * `position: absolute` + `inset-0` thì chiều cao đo theo hộp đệm của phần tử định vị gần nhất — một giá
 * trị **xác định**. Ảnh không còn tác động được tới khung. Đó là điều kiện để câu "mọi ảnh bìa bằng
 * nhau" đúng với **mọi tệp người dùng tải lên**, dù nó vuông, dọc hay siêu rộng: `object-cover` cắt cho
 * vừa khuôn thay vì bắt khuôn giãn theo ảnh.
 */
export default function QuizCover({
  thumbnailUrl,
  categoryName,
  title,
  coIcon = 'lon',
  hienNhan = false,
  className = '',
}: {
  thumbnailUrl: string | null | undefined
  categoryName: string | null | undefined
  title: string
  coIcon?: CoIcon
  /** Hiện tên danh mục ở đáy, trên một lớp phủ tối dần. Dùng ở nơi bìa đủ lớn để đọc được chữ. */
  hienNhan?: boolean
  className?: string
}) {
  const boMat = boMatCua(categoryName, title)

  return (
    // MỘT khối bọc cho cả hai nhánh, và mọi thứ bên trong đều `absolute`. Nhờ vậy khung có kích thước
    // giống hệt nhau dù bên trong là ảnh hay là biểu tượng — không nhánh nào ảnh hưởng được tới nó.
    <div
      className={`relative aspect-video w-full overflow-hidden ${className}`}
      style={thumbnailUrl ? undefined : { background: boMat.nen }}
    >
      {thumbnailUrl ? (
        <img
          src={thumbnailUrl}
          alt=""
          loading="lazy"
          className="absolute inset-0 h-full w-full object-cover"
        />
      ) : (
        /* `select-none` để kéo chọn chữ trên lưới không tô xanh cả loạt biểu tượng.
           Hơi mờ để nó là nền chứ không tranh chỗ với tiêu đề quiz ngay bên dưới. */
        <span
          aria-hidden
          className={`absolute inset-0 flex select-none items-center justify-center opacity-90 drop-shadow-sm ${CO_ICON[coIcon]}`}
        >
          {boMat.icon}
        </span>
      )}

      {hienNhan && (
        /* Lớp phủ tối dần để chữ danh mục đọc được cả trên ảnh sáng lẫn trên nền gradient */
        <span className="absolute inset-x-0 bottom-0 bg-linear-to-t from-black/60 to-transparent p-3 text-xs font-bold text-white">
          {categoryName ?? 'Chưa phân loại'}
        </span>
      )}
    </div>
  )
}
