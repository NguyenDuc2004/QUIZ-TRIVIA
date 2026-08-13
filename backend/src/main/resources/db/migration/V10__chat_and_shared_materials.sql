-- ============================================================
--  V10 — Trợ lý học tập RAG (features/08, FR-31)
-- ============================================================
-- Hai phần: chỗ lưu hội thoại, và một cột mở đường cho người học chạm được vào học liệu.

-- ------------------------------------------------------------
-- 1. Học liệu chia sẻ — điều kiện để chatbot phục vụ được Learner
-- ------------------------------------------------------------
-- features/08 nói "Learner hỏi khái niệm → nhận giải thích bám học liệu", nhưng học liệu là tài sản
-- riêng của Creator (`learning_materials.owner_id`) và truy vấn vector lọc theo đúng cột đó. Learner
-- không sở hữu học liệu nào, nên nếu để nguyên thì họ hỏi gì cũng truy xuất được CON SỐ KHÔNG — và
-- lúc đó mô hình sẽ trả lời bằng kiến thức nền của nó, tức là bịa. Bịa chính là thứ RAG sinh ra để
-- chống, nên đây không phải bất tiện nhỏ mà là lỗ hổng ở gốc.
--
-- Cột này là mức chia sẻ NHỎ NHẤT giải quyết được chuyện đó: Creator tự quyết tài liệu nào cho người
-- học đọc. Không mở mặc định — tài liệu đã tải lên trước khi có tính năng này giữ nguyên trạng thái
-- riêng tư, vì chủ của chúng chưa từng đồng ý chia sẻ.
ALTER TABLE learning_materials ADD COLUMN shared BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN learning_materials.shared IS
    'true = cho phép mọi người học hỏi trợ lý AI trên tài liệu này. Mặc định false: chia sẻ phải là '
    'hành động có ý thức của chủ tài liệu, không phải trạng thái mặc định.';

-- Truy vấn vector lọc `owner_id = ? OR shared = true` rồi mới xếp theo khoảng cách. Index một phần
-- chỉ trên dòng đã chia sẻ: số tài liệu chia sẻ luôn nhỏ hơn nhiều tổng số tài liệu.
CREATE INDEX idx_learning_materials_shared ON learning_materials (id) WHERE shared = true;

-- ------------------------------------------------------------
-- 2. Phiên hội thoại
-- ------------------------------------------------------------
-- Hội thoại phải có ngữ cảnh (FR-31), nên lịch sử phải nằm ở server chứ không ở bộ nhớ trình duyệt:
-- người dùng F5 hay đổi máy thì mạch hội thoại vẫn còn, và mỗi lượt hỏi mới cần đọc lại vài lượt
-- gần nhất để dựng prompt.
CREATE TABLE chat_sessions (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL,
    -- Đặt từ câu hỏi đầu tiên, cắt ngắn. Không nhờ mô hình đặt tên: tốn thêm một lượt gọi cho một
    -- nhãn mà người dùng chỉ cần để nhận ra phiên nào là phiên nào.
    title      VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_chat_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Danh sách phiên luôn xếp theo lần hoạt động gần nhất, không theo lúc tạo: phiên mở tuần trước mà
-- vẫn đang dùng thì phải nằm trên phiên mở hôm qua rồi bỏ đó.
CREATE INDEX idx_chat_sessions_user ON chat_sessions (user_id, updated_at DESC);

CREATE TABLE chat_messages (
    id         UUID        PRIMARY KEY,
    session_id UUID        NOT NULL,
    role       VARCHAR(10) NOT NULL,
    content    TEXT        NOT NULL,
    -- Nguồn học liệu đã dùng để trả lời, dạng [{"materialId":…,"title":…,"excerpt":…}].
    -- Lưu cùng câu trả lời chứ không tra lại lúc hiển thị: học liệu có thể bị xoá hoặc sửa sau đó,
    -- mà trích dẫn phải nói đúng thứ mô hình ĐÃ đọc lúc trả lời, không phải thứ hiện có bây giờ.
    sources    JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_chat_messages_session FOREIGN KEY (session_id)
        REFERENCES chat_sessions (id) ON DELETE CASCADE,
    CONSTRAINT ck_chat_messages_role CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX idx_chat_messages_session ON chat_messages (session_id, created_at);

COMMENT ON COLUMN chat_messages.sources IS
    'Trích dẫn học liệu đã dùng, chốt tại thời điểm trả lời. Không tra lại lúc hiển thị vì tài liệu '
    'có thể đã bị xoá hoặc sửa — trích dẫn phải nói đúng thứ mô hình đã đọc.';
