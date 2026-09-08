/* Chụp wireframe trong mockup.html -> assets/hinh-2.30.png, hinh-2.31.png
 *
 * Chỉ hai màn, không phải cả mười. Mục 2.2.3 của báo cáo trước đây có đủ 10 bản phác (2.30-2.39),
 * nhưng Chương 3 lại có ảnh chụp thật của ĐÚNG mười màn đó sau khi hiện thực — vẽ phác rồi vài chục
 * trang sau chụp ảnh chính nó là lặp, và lặp mất khoảng 5 trang.
 *
 * Giữ lại hai màn vì mục 2.2.3 lập luận có hai họ bố cục: khu học tập theo lưới thẻ, khu quản trị
 * theo bảng. Mỗi họ một đại diện là đủ chứng minh, tám màn còn lại chỉ minh hoạ thêm.
 *
 * `mockup.html` vẫn giữ nguyên cả 10 màn — không xoá, để cần dựng lại bản phác nào cũng còn định
 * nghĩa. Bảng dưới quyết định màn nào ra hình nào. */
const path = require("path");
const fs = require("fs");

(async () => {
  let puppeteer;
  try { puppeteer = require("puppeteer"); } catch { puppeteer = require("puppeteer-core"); }
  const chrome = "C:/Program Files/Google/Chrome/Application/chrome.exe";
  const browser = await puppeteer.launch({ executablePath: chrome, args: ["--no-sandbox"], headless: "new" });
  const page = await browser.newPage();
  await page.setViewport({ width: 920, height: 900, deviceScaleFactor: 2 });
  await page.goto("file://" + path.join(__dirname, "mockup.html").replace(/\\/g, "/"), { waitUntil: "networkidle0" });
  const screens = await page.$$(".screen");

  /* Chỉ số màn (đếm từ 0 theo thứ tự trong mockup.html) -> số hình trong báo cáo.
   * 1 = trang khám phá quiz (đại diện lưới thẻ) · 8 = quản lý người dùng (đại diện bảng) */
  const CAN_CHUP = { 1: "2.30", 8: "2.31" };

  for (const [chiSo, so] of Object.entries(CAN_CHUP)) {
    const man = screens[Number(chiSo)];
    if (!man) {
      console.error(`FAIL hinh-${so}: mockup.html không có màn thứ ${chiSo} (hiện có ${screens.length})`);
      continue;
    }
    const out = path.join(__dirname, "..", "assets", `hinh-${so}.png`);
    await man.screenshot({ path: out });
    console.log("OK ->", `hinh-${so}.png`, fs.statSync(out).size, "bytes");
  }
  await browser.close();
})().catch((e) => { console.error("FAIL:", e.message); process.exit(1); });
