/* Sinh tài liệu tóm tắt nghiệp vụ + WebSocket -> ../TomTat-NghiepVu-WebSocket.docx
 *
 * Chạy:  cd bao-cao-datn/build && node gen-nghiepvu.js
 *
 * ## Tài liệu này dùng để làm gì
 * Hai câu hỏi khác nhau mà hội đồng hay hỏi sau khi trình bày: "hệ thống làm những gì" (nghiệp vụ) và
 * "phòng đấu chạy bằng cái gì" (WebSocket). Tài liệu gom cả hai, để mở ra là trả lời được ngay.
 *
 * ## Nguyên tắc
 * Mọi con số, tên đích đến STOMP, công thức tính điểm đều trích từ mã nguồn thật — SpeedScorer.java,
 * WebSocketConfig.java, GameEventType.java — nên nói ra là kiểm chứng được. Chỗ nào là quyết định có
 * đánh đổi thì ghi cả mặt được lẫn mặt mất.
 */
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, WidthType, HeadingLevel, BorderStyle, ShadingType,
} = require("docx");

const OUT = path.join(__dirname, "..", "TomTat-NghiepVu-WebSocket.docx");
const FONT = "Times New Roman";
const MONO = "Consolas";
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
    spacing: { before: cap === 1 ? 320 : 240, after: 120, line: 340 },
    children: [new TextRun({ text, font: FONT, size: cap === 1 ? 30 : 28, bold: true })],
  });

const g = (text) =>
  new Paragraph({
    spacing: { line: 320, after: 80 },
    alignment: AlignmentType.JUSTIFIED,
    indent: { left: 340, hanging: 200 },
    children: [new TextRun({ text: "— " + text, font: FONT, size: SIZE })],
  });

const ma = (dong) =>
  dong.split("\n").map((d, i, a) =>
    new Paragraph({
      spacing: { line: 260, before: i === 0 ? 100 : 0, after: i === a.length - 1 ? 140 : 0 },
      shading: { fill: "F3F4F6", type: ShadingType.CLEAR, color: "auto" },
      indent: { left: 240 },
      children: [new TextRun({ text: d || " ", font: MONO, size: 22 })],
    }));

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
      children: String(t).split("\n").map((d) =>
        new Paragraph({ spacing: { line: 260 }, children: [new TextRun({ text: d, font: FONT, size: 24, bold: dam })] })),
    });
  const cot = rong ?? Array(dauMuc.length).fill(Math.floor(CW / dauMuc.length));
  const rows = [new TableRow({ tableHeader: true, children: dauMuc.map((t) => o(t, true, "E7EEF6")) })];
  for (const hg of hang) rows.push(new TableRow({ children: hg.map((t) => o(t, false)) }));
  return new Table({ width: { size: CW, type: WidthType.DXA }, columnWidths: cot, rows });
}

const noi = [];
const HOI = [3500, CW - 3500];

noi.push(p("TÓM TẮT NGHIỆP VỤ VÀ KIẾN THỨC WEBSOCKET", { giua: true, dam: true, co: 34 }));
noi.push(p("Đồ án Xây dựng ứng dụng Quiz/Trivia tích hợp trí tuệ nhân tạo", { giua: true, co: 28 }));
noi.push(p("Nguyễn Khắc Minh Đức — 2022601585 — GVHD: ThS. Nguyễn Đức Lưu", { giua: true, nghieng: true, co: 24 }));
noi.push(p(
  "Tài liệu mở ra là trả lời được. Phần I trả lời câu \"hệ thống làm những gì, luồng ra sao\"; phần II " +
  "trả lời câu \"phòng đấu chạy bằng công nghệ gì\"; phần III là bảng câu hỏi kèm câu trả lời có căn cứ. " +
  "Mọi tên đích đến, công thức và con số đều trích từ mã nguồn thật."));

/* ═══════════════ PHẦN I — NGHIỆP VỤ ═══════════════ */
noi.push(h("Phần I — Tóm tắt nghiệp vụ"));

noi.push(h("1. Bốn tác nhân và ranh giới quyền", 2));
noi.push(bang(["Tác nhân", "Làm được gì"], [
  ["Khách (chưa đăng nhập)", "CHỈ xem danh sách và giới thiệu quiz công khai. Không làm bài, không xem nội dung câu hỏi — tránh lộ đề"],
  ["Người học (LEARNER)", "Làm bài, vào phòng đấu, hỏi trợ lý, nhận gợi ý và lộ trình, ôn thẻ ghi nhớ, vào lớp học"],
  ["Người tạo nội dung (CREATOR)", "Toàn bộ quyền của người học, cộng thêm: quản lý quiz và ngân hàng câu hỏi, nạp học liệu, sinh đề bằng AI, mở lớp và giao bài"],
  ["Quản trị viên (ADMIN)", "Toàn bộ quyền của người tạo nội dung, cộng thêm: quản lý tài khoản, danh mục, giám sát chi phí AI, rà soát tính toàn vẹn"],
], [2600, CW - 2600]));
noi.push(p(
  "Ba vai trò đã đăng nhập có quan hệ tổng quát hoá: CREATOR bao hàm LEARNER, ADMIN bao hàm CREATOR. " +
  "Nhờ vậy một người vừa tạo nội dung vừa làm bài bình thường, không cần hai tài khoản."));
noi.push(p(
  "Ngoại lệ DUY NHẤT của nguyên tắc \"muốn tạo dữ liệu học tập thì phải có tài khoản\" là phòng đấu: " +
  "khách biết mã PIN VÀ được chủ phòng bật tuỳ chọn cho khách thì vào chơi được, vì tình huống thực tế " +
  "là quét mã QR trong lớp, không phải ai cũng có tài khoản. Đổi lại, khách dùng khoá phiên riêng chỉ " +
  "mở đúng một phòng, và dữ liệu của họ chỉ sống trong một ván: không lịch sử, không thống kê, không " +
  "lên đồ thị gợi ý.", { dam: true }));

noi.push(h("2. Mười sáu nhóm chức năng", 2));
noi.push(bang(["Nhóm", "Nội dung chính"], [
  ["Xác thực và phân quyền", "Đăng ký, đăng nhập email và Google, đặt lại mật khẩu bằng OTP, JWT ngắn hạn kèm refresh token xoay vòng, đăng xuất một hoặc mọi thiết bị"],
  ["Quản lý quiz và ngân hàng câu hỏi", "Năm loại câu hỏi, danh mục, độ khó, thời lượng, chế độ công khai hoặc riêng tư, ảnh bìa"],
  ["Làm bài cá nhân", "Hai chế độ luyện tập và tính giờ, tự động lưu, chấm tự động, xem lại kèm lời giải thích"],
  ["Phòng đấu thời gian thực", "Mã PIN sáu số và mã QR, phát câu hỏi đồng thời, điểm theo tốc độ, bảng xếp hạng trực tiếp"],
  ["Sinh đề bằng RAG", "Nạp học liệu, chia đoạn, dựng vector, sinh câu hỏi bám tài liệu, người duyệt mới vào ngân hàng"],
  ["Chấm và giải thích câu tự luận", "Chấm theo đáp án mẫu và tiêu chí, trả điểm kèm nhận xét và gợi ý cải thiện"],
  ["Trợ lý học tập", "Hỏi đáp bám học liệu, trả lời theo luồng, kèm trích dẫn nguồn"],
  ["Gợi ý cá nhân hoá bằng Neo4j", "Quiz theo chủ đề còn yếu, theo người học tương tự, và lộ trình thứ tự chủ đề nên ôn"],
  ["Thống kê và báo cáo", "Tiến độ cá nhân theo chủ đề, thống kê theo quiz cho chủ sở hữu"],
  ["Quản trị hệ thống", "Tài khoản, danh mục, cấu hình nhà cung cấp AI, nhật ký và chi phí gọi mô hình"],
  ["Thẻ ghi nhớ", "Bộ thẻ và phiên ôn theo thuật toán lặp lại ngắt quãng SM-2"],
  ["Chống gian lận khi thi", "Thu tín hiệu hành vi trong chế độ thi, tính điểm rủi ro kèm lý do"],
  ["Trò chơi hoá", "Điểm kinh nghiệm, cấp độ, huy hiệu, chuỗi ngày học, thử thách hằng ngày"],
  ["Lớp học và giao bài", "Mã lớp sáu ký tự, thành viên, bài tập kèm hạn nộp, theo dõi tình hình nộp"],
  ["Bảng xếp hạng theo mùa", "Mùa giải có thời hạn, bảng xếp hạng chốt lại khi mùa kết thúc"],
  ["Thông báo nhắc ôn tập", "Thông báo theo loại, có khoá chống trùng, người dùng tắt được từng loại"],
], [3000, CW - 3000]));

noi.push(h("3. Chín quy tắc nghiệp vụ hay bị hỏi", 2));
noi.push(p("Đây là những quyết định có lý do, không phải chi tiết kỹ thuật vụn vặt. Nắm chín điều này là trả lời được phần lớn câu hỏi về nghiệp vụ."));
noi.push(bang(["Quy tắc", "Vì sao"], [
  ["Đề được CHỐT tại thời điểm bắt đầu làm bài",
   "Hệ thống sinh sẵn toàn bộ câu trả lời trống ngay khi bắt đầu. Nhờ vậy người tạo nội dung sửa quiz giữa chừng cũng không làm đổi đề của người đang làm dở"],
  ["Điểm người chấm tay LUÔN thắng điểm AI",
   "Nếu AI chấm xong muộn hơn, kết quả của nó bị bỏ qua. Con người giữ quyền kết luận cuối"],
  ["Điểm AI trả về luôn bị ràng buộc trong miền 0 tới điểm tối đa của câu",
   "Đây là hàng rào cuối chống tiêm chỉ thị: dù mô hình nghe theo câu lệnh người học chèn vào bài, điểm vẫn không vượt được trần thật"],
  ["Câu hỏi AI sinh ra là BẢN NHÁP",
   "Chỉ vào ngân hàng khi người tạo nội dung tích chọn và bấm lưu — nguyên tắc con người ở vòng cuối"],
  ["Trợ lý nói \"không biết\" thay vì suy đoán",
   "Khi không còn đoạn học liệu nào đủ liên quan, prompt nói rõ là không có tài liệu. Một câu trả lời sai nhưng trôi chảy nguy hiểm hơn việc không trả lời"],
  ["Chống gian lận KHÔNG tự kết luận ai gian lận",
   "Tín hiệu thu từ trình duyệt nên chặn được và giả mạo được. Hệ thống chỉ tính điểm rủi ro kèm lý do; người phụ trách kết luận. Lượt luyện tập không thu tín hiệu nào"],
  ["Truy cập tài nguyên người khác trả về 404, không phải 403",
   "403 là thừa nhận tài nguyên đó tồn tại. 404 không tiết lộ gì"],
  ["Đổi mật khẩu thu hồi phiên trên MỌI thiết bị",
   "Người dùng đổi mật khẩu thường tin rằng mình vừa cắt hết truy cập — hệ thống phải làm đúng điều đó"],
  ["Chưa đủ dữ liệu thì KHÔNG gợi ý bừa",
   "API gợi ý trả danh sách rỗng kèm hướng dẫn làm một bài, thay vì đưa ra lời khuyên không có căn cứ"],
], [3400, CW - 3400]));

noi.push(h("4. Bốn luồng nghiệp vụ chính", 2));
noi.push(p("Làm bài cá nhân", { dam: true }));
noi.push(g("Chọn quiz, chọn chế độ luyện tập hoặc tính giờ · hệ thống tạo lượt làm bài và sinh sẵn các câu trả lời trống để chốt đề · mỗi lựa chọn được lưu ngay · nộp bài hoặc hết giờ tự chuyển trạng thái · chấm tự động các câu có đáp án xác định, câu tự luận đánh dấu chờ AI · phát sự kiện đồng bộ sang Neo4j."));
noi.push(p("Phòng đấu thời gian thực", { dam: true }));
noi.push(g("Chủ phòng mở phòng từ một quiz, nhận mã PIN sáu số và mã QR · người chơi vào bằng PIN hoặc quét QR, chọn biệt danh và ảnh đại diện · chủ phòng bắt đầu ván · máy chủ phát từng câu hỏi đồng thời tới mọi người · mỗi đáp án được chấm và trả riêng cho người gửi · hết câu thì phát bảng xếp hạng · kết thúc ván mới ghi kết quả xuống cơ sở dữ liệu."));
noi.push(p("Sinh đề bằng AI từ học liệu", { dam: true }));
noi.push(g("Người tạo nội dung nạp tài liệu · hệ thống bóc tách, chia đoạn, dựng vector, chuyển trạng thái sang sẵn sàng · yêu cầu sinh đề trả về mã công việc ngay vì tác vụ chạy nền · công việc truy hồi đoạn liên quan trong phạm vi được phép đọc, dựng prompt, gọi mô hình, kiểm chứng cấu trúc JSON · câu hỏi nháp hiện ra để người tạo nội dung duyệt."));
noi.push(p("Gợi ý và lộ trình học", { dam: true }));
noi.push(g("Sau mỗi lượt nộp, hệ thống phát sự kiện ở pha sau khi giao dịch được ghi nhận · công việc nền đồng bộ hành vi sang Neo4j · chạy lần thứ hai sau khi AI chấm xong câu tự luận vì lúc mới nộp những câu đó còn chưa có điểm · ba truy vấn Cypher trả về quiz theo chủ đề còn yếu, quiz theo người học tương tự, và thứ tự chủ đề nên ôn."));

/* ═══════════════ PHẦN II — WEBSOCKET ═══════════════ */
noi.push(h("Phần II — WebSocket trong phòng đấu"));

noi.push(h("1. Vì sao phải dùng WebSocket", 2));
noi.push(p(
  "Đây là câu trả lời gốc, mọi câu hỏi khác về phòng đấu đều quay về đây. Ràng buộc nghiệp vụ: khi máy " +
  "chủ chuyển sang câu tiếp theo, MỌI NGƯỜI CHƠI PHẢI NHẬN CÙNG LÚC."));
noi.push(p(
  "Nếu để trình duyệt hỏi lại theo chu kỳ (polling), mỗi người nhận câu hỏi lệch nhau trung bình nửa " +
  "chu kỳ. Mà điểm lại tính theo tốc độ trả lời, nên độ lệch đó biến thành chênh lệch điểm — tức là " +
  "BẤT CÔNG, không chỉ là chậm. Đó là lý do độ trễ ở đây là yêu cầu chức năng chứ không phải chỉ tiêu " +
  "kỹ thuật, và cũng là lý do đề tài phải đo hiệu năng chịu tải.", { dam: true }));

noi.push(h("2. WebSocket là gì", 2));
noi.push(g("Bắt đầu bằng một yêu cầu HTTP có tiêu đề nâng cấp (Upgrade). Máy chủ đồng ý thì kết nối TCP đó chuyển sang giao thức WebSocket và được GIỮ MỞ."));
noi.push(g("Sau đó hai chiều đều chủ động gửi được (song công), không cần client hỏi trước. Đây là khác biệt căn bản so với REST."));
noi.push(g("Chi phí mỗi thông điệp rất nhỏ vì không phải lặp lại tiêu đề HTTP cho từng lần gửi."));
noi.push(g("Hệ quả quan trọng: kết nối DÍNH vào đúng một tiến trình máy chủ. Hai người cùng phòng có thể đang nối vào hai tiến trình khác nhau — bài toán được giải ở mục 6."));

noi.push(h("3. STOMP — vì sao không dùng WebSocket trần", 2));
noi.push(p(
  "WebSocket chỉ cho một ống truyền byte, không có khái niệm \"gửi tới ai\" hay \"ai quan tâm chủ đề nào\". " +
  "Dùng trần thì phải tự định nghĩa định dạng thông điệp và tự quản lý danh sách người nhận."));
noi.push(p(
  "STOMP là giao thức nhắn tin đơn giản chạy TRÊN WebSocket, cho sẵn khái niệm đích đến (destination) " +
  "và cơ chế đăng ký theo chủ đề. Hệ thống chỉ cần cho người chơi đăng ký chủ đề của phòng rồi gửi tin " +
  "tới đó."));
noi.push(p("Các đích đến thật trong đồ án:"));
noi.push(bang(["Đích đến", "Dùng để"], [
  ["/topic/room/{mã phòng}", "Phát cho CẢ PHÒNG: câu hỏi mới, bảng xếp hạng, người vào hoặc rời phòng"],
  ["/queue/room/{mã phòng}", "Gửi RIÊNG một người: kết quả đáp án của chính người đó"],
  ["/queue/errors", "Báo lỗi riêng cho một người"],
  ["/app/...", "Tiền tố cho thông điệp client GỬI LÊN máy chủ"],
  ["/user/...", "Tiền tố để máy chủ gửi tới đúng một người dùng"],
], [3000, CW - 3000]));
noi.push(p(
  "Broker dùng loại đơn giản chạy trong bộ nhớ (simple broker), không phải broker ngoài như RabbitMQ — " +
  "RabbitMQ đã bị loại khỏi phạm vi đề tài. Muốn chạy nhiều tiến trình vẫn đồng bộ thì mọi thông điệp " +
  "đi vòng qua Redis Pub/Sub trước."));

noi.push(h("4. SockJS — đường lùi khi WebSocket bị chặn", 2));
noi.push(p(
  "Một số mạng trường học hoặc proxy chặn WebSocket. SockJS tự dò và lùi về long-polling trên HTTP " +
  "thường, nên phòng đấu vẫn chơi được, chỉ chậm hơn. Đây là điểm đáng nói nếu bị hỏi \"mạng trường " +
  "chặn thì sao\"."));

noi.push(h("5. Xác thực tại khung CONNECT, không phải lúc bắt tay", 2));
noi.push(p(
  "Chỗ này hay bị hỏi vì nó trái với thói quen. Lý do: TRÌNH DUYỆT KHÔNG CHO GẮN TIÊU ĐỀ TUỲ Ý vào yêu " +
  "cầu nâng cấp WebSocket — API WebSocket của trình duyệt không có tham số header. Vậy nên không thể " +
  "gửi Authorization như một lời gọi REST bình thường.", { dam: true }));
noi.push(p(
  "Giải pháp: để kết nối bắt tay xong, rồi xác thực ở khung STOMP CONNECT — khung đầu tiên client gửi " +
  "sau khi nối. Lớp chặn StompAuthChannelInterceptor kiểm JWT ở đó, trước khi cho đăng ký bất kỳ kênh " +
  "nào. Khách vãng lai dùng khoá phiên riêng lưu ở Redis, chỉ mở đúng một phòng, không phải JWT nên " +
  "không mở được API nào khác."));

noi.push(h("6. Redis Pub/Sub — chạy nhiều tiến trình", 2));
noi.push(p(
  "Vấn đề: kết nối WebSocket dính vào một tiến trình. Tiến trình A tính xong điểm chỉ đẩy được cho " +
  "client của A; người đang nối vào B không thấy gì."));
noi.push(g("Tiến trình xử lý XUẤT BẢN sự kiện lên một kênh Redis."));
noi.push(g("Mọi tiến trình đều ĐĂNG KÝ kênh đó, nhận được rồi phát tiếp cho client của riêng mình."));
noi.push(g("Đo ở mục 3.5: mỗi sự kiện đi vòng qua Redis tốn khoảng 2 mili giây."));
noi.push(p(
  "Hạn chế phải nói ra: Redis Pub/Sub là at-most-once, ai không đang đăng ký lúc tin phát ra thì mất " +
  "tin. Chấp nhận được ở đây vì sự kiện ván đấu chỉ có giá trị vài giây, và trạng thái phòng vẫn nằm ở " +
  "Redis nên client vào lại thì ĐỌC LẠI TRẠNG THÁI chứ không cần ai phát lại."));

noi.push(h("7. Mười ba loại sự kiện trong một ván", 2));
noi.push(bang(["Nhóm", "Sự kiện"], [
  ["Người chơi", "PLAYER_JOINED · PLAYER_LEFT · PLAYER_READY · PLAYER_AVATAR_CHANGED"],
  ["Diễn biến ván", "GAME_STARTED · QUESTION · QUESTION_CLOSED · GAME_FINISHED"],
  ["Trả lời và điểm", "PLAYER_ANSWERED · ANSWER_RESULT · LEADERBOARD"],
  ["Chống gian lận", "PROCTORING_FLAG · PROCTORING_WARNING"],
], [2200, CW - 2200]));

noi.push(h("8. Công thức tính điểm theo tốc độ", 2));
noi.push(ma(
  "sai hoặc quá hạn  ->  0\n" +
  "đúng              ->  điểm_câu x (500 + 500 x phần_thời_gian_còn_lại)"));
noi.push(p("Ba điều cần nhớ về công thức này:"));
noi.push(g("Trả lời đúng ngay lập tức được điểm_câu × 1000; đúng nhưng sát giờ chót vẫn được điểm_câu × 500."));
noi.push(g("Nghĩa là ĐÚNG LUÔN HƠN SAI, nhanh chỉ là phần thưởng cộng thêm — tránh việc bấm bừa thật nhanh lại lợi hơn suy nghĩ rồi trả lời đúng."));
noi.push(g("Thời gian được đo Ở MÁY CHỦ, từ lúc máy chủ phát câu hỏi tới lúc nhận đáp án — không lấy con số do client gửi lên. Nếu tin client thì người chơi sửa được điểm của mình."));

noi.push(h("9. Mất kết nối giữa ván", 2));
noi.push(p(
  "Trạng thái phòng nằm ở Redis chứ không ở bộ nhớ tiến trình, nên người chơi rớt mạng rồi vào lại sẽ " +
  "đọc lại trạng thái và GIỮ NGUYÊN ĐIỂM. Đây là yêu cầu chức năng, không phải tính năng phụ: mạng " +
  "trong lớp học vốn chập chờn."));

/* ═══════════════ PHẦN III — CÂU HỎI ═══════════════ */
noi.push(h("Phần III — Câu hỏi hội đồng hay đặt"));
noi.push(bang(["Câu hỏi", "Trả lời"], [
  ["Vì sao dùng WebSocket mà không phải gọi API theo chu kỳ?",
   "Vì điểm tính theo tốc độ trả lời. Hỏi lại theo chu kỳ thì mỗi người nhận câu hỏi lệch nhau trung bình nửa chu kỳ, và độ lệch đó biến thành chênh lệch điểm — bất công, không chỉ là chậm."],
  ["WebSocket khác HTTP ở điểm nào?",
   "HTTP là hỏi mới có đáp. WebSocket giữ kết nối mở và hai chiều đều chủ động gửi được, nên máy chủ đẩy câu hỏi xuống mà không cần client hỏi trước."],
  ["Vì sao cần STOMP, dùng WebSocket trần không được à?",
   "Được, nhưng phải tự định nghĩa định dạng thông điệp và tự quản danh sách người nhận. STOMP cho sẵn khái niệm đích đến và đăng ký theo chủ đề: chỉ cần cho người chơi đăng ký /topic/room/{mã} rồi gửi tin tới đó."],
  ["Xác thực WebSocket thế nào?",
   "Ở khung STOMP CONNECT, không phải lúc bắt tay. Vì trình duyệt không cho gắn tiêu đề tuỳ ý vào yêu cầu nâng cấp WebSocket, nên không gửi Authorization như REST được. Lớp chặn kiểm JWT ngay khung đầu tiên, trước khi cho đăng ký kênh nào."],
  ["Mạng trường chặn WebSocket thì sao?",
   "Có SockJS làm đường lùi: tự dò và chuyển sang long-polling trên HTTP thường. Chậm hơn nhưng vẫn chơi được."],
  ["Chạy nhiều máy chủ thì người chơi hai bên có thấy nhau không?",
   "Có, nhờ Redis Pub/Sub. Tiến trình xử lý xuất bản sự kiện lên kênh Redis, mọi tiến trình đăng ký kênh đó nhận được rồi phát tiếp cho client của mình. Đo được khoảng 2 mili giây cho mỗi vòng."],
  ["Người chơi gian lận bằng cách sửa thời gian được không?",
   "Không. Thời gian trả lời đo ở MÁY CHỦ, từ lúc phát câu hỏi tới lúc nhận đáp án, không lấy con số client gửi lên."],
  ["Trả lời thật nhanh nhưng bừa thì có lợi hơn không?",
   "Không. Sai được 0 điểm. Đúng được ít nhất một nửa số điểm tối đa, nhanh chỉ cộng thêm phần còn lại. Đúng luôn hơn sai."],
  ["Rớt mạng giữa ván thì mất điểm à?",
   "Không. Trạng thái phòng nằm ở Redis chứ không ở bộ nhớ tiến trình, nên vào lại là đọc lại trạng thái và giữ nguyên điểm."],
  ["Khách không có tài khoản vào chơi thì quản lý thế nào?",
   "Khách phải biết mã PIN VÀ chủ phòng phải bật tuỳ chọn cho khách. Khách dùng khoá phiên riêng lưu ở Redis, chỉ mở đúng một phòng, không phải JWT nên không mở được API nào khác. Dữ liệu của họ chỉ sống trong một ván."],
  ["Hệ thống chịu được bao nhiêu người trong một phòng?",
   "Đã đo tới 200 người. Ngưỡng dùng được trong thực tế là 100 người mỗi phòng, khi đó P95 là 216 mili giây và không mất sự kiện nào. Phải nói kèm: đo trên một máy đơn, không bao gồm độ trễ mạng thật."],
  ["Vì sao không dùng RabbitMQ hay Kafka cho phần nhắn tin?",
   "Nhu cầu ở đây là phát tán trong vài chục mili giây giữa vài tiến trình, không phải hàng đợi bền có bảo đảm nhận. Thêm một hệ trung gian nữa làm tăng độ trễ và chi phí vận hành mà không giải quyết thêm bài toán nào của đồ án."],
], HOI));

const doc = new Document({
  styles: { default: { document: { run: { font: FONT, size: SIZE } } } },
  sections: [{
    properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1134, right: 1134, bottom: 1134, left: 1701 } } },
    children: noi,
  }],
});

Packer.toBuffer(doc).then((b) => {
  fs.writeFileSync(OUT, b);
  console.log(`OK -> ${path.basename(OUT)}  ${fs.statSync(OUT).size} bytes`);
});
