package com.datn.quizai.file.service;

import com.datn.quizai.common.exception.BusinessException;

/**
 * Kiểm một đường dẫn ảnh có đúng là ảnh <b>đã tải lên hệ thống này</b> hay không.
 *
 * <h3>Vì sao là một chỗ dùng chung, không phải một hàm private trong mỗi service</h3>
 * Đây là <b>luật an toàn</b>, không phải luật hiển thị. Bản đầu chỉ có ảnh bìa quiz nên nó nằm private
 * trong {@code QuizService}; tới lúc câu hỏi cũng cần ảnh (FR-11) thì hoặc nhân đôi luật, hoặc tách ra.
 * Nhân đôi một luật an toàn nghĩa là lần sau ai đó nới nó ở một chỗ mà quên chỗ kia — và chỗ bị quên
 * chính là lỗ hổng, vì không ai nghĩ nó còn tồn tại.
 *
 * <h3>Vì sao không nhận URL ngoài</h3>
 * Hai lý do, và cái thứ hai mới là lý do thật:
 * <ol>
 *   <li>Ảnh bên thứ ba chết bất cứ lúc nào — đề thi mất hình giữa buổi kiểm tra.</li>
 *   <li>Mỗi lần người học mở đề là <b>một request kèm IP gửi sang máy chủ lạ</b>. Người soạn đề nhúng
 *       được một pixel theo dõi vào bài thi của người khác, và người bị theo dõi không hề biết.</li>
 * </ol>
 * Chặn cả {@code ..} để không ai ghép được đường dẫn thoát ra ngoài thư mục ảnh.
 */
public final class UploadedImagePath {

    /** Tiền tố mà {@code POST /api/v1/files/images} sinh ra. */
    private static final String TIEN_TO = "/uploads/";

    private UploadedImagePath() {
    }

    /**
     * @param url    đường dẫn client gửi lên; {@code null}/rỗng đều trả về {@code null}
     * @param tenAnh tên loại ảnh để đưa vào thông báo lỗi ("Ảnh bìa", "Ảnh câu hỏi") — người soạn đề cần
     *               biết ô nào sai khi form có nhiều ô ảnh
     * @throws BusinessException 400 khi đường dẫn không phải ảnh của hệ thống
     */
    public static String hopLeHoacNull(String url, String tenAnh) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String sach = url.trim();
        if (!sach.startsWith(TIEN_TO) || sach.contains("..")) {
            throw BusinessException.badRequest(tenAnh + " phải là ảnh đã tải lên hệ thống");
        }
        return sach;
    }
}
