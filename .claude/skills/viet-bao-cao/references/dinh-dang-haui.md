# Định dạng trình bày ĐATN HaUI + preset docx-js

> Các giá trị dưới đây là quy cách trình bày ĐATN Trường ĐH Công nghiệp Hà Nội. Nếu user có quy định khác của khoa → ưu tiên quy định đó.
> Dàn ý nội dung nằm ở `cau-truc-bao-cao.md` (khung 3 chương).

## Quy cách trình bày

| Yếu tố | Quy định | Giá trị docx-js |
|--------|----------|-----------------|
| Khổ giấy | A4 đứng | width 11906, height 16838 (DXA) |
| Lề trái | 3 cm | margin.left = 1701 |
| Lề phải | 2 cm | margin.right = 1134 |
| Lề trên | 2 cm | margin.top = 1134 |
| Lề dưới | 2 cm | margin.bottom = 1134 |
| Font chữ | Times New Roman | font: "Times New Roman" |
| Cỡ chữ thân bài | 13 pt | size: 26 (nửa point) |
| Giãn dòng | 1.5 lines | spacing.line = 360, lineRule "auto" |
| Giãn đoạn | 6 pt sau đoạn | spacing.after = 120 |
| Thụt đầu dòng | 1 cm | indent.firstLine = 567 |
| Căn lề thân bài | Đều 2 bên (justify) | AlignmentType.JUSTIFIED |
| Tên chương (H1) | 14pt, IN HOA, đậm, căn giữa | size 28, bold, allCaps, center |
| Mục cấp 2 (H2: 1.1) | 13pt, đậm | size 26, bold |
| Mục cấp 3 (H3: 1.1.1) | 13pt, đậm nghiêng | size 26, bold, italics |
| Caption hình | "Hình x.y. Mô tả" — 13pt nghiêng, căn giữa, DƯỚI hình | size 26, italics, center |
| Caption bảng | "Bảng x.y. Mô tả" — 13pt đậm, căn giữa, TRÊN bảng | size 26, bold, center |
| Số trang | Footer căn giữa; front matter i,ii,iii; thân bài 1,2,3 | PageNumber.CURRENT |
| Đánh số mục | Chương 1 → 1.1 → 1.1.1 (tối đa 3-4 cấp) | numbering hoặc text thủ công |

## Preset docx-js (sao chép & điều chỉnh)

```javascript
const { Document, Packer, Paragraph, TextRun, AlignmentType, HeadingLevel,
        TableOfContents, PageNumber, Header, Footer, LevelFormat } = require("docx");
const fs = require("fs");

const FONT = "Times New Roman";

const doc = new Document({
  styles: {
    default: {
      document: { run: { font: FONT, size: 26 },                 // 13pt
        paragraph: { spacing: { line: 360, after: 120 } } },     // 1.5 line, 6pt after
    },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 28, bold: true, allCaps: true },           // 14pt IN HOA đậm
        paragraph: { alignment: AlignmentType.CENTER, outlineLevel: 0,
          spacing: { before: 360, after: 240 } } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 26, bold: true },                          // 13pt đậm
        paragraph: { outlineLevel: 1, spacing: { before: 240, after: 120 } } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 26, bold: true, italics: true },           // 13pt đậm nghiêng
        paragraph: { outlineLevel: 2, spacing: { before: 180, after: 120 } } },
      { id: "BodyVN", name: "Body VN", basedOn: "Normal", next: "BodyVN",
        run: { font: FONT, size: 26 },
        paragraph: { alignment: AlignmentType.JUSTIFIED,
          indent: { firstLine: 567 }, spacing: { line: 360, after: 120 } } },
      { id: "Caption", name: "Caption", basedOn: "Normal", next: "Normal",
        run: { font: FONT, size: 26, italics: true },
        paragraph: { alignment: AlignmentType.CENTER, spacing: { before: 60, after: 180 } } },
    ],
  },
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },                              // A4
        margin: { top: 1134, right: 1134, bottom: 1134, left: 1701 },       // 2-2-2-3 cm
      },
    },
    footers: {
      default: new Footer({ children: [ new Paragraph({
        alignment: AlignmentType.CENTER,
        children: [ new TextRun({ children: [PageNumber.CURRENT], font: FONT, size: 24 }) ],
      }) ] }),
    },
    children: [
      // Mục lục:
      new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun("MỤC LỤC")] }),
      new TableOfContents("Mục lục", { hyperlink: true, headingStyleRange: "1-3" }),

      // Tên chương:
      new Paragraph({ heading: HeadingLevel.HEADING_1,
        children: [new TextRun("CHƯƠNG 1. TỔNG QUAN VỀ ỨNG DỤNG QUIZ/TRIVIA TÍCH HỢP AI")] }),
      // Mục con:
      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("1.1. Hệ thống Quiz/Trivia trực tuyến")] }),
      // Đoạn văn thân bài:
      new Paragraph({ style: "BodyVN", children: [new TextRun("Nội dung đoạn...")] }),
      // Caption hình (đặt ngay dưới ImageRun/placeholder):
      new Paragraph({ style: "Caption", children: [new TextRun("Hình 1.1. Sơ đồ kiến trúc tổng thể hệ thống")] }),
    ],
  }],
});

Packer.toBuffer(doc).then(b => fs.writeFileSync("docs/bao-cao/bao-cao-datn.docx", b));
```

## Cách chạy script tạo .docx (QUAN TRỌNG trên Windows)

`docx` được cài global (`npm install -g docx`) nên **không tự nằm trên `require` path** → chạy `node script.js` trực tiếp sẽ lỗi `Cannot find module 'docx'`. Phải trỏ `NODE_PATH` vào global node_modules:

```bash
# Bash tool (git bash)
NODE_PATH="$(npm root -g)" node build-bao-cao.js
```

```powershell
# PowerShell
$env:NODE_PATH = (npm root -g); node build-bao-cao.js
```

Phương án thay thế (ổn định hơn cho lâu dài): tạo thư mục build riêng và cài local — `cd build-report && npm init -y && npm install docx`, rồi `node build-bao-cao.js` chạy bình thường. Khi đó KHÔNG cần đặt `NODE_PATH`.

## Lưu ý quan trọng

- **TOC chỉ cập nhật số trang khi mở file trong Word** rồi bấm "Update Field" (hoặc Ctrl+A → F9). Báo user thao tác này; docx-js không tính sẵn số trang.
- **Tên chương phải dùng `HeadingLevel.HEADING_1`** (không gắn style tùy biến lên đoạn heading) để TOC nhận diện được.
- **Đánh số mục thủ công trong text** ("1.1.", "1.1.1.") cho chắc chắn, dễ kiểm soát hơn auto-numbering của Word khi xuất từ docx-js.
- **Front matter dùng số trang La Mã (i, ii, iii)**: tạo section riêng cho front matter với `pageNumberFormat`, hoặc xử lý sau khi mở Word. Nếu phức tạp → để mặc định và hướng dẫn user chỉnh trong Word.
- **Hình/sơ đồ**: nếu chưa có ảnh thật, chèn 1 đoạn placeholder `[HÌNH x.y: ... — cần chèn]` + caption, để user thay bằng ảnh export từ draw.io/figma/screenshot sau.
- **Đổi font sang 13pt = `size: 26`** (docx dùng nửa point). Đừng nhầm với point thật.
- Caption nghiêng cỡ 13 (`size 26`); một số khoa dùng 12pt cho caption → đổi `size: 24` nếu cần.
