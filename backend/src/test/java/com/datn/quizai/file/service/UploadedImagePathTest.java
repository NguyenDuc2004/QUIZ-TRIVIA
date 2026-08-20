package com.datn.quizai.file.service;

import com.datn.quizai.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Luật "chỉ nhận ảnh đã tải lên hệ thống này" (features/02, FR-11).
 *
 * <h3>Đây là luật AN TOÀN, không phải luật hiển thị</h3>
 * Nó từng nằm private trong {@code QuizService} cho ảnh bìa quiz. Tới khi câu hỏi cũng cần ảnh thì hoặc
 * nhân đôi luật, hoặc tách ra — tách ra, vì nhân đôi một luật an toàn nghĩa là lần sau ai đó nới nó ở một
 * chỗ mà quên chỗ kia, và <b>chỗ bị quên chính là lỗ hổng</b> vì không ai nghĩ nó còn tồn tại.
 *
 * <h3>Hai điều luật này chặn</h3>
 * <ol>
 *   <li><b>URL ngoài.</b> Mỗi lần người học mở đề là một request kèm IP gửi sang máy chủ lạ — người soạn
 *       đề nhúng được pixel theo dõi vào bài thi của người khác, và người bị theo dõi không hề biết.</li>
 *   <li><b>Thoát thư mục</b> bằng {@code ..} — đọc được tệp ngoài thư mục ảnh.</li>
 * </ol>
 */
class UploadedImagePathTest {

    @Test
    @DisplayName("Đường dẫn nội bộ do hệ thống sinh ra thì nhận")
    void shouldAcceptInternalPath() {
        assertThat(UploadedImagePath.hopLeHoacNull("/uploads/images/abc.png", "Ảnh câu hỏi"))
                .isEqualTo("/uploads/images/abc.png");
    }

    @Test
    @DisplayName("Bỏ trống là hợp lệ — ảnh không bắt buộc")
    void shouldTreatBlankAsNull() {
        // Câu hỏi chỉ có chữ là chuyện bình thường, không phải thiếu dữ liệu
        assertThat(UploadedImagePath.hopLeHoacNull(null, "Ảnh câu hỏi")).isNull();
        assertThat(UploadedImagePath.hopLeHoacNull("", "Ảnh câu hỏi")).isNull();
        assertThat(UploadedImagePath.hopLeHoacNull("   ", "Ảnh câu hỏi")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://ke-xau.example/pixel.gif",
            "http://cdn-ngoai.example/anh.png",
            "//cdn-ngoai.example/anh.png",
            "data:image/png;base64,iVBORw0KGgo=",
    })
    @DisplayName("URL NGOÀI bị từ chối — người soạn đề không nhúng được pixel theo dõi vào bài thi")
    void shouldRejectExternalUrl(String url) {
        BusinessException loi = catchThrowableOfType(BusinessException.class,
                () -> UploadedImagePath.hopLeHoacNull(url, "Ảnh câu hỏi"));

        assertThat(loi).isNotNull();
        assertThat(loi.getStatus().value()).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/uploads/../../etc/passwd",
            "/uploads/images/../../../secret.txt",
    })
    @DisplayName("Đường dẫn có `..` bị từ chối dù bắt đầu đúng tiền tố")
    void shouldRejectPathTraversal(String url) {
        // Chỉ kiểm tiền tố là chưa đủ: "/uploads/../.." vẫn bắt đầu bằng "/uploads/"
        assertThat(catchThrowableOfType(BusinessException.class,
                () -> UploadedImagePath.hopLeHoacNull(url, "Ảnh câu hỏi"))).isNotNull();
    }

    @Test
    @DisplayName("Thông báo lỗi nói RÕ ô nào sai")
    void shouldNameTheFieldInError() {
        // Form soạn đề có nhiều ô ảnh; "ảnh không hợp lệ" trơn thì người soạn phải đoán ô nào
        assertThat(catchThrowableOfType(BusinessException.class,
                () -> UploadedImagePath.hopLeHoacNull("http://x.example/a.png", "Ảnh bìa")).getMessage())
                .contains("Ảnh bìa");

        assertThat(catchThrowableOfType(BusinessException.class,
                () -> UploadedImagePath.hopLeHoacNull("http://x.example/a.png", "Ảnh câu hỏi")).getMessage())
                .contains("Ảnh câu hỏi");
    }

    @Test
    @DisplayName("Cắt khoảng trắng thừa")
    void shouldTrim() {
        assertThat(UploadedImagePath.hopLeHoacNull("  /uploads/images/a.png  ", "Ảnh câu hỏi"))
                .isEqualTo("/uploads/images/a.png");
    }
}
