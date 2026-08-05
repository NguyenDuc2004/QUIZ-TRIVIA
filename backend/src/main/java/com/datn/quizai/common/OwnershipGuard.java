package com.datn.quizai.common;

import com.datn.quizai.auth.service.JwtService;
import com.datn.quizai.common.exception.BusinessException;
import com.datn.quizai.user.domain.Role;

import java.util.UUID;

/**
 * Kiểm tra quyền sở hữu tài nguyên (docs/security.md §2 — Broken Access Control).
 * Admin được phép quản lý tài nguyên của mọi người; ngoài ra chỉ chủ sở hữu.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {
    }

    /** @throws BusinessException 403 nếu người gọi không phải chủ sở hữu và không phải Admin */
    public static void assertCanManage(UUID ownerId, JwtService.AuthenticatedUser current, String resource) {
        if (current.role() == Role.ADMIN || ownerId.equals(current.id())) {
            return;
        }
        throw BusinessException.forbidden("Bạn không có quyền thao tác trên " + resource + " của người khác");
    }

    public static boolean canManage(UUID ownerId, JwtService.AuthenticatedUser current) {
        return current != null && (current.role() == Role.ADMIN || ownerId.equals(current.id()));
    }
}
