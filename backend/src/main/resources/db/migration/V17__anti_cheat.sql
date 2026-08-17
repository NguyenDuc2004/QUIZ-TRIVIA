-- ============================================================
--  V17 — Chống gian lận thi trực tuyến (features/12)
-- ============================================================
-- Hai bảng: nhật ký sự kiện thô và bản tổng hợp cho mỗi lượt thi.
--
-- Ba ràng buộc đạo đức của đặc tả, và chúng định hình cả schema:
--   1. CHỈ chế độ thi (EXAM), không áp dụng luyện tập.
--   2. KHÔNG thu dữ liệu ngoài phạm vi bài thi — không ghi hình, không đọc nội dung dán, chỉ đếm.
--   3. Tín hiệu client GIẢ MẠO ĐƯỢC → chỉ để cảnh báo cho con người, KHÔNG tự động phạt.
--
-- Điểm 3 là lý do có cột `review_status` với mặc định `PENDING`: hệ thống không bao giờ tự kết luận một bài
-- là gian lận. Nó chỉ xếp bài đó vào hàng chờ người thật xem.

CREATE TABLE proctoring_events (
    id          UUID        PRIMARY KEY,
    attempt_id  UUID        NOT NULL,
    -- Lưu user_id dù suy được từ attempt: truy vấn thống kê hành vi theo người dùng là việc thường xuyên,
    -- và join qua quiz_attempts cho mỗi lần đếm là tốn vô ích.
    user_id     UUID        NOT NULL,
    event_type  VARCHAR(30) NOT NULL,
    -- Chi tiết KHÔNG chứa nội dung: với PASTE chỉ có độ dài, với ANSWER_TIMING chỉ có số giây.
    -- Ghi nội dung dán là thu thập dữ liệu ngoài phạm vi bài thi, và đó là điều đặc tả cấm.
    detail      JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_proctoring_events_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_proctoring_events_user    FOREIGN KEY (user_id)    REFERENCES users (id)         ON DELETE CASCADE,
    CONSTRAINT ck_proctoring_events_type    CHECK (event_type IN (
        'TAB_HIDDEN',        -- chuyển tab hoặc thu nhỏ cửa sổ (visibilitychange)
        'WINDOW_BLUR',       -- cửa sổ mất focus (blur)
        'COPY',              -- sao chép từ đề bài
        'PASTE',             -- dán vào ô trả lời
        'FULLSCREEN_EXIT',   -- thoát toàn màn hình khi đang ở chế độ nghiêm ngặt
        'ANSWER_TOO_FAST'    -- trả lời nhanh bất thường so với độ khó
    ))
);

CREATE INDEX idx_proctoring_events_attempt ON proctoring_events (attempt_id, occurred_at);

-- Nhật ký sự kiện lớn nhanh (một bài thi 30 phút có thể vài chục dòng). Chỉ mục theo người dùng để trang
-- báo cáo tra được lịch sử hành vi mà không quét toàn bảng.
CREATE INDEX idx_proctoring_events_user ON proctoring_events (user_id, occurred_at DESC);

CREATE TABLE attempt_integrity (
    id            UUID        PRIMARY KEY,
    -- Một lượt thi có đúng một bản tổng hợp. UNIQUE là chốt idempotent: tính lại không tạo dòng thứ hai.
    attempt_id    UUID        NOT NULL UNIQUE,
    risk_score    INTEGER     NOT NULL DEFAULT 0,
    -- Danh sách cờ dạng ["NHIEU_LAN_CHUYEN_TAB","DAN_NOI_DUNG_DAI"] — để giao diện hiện lý do cụ thể thay vì
    -- chỉ một con số. Một điểm rủi ro 70 mà không nói vì sao thì người rà soát không làm gì được với nó.
    flags         JSONB       NOT NULL DEFAULT '[]',
    -- Nhận định của mô hình. NULL khi chưa gọi hoặc gọi thất bại — KHÁC với chuỗi rỗng, vì "AI chưa xem" và
    -- "AI xem rồi không thấy gì" là hai chuyện khác nhau với người rà soát.
    ai_note       TEXT,
    review_status VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    reviewed_by   UUID,
    reviewed_at   TIMESTAMPTZ,
    review_note   TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_attempt_integrity_attempt  FOREIGN KEY (attempt_id)  REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    -- ON DELETE SET NULL: xoá tài khoản người rà soát không được xoá kết luận của họ
    CONSTRAINT fk_attempt_integrity_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id)         ON DELETE SET NULL,
    CONSTRAINT ck_attempt_integrity_status   CHECK (review_status IN ('PENDING', 'VALID', 'INVALID')),
    CONSTRAINT ck_attempt_integrity_score    CHECK (risk_score BETWEEN 0 AND 100)
);

-- Truy vấn nóng của trang quản trị: bài bị gắn cờ, chưa ai rà soát, điểm rủi ro cao trước.
CREATE INDEX idx_attempt_integrity_pending
    ON attempt_integrity (risk_score DESC) WHERE review_status = 'PENDING';

COMMENT ON TABLE proctoring_events IS
    'Nhật ký tín hiệu hành vi trong chế độ thi. KHÔNG chứa nội dung người dùng — với PASTE chỉ lưu độ dài, '
    'không lưu văn bản. Chỉ ghi cho lượt thi EXAM, không ghi cho luyện tập.';

COMMENT ON COLUMN attempt_integrity.review_status IS
    'PENDING = hệ thống gắn cờ, CHỜ NGƯỜI THẬT xem. Hệ thống không bao giờ tự kết luận gian lận: tín hiệu '
    'client giả mạo hoặc chặn được, nên chúng chỉ là cảnh báo hỗ trợ quyết định của con người.';
