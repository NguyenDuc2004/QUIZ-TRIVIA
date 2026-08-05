package com.datn.quizai.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test tầng JPA trên PostgreSQL thật (Testcontainers) — schema do Flyway dựng,
 * nên đồng thời kiểm luôn migration V1 chạy được và ràng buộc DB có hiệu lực.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
class UserRepositoryTest {

    /** Dùng image pgvector vì migration V1 có `CREATE EXTENSION vector`. */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Lưu user → sinh UUID, mốc thời gian và vai trò mặc định LEARNER")
    void shouldPersistUserWithGeneratedFields() {
        User saved = userRepository.saveAndFlush(
                new User("hoc.vien@example.com", "hash", "Học Viên", Role.LEARNER));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.LEARNER);
        assertThat(saved.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("findByEmail tìm đúng user; email không tồn tại trả Optional rỗng")
    void shouldFindByEmail() {
        userRepository.saveAndFlush(new User("creator@example.com", "hash", "Người Tạo", Role.CREATOR));

        Optional<User> found = userRepository.findByEmail("creator@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Người Tạo");
        assertThat(found.get().getRole()).isEqualTo(Role.CREATOR);

        assertThat(userRepository.findByEmail("khong-ton-tai@example.com")).isEmpty();
    }

    @Test
    @DisplayName("Ràng buộc UNIQUE trên email chặn trùng ở tầng DB")
    void shouldRejectDuplicateEmailAtDatabaseLevel() {
        userRepository.saveAndFlush(new User("trung@example.com", "hash", "Bản Gốc", Role.LEARNER));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("trung@example.com", "hash-khac", "Bản Trùng", Role.LEARNER)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Lưu và đọc lại tên có dấu tiếng Việt không bị sai bảng mã")
    void shouldStoreVietnameseCharactersCorrectly() {
        userRepository.saveAndFlush(
                new User("tieng.viet@example.com", "hash", "Nguyễn Khắc Minh Đức", Role.LEARNER));
        userRepository.flush();

        assertThat(userRepository.findByEmail("tieng.viet@example.com"))
                .get()
                .extracting(User::getDisplayName)
                .isEqualTo("Nguyễn Khắc Minh Đức");
    }

    @Test
    @DisplayName("existsByEmail phản ánh đúng trạng thái dữ liệu")
    void shouldCheckExistenceByEmail() {
        userRepository.saveAndFlush(new User("co.that@example.com", "hash", "Có Thật", Role.LEARNER));

        assertThat(userRepository.existsByEmail("co.that@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("khong.co@example.com")).isFalse();
    }
}
