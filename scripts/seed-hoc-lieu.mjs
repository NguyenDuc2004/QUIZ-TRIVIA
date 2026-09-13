/**
 * Nạp học liệu mẫu cho tài khoản người tạo nội dung demo, qua chính pipeline RAG của dự án.
 *
 * Chạy:  node scripts/seed-hoc-lieu.mjs
 * Yêu cầu: backend đang chạy (mặc định cổng 8081 — xem chú thích ở `frontend/vite.config.ts`).
 *
 * ## Vì sao cần
 * `seed-demo.mjs` dựng quiz, người dùng và lượt làm bài, nhưng **không nạp học liệu**. Hệ quả là hai
 * chức năng AI trông như chưa làm xong khi mở lên: màn Học liệu trống, và Trợ lý học tập không có gì
 * để trích dẫn nên chỉ trả lời "không tìm thấy trong học liệu". Cả hai đều là trụ cột của đề tài.
 *
 * ## Có tốn gì không
 * Có, nhưng không phải hạn mức sinh đề. Nạp học liệu gọi API **embedding** để dựng vector cho từng
 * đoạn. `AiQuotaService` cố ý không tính lượt embedding vào hạn mức mỗi ngày của người dùng — người
 * dùng không chủ động "xin" từng lượt gọi đó — nên script này không làm cạn hạn mức sinh đề. Nó vẫn
 * tiêu quota phía nhà cung cấp.
 *
 * ## Chạy lại không nhân đôi
 * Tài liệu trùng tiêu đề thì bỏ qua.
 */

const API = process.env.API ?? 'http://localhost:8081/api/v1'
const MAT_KHAU = 'MatKhau@123'
const EMAIL_GV = 'gv.demo@quizai.local'

/** Đợi tài liệu chuyển sang READY — sinh đề và hỏi trợ lý đều cần trạng thái này. */
const CHO_TOI_DA_MS = 120000

/* Nội dung tự soạn, không chép từ nguồn có bản quyền — mỗi tài liệu đủ dài để chia được vài đoạn,
 * và bám đúng những chủ đề đã có quiz, để phần trích dẫn của trợ lý có chỗ neo. */
const TAI_LIEU = [
  {
    title: 'Chuẩn hoá cơ sở dữ liệu quan hệ — 1NF, 2NF, 3NF',
    topic: 'Cơ sở dữ liệu',
    content: `Chuẩn hoá là quá trình tổ chức lại các quan hệ nhằm giảm dư thừa dữ liệu và loại bỏ các
bất thường khi thêm, sửa, xoá. Quá trình này dựa trên khái niệm phụ thuộc hàm: thuộc tính B phụ thuộc
hàm vào thuộc tính A khi mỗi giá trị của A xác định duy nhất một giá trị của B.

Dạng chuẩn thứ nhất (1NF) yêu cầu mọi thuộc tính của quan hệ đều mang giá trị nguyên tố, nghĩa là
không chứa nhóm lặp và không chứa giá trị đa trị. Một bảng lưu nhiều số điện thoại trong cùng một ô,
ngăn cách bằng dấu phẩy, vi phạm 1NF; cách sửa là tách thành một quan hệ riêng.

Dạng chuẩn thứ hai (2NF) yêu cầu quan hệ đã ở 1NF và mọi thuộc tính không khoá đều phụ thuộc hàm đầy
đủ vào toàn bộ khoá chính. Vi phạm 2NF chỉ xảy ra khi khoá chính gồm nhiều thuộc tính: nếu một thuộc
tính không khoá chỉ phụ thuộc vào một phần của khoá thì đó là phụ thuộc bộ phận và cần tách bảng.

Dạng chuẩn thứ ba (3NF) yêu cầu quan hệ đã ở 2NF và không tồn tại phụ thuộc bắc cầu: không thuộc tính
không khoá nào được xác định bởi một thuộc tính không khoá khác. Ví dụ trong bảng lưu mã sinh viên,
mã lớp và tên lớp, tên lớp phụ thuộc vào mã lớp chứ không trực tiếp vào mã sinh viên, nên cần tách
thông tin lớp sang một quan hệ riêng.

Chuẩn hoá làm giảm dư thừa nhưng tăng số phép nối khi truy vấn. Trong hệ thống đọc nhiều, đôi khi
người thiết kế cố ý phi chuẩn hoá một phần để đổi lấy tốc độ, và chấp nhận chi phí đồng bộ dữ liệu.`,
  },
  {
    title: 'Dao động điều hoà — phương trình, chu kỳ và năng lượng',
    topic: 'Vật lý',
    content: `Dao động điều hoà là dao động trong đó li độ của vật là một hàm sin hoặc cosin theo thời
gian. Phương trình li độ có dạng x = A·cos(ωt + φ), trong đó A là biên độ, ω là tần số góc và φ là pha
ban đầu. Vận tốc là đạo hàm của li độ theo thời gian, gia tốc là đạo hàm của vận tốc.

Vận tốc đạt độ lớn cực đại bằng ωA khi vật đi qua vị trí cân bằng, và bằng không tại hai biên. Gia tốc
thì ngược lại: bằng không tại vị trí cân bằng và đạt độ lớn cực đại bằng ω²A tại biên. Gia tốc luôn
hướng về vị trí cân bằng và tỉ lệ với li độ nhưng trái dấu.

Con lắc lò xo có tần số góc bằng căn bậc hai của tỉ số giữa độ cứng lò xo và khối lượng vật. Chu kỳ
của con lắc lò xo do đó không phụ thuộc vào biên độ và cũng không phụ thuộc vào gia tốc trọng trường.

Con lắc đơn dao động nhỏ có tần số góc bằng căn bậc hai của tỉ số giữa gia tốc trọng trường và chiều
dài dây. Chu kỳ con lắc đơn phụ thuộc vào chiều dài dây và vị trí địa lý, nhưng không phụ thuộc khối
lượng vật nặng. Điều kiện dao động điều hoà của con lắc đơn là biên độ góc phải nhỏ.

Trong dao động điều hoà không có ma sát, cơ năng được bảo toàn và tỉ lệ với bình phương biên độ. Động
năng và thế năng biến thiên tuần hoàn với tần số gấp đôi tần số dao động, và tổng của chúng luôn không
đổi.`,
  },
  {
    title: 'Cách mạng tháng Tám 1945 — bối cảnh và diễn biến chính',
    topic: 'Lịch sử Việt Nam',
    content: `Đầu năm 1945, Chiến tranh thế giới thứ hai bước vào giai đoạn kết thúc. Ngày 9 tháng 3
năm 1945, Nhật đảo chính Pháp trên toàn Đông Dương, chấm dứt sự cai trị của thực dân Pháp và dựng lên
chính quyền thân Nhật. Sự kiện này tạo ra khoảng trống quyền lực mà lực lượng cách mạng có thể tận
dụng.

Ngày 12 tháng 3 năm 1945, Ban Thường vụ Trung ương Đảng ra chỉ thị "Nhật – Pháp bắn nhau và hành động
của chúng ta", xác định kẻ thù chính lúc này là phát xít Nhật và phát động cao trào kháng Nhật cứu
nước. Phong trào phá kho thóc giải quyết nạn đói lan rộng ở đồng bằng Bắc Bộ.

Ngày 15 tháng 8 năm 1945, Nhật đầu hàng Đồng minh không điều kiện. Thời cơ khởi nghĩa chín muồi vì
quân Nhật ở Đông Dương mất tinh thần trong khi quân Đồng minh chưa vào. Hội nghị toàn quốc của Đảng
họp tại Tân Trào quyết định phát động Tổng khởi nghĩa trên cả nước.

Khởi nghĩa giành chính quyền thắng lợi ở Hà Nội ngày 19 tháng 8, ở Huế ngày 23 tháng 8 và ở Sài Gòn
ngày 25 tháng 8 năm 1945. Ngày 30 tháng 8, vua Bảo Đại thoái vị, chấm dứt chế độ quân chủ ở Việt Nam.

Ngày 2 tháng 9 năm 1945, tại Quảng trường Ba Đình, Chủ tịch Hồ Chí Minh đọc bản Tuyên ngôn Độc lập,
khai sinh nước Việt Nam Dân chủ Cộng hoà. Thắng lợi của Cách mạng tháng Tám đưa nhân dân từ thân phận
nô lệ thành người làm chủ đất nước.`,
  },
]

const nghi = (ms) => new Promise((r) => setTimeout(r, ms))

async function goi(duongDan, { method = 'GET', token, body } = {}) {
  const res = await fetch(API + duongDan, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  const chu = await res.text()
  const dl = chu ? JSON.parse(chu) : null
  if (!res.ok) {
    const e = new Error(`${method} ${duongDan} → ${res.status}: ${dl?.message ?? chu}`)
    e.status = res.status
    throw e
  }
  return dl
}

/** Chờ tài liệu xử lý xong. In trạng thái để người chạy biết nó đang chạy chứ không treo. */
async function choSanSang(token, id, tieuDe) {
  const hetHan = Date.now() + CHO_TOI_DA_MS
  while (Date.now() < hetHan) {
    const m = await goi(`/ai/materials/${id}`, { token })
    if (m.status === 'READY') return m
    if (m.status === 'FAILED') throw new Error(`xử lý hỏng: ${m.errorMessage ?? 'không rõ lý do'}`)
    await nghi(2000)
  }
  throw new Error(`"${tieuDe}" chưa READY sau ${CHO_TOI_DA_MS / 1000}s`)
}

async function main() {
  console.log(`API: ${API}\n`)

  const gv = await goi('/auth/login', {
    method: 'POST',
    body: { email: EMAIL_GV, password: MAT_KHAU },
  })

  const daCo = new Set(((await goi('/ai/materials?size=100', { token: gv.accessToken })).content ?? []).map((m) => m.title))

  let them = 0
  for (const tl of TAI_LIEU) {
    if (daCo.has(tl.title)) {
      console.log(`= đã có  ${tl.title}`)
      continue
    }

    console.log(`\n▸ ${tl.title}`)
    const m = await goi('/ai/materials', { method: 'POST', token: gv.accessToken, body: tl })
    console.log(`  nạp xong, đang chia đoạn và dựng vector…`)
    const xong = await choSanSang(gv.accessToken, m.id, tl.title)
    console.log(`  ✓ READY — ${xong.chunkCount ?? '?'} đoạn`)
    them++
  }

  console.log(`\nXong. Thêm ${them} tài liệu cho ${EMAIL_GV}.`)
  console.log('Trợ lý học tập giờ có nguồn để trích dẫn, và màn Sinh đề AI có tài liệu để chọn.')
}

main().catch((e) => {
  console.error('\nHỏng:', e.message)
  process.exit(1)
})
