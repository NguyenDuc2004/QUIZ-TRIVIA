package com.datn.quizai.user.repository;

import com.datn.quizai.user.domain.Role;
import com.datn.quizai.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** Tra theo `sub` của Google — ổn định hơn email vì người dùng đổi được địa chỉ Gmail. */
    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Danh sách người dùng cho trang quản trị, lọc theo từ khoá / vai trò / trạng thái khoá
     * (features/10). Mọi tham số lọc đều cho phép null = không lọc theo tiêu chí đó.
     * <p>
     * Tìm theo cả email và tên hiển thị vì quản trị viên có thể chỉ nhớ một trong hai. Dùng
     * {@code lower(...) like} thay vì so khớp chính xác: người dùng thật hiếm khi nhớ đủ email.
     * <p>
     * <b>{@code lower()} chỉ áp cho CỘT, không áp cho tham số</b>, và {@code keyword} phải được bọc
     * {@code %} sẵn ở tầng service. Viết {@code like lower(concat('%', :keyword, '%'))} thì khi
     * {@code keyword} là null, driver PostgreSQL không suy được kiểu tham số nên gửi dưới dạng
     * {@code bytea} và truy vấn đổ với {@code function lower(bytea) does not exist} — mất luôn cả
     * nhánh "không lọc theo từ khoá". Đây là cùng một cái bẫy đã ghi ở
     * {@code MaterialChunkRepository}: cẩn thận với tham số null trong biểu thức {@code :x is null}.
     */
    @Query("""
            select u from User u
            where (:keyword is null
                   or lower(u.email) like :keyword
                   or lower(u.displayName) like :keyword)
              and (:role is null or u.role = :role)
              and (:locked is null or u.locked = :locked)
            """)
    Page<User> search(@Param("keyword") String keyword,
                      @Param("role") Role role,
                      @Param("locked") Boolean locked,
                      Pageable pageable);

    long countByRole(Role role);

    long countByLockedTrue();
}
