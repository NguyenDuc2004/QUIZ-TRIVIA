-- V7: mã PIN 6 số + QR, cho phép khách vãng lai, avatar và trạng thái sẵn sàng
-- Đặc tả: docs/features/04-multiplayer-realtime.md

-- Host quyết định từng phòng có cho khách vào hay không.
-- Mặc định FALSE để giữ nguyên luật cũ (docs/overview.md): chưa đăng nhập thì không vào phòng đấu.
-- Bật lên thì phòng đó chấp nhận người quét QR vào chơi mà không cần tài khoản.
ALTER TABLE game_rooms ADD COLUMN allow_guests BOOLEAN NOT NULL DEFAULT FALSE;

-- Người chơi trong phòng giờ có thể là khách (không có tài khoản), nên:
--   * user_id được phép NULL
--   * display_name lưu ngay tại đây thay vì luôn phải join sang users
ALTER TABLE game_room_players ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE game_room_players ADD COLUMN display_name VARCHAR(50);
ALTER TABLE game_room_players ADD COLUMN avatar VARCHAR(40);
ALTER TABLE game_room_players ADD COLUMN is_guest BOOLEAN NOT NULL DEFAULT FALSE;

-- Điền tên cho những dòng đã có, để cột display_name luôn dùng được
UPDATE game_room_players p
SET display_name = u.display_name
FROM users u
WHERE u.id = p.user_id AND p.display_name IS NULL;

-- Ràng buộc UNIQUE (room_id, user_id) cũ không còn đúng: nhiều khách trong cùng phòng đều có
-- user_id NULL. PostgreSQL coi các NULL là khác nhau nên UNIQUE vẫn cho qua, nhưng ta khai lại
-- bằng chỉ mục một phần cho rõ ý: chỉ chặn trùng với người dùng đã đăng nhập.
ALTER TABLE game_room_players DROP CONSTRAINT uk_game_room_players;
CREATE UNIQUE INDEX uk_game_room_players_user
    ON game_room_players (room_id, user_id)
    WHERE user_id IS NOT NULL;

-- Người chơi phải là một trong hai: tài khoản thật, hoặc khách có tên hiển thị
ALTER TABLE game_room_players ADD CONSTRAINT ck_game_room_players_identity
    CHECK ((user_id IS NOT NULL AND is_guest = FALSE)
        OR (user_id IS NULL AND is_guest = TRUE AND display_name IS NOT NULL));
