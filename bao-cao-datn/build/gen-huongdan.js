/* Sinh hướng dẫn thuyết trình -> ../HuongDanThuyetTrinh-QuizAI.docx
 *
 * Chạy:  cd bao-cao-datn/build && node gen-huongdan.js
 *
 * ## Tài liệu này dùng để làm gì
 * Không phải bài nói viết sẵn để đọc thuộc. Nó là tờ chuẩn bị: mỗi slide nói ý gì trong bao lâu, câu
 * nào phải nói đúng chữ (những câu có số liệu), và quan trọng nhất — bảng câu hỏi hội đồng nhiều khả
 * năng hỏi kèm câu trả lời có căn cứ.
 *
 * ## Nguyên tắc
 * Mọi con số ở đây trùng với báo cáo và slide. Phần "câu hỏi có thể gặp" chỉ ghi những câu mà đồ án
 * TRẢ LỜI ĐƯỢC bằng bằng chứng — kể cả khi câu trả lời là "chỗ đó em chưa đo được, và đây là lý do".
 * Một câu trả lời thành thật về giới hạn an toàn hơn nhiều so với một câu trả lời vòng vo.
 */
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, WidthType, HeadingLevel, BorderStyle, ShadingType,
} = require("docx");

const OUT = path.join(__dirname, "..", "HuongDanThuyetTrinh-QuizAI.docx");
const FONT = "Times New Roman";
const SIZE = 26; // 13pt
const CW = 9071;

const p = (text, o = {}) =>
  new Paragraph({
    spacing: { line: 340, after: 110 },
    alignment: o.giua ? AlignmentType.CENTER : AlignmentType.JUSTIFIED,
    children: [new TextRun({ text, font: FONT, size: o.co ?? SIZE, bold: o.dam, italics: o.nghieng })],
  });

const h = (text, cap = 1) =>
  new Paragraph({
    heading: cap === 1 ? HeadingLevel.HEADING_1 : HeadingLevel.HEADING_2,
    spacing: { before: 240, after: 120, line: 340 },
    children: [new TextRun({ text, font: FONT, size: cap === 1 ? 30 : 28, bold: true })],
  });

function bang(dauMuc, hang, rong) {
  const vien = () => {
    const b = { style: BorderStyle.SINGLE, size: 4, color: "999999" };
    return { top: b, bottom: b, left: b, right: b };
  };
  const o = (t, dam, fill) =>
    new TableCell({
      borders: vien(),
      shading: fill ? { fill, type: ShadingType.CLEAR, color: "auto" } : undefined,
      margins: { top: 60, bottom: 60, left: 100, right: 100 },
      children: String(t)
        .split("\n")
        .map((d) => new Paragraph({ spacing: { line: 260 }, children: [new TextRun({ text: d, font: FONT, size: 24, bold: dam })] })),
    });
  const cot = rong ?? Array(dauMuc.length).fill(Math.floor(CW / dauMuc.length));
  const rows = [new TableRow({ tableHeader: true, children: dauMuc.map((t) => o(t, true, "E7EEF6")) })];
  for (const hg of hang) rows.push(new TableRow({ children: hg.map((t) => o(t, false)) }));
  return new Table({ width: { size: CW, type: WidthType.DXA }, columnWidths: cot, rows });
}

/* ─────────────────── kịch bản theo slide ─────────────────── */
const KICH_BAN = [
  ["1", "Bìa", "0:20", "Chào hội đồng, giới thiệu tên và tên đề tài. Không đọc lại cả trang bìa."],
  ["2", "Nội dung thuyết trình", "0:15", "Đọc lướt bốn phần để hội đồng biết đường đi. Đừng dừng lâu."],
  ["3", "NGĂN — I. Lý do chọn đề tài", "0:05", "Slide ngăn: chỉ đọc tên phần rồi lật ngay. Đừng giải thích gì ở đây."],
  ["4", "Ba khoảng trống", "1:10", "Soạn đề thủ công, không chấm được tự luận, gợi ý theo lượt xem chứ không theo năng lực. Đây là chỗ thuyết phục hội đồng rằng đề tài có lý do tồn tại."],
  ["5", "Bốn trọng tâm", "0:50", "Bám nguyên bốn mục của phiếu giao đề tài. Nói rõ trọng tâm thứ tư là ĐO chứ không phải làm thêm chức năng."],
  ["6", "NGĂN — II. Cơ sở lý thuyết", "0:05", "Lật ngay."],
  ["7", "Kiến trúc tổng quan", "1:05", "Ba kênh giao tiếp cho ba dạng dữ liệu: REST, WebSocket, SSE. Chỉ vào hình khi nói. Dòng chữ nhỏ dưới sơ đồ gánh luôn phần công nghệ — nếu bị hỏi thì nêu ba lựa chọn đáng nói: pgvector để lọc quyền cùng lúc với tìm vector, Neo4j cho quan hệ, và lớp điều phối mô hình tự viết."],
  ["8", "Pipeline RAG", "1:10", "Hai pha: nạp học liệu và truy hồi. Nhấn vào điểm lọc quyền TRƯỚC khi xếp hạng — đây là chỗ từng có lỗi thật, kể ra được thì rất có sức nặng."],
  ["9", "NGĂN — III. Thực nghiệm", "0:05", "Lật ngay. Đây là phần dài nhất, bảy slide."],
  ["10", "Phòng đấu — cách làm", "1:15", "Bốn bước ở cột trái, ảnh màn thật ở cột phải. ĐỪNG mô tả lại ảnh — chỉ tay vào mã PIN và mã QR rồi nói tiếp về cách làm. Nhấn: điểm phụ thuộc tốc độ nên độ trễ thành yêu cầu CHỨC NĂNG, không phải chỉ tiêu kỹ thuật. Đây là bệ đỡ cho slide 14."],
  ["11", "Sinh đề — cách làm", "1:20", "Bốn bước: nạp học liệu, truy hồi có lọc quyền, sinh JSON có kiểm chứng lược đồ, người duyệt cuối. Nhấn bước cuối: AI không tự đưa câu hỏi vào ngân hàng. Số để sẵn nếu bị hỏi: chia đoạn 1 500 ký tự chồng lấp 200, mỗi đoạn thành vector 768 chiều, sinh đề lấy 6 đoạn gần nhất và KHÔNG áp ngưỡng khoảng cách."],
  ["12", "Trợ lý học tập — cách làm", "0:50", "Nhấn hai điều: câu trả lời kèm khối trích dẫn nguồn, và hỏi ngoài học liệu thì trợ lý nói không tìm thấy chứ không đoán. Số để sẵn: trợ lý lấy 5 đoạn và bỏ đoạn vượt ngưỡng khoảng cách 0,75 — KHÁC sinh đề. Bảng 1.4 của báo cáo ghi một bộ tham số chung; nếu thầy đối chiếu thì nói rõ bảng đó là tham số của trợ lý, còn sinh đề lấy 6 đoạn và sinh thẻ ghi nhớ lấy 8."],
  ["13", "Lộ trình học — cách làm", "0:50", "Slide đại diện cho trụ cột Neo4j. Nêu ba truy vấn: chủ đề còn yếu, người học tương tự, thứ tự chủ đề nên ôn. Nói rõ PostgreSQL là nguồn sự thật, Neo4j chỉ là hình chiếu."],
  ["14", "Kết quả đo hiệu năng", "1:20", "Câu phải nói đúng chữ: \"P95 là 216 mili giây ở mức 100 người mỗi phòng, và không mất một sự kiện nào ở mọi mức tải đã thử tới 200 người.\" Nói ngay giới hạn: đo trên một máy đơn, không có độ trễ mạng thật."],
  ["15", "Độ chính xác AI", "1:20", "Câu phải nói đúng chữ: \"Sai lệch điểm trung bình 0,13 trên thang 10 khi đối chiếu với đáp án theo tiêu chí.\" Nói ngay: cỡ mẫu nhỏ, chưa đối chiếu với nhiều giáo viên chấm độc lập. Đừng đọc hết tám dòng trong bảng."],
  ["16", "Kiểm thử", "0:40", "568 phép kiểm máy chủ và 128 phép kiểm giao diện, đều đạt. Rồi nói thẳng: ba lỗi thật lộ ra khi dùng chứ không qua kiểm thử — đây là chỗ ghi điểm thành thật, đừng bỏ."],
  ["17", "NGĂN — IV. Kết luận", "0:05", "Lật ngay."],
  ["18", "Kết quả đạt được", "1:20", "Bốn hàng nói về sản phẩm trên tổng thể trang web, cố ý KHÔNG có con số — số đã nói hết ở slide 14 và 15. Đừng đọc lại y nguyên chữ trên màn, mỗi hàng diễn giải thêm một câu. Kết lại bằng một câu tự nói: phần khó nhất của hệ thống tích hợp mô hình ngôn ngữ không nằm ở việc gọi được mô hình, mà ở việc dựng đủ hàng rào quanh nó."],
  ["19", "Hướng phát triển và hạn chế", "0:40", "Ba hướng ở trên, ba hạn chế ở dưới. Nói theo CẶP — mỗi hướng là lời đáp cho đúng một hạn chế ngay bên dưới. Tự nêu hạn chế trước khi hội đồng hỏi thì đó là điểm cộng, để bị chỉ ra thì thành thiếu sót."],
  ["20", "Cảm ơn", "0:15", "Cảm ơn và mời hội đồng đặt câu hỏi."],
];

/* ─────────────────── câu hỏi có thể gặp ─────────────────── */
const CAU_HOI = [
  [
    "Vì sao không dùng Spring AI hay LangChain4j cho phần AI?",
    "Ba thứ đồ án phải đo và báo cáo là cơ chế dự phòng giữa hai nhà cung cấp, hạn mức, và nhật ký lời gọi. Cả ba đều nằm ở lớp điều phối. Dùng thư viện trừu tượng hoá sẵn thì ba thứ đó bị gói lại phía sau một giao diện chung, khó can thiệp và khó đo. Lớp AiOrchestrator tự viết bằng WebClient chỉ khoảng vài trăm dòng.",
  ],
  [
    "Hệ thống chống việc người học bảo AI cho điểm cao bằng cách nào?",
    "Bốn lớp. Bài làm được rào trong khối dữ liệu có dấu mở và đóng riêng; chỉ dẫn hệ thống nói rõ câu lệnh bên trong khối đó là nội dung cần chấm; chuỗi rào do người học tự gõ bị vô hiệu hoá trước khi dựng prompt; và lớp cuối là ràng buộc miền giá trị — điểm mô hình trả về luôn bị cắt về khoảng từ 0 tới điểm tối đa của câu. Dù ba lớp đầu thủng thì điểm vẫn không vượt được trần thật. Đã thử tấn công 2 lần, chặn được cả 2.",
  ],
  [
    "Con số 216 mili giây đo trong điều kiện nào?",
    "Trên một máy đơn: máy chủ, ba cơ sở dữ liệu và toàn bộ client chạy cùng máy, ngày 08/08/2026. Con số này là chi phí xử lý của máy chủ và tầng phát tán, KHÔNG bao gồm độ trễ mạng thật, nên không được đọc thành trải nghiệm của người dùng ở xa. Báo cáo ghi rõ giới hạn này ở mục 3.5.",
  ],
  [
    "Sao không đo trên nhiều máy chủ có mạng thật?",
    "Không có hạ tầng cho việc đó trong phạm vi đồ án. Nhưng có đo một phần liên quan: chạy hai tiến trình máy chủ trên cùng máy để kiểm chứng cơ chế phát tán qua Redis, chi phí khoảng 2 mili giây cho mỗi sự kiện đi vòng qua Redis. Đo trên hạ tầng thật là hướng phát triển trung hạn.",
  ],
  [
    "Làm sao biết AI chấm đúng?",
    "Đối chiếu điểm mô hình trả về với đáp án theo tiêu chí chấm do người soạn cung cấp, trên 8 bài: 7 bài có điểm nằm trong khoảng chuẩn, sai lệch trung bình 0,13 trên thang 10. Nhưng phải nói rõ giới hạn: đây là đối chiếu với ĐÁP ÁN THEO TIÊU CHÍ, chưa phải với nhiều giáo viên chấm độc lập, nên chưa kết luận được là AI chấm ngang giáo viên. Cỡ mẫu cũng nhỏ.",
  ],
  [
    "Nếu mô hình trả lời sai thì sao?",
    "Hệ thống không để nội dung AI sinh tự vào ngân hàng câu hỏi — người tạo nội dung phải duyệt từng câu. Với chấm bài, điểm do người chấm tay luôn thắng: kết quả AI trả về muộn hơn bị bỏ qua. Đây là nguyên tắc con người ở vòng cuối, áp dụng nhất quán.",
  ],
  [
    "Vì sao dùng tới ba cơ sở dữ liệu?",
    "Mỗi hệ giải một bài toán khác nhau và được phân định nguồn sự thật rõ ràng. PostgreSQL giữ sự thật. Redis giữ trạng thái ngắn hạn của phòng đang chơi, phiên đăng nhập và hạn mức — những thứ đổi liên tục trong vài phút rồi hết giá trị. Neo4j giữ bản chiếu hành vi phục vụ phân tích. Hệ quả thực tế: mất dữ liệu ở hai hệ sau không phải sự cố, vì dựng lại được từ PostgreSQL.",
  ],
  [
    "Neo4j có thật sự cần không, hay dùng SQL cũng được?",
    "Với ba truy vấn hiện có thì SQL cũng làm được. Chọn Neo4j vì các truy vấn gợi ý là truy vấn duyệt quan hệ nhiều bước — tìm người học tương tự nghĩa là đi qua hai cạnh ATTEMPTED — và loại truy vấn đó ở đồ thị có chi phí không phụ thuộc tổng kích thước dữ liệu, còn ở quan hệ thì mỗi bước là một phép kết. Đây cũng là một trọng tâm phiếu giao đề tài nêu đích danh.",
  ],
  [
    "Chức năng chống gian lận có kết luận ai gian lận không?",
    "Không, và đó là thiết kế có chủ ý. Tín hiệu thu từ trình duyệt người thi nên chặn được và giả mạo được, mỗi tín hiệu đều có cách giải thích vô hại. Hệ thống chỉ tính điểm rủi ro, liệt kê lý do cụ thể của từng cờ, rồi để người phụ trách kết luận — giao diện nói thẳng giới hạn đó ngay cạnh con số chứ không giấu. Hệ thống không tự trừ điểm, không tự huỷ bài của ai.",
  ],
  [
    "Điểm khó nhất khi làm đồ án là gì?",
    "Không phải gọi được mô hình mà là dựng đủ hàng rào quanh nó. Ví dụ cụ thể: chỉ mục xấp xỉ cho kho vector xếp hạng TRƯỚC rồi mới lọc quyền đọc, nên trợ lý trả lời \"không có tài liệu\" trong khi kho có đoạn hợp lệ — một lỗi hoàn toàn im lặng, không ngoại lệ, không mã lỗi. Toàn bộ số liệu đánh giá AI đo trước khi phát hiện lỗi này đều phải bỏ và đo lại.",
  ],
  [
    "Bộ kiểm thử có bỏ sót gì không?",
    "Có, và báo cáo ghi lại ở mục 3.4.4. Ba lỗi thật lộ ra khi mở ra dùng chứ không qua kiểm thử, trong đó một lỗi có hẳn một phép kiểm khẳng định đúng cái hành vi sai đó — phép kiểm viết đúng với đặc tả tại thời điểm viết, và đặc tả mới là thứ hết hạn khi hệ thống lớn lên. Vì vậy bước chạy thật và xác nhận trên trình duyệt được đặt thành bước bắt buộc, ngang hàng với chạy kiểm thử tự động.",
  ],
  [
    "Hệ thống chịu được bao nhiêu người dùng?",
    "Câu trả lời trung thực: chưa đo được số người dùng đồng thời của cả hệ thống. Cái đã đo là độ trễ phòng đấu theo số người trong MỘT phòng, tới 200 người. Ngưỡng dùng được trong thực tế là 100 người mỗi phòng. Chưa đo nhiều phòng chạy song song, và đó là hướng phát triển trung hạn vì nó gần với một buổi học thật hơn.",
  ],
];

/* ─────────────────── dựng tài liệu ─────────────────── */
const noi = [];
noi.push(p("HƯỚNG DẪN THUYẾT TRÌNH BẢO VỆ ĐỒ ÁN TỐT NGHIỆP", { giua: true, dam: true, co: 34 }));
noi.push(p("Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo", { giua: true, co: 28 }));
noi.push(p("Nguyễn Khắc Minh Đức — 2022601585 — GVHD: ThS. Nguyễn Đức Lưu", { giua: true, nghieng: true, co: 24 }));

noi.push(h("1. Phân bổ thời gian"));
noi.push(p("Bộ slide gồm 20 slide, trong đó bốn slide ngăn chỉ mất năm giây mỗi cái. Cộng lại đúng 15 phút theo bảng dưới. Bốn slide cách làm ở phần III đặt ảnh màn hình thật bên phải chữ: ảnh ở đó để hội đồng thấy sản phẩm có thật, KHÔNG phải để đọc — mọi màn hình ấy đều được demo trực tiếp ngay sau phần thuyết trình. Ba mốc quan trọng nhất là slide 4 (thuyết phục về lý do đề tài), slide 14 và 15 (hai phép đo bắt buộc theo phiếu giao đề tài). Nếu bị nhắc rút ngắn, cắt bớt ở slide 12 và 13; tuyệt đối không cắt hai slide kết quả đo."));
noi.push(bang(["Slide", "Nội dung", "Thời lượng", "Ý cần nói"], KICH_BAN.map((k) => [k[0], k[1], k[2], k[3]]), [900, 1900, 1100, CW - 3900]));

noi.push(h("2. Những câu phải nói đúng chữ"));
noi.push(p("Bốn câu dưới đây chứa số liệu, và số liệu là thứ hội đồng hỏi lại. Nói đúng con số, và nói kèm giới hạn của nó ngay trong cùng một hơi — không chờ bị hỏi mới nêu giới hạn."));
noi.push(bang(["Câu", "Giới hạn phải nói kèm"], [
  ["P95 là 216 mili giây ở mức 100 người mỗi phòng, không mất một sự kiện nào ở mọi mức tải đã thử tới 200 người.", "Đo trên một máy đơn, không bao gồm độ trễ mạng thật."],
  ["Sai lệch điểm trung bình 0,13 trên thang 10 khi AI chấm câu tự luận.", "Đối chiếu với đáp án theo tiêu chí, chưa phải với nhiều giáo viên chấm độc lập; cỡ mẫu 8 bài."],
  ["10 trên 10 câu AI sinh ra đúng chuẩn cấu trúc, không câu nào bị bộ kiểm loại.", "Đây là đúng CẤU TRÚC, không phải đúng chất lượng sư phạm — đồ án không đánh giá phần đó."],
  ["568 phép kiểm máy chủ và 128 phép kiểm giao diện, tất cả đều đạt.", "Vẫn có ba lỗi thật lọt qua và chỉ lộ khi dùng thật; trình bày ở mục 3.4.4."],
], [4600, CW - 4600]));

noi.push(h("3. Câu hỏi hội đồng có thể đặt"));
noi.push(p("Sắp theo mức khả năng được hỏi. Câu trả lời dưới đây đều có căn cứ trong báo cáo; chỗ nào đồ án chưa làm được thì nói thẳng là chưa làm được kèm lý do, đừng vòng vo — một câu trả lời thành thật về giới hạn an toàn hơn nhiều."));
let i = 0;
for (const [hoi, dap] of CAU_HOI) {
  i++;
  noi.push(new Paragraph({
    spacing: { before: 180, after: 60, line: 340 },
    children: [new TextRun({ text: `Câu ${i}. ${hoi}`, font: FONT, size: SIZE, bold: true })],
  }));
  noi.push(p(dap));
}

noi.push(h("4. Chuẩn bị trước buổi bảo vệ"));
noi.push(bang(["Việc", "Ghi chú"], [
  ["Bật sẵn hệ thống", "docker compose up -d, backend và frontend chạy trước ít nhất 5 phút. Mở sẵn các tab cần demo để không phải gõ địa chỉ."],
  ["Backend chạy cổng 8081, KHÔNG phải 8080", "Trên máy demo, cổng 8080 đang bị một dịch vụ khác của Windows chiếm. Chạy backend kèm --server.port=8081 cho khớp frontend/.env.local; quên bước này thì mọi lời gọi API rơi vào chỗ trống và màn hình trắng dữ liệu."],
  ["Đừng demo đăng nhập bằng Google", "Client ID chưa khai báo http://localhost:5173 trong danh sách origin được phép, nên nút đó không chạy trên máy demo. Đăng nhập bằng email và mật khẩu. Nếu hội đồng hỏi, nói rõ đây là cấu hình origin phía Google cho từng địa chỉ triển khai, không phải lỗi mã nguồn."],
  ["Chuẩn bị sẵn một phòng đấu", "Nếu định demo trực tiếp: mở phòng và để sẵn mã PIN. Có người thứ hai vào bằng điện thoại thì phần này thuyết phục nhất."],
  ["Kiểm tra khoá API của mô hình", "Hạn mức gói miễn phí có thể cạn. Nếu demo sinh đề mà bị chặn hạn mức thì nói rõ đó là giới hạn của gói miễn phí, và cho xem kết quả đã sinh sẵn."],
  ["Mang bản in", "Báo cáo, và một tờ ghi bốn con số ở mục 2 phòng khi quên."],
  ["Không demo thứ chưa chắc", "Chỉ demo những màn đã thử lại ngay trước buổi. Một thao tác hỏng giữa buổi tốn nhiều thời gian hơn giá trị nó mang lại."],
], [2600, CW - 2600]));

const doc = new Document({
  styles: { default: { document: { run: { font: FONT, size: SIZE } } } },
  sections: [{
    properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1134, right: 1134, bottom: 1134, left: 1701 } } },
    children: noi,
  }],
});

Packer.toBuffer(doc).then((b) => {
  fs.writeFileSync(OUT, b);
  console.log(`OK -> ${path.basename(OUT)}  ${fs.statSync(OUT).size} bytes  (${KICH_BAN.length} slide · ${CAU_HOI.length} câu hỏi)`);
});
