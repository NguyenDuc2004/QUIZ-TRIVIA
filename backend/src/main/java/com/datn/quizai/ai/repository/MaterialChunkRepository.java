package com.datn.quizai.ai.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
     * Tìm {@code limit} đoạn gần nghĩa nhất với vector truy vấn, bằng cosine distance
     * (toán tử {@code <=>} của pgvector — càng nhỏ càng giống).
     * <p>
     * Luôn lọc theo {@code ownerId}: học liệu là tài sản riêng, không được để truy vấn của người
     * này lôi ra nội dung tài liệu của người khác.
     *
     * @param materialId giới hạn trong một tài liệu; null = tìm trong mọi tài liệu của người đó
     */
    public List<Chunk> searchSimilar(UUID ownerId, UUID materialId, List<Float> queryEmbedding, int limit) {
        return jdbc.query("""
                        select c.id, c.material_id, m.title, c.chunk_index, c.content,
                               c.embedding <=> cast(? as vector) as distance
                        from material_chunks c
                                 join learning_materials m on m.id = c.material_id
                        where m.owner_id = ?
                          and m.status = 'READY'
                          and (cast(? as uuid) is null or m.id = cast(? as uuid))
                          and c.embedding is not null
                        order by distance
                        limit ?
                        """,
                (rs, rowNum) -> new Chunk(
                        rs.getObject("id", UUID.class),
                        rs.getObject("material_id", UUID.class),
                        rs.getString("title"),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getDouble("distance")),
                toVectorLiteral(queryEmbedding), ownerId, materialId, materialId, limit);
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
