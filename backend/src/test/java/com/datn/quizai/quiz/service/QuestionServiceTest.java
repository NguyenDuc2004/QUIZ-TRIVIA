package com.datn.quizai.quiz.service;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.quiz.domain.Difficulty;
import com.datn.quizai.quiz.domain.Question;
import com.datn.quizai.quiz.domain.QuestionType;
import com.datn.quizai.quiz.dto.QuestionOptionRequest;
import com.datn.quizai.quiz.dto.QuestionRequest;
import com.datn.quizai.quiz.dto.QuestionResponse;
import com.datn.quizai.quiz.repository.QuestionRepository;
import com.datn.quizai.user.domain.Role;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit test luật soạn câu hỏi theo từng loại (FR-9) và kiểm quyền sở hữu.
 * Đây là phần dễ sai nhất khi soạn đề nên test kỹ từng loại.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestionServiceTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID QUESTION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuestionService questionService;

    // ===== Luật theo loại câu hỏi =====

    @Test
    @DisplayName("SINGLE_CHOICE: 2 lựa chọn, 1 đáp án đúng → tạo được")
    void shouldAcceptValidSingleChoice() {
        stubSave();

        QuestionResponse response = questionService.create(request(QuestionType.SINGLE_CHOICE,
                option("Hà Nội", true), option("Huế", false)), OWNER_ID);

        assertThat(response.options()).hasSize(2);
        assertThat(response.options().stream().filter(QuestionResponse.OptionResponse::correct)).hasSize(1);
    }

    @Test
    @DisplayName("SINGLE_CHOICE: chỉ 1 lựa chọn → 400")
    void shouldRejectSingleChoiceWithOneOption() {
        assertBadRequest(() -> questionService.create(
                        request(QuestionType.SINGLE_CHOICE, option("Duy nhất", true)), OWNER_ID),
                "Câu một đáp án phải có ít nhất 2 lựa chọn");
    }

    @Test
    @DisplayName("SINGLE_CHOICE: 2 đáp án đúng → 400")
    void shouldRejectSingleChoiceWithTwoCorrect() {
        assertBadRequest(() -> questionService.create(request(QuestionType.SINGLE_CHOICE,
                        option("A", true), option("B", true)), OWNER_ID),
                "Câu một đáp án phải có đúng 1 đáp án đúng");
    }

    @Test
    @DisplayName("MULTIPLE_CHOICE: 3 lựa chọn, 2 đúng → tạo được")
    void shouldAcceptValidMultipleChoice() {
        stubSave();

        QuestionResponse response = questionService.create(request(QuestionType.MULTIPLE_CHOICE,
                option("A", true), option("B", true), option("C", false)), OWNER_ID);

        assertThat(response.options()).hasSize(3);
    }

    @Test
    @DisplayName("MULTIPLE_CHOICE: chỉ 1 đáp án đúng → 400")
    void shouldRejectMultipleChoiceWithOneCorrect() {
        assertBadRequest(() -> questionService.create(request(QuestionType.MULTIPLE_CHOICE,
                        option("A", true), option("B", false), option("C", false)), OWNER_ID),
                "Câu nhiều đáp án phải có ít nhất 2 đáp án đúng");
    }

    @Test
    @DisplayName("MULTIPLE_CHOICE: tất cả đều đúng → 400 (phải còn ít nhất 1 lựa chọn sai)")
    void shouldRejectMultipleChoiceWithAllCorrect() {
        assertBadRequest(() -> questionService.create(request(QuestionType.MULTIPLE_CHOICE,
                        option("A", true), option("B", true), option("C", true)), OWNER_ID),
                "Câu nhiều đáp án phải có ít nhất 1 lựa chọn sai");
    }

    @Test
    @DisplayName("TRUE_FALSE: 3 lựa chọn → 400")
    void shouldRejectTrueFalseWithThreeOptions() {
        assertBadRequest(() -> questionService.create(request(QuestionType.TRUE_FALSE,
                        option("Đúng", true), option("Sai", false), option("Không rõ", false)), OWNER_ID),
                "Câu Đúng/Sai phải có đúng 2 lựa chọn");
    }

    @Test
    @DisplayName("FILL_BLANK: mọi đáp án đều được chuẩn hóa thành 'đúng'")
    void shouldForceAllFillBlankOptionsCorrect() {
        stubSave();

        QuestionResponse response = questionService.create(request(QuestionType.FILL_BLANK,
                option("Hà Nội", false), option("ha noi", false)), OWNER_ID);

        assertThat(response.options()).hasSize(2);
        assertThat(response.options()).allMatch(QuestionResponse.OptionResponse::correct);
    }

    @Test
    @DisplayName("SHORT_ANSWER: gửi 2 đáp án mẫu → 400")
    void shouldRejectShortAnswerWithTwoOptions() {
        assertBadRequest(() -> questionService.create(request(QuestionType.SHORT_ANSWER,
                        option("Đáp án mẫu 1", true), option("Đáp án mẫu 2", true)), OWNER_ID),
                "Câu trả lời ngắn chỉ lưu đúng 1 đáp án mẫu để AI chấm đối chiếu");
    }

    @Test
    @DisplayName("Không truyền độ khó/điểm → mặc định MEDIUM và 1 điểm")
    void shouldApplyDefaults() {
        stubSave();

        // Thứ tự: type, content, explanation, imageUrl, rubric, difficulty, topic, points, timeLimit, options
        QuestionRequest request = new QuestionRequest(QuestionType.TRUE_FALSE, "  1 + 1 = 2  ", null, null,
                null, null, "  ", null, null, List.of(option("Đúng", true), option("Sai", false)));

        QuestionResponse response = questionService.create(request, OWNER_ID);

        assertThat(response.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(response.points()).isEqualTo(1);
        assertThat(response.content()).isEqualTo("1 + 1 = 2");
        assertThat(response.topic()).isNull();
    }

    // ===== Quyền sở hữu =====

    @Test
    @DisplayName("Sửa câu hỏi của người khác → 403")
    void shouldReject403WhenUpdatingOthersQuestion() {
        given(questionRepository.findByIdWithOptions(QUESTION_ID))
                .willReturn(Optional.of(existingQuestion(OWNER_ID)));

        assertThatThrownBy(() -> questionService.update(QUESTION_ID,
                request(QuestionType.TRUE_FALSE, option("Đúng", true), option("Sai", false)),
                principal(OTHER_ID, Role.CREATOR)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Admin sửa được câu hỏi của người khác")
    void shouldAllowAdminToUpdateOthersQuestion() {
        given(questionRepository.findByIdWithOptions(QUESTION_ID))
                .willReturn(Optional.of(existingQuestion(OWNER_ID)));

        QuestionResponse response = questionService.update(QUESTION_ID,
                request(QuestionType.TRUE_FALSE, option("Đúng", true), option("Sai", false)),
                principal(OTHER_ID, Role.ADMIN));

        assertThat(response.type()).isEqualTo(QuestionType.TRUE_FALSE);
    }

    @Test
    @DisplayName("Xóa câu hỏi đang nằm trong quiz → 409 kèm số quiz đang dùng")
    void shouldReject409WhenDeletingQuestionUsedByQuizzes() {
        given(questionRepository.findByIdWithOptions(QUESTION_ID))
                .willReturn(Optional.of(existingQuestion(OWNER_ID)));
        given(questionRepository.countUsagesInQuizzes(QUESTION_ID)).willReturn(2L);

        assertThatThrownBy(() -> questionService.delete(QUESTION_ID, principal(OWNER_ID, Role.CREATOR)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang được dùng trong 2 quiz");

        then(questionRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("Xóa câu hỏi chưa dùng ở quiz nào → xóa thật")
    void shouldDeleteUnusedQuestion() {
        Question question = existingQuestion(OWNER_ID);
        given(questionRepository.findByIdWithOptions(QUESTION_ID)).willReturn(Optional.of(question));
        given(questionRepository.countUsagesInQuizzes(QUESTION_ID)).willReturn(0L);

        questionService.delete(QUESTION_ID, principal(OWNER_ID, Role.CREATOR));

        then(questionRepository).should().delete(question);
    }

    @Test
    @DisplayName("Câu hỏi không tồn tại → 404")
    void shouldReject404WhenQuestionMissing() {
        given(questionRepository.findByIdWithOptions(QUESTION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.get(QUESTION_ID, principal(OWNER_ID, Role.CREATOR)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không tìm thấy câu hỏi");
    }

    // ===== Helper =====

    private void stubSave() {
        given(userRepository.getReferenceById(OWNER_ID)).willReturn(owner(OWNER_ID));
        given(questionRepository.save(any(Question.class))).willAnswer(invocation -> {
            Question saved = invocation.getArgument(0);
            saved.setId(QUESTION_ID);
            return saved;
        });
    }

    private void assertBadRequest(Runnable action, String expectedMessage) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .hasMessage(expectedMessage)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private QuestionRequest request(QuestionType type, QuestionOptionRequest... options) {
        return new QuestionRequest(type, "Thủ đô Việt Nam là gì?", "Giải thích", null, null,
                Difficulty.EASY, "Địa lý", 2, null, List.of(options));
    }

    private QuestionOptionRequest option(String content, boolean correct) {
        return new QuestionOptionRequest(content, correct);
    }

    private User owner(UUID id) {
        User user = new User("creator@example.com", "hash", "Người Tạo", Role.CREATOR);
        user.setId(id);
        return user;
    }

    private Question existingQuestion(UUID ownerId) {
        Question question = new Question(owner(ownerId), QuestionType.SINGLE_CHOICE, "Câu cũ");
        question.setId(QUESTION_ID);
        return question;
    }

    private JwtService.AuthenticatedUser principal(UUID id, Role role) {
        return new JwtService.AuthenticatedUser(id, "user@example.com", role);
    }
}
