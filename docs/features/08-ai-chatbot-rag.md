# 08 — Trợ lý học tập thông minh (RAG Chatbot)

**Ưu tiên:** [M] Must · **Trụ cột phiếu:** Generative AI + RAG

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
