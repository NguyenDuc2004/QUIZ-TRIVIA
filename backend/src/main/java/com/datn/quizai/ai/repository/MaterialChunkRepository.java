package com.datn.quizai.ai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Kho vector các đoạn học liệu — bảng `material_chunks` (pgvector).
 * <p>
 * Dùng {@link JdbcTemplate} thay vì JPA vì Hibernate không có kiểu {@code vector} sẵn: mọi thao
 * tác đều phải {@code cast(? as vector)} nên map entity chỉ thêm một lớp trung gian vô ích.
 * Đây thuần tuý là một vector store, viết SQL thẳng lại rõ ý hơn.
 */
@Repository
public class MaterialChunkRepository {

    private final JdbcTemplate jdbc;

    public MaterialChunkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Một đoạn học liệu lấy được từ similarity search. */
    public record Chunk(UUID id, UUID materialId, String materialTitle, int chunkIndex,
                        String content, double distance) {
    }

    public void insert(UUID materialId, int chunkIndex, String content, List<Float> embedding) {
        jdbc.update("""
                        insert into material_chunks (id, material_id, chunk_index, content, embedding)
                        values (?, ?, ?, ?, cast(? as vector))
                        """,
                UUID.randomUUID(), materialId, chunkIndex, content, toVectorLiteral(embedding));
    }

    public void deleteByMaterialId(UUID materialId) {
        jdbc.update("delete from material_chunks where material_id = ?", materialId);
    }

    public int countByMaterialId(UUID materialId) {
        Integer count = jdbc.queryForObject(
                "select count(*) from material_chunks where material_id = ?", Integer.class, materialId);
        return count == null ? 0 : count;
    }

    /**
     * Tìm {@code limit} đoạn gần nghĩa nhất trong học liệu <b>của chính người gọi</b>.
     * <p>
     * Dùng cho sinh đề (features/05): Creator soạn đề từ tài liệu mình đã tải lên, không có lý do
     * chạm tới tài liệu người khác.
     *
     * @param materialId giới hạn trong một tài liệu; null = tìm trong mọi tài liệu của người đó
     */
    public List<Chunk> searchSimilar(UUID ownerId, UUID materialId, List<Float> queryEmbedding, int limit) {
        return search(ownerId, materialId, false, queryEmbedding, limit);
    }

    /**
     * Như trên nhưng <b>kèm cả học liệu đã chia sẻ</b> của người khác — dùng cho trợ lý học tập
     * (features/08).
     * <p>
     * Người học không sở hữu học liệu nào, nên nếu chỉ tìm trong tài liệu của chính họ thì mọi câu
     * hỏi đều truy xuất được con số không, và mô hình sẽ trả lời bằng kiến thức nền của nó — tức là
     * bịa, đúng thứ RAG sinh ra để chống.
     * <p>
     * Chỉ mở tới tài liệu mà chủ của nó <b>đã chủ động bật</b> {@code shared}. Đây là lằn ranh duy
     * nhất: tài liệu không bật cờ vẫn tuyệt đối riêng tư.
     */
    public List<Chunk> searchSimilarIncludingShared(UUID userId, UUID materialId,
                                                    List<Float> queryEmbedding, int limit) {
        return search(userId, materialId, true, queryEmbedding, limit);
    }

    /**
     * Toán tử {@code <=>} của pgvector là cosine distance — càng nhỏ càng giống.
     * <p>
     * {@code includeShared} nằm trong câu SQL thay vì thành hai câu riêng: hai bản gần như y hệt thì
     * sửa một chỗ mà quên chỗ kia là chuyện của thời gian, mà chỗ dễ quên nhất lại chính là điều kiện
     * lọc quyền.
     */
    private List<Chunk> search(UUID userId, UUID materialId, boolean includeShared,
                               List<Float> queryEmbedding, int limit) {
        // Ghép điều kiện giới hạn tài liệu ở Java, KHÔNG viết "(cast(? as uuid) is null or m.id = ?)"
        // rồi truyền null vào.
        //
        // Bản trước làm đúng như vậy và nhánh null LẶNG LẼ KHÔNG TRẢ VỀ GÌ: đo thật trên PostgreSQL,
        // cùng một câu hỏi với materialId cụ thể ra 1 đoạn (khoảng cách 0.238), còn để null ra 0 đoạn.
        // Không có lỗi, không có cảnh báo — chỉ là kho vector coi như rỗng. Hậu quả nặng vì cả hai
        // tính năng RAG đều dựa vào nhánh này: trợ lý học tập trả lời "không có tài liệu" dù kho đầy,
        // và sinh đề với useMaterials=true nhưng không chọn tài liệu cụ thể thì mô hình sinh câu hỏi
        // từ kiến thức nền của nó — tức là bịa, đúng thứ RAG sinh ra để chống.
        //
        // Chuỗi ghép vào đây là hằng số trong mã nguồn, không phải dữ liệu người dùng, nên không có
        // đường tiêm SQL; id vẫn đi qua tham số.
        String materialFilter = materialId == null ? "" : " and m.id = ? ";

        List<Object> args = new ArrayList<>();
        args.add(toVectorLiteral(queryEmbedding));
        args.add(userId);
        args.add(includeShared);
        if (materialId != null) {
            args.add(materialId);
        }
        args.add(limit);

        String sql = """
                        select c.id, c.material_id, m.title, c.chunk_index, c.content,
                               c.embedding <=> cast(? as vector) as distance
                        from material_chunks c
                                 join learning_materials m on m.id = c.material_id
                        where (m.owner_id = ? or (? and m.shared = true))
                          and m.status = 'READY'
                          and c.embedding is not null
                        """ + materialFilter + """
                        order by distance
                        limit ?
                        """;
        return jdbc.query(sql,
                (rs, rowNum) -> new Chunk(
                        rs.getObject("id", UUID.class),
                        rs.getObject("material_id", UUID.class),
                        rs.getString("title"),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getDouble("distance")),
                args.toArray());
    }

    /** pgvector nhận vector dưới dạng chuỗi {@code [0.1,0.2,…]}. */
    private String toVectorLiteral(List<Float> embedding) {
        StringBuilder sb = new StringBuilder(embedding.size() * 8 + 2).append('[');
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding.get(i));
        }
        return sb.append(']').toString();
    }
}
