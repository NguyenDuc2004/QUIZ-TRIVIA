/* Sinh slide bảo vệ ĐATN Quiz/Trivia tích hợp AI (.pptx) bằng pptxgenjs.
 *
 * Chạy:  cd bao-cao-datn/build && node gen-slides.js  -> ../Slide-BaoVe-QuizAI.pptx
 *
 * ## Nguyên tắc nội dung
 * Mọi con số trên slide phải là số ĐÃ ĐO, truy được về mục tương ứng của báo cáo. Slide bảo vệ là nơi
 * người ta hỏi lại từng con số, nên một số ước lượng lọt vào đây nguy hiểm hơn nằm trong báo cáo.
 *
 * ## Hình
 * Dùng lại đúng bộ hình của báo cáo trong `../assets/`. Hình nào thiếu thì slide vẫn dựng nhưng in
 * cảnh báo ra màn hình — thà biết slide nào đang trống còn hơn phát hiện lúc đang trình bày.
 */
const path = require("path");
const fs = require("fs");
const pptxgen = require("pptxgenjs");

const ASSETS = path.join(__dirname, "..", "assets");
const OUT = path.join(__dirname, "..", "Slide-BaoVe-QuizAI.pptx");

/* Bảng màu lấy theo giao diện sản phẩm (docs/ui-design-system.md): tím đặc làm màu nhấn. */
const TIM = "7C3AED";
const TIM_NHAT = "F5F3FF";
const MUC = "475569";
const DAM = "0F172A";
const NEN = "FFFFFF";
const VIEN = "E2E8F0";

const thieu = [];
const anh = (ten) => {
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

/** Đặt ảnh vào giữa khung cho trước, giữ nguyên tỉ lệ. */
function datAnh(s, p, khung) {
  const { w, h } = coAnh(p);
  const ty = Math.min(khung.w / w, khung.h / h);
  const W = w * ty;
  const H = h * ty;
  s.addImage({ path: p, x: khung.x + (khung.w - W) / 2, y: khung.y + (khung.h - H) / 2, w: W, h: H });
}

const pres = new pptxgen();
pres.layout = "LAYOUT_16x9"; // 10 x 5.625 inch
const W = 10;
const H = 5.625;

pres.defineSlideMaster({
  title: "CHINH",
  background: { color: NEN },
  objects: [
    { rect: { x: 0, y: 0, w: W, h: 0.62, fill: { color: TIM } } },
    { rect: { x: 0, y: H - 0.32, w: W, h: 0.32, fill: { color: TIM_NHAT } } },
  ],
});

let so = 0;

/** Slide nội dung: tiêu đề trên nền tím, chân trang có số slide. */
function trang(tieuDe) {
  so++;
  const s = pres.addSlide({ masterName: "CHINH" });
  s.addText(tieuDe, { x: 0.45, y: 0.06, w: W - 1.4, h: 0.5, fontSize: 20, bold: true, color: "FFFFFF", valign: "middle" });
  s.addText(`${so}`, { x: W - 0.75, y: H - 0.32, w: 0.4, h: 0.32, fontSize: 10, color: MUC, align: "right", valign: "middle" });
  s.addText("Quiz/Trivia tích hợp AI · Nguyễn Khắc Minh Đức", { x: 0.45, y: H - 0.32, w: 6, h: 0.32, fontSize: 9, color: MUC, valign: "middle" });
  return s;
}

/** Thẻ số liệu — dùng cho các slide kết quả đo. */
function the(s, x, y, w, h, so_, nhan, phu) {
  s.addShape(pres.ShapeType.roundRect, { x, y, w, h, fill: { color: TIM_NHAT }, line: { color: VIEN, width: 1 }, rectRadius: 0.08 });
  s.addText(so_, { x, y: y + 0.1, w, h: h * 0.45, fontSize: 26, bold: true, color: TIM, align: "center", valign: "middle" });
  s.addText(nhan, { x, y: y + h * 0.5, w, h: h * 0.26, fontSize: 11, color: DAM, align: "center", valign: "middle" });
  if (phu) s.addText(phu, { x, y: y + h * 0.74, w, h: h * 0.24, fontSize: 9, color: MUC, align: "center", valign: "middle" });
}

/** Danh sách gạch đầu dòng, cỡ chữ đủ lớn để đọc từ cuối phòng. */
function y(s, muc, o = {}) {
  s.addText(
    muc.map((t) => (typeof t === "string" ? { text: t, options: { bullet: { code: "2022" }, breakLine: true } } : t)),
    { x: o.x ?? 0.55, y: o.y ?? 0.95, w: o.w ?? W - 1.1, h: o.h ?? 3.9, fontSize: o.co ?? 14, color: DAM, lineSpacingMultiple: 1.35, valign: "top" },
  );
}

/* ─────────────────────────────── 1. Bìa ─────────────────────────────── */
{
  const s = pres.addSlide();
  s.background = { color: TIM };
  s.addText("TRƯỜNG ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI — KHOA CÔNG NGHỆ THÔNG TIN", {
    x: 0.6, y: 0.5, w: W - 1.2, h: 0.3, fontSize: 12, color: "DDD6FE", align: "center",
  });
  s.addText("ĐỒ ÁN TỐT NGHIỆP", { x: 0.6, y: 0.95, w: W - 1.2, h: 0.4, fontSize: 16, color: "EDE9FE", align: "center", charSpacing: 3 });
  s.addText("XÂY DỰNG ỨNG DỤNG QUIZ/TRIVIA\nTÍCH HỢP TRÍ TUỆ NHÂN TẠO", {
    x: 0.6, y: 1.5, w: W - 1.2, h: 1.3, fontSize: 30, bold: true, color: "FFFFFF", align: "center", lineSpacingMultiple: 1.15,
  });
  s.addShape(pres.ShapeType.line, { x: W / 2 - 1, y: 2.95, w: 2, h: 0, line: { color: "C4B5FD", width: 2 } });
  s.addText(
    [
      { text: "Sinh viên thực hiện: ", options: { color: "DDD6FE" } },
      { text: "Nguyễn Khắc Minh Đức", options: { bold: true, color: "FFFFFF" } },
      { text: "   ·   MSV: 2022601585", options: { color: "DDD6FE", breakLine: true } },
      { text: "Giảng viên hướng dẫn: ", options: { color: "DDD6FE" } },
      { text: "ThS. Nguyễn Đức Lưu", options: { bold: true, color: "FFFFFF" } },
    ],
    { x: 0.6, y: 3.3, w: W - 1.2, h: 1, fontSize: 14, align: "center", lineSpacingMultiple: 1.4 },
  );
  s.addText("Hà Nội, 09/2026", { x: 0.6, y: 4.6, w: W - 1.2, h: 0.3, fontSize: 12, italic: true, color: "DDD6FE", align: "center" });
}

/* ─────────────────────────────── 2. Nội dung ─────────────────────────────── */
{
  const s = trang("Nội dung trình bày");
  const muc = [
    ["1", "Đặt vấn đề và mục tiêu"],
    ["2", "Công nghệ và kiến trúc hệ thống"],
    ["3", "Phân tích và thiết kế"],
    ["4", "Sản phẩm đã hoàn thành"],
    ["5", "Kết quả đo hiệu năng và độ chính xác AI"],
    ["6", "Kết luận, hạn chế và hướng phát triển"],
  ];
  muc.forEach(([n, t], i) => {
    const yy = 1.0 + i * 0.66;
    s.addShape(pres.ShapeType.ellipse, { x: 1.35, y: yy, w: 0.42, h: 0.42, fill: { color: TIM } });
    s.addText(n, { x: 1.35, y: yy, w: 0.42, h: 0.42, fontSize: 14, bold: true, color: "FFFFFF", align: "center", valign: "middle" });
    s.addText(t, { x: 1.95, y: yy, w: 6.8, h: 0.42, fontSize: 16, color: DAM, valign: "middle" });
  });
}

/* ─────────────────────────────── 3. Đặt vấn đề ─────────────────────────────── */
{
  const s = trang("1. Đặt vấn đề");
  y(s, [
    "Các nền tảng quiz trực tuyến hiện có mạnh ở phần tổ chức trò chơi, nhưng để lại ba khoảng trống:",
  ], { h: 0.6 });
  const kt = [
    ["Soạn đề thủ công", "Giáo viên phải tự gõ từng câu, dù học liệu của môn đã có sẵn dưới dạng tài liệu"],
    ["Không chấm được câu tự luận", "Chỉ chấm được câu có đáp án xác định; câu trả lời ngắn vẫn phải chấm tay"],
    ["Gợi ý theo lượt xem, không theo năng lực", "Người học không biết mình yếu chủ đề nào và nên ôn gì tiếp theo"],
  ];
  kt.forEach(([t, m], i) => {
    const yy = 1.5 + i * 1.15;
    s.addShape(pres.ShapeType.roundRect, { x: 0.55, y: yy, w: W - 1.1, h: 1.0, fill: { color: TIM_NHAT }, line: { color: VIEN, width: 1 }, rectRadius: 0.06 });
    s.addText(t, { x: 0.8, y: yy + 0.1, w: W - 1.6, h: 0.35, fontSize: 15, bold: true, color: TIM, valign: "middle" });
    s.addText(m, { x: 0.8, y: yy + 0.45, w: W - 1.6, h: 0.45, fontSize: 12, color: MUC, valign: "top" });
  });
}

/* ─────────────────────────────── 4. Mục tiêu ─────────────────────────────── */
{
  const s = trang("1. Mục tiêu — bốn trọng tâm theo phiếu giao đề tài");
  const tru = [
    ["Phòng đấu thời gian thực", "Nhiều người chơi cùng lúc, độ trễ thấp, tính điểm theo tốc độ trả lời"],
    ["Sinh đề và trợ lý bằng RAG", "Sinh câu hỏi từ chính học liệu; trợ lý trả lời kèm trích dẫn nguồn"],
    ["Gợi ý cá nhân hoá bằng Neo4j", "Phân tích hành vi làm bài trên đồ thị để gợi ý quiz và lộ trình ôn"],
    ["Đo hiệu năng và độ chính xác AI", "Không chỉ làm chạy được mà phải đo và báo cáo bằng số liệu thật"],
  ];
  tru.forEach(([t, m], i) => {
    const c = i % 2;
    const r = Math.floor(i / 2);
    const x = 0.55 + c * 4.5;
    const yy = 1.0 + r * 1.9;
    s.addShape(pres.ShapeType.roundRect, { x, y: yy, w: 4.35, h: 1.7, fill: { color: "FFFFFF" }, line: { color: TIM, width: 1.5 }, rectRadius: 0.08 });
    s.addShape(pres.ShapeType.ellipse, { x: x + 0.22, y: yy + 0.22, w: 0.38, h: 0.38, fill: { color: TIM } });
    s.addText(String(i + 1), { x: x + 0.22, y: yy + 0.22, w: 0.38, h: 0.38, fontSize: 13, bold: true, color: "FFFFFF", align: "center", valign: "middle" });
    s.addText(t, { x: x + 0.72, y: yy + 0.2, w: 3.4, h: 0.42, fontSize: 14, bold: true, color: DAM, valign: "middle" });
    s.addText(m, { x: x + 0.25, y: yy + 0.72, w: 3.85, h: 0.85, fontSize: 11.5, color: MUC, valign: "top" });
  });
}

/* ─────────────────────────────── 5. Công nghệ ─────────────────────────────── */
{
  const s = trang("2. Công nghệ sử dụng");
  const nhom = [
    ["Máy chủ", "Java 21 · Spring Boot 3.5\nSpring Security · Data JPA\nWebSocket (STOMP) · Flyway"],
    ["Giao diện", "React 19 · TypeScript\nVite 8 · Ant Design v6\nTailwind CSS v4"],
    ["Dữ liệu", "PostgreSQL 16 + pgvector\nNeo4j 5\nRedis 7"],
    ["Trí tuệ nhân tạo", "Google Gemini (chính)\nGroq (dự phòng)\nApache Tika · RAG tự viết"],
  ];
  nhom.forEach(([t, m], i) => {
    const x = 0.45 + i * 2.32;
    s.addShape(pres.ShapeType.roundRect, { x, y: 1.05, w: 2.14, h: 3.3, fill: { color: TIM_NHAT }, line: { color: VIEN, width: 1 }, rectRadius: 0.08 });
    s.addShape(pres.ShapeType.rect, { x, y: 1.05, w: 2.14, h: 0.5, fill: { color: TIM } });
    s.addText(t, { x, y: 1.05, w: 2.14, h: 0.5, fontSize: 13, bold: true, color: "FFFFFF", align: "center", valign: "middle" });
    s.addText(m, { x: x + 0.12, y: 1.7, w: 1.9, h: 2.5, fontSize: 11.5, color: DAM, align: "center", valign: "top", lineSpacingMultiple: 1.3 });
  });
  s.addText("Không dùng Spring AI hay LangChain4j — lớp điều phối mô hình tự hiện thực để kiểm soát dự phòng, hạn mức và nhật ký.", {
    x: 0.55, y: 4.55, w: W - 1.1, h: 0.4, fontSize: 11, italic: true, color: MUC, align: "center",
  });
}

/* ─────────────────────────────── 6-8. Hình kiến trúc ─────────────────────────────── */
const HINH = [
  ["2. Kiến trúc tổng thể hệ thống", "1.1", "Ba kênh giao tiếp: REST cho nghiệp vụ, WebSocket cho phòng đấu, SSE cho luồng trả lời của trợ lý."],
  ["2. Pipeline RAG — nạp học liệu và truy hồi", "1.2", "Lọc quyền đọc TRƯỚC khi xếp hạng theo khoảng cách, không dùng chỉ mục xấp xỉ."],
  ["3. Biểu đồ use case tổng quát", "2.1", "Bốn tác nhân: Khách, Người học, Người tạo nội dung, Quản trị viên."],
  ["3. Thiết kế cơ sở dữ liệu", "2.28", "35 bảng trên PostgreSQL, dựng qua 23 tệp migration Flyway đánh số."],
  ["3. Phân lớp và cấu trúc mô-đun", "2.29", "Khối đơn mô-đun hoá, chia theo nghiệp vụ; mỗi mô-đun có đủ năm tầng bên trong."],
];
for (const [tieuDe, hinh, chu] of HINH) {
  const s = trang(tieuDe);
  const p = anh(hinh);
  if (p) datAnh(s, p, { x: 0.5, y: 0.85, w: W - 1.0, h: 3.5 });
  s.addText(chu, { x: 0.55, y: 4.5, w: W - 1.1, h: 0.45, fontSize: 11.5, color: MUC, align: "center", valign: "middle" });
}

/* ─────────────────────────────── 9-13. Sản phẩm ─────────────────────────────── */
const MAN = [
  ["4. Khám phá quiz và gợi ý cá nhân hoá", "3.3", "Gợi ý sinh từ đồ thị Neo4j, kèm lý do vì sao được gợi ý."],
  ["4. Học liệu và sinh đề bằng AI", "3.7", "Câu hỏi AI sinh phải qua bước người tạo nội dung duyệt mới vào ngân hàng."],
  ["4. Trợ lý học tập", "3.8", "Trả lời theo luồng, kèm khối trích dẫn đoạn học liệu đã dựa vào."],
  ["4. Lộ trình học cá nhân hoá", "3.9", "Thứ tự chủ đề nên ôn, dựng từ năng lực đo được trên từng chủ đề."],
  ["4. Trang quản trị và giám sát chi phí AI", "3.13", "Số liệu đọc trực tiếp từ cơ sở dữ liệu tại thời điểm mở trang."],
];
for (const [tieuDe, hinh, chu] of MAN) {
  const s = trang(tieuDe);
  const p = anh(hinh);
  if (p) datAnh(s, p, { x: 0.5, y: 0.85, w: W - 1.0, h: 3.5 });
  s.addText(chu, { x: 0.55, y: 4.5, w: W - 1.1, h: 0.45, fontSize: 11.5, color: MUC, align: "center", valign: "middle" });
}

/* ── Phòng đấu: trụ cột số một nhưng CHƯA CÓ ảnh chụp (hinh-3.6 là một trong ba ảnh phải chụp tay).
 * Dựng slide bằng nội dung thay vì bỏ trống — một trụ cột vắng mặt trong bài bảo vệ tệ hơn nhiều so
 * với một slide không có ảnh. Có ảnh rồi thì thêm `datAnh` vào đây. */
{
  const s = trang("4. Phòng đấu thời gian thực");
  const b = [
    ["Vào phòng", "Mã PIN sáu số hoặc quét mã QR. Khách chưa có tài khoản vẫn chơi được khi chủ phòng cho phép, dùng khoá phiên riêng chỉ mở đúng một phòng."],
    ["Đồng bộ trạng thái", "STOMP trên WebSocket; xác thực tại khung CONNECT vì trình duyệt không cho gắn tiêu đề vào yêu cầu nâng cấp WebSocket."],
    ["Chạy nhiều tiến trình", "Sự kiện phát tán qua Redis Pub/Sub để mọi tiến trình đang giữ kết nối của phòng đều nhận được và phát tiếp cho người chơi của mình."],
    ["Tính điểm theo tốc độ", "Điểm phụ thuộc thời gian trả lời — nên độ trễ trở thành yêu cầu chức năng, không chỉ là chỉ tiêu kỹ thuật."],
  ];
  b.forEach(([t, m], i) => {
    const yy = 0.95 + i * 0.95;
    s.addShape(pres.ShapeType.rect, { x: 0.55, y: yy, w: 0.07, h: 0.8, fill: { color: TIM } });
    s.addText(t, { x: 0.78, y: yy, w: 2.3, h: 0.8, fontSize: 13.5, bold: true, color: TIM, valign: "middle" });
    s.addText(m, { x: 3.05, y: yy, w: W - 3.6, h: 0.8, fontSize: 12, color: DAM, valign: "middle" });
  });
  s.addText("Ảnh chụp phòng chờ và màn chơi sẽ chèn vào slide này (Hình 3.6 của báo cáo).", {
    x: 0.55, y: 4.8, w: W - 1.1, h: 0.35, fontSize: 10, italic: true, color: MUC, align: "center",
  });
}

/* ─────────────────────────────── 14. Hiệu năng ─────────────────────────────── */
{
  const s = trang("5. Kết quả đo hiệu năng phòng đấu thời gian thực");
  const p = anh("3.17");
  if (p) datAnh(s, p, { x: 0.5, y: 0.8, w: 5.9, h: 3.1 });
  the(s, 6.6, 0.9, 1.5, 1.35, "216 ms", "P95", "ở 100 người");
  the(s, 8.25, 0.9, 1.3, 1.35, "0", "sự kiện mất", "mọi mức tải");
  the(s, 6.6, 2.45, 1.5, 1.35, "200", "người/phòng", "mức đã thử");
  the(s, 8.25, 2.45, 1.3, 1.35, "2 ms", "qua Redis", "mỗi sự kiện");
  s.addText(
    "Đo ngày 08/08/2026 trên một máy đơn, KHÔNG bao gồm độ trễ mạng thật — đây là chi phí xử lý của máy chủ và tầng phát tán.",
    { x: 0.55, y: 4.25, w: W - 1.1, h: 0.6, fontSize: 11, italic: true, color: MUC, align: "center", valign: "middle" },
  );
}

/* ─────────────────────────────── 15. Độ chính xác AI ─────────────────────────────── */
{
  const s = trang("5. Kết quả đo độ chính xác các chức năng AI");
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
      ["Chức năng", "Chỉ số", "Kết quả"].map((t) => ({ text: t, options: { bold: true, color: "FFFFFF", fill: { color: TIM }, fontSize: 13 } })),
      ...hang.map((r) => r.map((t, i) => ({ text: t, options: { fontSize: 12.5, bold: i === 2, color: i === 2 ? TIM : DAM, align: i === 2 ? "center" : "left" } }))),
    ],
    { x: 0.6, y: 0.95, w: W - 1.2, colW: [2.6, 4.4, 1.8], border: { type: "solid", color: VIEN, pt: 1 }, rowH: 0.36, valign: "middle", margin: 0.06 },
  );
  s.addText("Đo ngày 14/08/2026. Cỡ mẫu nhỏ — đủ phát hiện lỗi hệ thống và xu hướng, chưa đủ cho kết luận thống kê.", {
    x: 0.55, y: 4.35, w: W - 1.1, h: 0.45, fontSize: 11, italic: true, color: MUC, align: "center", valign: "middle",
  });
}

/* ─────────────────────────────── 16. Kiểm thử ─────────────────────────────── */
{
  const s = trang("5. Kiểm thử");
  the(s, 0.6, 1.0, 2.0, 1.5, "606", "phép kiểm máy chủ", "54 lớp · 0 hỏng");
  the(s, 2.8, 1.0, 2.0, 1.5, "128", "phép kiểm giao diện", "21 tệp · 0 hỏng");
  the(s, 5.0, 1.0, 2.0, 1.5, "31", "ca kiểm thử tay", "trên trình duyệt thật");
  the(s, 7.2, 1.0, 2.2, 1.5, "38", "trang được quét", "bằng 4 vai trò");
  y(s, [
    "Kiểm thử theo tháp: nhiều phép kiểm ở tầng thấp, ít nhưng phủ đường đi thật ở tầng cao.",
    "Kiểm thử tích hợp dùng Testcontainers dựng PostgreSQL thật có pgvector cho mỗi lần chạy.",
    "Ba lỗi thật của sản phẩm lộ ra khi dùng thật chứ không qua kiểm thử — một trong số đó có hẳn một phép kiểm khẳng định đúng cái hành vi sai.",
  ], { y: 2.75, h: 1.6, co: 13 });
}

/* ─────────────────────────────── 17. Kết luận ─────────────────────────────── */
{
  const s = trang("6. Kết luận");
  s.addText("Đã hoàn thành", { x: 0.55, y: 0.95, w: 4.3, h: 0.35, fontSize: 15, bold: true, color: TIM });
  y(s, [
    "16 nhóm chức năng với 87 yêu cầu chức năng",
    "Bốn trọng tâm của phiếu giao đề tài đều có sản phẩm và số liệu đối chứng",
    "Bảy nhóm chức năng mở rộng ngoài yêu cầu bắt buộc",
  ], { x: 0.55, y: 1.35, w: 4.3, h: 2.0, co: 12.5 });

  s.addText("Hạn chế", { x: 5.15, y: 0.95, w: 4.3, h: 0.35, fontSize: 15, bold: true, color: "B45309" });
  y(s, [
    "Số liệu đo trên một máy đơn, không có độ trễ mạng thật",
    "Cỡ mẫu đánh giá AI nhỏ; chấm đối chiếu với đáp án theo tiêu chí, chưa phải với nhiều giáo viên",
    "Chưa quan sát được một lần chuyển nhà cung cấp mô hình do lỗi tạm thời",
  ], { x: 5.15, y: 1.35, w: 4.3, h: 2.0, co: 12.5 });

  s.addShape(pres.ShapeType.roundRect, { x: 0.55, y: 3.5, w: W - 1.1, h: 1.0, fill: { color: TIM_NHAT }, line: { color: TIM, width: 1 }, rectRadius: 0.06 });
  s.addText(
    "Phần khó nhất của một hệ thống tích hợp mô hình ngôn ngữ không nằm ở việc gọi được mô hình, mà ở việc dựng đủ hàng rào quanh nó: giới hạn miền giá trị, kiểm chứng cấu trúc đầu ra, cách ly quyền đọc dữ liệu, và giữ quyền kết luận cuối cùng cho con người.",
    { x: 0.8, y: 3.6, w: W - 1.6, h: 0.8, fontSize: 12.5, italic: true, color: DAM, valign: "middle" },
  );
}

/* ─────────────────────────────── 18. Hướng phát triển ─────────────────────────────── */
{
  const s = trang("6. Hướng phát triển");
  const gd = [
    ["Ngắn hạn", "Sửa hạn chế hiển thị nguồn dư của trợ lý bằng cách đo phân bố khoảng cách thực tế rồi mới chọn ngưỡng · dọn tệp ảnh mồ côi"],
    ["Trung hạn", "Đo trên hạ tầng nhiều máy chủ có độ trễ mạng thật · tăng cỡ mẫu đánh giá AI và mời nhiều người chấm độc lập"],
    ["Dài hạn", "Ứng dụng di động cho phòng đấu · sinh câu hỏi theo nhiều mức nhận thức · mở rộng phân tích đồ thị sang câu hỏi có giá trị sư phạm"],
  ];
  gd.forEach(([t, m], i) => {
    const yy = 1.05 + i * 1.2;
    s.addShape(pres.ShapeType.rect, { x: 0.55, y: yy, w: 0.08, h: 1.0, fill: { color: TIM } });
    s.addText(t, { x: 0.78, y: yy, w: 1.8, h: 0.4, fontSize: 14, bold: true, color: TIM, valign: "middle" });
    s.addText(m, { x: 0.78, y: yy + 0.38, w: W - 1.5, h: 0.62, fontSize: 12, color: DAM, valign: "top" });
  });
}

/* ─────────────────────────────── 19. Cảm ơn ─────────────────────────────── */
{
  const s = pres.addSlide();
  s.background = { color: TIM };
  s.addText("EM XIN CHÂN THÀNH CẢM ƠN", { x: 0.6, y: 2.0, w: W - 1.2, h: 0.7, fontSize: 30, bold: true, color: "FFFFFF", align: "center" });
  s.addText("Kính mong nhận được ý kiến đóng góp của các thầy cô", {
    x: 0.6, y: 2.8, w: W - 1.2, h: 0.4, fontSize: 15, color: "DDD6FE", align: "center",
  });
  s.addText("Nguyễn Khắc Minh Đức · 2022601585 · GVHD: ThS. Nguyễn Đức Lưu", {
    x: 0.6, y: 3.9, w: W - 1.2, h: 0.35, fontSize: 12, color: "C4B5FD", align: "center",
  });
}

pres.writeFile({ fileName: OUT }).then(() => {
  console.log(`OK -> ${path.basename(OUT)}  ${fs.statSync(OUT).size} bytes  (${so + 2} slide)`);
  if (thieu.length) {
    console.log(`\nTHIẾU ${thieu.length} hình, slide tương ứng đang trống:`);
    for (const t of new Set(thieu)) console.log(`  ${t}`);
  }
});
