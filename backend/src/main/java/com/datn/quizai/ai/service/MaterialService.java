package com.datn.quizai.ai.service;

import com.datn.quizai.ai.domain.LearningMaterial;
import com.datn.quizai.ai.domain.MaterialSourceType;
import com.datn.quizai.ai.domain.MaterialStatus;
import com.datn.quizai.ai.dto.CreateMaterialRequest;
import com.datn.quizai.ai.dto.MaterialResponse;
import com.datn.quizai.ai.rag.TextExtractor;
import com.datn.quizai.ai.repository.LearningMaterialRepository;
import com.datn.quizai.ai.repository.MaterialChunkRepository;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.dto.PageResponse;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.user.domain.User;
import com.datn.quizai.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

/**
 * Quản lý học liệu cho RAG (docs/features/05).
 * <p>
 * Học liệu là <b>dữ liệu riêng</b> của từng người: mọi truy vấn đều lọc theo chủ sở hữu, và tài
 * liệu của người khác trả 404 — cùng quy ước đã dùng cho quiz riêng tư và bài làm.
 */
@Service
public class MaterialService {

    /** Tài liệu lớn hơn mức này sinh ra hàng nghìn lời gọi embedding, chặn từ đầu. */
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private final LearningMaterialRepository materialRepository;
    private final MaterialChunkRepository chunkRepository;
    private final UserRepository userRepository;
    private final TextExtractor textExtractor;
    private final ApplicationEventPublisher eventPublisher;

    public MaterialService(LearningMaterialRepository materialRepository,
                           MaterialChunkRepository chunkRepository,
                           UserRepository userRepository,
                           TextExtractor textExtractor,
                           ApplicationEventPublisher eventPublisher) {
        this.materialRepository = materialRepository;
        this.chunkRepository = chunkRepository;
        this.userRepository = userRepository;
        this.textExtractor = textExtractor;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PageResponse<MaterialResponse> listMine(UUID ownerId, Pageable pageable) {
        return PageResponse.of(materialRepository.findMine(ownerId, pageable), MaterialResponse::from);
    }

    @Transactional(readOnly = true)
    public MaterialResponse get(UUID materialId, JwtService.AuthenticatedUser current) {
        return MaterialResponse.from(requireOwned(materialId, current));
    }

    /** Nạp học liệu từ văn bản dán trực tiếp. */
    @Transactional
    public MaterialResponse createFromText(CreateMaterialRequest request, UUID ownerId) {
        LearningMaterial material = save(ownerId, request.title(), request.topic(),
                MaterialSourceType.TEXT, null);

        // Phát sự kiện thay vì gọi thẳng: job nền chỉ được chạy SAU KHI transaction này commit,
        // nếu không nó đọc CSDL sẽ chưa thấy dòng học liệu vừa tạo
        eventPublisher.publishEvent(new MaterialCreatedEvent(material.getId(), request.content(), ownerId));
        return MaterialResponse.from(material);
    }

    /** Nạp học liệu từ file PDF/DOCX/TXT — Tika trích text ngay để báo lỗi sớm nếu file hỏng. */
    @Transactional
    public MaterialResponse createFromFile(MultipartFile file, String title, String topic, UUID ownerId) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Chưa chọn file học liệu");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw BusinessException.badRequest("Tài liệu tối đa " + (MAX_FILE_BYTES / 1024 / 1024) + "MB");
        }

        String text;
        try (var input = file.getInputStream()) {
            text = textExtractor.extract(input, file.getOriginalFilename());
        } catch (IOException e) {
            throw BusinessException.badRequest("Không đọc được file tải lên");
        }

        String resolvedTitle = title == null || title.isBlank()
                ? stripExtension(file.getOriginalFilename())
                : title.trim();

        LearningMaterial material = save(ownerId, resolvedTitle, topic,
                sourceTypeOf(file.getOriginalFilename()), null);

        eventPublisher.publishEvent(new MaterialCreatedEvent(material.getId(), text, ownerId));
        return MaterialResponse.from(material);
    }

    /**
     * Bật/tắt chia sẻ học liệu cho người học (features/08).
     * <p>
     * Chỉ chủ tài liệu quyết định, và <b>không</b> có API nào cho Admin bật hộ: chia sẻ nội dung của
     * người khác là việc của chính họ.
     * <p>
     * Tắt chia sẻ có hiệu lực <b>ngay lượt hỏi tiếp theo</b> — truy vấn vector đọc cờ trực tiếp, không
     * qua bản sao nào. Nhưng câu trả lời đã sinh ra trước đó vẫn giữ nguyên phần trích dẫn: nó là bản
     * ghi của việc đã xảy ra, sửa lại thì lịch sử hội thoại thành sai.
     */
    @Transactional
    public MaterialResponse setShared(UUID materialId, boolean shared,
                                      JwtService.AuthenticatedUser current) {
        LearningMaterial material = requireOwned(materialId, current);

        if (shared && material.getStatus() != MaterialStatus.READY) {
            // Tài liệu chưa nạp xong thì chưa có vector nào, chia sẻ ra cũng không ai truy xuất được —
            // để bật được sẽ tạo ra một công tắc bật rồi mà không có tác dụng gì
            throw BusinessException.conflict(
                    "Tài liệu chưa xử lý xong nên chưa chia sẻ được. Đợi trạng thái chuyển sang Sẵn sàng.");
        }

        material.setShared(shared);
        return MaterialResponse.from(material);
    }

    @Transactional
    public void delete(UUID materialId, JwtService.AuthenticatedUser current) {
        LearningMaterial material = requireOwned(materialId, current);
        // Xoá đoạn trước: bảng chunks có ON DELETE CASCADE nhưng xoá tường minh cho rõ ý
        chunkRepository.deleteByMaterialId(material.getId());
        materialRepository.delete(material);
    }

    // ------------------------------------------------------------------ nội bộ

    private LearningMaterial save(UUID ownerId, String title, String topic,
                                  MaterialSourceType sourceType, String fileUrl) {
        User owner = userRepository.getReferenceById(ownerId);
        LearningMaterial material = new LearningMaterial(owner, title.trim(), sourceType);
        material.setTopic(topic == null || topic.isBlank() ? null : topic.trim());
        material.setFileUrl(fileUrl);
        return materialRepository.save(material);
    }

    /** Tài liệu của người khác trả 404, không phải 403 — không tiết lộ là nó tồn tại. */
    private LearningMaterial requireOwned(UUID materialId, JwtService.AuthenticatedUser current) {
        LearningMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy học liệu"));

        if (!material.getOwner().getId().equals(current.id())) {
            throw BusinessException.notFound("Không tìm thấy học liệu");
        }
        return material;
    }

    /**
     * Đoán loại nguồn từ tên file chỉ để <b>hiển thị</b>. Việc đọc nội dung do Tika tự nhận dạng,
     * không dựa vào phần mở rộng.
     */
    private MaterialSourceType sourceTypeOf(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return MaterialSourceType.PDF;
        }
        if (lower.endsWith(".docx") || lower.endsWith(".doc")) {
            return MaterialSourceType.DOCX;
        }
        return MaterialSourceType.TXT;
    }

    private String stripExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "Học liệu không tên";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
