import { Button, Typography } from 'antd'
import MathText from './MathText'

const { Text } = Typography

/** Có ít nhất một cặp `$...$` với phần giữa không rỗng — tức người viết ĐÃ đánh dấu công thức. */
export function coDanhDauCongThuc(noiDung: string): boolean {
  return /\$[^$]+\$/.test(noiDung ?? '')
}

/**
 * Trông có vẻ là toán nhưng CHƯA được đánh dấu.
 *
 * Cố ý bắt hẹp — chỉ `^` (luỹ thừa) và lệnh LaTeX viết trần. Không bắt dấu `/` hay `*`: chúng xuất
 * hiện đầy trong câu chữ bình thường ("và/hoặc", "km/h"), và một gợi ý nhảy ra sai chỗ vài lần là
 * người dùng thôi đọc nó.
 */
function coVeLaToan(noiDung: string): boolean {
  return /\^|\\frac|\\sqrt|\\sum|\\int|\\cdot/.test(noiDung ?? '')
}

/**
 * Ký tự được coi là "thuộc về một biểu thức toán".
 *
 * Chữ cái tiếng Việt có dấu (`à`, `ạ`, `ố`…) **không** nằm trong tập này, và đó chính là thứ khoanh
 * vùng công thức giúp: một câu như *"Đạo hàm của hàm số y = x^2 là:"* tự đứt ở `số` và `là`, để lại
 * đúng đoạn `y = x^2` ở giữa. Không cần hiểu tiếng Việt, chỉ cần biết chữ nào có dấu.
 */
const KY_TU_TOAN = /[A-Za-z0-9 ^_{}()[\]+\-*/=.,'|<>\\]/

/** Chữ cái theo Unicode — dùng để biết một ký tự có thuộc về một TỪ hay không. */
const LA_CHU_CAI = /\p{L}/u

/**
 * Tên hàm toán viết bằng chữ cái — chúng THUỘC về công thức dù trông như một từ.
 *
 * Không có danh sách này thì `y' = 2^x . ln 2` bị cắt ngay trước `ln`, và đáp án của mọi câu đạo hàm
 * hàm mũ đều hỏng.
 */
const TEN_HAM_TOAN = new Set([
  'ln', 'log', 'lg', 'exp', 'sin', 'cos', 'tan', 'cot', 'sec', 'csc',
  'lim', 'max', 'min', 'sup', 'inf', 'det', 'dim', 'mod', 'gcd', 'lcm',
  'sqrt', 'arcsin', 'arccos', 'arctan',
])

/**
 * Một "từ" thuần chữ cái, dài từ 2 ký tự và không phải tên hàm toán.
 *
 * Đây là chỗ khó nhất của việc khoanh vùng: tiếng Việt có rất nhiều từ **không dấu** — `khi`, `cho`,
 * `tam`, `va`, `la` — nên xét theo ký tự thì chúng giống hệt biến số. Xét theo TỪ thì phân biệt được:
 * biến số trong đề phổ thông gần như luôn là một chữ cái (`x`, `y`, `a`), còn từ tiếng Việt thì từ hai
 * chữ trở lên.
 *
 * Chấp nhận đánh đổi: một công thức đặt tên biến hai chữ (`ab = cd`) sẽ bị cắt. Hiếm, và người viết
 * thấy ngay ở ô nhập rồi sửa — trong khi nhầm chiều ngược lại thì `khi`, `và` bị dựng thành ký hiệu
 * nghiêng ngay giữa câu tiếng Việt, trông như hệ thống hỏng.
 */
function laTuTiengViet(token: string): boolean {
  return /^[A-Za-z]{2,}$/.test(token) && !TEN_HAM_TOAN.has(token.toLowerCase())
}

/**
 * Cắt một đoạn thành các cụm ngăn bởi từ tiếng Việt, rồi chỉ bọc những cụm thật sự có dấu hiệu toán.
 *
 * Nhờ bước này, `x^2 + y^2 khi x = 3` ra `$x^2 + y^2$ khi x = 3` — cụm sau không có `^` nên không bọc,
 * và chữ `khi` không bị kéo vào giữa công thức.
 */
function tachTheoTu(doan: string): string {
  const cum: string[] = []
  let dem = ''
  for (const token of doan.split(/(\s+)/)) {
    if (laTuTiengViet(token)) {
      cum.push(dem, token)
      dem = ''
    } else {
      dem += token
    }
  }
  cum.push(dem)

  return cum
    .map((c) => {
      if (laTuTiengViet(c.trim()) || !coVeLaToan(c)) {
        return c
      }
      const truoc = c.match(/^\s*/)?.[0] ?? ''
      const sau = c.match(/[\s.,]*$/)?.[0] ?? ''
      const loi = c.slice(truoc.length, c.length - sau.length)
      return loi.length >= 2 ? `${truoc}$${loi}$${sau}` : c
    })
    .join('')
}

/**
 * Bọc các đoạn trông giống công thức trong `$...$`.
 *
 * ## Đây là ĐỀ NGHỊ MỘT LẦN BẤM, không phải chuyển đổi tự động
 * Khác biệt với việc tự đổi nội dung lúc hiển thị nằm ở chỗ **người viết nhìn thấy kết quả ngay**:
 * dấu `$` hiện ra trong chính ô họ đang gõ, và hộp xem trước bên dưới dựng công thức lên. Đoán sai
 * thì họ xoá hai dấu đô la là xong. Tự đổi ở tầng hiển thị thì họ không bao giờ biết chữ mình đã bị
 * hiểu khác.
 *
 * ## Đã có `$` rồi thì không đụng
 * Người viết đã tự đánh dấu nghĩa là họ hiểu cú pháp và đã chọn ranh giới của mình. Bọc chồng lên là
 * phá đúng thứ họ vừa làm đúng.
 */
export function bocCongThuc(noiDung: string): string {
  if (!noiDung || coDanhDauCongThuc(noiDung)) {
    return noiDung
  }

  // Tách thành các đoạn xen kẽ: đoạn "có thể là toán" và đoạn không.
  const doan: { chu: string; laToan: boolean }[] = []
  for (const ky of noiDung) {
    const laToan = KY_TU_TOAN.test(ky)
    const cuoi = doan.at(-1)
    if (cuoi && cuoi.laToan === laToan) {
      cuoi.chu += ky
    } else {
      doan.push({ chu: ky, laToan })
    }
  }

  const laChuCai = (ky: string | undefined) => Boolean(ky) && LA_CHU_CAI.test(ky as string)

  return doan
    .map((d, i) => {
      if (!d.laToan) {
        return d.chu
      }

      // `loi` co dần lại còn đúng phần công thức; `tienTo`/`hauTo` giữ phần bị cắt để trả lại nguyên
      // vẹn — người viết không được mất một ký tự nào chỉ vì bấm nút này.
      let loi = d.chu
      let tienTo = ''
      let hauTo = ''

      const catCuoi = (mau: RegExp) => {
        const khop = loi.match(mau)?.[0]
        if (khop) {
          hauTo = khop + hauTo
          loi = loi.slice(0, loi.length - khop.length)
        }
      }
      const catDau = (mau: RegExp) => {
        const khop = loi.match(mau)?.[0]
        if (khop) {
          tienTo += khop
          loi = loi.slice(khop.length)
        }
      }

      // ── Cắt phần chữ cái dính vào từ tiếng Việt ở hai đầu ──
      //
      // Chữ có dấu (à, ố) không thuộc tập ký tự toán, nhưng chữ cái THƯỜNG trong cùng một từ thì có:
      // "là" đứt thành `l` + `à`, "Hàm" thành `H` + `àm`. Không cắt thì công thức nuốt mất chữ cái
      // đầu của từ bên cạnh — `$y = x^2 l$à:`.
      //
      // Dấu hiệu để biết: ký tự NGAY SAU đoạn có phải chữ cái không. Nếu có, mấy chữ cái ASCII ở cuối
      // đoạn thuộc về từ đó chứ không thuộc công thức. Nhờ vậy `y = x^2 + a` — kết thúc bằng biến,
      // phía sau là dấu hai chấm hoặc hết chuỗi — vẫn giữ nguyên chữ `a`.
      if (laChuCai(doan[i + 1]?.chu[0])) {
        catCuoi(/[A-Za-z]+$/)
      }
      const truocDo = doan[i - 1]?.chu
      if (laChuCai(truocDo?.at(-1))) {
        catDau(/^[A-Za-z]+/)
      }

      catDau(/^\s*/)
      catCuoi(/[\s]*$/)

      return tienTo + tachTheoTu(loi) + hauTo
    })
    .join('')
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
 * một dòng chữ, hoặc xoá hai dấu đô la; đoán sai lúc tự đổi thì họ mất nội dung mà không hay biết.
 *
 * ## Chỉ hiện khi có ích
 * Không có dấu hiệu toán nào thì component trả `null`. Một dòng nhắc thường trực ở mọi câu hỏi sẽ bị
 * đọc lướt qua ngay từ câu thứ ba, và lúc thật sự cần thì nó đã thành nền.
 */
export default function XemTruocCongThuc({
  noiDung,
  onDoi,
}: {
  noiDung: string
  /** Có truyền thì hiện nút "Bọc thành công thức". Bỏ trống thì chỉ gợi ý bằng chữ. */
  onDoi?: (noiDungMoi: string) => void
}) {
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
      <div className="mt-1 flex flex-wrap items-center gap-2">
        <Text className="text-ink-soft text-xs">
          Có vẻ đây là công thức. Bọc phần công thức trong <b>$...$</b> để nó hiện thành công thức
          thật — ví dụ <code>$y = x^2$</code>.
        </Text>
        {onDoi && (
          <Button size="small" onClick={() => onDoi(bocCongThuc(noiDung))}>
            Bọc thành công thức
          </Button>
        )}
      </div>
    )
  }

  return null
}
