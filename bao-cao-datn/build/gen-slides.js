/* Sinh slide bảo vệ ĐATN EduExam AI (.pptx) bằng pptxgenjs.
 * Chạy: cd bao-cao/build && node gen-slides.js  -> ../Slide-BaoVe-EduExam.pptx
 */
const path = require("path");
const fs = require("fs");
const pptxgen = require("pptxgenjs");

const ASSETS = path.join(__dirname, "..", "assets");
const OUT = path.join(__dirname, "..", "Slide-BaoVe-EduExam.pptx");

// ---- palette (navy + teal + amber) ----
const NAVY = "0E2A47", NAVY2 = "16395E", TEAL = "0D9488", TEALL = "14B8A6";
const AMBER = "F59E0B", BGSOFT = "F4F7FB", INK = "1E293B", MUTED = "64748B", WHITE = "FFFFFF";
const HF = "Georgia", BF = "Calibri"; // header / body font

const pres = new pptxgen();
pres.defineLayout({ name: "W", width: 13.333, height: 7.5 });
pres.layout = "W";
pres.author = "Thừa Văn An";
pres.title = "EduExam AI — Đồ án tốt nghiệp";
const W = 13.333, H = 7.5;

const shadow = () => ({ type: "outer", color: "0E2A47", blur: 7, offset: 3, angle: 135, opacity: 0.18 });

function pngWH(p) { const b = fs.readFileSync(p); return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) }; }
function img(slide, name, box) { // box: {x,y, maxW, maxH} -> contain
  const p = path.join(ASSETS, name);
  const { w, h } = pngWH(p);
  const s = Math.min(box.maxW / w, box.maxH / h);
  const W2 = w * s, H2 = h * s;
  slide.addImage({ path: p, x: box.x + (box.maxW - W2) / 2, y: box.y + (box.maxH - H2) / 2, w: W2, h: H2 });
}

let pageNo = 0;
function content(title) {
  const s = pres.addSlide();
  s.background = { color: WHITE };
  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: W, h: 1.05, fill: { color: NAVY } });
  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 1.05, w: W, h: 0.06, fill: { color: TEAL } });
  s.addText(title, { x: 0.6, y: 0.12, w: 12.1, h: 0.8, margin: 0, fontFace: HF, fontSize: 25, bold: true, color: WHITE, valign: "middle" });
  pageNo++;
  s.addText("EduExam AI — Hệ thống thi trực tuyến tích hợp AI", { x: 0.6, y: 7.05, w: 9, h: 0.35, margin: 0, fontFace: BF, fontSize: 9, color: MUTED });
  s.addText(String(pageNo), { x: 12.4, y: 7.05, w: 0.5, h: 0.35, margin: 0, fontFace: BF, fontSize: 9, color: MUTED, align: "right" });
  return s;
}
function card(s, x, y, w, h, fill) {
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x, y, w, h, fill: { color: fill || BGSOFT }, line: { color: "E2E8F0", width: 1 }, rectRadius: 0.08, shadow: shadow() });
}
function chip(s, x, y, label, color) {
  s.addShape(pres.shapes.OVAL, { x, y, w: 0.5, h: 0.5, fill: { color } });
  s.addText(label, { x, y, w: 0.5, h: 0.5, margin: 0, align: "center", valign: "middle", fontFace: HF, fontSize: 18, bold: true, color: WHITE });
}

// ============ 1. BÌA ============
{
  const s = pres.addSlide();
  s.background = { color: NAVY };
  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: W, h: 0.18, fill: { color: TEAL } });
  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 7.32, w: W, h: 0.18, fill: { color: AMBER } });
  s.addText("TRƯỜNG ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI  •  KHOA CÔNG NGHỆ THÔNG TIN", { x: 0.8, y: 0.55, w: 11.7, h: 0.4, margin: 0, fontFace: BF, fontSize: 13, color: "9FB6D4", align: "center", charSpacing: 1 });
  s.addText("ĐỒ ÁN TỐT NGHIỆP", { x: 0.8, y: 1.15, w: 11.7, h: 0.5, margin: 0, fontFace: BF, fontSize: 15, bold: true, color: AMBER, align: "center", charSpacing: 3 });
  s.addText("Hệ thống thi trực tuyến tích hợp AI", { x: 0.8, y: 1.95, w: 11.7, h: 0.9, margin: 0, fontFace: HF, fontSize: 38, bold: true, color: WHITE, align: "center" });
  s.addText("Tự động hóa công tác ra đề và giám sát hành vi gian lận", { x: 0.8, y: 2.95, w: 11.7, h: 0.6, margin: 0, fontFace: HF, fontSize: 19, italic: true, color: "CADCFC", align: "center" });
  s.addShape(pres.shapes.RECTANGLE, { x: 5.17, y: 3.85, w: 3.0, h: 0.04, fill: { color: TEAL } });
  s.addText([
    { text: "Giảng viên hướng dẫn:  ", options: { color: "9FB6D4" } }, { text: "Phạm Văn Hà", options: { bold: true, color: WHITE, breakLine: true } },
    { text: "Sinh viên thực hiện:  ", options: { color: "9FB6D4" } }, { text: "Thừa Văn An", options: { bold: true, color: WHITE, breakLine: true } },
    { text: "Mã số sinh viên:  ", options: { color: "9FB6D4" } }, { text: "2022601712", options: { bold: true, color: WHITE, breakLine: true } },
    { text: "Ngành:  ", options: { color: "9FB6D4" } }, { text: "Kỹ thuật phần mềm — Khóa 17", options: { bold: true, color: WHITE } },
  ], { x: 3.8, y: 4.25, w: 5.7, h: 2.0, margin: 0, fontFace: BF, fontSize: 15, align: "center", lineSpacingMultiple: 1.35 });
  s.addText("Hà Nội — 2026", { x: 0.8, y: 6.6, w: 11.7, h: 0.4, margin: 0, fontFace: BF, fontSize: 13, color: "9FB6D4", align: "center" });
}

// ============ 2. NỘI DUNG TRÌNH BÀY ============
{
  const s = content("Nội dung trình bày");
  const items = [
    ["1", "Đặt vấn đề & mục tiêu", "Bối cảnh, hai thách thức, phạm vi đề tài"],
    ["2", "Cơ sở lý thuyết & công nghệ", "Kiến trúc, RAG, giám sát, bảo mật"],
    ["3", "Phân tích & thiết kế hệ thống", "Use case, cơ sở dữ liệu, giao diện"],
    ["4", "Kết quả & kiểm thử", "Sản phẩm, bộ kiểm thử, đánh giá AI"],
    ["5", "Kết luận & hướng phát triển", "Kết quả đạt được, hạn chế, định hướng"],
  ];
  let y = 1.55;
  for (const [n, t, d] of items) {
    chip(s, 0.9, y + 0.05, n, n === "3" ? AMBER : TEAL);
    s.addText(t, { x: 1.6, y: y - 0.05, w: 10.8, h: 0.45, margin: 0, fontFace: HF, fontSize: 19, bold: true, color: NAVY });
    s.addText(d, { x: 1.6, y: y + 0.4, w: 10.8, h: 0.35, margin: 0, fontFace: BF, fontSize: 13, color: MUTED });
    y += 1.08;
  }
}

// ============ 3. ĐẶT VẤN ĐỀ ============
{
  const s = content("Đặt vấn đề");
  s.addText("Thi trực tuyến là nhu cầu tất yếu của chuyển đổi số giáo dục, nhưng đặt ra hai thách thức chưa được giải quyết trọn vẹn:", { x: 0.6, y: 1.3, w: 12.1, h: 0.6, margin: 0, fontFace: BF, fontSize: 15, color: INK });
  card(s, 0.6, 2.1, 5.95, 2.5, BGSOFT);
  s.addText("01", { x: 0.9, y: 2.3, w: 1.5, h: 0.8, margin: 0, fontFace: HF, fontSize: 40, bold: true, color: TEAL });
  s.addText("Ra đề tốn nhiều công sức", { x: 0.9, y: 3.05, w: 5.4, h: 0.5, margin: 0, fontFace: HF, fontSize: 18, bold: true, color: NAVY });
  s.addText("Soạn ngân hàng câu hỏi đủ lớn để mỗi sinh viên một đề khác nhau mất hàng giờ thủ công.", { x: 0.9, y: 3.55, w: 5.4, h: 0.9, margin: 0, fontFace: BF, fontSize: 13.5, color: INK });
  card(s, 6.78, 2.1, 5.95, 2.5, BGSOFT);
  s.addText("02", { x: 7.08, y: 2.3, w: 1.5, h: 0.8, margin: 0, fontFace: HF, fontSize: 40, bold: true, color: AMBER });
  s.addText("Khó bảo đảm trung thực", { x: 7.08, y: 3.05, w: 5.4, h: 0.5, margin: 0, fontFace: HF, fontSize: 18, bold: true, color: NAVY });
  s.addText("Một giảng viên không thể quan sát hàng chục sinh viên ở nhiều địa điểm khác nhau.", { x: 7.08, y: 3.55, w: 5.4, h: 0.9, margin: 0, fontFace: BF, fontSize: 13.5, color: INK });
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: 0.6, y: 4.85, w: 12.13, h: 1.7, fill: { color: NAVY }, rectRadius: 0.08 });
  s.addText("Giải pháp", { x: 0.95, y: 5.05, w: 3, h: 0.4, margin: 0, fontFace: HF, fontSize: 15, bold: true, color: AMBER });
  s.addText("Ứng dụng AI: dùng kỹ thuật RAG sinh câu hỏi trực tiếp từ tài liệu giảng dạy, và giám sát thi bằng sự kiện trình duyệt kết hợp nhận diện khuôn mặt phía máy khách — không lưu video, tôn trọng quyền riêng tư.", { x: 0.95, y: 5.45, w: 11.4, h: 0.95, margin: 0, fontFace: BF, fontSize: 14.5, color: WHITE });
}

// ============ 4. MỤC TIÊU & PHẠM VI ============
{
  const s = content("Mục tiêu & phạm vi");
  card(s, 0.6, 1.35, 5.95, 5.0);
  s.addShape(pres.shapes.RECTANGLE, { x: 0.6, y: 1.35, w: 0.1, h: 5.0, fill: { color: TEAL } });
  s.addText("Mục tiêu", { x: 0.95, y: 1.55, w: 5.4, h: 0.5, margin: 0, fontFace: HF, fontSize: 20, bold: true, color: NAVY });
  s.addText([
    { text: "Làm chủ Spring Boot (Java 21), React + TypeScript, PostgreSQL/pgvector và tích hợp AI (Gemini) qua RAG", options: { bullet: true, breakLine: true } },
    { text: "Tự động sinh đề thi từ tài liệu giảng dạy", options: { bullet: true, breakLine: true } },
    { text: "Giám sát hành vi gian lận thời gian thực, bảo đảm quyền riêng tư", options: { bullet: true, breakLine: true } },
    { text: "Xây dựng hệ thống thi trực tuyến hoàn chỉnh ba vai trò", options: { bullet: true } },
  ], { x: 0.95, y: 2.15, w: 5.35, h: 3.9, margin: 0, fontFace: BF, fontSize: 14.5, color: INK, paraSpaceAfter: 10, valign: "top" });
  card(s, 6.78, 1.35, 5.95, 5.0);
  s.addShape(pres.shapes.RECTANGLE, { x: 6.78, y: 1.35, w: 0.1, h: 5.0, fill: { color: AMBER } });
  s.addText("Phạm vi", { x: 7.13, y: 1.55, w: 5.4, h: 0.5, margin: 0, fontFace: HF, fontSize: 20, bold: true, color: NAVY });
  s.addText([
    { text: "Ứng dụng web ba vai trò: Sinh viên, Giảng viên, Quản trị viên", options: { bullet: true, breakLine: true } },
    { text: "Xác thực & phân quyền; quản lý lớp và ngân hàng câu hỏi", options: { bullet: true, breakLine: true } },
    { text: "Sinh đề RAG; tổ chức & làm bài thi có tự động lưu, khôi phục", options: { bullet: true, breakLine: true } },
    { text: "Giám sát thi; chấm điểm tự động & báo cáo kết quả", options: { bullet: true, breakLine: true } },
    { text: "Công cụ hỗ trợ giám sát, không thay thế giám thị; không thương mại", options: { bullet: true } },
  ], { x: 7.13, y: 2.15, w: 5.35, h: 3.9, margin: 0, fontFace: BF, fontSize: 14.5, color: INK, paraSpaceAfter: 10, valign: "top" });
}

// ============ 5. KHẢO SÁT HIỆN TRẠNG ============
{
  const s = content("Khảo sát hiện trạng");
  s.addText("Phần lớn công cụ phổ biến mới dừng ở mức số hóa quy trình thi; hầu như chưa có công cụ nào sinh câu hỏi tự động từ tài liệu của giảng viên.", { x: 0.6, y: 1.3, w: 12.1, h: 0.55, margin: 0, fontFace: BF, fontSize: 14, color: INK });
  const hd = (t) => ({ text: t, options: { fill: { color: NAVY }, color: WHITE, bold: true, align: "center", valign: "middle", fontFace: BF, fontSize: 13 } });
  const rows = [
    [hd("Hệ thống"), hd("Sinh đề bằng AI"), hd("Chống gian lận"), hd("Tiếng Việt"), hd("Chi phí")],
    ["Google Forms", "Không", "Không", "Tốt", "Miễn phí"],
    ["Microsoft Forms", "Không", "Không", "Tốt", "Theo gói Office"],
    ["Moodle Quiz", "Không (random bank)", "Hạn chế (plugin)", "Tốt", "Mã nguồn mở"],
    ["Azota", "Không", "Cơ bản", "Tốt", "Có gói trả phí"],
    [{ text: "EduExam AI", options: { bold: true, color: NAVY } }, { text: "Có (RAG)", options: { bold: true, color: TEAL } }, { text: "Hai lớp", options: { bold: true, color: TEAL } }, { text: "Tốt", options: { color: INK } }, { text: "Miễn phí", options: { color: INK } }],
  ];
  s.addTable(rows, {
    x: 0.6, y: 2.0, w: 12.13, colW: [3.0, 2.6, 2.6, 1.93, 2.0], rowH: 0.62,
    fontFace: BF, fontSize: 13, color: INK, valign: "middle", align: "center",
    border: { pt: 0.5, color: "D5DEE9" }, fill: { color: WHITE },
  });
  s.addText("Bảng so sánh tập trung vào hai bài toán cốt lõi của đề tài: sinh đề tự động và giám sát chống gian lận.", { x: 0.6, y: 6.4, w: 12.1, h: 0.4, margin: 0, fontFace: BF, fontSize: 12, italic: true, color: MUTED });
}

// ============ 6. CÔNG NGHỆ ============
{
  const s = content("Công nghệ sử dụng");
  const blocks = [
    ["Backend", TEAL, ["Java 21 (LTS)", "Spring Boot 3.3", "Spring Security + JPA", "LangChain4j, Apache Tika", "JJWT, Flyway, MapStruct"]],
    ["Frontend", NAVY2, ["React 18 + Vite 5", "TypeScript (strict)", "Tailwind + shadcn/ui", "TanStack Query, Zustand", "MediaPipe (WASM)"]],
    ["AI / LLM", AMBER, ["Google Gemini Flash", "gemini-embedding-001", "Vector 768 chiều", "Kỹ thuật RAG", "Human-in-the-loop"]],
    ["Cơ sở dữ liệu", "6D2E46", ["PostgreSQL 16", "pgvector (HNSW)", "Flyway migration", "49 bảng dữ liệu", "Docker Compose"]],
  ];
  let x = 0.6;
  for (const [t, c, items] of blocks) {
    card(s, x, 1.45, 2.93, 4.9);
    s.addShape(pres.shapes.RECTANGLE, { x, y: 1.45, w: 2.93, h: 0.7, fill: { color: c } });
    s.addText(t, { x, y: 1.45, w: 2.93, h: 0.7, margin: 0, align: "center", valign: "middle", fontFace: HF, fontSize: 16, bold: true, color: WHITE });
    s.addText(items.map((it, i) => ({ text: it, options: { bullet: true, breakLine: i < items.length - 1 } })),
      { x: x + 0.25, y: 2.35, w: 2.45, h: 3.8, margin: 0, fontFace: BF, fontSize: 13, color: INK, paraSpaceAfter: 10, valign: "top" });
    x += 3.04;
  }
}

// ============ 7. KIẾN TRÚC HỆ THỐNG ============
{
  const s = content("Kiến trúc tổng thể");
  img(s, "hinh-1.1.png", { x: 0.5, y: 1.35, maxW: 8.3, maxH: 5.5 });
  card(s, 9.05, 1.5, 3.7, 4.85, BGSOFT);
  s.addText("Đặc điểm", { x: 9.3, y: 1.7, w: 3.2, h: 0.4, margin: 0, fontFace: HF, fontSize: 16, bold: true, color: NAVY });
  s.addText([
    { text: "Monolith mô-đun hóa, phân lớp Web–Service–Domain–Persistence", options: { bullet: true, breakLine: true } },
    { text: "REST cho nghiệp vụ, WebSocket cho giám sát thời gian thực", options: { bullet: true, breakLine: true } },
    { text: "Backend không trạng thái với JWT trong header", options: { bullet: true, breakLine: true } },
    { text: "pgvector lưu & truy vấn embedding ngay trong CSDL quan hệ", options: { bullet: true } },
  ], { x: 9.3, y: 2.25, w: 3.25, h: 4.0, margin: 0, fontFace: BF, fontSize: 13, color: INK, paraSpaceAfter: 12, valign: "top" });
}

// ============ 8. SINH ĐỀ RAG ============
{
  const s = content("Nhánh AI 1 — Sinh đề tự động (RAG)");
  img(s, "hinh-1.2.png", { x: 0.5, y: 1.3, maxW: 3.3, maxH: 5.6 });
  s.addText("Truy hồi tăng cường sinh (RAG) bổ sung các đoạn tài liệu liên quan vào ngữ cảnh mô hình → câu hỏi bám sát nguồn, kèm trích dẫn để kiểm chứng.", { x: 4.2, y: 1.4, w: 8.5, h: 0.9, margin: 0, fontFace: BF, fontSize: 14.5, color: INK });
  card(s, 4.2, 2.45, 4.15, 1.85, BGSOFT);
  s.addText("Pha lập chỉ mục", { x: 4.45, y: 2.6, w: 3.7, h: 0.4, margin: 0, fontFace: HF, fontSize: 15, bold: true, color: TEAL });
  s.addText("Tika bóc tách → chia đoạn ~800 token → embedding 768 chiều → lưu document_chunks (chỉ mục HNSW)", { x: 4.45, y: 3.0, w: 3.65, h: 1.2, margin: 0, fontFace: BF, fontSize: 12.5, color: INK });
  card(s, 8.55, 2.45, 4.18, 1.85, BGSOFT);
  s.addText("Pha sinh câu hỏi", { x: 8.8, y: 2.6, w: 3.7, h: 0.4, margin: 0, fontFace: HF, fontSize: 15, bold: true, color: AMBER });
  s.addText("Truy hồi top-8 đoạn (cosine) → dựng prompt → Gemini Flash sinh câu hỏi + trích dẫn → giảng viên duyệt", { x: 8.8, y: 3.0, w: 3.7, h: 1.2, margin: 0, fontFace: BF, fontSize: 12.5, color: INK });
  s.addShape(pres.shapes.ROUNDED_RECTANGLE, { x: 4.2, y: 4.5, w: 8.53, h: 1.95, fill: { color: NAVY }, rectRadius: 0.08 });
  const stat = (x, num, lbl) => { s.addText(num, { x, y: 4.65, w: 2.0, h: 0.7, margin: 0, align: "center", fontFace: HF, fontSize: 30, bold: true, color: TEALL }); s.addText(lbl, { x: x - 0.3, y: 5.4, w: 2.6, h: 0.8, margin: 0, align: "center", fontFace: BF, fontSize: 11.5, color: "CADCFC" }); };
  stat(4.5, "768", "chiều vector"); stat(6.55, "top-8", "đoạn truy hồi"); stat(8.6, "< 60s", "sinh 10 câu"); stat(10.6, "10/giờ", "giới hạn tần suất");
}

// ============ 9. GIÁM SÁT ============
{
  const s = content("Nhánh AI 2 — Giám sát thi (Proctoring)");
  img(s, "hinh-1.3.png", { x: 0.5, y: 1.3, maxW: 7.2, maxH: 4.0 });
  card(s, 8.0, 1.4, 4.73, 5.0, BGSOFT);
  s.addText("Cơ chế", { x: 8.25, y: 1.55, w: 4.2, h: 0.4, margin: 0, fontFace: HF, fontSize: 16, bold: true, color: NAVY });
  s.addText([
    { text: "Phân tích hình ảnh hoàn toàn phía máy khách, KHÔNG lưu video", options: { bullet: true, breakLine: true } },
    { text: "Nhận diện khuôn mặt bằng MediaPipe (WebAssembly)", options: { bullet: true, breakLine: true } },
    { text: "Sự kiện trình duyệt: chuyển tab, thoát toàn màn hình, sao chép/dán, devtools", options: { bullet: true, breakLine: true } },
    { text: "Truyền thời gian thực qua WebSocket (STOMP), xác thực JWT, tự kết nối lại & gửi lại sự kiện đã đệm", options: { bullet: true, breakLine: true } },
    { text: "Mỗi sự kiện gắn mức độ; vi phạm chỉ ghi log, KHÔNG tự trừ điểm", options: { bullet: true } },
  ], { x: 8.25, y: 2.1, w: 4.25, h: 4.2, margin: 0, fontFace: BF, fontSize: 13.5, color: INK, paraSpaceAfter: 11, valign: "top" });
  s.addText("Tuân thủ Nghị định 13/2023/NĐ-CP: có thông báo và xác nhận đồng ý trước khi giám sát.", { x: 0.5, y: 5.7, w: 7.2, h: 0.7, margin: 0, fontFace: BF, fontSize: 13, italic: true, color: NAVY, align: "center" });
}

// ============ 10. PHÂN TÍCH & THIẾT KẾ ============
{
  const s = content("Phân tích & thiết kế hệ thống");
  img(s, "hinh-2.1.png", { x: 0.5, y: 1.3, maxW: 3.2, maxH: 5.6 });
  s.addText("Biểu đồ use case tổng quát — ba tác nhân", { x: 0.4, y: 6.55, w: 3.4, h: 0.35, margin: 0, fontFace: BF, fontSize: 11, italic: true, color: MUTED, align: "center" });
  const roles = [
    ["Sinh viên", TEAL, "Tham gia lớp bằng mã; làm bài thi có giám sát; luyện tập, ôn tập SM-2; xem điểm và phân tích"],
    ["Giảng viên", NAVY2, "Quản lý lớp & ngân hàng câu hỏi; sinh đề bằng AI; tạo kỳ thi; cấu hình giám sát; xem kết quả & vi phạm"],
    ["Quản trị viên", AMBER, "Quản lý tài khoản & dữ liệu nền; dashboard tổng quan; nhật ký kiểm toán chỉ-ghi-thêm"],
  ];
  let y = 1.45;
  for (const [t, c, d] of roles) {
    card(s, 4.2, y, 8.53, 1.55);
    s.addShape(pres.shapes.RECTANGLE, { x: 4.2, y, w: 0.12, h: 1.55, fill: { color: c } });
    s.addText(t, { x: 4.5, y: y + 0.15, w: 8.0, h: 0.45, margin: 0, fontFace: HF, fontSize: 17, bold: true, color: NAVY });
    s.addText(d, { x: 4.5, y: y + 0.62, w: 8.05, h: 0.8, margin: 0, fontFace: BF, fontSize: 13.5, color: INK });
    y += 1.72;
  }
}

// ============ 11. CHỨC NĂNG CHÍNH ============
{
  const s = content("Các chức năng chính");
  const cols = [
    ["Ngân hàng câu hỏi", TEAL, ["8 loại câu hỏi", "Phiên bản hóa, thẻ, thư mục", "Bộ lọc thông minh, thùng rác", "Phát hiện trùng theo ngữ nghĩa", "Trợ lý AI: gợi ý, viết lại, chấm chất lượng"]],
    ["Thi & chấm điểm", NAVY2, ["Kỳ thi cố định/ngẫu nhiên", "Đếm ngược theo giờ máy chủ", "Tự động lưu & khôi phục", "Tự nộp khi hết giờ", "Chấm tự động 8 loại, export Excel"]],
    ["Học tập & quản trị", AMBER, ["Luyện tập, ôn tập SM-2", "Trò chơi hóa, bảng xếp hạng", "Phân tích IDI / IRT", "Quản lý tài khoản & dữ liệu nền", "Nhật ký kiểm toán"]],
  ];
  let x = 0.6;
  for (const [t, c, items] of cols) {
    card(s, x, 1.45, 3.91, 4.9);
    s.addShape(pres.shapes.RECTANGLE, { x, y: 1.45, w: 3.91, h: 0.72, fill: { color: c } });
    s.addText(t, { x, y: 1.45, w: 3.91, h: 0.72, margin: 0, align: "center", valign: "middle", fontFace: HF, fontSize: 16, bold: true, color: WHITE });
    s.addText(items.map((it, i) => ({ text: it, options: { bullet: true, breakLine: i < items.length - 1 } })),
      { x: x + 0.28, y: 2.4, w: 3.4, h: 3.8, margin: 0, fontFace: BF, fontSize: 13, color: INK, paraSpaceAfter: 11, valign: "top" });
    x += 4.06;
  }
}

// ============ 12. KIỂM THỬ ============
{
  const s = content("Kết quả kiểm thử");
  const stat = (x, num, lbl, c) => {
    card(s, x, 1.5, 2.86, 2.0);
    s.addText(num, { x, y: 1.7, w: 2.86, h: 0.9, margin: 0, align: "center", fontFace: HF, fontSize: 36, bold: true, color: c });
    s.addText(lbl, { x: x + 0.1, y: 2.65, w: 2.66, h: 0.7, margin: 0, align: "center", fontFace: BF, fontSize: 12.5, color: MUTED });
  };
  stat(0.6, "5.106", "ca kiểm thử tự động", TEAL);
  stat(3.66, "440", "tệp test (BE + FE)", NAVY2);
  stat(6.72, "0", "ca thất bại (all-pass)", TEAL);
  stat(9.78, "~50", "lớp test tích hợp", AMBER);
  card(s, 0.6, 3.85, 6.0, 2.65);
  s.addText("Độ phủ mã (coverage)", { x: 0.9, y: 4.0, w: 5.4, h: 0.45, margin: 0, fontFace: HF, fontSize: 16, bold: true, color: NAVY });
  s.addChart(pres.charts.BAR, [{ name: "Coverage", labels: ["Backend", "Frontend"], values: [91.3, 84.4] }], {
    x: 0.8, y: 4.5, w: 5.6, h: 1.9, barDir: "col", chartColors: [TEAL, NAVY2],
    showValue: true, dataLabelPosition: "outEnd", dataLabelColor: INK, dataLabelFontSize: 12,
    valAxisHidden: true, catAxisLabelColor: MUTED, catAxisLabelFontSize: 12, showLegend: false,
    valAxisMaxVal: 100, valAxisMinVal: 0, chartArea: { fill: { color: "FFFFFF" } },
  });
  card(s, 6.78, 3.85, 5.95, 2.65, BGSOFT);
  s.addText("Chiến lược kiểm thử", { x: 7.05, y: 4.0, w: 5.4, h: 0.45, margin: 0, fontFace: HF, fontSize: 16, bold: true, color: NAVY });
  s.addText([
    { text: "BE: JUnit 5 + Mockito + AssertJ (đơn vị); Testcontainers + REST Assured (tích hợp)", options: { bullet: true, breakLine: true } },
    { text: "FE: Vitest + React Testing Library + MSW", options: { bullet: true, breakLine: true } },
    { text: "Ưu tiên độ phủ cao cho các bộ chấm điểm", options: { bullet: true } },
  ], { x: 7.05, y: 4.5, w: 5.45, h: 1.9, margin: 0, fontFace: BF, fontSize: 13, color: INK, paraSpaceAfter: 10, valign: "top" });
}

// ============ 13. ĐÁNH GIÁ AI ============
{
  const s = content("Đánh giá các thành phần AI");
  s.addText("Hai thành phần AI được đánh giá riêng bằng thực nghiệm (kết quả mang tính xác suất). Các mục tiêu đề ra:", { x: 0.6, y: 1.3, w: 12.1, h: 0.55, margin: 0, fontFace: BF, fontSize: 15, color: INK });
  card(s, 0.6, 2.05, 5.95, 4.4);
  s.addShape(pres.shapes.RECTANGLE, { x: 0.6, y: 2.05, w: 5.95, h: 0.72, fill: { color: TEAL } });
  s.addText("Sinh đề (RAG)", { x: 0.6, y: 2.05, w: 5.95, h: 0.72, margin: 0, align: "center", valign: "middle", fontFace: HF, fontSize: 17, bold: true, color: WHITE });
  s.addText([
    { text: "Sinh 10 câu trong dưới 60 giây", options: { bullet: true, breakLine: true } },
    { text: "Trên 70% câu được chấp nhận không cần sửa", options: { bullet: true, breakLine: true } },
    { text: "Trích dẫn đúng và bám sát tài liệu nguồn", options: { bullet: true } },
  ], { x: 0.95, y: 3.0, w: 5.3, h: 3.3, margin: 0, fontFace: BF, fontSize: 15, color: INK, paraSpaceAfter: 14, valign: "top" });
  card(s, 6.78, 2.05, 5.95, 4.4);
  s.addShape(pres.shapes.RECTANGLE, { x: 6.78, y: 2.05, w: 5.95, h: 0.72, fill: { color: NAVY2 } });
  s.addText("Giám sát", { x: 6.78, y: 2.05, w: 5.95, h: 0.72, margin: 0, align: "center", valign: "middle", fontFace: HF, fontSize: 17, bold: true, color: WHITE });
  s.addText([
    { text: "Phát hiện chuyển tab ≥ 90%", options: { bullet: true, breakLine: true } },
    { text: "Nhận diện khuôn mặt ≥ 80%", options: { bullet: true, breakLine: true } },
    { text: "Tỷ lệ báo nhầm dưới 15%", options: { bullet: true } },
  ], { x: 7.13, y: 3.0, w: 5.3, h: 3.3, margin: 0, fontFace: BF, fontSize: 15, color: INK, paraSpaceAfter: 14, valign: "top" });
}

// ============ 14. KẾT QUẢ ĐẠT ĐƯỢC ============
{
  const s = content("Kết luận — Kết quả đạt được");
  const items = [
    ["Sản phẩm", "Ứng dụng web hoàn chỉnh ba vai trò: xác thực & phân quyền, quản lý lớp & ngân hàng câu hỏi đa dạng, tổ chức & làm bài thi có tự động lưu/khôi phục, chấm điểm tự động và quản trị hệ thống."],
    ["Nhánh AI 1", "Pipeline RAG hoàn chỉnh: upload tài liệu → embedding trên pgvector → truy hồi ngữ nghĩa → sinh câu hỏi kèm trích dẫn, có cơ chế review của giảng viên."],
    ["Nhánh AI 2", "Giám sát thời gian thực kết hợp sự kiện trình duyệt và nhận diện khuôn mặt phía máy khách, truyền qua WebSocket, tôn trọng quyền riêng tư."],
    ["Kỹ thuật", "Kiến trúc monolith mô-đun phân lớp, REST + WebSocket, bảo mật JWT + RBAC, bộ kiểm thử tự động quy mô lớn (5.106 ca, độ phủ 84–91%)."],
  ];
  let y = 1.45;
  for (const [t, d] of items) {
    s.addShape(pres.shapes.OVAL, { x: 0.65, y: y + 0.05, w: 0.28, h: 0.28, fill: { color: TEAL } });
    s.addText(t, { x: 1.1, y: y - 0.05, w: 3.0, h: 0.45, margin: 0, fontFace: HF, fontSize: 17, bold: true, color: NAVY });
    s.addText(d, { x: 4.0, y: y - 0.05, w: 8.7, h: 1.15, margin: 0, fontFace: BF, fontSize: 13.5, color: INK });
    y += 1.28;
  }
}

// ============ 15. HẠN CHẾ & HƯỚNG PHÁT TRIỂN ============
{
  const s = content("Hạn chế & hướng phát triển");
  card(s, 0.6, 1.4, 5.95, 5.0, BGSOFT);
  s.addShape(pres.shapes.RECTANGLE, { x: 0.6, y: 1.4, w: 0.1, h: 5.0, fill: { color: AMBER } });
  s.addText("Hạn chế", { x: 0.95, y: 1.6, w: 5.4, h: 0.5, margin: 0, fontFace: HF, fontSize: 19, bold: true, color: NAVY });
  s.addText([
    { text: "Chất lượng câu hỏi phụ thuộc mô hình và tài liệu; vẫn cần giảng viên review", options: { bullet: true, breakLine: true } },
    { text: "Giám sát mới ở mức hỗ trợ; chưa phát hiện gian lận tinh vi, có thể báo nhầm", options: { bullet: true, breakLine: true } },
    { text: "Chạy trên một máy chủ đơn, chưa tối ưu mở rộng theo chiều ngang", options: { bullet: true, breakLine: true } },
    { text: "Gói Gemini miễn phí có giới hạn tần suất", options: { bullet: true } },
  ], { x: 0.95, y: 2.2, w: 5.35, h: 4.0, margin: 0, fontFace: BF, fontSize: 14, color: INK, paraSpaceAfter: 12, valign: "top" });
  card(s, 6.78, 1.4, 5.95, 5.0, BGSOFT);
  s.addShape(pres.shapes.RECTANGLE, { x: 6.78, y: 1.4, w: 0.1, h: 5.0, fill: { color: TEAL } });
  s.addText("Hướng phát triển", { x: 7.13, y: 1.6, w: 5.4, h: 0.5, margin: 0, fontFace: HF, fontSize: 19, bold: true, color: NAVY });
  s.addText([
    { text: "Chấm tự luận bằng LLM; đa nhà cung cấp mô hình (Gemini, OpenAI...)", options: { bullet: true, breakLine: true } },
    { text: "Phát hiện vật thể, theo dõi hướng nhìn, âm thanh đáng ngờ", options: { bullet: true, breakLine: true } },
    { text: "Ứng dụng di động; tích hợp LMS; SSO/OAuth", options: { bullet: true, breakLine: true } },
    { text: "Tách dịch vụ, đa trường (multi-tenant), triển khai cloud", options: { bullet: true, breakLine: true } },
    { text: "Cá nhân hóa lộ trình ôn tập bằng học máy", options: { bullet: true } },
  ], { x: 7.13, y: 2.2, w: 5.35, h: 4.0, margin: 0, fontFace: BF, fontSize: 14, color: INK, paraSpaceAfter: 11, valign: "top" });
}

// ============ 16. CẢM ƠN ============
{
  const s = pres.addSlide();
  s.background = { color: NAVY };
  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 0, w: W, h: 0.18, fill: { color: TEAL } });
  s.addShape(pres.shapes.RECTANGLE, { x: 0, y: 7.32, w: W, h: 0.18, fill: { color: AMBER } });
  s.addText("Trân trọng cảm ơn", { x: 0.8, y: 2.5, w: 11.7, h: 1.0, margin: 0, fontFace: HF, fontSize: 44, bold: true, color: WHITE, align: "center" });
  s.addText("Kính mong nhận được ý kiến đóng góp của quý thầy cô", { x: 0.8, y: 3.7, w: 11.7, h: 0.6, margin: 0, fontFace: HF, fontSize: 19, italic: true, color: "CADCFC", align: "center" });
  s.addShape(pres.shapes.RECTANGLE, { x: 5.67, y: 4.55, w: 2.0, h: 0.04, fill: { color: TEAL } });
  s.addText("Sinh viên: Thừa Văn An  •  GVHD: Phạm Văn Hà", { x: 0.8, y: 4.8, w: 11.7, h: 0.4, margin: 0, fontFace: BF, fontSize: 14, color: "9FB6D4", align: "center" });
}

pres.writeFile({ fileName: OUT }).then(() => console.log("OK ->", OUT)).catch((e) => { console.error(e); process.exit(1); });
