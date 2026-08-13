package com.datn.quizai.ai.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test kho vector học liệu trên PostgreSQL + pgvector <b>thật</b>.
 * <p>
 * Lớp này sinh ra từ một lỗi <b>im lặng</b>: nhánh {@code materialId = null} — nghĩa là "tìm trong mọi
 * tài liệu đọc được" — trả về rỗng, không lỗi, không cảnh báo. Nguyên nhân là câu SQL cũ viết
 * {@code (cast(? as uuid) is null or m.id = cast(? as uuid))} rồi truyền {@code null} vào.
 * <p>
 * Hậu quả trải khắp cả hai tính năng RAG, mà không tính năng nào báo hỏng:
 * <ul>
 *   <li>Trợ lý học tập (features/08) trả lời "tài liệu không đề cập" dù kho đầy tài liệu đúng chủ đề.</li>
 *   <li>Sinh đề (features/05) với {@code useMaterials = true} nhưng không chọn tài liệu cụ thể thì
 *       không có ngữ cảnh nào, nên mô hình sinh câu hỏi từ kiến thức nền của nó — tức là bịa, đúng
 *       thứ RAG sinh ra để chống.</li>
 * </ul>
 * <p>
 * Test đặt ở <b>tầng repository</b> chứ không qua API: dữ liệu do chính nó chèn nên kết quả không phụ
 * thuộc học liệu mà ca test khác chia sẻ. Học liệu {@code shared} là dùng chung toàn hệ thống, nên bất
 * kỳ phép kiểm nào về truy xuất mà đi qua API đều dễ đổi kết quả theo thứ tự chạy.
 */
@SpringBootTest
@Testcontainers
class MaterialChunkRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private MaterialChunkRepository repository;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID ownerId;
    private UUID otherOwnerId;

    @BeforeEach
    void freshStore() {
        // Dọn sạch trước mỗi ca: kho vector là dữ liệu dùng chung, để lại thì ca sau đếm sai
        jdbc.update("delete from material_chunks");
        jdbc.update("delete from learning_materials");

        ownerId = insertUser("chu-kho-vector-" + UUID.randomUUID() + "@example.com");
        otherOwnerId = insertUser("nguoi-khac-" + UUID.randomUUID() + "@example.com");
    }

    @Test
    @DisplayName("materialId = null: tìm trong MỌI tài liệu của mình — nhánh này từng lặng lẽ trả rỗng")
    void shouldSearchAllOwnMaterialsWhenMaterialIdIsNull() {
        UUID first = insertMaterial(ownerId, "Tài liệu một", true);
        UUID second = insertMaterial(ownerId, "Tài liệu hai", true);
        insertChunk(first, 0, "Nội dung tài liệu một");
        insertChunk(second, 0, "Nội dung tài liệu hai");

        List<MaterialChunkRepository.Chunk> found =
                repository.searchSimilar(ownerId, null, queryVector(), 5);

        assertThat(found).as("để null nghĩa là tìm trong mọi tài liệu, không phải không tìm gì")
                .hasSize(2);
        assertThat(found).extracting(MaterialChunkRepository.Chunk::materialTitle)
                .containsExactlyInAnyOrder("Tài liệu một", "Tài liệu hai");
    }

    @Test
    @DisplayName("materialId cụ thể: chỉ trả đoạn của đúng tài liệu đó")
    void shouldRestrictToOneMaterial() {
        UUID target = insertMaterial(ownerId, "Tài liệu cần tìm", true);
        UUID other = insertMaterial(ownerId, "Tài liệu không liên quan", true);
        insertChunk(target, 0, "Đoạn của tài liệu cần tìm");
        insertChunk(other, 0, "Đoạn của tài liệu khác");

        List<MaterialChunkRepository.Chunk> found =
                repository.searchSimilar(ownerId, target, queryVector(), 5);

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().materialTitle()).isEqualTo("Tài liệu cần tìm");
    }

    @Test
    @DisplayName("Sinh đề KHÔNG được với tới tài liệu người khác, kể cả tài liệu đã chia sẻ")
    void shouldKeepGenerationScopedToOwner() {
        // searchSimilar (dùng cho sinh đề) cố tình bỏ qua cờ shared: Creator soạn đề từ tài liệu mình
        // đã tải lên, không có lý do lấy nội dung người khác vào đề của mình
        UUID sharedByOther = insertMaterial(otherOwnerId, "Tài liệu người khác đã chia sẻ", true);
        insertChunk(sharedByOther, 0, "Nội dung của người khác");
        jdbc.update("update learning_materials set shared = true where id = ?", sharedByOther);

        List<MaterialChunkRepository.Chunk> found =
                repository.searchSimilar(ownerId, null, queryVector(), 5);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Trợ lý học tập thấy tài liệu ĐÃ chia sẻ của người khác, và chỉ tài liệu đã chia sẻ")
    void shouldIncludeSharedMaterialsForAssistant() {
        UUID shared = insertMaterial(otherOwnerId, "Bài giảng đã chia sẻ", true);
        UUID unshared = insertMaterial(otherOwnerId, "Ghi chú riêng tư", true);
        insertChunk(shared, 0, "Nội dung được chia sẻ");
        insertChunk(unshared, 0, "Nội dung riêng tư");
        jdbc.update("update learning_materials set shared = true where id = ?", shared);

        List<MaterialChunkRepository.Chunk> found =
                repository.searchSimilarIncludingShared(ownerId, null, queryVector(), 5);

        assertThat(found).hasSize(1);
        assertThat(found.getFirst().materialTitle()).isEqualTo("Bài giảng đã chia sẻ");
    }

    @Test
    @DisplayName("Tài liệu chưa xử lý xong bị loại — vector của nó chưa đáng tin để trả lời")
    void shouldIgnoreMaterialsNotReady() {
        UUID processing = insertMaterial(ownerId, "Đang xử lý", false);
        insertChunk(processing, 0, "Đoạn của tài liệu chưa xong");

        assertThat(repository.searchSimilar(ownerId, null, queryVector(), 5)).isEmpty();
    }

    @Test
    @DisplayName("limit được tôn trọng và kết quả xếp theo khoảng cách tăng dần")
    void shouldRespectLimitAndOrder() {
        UUID material = insertMaterial(ownerId, "Tài liệu nhiều đoạn", true);
        // Đoạn 0 trùng khít vector truy vấn (khoảng cách 0); các đoạn sau lệch dần
        insertChunk(material, 0, "Đoạn giống nhất", queryVector());
        insertChunk(material, 1, "Đoạn lệch một chút", tiltedVector(0.3f));
        insertChunk(material, 2, "Đoạn lệch nhiều", tiltedVector(0.9f));

        List<MaterialChunkRepository.Chunk> found =
                repository.searchSimilar(ownerId, null, queryVector(), 2);

        assertThat(found).hasSize(2);
        assertThat(found.getFirst().content()).isEqualTo("Đoạn giống nhất");
        assertThat(found.getFirst().distance()).isLessThan(found.get(1).distance());
    }

    // ------------------------------------------------------------------ helper

    /** Vector truy vấn cố định: 768 chiều, dồn hết vào chiều đầu. */
    private List<Float> queryVector() {
        List<Float> vector = new ArrayList<>(768);
        vector.add(1.0f);
        for (int i = 1; i < 768; i++) {
            vector.add(0.0f);
        }
        return vector;
    }

    /** Vector lệch khỏi vector truy vấn một góc, để kiểm thứ tự sắp xếp theo khoảng cách. */
    private List<Float> tiltedVector(float tilt) {
        List<Float> vector = queryVector();
        vector.set(1, tilt);
        return vector;
    }

    private UUID insertUser(String email) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into users (id, email, password_hash, display_name, role)
                values (?, ?, 'khong-dung-den', 'Người dùng test', 'CREATOR')
                """, id, email);
        return id;
    }

    private UUID insertMaterial(UUID owner, String title, boolean ready) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into learning_materials (id, owner_id, title, source_type, status)
                values (?, ?, ?, 'TEXT', ?)
                """, id, owner, title, ready ? "READY" : "PROCESSING");
        return id;
    }

    private void insertChunk(UUID materialId, int index, String content) {
        insertChunk(materialId, index, content, queryVector());
    }

    private void insertChunk(UUID materialId, int index, String content, List<Float> embedding) {
        repository.insert(materialId, index, content, embedding);
    }
}