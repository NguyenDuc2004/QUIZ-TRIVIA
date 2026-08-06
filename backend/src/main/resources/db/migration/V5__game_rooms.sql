-- V5: phòng đấu trí thời gian thực
-- Nguồn thiết kế: docs/database.md §1.2 · đặc tả: docs/features/04-multiplayer-realtime.md
--
-- Hai bảng này chỉ giữ *metadata và kết quả cuối*. Toàn bộ trạng thái đang chơi
-- (câu hiện tại, ai đã trả lời, điểm tạm) nằm ở Redis key `room:{code}` để giảm độ trễ.

CREATE TABLE game_rooms (
    id          UUID        PRIMARY KEY,
    room_code   VARCHAR(8)  NOT NULL,
    host_id     UUID        NOT NULL,
    quiz_id     UUID        NOT NULL,
    status      VARCHAR(10) NOT NULL DEFAULT 'WAITING',
    -- Thời gian mỗi câu do host chọn khi mở phòng; NULL = theo `questions.time_limit_sec`
    -- của từng câu, không có nữa thì lấy mặc định của hệ thống
    seconds_per_question INTEGER,
    started_at  TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_game_rooms_host   FOREIGN KEY (host_id) REFERENCES users (id)   ON DELETE CASCADE,
    CONSTRAINT fk_game_rooms_quiz   FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT uk_game_rooms_code   UNIQUE (room_code),
    CONSTRAINT ck_game_rooms_status CHECK (status IN ('WAITING', 'PLAYING', 'FINISHED')),
    CONSTRAINT ck_game_rooms_seconds CHECK (seconds_per_question IS NULL OR seconds_per_question >= 5)
);

CREATE INDEX idx_game_rooms_host ON game_rooms (host_id, created_at DESC);

CREATE TABLE game_room_players (
    id          UUID        PRIMARY KEY,
    room_id     UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    final_score INTEGER     NOT NULL DEFAULT 0,
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_game_room_players_room FOREIGN KEY (room_id) REFERENCES game_rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_game_room_players_user FOREIGN KEY (user_id) REFERENCES users (id)      ON DELETE CASCADE,
    -- Một người chỉ có một chỗ trong phòng, vào lại phòng không tạo thêm dòng mới
    CONSTRAINT uk_game_room_players      UNIQUE (room_id, user_id)
);

CREATE INDEX idx_game_room_players_room ON game_room_players (room_id, final_score DESC);
