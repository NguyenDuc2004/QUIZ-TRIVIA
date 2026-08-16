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

import java.time.LocalDate;
import java.util.UUID;

/**
 * Thử thách của một ngày — bảng `daily_challenges` (features/13, FR-52).
 * <p>
 * Một thử thách cho mỗi ngày, <b>dùng chung cho mọi người</b>: đơn giản hơn sinh riêng cho từng người, và
 * tạo được cảm giác cùng làm một việc trong ngày. Ràng buộc UNIQUE trên ngày là thứ chặn tạo trùng khi hai
 * người mở trang cùng lúc.
 */
@Entity
@Table(name = "daily_challenges")
@Getter
@Setter
@NoArgsConstructor
public class DailyChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "challenge_date", nullable = false, unique = true)
    private LocalDate challengeDate;

    @Column(nullable = false, length = 200)
    private String description;

    /** JSON dạng {@code {"type":"COMPLETE_ATTEMPTS","target":3}}. */
    // @JdbcTypeCode(JSON) là bắt buộc: thiếu nó thì Hibernate gửi String như `character varying` và
    // PostgreSQL từ chối ghi vào cột jsonb. columnDefinition chỉ ảnh hưởng lúc sinh schema, không ảnh
    // hưởng cách tham số được gửi.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String rule;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward;
}
