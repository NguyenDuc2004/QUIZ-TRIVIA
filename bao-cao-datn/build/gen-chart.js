/* Sinh biểu đồ số liệu -> assets/hinh-3.17.png (dựng SVG rồi chụp bằng Chrome).
 *
 * Số liệu lấy nguyên từ docs/bao-cao/so-lieu-3.5-hieu-nang-realtime.md (đo 08/08/2026).
 * SỬA SỐ Ở ĐÂY THÌ PHẢI SỬA CẢ BẢNG trong 03-chuong-3.md — hai chỗ phải khớp nhau.
 *
 * ## Ba quyết định về cách vẽ, để người sau không "sửa cho đẹp" rồi làm hỏng nghĩa
 *
 * 1. **Trục tung tuyến tính, không log.** Kết luận của mục 3.5 là độ trễ tăng *siêu tuyến tính*.
 *    Trục log sẽ bẻ đường cong đó thành gần thẳng — tức xoá đúng cái phát hiện mà hình sinh ra để
 *    trình bày. Hệ quả phải chấp nhận: bốn mốc đầu (10–50 người) nằm sát trục. Không sao, vì bảng
 *    ngay trên hình đã có số chính xác; việc của hình là cho thấy HÌNH DẠNG.
 *
 * 2. **Trục hoành theo giá trị thật, không chia đều.** Các mốc 10/30/50/100/150/200 cách nhau không
 *    đều. Xếp đều khoảng cách sẽ phóng đại đoạn đầu và bóp đoạn cuối — lại làm sai hình dạng.
 *
 * 3. **Phân biệt hai đường KHÔNG bằng màu.** Báo cáo in đen trắng, màu biến thành hai mức xám gần
 *    nhau. Nên mỗi đường mang thêm nét liền/nét đứt và dấu tròn/dấu vuông, kèm nhãn ghi thẳng ở
 *    cuối đường. Bỏ màu đi vẫn đọc được.
 */
const path = require("path");
const fs = require("fs");

/* Đo ngày 08/08/2026 — bảng mục 3.5 §3 */
const DL = [
  { nguoi: 10, p50: 18, p95: 20 },
  { nguoi: 30, p50: 26, p95: 32 },
  { nguoi: 50, p50: 48, p95: 52 },
  { nguoi: 100, p50: 180, p95: 216 },
  { nguoi: 150, p50: 542, p95: 566 },
  { nguoi: 200, p50: 1411, p95: 1509 },
];

const W = 940,
  H = 540;
const M = { top: 30, right: 104, bottom: 62, left: 82 };
const PW = W - M.left - M.right;
const PH = H - M.top - M.bottom;

const X_MAX = 210;
const Y_MAX = 1600;
const Y_BUOC = 200;

const x = (v) => M.left + (v / X_MAX) * PW;
const y = (v) => M.top + PH - (v / Y_MAX) * PH;

/* Dấu phân cách hàng nghìn theo lối Việt Nam: khoảng trắng, như trong bảng của báo cáo */
const so = (n) => String(n).replace(/\B(?=(\d{3})+(?!\d))/g, " ");

const duong = (khoa) => DL.map((d, i) => `${i ? "L" : "M"}${x(d.nguoi).toFixed(1)} ${y(d[khoa]).toFixed(1)}`).join(" ");

/* Slot 1 và 2 của bảng màu tham chiếu — đã chạy validate_palette.js, đạt cả 5 phép kiểm ở nền sáng */
const C_P95 = "#2a78d6";
const C_P50 = "#eb6834";
const MUC = "#6b7280";
const LUOI = "#e5e7eb";
const CHU = "#111827";

let luoi = "";
for (let v = 0; v <= Y_MAX; v += Y_BUOC) {
  luoi += `<line x1="${M.left}" y1="${y(v)}" x2="${M.left + PW}" y2="${y(v)}" stroke="${LUOI}" stroke-width="1"/>
    <text x="${M.left - 12}" y="${y(v) + 5}" text-anchor="end" font-size="14" fill="${MUC}">${so(v)}</text>`;
}

let truc = "";
for (const d of DL) {
  truc += `<line x1="${x(d.nguoi)}" y1="${M.top + PH}" x2="${x(d.nguoi)}" y2="${M.top + PH + 6}" stroke="${MUC}" stroke-width="1"/>
    <text x="${x(d.nguoi)}" y="${M.top + PH + 26}" text-anchor="middle" font-size="14" fill="${MUC}">${d.nguoi}</text>`;
}

const dauP95 = DL.map((d) => `<circle cx="${x(d.nguoi)}" cy="${y(d.p95)}" r="5.5" fill="${C_P95}" stroke="#ffffff" stroke-width="2"/>`).join("");
const dauP50 = DL.map(
  (d) => `<rect x="${x(d.nguoi) - 5}" y="${y(d.p50) - 5}" width="10" height="10" fill="${C_P50}" stroke="#ffffff" stroke-width="2"/>`,
).join("");

/* Ngưỡng dùng được trong thực tế — mục 3.5 §3 gọi tên mốc 100 người, hình phải chỉ ra được */
const NGUONG = `
  <line x1="${x(100)}" y1="${M.top}" x2="${x(100)}" y2="${M.top + PH}" stroke="${MUC}" stroke-width="1" stroke-dasharray="3 4" opacity="0.65"/>
  <text x="${x(100) - 8}" y="${M.top + 16}" text-anchor="end" font-size="13" fill="${MUC}">ngưỡng dùng được</text>
  <text x="${x(100) - 8}" y="${M.top + 33}" text-anchor="end" font-size="13" fill="${MUC}">— 100 người</text>`;

const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" font-family="Segoe UI, Arial, sans-serif">
  <rect width="${W}" height="${H}" fill="#ffffff"/>
  ${luoi}
  ${NGUONG}
  <line x1="${M.left}" y1="${M.top + PH}" x2="${M.left + PW}" y2="${M.top + PH}" stroke="${MUC}" stroke-width="1.5"/>
  ${truc}

  <path d="${duong("p50")}" fill="none" stroke="${C_P50}" stroke-width="2" stroke-dasharray="7 5" stroke-linejoin="round"/>
  <path d="${duong("p95")}" fill="none" stroke="${C_P95}" stroke-width="2" stroke-linejoin="round"/>
  ${dauP50}${dauP95}

  <text x="${x(200) + 12}" y="${y(1509) - 2}" font-size="14" font-weight="600" fill="${CHU}">P95 — ${so(1509)} ms</text>
  <text x="${x(200) + 12}" y="${y(1411) + 16}" font-size="14" font-weight="600" fill="${CHU}">P50 — ${so(1411)} ms</text>
  <text x="${x(100) - 10}" y="${y(216) - 10}" text-anchor="end" font-size="13" fill="${CHU}">P95 216 ms</text>

  <text x="${M.left}" y="${H - 14}" font-size="14" fill="${MUC}">Số người chơi trong phòng</text>
  <text transform="translate(20 ${M.top + PH / 2}) rotate(-90)" text-anchor="middle" font-size="14" fill="${MUC}">Độ trễ phát câu hỏi (ms)</text>

  <g transform="translate(${M.left + 16} ${M.top + 12})">
    <line x1="0" y1="0" x2="26" y2="0" stroke="${C_P95}" stroke-width="2"/>
    <circle cx="13" cy="0" r="5.5" fill="${C_P95}" stroke="#ffffff" stroke-width="2"/>
    <text x="34" y="5" font-size="14" fill="${CHU}">P95</text>
    <line x1="0" y1="22" x2="26" y2="22" stroke="${C_P50}" stroke-width="2" stroke-dasharray="7 5"/>
    <rect x="8" y="17" width="10" height="10" fill="${C_P50}" stroke="#ffffff" stroke-width="2"/>
    <text x="34" y="27" font-size="14" fill="${CHU}">P50</text>
  </g>
</svg>`;

(async () => {
  const html = `<!doctype html><meta charset="utf-8"><body style="margin:0">${svg}</body>`;
  const tmp = path.join(__dirname, "_chart.html");
  fs.writeFileSync(tmp, html, "utf8");

  let puppeteer;
  try {
    puppeteer = require("puppeteer");
  } catch {
    puppeteer = require("puppeteer-core");
  }
  const chrome = "C:/Program Files/Google/Chrome/Application/chrome.exe";
  const browser = await puppeteer.launch({ executablePath: chrome, args: ["--no-sandbox"], headless: "new" });
  const page = await browser.newPage();
  await page.setViewport({ width: W, height: H, deviceScaleFactor: 2 });
  await page.goto("file://" + tmp.replace(/\\/g, "/"), { waitUntil: "networkidle0" });
  const out = path.join(__dirname, "..", "assets", "hinh-3.17.png");
  await page.screenshot({ path: out });
  await browser.close();
  fs.unlinkSync(tmp);
  console.log("OK hinh-3.17.png", fs.statSync(out).size, "bytes");
})().catch((e) => {
  console.error("FAIL:", e.message);
  process.exit(1);
});
