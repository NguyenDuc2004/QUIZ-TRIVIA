package com.datn.quizai.file.service;

import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.file.dto.UploadedFileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Lưu ảnh người dùng tải lên xuống đĩa và trả về đường dẫn công khai.
 * <p>
 * <b>Ba luật an toàn</b> (docs/security.md — Unrestricted File Upload):
 * <ol>
 *   <li><b>Không bao giờ dùng tên file client gửi.</b> Tên mới do server sinh từ UUID, phần mở rộng
 *       lấy từ định dạng dò được. Nhờ vậy không có đường nào cho {@code ../../} hay tên file lạ.</li>
 *   <li><b>Nhận dạng bằng chữ ký byte</b>, không tin {@code Content-Type} client khai (xem
 *       {@link ImageType}).</li>
 *   <li><b>Chặn dung lượng riêng cho ảnh</b>, chặt hơn giới hạn multipart chung (giới hạn chung
 *       phải nới rộng cho học liệu RAG ở features/05).</li>
 * </ol>
 * <p>
 * Hạn chế đã biết: đổi ảnh bìa thì file cũ vẫn nằm lại trên đĩa. Chấp nhận trong phạm vi đồ án —
 * dọn file mồ côi cần biết chắc không quiz nào còn trỏ tới, để làm sau nếu còn thời gian.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** Đường dẫn công khai của thư mục ảnh — phải khớp với resource handler ở {@code WebMvcConfig}. */
    public static final String PUBLIC_PREFIX = "/uploads/";
    private static final String IMAGE_FOLDER = "images";

    private final Path uploadRoot;
    private final long maxImageBytes;

    public FileStorageService(@Value("${app.storage.upload-dir}") String uploadDir,
                              @Value("${app.storage.max-image-size-bytes}") long maxImageBytes) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxImageBytes = maxImageBytes;
    }

    /**
     * Lưu một ảnh, trả về đường dẫn công khai dạng {@code /uploads/images/<uuid>.jpg}.
     *
     * @throws BusinessException 400 nếu file rỗng, quá lớn, hoặc không phải ảnh thuộc định dạng cho phép
     */
    public UploadedFileResponse storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Chưa chọn file ảnh");
        }
        if (file.getSize() > maxImageBytes) {
            throw BusinessException.badRequest(
                    "Ảnh tối đa " + (maxImageBytes / 1024 / 1024) + "MB, ảnh của bạn "
                            + Math.round(file.getSize() / 1024d / 1024d * 10) / 10d + "MB");
        }

        ImageType type = detectType(file);
        String fileName = UUID.randomUUID() + "." + type.extension();

        try {
            Path folder = uploadRoot.resolve(IMAGE_FOLDER);
            Files.createDirectories(folder);

            Path target = folder.resolve(fileName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Đã lưu ảnh {} ({} bytes, {})", fileName, file.getSize(), type.contentType());
            return new UploadedFileResponse(
                    PUBLIC_PREFIX + IMAGE_FOLDER + "/" + fileName,
                    fileName,
                    file.getSize(),
                    type.contentType());

        } catch (IOException e) {
            log.error("Không ghi được ảnh xuống {}", uploadRoot, e);
            throw new IllegalStateException("Không lưu được ảnh, thử lại sau", e);
        }
    }

    private ImageType detectType(MultipartFile file) {
        byte[] header = new byte[ImageType.HEADER_SIZE];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(header, 0, header.length);
            if (read < 4) {
                throw BusinessException.badRequest("File không phải ảnh hợp lệ");
            }
        } catch (IOException e) {
            throw BusinessException.badRequest("Không đọc được file tải lên");
        }

        return ImageType.detect(header).orElseThrow(() -> BusinessException.badRequest(
                "Chỉ nhận ảnh JPG, PNG, GIF hoặc WebP"));
    }
}
