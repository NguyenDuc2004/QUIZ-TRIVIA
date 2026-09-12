/* Sinh bộ tài liệu kiểm thử của đồ án:
 *   ../Testcase+TestPlan/Test_Plan_QuizAI.docx   — kế hoạch kiểm thử, 5 mục theo khung của khoa
 *   ../Testcase+TestPlan/Test_Case_QuizAI.xlsx   — hai sheet: ca kiểm thử chức năng · kiểm thử tự động
 *
 * Chạy:  cd bao-cao-datn/build && node gen-testdocs.js
 *
 * ## Vì sao có tệp này
 * Thư mục `Testcase+TestPlan/` trước đây chỉ chứa bài mẫu của HAI đồ án khác (EduExam AI và Phụ Kiện
 * Việt), không có tài liệu nào của Quiz AI. Trong khi đó mục 3.4 của báo cáo lại dẫn người đọc sang
 * "tài liệu kế hoạch và ca kiểm thử kèm theo đồ án" — một tài liệu chưa tồn tại.
 *
 * ## Mọi con số ở đây phải truy được về nguồn
 * - 31 ca kiểm thử chức năng: lấy nguyên từ Bảng 3.2 bản đầy đủ của báo cáo (commit trước khi bảng
 *   được rút gọn còn 12 dòng).
 * - Phân bố phép kiểm theo nhóm: lấy từ Bảng 3.4 của báo cáo.
 * - Tổng 606 phép kiểm / 54 lớp / 0 hỏng: lượt chạy `./mvnw test` ngày 09/09/2026, BUILD SUCCESS.
 * - 128 phép kiểm frontend / 21 tệp: lượt chạy `npm test` cùng ngày.
 * Sửa số ở đây mà không sửa báo cáo là làm hai tài liệu nói khác nhau — kiểm tra cả hai nơi.
 */
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, WidthType, HeadingLevel, BorderStyle, ShadingType, PageBreak,
} = require("docx");
const ExcelJS = require("exceljs");

const RA = path.join(__dirname, "..", "Testcase+TestPlan");
const FONT = "Times New Roman";
const SIZE = 26; // 13pt
const CONTENT_WIDTH = 9071;

/* ────────────────────────────── dữ liệu nguồn ────────────────────────────── */

/** 31 ca kiểm thử chức năng — nguyên văn Bảng 3.2 bản đầy đủ của báo cáo. */
const CA = [
  ["Đăng ký", "Email đã tồn tại", "Email trùng tài khoản có sẵn", "Mã lỗi 409, không tạo tài khoản thứ hai"],
  ["Đăng ký", "Tự đăng ký vai trò quản trị", "role=ADMIN", "Hạ xuống vai trò người học"],
  ["Đăng nhập Google", "Tài khoản mới, chọn vai trò người tạo", "role=CREATOR", "Tạo tài khoản đúng vai trò đã chọn"],
  ["Đăng nhập Google", "Tài khoản đã có, gửi kèm vai trò cao hơn", "Tài khoản người học gửi role=CREATOR", "Giữ nguyên vai trò cũ"],
  ["Đổi mật khẩu", "Sau khi đổi", "Hai thiết bị đang đăng nhập", "Thu hồi phiên trên mọi thiết bị"],
  ["Xem quiz", "Khách chưa đăng nhập xem quiz công khai", "Không có token", "Trả về thông tin giới thiệu, không kèm câu hỏi"],
  ["Xem quiz", "Khách xem quiz riêng tư của người khác", "Mã quiz riêng tư", "Trả về 404 chứ không phải 403"],
  ["Làm bài", "Chủ quiz sửa đề khi có người đang làm dở", "Sửa quiz giữa chừng", "Lượt đang làm giữ nguyên đề đã chốt"],
  ["Làm bài", "Hết giờ", "Quiz có thời lượng", "Tự chuyển sang trạng thái hết hạn"],
  ["Chấm tự luận", "Mô hình trả điểm vượt trần", "Điểm lớn hơn điểm tối đa của câu", "Giới hạn cứng về trần thật của câu"],
  ["Chấm tự luận", "Người đã chấm tay, AI trả kết quả sau", "Ghi đè điểm rồi mới có phản hồi AI", "Bỏ qua kết quả AI trả về muộn"],
  ["Chấm tự luận", "Gọi mô hình thất bại", "Ngắt nhà cung cấp", "Chuyển trạng thái dừng rõ ràng, không treo ở đang chấm"],
  ["Phòng đấu", "Khách vào phòng khi chủ phòng chưa bật", "Mã PIN đúng, cờ tắt", "Từ chối với mã 403"],
  ["Phòng đấu", "Mất kết nối rồi vào lại", "Ngắt WebSocket giữa ván", "Giữ nguyên điểm đã có"],
  ["Phòng đấu", "Hai tiến trình máy chủ", "Người chơi chia hai instance", "Cả hai bên nhận đủ sự kiện"],
  ["Sinh đề AI", "Người dùng đã hết hạn mức", "Hạn mức trong ngày đã dùng hết", "Trả 429 ngay, không gọi mô hình"],
  ["Sinh đề AI", "Câu trả về sai định dạng", "JSON thiếu trường", "Bộ kiểm cấu trúc loại câu đó"],
  ["Trợ lý học tập", "Hỏi ngoài phạm vi học liệu", "Câu hỏi không liên quan", "Trả lời không biết, không suy đoán"],
  ["Trợ lý học tập", "Học liệu của người khác chưa chia sẻ", "Tài liệu riêng tư", "Không xuất hiện trong truy hồi"],
  ["Gợi ý", "Neo4j ngừng hoạt động", "Dừng dịch vụ đồ thị", "API trả danh sách rỗng, không làm hỏng việc nộp bài"],
  ["Tải ảnh", "Tệp giả dạng ảnh", "Tệp mã lệnh đặt đuôi .png", "Từ chối theo chữ ký byte"],
  ["Ảnh đại diện", "Người học đổi ảnh", "Ảnh đã tải lên hệ thống", "Cho phép, và chỉ giữ một ảnh cho mỗi người"],
  ["Ảnh đại diện", "Dán URL bên ngoài", "Địa chỉ máy chủ lạ", "Từ chối với mã 400"],
  ["Giao bài", "Nộp sau hạn", "Nộp quá hạn nộp", "Vẫn nhận, đánh dấu là nộp trễ"],
  ["Giao bài", "Xoá quiz đang được giao", "Quiz gắn với bài tập", "Chặn thao tác xoá"],
  ["Gamification", "Cùng một hành động ghi nhận hai lần", "Gọi lại sự kiện cộng điểm", "Chỉ cộng đúng một lần"],
  ["Xếp hạng mùa", "Mùa có dưới mười người tham gia", "3 người", "Không phân hạng cho ai"],
  ["Thông báo", "Công việc nhắc ôn chạy lại trong ngày", "Khởi động lại máy chủ", "Không gửi trùng"],
  ["Chống gian lận", "Gửi tín hiệu cho lượt luyện tập", "Lượt không tính điểm", "Máy chủ từ chối ghi nhận"],
  ["Chống gian lận", "Câu chữ hiện cho người thi", "Đã ghi nhận 5 lần rời trang", "Không chứa chữ mang tính kết tội"],
  ["Chống gian lận", "Mốc thời gian ở tương lai", "Đồng hồ máy khách sai", "Cắt về thời điểm hiện tại"],
];

/** Phân bố phép kiểm máy chủ theo nhóm chức năng — Bảng 3.4 của báo cáo. */
const PHAN_BO = [
  ["AI: RAG, sinh đề, chấm tự luận, hạn mức", 116], ["Làm bài và chấm điểm", 57],
  ["Xác thực và phân quyền", 43], ["Quản lý quiz và câu hỏi", 38],
  ["Lớp học và giao bài", 38], ["Chống gian lận", 33],
  ["Phòng đấu thời gian thực", 30], ["Tải ảnh lên", 28],
  ["Gợi ý cá nhân hoá (Neo4j)", 28], ["Flashcard và lặp lại ngắt quãng", 25],
  ["Thông báo và nhắc ôn", 24], ["Bảng xếp hạng theo mùa", 24],
  ["Gamification", 21], ["Trợ lý học tập", 20],
  ["Quản trị hệ thống", 19], ["Thống kê và báo cáo", 17],
  ["Hồ sơ người dùng", 16], ["Khởi động ứng dụng", 1],
];

/* ────────────────────────────── kế hoạch kiểm thử ────────────────────────────── */

const p = (text, o = {}) =>
  new Paragraph({
    spacing: { line: 360, after: 120 },
    alignment: o.giua ? AlignmentType.CENTER : AlignmentType.JUSTIFIED,
    children: [new TextRun({ text, font: FONT, size: o.co ?? SIZE, bold: o.dam, italics: o.nghieng })],
  });

const h = (text, cap) =>
  new Paragraph({
    heading: cap === 1 ? HeadingLevel.HEADING_1 : HeadingLevel.HEADING_2,
    spacing: { before: 240, after: 120, line: 360 },
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
      children: [new Paragraph({ spacing: { line: 276 }, children: [new TextRun({ text: String(t), font: FONT, size: 24, bold: dam })] })],
    });
  const cot = rong ?? Array(dauMuc.length).fill(Math.floor(CONTENT_WIDTH / dauMuc.length));
  const rows = [new TableRow({ tableHeader: true, children: dauMuc.map((t) => o(t, true, "E7EEF6")) })];
  for (const hg of hang) rows.push(new TableRow({ children: hg.map((t) => o(t, false)) }));
  return new Table({ width: { size: CONTENT_WIDTH, type: WidthType.DXA }, columnWidths: cot, rows });
}

function keHoach() {
  const noi = [];
  noi.push(p("TRƯỜNG ĐẠI HỌC CÔNG NGHIỆP HÀ NỘI", { giua: true, dam: true }));
  noi.push(p("KHOA CÔNG NGHỆ THÔNG TIN", { giua: true, dam: true }));
  noi.push(p(""));
  noi.push(p("KẾ HOẠCH KIỂM THỬ", { giua: true, dam: true, co: 40 }));
  noi.push(p("Ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo", { giua: true, co: 30 }));
  noi.push(p(""));
  noi.push(p("Sinh viên thực hiện: Nguyễn Khắc Minh Đức — MSV 2022601585", { giua: true }));
  noi.push(p("Giảng viên hướng dẫn: ThS. Nguyễn Đức Lưu", { giua: true }));
  noi.push(p("Hà Nội, 09/2026", { giua: true, nghieng: true }));
  noi.push(new Paragraph({ children: [new PageBreak()] }));

  noi.push(h("1. Tổng quan", 1));

  noi.push(h("1.1. Giới thiệu chung", 2));
  noi.push(p("Tài liệu trình bày kế hoạch kiểm thử cho hệ thống Quiz/Trivia tích hợp trí tuệ nhân tạo, gồm phạm vi kiểm thử, chiến lược áp dụng cho từng tầng, tiêu chí chấp nhận sản phẩm và danh mục sản phẩm bàn giao. Tài liệu đi kèm bảng ca kiểm thử Test_Case_QuizAI.xlsx, nơi ghi chi tiết dữ liệu và kết quả của từng ca."));

  noi.push(h("1.2. Các tài liệu liên quan", 2));
  noi.push(bang(["Tài liệu", "Nội dung tham chiếu"], [
    ["Báo cáo đồ án tốt nghiệp, mục 3.4", "Kết quả kiểm thử chức năng và các lỗi bộ kiểm thử không phát hiện được"],
    ["Báo cáo đồ án tốt nghiệp, mục 3.5", "Số liệu đo hiệu năng phòng đấu thời gian thực"],
    ["Báo cáo đồ án tốt nghiệp, mục 3.6", "Số liệu đo độ chính xác của các chức năng AI"],
    ["docs/features/ (16 tệp)", "Đặc tả yêu cầu chức năng, nguồn để xây dựng ca kiểm thử"],
    ["docs/security.md", "Yêu cầu bảo mật, nguồn cho nhóm ca kiểm thử phân quyền"],
  ], [2800, CONTENT_WIDTH - 2800]));

  noi.push(h("1.3. Giới thiệu chung về dự án", 2));
  noi.push(p("Hệ thống là ứng dụng web cho phép người học làm quiz cá nhân và thi đấu nhiều người theo thời gian thực, đồng thời tích hợp trí tuệ nhân tạo tạo sinh cho ba chức năng: sinh câu hỏi từ học liệu, chấm và giải thích câu trả lời ngắn, và trợ lý học tập trả lời bám học liệu. Hành vi làm bài được phân tích trên cơ sở dữ liệu đồ thị để gợi ý quiz và lộ trình ôn tập."));
  noi.push(p("Phần máy chủ dùng Java 21 với Spring Boot 3.5; phần giao diện dùng React 19, TypeScript và Vite 8. Dữ liệu lưu trên PostgreSQL 16 kèm pgvector, Neo4j 5 và Redis 7. Hệ thống gồm 16 nhóm chức năng với 87 yêu cầu chức năng."));

  noi.push(h("1.4. Phạm vi kiểm thử", 2));
  noi.push(p("Nằm trong phạm vi:"));
  noi.push(bang(["Nhóm", "Nội dung kiểm thử"], [
    ["Chức năng", "16 nhóm chức năng, kiểm ở cả tầng dịch vụ, tầng truy cập dữ liệu, tầng điều khiển và giao diện"],
    ["Phân quyền", "Bốn tác nhân Khách, Người học, Người tạo nội dung, Quản trị viên; kiểm cả quyền theo vai trò lẫn quyền sở hữu tài nguyên"],
    ["Thời gian thực", "Phòng đấu qua STOMP trên WebSocket, đồng bộ nhiều tiến trình qua Redis Pub/Sub"],
    ["Tích hợp AI", "Bộ kiểm cấu trúc kết quả mô hình, cơ chế dự phòng giữa hai nhà cung cấp, hạn mức, chống tiêm chỉ thị"],
    ["Hiệu năng", "Độ trễ phát câu hỏi theo số người chơi trong phòng, từ 10 đến 200 người"],
    ["Độ chính xác AI", "Sai lệch điểm khi chấm tự luận, tỉ lệ câu sinh đúng cấu trúc, khả năng bám nguồn của trợ lý"],
  ], [2200, CONTENT_WIDTH - 2200]));
  noi.push(p(""));
  noi.push(p("Nằm ngoài phạm vi:"));
  noi.push(bang(["Nội dung", "Lý do"], [
    ["Kiểm thử trên nhiều trình duyệt và thiết bị di động", "Sản phẩm hướng tới trình duyệt máy tính; giao diện di động chưa nằm trong mục tiêu của đồ án"],
    ["Kiểm thử xâm nhập theo chuẩn công nghiệp", "Vượt quá phạm vi một đồ án tốt nghiệp; đồ án chỉ kiểm các rủi ro theo OWASP Top 10 ở mức mã nguồn"],
    ["Kiểm thử chịu tải trên hạ tầng nhiều máy chủ có độ trễ mạng thật", "Không có hạ tầng; phép đo thực hiện trên một máy đơn và đã ghi rõ giới hạn này trong báo cáo"],
    ["Đánh giá chất lượng sư phạm của câu hỏi do AI sinh", "Cần chuyên gia môn học đánh giá; đây cũng là lý do hệ thống buộc người tạo nội dung duyệt từng câu"],
  ], [3400, CONTENT_WIDTH - 3400]));

  noi.push(h("1.5. Các rủi ro", 2));
  noi.push(bang(["Rủi ro", "Ảnh hưởng", "Cách xử lý"], [
    ["Hạn mức gói miễn phí của nhà cung cấp mô hình làm lời gọi thất bại giữa lượt đo", "Kết quả đo phản ánh lỗi hạn mức chứ không phản ánh chất lượng mô hình", "Giãn nhịp 70 giây mỗi lượt gọi; bài nào gọi thất bại thì loại khỏi thống kê thay vì tính không điểm"],
    ["Kiểm thử tích hợp phụ thuộc ba cơ sở dữ liệu ngoài", "Môi trường lệch nhau giữa các lần chạy làm kết quả không tái lập được", "Dùng Testcontainers dựng PostgreSQL thật kèm pgvector cho mỗi lần chạy"],
    ["Đặc tả thay đổi khi hệ thống lớn lên, phép kiểm cũ vẫn xanh", "Phép kiểm khẳng định đúng một hành vi nay đã sai", "Rà lại phép kiểm liên quan mỗi lần đổi luật nghiệp vụ; ghi nhận trường hợp đã gặp trong mục 3.4.4 của báo cáo"],
    ["Một số lỗi chỉ lộ khi có người dùng thật", "Bộ kiểm thử toàn đạt nhưng sản phẩm vẫn lỗi", "Bổ sung bước chạy thật và xác nhận trên trình duyệt thành bước bắt buộc, ngang hàng với chạy kiểm thử tự động"],
  ], [2600, 2000, CONTENT_WIDTH - 4600]));

  noi.push(h("2. Các tiêu chí chấp nhận sản phẩm", 1));
  noi.push(bang(["Tiêu chí", "Ngưỡng", "Kết quả"], [
    ["Toàn bộ phép kiểm tự động ở máy chủ phải đạt", "606/606", "Đạt"],
    ["Toàn bộ phép kiểm tự động ở giao diện phải đạt", "128/128", "Đạt"],
    ["Không còn yêu cầu chức năng mức bắt buộc nào chưa hiện thực", "0 yêu cầu", "Đạt"],
    ["Độ trễ phát câu hỏi ở mức 100 người mỗi phòng", "P95 dưới 500 ms", "Đạt — 216 ms"],
    ["Không mất sự kiện ở mọi mức tải đã thử", "0 sự kiện mất", "Đạt"],
    ["Sai lệch điểm trung bình khi AI chấm tự luận", "Dưới 1,0 trên thang 10", "Đạt — 0,13"],
    ["Câu hỏi AI sinh đúng chuẩn cấu trúc", "Từ 90% trở lên", "Đạt — 10/10"],
    ["Bài tấn công tiêm chỉ thị bị chặn", "100%", "Đạt — 2/2"],
  ], [4200, 2200, CONTENT_WIDTH - 6400]));

  noi.push(h("3. Chiến lược kiểm thử", 1));
  noi.push(p("Kiểm thử tổ chức theo tháp kiểm thử: nhiều phép kiểm ở tầng thấp chạy nhanh, ít phép kiểm ở tầng cao nhưng phủ đường đi thật của dữ liệu."));
  noi.push(bang(["Tầng", "Công cụ", "Phạm vi kiểm"], [
    ["Đơn vị — dịch vụ", "JUnit 5, Mockito", "Logic nghiệp vụ thuần: chấm điểm, tính điểm theo tốc độ, lịch lặp lại ngắt quãng, quy tắc phân hạng"],
    ["Truy cập dữ liệu", "@DataJpaTest, Testcontainers", "Truy vấn JPA và truy vấn vector trên PostgreSQL thật có pgvector"],
    ["Điều khiển", "@WebMvcTest, MockMvc", "Ràng buộc dữ liệu vào, mã trạng thái trả về, khuôn dạng lỗi"],
    ["Bảo mật", "Spring Security Test", "Phân quyền theo vai trò kết hợp kiểm quyền sở hữu tài nguyên"],
    ["Tích hợp", "@SpringBootTest, Testcontainers", "Luồng xuyên tầng: đăng nhập, làm bài, sinh đề bằng AI, đồng bộ đồ thị"],
    ["Thời gian thực", "STOMP client trong phép kiểm", "Vòng đời phòng đấu, phát tán sự kiện qua Redis giữa hai tiến trình"],
    ["Giao diện", "Vitest, Testing Library", "Kết xuất thành phần, luồng biểu mẫu, trạng thái rỗng và trạng thái lỗi"],
    ["Thủ công", "Trình duyệt thật", "31 ca ở bảng ca kiểm thử; cùng bộ quét tự động mở toàn bộ trang bằng bốn vai trò"],
  ], [2200, 2400, CONTENT_WIDTH - 4600]));
  noi.push(p(""));
  noi.push(p("Kiểm thử thủ công không phải phần bù cho kiểm thử tự động mà nhắm vào đúng loại lỗi tự động khó bắt: lỗi chỉ xuất hiện khi mở đúng trang bằng đúng vai trò, và lỗi mà phép kiểm vẫn xanh vì nó khẳng định chính hành vi đã sai."));

  noi.push(h("4. Lịch trình công việc", 1));
  noi.push(bang(["Giai đoạn", "Thời gian", "Nội dung"], [
    ["Kiểm thử theo lát cắt", "27/07 – 18/08/2026", "Mỗi lát cắt dọc hoàn thành thì viết và chạy phép kiểm cho lát cắt đó trước khi sang chức năng kế tiếp"],
    ["Đo hiệu năng thời gian thực", "08/08/2026", "Đo độ trễ phát câu hỏi ở sáu mức tải từ 10 đến 200 người chơi"],
    ["Đo độ chính xác AI", "08/08 và 14/08/2026", "Đo lần đầu, phát hiện lỗi truy hồi làm số liệu không dùng được, sửa rồi đo lại"],
    ["Đo đường dự phòng nhà cung cấp", "20/08/2026", "Sinh đề qua nhà cung cấp dự phòng, đối chiếu độ trễ hai bên"],
    ["Kiểm thử hồi quy", "09/09/2026", "Chạy lại toàn bộ; quét tự động 38 trang bằng bốn vai trò"],
  ], [2600, 2200, CONTENT_WIDTH - 4800]));

  noi.push(h("5. Các sản phẩm bàn giao", 1));
  noi.push(bang(["Sản phẩm", "Vị trí"], [
    ["Kế hoạch kiểm thử (tài liệu này)", "bao-cao-datn/Testcase+TestPlan/Test_Plan_QuizAI.docx"],
    ["Bảng ca kiểm thử", "bao-cao-datn/Testcase+TestPlan/Test_Case_QuizAI.xlsx"],
    ["Mã nguồn phép kiểm tự động máy chủ", "backend/src/test/ — 54 lớp"],
    ["Mã nguồn phép kiểm tự động giao diện", "frontend/src/ — 21 tệp .test.tsx"],
    ["Kịch bản đo hiệu năng và độ chính xác AI", "docs/bao-cao/kich-ban-do/"],
    ["Số liệu đo đã ghi nhận", "docs/bao-cao/so-lieu-3.5-hieu-nang-realtime.md · so-lieu-3.6-do-chinh-xac-ai.md"],
    ["Bộ quét lỗi giao diện tự động", "scripts/kiem-tra-web.mjs"],
  ], [3600, CONTENT_WIDTH - 3600]));

  return new Document({
    styles: { default: { document: { run: { font: FONT, size: SIZE } } } },
    sections: [{
      properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1134, right: 1134, bottom: 1134, left: 1701 } } },
      children: noi,
    }],
  });
}

/* ────────────────────────────── bảng ca kiểm thử ────────────────────────────── */

async function caKiemThu() {
  const wb = new ExcelJS.Workbook();
  wb.creator = "Nguyen Khac Minh Duc - 2022601585";
  wb.created = new Date();

  const dauMuc = (ws, cot) => {
    ws.columns = cot;
    const hg = ws.getRow(1);
    hg.font = { bold: true, name: "Calibri", size: 11 };
    hg.fill = { type: "pattern", pattern: "solid", fgColor: { argb: "FFE7EEF6" } };
    hg.alignment = { vertical: "middle", horizontal: "center", wrapText: true };
    hg.height = 28;
    ws.views = [{ state: "frozen", ySplit: 1 }];
    ws.autoFilter = { from: { row: 1, column: 1 }, to: { row: 1, column: cot.length } };
  };

  /* Sheet 1 — ca kiểm thử chức năng */
  const s1 = wb.addWorksheet("Ca kiem thu chuc nang");
  dauMuc(s1, [
    { header: "Mã ca", key: "ma", width: 10 },
    { header: "Chức năng", key: "cn", width: 20 },
    { header: "Kịch bản kiểm thử", key: "kb", width: 42 },
    { header: "Dữ liệu kiểm thử", key: "dl", width: 34 },
    { header: "Kết quả mong đợi", key: "kq", width: 46 },
    { header: "Kết quả thực tế", key: "tt", width: 16 },
  ]);
  CA.forEach(([cn, kb, dl, kq], i) => {
    const r = s1.addRow({ ma: `TC-${String(i + 1).padStart(2, "0")}`, cn, kb, dl, kq, tt: "Đạt" });
    r.alignment = { vertical: "top", wrapText: true };
    r.getCell("tt").alignment = { vertical: "middle", horizontal: "center" };
    r.getCell("tt").font = { bold: true, color: { argb: "FF1B7F4B" } };
  });

  /* Sheet 2 — kiểm thử tự động */
  const s2 = wb.addWorksheet("Kiem thu tu dong");
  dauMuc(s2, [
    { header: "Nhóm chức năng", key: "nhom", width: 44 },
    { header: "Số phép kiểm", key: "so", width: 16 },
    { header: "Đạt", key: "dat", width: 10 },
    { header: "Hỏng", key: "hong", width: 10 },
  ]);
  for (const [nhom, so] of PHAN_BO) {
    const r = s2.addRow({ nhom, so, dat: so, hong: 0 });
    r.alignment = { vertical: "middle" };
    ["so", "dat", "hong"].forEach((k) => (r.getCell(k).alignment = { horizontal: "center" }));
  }
  const tong = PHAN_BO.reduce((a, b) => a + b[1], 0);
  const rt = s2.addRow({ nhom: "Tổng cộng — máy chủ (54 lớp kiểm thử)", so: tong, dat: tong, hong: 0 });
  rt.font = { bold: true };
  ["so", "dat", "hong"].forEach((k) => (rt.getCell(k).alignment = { horizontal: "center" }));
  const rf = s2.addRow({ nhom: "Giao diện — Vitest (21 tệp kiểm thử)", so: 128, dat: 128, hong: 0 });
  rf.font = { bold: true };
  ["so", "dat", "hong"].forEach((k) => (rf.getCell(k).alignment = { horizontal: "center" }));

  s2.addRow({});
  const ghi = s2.addRow({ nhom: "Số liệu lấy từ lượt chạy ./mvnw test và npm test ngày 09/09/2026, cả hai đều BUILD SUCCESS." });
  ghi.font = { italic: true, size: 10 };

  const tep = path.join(RA, "Test_Case_QuizAI.xlsx");
  await wb.xlsx.writeFile(tep);
  return tep;
}

/* ────────────────────────────── chạy ────────────────────────────── */

(async () => {
  fs.mkdirSync(RA, { recursive: true });

  const tepPlan = path.join(RA, "Test_Plan_QuizAI.docx");
  fs.writeFileSync(tepPlan, await Packer.toBuffer(keHoach()));
  console.log(`OK -> ${path.basename(tepPlan)}  ${fs.statSync(tepPlan).size} bytes`);

  const tepCase = await caKiemThu();
  console.log(`OK -> ${path.basename(tepCase)}  ${fs.statSync(tepCase).size} bytes  (${CA.length} ca + ${PHAN_BO.length} nhóm)`);
})().catch((e) => {
  console.error("FAIL:", e.message);
  process.exit(1);
});
