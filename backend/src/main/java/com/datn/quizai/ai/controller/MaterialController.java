package com.datn.quizai.ai.controller;

import com.datn.quizai.ai.dto.CreateMaterialRequest;
import com.datn.quizai.ai.dto.MaterialResponse;
import com.datn.quizai.ai.service.MaterialService;
import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Học liệu RAG — docs/api.md §6, docs/features/08.
 *
 * <h3>Vì sao tách khỏi {@code AiController}</h3>
 * {@code AiController} gắn {@code @PreAuthorize("hasAnyRole('CREATOR','ADMIN')")} <b>cấp lớp</b>, và
 * nhóm endpoint học liệu phải mở cho cả người học. Cách xử lý là tách sang lớp riêng chứ không đục
 * một lỗ ngoại lệ trong luật phân quyền của lớp kia — đúng tiền lệ {@code ChatController} đã đặt khi
 * mở danh sách "học liệu hỏi được" cho người học. Một lớp có luật thống nhất thì đọc là biết; một lớp
 * cấm cả cụm rồi mở lẻ vài phương thức là cách chắc chắn để sau này có người mở quyền quá tay.
 *
 * <h3>Vì sao người học được nạp học liệu</h3>
 * Trước 04/09/2026 chỉ CREATOR/ADMIN nạp được, và hệ quả là <b>trợ lý học tập chết hẳn với người học
 * đơn lẻ</b>: họ không sở hữu tài liệu nào, truy hồi luôn ra 0 đoạn, và prompt bắt mô hình nói "chưa
 * có tài liệu để dựa vào". Chức năng của họ phụ thuộc vào việc một Creator nào đó có bấm nút chia sẻ
 * hay không — một phụ thuộc mà bản thân họ không tác động được.
 *
 * <h3>Lằn ranh vẫn giữ</h3>
 * <ul>
 *   <li><b>Đọc/sửa/xoá vẫn chỉ trong tài liệu của chính mình.</b> Mọi phương thức của
 *       {@code MaterialService} đều đi qua {@code requireOwned}, nên mở lớp này cho người học nghĩa là
 *       mở tới <i>dữ liệu của chính họ</i>, không phải kho chung.</li>
 *   <li><b>Chia sẻ vẫn là CREATOR/ADMIN.</b> Bật {@code shared} là đưa tài liệu vào trợ lý của
 *       <i>mọi</i> người học khác — một hành vi xuất bản, không phải một thiết lập cá nhân, nên nó là
 *       ngoại lệ duy nhất bị siết lại ở cấp phương thức bên dưới.</li>
 *   <li><b>Sinh đề, job, duyệt câu hỏi</b> vẫn nằm nguyên ở {@code AiController} với luật cũ.</li>
 * </ul>
 *
 * <h3>Chi phí — điều mà luật vai trò cũ đang thay mặt canh</h3>
 * Javadoc cũ của {@code AiController} nói rõ lý do khoá là "mỗi lời gọi đều tốn tiền API". Nhưng vai
 * trò là một công cụ tồi để canh chi phí: nó chặn đúng người cần dùng và không chặn gì ở người đã có
 * quyền. Thay bằng thứ đo đúng đại lượng cần đo — <b>trần số tài liệu mỗi người học</b>, cưỡng chế ở
 * {@code MaterialService}. Hạn mức AI theo ngày <i>không</i> đỡ được việc này: nó cố ý không tính lượt
 * nhúng học liệu (xem {@code AiQuotaService}).
 */
@RestController
@RequestMapping("/api/v1/ai/materials")
@Tag(name = "AI", description = "Học liệu RAG và sinh đề bằng AI")
@SecurityRequirement(name = "bearerAuth")
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    @Operation(summary = "Học liệu của tôi")
    public PageResponse<MaterialResponse> listMaterials(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PageableDefault(size = 20) Pageable pageable) {
        return materialService.listMine(current.id(), pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết một học liệu (dùng để hỏi lại trạng thái xử lý)")
    public MaterialResponse getMaterial(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                        @PathVariable UUID id) {
        return materialService.get(id, current);
    }

    @PostMapping
    @Operation(summary = "Nạp học liệu bằng văn bản dán trực tiếp; xử lý chạy nền")
    public ResponseEntity<MaterialResponse> createMaterial(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @Valid @RequestBody CreateMaterialRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(materialService.createFromText(request, current));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Nạp học liệu từ file PDF/DOCX/TXT; Tika trích text rồi xử lý nền")
    public ResponseEntity<MaterialResponse> uploadMaterial(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String topic) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(materialService.createFromFile(file, title, topic, current));
    }

    /**
     * Ngoại lệ duy nhất bị siết chặt hơn phần còn lại của lớp này.
     * <p>
     * Bật {@code shared} không phải một thiết lập cá nhân — nó đẩy tài liệu vào trợ lý của <b>mọi</b>
     * người học trong hệ thống. Đó là hành vi xuất bản, và cùng lúc là một bề mặt kiểm duyệt: nội dung
     * sai của một người sẽ thành căn cứ trả lời cho người khác.
     */
    @PatchMapping("/{id}/shared")
    @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
    @Operation(summary = "Bật/tắt chia sẻ học liệu cho người học (features/08). Chỉ tài liệu đã bật "
            + "mới được trợ lý AI dùng để trả lời cho người khác; tài liệu chưa xử lý xong trả 409. "
            + "Yêu cầu vai trò CREATOR/ADMIN.")
    public MaterialResponse setMaterialShared(
            @AuthenticationPrincipal JwtService.AuthenticatedUser current,
            @PathVariable UUID id,
            @RequestParam boolean shared) {
        return materialService.setShared(id, shared, current);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá học liệu và toàn bộ vector của nó")
    public ResponseEntity<Void> deleteMaterial(@AuthenticationPrincipal JwtService.AuthenticatedUser current,
                                               @PathVariable UUID id) {
        materialService.delete(id, current);
        return ResponseEntity.noContent().build();
    }
}
