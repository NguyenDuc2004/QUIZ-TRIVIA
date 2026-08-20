package com.datn.quizai.file.controller;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.file.dto.UploadedFileResponse;
import com.datn.quizai.file.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Tải file lên — docs/api.md §3.1.
 * <p>
 * Hai đường, hai mức quyền, vì rủi ro của chúng khác nhau:
 * <ul>
 *   <li>{@code /images} — ảnh bìa quiz và ảnh câu hỏi, <b>không giới hạn số lượng</b>, nên chỉ mở cho
 *       CREATOR/ADMIN. Mở cho mọi tài khoản là biến hệ thống thành chỗ chứa file miễn phí cho bất kỳ ai
 *       đăng ký được.</li>
 *   <li>{@code /avatar} — <b>mọi người dùng đã đăng nhập</b>. Ảnh đại diện là nhu cầu có thật của người
 *       học, và mỗi người chỉ giữ đúng một file, nên nó không mang cái rủi ro ở trên.</li>
 * </ul>
 * Bản đầu chỉ có đường thứ nhất, kèm chú thích "người học không có nhu cầu". Điều đó đúng cho tới khi có
 * trang hồ sơ sửa được: từ lúc đó người học bấm "Chọn ảnh từ máy" và nhận <b>403 Không có quyền</b>, ngay
 * trên chính trang hồ sơ của mình.
 */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File", description = "Tải ảnh lên cho quiz và câu hỏi")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải một ảnh lên, trả về đường dẫn công khai để lưu vào quiz/câu hỏi")
    @PreAuthorize("hasAnyRole('CREATOR', 'ADMIN')")
    public ResponseEntity<UploadedFileResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fileStorageService.storeImage(file));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải ảnh đại diện của chính mình; ảnh cũ bị thay thế")
    public ResponseEntity<UploadedFileResponse> uploadAvatar(
            @AuthenticationPrincipal JwtService.AuthenticatedUser currentUser,
            @RequestParam("file") MultipartFile file) {

        // id lấy từ token chứ không nhận qua tham số: nó được ghép vào tên file
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileStorageService.storeAvatar(file, currentUser.id()));
    }
}
