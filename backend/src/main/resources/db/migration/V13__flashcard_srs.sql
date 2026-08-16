-- ============================================================
--  V13 — Flashcard & lặp lại ngắt quãng (features/11)
-- ============================================================
-- Ba bảng, tách theo đúng ranh giới sở hữu:
--   flashcard_decks   — bộ thẻ, thuộc một người tạo
--   flashcards        — nội dung thẻ, thuộc một bộ
--   flashcard_reviews — TRẠNG THÁI ÔN CỦA TỪNG NGƯỜI trên từng thẻ
--
-- Vì sao trạng thái ôn phải là bảng riêng chứ không phải cột trên `flashcards`: một thẻ dùng chung có
-- thể được nhiều người ôn, và mỗi người có lịch riêng. Nhét `due_date`/`ease_factor` vào `flashcards`
-- thì hai người ôn cùng bộ thẻ sẽ ghi đè lịch của nhau.

CREATE TABLE flashcard_decks (
    id          UUID         PRIMARY KEY,
    owner_id    UUID         NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    -- Chủ đề tự do, khớp cách `questions.topic` đang làm (features/02) để sau này gộp thống kê theo
    -- chủ đề giữa quiz và flashcard mà không phải chuẩn hoá lại.
    topic       VARCHAR(100),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_flashcard_decks_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_flashcard_decks_owner ON flashcard_decks (owner_id, created_at DESC);

CREATE TABLE flashcards (
    id         UUID        PRIMARY KEY,
    deck_id    UUID        NOT NULL,
    front      TEXT        NOT NULL,
    back       TEXT        NOT NULL,
    hint       TEXT,
    -- Nguồn gốc thẻ. Giữ lại vì nó trả lời được câu hỏi thực tế "thẻ này ở đâu ra": thẻ AI sinh cần
    -- được nhìn bằng mắt khác thẻ người tự viết, và thẻ sinh từ câu trả lời sai là bằng chứng cho
    -- vòng lặp "làm sai → ôn lại" mà tính năng này tồn tại để tạo ra.
    source     VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    -- Câu hỏi gốc khi thẻ sinh từ một câu trả lời sai; NULL với thẻ tự viết hoặc thẻ sinh từ học liệu.
    -- ON DELETE SET NULL: xoá câu hỏi thì thẻ vẫn còn giá trị ôn tập, chỉ mất đường truy về nguồn.
    question_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_flashcards_deck     FOREIGN KEY (deck_id)     REFERENCES flashcard_decks (id) ON DELETE CASCADE,
    CONSTRAINT fk_flashcards_question FOREIGN KEY (question_id) REFERENCES questions (id)       ON DELETE SET NULL,
    CONSTRAINT ck_flashcards_source   CHECK (source IN ('MANUAL', 'AI_GENERATED', 'FROM_WRONG_ANSWER')),
    CONSTRAINT ck_flashcards_front    CHECK (length(btrim(front)) > 0),
    CONSTRAINT ck_flashcards_back     CHECK (length(btrim(back)) > 0)
);

CREATE INDEX idx_flashcards_deck ON flashcards (deck_id, created_at);

-- Không tạo hai thẻ cùng một câu hỏi sai trong cùng bộ: người học làm sai câu đó nhiều lần là chuyện
-- thường, và mỗi lần lại sinh thêm một thẻ trùng thì bộ thẻ đầy rác trong khi lịch ôn vẫn chỉ cần một.
CREATE UNIQUE INDEX uk_flashcards_deck_question
    ON flashcards (deck_id, question_id) WHERE question_id IS NOT NULL;

CREATE TABLE flashcard_reviews (
    id               UUID        PRIMARY KEY,
    flashcard_id     UUID        NOT NULL,
    user_id          UUID        NOT NULL,
    -- SM-2: hệ số dễ, càng thấp thì khoảng ôn giãn càng chậm. 1.3 là sàn của thuật toán —
    -- thấp hơn nữa thì thẻ quay lại quá dày và người học không bao giờ thoát khỏi nó.
    ease_factor      NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    interval_days    INTEGER      NOT NULL DEFAULT 0,
    repetitions      INTEGER      NOT NULL DEFAULT 0,
    -- Ngày (không giờ): lịch ôn tính theo ngày nên dùng DATE để so sánh "đến hạn hôm nay" không phụ
    -- thuộc giờ trong ngày. Thẻ mới có due_date = hôm nay, tức đến hạn ngay.
    due_date         DATE         NOT NULL DEFAULT CURRENT_DATE,
    last_reviewed_at TIMESTAMPTZ,
    -- Tổng số lần đã ôn, KHÁC `repetitions`: repetitions bị reset về 0 mỗi lần trả lời sai, còn cột này
    -- chỉ tăng. Cần cả hai — một cái để chạy thuật toán, một cái để thống kê thật số lần đã ôn.
    total_reviews    INTEGER      NOT NULL DEFAULT 0,
    lapses           INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT fk_flashcard_reviews_card FOREIGN KEY (flashcard_id) REFERENCES flashcards (id) ON DELETE CASCADE,
    CONSTRAINT fk_flashcard_reviews_user FOREIGN KEY (user_id)      REFERENCES users (id)      ON DELETE CASCADE,
    -- Một người có đúng một trạng thái ôn trên một thẻ.
    CONSTRAINT uk_flashcard_reviews      UNIQUE (flashcard_id, user_id),
    CONSTRAINT ck_flashcard_reviews_ease CHECK (ease_factor >= 1.30)
);

-- Truy vấn nóng nhất của tính năng: "thẻ nào của tôi đến hạn hôm nay". Đặt user_id trước due_date vì
-- mọi lời gọi đều lọc theo người dùng trước, rồi mới tới ngày.
CREATE INDEX idx_flashcard_reviews_due ON flashcard_reviews (user_id, due_date);

COMMENT ON TABLE flashcard_reviews IS
    'Trạng thái lặp lại ngắt quãng của TỪNG người dùng trên TỪNG thẻ (SM-2 rút gọn). '
    'Một thẻ được nhiều người ôn với lịch riêng, nên trạng thái không thể nằm trên bảng flashcards.';

COMMENT ON COLUMN flashcards.source IS
    'MANUAL = người dùng tự viết · AI_GENERATED = sinh từ học liệu qua RAG · '
    'FROM_WRONG_ANSWER = sinh từ câu người học trả lời sai.';
