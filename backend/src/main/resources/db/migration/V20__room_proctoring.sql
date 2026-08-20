-- Cảnh báo gian lận trong phòng đấu real-time (features/12).
--
-- VÌ SAO PHẢI CÓ BẢNG RIÊNG, KHÔNG DÙNG `proctoring_events` CỦA V17
--
-- `proctoring_events` có `attempt_id NOT NULL` trỏ `quiz_attempts`, và `user_id NOT NULL` trỏ `users`.
-- Phòng đấu vi phạm CẢ HAI:
--   1. Phòng đấu không tạo dòng `quiz_attempts` nào — điểm nằm ở `game_room_players`.
--   2. Khách vãng lai không có dòng `users`; họ vào bằng mã PIN và một khoá phiên Redis.
-- Nhồi một `attempt_id` giả để lách ràng buộc là làm hỏng ý nghĩa của bảng cũ: mọi truy vấn thống kê
-- theo lượt thi sẽ đếm cả những dòng không thuộc lượt thi nào.
--
-- KHÁC BIỆT VỀ BẢN CHẤT, KHÔNG CHỈ VỀ KHOÁ NGOẠI
--
-- Bảng V17 phục vụ *rà soát sau bài thi tính điểm*: có điểm rủi ro, có `PENDING → VALID/INVALID`, có
-- ghi chú của người rà soát. Bảng này phục vụ *một lời nhắc giữa ván chơi* — nên nó KHÔNG có trạng
-- thái rà soát và KHÔNG có điểm rủi ro. Thêm hai cột đó vào đây là hứa một quy trình xử lý mà phòng
-- đấu không có: ván xong là phòng tan, không ai quay lại kết luận gì.

CREATE TABLE room_proctoring_events
(
    id         UUID PRIMARY KEY,

    room_id    UUID        NOT NULL,

    -- Danh tính TRONG PHẠM VI PHÒNG: với thành viên đây là `users.id`, với khách là UUID ngẫu nhiên
    -- sinh lúc vào phòng. Cố ý KHÔNG có khoá ngoại tới `users`: một nửa người chơi không nằm ở đó.
    player_id  UUID        NOT NULL,

    -- Chốt tên tại thời điểm chơi. Với khách đây là nguồn duy nhất — họ không có tài khoản để tra lại.
    -- Cùng lý do với `game_room_players.display_name`.
    player_name VARCHAR(50),

    is_guest   BOOLEAN     NOT NULL DEFAULT FALSE,

    event_type VARCHAR(30) NOT NULL,

    -- Câu thứ mấy lúc tín hiệu xảy ra. Đây là cột làm nên KHUÔN LẶP: đếm số câu KHÁC NHAU có tín hiệu
    -- thì mới phân biệt được "một lần bị gián đoạn" với "lặp đi lặp lại mỗi câu".
    -- -1 = đang ở phòng chờ, chưa vào câu nào.
    question_index INTEGER  NOT NULL DEFAULT -1,

    occurred_at TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_room_proctoring_room FOREIGN KEY (room_id) REFERENCES game_rooms (id) ON DELETE CASCADE,

    -- Chỉ hai loại. `PASTE`/`COPY` không áp cho phòng đấu: câu hỏi hiện trên màn hình vài chục giây,
    -- việc cần chặn là tra cứu ở tab khác chứ không phải sao chép đề. Liệt kê tường minh để một tín
    -- hiệu client tự bịa loại mới không lọt xuống cơ sở dữ liệu.
    CONSTRAINT ck_room_proctoring_type CHECK (event_type IN ('TAB_HIDDEN', 'TAB_VISIBLE'))
);

-- Truy vấn duy nhất của bảng: "phòng này có những tín hiệu gì" cho bản tổng kết của host sau ván.
CREATE INDEX idx_room_proctoring_room ON room_proctoring_events (room_id, player_id);
