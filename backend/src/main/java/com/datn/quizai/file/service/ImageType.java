package com.datn.quizai.file.service;

import java.util.Arrays;

/**
 * Các định dạng ảnh được chấp nhận, nhận dạng bằng <b>chữ ký byte đầu file</b> (magic number).
 * <p>
 * Cố tình không tin {@code Content-Type} hay phần mở rộng client gửi lên: hai thứ đó do client
 * đặt nên sửa được, kẻ tấn công có thể đặt tên {@code anh.jpg} cho một file script rồi nhờ server
 * phục vụ lại. Đọc chữ ký byte là cách duy nhất biết file thực sự là gì.
 */
public enum ImageType {

    JPEG("jpg", "image/jpeg", new int[]{0xFF, 0xD8, 0xFF}),
    PNG("png", "image/png", new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
    GIF("gif", "image/gif", new int[]{0x47, 0x49, 0x46, 0x38}),
    /** WebP: 'RIFF' ở byte 0..3 rồi 'WEBP' ở byte 8..11 — kiểm riêng ở {@link #matches}. */
    WEBP("webp", "image/webp", new int[]{0x52, 0x49, 0x46, 0x46});

    /** Số byte đầu file cần đọc để nhận dạng được mọi định dạng ở trên. */
    public static final int HEADER_SIZE = 12;

    private final String extension;
    private final String contentType;
    private final int[] signature;

    ImageType(String extension, String contentType, int[] signature) {
        this.extension = extension;
        this.contentType = contentType;
        this.signature = signature;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return contentType;
    }

    /** Nhận dạng định dạng từ những byte đầu tiên; rỗng nghĩa là không phải ảnh hợp lệ. */
    public static java.util.Optional<ImageType> detect(byte[] header) {
        return Arrays.stream(values()).filter(type -> type.matches(header)).findFirst();
    }

    private boolean matches(byte[] header) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((header[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        // RIFF còn dùng cho WAV/AVI, phải kiểm thêm nhãn 'WEBP' ở byte 8..11
        if (this == WEBP) {
            return header.length >= HEADER_SIZE
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
        }
        return true;
    }
}
