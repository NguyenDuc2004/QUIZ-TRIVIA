# CHƯƠNG 1. TỔNG QUAN VỀ ỨNG DỤNG QUIZ/TRIVIA TÍCH HỢP TRÍ TUỆ NHÂN TẠO

Chương này trình bày cơ sở lý thuyết và các công nghệ được lựa chọn để xây dựng hệ thống. Mỗi mục được
trình bày theo mạch: khái niệm, đặc điểm chính, và vai trò cụ thể của công nghệ đó trong đồ án. Việc nêu
rõ vai trò là cần thiết, bởi một hệ thống dùng đồng thời ba loại cơ sở dữ liệu và hai nhà cung cấp mô
hình ngôn ngữ chỉ hợp lý khi mỗi thành phần giải quyết một bài toán mà thành phần khác không làm tốt hơn.

## 1.1. Hệ thống Quiz/Trivia trực tuyến

**Khái niệm.** Hệ thống Quiz/Trivia trực tuyến là ứng dụng cho phép người dùng tạo, quản lý và tham gia
các bài kiểm tra dạng trắc nghiệm hoặc đố vui trên môi trường web. Về bản chất, đây là sự kết hợp giữa
một hệ quản lý nội dung (quản lý bài thi, câu hỏi, đáp án) và một cơ chế đánh giá tự động (chấm điểm, trả
kết quả, thống kê).

**Học tập trò chơi hoá (gamified learning).** Điểm khiến các nền tảng quiz hiện đại khác biệt với bài
kiểm tra giấy là việc áp dụng yếu tố trò chơi vào hoạt động học: tính điểm theo tốc độ trả lời, bảng xếp
hạng trực tiếp, hiệu ứng phản hồi tức thì, chuỗi ngày học liên tục. Các yếu tố này tác động vào động lực
bên ngoài của người học, giúp duy trì tần suất ôn tập — điều mà bài tập truyền thống thường không đạt
được.

**Phân tích các nền tảng tương tự.**

| Tiêu chí | Kahoot! | Quizizz | Nhận xét |
|---|---|---|---|
| Thi đấu trực tiếp | Rất mạnh, là điểm nhấn chính; giáo viên trình chiếu câu hỏi, học sinh trả lời trên thiết bị riêng | Có, cùng mô hình phòng chơi theo mã | Cả hai đều chứng minh mô hình phòng đấu theo mã tham gia là hiệu quả và dễ dùng |
| Loại câu hỏi | Tập trung vào trắc nghiệm nhiều lựa chọn, đúng/sai | Đa dạng hơn, có điền khuyết | Câu tự luận/trả lời ngắn ít được hỗ trợ vì khó chấm tự động |
| Soạn đề | Nhập tay hoặc lấy từ thư viện có sẵn | Nhập tay, có kho câu hỏi cộng đồng | Không sinh câu hỏi từ học liệu riêng của người dùng |
| Phản hồi cho người học | Đúng/sai, điểm số | Đúng/sai, có giải thích nếu người soạn tự nhập | Không tự động giải thích vì sao sai |
| Cá nhân hoá | Hạn chế, chủ yếu theo lựa chọn của người dùng | Có chế độ luyện tập lại câu sai | Chưa dựa trên mô hình hành vi nhiều chiều |

**Vai trò trong đồ án.** Hai nền tảng trên là cơ sở tham chiếu cho phần nghiệp vụ quiz nền tảng: cấu trúc
bài thi, cơ chế phòng chơi theo mã tham gia, tính điểm theo tốc độ. Đồ án kế thừa những điểm đã được
chứng minh hiệu quả, đồng thời bổ sung ba phần mà các nền tảng này còn để trống: sinh đề từ học liệu bằng
AI, chấm và giải thích câu tự luận, và gợi ý cá nhân hoá dựa trên đồ thị hành vi.

## 1.2. Trí tuệ nhân tạo tạo sinh và mô hình ngôn ngữ lớn

**Trí tuệ nhân tạo tạo sinh (Generative AI)** là nhóm mô hình có khả năng tạo ra nội dung mới — văn bản,
hình ảnh, âm thanh — thay vì chỉ phân loại hoặc dự đoán trên dữ liệu có sẵn. Trong phạm vi đồ án, dạng
được sử dụng là mô hình sinh văn bản.

**Mô hình ngôn ngữ lớn (Large Language Model — LLM)** là mô hình học sâu, thường dựa trên kiến trúc
Transformer, được huấn luyện trên khối văn bản rất lớn để học phân phối xác suất của từ tiếp theo trong
một chuỗi. Nhờ quy mô tham số và dữ liệu, LLM thể hiện được khả năng làm nhiều tác vụ ngôn ngữ khác nhau
chỉ thông qua chỉ dẫn bằng lời (prompt), không cần huấn luyện lại cho từng tác vụ.

**Ba đặc điểm có ảnh hưởng trực tiếp tới thiết kế hệ thống:**

*Thứ nhất, mô hình không có tri thức về dữ liệu riêng của người dùng.* Một LLM chỉ biết những gì có trong
dữ liệu huấn luyện của nó, nên không thể trả lời về giáo trình mà một giáo viên cụ thể vừa tải lên. Đây
là lý do cần kiến trúc RAG (mục 1.3).

*Thứ hai, mô hình có thể "ảo giác" (hallucination)* — sinh ra nội dung nghe hợp lý nhưng sai sự thật, và
sinh ra với cùng giọng điệu tự tin như khi nó đúng. Trong ứng dụng ôn thi, đây là rủi ro nghiêm trọng
nhất, vì người học chưa nắm kiến thức thì không có cơ sở để phát hiện.

*Thứ ba, đầu ra không xác định (non-deterministic) và không đảm bảo đúng định dạng.* Cùng một prompt có
thể cho hai kết quả khác nhau, và mô hình có thể trả về JSON thiếu dấu ngoặc hoặc kèm lời dẫn. Vì vậy hệ
thống phải kiểm tra và xác thực đầu ra thay vì tin tưởng hoàn toàn.

**Nhà cung cấp mô hình trong đồ án.** Hệ thống dùng **Google Gemini** (`gemini-3.6-flash`) làm nhà cung
cấp chính, và **xAI Grok** làm nhà cung cấp dự phòng. Lý do dùng dịch vụ qua API thay vì tự triển khai mô
hình nguồn mở: chất lượng tiếng Việt tốt hơn ở cùng mức chi phí, và không cần hạ tầng GPU — phù hợp phạm
vi một đồ án tốt nghiệp.

**Ba chức năng AI trong hệ thống:**

1. **Sinh đề từ học liệu** — sinh bộ câu hỏi trắc nghiệm có cấu trúc từ tài liệu người dùng tải lên.
2. **Chấm và giải thích câu tự luận** — cho điểm câu trả lời ngắn kèm nhận xét, mở ra khả năng dùng loại
   câu hỏi có giá trị đánh giá cao mà vẫn tự động hoá được.
3. **Trợ lý học tập** — hỏi–đáp về nội dung học liệu, trả lời theo luồng (streaming) kèm trích dẫn nguồn.

Cả ba đều đi qua một lớp điều phối tự viết (`AiOrchestrator`) chứ không gọi trực tiếp API của nhà cung cấp
từ tầng nghiệp vụ. Lớp này chịu trách nhiệm chọn nhà cung cấp, chuyển dự phòng khi lỗi, ghi nhật ký số
token và độ trễ, và áp hạn mức. Đồ án **không dùng** các thư viện trừu tượng hoá sẵn như Spring AI hay
LangChain4j, nhằm kiểm soát trực tiếp cơ chế dự phòng và hạn mức — vốn là hai điểm cần đo và báo cáo.

## 1.3. Kiến trúc RAG (Retrieval-Augmented Generation)

**Khái niệm.** RAG là kiến trúc kết hợp một bước **truy xuất** (retrieval) trước bước **sinh**
(generation): thay vì hỏi mô hình ngôn ngữ trực tiếp, hệ thống tìm trong kho tri thức riêng những đoạn
văn bản liên quan nhất tới câu hỏi, đưa các đoạn đó vào prompt làm ngữ cảnh, rồi yêu cầu mô hình trả lời
**chỉ dựa trên ngữ cảnh** ấy.

**Vấn đề RAG giải quyết.** RAG xử lý đồng thời hai hạn chế đã nêu ở mục 1.2: mô hình được cấp tri thức
riêng mà nó chưa từng học, và mô hình bị ràng buộc vào nguồn cụ thể nên giảm hẳn khả năng bịa. Cách tiếp
cận thay thế là tinh chỉnh (fine-tuning) mô hình trên tài liệu riêng, nhưng không phù hợp ở đây: mỗi lần
người dùng tải tài liệu mới lại phải huấn luyện lại, chi phí cao, và mô hình vẫn không nêu được nguồn.

**Hai quy trình của RAG trong hệ thống.**

*Quy trình nạp học liệu (ingest) — chạy một lần khi người dùng tải tài liệu:*

```
Tệp PDF/DOCX/TXT
   → Apache Tika trích xuất văn bản thuần
   → chia thành các đoạn (chunk) có độ dài phù hợp, gối nhau một phần
   → gọi mô hình embedding sinh vector 768 chiều cho từng đoạn
   → lưu đoạn văn bản kèm vector vào PostgreSQL (bảng material_chunks, cột kiểu vector)
```

*Quy trình truy xuất và sinh (retrieval + generation) — chạy mỗi lần có câu hỏi:*

```
Câu hỏi của người dùng
   → sinh vector cho câu hỏi bằng cùng mô hình embedding
   → tìm k đoạn có khoảng cách cosine nhỏ nhất trong phạm vi tài liệu người dùng được phép đọc
   → loại các đoạn có khoảng cách vượt ngưỡng (không đủ liên quan)
   → ghép các đoạn còn lại thành ngữ cảnh, dựng prompt kèm chỉ dẫn "chỉ trả lời dựa trên ngữ cảnh"
   → AiOrchestrator gọi LLM → trả lời theo luồng kèm danh sách tài liệu đã dựa vào
```

**Vector nhúng (embedding) và tìm kiếm ngữ nghĩa.** Embedding là phép biểu diễn một đoạn văn bản thành
vector số thực trong không gian nhiều chiều, sao cho hai đoạn gần nghĩa nằm gần nhau. Nhờ vậy, tìm kiếm
theo embedding là **tìm kiếm theo ngữ nghĩa** chứ không theo từ khoá: câu hỏi "Thymeleaf dùng để làm gì"
vẫn khớp được đoạn văn bản không chứa nguyên cụm từ đó. Độ gần được đo bằng **khoảng cách cosine** — giá
trị càng nhỏ thì hai vector càng cùng hướng, tức càng gần nghĩa.

**Bám nguồn (grounding) — yêu cầu thiết kế, không phải tuỳ chọn.** Hai biện pháp được áp dụng:

- **Ngưỡng khoảng cách.** Truy vấn vector luôn trả về đúng số đoạn được yêu cầu, kể cả khi không đoạn nào
  liên quan tới câu hỏi — đoạn "gần nhất" trong một kho toàn tài liệu Toán vẫn là một đoạn Toán khi người
  dùng hỏi về Lịch sử. Nếu không lọc, prompt sẽ chứa ngữ cảnh sai và mô hình sẽ cố trả lời từ đó. Vì vậy
  hệ thống loại bỏ mọi đoạn có khoảng cách vượt ngưỡng, và khi không còn đoạn nào thì prompt nói rõ là
  không có tài liệu liên quan — mô hình trả lời "không biết" thay vì đoán.
- **Trả kèm trích dẫn.** Mỗi câu trả lời đi cùng danh sách tài liệu và đoạn văn bản đã dựa vào, để người
  dùng tự đối chiếu. Một trợ lý trả lời trôi chảy mà không cho biết lấy từ đâu sẽ được tin nhiều hơn mức
  nó đáng được tin.

**Vai trò trong đồ án.** RAG là nền tảng chung cho hai trong ba chức năng AI: sinh đề (lấy ngữ cảnh từ học
liệu của người soạn) và trợ lý học tập (lấy ngữ cảnh từ học liệu người học được phép đọc).

## 1.4. Spring Boot

**Khái niệm.** Spring Boot là framework phát triển ứng dụng Java dựa trên Spring Framework, ra đời để
giảm khối lượng cấu hình thủ công. Cơ chế **tự động cấu hình** (auto-configuration) dò các thư viện có
trong classpath rồi thiết lập sẵn các bean tương ứng; cơ chế **starter** đóng gói các nhóm phụ thuộc
thường dùng chung.

**Đặc điểm chính:** hỗ trợ **tiêm phụ thuộc** (dependency injection) giúp các lớp không tự khởi tạo phụ
thuộc của mình nên dễ thay thế khi kiểm thử; máy chủ nhúng (embedded server) cho phép chạy ứng dụng như
một tiến trình Java thông thường; và hệ sinh thái module phong phú (Spring Web, Security, Data JPA, Data
Neo4j, WebSocket) — điểm quan trọng với đồ án này vì hệ thống phải nói chuyện với ba loại cơ sở dữ liệu
và hai giao thức thời gian thực.

**Vai trò trong đồ án.** Spring Boot 3.x trên Java 21 (LTS) là nền của toàn bộ phía máy chủ: cung cấp
REST API, xử lý toàn bộ nghiệp vụ (quản lý quiz, chấm bài, phòng đấu), tích hợp AI, và bảo mật. Kiến trúc
được tổ chức **phân lớp**: `Controller` nhận và kiểm tra dữ liệu vào rồi trả DTO; `Service` chứa nghiệp
vụ; `Repository` truy cập dữ liệu; `Domain` là các thực thể. Quy tắc bắt buộc trong đồ án: controller
không chứa logic nghiệp vụ, và thực thể không bao giờ được trả trực tiếp ra API.

Các thành phần Spring được sử dụng: **Spring Web** (REST), **Spring Security** kết hợp JWT (xác thực,
phân quyền), **Spring Data JPA** với Hibernate (PostgreSQL), **Spring Data Neo4j** (đồ thị gợi ý),
**Spring WebSocket** (phòng đấu), **Spring Mail** (gửi mã OTP), cùng **Flyway** cho quản lý phiên bản
lược đồ, **Jakarta Bean Validation** cho kiểm tra dữ liệu vào, **Resilience4j** cho cầu dao (circuit
breaker) khi gọi AI, và **springdoc-openapi** sinh tài liệu API.

## 1.5. React và TypeScript

**React** là thư viện JavaScript xây dựng giao diện người dùng theo hướng **thành phần** (component):
giao diện được chia thành các đơn vị độc lập, mỗi đơn vị quản lý trạng thái riêng và được tái sử dụng.
React dùng **DOM ảo** để so sánh và chỉ cập nhật phần thay đổi thật, giúp giao diện phản hồi nhanh — điều
cần thiết với các màn hình cập nhật liên tục như bảng xếp hạng trực tiếp trong phòng đấu.

**TypeScript** là phần mở rộng của JavaScript, bổ sung hệ thống kiểu tĩnh. Lợi ích thực tế trong đồ án
này là các lỗi sai tên trường hoặc sai kiểu dữ liệu trả về từ API bị phát hiện ngay khi biên dịch, thay
vì trở thành lỗi lúc chạy.

**Các thư viện đi kèm:**

| Thư viện | Vai trò |
|---|---|
| **Vite 8** | Công cụ build và máy chủ phát triển, nạp lại thay đổi gần như tức thì |
| **TanStack Query** | Quản lý dữ liệu lấy từ máy chủ: bộ đệm, trạng thái đang tải, tự làm mới |
| **Zustand** | Quản lý trạng thái phía trình duyệt (phiên đăng nhập, thông tin người dùng) |
| **React Router 7** | Điều hướng giữa các trang trong ứng dụng đơn trang |
| **Ant Design v6** | Bộ thành phần giao diện sẵn có (bảng, biểu mẫu, hộp thoại) |
| **Tailwind CSS v4** | Lớp tiện ích cho bố cục và khoảng cách |
| **React Hook Form + Zod** | Xử lý biểu mẫu và xác thực dữ liệu vào theo lược đồ |
| **@stomp/stompjs + SockJS** | Kết nối WebSocket theo giao thức STOMP cho phòng đấu |

**Hai lưu ý về lựa chọn công nghệ:**

*Về Ant Design kết hợp Tailwind:* Ant Design lo phần thành phần, Tailwind chỉ dùng cho bố cục và khoảng
cách. Tailwind v4 mặc định nạp bộ reset CSS (Preflight) sẽ đè lên kiểu dáng của Ant Design, nên hệ thống
chỉ nạp phần `theme` và `utilities`, không nạp Preflight.

*Về việc không dùng Next.js:* đồ án dùng React thuần với Vite. Hệ thống là ứng dụng sau đăng nhập, dữ liệu
gắn với từng người dùng nên không hưởng lợi từ kết xuất phía máy chủ; thêm một tầng máy chủ Node chỉ làm
kiến trúc phức tạp hơn mà không giải quyết vấn đề nào của đề tài.

## 1.6. PostgreSQL và phần mở rộng pgvector

**PostgreSQL** là hệ quản trị cơ sở dữ liệu quan hệ nguồn mở, tuân thủ chuẩn SQL, hỗ trợ giao dịch ACID.
Hai đặc điểm được dùng nhiều trong đồ án: kiểu dữ liệu **JSONB** cho phép lưu dữ liệu có cấu trúc thay
đổi (ví dụ nội dung câu trả lời, vốn khác nhau giữa năm loại câu hỏi) mà không phải tạo bảng riêng cho
từng loại; và khả năng **mở rộng bằng extension**.

**pgvector** là extension bổ sung kiểu dữ liệu `vector` cùng các toán tử tính khoảng cách giữa hai vector,
trong đó `<=>` là khoảng cách cosine. Nhờ pgvector, các vector nhúng của học liệu được lưu **cùng cơ sở dữ
liệu** với dữ liệu nghiệp vụ.

**Vai trò trong đồ án.** PostgreSQL 16 lưu toàn bộ dữ liệu nghiệp vụ (người dùng, quiz, câu hỏi, lượt làm
bài, phòng đấu, hội thoại trợ lý) và đồng thời là kho vector cho RAG.

Việc dùng chung một cơ sở dữ liệu cho cả hai mục đích là lựa chọn có chủ ý. Một hệ quản trị vector chuyên
biệt (Pinecone, Milvus, Qdrant) sẽ nhanh hơn ở quy mô rất lớn, nhưng đổi lại phải vận hành thêm một dịch
vụ, và quan trọng hơn là **mất khả năng lọc quyền truy cập cùng lúc với tìm kiếm vector trong một truy
vấn**. Trong hệ thống này, mỗi lần truy xuất đều phải giới hạn trong phạm vi tài liệu người gọi được phép
đọc; giữ vector cùng chỗ với dữ liệu quyền cho phép làm việc đó bằng một câu SQL.

Lược đồ được quản lý bằng **Flyway**: mọi thay đổi cấu trúc là một tệp migration được đánh số và không
bao giờ sửa lại tệp đã áp dụng, nhờ đó cơ sở dữ liệu ở mọi môi trường luôn dựng lại được từ đầu một cách
xác định.

## 1.7. Neo4j — cơ sở dữ liệu đồ thị

**Khái niệm.** Neo4j là hệ quản trị cơ sở dữ liệu đồ thị, lưu dữ liệu dưới dạng **nút** (node) và **quan
hệ** (relationship), cả hai đều có thể mang thuộc tính. Khác với mô hình quan hệ — nơi liên kết giữa các
bảng được suy ra lúc truy vấn qua phép kết (JOIN) — trong đồ thị, quan hệ là đối tượng được lưu trực tiếp
và có thể duyệt qua với chi phí không phụ thuộc tổng kích thước dữ liệu.

**Cypher** là ngôn ngữ truy vấn của Neo4j, mô tả mẫu (pattern) cần tìm bằng cú pháp gợi hình:
`(a:User)-[:ATTEMPTED]->(q:Quiz)<-[:ATTEMPTED]-(b:User)` diễn tả "hai người dùng cùng làm một bài thi".

**Vì sao cần đồ thị cho bài toán gợi ý.** Câu hỏi gợi ý điển hình là: *"những người có hành vi tương tự
người dùng này thường làm tiếp bài nào mà người này chưa làm?"* Trên mô hình quan hệ, đây là hai phép tự
kết trên bảng lượt làm bài kèm nhóm và đếm — viết được nhưng khó đọc và khó mở rộng khi thêm điều kiện.
Trên Cypher, câu truy vấn viết ra gần đúng như cách phát biểu bài toán. Càng nhiều bậc quan hệ cần duyệt
(người → bài → chủ đề → bài khác), khác biệt này càng rõ.

**Mô hình đồ thị trong đồ án.** Các nút chính là `User`, `Quiz`, `Topic`; các quan hệ chính:

- `(User)-[:ATTEMPTED]->(Quiz)` — người dùng đã làm bài thi, kèm thuộc tính điểm số.
- `(User)-[:WEAK_IN]->(Topic)` — người dùng yếu ở chủ đề nào, suy ra từ kết quả làm bài.
- `(Quiz)-[:SIMILAR_TO]->(Quiz)` — quan hệ tương tự giữa các bài thi.

**Vai trò trong đồ án.** Neo4j 5 là nơi lưu **mô hình hành vi** phục vụ gợi ý bài thi và lộ trình học cá
nhân hoá. Đây là ví dụ của nguyên tắc **lưu trữ đa hệ** (polyglot persistence): PostgreSQL giữ dữ liệu
gốc có tính giao dịch, Neo4j giữ bản chiếu quan hệ hành vi được đồng bộ sang sau mỗi lượt làm bài.

## 1.8. Redis

**Khái niệm.** Redis là hệ lưu trữ khoá–giá trị trong bộ nhớ, cho độ trễ đọc/ghi ở mức dưới một
mili-giây. Redis hỗ trợ nhiều kiểu dữ liệu (chuỗi, băm, danh sách, tập hợp, tập hợp có thứ tự), đặt thời
gian sống (TTL) cho từng khoá, và cung cấp cơ chế **xuất bản – đăng ký** (Pub/Sub).

**Bốn vai trò trong đồ án:**

*Thứ nhất, quản lý phiên đăng nhập.* Mỗi refresh token là một khoá `session:{token}` có TTL. Cách này cho
phép **thu hồi** phiên — điều mà JWT tự thân không làm được vì token đã cấp thì có hiệu lực tới khi hết
hạn. Nhờ đó hệ thống hiện thực được "đăng xuất khỏi mọi thiết bị" và "đổi mật khẩu thì thu hồi mọi phiên".

*Thứ hai, lưu trạng thái phòng đấu.* Trạng thái đang diễn ra của một phòng (câu hỏi hiện tại, điểm từng
người, thời điểm bắt đầu) thay đổi liên tục trong vài phút rồi hết giá trị. Ghi vào PostgreSQL mỗi lần
đổi là tốn kém không cần thiết; Redis với TTL phù hợp hơn. Kết quả cuối ván mới ghi xuống cơ sở dữ liệu
quan hệ.

*Thứ ba, đồng bộ thời gian thực bằng Pub/Sub.* Khi một người chơi trả lời, sự kiện cập nhật điểm được
xuất bản qua Redis để mọi tiến trình máy chủ đang giữ kết nối WebSocket của phòng đó đều nhận được và
phát tiếp cho người chơi của mình. Cơ chế này là điều kiện để hệ thống chạy nhiều tiến trình mà người
chơi trong cùng phòng vẫn thấy trạng thái nhất quán.

*Thứ tư, bộ đệm và hạn mức AI.* Redis lưu bộ đệm kết quả và bộ đếm hạn mức cho các lời gọi mô hình ngôn
ngữ — cần thiết vì mỗi lời gọi đều phát sinh chi phí và nhà cung cấp giới hạn số lượt mỗi phút.

Ngoài ra, kiểu **tập hợp có thứ tự** (sorted set) của Redis phù hợp cho bảng xếp hạng, vì việc chèn phần
tử và lấy nhóm dẫn đầu đều có chi phí thấp.

## 1.9. WebSocket và giao thức STOMP

**Vấn đề của HTTP với dữ liệu thời gian thực.** HTTP hoạt động theo mô hình yêu cầu – phản hồi: máy chủ
chỉ gửi dữ liệu khi trình duyệt hỏi. Muốn cập nhật liên tục, cách thô sơ là hỏi lại theo chu kỳ
(polling), nhưng cách này vừa tạo độ trễ trung bình bằng nửa chu kỳ, vừa sinh nhiều yêu cầu vô ích khi
không có gì mới.

**WebSocket** là giao thức cho phép mở một kết nối **song công** (hai chiều) và duy trì liên tục trên
cùng cổng của HTTP. Sau bước bắt tay nâng cấp giao thức, cả hai phía đều có thể chủ động gửi dữ liệu bất
cứ lúc nào, nên máy chủ đẩy được cập nhật ngay khi có thay đổi.

**STOMP (Simple Text Oriented Messaging Protocol)** là giao thức nhắn tin chạy **trên** WebSocket, bổ
sung lớp ngữ nghĩa mà WebSocket thuần không có: khái niệm **đích đến** (destination), cơ chế **đăng ký**
theo chủ đề, và khung tin có tiêu đề. Nhờ STOMP, thay vì tự định nghĩa định dạng tin và tự quản lý danh
sách người nhận, ứng dụng chỉ cần cho người chơi đăng ký chủ đề `/topic/room/{code}` rồi gửi tin tới đó.

**Vai trò trong đồ án.** WebSocket + STOMP là nền của phòng đấu thời gian thực: đồng bộ trạng thái phòng
(ai vào, ai rời, chủ phòng bắt đầu ván), phát câu hỏi đồng thời cho mọi người chơi, nhận đáp án, và cập
nhật bảng xếp hạng trực tiếp.

**Tính điểm theo tốc độ** là lý do độ trễ trở thành yêu cầu chức năng chứ không chỉ là chỉ tiêu kỹ thuật:
nếu điểm phụ thuộc thời gian trả lời, thì độ trễ mạng không đều giữa các người chơi sẽ trực tiếp gây bất
công về điểm số. Đây cũng là lý do phiếu giao đề tài yêu cầu đo hiệu năng chịu tải thời gian thực
(trình bày ở mục 3.5).

**Bảo mật kết nối.** Xác thực JWT được thực hiện tại **khung STOMP CONNECT** thay vì lúc bắt tay HTTP, vì
trình duyệt không cho phép gắn tiêu đề tuỳ ý vào yêu cầu nâng cấp WebSocket.

## 1.10. Docker và Docker Compose

**Docker** là nền tảng ảo hoá ở mức ứng dụng: mỗi ứng dụng cùng toàn bộ phụ thuộc của nó được đóng vào
một **container** dùng chung nhân hệ điều hành của máy chủ, nên nhẹ và khởi động nhanh hơn máy ảo.
**Docker Compose** khai báo một tập nhiều container cùng cấu hình mạng và ổ lưu trữ trong một tệp YAML,
rồi bật tất cả bằng một lệnh.

**Vai trò trong đồ án.** Hệ thống cần đồng thời ba dịch vụ dữ liệu: PostgreSQL 16 kèm pgvector, Neo4j 5,
và Redis 7. Cài đặt thủ công cả ba trên máy phát triển vừa tốn thời gian vừa dễ lệch phiên bản; Docker
Compose cho phép dựng đúng bộ đó bằng một lệnh `docker compose up -d`, và phiên bản được ghi thẳng trong
tệp cấu hình nên môi trường của mọi máy là như nhau.

Việc dùng container còn phục vụ **kiểm thử tự động**: các bài kiểm thử tích hợp dùng **Testcontainers**
để dựng PostgreSQL thật (có pgvector) cho mỗi lần chạy, nhờ đó kiểm thử truy vấn vector trên đúng hệ quản
trị sẽ dùng khi triển khai, thay vì thay bằng cơ sở dữ liệu trong bộ nhớ có hành vi khác.

## 1.11. Kiến trúc client–server phân lớp của hệ thống

**Mô hình tổng thể.** Hệ thống theo kiến trúc client–server: trình duyệt chạy ứng dụng React, giao tiếp
với máy chủ Spring Boot qua ba kênh, mỗi kênh cho một dạng dữ liệu khác nhau:

| Kênh | Dùng cho | Lý do chọn |
|---|---|---|
| **REST (HTTPS)** | Nghiệp vụ thường: đăng nhập, quản lý quiz, làm bài, xem kết quả | Mô hình yêu cầu – phản hồi phù hợp; dễ đặt bộ đệm, dễ kiểm thử |
| **WebSocket (STOMP)** | Phòng đấu thời gian thực | Cần hai chiều, máy chủ chủ động đẩy tin |
| **SSE (Server-Sent Events)** | Trả lời của trợ lý học tập | Một chiều từ máy chủ, phù hợp luồng văn bản sinh dần |

Sở dĩ trợ lý học tập dùng SSE chứ không WebSocket: dòng dữ liệu chỉ đi một chiều (máy chủ → trình duyệt),
và SSE chạy trên HTTP thông thường nên đơn giản hơn hẳn.

**Sơ đồ kiến trúc tổng thể.**

```
┌─────────────────────────────────────────────────────────────┐
│                     Client (Browser)                        │
│     React SPA + TypeScript + Vite + Ant Design + Tailwind   │
└──────────────┬───────────────────────────┬──────────────────┘
               │ HTTPS (REST + SSE)        │ WebSocket (STOMP)
               ▼                           ▼
┌─────────────────────────────────────────────────────────────┐
│                  Backend — Spring Boot                      │
│  ┌────────────┐  ┌───────────┐  ┌──────────┐  ┌───────────┐ │
│  │ Controller │→ │  Service  │→ │Repository│→ │  Domain   │ │
│  │ REST + WS  │  │  (logic)  │  │ JPA/Neo4j│  │ (Entity)  │ │
│  └────────────┘  └─────┬─────┘  └──────────┘  └───────────┘ │
│   ┌────────────────────┼────────────────────┐               │
│   ▼                    ▼                    ▼               │
│ ┌──────────────┐ ┌───────────────┐ ┌──────────────────┐     │
│ │ RAG Pipeline │ │AI Orchestrator│ │ Realtime Game    │     │
│ │ ingest +     │ │ Gemini→Grok   │ │ Engine (WebSocket│     │
│ │ retrieval    │ │ +CircuitBrkr  │ │ + Redis Pub/Sub) │     │
│ └──────────────┘ └───────────────┘ └──────────────────┘     │
│   Security (JWT) · Async Jobs · Caching · Validation        │
└──────┬──────────┬──────────────┬─────────────┬──────────────┘
       │          │              │             │
       ▼          ▼              ▼             ▼
┌───────────┐ ┌────────┐ ┌────────────┐ ┌──────────────────────┐
│PostgreSQL │ │ Neo4j  │ │   Redis    │ │ External AI Providers│
│+ pgvector │ │ đồ thị │ │ cache,     │ │ Gemini API (chính)   │
│dữ liệu +  │ │ hành vi│ │ session,   │ │ Grok API (dự phòng)  │
│vector     │ │ gợi ý  │ │ realtime   │ └──────────────────────┘
└───────────┘ └────────┘ └────────────┘
```

*Hình 1.1. Kiến trúc tổng thể hệ thống*

**Nguyên tắc thiết kế được tuân thủ:**

1. **Kiến trúc phân lớp.** Controller → Service → Repository → Domain, không để logic nghiệp vụ ở
   controller. Mỗi tầng chỉ phụ thuộc tầng kế dưới.
2. **Module hoá theo tính năng.** Mã nguồn nhóm theo tính năng (`auth`, `quiz`, `attempt`, `realtime`,
   `ai`, `recommend`, `analytics`), trong mỗi tính năng mới chia tiếp theo tầng. Nhờ vậy sửa một tính năng
   chỉ cần mở một thư mục, mà ranh giới các tầng vẫn rõ.
3. **API không trạng thái.** Xác thực bằng JWT nên máy chủ không giữ trạng thái phiên trong bộ nhớ; trạng
   thái thời gian thực đặt ở Redis. Đây là điều kiện để mở rộng ngang.
4. **Cô lập lớp AI.** Mọi lời gọi mô hình ngôn ngữ đi qua interface `AiProvider` và lớp điều phối
   `AiOrchestrator`, nên đổi nhà cung cấp không ảnh hưởng tầng nghiệp vụ.
5. **Lưu trữ đa hệ.** Mỗi loại dữ liệu dùng hệ quản trị phù hợp: quan hệ cho dữ liệu giao dịch, đồ thị
   cho quan hệ hành vi, khoá–giá trị cho dữ liệu ngắn hạn và thông điệp thời gian thực.
6. **Tác vụ AI nặng chạy bất đồng bộ.** Sinh đề có thể mất hàng chục giây, nên chạy dưới dạng công việc
   nền và trả về mã công việc (`jobId`) ngay, thay vì giữ kết nối HTTP chờ.

**Cơ chế dự phòng nhà cung cấp AI.** Mọi lời gọi mô hình đi theo trình tự: gọi Gemini trước; nếu gặp lỗi
tạm thời (vượt hạn mức 429, lỗi máy chủ 5xx, hết thời gian chờ) thì chuyển sang Grok; nếu cả hai lỗi thì
trả về thông báo lỗi rõ ràng cho người dùng. **Cầu dao** (circuit breaker) của Resilience4j tránh việc
gọi lặp lại một nhà cung cấp đang lỗi.

**Về lựa chọn kiến trúc khối đơn (monolith).** Hệ thống được xây dựng thành **một khối phân lớp**, không
tách vi dịch vụ (microservices). Lý do: các module chia sẻ nhiều dữ liệu chung (người dùng, quiz, lượt làm
bài) nên tách ra sẽ phải gọi chéo qua mạng và mất giao dịch nội bộ; đồng thời một hệ thống do một người
phát triển trong hai tháng thì chi phí vận hành nhiều dịch vụ độc lập lớn hơn lợi ích thu được. Việc chia
module theo tính năng đã đủ để giữ ranh giới rõ ràng, và mở đường cho việc tách về sau nếu cần.

---

**Tóm kết chương 1.** Chương này đã trình bày cơ sở lý thuyết và làm rõ vai trò của từng công nghệ trong
hệ thống: nghiệp vụ quiz và bài học từ các nền tảng tương tự; mô hình ngôn ngữ lớn cùng ba hạn chế dẫn
tới lựa chọn kiến trúc RAG; ba hệ quản trị dữ liệu với ba mục đích khác nhau; WebSocket/STOMP cho thời
gian thực; và kiến trúc khối đơn phân lớp gắn kết các thành phần đó. Trên cơ sở này, chương 2 tiến hành
khảo sát nhu cầu, phân tích yêu cầu và thiết kế chi tiết hệ thống.
