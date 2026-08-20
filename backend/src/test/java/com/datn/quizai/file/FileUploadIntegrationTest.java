package com.datn.quizai.file;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test tải ảnh lên và gắn làm ảnh bìa quiz.
 * <p>
 * Ghi vào một thư mục tạm của hệ điều hành chứ không ghi vào {@code uploads/} của dự án,
 * để chạy test không để lại rác trong mã nguồn.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileUploadIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    /**
     * Tự tạo thư mục tạm thay vì dùng {@code @TempDir}: extension của {@code @TempDir} chạy sau
     * khi Spring đã dựng context, nên lúc {@code @DynamicPropertySource} đọc giá trị thì trường
     * tĩnh vẫn còn null.
     */
    static final Path tempUploadDir = createTempUploadDir();

    private static Path createTempUploadDir() {
        try {
            return Files.createTempDirectory("quizai-uploads-test");
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được thư mục tạm cho test", e);
        }
    }

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.upload-dir", tempUploadDir::toString);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String creatorToken;
    private String learnerToken;

    @BeforeAll
    void registerUsers() throws Exception {
        creatorToken = register("creator-file@example.com", "CREATOR");
        learnerToken = register("learner-file@example.com", "LEARNER");
    }

    /** Một file PNG hợp lệ tối thiểu: đúng 8 byte chữ ký rồi phần thân giả. */
    private static MockMultipartFile pngFile(String originalName) {
        byte[] content = new byte[64];
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, content, 0, signature.length);
        return new MockMultipartFile("file", originalName, MediaType.IMAGE_PNG_VALUE, content);
    }

    @Test
    @DisplayName("Creator tải ảnh lên → file nằm trên đĩa và đọc lại được qua /uploads")
    void shouldUploadAndServeImage() throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/files/images")
                        .file(pngFile("anh-bia.png"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andReturn().getResponse().getContentAsString();

        JsonNode uploaded = objectMapper.readTree(body);
        String url = uploaded.get("url").asText();

        assertThat(url).startsWith("/uploads/images/").endsWith(".png");
        // Tên file do server sinh, KHÔNG lấy tên client gửi lên
        assertThat(url).doesNotContain("anh-bia");
        assertThat(Files.exists(tempUploadDir.resolve("images").resolve(uploaded.get("fileName").asText())))
                .isTrue();

        // Ảnh phải xem được khi chưa đăng nhập (card quiz công khai hiện cho Guest)
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("File không phải ảnh (đặt tên .png, khai image/png) vẫn bị từ chối 400")
    void shouldRejectDisguisedFile() throws Exception {
        MockMultipartFile fake = new MockMultipartFile(
                "file", "virus.png", MediaType.IMAGE_PNG_VALUE, "<?php echo 1; ?>".getBytes());

        mockMvc.perform(multipart("/api/v1/files/images")
                        .file(fake)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ nhận ảnh JPG, PNG, GIF hoặc WebP"));
    }

    @Test
    @DisplayName("Learner không tải được ảnh QUIZ (403); Guest bị chặn (401)")
    void shouldRestrictUploadByRole() throws Exception {
        // Chỉ đúng cho /files/images — ảnh quiz không giới hạn số lượng nên phải khoá theo vai trò.
        // Ảnh ĐẠI DIỆN đi đường khác và learner phải tải được: xem các test ở cuối lớp.
        mockMvc.perform(multipart("/api/v1/files/images")
                        .file(pngFile("anh.png"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/api/v1/files/images").file(pngFile("anh.png")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Gắn ảnh vừa tải lên làm ảnh bìa quiz, Guest xem danh sách thấy được")
    void shouldAttachThumbnailToQuiz() throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/files/images")
                        .file(pngFile("bia.png"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String url = objectMapper.readTree(body).get("url").asText();

        mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Quiz có ảnh bìa","visibility":"PUBLIC","thumbnailUrl":"%s"}
                                """.formatted(url)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thumbnailUrl").value(url));

        mockMvc.perform(get("/api/v1/quizzes").param("q", "Quiz có ảnh bìa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].thumbnailUrl").value(url));
    }

    @Test
    @DisplayName("Không nhận URL ngoài hay đường dẫn thoát thư mục làm ảnh bìa")
    void shouldRejectForeignThumbnailUrl() throws Exception {
        for (String url : new String[]{
                "https://example.com/anh.jpg",
                "/uploads/../../etc/passwd",
                "javascript:alert(1)"}) {

            mockMvc.perform(post("/api/v1/quizzes")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"Quiz ảnh ngoài","thumbnailUrl":"%s"}
                                    """.formatted(url)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Ảnh bìa phải là ảnh đã tải lên hệ thống"));
        }
    }

    @Test
    @DisplayName("Bỏ trống ảnh bìa vẫn tạo được quiz, thumbnailUrl là null")
    void shouldAllowQuizWithoutThumbnail() throws Exception {
        mockMvc.perform(post("/api/v1/quizzes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Quiz không ảnh\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.thumbnailUrl").doesNotExist());
    }

    // ======================================================================= ảnh đại diện

    @Test
    @DisplayName("Learner tải được ảnh ĐẠI DIỆN — đây là nhu cầu có thật của người học")
    void learnerCanUploadAvatar() throws Exception {
        // Trước đây trang hồ sơ gọi /files/images, nên người học bấm "Chọn ảnh từ máy" trên chính hồ sơ
        // của mình lại nhận 403 "Bạn không có quyền thực hiện hành động này".
        String body = mockMvc.perform(multipart("/api/v1/files/avatar")
                        .file(pngFile("toi.png"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String url = objectMapper.readTree(body).get("url").asText();
        assertThat(url).startsWith("/uploads/avatars/").endsWith(".png");
        // Tên file do server sinh, không lấy tên client gửi
        assertThat(url).doesNotContain("toi");

        // Ảnh đại diện hiện ở bảng xếp hạng và phòng đấu, nên phải xem được cả khi chưa đăng nhập
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Tên file ảnh đại diện gắn với id người tải, lấy từ token chứ không từ tham số")
    void avatarFileNameIsTiedToTheUploader() throws Exception {
        String id = idCuaToi(learnerToken);

        String body = mockMvc.perform(multipart("/api/v1/files/avatar")
                        .file(pngFile("a.png"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(body).get("fileName").asText())
                .as("id người dùng là phần đầu tên file — nếu nhận id qua tham số thì đây là "
                        + "đường ghi đè ảnh người khác")
                .startsWith(id + "-");
    }

    @Test
    @DisplayName("Tải lại ảnh đại diện: mỗi người vẫn chỉ tốn ĐÚNG MỘT file, và URL đổi")
    void avatarKeepsExactlyOneFilePerUser() throws Exception {
        String creatorId = idCuaToi(creatorToken);

        String url1 = taiAnhDaiDien(creatorToken);
        String url2 = taiAnhDaiDien(creatorToken);
        String url3 = taiAnhDaiDien(creatorToken);

        // Ràng buộc "mỗi người một file" chính là thứ cho phép mở endpoint này cho MỌI tài khoản:
        // không có nó thì bất kỳ ai đăng ký được cũng biến hệ thống thành chỗ chứa file miễn phí.
        assertThat(cacAnhCua(creatorId))
                .as("ba lần tải lên nhưng chỉ còn lại một file")
                .hasSize(1);

        // URL phải đổi mỗi lần, nếu không thì trình duyệt và các màn hình khác vẫn hiện ảnh cũ từ cache
        assertThat(List.of(url1, url2, url3)).doesNotHaveDuplicates();
        assertThat(cacAnhCua(creatorId).get(0)).endsWith(url3.substring(url3.lastIndexOf('/') + 1));
    }

    @Test
    @DisplayName("Dọn ảnh cũ chỉ đụng vào ảnh của CHÍNH người tải, không đụng ảnh người khác")
    void cleanupNeverTouchesOtherUsersAvatars() throws Exception {
        String learnerId = idCuaToi(learnerToken);
        taiAnhDaiDien(learnerToken);

        // Người khác tải ảnh lên nhiều lần
        taiAnhDaiDien(creatorToken);
        taiAnhDaiDien(creatorToken);

        assertThat(cacAnhCua(learnerId)).hasSize(1);
    }

    @Test
    @DisplayName("Guest không tải được ảnh đại diện (401) — không có id thì không biết ghi cho ai")
    void guestCannotUploadAvatar() throws Exception {
        mockMvc.perform(multipart("/api/v1/files/avatar").file(pngFile("a.png")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("File giả dạng ảnh vẫn bị chặn ở đường ảnh đại diện — dùng chung phép kiểm")
    void avatarRejectsDisguisedFile() throws Exception {
        MockMultipartFile fake = new MockMultipartFile(
                "file", "avatar.png", MediaType.IMAGE_PNG_VALUE, "<?php echo 1; ?>".getBytes());

        mockMvc.perform(multipart("/api/v1/files/avatar")
                        .file(fake)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ nhận ảnh JPG, PNG, GIF hoặc WebP"));
    }

    @Test
    @DisplayName("Learner gắn được ảnh vừa tải làm ảnh đại diện của mình")
    void learnerCanSetUploadedAvatarOnProfile() throws Exception {
        String url = taiAnhDaiDien(learnerToken);

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + learnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Người học có ảnh","avatarUrl":"%s"}
                                """.formatted(url)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(url));
    }

    /** Tải một ảnh đại diện, trả về URL công khai. */
    private String taiAnhDaiDien(String token) throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/files/avatar")
                        .file(pngFile("anh.png"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("url").asText();
    }

    /** Các file ảnh đại diện còn lại trên đĩa của một người dùng. */
    private List<String> cacAnhCua(String userId) throws IOException {
        Path folder = tempUploadDir.resolve("avatars");
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(f -> f.getFileName().toString())
                    .filter(name -> name.startsWith(userId + "-"))
                    .toList();
        }
    }

    private String idCuaToi(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String register(String email, String role) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"MatKhau@123","displayName":"Người dùng test","role":"%s"}
                                """.formatted(email, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
