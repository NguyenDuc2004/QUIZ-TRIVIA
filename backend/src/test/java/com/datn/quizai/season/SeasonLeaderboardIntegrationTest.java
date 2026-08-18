package com.datn.quizai.season;

import com.datn.quizai.season.domain.Season;
import com.datn.quizai.season.domain.SeasonStatus;
import com.datn.quizai.season.repository.SeasonRepository;
import com.datn.quizai.season.service.SeasonClosingService;
import com.datn.quizai.season.service.SeasonLeaderboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bảng xếp hạng theo mùa (features/15).
 * <p>
 * Bốn nhóm phép kiểm, mỗi nhóm nhắm một tuyên bố cụ thể của lát cắt này:
 * <ol>
 *   <li><b>Redis chỉ là chỉ mục</b> — xoá sạch ZSET rồi đọc lại vẫn ra đúng bảng xếp hạng, vì nó được dựng
 *       lại từ {@code xp_events}. Đây là tuyên bố quan trọng nhất: nếu sai thì mất Redis là mất dữ liệu.</li>
 *   <li><b>Chốt mùa idempotent</b> — chạy hai lần không trao thưởng hai lần, không tạo hai mùa mới.</li>
 *   <li><b>Chưa có điểm khác hạng cuối</b> — trả 204, không trả một con số hạng sai.</li>
 *   <li><b>Không có đường ghi</b> — điểm chỉ đến từ XP.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeasonLeaderboardIntegrationTest {

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
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private SeasonLeaderboardService leaderboardService;
    @Autowired
    private SeasonClosingService closingService;

    private String token;

    @BeforeAll
    void setUp() throws Exception {
        token = register("mua-chinh@example.com");
    }

    // ==================================================== 1. Redis chỉ là chỉ mục

    @Test
    @DisplayName("Xoá sạch Redis rồi đọc lại: bảng xếp hạng vẫn đúng vì được dựng lại từ xp_events")
    void leaderboardSurvivesRedisLoss() {
        Season mua = leaderboardService.muaHienTai();
        UUID a = taoNguoiDungCoXp("mua-a@example.com", 500);
        UUID b = taoNguoiDungCoXp("mua-b@example.com", 300);

        // Xoá ZSET — mô phỏng Redis restart mà không bật AOF
        redisTemplate.delete(SeasonLeaderboardService.key(mua.getId()));
        assertThat(redisTemplate.opsForZSet().zCard(SeasonLeaderboardService.key(mua.getId())))
                .isIn(0L, null);

        var top = leaderboardService.top(10);

        // Nếu ZSET là nơi duy nhất giữ điểm thì đây là danh sách rỗng, và cả bảng xếp hạng mất vĩnh viễn
        assertThat(top).isNotEmpty();
        var cuaA = top.stream().filter(d -> d.userId().equals(a)).findFirst().orElseThrow();
        var cuaB = top.stream().filter(d -> d.userId().equals(b)).findFirst().orElseThrow();
        assertThat(cuaA.score()).isEqualTo(500);
        assertThat(cuaB.score()).isEqualTo(300);
        assertThat(cuaA.rank()).isLessThan(cuaB.rank());
    }

    @Test
    @DisplayName("XP kiếm NGOÀI khoảng thời gian mùa không được tính vào điểm mùa")
    void xpOutsideSeasonWindowDoesNotCount() {
        Season mua = leaderboardService.muaHienTai();
        UUID user = taoNguoiDung("mua-ngoai-khoang@example.com");

        // XP trước khi mùa bắt đầu
        themXp(user, 999, mua.getStartAt().minusDays(5));
        redisTemplate.delete(SeasonLeaderboardService.key(mua.getId()));

        var cuaToi = leaderboardService.thuHangCuaToi(user);

        // Điểm mùa là tổng XP TRONG khoảng thời gian mùa. Không lọc theo thời gian thì mùa mới nào cũng thừa
        // hưởng toàn bộ XP lịch sử, và bảng xếp hạng "theo mùa" chỉ là bảng XP toàn thời gian đổi tên.
        assertThat(cuaToi).as("chưa có điểm trong mùa thì không có thứ hạng").isNull();
    }

    // ==================================================== 2. Chốt mùa idempotent

    @Test
    @DisplayName("Chốt mùa hai lần: không trao thưởng hai lần, không tạo hai mùa mới")
    void closingSeasonIsIdempotent() {
        // Mùa riêng cho ca này để không đụng mùa của các ca khác
        Season mua = taoMuaRiengDaQuaHan();
        UUID quan = taoNguoiDungCoXpTrongMua("mua-quan-quan@example.com", 1000, mua);

        var lan1 = closingService.chotMua(mua.getId());
        assertThat(lan1.soNguoiLuu()).isPositive();
        assertThat(lan1.tenMuaMoi()).isNotNull();

        long soDongLan1 = demDongXepHang(mua.getId());
        long soHuyHieuLan1 = demHuyHieuMua(quan);
        long soMuaActiveLan1 = demMuaActive();

        // Chạy lại — mô phỏng job chạy hai lần, hoặc quản trị viên bấm chốt tay sau khi job đã chạy
        var lan2 = closingService.chotMua(mua.getId());

        assertThat(lan2.soNguoiLuu()).as("lần hai không lưu thêm ai").isZero();
        assertThat(demDongXepHang(mua.getId())).isEqualTo(soDongLan1);
        assertThat(demHuyHieuMua(quan)).as("không trao huy hiệu mùa hai lần").isEqualTo(soHuyHieuLan1);
        assertThat(demMuaActive()).as("không tạo hai mùa đang chạy").isEqualTo(soMuaActiveLan1);
    }

    @Test
    @DisplayName("Chốt mùa trao đúng huy hiệu theo hạng, và ghi hạng vào bảng lưu trữ")
    void closingAwardsBadgesByRank() {
        Season mua = taoMuaRiengDaQuaHan();
        UUID nhat = taoNguoiDungCoXpTrongMua("mua-hang1@example.com", 900, mua);
        UUID nhi = taoNguoiDungCoXpTrongMua("mua-hang2@example.com", 800, mua);

        closingService.chotMua(mua.getId());

        assertThat(hangCua(mua.getId(), nhat)).isEqualTo(1);
        assertThat(hangCua(mua.getId(), nhi)).isEqualTo(2);
        assertThat(coHuyHieu(nhat, "SEASON_TOP1")).isTrue();
        // Hạng 2 nhận huy hiệu top 3, không nhận huy hiệu quán quân
        assertThat(coHuyHieu(nhi, "SEASON_TOP1")).isFalse();
        assertThat(coHuyHieu(nhi, "SEASON_TOP3")).isTrue();

        // Mùa cũ phải chuyển ENDED và ZSET của nó bị xoá — giữ lại là một chỗ nữa để lệch
        assertThat(seasonRepository.findById(mua.getId()).orElseThrow().getStatus())
                .isEqualTo(SeasonStatus.ENDED);
        assertThat(redisTemplate.hasKey(SeasonLeaderboardService.key(mua.getId()))).isFalse();
    }

    @Test
    @DisplayName("Mùa mới bắt đầu đúng lúc mùa cũ kết thúc, không có khoảng trống")
    void newSeasonStartsWhereOldOneEnded() {
        Season mua = taoMuaRiengDaQuaHan();
        var ketThucCu = mua.getEndAt();

        closingService.chotMua(mua.getId());

        Season muaMoi = seasonRepository.findByStatus(SeasonStatus.ACTIVE).orElseThrow();
        // Lấy now() làm mốc bắt đầu thì XP kiếm trong khoảng job chạy muộn không thuộc mùa nào
        assertThat(muaMoi.getStartAt()).isEqualTo(ketThucCu);
        assertThat(muaMoi.getEndAt()).isAfter(muaMoi.getStartAt());
    }

    // ==================================================== 3. Chưa có điểm khác hạng cuối

    @Test
    @DisplayName("Người chưa có điểm: /current/me trả 204, KHÔNG trả hạng cuối")
    void noScoreReturnsNoContent() throws Exception {
        String tokenMoi = register("mua-chua-co-diem@example.com");

        mockMvc.perform(get("/api/v1/leaderboard/season/current/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMoi))
                .andExpect(status().isNoContent());

        // Và trong bảng đầy đủ thì thuHangCuaToi phải là null, không phải một hạng bịa
        String body = mockMvc.perform(get("/api/v1/leaderboard/season/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMoi))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(body).get("thuHangCuaToi").isNull()).isTrue();
    }

    @Test
    @DisplayName("Bảng xếp hạng trả kèm tên mùa, mốc thời gian và tổng số người tham gia")
    void leaderboardCarriesSeasonContext() throws Exception {
        taoNguoiDungCoXp("mua-co-diem@example.com", 120);

        mockMvc.perform(get("/api/v1/leaderboard/season/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenMua").exists())
                .andExpect(jsonPath("$.batDau").exists())
                .andExpect(jsonPath("$.ketThuc").exists())
                // Thứ hạng cần mẫu số để có nghĩa: "hạng 7" khác hẳn "hạng 7 / 8"
                .andExpect(jsonPath("$.soNguoiThamGia").isNumber())
                .andExpect(jsonPath("$.top").isArray());
    }

    // ==================================================== 4. Không có đường ghi

    @Test
    @DisplayName("Không có endpoint nào cộng điểm mùa")
    void thereIsNoWriteEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/leaderboard/season/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":999999}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Khách chưa đăng nhập nhận 401")
    void guestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboard/season/current")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/leaderboard/season/history")).andExpect(status().isUnauthorized());
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

    private UUID taoNguoiDung(String email) {
        return UUID.fromString(jdbc.queryForObject("""
                insert into users (id, email, display_name, role, password_hash, created_at, updated_at)
                values (gen_random_uuid(), ?, 'Người học', 'LEARNER', 'x', now(), now())
                returning id::text
                """, String.class, email));
    }

    private UUID taoNguoiDungCoXp(String email, int xp) {
        UUID user = taoNguoiDung(email);
        themXp(user, xp, java.time.OffsetDateTime.now());
        return user;
    }

    private UUID taoNguoiDungCoXpTrongMua(String email, int xp, Season mua) {
        UUID user = taoNguoiDung(email);
        themXp(user, xp, mua.getStartAt().plusHours(1));
        return user;
    }

    /** Ghi thẳng vào xp_events — nguồn sự thật của điểm mùa. */
    private void themXp(UUID user, int xp, java.time.OffsetDateTime luc) {
        jdbc.update("""
                insert into xp_events (id, user_id, source_type, source_key, xp, created_at)
                values (gen_random_uuid(), ?, 'ATTEMPT_SUBMITTED', ?, ?, ?)
                """, user, UUID.randomUUID().toString(), xp, luc);
    }

    /**
     * Tạo một mùa riêng đã quá hạn để chốt.
     * <p>
     * Phải đóng mùa đang chạy trước: chỉ mục {@code uk_seasons_one_active} chặn hai mùa ACTIVE cùng lúc — và
     * đó chính là điều nó phải chặn.
     */
    private static int lanTaoMua = 0;

    private Season taoMuaRiengDaQuaHan() {
        jdbc.update("update seasons set status = 'ENDED' where status = 'ACTIVE'");

        // Mỗi ca một khoảng thời gian KHÔNG GIAO NHAU. Dùng chung khoảng thì XP của ca trước lọt vào mùa của
        // ca sau và thứ hạng mong đợi không còn xác định — đúng lỗi đã gặp: mong hạng 1 nhưng ra hạng 3.
        int lui = ++lanTaoMua * 100;
        UUID id = UUID.fromString(jdbc.queryForObject("""
                insert into seasons (id, name, start_at, end_at, status)
                values (gen_random_uuid(), 'Mùa kiểm thử ' || substr(gen_random_uuid()::text, 1, 6),
                        now() - make_interval(days => ?), now() - make_interval(days => ?), 'ACTIVE')
                returning id::text
                """, String.class, lui + 30, lui));
        return seasonRepository.findById(id).orElseThrow();
    }

    private long demDongXepHang(UUID seasonId) {
        Long n = jdbc.queryForObject("select count(*) from season_rankings where season_id = ?",
                Long.class, seasonId);
        return n == null ? 0 : n;
    }

    private long demHuyHieuMua(UUID user) {
        Long n = jdbc.queryForObject("""
                select count(*) from user_badges ub join badges b on b.id = ub.badge_id
                where ub.user_id = ? and b.code like 'SEASON_%'
                """, Long.class, user);
        return n == null ? 0 : n;
    }

    private long demMuaActive() {
        Long n = jdbc.queryForObject("select count(*) from seasons where status = 'ACTIVE'", Long.class);
        return n == null ? 0 : n;
    }

    private int hangCua(UUID seasonId, UUID user) {
        Integer hang = jdbc.queryForObject("""
                select final_rank from season_rankings where season_id = ? and user_id = ?
                """, Integer.class, seasonId, user);
        return hang == null ? -1 : hang;
    }

    private boolean coHuyHieu(UUID user, String code) {
        Long n = jdbc.queryForObject("""
                select count(*) from user_badges ub join badges b on b.id = ub.badge_id
                where ub.user_id = ? and b.code = ?
                """, Long.class, user, code);
        return n != null && n > 0;
    }
}
