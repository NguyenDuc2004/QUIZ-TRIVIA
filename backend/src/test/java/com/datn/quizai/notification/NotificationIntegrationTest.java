package com.datn.quizai.notification;

import com.datn.quizai.attempt.service.AttemptSubmittedEvent;
import com.datn.quizai.gamification.service.GamificationEventListener;
import com.datn.quizai.notification.domain.NotificationType;
import com.datn.quizai.notification.service.SrsReminderJob;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.RequestBuilder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Thông báo & nhắc ôn tập (features/16).
 * <p>
 * Bốn nhóm điều chỉ hỏng khi có cơ sở dữ liệu thật, nên phải kiểm ở đây chứ không kiểm bằng mock:
 * <ol>
 *   <li><b>Không gửi trùng</b> — job hằng ngày sớm muộn cũng chạy hai lần, và chốt là ràng buộc duy nhất trên
 *       {@code (user_id, dedupe_key)}. Mock repository thì cái được kiểm chỉ là "service có gọi save không".</li>
 *   <li><b>Cài đặt được tôn trọng ở lúc TẠO</b>, không phải lúc đọc — tắt loại nào thì không có dòng nào.</li>
 *   <li><b>Thông báo là của riêng một người</b> — không đọc, không đánh dấu được của người khác.</li>
 *   <li><b>FR-53 đã trả</b> — lên cấp và mở khoá huy hiệu sinh thông báo. Đây là món nợ tính năng 13 để lại.</li>
 * </ol>
 * Gọi thẳng listener/job thay vì đi qua luồng HTTP đầy đủ ở phần sinh thông báo: luồng nộp bài cần quiz, câu
 * hỏi, bắt đầu, nộp — bốn bước không liên quan tới thứ đang kiểm mà mỗi bước là một chỗ có thể vỡ vì lý do
 * khác. Phần đọc thì vẫn đi qua HTTP thật, vì phân quyền chỉ có nghĩa ở tầng đó.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationIntegrationTest {

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
    private SrsReminderJob srsReminderJob;
    @Autowired
    private GamificationEventListener gamificationListener;

    private String token;
    private UUID userId;

    @BeforeAll
    void setUp() throws Exception {
        token = register("thongbao-chinh@example.com");
        userId = idCuaEmail("thongbao-chinh@example.com");
    }

    // ======================================================== 1. FR-66 — nhắc ôn tập, không gửi trùng

    @Test
    @DisplayName("Có thẻ đến hạn thì được nhắc, và chạy job LẦN HAI không gửi thêm")
    void shouldRemindOncePerDay() {
        UUID user = taoNguoiDung("thongbao-srs@example.com");
        taoTheDenHan(user, 3, LocalDate.now().minusDays(1));

        LocalDate homNay = LocalDate.now();
        assertThat(srsReminderJob.nhacOnTap(homNay)).as("lần đầu phải gửi").isPositive();
        assertThat(demThongBao(user, NotificationType.SRS_REMINDER)).isEqualTo(1);

        // Chạy lại đúng ngày đó — deploy lại giữa trưa, hai instance cùng thức, hay gọi tay để thử
        srsReminderJob.nhacOnTap(homNay);

        assertThat(demThongBao(user, NotificationType.SRS_REMINDER))
                .as("gửi lần hai là kiểu lỗi làm người ta tắt thông báo vĩnh viễn")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Nội dung nhắc nói ĐÚNG số thẻ đến hạn, kể cả thẻ quá hạn từ hôm trước")
    void shouldCountOverdueCards() {
        UUID user = taoNguoiDung("thongbao-quahan@example.com");
        taoTheDenHan(user, 2, LocalDate.now().minusDays(5));   // quá hạn lâu
        taoTheDenHan(user, 3, LocalDate.now());                // đến hạn hôm nay

        srsReminderJob.nhacOnTap(LocalDate.now());

        // 5, không phải 3: thẻ quá hạn từ những ngày không mở ứng dụng vẫn phải được nhắc — đó đúng là lúc
        // người ta cần ôn nhất
        assertThat(tieuDeDauTien(user)).contains("5 thẻ");
    }

    @Test
    @DisplayName("Người KHÔNG có thẻ đến hạn thì không nhận gì — job không gửi cho cả hệ thống")
    void shouldNotRemindUsersWithoutDueCards() {
        UUID user = taoNguoiDung("thongbao-khongthe@example.com");
        // Không tạo thẻ nào. Truy vấn `group by` chỉ trả về người thật sự có thẻ, nên người này không xuất hiện.
        UUID nguoiKhac = taoNguoiDung("thongbao-nguoikhac@example.com");
        taoTheDenHan(nguoiKhac, 1, LocalDate.now());

        srsReminderJob.nhacOnTap(LocalDate.now());

        assertThat(demThongBao(user, NotificationType.SRS_REMINDER)).isZero();
        // Đối chứng: nếu thiếu dòng này thì test vẫn xanh cả khi job chẳng gửi cho ai — tức là nó không kiểm
        // được điều nào cả
        assertThat(demThongBao(nguoiKhac, NotificationType.SRS_REMINDER))
                .as("job phải thật sự có chạy và có gửi cho người CÓ thẻ đến hạn")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Thẻ đến hạn NGÀY MAI thì hôm nay chưa nhắc")
    void shouldNotRemindForFutureCards() {
        UUID user = taoNguoiDung("thongbao-mai@example.com");
        taoTheDenHan(user, 4, LocalDate.now().plusDays(1));

        srsReminderJob.nhacOnTap(LocalDate.now());
        assertThat(demThongBao(user, NotificationType.SRS_REMINDER)).isZero();

        // Đối chứng: chính người này, chính bộ thẻ này, chạy job cho NGÀY MAI thì phải được nhắc. Không có
        // bước này thì test cũng xanh khi fixture tạo thẻ sai hoặc job không chạy gì cả.
        srsReminderJob.nhacOnTap(LocalDate.now().plusDays(1));
        assertThat(demThongBao(user, NotificationType.SRS_REMINDER))
                .as("thẻ đến hạn ngày mai thì ngày mai phải nhắc")
                .isEqualTo(1);
    }

    // ======================================================== 2. Cài đặt được tôn trọng lúc TẠO

    @Test
    @DisplayName("Tắt loại nhắc ôn thì KHÔNG có dòng nào trong CSDL, không phải chỉ bị ẩn khi đọc")
    void shouldNotEvenStoreDisabledType() throws Exception {
        String tokenTat = register("thongbao-tat@example.com");
        UUID user = idCuaEmail("thongbao-tat@example.com");
        taoTheDenHan(user, 6, LocalDate.now());

        mockMvc.perform(put("/api/v1/notifications/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenTat)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabledTypes\":[\"SRS_REMINDER\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disabledTypes[0]").value("SRS_REMINDER"));

        srsReminderJob.nhacOnTap(LocalDate.now());

        // Kiểm ở CSDL, không kiểm qua API: lọc lúc đọc thì bảng vẫn phình theo thứ người dùng đã nói là
        // không muốn, và một ngày nào đó có ai viết truy vấn khác quên mất bộ lọc
        assertThat(demThongBao(user, NotificationType.SRS_REMINDER)).isZero();
    }

    @Test
    @DisplayName("Bật lại thì nhắc lại được — nhưng không phải cho ngày đã bị bỏ qua")
    void shouldRemindAgainAfterReEnabling() throws Exception {
        String tokenBatLai = register("thongbao-batlai@example.com");
        UUID user = idCuaEmail("thongbao-batlai@example.com");
        taoTheDenHan(user, 2, LocalDate.now());

        capNhatCaiDat(tokenBatLai, "[\"SRS_REMINDER\"]");
        srsReminderJob.nhacOnTap(LocalDate.now());
        assertThat(demThongBao(user, NotificationType.SRS_REMINDER)).isZero();

        capNhatCaiDat(tokenBatLai, "[]");
        srsReminderJob.nhacOnTap(LocalDate.now().plusDays(1));

        assertThat(demThongBao(user, NotificationType.SRS_REMINDER)).isEqualTo(1);
    }

    @Test
    @DisplayName("Loại SYSTEM không tắt được — gửi lên vẫn bị bỏ qua, không báo lỗi")
    void shouldIgnoreAttemptToDisableSystem() throws Exception {
        // SYSTEM là kênh nói những việc người dùng CẦN biết (bảo trì, sự cố dữ liệu). Cho tắt là để người
        // dùng tự bỏ tai nghe rồi mình lại yên tâm là đã thông báo.
        mockMvc.perform(put("/api/v1/notifications/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabledTypes\":[\"SYSTEM\",\"ACHIEVEMENT\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disabledTypes.length()").value(1))
                .andExpect(jsonPath("$.disabledTypes[0]").value("ACHIEVEMENT"));

        capNhatCaiDat(token, "[]");   // dọn lại cho các test sau
    }

    @Test
    @DisplayName("Trang cài đặt chỉ liệt kê loại ĐÃ CÓ nguồn phát")
    void shouldOnlyOfferTypesWithProducers() throws Exception {
        // Công tắc cho loại chưa ai gửi là công tắc không làm gì — đúng cái đã hoãn ở FR-84 (hạn mức AI).
        // ASSIGNMENT_DUE đã có nguồn phát từ tính năng 14 (job nhắc hạn nộp). ROOM_INVITE thì chưa: phòng
        // đấu vào bằng mã PIN, không có cơ chế mời.
        JsonNode caiDat = json(get("/api/v1/notifications/settings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));

        var ten = new java.util.ArrayList<String>();
        caiDat.get("dieuChinhDuoc").forEach(n -> ten.add(n.get("type").asText()));

        assertThat(ten).containsExactlyInAnyOrder("SRS_REMINDER", "ACHIEVEMENT", "ASSIGNMENT_DUE");
        assertThat(ten).as("SYSTEM không tắt được nên không hiện công tắc").doesNotContain("SYSTEM");
        assertThat(ten).as("ROOM_INVITE chưa có nguồn phát").doesNotContain("ROOM_INVITE");
    }

    // ======================================================== 3. Thông báo là của riêng một người

    @Test
    @DisplayName("Chỉ thấy thông báo của mình; số chưa đọc và danh sách đều theo người gọi")
    void shouldOnlyListOwnNotifications() throws Exception {
        String tokenA = register("thongbao-a@example.com");
        UUID a = idCuaEmail("thongbao-a@example.com");
        UUID b = taoNguoiDung("thongbao-b@example.com");

        taoThongBaoTay(a, "Của A");
        taoThongBaoTay(b, "Của B");

        JsonNode ds = json(get("/api/v1/notifications")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA));

        assertThat(ds.get("totalElements").asInt()).isEqualTo(1);
        assertThat(ds.get("content").get(0).get("title").asText()).isEqualTo("Của A");
    }

    @Test
    @DisplayName("Đánh dấu đã đọc thông báo của NGƯỜI KHÁC không làm gì cả, và không tiết lộ id có thật")
    void shouldNotMarkOthersNotificationAsRead() throws Exception {
        String tokenA = register("thongbao-cheo-a@example.com");
        UUID b = taoNguoiDung("thongbao-cheo-b@example.com");
        UUID cuaB = taoThongBaoTay(b, "Riêng của B");

        mockMvc.perform(put("/api/v1/notifications/{id}/read", cuaB)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        assertThat(daDoc(cuaB)).as("thông báo của B phải còn nguyên chưa đọc").isFalse();
    }

    @Test
    @DisplayName("Đánh dấu đã đọc: một cái, rồi tất cả — số chưa đọc giảm đúng")
    void shouldMarkReadAndCountUnread() throws Exception {
        String tokenC = register("thongbao-doc@example.com");
        UUID c = idCuaEmail("thongbao-doc@example.com");
        UUID t1 = taoThongBaoTay(c, "Một");
        taoThongBaoTay(c, "Hai");
        taoThongBaoTay(c, "Ba");

        assertThat(soChuaDoc(tokenC)).isEqualTo(3);

        mockMvc.perform(put("/api/v1/notifications/{id}/read", t1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenC))
                .andExpect(status().isNoContent());
        assertThat(soChuaDoc(tokenC)).isEqualTo(2);

        // Trả về SỐ DÒNG VỪA ĐỔI, không phải tổng số thông báo: cái đã đọc từ trước không được đếm lại
        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daDanhDau").value(2));

        assertThat(soChuaDoc(tokenC)).isZero();
    }

    @Test
    @DisplayName("Chưa đăng nhập thì không đọc được thông báo của ai")
    void shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notifications/unread-count")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("KHÔNG có endpoint tạo thông báo — đó sẽ là kênh spam sẵn có")
    void shouldNotExposeCreateEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"%s\",\"title\":\"spam\"}".formatted(userId)))
                .andExpect(status().isMethodNotAllowed());
    }

    // ======================================================== 4. FR-53 — món nợ của tính năng 13

    @Test
    @DisplayName("Lên cấp sinh thông báo thành tích, và XỬ LÝ LẠI CÙNG SỰ KIỆN không sinh cái thứ hai")
    void shouldNotifyOnLevelUp() {
        UUID user = taoNguoiDung("thongbao-lencap@example.com");

        // Cấp 2 cần 100 XP tích luỹ (LevelCalculator: 100 * level^1.5). Mỗi bài đúng 100% cho
        // XP_NOP_BAI 20 + XP_THUONG_HOAN_HAO 15 = 35 XP, nên phải BA bài mới vượt ngưỡng (105 XP).
        // Bản đầu của test này chỉ nộp hai bài (70 XP) — người dùng chưa từng lên cấp 2, và thông
        // báo nó đếm được thật ra là HUY HIỆU. Nói dối về chính thứ mình kiểm.
        UUID baiCuoi = null;
        for (int i = 0; i < 3; i++) {
            baiCuoi = taoBaiLam(user, 10, 10);
            gamificationListener.onAttemptSubmitted(new AttemptSubmittedEvent(baiCuoi, user));
        }

        assertThat(capCua(user))
                .as("ba bài hoàn hảo = 105 XP, phải vượt ngưỡng 100 XP của cấp 2")
                .isEqualTo(2);
        assertThat(tieuDeThanhTich(user, "level:2"))
                .as("FR-53: lên cấp phải có thông báo, khoá chống trùng là level:{cấp}")
                .isNotNull();

        long truoc = demThongBao(user, NotificationType.ACHIEVEMENT);

        // XỬ LÝ LẠI ĐÚNG SỰ KIỆN ĐÓ — đây mới là "retry", thứ khoá chống trùng tồn tại để chặn.
        // Nộp THÊM một bài mới thì khác hẳn: XP tăng tiếp nên có thể mở huy hiệu mới, và một thông
        // báo mới lúc đó là ĐÚNG. Lẫn hai chuyện này là lý do bản cũ đỏ.
        gamificationListener.onAttemptSubmitted(new AttemptSubmittedEvent(baiCuoi, user));

        assertThat(demThongBao(user, NotificationType.ACHIEVEMENT))
                .as("chạy lại cùng một sự kiện không được sinh thông báo thứ hai")
                .isEqualTo(truoc);
    }

    @Test
    @DisplayName("Làm THÊM bài mới mà mở được huy hiệu mới thì VẪN phải có thông báo mới")
    void shouldNotifyAgainForADifferentAchievement() {
        UUID user = taoNguoiDung("thongbao-huyhieu-moi@example.com");

        // Bài 1 hoàn hảo — mở huy hiệu PERFECT_1
        gamificationListener.onAttemptSubmitted(
                new AttemptSubmittedEvent(taoBaiLam(user, 10, 10), user));
        long sauBai1 = demThongBao(user, NotificationType.ACHIEVEMENT);
        assertThat(sauBai1).isPositive();

        // Làm đủ 10 bài hoàn hảo để mở PERFECT_10 — một huy hiệu CHẮC CHẮN chưa thể có trước đó.
        //
        // Bản cũ chỉ nộp thêm một bài và trông chờ tổng XP vượt ngưỡng 50 của huy hiệu FIRST_STEPS.
        // Phép tính đó bỏ sót một nguồn XP thứ hai: cùng sự kiện nộp bài còn ghi tiến độ THỬ THÁCH HẰNG
        // NGÀY, và thử thách được chọn theo *số ngày trong năm* (`DailyChallengeService`).
        //
        // Hệ quả: những ngày mà mẫu thử thách rơi vào "Làm đúng 100% một bài quiz" (thưởng 80 XP), bài 1
        // đã được 35 + 80 = 115 XP và mở luôn FIRST_STEPS — nên bài 2 không còn huy hiệu nào mới và test
        // đỏ. Ba ngày còn lại trong chu kỳ bốn ngày thì xanh. Một phép kiểm **xanh 3 ngày, đỏ 1 ngày**
        // theo đúng nghĩa đen, và không có gì trong thông báo lỗi chỉ ra nguyên nhân là cuốn lịch.
        //
        // PERFECT_10 đếm số bài hoàn hảo, không đếm XP, nên nó nằm ngoài tầm với của mọi nguồn XP phụ.
        for (int i = 0; i < 9; i++) {
            gamificationListener.onAttemptSubmitted(
                    new AttemptSubmittedEvent(taoBaiLam(user, 10, 10), user));
        }

        // Khoá chống trùng là `badge:{mã}`, nên huy hiệu khác mã thì phải qua được — chống trùng
        // KHÔNG có nghĩa là "mỗi người một thông báo thành tích".
        assertThat(demThongBao(user, NotificationType.ACHIEVEMENT))
                .as("huy hiệu khác thì khoá chống trùng khác, phải sinh thông báo mới")
                .isGreaterThan(sauBai1);
    }

    @Test
    @DisplayName("Thông báo thành tích mang data để giao diện điều hướng được")
    void shouldCarryNavigationData() {
        UUID user = taoNguoiDung("thongbao-data@example.com");
        gamificationListener.onAttemptSubmitted(
                new AttemptSubmittedEvent(taoBaiLam(user, 10, 10), user));

        String data = jdbc.queryForObject("""
                select data::text from notifications
                where user_id = ? and type = 'ACHIEVEMENT' order by created_at limit 1
                """, String.class, user);

        assertThat(data).isNotNull();
        assertThat(data).contains("kind");
    }

    @Test
    @DisplayName("Tắt loại thành tích thì lên cấp không sinh thông báo")
    void shouldRespectSettingsForAchievements() throws Exception {
        String tokenTat = register("thongbao-tat-thanhtich@example.com");
        UUID user = idCuaEmail("thongbao-tat-thanhtich@example.com");
        capNhatCaiDat(tokenTat, "[\"ACHIEVEMENT\"]");

        gamificationListener.onAttemptSubmitted(
                new AttemptSubmittedEvent(taoBaiLam(user, 10, 10), user));

        assertThat(demThongBao(user, NotificationType.ACHIEVEMENT)).isZero();
        // Nhưng XP vẫn cộng: tắt thông báo là tắt việc được nhắc, không phải tắt việc học được tính
        Integer xp = jdbc.queryForObject("select total_xp from user_stats where user_id = ?",
                Integer.class, user);
        assertThat(xp).isPositive();
    }

    // ================================================================ helper

    private void capNhatCaiDat(String token, String mangJson) throws Exception {
        mockMvc.perform(put("/api/v1/notifications/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disabledTypes\":%s}".formatted(mangJson)))
                .andExpect(status().isOk());
    }

    private long soChuaDoc(String token) throws Exception {
        return json(get("/api/v1/notifications/unread-count")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).get("soChuaDoc").asLong();
    }

    private JsonNode json(RequestBuilder request) throws Exception {
        return objectMapper.readTree(mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người dùng"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private UUID idCuaEmail(String email) {
        return UUID.fromString(jdbc.queryForObject("select id::text from users where email = ?",
                String.class, email));
    }

    /** Người dùng tạo thẳng bằng SQL — nhanh hơn đăng ký qua HTTP khi test không cần token của họ. */
    private UUID taoNguoiDung(String email) {
        return UUID.fromString(jdbc.queryForObject("""
                insert into users (id, email, password_hash, display_name, role)
                values (gen_random_uuid(), ?, 'x', 'Người dùng', 'LEARNER')
                returning id::text
                """, String.class, email));
    }

    /** Một bộ thẻ với {@code soThe} thẻ, tất cả đến hạn vào {@code ngayDenHan} cho đúng người đó. */
    private void taoTheDenHan(UUID user, int soThe, LocalDate ngayDenHan) {
        UUID deckId = UUID.fromString(jdbc.queryForObject("""
                insert into flashcard_decks (id, owner_id, title)
                values (gen_random_uuid(), ?, 'Bộ thẻ test thông báo')
                returning id::text
                """, String.class, user));

        for (int i = 0; i < soThe; i++) {
            UUID cardId = UUID.fromString(jdbc.queryForObject("""
                    insert into flashcards (id, deck_id, front, back, source)
                    values (gen_random_uuid(), ?, ?, 'Mặt sau', 'MANUAL')
                    returning id::text
                    """, String.class, deckId, "Mặt trước " + i));

            jdbc.update("""
                    insert into flashcard_reviews (id, flashcard_id, user_id, ease_factor, interval_days,
                                                   repetitions, due_date)
                    values (gen_random_uuid(), ?, ?, 2.5, 1, 0, ?)
                    """, cardId, user, ngayDenHan);
        }
    }

    private UUID taoBaiLam(UUID user, int diem, int diemToiDa) {
        UUID quizId = UUID.fromString(jdbc.queryForObject("""
                insert into quizzes (id, owner_id, title, visibility, difficulty)
                values (gen_random_uuid(), ?, 'Quiz cho thông báo', 'PRIVATE', 'MEDIUM')
                returning id::text
                """, String.class, user));
        return UUID.fromString(jdbc.queryForObject("""
                insert into quiz_attempts (id, user_id, quiz_id, mode, status, started_at,
                                          total_score, max_score)
                values (gen_random_uuid(), ?, ?, 'PRACTICE', 'SUBMITTED', now(), ?, ?)
                returning id::text
                """, String.class, user, quizId, diem, diemToiDa));
    }

    /** Thông báo tạo thẳng bằng SQL: phần đang kiểm là đọc/phân quyền, không phải đường sinh ra nó. */
    private UUID taoThongBaoTay(UUID user, String title) {
        return UUID.fromString(jdbc.queryForObject("""
                insert into notifications (id, user_id, type, title, body)
                values (gen_random_uuid(), ?, 'SYSTEM', ?, 'Nội dung')
                returning id::text
                """, String.class, user, title));
    }

    private long demThongBao(UUID user, NotificationType type) {
        Long n = jdbc.queryForObject("select count(*) from notifications where user_id = ? and type = ?",
                Long.class, user, type.name());
        return n == null ? 0 : n;
    }

    private int capCua(UUID user) {
        Integer level = jdbc.queryForObject("select level from user_stats where user_id = ?",
                Integer.class, user);
        return level == null ? 1 : level;
    }

    /** Tiêu đề của thông báo thành tích có đúng khoá chống trùng này, null nếu chưa có. */
    private String tieuDeThanhTich(UUID user, String dedupeKey) {
        return jdbc.query("""
                        select title from notifications where user_id = ? and dedupe_key = ?
                        """,
                rs -> rs.next() ? rs.getString(1) : null, user, dedupeKey);
    }

    private String tieuDeDauTien(UUID user) {
        return jdbc.queryForObject("""
                select title from notifications where user_id = ? order by created_at limit 1
                """, String.class, user);
    }

    private boolean daDoc(UUID notificationId) {
        Boolean b = jdbc.queryForObject("select is_read from notifications where id = ?",
                Boolean.class, notificationId);
        return Boolean.TRUE.equals(b);
    }
}
