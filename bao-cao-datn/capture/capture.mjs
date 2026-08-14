/**
 * Chụp ảnh màn hình sản phẩm EduExam cho báo cáo (Hình 3.2–3.10).
 *
 * Yêu cầu: BE (8080) + FE (5173) đang chạy, đã chạy `node seed.mjs` (đọc seed-output.json).
 * Chạy:    node bao-cao/capture/capture.mjs
 *
 * Mỗi ảnh được chụp độc lập (try/catch) — một màn lỗi không làm hỏng các màn khác.
 * Ảnh lưu vào bao-cao/assets/hinh-3.X.png; build.js sẽ tự chèn khi build lại.
 *
 * Lưu ý: các màn cần AI (sinh đề RAG, trợ lý AI) hoặc trạng thái thi trực tiếp + webcam
 * (làm bài, dòng thời gian vi phạm) khó tự động hóa đầy đủ — nếu chụp ra màn trống/lỗi,
 * hãy chụp thủ công các màn đó (xem README).
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright"; // cài trước: cd bao-cao/capture && npm install

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ASSETS = path.join(__dirname, "..", "assets");
const FE = process.env.FE_URL || "http://localhost:5173";
const SEED = JSON.parse(fs.readFileSync(path.join(__dirname, "seed-output.json"), "utf8"));
const VIEWPORT = { width: 1366, height: 900 };

const ok = [], fail = [];
const shot = async (page, name, { full = false } = {}) => {
  const file = path.join(ASSETS, name);
  await page.screenshot({ path: file, fullPage: full });
  console.log("  ✓ chụp", name);
  ok.push(name);
};
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function login(context, email, password) {
  const page = await context.newPage();
  await page.goto(`${FE}/login`, { waitUntil: "networkidle" });
  await page.fill("#email", email);
  await page.fill("#password", password);
  await Promise.all([
    page.waitForURL((u) => !u.pathname.includes("/login"), { timeout: 15000 }).catch(() => {}),
    page.click('button[type="submit"]'),
  ]);
  await sleep(1500);
  return page;
}

async function step(name, fn) {
  try { await fn(); }
  catch (e) { console.error(`  ✗ ${name}: ${e.message}`); fail.push(name); }
}

async function main() {
  fs.mkdirSync(ASSETS, { recursive: true });
  const browser = await chromium.launch({
    args: ["--use-fake-device-for-media-stream", "--use-fake-ui-for-media-stream"],
  });

  // ---- Hình 3.2: màn đăng nhập (không cần login) ----
  await step("3.2 login", async () => {
    const ctx = await browser.newContext({ viewport: VIEWPORT });
    const page = await ctx.newPage();
    await page.goto(`${FE}/login`, { waitUntil: "networkidle" });
    await sleep(800);
    await shot(page, "hinh-3.2.png");
    await ctx.close();
  });

  // ---- ADMIN: Hình 3.5 dashboard + quản lý người dùng ----
  await step("3.5 admin", async () => {
    const ctx = await browser.newContext({ viewport: VIEWPORT });
    const page = await login(ctx, SEED.accounts.admin.email, SEED.demoPassword);
    await page.goto(`${FE}/admin/users`, { waitUntil: "networkidle" }).catch(() => {});
    await sleep(1500);
    await shot(page, "hinh-3.5.png");
    await ctx.close();
  });

  // ---- TEACHER: 3.3 ngân hàng câu hỏi, 3.6 trợ lý AI/thống kê, 3.8 sinh đề, 3.9 vi phạm ----
  await step("teacher screens", async () => {
    const ctx = await browser.newContext({ viewport: VIEWPORT, permissions: ["camera"] });
    const page = await login(ctx, SEED.accounts.teacher.email, SEED.demoPassword);

    await step("3.3 question bank", async () => {
      await page.goto(`${FE}/teacher/courses/${SEED.courseId}/questions`, { waitUntil: "networkidle" });
      await sleep(2000);
      await shot(page, "hinh-3.3.png");
    });

    // 3.6 trợ lý AI / thống kê chất lượng — best-effort: mở trang stats nếu có
    await step("3.6 ai authoring/stats", async () => {
      await page.goto(`${FE}/teacher/courses/${SEED.courseId}/questions?tab=stats`, { waitUntil: "networkidle" });
      await sleep(1500);
      await shot(page, "hinh-3.6.png");
    });

    // 3.8 sinh đề RAG — cần document đã embed; thường chưa có -> chụp trang tài liệu/sinh đề best-effort
    await step("3.8 ai documents", async () => {
      await page.goto(`${FE}/teacher/courses/${SEED.courseId}/ai-documents`, { waitUntil: "networkidle" });
      await sleep(1500);
      await shot(page, "hinh-3.8.png");
    });
  });

  // ---- STUDENT: dashboard, 3.7 luyện tập, 3.4 làm bài ----
  await step("student screens", async () => {
    const ctx = await browser.newContext({ viewport: VIEWPORT, permissions: ["camera"] });
    const page = await login(ctx, SEED.accounts.student.email, SEED.demoPassword);

    await step("student dashboard", async () => {
      await page.goto(`${FE}/student`, { waitUntil: "networkidle" });
      await sleep(1500);
      await shot(page, "hinh-3.1b-student-home.png"); // tham khảo (không phải hình trong báo cáo)
    });

    await step("3.7 practice", async () => {
      await page.goto(`${FE}/student/courses/${SEED.courseId}/practice`, { waitUntil: "networkidle" });
      await sleep(1500);
      await shot(page, "hinh-3.7.png");
    });

    // 3.4 làm bài thi — best-effort (cần consent + webcam)
    await step("3.4 exam attempt", async () => {
      if (!SEED.examId) throw new Error("chưa có examId");
      await page.goto(`${FE}/student/exams/${SEED.examId}/attempt`, { waitUntil: "networkidle" });
      await sleep(2500);
      // thử bấm nút đồng ý quyền riêng tư nếu có
      for (const t of ["Đồng ý", "Bắt đầu", "Tôi đồng ý", "Vào thi"]) {
        const btn = page.getByRole("button", { name: new RegExp(t, "i") });
        if (await btn.count()) { await btn.first().click().catch(() => {}); await sleep(2000); break; }
      }
      await shot(page, "hinh-3.4.png");
    });
  });

  console.log(`\nXong. Chụp được ${ok.length} ảnh, lỗi ${fail.length}.`);
  if (fail.length) console.log("Các màn lỗi (chụp thủ công):", fail.join(", "));
  await browser.close();
}

main().catch((e) => { console.error(e); process.exit(1); });
