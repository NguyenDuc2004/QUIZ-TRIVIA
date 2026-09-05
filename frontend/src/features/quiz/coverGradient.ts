/**
 * Khối màu thay ảnh bìa cho quiz chưa có ảnh, và biểu tượng của danh mục.
 *
 * ## Màu theo DANH MỤC, không theo tiêu đề
 * Bản đầu chọn màu bằng `title.charCodeAt(0)`, tức hai quiz cùng thuộc "Toán học" ra hai màu khác nhau
 * còn một quiz Toán và một quiz Lịch sử lại có thể trùng màu. Màu khi đó chỉ là trang trí ngẫu nhiên —
 * mắt người dùng học được một quy luật *không tồn tại*.
 *
 * Buộc màu vào danh mục thì nó **mang thông tin**: lướt lưới Khám phá là nhận ra ngay mảng nào là mảng
 * nào, và bộ lọc danh mục phía trên có một hình ảnh tương ứng dưới lưới. Cùng lý do với việc chọn theo
 * tiêu đề thay vì random ở bản cũ — chỉ là chọn đúng thứ để bám vào.
 *
 * ## Vì sao đặt ở đây chứ không trong component
 * Cùng một quiz phải ra **cùng một màu** ở mọi chỗ nó xuất hiện: lưới Khám phá, thẻ Gợi ý, danh sách bài
 * được giao. Mỗi nơi tự chọn thì người dùng thấy hai thẻ khác màu và không nhận ra đó là một quiz.
 */

interface BoMat {
  /** Nền khối bìa. */
  nen: string
  /** Biểu tượng — đủ để nhận ra danh mục khi lướt nhanh, không cần đọc chữ. */
  icon: string
}

/**
 * Sáu danh mục có sẵn của hệ thống (migration V2).
 *
 * Màu chọn theo liên tưởng quen thuộc của từng môn chứ không bốc ngẫu nhiên: xanh dương cho khoa học tự
 * nhiên, cam đất cho lịch sử, xanh lá cho ngôn ngữ. Liên tưởng sai còn khó nhớ hơn không có liên tưởng.
 */
const THEO_DANH_MUC: Record<string, BoMat> = {
  'Toán học': { nen: 'linear-gradient(135deg, #1e3a8a, #3b82f6)', icon: '📐' },
  'Tin học': { nen: 'linear-gradient(135deg, #0f766e, #14b8a6)', icon: '💻' },
  'Tiếng Anh': { nen: 'linear-gradient(135deg, #166534, #22c55e)', icon: '🔤' },
  'Vật lý': { nen: 'linear-gradient(135deg, #4c1d95, #8b5cf6)', icon: '⚛️' },
  'Lịch sử': { nen: 'linear-gradient(135deg, #9a3412, #f97316)', icon: '🏛️' },
  'Kiến thức chung': { nen: 'linear-gradient(135deg, #a16207, #eab308)', icon: '🧠' },
}

/**
 * Dự phòng cho danh mục người dùng tự thêm, và cho quiz **chưa phân loại**.
 *
 * Vẫn phải ổn định theo tên chứ không random: quiz chưa phân loại cũng cần giữ nguyên màu giữa các lần
 * vẽ, nếu không thì mỗi lần cuộn trang lưới lại đổi màu.
 */
const DU_PHONG: BoMat[] = [
  { nen: 'linear-gradient(135deg, #5624d0, #a435f0)', icon: '📚' },
  { nen: 'linear-gradient(135deg, #be123c, #f43f5e)', icon: '✏️' },
  { nen: 'linear-gradient(135deg, #0369a1, #0ea5e9)', icon: '🔍' },
  { nen: 'linear-gradient(135deg, #4d7c0f, #84cc16)', icon: '🌱' },
  { nen: 'linear-gradient(135deg, #7c2d12, #c2410c)', icon: '🗂️' },
]

function duPhongCua(khoa: string): BoMat {
  // Cộng mã ký tự thay vì chỉ lấy ký tự đầu: tên danh mục tiếng Việt hay trùng chữ cái đầu
  // ("Tin học", "Tiếng Anh", "Toán học"), nên lấy mỗi ký tự đầu là dồn hết vào một màu.
  let tong = 0
  for (let i = 0; i < khoa.length; i++) {
    tong += khoa.charCodeAt(i)
  }
  return DU_PHONG[tong % DU_PHONG.length]
}

/** Bộ mặt của một quiz: nền khối bìa và biểu tượng danh mục. */
export function boMatCua(categoryName: string | null | undefined, title: string): BoMat {
  if (categoryName && THEO_DANH_MUC[categoryName]) {
    return THEO_DANH_MUC[categoryName]
  }
  return duPhongCua(categoryName ?? title)
}

/* `coverOf` (chỉ trả về nền, bỏ biểu tượng) đã xoá ngày 05/09/2026: từ khi có `QuizCover` thì không
   còn chỗ nào chỉ cần màu mà không cần biểu tượng, và một hàm export không ai gọi là một lời mời
   dựng lại khối bìa bằng tay ở chỗ tiếp theo. */
