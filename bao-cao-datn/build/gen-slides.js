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

/** Khối "nhãn | mô tả" dùng cho các slide mô tả cách làm.
 *
 * `batDau` để 1,58 chứ không phải 1,36: hình trang trí chéo ở góc trên phải của mẫu khoa buông xuống
 * tới khoảng y = 1,74in trong dải x > 7,9in, và ở mốc cũ nó đè lên dòng đầu của hàng thứ nhất — chữ
 * bị che mất một đoạn giữa câu, chỉ thấy khi render ra ảnh mà nhìn. Giãn cách hàng rút từ 0,86 xuống
 * 0,80 để bốn hàng vẫn kết thúc trước dòng chú thích cuối slide. */
function khoiCachLam(s, muc, batDau = 1.58) {
  muc.forEach(([t, m], i) => {
    const yy = batDau + i * 0.8;
    s.addShape(pres.ShapeType.rect, { x: 0.62, y: yy, w: 0.07, h: 0.72, fill: { color: XANH_DAM } });
    s.addText(t, { x: 0.82, y: yy, w: 2.2, h: 0.72, fontSize: 12.5, bold: true, color: XANH_DAM, fontFace: FONT, valign: "middle" });
    s.addText(m, { x: 3.0, y: yy, w: W - 3.6, h: 0.72, fontSize: 12.5, color: DAM, fontFace: FONT, valign: "middle" });
  });
}

/**
 * Slide "cách làm": bốn bước xếp thành cột hẹp bên trái, ảnh màn hình thật bên phải.
 *
 * Cột hẹp nên nhãn nằm TRÊN mô tả chứ không nằm cạnh — để cạnh nhau thì mỗi bên chỉ còn hơn hai inch
 * và câu nào cũng vỡ dòng.
 *
 * Ảnh dọc thì khớp theo chiều cao, ảnh ngang thì khớp theo chiều rộng, rồi căn giữa trong khung phải;
 * không ép cùng một cách cho cả hai, vì ảnh phòng đấu tỉ lệ 0,80 còn ảnh trợ lý tỉ lệ 1,60.
 */
function cachLamCoAnh(tenPhan, mucCon, hinh, tuKhoa, muc) {
  const s = trang(tenPhan, mucCon);

  muc.forEach(([t, m], i) => {
    const yy = 1.45 + i * 0.85;
    s.addShape(pres.ShapeType.rect, { x: 0.55, y: yy, w: 0.07, h: 0.78, fill: { color: XANH_DAM } });
    s.addText(t, { x: 0.76, y: yy, w: 4.1, h: 0.3, fontSize: 12, bold: true, color: XANH_DAM, fontFace: FONT, valign: "middle" });
    s.addText(m, { x: 0.76, y: yy + 0.3, w: 4.1, h: 0.46, fontSize: 10.5, color: DAM, fontFace: FONT, valign: "top", lineSpacingMultiple: 1.05 });
  });

  const p = anh(hinh, tuKhoa);
  if (!p) return s;
  const { w, h } = coAnh(p);

  /* Khung ảnh né hình trang trí chéo của mẫu khoa, vốn phủ dải x > 7,87in ở phía trên y = 1,74in.
   *
   * Ảnh DỌC cao hết khung nên mép trên chạm vùng đó — xử lý bằng cách thu bề ngang khung lại để mép
   * phải dừng trước 7,85in. Ảnh NGANG thì thấp, chỉ cần đẩy khung xuống dưới 1,8in là dùng được trọn
   * bề ngang. Ép chung một khung cho cả hai thì hoặc ảnh dọc bị đè, hoặc ảnh ngang bé đi một cách
   * vô cớ — hai ảnh ở phần này tỉ lệ 0,80 và 1,60, chênh nhau gấp đôi. */
  const K = w / h < 1
    ? { x: 5.1, y: 1.4, w: 2.74, h: 3.42 }
    : { x: 5.1, y: 1.8, w: 4.3, h: 3.0 };
  const ty = Math.min(K.w / w, K.h / h);
  s.addImage({ path: p, x: K.x + (K.w - w * ty) / 2, y: K.y + (K.h - h * ty) / 2, w: w * ty, h: h * ty });
  return s;
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
/* Kiến trúc vẽ bằng hình khối chứ KHÔNG chèn Hình 1.1 của báo cáo.
 *
 * Hình 1.1 có 15 khối và hơn 20 mũi tên. Chiếu lên màn chiếu thì chữ nhỏ tới mức không đọc được, mà
 * mỗi nhãn con trong đó — "Security Filter — JWT + RBAC", "Circuit Breaker", "Service Layer" — lại là
 * một lời mời hội đồng hỏi sang chuyện ngoài bốn trụ cột của đề tài.
 *
 * Bản slide giữ đúng ba điều cần nói: ba tầng, BA KÊNH GIAO TIẾP (chỗ đáng để bị hỏi, và người bảo vệ
 * có câu trả lời), bốn kho dữ liệu. Báo cáo vẫn giữ nguyên sơ đồ đầy đủ — ai muốn soi thì soi ở đó. */
{
  const s = trang(P2, "1. Kiến trúc tổng quan hệ thống");
  const hop = (x, yy, w, h, vien, net) =>
    s.addShape(pres.ShapeType.roundRect, {
      x, y: yy, w, h, fill: { color: TRANG }, rectRadius: 0.08,
      line: { color: vien, width: 1.25, ...(net ? { dashType: net } : {}) },
    });

  // Tầng 1 — trình duyệt.
  // Mép phải phải dừng trước 7,8in: hình trang trí chéo của mẫu khoa đổ xuống tới đó ở dải y này và
  // cắt ngang qua khung. Bản trước đã bị đúng lỗi ấy, chỉ thấy khi render ra ảnh mà nhìn.
  hop(1.5, 1.3, 6.0, 0.56, XANH_DAM);
  s.addText(
    [
      { text: "Trình duyệt", options: { bold: true, fontSize: 14.5, color: XANH_DAM } },
      { text: "     React 19 · TypeScript", options: { fontSize: 11.5, color: MUC } },
    ],
    { x: 1.5, y: 1.3, w: 6.0, h: 0.56, fontFace: FONT, align: "center", valign: "middle" },
  );

  // Ba kênh giao tiếp — mỗi dạng dữ liệu một kênh, đây là điểm đáng nói nhất của slide
  [
    ["REST", "nghiệp vụ thông thường"],
    ["WebSocket", "phòng đấu thời gian thực"],
    ["SSE", "luồng trả lời của trợ lý"],
  ].forEach(([t, m], i) => {
    const x = 1.5 + i * 2.0;
    s.addShape(pres.ShapeType.downArrow, { x: x + 0.7, y: 1.94, w: 0.6, h: 0.44, fill: { color: XANH } });
    s.addText(t, { x, y: 2.4, w: 2.0, h: 0.26, fontSize: 13, bold: true, color: XANH_DAM, fontFace: FONT, align: "center", valign: "middle" });
    s.addText(m, { x: x - 0.15, y: 2.63, w: 2.3, h: 0.24, fontSize: 10.5, color: MUC, fontFace: FONT, align: "center", valign: "middle" });
  });

  // Tầng 2 — máy chủ. Liệt kê bốn việc theo đúng bốn trụ cột của phiếu giao đề tài, không liệt kê
  // tên lớp hay tên bộ lọc: hỏi vào bốn cái này thì người bảo vệ nói được.
  hop(0.6, 2.94, 8.8, 0.68, XANH_DAM);
  s.addText(
    [
      { text: "Máy chủ ứng dụng", options: { bold: true, fontSize: 14.5, color: XANH_DAM } },
      { text: "     Spring Boot 3.5 · Java 21", options: { fontSize: 11.5, color: MUC } },
      { text: "\nXác thực và phân quyền  ·  Quiz và bài làm  ·  Phòng đấu  ·  Sinh đề và trợ lý (RAG)", options: { fontSize: 10.5, color: DAM } },
    ],
    { x: 0.6, y: 2.94, w: 8.8, h: 0.68, fontFace: FONT, align: "center", valign: "middle", lineSpacingMultiple: 1.15 },
  );

  s.addShape(pres.ShapeType.downArrow, { x: 4.87, y: 3.68, w: 0.26, h: 0.2, fill: { color: XANH } });

  // Tầng 3 — ba kho dữ liệu trong Docker + một dịch vụ ngoài. Khung nét đứt để phân biệt trong/ngoài
  // mà không phải vẽ thêm khung nhóm.
  [
    ["PostgreSQL 16", "dữ liệu nghiệp vụ\nvà kho vector", false],
    ["Neo4j 5", "đồ thị hành vi\nđể gợi ý", false],
    ["Redis 7", "phiên và\ntrạng thái phòng", false],
    ["Gemini → Groq", "mô hình ngôn ngữ\n(dịch vụ ngoài)", true],
  ].forEach(([t, m, ngoai], i) => {
    const x = 0.6 + i * 2.25;
    hop(x, 3.94, 2.05, 0.82, ngoai ? XANH : XANH_NHAT, ngoai ? "dash" : null);
    s.addText(t, { x, y: 3.99, w: 2.05, h: 0.26, fontSize: 12.5, bold: true, color: XANH_DAM, fontFace: FONT, align: "center", valign: "middle" });
    s.addText(m, { x, y: 4.24, w: 2.05, h: 0.46, fontSize: 10, color: MUC, fontFace: FONT, align: "center", valign: "middle", lineSpacingMultiple: 1.1 });
  });

  // Dòng này gánh luôn phần của slide "Công nghệ sử dụng" đã bỏ: nó nêu đúng những thành phần mà
  // sơ đồ trên không hiện tên.
  s.addText("Khối đơn phân lớp; ba kho dữ liệu chạy trong Docker, mô hình ngôn ngữ là dịch vụ ngoài có dự phòng tự chuyển.\nNgoài ra: Spring Security · Flyway · Apache Tika · Ant Design v6 · Tailwind CSS v4 — lớp điều phối mô hình tự hiện thực, không dùng Spring AI hay LangChain4j.",
    { x: 0.6, y: 4.78, w: W - 1.2, h: 0.46, fontSize: 10, italic: true, color: MUC, fontFace: FONT, align: "center", valign: "middle", lineSpacingMultiple: 1.1 });
}
slideAnh(P2, "2. Pipeline RAG — nạp học liệu và truy hồi", "1.2", "pipeline rag", [
  "Một đường ống phục vụ cả hai chức năng AI: sinh đề và trợ lý.",
  "Pha lập chỉ mục chạy nền: Tika bóc tách, chia đoạn có chồng lấp, mỗi đoạn thành vector 768 chiều trong pgvector.",
  "Pha truy hồi lọc quyền đọc TRƯỚC khi xếp hạng theo khoảng cách cosine, và không dùng chỉ mục xấp xỉ.",
]);
/* ═══════════════════ PHẦN III ═══════════════════ */
slideNgan("III", "THỰC NGHIỆM");
const P3 = "III. THỰC NGHIỆM";
cachLamCoAnh(P3, "1. Phòng đấu thời gian thực — cách làm", "3.5", "phòng đấu", [
  ["Vào phòng", "Mã PIN sáu số hoặc quét mã QR — khách không cần tài khoản"],
  ["Đồng bộ trạng thái", "STOMP trên WebSocket, xác thực tại khung CONNECT"],
  ["Chạy nhiều tiến trình", "Phát tán sự kiện qua Redis Pub/Sub"],
  ["Tính điểm theo tốc độ", "Độ trễ thành yêu cầu CHỨC NĂNG, không chỉ là chỉ tiêu"],
]);
cachLamCoAnh(P3, "2. Sinh đề bằng AI từ học liệu — cách làm", "3.6", "sinh đề", [
  ["Nạp học liệu", "Tika bóc tách → chia đoạn → vector trong pgvector"],
  ["Truy hồi có lọc quyền", "Lọc quyền đọc TRƯỚC khi xếp hạng theo cosine"],
  ["Sinh có cấu trúc", "Buộc trả JSON theo lược đồ, máy chủ kiểm chứng lại"],
  ["Người duyệt cuối", "Câu sinh ra là BẢN NHÁP, người tạo tích chọn mới lưu"],
]);
cachLamCoAnh(P3, "3. Trợ lý học tập — cách làm", "3.7", "trợ lý học tập", [
  ["Dùng chung đường ống", "Cùng kho vector với chức năng sinh đề"],
  ["Trả lời theo luồng", "Đẩy từng mảnh qua SSE, chữ hiện dần"],
  ["Nêu nguồn", "Kèm trích dẫn: tài liệu và đoạn đã dựa vào"],
  ["Không suy đoán", "Ngoài học liệu thì trả lời không tìm thấy"],
]);
cachLamCoAnh(P3, "4. Lộ trình học cá nhân hoá — cách làm", "3.8", "lộ trình học", [
  ["Dựng đồ thị", "Ba loại nút: người học, quiz, chủ đề"],
  ["Đồng bộ một chiều", "PostgreSQL là nguồn sự thật, Neo4j là hình chiếu"],
  ["Tìm chủ đề còn yếu", "Xếp thứ tự chủ đề nên ôn theo độ chính xác"],
  ["Gợi ý theo người giống mình", "Truy vấn đi qua hai cạnh của đồ thị"],
]);
{
  const s = trang(P3, "5. Kết quả đo hiệu năng phòng đấu thời gian thực");
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
  const s = trang(P3, "6. Kết quả đo độ chính xác các chức năng AI");
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
  const s = trang(P3, "7. Kiểm thử");
  the(s, 0.62, 1.44, 2.0, 1.4, "568", "phép kiểm máy chủ", "56 lớp · 0 hỏng");
  the(s, 2.82, 1.44, 2.0, 1.4, "128", "phép kiểm giao diện", "21 tệp · 0 hỏng");
  the(s, 5.02, 1.44, 2.0, 1.4, "31", "ca kiểm thử tay", "trên trình duyệt thật");
  the(s, 7.22, 1.44, 2.16, 1.4, "38", "trang được quét", "bằng 4 vai trò");
  y(s, [
    "Kiểm thử theo tháp — nhiều ở tầng thấp, ít ở tầng cao nhưng phủ đường đi thật.",
    "Testcontainers dựng PostgreSQL thật có pgvector cho mỗi lần chạy.",
    "Ba lỗi thật lộ ra khi dùng, không qua kiểm thử.",
  ], { y: 3.1, h: 1.6, co: 12 });
}

/* ═══════════════════ PHẦN IV ═══════════════════ */
slideNgan("IV", "KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN");
const P4 = "IV. KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN";
/* Hai slide dưới đây theo đúng bố cục mẫu của khoa: cột nhãn bên trái nối bằng một đường dọc, thẻ mô
 * tả bên phải; riêng slide hướng phát triển dùng nhãn hình mũi tên và có khối "Một số hạn chế" bên
 * dưới. Hạn chế vì thế chuyển hẳn sang slide 22, trả slide 21 về đúng một việc là kết quả. */
{
  const s = trang(P4, "1. Kết quả đạt được");

  /* Bốn hàng nói về SẢN PHẨM — đã dựng được những gì trên tổng thể trang web — chứ không đặt con số
   * đo đạc ở đây.
   *
   * Vì sao không để số: slide 17 và 18 đã là hai slide số liệu, mỗi con số ở đó đều có phương pháp đo
   * và phần giới hạn đi kèm để trả lời. Rải thêm số sang slide kết quả là mở thêm một mặt trận nữa mà
   * không thêm được thông tin gì — hội đồng sẽ hỏi lại đúng những con số ấy ở một chỗ không có chỗ
   * trình bày cách đo. */
  const ket = [
    ["NGÂN HÀNG\nCÂU HỎI VÀ ĐỀ THI", "Xây dựng được chức năng soạn câu hỏi đủ loại, quản lý ngân hàng đề và phân quyền công khai hay riêng tư."],
    ["LÀM BÀI\nVÀ PHÒNG ĐẤU", "Xây dựng chức năng luyện tập, thi có giới hạn thời gian và phòng đấu nhiều người theo thời gian thực."],
    ["TÍCH HỢP\nTRÍ TUỆ NHÂN TẠO", "Xây dựng chức năng sinh đề và trợ lý bám theo học liệu, cùng chức năng chấm câu tự luận."],
    ["GỢI Ý\nVÀ THỐNG KÊ", "Xây dựng hệ gợi ý quiz và lộ trình cá nhân hoá, bảng xếp hạng và trang thống kê."],
  ];

  // Đường nối dọc vẽ TRƯỚC các huy hiệu để huy hiệu nằm đè lên, không bị đường cắt ngang qua.
  const tam = (i) => 1.4 + i * 0.88 + 0.4;
  s.addShape(pres.ShapeType.line, { x: 0.87, y: tam(0), w: 0, h: tam(3) - tam(0), line: { color: XANH_NHAT, width: 1.75 } });

  ket.forEach(([t, m], i) => {
    const yy = 1.4 + i * 0.88;
    s.addShape(pres.ShapeType.ellipse, { x: 0.62, y: yy + 0.15, w: 0.5, h: 0.5, fill: { color: XANH_DAM } });
    s.addText(String(i + 1), { x: 0.62, y: yy + 0.15, w: 0.5, h: 0.5, fontSize: 15, bold: true, color: TRANG, fontFace: FONT, align: "center", valign: "middle" });
    s.addShape(pres.ShapeType.roundRect, { x: 1.26, y: yy, w: 2.3, h: 0.8, fill: { color: XANH_DAM }, rectRadius: 0.1 });
    s.addText(t, { x: 1.3, y: yy, w: 2.22, h: 0.8, fontSize: 10.5, bold: true, color: TRANG, fontFace: FONT, align: "center", valign: "middle", lineSpacingMultiple: 1.05 });
    s.addShape(pres.ShapeType.roundRect, { x: 3.68, y: yy, w: 5.72, h: 0.8, fill: { color: TRANG }, line: { color: XANH_NHAT, width: 1 }, rectRadius: 0.08 });
    s.addText(m, { x: 3.86, y: yy, w: 5.4, h: 0.8, fontSize: 11.5, color: DAM, fontFace: FONT, valign: "middle", lineSpacingMultiple: 1.12 });
  });
}
{
  const s = trang(P4, "2. Hướng phát triển");

  // Nhãn hình mũi tên (homePlate) đúng như mẫu — mỗi hướng là lời đáp cho một hạn chế ở khối dưới.
  [
    ["Nâng cấp phép đo", "Nhiều máy chủ, có độ trễ mạng thật"],
    ["Mở rộng đánh giá AI", "Tăng cỡ mẫu, nhiều người chấm độc lập"],
    ["Mở rộng sản phẩm", "Ứng dụng di động  ·  nhiều mức nhận thức"],
  ].forEach(([t, m], i) => {
    const yy = 1.32 + i * 0.82;
    s.addShape(pres.ShapeType.ellipse, { x: 0.62, y: yy + 0.08, w: 0.56, h: 0.56, fill: { color: XANH_DAM } });
    s.addText(String(i + 1), { x: 0.62, y: yy + 0.08, w: 0.56, h: 0.56, fontSize: 15, bold: true, color: TRANG, fontFace: FONT, align: "center", valign: "middle" });
    s.addShape(pres.ShapeType.homePlate, { x: 1.22, y: yy, w: 2.5, h: 0.72, fill: { color: XANH_DAM } });
    s.addText(t, { x: 1.3, y: yy, w: 2.2, h: 0.72, fontSize: 11.5, bold: true, color: TRANG, fontFace: FONT, align: "center", valign: "middle" });
    s.addShape(pres.ShapeType.roundRect, { x: 3.84, y: yy, w: 5.56, h: 0.72, fill: { color: TRANG }, line: { color: XANH_NHAT, width: 1 }, rectRadius: 0.08 });
    s.addText(m, { x: 3.98, y: yy, w: 5.28, h: 0.72, fontSize: 12.5, color: DAM, fontFace: FONT, align: "center", valign: "middle" });
  });

  s.addShape(pres.ShapeType.roundRect, { x: 3.55, y: 3.84, w: 2.9, h: 0.36, fill: { color: XANH_DAM }, rectRadius: 0.1 });
  s.addText("Một số hạn chế", { x: 3.55, y: 3.84, w: 2.9, h: 0.36, fontSize: 12.5, bold: true, color: TRANG, fontFace: FONT, align: "center", valign: "middle" });

  /* Hạn chế thứ ba KHÔNG dùng lại câu "chưa quan sát được một lần chuyển nhà cung cấp" của báo cáo:
   * nhật ký gọi AI tối 01/10 cho thấy đúng việc đó đã xảy ra (Gemini lỗi tạm thời → Groq trả lời
   * thành công), nên câu ấy nay không còn đúng. Thay bằng một hạn chế vẫn đúng và kiểm được trong mã
   * nguồn: chỉ Gemini có API embedding nên phần truy hồi không có nhà cung cấp dự phòng. */
  [
    ["Phạm vi đo", "Một máy đơn, không có mạng thật"],
    ["Cỡ mẫu AI", "Mẫu nhỏ, chưa có nhiều người chấm"],
    ["Dự phòng mô hình", "Embedding chưa có dự phòng"],
  ].forEach(([t, m], i) => {
    const x = 0.6 + i * 2.95;
    s.addShape(pres.ShapeType.roundRect, { x, y: 4.3, w: 2.75, h: 0.76, fill: { color: TRANG }, line: { color: XANH_NHAT, width: 1 }, rectRadius: 0.08 });
    s.addText(`${i + 1}. ${t}`, { x: x + 0.14, y: 4.36, w: 2.47, h: 0.26, fontSize: 11.5, bold: true, color: XANH_DAM, fontFace: FONT, valign: "middle" });
    s.addText(m, { x: x + 0.14, y: 4.62, w: 2.47, h: 0.38, fontSize: 10.5, color: MUC, fontFace: FONT, valign: "top", lineSpacingMultiple: 1.05 });
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
