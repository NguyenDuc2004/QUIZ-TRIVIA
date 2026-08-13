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
    @DisplayName("Đoạn được phép đọc phải tìm ra dù có NHIỀU đoạn riêng tư gần hơn nó")
    void shouldFindPermittedChunkEvenWhenPrivateOnesAreCloser() {
        // Tái hiện lỗi mà bản trước mắc: câu SQL phẳng "where <quyền> order by distance limit n" khiến
        // PostgreSQL dùng index vector lấy n đoạn gần nhất TOÀN KHO rồi mới lọc quyền lên n dòng đó.
        // Đoạn không được phép đọc bị loại mà không có gì bù lại, nên kết quả rỗng dù kho có đoạn hợp lệ.
        //
        // Ở đây 10 đoạn riêng tư của người khác đều gần vector truy vấn hơn đoạn đã chia sẻ. Với limit 5,
        // cách xếp-trước-lọc-sau sẽ chọn đúng 5 đoạn riêng tư rồi loại sạch → trả về rỗng.
        UUID privateOfOther = insertMaterial(otherOwnerId, "Tài liệu riêng của người khác", true);
        for (int i = 0; i < 10; i++) {
            insertChunk(privateOfOther, i, "Đoạn riêng tư " + i, queryVector());
        }

        UUID shared = insertMaterial(otherOwnerId, "Bài giảng đã chia sẻ", true);
        insertChunk(shared, 0, "Đoạn đã chia sẻ, xa hơn một chút", tiltedVector(0.4f));
        jdbc.update("update learning_materials set shared = true where id = ?", shared);

        List<MaterialChunkRepository.Chunk> found =
                repository.searchSimilarIncludingShared(ownerId, null, queryVector(), 5);

        assertThat(found).as("lọc quyền phải xảy ra TRƯỚC khi xếp theo khoảng cách và cắt limit")
                .hasSize(1);
        assertThat(found.getFirst().materialTitle()).isEqualTo("Bài giảng đã chia sẻ");
    }

    @Test
    @DisplayName("Kho vector KHÔNG được có chỉ mục xấp xỉ — chỉ mục ANN đứng trước bộ lọc quyền làm mất kết quả")
    void shouldNotHaveApproximateIndexOnEmbedding() {
        // Chốt chặn ở tầng schema, không phải tầng hành vi — và đây là lựa chọn có cân nhắc.
        //
        // Lỗi thật (13/08) là: chỉ mục IVFFlat lấy n đoạn gần nhất TOÀN KHO rồi mới lọc quyền lên n
        // dòng đó, nên đoạn được phép đọc bị loại mất mà không có gì bù lại. Trên dev, trợ lý trả lời
        // "không có tài liệu" trong khi kho có 9 đoạn hợp lệ nói đúng chủ đề câu hỏi.
        //
        // Đã thử viết ca hồi quy theo hành vi và KHÔNG tái hiện được ổn định: lỗi chỉ xuất hiện khi bộ
        // tối ưu chọn đi qua chỉ mục, mà với lượng dữ liệu một ca test dựng ra thì nó luôn chọn quét
        // tuần tự — và quét tuần tự thì lọc quyền chạy trước nên kết quả đúng kể cả với câu SQL sai.
        // Thử buộc bằng `enable_seqscan = off` trên một kết nối riêng cũng không đủ: câu truy vấn thật
        // có JOIN nên kế hoạch rẽ hướng khác. Một ca test xanh trong cả hai trường hợp thì không bảo vệ
        // gì cả, chỉ tạo cảm giác an toàn — nên bỏ nó đi và chặn ở chỗ chặn được.
        //
        // Chỗ chặn được là chính điều kiện làm lỗi tái phát: sự tồn tại của một chỉ mục xấp xỉ. Ai thêm
        // lại `ivfflat`/`hnsw` sẽ thấy ca này đỏ kèm lời giải thích, thay vì thấy trợ lý im lặng trả về
        // rỗng vài tuần sau đó.
        List<String> approximateIndexes = jdbc.queryForList("""
                select indexdef from pg_indexes
                where tablename = 'material_chunks'
                  and (indexdef ilike '%ivfflat%' or indexdef ilike '%hnsw%')
                """, String.class);

        assertThat(approximateIndexes).as("""
                Có chỉ mục ANN trên material_chunks. Truy xuất RAG phải lọc quyền đọc TRƯỚC rồi mới xếp \
                theo khoảng cách; chỉ mục xấp xỉ làm ngược lại nên bỏ sót đoạn được phép đọc, và bỏ sót \
                trong im lặng. Nếu thật sự cần ANN vì kho đã quá lớn: dùng HNSW kèm \
                `SET hnsw.iterative_scan = relaxed_order` (pgvector 0.8) và kiểm lại bằng dữ liệu thật, \
                rồi mới sửa ca test này.""")
                .isEmpty();
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