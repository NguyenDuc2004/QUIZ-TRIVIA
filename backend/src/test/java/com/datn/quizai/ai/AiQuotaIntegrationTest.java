package com.datn.quizai.ai;

import com.datn.quizai.ai.service.AiQuotaService;
import com.datn.quizai.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Hạn mức số lượt gọi AI mỗi ngày (features/10, FR-84).
 *
 * <h3>Vì sao tính năng này phải có test tích hợp, không chỉ unit test</h3>
 * Nó từng bị hoãn với đúng lý do: một ô nhập hạn mức <b>không chặn được gì</b> còn tệ hơn không có ô nào,
 * vì quản trị viên sẽ tin rằng chi phí đã bị giới hạn. Nên thứ cần chứng minh không phải "lưu được số" mà
 * là "**thật sự chặn**" — và điều đó chỉ chứng minh được khi có Redis thật và bảng audit thật.
 *
 * <h3>Ba luật, mỗi luật hỏng theo một kiểu khác nhau</h3>
 * <ol>
 *   <li><b>null ≠ 0.</b> Lẫn hai thứ thì hoặc không cấm được ai, hoặc cấm sạch người dùng mới.</li>
 *   <li><b>Redis mất bộ đếm phải dựng lại được.</b> Redis chạy không bật AOF; không dựng lại thì một lần
 *       restart là xoá hạn mức của cả hệ thống, mà không ai nhận ra.</li>
 *   <li><b>Lần bị chặn không được tính là đã dùng.</b> Không lùi lại thì bộ đếm hiện ở khu quản trị leo
 *       mãi và thành vô nghĩa.</li>
 * </ol>
 */
@SpringBootTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = "app.ai.default-daily-quota=0")
class AiQuotaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private AiQuotaService quotaService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private UUID user;

    @BeforeEach
    void setUp() {
        user = taoNguoiDung();
        xoaBoDem(user);
    }

    @Test
    @DisplayName("Chưa đặt hạn mức riêng và hệ thống chưa bật: không chặn ai")
    void shouldNotBlockWhenQuotaIsNotConfigured() {
        // Mặc định hệ thống là 0 = CHƯA BẬT. Hiểu nhầm nó thành "cấm" là chặn sạch mọi người dùng ngay
        // lần đầu triển khai — và triệu chứng là "AI hỏng", rất khó lần ra nguyên nhân.
        assertThatCode(() -> {
            for (int i = 0; i < 50; i++) {
                quotaService.kiemTraVaGhiNhan(user);
            }
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Đặt hạn mức 3: lượt thứ tư bị chặn với 429")
    void shouldBlockAfterQuotaExhausted() {
        datHanMuc(user, 3);

        for (int i = 0; i < 3; i++) {
            quotaService.kiemTraVaGhiNhan(user);
        }
        BusinessException loi = catchThrowableOfType(BusinessException.class,
                () -> quotaService.kiemTraVaGhiNhan(user));

        assertThat(loi).isNotNull();
        assertThat(loi.getStatus().value()).isEqualTo(429);
        // Thông báo phải nói RÕ số và lúc nào được dùng lại — "hết hạn mức" trơn thì người dùng không biết
        // nên chờ hay nên báo lỗi
        assertThat(loi.getMessage()).contains("3").contains("00:00");
    }

    @Test
    @DisplayName("Lần bị chặn KHÔNG được tính là đã dùng")
    void shouldNotCountRejectedCall() {
        datHanMuc(user, 2);
        quotaService.kiemTraVaGhiNhan(user);
        quotaService.kiemTraVaGhiNhan(user);

        for (int i = 0; i < 5; i++) {
            catchThrowableOfType(BusinessException.class, () -> quotaService.kiemTraVaGhiNhan(user));
        }

        // Vẫn đúng 2, không phải 7. Không lùi lại thì con số ở khu quản trị leo mãi và mất hết ý nghĩa.
        assertThat(quotaService.daDungHomNay(user)).isEqualTo(2);
    }

    @Test
    @DisplayName("Hạn mức 0 đặt RIÊNG nghĩa là cấm — khác hẳn null")
    void shouldTreatExplicitZeroAsBan() {
        datHanMuc(user, 0);

        BusinessException loi = catchThrowableOfType(BusinessException.class,
                () -> quotaService.kiemTraVaGhiNhan(user));

        assertThat(loi).as("0 đặt riêng = cấm, phải chặn ngay lượt đầu").isNotNull();
        assertThat(loi.getStatus().value()).isEqualTo(429);
    }

    @Test
    @DisplayName("Xoá hạn mức riêng (về null) thì hết bị chặn")
    void shouldReturnToDefaultWhenQuotaCleared() {
        datHanMuc(user, 0);
        assertThat(catchThrowableOfType(BusinessException.class,
                () -> quotaService.kiemTraVaGhiNhan(user))).isNotNull();

        datHanMuc(user, null);

        assertThatCode(() -> quotaService.kiemTraVaGhiNhan(user)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis mất bộ đếm: dựng lại từ bảng audit, KHÔNG reset về 0")
    void shouldRebuildCounterFromAuditLog() {
        datHanMuc(user, 5);
        // Ba lời gọi đã ghi vào audit hôm nay (mô phỏng: đã chạy trước khi Redis restart)
        for (int i = 0; i < 3; i++) {
            ghiAudit(user);
        }
        xoaBoDem(user);   // Redis khởi động lại

        assertThat(quotaService.daDungHomNay(user))
                .as("dựng lại từ ai_request_logs, không phải bắt đầu lại từ 0")
                .isEqualTo(3);

        // Còn đúng 2 lượt, không phải 5
        quotaService.kiemTraVaGhiNhan(user);
        quotaService.kiemTraVaGhiNhan(user);
        assertThat(catchThrowableOfType(BusinessException.class,
                () -> quotaService.kiemTraVaGhiNhan(user)))
                .as("một lần restart Redis không được tặng thêm lượt cho ai")
                .isNotNull();
    }

    @Test
    @DisplayName("Tác vụ hệ thống (userId null) không bị chặn")
    void shouldNotBlockSystemTasks() {
        // Job nền không thuộc về ai; chặn nó bằng hạn mức của một người là gán chi phí sai chỗ
        assertThatCode(() -> quotaService.kiemTraVaGhiNhan(null)).doesNotThrowAnyException();
        assertThat(quotaService.daDungHomNay(null)).isZero();
    }

    @Test
    @DisplayName("Hạn mức của người này không ảnh hưởng người kia")
    void shouldIsolateQuotaPerUser() {
        UUID nguoiKhac = taoNguoiDung();
        xoaBoDem(nguoiKhac);
        datHanMuc(user, 1);
        datHanMuc(nguoiKhac, 1);

        quotaService.kiemTraVaGhiNhan(user);
        assertThat(catchThrowableOfType(BusinessException.class,
                () -> quotaService.kiemTraVaGhiNhan(user))).isNotNull();

        assertThatCode(() -> quotaService.kiemTraVaGhiNhan(nguoiKhac)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("kiemTra() KHÔNG cộng lượt — cộng cả lúc nhận việc lẫn lúc gọi mô hình là trừ đôi")
    void checkOnlyMustNotConsumeQuota() {
        datHanMuc(user, 3);

        for (int i = 0; i < 10; i++) {
            quotaService.kiemTra(user);
        }

        // Vẫn 0: nếu kiemTra() cộng lượt thì người dùng mất một nửa hạn mức mà không hiểu vì sao —
        // mỗi lần bấm bị trừ hai (một ở lúc nhận việc, một ở lúc gọi mô hình thật).
        assertThat(quotaService.daDungHomNay(user)).isZero();

        // Và vẫn còn đủ 3 lượt thật
        for (int i = 0; i < 3; i++) {
            quotaService.kiemTraVaGhiNhan(user);
        }
        assertThat(catchThrowableOfType(BusinessException.class,
                () -> quotaService.kiemTraVaGhiNhan(user))).isNotNull();
    }

    @Test
    @DisplayName("Hết lượt rồi thì kiemTra() chặn NGAY — người dùng biết trước khi tạo job")
    void checkOnlyMustBlockWhenExhausted() {
        // Đây là lỗi tìm ra khi chạy thật ngày 20/08: tác vụ AI nặng chạy nền nên `POST /ai/generate-questions`
        // trả 202 kèm jobId ngay, còn chốt hạn mức chỉ nằm ở tầng orchestrator mà luồng nền mới gọi tới.
        // Người đã bị cấm nhận 202 rồi mới thấy job hỏng — chi phí vẫn khống chế đúng (mô hình không bị gọi),
        // nhưng bấm mười lần là tạo mười job hỏng.
        datHanMuc(user, 1);
        quotaService.kiemTraVaGhiNhan(user);

        BusinessException loi = catchThrowableOfType(BusinessException.class,
                () -> quotaService.kiemTra(user));

        assertThat(loi).isNotNull();
        assertThat(loi.getStatus().value()).isEqualTo(429);
    }

    @Test
    @DisplayName("Bị CẤM (hạn mức 0) thì kiemTra() chặn ngay từ lượt đầu")
    void checkOnlyMustBlockBannedUser() {
        datHanMuc(user, 0);

        assertThat(catchThrowableOfType(BusinessException.class, () -> quotaService.kiemTra(user)))
                .isNotNull();
    }

    @Test
    @DisplayName("Chưa bật hạn mức thì kiemTra() không chặn ai")
    void checkOnlyMustNotBlockWhenNotConfigured() {
        assertThatCode(() -> quotaService.kiemTra(user)).doesNotThrowAnyException();
        assertThatCode(() -> quotaService.kiemTra(null)).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------------ trợ giúp

    private UUID taoNguoiDung() {
        return UUID.fromString(jdbc.queryForObject("""
                insert into users (id, email, password_hash, display_name, role)
                values (gen_random_uuid(), ?, 'x', 'Người dùng hạn mức', 'CREATOR')
                returning id::text
                """, String.class, "quota-" + UUID.randomUUID() + "@example.com"));
    }

    private void datHanMuc(UUID userId, Integer quota) {
        jdbc.update("update users set ai_daily_quota = ? where id = ?", quota, userId);
        xoaBoDem(userId);
    }

    private void ghiAudit(UUID userId) {
        jdbc.update("""
                insert into ai_request_logs (id, user_id, feature, provider, model, status, created_at)
                values (gen_random_uuid(), ?, 'generation', 'gemini', 'test-model', 'SUCCESS', now())
                """, userId);
    }

    private void xoaBoDem(UUID userId) {
        redisTemplate.delete("aiquota:" + userId + ":" + LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
    }
}
