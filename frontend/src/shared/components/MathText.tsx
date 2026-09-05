import { useMemo } from 'react'
import katex from 'katex'
import 'katex/dist/katex.min.css'

/**
 * Hiện một đoạn văn bản có thể chứa công thức toán, đánh dấu bằng `$...$`.
 *
 * ## Vì sao phải có mốc `$`, không tự nhận diện
 * KaTeX chỉ dựng được **LaTeX**. Nội dung câu hỏi trong hệ thống là văn bản thường do người dùng hoặc mô
 * hình viết ra — `y = 2^(x^2 - x)` là chữ, không phải LaTeX. Muốn dựng nó thành công thức thì phải *đoán*
 * chỗ nào là toán rồi tự chuyển đổi, và đoán sai là làm hỏng chính câu chữ của người dùng: một câu Tin học
 * nhắc tới `a/b` hay một câu Tiếng Anh có dấu `^` sẽ bị bóp thành ký hiệu toán vô nghĩa.
 *
 * Mốc `$` là quy ước phổ biến (LaTeX, Markdown, Notion, GitHub) và nó đặt quyền quyết định vào tay người
 * viết đề: đánh dấu thì thành công thức, không đánh dấu thì giữ nguyên. Hệ thống không suy diễn gì.
 *
 * ## Hỏng thì hiện lại chữ gốc, không hiện lỗi
 * `throwOnError: false` — công thức sai cú pháp sẽ hiện nguyên đoạn chữ thay vì ném lỗi làm trắng cả
 * trang. Một câu hỏi sai định dạng vẫn phải đọc được; người học không sửa được cú pháp LaTeX của người ra
 * đề, nên chặn họ lại là phạt nhầm người.
 */
export default function MathText({ children, className }: { children: string; className?: string }) {
  const phan = useMemo(() => tach(children ?? ''), [children])

  // Không có công thức nào thì trả về chuỗi trần — tránh bọc thêm một lớp thẻ vô ích ở hàng nghìn dòng
  // bảng, và giữ nguyên hành vi cắt dòng của những chỗ đang dùng `line-clamp`.
  if (phan.length === 1 && !phan[0].laToan) {
    return <>{children}</>
  }

  return (
    <span className={className}>
      {phan.map((p, i) =>
        p.laToan ? (
          <span key={i} dangerouslySetInnerHTML={{ __html: dung(p.noiDung) }} />
        ) : (
          <span key={i}>{p.noiDung}</span>
        ),
      )}
    </span>
  )
}

interface Phan {
  noiDung: string
  laToan: boolean
}

/**
 * Tách chuỗi thành các đoạn chữ và đoạn công thức.
 *
 * ## Luật: KHÔNG BAO GIỜ sửa một ký tự nào của chữ thường
 * Một dấu `$` chỉ mở công thức khi có dấu `$` đóng ở phía sau **và** phần ở giữa không rỗng. Mọi trường
 * hợp còn lại giữ nguyên xi:
 *
 * - `100$$ một tháng` → hai dấu liền nhau, phần giữa rỗng ⇒ không phải công thức, giữ đủ cả hai dấu.
 * - `Chi phí $50 cho mỗi lần gọi` → không có dấu đóng ⇒ giữ nguyên.
 *
 * Bản đầu gộp `$$` thành một `$` để "thoát" ký tự. Đó là một mâu thuẫn với chính lý do component này tồn
 * tại: nó **sửa chữ của người dùng** — người viết gõ hai dấu, màn hình hiện một. Không đổi gì là hành vi
 * duy nhất không bao giờ sai.
 */
function tach(chuoi: string): Phan[] {
  const ket: Phan[] = []
  let dem = ''
  let i = 0

  while (i < chuoi.length) {
    if (chuoi[i] === '$') {
      const dong = chuoi.indexOf('$', i + 1)
      // Không có dấu đóng, hoặc phần giữa rỗng: đây là dấu đô la thật, không phải mốc công thức.
      if (dong === -1) {
        dem += chuoi.slice(i)
        break
      }
      if (dong === i + 1) {
        dem += '$$'
        i += 2
        continue
      }
      if (dem) {
        ket.push({ noiDung: dem, laToan: false })
        dem = ''
      }
      ket.push({ noiDung: chuoi.slice(i + 1, dong), laToan: true })
      i = dong + 1
      continue
    }

    dem += chuoi[i]
    i += 1
  }

  if (dem) {
    ket.push({ noiDung: dem, laToan: false })
  }
  return ket.length > 0 ? ket : [{ noiDung: '', laToan: false }]
}

function dung(latex: string): string {
  return katex.renderToString(latex, {
    throwOnError: false,
    // Chặn các lệnh LaTeX động (\includegraphics, \url…). Nội dung này do NGƯỜI DÙNG viết và hiện trên
    // màn hình người khác, nên nó là dữ liệu không tin được — cùng lý do với việc chỉ nhận ảnh đã tải lên
    // hệ thống ở `UploadedImagePath`.
    trust: false,
    strict: false,
    output: 'html',
  })
}
