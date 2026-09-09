/**
 * Quét toàn bộ trang của web và thu lỗi thật lúc chạy.
 *
 * Chạy:  node scripts/kiem-tra-web.mjs
 * Yêu cầu: docker compose up -d · backend ở 8081 · frontend ở 5173 · đã chạy seed-demo.mjs
 *
 * ## Vì sao cần thứ này ngoài test
 * `npm test` và `./mvnw test` kiểm những gì đã nghĩ tới. Còn những lỗi kiểu *gọi API sai đường dẫn*,
 * *một trang trắng vì lỗi render*, *một endpoint trả 500 chỉ khi dữ liệu thật* thì không phép kiểm nào
 * bắt được — chúng chỉ lộ khi mở đúng trang đó bằng đúng vai trò đó. Bộ quét này làm việc mở trang.
 *
 * ## Thu ba loại tín hiệu
 * 1. `pageerror` — ngoại lệ JS chưa bắt. Nặng nhất: thường là màn hình trắng.
 * 2. `console` mức error/warning — React key trùng, cảnh báo API lỗi thời, lỗi mạng bị bắt rồi ghi log.
 * 3. Phản hồi HTTP >= 400 — API gọi sai, thiếu quyền, hoặc lỗi máy chủ.
 *
 * Lọc bỏ những thứ ồn mà không phải lỗi của mình (xem `BO_QUA`), vì một danh sách đầy tiếng ồn thì
 * không ai đọc, và lỗi thật nằm lẫn trong đó cũng như không có.
 */
import fs from 'node:fs'
import path from 'node:path'
import { createRequire } from 'node:module'

const require = createRequire(path.join(process.cwd(), 'bao-cao-datn/build/'))
const puppeteer = require('puppeteer')

const FE = process.env.FE_URL ?? 'http://localhost:5173'
const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe'
const KHUNG = { width: 1440, height: 900 }
const MAT_KHAU = 'MatKhau@123'

const VAI = {
  khach: null,
  hoc: { email: 'hs1.demo@quizai.local', matKhau: MAT_KHAU },
  tao: { email: 'gv.demo@quizai.local', matKhau: MAT_KHAU },
  quantri: { email: process.env.APP_ADMIN_EMAIL, matKhau: process.env.APP_ADMIN_PASSWORD },
}

/** Đường dẫn cần kiểm, kèm vai trò nào mở được. `{id}` được thay bằng id thật lấy từ API. */
const TRANG = [
  ['khach', '/login'],
  ['khach', '/register'],
  ['khach', '/forgot-password'],
  ['hoc', '/'],
  ['hoc', '/quizzes'],
  ['hoc', '/quizzes/{quiz}'],
  ['hoc', '/my-attempts'],
  ['hoc', '/my-progress'],
  ['hoc', '/learning-path'],
  ['hoc', '/flashcards'],
  ['hoc', '/flashcards/review'],
  ['hoc', '/achievements'],
  ['hoc', '/leaderboard'],
  ['hoc', '/classrooms'],
  ['hoc', '/my-assignments'],
  ['hoc', '/notifications'],
  ['hoc', '/notifications/settings'],
  ['hoc', '/assistant'],
  ['hoc', '/rooms'],
  ['hoc', '/profile'],
  ['hoc', '/ai/materials'],
  ['tao', '/my-quizzes'],
  ['tao', '/my-quizzes/{quiz}'],
  ['tao', '/my-quizzes/{quiz}/stats'],
  ['tao', '/question-bank'],
  ['tao', '/ai/materials'],
  ['tao', '/ai/generate'],
  ['tao', '/classrooms'],
  ['tao', '/classrooms/{lop}'],
  ['quantri', '/admin'],
  ['quantri', '/admin/users'],
  ['quantri', '/admin/categories'],
  ['quantri', '/admin/quizzes'],
  ['quantri', '/admin/rooms'],
  ['quantri', '/admin/integrity'],
  ['quantri', '/admin/ai'],
  ['quantri', '/admin/profile'],
  // Đường dẫn không tồn tại: phải ra trang 404 của ứng dụng, không phải màn trắng
  ['hoc', '/duong-dan-khong-ton-tai'],
]

/**
 * Tiếng ồn không phải lỗi của dự án. Giữ danh sách ngắn và ghi lý do từng dòng — một bộ lọc rộng quá
 * sẽ che mất đúng loại lỗi cần tìm.
 */
const BO_QUA = [
  /Download the React DevTools/, // lời mời cài extension
  /\[vite\] connect(ing|ed)/, // log của dev server
  /Not implemented: Window's getComputedStyle/, // jsdom, không xảy ra trên Chrome thật
  /favicon\.ico/, // thiếu favicon không phải lỗi chức năng
  /React Router Future Flag/, // cảnh báo chuẩn bị v8
]

const bien = { khach: null, hoc: null, tao: null, quantri: null }
const loi = []

const nghi = (ms) => new Promise((r) => setTimeout(r, ms))
const boQua = (s) => BO_QUA.some((r) => r.test(s))

function ghi(vai, duongDan, loai, chiTiet) {
  if (boQua(chiTiet)) return
  loi.push({ vai, duongDan, loai, chiTiet: chiTiet.slice(0, 300) })
}

/** Gắn ba bộ thu vào một trang. Trả về hàm đổi đường dẫn đang theo dõi. */
function theoDoi(page, vai) {
  let hienTai = '?'
  page.on('pageerror', (e) => ghi(vai, hienTai, 'NGOẠI LỆ JS', e.message))
  page.on('console', (m) => {
    const t = m.type()
    if (t === 'error' || t === 'warning') ghi(vai, hienTai, t === 'error' ? 'CONSOLE ERROR' : 'CONSOLE WARN', m.text())
  })
  page.on('response', (r) => {
    const s = r.status()
    if (s >= 400) ghi(vai, hienTai, `HTTP ${s}`, `${r.request().method()} ${r.url().replace(FE, '')}`)
  })
  return (d) => {
    hienTai = d
  }
}

async function trang(browser, vai) {
  const ctx = vai === 'khach' ? browser : await browser.createBrowserContext()
  const page = await ctx.newPage()
  await page.setViewport(KHUNG)
  await page.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: 'light' }])
  await page.evaluateOnNewDocument(() => {
    try {
      localStorage.setItem('quizai-theme', 'light')
    } catch {
      /* bỏ qua */
    }
  })
  const dat = theoDoi(page, vai)

  if (vai !== 'khach') {
    const tk = VAI[vai]
    if (!tk?.email || !tk?.matKhau) throw new Error(`thiếu tài khoản cho vai trò ${vai}`)
    dat('/login (đăng nhập)')
    await page.goto(`${FE}/login`, { waitUntil: 'networkidle2' })
    await page.type('input[name="email"]', tk.email)
    await page.type('input[name="password"]', tk.matKhau)
    await page.click('button[type="submit"]')
    await page.waitForFunction(() => !location.pathname.startsWith('/login'), { timeout: 20000 })
    await nghi(1000)
  }
  return { page, dat }
}

/** Lấy id thật để thay vào `{quiz}` và `{lop}` — kiểm trang chi tiết mà không gắn cứng UUID. */
async function layId() {
  const API = process.env.API ?? 'http://localhost:8081/api/v1'
  const dn = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: VAI.tao.email, password: MAT_KHAU }),
  }).then((r) => r.json())

  const h = { Authorization: 'Bearer ' + dn.accessToken }
  // `GET /quizzes` mở cho khách, nên quiz của mình lấy bằng cờ `mine=true` chứ không phải /quizzes/mine
  const quiz = await fetch(`${API}/quizzes?mine=true&size=1`, { headers: h }).then((r) => r.json())
  const lop = await fetch(`${API}/classrooms`, { headers: h }).then((r) => r.json())

  return {
    quiz: quiz?.content?.[0]?.id ?? null,
    lop: (Array.isArray(lop) ? lop : (lop?.content ?? []))?.[0]?.id ?? null,
  }
}

async function main() {
  const id = await layId()
  console.log(`id thay vào: quiz=${id.quiz ?? 'KHÔNG CÓ'} lop=${id.lop ?? 'KHÔNG CÓ'}\n`)

  const browser = await puppeteer.launch({ executablePath: CHROME, headless: 'new', args: ['--no-sandbox'] })
  const phien = {}

  let soTrang = 0
  for (const [vai, mau] of TRANG) {
    const duongDan = mau.replace(/\{(\w+)\}/g, (_, k) => id[k] ?? '')
    if (mau.includes('{') && /\/$|\/\//.test(duongDan)) {
      console.log(`— bỏ qua ${mau} (không có id để thay)`)
      continue
    }

    if (!phien[vai]) phien[vai] = await trang(browser, vai)
    const { page, dat } = phien[vai]

    dat(`[${vai}] ${duongDan}`)
    const truoc = loi.length
    try {
      await page.goto(FE + duongDan, { waitUntil: 'networkidle2', timeout: 30000 })
      await nghi(1800)
      // Trang trắng: có body nhưng không có chữ nào — dấu hiệu render đổ mà không ném lỗi
      const chu = await page.evaluate(() => document.body?.innerText?.trim().length ?? 0)
      if (chu < 20) ghi(vai, `[${vai}] ${duongDan}`, 'TRANG TRẮNG', `body chỉ có ${chu} ký tự`)
    } catch (e) {
      ghi(vai, `[${vai}] ${duongDan}`, 'KHÔNG TẢI ĐƯỢC', e.message)
    }
    soTrang++
    const moi = loi.length - truoc
    console.log(`${moi === 0 ? '✓' : '✗'} [${vai}] ${duongDan}${moi ? `  — ${moi} vấn đề` : ''}`)
  }

  await browser.close()

  console.log(`\n${'─'.repeat(70)}`)
  console.log(`Đã mở ${soTrang} trang · ${loi.length} vấn đề\n`)

  const theoLoai = {}
  for (const l of loi) (theoLoai[l.loai] ??= []).push(l)

  for (const [loai, ds] of Object.entries(theoLoai).sort((a, b) => b[1].length - a[1].length)) {
    console.log(`\n### ${loai} — ${ds.length}`)
    const gom = {}
    for (const l of ds) (gom[l.chiTiet] ??= []).push(l.duongDan)
    for (const [ct, dss] of Object.entries(gom).sort((a, b) => b[1].length - a[1].length)) {
      console.log(`  ${dss.length}× ${ct}`)
      if (dss.length <= 4) for (const d of dss) console.log(`        ${d}`)
      else console.log(`        ${dss.slice(0, 3).join(' · ')} … và ${dss.length - 3} trang khác`)
    }
  }

  const tep = path.join(process.cwd(), 'bao-cao-datn', 'kiem-tra-web.json')
  fs.writeFileSync(tep, JSON.stringify(loi, null, 2), 'utf8')
  console.log(`\nChi tiết đầy đủ: ${tep}`)
}

main().catch((e) => {
  console.error('\nHỏng:', e.message)
  process.exit(1)
})
