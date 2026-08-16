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
 * Sinh thẻ ghi nhớ từ học liệu qua RAG (features/11, FR-38).
 * <p>
 * Tái dùng đúng pipeline của sinh đề: truy xuất đoạn liên quan từ pgvector → dựng prompt có rào ngữ cảnh →
 * gọi {@link AiOrchestrator} → lọc kết quả. Không có đường nào gọi mô hình trực tiếp.
 *
 * <h3>Bắt buộc phải có học liệu</h3>
 * Khác sinh đề — nơi bỏ chọn học liệu thì sinh theo kiến thức chung — sinh thẻ <b>luôn</b> cần nguồn.
 * Lý do: thẻ ghi nhớ được người học ôn đi ôn lại hàng chục lần trong nhiều tháng, nên một thẻ sai sẽ được
 * học thuộc chứ không chỉ được đọc qua. Kiến thức chung của mô hình không kiểm chứng được, và người dùng
 * không có tài liệu nào để đối chiếu khi duyệt.
 */
@Service
public class FlashcardGenerationService {

    private static final Logger log = LoggerFactory.getLogger(FlashcardGenerationService.class);

    /**
     * Số đoạn học liệu lấy về.
     * <p>
     * Lấy nhiều hơn sinh đề (6) vì một thẻ chỉ cần một ý nhỏ, nên cần nhiều ý khác nhau để đủ số thẻ mà
     * không bắt mô hình vắt cùng một đoạn thành mấy thẻ trùng nhau.
     */
    private static final int RETRIEVAL_TOP_K = 8;

    public static final int MAX_FLASHCARDS = 30;

    private final AiOrchestrator aiOrchestrator;
    private final MaterialChunkRepository chunkRepository;

    public FlashcardGenerationService(AiOrchestrator aiOrchestrator,
                                      MaterialChunkRepository chunkRepository) {
        this.aiOrchestrator = aiOrchestrator;
        this.chunkRepository = chunkRepository;
    }

    /**
     * Kết quả một mẻ sinh thẻ.
     *
     * @param rejected  các thẻ bị loại kèm lý do — trả về để giải trình vì sao yêu cầu 15 mà nhận 11
     * @param sourceExcerpts đoạn học liệu đã dùng, để người duyệt đối chiếu từng thẻ với nguồn
     */
    public record Result(
            List<GeneratedFlashcard> flashcards,
            List<String> rejected,
            List<String> sourceExcerpts,
            String provider,
            String model,
            long latencyMs
    ) {
    }

    public Result generate(String chuDe, int soLuong, UUID materialId, UUID ownerId) {
        if (soLuong < 1 || soLuong > MAX_FLASHCARDS) {
            throw BusinessException.badRequest("Số thẻ phải từ 1 đến " + MAX_FLASHCARDS);
        }

        List<MaterialChunkRepository.Chunk> chunks = retrieve(chuDe, materialId, ownerId);

        AiPrompt prompt = AiPrompt.json(
                FlashcardPromptBuilder.systemInstruction(),
                FlashcardPromptBuilder.userPrompt(chuDe, soLuong, chunks));

        // background = true: chạy trong job nền nên chờ được hết cửa sổ hạn mức theo phút, thay vì bỏ
        // cuộc rồi đánh job là FAILED. Cùng lý do như sinh đề.
        AiCompletion completion = aiOrchestrator.complete(prompt, "generate-flashcards", ownerId, true);
        FlashcardJsonParser.ParseResult parsed = FlashcardJsonParser.parse(completion.text());

        if (parsed.flashcards().isEmpty()) {
            throw BusinessException.badRequest(
                    "AI không tạo được thẻ hợp lệ nào từ học liệu này. Thử nêu chủ đề rõ hơn, "
                            + "hoặc chọn tài liệu khác.");
        }

        log.info("Sinh thẻ: {} thẻ hợp lệ / {} thẻ bị loại, provider {}, {}ms",
                parsed.flashcards().size(), parsed.rejected().size(),
                completion.provider(), completion.latencyMs());

        return new Result(
                parsed.flashcards(),
                parsed.rejected(),
                chunks.stream().map(MaterialChunkRepository.Chunk::content).toList(),
                completion.provider(),
                completion.model(),
                completion.latencyMs());
    }

    /**
     * Lấy các đoạn học liệu liên quan nhất.
     * <p>
     * Dùng {@code searchSimilarIncludingShared}: người học ôn được từ tài liệu mà Creator đã chia sẻ, không
     * chỉ tài liệu tự mình nạp. Đó là cách học liệu chia sẻ đang dùng ở trợ lý học tập (features/08), giữ
     * nguyên ở đây để cùng một tài liệu không lúc thì đọc được lúc thì không.
     */
    private List<MaterialChunkRepository.Chunk> retrieve(String chuDe, UUID materialId, UUID ownerId) {
        String query = chuDe == null || chuDe.isBlank() ? "nội dung chính của tài liệu" : chuDe;

        List<Float> queryEmbedding = aiOrchestrator.embed(query, ownerId, true);
        List<MaterialChunkRepository.Chunk> chunks = chunkRepository.searchSimilarIncludingShared(
                ownerId, materialId, queryEmbedding, RETRIEVAL_TOP_K);

        if (chunks.isEmpty()) {
            throw BusinessException.badRequest(
                    "Chưa có học liệu nào sẵn sàng để sinh thẻ. Tải tài liệu lên và đợi xử lý xong, "
                            + "hoặc chọn một tài liệu đã được chia sẻ.");
        }
        return chunks;
    }
}
