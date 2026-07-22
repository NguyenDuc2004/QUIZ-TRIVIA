# 05 — AI sinh cấu trúc đề thi từ học liệu (RAG)

**Ưu tiên:** [M] Must · **Trụ cột phiếu:** Generative AI + RAG

## Mục tiêu
Tự động sinh bộ câu hỏi/đề thi có cấu trúc chuẩn, **bám sát tài liệu học liệu** thông qua kiến trúc RAG, giảm công soạn thủ công.

## Use case
- Creator tải lên tài liệu học liệu (PDF/DOCX/TXT).
- Creator yêu cầu sinh N câu hỏi theo loại/độ khó → duyệt & lưu.

## Yêu cầu chức năng
- **FR-29** [M] Sinh câu hỏi từ học liệu (RAG) hoặc từ chủ đề (text).
- Cấu hình: số lượng, loại câu hỏi, độ khó, ngôn ngữ.
- Human-in-the-loop: câu hỏi ở trạng thái nháp, Creator duyệt trước khi xuất bản.

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

## Ghi chú kỹ thuật
- **Structured output + validate + retry** khi LLM trả sai JSON.
- **Grounding:** yêu cầu LLM chỉ dùng ngữ cảnh truy xuất → giảm ảo giác.
- **Cache** theo hash(prompt) để tiết kiệm chi phí.
- Fallback Gemini → Grok (xem [architecture.md](../architecture.md) mục 5).
- Là đối tượng **đánh giá độ chính xác AI** — [roadmap.md](../roadmap.md) mục 2.3.
