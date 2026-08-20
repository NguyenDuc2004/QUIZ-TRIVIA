-- Chế độ thi nghiêm ngặt (features/12, FR-48).
--
-- ĐẶT Ở QUIZ, KHÔNG ĐẶT Ở LƯỢT LÀM BÀI
--
-- Người quyết định mức nghiêm khắc là người ra đề, không phải người làm bài — nên cờ này thuộc về quiz.
-- Đặt ở `quiz_attempts` thì người làm bài tự chọn được, tức tự tắt được, và cả tính năng thành trang trí.
--
-- MẶC ĐỊNH FALSE, KHÔNG PHẢI TRUE
--
-- Bật sẵn cho mọi quiz cũ là đổi hành vi của những bài thi đang chạy mà chủ quiz không hề biết: người học
-- đang làm bài bình thường bỗng bị ép toàn màn hình. Ai cần thì bật.
--
-- CỜ NÀY CHỈ CÓ NGHĨA VỚI LƯỢT `EXAM`
--
-- Không có ràng buộc nào ở đây bắt được điều đó vì `mode` nằm ở bảng khác. Chốt ở tầng service: lượt
-- PRACTICE bỏ qua cờ này hoàn toàn, giống như tín hiệu chống gian lận cũng chỉ thu ở lượt EXAM.

ALTER TABLE quizzes
    ADD COLUMN strict_exam BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN quizzes.strict_exam IS
    'Chế độ thi nghiêm ngặt (FR-48): yêu cầu toàn màn hình và khoá chuột phải khi làm bài ở chế độ EXAM. '
    'Đây là RÀO CẢN MA SÁT chứ không phải khoá: trình duyệt không cho ép toàn màn hình, người dùng luôn '
    'bấm Esc thoát được, và chuột phải chặn được thì phím tắt vẫn mở được devtools. Giá trị thật của nó '
    'là làm việc gian lận trở nên CÓ CHỦ Ý và để lại tín hiệu FULLSCREEN_EXIT trong proctoring_events.';
