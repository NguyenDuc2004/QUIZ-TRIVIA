package com.datn.quizai.ai.service;

import com.datn.quizai.ai.domain.AiJob;
import com.datn.quizai.ai.domain.AiJobType;
import com.datn.quizai.ai.dto.AiJobResponse;
import com.datn.quizai.ai.dto.ApproveQuestionsRequest;
import com.datn.quizai.ai.dto.GenerateQuestionsRequest;
import com.datn.quizai.ai.generation.GeneratedQuestion;
import com.datn.quizai.ai.generation.GenerationCommand;
import com.datn.quizai.ai.generation.QuestionGenerationService;
import com.datn.quizai.ai.repository.AiJobRepository;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.quiz.dto.QuestionOptionRequest;
import com.datn.quizai.quiz.dto.QuestionRequest;
import com.datn.quizai.quiz.dto.QuestionResponse;
import com.datn.quizai.quiz.service.QuestionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Điều phối các tác vụ AI chạy nền và bước Creator duyệt câu hỏi (docs/features/05).
 * <p>
 * Sinh đề mất hàng chục giây nên API trả {@code 202} kèm {@code jobId} thay vì giữ request.
 * Kết quả nằm ở {@code ai_jobs.result} tới khi Creator duyệt — <b>không</b> tự lưu vào ngân hàng
 * câu hỏi, vì AI có thể bịa và người ra đề phải là người quyết định.
 */
@Service
public class AiJobService {

    private static final Logger log = LoggerFactory.getLogger(AiJobService.class);

    private final AiJobRepository jobRepository;
    private final QuestionGenerationService generationService;
    private final QuestionService questionService;
    private final com.datn.quizai.user.repository.UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AiJobStatusWriter statusWriter;
    private final ApplicationEventPublisher eventPublisher;

    public AiJobService(AiJobRepository jobRepository,
                        QuestionGenerationService generationService,
                        QuestionService questionService,
                        com.datn.quizai.user.repository.UserRepository userRepository,
                        ObjectMapper objectMapper,
                        AiJobStatusWriter statusWriter,
                        ApplicationEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.generationService = generationService;
        this.questionService = questionService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.statusWriter = statusWriter;
        this.eventPublisher = eventPublisher;
    }

    /** Tạo job rồi trả về ngay; việc sinh đề chạy ở luồng nền. */
    @Transactional
    public AiJobResponse submitGeneration(GenerateQuestionsRequest request,
                                          JwtService.AuthenticatedUser current) {
        AiJob job = new AiJob(userRepository.getReferenceById(current.id()),
                AiJobType.GENERATE_QUESTIONS, toJson(request));
        jobRepository.save(job);

        // Phát sự kiện thay vì gọi thẳng: job nền chỉ chạy sau khi transaction này commit,
        // nếu không nó đọc CSDL sẽ chưa thấy dòng job vừa tạo
        eventPublisher.publishEvent(new GenerationRequestedEvent(job.getId(), request, current.id()));
        return AiJobResponse.from(job);
    }

    /**
     * Chạy sinh đề ở luồng nền, sau khi transaction tạo job đã commit.
     * <p>
     * Mỗi bước đổi trạng thái đi qua {@link AiJobStatusWriter} để commit ngay — client hỏi giữa
     * chừng phải thấy được {@code RUNNING}.
     */
    @Async("aiTaskExecutor")
    @TransactionalEventListener
    public void onGenerationRequested(GenerationRequestedEvent event) {
        runGeneration(event.jobId(), event.request(), event.ownerId());
    }

    /** Tách riêng để test gọi thẳng được, không phải dựng cả cơ chế sự kiện. */
    public void runGeneration(UUID jobId, GenerateQuestionsRequest request, UUID ownerId) {
        statusWriter.markRunning(jobId);

        try {
            QuestionGenerationService.GenerationResult result = generationService.generate(
                    new GenerationCommand(request.topic(), request.count(),
                            request.types() == null ? List.of() : request.types(),
                            request.difficulty(), request.materialId(), request.useMaterials()),
                    ownerId);

            statusWriter.markSucceeded(jobId, toJson(result));

        } catch (Exception e) {
            log.warn("Job sinh đề {} thất bại: {}", jobId, e.getMessage());
            String reason = e instanceof BusinessException ? e.getMessage() : "Sinh đề thất bại, thử lại sau";
            statusWriter.markFailed(jobId, reason);
        }
    }

    @Transactional(readOnly = true)
    public AiJobResponse get(UUID jobId, JwtService.AuthenticatedUser current) {
        return AiJobResponse.from(requireOwned(jobId, current));
    }

    /**
     * Creator duyệt: chỉ những câu được chọn mới thành câu hỏi thật trong ngân hàng.
     * <p>
     * Lưu qua {@code QuestionService} chứ không insert thẳng, để câu do AI sinh cũng phải qua
     * đúng bộ luật của từng loại câu hỏi như câu người soạn tay.
     */
    @Transactional
    public List<QuestionResponse> approve(UUID jobId, ApproveQuestionsRequest request,
                                          JwtService.AuthenticatedUser current) {
        AiJob job = requireOwned(jobId, current);

        if (job.getStatus() != com.datn.quizai.ai.domain.AiJobStatus.SUCCEEDED) {
            throw BusinessException.conflict("Job chưa hoàn thành nên chưa có câu hỏi để duyệt");
        }

        List<GeneratedQuestion> generated = readGenerated(job);
        List<QuestionResponse> saved = new ArrayList<>();

        for (Integer index : request.indexes()) {
            if (index == null || index < 0 || index >= generated.size()) {
                throw BusinessException.badRequest("Vị trí câu hỏi không hợp lệ: " + index);
            }
            saved.add(questionService.create(toQuestionRequest(generated.get(index)), current.id()));
        }

        log.info("Creator {} đã duyệt {}/{} câu hỏi từ job {}",
                current.email(), saved.size(), generated.size(), jobId);
        return saved;
    }

    // ------------------------------------------------------------------ nội bộ

    private AiJob requireOwned(UUID jobId, JwtService.AuthenticatedUser current) {
        AiJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy job"));

        if (!job.getUser().getId().equals(current.id())) {
            throw BusinessException.notFound("Không tìm thấy job");
        }
        return job;
    }

    private List<GeneratedQuestion> readGenerated(AiJob job) {
        try {
            JsonNode questions = objectMapper.readTree(job.getResult()).path("questions");
            return objectMapper.readerForListOf(GeneratedQuestion.class).readValue(questions);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Không đọc được kết quả job " + job.getId(), e);
        }
    }

    /** Câu AI sinh mặc định 1 điểm; Creator chỉnh lại sau khi lưu nếu muốn. */
    private QuestionRequest toQuestionRequest(GeneratedQuestion question) {
        List<QuestionOptionRequest> options = question.options().stream()
                .map(option -> new QuestionOptionRequest(option.content(), option.correct()))
                .toList();

        // rubric để null: mô hình sinh đề không tự đặt tiêu chí chấm, Creator soạn khi cần (features/06)
        return new QuestionRequest(
                question.type(), question.content(), question.explanation(), null,
                question.difficulty(), question.topic(), 1, null, options);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không tuần tự hoá được dữ liệu job", e);
        }
    }
}
