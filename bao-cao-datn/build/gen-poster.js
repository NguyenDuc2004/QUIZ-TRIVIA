/* Sinh poster ĐATN khổ A0 dọc -> ../Poster-QuizAI.png (và _poster.html để chỉnh tay nếu cần).
 *
 * Chạy:  cd bao-cao-datn/build && node gen-poster.js
 *
 * ## Bố cục
 * Theo mẫu poster của khoa: dải đầu trang hai khối (trường bên trái, thông tin đồ án bên phải),
 * dải tên đề tài, rồi sáu mục đánh số. Hàng A ba cột (đặt vấn đề · kiến trúc · công nghệ),
 * hàng B hai cột (luồng xử lý · giao diện), hàng C một dải kết quả.
 *
 * ## A0 là khung CỨNG
 * A0 dọc 841 × 1189 mm, chụp ở 96 DPI ra 3179 × 4494 px; deviceScaleFactor 2 cho ảnh 6358 × 8988 px,
 * tương đương 192 DPI khi in — dư cho máy in poster.
 *
 * Nội dung tràn khỏi khung thì Chrome CẮT MẤT MÀ KHÔNG BÁO GÌ. Một lần trước poster tràn 1323 px và
 * ba mục cuối biến mất, chỉ phát hiện khi mở ảnh ra xem. Vì vậy bản này đo chiều cao thật của trang
 * sau khi dựng và DỪNG nếu vượt khung, thay vì xuất ra một tấm ảnh cụt.
 *
 * ## Cấm dấu huyền sắc ngược trong chuỗi
 * Toàn bộ HTML nằm trong một template literal. Một dấu nháy ngược lọt vào — kể cả trong chú thích
 * CSS — là kết thúc chuỗi sớm và tệp hỏng theo kiểu rất khó đọc. Đã dính một lần.
 */
const fs = require("fs");
const path = require("path");

const ASSETS = path.join(__dirname, "..", "assets");
const OUT_PNG = path.join(__dirname, "..", "Poster-QuizAI.png");
const OUT_HTML = path.join(__dirname, "_poster.html");

/* A0 dọc ở 96 DPI */
const W = 3179;
const H = 4494;

const thieu = [];
const b64 = (ten) => {
  const p = path.join(ASSETS, "hinh-" + ten + ".png");
  if (!fs.existsSync(p)) {
    thieu.push("hinh-" + ten + ".png");
    return null;
  }
  return "data:image/png;base64," + fs.readFileSync(p).toString("base64");
};

/** Ô ảnh có chiều cao CỐ ĐỊNH — ảnh ghép rất cao, để nó tự giãn là đẩy mọi thứ phía dưới ra ngoài khung. */
const anh = (ten, chu, cao) => {
  const d = b64(ten);
  const trong = '<div class="trong" style="height:' + cao + 'px">chưa có ảnh</div>';
  const img = '<img src="' + d + '" alt="" style="height:' + cao + 'px">';
  return '<figure class="anh">' + (d ? img : trong) + '<figcaption>' + chu + '</figcaption></figure>';
};

/** Một bước trong luồng xử lý: số thứ tự trong vòng tròn + mô tả. */
const buoc = (so, chu) =>
  '<div class="buoc"><div class="tron">' + so + '</div><div class="chu">' + chu + '</div></div>';

const mui = '<div class="mui">&#10140;</div>';

const luong = (ten, mau, cacBuoc) =>
  '<div class="luong ' + mau + '">' +
  '<div class="ten-luong">' + ten + '</div>' +
  '<div class="day">' + cacBuoc.map((b, i) => buoc(i + 1, b)).join(mui) + "</div>" +
  "</div>";

const nhomCongNghe = (ten, muc) =>
  '<div class="nhom"><div class="nhan">' + ten + '</div><div class="the-list">' +
  muc.map((m) => '<span class="the">' + m + "</span>").join("") + "</div></div>";

const ketQua = (so, nhan, phu) =>
  '<div class="kq"><div class="kq-so">' + so + '</div><div class="kq-nhan">' + nhan + '</div>' +
  '<div class="kq-phu">' + phu + "</div></div>";

const html = '<!doctype html>\n<html lang="vi"><head><meta charset="utf-8"><style>\n' + `
  * { margin: 0; padding: 0; box-sizing: border-box; }
  html, body { width: ${W}px; height: ${H}px; }
  body {
    font-family: 'Segoe UI', Arial, sans-serif;
    background: #ffffff;
    color: #0f172a;
    padding: 34px;
    display: grid;
    grid-template-rows: 300px 232px 1fr 1904px 360px;
    gap: 26px;
    overflow: hidden;
  }

  /* ---------- dải đầu trang ---------- */
  .dau { display: grid; grid-template-columns: 1.08fr 1fr; gap: 26px; }

  .truong {
    background: #ffffff; border: 4px solid #16336b; border-radius: 22px;
    padding: 26px 34px; display: flex; align-items: center; gap: 30px;
  }
  .dau-hieu {
    width: 128px; height: 128px; flex: none; border-radius: 20px;
    background: #16336b; color: #ffd24a;
    display: flex; align-items: center; justify-content: center;
    font-size: 54px; font-weight: 800; letter-spacing: 1px;
  }
  .truong .ten1 { font-size: 40px; font-weight: 800; color: #16336b; line-height: 1.2; }
  .truong .ten2 { font-size: 33px; font-weight: 700; color: #1b4b9a; line-height: 1.25; margin-top: 6px; }
  .truong .ten3 { font-size: 25px; color: #475569; margin-top: 8px; letter-spacing: 2px; }

  .dot {
    background: linear-gradient(135deg, #16336b 0%, #1b4b9a 100%);
    border-radius: 22px; padding: 26px 36px; color: #ffffff;
    display: flex; flex-direction: column; justify-content: center;
  }
  .dot .loai { font-size: 38px; font-weight: 800; line-height: 1.2; }
  .dot .nganh {
    display: inline-block; margin-top: 10px; align-self: flex-start;
    background: #ffd24a; color: #16336b; font-size: 27px; font-weight: 800;
    padding: 7px 20px; border-radius: 10px; letter-spacing: 1px;
  }
  .dot .ai { font-size: 29px; line-height: 1.75; margin-top: 16px; color: #e8eefc; }
  .dot .ai b { color: #ffffff; }

  /* ---------- dải tên đề tài ---------- */
  .de-tai {
    border: 4px solid #e2e8f0; border-radius: 22px; background: #f8fafc;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    padding: 18px 40px; text-align: center;
  }
  .de-tai h1 { font-size: 63px; font-weight: 800; color: #16336b; line-height: 1.18; letter-spacing: 0.5px; }
  .de-tai .phu { font-size: 32px; color: #475569; margin-top: 12px; }

  /* ---------- khung mục ---------- */
  section {
    border: 3px solid #d7dfea; border-radius: 20px; padding: 22px 26px;
    background: #ffffff; overflow: hidden; display: flex; flex-direction: column;
  }
  section > h2 {
    font-size: 31px; font-weight: 800; color: #16336b; letter-spacing: 0.5px;
    display: flex; align-items: center; gap: 14px; margin-bottom: 16px; flex: none;
  }
  section > h2 .n {
    width: 46px; height: 46px; flex: none; border-radius: 11px; background: #16336b; color: #ffd24a;
    display: flex; align-items: center; justify-content: center; font-size: 27px; font-weight: 800;
  }

  /* ---------- hàng A ---------- */
  .hang-a { display: grid; grid-template-columns: 0.8fr 1.6fr 1fr; gap: 26px; }

  .van-de li {
    list-style: none; font-size: 27px; line-height: 1.46; color: #1e293b;
    padding-left: 34px; position: relative; margin-bottom: 15px;
  }
  .van-de li::before {
    content: ''; position: absolute; left: 0; top: 12px;
    width: 16px; height: 16px; border-radius: 5px; background: #ef4444;
  }
  .giai-phap {
    margin-top: auto; background: #fffbeb; border: 3px solid #ffd24a; border-radius: 16px; padding: 18px 22px;
  }
  .giai-phap .nhan { font-size: 27px; font-weight: 800; color: #92400e; margin-bottom: 8px; }
  .giai-phap p { font-size: 25px; line-height: 1.45; color: #78350f; }

  .anh { display: flex; flex-direction: column; }
  .anh img { width: 100%; object-fit: contain; object-position: center; display: block; background: #ffffff; }
  .anh .trong {
    display: flex; align-items: center; justify-content: center;
    color: #94a3b8; font-size: 26px; border: 3px dashed #cbd5e1; border-radius: 14px;
  }
  .anh figcaption { font-size: 23px; color: #64748b; text-align: center; margin-top: 10px; font-style: italic; }

  .nhom { margin-bottom: 15px; }
  .nhom .nhan {
    font-size: 23px; font-weight: 800; color: #16336b; letter-spacing: 1.5px; margin-bottom: 8px;
  }
  .the-list { display: flex; flex-wrap: wrap; gap: 8px; }
  .the {
    font-size: 23px; background: #eef2ff; color: #1e3a8a; border: 2px solid #c7d2fe;
    border-radius: 9px; padding: 5px 13px; white-space: nowrap;
  }

  /* ---------- hàng B ---------- */
  .hang-b { display: grid; grid-template-columns: 1.42fr 1fr; gap: 26px; }

  .luong { border-radius: 16px; padding: 22px 20px; margin-bottom: 20px; flex: 1; display: flex; flex-direction: column; justify-content: center; }
  .luong:last-child { margin-bottom: 0; }
  .luong.xanh { background: #eff6ff; border: 3px solid #bfdbfe; }
  .luong.tim { background: #f5f3ff; border: 3px solid #ddd6fe; }
  .luong.luc { background: #f0fdf4; border: 3px solid #bbf7d0; }
  .luong.luc .ten-luong { color: #166534; }
  .luong.luc .tron { background: #15803d; }
  .ten-luong { font-size: 29px; font-weight: 800; color: #16336b; margin-bottom: 18px; }
  .luong.tim .ten-luong { color: #5b21b6; }
  .day { display: flex; align-items: stretch; gap: 7px; }
  .buoc {
    flex: 1 1 0; background: #ffffff; border: 2px solid #cbd5e1; border-radius: 12px;
    padding: 11px 9px; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 7px;
  }
  .tron {
    width: 38px; height: 38px; flex: none; border-radius: 50%; background: #16336b; color: #ffffff;
    display: flex; align-items: center; justify-content: center; font-size: 21px; font-weight: 800;
  }
  .luong.tim .tron { background: #6d28d9; }
  .buoc .chu { font-size: 22px; line-height: 1.34; color: #1e293b; }
  .mui { flex: none; align-self: center; font-size: 26px; color: #94a3b8; }

  .diem {
    flex: none; background: #0f2a56; border-radius: 16px; padding: 26px 30px; color: #e8eefc;
  }
  .diem .nhan {
    font-size: 27px; font-weight: 800; color: #ffd24a; letter-spacing: 1px; margin-bottom: 14px;
  }
  .diem li {
    list-style: none; font-size: 24px; line-height: 1.45; margin-bottom: 13px;
    padding-left: 30px; position: relative;
  }
  .diem li:last-child { margin-bottom: 0; }
  .diem li::before {
    content: ''; position: absolute; left: 0; top: 11px;
    width: 13px; height: 13px; border-radius: 4px; background: #ffd24a;
  }
  .diem b { color: #ffffff; }

  /* ---------- hàng C ---------- */
  .hang-c { display: grid; grid-template-columns: repeat(6, 1fr); gap: 18px; }
  .kq {
    border: 3px solid #d7dfea; border-radius: 16px; padding: 16px 14px; text-align: center;
    display: flex; flex-direction: column; justify-content: center; background: #f8fafc;
  }
  .kq-so { font-size: 46px; font-weight: 800; color: #16336b; line-height: 1.1; }
  .kq-nhan { font-size: 24px; font-weight: 700; color: #1e293b; margin-top: 7px; line-height: 1.3; }
  .kq-phu { font-size: 21px; color: #64748b; margin-top: 7px; line-height: 1.35; }
` + "\n</style></head>\n<body>\n" + `

<div class="dau">
  <div class="truong">
    <div class="dau-hieu">HaUI</div>
    <div>
      <div class="ten1">ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI</div>
      <div class="ten2">TRƯỜNG CÔNG NGHỆ THÔNG TIN VÀ TRUYỀN THÔNG</div>
      <div class="ten3">SCHOOL OF INFORMATION AND COMMUNICATION TECHNOLOGY</div>
    </div>
  </div>
  <div class="dot">
    <div class="loai">BÁO CÁO ĐỒ ÁN TỐT NGHIỆP ĐẠI HỌC</div>
    <div class="nganh">NGÀNH: KỸ THUẬT PHẦN MỀM</div>
    <div class="ai">
      <b>Sinh viên thực hiện:</b> Nguyễn Khắc Minh Đức &nbsp;·&nbsp; MSV 2022601585<br>
      <b>Giảng viên hướng dẫn:</b> ThS. Nguyễn Đức Lưu
    </div>
  </div>
</div>

<div class="de-tai">
  <h1>XÂY DỰNG ỨNG DỤNG QUIZ/TRIVIA TÍCH HỢP TRÍ TUỆ NHÂN TẠO</h1>
  <div class="phu">Sinh đề và trợ lý học tập bằng RAG · Phòng đấu thời gian thực · Gợi ý cá nhân hoá trên đồ thị</div>
</div>

<div class="hang-a">
  <section class="van-de">
    <h2><span class="n">1</span>ĐẶT VẤN ĐỀ</h2>
    <ul>
      <li>Giáo viên phải tự gõ từng câu hỏi, dù học liệu của môn đã có sẵn dưới dạng tài liệu.</li>
      <li>Câu trả lời ngắn và tự luận hoặc không được hỗ trợ, hoặc chỉ so khớp chuỗi máy móc, hoặc phải chấm tay.</li>
      <li>Gợi ý nội dung học tiếp dựa trên mức phổ biến của bài thi, chưa dựa trên năng lực suy ra từ hành vi.</li>
      <li>Người học không biết mình yếu chủ đề nào và nên ôn gì tiếp theo.</li>
    </ul>
    <div class="giai-phap">
      <div class="nhan">GIẢI PHÁP</div>
      <p>Nền tảng quiz tích hợp AI: sinh đề bám chính học liệu bằng RAG, chấm tự luận có hàng rào nhiều lớp,
      gợi ý theo năng lực trên đồ thị Neo4j, và phòng đấu thời gian thực tính điểm theo tốc độ.</p>
    </div>
  </section>

  <section>
    <h2><span class="n">2</span>KIẾN TRÚC HỆ THỐNG</h2>
    ${anh("1.1", "Khối đơn phân lớp · ba kênh giao tiếp: REST, WebSocket, SSE", 1130)}
  </section>

  <section>
    <h2><span class="n">3</span>CÔNG NGHỆ SỬ DỤNG</h2>
    ${nhomCongNghe("GIAO DIỆN", ["React 19", "TypeScript", "Vite 8", "Ant Design v6", "Tailwind v4", "TanStack Query", "Zustand", "STOMP", "SSE"])}
    ${nhomCongNghe("MÁY CHỦ", ["Java 21", "Spring Boot 3.5", "Spring Security", "Data JPA", "WebSocket", "Flyway", "Resilience4j", "Apache Tika"])}
    ${nhomCongNghe("TRÍ TUỆ NHÂN TẠO", ["Google Gemini", "Groq (dự phòng)", "RAG tự viết", "AiOrchestrator"])}
    ${nhomCongNghe("DỮ LIỆU", ["PostgreSQL 16", "pgvector", "Neo4j 5", "Redis 7"])}
    ${nhomCongNghe("HẠ TẦNG & KIỂM THỬ", ["Docker Compose", "JUnit 5", "Testcontainers", "Vitest"])}
  </section>
</div>

<div class="hang-b">
  <section>
    <h2><span class="n">4</span>LUỒNG XỬ LÝ CỐT LÕI</h2>
    ${luong("LUỒNG 1 — Nạp học liệu và sinh đề (RAG)", "xanh", [
      "Nạp tài liệu PDF / DOCX / TXT",
      "Apache Tika bóc tách văn bản",
      "Chia đoạn 1500 ký tự, chồng lấp 200",
      "Gemini embedding, vector 768 chiều",
      "Lưu vào pgvector (material_chunks)",
      "Lọc quyền đọc TRƯỚC rồi mới xếp cosine, top-K = 5",
      "Loại đoạn vượt ngưỡng 0,75",
      "Sinh JSON, kiểm lược đồ, người duyệt mới vào ngân hàng",
    ])}
    ${luong("LUỒNG 2 — Phòng đấu thời gian thực", "tim", [
      "Chủ phòng mở phòng, nhận mã PIN 6 số và mã QR",
      "Người chơi vào bằng PIN hoặc quét QR, khách cũng vào được",
      "Xác thực JWT ngay tại khung STOMP CONNECT",
      "Trạng thái ván giữ trên Redis",
      "Máy chủ đẩy câu hỏi đồng thời qua WebSocket",
      "Sự kiện phát tán qua Redis Pub/Sub tới mọi tiến trình",
      "Tính điểm theo tốc độ trả lời",
      "Bảng xếp hạng cập nhật ngay sau mỗi câu",
    ])}
    ${luong("LUỒNG 3 — Gợi ý cá nhân hoá trên đồ thị (Neo4j)", "luc", [
      "Người học nộp bài",
      "Phát sự kiện sau khi giao dịch được ghi nhận",
      "Công việc nền đồng bộ sang Neo4j bằng MERGE",
      "Tính lại năng lực từng chủ đề trên toàn lịch sử",
      "Cypher duyệt quan hệ nhiều bậc",
      "Gợi ý quiz theo chủ đề còn yếu",
      "Gợi ý theo người học có kết quả tương tự",
      "Lộ trình: thứ tự chủ đề nên ôn",
    ])}
    <div class="diem">
      <div class="nhan">BA ĐIỂM KỸ THUẬT ĐÁNG CHÚ Ý</div>
      <ul>
        <li><b>Lọc quyền đọc phải đứng TRƯỚC khâu xếp hạng.</b> Lọc sau thì chỉ mục xấp xỉ lấy 5 đoạn gần nhất
        toàn kho rồi mới loại theo quyền — đo thật: trả về rỗng trong khi kho có 9 đoạn hợp lệ, và hỏng
        hoàn toàn im lặng, không lỗi, không cảnh báo.</li>
        <li><b>Điểm tính theo tốc độ trả lời nên độ trễ là yêu cầu CHỨC NĂNG</b>, không phải chỉ tiêu kỹ thuật:
        độ trễ không đều giữa người chơi gây bất công về điểm.</li>
        <li><b>Con người giữ quyền kết luận cuối.</b> Câu hỏi AI sinh ra chỉ là bản nháp, chỉ vào ngân hàng khi
        người tạo nội dung duyệt; điểm do AI chấm luôn bị ràng buộc trong miền điểm thật của câu.</li>
      </ul>
    </div>
  </section>

  <section>
    <h2><span class="n">5</span>GIAO DIỆN SẢN PHẨM</h2>
    ${anh("3.6", "Học liệu và sinh đề bằng AI — mỗi bộ câu hỏi ghi rõ số đoạn học liệu đã bám theo", 900)}
    <div style="height:18px"></div>
    ${anh("3.5", "Phòng đấu — mã PIN, mã QR và bảng xếp hạng cập nhật trực tiếp", 800)}
  </section>
</div>

<div class="hang-c">
  ${ketQua("16", "nhóm chức năng", "87 yêu cầu chức năng, 4 tác nhân")}
  ${ketQua("696", "phép kiểm tự động", "568 máy chủ + 128 giao diện, 0 hỏng")}
  ${ketQua("216 ms", "P95 phòng đấu", "100 người mỗi phòng, 0 sự kiện mất")}
  ${ketQua("0,13", "sai lệch chấm tự luận", "trên thang 10, đối chiếu đáp án tiêu chí")}
  ${ketQua("10/10", "câu sinh đúng chuẩn", "kiểm lại độc lập ở phía kịch bản đo")}
  ${ketQua("3", "hệ quản trị dữ liệu", "PostgreSQL + pgvector · Neo4j · Redis")}
</div>

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

  /* Khung A0 cắt im lặng — đo trước khi chụp. */
  const do_ = await page.evaluate(() => ({
    cao: document.body.scrollHeight,
    rong: document.body.scrollWidth,
  }));

  await page.screenshot({ path: OUT_PNG });
  await browser.close();

  const { size } = fs.statSync(OUT_PNG);
  console.log("OK -> " + path.basename(OUT_PNG) + "  " + (size / 1024 / 1024).toFixed(1) + " MB  " +
    W * 2 + "x" + H * 2 + " px (A0 dọc, ~192 DPI)");
  console.log("     nội dung cao " + do_.cao + " px / khung " + H + " px");

  if (thieu.length) {
    console.warn("\nTHIẾU " + thieu.length + " ảnh, ô tương ứng để trống:");
    for (const t of new Set(thieu)) console.warn("  " + t);
  }
  if (do_.cao > H || do_.rong > W) {
    console.error("\nTRÀN KHUNG A0: cao " + do_.cao + "/" + H + " px, rộng " + do_.rong + "/" + W + " px.");
    console.error("Chrome đã cắt phần thừa mà không báo. Rút bớt nội dung hoặc hạ chiều cao ô ảnh rồi chạy lại.");
    process.exit(1);
  }
})().catch((e) => {
  console.error("FAIL:", e.message);
  process.exit(1);
});
