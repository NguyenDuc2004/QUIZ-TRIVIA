-- V4: ảnh bìa cho quiz
-- Đặc tả: docs/features/02-quiz-management.md (FR-11)

-- Lưu đường dẫn tương đối do server sinh (ví dụ /uploads/images/<uuid>.jpg),
-- không lưu tên file người dùng gửi lên. NULL = dùng khối màu tự sinh ở giao diện.
ALTER TABLE quizzes ADD COLUMN thumbnail_url VARCHAR(500);
