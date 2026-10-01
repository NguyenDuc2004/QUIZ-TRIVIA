# Định dạng trình bày ĐATN HaUI + preset docx-js

> Các giá trị dưới đây lấy từ **`docs/2025-phu-luc-quy-dinh-lam-da-kltn_SV.docx` — Phụ lục 01, mục II**
> (quy cách chung đối với ĐA, KLTN của Trường ĐH Công nghiệp Hà Nội, bản 2025). Đây là văn bản gốc;
> nếu khoa có quy định riêng mới hơn thì ưu tiên bản đó.
>
> **Bản trước của tệp này ghi 13pt và lề 2-2-2-3, cả hai đều sai** so với văn bản trên, và báo cáo đã
> dựng theo giá trị sai suốt nhiều phiên bản trước khi đối chiếu lại. Đừng sửa các con số dưới đây
> nếu không mở văn bản gốc ra đọc.
> Dàn ý nội dung nằm ở `cau-truc-bao-cao.md` (khung 3 chương).

## Quy cách trình bày

| Yếu tố | Quy định | Giá trị docx-js |
|--------|----------|-----------------|
| Khổ giấy | A4 đứng | width 11906, height 16838 (DXA) |
| Lề trái | **3,5 cm** | margin.left = 1984 |
| Lề phải | 2 cm | margin.right = 1134 |
| Lề trên | **2,5 cm** | margin.top = 1417 |
| Lề dưới | 2 cm | margin.bottom = 1134 |
| Font chữ | Times New Roman | font: "Times New Roman" |
| Cỡ chữ thân bài | **14 pt** | size: 28 (nửa point) |
| Giãn dòng | 1.5 lines | spacing.line = 360, lineRule "auto" |
| Giãn đoạn | 6 pt sau đoạn | spacing.after = 120 |
| Thụt đầu dòng | 1 cm | indent.firstLine = 567 |
| Căn lề thân bài | Đều 2 bên (justify) | AlignmentType.JUSTIFIED |
| Tên chương (H1) | 16pt, IN HOA, đậm, căn giữa | size 32, bold, allCaps, center |
| Mục cấp 2 (H2: 1.1) | 14pt, đậm | size 28, bold |
| Mục cấp 3 (H3: 1.1.1) | 14pt, đậm nghiêng | size 28, bold, italics |
| Caption hình | "Hình x.y. Mô tả" — 14pt nghiêng, căn giữa, DƯỚI hình | size 28, italics, center |
| Caption bảng | "Bảng x.y. Mô tả" — 14pt đậm, căn giữa, TRÊN bảng | size 28, bold, center |
| Số trang | **HEADER căn giữa** (quy định: "ở giữa, phía trên đầu mỗi trang"); front matter i,ii,iii; thân bài 1,2,3 | PageNumber.CURRENT |
| Đánh số mục | Chương 1 → 1.1 → 1.1.1 (tối đa 3-4 cấp) | numbering hoặc text thủ công |

## Những điều dễ bỏ sót (đều nằm trong Phụ lục 01)

- **Độ dày 30–60 trang, KHÔNG kể phụ lục** (mục II.1). Phụ lục không được dày hơn phần chính (mục II.6).
- **Thứ tự front matter** theo mẫu mục lục ở mục II.7: Danh mục ký hiệu và chữ viết tắt → Danh mục bảng
  → Danh mục hình vẽ, đồ thị → MỞ ĐẦU. Nên gói mục lục gọn trong một trang.
- **Mỗi nhóm tiểu mục phải có ít nhất hai tiểu mục** (mục II.2) — không được có 2.1.1 mà thiếu 2.1.2.
  Đánh số nhiều nhất bốn chữ số.
- **Đầu đề bảng đặt TRÊN bảng, đầu đề hình đặt DƯỚI hình** (mục II.3).
- Khi nhắc tới hình hay bảng phải **nêu rõ số hiệu** — cấm viết "bảng dưới đây".
- **Tài liệu tham khảo xếp riêng theo từng ngôn ngữ**, trong mỗi nhóm xếp ABC: người nước ngoài theo họ,
  người Việt theo tên mà vẫn giữ nguyên thứ tự họ tên (mục II.8).

## Preset docx-js (sao chép & điều chỉnh)

```javascript
const { Document, Packer, Paragraph, TextRun, AlignmentType, HeadingLevel,
        TableOfContents, PageNumber, Header, Footer, LevelFormat } = require("docx");
const fs = require("fs");

const FONT = "Times New Roman";

const doc = new Document({
  styles: {
    default: {
      document: { run: { font: FONT, size: 28 },                 // 14pt
        paragraph: { spacing: { line: 360, after: 120 } } },     // 1.5 line, 6pt after
    },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 32, bold: true, allCaps: true },           // 16pt IN HOA đậm
        paragraph: { alignment: AlignmentType.CENTER, outlineLevel: 0,
          spacing: { before: 360, after: 240 } } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 28, bold: true },                          // 14pt đậm
        paragraph: { outlineLevel: 1, spacing: { before: 240, after: 120 } } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { font: FONT, size: 28, bold: true, italics: true },           // 14pt đậm nghiêng
        paragraph: { outlineLevel: 2, spacing: { before: 180, after: 120 } } },
      { id: "BodyVN", name: "Body VN", basedOn: "Normal", next: "BodyVN",
        run: { font: FONT, size: 28 },
        paragraph: { alignment: AlignmentType.JUSTIFIED,
          indent: { firstLine: 567 }, spacing: { line: 360, after: 120 } } },
      { id: "Caption", name: "Caption", basedOn: "Normal", next: "Normal",
        run: { font: FONT, size: 28, italics: true },
        paragraph: { alignment: AlignmentType.CENTER, spacing: { before: 60, after: 180 } } },
    ],
  },
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },                              // A4
        margin: { top: 1417, right: 1134, bottom: 1134, left: 1984 },       // trên 2,5 · dưới 2 · trái 3,5 · phải 2 cm
      },
    },
    headers: {
      default: new Header({ children: [ new Paragraph({
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
