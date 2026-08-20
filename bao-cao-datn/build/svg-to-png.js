const fs = require("fs");
const path = require("path");

(async () => {
  const [svgIn, pngOut, scaleArg] = process.argv.slice(2);
  if (!svgIn || !pngOut) {
    console.error("usage: svg-to-png.js <svg> <png> [scale]");
    process.exit(1);
  }
  const scale = parseFloat(scaleArg) || 2;
  const svgSource = fs.readFileSync(svgIn, "utf8");
  // pick puppeteer from mermaid-cli
  const mPath = path.join(__dirname, "node_modules", "@mermaid-js", "mermaid-cli", "node_modules", "puppeteer");
  let puppeteer;
  try {
    puppeteer = require(mPath);
  } catch {
    puppeteer = require("puppeteer");
  }
  // Parse width/height from svg viewBox
  const vb = (svgSource.match(/viewBox="([\d.\s-]+)"/) || [])[1];
  let w = 800, h = 600;
  if (vb) {
    const parts = vb.split(/\s+/).map(Number);
    w = parts[2]; h = parts[3];
  }
  const W = Math.ceil(w * scale), H = Math.ceil(h * scale);
  const cfgPath = path.join(__dirname, "puppeteer-config.json");
  const cfg = fs.existsSync(cfgPath) ? JSON.parse(fs.readFileSync(cfgPath, "utf8")) : {};
  const browser = await puppeteer.launch({ ...cfg, headless: "new" });
  const page = await browser.newPage();
  await page.setViewport({ width: W, height: H, deviceScaleFactor: 1 });
  const html = `<!doctype html><html><head><style>html,body{margin:0;padding:0;background:#fff}svg{display:block}</style></head><body>${svgSource.replace(/<svg([^>]*)>/, `<svg$1 width="${W}" height="${H}">`)}</body></html>`;
  await page.setContent(html, { waitUntil: "domcontentloaded" });
  await page.waitForSelector("svg");
  const svgEl = await page.$("svg");
  await svgEl.screenshot({ path: pngOut, omitBackground: false });
  await browser.close();
  const stat = fs.statSync(pngOut);
  console.log(`OK ${pngOut} ${W}x${H} ${stat.size} bytes`);
})().catch(e => { console.error(e); process.exit(1); });
