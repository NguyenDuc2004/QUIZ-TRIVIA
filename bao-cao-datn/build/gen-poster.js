/* Sinh poster ĐATN khổ A0 dọc -> ../Poster-QuizAI.png (và .html để chỉnh tay nếu cần).
 *
 * Chạy:  cd bao-cao-datn/build && node gen-poster.js
 *
 * ## Vì sao dựng bằng HTML rồi chụp, không vẽ trong PowerPoint
 * Cùng lý do với 44 hình của báo cáo: nội dung nằm trong mã nguồn nên sửa một dòng chữ là dựng lại
 * được, và số liệu lấy đúng từ một chỗ. Poster in ra khổ lớn, sai một con số thì phải in lại cả tờ.
 *
 * ## Khổ giấy
 * A0 dọc: 841 × 1189 mm. Chụp ở 96 DPI cho ra 3179 × 4494 px, đủ nét khi in ở 150 DPI vì Chrome
 * dựng chữ theo vector rồi mới rasterise ở `deviceScaleFactor`. Đặt hệ số 2 thì tệp ra ~6358 px bề
 * ngang, tương đương 192 DPI ở khổ A0 — dư cho máy in poster.
 *
 * ## Mọi con số phải đo được
 * Giống slide bảo vệ: người xem poster đứng cạnh và hỏi lại từng con số. Không có số ước lượng nào ở
 * đây; tất cả truy về mục tương ứng của báo cáo.
 */
const fs = require("fs");
const path = require("path");

const ASSETS = path.join(__dirname, "..", "assets");
const OUT_PNG = path.join(__dirname, "..", "Poster-QuizAI.png");
const OUT_HTML = path.join(__dirname, "_poster.html");

/* A0 dọc ở 96 DPI */
const W = 3179;
const H = 4494;

const b64 = (ten) => {
  const p = path.join(ASSETS, `hinh-${ten}.png`);
  if (!fs.existsSync(p)) {
    console.warn(`  ! thiếu hinh-${ten}.png — ô ảnh sẽ trống`);
    return null;
  }
  return "data:image/png;base64," + fs.readFileSync(p).toString("base64");
};

const anhKhoi = (ten, chu) => {
  const d = b64(ten);
  return `<figure class="anh">
    ${d ? `<img src="${d}" alt="">` : '<div class="trong">chưa có ảnh</div>'}
    <figcaption>${chu}</figcaption>
  </figure>`;
};

const html = `<!doctype html>
<html lang="vi"><head><meta charset="utf-8">
<style>
  @font-face { font-family: 'Inter'; src: local('Segoe UI'); }
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    width: ${W}px; height: ${H}px;
    font-family: 'Segoe UI', Arial, sans-serif;
    color: #0f172a; background: #ffffff;
    display: flex; flex-direction: column;
  }

  /* ---- đầu poster ---- */
  header {
    background: linear-gradient(135deg, #6d28d9 0%, #8b5cf6 100%);
    color: #fff; padding: 52px 90px 46px; text-align: center;
  }
  .truong { font-size: 34px; letter-spacing: 2px; color: #ddd6fe; }
  .loai { font-size: 30px; letter-spacing: 8px; color: #ede9fe; margin-top: 18px; }
  h1 { font-size: 86px; line-height: 1.12; font-weight: 800; margin: 20px 0 24px; }
  .nguoi { font-size: 34px; color: #ede9fe; line-height: 1.7; }
  .nguoi b { color: #fff; }

  /* ---- thân ---- */
  main { flex: 1; padding: 40px 90px 0; display: flex; flex-direction: column; gap: 34px; }
  .hang { display: flex; gap: 34px; }
  .cot { flex: 1; display: flex; flex-direction: column; gap: 34px; }

  section { border: 3px solid #e2e8f0; border-radius: 28px; padding: 34px 44px; }
  section > h2 {
    font-size: 46px; font-weight: 700; color: #6d28d9;
    margin-bottom: 20px; display: flex; align-items: center; gap: 20px;
  }
  section > h2::before {
    content: ''; width: 14px; height: 46px; background: #7c3aed; border-radius: 8px; flex: none;
  }
  p, li { font-size: 31px; line-height: 1.55; color: #1e293b; }
  ul { padding-left: 40px; }
  li { margin-bottom: 14px; }
  .nho { font-size: 26px; color: #64748b; }

  /* ---- thẻ số liệu ---- */
  .so-luoi { display: grid; grid-template-columns: repeat(4, 1fr); gap: 26px; }
  .so {
    background: #f5f3ff; border: 3px solid #ddd6fe; border-radius: 24px;
    padding: 30px 18px; text-align: center;
  }
  .so .v { font-size: 66px; font-weight: 800; color: #6d28d9; line-height: 1.1; }
  .so .n { font-size: 27px; color: #0f172a; margin-top: 10px; font-weight: 600; }
  .so .p { font-size: 23px; color: #64748b; margin-top: 6px; }

  /* ---- trụ cột ---- */
  .tru { display: grid; grid-template-columns: 1fr 1fr; gap: 26px; }
  .tru > div { background: #faf9ff; border: 3px solid #e9e5ff; border-radius: 22px; padding: 28px 30px; }
  .tru h3 { font-size: 34px; color: #6d28d9; margin-bottom: 12px; }
  .tru p { font-size: 28px; color: #475569; line-height: 1.5; }

  /* ---- ảnh ---- */
  .anh { border: 3px solid #e2e8f0; border-radius: 20px; overflow: hidden; background: #fff; }
  /* Chiều cao ô ảnh CỐ ĐỊNH. Khổ A0 là khung cứng, mà ảnh ghép (phòng đấu) rất cao — để ảnh tự
     giãn thì cả poster tràn 1323px và ba mục cuối bị cắt mất mà không có gì báo. object-fit contain giữ
     nguyên tỉ lệ, chừa nền trắng hai bên nếu ảnh không vừa khung. */
  .anh img { width: 100%; height: 800px; object-fit: contain; object-position: top; display: block; background: #fff; }
  .anh .trong { height: 800px; display: flex; align-items: center; justify-content: center; color: #94a3b8; font-size: 30px; }
  .anh figcaption { font-size: 25px; color: #64748b; padding: 16px 22px; background: #f8fafc; }

  /* ---- công nghệ ---- */
  .cn { display: flex; flex-wrap: wrap; gap: 16px; }
  .cn span {
    font-size: 27px; background: #f1f5f9; border: 2px solid #e2e8f0;
    border-radius: 999px; padding: 12px 26px; color: #334155;
  }

  footer {
    margin-top: 34px; background: #0f172a; color: #cbd5e1;
    padding: 34px 90px; font-size: 27px; display: flex; justify-content: space-between;
  }
</style></head>
<body>

<header>
  <div class="truong">TRƯỜNG ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI — KHOA CÔNG NGHỆ THÔNG TIN</div>
  <div class="loai">ĐỒ ÁN TỐT NGHIỆP</div>
  <h1>XÂY DỰNG ỨNG DỤNG QUIZ/TRIVIA<br>TÍCH HỢP TRÍ TUỆ NHÂN TẠO</h1>
  <div class="nguoi">
    Sinh viên thực hiện: <b>Nguyễn Khắc Minh Đức</b> &nbsp;·&nbsp; MSV: 2022601585<br>
    Giảng viên hướng dẫn: <b>ThS. Nguyễn Đức Lưu</b> &nbsp;·&nbsp; Hà Nội, 09/2026
  </div>
</header>

<main>

  <section>
    <h2>Bài toán</h2>
    <p>Các nền tảng quiz trực tuyến hiện có mạnh ở phần tổ chức trò chơi, nhưng để lại ba khoảng trống:
    giáo viên phải <b>soạn từng câu bằng tay</b> dù học liệu của môn đã có sẵn; hệ thống <b>không chấm
    được câu tự luận</b>; và phần gợi ý dựa trên lượt xem chứ <b>không dựa trên năng lực</b> của người
    học. Đồ án xây dựng một hệ thống lấp ba khoảng trống đó, đồng thời đo và báo cáo bằng số liệu thật.</p>
  </section>

  <section>
    <h2>Bốn trọng tâm</h2>
    <div class="tru">
      <div><h3>Phòng đấu thời gian thực</h3><p>Nhiều người chơi cùng lúc qua STOMP trên WebSocket, phát tán sự kiện qua Redis Pub/Sub, tính điểm theo tốc độ trả lời.</p></div>
      <div><h3>Sinh đề và trợ lý bằng RAG</h3><p>Sinh câu hỏi từ chính học liệu người dùng nạp lên; trợ lý trả lời kèm trích dẫn đoạn tài liệu đã dựa vào.</p></div>
      <div><h3>Gợi ý cá nhân hoá bằng Neo4j</h3><p>Hành vi làm bài đồng bộ sang đồ thị để gợi ý quiz theo chủ đề còn yếu và đề xuất thứ tự ôn tập.</p></div>
      <div><h3>Đo hiệu năng và độ chính xác AI</h3><p>Không dừng ở chạy được: đo độ trễ phòng đấu theo mức tải và đối chiếu điểm AI chấm với đáp án theo tiêu chí.</p></div>
    </div>
  </section>

  <div class="hang">
    <div class="cot">
      ${anhKhoi("1.1", "Hình 1. Kiến trúc tổng thể — ba kênh giao tiếp, ba cơ sở dữ liệu, hai nhà cung cấp mô hình")}
      ${anhKhoi("3.8", "Hình 3. Trợ lý học tập trả lời kèm trích dẫn nguồn từ học liệu")}
    </div>
    <div class="cot">
      ${anhKhoi("3.6", "Hình 2. Phòng đấu — mã PIN, mã QR và bảng xếp hạng trực tiếp")}
      ${anhKhoi("3.17", "Hình 4. Độ trễ phát câu hỏi theo số người chơi trong phòng")}
    </div>
  </div>

  <section>
    <h2>Kết quả đo được</h2>
    <div class="so-luoi">
      <div class="so"><div class="v">216 ms</div><div class="n">P95 phát câu hỏi</div><div class="p">ở 100 người/phòng</div></div>
      <div class="so"><div class="v">0</div><div class="n">sự kiện mất</div><div class="p">tới 200 người</div></div>
      <div class="so"><div class="v">0,13</div><div class="n">sai lệch điểm AI chấm</div><div class="p">trên thang 10</div></div>
      <div class="so"><div class="v">10/10</div><div class="n">câu sinh đúng cấu trúc</div><div class="p">qua bộ kiểm JSON</div></div>
      <div class="so"><div class="v">16</div><div class="n">nhóm chức năng</div><div class="p">87 yêu cầu</div></div>
      <div class="so"><div class="v">606</div><div class="n">phép kiểm máy chủ</div><div class="p">0 hỏng</div></div>
      <div class="so"><div class="v">128</div><div class="n">phép kiểm giao diện</div><div class="p">0 hỏng</div></div>
      <div class="so"><div class="v">2/2</div><div class="n">tấn công bị chặn</div><div class="p">tiêm chỉ thị</div></div>
    </div>
    <p class="nho" style="margin-top:24px">Số liệu hiệu năng đo ngày 08/08/2026, độ chính xác AI đo ngày 14/08/2026, trên một máy đơn — không bao gồm độ trễ mạng thật.</p>
  </section>

  <section>
    <h2>Công nghệ sử dụng</h2>
    <div class="cn">
      <span>Java 21</span><span>Spring Boot 3.5</span><span>Spring Security</span><span>WebSocket · STOMP</span>
      <span>React 19</span><span>TypeScript</span><span>Vite 8</span><span>Ant Design v6</span><span>Tailwind CSS v4</span>
      <span>PostgreSQL 16 + pgvector</span><span>Neo4j 5</span><span>Redis 7</span>
      <span>Google Gemini</span><span>Groq (dự phòng)</span><span>Apache Tika</span><span>Flyway</span><span>Docker Compose</span>
    </div>
  </section>

</main>

<footer>
  <div>Quiz/Trivia tích hợp trí tuệ nhân tạo</div>
  <div>Nguyễn Khắc Minh Đức · 2022601585 · GVHD: ThS. Nguyễn Đức Lưu</div>
</footer>

</body></html>`;

(async () => {
  fs.writeFileSync(OUT_HTML, html, "utf8");

  const puppeteer = require("puppeteer");
  const browser = await puppeteer.launch({
    executablePath: "C:/Program Files/Google/Chrome/Application/chrome.exe",
    headless: "new",
    args: ["--no-sandbox"],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: W, height: H, deviceScaleFactor: 2 });
  await page.goto("file://" + OUT_HTML.replace(/\\/g, "/"), { waitUntil: "networkidle0" });
  await page.screenshot({ path: OUT_PNG });
  await browser.close();

  const { size } = fs.statSync(OUT_PNG);
  console.log(`OK -> ${path.basename(OUT_PNG)}  ${(size / 1024 / 1024).toFixed(1)} MB  ${W * 2}×${H * 2} px (A0 dọc, ~192 DPI)`);
  console.log(`     bản HTML để chỉnh tay: ${path.relative(process.cwd(), OUT_HTML)}`);
})().catch((e) => {
  console.error("FAIL:", e.message);
  process.exit(1);
});
