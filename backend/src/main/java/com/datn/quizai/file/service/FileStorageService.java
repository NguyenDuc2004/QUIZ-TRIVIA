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
import java.util.stream.Stream;

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
 * <b>Ảnh đại diện thì không vướng chuyện đó</b>: mỗi người chỉ giữ một file, xem {@link #storeAvatar}.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    /** Đường dẫn công khai của thư mục ảnh — phải khớp với resource handler ở {@code WebMvcConfig}. */
    public static final String PUBLIC_PREFIX = "/uploads/";
    private static final String IMAGE_FOLDER = "images";
    private static final String AVATAR_FOLDER = "avatars";

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
        ImageType type = kiemTra(file);
        return ghi(file, type, IMAGE_FOLDER, UUID.randomUUID() + "." + type.extension());
    }

    /**
     * Lưu <b>ảnh đại diện</b> của một người dùng, trả về {@code /uploads/avatars/<id>-<ngaunhien>.jpg}.
     *
     * <h4>Vì sao tách riêng khỏi {@link #storeImage}</h4>
     * {@code storeImage} chỉ mở cho CREATOR/ADMIN, vì cho mọi tài khoản tải ảnh <i>không giới hạn số
     * lượng</i> thì hệ thống thành chỗ chứa file miễn phí cho bất kỳ ai đăng ký được. Nhưng ảnh đại diện
     * là nhu cầu có thật của <b>mọi</b> người dùng, kể cả người học.
     * <p>
     * Chỗ này gỡ đúng cái lo đó bằng <b>ràng buộc mỗi người một file</b>: tên file bắt đầu bằng id người
     * dùng, và mọi file cũ của chính họ bị xoá sau khi ghi file mới. Tải lên một nghìn lần vẫn chỉ tốn
     * một file — tổng dung lượng bị chặn bởi <i>số tài khoản</i>, không phải số lần bấm nút.
     * <p>
     * Phần ngẫu nhiên phía sau id là để <b>đổi URL mỗi lần đổi ảnh</b>. Giữ nguyên tên thì trình duyệt và
     * những màn hình khác đang hiện ảnh cũ vẫn lấy từ cache, người dùng đổi ảnh xong tưởng là hỏng.
     *
     * @param userId lấy từ token của người đang đăng nhập, <b>không bao giờ</b> từ tham số client gửi —
     *               nó được ghép vào tên file, nên nhận từ client là mở đường ghi đè ảnh người khác
     */
    public UploadedFileResponse storeAvatar(MultipartFile file, UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Thiếu id người dùng khi lưu ảnh đại diện");
        }
        ImageType type = kiemTra(file);
        String fileName = userId + "-" + UUID.randomUUID().toString().substring(0, 8)
                + "." + type.extension();

        UploadedFileResponse ketQua = ghi(file, type, AVATAR_FOLDER, fileName);
        xoaAnhCu(userId, fileName);
        return ketQua;
    }

    /**
     * Ba phép kiểm bắt buộc trước khi ghi bất kỳ ảnh nào: có file, không quá dung lượng, và đúng là ảnh.
     * <p>
     * Dùng chung cho cả hai đường tải lên — <b>cố ý không để mỗi đường tự kiểm</b>. Ba dòng này là luật an
     * toàn chứ không phải tiện ích: nhân đôi chúng nghĩa là lần sau ai đó sửa một chỗ mà quên chỗ kia, và
     * <b>chỗ bị quên chính là lỗ hổng</b>. Đúng lý do đã tách {@code UploadedImagePath} ra dùng chung.
     *
     * @throws BusinessException 400 nếu file rỗng, quá lớn, hoặc không thuộc định dạng ảnh cho phép
     */
    private ImageType kiemTra(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Chưa chọn file ảnh");
        }
        if (file.getSize() > maxImageBytes) {
            throw BusinessException.badRequest(
                    "Ảnh tối đa " + (maxImageBytes / 1024 / 1024) + "MB, ảnh của bạn "
                            + Math.round(file.getSize() / 1024d / 1024d * 10) / 10d + "MB");
        }
        return detectType(file);
    }

    /** Ghi file xuống {@code uploadRoot/<thuMuc>/<tenFile>} rồi dựng phản hồi. */
    private UploadedFileResponse ghi(MultipartFile file, ImageType type,
                                     String folderName, String fileName) {
        try {
            Path folder = uploadRoot.resolve(folderName);
            Files.createDirectories(folder);

            Path target = folder.resolve(fileName);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Đã lưu ảnh {}/{} ({} bytes, {})", folderName, fileName, file.getSize(),
                    type.contentType());
            return new UploadedFileResponse(
                    PUBLIC_PREFIX + folderName + "/" + fileName,
                    fileName,
                    file.getSize(),
                    type.contentType());

        } catch (IOException e) {
            log.error("Không ghi được ảnh xuống {}", uploadRoot, e);
            throw new IllegalStateException("Không lưu được ảnh, thử lại sau", e);
        }
    }

    /**
     * Xoá mọi ảnh đại diện cũ của người này, trừ file vừa ghi.
     * <p>
     * Chạy <b>sau</b> khi ghi file mới chứ không phải trước: xoá trước mà lượt ghi hỏng giữa chừng thì
     * người dùng mất luôn ảnh đang có. Lỗi ở bước dọn dẹp chỉ ghi log — ảnh mới đã lưu xong, để cả yêu
     * cầu thất bại vì một file rác nằm lại là đánh đổi sai.
     */
    private void xoaAnhCu(UUID userId, String fileNameMoi) {
        Path folder = uploadRoot.resolve(AVATAR_FOLDER);
        try (Stream<Path> cacFile = Files.list(folder)) {
            cacFile.filter(f -> f.getFileName().toString().startsWith(userId + "-"))
                    .filter(f -> !f.getFileName().toString().equals(fileNameMoi))
                    .forEach(f -> {
                        try {
                            Files.deleteIfExists(f);
                        } catch (IOException e) {
                            log.warn("Không xoá được ảnh đại diện cũ {}", f, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("Không đọc được thư mục ảnh đại diện {}", folder, e);
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
