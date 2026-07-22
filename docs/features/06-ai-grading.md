# 06 — AI chấm & giải thích câu tự luận

**Ưu tiên:** [M] Must · **Trụ cột phiếu:** AI

## Mục tiêu
Chấm điểm câu hỏi tự luận/điền khuyết không có đáp án cố định, kèm nhận xét và giải thích — điều quiz truyền thống không làm được.

## Use case
- Learner trả lời câu tự luận → AI chấm điểm + phản hồi.
- Creator xem và có thể chỉnh/ghi đè điểm AI.

## Yêu cầu chức năng
- **FR-30** [M] AI chấm & giải thích câu tự luận theo rubric.
- Điểm do AI chấm được đánh dấu rõ (`graded_by = ai`), Creator ghi đè được.

## Luồng xử lý
1. Câu có đáp án cố định → chấm bằng logic trước (so khớp/regex/near-match).
2. Câu tự luận → gọi AI với: câu hỏi, đáp án mẫu (nếu có), câu trả lời người học, rubric.
3. LLM trả JSON: `{ score, maxScore, isCorrect, feedback, suggestions }`.
4. Lưu vào `attempt_answers` (score, ai_feedback, graded_by).

## Đầu ra AI (JSON)
```json
{
  "score": 7,
  "maxScore": 10,
  "isCorrect": false,
  "feedback": "Trả lời đúng ý chính nhưng thiếu nguyên nhân X.",
  "suggestions": "Bổ sung phân tích về Y để trọn điểm."
}
```

## API liên quan
[api.md](../api.md) mục 6 (`/ai/grade`).

## Dữ liệu liên quan
`attempt_answers` (score, ai_feedback, graded_by) — [database.md](../database.md).

## Ghi chú kỹ thuật
- Với câu có đáp án cố định, chỉ dùng AI để **giải thích**, không để chấm → tiết kiệm chi phí.
- Fallback Gemini → Grok.
- Đánh giá độ chính xác: so điểm AI với người chấm — [roadmap.md](../roadmap.md) mục 2.3.
