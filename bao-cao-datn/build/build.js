/* Dựng báo cáo ĐATN Quiz/Trivia AI ra .docx theo định dạng HaUI.
 * Parse Markdown (marked.lexer) -> docx-js. Tự sinh mục lục + danh mục hình + danh mục bảng.
 */
const fs = require("fs");
const path = require("path");
const { marked } = require("marked");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, HeadingLevel, TableOfContents, PageNumber, Footer,
  BorderStyle, WidthType, ShadingType, PageBreak, NumberFormat, LevelFormat, ImageRun,
  PageBorderDisplay, PageBorderOffsetFrom,
} = require("docx");

const DIR = path.join(__dirname, "..");
const ASSETS = path.join(DIR, "assets");
const FONT = "Times New Roman";

function pngSize(buf) { return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) }; }
const MONO = "Consolas";
const SIZE = 26;            // 13pt
const CONTENT_WIDTH = 9071; // A4 (11906) - left 1701 - right 1134

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
    const maxW = 600, maxH = 820; // px — vừa khổ A4 (lề 3-2-2-2)
    const scale = Math.min(maxW / w, maxH / h, 1);
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
      if (tok.tokens && tok.tokens.length === 1 && tok.tokens[0].type === "strong" && /^Bảng\s+(\d+(?:\.\d+)*)\.?\s*/.test(tok.tokens[0].text)) {
        const m = tok.tokens[0].text.match(/^Bảng\s+(\d+(?:\.\d+)*)\.?\s*([\s\S]+)$/);
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
function coverElements() {
  return [
    center("", { after: 320 }),
    center("BỘ CÔNG THƯƠNG", { bold: true, size: 24, after: 40 }),
    center("ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI", { bold: true, size: 28, after: 40 }),
    center("TRƯỜNG CÔNG NGHỆ THÔNG TIN VÀ TRUYỀN THÔNG", { bold: true, size: 24, after: 80 }),
    center("──────────────────", { after: 760 }),
    center("ĐỒ ÁN TỐT NGHIỆP ĐẠI HỌC", { bold: true, size: 32, after: 80 }),
    center("NGÀNH: KỸ THUẬT PHẦN MỀM", { bold: true, size: 26, after: 720 }),
    center("XÂY DỰNG ỨNG DỤNG QUIZ/TRIVIA", { bold: true, size: 28, after: 60 }),
    center("TÍCH HỢP TRÍ TUỆ NHÂN TẠO", { bold: true, size: 28, after: 760 }),
    center("Giảng viên hướng dẫn:  ThS. Nguyễn Đức Lưu", { bold: true, size: 26, after: 80 }),
    center("Sinh viên thực hiện:  Nguyễn Khắc Minh Đức", { bold: true, size: 26, after: 80 }),
    center("Mã số sinh viên:  2022601585", { bold: true, size: 26, after: 80 }),
    center("Lớp:  2022DHKTPM01 — Khóa: K17", { bold: true, size: 26, after: 80 }),
    center("Ngành:  Kỹ thuật phần mềm", { bold: true, size: 26, after: 800 }),
    center("HÀ NỘI - 2026", { bold: true, size: 26 }),
  ];
}

// viền trang bìa
const coverBorder = () => ({ style: BorderStyle.SINGLE, size: 18, color: "1F3864", space: 24 });
const coverBorders = {
  pageBorders: { display: PageBorderDisplay.ALL_PAGES, offsetFrom: PageBorderOffsetFrom.PAGE },
  pageBorderTop: coverBorder(),
  pageBorderRight: coverBorder(),
  pageBorderBottom: coverBorder(),
  pageBorderLeft: coverBorder(),
};

// ---------- danh mục ----------
function listingParas(title, items, prefix) {
  const out = [new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: title, font: FONT, size: 28, bold: true, allCaps: true })] })];
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
  const out = [new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: "DANH MỤC CÁC THUẬT NGỮ, KÝ HIỆU VÀ TỪ VIẾT TẮT", font: FONT, size: 28, bold: true, allCaps: true })] })];
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
// Chương 3 và Kết luận chưa viết (chờ số liệu đo mục 3.5 và 3.6) — bỏ khỏi danh sách thay vì
// tạo file rỗng, để bản Word không có mục trống trong mục lục.
const bodyFiles = [
  "00-mo-dau.md",
  "01-chuong-1.md",
  "02-chuong-2.md",
  "05-tai-lieu-tham-khao.md",
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
  new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun({ text: "MỤC LỤC", font: FONT, size: 28, bold: true, allCaps: true })] }),
  new TableOfContents("Mục lục", { hyperlink: true, headingStyleRange: "1-3" }),
];

const PB = () => new Paragraph({ children: [new PageBreak()] });

// ---------- styles ----------
const styles = {
  default: { document: { run: { font: FONT, size: SIZE }, paragraph: { spacing: { line: 360, after: 120 } } } },
  paragraphStyles: [
    { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
      run: { font: FONT, size: 28, bold: true, allCaps: true },
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

const pageProps = { size: { width: 11906, height: 16838 }, margin: { top: 1134, right: 1134, bottom: 1134, left: 1701 } };
const footer = () => new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 24 })] })] });

const doc = new Document({
  styles,
  features: { updateFields: true },
  sections: [
    // bìa — không số trang, có viền trang
    { properties: { page: { ...pageProps, borders: coverBorders }, titlePage: true }, children: coverElements() },
    // front matter — số La Mã
    { properties: { page: { ...pageProps, pageNumbers: { start: 1, formatType: NumberFormat.LOWER_ROMAN } } }, footers: { default: footer() },
      children: [
        ...fmElements, PB(),
        ...tocElements, PB(),
        ...listingParas("DANH MỤC HÌNH ẢNH", figures, "Hình"), PB(),
        ...listingParas("DANH MỤC BẢNG", tables, "Bảng"), PB(),
        ...abbrElements(),
      ] },
    // thân bài — số thường, bắt đầu lại từ 1
    { properties: { page: { ...pageProps, pageNumbers: { start: 1, formatType: NumberFormat.DECIMAL } } }, footers: { default: footer() },
      children: bodyElements },
  ],
});

Packer.toBuffer(doc).then((buf) => {
  let out = path.join(DIR, "BaoCao-QuizAI-DATN.docx");
  try {
    fs.writeFileSync(out, buf);
  } catch (e) {
    if (e.code === "EBUSY" || e.code === "EPERM") {
      out = path.join(DIR, "BaoCao-QuizAI-DATN-new.docx");
      fs.writeFileSync(out, buf);
      console.log("(File chính đang mở/khóa — đã ghi sang bản mới)");
    } else throw e;
  }
  console.log("OK ->", out, "| bytes=", buf.length, "| figures=", figures.length, "| tables=", tables.length);
});
