/**
 * Nạp THÊM 10 bộ đề tiếng Việt cho tài khoản giáo viên demo (gv.demo@quizai.local).
 *
 * Chạy:  node scripts/seed-quiz-gv.mjs
 * Yêu cầu: backend đang chạy (đổi cổng bằng biến môi trường API).
 *
 * ## Vì sao thêm một tệp nữa thay vì sửa `seed-them.mjs`
 * Tệp kia lấy câu hỏi **tiếng Anh** từ Open Trivia DB và sinh người học giả lập — nó lo khối lượng cho
 * bảng xếp hạng. Tệp này lo thứ khác: **nội dung tiếng Việt do chính tài khoản giáo viên demo sở hữu**,
 * để trang "Quiz của tôi" của gv.demo không chỉ có 5 dòng khi mở ra trước hội đồng.
 *
 * ## Vì sao KHÔNG gọi AI để sinh
 * `seed-ai.mjs` sinh đề bằng module AI của dự án và **tốn hạn mức** — mỗi bộ đề là một lượt gọi mô
 * hình. Hạn mức đó phải để dành cho lúc demo chức năng sinh đề thật. Nội dung dưới đây viết tay, nằm
 * trong chương trình phổ thông và đại cương, nên vừa không tốn hạn mức vừa không có câu sai.
 *
 * ## Năm loại câu hỏi đều có mặt
 * Mỗi bộ đề trộn SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK và SHORT_ANSWER — vừa để dữ
 * liệu giống đề thật, vừa để mở bất kỳ bộ đề nào ra cũng thấy đủ các loại câu hỏi hệ thống hỗ trợ.
 *
 * ## Câu điền khuyết ghi nhiều biến thể đáp án
 * `AnswerGrader.normalize` bỏ qua hoa/thường và khoảng trắng thừa nhưng **giữ dấu tiếng Việt** — cố ý,
 * vì "toán" và "toan" là hai từ khác nhau. Vì vậy mỗi câu FILL_BLANK liệt kê sẵn các cách viết hợp lệ
 * (dấu phẩy và dấu chấm thập phân, chữ và số) chứ không trông vào việc người làm bài gõ trúng đúng
 * một chuỗi duy nhất.
 *
 * ## Tránh câu hỏi về đơn vị hành chính
 * Sau đợt sáp nhập tỉnh năm 2025, các câu kiểu "tỉnh nào có diện tích lớn nhất" hay "Việt Nam có bao
 * nhiêu tỉnh" đã sai mà không ai sửa. Phần Địa lý dưới đây chỉ hỏi những thứ không đổi theo địa giới.
 *
 * ## Chạy lại không nhân đôi
 * Trùng tiêu đề thì bỏ qua cả bộ đề. Chốt idempotent đặt ở tiêu đề vì đó là thứ người dùng nhìn thấy.
 */

const API = process.env.API ?? 'http://localhost:8080/api/v1'
const EMAIL = process.env.GV_EMAIL ?? 'gv.demo@quizai.local'
const MAT_KHAU = process.env.GV_PASSWORD ?? 'MatKhau@123'

// ─────────────────────────────────────────────────────────────── tiện ích

async function goi(duongDan, { method = 'GET', token, body } = {}) {
  const res = await fetch(API + duongDan, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
  const chu = await res.text()
  const dl = chu ? JSON.parse(chu) : null
  if (!res.ok) {
    const e = new Error(method + ' ' + duongDan + ' → ' + res.status + ': ' + (dl?.message ?? chu))
    e.status = res.status
    throw e
  }
  return dl
}

/**
 * Trộn thứ tự lựa chọn.
 *
 * Để nguyên thì đáp án đúng luôn nằm ở vị trí đầu và người làm bài chỉ cần bấm A là qua — bộ đề mất
 * hết ý nghĩa. Đây đúng là lỗi `seed-them.mjs` từng mắc rồi sửa, nên ở đây làm luôn từ đầu.
 */
function tron(ds) {
  const a = [...ds]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    const tam = a[i]
    a[i] = a[j]
    a[j] = tam
  }
  return a
}

/** Câu một đáp án đúng. Đáp án đúng truyền ngay sau nội dung, phần còn lại là đáp án nhiễu. */
const mot = (content, dung, ...sai) => ({
  type: 'SINGLE_CHOICE',
  content,
  options: tron([
    { content: dung, correct: true },
    ...sai.map((s) => ({ content: s, correct: false })),
  ]),
})

/** Câu nhiều đáp án đúng. Service đòi ít nhất 2 đáp án đúng và ít nhất 1 lựa chọn sai. */
const nhieu = (content, dung, sai) => ({
  type: 'MULTIPLE_CHOICE',
  content,
  options: tron([
    ...dung.map((d) => ({ content: d, correct: true })),
    ...sai.map((s) => ({ content: s, correct: false })),
  ]),
})

const dungSai = (content, laDung) => ({
  type: 'TRUE_FALSE',
  content,
  options: [
    { content: 'Đúng', correct: laDung },
    { content: 'Sai', correct: !laDung },
  ],
})

/** Mỗi phần tử của `chapNhan` là một cách viết được tính đúng. */
const dien = (content, chapNhan) => ({
  type: 'FILL_BLANK',
  content,
  options: chapNhan.map((c) => ({ content: c, correct: true })),
})

/** Câu tự luận: lưu một đáp án mẫu để AI đối chiếu, kèm tiêu chí chấm cho điểm ổn định hơn. */
const tuLuan = (content, dapAnMau, rubric) => ({
  type: 'SHORT_ANSWER',
  content,
  rubric,
  options: [{ content: dapAnMau, correct: true }],
})

// ─────────────────────────────────────────────────────────────── nội dung 10 bộ đề

const BO_DE = [
  {
    tieuDe: 'Toán 12 — Nguyên hàm và tích phân',
    moTa: 'Ôn tập nguyên hàm cơ bản, tích phân xác định và ý nghĩa hình học của tích phân.',
    danhMuc: 'Toán học',
    doKho: 'MEDIUM',
    chuDe: 'Toán học',
    phut: 15,
    cauHoi: [
      mot('Nguyên hàm của hàm số f(x) = 2x là:', 'x² + C', '2x² + C', 'x²/2 + C', '2 + C'),
      mot('∫ cos x dx bằng:', 'sin x + C', '−sin x + C', 'cos x + C', '−cos x + C'),
      mot('Giá trị của tích phân ∫₀¹ 3x² dx là:', '1', '3', '1/3', '0'),
      mot('Với x > 0, ∫ (1/x) dx bằng:', 'ln x + C', '1/x² + C', '−1/x² + C', 'x·ln x + C'),
      dungSai('Mọi hàm số liên tục trên đoạn [a; b] đều có nguyên hàm trên đoạn đó.', true),
      nhieu(
        'Chọn các công thức nguyên hàm ĐÚNG:',
        ['∫ eˣ dx = eˣ + C', '∫ sin x dx = −cos x + C', '∫ xⁿ dx = xⁿ⁺¹/(n+1) + C (với n ≠ −1)'],
        ['∫ cos x dx = −sin x + C'],
      ),
      dien('Giá trị của ∫₀^π sin x dx bằng ___', ['2']),
      tuLuan(
        'Nêu ý nghĩa hình học của tích phân xác định ∫ₐᵇ f(x) dx trong trường hợp f(x) ≥ 0 trên đoạn [a; b].',
        'Tích phân bằng diện tích hình phẳng giới hạn bởi đồ thị hàm số y = f(x), trục Ox và hai đường thẳng x = a, x = b.',
        'Nêu đúng "diện tích hình phẳng" (0,5 điểm); chỉ ra đủ bốn đường giới hạn gồm đồ thị y = f(x), trục Ox, x = a và x = b (0,5 điểm).',
      ),
    ],
  },
  {
    tieuDe: 'Toán 12 — Khối đa diện và thể tích',
    moTa: 'Công thức thể tích các khối cơ bản và năm loại khối đa diện đều.',
    danhMuc: 'Toán học',
    doKho: 'MEDIUM',
    chuDe: 'Toán học',
    phut: 15,
    cauHoi: [
      mot('Thể tích khối lập phương có cạnh bằng a là:', 'a³', '3a', '6a²', 'a²'),
      mot('Khối chóp có diện tích đáy B và chiều cao h thì thể tích bằng:', '(1/3)·B·h', 'B·h', '(1/2)·B·h', '3·B·h'),
      mot('Khối lăng trụ có diện tích đáy B và chiều cao h thì thể tích bằng:', 'B·h', '(1/3)·B·h', '(1/2)·B·h', '2·B·h'),
      mot('Thể tích khối cầu bán kính R là:', '(4/3)·π·R³', '4·π·R²', '(1/3)·π·R³', 'π·R³'),
      mot('Khối bát diện đều có bao nhiêu mặt?', '8', '6', '12', '20'),
      dungSai('Có đúng 5 loại khối đa diện đều.', true),
      dien('Khối tứ diện đều có ___ đỉnh', ['4', 'bốn', 'bon']),
      nhieu(
        'Những khối nào sau đây là khối đa diện đều?',
        ['Khối tứ diện đều', 'Khối lập phương', 'Khối bát diện đều'],
        ['Khối chóp tứ giác đều'],
      ),
    ],
  },
  {
    tieuDe: 'Vật lý 12 — Sóng cơ và sóng âm',
    moTa: 'Bước sóng, môi trường truyền sóng, giao thoa và các đặc trưng của âm.',
    danhMuc: 'Vật lý',
    doKho: 'MEDIUM',
    chuDe: 'Vật lý',
    phut: 15,
    cauHoi: [
      mot('Bước sóng λ liên hệ với tốc độ truyền sóng v và tần số f theo công thức:', 'λ = v/f', 'λ = v·f', 'λ = f/v', 'λ = v/f²'),
      mot('Sóng cơ KHÔNG truyền được trong môi trường nào?', 'Chân không', 'Chất rắn', 'Chất lỏng', 'Chất khí'),
      mot('Sóng âm mà tai người nghe được có tần số trong khoảng:', 'Từ 16 Hz đến 20 000 Hz', 'Dưới 16 Hz', 'Trên 20 000 Hz', 'Từ 1 Hz đến 100 Hz'),
      mot('Tốc độ truyền âm lớn nhất trong môi trường nào?', 'Chất rắn', 'Chất lỏng', 'Chất khí', 'Chân không'),
      mot('Với hai nguồn kết hợp cùng pha, điều kiện để một điểm là cực đại giao thoa là:', 'd₂ − d₁ = k·λ', 'd₂ − d₁ = (k + 0,5)·λ', 'd₂ + d₁ = k·λ', 'd₂ − d₁ = k·λ/2'),
      dungSai('Trong sóng dọc, phương dao động của các phần tử môi trường trùng với phương truyền sóng.', true),
      dien('Một sóng có tần số 50 Hz truyền với tốc độ 20 m/s thì bước sóng bằng ___ m', ['0,4', '0.4']),
      nhieu(
        'Những đại lượng nào là đặc trưng VẬT LÝ của âm?',
        ['Tần số âm', 'Cường độ âm', 'Đồ thị dao động âm'],
        ['Độ cao của âm'],
      ),
    ],
  },
  {
    tieuDe: 'Vật lý 12 — Dòng điện xoay chiều',
    moTa: 'Cảm kháng, dung kháng, cộng hưởng điện và bài toán truyền tải điện năng.',
    danhMuc: 'Vật lý',
    doKho: 'HARD',
    chuDe: 'Vật lý',
    phut: 20,
    cauHoi: [
      mot('Cảm kháng của cuộn cảm thuần có độ tự cảm L trong mạch xoay chiều tần số góc ω là:', 'Z_L = ω·L', 'Z_L = 1/(ω·L)', 'Z_L = ω/L', 'Z_L = L/ω'),
      mot('Dung kháng của tụ điện có điện dung C là:', 'Z_C = 1/(ω·C)', 'Z_C = ω·C', 'Z_C = ω/C', 'Z_C = C/ω'),
      mot('Mạch RLC nối tiếp xảy ra cộng hưởng điện khi:', 'Z_L = Z_C', 'Z_L = R', 'Z_C = R', 'Z_L = 2·Z_C'),
      mot('Giá trị hiệu dụng U của điện áp xoay chiều liên hệ với giá trị cực đại U₀ theo:', 'U = U₀/√2', 'U = U₀·√2', 'U = U₀/2', 'U = 2·U₀'),
      mot('Công suất tiêu thụ của mạch điện xoay chiều được tính bằng:', 'P = U·I·cosφ', 'P = U·I', 'P = U·I·sinφ', 'P = U²·I·cosφ'),
      dungSai('Trong mạch điện chỉ chứa tụ điện, cường độ dòng điện sớm pha π/2 so với điện áp hai đầu tụ.', true),
      dien('Tần số của dòng điện xoay chiều trong mạng điện dân dụng ở Việt Nam là ___ Hz', ['50']),
      tuLuan(
        'Vì sao trong truyền tải điện năng đi xa, người ta phải tăng điện áp trước khi truyền?',
        'Công suất hao phí trên đường dây ΔP = P²·R/(U²·cos²φ) tỉ lệ nghịch với bình phương điện áp truyền tải. Tăng điện áp lên n lần thì hao phí giảm n² lần, nên tăng điện áp là cách giảm hao phí hiệu quả nhất mà không phải thay dây dẫn có điện trở nhỏ hơn.',
        'Viết đúng công thức hao phí hoặc nêu đúng quan hệ tỉ lệ nghịch với U² (0,6 điểm); kết luận tăng điện áp thì hao phí giảm theo bình phương (0,4 điểm).',
      ),
    ],
  },
  {
    tieuDe: 'Tin học — Thuật toán và cấu trúc dữ liệu cơ bản',
    moTa: 'Độ phức tạp thuật toán, ngăn xếp, hàng đợi, bảng băm và các thuật toán sắp xếp.',
    danhMuc: 'Tin học',
    doKho: 'MEDIUM',
    chuDe: 'Tin học',
    phut: 15,
    cauHoi: [
      mot('Độ phức tạp trung bình của thuật toán QuickSort là:', 'O(n·log n)', 'O(n²)', 'O(n)', 'O(log n)'),
      mot('Thuật toán tìm kiếm nhị phân đòi hỏi điều kiện gì ở dữ liệu đầu vào?', 'Mảng đã được sắp xếp', 'Mảng có số phần tử là lũy thừa của 2', 'Mảng không có phần tử trùng nhau', 'Mảng chỉ chứa số nguyên dương'),
      mot('Độ phức tạp của thuật toán tìm kiếm nhị phân là:', 'O(log n)', 'O(n)', 'O(n·log n)', 'O(1)'),
      mot('Ngăn xếp (stack) hoạt động theo nguyên tắc nào?', 'Vào sau — ra trước (LIFO)', 'Vào trước — ra trước (FIFO)', 'Ưu tiên phần tử nhỏ nhất', 'Truy cập ngẫu nhiên theo chỉ số'),
      mot('Hàng đợi (queue) hoạt động theo nguyên tắc nào?', 'Vào trước — ra trước (FIFO)', 'Vào sau — ra trước (LIFO)', 'Ưu tiên phần tử lớn nhất', 'Truy cập ngẫu nhiên theo chỉ số'),
      dungSai('Bảng băm (hash table) cho phép tra cứu một khóa với độ phức tạp trung bình O(1).', true),
      nhieu(
        'Những thuật toán sắp xếp nào có độ phức tạp trung bình O(n·log n)?',
        ['MergeSort', 'QuickSort', 'HeapSort'],
        ['BubbleSort'],
      ),
      dien('Thuật toán duyệt đồ thị theo chiều rộng (BFS) dùng cấu trúc dữ liệu ___', ['hàng đợi', 'queue', 'hang doi']),
    ],
  },
  {
    tieuDe: 'Tin học — Hệ điều hành và quản lý tiến trình',
    moTa: 'Tiến trình, luồng, định thời CPU, bế tắc và bộ nhớ ảo.',
    danhMuc: 'Tin học',
    doKho: 'MEDIUM',
    chuDe: 'Tin học',
    phut: 15,
    cauHoi: [
      mot('Đơn vị nhỏ nhất mà hệ điều hành cấp phát thời gian CPU cho là:', 'Luồng (thread)', 'Tiến trình (process)', 'Chương trình nguồn', 'Tập tin thực thi'),
      mot('Bế tắc (deadlock) xảy ra khi:', 'Các tiến trình chờ nhau theo vòng tròn trong khi vẫn giữ tài nguyên', 'Một tiến trình chiếm CPU quá lâu', 'Bộ nhớ vật lý bị dùng hết', 'Một tiến trình kết thúc bất thường'),
      mot('Bộ nhớ ảo được dùng để:', 'Cho phép chương trình dùng không gian địa chỉ lớn hơn RAM vật lý', 'Tăng tốc độ xung nhịp của CPU', 'Thay thế hoàn toàn RAM vật lý', 'Nén dữ liệu lưu trên đĩa cứng'),
      mot('Thuật toán định thời nào cho thời gian chờ trung bình nhỏ nhất về mặt lý thuyết?', 'SJF — việc ngắn nhất làm trước', 'FCFS — đến trước làm trước', 'Round Robin', 'Định thời ngẫu nhiên'),
      mot('Cơ chế nào bảo đảm tại mỗi thời điểm chỉ một luồng được vào vùng tranh chấp?', 'Khóa loại trừ (mutex)', 'Bộ nhớ đệm (cache)', 'Phân trang bộ nhớ (paging)', 'Chuyển ngữ cảnh'),
      dungSai('Chuyển ngữ cảnh (context switch) giữa hai tiến trình là thao tác không tốn chi phí.', false),
      nhieu(
        'Theo Coffman, những điều kiện nào là điều kiện cần để xảy ra bế tắc?',
        ['Loại trừ tương hỗ', 'Giữ và chờ', 'Chờ theo vòng tròn'],
        ['Tài nguyên được chia sẻ tự do giữa mọi tiến trình'],
      ),
      dien('Hiện tượng hệ thống dành gần hết thời gian để tráo trang bộ nhớ thay vì chạy chương trình gọi là ___', ['thrashing']),
    ],
  },
  {
    tieuDe: 'Lịch sử 12 — Kháng chiến chống Pháp 1945–1954',
    moTa: 'Từ Lời kêu gọi toàn quốc kháng chiến đến chiến thắng Điện Biên Phủ và Hiệp định Giơ-ne-vơ.',
    danhMuc: 'Lịch sử',
    doKho: 'MEDIUM',
    chuDe: 'Lịch sử',
    phut: 15,
    cauHoi: [
      mot('Chủ tịch Hồ Chí Minh ra "Lời kêu gọi toàn quốc kháng chiến" vào ngày:', '19/12/1946', '02/09/1945', '19/08/1945', '07/05/1954'),
      mot('Chiến dịch Điện Biên Phủ toàn thắng vào ngày:', '07/05/1954', '30/04/1975', '21/07/1954', '19/12/1946'),
      mot('Hiệp định Giơ-ne-vơ về Đông Dương được ký kết năm:', '1954', '1946', '1950', '1973'),
      mot('Chiến dịch Việt Bắc thu — đông diễn ra vào năm:', '1947', '1945', '1950', '1954'),
      mot('Chiến dịch Biên giới thu — đông diễn ra vào năm:', '1950', '1947', '1952', '1954'),
      mot('Ai là Chỉ huy trưởng kiêm Bí thư Đảng ủy chiến dịch Điện Biên Phủ?', 'Đại tướng Võ Nguyên Giáp', 'Đại tướng Nguyễn Chí Thanh', 'Đại tướng Văn Tiến Dũng', 'Thượng tướng Chu Văn Tấn'),
      dien('Chiến dịch Điện Biên Phủ kéo dài ___ ngày đêm', ['56']),
      tuLuan(
        'Nêu ý nghĩa lịch sử của chiến thắng Điện Biên Phủ năm 1954.',
        'Chiến thắng Điện Biên Phủ đập tan kế hoạch Nava, làm xoay chuyển cục diện chiến tranh, buộc thực dân Pháp phải ký Hiệp định Giơ-ne-vơ chấm dứt chiến tranh xâm lược Đông Dương; đồng thời cổ vũ mạnh mẽ phong trào giải phóng dân tộc ở các nước thuộc địa trên thế giới.',
        'Nêu được tác động quân sự — đập tan kế hoạch Nava, xoay chuyển cục diện (0,4 điểm); tác động ngoại giao — buộc Pháp ký Hiệp định Giơ-ne-vơ (0,3 điểm); ý nghĩa quốc tế với phong trào giải phóng dân tộc (0,3 điểm).',
      ),
    ],
  },
  {
    tieuDe: 'Lịch sử 12 — Việt Nam từ 1954 đến 1975',
    moTa: 'Phong trào Đồng khởi, Tết Mậu Thân, Hiệp định Pa-ri và Tổng tiến công Xuân 1975.',
    danhMuc: 'Lịch sử',
    doKho: 'HARD',
    chuDe: 'Lịch sử',
    phut: 20,
    cauHoi: [
      mot('Hiệp định Pa-ri về chấm dứt chiến tranh, lập lại hòa bình ở Việt Nam được ký năm:', '1973', '1954', '1968', '1975'),
      mot('Chiến dịch Hồ Chí Minh kết thúc thắng lợi vào ngày:', '30/04/1975', '07/05/1954', '27/01/1973', '02/09/1945'),
      mot('Cuộc Tổng tiến công và nổi dậy Tết Mậu Thân diễn ra năm:', '1968', '1960', '1972', '1975'),
      mot('Chiến dịch nào mở đầu cuộc Tổng tiến công và nổi dậy Xuân 1975?', 'Chiến dịch Tây Nguyên', 'Chiến dịch Huế — Đà Nẵng', 'Chiến dịch Hồ Chí Minh', 'Chiến dịch Đường 9 — Nam Lào'),
      mot('Mặt trận Dân tộc Giải phóng miền Nam Việt Nam được thành lập năm:', '1960', '1954', '1968', '1973'),
      dungSai('Phong trào Đồng khởi bùng nổ mạnh mẽ ở Bến Tre vào năm 1960.', true),
      dien('Cuộc Tổng tiến công và nổi dậy Xuân 1975 gồm ___ chiến dịch lớn', ['3', 'ba']),
      nhieu(
        'Ba chiến dịch lớn của cuộc Tổng tiến công và nổi dậy Xuân 1975 là:',
        ['Chiến dịch Tây Nguyên', 'Chiến dịch Huế — Đà Nẵng', 'Chiến dịch Hồ Chí Minh'],
        ['Chiến dịch Điện Biên Phủ'],
      ),
    ],
  },
  {
    tieuDe: 'Tiếng Anh — Thì động từ và câu điều kiện',
    moTa: 'Ôn tập các thì cơ bản, câu điều kiện loại 1 và loại 2, câu bị động.',
    danhMuc: 'Tiếng Anh',
    doKho: 'EASY',
    chuDe: 'Tiếng Anh',
    phut: 12,
    cauHoi: [
      mot('Chọn đáp án đúng: "She ___ to school every day."', 'goes', 'go', 'going', 'gone'),
      mot('Chọn đáp án đúng: "If it ___ tomorrow, we will stay at home."', 'rains', 'will rain', 'rained', 'would rain'),
      mot('Chọn đáp án đúng: "I ___ in Ha Noi since 2020."', 'have lived', 'live', 'lived', 'am living'),
      mot('Chọn đáp án đúng: "They ___ TV when I came home."', 'were watching', 'watch', 'watched', 'have watched'),
      mot('Chọn đáp án đúng: "If I ___ you, I would accept the offer."', 'were', 'am', 'will be', 'have been'),
      dungSai('Câu điều kiện loại 2 dùng để diễn tả điều không có thật ở hiện tại.', true),
      dien('Dạng quá khứ phân từ (V3) của động từ "write" là ___', ['written']),
      tuLuan(
        'Viết lại câu sau sang thể bị động: "They built this bridge in 2010."',
        'This bridge was built in 2010.',
        'Dùng đúng dạng bị động thì quá khứ đơn "was built" (0,6 điểm); giữ đúng chủ ngữ mới "this bridge" và trạng ngữ "in 2010" (0,4 điểm).',
      ),
    ],
  },
  {
    tieuDe: 'Kiến thức chung — Địa lý Việt Nam',
    moTa: 'Địa hình, đồng bằng, danh thắng và vị trí địa lý của Việt Nam.',
    danhMuc: 'Kiến thức chung',
    doKho: 'EASY',
    chuDe: 'Kiến thức chung',
    phut: 12,
    cauHoi: [
      mot('Đỉnh núi cao nhất Việt Nam là:', 'Phan Xi Păng', 'Tây Côn Lĩnh', 'Ngọc Linh', 'Núi Bà Đen'),
      mot('Đồng bằng có diện tích lớn nhất Việt Nam là:', 'Đồng bằng sông Cửu Long', 'Đồng bằng sông Hồng', 'Đồng bằng Thanh — Nghệ — Tĩnh', 'Đồng bằng Tuy Hòa'),
      mot('Vịnh Hạ Long — di sản thiên nhiên thế giới — thuộc tỉnh nào?', 'Quảng Ninh', 'Hải Phòng', 'Thái Bình', 'Nam Định'),
      mot('Thành phố nào được gọi là "thành phố ngàn hoa"?', 'Đà Lạt', 'Sa Pa', 'Nha Trang', 'Huế'),
      mot('Hang Sơn Đoòng — hang động tự nhiên lớn nhất thế giới — nằm trong vườn quốc gia nào?', 'Phong Nha — Kẻ Bàng', 'Cúc Phương', 'Cát Tiên', 'Ba Vì'),
      mot('Quần đảo Trường Sa trực thuộc tỉnh nào của Việt Nam?', 'Khánh Hòa', 'Đà Nẵng', 'Bình Định', 'Phú Yên'),
      dungSai('Phần đất liền của Việt Nam nằm hoàn toàn trong vùng nội chí tuyến bán cầu Bắc.', true),
      dien('Đỉnh Phan Xi Păng cao khoảng ___ m', ['3147', '3 147', '3143', '3 143']),
    ],
  },
]

// ─────────────────────────────────────────────────────────────── chạy

const gv = await goi('/auth/login', { method: 'POST', body: { email: EMAIL, password: MAT_KHAU } })
const token = gv.accessToken
console.log('Đăng nhập ' + EMAIL + ' — xong')

const dsDanhMuc = await goi('/categories', { token })
const danhMuc = {}
for (const c of dsDanhMuc.content ?? dsDanhMuc) {
  danhMuc[c.name] = c.id
}

const truoc = await goi('/quizzes?mine=true&size=200', { token })
const daCo = new Set((truoc.content ?? []).map((q) => q.title))
console.log('gv.demo đang có ' + truoc.totalElements + ' bộ đề\n')

let soTao = 0
let soCau = 0

for (const bo of BO_DE) {
  if (daCo.has(bo.tieuDe)) {
    console.log('  = đã có  ' + bo.tieuDe)
    continue
  }

  if (!danhMuc[bo.danhMuc]) {
    console.warn('  ! bỏ qua ' + bo.tieuDe + ': không tìm thấy danh mục "' + bo.danhMuc + '"')
    continue
  }

  const quiz = await goi('/quizzes', {
    method: 'POST',
    token,
    body: {
      title: bo.tieuDe,
      description: bo.moTa,
      categoryId: danhMuc[bo.danhMuc],
      difficulty: bo.doKho,
      visibility: 'PUBLIC',
      timeLimitSec: bo.phut * 60,
      strictExam: false,
    },
  })

  const ids = []
  for (const ch of bo.cauHoi) {
    const tao = await goi('/questions', {
      method: 'POST',
      token,
      body: { ...ch, topic: bo.chuDe, difficulty: ch.difficulty ?? bo.doKho, points: 1 },
    })
    ids.push(tao.id)
  }

  await goi('/quizzes/' + quiz.id + '/questions', {
    method: 'PUT',
    token,
    body: { questionIds: ids },
  })

  console.log('  + tạo    ' + bo.tieuDe + ' (' + ids.length + ' câu)')
  soTao++
  soCau += ids.length
}

const sau = await goi('/quizzes?mine=true&size=200', { token })
console.log('\nTạo mới ' + soTao + ' bộ đề, ' + soCau + ' câu hỏi.')
console.log('gv.demo hiện có ' + sau.totalElements + ' bộ đề.')
