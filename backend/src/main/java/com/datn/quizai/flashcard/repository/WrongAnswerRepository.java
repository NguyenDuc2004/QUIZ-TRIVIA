package com.datn.quizai.flashcard.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Tìm những câu người học đã trả lời sai, để sinh thẻ ôn lại (features/11, FR-39).
 * <p>
 * Viết bằng SQL thuần thay vì JPQL: câu này phải gộp đáp án đúng của câu trắc nghiệm thành một chuỗi
 * ({@code string_agg}) và bỏ trùng theo câu hỏi, hai việc mà JPQL diễn đạt rất vòng vo.
 */
@Repository
public class WrongAnswerRepository {

    private final JdbcTemplate jdbc;

    public WrongAnswerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Một câu trả lời sai, đã kèm sẵn nội dung cần cho hai mặt thẻ.
     *
     * @param dapAnDung đáp án đúng ghép từ các lựa chọn đúng; với câu tự luận thì đây là {@code rubric}
     *                  hoặc rỗng — xem ghi chú ở {@link #timCauTraLoiSai}
     */
    public record CauSai(UUID questionId, String noiDung, String dapAnDung, String giaiThich,
                         String chuDe) {
    }

    /**
     * Câu trả lời sai của một người, mới nhất trước, mỗi câu hỏi chỉ lấy một lần.
     * <p>
     * <b>Chỉ lấy câu có đáp án xác định</b> — trắc nghiệm, đúng/sai và điền khuyết. Riêng
     * {@code SHORT_ANSWER} bị loại vì hai lý do: đáp án lưu kèm nó là một <i>câu trả lời mẫu</i> dài để AI
     * đối chiếu, đặt nguyên lên mặt sau thẻ thì thành một đoạn văn không học nổi; và {@code is_correct} của
     * nó do AI chấm theo thang điểm, nên "sai" ở đây có thể chỉ là thiếu một ý. Muốn sinh thẻ từ tự luận
     * thì phải nhờ AI rút gọn lại — đó là việc của FR-38, không phải chỗ này.
     * <p>
     * {@code is_correct = false} chứ không phải {@code score = 0}: câu chấm một phần vẫn có điểm nhưng
     * không đúng, và đó vẫn là câu cần ôn lại.
     */
    public List<CauSai> timCauTraLoiSai(UUID userId, int gioiHan) {
        return jdbc.query("""
                select q.id                                  as question_id,
                       q.content                             as noi_dung,
                       string_agg(o.content, ' | ' order by o.order_index) as dap_an_dung,
                       q.explanation                         as giai_thich,
                       q.topic                               as chu_de
                from attempt_answers aa
                         join quiz_attempts qa on qa.id = aa.attempt_id
                         join questions q     on q.id = aa.question_id
                         -- Cột là `is_correct`; entity QuestionOption đặt tên trường là `correct` nên
                         -- JPQL viết `correct`, còn SQL thuần bắt buộc dùng tên cột thật
                         join question_options o on o.question_id = q.id and o.is_correct = true
                where qa.user_id = ?
                  and aa.is_correct = false
                  and q.type in ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'FILL_BLANK')
                group by q.id, q.content, q.explanation, q.topic
                order by max(aa.answered_at) desc nulls last
                limit ?
                """,
                (rs, i) -> new CauSai(
                        rs.getObject("question_id", UUID.class),
                        rs.getString("noi_dung"),
                        rs.getString("dap_an_dung"),
                        rs.getString("giai_thich"),
                        rs.getString("chu_de")),
                userId, gioiHan);
    }
}
