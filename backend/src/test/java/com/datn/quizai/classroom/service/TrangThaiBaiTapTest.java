package com.datn.quizai.classroom.service;

import com.datn.quizai.attempt.domain.AttemptStatus;
import com.datn.quizai.attempt.domain.QuizAttempt;
import com.datn.quizai.classroom.domain.Assignment;
import com.datn.quizai.classroom.dto.TrangThaiBaiTap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trạng thái một bài tập với một học sinh — phần <b>có nhánh logic thật</b> của cả lát cắt 14, và là thứ
 * quyết định giáo viên nhìn thấy gì trong bảng theo dõi.
 * <p>
 * Test đơn vị chứ không tích hợp: hàm này thuần, chỉ nhận ba thứ (bài tập, lượt làm bài, thời điểm) và không
 * đụng cơ sở dữ liệu. Kiểm qua HTTP thì mỗi phép kiểm phải dựng lớp, thành viên, quiz, bài tập — bốn bước
 * không liên quan, mỗi bước một chỗ có thể vỡ vì lý do khác, và chạy chậm gấp nghìn lần.
 */
class TrangThaiBaiTapTest {

    private static final OffsetDateTime BAY_GIO = OffsetDateTime.parse("2026-08-18T10:00:00Z");
    private static final OffsetDateTime HOM_QUA = BAY_GIO.minusDays(1);
    private static final OffsetDateTime NGAY_MAI = BAY_GIO.plusDays(1);

    private Assignment baiTap(OffsetDateTime dueAt) {
        return new Assignment(null, null, "Bài tập", null, null, dueAt);
    }

    private QuizAttempt dangLam() {
        QuizAttempt a = new QuizAttempt();
        a.setStatus(AttemptStatus.IN_PROGRESS);
        return a;
    }

    private QuizAttempt daNopLuc(OffsetDateTime luc) {
        QuizAttempt a = new QuizAttempt();
        a.setStatus(AttemptStatus.SUBMITTED);
        a.setSubmittedAt(luc);
        return a;
    }

    @Test
    @DisplayName("Chưa làm, còn hạn → CHƯA LÀM")
    void shouldBeNotStarted() {
        assertThat(AssignmentService.trangThaiCua(baiTap(NGAY_MAI), null, BAY_GIO))
                .isEqualTo(TrangThaiBaiTap.CHUA_LAM);
    }

    @Test
    @DisplayName("Chưa làm, đã quá hạn → QUÁ HẠN")
    void shouldBeOverdue() {
        assertThat(AssignmentService.trangThaiCua(baiTap(HOM_QUA), null, BAY_GIO))
                .isEqualTo(TrangThaiBaiTap.QUA_HAN);
    }

    @Test
    @DisplayName("Bài KHÔNG có hạn thì không bao giờ quá hạn, dù để bao lâu")
    void shouldNeverBeOverdueWithoutDueDate() {
        // dueAt null là hợp lệ (giáo viên không muốn đặt hạn). Coi null là "hạn đã qua" thì mọi bài không
        // hạn đều đỏ lòm "quá hạn" ngay từ lúc giao.
        assertThat(AssignmentService.trangThaiCua(baiTap(null), null, BAY_GIO.plusYears(5)))
                .isEqualTo(TrangThaiBaiTap.CHUA_LAM);
    }

    @Test
    @DisplayName("Đang làm dở → ĐANG LÀM, kể cả khi đã quá hạn")
    void shouldBeInProgress() {
        assertThat(AssignmentService.trangThaiCua(baiTap(HOM_QUA), dangLam(), BAY_GIO))
                .isEqualTo(TrangThaiBaiTap.DANG_LAM);
    }

    @Test
    @DisplayName("Nộp trước hạn → ĐÃ NỘP")
    void shouldBeSubmittedOnTime() {
        assertThat(AssignmentService.trangThaiCua(baiTap(NGAY_MAI), daNopLuc(BAY_GIO), BAY_GIO))
                .isEqualTo(TrangThaiBaiTap.DA_NOP);
    }

    @Test
    @DisplayName("Nộp sau hạn → NỘP MUỘN")
    void shouldBeSubmittedLate() {
        assertThat(AssignmentService.trangThaiCua(baiTap(HOM_QUA), daNopLuc(BAY_GIO), BAY_GIO))
                .isEqualTo(TrangThaiBaiTap.NOP_TRE);
    }

    @Test
    @DisplayName("Bài nộp ĐÚNG HẠN không được thành nộp muộn chỉ vì hôm nay giáo viên mới mở bảng ra xem")
    void shouldNotBecomeLateWhenViewedLater() {
        // Đây là cái bẫy: so `now` với hạn thay vì so `submittedAt` với hạn. Không thấy khi test ngay sau
        // khi nộp, nhưng một tuần sau thì cả lớp bỗng thành nộp muộn — và điểm bị trừ oan.
        OffsetDateTime hanNop = BAY_GIO;
        OffsetDateTime nopSomHonHan = BAY_GIO.minusHours(2);
        OffsetDateTime giaoVienXemSauMotTuan = BAY_GIO.plusDays(7);

        assertThat(AssignmentService.trangThaiCua(
                baiTap(hanNop), daNopLuc(nopSomHonHan), giaoVienXemSauMotTuan))
                .isEqualTo(TrangThaiBaiTap.DA_NOP);
    }

    @Test
    @DisplayName("Bài không hạn thì nộp lúc nào cũng là ĐÃ NỘP, không phải nộp muộn")
    void shouldNeverBeLateWithoutDueDate() {
        assertThat(AssignmentService.trangThaiCua(
                baiTap(null), daNopLuc(BAY_GIO.plusYears(1)), BAY_GIO.plusYears(1)))
                .isEqualTo(TrangThaiBaiTap.DA_NOP);
    }
}
