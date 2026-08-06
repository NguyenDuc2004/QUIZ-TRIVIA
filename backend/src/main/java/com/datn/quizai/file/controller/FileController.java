package com.datn.quizai.file.controller;

import com.datn.quizai.file.dto.UploadedFileResponse;
import com.datn.quizai.file.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Tải file lên — docs/api.md §3.1.
 * <p>
 * Chỉ <b>CREATOR/ADMIN</b> được tải ảnh: người học không có nhu cầu, mà mở cho mọi tài khoản
 * thì thành chỗ chứa file miễn phí cho bất kỳ ai đăng ký được.
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
}
