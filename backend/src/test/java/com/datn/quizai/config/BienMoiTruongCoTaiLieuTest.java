package com.datn.quizai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mọi biến môi trường mà {@code application.yml} đọc đều phải có mặt trong {@code .env.example}.
 *
 * <h3>Vì sao cần một phép kiểm cho chuyện này</h3>
 * Một cần gạt không ai biết là một cần gạt không tồn tại. Lỗ này đã có thật và đã đủ nghiêm trọng:
 * <ul>
 *   <li>{@code AI_DEFAULT_DAILY_QUOTA} — hạn mức AI theo người. Cơ chế viết xong, có test, có màn
 *       quản trị, nhưng không có gì nhắc người triển khai bật nó. Mặc định là <b>không giới hạn</b>,
 *       mà màn đăng ký lại cho tự chọn vai trò gọi được AI.</li>
 *   <li>{@code UPLOAD_DIR} — mặc định là đường dẫn <b>tương đối</b>. Triển khai bằng container mà
 *       không trỏ ra volume riêng thì toàn bộ ảnh người dùng tải lên biến mất sau mỗi lần triển khai
 *       lại, và không có gì báo cho ai biết.</li>
 * </ul>
 * Cả hai đều không gây lỗi lúc chạy, không làm test nào đỏ, và chỉ lộ ra khi đã muộn.
 *
 * <h3>Phép kiểm này KHÔNG đòi giá trị phải giống nhau</h3>
 * Chỉ đòi cái <b>tên</b> có mặt. {@code .env.example} là bản mẫu để người triển khai biết có những
 * cần gạt nào, không phải bản sao cấu hình.
 */
class BienMoiTruongCoTaiLieuTest {

    /** Khớp `${TEN_BIEN:mặc định}` và `${TEN_BIEN}` trong application.yml. */
    private static final Pattern BIEN = Pattern.compile("\\$\\{([A-Z0-9_]+)[:}]");

    @Test
    @DisplayName("Mọi biến application.yml đọc đều có trong .env.example")
    void moiBienDeuCoTaiLieu() throws IOException {
        Path goc = timGocRepo();
        String yml = Files.readString(goc.resolve("backend/src/main/resources/application.yml"));
        String mau = Files.readString(goc.resolve(".env.example"));

        Set<String> thieu = new TreeSet<>();
        Matcher m = BIEN.matcher(yml);
        while (m.find()) {
            if (!mau.contains(m.group(1))) {
                thieu.add(m.group(1));
            }
        }

        assertThat(thieu)
                .as("Thêm các biến này vào .env.example kèm một dòng nói nó làm gì. "
                        + "Một cần gạt không ai biết là một cần gạt không tồn tại.")
                .isEmpty();
    }

    /**
     * Đi ngược lên tới thư mục chứa {@code .env.example}.
     * <p>
     * Không gắn cứng {@code ".."}: test chạy với thư mục làm việc là {@code backend/} khi gọi bằng
     * Maven, nhưng IDE có thể đặt ở gốc repo. Gắn cứng thì nó xanh ở chỗ này và đỏ ở chỗ kia, mà lý
     * do thì chẳng liên quan gì tới thứ đang được kiểm.
     */
    private Path timGocRepo() {
        Path thuMuc = Path.of("").toAbsolutePath();
        for (Path p = thuMuc; p != null; p = p.getParent()) {
            if (Files.exists(p.resolve(".env.example"))) {
                return p;
            }
        }
        throw new IllegalStateException("Không tìm thấy .env.example từ " + thuMuc);
    }

    @Test
    @DisplayName(".env.example KHÔNG chứa giá trị thật của secret")
    void mauKhongChuaSecret() throws IOException {
        List<String> khoaSecret = List.of("GEMINI_API_KEY", "GROQ_API_KEY", "APP_ADMIN_PASSWORD",
                "MAIL_PASSWORD", "GOOGLE_CLIENT_SECRET");

        for (String dong : Files.readAllLines(timGocRepo().resolve(".env.example"))) {
            String sach = dong.strip();
            for (String khoa : khoaSecret) {
                if (sach.startsWith(khoa + "=")) {
                    assertThat(sach.substring(khoa.length() + 1))
                            .as("%s trong .env.example phải để TRỐNG — đây là tệp được commit", khoa)
                            .isEmpty();
                }
            }
        }
    }
}
