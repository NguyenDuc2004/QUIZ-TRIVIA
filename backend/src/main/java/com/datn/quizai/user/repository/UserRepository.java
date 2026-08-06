package com.datn.quizai.user.repository;

import com.datn.quizai.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Tra theo `sub` của Google — ổn định hơn email vì người dùng đổi được địa chỉ Gmail. */
    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
