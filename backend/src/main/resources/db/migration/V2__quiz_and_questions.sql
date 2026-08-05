-- V2: danh mục, quiz, ngân hàng câu hỏi
-- Nguồn thiết kế: docs/database.md §1.2 · đặc tả: docs/features/02-quiz-management.md

CREATE TABLE categories (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE quizzes (
    id              UUID         PRIMARY KEY,
    owner_id        UUID         NOT NULL,
    category_id     UUID,
    title           VARCHAR(200) NOT NULL,
    description     VARCHAR(2000),
    difficulty      VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    visibility      VARCHAR(10)  NOT NULL DEFAULT 'PRIVATE',
    is_ai_generated BOOLEAN      NOT NULL DEFAULT FALSE,
    time_limit_sec  INTEGER,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_quizzes_owner      FOREIGN KEY (owner_id)    REFERENCES users (id)      ON DELETE CASCADE,
    CONSTRAINT fk_quizzes_category   FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL,
    CONSTRAINT ck_quizzes_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT ck_quizzes_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_quizzes_time_limit CHECK (time_limit_sec IS NULL OR time_limit_sec > 0)
);

CREATE INDEX idx_quizzes_owner      ON quizzes (owner_id);
CREATE INDEX idx_quizzes_category   ON quizzes (category_id);
-- Danh sách công khai luôn lọc visibility rồi sắp theo thời gian tạo
CREATE INDEX idx_quizzes_visibility ON quizzes (visibility, created_at DESC);

CREATE TABLE questions (
    id             UUID        PRIMARY KEY,
    owner_id       UUID        NOT NULL,
    type           VARCHAR(20) NOT NULL,
    content        TEXT        NOT NULL,
    explanation    TEXT,
    difficulty     VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    topic          VARCHAR(100),
    points         INTEGER     NOT NULL DEFAULT 1,
    time_limit_sec INTEGER,
    source         VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    ai_metadata    JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_questions_owner      FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_questions_type       CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE',
                                                       'FILL_BLANK', 'SHORT_ANSWER')),
    CONSTRAINT ck_questions_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT ck_questions_source     CHECK (source IN ('MANUAL', 'AI_GENERATED')),
    CONSTRAINT ck_questions_points     CHECK (points > 0)
);

CREATE INDEX idx_questions_owner ON questions (owner_id);
CREATE INDEX idx_questions_topic ON questions (topic);

-- Lựa chọn/đáp án của câu hỏi.
-- SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE : mỗi dòng là một lựa chọn, is_correct đánh dấu đáp án đúng.
-- FILL_BLANK                                  : mỗi dòng là một đáp án được chấp nhận (is_correct = TRUE).
-- SHORT_ANSWER                                : một dòng duy nhất chứa đáp án mẫu, dùng làm căn cứ khi AI chấm.
CREATE TABLE question_options (
    id          UUID    PRIMARY KEY,
    question_id UUID    NOT NULL,
    content     TEXT    NOT NULL,
    is_correct  BOOLEAN NOT NULL DEFAULT FALSE,
    order_index INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_question_options_question FOREIGN KEY (question_id)
        REFERENCES questions (id) ON DELETE CASCADE
);

CREATE INDEX idx_question_options_question ON question_options (question_id, order_index);

-- Bảng nối: một câu hỏi tái sử dụng được ở nhiều quiz, mỗi quiz có thứ tự riêng (FR-8)
CREATE TABLE quiz_questions (
    id          UUID    PRIMARY KEY,
    quiz_id     UUID    NOT NULL,
    question_id UUID    NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_quiz_questions_quiz     FOREIGN KEY (quiz_id)     REFERENCES quizzes (id)   ON DELETE CASCADE,
    CONSTRAINT fk_quiz_questions_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE,
    CONSTRAINT uk_quiz_questions          UNIQUE (quiz_id, question_id)
);

CREATE INDEX idx_quiz_questions_quiz ON quiz_questions (quiz_id, order_index);

-- Danh mục khởi tạo sẵn để dev/demo có dữ liệu dùng ngay
INSERT INTO categories (id, name, slug, description) VALUES
    (gen_random_uuid(), 'Toán học',        'toan-hoc',        'Đại số, giải tích, hình học'),
    (gen_random_uuid(), 'Tin học',         'tin-hoc',         'Lập trình, thuật toán, cơ sở dữ liệu'),
    (gen_random_uuid(), 'Tiếng Anh',       'tieng-anh',       'Từ vựng, ngữ pháp, đọc hiểu'),
    (gen_random_uuid(), 'Vật lý',          'vat-ly',          'Cơ học, điện học, quang học'),
    (gen_random_uuid(), 'Lịch sử',         'lich-su',         'Lịch sử Việt Nam và thế giới'),
    (gen_random_uuid(), 'Kiến thức chung', 'kien-thuc-chung', 'Đố vui, hiểu biết tổng hợp');
