package com.datn.quizai.classroom.service;

import com.datn.quizai.classroom.dto.AssignmentResponse;
import com.datn.quizai.classroom.dto.AssignmentResultRow;
import com.datn.quizai.classroom.dto.AssignmentResultsResponse;
import com.datn.quizai.classroom.dto.TrangThaiBaiTap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Xuất bảng điểm CSV (features/14, FR-58).
 *
 * <h3>Ba luật ở đây đều HỎNG LẶNG LẼ — đó là lý do chúng cần test</h3>
 * Không luật nào làm server trả lỗi. File vẫn 200, vẫn tải về, vẫn mở được; chỉ nội dung là sai, và chỉ
 * người mở file mới thấy:
 * <ol>
 *   <li><b>Thiếu BOM</b> → Excel trên Windows đọc "Nguyễn" thành "Nguyá»…n".</li>
 *   <li><b>Không thoát ký tự</b> → một dấu phẩy trong tên người đẩy lệch cả hàng, điểm gán sai người.</li>
 *   <li><b>Không chặn tiêm công thức</b> → một cái tên bắt đầu bằng {@code =} chạy như công thức trên máy
 *       giáo viên. Nạn nhân là người không làm gì sai.</li>
 * </ol>
 */
class BangDiemCsvWriterTest {

    @Test
    @DisplayName("Có BOM UTF-8 ở đầu tệp — thiếu nó là tiếng Việt lỗi hết trên Excel")
    void shouldStartWithUtf8Bom() {
        String csv = BangDiemCsvWriter.dung(ketQua(dong("Nguyễn Văn An", 8, TrangThaiBaiTap.DA_NOP)));

        assertThat(csv).startsWith("﻿");
        assertThat(csv).contains("Nguyễn Văn An");
    }

    @Test
    @DisplayName("Người CHƯA NỘP để ô trống, không phải 0")
    void shouldLeaveScoreBlankWhenNotSubmitted() {
        String csv = BangDiemCsvWriter.dung(ketQua(
                new AssignmentResultRow(UUID.randomUUID(), "Trần Thị Bình", null, null, null, null,
                        TrangThaiBaiTap.CHUA_LAM, "Chưa làm")));

        String dongDuLieu = BangDiemCsvWriter.cacDong(csv).getLast();
        // Ghi 0 là nói sai về một người chưa làm bài, và mọi phép trung bình trên cột đó sẽ sai theo
        assertThat(dongDuLieu).contains("\"Trần Thị Bình\",\"Chưa làm\",,,");
        assertThat(dongDuLieu).doesNotContain(",0,");
    }

    @Test
    @DisplayName("Tên chứa dấu phẩy không được làm lệch cột")
    void shouldEscapeComma() {
        String csv = BangDiemCsvWriter.dung(ketQua(dong("Nguyễn Văn An, Jr.", 7, TrangThaiBaiTap.DA_NOP)));

        String dongDuLieu = BangDiemCsvWriter.cacDong(csv).getLast();
        assertThat(dongDuLieu).startsWith("\"Nguyễn Văn An, Jr.\",");
        // Không thoát thì hàng này có 6 ô thay vì 5, và điểm bị gán lệch sang cột khác
        assertThat(demOTrongDong(dongDuLieu)).isEqualTo(5);
    }

    @Test
    @DisplayName("Tên chứa dấu nháy kép được nhân đôi theo RFC 4180")
    void shouldEscapeQuote() {
        String csv = BangDiemCsvWriter.dung(ketQua(dong("Lê \"Bảo\" Ngọc", 9, TrangThaiBaiTap.DA_NOP)));

        assertThat(BangDiemCsvWriter.cacDong(csv).getLast())
                .startsWith("\"Lê \"\"Bảo\"\" Ngọc\",");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "=HYPERLINK(\"http://ke-xau\",\"Bấm vào\")",
            "+1+1",
            "-2+3",
            "@SUM(A1:A9)",
    })
    @DisplayName("Chặn tiêm công thức: ô bắt đầu bằng = + - @ phải được vô hiệu hoá")
    void shouldNeutraliseFormulaInjection(String tenDoc) {
        // Tên hiển thị do người dùng tự đặt. Excel coi mọi ô bắt đầu bằng bốn ký tự này là CÔNG THỨC và
        // chạy nó khi giáo viên mở file — nạn nhân là giáo viên, người không làm gì sai cả.
        String csv = BangDiemCsvWriter.dung(ketQua(dong(tenDoc, 5, TrangThaiBaiTap.DA_NOP)));
        String dongDuLieu = BangDiemCsvWriter.cacDong(csv).getLast();

        // Dấu nháy đơn phía trước: Excel hiểu "ô này là chữ", hiện đúng tên nhưng không chạy gì
        assertThat(dongDuLieu).startsWith("\"'" + tenDoc.replace("\"", "\"\""));
        assertThat(dongDuLieu).doesNotStartWith("\"" + tenDoc.charAt(0));
    }

    @Test
    @DisplayName("Bọc ngoặc kép KHÔNG đủ để chặn công thức — phải có dấu nháy đơn")
    void quotesAloneDoNotStopExcel() {
        // Ghi lại nhầm lẫn hay gặp nhất: Excel vẫn diễn giải công thức nằm trong ngoặc kép của CSV.
        // Ngoặc kép là luật ĐỊNH DẠNG (RFC 4180), không phải luật an toàn.
        assertThat(BangDiemCsvWriter.oAnToan("=1+1")).isEqualTo("\"'=1+1\"");
        assertThat(BangDiemCsvWriter.oAnToan("An toàn")).isEqualTo("\"An toàn\"");
    }

    @Test
    @DisplayName("Có dòng bối cảnh ở đầu: lớp nào, bài nào, hạn bao giờ")
    void shouldCarryContextHeader() {
        // Một file tên "bang-diem.csv" nằm trong thư mục Tải về sau hai tuần thì không ai nhớ nó của lớp nào
        String csv = BangDiemCsvWriter.dung(ketQua(dong("Nguyễn Văn An", 8, TrangThaiBaiTap.DA_NOP)));

        assertThat(csv).contains("Toán 12A1").contains("Bài tập tuần 3").contains("Kiểm tra Đạo hàm");
    }

    @Test
    @DisplayName("Tên tệp bỏ ký tự hệ điều hành không cho đặt")
    void shouldCleanFileName() {
        assertThat(BangDiemCsvWriter.tenTep("Bài tập: tuần 3/4"))
                .isEqualTo("bang-diem-Bài tập tuần 34.csv")
                .doesNotContain("/").doesNotContain(":");

        assertThat(BangDiemCsvWriter.tenTep(null)).isEqualTo("bang-diem-bang-diem.csv");
    }

    // ------------------------------------------------------------------ dựng dữ liệu

    private static AssignmentResultRow dong(String ten, int diem, TrangThaiBaiTap trangThai) {
        return new AssignmentResultRow(UUID.randomUUID(), ten, UUID.randomUUID(), diem, 10,
                OffsetDateTime.of(2026, 8, 20, 14, 30, 0, 0, ZoneOffset.UTC), trangThai, "Đã nộp");
    }

    private static AssignmentResultsResponse ketQua(AssignmentResultRow... rows) {
        AssignmentResponse baiTap = new AssignmentResponse(
                UUID.randomUUID(),                                              // id
                UUID.randomUUID(),                                              // classroomId
                "Toán 12A1",                                                    // tenLop
                "Bài tập tuần 3",                                               // title
                "Làm hết 3 câu",                                                // instruction
                UUID.randomUUID(),                                              // quizId
                "Kiểm tra Đạo hàm",                                             // quizTitle
                3,                                                              // soCau
                null,                                                           // openAt
                OffsetDateTime.of(2026, 8, 21, 23, 59, 0, 0, ZoneOffset.UTC),   // dueAt
                null, null, null, null, null, null);

        return new AssignmentResultsResponse(baiTap, rows.length, rows.length, 0, 8, List.of(rows));
    }

    /** Đếm số ô của một dòng CSV, tôn trọng dấu phẩy nằm trong ngoặc kép. */
    private static int demOTrongDong(String dong) {
        int so = 1;
        boolean trongNgoac = false;
        for (char c : dong.toCharArray()) {
            if (c == '"') {
                trongNgoac = !trongNgoac;
            } else if (c == ',' && !trongNgoac) {
                so++;
            }
        }
        return so;
    }
}
