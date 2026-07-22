# 11 — Flashcard & Lặp lại ngắt quãng (Spaced Repetition)

**Ưu tiên:** [S] Should · **Tận dụng:** pipeline RAG, dữ liệu hành vi

## Mục tiêu
Cho phép người học ôn tập bằng **flashcard** (thẻ ghi nhớ) với thuật toán **lặp lại ngắt quãng (SRS)** để ghi nhớ hiệu quả, và **tự động sinh flashcard** từ học liệu hoặc từ câu trả lời sai.

## Use case
- Learner tạo bộ thẻ (deck) thủ công.
- Learner **tự sinh flashcard bằng AI** từ tài liệu học liệu đã upload.
- Hệ thống tự tạo flashcard từ các câu người học **trả lời sai** để ôn lại.
- Mỗi ngày, Learner ôn các thẻ "đến hạn" (due) theo lịch SRS.

## Yêu cầu chức năng
- **FR-37** [S] CRUD bộ thẻ (deck) và flashcard (mặt trước/mặt sau, gợi ý, tag, chủ đề).
- **FR-38** [S] **AI sinh flashcard** từ học liệu (dùng pipeline RAG) hoặc từ một quiz/chủ đề.
- **FR-39** [S] Tự động tạo flashcard từ câu trả lời sai (`attempt_answers.is_correct = false`).
- **FR-40** [S] **Lặp lại ngắt quãng (SRS):** lên lịch ôn theo thuật toán SM-2 (hoặc biến thể).
- **FR-41** [S] Phiên ôn tập: hiển thị thẻ đến hạn, người học tự đánh giá mức nhớ (Again/Hard/Good/Easy).
- **FR-42** [C] Thống kê ôn tập: số thẻ đã thuộc, chuỗi ngày ôn (streak), dự báo khối lượng ôn.

## Thuật toán SRS (SM-2 rút gọn)
Mỗi flashcard giữ trạng thái ôn tập của từng người dùng:
- `ease_factor` (mặc định 2.5), `interval` (ngày), `repetitions`, `due_date`.
- Sau mỗi lần ôn, người dùng chọn chất lượng nhớ `q ∈ {0..5}` (map từ Again/Hard/Good/Easy):
  - Nếu `q < 3` → reset `repetitions=0`, `interval=1` (ôn lại sớm).
  - Nếu `q ≥ 3` → tăng `repetitions`, tính `interval` mới (1 → 6 → `interval*ease`), cập nhật `ease_factor`.
  - `due_date = hôm nay + interval`.

## Luồng xử lý
### Sinh flashcard bằng AI (RAG)
```
Chọn tài liệu/chủ đề → retrieval học liệu liên quan (pgvector)
   → prompt yêu cầu cặp (mặt trước / mặt sau) dạng JSON
   → AiOrchestrator (Gemini→Grok) → validate → thẻ nháp → người dùng duyệt & lưu
```
### Phiên ôn tập
```
GET thẻ due (due_date ≤ hôm nay) → hiển thị lần lượt
   → người dùng đánh giá mức nhớ → cập nhật SRS (ease, interval, due_date)
```

## API liên quan
```
GET/POST/PUT/DELETE /api/v1/decks              Quản lý bộ thẻ
GET/POST/PUT/DELETE /api/v1/flashcards         Quản lý thẻ
POST   /api/v1/ai/generate-flashcards          Sinh thẻ từ học liệu/chủ đề (async → jobId)
GET    /api/v1/flashcards/due                   Lấy thẻ đến hạn ôn hôm nay
POST   /api/v1/flashcards/{id}/review           Gửi kết quả ôn { quality } → cập nhật SRS
GET    /api/v1/flashcards/stats                 Thống kê ôn tập
```

## Dữ liệu liên quan (bổ sung PostgreSQL)
- `flashcard_decks(id, owner_id, title, topic, created_at)`
- `flashcards(id, deck_id, front, back, hint, tag, source: manual/ai/from_wrong_answer, created_at)`
- `flashcard_reviews(id, flashcard_id, user_id, ease_factor, interval_days, repetitions, due_date, last_reviewed_at)`

## Ghi chú kỹ thuật
- Tái dùng pipeline RAG (skill `rag-pipeline`) và AiOrchestrator (skill `ai-orchestrator-call`).
- `flashcard_reviews` là trạng thái SRS **theo từng người dùng** (một thẻ có thể được nhiều người ôn).
- Job nền quét thẻ đến hạn để gửi thông báo nhắc ôn (nếu làm thêm notification).
- Có thể phản ánh chủ đề yếu sang Neo4j (`WEAK_IN`) để đồng bộ với hệ gợi ý.
