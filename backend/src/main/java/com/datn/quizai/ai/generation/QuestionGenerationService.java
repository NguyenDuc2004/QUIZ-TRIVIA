package com.datn.quizai.ai.generation;

import com.datn.quizai.ai.provider.AiCompletion;
import com.datn.quizai.ai.provider.AiOrchestrator;
import com.datn.quizai.ai.provider.AiPrompt;
import com.datn.quizai.ai.repository.MaterialChunkRepository;
import com.datn.quizai.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Sinh bộ câu hỏi nháp từ học liệu (RAG) hoặc từ chủ đề — FR-29.
 * <p>
 * Luồng: embedding câu truy vấn → similarity search lấy k đoạn gần nhất → prompt kèm ngữ cảnh →
 * {@code AiOrchestrator} → validate JSON → trả câu hỏi nháp.
 * <p>
 * Không tự lưu vào ngân hàng câu hỏi: Creator phải duyệt trước (human-in-the-loop).
 */
@Service
public class QuestionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationService.class);

    /** Số đoạn học liệu đưa vào prompt. Nhiều quá thì loãng ngữ cảnh và tốn token. */
    private static final int RETRIEVAL_TOP_K = 6;
    /** Số câu tối đa một lần, chặn cả chi phí lẫn thời gian chờ. */
    public static final int MAX_QUESTIONS = 20;

    private final AiOrchestrator aiOrchestrator;
    private final MaterialChunkRepository chunkRepository;

    public QuestionGenerationService(AiOrchestrator aiOrchestrator,
                                     MaterialChunkRepository chunkRepository) {
        this.aiOrchestrator = aiOrchestrator;
        this.chunkRepository = chunkRepository;
    }

    /** Kết quả một mẻ sinh đề, kèm đủ thông tin để giải trình cho Creator. */
    public record GenerationResult(
            List<GeneratedQuestion> questions,
            List<String> rejected,
            List<String> sourceExcerpts,
            String provider,
            String model,
            long latencyMs
    ) {
    }

    public GenerationResult generate(GenerationCommand command, UUID ownerId) {
        if (command.count() < 1 || command.count() > MAX_QUESTIONS) {
            throw BusinessException.badRequest("Số câu hỏi phải từ 1 đến " + MAX_QUESTIONS);
        }

        List<MaterialChunkRepository.Chunk> chunks = retrieve(command, ownerId);

        AiPrompt prompt = AiPrompt.json(
                QuestionPromptBuilder.systemInstruction(),
                QuestionPromptBuilder.userPrompt(command.topic(), command.count(),
                        command.types(), command.difficulty(), chunks));

        // background = true: sinh đề chạy trong job nền và trả jobId, không ai ngồi đợi HTTP response.
        // Nhờ vậy chờ được hết cửa sổ hạn mức theo phút thay vì bỏ cuộc rồi đánh job là FAILED.
        AiCompletion completion = aiOrchestrator.complete(prompt, "generation", ownerId, true);
        QuestionJsonParser.ParseResult parsed = QuestionJsonParser.parse(completion.text());

        if (parsed.questions().isEmpty()) {
            throw BusinessException.badRequest(
                    "AI không tạo được câu hỏi hợp lệ nào. Thử mô tả chủ đề rõ hơn hoặc giảm số câu.");
        }

        log.info("Sinh đề: {} câu hợp lệ / {} câu bị loại, provider {}, {}ms",
                parsed.questions().size(), parsed.rejected().size(), completion.provider(), completion.latencyMs());

        return new GenerationResult(
                parsed.questions(),
                parsed.rejected(),
                chunks.stream().map(MaterialChunkRepository.Chunk::content).toList(),
                completion.provider(),
                completion.model(),
                completion.latencyMs());
    }

    /**
     * Lấy các đoạn học liệu liên quan nhất.
     * <p>
     * Không chọn học liệu thì trả rỗng — sinh theo kiến thức chung, và prompt sẽ nói rõ điều đó
     * thay vì giả vờ có nguồn.
     */
    private List<MaterialChunkRepository.Chunk> retrieve(GenerationCommand command, UUID ownerId) {
        if (!command.useMaterials()) {
            return List.of();
        }

        String query = command.topic() == null || command.topic().isBlank()
                ? "nội dung chính của tài liệu"
                : command.topic();

        List<Float> queryEmbedding = aiOrchestrator.embed(query, ownerId, true);
        List<MaterialChunkRepository.Chunk> chunks =
                chunkRepository.searchSimilar(ownerId, command.materialId(), queryEmbedding, RETRIEVAL_TOP_K);

        if (chunks.isEmpty()) {
            throw BusinessException.badRequest(
                    "Chưa có học liệu nào sẵn sàng để tham chiếu. Tải tài liệu lên và đợi xử lý xong, "
                            + "hoặc bỏ chọn học liệu để sinh theo kiến thức chung.");
        }
        return chunks;
    }
}
