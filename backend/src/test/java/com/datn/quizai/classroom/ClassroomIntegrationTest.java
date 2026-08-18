package com.datn.quizai.classroom;

import com.datn.quizai.classroom.service.AssignmentDueReminderJob;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lớp học & giao bài (features/14).
 * <p>
 * Trạng thái bài tập đã có {@code TrangThaiBaiTapTest} kiểm riêng bằng unit test, nên ở đây <b>không kiểm
 * lại bảng chân trị</b>. Lớp này kiểm bốn nhóm chỉ hỏng khi có cơ sở dữ liệu và phân quyền thật:
 * <ol>
 *   <li><b>Ranh giới lớp</b> — người ngoài lớp nhận 404, học sinh không thấy danh sách thành viên, trợ giảng
 *       không xoá được lớp.</li>
 *   <li><b>Quiz PRIVATE của giáo viên làm được khi được giao</b> — đây là lý do có endpoint bắt đầu riêng,
 *       và nếu nó hỏng thì cả tính năng vô dụng với quiz thật (giáo viên hiếm khi để PUBLIC).</li>
 *   <li><b>Mỗi học sinh một lượt cho mỗi bài tập</b> — chốt là chỉ mục duy nhất của V19.</li>
 *   <li><b>Bảng theo dõi có dòng cho người CHƯA làm</b> — đó chính là câu hỏi giáo viên cần trả lời.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClassroomIntegrationTest {

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
    private AssignmentDueReminderJob dueReminderJob;

    private String tokenGiaoVien;
    private String tokenHocSinh;
    private String tokenNguoiNgoai;
    private UUID idHocSinh;

    @BeforeAll
    void setUp() throws Exception {
        tokenGiaoVien = register("lop-giaovien@example.com", "CREATOR");
        tokenHocSinh = register("lop-hocsinh@example.com", "LEARNER");
        tokenNguoiNgoai = register("lop-nguoingoai@example.com", "LEARNER");
        idHocSinh = idCuaEmail("lop-hocsinh@example.com");
    }

    // ======================================================== 1. Ranh giới lớp

    @Test
    @DisplayName("Tạo lớp sinh mã 6 ký tự; học sinh vào bằng mã đó")
    void shouldCreateAndJoin() throws Exception {
        JsonNode lop = taoLop("Toán 12A1");

        assertThat(lop.get("classCode").asText()).matches("^[A-Z2-9]{6}$");
        assertThat(lop.get("vaiTroCuaToi").asText()).isEqualTo("OWNER");

        JsonNode daVao = json(post("/api/v1/classrooms/join/{code}", lop.get("classCode").asText())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh));

        assertThat(daVao.get("id").asText()).isEqualTo(lop.get("id").asText());
        assertThat(daVao.get("vaiTroCuaToi").asText()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("Mã lớp CHỈ trả cho giáo viên — học sinh không cầm mã để mời người khác")
    void shouldHideClassCodeFromStudents() throws Exception {
        JsonNode lop = taoLop("Lớp giấu mã");
        thamGia(lop, tokenHocSinh);

        JsonNode nhinTuHocSinh = json(get("/api/v1/classrooms/{id}", lop.get("id").asText())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh));

        assertThat(nhinTuHocSinh.get("classCode").isNull()).isTrue();
        // Đối chứng: chính lớp đó, nhìn từ giáo viên thì CÓ mã — nếu thiếu, phép kiểm trên vẫn xanh cả khi
        // mã bị giấu với tất cả mọi người
        JsonNode nhinTuGiaoVien = json(get("/api/v1/classrooms/{id}", lop.get("id").asText())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVien));
        assertThat(nhinTuGiaoVien.get("classCode").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Người ngoài lớp nhận 404 chứ không phải 403 — không tiết lộ lớp đó có thật")
    void shouldHideClassroomFromOutsiders() throws Exception {
        JsonNode lop = taoLop("Lớp riêng");

        mockMvc.perform(get("/api/v1/classrooms/{id}", lop.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenNguoiNgoai))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Học sinh KHÔNG xem được danh sách thành viên")
    void shouldRestrictMemberList() throws Exception {
        JsonNode lop = taoLop("Lớp có danh sách");
        thamGia(lop, tokenHocSinh);

        mockMvc.perform(get("/api/v1/classrooms/{id}/members", lop.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh))
                .andExpect(status().isNotFound());

        // Đối chứng: giáo viên xem được, và thấy đúng một học sinh
        JsonNode ds = json(get("/api/v1/classrooms/{id}/members", lop.get("id").asText())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVien));
        assertThat(ds).hasSize(1);
    }

    @Test
    @DisplayName("Trợ giảng giao bài được nhưng KHÔNG xoá được lớp")
    void shouldLimitCoTeacher() throws Exception {
        JsonNode lop = taoLop("Lớp có trợ giảng");
        String lopId = lop.get("id").asText();
        thamGia(lop, tokenHocSinh);

        mockMvc.perform(put("/api/v1/classrooms/{id}/members/{u}/role", lopId, idHocSinh)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CO_TEACHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CO_TEACHER"));

        // Trợ giảng xem được thành viên
        mockMvc.perform(get("/api/v1/classrooms/{id}/members", lopId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh))
                .andExpect(status().isOk());

        // ...nhưng xoá lớp thì không. Xoá lớp không hoàn tác được nên nó là việc riêng của chủ nhiệm.
        mockMvc.perform(delete("/api/v1/classrooms/{id}", lopId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Vào lại lớp đã ở trong thì KHÔNG lỗi — bấm hai lần là chuyện thường")
    void shouldBeIdempotentOnJoin() throws Exception {
        JsonNode lop = taoLop("Lớp vào hai lần");
        thamGia(lop, tokenHocSinh);
        thamGia(lop, tokenHocSinh);

        Long soDong = jdbc.queryForObject("""
                select count(*) from classroom_members where classroom_id = ?::uuid and user_id = ?
                """, Long.class, lop.get("id").asText(), idHocSinh);
        assertThat(soDong).isEqualTo(1);
    }

    // ======================================================== 2. Quiz PRIVATE làm được khi được giao

    @Test
    @DisplayName("Quiz PRIVATE của giáo viên: học sinh KHÔNG tự vào làm được, nhưng LÀM ĐƯỢC khi được giao")
    void shouldAllowPrivateQuizThroughAssignment() throws Exception {
        // Đây là lý do tồn tại của endpoint bắt đầu riêng. Giáo viên hiếm khi để quiz PUBLIC, nên nếu chỗ
        // này hỏng thì cả tính năng vô dụng với quiz thật.
        String quizId = taoQuizRiengCoCau();
        JsonNode lop = taoLop("Lớp làm quiz riêng");
        thamGia(lop, tokenHocSinh);

        // Tự vào làm: 404, vì quiz PRIVATE của người khác
        mockMvc.perform(post("/api/v1/quizzes/{id}/attempts", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXAM\"}"))
                .andExpect(status().isNotFound());

        String baiTapId = giaoBai(lop, quizId, null).get("id").asText();

        JsonNode batDau = objectMapper.readTree(mockMvc.perform(
                        post("/api/v1/assignments/{id}/attempts", baiTapId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        assertThat(batDau.get("attemptId").asText()).isNotBlank();
    }

    @Test
    @DisplayName("Lượt bài tập luôn là EXAM — nên features/12 thu tín hiệu hành vi như mọi lượt thi")
    void shouldAlwaysUseExamMode() throws Exception {
        String quizId = taoQuizRiengCoCau();
        JsonNode lop = taoLop("Lớp kiểm chế độ");
        thamGia(lop, tokenHocSinh);
        String attemptId = batDauBaiTap(giaoBai(lop, quizId, null).get("id").asText(), tokenHocSinh);

        String mode = jdbc.queryForObject("select mode from quiz_attempts where id = ?::uuid",
                String.class, attemptId);
        assertThat(mode).isEqualTo("EXAM");
    }

    @Test
    @DisplayName("Người ngoài lớp KHÔNG bắt đầu được bài tập của lớp đó")
    void shouldBlockOutsiderFromAssignment() throws Exception {
        String quizId = taoQuizRiengCoCau();
        JsonNode lop = taoLop("Lớp chặn người ngoài");
        String baiTapId = giaoBai(lop, quizId, null).get("id").asText();

        mockMvc.perform(post("/api/v1/assignments/{id}/attempts", baiTapId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenNguoiNgoai))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Bài hẹn giờ mở: chưa tới giờ thì không làm được VÀ không hiện trong danh sách của tôi")
    void shouldRespectOpenAt() throws Exception {
        String quizId = taoQuizRiengCoCau();
        JsonNode lop = taoLop("Lớp hẹn giờ");
        thamGia(lop, tokenHocSinh);

        String baiTapId = giaoBaiVoiThoiGian(lop, quizId,
                OffsetDateTime.now().plusDays(7), null).get("id").asText();

        mockMvc.perform(post("/api/v1/assignments/{id}/attempts", baiTapId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh))
                .andExpect(status().isBadRequest());

        // Không hiện ra: bài mở tuần sau mà hiện hôm nay thì học sinh bấm vào và nhận lỗi, hoặc tưởng mình
        // đã bỏ lỡ
        JsonNode cuaToi = json(get("/api/v1/me/assignments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh));
        for (JsonNode bai : cuaToi) {
            assertThat(bai.get("id").asText()).isNotEqualTo(baiTapId);
        }
    }

    // ======================================================== 3. Mỗi học sinh một lượt

    @Test
    @DisplayName("Đã nộp thì KHÔNG làm lại được — không thì điểm bài tập vô nghĩa")
    void shouldAllowOnlyOneAttemptPerAssignment() throws Exception {
        String quizId = taoQuizRiengCoCau();
        JsonNode lop = taoLop("Lớp một lượt");
        thamGia(lop, tokenHocSinh);
        String baiTapId = giaoBai(lop, quizId, null).get("id").asText();

        String attemptId = batDauBaiTap(baiTapId, tokenHocSinh);

        // Chưa nộp: gọi lại trả về ĐÚNG lượt đang dở, không tạo lượt thứ hai
        assertThat(batDauBaiTap(baiTapId, tokenHocSinh)).isEqualTo(attemptId);

        mockMvc.perform(post("/api/v1/attempts/{id}/submit", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/assignments/{id}/attempts", baiTapId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh))
                .andExpect(status().isBadRequest());
    }

    // ======================================================== 4. Bảng theo dõi lớp

    @Test
    @DisplayName("Bảng theo dõi có dòng cho người CHƯA làm — đó chính là câu giáo viên cần trả lời")
    void shouldListMembersWhoHaveNotStarted() throws Exception {
        String quizId = taoQuizRiengCoCau();
        JsonNode lop = taoLop("Lớp theo dõi");
        thamGia(lop, tokenHocSinh);
        thamGia(lop, tokenNguoiNgoai);
        String baiTapId = giaoBai(lop, quizId, null).get("id").asText();

        // Chỉ MỘT trong hai em làm
        String attemptId = batDauBaiTap(baiTapId, tokenHocSinh);
        mockMvc.perform(post("/api/v1/attempts/{id}/submit", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh))
                .andExpect(status().isOk());

        JsonNode kq = json(get("/api/v1/assignments/{id}/results", baiTapId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVien));

        assertThat(kq.get("soThanhVien").asInt()).isEqualTo(2);
        assertThat(kq.get("soDaNop").asInt()).isEqualTo(1);
        assertThat(kq.get("danhSach")).hasSize(2);

        boolean coDongChuaLam = false;
        for (JsonNode dong : kq.get("danhSach")) {
            if ("CHUA_LAM".equals(dong.get("trangThai").asText())) {
                coDongChuaLam = true;
                // Chưa nộp thì điểm phải là null, KHÔNG phải 0 — trộn hai thứ đó làm hỏng cả điểm trung
                // bình lẫn cách đọc bảng
                assertThat(dong.get("diem").isNull()).isTrue();
            }
        }
        assertThat(coDongChuaLam).as("phải có dòng cho người chưa làm").isTrue();
    }

    @Test
    @DisplayName("Học sinh KHÔNG xem được bảng theo dõi của cả lớp")
    void shouldRestrictResults() throws Exception {
        String quizId = taoQuizRiengCoCau();
        JsonNode lop = taoLop("Lớp giấu bảng");
        thamGia(lop, tokenHocSinh);
        String baiTapId = giaoBai(lop, quizId, null).get("id").asText();

        mockMvc.perform(get("/api/v1/assignments/{id}/results", baiTapId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Không giao được quiz của NGƯỜI KHÁC")
    void shouldNotAssignSomeoneElseQuiz() throws Exception {
        // Cho giao quiz người khác thì một giáo viên phát tán được quiz PRIVATE của đồng nghiệp cho cả lớp
        // mình, và chủ quiz không có cách nào biết
        String tokenGiaoVienKhac = register("lop-giaovien-khac@example.com", "CREATOR");
        String quizId = taoQuizRiengCoCau();
        JsonNode lopCuaHo = objectMapper.readTree(mockMvc.perform(post("/api/v1/classrooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVienKhac)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lớp của người khác\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/classrooms/{id}/assignments", lopCuaHo.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVienKhac)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":\"%s\",\"title\":\"Bài tập\"}".formatted(quizId)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Không giao được quiz RỖNG — chặn ở lúc giao, không để học sinh phát hiện")
    void shouldNotAssignEmptyQuiz() throws Exception {
        String quizRong = taoQuiz("Quiz chưa có câu nào");
        JsonNode lop = taoLop("Lớp quiz rỗng");

        mockMvc.perform(post("/api/v1/classrooms/{id}/assignments", lop.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":\"%s\",\"title\":\"Bài tập rỗng\"}".formatted(quizRong)))
                .andExpect(status().isBadRequest());
    }

    // ======================================================== 5. Nhắc hạn nộp (features/16)

    @Test
    @DisplayName("Nhắc hạn nộp chỉ gửi cho người CHƯA nộp, và chạy hai lần không gửi trùng")
    void shouldRemindOnlyThoseWhoHaveNotSubmitted() throws Exception {
        String quizId = taoQuizRiengCoCau();
        JsonNode lop = taoLop("Lớp nhắc hạn");
        thamGia(lop, tokenHocSinh);
        thamGia(lop, tokenNguoiNgoai);

        String baiTapId = giaoBaiVoiThoiGian(lop, quizId, null,
                OffsetDateTime.now().plusHours(5)).get("id").asText();

        // Một em nộp trước
        String attemptId = batDauBaiTap(baiTapId, tokenHocSinh);
        mockMvc.perform(post("/api/v1/attempts/{id}/submit", attemptId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenHocSinh))
                .andExpect(status().isOk());

        assertThat(dueReminderJob.nhacHanNop(OffsetDateTime.now())).isPositive();

        UUID idNguoiNgoai = idCuaEmail("lop-nguoingoai@example.com");
        assertThat(demThongBaoHanNop(idNguoiNgoai)).as("người chưa nộp phải được nhắc").isEqualTo(1);
        assertThat(demThongBaoHanNop(idHocSinh))
                .as("người đã nộp mà vẫn nhận 'sắp hết hạn' là một thông báo sai sự thật")
                .isZero();

        // Chạy lại: khoá `assignment:{id}` chặn, không nhắc lần hai cho cùng một bài
        dueReminderJob.nhacHanNop(OffsetDateTime.now());
        assertThat(demThongBaoHanNop(idNguoiNgoai)).isEqualTo(1);
    }

    // ================================================================ helper

    private JsonNode taoLop(String ten) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/classrooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(ten)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private void thamGia(JsonNode lop, String token) throws Exception {
        mockMvc.perform(post("/api/v1/classrooms/join/{code}", lop.get("classCode").asText())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    private JsonNode giaoBai(JsonNode lop, String quizId, OffsetDateTime dueAt) throws Exception {
        return giaoBaiVoiThoiGian(lop, quizId, null, dueAt);
    }

    private JsonNode giaoBaiVoiThoiGian(JsonNode lop, String quizId,
                                        OffsetDateTime openAt, OffsetDateTime dueAt) throws Exception {
        StringBuilder body = new StringBuilder(
                "{\"quizId\":\"%s\",\"title\":\"Bài tập về nhà\"".formatted(quizId));
        if (openAt != null) {
            body.append(",\"openAt\":\"").append(openAt).append('"');
        }
        if (dueAt != null) {
            body.append(",\"dueAt\":\"").append(dueAt).append('"');
        }
        body.append('}');

        return objectMapper.readTree(mockMvc.perform(
                        post("/api/v1/classrooms/{id}/assignments", lop.get("id").asText())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVien)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private String batDauBaiTap(String baiTapId, String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(
                        post("/api/v1/assignments/{id}/attempts", baiTapId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString())
                .get("attemptId").asText();
    }

    /** Quiz PRIVATE (mặc định) có một câu — đúng tình huống thật: giáo viên không công khai đề. */
    private String taoQuizRiengCoCau() throws Exception {
        String quizId = taoQuiz("Đề kiểm tra riêng");

        String cauId = objectMapper.readTree(mockMvc.perform(post("/api/v1/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"1 + 1 = ?","type":"SINGLE_CHOICE","points":1,
                                 "options":[{"content":"2","correct":true},
                                            {"content":"3","correct":false}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/quizzes/{id}/questions", quizId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionIds\":[\"%s\"]}".formatted(cauId)))
                .andExpect(status().isOk());

        return quizId;
    }

    private String taoQuiz(String tieuDe) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenGiaoVien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\",\"difficulty\":\"EASY\"}".formatted(tieuDe)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
    }

    private long demThongBaoHanNop(UUID userId) {
        Long n = jdbc.queryForObject("""
                select count(*) from notifications where user_id = ? and type = 'ASSIGNMENT_DUE'
                """, Long.class, userId);
        return n == null ? 0 : n;
    }

    private JsonNode json(RequestBuilder request) throws Exception {
        return objectMapper.readTree(mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private String register(String email, String role) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người dùng","role":"%s"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }

    private UUID idCuaEmail(String email) {
        return UUID.fromString(jdbc.queryForObject("select id::text from users where email = ?",
                String.class, email));
    }
}
