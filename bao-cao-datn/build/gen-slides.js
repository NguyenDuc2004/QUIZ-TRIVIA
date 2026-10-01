/* Sinh slide bảo vệ ĐATN Quiz/Trivia tích hợp AI (.pptx) bằng pptxgenjs.
 *
 * Chạy:  cd bao-cao-datn/build && node gen-slides.js  -> ../Slide-BaoVe-QuizAI.pptx
 *
 * ## Form
 * Theo mẫu slide bảo vệ của khoa: bốn phần đánh số La Mã, mỗi phần mở đầu bằng một SLIDE NGĂN chỉ có
 * huy hiệu số và tên phần. Slide nội dung đặt tên phần ở giữa trên cùng, mục con đánh số Ả Rập căn
 * trái bên dưới. Chữ serif, nền xanh nhạt, trang trí hình học chéo ở góc.
 *
 * ## Nguyên tắc nội dung
 * Mọi con số trên slide phải là số ĐÃ ĐO, truy được về mục tương ứng của báo cáo. Slide bảo vệ là nơi
 * người ta hỏi lại từng con số, nên một số ước lượng lọt vào đây nguy hiểm hơn nằm trong báo cáo.
 */
const path = require("path");
const fs = require("fs");
const pptxgen = require("pptxgenjs");

const ASSETS = path.join(__dirname, "..", "assets");
const LOGO = path.join(ASSETS, "logo-haui.png");
if (!fs.existsSync(LOGO)) {
  console.error("Thiếu assets/logo-haui.png — không có logo trường để chèn.");
  process.exit(1);
}
/* Logo gốc 436x79 px, tỉ lệ 5.52. Mọi chỗ đặt logo đều tính bề ngang từ chiều cao theo tỉ lệ này
 * để không bóp méo chữ trong logo. */
const TY_LE_LOGO = 436 / 79;
const datLogo = (s, x, y, cao) => s.addImage({ path: LOGO, x, y, w: cao * TY_LE_LOGO, h: cao });
const OUT = path.join(__dirname, "..", "Slide-BaoVe-QuizAI.pptx");

const FONT = "Times New Roman";
const XANH_DAM = "1F4E9C";
const XANH = "2E75B6";
const XANH_NHAT = "BDD7EE";
const NEN = "EAF2FB";
const DAM = "1F3864";
const MUC = "44546A";
const TRANG = "FFFFFF";

/* Chú thích hình đọc thẳng từ Markdown báo cáo, để slide không tự đi một đường.
 *
 * Chương 3 từng bị đánh số lại và slide vẫn giữ số cũ: NĂM slide chiếu nhầm màn, trong đó slide
 * "sinh đề" chiếu màn Trợ lý — trụ cột biến mất khỏi bài bảo vệ mà không một dòng cảnh báo, vì tệp
 * vẫn tồn tại và ảnh vẫn hiện ra. Nên mỗi lần gọi ảnh phải khai kèm từ khoá, và chú thích hình mang
 * số đó trong báo cáo phải chứa từ khoá ấy. Lệch là dừng, không xuất tệp. */
const CHU_THICH = (() => {
  const m = new Map();
  for (const f of ["01-chuong-1.md", "02-chuong-2.md", "03-chuong-3.md"]) {
    const t = fs.readFileSync(path.join(__dirname, "..", f), "utf8");
    for (const k of t.matchAll(/\[HÌNH\s+([\d.]+)\s*:\s*([^\]]+?)\s*—\s*cần chèn\]/g)) m.set(k[1], k[2]);
  }
  return m;
})();

const thieu = [];
const lech = [];
const anh = (ten, tuKhoa) => {
  const mo = CHU_THICH.get(ten);
  if (!mo) lech.push(`hinh-${ten}: báo cáo không còn hình nào mang số này`);
  else if (tuKhoa && !mo.toLowerCase().includes(tuKhoa.toLowerCase()))
    lech.push(`hinh-${ten}: slide dùng cho "${tuKhoa}", báo cáo ghi "${mo}"`);
  const p = path.join(ASSETS, `hinh-${ten}.png`);
  if (!fs.existsSync(p)) {
    thieu.push(`hinh-${ten}.png`);
    return null;
  }
  return p;
};

/** Kích thước thật của PNG, để đặt ảnh vừa khung mà không méo tỉ lệ. */
function coAnh(p) {
  const b = fs.readFileSync(p);
  return { w: b.readUInt32BE(16), h: b.readUInt32BE(20) };
}

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9"; // 10 x 5.625 inch
const W = 10;
const H = 5.625;

/** Trang trí hình học chéo ở góc — bắt chước mẫu, không để slide trơ trọi. */
function trangTri(s, dam = false, gocDuoi = true) {
  const m1 = dam ? XANH_DAM : XANH;
  const m2 = dam ? XANH : XANH_NHAT;
  s.addShape(pres.ShapeType.rect, { x: W - 1.55, y: -0.5, w: 0.42, h: 2.3, fill: { color: m1 }, rotate: 32 });
  s.addShape(pres.ShapeType.rect, { x: W - 1.05, y: -0.7, w: 0.3, h: 2.1, fill: { color: m2 }, rotate: 32 });
  s.addShape(pres.ShapeType.rect, { x: W - 0.42, y: -0.55, w: 0.5, h: 1.9, fill: { color: m1 }, rotate: 32 });
  // Góc dưới trái chỉ vẽ ở bìa và slide ngăn. Ở slide nội dung, hình chéo này đè lên cột nhãn bên
  // trái và cắt ngang chữ — đã thấy nó xén mất chữ "Người duyệt cuối" ở bản trước.
  if (!gocDuoi) return;
  s.addShape(pres.ShapeType.rect, { x: 0.05, y: H - 1.75, w: 0.42, h: 2.3, fill: { color: m1 }, rotate: 32 });
  s.addShape(pres.ShapeType.rect, { x: 0.62, y: H - 1.4, w: 0.28, h: 1.8, fill: { color: m2 }, rotate: 32 });
}

/** Dấu hiệu trường ở góc trên trái của slide nội dung. */
function dauHieu(s) {
  datLogo(s, 0.26, 0.2, 0.3);
}

let so = 0;

/** Slide nội dung: tên phần ở giữa trên cùng, mục con căn trái bên dưới. */
function trang(tenPhan, mucCon) {
  so++;
  const s = pres.addSlide();
  s.background = { color: NEN };
  trangTri(s, false, false);
  dauHieu(s);
  s.addText(tenPhan, { x: 1.6, y: 0.18, w: W - 3.2, h: 0.5, fontSize: 23, bold: true, color: XANH_DAM, fontFace: FONT, align: "center", valign: "middle" });
  s.addShape(pres.ShapeType.line, { x: W / 2 - 0.75, y: 0.74, w: 1.5, h: 0, line: { color: XANH, width: 1.5 } });
  if (mucCon) s.addText(mucCon, { x: 0.55, y: 0.88, w: W - 1.1, h: 0.36, fontSize: 16, bold: true, color: DAM, fontFace: FONT, valign: "middle" });
  s.addText(`${so}`, { x: W - 0.72, y: H - 0.36, w: 0.45, h: 0.28, fontSize: 11, color: MUC, fontFace: FONT, align: "right" });
  return s;
}

/** Slide ngăn giữa các phần: huy hiệu số La Mã + tên phần trong khung trắng bo tròn. */
function slideNgan(laMa, ten) {
  const s = pres.addSlide();
  s.background = { color: NEN };
  trangTri(s, true);
  const yy = 2.05;
  s.addShape(pres.ShapeType.roundRect, { x: 1.5, y: yy - 0.18, w: W - 3.0, h: 1.55, fill: { color: TRANG }, line: { color: XANH_NHAT, width: 1 }, rectRadius: 0.16 });
  s.addShape(pres.ShapeType.homePlate, { x: 1.18, y: yy - 0.04, w: 1.5, h: 1.28, fill: { color: XANH_DAM } });
  s.addText(laMa, { x: 1.18, y: yy - 0.04, w: 1.2, h: 1.28, fontSize: 46, bold: true, color: TRANG, fontFace: FONT, align: "center", valign: "middle" });
  s.addText(ten, { x: 2.95, y: yy - 0.04, w: W - 4.35, h: 1.28, fontSize: 28, bold: true, color: XANH_DAM, fontFace: FONT, align: "center", valign: "middle" });
  return s;
}

/** Danh sách gạch đầu dòng. */
function y(s, muc, o = {}) {
  s.addText(
    muc.map((t) => (typeof t === "string" ? { text: t, options: { bullet: { code: "2022" }, breakLine: true } } : t)),
    { x: o.x ?? 0.62, y: o.y ?? 1.38, w: o.w ?? W - 1.24, h: o.h ?? 3.5, fontSize: o.co ?? 13.5, color: DAM, fontFace: FONT, lineSpacingMultiple: 1.4, valign: "top" },
  );
}

/** Thẻ số liệu cho các slide kết quả đo. */
function the(s, x, yy, w, h, soLieu, nhan, phu) {
  s.addShape(pres.ShapeType.roundRect, { x, y: yy, w, h, fill: { color: TRANG }, line: { color: XANH_NHAT, width: 1.25 }, rectRadius: 0.08 });
  s.addText(soLieu, { x, y: yy + 0.1, w, h: h * 0.45, fontSize: 24, bold: true, color: XANH_DAM, fontFace: FONT, align: "center", valign: "middle" });
  s.addText(nhan, { x, y: yy + h * 0.5, w, h: h * 0.26, fontSize: 11.5, color: DAM, fontFace: FONT, align: "center", valign: "middle" });
  if (phu) s.addText(phu, { x, y: yy + h * 0.74, w, h: h * 0.24, fontSize: 9.5, color: MUC, fontFace: FONT, align: "center", valign: "middle" });
}

/** Ảnh ngang căn giữa khung, ảnh dọc dựa trái và chú thích thành cột phải. */
function anhVaChu(s, p, chu) {
  const doan = Array.isArray(chu) ? chu : [chu];
  const duoi = { x: 0.6, y: 4.84, w: W - 1.2, h: 0.4, fontSize: 11, color: MUC, fontFace: FONT, align: "center", valign: "middle" };
  if (!p) {
    s.addText(doan.join(" "), duoi);
    return;
  }
  const { w, h } = coAnh(p);
  if (w / h >= 1.15) {
    const k = { x: 0.55, y: 1.34, w: W - 1.1, h: 3.4 };
    const ty = Math.min(k.w / w, k.h / h);
    s.addImage({ path: p, x: k.x + (k.w - w * ty) / 2, y: k.y + (k.h - h * ty) / 2, w: w * ty, h: h * ty });
    s.addText(doan.join(" "), duoi);
    return;
  }
  const Hc = 3.8;
  const Wa = (w / h) * Hc;
  s.addImage({ path: p, x: 0.55, y: 1.3, w: Wa, h: Hc });
  const x = 0.55 + Wa + 0.4;
  s.addText(doan.map((t) => ({ text: t, options: { breakLine: true, paraSpaceAfter: 10 } })),
    { x, y: 1.6, w: W - 0.55 - x, h: 3.1, fontSize: 12, color: DAM, fontFace: FONT, valign: "top", lineSpacingMultiple: 1.25 });
}

/** Một slide chỉ gồm ảnh và chú thích. */
function slideAnh(tenPhan, mucCon, hinh, tuKhoa, chu) {
  anhVaChu(trang(tenPhan, mucCon), anh(hinh, tuKhoa), chu);
}

/** Khối "nhãn | mô tả" dùng cho các slide mô tả cách làm. */
function khoiCachLam(s, muc, batDau = 1.36) {
  muc.forEach(([t, m], i) => {
    const yy = batDau + i * 0.86;
    s.addShape(pres.ShapeType.rect, { x: 0.62, y: yy, w: 0.07, h: 0.72, fill: { color: XANH_DAM } });
    s.addText(t, { x: 0.82, y: yy, w: 2.2, h: 0.72, fontSize: 12.5, bold: true, color: XANH_DAM, fontFace: FONT, valign: "middle" });
    s.addText(m, { x: 3.0, y: yy, w: W - 3.6, h: 0.72, fontSize: 11, color: DAM, fontFace: FONT, valign: "middle" });
  });
}

/* ═══════════════════ BÌA ═══════════════════ */
{
  const s = pres.addSlide();
  s.background = { color: TRANG };
  trangTri(s, true);
  // Logo đã có sẵn dòng "SCHOOL OF INFORMATION & COMMUNICATIONS TECHNOLOGY" nên bỏ dòng chữ rời,
  // tránh in hai lần cùng một nội dung.
  datLogo(s, (W - 0.52 * TY_LE_LOGO) / 2, 0.3, 0.52);

  s.addText("ĐỒ ÁN TỐT NGHIỆP", { x: 0.6, y: 1.06, w: W - 1.2, h: 0.68, fontSize: 38, bold: true, color: XANH_DAM, fontFace: FONT, align: "center", valign: "middle" });
  s.addShape(pres.ShapeType.rect, { x: 3.1, y: 1.8, w: 3.8, h: 0.42, fill: { color: XANH_DAM } });
  s.addText("Ngành Kỹ thuật phần mềm", { x: 3.1, y: 1.8, w: 3.8, h: 0.42, fontSize: 15, bold: true, color: TRANG, fontFace: FONT, align: "center", valign: "middle" });

  s.addText("ĐỀ TÀI:", { x: 0.95, y: 2.42, w: 2, h: 0.3, fontSize: 13.5, bold: true, color: DAM, fontFace: FONT, underline: true });
  s.addText("XÂY DỰNG ỨNG DỤNG QUIZ/TRIVIA\nTÍCH HỢP TRÍ TUỆ NHÂN TẠO", {
    x: 0.8, y: 2.74, w: W - 1.6, h: 0.86, fontSize: 20, bold: true, color: XANH_DAM, fontFace: FONT, align: "center", valign: "middle", lineSpacingMultiple: 1.2,
  });

  s.addShape(pres.ShapeType.rect, { x: 3.0, y: 3.74, w: 4.0, h: 1.0, fill: { color: TRANG }, line: { color: XANH_DAM, width: 1 } });
  s.addText(
    [
      { text: "GVHD", options: { bold: true } }, { text: "\t: ThS. Nguyễn Đức Lưu", options: { breakLine: true } },
      { text: "SVTH", options: { bold: true } }, { text: "\t: Nguyễn Khắc Minh Đức", options: { breakLine: true } },
      { text: "MSV", options: { bold: true } }, { text: "\t: 2022601585", options: {} },
    ],
    { x: 3.22, y: 3.8, w: 3.7, h: 0.88, fontSize: 12, color: DAM, fontFace: FONT, valign: "middle", lineSpacingMultiple: 1.25 },
  );
  s.addText("Hà Nội, tháng 10 năm 2026", { x: 0.6, y: 4.88, w: W - 1.2, h: 0.3, fontSize: 11, italic: true, color: MUC, fontFace: FONT, align: "center" });
}

/* ═══════════════════ NỘI DUNG THUYẾT TRÌNH ═══════════════════ */
const PHAN = [
  ["I", "LÝ DO CHỌN ĐỀ TÀI"],
  ["II", "CƠ SỞ LÝ THUYẾT"],
  ["III", "THỰC NGHIỆM"],
  ["IV", "KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN"],
];
{
  const s = pres.addSlide();
  s.background = { color: NEN };
  trangTri(s);
  dauHieu(s);
  s.addText("NỘI DUNG\nTHUYẾT TRÌNH", { x: 0.32, y: 1.9, w: 3.0, h: 1.3, fontSize: 24, bold: true, color: XANH_DAM, fontFace: FONT, align: "center", valign: "middle", lineSpacingMultiple: 1.2 });
  s.addShape(pres.ShapeType.line, { x: 1.02, y: 3.34, w: 1.6, h: 0, line: { color: XANH, width: 1.5 } });
  PHAN.forEach(([la, ten], i) => {
    const yy = 0.95 + i * 1.02;
    s.addShape(pres.ShapeType.roundRect, { x: 4.1, y: yy, w: 5.3, h: 0.78, fill: { color: TRANG }, line: { color: XANH_NHAT, width: 1 }, rectRadius: 0.1 });
    s.addShape(pres.ShapeType.homePlate, { x: 3.35, y: yy + 0.04, w: 1.0, h: 0.7, fill: { color: i % 2 ? XANH : XANH_DAM } });
    s.addText(la, { x: 3.35, y: yy + 0.04, w: 0.8, h: 0.7, fontSize: 21, bold: true, color: TRANG, fontFace: FONT, align: "center", valign: "middle" });
    s.addText(ten, { x: 4.5, y: yy, w: 4.7, h: 0.78, fontSize: 14.5, bold: true, color: DAM, fontFace: FONT, valign: "middle" });
  });
}

/* ═══════════════════ PHẦN I ═══════════════════ */
slideNgan("I", "LÝ DO CHỌN ĐỀ TÀI");
const P1 = "I. LÝ DO CHỌN ĐỀ TÀI";
{
  const s = trang(P1, "1. Ba khoảng trống của các nền tảng hiện có");
  const kt = [
    ["Soạn đề thủ công", "Giáo viên phải tự gõ từng câu, dù học liệu của môn đã có sẵn dưới dạng tài liệu"],
    ["Không chấm được câu tự luận", "Chỉ chấm được câu có đáp án xác định; câu trả lời ngắn vẫn phải chấm tay"],
    ["Gợi ý theo lượt xem, không theo năng lực", "Người học không biết mình yếu chủ đề nào và nên ôn gì tiếp theo"],
  ];
  kt.forEach(([t, m], i) => {
    const yy = 1.42 + i * 1.12;
    s.addShape(pres.ShapeType.roundRect, { x: 0.62, y: yy, w: W - 1.24, h: 0.98, fill: { color: TRANG }, line: { color: XANH_NHAT, width: 1 }, rectRadius: 0.08 });
    s.addText(t, { x: 0.85, y: yy + 0.08, w: W - 1.7, h: 0.34, fontSize: 14, bold: true, color: XANH_DAM, fontFace: FONT, valign: "middle" });
    s.addText(m, { x: 0.85, y: yy + 0.44, w: W - 1.7, h: 0.44, fontSize: 11.5, color: MUC, fontFace: FONT, valign: "top" });
  });
}
{
  const s = trang(P1, "2. Mục tiêu — bốn trọng tâm theo phiếu giao đề tài");
  const tru = [
    ["Phòng đấu thời gian thực", "Nhiều người chơi cùng lúc, độ trễ thấp, tính điểm theo tốc độ trả lời"],
    ["Sinh đề và trợ lý bằng RAG", "Sinh câu hỏi từ chính học liệu; trợ lý trả lời kèm trích dẫn nguồn"],
    ["Gợi ý cá nhân hoá bằng Neo4j", "Phân tích hành vi làm bài trên đồ thị để gợi ý quiz và lộ trình ôn"],
    ["Đo hiệu năng và độ chính xác AI", "Không chỉ làm chạy được mà phải đo và báo cáo bằng số liệu thật"],
  ];
  tru.forEach(([t, m], i) => {
    const c = i % 2;
    const r = Math.floor(i / 2);
    const x = 0.62 + c * 4.45;
    const yy = 1.42 + r * 1.72;
    s.addShape(pres.ShapeType.roundRect, { x, y: yy, w: 4.3, h: 1.56, fill: { color: TRANG }, line: { color: XANH_DAM, width: 1.25 }, rectRadius: 0.09 });
    s.addShape(pres.ShapeType.ellipse, { x: x + 0.2, y: yy + 0.18, w: 0.36, h: 0.36, fill: { color: XANH_DAM } });
    s.addText(String(i + 1), { x: x + 0.2, y: yy + 0.18, w: 0.36, h: 0.36, fontSize: 13, bold: true, color: TRANG, fontFace: FONT, align: "center", valign: "middle" });
    s.addText(t, { x: x + 0.68, y: yy + 0.16, w: 3.4, h: 0.4, fontSize: 13.5, bold: true, color: XANH_DAM, fontFace: FONT, valign: "middle" });
    s.addText(m, { x: x + 0.24, y: yy + 0.64, w: 3.82, h: 0.8, fontSize: 11, color: MUC, fontFace: FONT, valign: "top" });
  });
}

/* ═══════════════════ PHẦN II ═══════════════════ */
slideNgan("II", "CƠ SỞ LÝ THUYẾT");
const P2 = "II. CƠ SỞ LÝ THUYẾT";
slideAnh(P2, "1. Kiến trúc tổng quan hệ thống", "1.1", "kiến trúc tổng thể",
  "Khối đơn phân lớp · ba kênh giao tiếp: REST cho nghiệp vụ, WebSocket cho phòng đấu, SSE cho luồng trả lời của trợ lý.");
slideAnh(P2, "2. Pipeline RAG — nạp học liệu và truy hồi", "1.2", "pipeline rag", [
  "Một đường ống phục vụ cả hai chức năng AI: sinh đề và trợ lý.",
  "Pha lập chỉ mục chạy nền: Tika bóc tách, chia đoạn có chồng lấp, mỗi đoạn thành vector 768 chiều trong pgvector.",
  "Pha truy hồi lọc quyền đọc TRƯỚC khi xếp hạng theo khoảng cách cosine, và không dùng chỉ mục xấp xỉ.",
]);
{
  const s = trang(P2, "3. Công nghệ sử dụng");
  const nhom = [
    ["Máy chủ", "Java 21 · Spring Boot 3.5\nSpring Security · Data JPA\nWebSocket (STOMP) · Flyway"],
    ["Giao diện", "React 19 · TypeScript\nVite 8 · Ant Design v6\nTailwind CSS v4"],
    ["Dữ liệu", "PostgreSQL 16 + pgvector\nNeo4j 5\nRedis 7"],
    ["Trí tuệ nhân tạo", "Google Gemini (chính)\nGroq (dự phòng)\nApache Tika · RAG tự viết"],
  ];
  nhom.forEach(([t, m], i) => {
    const x = 0.55 + i * 2.28;
    s.addShape(pres.ShapeType.roundRect, { x, y: 1.4, w: 2.1, h: 2.62, fill: { color: TRANG }, line: { color: XANH_NHAT, width: 1 }, rectRadius: 0.09 });
    s.addShape(pres.ShapeType.rect, { x, y: 1.4, w: 2.1, h: 0.46, fill: { color: XANH_DAM } });
    s.addText(t, { x, y: 1.4, w: 2.1, h: 0.46, fontSize: 12.5, bold: true, color: TRANG, fontFace: FONT, align: "center", valign: "middle" });
    s.addText(m, { x: x + 0.12, y: 1.9, w: 1.86, h: 2.1, fontSize: 11, color: DAM, fontFace: FONT, align: "center", valign: "middle", lineSpacingMultiple: 1.3 });
  });
  s.addText("Không dùng Spring AI hay LangChain4j — lớp điều phối mô hình tự hiện thực để kiểm soát dự phòng, hạn mức và nhật ký.", {
    x: 0.6, y: 4.26, w: W - 1.2, h: 0.4, fontSize: 10.5, italic: true, color: MUC, fontFace: FONT, align: "center",
  });
}

/* ═══════════════════ PHẦN III ═══════════════════ */
slideNgan("III", "THỰC NGHIỆM");
const P3 = "III. THỰC NGHIỆM";
khoiCachLam(trang(P3, "1. Phòng đấu thời gian thực — cách làm"), [
  ["Vào phòng", "Mã PIN sáu số hoặc quét mã QR. Khách chưa có tài khoản vẫn chơi được khi chủ phòng cho phép, dùng khoá phiên riêng chỉ mở đúng một phòng."],
  ["Đồng bộ trạng thái", "STOMP trên WebSocket; xác thực tại khung CONNECT vì trình duyệt không cho gắn tiêu đề vào yêu cầu nâng cấp WebSocket."],
  ["Chạy nhiều tiến trình", "Sự kiện phát tán qua Redis Pub/Sub để mọi tiến trình đang giữ kết nối của phòng đều nhận được và phát tiếp cho người chơi của mình."],
  ["Tính điểm theo tốc độ", "Điểm phụ thuộc thời gian trả lời — nên độ trễ trở thành yêu cầu chức năng, không chỉ là chỉ tiêu kỹ thuật."],
]);
slideAnh(P3, "2. Phòng đấu — phòng chờ và màn chơi", "3.5", "phòng đấu", [
  "Phòng chờ hiện mã PIN sáu số và mã QR để vào phòng.",
  "Khi ván chạy, mọi người nhận câu hỏi cùng lúc; bảng xếp hạng cập nhật ngay sau mỗi câu.",
  "Khách chưa có tài khoản vẫn chơi được khi chủ phòng cho phép.",
]);
{
  const s = trang(P3, "3. Sinh đề bằng AI từ học liệu — cách làm");
  khoiCachLam(s, [
    ["Nạp học liệu", "Apache Tika bóc tách PDF, DOCX, TXT; chia đoạn có chồng lấp; mỗi đoạn thành một vector 768 chiều trong pgvector. Chạy nền."],
    ["Truy hồi có lọc quyền", "Lấy 5 đoạn gần nhất theo khoảng cách cosine, lọc phạm vi người gọi được đọc TRƯỚC khi xếp hạng, rồi bỏ đoạn vượt ngưỡng 0,75."],
    ["Sinh có cấu trúc", "Prompt buộc trả JSON theo lược đồ; máy chủ kiểm chứng lược đồ rồi loại câu trùng và câu sai định dạng."],
    ["Người duyệt cuối", "Câu sinh ra là BẢN NHÁP. Chúng chỉ vào ngân hàng câu hỏi khi người tạo nội dung tích chọn rồi bấm lưu."],
  ]);
  s.addText("Kết quả đo ở mục 3.6: 10/10 câu đúng chuẩn cấu trúc — kiểm lại độc lập ở phía kịch bản đo, không tin vào việc máy chủ đã lọc.", {
    x: 0.6, y: 4.88, w: W - 1.2, h: 0.36, fontSize: 10.5, italic: true, color: MUC, fontFace: FONT, align: "center",
  });
}
slideAnh(P3, "4. Sinh đề bằng AI — học liệu và câu hỏi nháp", "3.6", "sinh đề", [
  "Trên: học liệu đã nạp, kèm trạng thái xử lý.",
  "Dưới: kết quả sinh đề. Dòng đầu ghi nhà cung cấp đã phục vụ, thời gian chờ, và SỐ ĐOẠN HỌC LIỆU mà câu hỏi bám theo — bằng chứng câu hỏi đi ra từ tài liệu chứ không từ trí nhớ của mô hình.",
]);
slideAnh(P3, "5. Trợ lý học tập", "3.7", "trợ lý học tập",
  "Trả lời theo luồng, kèm khối trích dẫn nêu rõ tài liệu và đoạn đã dựa vào.");
slideAnh(P3, "6. Lộ trình học cá nhân hoá", "3.8", "lộ trình học",
  "Thứ tự chủ đề nên ôn, dựng từ năng lực đo được trên từng chủ đề.");
{
  const s = trang(P3, "7. Kết quả đo hiệu năng phòng đấu thời gian thực");
  const p = anh("3.14", "độ trễ");
  if (p) {
    const { w, h } = coAnh(p);
    const ty = Math.min(5.6 / w, 2.95 / h);
    s.addImage({ path: p, x: 0.6, y: 1.44, w: w * ty, h: h * ty });
  }
  the(s, 6.5, 1.46, 1.6, 1.3, "216 ms", "P95", "ở 100 người");
  the(s, 8.22, 1.46, 1.2, 1.3, "0", "sự kiện mất", "mọi mức tải");
  the(s, 6.5, 2.92, 1.6, 1.3, "200", "người/phòng", "mức đã thử");
  the(s, 8.22, 2.92, 1.2, 1.3, "2 ms", "qua Redis", "mỗi sự kiện");
  s.addText("Đo ngày 08/08/2026 trên một máy đơn, KHÔNG bao gồm độ trễ mạng thật — đây là chi phí xử lý của máy chủ và tầng phát tán.", {
    x: 0.6, y: 4.66, w: W - 1.2, h: 0.4, fontSize: 10.5, italic: true, color: MUC, fontFace: FONT, align: "center",
  });
}
{
  const s = trang(P3, "8. Kết quả đo độ chính xác các chức năng AI");
  const hang = [
    ["Chấm tự luận", "Sai lệch điểm trung bình", "0,13 / 10"],
    ["Chấm tự luận", "Bài có điểm trong khoảng chuẩn", "7 / 8"],
    ["Chống tiêm chỉ thị", "Bài tấn công bị chặn", "2 / 2"],
    ["Sinh đề", "Câu đúng chuẩn cấu trúc", "10 / 10"],
    ["Trợ lý — có học liệu", "Trả lời đúng và có trích dẫn", "3 / 3"],
    ["Trợ lý — ngoài học liệu", "Nói không biết thay vì suy đoán", "2 / 2"],
    ["Đường dự phòng", "Câu sinh được qua Groq", "9 / 9"],
  ];
  s.addTable(
    [
      ["Chức năng", "Chỉ số", "Kết quả"].map((t) => ({ text: t, options: { bold: true, color: TRANG, fill: { color: XANH_DAM }, fontSize: 12, fontFace: FONT } })),
      ...hang.map((r) => r.map((t, i) => ({ text: t, options: { fontSize: 11.5, fontFace: FONT, bold: i === 2, color: i === 2 ? XANH_DAM : DAM, align: i === 2 ? "center" : "left" } }))),
    ],
    { x: 0.62, y: 1.4, w: W - 1.24, colW: [2.6, 4.3, 1.86], border: { type: "solid", color: XANH_NHAT, pt: 1 }, rowH: 0.34, valign: "middle", margin: 0.06 },
  );
  s.addText("Đo ngày 14/08/2026. Cỡ mẫu nhỏ — đủ phát hiện lỗi hệ thống và xu hướng, chưa đủ cho kết luận thống kê.", {
    x: 0.6, y: 4.52, w: W - 1.2, h: 0.4, fontSize: 10.5, italic: true, color: MUC, fontFace: FONT, align: "center",
  });
}
{
  const s = trang(P3, "9. Kiểm thử");
  the(s, 0.62, 1.44, 2.0, 1.4, "568", "phép kiểm máy chủ", "56 lớp · 0 hỏng");
  the(s, 2.82, 1.44, 2.0, 1.4, "128", "phép kiểm giao diện", "21 tệp · 0 hỏng");
  the(s, 5.02, 1.44, 2.0, 1.4, "31", "ca kiểm thử tay", "trên trình duyệt thật");
  the(s, 7.22, 1.44, 2.16, 1.4, "38", "trang được quét", "bằng 4 vai trò");
  y(s, [
    "Kiểm thử theo tháp: nhiều phép kiểm ở tầng thấp, ít nhưng phủ đường đi thật ở tầng cao.",
    "Kiểm thử tích hợp dùng Testcontainers dựng PostgreSQL thật có pgvector cho mỗi lần chạy.",
    "Ba lỗi thật của sản phẩm lộ ra khi dùng thật chứ không qua kiểm thử — một trong số đó có hẳn một phép kiểm khẳng định đúng cái hành vi sai.",
  ], { y: 3.1, h: 1.6, co: 12 });
}

/* ═══════════════════ PHẦN IV ═══════════════════ */
slideNgan("IV", "KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN");
const P4 = "IV. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN";
{
  const s = trang(P4, "1. Kết quả đạt được và hạn chế");
  s.addText("Đã hoàn thành", { x: 0.62, y: 1.34, w: 4.2, h: 0.32, fontSize: 13.5, bold: true, color: XANH_DAM, fontFace: FONT });
  y(s, [
    "16 nhóm chức năng với 87 yêu cầu chức năng",
    "Bốn trọng tâm của phiếu giao đề tài đều có sản phẩm và số liệu đối chứng",
    "Bảy nhóm chức năng mở rộng ngoài yêu cầu bắt buộc",
  ], { x: 0.62, y: 1.7, w: 4.2, h: 2.1, co: 11.5 });
  s.addText("Hạn chế", { x: 5.2, y: 1.34, w: 4.2, h: 0.32, fontSize: 13.5, bold: true, color: "B45309", fontFace: FONT });
  y(s, [
    "Số liệu đo trên một máy đơn, không có độ trễ mạng thật",
    "Cỡ mẫu đánh giá AI nhỏ; chấm đối chiếu với đáp án theo tiêu chí, chưa phải với nhiều giáo viên",
    "Chưa quan sát được một lần chuyển nhà cung cấp mô hình do lỗi tạm thời",
  ], { x: 5.2, y: 1.7, w: 4.2, h: 2.1, co: 11.5 });
  s.addShape(pres.ShapeType.roundRect, { x: 0.62, y: 3.92, w: W - 1.24, h: 0.95, fill: { color: TRANG }, line: { color: XANH_DAM, width: 1 }, rectRadius: 0.08 });
  s.addText("Phần khó nhất của một hệ thống tích hợp mô hình ngôn ngữ không nằm ở việc gọi được mô hình, mà ở việc dựng đủ hàng rào quanh nó: giới hạn miền giá trị, kiểm chứng cấu trúc đầu ra, cách ly quyền đọc dữ liệu, và giữ quyền kết luận cuối cùng cho con người.", {
    x: 0.85, y: 3.99, w: W - 1.7, h: 0.8, fontSize: 11, italic: true, color: DAM, fontFace: FONT, valign: "middle",
  });
}
{
  const s = trang(P4, "2. Hướng phát triển");
  const gd = [
    ["Ngắn hạn", "Đo phân bố khoảng cách thực tế rồi mới chọn ngưỡng hiển thị nguồn của trợ lý · dọn tệp ảnh mồ côi"],
    ["Trung hạn", "Đo trên hạ tầng nhiều máy chủ có độ trễ mạng thật · tăng cỡ mẫu đánh giá AI, mời nhiều người chấm độc lập"],
    ["Dài hạn", "Ứng dụng di động cho phòng đấu · sinh câu hỏi theo nhiều mức nhận thức · mở rộng phân tích đồ thị sang câu hỏi có giá trị sư phạm"],
  ];
  gd.forEach(([t, m], i) => {
    const yy = 1.5 + i * 1.12;
    s.addShape(pres.ShapeType.roundRect, { x: 0.62, y: yy, w: W - 1.24, h: 0.98, fill: { color: TRANG }, line: { color: XANH_NHAT, width: 1 }, rectRadius: 0.08 });
    s.addShape(pres.ShapeType.rect, { x: 0.62, y: yy, w: 0.09, h: 0.98, fill: { color: XANH_DAM } });
    s.addText(t, { x: 0.9, y: yy + 0.06, w: 1.9, h: 0.38, fontSize: 13.5, bold: true, color: XANH_DAM, fontFace: FONT, valign: "middle" });
    s.addText(m, { x: 0.9, y: yy + 0.44, w: W - 1.9, h: 0.46, fontSize: 11, color: DAM, fontFace: FONT, valign: "top" });
  });
}

/* ═══════════════════ CẢM ƠN ═══════════════════ */
{
  const s = pres.addSlide();
  s.background = { color: NEN };
  trangTri(s, true);
  s.addText("XIN CHÂN THÀNH CẢM ƠN !", { x: 0.6, y: 2.2, w: W - 1.2, h: 0.8, fontSize: 34, bold: true, color: XANH_DAM, fontFace: FONT, align: "center", valign: "middle" });
  s.addShape(pres.ShapeType.line, { x: W / 2 - 1.1, y: 3.06, w: 2.2, h: 0, line: { color: XANH, width: 2 } });
  s.addText("Kính mong nhận được ý kiến đóng góp của các thầy cô", { x: 0.6, y: 3.24, w: W - 1.2, h: 0.4, fontSize: 13.5, color: MUC, fontFace: FONT, align: "center" });
  s.addText("Nguyễn Khắc Minh Đức · 2022601585 · GVHD: ThS. Nguyễn Đức Lưu", { x: 0.6, y: 4.28, w: W - 1.2, h: 0.35, fontSize: 11, color: MUC, fontFace: FONT, align: "center" });
}

/* Lệch số hiệu hình thì DỪNG, không ghi tệp. Một slide thiếu ảnh nhìn phát biết; một slide chiếu
 * nhầm màn trông vẫn bình thường cho tới lúc đứng trước hội đồng. */
if (lech.length) {
  console.error(`\nSỐ HIỆU HÌNH LỆCH VỚI BÁO CÁO (${lech.length}) — không xuất slide:`);
  for (const l of lech) console.error(`  ${l}`);
  process.exit(1);
}

pres.writeFile({ fileName: OUT }).then(() => {
  console.log(`OK -> ${path.basename(OUT)}  ${fs.statSync(OUT).size} bytes  (${so + 7} slide)`);
  if (thieu.length) {
    console.log(`\nTHIẾU ${thieu.length} hình, slide tương ứng đang trống:`);
    for (const t of new Set(thieu)) console.log(`  ${t}`);
  }
});
