package com.datn.quizai.ai.service;

import com.datn.quizai.ai.domain.AiJob;
import com.datn.quizai.ai.domain.AiJobStatus;
import com.datn.quizai.ai.domain.AiJobType;
import com.datn.quizai.ai.dto.AiJobResponse;
import com.datn.quizai.ai.dto.ApproveFlashcardsRequest;
import com.datn.quizai.ai.dto.GenerateFlashcardsRequest;
import com.datn.quizai.ai.generation.FlashcardGenerationService;
import com.datn.quizai.ai.generation.GeneratedFlashcard;
import com.datn.quizai.ai.repository.AiJobRepository;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.flashcard.domain.FlashcardSource;
import com.datn.quizai.flashcard.dto.FlashcardRequest;
import com.datn.quizai.flashcard.service.FlashcardService;
import com.datn.quizai.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

/**
 * Job nền sinh thẻ ghi nhớ bằng AI, và luồng duyệt thẻ (features/11, FR-38).
 * <p>
 * Tách khỏi {@code AiJobService} thay vì thêm vào đó: lớp kia đã lo cả nạp học liệu và sinh đề, thêm loại
 * job thứ ba vào nữa thì nó thành nơi mọi thứ về AI đổ vào. Cả hai dùng chung {@link AiJobStatusWriter},
 * {@link AiJobRepository} và endpoint tra trạng thái, nên tách ra không nhân bản gì.
 *
 * <h3>Vì sao phải có bước duyệt</h3>
 * Thẻ không được lưu thẳng sau khi mô hình trả về. Một thẻ sai lọt vào bộ sẽ được người học <b>ôn đi ôn lại
 * theo lịch SRS</b> — tức được học thuộc, chứ không chỉ được đọc qua một lần như một câu hỏi trong đề. Người
 * duy nhất phát hiện được nội dung sai là người đọc tài liệu gốc, nên kết quả job kèm cả các đoạn học liệu
 * đã dùng để họ đối chiếu.
 */
@Service
public class AiFlashcardJobService {

    private static final Logger log = LoggerFactory.getLogger(AiFlashcardJobService.class);

    private final AiJobRepository jobRepository;
    private final FlashcardGenerationService generationService;
    private final FlashcardService flashcardService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AiJobStatusWriter statusWriter;
    private final ApplicationEventPublisher eventPublisher;
    private final AiQuotaService quotaService;

    public AiFlashcardJobService(AiJobRepository jobRepository,
                                 FlashcardGenerationService generationService,
                                 FlashcardService flashcardService,
                                 UserRepository userRepository,
                                 ObjectMapper objectMapper,
                                 AiJobStatusWriter statusWriter,
                                 ApplicationEventPublisher eventPublisher,
                                 AiQuotaService quotaService) {
        this.jobRepository = jobRepository;
        this.quotaService = quotaService;
        this.generationService = generationService;
        this.flashcardService = flashcardService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.statusWriter = statusWriter;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AiJobResponse submit(GenerateFlashcardsRequest request,
                                JwtService.AuthenticatedUser current) {
        // Cùng lý do với AiJobService.submitGeneration: chốt hạn mức ngay lúc nhận việc để người
        // đã hết lượt không nhận 202 rồi mới thấy job hỏng. Kiểm-KHÔNG-cộng-lượt (FR-84).
        quotaService.kiemTra(current.id());

        // Kiểm quyền trên bộ thẻ NGAY, trước khi tốn một lời gọi mô hình: nếu bộ thẻ không thuộc người này
        // thì kết quả sinh ra cũng không lưu được, và người dùng đáng được biết ngay thay vì sau ba mươi
        // giây chờ job.
        flashcardService.requireOwnedDeck(request.deckId(), current.id());

        AiJob job = new AiJob(userRepository.getReferenceById(current.id()),
                AiJobType.GENERATE_FLASHCARDS, toJson(request));
        jobRepository.save(job);

        eventPublisher.publishEvent(
                new FlashcardGenerationRequestedEvent(job.getId(), request, current.id()));
        return AiJobResponse.from(job);
    }

    @Async("aiTaskExecutor")
    @TransactionalEventListener
    public void onRequested(FlashcardGenerationRequestedEvent event) {
        run(event.jobId(), event.request(), event.ownerId());
    }

    /** Tách riêng để test gọi thẳng được, không phải dựng cả cơ chế sự kiện. */
    public void run(UUID jobId, GenerateFlashcardsRequest request, UUID ownerId) {
        statusWriter.markRunning(jobId);
        try {
            FlashcardGenerationService.Result result = generationService.generate(
                    request.topic(), request.count(), request.materialId(), ownerId);
            statusWriter.markSucceeded(jobId, toJson(result));

        } catch (Exception e) {
            log.warn("Job sinh thẻ {} thất bại: {}", jobId, e.getMessage());
            String reason = e instanceof BusinessException
                    ? e.getMessage() : "Sinh thẻ thất bại, thử lại sau";
            statusWriter.markFailed(jobId, reason);
        }
    }

    /**
     * Người dùng duyệt: chỉ những thẻ được chọn mới thành thẻ thật trong bộ.
     * <p>
     * Bộ thẻ đích lấy từ <b>yêu cầu ban đầu đã lưu trong job</b>, không nhận lại từ client lúc duyệt: nhận
     * lại là mở đường ghi thẻ vào một bộ khác với bộ đã được kiểm quyền lúc gửi yêu cầu.
     */
    @Transactional
    public int approve(UUID jobId, ApproveFlashcardsRequest request,
                       JwtService.AuthenticatedUser current) {
        AiJob job = requireOwned(jobId, current);

        if (job.getStatus() != AiJobStatus.SUCCEEDED) {
            throw BusinessException.conflict("Job chưa hoàn thành nên chưa có thẻ để duyệt");
        }
        if (job.getType() != AiJobType.GENERATE_FLASHCARDS) {
            throw BusinessException.badRequest("Job này không phải job sinh thẻ ghi nhớ");
        }

        List<GeneratedFlashcard> generated = readGenerated(job);
        GenerateFlashcardsRequest goc = readRequest(job);

        List<FlashcardRequest> chon = request.indexes().stream()
                .distinct()
                .filter(i -> i != null && i >= 0 && i < generated.size())
                .map(generated::get)
                .map(t -> new FlashcardRequest(t.front(), t.back(), t.hint()))
                .toList();

        if (chon.isEmpty()) {
            throw BusinessException.badRequest("Không có thẻ nào hợp lệ trong danh sách đã chọn");
        }

        return flashcardService.addCards(goc.deckId(), current.id(), chon,
                FlashcardSource.AI_GENERATED);
    }

    private AiJob requireOwned(UUID jobId, JwtService.AuthenticatedUser current) {
        AiJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy job"));
        // 404 với job của người khác, không 403 — trả 403 là xác nhận job đó tồn tại
        if (!job.getUser().getId().equals(current.id())) {
            throw BusinessException.notFound("Không tìm thấy job");
        }
        return job;
    }

    private List<GeneratedFlashcard> readGenerated(AiJob job) {
        try {
            var node = objectMapper.readTree(job.getResult()).path("flashcards");
            return objectMapper.convertValue(node, new TypeReference<List<GeneratedFlashcard>>() {
            });
        } catch (Exception e) {
            throw BusinessException.badRequest("Không đọc được kết quả của job");
        }
    }

    private GenerateFlashcardsRequest readRequest(AiJob job) {
        try {
            return objectMapper.readValue(job.getRequest(), GenerateFlashcardsRequest.class);
        } catch (Exception e) {
            throw BusinessException.badRequest("Không đọc được yêu cầu ban đầu của job");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Không tuần tự hoá được dữ liệu job", e);
        }
    }
}
