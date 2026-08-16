package com.datn.quizai.flashcard.service;

import com.datn.quizai.flashcard.domain.Flashcard;
import com.datn.quizai.flashcard.domain.FlashcardDeck;
import com.datn.quizai.flashcard.domain.FlashcardSource;
import com.datn.quizai.flashcard.repository.FlashcardRepository;
import com.datn.quizai.flashcard.repository.WrongAnswerRepository;
import com.datn.quizai.quiz.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Sinh thẻ ôn từ những câu người học đã trả lời sai (features/11, FR-39).
 * <p>
 * Đây là chức năng có lý do tồn tại rõ nhất của cả tính năng: nó khép vòng lặp <i>làm bài → sai → ôn lại
 * đúng chỗ sai</i>. Người học không phải tự nhớ mình sai câu nào, và thẻ sinh ra bám đúng vào lỗ hổng kiến
 * thức thật thay vì một danh sách chung chung.
 * <p>
 * <b>Không gọi AI.</b> Câu hỏi đã có nội dung, đáp án đúng và phần giải thích trong cơ sở dữ liệu — ghép
 * chúng thành hai mặt thẻ là việc của một câu SQL. Gọi mô hình ở đây chỉ tốn hạn mức để viết lại thứ đã có
 * sẵn, và thêm một đường cho nó bịa ra nội dung khác với đáp án thật.
 */
@Service
public class WrongAnswerCardService {

    private static final Logger log = LoggerFactory.getLogger(WrongAnswerCardService.class);

    /**
     * Số câu sai lấy mỗi lần.
     * <p>
     * Có chặn trên vì người học làm nhiều bài thì số câu sai lên tới hàng trăm, và đổ tất cả vào một bộ thẻ
     * tạo ra khối lượng ôn mà không ai làm nổi trong một ngày — lịch SRS cho mọi thẻ mới đến hạn ngay hôm
     * tạo. Ba mươi thẻ là một phiên ôn dài nhưng còn làm được.
     */
    private static final int TOI_DA_MOI_LAN = 30;

    private final WrongAnswerRepository wrongAnswerRepository;
    private final FlashcardRepository cardRepository;
    private final QuestionRepository questionRepository;
    private final FlashcardService flashcardService;

    public WrongAnswerCardService(WrongAnswerRepository wrongAnswerRepository,
                                  FlashcardRepository cardRepository,
                                  QuestionRepository questionRepository,
                                  FlashcardService flashcardService) {
        this.wrongAnswerRepository = wrongAnswerRepository;
        this.cardRepository = cardRepository;
        this.questionRepository = questionRepository;
        this.flashcardService = flashcardService;
    }

    /**
     * Kết quả sinh thẻ.
     *
     * @param soDaTao  số thẻ mới thêm được
     * @param soBoQua  số câu sai bị bỏ qua vì bộ thẻ này đã có thẻ cho câu đó. Trả về con số này thay vì
     *                 im lặng, để người dùng hiểu vì sao bấm lần hai lại ra 0 thẻ mới
     */
    public record KetQua(int soDaTao, int soBoQua) {
    }

    @Transactional
    public KetQua sinhVaoBo(UUID deckId, UUID userId) {
        FlashcardDeck deck = flashcardService.requireOwnedDeck(deckId, userId);
        var cauSai = wrongAnswerRepository.timCauTraLoiSai(userId, TOI_DA_MOI_LAN);

        int daTao = 0;
        int boQua = 0;
        for (var cau : cauSai) {
            if (cardRepository.existsByDeckIdAndQuestionId(deckId, cau.questionId())) {
                boQua++;
                continue;
            }

            Flashcard card = new Flashcard();
            card.setDeck(deck);
            card.setFront(cau.noiDung());
            card.setBack(matSau(cau));
            // Chủ đề của câu hỏi thành gợi ý: nó cho người học một đường vào khi bí, mà không tiết lộ đáp án
            card.setHint(cau.chuDe());
            card.setSource(FlashcardSource.FROM_WRONG_ANSWER);
            // getReferenceById: chỉ cần khoá ngoại nên không nạp cả entity. Tuyệt đối không dùng
            // `new Question()` rồi setId — entity đó ở trạng thái detached và Hibernate có thể coi nó là
            // bản ghi mới cần insert.
            card.setQuestion(questionRepository.getReferenceById(cau.questionId()));
            cardRepository.save(card);

            // Thẻ phải có trạng thái ôn ngay, nếu không nó chỉ nằm trong danh sách mà không vào phiên ôn
            flashcardService.taoTrangThaiOn(card, userId);
            daTao++;
        }

        log.info("Người dùng {} sinh {} thẻ từ câu trả lời sai vào bộ {} (bỏ qua {} câu đã có thẻ)",
                userId, daTao, deckId, boQua);
        return new KetQua(daTao, boQua);
    }

    /**
     * Mặt sau: đáp án đúng, kèm phần giải thích của câu hỏi nếu có.
     * <p>
     * Ghép giải thích vào chứ không bỏ đi, vì đó chính là thứ trả lời câu hỏi <i>"vì sao mình sai"</i> —
     * mà một thẻ chỉ ghi đáp án đúng thì không trả lời được.
     */
    private static String matSau(WrongAnswerRepository.CauSai cau) {
        String dapAn = cau.dapAnDung() == null ? "" : cau.dapAnDung().trim();
        String giaiThich = cau.giaiThich() == null ? "" : cau.giaiThich().trim();
        if (giaiThich.isEmpty()) {
            return dapAn;
        }
        return dapAn.isEmpty() ? giaiThich : dapAn + "\n\n" + giaiThich;
    }
}
