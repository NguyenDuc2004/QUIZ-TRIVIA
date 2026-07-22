---
name: rag-pipeline
description: Dùng khi hiện thực hoặc chỉnh pipeline RAG — nạp học liệu (Tika→chunk→embedding→pgvector) và truy xuất (similarity search) cho sinh đề & chatbot. Đảm bảo grounding và chống ảo giác.
---

# Pipeline RAG (Retrieval-Augmented Generation)

## Ingestion (nạp học liệu)
```
Tài liệu (PDF/DOCX/TXT)
  → Apache Tika trích text
  → chunk (kích thước ~500–1000 token, overlap ~10–20%)
  → embedding (Gemini embedding)
  → lưu material_chunks(content, embedding vector, metadata jsonb, material_id, chunk_index)
```
- Chạy **bất đồng bộ** (job nền); cập nhật `learning_materials.status`: processing → ready.
- Gắn `topic` để lọc phạm vi truy xuất.

## Retrieval + Generation
```
Truy vấn/chủ đề
  → embedding truy vấn
  → similarity search pgvector (cosine, LIMIT k) — lọc theo topic/material nếu có
  → ghép k đoạn thành ngữ cảnh
  → prompt: system + ngữ cảnh + (yêu cầu) → AiOrchestrator (dùng skill ai-orchestrator-call)
```

## Grounding (chống ảo giác) — bắt buộc
- System prompt yêu cầu: "Chỉ trả lời dựa trên NGỮ CẢNH được cung cấp; nếu thiếu thông tin, nói rõ."
- Khi sinh đề: yêu cầu câu hỏi bám nội dung học liệu; kèm trích dẫn nguồn nếu có.

## pgvector
```sql
CREATE EXTENSION IF NOT EXISTS vector;
-- similarity: ORDER BY embedding <=> :queryEmbedding LIMIT :k
CREATE INDEX ON material_chunks USING ivfflat (embedding vector_cosine_ops);
```

## Checklist
- [ ] Ingestion chạy nền, có trạng thái.
- [ ] Chunk có overlap; embedding cùng model với truy vấn.
- [ ] Retrieval lọc theo topic khi phù hợp.
- [ ] Prompt có ràng buộc grounding.

## Tham chiếu
`docs/features/05-ai-rag-generation.md`, `08-ai-chatbot-rag.md`, `docs/database.md` mục 1.3.
