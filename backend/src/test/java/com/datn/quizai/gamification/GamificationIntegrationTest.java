package com.datn.quizai.gamification;

import com.datn.quizai.attempt.service.AttemptSubmittedEvent;
import com.datn.quizai.flashcard.service.FlashcardReviewedEvent;
import com.datn.quizai.gamification.service.GamificationEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Gamification (features/13).
 * <p>
 * Phép tính cấp độ và chuỗi ngày đã có {@code LevelAndStreakTest} kiểm riêng, nên ở đây <b>không kiểm lại
 * công thức</b>. Lớp này kiểm những gì chỉ hỏng khi có cơ sở dữ liệu thật:
 * <ol>
 *   <li><b>Idempotent</b> — cùng một hành động chạy hai lần chỉ cộng XP một lần. Đây là điều được tuyên bố
 *       trong đặc tả, nên phải có phép kiểm cho nó.</li>
 *   <li><b>Ôn thẻ không thành máy in XP</b> — bấm một thẻ nhiều lần trong ngày chỉ cộng một lần.</li>
 *   <li><b>Huy hiệu tự trao</b> đúng lúc đạt điều kiện, và không trao hai lần.</li>
 *   <li><b>Không có đường ghi qua API</b> — XP chỉ đến từ hành động học thật.</li>
 * </ol>
 * Gọi thẳng listener thay vì đi qua luồng nộp bài đầy đủ: luồng đó cần quiz, câu hỏi, bắt đầu, nộp — bốn
 * bước không liên quan tới thứ đang kiểm, mà mỗi bước là một chỗ có thể vỡ vì lý do khác.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GamificationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private GamificationEventListener listener;

    private String token;
    private UUID userId;

    @BeforeAll
    void setUp() throws Exception {
        token = register("gam-chinh@example.com");
        userId = UUID.fromString(jdbc.queryForObject("select id::text from users where email = ?",
                String.class, "gam-chinh@example.com"));
    }

    // ======================================================== 1. Idempotent

    @Test
    @DisplayName("Cùng một bài làm xử lý hai lần chỉ cộng XP MỘT lần")
    void awardingIsIdempotent() {
        UUID user = taoNguoiDung("gam-idempotent@example.com");
        UUID attemptId = taoBaiLam(user, 5, 10);   // không hoàn hảo

        listener.onAttemptSubmitted(new AttemptSubmittedEvent(attemptId, user));
        int lan1 = tongXp(user);
        assertThat(lan1).as("lần đầu phải cộng XP").isPositive();

        // Chạy lại đúng sự kiện đó — mô phỏng retry của luồng nền
        listener.onAttemptSubmitted(new AttemptSubmittedEvent(attemptId, user));

        assertThat(tongXp(user)).as("cộng lần hai là sai — đặc tả yêu cầu idempotent").isEqualTo(lan1);
        assertThat(demXpEvent(user)).isEqualTo(1);
    }

    @Test
    @DisplayName("Bài làm đúng 100% được thưởng thêm, bài thường thì không")
    void perfectAttemptGetsBonus() {
        UUID thuong = taoNguoiDung("gam-thuong@example.com");
        UUID hoanHao = taoNguoiDung("gam-hoanhao@example.com");

        listener.onAttemptSubmitted(new AttemptSubmittedEvent(taoBaiLam(thuong, 5, 10), thuong));
        listener.onAttemptSubmitted(new AttemptSubmittedEvent(taoBaiLam(hoanHao, 10, 10), hoanHao));

        assertThat(tongXp(hoanHao)).isGreaterThan(tongXp(thuong));
    }

    @Test
    @DisplayName("Bài KHÔNG có câu nào (max_score = 0) không được tính là hoàn hảo")
    void emptyAttemptIsNotPerfect() {
        UUID user = taoNguoiDung("gam-bai-rong@example.com");
        UUID thamChieu = taoNguoiDung("gam-tham-chieu@example.com");

        // 0 = 0 nên nếu không chặn max_score > 0 thì bài rỗng được thưởng như bài đúng hết, và huy hiệu
        // "Điểm tuyệt đối" được trao cho một bài không làm gì
        listener.onAttemptSubmitted(new AttemptSubmittedEvent(taoBaiLam(user, 0, 0), user));
        listener.onAttemptSubmitted(new AttemptSubmittedEvent(taoBaiLam(thamChieu, 5, 10), thamChieu));

        assertThat(tongXp(user)).isEqualTo(tongXp(thamChieu));
        assertThat(coHuyHieu(user, "PERFECT_1")).isFalse();
    }

    // ======================================================== 2. Ôn thẻ không thành máy in XP

    @Test
    @DisplayName("Ôn cùng một thẻ nhiều lần trong ngày chỉ cộng XP một lần")
    void reviewingSameCardTwiceInADayAwardsOnce() {
        UUID user = taoNguoiDung("gam-on-the@example.com");
        UUID cardId = UUID.randomUUID();
        LocalDate homNay = LocalDate.now();

        listener.onFlashcardReviewed(new FlashcardReviewedEvent(user, cardId, homNay));
        int lan1 = tongXp(user);

        // API ôn không chặn ôn sớm, nên không có chốt này thì bấm một thẻ trăm lần là trăm lần XP
        listener.onFlashcardReviewed(new FlashcardReviewedEvent(user, cardId, homNay));
        listener.onFlashcardReviewed(new FlashcardReviewedEvent(user, cardId, homNay));

        assertThat(tongXp(user)).isEqualTo(lan1);
    }

    @Test
    @DisplayName("Cùng một thẻ nhưng NGÀY KHÁC thì được cộng lại — thưởng người ôn đều")
    void reviewingSameCardOnAnotherDayAwardsAgain() {
        UUID user = taoNguoiDung("gam-on-the-2-ngay@example.com");
        UUID cardId = UUID.randomUUID();

        listener.onFlashcardReviewed(new FlashcardReviewedEvent(user, cardId, LocalDate.now().minusDays(1)));
        int sauNgayDau = tongXp(user);

        listener.onFlashcardReviewed(new FlashcardReviewedEvent(user, cardId, LocalDate.now()));

        // Khoá là `cardId:ngày` nên ngày khác là hành động khác. Nếu khoá chỉ có cardId thì người ôn đều
        // mỗi ngày chỉ được cộng XP đúng một lần trong cả đời — ngược hẳn với mục đích của tính năng.
        assertThat(tongXp(user)).isGreaterThan(sauNgayDau);
    }

    // ======================================================== 3. Huy hiệu

    @Test
    @DisplayName("Huy hiệu tự trao khi đạt điều kiện, và không trao hai lần")
    void badgesAreGrantedOnceWhenEarned() {
        UUID user = taoNguoiDung("gam-huy-hieu@example.com");

        // Một bài hoàn hảo → đủ điều kiện PERFECT_1, và 35 XP đủ điều kiện FIRST_STEPS (ngưỡng 50 thì chưa)
        listener.onAttemptSubmitted(new AttemptSubmittedEvent(taoBaiLam(user, 10, 10), user));
        assertThat(coHuyHieu(user, "PERFECT_1")).isTrue();

        // Thêm bài nữa để vượt 50 XP
        listener.onAttemptSubmitted(new AttemptSubmittedEvent(taoBaiLam(user, 10, 10), user));
        assertThat(coHuyHieu(user, "FIRST_STEPS")).isTrue();

        // Ràng buộc uk_user_badges chặn trao trùng; kiểm bằng số dòng chứ không bằng việc không có lỗi
        assertThat(jdbc.queryForObject("""
                select count(*) from user_badges ub join badges b on b.id = ub.badge_id
                where ub.user_id = ? and b.code = 'PERFECT_1'
                """, Long.class, user)).isEqualTo(1);
    }

    @Test
    @DisplayName("Huy hiệu chưa đạt vẫn trả về trong danh sách, với earnedAt = null")
    void unearnedBadgesAreStillListed() throws Exception {
        String body = mockMvc.perform(get("/api/v1/gamification/badges")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var badges = objectMapper.readTree(body);
        // Danh sách chỉ có huy hiệu đã đạt thì không tạo được động lực nào — người học không thấy còn gì
        // để hướng tới
        assertThat(badges).hasSizeGreaterThanOrEqualTo(10);
        assertThat(badges).anyMatch(b -> b.get("earnedAt").isNull());
    }

    // ======================================================== 4. Không có đường ghi

    @Test
    @DisplayName("Không có endpoint nào cộng XP: POST vào /gamification bị từ chối")
    void thereIsNoWriteEndpoint() throws Exception {
        // Mở đường ghi qua API là mở đường tự cộng điểm cho mình, và khi đó huy hiệu mất hết ý nghĩa
        mockMvc.perform(post("/api/v1/gamification/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalXp\":999999}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Khách chưa đăng nhập nhận 401 ở mọi endpoint gamification")
    void guestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/gamification/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/gamification/badges")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/gamification/daily")).andExpect(status().isUnauthorized());
    }

    // ======================================================== 5. Tổng quan & thử thách ngày

    @Test
    @DisplayName("Tổng quan có tiến độ trong cấp và cờ 'hôm nay đã học chưa'")
    void overviewCarriesProgressAndTodayFlag() throws Exception {
        mockMvc.perform(get("/api/v1/gamification/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(1))
                // Chuỗi 5 ngày có thể là "đã học hôm nay" hoặc "học đến hôm qua" — hai trạng thái khác nhau
                // hoàn toàn với người dùng, nên cần cờ riêng
                .andExpect(jsonPath("$.streakConHomNay").value(false))
                .andExpect(jsonPath("$.xpCanTrongCap").value(100))
                .andExpect(jsonPath("$.tongSoHuyHieu").exists());
    }

    @Test
    @DisplayName("Thử thách ngày được tạo ở lần hỏi đầu tiên, và hỏi lại vẫn ra đúng một thử thách")
    void dailyChallengeIsCreatedOnDemandAndStable() throws Exception {
        String lan1 = mockMvc.perform(get("/api/v1/gamification/daily")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.target").isNumber())
                .andReturn().getResponse().getContentAsString();

        String lan2 = mockMvc.perform(get("/api/v1/gamification/daily")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Mở lại trang không được đổi sang thử thách khác
        assertThat(objectMapper.readTree(lan1).get("id"))
                .isEqualTo(objectMapper.readTree(lan2).get("id"));
        assertThat(jdbc.queryForObject("select count(*) from daily_challenges where challenge_date = current_date",
                Long.class)).isEqualTo(1);
    }

    // ================================================================ helper

    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người học","role":"LEARNER"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    /** Tài khoản riêng cho mỗi ca: lớp này không dọn cơ sở dữ liệu giữa các ca nên XP sẽ tích lại. */
    private UUID taoNguoiDung(String email) {
        return UUID.fromString(jdbc.queryForObject("""
                insert into users (id, email, display_name, role, password_hash, created_at, updated_at)
                values (gen_random_uuid(), ?, 'Người học', 'LEARNER', 'x', now(), now())
                returning id::text
                """, String.class, email));
    }

    private UUID taoBaiLam(UUID user, int diem, int diemToiDa) {
        UUID quizId = UUID.fromString(jdbc.queryForObject("""
                insert into quizzes (id, owner_id, title, visibility, difficulty)
                values (gen_random_uuid(), ?, 'Quiz cho gamification', 'PRIVATE', 'MEDIUM')
                returning id::text
                """, String.class, user));
        return UUID.fromString(jdbc.queryForObject("""
                insert into quiz_attempts (id, user_id, quiz_id, mode, status, started_at,
                                          total_score, max_score)
                values (gen_random_uuid(), ?, ?, 'PRACTICE', 'SUBMITTED', now(), ?, ?)
                returning id::text
                """, String.class, user, quizId, diem, diemToiDa));
    }

    private int tongXp(UUID user) {
        Integer xp = jdbc.queryForObject("select total_xp from user_stats where user_id = ?",
                Integer.class, user);
        return xp == null ? 0 : xp;
    }

    private long demXpEvent(UUID user) {
        Long n = jdbc.queryForObject("select count(*) from xp_events where user_id = ?", Long.class, user);
        return n == null ? 0 : n;
    }

    private boolean coHuyHieu(UUID user, String code) {
        Long n = jdbc.queryForObject("""
                select count(*) from user_badges ub join badges b on b.id = ub.badge_id
                where ub.user_id = ? and b.code = ?
                """, Long.class, user, code);
        return n != null && n > 0;
    }
}
