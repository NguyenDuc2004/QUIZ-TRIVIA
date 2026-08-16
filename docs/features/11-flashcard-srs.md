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
- **FR-37** [S] ✅ CRUD bộ thẻ (deck) và flashcard (mặt trước/mặt sau, gợi ý, chủ đề).
- **FR-38** [S] ⏳ **AI sinh flashcard** từ học liệu (dùng pipeline RAG) hoặc từ một quiz/chủ đề.
- **FR-39** [S] ✅ Tự động tạo flashcard từ câu trả lời sai (`attempt_answers.is_correct = false`).
- **FR-40** [S] ✅ **Lặp lại ngắt quãng (SRS):** lên lịch ôn theo thuật toán SM-2 rút gọn.
- **FR-41** [S] ✅ Phiên ôn tập: hiển thị thẻ đến hạn, người học tự đánh giá mức nhớ (Again/Hard/Good/Easy).
- **FR-42** [C] ✅ Thống kê ôn tập: số thẻ đã thuộc, số đến hạn, dự báo khối lượng 7 ngày tới.

### Ba quyết định của bản này

| Quyết định | Vì sao |
|---|---|
| **Trạng thái ôn là bảng riêng** `flashcard_reviews`, không phải cột trên `flashcards` | Một thẻ có thể được nhiều người ôn với lịch riêng. Nhét `due_date`/`ease_factor` vào `flashcards` thì hai người ôn cùng bộ sẽ ghi đè lịch của nhau — và phần khó nhất của việc chia sẻ bộ thẻ về sau đã giải quyết sẵn |
| **FR-39 không gọi AI** | Nội dung câu hỏi, đáp án đúng và giải thích đã có trong cơ sở dữ liệu. Gọi mô hình chỉ tốn hạn mức để viết lại thứ có sẵn, và thêm một đường cho nó bịa nội dung khác với đáp án thật |
| **Sửa nội dung thẻ KHÔNG đặt lại lịch ôn** | Người dùng thường chỉ sửa lỗi chính tả hoặc diễn đạt lại. Mất tiến độ ôn vì một lần sửa chữ là hình phạt không ai muốn; muốn học lại từ đầu thì xoá rồi thêm mới — hành động đó rõ ràng hơn |

**"Đã thuộc" là khoảng ôn ≥ 21 ngày** — ngưỡng quy ước của SM-2 cho ghi nhớ dài hạn, **không phải kết quả
đo** mức độ ghi nhớ. Giao diện phải ghi rõ ngưỡng thay vì chỉ ghi "đã thuộc", để không ngụ ý một phép đo
không tồn tại.

**Không có chia sẻ bộ thẻ trong bản này.** Chia sẻ kéo theo cả một tầng quyền mới (ai xem được, ai sửa
được, sao chép hay dùng chung) — đó là việc chưa làm, không phải việc bỏ.

**Loại câu sinh được thẻ:** trắc nghiệm, đúng/sai, điền khuyết. Riêng `SHORT_ANSWER` bị loại vì đáp án lưu
kèm nó là một *câu trả lời mẫu* dài để AI đối chiếu (đặt nguyên lên mặt sau thẻ thì thành đoạn văn không học
nổi), và `is_correct` của nó do AI chấm theo thang điểm nên "sai" có thể chỉ là thiếu một ý. Muốn sinh thẻ
từ tự luận thì phải nhờ AI rút gọn — đó là FR-38.

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
