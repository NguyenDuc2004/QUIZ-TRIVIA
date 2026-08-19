-- ============================================================
--  V19 — Lớp học & giao bài (features/14)
-- ============================================================
-- Ba bảng mới + một cột thêm vào `quiz_attempts`.
--
-- Tính năng này KHÔNG dựng cơ chế làm bài mới. Nó là một lớp mỏng bọc quanh thứ đã có: học sinh làm bài
-- tập bằng đúng luồng `quiz_attempts` của features/03, và bảng theo dõi lớp là truy vấn của features/09
-- thêm một bộ lọc. Phần thật sự mới chỉ là "ai thuộc lớp nào" và "bài nào giao cho lớp nào".

CREATE TABLE classrooms (
    id          UUID         PRIMARY KEY,
    owner_id    UUID         NOT NULL,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    /*
     * Mã lớp để học sinh tự vào — KHÔNG dùng id.
     *
     * Sáu ký tự chữ-số, bỏ các ký tự dễ đọc nhầm (0/O, 1/I/L): mã này được đọc to trong lớp và chép tay
     * lên bảng, nên nhầm một ký tự là cả lớp vào sai chỗ. Cùng lý do với mã PIN phòng đấu, nhưng ở đây
     * dùng cả chữ vì lớp học sống lâu (một học kỳ) nên cần không gian mã lớn hơn nhiều.
     */
    class_code  VARCHAR(6)   NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_classrooms_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_classrooms_code  UNIQUE (class_code),
    CONSTRAINT ck_classrooms_code  CHECK (class_code ~ '^[A-Z2-9]{6}$')
);

CREATE INDEX idx_classrooms_owner ON classrooms (owner_id, created_at DESC);

CREATE TABLE classroom_members (
    id           UUID        PRIMARY KEY,
    classroom_id UUID        NOT NULL,
    user_id      UUID        NOT NULL,
    /*
     * STUDENT | CO_TEACHER (FR-59).
     *
     * Chủ nhiệm KHÔNG nằm trong bảng này — họ là `classrooms.owner_id`. Để chủ nhiệm thành một dòng
     * thành viên nữa thì có hai nguồn sự thật cho cùng một câu hỏi, và sớm muộn hai nguồn lệch nhau.
     */
    role         VARCHAR(12) NOT NULL DEFAULT 'STUDENT',
    joined_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_classroom_members_class FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_classroom_members_user  FOREIGN KEY (user_id)      REFERENCES users (id)      ON DELETE CASCADE,
    -- Vào lớp hai lần không tạo hai dòng: chốt ở CSDL vì học sinh bấm "Tham gia" hai lần là chuyện thường
    CONSTRAINT uk_classroom_members       UNIQUE (classroom_id, user_id),
    CONSTRAINT ck_classroom_members_role  CHECK (role IN ('STUDENT', 'CO_TEACHER'))
);

CREATE INDEX idx_classroom_members_user ON classroom_members (user_id);

CREATE TABLE assignments (
    id           UUID         PRIMARY KEY,
    classroom_id UUID         NOT NULL,
    quiz_id      UUID         NOT NULL,
    title        VARCHAR(200) NOT NULL,
    instruction  TEXT,
    -- NULL = mở ngay khi giao. Có giá trị = hẹn giờ mở, dùng khi giáo viên soạn trước cả tuần
    open_at      TIMESTAMPTZ,
    -- NULL = không có hạn. Đa số bài tập có hạn, nhưng bắt buộc thì giáo viên phải bịa một ngày
    due_at       TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_assignments_class FOREIGN KEY (classroom_id) REFERENCES classrooms (id) ON DELETE CASCADE,
    -- RESTRICT chứ không CASCADE: xoá một quiz đang được giao sẽ xoá luôn bài tập và mọi điểm gắn với nó.
    -- Chặn ở đây để giáo viên nhận lỗi rõ ràng thay vì mất dữ liệu trong im lặng.
    CONSTRAINT fk_assignments_quiz  FOREIGN KEY (quiz_id)      REFERENCES quizzes (id)    ON DELETE RESTRICT,
    CONSTRAINT ck_assignments_window CHECK (open_at IS NULL OR due_at IS NULL OR due_at > open_at)
);

CREATE INDEX idx_assignments_class ON assignments (classroom_id, created_at DESC);

/*
 * Gắn một lượt làm bài vào bài tập được giao.
 *
 * NULL = lượt tự luyện bình thường, và đó là đa số. Cột nullable trên bảng có sẵn thay vì một bảng nối
 * riêng: quan hệ là một-một (một lượt thuộc tối đa một bài tập), nên bảng nối chỉ thêm một phép join cho
 * mọi truy vấn thống kê mà không giải quyết gì.
 */
ALTER TABLE quiz_attempts
    ADD COLUMN assignment_id UUID,
    ADD CONSTRAINT fk_quiz_attempts_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE SET NULL;

-- Truy vấn nóng của bảng theo dõi lớp: mọi lượt của một bài tập.
CREATE INDEX idx_quiz_attempts_assignment ON quiz_attempts (assignment_id) WHERE assignment_id IS NOT NULL;

/*
 * Mỗi học sinh chỉ có MỘT lượt cho mỗi bài tập.
 *
 * Đặc tả không nói, nhưng để làm lại không giới hạn thì điểm bài tập mất hết ý nghĩa — ai kiên nhẫn hơn
 * thì điểm cao hơn. Chốt ở CSDL vì kiểm trong Java thua cuộc khi học sinh bấm hai tab cùng lúc.
 */
CREATE UNIQUE INDEX uk_quiz_attempts_assignment_user
    ON quiz_attempts (assignment_id, user_id) WHERE assignment_id IS NOT NULL;

COMMENT ON COLUMN classrooms.class_code IS
    'Mã 6 ký tự [A-Z2-9] — bỏ 0/O/1/I/L vì mã này được đọc to và chép tay.';

COMMENT ON INDEX uk_quiz_attempts_assignment_user IS
    'Mỗi học sinh một lượt cho mỗi bài tập. Không có nó thì làm lại nhiều lần và điểm bài tập vô nghĩa.';
