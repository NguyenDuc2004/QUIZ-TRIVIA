# 08 — Trợ lý học tập thông minh (RAG Chatbot)

**Ưu tiên:** [M] Must · **Trụ cột phiếu:** Generative AI + RAG · **Trạng thái:** ✅ đã hiện thực

## Mục tiêu
Gia sư ảo trả lời câu hỏi, giải thích khái niệm **dựa trên học liệu (RAG)**, và tạo mini-quiz theo yêu cầu, hỗ trợ người dùng ôn tập.

## Use case
- Learner hỏi khái niệm/kiến thức → nhận giải thích bám học liệu, có trích dẫn.
- Learner yêu cầu tạo nhanh vài câu hỏi ôn tập theo chủ đề.

## Yêu cầu chức năng
- **FR-31** [M] Trợ lý RAG hỏi-đáp trên học liệu, hội thoại có ngữ cảnh.
- Phản hồi **streaming** (SSE) để trải nghiệm mượt.
- Lưu lịch sử theo phiên chat.

## Luồng xử lý
```
Người dùng gửi tin nhắn → embedding → retrieval học liệu liên quan (pgvector)
   → prompt (system + ngữ cảnh + lịch sử phiên) → LLM (Gemini → Grok)
   → stream token về client (SSE) → lưu chat_messages
```

## Đặc điểm
- **RAG grounding:** trả lời bám nội dung học liệu, nêu rõ khi thiếu thông tin.
- **Context injection:** có thể tham chiếu kết quả/quiz của người dùng.
- **Guardrail:** giới hạn phạm vi học tập, lọc nội dung độc hại, chống prompt injection.

## API liên quan
[api.md](../api.md) mục 6 (`/ai/chat` — SSE, `/ai/chat/sessions`).

## Dữ liệu liên quan
`chat_sessions`, `chat_messages`, `material_chunks` (pgvector) — [database.md](../database.md).

## Ghi chú kỹ thuật
- Dùng chung pipeline RAG với [05-ai-rag-generation.md](05-ai-rag-generation.md).
- Streaming qua SSE (`text/event-stream`).
- Fallback Gemini → Grok; cache khi hợp lý.

---

## Lỗ hổng ở gốc: người học không sở hữu học liệu nào

Đặc tả viết *"Learner hỏi khái niệm → nhận giải thích bám học liệu"*, nhưng dữ liệu không đỡ được:
`learning_materials` chỉ có `owner_id`, không liên kết với quiz, và truy vấn vector lọc theo đúng cột
đó. Learner **không sở hữu học liệu nào**, nên mọi câu hỏi của họ truy xuất được **con số không** —
và mô hình sẽ trả lời bằng kiến thức nền của nó, tức là **bịa**, đúng thứ RAG sinh ra để chống.

Migration V10 thêm cột `learning_materials.shared`, mặc định **false**. Creator tự quyết tài liệu nào
cho người học đọc. Không mở mặc định: tài liệu tải lên trước khi có tính năng này giữ nguyên trạng
thái riêng tư, vì chủ của chúng chưa từng đồng ý chia sẻ.

Hai đường truy xuất tách bạch:

| Hàm | Dùng cho | Phạm vi |
|---|---|---|
| `searchSimilar` | sinh đề (features/05) | **chỉ tài liệu của chính mình** — soạn đề thì không có lý do lấy nội dung người khác |
| `searchSimilarIncludingShared` | trợ lý học tập | tài liệu của mình **+** tài liệu người khác đã bật `shared` |

## Streaming thật, không giả lập

`AiProvider.stream()` gọi `:streamGenerateContent?alt=sse` của Gemini. **Không** làm kiểu gọi
`complete()` rồi cắt nhỏ chuỗi trả về: thứ người dùng cảm nhận ở một trợ lý hội thoại là **thời gian
tới chữ đầu tiên**, mà cách giả lập giữ nguyên thời gian đó và chỉ thêm một lớp trang trí.

`alt=sse` là bắt buộc — thiếu nó Gemini trả về *một mảng JSON gửi dần*, mảnh đầu tiên là ký tự `[`,
không tách được từng mảnh nếu không tự viết bộ phân tích JSON theo luồng.

**Fallback Gemini → Grok chỉ hoạt động TRƯỚC mảnh đầu tiên.** Đây là giới hạn thật, không phải thiếu
sót: khi người dùng đã thấy chữ hiện ra, chuyển provider sẽ nối câu trả lời của mô hình A bằng câu
trả lời của mô hình B — hai mạch văn ghép vào nhau thành một đoạn vô nghĩa mà người đọc không biết
chỗ nối ở đâu. Thà dừng và báo lỗi. Cũng vì vậy mà ở đây không thử lại chính provider đó: thử lại
nghĩa là sinh lại từ đầu, và chữ đã phát ra thì không rút lại được.

## Ba lỗi chỉ lộ ra khi chạy thật

**1. Spring Security giết luồng SSE.** Nhịp dispatch `ASYNC` là phần tiếp của một request đã qua kiểm
quyền, nhưng bộ lọc JWT kế thừa `OncePerRequestFilter` nên cố tình không chạy lại → `SecurityContext`
trống → request bị chặn **giữa luồng**, lúc header đã gửi đi rồi:

```
Unable to handle the Spring Security Exception because the response is already committed
```

Sửa bằng `dispatcherTypeMatchers(ASYNC).permitAll()`. An toàn vì bên ngoài không tạo ra nhịp `ASYNC`
được — nó chỉ sinh ra từ một request đã được cho qua ở nhịp `REQUEST`.

**2. Mảnh token mất khoảng trắng đầu.** Chuẩn SSE quy định client bỏ *một* khoảng trắng đứng ngay sau
`data:`, mà mảnh của Gemini rất thường bắt đầu bằng khoảng trắng. Đo thật khi gửi chuỗi thô:

```
Gửi:  "Vòng lặp" " for" " dùng" " khi" " biết" " trước"
Nhận: "Vòng lặpfordùngkhibiếttrước"
```

Bọc mỗi mảnh trong JSON `{"t":"…"}` thì dấu ngoặc kép đứng ngay sau `data:`, không còn khoảng trắng
nào để bị bóc.

**3. Nhánh `materialId = null` của truy vấn vector lặng lẽ trả rỗng.** Câu SQL cũ viết
`(cast(? as uuid) is null or m.id = cast(? as uuid))` rồi truyền `null` vào. Đo thật: cùng một câu
hỏi, truyền `materialId` cụ thể ra **1 đoạn (khoảng cách 0.238)**, để `null` ra **0 đoạn** — không
lỗi, không cảnh báo, chỉ là kho vector coi như rỗng.

Lỗi này có **từ features/05** và ảnh hưởng cả hai tính năng RAG: trợ lý trả lời "không có tài liệu"
dù kho đầy, còn sinh đề với `useMaterials = true` nhưng không chọn tài liệu cụ thể thì sinh câu hỏi
từ kiến thức nền của mô hình. Sửa bằng cách ghép điều kiện ở Java thay vì truyền `null` vào SQL, và
thêm `MaterialChunkRepositoryTest` canh đúng nhánh đó.

## Ranh giới transaction

Một lượt hỏi gồm **hai lần ghi cách nhau hàng chục giây**: câu hỏi ghi ngay, câu trả lời ghi khi
luồng token kết thúc. Không gói cả hai vào một transaction — giữ transaction mở suốt thời gian gọi mô
hình là chiếm một kết nối CSDL để chờ mạng, chỉ vài người dùng đồng thời là cạn pool.

Nên: `prepare()` chạy trong transaction ngắn (mở phiên, lưu câu hỏi, truy xuất học liệu), rồi
`ChatMessageWriter` mở transaction **mới** (`REQUIRES_NEW`) lúc ghi câu trả lời. Bean riêng vì hai lý
do độc lập: lời gọi đến từ luồng của WebClient (ngoài mọi transaction), và Spring bỏ qua
`@Transactional` khi gọi phương thức trong cùng một bean.

`ChatMessageWriter` **nuốt lỗi và chỉ ghi log**: người dùng đã đọc câu trả lời trên màn hình rồi, ném
lỗi ở đó chỉ làm luồng kết thúc bằng thông báo hỏng ngay sau một câu trả lời hoàn chỉnh — họ sẽ tưởng
câu vừa đọc là sai. Mất một dòng lịch sử nhẹ hơn nhiều.

## Chống ảo giác và chống prompt injection

- **Không có ngữ cảnh thì prompt nói thẳng là không có**, và chỉ dẫn hệ thống cấm lấp chỗ trống bằng
  kiến thức nền. Đây là điểm sống còn: mô hình vẫn "biết" rất nhiều thứ ngoài học liệu, im lặng ở chỗ
  này chính là lúc nó tự do bịa.
- **Ngưỡng khoảng cách 0.75**: không có ngưỡng thì truy vấn *luôn* trả về 5 đoạn, kể cả khi kho toàn
  tài liệu Toán mà người ta hỏi Lịch sử. Prompt khi đó có ngữ cảnh sai và mô hình sẽ cố trả lời từ nó.
- **Học liệu và câu hỏi đều là dữ liệu**, rào trong khối mốc riêng; mốc xuất hiện trong dữ liệu người
  dùng bị vô hiệu hoá. Khác chấm tự luận ở một điểm: người hỏi *được phép* ra yêu cầu về cách trình
  bày, nên chỉ chặn hẹp — đòi đổi vai, đòi lộ chỉ dẫn hệ thống, đòi bỏ giới hạn phạm vi.
- **Trích dẫn lưu kèm câu trả lời** trong `chat_messages.sources`, chốt tại thời điểm trả lời. Không
  tra lại lúc hiển thị: tài liệu có thể đã bị xoá hoặc sửa, mà trích dẫn phải nói đúng thứ mô hình
  *đã đọc* — nếu không thì nó là trích dẫn giả.

## Frontend

Trang `/assistant`, mở cho **mọi tài khoản đã đăng nhập**.

Dùng `fetch` + `ReadableStream` chứ **không** dùng `EventSource`: `EventSource` chỉ gửi được `GET` và
không đặt được header, nên không mang nổi `Authorization`. Endpoint này là `POST` và cần token — hai
điều `EventSource` không làm được cái nào. Đổi lại phải tự bóc định dạng SSE, việc đó nhỏ và có lợi:
`fetch` cho luôn `AbortSignal` để người dùng bấm **Dừng** giữa lúc mô hình đang trả lời.

Vì đi thẳng qua `fetch`, luồng này **không hưởng interceptor của axios** nên không được tự làm mới
token. Phải gọi `refreshSessionForRawFetch()` khi gặp 401 — dùng chung hàm làm mới của interceptor để
không có hai lượt refresh song song (backend luân chuyển refresh token, hai lượt song song là tự đăng
xuất một phiên còn cứu được).

Câu trả lời **luôn hiện kèm tài liệu đã dựa vào**, và khi không dựa vào tài liệu nào thì nói rõ điều
đó. Một trợ lý AI trả lời trôi chảy mà không cho biết lấy từ đâu sẽ được người học tin nhiều hơn mức
nó đáng được tin — đây là ứng dụng ôn thi, tin sai thì học sai.

## Ngoài phạm vi
- **Mini-quiz theo yêu cầu** (có trong phần Use case nhưng không có FR nào): sinh đề đã là một tính
  năng riêng với luồng duyệt trước khi lưu; nhét thêm vào hội thoại là làm hai đường cho một việc.
- **Giới hạn hạn mức theo từng người dùng**: hiện chỉ có hàng rào chung của `AiThrottleState`.
