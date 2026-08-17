package com.datn.quizai.gamification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Định nghĩa một huy hiệu — bảng `badges` (features/13, FR-50).
 * <p>
 * Điều kiện lưu dạng <b>dữ liệu</b> ({@code condition} JSONB), không hardcode trong Java: thêm huy hiệu mới
 * chỉ cần một dòng INSERT, miễn là {@code type} đã được hỗ trợ. Nếu viết điều kiện thành mã thì mỗi huy hiệu
 * mới là một lần sửa và triển khai lại.
 */
@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    /** JSON dạng {@code {"type":"XP","threshold":500}}. Đọc bằng ObjectMapper ở tầng service. */
    // @JdbcTypeCode(JSON) là bắt buộc: thiếu nó thì Hibernate gửi String như `character varying` và
    // PostgreSQL từ chối ghi vào cột jsonb. columnDefinition chỉ ảnh hưởng lúc sinh schema, không ảnh
    // hưởng cách tham số được gửi.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String condition;

    @Column(length = 20)
    private String icon;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
