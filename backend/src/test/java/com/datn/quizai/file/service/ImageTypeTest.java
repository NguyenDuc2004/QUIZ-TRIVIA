package com.datn.quizai.file.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test nhận dạng ảnh bằng chữ ký byte.
 * <p>
 * Đây là chốt chặn an ninh của chức năng tải file: nếu nó nhận nhầm, người dùng đẩy được
 * file bất kỳ lên rồi nhờ server phục vụ lại. Nên phủ cả ca hợp lệ lẫn ca đội lốt.
 */
class ImageTypeTest {

    /** Dựng mảng byte bắt đầu bằng các giá trị cho trước, phần còn lại là 0. */
    private static byte[] header(int... firstBytes) {
        byte[] header = new byte[ImageType.HEADER_SIZE];
        for (int i = 0; i < firstBytes.length && i < header.length; i++) {
            header[i] = (byte) firstBytes[i];
        }
        return header;
    }

    @Test
    @DisplayName("Nhận đúng JPEG, PNG và GIF")
    void shouldDetectCommonFormats() {
        assertThat(ImageType.detect(header(0xFF, 0xD8, 0xFF, 0xE0))).contains(ImageType.JPEG);
        assertThat(ImageType.detect(header(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
                .contains(ImageType.PNG);
        assertThat(ImageType.detect(header(0x47, 0x49, 0x46, 0x38, 0x39, 0x61))).contains(ImageType.GIF);
    }

    @Test
    @DisplayName("WebP phải có cả 'RIFF' lẫn nhãn 'WEBP'")
    void shouldDetectWebp() {
        byte[] webp = header(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 'W', 'E', 'B', 'P');
        assertThat(ImageType.detect(webp)).contains(ImageType.WEBP);
    }

    @Test
    @DisplayName("File RIFF khác (WAV) tuy trùng 4 byte đầu nhưng KHÔNG được nhận là ảnh")
    void shouldRejectOtherRiffFiles() {
        byte[] wav = header(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 'W', 'A', 'V', 'E');
        assertThat(ImageType.detect(wav)).isEmpty();
    }

    @Test
    @DisplayName("File thực thi / script đặt tên .jpg vẫn bị từ chối vì chữ ký byte không khớp")
    void shouldRejectDisguisedFiles() {
        // Ảnh giả: nội dung là script nhưng client khai Content-Type image/jpeg
        assertThat(ImageType.detect("<?php echo 1; ?>".getBytes())).isEmpty();
        // File thực thi Windows (MZ) và ELF của Linux
        assertThat(ImageType.detect(header(0x4D, 0x5A, 0x90, 0x00))).isEmpty();
        assertThat(ImageType.detect(header(0x7F, 'E', 'L', 'F'))).isEmpty();
    }

    @Test
    @DisplayName("File rỗng hoặc quá ngắn không nhận dạng được")
    void shouldRejectTooShortInput() {
        assertThat(ImageType.detect(new byte[0])).isEmpty();
        assertThat(ImageType.detect(new byte[]{(byte) 0xFF, (byte) 0xD8})).isEmpty();
    }
}
