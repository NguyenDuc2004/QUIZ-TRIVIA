-- Ảnh cho từng câu hỏi (features/02, FR-11).
--
-- DÙNG LẠI NGUYÊN ĐƯỜNG TẢI ẢNH ĐÃ CÓ
--
-- `POST /api/v1/files/images` đã tồn tại cho ảnh bìa quiz: đã có kiểm kiểu tệp, giới hạn kích thước
-- riêng cho ảnh, và sinh đường dẫn nội bộ `/uploads/images/...`. Thêm một đường tải riêng cho ảnh câu
-- hỏi là nhân đôi cả ba thứ đó, và hai bản sao sẽ lệch nhau ở lần sửa đầu tiên.
--
-- CHỈ NHẬN ĐƯỜNG DẪN NỘI BỘ, KHÔNG NHẬN URL NGOÀI
--
-- Cùng ràng buộc với `quizzes.thumbnail_url`, và lý do không phải thẩm mỹ:
--   1. Ảnh bên thứ ba chết bất cứ lúc nào — đề thi mất hình giữa buổi kiểm tra.
--   2. Mỗi lần người học mở đề là một request kèm IP gửi sang máy chủ lạ; người soạn đề nhúng được
--      cả pixel theo dõi vào bài thi của người khác.
-- Ràng buộc chốt ở tầng service (`QuestionService`), giống `validThumbnailUrl` của quiz.

ALTER TABLE questions
    ADD COLUMN image_url VARCHAR(500);

COMMENT ON COLUMN questions.image_url IS
    'FR-11: ảnh minh hoạ của câu hỏi, đường dẫn nội bộ /uploads/images/... do POST /files/images sinh ra. '
    'NULL = câu hỏi chỉ có chữ. Không nhận URL ngoài — xem lý do ở V23.';
