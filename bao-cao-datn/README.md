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
| `assets/` | 41 hình PNG đã sinh (`hinh-1.1.png` … `hinh-2.37.png`, `hinh-3.1.png`, `hinh-3.17.png`) |
| `build/` | Script sinh hình và dựng file Word |

| `03-chuong-3.md` | Chương 3 — Thực nghiệm và đánh giá (số liệu 3.5 và 3.6 đã đo thật) |

| `04-ket-luan.md` | Kết luận — kết quả đạt được, hạn chế, bài học, hướng phát triển |

Bộ nội dung đã **đủ**. `build.js` vẫn tự bỏ qua file chưa tồn tại nên thêm/bớt phần đều không làm đổ build.

**Còn thiếu 15 ảnh chụp màn hình của Chương 3 — `hinh-3.2` … `hinh-3.16`.** Đó là ảnh sản phẩm đang
chạy, không sinh được từ định nghĩa text; `build.js` chèn khung xám thay chỗ. Script `capture/capture.mjs`
hiện là bản của **đồ án khác** (nói về Khoa, Môn học, gv.demo) nên chưa dùng lại được.

Hai hình còn lại của chương này **không** phải ảnh chụp và đã sinh xong:

| Hình | Sinh bằng | Vì sao không phải ảnh chụp |
|---|---|---|
| `3.1` sơ đồ triển khai | `gen-diagrams.js` | Là sơ đồ, cùng loại với 1.1 và 2.29 |
| `3.17` biểu đồ độ trễ | `gen-chart.js` | Vẽ từ bảng số liệu mục 3.5, sửa số là sinh lại |

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
node gen-chart.js          # SVG      -> 3.17 (biểu đồ độ trễ P50/P95)
```

## Bộ tài liệu kiểm thử

```bash
cd build
node gen-testdocs.js       # -> ../Testcase+TestPlan/Test_Plan_QuizAI.docx + Test_Case_QuizAI.xlsx
```

Hai tệp này là **sản phẩm bàn giao** kèm đồ án, được mục 3.4 của báo cáo dẫn tới. Số liệu trong đó lấy
từ Bảng 3.2 và Bảng 3.4 của báo cáo cùng lượt chạy `./mvnw test` / `npm test` — sửa một nơi thì phải
sửa cả nơi kia, nếu không hai tài liệu sẽ nói khác nhau.

`gen-chart.js` giữ số liệu ngay trong tệp, lấy từ `docs/bao-cao/so-lieu-3.5-hieu-nang-realtime.md`.
**Sửa số ở đó thì phải sửa cả bảng trong `03-chuong-3.md`** — hai chỗ phải khớp. Đầu tệp ghi rõ ba
quyết định về cách vẽ (trục tung tuyến tính chứ không log, trục hoành theo giá trị thật chứ không chia
đều, phân biệt hai đường bằng nét chứ không bằng màu) để người sau không "sửa cho đẹp" rồi làm sai nghĩa.

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
