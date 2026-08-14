/**
 * Seed dữ liệu demo cho EduExam qua REST API — phục vụ chụp ảnh báo cáo (Hình 3.2–3.9).
 *
 * Yêu cầu: backend chạy ở http://localhost:8080, đã seed tài khoản admin (env ADMIN_INITIAL_*).
 * Chạy:    node bao-cao/capture/seed.mjs
 *
 * Script idempotent ở mức hợp lý: bỏ qua bước nếu dữ liệu đã tồn tại (báo lỗi 409 -> skip).
 * Ghi kết quả (mật khẩu demo, mã lớp, id) ra bao-cao/capture/seed-output.json để capture.mjs dùng.
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.join(__dirname, "..", "..");
const API = process.env.VITE_API_BASE_URL || "http://localhost:8080";

// ---- đọc .env để lấy admin creds ----
function readEnv() {
  const env = { ...process.env };
  const envFile = path.join(ROOT, ".env");
  if (fs.existsSync(envFile)) {
    for (const line of fs.readFileSync(envFile, "utf8").split(/\r?\n/)) {
      const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
      if (m && !line.trim().startsWith("#")) env[m[1]] ??= m[2];
    }
  }
  return env;
}
const ENV = readEnv();
const ADMIN_EMAIL = ENV.ADMIN_INITIAL_EMAIL || "admin@eduexam.local";
const ADMIN_PASSWORD = ENV.ADMIN_INITIAL_PASSWORD;
const DEMO_PASSWORD = "Demo@12345"; // mật khẩu chung cho tài khoản demo sau khi đổi

if (!ADMIN_PASSWORD) {
  console.error("Thiếu ADMIN_INITIAL_PASSWORD trong .env — không thể đăng nhập admin.");
  process.exit(1);
}

// ---- helper HTTP ----
async function api(method, url, { token, body, raw } = {}) {
  const headers = { Accept: "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body && !raw) headers["Content-Type"] = "application/json";
  const res = await fetch(API + url, {
    method,
    headers,
    body: body ? (raw ? body : JSON.stringify(body)) : undefined,
  });
  const text = await res.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  return { status: res.status, ok: res.ok, data };
}

/** Đăng nhập; nếu mustChangePassword thì tự đổi sang newPassword và trả token mới. */
async function loginEnsure(email, password, newPassword) {
  let r = await api("POST", "/api/auth/login", { body: { email, password } });
  if (!r.ok) { console.error(`  login fail ${email}: ${r.status}`, r.data); return null; }
  let token = r.data.accessToken;
  if (r.data.user?.mustChangePassword && newPassword) {
    const c = await api("POST", "/api/auth/change-password", {
      token, body: { currentPassword: password, newPassword },
    });
    if (!c.ok) { console.error(`  change-password fail ${email}: ${c.status}`, c.data); return null; }
    token = c.data.accessToken || token;
    console.log(`  ${email}: đã đổi mật khẩu lần đầu -> ${newPassword}`);
  }
  return token;
}

async function main() {
  console.log("API:", API);
  const out = { api: API, demoPassword: DEMO_PASSWORD, accounts: {}, courseId: null, enrollmentCode: null, examId: null };

  // 1) Admin login (đổi mật khẩu nếu cần — giữ chính DEMO_PASSWORD để lần sau ổn định)
  console.log("[1] Đăng nhập admin...");
  const adminToken = await loginEnsure(ADMIN_EMAIL, ADMIN_PASSWORD, DEMO_PASSWORD)
    || await loginEnsure(ADMIN_EMAIL, DEMO_PASSWORD, null); // nếu đã đổi từ trước
  if (!adminToken) { console.error("Không đăng nhập được admin. Dừng."); process.exit(1); }
  out.accounts.admin = { email: ADMIN_EMAIL, password: "(env hoặc " + DEMO_PASSWORD + ")" };

  // 2) Tạo teacher + student
  console.log("[2] Tạo giảng viên + sinh viên demo...");
  const mkUser = async (email, fullName, role) => {
    const r = await api("POST", "/api/admin/users", { token: adminToken, body: { email, fullName, role } });
    if (r.ok) { console.log(`  + ${role} ${email} (temp: ${r.data.temporaryPassword})`); return r.data.temporaryPassword; }
    if (r.status === 409) { console.log(`  = ${role} ${email} đã tồn tại -> dùng DEMO_PASSWORD`); return null; }
    console.error(`  x tạo ${email} fail: ${r.status}`, r.data); return null;
  };
  const tEmail = "gv.demo@eduexam.local", sEmail = "sv.demo@eduexam.local";
  const tTemp = await mkUser(tEmail, "Giảng viên Demo", "TEACHER");
  const sTemp = await mkUser(sEmail, "Sinh viên Demo", "STUDENT");

  const teacherToken = await loginEnsure(tEmail, tTemp || DEMO_PASSWORD, DEMO_PASSWORD)
    || await loginEnsure(tEmail, DEMO_PASSWORD, null);
  const studentToken = await loginEnsure(sEmail, sTemp || DEMO_PASSWORD, DEMO_PASSWORD)
    || await loginEnsure(sEmail, DEMO_PASSWORD, null);
  out.accounts.teacher = { email: tEmail, password: DEMO_PASSWORD };
  out.accounts.student = { email: sEmail, password: DEMO_PASSWORD };
  if (!teacherToken || !studentToken) { console.error("Không login được teacher/student."); process.exit(1); }

  // 3) Lấy subjectId (dùng môn đầu tiên có sẵn)
  console.log("[3] Lấy danh sách môn học...");
  const subj = await api("GET", "/api/admin/subjects", { token: adminToken });
  const subjectId = Array.isArray(subj.data) && subj.data.length ? subj.data[0].id : null;
  if (!subjectId) {
    console.error("  Chưa có môn học nào. Hãy tạo Khoa + Môn học trong UI Admin (/admin/master-data) rồi chạy lại.");
    process.exit(1);
  }
  console.log("  subjectId:", subjectId);

  // 4) Teacher tạo lớp
  console.log("[4] Tạo lớp học...");
  const year = new Date().getFullYear();
  const course = await api("POST", "/api/courses", {
    token: teacherToken,
    body: { name: "Lớp Demo — Nhập môn CNTT", description: "Lớp minh họa cho báo cáo ĐATN.", subjectId, semester: `${year}-1` },
  });
  if (!course.ok) { console.error("  tạo lớp fail:", course.status, course.data); process.exit(1); }
  out.courseId = course.data.id;
  out.enrollmentCode = course.data.enrollmentCode;
  console.log(`  courseId=${out.courseId} enrollmentCode=${out.enrollmentCode}`);

  // 5) Tạo vài câu hỏi MCQ
  console.log("[5] Tạo câu hỏi MCQ demo...");
  const mcqs = [
    { content: "HTTP là viết tắt của cụm từ nào?", opts: ["HyperText Transfer Protocol", "High Transfer Text Protocol", "HyperText Transmission Process", "Home Tool Transfer Protocol"], correct: 0, topic: "Mạng máy tính" },
    { content: "Cấu trúc dữ liệu nào hoạt động theo nguyên tắc LIFO?", opts: ["Queue", "Stack", "Tree", "Graph"], correct: 1, topic: "Cấu trúc dữ liệu" },
    { content: "Trong CSDL quan hệ, khóa chính (primary key) dùng để làm gì?", opts: ["Mã hóa dữ liệu", "Định danh duy nhất một bản ghi", "Sắp xếp dữ liệu", "Nén dữ liệu"], correct: 1, topic: "Cơ sở dữ liệu" },
    { content: "Ngôn ngữ nào sau đây chạy phía máy chủ phổ biến?", opts: ["HTML", "CSS", "Java", "SVG"], correct: 2, topic: "Lập trình" },
    { content: "Độ phức tạp thời gian của tìm kiếm nhị phân là?", opts: ["O(n)", "O(n^2)", "O(log n)", "O(1)"], correct: 2, topic: "Giải thuật" },
  ];
  let created = 0;
  for (const q of mcqs) {
    const r = await api("POST", `/api/courses/${out.courseId}/questions`, {
      token: teacherToken,
      body: {
        type: "MCQ", difficulty: "MEDIUM", topic: q.topic, content: q.content,
        defaultPoints: 1.0, explanation: "",
        options: q.opts.map((t, i) => ({ text: t, isCorrect: i === q.correct })),
      },
    });
    if (r.ok) created++; else console.error("  tạo câu hỏi fail:", r.status, r.data);
  }
  console.log(`  đã tạo ${created}/${mcqs.length} câu hỏi`);

  // 6) Student join lớp
  console.log("[6] Sinh viên tham gia lớp...");
  const enr = await api("POST", "/api/enrollments", { token: studentToken, body: { enrollmentCode: out.enrollmentCode } });
  console.log(`  enroll: ${enr.status} ${enr.ok ? enr.data.status : JSON.stringify(enr.data)}`);

  // 7) Teacher tạo kỳ thi (FIXED, lấy tất cả câu hỏi vừa tạo)
  console.log("[7] Tạo kỳ thi...");
  const qlist = await api("GET", `/api/courses/${out.courseId}/questions`, { token: teacherToken });
  const items = Array.isArray(qlist.data?.content) ? qlist.data.content : (Array.isArray(qlist.data) ? qlist.data : []);
  const now = Date.now();
  const exam = await api("POST", "/api/exams", {
    token: teacherToken,
    body: {
      courseId: out.courseId, title: "Kiểm tra giữa kỳ — Demo", description: "Kỳ thi minh họa.",
      startsAt: new Date(now - 5 * 60000).toISOString(), endsAt: new Date(now + 24 * 3600000).toISOString(),
      durationMinutes: 30, showResultImmediately: true, selectionMode: "FIXED",
      questions: items.slice(0, 5).map((q, i) => ({ questionId: q.id, displayOrder: i, points: 1.0 })),
      randomizeQuestionOrder: false, randomizeOptionOrder: false,
    },
  });
  if (exam.ok) { out.examId = exam.data.id; console.log("  examId:", out.examId); }
  else console.error("  tạo kỳ thi fail:", exam.status, exam.data);

  fs.writeFileSync(path.join(__dirname, "seed-output.json"), JSON.stringify(out, null, 2));
  console.log("\nXong. Ghi bao-cao/capture/seed-output.json");
  console.log("Tài khoản demo (mật khẩu " + DEMO_PASSWORD + "):", tEmail, "|", sEmail);
}

main().catch((e) => { console.error(e); process.exit(1); });
