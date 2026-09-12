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
const sharp = require("sharp");

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
/** Người chơi thứ hai — phòng đấu cần ít nhất hai người thì bảng xếp hạng mới có nghĩa. */
const HS2 = { email: "hs2.demo@quizai.local", matKhau: MAT_KHAU };
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

const API = process.env.API || "http://localhost:8081/api/v1";

/** Đăng nhập bằng API, trả token. Dùng cho các bước cần dữ liệu chứ không cần giao diện. */
async function tokenHocVien() {
  const r = await fetch(`${API}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: HS.email, password: HS.matKhau }),
  });
  if (!r.ok) throw new Error(`đăng nhập API hỏng: ${r.status}`);
  return (await r.json()).accessToken;
}

/** Bắt đầu một lượt làm bài mới, trả mã lượt. Chọn quiz công khai đầu tiên có câu hỏi. */
async function batDauLamBai() {
  const t = await tokenHocVien();
  const h = { Authorization: "Bearer " + t, "Content-Type": "application/json" };
  const ds = await fetch(`${API}/quizzes?size=30`, { headers: h }).then((r) => r.json());
  for (const q of ds.content ?? []) {
    const r = await fetch(`${API}/quizzes/${q.id}/attempts`, { method: "POST", headers: h, body: "{}" });
    // Mã lượt nằm trong `attempt.id`, KHÔNG phải `id` ở cấp một. Lấy nhầm thì trang mở ra
    // `/attempts/undefined` và chụp về một ảnh "Không tìm thấy tài nguyên" mà script vẫn báo thành công.
    if (r.ok) return (await r.json()).attempt.id;
  }
  throw new Error("không bắt đầu được lượt làm bài nào");
}

/** Mã lượt đã nộp VÀ có câu được AI chấm — để màn kết quả có khối nhận xét. */
async function luotDaChamAI() {
  const t = await tokenHocVien();
  const h = { Authorization: "Bearer " + t };
  const ds = await fetch(`${API}/attempts?size=50`, { headers: h }).then((r) => r.json()).catch(() => null);
  const ls = ds?.content ?? [];

  /* Chọn lượt có câu AI chấm ĐẠT ĐIỂM CAO NHẤT, không phải lượt đầu gặp. Một bài "Em không nhớ rõ"
   * nhận 0 điểm vẫn chứng minh AI có chấm, nhưng minh hoạ cho báo cáo thì yếu: nó không cho thấy mô
   * hình đọc hiểu được nội dung, chỉ cho thấy nó biết bài trống. */
  let tot = null;
  for (const a of ls) {
    if (a.status !== "SUBMITTED") continue;
    const ct = await fetch(`${API}/attempts/${a.id}`, { headers: h }).then((r) => r.json());
    for (const c of ct.questions ?? []) {
      if (!c.aiFeedback) continue;
      const ty = (c.score ?? 0) / (c.maxScore || 1);
      if (!tot || ty > tot.ty) tot = { id: a.id, ty };
    }
  }
  if (tot) return tot.id;
  throw new Error("không có lượt nào đã được AI chấm — làm một bài có câu tự luận rồi chạy lại");
}

/** Đăng nhập bằng API cho một tài khoản bất kỳ. */
async function token(tk) {
  const r = await fetch(`${API}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: tk.email, password: tk.matKhau }),
  });
  if (!r.ok) throw new Error(`đăng nhập ${tk.email} hỏng: ${r.status}`);
  return (await r.json()).accessToken;
}

/** Mở phòng từ quiz công khai đầu tiên, trả mã phòng. */
async function moPhong() {
  const t = await token(HS);
  const h = { Authorization: "Bearer " + t, "Content-Type": "application/json" };
  const ds = await fetch(`${API}/quizzes?size=10`, { headers: h }).then((r) => r.json());
  const q = (ds.content ?? [])[0];
  if (!q) throw new Error("không có quiz công khai nào để mở phòng");
  const r = await fetch(`${API}/rooms`, {
    method: "POST",
    headers: h,
    body: JSON.stringify({ quizId: q.id, secondsPerQuestion: 30, allowGuests: true }),
  });
  if (!r.ok) throw new Error(`mở phòng hỏng: ${r.status}`);
  const v = await r.json();
  return { ma: v.code ?? v.roomCode, tokenHost: t };
}

/** Cho một tài khoản vào phòng qua API. */
async function vaoPhong(tk, ma) {
  const t = await token(tk);
  const r = await fetch(`${API}/rooms/${ma}/join`, { method: "POST", headers: { Authorization: "Bearer " + t } });
  if (!r.ok) throw new Error(`${tk.email} vào phòng hỏng: ${r.status}`);
}

/** Đăng nhập bằng GIAO DIỆN trên một trang đã có sẵn (dùng cho phiên thứ hai). */
async function dangNhapTrang(page, tk) {
  await page.goto(`${FE}/login`, { waitUntil: "networkidle2" });
  await page.type('input[name="email"]', tk.email);
  await page.type('input[name="password"]', tk.matKhau);
  await page.click('button[type="submit"]');
  await page.waitForFunction(() => !location.pathname.startsWith("/login"), { timeout: 20000 });
  await nghi(800);
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

  /* Màn LÀM BÀI. Lượt làm được tạo qua API để lấy mã lượt, rồi mới mở bằng trình duyệt — bấm dò nút
   * "Bắt đầu làm bài" qua nhiều bố cục trang giới thiệu thì mong manh hơn nhiều. Chọn vài phương án
   * trước khi chụp để ảnh không phải là một đề còn trắng tinh. */
  await man("3.4", "Đang làm bài", async () => {
    const id = await batDauLamBai();
    await toi(hs, `/attempts/${id}`, 2500);
    // Chọn hai phương án đầu của hai câu đầu, nếu bấm được
    await hs.evaluate(() => {
      const o = [...document.querySelectorAll('input[type="radio"], .ant-radio-wrapper, [role="radio"]')];
      o.slice(0, 1).forEach((e) => e.click?.());
    });
    await nghi(1200);
    await chup(hs, "3.4");
  });

  /* Màn KẾT QUẢ. Dùng lượt ĐÃ CÓ SẴN và đã được AI chấm thay vì làm bài mới: chú thích hình đòi có
   * nhận xét của AI cho câu tự luận, mà chấm lại là tốn một lượt gọi mô hình cho thứ đã có. Cuộn tới
   * đúng khối nhận xét vì nó nằm dưới màn hình đầu. */
  await man("3.5", "Kết quả bài làm", async () => {
    const id = await luotDaChamAI();
    await toi(hs, `/attempts/${id}`, 2500);
    await hs.evaluate(() => {
      const el = [...document.querySelectorAll("*")].find((e) => /^Nhận xét · /.test(e.textContent ?? "") && e.children.length === 0);
      el?.scrollIntoView({ block: "center" });
    });
    await nghi(1200);
    await chup(hs, "3.5");
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

  /* PHÒNG ĐẤU. Chú thích hình đòi CẢ phòng chờ (mã PIN, mã QR, danh sách người chơi) LẪN màn chơi
   * (câu hỏi, bảng xếp hạng trực tiếp), nên chụp hai ảnh rồi ghép dọc thành một hình.
   *
   * Phòng và lượt vào phòng đi qua API để lấy mã phòng chắc chắn; phần nhìn thì vẫn là giao diện thật.
   * Cần hai phiên trình duyệt độc lập — một người chơi thì bảng xếp hạng chỉ có một dòng. */
  await man("3.6", "Phòng chờ và phòng đấu", async () => {
    const { ma, tokenHost } = await moPhong();

    const ctx2 = await browser.createBrowserContext();
    const p2 = await ctx2.newPage();
    await p2.setViewport(KHUNG);
    await epSang(p2);
    await dangNhapTrang(p2, HS2);
    await vaoPhong(HS2, ma);
    await toi(p2, `/rooms/${ma}`, 1500);

    const host = hs;
    await toi(host, `/rooms/${ma}`, 2500);

    const tmp1 = path.join(ASSETS, "_tmp-3.6-cho.png");
    const tmp2 = path.join(ASSETS, "_tmp-3.6-choi.png");
    await host.screenshot({ path: tmp1 });

    /* Bắt đầu ván. Khi còn người chưa bấm sẵn sàng, nút này KHÔNG gửi lệnh ngay mà mở một hộp thoại
     * xác nhận (xem `batDauVan` trong RoomPage.tsx) — bỏ qua bước bấm "Vẫn bắt đầu" thì phòng đứng
     * nguyên ở phòng chờ và lệnh chờ câu hỏi sẽ hết giờ mà không có lỗi nào rõ ràng. */
    await host.evaluate(() => {
      const b = [...document.querySelectorAll("button")].find((e) => /Bắt đầu ván/.test(e.textContent ?? ""));
      b?.click();
    });
    await nghi(800);
    await host.evaluate(() => {
      const ok = [...document.querySelectorAll(".ant-modal button, button")].find((e) => /Vẫn bắt đầu/.test(e.textContent ?? ""));
      ok?.click();
    });
    await host.waitForFunction(() => /Câu \s*\d+\s*\/\s*\d+/.test(document.body.innerText), { timeout: 30000 });
    await nghi(2000);
    await host.screenshot({ path: tmp2 });

    /* Ghép dọc: hai ảnh cùng bề rộng nên chỉ cần cộng chiều cao, chừa một vạch trắng phân cách. */
    const a = sharp(tmp1);
    const b = sharp(tmp2);
    const ma_ = await a.metadata();
    const mb = await b.metadata();
    const KE = 10;
    await sharp({ create: { width: ma_.width, height: ma_.height + mb.height + KE, channels: 3, background: "#ffffff" } })
      .composite([
        { input: await a.toBuffer(), top: 0, left: 0 },
        { input: await b.toBuffer(), top: ma_.height + KE, left: 0 },
      ])
      .png()
      .toFile(path.join(ASSETS, "hinh-3.6.png"));

    fs.unlinkSync(tmp1);
    fs.unlinkSync(tmp2);
    await ctx2.close();
    console.log(`  ✓ hinh-3.6.png  (${fs.statSync(path.join(ASSETS, "hinh-3.6.png")).size} bytes, ghép 2 ảnh)`);
    xong.push("3.6");
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
const TU_CHUP_TAY = [];

main().catch((e) => {
  console.error("\nHỏng:", e.message);
  process.exit(1);
});
