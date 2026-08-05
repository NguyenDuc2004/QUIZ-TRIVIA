package com.datn.quizai.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Bao kết quả phân trang theo quy ước `?page=0&size=20&sort=createdAt,desc` (docs/api.md §10).
 * <p>
 * Dùng DTO riêng thay vì trả thẳng {@code Page}/{@code PageImpl} của Spring Data —
 * cấu trúc JSON của lớp đó không cam kết ổn định giữa các phiên bản.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <E, T> PageResponse<T> of(Page<E> source, Function<E, T> mapper) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.isLast());
    }
}
