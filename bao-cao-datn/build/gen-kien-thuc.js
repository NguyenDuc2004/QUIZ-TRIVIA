/* Sinh tài liệu ôn kiến thức RAG · Redis · Neo4j -> ../KienThuc-RAG-Redis-Neo4j.docx
 *
 * Chạy:  cd bao-cao-datn/build && node gen-kien-thuc.js
 *
 * ## Tài liệu này dùng để làm gì
 * Để hiểu đủ sâu mà phản biện, không phải để đọc thuộc. Khác hướng dẫn thuyết trình ở chỗ: hướng dẫn
 * lo phần NÓI GÌ TRONG 15 PHÚT, tài liệu này lo phần TRẢ LỜI ĐƯỢC KHI BỊ HỎI SÂU.
 *
 * ## Nguyên tắc
 * Mọi con số, tên khoá, tên bảng, đoạn truy vấn ở đây đều lấy từ mã nguồn thật của dự án — không phải
 * lý thuyết chung chung chép từ sách. Chỗ nào là đánh đổi có chủ ý thì ghi rõ cả mặt được lẫn mặt mất,
 * vì hội đồng hỏi "vì sao chọn" nhiều hơn hỏi "cái đó là gì".
 */
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  AlignmentType, WidthType, HeadingLevel, BorderStyle, ShadingType,
} = require("docx");

const OUT = path.join(__dirname, "..", "KienThuc-RAG-Redis-Neo4j.docx");
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

/** Gạch đầu dòng. */
const g = (text) =>
  new Paragraph({
    spacing: { line: 320, after: 80 },
    alignment: AlignmentType.JUSTIFIED,
    indent: { left: 340, hanging: 200 },
    children: [new TextRun({ text: "— " + text, font: FONT, size: SIZE })],
  });

/** Khối mã / truy vấn. */
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
const HOI = [3600, CW - 3600];

noi.push(p("TÀI LIỆU ÔN KIẾN THỨC ĐỂ PHẢN BIỆN", { giua: true, dam: true, co: 34 }));
noi.push(p("RAG · Redis · Neo4j — bám theo mã nguồn của đồ án Quiz/Trivia tích hợp AI", { giua: true, co: 28 }));
noi.push(p("Nguyễn Khắc Minh Đức — 2022601585 — GVHD: ThS. Nguyễn Đức Lưu", { giua: true, nghieng: true, co: 24 }));
noi.push(p(
  "Tài liệu này không phải bài nói. Nó trả lời cho câu hỏi: nếu hội đồng đào sâu vào ba công nghệ lõi " +
  "thì cần hiểu tới đâu. Mỗi phần đi theo cùng một mạch — khái niệm là gì, đồ án dùng nó ra sao, con số " +
  "thật là bao nhiêu, và câu hỏi hay gặp cùng câu trả lời có căn cứ. Mọi tên khoá, tham số và đoạn truy " +
  "vấn đều trích từ mã nguồn thật, nên nói ra là kiểm chứng được."));

/* ═══════════════════════ PHẦN I — RAG ═══════════════════════ */
noi.push(h("Phần I — RAG (Retrieval-Augmented Generation)"));

noi.push(h("1. RAG giải quyết bài toán gì", 2));
noi.push(p(
  "Mô hình ngôn ngữ trả lời bằng kiến thức nó học được lúc huấn luyện. Nó không biết giáo trình riêng " +
  "của một lớp, và quan trọng hơn — nó không chỉ ra được câu trả lời dựa vào đâu. Với một hệ thống ôn " +
  "thi, một câu trả lời sai nhưng trôi chảy còn tệ hơn việc không trả lời, vì người học chưa nắm kiến " +
  "thức thì không có cơ sở nào để nghi ngờ."));
noi.push(p("Có ba hướng xử lý, đồ án chọn hướng thứ ba:"));
noi.push(g("Để mô hình tự trả lời — loại, vì không truy vết được về nguồn."));
noi.push(g("Huấn luyện lại (fine-tune) trên tài liệu riêng — loại, vì mỗi lần thêm tài liệu lại phải huấn luyện lại, tốn kém, và mô hình vẫn không nói được câu trả lời dựa vào đoạn nào."));
noi.push(g("Truy hồi rồi mới sinh (RAG) — tìm trong học liệu những đoạn liên quan nhất, đưa chúng vào ngữ cảnh, rồi mới để mô hình viết câu trả lời dựa trên đó. Truy vết được tới từng đoạn."));
noi.push(p(
  "Câu chốt nếu bị hỏi vì sao không fine-tune: fine-tune dạy mô hình một VĂN PHONG hoặc một ĐỊNH DẠNG, " +
  "còn RAG cấp cho nó một NGUỒN. Bài toán ở đây là nguồn, không phải văn phong.", { dam: true }));

noi.push(h("2. Vector nhúng (embedding) — biến chữ thành toạ độ", 2));
noi.push(p(
  "Vector nhúng biến một đoạn văn thành một điểm trong không gian nhiều chiều, sao cho hai đoạn cùng ý " +
  "nghĩa nằm gần nhau kể cả khi dùng từ khác hẳn. Đây là điểm khác căn bản so với tìm kiếm từ khoá: " +
  "chuỗi \"xe hơi\" và \"ô tô\" không khớp nhau một ký tự nào, nhưng hai vector của chúng gần nhau."));
noi.push(bang(["Trong đồ án", "Giá trị"], [
  ["Mô hình sinh vector", "Gemini embedding"],
  ["Số chiều", "768"],
  ["Nơi lưu", "cột embedding vector(768) của bảng material_chunks, phần mở rộng pgvector trên PostgreSQL 16"],
], [3200, CW - 3200]));
noi.push(p(
  "Vì sao 768 chiều: đó là số chiều mô hình trả về, không phải lựa chọn của đồ án. Cần nhớ rằng số chiều " +
  "càng lớn thì vector càng nặng và phép so sánh càng tốn, nên nó là một đánh đổi giữa độ tinh và chi phí."));

noi.push(h("3. Khoảng cách cosine và toán tử <=> của pgvector", 2));
noi.push(p(
  "Khoảng cách cosine đo GÓC giữa hai vector, không quan tâm độ dài của chúng. Đó là lý do nó hợp với " +
  "văn bản: một đoạn dài và một đoạn ngắn cùng nói về một ý vẫn được coi là gần nhau, trong khi khoảng " +
  "cách Euclid sẽ phạt đoạn dài chỉ vì nó dài."));
noi.push(ma("embedding <=> cast(? as vector) as distance\norder by distance\nlimit ?"));
noi.push(p("Ba điều cần thuộc về toán tử này:"));
noi.push(g("<=> là cosine distance của pgvector — CÀNG NHỎ CÀNG GIỐNG. Nhiều người nhầm thành độ tương đồng (càng lớn càng giống)."));
noi.push(g("Giá trị chạy từ 0 (cùng hướng) tới 2 (ngược hướng); 1 nghĩa là vuông góc, tức không liên quan."));
noi.push(g("Đồ án cắt ở ngưỡng 0,75 — trên mức đó thì coi như không liên quan và loại bỏ."));

noi.push(h("4. Chia đoạn (chunking) và phần chồng lấp", 2));
noi.push(p(
  "Không thể nhét cả tài liệu vào ngữ cảnh: cửa sổ ngữ cảnh có hạn, và kể cả khi vừa thì phần liên quan " +
  "cũng bị loãng giữa hàng chục trang không liên quan. Nên tài liệu được cắt thành đoạn, mỗi đoạn sinh " +
  "một vector riêng, và hệ thống chỉ lấy vài đoạn gần nhất."));
noi.push(bang(["Tham số trong TextChunker.java", "Giá trị"], [
  ["DEFAULT_CHUNK_SIZE", "1500 ký tự"],
  ["DEFAULT_OVERLAP", "200 ký tự"],
  ["Cách cắt", "theo ranh giới câu, không cắt giữa câu"],
], [4200, CW - 4200]));
noi.push(p(
  "Vì sao phải chồng lấp 200 ký tự: một ý thường nằm vắt qua ranh giới. Cắt phẳng thì nửa đầu ý nằm ở " +
  "đoạn này, nửa sau ở đoạn kia, và không đoạn nào trả lời trọn câu hỏi. Chồng lấp bảo đảm ít nhất một " +
  "đoạn chứa đủ ý. Cái giá phải trả là kho phình thêm khoảng 13% và một ý có thể xuất hiện ở hai đoạn."));

noi.push(h("5. top-K và ngưỡng — vì sao cần CẢ HAI", 2));
noi.push(p(
  "Đây là chỗ hay bị hỏi và cũng hay bị trả lời sai. Hai cơ chế này giải hai bài toán khác nhau:"));
noi.push(bang(["Cơ chế", "Giá trị", "Kiểm soát điều gì"], [
  ["top-K", "5", "SỐ LƯỢNG — lấy nhiều nhất 5 đoạn gần nhất"],
  ["Ngưỡng khoảng cách", "0,75", "CHẤT LƯỢNG — loại đoạn không đủ liên quan"],
], [2000, 1400, CW - 3400]));
noi.push(p(
  "Vì sao thiếu ngưỡng thì hỏng: truy vấn vector LUÔN trả về đủ K đoạn, kể cả khi không đoạn nào liên " +
  "quan. Trong một kho toàn tài liệu Toán, đoạn \"gần nhất\" với câu hỏi về Lịch sử vẫn là một đoạn " +
  "Toán. Không lọc thì prompt chứa ngữ cảnh sai, và mô hình sẽ cố trả lời dựa trên ngữ cảnh sai đó — " +
  "đúng cái định nghĩa của ảo giác. Khi mọi đoạn đều bị ngưỡng loại, prompt nói rõ là không có tài liệu " +
  "liên quan, để mô hình trả lời \"không biết\" thay vì đoán."));

noi.push(h("6. Lọc quyền TRƯỚC khi xếp hạng — phần quan trọng nhất", 2));
noi.push(p(
  "Nếu chỉ kịp hiểu sâu một chỗ trong cả tài liệu này, hãy chọn chỗ này. Nó vừa là quyết định kỹ thuật " +
  "đáng nói nhất của đồ án, vừa là một lỗi thật đã đo được, và nó nối cả ba mặt: bảo mật, đúng đắn và " +
  "hiệu năng.", { dam: true }));
noi.push(p(
  "Ràng buộc: mỗi người chỉ được truy hồi trong phạm vi tài liệu của chính họ hoặc tài liệu đã được chia " +
  "sẻ. Nghe thì chỉ là một điều kiện WHERE, nhưng THỨ TỰ áp nó quyết định kết quả."));
noi.push(p("Cách viết sai — câu truy vấn phẳng:", { dam: true }));
noi.push(ma("where <điều kiện quyền>\norder by embedding <=> ?\nlimit 5"));
noi.push(p(
  "PostgreSQL nhìn thấy \"order by <toán tử vector> limit n\" liền dùng chỉ mục IVFFlat để lấy đúng n ứng " +
  "viên gần nhất TOÀN KHO, rồi mới lọc quyền trên n dòng đó. Những dòng không được phép đọc bị loại mà " +
  "không có gì bù vào, nên kết quả ít hơn n — và thường rỗng."));
noi.push(p("Đo thật trên dữ liệu của đồ án:", { dam: true }));
noi.push(g("Chỉ mục trả về 2 dòng, cả 2 thuộc tài liệu chưa chia sẻ. Lọc quyền loại sạch."));
noi.push(g("Trợ lý trả lời \"không tìm thấy tài liệu\" trong khi kho có đúng 9 đoạn hợp lệ nói về chính câu hỏi đó."));
noi.push(g("Với ivfflat.probes = 1 (mặc định) ra 0 đoạn; đặt probes = 100 ra 5 đoạn — cùng một câu truy vấn, chỉ khác tham số quét."));
noi.push(p(
  "Điều nguy hiểm nhất không phải con số 0, mà là nó IM LẶNG: không ngoại lệ, không mã lỗi, không cảnh " +
  "báo. Kho vector chỉ đơn giản trông như rỗng. Toàn bộ số liệu đánh giá AI đo trước khi phát hiện lỗi " +
  "này đều phải bỏ và đo lại.", { dam: true }));
noi.push(p("Cách viết đúng — dựng xong tập được phép đọc rồi mới tính khoảng cách:", { dam: true }));
noi.push(ma(
  "with duoc_phep_doc as materialized (\n" +
  "    select c.id, c.material_id, m.title, c.chunk_index, c.content, c.embedding\n" +
  "    from material_chunks c\n" +
  "        join learning_materials m on m.id = c.material_id\n" +
  "    where (m.owner_id = ? or (? and m.shared = true))\n" +
  "      and m.status = 'READY'\n" +
  ")\n" +
  "select id, material_id, title, chunk_index, content,\n" +
  "       embedding <=> cast(? as vector) as distance\n" +
  "from duoc_phep_doc\n" +
  "order by distance\n" +
  "limit ?"));
noi.push(p(
  "Từ khoá materialized buộc PostgreSQL vật chất hoá tập được phép đọc trước, rồi mới tính khoảng cách " +
  "trên đúng tập đó. Đây là tìm kiếm CHÍNH XÁC (exact), không phải xấp xỉ."));
noi.push(p("Đánh đổi — phải nói ra chứ đừng giấu:", { dam: true }));
noi.push(g("Mất: không dùng được chỉ mục xấp xỉ, nên phải quét tuần tự trên tập đã lọc quyền."));
noi.push(g("Được: không bao giờ bỏ sót. Ở quy mô vài trăm tới vài nghìn đoạn của đồ án, quét thẳng còn nhanh hơn dựng và duyệt cây."));
noi.push(g("Khi nào phải đổi: kho vượt cỡ vài chục nghìn đoạn thì mới cần ANN, và lúc đó phải bật kèm iterative scan của pgvector 0.8 (hnsw.iterative_scan) để chỉ mục tự quét thêm khi bộ lọc quyền loại bớt ứng viên — chứ không quay lại cách phẳng cũ."));
noi.push(p("Bốn thuật ngữ cần biết nếu hội đồng dùng tới:"));
noi.push(bang(["Thuật ngữ", "Nghĩa"], [
  ["ANN — approximate nearest neighbor", "Tìm láng giềng gần nhất theo lối xấp xỉ: nhanh hơn nhiều, đổi lại có thể bỏ sót"],
  ["IVFFlat", "Một kiểu chỉ mục ANN của pgvector: chia kho thành cụm, chỉ quét vài cụm gần nhất. Số cụm quét đặt bằng ivfflat.probes"],
  ["HNSW", "Kiểu chỉ mục ANN khác, dựng đồ thị nhiều tầng; thường chính xác hơn IVFFlat nhưng tốn bộ nhớ hơn"],
  ["Recall", "Tỉ lệ kết quả đúng mà chỉ mục thực sự tìm được. Chỉ mục xấp xỉ luôn có recall dưới 100%"],
], [3000, CW - 3000]));

noi.push(h("7. Prompt gồm ba phần và vì sao phải rào ngữ cảnh", 2));
noi.push(p("Prompt gửi đi luôn có đúng ba thành phần, tách bạch:"));
noi.push(g("Chỉ dẫn hệ thống — nói rõ vai trò, phạm vi được trả lời, và phải nói \"không biết\" khi thiếu căn cứ."));
noi.push(g("Ngữ cảnh — các đoạn học liệu vừa truy hồi, RÀO trong một khối dữ liệu có dấu mở và đóng riêng."));
noi.push(g("Lược đồ JSON đầu ra — với chức năng sinh đề, để kết quả kiểm chứng được bằng máy."));
noi.push(p(
  "Vì sao phải rào ngữ cảnh: nội dung đưa vào prompt có thể chứa câu lệnh trá hình (tấn công tiêm chỉ " +
  "thị — prompt injection). Rào lại rồi nói với mô hình rằng mọi câu lệnh bên trong khối đó là DỮ LIỆU " +
  "CẦN XỬ LÝ chứ không phải chỉ thị cần tuân theo. Với chức năng chấm tự luận, đây là bề mặt tấn công " +
  "lớn nhất vì bài làm do chính người học gõ."));

noi.push(h("8. Bốn lớp chống ảo giác", 2));
noi.push(bang(["Lớp", "Cơ chế"], [
  ["1. Ngưỡng khoảng cách", "Loại đoạn không đủ liên quan; hết đoạn thì prompt nói rõ là không có tài liệu"],
  ["2. Trả kèm trích dẫn", "Mỗi câu trả lời đi cùng danh sách tài liệu và đoạn đã dựa vào, để người đọc đối chiếu"],
  ["3. Kiểm chứng cấu trúc", "Kết quả sinh đề phải khớp lược đồ JSON; câu sai định dạng bị loại trước khi ra giao diện"],
  ["4. Con người ở vòng cuối", "Câu hỏi AI sinh ra chỉ là BẢN NHÁP, chỉ vào ngân hàng khi người tạo nội dung duyệt"],
], [2600, CW - 2600]));

noi.push(h("Câu hỏi hội đồng hay hỏi về RAG", 2));
noi.push(bang(["Câu hỏi", "Trả lời"], [
  ["RAG là gì, khác chatbot thường ở đâu?",
   "Chatbot thường trả lời bằng kiến thức mô hình đã học. RAG tìm trong kho tài liệu của người dùng những đoạn liên quan nhất, đưa vào ngữ cảnh rồi mới để mô hình viết. Khác biệt cốt lõi là TRUY VẾT ĐƯỢC về nguồn."],
  ["Vì sao không fine-tune mô hình trên giáo trình?",
   "Fine-tune dạy văn phong và định dạng, không dạy nguồn. Thêm một tài liệu là phải huấn luyện lại, và mô hình vẫn không nói được câu trả lời dựa vào đoạn nào."],
  ["Embedding 768 chiều nghĩa là gì?",
   "Mỗi đoạn văn được biểu diễn bằng 768 con số. Hai đoạn cùng ý nghĩa cho hai bộ số gần nhau, kể cả khi dùng từ khác. 768 là số chiều mô hình Gemini trả về."],
  ["Vì sao dùng cosine chứ không phải Euclid?",
   "Cosine đo góc, không quan tâm độ dài vector. Một đoạn dài và một đoạn ngắn cùng nói một ý vẫn được coi là gần nhau, còn Euclid sẽ phạt đoạn dài chỉ vì nó dài."],
  ["Ngưỡng 0,75 lấy ở đâu ra?",
   "Chọn theo quan sát trên kho học liệu của đồ án, không phải hằng số chuẩn của ngành. Đây là một giới hạn đã ghi trong báo cáo: cách làm chặt chẽ hơn là đo phân bố khoảng cách thực tế rồi mới chọn ngưỡng, và đó là hướng phát triển ngắn hạn."],
  ["Nếu kho học liệu lớn lên thì quét tuần tự có chậm không?",
   "Có. Ngưỡng là vài chục nghìn đoạn. Khi đó mới cần chỉ mục ANN, và phải bật kèm iterative scan của pgvector 0.8 để chỉ mục tự quét thêm khi bộ lọc quyền loại bớt ứng viên — chứ không quay lại cách lọc sau."],
  ["Làm sao chắc mô hình không bịa?",
   "Bốn lớp: ngưỡng khoảng cách, trả kèm trích dẫn, kiểm chứng cấu trúc JSON, và con người duyệt cuối. Đo ở mục 3.6: với hai câu hỏi nằm ngoài học liệu, trợ lý nói không biết chứ không suy đoán."],
  ["Chunk 1500 ký tự có phải con số chuẩn không?",
   "Không có con số chuẩn. 1500 đủ chứa trọn một ý mà không nuốt quá nhiều phần không liên quan; chồng lấp 200 để ý nằm vắt qua ranh giới không bị cắt đôi."],
], HOI));

/* ═══════════════════════ PHẦN II — REDIS ═══════════════════════ */
noi.push(h("Phần II — Redis"));

noi.push(h("1. Redis là gì và vì sao có mặt trong đồ án", 2));
noi.push(p(
  "Redis là kho dữ liệu khoá–giá trị chạy trong bộ nhớ chính. Nhanh vì mỗi thao tác không phải chạm đĩa. " +
  "Nó KHÔNG thay PostgreSQL: cách phân vai trong đồ án rất rõ — PostgreSQL giữ nguồn sự thật, Redis giữ " +
  "những thứ có vòng đời ngắn, đọc ghi liên tục, và quan trọng nhất là DỰNG LẠI ĐƯỢC nếu mất."));
noi.push(p(
  "Hệ quả thực tế của cách phân vai này, và cũng là câu trả lời gọn nhất nếu bị hỏi \"Redis chết thì " +
  "sao\": mất dữ liệu ở Redis không phải sự cố dữ liệu, vì mọi thứ trong đó đều suy lại được từ " +
  "PostgreSQL — trừ ván đấu đang chạy dở, và mất một ván đang chơi là cái giá chấp nhận được để đổi lấy " +
  "việc không phải ghi đĩa sau mỗi lần một người trả lời.", { dam: true }));

noi.push(h("2. Redis đảm nhiệm những gì — theo đúng khoá trong mã nguồn", 2));
noi.push(bang(["Vai trò", "Khoá thật", "Vì sao đặt ở Redis"], [
  ["Trạng thái phòng đang chơi", "room:{mã phòng}", "Câu hiện tại và điểm từng người đổi liên tục trong vài phút rồi hết giá trị. Chỉ kết quả cuối ván mới ghi xuống PostgreSQL"],
  ["Khoá phiên của khách vãng lai", "roomguest:{khoá}", "Khách không có JWT; khoá này chỉ mở đúng một phòng và chết cùng ván đấu"],
  ["Phiên đăng nhập", "session:{...}\nuser-sessions:{userId}", "Mỗi refresh token là một khoá có thời gian sống, nhờ đó THU HỒI ĐƯỢC — điều JWT tự thân không làm được"],
  ["Hạn mức và điều tiết AI", "aiquota:{...}\nai-throttle-until", "Bộ đếm theo ngày và mốc tạm ngừng gọi nhà cung cấp đang quá tải"],
  ["Mã OTP đặt lại mật khẩu", "pwd-otp:{...}\npwd-otp-attempts:{...}\npwd-otp-cooldown:{...}", "Mã sống mười phút, dùng một lần, sai quá năm lần thì huỷ — toàn bộ đều là dữ liệu có hạn dùng"],
  ["Bảng xếp hạng mùa đang chạy", "Sorted Set", "Cộng điểm bằng incrementScore, lấy top N bằng reverseRangeWithScores. Đây là chỉ mục dựng lại được từ bảng xp_events, không phải nguồn sự thật"],
  ["Bộ đệm lý do gợi ý", "recommendreason:{...}", "Kết quả tính toán tốn kém, dùng lại được trong thời gian ngắn"],
], [2300, 2300, CW - 4600]));

noi.push(h("3. Pub/Sub — vì sao phòng đấu bắt buộc phải có", 2));
noi.push(p(
  "Đây là phần đáng hỏi nhất về Redis trong đồ án, và cũng là phần dễ trả lời hụt nhất. Vấn đề như sau."));
noi.push(p(
  "Kết nối WebSocket là kết nối DÍNH vào đúng một tiến trình máy chủ — nó được giữ mở, không đi qua bộ " +
  "cân bằng tải lại ở mỗi lần gửi. Khi chạy nhiều tiến trình, hai người chơi trong cùng một phòng hoàn " +
  "toàn có thể đang nối vào hai tiến trình khác nhau. Tiến trình A tính xong điểm chỉ đẩy được cho các " +
  "client của chính A; người đang nối vào B không thấy gì cả."));
noi.push(p("Cách giải trong đồ án:"));
noi.push(g("Tiến trình xử lý XUẤT BẢN sự kiện lên một kênh Redis."));
noi.push(g("Mọi tiến trình đều ĐĂNG KÝ kênh đó, nhận được sự kiện rồi phát tiếp cho client của riêng mình."));
noi.push(g("Cấu hình ở RedisPubSubConfig: PatternTopic cho sự kiện phòng đấu, ChannelTopic cho thông báo."));
noi.push(p(
  "Số đo ở mục 3.5: mỗi sự kiện đi vòng qua Redis tốn khoảng 2 mili giây. Đó là cái giá của khả năng " +
  "chạy nhiều tiến trình.", { dam: true }));

noi.push(h("4. Hạn chế của Pub/Sub — nói ra trước khi bị hỏi", 2));
noi.push(p(
  "Redis Pub/Sub là at-most-once: ai không đang đăng ký đúng lúc tin phát ra thì mất tin. Không có hàng " +
  "đợi, không phát lại, không xác nhận đã nhận. Đây là giới hạn thật, và câu trả lời đúng là giải thích " +
  "vì sao nó chấp nhận được ở đây chứ không phải chối:"));
noi.push(g("Sự kiện ván đấu chỉ có giá trị trong vài giây; một sự kiện tới muộn cũng vô nghĩa."));
noi.push(g("Trạng thái phòng vẫn nằm ở Redis, nên client mất kết nối rồi vào lại sẽ ĐỌC LẠI TRẠNG THÁI, không cần ai phát lại tin."));
noi.push(g("Nếu bài toán đòi bảo đảm nhận thì phải dùng Redis Streams hoặc một hàng đợi thật như RabbitMQ hay Kafka — và khi đó phải chịu thêm độ trễ và độ phức tạp vận hành."));

noi.push(h("Câu hỏi hội đồng hay hỏi về Redis", 2));
noi.push(bang(["Câu hỏi", "Trả lời"], [
  ["Vì sao cần Redis khi đã có PostgreSQL?",
   "Ba lý do: trạng thái phòng đổi liên tục trong vài phút rồi hết giá trị, ghi đĩa mỗi lần đổi là lãng phí; phiên đăng nhập cần thu hồi được nên phải có khoá với thời gian sống; và phát tán sự kiện giữa nhiều tiến trình cần một kênh chung."],
  ["Redis mất dữ liệu thì hệ thống sao?",
   "Mọi thứ trong Redis đều dựng lại được từ PostgreSQL, trừ ván đấu đang chạy dở. Mất phiên thì người dùng đăng nhập lại; mất bảng xếp hạng mùa thì dựng lại từ bảng xp_events."],
  ["Vì sao không để refresh token trong chính JWT?",
   "JWT tự thân không thu hồi được trước hạn — đã ký là có hiệu lực tới lúc hết hạn. Đặt refresh token thành khoá Redis thì đổi mật khẩu là xoá khoá, phiên trên mọi thiết bị mất hiệu lực ngay."],
  ["Pub/Sub có bảo đảm không mất tin không?",
   "Không. Redis Pub/Sub là at-most-once. Chấp nhận được vì sự kiện ván đấu chỉ có giá trị vài giây, và client vào lại thì đọc lại trạng thái phòng chứ không cần phát lại tin."],
  ["Vì sao không dùng RabbitMQ hay Kafka?",
   "Nhu cầu ở đây là phát tán trong vài chục mili giây giữa vài tiến trình, không phải hàng đợi bền có bảo đảm nhận. Thêm một hệ trung gian nữa làm tăng độ trễ và chi phí vận hành mà không giải quyết thêm bài toán nào của đồ án."],
  ["Bảng xếp hạng dùng cấu trúc gì?",
   "Sorted Set — mỗi người là một phần tử, điểm là score. Cộng điểm bằng incrementScore, lấy top N bằng reverseRangeWithScores. Bảng đang chạy nằm ở Redis, chỉ khi mùa kết thúc mới chốt xuống PostgreSQL."],
], HOI));

/* ═══════════════════════ PHẦN III — NEO4J ═══════════════════════ */
noi.push(h("Phần III — Neo4j và cơ sở dữ liệu đồ thị"));

noi.push(h("1. Cơ sở dữ liệu đồ thị là gì", 2));
noi.push(p(
  "Dữ liệu gồm NÚT (node) mang nhãn và thuộc tính, nối với nhau bằng CẠNH (relationship) có kiểu và " +
  "hướng, và cạnh cũng mang thuộc tính được. Khác biệt căn bản với mô hình quan hệ: quan hệ biểu diễn " +
  "liên kết bằng khoá ngoại, và muốn đi qua một liên kết thì phải KẾT BẢNG (join)."));

noi.push(h("2. Vì sao duyệt nhiều bậc rẻ hơn — index-free adjacency", 2));
noi.push(p(
  "Đây là câu trả lời cốt lõi nếu hội đồng hỏi \"dùng SQL không được à\". Neo4j lưu sẵn con trỏ trực tiếp " +
  "từ mỗi nút sang các cạnh kề nó. Đi từ một nút sang hàng xóm là đi theo con trỏ — chi phí phụ thuộc số " +
  "hàng xóm chạm vào, KHÔNG phụ thuộc tổng kích thước dữ liệu. Tính chất này gọi là index-free adjacency."));
noi.push(p(
  "Trên mô hình quan hệ, mỗi bậc quan hệ là một phép tự kết (self-join). Chi phí mỗi phép kết phụ thuộc " +
  "kích thước bảng, và số phép kết tăng theo độ sâu. Nói gọn: đồ thị trả tiền theo SỐ HÀNG XÓM CHẠM VÀO, " +
  "quan hệ trả tiền theo KÍCH THƯỚC BẢNG.", { dam: true }));
noi.push(p(
  "Nhưng phải nói cho cân: ở quy mô dữ liệu của đồ án, SQL cũng chạy tốt. Khác biệt chỉ lộ ra khi dữ liệu " +
  "lớn và độ sâu tăng. Trả lời thành thật phần này an toàn hơn nhiều so với thổi phồng."));

noi.push(h("3. Cypher — ngôn ngữ truy vấn theo mẫu hình", 2));
noi.push(p(
  "Cypher mô tả MẪU HÌNH cần tìm bằng cú pháp gợi hình. Nút đặt trong ngoặc tròn, cạnh trong ngoặc vuông, " +
  "mũi tên chỉ hướng:"));
noi.push(ma("(a:User)-[:ATTEMPTED]->(q:Quiz)<-[:ATTEMPTED]-(b:User)"));
noi.push(p("Đọc là: \"hai người dùng a và b cùng làm một bài thi q\". Đó chính là định nghĩa của người học tương tự."));

noi.push(h("4. Mô hình đồ thị của đồ án", 2));
noi.push(bang(["Thành phần", "Tên", "Nội dung"], [
  ["Nút", "User", "Người học"],
  ["Nút", "Quiz", "Bài thi"],
  ["Nút", "Topic", "Chủ đề"],
  ["Cạnh", "ATTEMPTED", "Người học đã làm bài thi, kèm điểm và độ chính xác"],
  ["Cạnh", "PRACTICED", "Năng lực của người học trên một chủ đề, kèm total và accuracy"],
  ["Cạnh", "COVERS", "Bài thi bao gồm chủ đề nào"],
], [1400, 2000, CW - 3400]));
noi.push(p("Mô hình đã được lược bớt có chủ ý so với bản thiết kế ban đầu, theo hai nguyên tắc:"));
noi.push(g("Cạnh giữ SỰ THẬT ĐO ĐƯỢC, truy vấn giữ CÁCH DIỄN GIẢI. Quan hệ kiểu \"yếu ở chủ đề\" thực chất chỉ là PRACTICED nhìn qua một ngưỡng. Đưa ngưỡng vào cạnh thì mỗi lần đổi ngưỡng phải dựng lại toàn bộ đồ thị; để ngưỡng trong truy vấn thì đổi lúc nào cũng được."));
noi.push(g("Không lưu quan hệ mà hệ thống không có nguồn dữ liệu để suy ra. Quan hệ \"chủ đề tiên quyết\" bị loại vì không ai khai báo chủ đề nào phải học trước chủ đề nào — tự sinh ra nó là hệ thống bịa ra kiến thức sư phạm mà nó không có."));

noi.push(h("5. Ba truy vấn gợi ý — Cypher thật trong mã nguồn", 2));
noi.push(p("Gợi ý theo chủ đề còn yếu:", { dam: true }));
noi.push(ma(
  "MATCH (u:User {id: $userId})-[p:PRACTICED]->(t:Topic)<-[c:COVERS]-(q:Quiz)\n" +
  "WHERE p.total >= $minAnswers AND p.accuracy < $weakThreshold\n" +
  "OPTIONAL MATCH (:User)-[a:ATTEMPTED]->(q)\n" +
  "RETURN q.id AS quizId, q.title AS title, weakTopics\n" +
  "ORDER BY matchingQuestions DESC, attemptCount DESC"));
noi.push(p(
  "Đọc: đi từ người dùng qua các chủ đề họ đã luyện, lấy những chủ đề có đủ số câu đã trả lời " +
  "(minAnswers) mà độ chính xác dưới ngưỡng yếu, rồi vòng sang các bài thi bao những chủ đề ấy. Điều kiện " +
  "minAnswers chính là hàng rào chống kết luận vội từ một hai câu."));
noi.push(p("Gợi ý theo người học tương tự:", { dam: true }));
noi.push(ma(
  "MATCH (me:User {id: $userId})-[:ATTEMPTED]->(shared:Quiz)<-[:ATTEMPTED]-(peer:User)\n" +
  "WHERE peer.id <> $userId"));
noi.push(p("Đọc: hai bậc quan hệ — từ tôi sang các bài tôi đã làm, rồi vòng ngược về những người khác cũng làm các bài đó."));
noi.push(p("Truy vấn thứ ba là lộ trình học: thứ tự chủ đề nên ôn, dựng từ năng lực đo được trên từng chủ đề."));

noi.push(h("6. Đồng bộ dữ liệu — chỗ hay bị hỏi nhất", 2));
noi.push(p(
  "Nguồn sự thật là PostgreSQL. Neo4j chỉ là BẢN CHIẾU phục vụ phân tích. Toàn bộ thiết kế đồng bộ đi ra " +
  "từ một câu đó, và nếu nắm được câu đó thì trả lời được gần hết các câu hỏi về nhất quán dữ liệu.", { dam: true }));
noi.push(bang(["Quyết định", "Lý do"], [
  ["Không dùng giao dịch hai pha",
   "Đồ thị lệch hay mất thì dựng lại được từ lịch sử làm bài trong PostgreSQL. Chỉ cần thao tác bất biến theo số lần chạy (idempotent), hiện thực bằng MERGE"],
  ["Phát sự kiện ở pha SAU KHI giao dịch được ghi nhận",
   "Phát trước thì công việc nền có thể đọc dữ liệu chưa tồn tại — lượt làm bài chưa commit xong"],
  ["Đồng bộ chạy LẦN THỨ HAI sau khi AI chấm xong câu tự luận",
   "Lúc mới nộp, câu tự luận còn 0 điểm, nên năng lực tính ra chưa đúng"],
  ["Tính lại năng lực từ đầu trên toàn bộ lịch sử, KHÔNG cộng dồn",
   "Cộng dồn thì chạy hai lần là số liệu nhân đôi — mà chạy lại là chuyện bình thường của công việc nền"],
  ["Tạo ràng buộc duy nhất trên id của cả ba loại nút lúc khởi động",
   "Thiếu nó thì MERGE vẫn chạy nhưng quét toàn bộ nút mỗi lần, chậm dần theo kích thước đồ thị mà không có triệu chứng gì"],
  ["Neo4j chết không được làm hỏng việc nộp bài",
   "Đồng bộ chạy nền và nuốt lỗi; API gợi ý trả về danh sách rỗng thay vì lỗi hệ thống"],
], [3400, CW - 3400]));

noi.push(h("Câu hỏi hội đồng hay hỏi về Neo4j", 2));
noi.push(bang(["Câu hỏi", "Trả lời"], [
  ["Neo4j có thật sự cần không, SQL cũng làm được mà?",
   "Với ba truy vấn hiện có thì SQL cũng làm được — nói thẳng điều đó trước. Chọn đồ thị vì đây là loại truy vấn duyệt quan hệ nhiều bậc, mà ở đồ thị chi phí phụ thuộc số hàng xóm chạm vào chứ không phụ thuộc kích thước dữ liệu. Đây cũng là một trọng tâm phiếu giao đề tài nêu đích danh."],
  ["Index-free adjacency nghĩa là gì?",
   "Mỗi nút lưu sẵn con trỏ tới các cạnh kề, nên đi sang hàng xóm không phải tra chỉ mục. Đó là lý do duyệt nhiều bậc không đắt lên theo tổng kích thước dữ liệu."],
  ["Dữ liệu ở hai nơi có lệch nhau không?",
   "Có thể lệch, và hệ thống được thiết kế để lệch không nguy hiểm: PostgreSQL là nguồn sự thật, Neo4j dựng lại được từ lịch sử làm bài. Vì vậy không cần giao dịch hai pha, chỉ cần MERGE idempotent."],
  ["Người mới chưa có dữ liệu thì gợi ý kiểu gì?",
   "Không gợi ý bừa. API trả danh sách rỗng kèm hướng dẫn làm một bài để hệ thống hiểu năng lực. Gợi ý khi chưa có căn cứ là đưa ra lời khuyên sai."],
  ["Vì sao mô hình chỉ có ba loại quan hệ?",
   "Đã lược bớt có chủ ý. Cạnh chỉ giữ sự thật đo được; ngưỡng \"yếu\" nằm trong truy vấn để đổi ngưỡng không phải dựng lại đồ thị. Quan hệ chủ đề tiên quyết bị loại vì không có nguồn dữ liệu nào để suy ra nó."],
  ["Đồng bộ chạy lúc nào?",
   "Sau mỗi lượt nộp bài, phát sự kiện ở pha sau khi giao dịch được ghi nhận rồi khởi động công việc nền. Chạy lần thứ hai sau khi AI chấm xong câu tự luận, vì lúc mới nộp những câu đó còn chưa có điểm."],
], HOI));

/* ═══════════════════════ PHẦN IV ═══════════════════════ */
noi.push(h("Phần IV — Ba câu chốt nếu bị dồn"));
noi.push(p(
  "Khi bí, quay về ba câu này. Mỗi câu là một quyết định kỹ thuật có bằng chứng, và cả ba đều cho thấy " +
  "người làm hiểu hệ thống chứ không chỉ ghép thư viện."));
noi.push(bang(["Nếu bị hỏi về", "Câu chốt"], [
  ["RAG",
   "Thứ tự lọc quyền và xếp hạng không phải tuỳ chọn. Lọc sau thì chỉ mục xấp xỉ lấy 5 đoạn gần nhất toàn kho rồi mới loại theo quyền, kết quả rỗng trong khi kho có 9 đoạn hợp lệ — và nó hỏng hoàn toàn im lặng."],
  ["Redis",
   "Redis ở đây giữ đúng ba thứ: trạng thái sống trong vài phút, phiên cần thu hồi được, và kênh phát tán giữa nhiều tiến trình. Cả ba đều dựng lại được từ PostgreSQL, nên mất Redis không phải mất dữ liệu."],
  ["Neo4j",
   "PostgreSQL là nguồn sự thật, Neo4j là bản chiếu. Nhờ vậy không cần giao dịch hai pha, chỉ cần thao tác idempotent — và đồ thị hỏng thì dựng lại được từ lịch sử làm bài."],
], [1800, CW - 1800]));
noi.push(p(
  "Và một nguyên tắc chung cho cả buổi: chỗ nào đồ án chưa làm được thì nói thẳng là chưa làm được kèm " +
  "lý do. Một câu trả lời thành thật về giới hạn luôn an toàn hơn một câu vòng vo.", { dam: true, nghieng: true }));

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
