package com.datn.quizai.admin.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Tổng quan sức khoẻ hệ thống cho trang đầu khu quản trị (features/10, FR-40 đến FR-42).
 * <p>
 * Mọi con số ở đây <b>đến từ dữ liệu thật</b> trong cơ sở dữ liệu. Không có chỉ số nào được tính ra
 * bằng cách ước lượng hay nhân hệ số — `ui-design-system.md §7` cấm hiển thị dữ liệu không có thật, và
 * một trang tổng quan sai số còn tệ hơn không có trang tổng quan: người ta ra quyết định dựa trên nó.
 *
 * @param phongDangCho    phòng đấu ở trạng thái chờ người vào
 * @param phongDangChoi   phòng đang thi đấu — hai con số tách nhau vì chúng nói hai chuyện khác nhau:
 *                        phòng chờ lâu không ai vào là dấu hiệu người dùng bỏ giữa, còn phòng đang chơi
 *                        là tải thật lên tầng WebSocket
 * @param tokenThangNay   tổng token trong tháng dương lịch hiện tại — cắt theo tháng vì hạn mức và chi
 *                        phí của nhà cung cấp cũng tính theo tháng
 */
public record SystemOverview(
        // ----- người dùng -----
        long tongNguoiDung,
        long soNguoiHoc,
        long soNguoiTaoNoiDung,
        long soQuanTri,
        long soBiKhoa,
        long dangKyHomNay,

        // ----- nội dung -----
        long tongQuiz,
        long quizCongKhai,
        long tongCauHoi,
        long tongHocLieu,

        // ----- hoạt động -----
        long tongLuotLamBai,
        long luotLamBaiHomNay,
        long phongDangCho,
        long phongDangChoi,

        // ----- chi phí AI tháng này -----
        long luotGoiAiThangNay,
        long tokenThangNay,

        // ----- dữ liệu biểu đồ -----
        List<DiemTheoNgay> tangTruong,
        List<PhanBoDanhMuc> theoDanhMuc,
        List<TiLeHoanThanh> tiLeHoanThanh
) {
    /** Một điểm trên biểu đồ đường: số người đăng ký và số lượt làm bài trong ngày đó. */
    public record DiemTheoNgay(LocalDate ngay, long nguoiDungMoi, long luotLamBai) {
    }

    /** Một phần của biểu đồ tròn. {@code danhMuc} là "Chưa phân loại" khi quiz không gắn danh mục. */
    public record PhanBoDanhMuc(String danhMuc, long soQuiz) {
    }

    /**
     * Một cột của biểu đồ tỉ lệ hoàn thành.
     *
     * @param doChinhXacTrungBinh phần trăm, {@code null} khi chưa có bài nào đã nộp — không trả 0 vì
     *                            "0% đúng" và "chưa ai làm" là hai chuyện khác nhau
     */
    public record TiLeHoanThanh(String trangThai, long soLuot, Double doChinhXacTrungBinh) {
    }
}
