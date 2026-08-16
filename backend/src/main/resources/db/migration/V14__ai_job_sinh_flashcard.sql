-- ============================================================
--  V14 — Loại job mới: sinh flashcard bằng AI (features/11, FR-38)
-- ============================================================
-- `ai_jobs.type` có ràng buộc CHECK liệt kê từng giá trị, nên thêm một loại job đòi sửa ràng buộc.
-- Đây là lý do dùng CHECK thay vì bảng tra: mỗi loại job mới phải đi qua một migration có chủ ý, thay vì
-- lọt vào cơ sở dữ liệu chỉ vì ai đó thêm một giá trị enum trong Java.

ALTER TABLE ai_jobs DROP CONSTRAINT ck_ai_jobs_type;

ALTER TABLE ai_jobs ADD CONSTRAINT ck_ai_jobs_type
    CHECK (type IN ('INGEST_MATERIAL', 'GENERATE_QUESTIONS', 'GENERATE_FLASHCARDS'));

COMMENT ON COLUMN ai_jobs.type IS
    'INGEST_MATERIAL = trích văn bản, chia đoạn, sinh vector nhúng · '
    'GENERATE_QUESTIONS = sinh câu hỏi từ học liệu · '
    'GENERATE_FLASHCARDS = sinh thẻ ghi nhớ từ học liệu. Cả ba đều chạy nền và trả jobId vì đều gọi mô '
    'hình, tức đều có thể mất hàng chục giây và có thể phải chờ hết cửa sổ hạn mức.';
