/**
 * Sinh quiz tiếng Việt bằng **chính module AI của dự án** (dogfooding).
 *
 * Chạy:  node scripts/seed-ai.mjs
 * Yêu cầu: backend đang chạy, và `GET /ai/status` trả `available: true`.
 *
 * ## Vì sao tách khỏi `seed-them.mjs`
 * Tệp kia không tốn gì: Open Trivia DB miễn phí, người học giả lập là script thuần. Tệp này **tiêu
 * hạn mức AI thật** — mỗi bộ đề là một lượt gọi mô hình, và hạn mức miễn phí của Gemini đã từng cạn
 * trong dự án này. Trộn hai thứ vào một tệp thì không ai chạy được phần rẻ mà không trả giá phần đắt.
 *
 * ## Vì sao dùng chính API của dự án chứ không gọi thẳng Gemini
 * Đi đường này thì mọi thứ đúng như Creator bấm tay: qua `AiOrchestrator` (có dự phòng Gemini→Groq),
 * qua bộ kiểm hợp lệ JSON, qua bước **duyệt của con người** trước khi câu hỏi vào ngân hàng, và có ghi
 * `ai_request_logs` để trang giám sát chi phí có số liệu. Gọi thẳng nhà cung cấp thì sinh ra dữ liệu
 * *trông giống* dữ liệu thật nhưng thiếu đúng những thứ làm nó thật.
 *
 * Nó cũng là phép thử chính tính năng bán hàng của đồ án — nếu script này chạy trơn thì phần sinh đề
 * chạy trơn, và số liệu ở đây dùng được cho mục 3.6 của báo cáo.
 *
 * ## Chạy lại không nhân đôi
 * Quiz trùng tiêu đề thì bỏ qua, nên chạy lại chỉ bù những bộ còn thiếu — quan trọng vì mỗi lượt chạy
 * lại là một lượt tốn tiền.
 */

const API = process.env.API ?? 'http://localhost:8081/api/v1'
const MAT_KHAU = 'MatKhau@123'

/**
 * Gemini bản miễn phí cho 5 lượt/phút. Nghỉ giữa hai bộ đề để không tự đẩy mình vào cảnh chờ 429 —
 * backend có cơ chế thử lại, nhưng chờ chủ động vẫn nhanh hơn bị chặn rồi mới lùi.
 */
const NGHI_GIUA_HAI_BO_MS = 15000

/** Job sinh đề thường xong trong 10–30 giây; để rộng vì còn thời gian xếp hàng khi bị chặn hạn mức. */
const CHO_TOI_DA_MS = 180000

const BO_DE = [
  {
    chuDe: 'Đạo hàm và ứng dụng của đạo hàm trong khảo sát hàm số',
    danhMuc: 'Toán học',
    tieuDe: 'Toán 12 — Đạo hàm và khảo sát hàm số',
    doKho: 'MEDIUM',
  },
  {
    chuDe: 'Dao động cơ học: dao động điều hoà, con lắc lò xo, con lắc đơn',
    danhMuc: 'Vật lý',
    tieuDe: 'Vật lý 12 — Dao động cơ',
    doKho: 'MEDIUM',
  },
  {
    chuDe: 'Cách mạng tháng Tám năm 1945 và sự ra đời của nước Việt Nam Dân chủ Cộng hoà',
    danhMuc: 'Lịch sử',
    tieuDe: 'Lịch sử 12 — Cách mạng tháng Tám 1945',
    doKho: 'EASY',
  },
  {
    chuDe: 'Cơ sở dữ liệu quan hệ: khoá chính, khoá ngoại, chuẩn hoá và câu lệnh SQL cơ bản',
    danhMuc: 'Tin học',
    tieuDe: 'Tin học — Cơ sở dữ liệu quan hệ và SQL',
    doKho: 'MEDIUM',
  },
  {
    chuDe: 'Kiến thức chung về khoa học, địa lý và đời sống Việt Nam',
    danhMuc: 'Kiến thức chung',
    tieuDe: 'Kiến thức chung — Khoa học và đời sống',
    doKho: 'EASY',
  },
]

const SO_CAU = 10

// ─────────────────────────────────────────────────────────────── tiện ích

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

async function taiKhoan(email, displayName, role) {
  try {
    return await goi('/auth/register', {
      method: 'POST',
      body: { email, password: MAT_KHAU, displayName, role },
    })
  } catch (e) {
    if (e.status !== 409) throw e
    return goi('/auth/login', { method: 'POST', body: { email, password: MAT_KHAU } })
  }
}

/**
 * Hỏi lại tới khi job kết thúc.
 *
 * In `aiThrottledSeconds` khi nhà cung cấp đang chặn hạn mức: không có con số đó thì màn hình chỉ có
 * một dấu chấm chạy, và người chạy script tưởng nó treo trong khi hệ thống đang xếp hàng đúng như
 * thiết kế — cùng lý do giao diện sinh đề hiện con số này.
 */
async function choJob(token, jobId) {
  const hetHan = Date.now() + CHO_TOI_DA_MS
  let baoChan = false

  while (Date.now() < hetHan) {
    const job = await goi(`/ai/jobs/${jobId}`, { token })

    if (job.status === 'SUCCEEDED') return job
    if (job.status === 'FAILED') throw new Error(job.errorMessage ?? 'job hỏng')

    if (job.aiThrottledSeconds > 0 && !baoChan) {
      console.log(`      (nhà cung cấp chặn hạn mức, chờ ~${job.aiThrottledSeconds}s)`)
      baoChan = true
    }
    await nghi(3000)
  }
  throw new Error(`job không xong sau ${CHO_TOI_DA_MS / 1000}s`)
}

// ─────────────────────────────────────────────────────────────── chạy

async function main() {
  console.log(`API: ${API}\n`)

  // Đăng nhập TRƯỚC rồi mới hỏi trạng thái AI: `/ai/status` mở cho mọi vai trò nhưng vẫn cần xác
  // thực, nên gọi trước khi có token thì nhận 401 — và thông báo "bạn cần đăng nhập" nghe như lỗi
  // cấu hình AI, dẫn người đọc đi tìm nhầm chỗ.
  const gv = await taiKhoan('gv.ai@quizai.local', 'Kho đề AI', 'CREATOR')

  const trangThai = await goi('/ai/status', { token: gv.accessToken })
  if (!trangThai.available) {
    console.error('Dịch vụ AI chưa cấu hình (GEMINI_API_KEY). Dừng — chạy tiếp chỉ tốn thời gian.')
    process.exit(1)
  }
  console.log(`Nhà cung cấp: ${trangThai.providers.join(', ')}
`)

  const danhMuc = await goi('/categories')
  const idDanhMuc = Object.fromEntries(danhMuc.map((c) => [c.name, c.id]))

  const daCo = new Set(((await goi('/quizzes?size=200')).content ?? []).map((q) => q.title))

  let tongCau = 0
  let soBoXong = 0

  for (const bo of BO_DE) {
    if (daCo.has(bo.tieuDe)) {
      console.log(`= đã có  ${bo.tieuDe}`)
      continue
    }

    console.log(`\n▸ ${bo.tieuDe}`)
    console.log(`  chủ đề: ${bo.chuDe}`)

    let job
    try {
      const nhan = await goi('/ai/generate-questions', {
        method: 'POST',
        token: gv.accessToken,
        body: {
          topic: bo.chuDe,
          count: SO_CAU,
          difficulty: bo.doKho,
          types: ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'],
          useMaterials: false,
        },
      })
      console.log(`  job ${nhan.id} — đang sinh…`)
      job = await choJob(gv.accessToken, nhan.id)
    } catch (e) {
      console.warn(`  ! bỏ qua: ${e.message}`)
      await nghi(NGHI_GIUA_HAI_BO_MS)
      continue
    }

    const ketQua = typeof job.result === 'string' ? JSON.parse(job.result) : job.result
    const cauHoi = ketQua?.questions ?? []
    if (cauHoi.length === 0) {
      console.warn('  ! mô hình không trả câu nào, bỏ qua')
      await nghi(NGHI_GIUA_HAI_BO_MS)
      continue
    }

    // Duyệt TẤT CẢ. Script không đọc hiểu được nội dung nên không lọc thay người được — và lọc bừa
    // bằng vài luật máy móc còn tệ hơn, vì nó bỏ đi những câu đúng mà không ai biết.
    // Người dùng vẫn duyệt lại được ở Ngân hàng câu hỏi, đó mới là chỗ human-in-the-loop thật.
    const daDuyet = await goi(`/ai/jobs/${job.id}/approve`, {
      method: 'POST',
      token: gv.accessToken,
      body: { indexes: cauHoi.map((_, i) => i) },
    })
    console.log(`  duyệt ${daDuyet.length}/${cauHoi.length} câu vào ngân hàng`)

    const quiz = await goi('/quizzes', {
      method: 'POST',
      token: gv.accessToken,
      body: {
        title: bo.tieuDe,
        description:
          `Bộ câu hỏi do AI của hệ thống sinh từ chủ đề "${bo.chuDe}". ` +
          'Đã qua bước duyệt của người tạo nội dung trước khi xuất bản.',
        categoryId: idDanhMuc[bo.danhMuc] ?? null,
        difficulty: bo.doKho,
        visibility: 'PUBLIC',
        timeLimitSec: 900,
        strictExam: false,
      },
    })

    await goi(`/quizzes/${quiz.id}/questions`, {
      method: 'PUT',
      token: gv.accessToken,
      body: { questionIds: daDuyet.map((c) => c.id) },
    })

    console.log(`  + tạo quiz (${daDuyet.length} câu)`)
    tongCau += daDuyet.length
    soBoXong++

    await nghi(NGHI_GIUA_HAI_BO_MS)
  }

  console.log(`\nXong. ${soBoXong} quiz mới, ${tongCau} câu hỏi.`)
  console.log('Câu hỏi đánh dấu nguồn AI_GENERATED — xem lại được ở Ngân hàng câu hỏi.')
}

main().catch((e) => {
  console.error('\nHỏng:', e.message)
  process.exit(1)
})
