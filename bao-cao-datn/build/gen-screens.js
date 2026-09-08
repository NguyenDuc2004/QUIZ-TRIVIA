/* Chụp ảnh màn hình sản phẩm -> assets/hinh-3.2.png .. hinh-3.16.png
 *
 * Chạy:  cd bao-cao-datn/build && node gen-screens.js
 *        node gen-screens.js 3.13 3.14      # chỉ chụp lại vài hình
 *
 * ## Điều kiện
 * 1. `docker compose up -d` — ba CSDL đang chạy.
 * 2. Backend đang chạy. Cổng đọc từ BE (mặc định 8081: cổng 8080 hay bị dịch vụ khác của Windows
 *    chiếm, nên `frontend/.env` của dự án đã trỏ sang 8081 — xem chú thích trong `vite.config.ts`).
 * 3. Frontend đang chạy ở 5173.
 * 4. Đã chạy `node scripts/seed-demo.mjs` để có gv.demo / hs1.demo.
 *
 * ## Vì sao dùng puppeteer + Chrome của máy chứ không Playwright
 * `gen-mockup.js` đã đi đường này và có sẵn trong `build/node_modules`. Thêm Playwright là tải thêm
 * một bộ trình duyệt vài trăm MB để làm đúng việc mà thứ đang có làm được.
 *
 * ## Mỗi hình một `try/catch` riêng
 * Một màn hỏng không được kéo theo mười bốn màn còn lại — chạy lại cả lượt vì một lỗi là phí thời
 * gian, mà lượt chạy này phải đăng nhập ba vai trò. Cuối lượt in bảng tổng kết: hình nào xong, hình
 * nào trượt và vì sao.
 *
 * ## Ảnh nào script không chụp thay người được
 * Xem hằng `TU_CHUP_TAY` ở cuối tệp. Những màn cần hai người chơi thật hoặc cần bấm đúng nhịp thì
 * script chỉ dựng được khung rỗng — mà một khung rỗng nộp vào báo cáo còn tệ hơn là không có ảnh,
 * vì nó trông như tính năng chưa làm xong.
 */
const fs = require("fs");
const path = require("path");
const puppeteer = require("puppeteer");

const FE = process.env.FE_URL || "http://localhost:5173";
const CHROME = "C:/Program Files/Google/Chrome/Application/chrome.exe";
const ASSETS = path.join(__dirname, "..", "assets");
const KHUNG = { width: 1440, height: 900 };

/* Khoá lưu chế độ màu — xem `frontend/src/shared/theme/themeStore.ts`. Mặc định là `system`, mà Chrome
 * chạy ẩn ở máy này báo về `dark`, nên lượt chụp đầu ra 12 ảnh nền đen. Báo cáo in trên giấy trắng thì
 * ảnh nền đen vừa tốn mực vừa khó đọc. Đặt thẳng `light` và ép luôn `prefers-color-scheme` phòng khi
 * store chưa nạp kịp. */
const KHOA_THEME = "quizai-theme";

/** Đặt chế độ sáng TRƯỚC khi trang chạy dòng script đầu tiên — `main.tsx` đọc localStorage ngay ở cấp
 * module để áp `data-theme` trước lần render đầu, nên đặt sau khi tải là đã muộn. */
async function epSang(page) {
  await page.emulateMediaFeatures([{ name: "prefers-color-scheme", value: "light" }]);
  await page.evaluateOnNewDocument((khoa) => {
    try {
      localStorage.setItem(khoa, "light");
    } catch {
      /* chế độ riêng tư — bỏ qua, đã có emulateMediaFeatures đỡ */
    }
  }, KHOA_THEME);
}

const MAT_KHAU = "MatKhau@123";
const GV = { email: "gv.demo@quizai.local", matKhau: MAT_KHAU };
const HS = { email: "hs1.demo@quizai.local", matKhau: MAT_KHAU };
/* Admin dựng từ APP_ADMIN_EMAIL/APP_ADMIN_PASSWORD trong .env (xem AdminBootstrap) */
const QT = { email: process.env.APP_ADMIN_EMAIL, matKhau: process.env.APP_ADMIN_PASSWORD };

const chon = process.argv.slice(2);
const canChup = (so) => chon.length === 0 || chon.includes(so);

const xong = [];
const truot = [];
const nghi = (ms) => new Promise((r) => setTimeout(r, ms));

async function chup(page, so, { toanTrang = false } = {}) {
  const ten = `hinh-${so}.png`;
  const tep = path.join(ASSETS, ten);
  await page.screenshot({ path: tep, fullPage: toanTrang });
  console.log(`  ✓ ${ten}  (${fs.statSync(tep).size} bytes)`);
  xong.push(so);
}

async function man(so, mo, fn) {
  if (!canChup(so)) return;
  console.log(`\n▸ ${so} — ${mo}`);
  try {
    await fn();
  } catch (e) {
    console.error(`  ✗ ${e.message}`);
    truot.push({ so, mo, loi: e.message });
  }
}

/** Đăng nhập rồi trả về page đã vào trong. Ném lỗi nếu vẫn còn ở /login sau khi bấm. */
async function dangNhap(browser, tk, nhan) {
  if (!tk.email || !tk.matKhau) throw new Error(`thiếu tài khoản ${nhan} (đặt APP_ADMIN_EMAIL/APP_ADMIN_PASSWORD)`);
  const ctx = await browser.createBrowserContext();
  const page = await ctx.newPage();
  await page.setViewport(KHUNG);
  await epSang(page);
  await page.goto(`${FE}/login`, { waitUntil: "networkidle2" });
  await page.type('input[name="email"]', tk.email);
  await page.type('input[name="password"]', tk.matKhau);
  await page.click('button[type="submit"]');
  await page.waitForFunction(() => !location.pathname.startsWith("/login"), { timeout: 20000 });
  await nghi(1200);
  return page;
}

/** Điều hướng rồi chờ mạng lặng — dữ liệu tải xong mới chụp, không chụp trúng khung xương. */
async function toi(page, duongDan, chờ = 1500) {
  await page.goto(FE + duongDan, { waitUntil: "networkidle2", timeout: 30000 });
  await nghi(chờ);
}

async function main() {
  fs.mkdirSync(ASSETS, { recursive: true });
  const browser = await puppeteer.launch({ executablePath: CHROME, headless: "new", args: ["--no-sandbox"] });

  // ── Khách chưa đăng nhập ───────────────────────────────────────────────
  await man("3.2", "Đăng nhập", async () => {
    const page = await browser.newPage();
    await page.setViewport(KHUNG);
    await epSang(page);
    await toi(page, "/login");
    await chup(page, "3.2");
    await page.close();
  });

  // ── Người học ──────────────────────────────────────────────────────────
  const hs = await dangNhap(browser, HS, "người học");

  /* `/quizzes` nằm trong khu vực bảo vệ: khách vào bị đẩy về `/login`. Lượt chụp đầu vì thế cho ra
   * một ảnh màn đăng nhập thứ hai mà vẫn báo thành công — đúng bytes bằng nhau mới lộ. Chụp bằng
   * phiên người học. */
  await man("3.3", "Khám phá quiz", async () => {
    await toi(hs, "/quizzes", 2500);
    await chup(hs, "3.3");
  });

  await man("3.9", "Gợi ý và lộ trình học", async () => {
    await toi(hs, "/learning-path", 2500);
    await chup(hs, "3.9");
  });

  await man("3.10", "Thẻ ghi nhớ", async () => {
    await toi(hs, "/flashcards", 2000);
    await chup(hs, "3.10");
  });

  await man("3.12", "Thành tích và xếp hạng mùa", async () => {
    await toi(hs, "/achievements", 2000);
    await chup(hs, "3.12");
  });

  // ── Người tạo nội dung ─────────────────────────────────────────────────
  const gv = await dangNhap(browser, GV, "người tạo nội dung");

  await man("3.7", "Học liệu và sinh đề AI", async () => {
    await toi(gv, "/ai/materials", 2500);
    await chup(gv, "3.7");
  });

  /* Hỏi thật một câu chứ không chụp khung rỗng: chú thích hình hứa có "khối trích dẫn nguồn dưới câu
   * trả lời", mà khối đó chỉ xuất hiện sau khi mô hình trả lời xong. Tốn một lượt gọi AI. */
  await man("3.8", "Trợ lý học tập", async () => {
    await toi(gv, "/assistant", 2500);
    await gv.type(".chat-composer textarea", "Dạng chuẩn 3NF khác 2NF ở điểm nào?");
    await gv.keyboard.press("Enter");
    // Ô nhập bị khoá suốt lúc mô hình đang trả lời; mở lại là dấu hiệu đã xong.
    await gv.waitForFunction(() => {
      const o = document.querySelector(".chat-composer textarea");
      return o && !o.disabled && document.querySelectorAll(".chat-composer").length > 0;
    }, { timeout: 90000, polling: 500 });
    await nghi(1500);
    await chup(gv, "3.8");
  });

  /* Trang CHI TIẾT lớp, không phải danh sách — chú thích hình đòi có danh sách thành viên, bài tập
   * kèm hạn nộp và bảng theo dõi nộp bài, những thứ chỉ trang chi tiết mới có. Bấm vào thẻ đầu thay
   * vì gắn cứng UUID, để script không hỏng khi nạp lại dữ liệu demo. */
  await man("3.11", "Lớp học", async () => {
    await toi(gv, "/classrooms", 2000);
    await gv.evaluate(() => {
      const the = document.querySelector('a[href^="/classrooms/"]');
      if (the) the.click();
      else {
        const bam = [...document.querySelectorAll("*")].find((e) => /Lớp 12A1/.test(e.textContent ?? "") && e.children.length === 0);
        bam?.closest("[class*=cursor],div")?.click();
      }
    });
    await gv.waitForFunction(() => /^\/classrooms\/[^/]+$/.test(location.pathname), { timeout: 15000 });
    await nghi(2500);
    await chup(gv, "3.11");
  });

  // ── Quản trị ───────────────────────────────────────────────────────────
  const qt = await dangNhap(browser, QT, "quản trị");

  await man("3.13", "Tổng quan quản trị", async () => {
    await toi(qt, "/admin", 3000);
    await chup(qt, "3.13");
  });

  await man("3.14", "Quản lý người dùng", async () => {
    await toi(qt, "/admin/users", 2500);
    await chup(qt, "3.14");
  });

  await man("3.15", "Giám sát AI", async () => {
    await toi(qt, "/admin/ai", 2500);
    await chup(qt, "3.15");
  });

  await man("3.16", "Báo cáo tính toàn vẹn", async () => {
    await toi(qt, "/admin/integrity", 2500);
    await chup(qt, "3.16");
  });

  await browser.close();

  console.log(`\n${"─".repeat(60)}`);
  console.log(`Chụp được ${xong.length} hình: ${xong.join(", ") || "(không có)"}`);
  if (truot.length) {
    console.log(`\nTrượt ${truot.length}:`);
    for (const t of truot) console.log(`  ${t.so} (${t.mo}): ${t.loi}`);
  }
  console.log(`\nCòn phải chụp tay: ${TU_CHUP_TAY.map((t) => t.so).join(", ")}`);
  for (const t of TU_CHUP_TAY) console.log(`  ${t.so} — ${t.vi_sao}`);
}

/* Những màn script không dựng thay người được. Ghi ra đây thay vì lặng lẽ bỏ qua, để người chạy biết
 * chính xác còn nợ gì — và biết vì sao, để đừng mất công tự động hoá lại thứ đã cân nhắc rồi. */
const TU_CHUP_TAY = [
  { so: "3.4", vi_sao: "màn làm bài — phải bắt đầu một lượt thật rồi chụp lúc đồng hồ đang chạy" },
  { so: "3.5", vi_sao: "màn kết quả — cần một lượt đã nộp có câu tự luận đã được AI chấm" },
  { so: "3.6", vi_sao: "phòng đấu — cần host và ít nhất một người chơi khác cùng lúc" },
];

main().catch((e) => {
  console.error("\nHỏng:", e.message);
  process.exit(1);
});
