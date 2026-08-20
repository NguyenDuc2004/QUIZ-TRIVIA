package com.datn.quizai.quiz.service;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.QuestionSource;
import com.datn.quizai.quiz.domain.QuestionOption;
import com.datn.quizai.quiz.domain.QuestionType;
import com.datn.quizai.quiz.repository.QuestionRepository;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.OwnershipGuard;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.file.service.UploadedImagePath;
import com.datn.quizai.quiz.dto.QuestionOptionRequest;
import com.datn.quizai.quiz.dto.QuestionRequest;
import com.datn.quizai.quiz.dto.TopicResponse;
import com.datn.quizai.quiz.dto.QuestionResponse;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Ngân hàng câu hỏi (FR-8, FR-9, FR-10).
 * Câu hỏi thuộc về người tạo và dùng lại được ở nhiều quiz.
 */
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public QuestionService(QuestionRepository questionRepository, UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionResponse> listMyBank(UUID ownerId, QuestionType type, Difficulty difficulty,
                                                    String topic, String keyword, Pageable pageable) {
        return PageResponse.of(
                questionRepository.findBank(ownerId, type, difficulty, lowerOrNull(topic),
                        likePattern(keyword), pageable),
                QuestionResponse::from);
    }

    /**
     * Danh sách chủ đề người dùng đã dùng, kèm số câu — để giao diện vừa lọc được vừa gợi ý lúc
     * soạn câu mới, thay vì bắt họ nhớ chính xác mình đã gõ chữ gì lần trước.
     */
    @Transactional(readOnly = true)
    public List<TopicResponse> listMyTopics(UUID ownerId) {
        return questionRepository.findTopics(ownerId).stream()
                .map(row -> new TopicResponse(row.getTopic(), row.getQuestionCount()))
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(UUID questionId, JwtService.AuthenticatedUser current) {
        Question question = findOrThrow(questionId);
        OwnershipGuard.assertCanManage(question.getOwner().getId(), current, "câu hỏi");
        return QuestionResponse.from(question);
    }

    @Transactional
    public QuestionResponse create(QuestionRequest request, UUID ownerId) {
        return create(request, ownerId, QuestionSource.MANUAL, null);
    }

    /**
     * Tạo câu hỏi kèm <b>nguồn gốc</b>.
     * <p>
     * Câu do AI sinh phải mang {@link QuestionSource#AI_GENERATED} và bản ghi audit (provider,
     * model). Không đánh dấu thì sau khi Creator duyệt, câu AI nằm lẫn với câu tự soạn và không
     * còn cách nào tách ra — mất luôn khả năng thống kê "AI đóng góp bao nhiêu phần ngân hàng đề"
     * lẫn khả năng rà lại nếu một model sinh ra hàng loạt câu sai.
     */
    @Transactional
    public QuestionResponse create(QuestionRequest request, UUID ownerId,
                                   QuestionSource source, String aiMetadata) {
        User owner = userRepository.getReferenceById(ownerId);

        Question question = new Question(owner, request.type(), request.content().trim());
        question.setSource(source);
        question.setAiMetadata(aiMetadata);
        applyRequest(question, request);

        return QuestionResponse.from(questionRepository.save(question));
    }

    @Transactional
    public QuestionResponse update(UUID questionId, QuestionRequest request,
                                   JwtService.AuthenticatedUser current) {
        Question question = findOrThrow(questionId);
        OwnershipGuard.assertCanManage(question.getOwner().getId(), current, "câu hỏi");

        question.setType(request.type());
        question.setContent(request.content().trim());
        // Thay toàn bộ lựa chọn: đơn giản và tránh lệch thứ tự khi người dùng sửa nhiều lần
        question.clearOptions();
        applyRequest(question, request);

        return QuestionResponse.from(question);
    }

    /**
     * Xóa câu hỏi khỏi ngân hàng. Nếu câu hỏi đang nằm trong quiz nào thì <b>từ chối (409)</b>
     * để không âm thầm làm hụt câu hỏi của quiz đã xuất bản — người dùng phải bỏ nó khỏi
     * các quiz đó trước.
     */
    @Transactional
    public void delete(UUID questionId, JwtService.AuthenticatedUser current) {
        Question question = findOrThrow(questionId);
        OwnershipGuard.assertCanManage(question.getOwner().getId(), current, "câu hỏi");

        long usages = questionRepository.countUsagesInQuizzes(questionId);
        if (usages > 0) {
            throw BusinessException.conflict(
                    "Câu hỏi đang được dùng trong " + usages + " quiz. Hãy bỏ nó khỏi các quiz đó trước khi xóa.");
        }

        questionRepository.delete(question);
    }

    private void applyRequest(Question question, QuestionRequest request) {
        question.setExplanation(request.explanation());
        // Cùng luật an toàn với ảnh bìa quiz: chỉ nhận ảnh đã tải lên hệ thống này (FR-11).
        question.setImageUrl(UploadedImagePath.hopLeHoacNull(request.imageUrl(), "Ảnh câu hỏi"));
        question.setRubric(request.rubric());
        question.setDifficulty(request.difficulty() == null ? Difficulty.MEDIUM : request.difficulty());
        question.setTopic(blankToNull(request.topic()));
        question.setPoints(request.points() == null ? 1 : request.points());
        question.setTimeLimitSec(request.timeLimitSec());

        List<QuestionOptionRequest> options = validateOptions(request.type(), request.options());
        for (QuestionOptionRequest option : options) {
            question.addOption(new QuestionOption(option.content().trim(), option.correct()));
        }
    }

    /**
     * Luật riêng của từng loại câu hỏi (FR-9). Đây là phần dễ sai nhất khi soạn đề nên
     * chặn ngay ở service, không phụ thuộc giao diện.
     *
     * @return danh sách lựa chọn đã chuẩn hóa cờ {@code correct}
     */
    private List<QuestionOptionRequest> validateOptions(QuestionType type, List<QuestionOptionRequest> options) {
        long correctCount = options.stream().filter(QuestionOptionRequest::correct).count();

        return switch (type) {
            case SINGLE_CHOICE -> {
                requireAtLeast(options, 2, "Câu một đáp án phải có ít nhất 2 lựa chọn");
                requireExactlyOneCorrect(correctCount, "Câu một đáp án phải có đúng 1 đáp án đúng");
                yield options;
            }
            case MULTIPLE_CHOICE -> {
                requireAtLeast(options, 3, "Câu nhiều đáp án phải có ít nhất 3 lựa chọn");
                if (correctCount < 2) {
                    throw BusinessException.badRequest("Câu nhiều đáp án phải có ít nhất 2 đáp án đúng");
                }
                if (correctCount == options.size()) {
                    throw BusinessException.badRequest("Câu nhiều đáp án phải có ít nhất 1 lựa chọn sai");
                }
                yield options;
            }
            case TRUE_FALSE -> {
                if (options.size() != 2) {
                    throw BusinessException.badRequest("Câu Đúng/Sai phải có đúng 2 lựa chọn");
                }
                requireExactlyOneCorrect(correctCount, "Câu Đúng/Sai phải có đúng 1 đáp án đúng");
                yield options;
            }
            // Mỗi lựa chọn là một đáp án được chấp nhận → bắt buộc đánh dấu đúng hết
            case FILL_BLANK -> {
                requireAtLeast(options, 1, "Câu điền chỗ trống phải có ít nhất 1 đáp án được chấp nhận");
                yield options.stream()
                        .map(o -> new QuestionOptionRequest(o.content(), true))
                        .toList();
            }
            // Chỉ lưu một đáp án mẫu để AI đối chiếu khi chấm (features/06)
            case SHORT_ANSWER -> {
                if (options.size() != 1) {
                    throw BusinessException.badRequest(
                            "Câu trả lời ngắn chỉ lưu đúng 1 đáp án mẫu để AI chấm đối chiếu");
                }
                yield List.of(new QuestionOptionRequest(options.getFirst().content(), true));
            }
        };
    }

    private void requireAtLeast(List<QuestionOptionRequest> options, int min, String message) {
        if (options.size() < min) {
            throw BusinessException.badRequest(message);
        }
    }

    private void requireExactlyOneCorrect(long correctCount, String message) {
        if (correctCount != 1) {
            throw BusinessException.badRequest(message);
        }
    }

    private Question findOrThrow(UUID questionId) {
        return questionRepository.findByIdWithOptions(questionId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy câu hỏi"));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String lowerOrNull(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toLowerCase();
    }

    /**
     * Mẫu LIKE chữ thường ({@code %vòng lặp%}). Ghép ở Java thay vì
     * {@code lower(concat(...))} trong JPQL vì tham số null làm PostgreSQL báo
     * {@code function lower(bytea) does not exist}.
     */
    private String likePattern(String keyword) {
        String normalized = blankToNull(keyword);
        return normalized == null ? null : "%" + normalized.toLowerCase() + "%";
    }
}
