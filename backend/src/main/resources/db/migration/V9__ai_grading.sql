-- ============================================================
--  V9 — AI chấm & giải thích câu tự luận (features/06, FR-30)
-- ============================================================
-- Nền đã có sẵn từ V3: attempt_answers.ai_feedback và graded_by. Lần này bổ sung phần còn thiếu để
-- chấm được thật: tiêu chí chấm ở phía câu hỏi, và chỗ lưu gợi ý cải thiện + trạng thái chấm hỏng.

-- ------------------------------------------------------------
-- 1. Tiêu chí chấm (rubric) của câu tự luận
-- ------------------------------------------------------------
-- Không có rubric thì mô hình tự nghĩ ra thang điểm của riêng nó, và hai lần chấm cùng một bài có
-- thể lệch nhau. Rubric do Creator soạn là thứ neo điểm số lại. Để NULL được vì câu trắc nghiệm
-- không cần, và Creator cũng có quyền không soạn (khi đó chấm theo đáp án mẫu).
ALTER TABLE questions ADD COLUMN rubric TEXT;

COMMENT ON COLUMN questions.rubric IS
    'Tiêu chí chấm câu tự luận, ví dụ "Nêu đủ 3 nguyên nhân: mỗi ý 3 điểm; diễn đạt rõ: 1 điểm".';

-- ------------------------------------------------------------
-- 2. Kết quả chấm chi tiết trên từng câu trả lời
-- ------------------------------------------------------------
-- Tách `ai_suggestions` khỏi `ai_feedback`: một bên là *nhận xét bài đã làm*, một bên là *việc cần
-- làm để khá hơn*. Gộp chung thành một đoạn văn thì giao diện không tách ra để nhấn mạnh được, và
-- mô hình cũng hay bỏ quên phần gợi ý khi chỉ được yêu cầu "viết nhận xét".
ALTER TABLE attempt_answers ADD COLUMN ai_suggestions TEXT;
ALTER TABLE attempt_answers ADD COLUMN graded_at      TIMESTAMPTZ;

COMMENT ON COLUMN attempt_answers.ai_suggestions IS 'Gợi ý cải thiện, tách khỏi nhận xét để hiển thị riêng.';
COMMENT ON COLUMN attempt_answers.graded_at      IS 'Thời điểm chấm xong — dùng để dò câu kẹt PENDING_AI quá lâu.';

-- ------------------------------------------------------------
-- 3. Trạng thái "AI chấm hỏng"
-- ------------------------------------------------------------
-- Gọi mô hình có thể thất bại (hết hạn mức, mạng lỗi, JSON sai định dạng). Không có trạng thái kết
-- thúc riêng thì câu đó nằm mãi ở PENDING_AI, người học nhìn thấy "đang chấm" vĩnh viễn mà không ai
-- biết là đã hỏng. AI_FAILED là trạng thái dừng: giao diện nói thật, và Creator vào chấm tay được.
ALTER TABLE attempt_answers DROP CONSTRAINT ck_attempt_answers_graded;
ALTER TABLE attempt_answers ADD  CONSTRAINT ck_attempt_answers_graded
    CHECK (graded_by IN ('NOT_GRADED', 'AUTO', 'PENDING_AI', 'AI', 'AI_FAILED', 'HUMAN'));

-- ------------------------------------------------------------
-- 4. Tìm nhanh những câu còn chờ chấm
-- ------------------------------------------------------------
-- Index bộ phận: chỉ đánh trên số ít bản ghi đang chờ, không phình theo toàn bộ lịch sử làm bài.
CREATE INDEX idx_attempt_answers_pending_ai
    ON attempt_answers (attempt_id)
    WHERE graded_by = 'PENDING_AI';
