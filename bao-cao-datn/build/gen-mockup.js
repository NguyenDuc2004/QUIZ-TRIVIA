/* Chụp từng màn wireframe trong mockup.html -> assets/hinh-2.30.png .. 2.37.png */
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
  let n = 30;
  for (const s of screens) {
    const out = path.join(__dirname, "..", "assets", `hinh-2.${n}.png`);
    await s.screenshot({ path: out });
    console.log("OK ->", `hinh-2.${n}.png`, fs.statSync(out).size, "bytes");
    n++;
  }
  await browser.close();
})().catch((e) => { console.error("FAIL:", e.message); process.exit(1); });
