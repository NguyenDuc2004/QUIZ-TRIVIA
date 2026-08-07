# 05 — AI sinh cấu trúc đề thi từ học liệu (RAG)

**Ưu tiên:** [M] Must · **Trụ cột phiếu:** Generative AI + RAG

## Mục tiêu
Tự động sinh bộ câu hỏi/đề thi có cấu trúc chuẩn, **bám sát tài liệu học liệu** thông qua kiến trúc RAG, giảm công soạn thủ công.

## Use case
- Creator tải lên tài liệu học liệu (PDF/DOCX/TXT).
- Creator yêu cầu sinh N câu hỏi theo loại/độ khó → duyệt & lưu.

## Yêu cầu chức năng
- **FR-29** [M] ✅ Sinh câu hỏi từ học liệu (RAG) hoặc từ chủ đề (text).
- ✅ Cấu hình: số lượng, loại câu hỏi, độ khó. *(Ngôn ngữ cố định tiếng Việt — chưa cho chọn.)*
- ✅ Human-in-the-loop: câu hỏi ở trạng thái nháp, Creator tích chọn rồi mới lưu vào ngân hàng.

## Kiến trúc RAG

### Pipeline nạp học liệu (ingestion)
```
Tài liệu → Apache Tika (trích text) → chunk (có overlap)
   → embedding (Gemini) → lưu vector + metadata vào pgvector (material_chunks)
```

### Pipeline sinh đề (retrieval + generation)
```
Chủ đề/yêu cầu → embedding truy vấn → similarity search (pgvector) lấy k đoạn
   → prompt (structured output JSON) + ngữ cảnh học liệu
   → AiOrchestrator (Gemini → Grok)
   → validate JSON theo schema → loại trùng/sai định dạng
   → câu hỏi nháp cho Creator duyệt
```

## Schema JSON đầu ra
```json
{
  "questions": [
    {
      "type": "single_choice",
      "question": "...",
      "options": ["...", "...", "...", "..."],
      "correctAnswer": "...",
      "explanation": "...",
      "difficulty": "medium",
      "topic": "..."
    }
  ]
}
```

## Luồng xử lý (bất đồng bộ)
1. Upload học liệu → job xử lý nền (ingestion) → status `ready`.
2. Yêu cầu sinh đề → trả `jobId` (202).
3. Job chạy retrieval + generation + validate.
4. Client poll `/ai/jobs/{jobId}` lấy kết quả → Creator duyệt.

## API liên quan
[api.md](../api.md) mục 6 (`/ai/materials`, `/ai/generate-questions`, `/ai/jobs/{jobId}`).

## Dữ liệu liên quan
`learning_materials`, `material_chunks` (pgvector), `questions` — [database.md](../database.md).

## Quyết định thiết kế (đã hiện thực)

**1. Tự viết lớp tích hợp, không dùng Spring AI / LangChain4j.** Đề tài yêu cầu như vậy, và tự viết
thì kiểm soát được đúng những gì gửi đi. `AiProvider` là giao diện chung; `GeminiProvider` và
`GrokProvider` có thân request khác hẳn nhau (Gemini dùng `contents/parts`, xAI tương thích OpenAI)
— đó chính là lý do phải có lớp trừu tượng.

**2. Fallback chỉ khi lỗi *tạm thời*.** 429 và 5xx thì chuyển provider; 4xx còn lại là do mình gửi
sai, gửi sang provider khác cũng hỏng y vậy mà lại tốn thêm một lần gọi.

**3. Grok không có API embedding.** xAI hiện chỉ có sinh văn bản. Nên khi Gemini chết, *sinh đề* vẫn
chạy được nhưng *nạp học liệu mới* thì không — đã ghi rõ trong `GrokProvider`.

**4. Chunk có chồng lấn, cắt theo ranh giới câu.** Cắt cứng theo độ dài rất dễ chặt ngang một ý;
cho hai đoạn liền nhau chia sẻ một phần đuôi/đầu thì ý bị chặt vẫn còn nguyên ở ít nhất một đoạn.
Mặc định 1500 ký tự / chồng lấn 200.

**5. Vector store dùng JdbcTemplate, không dùng JPA.** Hibernate không có kiểu `vector`, mọi thao
tác đều phải `cast(? as vector)` nên map entity chỉ thêm một lớp trung gian vô ích.

**6. `QuestionJsonParser` bỏ câu hỏng, giữ câu tốt.** Mô hình có thể trả JSON bọc trong ```json,
thiếu trường, đặt sai tên loại, sinh câu trắc nghiệm không có đáp án đúng, hoặc lặp lại cùng một ý.
Một câu sai không được làm hỏng cả mẻ — người dùng đã chờ hàng chục giây, trả về 8 câu dùng được
vẫn hơn báo lỗi toàn bộ. Lý do loại từng câu được trả về để họ biết vì sao xin 10 mà nhận 8.

**7. Câu AI sinh vẫn phải qua `QuestionService`.** Không insert thẳng vào bảng: câu do AI sinh phải
chịu đúng bộ luật của từng loại câu hỏi như câu soạn tay, nếu không sẽ có câu hỏng nằm trong ngân hàng.

**8. Job nền chạy *sau khi transaction commit*.** Dùng `@TransactionalEventListener` thay vì gọi
thẳng phương thức `@Async` — gọi thẳng thì luồng nền khởi động trong lúc transaction tạo học liệu
còn chưa commit và nó đọc CSDL không thấy dòng nào.

**9. Cập nhật trạng thái qua bean riêng** (`AiJobStatusWriter`, `MaterialStatusWriter`). Gọi
`this.method()` trong cùng một lớp không đi qua proxy nên `@Transactional` mất tác dụng; `REQUIRES_NEW`
để client hỏi giữa chừng thấy được `RUNNING` chứ không phải chờ tới lúc xong.

## Ghi chú kỹ thuật
- **Structured output + validate + retry** khi LLM trả sai JSON.
- **Grounding:** yêu cầu LLM chỉ dùng ngữ cảnh truy xuất → giảm ảo giác.
- **Cache** theo hash(prompt) để tiết kiệm chi phí.
- Fallback Gemini → Grok (xem [architecture.md](../architecture.md) mục 5).
- Là đối tượng **đánh giá độ chính xác AI** — [roadmap.md](../roadmap.md) mục 2.3.

## Fallback Gemini → Grok: đã hiện thực, chưa chạy thật

`AiOrchestrator` thử Gemini rồi chuyển sang Grok khi gặp lỗi **tạm thời** (429, 5xx, timeout). Cơ chế
xong và có test tự động che phủ, nhưng **chưa từng chạy thật**: xAI không có gói miễn phí, team mới
chưa nạp tín dụng thì mọi lời gọi trả 403 `permission-denied` (đo thật 08/08/2026).

`GROK_API_KEY` vì vậy để **trống** — và đó là lựa chọn có chủ ý, không phải quên. Điền key của team
không tín dụng còn tệ hơn: Gemini hết hạn mức sẽ chuyển sang Grok, Grok trả 403 (lỗi *không* tạm
thời) nên luồng dừng luôn, và thông báo hữu ích "hết hạn mức, chờ N giây" bị thay bằng "permission
denied". Để trống thì orchestrator lọc Grok ra ngay từ đầu.

Cũng lưu ý: `grok-2` trong cấu hình cũ **đã bị xAI gỡ** (`Model not found`). Đã đổi sang `grok-4.5`.
Đây là lỗi độc lập với chuyện tín dụng — dù có tiền, cấu hình cũ vẫn hỏng.
