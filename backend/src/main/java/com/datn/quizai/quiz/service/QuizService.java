package com.datn.quizai.quiz.service;

import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.Quiz;
import com.datn.quizai.quiz.domain.QuizQuestion;
import com.datn.quizai.quiz.domain.Visibility;
import com.datn.quizai.quiz.repository.CategoryRepository;
import com.datn.quizai.quiz.repository.QuestionRepository;
import com.datn.quizai.quiz.repository.QuizRepository;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.OwnershipGuard;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.quiz.dto.QuizDetailResponse;
import com.datn.quizai.quiz.dto.QuizRequest;
import com.datn.quizai.quiz.dto.QuizSummaryResponse;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/** Nghiệp vụ quản lý quiz (FR-7) và gắn câu hỏi vào quiz theo thứ tự. */
@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public QuizService(QuizRepository quizRepository,
                       QuestionRepository questionRepository,
                       CategoryRepository categoryRepository,
                       UserRepository userRepository,
                       EntityManager entityManager) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    /** Danh sách quiz công khai — Guest gọi được. */
    @Transactional(readOnly = true)
    public PageResponse<QuizSummaryResponse> listPublic(UUID categoryId, Difficulty difficulty,
                                                       String keyword, Pageable pageable) {
        return PageResponse.of(
                quizRepository.findPublicQuizzes(categoryId, difficulty, likePattern(keyword), pageable),
                QuizSummaryResponse::from);
    }

    /** Quiz của chính người dùng, gồm cả quiz riêng tư. */
    @Transactional(readOnly = true)
    public PageResponse<QuizSummaryResponse> listMine(UUID ownerId, UUID categoryId, Difficulty difficulty,
                                                     String keyword, Pageable pageable) {
        return PageResponse.of(
                quizRepository.findOwnedQuizzes(ownerId, categoryId, difficulty, likePattern(keyword), pageable),
                QuizSummaryResponse::from);
    }

    /**
     * Thông tin giới thiệu quiz, <b>không kèm câu hỏi</b> — Guest xem được nếu quiz công khai.
     * Quiz riêng tư của người khác trả 404 (không phải 403) để không tiết lộ là nó tồn tại.
     */
    @Transactional(readOnly = true)
    public QuizSummaryResponse getSummary(UUID quizId, JwtService.AuthenticatedUser current) {
        Quiz quiz = quizRepository.findByIdWithOwner(quizId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));

        if (quiz.getVisibility() == Visibility.PRIVATE
                && !OwnershipGuard.canManage(quiz.getOwner().getId(), current)) {
            throw BusinessException.notFound("Không tìm thấy quiz");
        }

        return QuizSummaryResponse.from(quiz);
    }

    /** Chi tiết kèm câu hỏi và đáp án đúng — chỉ chủ sở hữu/Admin, dùng ở màn hình soạn quiz. */
    @Transactional(readOnly = true)
    public QuizDetailResponse getDetailForEditing(UUID quizId, JwtService.AuthenticatedUser current) {
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));
        OwnershipGuard.assertCanManage(quiz.getOwner().getId(), current, "quiz");
        return QuizDetailResponse.from(quiz);
    }

    @Transactional
    public QuizSummaryResponse create(QuizRequest request, UUID ownerId) {
        User owner = userRepository.getReferenceById(ownerId);

        Quiz quiz = new Quiz(owner, request.title().trim());
        applyRequest(quiz, request);

        return QuizSummaryResponse.from(quizRepository.save(quiz));
    }

    @Transactional
    public QuizSummaryResponse update(UUID quizId, QuizRequest request, JwtService.AuthenticatedUser current) {
        Quiz quiz = quizRepository.findByIdWithOwner(quizId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));
        OwnershipGuard.assertCanManage(quiz.getOwner().getId(), current, "quiz");

        quiz.setTitle(request.title().trim());
        applyRequest(quiz, request);

        return QuizSummaryResponse.from(quiz);
    }

    @Transactional
    public void delete(UUID quizId, JwtService.AuthenticatedUser current) {
        Quiz quiz = quizRepository.findByIdWithOwner(quizId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));
        OwnershipGuard.assertCanManage(quiz.getOwner().getId(), current, "quiz");

        // Xóa quiz chỉ xóa liên kết quiz_questions (ON DELETE CASCADE),
        // câu hỏi vẫn còn trong ngân hàng để dùng lại.
        quizRepository.delete(quiz);
    }

    /**
     * Đặt lại toàn bộ danh sách câu hỏi của quiz theo đúng thứ tự client gửi.
     * Câu hỏi phải thuộc quyền quản lý của người gọi (không gắn câu hỏi của người khác vào quiz mình).
     */
    @Transactional
    public QuizDetailResponse setQuestions(UUID quizId, List<UUID> questionIds,
                                           JwtService.AuthenticatedUser current) {
        Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy quiz"));
        OwnershipGuard.assertCanManage(quiz.getOwner().getId(), current, "quiz");

        // Bỏ trùng nhưng giữ nguyên thứ tự người dùng gửi
        List<UUID> orderedIds = new ArrayList<>(new LinkedHashSet<>(questionIds));

        // Xóa liên kết cũ rồi FLUSH ngay: nếu không, Hibernate chèn dòng mới trước khi
        // xóa dòng cũ và vi phạm ràng buộc uk_quiz_questions (quiz_id, question_id)
        // khi người dùng chỉ đổi thứ tự các câu hỏi cũ.
        quiz.getQuizQuestions().clear();
        entityManager.flush();

        if (orderedIds.isEmpty()) {
            return QuizDetailResponse.from(quiz);
        }

        Map<UUID, Question> found = questionRepository.findAllByIdWithOptions(orderedIds).stream()
                .collect(java.util.stream.Collectors.toMap(q -> q.getId(), Function.identity()));

        for (int index = 0; index < orderedIds.size(); index++) {
            UUID questionId = orderedIds.get(index);
            Question question = found.get(questionId);
            if (question == null) {
                throw BusinessException.notFound("Không tìm thấy câu hỏi " + questionId);
            }
            OwnershipGuard.assertCanManage(question.getOwner().getId(), current, "câu hỏi");
            quiz.getQuizQuestions().add(new QuizQuestion(quiz, question, index));
        }

        return QuizDetailResponse.from(quiz);
    }

    private void applyRequest(Quiz quiz, QuizRequest request) {
        quiz.setDescription(request.description());
        quiz.setDifficulty(request.difficulty() == null ? Difficulty.MEDIUM : request.difficulty());
        quiz.setVisibility(request.visibility() == null ? Visibility.PRIVATE : request.visibility());
        quiz.setTimeLimitSec(request.timeLimitSec());
        quiz.setThumbnailUrl(validThumbnailUrl(request.thumbnailUrl()));

        // null = client không gửi trường này → GIỮ NGUYÊN giá trị đang có. Đặt về false khi null sẽ âm thầm
        // tắt chế độ thi nghiêm ngặt mỗi lần một client cũ (hoặc một form thiếu trường) gọi cập nhật quiz.
        if (request.strictExam() != null) {
            quiz.setStrictExam(request.strictExam());
        }

        if (request.categoryId() == null) {
            quiz.setCategory(null);
        } else {
            quiz.setCategory(categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> BusinessException.badRequest("Danh mục không tồn tại")));
        }
    }

    /**
     * Chỉ chấp nhận ảnh bìa là đường dẫn nội bộ do {@code POST /api/v1/files/images} sinh ra.
     * <p>
     * Không nhận URL ngoài: ảnh bên thứ ba có thể chết bất cứ lúc nào, và mỗi lần người học mở
     * trang là gửi một request kèm IP sang máy chủ lạ — người tạo quiz nhúng được cả pixel theo dõi.
     * Chặn cả {@code ..} để không ai ghép được đường dẫn thoát ra ngoài thư mục ảnh.
     */
    private String validThumbnailUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("/uploads/") || trimmed.contains("..")) {
            throw BusinessException.badRequest("Ảnh bìa phải là ảnh đã tải lên hệ thống");
        }
        return trimmed;
    }

    /**
     * Chuẩn hóa từ khóa thành mẫu LIKE chữ thường ({@code %java%}).
     * <p>
     * Phải ghép mẫu ở đây thay vì gọi {@code lower(concat(...))} trong JPQL: khi tham số null,
     * PostgreSQL không suy được kiểu và báo {@code function lower(bytea) does not exist}.
     */
    private String likePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }
}
