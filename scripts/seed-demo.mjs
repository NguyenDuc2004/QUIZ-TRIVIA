/**
 * Seed dữ liệu mẫu cho môi trường dev/demo.
 *
 * Chạy:  node scripts/seed-demo.mjs
 * Yêu cầu: backend đang chạy ở http://localhost:8080 (đổi bằng biến môi trường API).
 *
 * ## Vì sao đi qua API chứ không ghi thẳng vào cơ sở dữ liệu
 * Ghi thẳng SQL nhanh hơn, nhưng nó **bỏ qua toàn bộ tầng nghiệp vụ**: băm mật khẩu, kiểm hợp lệ câu hỏi
 * theo từng loại, chốt đề lúc bắt đầu làm bài, phát sự kiện cộng điểm kinh nghiệm, đồng bộ sang Neo4j. Dữ
 * liệu sinh ra khi đó *trông giống* dữ liệu thật nhưng thiếu đúng những thứ làm nó thật — và lỗi chỉ lộ ra
 * khi mở giao diện lên thấy trống trơn. Đi qua API thì mọi thứ đúng như người dùng bấm tay.
 *
 * ## Chạy lại nhiều lần không nhân đôi
 * Tài khoản đã tồn tại thì đăng nhập lại thay vì tạo mới; quiz trùng tiêu đề thì bỏ qua. Nhờ vậy chạy lại
 * sau mỗi lần đổi schema là an toàn.
 *
 * ## Đây là dữ liệu DEMO, không phải dữ liệu thật
 * Mật khẩu để trong file này là chủ ý — chúng dùng cho tài khoản demo trên máy dev, và người chạy script
 * cần biết để đăng nhập thử. Không dùng script này với cơ sở dữ liệu thật.
 */

const API = process.env.API ?? 'http://localhost:8080/api/v1'
const MAT_KHAU = 'MatKhau@123'

// ─────────────────────────────────────────────────────────────── tiện ích gọi API

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
    e.body = dl
    throw e
  }
  return dl
}

/** Đăng ký, hoặc đăng nhập nếu email đã tồn tại. Đây là chốt idempotent của toàn bộ script. */
async function taiKhoan(email, displayName, role) {
  try {
    const r = await goi('/auth/register', {
      method: 'POST',
      body: { email, password: MAT_KHAU, displayName, role },
    })
    console.log(`  + tạo ${role.padEnd(7)} ${email}`)
    return { ...r, moi: true }
  } catch (e) {
    if (e.status !== 409) throw e
    const r = await goi('/auth/login', { method: 'POST', body: { email, password: MAT_KHAU } })
    console.log(`  = đã có ${role.padEnd(6)} ${email}`)
    return { ...r, moi: false }
  }
}

const chon = (mang, i) => mang[i % mang.length]

// ─────────────────────────────────────────────────────────────── nội dung mẫu

/**
 * Năm loại câu hỏi đều có mặt — đây là điều kiện để chụp được màn làm bài đầy đủ và để thử chức năng
 * chấm tự luận, vốn chỉ áp cho SHORT_ANSWER.
 */
function boCauHoi(chuDe) {
  const kho = {
    'Tin học': [
      {
        type: 'SINGLE_CHOICE',
        content: 'Mã trạng thái HTTP nào cho biết máy chủ không tìm thấy tài nguyên?',
        explanation: '404 Not Found — máy chủ hiểu yêu cầu nhưng không có tài nguyên tương ứng.',
        difficulty: 'EASY',
        options: [
          { content: '200 OK', correct: false },
          { content: '404 Not Found', correct: true },
          { content: '500 Internal Server Error', correct: false },
          { content: '302 Found', correct: false },
        ],
      },
      {
        type: 'MULTIPLE_CHOICE',
        content: 'Những thành phần nào sau đây thuộc kiến trúc của một ứng dụng web ba lớp?',
        explanation: 'Ba lớp gồm trình bày, nghiệp vụ và dữ liệu.',
        difficulty: 'MEDIUM',
        options: [
          { content: 'Lớp trình bày (giao diện)', correct: true },
          { content: 'Lớp nghiệp vụ', correct: true },
          { content: 'Lớp dữ liệu', correct: true },
          { content: 'Lớp biên dịch', correct: false },
        ],
      },
      {
        type: 'TRUE_FALSE',
        content: 'Giao thức HTTP là giao thức có trạng thái (stateful).',
        explanation: 'Sai — HTTP không trạng thái; trạng thái phiên phải tự quản lý bằng cookie hoặc token.',
        difficulty: 'EASY',
        options: [
          { content: 'Đúng', correct: false },
          { content: 'Sai', correct: true },
        ],
      },
      {
        type: 'FILL_BLANK',
        content: 'Cấu trúc dữ liệu hoạt động theo nguyên tắc vào sau ra trước gọi là ___.',
        explanation: 'Ngăn xếp (stack) — Last In First Out.',
        difficulty: 'MEDIUM',
        options: [
          { content: 'ngăn xếp', correct: true },
          { content: 'stack', correct: true },
        ],
      },
      {
        type: 'SHORT_ANSWER',
        content: 'Nêu ba nguyên nhân chính khiến một ứng dụng web chạy chậm.',
        explanation: 'Truy vấn cơ sở dữ liệu chưa tối ưu, tài nguyên tĩnh nặng, thiếu bộ đệm.',
        rubric: 'Mỗi nguyên nhân đúng 3 điểm; diễn đạt rõ ràng mạch lạc thêm 1 điểm. Tối đa 10 điểm.',
        difficulty: 'HARD',
        points: 10,
        options: [
          {
            content: 'Truy vấn cơ sở dữ liệu chưa tối ưu; tài nguyên tĩnh quá nặng; thiếu cơ chế bộ đệm.',
            correct: true,
          },
        ],
      },
    ],
    'Toán học': [
      {
        type: 'SINGLE_CHOICE',
        content: 'Đạo hàm của hàm số f(x) = x² là gì?',
        explanation: "Áp dụng công thức (xⁿ)' = n·xⁿ⁻¹.",
        difficulty: 'EASY',
        options: [
          { content: '2x', correct: true },
          { content: 'x', correct: false },
          { content: 'x³/3', correct: false },
          { content: '2', correct: false },
        ],
      },
      {
        type: 'TRUE_FALSE',
        content: 'Mọi hàm số liên tục trên một khoảng đều có đạo hàm trên khoảng đó.',
        explanation: 'Sai — hàm |x| liên tục tại 0 nhưng không có đạo hàm tại đó.',
        difficulty: 'MEDIUM',
        options: [
          { content: 'Đúng', correct: false },
          { content: 'Sai', correct: true },
        ],
      },
      {
        type: 'FILL_BLANK',
        content: 'Nghiệm của phương trình 2x + 6 = 0 là x = ___.',
        difficulty: 'EASY',
        options: [{ content: '-3', correct: true }],
      },
      {
        type: 'SHORT_ANSWER',
        content: 'Giải thích ý nghĩa hình học của đạo hàm tại một điểm.',
        explanation: 'Là hệ số góc của tiếp tuyến với đồ thị hàm số tại điểm đó.',
        rubric: 'Nêu đúng "hệ số góc của tiếp tuyến" 6 điểm; giải thích thêm về tốc độ biến thiên 4 điểm.',
        difficulty: 'HARD',
        points: 10,
        options: [
          {
            content: 'Đạo hàm tại một điểm là hệ số góc của tiếp tuyến với đồ thị tại điểm đó, đồng thời cho biết tốc độ biến thiên tức thời của hàm số.',
            correct: true,
          },
        ],
      },
    ],
    'Tiếng Anh': [
      {
        type: 'SINGLE_CHOICE',
        content: 'Chọn dạng bị động đúng của câu: "They build houses."',
        difficulty: 'EASY',
        options: [
          { content: 'Houses are built.', correct: true },
          { content: 'Houses is built.', correct: false },
          { content: 'Houses were build.', correct: false },
          { content: 'Houses are building.', correct: false },
        ],
      },
      {
        type: 'MULTIPLE_CHOICE',
        content: 'Những thì nào sau đây dùng trợ động từ "have"?',
        difficulty: 'MEDIUM',
        options: [
          { content: 'Present perfect', correct: true },
          { content: 'Past perfect', correct: true },
          { content: 'Simple past', correct: false },
          { content: 'Present continuous', correct: false },
        ],
      },
      {
        type: 'FILL_BLANK',
        content: 'She ___ to school every day. (go)',
        difficulty: 'EASY',
        options: [{ content: 'goes', correct: true }],
      },
    ],
    'Lịch sử': [
      {
        type: 'SINGLE_CHOICE',
        content: 'Chiến thắng Điện Biên Phủ diễn ra vào năm nào?',
        difficulty: 'EASY',
        options: [
          { content: '1954', correct: true },
          { content: '1945', correct: false },
          { content: '1975', correct: false },
          { content: '1968', correct: false },
        ],
      },
      {
        type: 'TRUE_FALSE',
        content: 'Nhà Lý dời đô về Thăng Long năm 1010.',
        difficulty: 'EASY',
        options: [
          { content: 'Đúng', correct: true },
          { content: 'Sai', correct: false },
        ],
      },
      {
        type: 'FILL_BLANK',
        content: 'Bản Tuyên ngôn Độc lập được đọc tại quảng trường ___.',
        difficulty: 'MEDIUM',
        options: [{ content: 'Ba Đình', correct: true }],
      },
    ],
  }
  return kho[chuDe] ?? []
}

/** Quiz mẫu. Quiz cuối bật chế độ thi nghiêm ngặt để thử FR-48 và phần minh bạch chống gian lận. */
const QUIZ = [
  {
    title: 'Nhập môn Web — HTTP và kiến trúc',
    description: 'Kiểm tra kiến thức nền về giao thức HTTP, kiến trúc ứng dụng web và cấu trúc dữ liệu.',
    chuDe: 'Tin học',
    danhMuc: 'Tin học',
    difficulty: 'MEDIUM',
    timeLimitSec: 900,
  },
  {
    title: 'Giải tích — Đạo hàm cơ bản',
    description: 'Ôn tập đạo hàm của hàm một biến và ý nghĩa hình học.',
    chuDe: 'Toán học',
    danhMuc: 'Toán học',
    difficulty: 'MEDIUM',
    timeLimitSec: 600,
  },
  {
    title: 'Tiếng Anh — Câu bị động và các thì',
    description: 'Bài luyện ngữ pháp cơ bản.',
    chuDe: 'Tiếng Anh',
    danhMuc: 'Tiếng Anh',
    difficulty: 'EASY',
    timeLimitSec: 480,
  },
  {
    title: 'Lịch sử Việt Nam — Các mốc lớn',
    description: 'Những sự kiện tiêu biểu trong lịch sử hiện đại.',
    chuDe: 'Lịch sử',
    danhMuc: 'Lịch sử',
    difficulty: 'EASY',
    timeLimitSec: 480,
  },
  {
    title: 'Kiểm tra giữa kỳ — Nhập môn Web (thi nghiêm ngặt)',
    description:
      'Bài kiểm tra tính điểm. Bài này bật chế độ thi nghiêm ngặt: yêu cầu toàn màn hình và ghi nhận tín hiệu rời trang.',
    chuDe: 'Tin học',
    danhMuc: 'Tin học',
    difficulty: 'HARD',
    timeLimitSec: 1200,
    strictExam: true,
  },
]

/**
 * Người học có thế mạnh khác nhau theo chủ đề.
 *
 * Đây là điểm quan trọng nhất của phần sinh hành vi: nếu ai cũng làm đúng như nhau thì đồ thị Neo4j không
 * có gì để phân biệt, và mọi gợi ý trả về cùng một danh sách — trông như tính năng hỏng. Mỗi người dưới đây
 * giỏi một mảng và yếu mảng khác, nên "chủ đề còn yếu" và "người học tương tự" mới có nghĩa.
 */
const NGUOI_HOC = [
  { ten: 'Nguyễn Thu Hà', gioi: ['Tin học'], yeu: ['Toán học'] },
  { ten: 'Trần Minh Khôi', gioi: ['Toán học'], yeu: ['Tiếng Anh'] },
  { ten: 'Lê Bảo Anh', gioi: ['Tiếng Anh'], yeu: ['Lịch sử'] },
  { ten: 'Phạm Quốc Huy', gioi: ['Lịch sử'], yeu: ['Tin học'] },
  { ten: 'Đỗ Thanh Mai', gioi: ['Tin học', 'Toán học'], yeu: ['Tiếng Anh'] },
  { ten: 'Vũ Hải Nam', gioi: ['Tiếng Anh', 'Lịch sử'], yeu: ['Toán học'] },
  { ten: 'Bùi Khánh Linh', gioi: ['Toán học'], yeu: ['Tin học'] },
  { ten: 'Hoàng Gia Bảo', gioi: [], yeu: [] },
]

// ─────────────────────────────────────────────────────────────── các bước seed

async function main() {
  console.log(`Seed dữ liệu demo → ${API}\n`)

  try {
    await goi('/categories')
  } catch {
    console.error('Không gọi được backend. Kiểm tra backend đã chạy ở ' + API + ' chưa.')
    process.exit(1)
  }

  // ── 1. tài khoản
  console.log('1. Tài khoản')
  const gv = await taiKhoan('gv.demo@quizai.local', 'Cô Nguyễn Thị Lan', 'CREATOR')
  const hocSinh = []
  for (const [i, nh] of NGUOI_HOC.entries()) {
    hocSinh.push({
      ...(await taiKhoan(`hs${i + 1}.demo@quizai.local`, nh.ten, 'LEARNER')),
      ...nh,
    })
  }

  // ── 2. danh mục có sẵn
  const danhMuc = await goi('/categories')
  const idDanhMuc = Object.fromEntries(danhMuc.map((c) => [c.name, c.id]))

  // ── 3. quiz + câu hỏi
  console.log('\n2. Quiz và câu hỏi')
  const quizDaCo = (await goi('/quizzes?mine=true&size=100', { token: gv.accessToken })).content ?? []
  const theoTieuDe = Object.fromEntries(quizDaCo.map((q) => [q.title, q]))
  const quizIds = []

  // questionId → đáp án đúng. Đề trả cho người LÀM BÀI cố ý không kèm đáp án, nên script phải tự nhớ từ
  // lúc tạo. Với câu trắc nghiệm là danh sách id phương án đúng; với câu điền khuyết là chuỗi.
  const dapAnDung = {}

  for (const mau of QUIZ) {
    if (theoTieuDe[mau.title]) {
      console.log(`  = đã có  ${mau.title}`)
      // Quiz từ lần chạy trước: ĐỌC LẠI đáp án qua endpoint dành cho chủ sở hữu thay vì bỏ qua.
      //
      // Bản đầu của script bỏ qua luôn phần sinh lượt làm bài cho những quiz này, và hệ quả là chạy lần
      // thứ hai KHÔNG BAO GIỜ sinh được lượt nào — đúng thứ cần nhất lại là thứ không bao giờ chạy. Trả
      // lời bừa thì làm sai dữ liệu năng lực theo chủ đề, nhưng đọc lại đáp án thì không phải đoán gì.
      const q = theoTieuDe[mau.title]
      // Phản hồi có dạng { quiz: {...}, questions: [...] }, không phải một mảng trần.
      const chiTietQuiz = await goi(`/quizzes/${q.id}/questions`, { token: gv.accessToken })
      for (const ch of chiTietQuiz.questions ?? []) {
        dapAnDung[ch.id] =
          ch.type === 'FILL_BLANK' || ch.type === 'SHORT_ANSWER'
            ? (ch.options?.find((o) => o.correct)?.content ?? '')
            : (ch.options ?? []).filter((o) => o.correct).map((o) => o.id)
      }
      quizIds.push({ id: q.id, ...mau })
      continue
    }

    const quiz = await goi('/quizzes', {
      method: 'POST',
      token: gv.accessToken,
      body: {
        title: mau.title,
        description: mau.description,
        categoryId: idDanhMuc[mau.danhMuc] ?? null,
        difficulty: mau.difficulty,
        visibility: 'PUBLIC',
        timeLimitSec: mau.timeLimitSec,
        strictExam: mau.strictExam ?? false,
      },
    })

    const cauHoiIds = []
    for (const ch of boCauHoi(mau.chuDe)) {
      const tao = await goi('/questions', {
        method: 'POST',
        token: gv.accessToken,
        body: { ...ch, topic: mau.chuDe, points: ch.points ?? 1 },
      })
      cauHoiIds.push(tao.id)

      dapAnDung[tao.id] =
        ch.type === 'FILL_BLANK' || ch.type === 'SHORT_ANSWER'
          ? (ch.options.find((o) => o.correct)?.content ?? '')
          : (tao.options ?? []).filter((o) => o.correct).map((o) => o.id)
    }

    await goi(`/quizzes/${quiz.id}/questions`, {
      method: 'PUT',
      token: gv.accessToken,
      body: { questionIds: cauHoiIds },
    })

    console.log(`  + tạo    ${mau.title} (${cauHoiIds.length} câu)`)
    quizIds.push({ id: quiz.id, ...mau })
  }

  // ── 4. lớp học và bài tập
  console.log('\n3. Lớp học và bài tập')
  const lopDaCo = await goi('/classrooms', { token: gv.accessToken })
  let lop = (lopDaCo.content ?? lopDaCo).find?.((l) => l.name === 'Lớp 12A1 — Tin học')

  if (!lop) {
    lop = await goi('/classrooms', {
      method: 'POST',
      token: gv.accessToken,
      body: { name: 'Lớp 12A1 — Tin học', description: 'Lớp demo dùng để thử chức năng giao bài.' },
    })
    console.log(`  + tạo lớp — mã lớp ${lop.classCode}`)
  } else {
    console.log(`  = đã có lớp — mã lớp ${lop.classCode}`)
  }

  for (const hs of hocSinh.slice(0, 5)) {
    try {
      await goi(`/classrooms/join/${lop.classCode}`, { method: 'POST', token: hs.accessToken })
    } catch (e) {
      if (e.status !== 409) throw e
    }
  }
  console.log('  = 5 học viên trong lớp')

  const baiDaCo = await goi(`/classrooms/${lop.id}/assignments`, { token: gv.accessToken })
  if ((baiDaCo.content ?? baiDaCo).length === 0) {
    const quizThi = quizIds.find((q) => q.strictExam) ?? quizIds[0]
    const hanNop = new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString()
    await goi(`/classrooms/${lop.id}/assignments`, {
      method: 'POST',
      token: gv.accessToken,
      body: {
        quizId: quizThi.id,
        title: 'Kiểm tra giữa kỳ — nộp trước cuối tuần',
        instruction: 'Làm bài trong một lượt, không mở tài liệu.',
        dueAt: hanNop,
      },
    })
    console.log('  + giao 1 bài tập, hạn 7 ngày')
  } else {
    console.log('  = đã có bài tập')
  }

  // ── 5. lượt làm bài
  console.log('\n4. Lượt làm bài (cho bảng xếp hạng, thống kê và đồ thị gợi ý)')
  let soLuot = 0

  for (const hs of hocSinh) {
    for (const quiz of quizIds.filter((q) => !q.strictExam)) {
      // Không phải ai cũng làm mọi quiz — dữ liệu đồng nhất làm gợi ý "người học tương tự" vô nghĩa
      if (!hs.gioi.includes(quiz.chuDe) && !hs.yeu.includes(quiz.chuDe) && Math.random() < 0.4) {
        continue
      }

      // Phản hồi có dạng { attempt: {...}, questions: [...] } — trạng thái nằm ở lớp trong.
      let chiTiet
      try {
        chiTiet = await goi(`/quizzes/${quiz.id}/attempts`, {
          method: 'POST',
          token: hs.accessToken,
          body: { mode: 'EXAM' },
        })
      } catch (e) {
        console.warn(`    ! ${hs.ten} không bắt đầu được "${quiz.title}": ${e.message}`)
        continue
      }
      if (chiTiet.attempt?.status !== 'IN_PROGRESS') continue
      const attemptId = chiTiet.attempt.id

      const tiLeDung = hs.gioi.includes(quiz.chuDe) ? 0.9 : hs.yeu.includes(quiz.chuDe) ? 0.3 : 0.6

      for (const cau of chiTiet.questions ?? []) {
        const dung = Math.random() < tiLeDung
        const luaChon = cau.options ?? []

        // Thân request là PHẲNG: { questionId, optionIds, text } — không bọc trong `answer`.
        const than = { questionId: cau.questionId }

        if (cau.type === 'SHORT_ANSWER') {
          than.text = dung
            ? 'Truy vấn cơ sở dữ liệu chưa tối ưu, tài nguyên tĩnh quá nặng, và thiếu cơ chế bộ đệm.'
            : 'Em không nhớ rõ.'
        } else if (cau.type === 'FILL_BLANK') {
          than.text = dung ? (dapAnDung[cau.questionId] ?? 'ngăn xếp') : 'không biết'
        } else if (luaChon.length > 0) {
          // Đề trả cho người làm KHÔNG kèm đáp án đúng — đúng như thiết kế chốt đề. Script biết đáp án vì
          // chính nó vừa tạo câu hỏi, nên tra lại từ bảng dựng lúc tạo thay vì đoán.
          const dungIds = dapAnDung[cau.questionId] ?? []
          const sai = luaChon.filter((o) => !dungIds.includes(o.id))
          than.optionIds =
            dung && dungIds.length > 0
              ? dungIds
              : [chon(sai.length > 0 ? sai : luaChon, Math.floor(Math.random() * 4)).id]
        } else {
          continue
        }

        try {
          await goi(`/attempts/${attemptId}/answers`, {
            method: 'POST',
            token: hs.accessToken,
            body: than,
          })
        } catch (e) {
          console.warn(`    ! trả lời hỏng: ${e.message}`)
        }
      }

      try {
        await goi(`/attempts/${attemptId}/submit`, { method: 'POST', token: hs.accessToken })
        soLuot++
      } catch (e) {
        console.warn(`    ! nộp bài hỏng: ${e.message}`)
      }
    }
  }
  console.log(`  + ${soLuot} lượt làm bài đã nộp`)

  // ── xong
  console.log('\n─────────────────────────────────────────────')
  console.log('XONG. Đăng nhập thử:')
  console.log(`  Giáo viên : gv.demo@quizai.local  / ${MAT_KHAU}`)
  console.log(`  Học viên  : hs1.demo@quizai.local / ${MAT_KHAU}`)
  console.log(`  Mã vào lớp: ${lop.classCode}`)
  console.log('\nQuiz "Kiểm tra giữa kỳ — Nhập môn Web" bật chế độ thi nghiêm ngặt,')
  console.log('dùng nó để thử phần minh bạch chống gian lận.')
}

main().catch((e) => {
  console.error('\nSeed hỏng:', e.message)
  process.exit(1)
})
