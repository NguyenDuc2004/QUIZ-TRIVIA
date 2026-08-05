-- V3: làm bài quiz (attempt) & câu trả lời
-- Nguồn thiết kế: docs/database.md §1.2 · đặc tả: docs/features/03-gameplay.md

CREATE TABLE quiz_attempts (
    id           UUID        PRIMARY KEY,
    user_id      UUID        NOT NULL,
    quiz_id      UUID        NOT NULL,
    mode         VARCHAR(10) NOT NULL DEFAULT 'EXAM',
    status       VARCHAR(15) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Hạn nộp = started_at + time_limit_sec của quiz; NULL nếu quiz không giới hạn giờ (FR-16)
    expires_at   TIMESTAMPTZ,
    submitted_at TIMESTAMPTZ,
    total_score  INTEGER     NOT NULL DEFAULT 0,
    -- Chốt tại thời điểm bắt đầu: chủ quiz sửa câu hỏi sau đó cũng không đổi thang điểm bài đã làm
    max_score    INTEGER     NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_quiz_attempts_user   FOREIGN KEY (user_id) REFERENCES users (id)   ON DELETE CASCADE,
    CONSTRAINT fk_quiz_attempts_quiz   FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT ck_quiz_attempts_mode   CHECK (mode IN ('PRACTICE', 'EXAM')),
    CONSTRAINT ck_quiz_attempts_status CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EXPIRED')),
    CONSTRAINT ck_quiz_attempts_score  CHECK (total_score >= 0 AND max_score >= 0)
);

-- Lịch sử làm bài của một người, mới nhất trước
CREATE INDEX idx_quiz_attempts_user ON quiz_attempts (user_id, started_at DESC);
-- Bảng xếp hạng theo quiz (FR-19) chỉ xét bài đã nộp
CREATE INDEX idx_quiz_attempts_quiz ON quiz_attempts (quiz_id, status, total_score DESC);
-- Mỗi người chỉ có tối đa một bài đang làm dở trên cùng một quiz → gọi lại API là làm tiếp
CREATE UNIQUE INDEX uk_quiz_attempts_in_progress
    ON quiz_attempts (user_id, quiz_id)
    WHERE status = 'IN_PROGRESS';

-- Mỗi dòng là một câu trong đề của riêng bài làm này.
-- Sinh sẵn ngay khi bắt đầu để chốt đề: chủ quiz thêm/bớt câu sau đó không ảnh hưởng bài đang làm.
CREATE TABLE attempt_answers (
    id           UUID        PRIMARY KEY,
    attempt_id   UUID        NOT NULL,
    question_id  UUID        NOT NULL,
    order_index  INTEGER     NOT NULL DEFAULT 0,
    -- {"optionIds":[...]} với câu trắc nghiệm, {"text":"..."} với câu điền/tự luận; NULL = chưa trả lời
    user_answer  JSONB,
    is_correct   BOOLEAN,
    score        INTEGER     NOT NULL DEFAULT 0,
    max_score    INTEGER     NOT NULL DEFAULT 1,
    ai_feedback  TEXT,
    graded_by    VARCHAR(15) NOT NULL DEFAULT 'NOT_GRADED',
    answered_at  TIMESTAMPTZ,

    CONSTRAINT fk_attempt_answers_attempt  FOREIGN KEY (attempt_id)  REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_answers_question FOREIGN KEY (question_id) REFERENCES questions (id)     ON DELETE CASCADE,
    CONSTRAINT uk_attempt_answers          UNIQUE (attempt_id, question_id),
    CONSTRAINT ck_attempt_answers_graded   CHECK (graded_by IN ('NOT_GRADED', 'AUTO', 'PENDING_AI', 'AI', 'HUMAN'))
);

CREATE INDEX idx_attempt_answers_attempt ON attempt_answers (attempt_id, order_index);
