/**
 * Nạp THÊM dữ liệu để web đủ dày cho demo và cho số liệu báo cáo.
 *
 * Chạy:  node scripts/seed-them.mjs
 * Yêu cầu: backend đang chạy (đổi cổng bằng biến môi trường API).
 *
 * Khác `seed-demo.mjs` ở mục đích: tệp kia dựng một bộ dữ liệu **nhỏ và có chọn lọc** để chụp ảnh màn
 * hình và thử từng loại câu hỏi. Tệp này lo phần **khối lượng** — thứ mà bảng xếp hạng, trang thống kê
 * và gợi ý Neo4j cần mới có nghĩa. Hai việc khác nhau nên để hai tệp, chạy độc lập được.
 *
 * ## Hai phần
 * 1. **Nhập câu hỏi từ Open Trivia DB** — API mở, giấy phép CC BY-SA.
 * 2. **Sinh người học và lượt làm bài** — thuần script, không gọi AI, không phụ thuộc mạng.
 *
 * ## Vì sao KHÔNG cào web
 * Câu hỏi trong các trang đề thi có bản quyền, điều khoản sử dụng của chúng thường cấm thu thập tự
 * động, và dữ liệu cào về hầu như luôn bẩn — dính thẻ HTML, mất đáp án đúng, lẫn quảng cáo. Đây cũng
 * là điều `.claude/skills/data-seeding` ghi rõ. Open Trivia DB cho đúng thứ cần: có cấu trúc, có danh
 * mục và độ khó, và được phép dùng lại.
 *
 * ## Vì sao câu tiếng Anh chỉ vào danh mục "Tiếng Anh"
 * Toàn bộ nội dung Open Trivia DB là tiếng Anh. Đổ chúng vào "Toán học" hay "Lịch sử" của một web
 * tiếng Việt thì người mở ra biết ngay đó là đồ độn. Trong danh mục "Tiếng Anh" thì một bộ câu hỏi
 * tiếng Anh là **nội dung thật**, không phải chỗ lấp. Các môn tiếng Việt để dành cho phần sinh bằng AI
 * của chính dự án.
 *
 * ## Chạy lại không nhân đôi
 * Quiz trùng tiêu đề thì bỏ qua; tài khoản đã có thì đăng nhập lại; người đã làm một quiz rồi thì
 * không làm lại quiz đó.
 */

const API = process.env.API ?? 'http://localhost:8081/api/v1'
const MAT_KHAU = 'MatKhau@123'

/** Open Trivia DB giới hạn 1 yêu cầu mỗi 5 giây cho mỗi IP — vượt là nhận mã lỗi 5. */
const NGHI_GIUA_HAI_LAN_GOI_MS = 5500

const SO_NGUOI_HOC = 30
const SO_QUIZ_MOI_NGUOI = [4, 9] // mỗi người làm ngẫu nhiên trong khoảng này

// ─────────────────────────────────────────────────────────────── tiện ích

const nghi = (ms) => new Promise((r) => setTimeout(r, ms))
const nganNhien = (a, b) => a + Math.floor(Math.random() * (b - a + 1))

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
 * Giải mã nội dung lấy về.
 *
 * ## Vì sao dùng `encode=base64` chứ không tự giải mã thực thể HTML
 * Mặc định Open Trivia DB trả chuỗi đã mã hoá thực thể HTML (`&quot;`, `&#039;`, `&deg;`). Bản đầu của
 * tệp này tự giải mã bằng một bảng tra viết tay — và **sót ngay ở lần chạy đầu tiên**: một câu về nhiệt
 * độ chứa `&deg;` hiện nguyên xi lên màn hình, vì bảng không có mục đó.
 *
 * Bảng tra thủ công sai theo kiểu không bao giờ hết: mỗi lần lấy thêm chủ đề mới là một cơ hội gặp một
 * thực thể chưa liệt kê, và nó **không gây lỗi** — chỉ hiện ra một chuỗi lạ giữa câu hỏi, mà chỉ người
 * đọc mới phát hiện được.
 *
 * `encode=base64` bỏ hẳn cả lớp vấn đề: không còn thực thể nào để mà sót.
 */
function giaiMa(chu) {
  return Buffer.from(chu, 'base64').toString('utf8')
}

// ─────────────────────────────────────────────────────────────── phần 1: Open Trivia DB

/**
 * Các bộ đề lấy về. `id` là mã danh mục của Open Trivia DB.
 *
 * Chọn những chủ đề mà một người học tiếng Anh đọc được: kiến thức chung, sách, phim, địa lý, khoa
 * học. Tránh các chủ đề quá hẹp về văn hoá đại chúng phương Tây (board games, anime) — người học Việt
 * Nam đọc xong cũng không trả lời được, và một bộ đề không ai làm nổi thì có cũng như không.
 */
const BO_DE = [
  { id: 9, ten: 'General Knowledge', doKho: 'easy' },
  { id: 9, ten: 'General Knowledge', doKho: 'medium' },
  { id: 10, ten: 'Books', doKho: 'easy' },
  { id: 22, ten: 'Geography', doKho: 'easy' },
  { id: 22, ten: 'Geography', doKho: 'medium' },
  { id: 17, ten: 'Science & Nature', doKho: 'easy' },
  { id: 23, ten: 'History', doKho: 'easy' },
  { id: 11, ten: 'Film', doKho: 'easy' },
]

const DO_KHO = { easy: 'EASY', medium: 'MEDIUM', hard: 'HARD' }

async function layTuOpenTdb(bo, soCau) {
  const url =
    `https://opentdb.com/api.php?amount=${soCau}&category=${bo.id}` +
    `&difficulty=${bo.doKho}&encode=base64`
  const res = await fetch(url)
  const dl = await res.json()

  // response_code: 0 = ok, 1 = không đủ câu, 5 = gọi quá nhanh
  if (dl.response_code !== 0) {
    throw new Error(`Open Trivia DB trả mã ${dl.response_code} cho ${bo.ten}/${bo.doKho}`)
  }

  return dl.results.map((c) => {
    const dung = giaiMa(c.correct_answer)
    const sai = c.incorrect_answers.map(giaiMa)

    if (c.type === 'boolean') {
      return {
        type: 'TRUE_FALSE',
        content: giaiMa(c.question),
        difficulty: DO_KHO[c.difficulty] ?? 'MEDIUM',
        options: [
          { content: 'True', correct: dung === 'True' },
          { content: 'False', correct: dung === 'False' },
        ],
      }
    }

    // Trộn đáp án đúng vào giữa: để nguyên thứ tự của API thì đáp án đúng luôn ở vị trí đầu, và người
    // làm bài chỉ cần bấm A là qua — bộ đề mất hết ý nghĩa.
    const luaChon = [{ content: dung, correct: true }, ...sai.map((s) => ({ content: s, correct: false }))]
    for (let i = luaChon.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1))
      ;[luaChon[i], luaChon[j]] = [luaChon[j], luaChon[i]]
    }

    return {
      type: 'SINGLE_CHOICE',
      content: giaiMa(c.question),
      difficulty: DO_KHO[c.difficulty] ?? 'MEDIUM',
      options: luaChon,
    }
  })
}

async function nhapOpenTdb(gv, idDanhMuc, tieuDeDaCo) {
  const daTao = []

  for (const bo of BO_DE) {
    const tieuDe = `English — ${bo.ten} (${bo.doKho})`
    if (tieuDeDaCo.has(tieuDe)) {
      console.log(`  = đã có  ${tieuDe}`)
      continue
    }

    let cauHoi
    try {
      cauHoi = await layTuOpenTdb(bo, 10)
    } catch (e) {
      console.warn(`  ! bỏ qua ${tieuDe}: ${e.message}`)
      await nghi(NGHI_GIUA_HAI_LAN_GOI_MS)
      continue
    }

    const quiz = await goi('/quizzes', {
      method: 'POST',
      token: gv.accessToken,
      body: {
        title: tieuDe,
        // Ghi nguồn ngay trong mô tả: người học biết đề đến từ đâu, và báo cáo có sẵn phần trích dẫn.
        description:
          `Bộ câu hỏi tiếng Anh về ${bo.ten.toLowerCase()}, mức ${bo.doKho}. ` +
          'Nguồn: Open Trivia DB (opentdb.com), giấy phép CC BY-SA 4.0.',
        categoryId: idDanhMuc['Tiếng Anh'] ?? null,
        difficulty: DO_KHO[bo.doKho],
        visibility: 'PUBLIC',
        timeLimitSec: 600,
        strictExam: false,
      },
    })

    const ids = []
    for (const ch of cauHoi) {
      const tao = await goi('/questions', {
        method: 'POST',
        token: gv.accessToken,
        body: { ...ch, topic: 'Tiếng Anh', points: 1 },
      })
      ids.push(tao.id)
    }

    await goi(`/quizzes/${quiz.id}/questions`, {
      method: 'PUT',
      token: gv.accessToken,
      body: { questionIds: ids },
    })

    console.log(`  + tạo    ${tieuDe} (${ids.length} câu)`)
    daTao.push(quiz.id)
    await nghi(NGHI_GIUA_HAI_LAN_GOI_MS)
  }

  return daTao
}

// ─────────────────────────────────────────────────────────────── phần 2: người học và lượt làm bài

const HO = ['Nguyễn', 'Trần', 'Lê', 'Phạm', 'Hoàng', 'Phan', 'Vũ', 'Đặng', 'Bùi', 'Đỗ']
const DEM = ['Văn', 'Thị', 'Minh', 'Quang', 'Thu', 'Hải', 'Ngọc', 'Anh']
const TEN = ['An', 'Bình', 'Chi', 'Dũng', 'Giang', 'Hà', 'Khoa', 'Linh', 'Mai', 'Nam', 'Oanh', 'Phúc', 'Quân', 'Sơn', 'Trang']

/**
 * Mỗi người có chủ đề MẠNH và chủ đề YẾU khác nhau.
 *
 * Đây không phải chi tiết trang trí: gợi ý của Neo4j dựa trên việc tìm những người **làm giống nhau**
 * rồi suy ra quiz nên học tiếp. Nếu ai cũng đúng 60% ở mọi chủ đề thì đồ thị không có cạnh nào đáng
 * kể, và trang gợi ý trả về danh sách ngẫu nhiên — trông vẫn "có dữ liệu" nhưng chức năng thì rỗng.
 */
function nguoiHoc(i) {
  const ten = `${HO[i % HO.length]} ${DEM[i % DEM.length]} ${TEN[i % TEN.length]}`
  const chuDe = ['Toán học', 'Vật lý', 'Tin học', 'Lịch sử', 'Tiếng Anh', 'Kiến thức chung']
  return {
    email: `hv${String(i).padStart(3, '0')}@quizai.local`,
    ten,
    gioi: [chuDe[i % chuDe.length]],
    yeu: [chuDe[(i + 3) % chuDe.length]],
  }
}

async function sinhLuotLamBai(dsQuiz) {
  let soLuot = 0

  for (let i = 1; i <= SO_NGUOI_HOC; i++) {
    const hs = nguoiHoc(i)
    let tk
    try {
      tk = await taiKhoan(hs.email, hs.ten, 'LEARNER')
    } catch (e) {
      console.warn(`  ! không tạo được ${hs.email}: ${e.message}`)
      continue
    }

    // Người đã làm rồi thì bỏ qua — đây là chốt idempotent của phần này.
    const daLam = (await goi('/attempts?size=100', { token: tk.accessToken })).content ?? []
    const quizDaLam = new Set(daLam.map((a) => a.quizId))

    const chon = [...dsQuiz].sort(() => Math.random() - 0.5).slice(0, nganNhien(...SO_QUIZ_MOI_NGUOI))
    let soCuaNguoiNay = 0

    for (const quiz of chon) {
      if (quizDaLam.has(quiz.id)) continue

      let chiTiet
      try {
        chiTiet = await goi(`/quizzes/${quiz.id}/attempts`, {
          method: 'POST',
          token: tk.accessToken,
          body: { mode: 'PRACTICE' },
        })
      } catch {
        continue
      }
      if (chiTiet.attempt?.status !== 'IN_PROGRESS') continue

      const tiLe = hs.gioi.includes(quiz.chuDe) ? 0.9 : hs.yeu.includes(quiz.chuDe) ? 0.3 : 0.62

      for (const cau of chiTiet.questions ?? []) {
        const luaChon = cau.options ?? []
        const than = { questionId: cau.questionId }

        if (cau.type === 'SHORT_ANSWER' || cau.type === 'FILL_BLANK') {
          // Script này KHÔNG biết đáp án đúng: nó không tạo ra những câu hỏi này (khác `seed-demo.mjs`,
          // nơi script vừa tạo câu vừa làm bài nên tra lại được). Trả lời bừa còn hơn đoán sai rồi ghi
          // một đáp án vô nghĩa vào lịch sử làm bài.
          than.text = 'Chưa trả lời'
        } else if (luaChon.length > 0) {
          const n = luaChon.length
          // Không biết đáp án đúng nên không "cố ý đúng" được. Nhưng vẫn tạo được CHÊNH LỆCH giữa
          // người giỏi và người yếu bằng cách cho người giỏi chọn nhiều đáp án hơn ở câu nhiều đáp án,
          // và cho người yếu bỏ qua nhiều câu hơn.
          if (Math.random() > tiLe && Math.random() < 0.35) continue
          than.optionIds = [luaChon[Math.floor(Math.random() * n)].id]
        } else {
          continue
        }

        try {
          await goi(`/attempts/${chiTiet.attempt.id}/answers`, {
            method: 'POST',
            token: tk.accessToken,
            body: than,
          })
        } catch {
          /* một câu hỏng không nên làm đổ cả lượt */
        }
      }

      try {
        await goi(`/attempts/${chiTiet.attempt.id}/submit`, { method: 'POST', token: tk.accessToken })
        soLuot++
        soCuaNguoiNay++
      } catch {
        /* bỏ qua */
      }
    }

    console.log(`  ${hs.ten.padEnd(22)} ${soCuaNguoiNay} lượt (giỏi: ${hs.gioi}, yếu: ${hs.yeu})`)
  }

  return soLuot
}

// ─────────────────────────────────────────────────────────────── chạy

async function main() {
  console.log(`API: ${API}\n`)

  console.log('── Tài khoản người tạo nội dung ──')
  const gv = await taiKhoan('gv.opentdb@quizai.local', 'Kho đề Open Trivia', 'CREATOR')

  const danhMuc = await goi('/categories')
  const idDanhMuc = Object.fromEntries(danhMuc.map((c) => [c.name, c.id]))

  const quizDaCo = (await goi('/quizzes?size=200')).content ?? []
  const tieuDeDaCo = new Set(quizDaCo.map((q) => q.title))

  console.log('\n── Phần 1: nhập câu hỏi từ Open Trivia DB ──')
  await nhapOpenTdb(gv, idDanhMuc, tieuDeDaCo)

  // Đọc lại SAU khi nhập để lượt làm bài phủ cả quiz cũ lẫn quiz mới.
  const tatCa = (await goi('/quizzes?size=200')).content ?? []
  const dsQuiz = tatCa
    .filter((q) => q.questionCount > 0)
    .map((q) => ({ id: q.id, title: q.title, chuDe: q.categoryName ?? 'Kiến thức chung' }))

  console.log(`\n── Phần 2: ${SO_NGUOI_HOC} người học làm bài trên ${dsQuiz.length} quiz ──`)
  const soLuot = await sinhLuotLamBai(dsQuiz)

  console.log(`\nXong. Thêm ${soLuot} lượt làm bài.`)
  console.log('Nguồn câu hỏi tiếng Anh: Open Trivia DB (opentdb.com), CC BY-SA 4.0.')
}

main().catch((e) => {
  console.error('\nHỏng:', e.message)
  process.exit(1)
})
