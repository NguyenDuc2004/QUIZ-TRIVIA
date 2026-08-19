package com.datn.quizai.classroom.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mã lớp được <b>đọc to và chép tay</b>, nên hai tính chất dưới đây quan trọng hơn là chuyện nó ngẫu nhiên:
 * đúng khuôn mà ràng buộc {@code CHECK} của V19 chấp nhận, và không chứa ký tự người ta đọc nhầm.
 */
class ClassCodeGeneratorTest {

    @Test
    @DisplayName("Mã đúng khuôn ràng buộc CHECK của V19: 6 ký tự [A-Z2-9]")
    void shouldMatchDatabaseConstraint() {
        // Sinh sai khuôn thì lỗi không nổ ở đây mà nổ lúc INSERT, với một thông báo ràng buộc khó đọc
        for (int i = 0; i < 500; i++) {
            assertThat(ClassCodeGenerator.sinh()).matches("^[A-Z2-9]{6}$");
        }
    }

    @Test
    @DisplayName("KHÔNG chứa 0, O, 1, I, L — năm ký tự người ta đọc nhầm nhất")
    void shouldAvoidConfusableCharacters() {
        for (int i = 0; i < 500; i++) {
            assertThat(ClassCodeGenerator.sinh()).doesNotContainAnyWhitespaces()
                    .doesNotContain("0").doesNotContain("O")
                    .doesNotContain("1").doesNotContain("I").doesNotContain("L");
        }
    }

    @Test
    @DisplayName("Không phát ra cùng một mã liên tiếp — đủ tản để va chạm là hiếm")
    void shouldSpreadAcrossSpace() {
        // Không kiểm "ngẫu nhiên" (không kiểm được bằng một phép assert), chỉ kiểm nó không kẹt ở vài giá
        // trị. Kẹt thì vòng thử-lại khi trùng mã ở service sẽ quay mãi.
        Set<String> da = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            da.add(ClassCodeGenerator.sinh());
        }
        assertThat(da).hasSizeGreaterThan(995);
    }
}
