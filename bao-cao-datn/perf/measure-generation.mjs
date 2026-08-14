/**
 * Đo thời gian sinh đề bằng AI (RAG) — phục vụ Bảng 3.4 (cột "Thời gian sinh") và NFR
 * "sinh 10 câu < 60 giây". Đây là số liệu ĐO ĐƯỢC bằng máy.
 *
 * LƯU Ý QUAN TRỌNG: cột "Tỷ lệ chấp nhận / số câu chấp nhận không sửa" trong Bảng 3.4 là
 * ĐÁNH GIÁ CHỦ QUAN của người review — script KHÔNG tự chấm. Bạn phải tự review từng câu
 * sinh ra và điền tỷ lệ chấp nhận. Script chỉ in thời gian + nội dung câu hỏi để bạn đánh giá.
 *
 * Tiền đề: BE chạy, đã upload tài liệu và tài liệu ở trạng thái READY (đã embedding),
 * GEMINI_API_KEY thật đã cấu hình ở backend.
 *
 * Chạy:
 *   node bao-cao/perf/measure-generation.mjs \
 *        --email gv.demo@eduexam.local --password Demo@12345 \
 *        --doc <DOCUMENT_ID> --count 10 --runs 3
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.join(__dirname, "..", "..");
const API = process.env.VITE_API_BASE_URL || "http://localhost:8080";

function arg(name, def) {
  const i = process.argv.indexOf(`--${name}`);
  return i >= 0 && process.argv[i + 1] ? process.argv[i + 1] : def;
}
function envFromFile() {
  const f = path.join(ROOT, ".env");
  const e = {};
  if (fs.existsSync(f)) for (const l of fs.readFileSync(f, "utf8").split(/\r?\n/)) {
    const m = l.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
    if (m && !l.trim().startsWith("#")) e[m[1]] = m[2];
  }
  return e;
}
const ENV = envFromFile();
const EMAIL = arg("email", "gv.demo@eduexam.local");
const PASSWORD = arg("password", "Demo@12345");
const DOC_ID = arg("doc", process.env.DOC_ID);
const COUNT = Number(arg("count", "10"));
const RUNS = Number(arg("runs", "3"));

if (!DOC_ID) {
  console.error("Thiếu --doc <DOCUMENT_ID>. Upload tài liệu trong UI (trạng thái READY) rồi lấy id.");
  process.exit(1);
}

async function api(method, url, { token, body } = {}) {
  const headers = { Accept: "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body) headers["Content-Type"] = "application/json";
  const res = await fetch(API + url, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const txt = await res.text();
  let data; try { data = txt ? JSON.parse(txt) : null; } catch { data = txt; }
  return { status: res.status, ok: res.ok, data };
}

async function main() {
  const login = await api("POST", "/api/auth/login", { body: { email: EMAIL, password: PASSWORD } });
  if (!login.ok) { console.error("Login fail:", login.status, login.data); process.exit(1); }
  const token = login.data.accessToken;

  console.log(`Đo sinh ${COUNT} câu MCQ, lặp ${RUNS} lần (doc=${DOC_ID})\n`);
  const times = [];
  for (let i = 1; i <= RUNS; i++) {
    const t0 = performance.now();
    const r = await api("POST", "/api/ai/generate", {
      token,
      body: { documentId: DOC_ID, questionTypes: ["MCQ"], count: COUNT, difficulty: "MEDIUM", topic: null, language: "vi" },
    });
    const ms = performance.now() - t0;
    if (!r.ok) { console.error(`  lần ${i}: lỗi ${r.status}`, r.data); continue; }
    const qs = r.data.questions || r.data.items || [];
    times.push(ms);
    console.log(`  lần ${i}: ${(ms / 1000).toFixed(1)}s — sinh ${qs.length} câu`);
    // in nội dung để bạn tự đánh giá tỷ lệ chấp nhận (Bảng 3.3)
    qs.forEach((q, k) => console.log(`     [${k + 1}] ${(q.content || q.questionText || "").slice(0, 80)}...`));
  }
  if (times.length) {
    const avg = times.reduce((a, b) => a + b, 0) / times.length / 1000;
    console.log(`\nThời gian trung bình: ${avg.toFixed(1)}s cho ${COUNT} câu (mục tiêu < 60s).`);
    console.log("→ Điền cột 'Thời gian sinh' của Bảng 3.4. Cột 'Tỷ lệ chấp nhận' bạn tự đánh giá từng câu.");
  }
}
main().catch((e) => { console.error(e); process.exit(1); });
