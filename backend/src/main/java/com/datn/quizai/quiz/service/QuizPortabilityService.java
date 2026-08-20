package com.datn.quizai.quiz.service;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.common.OwnershipGuard;
import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.quiz.domain.QuizQuestion;
import com.datn.quizai.quiz.domain.Visibility;
import com.datn.quizai.quiz.dto.QuestionOptionRequest;
import com.datn.quizai.quiz.dto.QuestionRequest;
import com.datn.quizai.quiz.dto.QuizPortableFormat;
import com.datn.quizai.quiz.dto.QuizRequest;
import com.datn.quizai.quiz.dto.QuizSummaryResponse;
import com.datn.quizai.quiz.repository.QuizRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Xuất và nhập quiz dưới dạng file (features/02, FR-12).
 *
 * <h3>Tính năng này để làm gì</h3>
 * Ba việc thật, không phải để có cho đủ mục:
 * <ul>
 *   <li><b>Sao lưu</b> một bộ đề đã soạn công phu, không phụ thuộc vào việc hệ thống còn chạy hay không.</li>
 *   <li><b>Chuyển giữa hai máy chủ</b> — máy dev và máy thật, hoặc máy của trường khác.</li>
 *   <li><b>Chia sẻ đề</b> cho đồng nghiệp mà không phải cho họ tài khoản.</li>
 * </ul>
 *
 * <h3>Nhập luôn TẠO MỚI, không bao giờ ghi đè</h3>
 * File nhập vào không mang id (xem {@link QuizPortableFormat}), nên mỗi lần nhập là một quiz mới thuộc về
 * người nhập. Cho phép ghi đè theo id thì một file cũ nhập nhầm sẽ <b>xoá mất công sức sửa đề của người
 * khác</b> mà không có cách nào lấy lại — và đó đúng là kiểu thao tác người ta hay làm nhầm nhất với chức
 * năng nhập file.
 *
 * <h3>Quiz nhập vào luôn PRIVATE</h3>
 * Không đọc {@code visibility} từ file. Nhập một file rồi thấy đề của mình lập tức xuất hiện ở mục Khám phá
 * cho cả thiên hạ xem là một bất ngờ không dễ chịu; muốn công khai thì bấm một nút nữa. Mặc định an toàn,
 * cùng nguyên tắc với quiz tạo mới.
 *
 * <h3>Nhập là "tất cả hoặc không có gì"</h3>
 * Một {@code @Transactional} bao cả quá trình. Nửa chừng hỏng mà đã ghi được năm câu thì người dùng có một
 * quiz cụt, không biết thiếu câu nào, và lần nhập lại tạo thêm một quiz cụt nữa.
 */
@Service
public class QuizPortabilityService {

    private static final Logger log = LoggerFactory.getLogger(QuizPortabilityService.class);

    /**
     * Chặn trên số câu mỗi file nhập.
     * <p>
     * Không có chặn thì một file 50 000 câu treo cả tiến trình nhập và ngốn hết bộ nhớ. Con số này rộng
     * gấp nhiều lần một bộ đề thật, nên nó chỉ chặn thứ bất thường.
     */
    private static final int TOI_DA_CAU_MOI_FILE = 500;

    private final QuizService quizService;
    private final QuestionService questionService;
    private final QuizRepository quizRepository;

    public QuizPortabilityService(QuizService quizService,
                                  QuestionService questionService,
                                  QuizRepository quizRepository) {
        this.quizService = quizService;
        this.questionService = questionService;
        this.quizRepository = quizRepository;
    }

    /** Xuất một quiz mình quản lý ra định dạng mang đi được. */
    @Transactional(readOnly = true)
    public QuizPortableFormat export(UUID quizId, JwtService.AuthenticatedUser current) {
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));
        OwnershipGuard.assertCanManage(quiz.getOwner().getId(), current, "quiz");

        return QuizPortableFormat.from(quiz, quiz.getQuizQuestions().stream()
                .sorted(Comparator.comparingInt(QuizQuestion::getOrderIndex))
                .map(QuizQuestion::getQuestion)
                .toList());
    }

    /**
     * Nhập một file thành quiz mới thuộc về người gọi.
     *
     * @return quiz vừa tạo
     */
    @Transactional
    public QuizSummaryResponse importQuiz(QuizPortableFormat file, JwtService.AuthenticatedUser current) {
        if (file.questions().size() > TOI_DA_CAU_MOI_FILE) {
            throw BusinessException.badRequest(
                    "File có quá nhiều câu hỏi (tối đa " + TOI_DA_CAU_MOI_FILE + ")");
        }
        if (file.formatVersion() > QuizPortableFormat.PHIEN_BAN) {
            // Từ chối rõ ràng thay vì cố đọc: file của bản mới hơn có thể chứa trường mà bản này không
            // hiểu, và đọc bừa sẽ im lặng làm mất đúng những trường đó.
            throw BusinessException.badRequest("File được tạo bởi phiên bản mới hơn của hệ thống ("
                    + file.formatVersion() + "), bản này chỉ đọc được tới phiên bản "
                    + QuizPortableFormat.PHIEN_BAN);
        }

        QuizSummaryResponse quiz = quizService.create(new QuizRequest(
                file.title().trim(),
                file.description(),
                null,                       // danh mục không mang theo: id danh mục của máy này vô nghĩa ở máy khác
                file.difficulty() == null ? Difficulty.MEDIUM : file.difficulty(),
                Visibility.PRIVATE,         // luôn riêng tư — xem javadoc lớp
                file.timeLimitSec(),
                null,                       // ảnh bìa trỏ vào uploads/ của máy cũ, mang sang là ảnh vỡ
                false                       // chế độ thi nghiêm ngặt do người nhập tự bật nếu muốn
        ), current.id());

        List<UUID> questionIds = file.questions().stream()
                .map(cau -> questionService.create(toRequest(cau), current.id()).id())
                .toList();

        // Trả về bản CHI TIẾT vừa gắn câu hỏi: `create` ở trên trả về quiz lúc chưa có câu nào, nên
        // questionCount của nó là 0 và giao diện sẽ hiện "quiz rỗng" ngay sau khi nhập thành công.
        var chiTiet = quizService.setQuestions(quiz.id(), questionIds, current);

        log.info("Người dùng {} nhập quiz {} với {} câu hỏi", current.id(), quiz.id(), questionIds.size());
        return chiTiet.quiz();
    }

    private QuestionRequest toRequest(QuizPortableFormat.CauHoi cau) {
        return new QuestionRequest(
                cau.type(),
                cau.content(),
                cau.explanation(),
                null,                       // ảnh không mang theo — xem javadoc QuizPortableFormat
                cau.rubric(),
                cau.difficulty() == null ? Difficulty.MEDIUM : cau.difficulty(),
                cau.topic(),
                cau.points() == null ? 1 : cau.points(),
                null,
                cau.options().stream()
                        .map(o -> new QuestionOptionRequest(o.content(), o.correct()))
                        .toList());
    }
}
