/* Dựng báo cáo ĐATN Quiz/Trivia AI ra .docx theo định dạng HaUI.
 * Parse Markdown (marked.lexer) -> docx-js. Tự sinh mục lục + danh mục hình + danh mục bảng.
 */
const fs = require("fs");
const path = require("path");
const { marked } = require("marked");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, HeadingLevel, TableOfContents, PageNumber, Header,
  BorderStyle, WidthType, ShadingType, PageBreak, NumberFormat, LevelFormat, ImageRun,
  PageBorderDisplay, PageBorderOffsetFrom,
} = require("docx");

const DIR = path.join(__dirname, "..");
const ASSETS = path.join(DIR, "assets");
const FONT = "Times New Roman";

function pngSize(buf) { return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) }; }
const MONO = "Consolas";
const SIZE = 28;            // 14pt — Phụ lục 01 mục II.1 của quy định 2025
const CONTENT_WIDTH = 8788; // A4 (11906) - left 1984 (3,5cm) - right 1134 (2cm)

// ---------- thu thập danh mục hình/bảng ----------
const figures = [];
const tables = [];

// ---------- inline ----------
function runs(tokens, style = {}) {
  const out = [];
  for (const t of tokens || []) {
    if (t.type === "text") {
      if (t.tokens && t.tokens.length) out.push(...runs(t.tokens, style));
      else out.push(new TextRun({ text: t.text, font: FONT, size: SIZE, ...style }));
    } else if (t.type === "strong") {
      out.push(...runs(t.tokens, { ...style, bold: true }));
    } else if (t.type === "em") {
      out.push(...runs(t.tokens, { ...style, italics: true }));
    } else if (t.type === "codespan") {
      out.push(new TextRun({ text: t.text, font: MONO, size: 22, ...style }));
    } else if (t.type === "link") {
      out.push(...runs(t.tokens, style));
    } else if (t.type === "br") {
      out.push(new TextRun({ break: 1 }));
    } else if (t.type === "del") {
      out.push(...runs(t.tokens, { ...style, strike: true }));
    } else if (t.text) {
      out.push(new TextRun({ text: t.text, font: FONT, size: SIZE, ...style }));
    }
  }
  return out.length ? out : [new TextRun({ text: "", font: FONT, size: SIZE })];
}

function cellBorders(color = "999999") {
  const b = { style: BorderStyle.SINGLE, size: 4, color };
  return { top: b, left: b, bottom: b, right: b };
}

function buildTable(tok) {
  const cols = tok.header.length;
  const colW = Math.floor(CONTENT_WIDTH / cols);
  const widths = Array(cols).fill(colW);

  const mkCell = (cell, i, header) =>
    new TableCell({
      width: { size: widths[i], type: WidthType.DXA },
      borders: cellBorders(),
      shading: header ? { fill: "E7EEF6", type: ShadingType.CLEAR, color: "auto" } : undefined,
      margins: { top: 60, bottom: 60, left: 100, right: 100 },
      children: [new Paragraph({
        alignment: header ? AlignmentType.CENTER : AlignmentType.LEFT,
        spacing: { line: 276, after: 0 },
        children: runs(cell.tokens, header ? { bold: true } : {}),
      })],
    });

  const rows = [];
  rows.push(new TableRow({ tableHeader: true, children: tok.header.map((c, i) => mkCell(c, i, true)) }));
  for (const r of tok.rows) rows.push(new TableRow({ children: r.map((c, i) => mkCell(c, i, false)) }));

  return new Table({ width: { size: CONTENT_WIDTH, type: WidthType.DXA }, columnWidths: widths, rows });
}

function figurePlaceholder(num, title) {
  const caption = new Paragraph({ style: "Caption", children: [new TextRun({ text: `Hình ${num}. ${title}`, font: FONT, size: SIZE, italics: true })] });
  const imgPath = path.join(ASSETS, `hinh-${num}.png`);
  if (fs.existsSync(imgPath)) {
    const data = fs.readFileSync(imgPath);
    const { w, h } = pngSize(data);
    // Khổ chữ A4 với lề 3-2-2-2 rộng 16 cm = 605 px ở 96 DPI, nên 600 px là ảnh tràn hết khổ chữ.
    // Mặc định co về 500 px (83% khổ chữ): đo trên 47 hình thì bớt được 6 trang mà không bỏ hình nào.
    // maxH 620 px = 16,4 cm, thay cho 820 px vốn cho một hình cao chiếm tới 84% chiều cao trang.
    //
    // CHỐT ĐỌC ĐƯỢC. Co ảnh làm chữ TRONG sơ đồ nhỏ theo. Mermaid vẽ ở `fontSize` 16px với `-s 2`, nên
    // chữ cao 32 px trong tệp PNG; sau khi co còn `32 × scale` px, in ở 96 DPI ra `32 × scale / 96 × 25,4`
    // mm. Chữ 10pt trên giấy cao khoảng 2,5 mm và dưới 1,5 mm thì khó đọc, nên hệ số co không được
    // xuống dưới 0,177. Hình nào vi phạm thì được giữ nguyên bề rộng 600 px — thà tốn thêm chỗ hơn là
    // in ra một sơ đồ không ai đọc được. Không có chốt này, lần hạ 600→500 đã đẩy thêm hai sơ đồ
    // xuống dưới ngưỡng mà không có gì báo.
    const CHU_PNG_PX = 32;
    const MM_TOI_THIEU = 1.5;
    const SCALE_TOI_THIEU = (MM_TOI_THIEU * 96) / (25.4 * CHU_PNG_PX);

    // CỠ ẢNH: ƯU TIÊN ĐỌC ĐƯỢC, KHÔNG ƯU TIÊN SỐ TRANG.
    //
    // Trước đây ảnh bị ép xuống 395 px chiều cao để hai hình xếp vừa một trang — mẹo đó bớt được 3
    // trang thật, nhưng đổi lại ảnh chụp màn hình co còn chưa tới 320 px bề ngang trong khi khổ chữ
    // rộng 605 px, tức bỏ trắng gần nửa bề ngang trang giấy ở hai bên mỗi hình. Số trang đã giải
    // quyết bằng cách khác (lược bớt use case), nên chốt đó bỏ đi.
    //
    // Nay ảnh tràn hết khổ chữ. Chặn chiều cao ở 881 px = 971 (vùng chữ A4) trừ 90 px dành cho chú
    // thích, để mỗi hình cùng chú thích của nó vẫn nằm gọn trong một trang — vượt ngưỡng này thì
    // Word đẩy chú thích sang trang sau và người đọc thấy một ảnh không tên.
    const CAO_TRANG = 952;
    const CAO_CHU_THICH = 90;
    const RONG_KHO_CHU = 580;   // khổ chữ 439,4 pt = 585,9 px; lùi 6 px cho khỏi tràn
    const CAO_TOI_DA = CAO_TRANG - CAO_CHU_THICH;

    const scale = Math.min(RONG_KHO_CHU / w, CAO_TOI_DA / h, 1);

    // Chốt đọc được giờ chỉ còn là lưới an toàn: ở cỡ này không hình nào chạm ngưỡng. Giữ lại để
    // nếu sau này thêm một sơ đồ quá cao thì có tiếng báo, thay vì in ra một hình không ai đọc nổi.
    if (scale < SCALE_TOI_THIEU) {
      console.warn(`  ! hinh-${num}: chữ trong hình co xuống ${(scale * 32 / 96 * 25.4).toFixed(2)} mm, dưới ngưỡng đọc được 1,5 mm`);
    }

    const W = Math.round(w * scale), H = Math.round(h * scale);
    return [
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { before: 160, after: 40 },
        children: [new ImageRun({ type: "png", data, transformation: { width: W, height: H }, altText: { title: `Hình ${num}`, description: title, name: `hinh-${num}` } })],
      }),
      caption,
    ];
  }
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 160, after: 40 },
      shading: { fill: "F2F2F2", type: ShadingType.CLEAR, color: "auto" },
      children: [new TextRun({ text: "[ Vị trí chèn hình (chụp màn hình sản phẩm) ]", italics: true, color: "888888", font: FONT, size: SIZE })],
    }),
    caption,
  ];
}

// ---------- block ----------
function blocksToElements(md) {
  const tokens = marked.lexer(md);
  const els = [];
  for (const tok of tokens) {
    if (tok.type === "heading") {
      const level = [HeadingLevel.HEADING_1, HeadingLevel.HEADING_2, HeadingLevel.HEADING_3, HeadingLevel.HEADING_4][Math.min(tok.depth, 4) - 1];
      els.push(new Paragraph({ heading: level, children: runs(tok.tokens) }));
    } else if (tok.type === "paragraph") {
      const txt = tok.text || "";
      // figure placeholder
      const fig = txt.match(/^\[H[ÌI]NH\s+([\d.]+):\s*([\s\S]+?)\s*[—-]\s*cần/i);
      if (fig) {
        const num = fig[1], title = fig[2].trim();
        figures.push({ num, title });
        els.push(...figurePlaceholder(num, title));
        continue;
      }
      // table caption: paragraph chỉ gồm 1 strong "Bảng x.y. ..."
      if (tok.tokens && tok.tokens.length === 1 && tok.tokens[0].type === "strong" && /^Bảng\s+(?:[A-Z]\.\d+|\d+(?:\.\d+)*)\.?\s*/.test(tok.tokens[0].text)) {
        const m = tok.tokens[0].text.match(/^Bảng\s+(\d+(?:\.\d+)*)\.?\s*([\s\S]+)$/); // chỉ bảng thân bài mới vào Danh mục bảng
        if (m) tables.push({ num: m[1], title: m[2].trim() });
        els.push(new Paragraph({
          alignment: AlignmentType.CENTER,
          spacing: { before: 120, after: 60 },
          children: [new TextRun({ text: tok.tokens[0].text, bold: true, font: FONT, size: SIZE })],
        }));
        continue;
      }
      // mục tài liệu tham khảo IEEE: "[n] ..." -> thụt treo, không thụt dòng đầu
      if (/^\[\d+\]\s/.test(txt)) {
        els.push(new Paragraph({
          alignment: AlignmentType.JUSTIFIED,
          indent: { left: 567, hanging: 567 },
          spacing: { line: 312, after: 80 },
          children: runs(tok.tokens),
        }));
        continue;
      }
      els.push(new Paragraph({ style: "BodyVN", children: runs(tok.tokens) }));
    } else if (tok.type === "list") {
      let idx = tok.start && Number(tok.start) ? Number(tok.start) : 1;
      for (const item of tok.items) {
        const itTokens = (item.tokens || []).flatMap((x) => x.tokens || (x.type === "text" ? [{ type: "text", text: x.text }] : []));
        els.push(new Paragraph({
          numbering: tok.ordered ? undefined : undefined, // dùng ký hiệu thủ công để chắc chắn
          alignment: AlignmentType.JUSTIFIED,
          indent: { left: 720, hanging: 360 },
          spacing: { line: 360, after: 60 },
          children: [
            new TextRun({ text: tok.ordered ? `${idx++}. ` : "• ", font: FONT, size: SIZE }),
            ...runs(itTokens),
          ],
        }));
      }
    } else if (tok.type === "table") {
      els.push(buildTable(tok));
      els.push(new Paragraph({ spacing: { after: 80 }, children: [] }));
    } else if (tok.type === "code") {
      for (const line of tok.text.split("\n")) {
        els.push(new Paragraph({
          spacing: { line: 240, after: 0 },
          children: [new TextRun({ text: line || " ", font: MONO, size: 18 })],
        }));
      }
      els.push(new Paragraph({ spacing: { after: 80 }, children: [] }));
    } else if (tok.type === "hr") {
      els.push(new Paragraph({ children: [new PageBreak()] }));
    } else if (tok.type === "blockquote") {
      els.push(new Paragraph({ style: "BodyVN", children: runs(tok.tokens || []) }));
    }
    // space -> bỏ qua
  }
  return els;
}

function readMd(name) { return fs.readFileSync(path.join(DIR, name), "utf8"); }

// ---------- trang bìa ----------
function center(text, opts = {}) {
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: opts.after ?? 120 }, children: [new TextRun({ text, font: FONT, bold: opts.bold ?? false, size: opts.size ?? SIZE, allCaps: opts.caps ?? false })] });
}
/* Bìa dựng theo mẫu bìa cứng của trường (Phụ lục 01 mục 9). Ba điểm khác bìa cũ:
 *   - khối GVHD / Sinh viên / Mã số sinh viên căn TRÁI và thẳng cột dấu hai chấm, không căn giữa
 *   - có dòng nhãn "TÊN ĐỀ TÀI" phía trên tên đề tài
 *   - bỏ ba dòng Lớp / Khóa / Ngành lặp lại ở cuối, vì ngành đã nêu ngay dưới "ĐỒ ÁN TỐT NGHIỆP"
 */
function dongThongTin(nhan, giaTri) {
  const o = (t, dam) => new Paragraph({
    spacing: { after: 100 },
    children: [new TextRun({ text: t, font: FONT, size: 26, bold: dam })],
  });
  const o_ = (t) => new TableCell({
    width: { size: t[1], type: WidthType.DXA },
    borders: {
      top: { style: BorderStyle.NONE }, bottom: { style: BorderStyle.NONE },
      left: { style: BorderStyle.NONE }, right: { style: BorderStyle.NONE },
    },
    margins: { top: 0, bottom: 0, left: 0, right: 0 },
    children: [o(t[0], true)],
  });
  return new TableRow({ children: [o_([nhan, 2600]), o_([": " + giaTri, 3400])] });
}

function coverElements() {
  return [
    center("BỘ CÔNG THƯƠNG", { bold: true, size: 26, after: 40 }),
    center("TRƯỜNG ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI", { bold: true, size: 28, after: 40 }),
    center("---------------------------------------", { bold: true, size: 26, after: 2300 }),

    center("ĐỒ ÁN TỐT NGHIỆP", { bold: true, size: 30, after: 80 }),
    center("NGÀNH KỸ THUẬT PHẦN MỀM", { size: 28, after: 1700 }),

    center("TÊN ĐỀ TÀI", { bold: true, size: 26, after: 100 }),
    center("XÂY DỰNG ỨNG DỤNG QUIZ/TRIVIA TÍCH HỢP", { bold: true, size: 28, after: 60 }),
    center("TRÍ TUỆ NHÂN TẠO", { bold: true, size: 28, after: 2100 }),

    new Table({
      width: { size: 6000, type: WidthType.DXA },
      columnWidths: [2600, 3400],
      indent: { size: 1500, type: WidthType.DXA },
      rows: [
        dongThongTin("GVHD", "ThS. Nguyễn Đức Lưu"),
        dongThongTin("Sinh viên", "Nguyễn Khắc Minh Đức"),
        dongThongTin("Mã số sinh viên", "2022601585"),
      ],
    }),

    center("", { after: 2700 }),
    center("HÀ NỘI - 2026", { bold: true, size: 26 }),
  ];
}

// viền trang bìa
const coverBorder = () => ({ style: BorderStyle.DOUBLE, size: 12, color: "000000", space: 24 });
const coverBorders = {
  pageBorders: { display: PageBorderDisplay.ALL_PAGES, offsetFrom: PageBorderOffsetFrom.PAGE },
  pageBorderTop: coverBorder(),
  pageBorderRight: coverBorder(),
  pageBorderBottom: coverBorder(),
  pageBorderLeft: coverBorder(),
};

// ---------- danh mục ----------
function listingParas(title, items, prefix) {
  const out = [new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: title, font: FONT, size: 32, bold: true, allCaps: true })] })];
  if (!items.length) {
    out.push(new Paragraph({ style: "BodyVN", children: [new TextRun({ text: "(Cập nhật sau khi chèn đầy đủ.)", italics: true, font: FONT, size: SIZE })] }));
    return out;
  }
  for (const it of items) {
    out.push(new Paragraph({
      spacing: { line: 312, after: 40 },
      tabStops: [{ type: "right", position: CONTENT_WIDTH, leader: "dot" }],
      children: [new TextRun({ text: `${prefix} ${it.num}. ${it.title}`, font: FONT, size: SIZE })],
    }));
  }
  return out;
}

// ---------- viết tắt ----------
const ABBR = [
  ["AI", "Artificial Intelligence — Trí tuệ nhân tạo"],
  ["ANN", "Approximate Nearest Neighbor — Tìm láng giềng gần nhất xấp xỉ"],
  ["API", "Application Programming Interface — Giao diện lập trình ứng dụng"],
  ["BCrypt", "Hàm băm mật khẩu dựa trên thuật toán Blowfish"],
  ["CSDL", "Cơ sở dữ liệu"],
  ["CTE", "Common Table Expression — Biểu thức bảng chung trong SQL"],
  ["DTO", "Data Transfer Object — Đối tượng truyền dữ liệu"],
  ["ERD", "Entity Relationship Diagram — Sơ đồ thực thể quan hệ"],
  ["FR", "Functional Requirement — Yêu cầu chức năng"],
  ["JWT", "JSON Web Token — Token xác thực dạng JSON"],
  ["LLM", "Large Language Model — Mô hình ngôn ngữ lớn"],
  ["NFR", "Non-Functional Requirement — Yêu cầu phi chức năng"],
  ["OTP", "One-Time Password — Mã dùng một lần"],
  ["OWASP", "Open Worldwide Application Security Project"],
  ["pgvector", "Extension lưu trữ và truy vấn vector của PostgreSQL"],
  ["PIN", "Personal Identification Number — Mã số tham gia phòng đấu"],
  ["PK / FK", "Primary Key / Foreign Key — Khóa chính / khóa ngoại"],
  ["Pub/Sub", "Publish–Subscribe — Cơ chế xuất bản và đăng ký thông điệp"],
  ["RAG", "Retrieval-Augmented Generation — Truy hồi tăng cường sinh"],
  ["RBAC", "Role-Based Access Control — Phân quyền theo vai trò"],
  ["REST", "Representational State Transfer"],
  ["SPA", "Single Page Application — Ứng dụng một trang"],
  ["SSE", "Server-Sent Events — Luồng sự kiện một chiều từ máy chủ"],
  ["STOMP", "Simple Text Oriented Messaging Protocol"],
  ["TTL", "Time To Live — Thời gian sống của dữ liệu"],
  ["UC", "Use Case — Trường hợp sử dụng"],
  ["UUID", "Universally Unique Identifier — Định danh duy nhất toàn cục"],
  ["VOPC", "View Of Participating Classes — Biểu đồ lớp phân tích"],
];
function abbrElements() {
  const out = [new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: "DANH MỤC CÁC THUẬT NGỮ, KÝ HIỆU VÀ TỪ VIẾT TẮT", font: FONT, size: 32, bold: true, allCaps: true })] })];
  const widths = [2400, CONTENT_WIDTH - 2400];
  const rows = [new TableRow({ tableHeader: true, children: ["Từ viết tắt", "Giải thích"].map((t, i) =>
    new TableCell({ width: { size: widths[i], type: WidthType.DXA }, borders: cellBorders(), shading: { fill: "E7EEF6", type: ShadingType.CLEAR, color: "auto" }, margins: { top: 60, bottom: 60, left: 100, right: 100 }, children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: t, bold: true, font: FONT, size: SIZE })] })] })) })];
  for (const [k, v] of ABBR) {
    rows.push(new TableRow({ children: [k, v].map((t, i) =>
      new TableCell({ width: { size: widths[i], type: WidthType.DXA }, borders: cellBorders(), margins: { top: 60, bottom: 60, left: 100, right: 100 }, children: [new Paragraph({ spacing: { line: 276 }, children: [new TextRun({ text: t, font: FONT, size: SIZE, bold: i === 0 })] })] })) }));
  }
  out.push(new Table({ width: { size: CONTENT_WIDTH, type: WidthType.DXA }, columnWidths: widths, rows }));
  return out;
}

// ---------- parse thân bài TRƯỚC (để thu thập figures/tables) ----------
// Bộ lọc `existsSync` phía dưới cho phép khai đủ danh sách ngay cả khi một phần chưa viết: file chưa có
// thì bị bỏ qua kèm cảnh báo, thay vì phải tạo file rỗng và để lại một mục trống trong mục lục.
const bodyFiles = [
  "00-mo-dau.md",
  "01-chuong-1.md",
  "02-chuong-2.md",
  "03-chuong-3.md",
  "04-ket-luan.md",
  "05-tai-lieu-tham-khao.md",
  "06-phu-luc.md",
].filter((f) => {
  const ok = fs.existsSync(path.join(DIR, f));
  if (!ok) console.warn(`(bỏ qua ${f} — chưa có file)`);
  return ok;
});
const bodyElements = [];
bodyFiles.forEach((f, i) => {
  if (i > 0) bodyElements.push(new Paragraph({ children: [new PageBreak()] }));
  bodyElements.push(...blocksToElements(readMd(f)));
});

// front matter (lời cảm ơn + cam đoan)
const fmElements = blocksToElements(readMd("front-matter.md"));

// mục lục
const tocElements = [
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: "MỤC LỤC", font: FONT, size: 32, bold: true, allCaps: true })] }),
  new TableOfContents("Mục lục", { hyperlink: true, headingStyleRange: "1-2" }),
];

const PB = () => new Paragraph({ children: [new PageBreak()] });

// ---------- styles ----------
const styles = {
  default: { document: { run: { font: FONT, size: SIZE }, paragraph: { spacing: { line: 360, after: 120 } } } },
  paragraphStyles: [
    { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { font: FONT, size: 32, bold: true, allCaps: true },
      paragraph: { alignment: AlignmentType.CENTER, outlineLevel: 0, spacing: { before: 240, after: 240 } } },
    { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { font: FONT, size: SIZE, bold: true },
      paragraph: { outlineLevel: 1, spacing: { before: 200, after: 120 } } },
    { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { font: FONT, size: SIZE, bold: true, italics: true },
      paragraph: { outlineLevel: 2, spacing: { before: 160, after: 100 } } },
    { id: "Heading4", name: "Heading 4", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { font: FONT, size: SIZE, bold: true },
      paragraph: { outlineLevel: 3, spacing: { before: 120, after: 80 } } },
    { id: "BodyVN", name: "Body VN", basedOn: "Normal", next: "BodyVN",
      run: { font: FONT, size: SIZE },
      paragraph: { alignment: AlignmentType.JUSTIFIED, indent: { firstLine: 567 }, spacing: { line: 360, after: 120 } } },
    { id: "Caption", name: "Caption", basedOn: "Normal", next: "Normal",
      run: { font: FONT, size: SIZE, italics: true },
      paragraph: { alignment: AlignmentType.CENTER, spacing: { before: 40, after: 160 } } },
  ],
};

const pageProps = { size: { width: 11906, height: 16838 }, margin: { top: 1417, right: 1134, bottom: 1134, left: 1984 } };
/* Quy định đặt số trang ở GIỮA, PHÍA TRÊN đầu trang — nên nó nằm ở header chứ không phải footer. */
const soTrang = () => new Header({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: SIZE })] })] });

const doc = new Document({
  styles,
  features: { updateFields: true },
  sections: [
    // bìa — không số trang, có viền trang
    { properties: { page: { ...pageProps, borders: coverBorders }, titlePage: true }, children: coverElements() },
    // front matter — số La Mã
    { properties: { page: { ...pageProps, pageNumbers: { start: 1, formatType: NumberFormat.LOWER_ROMAN } } }, headers: { default: soTrang() },
      children: [
        ...fmElements, PB(),
        ...tocElements, PB(),
        /* Thứ tự lấy theo mẫu trang mục lục ở Phụ lục 01 mục II.7: danh mục ký hiệu và chữ
         * viết tắt trước, rồi danh mục bảng, rồi danh mục hình vẽ. */
        ...abbrElements(), PB(),
        ...listingParas("DANH MỤC BẢNG", tables, "Bảng"), PB(),
        ...listingParas("DANH MỤC HÌNH VẼ, ĐỒ THỊ", figures, "Hình"),
      ] },
    // thân bài — số thường, bắt đầu lại từ 1
    { properties: { page: { ...pageProps, pageNumbers: { start: 1, formatType: NumberFormat.DECIMAL } } }, headers: { default: soTrang() },
      children: bodyElements },
  ],
});

/**
 * Tên bản Word theo PHIÊN BẢN, không ghi đè bản cũ.
 *
 * Trước đây build luôn ghi vào `BaoCao-QuizAI-DATN.docx`, nên mỗi lần dựng lại là mất bản trước. Trong
 * giai đoạn trao đổi với giảng viên thì cần chỉ đích danh "bản nào", chứ không phải "bản mới nhất".
 *
 *   node build.js            -> bao-cao-datn-v{n+1}.docx  (tự tăng số)
 *   node build.js --final    -> bao-cao-datn-final.docx   (bản chốt để nộp)
 */
function tenBanRa() {
  if (process.argv.includes("--final")) return path.join(DIR, "bao-cao-datn-final.docx");

  const daCo = fs.readdirSync(DIR)
    .map((f) => /^bao-cao-datn-v(\d+)\.docx$/.exec(f))
    .filter(Boolean)
    .map((m) => Number(m[1]));

  const tiepTheo = daCo.length ? Math.max(...daCo) + 1 : 1;
  return path.join(DIR, `bao-cao-datn-v${tiepTheo}.docx`);
}

/**
 * Chỉ giữ {@link SO_BAN_GIU} bản đánh số gần nhất, xoá các bản cũ hơn.
 *
 * Mỗi bản nặng khoảng 7 MB vì nhúng toàn bộ ảnh, nên sau vài chục lần dựng thì thư mục vừa nặng vừa
 * khó nhìn ra đâu là bản đang dùng. Giữ 5 bản là đủ cho việc đối chiếu lùi vài bước khi trao đổi với
 * giảng viên.
 *
 * KHÔNG bao giờ đụng tới `bao-cao-datn-final.docx` (bản chốt để nộp) hay bất cứ tệp nào không theo
 * khuôn `bao-cao-datn-v{số}.docx` — mẫu tên ở đây cố ý hẹp vì đây là thao tác xoá.
 *
 * Bản đang mở trong Word sẽ xoá không được; bỏ qua và báo ra, không để lỗi đó làm hỏng cả lần dựng.
 */
const SO_BAN_GIU = 5;

function donBanCu(vuaTao) {
  const ban = fs.readdirSync(DIR)
    .map((f) => ({ f, m: /^bao-cao-datn-v(\d+)\.docx$/.exec(f) }))
    .filter((x) => x.m)
    .map((x) => ({ ten: x.f, so: Number(x.m[1]) }))
    .sort((a, b) => b.so - a.so);

  const boDi = ban.slice(SO_BAN_GIU);
  if (!boDi.length) return;

  const daXoa = [];
  const khongXoaDuoc = [];
  for (const b of boDi) {
    if (path.join(DIR, b.ten) === vuaTao) continue; // không tự xoá bản vừa dựng
    try {
      fs.unlinkSync(path.join(DIR, b.ten));
      daXoa.push("v" + b.so);
    } catch (e) {
      khongXoaDuoc.push(`v${b.so} (${e.code === "EBUSY" || e.code === "EPERM" ? "đang mở" : e.code})`);
    }
  }
  if (daXoa.length) console.log(`   dọn ${daXoa.length} bản cũ: ${daXoa.reverse().join(", ")} — giữ ${SO_BAN_GIU} bản gần nhất`);
  if (khongXoaDuoc.length) console.log(`   không xoá được: ${khongXoaDuoc.join(", ")}`);
}

Packer.toBuffer(doc).then((buf) => {
  const out = tenBanRa();
  // Không bắt EBUSY nữa: tên file luôn mới nên không đụng bản đang mở trong Word.
  fs.writeFileSync(out, buf);
  console.log("OK ->", out, "| bytes=", buf.length, "| figures=", figures.length, "| tables=", tables.length);
  donBanCu(out);
});
