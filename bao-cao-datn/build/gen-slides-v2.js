/* Slide bảo vệ ĐATN EduExam — BẢN V2: sơ đồ vẽ tay bằng shapes + icon (sinh động).
 * Chạy: cd bao-cao/build && node gen-slides-v2.js -> ../Slide-BaoVe-EduExam-v2.pptx
 */
const path = require("path");
const pptxgen = require("pptxgenjs");
const React = require("react");
const ReactDOMServer = require("react-dom/server");
const sharp = require("sharp");
const FA = require("react-icons/fa");

const OUT = path.join(__dirname, "..", "Slide-BaoVe-EduExam-v2.pptx");

// palette
const NAVY = "0E2A47", NAVY2 = "16395E", TEAL = "0D9488", TEALL = "14B8A6";
const AMBER = "F59E0B", BERRY = "6D2E46", BGSOFT = "F4F7FB", INK = "1E293B", MUTED = "64748B", WHITE = "FFFFFF";
const LINEC = "94A3B8";
const HF = "Georgia", BF = "Calibri";

// ---- icon rendering ----
async function renderIcon(Comp, colorHex, size = 256) {
  const svg = ReactDOMServer.renderToStaticMarkup(React.createElement(Comp, { color: colorHex, size: String(size) }));
  const png = await sharp(Buffer.from(svg)).png().toBuffer();
  return "image/png;base64," + png.toString("base64");
}
const ICONS = {};
// [key, component, colorHex]
const ICONLIST = [
  ["arrowR", FA.FaArrowRight, "#94A3B8"], ["arrowD", FA.FaArrowDown, "#94A3B8"],
  ["react", FA.FaReact, "#FFFFFF"], ["server", FA.FaServer, "#FFFFFF"], ["db", FA.FaDatabase, "#FFFFFF"],
  ["robot", FA.FaRobot, "#FFFFFF"], ["pdf", FA.FaFilePdf, "#FFFFFF"], ["chunk", FA.FaAlignLeft, "#FFFFFF"],
  ["vector", FA.FaVectorSquare, "#FFFFFF"], ["search", FA.FaSearch, "#FFFFFF"], ["magic", FA.FaMagic, "#FFFFFF"],
  ["qlist", FA.FaClipboardList, "#FFFFFF"], ["usercheck", FA.FaUserCheck, "#FFFFFF"], ["win", FA.FaWindowRestore, "#FFFFFF"],
  ["eye", FA.FaEye, "#FFFFFF"], ["wifi", FA.FaWifi, "#FFFFFF"], ["shield", FA.FaShieldAlt, "#FFFFFF"],
  ["student", FA.FaUserGraduate, "#FFFFFF"], ["teacher", FA.FaChalkboardTeacher, "#FFFFFF"], ["admin", FA.FaUserShield, "#FFFFFF"],
  ["warn", FA.FaExclamationTriangle, "#FFFFFF"], ["clock", FA.FaUserClock, "#FFFFFF"], ["bulb", FA.FaLightbulb, "#FFFFFF"],
  ["target", FA.FaBullseye, "#FFFFFF"], ["scope", FA.FaObjectGroup, "#FFFFFF"], ["book", FA.FaBookOpen, "#FFFFFF"],
  ["diagram", FA.FaProjectDiagram, "#FFFFFF"], ["flask", FA.FaFlask, "#FFFFFF"], ["flag", FA.FaFlagCheckered, "#FFFFFF"],
  ["list", FA.FaListUl, "#FFFFFF"], ["chart", FA.FaChartBar, "#FFFFFF"], ["java", FA.FaJava, "#FFFFFF"],
  ["trophy", FA.FaTrophy, "#FFFFFF"], ["check", FA.FaCheckCircle, "#FFFFFF"], ["lock", FA.FaLock, "#FFFFFF"],
  ["search2", FA.FaSearchPlus, "#FFFFFF"], ["gears", FA.FaCogs, "#FFFFFF"], ["pen", FA.FaPenFancy, "#FFFFFF"],
  ["grad", FA.FaGraduationCap, "#FFFFFF"], ["compare", FA.FaBalanceScale, "#FFFFFF"], ["rocket", FA.FaRocket, "#FFFFFF"],
];

const pres = new pptxgen();
pres.defineLayout({ name: "W", width: 13.333, height: 7.5 });
pres.layout = "W";
pres.author = "Thừa Văn An";
pres.title = "EduExam AI — Đồ án tốt nghiệp (v2)";
const W = 13.333, H = 7.5;
const mkShadow = () => ({ type: "outer", color: "0E2A47", blur: 7, offset: 3, angle: 135, opacity: 0.16 });

let pageNo = 0;
function content(title, iconKey, iconColor) {
  const s = pres.addSlide();
  s.background = { color: WHITE };
  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: W, h: 1.05, fill: { color: NAVY } });
  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 1.05, w: W, h: 0.06, fill: { color: TEAL } });
  if (iconKey) {
    s.addShape(pres.shapes.OVAL, { x: 0.55, y: 0.24, w: 0.58, h: 0.58, fill: { color: iconColor || TEAL } });
    s.addImage({ data: ICONS[iconKey], x: 0.55 + 0.15, y: 0.24 + 0.15, w: 0.28, h: 0.28 });
  }
  s.addText(title, { x: iconKey ? 1.35 : 0.6, y: 0.12, w: 11.4, h: 0.8, margin: 0, fontFace: HF, fontSize: 24, bold: true, color: WHITE, valign: "middle" });
  pageNo++;
  s.addText("EduExam AI — Hệ thống thi trực tuyến tích hợp AI", { x: 0.6, y: 7.05, w: 9, h: 0.35, margin: 0, fontFace: BF, fontSize: 9, color: MUTED });
  s.addText(String(pageNo), { x: 12.4, y: 7.05, w: 0.5, h: 0.35, margin: 0, fontFace: BF, fontSize: 9, color: MUTED, align: "right" });
  return s;
}
function rcard(s, x, y, w, h, fill) {
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y, w, h, fill: { color: fill || BGSOFT }, line: { color: "E2E8F0", width: 1 }, rectRadius: 0.08, shadow: mkShadow() });
}
function circ(s, x, y, d, bg, key) {
  s.addShape(pres.shapes.OVAL, { x, y, w: d, h: d, fill: { color: bg } });
  const id = d * 0.52;
  s.addImage({ data: ICONS[key], x: x + (d - id) / 2, y: y + (d - id) / 2, w: id, h: id });
}
function arrowR(s, x, y) { s.addImage({ data: ICONS.arrowR, x, y, w: 0.5, h: 0.5 }); }
function arrowD(s, x, y) { s.addImage({ data: ICONS.arrowD, x, y, w: 0.45, h: 0.45 }); }
// box trong sơ đồ: nền nhạt, viền màu, icon-circle trên, tiêu đề + mô tả
function dbox(s, x, y, w, h, color, key, title, desc) {
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y, w, h, fill: { color: WHITE }, line: { color, width: 1.5 }, rectRadius: 0.08, shadow: mkShadow() });
  const d = 0.55; circ(s, x + (w - d) / 2, y + 0.15, d, color, key);
  s.addText(title, { x: x + 0.08, y: y + 0.74, w: w - 0.16, h: 0.44, margin: 0, align: "center", valign: "top", fontFace: BF, fontSize: 12.5, bold: true, color: NAVY });
  if (desc) s.addText(desc, { x: x + 0.12, y: y + 1.2, w: w - 0.24, h: h - 1.3, margin: 0, align: "center", valign: "top", fontFace: BF, fontSize: 10, color: MUTED });
}

async function build() {
  for (const [k, C, col] of ICONLIST) ICONS[k] = await renderIcon(C, col);

  // ===== 1. BÌA =====
  {
    const s = pres.addSlide(); s.background = { color: NAVY };
    s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: W, h: 0.18, fill: { color: TEAL } });
    s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 7.32, w: W, h: 0.18, fill: { color: AMBER } });
    circ(s, 6.17, 0.95, 1.0, TEAL, "grad");
    s.addText("TRƯỜNG ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI  •  KHOA CÔNG NGHỆ THÔNG TIN", { x: 0.8, y: 2.15, w: 11.7, h: 0.4, margin: 0, fontFace: BF, fontSize: 12.5, color: "9FB6D4", align: "center", charSpacing: 1 });
    s.addText("ĐỒ ÁN TỐT NGHIỆP", { x: 0.8, y: 2.6, w: 11.7, h: 0.45, margin: 0, fontFace: BF, fontSize: 14, bold: true, color: AMBER, align: "center", charSpacing: 3 });
    s.addText("Hệ thống thi trực tuyến tích hợp AI", { x: 0.8, y: 3.1, w: 11.7, h: 0.85, margin: 0, fontFace: HF, fontSize: 37, bold: true, color: WHITE, align: "center" });
    s.addText("Tự động hóa công tác ra đề và giám sát hành vi gian lận", { x: 0.8, y: 4.0, w: 11.7, h: 0.55, margin: 0, fontFace: HF, fontSize: 18, italic: true, color: "CADCFC", align: "center" });
    s.addShape(pres.shapes.RECTANGLE, { x: 5.17, y: 4.75, w: 3.0, h: 0.035, fill: { color: TEAL } });
    s.addText([
      { text: "GVHD:  ", options: { color: "9FB6D4" } }, { text: "Phạm Văn Hà", options: { bold: true, color: WHITE } },
      { text: "      SVTH:  ", options: { color: "9FB6D4" } }, { text: "Thừa Văn An", options: { bold: true, color: WHITE } },
      { text: "      MSSV:  ", options: { color: "9FB6D4" } }, { text: "2022601712", options: { bold: true, color: WHITE } },
    ], { x: 0.8, y: 5.1, w: 11.7, h: 0.45, margin: 0, fontFace: BF, fontSize: 15, align: "center" });
    s.addText("Ngành Kỹ thuật phần mềm — Khóa 17   •   Hà Nội, 2026", { x: 0.8, y: 5.7, w: 11.7, h: 0.4, margin: 0, fontFace: BF, fontSize: 13, color: "9FB6D4", align: "center" });
  }

  // ===== 2. NỘI DUNG TRÌNH BÀY =====
  {
    const s = content("Nội dung trình bày", "list", TEAL);
    const items = [
      ["warn", AMBER, "Đặt vấn đề & mục tiêu", "Bối cảnh, hai thách thức, phạm vi"],
      ["book", TEAL, "Cơ sở lý thuyết & công nghệ", "Kiến trúc, RAG, giám sát, bảo mật"],
      ["diagram", NAVY2, "Phân tích & thiết kế hệ thống", "Use case, cơ sở dữ liệu, giao diện"],
      ["flask", BERRY, "Kết quả & kiểm thử", "Sản phẩm, bộ kiểm thử, đánh giá AI"],
      ["flag", TEAL, "Kết luận & hướng phát triển", "Kết quả, hạn chế, định hướng"],
    ];
    let y = 1.5;
    for (const [ic, c, t, d] of items) {
      circ(s, 0.85, y, 0.62, c, ic);
      s.addText(t, { x: 1.7, y: y - 0.04, w: 10.8, h: 0.45, margin: 0, fontFace: HF, fontSize: 18, bold: true, color: NAVY });
      s.addText(d, { x: 1.7, y: y + 0.4, w: 10.8, h: 0.35, margin: 0, fontFace: BF, fontSize: 12.5, color: MUTED });
      y += 1.07;
    }
  }

  // ===== 3. ĐẶT VẤN ĐỀ =====
  {
    const s = content("Đặt vấn đề", "warn", AMBER);
    s.addText("Thi trực tuyến là nhu cầu tất yếu của chuyển đổi số giáo dục, nhưng đặt ra hai thách thức chưa được giải quyết trọn vẹn:", { x: 0.6, y: 1.25, w: 12.1, h: 0.55, margin: 0, fontFace: BF, fontSize: 15, color: INK });
    rcard(s, 0.6, 1.95, 5.95, 2.55, BGSOFT);
    circ(s, 0.9, 2.2, 0.85, TEAL, "clock");
    s.addText("Ra đề tốn nhiều công sức", { x: 1.95, y: 2.25, w: 4.4, h: 0.6, margin: 0, fontFace: HF, fontSize: 18, bold: true, color: NAVY, valign: "middle" });
    s.addText("Soạn ngân hàng câu hỏi đủ lớn để mỗi sinh viên một đề khác nhau mất hàng giờ làm thủ công.", { x: 0.95, y: 3.2, w: 5.3, h: 1.1, margin: 0, fontFace: BF, fontSize: 13.5, color: INK });
    rcard(s, 6.78, 1.95, 5.95, 2.55, BGSOFT);
    circ(s, 7.08, 2.2, 0.85, AMBER, "eye");
    s.addText("Khó bảo đảm trung thực", { x: 8.13, y: 2.25, w: 4.4, h: 0.6, margin: 0, fontFace: HF, fontSize: 18, bold: true, color: NAVY, valign: "middle" });
    s.addText("Một giảng viên không thể quan sát hàng chục sinh viên ở nhiều địa điểm khác nhau.", { x: 7.13, y: 3.2, w: 5.3, h: 1.1, margin: 0, fontFace: BF, fontSize: 13.5, color: INK });
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: 0.6, y: 4.75, w: 12.13, h: 1.75, fill: { color: NAVY }, rectRadius: 0.08 });
    circ(s, 0.95, 5.05, 0.85, AMBER, "bulb");
    s.addText("Giải pháp", { x: 2.0, y: 5.0, w: 4, h: 0.45, margin: 0, fontFace: HF, fontSize: 17, bold: true, color: AMBER });
    s.addText("Ứng dụng AI: dùng kỹ thuật RAG sinh câu hỏi trực tiếp từ tài liệu giảng dạy, và giám sát thi bằng sự kiện trình duyệt + nhận diện khuôn mặt phía máy khách — không lưu video, tôn trọng quyền riêng tư.", { x: 2.0, y: 5.45, w: 10.4, h: 0.95, margin: 0, fontFace: BF, fontSize: 14, color: WHITE });
  }

  // ===== 4. MỤC TIÊU & PHẠM VI =====
  {
    const s = content("Mục tiêu & phạm vi", "target", TEAL);
    rcard(s, 0.6, 1.35, 5.95, 5.0);
    circ(s, 0.9, 1.6, 0.7, TEAL, "target");
    s.addText("Mục tiêu", { x: 1.75, y: 1.65, w: 4.5, h: 0.55, margin: 0, fontFace: HF, fontSize: 20, bold: true, color: NAVY, valign: "middle" });
    s.addText([
      { text: "Làm chủ Spring Boot (Java 21), React + TypeScript, PostgreSQL/pgvector và AI (Gemini) qua RAG", options: { bullet: true, breakLine: true } },
      { text: "Tự động sinh đề thi từ tài liệu giảng dạy", options: { bullet: true, breakLine: true } },
      { text: "Giám sát gian lận thời gian thực, bảo đảm quyền riêng tư", options: { bullet: true, breakLine: true } },
      { text: "Xây dựng hệ thống thi trực tuyến hoàn chỉnh ba vai trò", options: { bullet: true } },
    ], { x: 0.95, y: 2.55, w: 5.35, h: 3.55, margin: 0, fontFace: BF, fontSize: 14.5, color: INK, paraSpaceAfter: 11, valign: "top" });
    rcard(s, 6.78, 1.35, 5.95, 5.0);
    circ(s, 7.08, 1.6, 0.7, AMBER, "scope");
    s.addText("Phạm vi", { x: 7.93, y: 1.65, w: 4.5, h: 0.55, margin: 0, fontFace: HF, fontSize: 20, bold: true, color: NAVY, valign: "middle" });
    s.addText([
      { text: "Ứng dụng web ba vai trò: Sinh viên, Giảng viên, Quản trị viên", options: { bullet: true, breakLine: true } },
      { text: "Xác thực & phân quyền; quản lý lớp và ngân hàng câu hỏi", options: { bullet: true, breakLine: true } },
      { text: "Sinh đề RAG; làm bài thi có tự động lưu, khôi phục", options: { bullet: true, breakLine: true } },
      { text: "Giám sát thi; chấm điểm tự động & báo cáo kết quả", options: { bullet: true, breakLine: true } },
      { text: "Công cụ hỗ trợ giám sát, không thay thế giám thị; không thương mại", options: { bullet: true } },
    ], { x: 7.13, y: 2.55, w: 5.35, h: 3.55, margin: 0, fontFace: BF, fontSize: 14.5, color: INK, paraSpaceAfter: 9, valign: "top" });
  }

  // ===== 5. KHẢO SÁT HIỆN TRẠNG =====
  {
    const s = content("Khảo sát hiện trạng", "compare", NAVY2);
    s.addText("Phần lớn công cụ phổ biến mới dừng ở mức số hóa quy trình thi; hầu như chưa có công cụ nào sinh câu hỏi tự động từ tài liệu của giảng viên.", { x: 0.6, y: 1.28, w: 12.1, h: 0.5, margin: 0, fontFace: BF, fontSize: 13.5, color: INK });
    const hd = (t) => ({ text: t, options: { fill: { color: NAVY }, color: WHITE, bold: true, align: "center", valign: "middle", fontFace: BF, fontSize: 13 } });
    const rows = [
      [hd("Hệ thống"), hd("Sinh đề bằng AI"), hd("Chống gian lận"), hd("Tiếng Việt"), hd("Chi phí")],
      ["Google Forms", "Không", "Không", "Tốt", "Miễn phí"],
      ["Microsoft Forms", "Không", "Không", "Tốt", "Theo gói Office"],
      ["Moodle Quiz", "Không (random)", "Hạn chế (plugin)", "Tốt", "Mã nguồn mở"],
      ["Azota", "Không", "Cơ bản", "Tốt", "Có gói trả phí"],
      [{ text: "EduExam AI", options: { bold: true, color: NAVY } }, { text: "Có (RAG)", options: { bold: true, color: TEAL } }, { text: "Hai lớp", options: { bold: true, color: TEAL } }, "Tốt", "Miễn phí"],
    ];
    s.addTable(rows, { x: 0.6, y: 1.95, w: 12.13, colW: [3.0, 2.6, 2.6, 1.93, 2.0], rowH: 0.6, fontFace: BF, fontSize: 13, color: INK, valign: "middle", align: "center", border: { pt: 0.5, color: "D5DEE9" }, fill: { color: WHITE } });
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: 0.6, y: 5.95, w: 12.13, h: 0.85, fill: { color: BGSOFT }, line: { color: TEAL, width: 1 }, rectRadius: 0.06 });
    circ(s, 0.85, 6.08, 0.58, TEAL, "check");
    s.addText("Khoảng trống: sinh đề tự động từ chính tài liệu giảng dạy — chính là hướng đề tài tập trung khai thác.", { x: 1.6, y: 5.95, w: 10.9, h: 0.85, margin: 0, fontFace: BF, fontSize: 13.5, italic: true, color: NAVY, valign: "middle" });
  }

  // ===== 6. CÔNG NGHỆ =====
  {
    const s = content("Công nghệ sử dụng", "gears", BERRY);
    const blocks = [
      ["java", "Backend", TEAL, ["Java 21 (LTS)", "Spring Boot 3.3", "Security + JPA + WebSocket", "LangChain4j, Apache Tika"]],
      ["react", "Frontend", NAVY2, ["React 18 + Vite 5", "TypeScript (strict)", "Tailwind + shadcn/ui", "MediaPipe (WASM)"]],
      ["robot", "AI / LLM", AMBER, ["Google Gemini Flash", "gemini-embedding-001 (768D)", "Kỹ thuật RAG", "Human-in-the-loop"]],
      ["db", "Cơ sở dữ liệu", BERRY, ["PostgreSQL 16", "pgvector (HNSW)", "49 bảng dữ liệu", "Docker Compose"]],
    ];
    let x = 0.6;
    for (const [ic, t, c, items] of blocks) {
      rcard(s, x, 1.45, 2.93, 4.95, WHITE);
      circ(s, x + (2.93 - 0.8) / 2, 1.7, 0.8, c, ic);
      s.addText(t, { x, y: 2.55, w: 2.93, h: 0.45, margin: 0, align: "center", fontFace: HF, fontSize: 16, bold: true, color: NAVY });
      s.addShape(pres.shapes.RECTANGLE, { x: x + 0.6, y: 3.05, w: 1.73, h: 0.025, fill: { color: c } });
      s.addText(items.map((it, i) => ({ text: it, options: { bullet: true, breakLine: i < items.length - 1 } })),
        { x: x + 0.28, y: 3.2, w: 2.45, h: 3.0, margin: 0, fontFace: BF, fontSize: 12.5, color: INK, paraSpaceAfter: 9, valign: "top" });
      x += 3.04;
    }
  }

  // ===== 7. KIẾN TRÚC (vẽ) =====
  {
    const s = content("Kiến trúc tổng thể hệ thống", "diagram", NAVY2);
    // AI box trên cùng
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: 4.55, y: 1.4, w: 4.25, h: 0.95, fill: { color: AMBER }, rectRadius: 0.08, shadow: mkShadow() });
    s.addImage({ data: ICONS.robot, x: 4.85, y: 1.62, w: 0.5, h: 0.5 });
    s.addText("Google Gemini — sinh đề RAG (LangChain4j)", { x: 5.45, y: 1.4, w: 3.25, h: 0.95, margin: 0, valign: "middle", fontFace: BF, fontSize: 13, bold: true, color: WHITE });
    arrowD(s, 6.45, 2.4);
    // 3 khối chính
    dbox(s, 0.7, 3.0, 3.0, 2.7, NAVY2, "react", "Trình duyệt (Client)", "React 18 + Vite + TypeScript; MediaPipe nhận diện khuôn mặt; WebSocket/STOMP client");
    dbox(s, 5.15, 3.0, 3.05, 2.7, TEAL, "server", "Backend — Spring Boot", "Mô-đun: auth, course, question, ai, exam, attempt, proctoring... Web → Service → Repository");
    dbox(s, 9.65, 3.0, 3.0, 2.7, BERRY, "db", "PostgreSQL 16", "+ pgvector (HNSW); 49 bảng dữ liệu; lưu & truy vấn embedding");
    arrowR(s, 3.78, 4.1); arrowR(s, 8.28, 4.1);
    s.addText("REST / WebSocket", { x: 3.5, y: 3.72, w: 1.6, h: 0.3, margin: 0, align: "center", fontFace: BF, fontSize: 10, italic: true, color: MUTED });
    s.addText("JPA", { x: 8.1, y: 3.72, w: 1.0, h: 0.3, margin: 0, align: "center", fontFace: BF, fontSize: 10, italic: true, color: MUTED });
    s.addText("Kiến trúc Monolith mô-đun hóa, phân lớp rõ ràng; backend không trạng thái với JWT trong header.", { x: 0.7, y: 5.95, w: 11.95, h: 0.5, margin: 0, align: "center", fontFace: BF, fontSize: 13, italic: true, color: NAVY });
  }

  // ===== 8. PIPELINE RAG (vẽ) =====
  {
    const s = content("Nhánh AI 1 — Sinh đề tự động (RAG)", "magic", AMBER);
    // Pha 1
    s.addShape(pres.shapes.RECTANGLE, { x: 0.6, y: 1.5, w: 0.12, h: 1.65, fill: { color: TEAL } });
    s.addText("Pha lập chỉ mục", { x: 0.85, y: 1.5, w: 3.0, h: 0.4, margin: 0, fontFace: HF, fontSize: 14, bold: true, color: TEAL });
    const r1 = [["pdf", "Tài liệu", "PDF/DOCX"], ["chunk", "Bóc tách + chia đoạn", "Tika, ~800 token"], ["vector", "Embedding", "768 chiều"], ["db", "pgvector", "document_chunks"]];
    let x = 0.85, y1 = 1.95; const bw = 2.7, gap = 0.42;
    r1.forEach((b, i) => { dbox(s, x, y1, bw, 1.55, TEAL, b[0], b[1], b[2]); if (i < r1.length - 1) arrowR(s, x + bw + (gap - 0.5) / 2, y1 + 0.5); x += bw + gap; });
    // Pha 2
    s.addShape(pres.shapes.RECTANGLE, { x: 0.6, y: 3.95, w: 0.12, h: 1.65, fill: { color: AMBER } });
    s.addText("Pha sinh câu hỏi", { x: 0.85, y: 3.95, w: 3.0, h: 0.4, margin: 0, fontFace: HF, fontSize: 14, bold: true, color: AMBER });
    const r2 = [["search", "Truy hồi", "top-8 cosine"], ["robot", "Gemini Flash", "prompt + ngữ cảnh"], ["qlist", "Câu hỏi", "kèm trích dẫn"], ["usercheck", "Giảng viên duyệt", "human-in-the-loop"]];
    x = 0.85; let y2 = 4.4;
    r2.forEach((b, i) => { dbox(s, x, y2, bw, 1.55, AMBER, b[0], b[1], b[2]); if (i < r2.length - 1) arrowR(s, x + bw + (gap - 0.5) / 2, y2 + 0.5); x += bw + gap; });
    s.addText("Kết quả bám sát tài liệu, có thể kiểm chứng; chỉ câu được duyệt mới lưu vào ngân hàng.", { x: 0.85, y: 6.1, w: 11.7, h: 0.4, margin: 0, align: "center", fontFace: BF, fontSize: 12.5, italic: true, color: NAVY });
  }

  // ===== 9. GIÁM SÁT (vẽ) =====
  {
    const s = content("Nhánh AI 2 — Giám sát thi (Proctoring)", "shield", TEAL);
    // cụm máy khách
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: 0.6, y: 1.55, w: 4.0, h: 4.5, fill: { color: BGSOFT }, line: { color: NAVY2, width: 1.5, dashType: "dash" }, rectRadius: 0.08 });
    s.addText("Phía máy khách (không lưu video)", { x: 0.6, y: 1.65, w: 4.0, h: 0.4, margin: 0, align: "center", fontFace: BF, fontSize: 12.5, bold: true, color: NAVY2 });
    dbox(s, 0.85, 2.18, 3.5, 1.78, NAVY2, "win", "Sự kiện trình duyệt", "Chuyển tab, thoát toàn màn hình, sao chép/dán, mở devtools");
    dbox(s, 0.85, 4.12, 3.5, 1.78, NAVY2, "eye", "Nhận diện khuôn mặt", "MediaPipe (WebAssembly): vắng mặt, nhiều mặt, quay đi");
    // websocket
    arrowR(s, 4.7, 3.6);
    circ(s, 5.35, 3.05, 1.0, TEAL, "wifi");
    s.addText("WebSocket / STOMP", { x: 4.85, y: 4.1, w: 2.0, h: 0.3, margin: 0, align: "center", fontFace: BF, fontSize: 11, bold: true, color: TEAL });
    s.addText("xác thực JWT, tự kết nối lại", { x: 4.7, y: 4.38, w: 2.3, h: 0.3, margin: 0, align: "center", fontFace: BF, fontSize: 9.5, italic: true, color: MUTED });
    arrowR(s, 6.9, 3.6);
    // server
    dbox(s, 7.55, 2.55, 2.5, 2.1, TEAL, "server", "ProctoringService", "Nhận & xử lý sự kiện, gắn mức độ nghiêm trọng");
    arrowR(s, 10.1, 3.4);
    dbox(s, 10.65, 2.55, 2.1, 2.1, BERRY, "db", "proctoring_events", "Nhật ký vi phạm (không tự trừ điểm)");
    s.addText("Vi phạm chỉ ghi log; giảng viên xem dòng thời gian. Tuân thủ Nghị định 13/2023/NĐ-CP (có đồng ý).", { x: 0.6, y: 6.2, w: 12.1, h: 0.4, margin: 0, align: "center", fontFace: BF, fontSize: 12.5, italic: true, color: NAVY });
  }

  // ===== 10. PHÂN TÍCH & THIẾT KẾ (use case vẽ) =====
  {
    const s = content("Phân tích & thiết kế — Tác nhân & chức năng", "diagram", NAVY2);
    const roles = [
      ["student", TEAL, "Sinh viên", ["Tham gia lớp bằng mã", "Làm bài thi có giám sát", "Luyện tập, ôn tập SM-2", "Xem điểm & phân tích"]],
      ["teacher", NAVY2, "Giảng viên", ["Quản lý lớp & câu hỏi", "Sinh đề bằng AI", "Tạo & cấu hình kỳ thi", "Xem kết quả & vi phạm"]],
      ["admin", AMBER, "Quản trị viên", ["Quản lý tài khoản", "Quản lý dữ liệu nền", "Dashboard tổng quan", "Nhật ký kiểm toán"]],
    ];
    s.addText("Hệ thống có ba tác nhân; mỗi tác nhân đảm nhận các nhóm chức năng riêng (mô hình hóa bằng biểu đồ use case):", { x: 0.6, y: 1.25, w: 12.1, h: 0.45, margin: 0, fontFace: BF, fontSize: 13.5, color: INK });
    let x = 0.6;
    for (const [ic, c, t, items] of roles) {
      rcard(s, x, 1.85, 3.91, 4.65, WHITE);
      s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: 1.85, w: 3.91, h: 1.45, fill: { color: c }, rectRadius: 0.08 });
      s.addImage({ data: ICONS[ic], x: x + (3.91 - 0.72) / 2, y: 2.0, w: 0.72, h: 0.72 });
      s.addText(t, { x, y: 2.74, w: 3.91, h: 0.5, margin: 0, align: "center", fontFace: HF, fontSize: 17, bold: true, color: WHITE });
      s.addText(items.map((it, i) => ({ text: it, options: { bullet: true, breakLine: i < items.length - 1 } })),
        { x: x + 0.38, y: 3.55, w: 3.2, h: 2.7, margin: 0, fontFace: BF, fontSize: 13.5, color: INK, paraSpaceAfter: 12, valign: "top" });
      x += 4.06;
    }
  }

  // ===== 11. CHỨC NĂNG CHÍNH =====
  {
    const s = content("Các chức năng chính", "list", TEAL);
    const cols = [
      ["qlist", "Ngân hàng câu hỏi", TEAL, ["8 loại câu hỏi", "Phiên bản, thẻ, thư mục", "Bộ lọc thông minh, thùng rác", "Phát hiện trùng ngữ nghĩa", "Trợ lý AI soạn thảo"]],
      ["flask", "Thi & chấm điểm", NAVY2, ["Kỳ thi cố định/ngẫu nhiên", "Đếm ngược theo giờ máy chủ", "Tự động lưu & khôi phục", "Tự nộp khi hết giờ", "Chấm tự động, export Excel"]],
      ["trophy", "Học tập & quản trị", AMBER, ["Luyện tập, ôn tập SM-2", "Trò chơi hóa, xếp hạng", "Phân tích IDI / IRT", "Quản lý tài khoản, dữ liệu nền", "Nhật ký kiểm toán"]],
    ];
    let x = 0.6;
    for (const [ic, t, c, items] of cols) {
      rcard(s, x, 1.45, 3.91, 4.95, WHITE);
      s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y: 1.45, w: 3.91, h: 0.95, fill: { color: c }, rectRadius: 0.08 });
      s.addImage({ data: ICONS[ic], x: x + 0.3, y: 1.66, w: 0.52, h: 0.52 });
      s.addText(t, { x: x + 0.95, y: 1.45, w: 2.9, h: 0.95, margin: 0, valign: "middle", fontFace: HF, fontSize: 15.5, bold: true, color: WHITE });
      s.addText(items.map((it, i) => ({ text: it, options: { bullet: true, breakLine: i < items.length - 1 } })),
        { x: x + 0.3, y: 2.6, w: 3.35, h: 3.6, margin: 0, fontFace: BF, fontSize: 13, color: INK, paraSpaceAfter: 10, valign: "top" });
      x += 4.06;
    }
  }

  // ===== 12. KIỂM THỬ =====
  {
    const s = content("Kết quả kiểm thử", "flask", BERRY);
    const stat = (x, ic, num, lbl, c) => {
      rcard(s, x, 1.5, 2.86, 2.05, WHITE);
      circ(s, x + 0.25, 1.72, 0.62, c, ic);
      s.addText(num, { x: x + 0.95, y: 1.68, w: 1.8, h: 0.7, margin: 0, fontFace: HF, fontSize: 28, bold: true, color: c, valign: "middle" });
      s.addText(lbl, { x: x + 0.2, y: 2.5, w: 2.5, h: 0.85, margin: 0, fontFace: BF, fontSize: 12, color: MUTED });
    };
    stat(0.6, "check", "5.106", "ca kiểm thử tự động", TEAL);
    stat(3.66, "list", "440", "tệp test (BE + FE)", NAVY2);
    stat(6.72, "check", "0", "ca thất bại (all-pass)", TEAL);
    stat(9.78, "gears", "~50", "lớp test tích hợp", AMBER);
    rcard(s, 0.6, 3.85, 6.0, 2.65, WHITE);
    s.addText("Độ phủ mã (coverage)", { x: 0.9, y: 4.0, w: 5.4, h: 0.45, margin: 0, fontFace: HF, fontSize: 16, bold: true, color: NAVY });
    s.addChart(pres.charts.BAR, [{ name: "Coverage", labels: ["Backend", "Frontend"], values: [91.3, 84.4] }], {
      x: 0.8, y: 4.5, w: 5.6, h: 1.9, barDir: "col", chartColors: [TEAL, NAVY2], showValue: true,
      dataLabelPosition: "outEnd", dataLabelColor: INK, dataLabelFontSize: 12, showLegend: false,
      catAxisLabelColor: MUTED, catAxisLabelFontSize: 12, valAxisMaxVal: 100, valAxisMinVal: 0,
      chartArea: { fill: { color: "FFFFFF" } },
    });
    rcard(s, 6.78, 3.85, 5.95, 2.65, BGSOFT);
    s.addText("Chiến lược kiểm thử", { x: 7.05, y: 4.0, w: 5.4, h: 0.45, margin: 0, fontFace: HF, fontSize: 16, bold: true, color: NAVY });
    s.addText([
      { text: "BE: JUnit 5 + Mockito + AssertJ; Testcontainers + REST Assured", options: { bullet: true, breakLine: true } },
      { text: "FE: Vitest + React Testing Library + MSW", options: { bullet: true, breakLine: true } },
      { text: "Ưu tiên độ phủ cao cho các bộ chấm điểm", options: { bullet: true } },
    ], { x: 7.05, y: 4.5, w: 5.45, h: 1.9, margin: 0, fontFace: BF, fontSize: 13, color: INK, paraSpaceAfter: 10, valign: "top" });
  }

  // ===== 13. ĐÁNH GIÁ AI =====
  {
    const s = content("Đánh giá các thành phần AI", "robot", AMBER);
    s.addText("Hai thành phần AI được đánh giá riêng bằng thực nghiệm (kết quả mang tính xác suất). Mục tiêu đề ra:", { x: 0.6, y: 1.28, w: 12.1, h: 0.5, margin: 0, fontFace: BF, fontSize: 15, color: INK });
    rcard(s, 0.6, 2.0, 5.95, 4.45, WHITE);
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: 0.6, y: 2.0, w: 5.95, h: 0.95, fill: { color: TEAL }, rectRadius: 0.08 });
    s.addImage({ data: ICONS.magic, x: 0.95, y: 2.22, w: 0.5, h: 0.5 });
    s.addText("Sinh đề (RAG)", { x: 1.6, y: 2.0, w: 4.8, h: 0.95, margin: 0, valign: "middle", fontFace: HF, fontSize: 17, bold: true, color: WHITE });
    s.addText([
      { text: "Sinh 10 câu trong dưới 60 giây", options: { bullet: true, breakLine: true } },
      { text: "Trên 70% câu chấp nhận không cần sửa", options: { bullet: true, breakLine: true } },
      { text: "Trích dẫn đúng, bám sát tài liệu nguồn", options: { bullet: true } },
    ], { x: 1.0, y: 3.2, w: 5.3, h: 3.0, margin: 0, fontFace: BF, fontSize: 15, color: INK, paraSpaceAfter: 14, valign: "top" });
    rcard(s, 6.78, 2.0, 5.95, 4.45, WHITE);
    s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: 6.78, y: 2.0, w: 5.95, h: 0.95, fill: { color: NAVY2 }, rectRadius: 0.08 });
    s.addImage({ data: ICONS.shield, x: 7.13, y: 2.22, w: 0.5, h: 0.5 });
    s.addText("Giám sát", { x: 7.78, y: 2.0, w: 4.8, h: 0.95, margin: 0, valign: "middle", fontFace: HF, fontSize: 17, bold: true, color: WHITE });
    s.addText([
      { text: "Phát hiện chuyển tab ≥ 90%", options: { bullet: true, breakLine: true } },
      { text: "Nhận diện khuôn mặt ≥ 80%", options: { bullet: true, breakLine: true } },
      { text: "Tỷ lệ báo nhầm dưới 15%", options: { bullet: true } },
    ], { x: 7.18, y: 3.2, w: 5.3, h: 3.0, margin: 0, fontFace: BF, fontSize: 15, color: INK, paraSpaceAfter: 14, valign: "top" });
  }

  // ===== 14. KẾT QUẢ ĐẠT ĐƯỢC =====
  {
    const s = content("Kết luận — Kết quả đạt được", "flag", TEAL);
    const items = [
      ["check", TEAL, "Sản phẩm", "Ứng dụng web hoàn chỉnh ba vai trò: xác thực & phân quyền, quản lý lớp & ngân hàng câu hỏi đa dạng, làm bài thi tự động lưu/khôi phục, chấm điểm tự động, quản trị."],
      ["magic", AMBER, "Nhánh AI 1", "Pipeline RAG hoàn chỉnh: upload → embedding pgvector → truy hồi ngữ nghĩa → sinh câu hỏi kèm trích dẫn, có review của giảng viên."],
      ["shield", NAVY2, "Nhánh AI 2", "Giám sát thời gian thực kết hợp sự kiện trình duyệt và nhận diện khuôn mặt phía máy khách, truyền qua WebSocket, tôn trọng quyền riêng tư."],
      ["gears", BERRY, "Kỹ thuật", "Kiến trúc monolith mô-đun, REST + WebSocket, bảo mật JWT + RBAC, bộ kiểm thử lớn (5.106 ca, phủ 84–91%)."],
    ];
    let y = 1.45;
    for (const [ic, c, t, d] of items) {
      circ(s, 0.65, y, 0.62, c, ic);
      s.addText(t, { x: 1.5, y: y - 0.02, w: 2.6, h: 0.5, margin: 0, fontFace: HF, fontSize: 17, bold: true, color: NAVY, valign: "middle" });
      s.addText(d, { x: 4.1, y: y - 0.05, w: 8.6, h: 1.15, margin: 0, fontFace: BF, fontSize: 13.5, color: INK });
      y += 1.28;
    }
  }

  // ===== 15. HẠN CHẾ & HƯỚNG PHÁT TRIỂN =====
  {
    const s = content("Hạn chế & hướng phát triển", "rocket", AMBER);
    rcard(s, 0.6, 1.4, 5.95, 5.0, BGSOFT);
    circ(s, 0.9, 1.62, 0.65, AMBER, "warn");
    s.addText("Hạn chế", { x: 1.7, y: 1.62, w: 4.5, h: 0.55, margin: 0, fontFace: HF, fontSize: 19, bold: true, color: NAVY, valign: "middle" });
    s.addText([
      { text: "Chất lượng câu hỏi phụ thuộc mô hình & tài liệu; cần giảng viên review", options: { bullet: true, breakLine: true } },
      { text: "Giám sát mới ở mức hỗ trợ; chưa bắt gian lận tinh vi, có thể báo nhầm", options: { bullet: true, breakLine: true } },
      { text: "Chạy trên một máy chủ đơn, chưa tối ưu mở rộng ngang", options: { bullet: true, breakLine: true } },
      { text: "Gói Gemini miễn phí có giới hạn tần suất", options: { bullet: true } },
    ], { x: 0.95, y: 2.4, w: 5.35, h: 3.8, margin: 0, fontFace: BF, fontSize: 14, color: INK, paraSpaceAfter: 12, valign: "top" });
    rcard(s, 6.78, 1.4, 5.95, 5.0, BGSOFT);
    circ(s, 7.08, 1.62, 0.65, TEAL, "rocket");
    s.addText("Hướng phát triển", { x: 7.88, y: 1.62, w: 4.5, h: 0.55, margin: 0, fontFace: HF, fontSize: 19, bold: true, color: NAVY, valign: "middle" });
    s.addText([
      { text: "Chấm tự luận bằng LLM; đa nhà cung cấp mô hình", options: { bullet: true, breakLine: true } },
      { text: "Phát hiện vật thể, theo dõi hướng nhìn, âm thanh", options: { bullet: true, breakLine: true } },
      { text: "Ứng dụng di động; tích hợp LMS; SSO/OAuth", options: { bullet: true, breakLine: true } },
      { text: "Tách dịch vụ, đa trường, triển khai cloud", options: { bullet: true, breakLine: true } },
      { text: "Cá nhân hóa lộ trình ôn tập bằng học máy", options: { bullet: true } },
    ], { x: 7.13, y: 2.4, w: 5.35, h: 3.8, margin: 0, fontFace: BF, fontSize: 14, color: INK, paraSpaceAfter: 10, valign: "top" });
  }

  // ===== 16. CẢM ƠN =====
  {
    const s = pres.addSlide(); s.background = { color: NAVY };
    s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: W, h: 0.18, fill: { color: TEAL } });
    s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 7.32, w: W, h: 0.18, fill: { color: AMBER } });
    circ(s, 6.17, 1.85, 1.0, TEAL, "grad");
    s.addText("Trân trọng cảm ơn", { x: 0.8, y: 3.05, w: 11.7, h: 1.0, margin: 0, fontFace: HF, fontSize: 44, bold: true, color: WHITE, align: "center" });
    s.addText("Kính mong nhận được ý kiến đóng góp của quý thầy cô", { x: 0.8, y: 4.2, w: 11.7, h: 0.6, margin: 0, fontFace: HF, fontSize: 19, italic: true, color: "CADCFC", align: "center" });
    s.addShape(pres.shapes.RECTANGLE, { x: 5.67, y: 5.05, w: 2.0, h: 0.035, fill: { color: TEAL } });
    s.addText("Sinh viên: Thừa Văn An   •   GVHD: Phạm Văn Hà", { x: 0.8, y: 5.3, w: 11.7, h: 0.4, margin: 0, fontFace: BF, fontSize: 14, color: "9FB6D4", align: "center" });
  }

  await pres.writeFile({ fileName: OUT });
  console.log("OK ->", OUT);
}
build().catch((e) => { console.error(e); process.exit(1); });
