# Bộ báo cáo ĐATN — Quiz/Trivia tích hợp AI

Nội dung báo cáo viết bằng Markdown, hình sinh từ định nghĩa dạng text, bản Word dựng bằng script.
Nhờ vậy sửa nội dung là sửa file `.md` rồi build lại, không phải chỉnh tay trong Word.

## Cấu trúc

| Đường dẫn | Nội dung |
|---|---|
| `front-matter.md` | Lời cảm ơn, lời cam đoan |
| `00-mo-dau.md` | Mở đầu (5 mục) |
| `01-chuong-1.md` | Chương 1 — Tổng quan về đề tài |
| `02-chuong-2.md` | Chương 2 — Phân tích và thiết kế hệ thống |
| `05-tai-lieu-tham-khao.md` | Tài liệu tham khảo `[1]`–`[15]` |
| `assets/` | 39 hình PNG đã sinh (`hinh-1.1.png` … `hinh-2.37.png`) |
| `build/` | Script sinh hình và dựng file Word |

| `03-chuong-3.md` | Chương 3 — Thực nghiệm và đánh giá (số liệu 3.5 và 3.6 đã đo thật) |

| `04-ket-luan.md` | Kết luận — kết quả đạt được, hạn chế, bài học, hướng phát triển |

Bộ nội dung đã **đủ**. `build.js` vẫn tự bỏ qua file chưa tồn tại nên thêm/bớt phần đều không làm đổ build.

**17 hình của Chương 3 (3.1–3.17) chưa có ảnh** — chúng là ảnh chụp màn hình sản phẩm và một biểu đồ,
không sinh được từ định nghĩa text. `build.js` chèn khung xám thay chỗ. Script `capture/capture.mjs`
hiện là bản của **đồ án khác** (nói về Khoa, Môn học, gv.demo) nên chưa dùng lại được.

## Dựng lại bản Word

```bash
cd build
npm install                # lần đầu: cài mermaid-cli, docx, sharp…
node build.js              # -> ../bao-cao-datn-v{n}.docx  (tự tăng số, KHÔNG ghi đè bản cũ)
node build.js --final      # -> ../bao-cao-datn-final.docx (bản chốt để nộp)
```

Bản Word đánh số phiên bản để trong lúc trao đổi với giảng viên còn chỉ đích danh được "bản nào",
thay vì "bản mới nhất". Bản cũ không bị ghi đè, nên build lại lúc đang mở file trong Word cũng không sao.

Mở file trong Word rồi nhấn `Ctrl+A` → `F9` → *Update entire table*. Mục lục và danh mục hình/bảng là
field của Word nên số trang chỉ điền sau bước này.

Định dạng theo chuẩn HaUI: Times New Roman 13pt, giãn dòng 1,5, lề trái 3cm / phải 2cm / trên–dưới 2cm,
số trang front matter i, ii, iii… rồi thân bài đếm lại từ 1.

## Sinh lại hình

Hình **không** vẽ bằng tay — mỗi hình là một định nghĩa dạng text trong script, nên sửa nội dung hình là
sửa vài dòng chữ rồi chạy lại.

```bash
cd build
node gen-diagrams.js       # Mermaid  -> 1.1, 1.2, 2.28 (ERD), 2.29 (mô-đun)
node gen-plantuml.js       # PlantUML -> 2.1–2.27 (use case, sequence, VOPC)
node gen-mockup.js         # HTML     -> 2.30–2.37 (wireframe, chụp bằng Chrome)
```

Yêu cầu ngoài `npm install`:

- **`plantuml.jar`** đặt trong `build/` và **Java** trong PATH — dùng cho `gen-plantuml.js`.
  Tệp `.jar` bị `.gitignore` bỏ qua nên phải tải lại: https://plantuml.com/download
- **Google Chrome** ở đường dẫn mặc định — `gen-mockup.js` dùng nó để chụp `mockup.html`.

Đặt tên hình theo đúng số trong báo cáo (`hinh-<số>.png`). `build.js` khớp placeholder
`[HÌNH x.y: … — cần chèn]` trong `.md` với file ảnh cùng số; thiếu ảnh thì nó chèn một khung xám thay
chỗ chứ không làm build đổ.

## Quy ước khi viết nội dung

- Bảng: dòng `**Bảng x.y. Tiêu đề**` đặt **ngay trên** bảng — `build.js` bắt dòng này để dựng danh mục bảng.
- Hình: dòng `[HÌNH x.y: mô tả — cần chèn]`, rồi caption `*Hình x.y. Tiêu đề*`.
- Trích dẫn `[n]` phải có mục tương ứng trong `05-tai-lieu-tham-khao.md`.
- **Không điền số liệu chưa đo.** Chỗ chờ số liệu để `«…»` kèm ghi chú, để không ai nhầm là đã đo.
